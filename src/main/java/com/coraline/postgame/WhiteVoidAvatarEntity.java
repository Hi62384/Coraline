package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** One Overworld manifestation, not the true entity or a complete-reset boss.
 * Its movement stays inside its leased native entity-storage chunk, making
 * missing-entity recovery distinguishable from an unloaded wandering boss. */
public final class WhiteVoidAvatarEntity extends Monster implements VoidManifestation {
    private static final EntityDataAccessor<Integer> PHASE=SynchedEntityData.defineId(WhiteVoidAvatarEntity.class,EntityDataSerializers.INT),CAST=SynchedEntityData.defineId(WhiteVoidAvatarEntity.class,EntityDataSerializers.INT);
    private UUID generation,seal;private BlockPos home=BlockPos.ZERO;private int nextBolt,nextSlam,nextSummon,nextShift,slamAt=-1,castUntil;
    private final ServerBossEvent boss=new ServerBossEvent(net.minecraft.network.chat.Component.translatable("entity.coraline.white_void_avatar"),net.minecraft.world.BossEvent.BossBarColor.WHITE,net.minecraft.world.BossEvent.BossBarOverlay.NOTCHED_10);
    public WhiteVoidAvatarEntity(EntityType<? extends WhiteVoidAvatarEntity> type,Level level){super(type,level);setPersistenceRequired();setMaxUpStep(2);setHealth(getMaxHealth());xpReward=150;}
    public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,360).add(Attributes.ATTACK_DAMAGE,8).add(Attributes.MOVEMENT_SPEED,.2).add(Attributes.ARMOR,12).add(Attributes.ARMOR_TOUGHNESS,4).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.FOLLOW_RANGE,80);}
    void configure(UUID generation,UUID seal,BlockPos home){if(this.generation!=null)return;this.generation=generation;this.seal=seal;this.home=home.immutable();moveTo(home.getX()+.5,home.getY(),home.getZ()+.5,0,0);}
    UUID generation(){return generation;}UUID seal(){return seal;}BlockPos home(){return home;}
    public int phase(){return entityData.get(PHASE);}public int cast(){return entityData.get(CAST);}
    boolean authentic(){return !level().isClientSide&&BreachWorldData.get(getServer()).authentic(this);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(PHASE,1);entityData.define(CAST,0);}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(2,new MeleeAttackGoal(this,1,false));goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,48));targetSelector.addGoal(1,new HurtByTargetGoal(this));targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));}
    @Override public boolean isPushable(){return false;}
    @Override public boolean canChangeDimensions(){return false;}
    @Override protected boolean shouldDespawnInPeaceful(){return false;}
    @Override public void move(MoverType type,Vec3 delta){
        if(!level().isClientSide&&generation!=null){double x=Math.max(home.getX()-4.5,Math.min(home.getX()+5.5,getX()+delta.x)),z=Math.max(home.getZ()-4.5,Math.min(home.getZ()+5.5,getZ()+delta.z));delta=new Vec3(x-getX(),delta.y,z-getZ());}
        super.move(type,delta);
    }
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){return !level().isClientSide&&authentic()&&super.hurt(source,amount);}
    @Override public boolean doHurtTarget(Entity target){return authentic()&&cast()==0&&super.doHurtTarget(target);}
    @Override public void aiStep(){
        super.aiStep();if(level().isClientSide)return;var data=BreachWorldData.get(getServer());
        if(!data.authentic(this)){if(data.defeated(this)&&isDeadOrDying())return;if(data.view().blocker().isEmpty()){((ServerLevel)level()).sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,getX(),getY()+2,getZ(),30,1,2,1,.03);discard();}return;}
        int phase=getHealth()>getMaxHealth()*2/3?1:getHealth()>getMaxHealth()/3?2:3;entityData.set(PHASE,phase);boss.setProgress(Math.max(0,getHealth()/getMaxHealth()));boss.setVisible(isAlive());
        if(tickCount>=castUntil)entityData.set(CAST,0);if(tickCount%100==0)data.rememberAvatar(this);
        var target=getTarget();if(target==null||!target.isAlive()||target.isSpectator()||target instanceof Player p&&p.isCreative())return;
        getLookControl().setLookAt(target,30,30);
        if(slamAt>=0){
            getNavigation().stop();if(tickCount>=slamAt){slamAt=-1;
                for(var player:level().getEntitiesOfClass(Player.class,getBoundingBox().inflate(5,1,5),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&p.onGround()&&hasLineOfSight(p)))if(player.hurt(damageSources().mobAttack(this),8+phase*2))player.knockback(1,getX()-player.getX(),getZ()-player.getZ());
                ((ServerLevel)level()).sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,getX(),getY()+.1,getZ(),45,3,.1,3,.06);hazard(position().add(2,0,0));hazard(position().add(-2,0,0));
            }return;
        }else if(tickCount>=nextSlam&&distanceToSqr(target)<64){nextSlam=tickCount+140-phase*20;slamAt=tickCount+24;cast(2,24);return;}
        if(tickCount<castUntil)return;
        if(tickCount>=nextBolt&&hasLineOfSight(target)&&distanceToSqr(target)<6400){
            nextBolt=tickCount+100-phase*15;cast(1,15);var bolt=com.coraline.registry.ModEntities.VOID_BOLT.get().create(level());
            if(bolt!=null){bolt.configure(this);bolt.setPos(getX(),getY()+3.5,getZ());var direction=target.getEyePosition().subtract(bolt.position()).normalize();bolt.shoot(direction.x,direction.y,direction.z,.65F+phase*.07F,1);level().addFreshEntity(bolt);}return;
        }
        if(tickCount>=nextSummon){nextSummon=tickCount+360-phase*40;cast(3,25);BreachAvatarDirector.summon(this,phase>=2?3:2);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new AvatarInfectionPulse((ServerLevel)level(),this,phase));return;}
        if(phase>=2&&tickCount>=nextShift){nextShift=tickCount+240-phase*30;shift();}
    }
    private void cast(int kind,int duration){entityData.set(CAST,kind);castUntil=tickCount+duration;level().playSound(null,blockPosition(),net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE,net.minecraft.sounds.SoundSource.HOSTILE,1,.5F+kind*.08F);}
    void hazard(Vec3 at){var hazard=com.coraline.registry.ModEntities.VOID_HAZARD.get().create(level());if(hazard!=null){hazard.configure(this);hazard.setPos(at);level().addFreshEntity(hazard);}}
    private void shift(){
        for(int i=0;i<8;i++){double angle=i*Math.PI/4;var at=home.offset((int)Math.round(Math.cos(angle)*4),0,(int)Math.round(Math.sin(angle)*4));
            int y=((ServerLevel)level()).getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,at.getX(),at.getZ());var next=new Vec3(at.getX()+.5,y,at.getZ()+.5);
            if(Math.abs(y-home.getY())>2||next.distanceToSqr(position())<4||!level().noCollision(this,getBoundingBox().move(next.subtract(position())))||level().containsAnyLiquid(getBoundingBox().move(next.subtract(position()))))continue;
            ((ServerLevel)level()).sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,getX(),getY()+2,getZ(),20,.5,1,.5,.05);teleportTo(next.x,next.y,next.z);cast(4,15);break;
        }
    }
    @Override public void startSeenByPlayer(ServerPlayer player){super.startSeenByPlayer(player);boss.addPlayer(player);}
    @Override public void stopSeenByPlayer(ServerPlayer player){super.stopSeenByPlayer(player);boss.removePlayer(player);}
    @Override public void remove(RemovalReason reason){boss.removeAllPlayers();super.remove(reason);}
    @Override public void addAdditionalSaveData(CompoundTag n){super.addAdditionalSaveData(n);if(generation!=null)n.putUUID("BreachGeneration",generation);if(seal!=null)n.putUUID("BreachSeal",seal);n.putLong("BreachHome",home.asLong());n.putInt("BoltAfter",Math.max(0,nextBolt-tickCount));n.putInt("SlamAfter",Math.max(0,nextSlam-tickCount));n.putInt("SummonAfter",Math.max(0,nextSummon-tickCount));n.putInt("ShiftAfter",Math.max(0,nextShift-tickCount));}
    @Override public void readAdditionalSaveData(CompoundTag n){super.readAdditionalSaveData(n);generation=n.hasUUID("BreachGeneration")?n.getUUID("BreachGeneration"):null;seal=n.hasUUID("BreachSeal")?n.getUUID("BreachSeal"):null;home=BlockPos.of(n.getLong("BreachHome"));nextBolt=tickCount+Math.max(20,Math.min(200,n.getInt("BoltAfter")));nextSlam=tickCount+Math.max(20,Math.min(200,n.getInt("SlamAfter")));nextSummon=tickCount+Math.max(40,Math.min(400,n.getInt("SummonAfter")));nextShift=tickCount+Math.max(40,Math.min(300,n.getInt("ShiftAfter")));}
}
