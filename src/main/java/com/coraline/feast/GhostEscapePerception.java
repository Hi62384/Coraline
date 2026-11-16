package com.coraline.feast;

import com.coraline.parents.ParentEntity;
import com.coraline.registry.ModItems;
import com.coraline.secrets.HouseCabinets;
import com.coraline.tycoon.TycoonNpc;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Real sight/reach facts for the escape director. Matching coordinates in a
 * different dimension, suspicion alone and client assertions are not evidence. */
public final class GhostEscapePerception {
    public static boolean pair(ServerPlayer p,VisitData.Scene s,CompoundTag ghost,TycoonNpc child,ParentEntity mother){
        return s!=null&&p.getUUID().equals(s.owner)&&GhostVictimMirrors.owned(p,s,ghost)&&GhostVictimActors.owns(child,s,ghost)
            &&mother!=null&&mother.isAlive()&&mother.isMom()&&mother.getUUID().equals(s.mom)&&child.isAlive()&&child.level()==mother.level();
    }
    public static boolean stoneVisible(TycoonNpc child){return child!=null&&(child.getMainHandItem().is(ModItems.SEEING_STONE.get())||child.getOffhandItem().is(ModItems.SEEING_STONE.get()));}
    public static GhostEscapeProgress.Discovery observe(ServerPlayer p,VisitData.Scene s,CompoundTag ghost,TycoonNpc child,ParentEntity mother){
        var n=ghost.getCompound(GhostEscapeProgress.KEY);
        if(!GhostEscapeProgress.owns(ghost,n)||!pair(p,s,ghost,child,mother)||!GhostEscapeProgress.active(n))return GhostEscapeProgress.Discovery.NONE;
        var phase=GhostEscapeProgress.phase(n);
        if(java.util.Set.of(GhostEscapeProgress.Phase.INTERCEPT,GhostEscapeProgress.Phase.WAGER,GhostEscapeProgress.Phase.SAFE,GhostEscapeProgress.Phase.RESTORING,GhostEscapeProgress.Phase.FAREWELL).contains(phase))return GhostEscapeProgress.Discovery.NONE;
        boolean hunting=java.util.Set.of(GhostEscapeProgress.Phase.HIDDEN,GhostEscapeProgress.Phase.MOVING_HIDE,GhostEscapeProgress.Phase.FINAL_CROSSING,GhostEscapeProgress.Phase.OPEN_DOOR,GhostEscapeProgress.Phase.CRAWL).contains(phase);
        boolean sight=GhostVictimNavigation.canSee(mother,child,hunting?24:16,hunting?150:120);
        if(hunting){
            boolean searched=n.contains("SearchedAt")&&GhostVictimProgress.clock(ghost)-n.getLong("SearchedAt")<=10&&n.getString("SearchedHide").equals(n.getString("HidingSpot"))
                &&mother.distanceToSqr(child)<4&&GhostVictimNavigation.canSee(mother,child,2,360);
            return sight&&mother.distanceToSqr(child)<1.7*1.7||searched?GhostEscapeProgress.Discovery.PHYSICAL_CAPTURE:GhostEscapeProgress.Discovery.NONE;
        }
        if(!sight)return GhostEscapeProgress.Discovery.NONE;
        if(stoneVisible(child))return GhostEscapeProgress.Discovery.EXPOSED_SEEING_STONE;
        if(phase==GhostEscapeProgress.Phase.SCAN||phase==GhostEscapeProgress.Phase.COLLECT_EYES||phase==GhostEscapeProgress.Phase.LEAVE_ROOM){
            for(var room:HouseCabinets.ROOMS)if(room.id().equals(n.getString("AssignedRoom"))&&HouseCabinets.contains(AcceptedButtonEyesWorld.house(s),room,child.blockPosition()))return GhostEscapeProgress.Discovery.EYE_ROOM;
        }
        if(phase==GhostEscapeProgress.Phase.LOCKPICK&&child.gesture()!=0)return GhostEscapeProgress.Discovery.LOCKPICKING;
        if(phase==GhostEscapeProgress.Phase.STEAL_KEY&&child.distanceToSqr(mother)<9)return GhostEscapeProgress.Discovery.STEALING;
        return GhostEscapeProgress.Discovery.NONE;
    }
    public static boolean canReachWaist(ServerPlayer p,VisitData.Scene s,CompoundTag ghost,TycoonNpc child,ParentEntity mother){
        if(!pair(p,s,ghost,child,mother)||child.distanceToSqr(mother)>2.5*2.5||child.isPassenger())return false;
        var at=mother.position().add(0,mother.getBbHeight()*.48,0);var delta=child.getEyePosition().subtract(mother.getEyePosition());
        if(delta.lengthSqr()<.001||GhostVictimNavigation.view(mother).dot(delta.normalize())>.1)return false;
        if(GhostVictimNavigation.canSee(mother,child,6,150))return false;
        return child.level().clip(new ClipContext(child.getEyePosition(),at,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,child)).getType()==HitResult.Type.MISS;
    }
    public static boolean signalPoint(ServerPlayer p,VisitData.Scene s,CompoundTag ghost,TycoonNpc child,BlockPos actualPoint){
        if(!GhostVictimMirrors.participant(p,s,ghost)||!GhostVictimActors.owns(child,s,ghost)||actualPoint==null||p.level()!=child.level()
            ||p.distanceToSqr(actualPoint.getCenter())>36||child.distanceToSqr(actualPoint.getCenter())>36||!p.isSpectator())return false;
        var delta=actualPoint.getCenter().subtract(p.getEyePosition());if(delta.lengthSqr()<.001||p.getLookAngle().dot(delta.normalize())<.92)return false;
        return child.level().clip(new ClipContext(child.getEyePosition(),actualPoint.getCenter(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,child)).getType()==HitResult.Type.MISS;
    }
    public static boolean destinationSignalPoint(ServerPlayer p,VisitData.Scene s,CompoundTag ghost,TycoonNpc child,BlockPos actualPoint){
        if(!GhostVictimMirrors.participant(p,s,ghost)||!GhostVictimActors.owns(child,s,ghost)||actualPoint==null||p.level()!=child.level()||!p.isSpectator())return false;
        var delta=actualPoint.getCenter().subtract(p.getEyePosition());
        if(delta.lengthSqr()>36||delta.lengthSqr()<.001||p.getLookAngle().dot(delta.normalize())<.92)return false;
        // The ghost inspects the selected destination. The child may be far
        // away: long, dangerous physical routes are deliberately permitted.
        return p.level().clip(new ClipContext(p.getEyePosition(),actualPoint.getCenter(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()==HitResult.Type.MISS;
    }
    private GhostEscapePerception(){}
}
