package com.coraline.feast;
import com.coraline.bobinsky.BobinskyHouse;
import com.coraline.secrets.Reconciliation;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Owner-only access to an invited house, including a house above a cliff. */
@Mod.EventBusSubscriber(modid="coraline")
public final class TeaVisitTravel {
    @SubscribeEvent public static void register(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("coraline").then(Commands.literal("visitspink")
            .executes(context->travel(context.getSource().getPlayerOrException())))
            .then(Commands.literal("leavespink").executes(context->leave(context.getSource().getPlayerOrException()))));
    }
    static int travel(ServerPlayer player){
        var scene=OtherMotherLoop.scene(player);if(scene==null||!scene.owner.equals(player.getUUID())||!MissingParentsTea.eligible(scene)||!MissingParentsTea.active(scene)||player.serverLevel()!=player.server.overworld()||!player.isAlive())return 0;
        var visit=MissingParentsTea.data(scene);
        if(!visit.getBoolean("Built")||MissingParentsTea.phase(visit)!=MissingParentsTea.Phase.WAITING_FOR_KNOCK||!visit.getBoolean("FloatingLanding"))return 0;
        if(!visit.contains("TravelReturn")&&!BobinskyHouse.bounds(visit).contains(player.position()))visit.putLong("TravelReturn",player.blockPosition().asLong());
        var at=BobinskyHouse.front(visit);player.serverLevel().getChunkAt(at);player.stopRiding();
        player.teleportTo(player.serverLevel(),at.getX()+.5,at.getY()+.05,at.getZ()+.5,0,0);player.fallDistance=0;
        return 1;
    }
    static int leave(ServerPlayer player){
        var scene=OtherMotherLoop.scene(player);if(scene==null||!scene.owner.equals(player.getUUID())||!MissingParentsTea.active(scene)||player.serverLevel()!=player.server.overworld()||!player.isAlive())return 0;
        var visit=MissingParentsTea.data(scene);
        if(MissingParentsTea.phase(visit)!=MissingParentsTea.Phase.WAITING_FOR_EXIT||!visit.getBoolean("ExitUnlocked"))return 0;
        net.minecraft.core.BlockPos target=null;
        if(visit.contains("TravelReturn"))target=Reconciliation.safe(player,net.minecraft.core.BlockPos.of(visit.getLong("TravelReturn")));
        if(target==null){var family=com.coraline.parents.ParentAccess.family(player.server,player.getUUID());if(family!=null&&family.bed!=null&&com.coraline.parents.FamilyData.level(player.server,family.homeDim)==player.serverLevel())target=Reconciliation.safe(player,family.bed);}
        if(target==null){player.sendSystemMessage(Component.literal("A safe return spot is not available yet."));return 0;}
        MissingParentsTea.release(player,visit);
        player.teleportTo(player.serverLevel(),target.getX()+.5,target.getY(),target.getZ()+.5,player.getYRot(),player.getXRot());player.fallDistance=0;
        visit.putBoolean("PlayerExited",true);OtherMotherLoop.changed(player);return 1;
    }
    static void invite(ServerPlayer player){
        player.sendSystemMessage(Component.literal("Visit Miss Spink and Miss Forcible. ").append(
            Component.literal("[Go to the house]").withStyle(style->style.withColor(net.minecraft.ChatFormatting.AQUA)
                .withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/coraline visitspink")))));
    }
    static void inviteReturn(ServerPlayer player){
        player.sendSystemMessage(Component.literal("[Return from the house]").withStyle(style->style.withColor(net.minecraft.ChatFormatting.AQUA)
            .withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/coraline leavespink"))));
    }
    private TeaVisitTravel(){}
}
