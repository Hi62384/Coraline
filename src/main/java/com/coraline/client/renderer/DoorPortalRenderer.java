package com.coraline.client.renderer;

import com.coraline.otherworld.DoorBlockEntity;
import com.coraline.block.TinyDoorBlock;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Perspective-compressed 3D fabric rings fit inside a wall, without replacing the owner's house. */
public final class DoorPortalRenderer implements BlockEntityRenderer<DoorBlockEntity> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("coraline","textures/block/tunnel_fabric.png");
    public DoorPortalRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(DoorBlockEntity block,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var state=block.getBlockState();if(!state.getValue(TinyDoorBlock.PORTAL)||!state.getValue(TinyDoorBlock.OPEN))return;
        pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(Axis.YP.rotationDegrees(-state.getValue(TinyDoorBlock.FACING).toYRot()+180));
        var v=buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        vertex(v,pose,-.44F,-.50F,.39F,0,0,0);vertex(v,pose,-.44F,.50F,.39F,0,1,0);
        vertex(v,pose,.44F,.50F,.39F,1,1,0);vertex(v,pose,.44F,-.50F,.39F,1,0,0);
        for(int ring=0;ring<24;ring++){
            float a=(float)Math.pow(.87,ring)*.50F,b=a*.87F,z=.19F+ring*.007F;
            for(int side=0;side<24;side++){
                double t=side*Math.PI/12,u=(side+1)*Math.PI/12;
                float brightness=(1-ring*.02F)*(.82F+.18F*(float)Math.sin(ring*1.7));
                vertex(v,pose,(float)Math.cos(t)*a,(float)Math.sin(t)*a,z,side/12F,ring/4F,brightness);
                vertex(v,pose,(float)Math.cos(u)*a,(float)Math.sin(u)*a,z,(side+1)/12F,ring/4F,brightness);
                vertex(v,pose,(float)Math.cos(u)*b,(float)Math.sin(u)*b,z+.007F,(side+1)/12F,(ring+1)/4F,brightness);
                vertex(v,pose,(float)Math.cos(t)*b,(float)Math.sin(t)*b,z+.007F,side/12F,(ring+1)/4F,brightness);
            }
        }
        var end=buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath("coraline","textures/block/tiny_door.png")));
        vertex(end,pose,-.021F,-.026F,.365F,0,1,.65F);vertex(end,pose,.021F,-.026F,.365F,1,1,.65F);
        vertex(end,pose,.021F,.026F,.365F,1,0,.65F);vertex(end,pose,-.021F,.026F,.365F,0,0,.65F);
        pose.popPose();
    }
    private static void vertex(VertexConsumer v,PoseStack p,float x,float y,float z,float u,float t,float color){v.vertex(p.last().pose(),x,y,z).color(color,color,color,1).uv(u,t).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(p.last().normal(),0,0,-1).endVertex();}
}
