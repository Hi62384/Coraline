package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.secrets.SecretDialogue;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.*;
import java.util.*;

/** Bounded, terrain-following destination search. Never excavates pools or copied structures. */
public final class OutingSites {
    public record Site(BlockPos arrival,List<BlockPos> approach,List<BlockPos> walk,List<Vec3> water) {
        CompoundTag save(){var n=new CompoundTag();n.putLong("Arrival",arrival.asLong());n.putLongArray("Approach",approach.stream().mapToLong(BlockPos::asLong).toArray());n.putLongArray("Walk",walk.stream().mapToLong(BlockPos::asLong).toArray());var list=new ListTag();for(var v:water){var t=new CompoundTag();t.putDouble("X",v.x);t.putDouble("Y",v.y);t.putDouble("Z",v.z);list.add(t);}n.put("Water",list);return n;}
        static Site load(CompoundTag n){var water=new ArrayList<Vec3>();for(var t:n.getList("Water",10)){var v=(CompoundTag)t;water.add(new Vec3(v.getDouble("X"),v.getDouble("Y"),v.getDouble("Z")));}return new Site(BlockPos.of(n.getLong("Arrival")),Arrays.stream(n.getLongArray("Approach")).mapToObj(BlockPos::of).toList(),Arrays.stream(n.getLongArray("Walk")).mapToObj(BlockPos::of).toList(),water);}
    }
    public static Site current(VisitData.Scene s){return s.loop.event.contains("OutingSite")?Site.load(s.loop.event.getCompound("OutingSite")):null;}
    static boolean copied(RealmData.Realm r,BlockPos p){if(r==null||r.sourceDoor==null)return false;var center=r.sourceDoor.offset(r.dx,0,r.dz);return p.distSqr(new BlockPos(center.getX(),p.getY(),center.getZ()))<Math.pow(r.radius-24,2)&&(r.islandComplete||r.copiedChunks.contains(ChunkPos.asLong((p.getX()-r.dx)>>4,(p.getZ()-r.dz)>>4)));}
    static boolean feet(ServerLevel l,BlockPos p){
        if(!l.hasChunkAt(p)||!l.getFluidState(p).isEmpty()||!l.getFluidState(p.above()).isEmpty())return false;
        var support=l.getBlockState(p.below()).getCollisionShape(l,p.below());
        // Stairs/slabs are walking support even though their UP face isn't a full square.
        if(support.isEmpty()||support.max(Direction.Axis.Y)<.5||support.max(Direction.Axis.Y)>1)return false;
        var covering=l.getBlockState(p).getCollisionShape(l,p);double offset=covering.isEmpty()?0:covering.max(Direction.Axis.Y);if(offset>.125)offset=0;
        return l.noCollision(new AABB(p.getX()+.18,p.getY()+offset,p.getZ()+.18,p.getX()+.82,p.getY()+offset+1.9,p.getZ()+.82));
    }
    static BlockPos surface(ServerLevel l,int x,int z){if(!l.hasChunkAt(new BlockPos(x,0,z)))return null;int top=l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);var at=new BlockPos(x,top,z);for(int y=top+1;y>=top-3;y--){var p=at.atY(y);if(feet(l,p))return p;}return null;}
    static boolean natural(ServerLevel l,BlockPos p){var b=l.getBlockState(p.below());return b.is(BlockTags.DIRT)||b.is(Blocks.GRASS_BLOCK)||b.is(Blocks.MOSS_BLOCK)||b.is(Blocks.SAND)||b.is(Blocks.GRAVEL)||b.is(BlockTags.BASE_STONE_OVERWORLD);}
    static boolean clearing(ServerLevel l,RealmData.Realm r,VisitData.Scene s,BlockPos c,boolean sky){
        for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){var p=surface(l,c.getX()+x,c.getZ()+z);if(p==null||p.getY()!=c.getY()||!copied(r,p)||!natural(l,p)||SecretDialogue.secretArea(s.loop.secrets,p))return false;if(sky&&!l.canSeeSky(p.above()))return false;}
        return true;
    }
    static Site choose(ServerPlayer p,VisitData.Scene s,FamilyOuting.Subtype subtype){
        var r=RealmData.get(p.server).realms.get(s.door);if(r==null||r.house==null||r.porch==null)return null;var l=p.serverLevel();var start=r.porch;
        if(!feet(l,start)){start=null;for(int d=1;d<=12&&start==null;d++)for(int x=-d;x<=d&&start==null;x++)for(int z=-d;z<=d;z++){var q=surface(l,r.porch.getX()+x,r.porch.getZ()+z);if(q!=null&&copied(r,q)){start=q;break;}}}
        if(start==null)return null;
        var back=new HashMap<BlockPos,BlockPos>();var queue=new ArrayDeque<BlockPos>();back.put(start,start);queue.add(start);var candidates=new ArrayList<BlockPos>();
        while(!queue.isEmpty()&&back.size()<55000){var q=queue.remove();double distance=Math.hypot(q.getX()-r.house.getX(),q.getZ()-r.house.getZ());if(distance>=65&&distance<=180&&OtherWorldEstate.outsideDistance(r.house,q)>12&&!SecretDialogue.secretArea(s.loop.secrets,q)&&natural(l,q))candidates.add(q);
            for(var dir:Direction.Plane.HORIZONTAL){var side=q.relative(dir);if(Math.abs(side.getX()-r.house.getX())>210||Math.abs(side.getZ()-r.house.getZ())>210)continue;for(int dy:new int[]{0,1,-1}){var n=side.above(dy);if(back.containsKey(n)||!copied(r,n)||!feet(l,n))continue;if(dy==1&&!l.noCollision(new AABB(q.getX()+.18,q.getY()+1,q.getZ()+.18,q.getX()+.82,q.getY()+2.9,q.getZ()+.82)))continue;back.put(n,q);queue.add(n);break;}}
        }
        Collections.shuffle(candidates,new Random(s.loop.event.getLong("OutingSeed")));int tried=0;
        for(var c:candidates){if(++tried>1600)break;boolean waterside=subtype==FamilyOuting.Subtype.FISHING_TRIP||subtype==FamilyOuting.Subtype.BOAT_RIDE;
            if(!clearing(l,r,s,c,subtype==FamilyOuting.Subtype.STARGAZING)&&!waterside)continue;
            if(waterside&&!shoreClear(l,r,s,c))continue;
            var water=waterside?waterRoute(l,r,c):List.<Vec3>of();if(waterside&&water.isEmpty())continue;
            var path=new ArrayList<BlockPos>();var q=c;while(!q.equals(start)){path.add(q);q=back.get(q);if(q==null)return null;}path.add(start);Collections.reverse(path);
            var walk=new ArrayList<BlockPos>();
            if(subtype==FamilyOuting.Subtype.WOODLAND_WALK){walk.addAll(woodlandLoop(l,r,s,c));if(walk.size()<6)continue;}
            return new Site(c,List.copyOf(path),List.copyOf(walk),water);
        }return null;
    }
    static boolean shoreClear(ServerLevel l,RealmData.Realm r,VisitData.Scene s,BlockPos c){for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){var q=c.offset(x,0,z);if(!copied(r,q)||!feet(l,q)||!natural(l,q)||SecretDialogue.secretArea(s.loop.secrets,q))return false;}return true;}
    /** A real, fully copied lake: every hull-sized sample and connecting segment is validated. */
    static List<Vec3> waterRoute(ServerLevel l,RealmData.Realm r,BlockPos shore){
        for(int dx=-24;dx<=24;dx+=4)for(int dz=-24;dz<=24;dz+=4){var center=shore.offset(dx,0,dz);var route=new ArrayList<Vec3>();boolean safe=true;
            for(int i=0;i<96;i++){double a=i*Math.PI*2/96;var v=new Vec3(center.getX()+Math.cos(a)*9+.5,shore.getY()-.15,center.getZ()+Math.sin(a)*7+.5);if(!waterSafe(l,r,v)){safe=false;break;}route.add(v);}
            if(!safe)continue;int closest=0;for(int i=1;i<route.size();i++)if(route.get(i).distanceToSqr(shore.getCenter())<route.get(closest).distanceToSqr(shore.getCenter()))closest=i;if(route.get(closest).distanceToSqr(shore.getCenter())>100)continue;Collections.rotate(route,-closest);return List.copyOf(route);
        }return List.of();
    }
    static boolean waterSafe(ServerLevel l,RealmData.Realm r,Vec3 at){for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var p=BlockPos.containing(at).offset(x,0,z);if(!copied(r,p)||!l.getFluidState(p).is(FluidTags.WATER)||!l.getFluidState(p.below()).is(FluidTags.WATER)||!l.isEmptyBlock(p.above())||!l.isEmptyBlock(p.above(2)))return false;}return true;}
    /** Local reachable rest points form a winding loop, rather than retracing the house approach. */
    static List<BlockPos> woodlandLoop(ServerLevel l,RealmData.Realm r,VisitData.Scene s,BlockPos center){
        var reached=new HashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();reached.add(center);queue.add(center);
        while(!queue.isEmpty()&&reached.size()<6500){var at=queue.remove();for(var direction:Direction.Plane.HORIZONTAL)for(int dy:new int[]{0,1,-1}){
            var next=at.relative(direction).above(dy);if(reached.contains(next)||Math.hypot(next.getX()-center.getX(),next.getZ()-center.getZ())>35||!copied(r,next)||!feet(l,next)||!natural(l,next)||SecretDialogue.secretArea(s.loop.secrets,next))continue;
            if(dy==1&&!l.noCollision(new AABB(at.getX()+.18,at.getY()+1,at.getZ()+.18,at.getX()+.82,at.getY()+2.9,at.getZ()+.82)))continue;
            reached.add(next);queue.add(next);break;
        }}
        var stops=new ArrayList<BlockPos>();stops.add(center);double rotation=Math.floorMod(s.loop.event.getLong("OutingSeed"),360)*Math.PI/180;
        for(int i=0;i<6;i++){double angle=rotation+i*Math.PI/3;var desired=new Vec3(center.getX()+Math.cos(angle)*(i%2==0?22:16),center.getY(),center.getZ()+Math.sin(angle)*(i%2==0?22:16));var nearest=reached.stream().filter(q->!stops.contains(q)).min(Comparator.comparingDouble(q->q.getCenter().distanceToSqr(desired))).orElse(null);if(nearest==null||nearest.getCenter().distanceToSqr(desired)>36)return List.of();stops.add(nearest);}
        stops.add(center);return List.copyOf(stops);
    }
    private OutingSites(){}
}
