package com.coraline.palace;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Durable owner-specific well encounter rules. Not a quest driver: callers must
 * supply real server-side playback, melee, custody and physical-action evidence.
 * This class never spawns actors, moves items, grants rewards or acknowledges audio.
 */
public final class PalaceWellProgress {
    public enum Phase {
        DRAG, INTRO_A, INTRO_B, INTRO_C, INTRO_D, RESCUE, RESCUED, BOSS, DEFEATED,
        SHATTERED, WRAPPED, KEY_BUNDLED, DISPOSED, FAREWELL_F, FAREWELL_G,
        FAREWELL_H, FAREWELL_I, DEPARTING, COMPLETE
    }
    // DEAD_AT_WELL remains readable for old saves; new witnessed falls are LOST.
    public enum WybieLife { ALIVE, DEAD_BEFORE_WELL, DEAD_AT_WELL, LOST_AT_WELL }
    public record Prerequisites(boolean chapterComplete, boolean doorPermanentlySealed,
                                boolean parentsReturned, boolean farewellComplete,
                                boolean eyesReleased, boolean catDeparted, boolean handSevered) {
        public boolean ready() {
            return chapterComplete && doorPermanentlySealed && parentsReturned
                    && farewellComplete && eyesReleased && catDeparted && handSevered;
        }
    }

    private final UUID owner, door;
    private UUID hand, wybie, session, bundleReceipt;
    private Phase phase;
    private WybieLife life;
    private int ticks, rescueTicks, attempts;
    private long lastServerTick = -1;
    private String blocker = "";
    private CompoundTag incompatibleOriginal;

    private PalaceWellProgress(UUID owner, UUID door) {
        this.owner = Objects.requireNonNull(owner);
        this.door = Objects.requireNonNull(door);
    }

    /** A missing live original is a blocker, not permission to create Wybie.
     * Optional-visit bans are intentionally not part of these prerequisites.
     */
    public static Optional<PalaceWellProgress> start(UUID owner, UUID door, UUID originalHand,
                                                    UUID originalWybie, boolean previouslyDead,
                                                    Prerequisites prerequisites) {
        if (prerequisites == null || !prerequisites.ready() || originalHand == null
                || (!previouslyDead && originalWybie == null)) return Optional.empty();
        var state = new PalaceWellProgress(owner, door);
        state.hand = originalHand;
        state.wybie = originalWybie;
        state.life = previouslyDead ? WybieLife.DEAD_BEFORE_WELL : WybieLife.ALIVE;
        state.phase = Phase.DRAG;
        state.session = UUID.randomUUID();
        state.attempts = 1;
        return Optional.of(state);
    }

    public Phase phase() { return phase; }
    public WybieLife wybieLife() { return life; }
    public UUID originalHand() { return hand; }
    public Optional<UUID> originalWybie() { return Optional.ofNullable(wybie); }
    public UUID session() { return session; }
    public int activeTicks() { return ticks; }
    public int rescueTicks() { return rescueTicks; }
    public int attempts() { return attempts; }
    public String blocker() { return blocker; }
    public boolean wybieCanAssist() { return blocker.isEmpty() && life == WybieLife.ALIVE && phase == Phase.BOSS; }

    private boolean owns(UUID actor, UUID token) {
        return blocker.isEmpty() && owner.equals(actor) && session != null && session.equals(token);
    }
    private void enter(Phase next) { phase = next; ticks = 0; }
    private void rotateSession() { session = UUID.randomUUID(); lastServerTick = -1; }

    /** Called only for an online, active owner. Offline elapsed wall/world time
     * must not be replayed. One invocation per server tick, with no catch-up loop.
     */
    public boolean activeTick(UUID actor, UUID token, long serverTick) {
        if (!owns(actor, token) || serverTick < 0 || serverTick <= lastServerTick
                || (phase != Phase.DRAG && phase != Phase.RESCUE)) return false;
        lastServerTick = serverTick;
        ticks++;
        if (phase == Phase.DRAG && ticks == PalaceJourneyTiming.HAND_DRAG)
            enter(life == WybieLife.ALIVE ? Phase.INTRO_A : Phase.BOSS);
        else if (phase == Phase.RESCUE && ticks == rescueTicks) {
            life = WybieLife.LOST_AT_WELL;
            enter(Phase.BOSS);
        }
        return true;
    }

    /** Only the existing heard-recording completion gate plus actual staged
     * motion may advance A-D. A cue name/client packet alone cannot advance it.
     * Route length is needed only at D and must come from a reachable path.
     */
    public boolean finishIntro(UUID actor, UUID token, char cue, boolean heardEnd,
                               boolean motionComplete, double reachableRouteLength) {
        if (!owns(actor, token) || !heardEnd || !motionComplete || life != WybieLife.ALIVE
                || cue < 'A' || cue > 'D' || phase != Phase.valueOf("INTRO_" + cue)) return false;
        if (cue == 'D') {
            int window;
            try { window = PalaceJourneyTiming.rescueWindow(reachableRouteLength); }
            catch (IllegalArgumentException noFairRoute) { return false; }
            rescueTicks = window;
            enter(Phase.RESCUE);
        } else enter(Phase.valueOf("INTRO_" + (char) (cue + 1)));
        return true;
    }

