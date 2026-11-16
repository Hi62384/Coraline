package com.coraline.postgame;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;

/** Only designated structure caches, never arbitrary Minecraft/modded locks. */
public final class OtherIronLockpick extends Item {
    public OtherIronLockpick(){super(new Properties().durability(96));}
    @Override public InteractionResult useOn(UseOnContext context){
        if(!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof SiteLockTarget cache)||!cache.locked())return InteractionResult.PASS;
        if(context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player)SiteLockpicking.open(player,context.getClickedPos(),context.getHand());
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
    @Override public boolean isValidRepairItem(ItemStack tool,ItemStack material){return material.is(com.coraline.clothing.OtherMaterials.OTHER_IRON.get())||super.isValidRepairItem(tool,material);}
    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.Level level,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flags){lines.add(net.minecraft.network.chat.Component.translatable("tooltip.coraline.other_iron_lockpick"));}
}
