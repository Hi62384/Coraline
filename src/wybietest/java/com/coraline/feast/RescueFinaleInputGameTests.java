package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import java.util.UUID;

/** Real owner/input/custody gates; no fabricated audio acknowledgements. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class RescueFinaleInputGameTests {
    private static ServerPlayer player(GameTestHelper h){var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"FinaleInputQA"));p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.stage=VisitData.Stage.DONE;VisitData.get(p.server).scenes.put(s.owner,s);ParentRescueStory.data(s).putBoolean("Started",true);RescueChapter.data(s).putBoolean("Started",true);return p;}
    private static void clean(ServerPlayer p){var s=OtherMotherLoop.scene(p);DoorData.get(p.server).doors.remove(s.door);VisitData.get(p.server).scenes.remove(s.owner);}
    @GameTest(template="empty") public static void manualLockRequiresExactDoorRecoveredOwnersKeyAndActualExitAndSurvivesReload(GameTestHelper h){
        var p=player(h);var other=player(h);var s=OtherMotherLoop.scene(p);var foreign=OtherMotherLoop.scene(other);var at=h.absolutePos(new BlockPos(2,3,2));var original=h.getLevel().getBlockState(at);var d=new DoorData.Door();d.id=s.door;d.owner=s.owner;d.pos=at;d.placed=true;d.dimension=h.getLevel().dimension().location().toString();DoorData.get(p.server).doors.put(d.id,d);var n=RescueChapter.data(s);n.putString("Phase","LOCK_DOOR");RescueChapter.data(foreign).putString("Phase","LOCK_DOOR");p.setPos(Vec3.atCenterOf(at));other.setPos(p.position());h.getLevel().setBlock(at,ModBlocks.TINY_DOOR.get().defaultBlockState().setValue(com.coraline.block.TinyDoorBlock.OPEN,true),2);
        var tile=h.getLevel().getBlockEntity(at);h.assertTrue(tile instanceof DoorBlockEntity,"Authenticated original Little Door tile fixture missing");((DoorBlockEntity)tile).doorId=d.id;tile.setChanged();
        try{
            var key=ParentRescueStory.boundKey(s);key.getOrCreateTag().putString("Keepsake","Original key metadata");other.getInventory().add(key.copy());h.assertFalse(BeldamFinale.block(other,at),"Traded owner's key let another player operate their door");h.assertFalse(n.getBoolean("PermanentlySealed"),"Foreign click sealed the owner's story");
            p.getInventory().add(ParentRescueStory.boundKey(foreign));n.putBoolean("CrawlReachedOverworld",true);n.putBoolean("KeyRecovered",true);BeldamFinale.block(p,at);h.assertFalse(n.getBoolean("PermanentlySealed"),"Unrelated key satisfied the owner's lock");p.getInventory().clearContent();p.getInventory().add(key.copy());n.remove("CrawlReachedOverworld");BeldamFinale.block(p,at);h.assertTrue(RescueChapter.phase(s)==RescueChapter.Phase.LOCK_DOOR&&!n.getBoolean("PermanentlySealed"),"Key click bypassed actual crawl exit");n.putBoolean("CrawlReachedOverworld",true);n.remove("KeyRecovered");BeldamFinale.block(p,at);h.assertFalse(n.getBoolean("PermanentlySealed"),"Unrecovered key satisfied the finale");
            n.putBoolean("KeyRecovered",true);h.assertFalse(BeldamFinale.block(p,at.east()),"Clicking an unrelated block operated the door");h.assertTrue(BeldamFinale.block(p,at)&&n.getBoolean("PermanentlySealed")&&RescueChapter.phase(s)==RescueChapter.Phase.BANGING,"Actual owner key click did not seal and start banging");h.assertTrue(!h.getLevel().getBlockState(at).getValue(com.coraline.block.TinyDoorBlock.OPEN)&&!h.getLevel().getBlockState(at).getValue(com.coraline.block.TinyDoorBlock.PORTAL),"Manual lock left the passage open");
            s.loop=OtherMotherLoopState.load(s.loop.save());h.assertTrue(RescueChapter.data(s).getBoolean("PermanentlySealed")&&ParentRescueStory.data(s).getBoolean("DoorLocked")&&RescueChapter.phase(s)==RescueChapter.Phase.BANGING,"Seal/checkpoint lost on reload");h.assertFalse(BeldamFinale.block(p,at),"Repeated click replayed the seal transaction");h.assertTrue(p.getInventory().getItem(0).getTag().getString("Keepsake").equals("Original key metadata"),"Manual lock destroyed or replaced the recovered original key");
            h.assertTrue(BeldamFinale.originalExitReady(p,s),"An unchanged real sealed door was not accepted after reload");
            var reOpened=h.getLevel().getBlockState(at).setValue(com.coraline.block.TinyDoorBlock.OPEN,true);
            h.getLevel().setBlock(at,reOpened,2);
            h.assertFalse(BeldamFinale.originalExitReady(p,s),"A reopened saved door was incorrectly accepted as permanently sealed");
            h.assertTrue(RescueChapter.phase(s)==RescueChapter.Phase.LOCK_DOOR
                    &&!RescueChapter.data(s).getBoolean("PermanentlySealed")
                    &&!ParentRescueStory.data(s).getBoolean("DoorLocked"),
                    "Stale seal did not restore the manual original key interaction");
            h.assertTrue(BeldamFinale.block(p,at)&&BeldamFinale.originalExitReady(p,s)
                    &&RescueChapter.phase(s)==RescueChapter.Phase.BANGING,
                    "Re-locking the original door after a stale seal failed to resume the ending");
        }finally{h.getLevel().setBlock(at,original,2);clean(p);clean(other);}h.succeed();
    }
    @GameTest(template="empty") public static void eyeChoiceNeedsPhysicalAuthenticEyesAndFreshOwnersNonceAcrossReload(GameTestHelper h){
        var p=player(h);var other=player(h);var s=OtherMotherLoop.scene(p);var n=RescueChapter.data(s);n.putString("Phase","SHOW_EYES");var nonce=UUID.randomUUID();n.putUUID("Input",nonce);RescueChapter.input(other,nonce,"show_eyes");RescueChapter.input(p,UUID.randomUUID(),"show_eyes");h.assertTrue(RescueChapter.phase(s)==RescueChapter.Phase.SHOW_EYES&&!n.contains("LastInput"),"Foreign/stale nonce consumed an owner action");
        RescueChapter.input(p,nonce,"show_eyes");h.assertTrue(RescueChapter.phase(s)==RescueChapter.Phase.SHOW_EYES,"Empty inventory bypassed physical eye presentation");
        for(int i=0;i<3;i++){var eye=RescueEyes.eye(s,i);eye.putBoolean("Defeated",true);eye.putBoolean("Discovered",true);h.assertTrue(RescueEyes.claim(p,s,i),"Could not award actual owned eye fixture");}
        s.loop=OtherMotherLoopState.load(s.loop.save());h.runAfterDelay(4,()->{try{RescueChapter.input(p,nonce,"show_eyes");h.assertTrue(RescueChapter.phase(s)==RescueChapter.Phase.FINAL_D&&!RescueChapter.data(s).getUUID("Input").equals(nonce),"Reloaded authentic presentation failed or retained its old nonce");RescueChapter.input(p,nonce,"behind_little_door");h.assertTrue(RescueChapter.phase(s)==RescueChapter.Phase.FINAL_D,"Replayed eye nonce skipped recording D");}finally{clean(p);clean(other);}h.succeed();});
    }
    @GameTest(template="empty") public static void storedOriginalEyeReopensLegacyLockedConfrontation(GameTestHelper h){
        var p=player(h);var s=OtherMotherLoop.scene(p);
        try{
            for(int i=0;i<3;i++){
                var eye=RescueEyes.eye(s,i);eye.putBoolean("Defeated",true);
                eye.putBoolean("Discovered",true);
                h.assertTrue(RescueEyes.claim(p,s,i),"Unable to issue original eye fixture");
            }
            h.assertTrue(RescueEyes.carriedByOwner(p,s),"All three original eyes should allow the sealed-room objective");
            var stored=p.getInventory().getItem(0).copy();
            p.getEnderChestInventory().setItem(0,stored);
            p.getInventory().setItem(0,ItemStack.EMPTY);
            h.assertTrue(RescueEyes.all(s)&&!RescueEyes.carriedByOwner(p,s),
                    "Saved eye claims bypassed the physical-inventory gate");
            var n=RescueChapter.data(s);n.putBoolean("RoomLocked",true);
            RescueChapter.go(p,s,RescueChapter.Phase.SHOW_EYES);
            s.loop=OtherMotherLoopState.load(s.loop.save());
            h.assertFalse(BeldamFinale.preflight(p,s),"A missing original eye was accepted inside the locked finale");
            h.assertTrue(RescueChapter.phase(s)==RescueChapter.Phase.HOUSE_SAFE
                            &&!RescueChapter.data(s).getBoolean("RoomLocked"),
                    "An older save could not reopen its sealed room to retrieve the original eye");
            p.getInventory().setItem(0,p.getEnderChestInventory().getItem(0).copy());
            p.getEnderChestInventory().setItem(0,ItemStack.EMPTY);
            h.assertTrue(RescueEyes.carriedByOwner(p,s),"Returning the actual stored eye did not restore the gate");
        }finally{clean(p);}
        h.succeed();
    }
    private RescueFinaleInputGameTests(){}
}
