package com.coraline.secrets;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import com.coraline.registry.ModBlocks;

/** Authored details around a common sealed access envelope. */
public final class SecretScenery {
    public static void exterior(ServerPlayer p,BlockPos c,int index,int roof){
        var random=new java.util.Random(c.asLong()^index);int variant=Math.floorMod(index/12,4);
        var trim=new net.minecraft.world.level.block.Block[]{Blocks.STRIPPED_SPRUCE_LOG,Blocks.POLISHED_ANDESITE,Blocks.CUT_COPPER,Blocks.CHISELED_STONE_BRICKS}[variant];
        for(int x=-6;x<=6;x++)for(int z=-7;z<=6;z++)if(Math.abs(x)==6||z==-7)for(int y=-3;y<0;y++)SecretWorld.place(p,c.offset(x,y,z),Blocks.STONE_BRICKS);
        for(int x:new int[]{-5,5})for(int z:new int[]{-5,5})for(int y=0;y<roof;y++)SecretWorld.place(p,c.offset(x,y,z),trim);
        for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)if(Math.abs(x)==6||Math.abs(z)==6)SecretWorld.place(p,c.offset(x,roof,z),Blocks.DARK_OAK_SLAB);
        // Entry canopy and paired lamps keep the access lane clear.
        for(int x=-2;x<=2;x++)for(int z=-7;z<=-5;z++)SecretWorld.place(p,c.offset(x,3,z),variant%2==0?Blocks.RED_WOOL:Blocks.PURPLE_WOOL);
        for(int x:new int[]{-2,2}){for(int y=0;y<3;y++)SecretWorld.place(p,c.offset(x,y,-7),Blocks.SPRUCE_FENCE);SecretWorld.place(p,c.offset(x,0,-6),Blocks.BARREL);SecretWorld.place(p,c.offset(x,1,-6),Blocks.LANTERN);}
        // Window boxes and timber banding, varied without adding texture assets.
        for(int x:new int[]{-6,6})for(int z:new int[]{-3,0,3}){SecretWorld.place(p,c.offset(x,0,z),Blocks.MOSS_BLOCK);SecretWorld.place(p,c.offset(x,1,z),random.nextBoolean()?Blocks.POTTED_FERN:Blocks.POTTED_AZALEA);}
        if(variant==0){for(int y=0;y<3;y++)for(int x=-4+y;x<=4-y;x++)for(int z=-4;z<=4;z++)SecretWorld.place(p,c.offset(x,roof+1+y,z),Blocks.SPRUCE_PLANKS);}
        else if(variant==1){for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(Math.abs(x)==2||Math.abs(z)==2)for(int y=1;y<=3;y++)SecretWorld.place(p,c.offset(x,roof+y,z),y==2?Blocks.TINTED_GLASS:trim);for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)SecretWorld.place(p,c.offset(x,roof+4,z),Blocks.DARK_OAK_SLAB);}
        else if(variant==2){for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)SecretWorld.place(p,c.offset(x,roof+1,z),(x+z)%2==0?Blocks.OXIDIZED_COPPER:Blocks.WAXED_COPPER_BLOCK);}
        else {for(int y=1;y<=4;y++)for(int x:new int[]{-4,4})for(int z:new int[]{-4,4})SecretWorld.place(p,c.offset(x,roof+y,z),y==4?Blocks.SEA_LANTERN:Blocks.STONE_BRICK_WALL);}
    }
    public static void decorate(ServerPlayer p,BlockPos c,int style,int roof){
        switch(style){
            case 0->{for(int y=0;y<4;y++)for(int x=-5+y;x<=5-y;x++)for(int z=-5;z<=5;z++)if(x==-5+y||x==5-y)SecretWorld.place(p,c.offset(x,roof+y,z),Blocks.DARK_OAK_PLANKS);for(int x:new int[]{-3,3})SecretWorld.place(p,c.offset(x,0,-6),Blocks.POTTED_RED_TULIP);}
            case 1->{for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)SecretWorld.place(p,c.offset(x,roof,z),Blocks.GLASS);for(int x:new int[]{-5,5})for(int z=-3;z<=3;z++)for(int y=1;y<5;y++)SecretWorld.place(p,c.offset(x,y,z),Blocks.LIGHT_BLUE_STAINED_GLASS);for(int x:new int[]{-4,4})for(int z=-3;z<=3;z+=2)SecretWorld.place(p,c.offset(x,2,z),Blocks.POTTED_AZALEA);}
            case 2,6->{for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)if((Math.abs(x)==5||Math.abs(z)==5)&&(x+z)%2==0)SecretWorld.place(p,c.offset(x,roof+1,z),Blocks.STONE_BRICK_WALL);for(int x:new int[]{-3,3})for(int z:new int[]{-3,3})SecretWorld.place(p,c.offset(x,roof,z),Blocks.SEA_LANTERN);if(style==6){SecretWorld.place(p,c.offset(0,roof+1,0),Blocks.AMETHYST_BLOCK);SecretWorld.place(p,c.offset(0,roof+2,0),Blocks.LIGHTNING_ROD);}}
            case 3->{for(int x:new int[]{-4,4})for(int z=-3;z<=3;z+=3){SecretWorld.place(p,c.offset(x,0,z),Blocks.LOOM);SecretWorld.place(p,c.offset(x,1,z),ModBlocks.DOLL.get());}for(int x=-3;x<=3;x++)SecretWorld.place(p,c.offset(x,4,4),x%2==0?Blocks.RED_WOOL:Blocks.WHITE_WOOL);}
            case 4->{for(int x:new int[]{-4,4})for(int z=-3;z<=3;z++)for(int y=0;y<4;y++)SecretWorld.place(p,c.offset(x,y,z),Blocks.BOOKSHELF);SecretWorld.place(p,c.offset(0,1,3),Blocks.CANDLE);}
            case 5->{for(int x:new int[]{-4,4})for(int z=-3;z<=3;z+=2)for(int y=0;y<3;y++)SecretWorld.place(p,c.offset(x,y,z),Blocks.BARREL);for(int x=-2;x<=2;x++)for(int y=2;y<=5;y++)if(Math.abs(x)+Math.abs(y-3)==2)SecretWorld.place(p,c.offset(x,y,4),Blocks.GOLD_BLOCK);}
            case 7->{for(int z:new int[]{-3,0,3})for(int x:new int[]{-4,4})for(int y=0;y<5;y++)SecretWorld.place(p,c.offset(x,y,z),Blocks.OAK_LOG);for(int z:new int[]{-3,0,3})for(int x=-4;x<=4;x++)SecretWorld.place(p,c.offset(x,5,z),Blocks.OAK_PLANKS);for(int z=-4;z<=3;z++)SecretWorld.place(p,c.offset(0,0,z),Blocks.RAIL);}
            case 8->{for(int x:new int[]{-4,4})for(int z=-3;z<=3;z++)for(int y=2;y<5;y++)SecretWorld.place(p,c.offset(x,y,z),(z+y)%2==0?Blocks.PURPLE_STAINED_GLASS:Blocks.MAGENTA_STAINED_GLASS);}
            case 9->{for(int x:new int[]{-4,4})for(int z:new int[]{-2,2}){SecretWorld.place(p,c.offset(x,1,z),ModBlocks.SOULGLASS_BLOCK.get());SecretWorld.place(p,c.offset(x,2,z),Blocks.AMETHYST_CLUSTER);}SecretWorld.place(p,c.offset(0,1,3),ModBlocks.SOULGLASS_BLOCK.get());}
            case 10->{for(int x:new int[]{-4,4})for(int z=-3;z<=3;z++)for(int y=2;y<5;y++)SecretWorld.place(p,c.offset(x,y,z),Blocks.IRON_BARS);SecretWorld.place(p,c.offset(0,1,3),ModBlocks.SOULGLASS_BLOCK.get());}
            case 11->{for(int x:new int[]{-4,4})for(int z:new int[]{-3,3})for(int y=0;y<5;y++)SecretWorld.place(p,c.offset(x,y,z),Blocks.OBSIDIAN);SecretWorld.place(p,c.offset(0,3,3),Blocks.CRYING_OBSIDIAN);SecretWorld.place(p,c.offset(0,4,3),Blocks.AMETHYST_CLUSTER);}
        }
    }
    public static void ruin(ServerPlayer p,BlockPos c,int index,int roof,int tier){
        var random=new java.util.Random(c.asLong()^index*9137L);
        // Retain the sealed ground-floor envelope and all clue/lock positions.
        // Broken upper windows, collapsed roofing and ruined exterior masonry are safe to vary.
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++){
            if((Math.abs(x)==5||Math.abs(z)==5)&&!(z==-5&&Math.abs(x)<=2)){
                SecretWorld.place(p,c.offset(x,0,z),random.nextBoolean()?Blocks.MOSSY_STONE_BRICKS:Blocks.CRACKED_STONE_BRICKS);
                if(random.nextInt(3)==0)SecretWorld.place(p,c.offset(x,1,z),Blocks.CRACKED_STONE_BRICKS);
                if(random.nextInt(4)==0)SecretWorld.place(p,c.offset(x,4,z),Blocks.COBWEB);
            }
            if(Math.abs(x)<5&&Math.abs(z)<5&&random.nextInt(4)==0){
                for(int y=roof;y<=roof+4;y++)SecretWorld.place(p,c.offset(x,y,z),Blocks.AIR);
                if(random.nextBoolean())SecretWorld.place(p,c.offset(x,roof-1,z),Blocks.COBWEB);
            }
        }
        for(int side:new int[]{-1,1}){
            for(int z=-3;z<=4;z+=3){
                SecretWorld.place(p,c.offset(side*6,0,z),Blocks.MOSSY_COBBLESTONE);
                SecretWorld.place(p,c.offset(side*6,1,z),random.nextBoolean()?Blocks.POTTED_DEAD_BUSH:Blocks.COBWEB);
            }
            SecretWorld.place(p,c.offset(side*2,1,-6),Blocks.SOUL_LANTERN);
            for(int y=0;y<2+random.nextInt(3);y++)SecretWorld.place(p,c.offset(side*6,y,5),Blocks.CRACKED_STONE_BRICKS);
            SecretWorld.place(p,c.offset(side*4,3,4),Blocks.COBWEB);
        }
        if(tier>=2){SecretWorld.place(p,c.offset(0,4,4),Blocks.CRYING_OBSIDIAN);SecretWorld.place(p,c.offset(-3,roof,3),Blocks.CRACKED_STONE_BRICKS);SecretWorld.place(p,c.offset(-3,roof+1,3),Blocks.SKELETON_SKULL);}
    }
    private SecretScenery(){}
}
