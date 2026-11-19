package com.coraline.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class ContestCakeItemRenderer extends BlockEntityWithoutLevelRenderer {
    public ContestCakeItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}

    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var tag=stack.getTag();
        var design=tag!=null&&tag.contains("CakeDesign",Tag.TAG_COMPOUND)?tag.getCompound("CakeDesign"):legacyCake(tag);
        pose.pushPose();
        pose.translate(.5,.07,.5);
        CakeMesh.render(design,pose,buffers,light);
        pose.popPose();
    }

    private static CompoundTag legacyCake(CompoundTag tag){
        int style=tag==null?0:tag.getInt("CakeStyle");
        var design=new CompoundTag();
        design.putInt("Base",style&3);
        design.putInt("Layers",2);
        design.putInt("Filling",0);
        design.putInt("Frosting",(style>>>2)&3);
        design.putInt("Topper",(style>>>4)&3);
        return design;
    }
}
