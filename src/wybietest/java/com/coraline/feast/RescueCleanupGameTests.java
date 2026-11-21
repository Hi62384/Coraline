package com.coraline.feast;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class RescueCleanupGameTests {
    @GameTest(template="empty") public static void retirementIsBoundedPersistedAndPreservesForeignEdits(GameTestHelper helper){
        var server=helper.getLevel().getServer();var player=new ServerPlayer(server,helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"CleanupQA"));
        var scene=new VisitData.Scene();scene.owner=player.getUUID();scene.door=java.util.UUID.randomUUID();scene.loop.event.putInt("Serial",7);VisitData.get(server).scenes.put(scene.owner,scene);
        var data=RescueChapter.data(scene);data.putBoolean("Started",true);var level=server.getLevel(ActivityWorld.KEY);var origin=new BlockPos(450,50,450);var web=new CompoundTag();var passage=new CompoundTag();
        for(int index=0;index<120;index++){var at=origin.offset(index%15,0,index/15);level.getChunkAt(at);var cell=new CompoundTag();cell.put("Original",NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState()));cell.put("Placed",NbtUtils.writeBlockState(Blocks.WHITE_WOOL.defaultBlockState()));(index<100?web:passage).put(Long.toString(at.asLong()),cell);level.setBlock(at,Blocks.WHITE_WOOL.defaultBlockState(),2);}
        data.put("WebCells",web);data.put("PassageCells",passage);data.putBoolean("WebReady",true);data.putBoolean("PassageReady",true);
        var changed=origin.offset(14,0,7);level.setBlock(changed,Blocks.GOLD_BLOCK.defaultBlockState(),2);
        try{
            RescueSiteCleanup.tick(player,scene);helper.assertTrue(web.size()+passage.size()==120,"Cleanup began before the physical real-world exit");
            data.putBoolean("CrawlReachedOverworld",true);RescueSiteCleanup.tick(player,scene);helper.assertTrue(web.size()+passage.size()==24,"Cleanup exceeded or ignored its 96-cell tick budget");
            var saved=new VisitData.Scene();saved.owner=scene.owner;saved.door=scene.door;saved.loop=OtherMotherLoopState.load(scene.loop.save());
            RescueSiteCleanup.tick(player,saved);helper.assertFalse(RescueChapter.data(saved).contains("WebCells"),"The 119 truly owned cells were not retired");helper.assertTrue(RescueChapter.data(saved).getCompound("PassageCells").size()==1&&RescueChapter.data(saved).getCompound("PassageCells").contains(Long.toString(changed.asLong())),"The foreign-edited cell must keep its unresolved original-owner receipt");
            helper.assertTrue(level.getBlockState(changed).is(Blocks.GOLD_BLOCK),"Cleanup replaced a foreign edit");
            for(int index=0;index<119;index++)helper.assertTrue(level.isEmptyBlock(origin.offset(index%15,0,index/15)),"Temporary block survived journal retirement");
            helper.assertFalse(RescueChapter.data(saved).getBoolean("WebReady")||RescueChapter.data(saved).getBoolean("PassageReady"),"Retired sites stayed eligible for re-entry");
        }finally{for(int index=0;index<120;index++)level.setBlock(origin.offset(index%15,0,index/15),Blocks.AIR.defaultBlockState(),2);VisitData.get(server).scenes.remove(scene.owner);}
        helper.succeed();
    }
    private RescueCleanupGameTests(){}
}
