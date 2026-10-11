package com.coraline.feast;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;
import static com.coraline.feast.ActivityScenery.*;

/** Compact, densely furnished concert gardens; all paths lead back to the central promenade. */
final class FestivalGrounds {
    static AABB bounds(BlockPos c){return new AABB(c.offset(-49,-3,-79),c.offset(49,19,16));}
    static void build(ServerLevel l,BlockPos c){
        if(l.getBlockState(c.below(2)).is(Blocks.MAGENTA_GLAZED_TERRACOTTA))return;
        for(int x=-47;x<=47;x++)for(int z=-76;z<=13;z++){
            boolean path=Math.abs(x)<=3||Math.abs(z+16)<=2||Math.abs(z+46)<=2||Math.abs(x-29)<=2||Math.abs(x+29)<=2;
            put(l,c,x,-1,z,path?(Math.floorMod(x+z,7)==0?Blocks.SEA_LANTERN:Blocks.POLISHED_DIORITE):Blocks.MOSS_BLOCK);
            if(Math.abs(x)==47||z==-76||z==13)put(l,c,x,0,z,Blocks.CHERRY_FENCE);
            if(!path&&Math.floorMod(x*13+z*7,43)==0)put(l,c,x,0,z,Math.floorMod(x,2)==0?Blocks.ALLIUM:Blocks.PINK_TULIP);
        }
        // Entry arch, readable from the arrival and exit portal.
        for(int x:new int[]{-5,5})box(l,c,x,0,6,x,6,6,Blocks.QUARTZ_PILLAR);
        box(l,c,-5,6,6,5,6,6,Blocks.CHERRY_PLANKS);box(l,c,-3,7,6,3,7,6,Blocks.GOLD_BLOCK);
        stage(l,c,0,-62,12,Blocks.PURPLE_WOOL); // great stage
        stage(l,c,-30,-34,8,Blocks.PINK_WOOL); // garden stage
        stage(l,c,30,-61,8,Blocks.CYAN_WOOL); // magic stage
        stage(l,c,0,-25,7,Blocks.MAGENTA_WOOL); // player's stage, audience looks north
        // Dance garden, with an inset lit mosaic and overhead light garlands.
        for(int x=17;x<=40;x++)for(int z=-40;z<=-25;z++)put(l,c,x,-1,z,Math.floorMod(x+z,4)==0?Blocks.SEA_LANTERN:(x+z)%2==0?Blocks.PURPLE_TERRACOTTA:Blocks.MAGENTA_TERRACOTTA);
        for(int x:new int[]{18,39})for(int z:new int[]{-39,-26})lamp(l,c,x,z);
        for(int x=-40;x<=40;x+=10){for(int z:new int[]{-16,-46}){lamp(l,c,x,z+3);if(x<40){for(int dx=0;dx<10;dx++)put(l,c,x+dx,6,z+3,dx%3==0?Blocks.SEA_LANTERN:Blocks.DARK_OAK_FENCE);}}}
        for(int x:new int[]{-40,-27,-14,15,28,41}){tent(l,c,x,2);bench(l,c,x,-5);}
        for(int x:new int[]{-40,-18,18,40})for(int z:new int[]{-71,-49,-20})tree(l,c,x,z);
        fountain(l,c,-15,-35);fountain(l,c,15,-51);
        // Lounge pockets and backstage crates, not empty grassy quadrants.
        for(int x:new int[]{-40,-24,24,40})for(int z:new int[]{-55,-12}){bench(l,c,x,z);for(int dx=-1;dx<=1;dx++)ActivityBlocks.set(l,c.offset(x+dx,0,z-3),Blocks.CHERRY_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,net.minecraft.world.level.block.state.properties.SlabType.TOP),2);put(l,c,x,1,z-3,Blocks.LANTERN);}
        for(int x=-8;x<=8;x+=4){put(l,c,x,1,-68,Blocks.BARREL);put(l,c,x,2,-68,Blocks.NOTE_BLOCK);}
        put(l,c,0,-2,0,Blocks.MAGENTA_GLAZED_TERRACOTTA);
    }
    private static void bench(ServerLevel l,BlockPos c,int x,int z){box(l,c,x-2,0,z,x+2,0,z,Blocks.CHERRY_SLAB);box(l,c,x-2,1,z+1,x+2,1,z+1,Blocks.CHERRY_FENCE);}
    private static void tent(ServerLevel l,BlockPos c,int x,int z){
        for(int dx:new int[]{-3,3})for(int dz:new int[]{-2,2})box(l,c,x+dx,0,z+dz,x+dx,3,z+dz,Blocks.CHERRY_FENCE);
        for(int dx=-3;dx<=3;dx++)for(int dz=-2;dz<=2;dz++)put(l,c,x+dx,4+(2-Math.abs(dz)),z+dz,dx%2==0?Blocks.WHITE_WOOL:Blocks.PINK_WOOL);
        box(l,c,x-2,0,z,x+2,0,z,Blocks.CHERRY_PLANKS);put(l,c,x-2,1,z,Blocks.CAKE);put(l,c,x+2,1,z,Blocks.LANTERN);put(l,c,x,1,z,Blocks.FLOWER_POT);
    }
    private static void stage(ServerLevel l,BlockPos c,int x,int z,int w,Block color){
        box(l,c,x-w,0,z-5,x+w,0,z+3,Blocks.DARK_OAK_PLANKS);
        box(l,c,x-w,-1,z-5,x+w,-1,z+3,Blocks.QUARTZ_BRICKS);
        box(l,c,x-w,1,z-5,x+w,4,z-5,color);
        for(int dx:new int[]{-w,w}){box(l,c,x+dx,1,z-4,x+dx,6,z+3,Blocks.QUARTZ_PILLAR);box(l,c,x+dx-1,1,z+1,x+dx+1,3,z+2,Blocks.BLACK_CONCRETE);put(l,c,x+dx,2,z+3,Blocks.NOTE_BLOCK);}
        for(int dx=-w;dx<=w;dx++)for(int dz=-5;dz<=3;dz++)put(l,c,x+dx,7+Math.max(0,2-Math.abs(dx)/3),z+dz,dx%3==0?Blocks.WHITE_WOOL:color);
        for(int dx=-w+1;dx<w;dx+=3){put(l,c,x+dx,6,z+2,Blocks.SEA_LANTERN);put(l,c,x+dx,4,z-4,Blocks.GOLD_BLOCK);}
        box(l,c,x-2,0,z+4,x+2,0,z+4,Blocks.DARK_OAK_SLAB);
    }
    private FestivalGrounds(){}
}
