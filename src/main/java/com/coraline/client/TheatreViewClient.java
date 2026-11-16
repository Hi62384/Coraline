package com.coraline.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Toggle opera-glass zoom: no held use key and no black spyglass overlay. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class TheatreViewClient {
    private static boolean available,zoom;
    public static final double ZOOM_FOV=.20;
    private static boolean grabbed(){var p=Minecraft.getInstance().player;return p!=null&&p.getVehicle() instanceof com.coraline.feast.CinematicPropEntity prop&&prop.kind()==com.coraline.feast.CinematicPropEntity.STUNT_CARRIER;}
    static void available(boolean value){available=value;if(!value)zoom=false;}
    @SubscribeEvent public static void key(InputEvent.Key e){var mc=Minecraft.getInstance();if(available&&!grabbed()&&mc.screen==null&&e.getKey()==GLFW.GLFW_KEY_V&&e.getAction()==GLFW.GLFW_PRESS)zoom=!zoom;}
    @SubscribeEvent public static void fov(ViewportEvent.ComputeFov e){if(!VisitEventClient.showVenue()||grabbed()){available=false;zoom=false;}if(available&&zoom&&Minecraft.getInstance().screen==null)e.setFOV(e.getFOV()*ZOOM_FOV);}
    private TheatreViewClient(){}
}
