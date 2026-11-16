package com.coraline.tea;
import com.coraline.entity.CharacterEntity;
import net.minecraft.network.syncher.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
public final class TeaVisitorEntity extends CharacterEntity {
    private static final EntityDataAccessor<Integer> PHASE=SynchedEntityData.defineId(TeaVisitorEntity.class,EntityDataSerializers.INT),TIME=SynchedEntityData.defineId(TeaVisitorEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CUP=SynchedEntityData.defineId(TeaVisitorEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CLOCK=SynchedEntityData.defineId(TeaVisitorEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MOTION=SynchedEntityData.defineId(TeaVisitorEntity.class,EntityDataSerializers.INT);
    public TeaVisitorEntity(EntityType<? extends PathfinderMob> type,Level level){super(type,level);setInvulnerable(true);setPersistenceRequired();}
    @Override public String characterId(){return getType()==com.coraline.registry.ModEntities.SPINK_VISITOR.get()?"miss_spink":"miss_forcible";}
    @Override protected void registerGoals(){}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(PHASE,0);entityData.define(TIME,0);entityData.define(CUP,false);entityData.define(CLOCK,false);entityData.define(MOTION,0);}
    public int storyMotion(){return entityData.get(MOTION);}public void storyMotion(int stage){entityData.set(MOTION,stage);}
    public boolean storyClock(){return entityData.get(CLOCK);}public void storyClock(boolean running){entityData.set(CLOCK,running);}
    public boolean hasCup(){return entityData.get(CUP);}public void cup(boolean holds){entityData.set(CUP,holds);}
    public int phase(){return entityData.get(PHASE);}public int elapsed(){return entityData.get(TIME);}public void pose(int phase,int time){entityData.set(PHASE,phase);entityData.set(TIME,time);}
    @Override public void tick(){super.tick();com.coraline.wybie.VisitGrounding.lift(this);if(level().isClientSide||!getPersistentData().hasUUID("TeaOwner"))return;var v=com.coraline.bobinsky.BobinskyVisitData.get(getServer()).teaVisits.get(getPersistentData().getUUID("TeaOwner"));if(v==null||!v.getBoolean("Active")||v.getBoolean("Cancelled")||!v.getUUID("Token").equals(getPersistentData().getUUID("TeaToken")))discard();}
    @Override public boolean hurt(DamageSource source,float amount){return false;}@Override public void die(DamageSource source){}@Override public void kill(){}@Override public boolean isPushable(){return false;}
    @Override public void addAdditionalSaveData(CompoundTag n){super.addAdditionalSaveData(n);n.putInt("VisitPhase",phase());n.putInt("VisitTime",elapsed());}
    @Override public void readAdditionalSaveData(CompoundTag n){super.readAdditionalSaveData(n);pose(n.getInt("VisitPhase"),n.getInt("VisitTime"));}
}
