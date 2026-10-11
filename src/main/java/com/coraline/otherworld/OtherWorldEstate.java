package com.coraline.otherworld;

import java.util.*;
import com.coraline.block.TinyDoorBlock;
import com.coraline.registry.ModBlocks;
import net.minecraft.core.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.levelgen.Heightmap;

/** Builds the deliberately perfected home over the copied landscape. */
public final class OtherWorldEstate {
    public static final int VERSION=6;
    private static final Map<ServerLevel,Map<UUID,Map<BlockPos,BlockState>>> temporaryWhite=new WeakHashMap<>();
    private static final int HALF_X=20,MIN_Z=-14,MAX_Z=16;
    private OtherWorldEstate(){}
    /** Horizontal distance from the complete landscaped estate, not its center. */
    public static double outsideDistance(BlockPos house,BlockPos at){
        double x=Math.max(0,Math.abs(at.getX()-house.getX())-43);
        double z=Math.max(0,Math.max(house.getZ()-20-at.getZ(),at.getZ()-house.getZ()-87));
        return Math.hypot(x,z);
    }
    public static boolean outsideHouse(BlockPos house,BlockPos at){return Math.abs(at.getX()-house.getX())>27||at.getZ()<house.getZ()-21||at.getZ()>house.getZ()+27;}

    public static void build(ServerLevel source,ServerLevel level,DoorData.Door door,RealmData.Realm realm){
        build(source,level,door,realm,chooseSite(source,door.pos));
    }
    static void build(ServerLevel source,ServerLevel level,DoorData.Door door,RealmData.Realm realm,BlockPos sourceCenter){
        // The entire house must clear the terrain, not merely its center column.
        // RealmCopy preserves world Y and only offsets X/Z.
        int ground=lowestSafeFloor(source,sourceCenter);
        int base=Math.max(level.getMinBuildHeight()+16,Math.min(level.getMaxBuildHeight()-38,ground));
        BlockPos center=new BlockPos(sourceCenter.getX()+realm.dx,base,sourceCenter.getZ()+realm.dz);
        realm.house=center;realm.porch=center.offset(0,1,22);realm.piano=center.offset(-11,1,-7);realm.table=center.offset(10,1,0);

        BlockPos old=door.pos.offset(realm.dx,0,realm.dz);
        if(!old.equals(center.offset(-HALF_X+1,1,8))&&level.getBlockState(old).is(ModBlocks.TINY_DOOR.get())){
            level.removeBlockEntity(old);level.setBlock(old,Blocks.AIR.defaultBlockState(),3);
        }
        prepareFoundation(source,level,center,realm.dx,realm.dz);
        shell(level,center);
        rooms(level,center);
        roof(level,center);
        OtherWorldInterior.decorate(level,center);
        garden(source,level,center,realm.dx,realm.dz,realm.radius);
        finishSideStairs(source,level,center,realm.dx,realm.dz);

        // Bring the tiny door forward to the room-facing surface of its alcove.
        realm.doorPos=center.offset(-HALF_X+1,1,8);
        var doorState=ModBlocks.TINY_DOOR.get().defaultBlockState().setValue(TinyDoorBlock.FACING,Direction.EAST).setValue(TinyDoorBlock.PORTAL,true).setValue(TinyDoorBlock.OPEN,true);
        level.setBlock(realm.doorPos,doorState,3);
        // A standing player needs a two-block-high approach, not merely an
        // interactable portal at floor level. Decoration used to refill this cap.
        for(BlockPos passage:new BlockPos[]{realm.doorPos.above(),realm.doorPos.east(),realm.doorPos.east().above()})
            level.setBlock(passage,Blocks.AIR.defaultBlockState(),3);
        if(level.getBlockEntity(realm.doorPos) instanceof DoorBlockEntity block){block.doorId=door.id;block.setChanged();}

        realm.bed=center.offset(11,9,-7);
        BlockState foot=Blocks.BLUE_BED.defaultBlockState().setValue(BedBlock.FACING,Direction.SOUTH).setValue(BedBlock.PART,BedPart.FOOT);
        level.setBlock(realm.bed,foot,3);level.setBlock(realm.bed.south(),foot.setValue(BedBlock.PART,BedPart.HEAD),3);
        realm.estateVersion=VERSION;
    }

