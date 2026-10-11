package com.coraline.feast;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Explicit meter controls until gameplay gain/loss rules are specified. */
@Mod.EventBusSubscriber(modid="coraline")
public final class LoopCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("coraline").then(Commands.literal("relationship").requires(s->s.hasPermission(2))
            .then(Commands.argument("trust",FloatArgumentType.floatArg(0,1)).then(Commands.argument("suspicion",FloatArgumentType.floatArg(0,1)).executes(c->{
                var p=c.getSource().getPlayerOrException();if(OtherMotherLoop.scene(p)==null){p.sendSystemMessage(Component.literal("Visit the Other World first."));return 0;}
                OtherMotherLoop.relationship(p,FloatArgumentType.getFloat(c,"trust"),FloatArgumentType.getFloat(c,"suspicion"));
                p.sendSystemMessage(Component.literal("Other Mother state: "+OtherMotherLoop.scene(p).welcomeState.id()));return 1;
            })))));
    }
}
