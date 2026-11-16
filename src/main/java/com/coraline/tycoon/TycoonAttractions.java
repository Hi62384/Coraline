package com.coraline.tycoon;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Canonical purchased buildings and independent, resumable visitor activities. */
public final class TycoonAttractions {
    public static CompoundTag records(TycoonData.Home h){return h.state.getCompound("Attractions");}
    public static BlockPos origin(CompoundTag n){return BlockPos.of(n.getLong("Position"));}
    public static BlockPos local(CompoundTag n,int x,int y,int z){return TycoonAttractionPlan.at(origin(n),n.getInt("Rotation"),x,y,z);}
    public static int occupied(TycoonData.Home h,UUID id){int count=0;for(var r:h.residents.values())if(TycoonVisits.inside(r)&&r.n.getCompound("Activity").hasUUID("Structure")&&id.equals(r.n.getCompound("Activity").getUUID("Structure")))count++;return count;}
    public static Map<BlockPos,BlockState> generated(ServerLevel level,TycoonData.Home h){
        var result=new LinkedHashMap<BlockPos,BlockState>();var records=records(h);
        for(String key:records.getAllKeys())result.putAll(TycoonLandscape.decode(level,records.getCompound(key).getCompound("Generated")));
        result.putAll(TycoonSoulFarms.generated(level,h));
        result.putAll(TycoonMachines.generated(level,h));
        return result;
    }
    private static boolean available(TycoonData.Home h,CompoundTag n){return !n.isEmpty()&&n.getBoolean("Operational")&&!TycoonBuildJob.touches(h,origin(n))&&occupied(h,n.getUUID("Id"))<TycoonAttractionType.parse(n.getString("Type")).capacity;}
    public static String preview(ServerPlayer p,String type,int tx,int tz,int rotation){
        var h=TycoonWorld.home(p);var l=p.server.getLevel(TycoonWorld.KEY);
        if(l==null||p.serverLevel()!=l&&!TycoonWorkers.constructing(p)||!TycoonLandscape.inBounds(tx,tz)||rotation<0||rotation>3)return "Choose owned terrain in your personal world.";
        var kind=TycoonAttractionType.parse(type);int x=tx*16+8,z=tz*16+8,y=h.center().getY()-1;
        for(int dx=-6;dx<=6;dx++)for(int dz=-6;dz<=6;dz++)y=Math.max(y,TycoonLandscape.height(h,x+dx,z+dz));
        var at=h.center().offset(x,y-h.center().getY(),z);var plan=TycoonAttractionPlan.create(kind,at,rotation,1,new CompoundTag());
        for(var point:plan.keySet()){
            if(!TycoonLandscape.owned(h,point)||TycoonLandscape.core(h,point)||point.distManhattan(h.center())<10)return "A full owned terrain tile away from the protected core is required.";
            for(var existing:records(h).getAllKeys())if(Math.abs(origin(records(h).getCompound(existing)).getX()-point.getX())<=7&&Math.abs(origin(records(h).getCompound(existing)).getZ()-point.getZ())<=7)return "This footprint overlaps an existing attraction.";
        }
        var n=new CompoundTag();n.putUUID("Id",UUID.randomUUID());n.putString("Type",kind.id());n.putLong("Position",at.asLong());n.putInt("Rotation",rotation);n.putInt("Tier",1);n.putInt("Invested",TycoonAttractionConfig.PRICES.get(kind.id()).get());n.put("Branches",new CompoundTag());n.putBoolean("Operational",true);n.putLong("Expires",h.clock()+1200);
        h.state.put("AttractionPreview",n);TycoonWorld.dirty(p);
        for(int dx:new int[]{-6,6})for(int dz:new int[]{-6,6})l.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,at.getX()+dx+.5,at.getY()+1,at.getZ()+dz+.5,4,.05,.5,.05,0);
        return kind.label+" preview at "+at.toShortString()+", rotation "+rotation+". Footprint 13×13; confirm within one minute. Price "+n.getInt("Invested")+" Void Power.";
    }
    public static String confirm(ServerPlayer p){
        var h=TycoonWorld.home(p);var n=h.state.getCompound("AttractionPreview").copy();var l=p.server.getLevel(TycoonWorld.KEY);
        if(n.isEmpty()||h.clock()>n.getLong("Expires")||l==null||p.serverLevel()!=l&&!TycoonWorkers.constructing(p))return "Create a fresh placement preview first.";
        n.remove("Expires");var plan=TycoonAttractionPlan.create(TycoonAttractionType.parse(n.getString("Type")),origin(n),n.getInt("Rotation"),1,n.getCompound("Branches"));
        var ground=new LinkedHashMap<BlockPos,BlockState>();var other=generated(l,h);var house=TycoonLandscape.decode(l,h.state.getCompound("HouseGenerated"));
        for(var at:plan.keySet()){
            if(other.containsKey(at)||house.containsKey(at))return "The preview now overlaps another building. Nothing spent.";
            ground.put(at,l.getBlockState(at));
        }
        // Foundations follow the existing terrain, avoiding unsupported floors.
        for(int dx=-6;dx<=6;dx++)for(int dz=-6;dz<=6;dz++){
            var floor=origin(n).offset(dx,0,dz);int groundY=TycoonLandscape.height(h,floor.getX()-h.center().getX(),floor.getZ()-h.center().getZ());
            for(int y=groundY;y<floor.getY();y++){var at=new BlockPos(floor.getX(),y,floor.getZ());ground.put(at,l.getBlockState(at));plan.put(at,Blocks.STONE_BRICKS.defaultBlockState());}
        }
        var entry=local(n,0,1,-7);if(!TycoonWorld.safeFeet(l,entry))return "Clear a supported, two-block-high path to the entrance before confirming.";
        n.put("Ground",TycoonLandscape.encode(ground,origin(n)));n.put("Generated",TycoonLandscape.encode(plan,origin(n)));
        var changes=new CompoundTag();changes.put(n.getUUID("Id").toString(),n);var patch=new CompoundTag();patch.put("AttractionChanges",changes);
        String result=TycoonBuildJob.begin(p,"attraction "+TycoonAttractionType.parse(n.getString("Type")).label,new CompoundTag(),plan,origin(n).asLong(),patch,n.getInt("Invested"));
        if(result.startsWith("Building"))h.state.remove("AttractionPreview");return result;
    }
    public static String upgrade(ServerPlayer p,UUID id,int branch){
        var h=TycoonWorld.home(p);var all=records(h).copy();var n=all.getCompound(id.toString());var l=p.server.getLevel(TycoonWorld.KEY);
        if(l==null||n.isEmpty()||p.serverLevel()!=l&&!TycoonWorkers.constructing(p)||occupied(h,id)>0)return "An empty owned attraction is required for construction.";
        var old=n.getCompound("Generated").copy();int cost;
        if(branch==-1){if(n.getInt("Tier")>=5)return "Already Tier 5.";cost=n.getInt("Tier")*12;n.putInt("Tier",n.getInt("Tier")+1);}
        else{if(branch<0||branch>2)return "Unknown upgrade branch.";var branches=n.getCompound("Branches");int rank=branches.getInt(Integer.toString(branch));if(rank>=3)return "This branch is complete.";if(rank>=n.getInt("Tier"))return "Upgrade the building tier first.";cost=8*(rank+1);branches.putInt(Integer.toString(branch),rank+1);n.put("Branches",branches);}
        var plan=TycoonAttractionPlan.create(TycoonAttractionType.parse(n.getString("Type")),origin(n),n.getInt("Rotation"),n.getInt("Tier"),n.getCompound("Branches"));
        // Retain foundation support from the original plan and every stocked cabinet.
        for(var entry:TycoonLandscape.decode(l,old).entrySet())if(entry.getKey().getY()<origin(n).getY())plan.put(entry.getKey(),entry.getValue());
        n.put("Generated",TycoonLandscape.encode(plan,origin(n)));n.putInt("Invested",n.getInt("Invested")+cost);n.putLong("UpgradedAt",h.clock());var changes=new CompoundTag();var fields=new CompoundTag();for(String field:List.of("Generated","Invested","UpgradedAt","Tier","Branches"))fields.put(field,n.get(field).copy());changes.put(id.toString(),fields);var patch=new CompoundTag();patch.put("AttractionChanges",changes);
        return TycoonBuildJob.begin(p,"attraction upgrade "+TycoonAttractionType.parse(n.getString("Type")).label,old,plan,origin(n).asLong(),patch,cost);
    }
    public static String sale(ServerPlayer p,UUID id,boolean confirm){
        var h=TycoonWorld.home(p);var n=records(h).getCompound(id.toString());if(n.isEmpty())return "Attraction unavailable.";
        if(occupied(h,id)>0)return "Return the occupants to the house before selling.";
        if(!confirm){h.state.putUUID("AttractionSale",id);h.state.putLong("AttractionSaleAt",h.clock());return "Confirm sale: refunds "+(int)(n.getInt("Invested")*TycoonAttractionConfig.REFUND.get())+" Power after safe removal. Empty the real gift cabinet first.";}
        if(!h.state.hasUUID("AttractionSale")||!id.equals(h.state.getUUID("AttractionSale"))||h.clock()-h.state.getLong("AttractionSaleAt")>600)return "Preview this sale before confirming.";
        var l=p.server.getLevel(TycoonWorld.KEY);if(l==null||p.serverLevel()!=l)return "Use your personal Work Desk.";
        if(l.getBlockEntity(local(n,5,1,-5)) instanceof TycoonBlockEntity be&&!be.isEmpty())return "Collect the stocked gifts from the cabinet first; nothing removed.";
        // Cabinet removal uses its normal onRemove path. Empty ownership-bearing
        // BEs are explicitly removed only after exact generated-state validation.
        var old=TycoonLandscape.decode(l,n.getCompound("Generated"));for(var e:old.entrySet())if(!l.getBlockState(e.getKey()).equals(e.getValue())||h.state.getCompound("PlayerEdits").contains(Long.toString(e.getKey().asLong())))return "A changed building cell must be restored before selling.";
        var changes=new CompoundTag();var removal=new CompoundTag();removal.putBoolean("Remove",true);changes.put(id.toString(),removal);var patch=new CompoundTag();patch.put("AttractionChanges",changes);
        var gift=new net.minecraft.world.item.ItemStack(TycoonContent.VOID_POWER.get(),(int)(n.getInt("Invested")*TycoonAttractionConfig.REFUND.get()));
        return TycoonBuildJob.beginRemoval(p,"attraction sale "+id,n.getCompound("Generated"),TycoonLandscape.decode(l,n.getCompound("Ground")),origin(n).asLong(),patch,gift);
    }
    public static double match(TycoonData.Resident r,TycoonAttractionType type,int tier,CompoundTag branches,int crowd){
        double score=0,weight=0;for(int i=0;i<TycoonAttractionType.INTERESTS.length;i++){
            double desire=r.trait(TycoonAttractionType.INTERESTS[i]),offered=type.quality(i,tier,branches),w=.25+desire;
            score+=(1-2*Math.abs(desire-offered))*w;weight+=w;
        }
        double sensitivity=r.trait("EmotionalSensitivity"),comfort=r.trait("ComfortPreference");
        double noise=(type==TycoonAttractionType.CIRCUS||type==TycoonAttractionType.PLAYGROUND)?1:.2;
        return Math.max(-1,Math.min(1,score/weight-.3*sensitivity*noise-.25*(1-r.trait("Socialness"))*Math.min(1,crowd/4.)-.2*comfort*(1-type.quality(6,tier,branches))));
    }
    public static double wanderChance(double curiosity){double[] values={0,.05,.12,.22,.35};double x=Math.max(0,Math.min(1,curiosity))*4;int i=Math.min(3,(int)x);return values[i]+(values[i+1]-values[i])*(x-i);}
    public static String assign(ServerPlayer p,TycoonData.Resident r,UUID id){
        var h=TycoonWorld.home(p);var n=records(h).getCompound(id.toString());if(!r.active()||!TycoonVisits.inside(r)||!available(h,n))return "That attraction is unavailable or full.";
        var activity=new CompoundTag();activity.putUUID("Id",UUID.randomUUID());activity.putUUID("Structure",id);activity.putString("State","TRAVELLING");activity.putLong("AssignedAt",h.clock());
        int min=TycoonAttractionConfig.MIN_ACTIVITY.get(),max=Math.max(min,TycoonAttractionConfig.MAX_ACTIVITY.get());activity.putInt("Duration",min+Math.floorMod(r.id.hashCode()^id.hashCode()^r.n.getInt("VisitCount"),max-min+1));
        r.n.put("Activity",activity);TycoonWorld.dirty(p);return "Let's visit the "+TycoonAttractionType.parse(n.getString("Type")).label+". Follow the path from the house.";
    }
    public static String returnHouse(ServerPlayer p,TycoonData.Resident r){
        var n=r.n.getCompound("Activity");n.putString("State","RETURNING");n.putLong("ReturningAt",TycoonWorld.home(p).clock());r.n.put("Activity",n);r.n.remove("SpyMarker");TycoonWorld.dirty(p);return "Let's return to the house.";
    }
    public static void cancel(TycoonData.Resident r){r.n.remove("Activity");}
    private static int[][] waypoints(TycoonAttractionType type){return switch(type){
        case MAZE->new int[][]{{-5,1,-4},{-5,1,4},{-3,1,4},{-3,1,-4},{-1,1,-4},{-1,1,4},{1,1,4},{1,1,-4},{3,1,-4},{3,1,4},{5,1,3}};
        case VILLAGE->new int[][]{{0,1,-4},{-3,1,-3},{0,1,1},{-3,1,3},{1,1,4}};
        case CIRCUS,THEATRE->new int[][]{{1,1,-4},{1,1,-2},{1,1,0}};
        case RESTAURANT->new int[][]{{0,1,-3},{1,1,-2},{1,1,2},{0,1,3}};
        case GALLERY->new int[][]{{0,1,-3},{-2,1,3},{0,1,4},{2,1,3}};
        case GARDEN->new int[][]{{0,1,-3},{-2,1,1},{-2,1,4},{2,1,4},{2,1,0}};
        case RETREAT->new int[][]{{1,1,-4},{1,1,-2},{2,1,3}};
        case PLAYGROUND->new int[][]{{0,1,-4},{0,1,-1},{-1,1,2},{2,1,3}};
        case WONDERLAND->new int[][]{{0,1,-3},{-2,1,0},{-2,1,4},{2,1,4},{2,1,0}};
    };}
    private static BlockPos waypoint(CompoundTag n,int index){var points=waypoints(TycoonAttractionType.parse(n.getString("Type")));var point=points[Math.floorMod(index,points.length)];return local(n,point[0],point[1],point[2]);}
    public static boolean actorTick(ServerPlayer p,TycoonData.Resident r,TycoonNpc actor){
        var h=TycoonWorld.home(p);var a=r.n.getCompound("Activity");
        if(a.isEmpty()){r.n.putString("Task","WAITING_FOR_ATTRACTION");TycoonCare.move(actor,h.center().offset(2,0,2),.6);return true;}
        if(a.getString("State").equals("RETURNING")){
            var at=TycoonWorld.landing((ServerLevel)actor.level(),h.center().offset(2,0,2));
            if(TycoonCare.move(actor,at,.85)){r.n.remove("Activity");actor.gesture(2,40);}
            else if(h.clock()-a.getLong("ReturningAt")>1200&&at!=null){actor.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,actor.getYRot(),0);r.n.remove("Activity");}
            return true;
        }
        if(!a.hasUUID("Structure")){r.n.remove("Activity");return true;}var n=records(h).getCompound(a.getUUID("Structure").toString());
        if(n.isEmpty()||!n.getBoolean("Operational")){returnHouse(p,r);return true;}
        var type=TycoonAttractionType.parse(n.getString("Type"));
        if(a.getString("State").equals("WANDERING")){
            r.n.putString("Task","WANDERING");TycoonEvidence.observe(p,r,actor);
            if(h.clock()-a.getLong("WanderAt")>1200){a.putString("State","TRAVELLING");a.putLong("AssignedAt",h.clock());a.remove("WanderTarget");}
            else if(a.contains("WanderTarget"))TycoonCare.move(actor,BlockPos.of(a.getLong("WanderTarget")),.8);
            return true;
        }
        if(a.getString("State").equals("TRAVELLING")){
            r.n.putString("Task","VISITING_"+type.name());
            if(TycoonCare.move(actor,local(n,0,1,-5),.85)){a.putString("State","ACTIVE");if(!a.contains("Progress"))a.putInt("Progress",0);}
            else if(h.clock()-a.getLong("AssignedAt")>1200)returnHouse(p,r);
            return true;
        }
        if(!a.getString("State").equals("ACTIVE")){returnHouse(p,r);return true;}
        // Progress counts actual simulated activity only, never wall time or menu opens.
        if(actor.distanceToSqr(origin(n).getCenter())>150){a.putString("State","TRAVELLING");a.putLong("AssignedAt",h.clock());return true;}
        int progress=a.getInt("Progress");r.n.putString("Task","ENJOYING_"+type.name());
        if(h.clock()>=a.getLong("NextWanderCheck")){
            a.putLong("NextWanderCheck",h.clock()+400);long roll=Math.floorMod(Objects.hash(r.id,a.getUUID("Id"),h.clock()/400),10000);
            if(roll<10000*wanderChance(r.trait("Curiosity"))*TycoonAttractionConfig.WANDER_SCALE.get()){
                var candidates=new ArrayList<BlockPos>();for(int dx=-16;dx<=16;dx+=8)for(int dz=-16;dz<=16;dz+=8){var at=actor.blockPosition().offset(dx,0,dz);if(TycoonLandscape.owned(h,at)&&TycoonWorld.safeFeet((ServerLevel)actor.level(),at))candidates.add(at);}
                if(!candidates.isEmpty()){var target=candidates.get(Math.floorMod(Objects.hash(r.id,progress),candidates.size()));a.putString("State","WANDERING");a.putLong("WanderAt",h.clock());a.putLong("WanderTarget",target.asLong());TycoonDollAnalysis.observe(r,"wander:"+a.getUUID("Id")+":"+progress,"An observed deviation from an activity",80,h.clock());return true;}
            }
        }
        if(TycoonGifts.storage(p,r,actor,n))return true;
        var goal=waypoint(n,a.getInt("ActivityStep"));
        boolean reached=TycoonWorld.safeFeet((ServerLevel)actor.level(),goal)&&TycoonCare.move(actor,goal,.65);
        if(reached){a.putInt("Progress",progress+20);a.putInt("AtSpot",a.getInt("AtSpot")+20);a.putInt("BlockedTicks",0);if(a.getInt("AtSpot")>=80){a.putInt("AtSpot",0);a.putInt("ActivityStep",a.getInt("ActivityStep")+1);}}
        else{a.putInt("AtSpot",0);a.putInt("BlockedTicks",a.getInt("BlockedTicks")+20);if(a.getInt("BlockedTicks")>=1200){TycoonWorld.message(p,r.name()+" couldn't reach the activity. Clear its paths before another visit.");returnHouse(p,r);return true;}}
        if(type==TycoonAttractionType.CIRCUS||type==TycoonAttractionType.THEATRE){look(actor,local(n,0,2,3));actor.gesture(2,25);}
        else if(type==TycoonAttractionType.GALLERY||type==TycoonAttractionType.GARDEN){look(actor,local(n,3,2,4));actor.gesture(2,25);}
        else if(type==TycoonAttractionType.RETREAT&&reached){actor.getNavigation().stop();actor.gesture(3,25);}
        else actor.gesture(type==TycoonAttractionType.PLAYGROUND?8:2,25);
        if(progress>=a.getInt("Duration")&&a.getInt("ActivityStep")>=waypoints(type).length)complete(p,r,actor,n,type,a);
        return true;
    }
    private static void complete(ServerPlayer p,TycoonData.Resident r,TycoonNpc actor,CompoundTag n,TycoonAttractionType type,CompoundTag a){
        var h=TycoonWorld.home(p);String key=a.getUUID("Structure").toString();var fatigue=r.n.getCompound("AttractionFatigue");var previous=fatigue.getCompound(key);long age=h.clock()-previous.getLong("At");double tired=Math.max(0,previous.getDouble("Amount")-Math.max(0,age)/24000.);
        if(n.getLong("UpgradedAt")>previous.getLong("At"))tired*=.4;
        double score=match(r,type,n.getInt("Tier"),n.getCompound("Branches"),occupied(h,n.getUUID("Id")))-tired*.55;
        int outcome=score<-.35?0:score<.0?1:score<.30?2:score<.60?3:4;String[] words={"hated","disliked","felt neutral about","enjoyed","loved"};
        TycoonDialogue.set(r,TycoonDialogue.trust(r)+TycoonAttractionConfig.TRUST.get(outcome).get(),TycoonDialogue.suspicion(r)+TycoonAttractionConfig.SUSPICION.get(outcome).get());
        var f=new CompoundTag();f.putLong("At",h.clock());f.putDouble("Amount",Math.min(1,tired+.25));fatigue.put(key,f);r.n.put("AttractionFatigue",fatigue);
        var memories=r.n.getList("Experiences",10);var memory=new CompoundTag();memory.putUUID("Receipt",a.getUUID("Id"));memory.putString("Structure",key);memory.putString("Type",type.id());memory.putString("Feeling",words[outcome]);memory.putLong("At",h.clock());memories.add(memory);while(memories.size()>40)memories.remove(0);r.n.put("Experiences",memories);
        TycoonDollAnalysis.observe(r,"activity:"+a.getUUID("Id"),"They "+words[outcome]+" the "+type.label,240,h.clock());
        TycoonWorld.message(p,r.name()+": I "+words[outcome]+" the "+type.label+". "+(outcome>=3?"Thank you for bringing me.":"Could we try something different?"));returnHouse(p,r);actor.gesture(outcome<2?5:2,60);TycoonWorld.dirty(p);
    }
    public static void tick(ServerPlayer p){
        var h=TycoonWorld.home(p);var l=p.server.getLevel(TycoonWorld.KEY);if(l==null)return;
        for(String key:records(h).getAllKeys()){
            var n=records(h).getCompound(key);var at=local(n,5,1,-5);
            if(!l.hasChunkAt(at))continue;
            if(l.getBlockEntity(at) instanceof TycoonBlockEntity be){
                if(be.owner==null){be.owner=TycoonWorld.owner(p);be.setChanged();}
                // A completed gift can leave a debit receipt after a crash;
                // retire it only once no canonical saved escrow references it.
                for(String receipt:new ArrayList<>(be.giftDebits.getAllKeys())){
                    boolean pending=h.residents.values().stream().anyMatch(r->r.n.getCompound("Gift").hasUUID("Id")&&r.n.getCompound("Gift").getUUID("Id").toString().equals(receipt));
                    for(String id:TycoonWorkers.workers(h).getAllKeys()){var carry=TycoonWorkers.workers(h).getCompound(id).getCompound("Carry");if(carry.hasUUID("Id")&&(receipt.equals(carry.getUUID("Id").toString())||receipt.equals("in:"+carry.getUUID("Id"))))pending=true;}
                    if(!pending){be.giftDebits.remove(receipt);be.setChanged();}
                }
            }
            var type=TycoonAttractionType.parse(n.getString("Type"));
            if(type==TycoonAttractionType.VILLAGE||type==TycoonAttractionType.CIRCUS||type==TycoonAttractionType.THEATRE)for(int slot=0;slot<(type==TycoonAttractionType.VILLAGE?3:1);slot++)performer(p,n,type,slot);
        }
        if(p.serverLevel()==l)for(var actor:l.getEntitiesOfClass(Villager.class,p.getBoundingBox().inflate(96),v->v.getPersistentData().hasUUID("TycoonAttraction")&&v.getPersistentData().hasUUID("TycoonOwner")&&TycoonWorld.owner(p).equals(v.getPersistentData().getUUID("TycoonOwner"))))if(!records(h).contains(actor.getPersistentData().getUUID("TycoonAttraction").toString()))actor.discard();
    }
    private static void look(TycoonNpc actor,BlockPos at){actor.getLookControl().setLookAt(at.getX()+.5,at.getY()+.5,at.getZ()+.5);}
    private static void performer(ServerPlayer p,CompoundTag n,TycoonAttractionType type,int slot){
        String identity="Performer"+slot,location="PerformerAt"+slot;
        var l=p.server.getLevel(TycoonWorld.KEY);var at=type==TycoonAttractionType.VILLAGE?local(n,slot==1?3:-3,1,slot==2?2:-4):local(n,0,1,3);if(l==null||!l.hasChunkAt(at)||p.serverLevel()!=l||p.distanceToSqr(at.getCenter())>64*64)return;
        Villager actor=n.hasUUID(identity)&&l.getEntity(n.getUUID(identity)) instanceof Villager v?v:null;
        if(actor==null){
            // Load the last canonical location before concluding it is missing.
            if(n.contains(location)){l.getChunkAt(BlockPos.of(n.getLong(location)));if(n.hasUUID(identity)&&l.getEntity(n.getUUID(identity)) instanceof Villager v)actor=v;}
            if(actor==null){if(n.hasUUID(identity)){n.putString(identity+"Blocker","Original performer unavailable; waiting for its native body.");TycoonWorld.dirty(p);return;}actor=EntityType.VILLAGER.create(l);if(actor==null)return;if(n.hasUUID(identity))actor.setUUID(n.getUUID(identity));actor.setPersistenceRequired();actor.setInvulnerable(true);actor.getPersistentData().putUUID("TycoonAttraction",n.getUUID("Id"));actor.getPersistentData().putUUID("TycoonOwner",TycoonWorld.owner(p));actor.setCustomName(net.minecraft.network.chat.Component.literal(type==TycoonAttractionType.VILLAGE?new String[]{"Village Baker","Village Artist","Village Host"}[slot]:type==TycoonAttractionType.CIRCUS?"Circus Illusionist":"Theatre Performer"));actor.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);n.putUUID(identity,actor.getUUID());n.putLong(location,at.asLong());TycoonWorld.dirty(p);try{if(!TycoonCheckpoints.home(p))return;}catch(java.io.IOException unavailable){return;}if(!l.addFreshEntity(actor)){n.putString(identity+"Blocker","Native performer admission was refused; no substitute issued.");TycoonWorld.dirty(p);return;}}
        }
        n.remove(identity+"MissingTicks");n.remove(identity+"Blocker");
        n.putLong(location,actor.blockPosition().asLong());actor.setNoAi(true);
        double phase=(TycoonWorld.home(p).clock()%240)/240.*Math.PI*2;actor.setYRot((float)(Math.sin(phase)*70));actor.setYHeadRot(actor.getYRot());
        if(type!=TycoonAttractionType.VILLAGE){actor.setPos(at.getX()+.5+Math.sin(phase)*1.2,at.getY(),at.getZ()+.5);if(p.tickCount%100==0)l.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,actor.getX(),actor.getY()+1,actor.getZ(),4,.5,.3,.5,0);}
        TycoonWorld.dirty(p);
    }
    public static ListTag projection(ServerPlayer p,boolean visitor){
        var h=TycoonWorld.home(p);var list=new ListTag();for(String key:records(h).getAllKeys()){
            var n=records(h).getCompound(key);var type=TycoonAttractionType.parse(n.getString("Type"));var row=new CompoundTag();row.putUUID("Id",n.getUUID("Id"));row.putString("Name",type.label);row.putString("Type",type.id());row.putInt("Tier",n.getInt("Tier"));row.putInt("Occupants",occupied(h,n.getUUID("Id")));row.putInt("Capacity",type.capacity);row.putBoolean("Available",available(h,n));row.putLong("Position",n.getLong("Position"));row.putInt("Distance",(int)Math.sqrt(p.distanceToSqr(origin(n).getCenter())));if(!visitor){row.putInt("Invested",n.getInt("Invested"));row.put("Branches",n.getCompound("Branches").copy());}list.add(row);
        }return list;
    }
    private TycoonAttractions(){}
}
