package com.coraline.feast;

import com.coraline.otherworld.OtherWorld;
import com.coraline.parents.ParentEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/** One rules pool, independent of relationships and loot. The caller supplies the next stage. */
@Mod.EventBusSubscriber(modid="coraline")
public final class OtherMotherTutorial {
    private OtherMotherTutorial() {}
    public static final OtherMotherDialogue.Pool POOL = new OtherMotherDialogue.Pool("tutorial", 4, "tutorial");
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    /** A one-shot deadline: never continues before the complete recording duration. */
    public static final class Completion {
        private final long deadline;
        private boolean consumed;
        public Completion(long deadline) { this.deadline = deadline; }
        public boolean advance(long now, boolean valid, Runnable next) {
            if (consumed) return false;
            if (!valid) { consumed = true; return false; }
            if (now < deadline) return false;
            consumed = true;
            next.run();
            return true;
        }
    }
    private record Pending(MinecraftServer server, VisitData.Scene scene, Completion completion,
                           Consumer<ServerPlayer> activityProposal) {}

    /** Invoke only at the tutorial stage, after the loot comment has finished.
     * False means busy/ineligible: nothing was selected or played; the caller may retry.
     * True schedules the supplied activity proposal immediately after this clip finishes.
     * An interrupted visit cancels the callback, rather than proposing activities at home. */
    public static boolean begin(ServerPlayer player, Consumer<ServerPlayer> activityProposal) {
        Objects.requireNonNull(activityProposal, "Activity proposal continuation is required");
        int duration = OtherMotherDialogue.play(player, POOL);
        if (duration == 0) return false;
        var scene = VisitData.get(player.server).scenes.get(player.getUUID());
        PENDING.put(player.getUUID(), new Pending(player.server, scene,
                new Completion(player.server.overworld().getGameTime() + duration), activityProposal));
        return true;
    }
    static boolean pending(ServerPlayer player) {
        var pending = PENDING.get(player.getUUID());
        return pending != null && pending.server == player.server;
    }
    public static void cancel(ServerPlayer player) {
        if (pending(player)) { PENDING.remove(player.getUUID()); VisitAudio.stopVoice(player); }
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        var pending = PENDING.get(player.getUUID());
        if (pending == null || pending.server != player.server) return;
        var scene = VisitData.get(player.server).scenes.get(player.getUUID());
        var tag = player.getPersistentData();
        boolean valid = player.isAlive() && scene == pending.scene && scene.stage == VisitData.Stage.DONE
                && OtherWorld.isOther(player.level()) && !tag.getBoolean("CoralineInTunnel")
                && tag.hasUUID("CoralineVisitDoor") && scene.door.equals(tag.getUUID("CoralineVisitDoor"))
                && scene.mom != null && player.serverLevel().getEntity(scene.mom) instanceof ParentEntity mother
                && mother.isAlive() && mother.isMom() && mother.isOtherParent();
        if (!valid) { cancel(player); return; }
        pending.completion.advance(player.server.overworld().getGameTime(), true, () -> {
            // Remove the guard before invoking the next audio stage, allowing immediate playback.
            PENDING.remove(player.getUUID());
            pending.activityProposal.accept(player);
        });
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PENDING.remove(player.getUUID());
    }
    @SubscribeEvent public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) cancel(player);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        PENDING.values().removeIf(p -> p.server == event.getServer());
    }
}
