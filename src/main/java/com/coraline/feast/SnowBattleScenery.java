package com.coraline.feast;
import static com.coraline.feast.ExpeditionActivities.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;

final class SnowBattleScenery {
    static void build(ServerLevel l,VisitData.Scene s,BlockPos c){var a=c.north(38);
        box(l,c,-38,-2,-76,38,-1,12,Blocks.SNOW_BLOCK);box(l,c,-38,0,-76,38,4,-76,Blocks.BLUE_ICE);box(l,c,-38,0,-76,-38,4,12,Blocks.BLUE_ICE);box(l,c,38,0,-76,38,4,12,Blocks.BLUE_ICE);box(l,c,-38,0,12,38,4,12,Blocks.SPRUCE_PLANKS);
        for(int x=-36;x<=36;x++)for(int z=-73;z<=8;z++)if(Math.floorMod(x*17+z*11,37)==0)put(l,c,x,0,z,Blocks.SNOW);
        // Frozen pond and a two-ended bridge, not a slippery arena-wide floor.
        box(l,a,-9,-1,-5,9,-1,5,Blocks.PACKED_ICE);box(l,a,-2,0,-8,2,0,8,Blocks.SPRUCE_SLAB);for(int z=-7;z<=7;z+=2){put(l,a,-3,0,z,Blocks.SPRUCE_FENCE);put(l,a,3,0,z,Blocks.SPRUCE_FENCE);}
        for(int side:new int[]{-1,1})for(int row:new int[]{-1,1}){int x=side*16,z=row*16;
            box(l,a,x-5,0,z-3,x+5,0,z-3,Blocks.SNOW_BLOCK);box(l,a,x-5,1,z-3,x-3,1,z-3,Blocks.SNOW_BLOCK);box(l,a,x+3,1,z-3,x+5,1,z-3,Blocks.SNOW_BLOCK);
            box(l,a,x-5,0,z-3,x-5,1,z+2,Blocks.SNOW_BLOCK);box(l,a,x+5,0,z-3,x+5,1,z+1,Blocks.SNOW_BLOCK);
            put(l,a,x-4,2,z-3,Blocks.LANTERN);put(l,a,x+4,2,z-3,Blocks.LANTERN);
        }
        // Raised overlooks have open sightlines and ramps at BOTH ends.
        for(int side:new int[]{-1,1}){int x=side*28;box(l,a,x-3,0,-5,x+3,1,5,Blocks.SPRUCE_PLANKS);for(int end:new int[]{-1,1})for(int i=0;i<2;i++)for(int dx=-2;dx<=2;dx++)ActivityBlocks.set(l,a.offset(x+dx,i,end*(7-i)),Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING,end>0?Direction.NORTH:Direction.SOUTH),2);put(l,a,x,2,0,Blocks.SPRUCE_FENCE);put(l,a,x,3,0,Blocks.LANTERN);}
        for(int x:new int[]{-33,33})for(int z:new int[]{-30,-12,12,30})tree(l,a,x,z);
        for(int x:new int[]{-8,8})for(int z:new int[]{-27,27}){box(l,a,x-2,0,z-1,x+2,0,z+1,Blocks.SNOW_BLOCK);put(l,a,x,1,z,Blocks.PACKED_ICE);put(l,a,x,2,z,Blocks.LANTERN);}
        // Low drifts break long firing lanes while keeping escape routes on either side.
        for(int x:new int[]{-10,10})for(int z:new int[]{-7,7}){box(l,a,x-2,0,z,x+2,0,z,Blocks.SNOW_BLOCK);put(l,a,x,1,z,Blocks.SNOW);}
        for(int x:new int[]{-35,35})for(int z:new int[]{-24,0,24}){box(l,a,x,0,z,x,4,z,Blocks.SPRUCE_FENCE);put(l,a,x,5,z,Blocks.SEA_LANTERN);for(int dz=1;dz<=3;dz++)put(l,a,x,4,z+dz,x<0?Blocks.RED_WOOL:Blocks.LIGHT_BLUE_WOOL);}
        for(int x=-30;x<=30;x+=10){put(l,a,x,1,-35,Blocks.SNOW_BLOCK);put(l,a,x,2,-35,Blocks.PACKED_ICE);put(l,a,x,3,-35,Blocks.LANTERN);}
        // Cabin-front market and referee/spectator gallery stay outside the fighting footprint.
        for(int x:new int[]{-26,26}){box(l,c,x-6,0,-4,x+6,0,7,Blocks.SPRUCE_PLANKS);box(l,c,x-6,1,7,x+6,5,7,Blocks.SPRUCE_LOG);box(l,c,x-6,1,-4,x-6,4,7,Blocks.SPRUCE_LOG);box(l,c,x+6,1,-4,x+6,4,7,Blocks.SPRUCE_LOG);box(l,c,x-7,5,-5,x+7,5,8,Blocks.SNOW_BLOCK);box(l,c,x-4,1,-3,x+4,1,-3,Blocks.SPRUCE_PLANKS);put(l,c,x-3,2,-3,Blocks.CAKE);put(l,c,x+3,2,-3,Blocks.LANTERN);sign(l,c.offset(x,3,-3),Direction.NORTH,x<0?"HOT COCOA":"SNOW CLUB","Winter festival");}
        box(l,c,-12,0,2,-8,0,5,Blocks.SMOOTH_QUARTZ);box(l,c,-11,1,3,-9,1,4,Blocks.GOLD_BLOCK);put(l,c,-10,2,3,Blocks.PACKED_ICE);put(l,c,-10,3,3,Blocks.GOLD_BLOCK);
        // Prize counter is at walking height, in front of the decorative trophy.
        box(l,c,-11,0,0,-9,0,0,Blocks.GOLD_BLOCK);
        sign(l,c.offset(-10,0,1),Direction.SOUTH,"WINNER'S PRIZE","Right-click reward","No climbing needed");
        for(int x:new int[]{-15,15}){box(l,c,x,0,-8,x,5,-8,Blocks.SPRUCE_LOG);put(l,c,x,6,-8,Blocks.SEA_LANTERN);}box(l,c,-15,6,-8,15,6,-8,Blocks.SPRUCE_SLAB);for(int x=-12;x<=12;x+=4)ActivityBlocks.set(l,c.offset(x,5,-8),Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true),2);
        for(int x=-16;x<=16;x+=4){var npc=MallScenery.employee(l,s,c.offset(x,0,9),x+36,-1);npc.getPersistentData().remove("MallNpc");npc.setNoAi(true);npc.setYRot(180);npc.setYBodyRot(180);npc.setYHeadRot(180);npc.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.SNOWBALL));var chair=LoopProps.spawn(l,s,ScenePropEntity.CHAIR,Vec3.atBottomCenterOf(c.offset(x,0,9)),850+x);chair.setYRot(180);npc.startRiding(chair,true);}
        for(int x:new int[]{-34,34}){put(l,c,x,0,9,Blocks.SNOW_BLOCK);put(l,c,x,1,9,Blocks.SNOW_BLOCK);put(l,c,x,2,9,Blocks.CARVED_PUMPKIN);}
        // Ice sculpture fountain: deliberately frozen, with no flowing water sources.
        box(l,a,-4,0,-33,4,0,-29,Blocks.BLUE_ICE);box(l,a,0,1,-31,0,3,-31,Blocks.PACKED_ICE);put(l,a,0,4,-31,Blocks.SEA_LANTERN);
    }
    private static void tree(ServerLevel l,BlockPos c,int x,int z){box(l,c,x,0,z,x,6,z,Blocks.SPRUCE_LOG);for(int y=3;y<=7;y++){int r=y<5?3:y<7?2:1;for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++)if(Math.abs(dx)+Math.abs(dz)<=r+1){put(l,c,x+dx,y,z+dz,Blocks.SPRUCE_LEAVES);if(y==7||Math.abs(dx)==r||Math.abs(dz)==r)put(l,c,x+dx,y+1,z+dz,Blocks.SNOW);}}}
    private SnowBattleScenery(){}
}
