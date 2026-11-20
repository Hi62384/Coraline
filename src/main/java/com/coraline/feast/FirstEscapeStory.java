package com.coraline.feast;

import com.coraline.entity.CharacterEntity;
import com.coraline.otherworld.*;
import com.coraline.parents.*;
import com.coraline.secrets.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
import static com.coraline.feast.FirstEscapeTimeline.Phase.*;

/** The existing owner's major-story ledger continues through all four first-escape acts. */
@Mod.EventBusSubscriber(modid="coraline")
public final class FirstEscapeStory {
    public static CompoundTag data(VisitData.Scene s){if(!s.loop.story.contains("FirstEscape"))s.loop.story.put("FirstEscape",new CompoundTag());return s.loop.story.getCompound("FirstEscape");}
    public static boolean engaged(VisitData.Scene s){return s!=null&&!AcceptedButtonEyes.active(s)&&s.loop.story.getCompound("FirstEscape").getBoolean("Started");}
    public static void recover(ServerPlayer p,VisitData.Scene s){if(engaged(s)&&p.getUUID().equals(s.owner)){data(s).putBoolean("Recover",true);data(s).remove("GrabApproach");FirstEscapeCarry.clear(data(s));OtherMotherLoop.changed(p);}}
    public static boolean sealed(MinecraftServer server,UUID owner){var s=VisitData.get(server).scenes.get(owner);return s!=null&&s.loop.story.getCompound("FirstEscape").getBoolean("OtherWorldSealed");}
    public static FirstEscapeTimeline.Phase phase(VisitData.Scene s){try{return FirstEscapeTimeline.Phase.valueOf(data(s).getString("Phase"));}catch(IllegalArgumentException invalid){return FIND_CANE;}}
    public static boolean begin(ServerPlayer p,VisitData.Scene s){
        if(engaged(s))return true;var old=BlackIsTraditional.data(s);if(!p.getUUID().equals(s.owner)||!old.getBoolean("Rejected")||!old.getBoolean("StudyRevealComplete")||BlackIsTraditional.phase(s)!=BlackIsTraditional.Phase.STUDY_REVEAL_COMPLETE||!OtherWorld.isOther(p.level())||Reconciliation.active(p))return false;
        for(var checkpoint:FirstEscapeTimeline.Phase.values()){String sound=FirstEscapeTimeline.sound(checkpoint);if(!sound.isEmpty()&&ShowRecordings.duration(sound)<=0)return false;}
        var r=SecretWorld.realm(p);if(r==null||r.house==null||r.doorPos==null||r.porch==null)return false;
        var n=data(s);n.putBoolean("Started",true);n.putBoolean("ChaseRewardPaid",s.loop.story.getBoolean("FirstEscapeRewardPaid"));n.putUUID("Token",UUID.randomUUID());n.putLong("House",r.house.asLong());n.putLong("LittleDoor",r.doorPos.asLong());n.putLong("SafePlayer",p.blockPosition().asLong());n.putLong("Prison",r.house.atY(Math.min(p.serverLevel().getMaxBuildHeight()-12,r.house.getY()+80)).asLong());
        p.closeContainer();p.stopRiding();p.setForcedPose(null);SecretSpies.suspend(p);VisitAudio.stop(p);n.putBoolean("RoomLocked",true);setRoomDoors(p,s,false);old.putBoolean("StudyLocked",false);setDoor(p,FirstEscapeScene.at(s,31,6,54),true);go(p,s,FIND_CANE);return true;
    }
    static void go(ServerPlayer actor,VisitData.Scene s,FirstEscapeTimeline.Phase next){
        if(!actor.getUUID().equals(s.owner)&&!com.coraline.multiplayer.StoryCoop.member(actor,s))return;
        var p=actor.getUUID().equals(s.owner)?actor:actor.server.getPlayerList().getPlayer(s.owner);if(p==null)return;
        if(next==MISSING_A)VisitAudio.stopMusic(p,"first_escape_music");
        ShowPlayback.transition(p,s,FirstEscapeTimeline.sound(next));var n=data(s);n.putString("Phase",next.name());n.putInt("Ticks",0);n.remove("AudioStarted");n.remove("AudioDone");n.remove("EscapeRecording");n.putUUID("Choice",UUID.randomUUID());n.remove("Blocker");
        refreshLittleDoorPassage(p,s);
        if(next==FIND_CANE||next==PRY_DOOR)prepareSealedRoom(p,s);
        if(next==COCOA_G){n.remove("MirrorThrowAt");n.remove("MirrorThrowFrame");n.remove("MirrorPrisonEntered");}
        if(next==ROOM_DARK){FirstEscapeScene.prepareRoom(p,s);FirstEscapeScene.roomProps(p,s);openLittleDoor(p,s,true);var m=mother(p,s);if(m!=null){m.setInvisible(true);var couch=FirstEscapeScene.prop(p,s,"BugCouch");if(couch!=null){m.stopRiding();m.getNavigation().stop();m.setPos(couch.position());m.startRiding(couch,true);}}}
        if(next==ROOM_TRANSFORMING){setRoomDoors(p,s,false);n.putBoolean("RoomLocked",true);}
        if(next==COCOA_E){n.putBoolean("ApologyRejected",true);n.putBoolean("CocoaMusic",false);}
        if(next==COCOA_G){grab(p,s);drag(p,s);}
        if(next==MASK_ANIMATION){n.remove("KnockdownAt");var w=wybie(p,s);if(w!=null){w.gameGesture(0,0);w.setPose(Pose.STANDING);w.setXRot(0);}}
        if(next==MIRROR_A)enterPrison(p,s);
        if(next==ESCAPE_A){p.stopRiding();n.putBoolean("RoomLocked",false);n.putInt("PullTicks",0);openLittleDoor(p,s,false);var head=FirstEscapeScene.prop(p,s,"PrisonHead");if(head!=null)head.frame(0,0,0);}
        if(next==MISSING_A){n.putBoolean("OtherWorldSealed",true);missing(p,s);brickDoor(p,s);}
        if(next==PUSH_WARDROBE){releaseGuide(p,s);openLittleDoor(p,s,false);}
        if(next==ESCAPE_COMPLETE)finishFirstEscape(p,s,false);
        OtherMotherLoop.changed(p);sync(p,s);
    }
    static void hint(ServerPlayer p,String message){if(p.tickCount%30==0)p.displayClientMessage(Component.literal(message),true);}
    public static boolean tick(ServerPlayer p,VisitData.Scene s){
        if(AcceptedButtonEyes.active(s))return false;
        if(!engaged(s)||!p.getUUID().equals(s.owner))return false;var n=data(s);if(!p.isAlive())return true;var state=phase(s);p.fallDistance=0;payChaseReward(p,s);
        refreshLittleDoorPassage(p,s);
        if(n.getBoolean("Recover")&&!recoverCheckpoint(p,s,state))return true;
        if(state==FIND_CANE||state==CANE_COLLECTED||state==PRY_DOOR)prepareSealedRoom(p,s);
        if(state==ESCAPE_COMPLETE){if(!n.getBoolean("Complete")){if(p.tickCount%20==0)finishFirstEscape(p,s,false);return true;}if(ParentRescueStory.started(s)){ParentRescueStory.tick(p,s);return true;}if(p.tickCount%40==0){missing(p,s);if(!MissingParentsCat.tunnelRestored(s))brickDoor(p,s);sync(p,s);}MissingParentsTea.tick(p,s);MissingParentsCat.tick(p,s);StoryTunnel.tickSource(p,s);return true;}
        if(!FirstEscapeTimeline.overworld(state)&&!OtherWorld.isOther(p.level())){
            var source=state==ESCAPE_TUNNEL?sourceDoor(p,s):null;
            if(source!=null&&p.level()==FamilyData.level(p.server,source.dimension)&&p.distanceToSqr(source.pos.getCenter())<=16*16)
                n.putBoolean("Recover",true);
            n.putString("Blocker","Story owner is outside the current scene; use safe recovery.");
            return true;
        }
        if(FirstEscapeTimeline.overworld(state)&&OtherWorld.isOther(p.level())){n.putString("Blocker","Waiting for Overworld return.");return true;}
        if(p.tickCount%20==0){BlackIsTraditional.retirePresentation(p,s);FirstEscapeScene.removeHallMirror(p,s);holdActors(p,s);if(state.ordinal()>=ROOM_DARK.ordinal()&&state.ordinal()<=ESCAPE_PUSH.ordinal())repairLittleDoor(p,s);if(state.ordinal()>=ROOM_DARK.ordinal()&&state.ordinal()<OPEN_LITTLE_DOOR.ordinal())openLittleDoor(p,s,state.ordinal()<MIRROR_A.ordinal());if(!FirstEscapeTimeline.overworld(state))LoopProps.hold(p.serverLevel(),FirstEscapeScene.room(s));}
        if(!FirstEscapeTimeline.prison(state)&&state!=ESCAPE_A&&state!=ESCAPE_TUNNEL&&!FirstEscapeTimeline.overworld(state)&&!((state==COCOA_G||state==MIRROR_THROW)&&n.getBoolean("MirrorPrisonEntered"))){
            if(!FirstEscapeScene.inHouseOrPorch(s,p.blockPosition())){verifiedTransfer(p,s,p.serverLevel(),safeHouseAnchor(s));hint(p,"The garden is beyond reach. Return to the house.");}
            else if(p.onGround()&&!p.isPassenger())n.putLong("SafePlayer",p.blockPosition().asLong());
        }
        int ticks=n.getInt("Ticks");String sound=FirstEscapeTimeline.sound(state);boolean done=false;
        if(!sound.isEmpty()){
            if(!n.getBoolean("AudioStarted")&&!readyForVoice(p,s,state))return true;
            done=recording(p,s,n,sound);ticks=n.getInt("Ticks");if(done)n.putBoolean("AudioDone",true);
        }else n.putInt("Ticks",++ticks);
        if(!FirstEscapeTimeline.overworld(state)&&state.ordinal()>=ROOM_DARK.ordinal()&&state.ordinal()<MIRROR_A.ordinal())FirstEscapeScene.roomProps(p,s);
        if(FirstEscapeTimeline.seated(state))holdSeat(p,s);
        if(state.ordinal()>=COCOA_B.ordinal()&&state.ordinal()<=COCOA_G.ordinal()){
            var box=FirstEscapeScene.prop(p,s,"BeetleBox");boolean taken=state.ordinal()>=COCOA_BITE.ordinal()||state==COCOA_B&&CocoaBeetlesChoreography.beetleTaken(ticks,ShowRecordings.duration(sound));
            if(box!=null){if(state==COCOA_B)moveProp(box,Vec3.atBottomCenterOf(FirstEscapeScene.room(s)).add(1,.88,2),.08);box.frame(taken?3:2,ticks,state==COCOA_B?CocoaBeetlesChoreography.trayOpen(ticks):1);}
        }
        if(state.ordinal()>=ESCAPE_A.ordinal()&&state.ordinal()<=ESCAPE_C.ordinal()){var w=wybie(p,s);if(w!=null&&n.getBoolean("Rescued")){if(n.getInt("RescueLandingRevision")<2){if(Math.abs(p.getY()-FirstEscapeScene.mirror(s).getY())>1){if(!rescueToHall(p,s,w))return true;}else n.putInt("RescueLandingRevision",2);}w.getLookControl().setLookAt(p,30,25);if(w.getNavigation().isDone()){var delta=p.position().subtract(w.position());float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));w.setYRot(net.minecraft.util.Mth.approachDegrees(w.getYRot(),yaw,15));w.setYBodyRot(w.getYRot());}}}
        if(java.util.EnumSet.of(PUSH_WARDROBE,PUSHING,ESCAPE_D,OPEN_LITTLE_DOOR,ESCAPE_E,ESCAPE_F,ESCAPE_G).contains(state)){var w=wybie(p,s);if(w!=null){w.getNavigation().stop();if(FirstEscapeVisualRules.helperWatchesEntrance(state.name(),ticks))lookEntrance(w,s);else w.getLookControl().setLookAt(p,40,30);}}
        switch(state){
            case FIND_CANE->{ensureCane(p,s);var cane=FirstEscapeScene.prop(p,s,"Cane");hint(p,cane!=null&&p.distanceToSqr(cane)<9?"Right-click the wooden cane to pick it up.":"Find the wooden cane on the front porch — through the front doors.");if(cane!=null&&p.tickCount%20==0)p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,cane.getX(),cane.getY()+.2,cane.getZ(),3,.12,.12,.12,.01);}
            case CANE_COLLECTED->{ensureTool(p,s,false);hint(p,"Use the cane to open the Little Door room.");}
            case PRY_DOOR->{p.swing(InteractionHand.MAIN_HAND,true);if(ticks%15==1)sfx(p,FirstEscapeScene.entrance(s),SoundEvents.WOODEN_DOOR_OPEN,.6F,.65F);if(ticks>=70){removeTool(p,s,false);go(p,s,ROOM_DARK);setRoomDoors(p,s,true);}}
            case ROOM_DARK->{if(FirstEscapeScene.inRoom(s,p.blockPosition())&&p.getX()<FirstEscapeScene.entrance(s).getX()-1.2)go(p,s,ROOM_TRANSFORMING);}
            case ROOM_TRANSFORMING->{animateRoom(p,s,ticks);if(ticks>=180)go(p,s,COCOA_A);}
            case COCOA_A->animateRoom(p,s,180+ticks);
            case COCOA_B->{var m=mother(p,s);if(m!=null)m.familyGesture(CocoaBeetlesChoreography.beetleTaken(ticks,ShowRecordings.duration(sound))?40:66,5);var lamp=FirstEscapeScene.prop(p,s,"BugLamp");if(lamp!=null)lamp.frame(4,ticks,1);}
            case CHAIR_CAPTURE->capture(p,s,ticks);
            case COCOA_BITE->{var m=mother(p,s);if(m!=null)m.familyGesture(40,5);if(ticks==20)sfx(p,m==null?p.blockPosition():m.blockPosition(),SoundEvents.GENERIC_EAT,.7F,.8F);if(ticks>=35){n.putBoolean("BeetleEaten",true);go(p,s,COCOA_C);}}
            case APOLOGY_CHOICE->hint(p,"Other Mother waits for your answer.");
            case COCOA_E->{var m=mother(p,s);if(m!=null){m.familyGesture(67,5);if(ticks>=40){m.stopRiding();walk(m,p.position(),.65);}}}
            case COCOA_F->{var m=mother(p,s);if(m!=null)m.familyGesture(41,5);}
            case COCOA_G->timedMirrorThrow(p,s,Math.max(ticks,ShowPlayback.audiblePosition(s)));
            case MIRROR_DRAG->drag(p,s);
            case MIRROR_THROW->{int frame=ticks+n.getInt("MirrorThrowFrame");mirrorThrow(p,s,frame);if(frame>=30&&enterPrison(p,s))go(p,s,MIRROR_A);}
            case MIRROR_A,MIRROR_B,MIRROR_C,MIRROR_D,MIRROR_E->prison(p,s,state,ticks);
            case MIRROR_RETRACT->{prison(p,s,state,ticks);if(ticks>=25)go(p,s,MIRROR_B);}
            case ESCAPE_A->{rescue(p,s,ticks);if(done&&(!n.getBoolean("Rescued")||!n.getBoolean("Punched"))){hint(p,"Punch the masked figure — he is trying to reach you.");done=false;}}
            case MASK_REMOVAL->hint(p,"Right-click the stranger's mask");
            case MASK_ANIMATION->{if(ticks>=25){n.putBoolean("MaskRemoved",true);go(p,s,ESCAPE_B);}}
            case ESCAPE_B->{var w=wybie(p,s);if(w!=null)w.gameGesture(ticks<40?42:43,5);}
            case STITCH_REMOVAL->hint(p,"Right-click to remove the stitching");
            case STITCH_ANIMATION->{if(ticks>=35){n.putBoolean("StitchesRemoved",true);go(p,s,ESCAPE_C);}}
            case ESCAPE_C->{var w=wybie(p,s);if(w!=null){w.gameGesture(ticks<20?44:45,5);if(ticks>=20)guide(p,s);}}
            case FOLLOW_WYBIE->{guide(p,s);var w=wybie(p,s);if(w!=null&&FirstEscapeGuide.arrived(p,s,w))go(p,s,PUSH_WARDROBE);}
            case PUSH_WARDROBE->hint(p,"Help Wybie push the wardrobe.");
            case PUSHING->{var w=wybie(p,s);if(w!=null)w.gameGesture(46,5);p.swing(InteractionHand.MAIN_HAND,true);if(ticks>=40)go(p,s,ESCAPE_D);}
            case ESCAPE_D->{var wardrobe=FirstEscapeScene.prop(p,s,"Wardrobe");if(wardrobe!=null)wardrobe.frame(3,ticks,Math.min(1,ticks/20F));var w=wybie(p,s);if(w!=null&&ticks>=40)lookEntrance(w,s);if(ticks==20){n.putBoolean("WardrobeFallen",true);sfx(p,BlockPos.of(n.getLong("LittleDoor")),SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,.8F,.65F);} }
            case OPEN_LITTLE_DOOR->hint(p,"Open the Little Door");
            case ESCAPE_E->{n.putBoolean("DecayedTunnel",true);ensureDecayedTunnel(p,s);}
            case ESCAPE_F->{var w=wybie(p,s);if(w!=null)w.gameGesture(ticks<40?47:48,5);if(ticks>=40)n.putBoolean("HandMissing",true);if(ticks>=40&&ticks<62&&w!=null)p.serverLevel().sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,Blocks.OAK_PLANKS.defaultBlockState()),w.getX(),w.getY()+1.1,w.getZ(),3,.14,.12,.14,.025);}
            case ESCAPE_G->{var w=wybie(p,s);if(w!=null)lookEntrance(w,s);}
            case ESCAPE_PUSH->pushIntoTunnel(p,s,ticks);
            case ESCAPE_TUNNEL->tunnel(p,s);
            case MISSING_A->{missing(p,s);if(ticks>=40)realWybie(p,s);}
            case WYBIE_GAZE->{var w=realWybie(p,s);if(w!=null&&gaze(p,w))go(p,s,MISSING_B);else hint(p,"Someone is waiting near the house.");}
            case MISSING_B->followReal(p,s);
            case SHOW_DOOR->{followReal(p,s);hint(p,"Show Wybie the Little Door.");var w=realWybie(p,s);var door=sourceDoor(p,s);if(w!=null&&door!=null&&p.distanceToSqr(door.pos.getCenter())<25&&w.distanceToSqr(door.pos.getCenter())<49)go(p,s,MISSING_C);}
            case MISSING_C->{var w=realWybie(p,s);if(w!=null)w.gameGesture(43,5);}
            case MISSING_D->{followReal(p,s);hint(p,"Find the doll.");}
            case THROW_SHOE->{ensureTool(p,s,true);shoe(p,s);hint(p,"Throw the shoe at Wybie.");}
            case MISSING_E->{var w=realWybie(p,s);if(w!=null)w.gameGesture(49,5);}
            case MISSING_F->chase(p,s,ticks);
            case MISSING_G,BICYCLE_DEPARTURE->departure(p,s);
            default->{}
        }
        if(done&&phase(s)==state){var next=state==COCOA_G&&n.getBoolean("MirrorPrisonEntered")?MIRROR_A:state==COCOA_G&&n.getBoolean("AtMirror")?MIRROR_THROW:FirstEscapeTimeline.after(state);if(next!=null)go(p,s,next);}
        if(p.tickCount%5==0)sync(p,s);if(p.tickCount%20==0)OtherMotherLoop.changed(p);return true;
    }
    private static boolean recording(ServerPlayer p,VisitData.Scene scene,CompoundTag story,String sound){
        if(ShowPlayback.debugSeeking(scene))return false;
        var recovery=story.getCompound("EscapeRecording");
        if(!sound.equals(recovery.getString("Sound"))){
            recovery=new CompoundTag();
            recovery.putString("Sound",sound);
            story.put("EscapeRecording",recovery);
        }
        if(RescueChapter.RecordingRecovery.exhausted(recovery)){
            int ticks=RescueChapter.RecordingRecovery.advance(recovery);
            story.putInt("Ticks",ticks);
            return RescueChapter.RecordingRecovery.dialogueWindowEnded(ticks,ShowRecordings.duration(sound));
        }
        long now=p.serverLevel().getGameTime();
        if(now<recovery.getLong("RetryAt"))return false;
        boolean running=story.getBoolean("AudioStarted")&&sound.equals(ShowPlayback.data(scene).getString("Sound"))
                &&ShowPlayback.ownsSession(p,scene)||ShowPlayback.start(p,scene,sound,false);
        if(running){
            story.putBoolean("AudioStarted",true);
            boolean done=ShowPlayback.tick(p,scene);
            var playback=ShowPlayback.data(scene);
            if(playback.getString("Blocker").isEmpty()&&!playback.getBoolean("SafetySkipped")){
                story.putInt("Ticks",ShowPlayback.position(scene));
                return done;
            }
        }
        ShowPlayback.stop(p,scene);
        story.remove("AudioStarted");
        RescueChapter.RecordingRecovery.failed(recovery);
        recovery.putLong("RetryAt",now+20);
        OtherMotherLoop.changed(p);
        return false;
    }

    static void cutsceneSafety(ServerPlayer p,VisitData.Scene s){
        var state=phase(s);boolean movement=java.util.EnumSet.of(PRY_DOOR,ROOM_TRANSFORMING,CHAIR_CAPTURE,COCOA_BITE,MIRROR_DRAG,MIRROR_THROW,MIRROR_RETRACT,MASK_ANIMATION,STITCH_ANIMATION,FOLLOW_WYBIE,PUSHING,ESCAPE_PUSH,BICYCLE_DEPARTURE).contains(state);
        String voice=FirstEscapeTimeline.sound(state);if(!movement&&(voice.isEmpty()||state==ESCAPE_A))return;
        if(!FirstEscapeTimeline.overworld(state)&&!OtherWorld.isOther(p.level())||FirstEscapeTimeline.overworld(state)&&OtherWorld.isOther(p.level()))return;
        int budget=voice.isEmpty()?600:StoryCutsceneWatchdog.voiceBudget(voice);
        if(!StoryCutsceneWatchdog.overdue(p,s,"first_escape",state.name(),budget))return;
        var n=data(s);ShowPlayback.safetyFinish(p,s,"First Escape checkpoint exceeded its safety window");
        switch(state){
            case PRY_DOOR->{removeTool(p,s,false);go(p,s,ROOM_DARK);setRoomDoors(p,s,true);}
            case ROOM_TRANSFORMING->{animateRoom(p,s,180);go(p,s,COCOA_A);}
            case CHAIR_CAPTURE->{if(!recoverChairCapture(p,s))StoryCutsceneWatchdog.retry(s,"first_escape");}
            case COCOA_BITE->{n.putBoolean("BeetleEaten",true);go(p,s,COCOA_C);}
            case COCOA_G,MIRROR_DRAG,MIRROR_THROW->{
                if((n.getBoolean("AtMirror")||n.getBoolean("MirrorPrisonEntered"))&&enterPrison(p,s))go(p,s,MIRROR_A);
                else StoryCutsceneWatchdog.retry(s,"first_escape");
            }
            case MIRROR_RETRACT->go(p,s,MIRROR_B);
            case MASK_ANIMATION->{n.putBoolean("MaskRemoved",true);go(p,s,ESCAPE_B);}
            case STITCH_ANIMATION->{n.putBoolean("StitchesRemoved",true);go(p,s,ESCAPE_C);}
            case FOLLOW_WYBIE->{
                var w=wybie(p,s);
                if(w!=null&&FirstEscapeGuide.arrived(p,s,w))go(p,s,PUSH_WARDROBE);
                else{hint(p,"Wybie is trying to reach the wardrobe. Clear his route.");StoryCutsceneWatchdog.retry(s,"first_escape");}
            }
            case PUSHING->go(p,s,ESCAPE_D);
            case ESCAPE_PUSH->{pushIntoTunnel(p,s,n.getInt("Ticks"));if(phase(s)==state)StoryCutsceneWatchdog.retry(s,"first_escape");}
            case BICYCLE_DEPARTURE->{var w=realWybie(p,s);if(w!=null&&CanonicalRealWybie.finishBlockedDeparture(p,s,w))go(p,s,ESCAPE_COMPLETE);else StoryCutsceneWatchdog.retry(s,"first_escape");}
            default->{var next=FirstEscapeTimeline.after(state);if(next!=null)go(p,s,next);}
        }
    }
    private static boolean readyForVoice(ServerPlayer p,VisitData.Scene s,FirstEscapeTimeline.Phase state){if(state==ESCAPE_A)return wybie(p,s)!=null;if(state==COCOA_A){animateRoom(p,s,180);var m=mother(p,s);var couch=FirstEscapeScene.prop(p,s,"BugCouch");return m!=null&&couch!=null&&m.getVehicle()==couch;}if(state==MISSING_B||state==MISSING_C||state==MISSING_D||state==MISSING_E||state==MISSING_F||state==MISSING_G)return realWybie(p,s)!=null;return true;}
    private static ParentEntity mother(ServerPlayer p,VisitData.Scene s){return s.mom!=null&&p.server.getLevel(OtherWorld.KEY).getEntity(s.mom) instanceof ParentEntity m?m:null;}
    private static void walk(Mob m,Vec3 at,double speed){m.setNoAi(false);m.setNoGravity(false);m.noPhysics=false;if(m instanceof ParentEntity parent)ScenePlacement.prepareWalker(parent);else m.setMaxUpStep(1.05F);if(m.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation nav){nav.setCanOpenDoors(true);nav.setCanPassDoors(true);}if(m.tickCount%10==0||m.getNavigation().isDone())m.getNavigation().moveTo(at.x,at.y,at.z,speed);}
    private static void sfx(ServerPlayer p,BlockPos at,SoundEvent sound,float volume,float pitch){p.serverLevel().playSound(null,at,sound,SoundSource.BLOCKS,volume,pitch);}
    static void safeTransfer(ServerPlayer p,ServerLevel level,BlockPos at){if(level==null)return;level.getChunkAt(at);var safe=OtherWorld.safePlayer(level,at);p.fallDistance=0;p.teleportTo(level,safe.getX()+.5,safe.getY(),safe.getZ()+.5,p.getYRot(),p.getXRot());}
    static void setDoor(ServerPlayer p,BlockPos at,boolean open){var l=p.server.getLevel(OtherWorld.KEY);var s=OtherMotherLoop.scene(p);if(l!=null&&s!=null&&l.getBlockState(at).getBlock() instanceof DoorBlock){for(var cell:List.of(at,at.above()))FirstEscapeScene.place(p,s,cell,l.getBlockState(cell));AuthoritativeHouseDoors.setOpen(l,at,open);for(var cell:List.of(at,at.above()))FirstEscapeScene.place(p,s,cell,l.getBlockState(cell));}}
    private static void setRoomDoors(ServerPlayer p,VisitData.Scene s,boolean open){for(int z:new int[]{41,42})setDoor(p,FirstEscapeScene.at(s,31,6,z),open);}
    private static BlockPos safeHouseAnchor(VisitData.Scene scene){
        var state=data(scene);
        if(state.contains("SafePlayer",Tag.TAG_LONG)){
            var saved=BlockPos.of(state.getLong("SafePlayer"));
            if(FirstEscapeScene.inHouseOrPorch(scene,saved))return saved;
        }
        return FirstEscapeScene.entrance(scene);
    }
    private static boolean verifiedTransfer(ServerPlayer player,VisitData.Scene scene,ServerLevel level,BlockPos anchor){
        if(level==null)return false;
        var target=RescueEpilogue.nearReturn(level,player,Pose.STANDING,anchor,6);
        if(target==null)return false;
        var state=data(scene);
        state.putBoolean("InternalTransfer",true);
        try{
            player.stopRiding();
            player.setForcedPose(null);
            player.teleportTo(level,target.x,target.y,target.z,player.getYRot(),player.getXRot());
        }finally{state.remove("InternalTransfer");}
        if(player.serverLevel()!=level||player.position().distanceToSqr(target)>.25)return false;
        player.fallDistance=0;
        return true;
    }
    private static boolean recoverCheckpoint(ServerPlayer p,VisitData.Scene s,FirstEscapeTimeline.Phase state){
        var n=data(s);
        var level=p.server.getLevel(OtherWorld.KEY);
        boolean prison=FirstEscapeTimeline.prison(state)||state==ESCAPE_A
                ||(state==COCOA_G||state==MIRROR_THROW)&&n.getBoolean("MirrorPrisonEntered");
        if(prison){
            if(level==null)return false;
            FirstEscapeScene.preparePrison(p,s);
            if(!verifiedTransfer(p,s,level,FirstEscapeScene.prison(s)))return false;
        }else if(!FirstEscapeTimeline.overworld(state)&&state!=ESCAPE_TUNNEL&&p.serverLevel()!=level
                &&!verifiedTransfer(p,s,level,safeHouseAnchor(s)))return false;
        if(state==ESCAPE_TUNNEL&&level==null)return false;
        n.remove("Recover");n.remove("AudioStarted");n.putInt("Ticks",0);ShowPlayback.stop(p,s);
        n.putBoolean("InternalTransfer",true);p.stopRiding();n.remove("InternalTransfer");p.setForcedPose(null);
        boolean inThrowPrison=(state==COCOA_G||state==MIRROR_THROW)&&n.getBoolean("MirrorPrisonEntered");
        if(state==COCOA_G||state==MIRROR_THROW){
            if(!inThrowPrison){n.remove("MirrorThrowAt");n.remove("MirrorThrowFrame");var m=mother(p,s);if(m!=null)m.setInvisible(false);}
        }
        if(state==ESCAPE_TUNNEL){ensureDecayedTunnel(p,s);if(!spawnInEscapeTunnel(p,s,Math.max(1,Math.min(19,n.getDouble("TunnelProgress"))))){n.putBoolean("Recover",true);return false;}}
        if(state==ESCAPE_A)n.remove("Rescued");
        return true;
    }
    private static void prepareSealedRoom(ServerPlayer p,VisitData.Scene s){
        // Prepare while the owner is fetching the porch cane, never after the
        // sightline opens. Also covers saves loaded at the old cane checkpoint.
        if(data(s).getBoolean("RoomBuilt")&&data(s).getInt("WallpaperRevision")>=2)return;
        setRoomDoors(p,s,false);FirstEscapeScene.prepareRoom(p,s);FirstEscapeScene.roomProps(p,s);openLittleDoor(p,s,true);
    }
    private static DoorData.Door sourceDoor(ServerPlayer p,VisitData.Scene s){var door=DoorData.get(p.server).doors.get(s.door);return door!=null&&s.owner.equals(door.owner)?door:null;}
    private static void holdActors(ServerPlayer p,VisitData.Scene s){var n=data(s);for(String key:new ArrayList<>(n.getAllKeys()))if(key.endsWith("Position")){BlockPos at=BlockPos.of(n.getLong(key));var l=FirstEscapeTimeline.overworld(phase(s))?p.serverLevel():p.server.getLevel(OtherWorld.KEY);if(l!=null)LoopProps.hold(l,at);}var m=mother(p,s);if(m!=null){n.putLong("MotherPosition",m.blockPosition().asLong());LoopProps.hold((ServerLevel)m.level(),m.blockPosition());} }
    private static void ensureCane(ServerPlayer p,VisitData.Scene s){var at=FirstEscapeScene.porch(s).offset(-2,0,1);var l=p.serverLevel();if(!l.getBlockState(at.below()).isFaceSturdy(l,at.below(),Direction.UP)){data(s).putString("Blocker","Porch cane support is missing; repair the porch before pickup.");return;}FirstEscapeScene.ensure(p,s,"Cane",CinematicPropEntity.STORY_CANE,0,l,Vec3.atBottomCenterOf(at).add(0,.025,0));var cane=FirstEscapeScene.prop(p,s,"Cane");if(cane!=null&&p.distanceToSqr(cane)<9)hint(p,"Right-click to pick up cane");}
    private static ItemStack tool(VisitData.Scene s,boolean shoe){var stack=new ItemStack(shoe?FirstEscapeItems.SHOE.get():FirstEscapeItems.CANE.get());stack.getOrCreateTag().putUUID("StoryOwner",s.owner);stack.getTag().putUUID("StoryToken",data(s).getUUID("Token"));return stack;}
    public static boolean validTool(VisitData.Scene s,ItemStack stack,boolean shoe){var tag=stack.getTag();return engaged(s)&&stack.is(shoe?FirstEscapeItems.SHOE.get():FirstEscapeItems.CANE.get())&&tag!=null&&tag.hasUUID("StoryOwner")&&s.owner.equals(tag.getUUID("StoryOwner"))&&tag.hasUUID("StoryToken")&&data(s).hasUUID("Token")&&data(s).getUUID("Token").equals(tag.getUUID("StoryToken"));}
    private static boolean giveTool(ServerPlayer p,VisitData.Scene s,boolean shoe){for(int i=0;i<p.getInventory().getContainerSize();i++)if(validTool(s,p.getInventory().getItem(i),shoe))return com.coraline.multiplayer.QuestItemRecovery.confirmHeld(p,s,shoe?"shoe":"cane");if(!com.coraline.multiplayer.QuestItemRecovery.reserveTool(p,s,shoe))return false;var stack=tool(s,shoe);SecretTransactions.give(p,stack);boolean added=stack.isEmpty();if(added){p.getInventory().setChanged();OtherMotherLoop.changed(p);return com.coraline.multiplayer.QuestItemRecovery.confirmHeld(p,s,shoe?"shoe":"cane");}else hint(p,"Make one inventory slot available for the story item.");return false;}
    private static void ensureTool(ServerPlayer p,VisitData.Scene s,boolean shoe){if(shoe&&data(s).hasUUID("ShoeFlight"))return;giveTool(p,s,shoe);}
    private static void removeTool(ServerPlayer p,VisitData.Scene s,boolean shoe){for(int slot=0;slot<p.getInventory().getContainerSize();slot++)if(validTool(s,p.getInventory().getItem(slot),shoe))p.getInventory().setItem(slot,ItemStack.EMPTY);p.getInventory().setChanged();}
    private static void cleanupPassage(ServerPlayer p,VisitData.Scene s){var l=p.server.getLevel(OtherWorld.KEY);if(l==null)return;var at=BlockPos.of(data(s).getLong("LittleDoor"));l.getChunkAt(at);if(l.getBlockEntity(at) instanceof DoorBlockEntity door)TunnelPassage.restore(l,door);var state=l.getBlockState(at);if(state.is(com.coraline.registry.ModBlocks.TINY_DOOR.get()))l.setBlock(at,state.setValue(com.coraline.block.TinyDoorBlock.OPEN,false).setValue(com.coraline.block.TinyDoorBlock.PORTAL,false),3);}
    public static boolean interact(ServerPlayer p,CinematicPropEntity prop){var s=OtherMotherLoop.scene(p);if(!engaged(s)||!prop.owned(s)||p.distanceToSqr(prop)>16)return false;if(MissingParentsCat.interact(p,prop))return true;var phase=phase(s);var n=data(s);
        if(prop.kind()==CinematicPropEntity.STORY_CANE&&phase==FIND_CANE){if(giveTool(p,s,false)){n.putBoolean("CaneCollected",true);prop.discard();n.remove("Cane");go(p,s,CANE_COLLECTED);sfx(p,p.blockPosition(),SoundEvents.ITEM_PICKUP,.7F,1);}return true;}
        if(prop.kind()==CinematicPropEntity.ROACH_WARDROBE&&phase==PUSH_WARDROBE){var w=wybie(p,s);if(w!=null&&w.distanceToSqr(prop)<36)go(p,s,PUSHING);else hint(p,"Wait for Wybie to reach the wardrobe.");return true;}
        if(prop.kind()==CinematicPropEntity.STORY_SHOE&&phase==THROW_SHOE){if(giveTool(p,s,true)){prop.discard();n.remove("ShoeFlight");}return true;}return !ParentRescueStory.started(s)&&phase!=ESCAPE_COMPLETE
            &&prop.owner.equals(p.getUUID())&&prop.kind()>=CinematicPropEntity.STORY_CANE
            &&prop.kind()<=CinematicPropEntity.STORY_SHOE;
    }
    public static boolean block(ServerPlayer p,BlockPos at){var s=OtherMotherLoop.scene(p);if(ParentRescueStory.started(s))return ParentRescueStory.block(p,at);if(MissingParentsCat.block(p,at))return true;if(!engaged(s))return false;var n=data(s);var state=phase(s);var block=p.serverLevel().getBlockState(at);
        if(FirstEscapeTimeline.overworld(state)){if(block.is(com.coraline.registry.ModBlocks.TINY_DOOR.get()))return false;return state!=ESCAPE_COMPLETE&&p.serverLevel().getBlockEntity(at) instanceof net.minecraft.world.Container;}
        if(block.getBlock() instanceof DoorBlock){var lower=block.getValue(DoorBlock.HALF)==DoubleBlockHalf.UPPER?at.below():at;boolean roomDoor=lower.equals(FirstEscapeScene.entrance(s))||lower.equals(FirstEscapeScene.at(s,31,6,42));
            if(roomDoor&&state==CANE_COLLECTED){if(validTool(s,p.getMainHandItem(),false)||validTool(s,p.getOffhandItem(),false)){p.closeContainer();go(p,s,PRY_DOOR);}else hint(p,"Hold the cane to pry open the door.");return true;}
            if(roomDoor){boolean locked=n.getBoolean("RoomLocked")&&state.ordinal()<ESCAPE_A.ordinal();if(locked)p.displayClientMessage(Component.literal("The door is locked."),true);return locked;}
            // The study and the two front doors remain usable for the cane, not every exit.
            if(lower.equals(FirstEscapeScene.at(s,31,6,54))||lower.equals(FirstEscapeScene.at(s,33,6,12))||lower.equals(FirstEscapeScene.at(s,34,6,12)))return false;p.displayClientMessage(Component.literal("The door is locked."),true);return true;
        }
        if(block.is(com.coraline.registry.ModBlocks.TINY_DOOR.get())){if(at.asLong()==n.getLong("LittleDoor")){if(state==OPEN_LITTLE_DOOR){if(openLittleDoor(p,s,true))go(p,s,ESCAPE_E);}else if(state.ordinal()>=ESCAPE_E.ordinal()&&state.ordinal()<=ESCAPE_PUSH.ordinal())openLittleDoor(p,s,true);else hint(p,"The wardrobe blocks the Little Door. Escape with the masked stranger first.");}return true;}
        return block.getBlock() instanceof BedBlock||p.serverLevel().getBlockEntity(at) instanceof net.minecraft.world.Container;
    }
    static boolean repairLittleDoor(ServerPlayer p,VisitData.Scene s){
        var n=data(s);var l=p.server.getLevel(OtherWorld.KEY);if(l==null||!n.contains("LittleDoor"))return false;
        var at=BlockPos.of(n.getLong("LittleDoor"));LoopProps.hold(l,at);var state=l.getBlockState(at);
        if(state.is(com.coraline.registry.ModBlocks.TINY_DOOR.get())){
            n.putString("LittleDoorFacing",state.getValue(com.coraline.block.TinyDoorBlock.FACING).getName());
            if(l.getBlockEntity(at) instanceof DoorBlockEntity door&&door.doorId==null){door.doorId=s.door;door.setChanged();}
            if(n.getBoolean("LittleDoorRepaired"))retireWashedDoorDrops(l,at,s);
            return true;
        }
        // Repair only the recorded story doorway and only air/flowing-liquid
        // damage. Never overwrite a player's solid block, container or another
        // door, and never mint a new owner/realm identity.
        if(!state.isAir()&&!(state.getBlock() instanceof LiquidBlock)||l.getBlockEntity(at)!=null)return false;
        var facing=Direction.byName(n.getString("LittleDoorFacing"));if(facing==null||!facing.getAxis().isHorizontal())facing=Direction.EAST;
        state=com.coraline.registry.ModBlocks.TINY_DOOR.get().defaultBlockState().setValue(com.coraline.block.TinyDoorBlock.FACING,facing);
        l.setBlock(at,state,3);if(!(l.getBlockEntity(at) instanceof DoorBlockEntity door))return false;
        door.doorId=s.door;door.setChanged();
        // A washed-away legacy door may have dropped this exact identity. Do
        // not sweep unrelated player drops or any other Little Door's item.
        retireWashedDoorDrops(l,at,s);
        n.putBoolean("LittleDoorRepaired",true);OtherMotherLoop.changed(p);return true;
    }
    private static void refreshLittleDoorPassage(ServerPlayer p,VisitData.Scene s){
        var level=p.server.getLevel(OtherWorld.KEY);var n=data(s);
        if(level==null||!n.contains("LittleDoor")||!(level.getBlockEntity(BlockPos.of(n.getLong("LittleDoor"))) instanceof DoorBlockEntity door))return;
        door.refreshStoryPassageBlock();
        // Recover a save made after sprinting past the old moving wardrobe.
        // Ordinary room movement and the later scripted escape are untouched.
        if(door.storyPassageBlocked&&p.serverLevel()==level&&!p.isPassenger()&&TunnelPassage.contains(p,door)){
            var facing=door.getBlockState().getValue(com.coraline.block.TinyDoorBlock.FACING);
            var safe=AcceptedButtonEyesWorld.landing(level,door.getBlockPos().relative(facing,3));
            if(safe!=null&&Vec3.atBottomCenterOf(safe).subtract(Vec3.atBottomCenterOf(door.getBlockPos())).dot(Vec3.atLowerCornerOf(facing.getNormal()))>=1){
                p.getPersistentData().remove("CoralineAutoCrawl");p.setForcedPose(null);p.setDeltaMovement(Vec3.ZERO);
                p.teleportTo(level,safe.getX()+.5,safe.getY(),safe.getZ()+.5,p.getYRot(),p.getXRot());
            }
        }
    }
    private static void retireWashedDoorDrops(ServerLevel level,BlockPos at,VisitData.Scene s){
        // Entity sections can become accessible a tick after their blocks. The
        // existing-door branch retries this exact-identity cleanup on that tick.
        for(var drop:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(at).inflate(1.5),e->e.getItem().is(com.coraline.registry.ModItems.TINY_DOOR.get())&&s.door.equals(DoorData.id(e.getItem()))))drop.discard();
    }
