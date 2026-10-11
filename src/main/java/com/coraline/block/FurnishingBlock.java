package com.coraline.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Low-profile, purpose-modelled Coraline furniture rather than full cubes. */
public final class FurnishingBlock extends Block {
    public enum Kind {SOFA,TABLE,LAMP,SIDEBOARD,CONSOLE,WALL_PANEL}
    private final VoxelShape shape;
    public FurnishingBlock(Properties properties,Kind kind){
        super(properties);
        shape=switch(kind){
            case SOFA -> Shapes.or(box(0,2,0,16,8,13),box(0,2,12,16,15,16));
            case TABLE -> Shapes.or(box(0,9,0,16,11,16),box(1,0,1,3,9,3),box(13,0,1,15,9,3),box(1,0,13,3,9,15),box(13,0,13,15,9,15));
            case LAMP -> Shapes.or(box(5,0,5,11,2,11),box(7,2,7,9,13,9),box(3,12,3,13,16,13));
            case SIDEBOARD -> Shapes.or(box(0,0,0,16,12,16),box(0,12,0,16,14,16));
            case CONSOLE -> Shapes.or(box(0,0,0,16,3,16),box(1,3,1,3,10,3),box(13,3,1,15,10,3),box(0,10,0,16,12,16),box(5,12,5,11,16,11));
            case WALL_PANEL -> box(0,0,0,16,16,16);
        };
    }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return shape;}
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return shape;}
}