    static BlockPos chooseSite(ServerLevel level,BlockPos door){
        BlockPos best=door;double bestScore=Double.MAX_VALUE;
        for(int ox=-20;ox<=20;ox+=5)for(int oz=-20;oz<=20;oz+=5){
            int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;
            // Sample the full footprint, including its corners. A flat center in a
            // hillside was previously chosen and buried half of the house.
            for(int x:new int[]{-20,-10,0,10,20})for(int z:new int[]{-14,-7,0,8,16}){
                int y=naturalSurface(level,door.getX()+ox+x,door.getZ()+oz+z);
                min=Math.min(min,y);max=Math.max(max,y);
            }
            double score=(max-min)*18+Math.hypot(ox,oz);
            if(score<bestScore){bestScore=score;best=new BlockPos(door.getX()+ox,max,door.getZ()+oz);}
        }return best;
    }
    private static int lowestSafeFloor(ServerLevel source,BlockPos center){
        int highest=source.getMinBuildHeight()+16;
        for(int x=-HALF_X;x<=HALF_X;x++)for(int z=MIN_Z;z<=MAX_Z;z++)
            highest=Math.max(highest,naturalSurface(source,center.getX()+x,center.getZ()+z));
        return highest;
    }
    private static int naturalSurface(ServerLevel level,int x,int z){
        int top=level.getHeight(Heightmap.Types.WORLD_SURFACE,x,z);
        for(int y=Math.min(level.getMaxBuildHeight()-2,top);y>level.getMinBuildHeight()+4;y--){
            var p=new BlockPos(x,y,z);var s=level.getBlockState(p);
            // Surface dirt can be only one block thick above a cave or an exposed
            // copied shell. Requiring three solid blocks selected stone deep below.
            if(natural(s)&&(natural(level.getBlockState(p.below()))||
                natural(level.getBlockState(p.north()))||natural(level.getBlockState(p.south()))||
                natural(level.getBlockState(p.east()))||natural(level.getBlockState(p.west()))))return y+1;
        }
        return Math.max(level.getMinBuildHeight()+5,level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z));
    }
    private static boolean natural(BlockState s){return s.is(BlockTags.BASE_STONE_OVERWORLD)||s.is(BlockTags.DIRT)||s.is(Blocks.GRASS_BLOCK)||s.is(Blocks.SAND)||s.is(Blocks.RED_SAND)||s.is(Blocks.GRAVEL)||s.is(Blocks.CLAY)||s.is(Blocks.SNOW_BLOCK);}
    private static boolean solid(ServerLevel l,BlockPos p){return l.getBlockState(p).isFaceSturdy(l,p,Direction.UP);}

    private static void prepareFoundation(ServerLevel source,ServerLevel l,BlockPos c,int dx,int dz){
        // Remove flowers before replacing the copied ground beneath them. Otherwise
        // neighbor updates pop them into item entities inside the finished rooms.
        for(int x=-HALF_X-8;x<=HALF_X+8;x++)for(int z=MIN_Z-8;z<=MAX_Z+8;z++)
            for(int y=c.getY()-8;y<=c.getY()+2;y++){
                BlockPos p=c.offset(x,y-c.getY(),z);
                if(l.getBlockState(p).is(BlockTags.FLOWERS))l.setBlock(p,Blocks.AIR.defaultBlockState(),2);
            }
        for(int x=-HALF_X;x<=HALF_X;x++)for(int z=MIN_Z;z<=MAX_Z;z++){
            int wx=c.getX()+x,wz=c.getZ()+z;
            clearAbove(l,wx,wz,c.getY(),34);
            var p=c.offset(x,-1,z);
            fillDown(l,p,(Math.abs(x)==HALF_X||z==MIN_Z||z==MAX_Z)?Blocks.DEEPSLATE_BRICKS.defaultBlockState():Blocks.STONE_BRICKS.defaultBlockState(),
                Math.max(16,c.getY()-naturalSurface(source,wx-dx,wz-dz)+4));
            l.setBlock(c.offset(x,0,z),Blocks.POLISHED_DIORITE.defaultBlockState(),2);
        }
        // Feather the raised foundation into the original terrain instead of
        // leaving its edge as a suspended rectangular slab.
        for(int x=-HALF_X-8;x<=HALF_X+8;x++)for(int z=MIN_Z-8;z<=MAX_Z+8;z++){
            int distance=Math.max(Math.abs(x)-HALF_X,Math.max(MIN_Z-z,z-MAX_Z));
            if(distance<1||distance>8)continue;
            int wx=c.getX()+x,wz=c.getZ()+z;
            int naturalY=naturalSurface(source,wx-dx,wz-dz)-1;
            int y=(int)Math.round((c.getY()-1)*(1-distance/8.0)+naturalY*distance/8.0);
            clearAbove(l,wx,wz,y,1);
            fillDown(l,new BlockPos(wx,y,wz),Blocks.DIRT.defaultBlockState(),Math.max(4,y-naturalY+3));
            l.setBlock(new BlockPos(wx,y,wz),Blocks.MOSS_BLOCK.defaultBlockState(),2);
        }
        // A broad, stepped porch disguises height changes instead of leaving a sheer platform edge.
        for(int z=17;z<=23;z++)for(int x=-9;x<=9;x++){
            int y=z<=18?0:z<=20?-1:z<=22?-2:-3;
            fillDown(l,c.offset(x,y,z),Blocks.STONE_BRICKS.defaultBlockState(),Math.max(16,c.getY()+y-naturalSurface(source,c.getX()+x-dx,c.getZ()+z-dz)+4));
            l.setBlock(c.offset(x,y,z),Blocks.POLISHED_DIORITE.defaultBlockState(),2);
        }
    }
    private static void clearAbove(ServerLevel l,int x,int z,int floor,int minimumHeight){
        int top=Math.min(l.getMaxBuildHeight()-1,Math.max(floor+minimumHeight,l.getHeight(Heightmap.Types.WORLD_SURFACE,x,z)));
        for(int y=floor+1;y<=top;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);
    }
    private static void fillDown(ServerLevel l,BlockPos p,BlockState state,int depth){for(int i=0;i<depth;i++){var q=p.below(i);if(i>0&&solid(l,q))break;l.setBlock(q,state,2);}}

    private static void shell(ServerLevel l,BlockPos c){
        BlockState siding=Blocks.CHERRY_PLANKS.defaultBlockState(),trim=Blocks.SMOOTH_QUARTZ.defaultBlockState();
        for(int y=1;y<=15;y++)for(int x=-HALF_X;x<=HALF_X;x++)for(int z=MIN_Z;z<=MAX_Z;z++){
            boolean wall=Math.abs(x)==HALF_X||z==MIN_Z||z==MAX_Z;
            if(!wall)continue;
            BlockState wallState=siding;
            if(y==1||y==8||y==15||Math.abs(x)%8==0&&z==MAX_Z||Math.abs(z)%7==0&&Math.abs(x)==HALF_X)wallState=trim;
            l.setBlock(c.offset(x,y,z),wallState,2);
        }
        for(int x=-19;x<=19;x++)for(int z=-13;z<=15;z++){
            l.setBlock(c.offset(x,0,z),Blocks.DARK_OAK_PLANKS.defaultBlockState(),2);
            l.setBlock(c.offset(x,8,z),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
            l.setBlock(c.offset(x,15,z),Blocks.DARK_OAK_PLANKS.defaultBlockState(),2);
        }
        // Separate music room, foyer, and kitchen. Their openings are full-height
        // walkable arches, so neither parent needs an unreliable door path.
        for(int x=-19;x<=-2;x++)for(int y=1;y<=7;y++)
            if(!(x==-11&&y<=2))l.setBlock(c.offset(x,y,-2),Blocks.DARK_OAK_PLANKS.defaultBlockState(),2);
        for(int z=-13;z<=15;z++)for(int y=1;y<=7;y++)
            if(!(z>=6&&z<=9&&y<=4))l.setBlock(c.offset(3,y,z),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
        windows(l,c);porch(l,c);
    }
    private static void windows(ServerLevel l,BlockPos c){
        // Solid stained-glass blocks fill the wall depth; panes here used to leave
        // open seams and visually detached window frames.
        for(int y:new int[]{3,4,5,10,11,12}){
            for(int x:new int[]{-15,-14,-8,-7,7,8,14,15})for(int z:new int[]{MIN_Z,MAX_Z})
                if(!(z==MAX_Z&&x>=-8&&x<=8&&y<=5))l.setBlock(c.offset(x,y,z),Blocks.YELLOW_STAINED_GLASS.defaultBlockState(),3);
            for(int z:new int[]{-9,-8,-1,0,12,13})for(int x:new int[]{-HALF_X,HALF_X})
                if(!(x==-HALF_X&&z>=7&&z<=9&&y<=5))l.setBlock(c.offset(x,y,z),Blocks.YELLOW_STAINED_GLASS.defaultBlockState(),3);
        }
        for(int x:new int[]{-16,-6,6,16})for(int z:new int[]{MIN_Z,MAX_Z})for(int y:new int[]{2,6,9,13})
            l.setBlock(c.offset(x,y,z),Blocks.SMOOTH_QUARTZ.defaultBlockState(),2);
    }
    private static void porch(ServerLevel l,BlockPos c){
        for(int x=-13;x<=13;x++)for(int z=17;z<=22;z++){
            l.setBlock(c.offset(x,0,z),Blocks.POLISHED_DIORITE.defaultBlockState(),2);
            if(Math.abs(x)>9)fillDown(l,c.offset(x,-1,z),Blocks.STONE_BRICKS.defaultBlockState(),48);
        }
        for(int x:new int[]{-12,-6,6,12})for(int y=1;y<8+(14-Math.abs(x))/4;y++)
            l.setBlock(c.offset(x,y,22),Blocks.QUARTZ_PILLAR.defaultBlockState(),2);
        // Keep porch shingles outside the upstairs wall, not through its rooms.
        for(int x=-14;x<=14;x++)for(int z=16;z<=23;z++){
            int roofY=8+(14-Math.abs(x))/4;
            l.setBlock(c.offset(x,roofY,z),Blocks.DEEPSLATE_TILES.defaultBlockState(),2);
            if(z==23&&Math.abs(x)<=10)for(int y=8;y<roofY;y++)l.setBlock(c.offset(x,y,z),Blocks.PINK_TERRACOTTA.defaultBlockState(),2);
        }
        for(int x=-13;x<=13;x++)if(Math.abs(x)>2&&Math.abs(x)!=6&&Math.abs(x)!=12)l.setBlock(c.offset(x,1,22),Blocks.BIRCH_FENCE.defaultBlockState(),3);
        for(int z=17;z<=22;z++)for(int x:new int[]{-13,13})l.setBlock(c.offset(x,1,z),Blocks.BIRCH_FENCE.defaultBlockState(),3);
        // Open lattice and spaced piers soften the raised porch foundation.
        for(int x=-13;x<=13;x++)if(Math.abs(x)>4)for(int y=-1;y>=-5;y--)
            l.setBlock(c.offset(x,y,22),x%5==0?Blocks.POLISHED_DIORITE.defaultBlockState():Blocks.DARK_OAK_FENCE.defaultBlockState(),2);
        // The front entrance and the main garden steps remain open and aligned.
        for(int y=1;y<=4;y++)for(int x=-1;x<=1;x++)l.setBlock(c.offset(x,y,MAX_Z),Blocks.AIR.defaultBlockState(),3);
        for(int x=-2;x<=2;x++)l.setBlock(c.offset(x,1,22),Blocks.AIR.defaultBlockState(),3);
        // A centered, continuous three-step flight joins the porch to the path.
        for(int z=23;z<=25;z++)for(int x=-3;x<=3;x++){
            int y=22-z;
            fillDown(l,c.offset(x,y-1,z),Blocks.STONE_BRICKS.defaultBlockState(),16);
            l.setBlock(c.offset(x,y,z),Blocks.POLISHED_DIORITE_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING,Direction.NORTH),3);
            l.setBlock(c.offset(x,y+1,z),Blocks.AIR.defaultBlockState(),3);
            l.setBlock(c.offset(x,y+2,z),Blocks.AIR.defaultBlockState(),3);
        }
        l.setBlock(c.offset(0,7,19),Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true),3);
    }

    private static void rooms(ServerLevel l,BlockPos c){
        // The Little Door is inset into the western wall of a blue sitting room.
        // Keep the two blocks immediately inside it clear for tunnel arrivals.
        for(int x=-17;x<=-15;x++)for(int y=1;y<=3;y++)
            l.setBlock(c.offset(x,y,1),y==3?Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState():Blocks.BRICKS.defaultBlockState(),2);
        for(int z:new int[]{6,10})for(int y=1;y<=4;y++)
            l.setBlock(c.offset(-HALF_X,y,z),Blocks.STRIPPED_CHERRY_LOG.defaultBlockState(),2);
        for(int z=6;z<=10;z++)l.setBlock(c.offset(-HALF_X,4,z),Blocks.SMOOTH_QUARTZ.defaultBlockState(),2);
        l.setBlock(c.offset(-16,1,2),Blocks.SHROOMLIGHT.defaultBlockState(),3);

        // Red wood, bookcases, and picture-like glazed tiles make the study distinct.
        for(int x=-19;x<=-3;x++)for(int z=-13;z<=-3;z++)l.setBlock(c.offset(x,0,z),Blocks.CRIMSON_PLANKS.defaultBlockState(),2);
        placeStudyDoor(l,c,false);

        // Cream kitchen at the back, with a distinct dark-wood supper room in front.
        for(int x=4;x<=19;x++)for(int z=-13;z<=-5;z++)
            l.setBlock(c.offset(x,0,z),(x+z)%2==0?Blocks.SMOOTH_QUARTZ.defaultBlockState():Blocks.CALCITE.defaultBlockState(),2);
        for(int x=5;x<=17;x++)kitchenCounter(l,c,x,-12);
        for(int z=-11;z<=-6;z++)kitchenCounter(l,c,18,z);
        l.setBlock(c.offset(6,1,-12),Blocks.SMOKER.defaultBlockState(),3);
        l.setBlock(c.offset(15,2,-12),Blocks.FLOWER_POT.defaultBlockState(),3);
        for(int x=4;x<=19;x++)for(int y=1;y<=7;y++)
            if(!(x>=11&&x<=13&&y<=4))l.setBlock(c.offset(x,y,-4),Blocks.DARK_OAK_PLANKS.defaultBlockState(),2);
        for(int x=4;x<=19;x++)for(int z=-3;z<=14;z++)l.setBlock(c.offset(x,0,z),Blocks.DARK_OAK_PLANKS.defaultBlockState(),2);
        // Supper is an open room: an old freestanding log perimeter obstructed
        // the entrance and separated the player from the table.

        stairs(l,c);bedroom(l,c);
        // Every hanging light is directly below a solid floor or ceiling.
        for(BlockPos p:new BlockPos[]{c.offset(-11,7,-7),c.offset(-9,7,9),c.offset(10,7,0),c.offset(-8,14,6),c.offset(10,14,-5)})
            l.setBlock(p,Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true),3);
    }
    private static void kitchenCounter(ServerLevel l,BlockPos c,int x,int z){
        l.setBlock(c.offset(x,1,z),Blocks.BIRCH_PLANKS.defaultBlockState(),2);
    }
    public static BlockPos studyDoor(BlockPos house){return house.offset(-11,1,-2);}
    public static BlockPos atticDoor(BlockPos house){return house.offset(21,16,7);}
    private static void placeStudyDoor(ServerLevel l,BlockPos house,boolean open){
        BlockPos p=studyDoor(house);
        BlockState state=Blocks.DARK_OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING,Direction.SOUTH)
            .setValue(DoorBlock.OPEN,open).setValue(DoorBlock.POWERED,false)
            .setValue(DoorBlock.HINGE,DoorHingeSide.LEFT);
        l.setBlock(p,state.setValue(DoorBlock.HALF,DoubleBlockHalf.LOWER),3);
        l.setBlock(p.above(),state.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),3);
    }
    /** The study door stays shut for the song and opens when Father goes to supper. */
    public static void lockStudy(ServerLevel l,BlockPos house,boolean locked){
        if(house==null)return;
        BlockPos p=studyDoor(house);
        if(!l.getBlockState(p).is(Blocks.DARK_OAK_DOOR))return;
        for(BlockPos half:new BlockPos[]{p,p.above()}){
            BlockState state=l.getBlockState(half);
            if(state.is(Blocks.DARK_OAK_DOOR)&&state.getValue(DoorBlock.OPEN)==locked)
                l.setBlock(half,state.setValue(DoorBlock.OPEN,!locked),3);
        }
    }
    private static void stairs(ServerLevel l,BlockPos c){
        BlockState stair=Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.NORTH);
        // One riser per block, with a continuous open headspace and a real landing.
        for(int step=0;step<8;step++)for(int x=-1;x<=1;x++){
            int z=12-step,y=1+step;
            for(int clear=y;clear<=10;clear++)l.setBlock(c.offset(x,clear,z),Blocks.AIR.defaultBlockState(),2);
            l.setBlock(c.offset(x,y,z),stair,3);
        }
        for(int x=-2;x<=2;x++)for(int z=2;z<=4;z++)l.setBlock(c.offset(x,8,z),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
        // Tread-level stringers support the rail all the way to the landing.
        for(int z=5;z<=12;z++)for(int x:new int[]{-2,2}){
            int rise=13-z;
            l.setBlock(c.offset(x,rise,z),Blocks.DARK_OAK_PLANKS.defaultBlockState(),2);
            l.setBlock(c.offset(x,rise+1,z),Blocks.DARK_OAK_FENCE.defaultBlockState(),3);
        }
        for(int z=2;z<=4;z++)for(int x:new int[]{-2,2})
            if(!(x==-2&&z>=3))l.setBlock(c.offset(x,9,z),Blocks.DARK_OAK_FENCE.defaultBlockState(),3);
    }
    private static void bedroom(ServerLevel l,BlockPos c){
        // Upstairs bedroom wall and doorway are reachable from the stair landing.
        for(int z=-13;z<=3;z++)for(int y=9;y<=14;y++)
            if(!(z>=0&&z<=2&&y<=11))l.setBlock(c.offset(4,y,z),Blocks.PURPLE_TERRACOTTA.defaultBlockState(),2);
        for(int x=5;x<=19;x++)for(int y=9;y<=14;y++)
            l.setBlock(c.offset(x,y,4),Blocks.PURPLE_TERRACOTTA.defaultBlockState(),2);
        for(int x=5;x<=19;x++)for(int z=-13;z<=3;z++)l.setBlock(c.offset(x,8,z),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
    }
    private static void roof(ServerLevel l,BlockPos c){
        // Every roof column is covered. The old implementation drew only two
        // sloping outlines and left the whole house open to the sky.
        for(int x=-22;x<=22;x++)for(int z=MIN_Z-2;z<=MAX_Z+2;z++){
            int roofY=16+(22-Math.abs(x))/3;
            BlockState shingle=((z+Math.abs(x))%5==0?Blocks.POLISHED_BLACKSTONE_BRICKS:Blocks.DEEPSLATE_TILES).defaultBlockState();
            l.setBlock(c.offset(x,roofY,z),shingle,2);
            if(z==MIN_Z||z==MAX_Z)for(int y=16;y<roofY;y++)
                l.setBlock(c.offset(x,y,z),(x%6==0?Blocks.SMOOTH_QUARTZ:Blocks.CHERRY_PLANKS).defaultBlockState(),2);
        }
        for(int z=MIN_Z-2;z<=MAX_Z+2;z++)l.setBlock(c.offset(0,24,z),Blocks.DARK_OAK_SLAB.defaultBlockState(),2);
        // Distinct turret, bay, and chimney follow the supplied house silhouette.
        for(int x=-23;x<=-13;x++)for(int z=10;z<=20;z++){
            int dx=Math.abs(x+18),dz=Math.abs(z-15);
            if(!towerFootprint(dx,dz))continue;
            BlockPos floor=c.offset(x,0,z);
            if(x<-HALF_X||z>MAX_Z)fillDown(l,floor.below(),Blocks.DEEPSLATE_BRICKS.defaultBlockState(),48);
            l.setBlock(floor,Blocks.CHERRY_PLANKS.defaultBlockState(),2);
            boolean outer=!towerFootprint(Math.abs(x+17),dz)||!towerFootprint(Math.abs(x+19),dz)||
                !towerFootprint(dx,Math.abs(z-14))||!towerFootprint(dx,Math.abs(z-16));
            for(int y=1;y<=25;y++){
                // The turret overlaps the house footprint. Its outside wall
                // must not become an unexplained freestanding wall indoors.
                if(x>=-19&&z<=15&&y<=15)continue;
                if(y>=16&&!outer)l.setBlock(c.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);
                if(outer)l.setBlock(c.offset(x,y,z),
                    y==1||y==8||y==17||y==25?Blocks.SMOOTH_QUARTZ.defaultBlockState():Blocks.CHERRY_PLANKS.defaultBlockState(),2);
            }
        }
        for(int y:new int[]{3,4,5,11,12,13,19,20,21})for(int x=-19;x<=-17;x++)
            l.setBlock(c.offset(x,y,20),Blocks.YELLOW_STAINED_GLASS.defaultBlockState(),3);
        for(int layer=0;layer<=8;layer++){
            int radius=Math.max(0,6-layer);
            for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)
                if(Math.max(Math.abs(x),Math.abs(z))==radius)
                    l.setBlock(c.offset(-18+x,26+layer,15+z),Blocks.DEEPSLATE_TILES.defaultBlockState(),2);
        }
        l.setBlock(c.offset(-18,35,15),Blocks.LIGHTNING_ROD.defaultBlockState(),3);
        for(int x:new int[]{-4,0,4})for(int y=18;y<=20;y++)
            l.setBlock(c.offset(x,y,MAX_Z),Blocks.YELLOW_STAINED_GLASS.defaultBlockState(),3);
        for(int x=-18;x<=18;x++)l.setBlock(c.offset(x,16+(22-Math.abs(x))/3,MAX_Z+3),Blocks.SMOOTH_QUARTZ.defaultBlockState(),2);
        for(int y=17;y<=29;y++)for(int x=13;x<=15;x++)for(int z=-11;z<=-9;z++)
            l.setBlock(c.offset(x,y,z),Blocks.BRICKS.defaultBlockState(),2);
        for(int x=10;x<=17;x++)for(int z=16;z<=19;z++){
            l.setBlock(c.offset(x,8,z),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
            l.setBlock(c.offset(x,15,z),Blocks.DEEPSLATE_TILES.defaultBlockState(),2);
            if(z==19)for(int y=9;y<=14;y++)l.setBlock(c.offset(x,y,z),
                (x==12||x==15)&&y>=10&&y<=12?Blocks.YELLOW_STAINED_GLASS.defaultBlockState():Blocks.CHERRY_PLANKS.defaultBlockState(),2);
            if(x==10||x==17)for(int y=9;y<=14;y++)l.setBlock(c.offset(x,y,z),
                z==18&&y>=10&&y<=12?Blocks.YELLOW_STAINED_GLASS.defaultBlockState():Blocks.CHERRY_PLANKS.defaultBlockState(),2);
        }
        for(int x=12;x<=15;x++)for(int y=9;y<=12;y++)l.setBlock(c.offset(x,y,16),Blocks.AIR.defaultBlockState(),2);
        balconyAndSideStairs(l,c);
    }
    private static boolean towerFootprint(int dx,int dz){return dx<=5&&dz<=5&&dx+dz<=8;}
    private static void balconyAndSideStairs(ServerLevel l,BlockPos c){
        // Retire the old second-floor balcony and its doorway on upgraded saves.
        for(int x=21;x<=26;x++)for(int z=-2;z<=19;z++)for(int y=0;y<=13;y++){
            var p=c.offset(x,y,z);var state=l.getBlockState(p);
            if(state.is(Blocks.CHERRY_PLANKS)||state.is(Blocks.CHERRY_STAIRS)||
                state.is(Blocks.BIRCH_FENCE)||state.is(Blocks.STRIPPED_CHERRY_LOG))
                l.setBlock(p,Blocks.AIR.defaultBlockState(),2);
        }
        for(int y=9;y<=11;y++)l.setBlock(c.offset(HALF_X,y,5),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
        // One straight, railed exterior flight climbs from the garden to the
        // attic-level landing. It never opens onto the second floor.
        BlockState stair=Blocks.CHERRY_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.NORTH);
        for(int z=24;z>=9;z--){
            int y=24-z;
            for(int x=23;x<=25;x++){
                for(int clear=1;clear<=3;clear++)l.setBlock(c.offset(x,y+clear,z),Blocks.AIR.defaultBlockState(),2);
                l.setBlock(c.offset(x,y-1,z),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
                l.setBlock(c.offset(x,y,z),stair,3);
            }
            for(int x:new int[]{22,26}){
                l.setBlock(c.offset(x,y,z),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
                l.setBlock(c.offset(x,y+1,z),Blocks.BIRCH_FENCE.defaultBlockState(),3);
                if(z%5==4)fillDown(l,c.offset(x,y,z),Blocks.STRIPPED_CHERRY_LOG.defaultBlockState(),
                    c.getY()+y-l.getMinBuildHeight()+1);
            }
        }
        for(int x=21;x<=26;x++)for(int z=6;z<=8;z++)l.setBlock(c.offset(x,15,z),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
        for(int x:new int[]{21,26})for(int z:new int[]{6,8})
            fillDown(l,c.offset(x,14,z),Blocks.STRIPPED_CHERRY_LOG.defaultBlockState(),c.getY()+15-l.getMinBuildHeight());
        for(int z=6;z<=8;z++)l.setBlock(c.offset(26,16,z),Blocks.BIRCH_FENCE.defaultBlockState(),3);
        for(int x=22;x<=26;x++)l.setBlock(c.offset(x,16,6),Blocks.BIRCH_FENCE.defaultBlockState(),3);
    }

    private static void finishSideStairs(ServerLevel source,ServerLevel l,BlockPos c,int dx,int dz){
        // Extend the side flight until it actually touches the copied terrain.
        BlockState step=Blocks.CHERRY_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.NORTH);
        for(int z=25;z<=44;z++){
            int y=24-z;
            int ground=naturalSurface(source,c.getX()+24-dx,c.getZ()+z-dz)-1;
            for(int x=23;x<=25;x++){
                BlockPos p=c.offset(x,y,z);
                clearAbove(l,p.getX(),p.getZ(),c.getY()+y-1,3);
                fillDown(l,p.below(),Blocks.STONE_BRICKS.defaultBlockState(),Math.max(8,c.getY()+y-ground+3));
                l.setBlock(p,step,3);
            }
            if(c.getY()+y<=ground+1){
                for(int next=z+1;next<=z+3;next++)for(int x=23;x<=25;x++){
                    int natural=naturalSurface(source,c.getX()+x-dx,c.getZ()+next-dz)-1;
                    l.setBlock(new BlockPos(c.getX()+x,natural,c.getZ()+next),Blocks.DIRT_PATH.defaultBlockState(),3);
                }
                break;
            }
        }
    }

    private static void garden(ServerLevel source,ServerLevel l,BlockPos c,int dx,int dz,int radius){
        int max=Math.min(86,Math.max(56,radius/2));
        int pathY=c.getY()-3;
        for(int z=26;z<=max;z++){
            int naturalPath=naturalSurface(source,c.getX()-dx,c.getZ()+z-dz)-1;
            pathY+=Integer.compare(naturalPath,pathY);
            for(int x=-42;x<=42;x++){
                int wx=c.getX()+x,wz=c.getZ()+z;
                int originalY=naturalSurface(source,wx-dx,wz-dz)-1;
                int leftRing=(x+23)*(x+23)+(z-51)*(z-51);
                int rightRing=(x-23)*(x-23)+(z-70)*(z-70);
                boolean path=Math.abs(x)<=2||(z==38||z==61||z==80)&&Math.abs(x)<=38||
                    leftRing>=100&&leftRing<=144||rightRing>=100&&rightRing<=144;
                boolean flowerBed=leftRing<100||rightRing<100||z>=45&&z<=77&&Math.abs(x)>=13&&Math.abs(x)<=17;
                int y=Math.abs(x)<=2?pathY:originalY;
                clearAbove(l,wx,wz,y,8);
                if(y>originalY)fillDown(l,new BlockPos(wx,y,wz),Blocks.DIRT.defaultBlockState(),y-originalY+3);
                // Copied terrain is only a visible shell. Seal each landscaped
                // column under paths and moss so small height changes cannot
                // reveal the blue void between neighboring surfaces.
                fillDown(l,new BlockPos(wx,y-1,wz),Blocks.DIRT.defaultBlockState(),12);
                l.setBlock(new BlockPos(wx,y,wz),path?Blocks.DIRT_PATH.defaultBlockState():Blocks.MOSS_BLOCK.defaultBlockState(),2);
                long h=net.minecraft.util.Mth.getSeed(wx,y,wz);
                if(!path&&Math.floorMod(h,flowerBed?3:8)==0){Block flower=switch(Math.floorMod(h,8)){case 0->Blocks.ALLIUM;case 1->Blocks.PINK_TULIP;case 2->Blocks.BLUE_ORCHID;case 3->Blocks.OXEYE_DAISY;case 4->Blocks.CORNFLOWER;case 5->Blocks.AZURE_BLUET;case 6->Blocks.LILY_OF_THE_VALLEY;default->Blocks.RED_TULIP;};l.setBlock(new BlockPos(wx,y+1,wz),flower.defaultBlockState(),3);}
                if((Math.abs(x)==40&&z%5!=0||z==max-2&&Math.abs(x)>5)&&!path)
                    l.setBlock(new BlockPos(wx,y+1,wz),gardenLeaves(Blocks.FLOWERING_AZALEA_LEAVES),3);
            }
        }
        for(int x:new int[]{-34,-25,25,34})for(int z:new int[]{32,46,79}){
            int wx=c.getX()+x,wz=c.getZ()+z,y=naturalSurface(source,wx-dx,wz-dz)-1;
            gardenTree(l,new BlockPos(wx,y,wz));
        }
        for(int z:new int[]{32,50,68,82})gardenArch(source,l,c,dx,dz,z);
        gardenGazebo(source,l,c,dx,dz,-25,67);
        gardenGazebo(source,l,c,dx,dz,25,51);
        int fx=c.getX()+10,fz=c.getZ()+40;
        int fy=naturalSurface(source,fx-dx,fz-dz)-1;
        BlockPos fountain=new BlockPos(fx,fy,fz);
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
            BlockPos p=fountain.offset(x,0,z);clearAbove(l,p.getX(),p.getZ(),fy,4);
            fillDown(l,p,Blocks.PRISMARINE_BRICKS.defaultBlockState(),Math.max(8,fy-naturalSurface(source,p.getX()-dx,p.getZ()-dz)+4));
            l.setBlock(p,Math.abs(x)==2||Math.abs(z)==2?Blocks.PRISMARINE_BRICKS.defaultBlockState():Blocks.WATER.defaultBlockState(),3);
        }
        l.setBlock(fountain,Blocks.SEA_LANTERN.defaultBlockState(),3);
        for(int y=1;y<=3;y++)l.setBlock(fountain.above(y),y==3?Blocks.WATER.defaultBlockState():Blocks.PRISMARINE_WALL.defaultBlockState(),3);
        for(int z:new int[]{28,38,54,70,82})for(int x:new int[]{-6,6}){
            int wx=c.getX()+x,wz=c.getZ()+z,y=naturalSurface(source,wx-dx,wz-dz)-1;
            var p=new BlockPos(wx,y+1,wz);
            l.setBlock(p,Blocks.COBBLESTONE_WALL.defaultBlockState(),3);
            l.setBlock(p.above(),Blocks.LANTERN.defaultBlockState(),3);
        }
    }
    private static void gardenTree(ServerLevel l,BlockPos ground){
        BlockPos p=ground.above();
        for(int i=0;i<8;i++)l.setBlock(p.above(i),Blocks.STRIPPED_CHERRY_LOG.defaultBlockState(),2);
        for(int x=-3;x<=3;x++)for(int y0=5;y0<=10;y0++)for(int z=-3;z<=3;z++)
            if(x*x+z*z+Math.abs(y0-8)*2<16)
                l.setBlock(p.offset(x,y0,z),(x==0&&z==0&&y0==8)?Blocks.SHROOMLIGHT.defaultBlockState():
                    gardenLeaves(Math.floorMod(x+z+y0,5)==0?Blocks.CHERRY_LEAVES:Blocks.FLOWERING_AZALEA_LEAVES),2);
        for(int x:new int[]{-2,2})l.setBlock(p.offset(x,4,0),Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true),3);
    }
    private static void gardenArch(ServerLevel source,ServerLevel l,BlockPos c,int dx,int dz,int z){
        int roofY=l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,c.getX(),c.getZ()+z)+5;
        for(int x:new int[]{-4,4})roofY=Math.max(roofY,naturalSurface(source,c.getX()+x-dx,c.getZ()+z-dz)+5);
        for(int x:new int[]{-4,4}){
            int ground=naturalSurface(source,c.getX()+x-dx,c.getZ()+z-dz);
            for(int y=ground;y<=roofY;y++)l.setBlock(new BlockPos(c.getX()+x,y,c.getZ()+z),Blocks.CHERRY_FENCE.defaultBlockState(),3);
        }
        for(int x=-4;x<=4;x++)l.setBlock(new BlockPos(c.getX()+x,roofY,c.getZ()+z),gardenLeaves(Blocks.FLOWERING_AZALEA_LEAVES),2);
        l.setBlock(new BlockPos(c.getX(),roofY-1,c.getZ()+z),Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true),3);
    }
    private static void gardenGazebo(ServerLevel source,ServerLevel l,BlockPos c,int dx,int dz,int ox,int oz){
        int floor=source.getMinBuildHeight()+5;
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)
            floor=Math.max(floor,naturalSurface(source,c.getX()+ox+x-dx,c.getZ()+oz+z-dz));
        BlockPos center=c.offset(ox,floor-c.getY(),oz);
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++){
            BlockPos p=center.offset(x,-1,z);
            clearAbove(l,p.getX(),p.getZ(),floor-1,8);
            fillDown(l,p,Blocks.STONE_BRICKS.defaultBlockState(),
                Math.max(12,floor-naturalSurface(source,p.getX()-dx,p.getZ()-dz)+3));
            l.setBlock(p,Math.abs(x)==5||Math.abs(z)==5?Blocks.SMOOTH_QUARTZ.defaultBlockState():Blocks.CHERRY_PLANKS.defaultBlockState(),2);
        }
        for(int x:new int[]{-5,5})for(int z:new int[]{-5,5})for(int y=0;y<=5;y++)
            l.setBlock(center.offset(x,y,z),Blocks.QUARTZ_PILLAR.defaultBlockState(),2);
        for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){
            int roofY=6+(6-Math.max(Math.abs(x),Math.abs(z)))/2;
            l.setBlock(center.offset(x,roofY,z),Blocks.DEEPSLATE_TILES.defaultBlockState(),2);
        }
        for(int x=-5;x<=5;x++)if(Math.abs(x)>1)for(int z:new int[]{-5,5})l.setBlock(center.offset(x,0,z),Blocks.CHERRY_FENCE.defaultBlockState(),3);
        l.setBlock(center.offset(0,8,0),Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true),3);
    }

    private static BlockState gardenLeaves(Block leaf){
        return leaf.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true);
    }

    private static void clearLegacyWhiteRing(ServerLevel l,BlockPos house,RealmData.Realm r){
        int cx=r.sourceDoor.getX()+r.dx,cz=r.sourceDoor.getZ()+r.dz;
        int low=r.radius-4,high=r.radius+33,oldY=house.getY()-4;
        for(int x=cx-high;x<=cx+high;x++)for(int z=cz-high;z<=cz+high;z++){
            double distance=Math.hypot(x-cx,z-cz);
            if(distance<low||distance>high)continue;
            BlockPos flat=new BlockPos(x,oldY,z);
            if(l.getBlockState(flat).is(Blocks.WHITE_CONCRETE))l.setBlock(flat,Blocks.AIR.defaultBlockState(),2);
            BlockPos top=new BlockPos(x,l.getHeight(Heightmap.Types.WORLD_SURFACE,x,z)-1,z);
            for(int below=0;below<=6;below++){
                BlockPos old=top.below(below);
                if(!old.equals(flat)&&l.getBlockState(old).is(Blocks.WHITE_CONCRETE))l.setBlock(old,Blocks.AIR.defaultBlockState(),2);
            }
        }
    }

    /** Reveal white ground only around a visitor at the island edge. */
    public static void extendWhite(ServerLevel l,RealmData.Realm r,BlockPos around){
        if(r.house==null||r.sourceDoor==null)return;
        var placed=temporaryWhite.computeIfAbsent(l,ignored->new HashMap<>()).computeIfAbsent(r.door,ignored->new HashMap<>());
        int cx=r.sourceDoor.getX()+r.dx,cz=r.sourceDoor.getZ()+r.dz;
        for(int x=around.getX()-8;x<=around.getX()+8;x++)for(int z=around.getZ()-8;z<=around.getZ()+8;z++){
            double dx=x-cx,dz=z-cz,dist=Math.hypot(dx,dz);
            if(dist<r.radius-1)continue;
            int innerX=cx+(int)Math.round(dx/dist*(r.radius-4));
            int innerZ=cz+(int)Math.round(dz/dist*(r.radius-4));
            int y=l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,innerX,innerZ)-1;
            y=Math.max(l.getMinBuildHeight()+4,Math.min(l.getMaxBuildHeight()-4,y));
            var p=new BlockPos(x,y,z);
            var previous=l.getBlockState(p);
            if(!previous.is(Blocks.WHITE_CONCRETE)){
                placed.putIfAbsent(p,previous);
                l.setBlock(p,Blocks.WHITE_CONCRETE.defaultBlockState(),2);
            }
        }
    }
    /** Restore transient edge blocks as soon as no visitor is close enough to see them. */
    public static void cleanupWhite(ServerLevel l){
        var byRealm=temporaryWhite.get(l);if(byRealm==null)return;
        for(var realmEntry:byRealm.entrySet()){
            var iterator=realmEntry.getValue().entrySet().iterator();
            while(iterator.hasNext()){
                var entry=iterator.next();var p=entry.getKey();boolean visible=false;
                for(var player:l.players()){
                    var tag=player.getPersistentData();
                    if(tag.hasUUID("CoralineVisitDoor")&&tag.getUUID("CoralineVisitDoor").equals(realmEntry.getKey())&&
                        player.distanceToSqr(p.getX()+.5,player.getY(),p.getZ()+.5)<18*18){visible=true;break;}
                }
                if(visible)continue;
                if(l.getBlockState(p).is(Blocks.WHITE_CONCRETE))l.setBlock(p,entry.getValue(),2);
                iterator.remove();
            }
        }
        byRealm.values().removeIf(Map::isEmpty);
    }
    public static void clearWhite(ServerLevel l){
        var byRealm=temporaryWhite.remove(l);if(byRealm==null)return;
        for(var positions:byRealm.values())for(var entry:positions.entrySet())
            if(l.getBlockState(entry.getKey()).is(Blocks.WHITE_CONCRETE))l.setBlock(entry.getKey(),entry.getValue(),2);
    }
}
