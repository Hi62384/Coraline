package com.coraline.client.renderer;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.*;

public final class DollItemRenderer extends BlockEntityWithoutLevelRenderer {
    private DollVisual visual;
    public DollItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffer,int light,int overlay) {
        if(visual==null) visual=new DollVisual();
        GameProfile profile=null;
        var owner=stack.getTagElement("BlockEntityTag");
        if(owner!=null) {
            profile=NbtUtils.readGameProfile(owner.getCompound("Profile"));
            if(profile==null && owner.hasUUID("Owner")) profile=new GameProfile(owner.getUUID("Owner"),owner.getString("OwnerName"));
        }
        pose.pushPose();
        if(stack.hasTag()&&stack.getTag().contains("SecretSpySkin")){
            int variant=Math.floorMod(stack.getTag().getInt("SecretSpySkin"),16);
            pose.translate(.5,.385,.5);pose.scale(.43F,-.43F,-.43F);
            visual.renderSkin(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("coraline","textures/entity/parents/"+(variant%2==0?"mom_":"dad_")+(variant/2)+".png"),false,pose,buffer,light,true);
            pose.popPose();return;
        }
        pose.translate(0.5,0.75,0.5);
        pose.mulPose(Axis.YP.rotationDegrees(context==ItemDisplayContext.GUI?25:0));
        pose.scale(0.46F,-0.46F,-0.46F);
        visual.render(profile,pose,buffer,light);
        pose.popPose();
    }
}
