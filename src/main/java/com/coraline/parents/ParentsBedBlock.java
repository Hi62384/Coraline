package com.coraline.parents;
import com.coraline.config.CoralineConfig;
import com.coraline.registry.ModSounds;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.network.NetworkHooks;

public final class ParentsBedBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty PART=IntegerProperty.create("part",0,3);
    public static final BooleanProperty AVAILABLE=BooleanProperty.create("available");
    public ParentsBedBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(PART,0).setValue(AVAILABLE,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(FACING,PART,AVAILABLE);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    public static BlockPos partPos(BlockPos anchor,Direction facing,int part){return anchor.relative(facing.getClockWise(),part%2).relative(facing.getOpposite(),part/2);}
    public static BlockPos anchorPos(BlockPos pos,BlockState state){int part=state.getValue(PART);var facing=state.getValue(FACING);return pos.relative(facing.getCounterClockWise(),part%2).relative(facing,part/2);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return s.getValue(PART)==0?new ParentsBedEntity(p,s):null;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return level.isClientSide?null:createTickerHelper(type,com.coraline.registry.ModBlockEntities.PARENTS_BED.get(),ParentsBedEntity::serverTick);
    }
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return box(0,0,0,16,10,16);}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity player,ItemStack stack){
        if(!(l instanceof ServerLevel sl)||!(l.getBlockEntity(p) instanceof ParentsBedEntity be))return;
        var tag=stack.getTagElement("BlockEntityTag");if(tag==null||!tag.hasUUID("Family"))return;
        be.familyId=tag.getUUID("Family");be.setChanged();var f=be.family();if(f==null)return;
        f.bed=p;f.homeDim=l.dimension().location().toString();f.chest=p;
        for(int part=1;part<4;part++)l.setBlock(partPos(p,s.getValue(FACING),part),s.setValue(PART,part),3);
        f.momDelivery=f.dadDelivery=l.getGameTime()+CoralineConfig.TASK_INTERVAL.get();FamilyData.get(sl.getServer()).setDirty();
    }
    @Override public java.util.List<ItemStack> getDrops(BlockState state,LootParams.Builder context){
        if(context.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof ParentsBedEntity be&&be.familyId!=null)return java.util.List.of(ParentsBedItem.forFamily(be.familyId));
        return java.util.List.of();
    }
    @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){
        if(!s.is(next.getBlock())&&l instanceof ServerLevel sl){
            if(s.getValue(PART)==0){
                if(l.getBlockEntity(p) instanceof ParentsBedEntity be){
                    Containers.dropContents(l,p,be);
                    var f=be.family();if(f!=null&&p.equals(f.bed)){f.bed=null;f.chest=null;FamilyData.get(sl.getServer()).setDirty();}
                }
                for(int part=1;part<4;part++){
                    BlockPos other=partPos(p,s.getValue(FACING),part);var state=l.getBlockState(other);
                    if(state.is(this)&&state.getValue(PART)==part&&state.getValue(FACING)==s.getValue(FACING))l.removeBlock(other,false);
                }
            }else{
                BlockPos anchor=anchorPos(p,s);var state=l.getBlockState(anchor);
                if(state.is(this)&&state.getValue(PART)==0)l.destroyBlock(anchor,true);
            }
        }super.onRemove(s,l,p,next,moving);
    }
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(l.isClientSide)return InteractionResult.SUCCESS;
        double depth=switch(s.getValue(FACING)){
            case NORTH->hit.getLocation().z-p.getZ();case SOUTH->1-(hit.getLocation().z-p.getZ());
            case WEST->hit.getLocation().x-p.getX();default->1-(hit.getLocation().x-p.getX());
        };
        boolean trunk=s.getValue(PART)<2&&depth<=5.0/16.0;
        p=anchorPos(p,s);
        if(!(l.getBlockEntity(p) instanceof ParentsBedEntity be))return InteractionResult.PASS;
        // Storage remains recoverable even after a parent's death. The trunk is part of the bed.
        if(player.isShiftKeyDown()||trunk){
            player.openMenu(be);return InteractionResult.CONSUME;
        }
        var f=be.family();if(f==null||f.dead){player.displayClientMessage(Component.literal("These parents are gone."),true);return InteractionResult.CONSUME;}
        if(!player.getUUID().equals(f.owner)){player.displayClientMessage(Component.literal("This is another family's bed."),true);return InteractionResult.CONSUME;}
        player.openMenu(be);
        return InteractionResult.CONSUME;
    }
}
