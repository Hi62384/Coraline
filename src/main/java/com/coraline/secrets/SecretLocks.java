package com.coraline.secrets;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** The same server-validated pin game opens structure locks and wilderness loot boxes. */
public final class SecretLocks {
    /** A negative room means this attempt belongs to a scattered loot box. */
    private record Attempt(UUID nonce, int room, BlockPos box, int tier, int visit,
                           long start, int pins, int completed, double speed,
                           double window, long last) {}
    private static final Map<UUID, Attempt> ACTIVE = new HashMap<>();

    private static Attempt begin(ServerPlayer p, int room, BlockPos box, int tier) {
        var s = SecretTransactions.state(p);
        double counter = s.counter(0);
        // The difficulty progression and Mother's lock counter are shared with structures.
        int pins = 2 + tier + (int)(counter * 2);
        double speed = .035 + tier * .012 + counter * .015;
        double window = .27 - .025 * tier - .06 * counter;
        var attempt = new Attempt(UUID.randomUUID(), room, box, tier, s.visit,
                p.serverLevel().getGameTime(), pins, 0, speed, window, 0);
        ACTIVE.put(p.getUUID(), attempt);
        send(p, attempt);
        return attempt;
    }

    public static void open(ServerPlayer p, int room) {
        var s = SecretTransactions.state(p);
        if (s == null || !SecretsRuntime.active(p)) return;
        var rooms = SecretWorld.rooms(s);
        if (room < 0 || room >= rooms.size()) return;
        int tier = rooms.get(room).getInt("Tier");
        if (SecretRegistry.tier(s.value(0)) < tier) {
            p.displayClientMessage(Component.literal("Code Knowledge required: " +
                    new String[]{"0.00", "0.33", "0.66", "1.00"}[tier]), true);
            return;
        }
        if (!s.unlocked.contains(room)) begin(p, room, null, tier);
    }

    /** Box tier 1 is open; tiers 2-4 use structure lock levels 1-3. */
    public static void openLootBox(ServerPlayer p, BlockPos box, int tier) {
        var s = SecretTransactions.state(p);
        if (s == null || !SecretsRuntime.active(p) || tier < 2 || tier > 4) return;
        int level = tier - 1;
        if (SecretRegistry.tier(s.value(0)) < level) {
            p.displayClientMessage(Component.literal("Tier " + tier +
                    " loot box — Code Knowledge " +
                    new String[]{"0.00", "0.33", "0.66", "1.00"}[level] + " required"), true);
            return;
        }
        if (!WildernessLoot.lockable(p, box, tier)) return;
        begin(p, -1, box.immutable(), level);
    }

    private static void send(ServerPlayer p, Attempt a) {
        var n = new CompoundTag();
        n.putBoolean("Lock", true);
        n.putUUID("Nonce", a.nonce);
        n.putLong("Start", a.start);
        n.putInt("Pins", a.pins);
        n.putInt("Completed", a.completed);
        n.putDouble("Speed", a.speed);
        n.putDouble("Window", a.window);
        SecretsNetwork.send(p, n);
    }

    public static void pin(ServerPlayer p, UUID nonce) {
        if(com.coraline.postgame.SiteLockpicking.pin(p,nonce))return;
        var a = ACTIVE.get(p.getUUID());
        if (a == null || !a.nonce.equals(nonce) || !SecretsRuntime.active(p)) return;
        long now = p.serverLevel().getGameTime();
        var s = SecretTransactions.state(p);
        if (s == null) return;
        if (a.visit != s.visit || now - a.start > 1200) {
            ACTIVE.remove(p.getUUID());
            return;
        }

        if (a.box != null) {
            if (!WildernessLoot.lockable(p, a.box, a.tier + 1)) {
                ACTIVE.remove(p.getUUID());
                return;
            }
        } else {
            var rooms = SecretWorld.rooms(s);
            if (a.room < 0 || a.room >= rooms.size()) {
                ACTIVE.remove(p.getUUID());
                return;
            }
            var room = rooms.get(a.room);
            var at = BlockPos.of(room.getLong("Pos"));
            if (p.distanceToSqr(at.offset(0, 1, -5).getCenter()) > 36 ||
                    SecretRegistry.tier(s.value(0)) < room.getInt("Tier")) {
                ACTIVE.remove(p.getUUID());
                return;
            }
        }
        if (now - a.last < 6) return;

        double cursor = (Math.sin((now - a.start) * a.speed) + 1) / 2;
        boolean hit = Math.abs(cursor - .5) <= a.window / 2;
        int done = hit ? a.completed + 1 : Math.max(0, a.completed - 1);
        p.playNotifySound(hit ? net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME.get()
                        : net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS.get(),
                net.minecraft.sounds.SoundSource.BLOCKS, .45F, hit ? 1.3F : .7F);
        if (done >= a.pins) {
            ACTIVE.remove(p.getUUID());
            if (a.box != null) {
                WildernessLoot.unlock(p, a.box, a.tier + 1);
                // The pin minigame is a normal client Screen (not a container menu).
                // A server close packet dismisses it after the reward is granted.
                p.closeContainer();
            } else {
                unlock(p, a.room);
                SecretTransactions.dirty(p);
                SecretsNetwork.open(p, false, false);
            }
        } else {
            var next = new Attempt(a.nonce, a.room, a.box, a.tier, a.visit,
                    a.start, a.pins, done, a.speed, a.window, now);
            ACTIVE.put(p.getUUID(), next);
            send(p, next);
        }
    }

    public static void unlock(ServerPlayer p, int index) {
        var s = SecretTransactions.state(p);
        if (s == null) return;
        var rooms = SecretWorld.rooms(s);
        if (index < 0 || index >= rooms.size()) return;
        s.unlocked.add(index);
        var at = BlockPos.of(rooms.get(index).getLong("Pos"));
        for (int y = 0; y < 3; y++) SecretWorld.place(p, at.offset(0, y, -5), Blocks.AIR);
        MotherSearch.observed(p, "magical_locks", at);
        SecretTransactions.dirty(p);
    }

    public static void clear(UUID id) { ACTIVE.remove(id); }
    private SecretLocks() {}
}
