package com.coraline.item;

import com.coraline.entity.WybieEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class DowsingRodItem extends Item {
    @Override public void appendHoverText(ItemStack stack, @org.jetbrains.annotations.Nullable Level level,
            java.util.List<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        boolean parents=com.coraline.parents.ParentStatus.clientHasParents;
        lines.add(net.minecraft.network.chat.Component.literal(parents
                ?"Right-click to find the magic well."
                :"Requires parents to use.")
                .withStyle(parents?net.minecraft.ChatFormatting.GREEN:net.minecraft.ChatFormatting.YELLOW));
    }
    public DowsingRodItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if(player instanceof ServerPlayer sp && com.coraline.parents.ParentAccess.family(sp.server,sp.getUUID())==null){
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Parents are needed to use this item. Claimable parents should be out there somewhere...").withStyle(net.minecraft.ChatFormatting.YELLOW),true);
            return InteractionResultHolder.fail(stack);
        }

        if(player instanceof ServerPlayer serverPlayer && com.coraline.doll.DollBondData.get(serverPlayer.server).bonds.containsKey(player.getUUID())){
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("The doll has already found you."),true);
            return InteractionResultHolder.fail(stack);
        }

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (!WybieEntity.spawnBehind(serverPlayer)) {
                return InteractionResultHolder.fail(stack);
            }
            player.getCooldowns().addCooldown(this, com.coraline.config.CoralineConfig.ROD_COOLDOWN.get());
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
