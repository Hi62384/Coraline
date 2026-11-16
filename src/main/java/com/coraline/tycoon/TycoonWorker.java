package com.coraline.tycoon;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Workers have no resident/agency identity and never run resident care AI. */
public final class TycoonWorker extends PathfinderMob {
    private static final EntityDataAccessor<Integer> ROLE=SynchedEntityData.defineId(TycoonWorker.class,EntityDataSerializers.INT),RANK=SynchedEntityData.defineId(TycoonWorker.class,EntityDataSerializers.INT),GESTURE=SynchedEntityData.defineId(TycoonWorker.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<java.util.Optional<UUID>> OWNER=SynchedEntityData.defineId(TycoonWorker.class,EntityDataSerializers.OPTIONAL_UUID);
    public UUID worker;
    public TycoonWorker(EntityType<? extends PathfinderMob> type,Level level){super(type,level);setPersistenceRequired();setMaxUpStep(1);if(getNavigation() instanceof GroundPathNavigation nav)nav.setCanOpenDoors(true);}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.MOVEMENT_SPEED,.28).add(Attributes.FOLLOW_RANGE,40);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(ROLE,0);entityData.define(RANK,1);entityData.define(GESTURE,0);entityData.define(OWNER,java.util.Optional.empty());}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new OpenDoorGoal(this,true));}
    public UUID owner(){return entityData.get(OWNER).orElse(null);}public int rank(){return entityData.get(RANK);}public int gesture(){return entityData.get(GESTURE);}public TycoonWorkerType role(){return TycoonWorkerType.values()[Math.floorMod(entityData.get(ROLE),TycoonWorkerType.values().length)];}
    public void configure(UUID owner,UUID id,TycoonWorkerType type,int rank){entityData.set(OWNER,java.util.Optional.of(owner));worker=id;entityData.set(ROLE,type.ordinal());entityData.set(RANK,rank);setCustomName(net.minecraft.network.chat.Component.literal(type.label));}
    public void presentation(int rank,int gesture){entityData.set(RANK,rank);entityData.set(GESTURE,gesture);}
    @Override public void tick(){super.tick();if(level().isClientSide||owner()==null)return;var h=TycoonData.get(getServer()).homes.get(owner());if(h==null||getServer().overworld().getGameTime()-h.state.getLong("WorkerOwnerLease")>40){getNavigation().stop();setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);}
        var row=h==null?new CompoundTag():h.state.getCompound("Workers").getCompound(worker==null?"":worker.toString());
        if(!row.isEmpty()&&row.hasUUID("Actor")&&getUUID().equals(row.getUUID("Actor"))&&row.hasUUID("Id")&&worker!=null&&worker.equals(row.getUUID("Id"))&&tickCount%20==0){row.putString("ActorDimension",level().dimension().location().toString());row.putLong("LastAt",blockPosition().asLong());TycoonData.get(getServer()).setDirty();}
        if(!row.getBoolean("Paid")){getNavigation().stop();setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);}
        if(tickCount%100==0&&(row.isEmpty()||row.hasUUID("Actor")&&!getUUID().equals(row.getUUID("Actor"))))discard();
    }
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){return false;}
    @Override public boolean canBeLeashed(Player player){return false;}
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
        if(worker==null||owner()==null||hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
        if(player instanceof net.minecraft.server.level.ServerPlayer p&&p.distanceToSqr(this)<=25)TycoonScope.run(p,owner(),()->{var h=TycoonWorld.home(p);var row=h.state.getCompound("Workers").getCompound(worker.toString());TycoonWorld.message(p,role().label+"  -  "+TycoonWorkers.taskLabel(row)+"  -  "+(row.getBoolean("Break")?"on break":"operating level "+rank())+". Manage policies at the Work Desk.");});
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    @Override public void addAdditionalSaveData(CompoundTag n){super.addAdditionalSaveData(n);if(owner()!=null)n.putUUID("Owner",owner());if(worker!=null)n.putUUID("Worker",worker);n.putInt("Role",entityData.get(ROLE));n.putInt("Rank",rank());}
    @Override public void readAdditionalSaveData(CompoundTag n){super.readAdditionalSaveData(n);worker=n.hasUUID("Worker")?n.getUUID("Worker"):null;entityData.set(OWNER,n.hasUUID("Owner")?java.util.Optional.of(n.getUUID("Owner")):java.util.Optional.empty());entityData.set(ROLE,n.getInt("Role"));entityData.set(RANK,Math.max(1,Math.min(5,n.getInt("Rank"))));}
}
