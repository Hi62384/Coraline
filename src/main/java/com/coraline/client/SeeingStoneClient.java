package com.coraline.client;

import com.coraline.client.renderer.SeeingStoneRenderer;
import com.coraline.registry.ModItems;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/** One framebuffer pass, only through the model's real hole; no second world render. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class SeeingStoneClient {
    private static PostChain chain;private static net.minecraft.client.renderer.PostPass mask;private static int width,height;private static boolean failed;
    public static boolean raised(){var mc=Minecraft.getInstance();return mc.player!=null&&mc.level!=null&&mc.options.getCameraType().isFirstPerson()&&mc.player.isUsingItem()&&mc.player.getUseItem().is(ModItems.SEEING_STONE.get());}
    private static float lift(float partial){var p=Minecraft.getInstance().player;return p==null?0:Math.min(1,(p.getTicksUsingItem()+partial)/5F);}
    public static void reset(){if(chain!=null)chain.close();chain=null;mask=null;width=height=0;failed=false;SpectralEyes.reset();}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase==TickEvent.Phase.END&&!raised()&&(chain!=null||failed))reset();
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void hand(RenderHandEvent event){
        if(!raised())return;var mc=Minecraft.getInstance();if(event.getHand()!=mc.player.getUsedItemHand())return;event.setCanceled(true);
        var pose=event.getPoseStack();pose.pushPose();
        try{
            pose.translate(0,-.12*(1-lift(event.getPartialTick())),-.42);pose.scale(.5F,.5F,.5F);
            // The actual hand projection/pose includes bob, tilt and current FOV.
            // A nominal world FOV produces a mask larger than the real opening.
            var projection=new Matrix4f(RenderSystem.getProjectionMatrix());
            var stoneToScreen=new Matrix4f(projection).mul(pose.last().pose());
            if(Math.abs(stoneToScreen.determinant())>1e-8F)
                filter(new Matrix4f(stoneToScreen).invert(),projection,event.getPartialTick());
            SeeingStoneRenderer.stone(pose,event.getMultiBufferSource(),event.getPackedLight());
            var arm=event.getHand()==net.minecraft.world.InteractionHand.MAIN_HAND?mc.player.getMainArm():mc.player.getMainArm().getOpposite();
            SeeingStoneRenderer.grasp(mc.player,arm,pose,event.getMultiBufferSource(),event.getPackedLight());
        }finally{pose.popPose();}
    }
    private static void filter(Matrix4f screenToStone,Matrix4f projection,float partial){
        if(failed)return;var mc=Minecraft.getInstance();var target=mc.getMainRenderTarget();var sorting=RenderSystem.getVertexSorting();
        try(var saved=new SeeingStoneRenderState()){
            if(chain==null){chain=new PostChain(mc.getTextureManager(),mc.getResourceManager(),target,ResourceLocation.fromNamespaceAndPath("coraline","shaders/post/seeing_stone.json"));mask=chain.addPass("coraline:seeing_stone",target,chain.getTempTarget("swap"));mask.addAuxAsset("SpectralSampler",SpectralEyes::texture,target.width,target.height);width=height=0;}
            if(width!=target.width||height!=target.height){width=target.width;height=target.height;chain.resize(width,height);}
            mask.getEffect().safeGetUniform("ScreenToStone").set(screenToStone);
            mask.getEffect().safeGetUniform("EyesEnabled").set(SpectralEyes.ready()?1F:0F);
            RenderSystem.disableBlend();RenderSystem.disableDepthTest();chain.process(partial);
            // A PostPass targeting the main framebuffer calls setClearColor(0)
            // and clears its depth. That persists beyond OpenGL state restore,
            // turning the next frame's unpainted horizon black. Copy COLOR only;
            // leave the world's depth and persistent sky clear color untouched.
            SeeingStoneFramebuffer.copyColor(chain.getTempTarget("swap"),target);
        }catch(Exception error){failed=true;org.slf4j.LoggerFactory.getLogger(SeeingStoneClient.class).error("Seeing Stone mask unavailable; normal sight retained",error);}
        finally{RenderSystem.setProjectionMatrix(projection,sorting);}
    }
    private SeeingStoneClient(){}
}