    /** The server attack handler must establish melee, reach and line of sight.
     * Ranged hits, another owner's attacks and hits on another hand never count.
     * At the deadline the failure wins regardless of attack/tick handler order.
     */
    public boolean rescueHit(UUID actor, UUID token, UUID target, long serverTick,
                             boolean melee, boolean reachable, boolean visible) {
        if (!owns(actor, token) || phase != Phase.RESCUE || !hand.equals(target)
                || !melee || !reachable || !visible || serverTick < 0
                || serverTick < lastServerTick) return false;
        // An attack may run before the encounter tick in this same server tick.
        int atHit = ticks + (serverTick > lastServerTick ? 1 : 0);
        if (atHit >= rescueTicks) return false;
        enter(Phase.RESCUED);
        return true;
    }

    public boolean climbedOut(UUID actor, UUID token, UUID original, boolean safelyOnGround) {
        if (!owns(actor, token) || phase != Phase.RESCUED || !Objects.equals(wybie, original) || !safelyOnGround) return false;
        enter(Phase.BOSS);
        return true;
    }

    /** Reconnect invalidates old input but preserves the rescue cursor and death.
     * Boss retries restart only the fight; neither path can resurrect/replay D.
     */
    public boolean resume(UUID actor) {
        if (!blocker.isEmpty() || !owner.equals(actor) || phase == Phase.COMPLETE) return false;
        rotateSession();
        return true;
    }
    public boolean retryBoss(UUID actor, UUID token) {
        if (!owns(actor, token) || phase != Phase.BOSS || attempts == Integer.MAX_VALUE) return false;
        attempts++;
        ticks = 0;
        rotateSession();
        return true;
    }
    public boolean defeated(UUID actor, UUID token, UUID target) {
        if (!owns(actor, token) || phase != Phase.BOSS || !hand.equals(target)) return false;
        enter(Phase.DEFEATED);
        return true;
    }
    public boolean shattered(UUID actor, UUID token, UUID target, boolean physicalSmashComplete) {
        if (!owns(actor, token) || phase != Phase.DEFEATED || !hand.equals(target) || !physicalSmashComplete) return false;
        enter(Phase.SHATTERED);
        return true;
    }
    public boolean wrapped(UUID actor, UUID token, boolean clothRockAndFragmentsPresent) {
        if (!owns(actor, token) || phase != Phase.SHATTERED || !clothRockAndFragmentsPresent) return false;
        enter(Phase.WRAPPED);
        return true;
    }

    /** This records a completed external custody transaction, NOT an item stack.
     * The eventual inventory/bundle controller must atomically remove and escrow
     * the actual bound original before calling this; this method cannot do so.
     */
    public boolean keyBundled(UUID actor, UUID token, UUID keyOwner, UUID keyDoor, UUID custodyReceipt) {
        if (!owns(actor, token) || phase != Phase.WRAPPED || !owner.equals(keyOwner)
                || !door.equals(keyDoor) || custodyReceipt == null || bundleReceipt != null) return false;
        bundleReceipt = custodyReceipt;
        enter(Phase.KEY_BUNDLED);
        return true;
    }
    public boolean disposed(UUID actor, UUID token, UUID custodyReceipt, boolean bundleDownShaft,
                             boolean heardEEnd) {
        if (!owns(actor, token) || phase != Phase.KEY_BUNDLED || !bundleReceipt.equals(custodyReceipt)
                || !bundleDownShaft || (life == WybieLife.ALIVE && !heardEEnd)) return false;
        enter(Phase.DISPOSED);
        return true;
    }
    public boolean beginFarewell(UUID actor, UUID token) {
        if (!owns(actor, token) || phase != Phase.DISPOSED) return false;
        enter(life == WybieLife.ALIVE ? Phase.FAREWELL_F : Phase.COMPLETE);
        return true;
    }
    public boolean finishFarewell(UUID actor, UUID token, char cue, boolean heardEnd, boolean motionComplete) {
        if (!owns(actor, token) || life != WybieLife.ALIVE || cue < 'F' || cue > 'I'
                || phase != Phase.valueOf("FAREWELL_" + cue) || !heardEnd || !motionComplete) return false;
        enter(cue == 'I' ? Phase.DEPARTING : Phase.valueOf("FAREWELL_" + (char) (cue + 1)));
        return true;
    }
    public boolean departed(UUID actor, UUID token, UUID original, boolean safelyOffscreen) {
        if (!owns(actor, token) || phase != Phase.DEPARTING || !Objects.equals(wybie, original) || !safelyOffscreen) return false;
        enter(Phase.COMPLETE);
        return true;
    }
    public Optional<Character> recording() {
        if (!blocker.isEmpty() || life != WybieLife.ALIVE) return Optional.empty();
        if (phase.name().startsWith("INTRO_")) return Optional.of(phase.name().charAt(6));
        if (phase == Phase.KEY_BUNDLED) return Optional.of('E');
        if (phase.name().startsWith("FAREWELL_")) return Optional.of(phase.name().charAt(9));
        return Optional.empty();
    }

