package com.coraline.tycoon;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Every discovery requires a physical source and a real line of sight. */
public final class TycoonEvidence {
    public static boolean perceives(Entity observer,Vec3 target,double range,double cosine){
        Vec3 delta=target.subtract(observer.getEyePosition());
        if(delta.lengthSqr()>range*range||delta.lengthSqr()<.0001)return false;
        if(observer.getLookAngle().dot(delta.normalize())<cosine)return false;
        var hit=observer.level().clip(new ClipContext(observer.getEyePosition(),target,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,observer));
        return hit.getType()==HitResult.Type.MISS||hit.getLocation().distanceToSqr(target)<.2;
    }
    public static void remember(ServerPlayer p,TycoonData.Resident r,String source,String type,double severity){
        var h=TycoonWorld.home(p);var memories=r.n.getList("EvidenceMemories",10);
        for(var value:memories){var memory=(CompoundTag)value;if(memory.getString("Source").equals(source)&&memory.getString("Type").equals(type)){
            memory.putLong("LastSeen",h.clock());double novelty=Math.max(0,severity-memory.getDouble("Severity"));
            if(novelty>0){memory.putDouble("Severity",severity);TycoonDialogue.set(r,TycoonDialogue.trust(r)-novelty*.08,TycoonDialogue.suspicion(r)+novelty*.3);}return;
        }}
        var memory=new CompoundTag();memory.putString("Source",source);memory.putString("Type",type);memory.putDouble("Severity",Math.max(0,Math.min(1,severity)));memory.putLong("FirstSeen",h.clock());memory.putLong("LastSeen",h.clock());memory.putBoolean("Discussed",false);memories.add(memory);while(memories.size()>64)memories.remove(0);r.n.put("EvidenceMemories",memories);
        TycoonDialogue.set(r,TycoonDialogue.trust(r)-severity*.08,TycoonDialogue.suspicion(r)+severity*(.15+.2*r.trait("Curiosity")));TycoonWorld.dirty(p);
    }
    public static void observe(ServerPlayer p,TycoonData.Resident r,TycoonNpc actor){
        if(actor.tickCount%100!=0)return;var h=TycoonWorld.home(p);
        // Sources are maintained by actual machinery/structures, not arbitrary
        // rolls about things elsewhere in the owner's region.
        var sources=h.state.getCompound("EvidenceSources");int examined=0;
        for(String key:sources.getAllKeys()){
            if(examined++>=128)break;var n=sources.getCompound(key);if(!n.getBoolean("Active"))continue;var at=BlockPos.of(n.getLong("At"));
            if(!actor.level().hasChunkAt(at)||actor.level().getBlockState(at).isAir())continue;
            if(perceives(actor,at.getCenter(),12,.2))remember(p,r,key,n.getString("Type"),n.getDouble("Severity"));
        }
        // Unfinished land is only noticed at a visible nearby edge.
        for(var dir:net.minecraft.core.Direction.Plane.HORIZONTAL){var at=actor.blockPosition().relative(dir,4);if(!TycoonLandscape.owned(h,at)&&perceives(actor,at.getCenter().add(0,-1,0),7,.25))remember(p,r,"edge:"+TycoonLandscape.tileX(h,at)+","+TycoonLandscape.tileZ(h,at),"unfinished_edge",.2);}
    }
    private TycoonEvidence(){}
}
