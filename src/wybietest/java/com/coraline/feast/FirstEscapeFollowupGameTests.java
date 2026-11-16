package com.coraline.feast;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class FirstEscapeFollowupGameTests {
    @GameTest(template="empty") public static void missedTeaRollQueuesOnceAndSurvivesReload(GameTestHelper h){
        var n=new net.minecraft.nbt.CompoundTag();n.putBoolean("Rolled",true);n.putBoolean("Selected",false);
        for(int i=0;i<300;i++)h.assertFalse(MissingParentsTea.invitationReady(n),"Missed roll bypassed its delay");
        n=n.copy();for(int i=0;i<299;i++)h.assertFalse(MissingParentsTea.invitationReady(n),"Reload reset or prematurely finished delay");
        h.assertTrue(MissingParentsTea.invitationReady(n)&&n.getBoolean("Selected"),"Missed one-time roll permanently erased required visit");
        h.assertTrue(MissingParentsTea.invitationReady(n)&&n.getInt("SelectionDelay")==0,"Already-selected invitation restarted its delay");h.succeed();
    }
    @GameTest(template="empty") public static void rescueLandingAcceptsHallCarpetButNeverBasementFallback(GameTestHelper h){
        var l=h.getLevel().getServer().getLevel(com.coraline.otherworld.OtherWorld.KEY);
        var p=new ServerPlayer(l.getServer(),l,new GameProfile(UUID.randomUUID(),"HallFloorQA"));
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();FirstEscapeStory.data(s).putLong("House",new BlockPos(113600,180,113600).asLong());
        var hall=FirstEscapeScene.mirror(s).north(2);
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
            var at=hall.offset(x,0,z);l.getChunkAt(at);l.setBlock(at.below(),Blocks.OAK_PLANKS.defaultBlockState(),2);l.setBlock(at,Blocks.BLUE_CARPET.defaultBlockState(),2);l.setBlock(at.above(),Blocks.AIR.defaultBlockState(),2);
            // A tempting basement landing at exactly the same X/Z.
            l.setBlock(at.below(5),Blocks.STONE.defaultBlockState(),2);l.setBlock(at.below(4),Blocks.AIR.defaultBlockState(),2);l.setBlock(at.below(3),Blocks.AIR.defaultBlockState(),2);
        }
        var owner=FirstEscapeScene.hallLanding(p,s,null);var helper=FirstEscapeScene.hallLanding(p,s,owner);
        h.assertTrue(owner!=null&&helper!=null,"Carpeted hall rescue has no safe landing");
        h.assertTrue(Math.abs(owner.y-hall.getY()-.0625)<.001&&Math.abs(helper.y-owner.y)<.001,"Rescue left the authored hall floor");
        h.assertTrue(owner.distanceToSqr(helper)>=1.44,"Rescued owner overlaps the masked helper");
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)l.setBlock(hall.offset(x,0,z),Blocks.STONE.defaultBlockState(),2);
        h.assertTrue(FirstEscapeScene.hallLanding(p,s,null)==null,"Obstructed hall silently teleports to basement/roof");h.succeed();
    }
    @GameTest(template="empty") public static void storySkipItemRejectsNonOperatorsWithoutStoryMutation(GameTestHelper h){
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"DeniedSkipQA"));
        h.assertTrue(com.coraline.registry.ModItems.STORY_SKIP_DEBUG.get() instanceof com.coraline.item.StorySkipDebugItem,"Debug skip item not registered");
        h.assertTrue(com.coraline.item.StorySkipDebugItem.request(p,false).contains("permission"),"First Escape skip accepted non-operator");
        h.assertTrue(com.coraline.item.StorySkipDebugItem.request(p,true).contains("permission"),"Final fight skip accepted non-operator");
        h.assertFalse(p.getPersistentData().contains("CoralineDebugStorySkip"),"Denied request created a confirmation");h.succeed();
    }
}
