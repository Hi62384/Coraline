package com.coraline.postgame;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;

/** No transaction inventory is stored in this block, including its dropped item. */
public final class MagicMirrorBlockEntity extends BlockEntity implements MenuProvider {
    private long flash=-1000;
    public MagicMirrorBlockEntity(BlockPos at,BlockState state){super(com.coraline.registry.ModBlockEntities.MAGIC_MIRROR.get(),at,state);}
    public boolean canUse(Player player){
        if(!(player instanceof net.minecraft.server.level.ServerPlayer p)||level!=p.level()||!p.isAlive()||p.isSpectator()||p.distanceToSqr(worldPosition.getCenter())>64||level.getBlockEntity(worldPosition)!=this||!getBlockState().is(com.coraline.registry.ModBlocks.MAGIC_MIRROR.get())||!level.getBlockState(worldPosition.above()).is(com.coraline.registry.ModBlocks.MAGIC_MIRROR.get())||!level.mayInteract(p,worldPosition))return false;
        if(getBlockState().getValue(MagicMirrorBlock.HALF)!=DoubleBlockHalf.LOWER)return false;
        var upper=level.getBlockState(worldPosition.above());if(upper.getValue(MagicMirrorBlock.HALF)!=DoubleBlockHalf.UPPER||upper.getValue(MagicMirrorBlock.FACING)!=getBlockState().getValue(MagicMirrorBlock.FACING))return false;
        if(level.dimension().equals(com.coraline.tycoon.TycoonWorld.KEY)){
            var home=com.coraline.tycoon.TycoonData.get(p.server).homes.get(p.getUUID());
            if(home==null||!home.active()||!com.coraline.tycoon.TycoonLandscape.owned(home,worldPosition))return false;
        }
        return true;
    }
    void completed(){flash=level.getGameTime();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,net.minecraft.sounds.SoundSource.BLOCKS,.7F,.7F);}
    public float flash(float partial){return level==null?0:Math.max(0,1-(level.getGameTime()+partial-flash)/20F);}
    @Override public CompoundTag getUpdateTag(){var n=new CompoundTag();n.putLong("Flash",flash);return n;}
    @Override public void handleUpdateTag(CompoundTag n){flash=n.getLong("Flash");}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public void onDataPacket(net.minecraft.network.Connection connection,ClientboundBlockEntityDataPacket packet){if(packet.getTag()!=null)handleUpdateTag(packet.getTag());}
    @Override public AABB getRenderBoundingBox(){return new AABB(worldPosition).expandTowards(0,1.15,0).inflate(.1);}
    @Override public Component getDisplayName(){return Component.translatable("block.coraline.magic_mirror");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return canUse(player)?new MagicMirrorMenu(id,inventory,this):null;}
}
