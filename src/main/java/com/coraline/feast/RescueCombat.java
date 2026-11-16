package com.coraline.feast;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

/** A scoped server hazard hit, not a general removal of Other World protection. */
public final class RescueCombat {
    private record Permit(java.util.UUID owner,DamageSource source){}
    private static final ThreadLocal<Permit> CURRENT=new ThreadLocal<>();
    public static boolean combatPhase(String phase){return phase.equals("MANTIS_FIGHT")||phase.equals("THEATRE_FIGHT")||phase.equals("SWARM_FIGHT");}
    public static boolean eligible(ServerPlayer p){
        var s=OtherMotherLoop.scene(p);if(s==null||!com.coraline.multiplayer.StoryCoop.member(p,s)||!p.isAlive()||p.isSpectator()||!com.coraline.feast.ActivityWorld.protectedWorld(p.level()))return false;
        var n=ParentRescueStory.data(s).getCompound("Continuation");
        return n.getBoolean("Started")&&combatPhase(n.getString("Phase"));
    }
    public static boolean permitted(ServerPlayer p,DamageSource source){var permit=CURRENT.get();return permit!=null&&permit.owner.equals(p.getUUID())&&permit.source==source&&eligible(p);}
    public static boolean harm(ServerPlayer p,float amount){
        if(!eligible(p)||!Float.isFinite(amount)||amount<=0||amount>12)return false;
        var previous=CURRENT.get();var source=p.damageSources().magic();CURRENT.set(new Permit(p.getUUID(),source));
        try{return p.hurt(source,amount);}finally{if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
    }
    /** Compensate for armor using vanilla mitigation; enchantments and resistance still help. */
    public static float armoredDamage(float target,float armor,float toughness){
        float low=target,high=64;
        for(int i=0;i<16;i++){float middle=(low+high)*.5F;if(net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(middle,armor,toughness)<target)low=middle;else high=middle;}
        return high;
    }
    public static boolean bossHarm(ServerPlayer p,float target){
        if(!eligible(p)||target<1||target>12)return false;
        float amount=armoredDamage(target,p.getArmorValue(),(float)p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS));
        var previous=CURRENT.get();var source=p.damageSources().generic();CURRENT.set(new Permit(p.getUUID(),source));
        try{return p.hurt(source,amount);}finally{if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
    }
    private RescueCombat(){}
}
