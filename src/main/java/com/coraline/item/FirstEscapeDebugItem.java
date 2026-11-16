package com.coraline.item;

import com.coraline.feast.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;

/** Owner-only operator milestone shortcut, never a reward generator. */
public final class FirstEscapeDebugItem extends Item {
    public FirstEscapeDebugItem(){super(new Properties().stacksTo(1));}
    @Override public Component getName(ItemStack stack){return Component.literal("Debug: Complete First Escape");}
    public static String activate(ServerPlayer p,boolean force){
        if(!p.hasPermissions(2))return "Operator permission required.";
        var s=OtherMotherLoop.scene(p);if(s==null||s.stage!=VisitData.Stage.DONE||!p.getUUID().equals(s.owner))return "Finish the first family visit and establish your Little Door first.";
        if(!force)return FirstEscapeStory.skipToAftermath(p,s);
        if(!FirstEscapeStory.finishFirstEscape(p,s,true))return "Cannot finish safely: the original owned Overworld door or event cleanup is unavailable.";
        MissingParentsTea.select(p,s,true);return "First Escape completed. Spink and Forcible visit preparation requested.";
    }
    private void activate(Player player){if(player instanceof ServerPlayer p&&!p.getCooldowns().isOnCooldown(this)){p.displayClientMessage(Component.literal(activate(p,p.isShiftKeyDown())),false);p.getCooldowns().addCooldown(this,20);}}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand){activate(p);return InteractionResultHolder.sidedSuccess(p.getItemInHand(hand),level.isClientSide);}
    @Override public InteractionResult useOn(UseOnContext c){if(c.getPlayer()!=null)activate(c.getPlayer());return InteractionResult.sidedSuccess(c.getLevel().isClientSide);}
    @Override public void appendHoverText(ItemStack s,Level l,java.util.List<Component> out,TooltipFlag f){out.add(Component.literal("Operator: skip the escape; keep Wybie's next cutscenes."));out.add(Component.literal("Sneak-use: bypass Wybie and force the Spink / Forcible visit."));out.add(Component.literal("No chase rewards. Original story actors are preserved."));}
}
