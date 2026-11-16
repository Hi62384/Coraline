package com.coraline.tycoon;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/** Workers choose and execute real existing gameplay operations, not multipliers. */
public final class TycoonWorkerJobs {
    public static void step(ServerPlayer p,CompoundTag w,TycoonWorker actor,TycoonWorkerType type,int rank){
        var h=TycoonWorld.home(p);w.remove("Blocker");
        var build=h.state.getCompound("BuildJob");if(build.hasUUID("Worker")&&w.getUUID("Id").equals(build.getUUID("Worker"))){w.putString("Task","BUILDING_AUTHORIZED_STRUCTURE");actor.getNavigation().stop();return;}
        if(w.contains("Carry")&&!TycoonHauling.recover(p,w)){w.putString("Task","RECOVERING_REAL_CARGO");return;}
        // A destination intent survives restarts. Finish that exact journaled
        // handoff before choosing a new fridge/cabinet or returning the cargo.
        if(w.getCompound("Carry").contains("Destination")){TycoonHauling.resumeDestination(p,w,actor);return;}
        switch(type){
            case OVERWORLD_MAID->maid(p,w,actor,rank);
            case OVERWORLD_CHEF->chef(p,w,actor,rank,false);
            case OTHER_CHEF->chef(p,w,actor,rank,true);
            case ORPHAN_BUYER->buyer(p,w,actor,rank);
            case DOLL_MAKER->dolls(p,w,actor,rank);
            case COMPLIANCE->{w.putString("Task","INSPECTION_PREPARATION");if(!h.state.getBoolean("Approved")&&!h.state.getBoolean("InspectionRequested")&&TycoonWorkers.approach(p,w,actor,near(actor,BlockPos.of(w.getLong("Area")))))TycoonAgency.request(p);}
            case VISIT_COORDINATOR->{w.putString("Task","SCHEDULING_NIGHT_VISITS");if(!h.state.getBoolean("VisitActive")&&TycoonVisits.nighttime(p)&&TycoonWorkers.approach(p,w,actor,near(actor,h.center())))w.putString("LastResult",TycoonVisits.inviteSupplied(p,(int)Math.min(TycoonWorkers.capacity(rank),TycoonPolicies.number(h,"maxVisitors"))));}
            case OTHER_MANAGER->manager(p,w,actor,rank);
            case GIFT_MANAGER->gifts(p,w,actor,rank);
            case GUARD->guard(p,w,actor,rank);
            case SOUL_MANAGER->souls(p,w,actor,rank);
            case STRUCTURE_MANAGER->structures(p,w,actor,rank);
            case TECHNICIAN->maintenance(p,w,actor,rank);
            case VOID_OPERATOR->{w.putString("Task","OPERATING_VOID_EXTRACTORS");for(String id:TycoonMachines.machines(h).getAllKeys()){var machine=TycoonMachines.machines(h).getCompound(id);if(!machine.getString("Type").equals("extractor")||machine.getBoolean("ManualBreak")||!machine.getBoolean("Break")&&machine.getInt("Operating")==Math.min(rank,machine.getInt("Unlocked")))continue;if(TycoonWorkers.approach(p,w,actor,near(actor,TycoonAttractions.origin(machine)))){machine.putBoolean("Break",false);machine.putInt("Operating",Math.min(rank,machine.getInt("Unlocked")));}break;}}
            case ACCOUNTANT->accountant(p,w,actor,rank);
            case MASTER->master(p,w,actor,rank);
        }
    }
    private static BlockPos near(TycoonWorker actor,BlockPos at){if(at==null)return null;var l=(ServerLevel)actor.level();for(var d:Direction.Plane.HORIZONTAL)for(int distance=1;distance<=2;distance++){var candidate=at.relative(d,distance);if(TycoonWorld.safeFeet(l,candidate))return candidate;}return TycoonWorld.safeFeet(l,at)?at:null;}
    private static boolean reach(ServerPlayer p,CompoundTag w,TycoonWorker actor,TycoonNpc child){if(child==null||child.level()!=actor.level())return false;var target=near(actor,child.blockPosition());if(!TycoonWorkers.approach(p,w,actor,target))return false;actor.getLookControl().setLookAt(child,30,30);child.getLookControl().setLookAt(actor,30,30);actor.presentation(w.getInt("Operating"),2);return true;}
    private static TycoonNpc npc(ServerPlayer p,TycoonData.Resident r,TycoonWorker worker){if(!r.n.hasUUID("Entity"))return null;var level=(ServerLevel)worker.level();return level.getEntity(r.n.getUUID("Entity")) instanceof TycoonNpc child?child:null;}
    private static List<TycoonData.Resident> children(ServerPlayer p,CompoundTag w,boolean visiting){
        var h=TycoonWorld.home(p);var list=h.residents.values().stream().filter(TycoonData.Resident::active).filter(r->visiting?r.n.getCompound("Visit").getString("Phase").equals("INSIDE"):!TycoonVisits.visiting(r)).sorted(Comparator.<TycoonData.Resident>comparingDouble(r->Math.min(r.need("Hunger"),r.need("Thirst"))).thenComparing(r->r.id.toString())).toList();
        var selected=new ArrayList<TycoonData.Resident>();for(var r:list)if(TycoonWorkers.reserve(h,w,(visiting?"visitor:":"care:")+w.getString("Type")+":"+r.id)){selected.add(r);if(selected.size()>=TycoonWorkers.capacity(w.getInt("Operating")))break;}return selected;
    }
    private static List<TycoonBlockEntity> furniture(ServerPlayer p,TycoonWorker actor,String kind){
        var h=TycoonWorld.home(p);var map=h.state.getCompound(actor.level()==p.server.overworld()?"Furniture":"OtherFurniture");var list=new ArrayList<TycoonBlockEntity>();int checked=0;
        for(String key:map.getAllKeys()){if(checked++>=256)break;if(!map.getString(key).equals(kind))continue;BlockPos at;try{at=BlockPos.of(Long.parseLong(key));}catch(NumberFormatException invalid){continue;}if(actor.distanceToSqr(at.getCenter())>64*64)continue;((ServerLevel)actor.level()).getChunkAt(at);if(actor.level().getBlockEntity(at) instanceof TycoonBlockEntity be&&h.owner.equals(be.owner)&&!be.getBlockState().getValue(TycoonBlock.BROKEN))list.add(be);}list.sort(Comparator.comparingDouble(be->actor.distanceToSqr(be.getBlockPos().getCenter())));return list;
    }
    private static void idle(ServerPlayer p,CompoundTag w,TycoonWorker actor,String status){w.putString("Task",status);TycoonWorkers.approach(p,w,actor,near(actor,BlockPos.of(w.getLong("Area"))));}
    private static void maid(ServerPlayer p,CompoundTag w,TycoonWorker actor,int rank){
        var h=TycoonWorld.home(p);w.putString("Task","CARING_FOR_RESIDENTS");var list=children(p,w,false);
        if(w.hasUUID("PlayingChild")){var playing=h.residents.get(w.getUUID("PlayingChild"));if(playing!=null&&playing.active()&&TycoonStaffPlay.step(p,w,actor,playing,npc(p,playing,actor)))return;w.remove("PlayingChild");}
        if(w.contains("Carry")&&TycoonHauling.item(w).is(Items.BUCKET)){fillWater(p,w,actor);return;}
        if(w.hasUUID("RequestChild")&&w.contains("Carry")){var requested=h.residents.get(w.getUUID("RequestChild"));if(requested!=null&&requested.active()&&requested.n.getString("Request").equals("ITEM")&&TycoonWarehouse.key(TycoonHauling.item(w).getItem()).equals(requested.n.getString("Wanted"))){if(reach(p,w,actor,npc(p,requested,actor))){TycoonHauling.consumed(p,w);TycoonCare.staffComplete(p,requested);w.remove("RequestChild");}return;}if(TycoonHauling.returnSupply(p,w))w.remove("RequestChild");return;}
        if(w.hasUUID("CareChild")&&w.contains("Carry")){var r=h.residents.get(w.getUUID("CareChild"));if(r!=null&&r.active()&&!TycoonVisits.visiting(r)){
            var child=npc(p,r,actor);boolean water=w.getCompound("Carry").getBoolean("Water")||TycoonHauling.item(w).is(Items.WATER_BUCKET);
            if(!water&&(r.need("Hunger")>=TycoonPolicies.number(h,"minHunger")||!TycoonMeals.ready(h,r))){if(TycoonHauling.returnSupply(p,w))w.remove("CareChild");return;}
            if(!reach(p,w,actor,child))return;if(water)TycoonHauling.hydrate(p,w,r);else TycoonHauling.meal(p,w,r,false,r.need("Neglect")>=TycoonPolicies.number(h,"neglectMax"));if(!w.contains("Carry"))w.remove("CareChild");return;
        }else{if(w.contains("Carry"))TycoonHauling.returnSupply(p,w);w.remove("CareChild");}}
        for(var r:list){
            if(r.need("Thirst")<TycoonPolicies.number(h,"minThirst")){
                w.putUUID("CareChild",r.id);for(var station:furniture(p,actor,"water_station"))if(station.water>0){if(TycoonWorkers.approach(p,w,actor,near(actor,station.getBlockPos())))TycoonHauling.water(p,w,station);return;}
                if(TycoonWorkers.approach(p,w,actor,near(actor,BlockPos.of(w.getLong("Area")))))TycoonHauling.fromWarehouse(p,w,TycoonWarehouse.count(h,Items.WATER_BUCKET)>0?Items.WATER_BUCKET:Items.BUCKET,1);return;
            }
            if(r.need("Hunger")<TycoonPolicies.number(h,"minHunger")&&TycoonMeals.ready(h,r)){
                w.putUUID("CareChild",r.id);for(var fridge:furniture(p,actor,"orphan_fridge")){int slot=foodSlot(fridge,rank);if(slot<0)continue;if(TycoonWorkers.approach(p,w,actor,near(actor,fridge.getBlockPos())))TycoonHauling.fromContainer(p,w,fridge,slot);return;}w.putString("Blocker","Supply the orphan fridge or hire a funded Chef.");return;
            }
            var child=npc(p,r,actor);boolean discretionary=r.need("Neglect")>=Math.min(74,TycoonPolicies.number(h,"neglectMin")+6);
            if(r.need("Neglect")>TycoonPolicies.number(h,"neglectMax")||discretionary&&r.n.getString("Request").equals("ATTENTION")){
                if(!reach(p,w,actor,child))return;int attention=r.n.getInt("StaffAttention")+100;r.n.putInt("StaffAttention",attention);child.gesture(2,100);if(attention>=200){r.n.putInt("StaffAttention",0);if(r.n.getString("Request").equals("ATTENTION"))TycoonCare.staffComplete(p,r);else r.need("Neglect",r.need("Neglect")-2);}return;
            }
            if(discretionary&&r.n.getString("Request").equals("ITEM")){
                var id=net.minecraft.resources.ResourceLocation.tryParse(r.n.getString("Wanted"));var item=id==null?null:ForgeRegistries.ITEMS.getValue(id);if(item==null)continue;
                if(w.contains("Carry")){if(TycoonHauling.item(w).is(item)&&reach(p,w,actor,child)){TycoonHauling.consumed(p,w);TycoonCare.staffComplete(p,r);}return;}
                if(TycoonWorkers.approach(p,w,actor,near(actor,BlockPos.of(w.getLong("Area"))))&&TycoonHauling.fromWarehouse(p,w,item,1))w.putUUID("RequestChild",r.id);return;
            }
            if(discretionary&&r.n.getString("Request").equals("CATCH")){w.putString("Task","APPROACHING_PLAY_PARTNER");if(reach(p,w,actor,child))TycoonStaffPlay.step(p,w,actor,r,child);return;}
        }
        if(w.contains("Carry")){TycoonHauling.returnSupply(p,w);return;}idle(p,w,actor,"AWAITING_CARE_NEEDS");
    }
    private static void fillWater(ServerPlayer p,CompoundTag w,TycoonWorker actor){
        var l=(ServerLevel)actor.level();var area=BlockPos.of(w.getLong("Area"));BlockPos source=w.contains("WaterSource")?BlockPos.of(w.getLong("WaterSource")):null;
        if(source==null||!l.getFluidState(source).isSource()||!l.getFluidState(source).is(net.minecraft.tags.FluidTags.WATER)){
            source=null;for(int dx=-8;dx<=8&&source==null;dx++)for(int dz=-8;dz<=8&&source==null;dz++)for(int dy=-2;dy<=2;dy++){var at=area.offset(dx,dy,dz);if(l.hasChunkAt(at)&&l.getFluidState(at).isSource()&&l.getFluidState(at).is(net.minecraft.tags.FluidTags.WATER)&&near(actor,at)!=null){source=at;break;}}
            if(source!=null)w.putLong("WaterSource",source.asLong());
        }
        if(source==null){w.putString("Blocker","Supply a real water source in the maid's working area and empty buckets in Supplies.");return;}
        w.putString("Task","FILLING_REAL_WATER_BUCKET");if(!TycoonWorkers.approach(p,w,actor,near(actor,source)))return;var state=l.getBlockState(source);if(state.getBlock() instanceof net.minecraft.world.level.block.BucketPickup pickup){var filled=pickup.pickupBlock(l,source,state);if(filled.is(Items.WATER_BUCKET))w.getCompound("Carry").put("Item",filled.save(new CompoundTag()));}TycoonWorld.dirty(p);
    }
    private static int foodSlot(TycoonBlockEntity fridge,int rank){int selected=-1;double best=-1;for(int i=0;i<fridge.getContainerSize();i++){var item=fridge.getItem(i);if(!item.isEdible())continue;double quality=rank<3?-i:TycoonFoodQuality.hunger(item);if(selected<0||quality>best){selected=i;best=quality;}}return selected;}
    private static Item warehouseFood(TycoonData.Home h,int rank){Item result=null;double best=-1;for(String key:h.state.getCompound("Warehouse").getAllKeys()){if(h.state.getCompound("Warehouse").getLong(key)<=0)continue;var id=net.minecraft.resources.ResourceLocation.tryParse(key);var item=id==null?null:ForgeRegistries.ITEMS.getValue(id);if(item==null)continue;var stack=new ItemStack(item);if(!stack.isEdible()||TycoonFoodContent.index(stack)>=0)continue;double quality=TycoonFoodQuality.hunger(stack);if(rank<3)quality=100-quality;if(result==null||quality>best){result=item;best=quality;}}return result;}
    private static void chef(ServerPlayer p,CompoundTag w,TycoonWorker actor,int rank,boolean personal){
        var h=TycoonWorld.home(p);w.putString("Task","RESTOCKING_FRIDGES");var fridges=furniture(p,actor,personal?"other_world_fridge":"orphan_fridge");
        for(var fridge:fridges){boolean room=false;for(int i=0;i<fridge.getContainerSize();i++)if(fridge.getItem(i).isEmpty()||fridge.getItem(i).getCount()<16){room=true;break;}if(!room)continue;
            if(w.contains("Carry")){if(TycoonWorkers.approach(p,w,actor,near(actor,fridge.getBlockPos())))TycoonHauling.toContainer(p,w,fridge);return;}
            if(!TycoonWorkers.approach(p,w,actor,near(actor,BlockPos.of(w.getLong("Area")))))return;
            if(personal){int index=Math.min((int)TycoonPolicies.number(h,"preferredFoodQuality"),Math.min(6,Math.max(0,(rank-1)*2)));var item=TycoonFoodContent.ITEMS.get(index).get();
                if(TycoonWarehouse.count(h,item)<4){long cost=TycoonFoodConfig.PRICES[index].get()*4L*TycoonSoulLedger.SCALE;if(TycoonWarehouse.total(h)>TycoonWarehouse.capacity(h)-4||!TycoonPolicies.budget(h,"food",cost,TycoonPolicies.number(h,"foodBudgetHour")*TycoonSoulLedger.SCALE)||!TycoonSoulLedger.debit(h,cost,TycoonPolicies.number(h,"reserve"))){w.putString("Blocker","Food budget, funds or supply capacity unavailable.");return;}TycoonWarehouse.add(h,item,4);TycoonPolicies.spent(h,"food",cost);TycoonFinance.record(h,"food",-cost);}
                TycoonHauling.fromWarehouse(p,w,item,4);
            }else{var item=warehouseFood(h,rank);if(item==null){w.putString("Blocker","Deposit real food into Supplies.");return;}TycoonHauling.fromWarehouse(p,w,item,(int)Math.min(8,TycoonWarehouse.count(h,item)));}return;
        }idle(p,w,actor,fridges.isEmpty()?"NEEDS_OWNED_FRIDGE":"FRIDGES_STOCKED");
    }
    private static void buyer(ServerPlayer p,CompoundTag w,TycoonWorker actor,int rank){
        var h=TycoonWorld.home(p);if(!TycoonPolicies.allowed(h,"placements")){idle(p,w,actor,"PLACEMENTS_NOT_AUTHORIZED");return;}if(!TycoonWorkers.approach(p,w,actor,near(actor,BlockPos.of(w.getLong("Area")))))return;var candidates=new ArrayList<>(TycoonAgency.candidates(h));
        // Candidate information is public prose until a separate observation unlock.
        TycoonCandidateSelection.order(candidates,TycoonPolicies.state(h).getString("CandidatePriority"),rank,w.getUUID("Id"),h.clock());
        if(!candidates.isEmpty())w.putString("LastResult",TycoonAgency.placeSupplied(p,candidates.get(0).id));w.putString("Task","AGENCY_PLACEMENT_COORDINATION");
    }
    private static void dolls(ServerPlayer p,CompoundTag w,TycoonWorker actor,int rank){
        var h=TycoonWorld.home(p);if(!TycoonPolicies.allowed(h,"dolls")){idle(p,w,actor,"DOLL_SPENDING_NOT_AUTHORIZED");return;}
        for(var r:children(p,w,false)){
            var d=r.n.getCompound("Doll");if(d.getString("Status").equals("PLACED"))continue;if(d.hasUUID("Id")&&!d.getString("Status").equals("WORKER_DELIVERY"))continue;
            w.putString("Task",d.hasUUID("Id")?"DELIVERING_PERSONALIZED_DOLL":"MAKING_PERSONALIZED_DOLL");
            if(!d.hasUUID("Id")){if(TycoonWorkers.approach(p,w,actor,near(actor,BlockPos.of(w.getLong("Area")))))TycoonDolls.craftForWorker(p,r);return;}
            actor.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,TycoonDolls.item(p,r,d));if(!w.hasUUID("DollHandoff")||!r.id.equals(w.getUUID("DollHandoff"))){if(reach(p,w,actor,npc(p,r,actor)))w.putUUID("DollHandoff",r.id);return;}if(TycoonWorkers.approach(p,w,actor,near(actor,r.bed()))&&TycoonDolls.deliverForWorker(p,r)){w.putLong("CompletedJobs",w.getLong("CompletedJobs")+1);w.remove("DollHandoff");}return;
        }idle(p,w,actor,"PERSONALIZED_DOLLS_CURRENT");
    }
    private static double known(TycoonData.Resident r,String trait,int rank){
        if(rank==5)return r.trait(trait);if(rank<3||TycoonDollAnalysis.progress(r)<.25)return .5;var definition=TycoonPersonality.definition(trait);if(definition==null)return .5;var interval=TycoonDollAnalysis.interval(r,definition);return (interval[0]+interval[1])/2;
    }
    private static double knownMatch(TycoonData.Resident r,TycoonAttractionType type,CompoundTag structure,int rank){double total=0;for(int i=0;i<TycoonAttractionType.INTERESTS.length;i++)total+=1-2*Math.abs(known(r,TycoonAttractionType.INTERESTS[i],rank)-type.quality(i,structure.getInt("Tier"),structure.getCompound("Branches")));return total/TycoonAttractionType.INTERESTS.length;}
    private static void manager(ServerPlayer p,CompoundTag w,TycoonWorker actor,int rank){
        var h=TycoonWorld.home(p);
        if(w.contains("Carry")){
            var recipient=w.hasUUID("CakeChild")?h.residents.get(w.getUUID("CakeChild")):null;
            if(recipient!=null&&TycoonWelcomeCake.eligible(p,recipient)){if(reach(p,w,actor,npc(p,recipient,actor))&&TycoonHauling.cake(p,w,recipient))w.remove("CakeChild");return;}
            if(TycoonHauling.returnSupply(p,w))w.remove("CakeChild");return;
        }
        for(var r:children(p,w,true)){
            if(r.n.hasUUID("RescuingFarm")||r.n.contains("ConversionRitual"))continue;var child=npc(p,r,actor);if(child==null)continue;
            if(TycoonDialogue.suspicion(r)>TycoonPolicies.number(h,"maximumVisitorSuspicionPercent")/100.){if(reach(p,w,actor,child)){var visit=r.n.getCompound("Visit");visit.putString("Phase","LEAVING");visit.putLong("LeavingAt",h.clock());w.putString("Task","ESCORTING_UNCOMFORTABLE_VISITOR_HOME");}return;}
            if(TycoonPolicies.allowed(h,"cakes")&&TycoonWelcomeCake.eligible(p,r)){
                if(w.contains("Carry")){if(reach(p,w,actor,child))TycoonHauling.cake(p,w,r);return;}
                var cake=TycoonFoodContent.ITEMS.get(7).get();long price=TycoonFoodConfig.PRICES[7].get()*TycoonSoulLedger.SCALE;
                if(TycoonWarehouse.count(h,cake)==0&&TycoonWarehouse.total(h)<TycoonWarehouse.capacity(h)&&TycoonPolicies.budget(h,"gifts",price,TycoonPolicies.number(h,"giftBudgetHour")*TycoonSoulLedger.SCALE)&&TycoonSoulLedger.debit(h,price,TycoonPolicies.number(h,"reserve"))){TycoonWarehouse.add(h,cake,1);TycoonPolicies.spent(h,"gifts",price);TycoonFinance.record(h,"cake",-price);}
                if(TycoonWarehouse.count(h,cake)>0&&TycoonWorkers.approach(p,w,actor,near(actor,BlockPos.of(w.getLong("Area"))))&&TycoonHauling.fromWarehouse(p,w,cake,1))w.putUUID("CakeChild",r.id);return;
            }
            if(TycoonPolicies.allowed(h,"proposals")&&h.clock()>=r.n.getLong("ProposalAfter")&&(rank==5?TycoonDialogue.trust(r)>=1:rank>=3?TycoonDialogue.trust(r)>.96&&Math.floorMod(Objects.hash(r.id,h.clock()/1200),8)==0:TycoonDialogue.trust(r)>.7&&Math.floorMod(Objects.hash(r.id,h.clock()/1200),12)==0)){
                if(!reach(p,w,actor,child))return;w.putString("Task","MAKING_BUTTON_EYE_OFFER");if(r.n.getLong("ProposalConfirmUntil")>h.clock())TycoonSouls.propose(p,r,true);else TycoonSouls.propose(p,r,false);return;
            }
            if(TycoonPolicies.allowed(h,"conversations")&&h.clock()>=r.n.getLong("TalkAfter")&&!r.n.contains("Activity")){
                if(!reach(p,w,actor,child))return;w.putString("Task","CONVERSING_WITH_VISITOR");var conversation=TycoonDialogue.open(p,r);if(conversation.hasUUID("Question")){int response=rank<3?Math.floorMod(Objects.hash(w.getUUID("Id"),h.clock()/200),3):0;TycoonDialogue.respond(p,r,conversation.getUUID("Question"),response);}return;
            }
            if(TycoonPolicies.allowed(h,"assignments")&&!r.n.contains("Activity")){
                UUID best=null;double fit=-100;for(String key:TycoonAttractions.records(h).getAllKeys()){var structure=TycoonAttractions.records(h).getCompound(key);var type=TycoonAttractionType.parse(structure.getString("Type"));if(TycoonAttractions.occupied(h,structure.getUUID("Id"))>=type.capacity||TycoonBuildJob.touches(h,TycoonAttractions.origin(structure)))continue;double score=rank<3?Math.floorMod(Objects.hash(r.id,key,h.clock()/1200),100)/100.:knownMatch(r,type,structure,rank);var fatigue=r.n.getCompound("AttractionFatigue").getCompound(key);score-=Math.max(0,fatigue.getDouble("Amount")-(h.clock()-fatigue.getLong("At"))/24000.);if(score>fit){fit=score;best=structure.getUUID("Id");}}
                if(best!=null&&reach(p,w,actor,child)){w.putString("Task","ASSIGNING_KNOWN_ATTRACTION");TycoonAttractions.assign(p,r,best);}return;
            }
        }idle(p,w,actor,"VISITOR_MANAGEMENT_CURRENT");
    }
    private static void gifts(ServerPlayer p,CompoundTag w,TycoonWorker actor,int rank){
        var h=TycoonWorld.home(p);if(!TycoonPolicies.allowed(h,"gifts")){idle(p,w,actor,"GIFT_DISTRIBUTION_NOT_AUTHORIZED");return;}
        for(String key:TycoonAttractions.records(h).getAllKeys()){
            var structure=TycoonAttractions.records(h).getCompound(key);var at=TycoonAttractions.local(structure,5,1,-5);((ServerLevel)actor.level()).getChunkAt(at);if(!(actor.level().getBlockEntity(at) instanceof TycoonBlockEntity cabinet)||!TycoonWorld.owner(p).equals(cabinet.owner))continue;
            boolean empty=true;for(int i=0;i<cabinet.getContainerSize();i++)if(!cabinet.getItem(i).isEmpty())empty=false;if(!empty)continue;
            if(!TycoonWorkers.reserve(h,w,"gift_cabinet:"+key))continue;w.putString("Task","STOCKING_REAL_GIFTS");
            if(w.contains("Carry")){if(TycoonWorkers.approach(p,w,actor,near(actor,at)))TycoonHauling.toContainer(p,w,cabinet);return;}
            Item best=null;double match=-1;for(String itemId:h.state.getCompound("Warehouse").getAllKeys()){
                var id=net.minecraft.resources.ResourceLocation.tryParse(itemId);var item=id==null?null:ForgeRegistries.ITEMS.getValue(id);if(item==null||TycoonWarehouse.count(h,item)<=0)continue;String interest=TycoonGifts.interest(new ItemStack(item));if(interest.isEmpty())continue;double score=0;int visitors=0;for(var r:h.residents.values())if(TycoonVisits.inside(r)&&known(r,"Giftableness",rank)>0){score+=known(r,interest,rank);visitors++;}if(visitors==0)score=.5;else score/=visitors;if(score>match){best=item;match=score;}}
            if(best!=null&&TycoonWorkers.approach(p,w,actor,near(actor,BlockPos.of(w.getLong("Area")))))TycoonHauling.fromWarehouse(p,w,best,(int)Math.min(4,TycoonWarehouse.count(h,best)));return;
        }idle(p,w,actor,"GIFT_STOCKS_CURRENT");
    }
    private static void guard(ServerPlayer p,CompoundTag w,TycoonWorker actor,int rank){
        var h=TycoonWorld.home(p);for(String key:TycoonSoulFarms.farms(h).getAllKeys()){
            var farm=TycoonSoulFarms.farms(h).getCompound(key);if(!TycoonSoulFarms.breaching(farm))continue;boolean detected=farm.getCompound("Breach").getBoolean("AlarmReported");for(var r:h.residents.values())if(r.n.hasUUID("RescuingFarm")&&farm.getUUID("Id").equals(r.n.getUUID("RescuingFarm"))&&r.n.getCompound("SpyMarker").getLong("Expires")>h.clock())detected=true;
            if(!detected||!TycoonWorkers.reserve(h,w,"guard_farm:"+key))continue;w.putString("Task","RESPONDING_TO_REPORTED_BREACH");if(TycoonWorkers.approach(p,w,actor,near(actor,TycoonSoulFarms.local(farm,-4,1,-2))))TycoonSoulFarms.secureByWorker(p,actor,farm);return;
        }
        for(var r:h.residents.values()){
            var marker=r.n.getCompound("SpyMarker");if(marker.getLong("Expires")<=h.clock()||r.n.hasUUID("RescuingFarm")||!TycoonWorkers.reserve(h,w,"guard_child:"+r.id))continue;w.putString("Task","INTERCEPTING_REPORTED_VISITOR");var last=BlockPos.of(marker.getLong("At"));if(!TycoonWorkers.approach(p,w,actor,near(actor,last)))return;var child=npc(p,r,actor);if(child!=null&&actor.distanceToSqr(child)<36&&TycoonEvidence.perceives(actor,child.getEyePosition(),8,-.4)){actor.getLookControl().setLookAt(child,30,30);TycoonAttractions.returnHouse(p,r);TycoonWorld.message(p,r.name()+" is being escorted back after a reported sighting.");}return;
        }
        var plots=new ArrayList<BlockPos>();if(w.getString("Zone").equals("Soul Farm District"))for(String key:TycoonSoulFarms.farms(h).getAllKeys())plots.add(TycoonSoulFarms.local(TycoonSoulFarms.farms(h).getCompound(key),0,1,-7));else if(w.getString("Zone").equals("Attraction District"))for(String key:TycoonAttractions.records(h).getAllKeys())plots.add(TycoonAttractions.local(TycoonAttractions.records(h).getCompound(key),0,1,-7));
        if(plots.isEmpty())plots.add(BlockPos.of(w.getLong("Area")));var goal=plots.get(Math.floorMod((int)(h.clock()/400)+w.getUUID("Id").hashCode(),plots.size()));w.putString("Task","PATROLLING_"+w.getString("Zone"));TycoonWorkers.approach(p,w,actor,near(actor,goal));
    }
    private static void souls(ServerPlayer p,CompoundTag w,TycoonWorker actor,int rank){
        var h=TycoonWorld.home(p);w.putString("Task","MANAGING_CAPTURED_SOULS");var collection=TycoonSouls.collection(h);
        if(rank>=4){
            if(w.contains("SoulRebalance")){var plan=w.getCompound("SoulRebalance");var farm=TycoonSoulFarms.farms(h).getCompound(plan.getUUID("To").toString());if(farm.isEmpty()){w.remove("SoulRebalance");return;}w.putString("Task","IMPROVING_MEASURED_SOUL_RESONANCE");if(TycoonWorkers.approach(p,w,actor,near(actor,TycoonSoulFarms.local(farm,-4,1,-2)))){if(TycoonSoulAllocation.commit(p,plan))w.putLong("CompletedJobs",w.getLong("CompletedJobs")+1);w.remove("SoulRebalance");}return;}
            if(h.clock()>=w.getLong("RebalanceAfter")){w.putLong("RebalanceAfter",h.clock()+1200);var plan=TycoonSoulAllocation.best(h);if(!plan.isEmpty()){w.put("SoulRebalance",plan);return;}}
        }
        for(String key:collection.getAllKeys()){
            var soul=collection.getCompound(key);if(!TycoonSouls.usable(soul)||!TycoonWorkers.reserve(h,w,"soul:"+key))continue;
            boolean unwanted=soul.getDouble("Quality")<TycoonPolicies.number(h,"minSoulQualityPercent")/100.||soul.getDouble("Restlessness")>TycoonPolicies.number(h,"maxRestlessnessPercent")/100.||soul.getDouble("Stability")<TycoonPolicies.number(h,"minimumStabilityPercent")/100.||soul.getLong("BaseMicros")<TycoonPolicies.number(h,"minimumBaseProductionMilli")*1000;
            String preferred=TycoonPolicies.state(h).getString("PreferredSoulSpecialization");if(!preferred.equals("Any")&&!preferred.equals(soul.getString("Primary"))&&!preferred.equals(soul.getString("Secondary")))unwanted=true;
            boolean rare=soul.getDouble("Quality")>=.85||soul.getDouble("Resonance")>=.95;
            if(unwanted&&TycoonPolicies.allowed(h,"autoRelease")&&!soul.getBoolean("Protected")&&!(rare&&TycoonPolicies.allowed(h,"keepRareSouls"))){
                if(!TycoonWorkers.approach(p,w,actor,near(actor,BlockPos.of(w.getLong("Area")))))return;TycoonSouls.release(p,soul.getUUID("Id"),false);TycoonSouls.release(p,soul.getUUID("Id"),true);return;
            }
            if(soul.hasUUID("Farm"))continue;UUID best=null;double score=-1;
            for(String farmId:TycoonSoulFarms.farms(h).getAllKeys()){
                var farm=TycoonSoulFarms.farms(h).getCompound(farmId);var assigned=TycoonSoulFarms.assigned(h,farm.getUUID("Id"));if(farm.getBoolean("Damaged")||TycoonSoulFarms.breaching(farm)||assigned.size()>=TycoonSoulFarms.capacity(farm))continue;assigned.add(soul);double fit=rank<3?1:TycoonSoulFarms.resonance(assigned);if(fit>score){score=fit;best=farm.getUUID("Id");}
            }
            if(best!=null){var farm=TycoonSoulFarms.farms(h).getCompound(best.toString());if(TycoonWorkers.approach(p,w,actor,near(actor,TycoonSoulFarms.local(farm,-4,1,-2))))TycoonSouls.assign(p,soul.getUUID("Id"),best);return;}
            if(TycoonPolicies.allowed(h,"farmUpgrades"))for(String farmId:TycoonSoulFarms.farms(h).getAllKeys()){var farm=TycoonSoulFarms.farms(h).getCompound(farmId);if(farm.getInt("Tier")<5&&!farm.getBoolean("Damaged")&&!TycoonSoulFarms.breaching(farm)){if(TycoonWorkers.approach(p,w,actor,near(actor,TycoonSoulFarms.local(farm,0,1,-7))))w.putString("LastResult",TycoonSoulFarms.upgrade(p,farm.getUUID("Id"),false));return;}}
            if(TycoonPolicies.allowed(h,"farmPurchases")&&TycoonSoulFarms.farms(h).size()<30){var tile=freePlot(p);if(tile!=null){var target=h.center().offset(tile[0]*16+8,0,tile[1]*16);if(TycoonWorkers.approach(p,w,actor,near(actor,target))){TycoonSoulFarms.preview(p,tile[0],tile[1],0);w.putString("LastResult",TycoonSoulFarms.confirm(p));}}return;}
        }idle(p,w,actor,"SOUL_ASSIGNMENTS_CURRENT");
    }
    private static List<int[]> plots(TycoonData.Home h){var result=new ArrayList<int[]>();for(int x=-8;x<8;x++)for(int z=-8;z<8;z++)result.add(new int[]{x,z});int direction=(int)TycoonPolicies.number(h,"expansionDirection");result.sort(Comparator.<int[]>comparingInt(tile->{int x=tile[0]*16+8,z=tile[1]*16+8;boolean preferred=switch(direction){case 1->z<0;case 2->x>0;case 3->z>0;case 4->x<0;default->true;};return x*x+z*z+(preferred?0:100000);}).thenComparingInt(tile->tile[0]).thenComparingInt(tile->tile[1]));return result;}
    private static int[] freePlot(ServerPlayer p){
        var h=TycoonWorld.home(p);var blocked=new HashSet<String>();for(String key:h.state.getCompound("PlayerEdits").getAllKeys())try{var at=BlockPos.of(Long.parseLong(key));blocked.add(TycoonLandscape.key(TycoonLandscape.tileX(h,at),TycoonLandscape.tileZ(h,at)));}catch(NumberFormatException invalid){/* Retain invalid provenance; never reinterpret it as a block position. */}
        int houseRadius=new int[]{0,3,6,9,12,16}[Math.max(0,Math.min(5,h.state.getInt("HouseTier")))];
        for(var tile:plots(h)){int x=tile[0],z=tile[1];
            if(!h.state.getCompound("Terrain").contains(TycoonLandscape.key(x,z))||TycoonPolicies.state(h).getBoolean("reserve_tile_"+x+","+z))continue;var center=h.center().offset(x*16+8,0,z*16+8);boolean occupied=false;
            if(blocked.contains(TycoonLandscape.key(x,z))||Math.abs(x*16+8)<=Math.max(8,houseRadius+7)&&Math.abs(z*16+8)<=Math.max(8,houseRadius+7))continue;
            for(var all:List.of(TycoonAttractions.records(h),TycoonSoulFarms.farms(h),TycoonMachines.machines(h)))for(String key:all.getAllKeys())if(Math.abs(TycoonAttractions.origin(all.getCompound(key)).getX()-center.getX())<14&&Math.abs(TycoonAttractions.origin(all.getCompound(key)).getZ()-center.getZ())<14)occupied=true;
            if(!occupied)return new int[]{x,z};
        }return null;
    }
    private static void structures(ServerPlayer p,CompoundTag w,TycoonWorker actor,int rank){
        var h=TycoonWorld.home(p);if(!TycoonPolicies.allowed(h,"construction")||h.state.contains("BuildJob")){idle(p,w,actor,"CONSTRUCTION_DISABLED_OR_BUSY");return;}
        if(TycoonLandscape.goodness(h).total()<TycoonPolicies.number(h,"desiredGoodness")){
            if(TycoonPolicies.allowed(h,"skyUpgrades")&&h.state.getInt("SkyTier")<Math.min(5,rank)){w.putString("Task","UPGRADING_AUTHORIZED_SKY");if(TycoonWorkers.approach(p,w,actor,near(actor,BlockPos.of(w.getLong("Area")))))w.putString("LastResult",TycoonSkies.select(p,h.state.getInt("SkyTier")+1));return;}
            if(TycoonPolicies.allowed(h,"houseUpgrades")&&h.state.getInt("HouseTier")<Math.min(5,rank)){w.putString("Task","UPGRADING_AUTHORIZED_HOUSE");if(TycoonWorkers.approach(p,w,actor,near(actor,h.center().offset(0,0,18)))){String result=TycoonHouses.purchase(p,h.state.getInt("HouseTier")+1);w.putString("LastResult",result);if(result.startsWith("Building"))return;}}
            if(TycoonPolicies.allowed(h,"terrainUpgrades"))for(String key:h.state.getCompound("Terrain").getAllKeys()){var tile=h.state.getCompound("Terrain").getCompound(key);if(tile.getInt("Tier")>=Math.min(5,rank))continue;String[] parts=key.split(",");w.putString("Task","UPGRADING_AUTHORIZED_TERRAIN");w.putString("LastResult",TycoonLandscape.purchase(p,Integer.parseInt(parts[0]),Integer.parseInt(parts[1]),tile.getInt("Tier")+1));return;}
        }
        if(TycoonPolicies.allowed(h,"attractionUpgrades"))for(String key:TycoonAttractions.records(h).getAllKeys()){var row=TycoonAttractions.records(h).getCompound(key);if(!TycoonPolicies.state(h).getBoolean("allow_structure_"+row.getString("Type"))||TycoonAttractions.occupied(h,row.getUUID("Id"))>0)continue;int branch=upgradeBranch(h,row,rank);if(branch==-2&&row.getInt("Tier")<Math.min(5,rank))branch=-1;if(branch==-2)continue;w.putString("Task","UPGRADING_AUTHORIZED_ATTRACTION");if(TycoonWorkers.approach(p,w,actor,near(actor,TycoonAttractions.local(row,0,1,-7))))w.putString("LastResult",TycoonAttractions.upgrade(p,row.getUUID("Id"),branch));return;}
        var tile=freePlot(p);if(tile==null){for(var candidate:plots(h)){int x=candidate[0],z=candidate[1];if(TycoonLandscape.adjacent(h,x,z)&&!h.state.getCompound("Terrain").contains(TycoonLandscape.key(x,z))&&!TycoonPolicies.state(h).getBoolean("reserve_tile_"+x+","+z)){w.putString("Task","PURCHASING_APPROVED_TERRAIN");w.putString("LastResult",TycoonLandscape.purchase(p,x,z,Math.min(5,rank)));return;}}return;}
        TycoonAttractionType best=null;double fit=-100;for(var type:TycoonAttractionType.values())if(TycoonPolicies.state(h).getBoolean("allow_structure_"+type.id())){double score=0;int count=0;var structure=new CompoundTag();structure.putInt("Tier",1);for(var r:h.residents.values())if(r.active()){score+=knownMatch(r,type,structure,rank);count++;}score=count==0?0:score/count;long existing=TycoonAttractions.records(h).getAllKeys().stream().filter(key->TycoonAttractions.records(h).getCompound(key).getString("Type").equals(type.id())).count();score-=existing*.2;if(score>fit){fit=score;best=type;}}
        if(best==null){idle(p,w,actor,"NO_STRUCTURE_TYPES_AUTHORIZED");return;}var target=h.center().offset(tile[0]*16+8,0,tile[1]*16);w.putString("Task","BUILDING_APPROVED_ATTRACTION");if(TycoonWorkers.approach(p,w,actor,near(actor,target))){TycoonAttractions.preview(p,best.id(),tile[0],tile[1],0);w.putString("LastResult",TycoonAttractions.confirm(p));}
    }
    private static int upgradeBranch(TycoonData.Home h,CompoundTag row,int rank){
        int preferred=List.of("First","Second","Third").indexOf(TycoonPolicies.state(h).getString("PreferredBranch")),selected=-2;double best=-Double.MAX_VALUE;var type=TycoonAttractionType.parse(row.getString("Type"));
        for(int i=0;i<3;i++){if(row.getCompound("Branches").getInt(Integer.toString(i))>=Math.min(3,row.getInt("Tier")))continue;if(i==preferred)return i;var improved=row.copy();var branches=improved.getCompound("Branches");branches.putInt(Integer.toString(i),branches.getInt(Integer.toString(i))+1);improved.put("Branches",branches);double score=0;for(var r:h.residents.values())if(r.active())score+=knownMatch(r,type,improved,rank);if(selected==-2||score>best){best=score;selected=i;}}
        return selected;
    }
    private static void maintenance(ServerPlayer p,CompoundTag w,TycoonWorker actor,int rank){
        var h=TycoonWorld.home(p);if(!TycoonPolicies.allowed(h,"repairs")||h.state.contains("BuildJob")){idle(p,w,actor,"REPAIRS_NOT_AUTHORIZED_OR_BUSY");return;}for(String key:TycoonSoulFarms.farms(h).getAllKeys()){var farm=TycoonSoulFarms.farms(h).getCompound(key);if(!farm.getBoolean("Damaged")||TycoonSoulFarms.breaching(farm))continue;if(!TycoonWorkers.reserve(h,w,"repair:"+key))continue;w.putString("Task","REPAIRING_DAMAGED_CONTAINMENT");if(TycoonWorkers.approach(p,w,actor,near(actor,TycoonSoulFarms.local(farm,0,1,-7))))w.putString("LastResult",TycoonSoulFarms.upgrade(p,farm.getUUID("Id"),true));return;}
        var level=p.server.getLevel(TycoonWorld.KEY);if(level!=null)for(boolean machine:new boolean[]{true,false}){var records=machine?TycoonMachines.machines(h):TycoonAttractions.records(h);for(String key:records.getAllKeys()){var row=records.getCompound(key);if(!level.hasChunkAt(TycoonAttractions.origin(row))||!TycoonMaintenance.damaged(level,row)||!TycoonWorkers.reserve(h,w,"repair:"+key))continue;w.putString("Task",machine?"REPAIRING_PAID_MACHINE":"REPAIRING_OWNED_ATTRACTION");if(TycoonWorkers.approach(p,w,actor,near(actor,TycoonAttractions.origin(row).offset(0,1,-7))))w.putString("LastResult",TycoonMaintenance.repair(p,row.getUUID("Id"),machine));return;}}
        idle(p,w,actor,"MAINTENANCE_CURRENT");
    }
    private static void accountant(ServerPlayer p,CompoundTag w,TycoonWorker actor,int rank){
        var h=TycoonWorld.home(p);if(!TycoonPolicies.allowed(h,"investing")||h.clock()<w.getLong("TradeAfter")){idle(p,w,actor,"INVESTMENT_POLICY_WAIT");return;}if(!TycoonWorkers.approach(p,w,actor,near(actor,BlockPos.of(w.getLong("Area")))))return;var market=TycoonMarket.get(p.server);TycoonMarket.Company best=null;double bestScore=0;
        for(var company:TycoonMarket.COMPANIES){if(!TycoonPolicies.state(h).getBoolean("allow_company_"+company.id()))continue;var history=market.history(company.id());if(history.length<3)continue;var holding=h.state.getCompound("Shares").getCompound(company.id());long quantity=holding.getLong("Quantity"),basis=holding.getLong("CostBasis"),value=market.price(company.id())*quantity;
            if(quantity>0&&(value<basis*(100-TycoonPolicies.number(h,"stopLossPercent"))/100||history[history.length-1]<history[Math.max(0,history.length-4)]*.95)){w.putString("LastResult",TycoonMarket.trade(p,company.id(),(int)Math.min(10000,quantity),false,market.revision(),UUID.nameUUIDFromBytes((w.getUUID("Id")+":"+h.clock()).getBytes(java.nio.charset.StandardCharsets.UTF_8)),true));w.putLong("TradeAfter",h.clock()+TycoonPolicies.number(h,"tradeCooldown"));return;}
            double momentum=(history[history.length-1]-history[Math.max(0,history.length-(rank>=3?10:3))])/(double)history[Math.max(0,history.length-(rank>=3?10:3))];double risk=TycoonPolicies.state(h).getString("Risk").equals("Aggressive")?.1:TycoonPolicies.state(h).getString("Risk").equals("Balanced")?.5:1;double score=momentum-company.volatility()*risk;if(score>bestScore){bestScore=score;best=company;}
        }
        if(best!=null){w.putString("Task","TRADING_PUBLIC_PRICE_HISTORY");w.putString("LastResult",TycoonMarket.trade(p,best.id(),1,true,market.revision(),UUID.nameUUIDFromBytes((w.getUUID("Id")+":"+h.clock()).getBytes(java.nio.charset.StandardCharsets.UTF_8)),true));w.putLong("TradeAfter",h.clock()+TycoonPolicies.number(h,"tradeCooldown"));}else w.putString("Task","NO_STRATEGY_SIGNAL");
    }
    private static void master(ServerPlayer p,CompoundTag w,TycoonWorker actor,int rank){
        var h=TycoonWorld.home(p);w.putString("Task","COORDINATING_"+TycoonPolicies.state(h).getString("Strategy"));String strategy=TycoonPolicies.state(h).getString("Strategy");
        for(String key:TycoonWorkers.workers(h).getAllKeys()){var row=TycoonWorkers.workers(h).getCompound(key);var type=TycoonWorkerType.parse(row.getString("Type"));if(strategy.equals("Maximum Safety"))row.putInt("Priority",type==TycoonWorkerType.OVERWORLD_MAID?100:type==TycoonWorkerType.GUARD?95:type==TycoonWorkerType.TECHNICIAN?90:40);else if(strategy.equals("Maximum Growth"))row.putInt("Priority",type==TycoonWorkerType.ORPHAN_BUYER||type==TycoonWorkerType.STRUCTURE_MANAGER?85:type==TycoonWorkerType.OVERWORLD_MAID?100:50);else if(strategy.equals("Maximum Profit"))row.putInt("Priority",type==TycoonWorkerType.SOUL_MANAGER||type==TycoonWorkerType.OTHER_MANAGER?90:type==TycoonWorkerType.OVERWORLD_MAID?100:40);
            if(rank>=3&&TycoonPolicies.allowed(h,"salaryDowngrade")&&!row.getBoolean("Break")&&h.state.getLong("SoulEssence")>TycoonPolicies.number(h,"reserve")+120)row.putInt("Operating",Math.min(row.getInt("Unlocked"),rank));
        }TycoonWorkers.approach(p,w,actor,near(actor,BlockPos.of(w.getLong("Area"))));
    }
    private TycoonWorkerJobs(){}
}
