package com.coraline.feast;

import java.util.*;
import com.coraline.secrets.HouseCabinets;
import com.coraline.tycoon.TycoonNpc;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** Owner-bound investigations use authored Rosewood storage. Legacy prepared
 * chests remain valid for their original attempts. */
public final class GhostEscapeRooms {
    private static CompoundTag sites(CompoundTag ghost){if(!ghost.contains("EscapeRoomSites"))ghost.put("EscapeRoomSites",new CompoundTag());return ghost.getCompound("EscapeRoomSites");}
    private static HouseCabinets.Room room(String id){for(var room:HouseCabinets.ROOMS)if(room.id().equals(id))return room;return null;}
    public static List<String> prepare(ServerPlayer p,VisitData.Scene s,CompoundTag ghost,TycoonNpc child){
        var valid=new ArrayList<String>();if(!GhostVictimMirrors.owned(p,s,ghost)||!GhostVictimActors.owns(child,s,ghost))return valid;
        var level=AcceptedButtonEyesWorld.level(p);if(level==null||child.level()!=level)return valid;
        for(var room:HouseCabinets.ROOMS){
            var all=sites(ghost);var row=all.getCompound(room.id());var d=room.doors()[0];var door=GhostVictimWorld.at(s,d[0],d[1],d[2]);level.getChunkAt(door);
            var state=level.getBlockState(door);if(!(state.getBlock() instanceof DoorBlock))continue;
            if(!row.contains("Container")){
                for(int i=0;i<HouseCabinets.CABINETS.length;i++){
                    var cabinet=HouseCabinets.CABINETS[i];
                    if(!room.id().equals(cabinet.room()))continue;
                    var at=GhostVictimWorld.at(s,cabinet.x(),cabinet.y(),cabinet.z());
                    level.getChunkAt(at);
                    Direction front=null;
                    for(var direction:Direction.Plane.HORIZONTAL){
                        var step=at.relative(direction);
                        if(HouseCabinets.contains(AcceptedButtonEyesWorld.house(s),room,step)
                                &&GhostVictimNavigation.clear(level,child,step)
                                &&(front==null||step.distSqr(door)<at.relative(front).distSqr(door)))front=direction;
                    }
                    if(front==null)continue;
                    var storage=level.getBlockState(at);
                    if(storage.isAir()&&level.getBlockState(at.below()).isFaceSturdy(level,at.below(),Direction.UP)){
                        FirstEscapeScene.place(p,s,at,HouseCabinets.storageBlock(i).defaultBlockState()
                                .setValue(com.coraline.secrets.HouseStorageBlock.FACING,front));
                        storage=level.getBlockState(at);
                    }
                    if(!(storage.getBlock() instanceof com.coraline.secrets.HouseStorageBlock))continue;
                    row.putBoolean("AuthoredCabinet",true);
                    row.putString("Label",cabinet.name());
                    row.putLong("Container",at.asLong());
                    row.putLong("Door",door.asLong());
                    all.put(room.id(),row);
                    OtherMotherLoop.changed(p);
                    break;
                }
            }
            if(validContainer(p,s,ghost,room.id()))valid.add(room.id());
        }
        return List.copyOf(valid);
    }
    public static BlockPos door(VisitData.Scene s,String id){var room=room(id);if(room==null)return null;var d=room.doors()[0];return GhostVictimWorld.at(s,d[0],d[1],d[2]);}
    public static BlockPos outside(VisitData.Scene s,String id,ServerLevel level){
        var at=door(s,id);if(at==null)return null;var state=level.getBlockState(at);if(!(state.getBlock() instanceof DoorBlock))return null;
        var room=room(id);var front=at.relative(state.getValue(DoorBlock.FACING));var back=at.relative(state.getValue(DoorBlock.FACING).getOpposite());
        return HouseCabinets.contains(AcceptedButtonEyesWorld.house(s),room,front)?back:front;
    }
    public static BlockPos inside(VisitData.Scene s,String id,ServerLevel level){
        var at=door(s,id);var outside=outside(s,id,level);if(at==null||outside==null)return null;return at.offset(at.getX()-outside.getX(),0,at.getZ()-outside.getZ());
    }
    /** Find an actual reachable exit for the child, including a second authored
     * doorway if the originally selected one was changed during the search. */
    public static BlockPos reachableExit(VisitData.Scene s,String id,ServerLevel level,TycoonNpc child){
        var room=room(id);if(room==null||level==null||child.level()!=level)return null;
        for(int[] offset:room.doors()){
            var at=GhostVictimWorld.at(s,offset[0],offset[1],offset[2]);var state=level.getBlockState(at);
            if(!(state.getBlock() instanceof DoorBlock))continue;
            var front=at.relative(state.getValue(DoorBlock.FACING));var back=at.relative(state.getValue(DoorBlock.FACING).getOpposite());
            var target=HouseCabinets.contains(AcceptedButtonEyesWorld.house(s),room,front)?back:front;
            if(!GhostVictimNavigation.clear(level,child,target))continue;
            var path=child.getNavigation().createPath(target,0);if(path!=null&&path.canReach())return at;
        }
        return null;
    }
    public static BlockPos outside(VisitData.Scene s,String id,BlockPos door,ServerLevel level){
        var room=room(id);if(room==null||door==null)return null;var state=level.getBlockState(door);
        if(!(state.getBlock() instanceof DoorBlock))return null;
        var front=door.relative(state.getValue(DoorBlock.FACING));var back=door.relative(state.getValue(DoorBlock.FACING).getOpposite());
        return HouseCabinets.contains(AcceptedButtonEyesWorld.house(s),room,front)?back:front;
    }
    public static BlockPos container(CompoundTag ghost,String id){var row=sites(ghost).getCompound(id);return row.contains("Container")?BlockPos.of(row.getLong("Container")):null;}
    static BlockPos approachContainer(TycoonNpc child,BlockPos at){
        var level=(ServerLevel)child.level();
        var state=level.getBlockState(at);
        if(state.getBlock() instanceof com.coraline.secrets.HouseStorageBlock){
            var front=at.relative(state.getValue(com.coraline.secrets.HouseStorageBlock.FACING));
            if(GhostVictimNavigation.clear(level,child,front))return front;
        }
        return GhostVictimNavigation.approach(child,at);
    }
    public static boolean validContainer(ServerPlayer p,VisitData.Scene s,CompoundTag ghost,String id){
        if(!GhostVictimMirrors.owned(p,s,ghost))return false;var at=container(ghost,id);var level=AcceptedButtonEyesWorld.level(p);if(at==null||level==null)return false;level.getChunkAt(at);
        var row=sites(ghost).getCompound(id);
        if(row.getBoolean("AuthoredCabinet")){
            if(!(level.getBlockState(at).getBlock() instanceof com.coraline.secrets.HouseStorageBlock))return false;
            for(var cabinet:HouseCabinets.CABINETS)
                if(id.equals(cabinet.room())&&at.equals(GhostVictimWorld.at(s,cabinet.x(),cabinet.y(),cabinet.z())))return true;
            return false;
        }
        if(!(level.getBlockEntity(at) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest)||!level.getBlockState(at).is(Blocks.CHEST))return false;
        var marker=chest.getPersistentData();return marker.hasUUID("GhostEscapeOwner")&&s.owner.equals(marker.getUUID("GhostEscapeOwner"))&&marker.hasUUID("GhostEscapeDoor")&&s.door.equals(marker.getUUID("GhostEscapeDoor"))
            &&marker.hasUUID("GhostEscapeSession")&&ghost.getUUID("Session").equals(marker.getUUID("GhostEscapeSession"));
    }
    public static boolean openPickedDoor(ServerPlayer p,VisitData.Scene s,CompoundTag ghost,TycoonNpc child){
        var escape=ghost.getCompound(GhostEscapeProgress.KEY);String id=escape.getString("AssignedRoom");var room=room(id);
        if(room==null||!GhostVictimMirrors.owned(p,s,ghost)||!GhostVictimActors.owns(child,s,ghost)||!GhostEscapeProgress.owns(ghost,escape)
            ||GhostEscapeProgress.phase(escape)!=GhostEscapeProgress.Phase.LOCKPICK||!(child.level() instanceof ServerLevel level)||level!=AcceptedButtonEyesWorld.level(p))return false;
        var at=door(s,id);if(at==null||child.distanceToSqr(at.getCenter())>9)return false;
        for(int[] d:room.doors()){
            var position=GhostVictimWorld.at(s,d[0],d[1],d[2]);var state=level.getBlockState(position);if(!(state.getBlock() instanceof DoorBlock))return false;
        }
        var permits=escape.getCompound("UnlockedRooms");permits.putBoolean(id,true);escape.put("UnlockedRooms",permits);
        for(int[] d:room.doors()){var position=GhostVictimWorld.at(s,d[0],d[1],d[2]);var state=level.getBlockState(position);((DoorBlock)state.getBlock()).setOpen(child,level,state,position,true);}
        OtherMotherLoop.changed(p);return true;
    }
    public static boolean closeAfterLeaving(ServerPlayer p,VisitData.Scene s,CompoundTag ghost,TycoonNpc child){
        var escape=ghost.getCompound(GhostEscapeProgress.KEY);var room=room(escape.getString("AssignedRoom"));
        if(room==null||!GhostVictimMirrors.owned(p,s,ghost)||!GhostVictimActors.owns(child,s,ghost)||!GhostEscapeProgress.owns(ghost,escape)||!(child.level() instanceof ServerLevel level)||level!=AcceptedButtonEyesWorld.level(p)
            ||HouseCabinets.contains(AcceptedButtonEyesWorld.house(s),room,child.blockPosition()))return false;
        for(int[] d:room.doors()){
            var at=GhostVictimWorld.at(s,d[0],d[1],d[2]);if(child.getBoundingBox().intersects(new AABB(at).expandTowards(0,1,0)))return false;
            var state=level.getBlockState(at);if(state.getBlock() instanceof DoorBlock door)door.setOpen(child,level,state,at,false);
        }
        escape.getCompound("UnlockedRooms").remove(room.id());OtherMotherLoop.changed(p);return true;
    }
    public static boolean permittedDoor(VisitData.Scene s,BlockPos position){
        if(!GhostVictimDirector.active(s))return false;var ghost=GhostVictimEntry.data(s);var escape=ghost.getCompound(GhostEscapeProgress.KEY);
        if(!GhostEscapeProgress.owns(ghost,escape)||!GhostEscapeProgress.active(escape))return false;
        boolean wager=escape.getBoolean("WagerAgreed");
        for(var room:HouseCabinets.ROOMS)if(wager||room.id().equals(escape.getString("AssignedRoom"))&&escape.getCompound("UnlockedRooms").getBoolean(room.id()))
            for(int[] d:room.doors()){var at=GhostVictimWorld.at(s,d[0],d[1],d[2]);if(at.equals(position)||at.above().equals(position))return true;}
        return false;
    }
    public static boolean retire(ServerPlayer p,VisitData.Scene s,CompoundTag ghost){
        if(!GhostVictimMirrors.owned(p,s,ghost))return false;var level=AcceptedButtonEyesWorld.level(p);if(level==null)return false;boolean complete=true;
        for(String id:sites(ghost).getAllKeys()){
            var row=sites(ghost).getCompound(id);if(row.getBoolean("Retired")||!row.contains("Container"))continue;
            var at=BlockPos.of(row.getLong("Container"));level.getChunkAt(at);var block=level.getBlockEntity(at);
            if(block==null&&level.getBlockState(at).isAir()){row.putBoolean("Retired",true);continue;}
            if(!(block instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest)||!chest.isEmpty()){complete=false;continue;}
            var marker=chest.getPersistentData();var expected=NbtUtils.readBlockState(level.holderLookup(net.minecraft.core.registries.Registries.BLOCK),row.getCompound("Placed"));
            if(!level.getBlockState(at).equals(expected)||!marker.hasUUID("GhostEscapeOwner")||!s.owner.equals(marker.getUUID("GhostEscapeOwner"))
                ||!marker.hasUUID("GhostEscapeDoor")||!s.door.equals(marker.getUUID("GhostEscapeDoor"))||!marker.hasUUID("GhostEscapeSession")||!ghost.getUUID("Session").equals(marker.getUUID("GhostEscapeSession"))){complete=false;continue;}
            if(level.setBlock(at,Blocks.AIR.defaultBlockState(),2)){
                var cells=FirstEscapeScene.context(s).getCompound("Environment");String key=Long.toString(at.asLong());if(cells.contains(key))cells.getCompound(key).put("Placed",NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState()));row.putBoolean("Retired",true);
            }else complete=false;
        }
        OtherMotherLoop.changed(p);return complete;
    }
    private GhostEscapeRooms(){}
}
