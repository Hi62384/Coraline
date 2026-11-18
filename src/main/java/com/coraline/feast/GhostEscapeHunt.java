package com.coraline.feast;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import com.coraline.block.TinyDoorBlock;
import com.coraline.otherworld.TunnelPassage;
import com.coraline.registry.ModBlocks;

/** Routes the agreed wager through real, authored cover points. This adapter
 * deliberately refuses to claim a hide, crossing or victory from a timer. */
public final class GhostEscapeHunt {
    public static boolean start(ServerPlayer p,VisitData.Scene s,CompoundTag ghost){
        var e=ghost.getCompound(GhostEscapeProgress.KEY);
        if(!GhostVictimMirrors.owned(p,s,ghost)||!GhostEscapeProgress.owns(ghost,e)||GhostEscapeProgress.phase(e)!=GhostEscapeProgress.Phase.WAGER
            ||!e.getBoolean("WagerAgreed")||!e.getBoolean("PlayerEyesHeld"))return false;
        var child=GhostVictimActors.find(p,s,ghost);String hide=e.getString("InitialHide");if(child==null||!GhostEscapeHiding.reachable(child,e,hide))return false;
        e.remove("HuntRetryAt");e.remove("Blocker");e.putString("TargetHide",hide);e.putBoolean("InitialHideStaging",true);
        ghost.remove("ChildRoutine");
        child.setInvisible(false);child.setNoAi(false);child.noPhysics=false;child.setNoGravity(false);
        OtherMotherLoop.changed(p);return true;
    }
    public static boolean tick(ServerPlayer p,VisitData.Scene s,CompoundTag ghost){
        var e=ghost.getCompound(GhostEscapeProgress.KEY);
        if(!GhostVictimMirrors.owned(p,s,ghost)||!GhostEscapeProgress.owns(ghost,e))return false;
        var phase=GhostEscapeProgress.phase(e);
        if(phase==GhostEscapeProgress.Phase.MOVING_HIDE&&e.getBoolean("WagerAgreed")&&!e.contains("HidingSpot")
            &&!e.getString("InitialHide").isBlank()&&e.getString("InitialHide").equals(e.getString("TargetHide"))){
            // Prepared saves from the old opening have not reached any cover.
            // Resume their same destination as staging, without a new draw.
            GhostEscapeProgress.move(e,GhostEscapeProgress.Phase.WAGER,GhostVictimProgress.clock(ghost));
            e.putBoolean("InitialHideStaging",true);phase=GhostEscapeProgress.Phase.WAGER;
        }
        if(phase==GhostEscapeProgress.Phase.WAGER){
            long clock=GhostVictimProgress.clock(ghost);
            if(!e.getBoolean("InitialHideStaging")&&clock>=e.getLong("HuntRetryAt")){
                e.putLong("HuntRetryAt",clock+20);
                if(!start(p,s,ghost))e.putString("Blocker","Clear the child's physical route to the initial hiding place; the accepted wager will resume.");
            }
            var stagingChild=GhostVictimActors.find(p,s,ghost);String hide=e.getString("InitialHide");
            if(e.getBoolean("InitialHideStaging")&&stagingChild!=null&&stagingChild.level()==AcceptedButtonEyesWorld.level(p)){
                var node=GhostEscapeHiding.node(e,hide);var stagingMother=AcceptedButtonEyesWorld.mother(p,s);
                if(node.contains("Position")&&!node.getBoolean("Compromised")){
                    stagingChild.setPose(Pose.CROUCHING);stagingChild.refreshDimensions();
                    if(GhostVictimRoutines.travel(p,s,ghost,stagingChild,BlockPos.of(node.getLong("Position")),false)==GhostVictimNavigation.Result.ARRIVED
                        &&GhostEscapeHiding.concealed(stagingChild,stagingMother,e,hide)&&GhostEscapeProgress.hidingArrival(ghost,hide,true)){
                        e.remove("InitialHideStaging");e.remove("Blocker");ghost.remove("ChildRoutine");stagingChild.getNavigation().stop();
                        GhostVictimDirector.words(ghost,"I'm hidden. Tell me when to move.");OtherMotherLoop.changed(p);
                    }
                }else{e.remove("InitialHideStaging");e.putString("Blocker","The original starting cover has changed; restore its clearance before the hunt can begin.");}
            }
            return true;
        }
        if(!java.util.Set.of(GhostEscapeProgress.Phase.MOVING_HIDE,GhostEscapeProgress.Phase.HIDDEN,
            GhostEscapeProgress.Phase.FINAL_CROSSING,GhostEscapeProgress.Phase.OPEN_DOOR,GhostEscapeProgress.Phase.CRAWL).contains(phase))return false;
        var child=GhostVictimActors.find(p,s,ghost);if(child==null)return true;
        var mother=AcceptedButtonEyesWorld.mother(p,s);String target=e.getString("TargetHide");
        if(phase==GhostEscapeProgress.Phase.FINAL_CROSSING){
            child.setPose(Pose.STANDING);child.refreshDimensions();
            var realm=AcceptedButtonEyesWorld.realm(p,s);if(realm==null||realm.doorPos==null||child.level()!=p.server.getLevel(com.coraline.otherworld.OtherWorld.KEY))return true;
            var approach=GhostVictimNavigation.approach(child,realm.doorPos);if(approach==null){e.putString("Blocker","No clear path reaches the Little Door.");return true;}
            if(GhostVictimRoutines.travel(p,s,ghost,child,approach,true)==GhostVictimNavigation.Result.ARRIVED&&GhostEscapeProgress.reachedDoor(ghost,true)){
                ghost.remove("ChildRoutine");
                child.getNavigation().stop();
                OtherMotherLoop.changed(p);
            }
            return true;
        }
        if(phase==GhostEscapeProgress.Phase.OPEN_DOOR){
            var realm=AcceptedButtonEyesWorld.realm(p,s);
            var other=AcceptedButtonEyesWorld.level(p);
            if(realm==null||realm.doorPos==null||other==null||child.level()!=other)return true;
            if(openPassage(other,realm.doorPos,s.door)==null){
                e.putString("Blocker","The original Little Door passage is blocked. The child is waiting at the entrance.");
                return true;
            }
            if(GhostEscapeProgress.openedDoor(ghost,true)){
                child.gesture(6,5);
                child.setPose(Pose.SWIMMING);
                child.refreshDimensions();
                e.remove("Blocker");
                ghost.remove("ChildRoutine");
                OtherMotherLoop.changed(p);
            }
            return true;
        }
        if(phase==GhostEscapeProgress.Phase.CRAWL){
            // Entity pose is not a durable story receipt. Reapply it after a
            // reload on either side before asking the real pathfinder to move.
            child.setPose(Pose.SWIMMING);child.refreshDimensions();child.gesture(6,5);
            var realm=AcceptedButtonEyesWorld.realm(p,s);if(realm==null||realm.sourceDoor==null||realm.doorPos==null)return true;
            var source=com.coraline.otherworld.DoorData.get(p.server).doors.get(s.door);
            if(source==null||!s.owner.equals(source.owner)||!realm.sourceDoor.equals(source.pos))return true;
            var outside=com.coraline.parents.FamilyData.level(p.server,source.dimension);if(outside==null)return true;
            LoopProps.hold(outside,realm.sourceDoor);
            var original=outside.getBlockState(realm.sourceDoor);if(!original.is(ModBlocks.TINY_DOOR.get()))return true;
            if(!(outside.getBlockEntity(realm.sourceDoor) instanceof com.coraline.otherworld.DoorBlockEntity sourceDoor)
                    ||!s.door.equals(sourceDoor.doorId))return true;
            boolean crossed=child.level()==outside;
            if(crossed){
                var owner=child.getPersistentData();
                if(!owner.hasUUID("ShowTravelOwner")||!s.owner.equals(owner.getUUID("ShowTravelOwner"))
                        ||!owner.contains("GhostEscapeChild"))return true;
                var room=realm.sourceDoor.relative(original.getValue(TinyDoorBlock.FACING),2);
                if(child.position().distanceToSqr(net.minecraft.world.phys.Vec3.atBottomCenterOf(room))>.35*.35){
                    if(openPassage(outside,realm.sourceDoor,s.door)==null){
                        e.putString("Blocker","The original Little Door passage is being restored.");
                        return true;
                    }
                    if(GhostVictimRoutines.crawl(p,s,ghost,child,room)!=GhostVictimNavigation.Result.ARRIVED)return true;
                }
                if(!outside.noCollision(child,child.getDimensions(Pose.STANDING).makeBoundingBox(child.position()))){
                    e.putString("Blocker","The real-room exit needs standing clearance before restoration.");
                    return true;
                }
                if(GhostEscapeProgress.resolveHunt(ghost,true,false)){
                    child.setPose(Pose.STANDING);child.refreshDimensions();child.gesture(0,0);GhostVictimActors.remember(ghost,child);
                    e.putLong("HomeLanding",room.asLong());
                    ghost.remove("ChildRoutine");
                    OtherMotherLoop.changed(p);
                    p.server.overworld().getDataStorage().save();
                }
                return true;
            }
            var other=p.server.getLevel(com.coraline.otherworld.OtherWorld.KEY);
            if(other==null||child.level()!=other)return true;
            var sourceOpen=openPassage(outside,realm.sourceDoor,s.door);
            var realmOpen=openPassage(other,realm.doorPos,s.door);
            if(sourceOpen==null||realmOpen==null){
                e.putString("Blocker","The original Little Door passage is being restored.");
                return true;
            }
            var sourceFacing=sourceOpen.getValue(TinyDoorBlock.FACING);
            var far=TunnelPassage.inside(realm.sourceDoor,sourceFacing,TunnelPassage.TRANSFER_DEPTH);
            var farFeet=BlockPos.containing(far);
            if(!GhostVictimNavigation.clear(outside,child,farFeet)
                    ||!GhostVictimNavigation.clear(outside,child,farFeet.relative(sourceFacing,2))){
                e.putString("Blocker","Clear a child-sized route beyond the original Little Door; the crossing remains safely pending.");
                return true;
            }
            var facing=realmOpen.getValue(TinyDoorBlock.FACING);
            var end=BlockPos.containing(TunnelPassage.inside(realm.doorPos,facing,TunnelPassage.TRANSFER_DEPTH));
            if(child.distanceToSqr(end.getCenter())>1.2*1.2){GhostVictimRoutines.crawl(p,s,ghost,child,end);return true;}
            var moved=ShowTravel.guest(child,p,s,outside,far,true,"GhostEscapeChild");if(moved instanceof com.coraline.tycoon.TycoonNpc same
                    &&same.level()==outside&&child.getUUID().equals(same.getUUID())){
                same.setPose(Pose.SWIMMING);same.refreshDimensions();GhostVictimActors.remember(ghost,same);ghost.remove("ChildRoutine");OtherMotherLoop.changed(p);
            }
            return true;
        }
        if(phase==GhostEscapeProgress.Phase.HIDDEN&&e.getBoolean("StopRequested")){
            var next=GhostEscapeHiding.node(e,target);
            if(!next.contains("Position")||next.getBoolean("Compromised")||!GhostEscapeHiding.reachable(child,e,target)){
                e.remove("StopRequested");e.putString("Blocker","The requested route is blocked; choose a reachable cover position or the Little Door.");return true;
            }
            var destination=BlockPos.of(next.getLong("Position"));
            if(GhostVictimRoutines.travel(p,s,ghost,child,destination,true)==GhostVictimNavigation.Result.ARRIVED&&GhostEscapeHiding.concealed(child,mother,e,target)){
                e.remove("StopRequested");e.putString("HidingSpot",target);e.remove("TargetHide");child.getNavigation().stop();ghost.remove("ChildRoutine");OtherMotherLoop.changed(p);
            }
            return true;
        }
        if(phase==GhostEscapeProgress.Phase.MOVING_HIDE){
            child.setPose(Pose.CROUCHING);child.refreshDimensions();
            if(e.getBoolean("StopRequested")){
                CompoundTag selected=null;double nearest=Double.MAX_VALUE;
                for(var raw:e.getList("HidingNodes",net.minecraft.nbt.Tag.TAG_COMPOUND)){
                    var candidate=(CompoundTag)raw;String id=candidate.getString("Id");
                    if(candidate.getBoolean("Compromised")||!GhostEscapeHiding.reachable(child,e,id))continue;
                    double distance=child.distanceToSqr(BlockPos.of(candidate.getLong("Position")).getCenter());
                    if(distance<nearest){nearest=distance;selected=candidate;}
                }
                if(selected==null){e.putString("Blocker","No reachable covered position is available for the hide signal.");child.getNavigation().stop();return true;}
                target=selected.getString("Id");e.putString("TargetHide",target);
            }
            var node=GhostEscapeHiding.node(e,target);if(!node.contains("Position")||node.getBoolean("Compromised")||!GhostEscapeHiding.reachable(child,e,target)){
                e.putString("Blocker","The selected hiding route changed. Choose another real hiding position.");child.getNavigation().stop();return true;
            }
            var at=BlockPos.of(node.getLong("Position"));
            if(GhostVictimRoutines.travel(p,s,ghost,child,at,false)==GhostVictimNavigation.Result.ARRIVED&&GhostEscapeHiding.concealed(child,mother,e,target)){
                GhostEscapeProgress.hidingArrival(ghost,target,true);e.remove("StopRequested");ghost.remove("ChildRoutine");child.getNavigation().stop();
                GhostVictimDirector.words(ghost,"I'm hidden. Tell me when to move.");OtherMotherLoop.changed(p);
            }
            return true;
        }
        child.getNavigation().stop();child.setSprinting(false);
        // Physical detection remains owned by GhostEscapePerception and the
        // Mother patrol. The player must signal to choose a verified next node.
        if(!GhostEscapeHiding.concealed(child,mother,e,e.getString("HidingSpot"))){
            for(var candidate:e.getList("HidingNodes",net.minecraft.nbt.Tag.TAG_COMPOUND)){
                var row=(CompoundTag)candidate;String id=row.getString("Id");
                if(!row.getBoolean("Compromised")&&GhostEscapeHiding.concealed(child,mother,e,id)){
                    e.putString("HidingSpot",id);e.putLong("HidingAt",GhostVictimProgress.clock(ghost));break;
                }
            }
        }
        return true;
    }
    private static net.minecraft.world.level.block.state.BlockState openPassage(net.minecraft.server.level.ServerLevel level,BlockPos at,java.util.UUID expected){
        LoopProps.hold(level,at);
        var state=level.getBlockState(at);
        if(!state.is(ModBlocks.TINY_DOOR.get())
                ||!(level.getBlockEntity(at) instanceof com.coraline.otherworld.DoorBlockEntity door)
                ||!expected.equals(door.doorId)||!TunnelPassage.ensure(level,door))return null;
        var open=state.setValue(TinyDoorBlock.OPEN,true).setValue(TinyDoorBlock.PORTAL,true);
        if(!open.equals(state))level.setBlock(at,open,3);
        return level.getBlockState(at).equals(open)?open:null;
    }
    private GhostEscapeHunt(){}
}
