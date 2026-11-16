package com.coraline.feast;

import java.util.*;
import com.coraline.entity.CharacterEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

/** A running hand-in-hand route. Both feet follow the same validated corridor;
 * the passenger never tries to navigate into the leader's collision box. */
final class FirstEscapeGuide {
    static final double SPEED=.29;
    static void clear(CompoundTag n){for(String key:List.of("GuideRoute","GuideNode","GuideGoal","GuideTrail","GuideStall","GuideHeldTicks"))n.remove(key);}
    static void release(ServerPlayer p,VisitData.Scene s,CharacterEntity w){
        var n=FirstEscapeStory.data(s);var grip=FirstEscapeScene.prop(p,s,"GuideGrip");
        if(grip==null&&p.getVehicle() instanceof CinematicPropEntity current&&current.owned(s)&&current.kind()==CinematicPropEntity.GRAB_CARRIER&&current.cue()==2)grip=current;
        if(grip!=null){var feet=grip.position();n.putBoolean("InternalTransfer",true);grip.ejectPassengers();n.remove("InternalTransfer");grip.discard();
            if(clearBody(p,feet))p.teleportTo(feet.x,feet.y,feet.z);}
        n.remove("GuideGrip");n.remove("GuideGripPosition");clear(n);p.setDeltaMovement(Vec3.ZERO);
        if(w!=null){w.setSprinting(false);w.setNoAi(false);w.setNoGravity(false);w.setDeltaMovement(Vec3.ZERO);w.getNavigation().stop();}
    }
    private static boolean clearBody(ServerPlayer p,Vec3 at){return !p.serverLevel().getBlockCollisions(null,p.getDimensions(Pose.STANDING).makeBoundingBox(at.add(0,.003,0))).iterator().hasNext()&&!p.serverLevel().containsAnyLiquid(p.getDimensions(Pose.STANDING).makeBoundingBox(at));}
    private static void blocked(ServerPlayer p,VisitData.Scene s,CharacterEntity w,String reason){
        var n=FirstEscapeStory.data(s);int stuck=n.getInt("GuideStall")+1;n.putInt("GuideStall",stuck);n.putString("Blocker",reason);
        if(stuck>=40){release(p,s,w);n.putLong("GuideRetryAt",OtherMotherLoop.now(p)+100);n.putString("Blocker",reason);FirstEscapeStory.hint(p,"Wybie let go: clear the path to the wardrobe, then approach him to retry.");}
    }
    static BlockPos goal(ServerPlayer p,VisitData.Scene s){
        var door=BlockPos.of(FirstEscapeStory.data(s).getLong("LittleDoor"));var desired=door.offset(2,0,2);
        for(int radius=0;radius<=3;radius++)for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            if(radius>0&&Math.abs(x)!=radius&&Math.abs(z)!=radius)continue;var cell=desired.offset(x,0,z);
            if(FirstEscapeScene.inRoom(s,cell)&&FirstEscapeCarry.fits(p,cell))return cell;
        }return null;
    }
    static boolean arrived(ServerPlayer p,VisitData.Scene s,CharacterEntity w){var n=FirstEscapeStory.data(s);return n.getBoolean("GuideArrived")&&FirstEscapeScene.inRoom(s,w.blockPosition())&&FirstEscapeScene.inRoom(s,p.blockPosition())&&p.distanceToSqr(w)<9;}
    static void tick(ServerPlayer p,VisitData.Scene s,CharacterEntity w){
        var n=FirstEscapeStory.data(s);long now=OtherMotherLoop.now(p);
        if(n.getLong("GuideRetryAt")>now){FirstEscapeStory.hint(p,"Clear the path, then take Wybie's hand again.");return;}
        // Migrate a mounted old-save softlock without retaining its stale route.
        if(n.getInt("GuideRevision")<2){release(p,s,w);n.putInt("GuideRevision",2);n.remove("GuideArrived");}
        var grip=FirstEscapeScene.prop(p,s,"GuideGrip");
        if(grip==null&&p.getVehicle() instanceof CinematicPropEntity current&&current.owned(s)&&current.kind()==CinematicPropEntity.GRAB_CARRIER&&current.cue()==2)grip=current;
        if(grip==null){if(p.distanceToSqr(w)>9){w.setNoAi(false);w.setNoGravity(false);w.setSprinting(true);w.getNavigation().moveTo(p,1.8);return;}
            grip=FirstEscapeScene.ensure(p,s,"GuideGrip",CinematicPropEntity.GRAB_CARRIER,0,p.serverLevel(),p.position());
            if(grip==null)return;grip.frame(2,0,1);if(!p.startRiding(grip,true)){grip.discard();return;}
        }else if(p.getVehicle()!=grip&&!p.startRiding(grip,true)){blocked(p,s,w,"Hand grip could not attach");return;}
        grip.frame(2,n.getInt("Ticks"),1);w.setNoAi(true);w.setNoGravity(true);w.noPhysics=false;w.setSprinting(true);w.getNavigation().stop();w.setDeltaMovement(Vec3.ZERO);w.gameGesture(45,5);
        int held=n.getInt("GuideHeldTicks")+1;n.putInt("GuideHeldTicks",held);
        if(held>600){release(p,s,w);n.putLong("GuideRetryAt",now+100);n.putString("Blocker","Running guide exceeded its safe hold window; clear the path and retry.");FirstEscapeStory.hint(p,"Wybie let go. Clear the path to the wardrobe, then approach him to retry.");return;}
        var goal=goal(p,s);if(goal==null){blocked(p,s,w,"No safe space by the cockroach wardrobe");return;}
        if(!n.contains("GuideRoute")||n.getLong("GuideGoal")!=goal.asLong()){
            var route=FirstEscapeCarry.route(p,w.blockPosition(),goal);var list=new ListTag();
            // Centre the initial cell before taking its first corner.
            if(FirstEscapeCarry.fits(p,w.blockPosition()))list.add(LongTag.valueOf(w.blockPosition().asLong()));
            for(var cell:route)list.add(LongTag.valueOf(cell.asLong()));
            if(route.isEmpty()&&!w.blockPosition().equals(goal)){blocked(p,s,w,"The hallway/room path is obstructed");return;}
            n.put("GuideRoute",list);n.putInt("GuideNode",0);n.putLong("GuideGoal",goal.asLong());
        }
        var route=n.getList("GuideRoute",4);int index=n.getInt("GuideNode");
        Vec3 target=index<route.size()?FirstEscapeCarry.feet(p,BlockPos.of(((LongTag)route.get(index)).getAsLong())):w.position();
        if(index<route.size()){
            var node=BlockPos.containing(target);LoopProps.hold(p.serverLevel(),node);LoopProps.hold(p.serverLevel(),w.blockPosition());
            if(w.isAddedToWorld()&&!p.serverLevel().areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(node).toLong())){blocked(p,s,w,"Waiting for hallway entity chunks");return;}
            var delta=target.subtract(w.position());var horizontal=delta.multiply(1,0,1);double length=horizontal.length();
            var at=w.position().add(length>SPEED?horizontal.scale(SPEED/length):horizontal);
            var floorShapes=new ArrayList<CarryStepGeometry.FloorBox>();
            // A diagonal interpolation into a 1/16-block rug clips its edge.
            // Lift the complete footprint before crossing, and lower it only
            // once both feet have cleared the covering (same rule as the carry).
            for(double dx:new double[]{-.2999,.2999})for(double dz:new double[]{-.2999,.2999}){
                var cell=BlockPos.containing(at.x+dx,goal.getY(),at.z+dz);
                for(var local:p.serverLevel().getBlockState(cell).getCollisionShape(p.serverLevel(),cell).toAabbs()){
                    var box=local.move(cell);floorShapes.add(new CarryStepGeometry.FloorBox(box.minX,box.minY,box.minZ,box.maxX,box.maxY,box.maxZ));
                }
            }
            var grounded=CarryStepGeometry.floor(new CarryStepGeometry.Point(at.x,at.y,at.z),goal.getY(),floorShapes);
            if(grounded==null){n.remove("GuideRoute");blocked(p,s,w,"A raised obstacle blocks the running path");return;}
            var next=new Vec3(grounded.x(),grounded.y(),grounded.z());var step=next.subtract(w.position());
            if(!clearBody(p,next)){n.remove("GuideRoute");blocked(p,s,w,"A block obstructs the running path");return;}
            if(step.horizontalDistanceSqr()>.0001){float yaw=(float)Math.toDegrees(Math.atan2(-step.x,step.z));w.setYRot(yaw);w.setYBodyRot(yaw);w.setYHeadRot(yaw);}
            w.setPos(next);w.setOnGround(true);if(next.distanceToSqr(target)<.0001)n.putInt("GuideNode",index+1);
        }
        // Keep a short physical breadcrumb trail, so the held player rounds the
        // door frame instead of taking a diagonal shortcut through the wall.
        var trail=n.getList("GuideTrail",10);var point=new CompoundTag();point.putDouble("X",w.getX());point.putDouble("Y",w.getY());point.putDouble("Z",w.getZ());trail.add(point);
        while(trail.size()>12)trail.remove(0);n.put("GuideTrail",trail);
        Vec3 hand=null;double travelled=0;Vec3 previous=w.position();
        for(int i=trail.size()-1;i>=0;i--){var v=trail.getCompound(i);var at=new Vec3(v.getDouble("X"),v.getDouble("Y"),v.getDouble("Z"));travelled+=at.distanceTo(previous);previous=at;if(travelled>=.85){hand=at;break;}}
        if(hand!=null&&clearBody(p,hand)){grip.setPos(hand);n.putLong("GuideGripPosition",grip.blockPosition().asLong());}
        if(index>=route.size()){
            // Bring the trailing hand inside the room before releasing/advancing.
            if(hand==null){var behind=target.add(.8,0,0);if(clearBody(p,behind))grip.setPos(behind);}
            if(FirstEscapeScene.inRoom(s,grip.blockPosition())&&FirstEscapeScene.inRoom(s,w.blockPosition())){n.putBoolean("GuideArrived",true);return;}
            blocked(p,s,w,"Trailing hand has not cleared the doorway");
        }else n.putInt("GuideStall",0);
    }
    private FirstEscapeGuide(){}
}
