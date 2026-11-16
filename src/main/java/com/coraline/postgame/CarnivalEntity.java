package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Eight real hits on the highlighted moving targets, from the firing lane.
 * Admission is finite per attempt; success unlocks one existing world cache. */
public final class CarnivalEntity extends BlockEntity {
    private Tag raw=new CompoundTag();private InfiniteStructurePlan.Plan plan;
    public CarnivalEntity(BlockPos at,BlockState state){super(com.coraline.registry.ModBlockEntities.CARNIVAL.get(),at,state);}
    private CompoundTag data(){return raw instanceof CompoundTag n?n:new CompoundTag();}
    void configure(InfiniteStructurePlan.Plan plan){if(this.plan!=null||plan.family()!=InfiniteStructurePlan.Family.ENDLESS_CARNIVAL||!CarnivalLayout.core(plan).equals(worldPosition)||!(raw instanceof CompoundTag n)||!n.isEmpty())return;this.plan=plan;raw=CarnivalLayout.metadata(plan);setChanged();}
    private boolean authentic(){if(plan==null||isRemoved()||!(level instanceof ServerLevel server)||server.getBlockEntity(worldPosition)!=this||!getBlockState().is(com.coraline.registry.ModBlocks.CARNIVAL_CONTROL.get())||!(server.getChunkSource().getGenerator() instanceof InfiniteChunkGenerator generator))return false;var n=data();for(String key:List.of("Phase","Score","Ticks"))if(!n.contains(key,Tag.TAG_INT))return false;if(n.getInt("Phase")<0||n.getInt("Phase")>2||n.getInt("Score")<0||n.getInt("Score")>8||n.getInt("Ticks")<0||n.getInt("Ticks")>1200||!n.contains("LastTick",Tag.TAG_LONG)||n.getLong("LastTick")< -1||!n.contains("RetryAfter",Tag.TAG_LONG)||n.getLong("RetryAfter")<0||!n.contains("Targets",Tag.TAG_LIST))return false;var list=n.getList("Targets",Tag.TAG_INT_ARRAY);if(list.size()!=((ListTag)n.get("Targets")).size()||list.size()>4)return false;var ids=new HashSet<UUID>();for(var entry:list){if(!(entry instanceof IntArrayTag array)||array.getAsIntArray().length!=4||!ids.add(NbtUtils.loadUUID(array)))return false;}if(n.getInt("Phase")!=0&&(!n.hasUUID("Owner")||!n.hasUUID("Session")||list.size()!=4)||n.getInt("Phase")==1&&n.getInt("Score")>=8||n.getInt("Phase")==2&&n.getInt("Score")!=8)return false;try{var epoch=generator.structureGeneration();return epoch.id().equals(plan.generation())&&epoch.seed()==plan.seed()&&InfiniteWorldData.get(server.getServer()).matchesGeneration(epoch.id());}catch(IllegalStateException unavailable){return false;}}
    boolean questAvailable(){return authentic();}
    boolean complete(){return authentic()&&data().getInt("Phase")==2;}
    int expected(){return authentic()&&data().getInt("Phase")==1?CarnivalLayout.expected(plan,data().getInt("Score")):-1;}
    boolean member(CarnivalTargetEntity target){if(!authentic()||data().getInt("Phase")!=1||target.level()!=level)return false;var n=target.marker();int index=n.getInt("Index");var ids=data().getList("Targets",Tag.TAG_INT_ARRAY);return index>=0&&index<4&&n.hasUUID("Generation")&&plan.generation().equals(n.getUUID("Generation"))&&n.hasUUID("Site")&&plan.site().equals(n.getUUID("Site"))&&n.hasUUID("Session")&&data().getUUID("Session").equals(n.getUUID("Session"))&&ids.size()==4&&NbtUtils.loadUUID(ids.get(index)).equals(target.getUUID());}
    void position(CarnivalTargetEntity target){if(!member(target))return;int index=target.marker().getInt("Index"),time=data().getInt("Ticks");target.setPos(plan.x()-5.4+index*3.6+Math.sin(time*.055+index*1.7)*.75,plan.y()+2.2+Math.sin(time*.04+index)*.25,plan.z()+8);}
    private static void tell(ServerPlayer p,String key){p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.coraline."+key),true);}
    private void sync(){var state=getBlockState();var next=state.setValue(CarnivalBlock.PHASE,data().getInt("Phase"));if(!state.equals(next))level.setBlock(worldPosition,next,3);}
    private void abort(){data().putInt("Phase",0);data().putLong("RetryAfter",level.getGameTime()+100);setChanged();sync();}
    void start(ServerPlayer player){
        if(!authentic()||!player.isAlive()||player.isCreative()||player.isSpectator()||player.level()!=level||player.distanceToSqr(worldPosition.getCenter())>36||!level.mayInteract(player,worldPosition)||!InfiniteWorldData.get(player.server).accessible())return;if(complete()){tell(player,"carnival_complete");return;}if(data().getInt("Phase")==1){tell(player,"carnival_active");return;}if(level.getGameTime()<data().getLong("RetryAfter")){tell(player,"carnival_retry");return;}
        var targets=new ArrayList<CarnivalTargetEntity>();var ids=new ListTag();for(int i=0;i<4;i++){var target=com.coraline.registry.ModEntities.CARNIVAL_TARGET.get().create(level);if(target==null)return;targets.add(target);ids.add(NbtUtils.createUUID(target.getUUID()));}
        data().putUUID("Owner",player.getUUID());data().putUUID("Session",UUID.randomUUID());data().put("Targets",ids);data().putInt("Phase",1);data().putInt("Score",0);data().putInt("Ticks",0);data().putLong("LastTick",level.getGameTime());setChanged();sync();
        for(int i=0;i<targets.size();i++){var marker=new CompoundTag();marker.putInt("Version",1);marker.putUUID("Generation",plan.generation());marker.putUUID("Site",plan.site());marker.putUUID("Session",data().getUUID("Session"));marker.putLong("Control",worldPosition.asLong());marker.putInt("Index",i);var target=targets.get(i);target.configure(this,marker);if(!level.hasChunkAt(target.blockPosition())||!level.addFreshEntity(target)){abort();return;}}
        tell(player,"carnival_started");
    }
    boolean hit(CarnivalTargetEntity target,ServerPlayer player){
        if(!member(target)||!data().getUUID("Owner").equals(player.getUUID())||player.level()!=level||!player.isAlive()||player.isCreative()||player.isSpectator()||Math.abs(player.getX()-plan.x())>5.5||player.getZ()<plan.z()-3.5||player.getZ()>plan.z()+.5||Math.abs(player.getY()-plan.y()-1)>1.5)return false;
        if(target.color()!=expected()){player.playNotifySound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS.get(),net.minecraft.sounds.SoundSource.BLOCKS,.5F,.8F);return false;}
        int score=data().getInt("Score")+1;data().putInt("Score",score);setChanged();player.playNotifySound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME.get(),net.minecraft.sounds.SoundSource.BLOCKS,.7F,1+target.color()*.12F);
        if(score==8){data().putInt("Phase",2);sync();unlockReward();PostgameQuests.completed(player,plan);tell(player,"carnival_complete");}else player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.coraline.carnival_score",score),true);return true;
    }
    private void unlockReward(){if(!complete())return;for(int i=0;i<plan.modules().size();i++)if(plan.modules().get(i).role()==InfiniteStructurePlan.Role.CARNIVAL_VAULT){var at=SiteCacheReceipt.position(plan,i);if(level.hasChunkAt(at)&&level.getBlockEntity(at) instanceof SiteCacheBlockEntity cache)cache.unlock(plan.site());}}
    static void tick(Level level,BlockPos at,BlockState state,CarnivalEntity game){
        if(!game.authentic())return;if(level.getGameTime()%20==0)game.sync();if(game.complete()){if(level.getGameTime()%20==0)game.unlockReward();return;}if(game.data().getInt("Phase")!=1)return;long now=level.getGameTime(),last=game.data().getLong("LastTick");if(now==last)return;if(now<last){game.abort();return;}game.data().putLong("LastTick",now);
        var player=level.getServer().getPlayerList().getPlayer(game.data().getUUID("Owner"));if(player==null||player.level()!=level||!player.isAlive()||player.isCreative()||player.isSpectator()||player.distanceToSqr(at.getCenter())>40*40){game.abort();return;}int time=game.data().getInt("Ticks")+1;game.data().putInt("Ticks",time);game.setChanged();if(time>=1200){game.abort();tell(player,"carnival_timeout");}
    }
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);n.put("CoralineCarnival",raw.copy());}
    @Override public void load(CompoundTag n){super.load(n);raw=n.contains("CoralineCarnival")?n.get("CoralineCarnival").copy():new CompoundTag();plan=null;if(raw instanceof CompoundTag tag)CarnivalLayout.read(tag).filter(p->CarnivalLayout.core(p).equals(worldPosition)).ifPresent(p->plan=p);}
}
