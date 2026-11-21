package com.coraline.feast;

import java.util.*;
import com.coraline.otherworld.*;
import com.coraline.registry.*;
import com.coraline.parents.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline")
@PrefixGameTestTemplate(false)
public final class MissingParentsCatGameTests {
    private static ServerPlayer fixture(GameTestHelper h){
        var p=MissingParentsTeaGameTests.fixture(h);var s=OtherMotherLoop.scene(p);
        MissingParentsTeaGameTests.ready(p,s);FirstEscapeStory.data(s).putBoolean("WybieDeparted",true);
        MissingParentsTea.data(s).putBoolean("Completed",true);s.loop.story.putBoolean("SeeingStoneReceipt",true);
        return p;
    }
    private static void remove(ServerPlayer p){
        var s=OtherMotherLoop.scene(p);var n=MissingParentsCat.data(s);
        for(String key:List.of("MirrorActor","ParentDolls"))if(n.hasUUID(key)){var e=p.serverLevel().getEntity(n.getUUID(key));if(e!=null)e.discard();}
        var actor=ShowActors.actor(p,s,"cat");if(actor!=null)actor.discard();
        MissingParentsTeaGameTests.remove(p,s);
    }
    @GameTest(template="empty") public static void missingBedBeforeCatStartsPreservesTheMirrorClue(GameTestHelper h){
        var p=fixture(h);var s=OtherMotherLoop.scene(p);var n=MissingParentsCat.data(s);
        var family=new FamilyData.Family();family.owner=s.owner;family.storyMissing=true;
        FamilyData.get(p.server).families.put(family.id,family);
        try{
            MissingParentsCat.tick(p,s);
            h.assertFalse(n.getBoolean("Started")||n.getBoolean("Complete"),"Absent original bed completed an unstarted Cat story");
            h.assertFalse(n.getBoolean("DollsRetrieved")||s.loop.story.getBoolean("ParentDollsDiscovered"),"Missing original bed granted the mirror clue for free");
            h.assertTrue(n.getString("Blocker").contains("original parents' bed"),"Player was not told how to restore the Cat encounter");
        }finally{remove(p);}h.succeed();
    }
    @GameTest(template="empty") public static void missingBedFallbackCannotLoseTheOriginalCatForTheBoss(GameTestHelper h){
        var p=fixture(h);var s=OtherMotherLoop.scene(p);var n=MissingParentsCat.data(s);
        var family=new FamilyData.Family();family.owner=s.owner;family.storyMissing=true;
        FamilyData.get(p.server).families.put(family.id,family);
        try{
            n.putBoolean("Started",true);n.putString("Phase","A");n.putUUID("Owner",s.owner);n.putUUID("Door",s.door);
            n.putInt("MissingBedTicks",199);
            MissingParentsCat.tick(p,s);
            h.assertTrue(n.getInt("MissingBedTicks")==200,"Interrupted missing-bed timer did not resume");
            h.assertFalse(n.getBoolean("Complete")||n.getBoolean("TunnelRestored"),"Lost Cat advanced toward an impossible later fight");
            h.assertFalse(n.getBoolean("DollsRetrieved")||s.loop.story.getBoolean("ParentDollsDiscovered"),"Original Cat was missing but the clue was marked collected");
            h.assertTrue(n.getString("Blocker").contains("original Cat"),"Missing canonical Cat produced no actionable blocker");
            h.assertFalse(MissingParentsCat.missingBedRecoveryReady(false,true,200),"Unstarted Cat scene completed");
            h.assertFalse(MissingParentsCat.missingBedRecoveryReady(true,false,200),"Missing original Cat completed");
            h.assertFalse(MissingParentsCat.missingBedRecoveryReady(true,true,199),"Scene completed before its recovery grace");
            h.assertTrue(MissingParentsCat.missingBedRecoveryReady(true,true,200),"Loaded Cat could not resume a blocked scene");
        }finally{remove(p);}h.succeed();
    }
    @GameTest(template="empty") public static void catWatchdogCannotAwardDollsWhileOriginalCatIsUnloaded(GameTestHelper h){
        var p=fixture(h);var s=OtherMotherLoop.scene(p);var n=MissingParentsCat.data(s);
        try{
            n.putBoolean("Started",true);n.putString("Phase","FOLLOW_BED");
            n.putLong("Bed",p.blockPosition().asLong());n.putInt("StuckTicks",200);
            for(int tick=0;tick<22;tick++)MissingParentsCat.cutsceneSafety(p,s);
            h.assertTrue(MissingParentsCat.phase(s)==MissingParentsCat.Phase.FOLLOW_BED,
                    "An unloaded Cat was skipped by the follow-bed watchdog");
            h.assertFalse(n.getBoolean("DollsRetrieved")||s.loop.story.getBoolean("ParentDollsDiscovered"),
                    "Unloaded Cat granted unearned parent dolls");
            n.putString("Phase","E");
            for(int tick=0;tick<50;tick++)MissingParentsCat.cutsceneSafety(p,s);
            h.assertTrue(MissingParentsCat.phase(s)==MissingParentsCat.Phase.E,
                    "Cat was missing but the dolls performance was completed");
            h.assertFalse(n.getBoolean("DollsRetrieved")||s.loop.story.getBoolean("ParentDollsDiscovered"),
                    "Voice watchdog awarded dolls without the original Cat");
            n.putString("Phase","F");
            MissingParentsCat.cutsceneSafety(p,s);
            h.assertTrue(MissingParentsCat.phase(s)==MissingParentsCat.Phase.F,
                    "Cat's finale voice skipped past an absent original actor");
            h.assertFalse(n.getBoolean("FFinished")||n.getBoolean("TunnelRestored"),
                    "Tunnel opened without the original Cat");
        }finally{remove(p);}h.succeed();
    }
    @GameTest(template="empty",batch="open-air-cat-mirror") public static void openingLineCanResolveAMirrorWithoutAnExistingHouseWall(GameTestHelper h){
        var p=fixture(h);var s=OtherMotherLoop.scene(p);var l=p.serverLevel();var bed=new BlockPos(130600,180,130600);var n=MissingParentsCat.data(s);
        try{
            for(int x=-13;x<=13;x++)for(int z=-13;z<=13;z++){var at=bed.offset(x,0,z);l.getChunkAt(at);l.setBlock(at.below(),Blocks.CHERRY_PLANKS.defaultBlockState(),2);for(int y=0;y<=5;y++)l.setBlock(at.above(y),Blocks.AIR.defaultBlockState(),2);}
            var state=ModBlocks.PARENTS_BED.get().defaultBlockState();for(int part=0;part<4;part++)l.setBlock(ParentsBedBlock.partPos(bed,Direction.NORTH,part),state.setValue(ParentsBedBlock.PART,part),2);
            h.assertTrue(MissingParentsCat.mirror(p,s,bed),"Open-air bed leaves A waiting forever for a wall");
            var chosen=BlockPos.of(n.getLong("Mirror"));var approach=BlockPos.of(n.getLong("MirrorApproach"));
            h.assertTrue(l.getBlockState(chosen).isAir()&&l.getBlockState(chosen.below()).is(Blocks.CHERRY_PLANKS),"Mirror fallback overwrites the player's floor/build");
            h.assertTrue(l.isEmptyBlock(approach)&&l.isEmptyBlock(approach.above())&&approach.distSqr(chosen)<25,"Mirror has no local approach");
            long saved=n.getLong("Mirror");h.assertTrue(MissingParentsCat.mirror(p,s,bed)&&saved==n.getLong("Mirror"),"Retry moves a valid mirror checkpoint");
        }finally{remove(p);}h.succeed();
    }
    @GameTest(template="empty",batch="blocked-cat-mirror") public static void blockedMirrorSkipsUnavailablePresentationWithoutCreatingRewards(GameTestHelper h){
        var p=fixture(h);var s=OtherMotherLoop.scene(p);var l=p.serverLevel();var bed=new BlockPos(130900,180,130900);var n=MissingParentsCat.data(s);var family=new FamilyData.Family();family.owner=s.owner;family.bed=bed;FamilyData.get(p.server).families.put(family.id,family);
        try{
            for(int x=-13;x<=13;x++)for(int z=-13;z<=13;z++)for(int y=-1;y<=5;y++){var at=bed.offset(x,y,z);l.getChunkAt(at);l.setBlock(at,Blocks.STONE.defaultBlockState(),2);}
            family.bed=bed;n.putString("Phase","A");n.putBoolean("Started",true);n.putUUID("Owner",s.owner);n.putUUID("Door",s.door);
            for(int tick=0;tick<200;tick++)MissingParentsCat.cutsceneSafety(p,s);
            h.assertTrue(MissingParentsCat.phase(s)==MissingParentsCat.Phase.A&&!n.contains("MirrorApproach")&&!n.hasUUID("MirrorActor"),"Blocked staging or absent original Cat fabricated mirror advancement");
            h.assertTrue(!n.getBoolean("MirrorBroken")&&!s.loop.story.getBoolean("ParentDollsDiscovered")&&!n.getBoolean("Complete"),"Missing original Cat was allowed to shatter the mirror or grant dolls");
            h.assertTrue(s.loop.story.getCompound("CutsceneSafety").getList("History",10).isEmpty(),"Watchdog says it continued without a valid next step");
        }finally{remove(p);}h.succeed();
    }
    @GameTest(template="empty") public static void requiresEveryCanonicalReceiptAndNeverRepeats(GameTestHelper h){
        var p=fixture(h);var s=OtherMotherLoop.scene(p);
        try{
            h.assertTrue(MissingParentsCat.eligible(s),"Canonical completed aftermath not eligible");
            var root=FirstEscapeStory.data(s);
            for(String flag:List.of("Complete","WybieDeparted","OtherWorldSealed")){
                root.putBoolean(flag,false);h.assertFalse(MissingParentsCat.eligible(s),"Missing prerequisite accepted: "+flag);root.putBoolean(flag,true);
            }
            root.putString("Phase","FALLING");h.assertFalse(MissingParentsCat.eligible(s),"Wrong escape checkpoint accepted");root.putString("Phase","ESCAPE_COMPLETE");
            MissingParentsTea.data(s).putBoolean("Active",true);h.assertFalse(MissingParentsCat.eligible(s),"Live tea cinematic overlapped");MissingParentsTea.data(s).putBoolean("Active",false);
            s.loop.story.putBoolean("SeeingStonePending",true);h.assertFalse(MissingParentsCat.eligible(s),"Unresolved Seeing Stone bypassed");s.loop.story.putBoolean("SeeingStonePending",false);
            MissingParentsCat.data(s).putBoolean("Complete",true);h.assertFalse(MissingParentsCat.eligible(s),"Completed event repeats");MissingParentsCat.data(s).putBoolean("Complete",false);
            ParentRescueStory.data(s).putBoolean("Started",true);h.assertFalse(MissingParentsCat.eligible(s),"Later storyline overwritten");
        }finally{remove(p);}h.succeed();
    }
    @GameTest(template="empty") public static void mirrorRightClickRequiresOwnerRangeAndOnePrompt(GameTestHelper h){
        var p=fixture(h);var helper=fixture(h);var s=OtherMotherLoop.scene(p);var n=MissingParentsCat.data(s);var at=h.absolutePos(new BlockPos(3,40,3));
        try{
            n.putLong("Mirror",at.asLong());n.putString("Phase","WAIT_BREAK");p.setPos(at.getCenter().add(0,0,10));
            h.assertFalse(MissingParentsCat.block(p,at),"Remote mirror activation accepted");p.setPos(at.getCenter());
            h.assertFalse(MissingParentsCat.owns(helper,s),"Helper owns canonical scene");
            h.assertFalse(MissingParentsCat.block(helper,at),"Helper activated another owner's mirror");
            h.assertTrue(MissingParentsCat.block(p,at),"Prompted nearby mirror not interactive");
            h.assertTrue(MissingParentsCat.phase(s)==MissingParentsCat.Phase.C,"Right-click skipped C");
            var mirror=(CinematicPropEntity)p.serverLevel().getEntity(n.getUUID("MirrorActor"));
            h.assertTrue(mirror!=null&&mirror.getBoundingBox().contains(mirror.position().add(1,2.5,0)),"Mirror's upper/corner area cannot be clicked");
            mirror.setYRot(90);mirror.setPos(mirror.position());
            h.assertTrue(mirror.getBoundingBox().contains(mirror.position().add(0,2.5,1))&&!mirror.getBoundingBox().contains(mirror.position().add(1,2.5,0)),"Mirror hitbox did not follow its facing");
            h.assertFalse(MissingParentsCat.block(p,at),"Repeated click restarted C");
            h.assertFalse(n.getBoolean("MirrorBroken"),"Mirror shattered before actual C ending");
        }finally{remove(p);remove(helper);}h.succeed();
    }
    @GameTest(template="empty") public static void dollsFollowEveryBedFacingAndKeepTheirIdentity(GameTestHelper h){
        var p=fixture(h);var s=OtherMotherLoop.scene(p);var n=MissingParentsCat.data(s);var foot=h.absolutePos(new BlockPos(4,40,4));
        try{
            p.serverLevel().getChunkAt(foot);n.putString("Phase","E");n.putInt("MomSkin",2);n.putInt("DadSkin",3);
            UUID identity=null;
            for(var facing:Direction.Plane.HORIZONTAL){
                var head=foot.relative(facing);var state=Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING,facing);
                p.serverLevel().setBlock(foot.below(),Blocks.STONE.defaultBlockState(),2);p.serverLevel().setBlock(head.below(),Blocks.STONE.defaultBlockState(),2);
                p.serverLevel().setBlock(foot,state.setValue(BedBlock.PART,BedPart.FOOT),2);p.serverLevel().setBlock(head,state.setValue(BedBlock.PART,BedPart.HEAD),2);
                h.assertTrue(MissingParentsCat.bedFoot(p,head).equals(foot),"Head did not resolve canonical foot");
                h.assertTrue(MissingParentsCat.bedFront(p,head)==facing.getOpposite(),"Retrieval goes through headboard");
                n.putLong("BedFoot",foot.asLong());n.putString("BedFront",facing.getOpposite().getName());MissingParentsCat.props(p,s);
                var prop=(CinematicPropEntity)p.serverLevel().getEntity(n.getUUID("ParentDolls"));
                h.assertTrue(prop!=null&&prop.owned(s),"Owner dolls absent");
                var expected=Vec3.atBottomCenterOf(foot).add(facing.getOpposite().getStepX()*.95,.12,facing.getOpposite().getStepZ()*.95);
                h.assertTrue(prop.position().distanceToSqr(expected)<1e-8,"Dolls positioned through bed side");
                h.assertTrue(Math.abs(prop.getYRot()-facing.getOpposite().toYRot())<.001,"Dolls face wrong direction");
                if(identity==null)identity=prop.getUUID();else h.assertTrue(identity.equals(prop.getUUID()),"Facing/retry cloned clue");
                p.serverLevel().removeBlock(foot,false);p.serverLevel().removeBlock(head,false);p.serverLevel().removeBlock(head.below(),false);
            }
        }finally{p.serverLevel().removeBlock(foot,false);p.serverLevel().removeBlock(foot.below(),false);remove(p);}h.succeed();
    }
    @GameTest(template="empty") public static void hiddenCatReleaseDoesNotWaitForUnrelatedUnloadedLease(GameTestHelper h){
        var p=fixture(h);var s=OtherMotherLoop.scene(p);var at=h.absolutePos(new BlockPos(3,45,3));p.setPos(at.getCenter());
        try{
            for(int z=0;z<=5;z++)for(int y=0;y<=3;y++)p.serverLevel().setBlock(at.offset(0,y,z),Blocks.AIR.defaultBlockState(),2);
            var cat=ShowActors.acquire(p,s,"cat",p.serverLevel(),p.position().add(0,0,4));h.assertTrue(cat!=null,"Canonical Cat not acquired");
            var unrelated=new CompoundTag();unrelated.putUUID("Entity",UUID.randomUUID());s.loop.event.getCompound("ShowActors").put("unloaded_test_actor",unrelated);
            p.setYRot(0);p.setXRot(0);h.assertTrue(ShowActors.visibleToAnyone(cat,p),"Visible Cat fixture outside gaze");
            h.assertFalse(ShowActors.releaseHidden(p,s,"cat"),"Visible Cat disappeared");
            p.setYRot(180);h.assertFalse(ShowActors.visibleToAnyone(cat,p),"Behind-camera Cat still in view");
            h.assertTrue(ShowActors.releaseHidden(p,s,"cat"),"Unrelated unloaded identity blocked Cat-only release");
            h.assertTrue(s.loop.event.getCompound("ShowActors").contains("unloaded_test_actor"),"Other lease discarded");
            h.assertTrue(ShowActors.actor(p,s,"cat")==null,"Cat lease not removed");
        }finally{remove(p);}h.succeed();
    }
    @GameTest(template="empty") public static void decayedDoorSurvivesReloadAndIgnoresDaytimeWithoutMice(GameTestHelper h){
        var p=fixture(h);var s=OtherMotherLoop.scene(p);long time=p.serverLevel().getDayTime();
        try{
            var block=new DoorBlockEntity(BlockPos.ZERO,ModBlocks.TINY_DOOR.get().defaultBlockState());block.doorId=s.door;block.storyDecayed=true;block.tunnelPreview=1;
            var saved=block.saveWithoutMetadata();h.assertFalse(saved.contains("TunnelPreview"),"Debug appearance persisted");
            var reload=new DoorBlockEntity(BlockPos.ZERO,ModBlocks.TINY_DOOR.get().defaultBlockState());reload.load(saved);
            h.assertTrue(reload.storyDecayed&&s.door.equals(reload.doorId)&&reload.tunnelPreview==0,"Decayed appearance lost on reload");
            h.assertTrue(block.getUpdateTag().getInt("TunnelPreview")==1,"Operator preview not synchronized");
            var door=new DoorData.Door();door.id=s.door;door.owner=s.owner;door.exists=true;door.placed=true;door.activeUntil=0;
            MissingParentsCat.data(s).putBoolean("TunnelRestored",true);
            for(long day:new long[]{1000,18000,25000,42000}){p.serverLevel().setDayTime(day);h.assertTrue(OtherWorld.active(p.serverLevel(),door),"Permanent tunnel depends on night/mice");}
            var state=OtherMotherLoopState.load(s.loop.save());h.assertTrue(state.story.getCompound("MissingParentsCat").getBoolean("TunnelRestored"),"Tunnel receipt lost on reload");
        }finally{p.serverLevel().setDayTime(time);remove(p);}h.succeed();
    }
    @GameTest(template="empty") public static void everyCatDebugControlIsRegisteredAndOwnerProtected(GameTestHelper h){
        for(String id:MissingParentsCatDebug.actions().keySet())h.assertTrue(NumberedDebugControls.find("cat_story",id)!=null,"Missing Cat debug control: "+id);
        var p=fixture(h);var helper=fixture(h);var s=OtherMotherLoop.scene(p);
        try{h.assertTrue(MissingParentsCatDebug.run(helper,s,"reset").contains("authorization"),"Helper reset owner storyline");h.assertTrue(MissingParentsCat.phase(s)==MissingParentsCat.Phase.PENDING,"Denied debug action changed checkpoint");}
        finally{remove(p);remove(helper);}h.succeed();
    }
}
