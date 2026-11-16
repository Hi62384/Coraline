package com.coraline.tycoon;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;

/** Synchronous server-authorized household context. The actual player identity,
 * inventory, player-file receipts and personal story are never impersonated. */
public final class TycoonScope {
    private record Frame(UUID actor,UUID owner,UUID epoch){}
    private static final ThreadLocal<Frame> CURRENT=new ThreadLocal<>();
    private static UUID epoch(TycoonData.Home h){return h.state.hasUUID("CapabilityEpoch")?h.state.getUUID("CapabilityEpoch"):null;}
    public static UUID owner(ServerPlayer actor){
        var frame=CURRENT.get();if(frame==null||!frame.actor.equals(actor.getUUID()))return actor.getUUID();
        var home=TycoonData.get(actor.server).homes.get(frame.owner);
        if(home==null||!TycoonCollaborators.allowed(actor,frame.owner)||!Objects.equals(frame.epoch,epoch(home)))throw new SecurityException("Household permission or capability epoch changed");
        return frame.owner;
    }
    public static <T> T call(ServerPlayer actor,UUID owner,Supplier<T> action,T denied){
        if(!actor.server.isSameThread()||!TycoonCollaborators.allowed(actor,owner))return denied;
        var home=TycoonData.get(actor.server).homes.get(owner);if(home==null)return denied;
        var previous=CURRENT.get();CURRENT.set(new Frame(actor.getUUID(),owner,epoch(home)));
        try{return action.get();}finally{if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
    }
    public static void run(ServerPlayer actor,UUID owner,Runnable action){call(actor,owner,()->{action.run();return true;},false);}
    private TycoonScope(){}
}
