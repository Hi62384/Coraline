package com.coraline.item;

import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;

/** Physical keepsake; the owner's saved claim ledger, never an item count, grants progression. */
public final class GhostEyeItem extends Item {
    public GhostEyeItem(){super(new Properties().stacksTo(1));}
    @Override public Component getName(ItemStack stack){int eye=stack.hasTag()?stack.getTag().getInt("GhostEye"):0;return Component.literal(new String[]{"Garden Ghost Eye","Theatre Ghost Eye","Circus Ghost Eye"}[Math.max(0,Math.min(2,eye))]);}
    @Override public boolean onDroppedByPlayer(ItemStack stack,net.minecraft.world.entity.player.Player p){return false;}
    @Override public boolean canFitInsideContainerItems(){return false;}
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> out){out.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;@Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new com.coraline.client.renderer.GhostEyeRenderer();return renderer;}});}
}
