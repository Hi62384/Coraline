package com.coraline.tycoon;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Distinct, walkable, code-native buildings. No raster or audio masters added. */
public final class TycoonAttractionPlan {
    public static BlockPos at(BlockPos origin,int rotation,int x,int y,int z){
        return switch(Math.floorMod(rotation,4)){case 1->origin.offset(-z,y,x);case 2->origin.offset(-x,y,-z);case 3->origin.offset(z,y,-x);default->origin.offset(x,y,z);};
    }
    private final BlockPos origin;private final int rotation;private final Map<BlockPos,BlockState> blocks=new LinkedHashMap<>();
    private TycoonAttractionPlan(BlockPos o,int r){origin=o;rotation=r;}
    private void put(int x,int y,int z,Block b){blocks.put(at(origin,rotation,x,y,z),b.defaultBlockState());}
    private void box(int x1,int y1,int z1,int x2,int y2,int z2,Block b){for(int x=x1;x<=x2;x++)for(int y=y1;y<=y2;y++)for(int z=z1;z<=z2;z++)put(x,y,z,b);}
    private void bench(int x,int z){put(x,1,z,Blocks.SPRUCE_SLAB);put(x,1,z+1,Blocks.SPRUCE_TRAPDOOR);}
    private void cottage(int x,int z,int tier){box(x,1,z,x+3,3,z+3,Blocks.STRIPPED_OAK_WOOD);box(x+1,1,z,x+2,2,z+2,Blocks.AIR);put(x,2,z+1,Blocks.GLASS);box(x-1,4,z-1,x+4,4,z+4,tier>2?Blocks.DARK_PRISMARINE:Blocks.SPRUCE_SLAB);}
    public static Map<BlockPos,BlockState> create(TycoonAttractionType type,BlockPos origin,int rotation,int tier,net.minecraft.nbt.CompoundTag branches){
        var p=new TycoonAttractionPlan(origin,rotation);
        // A clear, common outer envelope makes upgrades reversible and protects
        // every interior air cell against later terrain generation.
        p.box(-6,0,-6,6,0,6,Blocks.SMOOTH_STONE);
        p.box(-6,1,-6,6,6,6,Blocks.AIR);
        p.box(-1,0,-6,1,0,6,Blocks.POLISHED_ANDESITE);
        switch(type){
            case VILLAGE->{p.cottage(-5,-4,tier);p.cottage(2,-4,tier);p.cottage(-5,2,tier);p.box(2,1,3,5,1,4,Blocks.OAK_SLAB);p.box(2,3,3,5,3,4,Blocks.RED_WOOL);p.put(3,2,4,Blocks.CAKE);}
            case WONDERLAND->{for(int x:new int[]{-4,4})for(int z:new int[]{-3,3}){p.box(x,1,z,x,3,z,Blocks.QUARTZ_PILLAR);p.box(x-1,4,z-1,x+1,4,z+1,(x+z)%2==0?Blocks.MAGENTA_WOOL:Blocks.CYAN_WOOL);}p.box(-1,1,1,1,1,3,Blocks.PINK_STAINED_GLASS);p.put(0,2,2,Blocks.AMETHYST_CLUSTER);}
            case CIRCUS->{for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)if(Math.abs(x)==6||Math.abs(z)==6){if(z!=-6||Math.abs(x)>1)p.box(x,1,z,x,3,z,Math.floorMod(x+z,2)==0?Blocks.RED_WOOL:Blocks.WHITE_WOOL);}for(int y=4;y<=6;y++){int r=7-y;p.box(-r,y,-r,r,y,r,y%2==0?Blocks.RED_WOOL:Blocks.WHITE_WOOL);}p.box(-2,0,-1,2,0,3,Blocks.RED_CONCRETE);for(int x:new int[]{-4,4})for(int z=-3;z<=3;z+=2)p.bench(x,z);}
            case GARDEN->{for(int x:new int[]{-5,5})for(int z=-4;z<=4;z+=2){p.put(x,1,z,Blocks.FLOWERING_AZALEA);p.put(x-(int)Math.signum(x),1,z,Blocks.ALLIUM);}p.box(-1,1,1,1,1,3,Blocks.BLUE_STAINED_GLASS);p.put(0,2,2,Blocks.SEA_LANTERN);p.bench(-3,3);p.bench(3,-3);}
            case RESTAURANT->{p.box(-6,1,5,6,3,6,Blocks.PINK_TERRACOTTA);p.box(-6,1,-5,-6,3,4,Blocks.WHITE_TERRACOTTA);p.box(6,1,-5,6,3,4,Blocks.WHITE_TERRACOTTA);p.box(-5,1,4,5,1,4,Blocks.QUARTZ_BLOCK);for(int x:new int[]{-3,3})for(int z:new int[]{-3,1}){p.put(x,1,z,Blocks.OAK_SLAB);p.put(x,2,z,Blocks.CAKE);p.bench(x-1,z);}p.put(4,2,4,Blocks.SMOKER);}
            case GALLERY->{p.box(-6,1,6,6,4,6,Blocks.WHITE_CONCRETE);p.box(-6,1,-5,-6,4,5,Blocks.WHITE_CONCRETE);p.box(6,1,-5,6,4,5,Blocks.WHITE_CONCRETE);for(int x=-4;x<=4;x+=2){p.put(x,2,5,x%4==0?Blocks.BLUE_GLAZED_TERRACOTTA:Blocks.YELLOW_GLAZED_TERRACOTTA);p.put(x,3,5,x%4==0?Blocks.RED_GLAZED_TERRACOTTA:Blocks.LIME_GLAZED_TERRACOTTA);}for(int x:new int[]{-3,3}){p.put(x,1,1,Blocks.QUARTZ_BLOCK);p.put(x,2,1,Blocks.AMETHYST_BLOCK);}}
            case MAZE->{for(int x=-4;x<=4;x+=2)for(int z=-4;z<=4;z++)if(z!=(x%4==0?4:-4))p.box(x,1,z,x,2,z,Blocks.OAK_LEAVES);p.box(-6,1,-5,-6,2,5,Blocks.OAK_LEAVES);p.put(5,1,4,Blocks.GOLD_BLOCK);}
            case PLAYGROUND->{p.box(-4,1,1,-4,4,1,Blocks.OAK_FENCE);p.box(4,1,1,4,4,1,Blocks.OAK_FENCE);p.box(-4,4,1,4,4,1,Blocks.OAK_LOG);p.put(-2,1,1,Blocks.OAK_SLAB);p.put(2,1,1,Blocks.OAK_SLAB);for(int i=0;i<4;i++)p.put(-4+i,1+i,-3,Blocks.LIME_CONCRETE);p.box(3,1,3,4,2,4,Blocks.HAY_BLOCK);}
            case THEATRE->{p.box(-5,1,3,5,1,5,Blocks.DARK_OAK_PLANKS);p.box(-6,1,6,6,5,6,Blocks.BLACK_WOOL);p.box(-5,2,4,-4,4,4,Blocks.RED_WOOL);p.box(4,2,4,5,4,4,Blocks.RED_WOOL);for(int x=-4;x<=4;x+=2)for(int z=-3;z<=1;z+=2)p.bench(x,z);}
            case RETREAT->{p.box(-6,1,6,6,3,6,Blocks.SPRUCE_PLANKS);p.box(-6,1,-5,-6,3,5,Blocks.SPRUCE_PLANKS);for(int z=-3;z<=3;z+=3){p.bench(-4,z);p.bench(3,z);}p.box(-4,1,5,4,2,5,Blocks.BOOKSHELF);p.box(-1,1,0,1,1,2,Blocks.LIGHT_BLUE_CARPET);}
        }
        // Distinct tier trim and branch exhibits are physical, not menu-only.
        for(int x:new int[]{-6,6})for(int z:new int[]{-6,6}){p.put(x,1,z,tier>=3?Blocks.CHISELED_QUARTZ_BLOCK:Blocks.STONE_BRICKS);p.put(x,2,z,tier>=4?Blocks.SEA_LANTERN:Blocks.LANTERN);}
        for(int i=0;i<3;i++)if(branches.getInt(Integer.toString(i))>0){p.put(-4+i*4,1,5,Blocks.QUARTZ_BLOCK);p.put(-4+i*4,2,5,i==0?Blocks.AMETHYST_BLOCK:i==1?Blocks.BLUE_GLAZED_TERRACOTTA:Blocks.FLOWERING_AZALEA);}
        p.put(5,1,-5,TycoonContent.BLOCKS.get("storage_cabinet").get());
        return p.blocks;
    }
    private TycoonAttractionPlan(){origin=BlockPos.ZERO;rotation=0;}
}
