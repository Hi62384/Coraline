package com.coraline.client.renderer;

import com.coraline.feast.VillageActivity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;

/** The village residents borrow the same sewn-button eye art as the Other World dolls. */
public final class VillageVillagerEyes extends RenderLayer<Villager,VillagerModel<Villager>> {
    private static final ResourceLocation EYES=ResourceLocation.fromNamespaceAndPath("coraline","textures/entity/doll_button_eyes.png");
    public VillageVillagerEyes(RenderLayerParent<Villager,VillagerModel<Villager>> renderer){super(renderer);}
    @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,Villager villager,float limb,float amount,float partial,float age,float yaw,float pitch){
        if(villager.getCustomName()==null||!VillageActivity.RESIDENT_NAME.equals(villager.getCustomName().getString()))return;
        pose.pushPose();getParentModel().getHead().translateAndRotate(pose);
        // Replace the vanilla white/green eye pixels with a small swatch of the
        // same villager face texture before sewing the buttons over them.
        VertexConsumer skin=buffers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(villager)));
        float u=9.5F/64F,v=10.5F/64F;
        skinVertex(skin,pose,-.25F,-.29F,u,v,light);skinVertex(skin,pose,-.25F,-.14F,u,v,light);
        skinVertex(skin,pose,.25F,-.14F,u,v,light);skinVertex(skin,pose,.25F,-.29F,u,v,light);
        VertexConsumer out=buffers.getBuffer(RenderType.entityCutoutNoCull(EYES));
        // Each half of the sprite is one button. Put the two small quads over
        // the vanilla eye pixels, not one stretched sheet over the whole face.
        button(out,pose,-.09F,0F,.5F,light);
        button(out,pose,.09F,.5F,1F,light);
        pose.popPose();
    }
    private static void button(VertexConsumer out,PoseStack pose,float x,float u0,float u1,int light){
        float y=-.215F,r=.079F;
        vertex(out,pose,x-r,y-r,u0,0,light);vertex(out,pose,x-r,y+r,u0,1,light);
        vertex(out,pose,x+r,y+r,u1,1,light);vertex(out,pose,x+r,y-r,u1,0,light);
    }
    private static void vertex(VertexConsumer out,PoseStack pose,float x,float y,float u,float v,int light){
        out.vertex(pose.last().pose(),x,y,-.255F).color(255,255,255,255).uv(u,v)
            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.last().normal(),0,0,-1).endVertex();
    }
    private static void skinVertex(VertexConsumer out,PoseStack pose,float x,float y,float u,float v,int light){
        out.vertex(pose.last().pose(),x,y,-.252F).color(255,255,255,255).uv(u,v)
            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.last().normal(),0,0,-1).endVertex();
    }
}
