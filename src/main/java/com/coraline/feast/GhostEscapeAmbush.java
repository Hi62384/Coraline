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
        if(!node.contains("Position")){e.putString("Blocker","The assigned hiding position is unavailable.");return true;}
        var at=BlockPos.of(node.getLong("Position"));
        if(phase==GhostEscapeProgress.Phase.APPROACH_AMBUSH){
            if(GhostVictimRoutines.travel(p,s,ghost,child,at,false)==GhostVictimNavigation.Result.ARRIVED&&GhostEscapeHiding.concealed(child,mother,e,id)){
                GhostEscapeProgress.hiddenForKey(ghost,id,true);ghost.remove("ChildRoutine");child.getNavigation().stop();
                GhostVictimDirector.words(ghost,"I'm hidden. Lead her close, then keep her looking away from me.");OtherMotherLoop.changed(p);
            }
            return true;
        }
        // The opportunity is broken immediately by lost reach, eye contact,
        // changed dimensions or an interrupted approach; it never accumulates.
        boolean near=mother!=null&&GhostEscapePerception.pair(p,s,ghost,child,mother)&&mother.distanceToSqr(at.getCenter())<=36;
        boolean away=near&&!GhostVictimNavigation.canSee(mother,child,8,150);
        if(!near||!away){
            GhostEscapeProgress.keyOpportunity(ghost,false,false,false);
            GhostVictimRoutines.travel(p,s,ghost,child,at,false);return true;
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
    private GhostEscapeAmbush(){}
}
