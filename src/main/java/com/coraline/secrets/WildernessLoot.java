package com.coraline.secrets;

import com.coraline.feast.OtherMotherLoop;
import com.coraline.otherworld.OtherWorldEstate;
import com.coraline.otherworld.RealmData;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Four tiers of visit-specific wilderness loot boxes. Boxes are generated lazily
 * on already copied terrain, not by eagerly loading the whole 500-block realm.
 * The existing SecretWorld block ledger restores all boxes next visit.
 * Tier 1 requires no Code Knowledge; tiers 2-4 use the existing lock GUI.
 */
@Mod.EventBusSubscriber(modid = "coraline")
public final class WildernessLoot {
    private static final int CELL = 44;
    private static final int SEARCH_RADIUS = 2;
    private static final int MAX_BOXES_PER_VISIT = 360;
    private static final String CELLS = "WildernessLootCells";
    private static final String BOXES = "WildernessLootBoxes";
    private static final String COUNT = "WildernessLootCount";
    private static final String FORMAT = "WildernessLootTierFormat";
    private static final String CLAIM_PREFIX = "wilderness_box:";
    private static final double[] SECRET_CHANCE = {0.06, 0.12, 0.20, 0.32};

    /**
     * The previous scattered-box patch stored zero-based grades 0-3. Existing
     * visits may already contain those boxes; preserve their placements and
     * opened status while migrating to the new one-based tiers 1-4.
     */
    private static void ensureTierFormat(ServerPlayer player, SecretsState state) {
        if (state.world.getInt(FORMAT) >= 2) return;
        CompoundTag old = state.world.getCompound(BOXES);
        for (String key : old.getAllKeys()) {
            int grade = old.getInt(key);
            old.putInt(key, Math.max(1, Math.min(4, grade + 1)));
        }
        state.world.put(BOXES, old);
        state.world.putInt(FORMAT, 2);
        SecretTransactions.dirty(player);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 20 != 0 || !SecretsRuntime.active(player)) return;
        var scene = OtherMotherLoop.scene(player);
        var state = SecretTransactions.state(player);
        RealmData.Realm realm = SecretWorld.realm(player);
        if (scene == null || state == null || realm == null || realm.house == null || realm.sourceDoor == null
                || !realm.ready || state.visit != scene.loop.visits) return;
        ensureTierFormat(player, state);
        if (state.world.getInt(COUNT) >= MAX_BOXES_PER_VISIT) return;

        CompoundTag visited = state.world.getCompound(CELLS);
        CompoundTag boxes = state.world.getCompound(BOXES);
        int cellX = Math.floorDiv(player.getBlockX() - realm.house.getX(), CELL);
        int cellZ = Math.floorDiv(player.getBlockZ() - realm.house.getZ(), CELL);
        int processed = 0;
        boolean changed = false;

