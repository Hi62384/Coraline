package com.coraline.tycoon;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;

/** Ephemeral physical NPC seat, never a floating label or a second resident. */
public final class TycoonSupperSeat extends Entity {
    public UUID owner;
    public TycoonSupperSeat(EntityType<? extends TycoonSupperSeat> type,Level level){super(type,level);setNoGravity(true);}
    @Override protected void defineSynchedData(){}
    @Override protected void readAdditionalSaveData(CompoundTag n){owner=n.hasUUID("Owner")?n.getUUID("Owner"):null;}
    @Override protected void addAdditionalSaveData(CompoundTag n){if(owner!=null)n.putUUID("Owner",owner);}
    @Override protected boolean canAddPassenger(Entity e){return getPassengers().isEmpty()&&e instanceof TycoonNpc a&&!a.staff()&&owner!=null&&owner.equals(a.owner);}
    @Override public double getPassengersRidingOffset(){return 0;}
    @Override protected void positionRider(Entity rider,MoveFunction move){move.accept(rider,getX(),getY(),getZ());}
    @Override public void tick(){super.tick();if(!level().isClientSide&&tickCount>60&&getPassengers().isEmpty())discard();}
}
