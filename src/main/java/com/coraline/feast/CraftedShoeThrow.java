package com.coraline.feast;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.stats.Stats;
import net.minecraft.world.*;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Ordinary crafted copies reuse the existing shoe item and vanilla projectile
 * tracking/rendering. They never mint or satisfy First Escape custody tokens. */
@Mod.EventBusSubscriber(modid="coraline")
public final class CraftedShoeThrow {
    private static final String FLIGHT="CoralineCraftedShoeFlight", RECOVER="CoralineCraftedShoeRecover";
    static boolean bound(ItemStack stack) {
        var tag=stack.getTag();return tag!=null&&(tag.contains("StoryOwner")||tag.contains("StoryToken"));
    }
    static boolean throwOrdinary(ServerPlayer player,InteractionHand hand,ItemStack stack) {
        if(!stack.is(FirstEscapeItems.SHOE.get())||bound(stack)||player.getCooldowns().isOnCooldown(stack.getItem()))return false;
        var ball=new Snowball(player.serverLevel(),player);ball.setItem(stack.copyWithCount(1));
        ball.getPersistentData().putBoolean(FLIGHT,true);
        ball.getPersistentData().putBoolean(RECOVER,!player.getAbilities().instabuild);
        ball.getPersistentData().putUUID("CoralineCraftedShoeOwner",player.getUUID());
        ball.shootFromRotation(player,player.getXRot(),player.getYRot(),0,1.05F,.5F);
        if(!player.serverLevel().addFreshEntity(ball))return false;
        if(!player.getAbilities().instabuild)stack.shrink(1);
        player.getCooldowns().addCooldown(FirstEscapeItems.SHOE.get(),10);
        player.awardStat(Stats.ITEM_USED.get(FirstEscapeItems.SHOE.get()));player.swing(hand,true);
        player.level().playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.SNOWBALL_THROW,SoundSource.PLAYERS,.55F,.85F);
        return true;
    }
    /** LOWEST respects earlier protection cancellation; damage still goes
     * through vanilla/Forge hurt permissions, immunities and PvP protection. */
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void impact(ProjectileImpactEvent event) {
        if(!(event.getProjectile() instanceof Snowball ball)||ball.level().isClientSide)return;
        var tag=ball.getPersistentData();
        if(!tag.getBoolean(FLIGHT)||!ball.getItem().is(FirstEscapeItems.SHOE.get())||bound(ball.getItem()))return;
        tag.putBoolean(FLIGHT,false);
        if(tag.getBoolean(RECOVER)) {
            tag.putBoolean(RECOVER,false);var returned=ball.spawnAtLocation(ball.getItem().copyWithCount(1));
            if(returned!=null&&tag.hasUUID("CoralineCraftedShoeOwner"))returned.getPersistentData().putUUID("CoralineCraftedShoeOwner",tag.getUUID("CoralineCraftedShoeOwner"));
        }
        if(event.getRayTraceResult() instanceof EntityHitResult hit)
            hit.getEntity().hurt(ball.damageSources().thrown(ball,ball.getOwner()),1);
    }
    private CraftedShoeThrow() {}
}
