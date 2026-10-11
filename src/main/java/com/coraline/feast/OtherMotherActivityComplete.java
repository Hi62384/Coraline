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

/** Activity owners call this after gameplay ends; their callback restores normal free time. */
@Mod.EventBusSubscriber(modid="coraline")
public final class OtherMotherActivityComplete {
    private OtherMotherActivityComplete() {}
    public static OtherMotherDialogue.Pool pool(OtherMotherWelcome.State state) {
        return new OtherMotherDialogue.Pool("activity_complete/" + state.id(), 3, "complete");
    }
    private record Pending(MinecraftServer server, VisitData.Scene scene,
                           OtherMotherTutorial.Completion completion, Consumer<ServerPlayer> freeTime) {}
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    /** True starts one reaction and schedules freeTime once after its complete recording duration.
     * False means busy/ineligible; the activity owner retains its end phase and can retry.
     * This hook never grants rewards or changes relationship values or selected activities. */
    public static boolean begin(ServerPlayer player, Consumer<ServerPlayer> freeTime) {
        Objects.requireNonNull(freeTime, "Free-time continuation is required");
        if (!player.isAlive() || pending(player) || OtherMotherActivities.active(player)) return false;
        var scene = VisitData.get(player.server).scenes.get(player.getUUID());
        if (scene == null) return false;
        int ticks = OtherMotherDialogue.playActivityComplete(player, pool(scene.welcomeState));
        if (ticks == 0) return false;
        PENDING.put(player.getUUID(), new Pending(player.server, scene,
                new OtherMotherTutorial.Completion(player.server.overworld().getGameTime() + ticks), freeTime));
        return true;
    }
    public static boolean pending(ServerPlayer player) {
        var p = PENDING.get(player.getUUID());
        return p != null && p.server == player.server;
    }
    public static void cancel(ServerPlayer player) {
        if (!pending(player)) return;
        PENDING.remove(player.getUUID());
        VisitAudio.stopVoice(player);
    }
    private static boolean valid(ServerPlayer player, Pending p) {
        var scene = VisitData.get(player.server).scenes.get(player.getUUID());
        var tag = player.getPersistentData();
        if (!player.isAlive() || scene != p.scene || scene.stage != VisitData.Stage.DONE
                || !OtherWorld.isOther(player.level()) || tag.getBoolean("CoralineInTunnel")
                || !tag.hasUUID("CoralineVisitDoor") || !scene.door.equals(tag.getUUID("CoralineVisitDoor"))
                || scene.mom == null) return false;
        // An activity may finish far from the mother's loaded chunk. Do not move her to play a voice.
        var mother = player.serverLevel().getEntity(scene.mom);
        return mother == null || mother instanceof ParentEntity parent && parent.isAlive()
                && parent.isMom() && parent.isOtherParent();
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        var p = PENDING.get(player.getUUID());
        if (p == null || p.server != player.server) return;
        if (!valid(player, p)) { cancel(player); return; }
        p.completion.advance(player.server.overworld().getGameTime(), true, () -> {
            PENDING.remove(player.getUUID());
            p.freeTime.accept(player);
        });
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) cancel(player);
    }
    @SubscribeEvent public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) cancel(player);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        PENDING.values().removeIf(p -> p.server == event.getServer());
    }
}
