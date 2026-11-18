package com.coraline.feast;

import com.coraline.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Physical key theft. A clock alone cannot grant the key: the same canonical
 * child must continuously reach the distracted canonical Mother's waist. */
public final class GhostEscapeAmbush {
    public static boolean tick(ServerPlayer p,VisitData.Scene s,CompoundTag ghost){
        var e=ghost.getCompound(GhostEscapeProgress.KEY);
        if(!GhostVictimMirrors.owned(p,s,ghost)||!GhostEscapeProgress.owns(ghost,e))return false;
        var phase=GhostEscapeProgress.phase(e);
        if(phase!=GhostEscapeProgress.Phase.APPROACH_AMBUSH&&phase!=GhostEscapeProgress.Phase.WAIT_LURE&&phase!=GhostEscapeProgress.Phase.STEAL_KEY)return false;
        var child=GhostVictimActors.find(p,s,ghost);var mother=AcceptedButtonEyesWorld.mother(p,s);
        if(child==null||child.level()!=AcceptedButtonEyesWorld.level(p))return true;
        GhostEscapeCovert.hands(child);
        String id=phase==GhostEscapeProgress.Phase.APPROACH_AMBUSH?e.getString("TargetHide"):e.getString("HidingSpot");
        var node=GhostEscapeHiding.node(e,id);
        if(!node.contains("Position")||node.getBoolean("Compromised")){
            returnToMirror(p,ghost,e,"This hiding place is gone. Meet me at the bedroom mirror.");
            return true;
        }
        var at=BlockPos.of(node.getLong("Position"));
        long clock=GhostVictimProgress.clock(ghost);
        if(phase==GhostEscapeProgress.Phase.WAIT_LURE&&clock%100==0
                &&clock>=e.getLong("DistractionUntil")
                &&workingLights(ghost,child).stream().noneMatch(light->at.distSqr(light)<=36)){
            GhostVictimWorld.refreshRoomLights(p,s,ghost);
            if(workingLights(ghost,child).stream().noneMatch(light->at.distSqr(light)<=36)){
                String replacement=coverNearLight(ghost,e,child);
                e.remove("StealSignaled");
                ghost.remove("ChildRoutine");
                if(replacement.isEmpty()){
                    returnToMirror(p,ghost,e,"The light here is gone. Meet me at the bedroom mirror.");
                    return true;
                }else{
                    e.putString("TargetHide",replacement);
                    GhostEscapeProgress.move(e,GhostEscapeProgress.Phase.APPROACH_AMBUSH,clock);
                    GhostVictimDirector.words(ghost,"The light here is gone. I'm moving to another hiding place.");
                }
                OtherMotherLoop.changed(p);
                return true;
            }
        }
        if(phase==GhostEscapeProgress.Phase.APPROACH_AMBUSH){
            var arrival=GhostVictimRoutines.travel(p,s,ghost,child,at,false);
            if(arrival==GhostVictimNavigation.Result.BLOCKED){
                returnToMirror(p,ghost,e,"I can't reach that hiding place. Meet me at the bedroom mirror.");
            }else if(arrival==GhostVictimNavigation.Result.ARRIVED&&GhostEscapeHiding.concealed(child,mother,e,id)){
                GhostEscapeProgress.hiddenForKey(ghost,id,true);ghost.remove("ChildRoutine");child.getNavigation().stop();
                GhostVictimDirector.words(ghost,"I'm hidden. Lead her close, then keep her looking away from me.");OtherMotherLoop.changed(p);
            }
            return true;
        }
        // The opportunity is broken immediately by lost reach, eye contact,
        // changed dimensions or an interrupted approach; it never accumulates.
        boolean near=mother!=null&&GhostEscapePerception.pair(p,s,ghost,child,mother)&&mother.distanceToSqr(at.getCenter())<=36;
        boolean distracted=near&&distractionReady(ghost,e,mother);
        boolean away=distracted&&!GhostVictimNavigation.canSee(mother,child,8,150);
        if(!away||!e.getBoolean("StealSignaled")){
            if(phase==GhostEscapeProgress.Phase.STEAL_KEY){
                GhostEscapeProgress.move(e,GhostEscapeProgress.Phase.WAIT_LURE,clock);
                e.remove("StealSignaled");
                GhostVictimDirector.words(ghost,"She turned back. Wait for another opening.");
                OtherMotherLoop.changed(p);
            }
            GhostVictimRoutines.travel(p,s,ghost,child,at,false);
            return true;
        }
        var approach=GhostVictimNavigation.approach(child,mother.blockPosition());
        if(approach!=null)GhostVictimRoutines.travel(p,s,ghost,child,approach,false);
        boolean reaching=GhostEscapePerception.canReachWaist(p,s,ghost,child,mother);
        if(reaching){child.getNavigation().stop();child.gesture(2,5);child.getLookControl().setLookAt(mother.getX(),mother.getY()+mother.getBbHeight()*.48,mother.getZ(),30,30);}
        if(GhostEscapeProgress.keyOpportunity(ghost,near,away,reaching)){
            var key=new ItemStack(ModItems.LITTLE_DOOR_STORY_KEY.get());var tag=key.getOrCreateTag();
            tag.putUUID("GhostKeyOwner",s.owner);tag.putUUID("GhostKeyDoor",s.door);tag.putUUID("GhostKeyAttempt",e.getUUID("Attempt"));
            // This is a container-key capability, never the player's return key.
            tag.putBoolean("GhostContainerKey",true);
            var inventory=e.getCompound("QuestInventory");inventory.put("ContainerKey",key.save(new CompoundTag()));e.put("QuestInventory",inventory);
            e.remove("ReportSpoken");ghost.remove("ChildRoutine");child.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            GhostVictimDirector.words(ghost,"I have it. Meet me at the mirror.");OtherMotherLoop.changed(p);
        }
        return true;
    }
    private static void returnToMirror(ServerPlayer p,CompoundTag ghost,CompoundTag escape,String message){
        escape.remove("ReportSpoken");
        escape.remove("StealSignaled");
        ghost.remove("ChildRoutine");
        GhostEscapeProgress.move(escape,GhostEscapeProgress.Phase.ROOM_REPORT,GhostVictimProgress.clock(ghost));
        GhostVictimDirector.words(ghost,message);
        OtherMotherLoop.changed(p);
    }
    private static java.util.List<BlockPos> workingLights(CompoundTag ghost,com.coraline.tycoon.TycoonNpc child){
        var level=child.level();
        var points=ghost.getCompound("Points");
        var lamps=new java.util.ArrayList<BlockPos>();
        for(String id:points.getAllKeys()){
            if(!id.startsWith("room_light_"))continue;
            var point=points.getCompound(id);
            if(!level.dimension().location().toString().equals(point.getString("Dimension")))continue;
            var at=BlockPos.of(point.getLong("Position"));
            if(level.hasChunkAt(at)&&(point.contains("EffectUntil")
                    ||GhostVictimManifestations.lightSource(level.getBlockState(at))))lamps.add(at);
        }
        return lamps;
    }
    static String coverNearLight(CompoundTag ghost,CompoundTag escape,com.coraline.tycoon.TycoonNpc child){
        if(child==null)return "";
        var lamps=workingLights(ghost,child);
        var covers=new java.util.ArrayList<CompoundTag>();
        for(var tag:escape.getList("HidingNodes",net.minecraft.nbt.Tag.TAG_COMPOUND)){
            var node=(CompoundTag)tag;
            if(!node.contains("Position")||node.getBoolean("Compromised"))continue;
            var feet=BlockPos.of(node.getLong("Position"));
            if(lamps.stream().anyMatch(lamp->feet.distSqr(lamp)<=36))covers.add(node);
        }
        covers.sort(java.util.Comparator.comparingDouble(node->BlockPos.of(node.getLong("Position")).distSqr(child.blockPosition())));
        for(var node:covers){
            var id=node.getString("Id");
            if(GhostEscapeHiding.reachable(child,escape,id))return id;
        }
        return "";
    }
    static boolean distractionReady(CompoundTag ghost,CompoundTag escape,com.coraline.parents.ParentEntity mother){
        if(mother==null||!escape.contains("Distraction")||GhostVictimProgress.clock(ghost)>=escape.getLong("DistractionUntil"))return false;
        var point=BlockPos.of(escape.getLong("Distraction"));
        if(!ghost.contains("MotherInvestigation")||ghost.getLong("MotherInvestigation")!=point.asLong())return false;
        var direction=point.getCenter().subtract(mother.getEyePosition());
        return mother.distanceToSqr(point.getCenter())<=36&&GhostVictimNavigation.view(mother).dot(direction.normalize())>.65;
    }
    private GhostEscapeAmbush(){}
}
