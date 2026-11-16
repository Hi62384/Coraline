package com.coraline.feast;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.coraline.secrets.Reconciliation;
import static com.coraline.feast.OtherMotherLoopState.Phase.*;

/** Server-owned operator audition of the CURRENT genuine recording. Never a story-completion path. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="coraline")
public final class ShowAudioSeek {
    // Deliberately not persisted: a server restart cannot resume an unaudible offset cursor.
    private static final java.util.Map<UUID,UUID> ACTIVE=new java.util.HashMap<>();
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void serverStopped(net.minecraftforge.event.server.ServerStoppedEvent event){ACTIVE.clear();}
    public static boolean request(ServerPlayer player, UUID observedNonce, int observedSerial,
                                  String observedSound, int targetTick) {
        if (!player.hasPermissions(2)) return false;
        VisitData.Scene scene = OtherMotherLoop.scene(player);
        if (scene == null || !scene.owner.equals(player.getUUID())) return false;
        CompoundTag cursor = ShowPlayback.data(scene);
        if (!ShowPlayback.ownsSession(player, scene) || !cursor.hasUUID("Nonce")
            || !cursor.getUUID("Nonce").equals(observedNonce)
            || scene.loop.event.getInt("Serial") != observedSerial
            || !cursor.getString("Sound").equals(observedSound)) return false;
        if (!ShowSeekAvailability.productionReady()) return reject(player, ShowSeekAvailability.blocker());
        if (!contextValid(player, scene) || cursor.getBoolean("Finished")
            || cursor.getBoolean("SeekPending") || !cursor.getString("Blocker").isEmpty())
            return reject(player, "Seek requires an active owned performance in its venue. Recover a blocked recording first.");
        ShowSeekAvailability.Media media = ShowSeekAvailability.media(observedSound);
        if (media == null || media.durationTicks() != cursor.getInt("Duration")
            || media.durationTicks() != ShowRecordings.duration(observedSound))
            return reject(player, "This recording has no verified seek identity and decoded duration.");
        try { media.frameAtTick(targetTick); }
        catch (IllegalArgumentException invalid) { return reject(player, "Seek must be inside the current recording, before its final frame."); }

        // Everything above is read-only. Rotate the nonce before any asynchronous work.
        // Replaying the same request now fails; the old stream cannot acknowledge the new cursor.
        UUID next = UUID.randomUUID();
        boolean music = cursor.getBoolean("Music");
        ShowPlaybackNetwork.cancelSeek(player, observedNonce);
        if (music) VisitAudio.stopMusic(player, observedSound); else VisitAudio.stopVoice(player, observedSound);
        ShowPlayerSafety.release(player, scene);
        CompoundTag event = eventData(scene);
        event.putBoolean("DebugPaused", true);
        event.putBoolean("DebugAudio", false);
        event.putBoolean("DebugApproach",false);
        event.remove("DebugPreview");
        cursor.putUUID("Nonce", next);
        ACTIVE.put(scene.owner,next);
        cursor.putBoolean("DebugSeek", true);
        cursor.putBoolean("SeekPending", true);
        cursor.putInt("SeekOffset", targetTick);
        cursor.putBoolean("SeekReachedEnd", false);
        cursor.putBoolean("Started", false);
        cursor.putBoolean("Ended", false);
        cursor.putBoolean("Finished", false);
        cursor.putInt("LoadingTicks", 0);
        cursor.putInt("EndWait", 0);
        // Position remains the prior acknowledged cursor while the decoder prepares.
        ShowPlaybackNetwork.seek(player, new ShowPlaybackNetwork.SeekStart(next, observedNonce, scene.owner,
            scene.door, observedSerial, observedSound, music, targetTick, media.durationTicks()));
        OtherMotherLoop.changed(player);
        return true;
    }

    public static boolean contextValid(ServerPlayer player, VisitData.Scene scene) {
        if (!player.hasPermissions(2) || !player.isAlive() || player.isSleeping()
            || !ShowPlayback.ownsSession(player, scene) || scene.loop.suspicion >= 1
            || scene.loop.secrets.searching() || Reconciliation.active(player)) return false;
        String sound = ShowPlayback.data(scene).getString("Sound");
        CompoundTag event;
        String expected;
        if (scene.loop.phase == THEATRE_ACTIVE && TheatreVisit.owned(player, scene) && TheatreSite.inside(player, scene)) {
            event = TheatreVisit.data(scene);
            int segment = event.getInt("Segment");
            expected = segment == 0 ? "theatre_music" : segment >= 1 && segment <= 6 ? "theatre_" + (char) ('a' + segment - 1) : "";
            if (!event.getString("Phase").equals("PERFORMING")) return false;
        } else if (scene.loop.phase == CIRCUS_ACTIVE && MouseCircusVisit.owned(player, scene) && MouseCircusSite.inside(player, scene)) {
            event = MouseCircusVisit.data(scene);
            expected = switch (event.getInt("Segment")) { case 0 -> "mouse_circus_intro"; case 1 -> "mouse_circus_music"; case 2 -> "mouse_circus_end"; default -> ""; };
            if (!event.getString("Phase").equals("SHOW")) return false;
        } else return false;
        return sound.equals(expected) && !expected.isEmpty() && !event.getBoolean("NeedsRecovery")
            && !event.getBoolean("PerformanceDone");
    }

    public static void signal(ServerPlayer player, UUID nonce, int serial, String sound, int status) {
        VisitData.Scene scene = OtherMotherLoop.scene(player);
        if (scene == null || !scene.owner.equals(player.getUUID())) return;
        CompoundTag cursor = ShowPlayback.data(scene);
        if (!cursor.getBoolean("DebugSeek") || !cursor.hasUUID("Nonce") || !cursor.getUUID("Nonce").equals(nonce)
            || !cursor.getString("Sound").equals(sound) || scene.loop.event.getInt("Serial") != serial
            || !nonce.equals(ACTIVE.get(scene.owner)) || !ShowSeekAvailability.productionReady() || !contextValid(player, scene)) return;
        if (status == 0 && cursor.getBoolean("SeekPending") && cursor.getString("Blocker").isEmpty()) {
            boolean ready=scene.loop.phase==THEATRE_ACTIVE
                ?TheatreVisit.prepareSeekFrame(player,scene,cursor.getInt("SeekOffset"))
                :MouseCircusVisit.prepareSeekFrame(player,scene,cursor.getInt("SeekOffset"));
            if(!ready){continuityFailed(player,scene);return;}
            cursor.putInt("Position", cursor.getInt("SeekOffset"));
            cursor.putBoolean("Started", true);
            cursor.putBoolean("SeekJustStarted",true);
            cursor.putBoolean("SeekPending", false);
            OtherMotherLoop.changed(player);
        } else if (status == 1 && cursor.getBoolean("Started")) {
            cursor.putBoolean("Ended", true);
        } else if (status == 2) {
            fail(player, scene, "Offset playback failed or was interrupted. Recover the safe recording checkpoint.");
        }
    }

    /** Called by ShowPlayback only. Never returns a story-completion signal. */
    static void tick(ServerPlayer player, VisitData.Scene scene) {
        CompoundTag cursor = ShowPlayback.data(scene);
        if (!cursor.hasUUID("Nonce") || !cursor.getUUID("Nonce").equals(ACTIVE.get(scene.owner)) || !ShowSeekAvailability.productionReady() || !contextValid(player, scene)) {
            fail(player, scene, "Offset audition stopped because its owner, venue or performance session changed.");
            return;
        }
        if (!cursor.getString("Blocker").isEmpty() || cursor.getBoolean("SeekReachedEnd")) return;
        if (!cursor.getBoolean("Started")) {
            int waited = cursor.getInt("LoadingTicks") + 1;
            cursor.putInt("LoadingTicks", waited);
            if (waited >= 600) fail(player, scene, "Offset decoding did not start within 30 seconds. Recover the safe checkpoint.");
            return;
        }
        if(cursor.getBoolean("SeekJustStarted")){cursor.remove("SeekJustStarted");return;}
        int position = Math.min(cursor.getInt("Duration"), cursor.getInt("Position") + 1);
        cursor.putInt("Position", position);
        if (position >= cursor.getInt("Duration")) {
            if (cursor.getBoolean("Ended")) {
                cursor.putBoolean("SeekReachedEnd", true);
                ShowPlayerSafety.release(player, scene);
            } else {
                int wait = cursor.getInt("EndWait") + 1;
                cursor.putInt("EndWait", wait);
                if (wait >= 200) fail(player, scene, "Offset stream end was not acknowledged. Recover the safe checkpoint.");
            }
        }
    }

    static void stopping(ServerPlayer player, VisitData.Scene scene) {
        CompoundTag cursor = ShowPlayback.data(scene);
        if (cursor.hasUUID("Nonce")) ACTIVE.remove(scene.owner,cursor.getUUID("Nonce"));
        if (cursor.getBoolean("DebugSeek")) {
            if (cursor.hasUUID("Nonce")) ShowPlaybackNetwork.cancelSeek(player, cursor.getUUID("Nonce"));
            ShowPlayerSafety.release(player, scene);
        }
        cursor.remove("DebugSeek"); cursor.remove("SeekPending"); cursor.remove("SeekOffset"); cursor.remove("SeekReachedEnd"); cursor.remove("SeekJustStarted");
    }
    static void continuityFailed(ServerPlayer player,VisitData.Scene scene){
        fail(player,scene,"Offset actors, attachment or stage state could not be restored. Recover the safe recording checkpoint.");
    }
    private static void fail(ServerPlayer player, VisitData.Scene scene, String message) {
        CompoundTag cursor = ShowPlayback.data(scene);
        if (!cursor.getString("Blocker").isEmpty()) return;
        if (cursor.hasUUID("Nonce")) ACTIVE.remove(scene.owner,cursor.getUUID("Nonce"));
        cursor.putString("Blocker", message);
        cursor.putBoolean("Started", false);
        cursor.putBoolean("SeekPending", false);
        if (cursor.hasUUID("Nonce")) ShowPlaybackNetwork.cancelSeek(player, cursor.getUUID("Nonce"));
        ShowPlayerSafety.release(player, scene);
        OtherMotherLoop.changed(player);
    }
    private static CompoundTag eventData(VisitData.Scene scene) {
        return scene.loop.phase == THEATRE_ACTIVE ? TheatreVisit.data(scene) : MouseCircusVisit.data(scene);
    }
    private static boolean reject(ServerPlayer player, String message) { player.sendSystemMessage(Component.literal(message)); return false; }
    private ShowAudioSeek() {}
}
