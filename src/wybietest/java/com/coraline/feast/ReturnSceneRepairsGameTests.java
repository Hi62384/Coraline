package com.coraline.feast;
import com.coraline.registry.*;
import com.coraline.parents.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class ReturnSceneRepairsGameTests {
    @GameTest(template="empty") public static void floatingVisitTravelReturnsOwnerOnlyAfterVisit(GameTestHelper h){
        var p=MissingParentsTeaGameTests.fixture(h);var s=OtherMotherLoop.scene(p);var origin=p.blockPosition();var center=origin.south(48);var level=p.serverLevel();
        var startFloor=level.getBlockState(origin.below());var front=center.north(10);var arrivalFloor=level.getBlockState(front.below());
        try{
            MissingParentsTeaGameTests.ready(p,s);MissingParentsTeaGameTests.owner(p,s);var visit=MissingParentsTea.data(s);
            visit.putLong("House",center.asLong());visit.putBoolean("Active",true);visit.putBoolean("DetailedTeaHouse",true);visit.putBoolean("FloatingLanding",true);visit.putBoolean("Built",true);visit.putString("StoryPhase","WAITING_FOR_KNOCK");
            level.setBlock(origin.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);level.setBlock(front.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
            h.assertTrue(TeaVisitTravel.travel(p)==1&&p.distanceToSqr(front.getCenter())<2,"Invitation did not reach the floating landing");
            h.assertTrue(TeaVisitTravel.leave(p)==0,"Return link bypassed the visit");
            visit.putBoolean("ExitUnlocked",true);visit.putString("StoryPhase","WAITING_FOR_EXIT");
            h.assertTrue(TeaVisitTravel.leave(p)==1&&p.distanceToSqr(origin.getCenter())<4,"Floating house did not provide a safe return");
        }finally{
            level.setBlock(origin.below(),startFloor,2);level.setBlock(front.below(),arrivalFloor,2);MissingParentsTeaGameTests.remove(p,s);
        }h.succeed();
    }
    @GameTest(template="empty") public static void teaHouseAllowsFloatingAndUnevenSitesWithoutOverwritingInventories(GameTestHelper h){
        var p=MissingParentsTeaGameTests.fixture(h);var s=OtherMotherLoop.scene(p);var at=p.blockPosition().south(48);var level=p.serverLevel();
        var originals=com.coraline.bobinsky.BobinskyHouse.snapshot(level,at,com.coraline.tea.TeaHouse.RX,com.coraline.tea.TeaHouse.RZ+2);
        try{
            for(var pos:net.minecraft.core.BlockPos.betweenClosed(at.offset(-8,-1,-11),at.offset(8,9,11)))level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
            h.assertTrue(MissingParentsTeaPlacement.safe(p,at),"A floating house still requires ground support");
            level.setBlock(at.offset(-3,2,2),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
            level.setBlock(at.offset(3,-1,-2),net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(),2);
            h.assertTrue(MissingParentsTeaPlacement.safe(p,at),"Uneven terrain still blocks the visit");
            level.setBlock(at,net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(),2);
            h.assertFalse(MissingParentsTeaPlacement.safe(p,at),"Fallback would overwrite a player's inventory");
        }finally{
            var journal=new net.minecraft.nbt.CompoundTag();journal.putLong("House",at.asLong());journal.put("Originals",originals);
            com.coraline.bobinsky.BobinskyHouse.restore(level,journal);MissingParentsTeaGameTests.remove(p,s);
        }h.succeed();
    }
    @GameTest(template="empty") public static void keyIsShownBeforeLockAndStaysVisibleUntilSwallow(GameTestHelper h){
        var p=MissingParentsTeaGameTests.fixture(h);var s=OtherMotherLoop.scene(p);var n=ParentRescueStory.data(s);var mother=ModEntities.MOM.get().create(p.serverLevel());
        try{n.putBoolean("Started",true);n.putBoolean("KeyTaken",true);n.putInt("KeyStep",1);mother.setItemSlot(EquipmentSlot.MAINHAND,ParentRescueStory.boundKey(s));
            for(int i=0;i<49;i++)ParentRescueStory.keyScene(p,s,mother,i);
            h.assertTrue(n.getInt("KeyStep")==1&&!n.getBoolean("DoorLocked")&&ParentRescueStory.key(mother.getMainHandItem(),s),"Key vanished or lock happened before showing the captured original");
            ParentRescueStory.keyScene(p,s,mother,50);h.assertTrue(n.getInt("KeyStep")==2,"Presentation never releases the lock approach");
            n.putInt("KeyStep",4);n.putInt("KeyTicks",0);
            for(int i=0;i<45;i++)ParentRescueStory.keyScene(p,s,mother,i);
            h.assertTrue(n.getInt("KeyStep")==5&&ParentRescueStory.key(mother.getMainHandItem(),s),"Key disappears before reaching her mouth");
            for(int i=0;i<34;i++)ParentRescueStory.keyScene(p,s,mother,i);
            h.assertFalse(mother.getMainHandItem().isEmpty(),"Key hidden before swallowing cue");ParentRescueStory.keyScene(p,s,mother,35);
            h.assertTrue(mother.getMainHandItem().isEmpty(),"Key still visible after swallowing cue");
        }finally{mother.discard();MissingParentsTeaGameTests.remove(p,s);}h.succeed();
    }
    @GameTest(template="empty") public static void bedRefundRevokesOnlyOldBedCopies(GameTestHelper h){
        var p=MissingParentsTeaGameTests.fixture(h);var s=OtherMotherLoop.scene(p);var f=new FamilyData.Family();f.owner=p.getUUID();FamilyData.get(p.server).families.put(f.id,f);
        try{var old=ParentsBedItem.forFamily(f.id);var ordinary=new ItemStack(Items.STONE);ordinary.getOrCreateTagElement("BlockEntityTag").putUUID("Family",f.id);
            h.assertTrue(ParentsBedSafety.recover(p,f,false).startsWith("Your parents' lost bed"),"Missing bed was not recovered");
            ParentsBedSafety.retireRevoked(old,p.serverLevel());ParentsBedSafety.retireRevoked(ordinary,p.serverLevel());
            h.assertTrue(old.isEmpty()&&ordinary.is(Items.STONE),"Revocation kept a stale bed or deleted an unrelated item");
            int revision=f.bedCustody.getInt("Revision");var current=p.getInventory().items.stream().filter(i->i.is(ModItems.PARENTS_BED.get())).findFirst().orElseThrow();
            // The direct caller must be checked even before player-list registration.
            var item=new net.minecraft.world.entity.item.ItemEntity(p.serverLevel(),p.getX(),p.getY(),p.getZ(),current.copy());p.serverLevel().addFreshEntity(item);
            try{ParentsBedSafety.recover(p,f,false);h.assertTrue(f.bedCustody.getInt("Revision")==revision,"Existing current bed caused another refund");}finally{item.discard();}
        }finally{MissingParentsTeaGameTests.remove(p,s);}h.succeed();
    }
    @GameTest(template="empty") public static void pianoScoreSurvivesRevealHandoffAndStopsAtCaneUse(GameTestHelper h){
        for(String phase:java.util.List.of("P","Q","R","S"))h.assertTrue(LongFaceMusicPlan.stage(phase,"")==1,"Dialogue score is not quiet");
        h.assertTrue(LongFaceMusicPlan.stage("STUDY_REVEAL_COMPLETE","")==2&&LongFaceMusicPlan.stage("","FIND_CANE")==2,"Reveal handoff cuts the score");
        h.assertTrue(LongFaceMusicPlan.stage("","CANE_COLLECTED")==2,"Picking up the cane cuts the score");
        for(String stale:java.util.List.of("","P","Q","R","S","STUDY_REVEAL_COMPLETE")){
            h.assertTrue(LongFaceMusicPlan.stage(stale,"CANE_COLLECTED")==2,"Delayed study packet cuts the carried-cane score");
            h.assertTrue(LongFaceMusicPlan.stage(stale,"PRY_DOOR")==0,"Delayed study packet keeps score playing after cane use");
            h.assertTrue(LongFaceMusicPlan.stage(stale,"ROOM_DARK")==0,"Score restarts after opening the door");
        }
        h.assertTrue(LongFaceMusicPlan.gain(1)<LongFaceMusicPlan.gain(2),"Dialogue ducking missing");h.succeed();
    }
    @GameTest(template="empty") public static void parentsBedLocksAtTunnelExitAndUnlocksAfterStory(GameTestHelper h){
        var p=MissingParentsTeaGameTests.fixture(h);var s=OtherMotherLoop.scene(p);var family=new FamilyData.Family();family.owner=p.getUUID();
        FamilyData.get(p.server).families.put(family.id,family);var at=h.absolutePos(new net.minecraft.core.BlockPos(2,2,2));
        var facing=net.minecraft.core.Direction.NORTH;var state=ModBlocks.PARENTS_BED.get().defaultBlockState().setValue(ParentsBedBlock.FACING,facing);
        try{
            p.serverLevel().setBlock(at,state,3);var bed=(ParentsBedEntity)p.serverLevel().getBlockEntity(at);bed.familyId=family.id;family.bed=at;
            for(int part=1;part<4;part++)p.serverLevel().setBlock(ParentsBedBlock.partPos(at,facing,part),state.setValue(ParentsBedBlock.PART,part),3);
            var escape=FirstEscapeStory.data(s);escape.putBoolean("Started",true);escape.putString("Phase","MISSING_A");
            for(int part=0;part<4;part++){
                var cell=ParentsBedBlock.partPos(at,facing,part);
                h.assertTrue(ParentsBedSafety.protectedAt(p.serverLevel(),cell),"A bed part is breakable immediately after tunnel exit");
                h.assertFalse(p.serverLevel().destroyBlock(cell,true),"Direct destruction removed a protected bed part");
                h.assertFalse(p.serverLevel().setBlock(cell,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3),"Direct replacement removed a protected bed part");
            }
            escape.putBoolean("Complete",true);escape.putString("Phase","ESCAPE_COMPLETE");
            h.assertTrue(ParentsBedSafety.protectedAt(p.serverLevel(),at),"Bed unlocked during missing-parents story");
            var continuation=RescueChapter.data(s);continuation.putBoolean("Started",true);continuation.putString("Phase","COMPLETE");
            h.assertFalse(ParentsBedSafety.protectedAt(p.serverLevel(),at),"Finishing the story left the bed locked");
            continuation.putBoolean("Started",false);s.loop.story.putBoolean("BeldamDefeatedByGhostEscape",true);
            h.assertFalse(ParentsBedSafety.protectedAt(p.serverLevel(),at),"Alternate completed story left the bed locked");
            h.assertTrue(p.serverLevel().destroyBlock(at,false),"Bed still cannot be removed after finishing story");
        }finally{
            FirstEscapeStory.data(s).putBoolean("Started",false);FirstEscapeStory.data(s).putBoolean("Complete",false);
            RescueChapter.data(s).putBoolean("Started",true);RescueChapter.data(s).putString("Phase","COMPLETE");
            p.serverLevel().removeBlock(at,false);FamilyData.get(p.server).families.remove(family.id);MissingParentsTeaGameTests.remove(p,s);
        }h.succeed();
    }
}
