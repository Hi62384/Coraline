package com.coraline.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

/** Tiny code-native mesh; no Heart-of-the-Sea recolour or high-resolution packaged art. */
public final class VoidHeartRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/white_concrete.png");
    public VoidHeartRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        double tick=Minecraft.getInstance().level==null?0:Minecraft.getInstance().level.getGameTime()+Minecraft.getInstance().getFrameTime();
        float pulse=(float)(1+Math.sin(tick*.055)*.025);pose.pushPose();pose.translate(.5,.48,.5);pose.scale(pulse,pulse,pulse);
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        int[] widths={2,4,6,8,8,6,4};
        for(int row=0;row<widths.length;row++){
            double y=-.27+row*.075,w=widths[row]*.065;
            CinematicPropRenderer.box(pose,out,light,0,y,0,w,.081,.20+(row%2)*.025,0x17101F);
            if(row<5)CinematicPropRenderer.box(pose,out,15728880,Math.sin(row*1.9)*.045,y+.017,-.116,.025,.045,.012,0x8A3BCC);
        }
        for(int side:new int[]{-1,1})CinematicPropRenderer.box(pose,out,light,side*.135,.235,0,.22,.14,.20,0x211328);
        CinematicPropRenderer.box(pose,out,15728880,-.10,.205,-.11,.07,.026,.012,0xBC72FF);
        for(int i=0;i<3;i++){double a=tick*.014+i*2.094;CinematicPropRenderer.box(pose,out,15728880,Math.cos(a)*.34,.07+Math.sin(a*1.5)*.2,Math.sin(a)*.15,.018,.018,.018,0x8E55D9);}
        pose.popPose();
    }
}
