package com.coraline.client.renderer;
import com.coraline.tycoon.TycoonDollEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
public final class TycoonDollRenderer extends EntityRenderer<TycoonDollEntity>{
    public TycoonDollRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=.12F;}
    @Override public ResourceLocation getTextureLocation(TycoonDollEntity e){return ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/white_wool.png");}
    @Override public void render(TycoonDollEntity e,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){pose.pushPose();pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180-yaw));pose.scale(.4F,.4F,.4F);TycoonDollVisual.render(e.appearance(),pose,buffers,light);pose.popPose();super.render(e,yaw,partial,pose,buffers,light);}
}
