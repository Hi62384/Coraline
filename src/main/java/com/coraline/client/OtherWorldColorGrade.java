package com.coraline.client;

import com.coraline.config.OtherWorldVisualConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Dedicated dimension-local post pass. Never takes ownership of GameRenderer's other effects. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class OtherWorldColorGrade {
    private static PostChain chain;private static net.minecraft.client.renderer.PostPass colorPass;private static int width,height;private static double strength=-1,whiteout=-1;private static boolean failed;
    public static void reset(){if(chain!=null)chain.close();chain=null;failed=false;strength=whiteout=-1;}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        if(!OtherWorldAtmosphere.active()){if(chain!=null)reset();return;}
        var mc=Minecraft.getInstance();var target=mc.getMainRenderTarget();double desired=OtherWorldVisualConfig.SATURATION.get();
        float fade=OtherWorldWhiteout.amount(event.getPartialTick());
        if((desired==1&&fade<=.001F)||failed)return;
        try{
            if(chain==null){chain=new PostChain(mc.getTextureManager(),mc.getResourceManager(),target,ResourceLocation.fromNamespaceAndPath("coraline","shaders/post/otherworld_color.json"));colorPass=chain.addPass("coraline:otherworld_color",target,chain.getTempTarget("swap"));chain.addPass("blit",chain.getTempTarget("swap"),target);width=height=-1;}
            if(width!=target.width||height!=target.height){width=target.width;height=target.height;chain.resize(width,height);}
            if(strength!=desired){colorPass.getEffect().safeGetUniform("Saturation").set((float)desired);strength=desired;}
            if(whiteout!=fade){colorPass.getEffect().safeGetUniform("Whiteout").set(fade);whiteout=fade;}
            RenderSystem.disableBlend();RenderSystem.disableDepthTest();chain.process(event.getPartialTick());
        }catch(Exception ex){failed=true;org.slf4j.LoggerFactory.getLogger(OtherWorldColorGrade.class).error("Other World color pass disabled after shader error",ex);}
        finally{target.bindWrite(false);RenderSystem.enableDepthTest();RenderSystem.depthMask(true);RenderSystem.defaultBlendFunc();}
    }
}
