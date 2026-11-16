package com.coraline.client.renderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;
public final class TycoonDollItemRenderer extends BlockEntityWithoutLevelRenderer {
    public TycoonDollItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    @Override public void renderByItem(ItemStack item,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){pose.pushPose();pose.translate(.5,.1,.5);pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(context==ItemDisplayContext.GUI?155:180));pose.scale(.68F,.68F,.68F);TycoonDollVisual.render(item.hasTag()?item.getTag().getInt("Appearance"):0,pose,buffers,light);pose.popPose();}
}
