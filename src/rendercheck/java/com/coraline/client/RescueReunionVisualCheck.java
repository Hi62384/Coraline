package com.coraline.client;

import com.coraline.client.renderer.ParentReunionLayer;
import com.coraline.client.clothing.TailoredArmorModel;
import com.coraline.parents.ParentEntity;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.*;

/** Native existing skins/wardrobes plus articulated snow; not reunion scene acceptance. */
public final class RescueReunionVisualCheck {
    private static final class Measure implements VertexConsumer {
        int vertices;double area;final Vector3f[] corners={new Vector3f(),new Vector3f(),new Vector3f(),new Vector3f()};
        @Override public VertexConsumer vertex(double x,double y,double z){if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))throw new AssertionError("Non-finite frost vertex");corners[vertices%4].set((float)x,(float)y,(float)z);vertices++;if(vertices%4==0)area+=new Vector3f(corners[1]).sub(corners[0]).cross(new Vector3f(corners[2]).sub(corners[0])).length();return this;}
        @Override public VertexConsumer color(int r,int g,int b,int a){return this;}@Override public VertexConsumer uv(float u,float v){return this;}@Override public VertexConsumer overlayCoords(int u,int v){return this;}@Override public VertexConsumer uv2(int u,int v){return this;}@Override public VertexConsumer normal(float x,float y,float z){return this;}@Override public void endVertex(){}@Override public void defaultColor(int r,int g,int b,int a){}@Override public void unsetDefaultColor(){}
    }
    public static void capture(Minecraft mc){
        var surface=new TextureTarget(1200,850,true,Minecraft.ON_OSX);var projection=new Matrix4f(RenderSystem.getProjectionMatrix());var sorting=RenderSystem.getVertexSorting();var stack=RenderSystem.getModelViewStack();stack.pushPose();
        try{surface.setClearColor(.12F,.14F,.18F,1);surface.clear(Minecraft.ON_OSX);surface.bindWrite(true);RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,1200,850,0,-1000,1000),VertexSorting.ORTHOGRAPHIC_Z);stack.setIdentity();RenderSystem.applyModelViewMatrix();RenderSystem.enableDepthTest();Lighting.setupFor3DItems();var buffers=MultiBufferSource.immediate(new BufferBuilder(65536));
            for(int row=0;row<2;row++){boolean mom=row==0;String id=(mom?"mom_":"dad_")+"0";var model=new PlayerModel<ParentEntity>(mc.getEntityModels().bakeLayer(mom?ModelLayers.PLAYER_SLIM:ModelLayers.PLAYER),mom);model.young=false;model.hat.visible=false;var clothes=new HumanoidModel<ParentEntity>(TailoredArmorModel.geometry("wardrobe/parents/"+id+".json",null).bakeRoot());model.copyPropertiesTo(clothes);clothes.young=false;clothes.hat.visible=false;
                for(int skin=0;skin<64;skin++){var measured=new Measure();ParentReunionLayer.renderFrost(model,new PoseStack(),type->measured,15728880,1,(mom?"mom_":"dad_")+skin);if(measured.vertices<100||measured.area<=0)throw new AssertionError("Missing authored-wardrobe frost "+skin);}
                var full=new Measure();var half=new Measure();var gone=new Measure();ParentReunionLayer.renderFrost(model,new PoseStack(),type->full,15728880,1,id);ParentReunionLayer.renderFrost(model,new PoseStack(),type->half,15728880,.5F,id);ParentReunionLayer.renderFrost(model,new PoseStack(),type->gone,15728880,0,id);if(half.area>=full.area||half.area<=0||gone.vertices!=0)throw new AssertionError("Snow did not progressively shrink then disappear");
                for(int col=0;col<3;col++){float snow=1-col*.5F;var pose=new PoseStack();pose.translate(200+col*400,110+row*425,0);pose.scale(180,180,-180);pose.mulPose(Axis.YP.rotationDegrees(-18));model.renderToBuffer(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath("coraline","textures/entity/parents/"+id+".png"))),15728880,OverlayTexture.NO_OVERLAY,1,1,1,1);clothes.renderToBuffer(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath("coraline","textures/models/wardrobe/parents/"+id+".png"))),15728880,OverlayTexture.NO_OVERLAY,1,1,1,1);ParentReunionLayer.renderFrost(model,pose,buffers,15728880,snow,id);buffers.endBatch();}
            }
            var folder=java.nio.file.Path.of("../artwork/rescue_rig_qa");java.nio.file.Files.createDirectories(folder);try(var pixels=net.minecraft.client.Screenshot.takeScreenshot(surface)){pixels.writeToFile(folder.resolve("parents_frost_melt.png"));}System.out.println("RESCUE_REUNION_FROST_NATIVE_CAPTURED 128 existing wardrobe profiles; full/half/zero shrinking geometry; actual original skins and wardrobe models; NOT whole reunion scene acceptance");
        }catch(Exception failure){throw new IllegalStateException("Native reunion frost check failed",failure);}finally{surface.destroyBuffers();mc.getMainRenderTarget().bindWrite(true);RenderSystem.setProjectionMatrix(projection,sorting);stack.popPose();RenderSystem.applyModelViewMatrix();RenderSystem.defaultBlendFunc();}
    }
    private RescueReunionVisualCheck(){}
}
