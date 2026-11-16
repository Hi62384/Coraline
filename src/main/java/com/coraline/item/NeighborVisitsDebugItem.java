package com.coraline.item;

import com.coraline.feast.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Explicit Creative shortcut: progression only, no rewards or relationship changes. */
public final class NeighborVisitsDebugItem extends Item {
    public NeighborVisitsDebugItem(){super(new Properties().stacksTo(1));}
    public static String complete(ServerPlayer p){
        if(!p.isCreative())return "Neighbor Visits Complete Debug is Creative-only.";
        var s=OtherMotherLoop.scene(p);
        if(s==null||!p.getUUID().equals(s.owner)||s.stage!=VisitData.Stage.DONE)return "Complete the first family visit first.";
        var neighborData=com.coraline.bobinsky.BobinskyVisitData.get(p.server);
        var bobinsky=neighborData.visits.get(p.getUUID());if(bobinsky!=null&&bobinsky.getBoolean("Active"))com.coraline.bobinsky.BobinskyVisit.cancel(p.server,bobinsky,p);
        var tea=neighborData.teaVisits.get(p.getUUID());if(tea!=null&&tea.getBoolean("Active"))com.coraline.tea.TeaVisit.cancel(p.server,tea,p);
        if(s.wybieActor!=null){for(var level:p.server.getAllLevels()){var actor=level.getEntity(s.wybieActor);if(actor!=null)actor.discard();}s.wybieActor=null;com.coraline.feast.VisitAudio.stopMusic(p,"wybie_visit_music");}
        s.wybieVisited=true;s.bobinskyVisited=true;s.teaVisited=true;
        s.loop.story.putBoolean("WybieIntroductionComplete",true);
        s.loop.story.putBoolean("BobinskyIntroductionComplete",true);
        s.loop.story.putBoolean("TeaIntroductionComplete",true);
        s.neighborVisitsReady=true;s.nextNeighborAttempt=Long.MAX_VALUE;
        s.loop.story.putBoolean("NeighborVisitsDebugComplete",true);
        OtherMotherLoop.changed(p);VisitData.get(p.server).setDirty();
        return "Completed Wybie, Bobinsky, and Spink & Forcible's Overworld visits. No rewards granted; existing Wybie bans are unchanged.";
    }
    private void use(Player player){if(player instanceof ServerPlayer p){p.displayClientMessage(Component.literal(complete(p)),false);p.getCooldowns().addCooldown(this,20);}}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand){use(p);return InteractionResultHolder.sidedSuccess(p.getItemInHand(hand),level.isClientSide);}
    @Override public InteractionResult useOn(UseOnContext c){if(c.getPlayer()!=null)use(c.getPlayer());return InteractionResult.sidedSuccess(c.getLevel().isClientSide);}
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<Component> lines,TooltipFlag flag){lines.add(Component.literal("Creative: complete all three Overworld neighbor visits.").withStyle(net.minecraft.ChatFormatting.GREEN));lines.add(Component.literal("No visit rewards. Does not undo a Wybie ban."));}
}
