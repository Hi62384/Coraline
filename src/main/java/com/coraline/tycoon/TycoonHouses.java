package com.coraline.tycoon;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

public final class TycoonHouses {
    public static final String[] NAMES={"No House","Simple Cottage","Comfortable House","Elegant House","Grand Other House","Magnificent Mansion"};
    public static Map<BlockPos,BlockState> plan(TycoonData.Home h,int tier){
        var cells=new LinkedHashMap<BlockPos,BlockState>();var c=h.center();int radius=new int[]{0,3,6,9,12,16}[tier],height=tier>=4?9:5;
        Block wall=tier==1?Blocks.OAK_PLANKS:tier==2?Blocks.STRIPPED_SPRUCE_WOOD:tier==3?Blocks.CALCITE:tier==4?Blocks.PINK_TERRACOTTA:Blocks.QUARTZ_BRICKS;
        Block floor=tier<=2?Blocks.SPRUCE_PLANKS:tier==3?Blocks.DARK_OAK_PLANKS:tier==4?Blocks.POLISHED_DIORITE:Blocks.SMOOTH_QUARTZ;
        Block roof=tier<=2?Blocks.DARK_OAK_PLANKS:tier==3?Blocks.DEEPSLATE_BRICKS:tier==4?Blocks.WAXED_COPPER_BLOCK:Blocks.PURPUR_BLOCK;
        for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            var base=c.offset(x,-1,z);if(!TycoonLandscape.core(h,base))cells.put(base,floor.defaultBlockState());
            boolean edge=Math.abs(x)==radius||Math.abs(z)==radius;
            for(int y=0;y<height;y++){if(edge){boolean door=z==radius&&Math.abs(x)<=1&&y<=2;boolean window=y>=1&&y<=2&&Math.floorMod(x+z,4)!=0;cells.put(c.offset(x,y,z),(door?Blocks.AIR:window?Blocks.GLASS:wall).defaultBlockState());}else cells.put(c.offset(x,y,z),Blocks.AIR.defaultBlockState());}
            int rise=(radius-Math.abs(x))/3;cells.put(c.offset(x,height+rise,z),roof.defaultBlockState());
            if(tier>=4&&!TycoonLandscape.core(h,base)&&Math.abs(x)<radius&&Math.abs(z)<radius){boolean stairs=x==radius-2&&z>=-3&&z<=3;int y=4;cells.put(c.offset(x,y,z),(stairs?Blocks.AIR:floor).defaultBlockState());}
        }
        // Inner rooms, a clear central portal/desk circulation area and actual
        // stairs to the upper rooms, rather than five differently colored shells.
        if(tier>=2)for(int side:new int[]{-1,1})for(int z=-radius+1;z<=radius-2;z++)for(int y=0;y<4;y++)if(z!=0&&z!=1)cells.put(c.offset(side*(radius-2),y,z),wall.defaultBlockState());
        if(tier>=4)for(int step=0;step<5;step++)cells.put(c.offset(radius-2,step-1,-3+step),Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,net.minecraft.core.Direction.SOUTH));
        for(int x=-radius+1;x<radius;x+=Math.max(2,radius-1))for(int z=-radius+1;z<radius;z+=Math.max(2,radius-1)){
            if(Math.abs(x)<=2&&Math.abs(z)<=2)continue;cells.put(c.offset(x,3,z),Blocks.LANTERN.defaultBlockState());
            if(tier>=2){cells.put(c.offset(x,0,z),Blocks.BOOKSHELF.defaultBlockState());cells.put(c.offset(x,1,z),Blocks.POTTED_AZALEA.defaultBlockState());}
            if(tier>=3&&x<0){cells.put(c.offset(x,0,z+1),Blocks.SPRUCE_STAIRS.defaultBlockState());cells.put(c.offset(x+1,0,z),Blocks.CRAFTING_TABLE.defaultBlockState());}
            if(tier>=4){cells.put(c.offset(x,5,z),Blocks.RED_CARPET.defaultBlockState());cells.put(c.offset(x,7,z),Blocks.SEA_LANTERN.defaultBlockState());}
        }
        if(tier==5)for(int x:new int[]{-radius,radius})for(int z:new int[]{-radius,radius})for(int y=0;y<=height+3;y++)cells.put(c.offset(x,y,z),y%4==0?Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState():Blocks.QUARTZ_PILLAR.defaultBlockState());
        cells.put(c.offset(0,3,0),Blocks.LANTERN.defaultBlockState());
        for(var at:List.of(c.offset(-1,0,-1),c.offset(1,0,-1)))for(int y=0;y<=2;y++)cells.remove(at.above(y));
        return cells;
    }
    public static String purchase(ServerPlayer p,int tier){var h=TycoonWorld.home(p);if(tier<1||tier>5||tier<=h.state.getInt("HouseTier")||p.serverLevel().dimension()!=TycoonWorld.KEY&&!TycoonWorkers.constructing(p))return "Choose a higher Other House tier at your Work Desk.";var next=plan(h,tier);for(var at:next.keySet())if(!TycoonLandscape.owned(h,at))return "Purchase connected terrain under the full house footprint first.";var patch=new CompoundTag();patch.putInt("HouseTier",tier);patch.put("HouseGenerated",TycoonLandscape.encode(next,h.center()));int cost=TycoonPhaseTwoConfig.HOUSE[tier].get()-TycoonPhaseTwoConfig.HOUSE[h.state.getInt("HouseTier")].get();return TycoonBuildJob.begin(p,NAMES[tier],h.state.getCompound("HouseGenerated"),next,h.center().asLong(),patch,Math.max(1,cost));}
    private TycoonHouses(){}
}
