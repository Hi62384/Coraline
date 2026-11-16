package com.coraline.tycoon;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Paid, bounded converters and ingredient-consuming Void Power extractors. */
public final class TycoonMachines {
    public static CompoundTag machines(TycoonData.Home h){return h.state.getCompound("Machines");}
    public static Map<BlockPos,BlockState> plan(BlockPos origin,boolean extractor,int level){
        var out=new LinkedHashMap<BlockPos,BlockState>();for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=0;y<=3;y++)out.put(origin.offset(x,y,z),(y==0?Blocks.DEEPSLATE_TILES:Blocks.AIR).defaultBlockState());
        for(int x:new int[]{-2,2})for(int z:new int[]{-2,2})for(int y=1;y<=2;y++)out.put(origin.offset(x,y,z),(y==1?Blocks.WAXED_CUT_COPPER:Blocks.IRON_BARS).defaultBlockState());
        out.put(origin.above(),(extractor?Blocks.BLAST_FURNACE:Blocks.CRYING_OBSIDIAN).defaultBlockState());out.put(origin.above(2),(extractor?Blocks.IRON_BLOCK:Blocks.PURPLE_STAINED_GLASS).defaultBlockState());out.put(origin.above(3),Blocks.SEA_LANTERN.defaultBlockState());
        for(int i=0;i<level;i++)out.put(origin.offset(-2+i,1,2),(extractor?Blocks.COPPER_BLOCK:Blocks.AMETHYST_BLOCK).defaultBlockState());return out;
    }
    public static Map<BlockPos,BlockState> generated(ServerLevel level,TycoonData.Home h){var result=new LinkedHashMap<BlockPos,BlockState>();var all=machines(h);for(String key:all.getAllKeys())result.putAll(TycoonLandscape.decode(level,all.getCompound(key).getCompound("Generated")));return result;}
    public static String build(ServerPlayer p,String type,int tx,int tz){
        var h=TycoonWorld.home(p);var l=p.server.getLevel(TycoonWorld.KEY);if(l==null||p.serverLevel()!=l||!Set.of("converter","extractor").contains(type)||!TycoonLandscape.inBounds(tx,tz)||machines(h).size()>=TycoonAutomationConfig.MAX_CONVERTERS.get())return "Invalid owned machine plot or machine limit reached.";
        int x=tx*16+8,z=tz*16+8,y=h.center().getY()-1;for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)y=Math.max(y,TycoonLandscape.height(h,x+dx,z+dz));var origin=h.center().offset(x,y-h.center().getY(),z);var plan=plan(origin,type.equals("extractor"),1);var other=TycoonAttractions.generated(l,h);var house=TycoonLandscape.decode(l,h.state.getCompound("HouseGenerated"));var ground=new LinkedHashMap<BlockPos,BlockState>();
        for(var at:plan.keySet()){if(!TycoonLandscape.owned(h,at)||TycoonLandscape.core(h,at)||other.containsKey(at)||house.containsKey(at))return "Machine footprint overlaps protected terrain or another structure.";ground.put(at,l.getBlockState(at));}
        for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){var at=origin.offset(dx,0,dz);int gy=TycoonLandscape.height(h,at.getX()-h.center().getX(),at.getZ()-h.center().getZ());for(int yy=gy;yy<origin.getY();yy++){var base=new BlockPos(at.getX(),yy,at.getZ());ground.put(base,l.getBlockState(base));plan.put(base,Blocks.DEEPSLATE_BRICKS.defaultBlockState());}}
        var row=new CompoundTag();var id=UUID.randomUUID();row.putUUID("Id",id);row.putUUID("Owner",h.owner);row.putString("Type",type);row.putLong("Position",origin.asLong());row.putInt("Unlocked",1);row.putInt("Operating",1);row.putString("Recipe","minecraft:oak_log");row.putBoolean("Break",true);row.put("Generated",TycoonLandscape.encode(plan,origin));row.put("Ground",TycoonLandscape.encode(ground,origin));var changes=new CompoundTag();changes.put(id.toString(),row);var patch=new CompoundTag();patch.put("MachineChanges",changes);
        return TycoonBuildJob.begin(p,"attraction "+type,new CompoundTag(),plan,origin.asLong(),patch,type.equals("extractor")?40:32);
    }
    public static String configure(ServerPlayer p,UUID id,String key,String value){var h=TycoonWorld.home(p);var row=machines(h).getCompound(id.toString());if(row.isEmpty())return "Machine unavailable.";switch(key){
        case "recipe"->{if(!TycoonAutomationConfig.OUTPUTS.containsKey(value))return "Only configured survival-resource recipes are allowed.";row.putString("Recipe",value);row.putInt("Progress",0);}
        case "level"->{int level=Integer.parseInt(value);if(level<1||level>row.getInt("Unlocked"))return "Operating level is not unlocked.";row.putInt("Operating",level);}
        case "break"->{row.putBoolean("Break",Boolean.parseBoolean(value));row.putBoolean("ManualBreak",Boolean.parseBoolean(value));}
        default->{return "Unknown machine policy.";}
    }TycoonWorld.dirty(p);return "Machine setting saved.";}
    public static String upgrade(ServerPlayer p,UUID id){
        var h=TycoonWorld.home(p);var row=machines(h).getCompound(id.toString());var l=p.server.getLevel(TycoonWorld.KEY);if(l==null||p.serverLevel()!=l||row.isEmpty()||row.getInt("Unlocked")>=5)return "Select an owned machine below Level 5.";int level=row.getInt("Unlocked")+1;var origin=TycoonAttractions.origin(row);var plan=plan(origin,row.getString("Type").equals("extractor"),level);for(var e:TycoonLandscape.decode(l,row.getCompound("Generated")).entrySet())if(e.getKey().getY()<origin.getY())plan.put(e.getKey(),e.getValue());var fields=new CompoundTag();fields.putInt("Unlocked",level);fields.put("Generated",TycoonLandscape.encode(plan,origin));var changes=new CompoundTag();changes.put(id.toString(),fields);var patch=new CompoundTag();patch.put("MachineChanges",changes);return TycoonBuildJob.begin(p,"attraction machine upgrade",row.getCompound("Generated"),plan,origin.asLong(),patch,12*level);
    }
    public static long producedCount(TycoonData.Home h){long total=0;var stock=h.state.getCompound("ProducedResources");for(String key:stock.getAllKeys())total+=Math.max(0,stock.getLong(key));return total;}
    public static void tick(ServerPlayer p){
        var h=TycoonWorld.home(p);var l=p.server.getLevel(TycoonWorld.KEY);if(l==null)return;
        for(String key:machines(h).getAllKeys()){
            var row=machines(h).getCompound(key);if(row.getLong("LastTick")==h.clock())continue;row.putLong("LastTick",h.clock());if(row.getBoolean("Break")){row.putString("Status","ON_BREAK");continue;}
            var origin=TycoonAttractions.origin(row);if(TycoonBuildJob.touches(h,origin)){row.putString("Status","CONSTRUCTION");continue;}
            if(l.hasChunkAt(origin)&&p.tickCount%100==0){boolean valid=true;for(var e:TycoonLandscape.decode(l,row.getCompound("Generated")).entrySet())if(!e.getValue().isAir()&&!l.getBlockState(e.getKey()).equals(e.getValue())){valid=false;break;}row.putBoolean("Verified",valid);}
            if(!row.getBoolean("Verified")){row.putString("Status","DAMAGED_OR_UNVERIFIED");continue;}
            int progress=Math.min(1200,row.getInt("Progress")+20);row.putInt("Progress",progress);row.putString("Status","WORKING");
            if(progress>=1200){if(row.getString("Type").equals("extractor"))extract(p,row);else convert(p,row);}
            if(p.serverLevel()==l&&p.distanceToSqr(origin.getCenter())<48*48)l.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,origin.getX()+.5,origin.getY()+2,origin.getZ()+.5,3,.5,.4,.5,.03);
        }TycoonWorld.dirty(p);
    }
    private static void convert(ServerPlayer p,CompoundTag row){
        var h=TycoonWorld.home(p);String recipe=row.getString("Recipe");if(!TycoonAutomationConfig.OUTPUTS.containsKey(recipe)){row.putString("Status","INVALID_RECIPE");return;}
        int operating=Math.max(1,Math.min(row.getInt("Unlocked"),row.getInt("Operating")));double multiplier=TycoonAutomationConfig.CONVERTER_MULTIPLIERS.get(operating-1).get();int amount=(int)Math.floor(TycoonAutomationConfig.OUTPUTS.get(recipe).get()*multiplier);long cost=Math.round(TycoonAutomationConfig.OUTPUT_COSTS.get(recipe).get()*multiplier*TycoonSoulLedger.SCALE);
        if(producedCount(h)>TycoonWarehouse.capacity(h)-amount){row.putString("Status","OUTPUT_FULL");return;}
        long budget=TycoonPolicies.number(h,"converterBudgetHour")*TycoonSoulLedger.SCALE;if(!TycoonPolicies.budget(h,"converters",cost,budget)||!TycoonSoulLedger.debit(h,cost,TycoonPolicies.number(h,"reserve"))){row.putString("Status","INSUFFICIENT_FUNDS_OR_BUDGET");return;}
        var stock=h.state.getCompound("ProducedResources");stock.putLong(recipe,stock.getLong(recipe)+amount);h.state.put("ProducedResources",stock);row.putInt("Progress",0);row.putLong("Produced",row.getLong("Produced")+amount);row.putLong("PaidMicros",row.getLong("PaidMicros")+cost);TycoonPolicies.spent(h,"converters",cost);TycoonFinance.record(h,"converter",-cost);TycoonWorld.dirty(p);
    }
    private static void extract(ServerPlayer p,CompoundTag row){
        var h=TycoonWorld.home(p);int batches=Math.max(1,Math.min(row.getInt("Unlocked"),row.getInt("Operating")));long cost=500_000L*batches;
        if(TycoonWarehouse.total(h)>TycoonWarehouse.capacity(h)-4L*batches){row.putString("Status","SUPPLY_STORAGE_FULL");return;}
        for(var item:List.of(Items.AMETHYST_SHARD,Items.COAL,Items.REDSTONE))if(supply(h,item)<batches){row.putString("Status","MISSING_REAL_INGREDIENTS");return;}
        if(!TycoonSoulLedger.debit(h,cost,TycoonPolicies.number(h,"reserve"))){row.putString("Status","INSUFFICIENT_FUNDS");return;}
        for(var item:List.of(Items.AMETHYST_SHARD,Items.COAL,Items.REDSTONE))takeSupply(h,item,batches);TycoonWarehouse.add(h,TycoonContent.VOID_POWER.get(),4L*batches);row.putInt("Progress",0);row.putLong("Produced",row.getLong("Produced")+4L*batches);TycoonFinance.record(h,"extractor",-cost);
    }
    public static long supply(TycoonData.Home h,Item item){return TycoonWarehouse.count(h,item)+(TycoonPolicies.allowed(h,"useProducedSupplies")?Math.max(0,h.state.getCompound("ProducedResources").getLong(TycoonWarehouse.key(item))):0);}
    public static boolean takeSupply(TycoonData.Home h,Item item,int count){if(count<0||supply(h,item)<count)return false;int first=(int)Math.min(count,TycoonWarehouse.count(h,item));TycoonWarehouse.take(h,item,first);int rest=count-first;if(rest>0){var stock=h.state.getCompound("ProducedResources");stock.putLong(TycoonWarehouse.key(item),stock.getLong(TycoonWarehouse.key(item))-rest);}return true;}
    public static ListTag projection(TycoonData.Home h){var list=new ListTag();for(String key:machines(h).getAllKeys()){var row=machines(h).getCompound(key).copy();row.remove("Generated");row.remove("Ground");list.add(row);}return list;}
    private TycoonMachines(){}
}
