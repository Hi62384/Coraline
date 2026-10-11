package com.coraline.client.clothing;

import com.coraline.clothing.*;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class SpiderInput {
    private static boolean previous;
    @SubscribeEvent public static void input(TickEvent.ClientTickEvent event){
        var p=Minecraft.getInstance().player;
        if(event.phase!=TickEvent.Phase.START||p==null||!SpiderArmor.superSet(p))return;
        boolean jump=p.input.jumping;
        p.getPersistentData().putBoolean("CoralineSpiderJump",jump);p.getPersistentData().putInt("CoralineSpiderInputTick",p.tickCount);
        if(jump!=previous||p.tickCount%10==0){SpiderControls.send(jump);previous=jump;}
    }
}
