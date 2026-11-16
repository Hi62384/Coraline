package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.parents.ParentEntity;
import com.coraline.parents.ParentAccess;
import com.coraline.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
import static com.coraline.feast.OtherMotherLoopState.Phase.*;

/** Repeat-visit orchestration. Existing audio pools/transport remain the only voice pipeline. */
@Mod.EventBusSubscriber(modid="coraline")
public final class OtherMotherLoop {
    public static final int ATTENDANCE_TICKS=5*60*20; // Production relaxation period.
    private static final Map<UUID,OtherMotherLoopState.Phase> listening=new HashMap<>();
    private static final Map<UUID,ServerBossEvent> bars=new HashMap<>();
    private OtherMotherLoop(){}
    public static VisitData.Scene scene(ServerPlayer p){return com.coraline.multiplayer.StorySessionData.get(p.server).scene(p);}
    public static VisitData.Scene active(ServerPlayer p){
        if(com.coraline.multiplayer.StoryCoop.guest(p)&&com.coraline.multiplayer.StorySessionData.get(p.server).admittedOwner(p.getUUID()).isEmpty())return null;
        var s=scene(p);var n=p.getPersistentData();
        return s!=null&&s.stage==VisitData.Stage.DONE&&s.loop.active()&&n.hasUUID("CoralineVisitDoor")&&s.door.equals(n.getUUID("CoralineVisitDoor"))
            &&(OtherWorld.isOther(p.level())||ActivityWorld.isActivity(p.level()))?s:null;
    }
    public static boolean locked(ServerPlayer p){var s=active(p);if(s!=null&&VisitEvents.type(s)==VisitEvents.Type.HOUSE_PARTY&&s.loop.phase.name().startsWith("PARTY_")&&!s.loop.secrets.searching())return false;return s!=null&&!s.loop.canLeave();}
    public static boolean portalOnly(ServerPlayer p){var s=active(p);return s!=null&&(s.loop.phase==ARRIVAL||s.loop.phase==READY);}
    public static void changed(ServerPlayer p){VisitData.get(p.server).setDirty();com.coraline.multiplayer.StorySessionData.get(p.server).touch(p.server);}
    public static long now(ServerPlayer p){return p.server.overworld().getGameTime();}
    static void phase(ServerPlayer p,VisitData.Scene s,OtherMotherLoopState.Phase phase){
        s.loop.phase=phase;s.loop.deadline=0;listening.remove(p.getUUID());changed(p);
    }
    public static void relationship(ServerPlayer p,float trust,float suspicion){
        var s=scene(p);if(s==null)return;s.loop.trust=OtherMotherLoopState.clamp(trust);s.loop.suspicion=OtherMotherLoopState.clamp(suspicion);
        if(!AcceptedButtonEyes.active(s)&&(BlackIsTraditional.threshold(s.loop.trust)||BlackIsTraditional.threshold(s.loop.suspicion))&&!BlackIsTraditional.data(s).getBoolean("Started"))s.loop.story.putBoolean("BlackIsTraditionalQueued",true);
        s.welcomeState=AcceptedButtonEyes.active(s)?AcceptedButtonEyes.dialogueState(s):OtherMotherLoopState.classify(s.loop.trust,s.loop.suspicion);changed(p);
    }
    /** Award only at the completed inbound tunnel crossing, including the first visit. */
    public static void rewardEntry(ServerPlayer p,RealmData.Realm realm){
        var tag=p.getPersistentData();
        if(!OtherWorld.isOther(p.level())||tag.getBoolean("CoralineInTunnel")||tag.getBoolean("CoralineReturning"))return;
        if(!tag.hasUUID("CoralineEntryTrustDoor")||!realm.door.equals(tag.getUUID("CoralineEntryTrustDoor")))tag.putInt("CoralineEntryTrustPoints",0);
        tag.putUUID("CoralineEntryTrustDoor",realm.door);
        tag.putInt("CoralineEntryTrustPoints",Math.min(100,tag.getInt("CoralineEntryTrustPoints")+8));
        applyEntryTrust(p);
    }
    /** Scene creation can finish after arrival; consume a saved bonus exactly once. */
    public static void applyEntryTrust(ServerPlayer p){
        var tag=p.getPersistentData();var s=scene(p);
        if(s==null||!tag.hasUUID("CoralineEntryTrustDoor")||!s.door.equals(tag.getUUID("CoralineEntryTrustDoor")))return;
        int points=tag.getInt("CoralineEntryTrustPoints");
        tag.remove("CoralineEntryTrustDoor");tag.remove("CoralineEntryTrustPoints");
        if(points>0)relationship(p,s.loop.trust+points/100F,s.loop.suspicion);
    }
    /** Called during tunnel staging, after the copy has spawned this generation of parents. */
    public static boolean prepare(ServerPlayer p,RealmData.Realm realm){
        var s=scene(p);if(s==null||s.stage!=VisitData.Stage.DONE)return true;
        if(s.loop.event.getBoolean("ShowCleanupDeferred"))return false;
        if(MotherGoesOut.pending(s))return MotherGoesOut.preserveActor(p,s);
        p.serverLevel().getChunkAt(realm.bed==null?realm.doorPos:realm.bed);
        var parents=p.serverLevel().getEntitiesOfClass(ParentEntity.class,new net.minecraft.world.phys.AABB(realm.doorPos).inflate(realm.radius+16),e->e.isOtherParent()&&e.getPersistentData().hasUUID("RealmDoor")&&realm.door.equals(e.getPersistentData().getUUID("RealmDoor")));
        // Old saves and interrupted staging may contain many copies. Keep one
        // current-generation mother, preferring the scene's recorded actor.
        var moms=parents.stream().filter(ParentEntity::isMom).toList();
        var mom=moms.stream().filter(e->e.isAlive()&&e.getPersistentData().getInt("RealmGeneration")==realm.parentGeneration&&e.getUUID().equals(s.mom)).findFirst().orElse(null);
        if(mom==null)mom=moms.stream().filter(e->e.isAlive()&&e.getPersistentData().getInt("RealmGeneration")==realm.parentGeneration).findFirst().orElse(null);
        for(var duplicate:moms)if(duplicate!=mom)duplicate.discard();
        if(mom==null){
            // Old/debug-completed saves can retain a ready estate but no loaded
            // counterpart. Never start a locked dialogue loop without its actor.
            var family=ParentAccess.family(p.server,p.getUUID());
            mom=ModEntities.MOM.get().create(p.serverLevel());if(mom==null)return false;
            mom.makeOtherParent(family==null?0:Math.max(0,family.momSkin));
            if(family!=null)mom.getPersistentData().putUUID("OriginalFamily",family.id);
            mom.getPersistentData().putUUID("RealmDoor",realm.door);
            mom.getPersistentData().putInt("RealmGeneration",realm.parentGeneration);
            mom.getPersistentData().putLong("OtherHome",realm.doorPos.east(4).asLong());
            var at=OtherWorld.safePlayer(p.serverLevel(),realm.doorPos.east(4));
            mom.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);
            if(!p.serverLevel().addFreshEntity(mom))return false;
        }
        s.mom=mom.getUUID();s.dad=parents.stream().filter(e->!e.isMom()).map(ParentEntity::getUUID).findFirst().orElse(BlackIsTraditional.active(s)?s.dad:null);
        {
            mom.getPersistentData().putUUID("LoopOwner",p.getUUID());ScenePlacement.prepareWalker(mom);
            var spot=OtherWorld.safePlayer(p.serverLevel(),realm.doorPos.offset(2,0,1));mom.teleportTo(spot.getX()+.5,spot.getY(),spot.getZ()+.5);
        }
        s.loop.motherPos=mom.blockPosition();
        if(!VisitEvents.keepFather(s))removeFather(p,s);
        else if(s.dad!=null&&p.serverLevel().getEntity(s.dad) instanceof ParentEntity dad){dad.getPersistentData().putUUID("VisitEventOwner",p.getUUID());dad.eventGaze(true);}
        changed(p);return true;
    }
    /** Actual entrance only: staging must never count as another visit. */
    public static boolean entered(ServerPlayer p,RealmData.Realm realm){
        var s=scene(p);if(s==null||!s.door.equals(realm.door))return false;
        if(AcceptedButtonEyes.active(s)){AcceptedButtonEyes.data(s).putBoolean("Recover",true);changed(p);return true;}
        if(s.stage!=VisitData.Stage.DONE){s.loop.visits++;changed(p);return false;}
        boolean resumingStory=BlackIsTraditional.restoreOnEntry(p,s);
        if(!prepare(p,realm))return false;
        s.loop.visits++;changed(p);
        if(resumingStory){BlackIsTraditional.recover(p);return true;}
        if(!s.loop.active()){
            s.loop.generation++;s.loop.loot=VisitGiftPools.choose(p,s);
            s.loop.activityLoot=s.loop.loot.expanded()?(p.getRandom().nextBoolean()?OtherMotherDialogue.Loot.FOOD:OtherMotherDialogue.Loot.TREASURE):s.loop.loot;
            s.loop.giftPrepared=false;s.loop.giftRevealUntil=0;
            s.loop.giftBefore=OtherMotherDialogue.choose(OtherMotherDialogue.lootPool(s.loop.loot,OtherMotherDialogue.Moment.BEFORE),s.lastDialogue.getOrDefault(OtherMotherDialogue.lootPool(s.loop.loot,OtherMotherDialogue.Moment.BEFORE).path(),""),p.getRandom());
            s.loop.giftAfter=OtherMotherDialogue.choose(OtherMotherDialogue.lootPool(s.loop.loot,OtherMotherDialogue.Moment.AFTER),s.lastDialogue.getOrDefault(OtherMotherDialogue.lootPool(s.loop.loot,OtherMotherDialogue.Moment.AFTER).path(),""),p.getRandom());
            s.loop.extra=p.getRandom().nextBoolean();s.loop.opened=false;s.loop.gifts.clear();
            s.loop.secrets.dialogue.putBoolean("OpeningActivityWait",true);
            s.welcomeAfter=0;phase(p,s,WELCOME);
            p.sendSystemMessage(Component.literal("Your next visit has begun. Find your Other Mother."));
        }
        VisitGuide.instructions(p);return true;
    }
    private static ParentEntity mother(ServerPlayer p,VisitData.Scene s){return s.mom!=null&&p.serverLevel().getEntity(s.mom) instanceof ParentEntity m?m:null;}
    private static void pruneDuplicateMothers(ServerPlayer p,VisitData.Scene s,RealmData.Realm realm){
        var current=mother(p,s);if(current==null)return;
        for(var candidate:p.serverLevel().getEntitiesOfClass(ParentEntity.class,
            new net.minecraft.world.phys.AABB(realm.doorPos).inflate(realm.radius+16),e->
                e!=current&&e.isOtherParent()&&e.isMom()&&e.getPersistentData().hasUUID("RealmDoor")&&
                realm.door.equals(e.getPersistentData().getUUID("RealmDoor"))))candidate.discard();
    }
    private static void removeFather(ServerPlayer p,VisitData.Scene s){
        if(s.dad!=null&&p.serverLevel().getEntity(s.dad) instanceof ParentEntity dad&&dad.isOtherParent()&&!dad.isMom()){dad.stopRiding();dad.discard();}
        s.dad=null;
    }
    public static boolean retiredFather(ParentEntity parent){
        if(parent.isMom()||!parent.isOtherParent()||!parent.getPersistentData().hasUUID("RealmDoor"))return false;
        var d=DoorData.get(parent.getServer()).doors.get(parent.getPersistentData().getUUID("RealmDoor"));
        var s=d==null?null:VisitData.get(parent.getServer()).scenes.get(d.owner);
        return s!=null&&s.stage==VisitData.Stage.DONE&&s.door.equals(d.id);
    }
    private static int speak(ServerPlayer p,VisitData.Scene s){
        var l=s.loop;
        if(l.phase==WELCOME){
            if(now(p)<s.welcomeAfter)return 0;
            var sound=OtherMotherWelcome.choose(s.welcomeState,s.lastWelcome,p.getRandom());
            VisitAudio.play(p,sound,false);s.lastWelcome=sound;s.welcomeAfter=now(p)+VisitAudio.length(sound);return VisitAudio.length(sound);
        }
        OtherMotherDialogue.Pool pool=switch(l.phase){
            case BEFORE->OtherMotherDialogue.lootPool(l.loot,OtherMotherDialogue.Moment.BEFORE);
            case AFTER->OtherMotherDialogue.lootPool(l.loot,OtherMotherDialogue.Moment.AFTER);
            case EXTRA->OtherMotherDialogue.extraPool(s.welcomeState);
            case TUTORIAL->OtherMotherTutorial.POOL;
            case PROPOSAL->l.activity.pool();
            case WARNING->OtherMotherActivities.waitPool(s.welcomeState);
            case ARRIVAL->OtherMotherActivities.arrivalPool(s.welcomeState,l.late?OtherMotherActivities.Arrival.LATE:OtherMotherActivities.Arrival.ON_TIME);
            case COMPLETE->OtherMotherActivityComplete.pool(s.welcomeState);
            default->null;
        };
        if(l.phase==BEFORE||l.phase==AFTER){
            String selected=l.phase==BEFORE?l.giftBefore:l.giftAfter;
            if(selected.isEmpty()||!validGiftVoice(pool,selected)){selected=OtherMotherDialogue.choose(pool,s.lastDialogue.getOrDefault(pool.path(),""),p.getRandom());if(l.phase==BEFORE)l.giftBefore=selected;else l.giftAfter=selected;changed(p);}
            return OtherMotherDialogue.playSelected(p,pool,selected);
        }
        return pool==null?0:OtherMotherDialogue.playActivityArrival(p,pool);
    }
    private static boolean validGiftVoice(OtherMotherDialogue.Pool pool,String sound){for(int i=0;i<pool.size();i++)if(pool.sound(i).equals(sound))return true;return false;}
    static void nextActivity(ServerPlayer p,VisitData.Scene s){
        if(!VisitEvents.cleanup(p,s))return;s.loop.event.putString("Type",VisitEvents.Type.ACTIVITY.name());
        if(AcceptedButtonEyes.active(s)){s.loop.activity=AcceptedButtonEyes.nextActivity(s);s.loop.generation++;phase(p,s,PROPOSAL);return;}
        var previous=s.loop.activity;var next=OtherMotherActivities.chooseActivity(p.getRandom());for(int i=0;i<32&&next==previous;i++)next=OtherMotherActivities.chooseActivity(p.getRandom());s.loop.activity=next;phase(p,s,PROPOSAL);
    }
    public static void clearActivityWindow(ServerPlayer p){var s=scene(p);if(s==null)return;VisitEvents.suspend(p,s);LoopProps.removePortals(p.server,s);removeBar(p.getUUID());s.loop.arrivalDeadline=0;phase(p,s,EXPLORING);}
    public static boolean afterReconciliation(ServerPlayer p){if(!com.coraline.secrets.Reconciliation.permitted(p)||scene(p).loop.suspicion>=1||com.coraline.secrets.SecretDialogue.busy(p))return false;var s=scene(p);s.loop.secrets.dialogue.remove("OpeningActivityWait");clearActivityWindow(p);s.loop.secrets.search=com.coraline.secrets.SecretsState.Search.NORMAL;s.loop.secrets.pendingTicks=0;s.loop.secrets.searchTicks=0;if(VisitEvents.resumeNumberedIntro(p)||VisitEvents.resumeDinner(p)||FamilyOuting.resume(p)||FamilyGameNight.resume(p)||SpecialDelivery.resume(p)||HouseParty.resume(p)||MotherGoesOut.resume(p)||WybieAdventure.resume(p)||TheatreVisit.resume(p)||MouseCircusVisit.resume(p))return true;s.loop.secrets.dialogue.putBoolean("ReconciledProposal",true);s.welcomeState=OtherMotherLoopState.classify(s.loop.trust,s.loop.suspicion);nextActivity(p,s);return true;}
    private static void advance(ServerPlayer p,VisitData.Scene s){
        switch(s.loop.phase){
            case WELCOME->phase(p,s,BEFORE);
            case BEFORE->{if(!LoopProps.prepareGifts(p,s)){fail(p,"There isn't a safe gift-table location. Returning home; try again on another night.");return;}phase(p,s,GIFTS);}
            case AFTER->phase(p,s,s.loop.extra?EXTRA:TUTORIAL);
            case EXTRA->phase(p,s,TUTORIAL);
            case TUTORIAL->VisitEvents.next(p,s,false);
            case COMPLETE->{if(!AcceptedButtonEyes.afterReaction(p,s))VisitEvents.next(p,s,false);}
            case PROPOSAL->{
                phase(p,s,WARNING);
                if(!s.loop.secrets.dialogue.getBoolean("OpeningActivityWait"))advance(p,s);
            }
            case WARNING->{
                s.loop.secrets.dialogue.remove("OpeningActivityWait");
                if(!LoopProps.choosePortal(p,s)){fail(p,"No safe activity portal location was found. Returning you home safely.");return;}
                phase(p,s,WINDOW);s.loop.arrivalDeadline=AcceptedButtonEyes.active(s)?0:now(p)+ATTENDANCE_TICKS;s.loop.secrets.world.remove("LateSearchScheduled");changed(p);
                s.loop.secrets.dialogue.remove("ReconciledProposal");
            }
            case ARRIVAL->{phase(p,s,READY);p.displayClientMessage(Component.literal("Enter the activity portal."),true);}
            default->{}
        }
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){if(event.player instanceof net.minecraft.server.level.ServerPlayer coopPlayer&&com.coraline.multiplayer.StoryCoop.directorBlocked(coopPlayer))return;
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer p))return;
        if(com.coraline.postgame.TrueHeartBattle.recoveryPending(p))return;
        var ending=scene(p);if(GhostEscapeEnding.epilogueActive(ending)){GhostEscapeEnding.tick(p,ending,GhostVictimEntry.data(ending));removeBar(p.getUUID());return;}
        if(AcceptedButtonEyes.tick(p,scene(p))){removeBar(p.getUUID());return;}
        if(FirstEscapeStory.tick(p,scene(p))){removeBar(p.getUUID());return;}
        if(p.tickCount%20==0)MotherGoesOut.recoverDoors(p,scene(p));
        var s=active(p);if(s==null){
            removeBar(p.getUUID());if(ActivityWorld.isActivity(p.level())){OtherWorld.leave(p,false);return;}
            // Migrate a repeat visit already open under the old welcome-only behavior.
            var prior=scene(p);var tag=p.getPersistentData();
            if(prior!=null&&prior.stage==VisitData.Stage.DONE&&OtherWorld.isOther(p.level())&&!tag.getBoolean("CoralineInTunnel")&&tag.hasUUID("CoralineVisitDoor")&&prior.door.equals(tag.getUUID("CoralineVisitDoor"))&&!tag.getBoolean("CoralineDebugVisitCompleted")){
                var r=RealmData.get(p.server).realms.get(prior.door);if(r!=null&&prepare(p,r))entered(p,r);
            }
            return;
        }
        if(!p.isAlive()){depart(p,false);return;}
        if(com.coraline.item.SkipEventDebugItem.retry(p))return;
        if(com.coraline.secrets.CatEncounter.active(p)){removeBar(p.getUUID());return;}
        var realm=RealmData.get(p.server).realms.get(s.door);var door=DoorData.get(p.server).doors.get(s.door);
        if(realm==null||door==null||door.revoked){fail(p,"Your Other World visit has ended.");return;}
        if(ActivityWorld.isActivity(p.level())){if(VisitEvents.venueEvent(s)){if(!s.loop.event.getBoolean("ShowCleanupDeferred")){HouseParty.backgroundTick(p,s);if(!com.coraline.secrets.SecretDialogue.busy(p))VisitEvents.tick(p,s);}else fail(p,"This event is waiting for a safe return. You have returned home.");}else ActivityWorld.tick(p,s);return;}
        if(p.getPersistentData().getBoolean("CoralineInTunnel")){removeBar(p.getUUID());return;}
        DeliveryRoute.prewarm(p,s);
        if(!AcceptedButtonEyes.active(s)&&BlackIsTraditional.pending(s)&&BlackIsTraditional.threshold(s.loop.suspicion)&&!BlackIsTraditional.active(s)){if(VisitEvents.cleanup(p,s))BlackIsTraditional.begin(p,s);else{removeBar(p.getUUID());return;}}
        if(BlackIsTraditional.tick(p,s)){removeBar(p.getUUID());return;}
        // Repair visits saved in an already-locked phase with no loaded mother.
        // If recovery genuinely fails, release the player instead of trapping them.
        if(p.tickCount%20==0&&mother(p,s)==null&&!prepare(p,realm)){
            fail(p,"Your Other Mother could not be restored. You have returned home safely.");return;
        }
        if(p.tickCount%100==0)pruneDuplicateMothers(p,s,realm);
        if(s.loop.phase==ACTIVITY){phase(p,s,COMPLETE);} // Safe recovery from interrupted dimension transfer.
        if(p.tickCount%20==0){
            if(!s.loop.motherPos.equals(BlockPos.ZERO))LoopProps.hold(p.serverLevel(),s.loop.motherPos);
            var m=mother(p,s);if(m!=null){m.getPersistentData().putUUID("LoopOwner",p.getUUID());m.relationship(s.loop.trust,s.loop.suspicion);}
            LoopProps.ensure(p,s);changed(p);
        }
        var l=s.loop;
        if(l.phase==WARNING&&!l.secrets.dialogue.getBoolean("OpeningActivityWait")){
            advance(p,s);return;
        }
        if(!s.loop.event.getBoolean("ShowCleanupDeferred")){HouseParty.backgroundTick(p,s);MotherGoesOut.backgroundTick(p,s);WybieAdventure.backgroundTick(p,s);}
        if(com.coraline.secrets.MotherInterrupts.check(p))return;
        if(l.secrets.dialogue.getBoolean("ReconciledProposal")&&(l.suspicion>=1||l.secrets.search==com.coraline.secrets.SecretsState.Search.SCRIPTED_TRAP)){
            l.secrets.dialogue.remove("ReconciledProposal");VisitAudio.stopVoice(p);
            if(l.phase==PROPOSAL||l.phase==WARNING)phase(p,s,EXPLORING);
            return;
        }
        if(com.coraline.secrets.SecretsRuntime.active(p)&&com.coraline.secrets.SecretDialogue.busy(p))return;
        if(VisitEvents.tick(p,s))return;
        if(l.phase==EXPLORING){removeBar(p.getUUID());return;}
        if(l.phase==GIFTS){LoopProps.finishGiftReveal(p,s);if(l.collected())phase(p,s,AFTER);else if(p.tickCount%40==0)VisitGuide.trail(p,s);return;}
        if(l.phase==WINDOW){
            if(p.isSleeping())return;
            if(AcceptedButtonEyes.active(s)){
                l.arrivalDeadline=0;l.late=false;removeBar(p.getUUID());
                if(p.tickCount%40==0)VisitGuide.trail(p,s);
                if(p.distanceToSqr(Vec3.atBottomCenterOf(l.portal))<=16){com.coraline.secrets.MotherSearch.attended(p);phase(p,s,ARRIVAL);}
                return;
            }
            long remaining=Math.max(0,l.arrivalDeadline-now(p));
            if(remaining==0&&AcceptedButtonEyes.active(s)){LoopProps.removePortals(p.server,s);nextActivity(p,s);return;}
            if(remaining==0)com.coraline.secrets.MotherSearch.attendanceExpired(p);
            var bar=bars.computeIfAbsent(p.getUUID(),id->new ServerBossEvent(Component.empty(),BossEvent.BossBarColor.BLUE,BossEvent.BossBarOverlay.PROGRESS));bar.addPlayer(p);
            bar.setName(Component.literal(activityName(l.activity)+" — "+String.format(Locale.ROOT,"%d:%02d",(remaining+19)/1200,((remaining+19)/20)%60)));
            bar.setProgress(Math.min(1,remaining/(float)ATTENDANCE_TICKS));
            if(p.tickCount%40==0)VisitGuide.trail(p,s);
            if(p.distanceToSqr(Vec3.atBottomCenterOf(l.portal))<=16){com.coraline.secrets.MotherSearch.attended(p);l.late=now(p)>=l.arrivalDeadline-2400;relationship(p,l.trust+(l.late?.015F:.035F),l.suspicion+(l.late?.01F:0));removeBar(p.getUUID());phase(p,s,ARRIVAL);}
            return;
        }
        if(l.phase==ARRIVAL||l.phase==READY){
            // Arrival dialogue must not tether or repeatedly teleport the player.
            if(l.phase==READY){if(p.distanceToSqr(Vec3.atBottomCenterOf(l.portal))<1.2)ActivityWorld.enter(p,s);return;}
        }
        if(listening.get(p.getUUID())!=l.phase){
            int ticks=speak(p,s);if(ticks>0){l.deadline=now(p)+ticks;listening.put(p.getUUID(),l.phase);changed(p);}return;
        }
        if(now(p)>=l.deadline&&(!l.secrets.dialogue.getBoolean("ReconciledProposal")||l.secrets.dialogue.getBoolean("LoopFinished")||now(p)>=l.deadline+20))advance(p,s);
    }
    public static String activityName(OtherMotherActivities.Activity activity){return activity.id().replace('_',' ');}
    public static boolean control(ParentEntity m){
        if(com.coraline.secrets.CatEncounter.holdMother(m))return true;
        if(!m.isMom()||!m.getPersistentData().hasUUID("LoopOwner"))return false;
        var owner=m.getPersistentData().getUUID("LoopOwner");var s=VisitData.get(m.getServer()).scenes.get(owner);
        if(s==null){m.getPersistentData().remove("LoopOwner");m.relationship(0,0);return false;}
        MotherGoesOut.recoverActor(m,s);m.relationship(s.loop.trust,s.loop.suspicion);
        var p=m.getServer().getPlayerList().getPlayer(owner);
        if(p==null||p.level()!=m.level()||p.getPersistentData().getBoolean("CoralineInTunnel")){m.getNavigation().stop();return true;}
        if(!s.loop.active()){m.getPersistentData().remove("LoopOwner");m.relationship(s.loop.trust,s.loop.suspicion);return false;}
        if(m.tickCount%20==0){s.loop.motherPos=m.blockPosition();VisitData.get(m.getServer()).setDirty();}
        if(ParentRescueStory.parentControl(m))return true;
        if(BlackIsTraditional.parentControl(m,p,s))return true;
        if(MotherGoesOut.control(m,p,s))return true;
        if(com.coraline.secrets.MotherSearch.control(m,p))return true;
        if(VisitEvents.motherControl(m,p,s))return true;
        if(s.loop.phase==EXPLORING){if(m.tickCount%40==0)m.getNavigation().stop();return true;}
        boolean wait=s.loop.phase==WINDOW||s.loop.phase==ARRIVAL||s.loop.phase==READY;
        boolean presenting=s.loop.phase==GIFTS&&s.loop.loot.expanded();
        Vec3 target=wait?portalWaitingSpot((ServerLevel)m.level(),s.loop.portal):presenting?portalWaitingSpot((ServerLevel)m.level(),s.table):p.position();
        m.setNoAi(false);m.setNoGravity(false);
        if(m.tickCount%5==0){
            ScenePlacement.prepareWalker(m);
            if(m.distanceToSqr(target)>((wait||presenting)?1.44:4)){
                var path=m.getNavigation().getTargetPos();
                // Keep a useful path running instead of rebuilding it twice a second.
                if(m.getNavigation().isDone()||path==null||path.distSqr(BlockPos.containing(target))>4)
                    m.getNavigation().moveTo(target.x,target.y,target.z,1.35);
            }else m.getNavigation().stop();
        }
        // Recover from obstructed player builds without excavating them or moving the player.
        if(m.tickCount%400==0&&m.distanceToSqr(target)>(wait?4:36)){
            if(wait)m.teleportTo(target.x,target.y,target.z);
            else {var safe=OtherWorld.safePlayer((ServerLevel)m.level(),BlockPos.containing(target));m.teleportTo(safe.getX()+.5,safe.getY(),safe.getZ()+.5);}
        }
        if(wait&&!m.getNavigation().isDone()){
            m.setYHeadRot(net.minecraft.util.Mth.approachDegrees(m.getYHeadRot(),m.getYRot(),15));
            m.setXRot(net.minecraft.util.Mth.approachDegrees(m.getXRot(),0,10));m.setYBodyRot(m.getYRot());
        }else m.lookAtStoryPlayer(p);
        return true;
    }
    static Vec3 portalWaitingSpot(ServerLevel level,BlockPos portal){
        for(var offset:new BlockPos[]{portal.east(2),portal.west(2),portal.east(2).north(),portal.west(2).north(),portal.north(2)})for(int dy:new int[]{0,1,-1}){
            var at=offset.above(dy);var feet=Vec3.atBottomCenterOf(at);
            var box=net.minecraft.world.entity.EntityDimensions.fixed(.7F,1.9F).makeBoundingBox(feet);
            if(level.getBlockState(at.below()).isFaceSturdy(level,at.below(),net.minecraft.core.Direction.UP)&&level.noCollision(box)&&!level.containsAnyLiquid(box))return feet;
        }
        return Vec3.atBottomCenterOf(portal.north(2));
    }
    public static BlockPos target(ServerPlayer p,VisitData.Scene s){return switch(s.loop.phase){case GIFTS->s.table.north(2);case WINDOW,ARRIVAL,READY->s.loop.portal.north(2);default->null;};}
    public static void depart(ServerPlayer p,boolean bed){
        if(AcceptedButtonEyes.active(scene(p)))return;
        com.coraline.secrets.CatEncounter.cancel(p,false);
        com.coraline.secrets.Reconciliation.cancel(p);
        var s=active(p);if(s==null)return;
        if(!VisitEvents.cleanup(p,s))ShowLifecycle.deferCleanup(p,s);
        s.loop.secrets.dialogue.remove("ReconciledProposal");
        s.loop.secrets.dialogue.remove("LoopVoice");
        s.loop.secrets.dialogue.remove("LoopFinished");
        NewActivitySafety.abort(p,s);
        OceanMonumentActivity.abort(p,s);
        if(s.loop.activity==OtherMotherActivities.Activity.MICE_CIRCUS)MiceCircusActivity.refund(p,s);
        var d=DoorData.get(p.server).doors.get(s.door);
        if(d!=null){var source=com.coraline.parents.FamilyData.level(p.server,d.dimension);if(source!=null){d.closedNight=Math.floorDiv(source.getDayTime(),24000);d.activeUntil=0;
            if(bed)source.setDayTime((d.closedNight+1)*24000);
            var state=source.getBlockState(d.pos);if(state.is(com.coraline.registry.ModBlocks.TINY_DOOR.get()))source.setBlock(d.pos,state.setValue(com.coraline.block.TinyDoorBlock.PORTAL,false).setValue(com.coraline.block.TinyDoorBlock.OPEN,false),3);
            DoorData.get(p.server).setDirty();}}
        phase(p,s,NONE);s.loop.gifts.clear();LoopProps.clean(p.server,s);removeBar(p.getUUID());VisitAudio.stopVoice(p);
    }
    /** Creative shortcut leaves the current visit resumable on the next door entry. */
    public static void resetForDebug(ServerPlayer p,VisitData.Scene s){
        if(AcceptedButtonEyes.active(s)){p.displayClientMessage(Component.literal("Use Accepted Button-Eyes Storyline controls for this checkpoint."),true);return;}
        if(!VisitEvents.cleanup(p,s))ShowLifecycle.deferCleanup(p,s);
        com.coraline.secrets.Reconciliation.cancel(p);
        s.loop.secrets.dialogue.remove("ReconciledProposal");
        s.loop.secrets.dialogue.remove("LoopVoice");
        s.loop.secrets.dialogue.remove("LoopFinished");
        NewActivitySafety.abort(p,s);
        OceanMonumentActivity.abort(p,s);
        if(s.loop.activity==OtherMotherActivities.Activity.MICE_CIRCUS)MiceCircusActivity.refund(p,s);
        s.loop.phase=NONE;s.loop.deadline=0;s.loop.gifts.clear();s.welcomeAfter=0;
        LoopProps.clean(p.server,s);removeBar(p.getUUID());listening.remove(p.getUUID());VisitAudio.stopVoice(p);changed(p);
    }
    static void fail(ServerPlayer p,String message){if(AcceptedButtonEyes.active(scene(p))){AcceptedButtonEyes.recoverFailure(p,message);return;}depart(p,false);OtherWorld.leave(p,false);p.sendSystemMessage(Component.literal(message));}
    private static void removeBar(UUID id){var b=bars.remove(id);if(b!=null)b.removeAllPlayers();}
    public static void forget(UUID id){listening.remove(id);removeBar(id);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){forget(e.getEntity().getUUID());}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p){forget(p.getUUID());if(com.coraline.multiplayer.StoryCoop.guest(p))return;var s=scene(p);if(AcceptedButtonEyes.active(s)){p.getPersistentData().putUUID("CoralineVisitDoor",s.door);AcceptedButtonEyes.data(s).putBoolean("Recover",true);changed(p);return;}if(s!=null&&s.loop.active()){s.welcomeAfter=0;changed(p);if(active(p)==null){OceanMonumentActivity.abort(p,s);
        if(s.loop.activity==OtherMotherActivities.Activity.MICE_CIRCUS)MiceCircusActivity.refund(p,s);s.loop.phase=NONE;s.loop.gifts.clear();LoopProps.clean(p.server,s);}}}}
    @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent e){listening.clear();bars.values().forEach(ServerBossEvent::removeAllPlayers);bars.clear();}
}
