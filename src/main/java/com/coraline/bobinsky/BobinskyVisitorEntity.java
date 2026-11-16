package com.coraline.bobinsky;
import com.coraline.entity.CharacterEntity;
import net.minecraft.network.syncher.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
public final class BobinskyVisitorEntity extends CharacterEntity {
    private static final EntityDataAccessor<Integer> PHASE=SynchedEntityData.defineId(BobinskyVisitorEntity.class,EntityDataSerializers.INT),TIME=SynchedEntityData.defineId(BobinskyVisitorEntity.class,EntityDataSerializers.INT);
    public BobinskyVisitorEntity(EntityType<? extends PathfinderMob> type,Level level){super(type,level);setInvulnerable(true);setPersistenceRequired();}
    @Override public String characterId(){return "bobinsky";}
    @Override protected void registerGoals(){}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(PHASE,0);entityData.define(TIME,0);}
    public int phase(){return entityData.get(PHASE);}public int elapsed(){return entityData.get(TIME);}public void pose(int phase,int time){entityData.set(PHASE,phase);entityData.set(TIME,time);}
    @Override public void tick(){super.tick();com.coraline.wybie.VisitGrounding.lift(this);if(level().isClientSide||!getPersistentData().hasUUID("BobinskyOwner"))return;var v=BobinskyVisitData.get(getServer()).visits.get(getPersistentData().getUUID("BobinskyOwner"));if(v==null||!v.getBoolean("Active")||v.getBoolean("Cancelled")||!v.getUUID("Token").equals(getPersistentData().getUUID("BobinskyToken")))discard();}
    @Override public boolean hurt(DamageSource source,float amount){return false;}
    @Override public void die(DamageSource source){}@Override public void kill(){}@Override public boolean isPushable(){return false;}
    @Override public void addAdditionalSaveData(CompoundTag n){super.addAdditionalSaveData(n);n.putInt("VisitPhase",phase());n.putInt("VisitTime",elapsed());}
    @Override public void readAdditionalSaveData(CompoundTag n){super.readAdditionalSaveData(n);pose(n.getInt("VisitPhase"),n.getInt("VisitTime"));}
}
