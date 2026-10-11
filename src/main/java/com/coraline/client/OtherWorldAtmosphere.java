package com.coraline.client;

import com.coraline.config.OtherWorldVisualConfig;
import com.coraline.otherworld.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class OtherWorldAtmosphere {
    static boolean active(){var mc=Minecraft.getInstance();return mc.level!=null&&OtherWorld.isOther(mc.level)&&OtherWorldVisualConfig.ENABLED.get();}
    private static boolean obscured(){var p=Minecraft.getInstance().player;return p!=null&&(p.hasEffect(MobEffects.BLINDNESS)||p.hasEffect(MobEffects.DARKNESS));}
    @SubscribeEvent public static void color(ViewportEvent.ComputeFogColor event){
        if(!active()||obscured())return;var fluid=event.getCamera().getFluidInCamera();
        if(fluid!=FogType.NONE&&fluid!=FogType.WATER)return;
        int c=(fluid==FogType.WATER?OtherWorldVisualConfig.WATER_FOG:OtherWorldVisualConfig.FOG).get();
        var mc=Minecraft.getInstance();float exposure=mc.level.getBrightness(net.minecraft.world.level.LightLayer.SKY,event.getCamera().getBlockPosition())/15F;
        float strength=fluid==FogType.WATER?1:.28F+.72F*exposure,white=OtherWorldWhiteout.amount(0);
        event.setRed(net.minecraft.util.Mth.lerp(white,OtherWorldVisualConfig.red(c)*strength,1));event.setGreen(net.minecraft.util.Mth.lerp(white,OtherWorldVisualConfig.green(c)*strength,1));event.setBlue(net.minecraft.util.Mth.lerp(white,OtherWorldVisualConfig.blue(c)*strength,1));
    }
    @SubscribeEvent public static void fog(ViewportEvent.RenderFog event){
        if(!active()||obscured())return;
        if(event.getType()==FogType.WATER){event.setNearPlaneDistance(0);event.setFarPlaneDistance(OtherWorldVisualConfig.WATER_DISTANCE.get());event.setCanceled(true);return;}
        if(event.getType()!=FogType.NONE||event.getMode()!=FogRenderer.FogMode.FOG_TERRAIN)return;
        var view=RealmVisualState.client;if(view!=null&&view.tunnel())return;
        float end=Math.min(event.getFarPlaneDistance(),OtherWorldVisualConfig.FOG_END.get());
        if(view!=null){
            var cam=event.getCamera().getPosition();double distance=Math.hypot(cam.x-view.center().getX(),cam.z-view.center().getZ());
            end=Math.min(end,Math.max(40,(float)(view.radius()-distance+32)));
        }
        end=net.minecraft.util.Mth.lerp(OtherWorldWhiteout.amount(0),end,8);
        event.setFarPlaneDistance(end);event.setNearPlaneDistance(end*OtherWorldVisualConfig.FOG_START.get().floatValue());event.setCanceled(true);
    }
}
