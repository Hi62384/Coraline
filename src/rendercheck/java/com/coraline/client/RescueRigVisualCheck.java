package com.coraline.client;

import com.coraline.client.renderer.CharacterModel;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import java.util.*;

/** Real Minecraft resource/model/buffer path, without loading a world. Not game-camera acceptance. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class RescueRigVisualCheck {
    private static boolean done;
    private static final int LIGHT=15728880,OVERLAY=OverlayTexture.NO_OVERLAY;
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/white_concrete.png"),ATLAS=ResourceLocation.fromNamespaceAndPath("coraline","characters/cast_atlas.png");
    private static final class Capture implements VertexConsumer {
        final List<float[]> points=new ArrayList<>();double minX=Double.POSITIVE_INFINITY,minY=minX,maxX=Double.NEGATIVE_INFINITY,maxY=maxX;
        @Override public VertexConsumer vertex(double x,double y,double z){if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))throw new AssertionError("Non-finite rig vertex");points.add(new float[]{(float)x,(float)y,(float)z});minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);return this;}
        @Override public VertexConsumer color(int r,int g,int b,int a){return this;}
        @Override public VertexConsumer uv(float u,float v){return this;}
        @Override public VertexConsumer overlayCoords(int u,int v){return this;}
        @Override public VertexConsumer uv2(int u,int v){return this;}
        @Override public VertexConsumer normal(float x,float y,float z){return this;}
        @Override public void endVertex(){}
        @Override public void defaultColor(int r,int g,int b,int a){}
        @Override public void unsetDefaultColor(){}
    }
    private static void model(CharacterModel source,CharacterModel target,float u,PoseStack pose,VertexConsumer out){if(u==0)source.renderSpeech(pose,out,LIGHT,OVERLAY,1,1,1,1,0);else if(u==1)target.renderSpeech(pose,out,LIGHT,OVERLAY,1,1,1,1,0);else target.renderRigTransition(source,pose,out,LIGHT,OVERLAY,u);}
    @SubscribeEvent public static void render(ScreenEvent.Render.Post event){
        if(!Boolean.getBoolean("coraline.rescueRigVisualCheck")||done)return;var mc=Minecraft.getInstance();if(mc.getOverlay()!=null)return;done=true;
        var source=new CharacterModel("evil_other_mother");var target=new CharacterModel("beldam");source.storyBlind(true);target.storyBlind(true);
        var neutral=new PoseStack();var end=new Capture();var nativeEnd=new Capture();target.renderRigTransition(source,neutral,end,LIGHT,OVERLAY,1);target.renderSpeech(neutral,nativeEnd,LIGHT,OVERLAY,1,1,1,1,0);
        if(end.points.size()!=nativeEnd.points.size()||end.points.isEmpty())throw new AssertionError("Morph endpoint changed topology");double error=0;for(int i=0;i<end.points.size();i++)for(int a=0;a<3;a++)error=Math.max(error,Math.abs(end.points.get(i)[a]-nativeEnd.points.get(i)[a]));if(error>.00001)throw new AssertionError("Morph endpoint differs from the actual Beldam: "+error);
        source.storyBlind(false);var intact=new Capture();source.renderSpeech(neutral,intact,LIGHT,OVERLAY,1,1,1,1,0);source.storyBlind(true);var blind=new Capture();source.renderSpeech(neutral,blind,LIGHT,OVERLAY,1,1,1,1,0);if(blind.points.size()>=intact.points.size())throw new AssertionError("Button meshes remained after the Cat attack");
        float[] phases={0,.25F,.5F,.75F,1};var rotated=new PoseStack();rotated.mulPose(Axis.YP.rotationDegrees(-18));var whole=new Capture();for(float phase:phases)model(source,target,phase,rotated,whole);double scale=Math.min(292/(whole.maxX-whole.minX),650/(whole.maxY-whole.minY));
        var surface=new TextureTarget(1600,1000,true,Minecraft.ON_OSX);var projection=new Matrix4f(RenderSystem.getProjectionMatrix());var sorting=RenderSystem.getVertexSorting();var modelView=RenderSystem.getModelViewStack();modelView.pushPose();
        try{surface.setClearColor(.12F,.14F,.18F,1);surface.clear(Minecraft.ON_OSX);surface.bindWrite(true);RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,1600,1000,0,-1000,1000),VertexSorting.ORTHOGRAPHIC_Z);modelView.setIdentity();RenderSystem.applyModelViewMatrix();RenderSystem.enableDepthTest();Lighting.setupFor3DItems();var buffers=MultiBufferSource.immediate(new BufferBuilder(65536));
            for(int i=0;i<phases.length;i++){var pose=new PoseStack();pose.translate(160+i*320-scale*(whole.minX+whole.maxX)/2,30-scale*whole.minY,0);pose.scale((float)scale,(float)scale,(float)-scale);pose.mulPose(Axis.YP.rotationDegrees(-18));model(source,target,phases[i],pose,buffers.getBuffer(RenderType.entityCutoutNoCull(phases[i]>0&&phases[i]<1?WHITE:ATLAS)));}buffers.endBatch();
            source.storyHeadOnly(true);target.storyHeadOnly(true);for(int i=0;i<3;i++){var face=i<2?source:target;source.storyBlind(i!=0);var bounds=new Capture();face.renderSpeech(new PoseStack(),bounds,LIGHT,OVERLAY,1,1,1,1,0);double zoom=Math.min(430/(bounds.maxX-bounds.minX),260/(bounds.maxY-bounds.minY));var pose=new PoseStack();pose.translate(266+i*533-zoom*(bounds.minX+bounds.maxX)/2,720-zoom*bounds.minY,0);pose.scale((float)zoom,(float)zoom,(float)-zoom);face.renderSpeech(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(ATLAS)),LIGHT,OVERLAY,1,1,1,1,0);}buffers.endBatch();
            var folder=java.nio.file.Path.of("../artwork/rescue_rig_qa");java.nio.file.Files.createDirectories(folder);try(var pixels=net.minecraft.client.Screenshot.takeScreenshot(surface)){int foreground=0;for(int y=0;y<pixels.getHeight();y+=2)for(int x=0;x<pixels.getWidth();x+=2){int value=pixels.getPixelRGBA(x,y);if((value&255)>80||(value>>8&255)>80||(value>>16&255)>80)foreground++;}if(foreground<2500)throw new AssertionError("Native rig image is blank");pixels.writeToFile(folder.resolve("beldam_transition.png"));}
            java.nio.file.Files.writeString(folder.resolve("readme.txt"),"Top row: blinded evil mother, 25%, 50%, 75%, native Beldam. Shared camera scale. Bottom row: intact buttons, ripped-out buttons, blinded Beldam.\nNative endpoint max vertex error="+error+"; intact vertices="+intact.points.size()+"; blinded vertices="+blind.points.size()+".\nThis is an isolated real-renderer preview, NOT game-camera, animation, audio or FPS acceptance.\n");
            System.out.println("RESCUE_RIG_NATIVE_CAPTURED endpoint_error="+error+" vertices="+end.points.size()+"; not game-camera acceptance");
        }catch(Exception failure){throw new IllegalStateException("Rescue rig native capture failed",failure);}finally{surface.destroyBuffers();mc.getMainRenderTarget().bindWrite(true);RenderSystem.setProjectionMatrix(projection,sorting);modelView.popPose();RenderSystem.applyModelViewMatrix();RenderSystem.enableDepthTest();RenderSystem.defaultBlendFunc();}
        com.coraline.client.renderer.FirstEscapeGeometryCheck.verify();handPoses(mc);RescueReunionVisualCheck.capture(mc);skyDirections(mc);mc.stop();
    }
    private static void handPoses(Minecraft mc){
        var surface=new TextureTarget(1200,1000,true,Minecraft.ON_OSX);var projection=new Matrix4f(RenderSystem.getProjectionMatrix());var sorting=RenderSystem.getVertexSorting();var stack=RenderSystem.getModelViewStack();stack.pushPose();double separation=Double.POSITIVE_INFINITY;
        try{surface.setClearColor(.12F,.14F,.18F,1);surface.clear(Minecraft.ON_OSX);surface.bindWrite(true);RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,1200,1000,0,-1000,1000),VertexSorting.ORTHOGRAPHIC_Z);stack.setIdentity();RenderSystem.applyModelViewMatrix();RenderSystem.enableDepthTest();Lighting.setupFor3DItems();var buffers=MultiBufferSource.immediate(new BufferBuilder(65536));
            for(int row=0;row<2;row++)for(int col=0;col<2;col++){var model=new CharacterModel(row==0?"evil_other_mother":"beldam");int gesture=66+col;
                for(int tick=0;tick<=200;tick++){model.rescuePose(gesture,tick);var rightPose=new PoseStack();model.transformBone("right_forearm",rightPose);var leftPose=new PoseStack();model.transformBone("left_forearm",leftPose);var right=rightPose.last().pose().transformPosition(new org.joml.Vector3f(0,.56F,0));var left=leftPose.last().pose().transformPosition(new org.joml.Vector3f(0,.56F,0));if(right.x>=-.15F||left.x<=.15F)throw new AssertionError("Conversational/threat pose crosses its wrists: "+gesture+" "+right+" / "+left);separation=Math.min(separation,left.x-right.x);}
                var bounds=new Capture();model.renderSpeech(new PoseStack(),bounds,LIGHT,OVERLAY,1,1,1,1,0);double zoom=Math.min(520/(bounds.maxX-bounds.minX),440/(bounds.maxY-bounds.minY));var pose=new PoseStack();pose.translate(300+col*600-zoom*(bounds.minX+bounds.maxX)/2,25+row*500-zoom*bounds.minY,0);pose.scale((float)zoom,(float)zoom,(float)-zoom);model.renderSpeech(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(ATLAS)),LIGHT,OVERLAY,1,1,1,1,0);
            }buffers.endBatch();try(var pixels=net.minecraft.client.Screenshot.takeScreenshot(surface)){pixels.writeToFile(java.nio.file.Path.of("../artwork/rescue_rig_qa/mother_hand_poses.png"));}System.out.println("RESCUE_HAND_POSES_NATIVE_CAPTURED both rigs gestures 66/67; 804 wrist-side checks; minimum separation="+separation);
        }catch(Exception failure){throw new IllegalStateException("Native hand pose check failed",failure);}finally{surface.destroyBuffers();mc.getMainRenderTarget().bindWrite(true);RenderSystem.setProjectionMatrix(projection,sorting);stack.popPose();RenderSystem.applyModelViewMatrix();RenderSystem.defaultBlendFunc();}
    }
    private static void skyDirections(Minecraft mc){
        var surface=new TextureTarget(960,480,true,Minecraft.ON_OSX);var projection=new Matrix4f(RenderSystem.getProjectionMatrix());var sorting=RenderSystem.getVertexSorting();var stack=RenderSystem.getModelViewStack();stack.pushPose();
        try{surface.setClearColor(0,0,0,1);surface.clear(Minecraft.ON_OSX);surface.bindWrite(true);stack.setIdentity();RenderSystem.applyModelViewMatrix();RenderSystem.setShaderColor(1,1,1,1);RenderSystem.enableDepthTest();RenderSystem.depthFunc(515);RenderSystem.depthMask(true);
            for(int index=0;index<6;index++){int x=index%3*320,y=(1-index/3)*240;RenderSystem.viewport(x,y,320,240);RenderSystem.setProjectionMatrix(new Matrix4f().perspective((float)Math.toRadians(90),320/240F,.05F,300),VertexSorting.ORTHOGRAPHIC_Z);var camera=new PoseStack();if(index<4)camera.mulPose(Axis.YP.rotationDegrees(index*90));else camera.mulPose(Axis.XP.rotationDegrees(index==4?90:-90));
                if(index==0){RenderSystem.disableCull();RenderSystem.setShader(GameRenderer::getPositionColorShader);var geometry=Tesselator.getInstance().getBuilder();geometry.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);for(float[] point:new float[][]{{-50,-50},{50,-50},{50,50},{-50,50}})geometry.vertex(point[0],point[1],-70).color(1F,0F,0F,1F).endVertex();for(float[] point:new float[][]{{-.5F,-.5F},{.5F,-.5F},{.5F,.5F},{-.5F,.5F}})geometry.vertex(point[0],point[1],-4).color(.95F,.70F,.15F,1F).endVertex();BufferUploader.drawWithShader(geometry.end());}
                RescueWorldEffects.renderCelestialSky(camera,0,true);RenderSystem.depthMask(true);
            }
            try(var pixels=net.minecraft.client.Screenshot.takeScreenshot(surface)){int center=pixels.getPixelRGBA(160,120),outside=pixels.getPixelRGBA(250,120);if((center&255)<180||(center>>8&255)<100||(center>>16&255)>100)throw new AssertionError("Celestial sphere covered the nearby golden foreground");if((outside&255)>180&&(outside>>8&255)<40)throw new AssertionError("Distant venue geometry leaked through the enclosing environment");pixels.writeToFile(java.nio.file.Path.of("../artwork/rescue_rig_qa/celestial_sky_directions.png"));}
            System.out.println("CELESTIAL_SKY_NATIVE_CAPTURED six perspective directions; near foreground retained; far venue hidden; NOT complete vision acceptance");
        }catch(Exception failure){throw new IllegalStateException("Native celestial sky check failed",failure);}finally{surface.destroyBuffers();mc.getMainRenderTarget().bindWrite(true);RenderSystem.setProjectionMatrix(projection,sorting);stack.popPose();RenderSystem.applyModelViewMatrix();RenderSystem.depthMask(true);RenderSystem.enableCull();RenderSystem.enableDepthTest();RenderSystem.defaultBlendFunc();}
    }
    private RescueRigVisualCheck(){}
}
