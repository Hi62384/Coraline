package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Four actual protected lights, focused through a native spyglass for twenty
 * consecutive nearby ticks each. No client-supplied completion packet. */
public final class ObservatoryEntity extends BlockEntity {
    private Tag raw=new CompoundTag();private InfiniteStructurePlan.Plan plan;private UUID focusOwner;private int focus=-1,focusTicks;
    public ObservatoryEntity(BlockPos at,BlockState state){super(com.coraline.registry.ModBlockEntities.OBSERVATORY.get(),at,state);}
    private CompoundTag data(){return raw instanceof CompoundTag n?n:new CompoundTag();}
    void configure(InfiniteStructurePlan.Plan plan){if(this.plan!=null||plan.family()!=InfiniteStructurePlan.Family.FORGOTTEN_OBSERVATORY||!ObservatoryLayout.core(plan).equals(worldPosition)||!(raw instanceof CompoundTag n)||!n.isEmpty())return;this.plan=plan;raw=ObservatoryLayout.metadata(plan);setChanged();}
    boolean authentic(){if(plan==null||isRemoved()||!(level instanceof ServerLevel server)||!getBlockState().is(com.coraline.registry.ModBlocks.OBSERVATORY_TELESCOPE.get())||server.getBlockEntity(worldPosition)!=this||!(server.getChunkSource().getGenerator() instanceof InfiniteChunkGenerator generator)||!data().contains("Observed",Tag.TAG_INT)||data().getInt("Observed")<0||data().getInt("Observed")>15)return false;try{var epoch=generator.structureGeneration();return epoch.id().equals(plan.generation())&&epoch.seed()==plan.seed()&&InfiniteWorldData.get(server.getServer()).matchesGeneration(epoch.id());}catch(IllegalStateException unavailable){return false;}}
    boolean complete(){return authentic()&&data().getInt("Observed")==15;}
    private int aligned(ServerPlayer player){
        if(player==null||!player.isAlive()||player.isCreative()||player.isSpectator()||player.level()!=level||player.distanceToSqr(worldPosition.getCenter())>16||!player.isUsingItem()||!player.getUseItem().is(Items.SPYGLASS)||!InfiniteWorldData.get(player.server).accessible())return -1;
        var stars=ObservatoryLayout.stars(plan);for(int i=0;i<stars.size();i++){
            if((data().getInt("Observed")&(1<<i))!=0)continue;var at=stars.get(i);if(!level.hasChunkAt(at))continue;var state=level.getBlockState(at);if(!state.is(com.coraline.registry.ModBlocks.OBSERVATORY_STAR.get())||state.getValue(ObservatoryStarBlock.COLOR)!=i)continue;
            var direction=at.getCenter().subtract(player.getEyePosition());if(direction.lengthSqr()<1||player.getViewVector(1).dot(direction.normalize())<.999)continue;
            var hit=level.clip(new net.minecraft.world.level.ClipContext(player.getEyePosition(),at.getCenter(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,player));if(hit.getType()==HitResult.Type.BLOCK&&hit.getBlockPos().equals(at))return i;
        }return -1;
    }
    private void sync(){var state=getBlockState();var next=state.setValue(ObservatoryBlock.OBSERVED,Integer.bitCount(data().getInt("Observed"))).setValue(ObservatoryBlock.ROTATION,focus<0?state.getValue(ObservatoryBlock.ROTATION):(focus*2+3)&7);if(!state.equals(next))level.setBlock(worldPosition,next,3);}
    private void unlockReward(){if(!complete())return;for(int i=0;i<plan.modules().size();i++)if(plan.modules().get(i).role()==InfiniteStructurePlan.Role.RESEARCH_VAULT){var at=SiteCacheReceipt.position(plan,i);if(level.hasChunkAt(at)&&level.getBlockEntity(at) instanceof SiteCacheBlockEntity cache)cache.unlock(plan.site());}}
    static void tick(Level level,BlockPos at,BlockState state,ObservatoryEntity telescope){
        if(!telescope.authentic())return;if(level.getGameTime()%20==0)telescope.sync();if(telescope.complete()){if(level.getGameTime()%20==0)telescope.unlockReward();return;}
        ServerPlayer observer=telescope.focusOwner==null?null:level.getServer().getPlayerList().getPlayer(telescope.focusOwner);int index=telescope.aligned(observer);
        if(index<0){observer=null;for(var player:level.getEntitiesOfClass(ServerPlayer.class,new AABB(at).inflate(4))){int found=telescope.aligned(player);if(found>=0){observer=player;index=found;break;}}}
        if(observer==null){telescope.focusOwner=null;telescope.focus=-1;telescope.focusTicks=0;return;}
        if(!observer.getUUID().equals(telescope.focusOwner)||index!=telescope.focus){telescope.focusOwner=observer.getUUID();telescope.focus=index;telescope.focusTicks=0;telescope.sync();}
        if(++telescope.focusTicks<20)return;telescope.data().putInt("Observed",telescope.data().getInt("Observed")|(1<<index));telescope.setChanged();telescope.sync();telescope.focusTicks=0;
        observer.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.coraline.observatory_observed",Integer.bitCount(telescope.data().getInt("Observed"))),true);observer.playNotifySound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME.get(),net.minecraft.sounds.SoundSource.BLOCKS,.7F,1+index*.15F);if(telescope.complete()){telescope.unlockReward();PostgameQuests.completed(observer,telescope.plan);}
    }
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);n.put("CoralineObservatory",raw.copy());}
    @Override public void load(CompoundTag n){super.load(n);raw=n.contains("CoralineObservatory")?n.get("CoralineObservatory").copy():new CompoundTag();plan=null;focusOwner=null;focus=-1;focusTicks=0;if(raw instanceof CompoundTag tag)ObservatoryLayout.read(tag).filter(p->ObservatoryLayout.core(p).equals(worldPosition)).ifPresent(p->plan=p);}
}
