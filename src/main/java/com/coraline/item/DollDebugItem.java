package com.coraline.item;

import com.coraline.doll.DollBondData;
import com.coraline.parents.ParentAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class DollDebugItem extends Item {
    public DollDebugItem(){super(new Properties().stacksTo(1));}
    public static String bind(ServerPlayer player){
        if(!player.isCreative())return "Doll Binding Debug is Creative-only.";
        if(ParentAccess.family(player.server,player.getUUID())==null)return "Claim living parents first.";
        var data=DollBondData.get(player.server);
        data.validateFamily(player.getUUID());
        if(data.bonds.containsKey(player.getUUID()))return "You already have a bound doll. No duplicate was created.";
        if(player.getInventory().getFreeSlot()<0)return "Make one empty inventory slot for your doll first.";
        var doll=data.claim(player);
        if(doll.isEmpty())return "Could not bind a doll. Check your parents.";
        var copy=doll.copy();player.getInventory().add(doll);
        data.satisfied(copy,player.server.overworld().getGameTime(),true);
        player.containerMenu.broadcastChanges();
        return "Doll bound and added to your inventory. Place it near your normal respawn bed.";
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(player instanceof ServerPlayer serverPlayer){
            player.displayClientMessage(Component.literal(bind(serverPlayer)),false);
            player.getCooldowns().addCooldown(this,20);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<Component> lines,TooltipFlag flag){
        lines.add(Component.literal("Creative: instantly receive your bound doll, skipping Wybie.").withStyle(net.minecraft.ChatFormatting.AQUA));
        lines.add(Component.literal("Requires claimed parents. Will not duplicate an existing doll.").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
