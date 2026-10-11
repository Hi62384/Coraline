package com.coraline.feast;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;
import static com.coraline.feast.ActivityScenery.*;

final class DesertTempleCourse {
    static int top(int room){return -18-room*29;}
    static BlockPos tile(BlockPos c,int room,TemplePath.Tile t){return c.offset(t.x()-TemplePath.size(room)/2,0,top(room)-t.z());}
    static AABB bounds(BlockPos c){return new AABB(c.offset(-18,-3,-167),c.offset(18,19,12));}
    static void build(ServerLevel l,BlockPos c){
        if(l.getBlockState(c.below(2)).is(Blocks.ORANGE_GLAZED_TERRACOTTA))return;
        for(int z=-164;z<=10;z++)for(int x=-16;x<=16;x++){
            put(l,c,x,-1,z,Math.abs(x)%5==0?Blocks.CUT_SANDSTONE:Blocks.SMOOTH_SANDSTONE);
            if(Math.abs(x)==16)for(int y=0;y<=14;y++)put(l,c,x,y,z,y==3||y==12?Blocks.GOLD_BLOCK:Blocks.SANDSTONE);
            put(l,c,x,15,z,Math.abs(x)<3&&z%3==0?Blocks.ORANGE_STAINED_GLASS:Blocks.SANDSTONE);
        }
        for(int end:new int[]{10,-164})box(l,c,-16,0,end,16,15,end,Blocks.CUT_SANDSTONE);
        for(int room=0;room<5;room++){
            int top=top(room),n=TemplePath.size(room),half=n/2;
            // Walls keep traversal on the board; the entry ramp offers a clear elevated preview.
            box(l,c,-half-1,0,top-n+1,-half-1,4,top,Blocks.CUT_SANDSTONE);
            box(l,c,n-half,0,top-n+1,n-half,4,top,Blocks.CUT_SANDSTONE);
            for(int x=0;x<n;x++)for(int z=0;z<n;z++)put(l,c,x-half,0,top-z,Blocks.STONE_PRESSURE_PLATE);
            for(int step=1;step<=3;step++)box(l,c,-half,0,top+step,n-half-1,step-2,top+step,Blocks.SMOOTH_SANDSTONE);
            box(l,c,-half,0,top+4,n-half-1,2,top+6,Blocks.SMOOTH_SANDSTONE);
            // Public approach staircase up to the viewing balcony.
            for(int step=0;step<3;step++)box(l,c,-2,0,top+9-step,2,step,top+9-step,Blocks.SMOOTH_SANDSTONE);
            gate(l,c,room,true);door(l,c,room,true);
            for(int side:new int[]{-1,1})for(int z=top-10;z<=top+10;z+=5){
                int x=side*(10+room%2);box(l,c,x,0,z,x,9+room,z,Blocks.SANDSTONE);
                put(l,c,x,1,z+1,Blocks.CHISELED_SANDSTONE);put(l,c,x,6,z+1,Blocks.GOLD_BLOCK);put(l,c,x,10+room,z,Blocks.SEA_LANTERN);
                // Mural panels and carved button-eyed guardian statues outside sight lines.
                for(int dx=-1;dx<=1;dx++)for(int y=2;y<=5;y++)put(l,c,x+dx,y,z-1,(dx+y+room)%3==0?Blocks.ORANGE_TERRACOTTA:Blocks.RED_SANDSTONE);
                put(l,c,x-1,5,z,Blocks.BLACK_TERRACOTTA);put(l,c,x+1,5,z,Blocks.BLACK_TERRACOTTA);
                put(l,c,side*14,0,z,Blocks.DECORATED_POT);put(l,c,side*14,1,z,Blocks.LANTERN);
            }
            for(int x=-8;x<=8;x++)put(l,c,x,11+room/2,top+7,x%3==0?Blocks.GOLD_BLOCK:Blocks.CUT_SANDSTONE);
        }
        fountain(l,c,-10,1);fountain(l,c,10,1);
        // Final royal treasure hall, behind the fifth-room gate.
        for(int x:new int[]{-11,-7,7,11})for(int z=-162;z<=-148;z+=4){box(l,c,x,0,z,x+1,1+(Math.abs(x)%3),z+1,Blocks.GOLD_BLOCK);put(l,c,x,3,z,Blocks.EMERALD_BLOCK);}
        box(l,c,-3,0,-160,3,0,-156,Blocks.CHISELED_QUARTZ_BLOCK);put(l,c,0,1,-158,Blocks.GOLD_BLOCK);
        put(l,c,0,-2,0,Blocks.ORANGE_GLAZED_TERRACOTTA);
    }
    // Transparent magical gate: nothing obscures the board during memorization.
    static void gate(ServerLevel l,BlockPos c,int room,boolean closed){int n=TemplePath.size(room);box(l,c,-n/2,3,top(room)+4,n-n/2-1,5,top(room)+4,closed?Blocks.BARRIER:Blocks.AIR);}
    static void door(ServerLevel l,BlockPos c,int room,boolean closed){int n=TemplePath.size(room);box(l,c,-16,0,top(room)-n,16,8,top(room)-n,Blocks.CUT_SANDSTONE);if(!closed)box(l,c,-n/2,0,top(room)-n,n-n/2-1,3,top(room)-n,Blocks.AIR);}
    private DesertTempleCourse(){}
}