private static boolean openLittleDoor(ServerPlayer p,VisitData.Scene s,boolean open){var l=p.server.getLevel(OtherWorld.KEY);if(!repairLittleDoor(p,s))return false;var at=BlockPos.of(data(s).getLong("LittleDoor"));var state=l.getBlockState(at);boolean changed=state.getValue(com.coraline.block.TinyDoorBlock.OPEN)!=open;l.setBlock(at,state.setValue(com.coraline.block.TinyDoorBlock.OPEN,open).setValue(com.coraline.block.TinyDoorBlock.PORTAL,open),3);if(changed)l.playSound(null,at,open?SoundEvents.WOODEN_DOOR_OPEN:SoundEvents.WOODEN_DOOR_CLOSE,SoundSource.BLOCKS,.8F,1);if(l.getBlockEntity(at) instanceof DoorBlockEntity door){if(open){boolean built=TunnelPassage.ensure(l,door);if(!built)hint(p,"Clear the space behind the Little Door; its passage cannot form yet.");return built;}else TunnelPassage.restore(l,door);}return !open;}
    private static void animateRoom(ServerPlayer p,VisitData.Scene s,int time){var wardrobe=FirstEscapeScene.prop(p,s,"Wardrobe");var n=data(s);if(wardrobe!=null){var start=Vec3.atBottomCenterOf(BlockPos.of(n.getLong("LittleDoor"))).add(2,0,3);var end=Vec3.atBottomCenterOf(BlockPos.of(n.getLong("LittleDoor"))).add(1.1,0,0);wardrobe.setPos(start.lerp(end,CocoaBeetlesChoreography.wardrobe(time)));wardrobe.frame(time<CocoaBeetlesChoreography.WARDROBE_BLOCKED?1:0,time,CocoaBeetlesChoreography.ramp(time,65,15));}
        if(wardrobe!=null&&CocoaBeetlesChoreography.music(time,n.getBoolean("ApologyRejected")))n.putBoolean("CocoaMusic",true);
        FirstEscapeScene.light(p,s,FirstEscapeScene.room(s).above(4),time<130?0:Math.min(13,(time-130)/3));
        var lamp=FirstEscapeScene.prop(p,s,"BugLamp");if(lamp!=null)FirstEscapeScene.followLight(p,s,"MantisLamp",lamp.position().add(.65,2.05,0),time<80?0:Math.min(11,(time-80)/4));
        FirstEscapeScene.light(p,s,FirstEscapeScene.room(s).offset(1,1,2),time<110?0:Math.min(9,(time-110)/7));
        for(int index=0;index<2;index++)FirstEscapeScene.light(p,s,FirstEscapeScene.room(s).offset(index==0?-8:8,3,-3),Math.round(11*CocoaBeetlesChoreography.sconce(time,index)));
        FirstEscapeScene.light(p,s,FirstEscapeScene.room(s).offset(-2,1,5),time<120?0:Math.min(12,(time-120)/4));
        String[] keys={"BugLamp","BugChair","BugCouch","BugTable","BeetleBox","Chandelier","Parlor"};for(int i=0;i<keys.length;i++){var prop=FirstEscapeScene.prop(p,s,keys[i]);if(prop!=null)prop.frame(1,time,CocoaBeetlesChoreography.reveal(time,i));}
        var couch=FirstEscapeScene.prop(p,s,"BugCouch");var mother=mother(p,s);if(mother!=null&&couch!=null){mother.setInvisible(time<115);mother.getNavigation().stop();if(!mother.isPassenger()){if(mother.distanceToSqr(couch)>2)walk(mother,couch.position(),1);else mother.startRiding(couch,true);}mother.setYRot(couch.getYRot()+180);mother.setYBodyRot(mother.getYRot());mother.lookAtStoryPlayer(p);}
    }
    private static boolean moveProp(CinematicPropEntity prop,Vec3 target,double speed){var delta=target.subtract(prop.position());if(delta.length()<.08)return true;var next=prop.position().add(delta.normalize().scale(Math.min(speed,delta.length())));var box=prop.getBoundingBox().move(next.subtract(prop.position()));if(prop.level().getBlockCollisions(prop,box.deflate(.05)).iterator().hasNext())return false;prop.setPos(next);return prop.position().distanceToSqr(target)<.02;}
    private static void capture(ServerPlayer p,VisitData.Scene s,int time){var chair=FirstEscapeScene.prop(p,s,"BugChair");if(chair==null)return;var n=data(s);
        var center=Vec3.atBottomCenterOf(FirstEscapeScene.room(s));var lamp=FirstEscapeScene.prop(p,s,"BugLamp");if(lamp!=null){moveProp(lamp,center.add(-1.5,0,1.2),.065);lamp.setYRot(18);lamp.frame(2,time,1);if(time%5==0)FirstEscapeScene.followLight(p,s,"MantisLamp",lamp.position().add(.65,2.05,0),11);}
        var tray=FirstEscapeScene.prop(p,s,"BeetleBox");if(tray!=null){moveProp(tray,center.add(-.85,1.15,1.5),.065);tray.frame(1,time,1);}
        var before=chair.position();var route=n.getCompound("ChairFloorRoute");n.put("ChairFloorRoute",route);
        if(!p.isPassenger()){
            String purpose=n.getBoolean("ChairBehindReached")?"capture":"behind";
            var behind=p.getLookAngle().multiply(-1,0,-1).normalize();var desired=purpose.equals("behind")?p.position().add(behind.scale(1.2)):p.position();
            if(!purpose.equals(n.getString("ChairGoalPurpose"))||!n.contains("ChairGoal")||BlockPos.of(n.getLong("ChairPlayerAt")).distSqr(p.blockPosition())>1){
                var goal=CinematicFloorRoute.nearest(p,s,chair,desired);if(goal!=null){n.putLong("ChairGoal",goal.asLong());n.putLong("ChairPlayerAt",p.blockPosition().asLong());n.putString("ChairGoalPurpose",purpose);route.remove("Nodes");}
            }
            boolean reached=n.contains("ChairGoal")&&CinematicFloorRoute.move(p,s,chair,route,BlockPos.of(n.getLong("ChairGoal")),.09);
            if(purpose.equals("behind")&&reached){n.putBoolean("ChairBehindReached",true);n.remove("ChairGoal");}
            else if(purpose.equals("capture")&&chair.distanceToSqr(p)<2.25&&p.startRiding(chair,true)){n.putBoolean("Captured",true);n.remove("ChairGoal");}
        }else if(p.getVehicle()==chair){
            chair.setYRot(90);if(!n.getString("ChairGoalPurpose").equals("seated")){var goal=CinematicFloorRoute.nearest(p,s,chair,center.add(-1.4,0,2));if(goal!=null){n.putLong("ChairGoal",goal.asLong());n.putString("ChairGoalPurpose","seated");route.remove("Nodes");}}
            if(n.contains("ChairGoal")&&CinematicFloorRoute.move(p,s,chair,route,BlockPos.of(n.getLong("ChairGoal")),.09)){go(p,s,COCOA_B);return;}
        }
        int stalled=before.distanceToSqr(chair.position())<.00001?n.getInt("ChairStallTicks")+1:0;n.putInt("ChairStallTicks",stalled);
        if(stalled>=60||time>=400)recoverChairCapture(p,s);chair.frame(2,time,1);
    }
    /** Bounded same-chair recovery. Never mounts an obstructed/occupied seat or
     * sets Captured merely because a timer elapsed. */
    static boolean recoverChairCapture(ServerPlayer p,VisitData.Scene s){
        var chair=FirstEscapeScene.prop(p,s,"BugChair");if(chair==null)return false;var n=data(s);var center=FirstEscapeScene.room(s).offset(-1,0,2);chair.setYRot(90);
        for(int radius=0;radius<=5;radius++)for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            if(Math.max(Math.abs(x),Math.abs(z))!=radius)continue;var at=center.offset(x,0,z);if(!CinematicFloorRoute.fits(p,s,chair,at))continue;var feet=PartyFloor.at(p,at);
            var rider=new AABB(feet.x+.3,feet.y+.2,feet.z-.31,feet.x+.91,feet.y+2.1,feet.z+.31);if(p.serverLevel().getBlockCollisions(null,rider).iterator().hasNext())continue;
            if(chair.isVehicle()&&p.getVehicle()!=chair)continue;p.stopRiding();chair.setPos(feet);
            if(p.startRiding(chair,true)){n.putBoolean("Captured",true);n.putString("CaptureRecovery","Same chair moved to a clear parlor seat after a blocked route");go(p,s,COCOA_B);return true;}
        }
        return false;
    }
    private static void holdSeat(ServerPlayer p,VisitData.Scene s){if(p.isSpectator()||data(s).getBoolean("CaptureBypassed"))return;var chair=FirstEscapeScene.prop(p,s,"BugChair");if(chair!=null&&phase(s).ordinal()<=COCOA_G.ordinal()&&!data(s).getBoolean("Grabbed")&&p.getVehicle()!=chair)p.startRiding(chair,true);}
    private static void grab(ServerPlayer p,VisitData.Scene s){
        var m=mother(p,s);if(m==null)return;var n=data(s);m.familyGesture(42,5);
        var carrier=FirstEscapeScene.prop(p,s,"Carrier");
        if(n.getBoolean("Grabbed")&&carrier!=null){m.getNavigation().stop();m.setDeltaMovement(Vec3.ZERO);holdGrip(p,m,carrier);return;}
        m.stopRiding();var chair=FirstEscapeScene.prop(p,s,"BugChair");var target=chair==null?p.position():chair.position();
        // Approach a reachable cell beside the seat, not its occupied centre. Vanilla
        // navigation can otherwise walk forever against carpet/furniture at the grab cue.
        if(m.distanceToSqr(p.position())>3.25){
            BlockPos goal=n.contains("GrabApproach")?BlockPos.of(n.getLong("GrabApproach")):null;double best=Double.MAX_VALUE;
            if(goal!=null&&!FirstEscapeCarry.fits(p,goal)){n.remove("GrabApproach");goal=null;}
            if(goal==null)for(var direction:Direction.Plane.HORIZONTAL){var at=BlockPos.containing(target).relative(direction);
                if(!FirstEscapeCarry.fits(p,at))continue;var route=FirstEscapeCarry.route(p,m.blockPosition(),at);
                if(route.isEmpty()&&!m.blockPosition().equals(at))continue;double score=m.distanceToSqr(Vec3.atBottomCenterOf(at));if(score<best){best=score;goal=at;}
            }
            if(goal==null){m.getNavigation().stop();m.setDeltaMovement(Vec3.ZERO);n.putString("Blocker","No clear approach to the captured chair. Use /coraline recover to retry this checkpoint.");return;}
            n.putLong("GrabApproach",goal.asLong());FirstEscapeCarry.move(p,s,m,goal);return;
        }
        FirstEscapeCarry.clear(n);m.getNavigation().stop();m.setDeltaMovement(Vec3.ZERO);
        carrier=FirstEscapeScene.ensure(p,s,"Carrier",CinematicPropEntity.GRAB_CARRIER,0,p.serverLevel(),m.position().add(0,.65,0));
        if(carrier!=null){n.putBoolean("InternalTransfer",true);p.stopRiding();boolean mounted=p.startRiding(carrier,true);n.remove("InternalTransfer");if(mounted){n.putBoolean("Grabbed",true);m.getNavigation().stop();m.setDeltaMovement(Vec3.ZERO);holdGrip(p,m,carrier);}}
    }
    private static void holdGrip(ServerPlayer p,ParentEntity m,CinematicPropEntity carrier){
        p.setForcedPose(Pose.SWIMMING);p.setPose(Pose.SWIMMING);
        // A .38 m grip put the first-person camera inside the long torso. Keep
        // the whole captive outside it, including at corners; never fall back
        // to the captor's body center when the forward grip is obstructed.
        var forward=m.getLookAngle().multiply(1,0,1).normalize();if(forward.lengthSqr()<.5)forward=Vec3.directionFromRotation(0,m.getYRot());var side=new Vec3(-forward.z,0,forward.x);Vec3 at=null;
        for(var offset:new Vec3[]{forward.scale(.95),forward.scale(.80),side.scale(.80),side.scale(-.80),forward.scale(-.80)}){var candidate=m.position().add(offset).add(0,.85,0);var captive=p.getDimensions(Pose.SWIMMING).makeBoundingBox(candidate.add(0,carrier.getPassengersRidingOffset()+p.getMyRidingOffset(),0));if(!p.serverLevel().getBlockCollisions(p,captive).iterator().hasNext()){at=candidate;break;}}
        if(at==null)return; // Retain the last safe grip through a narrow corner.
        carrier.setPos(at);if(p.getVehicle()!=carrier)p.startRiding(carrier,true);
        // Move the captive, not their view. Looking at the tall captor's head
        // every tick forced a steep upward pitch and prevented free look.
        p.setPos(carrier.position().add(0,carrier.getPassengersRidingOffset()+p.getMyRidingOffset(),0));
    }
    static void drag(ServerPlayer p,VisitData.Scene s){
        var n=data(s);if(!n.getBoolean("Grabbed"))grab(p,s);if(!n.getBoolean("Grabbed"))return;
        setRoomDoors(p,s,true);var m=mother(p,s);if(m==null)return;
        var target=n.getBoolean("DragInHall")?FirstEscapeScene.mirror(s).north(1):FirstEscapeScene.entrance(s).east(2);
        if(!n.getBoolean("AtMirror")&&FirstEscapeCarry.move(p,s,m,target)){if(n.getBoolean("DragInHall")){n.putBoolean("AtMirror",true);if(phase(s)==MIRROR_DRAG)go(p,s,MIRROR_THROW);}else n.putBoolean("DragInHall",true);}
        var carrier=FirstEscapeScene.prop(p,s,"Carrier");if(carrier!=null)holdGrip(p,m,carrier);
    }
    static void timedMirrorThrow(ServerPlayer p,VisitData.Scene s,int ticks){
        var n=data(s);if(n.getBoolean("MirrorPrisonEntered"))return;
        if(!n.contains("MirrorThrowAt")){
            drag(p,s);
            if(!n.getBoolean("AtMirror")||!ShowPlayback.started(s)||ticks<FirstEscapeVisualRules.mirrorThrowCue(ShowRecordings.duration("cocoa_beetles_g")))return;
            n.putInt("MirrorThrowAt",ticks);
        }
        int frame=ticks-n.getInt("MirrorThrowAt")+1;n.putInt("MirrorThrowFrame",frame);mirrorThrow(p,s,frame);
        // G's original playback session stays alive in the prison. Only its
        // genuine end acknowledgement advances to A; no voice tail is cut off.
        if(frame>=30)enterPrison(p,s);
    }
    private static boolean enterPrison(ServerPlayer p,VisitData.Scene s){
        var n=data(s);var c=FirstEscapeScene.prison(s);var l=p.server.getLevel(OtherWorld.KEY);
        if(n.getBoolean("MirrorPrisonEntered")&&p.level()==l&&p.distanceToSqr(c.getCenter())<100)return true;
        if(l==null)return false;
        FirstEscapeScene.preparePrison(p,s);var m=mother(p,s);if(m!=null){m.setNoGravity(false);m.setInvisible(true);m.getNavigation().stop();}
        n.putBoolean("InternalTransfer",true);p.stopRiding();p.setForcedPose(null);p.setPose(Pose.STANDING);n.remove("InternalTransfer");
        if(!verifiedTransfer(p,s,l,c)){n.putString("Blocker","The mirror prison landing is obstructed.");return false;}
        retireGrab(p,s,"Carrier");p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,Vec3.atBottomCenterOf(c.north(4)).add(0,1.7,.2));n.putBoolean("MirrorPrisonEntered",true);prison(p,s,MIRROR_A,0);sync(p,s);return true;
    }
    private static void mirrorThrow(ServerPlayer p,VisitData.Scene s,int ticks){var carrier=FirstEscapeScene.prop(p,s,"Carrier");if(carrier!=null)moveProp(carrier,FirstEscapeScene.mirror(s).getCenter().add(0,.4,.8),.13);var mirror=FirstEscapeScene.prop(p,s,"HallMirror");if(mirror!=null)mirror.frame(1,ticks,Math.min(1,ticks/20F));if(ticks==1)sfx(p,FirstEscapeScene.mirror(s),SoundEvents.GLASS_BREAK,.6F,.6F);}
    static void acceptedPrisonFrame(ServerPlayer p,VisitData.Scene s,FirstEscapeTimeline.Phase state,int ticks){prison(p,s,state,ticks);}
    static void acceptedPrisonStill(ServerPlayer p,VisitData.Scene s){prison(p,s,MIRROR_E,ShowRecordings.duration("mirror_prison_e"));}
    private static void prison(ServerPlayer p,VisitData.Scene s,FirstEscapeTimeline.Phase state,int ticks){var c=FirstEscapeScene.prison(s);var l=p.server.getLevel(OtherWorld.KEY);var m=mother(p,s);if(m!=null)m.setInvisible(true);
        FirstEscapeScene.light(p,s,c.above(3),state==MIRROR_C||state==MIRROR_D||state==MIRROR_E?8:5);var bed=FirstEscapeScene.ensure(p,s,"PrisonBed",CinematicPropEntity.PRISON_BED,0,l,Vec3.atBottomCenterOf(c.south(2)));if(bed!=null)bed.frame(state.ordinal(),ticks,state==MIRROR_C||state==MIRROR_D||state==MIRROR_E?1:0);
        var head=AcceptedButtonEyes.active(s)?null:FirstEscapeScene.ensure(p,s,"PrisonHead",CinematicPropEntity.PRISON_HEAD,0,l,Vec3.atBottomCenterOf(c.north(4)).add(0,1.7,.2));if(head!=null)head.frame(state.ordinal(),ticks,state==MIRROR_A?Math.min(1,ticks/15F):state==MIRROR_RETRACT?Math.max(0,1-ticks/25F):0);
        for(int i=0;i<3;i++){var ghost=FirstEscapeScene.ensure(p,s,"Ghost"+i,CinematicPropEntity.GHOST_CHILD,i,l,Vec3.atBottomCenterOf(c.south(2)));if(ghost==null)continue;double reveal=state==MIRROR_C?Math.max(0,Math.min(1,(ticks-(ShowRecordings.duration("mirror_prison_c")-60)-i*8)/30.)):state==MIRROR_D?Math.min(1,(ShowRecordings.duration("mirror_prison_d")-ticks)/100.):state==MIRROR_E?.24:0;reveal=Math.max(0,reveal);double a=ticks*.006+i*2.1,radius=state==MIRROR_D?1.3*reveal:0;Vec3 target=Vec3.atBottomCenterOf(c.south(2)).add((i-1)*.65+Math.sin(a)*radius,state==MIRROR_E?-.6:-1.6+reveal*2.2,Math.cos(a)*radius);ghost.setPos(ghost.position().lerp(target,.1));ghost.frame(state==MIRROR_E?2:1,ticks,(float)reveal);}
        if(p.distanceToSqr(c.getCenter())>100)safeTransfer(p,l,c);
    }
    private static CharacterEntity wybie(ServerPlayer p,VisitData.Scene s){var w=ShowActors.actor(p,s,"other_wybie");if(w==null){var n=data(s);w=ShowActors.acquire(p,s,"other_wybie",p.server.getLevel(OtherWorld.KEY),Vec3.atBottomCenterOf(FirstEscapeScene.mirror(s).north(2)));}if(w instanceof CharacterEntity actor){if(phase(s)!=ESCAPE_C&&phase(s)!=FOLLOW_WYBIE){actor.setNoGravity(false);actor.setNoAi(false);}actor.noPhysics=false;data(s).putInt("OtherWybieId",actor.getId());data(s).putLong("OtherWybiePosition",actor.blockPosition().asLong());return actor;}return null;}
    static boolean rescueToHall(ServerPlayer p,VisitData.Scene s,CharacterEntity w){
        var n=data(s);var owner=FirstEscapeScene.hallLanding(p,s,null);var helper=owner==null?null:FirstEscapeScene.hallLanding(p,s,owner);
        if(owner==null||helper==null){n.putString("Blocker","The hallway beside the mirror needs clear floor for the rescue. Use /coraline recover after clearing it.");hint(p,"The hallway beside the mirror is obstructed.");return false;}
        n.putBoolean("InternalTransfer",true);var previousVehicle=p.getVehicle();p.stopRiding();p.setForcedPose(null);p.setPose(Pose.STANDING);n.remove("InternalTransfer");
        // Remove the client-side seat before its teleport packet. Otherwise the
        // old prison carrier can position its rider again after the landing.
        if(previousVehicle!=null&&p.connection!=null)p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetPassengersPacket(previousVehicle));
        retireGrab(p,s,"RescueHand");retireGrab(p,s,"Carrier");
        p.teleportTo(p.server.getLevel(OtherWorld.KEY),owner.x,owner.y,owner.z,p.getYRot(),0);p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
        w.stopRiding();w.getNavigation().stop();w.setDeltaMovement(Vec3.ZERO);w.setPos(helper);w.setOnGround(true);
        n.putLong("SafePlayer",BlockPos.containing(owner).asLong());n.putBoolean("Rescued",true);n.putInt("RescueLandingRevision",2);n.remove("Blocker");
        VisitAudio.play(p,"first_escape_music",true);n.putBoolean("EscapeMusicStarted",true);
        return true;
    }
    private static void retireGrab(ServerPlayer p,VisitData.Scene s,String key){
        var n=data(s);if(n.contains(key+"Position"))LoopProps.hold(p.server.getLevel(OtherWorld.KEY),BlockPos.of(n.getLong(key+"Position")));
        var prop=FirstEscapeScene.prop(p,s,key);if(prop==null)return;
        // Tracker removals normally flush at tick end. A teleport can be sent
        // first, allowing the old local carrier to overwrite its new position.
        if(prop.kind()==CinematicPropEntity.GRAB_CARRIER&&p.connection!=null)p.connection.send(new net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket(prop.getId()));
        prop.discard();n.remove(key);n.remove(key+"Position");
    }
    /** Also cleans up saved carriers when their chunk loads after the rescue. */
    static boolean retiredGrab(CinematicPropEntity prop,VisitData.Scene s){return prop.kind()==CinematicPropEntity.GRAB_CARRIER&&prop.cue()!=2&&engaged(s)&&(data(s).getBoolean("Rescued")||data(s).getBoolean("MirrorPrisonEntered")&&data(s).hasUUID("Carrier")&&prop.getUUID().equals(data(s).getUUID("Carrier")));}
    private static void rescue(ServerPlayer p,VisitData.Scene s,int ticks){
        var w=wybie(p,s);if(w==null)return;w.gameGesture(50,5);var n=data(s);
        if(ticks<24&&!n.getBoolean("Rescued")){var carrier=FirstEscapeScene.ensure(p,s,"RescueHand",CinematicPropEntity.GRAB_CARRIER,0,p.server.getLevel(OtherWorld.KEY),p.position());if(carrier!=null){carrier.frame(FirstEscapeVisualRules.RESCUE_HAND_CUE,ticks,1);if(p.getVehicle()!=carrier)p.startRiding(carrier,true);moveProp(carrier,FirstEscapeScene.prison(s).north(3).getCenter(),.08);}}
        else if(!n.getBoolean("Rescued")&&!rescueToHall(p,s,w))return;
        if(n.getBoolean("Punched"))w.gameGesture(51,5);
        else hint(p,"Punch the masked figure");
    }
    private static Vec3 guideTarget(VisitData.Scene s){var wardrobe=BlockPos.of(data(s).getLong("LittleDoor"));return Vec3.atBottomCenterOf(wardrobe).add(2.5,0,2.5);}
    private static void releaseGuide(ServerPlayer p,VisitData.Scene s){FirstEscapeGuide.release(p,s,wybie(p,s));}
    private static void guide(ServerPlayer p,VisitData.Scene s){setRoomDoors(p,s,true);var w=wybie(p,s);if(w!=null)FirstEscapeGuide.tick(p,s,w);else{FirstEscapeGuide.release(p,s,null);hint(p,"Waiting for the original Wybie to load. You are free to move.");}}
    private static void lookEntrance(Mob actor,VisitData.Scene s){actor.getNavigation().stop();var at=FirstEscapeScene.entrance(s);actor.getLookControl().setLookAt(at.getX()+.5,at.getY()+1.5,at.getZ()+.5);}
    static Direction escapeDirection(VisitData.Scene s){var direction=Direction.byName(data(s).getString("EscapeDirection"));return direction!=null&&direction.getAxis().isHorizontal()?direction:Direction.SOUTH;}
    static BlockPos tunnelCell(VisitData.Scene s,int side,int y,int along){var forward=escapeDirection(s);return BlockPos.of(data(s).getLong("EscapeTunnel")).relative(forward,along).relative(forward.getClockWise(),side).above(y);}
    private static boolean spawnInEscapeTunnel(ServerPlayer p,VisitData.Scene s,double progress){
        var level=p.server.getLevel(OtherWorld.KEY);
        var story=data(s);
        if(level==null||!story.getBoolean("EscapeTunnelBuilt")||!story.contains("EscapeTunnel",Tag.TAG_LONG))return false;
        var at=Vec3.atBottomCenterOf(BlockPos.of(story.getLong("EscapeTunnel")))
                .add(Vec3.atLowerCornerOf(escapeDirection(s).getNormal()).scale(progress));
        var feet=BlockPos.containing(at);
        level.getChunkAt(feet);
        var volume=new net.minecraft.world.phys.AABB(at.x-.29,at.y+.01,at.z-.29,at.x+.29,at.y+.59,at.z+.29);
        if(!level.getWorldBorder().isWithinBounds(feet)||!level.noCollision(p,volume)||!level.getFluidState(feet).isEmpty()
                ||!level.getBlockState(feet.below()).isFaceSturdy(level,feet.below(),Direction.UP)){
            hint(p,"The original crawlspace is obstructed.");
            return false;
        }
        p.setForcedPose(Pose.SWIMMING);p.setPose(Pose.SWIMMING);
        p.teleportTo(level,at.x,at.y,at.z,p.getYRot(),p.getXRot());
        return p.level()==level&&p.position().distanceToSqr(at)<=.25;
    }
    static void ensureDecayedTunnel(ServerPlayer p,VisitData.Scene s){var n=data(s);if(n.getBoolean("EscapeTunnelBuilt")){refreshDecayedTunnelMaterials(p,s);return;}BlockPos origin=FirstEscapeScene.prison(s).east(24);n.putLong("EscapeTunnel",origin.asLong());var l=p.server.getLevel(OtherWorld.KEY);var doorState=l.getBlockState(BlockPos.of(n.getLong("LittleDoor")));var forward=doorState.is(com.coraline.registry.ModBlocks.TINY_DOOR.get())?doorState.getValue(com.coraline.block.TinyDoorBlock.FACING).getOpposite():Direction.SOUTH;n.putString("EscapeDirection",forward.getName());for(int z=0;z<=22;z++)for(int x=-2;x<=2;x++)for(int y=-1;y<=3;y++){var at=tunnelCell(s,x,y,z);l.getChunkAt(at);boolean shell=Math.abs(x)>=1||y==-1||y>=1||z==0||z==22;FirstEscapeScene.place(p,s,at,(shell?decayedTunnelMaterial(z,x,y):Blocks.AIR).defaultBlockState());}for(int z=3;z<22;z+=4){FirstEscapeScene.light(p,s,tunnelCell(s,0,0,z),5);}n.putBoolean("EscapeTunnelBuilt",true);n.putInt("EscapeTunnelLookVersion",6);}
    private static Block decayedTunnelMaterial(int along,int side,int y){return (along+Math.abs(side)*3+y*5)%7==0?Blocks.MOSS_BLOCK:Blocks.MOSSY_STONE_BRICKS;}
    private static void refreshDecayedTunnelMaterials(ServerPlayer p,VisitData.Scene s){
        var n=data(s);if(n.getInt("EscapeTunnelLookVersion")>=6||!n.contains("EscapeTunnel"))return;var l=p.server.getLevel(OtherWorld.KEY);if(l==null)return;var owned=n.getCompound("Environment");
        for(int z=0;z<=22;z++)for(int x=-2;x<=2;x++)for(int y=-1;y<=3;y++){
            if(!(Math.abs(x)>=1||y==-1||y>=1||z==0||z==22))continue;var at=tunnelCell(s,x,y,z);var key=Long.toString(at.asLong());if(!owned.contains(key))continue;l.getChunkAt(at);var state=l.getBlockState(at);
            if(!state.is(Blocks.BARRIER)&&!state.is(Blocks.MOSS_BLOCK)&&!state.is(Blocks.MOSSY_STONE_BRICKS)&&!state.is(Blocks.BROWN_WOOL)&&!state.is(Blocks.DARK_OAK_PLANKS)&&!state.is(Blocks.GRAY_WOOL)&&!state.is(Blocks.LIGHT_GRAY_WOOL))continue;
            var expected=NbtUtils.readBlockState(l.holderLookup(net.minecraft.core.registries.Registries.BLOCK),owned.getCompound(key).getCompound("Placed"));
            if(state.equals(expected))FirstEscapeScene.place(p,s,at,decayedTunnelMaterial(z,x,y).defaultBlockState());
        }
        // Web strands are the same connected geometry used at the doorway.
        // Retire only the old decorations recorded by this scene.
        for(int z=3;z<22;z+=4){
            for(var at:List.of(tunnelCell(s,1,2,z),tunnelCell(s,-1,2,z+1))){
                var record=owned.getCompound(Long.toString(at.asLong()));
                if(record.isEmpty())continue;
                l.getChunkAt(at);
                var old=l.getBlockState(at);
                if(!old.is(Blocks.COBWEB)&&!old.is(Blocks.HANGING_ROOTS))continue;
                var expected=NbtUtils.readBlockState(l.holderLookup(net.minecraft.core.registries.Registries.BLOCK),record.getCompound("Placed"));
                if(old.equals(expected))FirstEscapeScene.place(p,s,at,decayedTunnelMaterial(z,1,2).defaultBlockState());
            }
        }
        n.putInt("EscapeTunnelLookVersion",6);OtherMotherLoop.changed(p);
    }
    private static void pushIntoTunnel(ServerPlayer p,VisitData.Scene s,int ticks){
        var n=data(s);var w=wybie(p,s);if(w!=null)w.gameGesture(46,5);var little=BlockPos.of(n.getLong("LittleDoor"));
        if(p.distanceToSqr(little.getCenter())>16){hint(p,"Come back to the Little Door — Wybie is waiting.");n.putInt("Ticks",0);return;}
        if(ticks<25){var delta=little.getCenter().subtract(p.position());if(delta.lengthSqr()>.5){p.setDeltaMovement(delta.normalize().scale(.12));p.hurtMarked=true;}}
        else{ensureDecayedTunnel(p,s);p.stopRiding();if(spawnInEscapeTunnel(p,s,1)){openLittleDoor(p,s,false);go(p,s,ESCAPE_TUNNEL);}}
    }
    private static void tunnel(ServerPlayer p,VisitData.Scene s){var origin=BlockPos.of(data(s).getLong("EscapeTunnel"));p.setForcedPose(net.minecraft.world.entity.Pose.SWIMMING);p.setPose(net.minecraft.world.entity.Pose.SWIMMING);var forward=escapeDirection(s);double progress=p.position().subtract(Vec3.atBottomCenterOf(origin)).dot(new Vec3(forward.getStepX(),0,forward.getStepZ()));data(s).putDouble("TunnelProgress",Math.max(1,Math.min(20,progress)));hint(p,"Escape through the tunnel.");if(progress>=20){var d=sourceDoor(p,s);if(d==null){data(s).putString("Blocker","The original Overworld door record is unavailable.");return;}var source=FamilyData.level(p.server,d.dimension);if(!verifiedTransfer(p,s,source,d.pos)){data(s).putString("Blocker","The original Little Door return landing is obstructed.");return;}p.getPersistentData().remove("CoralineInTunnel");p.getPersistentData().remove("CoralineReturning");p.setForcedPose(null);p.setPose(net.minecraft.world.entity.Pose.STANDING);p.getPersistentData().remove("CoralineAutoCrawl");go(p,s,MISSING_A);} }
    private static void missing(ServerPlayer p,VisitData.Scene s){var family=ParentAccess.family(p.server,p.getUUID());if(family!=null){family.storyMissing=true;FamilyData.get(p.server).setDirty();}com.coraline.doll.DollBondData.get(p.server).markStoryMissing(p.getUUID());}
    /** Skip the escape itself, but retain the real Overworld Wybie/neighbor/Cat
     * cutscenes. Older skips jumped over this handoff as well. */
    public static String skipToAftermath(ServerPlayer p,VisitData.Scene s){
        if(!p.hasPermissions(2)||s==null||!p.getUUID().equals(s.owner))return "Owner/operator authorization required.";
        var n=data(s);
        if(MissingParentsTea.active(s)||MissingParentsTea.data(s).getBoolean("Completed")||MissingParentsCat.data(s).getBoolean("Started")||ParentRescueStory.started(s))return "Post-escape scenes are already unlocked. Continue the current objective; they were not reset.";
        if(n.getBoolean("DebugAftermathStarted")){return "The post-escape Wybie scene is already running. Continue its current objective.";}
        if(!finishFirstEscape(p,s,true))return "Cannot finish safely: restore the original owned Little Door or allow the original Wybie to load.";
        n.putBoolean("Complete",false);n.putBoolean("WybieDeparted",false);n.putBoolean("DebugAftermathStarted",true);
        n.remove("ChaseClose");n.remove("ChaseTarget");n.remove("StoryBike");n.remove("StoryBikePosition");
        s.loop.phase=OtherMotherLoopState.Phase.BLACK_IS_TRADITIONAL;
        go(p,s,MISSING_A);p.server.overworld().getDataStorage().save();
        return "Escape skipped. The real Overworld Wybie cutscenes now play, followed by Spink/Forcible, the Cat and the rescue chapter.";
    }
    /** Shared, idempotent milestone transaction. Debug skips never earn the chase reward. */
    public static boolean finishFirstEscape(ServerPlayer p,VisitData.Scene s,boolean debugSkip){
        if(s==null||!p.getUUID().equals(s.owner)||debugSkip&&!p.hasPermissions(2))return false;
        var n=data(s);if(n.getBoolean("Complete"))return true;
        var d=sourceDoor(p,s);if(d==null||FamilyData.level(p.server,d.dimension)!=p.server.overworld())return false;
        if(debugSkip&&!engaged(s)&&!VisitEvents.cleanup(p,s))return false;
        if(p.serverLevel()!=p.server.overworld()&&!verifiedTransfer(p,s,p.server.overworld(),d.pos)){
            n.putString("Blocker","Waiting for a clear landing at the original Little Door.");
            return false;
        }
        if(!n.hasUUID("Token"))n.putUUID("Token",UUID.randomUUID());
        var realm=RealmData.get(p.server).realms.get(s.door);if(!n.contains("House")&&realm!=null&&realm.house!=null){n.putLong("House",realm.house.asLong());if(realm.doorPos!=null)n.putLong("LittleDoor",realm.doorPos.asLong());}
        n.putBoolean("Started",true);n.putBoolean("InternalTransfer",true);p.closeContainer();p.stopRiding();p.stopSleepInBed(true,false);p.setForcedPose(null);p.setPose(Pose.STANDING);n.remove("InternalTransfer");
        ShowPlayback.stop(p,s);VisitAudio.stop(p);removeTool(p,s,false);removeTool(p,s,true);
        if(n.contains("LittleDoor"))cleanupPassage(p,s);
        // Keep the original for the well and future chapters, including debug cleanup.
        var real=ShowActors.actor(p,s,"wybie");
        if(real instanceof CharacterEntity actor&&!(debugSkip?CanonicalRealWybie.debugPark(p,s,actor):CanonicalRealWybie.park(p,s,actor))){n.putString("Blocker","Waiting for real Wybie's safe offscreen storage; he will not be deleted.");return false;}
        if(real==null&&s.loop.event.getCompound("ShowActors").getCompound("wybie").hasUUID("Entity")){n.putString("Blocker","Waiting for the original Wybie to load before preserving him.");return false;}
        if(debugSkip&&s.wybieActor!=null){for(var l:p.server.getAllLevels())if(l.getEntity(s.wybieActor) instanceof com.coraline.wybie.WybieVisitorEntity w&&s.owner.equals(w.owner)){if(!CanonicalRealWybie.debugPark(p,s,w))return false;break;}}
        for(var l:p.server.getAllLevels()){var discard=new ArrayList<Entity>();for(var e:l.getAllEntities()){
            if(e instanceof CinematicPropEntity prop&&prop.canonicalGhost(s)){prop.setInvisible(true);prop.frame(0,0,0);continue;}
            if(e instanceof CinematicPropEntity prop&&prop.owned(s)&&prop.kind()>=CinematicPropEntity.STORY_CANE&&prop.kind()<=CinematicPropEntity.STORY_SHOE||e instanceof WybieBicycleEntity bike&&storyBike(bike)&&s.owner.equals(bike.getPersistentData().getUUID("FirstEscapeOwner")))discard.add(e);
        }for(var e:discard){e.ejectPassengers();e.discard();}}
        FirstEscapeScene.restore(p,s);ShowActors.restore(p,s);
        for(String key:List.of("CoralineInTunnel","CoralineReturning","CoralineAutoCrawl","CoralineWakeAt"))p.getPersistentData().remove(key);
        var old=BlackIsTraditional.data(s);old.putBoolean("Started",true);old.putBoolean("Rejected",true);old.putBoolean("StudyRevealComplete",true);old.putString("Phase",BlackIsTraditional.Phase.STUDY_REVEAL_COMPLETE.name());
        n.putBoolean("Complete",true);n.putBoolean("OtherWorldSealed",true);n.putBoolean("WybieDeparted",true);n.putBoolean("ShoeResolved",true);n.putBoolean("DecayedTunnel",false);n.putString("Phase",ESCAPE_COMPLETE.name());n.putInt("Ticks",0);n.putUUID("Choice",UUID.randomUUID());n.remove("Recover");n.remove("InternalTransfer");n.putBoolean("DebugSkipped",debugSkip);
        if(debugSkip)OtherWorld.prepareStoryRealm(p,d);
        s.loop.story.putString("MajorStoryPhase","POST_FIRST_ESCAPE");missing(p,s);brickDoor(p,s);OtherMotherLoop.changed(p);MissingParentsTea.select(p,s,false);sync(p,s);p.server.overworld().getDataStorage().save();return true;
    }
    private static void brickDoor(ServerPlayer p,VisitData.Scene s){var d=sourceDoor(p,s);if(d==null)return;var l=FamilyData.level(p.server,d.dimension);if(l==null)return;l.getChunkAt(d.pos);var state=l.getBlockState(d.pos);if(!state.is(com.coraline.registry.ModBlocks.TINY_DOOR.get()))return;if(l.getBlockEntity(d.pos) instanceof DoorBlockEntity door&&!TunnelPassage.restore(l,door))return;l.setBlock(d.pos,state.setValue(com.coraline.block.TinyDoorBlock.PORTAL,false).setValue(com.coraline.block.TinyDoorBlock.OPEN,false),3);var behind=d.pos.relative(state.getValue(com.coraline.block.TinyDoorBlock.FACING).getOpposite());if(l.getBlockEntity(behind)==null){var n=data(s);if(!n.contains("BrickOriginal"))n.put("BrickOriginal",NbtUtils.writeBlockState(l.getBlockState(behind)));l.setBlock(behind,Blocks.BRICKS.defaultBlockState(),2);}d.activeUntil=0;DoorData.get(p.server).setDirty();}
    private static CharacterEntity realWybie(ServerPlayer p,VisitData.Scene s){var n=data(s);var w=ShowActors.actor(p,s,"wybie");if(w==null){var d=sourceDoor(p,s);if(d==null)return null;var at=OtherWorld.safePlayer(p.serverLevel(),d.pos.offset(8,0,-8));if(at==null)return null;if(CanonicalRealWybie.recorded(s)){var canonical=CanonicalRealWybie.find(p,s);if(canonical==null||!CanonicalRealWybie.bind(p,s,canonical)){n.putString("Blocker","Canonical Wybie is unavailable or story-dead; no replacement will be created.");return null;}w=ShowActors.adopt(p,s,"wybie",canonical,p.serverLevel(),Vec3.atBottomCenterOf(at));}else if(s.wybieActor!=null){for(var level:p.server.getAllLevels())if(level.getEntity(s.wybieActor) instanceof com.coraline.wybie.WybieVisitorEntity canonical){w=ShowActors.adopt(p,s,"wybie",canonical,p.serverLevel(),Vec3.atBottomCenterOf(at));break;}if(w==null){n.putString("Blocker","Waiting for the canonical Wybie visitor to load; no duplicate will be created.");return null;}}else w=ShowActors.acquire(p,s,"wybie",p.serverLevel(),Vec3.atBottomCenterOf(at));}if(w instanceof CharacterEntity actor){if(!CanonicalRealWybie.bind(p,s,actor))return null;actor.setNoGravity(false);actor.noPhysics=false;actor.setNoAi(false);n.putInt("RealWybieId",actor.getId());n.putLong("RealWybiePosition",actor.blockPosition().asLong());return actor;}return null;}
    private static boolean gaze(ServerPlayer p,Mob w){var delta=w.getEyePosition().subtract(p.getEyePosition());return delta.lengthSqr()<24*24&&p.hasLineOfSight(w)&&p.getLookAngle().dot(delta.normalize())>.88;}
    private static void followReal(ServerPlayer p,VisitData.Scene s){var w=realWybie(p,s);if(w!=null){if(w.distanceToSqr(p)>9)walk(w,p.position(),1.15);else w.getNavigation().stop();w.getLookControl().setLookAt(p,30,20);} }
    public static void throwShoe(ServerPlayer p,ItemStack stack){var s=OtherMotherLoop.scene(p);if(!engaged(s)||phase(s)!=THROW_SHOE||!validTool(s,stack,true)||data(s).hasUUID("ShoeFlight"))return;var e=FirstEscapeScene.ensure(p,s,"ShoeFlight",CinematicPropEntity.STORY_SHOE,0,p.serverLevel(),p.getEyePosition().add(p.getLookAngle().scale(.4)));if(e!=null){stack.shrink(1);e.setDeltaMovement(p.getLookAngle().scale(1.05));e.frame(0,0,0);OtherMotherLoop.changed(p);} }
    private static void shoe(ServerPlayer p,VisitData.Scene s){var e=FirstEscapeScene.prop(p,s,"ShoeFlight");if(e==null)return;if(e.cue()==1){finishShoeThrow(p,s,e);return;}data(s).putLong("ShoeFlightPosition",e.blockPosition().asLong());var w=realWybie(p,s);Vec3 from=e.position(),to=from.add(e.getDeltaMovement());var wall=p.serverLevel().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,e));var hit=w==null?Optional.<Vec3>empty():w.getBoundingBox().inflate(.2).clip(from,to);if(hit.isPresent()&&(wall.getType()==HitResult.Type.MISS||hit.get().distanceToSqr(from)<wall.getLocation().distanceToSqr(from))){w.gameGesture(49,40);finishShoeThrow(p,s,e);return;}
        e.frame(e.cue(),e.time()+1,0);if(wall.getType()!=HitResult.Type.MISS||e.time()>100||e.getY()<p.getY()-12){finishShoeThrow(p,s,e);}else {e.setPos(to);e.setDeltaMovement(e.getDeltaMovement().add(0,-.04,0).scale(.98));} }
    private static void finishShoeThrow(ServerPlayer p,VisitData.Scene s,CinematicPropEntity shoe){shoe.discard();data(s).remove("ShoeFlight");data(s).putBoolean("ShoeResolved",true);go(p,s,MISSING_E);}
    private static Vec3 escapeTarget(ServerPlayer p,VisitData.Scene s,CharacterEntity w){var n=data(s);if(n.contains("ChaseTarget")&&w.distanceToSqr(Vec3.atBottomCenterOf(BlockPos.of(n.getLong("ChaseTarget"))))>9)return Vec3.atBottomCenterOf(BlockPos.of(n.getLong("ChaseTarget")));n.remove("ChaseTarget");for(int radius:new int[]{12,18,24})for(var direction:Direction.Plane.HORIZONTAL){var at=OtherWorld.safePlayer(p.serverLevel(),w.blockPosition().relative(direction,radius));var path=w.getNavigation().createPath(at,0);if(path!=null&&path.canReach()&&path.getNodeCount()>5){n.putLong("ChaseTarget",at.asLong());return Vec3.atBottomCenterOf(at);}}return w.position();}
    private static void chase(ServerPlayer p,VisitData.Scene s,int ticks){var w=realWybie(p,s);if(w==null)return;walk(w,escapeTarget(p,s,w),2.35);var n=data(s);if(p.distanceToSqr(w)<20.25)n.putInt("ChaseClose",n.getInt("ChaseClose")+1);int required=Math.max(10,Math.min(30,ShowRecordings.duration("missing_parents_f")/5));if(n.getInt("ChaseClose")>=required&&!n.getBoolean("ChaseRewardEarned")){n.putBoolean("ChaseRewardEarned",true);OtherMotherLoop.changed(p);}payChaseReward(p,s);hint(p,"Chase Wybie!");}
    static void departure(ServerPlayer p,VisitData.Scene s){
        var w=realWybie(p,s);if(w==null)return;var n=data(s);
        if(phase(s)==BICYCLE_DEPARTURE){int age=n.getInt("BicycleDepartureTicks")+1;n.putInt("BicycleDepartureTicks",age);if((age>=160||age>=60&&n.getInt("BikeStallTicks")>=40)&&CanonicalRealWybie.finishBlockedDeparture(p,s,w)){var old=n.hasUUID("StoryBike")?p.serverLevel().getEntity(n.getUUID("StoryBike")):null;if(old instanceof WybieBicycleEntity)old.discard();go(p,s,ESCAPE_COMPLETE);return;}}
        if(!n.hasUUID("StoryBike")){
            var bike=com.coraline.registry.ModEntities.WYBIE_BICYCLE.get().create(p.serverLevel());if(bike==null)return;
            bike.configure(s.owner,s.door,UUID.randomUUID(),s.loop.event.getInt("Serial"),w.getUUID(),true);bike.getPersistentData().putUUID("FirstEscapeOwner",s.owner);bike.setPos(StoryBikeRoute.spawn(bike,w.position(),p));
            if(p.serverLevel().addFreshEntity(bike)){n.putUUID("StoryBike",bike.getUUID());n.putLong("StoryBikePosition",bike.blockPosition().asLong());}
        }
        if(!n.contains("StoryBikePosition")&&n.contains("RealWybiePosition"))n.putLong("StoryBikePosition",n.getLong("RealWybiePosition"));
        if(n.contains("StoryBikePosition"))LoopProps.hold(p.serverLevel(),BlockPos.of(n.getLong("StoryBikePosition")));
        var raw=n.hasUUID("StoryBike")?p.serverLevel().getEntity(n.getUUID("StoryBike")):null;
        if(!(raw instanceof WybieBicycleEntity bike)){n.putString("Blocker","Waiting for the original departure bicycle to load; no duplicate created.");return;}
        n.putLong("StoryBikePosition",bike.blockPosition().asLong());LoopProps.hold(p.serverLevel(),bike.blockPosition());
        if(w.getVehicle()!=bike){walk(w,bike.position(),1.1);if(w.distanceToSqr(bike)<4){w.getNavigation().stop();w.setDeltaMovement(Vec3.ZERO);w.startRiding(bike,true);}}
        if(w.getVehicle()==bike){
            // Drive on the story's authoritative tick too, including the mount
            // frame. Vehicle tick order/chunk scheduling cannot leave a posed
            // rider standing still. The shared tick stamp prevents double speed.
            driveStoryBike(bike,p,s,w);
            w.gameGesture(bike.speed()>.01?52:0,5);int ridden=n.getInt("DepartureRiddenTicks")+1;n.putInt("DepartureRiddenTicks",ridden);
            // park() checks every viewer, including the 128-block view boundary.
            // The old unlimited-distance inView() gate could reject departure forever.
            if(phase(s)==BICYCLE_DEPARTURE&&ridden>=40&&p.distanceToSqr(bike)>12*12&&CanonicalRealWybie.park(p,s,w)){
                bike.discard();n.remove("Blocker");go(p,s,ESCAPE_COMPLETE);
            }
        }
    }
    public static boolean storyBike(WybieBicycleEntity bike){return bike.getPersistentData().hasUUID("FirstEscapeOwner");}
    public static boolean protectedWybie(CharacterEntity actor){
        if(actor.getServer()==null||!actor.characterId().equals("wybie"))return false;
        var tag=actor.getPersistentData();var owner=tag.hasUUID("CanonicalRealWybieOwner")?tag.getUUID("CanonicalRealWybieOwner"):tag.hasUUID("ShowActorOwner")?tag.getUUID("ShowActorOwner"):null;
        var s=owner==null?null:VisitData.get(actor.getServer()).scenes.get(owner);
        return engaged(s)&&FirstEscapeTimeline.overworld(phase(s))&&phase(s)!=ESCAPE_COMPLETE;
    }
    public static void driveStoryBike(WybieBicycleEntity bike){
        var p=bike.getServer().getPlayerList().getPlayer(bike.getPersistentData().getUUID("FirstEscapeOwner"));var s=p==null?null:OtherMotherLoop.scene(p);
        if(!engaged(s)||!data(s).hasUUID("StoryBike")||!bike.getUUID().equals(data(s).getUUID("StoryBike"))){bike.stop();return;}
        driveStoryBike(bike,p,s,realWybie(p,s));
    }
    private static void driveStoryBike(WybieBicycleEntity bike,ServerPlayer p,VisitData.Scene s,CharacterEntity w){
        if(w==null||w.getVehicle()!=bike){bike.stop();return;}
        long now=p.serverLevel().getGameTime();if(bike.storyDrivenAt==now)return;bike.storyDrivenAt=now;var n=data(s);
        if(n.getInt("BikeRouteGeometryVersion")<2){n.putInt("BikeRouteGeometryVersion",2);n.remove("BikeRoute");n.remove("BikeRouteIndex");n.remove("BikeRetryAt");n.remove("BikeRebased");}
        n.putLong("StoryBikePosition",bike.blockPosition().asLong());LoopProps.hold(p.serverLevel(),bike.blockPosition());
        var last=n.contains("BikeProgressPosition")?BlockPos.of(n.getLong("BikeProgressPosition")).getCenter():null;
        if(last==null||bike.position().distanceToSqr(last)>2){n.putLong("BikeProgressPosition",bike.blockPosition().asLong());n.putLong("BikeProgressAt",now);}
        else if(now-n.getLong("BikeProgressAt")>80){n.remove("BikeRoute");n.remove("BikeRouteIndex");n.putLong("BikeProgressAt",now);}
        Vec3 target=bikeWaypoint(p,s,w,bike);
        if(target==null){
            bike.stop();
            // Old saves can already have the bicycle embedded in a bank or
            // shallow water by the old centre-only placement. Settle the SAME
            // vehicle onto a verified nearby start once, then actually ride.
            if(n.getInt("BikeStallTicks")>=20&&!n.getBoolean("BikeRebased")){
                n.putBoolean("BikeRebased",true);var start=StoryBikeRoute.spawn(bike,bike.position(),p);
                if(start.distanceToSqr(bike.position())>.01&&start.distanceToSqr(bike.position())<=4){bike.storySettle(start);n.remove("BikeRoute");n.remove("BikeRetryAt");n.putInt("BikeStallTicks",0);return;}
            }
            n.putString("Blocker","The bicycle needs a clear, dry route away from this bank; bounded departure recovery remains active.");return;
        }
        LoopProps.hold(p.serverLevel(),BlockPos.containing(target));n.remove("Blocker");
        var before=bike.position();bike.storyAdvance(target);if(bike.position().distanceToSqr(before)<.0001){n.putInt("BikeStallTicks",n.getInt("BikeStallTicks")+1);n.remove("BikeRoute");}else n.putInt("BikeStallTicks",0);
    }
    public static boolean parentControl(ParentEntity m,ServerPlayer p,VisitData.Scene s){if(!engaged(s))return false;if(!m.isMom())return true;var state=phase(s);if(state.ordinal()<COCOA_E.ordinal()){var couch=FirstEscapeScene.prop(p,s,"BugCouch");if(couch!=null){if(m.getVehicle()!=couch){if(m.distanceToSqr(couch)<3)m.startRiding(couch,true);else walk(m,couch.position(),1);}m.lookAtStoryPlayer(p);}}return true;}
    public static boolean choice(ServerPlayer p,UUID nonce,String action){var s=OtherMotherLoop.scene(p);if(!engaged(s)||phase(s)!=APOLOGY_CHOICE||!data(s).hasUUID("Choice")||!data(s).getUUID("Choice").equals(nonce))return false;if(action.equals("escape:apologize"))return AcceptedButtonEyes.begin(p,s,AcceptedButtonEyesProgress.Route.ACCEPTED_PROPOSAL);
        if(action.equals("escape:reject"))go(p,s,COCOA_E);else return false;return true;}
    private static void sync(ServerPlayer p,VisitData.Scene s){var n=data(s);
        if(!n.hasUUID("Choice")){n.putUUID("Choice",UUID.randomUUID());OtherMotherLoop.changed(p);}
        var level=p.server.getLevel(OtherWorld.KEY);var mother=level==null||s.mom==null?null:level.getEntity(s.mom);
        var packet=new CompoundTag();packet.putBoolean("FirstEscapeStory",true);packet.putString("Phase",phase(s).name());packet.putInt("Ticks",n.getInt("Ticks"));packet.putBoolean("ClockRunning",FirstEscapeTimeline.sound(phase(s)).isEmpty()||ShowPlayback.started(s));packet.putString("Sound",FirstEscapeTimeline.sound(phase(s)));packet.putInt("Duration",ShowRecordings.duration(FirstEscapeTimeline.sound(phase(s))));packet.putUUID("Choice",n.getUUID("Choice"));packet.putUUID("Door",s.door);packet.putLong("RoomMin",FirstEscapeScene.roomMin(s).asLong());packet.putLong("RoomMax",FirstEscapeScene.roomMax(s).asLong());packet.putInt("Mother",mother==null?-1:mother.getId());packet.putInt("OtherWybie",n.getInt("OtherWybieId"));packet.putInt("RealWybie",n.getInt("RealWybieId"));for(String key:List.of("MaskRemoved","StitchesRemoved","HandMissing","BeetleEaten","DecayedTunnel","CocoaMusic","Rescued","MirrorPrisonEntered"))packet.putBoolean(key,n.getBoolean(key));if(n.contains("KnockdownAt"))packet.putInt("KnockdownAge",(int)Math.max(0,OtherMotherLoop.now(p)-n.getLong("KnockdownAt")));packet.putBoolean("Completed",phase(s)==ESCAPE_COMPLETE);var tunnel=FirstEscapeStory.data(s);if(tunnel.contains("EscapeTunnel")){packet.putLong("EscapeTunnel",tunnel.getLong("EscapeTunnel"));packet.putString("EscapeDirection",tunnel.getString("EscapeDirection"));}SecretsNetwork.send(p,packet);}
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST) public static void click(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract e){if(!(e.getEntity() instanceof ServerPlayer p))return;var s=OtherMotherLoop.scene(p);if(!engaged(s)||p.distanceToSqr(e.getTarget())>16)return;var w=ShowActors.actor(p,s,"other_wybie");if(e.getTarget()!=w){if(e.getTarget() instanceof CinematicPropEntity prop&&interact(p,prop)){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);}return;}e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);if(phase(s)==MASK_REMOVAL){p.swing(InteractionHand.MAIN_HAND,true);go(p,s,MASK_ANIMATION);}else if(phase(s)==STITCH_REMOVAL){p.swing(InteractionHand.MAIN_HAND,true);go(p,s,STITCH_ANIMATION);}}
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST) public static void punch(net.minecraftforge.event.entity.player.AttackEntityEvent e){if(!(e.getEntity() instanceof ServerPlayer p))return;var s=OtherMotherLoop.scene(p);if(!engaged(s))return;if(e.getTarget()==ShowActors.actor(p,s,"wybie")&&FirstEscapeTimeline.overworld(phase(s))){e.setCanceled(true);return;}if(e.getTarget()!=ShowActors.actor(p,s,"other_wybie"))return;e.setCanceled(true);if((phase(s)==ESCAPE_A||phase(s)==MASK_REMOVAL)&&p.distanceToSqr(e.getTarget())<16){data(s).putBoolean("Punched",true);data(s).putLong("KnockdownAt",OtherMotherLoop.now(p));((CharacterEntity)e.getTarget()).gameGesture(51,25);sfx(p,e.getTarget().blockPosition(),SoundEvents.PLAYER_ATTACK_WEAK,.5F,.8F);OtherMotherLoop.changed(p);}}
    @SubscribeEvent public static void mount(net.minecraftforge.event.entity.EntityMountEvent e){if(e.isDismounting()&&e.getEntityMounting() instanceof ServerPlayer p){var s=OtherMotherLoop.scene(p);if(!AcceptedButtonEyes.active(s)&&engaged(s)&&!data(s).getBoolean("InternalTransfer")&&(FirstEscapeTimeline.seated(phase(s))||phase(s)==MIRROR_DRAG||phase(s)==MIRROR_THROW)&&e.getEntityBeingMounted() instanceof CinematicPropEntity prop&&prop.owned(s))e.setCanceled(true);}}
    @SubscribeEvent public static void useBlock(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock e){if(e.getEntity() instanceof ServerPlayer p&&engaged(OtherMotherLoop.scene(p))&&block(p,e.getPos())){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);}}
    @SubscribeEvent public static void login(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p){var s=OtherMotherLoop.scene(p);if(engaged(s)){data(s).putBoolean("Recover",true);if(MissingParentsCat.data(s).getBoolean("Started"))MissingParentsCat.data(s).putBoolean("Recover",true);if(ParentRescueStory.started(s))ParentRescueStory.data(s).putBoolean("Recover",true);if(MissingParentsTea.active(s))MissingParentsTea.data(s).putBoolean("Recover",true);}}}
    private static Vec3 bikeWaypoint(ServerPlayer p,VisitData.Scene s,CharacterEntity w,WybieBicycleEntity bike){
        var n=data(s);var points=n.getLongArray("BikeRoute");int index=n.getInt("BikeRouteIndex");
        while(index<points.length&&bike.position().subtract(Vec3.atBottomCenterOf(BlockPos.of(points[index]))).horizontalDistanceSqr()<.0256)index++;
        if(index<points.length){n.putInt("BikeRouteIndex",index);return Vec3.atBottomCenterOf(BlockPos.of(points[index]));}
        if(n.getLong("BikeRetryAt")>p.serverLevel().getGameTime()){n.putInt("BikeStallTicks",n.getInt("BikeStallTicks")+1);return null;}
        points=StoryBikeRoute.plan(bike,p);if(points.length==0){n.putLong("BikeRetryAt",p.serverLevel().getGameTime()+10);n.putInt("BikeStallTicks",n.getInt("BikeStallTicks")+1);return null;}
        n.putLongArray("BikeRoute",points);n.putInt("BikeRouteIndex",0);return Vec3.atBottomCenterOf(BlockPos.of(points[0]));
    }
    @SubscribeEvent public static void cinematicDamage(net.minecraftforge.event.entity.living.LivingAttackEvent e){if(e.getEntity() instanceof ServerPlayer p){if(RescueCombat.permitted(p,e.getSource()))return;var s=OtherMotherLoop.scene(p);if(engaged(s)&&!FirstEscapeTimeline.overworld(phase(s)))e.setCanceled(true);}}
    public static boolean debugSection(String section){return List.of("cocoa","prison","escape","missing").contains(section);}
    public static Map<String,String> debugActions(String section){var out=new LinkedHashMap<String,String>();switch(section){
        case "cocoa"->{out.put("cane","Spawn/recover the canonical porch cane");out.put("pry","Use held cane at the actual double doors");out.put("room","Preview room transformation");out.put("chair","Test living-chair capture");out.put("bite","Preview beetle bite");out.put("apology","Test the exact apology decision");out.put("morph","Preview original Evil Mother transformation");out.put("mirror","Test physical mirror drag");}
        case "prison"->{out.put("cell","Build/enter the physical prison");out.put("glow","Preview green bed during C");out.put("ghosts","Preview three storytelling ghosts during D");out.put("heads","Preview heads at the bed during E");out.put("rescue","Start masked Wybie rescue");}
        case "escape"->{out.put("rescue","Start masked Wybie rescue");out.put("knockdown","Test nearby non-lethal knockdown");out.put("mask","Remove mask at its interaction gate");out.put("stitches","Remove stitches at their interaction gate");out.put("guide","Test actual Wybie guide route");out.put("push","Use the actual wardrobe interaction");out.put("fall","Preview timed wardrobe fall during D");out.put("door","Open the actual Little Door at its gate");out.put("decay","Preview decayed tunnel during E");out.put("hand","Preview timed sawdust-hand cue during F");out.put("tunnel","Test final tunnel push and walkable escape");}
        case "missing"->{out.put("hide","Apply missing-parent/canonical-doll ledgers");out.put("wybie","Spawn/recover the canonical real Wybie");out.put("gaze","Test real proximity/line-of-sight gaze");out.put("follow","Test Wybie following during B");out.put("brick","Go to actual bricked Little Door");out.put("doll","Preview narrative missing-doll search during D");out.put("shoe","Give/recover the unique shoe at its gate");out.put("collision","Throw the held shoe through real collision checks");out.put("chase","Preview the short chase during F");out.put("reward","Retire legacy chase reward receipt (no item grant)");out.put("bike","Preview actual bicycle departure during G");out.put("seal","Apply persistent owner-only portal seal");}
        default->{}
        }return out;}
    public static String debugSection(FirstEscapeTimeline.Phase state){return state.ordinal()<MIRROR_A.ordinal()?"cocoa":state.ordinal()<ESCAPE_A.ordinal()?"prison":state.ordinal()<MISSING_A.ordinal()?"escape":"missing";}
    public static List<String> inspect(ServerPlayer p,VisitData.Scene s){var n=data(s);return List.of("Checkpoint "+phase(s)+" | started "+engaged(s),"Recording "+FirstEscapeTimeline.sound(phase(s))+" | position "+ShowPlayback.position(s)+" | duration "+ShowRecordings.duration(FirstEscapeTimeline.sound(phase(s))),"Audio started "+n.getBoolean("AudioStarted")+" | done "+n.getBoolean("AudioDone"),"Owner "+s.owner+" | seated "+p.isPassenger(),"Cane "+n.getBoolean("CaneCollected")+" | bite "+n.getBoolean("BeetleEaten")+" | apology rejected "+n.getBoolean("ApologyRejected"),"Punched "+n.getBoolean("Punched")+" | mask "+n.getBoolean("MaskRemoved")+" | stitches "+n.getBoolean("StitchesRemoved")+" | hand missing "+n.getBoolean("HandMissing"),"Other Wybie "+n.getInt("OtherWybieId")+" | real Wybie "+n.getInt("RealWybieId"),"Shoe in flight "+n.hasUUID("ShoeFlight")+" | earned "+n.getBoolean("ChaseRewardEarned")+" | paid "+n.getBoolean("ChaseRewardPaid"),"Sealed "+n.getBoolean("OtherWorldSealed")+" | complete "+n.getBoolean("Complete"),"Journalled cells "+n.getCompound("Environment").size()+" | blocker "+n.getString("Blocker"));}
    public static String debug(ServerPlayer p,VisitData.Scene s,String action,String value){
        if(!NumberedDebugActions.operator(p,s))return "Owner/operator validation failed.";
        if(action.equals("state"))return "Open Details for phase, audio, actors, cues and blockers.";
        if(action.equals("begin"))return begin(p,s)?"Continuation active.":"Requires the actual rejected-button study reveal endpoint in the Other World.";
        if(!engaged(s))return "Begin the continuation at its valid story boundary first.";
        var n=data(s);
        var previews=Map.ofEntries(Map.entry("room",ROOM_TRANSFORMING),Map.entry("chair",CHAIR_CAPTURE),Map.entry("bite",COCOA_BITE),Map.entry("apology",APOLOGY_CHOICE),Map.entry("morph",COCOA_F),Map.entry("mirror",MIRROR_DRAG),Map.entry("cell",MIRROR_A),Map.entry("glow",MIRROR_C),Map.entry("ghosts",MIRROR_D),Map.entry("heads",MIRROR_E),Map.entry("rescue",ESCAPE_A),Map.entry("guide",FOLLOW_WYBIE),Map.entry("fall",ESCAPE_D),Map.entry("decay",ESCAPE_E),Map.entry("hand",ESCAPE_F),Map.entry("tunnel",ESCAPE_PUSH),Map.entry("follow",MISSING_B),Map.entry("doll",MISSING_D),Map.entry("chase",MISSING_F),Map.entry("bike",MISSING_G));
        if(previews.containsKey(action))return debug(p,s,"phase_"+previews.get(action).name().toLowerCase(Locale.ROOT),value);
        if(action.equals("cane")){if(phase(s)!=FIND_CANE&&phase(s)!=CANE_COLLECTED)return "Cane retrieval has already ended.";ensureCane(p,s);return "The canonical porch cane was checked; use its normal pickup interaction.";}
        if(action.equals("pry")){block(p,FirstEscapeScene.entrance(s));return "Held-cane and door-phase guards checked.";}
        if(action.equals("mask")||action.equals("stitches")){var w=wybie(p,s);if(w==null||p.distanceToSqr(w)>16)return "Move within reach of the actual Wybie actor.";if(action.equals("mask")&&phase(s)==MASK_REMOVAL)go(p,s,MASK_ANIMATION);else if(action.equals("stitches")&&phase(s)==STITCH_REMOVAL)go(p,s,STITCH_ANIMATION);return "Checked the current removal interaction gate.";}
        if(action.equals("knockdown")){var w=wybie(p,s);if(w!=null&&p.distanceToSqr(w)<16&&(phase(s)==ESCAPE_A||phase(s)==MASK_REMOVAL)){n.putBoolean("Punched",true);n.putLong("KnockdownAt",OtherMotherLoop.now(p));w.gameGesture(51,25);}return "Nearby non-lethal knockdown guard checked.";}
        if(action.equals("push")){var prop=FirstEscapeScene.prop(p,s,"Wardrobe");if(prop!=null)interact(p,prop);return "Actual nearby wardrobe interaction checked.";}
        if(action.equals("door")){block(p,BlockPos.of(n.getLong("LittleDoor")));return "Actual Little Door interaction gate checked.";}
        if(List.of("hide","wybie","gaze","brick","shoe","collision","reward","seal").contains(action)){
            if(!FirstEscapeTimeline.overworld(phase(s)))return "Finish the actual escape tunnel return before Overworld controls.";
            switch(action){case "hide"->missing(p,s);case "wybie"->realWybie(p,s);case "gaze"->{var w=realWybie(p,s);if(phase(s)==WYBIE_GAZE&&w!=null&&gaze(p,w))go(p,s,MISSING_B);}case "brick"->{brickDoor(p,s);var d=sourceDoor(p,s);if(d!=null)safeTransfer(p,FamilyData.level(p.server,d.dimension),d.pos);}case "shoe"->{if(phase(s)==THROW_SHOE)ensureTool(p,s,true);}case "collision"->throwShoe(p,p.getMainHandItem());case "reward"->payChaseReward(p,s);case "seal"->{n.putBoolean("OtherWorldSealed",true);brickDoor(p,s);}default->{}}
            OtherMotherLoop.changed(p);return "Owner-scoped control checked; normal collision, proximity and once-only reward guards remain active.";
        }
        if(action.equals("recover")){n.putBoolean("Recover",true);return "Current safe recording checkpoint will replay; completed interactions are preserved.";}
        if(action.equals("scene")){if(FirstEscapeTimeline.overworld(phase(s))){var d=sourceDoor(p,s);if(d!=null)safeTransfer(p,FamilyData.level(p.server,d.dimension),d.pos);}else safeTransfer(p,p.server.getLevel(OtherWorld.KEY),FirstEscapeTimeline.prison(phase(s))?FirstEscapeScene.prison(s):FirstEscapeScene.room(s));return "Moved to current scene.";}
        if(action.equals("reset")){
            ShowPlayback.stop(p,s);n.putBoolean("InternalTransfer",true);p.stopRiding();p.setForcedPose(null);cleanupPassage(p,s);removeTool(p,s,false);removeTool(p,s,true);FirstEscapeScene.restore(p,s);var originalDoor=sourceDoor(p,s);if(originalDoor!=null&&n.contains("BrickOriginal")){var source=FamilyData.level(p.server,originalDoor.dimension);if(source!=null){var state=source.getBlockState(originalDoor.pos);if(state.is(com.coraline.registry.ModBlocks.TINY_DOOR.get())){var behind=originalDoor.pos.relative(state.getValue(com.coraline.block.TinyDoorBlock.FACING).getOpposite());if(source.getBlockState(behind).is(Blocks.BRICKS))source.setBlock(behind,NbtUtils.readBlockState(source.holderLookup(net.minecraft.core.registries.Registries.BLOCK),n.getCompound("BrickOriginal")),2);}}}
            var discard=new ArrayList<Entity>();for(var l:p.server.getAllLevels())for(var entity:l.getAllEntities())if(entity instanceof CinematicPropEntity prop&&prop.owned(s)&&prop.kind()>=CinematicPropEntity.STORY_CANE||entity instanceof WybieBicycleEntity bike&&storyBike(bike)&&s.owner.equals(bike.getPersistentData().getUUID("FirstEscapeOwner")))discard.add(entity);for(var entity:discard)entity.discard();
            ShowActors.restore(p,s);var family=ParentAccess.family(p.server,s.owner);if(family!=null){family.storyMissing=false;FamilyData.get(p.server).setDirty();}var bond=com.coraline.doll.DollBondData.get(p.server).bonds.get(s.owner);if(bond!=null){bond.storyMissing=false;com.coraline.doll.DollBondData.get(p.server).setDirty();}
            s.loop.story.remove("FirstEscape");OtherMotherLoop.changed(p);return "Continuation reset and journalled scene restored. Story rewards and original proposal progression were not rolled back.";
        }
        if(action.startsWith("phase_")){try{var next=FirstEscapeTimeline.Phase.valueOf(action.substring(6).toUpperCase(Locale.ROOT));if(FirstEscapeTimeline.overworld(next)&&!FirstEscapeTimeline.overworld(phase(s)))return "Return through the actual escape tunnel before previewing Overworld checkpoints.";if(!FirstEscapeTimeline.overworld(next)&&FirstEscapeTimeline.overworld(phase(s)))return "Reset and begin at the actual story boundary before previewing Other World checkpoints.";go(p,s,next);n.putBoolean("Recover",true);return "Checkpoint selected. Voice completion and interaction gates still apply.";}catch(IllegalArgumentException invalid){return "Unknown checkpoint.";}}
        return "Unknown continuation control.";
    }
    private static boolean inView(ServerPlayer p,Mob actor){var delta=actor.getEyePosition().subtract(p.getEyePosition());return p.hasLineOfSight(actor)&&p.getLookAngle().dot(delta.normalize())>.2;}
    private static void payChaseReward(ServerPlayer p,VisitData.Scene s){var n=data(s);if(!n.getBoolean("ChaseRewardEarned")||n.getBoolean("ChaseRewardPaid"))return;n.putBoolean("ChaseRewardPaid",true);s.loop.story.putBoolean("FirstEscapeRewardPaid",true);OtherMotherLoop.changed(p);}
    @SubscribeEvent public static void respawn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p){var s=OtherMotherLoop.scene(p);if(engaged(s))data(s).putBoolean("Recover",true);}}
    @SubscribeEvent public static void storyToolDrops(net.minecraftforge.event.entity.living.LivingDropsEvent e){if(e.getEntity() instanceof ServerPlayer p){var s=OtherMotherLoop.scene(p);if(engaged(s))e.getDrops().removeIf(item->validTool(s,item.getItem(),false)||validTool(s,item.getItem(),true));}}
    @SubscribeEvent public static void storyToolPickup(net.minecraftforge.event.entity.player.EntityItemPickupEvent e){var stack=e.getItem().getItem();var tag=stack.getTag();if((stack.is(FirstEscapeItems.CANE.get())||stack.is(FirstEscapeItems.SHOE.get()))&&tag!=null&&tag.hasUUID("StoryOwner")&&!tag.getUUID("StoryOwner").equals(e.getEntity().getUUID()))e.setCanceled(true);}
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST) public static void headInteraction(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific e){if(!(e.getEntity() instanceof ServerPlayer p))return;var s=OtherMotherLoop.scene(p);if(!engaged(s)||e.getTarget()!=ShowActors.actor(p,s,"other_wybie")||p.distanceToSqr(e.getTarget())>16)return;e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);if(e.getHand()!=InteractionHand.MAIN_HAND)return;if(e.getLocalPos().y<e.getTarget().getBbHeight()*.65){hint(p,"Reach for the stranger's face.");return;}if(phase(s)==MASK_REMOVAL){p.swing(InteractionHand.MAIN_HAND,true);go(p,s,MASK_ANIMATION);}else if(phase(s)==STITCH_REMOVAL){p.swing(InteractionHand.MAIN_HAND,true);go(p,s,STITCH_ANIMATION);}}
    @SubscribeEvent public static void protectSealedDoor(net.minecraftforge.event.level.BlockEvent.BreakEvent e){if(!(e.getPlayer() instanceof ServerPlayer p))return;var s=OtherMotherLoop.scene(p);if(!engaged(s)||!data(s).getBoolean("OtherWorldSealed"))return;var d=sourceDoor(p,s);if(d==null||FamilyData.level(p.server,d.dimension)!=p.serverLevel())return;var frame=p.serverLevel().getBlockState(d.pos);if(e.getPos().equals(d.pos)||frame.is(com.coraline.registry.ModBlocks.TINY_DOOR.get())&&e.getPos().equals(d.pos.relative(frame.getValue(com.coraline.block.TinyDoorBlock.FACING).getOpposite())))e.setCanceled(true);}
    private FirstEscapeStory(){}
}
