package com.coraline.feast;

import com.coraline.otherworld.OtherWorld;
import com.coraline.parents.ParentEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Dialogue and attendance timing only; the owning activity system supplies portal/gameplay callbacks. */
@Mod.EventBusSubscriber(modid="coraline")
public final class OtherMotherActivities {
    private OtherMotherActivities() {}
    public static final int WINDOW_TICKS = 5 * 60 * 20;
    public enum Activity {
        OTHER_VILLAGE, OTHER_WOODLAND_MANSION, OTHER_DESERT_TEMPLE, MAGIC_SHOW,
        DREAM_PARKOUR, GRAND_FEAST, MICE_CIRCUS, CARNIVAL, MAGIC_GARDEN, MUSIC_FESTIVAL,
        SHOPPING_MALL, SNOWBALL_BATTLE, CAKE_MAKING_CONTEST, FISHING_CONTEST,
        ARCHERY_CONTEST, ICE_BOAT_RACE, GARDEN_MAZE, TREASURE_HUNT, OTHER_MINESHAFT,
        OTHER_OCEAN_MONUMENT, OTHER_NETHER_FORTRESS, OTHER_END_CITY;
        public String id() { return name().toLowerCase(Locale.ROOT); }
        public OtherMotherDialogue.Pool pool() {
            return new OtherMotherDialogue.Pool("activities/" + id(), 2, "proposal");
        }
    }
    public static Activity chooseActivity(RandomSource random) {
        return switch(random.nextInt(22)){
            case 0->Activity.OTHER_VILLAGE;
            case 1->Activity.ARCHERY_CONTEST;
            case 2->Activity.CAKE_MAKING_CONTEST;
            case 3->Activity.CARNIVAL;
            case 4->Activity.DREAM_PARKOUR;
            case 5->Activity.FISHING_CONTEST;
            case 6->Activity.GARDEN_MAZE;
            case 7->Activity.GRAND_FEAST;
            case 8->Activity.MAGIC_SHOW;
            case 9->Activity.MAGIC_GARDEN;
            case 10->Activity.ICE_BOAT_RACE;
            case 11->Activity.MICE_CIRCUS;
            case 12->Activity.MUSIC_FESTIVAL;
            case 13->Activity.OTHER_DESERT_TEMPLE;
            case 14->Activity.OTHER_END_CITY;
            case 15->Activity.OTHER_MINESHAFT;
            case 16->Activity.OTHER_NETHER_FORTRESS;
            case 17->Activity.OTHER_WOODLAND_MANSION;
            case 18->Activity.SHOPPING_MALL;
            case 19->Activity.SNOWBALL_BATTLE;
            case 20->Activity.TREASURE_HUNT;
            default->Activity.OTHER_OCEAN_MONUMENT;
        };
    }
    public static OtherMotherDialogue.Pool waitPool(OtherMotherWelcome.State state) {
        return new OtherMotherDialogue.Pool("activity_wait/" + state.id(), 3, "wait");
    }
    public enum Arrival { ON_TIME, LATE }
    public static OtherMotherDialogue.Pool arrivalPool(OtherMotherWelcome.State state, Arrival arrival) {
        return new OtherMotherDialogue.Pool("activity_arrival/" + state.id() + "/"
                + arrival.name().toLowerCase(Locale.ROOT), 3, "arrival");
    }
    public enum Phase { PROPOSAL, WARNING, WINDOW, ARRIVAL_WAIT, GREETING, ARRIVED, EXPIRED }

