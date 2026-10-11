package com.coraline.block;

import com.coraline.clothing.ClothingEffects;
import com.coraline.clothing.UtilityItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Placed lantern; portable is retained only for cleaning up legacy saved lights. */
public final class MoonlitLanternBlock extends Block {
    public static final BooleanProperty NIGHT=BooleanProperty.create("night");
    public static final BooleanProperty PORTABLE=BooleanProperty.create("portable");
    private static final VoxelShape BODY=Block.box(4,0,4,12,12,12);
    public MoonlitLanternBlock(){
        super(BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.METAL).noOcclusion().randomTicks().lightLevel(s->s.getValue(NIGHT)?15:1));
        registerDefaultState(stateDefinition.any().setValue(NIGHT,false).setValue(PORTABLE,false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(NIGHT,PORTABLE);}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return state.getValue(PORTABLE)?Shapes.empty():BODY;}
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return state.getValue(PORTABLE)?Shapes.empty():BODY;}
    @Override public RenderShape getRenderShape(BlockState state){return state.getValue(PORTABLE)?RenderShape.INVISIBLE:RenderShape.MODEL;}
    @Override public void animateTick(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,RandomSource random){
        if(!state.getValue(PORTABLE)&&random.nextInt(3)==0)level.addParticle(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,pos.getX()+.5,pos.getY()+.7,pos.getZ()+.5,0,.012,0);
    }
    @Override public boolean canBeReplaced(BlockState state,net.minecraft.world.item.context.BlockPlaceContext context){return state.getValue(PORTABLE);}
    @Override public void onPlace(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,BlockState oldState,boolean moving){
        super.onPlace(state,level,pos,oldState,moving);
        if(!level.isClientSide)level.scheduleTick(pos,this,20);
    }
    @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(state.getValue(PORTABLE)){
            level.removeBlock(pos,false);return;
        }
        boolean night=ClothingEffects.lanternNight(level);
        if(state.getValue(NIGHT)!=night)level.setBlock(pos,state.setValue(NIGHT,night),3);
        level.scheduleTick(pos,this,20);
    }
    @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){tick(state,level,pos,random);}
}
