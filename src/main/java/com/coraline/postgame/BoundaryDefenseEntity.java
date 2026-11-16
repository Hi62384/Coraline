package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Finite world encounter. Missing/unloaded attackers never count as kills;
 * surviving 1,200 actual nearby ticks is an independent defense victory. */
public final class BoundaryDefenseEntity extends BlockEntity {
    private Tag raw=new CompoundTag();private InfiniteStructurePlan.Plan plan;
    public BoundaryDefenseEntity(BlockPos at,BlockState state){super(com.coraline.registry.ModBlockEntities.BOUNDARY_DEFENSE.get(),at,state);}
    private CompoundTag data(){return raw instanceof CompoundTag n?n:new CompoundTag();}
    void configure(InfiniteStructurePlan.Plan plan){if(this.plan!=null||!BoundaryDefenseLayout.family(plan.family())||!BoundaryDefenseLayout.core(plan).equals(worldPosition)||!(raw instanceof CompoundTag n)||!n.isEmpty())return;this.plan=plan;raw=BoundaryDefenseLayout.metadata(plan);data().putInt("Stage",0);data().putInt("Integrity",integrityLimit());data().putInt("Ticks",0);data().putLong("LastTick",-1);data().putLong("RetryAfter",0);data().put("Members",new ListTag());setChanged();}
    boolean majorHeart(){return plan!=null&&plan.family()==InfiniteStructurePlan.Family.ENTITY_HEART_CHAMBER;}
    java.util.UUID site(){return plan==null?null:plan.site();}
    java.util.UUID completion(){return data().hasUUID("Completion")?data().getUUID("Completion"):null;}
    private int integrityLimit(){return majorHeart()?64:32;}
    private int duration(){return majorHeart()?2400:1200;}
    private int memberLimit(){return majorHeart()?8:4;}
    private boolean valid(){
        if(plan==null)return false;var n=data();for(String key:List.of("Stage","Integrity","Ticks"))if(!n.contains(key,Tag.TAG_INT))return false;
        if(n.getInt("Stage")<0||n.getInt("Stage")>3||n.getInt("Integrity")<0||n.getInt("Integrity")>integrityLimit()||n.getInt("Ticks")<0||n.getInt("Ticks")>duration()||!n.contains("LastTick",Tag.TAG_LONG)||n.getLong("LastTick")< -1||!n.contains("RetryAfter",Tag.TAG_LONG)||n.getLong("RetryAfter")<0||!n.contains("Members",Tag.TAG_LIST))return false;
        var list=n.getList("Members",Tag.TAG_COMPOUND);if(list.size()!=((ListTag)n.get("Members")).size()||list.size()>memberLimit())return false;var ids=new HashSet<UUID>();for(var raw:list){var row=(CompoundTag)raw;if(!row.hasUUID("Entity")||!ids.add(row.getUUID("Entity"))||!row.contains("Dead",Tag.TAG_BYTE)||row.getByte("Dead")<0||row.getByte("Dead")>1)return false;}
        if(majorHeart()&&n.getInt("Stage")==3){if(!n.hasUUID("Completion")||!n.contains("Outcome",Tag.TAG_STRING))return false;try{BreachState.Resolution.valueOf(n.getString("Outcome"));}catch(IllegalArgumentException bad){return false;}}
        return n.getInt("Stage")==0?list.isEmpty()&&!n.contains("Session")&&!n.contains("Owner"):n.hasUUID("Session")&&n.hasUUID("Owner")&&list.size()==memberLimit();
    }
    boolean authentic(){if(!valid()||isRemoved()||!(level instanceof ServerLevel server)||!getBlockState().is(com.coraline.registry.ModBlocks.BOUNDARY_BEACON.get())||server.getBlockEntity(worldPosition)!=this||!(server.getChunkSource().getGenerator() instanceof InfiniteChunkGenerator generator))return false;try{var epoch=generator.structureGeneration();return epoch.id().equals(plan.generation())&&epoch.seed()==plan.seed()&&InfiniteWorldData.get(server.getServer()).matchesGeneration(epoch.id());}catch(IllegalStateException unavailable){return false;}}
    boolean complete(){return authentic()&&data().getInt("Stage")==3;}
    boolean fighting(){return authentic()&&data().getInt("Stage")==1;}
    boolean member(Mob mob,CompoundTag marker){return fighting()&&marker.hasUUID("Session")&&data().getUUID("Session").equals(marker.getUUID("Session"))&&marker.hasUUID("Generation")&&plan.generation().equals(marker.getUUID("Generation"))&&marker.hasUUID("Site")&&plan.site().equals(marker.getUUID("Site"))&&data().getList("Members",Tag.TAG_COMPOUND).stream().anyMatch(n->((CompoundTag)n).getUUID("Entity").equals(mob.getUUID())&&!((CompoundTag)n).getBoolean("Dead"));}
    void killed(Mob mob,CompoundTag marker){if(!member(mob,marker))return;for(var raw:data().getList("Members",Tag.TAG_COMPOUND)){var n=(CompoundTag)raw;if(n.getUUID("Entity").equals(mob.getUUID())){n.putBoolean("Dead",true);setChanged();return;}}}
    void hit(){if(!fighting())return;data().putInt("Integrity",Math.max(0,data().getInt("Integrity")-1));setChanged();}
    void start(ServerPlayer player){
        if(!authentic()||player.isCreative()||player.isSpectator()||!player.isAlive()||player.level()!=level||player.distanceToSqr(worldPosition.getCenter())>36||!level.mayInteract(player,worldPosition)||!InfiniteWorldData.get(player.server).accessible())return;
        if(complete()){tell(player,"boundary_complete");return;}if(fighting()){tell(player,"boundary_active");return;}long now=level.getGameTime();if(now<data().getLong("RetryAfter")){tell(player,"boundary_retry");return;}
        var mobs=new ArrayList<Mob>();var spawns=new ArrayList<BlockPos>();for(int x:new int[]{-4,4})for(int z:new int[]{-4,4})spawns.add(worldPosition.offset(x,0,z));if(majorHeart())for(int[] off:new int[][]{{-7,0},{7,0},{0,-7},{0,7}})spawns.add(worldPosition.offset(off[0],0,off[1]));for(var at:spawns){
            var mob=PostgameHostiles.TYPES.get(PostgameHostiles.Kind.RIFT_SOLDIER).get().create(level);if(mob==null)return;mob.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
            if(!level.hasChunkAt(at)||!level.getWorldBorder().isWithinBounds(at)||!level.noCollision(mob,mob.getBoundingBox())||level.containsAnyLiquid(mob.getBoundingBox())){tell(player,"boundary_clear_space");return;}mobs.add(mob);
        }
        var members=new ListTag();var session=UUID.randomUUID();for(var mob:mobs){var row=new CompoundTag();row.putUUID("Entity",mob.getUUID());row.putBoolean("Dead",false);members.add(row);}
        data().put("Members",members);data().putUUID("Session",session);data().putUUID("Owner",player.getUUID());data().putInt("Stage",1);data().putInt("Ticks",0);data().putInt("Integrity",integrityLimit());data().putLong("LastTick",now);setChanged();sync();
        for(var mob:mobs){var marker=new CompoundTag();marker.putInt("Version",1);marker.putUUID("Generation",plan.generation());marker.putUUID("Site",plan.site());marker.putUUID("Session",session);marker.putLong("Beacon",worldPosition.asLong());mob.getPersistentData().put(BoundaryRaiders.KEY,marker);mob.setPersistenceRequired();if(!level.addFreshEntity(mob)){fail();return;}}
        tell(player,majorHeart()?"heart_started":"boundary_started");
    }
    private void fail(){data().putInt("Stage",2);data().putLong("RetryAfter",level.getGameTime()+200);setChanged();sync();}
    private void win(){data().putInt("Stage",3);if(majorHeart()){data().putUUID("Completion",UUID.randomUUID());data().putString("Outcome",BreachState.Resolution.REFUSED.name());var outcome=BreachWorldData.get(level.getServer()).resolveHeart((ServerLevel)level,this);data().putString("Outcome",outcome.name());}setChanged();sync();unlockReward();var owner=level.getServer().getPlayerList().getPlayer(data().getUUID("Owner"));if(owner!=null){PostgameQuests.completed(owner,plan);tell(owner,majorHeart()?"heart_victory":"boundary_victory");}}
    private void unlockReward(){if(!complete())return;for(int i=0;i<plan.modules().size();i++)if(plan.modules().get(i).role()==InfiniteStructurePlan.Role.DEFENSE_VAULT){var at=SiteCacheReceipt.position(plan,i);if(level.hasChunkAt(at)&&level.getBlockEntity(at) instanceof SiteCacheBlockEntity cache)cache.unlock(plan.site());}}
    private void sync(){var state=getBlockState();var next=state.setValue(BoundaryDefenseBlock.STAGE,data().getInt("Stage"));if(!state.equals(next))level.setBlock(worldPosition,next,3);}
    private static void tell(ServerPlayer p,String key){p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.coraline."+key),true);}
    static void tick(Level level,BlockPos at,BlockState state,BoundaryDefenseEntity defense){
        if(!defense.authentic())return;if(level.getGameTime()%20==0)defense.sync();if(defense.complete()){if(level.getGameTime()%20==0)defense.unlockReward();return;}if(!defense.fighting())return;
        long now=level.getGameTime(),last=defense.data().getLong("LastTick");if(now==last)return;if(now<last){defense.fail();return;}defense.data().putLong("LastTick",now);
        var owner=level.getServer().getPlayerList().getPlayer(defense.data().getUUID("Owner"));if(owner==null||owner.level()!=level||!owner.isAlive()||owner.isSpectator()||owner.isCreative()||owner.distanceToSqr(at.getCenter())>24*24){defense.fail();return;}
        if(defense.data().getInt("Integrity")<=0){tell(owner,"boundary_failed");defense.fail();return;}defense.data().putInt("Ticks",defense.data().getInt("Ticks")+1);defense.setChanged();
        boolean all=defense.data().getList("Members",Tag.TAG_COMPOUND).stream().allMatch(n->((CompoundTag)n).getBoolean("Dead"));if(all||!defense.majorHeart()&&defense.data().getInt("Ticks")>=defense.duration()){defense.win();return;}if(defense.data().getInt("Ticks")>=defense.duration()){tell(owner,"heart_timeout");defense.fail();return;}
        if(now%20==0)owner.displayClientMessage(net.minecraft.network.chat.Component.translatable(defense.majorHeart()?"message.coraline.heart_status":"message.coraline.boundary_status",(defense.duration()-defense.data().getInt("Ticks")+19)/20,defense.data().getInt("Integrity")),true);
    }
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);n.put("CoralineBoundaryDefense",raw.copy());}
    @Override public void load(CompoundTag n){super.load(n);raw=n.contains("CoralineBoundaryDefense")?n.get("CoralineBoundaryDefense").copy():new CompoundTag();plan=null;if(raw instanceof CompoundTag tag)BoundaryDefenseLayout.read(tag).filter(p->BoundaryDefenseLayout.core(p).equals(worldPosition)).ifPresent(p->plan=p);}
}
