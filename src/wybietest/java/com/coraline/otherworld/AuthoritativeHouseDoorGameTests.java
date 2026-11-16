package com.coraline.otherworld;

import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.gametest.*;

/** Source-only regression fixture; sound still needs a real client observer. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class AuthoritativeHouseDoorGameTests {
    @GameTest(template="empty") public static void allowedDoorsToggleBothHalvesButIronAndKeyedDoorsStayLocked(GameTestHelper h){
        var l=h.getLevel();var p=new ServerPlayer(l.getServer(),l,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"DoorSoundQA"));var at=h.absolutePos(new BlockPos(2,2,2));
        l.setBlock(at.below(),Blocks.STONE.defaultBlockState(),2);
        for(Block block:new Block[]{Blocks.OAK_DOOR,Blocks.SPRUCE_DOOR,com.coraline.registry.ModBlocks.ROSEWOOD_GLASS_DOOR.get(),Blocks.IRON_DOOR,com.coraline.registry.ModBlocks.LOCKED_HOUSE_DOOR.get()}){
            var lower=block.defaultBlockState().setValue(DoorBlock.FACING,Direction.NORTH);l.setBlock(at,lower,2);l.setBlock(at.above(),lower.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),2);
            boolean allowed=block!=Blocks.IRON_DOOR&&!(block instanceof com.coraline.block.LockedHouseDoorBlock);
            h.assertTrue(AuthoritativeHouseDoors.toggle(p,at.above())==allowed,"Incorrect manual access policy: "+block);
            h.assertTrue(l.getBlockState(at).getValue(DoorBlock.OPEN)==allowed&&l.getBlockState(at.above()).getValue(DoorBlock.OPEN)==allowed,"Door halves disagree");
            if(allowed){h.assertFalse(AuthoritativeHouseDoors.setOpen(l,at,true),"Already-open door emits a duplicate transition");h.assertTrue(AuthoritativeHouseDoors.toggle(p,at),"Door cannot close");h.assertFalse(l.getBlockState(at.above()).getValue(DoorBlock.OPEN),"Upper half remains open");}
            l.removeBlock(at.above(),false);l.removeBlock(at,false);
        }
        h.succeed();
    }
}
