package com.coraline.client.renderer;

import com.coraline.otherworld.DoorBlockEntity;
import com.coraline.block.TinyDoorBlock;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Real-depth fabric behind the open door, rather than a flat portal card. */
public final class DoorPortalRenderer implements BlockEntityRenderer<DoorBlockEntity> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("coraline","textures/block/tunnel_fabric.png");
    public DoorPortalRenderer(BlockEntityRendererProvider.Context context){}
    @Override public int getViewDistance(){return 128;}
    @Override public boolean shouldRenderOffScreen(DoorBlockEntity door){return true;}
    @Override public void render(DoorBlockEntity block,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var state=block.getBlockState();
        if(block.hasPostgameBinding()){
            if(!state.getValue(TinyDoorBlock.PORTAL)||!state.getValue(TinyDoorBlock.OPEN))return;
            pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(Axis.YP.rotationDegrees(-state.getValue(TinyDoorBlock.FACING).toYRot()+180));
            var surface=buffers.getBuffer(StageCurtainRenderType.cloth(TEXTURE));float pulse=.8F+.2F*(float)Math.sin((block.getLevel().getGameTime()+partial)*.05F);
            for(int side:new int[]{-1,1}){
                surface.vertex(pose.last().pose(),-side*.48F,-.48F,.15F).color(.62F*pulse,.35F*pulse,1,1).uv(0,1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0,0,side).endVertex();
                surface.vertex(pose.last().pose(),side*.48F,-.48F,.15F).color(.8F*pulse,.6F*pulse,1,1).uv(1,1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0,0,side).endVertex();
                surface.vertex(pose.last().pose(),side*.48F,.48F,.15F).color(.62F*pulse,.35F*pulse,1,1).uv(1,0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0,0,side).endVertex();
                surface.vertex(pose.last().pose(),-side*.48F,.48F,.15F).color(.8F*pulse,.6F*pulse,1,1).uv(0,0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0,0,side).endVertex();
            }pose.popPose();return;
        }
        if(block.storyBricked){
            pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(Axis.YP.rotationDegrees(-state.getValue(TinyDoorBlock.FACING).toYRot()+180));
            var brick=buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/bricks.png")));
            brick.vertex(pose.last().pose(),-.49F,-.49F,.15F).color(255,255,255,255).uv(0,1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.last().normal(),0,0,1).endVertex();
            brick.vertex(pose.last().pose(),.49F,-.49F,.15F).color(255,255,255,255).uv(1,1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.last().normal(),0,0,1).endVertex();
            brick.vertex(pose.last().pose(),.49F,.49F,.15F).color(255,255,255,255).uv(1,0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.last().normal(),0,0,1).endVertex();
            brick.vertex(pose.last().pose(),-.49F,.49F,.15F).color(255,255,255,255).uv(0,0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.last().normal(),0,0,1).endVertex();pose.popPose();return;
        }
    }
}
