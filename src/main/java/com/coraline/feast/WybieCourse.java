package com.coraline.feast;

import com.coraline.otherworld.OtherWorld;
import com.coraline.secrets.SecretWorld;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.*;
import java.util.*;

/** Small, terrain-validated circuits. Block edits are reversible and never erase garden features. */
public final class WybieCourse {
    public record Gate(Vec3 position,Vec3 normal,int sample,String challenge,int points,double width){}
    public record Course(List<Vec3> samples,List<Gate> gates){
        public Vec3 start(){return gates.get(0).position;}
        public double distanceSquared(Vec3 point){double best=Double.MAX_VALUE;for(var at:samples)best=Math.min(best,at.distanceToSqr(point));return best;}
    }
    private static final Map<UUID,Course> CACHE=new HashMap<>();
    public static Course get(CompoundTag n){
        if(!n.hasUUID("Session")||!n.contains("Course"))return null;
        return CACHE.computeIfAbsent(n.getUUID("Session"),key->{var data=n.getCompound("Course");var samples=new ArrayList<Vec3>();for(var t:data.getList("Samples",10)){var v=(CompoundTag)t;samples.add(read(v));}var gates=new ArrayList<Gate>();for(var t:data.getList("Gates",10)){var v=(CompoundTag)t;gates.add(new Gate(read(v),new Vec3(v.getDouble("NX"),0,v.getDouble("NZ")),v.getInt("Sample"),v.getString("Challenge"),v.getInt("Points"),v.getDouble("Width")));}return new Course(List.copyOf(samples),List.copyOf(gates));});
    }
    private static Vec3 read(CompoundTag n){return new Vec3(n.getDouble("X"),n.getDouble("Y"),n.getDouble("Z"));}
    private static CompoundTag write(Vec3 p){var n=new CompoundTag();n.putDouble("X",p.x);n.putDouble("Y",p.y);n.putDouble("Z",p.z);return n;}
    public static boolean generate(ServerPlayer p,VisitData.Scene scene,CompoundTag n){
        if(get(n)!=null)return true;restoreLoaded(p.serverLevel(),scene);if(scene.loop.story.contains("WybieCourseCleanup")){n.putString("CourseProblem","The previous course is waiting for its loaded chunks to be restored.");return false;}var realm=SecretWorld.realm(p);if(realm==null||realm.house==null)return false;
        // These bounds are the existing authored RosewoodGarden transform, not world coordinates.
        // Test several compact scenic loops; reject, rather than flatten, a obstructed garden.
        Random random=new Random(n.getLong("Seed"));
        int[][] centers={{-15,-85},{16,-104},{-43,-111},{-16,-130},{5,-69},{-63,-85}};
        for(int attempt=0;attempt<12;attempt++){
            int[] center=centers[Math.floorMod(attempt+(int)n.getLong("Seed"),centers.length)];double rx=attempt<6?19:11,rz=attempt<6?14:9;
            var points=new ArrayList<Vec3>();var gates=new ArrayList<Gate>();boolean valid=true;double rotation=random.nextDouble()*.4;
            for(int i=0;i<96;i++){
                double angle=i*Math.PI*2/96+rotation;
                // A smooth kidney-shaped circuit: sweeping curves, a weaving side and tight turns.
                double x=realm.house.getX()+center[0]+rx*Math.cos(angle)+(i>18&&i<44?Math.sin(angle*6)*1.3:0);
                double z=realm.house.getZ()+center[1]+rz*Math.sin(angle)+Math.cos(angle*2)*2.1;
                var feet=surface(p.serverLevel(),x,z,realm.house.getY());
                if(feet==null||!points.isEmpty()&&Math.abs(feet.y-points.get(points.size()-1).y)>.6){valid=false;break;}points.add(feet);
            }
            if(!valid||points.size()!=96||Math.abs(points.get(0).y-points.get(95).y)>.6)continue;
            String[] challenges={"start","curve","precision turn","slalom","bridge","gentle jump","curve","precision turn"};
            int[] values={100,100,100,150,150,125,100,100};
            for(int i=0;i<8;i++){int index=i*12;var at=points.get(index);var normal=points.get((index+1)%96).subtract(points.get((index+95)%96)).multiply(1,0,1).normalize();gates.add(new Gate(at,normal,index,challenges[i],values[i],i==4?1.0:1.6));}
            var course=new Course(List.copyOf(points),List.copyOf(gates));var tag=new CompoundTag();var ps=new ListTag();for(var at:points)ps.add(write(at));tag.put("Samples",ps);
            var gs=new ListTag();for(var g:gates){var v=write(g.position);v.putDouble("NX",g.normal.x);v.putDouble("NZ",g.normal.z);v.putInt("Sample",g.sample);v.putString("Challenge",g.challenge);v.putInt("Points",g.points);v.putDouble("Width",g.width);gs.add(v);}tag.put("Gates",gs);n.put("Course",tag);CACHE.put(n.getUUID("Session"),course);
            decorate(p,scene,n,course);return true;
        }
        n.putString("CourseProblem","No safe loop fits the loaded garden without removing its existing features.");return false;
    }
    public static Vec3 surface(ServerLevel level,double x,double z,int expectedY){
        int bx=(int)Math.floor(x),bz=(int)Math.floor(z);var column=new BlockPos(bx,expectedY,bz);if(!level.hasChunkAt(column))return null;
        // Search locally around the authored garden grade, so rooftops/trees never become tracks.
        for(int y=expectedY+5;y>=expectedY-7;y--){var at=new BlockPos(bx,y,bz);var below=level.getBlockState(at.below());if(!below.isFaceSturdy(level,at.below(),Direction.UP)||!below.getFluidState().isEmpty())continue;
            var box=new AABB(x-1.1,y,z-1.1,x+1.1,y+2.4,z+1.1);
            if(level.noCollision(box)&&!level.containsAnyLiquid(box)&&level.getWorldBorder().isWithinBounds(at))return new Vec3(x,y,z);
        }return null;
    }
    private static void decorate(ServerPlayer p,VisitData.Scene s,CompoundTag n,Course c){
        for(int i=0;i<c.samples.size();i+=3){var at=BlockPos.containing(c.samples.get(i)).below();var old=p.serverLevel().getBlockState(at);
            if(old.is(Blocks.GRASS_BLOCK)||old.is(Blocks.DIRT)||old.is(Blocks.COARSE_DIRT))place(p,n,at,Blocks.GRAVEL.defaultBlockState(),false);
        }
        for(int i=0;i<c.gates.size();i++){
            var g=c.gates.get(i);Vec3 side=new Vec3(-g.normal.z,0,g.normal.x);var color=i%2==0?Blocks.MAGENTA_WOOL:Blocks.CYAN_WOOL;
            for(int sign:new int[]{-1,1}){var at=BlockPos.containing(g.position.add(side.scale((i==0||i==3?2:2.7)*sign)));if(!p.serverLevel().getBlockState(at.below()).isFaceSturdy(p.serverLevel(),at.below(),Direction.UP))continue;
                place(p,n,at,Blocks.OAK_FENCE.defaultBlockState(),true);place(p,n,at.above(),color.defaultBlockState(),true);if(i==0||i==3)place(p,n,at.above(2),Blocks.OAK_FENCE.defaultBlockState(),true);
                var flower=at.offset((int)Math.signum(side.x*sign),0,(int)Math.signum(side.z*sign));if(p.serverLevel().getBlockState(flower.below()).is(Blocks.GRASS_BLOCK))place(p,n,flower,Blocks.ALLIUM.defaultBlockState(),true);
            }
            // Open overhead arches above selected gates, never blocks in the riding corridor.
            if(i==0||i==3)for(int cross=-2;cross<=2;cross++)place(p,n,BlockPos.containing(g.position.add(side.scale(cross))).above(3),Blocks.OAK_SLAB.defaultBlockState(),true);
        }
        for(int i:new int[]{26,30,34}){var at=c.samples.get(i);var forward=c.samples.get(i+1).subtract(c.samples.get(i-1)).normalize();var side=new Vec3(-forward.z,0,forward.x).scale(i%8==2?1.65:-1.65);var post=BlockPos.containing(at.add(side));if(p.serverLevel().getBlockState(post.below()).isFaceSturdy(p.serverLevel(),post.below(),Direction.UP))place(p,n,post,Blocks.OAK_FENCE.defaultBlockState(),true);}
        // A small raised bridge and gentle drop: no excavated water or destructive terrain edits.
        addDeck(p,n,c,44,48,"BridgeDeck");addDeck(p,n,c,56,59,"JumpDeck");
    }
    private static void addDeck(ServerPlayer p,CompoundTag n,Course c,int first,int last,String key){
        var positions=new LinkedHashSet<BlockPos>();double y=c.samples.get(first).y;
        for(int i=first;i<=last;i++){var at=c.samples.get(i);if(Math.abs(at.y-y)>.05)return;var direction=c.samples.get(i+1).subtract(at).normalize();var side=new Vec3(-direction.z,0,direction.x);
            for(int step=0;step<5;step++){var sample=at.add(c.samples.get(i+1).subtract(at).scale(step/5.0));positions.add(BlockPos.containing(sample));positions.add(BlockPos.containing(sample.add(side.scale(.55))));positions.add(BlockPos.containing(sample.add(side.scale(-.55))));}}
        for(var at:positions)if(!p.serverLevel().isEmptyBlock(at)||!p.serverLevel().isEmptyBlock(at.above())||!p.serverLevel().isEmptyBlock(at.above(2)))return;
        for(var at:positions)place(p,n,at,Blocks.OAK_SLAB.defaultBlockState(),true);n.putBoolean(key,true);
    }
    private static void place(ServerPlayer p,CompoundTag n,BlockPos at,BlockState replacement,boolean airOnly){
        var level=p.serverLevel();if(!level.hasChunkAt(at))return;var before=level.getBlockState(at);if(before.hasBlockEntity()||airOnly&&!before.isAir()||before.equals(replacement))return;
        var blocks=n.getCompound("CourseBlocks");String key=Long.toString(at.asLong());if(blocks.contains(key))return;var record=new CompoundTag();record.put("Before",NbtUtils.writeBlockState(before));record.put("After",NbtUtils.writeBlockState(replacement));blocks.put(key,record);n.put("CourseBlocks",blocks);level.setBlock(at,replacement,2|16);
    }
    public static void tickScore(ServerPlayer p,VisitData.Scene s,WybieBicycleEntity bike){
        var n=WybieAdventure.data(s);var c=get(n);if(c==null||bike==null||p.getVehicle()!=bike||!bike.ownedBy(p,s)){if(n.getBoolean("RunActive"))WybieAdventure.failRun(p,s,"Dismounted");return;}
        Vec3 here=bike.position(),before=new Vec3(n.getDouble("BikeX"),n.getDouble("BikeY"),n.getDouble("BikeZ"));
        boolean previous=n.getBoolean("HadBikePosition");n.putDouble("BikeX",here.x);n.putDouble("BikeY",here.y);n.putDouble("BikeZ",here.z);n.putBoolean("HadBikePosition",true);
        if(!previous)return;
        if(!n.getBoolean("RunActive")){
            var gate=c.gates.get(0);if(here.distanceToSqr(gate.position)<16&&crossed(before,here,gate)&&bike.speed()>.03){WybieAdventureLedger.startRun(n);n.putInt("PathProgress",0);n.putBoolean("RunMounted",true);}return;
        }
        if(here.distanceToSqr(before)>4||bike.hardCollisionAt==bike.tickCount||bike.isInWaterOrBubble()||n.getInt("SectionTicks")>600){WybieAdventure.failRun(p,s,"Section failed");return;}
        int progress=n.getInt("PathProgress"),nearest=progress;double closest=Double.MAX_VALUE;
        for(int delta=-2;delta<=6;delta++){int sample=Math.floorMod(progress+delta,c.samples.size());double d=c.samples.get(sample).distanceToSqr(here);if(d<closest){closest=d;nearest=sample;}}
        if(closest>9){int away=n.getInt("OffCourseTicks")+1;n.putInt("OffCourseTicks",away);if(away>25)WybieAdventure.failRun(p,s,"Left the course");return;}n.putInt("OffCourseTicks",0);
        int advance=Math.floorMod(nearest-progress,c.samples.size());if(advance<=6)n.putInt("PathProgress",nearest);
        if(n.getInt("Checkpoint")==4&&nearest>=44&&nearest<=48&&n.getBoolean("BridgeDeck")){
            var center=c.samples.get(nearest);double dx=here.x-center.x,dz=here.z-center.z;
            if(dx*dx+dz*dz>.85*.85||here.y<center.y+.35){WybieAdventure.failRun(p,s,"Left the narrow bridge");return;}
        }
        if(n.getInt("Checkpoint")==3&&nearest>=24&&nearest<=36){var center=c.samples.get(nearest);double dx=here.x-center.x,dz=here.z-center.z;if(dx*dx+dz*dz>1.15*1.15){WybieAdventure.failRun(p,s,"Missed the slalom");return;}}
        if(!bike.onGround())n.putInt("AirTicks",n.getInt("AirTicks")+1);else if(n.getInt("AirTicks")>=2)n.putBoolean("JumpLanded",true);
        int expected=n.getInt("Checkpoint");var gate=c.gates.get(expected);
        if(crossed(before,here,gate)&&Math.abs(here.y-gate.position.y)<1.3){
            int delta=Math.floorMod(gate.sample-nearest,c.samples.size());if(delta>3&&delta<c.samples.size()-3){WybieAdventure.failRun(p,s,"Skipped course section");return;}
            if(expected==5&&n.getBoolean("JumpDeck")&&!n.getBoolean("JumpLanded")){n.putBoolean("AwaitJumpLanding",true);return;}
            award(p,s,c,expected);
        }else if(n.getBoolean("AwaitJumpLanding")&&bike.onGround()){if(n.getBoolean("JumpLanded"))award(p,s,c,expected);else WybieAdventure.failRun(p,s,"Jump landing missed");}
        // Crossing a future gate never bypasses checkpoint order, even after a teleport.
        for(int i=0;i<c.gates.size();i++)if(i!=expected&&i!=Math.floorMod(expected-1,c.gates.size())&&crossed(before,here,c.gates.get(i))){WybieAdventure.failRun(p,s,"Wrong checkpoint order");break;}
    }
    private static void award(ServerPlayer p,VisitData.Scene s,Course c,int index){var n=WybieAdventure.data(s);WybieAdventureLedger.checkpoint(n,index,c.gates.size(),c.gates.get(index).points);n.remove("AwaitJumpLanding");if(index==0||index==4||index==5){var w=WybieOptionalVisits.actor(p,n.getCompound("Companion"));if(w!=null)w.gameGesture(7,35);}}
    private static boolean crossed(Vec3 before,Vec3 after,Gate g){return WybieBicyclePhysics.forwardCrossing(before.x,before.z,after.x,after.z,g.position.x,g.position.z,g.normal.x,g.normal.z,g.width);}
    public static void cleanup(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        if(n.hasUUID("Session"))CACHE.remove(n.getUUID("Session"));var pending=s.loop.story.getCompound("WybieCourseCleanup");
        var blocks=n.getCompound("CourseBlocks");for(String key:blocks.getAllKeys())pending.put(key,blocks.getCompound(key).copy());s.loop.story.put("WybieCourseCleanup",pending);n.remove("CourseBlocks");n.remove("Course");restoreLoaded(p.serverLevel(),s);
    }
    public static void restoreLoaded(ServerLevel level,VisitData.Scene s){
        if(!OtherWorld.isOther(level)||!s.loop.story.contains("WybieCourseCleanup"))return;var pending=s.loop.story.getCompound("WybieCourseCleanup");var lookup=level.holderLookup(Registries.BLOCK);
        for(String key:new ArrayList<>(pending.getAllKeys())){BlockPos at;try{at=BlockPos.of(Long.parseLong(key));}catch(NumberFormatException e){pending.remove(key);continue;}if(!level.hasChunkAt(at))continue;
            var pair=pending.getCompound(key);if(level.getBlockState(at).equals(NbtUtils.readBlockState(lookup,pair.getCompound("After"))))level.setBlock(at,NbtUtils.readBlockState(lookup,pair.getCompound("Before")),2|16);pending.remove(key);
        }
        if(pending.isEmpty())s.loop.story.remove("WybieCourseCleanup");VisitData.get(level.getServer()).setDirty();
    }
    public static void clearCache(){CACHE.clear();}
    private WybieCourse(){}
}
