package com.coraline.welcome;

import com.coraline.feast.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class WelcomeAudioGameTests {
    @GameTest(template="empty",timeoutTicks=2000) public static void arrivalAndCompletionDeferTheirContinuations(GameTestHelper h) {
        var server=h.getLevel().getServer();var level=server.getLevel(com.coraline.otherworld.OtherWorld.KEY);
        var player=new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ArrivalVoice"));
        level.setChunkForced(0,0,true);level.getChunk(0,0);
        var mother=com.coraline.registry.ModEntities.MOM.get().create(level);
        mother.makeOtherParent(0);mother.setNoAi(true);mother.setNoGravity(true);mother.setPos(0,120,0);
        level.addFreshEntity(mother);
        var scene=new VisitData.Scene();scene.owner=player.getUUID();scene.door=UUID.randomUUID();
        scene.mom=mother.getUUID();scene.stage=VisitData.Stage.DONE;scene.piano=BlockPos.ZERO;scene.table=BlockPos.ZERO;
        VisitData.get(server).scenes.put(scene.owner,scene);player.getPersistentData().putUUID("CoralineVisitDoor",scene.door);
        var calls=new java.util.concurrent.atomic.AtomicInteger();var greetingEnd=new java.util.concurrent.atomic.AtomicLong(-1);
        var freeCalls=new java.util.concurrent.atomic.AtomicInteger();var completionEnd=new java.util.concurrent.atomic.AtomicLong(-1);
        var chosen=OtherMotherActivities.Activity.MAGIC_GARDEN;
        var listener=new OtherMotherActivities.Listener(){
            @Override public void portalReady(net.minecraft.server.level.ServerPlayer p,OtherMotherActivities.Activity activity,long deadline){
                h.assertTrue(activity==chosen,"Selected activity changed");
                scene.welcomeState=OtherMotherWelcome.State.HIGH_TRUST_HIGH_SUSPICION;
                h.assertFalse(OtherMotherActivities.arrive(p,OtherMotherActivities.Activity.CARNIVAL),"Wrong portal accepted");
                h.assertTrue(OtherMotherActivities.arrive(p,chosen),"Arrival not accepted");
                h.assertTrue(calls.get()==0,"Activity began synchronously before greeting");
                var pool=OtherMotherActivities.arrivalPool(scene.welcomeState,OtherMotherActivities.Arrival.ON_TIME);
                h.assertTrue(scene.lastDialogue.containsKey(pool.path()),"Arrival ignored current state");
                greetingEnd.set(scene.welcomeAfter);
                h.assertFalse(OtherMotherActivities.arrive(p,chosen),"Repeated portal touch restarted greeting");
            }
            @Override public void arrived(net.minecraft.server.level.ServerPlayer p,OtherMotherActivities.Activity activity){
                h.assertTrue(server.overworld().getGameTime()>=greetingEnd.get(),"Activity started before audio finished");
                h.assertTrue(activity==chosen,"Continuation used different activity");calls.incrementAndGet();
                // The test activity finishes immediately; no actual arena/gameplay is invented here.
                scene.welcomeState=OtherMotherWelcome.State.HIGH_SUSPICION_LOW_TRUST;
                h.assertTrue(OtherMotherActivityComplete.begin(p,q->{
                    h.assertTrue(server.overworld().getGameTime()>=completionEnd.get(),"Free time began before reaction finished");
                    freeCalls.incrementAndGet();
                }),"Activity completion reaction could not begin");
                completionEnd.set(scene.welcomeAfter);
                h.assertTrue(scene.lastDialogue.containsKey(OtherMotherActivityComplete.pool(scene.welcomeState).path()),"Completion ignored updated state");
                h.assertFalse(OtherMotherActivityComplete.begin(p,q->freeCalls.incrementAndGet()),"Duplicate completion started");
                h.assertTrue(OtherMotherDialogue.playLoot(p,OtherMotherDialogue.Loot.FOOD,OtherMotherDialogue.Moment.BEFORE)==0,"Loot interrupted completion reaction");
                h.assertTrue(freeCalls.get()==0,"Free time began synchronously");
            }
        };
        var started=new java.util.concurrent.atomic.AtomicBoolean();
        h.succeedWhen(()->{
            if(!started.get()){
                h.assertTrue(level.getEntity(scene.mom)==mother,"Waiting for test mother's chunk to become entity-accessible");
                h.assertTrue(OtherMotherActivities.begin(player,chosen,listener),"Proposal could not start");
                started.set(true);
            }
            OtherMotherActivities.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                    net.minecraftforge.event.TickEvent.Phase.END,player));
            h.assertTrue(calls.get()==1,"Waiting for one complete greeting and continuation");
            h.assertFalse(OtherMotherActivities.active(player),"Completed activity remained pending");
            h.assertFalse(OtherMotherActivities.arrive(player,chosen),"Completed arrival replayed");
            OtherMotherActivityComplete.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                    net.minecraftforge.event.TickEvent.Phase.END,player));
            h.assertTrue(freeCalls.get()==1,"Waiting for complete reaction before free time");
            h.assertFalse(OtherMotherActivityComplete.pending(player),"Completed reaction remained pending");
            OtherMotherActivityComplete.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                    net.minecraftforge.event.TickEvent.Phase.END,player));
            h.assertTrue(freeCalls.get()==1,"Free-time continuation repeated");
            OtherMotherActivities.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                    net.minecraftforge.event.TickEvent.Phase.END,player));
            h.assertTrue(calls.get()==1,"Continuation fired twice");
            mother.discard();VisitData.get(server).scenes.remove(scene.owner);level.setChunkForced(0,0,false);
        });
    }
    @GameTest(template="empty") public static void lootAndExtraPoolsAreComplete(GameTestHelper h) {
        var random=RandomSource.create(8675309);int count=0;
        for(var pool:OtherMotherDialogue.pools()){
            String previous="";var seen=new HashSet<String>();
            for(int i=0;i<80;i++){
                String next=OtherMotherDialogue.choose(pool,previous,random);
                if(pool.size()>1)h.assertFalse(next.equals(previous),"Repeated dialogue in "+pool.path());
                seen.add(next);previous=next;
            }
            h.assertTrue(seen.size()==pool.size(),"Incorrect pool size");
            for(int i=0;i<pool.size();i++){
                String name=pool.sound(i);
                h.assertTrue(VisitAudio.SOUNDS.containsKey(name)&&VisitAudio.SOUNDS.get(name).isPresent(),"Unregistered "+name);
                h.assertTrue(VisitAudio.length(name)>20,"Missing duration "+name);
                h.assertTrue(WelcomeAudioGameTests.class.getResource("/assets/coraline/sounds/visit/other_mother/"+pool.path()+"/"+pool.fileName(i))!=null,"Missing recording "+name);
                count++;
            }
        }
        h.assertTrue(count==248,"Expected 238 existing and 10 new scolding recordings");h.succeed();
    }
    @GameTest(template="empty") public static void completionPoolsUseOnlyExistingCurrentState(GameTestHelper h) {
        var paths=new HashSet<String>();var random=RandomSource.create(72);
        for(var state:OtherMotherWelcome.State.values()){
            var scene=new VisitData.Scene();scene.welcomeState=state;
            var pool=OtherMotherActivityComplete.pool(scene.welcomeState);
            h.assertTrue(pool.path().equals("activity_complete/"+state.id()),"Wrong completion state");
            h.assertTrue(pool.filePrefix().equals("complete")&&pool.size()==3,"Wrong completion recording names/count");
            h.assertTrue(paths.add(pool.path()),"Completion states share history");
            String previous="";var seen=new HashSet<String>();
            for(int i=0;i<60;i++){
                String next=OtherMotherDialogue.choose(pool,previous,random);
                h.assertFalse(next.equals(previous),"Immediate completion repeat");seen.add(next);previous=next;
            }
            h.assertTrue(seen.size()==3,"Completion random selector missed a variation");
            h.assertTrue(scene.welcomeState==state,"Completion altered relationship state");
        }
        h.assertTrue(paths.size()==5,"Missing completion state");h.succeed();
    }
    @GameTest(template="empty") public static void activityCatalogAndStateRouting(GameTestHelper h) {
        var ids=new HashSet<String>();
        for(var activity:OtherMotherActivities.Activity.values()){
            h.assertTrue(ids.add(activity.id()),"Duplicate activity ID");
            h.assertTrue(activity.pool().size()==2&&activity.pool().filePrefix().equals("proposal"),"Invalid proposal pool");
        }
        h.assertTrue(ids.size()==22,"Expected 22 distinct activities");
        var random=RandomSource.create(87);
        for(int i=0;i<2000;i++)ids.remove(OtherMotherActivities.chooseActivity(random).id());
        h.assertTrue(ids.isEmpty(),"Random selector missed an activity");
        var scene=new VisitData.Scene();
        scene.welcomeState=OtherMotherWelcome.State.HIGH_SUSPICION_LOW_TRUST;
        var pool=OtherMotherActivities.waitPool(scene.welcomeState);
        h.assertTrue(pool.path().equals("activity_wait/high_suspicion_low_trust")&&pool.size()==3,"Wrong current-state warning pool");
        h.assertTrue(scene.welcomeState==OtherMotherWelcome.State.HIGH_SUSPICION_LOW_TRUST,"Routing modified state");
        h.succeed();
    }
    @GameTest(template="empty") public static void warningPrecedesFullFiveMinuteWindow(GameTestHelper h) {
        var flow=new OtherMotherActivities.Sequence(100,160);
        h.assertFalse(flow.ready(259),"Proposal cut short");
        h.assertTrue(flow.deadline()==0,"Timer began during proposal");
        flow.warning(260,220);
        h.assertTrue(flow.phase()==OtherMotherActivities.Phase.WARNING,"Warning skipped");
        h.assertFalse(flow.ready(479),"Warning cut short");
        h.assertTrue(flow.deadline()==0,"Timer began during warning");
        h.assertTrue(flow.arrival(479)==null,"Arrival accepted before warning completed");
        flow.open(480);
        h.assertTrue(flow.deadline()==6480,"Did not receive all 6000 ticks after warning");
        h.assertFalse(flow.expire(6479),"Window expired early");
        h.assertTrue(flow.expire(6480),"Window did not expire");
        h.assertFalse(flow.expire(6481),"Window expired twice");
        h.assertTrue(flow.arrival(6481)==OtherMotherActivities.Arrival.LATE,"Expired deadline lost late classification");
        h.assertTrue(flow.deadline()==6480,"Deadline lost after expiry");h.succeed();
    }
    @GameTest(template="empty") public static void timelyActivityArrivalIsOneShot(GameTestHelper h) {
        var flow=new OtherMotherActivities.Sequence(0,20);flow.warning(20,20);flow.open(40);
        h.assertTrue(flow.arrival(6039)==OtherMotherActivities.Arrival.ON_TIME,"Timely arrival rejected");
        h.assertTrue(flow.recordArrival(6039),"Timely arrival not recorded");
        h.assertFalse(flow.recordArrival(6040),"Duplicate arrival recorded");
        h.assertTrue(flow.greet(6039,200),"Greeting did not begin");
        h.assertFalse(flow.greet(6039,200),"Duplicate arrival accepted");
        h.assertFalse(flow.expire(6040),"Attended activity marked missed");
        h.assertFalse(flow.finishGreeting(6238),"Activity began before greeting ended");
        h.assertTrue(flow.finishGreeting(6239),"Activity did not continue after greeting");
        h.assertFalse(flow.finishGreeting(6240),"Activity continued twice");
        var late=new OtherMotherActivities.Sequence(0,20);late.warning(20,20);late.open(40);
        h.assertTrue(late.arrival(6040)==OtherMotherActivities.Arrival.LATE,"Deadline boundary not classified late");
        h.assertTrue(late.expire(6040),"Expiry notification skipped");
        h.assertTrue(late.recordArrival(6100),"Late attendance not recorded");
        h.assertTrue(late.greet(6100,120),"Late greeting rejected");
        h.assertFalse(late.finishGreeting(6219),"Late greeting cut short");
        h.assertTrue(late.finishGreeting(6220),"Late arrival did not continue");h.succeed();
    }
    @GameTest(template="empty") public static void busyVoiceCannotTurnTimelyAttendanceLate(GameTestHelper h) {
        var flow=new OtherMotherActivities.Sequence(0,20);flow.warning(20,20);flow.open(40);
        h.assertTrue(flow.recordArrival(6039),"Arrival not queued");
        h.assertFalse(flow.greet(6039,0),"Empty greeting accepted");
        h.assertFalse(flow.expire(6050),"Queued timely arrival expired while voice busy");
        h.assertTrue(flow.recordedArrival()==OtherMotherActivities.Arrival.ON_TIME,"Attendance changed while waiting");
        h.assertTrue(flow.greet(6100,200),"Queued greeting did not start");
        h.assertTrue(flow.deadline()==6040,"Greeting restarted attendance timer");
        h.assertFalse(flow.finishGreeting(6299),"Queued greeting truncated");
        h.assertTrue(flow.finishGreeting(6300),"Queued activity failed to continue");h.succeed();
    }
    @GameTest(template="empty") public static void arrivalPoolsFollowCurrentStateAndAttendance(GameTestHelper h) {
        var paths=new HashSet<String>();var scene=new VisitData.Scene();
        for(var state:OtherMotherWelcome.State.values())for(var arrival:OtherMotherActivities.Arrival.values()){
            scene.welcomeState=state;
            var pool=OtherMotherActivities.arrivalPool(scene.welcomeState,arrival);
            h.assertTrue(pool.path().equals("activity_arrival/"+state.id()+"/"+arrival.name().toLowerCase(Locale.ROOT)),"Wrong arrival routing");
            h.assertTrue(pool.size()==3&&pool.filePrefix().equals("arrival"),"Wrong arrival recording names/count");
            h.assertTrue(paths.add(pool.path()),"Shared state/condition history");
            h.assertTrue(scene.welcomeState==state,"Greeting changed relationship state");
        }
        h.assertTrue(paths.size()==10,"Missing state/condition combination");h.succeed();
    }
    @GameTest(template="empty") public static void tutorialIsOneSharedFourChoicePool(GameTestHelper h) {
        var pool=OtherMotherTutorial.POOL;
        h.assertTrue(pool.size()==4&&pool.path().equals("tutorial"),"Tutorial is not one generic pool");
        for(var state:OtherMotherWelcome.State.values()){
            var scene=new VisitData.Scene();scene.welcomeState=state;
            String next=OtherMotherDialogue.choose(pool,pool.sound(0),RandomSource.create(42));
            h.assertTrue(next.equals(OtherMotherDialogue.choose(pool,pool.sound(0),RandomSource.create(42))),"State changed tutorial choice");
            h.assertTrue(scene.welcomeState==state,"Tutorial changed relationship state");
        }h.succeed();
    }
    @GameTest(template="empty") public static void activityFollowsTutorialExactlyOnce(GameTestHelper h) {
        var completion=new OtherMotherTutorial.Completion(400);
        var calls=new java.util.concurrent.atomic.AtomicInteger();
        h.assertFalse(completion.advance(399,true,calls::incrementAndGet),"Proposal interrupted tutorial");
        h.assertTrue(calls.get()==0,"Proposal ran early");
        h.assertTrue(completion.advance(400,true,calls::incrementAndGet),"Proposal did not follow immediately");
        h.assertFalse(completion.advance(401,true,calls::incrementAndGet),"Proposal repeated");
        h.assertTrue(calls.get()==1,"Proposal did not run exactly once");
        var cancelled=new OtherMotherTutorial.Completion(500);
        cancelled.advance(450,false,calls::incrementAndGet);
        cancelled.advance(500,true,calls::incrementAndGet);
        h.assertTrue(calls.get()==1,"Cancelled visit launched activity");h.succeed();
    }
    @GameTest(template="empty") public static void dialogueHistoryPersistsPerPool(GameTestHelper h) {
        var data=new VisitData();var s=new VisitData.Scene();s.owner=UUID.randomUUID();s.door=UUID.randomUUID();
        s.piano=BlockPos.ZERO;s.table=BlockPos.ZERO;
        for(var pool:OtherMotherDialogue.pools())s.lastDialogue.put(pool.path(),pool.sound(pool.size()-1));
        data.scenes.put(s.owner,s);
        var loaded=VisitData.load(data.save(new CompoundTag())).scenes.get(s.owner);
        h.assertTrue(loaded.lastDialogue.equals(s.lastDialogue),"Dialogue histories lost on reload");
        h.assertTrue(new VisitData.Scene().lastDialogue.isEmpty(),"New family inherited history");h.succeed();
    }
    @GameTest(template="empty") public static void allPoolsAvoidImmediateRepeat(GameTestHelper h) {
        var random=RandomSource.create(112358);
        for(var state:OtherMotherWelcome.State.values()){
            String previous="";var seen=new HashSet<String>();
            for(int i=0;i<120;i++){
                String next=OtherMotherWelcome.choose(state,previous,random);
                h.assertTrue(!next.equals(previous),"Repeated same welcome");
                h.assertTrue(next.startsWith("other_mother_welcome_"+state.id()+"_"),"Selected wrong state");
                seen.add(next);previous=next;
            }
            h.assertTrue(seen.size()==3,"Pool did not contain exactly three recordings");
        }h.succeed();
    }
    @GameTest(template="empty") public static void welcomeStatePersistsWithFamilyScene(GameTestHelper h) {
        var data=new VisitData();var s=new VisitData.Scene();s.owner=UUID.randomUUID();s.door=UUID.randomUUID();s.family=UUID.randomUUID();
        s.piano=BlockPos.ZERO;s.table=BlockPos.ZERO;s.welcomeState=OtherMotherWelcome.State.HIGH_TRUST_HIGH_SUSPICION;
        s.lastWelcome=s.welcomeState.sound(2);s.welcomeAfter=2345;data.scenes.put(s.owner,s);
        var loaded=VisitData.load(data.save(new CompoundTag())).scenes.get(s.owner);
        h.assertTrue(loaded.welcomeState==s.welcomeState&&loaded.lastWelcome.equals(s.lastWelcome)&&loaded.welcomeAfter==2345,"Welcome state lost on reload");
        h.assertTrue(loaded.family.equals(s.family),"Family association lost");h.succeed();
    }
    @GameTest(template="empty") public static void oldSavesUseFriendlyDefault(GameTestHelper h) {
        h.assertTrue(OtherMotherWelcome.State.fromId("")==OtherMotherWelcome.State.LOW_TRUST_LOW_SUSPICION,"Missing state lacks default");
        h.assertTrue(OtherMotherWelcome.State.fromId("unknown")==OtherMotherWelcome.State.LOW_TRUST_LOW_SUSPICION,"Unknown state crashes migration");h.succeed();
    }
    @GameTest(template="empty") public static void replacementParentsDiscardWelcomeHistory(GameTestHelper h) {
        var server=h.getLevel().getServer();var data=VisitData.get(server);
        var scene=new VisitData.Scene();scene.owner=UUID.randomUUID();scene.family=UUID.randomUUID();scene.door=UUID.randomUUID();
        scene.piano=BlockPos.ZERO;scene.table=BlockPos.ZERO;scene.stage=VisitData.Stage.DONE;
        scene.welcomeState=OtherMotherWelcome.State.HIGH_SUSPICION_LOW_TRUST;scene.lastWelcome=scene.welcomeState.sound(1);
        data.scenes.put(scene.owner,scene);
        FamilyVisitReset.validate(server,scene.owner,UUID.randomUUID());
        h.assertFalse(data.scenes.containsKey(scene.owner),"Replacement family inherited welcome state/history");
        var fresh=new VisitData.Scene();
        h.assertTrue(fresh.lastWelcome.isEmpty()&&fresh.welcomeState==OtherMotherWelcome.State.LOW_TRUST_LOW_SUSPICION,"New family is not friendly/default");h.succeed();
    }
    @GameTest(template="empty") public static void firstVisitAndOtherRealmsNeverTriggerWelcome(GameTestHelper h) {
        var scene=new VisitData.Scene();scene.door=UUID.randomUUID();
        for(var stage:VisitData.Stage.values())if(stage!=VisitData.Stage.DONE){
            scene.stage=stage;h.assertFalse(OtherMotherWelcome.eligible(scene,scene.door,1000),"Welcome interrupts first visit at "+stage);
        }
        scene.stage=VisitData.Stage.DONE;scene.welcomeAfter=500;
        h.assertFalse(OtherMotherWelcome.eligible(scene,UUID.randomUUID(),1000),"Wrong family's realm greeted player");
        h.assertFalse(OtherMotherWelcome.eligible(scene,scene.door,499),"Welcome ignores playback cooldown");
        h.assertTrue(OtherMotherWelcome.eligible(scene,scene.door,500),"Completed visit cannot welcome player");h.succeed();
    }
    @GameTest(template="empty") public static void allWelcomeResourcesAreRegistered(GameTestHelper h) {
        for(var state:OtherMotherWelcome.State.values())for(int i=0;i<3;i++){
            String name=state.sound(i);
            h.assertTrue(VisitAudio.SOUNDS.containsKey(name)&&VisitAudio.SOUNDS.get(name).isPresent(),"Missing sound registration: "+name);
            h.assertTrue(VisitAudio.length(name)>20,"Missing duration: "+name);
            String path="/assets/coraline/sounds/visit/welcome/"+state.id()+"/welcome_0"+(i+1)+".ogg";
            h.assertTrue(WelcomeAudioGameTests.class.getResource(path)!=null,"Missing audio: "+path);
        }h.succeed();
    }
}
