package com.coraline.client;

import com.coraline.parents.ParentEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class ParentClaimHint {
    @SubscribeEvent public static void render(RenderGuiEvent.Post event){
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.screen!=null||mc.options.hideGui||!(mc.hitResult instanceof EntityHitResult hit)
                ||!(hit.getEntity() instanceof ParentEntity parent)||parent.isOtherParent()||parent.isClaimed()||!parent.isAlive())return;
        var text=Component.literal("Right-click to claim Mom and Dad as your parents");
        int x=(mc.getWindow().getGuiScaledWidth()-mc.font.width(text))/2;
        int y=mc.getWindow().getGuiScaledHeight()/2+18;
        event.getGuiGraphics().fill(x-4,y-3,x+mc.font.width(text)+4,y+12,0xA0000000);
        event.getGuiGraphics().drawString(mc.font,text,x,y,0xFFF1CF,true);
    }
}
