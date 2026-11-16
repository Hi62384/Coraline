package com.coraline.feast;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class TeaEntranceGameTests {
    @GameTest(template="empty") public static void unevenGrassyLandingGetsFloorAndClearHeadroomWithoutOverwritingLaterEdits(GameTestHelper h){
        var p=AcceptedBranchPlayers.fixture(h);var scene=OtherMotherLoop.scene(p);var level=p.serverLevel();var c=new BlockPos(120000+Math.floorMod(p.getUUID().hashCode(),10000)*32,180,120000);
        var before=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();var n=new CompoundTag();n.putLong("House",c.asLong());var placed=new CompoundTag();
        try{
            for(int x=-1;x<=1;x++)for(int z=-11;z<=-10;z++)for(int y=-1;y<=3;y++){var at=c.offset(x,y,z);level.getChunkAt(at);before.put(at,level.getBlockState(at));var state=(y==0?Blocks.GRASS_BLOCK:y==1?Blocks.TALL_GRASS:Blocks.AIR).defaultBlockState();level.setBlock(at,state,2);placed.put(Long.toString(at.asLong()),NbtUtils.writeBlockState(state));}
            n.put("Placed",placed);var method=MissingParentsTea.class.getDeclaredMethod("clearEntrance",net.minecraft.server.level.ServerPlayer.class,CompoundTag.class);method.setAccessible(true);method.invoke(null,p,n);
            for(int x=-1;x<=1;x++)for(int z=-11;z<=-10;z++){h.assertTrue(level.getBlockState(c.offset(x,-1,z)).is(Blocks.STONE_BRICKS),"Landing floor missing");for(int y=0;y<=3;y++)h.assertTrue(level.getBlockState(c.offset(x,y,z)).isAir(),"Terrain or grass still blocks door approach");}
            var edited=c.offset(1,0,-11);level.setBlock(edited,Blocks.GOLD_BLOCK.defaultBlockState(),2);method.invoke(null,p,n);h.assertTrue(level.getBlockState(edited).is(Blocks.GOLD_BLOCK),"Repair overwrote a later player edit");
        }catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}finally{before.forEach((at,state)->level.setBlock(at,state,2));AcceptedBranchPlayers.remove(p,scene);}h.succeed();
    }
    private TeaEntranceGameTests(){}
}
