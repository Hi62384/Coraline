package com.coraline.feast;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class StoryBacklogGameTests {
    @GameTest(template="empty") public static void seeingStoneUseSurvivesRealmProtection(GameTestHelper h){
        var level=h.getLevel().getServer().getLevel(com.coraline.otherworld.OtherWorld.KEY);
        var p=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"StoneUseQA"));
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);
        var hand=net.minecraft.world.InteractionHand.MAIN_HAND;var stone=new net.minecraft.world.item.ItemStack(com.coraline.registry.ModItems.SEEING_STONE.get());p.setItemInHand(hand,stone);
        var air=new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem(p,hand);
        com.coraline.otherworld.RealmProtection.useItem(air);h.assertFalse(air.isCanceled(),"Protected realm cancels the Stone in empty space");
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(BlockPos.ZERO),net.minecraft.core.Direction.UP,BlockPos.ZERO,false);
        var block=new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(p,hand,BlockPos.ZERO,hit);
        com.coraline.otherworld.RealmProtection.useBlock(block);
        h.assertTrue(!block.isCanceled()&&block.getUseItem()==net.minecraftforge.eventbus.api.Event.Result.ALLOW&&block.getUseBlock()==net.minecraftforge.eventbus.api.Event.Result.DENY,"Looking at scenery either cancels the Stone or operates the block");
        stone.getItem().use(level,p,hand);h.assertTrue(p.isUsingItem()&&p.getUseItem().is(stone.getItem()),"Stone never enters its raised-use state");p.stopUsingItem();
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.stage=VisitData.Stage.BED;
        VisitData.get(p.server).scenes.put(s.owner,s);p.getPersistentData().putUUID("CoralineVisitDoor",s.door);
        try{
            var sleeping=new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem(p,hand);com.coraline.otherworld.RealmProtection.useItem(sleeping);h.assertTrue(sleeping.isCanceled(),"Stone bypasses scripted sleep lock");
            var sleepingBlock=new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(p,hand,BlockPos.ZERO,hit);com.coraline.otherworld.RealmProtection.useBlock(sleepingBlock);h.assertTrue(sleepingBlock.isCanceled(),"Block-targeted Stone bypasses scripted sleep lock");
        }finally{VisitData.get(p.server).scenes.remove(s.owner);}
        h.succeed();
    }
    @GameTest(template="empty") public static void cancelledVoiceCannotDelayNextProposal(GameTestHelper h){
        var s=new VisitData.Scene();s.welcomeAfter=Long.MAX_VALUE;s.loop.secrets.dialogue.putString("VoiceClockSound","old");s.loop.secrets.dialogue.putLong("VoiceClockEnd",Long.MAX_VALUE);
        VisitEvents.clearCancelledVoice(s);h.assertTrue(s.welcomeAfter==0&&!s.loop.secrets.dialogue.contains("VoiceClockEnd")&&!s.loop.secrets.dialogue.contains("VoiceClockSound"),"Cancelled invitation still reserves its old playback interval");h.succeed();
    }
    @GameTest(template="empty") public static void proposalSeatingAndRunGate(GameTestHelper h){
        var s=new VisitData.Scene();s.table=new BlockPos(20,6,30);var player=BlackIsTraditional.seat(s,0);
        var mother=BlackIsTraditional.seat(s,1);var father=BlackIsTraditional.seat(s,2);
        h.assertTrue(mother.x<player.x-2&&father.x>player.x+2,"Parents are not on the player's left/right table edges");
        for(var parent:java.util.List.of(mother,father))h.assertTrue(parent.z>player.z+1&&parent.z<s.table.getZ()&&parent.distanceTo(player)>2&&parent.distanceTo(player)<5,"Parent is across the table or overlapping the player instead of on a side");
        h.assertTrue(StoryAudioContinuity.next("black_is_traditional_l").isEmpty(),"Run recording auto-plays before reaching the stairs");h.succeed();
    }
    @GameTest(template="empty") public static void ordinarySelectionKeepsOutingAndMotherAwayReachable(GameTestHelper h){
        var candidates=java.util.List.of(VisitEvents.Type.ACTIVITY,VisitEvents.Type.FAMILY_OUTING,VisitEvents.Type.OTHER_MOTHER_GOES_OUT);var random=new java.util.Random(74);var seen=java.util.EnumSet.noneOf(VisitEvents.Type.class);var previous=VisitEvents.Type.ACTIVITY;
        for(int skip=0;skip<200;skip++){var next=VisitEventSelection.chooseFortySixty(candidates,candidates::contains,previous,VisitEvents.Type.ACTIVITY,random::nextInt);h.assertTrue(next!=previous,"Repeated skip selected the same event");seen.add(next);previous=next;}
        h.assertTrue(seen.contains(VisitEvents.Type.FAMILY_OUTING)&&seen.contains(VisitEvents.Type.OTHER_MOTHER_GOES_OUT),"A requested ordinary event became unreachable");
        h.assertTrue(NumberedEventDialogue.ready("mother_goes_out"),"Mother-away existing dialogue assets are not ready");h.succeed();
    }
    @GameTest(template="empty") public static void gameNightControllerIsNotStarvedByCompanionGoal(GameTestHelper h){
        var w=com.coraline.registry.CharacterCast.ENTITIES.get("other_wybie").get().create(h.getLevel());var goal=new WybieOptionalVisits.CompanionGoal(w);
        w.getPersistentData().putUUID("WybieCompanionOwner",UUID.randomUUID());h.assertTrue(goal.canUse(),"Normal following is unavailable");
        w.getPersistentData().putUUID("GameNightOwner",UUID.randomUUID());h.assertFalse(goal.canUse(),"Companion MOVE/LOOK goal still starves game-night seating");w.discard();h.succeed();
    }
    @GameTest(template="empty") public static void basementPermissionEndsAfterPhysicalExit(GameTestHelper h){
        var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"BasementAccessQA"));var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.loop.event.putInt("Serial",2);s.loop.event.putBoolean("TheatrePending",true);
        var n=TheatreVisit.data(s);n.putUUID("Owner",s.owner);n.putInt("Serial",2);n.putBoolean("InvitationDone",true);
        h.assertTrue(TheatreVisit.basementAccess(p,s),"Invited access missing");n.putBoolean("BasementExited",true);h.assertFalse(TheatreVisit.basementAccess(p,s),"Finished visit leaves the basement unlocked");h.succeed();
    }
    private StoryBacklogGameTests(){}
}
