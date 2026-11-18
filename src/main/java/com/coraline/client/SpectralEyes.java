package com.coraline.client;

import com.coraline.feast.*;
import com.coraline.client.renderer.*;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Eye-only color attachment: no extra world render, wall sight or another owner's eyes. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class SpectralEyes {
    private static TextureTarget target;
    private static MultiBufferSource.BufferSource buffers;
    private static boolean ready,failed;
    public static void reset(){if(target!=null)target.destroyBuffers();target=null;ready=failed=false;}
    public static boolean ready(){return ready;}
    public static int texture(){return target==null?-1:target.getColorTextureId();}
    public static void bind(){if(target!=null)target.bindWrite(false);}
    public static boolean owned(CinematicPropEntity eye){var p=Minecraft.getInstance().player;return p!=null&&p.getUUID().equals(eye.owner);}
    public static boolean world(CinematicPropEntity eye,PoseStack pose,MultiBufferSource source){
        if(eye.kind()!=RescueProps.EYE&&eye.kind()!=RescueProps.CONTROL_POINT)return false;
        if(eye.kind()==RescueProps.EYE&&owned(eye)&&!eye.isInvisible()&&eye.cue()>0){pose.pushPose();pose.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());pose.mulPose(Axis.YP.rotationDegrees(180));if(eye.kind()==RescueProps.CONTROL_POINT){var out=source.getBuffer(SpectralEyeRenderType.EYES);GhostEyeRenderer.control(pose,out);}else GhostEyeRenderer.eye(pose,source,eye.slot(),1);pose.popPose();}
        return true;
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void capture(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;ready=false;
        if(!SeeingStoneClient.raised()||failed)return;
        var mc=Minecraft.getInstance();var main=mc.getMainRenderTarget();
        var projection=new org.joml.Matrix4f(RenderSystem.getProjectionMatrix());var sorting=RenderSystem.getVertexSorting();
        try(var saved=new SeeingStoneRenderState()){
            if(target==null)target=new TextureTarget(main.width,main.height,true,Minecraft.ON_OSX);
            else if(target.width!=main.width||target.height!=main.height)target.resize(main.width,main.height,Minecraft.ON_OSX);
            RenderSystem.depthMask(true);target.setClearColor(0,0,0,0);target.clear(Minecraft.ON_OSX);
            // Capture before the dimension's post grade clears/replaces depth.
            target.copyDepthFrom(main);target.bindWrite(true);RenderSystem.setProjectionMatrix(event.getProjectionMatrix(),sorting);
            if(buffers==null)buffers=MultiBufferSource.immediate(new BufferBuilder(4096));
            RenderSystem.setShaderColor(1,1,1,1);RenderSystem.setShaderFogStart(Float.MAX_VALUE);RenderSystem.setShaderFogEnd(Float.MAX_VALUE);
            // Forge AFTER_LEVEL supplies GameRenderer's separate post-level
            // stack, not the rotated stack passed to LevelRenderer. Reconstruct
            // the camera view so silhouettes follow pitch/yaw instead of staying
            // painted at the old horizon when the player looks at the sky.
            var camera=event.getCamera();var origin=camera.getPosition();var pose=new PoseStack();
            RenderSystem.getModelViewStack().setIdentity();RenderSystem.applyModelViewMatrix();
            // Camera.rotation describes its +Z forward vector. Minecraft's
            // OpenGL view looks down -Z, so use LevelRenderer's exact order,
            // including its 180-degree yaw conversion (not inverse quaternion).
            pose.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
            pose.mulPose(Axis.YP.rotationDegrees(camera.getYRot()+180F));
            var out=buffers.getBuffer(SpectralEyeRenderType.EYES);
            for(var eye:mc.level.getEntitiesOfClass(CinematicPropEntity.class,mc.player.getBoundingBox().inflate(64),e->(e.kind()==RescueProps.EYE&&e.slot()<3||e.kind()==RescueProps.CONTROL_POINT)&&!e.isInvisible()&&owned(e))){
                pose.pushPose();pose.translate(eye.getX()-origin.x,eye.getY()-origin.y,eye.getZ()-origin.z);pose.mulPose(camera.rotation());pose.mulPose(Axis.YP.rotationDegrees(180));
                try{float pulse=.5F+.5F*(float)Math.sin((eye.tickCount+event.getPartialTick())*.38F);GhostEyeRenderer.spectral(pose,out,eye.slot(),eye.kind()==RescueProps.CONTROL_POINT,eye.cue()>0,pulse);}finally{pose.popPose();}
            }
            SpectralMobTint.render(mc,pose,buffers,origin,event.getPartialTick());
            buffers.endBatch();ready=true;
        }catch(RuntimeException error){failed=true;org.slf4j.LoggerFactory.getLogger(SpectralEyes.class).error("Spectral eye layer unavailable",error);}
        finally{RenderSystem.setProjectionMatrix(projection,sorting);}
    }
    private SpectralEyes(){}
}
