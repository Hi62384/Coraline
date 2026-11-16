package com.coraline.feast;

import com.coraline.secrets.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.*;
import java.util.*;

/** Reversible bedroom furniture puzzle: no inventory items or house blocks are consumed. */
final class BedroomBarricade {
    private static final int PIECES=3,LEGACY_PUSHES=4;
    static final double PUSH_DISTANCE=.5;
    private static CompoundTag data(VisitData.Scene s){return BlackIsTraditional.data(s).getCompound("Barricade");}
    static boolean done(VisitData.Scene s){return data(s).getInt("Completed")>=PIECES;}
    private static boolean working(VisitData.Scene s){return BlackIsTraditional.active(s)&&(BlackIsTraditional.phase(s)==BlackIsTraditional.Phase.M||BlackIsTraditional.phase(s)==BlackIsTraditional.Phase.GO_TO_BED);}
    static void tick(ServerPlayer p,VisitData.Scene s){
        if(!working(s))return;var realm=SecretWorld.realm(p);if(realm==null||realm.house==null)return;
        var room=PartyRegions.rooms().stream().filter(r->r.id().equals("child_bedroom")).findFirst().orElseThrow();
        if(!room.bounds(realm.house).contains(p.position()))return;
        if(!BlackIsTraditional.data(s).contains("Barricade"))BlackIsTraditional.data(s).put("Barricade",new CompoundTag());var n=data(s);
        if(!n.contains("Door")){
            BlockPos best=null;double distance=Double.MAX_VALUE;var bed=BlockPos.of(BlackIsTraditional.data(s).getLong("Bed"));
            for(int x=room.min()[0]-1;x<=room.max()[0]+1;x++)for(int z=room.min()[2]-1;z<=room.max()[2]+1;z++){
                if(x>room.min()[0]&&x<room.max()[0]&&z>room.min()[2]&&z<room.max()[2])continue;
                var at=HouseCabinets.at(realm.house,x,room.floor()+1,z);var state=p.serverLevel().getBlockState(at);
                if(state.getBlock() instanceof DoorBlock&&state.getValue(DoorBlock.HALF)==DoubleBlockHalf.LOWER&&at.distSqr(bed)<distance){best=at;distance=at.distSqr(bed);}
            }
            if(best==null){n.putString("Blocker","Bedroom door could not be located. Use /coraline recover to retry setup.");hint(p,n.getString("Blocker"));return;}
            n.putLong("Door",best.asLong());var inward=Vec3.atBottomCenterOf(bed).subtract(Vec3.atBottomCenterOf(best)).multiply(1,0,1);
            n.putString("Inward",Math.abs(inward.x)>Math.abs(inward.z)?inward.x>0?"east":"west":inward.z>0?"south":"north");
        }
        if(!n.contains("LayoutSeed"))n.putLong("LayoutSeed",p.getRandom().nextLong());
        // Old unstarted puzzles were filled in scan order, so their three chairs
        // all occupied one corner. Migrate only untouched layouts; never erase a
        // player's pushes or reroll furniture on each tick/reload.
        if(n.getInt("LayoutVersion")<2&&java.util.stream.IntStream.range(0,PIECES).allMatch(i->n.getInt("Push"+i)==0)){
            clear(p,s);for(int i=0;i<PIECES;i++)n.remove("Origin"+i);n.putInt("LayoutVersion",2);
        }
        if(java.util.stream.IntStream.range(0,PIECES).anyMatch(i->!n.contains("Origin"+i))){
            var choices=new LinkedHashMap<BlockPos,Vec3>();
            for(int x=room.min()[0]+1;x<room.max()[0];x++)for(int z=room.min()[2]+1;z<room.max()[2];z++){
                var at=HouseCabinets.at(realm.house,x,room.floor()+1,z);var feet=PartyFloor.at(p,at);
                if(feet==null)continue;var box=new AABB(feet,feet).inflate(.5,0,.5).expandTowards(0,2.1,0);
                if(!p.serverLevel().getBlockCollisions(null,box).iterator().hasNext())choices.put(at,feet);
            }
            // Only the connected floor containing the player can supply pieces.
            var start=choices.keySet().stream().min(Comparator.comparingDouble(at->at.distSqr(p.blockPosition()))).orElse(null);
            var reached=new HashSet<BlockPos>();var pending=new ArrayDeque<BlockPos>();if(start!=null){pending.add(start);reached.add(start);}
            while(!pending.isEmpty()){var at=pending.removeFirst();for(var dir:Direction.Plane.HORIZONTAL){var next=at.relative(dir);if(choices.containsKey(next)&&reached.add(next))pending.addLast(next);}}
            var door=Vec3.atBottomCenterOf(BlockPos.of(n.getLong("Door")));var candidates=new ArrayList<Vec3>();
            for(var at:reached){var feet=choices.get(at);if(feet.distanceToSqr(door)<6)continue;var box=new AABB(feet,feet).inflate(.6,0,.6).expandTowards(0,2.1,0);
                if(p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,box,e->!e.getPersistentData().getBoolean("BedroomBarricade")).isEmpty())candidates.add(feet);}
            // Sort before seeded shuffling: HashSet iteration must not change a
            // saved layout when the same house reloads on a different JVM.
            candidates.sort(Comparator.comparingDouble((Vec3 at)->at.x).thenComparingDouble(at->at.z));
            var selected=spread(candidates,n.getLong("LayoutSeed"));
            if(selected.size()<PIECES){n.putString("Blocker","Clear three separated, reachable bedroom floor areas for the barricade furniture.");hint(p,n.getString("Blocker"));return;}
            for(int i=0;i<PIECES;i++)if(!n.contains("Origin"+i)){var at=selected.get(i);n.putLong("Origin"+i,BlockPos.containing(at).asLong());n.putDouble("OriginY"+i,at.y);}
        }
        int completed=0;
        for(int i=0;i<PIECES;i++){
            double length=origin(n,i).distanceTo(target(n,i)),travel=travel(n,i);boolean placed=travel>=length-.000001;
            if(placed)completed++;
            var chair=find(p,s,i);if(chair==null){
                Vec3 origin=origin(n,i);
                chair=VisitEvents.prop(p,s,ScenePropEntity.CHAIR,origin,6100+i);chair.getPersistentData().putBoolean("BedroomBarricade",true);
                chair.getPersistentData().putInt("BarricadePiece",i);n.putLong("Origin"+i,BlockPos.containing(origin).asLong());
                chair.setCustomName(Component.literal("Push chair to the door — right-click"));chair.setCustomNameVisible(true);
            }
            var from=origin(n,i);var to=target(n,i);
            chair.setPos(length<.000001?to:from.lerp(to,Math.min(1,travel/length)));chair.setYRot(90*i);
            chair.getPersistentData().putBoolean("BarricadePlaced",placed);chair.setGlowingTag(!placed);
        }
        n.putInt("Completed",completed);
        if(completed>0&&!n.getBoolean("DoorClosed")){BlackIsTraditional.closeDoor(p,BlockPos.of(n.getLong("Door")));n.putBoolean("DoorClosed",true);}
        hint(p,done(s)?BlackIsTraditional.phase(s)==BlackIsTraditional.Phase.M?"Door secured. Wait for the music to finish, then sleep.":"Door secured. Right-click your bed to sleep.":"Barricade the bedroom — push the glowing chairs to the door ("+completed+"/3). Right-click to push.");
    }
    private static Vec3 origin(CompoundTag n,int i){var at=Vec3.atBottomCenterOf(BlockPos.of(n.getLong("Origin"+i)));return n.contains("OriginY"+i)?new Vec3(at.x,n.getDouble("OriginY"+i),at.z):at;}
    /** Preserve the exact physical progress of saves from the old four-push puzzle. */
    private static double travel(CompoundTag n,int i){
        double length=origin(n,i).distanceTo(target(n,i));String key="Travel"+i;
        if(!n.contains(key))n.putDouble(key,length*Math.max(0,Math.min(1,n.getInt("Push"+i)/(double)LEGACY_PUSHES)));
        return Math.max(0,Math.min(length,n.getDouble(key)));
    }
    static boolean advance(CompoundTag n,int i){
        if(i<0||i>=PIECES||!n.contains("Origin"+i)||!n.contains("Door"))return false;
        double before=travel(n,i),length=origin(n,i).distanceTo(target(n,i));if(before>=length-.000001)return false;
        n.putDouble("Travel"+i,Math.min(length,before+PUSH_DISTANCE));n.putInt("Push"+i,n.getInt("Push"+i)+1);return true;
    }
    /** Seeded farthest-point sampling gives different room-wide layouts, not
     * three adjacent cells selected by a nested x/z scan. */
    static List<Vec3> spread(List<Vec3> candidates,long seed){
        var remaining=new ArrayList<>(candidates);Collections.shuffle(remaining,new Random(seed));var chosen=new ArrayList<Vec3>();
        if(remaining.isEmpty())return chosen;chosen.add(remaining.remove(0));
        while(chosen.size()<PIECES&&!remaining.isEmpty()){
            var best=remaining.stream().max(Comparator.comparingDouble(at->chosen.stream().mapToDouble(at::distanceToSqr).min().orElse(0))).orElseThrow();
            if(chosen.stream().anyMatch(at->at.distanceToSqr(best)<9))break;chosen.add(best);remaining.remove(best);
        }
        return chosen;
    }
    private static Vec3 target(CompoundTag n,int i){var in=Direction.byName(n.getString("Inward"));if(in==null)in=Direction.EAST;var side=in.getClockWise();return Vec3.atBottomCenterOf(BlockPos.of(n.getLong("Door"))).add(Vec3.atLowerCornerOf(in.getNormal()).scale(.8+i*.45)).add(Vec3.atLowerCornerOf(side.getNormal()).scale((i-1)*.6));}
    private static ScenePropEntity find(ServerPlayer p,VisitData.Scene s,int i){return VisitEvents.props(p,s).stream().filter(e->e.getPersistentData().getBoolean("BedroomBarricade")&&e.getPersistentData().getInt("BarricadePiece")==i).findFirst().orElse(null);}
    static boolean interact(ServerPlayer p,VisitData.Scene s,ScenePropEntity prop){
        if(!prop.getPersistentData().getBoolean("BedroomBarricade"))return false;
        if(!working(s)||!p.getUUID().equals(prop.owner)||!Objects.equals(s.door,prop.storyDoor)||!PartyMatches.withinReach(p,prop))return true;
        var n=data(s);long now=p.serverLevel().getGameTime();if(now<n.getLong("NextPush"))return true;
        int piece=prop.getPersistentData().getInt("BarricadePiece");if(piece<0||piece>=PIECES)return true;
        if(!advance(n,piece))return true;n.putLong("NextPush",now+4);
        p.serverLevel().playSound(null,prop.blockPosition(),net.minecraft.sounds.SoundEvents.WOOD_PLACE,net.minecraft.sounds.SoundSource.BLOCKS,.5F,.65F);tick(p,s);OtherMotherLoop.changed(p);return true;
    }
    static boolean blockDoor(ServerPlayer p,VisitData.Scene s,BlockPos at){
        var n=data(s);if(!working(s)||!n.contains("Door")||n.getInt("Completed")==0)return false;
        var door=BlockPos.of(n.getLong("Door"));return at.equals(door)||at.equals(door.above());
    }
    static void clear(ServerPlayer p,VisitData.Scene s){for(var prop:VisitEvents.props(p,s))if(prop.getPersistentData().getBoolean("BedroomBarricade"))prop.discard();}
    static void retry(ServerPlayer p,VisitData.Scene s){clear(p,s);var n=data(s);for(String key:new HashSet<>(n.getAllKeys()))if(key.startsWith("Origin")||key.equals("Blocker"))n.remove(key);}
    private static void hint(ServerPlayer p,String text){if(p.tickCount%30==0)p.displayClientMessage(Component.literal(text),true);}
    private BedroomBarricade(){}
}
