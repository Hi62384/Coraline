package com.coraline.feast;

import java.util.UUID;
import com.coraline.block.TinyDoorBlock;
import com.coraline.otherworld.*;
import com.coraline.tycoon.TycoonNpc;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

/** Authoritative continuation of the same accepted-route journal. */
public final class GhostVictimDirector {
    private static final int SUPPER_TICKS=1800;
    public static boolean active(VisitData.Scene s){return s!=null&&AcceptedButtonEyes.active(s)&&GhostVictimEntry.data(s).getBoolean("Started");}
    /** Bound the authored, non-choice cutscene beats. Deliberate mirror dialogue,
     * exploration and bedtime waiting remain player-paced. A recovery never
     * awards trust for a meal/gift the child did not actually complete. */
    public static void cutsceneSafety(ServerPlayer p,VisitData.Scene s){
        if(!active(s))return;var n=GhostVictimEntry.data(s);if(GhostEscapeProgress.bodyRestored(n))return;var phase=GhostVictimProgress.phase(n);
        if(phase==GhostVictimProgress.Phase.INTRODUCTION&&!n.getBoolean("VoiceStarted")
            &&!arrivalReady(p,s,AcceptedButtonEyesWorld.level(p),AcceptedButtonEyesWorld.mother(p,s)))return;
        int budget=switch(phase){case INTRODUCTION->Math.max(1200,StoryCutsceneWatchdog.voiceBudget(OtherMotherWelcome.State.HIGH_TRUST_LOW_SUSPICION.sound(0)));
            case FIRST_SUPPER->SUPPER_TICKS+2400;
            case DAILY_ARRIVAL,VICTIM_CAPTURED,VICTIM_ESCAPED->1800;
            case DAILY_GIFT->Math.max(1800,GhostVictimConfig.GIFT_SECONDS.get()*20+1200);
            case BEDTIME->n.contains("BedtimeArrived")?0:1800;
            case NIGHT_PREPARATION->Math.max(1800,GhostVictimConfig.PREPARATION_SECONDS.get()*20+1200);default->0;};
        if(budget==0||!StoryCutsceneWatchdog.overdue(p,s,"next_victim",phase.name(),budget))return;
        var child=GhostVictimActors.find(p,s,n);if(child!=null){child.stopRiding();child.setPose(Pose.STANDING);child.refreshDimensions();child.gesture(0,0);}
        switch(phase){

            case INTRODUCTION->{if(child!=null)child.getNavigation().stop();move(p,s,n,GhostVictimProgress.Phase.FIRST_SUPPER);}
            case FIRST_SUPPER->finishSupper(p,s,n,child,false);
            case BEDTIME->{n.putString("LastRouteBlocker","The child could not reach the bedroom; skipped bedtime safely.");GhostVictimSchedule.tomorrow(n);move(p,s,n,GhostVictimProgress.Phase.NIGHT_PREPARATION);}
            case NIGHT_PREPARATION->{
                var home=AcceptedButtonEyesWorld.level(p);
                if(child==null||home==null){StoryCutsceneWatchdog.retry(s,"next_victim");return;}
                if(child!=null&&home!=null&&child.level()!=home){
                    var recovered=ShowTravel.guest(child,p,s,home,Vec3.atBottomCenterOf(GhostVictimWorld.hall(s)),false,"VictimHomeVisit");
                    if(!(recovered instanceof TycoonNpc same)){StoryCutsceneWatchdog.retry(s,"next_victim");return;}
                    same.setInvisible(false);same.noPhysics=false;same.setNoAi(false);same.setNoGravity(false);GhostVictimActors.remember(n,same);
                }
                GhostVictimActivities.finishScouting(p,s,n);n.remove("ChildAtHome");
                if(!GhostVictimSchedule.morning(n)){StoryCutsceneWatchdog.retry(s,"next_victim");return;}
                n.remove("ChildRoutine");OtherMotherLoop.changed(p);
            }
            case DAILY_ARRIVAL->{
                var home=AcceptedButtonEyesWorld.level(p);
                if(child==null||home==null){StoryCutsceneWatchdog.retry(s,"next_victim");return;}
                if(child!=null&&home!=null&&child.level()!=home){
                    var recovered=ShowTravel.guest(child,p,s,home,Vec3.atBottomCenterOf(GhostVictimWorld.hall(s)),false,"VictimHomeVisit");
                    if(!(recovered instanceof TycoonNpc same)){StoryCutsceneWatchdog.retry(s,"next_victim");return;}
                    same.setInvisible(false);same.noPhysics=false;same.setNoAi(false);same.setNoGravity(false);GhostVictimActors.remember(n,same);
                }
                n.remove("ChildAtHome");n.remove("CrawlExit");move(p,s,n,GhostVictimProgress.Phase.DAILY_GIFT);
            }
            case DAILY_GIFT->{GhostVictimManifestations.show(p,s,n,"gift_"+GhostVictimSchedule.today(n).getString("Gift"),false);n.remove("GiftPlaced");n.remove("GiftPresentedAt");move(p,s,n,GhostVictimProgress.Phase.BETWEEN_ACTIVITIES);}
            case VICTIM_CAPTURED->{
                var failed=n.getCompound(GhostEscapeProgress.KEY);
                if(GhostEscapeProgress.owns(n,failed)&&GhostEscapeProgress.phase(failed)==GhostEscapeProgress.Phase.FAILED_SEWING&&!GhostEscapeEyes.forfeit(p,s)){StoryCutsceneWatchdog.retry(s,"next_victim");return;}
                var needle=FirstEscapeScene.prop(p,s,"VictimNeedle");if(needle!=null)needle.discard();n.remove("CaptureSeatedAt");GhostVictimProgress.archiveVictim(n,"CAPTURED");OtherMotherLoop.changed(p);
            }
            case VICTIM_ESCAPED->{
                if(child==null){StoryCutsceneWatchdog.retry(s,"next_victim");return;}
                var originalDoor=DoorData.get(p.server).doors.get(s.door);var source=originalDoor==null?null:com.coraline.parents.FamilyData.level(p.server,originalDoor.dimension);
                var safe=source==null?null:OtherWorld.safePlayer(source,originalDoor.pos);
                if(source==null||safe==null){StoryCutsceneWatchdog.retry(s,"next_victim");return;}
                var returned=ShowTravel.guest(child,p,s,source,Vec3.atBottomCenterOf(safe),false,"VictimHomeVisit");
                if(!(returned instanceof TycoonNpc)){StoryCutsceneWatchdog.retry(s,"next_victim");return;}
                returned.setInvisible(true);returned.noPhysics=true;returned.setNoAi(true);returned.getNavigation().stop();
                GhostVictimProgress.archiveVictim(n,"ESCAPED");OtherMotherLoop.changed(p);
            }
            default->{}
        }
    }
    public static boolean tick(ServerPlayer p,VisitData.Scene s){
        if(!active(s)&&!GhostVictimEntry.begin(p,s))return false;
        var n=GhostVictimEntry.data(s);if(!GhostVictimMirrors.owned(p,s,n))return false;
        if(GhostVictimProgress.phase(n)==GhostVictimProgress.Phase.NEW_VICTIM_RESET)GhostVictimActivities.retireOuting(p,s,n);
        long game=p.server.overworld().getGameTime();if(n.getLong("LastServerTick")==game)return true;n.putLong("LastServerTick",game);
        var previousSpeaker=com.coraline.multiplayer.GhostCooperation.speaker(p,s,n);
        boolean wasInMirror=GhostVictimProgress.mirror(n);
        boolean punished=GhostVictimProgress.tick(n,GhostVictimConfig.tuning());
        if(wasInMirror&&!GhostVictimProgress.mirror(n)){GhostVictimMirrors.close(p);if(previousSpeaker!=null&&previousSpeaker!=p)GhostVictimMirrors.close(previousSpeaker);}
        if(punished){FirstEscapeScene.preparePrison(p,s);n.putBoolean("PrisonRetired",false);n.putBoolean("ReturningFromPunishment",true);AcceptedButtonEyesWorld.transfer(p,s,FirstEscapeScene.prison(s));GhostVictimMirrors.close(p);}
        if(GhostVictimProgress.imprisoned(n)){
            if(p.distanceToSqr(FirstEscapeScene.prison(s).getCenter())>9)AcceptedButtonEyesWorld.transfer(p,s,FirstEscapeScene.prison(s));
        }else{
            if(n.getBoolean("ReturningFromPunishment")){n.remove("ReturningFromPunishment");if(AcceptedButtonEyesWorld.transfer(p,s,GhostVictimWorld.hall(s))){FirstEscapeScene.retirePrison(p,s);n.putBoolean("PrisonRetired",true);}}
            var escape=n.getCompound(GhostEscapeProgress.KEY);var escapePhase=GhostEscapeProgress.phase(escape);
            if(!GhostEscapeProgress.owns(n,escape)||!java.util.Set.of(GhostEscapeProgress.Phase.SAFE,GhostEscapeProgress.Phase.RESTORING,GhostEscapeProgress.Phase.FAREWELL,GhostEscapeProgress.Phase.COMPLETE).contains(escapePhase))GhostVictimEntry.ghostControls(p,s,n);
        }
        if(p.tickCount%20==0)AcceptedButtonEyesInventory.enforce(p);
        if(n.getCompound(GhostEscapeProgress.KEY).hasUUID("Attempt")){
            if(GhostEscapeRestoration.tick(p,s,n)){if(p.tickCount%10==0)GhostVictimSnapshot.send(p,s);return true;}
            if(GhostEscapeProgress.phase(n.getCompound(GhostEscapeProgress.KEY))==GhostEscapeProgress.Phase.INTERCEPT){GhostEscapeWager.open(p,s,n);}
            boolean hunting=GhostEscapeHunt.tick(p,s,n);
            GhostEscapeCovert.monitor(p,s,n);
            if(hunting||GhostEscapeAmbush.tick(p,s,n)||GhostEscapeCovert.tick(p,s,n)){
                GhostVictimManifestations.tick(p,s,n);if(p.tickCount%10==0)GhostVictimSnapshot.send(p,s);return true;
            }
        }
        if(!GhostVictimEntry.tick(p,s))houseTick(p,s,n);
        GhostVictimManifestations.tick(p,s,n);
        if(GhostVictimProgress.mirror(n)&&n.hasUUID("Conversation")){
            var child=GhostVictimActors.find(p,s,n);
            var speaker=com.coraline.multiplayer.GhostCooperation.speaker(p,s,n);
            if(speaker==null||!GhostVictimMirrors.childPresent(speaker,n,n.getString("Mirror"),child)){if(speaker!=null)GhostVictimMirrors.close(speaker);GhostVictimProgress.leaveMirror(n);GhostVictimMirrors.close(p);}
        }
        if(p.tickCount%10==0)GhostVictimSnapshot.send(p,s);if(p.tickCount%20==0)OtherMotherLoop.changed(p);return true;
    }
    private static void move(ServerPlayer p,VisitData.Scene s,CompoundTag n,GhostVictimProgress.Phase phase){
        ShowPlayback.stop(p,s);GhostVictimProgress.move(n,phase);n.remove("ChildRoutine");OtherMotherLoop.changed(p);
    }
    private static void houseTick(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        var realm=AcceptedButtonEyesWorld.realm(p,s);var level=AcceptedButtonEyesWorld.level(p);if(realm==null||level==null)return;
        var child=GhostVictimActors.find(p,s,n);
        if(child!=null&&GhostVictimProgress.child(n).getBoolean("PendingGhostReport")){
            var mother=AcceptedButtonEyesWorld.mother(p,s);
            if(mother!=null&&child.level()==mother.level()&&child.distanceToSqr(mother)<36){
                child.getLookControl().setLookAt(mother,30,30);
                if(GhostVictimNavigation.canSee(child,mother,6,140)){
                    GhostVictimProgress.child(n).remove("PendingGhostReport");n.putDouble("Attention",GhostVictimProgress.unit(GhostVictimProgress.attention(n)+.12));
                    words(n,"Something in the mirror frightened me. Please make it stop.");
                    n.putLong("MotherInvestigation",child.blockPosition().asLong());n.putLong("MotherInvestigateUntil",GhostVictimProgress.clock(n)+200);
                }
            }
        }
        var phase=GhostVictimProgress.phase(n);
        if(child!=null&&child.level()==level&&phase!=GhostVictimProgress.Phase.NIGHT_PREPARATION&&phase!=GhostVictimProgress.Phase.VICTIM_CAPTURED&&phase!=GhostVictimProgress.Phase.VICTIM_ESCAPED){
            if(GhostVictimProgress.motherTrust(n)>=1){child.stopRiding();move(p,s,n,GhostVictimProgress.Phase.VICTIM_CAPTURED);}
            else if(GhostVictimProgress.motherTrust(n)<=0){child.stopRiding();move(p,s,n,GhostVictimProgress.Phase.VICTIM_ESCAPED);}
        }
        switch(GhostVictimProgress.phase(n)){
            case WAITING_FOR_VICTIM->{
                var host=AcceptedButtonEyesWorld.mother(p,s);
                if(!arrivalReady(p,s,level,host)){
                    n.putString("Blocker","Waiting for the ghost and Other Mother to reach the Little Door room.");
                    return;
                }
                // Arrival is an authored scene, not a functional portal traversal.
                // A missing door state must not prevent the child from appearing.
                var state=level.getBlockState(realm.doorPos);
                var facing=state.hasProperty(TinyDoorBlock.FACING)?state.getValue(TinyDoorBlock.FACING):net.minecraft.core.Direction.SOUTH;
                var entry=BlockPos.containing(TunnelPassage.inside(realm.doorPos,facing,1.2));
                if(child==null&&n.contains("ArrivalRetryAt")&&GhostVictimProgress.clock(n)<n.getLong("ArrivalRetryAt"))return;
                n.putLong("ArrivalRetryAt",GhostVictimProgress.clock(n)+20);
                child=GhostVictimActors.arrive(p,s,n,level,entry);
                if(child==null&&!n.hasUUID("VictimActor")){
                    // A blocked crawl mouth must not strand this introduction.
                    for(int distance=1;distance<=3&&child==null;distance++)child=GhostVictimActors.arrive(p,s,n,level,realm.doorPos.relative(facing,distance));
                }
                if(child==null&&!n.hasUUID("VictimActor")){
                    var safe=OtherWorld.safePlayer(level,realm.doorPos.relative(facing,2));
                    if(safe!=null){child=GhostVictimActors.arrive(p,s,n,level,safe);if(child!=null)n.putLong("CrawlExit",safe.asLong());}
                }
                if(child==null&&!n.hasUUID("VictimActor")){
                    // Furniture or the waiting mother can occupy the crawl mouth.
                    // Search the actual room floor without replacing terrain or identity.
                    var middle=FirstEscapeScene.room(s);
                    for(int radius=1;radius<=6&&child==null;radius++)for(int dx=-radius;dx<=radius&&child==null;dx++)for(int dz=-radius;dz<=radius&&child==null;dz++){
                        if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
                        var candidate=middle.offset(dx,0,dz);if(!FirstEscapeScene.inRoom(s,candidate))continue;
                        child=GhostVictimActors.arrive(p,s,n,level,candidate);
                        if(child!=null)n.putLong("CrawlExit",candidate.asLong());
                    }
                }
                if(child==null&&!n.hasUUID("VictimActor")&&GhostVictimProgress.age(n)>=200){
                    child=GhostVictimActors.arrive(p,s,n,level,entry,true);
                }
                if(child==null&&GhostVictimProgress.age(n)>=100)child=GhostVictimActors.recoverArrival(p,s,n,level,entry);
                if(child==null){n.putString("Blocker","Waiting for the child's original actor and a clear Little Door passage.");return;}
                if(child.level()!=level){
                    var returned=ShowTravel.guest(child,p,s,level,Vec3.atBottomCenterOf(entry),false,"VictimHomeVisit");
                    if(!(returned instanceof TycoonNpc same))return;
                    child=same;GhostVictimActors.remember(n,child);
                }
                child.setInvisible(false);
                if(!n.contains("CrawlExit"))n.putLong("CrawlExit",realm.doorPos.relative(facing,2).asLong());
                move(p,s,n,GhostVictimProgress.Phase.INTRODUCTION);
                n.remove("Blocker");n.remove("ArrivalRetryAt");
            }
            case INTRODUCTION->{
                if(child==null)return;
                var mother=AcceptedButtonEyesWorld.mother(p,s);
                if(mother==null||mother.level()!=level||!n.getBoolean("VoiceStarted")&&!arrivalReady(p,s,level,mother))return;
                var target=BlockPos.of(n.getLong("CrawlExit"));
                if(child.getPose()==Pose.SWIMMING){
                    child.gesture(6,5);
                    if(n.getBoolean("EmergenceRecovery")||GhostVictimProgress.age(n)>=100){
                        // Only this short authored emergence bypasses a wedged crawl
                        // route. Normal child navigation resumes after the room landing.
                        child.getNavigation().stop();child.noPhysics=true;child.setNoGravity(true);
                        var goal=Vec3.atBottomCenterOf(target);var delta=goal.subtract(child.position());
                        child.setPos(child.position().add(delta.length()>.12?delta.normalize().scale(.12):delta));
                        GhostVictimActors.remember(n,child);
                        if(child.distanceToSqr(goal)>.04)return;
                    }else if(GhostVictimRoutines.travel(p,s,n,child,target,false)!=GhostVictimNavigation.Result.ARRIVED)return;
                    child.setPose(Pose.STANDING);child.gesture(0,0);child.refreshDimensions();child.noPhysics=false;child.setNoGravity(false);child.setNoAi(false);n.remove("EmergenceRecovery");n.remove("ChildRoutine");}
                mother.getLookControl().setLookAt(child,30,30);mother.familyGesture(66,5);child.getLookControl().setLookAt(mother,30,30);
                if(!n.getBoolean("VoiceStarted")&&ShowPlayback.start(p,s,OtherMotherWelcome.State.HIGH_TRUST_LOW_SUSPICION.sound(0),false))n.putBoolean("VoiceStarted",true);
                if(n.getBoolean("VoiceStarted")&&ShowPlayback.tick(p,s)||GhostVictimProgress.age(n)>1200){
                    words(n,"This looks just like home... Are you my other mother?");move(p,s,n,GhostVictimProgress.Phase.FIRST_SUPPER);}
            }
            case FIRST_SUPPER->{
                if(child==null||child.level()!=level)return;
                var father=AcceptedButtonEyesWorld.father(p,s);
                if(father==null&&p.tickCount%20==0&&p.level()==level){
                    VisitEvents.ensureFather(p,s);father=AcceptedButtonEyesWorld.father(p,s);
                    if(father!=null){
                        father.getPersistentData().putUUID("AcceptedOwner",s.owner);
                        GhostVictimEntry.disguise(p,s,n);
                    }
                }
                GhostVictimWorld.ensureHouseProps(p,s,n);
                if(!GhostVictimWorld.ensureSupperPoints(p,s,n))return;
                var table=GhostVictimWorld.diningTable(p,s);
                var destination=table.south(2);
                long supperClock=GhostVictimProgress.clock(n);
                if(!n.contains("SupperWalkStarted"))n.putLong("SupperWalkStarted",supperClock);
                var seat=FirstEscapeScene.prop(p,s,"VictimSupperSeat");
                if(seat==null||child.getVehicle()!=seat){
                    var scenery=FirstEscapeScene.context(s);
                    if(seat==null&&scenery.hasUUID("VictimSupperSeat")){
                        scenery.remove("VictimSupperSeat");
                        scenery.remove("VictimSupperSeatPosition");
                        OtherMotherLoop.changed(p);
                    }
                    var route=GhostVictimRoutines.travel(p,s,n,child,destination,false);
                    if(route!=GhostVictimNavigation.Result.ARRIVED){
                        if(supperClock-n.getLong("SupperWalkStarted")<160)return;
                        // A saved house can have a blocked supper approach. Recover
                        // at a collision-tested place beside the table so the meal
                        // does not wait indefinitely for an impossible path.
                        BlockPos recovery=null;
                        for(var candidate:new BlockPos[]{destination,destination.east(),destination.west(),table.north(2),table.east(2),table.west(2)}){
                            if(GhostVictimNavigation.clear(level,child,candidate)){recovery=candidate;break;}
                        }
                        if(recovery==null)return;
                        destination=recovery;
                        child.getNavigation().stop();child.setPos(Vec3.atBottomCenterOf(recovery));
                        n.remove("ChildRoutine");
                    }
                    seat=FirstEscapeScene.ensure(p,s,"VictimSupperSeat",CinematicPropEntity.SEAT,101,level,Vec3.atBottomCenterOf(destination));
                    if(seat==null||!child.startRiding(seat,true))return;
                    seat.setYRot(180);
                }
                var mother=AcceptedButtonEyesWorld.mother(p,s);
                var motherSeat=FirstEscapeScene.prop(p,s,"MotherSupperSeat");
                var fatherSeat=FirstEscapeScene.prop(p,s,"FatherSupperSeat");
                if(mother==null||father==null||motherSeat==null||fatherSeat==null
                    ||mother.getVehicle()!=motherSeat||father.getVehicle()!=fatherSeat)return;
                if(!n.contains("SupperSeated")){
                    n.putLong("SupperSeated",GhostVictimProgress.clock(n));
                }
                if(!n.getBoolean("SupperFoodReady")){
                    if(!GhostVictimManifestations.display(p,s,n,"supper_food",Vec3.atBottomCenterOf(table).add(0,1,0),
                        GhostVictimProgress.Disturbance.FOOD,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COOKED_CHICKEN)))return;
                    GhostVictimManifestations.show(p,s,n,"supper_food",true);n.putBoolean("SupperFoodReady",true);
                }
                long elapsed=GhostVictimProgress.clock(n)-n.getLong("SupperSeated");
                supperConversation(n,elapsed);
                if(n.contains("ReactionLook")&&GhostVictimProgress.clock(n)<n.getLong("ReactionLookUntil")){
                    var reaction=BlockPos.of(n.getLong("ReactionLook")).getCenter();
                    child.getLookControl().setLookAt(reaction.x,reaction.y,reaction.z,30,30);child.gesture(0,5);
                }else{
                    n.remove("ReactionLook");n.remove("ReactionLookUntil");
                    child.getLookControl().setLookAt(table.getX()+.5,table.getY()+1,table.getZ()+.5,25,25);child.gesture(3,5);
                }
                if(elapsed>=SUPPER_TICKS)finishSupper(p,s,n,child,true);
            }
            case EXPLORATION->{if(child==null)return;GhostVictimWorld.ensureHouseProps(p,s,n);GhostVictimRoutines.explore(p,s,n,child);
                if(GhostVictimProgress.age(n)>=GhostVictimConfig.EXPLORATION_SECONDS.get()*20L)move(p,s,n,GhostVictimProgress.Phase.BEDTIME);}
            case BEDTIME->{
                if(child==null)return;var target=GhostVictimWorld.bedroom(s);
                if(!n.contains("BedtimeArrived")){
                    if(GhostVictimRoutines.travel(p,s,n,child,target,false)!=GhostVictimNavigation.Result.ARRIVED)return;
                    n.putLong("BedtimeArrived",GhostVictimProgress.clock(n));words(n,"I'd better get to bed. Will you still be here tomorrow?");}
                var mirror=n.getCompound("Mirrors").getCompound("bedroom");var at=BlockPos.of(mirror.getLong("Position"));child.getLookControl().setLookAt(at.getX()+.5,at.getY()+1.35,at.getZ()+.5,30,30);
                var mother=AcceptedButtonEyesWorld.mother(p,s);
                // Give the mother a short chance to walk out naturally, then
                // start the player's full mirror window even if her route is
                // obstructed. A wedged parent must not freeze the whole night.
                if(!n.contains("BedtimeWindowStarted")&&((mother==null||mother.level()!=child.level()||mother.distanceToSqr(child)>64)
                        ||GhostVictimProgress.clock(n)-n.getLong("BedtimeArrived")>=600))n.putLong("BedtimeWindowStarted",GhostVictimProgress.clock(n));
                if(n.contains("BedtimeWindowStarted")&&GhostVictimProgress.clock(n)-n.getLong("BedtimeWindowStarted")>=GhostVictimConfig.BEDTIME_SECONDS.get()*20L&&!GhostVictimProgress.mirror(n)){
                    n.remove("BedtimeArrived");n.remove("BedtimeWindowStarted");GhostVictimMirrors.close(p);child.gesture(6,60);GhostVictimSchedule.tomorrow(n);move(p,s,n,GhostVictimProgress.Phase.NIGHT_PREPARATION);}
            }
            case NIGHT_PREPARATION->{
                if(child==null)return;
                GhostVictimActivities.prepareScouting(p,s,n);
                if(!n.getBoolean("ChildAtHome")){
                    if(GhostVictimProgress.age(n)<100){child.gesture(6,5);return;}
                    var state=level.getBlockState(realm.doorPos);if(!state.hasProperty(TinyDoorBlock.FACING))return;
                    var facing=state.getValue(TinyDoorBlock.FACING);var front=realm.doorPos.relative(facing,2);
                    if(child.getPose()!=Pose.SWIMMING){
                        if(GhostVictimRoutines.travel(p,s,n,child,front,false)!=GhostVictimNavigation.Result.ARRIVED)return;
                        child.gesture(6,100);child.setPose(Pose.SWIMMING);child.refreshDimensions();n.remove("ChildRoutine");}
                    child.gesture(6,5);var inside=BlockPos.containing(TunnelPassage.inside(realm.doorPos,facing,2.2));
                    if(GhostVictimRoutines.travel(p,s,n,child,inside,false)!=GhostVictimNavigation.Result.ARRIVED)return;
                    var real=DoorData.get(p.server).doors.get(s.door);if(real==null)return;
                    var home=com.coraline.parents.FamilyData.level(p.server,real.dimension);if(home==null)return;
                    var safe=OtherWorld.safePlayer(home,real.pos);if(safe==null||safe.distSqr(real.pos)>25)return;
                    var transferred=ShowTravel.guest(child,p,s,home,Vec3.atBottomCenterOf(safe),false,"VictimHomeVisit");
                    if(!(transferred instanceof TycoonNpc same))return;same.setInvisible(true);same.noPhysics=true;same.setNoAi(true);same.getNavigation().stop();
                    GhostVictimActors.remember(n,same);n.putBoolean("ChildAtHome",true);OtherMotherLoop.changed(p);
                }
                if(GhostVictimProgress.age(n)>=GhostVictimConfig.PREPARATION_SECONDS.get()*20L){GhostVictimActivities.finishScouting(p,s,n);GhostVictimSchedule.morning(n);n.remove("ChildRoutine");OtherMotherLoop.changed(p);}
            }
            case DAILY_ARRIVAL->{
                if(child==null)return;var state=level.getBlockState(realm.doorPos);if(!state.hasProperty(TinyDoorBlock.FACING))return;
                var facing=state.getValue(TinyDoorBlock.FACING);
                if(n.getBoolean("ChildAtHome")){
                    var at=TunnelPassage.inside(realm.doorPos,facing,1.2);var transferred=ShowTravel.guest(child,p,s,level,at,false,"VictimHomeVisit");
                    if(!(transferred instanceof TycoonNpc same))return;child=same;child.setInvisible(false);child.noPhysics=false;child.setNoAi(false);child.gesture(6,100);child.setPose(Pose.SWIMMING);child.refreshDimensions();
                    n.remove("ChildAtHome");GhostVictimActors.remember(n,child);OtherMotherLoop.changed(p);}
                child.gesture(6,5);if(GhostVictimRoutines.travel(p,s,n,child,realm.doorPos.relative(facing,2),false)!=GhostVictimNavigation.Result.ARRIVED)return;
                child.setPose(Pose.STANDING);child.gesture(0,0);child.refreshDimensions();words(n,"I'm back! What are we doing today?");move(p,s,n,GhostVictimProgress.Phase.DAILY_GIFT);
            }
            case DAILY_GIFT->{
                if(child==null)return;var gift=GhostVictimWorld.hall(s);
                String giftName=GhostVictimSchedule.today(n).getString("Gift"),point="gift_"+giftName;
                if(!n.getBoolean("GiftPlaced")){
                    if(GhostVictimRoutines.travel(p,s,n,child,gift.south(),false)!=GhostVictimNavigation.Result.ARRIVED)return;
                    if(!GhostVictimManifestations.display(p,s,n,point,Vec3.atBottomCenterOf(gift).add(0,1,0),GhostVictimProgress.Disturbance.GIFT_SECRET,gift(giftName)))return;
                    GhostVictimManifestations.show(p,s,n,point,true);n.putBoolean("GiftPlaced",true);n.putLong("GiftPresentedAt",GhostVictimProgress.clock(n));words(n,"You got this for me? Thank you!");}
                GhostVictimWorld.configureGiftSecret(n,giftName);child.getLookControl().setLookAt(gift.getX()+.5,gift.getY()+1,gift.getZ()+.5,30,30);
                if(GhostVictimProgress.clock(n)-n.getLong("GiftPresentedAt")>=GhostVictimConfig.GIFT_SECONDS.get()*20L){
                    GhostVictimManifestations.show(p,s,n,point,false);n.remove("GiftPlaced");n.remove("GiftPresentedAt");GhostVictimProgress.trust(n,GhostVictimProgress.motherTrust(n)+.08,GhostVictimProgress.ghostTrust(n));
                    move(p,s,n,GhostVictimProgress.Phase.BETWEEN_ACTIVITIES);}
            }
            case VICTIM_CAPTURED->{
                if(child==null)return;if(!AcceptedButtonEyesWorld.prepare(p,s))return;
                if(!n.contains("CaptureSeatedAt")){
                    var chair=AcceptedButtonEyesWorld.chair(s);
                    if(GhostVictimRoutines.travel(p,s,n,child,chair.south(),false)!=GhostVictimNavigation.Result.ARRIVED)return;
                    var seat=FirstEscapeScene.ensure(p,s,"VictimSewingSeat",CinematicPropEntity.SEAT,102,level,Vec3.atBottomCenterOf(chair));
                    if(seat==null||!child.startRiding(seat,true))return;n.putLong("CaptureSeatedAt",GhostVictimProgress.clock(n));
                    var sound=IncidentalDialogue.sound("Other Mother",AcceptedButtonEyes.INVITATION);if(sound!=null)ShowPlayback.start(p,s,sound,false);
                    words(n,"Wait... I don't want those!");}
                long age=GhostVictimProgress.clock(n)-n.getLong("CaptureSeatedAt");ShowPlayback.tick(p,s);
                var mother=AcceptedButtonEyesWorld.mother(p,s);if(mother!=null){mother.getLookControl().setLookAt(child,30,30);mother.familyGesture(81,5);}
                child.ritual((int)Math.max(0,Math.min(160,age-60)));
                var needle=FirstEscapeScene.ensure(p,s,"VictimNeedle",AcceptedButtonEyesWorld.NEEDLE,0,level,child.getEyePosition().add(.25,0,0));
                if(needle!=null){needle.setPos(child.getEyePosition().add(.25,Math.sin(age*.12)*.1,0));needle.grabTarget(new Vec3(-1,0,0));}
                if(age>=240){
                    var failed=n.getCompound(GhostEscapeProgress.KEY);
                    if(GhostEscapeProgress.owns(n,failed)&&GhostEscapeProgress.phase(failed)==GhostEscapeProgress.Phase.FAILED_SEWING&&!GhostEscapeEyes.forfeit(p,s))return;
                    child.stopRiding();if(needle!=null){needle.discard();FirstEscapeScene.context(s).remove("VictimNeedle");FirstEscapeScene.context(s).remove("VictimNeedlePosition");}n.remove("CaptureSeatedAt");GhostVictimProgress.archiveVictim(n,"CAPTURED");OtherMotherLoop.changed(p);
                }
            }
            case VICTIM_ESCAPED->{
                if(child==null)return;var state=level.getBlockState(realm.doorPos);if(!state.hasProperty(TinyDoorBlock.FACING))return;var facing=state.getValue(TinyDoorBlock.FACING);
                if(child.getPose()!=Pose.SWIMMING){if(GhostVictimRoutines.travel(p,s,n,child,realm.doorPos.relative(facing,2),true)!=GhostVictimNavigation.Result.ARRIVED)return;child.gesture(6,100);child.setPose(Pose.SWIMMING);child.refreshDimensions();n.remove("ChildRoutine");words(n,"I am going home. I'm not coming back!");}
                child.gesture(6,5);if(GhostVictimRoutines.travel(p,s,n,child,BlockPos.containing(TunnelPassage.inside(realm.doorPos,facing,2.2)),false)!=GhostVictimNavigation.Result.ARRIVED)return;
                child.setInvisible(true);child.noPhysics=true;GhostVictimProgress.archiveVictim(n,"ESCAPED");OtherMotherLoop.changed(p);
            }
            case BETWEEN_ACTIVITIES,ACTIVITY->GhostVictimActivities.tick(p,s,n);
            default->{}
        }
    }
    private static boolean arrivalReady(ServerPlayer p,VisitData.Scene s,net.minecraft.server.level.ServerLevel level,com.coraline.parents.ParentEntity mother){
        return s.owner.equals(p.getUUID())&&level!=null&&p.level()==level&&mother!=null&&mother.level()==level
            &&FirstEscapeScene.fullyInRoom(s,p.getBoundingBox())&&FirstEscapeScene.fullyInRoom(s,mother.getBoundingBox());
    }
    private static void familyWords(CompoundTag n,String text){
        n.putString("FamilyWords",text);n.putLong("FamilyWordsUntil",GhostVictimProgress.clock(n)+Math.max(100,text.length()*2));
    }
    private static void supperConversation(CompoundTag n,long elapsed){
        int beat=elapsed>=1480?5:elapsed>=1150?4:elapsed>=800?3:elapsed>=460?2:elapsed>=210?1:0;
        if(n.getInt("SupperDialogueBeat")>beat)return;
        n.putInt("SupperDialogueBeat",beat+1);
        switch(beat){
            case 0->familyWords(n,"Other Mother: I made your favorites. Sit close, darling. This is your home, too.");
            case 1->familyWords(n,"Other Father: Pass the bread, would you? It came out of the oven just before you arrived.");
            case 2->words(n,"You remembered what I like. Can I have some more?");
            case 3->familyWords(n,"Other Mother: As much as you like. You needn't wait for a special occasion here.");
            case 4->familyWords(n,"Other Father: We can show you the rest of the house after supper. There's a surprise in almost every room.");
            case 5->familyWords(n,"Other Mother: I want you to feel at home here. There's always a place for you at this table.");
        }
    }
    private static void finishSupper(ServerPlayer p,VisitData.Scene s,CompoundTag n,TycoonNpc child,boolean completed){
        if(child!=null)child.stopRiding();
        n.remove("SupperSeated");n.remove("SupperDialogueBeat");n.remove("SupperFoodReady");
        GhostVictimManifestations.show(p,s,n,"supper_food",false);
        if(completed)GhostVictimProgress.trust(n,GhostVictimProgress.motherTrust(n)+.06,GhostVictimProgress.ghostTrust(n));
        familyWords(n,"Other Mother: Supper is over, darling. Go explore the house until bedtime.");
        move(p,s,n,GhostVictimProgress.Phase.EXPLORATION);
    }
    public static void words(CompoundTag n,String text){n.putString("ChildWords",text);n.putLong("ChildTalkingUntil",GhostVictimProgress.clock(n)+Math.max(40,text.length()*2));}
    static net.minecraft.world.item.ItemStack gift(String id){return new net.minecraft.world.item.ItemStack(switch(id){
        case "toy"->com.coraline.tycoon.TycoonContent.BALL.get();case "music_box"->net.minecraft.world.item.Items.MUSIC_DISC_CAT;
        case "flower"->net.minecraft.world.item.Items.BLUE_ORCHID;case "doll"->com.coraline.registry.ModItems.DOLL.get();
        case "gadget"->net.minecraft.world.item.Items.CLOCK;case "magical_trinket"->net.minecraft.world.item.Items.AMETHYST_SHARD;
        case "personal_present"->net.minecraft.world.item.Items.WRITABLE_BOOK;default->net.minecraft.world.item.Items.CAKE;});}
    public static boolean choice(ServerPlayer p,UUID nonce,String value){var s=OtherMotherLoop.scene(p);if(!active(s))return false;var n=GhostVictimEntry.data(s);if(GhostEscapeProgress.bodyRestored(n))return false;var e=n.getCompound(GhostEscapeProgress.KEY);if(GhostEscapeProgress.phase(e)==GhostEscapeProgress.Phase.INTERCEPT)return p.getUUID().equals(s.owner)&&GhostEscapeWager.choice(p,s,n,nonce,value);return GhostVictimMirrors.choice(p,s,n,GhostVictimActors.find(p,s,n),nonce,value);}
    public static void request(ServerPlayer p,UUID nonce,String value){
        var s=OtherMotherLoop.scene(p);if(!active(s)||value==null||value.length()>100)return;var n=GhostVictimEntry.data(s);
        if(!n.hasUUID("Session")||!n.getUUID("Session").equals(nonce))return;
        if(GhostEscapeProgress.bodyRestored(n))return;
        if(!GhostVictimMirrors.participant(p,s,n))return;
        if(!p.getUUID().equals(s.owner)&&!(value.startsWith("inspect:")||value.startsWith("inspect_mirror:")||value.startsWith("mirror:")||value.startsWith("manifest:")||value.startsWith("manifest_fright:")||value.startsWith("escape_hide:")||value.equals("escape_go")||value.equals("escape_stop")))return;
        if(value.equals("prison_exit")){GhostVictimEntry.leave(p,s,nonce);return;}
        if(value.startsWith("inspect_mirror:")){GhostVictimMirrors.inspect(p,s,n,value.substring(15));return;}
        if(value.startsWith("inspect:")){GhostVictimManifestations.inspect(p,s,n,value.substring(8));return;}
        if(value.equals("follow_activity")){GhostVictimActivities.follow(p,s,n);return;}
        if(value.startsWith("scout:")){GhostVictimActivities.scout(p,s,n,value.substring(6));return;}
        if(value.equals("scout_return")&&ActivityWorld.isActivity(p.level())&&n.contains("ScoutActivity")){
            for(Tag entry:n.getList("ScoutSites",Tag.TAG_COMPOUND)){var row=(CompoundTag)entry;if(row.getString("Id").equals(n.getString("ScoutActivity"))&&p.distanceToSqr(GhostVictimActivities.entrance(BlockPos.of(row.getLong("Center")),row.getString("Id")).getCenter())<36){GhostVictimActivities.returnGhost(p,s,n);n.remove("ScoutActivity");return;}}
        }
        if(value.equals("return_house")&&ActivityWorld.isActivity(p.level())){var a=GhostVictimActivities.data(n);if(a.contains("Center")&&p.distanceToSqr(GhostVictimActivities.entrance(BlockPos.of(a.getLong("Center")),a.getString("Id")).getCenter())<36)GhostVictimActivities.returnGhost(p,s,n);return;}
        if(value.startsWith("escape_hide:")||value.equals("escape_door")){
            var e=n.getCompound(GhostEscapeProgress.KEY);var child=GhostVictimActors.find(p,s,n);
            if(child==null||!GhostEscapeProgress.owns(n,e)||GhostEscapeProgress.phase(e)!=GhostEscapeProgress.Phase.HIDDEN)return;
            String target=value.equals("escape_door")?"final_door":value.substring(12);BlockPos at;
            if(target.equals("final_door")){if(!e.contains("FinalDoor"))return;at=BlockPos.of(e.getLong("FinalDoor"));var path=child.getNavigation().createPath(at,0);if(path==null||!path.canReach())return;}
            else{var node=GhostEscapeHiding.node(e,target);if(!node.contains("Position")||target.equals(e.getString("HidingSpot"))||!GhostEscapeHiding.reachable(child,e,target))return;at=BlockPos.of(node.getLong("Position"));}
            if(!GhostEscapePerception.destinationSignalPoint(p,s,n,child,at))return;
            String before=e.getString("TargetHide");e.putString("TargetHide",target);
            if(!GhostEscapeProgress.signal(n,true,true,false)){if(before.isBlank())e.remove("TargetHide");else e.putString("TargetHide",before);return;}
            p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,at.getX()+.5,at.getY()+.5,at.getZ()+.5,5,.1,.1,.1,0);
            OtherMotherLoop.changed(p);return;
        }
        if(value.equals("escape_go")||value.equals("escape_stop")){
            var e=n.getCompound(GhostEscapeProgress.KEY);var child=GhostVictimActors.find(p,s,n);if(child==null||child.level()!=p.level())return;
            var point=child.blockPosition();if(!GhostEscapePerception.signalPoint(p,s,n,child,point))return;
            if(!GhostEscapeProgress.signal(n,!value.equals("escape_stop"),true,false))return;
            if(value.equals("escape_stop")){GhostEscapeCovert.hands(child);child.getNavigation().stop();child.setSprinting(false);child.gesture(0,0);n.remove("ChildRoutine");}
            OtherMotherLoop.changed(p);return;
        }
        if(value.startsWith("mirror:")){
            String id=value.substring(7);var child=GhostVictimActors.find(p,s,n);
            if(!GhostVictimMirrors.childPresent(p,n,id,child)){
                p.displayClientMessage(net.minecraft.network.chat.Component.literal("Wait until the child is near and looking toward this mirror."),true);
                return;
            }
            if(GhostVictimMirrors.enter(p,s,n,id))GhostVictimMirrors.open(p,s,n,child);
        }else if(value.startsWith("manifest:"))GhostVictimManifestations.perform(p,s,n,value.substring(9),GhostVictimActors.find(p,s,n));
        else if(value.startsWith("manifest_fright:"))GhostVictimManifestations.perform(p,s,n,value.substring(16),GhostVictimActors.find(p,s,n),true);
    }
    public static boolean parent(com.coraline.parents.ParentEntity actor,ServerPlayer p,VisitData.Scene s){
        if(!active(s))return false;var n=GhostVictimEntry.data(s);
        if(actor.isMom()&&actor.getUUID().equals(s.mom)&&actor.level()==AcceptedButtonEyesWorld.level(p)&&actor.tickCount%20==0){
            // This adapter returns before OtherMotherLoop.control, so that
            // controller cannot remember her last patrol chunk for us.
            s.loop.motherPos=actor.blockPosition();OtherMotherLoop.changed(p);
        }
        if(GhostEscapeProgress.bodyRestored(n)){
            // The wager is over. An old BEDTIME phase must not send the
            // defeated original Mother walking away during the epilogue.
            actor.getNavigation().stop();actor.setSprinting(false);return true;
        }
        var child=GhostVictimActors.find(p,s,n);actor.setInvisible(false);actor.eventGaze(true);actor.familyGesture(n.contains("CaptureSeatedAt")&&actor.isMom()?81:66,5);
        if(GhostEscapeMother.control(actor,p,s,n))return true;
        if(GhostVictimActivities.escort(actor,p,s,n))return true;
        if(GhostVictimProgress.phase(n)==GhostVictimProgress.Phase.BEDTIME&&n.contains("BedtimeArrived")){
            String key=actor.isMom()?"MotherBedtimeExit":"FatherBedtimeExit";var route=n.getCompound(key);n.put(key,route);
            var at=GhostVictimWorld.at(s,35,15,48);var result=GhostVictimNavigation.walk(actor,route,at,GhostVictimProgress.clock(n),1,600,door->GhostVictimWorld.ordinaryDoor(s,door));
            if(result==GhostVictimNavigation.Result.BLOCKED&&GhostVictimProgress.clock(n)%100==0)GhostVictimNavigation.stop(actor,route);
            actor.familyGesture(66,5);return true;
        }
        String supperKey=actor.isMom()?"MotherSupperSeat":"FatherSupperSeat";
        var supperSeat=FirstEscapeScene.prop(p,s,supperKey);
        if(GhostVictimProgress.phase(n)!=GhostVictimProgress.Phase.FIRST_SUPPER&&supperSeat!=null&&actor.getVehicle()==supperSeat)actor.stopRiding();
        var arrival=GhostVictimProgress.phase(n);
        if(actor.isMom()&&(arrival==GhostVictimProgress.Phase.WAITING_FOR_VICTIM||arrival==GhostVictimProgress.Phase.DAILY_ARRIVAL)){
            var realm=AcceptedButtonEyesWorld.realm(p,s);var home=AcceptedButtonEyesWorld.level(p);
            if(realm!=null&&realm.doorPos!=null&&actor.level()==home){
                var state=home.getBlockState(realm.doorPos);
                if(state.hasProperty(TinyDoorBlock.FACING)){
                    var facing=state.getValue(TinyDoorBlock.FACING);var side=facing.getClockWise();
                    var preferred=realm.doorPos.relative(facing,3).relative(side,2);var at=GhostVictimNavigation.approach(actor,preferred);
                    if(at==null||!FirstEscapeScene.inRoom(s,at))at=GhostVictimNavigation.approach(actor,FirstEscapeScene.room(s));
                    if(at!=null&&FirstEscapeScene.inRoom(s,at)){
                        var route=n.getCompound("MotherArrivalRoute");
                        if(!route.getString("Phase").equals(arrival.name())||route.getInt("Day")!=n.getInt("Day")){route=new CompoundTag();route.putString("Phase",arrival.name());route.putInt("Day",n.getInt("Day"));n.put("MotherArrivalRoute",route);}
                        var result=GhostVictimNavigation.walk(actor,route,at,GhostVictimProgress.clock(n),.95,600,door->GhostVictimWorld.ordinaryDoor(s,door));
                        if(result==GhostVictimNavigation.Result.BLOCKED&&GhostVictimProgress.clock(n)%100==0)GhostVictimNavigation.stop(actor,route);
                    }else actor.getNavigation().stop();
                    if(child!=null&&child.level()==actor.level())actor.getLookControl().setLookAt(child,30,30);
                    else{var door=realm.doorPos.getCenter();actor.getLookControl().setLookAt(door.x,door.y,door.z,30,30);}
                    return true;
                }
            }
        }else if(actor.isMom())n.remove("MotherArrivalRoute");
        if(GhostVictimProgress.phase(n)==GhostVictimProgress.Phase.FIRST_SUPPER&&child!=null&&actor.level()==child.level()){
            var table=GhostVictimWorld.diningTable(p,s);var at=actor.isMom()?table.west(2):table.east(2);
            actor.getLookControl().setLookAt(child,25,25);
            if(actor.isPassenger()&&actor.getVehicle()!=supperSeat)actor.stopRiding();
            if(actor.getVehicle()!=supperSeat||supperSeat==null){
                var route=n.getCompound(supperKey+"Route");n.put(supperKey+"Route",route);
                if(GhostVictimNavigation.walk(actor,route,at,GhostVictimProgress.clock(n),.95,600,door->GhostVictimWorld.ordinaryDoor(s,door))==GhostVictimNavigation.Result.ARRIVED){
                    var seat=FirstEscapeScene.ensure(p,s,supperKey,CinematicPropEntity.SEAT,actor.isMom()?103:104,(net.minecraft.server.level.ServerLevel)actor.level(),Vec3.atBottomCenterOf(at));
                    if(seat!=null){seat.setYRot(actor.isMom()?270:90);actor.startRiding(seat,true);}
                }
            }else{actor.getNavigation().stop();actor.familyGesture(3,5);}
            return true;
        }
        if(actor.isMom()&&n.contains("MotherInvestigation")&&GhostVictimProgress.clock(n)<n.getLong("MotherInvestigateUntil")){
            var at=BlockPos.of(n.getLong("MotherInvestigation"));actor.getLookControl().setLookAt(at.getX()+.5,at.getY()+1,at.getZ()+.5,25,25);
            var route=n.getCompound("MotherInvestigationRoute");n.put("MotherInvestigationRoute",route);
            GhostVictimNavigation.walk(actor,route,at,GhostVictimProgress.clock(n),.95,200,door->GhostVictimWorld.ordinaryDoor(s,door));return true;
        }
        if(actor.isMom())n.remove("MotherInvestigationRoute");
        if(actor.isMom()&&arrival==GhostVictimProgress.Phase.EXPLORATION&&child!=null&&actor.level()==child.level()){
            var route=n.getCompound("MotherExploreRoute");n.put("MotherExploreRoute",route);
            var stops=new BlockPos[]{GhostVictimWorld.kitchen(s),GhostVictimWorld.hall(s),GhostVictimWorld.diningTable(p,s).north(2),GhostVictimWorld.bedroom(s).south(2)};
            long clock=GhostVictimProgress.clock(n);
            if(clock<route.getLong("PauseUntil")){actor.getNavigation().stop();return true;}
            var result=GhostVictimNavigation.walk(actor,route,stops[Math.floorMod(route.getInt("Index"),stops.length)],clock,.85,400,door->GhostVictimWorld.ordinaryDoor(s,door));
            if(result!=GhostVictimNavigation.Result.MOVING){
                GhostVictimNavigation.stop(actor,route);route.putInt("Index",route.getInt("Index")+1);route.putLong("PauseUntil",clock+100);
            }
            return true;
        }
        if(actor.isMom())n.remove("MotherExploreRoute");
        if(child!=null&&child.level()==actor.level()){actor.getLookControl().setLookAt(child,25,25);if(actor.distanceToSqr(child)>16){var route=n.getCompound(actor.isMom()?"MotherRoute":"FatherRoute");n.put(actor.isMom()?"MotherRoute":"FatherRoute",route);GhostVictimNavigation.walk(actor,route,child.blockPosition().east(actor.isMom()?1:-1),GhostVictimProgress.clock(n),.95,400,door->GhostVictimWorld.ordinaryDoor(s,door));}else actor.getNavigation().stop();}
        if(child==null||child.level()!=actor.level())actor.getNavigation().stop();return true;
    }
    private GhostVictimDirector(){}
}
