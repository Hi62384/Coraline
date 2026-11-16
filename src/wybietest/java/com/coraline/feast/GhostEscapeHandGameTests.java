package com.coraline.feast;

import java.util.UUID;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class GhostEscapeHandGameTests {
    @GameTest(template="empty") public static void originalDoorCutsTheSameHandAndActualCollisionUnlocksTheJourney(GameTestHelper h){
        var p=AcceptedBranchPlayers.fixture(h);var s=VisitData.get(p.server).scenes.get(p.getUUID());var level=p.serverLevel();
        s.mom=UUID.randomUUID();var feet=p.blockPosition();var at=feet.west();var old=level.getBlockState(at);var floor=level.getBlockState(feet.below());var doorFloor=level.getBlockState(at.below());
        var ghost=new CompoundTag();GhostVictimProgress.begin(ghost,"ACCEPTED_PROPOSAL");ghost.putUUID("Owner",s.owner);ghost.putUUID("Door",s.door);ghost.putUUID("Session",UUID.randomUUID());
        GhostVictimProgress.newVictim(ghost,UUID.randomUUID(),new CompoundTag());AcceptedButtonEyes.data(s).put(GhostVictimProgress.KEY,ghost);
        var escape=GhostEscapeProgress.data(ghost);escape.putUUID("Attempt",UUID.randomUUID());escape.putUUID("Victim",GhostVictimProgress.child(ghost).getUUID("Id"));escape.putUUID("Session",ghost.getUUID("Session"));
        for(String flag:java.util.List.of("WagerWon","QuestEyesSecured","RestorationCommitted","ChildDepartureWitnessed","FarewellComplete","AgencyRegistrationSaved"))escape.putBoolean(flag,true);
        UUID escrow=UUID.randomUUID();escape.putUUID("OriginalEscrow",escrow);escape.putUUID("RestoredEscrow",escrow);GhostEscapeProgress.move(escape,GhostEscapeProgress.Phase.COMPLETE,0);
        GhostEscapeEndingProgress.begin(ghost,s.owner,s.door);GhostEscapeEndingProgress.sealed(ghost,s.owner,s.door,true,true,true);
        var chapter=RescueChapter.data(s);GhostEscapeEndingProgress.beginEpilogue(ghost,s.owner,s.door,chapter,UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),true,true);
        chapter.putString("Phase",RescueChapter.Phase.COMPLETE.name());for(String flag:java.util.List.of("ParentsReturned","FarewellComplete","FarewellCatDeparted","PermanentlySealed"))chapter.putBoolean(flag,true);
        RescueEyes.ledger(s).putBoolean("Released",true);ParentRescueStory.data(s).putBoolean("DoorLocked",true);GhostEscapeEndingProgress.finishEpilogue(ghost,s.owner,s.door,chapter,true);
        var source=new com.coraline.otherworld.DoorData.Door();source.id=s.door;source.owner=s.owner;source.pos=at;source.dimension=level.dimension().location().toString();source.placed=true;
        com.coraline.otherworld.DoorData.get(p.server).doors.put(s.door,source);
        level.setBlock(at.below(),Blocks.STONE.defaultBlockState(),2);level.setBlock(feet.below(),Blocks.STONE.defaultBlockState(),2);
        level.setBlock(at,com.coraline.registry.ModBlocks.TINY_DOOR.get().defaultBlockState().setValue(com.coraline.block.TinyDoorBlock.FACING,net.minecraft.core.Direction.EAST),2);
        ((com.coraline.otherworld.DoorBlockEntity)level.getBlockEntity(at)).doorId=s.door;
        var hand=RescueProps.ensure(p,s,"SeveredHand",RescueProps.HAND,0,Vec3.atBottomCenterOf(feet).add(0,.65,0));var id=hand.getUUID();
        var tag=hand.getPersistentData();tag.putUUID("GhostHandOwner",s.owner);tag.putUUID("GhostHandDoor",s.door);tag.putUUID("GhostHandSession",ghost.getUUID("Session"));tag.putUUID("GhostHandAttempt",escape.getUUID("Attempt"));tag.putUUID("GhostHandMother",s.mom);
        GhostEscapeHandProgress.begin(ghost,s.owner,s.door,s.mom,id,true,false);p.setPos(Vec3.atBottomCenterOf(feet.east()));
        try{
            h.assertFalse(PalaceJourney.prerequisites(s).ready(),"Reaching Hand prematurely unlocked key disposal");
            GhostEscapeSeveredHand.closeOnHand(p,s,ghost);
            h.assertTrue(GhostEscapeHandProgress.existing(ghost,s.owner,s.door,s.mom).getString("Phase").equals("REACHING"),"Absent key cut the Hand");
            p.getInventory().setItem(0,ParentRescueStory.boundKey(s));
            h.assertTrue(GhostEscapeSeveredHand.closeOnHand(p,s,ghost)&&GhostEscapeHandProgress.existing(ghost,s.owner,s.door,s.mom).getString("Phase").equals("FALLING"),"Original door/key could not cut the bound Hand");
            h.assertFalse(chapter.getBoolean("HandSevered"),"Cut click fabricated landing");
            for(int tick=0;tick<30&&!chapter.getBoolean("HandSevered");tick++)GhostEscapeSeveredHand.tick(p,s,ghost);
            h.assertTrue(hand.onGround()&&chapter.getBoolean("HandSevered")&&PalaceJourney.prerequisites(s).ready(),"Actual collision landing did not unlock the existing well journey");
            h.assertTrue(RescueProps.find(p,s,"SeveredHand")==hand&&ParentRescueStory.scenery(s).getUUID("RescueSeveredHand").equals(id)&&ParentRescueStory.key(p.getInventory().getItem(0),s)&&p.getInventory().getItem(0).getCount()==1,"Handoff replaced the Hand or consumed/duplicated the original key");
            var landed=hand.position();GhostEscapeSeveredHand.tick(p,s,ghost);h.assertTrue(hand.position().equals(landed),"Completed handoff repositioned the canonical Hand");
        }finally{hand.discard();level.setBlock(at,old,2);level.setBlock(feet.below(),floor,2);level.setBlock(at.below(),doorFloor,2);AcceptedBranchPlayers.remove(p,s);}h.succeed();
    }
    private GhostEscapeHandGameTests(){}
}
