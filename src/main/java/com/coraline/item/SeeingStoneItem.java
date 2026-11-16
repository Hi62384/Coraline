package com.coraline.item;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
/** Inspect through the existing physical opening; releasing use lowers it immediately. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="coraline")
public final class SeeingStoneItem extends Item {
    @net.minecraftforge.eventbus.api.SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void entityUse(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract event){raiseOnEntity(event);}
    @net.minecraftforge.eventbus.api.SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void preciseEntityUse(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific event){raiseOnEntity(event);}
    private static void raiseOnEntity(net.minecraftforge.event.entity.player.PlayerInteractEvent event){
        if(!(event.getItemStack().getItem() instanceof SeeingStoneItem))return;
        event.getEntity().startUsingItem(event.getHand());event.setCanceled(true);event.setCancellationResult(net.minecraft.world.InteractionResult.CONSUME);
    }
    @Override public net.minecraft.world.InteractionResult onItemUseFirst(ItemStack stack,net.minecraft.world.item.context.UseOnContext context){
        if(context.getPlayer()==null)return net.minecraft.world.InteractionResult.PASS;
        context.getPlayer().startUsingItem(context.getHand());return net.minecraft.world.InteractionResult.CONSUME;
    }

    public SeeingStoneItem(){super(new Properties().stacksTo(1));}
    @Override public Component getName(ItemStack s){return Component.literal("Seeing Stone");}
    @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand){player.startUsingItem(hand);return net.minecraft.world.InteractionResultHolder.consume(player.getItemInHand(hand));}
    @Override public int getUseDuration(ItemStack stack){return 72000;}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.NONE;}
    @Override public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context){var player=context.getPlayer();if(player==null)return net.minecraft.world.InteractionResult.PASS;player.startUsingItem(context.getHand());return net.minecraft.world.InteractionResult.CONSUME;}
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> out){out.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;@Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new com.coraline.client.renderer.SeeingStoneRenderer();return renderer;}});}
}
