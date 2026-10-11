package com.coraline.client.renderer;
import com.coraline.otherworld.TunnelCoreEntity;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
public final class TunnelRenderer implements BlockEntityRenderer<TunnelCoreEntity> {
    public TunnelRenderer(BlockEntityRendererProvider.Context context){}
    @Override public boolean shouldRenderOffScreen(TunnelCoreEntity entity){return true;}
    @Override public void render(TunnelCoreEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        renderFabric(pose,buffers,entity.getLevel()==null?0:entity.getLevel().getGameTime()+partial);
    }
    public static void renderFabric(PoseStack pose,MultiBufferSource buffers,float time){
        var texture=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("coraline","textures/block/tunnel_fabric.png");
        // Opaque, depth-writing fabric must occlude the square backing/door corners.
        // Full-bright vertices retain the magical glow without a color-only pass.
        var v=buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        // Continue behind the near door at z=-1; ending at zero exposed the square
        // collision shell for the final three quarters of a block on the way home.
        for(int ring=-8;ring<144;ring++)for(int side=0;side<48;side++){
            vertex(v,pose,side,ring,time);vertex(v,pose,side+1,ring,time);
            vertex(v,pose,side+1,ring+1,time);vertex(v,pose,side,ring+1,time);
        }
    }
    private static void vertex(VertexConsumer v,PoseStack p,int side,int ring,float time){
        float z=ring*.125F;double angle=side*Math.PI/24;
        double fold=z*5.2+.5*Math.sin(angle*3+z*.7)+.25*Math.sin(z*1.3);
        float radius=.46F+(float)Math.sin(fold)*.025F+(float)Math.sin(angle*5+z)*.013F;
        float glow=.82F+.12F*(float)Math.sin(angle*2-z*.7+time*.008F);
        float shade=.78F+.22F*(float)Math.cos(fold-.7);
        v.vertex(p.last().pose(),.5F+(float)Math.cos(angle)*radius,1.5F+(float)Math.sin(angle)*radius,z)
            .color(shade*glow,shade*(.9F+.1F*glow),shade,1F)
            .uv(side/48F+z*.025F,z/4F).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
            .uv2(15728880).normal(p.last().normal(),(float)-Math.cos(angle),(float)-Math.sin(angle),0).endVertex();
    }
}
