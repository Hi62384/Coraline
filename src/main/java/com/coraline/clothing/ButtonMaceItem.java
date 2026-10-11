package com.coraline.clothing;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;

/** A blunt single-target hit with a full-strength radial impact, not a sword sweep. */
public final class ButtonMaceItem extends Item {
    private final Multimap<Attribute,AttributeModifier> modifiers=ImmutableMultimap.<Attribute,AttributeModifier>builder()
        .put(Attributes.ATTACK_DAMAGE,new AttributeModifier(BASE_ATTACK_DAMAGE_UUID,"Button mace damage",13,AttributeModifier.Operation.ADDITION))
        .put(Attributes.ATTACK_SPEED,new AttributeModifier(BASE_ATTACK_SPEED_UUID,"Button mace speed",-3.45,AttributeModifier.Operation.ADDITION)).build();
    public ButtonMaceItem(){super(new Item.Properties().durability(520));}
    @Override public Multimap<Attribute,AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot){return slot==EquipmentSlot.MAINHAND?modifiers:super.getDefaultAttributeModifiers(slot);}
    @Override public boolean hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker){
        stack.hurtAndBreak(1,attacker,e->e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        if(!(attacker instanceof Player player)||!(attacker.level() instanceof ServerLevel level))return true;
        double radius=WeaponConfig.MACE_RADIUS.get();
        for(var mob:level.getEntitiesOfClass(Mob.class,target.getBoundingBox().inflate(radius),m->m!=target&&m.isAlive()&&!m.isAlliedTo(player)&&!player.isAlliedTo(m)&&m.distanceToSqr(target)<=radius*radius))
            mob.hurt(player.damageSources().playerAttack(player),14F);
        level.sendParticles(ParticleTypes.CRIT,target.getX(),target.getY()+.8,target.getZ(),12,.55,.25,.55,.03);
        return true;
    }
}
