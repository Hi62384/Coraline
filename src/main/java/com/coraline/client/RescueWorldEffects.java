package com.coraline.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Owner-local depth-aware white collapse and actual framebuffer blur, never a HUD color card. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class RescueWorldEffects {
    private static PostChain chain;private static PostPass effect;private static int width,height;private static boolean failed;
    private static final int[][] QUAD={{0,0},{0,1},{1,1},{1,0}};
    private static final ResourceLocation CELESTIAL=ResourceLocation.fromNamespaceAndPath("coraline","textures/sky/celestial_panorama_v1.png");
    private static void reset(){if(chain!=null)chain.close();chain=null;effect=null;failed=false;width=height=0;}
    @SubscribeEvent(priority=EventPriority.HIGH) public static void render(RenderLevelStageEvent event){
        if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_SKY&&RescueClient.celestial()){sky(event,false);return;}
        // Drain terrain before entities are drawn, so mobs and players keep their colors.
        boolean terrainPass=event.getStage()==RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS;
        if(!terrainPass&&event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        // A real, depth-tested surrounding surface also hides distant venue
        // geometry below the pocket while retaining the nearby original ghosts.
        // Draw this before translucent entities. Their color-only render pass
        // cannot protect the golden children from a later depth-tested sky.
        if(terrainPass&&RescueClient.celestial())sky(event,true);
        boolean accepted=!terrainPass&&AcceptedButtonEyesClient.house()&&AcceptedButtonEyesClient.stage()>.001F;
        boolean collapse=!terrainPass&&(accepted||RescueClient.phase().equals("COLLAPSE")||RescueClient.flag("HouseSafe")&&!RescueClient.flag("WebEntered"));float green=!terrainPass&&!accepted&&RescueClient.flag("GreenHouse")&&!RescueClient.flag("WebEntered")?1:0,blur=terrainPass||accepted?0:RescueClient.value("VisionBlur")*8;
        boolean theatreDark=!terrainPass&&java.util.Set.of("THEATRE_SEARCH","WRAPPER_REACH","THEATRE_GRAB","THEATRE_FIGHT","THEATRE_EYE").contains(RescueClient.phase());
        boolean drainWorld=Minecraft.getInstance().level!=null&&com.coraline.otherworld.OtherWorld.isOther(Minecraft.getInstance().level);
        boolean drained=terrainPass&&drainWorld&&(RescueClient.value("DrainRadius0")>0||RescueClient.value("DrainRadius1")>0||RescueClient.value("DrainRadius2")>0);
        if(!accepted&&(!RescueClient.active()||!collapse&&green==0&&blur==0&&!drained&&!theatreDark)){if(!terrainPass&&!drainWorld&&(chain!=null||failed))reset();return;}if(failed)return;
        var mc=Minecraft.getInstance();var target=mc.getMainRenderTarget();var projection=new org.joml.Matrix4f(RenderSystem.getProjectionMatrix());var sorting=RenderSystem.getVertexSorting();
        try{
            if(chain==null){chain=new PostChain(mc.getTextureManager(),mc.getResourceManager(),target,ResourceLocation.fromNamespaceAndPath("coraline","shaders/post/rescue_world.json"));effect=chain.addPass("coraline:rescue_world",target,chain.getTempTarget("swap"));effect.addAuxAsset("DepthSampler",target::getDepthTextureId,target.width,target.height);chain.addPass("blit",chain.getTempTarget("swap"),target);}
            if(width!=target.width||height!=target.height){width=target.width;height=target.height;chain.resize(width,height);}
            var cameraView=new org.joml.Matrix4f().rotationX(event.getCamera().getXRot()*net.minecraft.util.Mth.DEG_TO_RAD).rotateY((event.getCamera().getYRot()+180F)*net.minecraft.util.Mth.DEG_TO_RAD);var inverse=new org.joml.Matrix4f(event.getProjectionMatrix()).mul(cameraView).invert();var camera=event.getCamera().getPosition();var house=BlockPos.of(accepted?AcceptedButtonEyesClient.housePosition():RescueClient.position("House"));var room=BlockPos.of(RescueClient.position("Room"));
            effect.getEffect().safeGetUniform("InverseViewProjection").set(inverse);effect.getEffect().safeGetUniform("CameraHouse").set((float)(camera.x-house.getX()),(float)(camera.y-house.getY()),(float)(camera.z-house.getZ()));effect.getEffect().safeGetUniform("RoomHouse").set((float)(room.getX()-house.getX()),(float)(room.getY()-house.getY()),(float)(room.getZ()-house.getZ()));effect.getEffect().safeGetUniform("Collapse").set(collapse?1F:0F);effect.getEffect().safeGetUniform("Radius").set(accepted?AcceptedButtonEyesClient.radius():RescueClient.value("CollapseRadius"));effect.getEffect().safeGetUniform("GreenRoom").set(green);effect.getEffect().safeGetUniform("BlurRadius").set(blur);
            effect.getEffect().safeGetUniform("AcceptedSky").set(accepted?Math.min(1,AcceptedButtonEyesClient.stage()/5):0);
            effect.getEffect().safeGetUniform("TheatreDark").set(theatreDark?1F:0F);
            for(int i=0;i<3;i++){
                var origin=BlockPos.of(RescueClient.position("DrainOrigin"+i));
                effect.getEffect().safeGetUniform("Drain"+i).set((float)(origin.getX()-house.getX()),(float)(origin.getY()-house.getY()),(float)(origin.getZ()-house.getZ()),!terrainPass||accepted||!drainWorld?0:RescueClient.value("DrainRadius"+i));
            }
            RenderSystem.disableBlend();RenderSystem.disableDepthTest();chain.process(event.getPartialTick());
        }catch(Exception error){failed=true;org.slf4j.LoggerFactory.getLogger(RescueWorldEffects.class).error("Rescue scene post effect unavailable; normal world sight retained",error);}
        finally{target.bindWrite(true);RenderSystem.setProjectionMatrix(projection,sorting);RenderSystem.enableDepthTest();RenderSystem.depthMask(true);RenderSystem.defaultBlendFunc();}
    }
    private static void sky(RenderLevelStageEvent event,boolean foreground){
        var pose=new PoseStack();
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(event.getCamera().getXRot()));
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(event.getCamera().getYRot()+180F));
        renderCelestialSky(pose,Minecraft.getInstance().level.getGameTime()+event.getPartialTick(),foreground);
    }
    /** Perspective environment mesh, shared by the scene and native renderer checks. */
    public static void renderCelestialSky(PoseStack cameraPose,double clock,boolean foreground){
        double radius=foreground?48:140;var pose=cameraPose.last().pose();if(foreground)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();RenderSystem.setShader(GameRenderer::getPositionTexColorShader);RenderSystem.setShaderTexture(0,CELESTIAL);Minecraft.getInstance().getTextureManager().getTexture(CELESTIAL).setFilter(true,false);var builder=Tesselator.getInstance().getBuilder();builder.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
        try{for(int lat=0;lat<36;lat++)for(int lon=0;lon<96;lon++)for(int[] corner:QUAD){double v=(lat+corner[0])/36.,u=(lon+corner[1])/96.,a=-Math.PI/2+v*Math.PI,b=u*Math.PI*2+clock*.00025,x=Math.cos(a)*Math.cos(b),y=Math.sin(a),z=Math.cos(a)*Math.sin(b);builder.vertex(pose,(float)(x*radius),(float)(y*radius),(float)(z*radius)).uv((float)u,(float)(1-v)).color(1F,1F,1F,1F).endVertex();}BufferUploader.drawWithShader(builder.end());}
        finally{RenderSystem.enableCull();RenderSystem.enableDepthTest();RenderSystem.depthMask(true);RenderSystem.defaultBlendFunc();}
    }
    private RescueWorldEffects(){}
}
