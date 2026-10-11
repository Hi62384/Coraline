package com.coraline.feast;
import static com.coraline.feast.ExpeditionActivities.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;

final class TreasureScenery {
    static BlockPos vaultDoor(BlockPos c,int id){return TreasureRoute.LANDMARKS.get(id).at(c).offset(6,0,7);}
    static BlockPos prize(BlockPos c,int id){return TreasureRoute.LANDMARKS.get(id).at(c).offset(0,-5,-6);}
    static void build(ServerLevel l,BlockPos c,long seed){box(l,c,-72,-2,-147,72,-1,12,Blocks.DIRT);box(l,c,-72,-1,-147,72,-1,12,Blocks.GRASS_BLOCK);
        box(l,c,-72,0,-147,72,3,-146,Blocks.FLOWERING_AZALEA_LEAVES);box(l,c,-72,0,-147,-71,3,12,Blocks.FLOWERING_AZALEA_LEAVES);box(l,c,71,0,-147,72,3,12,Blocks.FLOWERING_AZALEA_LEAVES);box(l,c,-72,0,11,72,3,12,Blocks.FLOWERING_AZALEA_LEAVES);
        for(int z=-140;z<=8;z++)for(int x=-2;x<=2;x++)put(l,c,x,-1,z,Blocks.MOSSY_STONE_BRICKS);
        // Winding branches form connected walks; their destinations are never highlighted.
        for(int id=0;id<TreasureRoute.LANDMARKS.size();id++){var m=TreasureRoute.LANDMARKS.get(id);int side=Integer.signum(m.x());for(int n=0;n<=Math.abs(m.x());n++){int z=m.z()+8+(int)Math.round(Math.sin(n*.15)*2);for(int dz=-1;dz<=1;dz++)put(l,c,n*side,-1,z+dz,Blocks.GRAVEL);}}
        var random=new java.util.Random(seed);for(int i=0;i<110;i++){int x=random.nextInt(135)-67,z=-random.nextInt(140);boolean near=false;for(var m:TreasureRoute.LANDMARKS)if(Math.hypot(x-m.x(),z-m.z())<11)near=true;if(near||Math.abs(x)<6)continue;put(l,c,x,0,z,i%3==0?Blocks.AZALEA:Blocks.GRASS);if(i%5==0){put(l,c,x,0,z,Blocks.MOSS_BLOCK);put(l,c,x,1,z,Blocks.FLOWERING_AZALEA);}}
        for(int z:new int[]{-10,-44,-92,-140})for(int x:new int[]{-66,66})ActivityScenery.tree(l,c,x,z);
        for(int z=-8;z>=-136;z-=16){ActivityScenery.lamp(l,c,4,z);put(l,c,-5,0,z,Blocks.CHERRY_SLAB);put(l,c,-6,0,z,Blocks.CHERRY_SLAB);}
        for(int id=0;id<TreasureRoute.LANDMARKS.size();id++)landmark(l,c,id);
        enrich(l,c);
        var gardens=TreasureRoute.gardens(seed);
        for(int id=0;id<gardens.size();id++)sealGarden(l,TreasureRoute.LANDMARKS.get(id).at(c),gardens.get(id));
        sign(l,c.offset(-4,1,3),Direction.SOUTH,"ADVENTURE ESTATE","Use your Treasure Note","Read the landmarks","No coordinates needed");
    }
    static Block sealFlower(int color){return new Block[]{Blocks.RED_TULIP,Blocks.BLUE_ORCHID,Blocks.WHITE_TULIP,Blocks.ALLIUM}[color];}
    static void sealGarden(ServerLevel l,BlockPos b,TreasureRoute.SealGarden fact){
        // Identical framed gardens at every landmark: colour AND lamp count solve
        // the verse together. Neither alone is unique; assignments change each visit.
        box(l,b,-9,-1,-8,-6,-1,5,Blocks.SMOOTH_STONE);
        for(int i=0;i<4;i++){
            int z=-6+i*3;
            put(l,b,-8,0,z,Blocks.STONE_BRICK_WALL);
            put(l,b,-8,1,z,i<fact.lamps()?Blocks.LANTERN:Blocks.AIR);
            put(l,b,-6,-1,z,Blocks.GRASS_BLOCK);put(l,b,-6,0,z,sealFlower(fact.flower()));
        }
        sign(l,b.offset(-8,0,6),Direction.SOUTH,"SEAL GARDEN","Petals and lamps","Both verses must agree");
    }
    private static void enrich(ServerLevel l,BlockPos c){
        // Garden rooms, flower ribbons and framed walking routes give the clues an estate to live in.
        for(var m:TreasureRoute.LANDMARKS){var b=m.at(c);
            for(int x=-10;x<=10;x++)for(int z:new int[]{-9,9}){if(Math.abs(x)<=3||z==9&&x>=5&&x<=7)continue;put(l,b,x,-1,z,Blocks.MOSS_BLOCK);put(l,b,x,0,z,(x+m.x())%3==0?Blocks.ALLIUM:Blocks.PINK_TULIP);if(Math.abs(x)==10){put(l,b,x,0,z,Blocks.STONE_BRICK_WALL);put(l,b,x,1,z,Blocks.LANTERN);}}
            for(int z=-7;z<=5;z+=4){put(l,b,-10,0,z,Blocks.FLOWERING_AZALEA_LEAVES);put(l,b,10,0,z,Blocks.FLOWERING_AZALEA_LEAVES);}
            // Front garden gate stays clear of each clue barrel and cellar stair.
            for(int x:new int[]{-2,2})box(l,b,x,0,10,x,3,10,Blocks.CHERRY_FENCE);
            box(l,b,-2,4,10,2,4,10,Blocks.CHERRY_SLAB);put(l,b,-2,3,10,Blocks.FLOWERING_AZALEA_LEAVES);put(l,b,2,3,10,Blocks.FLOWERING_AZALEA_LEAVES);
        }
        for(int z=-10;z>=-136;z-=7)for(int x:new int[]{-3,3}){boolean near=false;for(var m:TreasureRoute.LANDMARKS)if(Math.hypot(x-m.x(),z-m.z())<12)near=true;if(!near)put(l,c,x,0,z,z%2==0?Blocks.WHITE_TULIP:Blocks.ALLIUM);}
        for(int[] point:new int[][]{{-64,-42},{-61,-85},{-17,-52},{-19,-92},{16,-62},{62,-47},{62,-91},{-19,-133},{50,-136}}){var b=c.offset(point[0],0,point[1]);ActivityScenery.tree(l,b,0,0);for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)if(x*x+z*z>=8&&x*x+z*z<=12)put(l,b,x,0,z,Blocks.PINK_TULIP);}
        // A recognizable open crescent above the fountain, not a flat painted floor.
        var moon=TreasureRoute.LANDMARKS.get(0).at(c);box(l,moon,-2,4,0,2,9,0,Blocks.AIR);for(int[] p:new int[][]{{1,4},{0,4},{-1,5},{-2,6},{-2,7},{-1,8},{0,9},{1,9}})put(l,moon,p[0],p[1],0,Blocks.END_STONE_BRICKS);
        var gazebo=TreasureRoute.LANDMARKS.get(3).at(c);for(int x:new int[]{-5,-4,4,5})put(l,gazebo,x,0,6,Blocks.RED_TULIP);
        for(int id:new int[]{6,11}){var b=TreasureRoute.LANDMARKS.get(id).at(c);for(int x:new int[]{-3,3})box(l,b,x-1,1,4,x+1,3,4,Blocks.GLASS_PANE);Block roof=id==6?Blocks.GLASS:Blocks.GREEN_TERRACOTTA;for(int i=0;i<4;i++){box(l,b,-6+i,5+i,-6,-6+i,5+i,5,roof);box(l,b,6-i,5+i,-6,6-i,5+i,5,roof);}box(l,b,-2,9,-6,2,9,5,roof);if(id==11){box(l,b,4,5,-3,4,9,-3,Blocks.BRICKS);put(l,b,-3,0,1,Blocks.CHISELED_BOOKSHELF);put(l,b,3,0,1,Blocks.CHERRY_STAIRS);}}
        // Lit, layered treasure instead of a bare underground cube.
        for(int id:new int[]{6,7,11}){var b=TreasureRoute.LANDMARKS.get(id).at(c);for(int x:new int[]{-5,5}){box(l,b,x,-6,-10,x,-3,-10,Blocks.QUARTZ_PILLAR);put(l,b,x,-2,-10,Blocks.GOLD_BLOCK);put(l,b,x,-4,-9,Blocks.GOLD_BLOCK);put(l,b,x,-3,-9,Blocks.LANTERN);}for(int x=-3;x<=3;x++)put(l,b,x,-6,-9,x%2==0?Blocks.GOLD_BLOCK:Blocks.EMERALD_BLOCK);put(l,b,0,-5,-9,Blocks.DIAMOND_BLOCK);box(l,b,-2,-7,-5,2,-7,0,Blocks.RED_TERRACOTTA);}
    }
    static void landmark(ServerLevel l,BlockPos c,int id){var m=TreasureRoute.LANDMARKS.get(id);var b=m.at(c);
        switch(id){
            case 0->{ActivityScenery.fountain(l,b,0,0);for(int y=4;y<=8;y++){put(l,b,-1,y,0,Blocks.QUARTZ_BLOCK);if(y==4||y==8)put(l,b,0,y,0,Blocks.GOLD_BLOCK);}for(int x=-5;x<=5;x++)for(int z:new int[]{-5,5})put(l,b,x,0,z,Blocks.WHITE_TULIP);}
            case 1->{box(l,b,-9,-2,-4,9,-2,4,Blocks.STONE_BRICKS);box(l,b,-9,-1,-4,9,-1,4,Blocks.STONE_BRICKS);box(l,b,-8,-1,-3,8,-1,3,Blocks.WATER);box(l,b,-2,0,-6,2,0,6,Blocks.CRIMSON_SLAB);for(int z=-5;z<=5;z+=2){put(l,b,-3,0,z,Blocks.CRIMSON_FENCE);put(l,b,3,0,z,Blocks.CRIMSON_FENCE);}for(int x:new int[]{-3,3})for(int z:new int[]{-6,6}){put(l,b,x,0,z,Blocks.GOLD_BLOCK);put(l,b,x,1,z,Blocks.LANTERN);}}
            case 2->{box(l,b,-1,0,-1,1,10,1,Blocks.OAK_LOG);for(int y=7;y<=12;y++)for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)if(x*x+z*z+(y-9)*(y-9)<43)put(l,b,x,y,z,Blocks.OAK_LEAVES);for(int x:new int[]{-3,3}){put(l,b,x,6,0,Blocks.GOLD_BLOCK);hanging(l,b.offset(x,5,0));box(l,b,x,0,-2,x,0,2,Blocks.OAK_SLAB);}}
            case 3->{box(l,b,-5,-1,-5,5,-1,5,Blocks.SMOOTH_QUARTZ);for(int x:new int[]{-4,4})for(int z:new int[]{-4,4})box(l,b,x,0,z,x,4,z,Blocks.GOLD_BLOCK);box(l,b,-5,5,-5,5,5,5,Blocks.CHERRY_SLAB);for(int x=-5;x<=5;x++)put(l,b,x,0,-6,Blocks.RED_TULIP);hanging(l,b.offset(0,4,0));}
            case 4->{box(l,b,-3,0,-3,3,10,3,Blocks.STONE_BRICKS);box(l,b,-2,0,-2,2,8,2,Blocks.AIR);box(l,b,-1,0,3,1,2,3,Blocks.AIR);box(l,b,-2,7,4,2,9,4,Blocks.WHITE_CONCRETE);put(l,b,0,8,5,Blocks.BLACK_CONCRETE);put(l,b,0,9,5,Blocks.BLACK_CONCRETE);put(l,b,1,8,5,Blocks.BLACK_CONCRETE);box(l,b,-4,11,-4,4,11,4,Blocks.CUT_COPPER_SLAB);}
            case 5->{for(int i=0;i<3;i++){int x=(i-1)*5,h=3+i*2;box(l,b,x,0,0,x,h,0,Blocks.MUSHROOM_STEM);box(l,b,x-2,h,-2,x+2,h,2,Blocks.RED_MUSHROOM_BLOCK);put(l,b,x,h+1,0,Blocks.SHROOMLIGHT);}}
            case 6->{house(l,b,Blocks.SMOOTH_QUARTZ,Blocks.GLASS);for(int x=-4;x<=4;x+=2){put(l,b,x,0,-2,Blocks.FARMLAND);put(l,b,x,1,-2,Blocks.FLOWERING_AZALEA);}for(int z=-5;z<=5;z++)put(l,b,-6,0,z,Blocks.ALLIUM);}
            case 7->{box(l,b,-4,0,-4,4,6,4,Blocks.CRACKED_STONE_BRICKS);box(l,b,-3,0,-3,3,6,3,Blocks.AIR);box(l,b,-1,0,4,1,3,4,Blocks.AIR);for(int x=-4;x<=4;x+=2)box(l,b,x,7,-4,x,8,-4,Blocks.MOSSY_STONE_BRICKS);}
            case 8->{box(l,b,-2,-1,-2,2,0,2,Blocks.MOSSY_STONE_BRICKS);box(l,b,-1,0,-1,1,0,1,Blocks.WATER);for(int x:new int[]{-2,2})box(l,b,x,1,0,x,3,0,Blocks.OAK_FENCE);box(l,b,-3,4,-2,3,4,2,Blocks.DARK_OAK_SLAB);put(l,b,2,1,1,Blocks.CAULDRON);}
            case 9->{for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)put(l,b,x,-1,z,(x+z)%2==0?Blocks.WHITE_CONCRETE:Blocks.BLACK_CONCRETE);for(int x=-4;x<=4;x+=2){put(l,b,x,0,-4,Blocks.POLISHED_BLACKSTONE_WALL);put(l,b,x,0,4,Blocks.DIORITE_WALL);}box(l,b,-3,0,3,-3,2,3,Blocks.QUARTZ_PILLAR);put(l,b,-3,3,3,Blocks.CHISELED_QUARTZ_BLOCK);}
            case 10->{for(int x:new int[]{-5,0,5}){box(l,b,x,0,0,x,3,0,Blocks.OAK_LOG);box(l,b,x-2,3,-2,x+2,4,2,Blocks.OAK_LEAVES);put(l,b,x,3,2,Blocks.GOLD_BLOCK);}}
            case 11->house(l,b,Blocks.CHERRY_PLANKS,Blocks.GREEN_TERRACOTTA);
        }
        // Clues are physical, non-glowing barrels, tucked into the described search feature.
        put(l,m.note(c),0,0,0,Blocks.BARREL);sign(l,m.note(c).south(),Direction.SOUTH,"Folded parchment","Right-click to inspect");
        put(l,b,-7,0,7,Blocks.CHEST); // Tempting decoy; never a real storage inventory.
        if(id==6||id==7||id==11)vault(l,b);
    }
    private static void house(ServerLevel l,BlockPos b,Block wall,Block roof){box(l,b,-5,0,-5,5,4,4,wall);box(l,b,-4,0,-4,4,3,3,Blocks.AIR);box(l,b,-1,0,4,1,2,4,Blocks.AIR);box(l,b,-6,5,-6,6,5,5,roof);for(int x:new int[]{-5,5})box(l,b,x,1,-3,x,3,2,Blocks.GLASS);hanging(l,b.offset(0,3,0));}
    private static void vault(ServerLevel l,BlockPos b){box(l,b,-7,-8,-12,8,-1,1,Blocks.STONE_BRICKS);box(l,b,-6,-6,-11,7,-2,0,Blocks.AIR);box(l,b,-5,-7,-10,5,-7,-2,Blocks.CHISELED_STONE_BRICKS);
        for(int i=0;i<7;i++){box(l,b,5,-i-1,6-i,7,2-i,6-i,Blocks.AIR);for(int x=5;x<=7;x++)ActivityBlocks.set(l,b.offset(x,-i-1,6-i),Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.SOUTH),2);}
        box(l,b,5,0,7,7,2,7,Blocks.CHISELED_STONE_BRICKS);box(l,b,6,0,7,6,1,7,Blocks.IRON_BLOCK);sign(l,b.offset(6,2,8),Direction.SOUTH,"SEALED CELLAR","Five seals required");put(l,b,0,-6,-6,Blocks.GOLD_BLOCK);for(int x:new int[]{-5,4}){put(l,b,x,-6,-9,Blocks.GILDED_BLACKSTONE);put(l,b,x,-5,-9,Blocks.GOLD_BLOCK);put(l,b,x,-4,-9,Blocks.CHEST);}for(int z:new int[]{-10,-2})put(l,b,-6,-3,z,Blocks.SEA_LANTERN);
    }
    private static void hanging(ServerLevel l,BlockPos p){ActivityBlocks.set(l,p,Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true),2);}
    private TreasureScenery(){}
}
