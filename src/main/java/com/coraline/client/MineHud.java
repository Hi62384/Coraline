package com.coraline.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class MineHud {
    private static int hearts,lease;
    public static void receive(int value){hearts=Math.max(0,Math.min(5,value));lease=value<0?0:40;}
    public static boolean active(){var mc=Minecraft.getInstance();return lease>0&&mc.player!=null&&com.coraline.feast.ActivityWorld.isActivity(mc.player.level());}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase==TickEvent.Phase.END){if(lease>0)lease--;if(Minecraft.getInstance().player==null)lease=0;}}
    @SubscribeEvent public static void render(RenderGuiEvent.Post e){
        var mc=Minecraft.getInstance();if(!active()||mc.options.hideGui)return;
        int x=mc.getWindow().getGuiScaledWidth()/2-91,y=mc.getWindow().getGuiScaledHeight()-64;
        for(int i=0;i<5;i++)e.getGuiGraphics().drawString(mc.font,"♥",x+i*12,y,i<hearts?0xFFFF5555:0xFF555555,true);
    }
}
