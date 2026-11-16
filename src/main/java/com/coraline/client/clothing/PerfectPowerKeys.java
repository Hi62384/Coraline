package com.coraline.client.clothing;

import net.minecraft.client.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class PerfectPowerKeys {
    private static final KeyMapping BARRIER=new KeyMapping("key.coraline.perfect_barrier",org.lwjgl.glfw.GLFW.GLFW_KEY_V,"key.categories.coraline");
    @Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {@SubscribeEvent public static void keys(RegisterKeyMappingsEvent event){event.register(BARRIER);}}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(event.phase!=TickEvent.Phase.END)return;while(BARRIER.consumeClick()){var mc=Minecraft.getInstance();if(mc.player!=null&&mc.screen==null)com.coraline.clothing.PerfectAbilityNetwork.barrier();}}
    private PerfectPowerKeys(){}
}
