package com.coraline.clothing;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.*;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="coraline")
public final class ArmorSetEffects {
    public static boolean soulbound(Player p){return ArmorSets.full(p,ArmorSets.Set.SOULBOUND)||ArmorSets.full(p,ArmorSets.Set.SOULBOUND_POTENT_BELDAM_WEAVE);}
    @SubscribeEvent public static void soulHit(LivingDamageEvent event){
        if(event.getAmount()>0&&!event.getEntity().level().isClientSide&&event.getEntity() instanceof net.minecraft.world.entity.Mob&&!event.getSource().is(DamageTypes.THORNS)&&event.getSource().getEntity() instanceof Player p&&soulbound(p))p.heal(ClothingConfig.SOUL_HIT.get().floatValue());
    }
    @SubscribeEvent public static void soulKill(LivingDeathEvent event){
        if(!event.getEntity().level().isClientSide&&event.getEntity() instanceof net.minecraft.world.entity.Mob&&event.getSource().getEntity() instanceof Player p&&soulbound(p))p.heal(ClothingConfig.SOUL_KILL.get().floatValue());
    }
    @SubscribeEvent public static void retaliation(LivingDamageEvent event){
        if(!(event.getEntity() instanceof Player p)||p.level().isClientSide||event.getAmount()<=0||event.getSource().is(DamageTypes.THORNS))return;
        if(!(event.getSource().getEntity() instanceof LivingEntity attacker)||attacker==p)return;
        int pieces=ArmorSets.count(p,ArmorSets.Set.BUTTONGUARD);
        if(pieces>0)attacker.hurt(p.damageSources().thorns(p),(float)(event.getAmount()*pieces*ClothingConfig.REFLECT.get()));
        if(ArmorSets.full(p,ArmorSets.Set.GARDEN_VINES)&&p.getRandom().nextDouble()<ClothingConfig.VINE_CHANCE.get()){
            attacker.addEffect(new MobEffectInstance(MobEffects.POISON,ClothingConfig.VINE_TICKS.get(),0),p);
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,ClothingConfig.VINE_TICKS.get(),0),p);
        }
    }
    @SubscribeEvent public static void looting(LootingLevelEvent event){
        if(event.getDamageSource()!=null&&event.getDamageSource().getEntity() instanceof Player p&&ArmorSets.full(p,ArmorSets.Set.LUCK))event.setLootingLevel(event.getLootingLevel()+ClothingConfig.LUCK_LOOTING.get());
    }
}
