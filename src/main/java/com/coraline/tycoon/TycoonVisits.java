package com.coraline.tycoon;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import com.coraline.otherworld.MouseEntity;

/** Independent multi-resident visit states, always using the same NPC UUID.
 * Mouse and child movement are real navigation; transfer occurs only at a door.
 */
public final class TycoonVisits {
    private static final Set<String> ACTIVE=Set.of("INVITED","FOLLOWING","ENTERING","INSIDE","LEAVING","RETURNING");
    public static boolean nighttime(ServerPlayer p){long time=Math.floorMod(p.server.overworld().getDayTime(),24000);return time>=12500&&time<23500;}
    public static boolean visiting(TycoonData.Resident r){return ACTIVE.contains(r.n.getCompound("Visit").getString("Phase"));}
    public static boolean inside(TycoonData.Resident r){return Set.of("ENTERING","INSIDE","LEAVING").contains(r.n.getCompound("Visit").getString("Phase"));}
    public static long count(TycoonData.Home h){return h.residents.values().stream().filter(TycoonVisits::inside).count();}
    public static boolean eligible(ServerPlayer p,TycoonData.Resident r){return r.active()&&!visiting(r)&&r.bed()!=null&&TycoonWorld.home(p).active()&&r.need("Neglect")>=50&&r.need("Neglect")<75&&r.need("Hunger")>30&&r.need("Thirst")>30&&TycoonRooms.survey(p).beds().contains(r.bed())&&TycoonDolls.valid(p,r);}
    public static List<TycoonData.Resident> eligible(ServerPlayer p){if(!nighttime(p))return List.of();TycoonVisitTickets.warmHome(p);return TycoonWorld.home(p).residents.values().stream().filter(r->eligible(p,r)).toList();}
    public static String invite(ServerPlayer p){
        return invite(p,Integer.MAX_VALUE,false);
    }
    static String inviteSupplied(ServerPlayer p,int limit){if(TycoonWorkers.contextWorker(p)==null||!TycoonPolicies.allowed(TycoonWorld.home(p),"invitations"))return "Automated invitations are disabled.";return invite(p,Math.max(0,limit),true);}
    private static String invite(ServerPlayer p,int limit,boolean worker){
        var h=TycoonWorld.home(p);if(!worker&&p.serverLevel().dimension()!=TycoonWorld.KEY)return "Use Spawn Mice from your personal Work Desk.";
        if(!nighttime(p))return "The mice only come at night.";
        if(h.state.getBoolean("VisitActive"))return "A nighttime invitation session is already active.";
        var group=eligible(p).stream().limit(limit).toList();if(group.isEmpty())return "No eligible residents: delivered doll, safe bed, Neglect50–74 and safe food/water are required.";
        var session=UUID.randomUUID();h.state.putUUID("VisitSession",session);h.state.putBoolean("VisitActive",true);h.state.putBoolean("SendHome",false);h.state.putLong("VisitStarted",h.clock());
        for(var r:group){var n=new CompoundTag();n.putUUID("Session",session);n.putString("Phase","INVITED");n.putLong("Started",h.clock());n.putLong("HesitateUntil",h.clock()+40+(int)((1-r.trait("Curiosity"))*160));r.n.put("Visit",n);}
        TycoonVisitTickets.home(p);TycoonVisitTickets.world(p,h.center());TycoonWorld.dirty(p);return "Mice are guiding "+group.size()+" eligible residents from their real rooms. Keep the entrance clear.";
    }
    public static String sendHome(ServerPlayer p){var h=TycoonWorld.home(p);h.state.putBoolean("SendHome",true);boolean breach=false;for(var r:h.residents.values())if(visiting(r)){if(r.n.hasUUID("RescuingFarm")){breach=true;continue;}if(r.n.contains("ConversionRitual"))continue;var n=r.n.getCompound("Visit");if(inside(r)){TycoonAttractions.cancel(r);n.putString("Phase","LEAVING");n.putLong("LeavingAt",h.clock());}else if(n.getString("Phase").equals("RETURNING")){}else finish(p,r,"Invitation cancelled safely");}TycoonWorld.dirty(p);return breach?"Ordinary visitors are leaving. Secure active containment breaches or use Concede Breach and Evacuate; a breach cannot be cancelled for free.":"Visitors are returning through the Little Door. Blocked departures have bounded recovery.";}
    public static boolean blockExit(ServerPlayer p){var h=TycoonWorld.home(p);if(count(h)>0){TycoonWorld.message(p,"You must send your visitors home first.");return true;}if(h.state.getBoolean("VisitActive")&&h.residents.values().stream().anyMatch(r->Set.of("INVITED","FOLLOWING").contains(r.n.getCompound("Visit").getString("Phase")))){TycoonWorld.message(p,"The mice are still guiding visitors. Use Send All Visitors Home to cancel the invitation first.");return true;}return false;}