        for (int ring = 0; ring <= SEARCH_RADIUS && processed < 10; ring++) {
            for (int dx = -ring; dx <= ring && processed < 10; dx++) {
                for (int dz = -ring; dz <= ring && processed < 10; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    int x = cellX + dx, z = cellZ + dz;
                    String key = x + ":" + z;
                    if (visited.contains(key)) continue;
                    // Uncopied terrain remains eligible for a later visit to the cell.
                    int result = populateCell(player, realm, state, boxes, x, z);
                    if (result < 0) continue;
                    visited.putBoolean(key, true);
                    processed++;
                    changed = true;
                }
            }
        }
        if (changed) {
            state.world.put(CELLS, visited);
            state.world.put(BOXES, boxes);
            SecretTransactions.dirty(player);
        }
    }

    /** -1 = wait for copied terrain, 0 = no suitable spawn, positive = placed. */
    private static int populateCell(ServerPlayer player, RealmData.Realm realm, SecretsState state,
                                    CompoundTag boxes, int cellX, int cellZ) {
        Random random = new Random(state.seed ^ ((long) cellX * 0x9E3779B97F4A7C15L)
                ^ ((long) cellZ * 0xC2B2AE3D27D4EB4FL) ^ 0x5126A9B4CL);
        if (random.nextDouble() >= 0.90) return 0;

        boolean waitingForCopy = false;
        int added = 0;
        for (int attempt = 0; attempt < 6; attempt++) {
            if (state.world.getInt(COUNT) >= MAX_BOXES_PER_VISIT) break;
            int x = realm.house.getX() + cellX * CELL + 5 + random.nextInt(CELL - 10);
            int z = realm.house.getZ() + cellZ * CELL + 5 + random.nextInt(CELL - 10);
            BlockPos sample = new BlockPos(x, realm.house.getY(), z);
            double distance = OtherWorldEstate.outsideDistance(realm.house, sample);
            if (distance < 64) continue;
            double ax = x - realm.sourceDoor.getX() - realm.dx;
            double az = z - realm.sourceDoor.getZ() - realm.dz;
            if (ax * ax + az * az > (realm.radius - 20.0) * (realm.radius - 20.0)) continue;
            if (!player.serverLevel().hasChunkAt(sample) || !SecretWorld.terrainCopied(realm, sample)) {
                waitingForCopy = true;
                continue;
            }
            int y = SecretWorld.ground(player.serverLevel(), x, z);
            if (y == Integer.MIN_VALUE) continue;
            BlockPos at = new BlockPos(x, y, z);
            if (!suitable(player, state, at, boxes)) continue;

            int tier = tierForDistance(random, distance, realm.radius);
            SecretWorld.place(player, at, blockFor(tier));
            boxes.putInt(Long.toString(at.asLong()), tier);
            state.world.putInt(COUNT, state.world.getInt(COUNT) + 1);
            added++;
            // Occasional two-box stashes, but not placed adjacent to each other.
            if (added == 1 && random.nextDouble() < 0.20) continue;
            break;
        }
        return added > 0 ? added : waitingForCopy ? -1 : 0;
    }

    private static boolean suitable(ServerPlayer player, SecretsState state, BlockPos at, CompoundTag boxes) {
        var level = player.serverLevel();
        if (!level.isEmptyBlock(at) || !level.isEmptyBlock(at.above())) return false;
        for (var room : SecretWorld.rooms(state)) {
            BlockPos center = BlockPos.of(room.getLong("Pos"));
            if (Math.abs(at.getX() - center.getX()) < 19
                    && Math.abs(at.getZ() - center.getZ()) < 21) return false;
        }
        // Don't make boxes physically overlap or crowd a small area.
        for (String key : boxes.getAllKeys()) {
            BlockPos other;
            try { other = BlockPos.of(Long.parseLong(key)); }
            catch (NumberFormatException invalid) { continue; }
            if (Math.abs(other.getX() - at.getX()) < 7 && Math.abs(other.getZ() - at.getZ()) < 7)
                return false;
        }
        // Conservative terrain slope: no cliff-edge or floating loot boxes.
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            int ground = SecretWorld.ground(level, at.getX() + dx, at.getZ() + dz);
            if (ground == Integer.MIN_VALUE || Math.abs(ground - at.getY()) > 1) return false;
        }
        return true;
    }

    /** Distance-weighted: outer regions strongly favor higher tier boxes. */
    private static int tierForDistance(Random random, double distance, int realmRadius) {
        double fraction = Math.max(0, Math.min(1,
                (distance - 64) / Math.max(1.0, realmRadius - 64)));
        double roll = random.nextDouble();
        if (fraction < .18) return roll < .90 ? 1 : 2;
        if (fraction < .43) return roll < .17 ? 1 : roll < .90 ? 2 : 3;
        if (fraction < .69) return roll < .10 ? 1 : roll < .32 ? 2 : roll < .91 ? 3 : 4;
        return roll < .04 ? 1 : roll < .12 ? 2 : roll < .38 ? 3 : 4;
    }

    private static Block blockFor(int tier) {
        return switch (tier) {
            case 2 -> Blocks.CHEST;
            case 3 -> Blocks.CYAN_SHULKER_BOX;
            case 4 -> Blocks.PURPLE_SHULKER_BOX;
            default -> Blocks.BARREL;
        };
    }

    private static String label(int tier) {
        return switch (tier) {
            case 2 -> "Tier 2 treasure chest";
            case 3 -> "Tier 3 rare loot box";
            case 4 -> "Tier 4 enchanted loot box";
            default -> "Tier 1 supply crate";
        };
    }

    /** Called from the shared pin minigame on each pin and before loot payout. */
    public static boolean lockable(ServerPlayer player, BlockPos at, int tier) {
        if (tier < 2 || tier > 4 || !SecretsRuntime.active(player)) return false;
        var state = SecretTransactions.state(player);
        if (state == null) return false;
        ensureTierFormat(player, state);
        String key = Long.toString(at.asLong());
        return state.world.getCompound(BOXES).contains(key)
                && state.world.getCompound(BOXES).getInt(key) == tier
                && !state.claimed.contains(CLAIM_PREFIX + key)
                && player.serverLevel().getBlockState(at).is(blockFor(tier))
                && player.distanceToSqr(at.getCenter()) <= 36
                && SecretRegistry.tier(state.value(0)) >= tier - 1;
    }

    /** Successful server-validated lockpick: immediately open that specific box. */
    public static void unlock(ServerPlayer player, BlockPos at, int tier) {
        if (lockable(player, at, tier)) collect(player, at, tier);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interact(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !SecretsRuntime.active(player)) return;
        SecretsState state = SecretTransactions.state(player);
        if (state == null) return;
        ensureTierFormat(player, state);
        BlockPos at = event.getPos();
        String key = Long.toString(at.asLong());
        CompoundTag boxes = state.world.getCompound(BOXES);
        if (!boxes.contains(key)) return;
        int tier = boxes.getInt(key);
        if (tier < 1 || tier > 4 || !player.serverLevel().getBlockState(at).is(blockFor(tier))) return;
        // Cancel vanilla container access for both hands, even with insufficient
        // knowledge. The offhand must not open a second lock screen or loot roll.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() != InteractionHand.MAIN_HAND || player.distanceToSqr(at.getCenter()) > 36) return;
        if (state.claimed.contains(CLAIM_PREFIX + key)) return;
        if (tier == 1) collect(player, at, tier);
        else SecretLocks.openLootBox(player, at, tier);
    }

    private static void collect(ServerPlayer player, BlockPos at, int tier) {
        var state = SecretTransactions.state(player);
        if (state == null || !SecretsRuntime.active(player)) return;
        ensureTierFormat(player, state);
        String key = Long.toString(at.asLong());
        if (state.world.getCompound(BOXES).getInt(key) != tier
                || !player.serverLevel().getBlockState(at).is(blockFor(tier))
                || player.distanceToSqr(at.getCenter()) > 36
                || SecretRegistry.tier(state.value(0)) < tier - 1) return;
        if (!state.claimed.add(CLAIM_PREFIX + key)) return;

        Random random = new Random(player.getRandom().nextLong());
        int rolls = switch (tier) {
            case 2 -> 2 + random.nextInt(2);
            case 3 -> 3;
            case 4 -> 3 + random.nextInt(2);
            default -> 1 + random.nextInt(2);
        };
        List<ItemStack> rewards = new ArrayList<>();
        for (int i = 0; i < rolls; i++) rewards.add(loot(tier, random));
        String secretId = random.nextDouble() < SECRET_CHANCE[tier - 1]
                ? chooseSecret(state, tier, random) : null;

        // Save the one-time claim before sending any inventory items or evidence.
        SecretWorld.place(player, at, Blocks.AIR);
        SecretTransactions.checkpoint(player);
        for (ItemStack reward : rewards) SecretTransactions.give(player, reward);
        if (secretId != null) {
            SecretTransactions.issue(player, secretId);
            player.displayClientMessage(Component.literal("Secret evidence found! File it in your bedroom journal."), true);
        } else {
            player.displayClientMessage(Component.literal(label(tier) + " opened — loot collected!"), true);
        }
        player.playNotifySound(SoundEvents.CHEST_OPEN, SoundSource.BLOCKS,
                .8F, .95F + random.nextFloat() * .15F);
        player.serverLevel().sendParticles(tier >= 3 ? ParticleTypes.END_ROD : ParticleTypes.HAPPY_VILLAGER,
                at.getX() + .5, at.getY() + .6, at.getZ() + .5,
                tier >= 3 ? 12 : 6, .25, .20, .25, .025);
        SecretTransactions.dirty(player);
    }

    /** Draw an unfiled, unissued registered Secret Item suitable for this tier. */
    private static String chooseSecret(SecretsState state, int boxTier, Random random) {
        int maxTier = Math.min(2, boxTier - 1);
        int preferredTier = switch (boxTier) {
            case 2 -> random.nextDouble() < .35 ? 1 : 0;
            case 3 -> random.nextDouble() < .65 ? 2 : 1;
            case 4 -> random.nextDouble() < .85 ? 2 : 1;
            default -> 0;
        };
        List<String> preferred = new ArrayList<>();
        List<String> fallback = new ArrayList<>();
        for (var secret : SecretRegistry.ALL.values()) {
            String id = secret.id();
            if (secret.tier() > maxTier || state.journal.contains(id)
                    || state.issued.containsValue(id)) continue;
            fallback.add(id);
            if (secret.tier() == preferredTier) preferred.add(id);
        }
        var pool = !preferred.isEmpty() ? preferred : fallback;
        return pool.isEmpty() ? null : pool.get(random.nextInt(pool.size()));
    }

    private static ItemStack loot(int tier, Random random) {
        int roll = random.nextInt(6);
        if (tier == 1) return switch (roll) {
            case 0 -> stack(Items.BREAD, 2 + random.nextInt(4));
            case 1 -> stack(Items.IRON_INGOT, 1 + random.nextInt(3));
            case 2 -> stack(Items.COAL, 3 + random.nextInt(6));
            case 3 -> stack(Items.TORCH, 4 + random.nextInt(9));
            case 4 -> stack(Items.REDSTONE, 3 + random.nextInt(6));
            default -> stack(Items.COOKED_BEEF, 1 + random.nextInt(3));
        };
        if (tier == 2) return switch (roll) {
            case 0 -> stack(Items.IRON_INGOT, 2 + random.nextInt(4));
            case 1 -> stack(Items.GOLD_INGOT, 1 + random.nextInt(4));
            case 2 -> stack(Items.EMERALD, 1 + random.nextInt(3));
            case 3 -> stack(Items.LAPIS_LAZULI, 4 + random.nextInt(6));
            case 4 -> stack(Items.EXPERIENCE_BOTTLE, 2 + random.nextInt(4));
            default -> stack(Items.GOLDEN_CARROT, 2 + random.nextInt(4));
        };
        if (tier == 3) return switch (roll) {
            case 0 -> stack(Items.DIAMOND, 1 + random.nextInt(2));
            case 1 -> stack(Items.EMERALD, 2 + random.nextInt(5));
            case 2 -> stack(Items.GOLD_INGOT, 4 + random.nextInt(5));
            case 3 -> stack(com.coraline.clothing.OtherMaterials.OTHER_IRON.get(), 1 + random.nextInt(2));
            case 4 -> stack(Items.EXPERIENCE_BOTTLE, 4 + random.nextInt(5));
            default -> stack(Items.GOLDEN_APPLE, 1);
        };
        return switch (roll) {
            case 0 -> stack(com.coraline.clothing.OtherMaterials.OTHER_DIAMOND.get(), 1);
            case 1 -> stack(com.coraline.clothing.OtherMaterials.MOONSTONE.get(), 1);
            case 2 -> stack(Items.DIAMOND, 2 + random.nextInt(3));
            case 3 -> stack(Items.EMERALD, 5 + random.nextInt(5));
            case 4 -> stack(com.coraline.clothing.OtherMaterials.OTHER_IRON.get(), 2 + random.nextInt(3));
            default -> stack(Items.GOLDEN_APPLE, 2);
        };
    }

    private static ItemStack stack(Item item, int count) { return new ItemStack(item, count); }
    private WildernessLoot() {}
}
