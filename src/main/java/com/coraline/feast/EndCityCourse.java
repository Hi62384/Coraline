package com.coraline.feast;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import java.util.*;
import static com.coraline.feast.ActivityScenery.*;

public final class EndCityCourse {
    public record Destination(int x,int y,int z,int stage) {public Vec3 vector(){return new Vec3(x+.5,y,z+.5);}}
    static int top(int room){return -30-room*52;}
    static int height(int room){return room*8;}
    static int radius(int room){return new int[]{5,6,8,9,11}[room];}
    public static int stages(int room){return new int[]{1,1,2,2,3}[room];}
    static AABB bounds(BlockPos c){return new AABB(c.offset(-25,-5,-285),c.offset(31,59,12));}
    static BlockPos room(BlockPos c,int room){return c.offset(0,height(room),top(room));}
    public static List<Destination> candidates(int room){
        var list=new ArrayList<Destination>();int count=stages(room),r=radius(room);
        for(int stage=0;stage<count;stage++){
            int ring=stage==0?2:stage==1?5:9;
            for(int sign:new int[]{-1,1}){list.add(new Destination(sign*ring,0,0,stage));list.add(new Destination(0,0,sign*ring,stage));}
            
        }
        list.add(new Destination(r+3,0,0,count));list.add(new Destination(r+3,0,3,count));
        return List.copyOf(list);
    }
    static boolean safe(ServerLevel l,BlockPos base,Destination d){var at=base.offset(d.x,d.y,d.z);return l.getBlockState(at.below()).isFaceSturdy(l,at.below(),net.minecraft.core.Direction.UP)&&l.noCollision(new AABB(at.getX()+.2,at.getY(),at.getZ()+.2,at.getX()+.8,at.getY()+1.9,at.getZ()+.8))&&!l.containsAnyLiquid(new AABB(at));}
    static void build(ServerLevel l,BlockPos c){
        if(l.getBlockState(c.below(2)).is(Blocks.CHISELED_QUARTZ_BLOCK))return;
        if(!l.isEmptyBlock(c.below(2)))erase(l,bounds(c),c.below(2));
        box(l,c,-10,-1,-9,10,-1,10,Blocks.END_STONE_BRICKS);for(int x:new int[]{-9,9}){box(l,c,x,0,-8,x,6,8,Blocks.PURPUR_PILLAR);box(l,c,x,7,-8,x,7,8,Blocks.END_ROD);}
        for(int room=0;room<5;room++){
            var b=room(c,room);int r=radius(room),count=stages(room);
            // Floating terraced island, enclosing shells and lit hanging tower crowns.
            for(int y=-4;y<=-1;y++)box(l,b,-r-y-1,y,-r-y-1,r+y+1,y,r+y+1,Blocks.END_STONE);
            box(l,b,-r,-1,-r,r,-1,r,Blocks.END_STONE_BRICKS);
            shell(l,b,r,0,11+room,room);
            for(int sign:new int[]{-1,1}){put(l,b,sign*(r-1),-1,r-1,Blocks.END_STONE);put(l,b,sign*(r-1),0,r-1,Blocks.CHORUS_FLOWER);}
            for(int x:new int[]{-r,r})for(int z:new int[]{-r,r}){box(l,b,x,-2,z,x,16+room,z,Blocks.PURPUR_PILLAR);put(l,b,x,17+room,z,Blocks.END_ROD);}
            // Build safe landing pads before the decorative rods. None intersect a shell.
            for(var d:candidates(room)){box(l,b,d.x-1,d.y-1,d.z-1,d.x+1,d.y-1,d.z+1,Blocks.PURPUR_BLOCK);put(l,b,d.x,d.y-1,d.z,Blocks.SEA_LANTERN);}
            // Rune pedestals are flush with the floor and never obstruct movement.
            for(int x:new int[]{-r+2,r-2})for(int z:new int[]{-r+2,r-2})put(l,b,x,-1,z,Blocks.CHISELED_QUARTZ_BLOCK);
            put(l,b,0,-1,0,Blocks.GOLD_BLOCK);
            // Escape balcony and an enclosed bridge lead to the next tower's boarding alcove.
            int deck=0;box(l,b,r+2,deck-1,-4,24,deck-1,5,Blocks.END_STONE_BRICKS);
            for(int z=-4;z<=5;z++){put(l,b,24,deck,z,Blocks.PURPUR_PILLAR);put(l,b,r+2,deck,z,Blocks.END_ROD);}
            for(int z=-39;z<=0;z++){box(l,b,20,deck-1,z,23,deck-1,z,Blocks.PURPUR_BLOCK);put(l,b,20,deck,z,Blocks.PURPUR_PILLAR);put(l,b,23,deck,z,Blocks.PURPUR_PILLAR);}
            for(int side:new int[]{-1,1}){box(l,b,side*(r+5),8,-5,side*(r+5),10,-5,Blocks.PURPUR_PILLAR);put(l,b,side*(r+5),11,-5,Blocks.SHROOMLIGHT);}
        }
        // End-ship inspired final sanctuary: keel, sails, mast and mounted dragon ornament.
        var vault=c.offset(0,40,-270);box(l,vault,-10,-2,-10,10,-1,10,Blocks.PURPUR_BLOCK);
        for(int x:new int[]{-10,10})box(l,vault,x,0,-10,x,3,10,Blocks.PURPUR_PILLAR);
        box(l,vault,0,0,-7,0,13,-7,Blocks.PURPUR_PILLAR);box(l,vault,-7,6,-7,7,11,-7,Blocks.MAGENTA_STAINED_GLASS);
        for(int x:new int[]{-7,-3,3,7}){put(l,vault,x,0,-2,Blocks.PURPLE_SHULKER_BOX);put(l,vault,x,1,-2,Blocks.END_ROD);put(l,vault,x,0,5,Blocks.GOLD_BLOCK);}
        put(l,vault,0,1,-6,Blocks.DRAGON_HEAD);put(l,c,0,-2,0,Blocks.CHISELED_QUARTZ_BLOCK);
    }
    private static void shell(ServerLevel l,BlockPos b,int r,int bottom,int ceiling,int variant){
        for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++){
            if(Math.abs(x)==r||Math.abs(z)==r)for(int y=bottom;y<=ceiling;y++)put(l,b,x,y,z,y>=2&&y<=5&&Math.floorMod(x+z+variant,4)!=0?Blocks.PURPLE_STAINED_GLASS:y%4==0?Blocks.PURPUR_PILLAR:Blocks.PURPUR_BLOCK);
            put(l,b,x,ceiling,z,Blocks.PURPUR_BLOCK);
        }
    }
    private EndCityCourse(){}
}
