package com.coraline.feast;

import com.coraline.otherworld.RealmData;
import com.coraline.parents.ParentAccess;
import com.coraline.parents.ParentEntity;
import com.coraline.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** An aerial heart-garden reveal followed by owner-only harvesting. */
@Mod.EventBusSubscriber(modid="coraline")
public final class MagicGardenActivity {
    private static final String OWNER="CoralineFlyingGardenOwner",GEN="CoralineFlyingGardenGeneration";
    private static final int PLANTS=60,ASCENT=160,DESCENT=150,HARVEST=1600;
    private static final Set<UUID> MUSIC_SESSIONS=new HashSet<>();
    private static final Set<UUID> HARVEST_MUSIC_SESSIONS=new HashSet<>();
    private MagicGardenActivity(){}
    static BlockPos center(ServerPlayer p,VisitData.Scene s){return VillageActivity.center(p,s).east(1360);}
    static boolean exitReady(VisitData.Scene s){return s.loop.flyingGardenStage>=6;}
    private static void put(ServerLevel l,BlockPos at,Block b){ActivityBlocks.set(l,at,ActivityWorld.persistentLeaves(b),2|32);}
    private static void mark(Entity e,VisitData.Scene s){e.getPersistentData().putUUID(OWNER,s.owner);e.getPersistentData().putInt(GEN,s.loop.generation);e.getPersistentData().putBoolean("CoralineDecoration",true);}
    private static boolean owned(Entity e,VisitData.Scene s){return e.getPersistentData().hasUUID(OWNER)&&s.owner.equals(e.getPersistentData().getUUID(OWNER))&&e.getPersistentData().getInt(GEN)==s.loop.generation;}
    private static boolean heart(int x,int z){
        double u=x/34.0,v=-z/34.0,a=u*u+v*v-1;
        return a*a*a-u*u*v*v*v<0;
    }
    static void build(ServerLevel l,BlockPos c){
        if(l.getBlockState(c.below(4)).is(Blocks.CYAN_GLAZED_TERRACOTTA))return;
        if(l.getBlockState(c.below(4)).is(Blocks.BLUE_GLAZED_TERRACOTTA))removeBuild(l,c);
        if(l.getBlockState(c.below(4)).is(Blocks.PINK_GLAZED_TERRACOTTA)||l.getBlockState(c.below(4)).is(Blocks.MAGENTA_GLAZED_TERRACOTTA))removeBuild(l,c);
        for(int x=-49;x<=49;x++)for(int z=-49;z<=49;z++){
            var ground=c.offset(x,-1,z);put(l,ground.below(),Blocks.DIRT);
            boolean border=Math.abs(x)==49||Math.abs(z)==49;
            boolean landing=Math.abs(x)<=5&&z>=38;
            boolean shape=heart(x,z+4),edge=shape&&!heart((int)(x*1.09),(int)((z+4)*1.09));
            boolean entry=Math.abs(x)<3&&z>26;
            Block surface=border?Blocks.POLISHED_BLACKSTONE_BRICKS:landing?Blocks.CHERRY_PLANKS:
                edge?Blocks.MOSS_BLOCK:Blocks.GRASS_BLOCK;
            put(l,ground,surface);
            if(border){put(l,ground.above(),Blocks.CHERRY_FENCE);if(Math.floorMod(x+z,8)==0)put(l,ground.above(2),Blocks.SEA_LANTERN);}
            // A flowering hedge traces the outline. Its color is real foliage,
            // not a painted floor: the form emerges only in the aerial view.
            if(edge&&!landing&&!entry)put(l,c.offset(x,0,z),Blocks.FLOWERING_AZALEA_LEAVES);
        }
        // A wandering path connects the heart's entrance, central fountain and
        // northern rose arbor. Flower beds make broad organic color washes.
        for(int z=-34;z<=38;z++){
            int bend=(int)Math.round(3*Math.sin(z*.115));
            for(int dx=-2;dx<=2;dx++){
                int x=bend+dx;if(!heart(x,z+4)&&z<30)continue;
                put(l,c.offset(x,-1,z),Math.floorMod(z+dx,11)==0?Blocks.SEA_LANTERN:Math.abs(dx)==2?Blocks.MOSS_BLOCK:Blocks.CALCITE);
                put(l,c.offset(x,0,z),Blocks.AIR);
            }
        }
        for(int side:new int[]{-1,1}){
            flowerBed(l,c,side*17,-10,7,side<0?0:1);
            flowerBed(l,c,side*21,12,6,side<0?2:3);
            pond(l,c,side*24,-24);
            arch(l,c,side*11,25);
        }
        flowerBed(l,c,0,21,7,4);
        fountain(l,c,0,-5,5);
        for(int side:new int[]{-1,1}){
            // Satellite fountains act as eye-level landmarks without drawing a
            // flat-colored symbol across the aerial heart composition.
            if(side<0)moonFountain(l,c,side*32,12);
            else fountain(l,c,side*32,12,3);
            for(int z:new int[]{7,17}){
                put(l,c.offset(side*37,0,z),Blocks.CHERRY_STAIRS);
                put(l,c.offset(side*37,0,z+1),Blocks.POTTED_PINK_TULIP);
            }
            flowerSculpture(l,c,side*34,-4);
        }
        gazebo(l,c,0,-30);
        arch(l,c,0,29);
        for(int side:new int[]{-1,1})for(int step=6;step<=20;step++){
            int x=side*step,z=-8-(int)Math.round(13*smooth(step/20.0));
            for(int dz=-1;dz<=1;dz++){
                var at=c.offset(x,0,z+dz);
                if(l.getBlockState(at).is(Blocks.WATER))continue;
                put(l,at.below(),Math.floorMod(step+dz,7)==0?Blocks.SEA_LANTERN:Blocks.CALCITE);
                put(l,at,Blocks.AIR);
            }
        }
        // Path lanterns and living borders are deliberately clustered rather
        // than repeated at a rigid interval across a flower grid.
        for(int x=-46;x<=46;x++)for(int z=-46;z<=46;z++){
            if(heart(x,z+4)||Math.abs(x)>43&&Math.abs(z)>43)continue;
            if(Math.floorMod(x*x+z*z,173)==0)put(l,c.offset(x,-1,z),Blocks.SEA_LANTERN);
        }
        // A continuous garden promenade surrounds the heart, with paired flowerbeds
        // and blossom-covered arches framing the four long approaches.
        for(int x=-41;x<=41;x++)for(int z=-41;z<=41;z++){
            if((Math.abs(x)>=39&&Math.abs(x)<=40)||(Math.abs(z)>=39&&Math.abs(z)<=40)){
                if(heart(x,z+4))continue;
                put(l,c.offset(x,-1,z),Math.floorMod(x+z,13)==0?Blocks.SEA_LANTERN:Blocks.MOSS_BLOCK);
                if(!l.isEmptyBlock(c.offset(x,0,z)))put(l,c.offset(x,0,z),Blocks.AIR);
            }
        }
        for(int x:new int[]{-35,35})for(int z=-36;z<=36;z+=6){
            if(heart(x,z+4))continue;
            for(int dz=-1;dz<=1;dz++){
                put(l,c.offset(x,0,z+dz),Blocks.FLOWERING_AZALEA_LEAVES);
                put(l,c.offset(x+(x<0?1:-1),0,z+dz),dz==0?Blocks.PINK_TULIP:Blocks.ALLIUM);
            }
        }
        for(int x:new int[]{-27,27})for(int z:new int[]{-40,40}){
            if(heart(x,z+4))continue;
            for(int side:new int[]{-1,1})for(int y=0;y<4;y++)put(l,c.offset(x+side*2,y,z),Blocks.CHERRY_FENCE);
            for(int dx=-2;dx<=2;dx++)put(l,c.offset(x+dx,4,z),Blocks.FLOWERING_AZALEA_LEAVES);
            put(l,c.offset(x,5,z),Blocks.SEA_LANTERN);
        }
        for(int[] treeSpot:new int[][]{{-43,-35},{-40,-29},{42,-37},{44,-27},{-44,20},{-39,32},{44,23},{39,33},{-34,-17},{34,-18},{-13,37},{13,37}})
            tree(l,c.offset(treeSpot[0],0,treeSpot[1]));
        for(int z:new int[]{-44,43})for(int x:new int[]{-27,27}){
            for(int y=0;y<4;y++)put(l,c.offset(x,y,z),Blocks.CHERRY_FENCE);
            put(l,c.offset(x,4,z),Blocks.SEA_LANTERN);
            put(l,c.offset(x,2,z+(z<0?1:-1)),Blocks.LANTERN);
        }
        for(int x:new int[]{-39,39})for(int z:new int[]{-30,-10,10,28}){
            put(l,c.offset(x,0,z),Blocks.CHERRY_STAIRS);put(l,c.offset(x+2*(x<0?1:-1),0,z),Blocks.POTTED_BLUE_ORCHID);
        }
        for(int x:new int[]{-40,40})for(int z:new int[]{-43,40}){
            // Corner rest stops replace the old isolated water columns.
            for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)if(dx*dx+dz*dz<=9)
                put(l,c.offset(x+dx,-1,z+dz),dx*dx+dz*dz>=7?Blocks.CALCITE:Blocks.CHERRY_PLANKS);
            put(l,c.offset(x,0,z),Blocks.SEA_LANTERN);
            for(int dx:new int[]{-2,2}){
                put(l,c.offset(x+dx,0,z),Blocks.CHERRY_STAIRS);
                put(l,c.offset(x+dx,0,z+2),Blocks.POTTED_ALLIUM);
            }
        }
        for(int x=-5;x<=5;x++)for(int z=37;z<=48;z++){
            put(l,c.offset(x,-1,z),Math.abs(x)==5||z==37?Blocks.GOLD_BLOCK:Blocks.CHERRY_PLANKS);
            if(Math.abs(x)==5&&Math.floorMod(z,3)==0){put(l,c.offset(x,0,z),Blocks.CHERRY_FENCE);put(l,c.offset(x,1,z),Blocks.LANTERN);}
        }
        for(int x:new int[]{-2,2})for(int z:new int[]{40,46})put(l,c.offset(x,0,z),Blocks.POTTED_PINK_TULIP);
        for(int side:new int[]{-1,1}){
            for(int z=38;z<=47;z++){
                put(l,c.offset(side*7,-1,z),Blocks.MOSS_BLOCK);
                if(z%2==0)put(l,c.offset(side*7,0,z),Blocks.FLOWERING_AZALEA_LEAVES);
                else put(l,c.offset(side*8,0,z),Blocks.ALLIUM);
            }
            for(int z:new int[]{40,46}){
                put(l,c.offset(side*4,0,z),Blocks.CUT_COPPER);
                put(l,c.offset(side*4,1,z),Blocks.GOLD_BLOCK);
                put(l,c.offset(side*4,2,z),Blocks.CHAIN);
            }
            put(l,c.offset(side*8,0,42),Blocks.BARREL);
            put(l,c.offset(side*8,1,42),Blocks.POTTED_BLUE_ORCHID);
        }
        // The old aerial composition was surrounded by large empty lawns. New
        // winding circuits link themed garden rooms; all infill uses flowers,
        // foliage and real garden features rather than colored floor pixels.
        landscape(l,c);
        // Seal after paths and decorations, which may otherwise overwrite a basin rim.
        sealBasin(l,c,0,-5,5);sealBasin(l,c,-32,12,4);sealBasin(l,c,32,12,3);
        put(l,c.below(4),Blocks.CYAN_GLAZED_TERRACOTTA);
    }
    private static boolean soft(ServerLevel l,BlockPos at){
        Block floor=l.getBlockState(at.below()).getBlock();Block top=l.getBlockState(at).getBlock();
        return (floor==Blocks.GRASS_BLOCK||floor==Blocks.MOSS_BLOCK)&&
            (top==Blocks.AIR||top==Blocks.GRASS||top==Blocks.TALL_GRASS||top==Blocks.PINK_TULIP||
                top==Blocks.WHITE_TULIP||top==Blocks.ALLIUM||top==Blocks.BLUE_ORCHID||top==Blocks.AZURE_BLUET);
    }
    private static void trail(ServerLevel l,BlockPos c,int x0,int z0,int x1,int z1,double bend){
        int steps=(int)Math.ceil(Math.hypot(x1-x0,z1-z0)*2);
        for(int i=0;i<=steps;i++){
            double t=i/(double)steps,sway=Math.sin(Math.PI*t)*bend;
            int x=(int)Math.round(x0+(x1-x0)*t+sway*(z1-z0)/Math.max(1,Math.hypot(x1-x0,z1-z0)));
            int z=(int)Math.round(z0+(z1-z0)*t-sway*(x1-x0)/Math.max(1,Math.hypot(x1-x0,z1-z0)));
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
                BlockPos at=c.offset(x+dx,0,z+dz);
                boolean hedgeGate=l.getBlockState(at).is(Blocks.FLOWERING_AZALEA_LEAVES)&&l.getBlockState(at.below()).is(Blocks.MOSS_BLOCK);
                if(!soft(l,at)&&!hedgeGate)continue;
                put(l,at.below(),Math.floorMod(i+dx*3+dz*5,17)==0?Blocks.SEA_LANTERN:
                    Math.abs(dx)+Math.abs(dz)>1?Blocks.MOSS_BLOCK:Blocks.CALCITE);
                put(l,at,Blocks.AIR);
            }
        }
    }
    private static void room(ServerLevel l,BlockPos c,int x,int z,int palette){
        // Each room has a low flowering hedge, two breaks for entry, and
        // alternating inner beds. No wall fully seals the visitor in.
        for(int dx=-8;dx<=8;dx++)for(int dz=-8;dz<=8;dz++){
            double d=Math.hypot(dx*.95,dz*1.05);BlockPos at=c.offset(x+dx,0,z+dz);
            if(d>=6.6&&d<=8.3&&Math.abs(dx)>2&&soft(l,at)){
                put(l,at,Math.floorMod(dx+dz+palette,5)==0?Blocks.AZALEA_LEAVES:Blocks.FLOWERING_AZALEA_LEAVES);
                if(Math.floorMod(dx*7+dz*11,13)==0)put(l,at.above(),Blocks.PINK_PETALS);
            }else if(d<6.2&&soft(l,at)){
                if(Math.floorMod(dx*19+dz*23+palette*11,7)<=4){
                    put(l,at.below(),Blocks.MOSS_BLOCK);
                    Block bloom=switch(Math.floorMod(palette+dx/3+dz/3,7)){
                        case 0->Blocks.PINK_TULIP;case 1->Blocks.ALLIUM;case 2->Blocks.BLUE_ORCHID;
                        case 3->Blocks.WHITE_TULIP;case 4->Blocks.ORANGE_TULIP;case 5->Blocks.AZURE_BLUET;
                        default->Blocks.RED_TULIP;};
                    put(l,at,bloom);
                }
            }
        }
        for(int side:new int[]{-1,1}){
            BlockPos at=c.offset(x+side*5,0,z+side*4);
            if(soft(l,at)){put(l,at.below(),Blocks.CHERRY_PLANKS);put(l,at,Blocks.CHERRY_STAIRS);}
            BlockPos pot=c.offset(x+side*4,0,z-side*4);
            if(soft(l,pot)){put(l,pot,Blocks.DECORATED_POT);put(l,pot.above(),Blocks.PINK_PETALS);}
        }
        arch(l,c,x,z-8);
    }
    private static void pergola(ServerLevel l,BlockPos c,int x,int z){
        for(int dx:new int[]{-3,3})for(int dz:new int[]{-3,3}){
            BlockPos foot=c.offset(x+dx,0,z+dz);if(!soft(l,foot))continue;
            for(int y=0;y<4;y++)put(l,foot.above(y),Blocks.STRIPPED_CHERRY_LOG);
            put(l,foot.above(4),Blocks.FLOWERING_AZALEA_LEAVES);
            put(l,foot.offset(dx<0?-1:1,2,0),Blocks.FLOWERING_AZALEA_LEAVES);
        }
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)if(Math.abs(dx)==3||Math.abs(dz)==3){
            BlockPos roof=c.offset(x+dx,4,z+dz);if(l.isEmptyBlock(roof))put(l,roof,Blocks.FLOWERING_AZALEA_LEAVES);
        }
        for(int dx:new int[]{-2,2}){
            BlockPos seat=c.offset(x+dx,0,z);if(soft(l,seat))put(l,seat,Blocks.CHERRY_STAIRS);
        }
        BlockPos lamp=c.offset(x,4,z);if(l.isEmptyBlock(lamp))put(l,lamp,Blocks.SEA_LANTERN);
    }
    private static void topiary(ServerLevel l,BlockPos c,int x,int z,int shape){
        BlockPos at=c.offset(x,0,z);if(!soft(l,at))return;
        put(l,at,Blocks.CHERRY_LOG);
        if(shape==0){
            for(int y=1;y<=3;y++)for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)
                if(Math.abs(dx)+Math.abs(dz)<=1)put(l,at.offset(dx,y,dz),Blocks.AZALEA_LEAVES);
        }else{
            for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)if(dx*dx+dz*dz<=4)put(l,at.offset(dx,2,dz),Blocks.FLOWERING_AZALEA_LEAVES);
            for(int dx:new int[]{-1,1})put(l,at.offset(dx,3,0),Blocks.AZALEA_LEAVES);
        }
        for(int dx:new int[]{-2,2})if(soft(l,at.offset(dx,0,0)))put(l,at.offset(dx,0,0),Blocks.POTTED_ALLIUM);
    }
    private static void rill(ServerLevel l,BlockPos c,int side){
        for(int z=-18;z<=8;z++){
            int x=side*(24+(int)Math.round(4*Math.sin((z+18)*.10)));
            BlockPos at=c.offset(x,0,z);if(!soft(l,at))continue;
            put(l,at.below(),Blocks.WATER);put(l,at,Blocks.AIR);
            for(int bank:new int[]{-1,1}){
                BlockPos rim=at.offset(bank,0,0);
                if(soft(l,rim)){put(l,rim.below(),Blocks.CALCITE);
                    if(Math.floorMod(z+bank*5,4)==0)put(l,rim,Blocks.BLUE_ORCHID);}
            }
            if(z==-3||z==3){
                for(int dx=-2;dx<=2;dx++)put(l,at.offset(dx,0,0),Blocks.CHERRY_SLAB);
                for(int dx:new int[]{-2,2})put(l,at.offset(dx,1,0),Blocks.CHERRY_FENCE);
            }
        }
    }
    private static void raisedBed(ServerLevel l,BlockPos c,int x,int z,int palette){
        for(int dx=-5;dx<=5;dx++)for(int dz=-5;dz<=5;dz++){
            int r2=dx*dx+dz*dz;if(r2>25)continue;
            BlockPos at=c.offset(x+dx,0,z+dz);if(!soft(l,at))continue;
            if(r2>=16){put(l,at.below(),Blocks.CALCITE);put(l,at,Blocks.QUARTZ_SLAB);}
            else{put(l,at,Blocks.MOSS_BLOCK);
                put(l,at.above(),Math.floorMod(dx*7+dz*11+palette,4)==0?Blocks.ALLIUM:
                    palette%2==0?Blocks.PINK_TULIP:Blocks.BLUE_ORCHID);}
        }
        BlockPos center=c.offset(x,0,z);
        put(l,center.above(),Blocks.SEA_LANTERN);
        put(l,center.above(2),Blocks.FLOWERING_AZALEA_LEAVES);
    }
    private static void landscape(ServerLevel l,BlockPos c){
        rill(l,c,-1);rill(l,c,1);
        // Branching routes lead through distinct rooms and the existing heart.
        int[][] routes={{0,32,-27,24},{0,32,27,24},{-27,24,-30,2},{27,24,30,2},
            {-30,2,-25,-17},{30,2,25,-17},{-25,-17,-8,-31},{25,-17,8,-31},
            {-25,-17,-39,-28},{25,-17,39,-28},{-27,24,-39,34},{27,24,39,34},
            {-30,2,-16,7},{30,2,16,7},{-16,7,0,-5},{16,7,0,-5}};
        for(int i=0;i<routes.length;i++){int[] r=routes[i];trail(l,c,r[0],r[1],r[2],r[3],i%2==0?2.2:-2.2);}
        room(l,c,-29,10,0);room(l,c,29,10,3);room(l,c,-28,27,2);room(l,c,28,27,5);
        room(l,c,-29,-11,4);room(l,c,29,-11,6);room(l,c,-12,10,1);room(l,c,12,10,3);
        pergola(l,c,-28,27);pergola(l,c,28,27);pergola(l,c,-14,-28);pergola(l,c,14,-28);
        raisedBed(l,c,-38,-27,0);raisedBed(l,c,38,-27,1);
        raisedBed(l,c,-38,27,2);raisedBed(l,c,38,27,3);
        for(int[] at:new int[][]{{-34,26},{-21,28},{34,26},{21,28},{-35,-12},{-20,-12},{35,-12},{20,-12}})
            topiary(l,c,at[0],at[1],Math.floorMod(at[0]+at[1],2));
        // Dense but organic infill. The material palette follows nearby rooms,
        // so empty grass reads as garden beds instead of random pixel noise.
        for(int x=-45;x<=45;x++)for(int z=-45;z<=45;z++){
            BlockPos at=c.offset(x,0,z);if(!soft(l,at)||Math.abs(x)<=5&&z>=32)continue;
            double wave=Math.sin(x*.29+Math.cos(z*.15)*1.6)+Math.cos(z*.25-x*.09);
            int hash=Math.floorMod(x*31+z*73+x*z*7,19);
            boolean inside=heart(x,z+4);
            if(wave>(inside?-.55:.15)&&hash<(inside?16:14)){
                put(l,at.below(),hash==3?Blocks.SEA_LANTERN:Blocks.MOSS_BLOCK);
                Block bloom=switch(Math.floorMod((x+45)/14+(z+45)/12,6)){
                    case 0->Blocks.PINK_TULIP;case 1->Blocks.ALLIUM;case 2->Blocks.BLUE_ORCHID;
                    case 3->Blocks.WHITE_TULIP;case 4->Blocks.RED_TULIP;default->Blocks.AZURE_BLUET;};
                put(l,at,bloom);
            }else if(wave<-.8&&hash<9){put(l,at,Blocks.FLOWERING_AZALEA_LEAVES);}
            else if(hash==0){put(l,at,Blocks.PINK_PETALS);}
        }
        // A few sheltered details occupy the outer corners without repeating a
        // grid of identical trees or heavy ticking display entities.
        for(int[] spot:new int[][]{{-37,5},{37,5},{-24,38},{24,38},{-15,-40},{15,-40}}){
            BlockPos at=c.offset(spot[0],0,spot[1]);if(soft(l,at))tree(l,at);
        }
    }
    private static void flowerBed(ServerLevel l,BlockPos c,int cx,int cz,int radius,int palette){
        for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            double d=Math.hypot(dx*.9,dz*1.12);if(d>radius+.3)continue;
            BlockPos at=c.offset(cx+dx,0,cz+dz);
            if(d>radius-1){put(l,at.below(),Blocks.CHERRY_PLANKS);if(Math.floorMod(dx+dz,3)==0)put(l,at,Blocks.FLOWERING_AZALEA_LEAVES);}
            else{put(l,at.below(),Blocks.MOSS_BLOCK);
                if(Math.floorMod(dx*11+dz*17+palette*5,3)!=0)put(l,at,switch(Math.floorMod(palette+dx/3+dz/3,5)){
                    case 0->Blocks.PINK_TULIP;case 1->Blocks.ALLIUM;case 2->Blocks.BLUE_ORCHID;
                    case 3->Blocks.WHITE_TULIP;default->Blocks.AZURE_BLUET;});
            }
        }
    }
    private static void pond(ServerLevel l,BlockPos c,int cx,int cz){
        for(int dx=-6;dx<=6;dx++)for(int dz=-5;dz<=5;dz++){
            double d=dx*dx/36.0+dz*dz/25.0;if(d>1.12)continue;
            BlockPos at=c.offset(cx+dx,0,cz+dz);
            if(d>.68){put(l,at.below(),Blocks.CALCITE);if(Math.floorMod(dx*7+dz*13,4)==0)put(l,at,Blocks.FLOWERING_AZALEA_LEAVES);}
            else{put(l,at.below(2),Blocks.CLAY);put(l,at.below(),Blocks.WATER);put(l,at,Blocks.AIR);
                if(Math.floorMod(dx*5+dz*7,11)==0)put(l,at,Blocks.LILY_PAD);}
        }
        for(int dz=-3;dz<=3;dz++)put(l,c.offset(cx,0,cz+dz),Blocks.CHERRY_SLAB);
        for(int dz:new int[]{-3,3})for(int y=0;y<=2;y++)put(l,c.offset(cx,y,cz+dz),y==2?Blocks.LANTERN:Blocks.CHERRY_FENCE);
    }
    private static void fountain(ServerLevel l,BlockPos c,int cx,int cz,int radius){
        for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            int r2=dx*dx+dz*dz;if(r2>radius*radius)continue;
            BlockPos at=c.offset(cx+dx,0,cz+dz);put(l,at.below(2),Blocks.POLISHED_DIORITE);
            if(r2>=(radius-1)*(radius-1)){put(l,at.below(),Blocks.QUARTZ_BLOCK);put(l,at,Blocks.QUARTZ_SLAB);}
            else{put(l,at.below(),Blocks.PRISMARINE);put(l,at,Blocks.WATER);}
        }
        for(int y=0;y<=3;y++)put(l,c.offset(cx,y,cz),y==2?Blocks.SEA_LANTERN:Blocks.CHISELED_QUARTZ_BLOCK);
        for(int side:new int[]{-1,1})for(int dx=0;dx<=2;dx++){
            put(l,c.offset(cx+side*dx,3,cz),Blocks.QUARTZ_SLAB);
            put(l,c.offset(cx,3,cz+side*dx),Blocks.QUARTZ_SLAB);
        }
        put(l,c.offset(cx,4,cz),Blocks.AMETHYST_CLUSTER);
    }
    private static void sealBasin(ServerLevel l,BlockPos c,int cx,int cz,int radius){
        for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            int d=dx*dx+dz*dz;if(d>radius*radius)continue;
            if(d>=(radius-1)*(radius-1))put(l,c.offset(cx+dx,0,cz+dz),Blocks.QUARTZ_BLOCK);
        }
    }
    private static void moonFountain(ServerLevel l,BlockPos c,int cx,int cz){
        for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++){
            int r2=dx*dx+dz*dz;if(r2>16)continue;
            BlockPos at=c.offset(cx+dx,0,cz+dz);
            put(l,at.below(2),Blocks.POLISHED_DIORITE);
            if(r2>=9){put(l,at.below(),Blocks.CALCITE);put(l,at,Blocks.QUARTZ_SLAB);}
            else{put(l,at.below(),Blocks.PRISMARINE);put(l,at,Blocks.WATER);}
        }
        for(int y=0;y<=3;y++)put(l,c.offset(cx,y,cz),y==0?Blocks.QUARTZ_PILLAR:Blocks.SEA_LANTERN);
        // A crescent suspended over the pool distinguishes this fountain from
        // the tiered central and satellite basins.
        for(int[] p:new int[][]{{-2,2},{-1,3},{0,4},{1,3},{1,2},{0,1}})
            put(l,c.offset(cx+p[0],p[1],cz),Blocks.AMETHYST_BLOCK);
        for(int x:new int[]{-4,4})put(l,c.offset(cx+x,0,cz),Blocks.POTTED_BLUE_ORCHID);
    }
    private static void arch(ServerLevel l,BlockPos c,int x,int z){
        for(int side:new int[]{-1,1}){
            for(int y=0;y<=4;y++)put(l,c.offset(x+side*2,y,z),y%2==0?Blocks.CHERRY_FENCE:Blocks.FLOWERING_AZALEA_LEAVES);
            put(l,c.offset(x+side*3,0,z),Blocks.POTTED_PINK_TULIP);
        }
        for(int dx=-2;dx<=2;dx++)put(l,c.offset(x+dx,5,z),Blocks.FLOWERING_AZALEA_LEAVES);
        put(l,c.offset(x,5,z),Blocks.SEA_LANTERN);
    }
    private static void flowerSculpture(ServerLevel l,BlockPos c,int x,int z){
        for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)if(dx*dx+dz*dz<=4)
            put(l,c.offset(x+dx,-1,z+dz),dx*dx+dz*dz>=3?Blocks.CHERRY_PLANKS:Blocks.POLISHED_DIORITE);
        put(l,c.offset(x,0,z),Blocks.CHISELED_QUARTZ_BLOCK);
        put(l,c.offset(x,1,z),Blocks.QUARTZ_PILLAR);
        put(l,c.offset(x,2,z),Blocks.GOLD_BLOCK);
        for(int side:new int[]{-1,1}){
            put(l,c.offset(x+side,2,z),Blocks.AMETHYST_BLOCK);
            put(l,c.offset(x,2,z+side),Blocks.AMETHYST_BLOCK);
            put(l,c.offset(x+side,3,z),Blocks.AMETHYST_CLUSTER);
            put(l,c.offset(x,3,z+side),Blocks.AMETHYST_CLUSTER);
        }
        put(l,c.offset(x,3,z),Blocks.SEA_LANTERN);
        for(int side:new int[]{-1,1})put(l,c.offset(x+side*2,0,z),Blocks.POTTED_ALLIUM);
    }
    private static void gazebo(ServerLevel l,BlockPos c,int x,int z){
        for(int dx=-6;dx<=6;dx++)for(int dz=-6;dz<=6;dz++){
            int r2=dx*dx+dz*dz;if(r2>36)continue;
            put(l,c.offset(x+dx,-1,z+dz),r2>24?Blocks.CALCITE:Blocks.CHERRY_PLANKS);
            if(r2>24&&r2<35&&Math.floorMod(dx+dz,4)==0)put(l,c.offset(x+dx,0,z+dz),Blocks.POTTED_ALLIUM);
            if(r2<=36)put(l,c.offset(x+dx,6,z+dz),r2>24?Blocks.CHERRY_PLANKS:Math.floorMod(dx+dz,7)==0?Blocks.SEA_LANTERN:Blocks.PINK_STAINED_GLASS);
        }
        for(int dx:new int[]{-4,4})for(int dz:new int[]{-4,4})for(int y=0;y<=5;y++)put(l,c.offset(x+dx,y,z+dz),Blocks.STRIPPED_CHERRY_LOG);
        for(int side:new int[]{-1,1}){
            put(l,c.offset(x+side*3,0,z),Blocks.CHERRY_STAIRS);
            put(l,c.offset(x+side*4,1,z),Blocks.LANTERN);
        }
        put(l,c.offset(x,7,z),Blocks.GOLD_BLOCK);
    }
    private static void tree(ServerLevel l,BlockPos c){
        for(int y=0;y<5;y++)put(l,c.above(y),Blocks.CHERRY_LOG);
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)for(int y=4;y<=6;y++)if(dx*dx+dz*dz+Math.abs(y-5)*3<=11)
            put(l,c.offset(dx,y,dz),Blocks.CHERRY_LEAVES);
        put(l,c.above(7),Blocks.SEA_LANTERN);
    }
    static void removeBuild(ServerLevel l,BlockPos c){
        if(!l.getBlockState(c.below(4)).is(Blocks.CYAN_GLAZED_TERRACOTTA)&&!l.getBlockState(c.below(4)).is(Blocks.PINK_GLAZED_TERRACOTTA)&&!l.getBlockState(c.below(4)).is(Blocks.MAGENTA_GLAZED_TERRACOTTA)&&!l.getBlockState(c.below(4)).is(Blocks.BLUE_GLAZED_TERRACOTTA))return;
        for(int x=-49;x<=49;x++)for(int z=-49;z<=49;z++)for(int y=-4;y<=9;y++){
            var at=c.offset(x,y,z);if(!l.isEmptyBlock(at))put(l,at,Blocks.AIR);
        }
    }
    static void clean(ServerLevel l,VisitData.Scene s,BlockPos c){
        var found=new ArrayList<Entity>();for(var e:l.getEntitiesOfClass(Entity.class,new AABB(c).inflate(90,110,90),e->owned(e,s)))found.add(e);
        found.forEach(e->{e.ejectPassengers();e.discard();});
    }
    static void stopMusic(ServerPlayer p){MUSIC_SESSIONS.remove(p.getUUID());HARVEST_MUSIC_SESSIONS.remove(p.getUUID());VisitAudio.stop(p);}
    static void startMusic(ServerPlayer p){if(MUSIC_SESSIONS.add(p.getUUID()))VisitAudio.play(p,"magic_garden_fly",true);}
    private static void startHarvestMusic(ServerPlayer p){if(HARVEST_MUSIC_SESSIONS.add(p.getUUID()))VisitAudio.play(p,"coraline_garden",true);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){
        MUSIC_SESSIONS.remove(p.getUUID());HARVEST_MUSIC_SESSIONS.remove(p.getUUID());
        // The client clears its local sound when its player/world disappears;
        // do not try to send a packet through a closing connection.
    }}
    static void start(ServerPlayer p,VisitData.Scene s,ServerLevel l){
        stopMusic(p);
        var c=center(p,s);build(l,c);clean(l,s,c);var st=s.loop;
        st.flyingGardenStage=0;st.flyingGardenVoiceUntil=OtherMotherLoop.now(p)+VisitAudio.length(ActivityWorld.introSound(OtherMotherActivities.Activity.MAGIC_GARDEN));
        st.flyingGardenPhaseUntil=0;st.flyingGardenHarvestUntil=0;st.flyingGardenCollectedMask=0;st.flyingGardenHarvestCount=0;
        Arrays.fill(st.flyingGardenRegrowAt,0);Arrays.fill(st.flyingGardenPickCount,0);
        // Entry builds the activity before the dimension packet reaches the client.
        // Starting music here can be stopped by the client's world transition.
        ensureFather(p,s,l,c);ensureMantis(l,s,c);ensurePlants(l,s,c);OtherMotherLoop.changed(p);
    }
    static void prepare(ServerPlayer p,VisitData.Scene s,ServerLevel l,BlockPos c){
        LoopProps.hold(l,c);LoopProps.hold(l,c.south(43));LoopProps.hold(l,c.south(68));build(l,c);ensureFather(p,s,l,c);ensureMantis(l,s,c);ensurePlants(l,s,c);
        l.setWeatherParameters(120000,0,false,false);l.setRainLevel(0);l.setThunderLevel(0);
    }
    private static ParentEntity father(ServerLevel l,VisitData.Scene s,BlockPos c){
        var list=l.getEntitiesOfClass(ParentEntity.class,new AABB(c.south(35)).inflate(30,110,90),e->owned(e,s));return list.isEmpty()?null:list.get(0);
    }
    private static void ensureFather(ServerPlayer p,VisitData.Scene s,ServerLevel l,BlockPos c){
        if(father(l,s,c)!=null)return;
        var family=ParentAccess.family(p.server,s.owner);var realm=RealmData.get(p.server).realms.get(s.door);
        var dad=ModEntities.DAD.get().create(l);if(dad==null)return;
        dad.makeOtherParent(family==null?0:Math.max(0,family.dadSkin));dad.setNoAi(true);mark(dad,s);
        dad.getPersistentData().putUUID("RealmDoor",s.door);if(realm!=null)dad.getPersistentData().putInt("RealmGeneration",realm.parentGeneration);
        dad.setPos(c.getX()+2.5,c.getY(),c.getZ()+42.5);dad.setYRot(180);dad.setYBodyRot(180);l.addFreshEntity(dad);
    }
    private static ScenePropEntity mantis(ServerLevel l,VisitData.Scene s,BlockPos c){
        var list=l.getEntitiesOfClass(ScenePropEntity.class,new AABB(c.south(40)).inflate(30,110,90),e->owned(e,s)&&e.kind()==ScenePropEntity.GARDEN_MANTIS);
        return list.isEmpty()?null:list.get(0);
    }
    private static void ensureMantis(ServerLevel l,VisitData.Scene s,BlockPos c){
        if(mantis(l,s,c)!=null)return;
        var craft=LoopProps.spawn(l,s,ScenePropEntity.GARDEN_MANTIS,new Vec3(c.getX()+.5,c.getY()+1,c.getZ()+42.5),-1);
        mark(craft,s);craft.setYRot(180);craft.setNoGravity(true);
    }
    private static BlockPos plantPos(BlockPos c,int slot,int picks){
        // Collectibles grow inside designed beds. Rare specimens migrate to a
        // different room when picked, rather than reappearing at one campsite.
        int[][] rooms={{-29,10},{29,10},{-28,27},{28,27},{-29,-11},{29,-11},
            {-12,10},{12,10},{-16,-27},{16,-27}};
        int room=Math.floorMod(slot*7+(slot/10)*3+(slot%10<=1?picks*4:0),rooms.length);
        double angle=slot*2.399963229728653+picks*.31,radius=1.5+(slot/10)*.8;
        return c.offset(rooms[room][0]+(int)Math.round(Math.cos(angle)*radius),0,
            rooms[room][1]+(int)Math.round(Math.sin(angle)*radius));
    }
    private static BlockPos plantSite(ServerLevel l,BlockPos c,int slot,int picks){
        var origin=plantPos(c,slot,picks);
        for(int radius=0;radius<=8;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
            var at=origin.offset(dx,0,dz);
            if(l.isEmptyBlock(at)&&l.isEmptyBlock(at.above())&&
                (l.getBlockState(at.below()).is(Blocks.GRASS_BLOCK)||l.getBlockState(at.below()).is(Blocks.MOSS_BLOCK)))return at;
        }
        return origin;
    }
    private static ItemStack plantPrize(int slot){return switch(slot%10){
        case 0->new ItemStack(Items.ENCHANTED_GOLDEN_APPLE);
        case 1->new ItemStack(Items.CHORUS_FRUIT,2);
        case 2->new ItemStack(Items.GLOW_BERRIES,4);
        case 3->new ItemStack(Items.SWEET_BERRIES,5);
        case 4->new ItemStack(Items.PINK_TULIP,2);
        case 5->new ItemStack(Items.ALLIUM,2);
        case 6->new ItemStack(Items.GOLDEN_CARROT,3);
        case 7->new ItemStack(Items.AMETHYST_SHARD,4);
        case 8->new ItemStack(Items.SUNFLOWER,2);
        default->new ItemStack(Items.BLUE_ORCHID,2);
    };}
    private static void ensurePlants(ServerLevel l,VisitData.Scene s,BlockPos c){
        var existing=l.getEntitiesOfClass(ScenePropEntity.class,new AABB(c).inflate(48,6,48),e->owned(e,s)&&e.kind()==ScenePropEntity.GARDEN_PLANT);
        var occupied=new HashSet<BlockPos>();for(var prop:existing){occupied.add(prop.blockPosition());prop.setGlowingTag(false);prop.food(0,ItemStack.EMPTY);}
        for(int slot=0;slot<PLANTS;slot++){
            if((s.loop.flyingGardenCollectedMask&(1L<<slot))!=0)continue;
            int id=slot;var at=plantSite(l,c,slot,s.loop.flyingGardenPickCount[slot]);
            if(existing.stream().anyMatch(e->e.slot==id))continue;
            BlockPos free=null;
            for(int radius=0;radius<=8&&free==null;radius++)for(int dx=-radius;dx<=radius&&free==null;dx++)for(int dz=-radius;dz<=radius;dz++){
                var candidate=at.offset(dx,0,dz);
                if(!occupied.contains(candidate)&&l.isEmptyBlock(candidate)&&l.isEmptyBlock(candidate.above())&&
                    (l.getBlockState(candidate.below()).is(Blocks.GRASS_BLOCK)||l.getBlockState(candidate.below()).is(Blocks.MOSS_BLOCK))){free=candidate;break;}
            }
            if(free==null)continue;
            var prop=LoopProps.spawn(l,s,ScenePropEntity.GARDEN_PLANT,Vec3.atBottomCenterOf(free),slot);mark(prop,s);occupied.add(free);
        }
    }
    private static double smooth(double t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
    private static Vec3 flight(BlockPos c,int stage,double t){
        double rise=stage==2?smooth(t):stage==3?1:1-smooth(t);
        double z=stage==2?42+17*smooth(t):stage==3?59:59-17*smooth(t);
        double x=stage==2?7*Math.sin(Math.PI*t):stage==3?0:5*Math.sin(Math.PI*t);
        return new Vec3(c.getX()+.5+x,c.getY()+1+49*rise,c.getZ()+.5+z);
    }
    private static void moveFlight(ServerPlayer p,VisitData.Scene s,BlockPos c,long now){
        var st=s.loop;var craft=mantis(p.serverLevel(),s,c);if(craft==null)return;
        double t=st.flyingGardenStage==3?1:1-Math.max(0,st.flyingGardenPhaseUntil-now)/(double)(st.flyingGardenStage==2?ASCENT:DESCENT);
        Vec3 target=flight(c,st.flyingGardenStage,t);
        craft.move(MoverType.SELF,target.subtract(craft.position()));
        craft.setYRot((float)(180+12*Math.sin(Math.PI*t)));
        if(p.tickCount%10==0)p.serverLevel().sendParticles(ParticleTypes.END_ROD,craft.getX(),craft.getY()-.3,craft.getZ(),5,1,.35,1,.02);
    }
    private static void board(ServerPlayer p,VisitData.Scene s,BlockPos c,long now){
        var level=p.serverLevel();ensureMantis(level,s,c);ensureFather(p,s,level,c);
        var craft=mantis(level,s,c);var dad=father(level,s,c);
        if(craft==null||dad==null)return;
        if(dad.getVehicle()!=craft&&!dad.startRiding(craft,true))return;
        if(p.getVehicle()!=craft&&!p.startRiding(craft,true))return;
        var st=s.loop;st.flyingGardenStage=1;st.flyingGardenPhaseUntil=now+VisitAudio.length("magic_garden_hop_on");
        VisitAudio.play(p,"magic_garden_hop_on",false);
        p.sendSystemMessage(Component.literal("Father: Hop aboard! Look down as we climb."));OtherMotherLoop.changed(p);
    }
    private static void recoverSeats(ServerPlayer p,VisitData.Scene s,BlockPos c){
        var craft=mantis(p.serverLevel(),s,c);var dad=father(p.serverLevel(),s,c);
        if(craft==null||dad==null){ensureMantis(p.serverLevel(),s,c);ensureFather(p,s,p.serverLevel(),c);craft=mantis(p.serverLevel(),s,c);dad=father(p.serverLevel(),s,c);}
        if(craft==null)return;
        if(dad!=null&&dad.getVehicle()!=craft)dad.startRiding(craft,true);
        if(p.isAlive()&&p.getVehicle()!=craft)p.startRiding(craft,true);
    }
    private static void land(ServerPlayer p,VisitData.Scene s,BlockPos c,long now){
        var st=s.loop;var craft=mantis(p.serverLevel(),s,c);if(craft!=null)craft.move(MoverType.SELF,flight(c,4,1).subtract(craft.position()));
        p.stopRiding();var dad=father(p.serverLevel(),s,c);if(dad!=null){dad.stopRiding();dad.setPos(c.getX()+2.5,c.getY(),c.getZ()+42.5);}
        p.teleportTo(p.serverLevel(),c.getX()+.5,c.getY(),c.getZ()+39.5,p.getYRot(),p.getXRot());p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);
        st.flyingGardenStage=5;st.flyingGardenHarvestUntil=now+HARVEST;startHarvestMusic(p);
        p.serverLevel().playSound(null,c,SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.PLAYERS,1,1.2F);
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(10,90,20));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
            Component.literal("COLLECT FLOWERS").withStyle(net.minecraft.ChatFormatting.YELLOW,net.minecraft.ChatFormatting.BOLD)));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
            Component.literal("Right-click flowers in the garden to collect prizes.")));
        OtherMotherLoop.changed(p);
    }
    private static boolean lookingAtPlant(ServerPlayer p,VisitData.Scene s){
        var near=p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,p.getBoundingBox().inflate(4),
            e->e.kind()==ScenePropEntity.GARDEN_PLANT&&owned(e,s));
        var eye=p.getEyePosition();var look=p.getLookAngle();
        for(var plant:near){
            var toward=plant.position().add(0,.6,0).subtract(eye);
            if(toward.lengthSqr()<16&&toward.normalize().dot(look)>.975&&p.hasLineOfSight(plant))return true;
        }
        return false;
    }
    static void tick(ServerPlayer p,VisitData.Scene s,BlockPos c){
        var st=s.loop;long now=OtherMotherLoop.now(p);
        if(!p.isAlive()){if(MUSIC_SESSIONS.contains(p.getUUID())||HARVEST_MUSIC_SESSIONS.contains(p.getUUID()))stopMusic(p);return;}
        // Father's introduction is silent underneath. Flight music starts only
        // at boarding, then remains the same client sound through descent.
        if(st.flyingGardenStage>=1&&st.flyingGardenStage<=4)startMusic(p);
        if(st.flyingGardenStage==5)startHarvestMusic(p);
        if(p.tickCount%20==0)prepare(p,s,p.serverLevel(),c);
        if(p.getY()<c.getY()-4||Math.abs(p.getX()-(c.getX()+.5))>48||Math.abs(p.getZ()-(c.getZ()+.5))>48){
            if(st.flyingGardenStage<=4)recoverSeats(p,s,c);
            else{p.teleportTo(p.serverLevel(),c.getX()+.5,c.getY(),c.getZ()+39.5,p.getYRot(),p.getXRot());p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);}
        }
        switch(st.flyingGardenStage){
            case 0->{if(now>=st.flyingGardenVoiceUntil){startMusic(p);board(p,s,c,now);}else if(p.tickCount%40==0)p.displayClientMessage(Component.literal("Father is introducing his garden. The mantis is waiting nearby."),true);}
            case 1->{recoverSeats(p,s,c);if(now>=st.flyingGardenPhaseUntil){st.flyingGardenStage=2;st.flyingGardenPhaseUntil=now+ASCENT;OtherMotherLoop.changed(p);}}
            case 2->{recoverSeats(p,s,c);moveFlight(p,s,c,now);if(now>=st.flyingGardenPhaseUntil){st.flyingGardenStage=3;
                st.flyingGardenPhaseUntil=now+VisitAudio.length("magic_garden_mother_said")+20;
                p.setYRot(180);p.setXRot(43);
                VisitAudio.play(p,"magic_garden_mother_said",false);
                OtherMotherLoop.changed(p);}}
            case 3->{recoverSeats(p,s,c);moveFlight(p,s,c,now);
                if(p.tickCount%16==0)for(int side:new int[]{-1,1})for(var at:new BlockPos[]{c.offset(side*17,1,-10),c.offset(side*21,1,12),c.offset(side*24,1,-24)})
                    p.serverLevel().sendParticles(ParticleTypes.END_ROD,at.getX()+.5,at.getY()+.5,at.getZ()+.5,5,2,.8,2,.01);
                if(now>=st.flyingGardenPhaseUntil){st.flyingGardenStage=4;
                st.flyingGardenPhaseUntil=now+DESCENT;OtherMotherLoop.changed(p);}}
            case 4->{recoverSeats(p,s,c);moveFlight(p,s,c,now);if(now>=st.flyingGardenPhaseUntil)land(p,s,c,now);}
            case 5->{
                if(p.tickCount%16==0)for(int[] fountain:new int[][]{{0,-5},{-32,12},{32,12}})
                    p.serverLevel().sendParticles(ParticleTypes.SPLASH,c.getX()+fountain[0]+.5,c.getY()+3.5,c.getZ()+fountain[1]+.5,4,.45,.5,.45,.02);
                if(p.tickCount%30==0)for(int[] bed:new int[][]{{-29,10},{29,10},{-28,27},{28,27},{-29,-11},{29,-11}})
                    p.serverLevel().sendParticles(p,ParticleTypes.END_ROD,true,c.getX()+bed[0]+.5,c.getY()+1.1,c.getZ()+bed[1]+.5,3,2,.55,2,.008);
                if(p.tickCount%10==0){
                    boolean changed=false;
                    for(int slot=0;slot<PLANTS;slot++)if((st.flyingGardenCollectedMask&(1L<<slot))!=0&&st.flyingGardenRegrowAt[slot]>0){
                        if(now>=st.flyingGardenRegrowAt[slot]){st.flyingGardenCollectedMask&=~(1L<<slot);st.flyingGardenRegrowAt[slot]=0;changed=true;
                            var at=plantSite(p.serverLevel(),c,slot,st.flyingGardenPickCount[slot]);p.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER,at.getX()+.5,at.getY()+.4,at.getZ()+.5,8,.3,.3,.3,.02);
                        }else if(now+30>=st.flyingGardenRegrowAt[slot]&&p.tickCount%20==0){var at=plantSite(p.serverLevel(),c,slot,st.flyingGardenPickCount[slot]);p.serverLevel().sendParticles(ParticleTypes.END_ROD,at.getX()+.5,at.getY()+.3,at.getZ()+.5,3,.2,.2,.2,.01);}
                    }
                    if(changed){ensurePlants(p.serverLevel(),s,c);OtherMotherLoop.changed(p);}
                }
                if(now>=st.flyingGardenHarvestUntil){st.flyingGardenStage=6;OtherMotherLoop.changed(p);ActivityWorld.ensureExit(p,s);
                p.sendSystemMessage(Component.literal("Harvest time is over. The return portal has appeared by the landing pad."));}
                else if(p.tickCount%20==0)p.displayClientMessage(Component.literal(((st.flyingGardenHarvestUntil-now+19)/20)+"s  |  Plants: "+st.flyingGardenHarvestCount+
                    (lookingAtPlant(p,s)?"  |  Right-click to harvest":"")),true);}
            case 6->{if(p.tickCount%40==0)ActivityWorld.ensureExit(p,s);}
            default->{}
        }
    }
    public static boolean interactProp(ServerPlayer p,VisitData.Scene s,ScenePropEntity prop){
        if(s.loop.activity!=OtherMotherActivities.Activity.MAGIC_GARDEN||prop.kind()!=ScenePropEntity.GARDEN_PLANT)return false;
        var st=s.loop;if(st.flyingGardenStage!=5){p.displayClientMessage(Component.literal("Wait until the mantis has landed before harvesting."),true);return true;}
        int slot=prop.slot;if(slot<0||slot>=PLANTS||(st.flyingGardenCollectedMask&(1L<<slot))!=0)return true;
        st.flyingGardenCollectedMask|=1L<<slot;st.flyingGardenHarvestCount++;
        st.flyingGardenPickCount[slot]++;
        st.flyingGardenRegrowAt[slot]=slot%10==0&&st.flyingGardenPickCount[slot]>=2?0:
            OtherMotherLoop.now(p)+(slot%10==0?280:slot%10==1?220:slot%10<=3?160:120);
        OtherMotherLoop.changed(p);
        ItemStack reward=plantPrize(slot);p.getInventory().add(reward);if(!reward.isEmpty())p.drop(reward,false);
        p.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER,prop.getX(),prop.getY()+.7,prop.getZ(),12,.5,.5,.5,.05);
        p.serverLevel().playSound(null,prop.blockPosition(),SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.PLAYERS,.6F,slot%10==0?1.6F:1.1F);
        p.displayClientMessage(Component.literal("Picked "+plantPrize(slot).getHoverName().getString()),true);prop.discard();return true;
    }
}
