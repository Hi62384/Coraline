package com.coraline.tycoon;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
public final class TycoonBlock extends BaseEntityBlock {
    public final String kind;
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty OPEN=IntegerProperty.create("open",0,3);
    public static final BooleanProperty LIT=BooleanProperty.create("lit"),BROKEN=BooleanProperty.create("broken");
    public TycoonBlock(String id){super(Properties.of().strength(id.equals("world_work_desk")||id.equals("void_foundation")?-1:1.5F,3600000).sound(SoundType.WOOD).noOcclusion().noLootTable().lightLevel(s->id.equals("orphan_lamp")&&!s.getValue(BROKEN)?12:s.getValue(LIT)?7:0));kind=id;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(LIT,false).setValue(BROKEN,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,LIT,BROKEN,OPEN);}
    @Override public void tick(BlockState state,ServerLevel level,BlockPos at,net.minecraft.util.RandomSource random){if(!kind.equals("other_world_fridge")||!(level.getBlockEntity(at) instanceof TycoonBlockEntity be))return;int target=be.openUsers>0||be.mealDoorUntil>level.getGameTime()?3:0,was=state.getValue(OPEN),next=was<target?was+1:was>target?was-1:was;if(was!=next)level.setBlock(at,state.setValue(OPEN,next),3);if(next!=target||be.openUsers==0&&be.mealDoorUntil>level.getGameTime())level.scheduleTick(at,this,2);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return kind.equals("void_foundation")?null:new TycoonBlockEntity(p,s);}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return switch(kind){case "orphan_rug"->box(0,0,0,16,1,16);case "orphan_bed"->box(0,0,0,16,9,16);case "orphan_food_table","other_world_supper_table","world_work_desk"->box(0,0,0,16,13,16);case "special_dimension_little_door"->s.getValue(FACING).getAxis()==Direction.Axis.Z?box(1,0,5,15,16,11):box(5,0,1,11,16,15);case "orphan_plant"->box(3,0,3,13,14,13);default->Shapes.block();};}
    @Override public PushReaction getPistonPushReaction(BlockState s){return PushReaction.BLOCK;}
    @Override public void setPlacedBy(Level l,BlockPos at,BlockState s,LivingEntity actor,ItemStack item){if(actor instanceof ServerPlayer p&&l.getBlockEntity(at) instanceof TycoonBlockEntity be){be.owner=TycoonWorld.owner(p);be.setChanged();TycoonWorld.placed(p,this,be,item);}}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos at,Player player,InteractionHand hand,BlockHitResult hit){
        if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;if(l.isClientSide)return InteractionResult.SUCCESS;
        if(!(player instanceof ServerPlayer p)||!(l.getBlockEntity(at) instanceof TycoonBlockEntity be)||p.distanceToSqr(at.getCenter())>64||!TycoonCollaborators.allowed(p,be.owner))return InteractionResult.FAIL;
        return TycoonScope.call(p,be.owner,()->useOwned(s,l,at,p,be),InteractionResult.FAIL);
    }
    private InteractionResult useOwned(BlockState s,Level l,BlockPos at,ServerPlayer p,TycoonBlockEntity be){
        if(kind.equals("special_dimension_little_door")&&TycoonCollaborationVisits.past(p)){TycoonCollaborationVisits.leave(p);return InteractionResult.CONSUME;}
        if(!TycoonWorld.currentFurniture(p,be)){
            // Keep ordinary stored items recoverable without reactivating an
            // old entrance, household service or progression machine.
            if(TycoonCollaborators.storage(kind))p.openMenu(be);
            else p.displayClientMessage(net.minecraft.network.chat.Component.literal("This furnishing belongs to a retired Other World."),true);
            return InteractionResult.CONSUME;
        }
        if(kind.equals("special_dimension_little_door")){if(p.getMainHandItem().is(TycoonContent.WEAVER.get()))TycoonWorld.activate(p,at);else if(p.isShiftKeyDown()&&l.dimension()==Level.OVERWORLD)TycoonWorld.pickup(p,at);else TycoonWorld.door(p,be);}
        else if(kind.equals("world_work_desk"))TycoonNetwork.open(p,"desk",at);
        else if(s.getValue(BROKEN)&&p.isShiftKeyDown()){l.setBlock(at,s.setValue(BROKEN,false),3);p.displayClientMessage(net.minecraft.network.chat.Component.literal("Repaired the household furnishing."),true);}
        else if(kind.equals("water_station"))be.refill(p);
        else if(kind.equals("orphan_fridge")||kind.equals("other_world_fridge")||kind.equals("storage_cabinet")||kind.equals("toy_box"))p.openMenu(be);
        else if(kind.equals("orphan_food_table"))TycoonCare.serveAt(p,at);
        return InteractionResult.CONSUME;
    }
    @Override public void onRemove(BlockState s,Level l,BlockPos pos,BlockState next,boolean moved){if(s.getBlock()!=next.getBlock()&&!l.isClientSide&&l.getBlockEntity(pos) instanceof TycoonBlockEntity be){net.minecraft.world.Containers.dropContents(l,pos,be);TycoonWorld.removed((ServerLevel)l,pos,be);}super.onRemove(s,l,pos,next,moved);}
}
