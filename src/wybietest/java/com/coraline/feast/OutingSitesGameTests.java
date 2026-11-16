package com.coraline.feast;

import com.coraline.otherworld.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class OutingSitesGameTests {
    @GameTest(template="empty",batch="outing-copy-coordinates")
    public static void copiedTerrainUsesSourceChunksAndBoundaryMargin(GameTestHelper h){
        var r=new RealmData.Realm();r.sourceDoor=new BlockPos(100,80,100);r.dx=32000;r.dz=-16000;r.radius=240;
        var target=r.sourceDoor.offset(r.dx,0,r.dz);r.copiedChunks.add(ChunkPos.asLong(r.sourceDoor.getX()>>4,r.sourceDoor.getZ()>>4));
        h.assertTrue(OutingSites.copied(r,target),"Copy mask incorrectly uses destination chunk coordinates");
        h.assertFalse(OutingSites.copied(r,target.east(32)),"Uncopied neighboring terrain accepted");r.islandComplete=true;
        h.assertTrue(OutingSites.copied(r,target.east(150)),"Completed safe terrain rejected");
        h.assertFalse(OutingSites.copied(r,target.east(220)),"Destination intruded on the realm's protected edge margin");h.succeed();
    }
    @GameTest(template="empty",batch="outing-persistence")
    public static void siteKeepsRealPathsAndWaterCoordinatesOnSave(GameTestHelper h){
        var origin=new BlockPos(42000,120,42000);var approach=List.of(origin,origin.east(),origin.east().above());var walk=List.of(origin.east(12),origin.south(18),origin.west(14),origin);
        var site=new OutingSites.Site(origin,approach,walk,List.of(new Vec3(origin.getX()+.25,119.85,origin.getZ()+.75)));
        var restored=OutingSites.Site.load(site.save());h.assertTrue(site.equals(restored),"Destination, winding rest points or boat route changed across persistence");h.succeed();
    }
    @GameTest(template="empty",batch="outing-water-safety",timeoutTicks=100)
    public static void boatsRequireHullClearanceDepthAndAnActualLake(GameTestHelper h){
        var level=h.getLevel().getServer().getLevel(OtherWorld.KEY);var base=new BlockPos(42500,130,42500);level.getChunkAt(base);
        var r=new RealmData.Realm();r.sourceDoor=base;r.radius=250;r.islandComplete=true;
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){level.setBlock(base.offset(x,0,z),Blocks.WATER.defaultBlockState(),2);level.setBlock(base.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);for(int y=1;y<=2;y++)level.setBlock(base.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);}
        var boat=new Vec3(base.getX()+.5,base.getY()+.85,base.getZ()+.5);h.assertFalse(OutingSites.waterSafe(level,r,boat),"Shallow water accepted for a moving boat hull");
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)level.setBlock(base.offset(x,-1,z),Blocks.WATER.defaultBlockState(),2);
        h.assertTrue(OutingSites.waterSafe(level,r,boat),"Deep clear hull footprint rejected");level.setBlock(base.above(2),Blocks.STONE.defaultBlockState(),2);h.assertFalse(OutingSites.waterSafe(level,r,boat),"Low ceiling accepted above boat passengers");level.setBlock(base.above(2),Blocks.AIR.defaultBlockState(),2);
        h.assertTrue(OutingSites.waterRoute(level,r,base.west(6).above()).isEmpty(),"A tiny pool was mistaken for a complete lake circuit");
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=-1;y<=2;y++)level.setBlock(base.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);h.succeed();
    }
}
