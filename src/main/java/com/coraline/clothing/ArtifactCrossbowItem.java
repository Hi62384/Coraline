package com.coraline.clothing;

import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Crossbows use distinct charge time, durability and direct-hit damage. */
@Mod.EventBusSubscriber(modid="coraline")
public final class ArtifactCrossbowItem extends CrossbowItem {
    private static final String DAMAGE="CoralineCrossbowDamage";
    private final boolean perfect;
    public ArtifactCrossbowItem(boolean perfect){super(new Item.Properties().durability(perfect?978:652));this.perfect=perfect;}
    public static int chargeTicks(ItemStack stack){
        if(!(stack.getItem() instanceof ArtifactCrossbowItem crossbow))return -1;
        return crossbow.perfect?40:stack.getOrCreateTag().getBoolean("CoralineMoonlit")?10:30;
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        var player=event.player;boolean night=ClothingEffects.night(player.level());
        for(var stack:new ItemStack[]{player.getMainHandItem(),player.getOffhandItem()})
            if(stack.getItem() instanceof ArtifactCrossbowItem crossbow&&!crossbow.perfect&&stack.getOrCreateTag().getBoolean("CoralineMoonlit")!=night)stack.getOrCreateTag().putBoolean("CoralineMoonlit",night);
    }
    @SubscribeEvent public static void arrow(EntityJoinLevelEvent event){
        if(event.getLevel().isClientSide()||!(event.getEntity() instanceof AbstractArrow arrow)||!(arrow.getOwner() instanceof Player player))return;
        var main=player.getMainHandItem();var off=player.getOffhandItem();
        var stack=main.getItem() instanceof ArtifactCrossbowItem?main:off.getItem() instanceof ArtifactCrossbowItem?off:ItemStack.EMPTY;
        if(stack.getItem() instanceof ArtifactCrossbowItem crossbow)arrow.getPersistentData().putFloat(DAMAGE,crossbow.perfect?22F:12F);
    }
    @SubscribeEvent public static void impact(LivingHurtEvent event){
        if(event.getSource().getDirectEntity() instanceof AbstractArrow arrow&&arrow.getPersistentData().contains(DAMAGE))event.setAmount(arrow.getPersistentData().getFloat(DAMAGE));
    }
}
