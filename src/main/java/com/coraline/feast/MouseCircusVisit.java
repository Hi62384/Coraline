package com.coraline.feast;

import java.util.*;
import com.coraline.otherworld.*;
import com.coraline.secrets.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import static com.coraline.feast.OtherMotherLoopState.Phase.*;

/** One-time cinematic circus, independent of the existing MiceCircusActivity wager/race. */
public final class MouseCircusVisit {
    public static CompoundTag data(VisitData.Scene s){if(!s.loop.event.contains("MouseCircus"))s.loop.event.put("MouseCircus",new CompoundTag());return s.loop.event.getCompound("MouseCircus");}
    public static boolean pending(VisitData.Scene s){return s.loop.event.getBoolean("MouseCircusPending");}
    public static boolean protectedRoute(ServerPlayer p){var s=OtherMotherLoop.active(p);return s!=null&&owned(p,s)&&data(s).getBoolean("InvitationDone")&&s.loop.phase==CIRCUS_WAIT&&(MouseCircusSite.inAttic(p,s)||ShowHouseAccess.onRoute(p,s,p.position(),true));}
    public static boolean eligible(ServerPlayer p,VisitData.Scene s){return s.owner.equals(p.getUUID())&&s.loop.story.getBoolean("BobinskyIntroductionComplete")&&!s.loop.story.getBoolean("MouseCircusCompleted")&&!pending(s)&&s.loop.suspicion<1&&s.loop.secrets.search!=SecretsState.Search.SCRIPTED_TRAP&&NumberedEventDialogue.ready("mouse_circus")&&ShowRecordings.blocker(false).isEmpty();}
    public static boolean prepare(ServerPlayer p,VisitData.Scene s){if(!eligible(p,s))return false;var n=new CompoundTag();n.putUUID("Owner",s.owner);n.putInt("Serial",s.loop.event.getInt("Serial"));n.putString("Phase","INVITATION");n.put("Wybie",new CompoundTag());s.loop.event.put("MouseCircus",n);if(!MouseCircusSite.prepareAttic(p,s)||!MouseCircusSite.prepare(p,s)){MouseCircusSite.release(p,s);return false;}s.loop.event.putBoolean("MouseCircusPending",true);return true;}
    /** Called instead of generated event-intro dialogue. Original invitation plays in full once. */
    public static boolean invitation(ServerPlayer p,VisitData.Scene s){if(!owned(p,s))return false;var n=data(s);if(n.getBoolean("InvitationDone"))return true;if(!n.getBoolean("InvitationStarted")){var mother=MotherSearch.mother(p);if(mother==null||mother.distanceToSqr(p)>100||!mother.hasLineOfSight(p))return false;if(!ShowPlayback.start(p,s,"mouse_circus_invite",false))return false;n.putBoolean("InvitationStarted",true);}if(!ShowPlayback.tick(p,s))return false;n.putBoolean("InvitationDone",true);n.putString("Checkpoint","INVITATION_COMPLETE");beginWindow(p,s);return true;}
    public static void beginWindow(ServerPlayer p,VisitData.Scene s){if(!owned(p,s))return;var n=data(s);if(!n.getBoolean("InvitationDone"))return;ShowAttendance.begin(p,s);n.putString("Phase","TRAVEL");OtherMotherLoop.phase(p,s,CIRCUS_WAIT);arrivalMusic(p,s,true);if(WybieOptionalVisits.allowed(s)){var landing=MouseCircusSite.meeting(p,s);if(landing!=null){p.serverLevel().getChunkAt(landing);WybieOptionalVisits.acquire(p,s,n.getCompound("Wybie"),landing);}}else ShowHouseAccess.open(p,s,"attic");OtherMotherLoop.changed(p);}
    private static void arrivalMusic(ServerPlayer p,VisitData.Scene s,boolean play){var n=data(s);if(play==n.getBoolean("ArrivalMusic"))return;n.putBoolean("ArrivalMusic",play);if(play)VisitAudio.play(p,"bobinsky_visit_music",true);else VisitAudio.stopMusic(p,"bobinsky_visit_music");}
    public static boolean owned(ServerPlayer p,VisitData.Scene s){var n=data(s);return pending(s)&&s.owner.equals(p.getUUID())&&n.hasUUID("Owner")&&s.owner.equals(n.getUUID("Owner"))&&n.getInt("Serial")==s.loop.event.getInt("Serial");}
    public static boolean tick(ServerPlayer p,VisitData.Scene s){
        if(!owned(p,s)||!p.isAlive()||p.isSleeping())return true;var n=data(s);if(n.getBoolean("DebugPaused")){if(ShowPlayback.debugSeeking(s))debugSeekFrame(p,s);else if(n.getBoolean("DebugAudio"))ShowPlayback.tick(p,s);if(n.getBoolean("DebugApproach")&&MouseCircusSite.inside(p,s))bobinsky(p,s,1);return true;}if(n.getBoolean("NeedsRecovery"))return true;n.putLong("Clock",n.getLong("Clock")+1);int cooldown=s.loop.event.getInt("ShowPortalCooldown");if(cooldown>0)s.loop.event.putInt("ShowPortalCooldown",cooldown-1);
        if(s.loop.suspicion>=1||s.loop.secrets.search==SecretsState.Search.SCRIPTED_TRAP){suspend(p,s);return true;}
        if(Reconciliation.active(p)||s.loop.secrets.searching()||SecretDialogue.busy(p))return true;
        if(s.loop.phase==EVENT_INTRO){invitation(p,s);return true;}
        if(s.loop.phase==CIRCUS_WAIT){
            boolean attended=ShowAttendance.tick(p,s,(!WybieOptionalVisits.allowed(s)||n.getBoolean("WybieMet"))&&MouseCircusSite.inAttic(p,s),"mouse_circus","circus_search");
            arrivalMusic(p,s,s.loop.event.getBoolean("ShowAttended")||s.loop.event.getInt("ArrivalRemaining")>0);
            if(WybieOptionalVisits.allowed(s)&&!n.getCompound("Wybie").hasUUID("Actor")&&p.tickCount%40==0){var landing=MouseCircusSite.meeting(p,s);if(landing!=null){p.serverLevel().getChunkAt(landing);WybieOptionalVisits.acquire(p,s,n.getCompound("Wybie"),landing);}}
            var companion=WybieOptionalVisits.actor(p,n.getCompound("Wybie"));if(WybieOptionalVisits.allowed(s)&&!n.getBoolean("WybieMet")){var porch=MouseCircusSite.meeting(p,s);WybieOptionalVisits.stageWaiting(p,s,n.getCompound("Wybie"),porch);if(companion==null||companion.level()!=p.level()||porch==null||companion.distanceToSqr(p)>36||companion.distanceToSqr(porch.getCenter())>16||p.distanceToSqr(porch.getCenter())>49){if(p.tickCount%40==0)p.displayClientMessage(Component.literal("Meet Other Wybie on the front porch before going to the attic."),true);return true;}n.putBoolean("WybieMet",true);companion.gameGesture(1,50);ShowHouseAccess.open(p,s,"attic");OtherMotherLoop.changed(p);}
            WybieOptionalVisits.follow(p,s,n.getCompound("Wybie"));var wybie=WybieOptionalVisits.actor(p,n.getCompound("Wybie"));if(s.loop.event.getInt("ArrivalRemaining")<=1200&&wybie!=null&&p.tickCount%160==0)PartyGuests.gesture(wybie,2,60);
            if(!attended)return true;
            n.putString("Phase","ATTIC");n.putString("Checkpoint","ATTIC_REACHED");MouseCircusSite.prepareAttic(p,s);animateAttic(p,s);var tent=MouseCircusSite.tent(p,s);if(tent!=null&&p.distanceToSqr(tent.getCenter())<9){enter(p,s);return true;}if(p.tickCount%30==0)p.displayClientMessage(Component.literal("Approach the tent entrance to enter the Mouse Circus"),true);return true;
        }
        if(s.loop.phase==CIRCUS_ACTIVE){
            if(!MouseCircusSite.inside(p,s)){recover(p,s);return true;}
            if(n.getString("Phase").equals("EXIT_READY")){if(p.tickCount%30==0)p.displayClientMessage(Component.literal("Leave the Mouse Circus"),true);if(p.distanceToSqr(Vec3.atBottomCenterOf(MouseCircusSite.exit(p,s)))<9&&cooldown==0){if(n.getBoolean("DebugExitOnly")){n.remove("DebugExitOnly");leaveTemporarily(p,s);}else returnToAttic(p,s);}}
            else {if(!n.getString("Blocker").isEmpty()&&p.distanceToSqr(Vec3.atBottomCenterOf(MouseCircusSite.exit(p,s)))<9&&cooldown==0){leaveTemporarily(p,s);return true;}performance(p,s);}
        }
        if(s.loop.phase==CIRCUS_RETURN)departure(p,s);if(p.tickCount%20==0)OtherMotherLoop.changed(p);return true;
    }
    private static void animateAttic(ServerPlayer p,VisitData.Scene s){
        var at=MouseCircusSite.tent(p,s);if(at==null||!MouseCircusSite.inAttic(p,s))return;int clock=(int)data(s).getLong("Clock");
        for(var prop:p.serverLevel().getEntitiesOfClass(CinematicPropEntity.class,new AABB(at).inflate(8),e->e.owned(s))){
            prop.frame(0,clock,0);
            if(prop.kind()==CinematicPropEntity.CANNON&&Math.floorMod(clock,100)==0){
                p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,prop.getX(),prop.getY()+.85,prop.getZ(),5,.1,.1,.1,.015);
                p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,prop.getX()+.5,prop.getY()+1.4,prop.getZ(),4,.25,.35,.2,.01);
                p.serverLevel().playSound(null,prop.blockPosition(),net.minecraft.sounds.SoundEvents.SNOWBALL_THROW,net.minecraft.sounds.SoundSource.BLOCKS,.22F,.75F);
            }
        }
    }
    public static boolean interact(ServerPlayer p,VisitData.Scene s,CinematicPropEntity prop){
        if(!owned(p,s)||!prop.owned(s))return false;var n=data(s);
        if(prop.kind()==CinematicPropEntity.SEAT&&prop.slot()==0&&s.loop.phase==CIRCUS_ACTIVE&&MouseCircusSite.inside(p,s)){if(!prop.isVehicle())p.startRiding(prop,true);return true;}
        if(prop.kind()==CinematicPropEntity.TENT){if(prop.slot()==99){if(n.getString("Phase").equals("EXIT_READY"))returnToAttic(p,s);else if(!n.getString("Blocker").isEmpty())leaveTemporarily(p,s);return true;}if(s.loop.phase==CIRCUS_WAIT&&n.getString("Phase").equals("ATTIC")){enter(p,s);return true;}}
        if((prop.kind()==CinematicPropEntity.CANNON||prop.kind()==CinematicPropEntity.FERRIS)&&MouseCircusSite.inAttic(p,s)){if(n.getInt("Snacks")>=2)return true;var snack=new ItemStack(prop.kind()==CinematicPropEntity.FERRIS?CircusSouvenirs.POPCORN.get():CircusSouvenirs.COTTON_CANDY.get());snack.setHoverName(Component.literal(prop.kind()==CinematicPropEntity.FERRIS?"Circus Popcorn":"Cotton Candy"));if(p.getInventory().add(snack)){n.putInt("Snacks",n.getInt("Snacks")+1);p.getInventory().setChanged();OtherMotherLoop.changed(p);}return true;}return false;
    }
    private static void enter(ServerPlayer p,VisitData.Scene s){if(!s.loop.event.getBoolean("ShowAttended")||s.loop.event.getInt("ShowPortalCooldown")>0)return;var n=data(s);var wybie=WybieOptionalVisits.actor(p,n.getCompound("Wybie"));if(WybieOptionalVisits.allowed(s)&&(!n.getBoolean("WybieMet")||wybie==null||wybie.distanceToSqr(p)>144)){if(p.tickCount%30==0)p.displayClientMessage(Component.literal("Wait for Other Wybie to join you at the tent."),true);return;}if(!MouseCircusSite.enter(p,s))return;if(wybie!=null){var moved=MouseCircusSite.enterGuest(wybie,p,s);if(moved!=null){n.getCompound("Wybie").putUUID("Actor",moved.getUUID());var seat=TheatreSite.ensure(p,s,p.serverLevel(),CinematicPropEntity.SEAT,MouseCircusSite.audience(p,s).west(2),1);if(seat!=null)moved.startRiding(seat,true);}}
        arrivalMusic(p,s,false);if(wybie!=null){wybie.setYRot(180);wybie.setYBodyRot(180);wybie.setYHeadRot(180);}n.putString("Phase","SHOW");n.putString("Checkpoint","CIRCUS_ENTERED");n.putInt("Segment",Math.max(0,n.getInt("Segment")));n.putBoolean("SegmentStarted",false);OtherMotherLoop.phase(p,s,CIRCUS_ACTIVE);performance(p,s);OtherMotherLoop.changed(p);
    }
    private static void performance(ServerPlayer p,VisitData.Scene s){
        var n=data(s);ScriptedShowTimeline plan;try{plan=ScriptedShowTimeline.circus(ShowRecordings.measured());}catch(IllegalArgumentException bad){n.putString("Blocker",bad.getMessage());return;}int index=n.getInt("Segment");if(index>=plan.segments().size()){n.putString("Phase","EXIT_READY");return;}var segment=plan.segments().get(index);var c=MouseCircusSite.center(p,s);
        if(!n.getBoolean("SegmentStarted")){if(index==2&&!bobinsky(p,s,0))return;if(!ShowPlayback.start(p,s,segment.sound(),segment.music())){n.putString("Blocker",ShowPlayback.data(s).getString("Blocker"));return;}n.putBoolean("SegmentStarted",true);}
        boolean ended=ShowPlayback.tick(p,s);int at=ShowPlayback.position(s);if(!ShowPlayback.data(s).getString("Blocker").isEmpty()){n.putString("Blocker",ShowPlayback.data(s).getString("Blocker"));return;}if(!ShowPlayback.started(s))return;n.putString("Cue",plan.cue(segment.start()+at));
        if(index==0&&at>=segment.duration()-60){if(!n.getBoolean("Revealed")){MouseCircusChoreography.reveal(p,s,c);n.putBoolean("Revealed",true);react(p,s,7);}MouseCircusChoreography.introduction(p,s,c,at-segment.duration()+60);}
        if(index==1){if(!n.getBoolean("Revealed")){MouseCircusChoreography.reveal(p,s,c);n.putBoolean("Revealed",true);}MouseCircusChoreography.frame(p,s,c,at,segment.duration());int act=Math.min(4,(int)(at*5L/segment.duration()));if(n.getInt("Act")!=act){n.putInt("Act",act);react(p,s,act==3?4:7);}}
        if(index==2&&!bobinsky(p,s,at)){ShowPlayback.stop(p,s);n.putBoolean("SegmentStarted",false);OtherMotherLoop.changed(p);return;}
        if(ended){if(index==1){MouseCircusChoreography.frame(p,s,c,segment.duration(),segment.duration());n.putBoolean("MusicDone",true);n.putString("Checkpoint","MAIN_SHOW_COMPLETE");react(p,s,1);}n.putInt("Segment",index+1);n.putBoolean("SegmentStarted",false);if(index==2){n.putString("Phase","EXIT_READY");n.putString("Checkpoint","EXIT_READY");n.putBoolean("PerformanceDone",true);}else performance(p,s);OtherMotherLoop.changed(p);}
    }
    private static boolean bobinsky(ServerPlayer p,VisitData.Scene s,int ticks){var at=MouseCircusSite.audience(p,s).north(7);var b=ShowActors.actor(p,s,"other_bobinsky");if(b==null)b=ShowActors.acquire(p,s,"other_bobinsky",p.serverLevel(),Vec3.atBottomCenterOf(at));if(b==null){data(s).putString("Blocker","Waiting for the original Other Bobinsky actor.");return false;}if(data(s).getString("Blocker").equals("Waiting for the original Other Bobinsky actor."))data(s).remove("Blocker");if(ticks==0||b.blockPosition().equals(MouseCircusSite.audience(p,s).north(4))){ShowActors.position(b,Vec3.atBottomCenterOf(at),0,8,ticks,1);if(ticks==0)p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,at.getX()+.5,at.getY()+1,at.getZ()+.5,16,.8,1,.8,.03);}b.getNavigation().stop();b.getLookControl().setLookAt(p,25,20);ShowPlaybackNetwork.pose(b,8,ticks,1);return true;}
    private static void react(ServerPlayer p,VisitData.Scene s,int gesture){var w=WybieOptionalVisits.actor(p,data(s).getCompound("Wybie"));if(w!=null)PartyGuests.gesture(w,gesture,90);}
    private static void leaveTemporarily(ServerPlayer p,VisitData.Scene s){var n=data(s);ShowPlayback.stop(p,s);n.putBoolean("SegmentStarted",false);var w=WybieOptionalVisits.actor(p,n.getCompound("Wybie"));if(w!=null){var moved=MouseCircusSite.exitGuest(w,p,s);if(moved==null){n.putString("Blocker","Companion return is blocked; preserving the venue.");return;}n.getCompound("Wybie").putUUID("Actor",moved.getUUID());}if(MouseCircusSite.returnToAttic(p,s)){n.putString("Phase","ATTIC");OtherMotherLoop.phase(p,s,CIRCUS_WAIT);OtherMotherLoop.changed(p);}}
    private static void returnToAttic(ServerPlayer p,VisitData.Scene s){var n=data(s);if(!n.getBoolean("PerformanceDone"))return;var wybie=WybieOptionalVisits.actor(p,n.getCompound("Wybie"));if(wybie!=null){var moved=MouseCircusSite.exitGuest(wybie,p,s);if(moved==null){n.putString("Blocker","Waiting for companion return transition.");return;}n.getCompound("Wybie").putUUID("Actor",moved.getUUID());}if(!MouseCircusSite.returnToAttic(p,s))return;n.putString("Phase","DEPARTING");n.putString("Checkpoint","RETURNED_TO_ATTIC");n.putBoolean("Returned",true);ShowPlayback.stop(p,s);MouseCircusChoreography.cleanup(p,s,MouseCircusSite.center(p,s));ShowActors.restore(p,s);OtherMotherLoop.phase(p,s,CIRCUS_RETURN);react(p,s,1);OtherMotherLoop.changed(p);}
    private static void departure(ServerPlayer p,VisitData.Scene s){var n=data(s);ShowHouseAccess.open(p,s,"attic");if(MouseCircusSite.inAttic(p,s)){if(p.tickCount%40==0)p.displayClientMessage(Component.literal("Leave the attic when you are ready."),true);WybieOptionalVisits.follow(p,s,n.getCompound("Wybie"));return;}var w=WybieOptionalVisits.actor(p,n.getCompound("Wybie"));if(n.getCompound("Wybie").hasUUID("Actor")&&w==null){n.putString("Blocker","Companion is unloaded; preserving the return lease.");return;}if(w!=null){var door=MouseCircusSite.atticDoor(p,s);if(door!=null)TheatreVisit.navigate(w,door.west(3),p,false);if(!TheatreVisit.offscreen(p,w))return;WybieOptionalVisits.release(p,n.getCompound("Wybie"));}if(!n.getBoolean("PerformanceDone")||!n.getBoolean("Returned") )return;if(!cleanup(p,s))return;s.loop.story.putBoolean("MouseCircusCompleted",true);OtherMotherLoop.changed(p);VisitEvents.next(p,s,false);}
    public static void suspend(ServerPlayer p,VisitData.Scene s){if(!owned(p,s))return;arrivalMusic(p,s,false);ShowPlayback.stop(p,s);data(s).putBoolean("SegmentStarted",false);data(s).putBoolean("NeedsRecovery",true);ShowStageLease.release(p,s,true);OtherMotherLoop.changed(p);}
    public static void recover(ServerPlayer p,VisitData.Scene s){if(!owned(p,s))return;var n=data(s);n.remove("DebugPaused");ShowPlayback.stop(p,s);n.putBoolean("SegmentStarted",false);n.putBoolean("NeedsRecovery",false);if(n.getBoolean("Returned")){ShowStageLease.hold(p,s,true);OtherMotherLoop.phase(p,s,CIRCUS_RETURN);return;}if(MouseCircusSite.inside(p,s)){MouseCircusSite.prepare(p,s);OtherMotherLoop.phase(p,s,CIRCUS_ACTIVE);if(n.getBoolean("PerformanceDone"))n.putString("Phase","EXIT_READY");else{n.putString("Phase","SHOW");if(n.getInt("Segment")==0){n.putBoolean("Revealed",false);MouseCircusChoreography.cleanup(p,s,MouseCircusSite.center(p,s));}var at=MouseCircusSite.audience(p,s);p.stopRiding();p.teleportTo(p.serverLevel(),at.getX()+.5,at.getY(),at.getZ()+.5,p.getYRot(),p.getXRot());}}else if(!n.getBoolean("InvitationDone")){n.putBoolean("InvitationStarted",false);OtherMotherLoop.phase(p,s,EVENT_INTRO);}else OtherMotherLoop.phase(p,s,CIRCUS_WAIT);}
    public static boolean resume(ServerPlayer p){var s=OtherMotherLoop.scene(p);if(s==null||!pending(s))return false;ShowAttendance.reconciled(s);recover(p,s);return true;}
    public static boolean cleanup(ServerPlayer p,VisitData.Scene s){if(!pending(s))return s.owner.equals(p.getUUID());if(!owned(p,s))return false;ShowStageLease.hold(p,s,true);arrivalMusic(p,s,false);ShowPlayback.stop(p,s);var n=data(s);var lease=n.getCompound("Wybie");var w=WybieOptionalVisits.actor(p,lease);if(lease.hasUUID("Actor")&&w==null){n.putString("Blocker","Companion is unloaded; preserving the return lease.");return false;}if(w!=null){var moved=MouseCircusSite.exitGuest(w,p,s);if(moved==null){n.putString("Blocker","Companion return is blocked; retry safely.");return false;}lease.putUUID("Actor",moved.getUUID());}if(MouseCircusSite.inside(p,s)&&!MouseCircusSite.returnToAttic(p,s)){n.putString("Blocker","Owner return is blocked; preserving the venue.");return false;}if(!ShowActors.restore(p,s)||!MouseCircusSite.release(p,s)){n.putString("Blocker","Waiting for all original actors to return.");return false;}WybieOptionalVisits.release(p,lease);MouseCircusChoreography.cleanup(p,s,MouseCircusSite.center(p,s));ShowHouseAccess.restore(p,s);s.loop.event.remove("MouseCircusPending");OtherMotherLoop.changed(p);return true;}
    public static String blocker(VisitData.Scene s){return data(s).getString("Blocker");}
    public static void status(ServerPlayer p,VisitData.Scene s,CompoundTag out){out.putBoolean("ShowVenue",MouseCircusSite.inside(p,s));out.putString("ShowObjective",switch(data(s).getString("Phase")){case "INVITATION"->"Listen to Other Mother";case "TRAVEL"->WybieOptionalVisits.allowed(s)&&!data(s).getBoolean("WybieMet")?"Meet Other Wybie on the front porch":"Reach the attic";case "ATTIC"->"Enter the Mouse Circus";case "EXIT_READY"->"Leave the Mouse Circus";case "DEPARTING"->"Leave the attic";default->"Watch the Mouse Circus";});out.putInt("ArrivalRemaining",s.loop.event.getInt("ArrivalRemaining"));}
    public static void debug(ServerPlayer p,String command){
        if(!p.hasPermissions(2))return;var s=OtherMotherLoop.scene(p);if(s==null||!s.owner.equals(p.getUUID()))return;
        if(ShowDebugControls.needsSeek(false,command)){TheatreVisit.debugMessage(p,ShowDebugControls.SEEK_BLOCKER);return;}
        if(command.equals("state")){TheatreVisit.debugMessage(p,"eligible="+eligible(p,s)+" completed="+s.loop.story.getBoolean("MouseCircusCompleted")+" wybie="+WybieOptionalVisits.allowed(s)+" dimension="+ActivityWorld.KEY.location()+" arrival="+s.loop.event.getInt("ArrivalRemaining")+" mice="+MouseCircusChoreography.count(p,s,MouseCircusSite.center(p,s))+" / "+data(s)+" / "+ShowPlayback.data(s));return;}
        if(command.equals("reset")){s.loop.story.putBoolean("MouseCircusCompleted",false);OtherMotherLoop.changed(p);return;}
        if(command.equals("force")){VisitEvents.start(p,s,VisitEvents.Type.BOBINSKY_MOUSE_CIRCUS);return;}
        if(!owned(p,s)){TheatreVisit.debugMessage(p,"No owned Mouse Circus event is pending.");return;}var n=data(s);
        switch(command){
            case "recover"->recover(p,s);
            case "invitation"->{ShowPlayback.stop(p,s);n.putBoolean("DebugPaused",true);n.putBoolean("DebugAudio",ShowPlayback.start(p,s,"mouse_circus_invite",false));}
            case "wybie_present"->TheatreVisit.debugWybie(p,s,n.getCompound("Wybie"),true,true);
            case "wybie_absent"->TheatreVisit.debugWybie(p,s,n.getCompound("Wybie"),false,true);
            case "attic"->{if(MouseCircusSite.inside(p,s)){if(!MouseCircusSite.returnToAttic(p,s))TheatreVisit.debugMessage(p,"Saved attic return is blocked.");}else{var at=MouseCircusSite.atticDoor(p,s);var level=p.server.getLevel(OtherWorld.KEY);if(at!=null&&level!=null)TheatreVisit.debugTeleport(p,level,at);}}
            case "attic_props"->MouseCircusSite.prepareAttic(p,s);
            case "cotton_candy","popcorn"->{if(MouseCircusSite.inAttic(p,s)&&MouseCircusSite.prepareAttic(p,s)){var at=MouseCircusSite.tent(p,s);int kind=command.equals("popcorn")?CinematicPropEntity.FERRIS:CinematicPropEntity.CANNON;var prop=TheatreSite.ensure(p,s,p.serverLevel(),kind,kind==CinematicPropEntity.FERRIS?at.west(3):at.east(3),0);if(prop!=null){prop.frame(1,(int)n.getLong("Clock"),0);interact(p,s,prop);}}}
            case "enter"->{if(!s.loop.event.getBoolean("ShowAttended")){TheatreVisit.debugMessage(p,"The actual attic arrival must be registered before entering.");break;}enter(p,s);}
            case "audience"->{if(MouseCircusSite.inside(p,s))TheatreVisit.debugTeleport(p,p.serverLevel(),MouseCircusSite.audience(p,s));}
            case "introduction"->debugStart(p,s,0);
            case "formation","final_formation"->{if(debugPause(p,s)){var c=MouseCircusSite.center(p,s);MouseCircusChoreography.reveal(p,s,c);n.putBoolean("Revealed",true);int duration=ShowRecordings.duration("mouse_circus_music");if(command.equals("final_formation")&&duration>0)MouseCircusChoreography.frame(p,s,c,duration,duration);n.putString("DebugPreview","Visual symbol formation; audio cursor unchanged");}}
            case "music"->debugStart(p,s,1);
            case "bobinsky","approach"->{if(debugPause(p,s)){bobinsky(p,s,command.equals("bobinsky")?0:1);n.putBoolean("DebugApproach",command.equals("approach"));}}
            case "ending"->debugStart(p,s,2);
            case "open_exit"->{if(MouseCircusSite.inside(p,s)){debugPause(p,s);n.remove("DebugPaused");n.putBoolean("DebugExitOnly",!n.getBoolean("PerformanceDone"));n.putString("Phase","EXIT_READY");}}
            case "return"->{if(n.getBoolean("PerformanceDone"))returnToAttic(p,s);else if(MouseCircusSite.inside(p,s)){n.remove("DebugPaused");leaveTemporarily(p,s);}}
            case "departure"->{if(n.getBoolean("PerformanceDone")&&n.getBoolean("Returned"))departure(p,s);else TheatreVisit.debugMessage(p,"Departure requires the actual ending and safe attic return.");}
            case "complete"->{if(n.getBoolean("PerformanceDone")&&n.getBoolean("Returned"))departure(p,s);else TheatreVisit.debugMessage(p,"Completion requires the actual performance and attic return.");}
            default->TheatreVisit.debugMessage(p,"Unknown Mouse Circus control: "+command);
        }OtherMotherLoop.changed(p);
    }
    /** Prepare actors and lights at the requested offset, never an event-completion path. */
    static boolean prepareSeekFrame(ServerPlayer p,VisitData.Scene s,int at){
        if(!owned(p,s)||!MouseCircusSite.inside(p,s)||!data(s).getString("Phase").equals("SHOW"))return false;
        int segment=data(s).getInt("Segment"),duration=ShowPlayback.data(s).getInt("Duration");
        if(segment<0||segment>2||at<0||at>duration)return false;
        var center=MouseCircusSite.center(p,s);
        if(segment==2)return bobinsky(p,s,at);
        if(segment==0&&at<duration-60){MouseCircusChoreography.cleanup(p,s,center);ShowStageLighting.circus(p,s,-1,at);return true;}
        if(MouseCircusChoreography.count(p,s,center)<MouseCircusChoreography.COUNT)MouseCircusChoreography.reveal(p,s,center);
        if(MouseCircusChoreography.count(p,s,center)!=MouseCircusChoreography.COUNT)return false;
        if(segment==0){MouseCircusChoreography.introduction(p,s,center,at-duration+60);ShowStageLighting.circus(p,s,0,at);}
        else{MouseCircusChoreography.frame(p,s,center,at,duration);ShowStageLighting.circus(p,s,Math.min(4,(int)(at*5L/duration)),at);}
        return true;
    }
    /** No event flags, segment advancement or completion are produced by an offset audition. */
    private static void debugSeekFrame(ServerPlayer p,VisitData.Scene s){
        ShowPlayback.tick(p,s);var cursor=ShowPlayback.data(s);
        if(!ShowPlayback.started(s)||cursor.getBoolean("SeekReachedEnd")||!cursor.getString("Blocker").isEmpty())return;
        int at=ShowPlayback.position(s);
        if(!prepareSeekFrame(p,s,at))ShowAudioSeek.continuityFailed(p,s);
    }
    private static boolean debugPause(ServerPlayer p,VisitData.Scene s){if(!MouseCircusSite.inside(p,s))return false;ShowPlayback.stop(p,s);var n=data(s);n.putBoolean("DebugPaused",true);n.putBoolean("DebugAudio",false);n.putBoolean("DebugApproach",false);return true;}
    private static void debugStart(ServerPlayer p,VisitData.Scene s,int segment){if(!MouseCircusSite.inside(p,s))return;String blocker=ShowRecordings.blocker(false);if(!blocker.isEmpty()){TheatreVisit.debugMessage(p,blocker);return;}ShowPlayback.stop(p,s);var n=data(s);n.remove("DebugPaused");n.remove("DebugAudio");n.remove("NeedsRecovery");n.remove("Blocker");n.putInt("Segment",segment);n.putBoolean("SegmentStarted",false);n.putBoolean("PerformanceDone",false);n.putString("Phase","SHOW");if(segment==0){n.putBoolean("Revealed",false);MouseCircusChoreography.cleanup(p,s,MouseCircusSite.center(p,s));}OtherMotherLoop.phase(p,s,CIRCUS_ACTIVE);performance(p,s);}
    private MouseCircusVisit(){}
}
