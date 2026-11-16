package com.coraline.feast;

import net.minecraft.server.level.ServerPlayer;

/** Accepted physical movement steps, not guesses from a lag-batched tick displacement. */
public final class RescueWebHearing {
    private static final java.util.Map<ServerPlayer,RescueChallengeMath.StepBatch> steps=new java.util.WeakHashMap<>();
    public record Sample(double distance,double loudest,int packets){}
    public static void acceptedMove(ServerPlayer player,double previousX,double previousZ){
        if(!player.server.isSameThread())return;var scene=OtherMotherLoop.scene(player);
        if(!RescueChapter.owns(player,scene)||RescueChapter.phase(scene)!=RescueChapter.Phase.WEB_STEALTH)return;
        double distance=Math.hypot(player.getX()-previousX,player.getZ()-previousZ);
        if(distance>1e-7||!Double.isFinite(distance))steps.computeIfAbsent(player,key->new RescueChallengeMath.StepBatch()).record(distance);
    }
    public static Sample consume(ServerPlayer player,double observed){
        var batch=steps.remove(player);double fallback=Double.isFinite(observed)&&observed>=0?observed:Double.POSITIVE_INFINITY;return batch==null?new Sample(fallback,fallback,0):new Sample(batch.distance(),batch.loudestWithUnaccounted(observed),batch.count());
    }
    public static void reset(ServerPlayer player){steps.remove(player);}
    private RescueWebHearing(){}
}
