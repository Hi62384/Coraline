package com.coraline.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

public final class GhostEyeRenderer extends BlockEntityWithoutLevelRenderer {
    public static final int[] COLORS={0x50F2D0,0xF388DD,0x9DA3FF};
    public GhostEyeRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    @Override public void renderByItem(ItemStack item,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(.5,.5,.5);int slot=item.hasTag()?item.getTag().getInt("GhostEye"):0;eye(pose,buffers,Math.max(0,Math.min(2,slot)),1);pose.popPose();
    }
    public static void eye(PoseStack pose,MultiBufferSource buffers,int slot,float alpha){
        var out=buffers.getBuffer(RenderType.entityTranslucent(ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/white_concrete.png")));
        eye(pose,out,slot,alpha);
    }
    /** Emissive halo only in the Seeing Stone attachment, never in the world pass. */
    public static void spectral(PoseStack pose,com.mojang.blaze3d.vertex.VertexConsumer out,int slot,boolean control,boolean exposed,float pulse){
        float strength=control&&!exposed?.22F:.65F+.35F*pulse;
        int color=control?0xFFF17A:COLORS[Math.floorMod(slot,3)];
        FirstEscapePresentation.sphere(pose,out,15728880,0,0,.02,.49,.40,.035,color,.20F*strength);
        FirstEscapePresentation.sphere(pose,out,15728880,0,0,0,.39,.31,.035,color,.35F*strength);
        if(control){
            control(pose,out);
            if(exposed){
                // In front of the opaque housing, with a fully emissive yellow
                // core and a pulsing outer ring, exclusively in the stone pass.
                int yellow=pulse>.5F?0xFFFF35:0xE5AE00;
                FirstEscapePresentation.sphere(pose,out,15728880,0,0,-.23,.27,.27,.035,yellow,1);
                for(int i=0;i<8;i++){double a=i*Math.PI/4;FirstEscapePresentation.sphere(pose,out,15728880,Math.cos(a)*(.40+.06*pulse),Math.sin(a)*(.40+.06*pulse),-.24,.055,.055,.02,0xFFFF35,.55F+.45F*pulse);}
            }
        }
        else eye(pose,out,slot,1);
    }
    public static void control(PoseStack pose,com.mojang.blaze3d.vertex.VertexConsumer out){
        // Brass housing, cracked dark control button and exposed glowing wiring.
        FirstEscapePresentation.sphere(pose,out,15728880,0,0,0,.36,.36,.09,0xC59342,1);
        FirstEscapePresentation.sphere(pose,out,15728880,0,0,-.08,.26,.26,.05,0x283D38,1);
        FirstEscapePresentation.sphere(pose,out,15728880,0,0,-.13,.12,.12,.035,0xFFB95C,1);
        for(int i=0;i<4;i++){double angle=i*Math.PI/2;FirstEscapePresentation.sphere(pose,out,15728880,Math.cos(angle)*.20,Math.sin(angle)*.20,-.14,.035,.035,.02,0xFFE4A0,1);}
    }
    public static void eye(PoseStack pose,com.mojang.blaze3d.vertex.VertexConsumer out,int slot,float alpha){
        int rgb=COLORS[slot%3];FirstEscapePresentation.sphere(pose,out,15728880,0,0,0,.31,.22,.10,0xE8FFF4,alpha*.75F);
        FirstEscapePresentation.sphere(pose,out,15728880,0,0,-.085,.17,.18,.055,rgb,alpha);
        FirstEscapePresentation.sphere(pose,out,15728880,0,0,-.13,.055,.11,.028,0x15212D,alpha);
        FirstEscapePresentation.sphere(pose,out,15728880,-.045,.055,-.15,.025,.025,.012,0xFFFFFF,alpha);
    }
}
