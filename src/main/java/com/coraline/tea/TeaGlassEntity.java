package com.coraline.tea;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
/** Falling glass and floor fragments are scene geometry, not pickup-able items. */
public final class TeaGlassEntity extends Entity {
    private static final EntityDataAccessor<Boolean> BROKEN=SynchedEntityData.defineId(TeaGlassEntity.class,EntityDataSerializers.BOOLEAN);
    public TeaGlassEntity(EntityType<?> type,Level level){super(type,level);setInvulnerable(true);}
    @Override protected void defineSynchedData(){entityData.define(BROKEN,false);}
    public boolean broken(){return entityData.get(BROKEN);}public void shatter(){entityData.set(BROKEN,true);setNoGravity(true);setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);}
    @Override protected void readAdditionalSaveData(CompoundTag n){entityData.set(BROKEN,n.getBoolean("Broken"));setNoGravity(broken());}
    @Override protected void addAdditionalSaveData(CompoundTag n){n.putBoolean("Broken",broken());}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource s,float amount){return false;}
    @Override public void tick(){super.tick();if(level().isClientSide)return;var t=getPersistentData();var v=t.hasUUID("TeaOwner")?com.coraline.bobinsky.BobinskyVisitData.get(getServer()).teaVisits.get(t.getUUID("TeaOwner")):null;if(v==null||!v.getBoolean("Active")||v.getBoolean("Cancelled")||!t.hasUUID("TeaToken")||!v.getUUID("Token").equals(t.getUUID("TeaToken"))){discard();return;}if(!broken()){setDeltaMovement(getDeltaMovement().add(0,-.04,0));move(MoverType.SELF,getDeltaMovement());if(onGround())setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);}}
}
