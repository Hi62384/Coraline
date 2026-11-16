package com.coraline.clothing;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Sixty base damage including the player's base point, one full strike about every
 * 1.1 seconds. Cancelled attacks never reset or bypass vanilla protections. */
@Mod.EventBusSubscriber(modid="coraline")
public final class OtherVoidSwordItem extends SwordItem {
    static final int STRIKE_TICKS=23;
    private static final String NEXT="CoralineVoidSwordNextStrike";
    public OtherVoidSwordItem() {
        super(Tiers.NETHERITE,55,-3.1F,new Item.Properties().durability(4096).fireResistant().rarity(Rarity.EPIC));
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void attack(AttackEntityEvent event) {
        var player=event.getEntity();
        if(player.level().isClientSide||!(player.getMainHandItem().getItem() instanceof OtherVoidSwordItem item))return;
        long now=player.level().getServer().overworld().getGameTime();var state=player.getPersistentData();
        long next=state.getLong(NEXT);
        // Normalize a corrupt/far-future clock once, rather than leaving the
        // weapon permanently locked after an older world-time restore.
        if(next>now+STRIKE_TICKS){next=now+STRIKE_TICKS;state.putLong(NEXT,next);}
        if(now<next||player.getCooldowns().isOnCooldown(item)||player.getAttackStrengthScale(.5F)<1)
            event.setCanceled(true);
    }
    @Override public boolean hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker) {
        boolean result=super.hurtEnemy(stack,target,attacker);
        if(attacker instanceof net.minecraft.world.entity.player.Player player&&attacker.level() instanceof ServerLevel level) {
            player.getPersistentData().putLong(NEXT,level.getServer().overworld().getGameTime()+STRIKE_TICKS);
            player.getCooldowns().addCooldown(this,STRIKE_TICKS);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,target.getX(),target.getY()+target.getBbHeight()*.5,target.getZ(),12,.3,.4,.3,.04);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,target.getX(),target.getY()+target.getBbHeight()*.5,target.getZ(),6,.2,.3,.2,.01);
            level.playSound(null,target.blockPosition(),SoundEvents.WARDEN_ATTACK_IMPACT,SoundSource.PLAYERS,.45F,.65F);
        }
        return result;
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags) {
        lines.add(Component.translatable("tooltip.coraline.other_void_sword"));
    }
}
