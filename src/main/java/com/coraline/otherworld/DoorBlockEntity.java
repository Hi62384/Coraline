package com.coraline.otherworld;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import java.util.UUID;

public final class DoorBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity {
    public UUID doorId;
    public DoorBlockEntity(BlockPos pos,BlockState state){super(com.coraline.registry.ModBlockEntities.TINY_DOOR.get(),pos,state);}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);if(doorId!=null)tag.putUUID("Door",doorId);}
    @Override public void load(CompoundTag tag){super.load(tag);doorId=tag.hasUUID("Door")?tag.getUUID("Door"):null;}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
