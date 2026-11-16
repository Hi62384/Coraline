package com.coraline.otherworld;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;

/** Small native furniture: real footprints, not oversized full-block assemblies. */
public final class RosewoodFixtureBlock extends HorizontalDirectionalBlock {
    public enum Kind {MIRROR,SINK,TOILET,BATH,TABLE,CHAIR,SOFA,RACK,COUNTER}
    public static final BooleanProperty NORTH=BooleanProperty.create("north"),EAST=BooleanProperty.create("east"),SOUTH=BooleanProperty.create("south"),WEST=BooleanProperty.create("west");
    private final Kind kind;
    public RosewoodFixtureBlock(Kind kind){
        super(Properties.of().strength(1.5F).sound(SoundType.WOOD).noOcclusion().noLootTable());this.kind=kind;
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(NORTH,false).setValue(EAST,false).setValue(SOUTH,false).setValue(WEST,false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,NORTH,EAST,SOUTH,WEST);}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        var shape=switch(kind){
            case TABLE -> Shapes.or(box(0,10,0,16,12,16),box(1,0,1,3,10,3),box(13,0,13,15,10,15));
            case MIRROR -> switch(state.getValue(FACING)){
                case EAST -> box(0,1,2,2,15,14);case WEST -> box(14,1,2,16,15,14);
                case SOUTH -> box(2,1,0,14,15,2);default -> box(2,1,14,14,15,16);
            };
            case SINK -> box(1,0,1,15,13,15);
            case TOILET -> box(3,0,2,13,14,15);
            case BATH -> Shapes.or(box(0,0,0,16,3,16),box(0,3,0,2,12,16),box(14,3,0,16,12,16));
            case CHAIR -> Shapes.or(box(2,5,2,14,7,14),box(2,7,12,14,16,15));
            case SOFA -> Shapes.or(box(0,2,0,16,7,14),box(0,7,12,16,14,16));
            case RACK -> Shapes.or(box(5,0,5,11,2,11),box(7,2,7,9,16,9));
            case COUNTER -> box(0,0,0,16,14,16);
        };
        if(kind==Kind.MIRROR||state.getValue(FACING)==Direction.NORTH)return shape;
        var rotated=new VoxelShape[]{Shapes.empty()};var facing=state.getValue(FACING);
        shape.forAllBoxes((x0,y0,z0,x1,y1,z1)->{
            var box=switch(facing){
                case EAST -> Shapes.box(1-z1,y0,x0,1-z0,y1,x1);
                case SOUTH -> Shapes.box(1-x1,y0,1-z1,1-x0,y1,1-z0);
                default -> Shapes.box(z0,y0,1-x1,z1,y1,1-x0);
            };rotated[0]=Shapes.or(rotated[0],box);
        });return rotated[0];
    }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return getShape(state,level,pos,context);}
}
