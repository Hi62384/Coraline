package com.coraline.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;

/** Same appearance parameters as resident rig, simplified into a stitched toy. */
public final class TycoonDollVisual {
    public static void render(int seed,PoseStack pose,MultiBufferSource buffers,int light){
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/white_wool.png")));
        int skin=new int[]{0xE1B994,0xBA845E,0x835C43,0xF1D2BA,0xA66D4B}[(seed/5)%5],hair=new int[]{0x392A25,0xBB8139,0x251F25,0x77482D,0xD8C39E}[(seed/25)%5],cloth=new int[]{0x557892,0xAB6679,0x708657,0xAC935B,0x786894,0x477F77}[(seed/125)%6];
        box(pose,out,light,-.25F,0,-.18F,.5F,.06F,.36F,0x604B38);
        for(float x:new float[]{-.12F,.12F}){box(pose,out,light,x-.06F,.06F,-.07F,.12F,.35F,.14F,0x39465A);box(pose,out,light,x*2-.055F,.41F,-.055F,.11F,.3F,.11F,skin);}
        box(pose,out,light,-.19F,.39F,-.11F,.38F,.35F,.22F,cloth);box(pose,out,light,-.23F,.75F,-.18F,.46F,.4F,.36F,skin);box(pose,out,light,-.24F,1.1F,-.19F,.48F,.08F,.38F,hair);
        int style=(seed/750)%4;if(style>0){box(pose,out,light,-.27F,.85F,.04F,.08F,.29F,.15F,hair);box(pose,out,light,.19F,.85F,.04F,.08F,.29F,.15F,hair);}if(style==2){box(pose,out,light,-.31F,.9F,0,.12F,.14F,.16F,hair);box(pose,out,light,.19F,.9F,0,.12F,.14F,.16F,hair);}
        for(float x:new float[]{-.1F,.1F}){box(pose,out,light,x-.036F,.96F,-.192F,.072F,.072F,.02F,0x282631);for(float dx:new float[]{-.014F,.014F})for(float y:new float[]{.975F,1.0F})box(pose,out,light,x+dx-.005F,y,-.205F,.01F,.01F,.015F,0xDDC6A2);}
        box(pose,out,light,-.06F,.84F,-.195F,.12F,.012F,.015F,0x74534E);for(float y=.43F;y<.72F;y+=.045F)box(pose,out,light,-.009F,y,-.123F,.018F,.023F,.015F,0xDCCCAE);
        if((seed/3000)%2==1)box(pose,out,light,-.18F,.48F,-.124F,.36F,.035F,.012F,0xE9D6A4);
    }
    private static void box(PoseStack p,com.mojang.blaze3d.vertex.VertexConsumer b,int light,float x,float y,float z,float w,float h,float d,int color){TycoonNpcRenderer.box(p,b,light,x,y,z,w,h,d,color);}
    private TycoonDollVisual(){}
}
