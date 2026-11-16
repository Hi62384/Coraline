package com.coraline.postgame;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Explicit warning choices only. Learning and encounter consent are separate; no reset runs from a command. */
@Mod.EventBusSubscriber(modid="coraline")
public final class FinalResearchCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event){var root=Commands.literal("coraline");var research=Commands.literal("final_research");for(boolean how:new boolean[]{true,false})research.then(Commands.literal(how?"how":"later").then(Commands.argument("nonce",UuidArgument.uuid()).executes(context->{var player=context.getSource().getPlayerOrException();return PostgameQuests.decide(player,UuidArgument.getUuid(context,"nonce"),how)?1:0;})));root.then(research);root.then(Commands.literal("final_ending").executes(context->TrueHeartBattle.request(context.getSource().getPlayerOrException())).then(Commands.literal("confirm").then(Commands.argument("nonce",UuidArgument.uuid()).executes(context->TrueHeartBattle.confirm(context.getSource().getPlayerOrException(),UuidArgument.getUuid(context,"nonce"))))).then(Commands.literal("leave").executes(context->TrueHeartBattle.leave(context.getSource().getPlayerOrException()))));event.getDispatcher().register(root);}
    private FinalResearchCommands(){}
}
