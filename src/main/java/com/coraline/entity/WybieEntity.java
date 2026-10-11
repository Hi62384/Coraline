package com.coraline.entity;

import com.coraline.item.DollItem;
import com.coraline.registry.ModEntities;
import com.coraline.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import com.coraline.config.CoralineConfig;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class WybieEntity extends PathfinderMob {
    public static final int PHASE_APPROACH = 0;
    public static final int PHASE_LINE_A = 1;
    public static final int PHASE_LINE_B = 2;
    public static final int PHASE_LINE_C = 3;

    public static final int LINE_A_TICKS = 564;
    public static final int LINE_B_TICKS = 419;
    public static final int LINE_C_TICKS = 88;
    public static final int ENCOUNTER_COOLDOWN_TICKS = 1_400;

    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(WybieEntity.class, EntityDataSerializers.INT);

    private UUID playerId;
    private int phaseTicks;
    private boolean dollGiven;
    private Vec3 departureCheckpoint = Vec3.ZERO;

    public WybieEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.xpReward = 0;
        this.setMaxUpStep(1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.42D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.35D);
    }

    public static boolean spawnBehind(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        WybieEntity wybie = ModEntities.WYBIE.get().create(level);
        if (wybie == null) {
            return false;
        }

        Vec3 horizontalLook = new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z);
        if (horizontalLook.lengthSqr() < 0.001D) {
            horizontalLook = new Vec3(0.0D, 0.0D, 1.0D);
        }

        Vec3 wanted = player.position().subtract(horizontalLook.normalize().scale(9.0D));
        BlockPos spawnPos = findSafeSpawn(level, wybie, BlockPos.containing(wanted.x, player.getY(), wanted.z));
        if (spawnPos == null) return false;
        wybie.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                player.getYRot(), 0.0F);
        wybie.playerId = player.getUUID();

        if (!level.addFreshEntity(wybie)) {
            return false;
        }

        wybie.ringBell();
        return true;
    }

    static BlockPos findSafeSpawn(ServerLevel level, WybieEntity entity, BlockPos center) {
        for (int radius = 0; radius <= 8; radius++) {
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) != radius) continue;
                for (int y = 3; y >= -4; y--) {
                    BlockPos feet = center.offset(x, y, z);
                    if (!level.hasChunkAt(feet) || !level.getWorldBorder().isWithinBounds(feet)) continue;
                    entity.setPos(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
                    if (level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), net.minecraft.core.Direction.UP)
                            && level.noCollision(entity, entity.getBoundingBox().inflate(0.1, 0, 0.1))
                            && !level.containsAnyLiquid(entity.getBoundingBox())) return feet;
                }
            }
        }
        return null;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
        this.goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this) {
            @Override public boolean canUse() { return playerId == null && super.canUse(); }
            @Override public boolean canContinueToUse() { return playerId == null && super.canContinueToUse(); }
        });
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PHASE, PHASE_APPROACH);
    }

    public int getEncounterPhase() {
        return this.entityData.get(DATA_PHASE);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || !this.isAlive()) {
            return;
        }

        ServerPlayer player = getEncounterPlayer();
        if (this.playerId == null) return; // Spawn eggs create ordinary persistent NPCs.
        if (player == null || !player.isAlive() || player.level() != this.level()) {
            this.discard();
            return;
        }

        switch (getEncounterPhase()) {
            case PHASE_APPROACH -> tickApproach(player);
            case PHASE_LINE_A -> tickLineA(player);
            case PHASE_LINE_B -> tickLineB(player);
            case PHASE_LINE_C -> tickDeparture(player);
            default -> this.discard();
        }
        this.phaseTicks++;
    }

    private void tickApproach(ServerPlayer player) {
        // Let MoveControl steer the bicycle along its path, rather than fighting it by
        // snapping the body toward the player every tick (especially around corners).
        this.getLookControl().setLookAt(player, 20.0F, 20.0F);
        if (this.phaseTicks % CoralineConfig.WYBIE_REPATH.get() == 0)
            this.getNavigation().moveTo(player, CoralineConfig.WYBIE_APPROACH_SPEED.get());

        if (this.phaseTicks == 8) {
            ringBell();
        }

        if (this.distanceToSqr(player) <= 3.6D && this.hasLineOfSight(player)) {
            runOver(player);
        }
        if (this.phaseTicks >= CoralineConfig.WYBIE_APPROACH_TIMEOUT.get()) this.discard();
    }

    private void runOver(ServerPlayer player) {
        this.getNavigation().stop();
        player.hurt(this.damageSources().mobAttack(this), com.coraline.config.CoralineConfig.IMPACT_DAMAGE.get().floatValue());
        player.knockback(1.65D, this.getX() - player.getX(), this.getZ() - player.getZ());
        setPhase(PHASE_LINE_A);
        speak(ModSounds.WYBIE_LINE_A.get());
    }

    private void tickLineA(ServerPlayer player) {
        followAndWatch(player);
        if (this.phaseTicks >= LINE_A_TICKS) {
            setPhase(PHASE_LINE_B);
            speak(ModSounds.WYBIE_LINE_B.get());
        }
    }

    private void tickLineB(ServerPlayer player) {
        followAndWatch(player);

        if (!this.dollGiven && this.phaseTicks >= 20) {
            giveDoll(player);
            this.dollGiven = true;
        }

        if (this.phaseTicks >= LINE_B_TICKS) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, com.coraline.config.CoralineConfig.POISON_TICKS.get(), 0));
            beginDeparture(player);
        }
    }

    private void tickDeparture(ServerPlayer player) {
        if (phaseTicks % 20 == 0) {
            boolean stuck = position().distanceToSqr(departureCheckpoint) < 0.25D;
            departureCheckpoint = position();
            if (getNavigation().isDone() || stuck) {
                Path path = findEscapePath(player.position());
                if (path != null) getNavigation().moveTo(path, CoralineConfig.WYBIE_DEPARTURE_SPEED.get());
            }
        }

        if (this.phaseTicks >= LINE_C_TICKS) {
            this.discard();
        }
    }

    // Reachable endpoints farther from the player; navigation can initially turn sideways
    // or toward a gap to get around an obstacle. No direct velocity or duplicate move().
    Path findEscapePath(Vec3 threat) {
        Path best = null;
        double bestScore = -Double.MAX_VALUE;
        for (int attempt = 0; attempt < 16; attempt++) {
            Vec3 target = attempt < 10 ? DefaultRandomPos.getPosAway(this, 16, 5, threat)
                    : DefaultRandomPos.getPos(this, 12, 4);
            if (target == null || target.distanceToSqr(threat) < position().distanceToSqr(threat) + 4) continue;
            Path path = getNavigation().createPath(BlockPos.containing(target), 0);
            if (path == null || !path.canReach()) continue;
            double score = target.distanceToSqr(threat) - path.getNodeCount() * 0.5;
            if (score > bestScore) { bestScore = score; best = path; }
        }
        return best;
    }

    private void followAndWatch(ServerPlayer player) {
        watch(player);
        double distance = this.distanceTo(player);
        if (distance > 3.5D && phaseTicks % CoralineConfig.WYBIE_REPATH.get() == 0) {
            this.getNavigation().moveTo(player, 0.72D);
        } else if (distance < 2.75D) {
            this.getNavigation().stop();
        }
    }

    private void watch(ServerPlayer player) {
        this.getLookControl().setLookAt(player, 35.0F, 35.0F);
        double dx = player.getX() - this.getX();
        double dz = player.getZ() - this.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        if (getNavigation().isDone()) {
            this.setYRot(Mth.approachDegrees(this.getYRot(), yaw, 10.0F));
            this.yBodyRot = this.getYRot();
        }
    }

    private void giveDoll(ServerPlayer player) {
        ItemStack doll = com.coraline.doll.DollBondData.get(player.server).claim(player);
        if (doll.isEmpty()) return;
        if (!player.getInventory().add(doll)) {
            player.drop(doll, false);
        }
    }

    private void beginDeparture(ServerPlayer player) {
        this.getNavigation().stop();
        this.departureCheckpoint = this.position();
        setPhase(PHASE_LINE_C);
        Path path = findEscapePath(player.position());
        if (path != null) getNavigation().moveTo(path, CoralineConfig.WYBIE_DEPARTURE_SPEED.get());
        speak(ModSounds.WYBIE_LINE_C.get());
    }

    private void setPhase(int phase) {
        this.entityData.set(DATA_PHASE, phase);
        this.phaseTicks = 0;
    }

    private void ringBell() {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.4F, 1.65F);
    }

    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return SoundEvents.PLAYER_DEATH; }

    private void speak(net.minecraft.sounds.SoundEvent sound) {
        this.level().playSound(null, this, sound, SoundSource.NEUTRAL, 2.0F, 1.0F);
    }

    private ServerPlayer getEncounterPlayer() {
        if (this.playerId == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        net.minecraft.world.entity.player.Player player = serverLevel.getPlayerByUUID(this.playerId);
        return player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    @Override
    public boolean shouldBeSaved() {
        return this.playerId == null && super.shouldBeSaved();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.playerId != null) {
            tag.putUUID("EncounterPlayer", this.playerId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("EncounterPlayer")) {
            this.playerId = tag.getUUID("EncounterPlayer");
        }
    }
}
