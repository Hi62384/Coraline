package com.coraline.client.renderer;
import com.coraline.block.DollBlock;
import com.coraline.block.entity.DollBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;

public final class DollRenderer implements BlockEntityRenderer<DollBlockEntity> {
    private final DollVisual visual = new DollVisual();
    public DollRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(DollBlockEntity doll,float partialTick,PoseStack pose,MultiBufferSource buffer,int light,int overlay) {
        pose.pushPose();
        pose.translate(0.5,0.385,0.5);
        // The flipped model faces +Z. Minecraft's direction yaw has the opposite sign.
        pose.mulPose(Axis.YP.rotationDegrees(-doll.getBlockState().getValue(DollBlock.FACING).toYRot()));
        pose.scale(0.43F,-0.43F,-0.43F);
        if(doll.getOwnerId()==null&&com.coraline.otherworld.OtherWorld.isOther(doll.getLevel())){
            // Unbound scenery is not the player's personal, skin-matching doll.
            int variant=Math.floorMod(Long.hashCode(doll.getBlockPos().asLong()),16);
            visual.renderSkin(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("coraline",
                "textures/entity/parents/"+(variant%2==0?"mom_":"dad_")+(variant/2)+".png"),false,pose,buffer,light,true);
        }else visual.render(doll.getProfile(),pose,buffer,light,true);
        pose.popPose();
    }
}
