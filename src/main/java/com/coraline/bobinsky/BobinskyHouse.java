package com.coraline.bobinsky;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import java.util.*;
/** Durable original-state journal, committed before any temporary world blocks change. */
public final class BobinskyHouse {
    public static int radiusX(CompoundTag v){return v.getBoolean("DetailedTeaHouse")?8:4;}
    public static int radiusZ(CompoundTag v){return v.getBoolean("DetailedTeaHouse")?9:4;}
    public static AABB bounds(CompoundTag visit){var c=BlockPos.of(visit.getLong("House"));int x=radiusX(visit),z=radiusZ(visit);if(visit.getBoolean("DetailedTeaHouse")&&visit.getBoolean("TerrainClearanceV3"))return new AABB(c.offset(-x-2,-1,-z-3),c.offset(x+3,10,z+4));return new AABB(c.offset(-x,-1,-z-(visit.getBoolean("FloatingLanding")?2:0)),c.offset(x+1,10,z+1));}
    /** Entry policy excludes the porch and the terrain-restoration margin. */
    public static boolean insideRooms(CompoundTag visit,Vec3 position){
        var c=BlockPos.of(visit.getLong("House"));int x=radiusX(visit),z=radiusZ(visit);
        return new AABB(c.getX()-x+1,c.getY(),c.getZ()-z+1,
            c.getX()+x,c.getY()+9,c.getZ()+z).contains(position);
    }
    public static BlockPos front(CompoundTag v){return door(v).north();}
    public static BlockPos door(CompoundTag v){return BlockPos.of(v.getLong("House")).north(radiusZ(v));}
    /** Keep scenery until the entire footprint is outside a conservative forward view cone. */
    public static boolean unseen(ServerPlayer p,CompoundTag v){var box=bounds(v);if(box.inflate(.35).contains(p.position()))return false;var eye=p.getEyePosition();var look=p.getLookAngle();for(double x:new double[]{box.minX,box.maxX})for(double y:new double[]{box.minY,box.maxY})for(double z:new double[]{box.minZ,box.maxZ})if(new Vec3(x,y,z).subtract(eye).normalize().dot(look)>.05)return false;return box.getCenter().subtract(eye).normalize().dot(look)<=.05;}
    public static boolean deferRestore(ServerPlayer p,CompoundTag v){if(p==null||!p.isAlive()||p.level()!=p.server.overworld()||!v.contains("Originals")||!v.hasUUID("Door"))return false;var s=com.coraline.feast.VisitData.get(p.server).scenes.get(p.getUUID());return s!=null&&s.door.equals(v.getUUID("Door"))&&!unseen(p,v);}
    public static boolean isDoor(CompoundTag v,BlockPos pos){var d=door(v);return pos.equals(d)||pos.equals(d.above())||!v.getBoolean("Tea")&&(pos.equals(d.east())||pos.equals(d.east().above()));}
    public static void open(ServerLevel l,CompoundTag v,boolean open){var doors=v.getBoolean("Tea")?new BlockPos[]{door(v),door(v).above()}:new BlockPos[]{door(v),door(v).above(),door(v).east(),door(v).east().above()};for(var pos:doors){var s=l.getBlockState(pos);if(s.hasProperty(DoorBlock.OPEN))l.setBlock(pos,s.setValue(DoorBlock.OPEN,open),2);}}
    public static boolean natural(net.minecraft.world.level.block.state.BlockState s){
        var b=s.getBlock();return s.isAir()||s.is(net.minecraft.tags.BlockTags.DIRT)||s.is(net.minecraft.tags.BlockTags.LEAVES)||s.is(net.minecraft.tags.BlockTags.LOGS)||s.is(net.minecraft.tags.BlockTags.SAND)||b==Blocks.STONE||b==Blocks.GRAVEL||b==Blocks.GRASS||b==Blocks.TALL_GRASS||b==Blocks.FERN||b==Blocks.SNOW||b==Blocks.WATER||b==Blocks.DANDELION||b==Blocks.POPPY;
    }
    public static BlockPos find(ServerPlayer p){
        return find(p,4,4);
    }
    public static BlockPos find(ServerPlayer p,int rx,int rz){
        var level=p.serverLevel();var data=BobinskyVisitData.get(p.server);BlockPos best=null;int bestScore=Integer.MAX_VALUE;
        for(int i=0;i<36;i++){
            double angle=p.getRandom().nextDouble()*Math.PI*2;int radius=16+p.getRandom().nextInt(25);int x=(int)Math.floor(p.getX()+Math.cos(angle)*radius),z=(int)Math.floor(p.getZ()+Math.sin(angle)*radius);
            var probe=new BlockPos(x,(int)p.getY(),z);if(!level.hasChunkAt(probe)||!level.hasChunkAt(probe.offset(-rx-1,0,-rz-1))||!level.hasChunkAt(probe.offset(rx+1,0,rz+1)))continue;
            int y=ground(level,x,z,p.getY());if(y==Integer.MIN_VALUE)continue;
            var c=new BlockPos(x,y,z);if(y+10>=level.getMaxBuildHeight()||!level.getWorldBorder().isWithinBounds(c.offset(rx+1,0,rz+1))||!level.getWorldBorder().isWithinBounds(c.offset(-rx-1,0,-rz-1)))continue;
            int score=0;boolean reject=false;
            var candidate=new AABB(c.offset(-rx-1,-2,-rz-1),c.offset(rx+2,11,rz+2));for(var existing:BobinskyHouseProtection.houses(level))if(existing.getBoolean("Active")&&existing.contains("Originals")&&BobinskyHouse.bounds(existing).intersects(candidate)){reject=true;break;}if(reject)continue;
            for(var pos:BlockPos.betweenClosed(c.offset(-rx-1,-2,-rz-1),c.offset(rx+1,10,rz+1))){
                var s=level.getBlockState(pos);
                if(s.hasBlockEntity()||data.playerPlaced.contains(pos.asLong())||s.getBlock() instanceof BedBlock||net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()).getNamespace().equals("coraline")||s.getDestroySpeed(level,pos)<0){reject=true;break;}
                if(!natural(s))score+=20;
                if(pos.getY()>=y&&!s.isAir()&&s.isSolid())score+=2;
                if(pos.getY()==y-1&&s.isAir())score+=3;
                if(pos.getY()==y-1&&!s.getFluidState().isEmpty())score+=5;
            }
            // Prefer a clear site, but retain the least obstructed safe candidate
            // even when every score exceeds the old clearance cutoff. Both house
            // builders clear their entire journalled footprint before furnishing it.
            // Hard exclusions above still apply; fallback never overrides protection.
            if(reject||!level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,candidate).isEmpty())continue;
            if(score<bestScore){best=c;bestScore=score;if(score==0)break;}
        }
        return best;
    }
    /** Find nearby ground beneath trees instead of treating their canopy as a roof. */
    private static int ground(ServerLevel level,int x,int z,double playerY){
        int top=Math.min((int)Math.floor(playerY+8),level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z));
        int bottom=Math.max(level.getMinBuildHeight()+2,(int)Math.ceil(playerY-8));
        for(int y=top;y>=bottom;y--){
            var below=new BlockPos(x,y-1,z);var state=level.getBlockState(below);
            if(state.is(net.minecraft.tags.BlockTags.LOGS)||state.is(net.minecraft.tags.BlockTags.LEAVES))continue;
            if(state.isFaceSturdy(level,below,Direction.UP))return y;
        }
        return Integer.MIN_VALUE;
    }
    public static ListTag snapshot(ServerLevel level,BlockPos center){
        return snapshot(level,center,4,4);
    }
    public static ListTag snapshot(ServerLevel level,BlockPos center,int rx,int rz){
        var list=new ListTag();for(var pos:BlockPos.betweenClosed(center.offset(-rx,-1,-rz),center.offset(rx,9,rz))){var n=new CompoundTag();n.putLong("Pos",pos.asLong());n.put("State",NbtUtils.writeBlockState(level.getBlockState(pos)));var be=level.getBlockEntity(pos);if(be!=null)n.put("BlockEntity",be.saveWithFullMetadata());list.add(n);}return list;
    }
    public static void build(ServerLevel level,CompoundTag visit){
        var c=BlockPos.of(visit.getLong("House"));hold(level,c);
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=-1;y<=9;y++){
            var block=Blocks.AIR;boolean wall=Math.abs(x)==4||Math.abs(z)==4;
            if(y==-1)block=Blocks.DARK_OAK_PLANKS;
            else if(y<6&&wall)block=((x==0||x==1)&&z==-4&&y<3)?Blocks.AIR:Blocks.SPRUCE_PLANKS;
            else if(y==6||y>=7&&Math.abs(x)<=9-y)block=Blocks.DARK_OAK_PLANKS;
            if(y==2&&wall&&(x%3==0||z%3==0)&&!((x==0||x==1)&&z==-4))block=Blocks.GLASS;
            level.removeBlockEntity(c.offset(x,y,z));level.setBlock(c.offset(x,y,z),block.defaultBlockState(),2|16);
        }
        var door=door(visit);for(int dx=0;dx<=1;dx++)for(var half:new DoubleBlockHalf[]{DoubleBlockHalf.LOWER,DoubleBlockHalf.UPPER})level.setBlock(door.offset(dx,half==DoubleBlockHalf.UPPER?1:0,0),Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING,Direction.NORTH).setValue(DoorBlock.HINGE,dx==0?DoorHingeSide.LEFT:DoorHingeSide.RIGHT).setValue(DoorBlock.HALF,half),2);
        level.setBlock(c.offset(-2,0,2),Blocks.HAY_BLOCK.defaultBlockState(),2);level.setBlock(c.offset(2,0,2),Blocks.RED_WOOL.defaultBlockState(),2);level.setBlock(c.offset(0,5,0),Blocks.GLOWSTONE.defaultBlockState(),2);
    }
    public static void restore(ServerLevel level,CompoundTag visit){
        if(!visit.contains("Originals"))return;hold(level,BlockPos.of(visit.getLong("House")));
        // First restore states without neighbour drops; then reattach saved inventories.
        var states=level.holderLookup(net.minecraft.core.registries.Registries.BLOCK);
        for(var tag:visit.getList("Originals",10)){var n=(CompoundTag)tag;var pos=BlockPos.of(n.getLong("Pos"));level.removeBlockEntity(pos);level.setBlock(pos,NbtUtils.readBlockState(states,n.getCompound("State")),2|16);}
        for(var tag:visit.getList("Originals",10)){var n=(CompoundTag)tag;var pos=BlockPos.of(n.getLong("Pos"));if(n.contains("BlockEntity")&&level.getBlockEntity(pos)!=null){level.getBlockEntity(pos).load(n.getCompound("BlockEntity"));level.getBlockEntity(pos).setChanged();}level.updateNeighborsAt(pos,level.getBlockState(pos).getBlock());}
        level.getChunkSource().save(true); // Persist restoration before retiring its recovery journal.
        visit.remove("Originals");visit.remove("House");
    }
    private static final java.util.Map<ServerLevel,java.util.Map<Long,Long>> HELD=new java.util.WeakHashMap<>();
    public static void hold(ServerLevel level,BlockPos center){
        long now=level.getGameTime(),key=new net.minecraft.world.level.ChunkPos(center).toLong();var held=HELD.computeIfAbsent(level,ignored->new java.util.HashMap<>());var before=held.get(key);
        if(before!=null&&now>=before&&now-before<20)return;if(held.size()>256)held.entrySet().removeIf(entry->now-entry.getValue()>600);held.put(key,now);
        var origin=new net.minecraft.world.level.ChunkPos(center);for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)level.getChunkSource().addRegionTicket(TicketType.PORTAL,new net.minecraft.world.level.ChunkPos(origin.x+x,origin.z+z),2,center);}
}
