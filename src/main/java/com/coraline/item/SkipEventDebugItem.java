package com.coraline.item;

import com.coraline.feast.*;
import com.coraline.otherworld.OtherWorld;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Creative-only, scene-bound skip. Never completes rewards or manufactures visit flags. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="coraline")
public final class SkipEventDebugItem extends Item {
    public SkipEventDebugItem(){super(new Properties().stacksTo(1));}
    public static boolean request(ServerPlayer p){
        if(BlackIsTraditional.active(p)){p.displayClientMessage(Component.literal("Use the operator's Black Is Traditional recovery controls for this major storyline."),true);return false;}
        var s=OtherMotherLoop.active(p);if(!p.isCreative()||s==null||!OtherWorld.isOther(p.level())&&!ActivityWorld.isActivity(p.level())){p.displayClientMessage(Component.literal("Creative only: use during an active Other World visit."),true);return false;}
        var tag=p.getPersistentData();tag.putUUID("DebugSkipDoor",s.door);tag.putInt("DebugSkipSerial",s.loop.event.getInt("Serial"));tag.putLong("DebugSkipUntil",OtherMotherLoop.now(p)+2400);retry(p);return true;
    }
    public static boolean retry(ServerPlayer p){
        var tag=p.getPersistentData();if(!tag.hasUUID("DebugSkipDoor"))return false;var s=OtherMotherLoop.active(p);
        if(!p.isCreative()||s==null||!s.door.equals(tag.getUUID("DebugSkipDoor"))||s.loop.event.getInt("Serial")!=tag.getInt("DebugSkipSerial")||OtherMotherLoop.now(p)>tag.getLong("DebugSkipUntil")){clear(p);return false;}
        if(p.tickCount%20!=0)return true;
        if(VisitEvents.skipCurrent(p)){clear(p);p.displayClientMessage(Component.literal("Skipped the current event. Starting the next one; no completion rewards granted."),false);}
        else p.displayClientMessage(Component.literal("Returning scene actors safely before skipping…"),true);
        return true;
    }
    private static void clear(ServerPlayer p){for(String key:new String[]{"DebugSkipDoor","DebugSkipSerial","DebugSkipUntil"})p.getPersistentData().remove(key);}
    private void activate(Player p){if(p instanceof ServerPlayer server){request(server);p.getCooldowns().addCooldown(this,20);}}
    /** Aiming at a portal used to consume the click as portal interaction instead of item use. */
    private static boolean entityClick(Player p,InteractionHand hand){if(!p.isCreative()||hand!=InteractionHand.MAIN_HAND||!(p.getMainHandItem().getItem() instanceof SkipEventDebugItem item))return false;if(p instanceof ServerPlayer&&!p.getCooldowns().isOnCooldown(item))item.activate(p);return true;}
    @net.minecraftforge.eventbus.api.SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST) public static void entity(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract e){if(entityClick(e.getEntity(),e.getHand())){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);}}
    @net.minecraftforge.eventbus.api.SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST) public static void entityAt(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific e){if(entityClick(e.getEntity(),e.getHand())){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);}}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand){activate(p);return InteractionResultHolder.sidedSuccess(p.getItemInHand(hand),level.isClientSide);}
    @Override public InteractionResult useOn(UseOnContext c){if(c.getPlayer()!=null)activate(c.getPlayer());return InteractionResult.sidedSuccess(c.getLevel().isClientSide);}
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<Component> lines,TooltipFlag flag){lines.add(Component.literal("Creative: safely skip the current Other World event."));lines.add(Component.literal("No completion rewards or neighbour-visit credit."));}
}
