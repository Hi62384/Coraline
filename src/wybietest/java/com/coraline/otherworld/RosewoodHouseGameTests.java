package com.coraline.otherworld;
import com.coraline.block.TinyDoorBlock;
import com.coraline.registry.ModBlocks;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class RosewoodHouseGameTests {
    @GameTest(template="empty",batch="terrain",timeoutTicks=400) public static void terrainMigrationRemovesTheOldRectangularPedestal(GameTestHelper h){
        var source=h.getLevel();var target=source.getServer().getLevel(OtherWorld.KEY);h.assertTrue(target!=null,"Other World unavailable");
        var origin=h.absolutePos(new BlockPos(1500,150,1500));var realm=new RealmData.Realm();realm.dx=1600;realm.dz=1600;realm.house=origin.offset(realm.dx,20,realm.dz);realm.estateVersion=8;
        for(int x=-55;x<=55;x++)for(int z=-55;z<=55;z++){
            int top=origin.getY()+Math.floorDiv(x+z,16);
            // The fixture supplies a controlled slope. A fresh Overworld seed
            // may already have a taller mountain here; remove that overburden
            // rather than letting it change the expected floor-selection input.
            int existingTop=source.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,origin.getX()+x,origin.getZ()+z);
            for(int y=top+1;y<=existingTop;y++)source.setBlock(new BlockPos(origin.getX()+x,y,origin.getZ()+z),Blocks.AIR.defaultBlockState(),2);
            for(int y=origin.getY()-10;y<=top;y++){
                var state=(y==top?Blocks.GRASS_BLOCK:y>=top-2?Blocks.DIRT:Blocks.STONE).defaultBlockState();var pos=new BlockPos(origin.getX()+x,y,origin.getZ()+z);source.setBlock(pos,state,2);target.setBlock(pos.offset(realm.dx,0,realm.dz),state,2);
            }
        }
        var old=realm.house;for(int x=-33;x<=31;x++)for(int z=-43;z<=41;z++)for(int y=-40;y<=-2;y++)target.setBlock(old.offset(x,y,z),Blocks.STONE.defaultBlockState(),2);
        var door=new DoorData.Door();door.pos=origin;OtherWorldEstate.build(source,target,door,realm,origin);
        var corner=new BlockPos(origin.getX()+31,origin.getY()+Math.floorDiv(31+40,16),origin.getZ()+40).offset(realm.dx,0,realm.dz);
        h.assertTrue(target.getBlockState(corner).is(Blocks.GRASS_BLOCK),"Old plot corner wasn't restored to original terrain");
        h.assertTrue(target.isEmptyBlock(old.offset(31,-2,40)),"The old high pedestal remains outside the foundations: "+target.getBlockState(old.offset(31,-2,40))+" new house "+realm.house+" old "+old);
        h.assertTrue(realm.house.getY()<old.getY()-10,"House still uses the highest distant hillside as its floor");h.succeed();
    }
    @GameTest(template="empty",batch="rosewood",timeoutTicks=400) public static void authoredHouseAndStoryAnchors(GameTestHelper h){
        var center=h.absolutePos(new BlockPos(180,50,180));var door=new DoorData.Door();var realm=new RealmData.Realm();
        var landscape=center.offset(31,-2,40);h.getLevel().setBlock(landscape,Blocks.GRANITE.defaultBlockState(),2);
        RosewoodHouse.build(h.getLevel(),center,door,realm);
        h.assertTrue(realm.doorPos.equals(RosewoodHouse.at(center,14,6,38)),"Portal isn't in the blueprint recess");
        var state=h.getLevel().getBlockState(realm.doorPos);
        h.assertTrue(state.is(ModBlocks.TINY_DOOR.get())&&state.getValue(TinyDoorBlock.PORTAL),"Missing working little door");
        h.assertTrue(h.getLevel().isEmptyBlock(realm.doorPos.east())&&h.getLevel().isEmptyBlock(realm.doorPos.east().above()),"Portal approach blocked");
        h.assertTrue(h.getLevel().getBlockEntity(realm.doorPos) instanceof DoorBlockEntity block&&door.id.equals(block.doorId),"Door identity lost");
        h.assertTrue(h.getLevel().getBlockState(realm.bed).is(Blocks.BLUE_BED),"Story bed missing");
        h.assertTrue(OtherWorldEstate.insideStudy(center,realm.piano),"Piano not inside the new study");
        h.assertTrue(h.getLevel().getBlockState(OtherWorldEstate.studyDoor(center)).getBlock() instanceof DoorBlock,"Study entrance missing");
        OtherWorldEstate.lockStudy(h.getLevel(),center,true);
        h.assertFalse(h.getLevel().getBlockState(OtherWorldEstate.studyDoor(center)).getValue(DoorBlock.OPEN),"Study didn't close");
        OtherWorldEstate.lockStudy(h.getLevel(),center,false);
        h.assertTrue(h.getLevel().getBlockState(OtherWorldEstate.studyDoor(center)).getValue(DoorBlock.OPEN),"Study didn't reopen");
        h.assertTrue(h.getLevel().getBlockState(OtherWorldEstate.atticDoor(center)).is(Blocks.SPRUCE_DOOR),"Attic anchor missing");
        h.assertTrue(h.getLevel().getBlockState(RosewoodHouse.at(center,13,-2,12)).is(Blocks.STONE_BRICKS)||!h.getLevel().isEmptyBlock(RosewoodHouse.at(center,13,-2,12)),"Basement foundation lost");
        for(int x=14;x<=22;x++)for(int z=20;z<=22;z++)h.assertTrue(!h.getLevel().isEmptyBlock(RosewoodHouse.at(center,x,5,z)),"Cellar stair opening remains");
        for(int x=37;x<=41;x++)for(int z=35;z<=45;z++)h.assertTrue(!h.getLevel().isEmptyBlock(RosewoodHouse.at(center,x,23,z)),"Attic stair opening remains");
        h.assertTrue(h.getLevel().getBlockState(RosewoodHouse.at(center,30,6,32)).is(Blocks.SPRUCE_DOOR),"Boiler stair-side entrance missing");
        h.assertTrue(h.getLevel().getBlockState(RosewoodHouse.at(center,21,15,58)).is(Blocks.SPRUCE_DOOR),"Parents' en-suite connection missing");
        h.assertTrue(h.getLevel().getBlockState(RosewoodHouse.at(center,40,6,67)).is(ModBlocks.ROSEWOOD_OVEN.get()),"Kitchen rebuild missing");
        for(int x=16;x<=25;x++)for(int z=55;z<=64;z++)for(int y=7;y<=9;y++)h.assertTrue(h.getLevel().isEmptyBlock(RosewoodHouse.at(center,x,y,z)),"Study piano space obstructed");
        h.assertTrue(RosewoodHouse.at(center,51,6,68).getX()-RosewoodHouse.at(center,13,6,12).getX()==36,"Width reduction lost");
        h.assertTrue(h.getLevel().getBlockState(landscape).is(Blocks.GRANITE),"Landscape outside the foundations was flattened");
        h.assertTrue(h.getLevel().getBlockState(RosewoodHouse.at(center,26,-1,12)).is(Blocks.SPRUCE_DOOR),"Front exterior cellar entrance missing");
        for(int x=16;x<=58;x++)for(int z=6;z<=7;z++)for(int y=6;y<=7;y++)h.assertTrue(h.getLevel().isEmptyBlock(RosewoodHouse.at(center,x,y,z)),"Front porch walkway obstructed");
        for(int x=28;x>=20;x--)for(int z=9;z<=10;z++){int y=6+28-x;h.assertTrue(h.getLevel().getBlockState(RosewoodHouse.at(center,x,y,z)).getBlock() instanceof StairBlock,"Lower exterior flight has a missing tread");for(int head=1;head<=2;head++)h.assertTrue(h.getLevel().isEmptyBlock(RosewoodHouse.at(center,x,y+head,z)),"Lower exterior stair headroom blocked");}
        int step=0;for(int z=54;z<=64;z++){if(z==61||z==62)continue;int y=15+step++;h.assertTrue(h.getLevel().getBlockState(RosewoodHouse.at(center,7,y,z)).getBlock() instanceof StairBlock,"Upper exterior flight has a missing tread");for(int head=1;head<=2;head++)h.assertTrue(h.getLevel().isEmptyBlock(RosewoodHouse.at(center,7,y+head,z)),"Upper exterior stair headroom blocked");}
        h.succeed();
    }
}
