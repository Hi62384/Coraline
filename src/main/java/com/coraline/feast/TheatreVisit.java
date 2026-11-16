package com.coraline.feast;

import java.util.*;
import com.coraline.otherworld.*;
import com.coraline.parents.ParentEntity;
import com.coraline.secrets.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import static com.coraline.feast.OtherMotherLoopState.Phase.*;

/** Once-only event component. The durable completion is set only after the physical reunion. */
public final class TheatreVisit {
    public static CompoundTag data(VisitData.Scene s){if(!s.loop.event.contains("Theatre"))s.loop.event.put("Theatre",new CompoundTag());return s.loop.event.getCompound("Theatre");}
    public static boolean pending(VisitData.Scene s){return s.loop.event.getBoolean("TheatrePending");}
    public static boolean eligible(ServerPlayer p,VisitData.Scene s){return s.owner.equals(p.getUUID())&&s.loop.story.getBoolean("TeaIntroductionComplete")&&!s.loop.story.getBoolean("TheatreCompleted")&&!pending(s)&&s.loop.suspicion<1&&s.loop.secrets.search!=SecretsState.Search.SCRIPTED_TRAP&&NumberedEventDialogue.ready("theatre")&&ShowRecordings.blocker(true).isEmpty()&&NauticalOpeningShow.blocker().isEmpty();}
    public static OtherMotherDialogue.Pool pool(String category,OtherMotherWelcome.State state){return new OtherMotherDialogue.Pool("events/theatre/"+category+"/"+SecretDialogue.stateId(state),2);}
    public static List<OtherMotherDialogue.Pool> pools(){var all=new ArrayList<OtherMotherDialogue.Pool>();for(var state:OtherMotherWelcome.State.values())for(String category:List.of("invitation","ontime","late","search"))all.add(pool(category,state));return all;}
    public static boolean prepare(ServerPlayer p,VisitData.Scene s){
        if(!eligible(p,s)||!TheatreSite.prepare(p,s))return false;var n=new CompoundTag();n.putUUID("Owner",s.owner);n.putInt("Serial",s.loop.event.getInt("Serial"));n.putString("Phase","TRAVEL");n.putString("Checkpoint","BEFORE_SITTING");n.putInt("Segment",0);n.put("Wybie",new CompoundTag());s.loop.event.put("Theatre",n);s.loop.event.putBoolean("TheatrePending",true);return true;
    }
    public static void beginWindow(ServerPlayer p,VisitData.Scene s){if(!owned(p,s))return;data(s).putBoolean("InvitationDone",true);ShowAttendance.begin(p,s);OtherMotherLoop.phase(p,s,THEATRE_WAIT);ShowHouseAccess.open(p,s,"basement");}
    public static boolean tick(ServerPlayer p,VisitData.Scene s){
        if(!owned(p,s)||!p.isAlive()||p.isSleeping())return true;var n=data(s);
        if(n.getBoolean("DebugPaused")){if(ShowPlayback.debugSeeking(s))debugSeekFrame(p,s);else if(n.contains("NauticalPreview"))NauticalOpeningShow.preview(p,s);else if(n.getBoolean("DebugAudio"))ShowPlayback.tick(p,s);return true;}
        n.remove("NauticalPreview");
        if(n.getBoolean("NeedsRecovery"))return true;n.putLong("Clock",n.getLong("Clock")+1);int cooldown=s.loop.event.getInt("ShowPortalCooldown");if(cooldown>0)s.loop.event.putInt("ShowPortalCooldown",cooldown-1);
        if(s.loop.suspicion>=1||s.loop.secrets.search==SecretsState.Search.SCRIPTED_TRAP){suspend(p,s);return true;}
        if(Reconciliation.active(p)||s.loop.secrets.searching()||SecretDialogue.busy(p))return true;
        if(s.loop.phase==THEATRE_WAIT){
            boolean entrance=TheatreSite.atEntrance(p,s)&&s.loop.event.getInt("ShowPortalCooldown")==0;
            if(!ShowAttendance.tick(p,s,entrance,"theatre","theatre_search"))return true;
            if(entrance){var realm=RealmData.get(p.server).realms.get(s.door);var lease=n.getCompound("Wybie");if(WybieOptionalVisits.allowed(s)&&!lease.hasUUID("Actor")&&realm!=null&&realm.porch!=null)WybieOptionalVisits.acquire(p,s,lease,realm.porch.north(2));}
            if(entrance&&!n.getBoolean("ArrivalGreeting")){var mother=MotherSearch.mother(p);n.putBoolean("ArrivalGreeting",true);if(mother!=null&&mother.distanceToSqr(p)<100&&mother.hasLineOfSight(p)){n.putLong("GreetingUntil",n.getLong("Clock")+NumberedEventDialogue.mother(p,"theatre",s.loop.event.getBoolean("Late")?"theatre_late":"theatre_ontime"));}}
            if(n.getLong("Clock")<n.getLong("GreetingUntil"))return true;
            if(entrance&&TheatreSite.enter(p,s)){TheatreSite.audience(p,s);n.putString("Phase","SEATING");OtherMotherLoop.phase(p,s,THEATRE_ACTIVE);companions(p,s);preparePerformers(p,s);}
            return true;
        }
        if(s.loop.phase==THEATRE_ACTIVE){
            if(!TheatreSite.inside(p,s)){recover(p,s);return true;}
            if(n.getString("Phase").equals("SEATING")){if(p.tickCount%40==0){TheatreSite.ensure(p,s,p.serverLevel(),CinematicPropEntity.SEAT,TheatreSite.playerSeat(p,s),0);TheatreSite.ensure(p,s,p.serverLevel(),CinematicPropEntity.SEAT,TheatreSite.companionSeat(p,s),1);TheatreSite.audience(p,s);}if(p.tickCount%30==0)p.displayClientMessage(Component.literal(TheatreSite.atExit(p,s)?"Your raised reserved seat is farther back. V toggles opera glasses.":"Right-click your reserved seat; V toggles opera glasses"),true);return true;}
            if(n.getString("Phase").equals("PERFORMING"))performance(p,s);
            if(n.getString("Phase").equals("EXIT_READY")){if(n.getBoolean("PerformanceDone")){int finale=(int)Math.min(Integer.MAX_VALUE,n.getLong("Clock")-n.getLong("FinaleClock"));audienceReaction(p,s,finale<120?3:0,finale);rose(p,s);}var wybie=WybieOptionalVisits.actor(p,n.getCompound("Wybie"));if(wybie!=null){wybie.stopRiding();if(WybieOptionalVisits.allowed(s))WybieOptionalVisits.follow(p,s,n.getCompound("Wybie"));else debugWybie(p,s,n.getCompound("Wybie"),false,false);}if(p.tickCount%30==0)p.displayClientMessage(Component.literal("Leave the Theatre"),true);if(TheatreSite.atExit(p,s)&&cooldown==0){if(n.getBoolean("DebugExitOnly")){if(TheatreSite.exit(p,s)){n.remove("DebugExitOnly");recover(p,s);}}else returnToHouse(p,s);}}
            return true;
        }
        if(s.loop.phase==THEATRE_RETURN)reunion(p,s);
        if(p.tickCount%20==0)OtherMotherLoop.changed(p);return true;
    }
    public static boolean owned(ServerPlayer p,VisitData.Scene s){var n=data(s);return pending(s)&&s.owner.equals(p.getUUID())&&n.hasUUID("Owner")&&s.owner.equals(n.getUUID("Owner"))&&n.getInt("Serial")==s.loop.event.getInt("Serial");}
    public static boolean basementAccess(ServerPlayer p,VisitData.Scene s){return owned(p,s)&&data(s).getBoolean("InvitationDone")&&!data(s).getBoolean("BasementExited");}
    private static void companions(ServerPlayer p,VisitData.Scene s){var n=data(s);var lease=n.getCompound("Wybie");if(!WybieOptionalVisits.allowed(s))return;Mob actor=WybieOptionalVisits.actor(p,lease);if(actor!=null){if(actor.level()!=p.level())actor=TheatreSite.enterGuest(actor,p,s);if(actor!=null){lease.putUUID("Actor",actor.getUUID());var seat=TheatreSite.ensure(p,s,p.serverLevel(),CinematicPropEntity.SEAT,TheatreSite.companionSeat(p,s),1);if(seat!=null)actor.startRiding(seat,true);PartyGuests.gesture(actor,7,80);}}}
    private static boolean preparePerformers(ServerPlayer p,VisitData.Scene s){var c=TheatreSite.center(p,s);var a=ShowActors.acquire(p,s,"other_miss_spink",p.serverLevel(),Vec3.atBottomCenterOf(c.offset(-8,1,-28)));var b=ShowActors.acquire(p,s,"other_miss_forcible",p.serverLevel(),Vec3.atBottomCenterOf(c.offset(8,1,-28)));if(a==null||b==null){data(s).putString("Blocker","Waiting for the original performers to be available.");return false;}data(s).remove("Blocker");return true;}
    public static boolean interact(ServerPlayer p,VisitData.Scene s,CinematicPropEntity prop){
        if(!owned(p,s)||!TheatreSite.inside(p,s)||prop.kind()!=CinematicPropEntity.SEAT||prop.slot()!=0||!data(s).getString("Phase").equals("SEATING"))return false;
        if(!preparePerformers(p,s))return true;String blocker=ShowRecordings.blocker(true);if(blocker.isEmpty())blocker=NauticalOpeningShow.blocker();if(!blocker.isEmpty()){data(s).putString("Blocker",blocker);p.displayClientMessage(Component.literal("The performance is waiting for its original recordings."),true);return true;}
        if(!p.startRiding(prop,true))return true;data(s).putString("Phase","PERFORMING");data(s).putInt("OpeningSeatDelay",0);data(s).putBoolean("SegmentStarted",false);OtherMotherLoop.changed(p);return true;
    }
    private static ScriptedShowTimeline plan(){return ScriptedShowTimeline.theatre(ShowRecordings.measured());}
    private static void performance(ServerPlayer p,VisitData.Scene s){
        var n=data(s);ScriptedShowTimeline timeline;try{timeline=plan();}catch(IllegalArgumentException bad){n.putString("Blocker",bad.getMessage());safePause(p,s);return;}var segments=timeline.segments();int index=n.getInt("Segment");
        if(index>=segments.size()){finishPerformance(p,s);return;}
        if(index<=5&&!(p.getVehicle() instanceof CinematicPropEntity seat&&seat.owned(s)&&seat.kind()==CinematicPropEntity.SEAT&&seat.slot()==0)){safePause(p,s);return;}
        if(index==0&&n.getInt("OpeningSeatDelay")<NauticalOpeningPlan.SEATED_DELAY){n.putInt("OpeningSeatDelay",n.getInt("OpeningSeatDelay")+1);curtain(p,s,0);return;}
        if(index==0){
            if(NauticalOpeningShow.tick(p,s)){n.putInt("Segment",1);n.putInt("Intermission",0);n.putString("Checkpoint","ACT_ONE_COMPLETE");n.putBoolean("SegmentStarted",false);}
            else if(!n.getString("Blocker").isEmpty())safePause(p,s);
            return;
        }
        if(index==1&&n.getInt("Intermission")<80){n.putInt("Intermission",n.getInt("Intermission")+1);curtain(p,s,0);audienceReaction(p,s,3,n.getInt("Intermission"));ShowStageLighting.theatre(p,s,-1,0);return;}
        var segment=segments.get(index);if(!n.getBoolean("SegmentStarted")){if(!ShowPlayback.start(p,s,segment.sound(),segment.music())){n.putString("Blocker",ShowPlayback.data(s).getString("Blocker"));safePause(p,s);return;}n.putBoolean("SegmentStarted",true);n.putString("Checkpoint",index==0?"BEFORE_SITTING":index<4?"ACT_TWO_INITIALIZED":index==4?"AFTER_TRANSFORMATIONS":index==5?"BEFORE_PLAYER_GRAB":"AERIAL_STUNT");}
        boolean ended=ShowPlayback.tick(p,s);int at=ShowPlayback.position(s);if(!ShowPlayback.data(s).getString("Blocker").isEmpty()){n.putString("Blocker",ShowPlayback.data(s).getString("Blocker"));safePause(p,s);return;}if(!ShowPlayback.started(s))return;
        n.putString("Cue",timeline.cue(segment.start()+at));animate(p,s,index,at,segment.duration());
        // Animation may suspend/recover the show; a failed frame cannot advance its checkpoint.
        if(!n.getString("Phase").equals("PERFORMING"))return;
        if(ended){
            if(index==0){curtain(p,s,0);n.putString("Checkpoint","ACT_ONE_COMPLETE");}
            if(index==5){var seat=p.getVehicle() instanceof CinematicPropEntity current?current:TheatreSite.ensure(p,s,p.serverLevel(),CinematicPropEntity.SEAT,TheatreSite.playerSeat(p,s),0);if(!ShowPlayerSafety.attach(p,s,seat)){safePause(p,s);return;}var wybie=WybieOptionalVisits.actor(p,n.getCompound("Wybie"));if(wybie!=null)PartyGuests.gesture(wybie,4,80);}
            if(index>0&&index+1<segments.size())ShowPlayback.transition(p,s,segments.get(index+1).sound());
            n.putInt("Segment",index+1);n.putBoolean("SegmentStarted",false);
            if(index==segments.size()-1)finishPerformance(p,s);else if(index!=0)performance(p,s);
        }
    }
    private static void animate(ServerPlayer p,VisitData.Scene s,int segment,int at,int duration){
        var c=TheatreSite.center(p,s);var a=ShowActors.actor(p,s,"other_miss_spink");var b=ShowActors.actor(p,s,"other_miss_forcible");if(a==null||b==null){if(!preparePerformers(p,s)){safePause(p,s);return;}a=ShowActors.actor(p,s,"other_miss_spink");b=ShowActors.actor(p,s,"other_miss_forcible");}
        curtain(p,s,segment<=1?Math.min(1,at/30F):1);
        audienceReaction(p,s,segment==6||segment==5&&at>=duration-20?2:segment==4&&at>=50?0:1,at);
        if(at%20==0)ShowStageLighting.theatre(p,s,segment,at);
        if(segment==0){NauticalOpeningShow.frame(p,s,at);return;}
        int boardClock=at+(segment>=3?ShowRecordings.duration("theatre_b"):0)+(segment>=4?ShowRecordings.duration("theatre_c"):0);
        for(var set:p.serverLevel().getEntitiesOfClass(CinematicPropEntity.class,new AABB(c).inflate(45),e->e.owned(s)&&e.kind()==CinematicPropEntity.OCEAN_SET))set.frame(-1,0,0);
        float boardStrength=segment==2?(float)ScriptedShowTimeline.smooth(at/10.0):segment==4?(float)(1-ScriptedShowTimeline.smooth((at-40)/10.0)):1;
        if(segment>=2)TheatreSite.ensure(p,s,p.serverLevel(),CinematicPropEntity.WATER_BUCKET,c.north(25).above(),0);
        var diving=segment<=4?TheatreSite.ensure(p,s,p.serverLevel(),CinematicPropEntity.DIVING,c.north(33),0):null;if(diving!=null)diving.frame(segment>=2&&segment<=4&&(segment!=4||at<50)?1:0,boardClock,boardStrength);
        if(segment<=4){
            boolean bounce=segment>=2&&(segment!=4||at<50);
            float revealA=segment<3?0:segment==3?Math.min(1,at/40F):1,revealB=segment<4?0:Math.min(1,at/50F);
            for(int side:new int[]{-1,1}){
                double height=bounce?(CinematicPropEntity.divingBoardOffset(boardClock,side)+CinematicPropEntity.divingJumpHeight(boardClock,side))*boardStrength:0;
                boolean transforming=side<0?segment==3:segment==4&&at<50;
                int pose=transforming?3:bounce?2:4;
                ShowActors.position(side<0?a:b,Vec3.atBottomCenterOf(c).add(side*10,18.02+height,-28.25),0,pose,at,side<0?revealA:revealB);
            }
            return;
        }
        var trapeze=TheatreSite.ensure(p,s,p.serverLevel(),CinematicPropEntity.TRAPEZE,c.north(15),0);var origin=Vec3.atBottomCenterOf(c);var grab=p.position().subtract(origin).add(0,.7,-1.2);if(trapeze!=null){trapeze.grabTarget(grab);trapeze.frame(segment==6?(data(s).getBoolean("ResumeAfterRescue")?3:2):1,at,duration);}
        if(segment==5){for(int side:new int[]{-1,1}){var q=TheatreAerialGeometry.performer(side,at,duration,grab.x,grab.y,grab.z);ShowActors.position(side<0?a:b,origin.add(q.x(),q.y(),q.z()),0,q.airborne()?16:5,at,1);}return;}
        if(segment==6){var path=(data(s).getBoolean("ResumeAfterRescue")&&!ShowPlayback.debugSeeking(s))?ScriptedShowTimeline.theatrePlayer(540,duration):ScriptedShowTimeline.theatrePlayer(at,duration);if(!s.loop.event.getBoolean("ShowStunt")){var seat=TheatreSite.ensure(p,s,p.serverLevel(),CinematicPropEntity.SEAT,TheatreSite.playerSeat(p,s),0);if(!ShowPlayerSafety.attach(p,s,seat)){safePause(p,s);return;}}if(!ShowPlayerSafety.move(p,s,path)){safePause(p,s);return;}
            if(at<480&&!(data(s).getBoolean("ResumeAfterRescue")&&!ShowPlayback.debugSeeking(s))){for(int side:new int[]{-1,1}){var q=TheatreAerialGeometry.carrying(side,at,duration,false);ShowActors.position(side<0?a:b,origin.add(q.x(),q.y()-TheatreAerialGeometry.GRIP_HEIGHT,q.z()),0,5,at,1);}}
            else if(at<540&&!(data(s).getBoolean("ResumeAfterRescue")&&!ShowPlayback.debugSeeking(s))){double rise=TheatreAerialGeometry.rise(at);ShowActors.position(a,origin.add(-.7,rise,-25),0,6,at,1);ShowActors.position(b,origin.add(.7,rise,-25),0,6,at,1);if(at>=510&&at%5==0)p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH,c.getX()+.5,c.getY()+.8,c.getZ()-24.5,3,.8,.15,.8,.02);}
            else{ShowActors.position(a,p.position().add(-.8,-.85,0),0,6,at,1);ShowActors.position(b,p.position().add(.8,-.85,0),0,6,at,1);nRescued(p,s,at);}
        }
    }
    private static void nRescued(ServerPlayer p,VisitData.Scene s,int at){if(ShowPlayback.debugSeeking(s))return;var n=data(s);if(!n.getBoolean("Rescued")){n.putBoolean("Rescued",true);n.putString("Checkpoint","AFTER_AERIAL_RESCUE");p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH,p.getX(),p.getY(),p.getZ(),24,.8,.4,.8,.1);}}
    private static void audienceReaction(ServerPlayer p,VisitData.Scene s,int cue,int elapsed){
        for(var row:p.serverLevel().getEntitiesOfClass(CinematicPropEntity.class,new AABB(TheatreSite.center(p,s)).inflate(80),e->e.owned(s)&&e.kind()==CinematicPropEntity.DOG_ROW))row.frame(cue,elapsed,0);
    }
    static void curtain(ServerPlayer p,VisitData.Scene s,float openness){var c=TheatreSite.center(p,s);var prop=TheatreSite.ensure(p,s,p.serverLevel(),CinematicPropEntity.CURTAIN,c.north(16),0);if(prop!=null)prop.frame(0,0,openness);}
    private static void finishPerformance(ServerPlayer p,VisitData.Scene s){var n=data(s);ShowPlayerSafety.release(p,s);var safe=TheatreSite.center(p,s).offset(0,1,-24);p.teleportTo(p.serverLevel(),safe.getX()+.5,safe.getY(),safe.getZ()+.5,p.getYRot(),p.getXRot());n.putString("Phase","EXIT_READY");n.putString("Checkpoint","AFTER_AERIAL_RESCUE");n.putBoolean("PerformanceDone",true);n.putLong("FinaleClock",n.getLong("Clock"));audienceReaction(p,s,3,0);for(String id:List.of("other_miss_spink","other_miss_forcible")){var actor=ShowActors.actor(p,s,id);if(actor!=null)ShowPlaybackNetwork.pose(actor,7,0,1);}rose(p,s);OtherMotherLoop.changed(p);}
    private static void rose(ServerPlayer p,VisitData.Scene s){
        var n=data(s);if(acknowledgeRose(p,s))return;if(!n.hasUUID("RoseToken")){n.putUUID("RoseToken",UUID.randomUUID());OtherMotherLoop.changed(p);}UUID token=n.getUUID("RoseToken");
        var existing=p.serverLevel().getEntitiesOfClass(ItemEntity.class,new AABB(TheatreSite.center(p,s)).inflate(100),item->roseToken(item.getItem(),token));if(!existing.isEmpty())return;
        if(n.getBoolean("RoseSpawned")){n.putString("Blocker","Theatre Rose is awaiting collection or inventory space.");return;}var item=new ItemStack(Items.POPPY);item.setHoverName(Component.literal("Theatre Rose"));item.getOrCreateTag().putUUID("TheatreRoseToken",token);item.getOrCreateTag().putUUID("TheatreRoseOwner",s.owner);
        var wybie=WybieOptionalVisits.actor(p,n.getCompound("Wybie"));var performer=ShowActors.actor(p,s,"other_miss_spink");if(performer==null)performer=ShowActors.actor(p,s,"other_miss_forcible");if(wybie==null&&performer==null){n.putString("Blocker","Waiting for a performer to present the Theatre Rose.");return;}Vec3 source=(wybie!=null?wybie:performer).position().add(0,1.2,0);if(wybie==null)ShowPlaybackNetwork.pose(performer,8,0,1);if(wybie!=null){wybie.stopRiding();PartyGuests.gesture(wybie,1,100);wybie.setItemSlot(EquipmentSlot.MAINHAND,item.copy());}
        if(wybie!=null){int windup=n.getInt("RoseWindup")+1;n.putInt("RoseWindup",windup);PartyGuests.gesture(wybie,15,30);if(windup<12)return;PartyGuests.gesture(wybie,16,35);wybie.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);}
        var rose=new ItemEntity(p.serverLevel(),source.x,source.y,source.z,item);rose.setTarget(s.owner);rose.setUnlimitedLifetime();rose.setNoPickUpDelay();rose.setDeltaMovement(roseVelocity(source,p.position().add(0,.7,0)));if(p.serverLevel().addFreshEntity(rose)){n.putBoolean("RoseSpawned",true);OtherMotherLoop.changed(p);}
    }
    /** Reconcile an already collected reward before venue exit or a resumed delivery. */
    private static boolean acknowledgeRose(ServerPlayer p,VisitData.Scene s){
        var n=data(s);if(n.getBoolean("RoseDelivered"))return true;if(!n.hasUUID("RoseToken"))return false;
        UUID token=n.getUUID("RoseToken");
        for(int slot=0;slot<p.getInventory().getContainerSize();slot++)if(roseToken(p.getInventory().getItem(slot),token)){
            n.putBoolean("RoseDelivered",true);n.remove("PendingRose");OtherMotherLoop.changed(p);return true;
        }
        return false;
    }
    private static boolean roseToken(ItemStack item,UUID token){return item.hasTag()&&item.getTag().hasUUID("TheatreRoseToken")&&token.equals(item.getTag().getUUID("TheatreRoseToken"));}
    static Vec3 roseVelocity(Vec3 source,Vec3 target){double distance=source.distanceTo(target),ticks=Math.max(16,Math.min(32,distance*1.6)),drag=.98,sum=(1-Math.pow(drag,ticks))/(1-drag);var delta=target.subtract(source);return new Vec3(delta.x/sum,(delta.y+.04/(1-drag)*(ticks-drag*sum))/sum,delta.z/sum);}
    static void stageReunionParents(ServerPlayer p,VisitData.Scene s){
        var n=data(s);if(n.getBoolean("ReunionDone"))return;
        var r=RealmData.get(p.server).realms.get(s.door);var l=p.server.getLevel(OtherWorld.KEY);
        if(r==null||r.house==null||l==null)return;
        var top=HouseCabinets.at(r.house,26,4,-1);l.getChunkAt(top);
        if(r.piano!=null)l.getChunkAt(r.piano);if(s.loop.motherPos!=null)l.getChunkAt(s.loop.motherPos);boolean both=true;
        for(var id:new UUID[]{s.mom,s.dad}){
            if(id==null||!(l.getEntity(id) instanceof ParentEntity)){both=false;continue;}
            var parent=(ParentEntity)l.getEntity(id);var spot=top.relative(net.minecraft.core.Direction.EAST,parent.isMom()?-1:1);
            var safe=landingSpot(l,parent,spot);if(safe==null){both=false;continue;}
            n.putLong(parent.isMom()?"LandingMother":"LandingFather",safe.asLong());
            if(parent.distanceToSqr(Vec3.atBottomCenterOf(safe))>1&&p.level()==l&&p.hasLineOfSight(parent)){
                parent.setNoAi(false);if(parent.tickCount%20==0)parent.getNavigation().moveTo(safe.getX()+.5,safe.getY(),safe.getZ()+.5,.9);both=false;continue;
            }
            parent.stopRiding();parent.setNoAi(true);parent.setNoGravity(false);parent.getNavigation().stop();
            parent.setDeltaMovement(Vec3.ZERO);parent.setPos(Vec3.atBottomCenterOf(safe));
            parent.setYRot(180);parent.setYBodyRot(180);parent.setYHeadRot(180);parent.setXRot(10);
        }
        if(both){n.putBoolean("LandingParentsPlaced",true);OtherMotherLoop.changed(p);}
        else n.putBoolean("LandingParentsPlaced",false);
    }
    private static BlockPos landingSpot(ServerLevel level,ParentEntity actor,BlockPos expected){
        // Never use the broad safe-player search: it may pick another floor/room.
        for(int dz:new int[]{0,1,-1,2})for(int dy:new int[]{0,1,-1}){
            var at=expected.offset(0,dy,dz);var feet=Vec3.atBottomCenterOf(at);
            if(OutingSites.feet(level,at)&&level.noCollision(actor,actor.getDimensions(actor.getPose()).makeBoundingBox(feet)))return at;
        }return null;
    }
    private static void returnToHouse(ServerPlayer p,VisitData.Scene s){if(!data(s).getBoolean("PerformanceDone"))return;if(!data(s).getBoolean("RoseSpawned")&&!acknowledgeRose(p,s)){rose(p,s);return;}collectRose(p,s);var wybie=WybieOptionalVisits.actor(p,data(s).getCompound("Wybie"));if(wybie!=null){var returned=TheatreSite.exitGuest(wybie,p,s);if(returned==null){data(s).putString("Blocker","Waiting for companion return transition.");return;}data(s).getCompound("Wybie").putUUID("Actor",returned.getUUID());}stageReunionParents(p,s);if(!TheatreSite.exit(p,s))return;data(s).putString("Phase","REUNION");data(s).putString("Checkpoint","AFTER_THEATRE_EXIT");OtherMotherLoop.phase(p,s,THEATRE_RETURN);ShowPlayback.stop(p,s);OtherMotherLoop.changed(p);}
    private static void collectRose(ServerPlayer p,VisitData.Scene s){var n=data(s);if(acknowledgeRose(p,s)||!n.hasUUID("RoseToken"))return;for(var item:p.serverLevel().getEntitiesOfClass(ItemEntity.class,new AABB(TheatreSite.center(p,s)).inflate(100),i->roseToken(i.getItem(),n.getUUID("RoseToken")))){if(p.getInventory().add(item.getItem())){item.discard();n.putBoolean("RoseDelivered",true);p.getInventory().setChanged();}else{n.put("PendingRose",item.getItem().save(new CompoundTag()));item.discard();}break;}}
    private static void deliverPendingRose(ServerPlayer p,VisitData.Scene s){var n=data(s);if(acknowledgeRose(p,s)||!n.contains("PendingRose"))return;var stack=ItemStack.of(n.getCompound("PendingRose"));if(stack.isEmpty())return;if(p.getInventory().add(stack)){n.putBoolean("RoseDelivered",true);n.remove("PendingRose");p.getInventory().setChanged();OtherMotherLoop.changed(p);}else if(p.tickCount%100==0)p.displayClientMessage(Component.literal("Make one inventory space for your Theatre Rose."),true);}
    public static boolean parentControl(ParentEntity actor,ServerPlayer p,VisitData.Scene s){return pending(s)&&s.loop.phase==THEATRE_RETURN&&!Reconciliation.active(p)&&!s.loop.secrets.searching();}
    private static void reunion(ServerPlayer p,VisitData.Scene s){
        var house=RealmData.get(p.server).realms.get(s.door);if(house!=null&&house.house!=null&&!data(s).getBoolean("BasementExited")&&OtherWorld.isOther(p.level())&&p.getY()>=HouseCabinets.at(house.house,26,4,-1).getY()&&p.distanceToSqr(Vec3.atBottomCenterOf(HouseCabinets.at(house.house,26,4,-1)))<144){
            data(s).putBoolean("BasementExited",true);ShowHouseAccess.restore(p,s);OtherMotherLoop.changed(p);
        }
        var n=data(s);deliverPendingRose(p,s);if(n.getCompound("Wybie").hasUUID("Actor")&&WybieOptionalVisits.actor(p,n.getCompound("Wybie"))==null){n.putString("Blocker","Companion is unloaded; preserving the return lease.");return;}var realm=RealmData.get(p.server).realms.get(s.door);if(realm==null||realm.house==null)return;VisitEvents.ensureFather(p,s);var mother=MotherSearch.mother(p);var father=VisitEvents.father(p,s);var stairTop=HouseCabinets.at(realm.house,26,4,-1);
        if(mother==null||father==null)return;mother.lookAtStoryPlayer(p);father.lookAtStoryPlayer(p);
        if(!n.getBoolean("ReunionDone"))stageReunionParents(p,s);
        if(!n.getBoolean("ReunionStarted")){if(!n.getBoolean("LandingParentsPlaced")||mother.distanceToSqr(p)>100||father.distanceToSqr(p)>100||!mother.hasLineOfSight(p)||!father.hasLineOfSight(p))return;
            if(!ShowPlayback.start(p,s,"theatre_meet_parents",false))return;n.putBoolean("ReunionStarted",true);n.putString("Checkpoint","PARENT_REUNION");}
        if(!n.getBoolean("ReunionDone")){if(!ShowPlayback.tick(p,s))return;n.putBoolean("ReunionDone",true);}
        var target=RosewoodHouse.householdStops(realm.house).get(4).south(5);navigate(mother,target.west(2),p,true);navigate(father,target.east(2),p,true);WybieOptionalVisits.follow(p,s,n.getCompound("Wybie"));
        int returning=n.getInt("ReturnHomeTicks")+1;n.putInt("ReturnHomeTicks",returning);
        if(n.getString("Phase").equals("DEPARTING")||HouseParty.insideHouse(p,p.position())&&(p.distanceToSqr(Vec3.atBottomCenterOf(target))<100||returning>=200)){
            n.putString("Phase","DEPARTING");var wybie=WybieOptionalVisits.actor(p,n.getCompound("Wybie"));
            if(wybie!=null&&realm.porch!=null)navigate(wybie,realm.porch.north(10),p,false);
            if(realm.piano!=null)navigate(father,realm.piano,p,false);
            if(wybie==null||offscreen(p,wybie)||returning>=400){WybieOptionalVisits.release(p,n.getCompound("Wybie"));father.getPersistentData().remove("VisitEventOwner");father.eventGaze(false);ShowHouseAccess.restore(p,s);complete(p,s);}
        }
    }
    static void navigate(Mob actor,BlockPos target,ServerPlayer p,boolean wait){actor.stopRiding();actor.setNoAi(false);actor.setNoGravity(false);actor.noPhysics=false;if(wait&&actor.distanceToSqr(p)>144){actor.getNavigation().stop();actor.getLookControl().setLookAt(p,20,20);return;}if(actor.tickCount%20==0){if(actor.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation nav){nav.setCanOpenDoors(true);nav.setCanPassDoors(true);}actor.setMaxUpStep(1.05F);var path=actor.getNavigation().createPath(target,1);if(path!=null&&path.getNodeCount()>1)actor.getNavigation().moveTo(path,1.15);else if(!p.hasLineOfSight(actor)&&p.distanceToSqr(actor)>64){var safe=OtherWorld.safePlayer((ServerLevel)actor.level(),target);if(safe!=null)actor.setPos(Vec3.atBottomCenterOf(safe));}}}
    static boolean offscreen(ServerPlayer p,Entity e){return e.level()!=p.level()||!p.hasLineOfSight(e)||p.getLookAngle().dot(e.position().subtract(p.getEyePosition()).normalize())<.05;}
    private static void complete(ServerPlayer p,VisitData.Scene s){var n=data(s);if(!n.getBoolean("PerformanceDone")||!n.getBoolean("ReunionDone")||!n.getBoolean("RoseDelivered"))return;if(!cleanup(p,s))return;s.loop.story.putBoolean("TheatreCompleted",true);OtherMotherLoop.changed(p);VisitEvents.next(p,s,false);}
    private static void safePause(ServerPlayer p,VisitData.Scene s){ShowPlayerSafety.release(p,s);ShowPlayback.stop(p,s);NauticalOpeningShow.interrupt(s);NauticalOpeningShow.hide(p,s);curtain(p,s,0);data(s).putString("Phase","SEATING");data(s).putBoolean("SegmentStarted",false);var safe=TheatreSite.playerSeat(p,s).south();p.teleportTo(p.serverLevel(),safe.getX()+.5,safe.getY(),safe.getZ()+.5,p.getYRot(),p.getXRot());}
    public static void suspend(ServerPlayer p,VisitData.Scene s){
        if(!owned(p,s))return;
        ShowPlayerSafety.release(p,s);ShowPlayback.stop(p,s);NauticalOpeningShow.interrupt(s);NauticalOpeningShow.hide(p,s);
        data(s).putBoolean("SegmentStarted",false);data(s).putBoolean("NeedsRecovery",true);
        if(p.isAlive()&&TheatreSite.inside(p,s)){var safe=TheatreSite.playerSeat(p,s).south();p.teleportTo(p.serverLevel(),safe.getX()+.5,safe.getY(),safe.getZ()+.5,p.getYRot(),p.getXRot());}
        ShowStageLease.release(p,s,false);
        if(data(s).getBoolean("ReunionStarted")&&!data(s).getBoolean("ReunionDone"))data(s).putBoolean("ReunionStarted",false);
        OtherMotherLoop.changed(p);
    }
    public static void recover(ServerPlayer p,VisitData.Scene s){
        if(!owned(p,s))return;var n=data(s);
        if(n.contains("NauticalPreview")){ShowPlayback.stop(p,s);n.remove("NauticalPreview");n.remove("DebugAudio");NauticalOpeningShow.hide(p,s);}
        n.remove("DebugPaused");ShowPlayerSafety.release(p,s);
        if(!n.getBoolean("InvitationDone")){n.putBoolean("NeedsRecovery",false);OtherMotherLoop.phase(p,s,EVENT_INTRO);return;}
        if(n.getBoolean("PerformanceDone")){ShowStageLease.hold(p,s,false);n.putString("Phase",TheatreSite.inside(p,s)?"EXIT_READY":"REUNION");OtherMotherLoop.phase(p,s,TheatreSite.inside(p,s)?THEATRE_ACTIVE:THEATRE_RETURN);}
        else if(TheatreSite.inside(p,s)){if(n.getBoolean("Rescued"))n.putBoolean("ResumeAfterRescue",true);TheatreSite.prepare(p,s);TheatreSite.audience(p,s);safePause(p,s);OtherMotherLoop.phase(p,s,THEATRE_ACTIVE);}
        else{n.putString("Phase","TRAVEL");OtherMotherLoop.phase(p,s,THEATRE_WAIT);}
        n.putBoolean("NeedsRecovery",false);
    }
    public static boolean resume(ServerPlayer p){var s=OtherMotherLoop.scene(p);if(s==null||!pending(s))return false;ShowAttendance.reconciled(s);recover(p,s);return true;}
    public static boolean cleanup(ServerPlayer p,VisitData.Scene s){
        if(!pending(s))return s.owner.equals(p.getUUID());if(!owned(p,s))return false;
        ShowStageLease.hold(p,s,false);ShowPlayerSafety.release(p,s);ShowPlayback.stop(p,s);NauticalOpeningShow.hide(p,s);
        var n=data(s);var lease=n.getCompound("Wybie");var wybie=WybieOptionalVisits.actor(p,lease);
        if(lease.hasUUID("Actor")&&wybie==null){n.putString("Blocker","Companion is unloaded; preserving the return lease.");return false;}
        if(wybie!=null){var moved=TheatreSite.exitGuest(wybie,p,s);if(moved==null){n.putString("Blocker","Companion return is blocked; retry safely.");return false;}lease.putUUID("Actor",moved.getUUID());}
        if(TheatreSite.inside(p,s)&&!TheatreSite.exit(p,s)){n.putString("Blocker","Owner return is blocked; preserving the venue.");return false;}
        if(!ShowActors.restore(p,s)||!TheatreSite.release(p,s)){n.putString("Blocker","Waiting for all original actors to return.");return false;}
        WybieOptionalVisits.release(p,lease);ShowHouseAccess.restore(p,s);s.loop.event.remove("TheatrePending");OtherMotherLoop.changed(p);return true;
    }
    public static String blocker(VisitData.Scene s){return data(s).getString("Blocker");}
    public static void status(ServerPlayer p,VisitData.Scene s,CompoundTag out){
        boolean inside=TheatreSite.inside(p,s);var n=data(s);String phase=n.getString("Phase");out.putBoolean("ShowVenue",inside);
        out.putBoolean("TheatreQuiet",inside);
        out.putBoolean("TheatreOperaGlasses",inside&&Set.of("SEATING","PERFORMING").contains(phase)&&!s.loop.event.getBoolean("ShowStunt"));
        out.putBoolean("TheatrePrelude",owned(p,s)&&inside&&phase.equals("SEATING")&&!n.getBoolean("NeedsRecovery")&&!n.getBoolean("DebugPaused"));
        var realm=RealmData.get(p.server).realms.get(s.door);if(inside)out.putLong("TheatrePreludeAt",TheatreSite.center(p,s).asLong());else if(realm!=null&&realm.house!=null)out.putLong("TheatrePreludeAt",HouseCabinets.at(realm.house,34,6,36).asLong());
        if(!n.getBoolean("InvitationDone"))return;out.putString("ShowObjective",switch(phase){case "TRAVEL"->"Reach the basement theatre entrance";case "SEATING"->"Right-click your reserved seat to sit";case "EXIT_READY"->"Leave the Theatre";case "REUNION","DEPARTING"->"Follow your parents back to the house";default->"Watch the performance";});out.putInt("ArrivalRemaining",s.loop.event.getInt("ArrivalRemaining"));
    }
    public static void debug(ServerPlayer p,String command){
        if(!p.hasPermissions(2))return;var s=OtherMotherLoop.scene(p);if(s==null||!s.owner.equals(p.getUUID()))return;
        if(ShowDebugControls.needsSeek(true,command)){debugMessage(p,ShowDebugControls.SEEK_BLOCKER);return;}
        if(command.equals("state")){debugMessage(p,"eligible="+eligible(p,s)+" completed="+s.loop.story.getBoolean("TheatreCompleted")+" wybie="+WybieOptionalVisits.allowed(s)+" dimension="+ActivityWorld.KEY.location()+" seat="+TheatreSite.playerSeat(p,s)+" return="+s.loop.event.contains("TheatreReturn")+" / "+data(s)+" / "+ShowPlayback.data(s));return;}
        if(command.equals("reset")){s.loop.story.putBoolean("TheatreCompleted",false);OtherMotherLoop.changed(p);return;}
        if(command.equals("force")){VisitEvents.start(p,s,VisitEvents.Type.THEATRE);return;}
        if(!owned(p,s)){debugMessage(p,"No owned Theatre event is pending.");return;}var n=data(s);
        if(command.matches("opening_[a-n]")){
            if(!debugPause(p,s))return;
            char cue=Character.toUpperCase(command.charAt(8));
            n.putInt("NauticalPreview",cue-'A');n.putBoolean("DebugAudio",ShowPlayback.start(p,s,NauticalOpeningPlan.sound(cue),true));
            n.putString("DebugPreview","Opening "+cue+" audition only; no checkpoint or completion changes.");
            OtherMotherLoop.changed(p);return;
        }
        switch(command){
            case "recover"->recover(p,s);
            case "on_time","late"->{if(s.loop.secrets.searching()||Reconciliation.active(p)){debugMessage(p,"Finish the physical search/reconciliation before changing arrival.");break;}s.loop.event.putBoolean("ShowAttended",false);s.loop.event.putInt("ArrivalRemaining",command.equals("late")?0:1);ShowAttendance.arrive(p,s);}
            case "missed"->{s.loop.event.putBoolean("ShowAttended",false);s.loop.event.putInt("ArrivalRemaining",0);s.loop.event.putInt("GraceRemaining",0);s.loop.event.putBoolean("GraceRolled",true);ShowAttendance.tick(p,s,false,"theatre","theatre_search");}
            case "entrance"->{if(TheatreSite.inside(p,s)){if(!TheatreSite.exit(p,s))debugMessage(p,"Saved return is blocked.");}else{var level=p.server.getLevel(OtherWorld.KEY);var at=TheatreSite.houseDoor(p,s);if(level!=null&&at!=null)debugTeleport(p,level,at);}}
            case "enter"->{if(TheatreSite.enter(p,s)){TheatreSite.audience(p,s);n.putString("Phase","SEATING");OtherMotherLoop.phase(p,s,THEATRE_ACTIVE);companions(p,s);preparePerformers(p,s);}else debugMessage(p,"Reach the real basement doorway first; safe entry is unavailable.");}
            case "seat"->{if(TheatreSite.inside(p,s))debugTeleport(p,p.serverLevel(),TheatreSite.playerSeat(p,s));}
            case "wybie_present"->debugWybie(p,s,n.getCompound("Wybie"),true,false);
            case "wybie_absent"->debugWybie(p,s,n.getCompound("Wybie"),false,false);
            case "audience"->{if(TheatreSite.inside(p,s))TheatreSite.audience(p,s);}
            case "act_one"->{NauticalOpeningShow.restart(s);debugStart(p,s,0);}
            case "act_two","a"->debugStart(p,s,1);
            case "b"->debugStart(p,s,2);
            case "spink_transform"->debugPreview(p,s,3,20);
            case "forcible_transform"->debugPreview(p,s,4,25);
            case "freeze"->debugPreview(p,s,4,50);
            case "trapeze"->debugStart(p,s,5);
            case "grab"->{int duration=ShowRecordings.duration("theatre_e");if(duration>20)debugPreview(p,s,5,duration-1);}
            case "swing"->debugStart(p,s,6);
            case "rescue"->debugPreview(p,s,6,540);
            case "applause"->{if(debugPause(p,s)){for(String id:List.of("other_miss_spink","other_miss_forcible")){var actor=ShowActors.actor(p,s,id);if(actor!=null)ShowPlaybackNetwork.pose(actor,7,0,1);}n.putString("DebugPreview","APPLAUSE");}}
            case "rose"->{if(TheatreSite.inside(p,s))rose(p,s);}
            case "open_exit"->{if(TheatreSite.inside(p,s)){debugPause(p,s);n.remove("DebugPaused");n.putBoolean("DebugExitOnly",!n.getBoolean("PerformanceDone"));n.putString("Phase","EXIT_READY");}}
            case "exit","return"->{if(n.getBoolean("PerformanceDone"))returnToHouse(p,s);else if(TheatreSite.inside(p,s)&&TheatreSite.exit(p,s)){n.remove("DebugPaused");n.remove("DebugExitOnly");recover(p,s);}}
            case "parents_audio"->{if(!TheatreSite.inside(p,s)){ShowPlayback.stop(p,s);n.putBoolean("DebugPaused",true);n.putBoolean("DebugAudio",ShowPlayback.start(p,s,"theatre_meet_parents",false));}}
            case "escort"->{if(n.getBoolean("PerformanceDone")&&n.getBoolean("ReunionDone")){n.remove("DebugPaused");OtherMotherLoop.phase(p,s,THEATRE_RETURN);reunion(p,s);}else debugMessage(p,"Parent escort requires the actual performance and reunion recording to finish.");}
            case "complete"->{if(n.getBoolean("PerformanceDone")&&n.getBoolean("ReunionDone")&&n.getBoolean("RoseDelivered"))complete(p,s);else debugMessage(p,"Completion requires performance, reunion and the unique rose delivery.");}
            default->debugMessage(p,"Unknown Theatre control: "+command);
        }OtherMotherLoop.changed(p);
    }
    /** Reconstruct the acknowledged frame before accepting its new audio cursor.
     * This is an audition only: no checkpoint, reward, rose or completion changes. */
    static boolean prepareSeekFrame(ServerPlayer p,VisitData.Scene s,int at){
        if(!owned(p,s)||!TheatreSite.inside(p,s)||!data(s).getString("Phase").equals("PERFORMING")||!preparePerformers(p,s))return false;
        int segment=data(s).getInt("Segment"),duration=ShowPlayback.data(s).getInt("Duration");
        if(segment<0||segment>6||at<0||at>duration)return false;
        var seat=TheatreSite.ensure(p,s,p.serverLevel(),CinematicPropEntity.SEAT,TheatreSite.playerSeat(p,s),0);
        if(seat==null)return false;
        if(segment==6){if(!ShowPlayerSafety.attach(p,s,seat))return false;}
        else if(p.getVehicle()!=seat&&!p.startRiding(seat,true))return false;
        // A backward seek must not keep geometry from a later visual preview.
        for(var prop:p.serverLevel().getEntitiesOfClass(CinematicPropEntity.class,new AABB(TheatreSite.center(p,s)).inflate(80),e->e.owned(s)))
            if(prop.kind()==CinematicPropEntity.DIVING&&(segment==0||segment>=5)||prop.kind()==CinematicPropEntity.TRAPEZE&&segment<5)prop.discard();
        int lighting=segment==0?(at/(double)duration<.16?0:at/(double)duration<.55?1:at/(double)duration<.82?2:5):segment;
        ShowStageLighting.theatre(p,s,lighting,at);
        animate(p,s,segment,at,duration);
        return data(s).getString("Phase").equals("PERFORMING")&&(segment!=6||ShowPlayerSafety.active(p));
    }
    /** Renders the acknowledged offset without running segment completion, roses or reunion. */
    private static void debugSeekFrame(ServerPlayer p,VisitData.Scene s){
        ShowPlayback.tick(p,s);var cursor=ShowPlayback.data(s);
        if(!ShowPlayback.started(s)||cursor.getBoolean("SeekReachedEnd")||!cursor.getString("Blocker").isEmpty())return;
        int segment=data(s).getInt("Segment");
        if(segment>=0&&segment<=6){animate(p,s,segment,ShowPlayback.position(s),cursor.getInt("Duration"));if(!data(s).getString("Phase").equals("PERFORMING"))ShowAudioSeek.continuityFailed(p,s);}
    }
    private static void debugStart(ServerPlayer p,VisitData.Scene s,int segment){if(!TheatreSite.inside(p,s)||!preparePerformers(p,s))return;String blocker=ShowRecordings.blocker(true);if(!blocker.isEmpty()){debugMessage(p,blocker);return;}ShowPlayerSafety.release(p,s);ShowPlayback.stop(p,s);var n=data(s);n.remove("DebugPaused");n.remove("DebugAudio");n.remove("NeedsRecovery");n.remove("Blocker");n.remove("ResumeAfterRescue");n.putInt("Segment",segment);n.putInt("Intermission",80);n.putBoolean("SegmentStarted",false);n.putString("Phase","PERFORMING");n.putBoolean("PerformanceDone",false);var seat=TheatreSite.ensure(p,s,p.serverLevel(),CinematicPropEntity.SEAT,TheatreSite.playerSeat(p,s),0);if(seat==null||!p.startRiding(seat,true)){n.putString("Phase","SEATING");return;}OtherMotherLoop.phase(p,s,THEATRE_ACTIVE);performance(p,s);}
    private static boolean debugPause(ServerPlayer p,VisitData.Scene s){if(!TheatreSite.inside(p,s)||!preparePerformers(p,s))return false;ShowPlayerSafety.release(p,s);ShowPlayback.stop(p,s);var n=data(s);n.remove("NauticalPreview");n.putBoolean("DebugPaused",true);n.putBoolean("DebugAudio",false);return true;}
    private static void debugPreview(ServerPlayer p,VisitData.Scene s,int segment,int at){if(!debugPause(p,s))return;int duration=ShowRecordings.duration("theatre_"+(char)('a'+segment-1));if(duration<=at){debugMessage(p,"Verified recording is too short or unavailable; preview is blocked.");return;}animate(p,s,segment,at,duration);data(s).putString("DebugPreview","Visual frame only: segment="+segment+" tick="+at);debugMessage(p,"Visual preview is paused. The recording has not been sought. Use Recover to resume the safe checkpoint.");}
    static void debugMessage(ServerPlayer p,String text){p.sendSystemMessage(Component.literal(text));}
    static boolean debugTeleport(ServerPlayer p,ServerLevel level,BlockPos at){level.getChunkAt(at);var safe=OtherWorld.safePlayer(level,at);if(safe==null)return false;p.stopRiding();p.teleportTo(level,safe.getX()+.5,safe.getY(),safe.getZ()+.5,p.getYRot(),p.getXRot());p.fallDistance=0;return true;}
    static void debugWybie(ServerPlayer p,VisitData.Scene s,CompoundTag lease,boolean present,boolean circus){if(present){if(!WybieOptionalVisits.allowed(s)){debugMessage(p,"Wybie's real introduction/forbidden gate does not allow attendance.");return;}if(!OtherWorld.isOther(p.level())){debugMessage(p,"Request Wybie from the real house before entering the venue.");return;}var realm=RealmData.get(p.server).realms.get(s.door);if(realm!=null&&realm.porch!=null&&WybieOptionalVisits.acquire(p,s,lease,realm.porch.north(2))==null)debugMessage(p,"Wybie is unavailable or already leased; no replacement actor was spawned.");}else{var actor=WybieOptionalVisits.actor(p,lease);if(lease.hasUUID("Actor")&&actor==null){debugMessage(p,"Saved Wybie is unloaded; preserving his lease.");return;}if(actor!=null){var returned=circus?MouseCircusSite.exitGuest(actor,p,s):TheatreSite.exitGuest(actor,p,s);if(returned==null){debugMessage(p,"Wybie's safe return is blocked.");return;}lease.putUUID("Actor",returned.getUUID());}WybieOptionalVisits.release(p,lease);}}
    private TheatreVisit(){}
}
