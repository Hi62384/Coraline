package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.Vec3;

/** Custom artificial inhabitants. Ordinary native navigation/damage is kept;
 * none of these are original story actors or replacements for a saved NPC. */
public final class OtherWorldHostile extends Monster {
    private int alertAfter;
    private int heavyAfter,chargedAt=-1000;
    private UUID chargedTarget;
    private final PostgameCombat combat;
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> CHARGING=net.minecraft.network.syncher.SynchedEntityData.defineId(OtherWorldHostile.class,net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> REVEALED=net.minecraft.network.syncher.SynchedEntityData.defineId(OtherWorldHostile.class,net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    public OtherWorldHostile(EntityType<? extends OtherWorldHostile> type,Level level){super(type,level);combat=new PostgameCombat(this);xpReward=kind()==PostgameHostiles.Kind.RIFT_GUARDIAN?35:kind()==PostgameHostiles.Kind.BUTTON_EYED_WANDERER?5:kind()==PostgameHostiles.Kind.PORCELAIN_SENTINEL?15:8;if(kind()==PostgameHostiles.Kind.PAPER_WRAITH){setNoGravity(true);moveControl=new net.minecraft.world.entity.ai.control.FlyingMoveControl(this,20,false);}}
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source){return switch(kind()){case THREADCRAWLER->net.minecraft.sounds.SoundEvents.SPIDER_HURT;case STITCHED_HOUND->net.minecraft.sounds.SoundEvents.WOLF_HURT;case PORCELAIN_SENTINEL->net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_HIT;default->super.getHurtSound(source);};}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return switch(kind()){case THREADCRAWLER->net.minecraft.sounds.SoundEvents.SPIDER_DEATH;case STITCHED_HOUND->net.minecraft.sounds.SoundEvents.WOLF_DEATH;case PORCELAIN_SENTINEL->net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_BREAK;default->super.getDeathSound();};}
    public PostgameHostiles.Kind kind(){return PostgameHostiles.kind(getType());}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(CHARGING,false);entityData.define(REVEALED,false);}
    public boolean charging(){return entityData.get(CHARGING);}
    void charging(boolean active){entityData.set(CHARGING,active);}
    public boolean revealed(){return entityData.get(REVEALED);}
    void reveal(){entityData.set(REVEALED,true);}
    @Override protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level){return kind()==PostgameHostiles.Kind.THREADCRAWLER?new net.minecraft.world.entity.ai.navigation.WallClimberNavigation(this,level):kind()==PostgameHostiles.Kind.PAPER_WRAITH?new net.minecraft.world.entity.ai.navigation.FlyingPathNavigation(this,level):super.createNavigation(level);}
    @Override public boolean onClimbable(){return kind()==PostgameHostiles.Kind.THREADCRAWLER?horizontalCollision:super.onClimbable();}
    public static AttributeSupplier.Builder attributes(PostgameHostiles.Kind kind){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,kind.health).add(Attributes.ATTACK_DAMAGE,kind.damage).add(Attributes.MOVEMENT_SPEED,kind.speed).add(Attributes.FLYING_SPEED,.45).add(Attributes.ARMOR,kind.armor).add(Attributes.FOLLOW_RANGE,kind==PostgameHostiles.Kind.CLOCKWORK_WATCHER?28:24);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        if(kind()==PostgameHostiles.Kind.RIFT_SOLDIER||kind()==PostgameHostiles.Kind.VOID_STALKER)goalSelector.addGoal(1,new FlankGoal(this));
        if(kind()==PostgameHostiles.Kind.STITCHED_HOUND)goalSelector.addGoal(1,new LeapAtTargetGoal(this,.35F));
        if(kind()==PostgameHostiles.Kind.PAPER_WRAITH)goalSelector.addGoal(1,new PostgameCombat.FlightGoal(this));
        if(kind()==PostgameHostiles.Kind.THREAD_SPINNER)goalSelector.addGoal(1,new PostgameCombat.ThreadGoal(this));
        goalSelector.addGoal(2,new MeleeAttackGoal(this,kind()==PostgameHostiles.Kind.CLOCKWORK_WATCHER?.9:1.1,false));
        goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,kind()==PostgameHostiles.Kind.CLOCKWORK_WATCHER?.65:.85));
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(7,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,10,true,false,p->kind()!=PostgameHostiles.Kind.FALSE_VILLAGER&&kind()!=PostgameHostiles.Kind.ROOTBOUND_HORROR||revealed()||distanceToSqr(p)<(kind()==PostgameHostiles.Kind.FALSE_VILLAGER?16:49)));
    }
    public static boolean naturalSpawn(EntityType<OtherWorldHostile> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos at,RandomSource random){
        if(!(level.getLevel().getChunkSource().getGenerator() instanceof InfiniteChunkGenerator generator))return false;
        InfiniteGeneration generation;try{generation=generator.structureGeneration();}catch(IllegalStateException unbound){return false;}
        if(reason==MobSpawnType.NATURAL||reason==MobSpawnType.CHUNK_GENERATION){var column=generation.terrain().column(at.getX(),at.getZ());if(at.getY()<column.surfaceY()-8){var kind=PostgameHostiles.kind(type);boolean allowed=switch(generation.terrain().caveEnvironment(at.getX(),at.getY(),at.getZ(),column)){
            case PORCELAIN->kind==PostgameHostiles.Kind.BUTTON_EYED_WANDERER||kind==PostgameHostiles.Kind.PORCELAIN_SENTINEL||kind==PostgameHostiles.Kind.THREADCRAWLER;
            case SOUL_GLASS->kind==PostgameHostiles.Kind.PAPER_WRAITH||kind==PostgameHostiles.Kind.PORCELAIN_SENTINEL||kind==PostgameHostiles.Kind.VOID_STALKER;
            case THREADROOT->kind==PostgameHostiles.Kind.THREADCRAWLER||kind==PostgameHostiles.Kind.THREAD_SPINNER||kind==PostgameHostiles.Kind.ROOTBOUND_HORROR;
            case CLOCKWORK->kind==PostgameHostiles.Kind.CLOCKWORK_WATCHER||kind==PostgameHostiles.Kind.RIFT_SOLDIER||kind==PostgameHostiles.Kind.THREADCRAWLER;
            case MIRROR->kind==PostgameHostiles.Kind.FALSE_VILLAGER||kind==PostgameHostiles.Kind.PAPER_WRAITH||kind==PostgameHostiles.Kind.THREAD_SPINNER;
            case WHITE_RIFT->kind==PostgameHostiles.Kind.VOID_STALKER||kind==PostgameHostiles.Kind.RIFT_GUARDIAN||kind==PostgameHostiles.Kind.RIFT_SOLDIER;
        };if(!allowed)return false;}}
        return Monster.checkMonsterSpawnRules(type,level,reason,at,random);
    }
    @Override public void aiStep(){
        super.aiStep();
        if(level().isClientSide||isNoAi())return;
        combat.tick();
        if(kind()==PostgameHostiles.Kind.PORCELAIN_SENTINEL){
            boolean preparing=chargedTarget!=null&&getTarget()!=null&&chargedTarget.equals(getTarget().getUUID())&&tickCount-chargedAt<20;
            entityData.set(CHARGING,preparing);if(preparing)getNavigation().stop();
        }
        boolean hound=kind()==PostgameHostiles.Kind.STITCHED_HOUND;
        if(!hound&&kind()!=PostgameHostiles.Kind.CLOCKWORK_WATCHER||tickCount<alertAfter||getTarget()==null||!getTarget().isAlive()||!hasLineOfSight(getTarget()))return;
        alertAfter=tickCount+100;
        int alerted=0;
        for(var ally:level().getEntitiesOfClass(OtherWorldHostile.class,getBoundingBox().inflate(hound?12:16),mob->mob!=this&&mob.isAlive()&&mob.getTarget()==null&&(!hound||mob.kind()==PostgameHostiles.Kind.STITCHED_HOUND))){
            if(alerted++>=8)break;ally.setTarget(getTarget());
        }
        level().playSound(null,blockPosition(),hound?net.minecraft.sounds.SoundEvents.WOLF_GROWL:net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL.get(),net.minecraft.sounds.SoundSource.HOSTILE,.55F,.65F);
    }
    @Override public void startSeenByPlayer(net.minecraft.server.level.ServerPlayer player){super.startSeenByPlayer(player);combat.startTracking(player);}
    @Override public void stopSeenByPlayer(net.minecraft.server.level.ServerPlayer player){super.stopSeenByPlayer(player);combat.stopTracking(player);}
    @Override public void remove(RemovalReason reason){if(combat!=null)combat.removed();super.remove(reason);}
    @Override public boolean doHurtTarget(Entity victim){
        if(kind()!=PostgameHostiles.Kind.PORCELAIN_SENTINEL)return super.doHurtTarget(victim);
        if(tickCount<heavyAfter)return false;
        if(!victim.getUUID().equals(chargedTarget)||tickCount-chargedAt>60){
            chargedTarget=victim.getUUID();chargedAt=tickCount;entityData.set(CHARGING,true);
            level().playSound(null,blockPosition(),net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE,net.minecraft.sounds.SoundSource.HOSTILE,.8F,.55F);return false;
        }
        if(tickCount-chargedAt<20)return false;chargedTarget=null;heavyAfter=tickCount+60;entityData.set(CHARGING,false);
        boolean hit=super.doHurtTarget(victim);if(hit&&victim instanceof LivingEntity living)living.knockback(.8,getX()-victim.getX(),getZ()-victim.getZ());return hit;
    }
    /** Soldiers take a short reachable flank between melee pursuits. A failed
     * path falls back to normal combat, never direct movement through walls. */
    private static final class FlankGoal extends Goal {
        private final OtherWorldHostile mob;private int next,until;private Vec3 waypoint;
        FlankGoal(OtherWorldHostile mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE));}
        @Override public boolean canUse(){
            var target=mob.getTarget();if(target==null||!target.isAlive()||mob.tickCount<next||!mob.hasLineOfSight(target))return false;
            double distance=mob.distanceToSqr(target);if(distance<16||distance>144)return false;
            if(mob.kind()==PostgameHostiles.Kind.VOID_STALKER&&target.getLookAngle().dot(mob.position().subtract(target.position()).normalize())<.35)return false;
            Vec3 delta=target.position().subtract(mob.position()).multiply(1,0,1);if(delta.lengthSqr()<.01)return false;
            var side=new Vec3(-delta.z,0,delta.x).normalize().scale(mob.getId()%2==0?3:-3);waypoint=target.position().add(side);
            next=mob.tickCount+60;var path=mob.getNavigation().createPath(BlockPos.containing(waypoint),0);return path!=null&&path.canReach();
        }
        @Override public void start(){until=mob.tickCount+24;mob.getNavigation().moveTo(waypoint.x,waypoint.y,waypoint.z,1.1);}
        @Override public boolean canContinueToUse(){return mob.tickCount<until&&mob.getTarget()!=null&&mob.getTarget().isAlive()&&!mob.getNavigation().isDone();}
        @Override public void stop(){mob.getNavigation().stop();}
    }
}