    /** Small deterministic state machine, shared by runtime and timing tests. */
    public static final class Sequence {
        private Phase phase = Phase.PROPOSAL;
        private long nextAt;
        private long attendanceDeadline;
        private Arrival recordedArrival;
        public Sequence(long now, int proposalTicks) {
            if (proposalTicks <= 0) throw new IllegalArgumentException("Proposal must have a duration");
            nextAt = now + proposalTicks;
        }
        public Phase phase() { return phase; }
        public long deadline() { return attendanceDeadline; }
        public boolean ready(long now) { return now >= nextAt; }
        public void warning(long now, int ticks) {
            if (phase != Phase.PROPOSAL || !ready(now) || ticks <= 0) throw new IllegalStateException("Proposal has not finished");
            phase = Phase.WARNING; nextAt = now + ticks;
        }
        public void open(long now) {
            if (phase != Phase.WARNING || !ready(now)) throw new IllegalStateException("Warning has not finished");
            phase = Phase.WINDOW; nextAt = now + WINDOW_TICKS;
            attendanceDeadline = nextAt;
        }
        /** The attendance timer, not the dialogue pool, classifies the arrival. */
        public Arrival arrival(long now) {
            if (phase != Phase.WINDOW && phase != Phase.EXPIRED) return null;
            return now < attendanceDeadline ? Arrival.ON_TIME : Arrival.LATE;
        }
        public Arrival recordedArrival() { return recordedArrival; }
        public boolean recordArrival(long now) {
            Arrival result = arrival(now);
            if (result == null) return false;
            recordedArrival = result; phase = Phase.ARRIVAL_WAIT; nextAt = now; return true;
        }
        public boolean greet(long now, int ticks) {
            if (phase != Phase.ARRIVAL_WAIT || ticks <= 0) return false;
            phase = Phase.GREETING; nextAt = now + ticks; return true;
        }
        public boolean finishGreeting(long now) {
            if (phase != Phase.GREETING || !ready(now)) return false;
            phase = Phase.ARRIVED; return true;
        }
        public boolean expire(long now) {
            if (phase != Phase.WINDOW || !ready(now)) return false;
            phase = Phase.EXPIRED; return true;
        }
    }
    public interface Listener {
        /** Called once AFTER the warning, with a full five minutes remaining. */
        void portalReady(ServerPlayer player, Activity activity, long deadlineTick);
        /** Start/teleport into the selected activity here, AFTER the arrival recording finishes. */
        default void arrived(ServerPlayer player, Activity activity) {}
        /** Deadline notification only. May apply existing penalties or cancel if late entry is forbidden. */
        default void expired(ServerPlayer player, Activity activity) {}
        default void cancelled(ServerPlayer player, Activity activity) {}
    }
    private record Pending(MinecraftServer server, VisitData.Scene scene, Activity activity,
                           Sequence sequence, Listener listener) {}
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    /** Start at the activity-proposal stage; does not replay welcome, loot or tutorial. */
    public static boolean begin(ServerPlayer player, Activity activity, Listener listener) {
        Objects.requireNonNull(activity); Objects.requireNonNull(listener);
        if (active(player)) return false;
        int ticks = OtherMotherDialogue.play(player, activity.pool());
        if (ticks == 0) return false;
        var scene = VisitData.get(player.server).scenes.get(player.getUUID());
        PENDING.put(player.getUUID(), new Pending(player.server, scene, activity,
                new Sequence(now(player), ticks), listener));
        return true;
    }
    /** Call at the tutorial stage after the loot comment. No additional dialogue is inserted. */
    public static boolean beginAfterTutorial(ServerPlayer player, Activity activity, Listener listener) {
        Objects.requireNonNull(activity); Objects.requireNonNull(listener);
        if (active(player)) return false;
        return OtherMotherTutorial.begin(player, p -> {
            if (!begin(p, activity, listener)) listener.cancelled(p, activity);
        });
    }
    public static boolean active(ServerPlayer player) {
        var pending = PENDING.get(player.getUUID());
        return pending != null && pending.server == player.server;
    }
    static boolean speaking(ServerPlayer player) {
        var p = PENDING.get(player.getUUID());
        return p != null && p.server == player.server && (p.sequence.phase() == Phase.PROPOSAL
                || p.sequence.phase() == Phase.WARNING || p.sequence.phase() == Phase.ARRIVAL_WAIT
                || p.sequence.phase() == Phase.GREETING);
    }
    public static long remainingTicks(ServerPlayer player) {
        var p = PENDING.get(player.getUUID());
        return p == null || p.server != player.server || p.sequence.phase() != Phase.WINDOW
                ? 0 : Math.max(0, p.sequence.deadline() - now(player));
    }
    /** Portal code calls BEFORE teleporting. True means arrival recorded, NOT permission to teleport yet.
     * Late entry uses the retained deadline; the activity owner may separately cancel expired activities. */
    public static boolean arrive(ServerPlayer player, Activity activity) {
        var p = PENDING.get(player.getUUID());
        if (p == null || p.server != player.server || p.activity != activity || !valid(player, p)) return false;
        long now = now(player);
        Arrival arrival = p.sequence.arrival(now);
        if (arrival == null) return false;
        // Preserve the deadline callback even when portal interaction occurs before this tick's expiry check.
        if (p.sequence.expire(now)) p.listener.expired(player, activity);
        if (PENDING.get(player.getUUID()) != p || !valid(player, p)) return false;
        if (!p.sequence.recordArrival(now)) return false;
        greet(player, p, now);
        return true;
    }
    private static void greet(ServerPlayer player, Pending p, long now) {
        int ticks = OtherMotherDialogue.playActivityArrival(player,
                arrivalPool(p.scene.welcomeState, p.sequence.recordedArrival()));
        if (ticks > 0) p.sequence.greet(now, ticks);
    }
    public static void cancel(ServerPlayer player) {
        var p = PENDING.get(player.getUUID());
        if (p == null || p.server != player.server) return;
        if (speaking(player)) VisitAudio.stopVoice(player);
        PENDING.remove(player.getUUID());
        p.listener.cancelled(player, p.activity);
    }
    private static long now(ServerPlayer player) { return player.server.overworld().getGameTime(); }
    private static boolean valid(ServerPlayer player, Pending pending) {
        var scene = VisitData.get(player.server).scenes.get(player.getUUID());
        var tag = player.getPersistentData();
        if (!player.isAlive() || scene != pending.scene || scene.stage != VisitData.Stage.DONE
                || !OtherWorld.isOther(player.level()) || tag.getBoolean("CoralineInTunnel")
                || !tag.hasUUID("CoralineVisitDoor") || !scene.door.equals(tag.getUUID("CoralineVisitDoor"))) return false;
        if (scene.mom == null) return false;
        var mother = player.serverLevel().getEntity(scene.mom);
        // Distant portal chunks need not keep the mother loaded, but reject a loaded dead/invalid parent.
        if (mother != null) return mother instanceof ParentEntity parent && parent.isAlive()
                && parent.isMom() && parent.isOtherParent();
        return pending.sequence.phase() != Phase.PROPOSAL && pending.sequence.phase() != Phase.WARNING;
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        var pending = PENDING.get(player.getUUID());
        if (pending == null || pending.server != player.server) return;
        if (!valid(player, pending)) { cancel(player); return; }
        long now = now(player);
        var sequence = pending.sequence;
        if (!sequence.ready(now)) return;
        switch (sequence.phase()) {
            case PROPOSAL -> {
                // Read current state NOW, not when the activity was chosen or tutorial began.
                int ticks = OtherMotherDialogue.playActivityContinuation(player, waitPool(pending.scene.welcomeState));
                if (ticks > 0) sequence.warning(now, ticks);
            }
            case WARNING -> {
                sequence.open(now);
                pending.listener.portalReady(player, pending.activity, sequence.deadline());
            }
            case WINDOW -> {
                if (sequence.expire(now)) {
                    pending.listener.expired(player, pending.activity);
                }
            }
            case GREETING -> {
                if (sequence.finishGreeting(now)) {
                    PENDING.remove(player.getUUID());
                    pending.listener.arrived(player, pending.activity);
                }
            }
            case ARRIVAL_WAIT -> greet(player, pending, now);
            default -> { }
        }
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
