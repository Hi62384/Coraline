package com.coraline.feast;

import java.util.UUID;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class GhostVictimProgressGameTests {
    @GameTest(template="empty") public static void activeManifestationsSuspendRegeneration(GameTestHelper h){
        var n=session();n.putDouble("Energy",.5);n.putLong("ActiveTicks",100);n.putLong("ManifestingUntil",150);
        for(int tick=0;tick<49;tick++)GhostVictimProgress.tick(n,GhostVictimProgress.Tuning.defaults());
        h.assertTrue(GhostVictimProgress.energy(n)==.5,"Energy regenerated during an active manifestation");
        GhostVictimProgress.tick(n,GhostVictimProgress.Tuning.defaults());h.assertTrue(GhostVictimProgress.energy(n)>.5,"Energy did not recover after the effect ended");h.succeed();
    }
    @GameTest(template="empty") public static void acceptedEscapeDialogueButtonsDecodeTheirWireAction(GameTestHelper h){
        var n=new CompoundTag();var actions=new ListTag();
        for(String id:java.util.List.of("begin","report_stone","room:storage","ambush","hide:cover_1")){
            var row=new CompoundTag();row.putString("Action","escape:"+id);actions.add(row);
        }
        n.put("EscapeOptions",actions);
        String[] expected={"begin","report_stone","room:storage","ambush","hide:cover_1"};
        for(int i=0;i<expected.length;i++)h.assertTrue(expected[i].equals(GhostVictimMirrors.indexedEscapeAction(n,7+i)),
                "Visible indexed escape option did not reach the matching choice: "+expected[i]);
        h.assertTrue(GhostVictimMirrors.indexedEscapeAction(n,6)==null&&GhostVictimMirrors.indexedEscapeAction(n,12)==null,
                "Out-of-range or ordinary dialogue was treated as an escape command");
        actions.getCompound(0).putString("Action","incorrect:begin");
        h.assertTrue(GhostVictimMirrors.indexedEscapeAction(n,7)==null,
                "Unknown extra button action was accepted");
        h.succeed();
    }
    @GameTest(template="empty") public static void acceptedEscapeChoiceVisibilityMatchesProgressPrerequisites(GameTestHelper h){
        var n=session();GhostVictimProgress.trust(n,.45,.65);
        GhostVictimProgress.move(n,GhostVictimProgress.Phase.BEDTIME);
        h.assertFalse(GhostVictimMirrors.escapePlanOfferReady(n),"Alliance offered before the private bedtime conversation window");
        n.putLong("BedtimeWindowStarted",GhostVictimProgress.clock(n));
        h.assertTrue(GhostVictimMirrors.escapePlanOfferReady(n),"The ready child cannot offer the escape plan during a private bedtime window");
        var escape=GhostEscapeProgress.data(n);escape.putUUID("Attempt",UUID.randomUUID());
        h.assertFalse(GhostVictimMirrors.escapePlanOfferReady(n),"Escape plan was offered again after the attempt began");
        GhostEscapeProgress.move(escape,GhostEscapeProgress.Phase.ROOM_REPORT,0);
        h.assertFalse(GhostVictimMirrors.ambushOfferReady(escape),"Key ambush offered without the actual eye room discovery");
        escape.putBoolean("EyeRoomKnown",true);
        h.assertTrue(GhostVictimMirrors.ambushOfferReady(escape),"Discovered eye room did not offer the key ambush");
        escape.putBoolean("KeyStolen",true);
        h.assertFalse(GhostVictimMirrors.ambushOfferReady(escape),"Already stolen key advertised another ambush");
        h.succeed();
    }
    @GameTest(template="empty") public static void bedtimeGetsANewPrivateWindowEveryNight(GameTestHelper h){
        var n=session();n.putLong("BedtimeWindowStarted",1);n.putLong("BedtimeArrived",1);var route=new CompoundTag();route.putLong("Started",1);n.put("MotherBedtimeExit",route);
        GhostVictimProgress.move(n,GhostVictimProgress.Phase.BEDTIME);
        h.assertFalse(n.contains("BedtimeWindowStarted")||n.contains("BedtimeArrived")||n.contains("MotherBedtimeExit"),"Previous night skips the mother's exit or the conversation window");h.succeed();
    }
    @GameTest(template="empty") public static void mirrorsAreLimitedToTodaysRealDimension(GameTestHelper h){
        var n=session();var at=new net.minecraft.core.BlockPos(4,6,8);
        GhostVictimMirrors.register(n,"bedroom",at,0,"coraline:other_world");
        h.assertTrue(GhostVictimMirrors.available(n,"bedroom","coraline:other_world"),"Scheduled bedroom mirror unavailable");
        h.assertFalse(GhostVictimMirrors.available(n,"bedroom","minecraft:overworld"),"Mirror works at matching coordinates in another dimension");
        GhostVictimMirrors.register(n,"unplanned",at,0,"coraline:other_world");
        h.assertFalse(GhostVictimMirrors.available(n,"unplanned","coraline:other_world"),"Unscheduled mirror became a conversation shortcut");h.succeed();
    }
    @GameTest(template="empty") public static void privateMirrorThreatWaitsForARealReport(GameTestHelper h){
        var n=session();GhostVictimProgress.trust(n,.5,.5);
        h.assertTrue(GhostVictimProgress.enterMirror(n,"bedroom",GhostVictimProgress.Tuning.defaults()),"Mirror unavailable");
        h.assertTrue(GhostVictimProgress.dialogue(n,n.getUUID("Conversation"),GhostVictimProgress.Dialogue.THREAT,true),"First threat rejected");
        h.assertTrue(GhostVictimProgress.attention(n)==0,"Unwitnessed mirror conversation gave Mother omniscient attention");
        h.assertTrue(GhostVictimProgress.child(n).getBoolean("PendingGhostReport")&&GhostVictimProgress.remembers(n,"ghost_threat"),"Child forgot the threat before it could report it physically");
        h.assertTrue(GhostVictimProgress.ghostTrust(n)<.5&&GhostVictimProgress.motherTrust(n)>.5,"Threat was beneficial");h.succeed();
    }
    @GameTest(template="empty") public static void archivedActorCannotBecomeTheNextVictim(GameTestHelper h){
        var n=session();UUID actor=UUID.randomUUID();n.putUUID("VictimActor",actor);n.putLong("ActorPosition",42);n.putString("ActorDimension","coraline:other_world");
        n.putBoolean("VoiceStarted",true);n.putBoolean("ChildAtHome",true);n.putLong("SupperSeated",15);n.putLong("CaptureSeatedAt",20);
        h.assertTrue(GhostVictimProgress.archiveVictim(n,"CAPTURED"),"Capture did not archive");
        var history=n.getList("VictimHistory",Tag.TAG_COMPOUND);
        h.assertTrue(history.getCompound(0).getUUID("RetiredActor").equals(actor),"Unloaded retired actor lost its tombstone");
        h.assertFalse(n.hasUUID("VictimActor")||n.contains("ActorPosition")||n.contains("ActorDimension"),"Next victim inherited the previous actor lease");
        h.assertFalse(n.contains("VoiceStarted")||n.contains("ChildAtHome")||n.contains("SupperSeated")||n.contains("CaptureSeatedAt"),"Next victim inherited completed scene gates and skipped its own arrival/supper");
        h.succeed();
    }
    @GameTest(template="empty") public static void earnedFriendshipCanReachFullTrustBeforeChildLeaves(GameTestHelper h){
        var n=session();var tuning=GhostVictimProgress.Tuning.defaults();
        // Distinct, actually witnessed discoveries unlock a new conversation
        // context. Repeating a light switch or reopening a screen cannot do so.
        for(int discovery=0;discovery<3;discovery++){
            n.putDouble("Energy",1);
            var result=GhostVictimProgress.disturb(n,"distinct_secret_"+discovery,GhostVictimProgress.Disturbance.ILLUSION,
                new GhostVictimProgress.Observation(true,false,false,true,false),true);
            h.assertTrue(result.performed(),"Distinct witnessed discovery was unavailable");
            n.putDouble("Energy",1);h.assertTrue(GhostVictimProgress.enterMirror(n,"bedroom",tuning),"Conversation mirror unavailable");
            var nonce=n.getUUID("Conversation");
            for(var choice:new GhostVictimProgress.Dialogue[]{GhostVictimProgress.Dialogue.FRIENDLY,GhostVictimProgress.Dialogue.HELP,
                GhostVictimProgress.Dialogue.TRAPPED,GhostVictimProgress.Dialogue.EVIDENCE,GhostVictimProgress.Dialogue.ACCUSATION}){
                // Advance the authoritative clock, not the energy drain of a
                // physically absent runtime. The mirror itself has its own test.
                n.putLong("ActiveTicks",GhostVictimProgress.clock(n)+60);
                h.assertTrue(GhostVictimProgress.dialogue(n,nonce,choice,true),"Fresh evidence conversation rejected");
                double after=GhostVictimProgress.ghostTrust(n);
                n.putLong("ActiveTicks",GhostVictimProgress.clock(n)+60);
                h.assertFalse(GhostVictimProgress.dialogue(n,nonce,choice,true),"Same statement farmed friendship");
                h.assertTrue(after==GhostVictimProgress.ghostTrust(n),"Rejected repeat changed trust");
            }
            h.assertTrue(GhostVictimProgress.readyToHelp(n)==(discovery>=1),
                "Alliance must follow two witnessed discoveries and a conversation about the trap");
            if(discovery==1)h.assertTrue(GhostVictimProgress.ghostTrust(n)<1,"Alliance still needs every trust point");
            GhostVictimProgress.leaveMirror(n);
        }
        h.assertTrue(GhostVictimProgress.ghostTrust(n)==1&&GhostVictimProgress.motherTrust(n)>0&&GhostVictimProgress.motherTrust(n)<.6,"Balanced evidence route cannot form an alliance before an automatic solo escape");
        h.assertFalse(n.getBoolean("EscapeStarted"),"Next Victim silently started the later escape storyline");h.succeed();
    }
    private static CompoundTag session(){
        var n=new CompoundTag();GhostVictimProgress.begin(n,"ACCEPTED_PROPOSAL");
        GhostVictimProgress.leavePrison(n);var c=new CompoundTag();c.putString("Name","Robin");
        GhostVictimProgress.newVictim(n,UUID.randomUUID(),c);return n;
    }
    @GameTest(template="empty") public static void prisonExitIsManualAndIdentityDoesNotReset(GameTestHelper h){
        var n=new CompoundTag();GhostVictimProgress.begin(n,"FINAL_BOSS_LOSS");
        for(int i=0;i<1200;i++)GhostVictimProgress.tick(n,GhostVictimProgress.Tuning.defaults());
        h.assertTrue(GhostVictimProgress.phase(n)==GhostVictimProgress.Phase.ENTER_MIRROR&&!n.getBoolean("OutsideMirror"),"Timer released the ghost without mirror interaction");
        h.assertTrue(GhostVictimProgress.leavePrison(n),"Manual mirror entry failed");
        var identity=new CompoundTag();identity.putString("Name","Robin");UUID child=UUID.randomUUID();
        h.assertTrue(GhostVictimProgress.newVictim(n,child,identity),"First identity not accepted");
        h.assertFalse(GhostVictimProgress.newVictim(n,UUID.randomUUID(),identity),"Reload duplicated or replaced the active child");
        h.assertTrue(GhostVictimProgress.child(n).getUUID("Id").equals(child)&&GhostVictimProgress.motherTrust(n)==.5&&GhostVictimProgress.ghostTrust(n)==0,"Wrong initial identity or trust");
        h.succeed();
    }
    @GameTest(template="empty") public static void independentTrustCapDoesNotTriggerRescue(GameTestHelper h){
        var n=session();GhostVictimProgress.trust(n,.75,.7);
        h.assertTrue(GhostVictimProgress.motherTrust(n)==.75&&GhostVictimProgress.ghostTrust(n)==.7,"Trust became opposing values");
        GhostVictimProgress.trust(n,.6,1);h.assertTrue(GhostVictimProgress.ghostTrust(n)==.99,"High Mother trust did not cap ghost trust at .99");
        GhostVictimProgress.trust(n,.59,1);var before=n.copy();
        h.assertTrue(GhostVictimProgress.ghostTrust(n)==1,"Low Mother trust prevented full friendship");
        GhostVictimProgress.tick(n,GhostVictimProgress.Tuning.defaults());
        h.assertTrue(GhostVictimProgress.phase(n)==GhostVictimProgress.phase(before)&&!n.getBoolean("SoulRestored")&&!n.getBoolean("EscapeStarted"),"Full trust started an unimplemented rescue");h.succeed();
    }
    @GameTest(template="empty") public static void mirrorDrainsAndClosesAtZeroWithStaleChoicesRejected(GameTestHelper h){
        var n=session();var tuning=GhostVictimProgress.Tuning.defaults();
        h.assertTrue(GhostVictimProgress.enterMirror(n,"bedroom",tuning),"Mirror entry failed");UUID conversation=n.getUUID("Conversation");
        h.assertFalse(GhostVictimProgress.dialogue(n,UUID.randomUUID(),GhostVictimProgress.Dialogue.HELP,true),"Forged conversation accepted");
        h.assertFalse(GhostVictimProgress.dialogue(n,conversation,GhostVictimProgress.Dialogue.HELP,false),"Absent child could hear across the world");
        for(int i=0;i<250;i++)GhostVictimProgress.tick(n,tuning);
        h.assertFalse(GhostVictimProgress.mirror(n),"Empty energy left a permanent mirror conversation");
        h.assertFalse(n.hasUUID("Conversation"),"Stale conversation nonce survived forced exit");h.succeed();
    }
    @GameTest(template="empty") public static void punishmentExpiresOnceAndDoesNotFreezeScheduleClock(GameTestHelper h){
        var n=session();GhostVictimProgress.trust(n,.5,.6);n.putDouble("Attention",1);
        GhostVictimProgress.enterMirror(n,"hall",GhostVictimProgress.Tuning.defaults());
        h.assertTrue(GhostVictimProgress.tick(n,GhostVictimProgress.Tuning.defaults()),"Maximum attention failed to punish");
        h.assertTrue(GhostVictimProgress.imprisoned(n)&&GhostVictimProgress.energy(n)==0&&!GhostVictimProgress.mirror(n),"Punishment did not suppress mirror/energy");
        var saved=n.copy();long clock=GhostVictimProgress.clock(saved);
        for(int i=0;i<1200;i++)GhostVictimProgress.tick(saved,GhostVictimProgress.Tuning.defaults());
        h.assertTrue(GhostVictimProgress.clock(saved)==clock+1200&&!GhostVictimProgress.imprisoned(saved),"Punishment paused the owner clock or never released");
        h.assertTrue(saved.getInt("Punishments")==1&&GhostVictimProgress.attention(saved)<.2,"Punishment retriggered every tick at expiry");h.succeed();
    }
    @GameTest(template="empty") public static void invisibleDisturbanceAndRepeatedDiscoveryDoNotFarmTrust(GameTestHelper h){
        var n=session();var unseen=new GhostVictimProgress.Observation(false,false,false,false,false);
        var saw=new GhostVictimProgress.Observation(true,false,false,false,false);
        GhostVictimProgress.disturb(n,"dish",GhostVictimProgress.Disturbance.FOOD,unseen,true);
        h.assertTrue(GhostVictimProgress.motherTrust(n)==.5&&GhostVictimProgress.attention(n)==0,"Unseen action changed belief or alerted omniscient Mother");
        n.putLong("ActiveTicks",601);n.putDouble("Energy",1);
        var result=GhostVictimProgress.disturb(n,"dish",GhostVictimProgress.Disturbance.FOOD,saw,true);
        h.assertTrue(result.performed()&&GhostVictimProgress.motherTrust(n)<.5,"Visible food illusion supplied no evidence");
        double trust=GhostVictimProgress.motherTrust(n);n.putInt("Day",2);n.putDouble("Energy",1);
        GhostVictimProgress.disturb(n,"dish",GhostVictimProgress.Disturbance.FOOD,saw,true);
        h.assertTrue(GhostVictimProgress.motherTrust(n)==trust,"Same discovery rewarded again on another day");h.succeed();
    }
    @GameTest(template="empty") public static void frighteningActionsCanBackfireAndLightIsNotEvidence(GameTestHelper h){
        var n=session();GhostVictimProgress.trust(n,.5,.5);
        var blamed=new GhostVictimProgress.Observation(true,true,true,false,true);
        GhostVictimProgress.disturb(n,"knife",GhostVictimProgress.Disturbance.KNIFE,blamed,true);
        h.assertTrue(GhostVictimProgress.ghostTrust(n)<.5&&GhostVictimProgress.motherTrust(n)>.5&&GhostVictimProgress.attention(n)>0,"Threat was an automatic beneficial manifestation");
        h.assertTrue(GhostVictimProgress.child(n).getInt("EvidenceCount")==0,"A frightening knife proved Mother's deception");
        n.putDouble("Energy",1);GhostVictimProgress.disturb(n,"lamp",GhostVictimProgress.Disturbance.LIGHT,new GhostVictimProgress.Observation(true,false,false,false,false),true);
        h.assertTrue(GhostVictimProgress.child(n).getInt("EvidenceCount")==0,"Every flickering light became strong evidence");h.succeed();
    }
    @GameTest(template="empty") public static void spentEnergyAndVictimArchiveAreIdempotent(GameTestHelper h){
        var n=session();n.putDouble("Energy",.01);var before=n.copy();
        h.assertFalse(GhostVictimProgress.spend(n,.2),"Unaffordable manifestation ran");
        h.assertTrue(n.equals(before),"Unaffordable action altered the journal");UUID id=GhostVictimProgress.child(n).getUUID("Id");
        h.assertTrue(GhostVictimProgress.archiveVictim(n,"CAPTURED"),"Victim capture failed to archive");
        h.assertFalse(GhostVictimProgress.archiveVictim(n,"CAPTURED"),"Repeated completion duplicated the victim");
        h.assertTrue(n.getList("VictimHistory",Tag.TAG_COMPOUND).getCompound(0).getUUID("Id").equals(id)&&n.getInt("CapturedCount")==1,"History lost original identity");h.succeed();
    }
    @GameTest(template="empty") public static void dayPlanPersistsAndUsesDistinctExistingActivities(GameTestHelper h){
        var n=session();var plan=GhostVictimSchedule.prepare(n,2).copy();var list=plan.getList("Activities",Tag.TAG_STRING);
        h.assertTrue(list.size()>=2&&list.size()<=3,"Wrong number of daily activities");var ids=new java.util.HashSet<String>();
        for(Tag id:list){h.assertTrue(ids.add(id.getAsString()),"Same activity repeated within one day");
            h.assertTrue(OtherMotherActivities.Activity.valueOf(id.getAsString().toUpperCase(java.util.Locale.ROOT))!=null,"Invented activity ID");}
        h.assertTrue(GhostVictimSchedule.prepare(n.copy(),2).equals(plan),"Reload rerolled the day's events, gift or mirrors");
        for(Tag point:plan.getList("Opportunities",Tag.TAG_STRING))h.assertTrue(ids.contains(point.getAsString().split(":")[0]),"Secret selected for an unavailable event");h.succeed();
    }
    @GameTest(template="empty") public static void nextMorningPreservesVictimTrustAndCannotCommitTwice(GameTestHelper h){
        var n=session();GhostVictimProgress.trust(n,.41,.72);UUID id=GhostVictimProgress.child(n).getUUID("Id");
        GhostVictimProgress.move(n,GhostVictimProgress.Phase.NIGHT_PREPARATION);var expected=GhostVictimSchedule.tomorrow(n).copy();
        h.assertTrue(GhostVictimSchedule.morning(n)&&!GhostVictimSchedule.morning(n),"Morning did not commit exactly once");
        h.assertTrue(n.getInt("Day")==2&&GhostVictimProgress.child(n).getUUID("Id").equals(id)&&GhostVictimProgress.motherTrust(n)==.41&&GhostVictimProgress.ghostTrust(n)==.72,"Morning reset child or relationships");
        h.assertTrue(GhostVictimSchedule.today(n).equals(expected),"Morning discarded the scouted opportunities");h.succeed();
    }
    @GameTest(template="empty") public static void activityCompletionRequiresCurrentParticipantAndCurrentEvent(GameTestHelper h){
        var n=session();GhostVictimProgress.move(n,GhostVictimProgress.Phase.ACTIVITY);String id=GhostVictimSchedule.currentActivity(n);
        h.assertFalse(GhostVictimSchedule.completeActivity(n,UUID.randomUUID(),id),"Foreign child completed owner's event");
        h.assertFalse(GhostVictimSchedule.completeActivity(n,GhostVictimProgress.child(n).getUUID("Id"),"not_the_event"),"Wrong event advanced the schedule");
        h.assertTrue(GhostVictimSchedule.completeActivity(n,GhostVictimProgress.child(n).getUUID("Id"),id),"Current event refused completion");
        h.assertFalse(GhostVictimSchedule.completeActivity(n,GhostVictimProgress.child(n).getUUID("Id"),id),"Repeated callback skipped an activity");h.succeed();
    }
    @GameTest(template="empty") public static void newVictimCannotInheritAnOldVictimsDailyPlans(GameTestHelper h){
        var n=session();GhostVictimSchedule.prepare(n,1);UUID old=GhostVictimProgress.child(n).getUUID("Id");
        GhostVictimProgress.archiveVictim(n,"ESCAPED");var identity=new CompoundTag();identity.putString("Name","Mira");UUID next=UUID.randomUUID();
        GhostVictimProgress.newVictim(n,next,identity);var plan=GhostVictimSchedule.today(n);
        h.assertTrue(plan.getUUID("Victim").equals(next)&&!plan.getUUID("Victim").equals(old),"New victim inherited the previous child's itinerary");
        h.assertTrue(GhostVictimProgress.motherTrust(n)==.5&&GhostVictimProgress.ghostTrust(n)==0&&GhostVictimProgress.child(n).getList("Memories",Tag.TAG_STRING).isEmpty(),"New victim inherited trust or evidence");h.succeed();
    }
}
