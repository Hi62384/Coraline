package com.coraline.feast;

import java.util.*;
import com.coraline.otherworld.*;
import com.coraline.parents.ParentEntity;
import com.coraline.secrets.SecretsNetwork;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import static com.coraline.feast.AcceptedButtonEyesProgress.Phase.*;

/** Alternate subtrack of the existing owner's story director, ending at the ghosts. */
@Mod.EventBusSubscriber(modid="coraline")
public final class AcceptedButtonEyes {
    public static final String INVITATION="Come along, dear. It's time for your new eyes.";
    public static final String FINISHED="There we are, dear. All finished. Come and meet me upstairs.";
    public static final String REVEAL_A="Those buttons weren't a gift, dear. They were how I claimed your soul.";
    public static final String REVEAL_B="You aren't a living child to me any longer. I will feed on what remains of your life.";
    public static final String REVEAL_C="And now there's nothing left for you to do. I think I'll keep what's left of you.";
    public static final String ACCEPTED_GHOST_E="mirror_prison_e_accepted";
    public static final String HOUSE_AMBIENCE="accepted_ghost_children_music";
    public static boolean active(VisitData.Scene s){return AcceptedButtonEyesProgress.active(s);}
    public static CompoundTag data(VisitData.Scene s){return AcceptedButtonEyesProgress.data(s);}
    public static OtherMotherWelcome.State dialogueState(VisitData.Scene s){return OtherMotherWelcome.State.HIGH_TRUST_LOW_SUSPICION;}
    public static boolean begin(ServerPlayer p,VisitData.Scene s,AcceptedButtonEyesProgress.Route route){
        if(s==null||!p.getUUID().equals(s.owner))return false;if(active(s))return true;
        var r=RealmData.get(p.server).realms.get(s.door);var level=p.server.getLevel(OtherWorld.KEY);
        if(r==null||r.house==null||level==null)return false;
        // Clean the old track before its scenery context changes. Keep its
        // durable quest/custody data for a later restoration implementation.
        ShowPlayback.stop(p,s);VisitAudio.stop(p);RescueBosses.hideBar(p);FirstEscapeScene.restore(p,s);
        var oldSerial=s.loop.event.getInt("Serial");s.loop.event=new CompoundTag();s.loop.event.putInt("Serial",oldSerial+1);s.loop.event.putString("Type",VisitEvents.Type.ACTIVITY.name());
        if(!AcceptedButtonEyesProgress.begin(data(s),route,s.door,VisitConfig.ACCEPTED_ACTIVITY_COUNT.get(),OtherMotherLoop.now(p)))return true;
        s.welcomeState=dialogueState(s);
        data(s).putUUID("Session",UUID.randomUUID());data(s).putBoolean("SeatRelease",true);
        com.coraline.secrets.Reconciliation.cancel(p);s.loop.secrets.search=com.coraline.secrets.SecretsState.Search.NORMAL;s.loop.secrets.pendingTicks=0;s.loop.secrets.searchTicks=0;s.loop.secrets.dialogue=new CompoundTag();
        var scenery=AcceptedButtonEyesProgress.scenery(s);scenery.putLong("House",r.house.asLong());scenery.putLong("LittleDoor",r.doorPos.asLong());scenery.putLong("Prison",r.house.above(80).asLong());
        p.getPersistentData().putUUID("CoralineVisitDoor",s.door);p.getPersistentData().remove("CoralineInTunnel");p.getPersistentData().remove("CoralineReturning");
        AcceptedButtonEyesInventory.closeForScene(p,s);p.stopRiding();p.setForcedPose(null);p.setHealth(p.getMaxHealth());p.clearFire();p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
        if(p.level()!=level&&!AcceptedButtonEyesWorld.transfer(p,s,AcceptedButtonEyesWorld.upstairs(s)))data(s).putBoolean("Recover",true);
        s.loop.phase=OtherMotherLoopState.Phase.ACCEPTED_BUTTON_EYES;s.loop.story.putString("MajorStoryPhase","BUTTON_EYES_BASEMENT_INVITATION");
        if(route==AcceptedButtonEyesProgress.Route.FINAL_BOSS_LOSS){data(s).putBoolean("BossCombatEnded",true);AcceptedButtonEyesWorld.transfer(p,s,AcceptedButtonEyesWorld.upstairs(s));}
        OtherMotherLoop.changed(p);sync(p,s);return true;
    }
    static void go(ServerPlayer p,VisitData.Scene s,AcceptedButtonEyesProgress.Phase next){
        ShowPlayback.stop(p,s);AcceptedButtonEyesProgress.move(data(s),next,OtherMotherLoop.now(p));
        if(next==APPROACH_CHAIR)data(s).putLong("SewingMusicStarted",OtherMotherLoop.now(p));
        if(next==MIRROR_IMPRISONMENT)VisitAudio.stopMusic(p,HOUSE_AMBIENCE);
        else if(next==MEET_UPSTAIRS)VisitAudio.play(p,HOUSE_AMBIENCE,true);
        s.loop.phase=OtherMotherLoopState.Phase.ACCEPTED_BUTTON_EYES;s.loop.story.putString("MajorStoryPhase",next.name());OtherMotherLoop.changed(p);sync(p,s);
    }
    public static boolean tick(ServerPlayer p,VisitData.Scene s){
        if(!active(s)||!p.getUUID().equals(s.owner))return false;s.welcomeState=dialogueState(s);var n=data(s);var phase=AcceptedButtonEyesProgress.phase(n);long age=Math.max(0,OtherMotherLoop.now(p)-n.getLong("PhaseStarted"));
        if(GhostEscapeEnding.epilogueActive(s)){GhostEscapeEnding.tick(p,s,GhostVictimEntry.data(s));return true;}
        if(!GhostEscapeProgress.bodyRestored(GhostVictimEntry.data(s)))p.fallDistance=0;
        if(GhostVictimDirector.tick(p,s)){if(p.tickCount%10==0)sync(p,s);return true;}
        if(n.getBoolean("SewingCompleted")){
            if(!AcceptedButtonEyesInventory.capture(p,s)){hint(p,n.getString("Blocker"));return true;}
            AcceptedButtonEyesWorld.sealAndRemoveBed(p,s);if(p.tickCount%20==0){restSewingProps(p,s);AcceptedButtonEyesInventory.enforce(p);}
        }
        // The ambient channel ignores repeated cues while already playing.
        // Resume it after ordinary activity music has finished at house return.
        if(n.getBoolean("SewingCompleted")&&phase.ordinal()>=MEET_UPSTAIRS.ordinal()&&OtherWorld.isOther(p.level())&&phase.ordinal()<MIRROR_IMPRISONMENT.ordinal()&&phase!=ACTIVITY_RUNNING&&phase!=ACTIVITY_REACTION&&p.tickCount%100==0)VisitAudio.play(p,HOUSE_AMBIENCE,true);
        if(p.tickCount%10==0)sync(p,s);
        if(OtherWorld.isOther(p.level())&&n.getBoolean("SewingCompleted")&&phase.ordinal()>=MEET_UPSTAIRS.ordinal()&&phase.ordinal()<WORLD_COLLAPSED.ordinal()){
            double[] radii={Math.max(260,AcceptedButtonEyesWorld.realm(p,s)==null?500:AcceptedButtonEyesWorld.realm(p,s).radius),180,130,90,55};
            double radius=radii[Math.min(4,AcceptedButtonEyesProgress.stage(n))];var home=AcceptedButtonEyesWorld.house(s);
            if(Math.hypot(p.getX()-home.getX(),p.getZ()-home.getZ())>radius-8){
                long now=OtherMotherLoop.now(p);if(!n.contains("VoidReturnAt"))n.putLong("VoidReturnAt",now+60);
                if(now>=n.getLong("VoidReturnAt")){AcceptedButtonEyesWorld.transfer(p,s,AcceptedButtonEyesWorld.upstairs(s));n.remove("VoidReturnAt");}
            }else n.remove("VoidReturnAt");
            if(s.loop.portal!=null&&Math.hypot(s.loop.portal.getX()-home.getX(),s.loop.portal.getZ()-home.getZ())>radius-12){LoopProps.choosePortal(p,s);}
        }
        if(phase==ACTIVITY_RUNNING||phase==ACTIVITY_REACTION||phase==ACTIVITY_INVITATION){
            if(phase==ACTIVITY_RUNNING&&ActivityWorld.isActivity(p.level()))return false;
            if(s.loop.phase==OtherMotherLoopState.Phase.COMPLETE)return false;
            if(phase==ACTIVITY_INVITATION){
                if(!Set.of(OtherMotherLoopState.Phase.PROPOSAL,OtherMotherLoopState.Phase.WARNING,OtherMotherLoopState.Phase.WINDOW,OtherMotherLoopState.Phase.ARRIVAL,OtherMotherLoopState.Phase.READY).contains(s.loop.phase))OtherMotherLoop.nextActivity(p,s);
                return false;
            }
            // A transfer interrupted before the normal return cannot count as
            // a completed activity, even if the ordinary director recovers it.
            if(phase==ACTIVITY_RUNNING&&OtherWorld.isOther(p.level())||phase==ACTIVITY_REACTION&&s.loop.phase!=OtherMotherLoopState.Phase.COMPLETE){ActivityWorld.abortAccepted(p,s);return true;}
        }
        if(!OtherWorld.isOther(p.level())){AcceptedButtonEyesWorld.transfer(p,s,phase.ordinal()>=MIRROR_IMPRISONMENT.ordinal()?FirstEscapeScene.prison(s):AcceptedButtonEyesWorld.upstairs(s));return true;}
        if(n.getBoolean("Recover")){n.remove("Recover");if(phase.ordinal()>=MIRROR_IMPRISONMENT.ordinal())AcceptedButtonEyesWorld.transfer(p,s,FirstEscapeScene.prison(s));else if(phase.ordinal()>=APPROACH_CHAIR.ordinal()&&phase.ordinal()<=PROCEDURE_COMPLETE.ordinal())AcceptedButtonEyesWorld.transfer(p,s,AcceptedButtonEyesWorld.chair(s).south());}
        if(phase.ordinal()<=PROCEDURE_COMPLETE.ordinal()&&!AcceptedButtonEyesWorld.prepare(p,s))return true;
        switch(phase){
            case BASEMENT_INVITATION->{var m=AcceptedButtonEyesWorld.mother(p,s);if(m!=null){m.setInvisible(false);m.familyGesture(66,5);m.lookAtStoryPlayer(p);}if(voice(p,s,INVITATION,age))go(p,s,WAITING_TO_SIT);}
            case WAITING_TO_SIT->{
                if(!AcceptedButtonEyesWorld.prepare(p,s)||!AcceptedButtonEyesWorld.parents(p,s))return true;
                var mother=AcceptedButtonEyesWorld.mother(p,s);if(mother!=null){mother.setInvisible(false);mother.familyGesture(66,5);walk(mother,AcceptedButtonEyesWorld.chair(s).east(),age);}
                hint(p,p.distanceToSqr(AcceptedButtonEyesWorld.chair(s).getCenter())<16?"Sit Down":"Go to the basement.");
            }
            case APPROACH_CHAIR->{holdSeat(p,s);var m=AcceptedButtonEyesWorld.mother(p,s);if(m!=null)walk(m,AcceptedButtonEyesWorld.chair(s).east(),age);if(sewingBeat(p,s,.12))go(p,s,TAKE_BUTTONS);}
            case TAKE_BUTTONS->{holdSeat(p,s);props(p,s,phase,(int)age);if(sewingBeat(p,s,.26))go(p,s,TAKE_NEEDLE);}
            case TAKE_NEEDLE->{holdSeat(p,s);props(p,s,phase,(int)age);if(sewingBeat(p,s,.40))go(p,s,RAISE_NEEDLE);}
            case RAISE_NEEDLE->{holdSeat(p,s);props(p,s,phase,(int)age);if(sewingBeat(p,s,.58)){n.putBoolean("NeedleReached",true);go(p,s,BLACKOUT);}}
            case BLACKOUT->{
                holdSeat(p,s);if(sewingFinale(p,s)){if(!AcceptedButtonEyesInventory.capture(p,s))return true;n.putBoolean("SewingCompleted",true);AcceptedButtonEyesWorld.sealAndRemoveBed(p,s);go(p,s,PROCEDURE_COMPLETE);}
            }
            case PROCEDURE_COMPLETE->{holdSeat(p,s);boolean finished=voice(p,s,FINISHED,age);if(sewingBeat(p,s,1)){n.putBoolean("SeatRelease",true);p.stopRiding();go(p,s,MEET_UPSTAIRS);}}
            case MEET_UPSTAIRS->{
                var m=AcceptedButtonEyesWorld.mother(p,s);var target=AcceptedButtonEyesWorld.upstairs(s);
                if(m!=null){m.setInvisible(false);m.setGlowingTag(true);m.familyGesture(0,5);walk(m,target,age);}hint(p,"Meet Other Mother upstairs - follow her glowing outline.");
                if(m!=null&&p.distanceToSqr(target.getCenter())<36&&m.distanceToSqr(target.getCenter())<9){m.setGlowingTag(false);go(p,s,ACTIVITY_INVITATION);}
            }
            case WORLD_COLLAPSED->{
                var m=AcceptedButtonEyesWorld.mother(p,s);var target=AcceptedButtonEyesWorld.upstairs(s);if(m!=null)walk(m,target,age);hint(p,"Find Other Mother.");
                if(m!=null&&p.distanceToSqr(m)<16){n.putBoolean("BeldamRevealed",true);n.putInt("RevealLine",0);go(p,s,BELDAM_REVEAL);}
            }
            case BELDAM_REVEAL->{
                var m=AcceptedButtonEyesWorld.mother(p,s);if(m!=null){m.getNavigation().stop();m.setDeltaMovement(Vec3.ZERO);m.familyGesture(67,5);m.lookAtStoryPlayer(p);}
                String[] words={REVEAL_A,REVEAL_B,REVEAL_C};int line=Math.min(2,n.getInt("RevealLine"));
                if(voice(p,s,words[line],age)){if(line==2)go(p,s,MIRROR_IMPRISONMENT);else{n.putInt("RevealLine",line+1);go(p,s,BELDAM_REVEAL);}}
            }
            case MIRROR_IMPRISONMENT->{
                p.setDeltaMovement(Vec3.ZERO);var m=AcceptedButtonEyesWorld.mother(p,s);if(m!=null)m.familyGesture(42,5);
                if(age>=30){FirstEscapeScene.preparePrison(p,s);if(AcceptedButtonEyesWorld.transfer(p,s,FirstEscapeScene.prison(s))&&age>=50)go(p,s,GHOST_C);}
            }
            case GHOST_C,GHOST_D,GHOST_E->{
                if(RescueEyes.ledger(s).getBoolean("Released")){n.putBoolean("GhostScenePlayed",true);go(p,s,ACCEPTED_BUTTON_EYES_MIRROR_PRISON);return true;}
                var original=phase==GHOST_C?FirstEscapeTimeline.Phase.MIRROR_C:phase==GHOST_D?FirstEscapeTimeline.Phase.MIRROR_D:FirstEscapeTimeline.Phase.MIRROR_E;
                String sound=phase==GHOST_E?ACCEPTED_GHOST_E:FirstEscapeTimeline.sound(original);
                if(!n.getBoolean("VoiceStarted")&&ShowPlayback.start(p,s,sound,false))n.putBoolean("VoiceStarted",true);
                boolean done=ShowPlayback.tick(p,s);int ticks=ShowPlayback.audiblePosition(s)+(phase==GHOST_E?232:0);
                FirstEscapeStory.acceptedPrisonFrame(p,s,original,ticks);
                if(done||age>Math.max(1200,ShowRecordings.duration(sound)+600)){
                    if(phase==GHOST_C)go(p,s,GHOST_D);else if(phase==GHOST_D)go(p,s,GHOST_E);else{n.putBoolean("GhostScenePlayed",true);go(p,s,ACCEPTED_BUTTON_EYES_MIRROR_PRISON);}
                }
            }
            case ACCEPTED_BUTTON_EYES_MIRROR_PRISON->{
                FirstEscapeStory.acceptedPrisonStill(p,s);if(p.distanceToSqr(FirstEscapeScene.prison(s).getCenter())>100)AcceptedButtonEyesWorld.transfer(p,s,FirstEscapeScene.prison(s));
                // No new quest, rescue, return, inventory restore or boss loop.
            }
            default->{}
        }
        if(n.getBoolean("SewingCompleted")&&AcceptedButtonEyesProgress.stage(n)>=4&&phase.ordinal()<MIRROR_IMPRISONMENT.ordinal()){
            var r=AcceptedButtonEyesWorld.realm(p,s);if(r!=null&&OtherWorldEstate.outsideHouse(r.house,p.blockPosition()))AcceptedButtonEyesWorld.transfer(p,s,AcceptedButtonEyesWorld.upstairs(s));
        }
        return true;
    }
    private static boolean voice(ServerPlayer p,VisitData.Scene s,String text,long age){
        var n=data(s);String sound=IncidentalDialogue.sound("Other Mother",text);
        if(sound!=null&&!n.getBoolean("VoiceStarted")&&ShowPlayback.start(p,s,sound,false))n.putBoolean("VoiceStarted",true);
        if(n.getBoolean("VoiceStarted")&&ShowPlayback.tick(p,s))return true;
        int duration=sound==null?0:ShowRecordings.duration(sound);
        if(age>Math.max(600,duration+600)){n.putString("LastSafetySkip","Voice: "+text);return true;}return false;
    }
    private static void hint(ServerPlayer p,String text){if(p.tickCount%20==0)p.displayClientMessage(Component.literal(text),true);}
    private static boolean sewingFinale(ServerPlayer p,VisitData.Scene s){
        String voice=IncidentalDialogue.sound("Other Mother",FINISHED);int length=Math.max(300,VisitAudio.length("accepted_little_me_music"));
        int finale=Math.max(100,voice==null?100:ShowRecordings.duration(voice)+4);
        return sewingBeat(p,s,Math.min(.82,Math.max(.58,1-finale/(double)length)));
    }
    private static boolean sewingBeat(ServerPlayer p,VisitData.Scene s,double fraction){
        var n=data(s);if(!n.contains("SewingMusicStarted"))n.putLong("SewingMusicStarted",OtherMotherLoop.now(p));
        return OtherMotherLoop.now(p)-n.getLong("SewingMusicStarted")>=Math.round(Math.max(300,VisitAudio.length("accepted_little_me_music"))*fraction);
    }
    private static boolean walk(ParentEntity actor,BlockPos target,long age){
        actor.stopRiding();var goal=Vec3.atBottomCenterOf(target);actor.setNoAi(false);actor.setNoGravity(false);actor.noPhysics=false;actor.setMaxUpStep(1.05F);
        if(actor.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation ground){ground.setCanOpenDoors(true);ground.setCanPassDoors(true);}
        if(actor.distanceToSqr(goal)<1){actor.getNavigation().stop();return true;}if(actor.tickCount%10==0)actor.getNavigation().moveTo(goal.x,goal.y,goal.z,1.05);
        if(age>400&&actor.level() instanceof ServerLevel level&&level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,actor.getBoundingBox().inflate(32),viewer->viewer.hasLineOfSight(actor)).isEmpty()){var safe=AcceptedButtonEyesWorld.landing(level,target);if(safe!=null&&Math.abs(safe.getY()-target.getY())<=2){actor.getNavigation().stop();actor.teleportTo(safe.getX()+.5,safe.getY(),safe.getZ()+.5);return true;}}return false;
    }
    private static void holdSeat(ServerPlayer p,VisitData.Scene s){
        var seat=FirstEscapeScene.prop(p,s,"SewingChair");
        if(seat==null){AcceptedButtonEyesWorld.prepare(p,s);seat=FirstEscapeScene.prop(p,s,"SewingChair");}
        if(seat!=null&&p.getVehicle()!=seat)p.startRiding(seat,true);
    }
    private static void props(ServerPlayer p,VisitData.Scene s,AcceptedButtonEyesProgress.Phase phase,int age){
        var m=AcceptedButtonEyesWorld.mother(p,s);if(m==null)return;
        m.setInvisible(false);m.getNavigation().stop();m.setDeltaMovement(Vec3.ZERO);m.lookAtStoryPlayer(p);m.familyGesture(phase==RAISE_NEEDLE?61:81,5);
        var buttons=FirstEscapeScene.prop(p,s,"SewingButtons");var needle=FirstEscapeScene.prop(p,s,"SewingNeedle");
        if(buttons!=null&&phase!=TAKE_BUTTONS)buttons.setInvisible(true);
        if(needle!=null&&phase!=TAKE_BUTTONS){
            var hand=m.position().add(0,1.1,0);var direction=p.getEyePosition().subtract(hand).normalize();if(direction.lengthSqr()<.001)direction=new Vec3(0,1,0);
            Vec3 target=phase==RAISE_NEEDLE?p.getEyePosition().subtract(direction.scale(.33)):hand;
            needle.grabTarget(direction);
            needle.setPos(needle.position().lerp(target,Math.min(1,(age+1)/25.)));needle.frame(phase.ordinal(),age,1);
        }
    }
    private static void restSewingProps(ServerPlayer p,VisitData.Scene s){
        if(!AcceptedButtonEyesProgress.scenery(s).contains("SewingChairAt"))return;
        var needle=FirstEscapeScene.prop(p,s,"SewingNeedle");if(needle!=null){needle.setPos(Vec3.atBottomCenterOf(AcceptedButtonEyesWorld.chair(s).east(2)).add(.25,.95,0));needle.frame(0,0,1);}
        var buttons=FirstEscapeScene.prop(p,s,"SewingButtons");if(buttons!=null)buttons.discard();
    }
    public static boolean interact(ServerPlayer p,VisitData.Scene s,CinematicPropEntity prop){
        if(!active(s)||!prop.owned(s)||!p.getUUID().equals(s.owner))return false;
        if(prop!=FirstEscapeScene.prop(p,s,"SewingChair"))return false;
        if(AcceptedButtonEyesProgress.phase(data(s))==WAITING_TO_SIT&&p.distanceToSqr(prop)<16&&p.startRiding(prop,true)){
            var m=AcceptedButtonEyesWorld.mother(p,s);var beside=AcceptedButtonEyesWorld.chair(s).east();
            if(m!=null)m.setInvisible(false);
            p.setForcedPose(null);p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;data(s).putBoolean("SeatRelease",false);go(p,s,APPROACH_CHAIR);
        }return true;
    }
    public static boolean parentControl(ParentEntity actor){
        if(!actor.getPersistentData().hasUUID("AcceptedOwner"))return false;
        var p=actor.getServer().getPlayerList().getPlayer(actor.getPersistentData().getUUID("AcceptedOwner"));if(p==null)return true;
        var s=OtherMotherLoop.scene(p);if(!active(s)||!Objects.equals(actor.getUUID(),actor.isMom()?s.mom:s.dad))return false;
        if(GhostVictimDirector.parent(actor,p,s))return true;
        var phase=AcceptedButtonEyesProgress.phase(data(s));
        actor.setInvisible(false);actor.eventGaze(true);actor.setNoAi(false);actor.setNoGravity(false);actor.noPhysics=false;
        if(!actor.isMom()){actor.getNavigation().stop();actor.familyGesture(82,5);var m=AcceptedButtonEyesWorld.mother(p,s);if(m!=null&&actor.tickCount%120<25)actor.getLookControl().setLookAt(m,20,15);return true;}
        if(phase==BASEMENT_INVITATION){actor.getNavigation().stop();actor.lookAtStoryPlayer(p);}
        if(phase==BELDAM_REVEAL){actor.getNavigation().stop();actor.setDeltaMovement(Vec3.ZERO);actor.lookAtStoryPlayer(p);}
        if(phase==WAITING_TO_SIT)actor.getLookControl().setLookAt(p,25,20);
        if(AcceptedButtonEyesProgress.stage(data(s))>=2&&phase==ACTIVITY_INVITATION)actor.familyGesture(80,5);
        return phase!=ACTIVITY_INVITATION&&phase!=ACTIVITY_RUNNING&&phase!=ACTIVITY_REACTION;
    }
    static OtherMotherActivities.Activity nextActivity(VisitData.Scene s){
        var n=data(s);var order=n.getList("ActivityOrder",Tag.TAG_STRING);
        if(order.isEmpty()){
            var enabled=new ArrayList<String>(VisitConfig.ACCEPTED_ACTIVITIES.get());
            if(enabled.isEmpty()||enabled.equals(List.of("archery_contest","garden_maze","dream_parkour","fishing_contest","ice_boat_race")))
                enabled=Arrays.stream(OtherMotherActivities.Activity.values()).map(OtherMotherActivities.Activity::id).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            // Save one permutation for this branch. The five default invitations
            // can now draw from every enabled activity without repeating one.
            var session=n.hasUUID("Session")?n.getUUID("Session"):s.door;
            Collections.shuffle(enabled,new Random(session.getMostSignificantBits()^session.getLeastSignificantBits()));
            for(String id:enabled)order.add(StringTag.valueOf(id));
            n.put("ActivityOrder",order);
        }
        int cursor=n.contains("ActivityCursor")?n.getInt("ActivityCursor"):AcceptedButtonEyesProgress.completed(n);
        String id=order.getString(Math.floorMod(cursor,order.size()));
        return OtherMotherActivities.Activity.valueOf(id.toUpperCase(Locale.ROOT));
    }
    static void activityEntered(ServerPlayer p,VisitData.Scene s){if(active(s)){
        if(AcceptedButtonEyesProgress.openActivity(data(s),s.loop.activity.id(),UUID.randomUUID(),OtherMotherLoop.now(p))){var n=data(s);n.putInt("ActivityCursor",(n.contains("ActivityCursor")?n.getInt("ActivityCursor"):AcceptedButtonEyesProgress.completed(n))+1);}OtherMotherLoop.changed(p);
    }}
    static boolean activityFinished(VisitData.Scene s){return ActivityWorld.groupFinished(s);}
    static void activityReturning(ServerPlayer p,VisitData.Scene s){if(active(s))AcceptedButtonEyesInventory.confiscateActivityLoot(p,s);if(active(s)&&activityFinished(s)&&data(s).hasUUID("ActivityInstance")){
        AcceptedButtonEyesProgress.gameCompleted(data(s),data(s).getUUID("ActivityInstance"));OtherMotherLoop.changed(p);
    }}
    static void activityReturned(ServerPlayer p,VisitData.Scene s){if(active(s)&&OtherWorld.isOther(p.level())&&data(s).hasUUID("ActivityInstance")){
        VisitAudio.play(p,HOUSE_AMBIENCE,true);
        AcceptedButtonEyesProgress.normalReturn(data(s),data(s).getUUID("ActivityInstance"),OtherMotherLoop.now(p));OtherMotherLoop.changed(p);
    }}
    static boolean afterReaction(ServerPlayer p,VisitData.Scene s){if(!active(s))return false;
        AcceptedButtonEyesInventory.cleanTemporary(p);
        if(!AcceptedButtonEyesProgress.commitReaction(data(s),OtherMotherLoop.now(p)))AcceptedButtonEyesProgress.abortActivity(data(s),OtherMotherLoop.now(p));
        s.welcomeState=dialogueState(s);s.loop.phase=OtherMotherLoopState.Phase.ACCEPTED_BUTTON_EYES;OtherMotherLoop.changed(p);sync(p,s);return true;
    }
    public static boolean sealed(ServerPlayer p){var s=OtherMotherLoop.scene(p);return active(s)&&data(s).getBoolean("SewingCompleted")&&!GhostEscapeProgress.bodyRestored(GhostVictimEntry.data(s));}
    static void recoverFailure(ServerPlayer p,String message){var s=OtherMotherLoop.scene(p);if(!active(s)||GhostEscapeProgress.bodyRestored(GhostVictimEntry.data(s)))return;
        data(s).putString("LastSafetyRecovery",message);var phase=AcceptedButtonEyesProgress.phase(data(s));
        if(phase==ACTIVITY_INVITATION||phase==ACTIVITY_RUNNING||phase==ACTIVITY_REACTION)ActivityWorld.abortAccepted(p,s);else data(s).putBoolean("Recover",true);
        OtherMotherLoop.changed(p);
    }
    @SubscribeEvent public static void commands(net.minecraftforge.event.RegisterCommandsEvent e){e.getDispatcher().register(net.minecraft.commands.Commands.literal("coraline_secrets_debug").requires(source->source.hasPermission(2)).executes(context->{SecretsNetwork.open(context.getSource().getPlayerOrException(),true,false);return 1;}));}
    @SubscribeEvent public static void breakDoor(net.minecraftforge.event.level.BlockEvent.BreakEvent e){if(e.getPlayer() instanceof ServerPlayer p&&sealed(p)&&OtherWorld.isOther(p.level())){
        var s=OtherMotherLoop.scene(p);var r=AcceptedButtonEyesWorld.realm(p,s);if(r==null)return;var door=p.level().getBlockState(r.doorPos);
        if(e.getPos().equals(r.doorPos)||door.hasProperty(com.coraline.block.TinyDoorBlock.FACING)&&e.getPos().equals(r.doorPos.relative(door.getValue(com.coraline.block.TinyDoorBlock.FACING).getOpposite())))e.setCanceled(true);
    }}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p){var s=OtherMotherLoop.scene(p);if(active(s)){
        if(GhostEscapeEnding.epilogueActive(s)){RescueChapter.sync(p,s);return;}
        if(GhostEscapeProgress.bodyRestored(GhostVictimEntry.data(s))){sync(p,s);return;}
        var n=data(s);n.putBoolean("Recover",true);n.remove("VoiceStarted");n.putLong("PhaseStarted",OtherMotherLoop.now(p));ShowPlayback.stop(p,s);sync(p,s);OtherMotherLoop.changed(p);
    }}}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){var s=OtherMotherLoop.scene(p);if(active(s)){ShowPlayback.stop(p,s);data(s).remove("VoiceStarted");OtherMotherLoop.changed(p);}}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void damage(net.minecraftforge.event.entity.living.LivingDamageEvent e){if(e.getEntity() instanceof ServerPlayer p){var s=OtherMotherLoop.scene(p);if(active(s)&&!GhostEscapeProgress.bodyRestored(GhostVictimEntry.data(s))&&e.getAmount()>=p.getHealth()){
        e.setCanceled(true);p.setHealth(p.getMaxHealth());p.clearFire();data(s).putBoolean("Recover",true);OtherMotherLoop.changed(p);
    }}}
    @SubscribeEvent public static void mount(EntityMountEvent e){if(e.isDismounting()&&e.getEntityMounting() instanceof ServerPlayer p){
        var s=OtherMotherLoop.scene(p);if(active(s)&&!GhostEscapeProgress.bodyRestored(GhostVictimEntry.data(s))&&!data(s).getBoolean("SeatRelease")&&e.getEntityBeingMounted()==FirstEscapeScene.prop(p,s,"SewingChair"))e.setCanceled(true);
    }}
    @SubscribeEvent public static void interactBlock(PlayerInteractEvent.RightClickBlock e){if(e.getEntity() instanceof ServerPlayer p&&sealed(p)){
        var s=OtherMotherLoop.scene(p);var r=AcceptedButtonEyesWorld.realm(p,s);
        if(r!=null&&OtherWorld.isOther(p.level())&&(e.getPos().equals(r.doorPos)||p.level().getBlockState(e.getPos()).getBlock() instanceof BedBlock)){e.setCanceled(true);e.setCancellationResult(InteractionResult.FAIL);hint(p,"The Little Door is sealed.");}
    }}
    public static final String[] DEBUG_ACTIONS={"accepted_proposal","boss_loss","basement","sewing","sewing_complete","blackout","inventory","activity_hotbar","sealed_door","stage","transparency","activity_complete","ruined_house","beldam","prison","ghosts","state","snapshot","reload","isolation"};
    /** Operator checkpoint after the three ghost-child lines, before manual mirror exit. */
    public static String debugAfterGhostConversation(ServerPlayer p){
        if(!p.hasPermissions(2))return "Operator permission is required.";
        var s=OtherMotherLoop.scene(p);
        if(s==null||!p.getUUID().equals(s.owner)||s.stage!=VisitData.Stage.DONE)return "Complete your own first family visit before using this checkpoint.";
        if(!active(s))return "Accept the button-eye proposal first.";
        var n=data(s);
        if(!n.getString("Route").equals(AcceptedButtonEyesProgress.Route.ACCEPTED_PROPOSAL.name()))return "This item requires the accepted proposal route.";
        if(GhostVictimEntry.data(s).getBoolean("Started")||GhostEscapeProgress.bodyRestored(GhostVictimEntry.data(s)))return "The ghost continuation has already begun.";
        if(AcceptedButtonEyesProgress.terminal(n))return "Already at the mirror prison checkpoint.";
        if(!n.getBoolean("SewingCompleted")){
            if(!AcceptedButtonEyesInventory.capture(p,s))return n.getString("Blocker");
            n.putBoolean("SewingCompleted",true);
        }
        AcceptedButtonEyesWorld.sealAndRemoveBed(p,s);
        n.putBoolean("SeatRelease",true);p.stopRiding();p.setForcedPose(null);
        FirstEscapeScene.preparePrison(p,s);
        if(!AcceptedButtonEyesWorld.transfer(p,s,FirstEscapeScene.prison(s)))return "Mirror prison landing unavailable; try again.";
        n.putBoolean("GhostScenePlayed",true);
        go(p,s,ACCEPTED_BUTTON_EYES_MIRROR_PRISON);
        OtherMotherLoop.changed(p);sync(p,s);
        return "At the mirror prison after the ghost children have spoken. Use the prison mirror to continue.";
    }
    public static String debug(ServerPlayer p,String value){
        if(!p.hasPermissions(2))return "Operator permission is required.";var parts=value.split(",",2);var action=parts[0];var s=OtherMotherLoop.scene(p);if(s==null)return "Begin the existing Other World story first.";
        if(action.equals("accepted_proposal")||action.equals("boss_loss"))return begin(p,s,action.equals("boss_loss")?AcceptedButtonEyesProgress.Route.FINAL_BOSS_LOSS:AcceptedButtonEyesProgress.Route.ACCEPTED_PROPOSAL)?"Accepted Button-Eyes Storyline started.":"The owner's Other World house is unavailable.";
        if(!active(s))return "Start the Accepted Button-Eyes Storyline first.";var n=data(s);
        if(AcceptedButtonEyesProgress.terminal(n)&&!Set.of("state","snapshot","reload","isolation").contains(action))return "This segment ends at the saved mirror-prison checkpoint. Inspect it without restarting the finished scene.";
        n.putBoolean("OperatorPreview",true);
        switch(action){
            case "basement","sewing"->{if(!AcceptedButtonEyesWorld.prepare(p,s))return n.getString("Blocker");AcceptedButtonEyesWorld.parents(p,s);AcceptedButtonEyesWorld.transfer(p,s,AcceptedButtonEyesWorld.chair(s).south());go(p,s,WAITING_TO_SIT);if(action.equals("sewing")){n.putBoolean("SeatRelease",false);holdSeat(p,s);go(p,s,APPROACH_CHAIR);}}
            case "sewing_complete","inventory","activity_hotbar"->{n.putBoolean("SewingCompleted",true);if(!AcceptedButtonEyesInventory.capture(p,s))return n.getString("Blocker");AcceptedButtonEyesWorld.sealAndRemoveBed(p,s);n.putBoolean("SeatRelease",true);p.stopRiding();go(p,s,action.equals("activity_hotbar")?ACTIVITY_INVITATION:MEET_UPSTAIRS);}
            case "blackout"->{AcceptedButtonEyesWorld.prepare(p,s);n.putBoolean("SeatRelease",false);holdSeat(p,s);go(p,s,BLACKOUT);}
            case "sealed_door"->{if(!n.getBoolean("SewingCompleted"))return "Complete sewing first; this test does not confiscate inventory silently.";AcceptedButtonEyesWorld.sealAndRemoveBed(p,s);}
            case "stage","transparency"->{int stage=parts.length>1?Integer.parseInt(parts[1]):5;if(stage<0||stage>5)return "Stage must be 0–5.";n.putInt("DebugStage",stage);}
            case "ruined_house"->{n.putInt("DebugStage",5);AcceptedButtonEyesWorld.transfer(p,s,AcceptedButtonEyesWorld.upstairs(s));}
            case "activity_complete"->{if(!ActivityWorld.isActivity(p.level())||!n.hasUUID("ActivityInstance"))return "Enter a normal branch activity first.";n.putBoolean("OperatorForcedActivity",true);switch(s.loop.activity){case ARCHERY_CONTEST->{s.loop.archeryIssued=true;s.loop.archeryShotsRemaining=0;}case GARDEN_MAZE->s.loop.gardenFinished=true;case DREAM_PARKOUR->s.loop.parkourRewarded=true;case FISHING_CONTEST->s.loop.fishingStage=5;case ICE_BOAT_RACE->{s.loop.iceStage=3;s.loop.iceResultAt=0;}default->{return "Unsupported branch activity.";}}ActivityWorld.exit(p,s);}
            case "beldam"->{n.putBoolean("BeldamRevealed",true);n.putInt("RevealLine",0);go(p,s,BELDAM_REVEAL);}
            case "prison","ghosts"->{n.putBoolean("SeatRelease",true);p.stopRiding();FirstEscapeScene.preparePrison(p,s);if(!AcceptedButtonEyesWorld.transfer(p,s,FirstEscapeScene.prison(s)))return "Mirror prison landing unavailable.";go(p,s,action.equals("ghosts")?GHOST_C:MIRROR_IMPRISONMENT);}
            case "state"->{return "Accepted Button-Eyes: "+AcceptedButtonEyesProgress.phase(n)+", completed="+AcceptedButtonEyesProgress.completed(n)+"/"+n.getInt("RequiredActivities")+", stage="+AcceptedButtonEyesProgress.stage(n)+", route="+n.getString("Route");}
            case "snapshot"->{return n.getBoolean("EscrowStored")?"Preserved inventory transaction "+n.getCompound("InventoryEscrow").getUUID("Transaction")+"; "+n.getCompound("InventoryEscrow").getList("Inventory",Tag.TAG_COMPOUND).size()+" occupied slots; no restoration in this segment.":"No inventory confiscated yet.";}
            case "reload"->{var roundtrip=s.loop.save();if(!roundtrip.getCompound("StoryProgression").getCompound(AcceptedButtonEyesProgress.KEY).equals(n))return "Checkpoint roundtrip failed.";return "In-memory checkpoint roundtrip passed; a real disconnect/server reload remains a separate verification.";}
            case "isolation"->{for(var other:VisitData.get(p.server).scenes.values())if(other!=s&&active(other)&&other.loop.story.getCompound(AcceptedButtonEyesProgress.KEY)==n)return "Isolation failure: shared branch state.";return "Owner journals are distinct; multiplayer rendering and reconnect still require native checks.";}
            default->{return "Unknown Accepted Button-Eyes control.";}
        }OtherMotherLoop.changed(p);sync(p,s);return "Accepted Button-Eyes development control applied: "+action;
    }
    private static void sync(ServerPlayer p,VisitData.Scene s){
        var n=data(s);var packet=new CompoundTag();packet.putBoolean("AcceptedButtonEyes",true);packet.putString("Phase",AcceptedButtonEyesProgress.phase(n).name());
        packet.putInt("Age",(int)Math.max(0,OtherMotherLoop.now(p)-n.getLong("PhaseStarted")));packet.putInt("Stage",n.contains("DebugStage")?n.getInt("DebugStage"):AcceptedButtonEyesProgress.stage(n));
        packet.putInt("SewingMusicLength",Math.max(300,VisitAudio.length("accepted_little_me_music")));packet.putBoolean("ButtonEyes",n.getBoolean("SewingCompleted"));packet.putBoolean("Beldam",n.getBoolean("BeldamRevealed"));
        var phase=AcceptedButtonEyesProgress.phase(n);String sound=phase==GHOST_C?"mirror_prison_c":phase==GHOST_D?"mirror_prison_d":phase==GHOST_E?ACCEPTED_GHOST_E:"";
        packet.putString("Sound",sound);packet.putInt("Ticks",ShowPlayback.audiblePosition(s));packet.putBoolean("ClockRunning",ShowPlayback.started(s));
        var m=AcceptedButtonEyesWorld.mother(p,s);packet.putInt("Mother",m==null?-1:m.getId());packet.putLong("House",AcceptedButtonEyesWorld.house(s).asLong());
        var realm=AcceptedButtonEyesWorld.realm(p,s);packet.putInt("RealmRadius",realm==null?500:realm.radius);
        if(GhostVictimDirector.active(s)&&GhostVictimProgress.phase(GhostVictimEntry.data(s))!=GhostVictimProgress.Phase.ENTER_MIRROR){packet.putString("Phase","NEXT_VICTIM");packet.putFloat("StageFloat",(float)GhostVictimWorld.deterioration(GhostVictimEntry.data(s)));packet.putBoolean("Beldam",false);}
        if(GhostEscapeProgress.bodyRestored(GhostVictimEntry.data(s))){packet.putString("Phase","RESTORED");packet.putBoolean("ButtonEyes",false);packet.putBoolean("Beldam",false);packet.putInt("Stage",0);packet.putFloat("StageFloat",0);packet.putString("Sound","");packet.putBoolean("ClockRunning",false);}
        SecretsNetwork.send(p,packet);
    }
    private AcceptedButtonEyes(){}
}
