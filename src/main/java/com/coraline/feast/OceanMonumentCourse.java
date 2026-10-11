package com.coraline.feast;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Six authored, sealed aquatic chambers. Geometry is also used by checkpoint tests. */
public final class OceanMonumentCourse {
    public static final int LENGTH=44, SECTIONS=6;
    public static final int VAULT_Z=-278;
    public static int start(int section){return -12-section*LENGTH;}
    public static int floor(int section){return section>=4?8:0;}
    public static int exitHeight(int section){return section>=3?8:0;}
    public static int doorway(int section){return section%2==0?-6:6;}
    public static AABB bounds(BlockPos c){return new AABB(c.offset(-16,-3,-291),c.offset(17,19,12));}
    public static Vec3 guardian(int section,int slot){
        return new Vec3(slot%2==0?-10.5:10.5,section==3?3+slot*5:floor(section)+3.5,start(section)-12-slot*13+.5);
    }
    public static int guardians(int section){return section==0?1:section==2||section==5?3:2;}
    static void put(ServerLevel l,BlockPos c,int x,int y,int z,Block b){ActivityBlocks.set(l,c.offset(x,y,z),b.defaultBlockState(),2);}
    static void box(ServerLevel l,BlockPos c,int x0,int y0,int z0,int x1,int y1,int z1,Block b){
        for(int x=x0;x<=x1;x++)for(int y=y0;y<=y1;y++)for(int z=z0;z<=z1;z++)put(l,c,x,y,z,b);
    }
    public static void build(ServerLevel l,BlockPos c){
        if(l.getBlockState(c.below(2)).is(Blocks.BLUE_GLAZED_TERRACOTTA))return;
        // Build the complete shell before adding water; no cascading water into the void.
        for(int z=-290;z<=10;z++)for(int x=-14;x<=14;x++){
            put(l,c,x,-1,z,Math.abs(x)==14?Blocks.DARK_PRISMARINE:(x%6==0&&z%6==0)?Blocks.SEA_LANTERN:Blocks.PRISMARINE_BRICKS);
            put(l,c,x,17,z,(Math.abs(x)==14||z%11==0)?Blocks.DARK_PRISMARINE:Blocks.PRISMARINE);
            if(Math.abs(x)==14||z==-290||z==10)for(int y=0;y<17;y++)
                put(l,c,x,y,z,(y==2||y==13)?Blocks.GOLD_BLOCK:(y>=5&&y<=10&&Math.floorMod(z,11)>2)?Blocks.CYAN_STAINED_GLASS:Blocks.PRISMARINE_BRICKS);
        }
        // Dry reception gallery and dry vault are separated by full-height retaining walls.
        for(int z:new int[]{-11,-277})box(l,c,-13,0,z,13,16,z,Blocks.PRISMARINE_BRICKS);
        for(int z=-276;z<=-12;z++)for(int x=-13;x<=13;x++)for(int y=0;y<17;y++)put(l,c,x,y,z,Blocks.WATER);
        for(int section=0;section<SECTIONS;section++){
            int top=start(section),bottom=top-LENGTH+1,f=floor(section);
            if(f>0)box(l,c,-13,0,bottom,13,f-1,top,Blocks.PRISMARINE);
            // Sealed thresholds admit only a framed, offset doorway. Doors compel weaving.
            int door=doorway(section),height=exitHeight(section);
            box(l,c,-13,f,bottom,13,16,bottom,Blocks.DARK_PRISMARINE);
            box(l,c,door-1,height,bottom,door+1,height+3,bottom,Blocks.WATER);
            for(int x=door-2;x<=door+2;x++)put(l,c,x,height+4,bottom,Blocks.SEA_LANTERN);
            // Repeating structural ribs vary with chamber identity, not identical empty boxes.
            for(int z=top-4;z>bottom+2;z-=11){
                for(int side:new int[]{-1,1}){
                    box(l,c,side*12,f,z,side*12,14,z,section==2?Blocks.QUARTZ_PILLAR:Blocks.PRISMARINE_BRICKS);
                    put(l,c,side*12,f+2,z,Blocks.SEA_LANTERN);
                    put(l,c,side*12,13,z,Blocks.GOLD_BLOCK);
                    // Coral and treasure niches remain outside the swim route.
                    put(l,c,side*11,f,z+2,Blocks.BRAIN_CORAL_BLOCK);
                    put(l,c,side*11,f+1,z+2,Blocks.BRAIN_CORAL);
                    put(l,c,side*11,f,z-2,Blocks.TUBE_CORAL_BLOCK);
                    put(l,c,side*11,f+1,z-2,Blocks.SEA_PICKLE);
                    if(section==5)put(l,c,side*10,f,z+3,Blocks.GOLD_BLOCK);
                }
                for(int x=-11;x<=11;x++)if(Math.abs(x)>8||section==2)put(l,c,x,15,z,Blocks.PRISMARINE_BRICKS);
            }
            // Gold/lantern inlays show the route but never replace solid cover with glass.
            for(int z=top;z>bottom;z--)put(l,c,0,f-1,z,z%5==0?Blocks.SEA_LANTERN:Blocks.CUT_COPPER);
            if(section==3){
                // The final threshold is eight blocks higher. Two shelves force a genuine ascent.
                // Shield the approach while the player begins rising; never expose the
                // entire climb to an uninterrupted beam from the lower balcony.
                box(l,c,-8,0,top-8,8,4,top-8,Blocks.PRISMARINE_BRICKS);
                box(l,c,5,0,top-21,7,16,top-19,Blocks.PRISMARINE_BRICKS);
                box(l,c,-13,5,top-24,3,5,top-13,Blocks.PRISMARINE_BRICKS);
                box(l,c,-3,10,top-40,13,10,top-28,Blocks.PRISMARINE_BRICKS);
                for(int y=0;y<16;y++)put(l,c,-11,y,top-33,y%4==0?Blocks.SEA_LANTERN:Blocks.WATER);
                put(l,c,10,-1,top-20,Blocks.SOUL_SAND);
                for(int y=0;y<16;y++)put(l,c,10,y,top-20,Blocks.BUBBLE_COLUMN);
            }else{
                // Broad alternating full-height cover. Every exposure has a nearby refuge.
                for(int n=0;n<6;n++){
                    int z=top-5-n*6,side=n%2==0?-1:1;
                    int x=side*(section==2?4:3);
                    box(l,c,x-1,f,z-1,x+1,16,z+1,section==5?Blocks.DARK_PRISMARINE:Blocks.PRISMARINE_BRICKS);
                    put(l,c,x,f+1,z+2,Blocks.SEA_LANTERN);
                    if(section==1||section==5)box(l,c,side<0?-13:6,f,z,side<0?-6:13,16,z,Blocks.PRISMARINE_BRICKS);
                }
            }
        }
        // Section three's last guardian looked straight down the final approach.
        // A full-height screen protects the left doorway without obstructing it.
        box(l,c,-8,0,start(2)-42,-8,16,start(2)-32,Blocks.PRISMARINE_BRICKS);
        box(l,c,-4,0,start(2)-42,-4,16,start(2)-32,Blocks.PRISMARINE_BRICKS);
        // Raised dry treasure floor. Nothing needs a water-breathing effect after success.
        box(l,c,-13,0,-289,13,7,-278,Blocks.PRISMARINE_BRICKS);
        for(int z=-289;z<=-278;z++)for(int x=-13;x<=13;x++){
            put(l,c,x,7,z,((x+z)%4==0)?Blocks.GOLD_BLOCK:Blocks.DARK_PRISMARINE);
            if(Math.abs(x)>8&&z%3==0){put(l,c,x,8,z,Blocks.GOLD_BLOCK);if(x%2==0)put(l,c,x,9,z,Blocks.SEA_LANTERN);}
        }
        box(l,c,-1,8,-282,1,8,-280,Blocks.PRISMARINE_BRICKS);
        put(l,c,0,8,-281,Blocks.GOLD_BLOCK);
        for(int side:new int[]{-1,1}){
            statue(l,c,side*8,0,-5);statue(l,c,side*7,8,-286);
            for(int z:new int[]{0,6}){box(l,c,side*9,0,z,side*9,5,z,Blocks.PRISMARINE_BRICKS);put(l,c,side*9,6,z,Blocks.SEA_LANTERN);}
        }
        gate(l,c,false);cyclingCover(l,c,0);put(l,c,0,-2,0,Blocks.BLUE_GLAZED_TERRACOTTA);
    }
    private static void statue(ServerLevel l,BlockPos c,int x,int y,int z){
        box(l,c,x-1,y,z-1,x+1,y,z+1,Blocks.GOLD_BLOCK);
        box(l,c,x-1,y+1,z-1,x+1,y+3,z+1,Blocks.PRISMARINE);
        put(l,c,x,y+2,z+2,Blocks.SEA_LANTERN);
        for(int side:new int[]{-1,1})put(l,c,x+side*2,y+2,z,Blocks.DARK_PRISMARINE);
        put(l,c,x,y+4,z,Blocks.GOLD_BLOCK);
    }
    static void gate(ServerLevel l,BlockPos c,boolean open){
        // A sealed viewing window keeps the airlock dry. Intro completion places the player in water.
        box(l,c,-1,0,-11,1,3,-11,Blocks.CYAN_STAINED_GLASS);
    }
    static void openVault(ServerLevel l,BlockPos c){
        // Supported wall signs are a vanilla, non-colliding water stop. The player
        // can swim through this narrow airlock into the dry gallery without a teleport.
        var seal=Blocks.WARPED_WALL_SIGN.defaultBlockState()
            .setValue(net.minecraft.world.level.block.WallSignBlock.FACING,net.minecraft.core.Direction.EAST);
        for(int y=8;y<=11;y++)ActivityBlocks.set(l,c.offset(5,y,-277),seal,2);
        for(int y=8;y<=12;y++)put(l,c,4,y,-277,Blocks.SEA_LANTERN);
    }
    static void cyclingCover(ServerLevel l,BlockPos c,long tick){
        int z=start(4)-22;boolean left=(tick/80)%2==0;
        for(int side:new int[]{-1,1})box(l,c,side<0?-10:8,8,z,side<0?-8:10,15,z,((side<0)==left)?Blocks.PRISMARINE_BRICKS:Blocks.WATER);
    }
    static void remove(ServerLevel l,BlockPos c){
        if(!l.getBlockState(c.below(2)).is(Blocks.BLUE_GLAZED_TERRACOTTA))return;
        for(int y=17;y>=-2;y--)for(int z=-290;z<=10;z++)for(int x=-14;x<=14;x++){
            var at=c.offset(x,y,z);if(!l.isEmptyBlock(at))ActivityBlocks.set(l,at,Blocks.AIR.defaultBlockState(),2);
        }
    }
    private OceanMonumentCourse(){}
}
