package com.coraline.otherworld;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import com.coraline.registry.ModBlocks;

/** Finishes the lived-in rooms after the architectural shell and roof are complete. */
final class OtherWorldInterior {
    private OtherWorldInterior(){}

    static void decorate(ServerLevel level,BlockPos house){
        sitting(level,house);
        study(level,house);
        kitchen(level,house);
        supper(level,house);
        hall(level,house);
        upperRooms(level,house);
        bedroom(level,house);
        attic(level,house);
        clearWindowRecesses(level,house);
    }
    private static void put(ServerLevel l,BlockPos c,int x,int y,int z,Block block){
        BlockState state=block.defaultBlockState();
        if(block instanceof SlabBlock)state=state.setValue(SlabBlock.TYPE,SlabType.TOP);
        l.setBlock(c.offset(x,y,z),state,3);
    }
    private static void put(ServerLevel l,BlockPos c,int x,int y,int z,BlockState state){l.setBlock(c.offset(x,y,z),state,3);}
    private static void box(ServerLevel l,BlockPos c,int x0,int y0,int z0,int x1,int y1,int z1,Block block){
        for(int x=x0;x<=x1;x++)for(int y=y0;y<=y1;y++)for(int z=z0;z<=z1;z++)put(l,c,x,y,z,block);
    }
    private static BlockState stair(Block block,Direction facing){return block.defaultBlockState().setValue(StairBlock.FACING,facing);}
    private static BlockState slab(Block block){return block.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.TOP);}
    private static void rug(ServerLevel l,BlockPos c,int x0,int z0,int x1,int z1,Block edge,Block center,int y){
        for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++)
            put(l,c,x,y,z,x==x0||x==x1||z==z0||z==z1?edge:center);
    }
    private static void lamp(ServerLevel l,BlockPos c,int x,int y,int z){
        put(l,c,x,y,z,Blocks.SEA_LANTERN);
        for(int dx:new int[]{-1,1})put(l,c,x+dx,y,z,Blocks.DARK_OAK_SLAB);
        for(int dz:new int[]{-1,1})put(l,c,x,y,z+dz,Blocks.DARK_OAK_SLAB);
    }
    private static void shelf(ServerLevel l,BlockPos c,int x,int z,Block top){
        put(l,c,x,1,z,Blocks.BARREL);put(l,c,x,2,z,top);
    }

    private static void sitting(ServerLevel l,BlockPos c){
        // Calm, blue sitting room. The tunnel arrival and door-facing aisle stay open.
        for(int z=1;z<=14;z++)for(int y=1;y<=7;y++){
            if(z==8&&y<=3||z>=12&&z<=13&&y>=3&&y<=5)continue;
            put(l,c,-19,y,z,y<=2?Blocks.STRIPPED_DARK_OAK_WOOD:y==7?Blocks.SMOOTH_QUARTZ:Blocks.LIGHT_BLUE_TERRACOTTA);
        }
        for(int x=-19;x<=2;x++)for(int y=1;y<=7;y++){
            if(x>=-1&&x<=1&&y<=4||
                (x==-15||x==-14||x==-8||x==-7)&&y>=3&&y<=5)continue;
            put(l,c,x,y,15,y<=2?Blocks.STRIPPED_DARK_OAK_WOOD:y==7?Blocks.SMOOTH_QUARTZ:Blocks.LIGHT_BLUE_TERRACOTTA);
        }
        box(l,c,-18,1,8,-18,2,8,Blocks.AIR);
        put(l,c,-16,1,2,Blocks.AIR); // no exposed, smoking campfire
        rug(l,c,-15,5,-7,11,Blocks.CYAN_CARPET,Blocks.LIGHT_BLUE_CARPET,1);
        for(int x=-15;x<=-8;x++)put(l,c,x,1,13,ModBlocks.BLUE_VELVET_SOFA.get());
        for(int x=-13;x<=-11;x++)put(l,c,x,1,8,ModBlocks.LOW_MARBLE_TABLE.get());
        put(l,c,-7,1,6,ModBlocks.BRASS_FLOOR_LAMP.get());
        // A recessed, smoke-free hearth with a continuous mantel.
        box(l,c,-18,1,-1,-13,4,-1,Blocks.POLISHED_BLACKSTONE_BRICKS);
        box(l,c,-17,1,0,-14,3,0,Blocks.POLISHED_BLACKSTONE_BRICKS);
        box(l,c,-16,1,0,-15,2,0,Blocks.SHROOMLIGHT);
        box(l,c,-16,1,1,-15,2,1,Blocks.IRON_BARS);
        box(l,c,-18,4,0,-13,4,1,Blocks.POLISHED_BLACKSTONE_SLAB);
        box(l,c,-17,0,1,-14,0,2,Blocks.POLISHED_BLACKSTONE_BRICKS);
        // A framed and reachable little door, flush with the inner wall.
        for(int z:new int[]{7,9})for(int y=1;y<=3;y++)put(l,c,-19,y,z,Blocks.SMOOTH_QUARTZ);
        for(int z=7;z<=9;z++)put(l,c,-19,3,z,Blocks.CHISELED_QUARTZ_BLOCK);
        put(l,c,-19,2,8,Blocks.AIR);
        box(l,c,-18,1,7,-18,2,9,Blocks.AIR);
        put(l,c,-18,1,14,ModBlocks.FLOWER_CONSOLE.get());
        put(l,c,-11,7,8,Blocks.SEA_LANTERN);
        // Picture and wainscot in place of the bare striped wall.
        box(l,c,-19,1,1,-19,1,5,Blocks.STRIPPED_DARK_OAK_WOOD);
        box(l,c,-19,3,2,-19,4,4,Blocks.LIGHT_BLUE_TERRACOTTA);
    }

    private static void study(ServerLevel l,BlockPos c){
        // The piano entity occupies the center: retain a generous open performance area.
        for(int z=-13;z<=-3;z++)for(int y=1;y<=7;y++){
            if((z==-9||z==-8)&&y>=3&&y<=5)continue;
            put(l,c,-19,y,z,y<=2?Blocks.STRIPPED_DARK_OAK_WOOD:Blocks.BROWN_TERRACOTTA);
        }
        for(int x=-18;x<=-3;x++)for(int y=1;y<=7;y++){
            if((x==-15||x==-14||x==-8||x==-7)&&y>=3&&y<=5)continue;
            put(l,c,x,y,-13,y<=2?Blocks.STRIPPED_DARK_OAK_WOOD:Blocks.BROWN_TERRACOTTA);
        }
        // Close the eastern side against the hall. The locked front door is the
        // sole way in or out during Father's song.
        for(int z=-13;z<=-2;z++)for(int y=1;y<=7;y++)
            put(l,c,-2,y,z,y<=2?Blocks.STRIPPED_DARK_OAK_WOOD:Blocks.DARK_OAK_PLANKS);
        rug(l,c,-16,-11,-7,-4,Blocks.BROWN_TERRACOTTA,Blocks.RED_TERRACOTTA,0);
        for(int x=-19;x<=-3;x++)put(l,c,x,1,-2,Blocks.STRIPPED_DARK_OAK_WOOD);
        // Reinstate the script-controlled entrance after the wall treatment.
        put(l,c,-11,1,-2,Blocks.AIR);put(l,c,-11,2,-2,Blocks.AIR);
        box(l,c,-18,1,-13,-16,5,-13,Blocks.BOOKSHELF);
        box(l,c,-6,1,-13,-4,5,-13,Blocks.BOOKSHELF);
        box(l,c,-19,1,-6,-19,4,-4,Blocks.BOOKSHELF);
        for(int x=-18;x<=-16;x++)put(l,c,x,1,-4,ModBlocks.BLUE_VELVET_SOFA.get());
        put(l,c,-5,1,-9,ModBlocks.FLOWER_CONSOLE.get());
        put(l,c,-4,1,-9,ModBlocks.BRASS_FLOOR_LAMP.get());
        for(int x:new int[]{-13,-9})for(int y=1;y<=4;y++)put(l,c,x,y,-3,Blocks.STRIPPED_DARK_OAK_LOG);
        lamp(l,c,-11,7,-7);
        // Dark-oak door with a carved-looking frame and brass-colored lintel.
        for(int x:new int[]{-12,-10})for(int y=1;y<=3;y++)put(l,c,x,y,-2,Blocks.STRIPPED_DARK_OAK_LOG);
        box(l,c,-12,3,-2,-10,3,-2,Blocks.STRIPPED_DARK_OAK_WOOD);
        BlockState door=Blocks.DARK_OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING,Direction.SOUTH)
            .setValue(DoorBlock.HINGE,DoorHingeSide.LEFT).setValue(DoorBlock.OPEN,false);
        put(l,c,-11,1,-2,door.setValue(DoorBlock.HALF,DoubleBlockHalf.LOWER));
        put(l,c,-11,2,-2,door.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER));
    }

    private static void kitchen(ServerLevel l,BlockPos c){
        for(int x=5;x<=19;x++)for(int y=5;y<=7;y++)
            if(!((x==7||x==8||x==14||x==15)&&y<=5))put(l,c,x,y,-13,Blocks.CALCITE);
        for(int z=-13;z<=-5;z++)for(int y=3;y<=7;y++)
            if(!((z==-9||z==-8)&&y<=5))put(l,c,19,y,z,Blocks.CALCITE);
        for(int x=5;x<=17;x++){
            put(l,c,x,1,-13,Blocks.BIRCH_PLANKS);
            put(l,c,x,1,-12,Blocks.BIRCH_PLANKS);
        }
        for(int x:new int[]{5,6,7,15,16,17})put(l,c,x,4,-12,Blocks.BARREL);
        put(l,c,6,1,-12,Blocks.SMOKER);
        put(l,c,7,1,-12,Blocks.BLAST_FURNACE);
        put(l,c,13,1,-12,Blocks.CAULDRON);
        put(l,c,15,1,-12,Blocks.BARREL);
        put(l,c,16,2,-12,Blocks.POTTED_RED_TULIP);
        for(int z=-11;z<=-7;z++){
            put(l,c,18,1,z,Blocks.BIRCH_PLANKS);
            put(l,c,19,1,z,Blocks.BIRCH_PLANKS);
        }
        put(l,c,18,2,-9,Blocks.CAKE);
        // A small breakfast table replaces the former wall-like island and
        // peninsula; the middle of this narrow room stays walkable.
        for(int x=10;x<=11;x++)put(l,c,x,1,-9,ModBlocks.LOW_MARBLE_TABLE.get());
        put(l,c,9,1,-8,stair(Blocks.BIRCH_STAIRS,Direction.EAST));
        put(l,c,12,1,-8,stair(Blocks.BIRCH_STAIRS,Direction.WEST));
        for(int x:new int[]{5,8})put(l,c,x,3,-13,Blocks.BARREL);
        put(l,c,17,2,-11,Blocks.BARREL);put(l,c,17,3,-11,Blocks.POTTED_DANDELION);
        lamp(l,c,10,7,-9);
    }

    private static void supper(ServerLevel l,BlockPos c){
        // Keep the central table/chair prop area empty and dress the perimeter.
        for(int z=-3;z<=15;z++)for(int y=1;y<=7;y++){
            if((z==-1||z==0||z==12||z==13)&&y>=3&&y<=5)continue;
            put(l,c,19,y,z,y<=2?Blocks.STRIPPED_DARK_OAK_WOOD:Blocks.BROWN_TERRACOTTA);
        }
        for(int x=5;x<=18;x++)for(int y=1;y<=7;y++){
            if((x==7||x==8||x==14||x==15)&&y>=3&&y<=5)continue;
            put(l,c,x,y,15,y<=2?Blocks.STRIPPED_DARK_OAK_WOOD:Blocks.BROWN_TERRACOTTA);
        }
        rug(l,c,7,-2,15,5,Blocks.BROWN_TERRACOTTA,Blocks.RED_TERRACOTTA,0);
        for(int z=4;z<=12;z++)put(l,c,19,1,z,Blocks.STRIPPED_DARK_OAK_WOOD);
        for(int x=6;x<=17;x++)put(l,c,x,1,15,Blocks.STRIPPED_DARK_OAK_WOOD);
        put(l,c,16,1,10,ModBlocks.FLOWER_CONSOLE.get());
        put(l,c,17,1,10,ModBlocks.CHERRY_SIDEBOARD.get());
        put(l,c,7,1,11,ModBlocks.FLOWER_CONSOLE.get());
        put(l,c,8,1,11,ModBlocks.CHERRY_SIDEBOARD.get());
        // Matching wall cabinets flank the supper table without narrowing its aisle.
        for(int z:new int[]{3,10}){
            box(l,c,18,1,z,18,4,z+2,Blocks.DARK_OAK_PLANKS);
            box(l,c,18,2,z,18,3,z+2,Blocks.BARREL);
        }
        lamp(l,c,10,7,0);
    }

    private static void hall(ServerLevel l,BlockPos c){
        // Clear, continuous access around the foot of the main stair and into the kitchen.
        for(int x=-1;x<=1;x++)for(int z=13;z<=14;z++){
            put(l,c,x,1,z,Blocks.AIR);put(l,c,x,2,z,Blocks.AIR);
        }
        for(int z=6;z<=9;z++)for(int y=1;y<=3;y++)put(l,c,3,y,z,Blocks.AIR);
        for(int z=-8;z<=-6;z++)for(int y=1;y<=3;y++)put(l,c,3,y,z,Blocks.AIR);
        for(int z=-8;z<=-6;z++)for(int y=1;y<=3;y++)put(l,c,4,y,z,Blocks.AIR);
        // The stair and its supported rails are built once by the shell.
    }

    private static void upperRooms(ServerLevel l,BlockPos c){
        // A central landing branches into a library, a parlor and a sewing room.
        for(int z=-13;z<=14;z++)for(int y=9;y<=14;y++)
            if(!((z>=-9&&z<=-8||z>=3&&z<=4)&&y<=11))put(l,c,-3,y,z,Blocks.SMOOTH_QUARTZ);
        for(int x=-19;x<=-4;x++)for(int y=9;y<=14;y++)
            if(!(x>=-11&&x<=-10&&y<=11))put(l,c,x,y,1,Blocks.SMOOTH_QUARTZ);
        for(int z=5;z<=15;z++)for(int y=9;y<=14;y++)
            if(!(z>=8&&z<=9&&y<=11))put(l,c,4,y,z,Blocks.SMOOTH_QUARTZ);
        for(int y=9;y<=14;y++){
            put(l,c,-3,y,1,Blocks.SMOOTH_QUARTZ);
            put(l,c,4,y,4,Blocks.SMOOTH_QUARTZ);
        }
        // West library: wrap shelves around a reading island, leaving both doors free.
        rug(l,c,-17,-11,-7,-2,Blocks.BLUE_CARPET,Blocks.LIGHT_BLUE_CARPET,9);
        box(l,c,-18,9,-13,-16,12,-13,Blocks.BOOKSHELF);
        box(l,c,-19,9,-6,-19,12,-5,Blocks.BOOKSHELF);
        box(l,c,-18,9,-3,-15,11,-3,Blocks.BOOKSHELF);
        for(int x=-15;x<=-12;x++)put(l,c,x,9,-6,ModBlocks.BLUE_VELVET_SOFA.get());
        for(int x=-14;x<=-13;x++)put(l,c,x,9,-8,ModBlocks.LOW_MARBLE_TABLE.get());
        lamp(l,c,-12,14,-8);
        // South-west parlor and sewing nook.
        rug(l,c,-17,4,-7,12,Blocks.MAGENTA_CARPET,Blocks.PINK_CARPET,9);
        for(int x=-16;x<=-11;x++)put(l,c,x,9,13,ModBlocks.ROSE_VELVET_SOFA.get());
        put(l,c,-16,9,5,ModBlocks.FLOWER_CONSOLE.get());
        box(l,c,-7,9,12,-5,12,12,Blocks.BOOKSHELF);
        lamp(l,c,-11,14,8);
        // East-front conservatory, with a clear route to the balcony opening.
        rug(l,c,7,7,17,13,Blocks.GREEN_CARPET,Blocks.LIME_CARPET,9);
        for(int x=8;x<=16;x+=4){put(l,c,x,9,13,Blocks.BARREL);put(l,c,x,10,13,Blocks.POTTED_FLOWERING_AZALEA);}
        for(int z:new int[]{7,11})put(l,c,18,9,z,stair(Blocks.BIRCH_STAIRS,Direction.WEST));
        box(l,c,8,9,6,11,9,6,Blocks.BIRCH_PLANKS);
        put(l,c,9,10,6,Blocks.POTTED_AZURE_BLUET);
        lamp(l,c,12,14,10);
        // Flush ceiling lights keep the landing clear and replace the former
        // row of barrel-and-lantern posts.
        for(int z=-10;z<=0;z+=5)put(l,c,2,15,z,Blocks.PEARLESCENT_FROGLIGHT);
        for(int z=-11;z<=0;z++)for(int x=0;x<=1;x++)
            put(l,c,x,9,z,z==-11||z==0?Blocks.BLUE_CARPET:Blocks.LIGHT_BLUE_CARPET);
        put(l,c,-3,11,-5,ModBlocks.BUTTON_ART_PANEL.get());
        put(l,c,-3,11,-2,ModBlocks.MODERN_WALL_LIGHT.get());
        put(l,c,-3,11,10,ModBlocks.MOON_ART_PANEL.get());
        put(l,c,-3,11,13,ModBlocks.MODERN_WALL_LIGHT.get());
        // The west parlor door opens onto the actual stair landing, not the void
        // above the stairwell. Keep a continuous two-block-high passage.
        for(int z=3;z<=4;z++)for(int x=-3;x<=-2;x++){
            put(l,c,x,8,z,Blocks.CHERRY_PLANKS);
            put(l,c,x,9,z,Blocks.AIR);put(l,c,x,10,z,Blocks.AIR);
        }
    }

    private static void bedroom(ServerLevel l,BlockPos c){
        // Keep the bed unobstructed and frame its canopy rather than walling it in.
        for(int z=-13;z<=3;z++)for(int y=9;y<=14;y++){
            if((z==-9||z==-8||z==-1||z==0)&&y>=10&&y<=12)continue;
            put(l,c,19,y,z,Blocks.PURPLE_TERRACOTTA);
        }
        for(int x=5;x<=18;x++)for(int y=9;y<=14;y++){
            if((x==7||x==8||x==14||x==15)&&y>=10&&y<=12)continue;
            put(l,c,x,y,-13,Blocks.PURPLE_TERRACOTTA);
        }
        rug(l,c,8,-10,15,0,Blocks.PURPLE_CARPET,Blocks.MAGENTA_CARPET,9);
        // Replace the overbearing floating canopy and barrel posts with a low,
        // symmetrical bed setting and fitted storage along the outer wall.
        for(int x=10;x<=12;x++)for(int z=-9;z<=-5;z++)put(l,c,x,14,z,Blocks.AIR);
        for(int x:new int[]{10,12})for(int z:new int[]{-9,-5})for(int y=9;y<=13;y++)put(l,c,x,y,z,Blocks.AIR);
        put(l,c,9,9,-7,ModBlocks.FLOWER_CONSOLE.get());
        put(l,c,13,9,-7,ModBlocks.BRASS_FLOOR_LAMP.get());
        box(l,c,17,9,-11,18,11,-9,Blocks.AIR);
        for(int z=-11;z<=-9;z++)put(l,c,18,9,z,ModBlocks.CHERRY_SIDEBOARD.get());
        for(int x=6;x<=8;x++)put(l,c,x,9,-10,ModBlocks.CHERRY_SIDEBOARD.get());
        put(l,c,7,9,-8,ModBlocks.FLOWER_CONSOLE.get());
        // Vanity, dresser, soft sitting corner, and framed wall details.
        box(l,c,16,9,0,18,9,0,Blocks.CHERRY_PLANKS);
        for(int x=16;x<=18;x++)put(l,c,x,10,0,Blocks.PINK_STAINED_GLASS);
        put(l,c,17,11,0,Blocks.PEARLESCENT_FROGLIGHT);
        for(int x=16;x<=17;x++)put(l,c,x,9,-2,stair(Blocks.CHERRY_STAIRS,Direction.NORTH));
        for(int x=9;x<=13;x++)put(l,c,x,9,2,ModBlocks.ROSE_VELVET_SOFA.get());
        put(l,c,11,9,0,ModBlocks.LOW_MARBLE_TABLE.get());
        put(l,c,16,9,3,ModBlocks.BRASS_FLOOR_LAMP.get());
        put(l,c,19,11,-4,ModBlocks.MOON_ART_PANEL.get());
        put(l,c,19,11,2,ModBlocks.BUTTON_ART_PANEL.get());
        lamp(l,c,12,14,-1);
        // The bed is installed last by OtherWorldEstate.build.
    }

    private static void attic(ServerLevel l,BlockPos c){
        // Retire the broken interior flight. The external side stairs are the
        // attic's only approach, even when an older estate is rebuilt in place.
        for(int z=-11;z<=-5;z++){
            for(int y=9;y<=14;y++)put(l,c,-5,y,z,Blocks.AIR);
            if(z>=-8)for(int y=9;y<=14;y++)put(l,c,-6,y,z,Blocks.AIR);
            put(l,c,-5,15,z,Blocks.DARK_OAK_PLANKS);
        }
        box(l,c,-8,16,-4,-2,19,-4,Blocks.STRIPPED_DARK_OAK_WOOD);
        box(l,c,-5,16,-4,-5,17,-4,Blocks.STRIPPED_DARK_OAK_WOOD);
        // A short roof dormer makes a full-height, weatherproof connection
        // from the attic room to the exterior landing at y=15.
        for(int x=-1;x<=21;x++)for(int z=6;z<=8;z++){
            put(l,c,x,15,z,Blocks.DARK_OAK_PLANKS);
            put(l,c,x,16,z,Blocks.AIR);put(l,c,x,17,z,Blocks.AIR);
        }
        for(int x=16;x<=21;x++){
            for(int z:new int[]{5,9})for(int y=16;y<=19;y++)
                put(l,c,x,y,z,y==19?Blocks.SMOOTH_QUARTZ:Blocks.CHERRY_PLANKS);
            for(int z=5;z<=9;z++)put(l,c,x,20,z,Blocks.DEEPSLATE_TILES);
        }
        for(int z=5;z<=9;z++)for(int y=16;y<=19;y++)
            put(l,c,21,y,z,z==7&&y<=17?Blocks.AIR:Blocks.CHERRY_PLANKS);
        for(int z=6;z<=8;z++)for(int y=16;y<=18;y++)put(l,c,22,y,z,Blocks.AIR);
        BlockState door=Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING,Direction.EAST)
            .setValue(DoorBlock.OPEN,false).setValue(DoorBlock.HINGE,DoorHingeSide.LEFT);
        put(l,c,21,16,7,door.setValue(DoorBlock.HALF,DoubleBlockHalf.LOWER));
        put(l,c,21,17,7,door.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER));
        for(int x=-9;x<=-1;x++)for(int z=-3;z<=8;z++)put(l,c,x,15,z,Blocks.DARK_OAK_PLANKS);
        for(int z=0;z<=7;z+=3)put(l,c,-8,16,z,Blocks.BARREL);
        lamp(l,c,-4,19,1);
    }
    private static void clearWindowRecesses(ServerLevel l,BlockPos c){
        // No bookshelf, wall treatment or cabinet may occupy the inside face
        // of a glazed exterior opening. Finish the glass after room dressing.
        for(int y:new int[]{3,4,5,10,11,12}){
            for(int x:new int[]{-15,-14,-8,-7,7,8,14,15})for(int z:new int[]{-14,16}){
                if(z==16&&x>=-8&&x<=8&&y<=5)continue;
                put(l,c,x,y,z,Blocks.YELLOW_STAINED_GLASS);
                put(l,c,x,y,z==16?15:-13,Blocks.AIR);
            }
            for(int z:new int[]{-9,-8,-1,0,12,13})for(int x:new int[]{-20,20}){
                if(x==-20&&z>=7&&z<=9&&y<=5)continue;
                if(x==-20&&z>=12)continue; // turret intersects this bay
                put(l,c,x,y,z,Blocks.YELLOW_STAINED_GLASS);
                put(l,c,x==20?19:-19,y,z,Blocks.AIR);
            }
        }
    }
}
