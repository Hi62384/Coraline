package com.coraline.client.renderer;

import com.mojang.blaze3d.vertex.*;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Reuses sewn-button art, attached to the actual head rather than camera billboards. */
public final class MansionWitnessEyes<T extends LivingEntity,M extends EntityModel<T>> extends RenderLayer<T,M> {
    private static final ResourceLocation EYES=ResourceLocation.fromNamespaceAndPath("coraline","textures/entity/doll_button_eyes.png");
    private static final ResourceLocation ALLAY=ResourceLocation.fromNamespaceAndPath("minecraft","textures/entity/allay/allay.png");
    private final Function<M,ModelPart> head;private final boolean small;
    public MansionWitnessEyes(RenderLayerParent<T,M> parent,Function<M,ModelPart> head,boolean small){super(parent);this.head=head;this.small=small;}
    @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,T entity,float limb,float amount,float partial,float age,float yaw,float pitch){
        if(entity.getCustomName()==null||!entity.getCustomName().getString().startsWith("Button-Eyed "))return;
        pose.pushPose();
        if(small&&getParentModel() instanceof net.minecraft.client.model.AllayModel allay)allay.root().translateAndRotate(pose);
        head.apply(getParentModel()).translateAndRotate(pose);
        if(small){
            // Allay eyes are tall white strips. Restore the adjacent cyan skin
            // before adding round buttons, rather than enlarging two buttons
            // until they overlap each other. Sample each original skin row.
            var skin=buffers.getBuffer(RenderType.entityCutoutNoCull(ALLAY));
            for(int side:new int[]{-1,1})for(int row=0;row<2;row++){
                float x=side/16F,left=x-.5F/16F,right=x+.5F/16F;
                float top=(-3F+row)/16F,bottom=top+1F/16F;
                float u=7.5F/32F,v=(7.5F+row)/32F,z=-.157F;
                vertex(skin,pose,left,top,z,u,v,light);vertex(skin,pose,left,bottom,z,u,v,light);
                vertex(skin,pose,right,bottom,z,u,v,light);vertex(skin,pose,right,top,z,u,v,light);
            }
        }
        var vertices=buffers.getBuffer(RenderType.entityCutoutNoCull(EYES));
        // Vanilla head-local pixels: Allay eyes at (+/-1,-2), illagers at
        // (+/-2,-3.5). Cover the complete original eye, not just its pupil.
        float y=(small?-2F:-3.5F)/16F,z=small?-.158F:-.25125F;
        float r=(small?.85F:1.45F)/16F;
        for(int side=0;side<2;side++){
            float x=(side==0?-1:1)*(small?1F:2F)/16F;
            // Crop each button's opaque bounds; the original 16x16 cells have
            // unequal padding and otherwise squash the buttons into tiny ovals.
            float u0=(side==0?2F:17F)/32F,u1=(side==0?15F:30F)/32F;
            float v0=3F/16F,v1=12F/16F;
            vertex(vertices,pose,x-r,y-r,z,u0,v0,light);vertex(vertices,pose,x-r,y+r,z,u0,v1,light);
            vertex(vertices,pose,x+r,y+r,z,u1,v1,light);vertex(vertices,pose,x+r,y-r,z,u1,v0,light);
        }
        pose.popPose();
    }
    private static void vertex(VertexConsumer out,PoseStack pose,float x,float y,float z,float u,float v,int light){
        out.vertex(pose.last().pose(),x,y,z).color(255,255,255,255).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.last().normal(),0,0,-1).endVertex();
    }
}
