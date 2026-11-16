package com.coraline.feast;

import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class GhostEscapeEpilogueGameTests {
    @GameTest(template="empty") public static void corruptHandReferenceCannotTransformAnUnrelatedOwnedProp(GameTestHelper h){
        var p=AcceptedBranchPlayers.fixture(h);var s=VisitData.get(p.server).scenes.get(p.getUUID());AcceptedButtonEyes.data(s).putBoolean("Started",true);
        var seat=CinematicPropEntity.TYPE.get().create(p.serverLevel());seat.configure(s,CinematicPropEntity.SEAT,0);seat.setPos(p.position());p.serverLevel().addFreshEntity(seat);
        var canonical=ParentRescueStory.scenery(s);canonical.putUUID("RescueSeveredHand",seat.getUUID());canonical.putLong("RescueSeveredHandPosition",seat.blockPosition().asLong());canonical.putString("RescueSeveredHandDimension",p.level().dimension().location().toString());
        try{
            var before=seat.position();h.assertTrue(RescueProps.ensure(p,s,"SeveredHand",RescueProps.HAND,0,p.position().add(10,0,0))==null,"Corrupt Hand receipt authorized an unrelated rig");
            h.assertTrue(seat.kind()==CinematicPropEntity.SEAT&&seat.position().equals(before)&&canonical.getUUID("RescueSeveredHand").equals(seat.getUUID()),"Hand recovery rewrote or moved an unrelated owned prop");
        }finally{seat.discard();AcceptedBranchPlayers.remove(p,s);}h.succeed();
    }
    @GameTest(template="empty") public static void recordedUnloadedRescuePropCannotBeReplacedAcrossBranchContexts(GameTestHelper h){
        var p=AcceptedBranchPlayers.fixture(h);var s=VisitData.get(p.server).scenes.get(p.getUUID());AcceptedButtonEyes.data(s).putBoolean("Started",true);
        var canonical=ParentRescueStory.scenery(s);var id=java.util.UUID.randomUUID();canonical.putUUID("RescueSeveredHand",id);canonical.putLong("RescueSeveredHandPosition",p.blockPosition().asLong());canonical.putString("RescueSeveredHandDimension",p.level().dimension().location().toString());
        try{
            h.assertTrue(ParentRescueStory.data(s).getCompound("Scenery")==canonical,"Rescue scenery writes were detached from saved quest data");
            h.assertTrue(RescueProps.ensure(p,s,"SeveredHand",RescueProps.HAND,0,p.position())==null,"Missing recorded original Hand caused a substitute spawn");
            h.assertTrue(canonical.getUUID("RescueSeveredHand").equals(id)&&!AcceptedButtonEyesProgress.scenery(s).hasUUID("RescueSeveredHand"),"Branch context overwrote the canonical unloaded Hand UUID");
        }finally{AcceptedBranchPlayers.remove(p,s);}h.succeed();
    }
    @GameTest(template="empty") public static void branchMigrationReusesOriginalGhostAndRetiresOnlyItsRecordedDuplicate(GameTestHelper h){
        var p=AcceptedBranchPlayers.fixture(h);var s=VisitData.get(p.server).scenes.get(p.getUUID());var level=p.serverLevel();
        AcceptedButtonEyes.data(s).putBoolean("Started",true);var first=FirstEscapeStory.data(s);var branch=AcceptedButtonEyesProgress.scenery(s);
        var original=CinematicPropEntity.TYPE.get().create(level);var duplicate=CinematicPropEntity.TYPE.get().create(level);
        original.configure(s,CinematicPropEntity.GHOST_CHILD,0);duplicate.configure(s,CinematicPropEntity.GHOST_CHILD,0);original.setPos(p.position());duplicate.setPos(p.position().add(2,0,0));level.addFreshEntity(original);level.addFreshEntity(duplicate);
        first.putUUID("Ghost0",original.getUUID());first.putLong("Ghost0Position",original.blockPosition().asLong());first.putLong("Prison",original.blockPosition().asLong());
        branch.putUUID("Ghost0",duplicate.getUUID());branch.putLong("Ghost0Position",duplicate.blockPosition().asLong());
        try{
            var before=original.position();FirstEscapeScene.reconcileOriginalGhostReferences(p,s);
            h.assertTrue(branch.getUUID("Ghost0").equals(original.getUUID())&&original.canonicalGhost(s),"Branch transition changed the original ghost's identity");
            h.assertTrue(s.loop.story.getCompound("RetiredCanonicalGhosts").getInt(duplicate.getUUID().toString())==0&&!s.loop.story.getCompound("RetiredCanonicalGhosts").contains(original.getUUID().toString()),"Migration retired the original rather than the recorded duplicate");
            duplicate.tick();h.assertTrue(duplicate.isRemoved()&&!original.isRemoved(),"Duplicate retirement removed the wrong ghost actor");
            var same=FirstEscapeScene.ensure(p,s,"Ghost0",CinematicPropEntity.GHOST_CHILD,0,level,p.position().add(10,0,0));
            h.assertTrue(same==original&&original.position().equals(before),"Ensuring an original ghost cloned or teleported it");
            RescueEyes.ledger(s).putBoolean("Released",true);
            h.assertTrue(FirstEscapeScene.ensure(p,s,"Ghost1",CinematicPropEntity.GHOST_CHILD,1,level,p.position())==null&&!branch.hasUUID("Ghost1"),"A released soul was respawned by a later prison reveal");
        }finally{original.discard();duplicate.discard();AcceptedBranchPlayers.remove(p,s);}h.succeed();
    }
    private GhostEscapeEpilogueGameTests(){}
}
