package com.coraline.tycoon;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Owner-online, fixed-point production. No real-world/offline catch-up. */
public final class TycoonSoulFarms {
    public static final int LIMIT=30,BREACH_TICKS=12000;
    public static CompoundTag farms(TycoonData.Home h){return h.state.getCompound("SoulFarms");}
    public static int capacity(CompoundTag farm){return TycoonSoulConfig.CAPACITY.get(Math.max(0,Math.min(4,farm.getInt("Tier")-1))).get();}
    public static long priceOrdinal(int ordinal){return ordinal<=1?0:ordinal==2?1:10L*(ordinal-2);}
    public static boolean breaching(CompoundTag farm){return farm.getCompound("Breach").getBoolean("Active");}
    public static List<CompoundTag> assigned(TycoonData.Home h,UUID id){var list=new ArrayList<CompoundTag>();var souls=TycoonSouls.collection(h);for(String key:souls.getAllKeys()){var n=souls.getCompound(key);if(TycoonSouls.usable(n)&&n.hasUUID("Farm")&&id.equals(n.getUUID("Farm")))list.add(n);}list.sort(Comparator.comparing(n->n.getUUID("Id").toString()));return list;}
    public static BlockPos local(CompoundTag farm,int x,int y,int z){return TycoonAttractionPlan.at(TycoonAttractions.origin(farm),farm.getInt("Rotation"),x,y,z);}
    public static Map<BlockPos,BlockState> generated(ServerLevel level,TycoonData.Home h){var result=new LinkedHashMap<BlockPos,BlockState>();var all=farms(h);for(String key:all.getAllKeys())result.putAll(TycoonLandscape.decode(level,all.getCompound(key).getCompound("Generated")));return result;}
    public static String preview(ServerPlayer p,int tx,int tz,int rotation){
        var h=TycoonWorld.home(p);if(p.serverLevel().dimension()!=TycoonWorld.KEY&&!TycoonWorkers.constructing(p)||!TycoonLandscape.inBounds(tx,tz)||rotation<0||rotation>3)return "Select a plot in your personal Other World.";
        if(farms(h).size()>=LIMIT)return "The hard limit is 30 Soul Farms.";
        int x=tx*16+8,z=tz*16+8,y=h.center().getY()-1;for(int dx=-6;dx<=6;dx++)for(int dz=-6;dz<=6;dz++)y=Math.max(y,TycoonLandscape.height(h,x+dx,z+dz));
        var origin=h.center().offset(x,y-h.center().getY(),z);var n=new CompoundTag();n.putUUID("Id",UUID.randomUUID());n.putUUID("Owner",h.owner);n.putLong("Position",origin.asLong());n.putInt("Rotation",rotation);n.putInt("Tier",1);n.putLong("PreviewUntil",h.clock()+1200);n.putInt("Ordinal",h.state.getInt("FarmPurchases")+1);n.putString("State","EMPTY");h.state.put("FarmPreview",n);TycoonWorld.dirty(p);
        for(int dx:new int[]{-6,6})for(int dz:new int[]{-6,6})p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,origin.getX()+dx+.5,origin.getY()+1,origin.getZ()+dz+.5,5,.05,.5,.05,0);
        return "Soul Farm preview: 13×13 at "+origin.toShortString()+". Cost "+TycoonSoulConfig.FARM_POWER.get()+" Power + "+priceOrdinal(n.getInt("Ordinal"))+" Essence. Machinery is highly suspicious, not a visitor attraction.";
    }
    public static String confirm(ServerPlayer p){
        var h=TycoonWorld.home(p);var n=h.state.getCompound("FarmPreview").copy();var l=p.server.getLevel(TycoonWorld.KEY);
        if(n.isEmpty()||h.clock()>=n.getLong("PreviewUntil")||l==null||p.serverLevel()!=l&&!TycoonWorkers.constructing(p)||farms(h).size()>=LIMIT||n.getInt("Ordinal")!=h.state.getInt("FarmPurchases")+1)return "Create a fresh owned farm preview.";
        var origin=TycoonAttractions.origin(n);var plan=TycoonSoulFarmPlan.create(origin,n.getInt("Rotation"),1);var occupied=TycoonAttractions.generated(l,h);var house=TycoonLandscape.decode(l,h.state.getCompound("HouseGenerated"));var before=new LinkedHashMap<BlockPos,BlockState>();
        for(var at:plan.keySet()){if(!TycoonLandscape.owned(h,at)||TycoonLandscape.core(h,at)||occupied.containsKey(at)||house.containsKey(at))return "Farm footprint conflicts with ownership, core or an existing structure. Nothing spent.";before.put(at,l.getBlockState(at));}
        for(int dx=-6;dx<=6;dx++)for(int dz=-6;dz<=6;dz++){var floor=origin.offset(dx,0,dz);int gy=TycoonLandscape.height(h,floor.getX()-h.center().getX(),floor.getZ()-h.center().getZ());for(int y=gy;y<floor.getY();y++){var at=new BlockPos(floor.getX(),y,floor.getZ());before.put(at,l.getBlockState(at));plan.put(at,Blocks.DEEPSLATE_BRICKS.defaultBlockState());}}
        if(!TycoonWorld.safeFeet(l,local(n,0,1,-7)))return "A supported, clear entrance path is required.";
        n.remove("PreviewUntil");n.put("Ground",TycoonLandscape.encode(before,origin));n.put("Generated",TycoonLandscape.encode(plan,origin));n.putInt("InvestedPower",TycoonSoulConfig.FARM_POWER.get());var changes=new CompoundTag();changes.put(n.getUUID("Id").toString(),n);var patch=new CompoundTag();patch.put("FarmChanges",changes);patch.putInt("FarmPurchases",n.getInt("Ordinal"));
        String result=TycoonBuildJob.beginPriced(p,"attraction Soul Farm",new CompoundTag(),plan,origin.asLong(),patch,TycoonSoulConfig.FARM_POWER.get(),priceOrdinal(n.getInt("Ordinal")));if(result.startsWith("Building"))h.state.remove("FarmPreview");return result;
    }
    public static boolean constructionValid(TycoonData.Home h,CompoundTag patch){
        var changes=patch.getCompound("FarmChanges");for(String id:changes.getAllKeys())if(breaching(farms(h).getCompound(id)))return false;return true;
    }
    public static String upgrade(ServerPlayer p,UUID id,boolean repair){
        var h=TycoonWorld.home(p);var n=farms(h).getCompound(id.toString());var l=p.server.getLevel(TycoonWorld.KEY);
        if(n.isEmpty()||l==null||p.serverLevel()!=l&&!TycoonWorkers.constructing(p)||breaching(n))return "Select an owned farm without an active breach.";
        if(repair&&!n.getBoolean("Damaged"))return "This farm is not damaged.";if(!repair&&(n.getBoolean("Damaged")||n.getInt("Tier")>=5))return "Repair damage first, or this farm is already Tier 5.";
        int tier=n.getInt("Tier")+(repair?0:1),cost=repair?tier*TycoonSoulConfig.REPAIR_POWER.get():16*tier;long soulCost=repair?0:Math.max(0,(tier-2)*5L);var origin=TycoonAttractions.origin(n);var plan=TycoonSoulFarmPlan.create(origin,n.getInt("Rotation"),tier);
        var previous=TycoonLandscape.decode(l,n.getCompound("Generated"));
        for(var e:previous.entrySet())if(e.getKey().getY()<origin.getY())plan.put(e.getKey(),e.getValue());
        // Damage is recorded at the instant of a player edit/failure; repairs may
        // replace only those known damaged cells, never unrelated player builds.
        var old=new LinkedHashMap<>(previous);if(repair)for(String key:n.getCompound("DamageCells").getAllKeys()){var at=BlockPos.of(Long.parseLong(key));if(l.getBlockState(at).isAir())old.put(at,Blocks.AIR.defaultBlockState());}
        var fields=new CompoundTag();fields.putInt("Tier",tier);fields.putBoolean("Damaged",false);fields.putString("State","EMPTY");fields.put("DamageCells",new CompoundTag());fields.put("Generated",TycoonLandscape.encode(plan,origin));fields.putInt("InvestedPower",n.getInt("InvestedPower")+cost);var changes=new CompoundTag();changes.put(id.toString(),fields);var patch=new CompoundTag();patch.put("FarmChanges",changes);
        return TycoonBuildJob.beginPriced(p,"attraction Soul Farm "+(repair?"repair":"upgrade"),TycoonLandscape.encode(old,origin),plan,origin.asLong(),patch,cost,soulCost);
    }
    public static double resonance(List<CompoundTag> souls){
        if(souls.size()<2)return 1;double sum=0;int pairs=0;for(int i=0;i<souls.size();i++)for(int j=i+1;j<souls.size();j++){
            var a=souls.get(i);var b=souls.get(j);double affinity=a.getString("Primary").equals(b.getString("Primary"))?.12:a.getString("Secondary").equals(b.getString("Primary"))?.08:-.025;
            double social=(a.getString("Primary").equals("Social")||b.getString("Primary").equals("Social"))?.04:0;
            sum+=.85+.3*(a.getDouble("Resonance")+b.getDouble("Resonance"))/2+affinity+social;pairs++;
        }return Math.max(.85,Math.min(1.25,sum/pairs));
    }
    public static long rate(TycoonData.Home h,CompoundTag farm){
        if(farm.isEmpty()||farm.getBoolean("Damaged")||farm.getBoolean("Paused")||!farm.getBoolean("Verified"))return 0;
        return rateFor(h,farm,assigned(h,farm.getUUID("Id")));
    }
    public static long rateFor(TycoonData.Home h,CompoundTag farm,List<CompoundTag> souls){
        if(souls.isEmpty())return 0;double base=0;
        for(var soul:souls){double stability=soul.getDouble("Stability"),efficiency=soul.getDouble("Efficiency"),restless=soul.getDouble("Restlessness");double phase=(h.clock()/200+Math.floorMod(soul.getUUID("Id").hashCode(),31))*.8;
            double fluctuation=1+(1-stability)*Math.sin(phase)*.12;double bonus=soul.getString("Primary").equals("Playful")&&Math.floorMod(h.clock()/200+soul.getUUID("Id").hashCode(),11)==0?1.08:1;
            double containment=Math.max(.75,1-restless*.08*(6-farm.getInt("Tier"))/5.);base+=Math.max(0,soul.getLong("BaseMicros"))*(.65+.35*efficiency)*fluctuation*bonus*containment;
        }
        return Math.max(0,Math.min(10_000_000L,Math.round(base*TycoonSoulConfig.MULTIPLIER.get(Math.max(0,Math.min(4,farm.getInt("Tier")-1))).get()*resonance(souls))));
    }
    public static void tick(ServerPlayer p){
        var h=TycoonWorld.home(p);var l=p.server.getLevel(TycoonWorld.KEY);if(l==null)return;
        for(String key:farms(h).getAllKeys()){
            var farm=farms(h).getCompound(key);var at=TycoonAttractions.origin(farm);
            if(l.hasChunkAt(at)&&p.tickCount%100==0&&!TycoonBuildJob.touches(h,at))verify(l,farm);
            // No elapsed timestamp multiplication: one owner tick = one second.
            long rate=TycoonBuildJob.touches(h,at)?0:rate(h,farm);farm.putLong("RateMicros",rate);if(farm.getLong("LastProducedClock")!=h.clock()){farm.putLong("LastProducedClock",h.clock());if(rate>0&&TycoonSoulLedger.creditMicros(h,rate))TycoonFinance.record(h,"farm_income",rate);breachTick(p,farm);}
            farm.putString("State",farm.getBoolean("Damaged")?"DAMAGED":breaching(farm)?"BREACH_IN_PROGRESS":farm.getBoolean("Paused")?"PAUSED":rate>0?"ACTIVE":"EMPTY");
            if(l.hasChunkAt(at)&&p.serverLevel()==l&&p.distanceToSqr(at.getCenter())<48*48){
                var souls=assigned(h,farm.getUUID("Id"));for(int i=0;i<souls.size();i++){var chamber=local(farm,-4+i*2,2,4);l.sendParticles(new net.minecraft.core.particles.DustParticleOptions(TycoonSouls.color(souls.get(i).getUUID("Id")),1.5F),chamber.getX()+.5,chamber.getY()+.5,chamber.getZ()+.5,rate>0?12:5,.12,.18,.12,0);l.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,chamber.getX()+.5,chamber.getY()+.5,chamber.getZ()+.5,rate>0?3:1,.08,.15,.08,0);}
                if(rate>0){var machine=local(farm,0,2,1);l.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,machine.getX()+.5,machine.getY()+.5,machine.getZ()+.5,3,.4,.6,.4,.05);}
            }
        }TycoonWorld.dirty(p);
    }
    private static void verify(ServerLevel l,CompoundTag farm){
        var plan=TycoonLandscape.decode(l,farm.getCompound("Generated"));boolean valid=true;for(var e:plan.entrySet()){
            if(e.getValue().isAir())continue;if(!l.hasChunkAt(e.getKey()))return;
            if(!l.getBlockState(e.getKey()).equals(e.getValue())){valid=false;var cells=farm.getCompound("DamageCells");cells.putBoolean(Long.toString(e.getKey().asLong()),true);farm.put("DamageCells",cells);}
        }farm.putBoolean("Verified",valid);if(!valid)farm.putBoolean("Damaged",true);
    }
    public static boolean edited(ServerPlayer p,BlockPos at){var h=TycoonWorld.home(p);boolean changed=false;for(String key:farms(h).getAllKeys()){var n=farms(h).getCompound(key);var c=TycoonAttractions.origin(n);if(Math.abs(c.getX()-at.getX())<=6&&Math.abs(c.getZ()-at.getZ())<=6&&at.getY()>=c.getY()&&at.getY()<=c.getY()+5){n.putBoolean("Damaged",true);n.putBoolean("Verified",false);var cells=n.getCompound("DamageCells");cells.putBoolean(Long.toString(at.asLong()),true);n.put("DamageCells",cells);h.state.getCompound("PlayerEdits").remove(Long.toString(at.asLong()));changed=true;}}if(changed)TycoonWorld.dirty(p);return changed;}
    public static boolean actorTick(ServerPlayer p,TycoonData.Resident r,TycoonNpc actor){
        var h=TycoonWorld.home(p);if(r.n.hasUUID("RescuingFarm")){
            r.n.putString("Task","SOUL_RESCUE_ATTEMPT");
            if(!r.n.hasUUID("RescuingFarm"))return false;var farm=farms(h).getCompound(r.n.getUUID("RescuingFarm").toString());if(!breaching(farm)){finishRescuer(p,r);return true;}
            TycoonDialogue.set(r,TycoonDialogue.trust(r),1);var machinery=local(farm,0,1,2);if(TycoonCare.move(actor,machinery,.85)){actor.getNavigation().stop();actor.gesture(8,25);actor.getLookControl().setLookAt(local(farm,0,2,4).getX()+.5,local(farm,0,2,4).getY()+.5,local(farm,0,2,4).getZ()+.5);farm.getCompound("Breach").putBoolean("Interacting",true);}else farm.getCompound("Breach").putBoolean("Interacting",false);return true;
        }
        if(!r.n.getCompound("Activity").getString("State").equals("WANDERING"))return false;
        for(String key:farms(h).getAllKeys()){
            var farm=farms(h).getCompound(key);if(farm.getBoolean("Damaged")||assigned(h,farm.getUUID("Id")).isEmpty()||actor.distanceToSqr(TycoonAttractions.origin(farm).getCenter())>12*12)continue;
            var soul=local(farm,-4,2,3);if(!TycoonEvidence.perceives(actor,soul.getCenter(),9,.1))continue;
            discover(p,r,farm);return true;
        }return false;
    }
    public static void discover(ServerPlayer p,TycoonData.Resident r,CompoundTag farm){
        var h=TycoonWorld.home(p);r.n.putBoolean("KnowsSoulFarm",true);TycoonDialogue.set(r,TycoonDialogue.trust(r),1);TycoonEvidence.remember(p,r,"farm:"+farm.getUUID("Id"),"captured_souls",1);TycoonDialogue.set(r,TycoonDialogue.trust(r),1);r.n.putString("Task","SOUL_RESCUE_ATTEMPT");r.n.putUUID("RescuingFarm",farm.getUUID("Id"));TycoonAttractions.cancel(r);
        if(!breaching(farm)){var breach=new CompoundTag();breach.putBoolean("Active",true);breach.putLong("Started",h.clock());breach.putInt("Progress",0);breach.putUUID("Child",r.id);breach.putBoolean("Interacting",false);farm.put("Breach",breach);}
        TycoonWorld.dirty(p);
    }
    private static void breachTick(ServerPlayer p,CompoundTag farm){
        if(!breaching(farm))return;var h=TycoonWorld.home(p);var b=farm.getCompound("Breach");int elapsed=b.getInt("Progress")+20;b.putInt("Progress",Math.min(BREACH_TICKS,elapsed));b.putDouble("Integrity",1-Math.min(1,elapsed/(double)BREACH_TICKS));
        if(farm.getInt("Tier")>=3&&!b.getBoolean("AlarmReported")){TycoonWorld.message(p,"Containment alarm: Soul Farm "+farm.getInt("Ordinal")+" is being opened. Secure it or concede the breach.");b.putBoolean("AlarmReported",true);}
        var l=p.server.getLevel(TycoonWorld.KEY);if(l!=null&&l.hasChunkAt(TycoonAttractions.origin(farm))){var at=local(farm,0,2,4);l.sendParticles(elapsed>6000?net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK:net.minecraft.core.particles.ParticleTypes.SMOKE,at.getX()+.5,at.getY()+.5,at.getZ()+.5,Math.max(1,elapsed/2400),1,.5,.3,.02);}
        if(elapsed>=BREACH_TICKS)fail(p,farm);
    }
    public static void fail(ServerPlayer p,CompoundTag farm){
        var h=TycoonWorld.home(p);if(!breaching(farm))return;var souls=assigned(h,farm.getUUID("Id"));for(var soul:souls){soul.remove("Farm");soul.putString("Status","ESCAPED");soul.putLong("EscapedAt",h.clock());}
        farm.getCompound("Breach").putBoolean("Active",false);farm.getCompound("Breach").putBoolean("Failed",true);farm.putBoolean("Damaged",true);farm.putBoolean("Verified",false);farm.putLong("RateMicros",0);var l=p.server.getLevel(TycoonWorld.KEY);var damage=farm.getCompound("DamageCells");
        for(int i=0;i<farm.getInt("Tier");i++){var at=local(farm,-4+i*2,2,4);damage.putBoolean(Long.toString(at.asLong()),true);if(l!=null){l.getChunkAt(at);if(l.getBlockState(at).is(Blocks.GLASS))l.setBlock(at,Blocks.AIR.defaultBlockState(),3);l.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,at.getX()+.5,at.getY()+.5,at.getZ()+.5,24,.3,1,.3,.05);}}
        farm.put("DamageCells",damage);for(var r:h.residents.values())if(r.n.hasUUID("RescuingFarm")&&farm.getUUID("Id").equals(r.n.getUUID("RescuingFarm")))finishRescuer(p,r);TycoonWorld.dirty(p);p.server.overworld().getDataStorage().save();
    }
    private static void finishRescuer(ServerPlayer p,TycoonData.Resident r){r.n.remove("RescuingFarm");TycoonDialogue.set(r,TycoonDialogue.trust(r),1);var v=r.n.getCompound("Visit");v.putString("Phase","LEAVING");v.putLong("LeavingAt",TycoonWorld.home(p).clock());r.n.putString("Task","RETURNING_AFTER_DISCOVERY");}
    public static String intervene(ServerPlayer p,UUID farmId,boolean concede){
        var h=TycoonWorld.home(p);var farm=farms(h).getCompound(farmId.toString());if(!breaching(farm))return "No active breach at this farm.";
        if(concede){fail(p,farm);return "Breach conceded: contained souls permanently escaped; visitors are returning. Your exit will unlock when they are home.";}
        if(p.serverLevel().dimension()!=TycoonWorld.KEY||p.distanceToSqr(local(farm,-4,1,-2).getCenter())>16)return "Use the farm's emergency containment control in person.";
        farm.getCompound("Breach").putBoolean("Active",false);farm.getCompound("Breach").putBoolean("Secured",true);farm.putBoolean("Paused",true);
        for(var r:h.residents.values())if(r.n.hasUUID("RescuingFarm")&&farmId.equals(r.n.getUUID("RescuingFarm")))finishRescuer(p,r);TycoonWorld.dirty(p);return "Emergency containment secured. Production paused; the child still knows the truth and is leaving.";
    }
    public static String pause(ServerPlayer p,UUID id){var h=TycoonWorld.home(p);var farm=farms(h).getCompound(id.toString());if(farm.isEmpty()||breaching(farm))return "Secure the farm first.";farm.putBoolean("Paused",!farm.getBoolean("Paused"));TycoonWorld.dirty(p);return farm.getBoolean("Paused")?"Production paused.":"Production resumed.";}
    static boolean secureByWorker(ServerPlayer p,TycoonWorker guard,CompoundTag farm){
        var h=TycoonWorld.home(p);var worker=TycoonWorkers.workers(h).getCompound(guard.worker.toString());if(!TycoonWorld.owner(p).equals(guard.owner())||guard.role()!=TycoonWorkerType.GUARD||!worker.getBoolean("Paid")||worker.getBoolean("Break")||!breaching(farm)||guard.distanceToSqr(local(farm,-4,1,-2).getCenter())>16)return false;
        farm.getCompound("Breach").putBoolean("Active",false);farm.getCompound("Breach").putBoolean("Secured",true);farm.putBoolean("Paused",true);for(var r:h.residents.values())if(r.n.hasUUID("RescuingFarm")&&farm.getUUID("Id").equals(r.n.getUUID("RescuingFarm")))finishRescuer(p,r);TycoonWorld.dirty(p);return true;
    }
    public static ListTag projection(TycoonData.Home h){var list=new ListTag();for(String key:farms(h).getAllKeys()){var farm=farms(h).getCompound(key);var row=farm.copy();row.remove("Generated");row.remove("Ground");row.remove("DamageCells");row.putInt("Capacity",capacity(farm));row.putInt("Assigned",assigned(h,farm.getUUID("Id")).size());row.putDouble("ResonanceMultiplier",resonance(assigned(h,farm.getUUID("Id"))));list.add(row);}return list;}
    private TycoonSoulFarms(){}
}
