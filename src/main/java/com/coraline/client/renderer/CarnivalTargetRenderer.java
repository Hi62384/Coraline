package com.coraline.client.renderer;

import com.coraline.postgame.CarnivalTargetEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Shared fabric atlas, actual entity motion and a clearly highlighted target. */
public final class CarnivalTargetRenderer extends EntityRenderer<CarnivalTargetEntity> {
    private final ModelPart rim,button,holes;
    public CarnivalTargetRenderer(EntityRendererProvider.Context context){super(context);
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("rim",CubeListBuilder.create().texOffs(0,0).addBox(-7,-7,-1,14,14,2),PartPose.ZERO);
        root.addOrReplaceChild("button",CubeListBuilder.create().texOffs(0,0).addBox(-6,-6,-1.2F,12,12,2.4F),PartPose.ZERO);
        var dots=CubeListBuilder.create().texOffs(0,0);for(int x:new int[]{-3,1})for(int y:new int[]{-3,1})dots.addBox(x,y,-1.4F,2,2,2.8F);
        root.addOrReplaceChild("holes",dots,PartPose.ZERO);var baked=LayerDefinition.create(mesh,64,64).bakeRoot();rim=baked.getChild("rim");button=baked.getChild("button");holes=baked.getChild("holes");
    }
    @Override public void render(CarnivalTargetEntity target,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        pose.pushPose();pose.translate(0,.475,0);var sink=buffers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(target)));int glow=target.lit()?15728880:light;
        rim.render(pose,sink,glow,OverlayTexture.NO_OVERLAY,target.lit()?1:.28F,target.lit()?.83F:.28F,target.lit()?.3F:.3F,1);
        float[][] colors={{.9F,.12F,.18F},{.12F,.38F,1},{.15F,.85F,.3F},{.7F,.2F,.9F}};var c=colors[Math.floorMod(target.color(),4)];button.render(pose,sink,glow,OverlayTexture.NO_OVERLAY,c[0],c[1],c[2],1);holes.render(pose,sink,light,OverlayTexture.NO_OVERLAY,.025F,.025F,.035F,1);
        pose.popPose();super.render(target,yaw,partial,pose,buffers,light);
    }
    @Override public ResourceLocation getTextureLocation(CarnivalTargetEntity target){return ResourceLocation.fromNamespaceAndPath("coraline","textures/models/sculpted/fabric.png");}
}
