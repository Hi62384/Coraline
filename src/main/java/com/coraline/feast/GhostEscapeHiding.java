package com.coraline.feast;

import com.coraline.tycoon.TycoonContent;
import com.coraline.tycoon.TycoonNpc;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Builds an owner-house hiding graph from real clearance, cover and paths.
 * The unspawned path probe never replaces or moves the canonical child. */
public final class GhostEscapeHiding {
    public static boolean prepare(net.minecraft.server.level.ServerPlayer p,VisitData.Scene s,CompoundTag ghost,TycoonNpc child){
        var escape=ghost.getCompound(GhostEscapeProgress.KEY);
        if(!GhostVictimMirrors.owned(p,s,ghost)||!GhostVictimActors.owns(child,s,ghost)||!GhostEscapeProgress.owns(ghost,escape)||!(child.level() instanceof ServerLevel level))return false;
        if(escape.contains("HidingNodes"))return !escape.getList("HidingNodes",Tag.TAG_COMPOUND).isEmpty();
        var realm=AcceptedButtonEyesWorld.realm(p,s);if(realm==null||realm.doorPos==null)return false;
        var state=level.getBlockState(realm.doorPos);if(!state.hasProperty(com.coraline.block.TinyDoorBlock.FACING))return false;
        var exit=realm.doorPos.relative(state.getValue(com.coraline.block.TinyDoorBlock.FACING),2);
        var probe=TycoonContent.NPC.get().create(level);if(probe==null)return false;
        if(probe.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation navigation)navigation.setCanOpenDoors(true);
        var nodes=new ListTag();int[][] rooms={{34,6,44},{47,6,47},{28,6,51},{44,6,37},{44,15,23},{35,15,48}};
        try{
            for(int room=0;room<rooms.length;room++){
                var center=GhostVictimWorld.at(s,rooms[room][0],rooms[room][1],rooms[room][2]);if(!GhostVictimNavigation.clear(level,probe,center))continue;
                int made=0;
                for(int dx=-8;dx<=8&&made<4;dx+=2)for(int dz=-8;dz<=8&&made<4;dz+=2){
                    var feet=center.offset(dx,0,dz);if(com.coraline.otherworld.OtherWorldEstate.outsideHouse(AcceptedButtonEyesWorld.house(s),feet)||!level.hasChunkAt(feet)||!GhostVictimNavigation.clear(level,probe,feet))continue;
                    boolean near=false;for(Tag entry:nodes)if(BlockPos.of(((CompoundTag)entry).getLong("Position")).distSqr(feet)<9){near=true;break;}if(near)continue;
                    var eye=Vec3.atBottomCenterOf(feet).add(0,probe.getEyeHeight(),0);var sight=Vec3.atBottomCenterOf(center).add(0,probe.getEyeHeight(),0);
                    var obstruction=level.clip(new ClipContext(sight,eye,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,probe));
                    if(obstruction.getType()==HitResult.Type.MISS)continue;
                    // The unspawned probe has no physics tick. clear() proved
                    // real dry footing, so enable grounded path queries here.
                    probe.setPos(Vec3.atBottomCenterOf(feet));probe.setOnGround(true);var toDoor=probe.getNavigation().createPath(exit,0);if(toDoor==null||!toDoor.canReach())continue;
                    var row=new CompoundTag();row.putString("Id","hide_"+nodes.size());row.putLong("Position",feet.asLong());row.putInt("Room",room);row.putString("Label",coverLabel(room,level.getBlockState(obstruction.getBlockPos()).getBlock().getName().getString()));row.putDouble("DistanceToDoor",pathLength(toDoor));nodes.add(row);made++;
                }
            }
            if(nodes.size()<4){escape.putString("Blocker","The house needs at least four covered, reachable hiding positions; no impossible wager was started.");return false;}
            var starts=new java.util.ArrayList<String>();
            for(Tag entry:nodes){var row=(CompoundTag)entry;var from=BlockPos.of(row.getLong("Position"));probe.setPos(Vec3.atBottomCenterOf(from));probe.setOnGround(true);var edges=new ListTag();
                for(Tag other:nodes){var target=(CompoundTag)other;if(target==row)continue;var path=probe.getNavigation().createPath(BlockPos.of(target.getLong("Position")),0);if(path!=null&&path.canReach())edges.add(StringTag.valueOf(target.getString("Id")));}
                row.put("Edges",edges);if(row.getDouble("DistanceToDoor")>=16&&!edges.isEmpty())starts.add(row.getString("Id"));
            }
            if(starts.isEmpty()){escape.putString("Blocker","No distant hiding position has a real route to the Little Door.");return false;}
            escape.put("HidingNodes",nodes);escape.putString("InitialHide",starts.get(Math.floorMod(escape.getUUID("Attempt").hashCode(),starts.size())));escape.putLong("FinalDoor",exit.asLong());OtherMotherLoop.changed(p);return true;
        }finally{probe.discard();}
    }
    static boolean refresh(net.minecraft.server.level.ServerPlayer player,VisitData.Scene scene,CompoundTag ghost,TycoonNpc child){
        var escape=ghost.getCompound(GhostEscapeProgress.KEY);
        long now=GhostVictimProgress.clock(ghost);
        if(now<escape.getLong("HideReplanAfter"))return false;
        escape.putLong("HideReplanAfter",now+200);
        var previous=escape.getList("HidingNodes",Tag.TAG_COMPOUND);
        String initial=escape.getString("InitialHide");
        var compromised=new java.util.HashSet<Long>();
        for(var tag:previous){
            var row=(CompoundTag)tag;
            if(row.getBoolean("Compromised"))compromised.add(row.getLong("Position"));
        }
        escape.remove("HidingNodes");
        if(!prepare(player,scene,ghost,child)){
            escape.put("HidingNodes",previous);escape.putString("InitialHide",initial);
            return false;
        }
        for(var tag:escape.getList("HidingNodes",Tag.TAG_COMPOUND)){
            var row=(CompoundTag)tag;
            if(compromised.contains(row.getLong("Position")))row.putBoolean("Compromised",true);
        }
        return true;
    }
    static String coverLabel(int room,String cover){
        String area=switch(room){case 0->"Hall";case 1->"Kitchen";case 2->"Living room";case 3->"Dining room";case 4->"Bedroom";default->"Upstairs corridor";};
        return cover.isBlank()?area+" cover":area+" cover behind "+cover.toLowerCase(java.util.Locale.ROOT);
    }
    private static double pathLength(net.minecraft.world.level.pathfinder.Path path){double length=0;for(int i=1;i<path.getNodeCount();i++)length+=Math.sqrt(path.getNode(i).asBlockPos().distSqr(path.getNode(i-1).asBlockPos()));return length;}
    public static CompoundTag node(CompoundTag escape,String id){for(Tag entry:escape.getList("HidingNodes",Tag.TAG_COMPOUND)){var n=(CompoundTag)entry;if(n.getString("Id").equals(id))return n;}return new CompoundTag();}
    public static boolean reachable(TycoonNpc child,CompoundTag escape,String id){
        var node=node(escape,id);if(!node.contains("Position")||node.getBoolean("Compromised")||!(child.level() instanceof ServerLevel level))return false;
        var at=BlockPos.of(node.getLong("Position"));if(!GhostVictimNavigation.clear(level,child,at))return false;var path=child.getNavigation().createPath(at,0);return path!=null&&path.canReach();
    }
    static boolean concealedAt(TycoonNpc child,com.coraline.parents.ParentEntity mother,BlockPos feet){
        if(mother==null||mother.level()!=child.level())return true;
        var eye=mother.getEyePosition();
        var target=Vec3.atBottomCenterOf(feet).add(0,child.getEyeHeight(),0);
        return eye.distanceToSqr(target)>24*24||child.level().clip(new ClipContext(eye,target,ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,mother)).getType()!=HitResult.Type.MISS;
    }
    public static boolean concealed(TycoonNpc child,com.coraline.parents.ParentEntity mother,CompoundTag escape,String id){
        var node=node(escape,id);if(!node.contains("Position")||node.getBoolean("Compromised")||!(child.level() instanceof ServerLevel level))return false;
        var at=BlockPos.of(node.getLong("Position"));return child.distanceToSqr(Vec3.atBottomCenterOf(at))<=.65*.65&&GhostVictimNavigation.clear(level,child,at)
            &&(mother==null||mother.level()!=level||!GhostVictimNavigation.canSee(mother,child,24,360));
    }
    private GhostEscapeHiding(){}
}
