package com.coraline.clothing;

import java.util.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Bounded primary rapier strikes. No global hurt-time reset, damage immunity
 * bypass, synthetic second attack, or special path for sweep damage. */
@Mod.EventBusSubscriber(modid="coraline")
public final class RapierCombat {
    private static final String NEXT="CoralineRapierNextStrike";
    private record Previous(UUID owner,DamageSource source,long worldTick,int targetTick) {}
    private record Permit(UUID target,long tick) {}
    private static final Map<LivingEntity,Previous> PREVIOUS=new WeakHashMap<>();
    private static final Map<Player,Permit> PERMITS=new WeakHashMap<>();
    private static long pruned=Long.MIN_VALUE;
    public static boolean held(Player p){return p.getMainHandItem().getItem() instanceof StoryWeapon weapon&&weapon.id.equals("needle_rapier");}
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void attack(AttackEntityEvent event) {
        var p=event.getEntity();if(p.level().isClientSide)return;
        PERMITS.remove(p);if(!held(p))return;
        long now=p.level().getServer().overworld().getGameTime();int interval=WeaponConfig.RAPIER_INTERVAL.get();
        long next=p.getPersistentData().getLong(NEXT);
        if(next>now+interval){next=now+interval;p.getPersistentData().putLong(NEXT,next);}
        if(now<next||p.getCooldowns().isOnCooldown(p.getMainHandItem().getItem())||p.getAttackStrengthScale(.5F)<1)event.setCanceled(true);
        else if(event.getTarget() instanceof LivingEntity&&event.getTarget()!=p)PERMITS.put(p,new Permit(event.getTarget().getUUID(),now));
    }
    /** Called exactly around Player.attack's primary Entity.hurt invocation.
     * Every permission and damage event still runs inside the ordinary hurt
     * method. An unsuccessful/cancelled hit restores the previous window. */
    public static boolean primaryHurt(Player p,Entity target,DamageSource source,float amount) {
        if(p.level().isClientSide)return target.hurt(source,amount);
        var permit=PERMITS.remove(p);long now=p.level().getServer().overworld().getGameTime();
        if(permit==null||permit.tick()!=now||!permit.target().equals(target.getUUID())||source.getEntity()!=p||source.getDirectEntity()!=p||!held(p)
            ||!source.is(DamageTypes.PLAYER_ATTACK)||!(target instanceof LivingEntity living)||target==p||target.level()!=p.level())
            return target.hurt(source,amount);
        var previous=PREVIOUS.get(living);
        int old=living.invulnerableTime,interval=WeaponConfig.RAPIER_INTERVAL.get();
        boolean own=previous!=null&&previous.owner().equals(p.getUUID())&&previous.source()==living.getLastDamageSource();
        boolean advance=previous!=null&&RapierCadence.ordinaryWindow(own,now-previous.worldTick(),living.tickCount-previous.targetTick(),old,interval);
        if(advance)living.invulnerableTime=0;
        boolean hurt=false;
        try {
            hurt=target.hurt(source,amount);
            if(hurt) {
                p.getPersistentData().putLong(NEXT,now+interval);p.getCooldowns().addCooldown(p.getMainHandItem().getItem(),interval);
                if(living.invulnerableTime==20&&living.getLastDamageSource()==source)PREVIOUS.put(living,new Previous(p.getUUID(),source,now,living.tickCount));
                else PREVIOUS.remove(living);
            }
            return hurt;
        } finally {
            if(advance&&!hurt)living.invulnerableTime=Math.max(old,living.invulnerableTime);
        }
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||event.player.level().isClientSide)return;
        PERMITS.remove(event.player);
        long now=event.player.level().getServer().overworld().getGameTime();
        if(pruned==Long.MIN_VALUE||now<pruned||now-pruned>=20) {
            PREVIOUS.entrySet().removeIf(entry->!entry.getKey().isAlive()||now<entry.getValue().worldTick()||now-entry.getValue().worldTick()>20);pruned=now;
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){PREVIOUS.clear();PERMITS.clear();pruned=Long.MIN_VALUE;}
    private RapierCombat() {}
}
