package com.coraline.item;

import com.coraline.feast.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Two deliberate, owner-bound operator shortcuts; no forged audio ACKs or combat rewards. */
public final class StorySkipDebugItem extends Item {
    public StorySkipDebugItem(){super(new Properties().stacksTo(1));}
    @Override public Component getName(ItemStack stack){return Component.literal("Debug: Story Skips");}
    public static String request(ServerPlayer p,boolean finale){
        if(!p.hasPermissions(2))return "Operator permission required.";
        var s=OtherMotherLoop.scene(p);if(s==null||!p.getUUID().equals(s.owner)||s.stage!=VisitData.Stage.DONE)return "Establish your own Little Door and complete the first family visit first.";
        var root=p.getPersistentData();var pending=root.getCompound("CoralineDebugStorySkip");long now=OtherMotherLoop.now(p);
        if(!pending.hasUUID("Door")||!s.door.equals(pending.getUUID("Door"))||pending.getBoolean("Finale")!=finale||now>pending.getLong("Until")){
            pending=new net.minecraft.nbt.CompoundTag();pending.putUUID("Door",s.door);pending.putBoolean("Finale",finale);pending.putLong("Until",now+100);root.put("CoralineDebugStorySkip",pending);
            return finale?"Confirm: sneak-use again within 5 seconds to skip the final fight (or arm it for later). The manual door lock and reunion remain.":"Confirm: use again without sneaking within 5 seconds to skip the escape, preserving the Overworld Wybie and following cutscenes.";
        }
        root.remove("CoralineDebugStorySkip");
        return finale?BeldamFinale.debugSkipFight(p,s):FirstEscapeDebugItem.activate(p,false);
    }
    private void activate(Player player){if(player instanceof ServerPlayer p&&!p.getCooldowns().isOnCooldown(this)){p.displayClientMessage(Component.literal(request(p,p.isShiftKeyDown())),false);p.getCooldowns().addCooldown(this,10);}}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand){if(hand==InteractionHand.MAIN_HAND)activate(p);return InteractionResultHolder.sidedSuccess(p.getItemInHand(hand),level.isClientSide);}
    @Override public InteractionResult useOn(UseOnContext c){if(c.getHand()==InteractionHand.MAIN_HAND&&c.getPlayer()!=null)activate(c.getPlayer());return InteractionResult.sidedSuccess(c.getLevel().isClientSide);}
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<Component> lines,TooltipFlag flag){lines.add(Component.literal("Operator only. Use twice in air to confirm."));lines.add(Component.literal("Right-click: skip the escape, keeping later cutscenes."));lines.add(Component.literal("Sneak + right-click: skip the boss, or arm the skip for later."));lines.add(Component.literal("No earlier eye quests skipped; original actors preserved."));}
}