    public CompoundTag save() {
        if (incompatibleOriginal != null) {
            var preserved = incompatibleOriginal.copy();
            preserved.putString("Blocker", blocker);
            return preserved;
        }
        var root = new CompoundTag();
        root.putInt("Version", 1);
        root.putUUID("Owner", owner); root.putUUID("Door", door);
        root.putUUID("Hand", hand); root.putUUID("Session", session);
        if (wybie != null) root.putUUID("Wybie", wybie);
        if (bundleReceipt != null) root.putUUID("BundleReceipt", bundleReceipt);
        root.putString("Phase", phase.name()); root.putString("WybieLife", life.name());
        root.putInt("Ticks", ticks); root.putInt("RescueTicks", rescueTicks);
        root.putInt("Attempts", attempts); root.putLong("LastServerTick", lastServerTick);
        return root;
    }
    public static PalaceWellProgress load(UUID owner, UUID door, CompoundTag root) {
        var state = new PalaceWellProgress(owner, door);
        try {
            if (!root.contains("Version", Tag.TAG_INT) || root.getInt("Version") != 1)
                throw new IllegalArgumentException("Unknown well schema");
            for (String key : new String[]{"Owner", "Door", "Hand", "Session"})
                if (!root.hasUUID(key)) throw new IllegalArgumentException("Missing " + key);
            if (!owner.equals(root.getUUID("Owner")) || !door.equals(root.getUUID("Door")))
                throw new IllegalArgumentException("Well owner/door identity mismatch");
            state.hand = root.getUUID("Hand"); state.session = root.getUUID("Session");
            if (root.contains("Wybie")) {
                if (!root.hasUUID("Wybie")) throw new IllegalArgumentException("Malformed original Wybie identity");
                state.wybie = root.getUUID("Wybie");
            }
            if (root.contains("BundleReceipt")) {
                if (!root.hasUUID("BundleReceipt")) throw new IllegalArgumentException("Malformed custody receipt");
                state.bundleReceipt = root.getUUID("BundleReceipt");
            }
            state.phase = Phase.valueOf(root.getString("Phase"));
            state.life = WybieLife.valueOf(root.getString("WybieLife"));
            for (String key : new String[]{"Ticks", "RescueTicks", "Attempts"})
                if (!root.contains(key, Tag.TAG_INT)) throw new IllegalArgumentException("Malformed " + key);
            if (!root.contains("LastServerTick", Tag.TAG_LONG)) throw new IllegalArgumentException("Malformed active clock");
            state.ticks = root.getInt("Ticks"); state.rescueTicks = root.getInt("RescueTicks");
            state.attempts = root.getInt("Attempts"); state.lastServerTick = root.getLong("LastServerTick");
            state.validate();
            // Quarantine remains quarantine across repeated reloads.
            if (root.contains("Blocker")) throw new IllegalArgumentException("Previously blocked save");
        } catch (IllegalArgumentException bad) {
            state.blocker = "Saved well encounter needs compatibility review: " + bad.getMessage();
            state.incompatibleOriginal = root.copy();
        }
        return state;
    }
    private void validate() {
        if (ticks < 0 || attempts < 1 || lastServerTick < -1 || rescueTicks < 0
                || (rescueTicks != 0 && (rescueTicks < 100 || rescueTicks > 160)))
            throw new IllegalArgumentException("Invalid encounter clock");
        if (life != WybieLife.DEAD_BEFORE_WELL && wybie == null)
            throw new IllegalArgumentException("Missing canonical live/deceased Wybie identity");
        if (phase == Phase.DRAG ? ticks >= PalaceJourneyTiming.HAND_DRAG
                : phase == Phase.RESCUE ? rescueTicks == 0 || ticks >= rescueTicks : ticks != 0)
            throw new IllegalArgumentException("Cursor outside its active phase");
        if ((phase.name().startsWith("INTRO_") || phase == Phase.RESCUE || phase == Phase.RESCUED
                || phase.name().startsWith("FAREWELL_") || phase == Phase.DEPARTING) && life != WybieLife.ALIVE)
            throw new IllegalArgumentException("Dead Wybie in a live-only scene");
        if (rescueTicks != 0 && phase.ordinal() < Phase.RESCUE.ordinal()
                || life == WybieLife.ALIVE && phase.ordinal() >= Phase.RESCUED.ordinal() && rescueTicks == 0)
            throw new IllegalArgumentException("Rescue branch has no reachable-route window");
        if ((life == WybieLife.DEAD_AT_WELL || life == WybieLife.LOST_AT_WELL) && (rescueTicks == 0 || phase.ordinal() < Phase.BOSS.ordinal()))
            throw new IllegalArgumentException("Death without completed rescue window");
        if (life == WybieLife.DEAD_BEFORE_WELL && rescueTicks != 0)
            throw new IllegalArgumentException("Previously dead Wybie entered rescue branch");
        if ((phase.ordinal() >= Phase.KEY_BUNDLED.ordinal()) != (bundleReceipt != null))
            throw new IllegalArgumentException("Missing/premature original-key custody receipt");
    }
}
