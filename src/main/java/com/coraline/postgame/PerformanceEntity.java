package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A player-driven timed performance. No input/camera lock and no completion
 * on a timeout: a missed phrase may be retried without resetting world loot. */
public final class PerformanceEntity extends BlockEntity {
    private Tag raw=new CompoundTag();private InfiniteStructurePlan.Plan plan;private double previousY=Double.NaN;
    public PerformanceEntity(BlockPos at,BlockState state){super(com.coraline.registry.ModBlockEntities.PERFORMANCE.get(),at,state);}
    private CompoundTag data(){return raw instanceof CompoundTag n?n:new CompoundTag();}
    void configure(InfiniteStructurePlan.Plan plan){if(this.plan!=null||plan.family()!=InfiniteStructurePlan.Family.GRAND_THEATRE||!PerformanceLayout.core(plan).equals(worldPosition)||!(raw instanceof CompoundTag n)||!n.isEmpty())return;this.plan=plan;raw=PerformanceLayout.metadata(plan);setChanged();}
    private boolean authentic(){if(plan==null||isRemoved()||!(level instanceof ServerLevel server)||server.getBlockEntity(worldPosition)!=this||!getBlockState().is(com.coraline.registry.ModBlocks.PERFORMANCE_CONTROL.get())||!(server.getChunkSource().getGenerator() instanceof InfiniteChunkGenerator generator))return false;var n=data();for(String key:List.of("Phase","Cue","Cursor"))if(!n.contains(key,Tag.TAG_INT))return false;if(n.getInt("Phase")<0||n.getInt("Phase")>2||n.getInt("Cue")<0||n.getInt("Cue")>8||n.getInt("Cursor")< -60||n.getInt("Cursor")>50||!n.contains("LastTick",Tag.TAG_LONG)||n.getLong("LastTick")< -1||n.getInt("Phase")==1&&(!n.hasUUID("Owner")||n.getInt("Cue")>=8)||n.getInt("Phase")==2&&(!n.hasUUID("Owner")||n.getInt("Cue")!=8))return false;try{var epoch=generator.structureGeneration();return epoch.id().equals(plan.generation())&&epoch.seed()==plan.seed()&&InfiniteWorldData.get(server.getServer()).matchesGeneration(epoch.id());}catch(IllegalStateException unavailable){return false;}}
    boolean questAvailable(){return authentic();}
    boolean complete(){return authentic()&&data().getInt("Phase")==2;}
    void start(ServerPlayer player){if(!authentic()||!player.isAlive()||player.isCreative()||player.isSpectator()||player.level()!=level||player.distanceToSqr(worldPosition.getCenter())>36||!level.mayInteract(player,worldPosition)||!InfiniteWorldData.get(player.server).accessible())return;if(complete()){say(player,"performance_complete");return;}if(data().getInt("Phase")==1){say(player,"performance_active");return;}data().putInt("Phase",1);data().putUUID("Owner",player.getUUID());data().putInt("Cue",0);data().putInt("Cursor",-60);data().putLong("LastTick",level.getGameTime());previousY=player.getY();setChanged();sync();say(player,"performance_started");}
    private void abort(){data().putInt("Phase",0);data().putInt("Cue",0);data().putInt("Cursor",0);setChanged();sync();previousY=Double.NaN;}
    private static void say(ServerPlayer p,String key){p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.coraline."+key),true);}
    private void sync(){var state=getBlockState();var next=state.setValue(PerformanceBlock.PHASE,data().getInt("Phase"));if(!state.equals(next))level.setBlock(worldPosition,next,3);int lit=data().getInt("Phase")==1?PerformanceLayout.cue(plan,data().getInt("Cue")):-1;var tiles=PerformanceLayout.tiles(plan);for(int i=0;i<tiles.size();i++){var at=tiles.get(i);if(!level.hasChunkAt(at))continue;var tile=level.getBlockState(at);if(tile.is(com.coraline.registry.ModBlocks.PERFORMANCE_TILE.get())&&tile.getValue(PerformanceTileBlock.COLOR)==i&&tile.getValue(PerformanceTileBlock.LIT)!=(i==lit))level.setBlock(at,tile.setValue(PerformanceTileBlock.LIT,i==lit),3);}}
    private void unlockReward(){if(!complete())return;for(int i=0;i<plan.modules().size();i++)if(plan.modules().get(i).role()==InfiniteStructurePlan.Role.PERFORMANCE_VAULT){var at=SiteCacheReceipt.position(plan,i);if(level.hasChunkAt(at)&&level.getBlockEntity(at) instanceof SiteCacheBlockEntity cache)cache.unlock(plan.site());}}
    static void tick(Level level,BlockPos at,BlockState state,PerformanceEntity stage){
        if(!stage.authentic())return;if(level.getGameTime()%20==0)stage.sync();if(stage.complete()){if(level.getGameTime()%20==0)stage.unlockReward();return;}if(stage.data().getInt("Phase")!=1)return;
        long now=level.getGameTime(),last=stage.data().getLong("LastTick");if(now==last)return;if(now<last){stage.abort();return;}stage.data().putLong("LastTick",now);
        var player=level.getServer().getPlayerList().getPlayer(stage.data().getUUID("Owner"));if(player==null||player.level()!=level||!player.isAlive()||player.isCreative()||player.isSpectator()||player.distanceToSqr(at.getCenter())>24*24){stage.abort();return;}
        int cursor=stage.data().getInt("Cursor")+1;stage.data().putInt("Cursor",cursor);stage.setChanged();int cue=PerformanceLayout.cue(stage.plan,stage.data().getInt("Cue"));var tile=PerformanceLayout.tiles(stage.plan).get(cue);
        if(cursor==0){stage.sync();level.playSound(null,tile,net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HARP.get(),net.minecraft.sounds.SoundSource.BLOCKS,.8F,.8F+cue*.15F);}
        if(cursor==20)level.playSound(null,tile,net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME.get(),net.minecraft.sounds.SoundSource.BLOCKS,.8F,1.3F);
        boolean onMark=Math.abs(player.getX()-tile.getX()-.5)<.8&&Math.abs(player.getZ()-tile.getZ()-.5)<.8&&level.getBlockState(tile).is(com.coraline.registry.ModBlocks.PERFORMANCE_TILE.get())&&level.getBlockState(tile.above()).getCollisionShape(level,tile.above()).isEmpty()&&level.getFluidState(tile.above()).isEmpty();
        boolean jump=Double.isFinite(stage.previousY)&&stage.previousY<=tile.getY()+1.05&&player.getY()>=tile.getY()+1.2&&player.getY()<tile.getY()+2.5;
        if(cursor>=20&&cursor<=44&&onMark&&jump){int completed=stage.data().getInt("Cue")+1;stage.data().putInt("Cue",completed);stage.data().putInt("Cursor",0);if(completed==8){stage.data().putInt("Phase",2);stage.sync();stage.unlockReward();PostgameQuests.completed(player,stage.plan);say(player,"performance_complete");}else{stage.sync();level.playSound(null,tile,net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HARP.get(),net.minecraft.sounds.SoundSource.BLOCKS,.8F,1+completed*.05F);}stage.setChanged();}
        else if(cursor>=50){stage.data().putInt("Cue",0);stage.data().putInt("Cursor",-40);stage.sync();stage.setChanged();say(player,"performance_missed");}
        stage.previousY=player.getY();
    }
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);n.put("CoralinePerformance",raw.copy());}
    @Override public void load(CompoundTag n){super.load(n);raw=n.contains("CoralinePerformance")?n.get("CoralinePerformance").copy():new CompoundTag();plan=null;previousY=Double.NaN;if(raw instanceof CompoundTag tag)PerformanceLayout.read(tag).filter(p->PerformanceLayout.core(p).equals(worldPosition)).ifPresent(p->plan=p);}
}
