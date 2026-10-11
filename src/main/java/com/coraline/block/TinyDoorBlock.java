package com.coraline.block;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class TinyDoorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN=BlockStateProperties.OPEN;
    public static final BooleanProperty PORTAL=BooleanProperty.create("portal");
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new com.coraline.otherworld.DoorBlockEntity(pos,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type){
        if(level.isClientSide||com.coraline.otherworld.OtherWorld.isOther(level))return null;
        return (world,pos,s,entity)->{
            if(entity instanceof com.coraline.otherworld.DoorBlockEntity door&&world instanceof net.minecraft.server.level.ServerLevel sl){
                var record=com.coraline.otherworld.DoorData.get(sl.getServer()).doors.get(door.doorId);
                if(record!=null&&record.revoked)world.removeBlock(pos,false);
            }
        };
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity placer,net.minecraft.world.item.ItemStack stack){
        if(level instanceof net.minecraft.server.level.ServerLevel sl&&placer instanceof Player player&&level.getBlockEntity(pos) instanceof com.coraline.otherworld.DoorBlockEntity block){
            if(player.getAbilities().instabuild){stack=stack.copy();stack.getOrCreateTag().remove("CoralineDoor");}
            var data=com.coraline.otherworld.DoorData.get(sl.getServer());var door=data.observe(stack,player.getUUID());if(door==null)return;
            door.owner=player.getUUID();door.placed=true;door.dimension=level.dimension().location().toString();door.pos=pos.immutable();block.doorId=door.id;block.setChanged();data.setDirty();
        }
    }
    @Override public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder){
        if(builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof com.coraline.otherworld.DoorBlockEntity block&&block.doorId!=null){
            var door=com.coraline.otherworld.DoorData.get(builder.getLevel().getServer()).doors.get(block.doorId);
            if(door!=null)return java.util.List.of(com.coraline.otherworld.DoorData.stack(door));
        }return super.getDrops(state,builder);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&level instanceof net.minecraft.server.level.ServerLevel sl&&level.getBlockEntity(pos) instanceof com.coraline.otherworld.DoorBlockEntity block)
            com.coraline.otherworld.DoorData.get(sl.getServer()).destroyed(block.doorId);
        super.onRemove(state,level,pos,next,moving);
    }
    public TinyDoorBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(OPEN,false).setValue(PORTAL,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(FACING,OPEN,PORTAL);}
    @Override public VoxelShape getCollisionShape(BlockState s,BlockGetter level,BlockPos pos,CollisionContext context){return s.getValue(PORTAL)&&s.getValue(OPEN)?Shapes.empty():getShape(s,level,pos,context);}
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.Entity entity){
        if(state.getValue(PORTAL)&&state.getValue(OPEN)&&entity instanceof net.minecraft.server.level.ServerPlayer player&&level.getBlockEntity(pos) instanceof com.coraline.otherworld.DoorBlockEntity block)
            com.coraline.otherworld.OtherWorld.enter(player,block);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        return switch(s.getValue(FACING)){case NORTH->box(0,0,12,16,16,16);case SOUTH->box(0,0,0,16,16,4);case WEST->box(12,0,0,16,16,16);default->box(0,0,0,4,16,16);};
    }
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(!s.getValue(OPEN)&&player instanceof net.minecraft.server.level.ServerPlayer serverPlayer&&com.coraline.otherworld.OtherWorld.lockTunnelDoor(serverPlayer,p))return InteractionResult.CONSUME;
        // The Other World Little Door can be walked into or interacted with;
        // neither route requires sneaking once the story permits a return.
        if(s.getValue(PORTAL)&&s.getValue(OPEN)&&com.coraline.otherworld.OtherWorld.isOther(l)){
            if(!l.isClientSide&&player instanceof net.minecraft.server.level.ServerPlayer serverPlayer&&
                l.getBlockEntity(p) instanceof com.coraline.otherworld.DoorBlockEntity block)
                com.coraline.otherworld.OtherWorld.enter(serverPlayer,block);
            return InteractionResult.sidedSuccess(l.isClientSide);
        }
        if(!l.isClientSide&&!com.coraline.otherworld.OtherWorld.isOther(l)&&l.getBlockEntity(p) instanceof com.coraline.otherworld.DoorBlockEntity block&&block.doorId==null)
            setPlacedBy(l,p,s,player,new net.minecraft.world.item.ItemStack(com.coraline.registry.ModItems.TINY_DOOR.get()));
        if(!l.isClientSide){l.setBlock(p,s.cycle(OPEN),3);l.playSound(null,p,s.getValue(OPEN)?net.minecraft.sounds.SoundEvents.WOODEN_DOOR_CLOSE:net.minecraft.sounds.SoundEvents.WOODEN_DOOR_OPEN,net.minecraft.sounds.SoundSource.BLOCKS,.8F,1);
            if(!s.getValue(OPEN))com.coraline.otherworld.SceneAudio.doorOpened((net.minecraft.server.level.ServerLevel)l,p);
            if(s.getValue(PORTAL)&&!s.getValue(OPEN))player.displayClientMessage(net.minecraft.network.chat.Component.literal("Sneak into the doorway to crawl through the tunnel."),true);
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
}
