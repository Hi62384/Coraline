package com.coraline.postgame;

/** Only the authenticated register removes these supernatural cell bindings. */
public final class OrphanageBindingBlock extends net.minecraft.world.level.block.IronBarsBlock {
    public OrphanageBindingBlock(){super(net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.IRON_BARS).strength(-1,3600000).noLootTable());}
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(net.minecraft.world.level.block.state.BlockState s,net.minecraft.world.level.BlockGetter l,net.minecraft.core.BlockPos at,net.minecraft.world.phys.shapes.CollisionContext c){return net.minecraft.world.phys.shapes.Shapes.or(box(0,0,7,16,16,9),box(7,0,0,9,16,16));}
    @Override public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(net.minecraft.world.level.block.state.BlockState s,net.minecraft.world.level.BlockGetter l,net.minecraft.core.BlockPos at,net.minecraft.world.phys.shapes.CollisionContext c){return getShape(s,l,at,c);}
    @Override public boolean canEntityDestroy(net.minecraft.world.level.block.state.BlockState s,net.minecraft.world.level.BlockGetter l,net.minecraft.core.BlockPos at,net.minecraft.world.entity.Entity entity){return false;}
}
