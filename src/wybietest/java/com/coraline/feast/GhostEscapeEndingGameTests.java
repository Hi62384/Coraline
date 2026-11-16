package com.coraline.feast;

import java.util.UUID;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class GhostEscapeEndingGameTests {
    @GameTest(template="empty") public static void physicalSealRequiresOriginalDoorAndKeyWithoutCompletingOtherQuests(GameTestHelper h){
        var p=AcceptedBranchPlayers.fixture(h);var s=VisitData.get(p.server).scenes.get(p.getUUID());var level=p.serverLevel();var at=p.blockPosition().west();
        var old=level.getBlockState(at);var floor=level.getBlockState(at.below());
        var ghost=new CompoundTag();GhostVictimProgress.begin(ghost,"ACCEPTED_PROPOSAL");ghost.putUUID("Owner",s.owner);ghost.putUUID("Door",s.door);ghost.putUUID("Session",UUID.randomUUID());
        GhostVictimProgress.newVictim(ghost,UUID.randomUUID(),new CompoundTag());AcceptedButtonEyes.data(s).put(GhostVictimProgress.KEY,ghost);
        var escape=GhostEscapeProgress.data(ghost);escape.putUUID("Attempt",UUID.randomUUID());escape.putUUID("Victim",GhostVictimProgress.child(ghost).getUUID("Id"));escape.putUUID("Session",ghost.getUUID("Session"));
        for(String flag:java.util.List.of("WagerWon","QuestEyesSecured","RestorationCommitted","ChildDepartureWitnessed","FarewellComplete","AgencyRegistrationSaved"))escape.putBoolean(flag,true);
        UUID escrow=UUID.randomUUID();escape.putUUID("OriginalEscrow",escrow);escape.putUUID("RestoredEscrow",escrow);GhostEscapeProgress.move(escape,GhostEscapeProgress.Phase.COMPLETE,0);
        GhostEscapeEndingProgress.begin(ghost,s.owner,s.door);
        var source=new com.coraline.otherworld.DoorData.Door();source.id=s.door;source.owner=s.owner;source.pos=at;source.dimension=level.dimension().location().toString();source.placed=true;source.activeUntil=100;
        com.coraline.otherworld.DoorData.get(p.server).doors.put(s.door,source);
        level.setBlock(at.below(),Blocks.STONE.defaultBlockState(),2);
        level.setBlock(at,com.coraline.registry.ModBlocks.TINY_DOOR.get().defaultBlockState().setValue(com.coraline.block.TinyDoorBlock.FACING,net.minecraft.core.Direction.EAST).setValue(com.coraline.block.TinyDoorBlock.OPEN,true).setValue(com.coraline.block.TinyDoorBlock.PORTAL,true),2);
        var tile=(com.coraline.otherworld.DoorBlockEntity)level.getBlockEntity(at);tile.doorId=s.door;
        try{
            var key=ParentRescueStory.boundKey(s);var foreign=key.copy();foreign.getOrCreateTag().putUUID("StoryKeyOwner",UUID.randomUUID());p.getInventory().setItem(0,foreign);
            h.assertTrue(GhostEscapeEnding.seal(p,s,at)&&level.getBlockState(at).getValue(com.coraline.block.TinyDoorBlock.OPEN),"Foreign key sealed the original door");
            p.getInventory().setItem(0,key);tile.doorId=UUID.randomUUID();
            h.assertFalse(GhostEscapeEnding.seal(p,s,at),"Saved coordinates authorized sealing another door identity");tile.doorId=s.door;
            h.assertTrue(GhostEscapeEnding.seal(p,s,at),"Real door and original key could not seal the completed escape");
            var actual=level.getBlockState(at);h.assertFalse(actual.getValue(com.coraline.block.TinyDoorBlock.OPEN)||actual.getValue(com.coraline.block.TinyDoorBlock.PORTAL),"Sealed door remained open or active");
            h.assertTrue(ParentRescueStory.key(p.getInventory().getItem(0),s)&&p.getInventory().getItem(0).getCount()==1,"Sealing consumed or duplicated the original key");
            var chapter=RescueChapter.data(s);h.assertTrue(chapter.getBoolean("PermanentlySealed")&&chapter.getBoolean("KeyRecovered")&&ParentRescueStory.data(s).getBoolean("DoorLocked")&&source.activeUntil==0,"Physical sealing did not reach canonical door custody");
            h.assertFalse(chapter.getBoolean("HandSevered")||chapter.getBoolean("ParentsReturned")||chapter.getBoolean("FarewellComplete")||RescueEyes.ledger(s).getBoolean("Released"),"Door sealing fabricated an unfinished reunion, soul farewell or hand encounter");
            h.assertTrue(GhostEscapeEnding.seal(p,s,at)&&p.getInventory().getItem(0).getCount()==1,"Repeated sealed-door click duplicated its key");
            p.getInventory().setItem(0,net.minecraft.world.item.ItemStack.EMPTY);source.activeUntil=100;
            // Simulate a restart after the story receipt was saved but before
            // the opened door chunk caught up with it.
            s.loop=OtherMotherLoopState.load(s.loop.save());
            ghost=AcceptedButtonEyes.data(s).getCompound(GhostVictimProgress.KEY);
            h.assertTrue(GhostEscapeEndingProgress.existing(ghost,s.owner,s.door)!=null,"Sealing receipt lost its owner, door, session or attempt on reload");
            level.setBlock(at,actual.setValue(com.coraline.block.TinyDoorBlock.OPEN,true).setValue(com.coraline.block.TinyDoorBlock.PORTAL,true),2);p.tickCount=20;
            h.assertTrue(GhostEscapeEnding.tick(p,s,ghost)&&!level.getBlockState(at).getValue(com.coraline.block.TinyDoorBlock.OPEN)&&!level.getBlockState(at).getValue(com.coraline.block.TinyDoorBlock.PORTAL)&&source.activeUntil==0,"Saved sealing receipt did not reconcile a stale open chunk");
            h.assertTrue(p.getInventory().getItem(0).isEmpty(),"Sealing reconciliation issued a replacement for a stored key");
            h.assertTrue(GhostEscapeEnding.tick(p,s,ghost)&&p.getInventory().getItem(0).isEmpty(),"Repeated reconciliation issued a replacement key");
        }finally{level.setBlock(at,old,2);level.setBlock(at.below(),floor,2);AcceptedBranchPlayers.remove(p,s);}
        h.succeed();
    }
    private GhostEscapeEndingGameTests(){}
}
