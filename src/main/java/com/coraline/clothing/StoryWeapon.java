package com.coraline.clothing;

import com.google.common.collect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.effect.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import java.util.*;

public final class StoryWeapon extends SwordItem {
    public final String id;
    private static final UUID WHIP_REACH=UUID.fromString("96262716-a3e0-4054-9e3f-942b668a9dfa");
    public StoryWeapon(String id,int totalDamage,float attacksPerSecond){super(id.equals("moonblade")?OtherMaterials.MOONSTONE_TIER:Tiers.IRON,totalDamage-(id.equals("moonblade")?4:3),attacksPerSecond-4,new Item.Properties());this.id=id;}
    @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level,Player p,net.minecraft.world.InteractionHand hand){
        if(!id.equals("thread_whip")||hand!=net.minecraft.world.InteractionHand.MAIN_HAND)return super.use(level,p,hand);
        // Alternate use gives the same attack and reach as left-click, not a second damage path.
        if(p.getCooldowns().isOnCooldown(this))return net.minecraft.world.InteractionResultHolder.fail(p.getItemInHand(hand));
        if(!level.isClientSide){
            var start=p.getEyePosition();var end=start.add(p.getLookAngle().scale(p.getAttributeValue(net.minecraftforge.common.ForgeMod.ENTITY_REACH.get())));
            var hit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level,p,start,end,p.getBoundingBox().expandTowards(end.subtract(start)).inflate(1),e->e instanceof LivingEntity&&e.isPickable()&&!e.isSpectator());
            if(hit!=null&&p.hasLineOfSight(hit.getEntity()))p.attack(hit.getEntity());else WhipAction.begin(p,null);
        }
        p.swing(hand);return net.minecraft.world.InteractionResultHolder.sidedSuccess(p.getItemInHand(hand),level.isClientSide);
    }
    @Override public Multimap<Attribute,AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot){
        var normal=super.getDefaultAttributeModifiers(slot);
        if(!id.equals("thread_whip")||slot!=EquipmentSlot.MAINHAND)return normal;
        return ImmutableMultimap.<Attribute,AttributeModifier>builder().putAll(normal).put(net.minecraftforge.common.ForgeMod.ENTITY_REACH.get(),new AttributeModifier(WHIP_REACH,"Thread whip reach",2.5,AttributeModifier.Operation.ADDITION)).build();
    }
    @Override public boolean hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker){
        boolean result=super.hurtEnemy(stack,target,attacker);
        if(!(attacker.level() instanceof ServerLevel level))return result;
        if(id.equals("vine_sword")){
            target.addEffect(new MobEffectInstance(MobEffects.POISON,WeaponConfig.VINE_TICKS.get()),attacker);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,WeaponConfig.VINE_TICKS.get()),attacker);
        }else if(id.equals("thread_whip")&&target instanceof Mob){
            var toward=attacker.position().subtract(target.position());double resistance=target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
            var pull=toward.normalize().scale(WeaponConfig.WHIP_PULL.get()*(1-resistance));target.setDeltaMovement(target.getDeltaMovement().add(pull.x,.15*(1-resistance),pull.z));target.hurtMarked=true;
        }else if(id.equals("spidervision_sword")&&target instanceof Mob&&attacker instanceof Player p){SpiderArmor.placeWeb(p,target.blockPosition());}
        return result;
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags){
        if(!id.equals("needle_rapier"))lines.add(Component.translatable("tooltip.coraline."+id).withStyle(net.minecraft.ChatFormatting.GREEN));
    }
}
