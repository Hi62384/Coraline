package com.coraline.tycoon;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** One owner scheduler: salary first, bounded physical jobs, shared reservations. */
public final class TycoonWorkers {
    private record Context(UUID owner,UUID worker,TycoonWorkerType type){}
    private static final ThreadLocal<Context> CONTEXT=new ThreadLocal<>();
    public static CompoundTag workers(TycoonData.Home h){return h.state.getCompound("Workers");}
    public static int capacity(int rank){return new int[]{2,4,8,16,32}[Math.max(0,Math.min(4,rank-1))];}
    public static UUID contextWorker(ServerPlayer p){var c=CONTEXT.get();return c!=null&&c.owner.equals(TycoonWorld.owner(p))?c.worker:null;}
    public static boolean constructing(ServerPlayer p){var c=CONTEXT.get();return c!=null&&c.owner.equals(TycoonWorld.owner(p))&&Set.of(TycoonWorkerType.STRUCTURE_MANAGER,TycoonWorkerType.SOUL_MANAGER,TycoonWorkerType.TECHNICIAN).contains(c.type);}
    public static boolean canConstruct(ServerPlayer p,int power,long soul){
        if(!constructing(p))return false;var h=TycoonWorld.home(p);var context=CONTEXT.get();var w=workers(h).getCompound(context.worker.toString());return permittedConstruction(h,w,power,soul);
    }
    private static boolean permittedConstruction(TycoonData.Home h,CompoundTag worker,int power,long soul){
        if(worker.isEmpty()||worker.getBoolean("Break")||!worker.getBoolean("Paid"))return false;var type=TycoonWorkerType.parse(worker.getString("Type"));String flag=type==TycoonWorkerType.TECHNICIAN?"repairs":type==TycoonWorkerType.SOUL_MANAGER?"farmPurchases":"construction";
        return (TycoonPolicies.allowed(h,flag)||type==TycoonWorkerType.SOUL_MANAGER&&TycoonPolicies.allowed(h,"farmUpgrades"))&&TycoonWarehouse.count(h,TycoonContent.VOID_POWER.get())>=power&&TycoonSoulLedger.canDebit(h,soul*TycoonSoulLedger.SCALE,TycoonPolicies.number(h,"reserve"))&&TycoonPolicies.budget(h,"constructionPower",power,TycoonPolicies.number(h,"constructionPowerHour"))&&TycoonPolicies.budget(h,"farmSoul",soul,TycoonPolicies.number(h,"farmSoulBudgetHour"));
    }
    public static String payConstruction(ServerPlayer p,UUID worker,int power,long soul,CompoundTag patch){
        var h=TycoonWorld.home(p);var row=workers(h).getCompound(worker.toString());if(!permittedConstruction(h,row,power,soul))return "Worker funding or construction permission changed.";
        if(!TycoonWarehouse.purchase(p,power,patch,null,new CompoundTag()))return "Worker supplies unavailable.";TycoonPolicies.spent(h,"constructionPower",power);TycoonPolicies.spent(h,"farmSoul",soul);TycoonFinance.record(h,"construction",-soul*TycoonSoulLedger.SCALE);row.putLong("CompletedJobs",row.getLong("CompletedJobs")+1);return "Purchased using authorized worker supplies.";
    }
    public static String hire(ServerPlayer p,String id){
        var h=TycoonWorld.home(p);var type=TycoonWorkerType.parse(id);if(workers(h).size()>=TycoonAutomationConfig.MAX_WORKERS.get())return "Worker limit reached.";
        var row=new CompoundTag();var worker=UUID.randomUUID();row.putUUID("Id",worker);row.putUUID("Owner",h.owner);row.putInt("BodyAdmissionVersion",1);row.putString("Type",type.id());row.putInt("Unlocked",1);row.putInt("Operating",1);row.putBoolean("Break",true);row.putString("Task","ON_BREAK");row.putString("Zone",type.overworld?"Orphanage":"Central House");row.putInt("Priority",type==TycoonWorkerType.OVERWORLD_MAID?100:type==TycoonWorkerType.GUARD?90:50);row.putLong("Area",type.overworld?(h.entrance()==null?p.blockPosition():h.entrance()).asLong():h.center().asLong());
        var all=workers(h).copy();all.put(worker.toString(),row);var patch=new CompoundTag();patch.put("Workers",all);return TycoonTransactions.purchase(p,TycoonAutomationConfig.PRICES.get(type.id()).get(),ItemStack.EMPTY,patch,null,new CompoundTag());
    }
    public static String configure(ServerPlayer p,UUID id,String key,String value){
        var h=TycoonWorld.home(p);var row=workers(h).getCompound(id.toString());if(row.isEmpty())return "Worker unavailable.";
        switch(key){case "break"->{row.putBoolean("Break",Boolean.parseBoolean(value));row.putBoolean("Paid",false);}case "level"->{int rank=Integer.parseInt(value);if(rank<1||rank>row.getInt("Unlocked"))return "Operating level not unlocked.";row.putInt("Operating",rank);row.putBoolean("Paid",false);}case "priority"->row.putInt("Priority",Math.max(0,Math.min(100,Integer.parseInt(value))));case "zone"->{if(!List.of("Central House","Attraction District","Soul Farm District","Restricted Facilities","General Terrain","Orphanage").contains(value))return "Unknown patrol zone.";row.putString("Zone",value);}case "area"->{if(TycoonWorkerType.parse(row.getString("Type")).overworld&&p.serverLevel()!=p.server.overworld()||!TycoonWorkerType.parse(row.getString("Type")).overworld&&p.serverLevel().dimension()!=TycoonWorld.KEY)return "Stand in the worker's correct dimension.";row.putLong("Area",p.blockPosition().asLong());}default->{return "Unknown worker policy.";}}
        TycoonWorld.dirty(p);return "Worker policy saved.";
    }
    public static String upgrade(ServerPlayer p,UUID id){var h=TycoonWorld.home(p);var row=workers(h).getCompound(id.toString());if(row.isEmpty()||row.getInt("Unlocked")>=5)return "Worker already at maximum, or unavailable.";int next=row.getInt("Unlocked")+1;var all=workers(h).copy();var changed=all.getCompound(id.toString());changed.putInt("Unlocked",next);var patch=new CompoundTag();patch.put("Workers",all);return TycoonTransactions.purchase(p,next*12,ItemStack.EMPTY,patch,null,new CompoundTag());}
    public static long salaryMicros(CompoundTag worker){TycoonWorkerType type;try{type=TycoonWorkerType.parse(worker.getString("Type"));}catch(IllegalArgumentException invalid){return 0;}int rank=Math.max(1,Math.min(5,Math.min(worker.getInt("Unlocked"),worker.getInt("Operating"))));return Math.round(TycoonAutomationConfig.SALARIES.get(type.id()).get(rank-1).get()*TycoonSoulLedger.SCALE);}
    private static boolean bill(TycoonData.Home h,CompoundTag w){
        if(w.getBoolean("Break")){w.putBoolean("Paid",false);return false;}long perMinute=salaryMicros(w),sum=perMinute+w.getLong("SalaryRemainder"),due=sum/60;
        if(!TycoonPolicies.budget(h,"payroll",due,TycoonPolicies.number(h,"payrollPerMinute")*60*TycoonSoulLedger.SCALE)||!TycoonSoulLedger.debit(h,due,TycoonPolicies.number(h,"reserve"))){
            if(TycoonPolicies.allowed(h,"salaryDowngrade")&&w.getInt("Operating")>1){w.putInt("Operating",w.getInt("Operating")-1);return bill(h,w);}w.putBoolean("Paid",false);w.putString("Task","UNPAID_PAUSED");return false;
        }
        w.putLong("SalaryRemainder",sum%60);w.putLong("SalaryPaid",w.getLong("SalaryPaid")+due);w.putBoolean("Paid",true);w.putLong("PaidAt",h.clock());TycoonPolicies.spent(h,"payroll",due);TycoonFinance.record(h,"payroll",-due);return true;
    }
    public static void tick(ServerPlayer p){
        var h=TycoonWorld.home(p);h.state.putLong("WorkerOwnerLease",p.server.overworld().getGameTime());var list=new ArrayList<CompoundTag>();for(String key:workers(h).getAllKeys()){var row=workers(h).getCompound(key);try{if(!row.hasUUID("Id")||row.getInt("Unlocked")<1||row.getInt("Unlocked")>5)throw new IllegalArgumentException();TycoonWorkerType.parse(row.getString("Type"));list.add(row);}catch(IllegalArgumentException invalid){row.putBoolean("Break",true);row.putBoolean("Paid",false);row.putString("Task","INVALID_RECORD_PRESERVED");}}
        // Pay essential priorities first, then age physical jobs independently.
        // Fair job scheduling must not let an accountant take a guard's payroll.
        list.sort(Comparator.<CompoundTag>comparingInt(w->w.getInt("Priority")).reversed().thenComparing(w->w.getUUID("Id").toString()));
        var ready=new HashMap<UUID,TycoonWorker>();for(var w:list){if(w.getLong("LastTick")==h.clock())continue;w.putLong("LastTick",h.clock());var actor=actor(p,w);boolean paid=actor!=null&&bill(h,w);if(actor==null)w.putBoolean("Paid",false);
            if(actor!=null){actor.presentation(w.getInt("Operating"),paid?2:0);if(!paid){actor.getNavigation().stop();continue;}if(w.contains("Carry"))actor.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.of(w.getCompound("Carry").getCompound("Item")));else actor.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);TycoonStaffPlay.hold(p,w,actor);ready.put(w.getUUID("Id"),actor);}
        }
        list.sort(Comparator.<CompoundTag>comparingLong(w->w.getLong("LastJobAt")).thenComparing(Comparator.<CompoundTag>comparingInt(w->w.getInt("Priority")).reversed()).thenComparing(w->w.getUUID("Id").toString()));
        int started=0;for(var w:list){var actor=ready.get(w.getUUID("Id"));if(actor==null||started>=4||h.clock()<w.getLong("NextTask"))continue;
            started++;w.putLong("LastJobAt",h.clock());w.putLong("NextTask",h.clock()+100);
            var type=TycoonWorkerType.parse(w.getString("Type"));int rank=w.getInt("Operating");
            // Imperfect levels make actual scheduling/choice errors, not idle penalties.
            if(rank<5&&Math.floorMod(Objects.hash(w.getUUID("Id"),h.clock()/100),100)<(5-rank)*8){w.putString("Task","RESCHEDULING");w.putLong("Mistakes",w.getLong("Mistakes")+1);w.putLong("NextTask",h.clock()+(6-rank)*40);continue;}
            CONTEXT.set(new Context(h.owner,w.getUUID("Id"),type));try{TycoonWorkerJobs.step(p,w,actor,type,rank);}finally{CONTEXT.remove();}
        }
        var reservations=h.state.getCompound("WorkerReservations");for(String key:new ArrayList<>(reservations.getAllKeys()))if(reservations.getCompound(key).getLong("Expires")<h.clock())reservations.remove(key);TycoonWorld.dirty(p);
    }
    public static boolean reserve(TycoonData.Home h,CompoundTag worker,String key){var all=h.state.getCompound("WorkerReservations");var old=all.getCompound(key);if(old.getLong("Expires")>=h.clock()&&old.hasUUID("Worker")&&!old.getUUID("Worker").equals(worker.getUUID("Id")))return false;var n=new CompoundTag();n.putUUID("Worker",worker.getUUID("Id"));n.putLong("Expires",h.clock()+1200);all.put(key,n);h.state.put("WorkerReservations",all);return true;}
    public static boolean nearbyCarer(ServerPlayer p,TycoonNpc child){var h=TycoonWorld.home(p);for(String key:workers(h).getAllKeys()){var row=workers(h).getCompound(key);if(!row.getString("Type").equals("overworld_maid")||row.getBoolean("Break")||!row.getBoolean("Paid")||!row.hasUUID("Actor"))continue;if(child.level() instanceof ServerLevel level&&level.getEntity(row.getUUID("Actor")) instanceof TycoonWorker worker&&worker.distanceToSqr(child)<100)return true;}return false;}
    private static TycoonWorker actor(ServerPlayer p,CompoundTag w){
        var h=TycoonWorld.home(p);var type=TycoonWorkerType.parse(w.getString("Type"));var target=type.overworld?p.server.overworld():p.server.getLevel(TycoonWorld.KEY);if(target==null){w.putString("Task","WAITING_FOR_WORKER_WORLD");return null;}
        var area=BlockPos.of(w.getLong("Area"));var at=w.contains("LastAt")?BlockPos.of(w.getLong("LastAt")):area;
        if(w.hasUUID("Actor")){
            var source=w.contains("ActorDimension",Tag.TAG_STRING)?com.coraline.parents.FamilyData.level(p.server,w.getString("ActorDimension")):target;
            if(source!=null)com.coraline.feast.LoopProps.hold(source,at);
            net.minecraft.world.entity.Entity found=null;for(var world:p.server.getAllLevels()){var candidate=world.getEntity(w.getUUID("Actor"));if(candidate==null)continue;if(found!=null){w.putString("Task","AMBIGUOUS_WORKER_IDENTITY");return null;}found=candidate;}
            if(found==null){w.putString("Task","WAITING_FOR_ORIGINAL_WORKER");return null;}
            if(!(found instanceof TycoonWorker actor)||!h.owner.equals(actor.owner())||!w.getUUID("Id").equals(actor.worker)||actor.role()!=type||!actor.isAlive()){w.putString("Task","WORKER_IDENTITY_UNAVAILABLE");return null;}
            w.putString("ActorDimension",actor.level().dimension().location().toString());w.putLong("LastAt",actor.blockPosition().asLong());w.remove("MissingTicks");
            if(actor.level()!=target){actor.getNavigation().stop();w.putString("Task","WORKER_IN_DIFFERENT_WORLD");return null;}
            if(w.getBoolean("ActorCheckpointPending")){if(!workerCheckpoint(p,w))return null;w.remove("ActorCheckpointPending");TycoonWorld.dirty(p);}
            return actor;
        }
        if(!w.contains("BodyAdmissionVersion",Tag.TAG_INT)||w.getInt("BodyAdmissionVersion")!=1){
            var recorded=w.contains("ActorDimension",Tag.TAG_STRING)?com.coraline.parents.FamilyData.level(p.server,w.getString("ActorDimension")):target;if(recorded!=null)com.coraline.feast.LoopProps.hold(recorded,at);
            if(p.server.overworld().getGameTime()<w.getLong("NextIdentityScan")){w.putString("Task","WAITING_FOR_ORIGINAL_WORKER");return null;}w.putLong("NextIdentityScan",p.server.overworld().getGameTime()+200);
            TycoonWorker original=null;for(var world:p.server.getAllLevels())for(var entity:world.getAllEntities())if(entity instanceof TycoonWorker candidate&&h.owner.equals(candidate.owner())&&w.getUUID("Id").equals(candidate.worker)){if(original!=null){w.putString("Task","AMBIGUOUS_WORKER_IDENTITY");return null;}original=candidate;}
            if(original==null||!original.isAlive()||original.role()!=type){w.putString("Task","WAITING_FOR_ORIGINAL_WORKER");return null;}
            w.putUUID("Actor",original.getUUID());w.putString("ActorDimension",original.level().dimension().location().toString());w.putLong("LastAt",original.blockPosition().asLong());w.putBoolean("ActorCheckpointPending",true);TycoonWorld.dirty(p);
            if(!workerCheckpoint(p,w))return null;w.remove("ActorCheckpointPending");TycoonWorld.dirty(p);if(original.level()!=target){w.putString("Task","WORKER_IN_DIFFERENT_WORLD");return null;}return original;
        }
        com.coraline.feast.LoopProps.hold(target,at);if(!target.hasChunkAt(at)){w.putString("Task","WAITING_FOR_WORKER_ARRIVAL");return null;}
        var safe=TycoonWorld.landing(target,at);if(safe==null){w.putString("Task","CLEAR_WORKER_ARRIVAL");return null;}
        var actor=TycoonAutomationContent.WORKER.get().create(target);if(actor==null)return null;actor.configure(h.owner,w.getUUID("Id"),type,w.getInt("Operating"));actor.moveTo(safe.getX()+.5,safe.getY(),safe.getZ()+.5,0,0);
        if(!target.noCollision(actor,actor.getBoundingBox())){w.putString("Task","CLEAR_WORKER_ARRIVAL");return null;}
        // Admission precedes insertion. An absent/unloaded admitted body is
        // never permission to create another actor with its saved UUID.
        w.putUUID("Actor",actor.getUUID());w.putString("ActorDimension",target.dimension().location().toString());w.putLong("LastAt",safe.asLong());w.putBoolean("ActorCheckpointPending",true);TycoonWorld.dirty(p);
        if(!workerCheckpoint(p,w))return null;w.remove("ActorCheckpointPending");TycoonWorld.dirty(p);
        if(!target.addFreshEntity(actor)){w.putString("Task","WORKER_ADMISSION_PAUSED");return null;}
        return actor;
    }
    private static boolean workerCheckpoint(ServerPlayer player,CompoundTag worker){try{if(TycoonCheckpoints.home(player))return true;}catch(java.io.IOException|RuntimeException failed){/* Preserve the actor admission and real cargo; never recreate a body. */}worker.putString("Task","WORKER_SAVE_PAUSED");return false;}
    public static boolean approach(ServerPlayer p,CompoundTag w,TycoonWorker actor,BlockPos goal){
        if(goal==null||!TycoonWorld.safeFeet((ServerLevel)actor.level(),goal)){w.putString("Blocker","Destination is obstructed or unsupported.");return false;}
        if(actor.distanceToSqr(Vec3.atBottomCenterOf(goal))<2.25){actor.getNavigation().stop();w.remove("RouteGoal");return true;}
        var path=actor.getNavigation().createPath(goal,0);if(path==null||!path.canReach()){w.putString("Blocker","No valid route to this task.");return false;}
        boolean visible=actor.getServer().getPlayerList().getPlayers().stream().anyMatch(viewer->viewer.level()==actor.level()&&viewer.distanceToSqr(actor)<64*64);
        if(visible){actor.getNavigation().moveTo(path,.75+w.getInt("Operating")*.12);return false;}
        if(!w.contains("RouteGoal")||w.getLong("RouteGoal")!=goal.asLong()){w.putLong("RouteGoal",goal.asLong());w.putLong("RouteReadyAt",TycoonWorld.home(p).clock()+Math.max(100,(long)(Math.sqrt(actor.distanceToSqr(goal.getCenter()))/.14)));}
        if(TycoonWorld.home(p).clock()<w.getLong("RouteReadyAt"))return false;
        actor.moveTo(goal.getX()+.5,goal.getY(),goal.getZ()+.5,actor.getYRot(),0);w.remove("RouteGoal");return true;
    }
    public static String taskLabel(CompoundTag worker){return switch(worker.getString("Task")){
        case "WAITING_FOR_WORKER_WORLD"->"Waiting for the work area to load";
        case "AMBIGUOUS_WORKER_IDENTITY"->"Paused: more than one worker matches this assignment";
        case "WAITING_FOR_ORIGINAL_WORKER"->"Waiting for the assigned worker to load";
        case "WORKER_IDENTITY_UNAVAILABLE"->"Paused: the assigned worker is unavailable";
        case "WORKER_IN_DIFFERENT_WORLD"->"Paused: the worker is in another world";
        case "WAITING_FOR_WORKER_ARRIVAL"->"Waiting for the arrival area to load";
        case "CLEAR_WORKER_ARRIVAL"->"Clear a safe standing space at the work area";
        case "WORKER_SAVE_PAUSED"->"Paused until the world can save safely";
        case "WORKER_ADMISSION_PAUSED"->"Paused: the worker could not arrive safely";
        default->worker.getString("Task").replace('_',' ').toLowerCase(Locale.ROOT);
    };}
    public static ListTag projection(TycoonData.Home h){var out=new ListTag();for(String key:workers(h).getAllKeys()){var row=workers(h).getCompound(key).copy();if(!row.hasUUID("Id")||salaryMicros(row)==0)continue;row.remove("Carry");row.putString("TaskLabel",taskLabel(row));row.putInt("Capacity",capacity(row.getInt("Operating")));row.putLong("SalaryPerMinute",salaryMicros(row));out.add(row);}return out;}
    private TycoonWorkers(){}
}
