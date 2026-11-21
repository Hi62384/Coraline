package com.coraline.feast;

import java.util.*;
import com.coraline.registry.ModItems;
import com.coraline.secrets.SecretsItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline")
@PrefixGameTestTemplate(false)
public final class StoryContinuationGameTests {
    @GameTest(template="empty") public static void mirrorRetryDoesNotAcceptHalfSavedRoute(GameTestHelper h){
        var p=fixture(h);var s=OtherMotherLoop.scene(p);var bed=new BlockPos(129312,240,129312);p.serverLevel().getChunkAt(bed);var n=MissingParentsCat.data(s);
        n.putLong("Mirror",bed.east(100).asLong());
        h.assertFalse(MissingParentsCat.mirror(p,s,bed),"Half-saved surface bypassed route validation");
        h.assertFalse(n.contains("Mirror")||n.contains("MirrorApproach"),"Failed route committed a checkpoint");
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
            p.serverLevel().setBlock(bed.offset(x,-1,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
            // The fresh GameTest world can contain terrain above this platform.
            for(int y=0;y<=3;y++)p.serverLevel().setBlock(bed.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        }
        var mirror=bed.east(3).above();var chunk=new net.minecraft.world.level.ChunkPos(mirror);boolean forced=p.serverLevel().getForcedChunks().contains(chunk.toLong());
        p.serverLevel().setChunkForced(chunk.x,chunk.z,true);
        p.serverLevel().setBlock(mirror,com.coraline.registry.ModBlocks.ROSEWOOD_MIRROR.get().defaultBlockState(),2);
        // Production deliberately waits one second between failed searches.
        // An immediate retry is not a route failure; let that cooldown expire.
        h.runAfterDelay(21,()->{try{
            h.assertTrue(p.level().getBlockState(bed.offset(3,-1,-2)).is(net.minecraft.world.level.block.Blocks.STONE),"Mirror fixture floor was not retained");
            h.assertTrue(p.level().isEmptyBlock(bed.offset(3,0,-2))&&p.level().isEmptyBlock(bed.offset(3,1,-2)),"Mirror fixture approach is not clear");
            h.assertTrue(MissingParentsCat.mirror(p,s,bed),"Safe mirror route did not recover: "+n.getString("Blocker")+" / "+mirror+" / "+p.level().getBlockState(mirror)+" / loaded="+p.serverLevel().hasChunkAt(mirror));
            h.assertTrue(n.contains("Mirror")&&n.contains("MirrorApproach"),"Route not atomically saved");h.succeed();
        }finally{for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)p.serverLevel().removeBlock(bed.offset(x,-1,z),false);p.serverLevel().removeBlock(mirror,false);if(!forced)p.serverLevel().setChunkForced(chunk.x,chunk.z,false);remove(p);}});
    }
    @GameTest(template="empty") public static void skippedEscapeQueuesMissingRealmAndResumesBlockedCatClue(GameTestHelper h){
        var p=fixture(h);var s=OtherMotherLoop.scene(p);var level=p.serverLevel();var at=h.absolutePos(new BlockPos(2,3,2));
        var d=new com.coraline.otherworld.DoorData.Door();d.id=s.door;d.owner=s.owner;d.pos=at;d.dimension=level.dimension().location().toString();d.placed=true;d.exists=true;
        com.coraline.otherworld.DoorData.get(p.server).doors.put(d.id,d);
        level.setBlock(at,com.coraline.registry.ModBlocks.TINY_DOOR.get().defaultBlockState(),2);
        var tile=(com.coraline.otherworld.DoorBlockEntity)level.getBlockEntity(at);tile.doorId=s.door;tile.storyBricked=true;
        var n=MissingParentsCat.data(s);n.putBoolean("Started",true);n.putString("Phase","F");
        try{
            // A genuine F checkpoint already has its leased Cat. The old
            // fixture lacked one, inadvertently testing an unsafe skipped cast.
            var cat=ShowActors.acquire(p,s,"cat",level,net.minecraft.world.phys.Vec3.atBottomCenterOf(at.north(3)));
            h.assertTrue(cat!=null,"Canonical Cat lease could not be staged for the saved final clue");
            h.assertFalse(StoryTunnel.restore(p,s),"Unbuilt destination was declared ready");
            h.assertTrue(com.coraline.otherworld.RealmData.get(p.server).realms.containsKey(s.door)&&com.coraline.otherworld.OtherWorld.weaving(p.server),"Skipped escape did not automatically queue its destination");
            MissingParentsCat.cutsceneSafety(p,s);
            h.assertTrue(s.loop.story.getCompound("CutsceneSafety").getList("History",10).isEmpty(),"Watchdog claimed recovery before destination existed");
            // Complete the destination fixture, as the asynchronous realm worker does.
            com.coraline.otherworld.OtherWorld.cancelCopy(p.server,s.door);
            var r=com.coraline.otherworld.RealmData.get(p.server).realms.get(s.door);r.house=new BlockPos(132000,160,132000);r.doorPos=r.house.south(5);r.ready=true;
            FirstEscapeStory.data(s).put("BrickOriginal",net.minecraft.nbt.NbtUtils.writeBlockState(net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()));
            level.setBlock(at.south(),net.minecraft.world.level.block.Blocks.BRICKS.defaultBlockState(),2);
            h.assertTrue(StoryTunnel.restore(p,s),"Ready destination did not unblock the clue");
            h.assertTrue(level.getBlockState(at).getValue(com.coraline.block.TinyDoorBlock.OPEN)&&level.getBlockState(at).getValue(com.coraline.block.TinyDoorBlock.PORTAL)&&!tile.storyBricked&&!tile.storyPassageBlocked,"Restored entrance remained locked");
            var safety=new CompoundTag();safety.putString("Step",s.loop.event.getInt("Serial")+":F");safety.putInt("Ticks",100000);var tracks=new CompoundTag();tracks.put("post_escape_cat",safety);s.loop.story.put("CutsceneSafety",tracks);
            MissingParentsCat.cutsceneSafety(p,s);
            h.assertTrue(MissingParentsCat.phase(s)==MissingParentsCat.Phase.DEPART&&n.getBoolean("FFinished")&&MissingParentsCat.tunnelRestored(s),"Stalled clue did not advance after realm recovery");
            h.assertTrue(MissingParentsCat.finishForReturn(p,s)&&n.getBoolean("Complete"),"Cat departure still locked the usable return passage");
        }finally{
            var cat=ShowActors.actor(p,s,"cat");
            if(cat!=null)cat.discard();
            FirstEscapeScene.restore(p,s);
            level.removeBlock(at,false);level.removeBlock(at.south(),false);remove(p);
        }h.succeed();
    }
    @GameTest(template="empty") public static void catDepartureCannotCompleteAnUnrestoredPassage(GameTestHelper h){var p=fixture(h);var s=OtherMotherLoop.scene(p);try{MissingParentsCat.data(s).putBoolean("FFinished",true);h.assertFalse(MissingParentsCat.finishForReturn(p,s),"Missing tunnel was treated as a completed clue");h.assertFalse(MissingParentsCat.data(s).getBoolean("Complete"),"Failed completion changed progress");}finally{remove(p);}h.succeed();}
    private static ServerPlayer fixture(GameTestHelper h){var p=MissingParentsTeaGameTests.fixture(h);var s=OtherMotherLoop.scene(p);MissingParentsTeaGameTests.ready(p,s);FirstEscapeStory.data(s).putBoolean("WybieDeparted",true);return p;}
    private static void remove(ServerPlayer p){MissingParentsTeaGameTests.remove(p,OtherMotherLoop.scene(p));}
    @GameTest(template="empty") public static void catWaitsForActualTeaCompletionAndResolvedReward(GameTestHelper h){var p=fixture(h);var s=OtherMotherLoop.scene(p);try{h.assertFalse(MissingParentsCat.eligible(s),"Cat bypassed tea");MissingParentsTea.data(s).putBoolean("Completed",true);s.loop.story.putBoolean("SeeingStoneReceipt",true);s.loop.story.putBoolean("SeeingStonePending",true);h.assertFalse(MissingParentsCat.eligible(s),"Cat bypassed unresolved Seeing Stone");s.loop.story.putBoolean("SeeingStonePending",false);h.assertTrue(MissingParentsCat.eligible(s),"Completed tea did not unlock Cat");MissingParentsCat.data(s).putBoolean("TunnelRestored",true);var loaded=new VisitData.Scene();loaded.loop=OtherMotherLoopState.load(s.loop.save());h.assertTrue(MissingParentsCat.tunnelRestored(loaded),"Permanent tunnel lost on reload");}finally{remove(p);}h.succeed();}
    @GameTest(template="empty") public static void returnKeyIsDistinctSingletonAndNeverDroppedOnFullInventory(GameTestHelper h){var p=fixture(h);var s=OtherMotherLoop.scene(p);try{h.assertTrue(ModItems.LITTLE_DOOR_STORY_KEY.get()!=SecretsItems.KEY.get(),"Story key reuses cabinet key capability");for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));h.assertFalse(ParentRescueStory.grantKey(p,s),"Full inventory issued a dropped story key");h.assertFalse(s.loop.story.getBoolean("LittleDoorKeyIssued"),"Failed grant committed receipt");p.getInventory().setItem(4,ItemStack.EMPTY);h.assertTrue(ParentRescueStory.grantKey(p,s),"Canonical key unavailable");h.assertTrue(ParentRescueStory.grantKey(p,s),"Existing canonical key not recognized");int count=0;for(var item:p.getInventory().items)if(ParentRescueStory.key(item,s))count+=item.getCount();h.assertTrue(count==1,"Repeated request cloned key");s.loop.story.putString("LittleDoorKeyHolder","EVIL_OTHER_MOTHER");h.assertFalse(ParentRescueStory.grantKey(p,s),"Swallowed key was reissued");}finally{remove(p);}h.succeed();}
    @GameTest(template="empty") public static void rescueSceneryDoesNotReuseDiscardedFirstEscapeIdentities(GameTestHelper h){var p=fixture(h);var s=OtherMotherLoop.scene(p);try{FirstEscapeStory.data(s).putLong("House",new BlockPos(100,100,100).asLong());var n=ParentRescueStory.data(s);n.putBoolean("Started",true);var scene=new CompoundTag();scene.putLong("House",new BlockPos(400,100,400).asLong());n.put("Scenery",scene);h.assertTrue(FirstEscapeScene.room(s).getX()>350,"Rescue reused old First Escape scenery context");h.assertFalse(scene.contains("BugChair"),"Old discarded furniture identity leaked into rescue");}finally{remove(p);}h.succeed();}
    @GameTest(template="empty") public static void allTwentyOriginalContinuationRecordingsAreRegisteredAndMeasured(GameTestHelper h){int count=0;for(var entry:Map.of("missing_parents_cat_","abcdef","parent_rescue_","abcdefghijklmn").entrySet())for(char letter:entry.getValue().toCharArray()){String id=entry.getKey()+letter;h.assertTrue(VisitAudio.SOUNDS.containsKey(id),"Unregistered original "+id);h.assertTrue(ShowRecordings.duration(id)>0,"Missing measured original "+id);count++;}h.assertTrue(count==20,"Incomplete story recordings");h.succeed();}
    @GameTest(template="empty") public static void continuationDebugUsesGuardedOwnerControls(GameTestHelper h){for(String section:List.of("cat_story","parent_rescue")){h.assertTrue(NumberedDebugControls.SECTIONS.contains(section),"Missing debug section");for(String id:List.of("state","recover"))h.assertTrue(NumberedDebugControls.find(section,id)!=null,"Missing guarded control");}h.assertTrue(NumberedDebugControls.find("parent_rescue","enter")!=null,"Return cannot be tested through actual entrance");h.succeed();}
}
