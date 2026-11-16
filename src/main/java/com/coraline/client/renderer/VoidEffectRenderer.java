package com.coraline.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/** Visible spell geometry; no terrain edits or persistent luminous blocks. */
public final class VoidEffectRenderer<T extends Entity> extends EntityRenderer<T> {
    private final ModelPart shape;
    private final boolean ground;
    public VoidEffectRenderer(EntityRendererProvider.Context context,boolean ground){super(context);this.ground=ground;var mesh=new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("spell",ground?CubeListBuilder.create().texOffs(0,0).addBox(-19,-.4F,-1,38,.8F,2).addBox(-1,-.4F,-19,2,.8F,38).addBox(-12,-.4F,-12,24,.8F,2):CubeListBuilder.create().texOffs(0,0).addBox(-2,-2,-2,4,4,4).addBox(-4,-.5F,-.5F,8,1,1),PartPose.ZERO);
        shape=LayerDefinition.create(mesh,64,64).bakeRoot();
    }
    @Override public void render(T entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){pose.pushPose();pose.translate(0,ground?.04:0,0);pose.mulPose(Axis.YP.rotationDegrees((entity.tickCount+partial)*(ground?1.5F:12)));if(!ground)pose.mulPose(Axis.XP.rotationDegrees((entity.tickCount+partial)*8));shape.render(pose,buffers.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity))),15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,.82F,.55F,1,ground?.65F:.95F);pose.popPose();super.render(entity,yaw,partial,pose,buffers,light);}
    @Override public ResourceLocation getTextureLocation(T entity){return ResourceLocation.fromNamespaceAndPath("coraline","textures/models/sculpted/fabric.png");}
}
