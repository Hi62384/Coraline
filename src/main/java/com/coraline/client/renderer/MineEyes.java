package com.coraline.client.renderer;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.*;

/** Mine actors use lit, head-attached buttons, not fullbright camera-facing item icons. */
public final class MineEyes<T extends LivingEntity,M extends EntityModel<T>> extends RenderLayer<T,M> {
    private static final ResourceLocation ART=ResourceLocation.fromNamespaceAndPath("coraline","textures/entity/doll_button_eyes.png");
    public MineEyes(RenderLayerParent<T,M> parent){super(parent);}
    @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,T entity,float limb,float amount,float partial,float age,float yaw,float pitch){
        if(entity.getCustomName()==null||!entity.getCustomName().getString().startsWith("Button-Eyed "))return;
        ModelPart head=getParentModel() instanceof HeadedModel h?h.getHead():getParentModel() instanceof HierarchicalModel<?> h?h.root().getChild("head"):null;
        if(head==null)return;
        pose.pushPose();head.translateAndRotate(pose);
        boolean spider=entity instanceof Spider;float y=spider?-.5F:-3.5F,z=spider?-8.025F:-4.025F,r=spider?1.7F:1.45F;
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(ART));
        for(int side=0;side<2;side++){
            float x=side==0?-2:2,u0=(side==0?2F:17F)/32F,u1=(side==0?15F:30F)/32F;
            v(out,pose,x-r,y-r,z,u0,3F/16F,light);v(out,pose,x-r,y+r,z,u0,12F/16F,light);
            v(out,pose,x+r,y+r,z,u1,12F/16F,light);v(out,pose,x+r,y-r,z,u1,3F/16F,light);
        }pose.popPose();
    }
    private static void v(VertexConsumer out,PoseStack pose,float x,float y,float z,float u,float v,int light){out.vertex(pose.last().pose(),x/16F,y/16F,z/16F).color(255,255,255,255).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.last().normal(),0,0,-1).endVertex();}
}
