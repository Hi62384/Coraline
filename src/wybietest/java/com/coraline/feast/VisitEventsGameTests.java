package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.registry.ModEntities;
import com.coraline.secrets.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class VisitEventsGameTests {
    private static ServerPlayer fixture(GameTestHelper h,int offset){
        var level=h.getLevel().getServer().getLevel(OtherWorld.KEY);var at=new BlockPos(24000+offset*160,130,24000);level.getChunkAt(at);
        var p=new ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"EventQA"));p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);p.setPos(Vec3.atBottomCenterOf(at));
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.table=at;s.piano=at.east(20);s.loop.phase=OtherMotherLoopState.Phase.EXPLORING;s.loop.visits=2;s.loop.secrets.visit=2;s.loop.trust=.5F;s.loop.suspicion=.1F;VisitData.get(p.server).scenes.put(p.getUUID(),s);p.getPersistentData().putUUID("CoralineVisitDoor",s.door);
        var r=new RealmData.Realm();r.door=s.door;r.doorPos=at;r.sourceDoor=at;r.house=at;r.porch=at;r.table=s.table;r.piano=s.piano;r.ready=true;RealmData.get(p.server).realms.put(s.door,r);
        return p;
    }
    private static void clear(ServerPlayer p){var s=OtherMotherLoop.scene(p);VisitEvents.cleanup(p,s);RealmData.get(p.server).realms.remove(s.door);VisitData.get(p.server).scenes.remove(p.getUUID());}
    @GameTest(template="empty",batch="events-authority",timeoutTicks=100)
    public static void telescopeChecksSelectionsAndRejectsReplayAndForeignOwner(GameTestHelper h){
        var p=fixture(h,10);var other=fixture(h,11);var s=OtherMotherLoop.scene(p);var n=s.loop.event;
        n.putString("Type",VisitEvents.Type.FAMILY_OUTING.name());n.putInt("Serial",1);s.loop.phase=OtherMotherLoopState.Phase.OUTING_ACTIVE;
        var chunk=new net.minecraft.world.level.ChunkPos(s.table);p.serverLevel().setChunkForced(chunk.x,chunk.z,true);
        h.runAtTickTime(40,()->{
            var telescope=VisitEvents.prop(p,s,ScenePropEntity.OUTING_TELESCOPE,p.position().add(0,0,2),0);
            try{
                n.putString("OutingSubtype","STARGAZING");FamilyOuting.interact(p,telescope);h.assertFalse(n.hasUUID("TelescopeNonce"),"Telescope opened before Father's introduction finished");n.putBoolean("InteractionSpoken",true);
                FamilyOuting.interact(p,telescope);var first=n.getUUID("TelescopeNonce");
                h.assertTrue(!FamilyOuting.choice(other,first,"star_selection:0,1,2")&&n.hasUUID("TelescopeNonce"),"Foreign owner consumed or advanced the observation");
                FamilyOuting.choice(p,first,"star_correct");h.assertTrue(n.getInt("StarsObserved")==0&&!n.hasUUID("TelescopeNonce"),"Client success assertion granted progress");
                FamilyOuting.interact(p,telescope);var second=n.getUUID("TelescopeNonce");
                FamilyOuting.choice(p,first,"star_selection:0,1,2");h.assertTrue(n.getUUID("TelescopeNonce").equals(second),"Stale nonce consumed the fresh observation");
                FamilyOuting.choice(p,second,"star_selection:0,0,1");h.assertTrue(n.getInt("StarsObserved")==0,"Duplicate star IDs passed the pattern");
                for(int observation=0;observation<ConstellationPuzzle.ROUNDS;observation++){
                    FamilyOuting.interact(p,telescope);var nonce=n.getUUID("TelescopeNonce");String answer="star_selection:"+ConstellationPuzzle.targets(n.getLong("OutingSeed"),observation).stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));FamilyOuting.choice(p,nonce,answer);
                    h.assertTrue(n.getInt("StarsObserved")==observation+1,"Valid unordered selection did not advance exactly once");
                    FamilyOuting.choice(p,nonce,"star_selection:0,1,2");h.assertTrue(n.getInt("StarsObserved")==observation+1,"Selection replay advanced twice");
                }
                var finalNonce=n.getUUID("TelescopeNonce");FamilyOuting.interact(p,telescope);h.assertTrue(n.getUUID("TelescopeNonce").equals(finalNonce),"Completed telescope minted another observation");
                h.assertTrue(Math.abs(s.loop.trust-.505F)<.001,"Telescope granted repeated relationship rewards");
            }finally{telescope.discard();clear(p);clear(other);p.serverLevel().setChunkForced(chunk.x,chunk.z,false);}
            h.succeed();
        });
    }
    @GameTest(template="empty",batch="events-actors",timeoutTicks=100)
    public static void fatherRecoveryMatchesRealmGenerationAndDoesNotDuplicate(GameTestHelper h){
        var p=fixture(h,8);var s=OtherMotherLoop.scene(p);var r=RealmData.get(p.server).realms.get(s.door);r.parentGeneration=7;s.loop.event.putString("Type",VisitEvents.Type.EXPLORATION_30_MIN.name());s.loop.event.putInt("Serial",1);s.loop.phase=OtherMotherLoopState.Phase.FREE_TIME;
        var chunk=new net.minecraft.world.level.ChunkPos(s.piano);p.serverLevel().setChunkForced(chunk.x,chunk.z,true);for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){p.serverLevel().setBlock(s.piano.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);for(int y=0;y<3;y++)p.serverLevel().setBlock(s.piano.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);}
        h.runAtTickTime(40,()->VisitEvents.ensureFather(p,s));
        h.runAtTickTime(50,()->{var dad=p.serverLevel().getEntity(s.dad);h.assertTrue(dad instanceof com.coraline.parents.ParentEntity&&dad.getPersistentData().getInt("RealmGeneration")==7,"Realm protection rejected recovered Father generation");var id=s.dad;VisitEvents.ensureFather(p,s);h.assertTrue(s.dad.equals(id)&&VisitEvents.props(p,s).stream().filter(e->e.kind()==ScenePropEntity.PIANO).count()==1,"Recovery created duplicate Father or piano");dad.discard();clear(p);p.serverLevel().setChunkForced(chunk.x,chunk.z,false);h.succeed();});
    }
    @GameTest(template="empty",batch="events-restrictions",timeoutTicks=100)
    public static void freeTimeStillRunsForbiddenItemPursuit(GameTestHelper h){
        var p=fixture(h,6);var s=OtherMotherLoop.scene(p);s.loop.event.putString("Type",VisitEvents.Type.EXPLORATION_30_MIN.name());s.loop.phase=OtherMotherLoopState.Phase.FREE_TIME;
        var at=s.table;var chunk=new net.minecraft.world.level.ChunkPos(at);p.serverLevel().setChunkForced(chunk.x,chunk.z,true);
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){p.serverLevel().setBlock(at.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);for(int y=0;y<3;y++)p.serverLevel().setBlock(at.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);}
        var mom=ModEntities.MOM.get().create(p.serverLevel());mom.makeOtherParent(0);mom.getPersistentData().putUUID("LoopOwner",p.getUUID());mom.getPersistentData().putUUID("RealmDoor",s.door);mom.setPos(p.position().add(-2,0,0));mom.setNoAi(true);mom.setNoGravity(true);s.mom=mom.getUUID();
        h.runAtTickTime(40,()->p.serverLevel().addFreshEntity(mom));
        h.runAtTickTime(50,()->{h.assertTrue(SecretsRuntime.active(p)&&MotherSearch.mother(p)==mom,"New event disabled secret runtime or actor ownership");mom.setYRot(-90);mom.setYHeadRot(-90);h.assertTrue(mom.hasLineOfSight(p),"Native test Mother had no line of sight to evidence carrier");SecretTransactions.issue(p,"drawer_marks");h.assertTrue(MotherInterrupts.check(p)&&s.loop.secrets.search==SecretsState.Search.DIRECT_CHASE&&!Reconciliation.active(p),"Authorized free time disabled witnessed forbidden-item pursuit or scolded at a distance");mom.discard();clear(p);p.serverLevel().setChunkForced(chunk.x,chunk.z,false);h.succeed();});
    }
    private static void afterFatherLine(GameTestHelper h,VisitData.Scene s,Runnable action){h.runAfterDelay(VisitAudio.length(s.loop.event.getString("StudyVoice"))+1,action);}
    @GameTest(template="empty",batch="events-dialogue",timeoutTicks=1500)
    public static void dialogueChoicesAreOwnerBoundAndCannotBeReplayed(GameTestHelper h){
        var p=fixture(h,4);var s=OtherMotherLoop.scene(p);s.loop.event.putString("Type",VisitEvents.Type.EXPLORATION_30_MIN.name());s.loop.phase=OtherMotherLoopState.Phase.FREE_TIME;s.loop.event.putInt("Remaining",15000);
        var chunk=new net.minecraft.world.level.ChunkPos(s.table);p.serverLevel().setChunkForced(chunk.x,chunk.z,true);
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){p.serverLevel().setBlock(s.table.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);for(int y=0;y<3;y++)p.serverLevel().setBlock(s.table.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);}
        var dad=ModEntities.DAD.get().create(p.serverLevel());dad.makeOtherParent(0);dad.getPersistentData().putUUID("VisitEventOwner",p.getUUID());dad.getPersistentData().putUUID("RealmDoor",s.door);dad.setPos(p.position().add(2,0,0));dad.setNoAi(true);dad.setNoGravity(true);s.dad=dad.getUUID();
        var mom=ModEntities.MOM.get().create(p.serverLevel());mom.makeOtherParent(0);mom.getPersistentData().putUUID("LoopOwner",p.getUUID());mom.getPersistentData().putUUID("RealmDoor",s.door);mom.setPos(p.position().add(-2,0,0));mom.setNoAi(true);mom.setNoGravity(true);s.mom=mom.getUUID();
        h.runAtTickTime(40,()->{p.serverLevel().addFreshEntity(dad);p.serverLevel().addFreshEntity(mom);});
        h.runAtTickTime(50,()->{
            h.assertTrue(p.serverLevel().getEntity(s.dad)==dad&&dad.distanceToSqr(p)<49,"Dialogue fixture actor was not loaded in interaction range: "+dad.position());
            VisitEvents.openConversation(p,false);var first=VisitEvents.nonce(p);VisitEvents.choose(p,first,"1");h.assertTrue(VisitEvents.nonce(p).equals(first),"Choice skipped an unfinished spoken line");
            afterFatherLine(h,s,()->{
            VisitEvents.choose(p,first,"1");var second=VisitEvents.nonce(p);VisitEvents.choose(p,first,"0");h.assertTrue(VisitEvents.nonce(p).equals(second)&&s.loop.secrets.issued.isEmpty(),"Stale button request advanced dialogue");
            afterFatherLine(h,s,()->{VisitEvents.choose(p,second,"0");
            afterFatherLine(h,s,()->{VisitEvents.choose(p,VisitEvents.nonce(p),"0");h.assertTrue(s.loop.secrets.issued.containsValue("drawer_marks")&&s.loop.event.getBoolean("Rewarded"),"Tactful two-step branch failed to issue actual evidence");
            afterFatherLine(h,s,()->{
            VisitEvents.choose(p,VisitEvents.nonce(p),"1");VisitEvents.openConversation(p,true);VisitEvents.choose(p,VisitEvents.nonce(p),"1");h.assertTrue(s.loop.phase==OtherMotherLoopState.Phase.FREE_TIME&&s.loop.event.getInt("Remaining")==15000,"Declining cancellation reset timer");
            VisitEvents.openConversation(p,true);var end=VisitEvents.nonce(p);VisitEvents.choose(p,end,"0");var phase=s.loop.phase;VisitEvents.choose(p,end,"0");h.assertTrue(VisitEvents.type(s)!=VisitEvents.Type.EXPLORATION_30_MIN&&phase==s.loop.phase&&s.loop.visits==2,"Early cancellation repeated event or replayed welcome");
            mom.discard();dad.discard();clear(p);p.serverLevel().setChunkForced(chunk.x,chunk.z,false);h.succeed();
            });});});});
        });
    }
    @GameTest(template="empty",batch="events-ledger",timeoutTicks=100)
    public static void weightedCategoriesAndGraceDistribution(GameTestHelper h){
        var random=RandomSource.create(140);var types=EnumSet.noneOf(VisitEvents.Type.class);
        for(int i=0;i<200;i++){var selected=VisitEvents.select(random,null,false);types.add(selected);var next=VisitEvents.select(random,selected,false);h.assertTrue(next!=selected,"Back-to-back event repetition");}
        h.assertTrue(types.size()==VisitEvents.Type.values().length,"Selector did not include every event category");double low=0,high=0;
        for(int i=0;i<1000;i++){double u=(i+.5)/1000;int a=FamilyDinner.graceTicks(0,u),b=FamilyDinner.graceTicks(1,u);h.assertTrue(a>=1200&&a<=12000&&b>=1200&&b<=12000,"Grace outside 1–10 minutes");low+=a;high+=b;}
        h.assertTrue(high<low*.5,"Higher suspicion did not shorten average grace");h.succeed();
    }
    @GameTest(template="empty",batch="events-ledger",timeoutTicks=100)
    public static void timerPersistsAndDoesNotRunDuringDialogue(GameTestHelper h){
        var p=fixture(h,0);var s=OtherMotherLoop.scene(p);s.loop.event.putString("Type",VisitEvents.Type.EXPLORATION_30_MIN.name());s.loop.event.putInt("Remaining",36000);s.loop.phase=OtherMotherLoopState.Phase.FREE_TIME;p.tickCount=1;
        VisitEvents.tick(p,s);h.assertTrue(s.loop.event.getInt("Remaining")==35999&&VisitEvents.authorized(p)&&SecretsRuntime.active(p)&&s.loop.canLeave(),"Free time did not count exactly one active tick or disabled journal/spy/exit eligibility");
        var loaded=OtherMotherLoopState.load(s.loop.save());h.assertTrue(loaded.event.getInt("Remaining")==35999&&loaded.phase==OtherMotherLoopState.Phase.FREE_TIME,"Restart lost remaining duration");
        s.loop.phase=OtherMotherLoopState.Phase.EVENT_INTRO;VisitEvents.tick(p,s);h.assertTrue(s.loop.event.getInt("Remaining")==35999,"Waiting for introduction used free time");
        s.loop.phase=OtherMotherLoopState.Phase.FREE_TIME;s.loop.secrets.search=SecretsState.Search.DIRECT_CHASE;VisitEvents.tick(p,s);h.assertTrue(s.loop.event.getInt("Remaining")==35999,"Scripted pursuit consumed free time");s.loop.secrets.search=SecretsState.Search.NORMAL;
        s.loop.event.putInt("Remaining",1);VisitEvents.tick(p,s);h.assertTrue(s.loop.phase==OtherMotherLoopState.Phase.EVENT_RETURN&&p.position().equals(Vec3.atBottomCenterOf(s.table)),"Expiration teleported player or failed to wait for return");clear(p);h.succeed();
    }
    @GameTest(template="empty",batch="events-ledger",timeoutTicks=100)
    public static void fatherRewardUsesBoundEvidenceAndPerVisitLimit(GameTestHelper h){
        var p=fixture(h,1);var s=OtherMotherLoop.scene(p);
        h.assertTrue(VisitEvents.reward(p,s,"drawer_marks"),"Tactful registered secret was not issued");h.assertTrue(s.loop.secrets.issued.containsValue("drawer_marks")&&s.loop.secrets.journal.isEmpty(),"Reward bypassed physical evidence/journal activation");
        h.assertTrue(!VisitEvents.reward(p,s,"gift_preparation")&&!VisitEvents.reward(p,s,"soul_prisons"),"Unlimited or severe secrets awarded");
        s.loop.event.putBoolean("Rewarded",false);s.loop.secrets.issued.clear();h.assertTrue(!VisitEvents.reward(p,s,"drawer_marks"),"Same-visit confiscation allowed farming");s.loop.visits++;h.assertTrue(VisitEvents.reward(p,s,"drawer_marks"),"Later-visit rediscovery was blocked");clear(p);h.succeed();
    }
    @GameTest(template="empty",batch="events-ledger",timeoutTicks=100)
    public static void dinnerGraceRolledOnceAndReconciliationKeepsDinner(GameTestHelper h){
        var p=fixture(h,2);var s=OtherMotherLoop.scene(p);s.loop.event.putString("Type",VisitEvents.Type.FAMILY_DINNER.name());s.loop.event.putBoolean("DinnerPending",true);s.loop.phase=OtherMotherLoopState.Phase.DINNER_WAIT;s.loop.event.putInt("ArrivalRemaining",1);
        FamilyDinner.tick(p,s);h.assertTrue(s.loop.event.getInt("ArrivalRemaining")==0,"Arrival window did not use active ticks");FamilyDinner.tick(p,s);int grace=s.loop.event.getInt("GraceRemaining");h.assertTrue(grace>=1200&&grace<=12000,"No persistent grace delay");FamilyDinner.tick(p,s);h.assertTrue(s.loop.event.getInt("GraceRemaining")==grace-1,"Grace rerolled instead of decrementing");
        var chair=VisitEvents.prop(p,s,ScenePropEntity.CHAIR,p.position().add(2,0,0),2);OtherMotherLoop.clearActivityWindow(p);VisitEvents.propTick(chair);h.assertTrue(s.loop.event.getBoolean("DinnerPending")&&VisitEvents.keepFather(s)&&!chair.isRemoved(),"Confrontation discarded pending dinner or retired its father/furniture");h.assertTrue(VisitEvents.resumeDinner(p)&&s.loop.phase==OtherMotherLoopState.Phase.DINNER_WAIT&&s.loop.event.getBoolean("Resuming"),"Reconciliation selected an activity instead of dinner");chair.discard();clear(p);h.succeed();
    }
    @GameTest(template="empty",batch="events-ledger",timeoutTicks=100)
    public static void dinnerArrivalRequiresOwnSeatAndCannotEraseSearch(GameTestHelper h){
        var p=fixture(h,3);var s=OtherMotherLoop.scene(p);s.loop.event.putString("Type",VisitEvents.Type.FAMILY_DINNER.name());s.loop.event.putInt("Serial",1);s.loop.phase=OtherMotherLoopState.Phase.DINNER_WAIT;s.loop.event.putInt("ArrivalRemaining",500);
        var chair=VisitEvents.prop(p,s,ScenePropEntity.CHAIR,p.position(),0);chair.owner=UUID.randomUUID();h.assertTrue(!FamilyDinner.arrive(p,s,chair),"Helper occupied reserved chair");chair.owner=p.getUUID();s.loop.secrets.search=SecretsState.Search.SEARCHING;h.assertTrue(!FamilyDinner.arrive(p,s,chair),"Seat erased active pursuit");s.loop.secrets.search=SecretsState.Search.NORMAL;
        h.assertTrue(FamilyDinner.arrive(p,s,chair),"Owner could not sit for dinner");float trust=s.loop.trust;h.assertTrue(Math.abs(trust-.54F)<.001&&!FamilyDinner.arrive(p,s,chair)&&s.loop.trust==trust,"Arrival reward repeated or wrong");h.assertTrue(s.loop.phase==OtherMotherLoopState.Phase.DINNER_GREETING,"Meal began before greeting");p.stopRiding();chair.discard();clear(p);h.succeed();
    }
    @GameTest(template="empty",batch="events-ledger",timeoutTicks=100)
    public static void voiceCannotBeSkippedBeforeDurationAndMealEndsWithoutClicks(GameTestHelper h){
        var p=fixture(h,5);var s=OtherMotherLoop.scene(p);var n=s.loop.event;var pool=VisitEvents.pool(OtherMotherWelcome.State.LOW_TRUST_LOW_SUSPICION);n.putString("Voice",pool.path());n.putInt("VoiceLength",10);n.putInt("IntroElapsed",0);n.putBoolean("VoiceFinished",true);
        for(int i=0;i<9;i++)h.assertTrue(!VisitEvents.speak(p,s,pool),"Early client completion skipped recording duration");h.assertTrue(VisitEvents.speak(p,s,pool),"Full acknowledged voice did not complete");
        n.putString("Type",VisitEvents.Type.FAMILY_DINNER.name());n.putBoolean("DinnerPending",true);n.putInt("MealElapsed",2999);s.loop.phase=OtherMotherLoopState.Phase.DINNER;FamilyDinner.tick(p,s);h.assertTrue(s.loop.phase==OtherMotherLoopState.Phase.DINNER_FINISH&&n.getBoolean("DinnerPending"),"Dinner did not reach finish or stopped being resumable before closing dialogue");
        n.putBoolean("FatherFinished",true);var finish=FamilyDinner.pool("finish",SecretDialogue.state(p));n.putString("Voice",finish.path());n.putInt("VoiceLength",10);n.putInt("IntroElapsed",9);n.putBoolean("VoiceFinished",true);FamilyDinner.tick(p,s);h.assertTrue(VisitEvents.type(s)!=VisitEvents.Type.FAMILY_DINNER&&s.loop.visits==2&&Math.abs(s.loop.trust-.51F)<.001,"Dinner completion repeated dinner/welcome or granted excessive trust");clear(p);h.succeed();
    }
}
