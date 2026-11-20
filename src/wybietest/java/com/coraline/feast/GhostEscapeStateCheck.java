package com.coraline.feast;

import java.util.*;
import net.minecraft.nbt.*;

/** Actual serialized-NBT component checks, not a physical scene acceptance. */
public final class GhostEscapeStateCheck {
    private static int checks;
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
    private static CompoundTag session(){
        var ghost=new CompoundTag();GhostVictimProgress.begin(ghost,"ACCEPTED_PROPOSAL");ghost.putUUID("Session",UUID.randomUUID());ghost.putUUID("Owner",UUID.randomUUID());ghost.putUUID("Door",UUID.randomUUID());GhostVictimProgress.leavePrison(ghost);
        GhostVictimProgress.newVictim(ghost,UUID.randomUUID(),new CompoundTag());GhostVictimProgress.trust(ghost,.4,1);GhostVictimProgress.move(ghost,GhostVictimProgress.Phase.BEDTIME);
        require(GhostVictimProgress.enterMirror(ghost,"bedroom",GhostVictimProgress.Tuning.defaults()),"Mirror entry failed");
        require(GhostEscapeProgress.begin(ghost,List.of("storage","attic","archive"),Set.of("ghost_0","ghost_2"),true),"Valid bedtime alliance failed");return ghost;
    }
    public static void main(String[] args)throws Exception{
        var recording=new CompoundTag();
        for(int i=0;i<3;i++){
            require(!RescueChapter.RecordingRecovery.exhausted(recording),"Recording recovery ended before its retry limit");
            RescueChapter.RecordingRecovery.failed(recording);
            recording=TagParser.parseTag(recording.toString());
        }
        RescueChapter.RecordingRecovery.failed(recording);
        require(RescueChapter.RecordingRecovery.exhausted(recording)&&recording.getInt("RecordingFailures")==3,"Reload restarted the failed recording budget");
        for(int i=1;i<=100;i++){
            int tick=RescueChapter.RecordingRecovery.advance(recording);
            require(RescueChapter.RecordingRecovery.dialogueWindowEnded(tick,0)==(i==100),"Missing audio completed before the silent dialogue window");
        }
        var errand=session();
        require(!GhostEscapeProgress.homeErrandPending(errand),"Alliance was considered an active home errand before consent");
        require(GhostEscapeProgress.bedtimeAgreement(errand,true)&&GhostEscapeProgress.homeErrandPending(errand),
                "Agreed Seeing Stone errand was not protected from night watchdog progression");
        errand=TagParser.parseTag(errand.toString());
        require(GhostEscapeProgress.homeErrandPending(errand),"Reload lost unfinished original home errand");
        require(!GhostEscapeProgress.homeErrand(errand,false)&&GhostEscapeProgress.homeErrandPending(errand),
                "Nonphysical home return advanced the errand");
        errand.putBoolean("ChildAtHome",true);
        require(GhostEscapeProgress.homeErrand(errand,true)&&!GhostEscapeProgress.homeErrandPending(errand),
                "Real Seeing Stone home handoff did not release the morning guard");
        var ghost=session();var n=GhostEscapeProgress.data(ghost);String room=n.getString("EyeRoom");
        require(!GhostEscapeProgress.begin(ghost,List.of("storage"),Set.of(),true),"Repeated alliance rerolled the room");
        require(n.getString("EyeRoom").equals(room),"Eye room changed after reload/re-entry");
        require(n.getList("UnresolvedGhosts",Tag.TAG_STRING).size()==2,"Already-freed ghost was resurrected or unresolved eyes lost");
        GhostEscapeProgress.suspicion(n,1);require(GhostEscapeProgress.active(n)&&GhostEscapeProgress.phase(n)!=GhostEscapeProgress.Phase.FAILED_SEWING,"Maximum suspicion caused an omniscient loss");
        GhostEscapeProgress.witnessed(ghost,GhostEscapeProgress.Discovery.EXPOSED_SEEING_STONE,false);require(GhostEscapeProgress.active(n),"Unobserved stone caused loss");
        GhostEscapeProgress.witnessed(ghost,GhostEscapeProgress.Discovery.EXPOSED_SEEING_STONE,true);require(GhostEscapeProgress.phase(n)==GhostEscapeProgress.Phase.FAILED_SEWING,"Seen stone did not start the sewing failure");
        ghost=session();n=GhostEscapeProgress.data(ghost);GhostEscapeProgress.move(n,GhostEscapeProgress.Phase.INTERCEPT,0);
        GhostEscapeProgress.witnessed(ghost,GhostEscapeProgress.Discovery.EXPOSED_SEEING_STONE,true);require(GhostEscapeProgress.phase(n)==GhostEscapeProgress.Phase.INTERCEPT,"Mandatory confrontation triggered an obsolete covert-loss rule");
        GhostEscapeProgress.move(n,GhostEscapeProgress.Phase.WAIT_GO,0);ghost.putLong("ActiveTicks",2399);
        require(!GhostEscapeProgress.expireAssignment(ghost),"Assignment expired early");ghost.putLong("ActiveTicks",2400);require(GhostEscapeProgress.expireAssignment(ghost),"Search waits forever");
        n.putBoolean("EyeRoomKnown",true);GhostEscapeProgress.move(n,GhostEscapeProgress.Phase.APPROACH_AMBUSH,2400);
        require(GhostEscapeProgress.hiddenForKey(ghost,"hall_curtain",true),"Actual hiding fact rejected");
        require(!GhostEscapeProgress.keyOpportunity(ghost,true,true,true),"Key stolen immediately");ghost.putLong("ActiveTicks",2440);
        require(!GhostEscapeProgress.keyOpportunity(ghost,true,false,true),"Broken distraction counted");ghost.putLong("ActiveTicks",2460);
        require(!GhostEscapeProgress.keyOpportunity(ghost,true,true,true),"Interrupted progress accumulated");ghost.putLong("ActiveTicks",2519);
        require(!GhostEscapeProgress.keyOpportunity(ghost,true,true,true),"Continuous theft finished early");ghost.putLong("ActiveTicks",2520);
        require(GhostEscapeProgress.keyOpportunity(ghost,true,true,true)&&n.getBoolean("KeyStolen"),"Sustained physical opportunity did not steal key");
        n.putBoolean("PlayerEyesHeld",true);GhostEscapeProgress.move(n,GhostEscapeProgress.Phase.CRAWL,2520);
        require(!GhostEscapeProgress.resolveHunt(ghost,false,false)&&GhostEscapeProgress.phase(n)==GhostEscapeProgress.Phase.CRAWL,"An unfinished source-side crawl declared victory");
        UUID crossingAttempt=n.getUUID("Attempt");ghost=TagParser.parseTag(ghost.toString());n=GhostEscapeProgress.data(ghost);
        require(GhostEscapeProgress.phase(n)==GhostEscapeProgress.Phase.CRAWL&&n.getUUID("Attempt").equals(crossingAttempt),"Restart changed the pending physical crossing or its attempt");
        require(GhostEscapeProgress.resolveHunt(ghost,true,true)&&n.getBoolean("WagerWon"),"Safe crossing lost the same-tick catch tie");
        require(!GhostEscapeProgress.resolveHunt(ghost,false,true),"Pursuit reversed an already-safe victory");
        UUID escrow=UUID.randomUUID();var snapshot=new CompoundTag();snapshot.putUUID("Transaction",escrow);snapshot.putUUID("Owner",ghost.getUUID("Owner"));snapshot.putUUID("Door",ghost.getUUID("Door"));
        require(GhostEscapeProgress.beginRestoration(ghost,snapshot,true),"Safe arrival did not begin original escrow restoration");
        ghost=TagParser.parseTag(ghost.toString());n=GhostEscapeProgress.data(ghost);
        require(GhostEscapeProgress.phase(n)==GhostEscapeProgress.Phase.RESTORING&&n.getUUID("OriginalEscrow").equals(escrow),"Restart lost the in-progress original escrow transaction");
        require(!GhostEscapeProgress.beginRestoration(ghost,snapshot,true),"Restart began restoration twice");
        require(!GhostEscapeProgress.restorationCommitted(ghost,UUID.randomUUID(),true,true),"Foreign escrow committed");
        require(!GhostEscapeProgress.restorationCommitted(ghost,escrow,false,true),"Missing original inventory restoration counted");
        require(!GhostEscapeProgress.bodyRestored(ghost),"Pending restoration enabled normal-body presentation early");
        require(GhostEscapeProgress.restorationCommitted(ghost,escrow,true,true)&&!GhostEscapeProgress.restorationCommitted(ghost,escrow,true,true),"Restoration not exactly once");
        require(GhostEscapeProgress.bodyRestored(ghost),"Committed original escrow did not enable restored-body presentation");
        var wrongBody=ghost.copy();wrongBody.getCompound(GhostEscapeProgress.KEY).putUUID("RestoredEscrow",UUID.randomUUID());
        require(!GhostEscapeProgress.bodyRestored(wrongBody),"Mismatched escrow receipt cleared ghost restrictions");
        long bodyClock=GhostVictimProgress.clock(ghost);ghost.putDouble("Attention",1);ghost.putLong("PunishedUntil",bodyClock+9999);ghost.putLong("SuppressedUntil",bodyClock+9999);ghost.putBoolean("ReturningFromPunishment",true);
        require(!GhostVictimProgress.tick(ghost,GhostVictimProgress.Tuning.defaults())&&!GhostVictimProgress.imprisoned(ghost)&&!GhostVictimProgress.suppressed(ghost)&&!ghost.getBoolean("ReturningFromPunishment"),"Restored body was imprisoned again by stale ghost timers");
        require(GhostVictimProgress.clock(ghost)==bodyClock+1&&GhostEscapeProgress.bodyRestored(ghost)&&GhostEscapeProgress.phase(n)==GhostEscapeProgress.Phase.FAREWELL,"Stopping ghost punishment stalled farewell or lost the restoration receipt");
        ghost=session();n=GhostEscapeProgress.data(ghost);n.putBoolean("PlayerEyesHeld",true);GhostVictimProgress.leaveMirror(ghost);
        GhostEscapeProgress.move(n,GhostEscapeProgress.Phase.APPROACH_EXIT,GhostVictimProgress.clock(ghost));
        require(!GhostEscapeProgress.intercepted(ghost,false)&&GhostEscapeProgress.intercepted(ghost,true),"Wager interception accepted without the real Little Door approach");
        require(!GhostEscapeProgress.wagerAgreed(ghost,false,true)&&!GhostEscapeProgress.wagerAgreed(ghost,true,false),"Wager advanced without actual player consent");
        require(GhostEscapeProgress.wagerAgreed(ghost,true,true)&&GhostEscapeProgress.phase(n)==GhostEscapeProgress.Phase.WAGER,"Authenticated accepted wager did not start the hunt");
        GhostEscapeProgress.move(n,GhostEscapeProgress.Phase.HIDDEN,GhostVictimProgress.clock(ghost));
        require(!GhostEscapeProgress.signal(ghost,true,true,false),"Final run accepted without a server validated destination");
        n.putString("TargetHide","final_door");require(GhostEscapeProgress.signal(ghost,true,true,false)&&GhostEscapeProgress.phase(n)==GhostEscapeProgress.Phase.FINAL_CROSSING,"Verified final door signal did not start physical crossing");
        require(!GhostEscapeProgress.reachedDoor(ghost,false)&&GhostEscapeProgress.reachedDoor(ghost,true),"Door arrival was inferred from time instead of actual navigation");
        require(!GhostEscapeProgress.openedDoor(ghost,true),"Door crawl began before the opening animation window");
        ghost.putLong("ActiveTicks",GhostVictimProgress.clock(ghost)+60);require(GhostEscapeProgress.openedDoor(ghost,true),"Open physical door did not release the timed crawl stage");
        require(!GhostEscapeProgress.resolveHunt(ghost,false,false),"Door contact counted as a completed crossing before the crawl safe point");
        GhostEscapeProgress.move(n,GhostEscapeProgress.Phase.FAREWELL,GhostVictimProgress.clock(ghost));n.putBoolean("RestorationCommitted",true);n.putBoolean("WagerWon",true);
        require(!GhostEscapeProgress.childDeparture(ghost,false)&&GhostEscapeProgress.childDeparture(ghost,true),"Farewell accepted an unobserved child departure");
        require(GhostEscapeProgress.finishFarewell(ghost,true,true)&&GhostEscapeProgress.phase(n)==GhostEscapeProgress.Phase.COMPLETE,"Durable agency receipt did not finish the witnessed farewell");
        require(!GhostEscapeProgress.finishFarewell(ghost,true,true),"Farewell replay advanced the completed escape twice");
        var restored=TagParser.parseTag(ghost.toString());require(restored.equals(ghost),"Serialized checkpoint changed identities or progress");
        require(!GhostEscapeProgress.owns(restored,new CompoundTag()),"Unbound child inherited escape");
        var legacy=new CompoundTag();legacy.putBoolean("OriginalGhostHelper",true);legacy.putUUID("Id",n.getUUID("Victim"));legacy.putUUID("GhostRescueAttempt",n.getUUID("Attempt"));n.putBoolean("AgencyRegistrationSaved",true);
        require(com.coraline.tycoon.GhostRescueCandidates.legacyLineageMatches(ghost,legacy),"Completed rescue cannot authenticate its legacy candidate");
        var foreign=legacy.copy();foreign.putUUID("Id",UUID.randomUUID());require(!com.coraline.tycoon.GhostRescueCandidates.legacyLineageMatches(ghost,foreign),"Legacy migration accepted another child");
        foreign=legacy.copy();foreign.putUUID("GhostRescueAttempt",UUID.randomUUID());require(!com.coraline.tycoon.GhostRescueCandidates.legacyLineageMatches(ghost,foreign),"Legacy migration accepted another attempt");
        n.putBoolean("AgencyRegistrationSaved",false);require(!com.coraline.tycoon.GhostRescueCandidates.legacyLineageMatches(ghost,legacy),"Legacy migration accepted an unsaved agency handoff");
        var tycoon=new com.coraline.tycoon.TycoonData();UUID owner=UUID.randomUUID(),child=UUID.randomUUID(),attempt=UUID.randomUUID();var home=tycoon.home(owner);
        var identity=new CompoundTag();identity.putUUID("Id",child);identity.putString("Name","Robin Ellis");identity.putInt("Appearance",1947);identity.putString("Status","ACTIVE");identity.putDouble("MotherTrust",.4);
        var traits=new CompoundTag();traits.putDouble("Curiosity",.73);identity.put("Traits",traits);
        require(com.coraline.tycoon.GhostRescueCandidates.registerCandidate(home,identity,child,attempt,2),"Rescued child not registered");
        var candidate=home.state.getCompound("Candidates").getCompound(child.toString());
        require(candidate.getInt("Appearance")==1947&&candidate.getCompound("Traits").equals(traits)&&candidate.getString("Name").equals("Robin Ellis"),"Agency regenerated identity or personality");
        require(candidate.getDouble("Neglect")==2&&candidate.getString("Status").equals("CANDIDATE")&&!candidate.contains("MotherTrust"),"Ghost relationship leaked into ordinary care");
        require(!com.coraline.tycoon.GhostRescueCandidates.registerCandidate(home,identity,child,attempt,2),"Replayed farewell duplicated candidate");
        UUID originalActor=UUID.randomUUID();var actorAt=new net.minecraft.core.BlockPos(3,70,9);
        require(!com.coraline.tycoon.GhostRescueCandidates.bindOriginalActorReceipt(home,child,UUID.randomUUID(),originalActor,"minecraft:overworld",actorAt),"Foreign rescue attempt bound the agency actor");
        require(com.coraline.tycoon.GhostRescueCandidates.bindOriginalActorReceipt(home,child,attempt,originalActor,"minecraft:overworld",actorAt),"Original rescued actor could not bind its agency receipt");
        require(!com.coraline.tycoon.GhostRescueCandidates.bindOriginalActorReceipt(home,child,attempt,UUID.randomUUID(),"minecraft:overworld",actorAt),"Agency receipt silently replaced the original actor");
        require(home.state.getCompound("Candidates").getCompound(child.toString()).getUUID("GhostHelperActor").equals(originalActor),"Rejected replacement changed original actor identity");
        var permanent=com.coraline.tycoon.TycoonData.load(tycoon.save(new CompoundTag())).homes.get(owner);
        require(permanent.state.getCompound("Candidates").getCompound(child.toString()).equals(candidate),"Agency identity lost during permanent save/load");
        var resident=new com.coraline.tycoon.TycoonData.Resident(child,candidate.copy());resident.need("Neglect",37);
        require(resident.need("Neglect")==37,"Rescued child's Neglect was permanently locked");permanent.residents.put(child,resident);permanent.state.getCompound("Candidates").remove(child.toString());
        require(!com.coraline.tycoon.GhostRescueCandidates.registerCandidate(permanent,identity,child,UUID.randomUUID(),2),"Already-adopted child respawned in agency");
        ghost=session();n=GhostEscapeProgress.data(ghost);GhostVictimProgress.leaveMirror(ghost);GhostEscapeProgress.move(n,GhostEscapeProgress.Phase.HIDDEN,GhostVictimProgress.clock(ghost));n.putString("TargetHide","hide_1");ghost.putDouble("Attention",1);
        double motherTrust=GhostVictimProgress.motherTrust(ghost),ghostTrust=GhostVictimProgress.ghostTrust(ghost);
        require(!GhostVictimProgress.tick(ghost,GhostVictimProgress.Tuning.defaults())&&!GhostVictimProgress.imprisoned(ghost)&&GhostVictimProgress.suppressed(ghost),"Hunt attention teleported the ghost into normal imprisonment");
        require(GhostEscapeProgress.phase(n)==GhostEscapeProgress.Phase.HIDDEN&&GhostVictimProgress.motherTrust(ghost)==motherTrust&&GhostVictimProgress.ghostTrust(ghost)==ghostTrust,"Suppression rewrote the child's alliance or hunt phase");
        ghost.putDouble("Energy",1);require(!GhostEscapeProgress.signal(ghost,true,true,false),"Suppressed ghost can signal after an energy override");
        for(int tick=0;tick<1200;tick++)GhostVictimProgress.tick(ghost,GhostVictimProgress.Tuning.defaults());
        require(!GhostVictimProgress.suppressed(ghost)&&GhostVictimProgress.clock(ghost)>=1201,"Suppression stalled the authoritative clock or never expired");
        var longRoute=ghost.copy();var routeState=GhostEscapeProgress.data(longRoute);var routeNodes=new ListTag();var farNode=new CompoundTag();farNode.putString("Id","far_cover");farNode.putLong("Position",new net.minecraft.core.BlockPos(90,70,90).asLong());routeNodes.add(farNode);routeState.put("HidingNodes",routeNodes);routeState.putString("HidingSpot","original_cover");routeState.putString("TargetHide","far_cover");longRoute.putDouble("Energy",1);
        require(GhostEscapeProgress.signal(longRoute,true,true,false)&&GhostEscapeProgress.phase(routeState)==GhostEscapeProgress.Phase.MOVING_HIDE,"Validated distant cover was automatically refused");
        require(routeState.getString("HidingSpot").equals("original_cover")&&routeState.getString("TargetHide").equals("far_cover"),"Selecting distant cover fabricated arrival");
        require(GhostEscapeProgress.signal(longRoute,false,true,false)&&routeState.getBoolean("StopRequested")&&GhostEscapeProgress.phase(routeState)==GhostEscapeProgress.Phase.MOVING_HIDE&&routeState.getString("HidingSpot").equals("original_cover"),"Stop signal teleported or instantly concealed the moving child");
        UUID failedAttempt=n.getUUID("Attempt");GhostEscapeProgress.move(n,GhostEscapeProgress.Phase.FAILED_SEWING,GhostVictimProgress.clock(ghost));n.putBoolean("QuestEyesReturnedToBeldam",true);
        require(GhostVictimProgress.archiveVictim(ghost,"CAPTURED")&&!ghost.contains(GhostEscapeProgress.KEY),"New victim inherited the failed escape journal");
        require(GhostVictimProgress.child(ghost).isEmpty()&&ghost.getList("VictimHistory",Tag.TAG_COMPOUND).getCompound(0).getCompound(GhostEscapeProgress.KEY).getUUID("Attempt").equals(failedAttempt),"Capture lost the failed attempt history");
        ghost=session();n=GhostEscapeProgress.data(ghost);UUID searchAttempt=n.getUUID("Attempt");
        n.putBoolean("KeyStolen",true);n.putBoolean("EyeRoomKnown",true);n.putString("AssignedRoom",n.getString("EyeRoom"));
        n.getCompound("QuestInventory").putString("CustodySentinel","original");
        GhostEscapeProgress.move(n,GhostEscapeProgress.Phase.COLLECT_EYES,0);n.putBoolean("StoneVisible",true);ghost.putLong("ActiveTicks",1199);
        require(!GhostEscapeProgress.abandonBlockedRoom(ghost),"Search recovery interrupted a valid short retrieval");
        ghost.putLong("ActiveTicks",1200);require(GhostEscapeProgress.abandonBlockedRoom(ghost)&&GhostEscapeProgress.phase(n)==GhostEscapeProgress.Phase.LEAVE_ROOM,"Blocked retrieval did not start a physical retreat");
        require(n.getUUID("Attempt").equals(searchAttempt)&&n.getBoolean("KeyStolen")&&n.getBoolean("EyeRoomKnown")&&!n.getBoolean("PlayerEyesHeld")&&!n.getBoolean("StoneVisible"),"Retreat granted eyes or lost search/key identity");
        require(!GhostEscapeProgress.abandonBlockedRoom(ghost)&&!GhostEscapeProgress.leftRoom(ghost,false),"Retreat invented an exit or retriggered recovery");
        require(GhostEscapeProgress.leftRoom(ghost,true)&&GhostEscapeProgress.phase(n)==GhostEscapeProgress.Phase.ROOM_REPORT,"Actual exit did not restore the retry report");
        GhostEscapeProgress.move(n,GhostEscapeProgress.Phase.LOCKPICK,1200);n.putLong("PickStarted",1200);
        GhostEscapeProgress.move(n,GhostEscapeProgress.Phase.LEAVE_ROOM,1210);require(!n.contains("PickStarted"),"Cancelled lockpick kept progress for the next room");
        n.putBoolean("StoneObtained",true);n.putBoolean("ReportSpoken",true);n.putString("Blocker","old route");GhostEscapeProgress.move(n,GhostEscapeProgress.Phase.KEY_REPORT,1210);
        require(GhostEscapeProgress.assignedRoom(ghost,n.getString("EyeRoom"),true,true)&&!n.contains("ReportSpoken")&&!n.contains("Blocker"),"Retry retained an obsolete report or blocker");
        ghost.putLong("ActiveTicks",2410);require(GhostEscapeProgress.abandonBlockedRoom(ghost)&&GhostEscapeProgress.phase(n)==GhostEscapeProgress.Phase.READY,"Obstructed approach could not be reassigned without fabricating arrival");
        var foreignSearch=ghost.copy();foreignSearch.getCompound(GhostEscapeProgress.KEY).putUUID("Victim",UUID.randomUUID());
        require(!GhostEscapeProgress.abandonBlockedRoom(foreignSearch),"Another child's attempt was recovered");
        GhostEscapeProgress.move(n,GhostEscapeProgress.Phase.APPROACH_AMBUSH,2410);n.putString("TargetHide","blocked_cover");ghost.putLong("ActiveTicks",3609);
        require(!GhostEscapeProgress.expireAssignment(ghost),"Ambush approach expired before its movement budget");
        ghost.putLong("ActiveTicks",3610);require(GhostEscapeProgress.expireAssignment(ghost)&&GhostEscapeProgress.phase(n)==GhostEscapeProgress.Phase.READY&&!n.contains("TargetHide")&&!n.contains("HidingSpot"),"Blocked ambush route waited forever or fabricated concealment");
        require(!n.getBoolean("PlayerEyesHeld")&&n.getBoolean("KeyStolen")&&n.getUUID("Attempt").equals(searchAttempt),"Ambush recovery changed custody or quest identity");
        GhostVictimProgress.leaveMirror(ghost);ghost.putDouble("Energy",1);n.putBoolean("KeyStolen",false);
        GhostEscapeProgress.move(n,GhostEscapeProgress.Phase.STEAL_KEY,3610);n.putLong("OpportunityAt",3600);
        require(GhostEscapeProgress.signal(ghost,false,true,false)&&GhostEscapeProgress.phase(n)==GhostEscapeProgress.Phase.READY&&!n.contains("OpportunityAt")&&!n.getBoolean("KeyStolen"),"Stopping key theft routed the child to an unrelated searched room or granted the key");
        GhostEscapeProgress.move(n,GhostEscapeProgress.Phase.MOVING_HIDE,3610);
        GhostEscapeProgress.witnessed(ghost,GhostEscapeProgress.Discovery.PHYSICAL_CAPTURE,true);
        require(GhostEscapeProgress.phase(n)==GhostEscapeProgress.Phase.FAILED_SEWING,"Physical hunt capture did not enter canonical failure");
        var conversation=new CompoundTag();UUID wagerToken=UUID.randomUUID();conversation.putUUID("WagerConversation",wagerToken);conversation.putLong("WagerConversationUntil",700);conversation.putLong("WagerConversationReadyAt",120);
        require(GhostEscapeWager.conversationPending(conversation,101)&&GhostEscapeWager.conversationPending(conversation,700),"Visible wager token expired before its response deadline");
        require(!GhostEscapeWager.responseMatches(conversation,wagerToken,"0",119)&&GhostEscapeWager.responseMatches(conversation,wagerToken,"0",120),"Wager did not enforce its actual presentation delay");
        require(!GhostEscapeWager.responseMatches(conversation,wagerToken,"1",120)&&!GhostEscapeWager.responseMatches(conversation,wagerToken,"junk",120),"Removed refusal or malformed wager response was accepted");
        require(!GhostEscapeWager.responseMatches(conversation,UUID.randomUUID(),"0",120)&&!GhostEscapeWager.responseMatches(conversation,null,"0",120),"Foreign or missing token accepted a wager");
        require(!GhostEscapeWager.conversationPending(conversation,701)&&!GhostEscapeWager.responseMatches(conversation,wagerToken,"0",701),"Expired wager response remained valid");
        conversation=TagParser.parseTag(conversation.toString());require(GhostEscapeWager.responseMatches(conversation,wagerToken,"0",150),"Save/load lost the outstanding wager response");
        conversation.remove("WagerConversationReadyAt");require(!GhostEscapeWager.responseMatches(conversation,wagerToken,"0",119)&&GhostEscapeWager.responseMatches(conversation,wagerToken,"0",120),"Legacy wager presentation timing was reset or bypassed");
        conversation.remove("WagerConversation");require(!GhostEscapeWager.responseMatches(conversation,wagerToken,"0",150),"Consumed wager token could be replayed");
        ghost=session();n=GhostEscapeProgress.data(ghost);n.putString("InitialHide","distant_cover");n.putString("TargetHide","distant_cover");n.putBoolean("PlayerEyesHeld",true);GhostEscapeProgress.move(n,GhostEscapeProgress.Phase.WAGER,0);
        require(!GhostEscapeProgress.hidingArrival(ghost,"distant_cover",true),"Unagreed wager began hunting");n.putBoolean("WagerAgreed",true);
        require(!GhostEscapeProgress.hidingArrival(ghost,"distant_cover",false)&&GhostEscapeProgress.phase(n)==GhostEscapeProgress.Phase.WAGER,"Timer or route selection fabricated the initial hiding arrival");
        n.putString("TargetHide","near_door");require(!GhostEscapeProgress.hidingArrival(ghost,"near_door",true),"Wager opening bypassed its chosen distant cover");
        n.putString("TargetHide","distant_cover");require(GhostEscapeProgress.hidingArrival(ghost,"distant_cover",true)&&GhostEscapeProgress.phase(n)==GhostEscapeProgress.Phase.HIDDEN&&n.getString("HidingSpot").equals("distant_cover"),"Physical initial concealment did not start the hunt");
        require(!GhostEscapeProgress.hidingArrival(ghost,"distant_cover",true),"Initial hiding arrival was replayed");
        GhostVictimProgress.leaveMirror(ghost);double beforeMother=GhostVictimProgress.motherTrust(ghost),beforeGhost=GhostVictimProgress.ghostTrust(ghost);
        for(var action:new GhostEscapeProgress.Phase[]{GhostEscapeProgress.Phase.LOCKPICK,GhostEscapeProgress.Phase.SCAN,GhostEscapeProgress.Phase.COLLECT_EYES}){
            ghost.putDouble("Energy",1);n.putBoolean("PlayerEyesHeld",false);GhostEscapeProgress.move(n,action,GhostVictimProgress.clock(ghost));
            require(GhostEscapeProgress.stopSignalAvailable(action)&&GhostEscapeProgress.signal(ghost,false,true,false)&&GhostEscapeProgress.phase(n)==GhostEscapeProgress.Phase.LEAVE_ROOM,"Stop control could not cancel an active room operation: "+action);
            require(!n.getBoolean("PlayerEyesHeld")&&GhostVictimProgress.motherTrust(ghost)==beforeMother&&GhostVictimProgress.ghostTrust(ghost)==beforeGhost,"Room cancellation granted eyes or changed trust: "+action);
        }
        require(!GhostEscapeProgress.stopSignalAvailable(GhostEscapeProgress.Phase.INTERCEPT)&&!GhostEscapeProgress.stopSignalAvailable(GhostEscapeProgress.Phase.FAREWELL)&&!GhostEscapeProgress.stopSignalAvailable(GhostEscapeProgress.Phase.COMPLETE),"Stop control escaped a committed confrontation or ending");
        GhostEscapeProgress.move(n,GhostEscapeProgress.Phase.WAIT_GO,100);
        require(GhostEscapeProgress.countdown(n,2499)==1&&GhostEscapeProgress.countdown(n,2500)==0&&GhostEscapeProgress.countdown(n,9999)==0,"Signal countdown expired early or became negative");
        n.putLong("AmbushStarted",100);GhostEscapeProgress.move(n,GhostEscapeProgress.Phase.WAIT_LURE,100);require(GhostEscapeProgress.countdown(n,200)==5900,"Ambush HUD lost the five-minute window");
        GhostEscapeProgress.move(n,GhostEscapeProgress.Phase.STEAL_KEY,200);require(GhostEscapeProgress.countdown(n,201)==5899,"Starting key theft reset the ambush countdown");
        GhostEscapeProgress.move(n,GhostEscapeProgress.Phase.SCAN,100);require(GhostEscapeProgress.countdown(n,250)==150,"Room search lost its fifteen-second countdown");
        GhostEscapeProgress.move(n,GhostEscapeProgress.Phase.INTERCEPT,100);require(GhostEscapeProgress.countdown(n,250)==0&&GhostEscapeProgress.objective(n).contains("wager"),"Wager reused the obsolete bedtime timer or objective");
        GhostEscapeProgress.move(n,GhostEscapeProgress.Phase.LOCKPICK,100);require(GhostEscapeProgress.countdown(n,250)==0,"Walking to a lock invented active lockpicking progress");
        n.putLong("PickStarted",250);require(GhostEscapeProgress.countdown(n,275)==75,"Lockpicking countdown did not use its physical start");
        for(var stage:GhostEscapeProgress.Phase.values()){
            GhostEscapeProgress.move(n,stage,100);require(stage==GhostEscapeProgress.Phase.COMPLETE||!GhostEscapeProgress.objective(n).isBlank(),"Active escape stage lacks guidance: "+stage);
        }
        ghost=session();n=GhostEscapeProgress.data(ghost);UUID failedOwner=ghost.getUUID("Owner"),failedDoor=ghost.getUUID("Door");var failedLedger=new CompoundTag();
        UUID unloadedActor=UUID.randomUUID(),foreignHolder=UUID.randomUUID();ghost.putUUID("VictimActor",unloadedActor);
        GhostEscapeProgress.move(n,GhostEscapeProgress.Phase.FAILED_SEWING,50);var concealed=new CompoundTag();concealed.putString("Sentinel","original inventory");n.put("QuestInventory",concealed);
        var originalEye=new CompoundTag();UUID originalEyeId=UUID.randomUUID();originalEye.putUUID("Identity",originalEyeId);originalEye.putUUID("GhostEscapeHolder",n.getUUID("Attempt"));failedLedger.put("Eye0",originalEye);
        var otherEye=new CompoundTag();otherEye.putUUID("GhostEscapeHolder",foreignHolder);failedLedger.put("Eye1",otherEye);var claimedEye=new CompoundTag();claimedEye.putBoolean("Claimed",true);failedLedger.put("Eye2",claimedEye);
        require(!GhostEscapeProgress.handoffFailure(ghost)&&GhostVictimProgress.phase(ghost)!=GhostVictimProgress.Phase.VICTIM_CAPTURED,"Failure handed off before canonical custody was returned");
        var wrongOwner=ghost.copy();wrongOwner.putUUID("Owner",UUID.randomUUID());require(!GhostEscapeEyes.returnFailedCustody(wrongOwner,failedOwner,failedDoor,failedLedger)&&originalEye.hasUUID("GhostEscapeHolder"),"Another owner returned the failed child's eyes");
        var wrongDoor=ghost.copy();wrongDoor.putUUID("Door",UUID.randomUUID());require(!GhostEscapeEyes.returnFailedCustody(wrongDoor,failedOwner,failedDoor,failedLedger)&&n.contains("QuestInventory"),"Another realm consumed concealed quest inventory");
        var missingAttempt=ghost.copy();missingAttempt.getCompound(GhostEscapeProgress.KEY).remove("Attempt");require(!GhostEscapeEyes.returnFailedCustody(missingAttempt,failedOwner,failedDoor,failedLedger)&&!GhostEscapeProgress.handoffFailure(missingAttempt),"Missing attempt receipt authorized forfeiture or capture");
        require(GhostEscapeEyes.returnFailedCustody(ghost,failedOwner,failedDoor,failedLedger)&&!n.contains("QuestInventory")&&n.getBoolean("QuestEyesReturnedToBeldam"),"Unloaded child prevented failed custody recovery");
        require(!originalEye.hasUUID("GhostEscapeHolder")&&originalEye.getUUID("Identity").equals(originalEyeId)&&!originalEye.getBoolean("Claimed"),"Forfeiture replaced or claimed the original unresolved eye");
        require(otherEye.getUUID("GhostEscapeHolder").equals(foreignHolder)&&claimedEye.getBoolean("Claimed"),"Forfeiture rewrote another attempt or an already-recovered soul");
        require(GhostEscapeProgress.handoffFailure(ghost)&&GhostVictimProgress.phase(ghost)==GhostVictimProgress.Phase.VICTIM_CAPTURED&&ghost.getUUID("VictimActor").equals(unloadedActor),"Unloaded original actor could not enter canonical capture handoff");
        long captureStarted=GhostVictimProgress.age(ghost);require(!GhostEscapeProgress.handoffFailure(ghost)&&GhostVictimProgress.age(ghost)==captureStarted,"Repeated failure handoff reset the sewing recovery budget");
        require(GhostEscapeEyes.returnFailedCustody(ghost,failedOwner,failedDoor,failedLedger)&&!originalEye.hasUUID("GhostEscapeHolder"),"Repeated forfeiture was not idempotent");
        var releasedLedger=new CompoundTag();releasedLedger.putBoolean("Released",true);
        require(GhostEscapeEyes.returnFailedCustody(ghost,failedOwner,failedDoor,releasedLedger)&&releasedLedger.getAllKeys().size()==1,"Forfeiture recreated previously freed ghost eyes");
        var emptyLedger=new CompoundTag();require(GhostEscapeEyes.returnFailedCustody(ghost,failedOwner,failedDoor,emptyLedger)&&emptyLedger.isEmpty(),"Failure before collection minted new eye identities");
        require(GhostVictimProgress.archiveVictim(ghost,"CAPTURED")&&ghost.getList("VictimHistory",Tag.TAG_COMPOUND).getCompound(0).getUUID("RetiredActor").equals(unloadedActor),"Failure cleanup lost the late-loading original actor's retirement tombstone");
        for(String route:List.of("ACCEPTED_PROPOSAL","FINAL_BOSS_LOSS")){
            ghost=session();ghost.putString("EntryRoute",route);GhostVictimProgress.leaveMirror(ghost);n=GhostEscapeProgress.data(ghost);
            UUID endingOwner=ghost.getUUID("Owner"),endingDoor=ghost.getUUID("Door"),endingEscrow=UUID.randomUUID();
            require(GhostEscapeEndingProgress.begin(ghost,endingOwner,endingDoor)==null,"Unfinished escape opened the canonical ending: "+route);
            for(String receipt:List.of("WagerWon","QuestEyesSecured","RestorationCommitted","ChildDepartureWitnessed","FarewellComplete","AgencyRegistrationSaved"))n.putBoolean(receipt,true);
            n.putUUID("OriginalEscrow",endingEscrow);n.putUUID("RestoredEscrow",endingEscrow);GhostEscapeProgress.move(n,GhostEscapeProgress.Phase.COMPLETE,100);
            for(String receipt:List.of("WagerWon","QuestEyesSecured","RestorationCommitted","ChildDepartureWitnessed","FarewellComplete","AgencyRegistrationSaved")){
                var incomplete=ghost.copy();incomplete.getCompound(GhostEscapeProgress.KEY).remove(receipt);
                require(GhostEscapeEndingProgress.begin(incomplete,endingOwner,endingDoor)==null,"Ending ignored missing receipt "+receipt+" for "+route);
            }
            var ending=GhostEscapeEndingProgress.begin(ghost,endingOwner,endingDoor);
            require(ending!=null&&GhostEscapeEndingProgress.begin(ghost,endingOwner,endingDoor)==ending,"Ending rerolled its receipt for "+route);
            require(GhostEscapeEndingProgress.begin(ghost,UUID.randomUUID(),endingDoor)==null&&GhostEscapeEndingProgress.begin(ghost,endingOwner,UUID.randomUUID())==null,"Foreign owner/door acquired ending control");
            var wrongAttempt=ghost.copy();wrongAttempt.getCompound(GhostEscapeProgress.KEY).putUUID("Attempt",UUID.randomUUID());
            require(GhostEscapeEndingProgress.begin(wrongAttempt,endingOwner,endingDoor)==null&&wrongAttempt.getCompound(GhostEscapeProgress.KEY).getCompound(GhostEscapeEndingProgress.KEY).equals(ending),"Ending overwrote another attempt's handoff receipt");
            var corrupt=ghost.copy();corrupt.getCompound(GhostEscapeProgress.KEY).getCompound(GhostEscapeEndingProgress.KEY).putString("Stage","corrupt");require(GhostEscapeEndingProgress.begin(corrupt,endingOwner,endingDoor)==null,"Corrupt ending journal was silently reset");
            require(!GhostEscapeEndingProgress.keyPresent(ghost,endingOwner,endingDoor,false)&&GhostEscapeEndingProgress.keyPresent(ghost,endingOwner,endingDoor,true),"Key stage advanced without actual inventory proof");
            require(!GhostEscapeEndingProgress.sealed(ghost,endingOwner,endingDoor,false,true,true)&&!GhostEscapeEndingProgress.sealed(ghost,endingOwner,endingDoor,true,false,true)&&!GhostEscapeEndingProgress.sealed(ghost,endingOwner,endingDoor,true,true,false),"Door-sealing receipt accepted a foreign world, absent key or open portal");
            ghost=TagParser.parseTag(ghost.toString());require(GhostEscapeEndingProgress.sealed(ghost,endingOwner,endingDoor,true,true,true),"Saved valid ending could not record a real door seal");
            require(!GhostEscapeEndingProgress.sealed(ghost,endingOwner,endingDoor,true,true,true)&&GhostEscapeEndingProgress.existing(ghost,endingOwner,endingDoor).getBoolean("SealedByOwner"),"Door-sealing replay advanced the ending twice");
            var chapter=new CompoundTag();UUID realFamily=UUID.randomUUID(),realMom=UUID.randomUUID(),realDad=UUID.randomUUID();
            require(!GhostEscapeEndingProgress.beginEpilogue(ghost,endingOwner,endingDoor,chapter,realFamily,realMom,realDad,false,true)&&!GhostEscapeEndingProgress.beginEpilogue(ghost,endingOwner,endingDoor,chapter,realFamily,realMom,realDad,true,false),"Epilogue ignored absent original parents or unaccounted souls");
            require(!GhostEscapeEndingProgress.beginEpilogue(ghost,endingOwner,endingDoor,chapter,realFamily,realMom,realMom,true,true),"One parent UUID filled both reunion roles");
            var staleChapter=chapter.copy();staleChapter.putUUID("AlternateGhostAttempt",UUID.randomUUID());require(!GhostEscapeEndingProgress.beginEpilogue(ghost,endingOwner,endingDoor,staleChapter,realFamily,realMom,realDad,true,true),"Another rescue attempt acquired this ending's epilogue");
            UUID oldCombatInput=UUID.randomUUID();chapter.putUUID("Input",oldCombatInput);chapter.putString("Phase",RescueChapter.Phase.WEB_STEALTH.name());chapter.putInt("Ticks",999);chapter.putBoolean("Audio",true);
            require(!GhostEscapeEndingProgress.beginEpilogue(ghost,endingOwner,endingDoor,chapter,realFamily,realMom,realDad,true,true,RescueChapter.Phase.FINAL_M)&&!GhostEscapeEndingProgress.beginEpilogue(ghost,endingOwner,endingDoor,chapter,realFamily,realMom,realDad,true,true,null),"Ending accepted an absent or combat starting phase");
            require(GhostEscapeEndingProgress.beginEpilogue(ghost,endingOwner,endingDoor,chapter,realFamily,realMom,realDad,true,true),"Authentic completed escape could not enter the existing epilogue: "+route);
            require(chapter.getString("Phase").equals(RescueChapter.Phase.REUNION_A.name())&&chapter.getInt("Ticks")==0&&!chapter.getBoolean("Audio")&&!oldCombatInput.equals(chapter.getUUID("Input"))&&!chapter.getBoolean("ParentsReturned"),"Epilogue handoff retained combat state or prematurely rescued parents");
            chapter.putString("Phase",RescueChapter.Phase.WEB_STEALTH.name());require(!GhostEscapeEndingProgress.epilogueOwns(ghost,endingOwner,endingDoor,chapter),"Alternate ending reopened the final Beldam fight");
            chapter.putString("Phase",RescueChapter.Phase.REUNION_A.name());require(GhostEscapeEndingProgress.epilogueOwns(ghost,endingOwner,endingDoor,chapter),"Authenticated reunion phase did not own the epilogue");
            require(GhostEscapeEndingProgress.parentsMatch(ghost,endingOwner,endingDoor,realFamily,realMom,realDad)&&!GhostEscapeEndingProgress.parentsMatch(ghost,endingOwner,endingDoor,realFamily,UUID.randomUUID(),realDad),"Reunion replaced a saved original parent");
            for(String field:List.of("Owner","Door","Session","Attempt")){
                var foreignChapter=chapter.copy();foreignChapter.putUUID("AlternateGhost"+field,UUID.randomUUID());require(!GhostEscapeEndingProgress.epilogueOwns(ghost,endingOwner,endingDoor,foreignChapter),"Foreign "+field+" acquired epilogue control");
            }
            ghost=TagParser.parseTag(ghost.toString());chapter=TagParser.parseTag(chapter.toString());require(GhostEscapeEndingProgress.epilogueOwns(ghost,endingOwner,endingDoor,chapter),"Reload lost the original parent/session binding");
            require(!GhostEscapeEndingProgress.finishEpilogue(ghost,endingOwner,endingDoor,chapter,true),"Reunion start prematurely completed the canonical ending");chapter.putString("Phase",RescueChapter.Phase.COMPLETE.name());
            for(String receipt:List.of("ParentsReturned","FarewellComplete","FarewellCatDeparted"))chapter.putBoolean(receipt,true);
            for(String receipt:List.of("ParentsReturned","FarewellComplete","FarewellCatDeparted")){
                var unfinished=chapter.copy();unfinished.remove(receipt);require(!GhostEscapeEndingProgress.finishEpilogue(ghost,endingOwner,endingDoor,unfinished,true),"Alternative victory ignored missing "+receipt);
            }
            require(!GhostEscapeEndingProgress.finishEpilogue(ghost,endingOwner,endingDoor,chapter,false),"Alternative victory abandoned still-trapped original souls");chapter.put("VisionReturn",new CompoundTag());
            require(!GhostEscapeEndingProgress.finishEpilogue(ghost,endingOwner,endingDoor,chapter,true),"Alternative victory skipped restoration from the farewell vision");chapter.putBoolean("VisionRestored",true);
            require(GhostEscapeEndingProgress.finishEpilogue(ghost,endingOwner,endingDoor,chapter,true)&&!chapter.getBoolean("HandSevered")&&chapter.contains("VisionReturn"),"Completed farewell lost its saved return snapshot or fabricated the Hand encounter");
            require(!GhostEscapeEndingProgress.finishEpilogue(ghost,endingOwner,endingDoor,chapter,true)&&GhostEscapeEndingProgress.epilogueOwns(ghost,endingOwner,endingDoor,chapter),"Completed epilogue replayed or returned to the ghost story");
            UUID otherMother=UUID.randomUUID(),originalHand=UUID.randomUUID();
            require(GhostEscapeHandProgress.begin(ghost,endingOwner,endingDoor,otherMother,originalHand,false,false)==null&&GhostEscapeHandProgress.begin(ghost,endingOwner,endingDoor,otherMother,otherMother,true,false)==null,"Hand staging accepted absent physical proof or reused the mother's UUID");
            var unfinishedHand=ghost.copy();unfinishedHand.getCompound(GhostEscapeProgress.KEY).getCompound(GhostEscapeEndingProgress.KEY).putString("Stage",GhostEscapeEndingProgress.Stage.EPILOGUE.name());
            require(GhostEscapeHandProgress.begin(unfinishedHand,endingOwner,endingDoor,otherMother,originalHand,true,false)==null,"Hand appeared before the canonical farewell completed");
            var handReceipt=GhostEscapeHandProgress.begin(ghost,endingOwner,endingDoor,otherMother,originalHand,true,false);
            require(handReceipt!=null&&GhostEscapeHandProgress.begin(ghost,endingOwner,endingDoor,otherMother,originalHand,true,false)==handReceipt,"Hand staging replaced its existing receipt");
            require(GhostEscapeHandProgress.begin(ghost,endingOwner,endingDoor,otherMother,UUID.randomUUID(),true,false)==null&&GhostEscapeHandProgress.existing(ghost,endingOwner,endingDoor,UUID.randomUUID())==null,"Hand staging replaced the original part or mother");
            require(!GhostEscapeHandProgress.landed(ghost,endingOwner,endingDoor,otherMother,originalHand,true)&&!GhostEscapeHandProgress.handoffReady(ghost,endingOwner,endingDoor,otherMother,originalHand),"Reaching Hand became a well encounter before the actual cut");
            for(String field:List.of("Owner","Door","Session","Attempt")){
                var foreignHand=ghost.copy();foreignHand.getCompound(GhostEscapeProgress.KEY).getCompound(GhostEscapeEndingProgress.KEY).getCompound(GhostEscapeHandProgress.KEY).putUUID(field,UUID.randomUUID());
                require(GhostEscapeHandProgress.existing(foreignHand,endingOwner,endingDoor,otherMother)==null&&GhostEscapeHandProgress.begin(foreignHand,endingOwner,endingDoor,otherMother,originalHand,true,false)==null,"Foreign Hand "+field+" was accepted or overwritten");
            }
            require(!GhostEscapeHandProgress.cut(ghost,endingOwner,endingDoor,otherMother,originalHand,false,true,true)&&!GhostEscapeHandProgress.cut(ghost,endingOwner,endingDoor,otherMother,originalHand,true,false,true)&&!GhostEscapeHandProgress.cut(ghost,endingOwner,endingDoor,otherMother,originalHand,true,true,false),"Hand cut bypassed the closed original door, key or physical reach");
            require(!GhostEscapeHandProgress.cut(ghost,endingOwner,endingDoor,otherMother,null,true,true,true)&&!GhostEscapeHandProgress.cut(ghost,endingOwner,endingDoor,otherMother,UUID.randomUUID(),true,true,true),"Hand cut accepted an absent or different actor");
            ghost=TagParser.parseTag(ghost.toString());
            require(GhostEscapeHandProgress.cut(ghost,endingOwner,endingDoor,otherMother,originalHand,true,true,true)&&!GhostEscapeHandProgress.cut(ghost,endingOwner,endingDoor,otherMother,originalHand,true,true,true),"Reload lost the real cut or replayed it");
            ghost=TagParser.parseTag(ghost.toString());
            require(!GhostEscapeHandProgress.landed(ghost,endingOwner,endingDoor,otherMother,originalHand,false)&&!GhostEscapeHandProgress.landed(ghost,endingOwner,endingDoor,otherMother,null,true)&&!GhostEscapeHandProgress.handoffReady(ghost,endingOwner,endingDoor,otherMother,originalHand),"Elapsed time or absent actor fabricated ground contact");
            require(GhostEscapeHandProgress.landed(ghost,endingOwner,endingDoor,otherMother,originalHand,true)&&!GhostEscapeHandProgress.landed(ghost,endingOwner,endingDoor,otherMother,originalHand,true),"Actual landing failed or replayed");
            require(GhostEscapeHandProgress.handoffReady(ghost,endingOwner,endingDoor,otherMother,originalHand)&&!GhostEscapeHandProgress.handoffReady(ghost,endingOwner,endingDoor,otherMother,UUID.randomUUID()),"Well handoff lost the original Hand identity");
            var oldCut=ghost.copy();oldCut.getCompound(GhostEscapeProgress.KEY).getCompound(GhostEscapeEndingProgress.KEY).remove(GhostEscapeHandProgress.KEY);
            require(GhostEscapeHandProgress.begin(oldCut,endingOwner,endingDoor,otherMother,originalHand,true,true)!=null&&GhostEscapeHandProgress.handoffReady(oldCut,endingOwner,endingDoor,otherMother,originalHand),"Existing canonical physical cut could not retain its Hand");
        }
        var firstKeyStory=new CompoundTag();var firstKeyRescue=new CompoundTag();
        require(GhostEscapeEndingProgress.mayIssueFirstKey(firstKeyStory,firstKeyRescue),"Proposal route could not use the normal first key issuance");
        for(String marker:List.of("LittleDoorKeyIssued","LittleDoorKeyCustodyVersion","LittleDoorKeyHolder")){
            var issued=firstKeyStory.copy();if(marker.endsWith("Issued"))issued.putBoolean(marker,true);else if(marker.endsWith("Version"))issued.putInt(marker,1);else issued.putString(marker,"EVIL_OTHER_MOTHER");
            require(!GhostEscapeEndingProgress.mayIssueFirstKey(issued,firstKeyRescue),"Ending authorized a replacement for existing key receipt "+marker);
        }
        for(String marker:List.of("KeyTaken","KeySwallowed","KeyCustodyVersion","LegacyKeyReplacementReceipt")){
            var issued=firstKeyRescue.copy();if(marker.endsWith("Version"))issued.putInt(marker,1);else if(marker.endsWith("Receipt"))issued.putUUID(marker,UUID.randomUUID());else issued.putBoolean(marker,true);
            require(!GhostEscapeEndingProgress.mayIssueFirstKey(firstKeyStory,issued),"Ending replaced swallowed/legacy key: "+marker);
        }
        var existingCustody=new CompoundTag();existingCustody.put("Key",new CompoundTag());firstKeyRescue.put("Custody",existingCustody);
        require(!GhostEscapeEndingProgress.mayIssueFirstKey(firstKeyStory,firstKeyRescue),"Empty or damaged existing custody authorized another key");
        var malformedIssue=new CompoundTag();malformedIssue.putString("LittleDoorKeyIssued","broken");require(!GhostEscapeEndingProgress.mayIssueFirstKey(malformedIssue,new CompoundTag()),"Malformed issued-key receipt authorized a new key");
        var readOnlyEyes=new CompoundTag();require(!RescueEyes.all(readOnlyEyes)&&readOnlyEyes.isEmpty(),"Reading absent eye progress created new identities");readOnlyEyes.putBoolean("Released",true);
        require(RescueEyes.all(readOnlyEyes)&&readOnlyEyes.getAllKeys().size()==1,"Previously liberated souls were recreated for the alternate ending");
        var originalGhosts=new CompoundTag();var acceptedGhosts=new CompoundTag();UUID originalGhost=UUID.randomUUID(),branchDuplicate=UUID.randomUUID();originalGhosts.putUUID("Ghost0",originalGhost);acceptedGhosts.putUUID("Ghost0",branchDuplicate);
        require(RescueEpilogue.savedGhostReference(originalGhosts,acceptedGhosts,0).equals(originalGhost),"Boss-loss farewell replaced the original ghost UUID with a branch duplicate");
        require(RescueEpilogue.savedGhostReference(new CompoundTag(),acceptedGhosts,0).equals(branchDuplicate),"Proposal farewell lost its original accepted-scene ghost");
        require(RescueEpilogue.savedGhostReference(new CompoundTag(),new CompoundTag(),0)==null&&RescueEpilogue.savedGhostReference(originalGhosts,acceptedGhosts,3)==null,"Absent or invalid ghost slot authorized a new original actor");
        System.out.println("GHOST_ESCAPE_NBT_OK checks="+checks+"; component only, no physical/visual acceptance");
    }
    private GhostEscapeStateCheck(){}
}
