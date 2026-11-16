package com.coraline.tycoon;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** One resumable bounded scenery job per owner, with exact-state rollback.
 * Money is charged only after successful generation, through a durable receipt.
 */
public final class TycoonBuildJob {
    private static final Map<UUID,UUID> RUNNING=new HashMap<>();
    static boolean generatedMatch(BlockState actual,BlockState expected){
        if(expected==null)return false;if(actual.equals(expected))return true;
        // Vanilla shade/spread changes generated grass to dirt and back. The
        // caller still rejects every recorded PlayerEdit before accepting this
        // narrow soil transition; never adopt arbitrary non-generated blocks.
        return (expected.is(Blocks.GRASS_BLOCK)||expected.is(Blocks.DIRT))&&(actual.is(Blocks.GRASS_BLOCK)||actual.is(Blocks.DIRT));
    }
    public static void clear(UUID owner){RUNNING.remove(owner);}
    public static String begin(ServerPlayer p,String name,CompoundTag previous,Map<BlockPos,BlockState> next,long origin,CompoundTag patch,int cost){
        return begin(p,name,previous,next,origin,patch,cost,ItemStack.EMPTY,false,0);
    }
    public static String beginPriced(ServerPlayer p,String name,CompoundTag previous,Map<BlockPos,BlockState> next,long origin,CompoundTag patch,int cost,long soulCost){return begin(p,name,previous,next,origin,patch,cost,ItemStack.EMPTY,false,soulCost);}
    public static String beginRemoval(ServerPlayer p,String name,CompoundTag previous,Map<BlockPos,BlockState> next,long origin,CompoundTag patch,ItemStack refund){
        return begin(p,name,previous,next,origin,patch,0,refund,true,0);
    }
    private static String begin(ServerPlayer p,String name,CompoundTag previous,Map<BlockPos,BlockState> next,long origin,CompoundTag patch,int cost,ItemStack refund,boolean removal,long soulCost){
        var h=TycoonWorld.home(p);if(h.state.contains("BuildJob"))return "Construction is already in progress.";
        boolean automated=TycoonWorkers.constructing(p);if(automated&&!TycoonWorkers.canConstruct(p,cost,soulCost))return "Worker construction permission, budget or real supplies unavailable.";
        if(soulCost<0||h.state.getLong("SoulEssence")<soulCost)return "Requires "+soulCost+" Soul Essence. Nothing spent.";
        if(!automated&&!TycoonShop.has(p,TycoonContent.VOID_POWER.get(),cost))return "Requires "+cost+" Void Power.";
        var l=p.server.getLevel(TycoonWorld.KEY);if(l==null)return "Personal world unavailable; nothing spent.";
        var generated=TycoonLandscape.decode(l,previous);var house=TycoonLandscape.decode(l,h.state.getCompound("HouseGenerated"));var terrainCache=new HashMap<String,Map<BlockPos,BlockState>>();var before=new LinkedHashMap<BlockPos,BlockState>();var after=new LinkedHashMap<BlockPos,BlockState>();
        var structures=TycoonAttractions.generated(l,h);var positions=new LinkedHashSet<BlockPos>(generated.keySet());positions.addAll(next.keySet());var protectedCells=h.state.getCompound("PlayerEdits");
        for(var at:positions){l.getChunkAt(at);var old=l.getBlockState(at);var wanted=next.getOrDefault(at,Blocks.AIR.defaultBlockState());
            // Terrain provenance describes the ground beneath a house. Keep the
            // complete canonical house envelope (including clear interior air),
            // rather than planting trees through it or erasing its floor.
            // Changed cells remain protected: never adopt a player's edits as
            // generated house material.
            if(name.startsWith("terrain ")&&structures.containsKey(at)){
                if(!old.equals(structures.get(at))||protectedCells.contains(Long.toString(at.asLong())))return "Construction conflicts with a changed attraction cell at "+at.toShortString()+". Nothing spent.";
                continue;
            }
            if(name.startsWith("terrain ")&&house.containsKey(at)){
                if(!old.equals(house.get(at))||protectedCells.contains(Long.toString(at.asLong()))||l.getBlockEntity(at)!=null)return "Construction conflicts with a changed house cell at "+at.toShortString()+". Nothing spent.";
                continue;
            }
            if(old.equals(wanted))continue;
            String tile=TycoonLandscape.key(TycoonLandscape.tileX(h,at),TycoonLandscape.tileZ(h,at));var ground=terrainCache.computeIfAbsent(tile,k->TycoonLandscape.decode(l,h.state.getCompound("Terrain").getCompound(k)));
            boolean replaceGeneratedGround=name.startsWith("attraction ")||name.startsWith("Simple Cottage")||name.startsWith("Comfortable House")||name.startsWith("Elegant House")||name.startsWith("Grand Other House")||name.startsWith("Magnificent Mansion");
            boolean emptyOwnedCabinet=removal&&generatedMatch(old,generated.get(at))&&l.getBlockEntity(at) instanceof TycoonBlockEntity be&&TycoonWorld.owner(p).equals(be.owner)&&be.isEmpty();
            boolean repairCell=name.startsWith("attraction repair ")&&old.isAir()&&wanted.equals(generated.get(at));
            if(protectedCells.contains(Long.toString(at.asLong()))&&!repairCell||l.getBlockEntity(at)!=null&&!emptyOwnedCabinet||!old.isAir()&&!generatedMatch(old,generated.get(at))&&!(replaceGeneratedGround&&generatedMatch(old,ground.get(at))))return "Construction conflicts with a protected edit, inventory or non-generated block at "+at.toShortString()+". Nothing spent.";
            if(!wanted.getCollisionShape(l,at).isEmpty()&&!l.getEntities((net.minecraft.world.entity.Entity)null,new net.minecraft.world.phys.AABB(at),e->e instanceof net.minecraft.world.entity.LivingEntity).isEmpty())return "Move away from the construction footprint before confirming.";
            before.put(at,old);after.put(at,wanted);
        }
        var job=new CompoundTag();job.putUUID("Id",UUID.randomUUID());job.putString("Name",name);job.putInt("Cost",cost);job.putLong("SoulCost",soulCost);if(!refund.isEmpty())job.put("Refund",refund.save(new CompoundTag()));job.putBoolean("Removal",removal);job.put("Before",TycoonLandscape.encode(before,BlockPos.of(origin)));job.put("After",TycoonLandscape.encode(after,BlockPos.of(origin)));job.put("Patch",patch.copy());job.putInt("Cursor",0);h.state.put("BuildJob",job);TycoonWorld.dirty(p);
        if(automated)job.putUUID("Worker",TycoonWorkers.contextWorker(p));
        try{if(!TycoonCheckpoints.home(p)){h.state.remove("BuildJob");return "Construction intent could not be saved; nothing spent.";}}catch(java.io.IOException failure){h.state.remove("BuildJob");return "Construction checkpoint unavailable; nothing spent.";}
        return "Building "+name+". Cost "+cost+" Void Power is charged only on success.";
    }
    public static boolean touches(TycoonData.Home h,BlockPos at){if(!h.state.contains("BuildJob"))return false;var n=h.state.getCompound("BuildJob").getCompound("After");var c=BlockPos.of(n.getLong("Origin"));return Math.abs(c.getX()-at.getX())<33&&Math.abs(c.getZ()-at.getZ())<33&&Math.abs(c.getY()-at.getY())<48;}
    public static void tick(ServerPlayer p){
        var h=TycoonWorld.home(p);if(!h.state.contains("BuildJob"))return;var job=h.state.getCompound("BuildJob");var l=p.server.getLevel(TycoonWorld.KEY);if(l==null)return;
        if(h.state.hasUUID("CompletedBuild")&&h.state.getUUID("CompletedBuild").equals(job.getUUID("Id"))){if(job.getString("Name").startsWith("attraction repair "))for(var at:TycoonLandscape.decode(l,job.getCompound("After")).keySet())h.state.getCompound("PlayerEdits").remove(Long.toString(at.asLong()));h.state.remove("BuildJob");RUNNING.remove(TycoonWorld.owner(p));TycoonLandscape.recalculate(h);TycoonWorld.dirty(p);return;}
        if(!job.getUUID("Id").equals(RUNNING.get(TycoonWorld.owner(p)))){job.putInt("Cursor",0);RUNNING.put(TycoonWorld.owner(p),job.getUUID("Id"));}
        var before=TycoonLandscape.decode(l,job.getCompound("Before"));var after=TycoonLandscape.decode(l,job.getCompound("After"));var entries=new ArrayList<>(after.entrySet());int cursor=job.getInt("Cursor");
        for(int count=0;count<512&&cursor<entries.size();count++,cursor++){
            var e=entries.get(cursor);l.getChunkAt(e.getKey());var old=l.getBlockState(e.getKey());if(old.equals(e.getValue()))continue;
            boolean repairCell=job.getString("Name").startsWith("attraction repair ")&&old.isAir();
            if(!old.equals(before.get(e.getKey()))||h.state.getCompound("PlayerEdits").contains(Long.toString(e.getKey().asLong()))&&!repairCell){rollback(p,"Construction interrupted by a changed footprint; nothing spent.");return;}
            if(job.getBoolean("Removal")&&l.getBlockEntity(e.getKey()) instanceof TycoonBlockEntity be&&(!TycoonWorld.owner(p).equals(be.owner)||!be.isEmpty())){rollback(p,"A cabinet changed during removal; nothing spent.");return;}
            if(!e.getValue().getCollisionShape(l,e.getKey()).isEmpty()&&!l.getEntities((net.minecraft.world.entity.Entity)null,new net.minecraft.world.phys.AABB(e.getKey()),actor->actor instanceof net.minecraft.world.entity.LivingEntity).isEmpty()){
                job.putInt("Cursor",cursor);TycoonWorld.message(p,"Construction paused: move away from the next block before work resumes.");return;
            }
            l.setBlock(e.getKey(),e.getValue(),2|16);
        }
        job.putInt("Cursor",cursor);TycoonWorld.dirty(p);if(cursor<entries.size())return;
        p.server.saveEverything(true,true,true);
        if(!TycoonSoulFarms.constructionValid(h,job.getCompound("Patch"))){rollback(p,"Containment changed during construction; secure the breach first. Nothing spent.");return;}
        var patch=job.getCompound("Patch").copy();long soulCost=job.getLong("SoulCost");if(soulCost>0){long balance=h.state.getLong("SoulEssence");if(balance<soulCost){rollback(p,"Soul Essence changed before construction payment; nothing spent.");return;}patch.putLong("SoulEssence",balance-soulCost);}patch.putUUID("CompletedBuild",job.getUUID("Id"));patch.putString("LastBuildReceipt",job.getString("Name"));String result=job.hasUUID("Worker")?TycoonWorkers.payConstruction(p,job.getUUID("Worker"),job.getInt("Cost"),soulCost,patch):TycoonTransactions.purchase(p,job.getInt("Cost"),ItemStack.of(job.getCompound("Refund")),patch,null,new CompoundTag());
        if(!result.startsWith("Purchased")&&!result.startsWith("Purchase receipt")){rollback(p,result);return;}
        if(job.getString("Name").startsWith("attraction repair "))for(var at:after.keySet())h.state.getCompound("PlayerEdits").remove(Long.toString(at.asLong()));
        h.state.remove("BuildJob");RUNNING.remove(TycoonWorld.owner(p));TycoonLandscape.recalculate(h);h.milestone("world_scenery",25);TycoonWorld.message(p,"Completed "+job.getString("Name")+". "+result);TycoonWorld.dirty(p);
    }
    public static void rollback(ServerPlayer p,String reason){var h=TycoonWorld.home(p);var job=h.state.getCompound("BuildJob");var l=p.server.getLevel(TycoonWorld.KEY);if(l!=null){var before=TycoonLandscape.decode(l,job.getCompound("Before"));var after=TycoonLandscape.decode(l,job.getCompound("After"));for(var e:before.entrySet())if(l.getBlockState(e.getKey()).equals(after.get(e.getKey()))&&(!h.state.getCompound("PlayerEdits").contains(Long.toString(e.getKey().asLong()))||job.getString("Name").startsWith("attraction repair ")&&e.getValue().isAir()))l.setBlock(e.getKey(),e.getValue(),2|16);}h.state.remove("BuildJob");TycoonWorld.dirty(p);TycoonWorld.message(p,reason);}
    private TycoonBuildJob(){}
}
