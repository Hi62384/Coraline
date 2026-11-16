package com.coraline.postgame;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** The central manifestation has its own identity and victory journal. It is
 * never substituted for the Overworld Avatar or an ordinary chamber defense. */
public final class TrueHeartEntity extends Monster implements VoidManifestation {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> CAST=net.minecraft.network.syncher.SynchedEntityData.defineId(TrueHeartEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(CAST,0);}
    private UUID seal;private int volley,warning=-1;private final net.minecraft.server.level.ServerBossEvent boss=new net.minecraft.server.level.ServerBossEvent(net.minecraft.network.chat.Component.translatable("entity.coraline.true_heart"),net.minecraft.world.BossEvent.BossBarColor.WHITE,net.minecraft.world.BossEvent.BossBarOverlay.NOTCHED_10);
    public TrueHeartEntity(EntityType<? extends TrueHeartEntity> type,Level level){super(type,level);setPersistenceRequired();setNoGravity(true);moveControl=new net.minecraft.world.entity.ai.control.FlyingMoveControl(this,20,false);xpReward=0;}
    public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,600).add(Attributes.ATTACK_DAMAGE,10).add(Attributes.MOVEMENT_SPEED,.19).add(Attributes.FLYING_SPEED,.26).add(Attributes.ARMOR,14).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.FOLLOW_RANGE,64);}
    void configure(UUID seal){this.seal=seal;setHealth(getMaxHealth());}UUID seal(){return seal;}
    @Override protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level){return new net.minecraft.world.entity.ai.navigation.FlyingPathNavigation(this,level);}
    @Override protected void registerGoals(){goalSelector.addGoal(1,new MeleeAttackGoal(this,1,true));goalSelector.addGoal(5,new LookAtPlayerGoal(this,Player.class,64));targetSelector.addGoal(1,new NearestAttackableTargetGoal<>(this,Player.class,true));}
    @Override public int phase(){return getHealth()>400?1:getHealth()>200?2:3;}@Override public int cast(){return entityData.get(CAST);}
    @Override public void move(MoverType type,net.minecraft.world.phys.Vec3 delta){if(!level().isClientSide&&seal!=null)delta=new net.minecraft.world.phys.Vec3(Math.max(-28,Math.min(28,getX()+delta.x))-getX(),delta.y,Math.max(-28,Math.min(28,getZ()+delta.z))-getZ());super.move(type,delta);}
    @Override public boolean isPushable(){return false;}@Override public boolean canChangeDimensions(){return false;}@Override protected boolean shouldDespawnInPeaceful(){return false;}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){return !level().isClientSide&&TrueHeartBattle.fighting(this)&&source.getEntity() instanceof ServerPlayer p&&p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&p.level()==level()&&super.hurt(source,amount);}
    @Override public boolean doHurtTarget(Entity target){return TrueHeartBattle.fighting(this)&&warning<0&&super.doHurtTarget(target);}
    @Override public void aiStep(){super.aiStep();if(level().isClientSide)return;if(!TrueHeartBattle.owns(this)){discard();return;}boolean active=TrueHeartBattle.fighting(this);setNoAi(!active);boss.setVisible(active);boss.setProgress(Math.max(0,getHealth()/getMaxHealth()));if(!active)return;var target=getTarget();if(target==null||!target.isAlive()||target.isSpectator()||target instanceof Player p&&p.isCreative())return;
        if(getY()<81)setPos(getX(),81,getZ());if(getY()>90)setPos(getX(),90,getZ());if(Math.abs(getX())>28||Math.abs(getZ())>28)getNavigation().moveTo(0,83,0,1);
        if(warning>=0){getNavigation().stop();if(tickCount-warning>=24){warning=-1;entityData.set(CAST,0);var server=(net.minecraft.server.level.ServerLevel)level();server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,getX(),81,getZ(),50,6,.2,6,.08);for(var player:level().getEntitiesOfClass(ServerPlayer.class,getBoundingBox().inflate(8,2,8),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&p.onGround()&&hasLineOfSight(p)))if(player.hurt(damageSources().mobAttack(this),8+phase()*2))player.knockback(.8,getX()-player.getX(),getZ()-player.getZ());}return;}
        if(tickCount>=volley){volley=tickCount+100-phase()*15;if(distanceToSqr(target)<100){warning=tickCount;entityData.set(CAST,2);level().playSound(null,blockPosition(),net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE,net.minecraft.sounds.SoundSource.HOSTILE,1,.6F);((net.minecraft.server.level.ServerLevel)level()).sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,getX(),81,getZ(),24,5,.1,5,0);}else if(hasLineOfSight(target)){var direction=target.getEyePosition().subtract(getX(),getY()+3,getZ()).normalize();var bolt=new net.minecraft.world.entity.projectile.SmallFireball(level(),this,direction.x,direction.y,direction.z);bolt.setPos(getX(),getY()+3,getZ());level().addFreshEntity(bolt);}}
    }
    @Override public void die(net.minecraft.world.damagesource.DamageSource source){if(!level().isClientSide)TrueHeartBattle.defeated(this,source);super.die(source);}
    @Override public void startSeenByPlayer(ServerPlayer player){super.startSeenByPlayer(player);boss.addPlayer(player);}@Override public void stopSeenByPlayer(ServerPlayer player){super.stopSeenByPlayer(player);boss.removePlayer(player);}@Override public void remove(RemovalReason reason){boss.removeAllPlayers();super.remove(reason);}
    @Override public void addAdditionalSaveData(CompoundTag n){super.addAdditionalSaveData(n);if(seal!=null)n.putUUID("HeartSeal",seal);}@Override public void readAdditionalSaveData(CompoundTag n){super.readAdditionalSaveData(n);seal=n.hasUUID("HeartSeal")?n.getUUID("HeartSeal"):null;volley=tickCount+60;warning=-1;}
}
