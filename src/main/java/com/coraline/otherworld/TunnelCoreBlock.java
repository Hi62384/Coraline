package com.coraline.otherworld;
public final class TunnelCoreBlock extends net.minecraft.world.level.block.BaseEntityBlock {
    public TunnelCoreBlock(){super(Properties.of().strength(-1,3600000).lightLevel(s->12));}
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(net.minecraft.core.BlockPos p,net.minecraft.world.level.block.state.BlockState s){return new TunnelCoreEntity(p,s);}
    @Override public net.minecraft.world.level.block.RenderShape getRenderShape(net.minecraft.world.level.block.state.BlockState s){return net.minecraft.world.level.block.RenderShape.INVISIBLE;}
}