    public static void tick(ServerPlayer p){
        var h=TycoonWorld.home(p);if(!h.state.getBoolean("VisitActive"))return;TycoonVisitTickets.home(p);
        var pending=h.residents.values().stream().filter(r->r.active()&&Set.of("INVITED","FOLLOWING").contains(r.n.getCompound("Visit").getString("Phase"))).toList();
        int guides=0;for(var r:pending){var n=r.n.getCompound("Visit");if(!nighttime(p)||r.need("Neglect")<50||r.need("Neglect")>=75||r.need("Hunger")<=18||r.need("Thirst")<=18||!TycoonDolls.valid(p,r)){finish(p,r,"Invitation no longer eligible");continue;}
            if(guides++>=3)continue;
            if(n.hasUUID("Mouse")){if(n.contains("MouseAt"))p.server.overworld().getChunkAt(BlockPos.of(n.getLong("MouseAt")));if(p.server.overworld().getEntity(n.getUUID("Mouse"))!=null)continue;n.putInt("MouseMissing",n.getInt("MouseMissing")+20);if(n.getInt("MouseMissing")<100)continue;n.remove("Mouse");}
            if(h.entrance()==null){finish(p,r,"Entrance missing; invitation paused");continue;}var at=TycoonWorld.landing(p.server.overworld(),h.entrance());if(at==null)continue;
            var mouse=com.coraline.registry.ModEntities.MOUSE.get().create(p.server.overworld());if(mouse==null)continue;var tag=mouse.getPersistentData();tag.putUUID("TycoonOwner",TycoonWorld.owner(p));tag.putUUID("TycoonResident",r.id);tag.putUUID("TycoonSession",h.state.getUUID("VisitSession"));mouse.spying(true);mouse.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);if(p.server.overworld().addFreshEntity(mouse)){n.putUUID("Mouse",mouse.getUUID());n.putLong("MouseAt",at.asLong());n.remove("MouseMissing");}
        }
        for(var r:h.residents.values())if(visiting(r)&&r.n.hasUUID("Entity")){
            var n=r.n.getCompound("Visit");boolean inWorld=n.getString("LastDimension").equals(TycoonWorld.KEY.location().toString());var level=inWorld?p.server.getLevel(TycoonWorld.KEY):p.server.overworld();if(level==null)continue;var at=n.contains("LastAt")?BlockPos.of(n.getLong("LastAt")):inWorld?h.center():r.bed();if(at!=null){level.getChunkAt(at);if(inWorld)TycoonVisitTickets.world(p,at);}
            if(h.clock()-n.getLong("Started")>3600&&!inside(r)&&!n.getString("Phase").equals("RETURNING"))finish(p,r,"Mice could not reach the room; invitation ended without duplication");
            if(inside(r)&&h.clock()-n.getLong("LastSeen")>400&&level.getEntity(r.n.getUUID("Entity"))==null){n.putString("Phase","LEAVING");n.putLong("LeavingAt",Math.max(0,h.clock()-1200));recoverMissing(p,r);}
        }
        if(h.residents.values().stream().noneMatch(TycoonVisits::visiting)){h.state.putBoolean("VisitActive",false);h.state.putBoolean("SendHome",false);h.state.putLong("LastVisitCompleted",h.clock());TycoonVisitTickets.release(p.server,h);TycoonWorld.message(p,"All visitors are home. Your Overworld return is unlocked.");}
        TycoonWorld.dirty(p);
    }

    /** Called before the original story DoorData mouse controller. */
    public static boolean mouseTick(MouseEntity mouse){
        if(TycoonSpies.mouseTick(mouse))return true;
        var tag=mouse.getPersistentData();if(!tag.hasUUID("TycoonOwner"))return false;
        var p=TycoonOfflineCollaboration.representative(mouse.getServer(),tag.getUUID("TycoonOwner"));if(p==null){mouse.getNavigation().stop();return true;}
        return TycoonScope.call(p,tag.getUUID("TycoonOwner"),()->mouseOwned(p,mouse),true);
    }
    private static boolean mouseOwned(ServerPlayer p,MouseEntity mouse){
        var tag=mouse.getPersistentData();if(TycoonWorld.busy(p)){mouse.getNavigation().stop();return true;}
        var h=TycoonWorld.home(p);var r=tag.hasUUID("TycoonResident")?h.residents.get(tag.getUUID("TycoonResident")):null;
        if(r==null||!r.active()||!visiting(r)||inside(r)||!h.state.getBoolean("VisitActive")||!tag.hasUUID("TycoonSession")||!tag.getUUID("TycoonSession").equals(h.state.getUUID("VisitSession"))){mouse.discard();return true;}
        var n=r.n.getCompound("Visit");n.putLong("MouseAt",mouse.blockPosition().asLong());if(!r.n.hasUUID("Entity")){finish(p,r,"Resident identity unavailable");mouse.discard();return true;}
        if(!n.hasUUID("Mouse")||!n.getUUID("Mouse").equals(mouse.getUUID())){mouse.discard();return true;}
        if(r.bed()!=null)p.server.overworld().getChunkAt(r.bed());var entity=p.server.overworld().getEntity(r.n.getUUID("Entity"));if(!(entity instanceof TycoonNpc child)){mouse.getNavigation().stop();return true;}
        if(h.clock()<n.getLong("HesitateUntil"))return true;
        if(n.getString("Phase").equals("INVITED")){mouse.getNavigation().moveTo(child,1);if(mouse.distanceToSqr(child)<4){n.putString("Phase","FOLLOWING");child.gesture(2,40);TycoonWorld.message(p,r.name()+" noticed the little mice.");}}
        else if(n.getString("Phase").equals("FOLLOWING")){var target=h.entrance()==null?null:TycoonWorld.landing(p.server.overworld(),h.entrance());if(target==null)return true;
            // Mouse leads, but waits rather than racing out of the child's sight.
            if(mouse.distanceToSqr(child)>16)mouse.getNavigation().stop();else{var path=mouse.getNavigation().createPath(target,0);if(path!=null)mouse.getNavigation().moveTo(path,.7);}
            child.getNavigation().moveTo(mouse,.85);child.getLookControl().setLookAt(mouse,30,25);
            if(child.distanceToSqr(Vec3.atBottomCenterOf(target))<2.5){n.putString("Phase","ENTERING");n.putLong("TransitionAt",h.clock());mouse.getNavigation().stop();child.getNavigation().stop();}
        }
        TycoonWorld.dirty(p);return true;
    }

    /** True means the visit owns this NPC's task for the current care tick. */
    public static boolean actorTick(ServerPlayer p,TycoonData.Resident r,TycoonNpc actor){
        if(!visiting(r))return false;var h=TycoonWorld.home(p);var n=r.n.getCompound("Visit");n.putLong("LastSeen",h.clock());n.putLong("LastAt",actor.blockPosition().asLong());n.putString("LastDimension",actor.level().dimension().location().toString());
        if(!r.active()){if(actor.level()==p.server.overworld()){finish(p,r,"Returned for recorded welfare outcome");return false;}n.putString("Phase","LEAVING");n.putLong("LeavingAt",h.clock()-1200);}
        if(n.getString("Phase").equals("INSIDE")&&TycoonSoulFarms.actorTick(p,r,actor)){TycoonWorld.dirty(p);return true;}
        switch(n.getString("Phase")){
            case "INVITED","FOLLOWING"->{r.n.putString("Task","FOLLOWING_MICE");}
            case "ENTERING"->{if(actor.level()==p.server.overworld()){actor.getNavigation().stop();actor.gesture(2,25);if(h.clock()-n.getLong("TransitionAt")>=40)enter(p,r,actor);}else n.putString("Phase","INSIDE");}
            case "INSIDE"->{if(actor.level().dimension()!=TycoonWorld.KEY){finish(p,r,"Recovered completed crossing");return true;}r.n.putString("Task","EXPLORING_OTHER_WORLD");TycoonDialogue.passive(p,r,actor);if(h.state.getBoolean("SendHome")||h.clock()>=n.getLong("LeaveAfter")||r.need("Neglect")>=TycoonConfig.SEVERE.get()||r.need("Thirst")<18||r.need("Hunger")<18||TycoonDialogue.suspicion(r)>.92){TycoonAttractions.cancel(r);n.putString("Phase","LEAVING");n.putLong("LeavingAt",h.clock());TycoonWorld.message(p,r.name()+": I think I should go home now.");}else if(!TycoonMeals.otherWorld(p,r,actor))TycoonAttractions.actorTick(p,r,actor);}
            case "LEAVING"->{if(actor.level()==p.server.overworld()){n.putString("Phase","RETURNING");break;}r.n.putString("Task","RETURNING_TO_LITTLE_DOOR");var target=TycoonWorld.landing((ServerLevel)actor.level(),h.center().offset(-1,0,-1));boolean reached=TycoonCare.move(actor,target,.85);if(reached||h.clock()-n.getLong("LeavingAt")>1200)home(p,r,actor,!reached);}
            case "RETURNING"->{if(actor.level()!=p.server.overworld()){home(p,r,actor,true);break;}var approach=TycoonRooms.approach(TycoonRooms.survey(p).reachable(),r.bed());if(TycoonCare.move(actor,approach,.8)||h.clock()-n.getLong("ReturningAt")>1200)finish(p,r,"Returned home");}
            default->{}
        }
        TycoonWorld.dirty(p);return true;
    }

    private static void enter(ServerPlayer p,TycoonData.Resident r,TycoonNpc actor){
        var h=TycoonWorld.home(p);var l=p.server.getLevel(TycoonWorld.KEY);if(l==null||!TycoonWorld.prepare(p))return;var at=arrival(p,l,h);if(at==null){TycoonWorld.message(p,"Visitor arrival is obstructed. Clear the platform or Send All Visitors Home.");return;}
        var n=r.n.getCompound("Visit");n.putLong("TransitionAt",h.clock());n.putLong("LastAt",at.asLong());n.putString("LastDimension",TycoonWorld.KEY.location().toString());r.n.put("VisitActorSnapshot",actor.saveWithoutId(new CompoundTag()));TycoonVisitTickets.world(p,at);
        var moved=actor.changeDimension(l,new net.minecraftforge.common.util.ITeleporter(){@Override public Entity placeEntity(Entity e,ServerLevel from,ServerLevel to,float yaw,java.util.function.Function<Boolean,Entity> reposition){var placed=reposition.apply(false);if(placed!=null)placed.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,yaw,0);return placed;}});
        if(!(moved instanceof TycoonNpc child))return;
        TycoonDialogue.initialize(r);
        n.putString("Phase","INSIDE");n.putLong("EnteredAt",h.clock());n.putLong("LastSeen",h.clock());int min=TycoonPhaseTwoConfig.MIN_VISIT.get(),max=Math.max(min,TycoonPhaseTwoConfig.MAX_VISIT.get());double factor=.3+r.trait("Curiosity")*.35+r.trait("Activity")*.15+TycoonLandscape.goodness(h).total()*.002-TycoonDialogue.suspicion(r)*.15;long duration=min+(long)((max-min)*Math.max(0,Math.min(1,factor)));n.putLong("LeaveAfter",h.clock()+duration);r.n.putInt("VisitCount",r.n.getInt("VisitCount")+1);TycoonFoodShop.firstEntry(p,r);child.gesture(2,60);TycoonDialogue.arrival(p,r);TycoonWorld.dirty(p);
    }
    private static BlockPos arrival(ServerPlayer p,ServerLevel l,TycoonData.Home h){var c=h.center();for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){var at=c.offset(x,0,z);if(TycoonWorld.safeFeet(l,at)&&l.getEntitiesOfClass(TycoonNpc.class,new net.minecraft.world.phys.AABB(at).inflate(.4)).isEmpty())return at;}return null;}
    private static void home(ServerPlayer p,TycoonData.Resident r,TycoonNpc actor,boolean recovery){
        var h=TycoonWorld.home(p);var l=p.server.overworld();var at=r.bed()==null?null:TycoonWorld.landing(l,r.bed());if(at==null&&h.entrance()!=null)at=TycoonWorld.landing(l,h.entrance());if(at==null)at=TycoonWorld.emergencyReturn(l);if(at==null)return;final var destination=at;
        var moved=actor.level()==l?actor:actor.changeDimension(l,new net.minecraftforge.common.util.ITeleporter(){@Override public Entity placeEntity(Entity e,ServerLevel from,ServerLevel to,float yaw,java.util.function.Function<Boolean,Entity> reposition){var placed=reposition.apply(false);if(placed!=null)placed.moveTo(destination.getX()+.5,destination.getY(),destination.getZ()+.5,yaw,0);return placed;}});
        if(moved instanceof TycoonNpc){var n=r.n.getCompound("Visit");n.putString("Phase","RETURNING");n.putLong("ReturningAt",h.clock());n.putLong("LastAt",at.asLong());n.putString("LastDimension",net.minecraft.world.level.Level.OVERWORLD.location().toString());if(recovery)n.putString("Recovery","Blocked departure used bounded same-identity portal recovery");TycoonWorld.dirty(p);}
    }
    private static void explore(ServerPlayer p,TycoonData.Resident r,TycoonNpc actor){
        var h=TycoonWorld.home(p);var l=(ServerLevel)actor.level();if(actor.getY()<h.center().getY()-8){home(p,r,actor,true);return;}
        if(actor.getNavigation().isDone()&&h.clock()>=r.n.getCompound("Visit").getLong("NextExplore")){
            r.n.getCompound("Visit").putLong("NextExplore",h.clock()+80);var random=actor.getRandom();for(int tries=0;tries<12;tries++){var at=actor.blockPosition().offset(random.nextInt(13)-6,0,random.nextInt(13)-6);int y=TycoonLandscape.height(h,at.getX()-h.center().getX(),at.getZ()-h.center().getZ())+1;at=new BlockPos(at.getX(),y,at.getZ());if(!TycoonLandscape.owned(h,at)||!TycoonWorld.safeFeet(l,at)||!TycoonLandscape.owned(h,at.north())||!TycoonLandscape.owned(h,at.south())||!TycoonLandscape.owned(h,at.east())||!TycoonLandscape.owned(h,at.west()))continue;var path=actor.getNavigation().createPath(at,0);if(path!=null&&path.canReach()){actor.getNavigation().moveTo(path,.65+r.trait("Activity")*.25);break;}}
        }
    }
    private static void recoverMissing(ServerPlayer p,TycoonData.Resident r){
        var n=r.n.getCompound("Visit");if(!r.n.hasUUID("Entity"))return;
        var h=TycoonWorld.home(p);
        for(var level:p.server.getAllLevels())if(level.getEntity(r.n.getUUID("Entity")) instanceof TycoonNpc actor){
            if(h.owner.equals(actor.owner)&&r.id.equals(actor.resident)&&actor.isAlive())home(p,r,actor,true);return;
        }
        // A stale snapshot is not evidence that the original body died. Hold
        // its recorded native chunk and wait instead of constructing a copy.
        if(n.contains("LastAt")){var level=com.coraline.parents.FamilyData.level(p.server,n.getString("LastDimension"));if(level!=null){var last=BlockPos.of(n.getLong("LastAt"));level.getChunkAt(last);if(level.dimension()==TycoonWorld.KEY)TycoonVisitTickets.world(p,last);}}
        if(r.bed()!=null)p.server.overworld().getChunkAt(r.bed());
        n.putString("Recovery","Waiting for the recorded original visitor body; no snapshot substitute created.");TycoonWorld.dirty(p);
    }
    public static String debugEnter(ServerPlayer p,TycoonData.Resident r){if(!p.hasPermissions(2)||!r.n.getBoolean("DebugChild")||!r.n.hasUUID("Entity")||!(p.server.overworld().getEntity(r.n.getUUID("Entity")) instanceof TycoonNpc actor))return "A loaded owned test resident at home is required.";var h=TycoonWorld.home(p);if(!h.state.hasUUID("VisitSession"))h.state.putUUID("VisitSession",UUID.randomUUID());h.state.putBoolean("VisitActive",true);var n=new CompoundTag();n.putUUID("Session",h.state.getUUID("VisitSession"));n.putString("Phase","ENTERING");n.putLong("Started",h.clock());r.n.put("Visit",n);TycoonVisitTickets.home(p);enter(p,r,actor);return "Declared test crossing attempted with the existing canonical NPC identity.";}
    public static void finish(ServerPlayer p,TycoonData.Resident r,String reason){TycoonFoodShop.completed(p,r);var n=r.n.getCompound("Visit");if(n.hasUUID("Mouse")){var e=p.server.overworld().getEntity(n.getUUID("Mouse"));if(e instanceof MouseEntity)e.discard();}n.putString("Phase","HOME");n.putLong("CompletedAt",TycoonWorld.home(p).clock());n.putString("Completion",reason);r.n.putBoolean("FirstVisitComplete",r.n.getInt("VisitCount")>0);r.n.putString("Task","SETTLING_IN");TycoonWorld.dirty(p);}
    private TycoonVisits(){}
}
