package com.coraline.client.renderer;

import com.coraline.postgame.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Carved walnut, brass scrollwork and a softly moving silver-violet surface. */
public final class MagicMirrorRenderer implements BlockEntityRenderer<MagicMirrorBlockEntity> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("coraline","textures/models/sculpted/fabric.png");
    private final ModelPart wood,brass,pane,shimmer;
    public MagicMirrorRenderer(BlockEntityRendererProvider.Context context){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("wood",CubeListBuilder.create().texOffs(0,0).addBox(-8,0,-1,2,32,2).addBox(6,0,-1,2,32,2).addBox(-6,0,-1,12,3,2).addBox(-6,29,-1,12,3,2).addBox(-5,-1,-.5F,10,1,1).addBox(-3,-2,-.5F,6,1,1),PartPose.ZERO);
        var trim=CubeListBuilder.create().texOffs(0,0).addBox(-6.2F,3,-1.1F,.5F,26,.5F).addBox(5.7F,3,-1.1F,.5F,26,.5F).addBox(-6,3,-1.1F,12,.5F,.5F).addBox(-6,28.5F,-1.1F,12,.5F,.5F);
        for(int y:new int[]{1,27})for(int sign:new int[]{-1,1}){trim.addBox(sign*6.5F-.75F,y,-1.3F,1.5F,3,.5F).addBox(sign*5F-.6F,y+1,-1.3F,1.2F,1,.5F);}
        root.addOrReplaceChild("brass",trim,PartPose.ZERO);
        root.addOrReplaceChild("pane",CubeListBuilder.create().texOffs(0,0).addBox(-6,3,-.85F,12,26,.25F),PartPose.ZERO);
        root.addOrReplaceChild("shimmer",CubeListBuilder.create().texOffs(0,0).addBox(-5.5F,-.2F,-1.15F,11,.4F,.15F).addBox(-4,-1,-1.1F,8,.25F,.1F),PartPose.ZERO);
        var baked=LayerDefinition.create(mesh,64,64).bakeRoot();wood=baked.getChild("wood");brass=baked.getChild("brass");pane=baked.getChild("pane");shimmer=baked.getChild("shimmer");
    }
    @Override public void render(MagicMirrorBlockEntity mirror,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(.5,2,.5);pose.mulPose(Axis.YP.rotationDegrees(180-mirror.getBlockState().getValue(MagicMirrorBlock.FACING).toYRot()));pose.translate(0,0,7/16.);pose.scale(1,-1,-1);
        var solid=buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));wood.render(pose,solid,light,OverlayTexture.NO_OVERLAY,.23F,.12F,.09F,1);brass.render(pose,solid,light,OverlayTexture.NO_OVERLAY,.75F,.57F,.3F,1);
        float t=(mirror.getLevel().getGameTime()+partial)*.035F,flash=mirror.flash(partial),pulse=.5F+.5F*(float)Math.sin(t);
        pane.render(pose,solid,0xf000f0,OverlayTexture.NO_OVERLAY,.3F+flash*.5F,.27F+pulse*.08F+flash*.4F,.48F+pulse*.1F+flash*.3F,1);
        shimmer.y=5+(float)((Math.sin(t*.4F)+1)*11);shimmer.render(pose,solid,0xf000f0,OverlayTexture.NO_OVERLAY,.7F,.65F,.9F,1);pose.popPose();
    }
}
