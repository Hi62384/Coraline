package com.coraline.client;

import com.coraline.feast.ActivityWorld;
import com.coraline.feast.VisitAudio;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** An escape route even while a performance or falling challenge locks movement. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class ActivityLeaveClient {
    @SubscribeEvent public static void init(ScreenEvent.Init.Post event){
        var mc=Minecraft.getInstance();var screen=event.getScreen();
        if(!(screen instanceof PauseScreen)||mc.level==null||!ActivityWorld.isActivity(mc.level))return;
        event.addListener(Button.builder(Component.literal("Leave activity"),b->{
            mc.setScreen(null);VisitAudio.leaveActivity();
        }).bounds(screen.width-114,8,106,20).build());
    }
    private ActivityLeaveClient(){}
}
