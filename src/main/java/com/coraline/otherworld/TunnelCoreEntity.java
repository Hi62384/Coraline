package com.coraline.otherworld;
public final class TunnelCoreEntity extends net.minecraft.world.level.block.entity.BlockEntity {
    public TunnelCoreEntity(net.minecraft.core.BlockPos p,net.minecraft.world.level.block.state.BlockState s){super(com.coraline.registry.ModBlockEntities.TUNNEL_CORE.get(),p,s);}
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(){return new net.minecraft.world.phys.AABB(worldPosition.offset(-1,0,-1),worldPosition.offset(2,3,19));}
}
