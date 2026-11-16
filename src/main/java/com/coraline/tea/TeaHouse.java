package com.coraline.tea;

import com.coraline.bobinsky.BobinskyHouse;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;

/** Block-built basement apartment, with a clear scripted route through its furnishings. */
public final class TeaHouse {
    public static final int RX=8,RZ=9;
    private static void put(ServerLevel l,BlockPos c,int x,int y,int z,BlockState s){l.setBlock(c.offset(x,y,z),s,2|16);}
    private static void put(ServerLevel l,BlockPos c,int x,int y,int z,Block b){put(l,c,x,y,z,b.defaultBlockState());}
    private static BlockState stair(Block b,Direction d){return b.defaultBlockState().setValue(StairBlock.FACING,d);}
    private static void lamp(ServerLevel l,BlockPos c,int x,int y,int z){put(l,c,x,y,z,Blocks.LANTERN);}
    private static void curtain(ServerLevel l,BlockPos c,int x,int z){for(int y=0;y<=4;y++)put(l,c,x,y,z,Blocks.RED_WOOL);put(l,c,x,5,z,Blocks.DARK_OAK_SLAB);}
    private static void chair(ServerLevel l,BlockPos c,int x,int z,Direction face){put(l,c,x,0,z,stair(Blocks.DARK_OAK_STAIRS,face));put(l,c,x,1,z,Blocks.DARK_OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING,face).setValue(TrapDoorBlock.OPEN,true));}
    private static void candle(ServerLevel l,BlockPos c,int x,int y,int z,int count){put(l,c,x,y,z,Blocks.YELLOW_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES,count).setValue(CandleBlock.LIT,true));}
    public static void build(ServerLevel l,CompoundTag v){
        var c=BlockPos.of(v.getLong("House"));BobinskyHouse.hold(l,c);
        for(int x=-RX;x<=RX;x++)for(int z=-RZ;z<=RZ;z++)for(int y=-1;y<=9;y++){
            Block b=Blocks.AIR;
            if(y==-1)b=(Math.floorMod(x*17+z*7,11)<2)?Blocks.CRACKED_STONE_BRICKS:Blocks.STONE_BRICKS;
            else if(y<=5&&(Math.abs(x)==RX||Math.abs(z)==RZ))b=Math.floorMod(x*13+z*7+y,9)==0?Blocks.CRACKED_STONE_BRICKS:com.coraline.registry.ModBlocks.TEA_WEATHERED_BRICKS.get();
            else if(y==6)b=Blocks.DARK_OAK_PLANKS;
            else if(y==7)b=Blocks.POLISHED_DEEPSLATE;
            l.removeBlockEntity(c.offset(x,y,z));put(l,c,x,y,z,b);
        }
        // Low masonry plinth, cornice and roof trim: an apartment, not a wooden shed.
        for(int x=-RX;x<=RX;x++)for(int z=-RZ;z<=RZ;z++)if(Math.abs(x)==RX||Math.abs(z)==RZ){put(l,c,x,0,z,Blocks.POLISHED_ANDESITE);put(l,c,x,5,z,Blocks.DARK_OAK_PLANKS);put(l,c,x,8,z,Blocks.DEEPSLATE_BRICK_SLAB);}
        // Narrow old windows, deep sills, red fabric drapes and candlelit rear alcove.
        for(int z:new int[]{-6,-1,5})for(int side:new int[]{-1,1}){
            int x=side*RX;for(int dz=0;dz<=1;dz++)for(int y=2;y<=3;y++)put(l,c,x,y,z+dz,Blocks.LIGHT_GRAY_STAINED_GLASS);
            for(int dz=-1;dz<=2;dz++)put(l,c,x-side,1,z+dz,Blocks.DARK_OAK_SLAB);
            curtain(l,c,x-side,z-1);curtain(l,c,x-side,z+2);
            put(l,c,x-side,4,z,Blocks.DARK_OAK_SLAB);put(l,c,x-side,4,z+1,Blocks.DARK_OAK_SLAB);
        }
        for(int x=-2;x<=2;x++){put(l,c,x,2,9,Blocks.LIGHT_GRAY_STAINED_GLASS);put(l,c,x,3,9,Blocks.LIGHT_GRAY_STAINED_GLASS);put(l,c,x,1,8,Blocks.DARK_OAK_SLAB);candle(l,c,x,2,8,1);}
        curtain(l,c,-3,8);curtain(l,c,3,8);
        // Front foyer with a curtained wide arch. Leave both the threshold and walk lane open.
        for(int x:new int[]{-4,4})for(int y=0;y<=5;y++)put(l,c,x,y,-5,Blocks.DARK_OAK_LOG);
        for(int x=-4;x<=4;x++)put(l,c,x,4,-5,Blocks.DARK_OAK_SLAB);
        curtain(l,c,-4,-6);curtain(l,c,4,-6);
        for(int x=-2;x<=2;x++)for(int z=-8;z<=-6;z++)put(l,c,x,0,z,Blocks.RED_CARPET);
        put(l,c,-6,0,-7,Blocks.BARREL);put(l,c,-6,1,-7,Blocks.POTTED_DEAD_BUSH);put(l,c,6,0,-7,Blocks.BARREL);lamp(l,c,6,1,-7);
        var door=BobinskyHouse.door(v);l.setBlock(door,Blocks.DARK_OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING,Direction.NORTH).setValue(DoorBlock.HALF,DoubleBlockHalf.LOWER),2);l.setBlock(door.above(),Blocks.DARK_OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING,Direction.NORTH).setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),2);
        // Wooden ceiling beams and suspended warm lamps. Blue soul lanterns echo fairy lights.
        for(int z:new int[]{-5,1,6})for(int x=-7;x<=7;x++)put(l,c,x,5,z,Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X));
        for(int x:new int[]{-5,5})for(int z:new int[]{-4,4}){for(int y=0;y<5;y++)put(l,c,x,y,z,Blocks.DARK_OAK_FENCE);put(l,c,x,3,z+1,Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true));}
        for(int z:new int[]{-3,3}){put(l,c,0,4,z,Blocks.CHAIN);put(l,c,0,3,z,Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true));}
        // Separate kitchen, with an open bead/chain doorway facing the sitting room.
        for(int z=0;z<=5;z++)for(int y=0;y<=4;y++)if(z!=2&&z!=3)put(l,c,-4,y,z,com.coraline.registry.ModBlocks.TEA_WEATHERED_BRICKS.get());
        for(int z=2;z<=3;z++){put(l,c,-4,4,z,Blocks.DARK_OAK_SLAB);put(l,c,-4,3,z,Blocks.CHAIN);}
        for(int z=0;z<=5;z++){put(l,c,-7,0,z,z==2?Blocks.SMOKER:Blocks.BARREL);put(l,c,-7,1,z,z==3?Blocks.CAULDRON:Blocks.SMOOTH_SANDSTONE_SLAB);put(l,c,-7,3,z,Blocks.BARREL);}
        put(l,c,-6,1,0,Blocks.POTTED_FERN);lamp(l,c,-6,2,5);put(l,c,-6,0,5,Blocks.CRAFTING_TABLE);
        // Tea table and opposing seats preserve the scripted table interaction coordinates.
        for(int x=-1;x<=0;x++)for(int z=0;z<=1;z++){put(l,c,x,0,z,Blocks.DARK_OAK_FENCE);put(l,c,x,1,z,Blocks.DARK_OAK_PRESSURE_PLATE);}
        chair(l,c,1,1,Direction.WEST);chair(l,c,-3,1,Direction.EAST);
        for(int x=-3;x<=2;x++)for(int z=-1;z<=2;z++)if(l.getBlockState(c.offset(x,0,z)).isAir())put(l,c,x,0,z,((x+z)&1)==0?Blocks.BROWN_CARPET:Blocks.RED_CARPET);
        // Red velvet sofa, side tables, reading lamp and mismatched upholstered chairs.
        for(int x=-2;x<=2;x++){put(l,c,x,0,6,com.coraline.registry.ModBlocks.TEA_VELVET_SOFA.get());put(l,c,x,1,7,Blocks.RED_WOOL);}
        put(l,c,-3,0,6,Blocks.RED_NETHER_BRICK_SLAB);put(l,c,3,0,6,Blocks.RED_NETHER_BRICK_SLAB);
        for(int x:new int[]{-4,4}){put(l,c,x,0,7,Blocks.DARK_OAK_FENCE);put(l,c,x,1,7,Blocks.DARK_OAK_SLAB);lamp(l,c,x,2,7);}
        chair(l,c,5,0,Direction.WEST);chair(l,c,5,2,Direction.WEST);put(l,c,6,0,1,Blocks.BARREL);candle(l,c,6,1,1,3);
        for(int x=-2;x<=2;x++)for(int z=4;z<=5;z++)put(l,c,x,0,z,(Math.abs(x)==2||z==4)?Blocks.BROWN_CARPET:Blocks.CYAN_CARPET);
        // Tall display shelving with paired little memorial statues and candle votives.
        for(int x:new int[]{-6,6})for(int y:new int[]{1,3})for(int z=6;z<=8;z++){
            put(l,c,x,y,z,Blocks.DARK_OAK_SLAB);
            if(z==7)candle(l,c,x,y+1,z,2);else put(l,c,x,y+1,z,com.coraline.registry.ModBlocks.TEA_MEMORIAL_DOG.get());
        }
        // Chests, old books, vanity, pots and theatre-colored wall textiles.
        put(l,c,6,0,-4,Blocks.CHISELED_BOOKSHELF);put(l,c,6,1,-4,Blocks.BOOKSHELF);put(l,c,6,2,-4,Blocks.POTTED_RED_TULIP);
        put(l,c,-6,0,-3,Blocks.BARREL);put(l,c,-6,1,-3,Blocks.POTTED_DEAD_BUSH);put(l,c,-7,2,-3,Blocks.LIGHT_BLUE_STAINED_GLASS_PANE);
        for(int x:new int[]{-6,6}){put(l,c,x,2,-8,Blocks.RED_WALL_BANNER.defaultBlockState().setValue(WallBannerBlock.FACING,Direction.SOUTH));put(l,c,x,1,-8,Blocks.DARK_OAK_SLAB);}
        put(l,c,3,2,-9,com.coraline.registry.ModBlocks.TEA_THEATRE_POSTER.get());
    }
}
