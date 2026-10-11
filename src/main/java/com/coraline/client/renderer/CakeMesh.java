package com.coraline.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Purpose-built twelve-sided cakes with shared attachment heights and authored texture atlases. */
final class CakeMesh {
    private static final ResourceLocation SPONGE=ResourceLocation.fromNamespaceAndPath("coraline","textures/entity/festival_cake_sponge_atlas.png");
    private static final ResourceLocation FROST=ResourceLocation.fromNamespaceAndPath("coraline","textures/entity/festival_cake_frosting_atlas.png");
    private static final int SIDES=12;
    private CakeMesh(){}
    private static void v(VertexConsumer out,PoseStack.Pose pose,int light,float x,float y,float z,float u,float t,float nx,float ny,float nz,int color){
        Matrix4f pos=pose.pose();Matrix3f normal=pose.normal();
        out.vertex(pos,x,y,z).color((color>>>16)&255,(color>>>8)&255,color&255,255).uv(u,t)
            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal,nx,ny,nz).endVertex();
    }
    private static float u(int swatch,float t){return (Math.max(0,Math.min(6,swatch))+.08F+.84F*t)/7F;}
    private static void disk(PoseStack pose,VertexConsumer out,int light,float r,float y,int swatch,int color){
        var m=pose.last();for(int i=0;i<SIDES;i++){
            double a=i*Math.PI*2/SIDES,b=(i+1)*Math.PI*2/SIDES;
            float x1=(float)Math.cos(a)*r,z1=(float)Math.sin(a)*r,x2=(float)Math.cos(b)*r,z2=(float)Math.sin(b)*r;
            v(out,m,light,0,y,0,u(swatch,.5F),.5F,0,1,0,color);
            v(out,m,light,x2,y,z2,u(swatch,.5F+x2/(2*r)),.5F+z2/(2*r),0,1,0,color);
            v(out,m,light,x1,y,z1,u(swatch,.5F+x1/(2*r)),.5F+z1/(2*r),0,1,0,color);
            v(out,m,light,0,y,0,u(swatch,.5F),.5F,0,1,0,color);
        }
    }
    private static void band(PoseStack pose,VertexConsumer out,int light,float r,float bottom,float top,int swatch,int color,int count){
        var m=pose.last();for(int i=0;i<count;i++){
            double a=i*Math.PI*2/SIDES,b=(i+1)*Math.PI*2/SIDES;
            float x1=(float)Math.cos(a)*r,z1=(float)Math.sin(a)*r,x2=(float)Math.cos(b)*r,z2=(float)Math.sin(b)*r;
            float nx=(float)Math.cos((a+b)/2),nz=(float)Math.sin((a+b)/2);
            v(out,m,light,x1,bottom,z1,u(swatch,.15F),.87F,nx,0,nz,color);
            v(out,m,light,x1,top,z1,u(swatch,.15F),.12F,nx,0,nz,color);
            v(out,m,light,x2,top,z2,u(swatch,.82F),.12F,nx,0,nz,color);
            v(out,m,light,x2,bottom,z2,u(swatch,.82F),.87F,nx,0,nz,color);
        }
    }
    static void stand(PoseStack pose,MultiBufferSource buffers,int light){
        var porcelain=buffers.getBuffer(RenderType.entityCutout(FROST));
        band(pose,porcelain,light,.36F,0,.055F,0,0xD8B85D,SIDES);
        disk(pose,porcelain,light,.36F,.055F,0,0xF7DC8C);
        band(pose,porcelain,light,.085F,.055F,.22F,0,0xECD8C0,SIDES);
        band(pose,porcelain,light,.46F,.22F,.265F,0,0xF7F0E5,SIDES);
        disk(pose,porcelain,light,.46F,.265F,0,0xFFFAEF);
        band(pose,porcelain,light,.465F,.22F,.265F,0,0xCDA347,SIDES);
    }
    static void render(CompoundTag d,PoseStack pose,MultiBufferSource buffers,int light){
        stand(pose,buffers,light);
        int base=Math.max(0,Math.min(6,d.getInt("Base"))),layers=Math.max(1,Math.min(3,d.getInt("Layers")));
        int filling=d.getInt("Filling"),frost=d.getInt("Frosting"),toppings=d.getInt("Toppings"),topper=d.getInt("Topper");
        int coverage=d.contains("FrostCoverage")?d.getInt("FrostCoverage"):4;
        var sponge=buffers.getBuffer(RenderType.entityCutout(SPONGE));var icing=buffers.getBuffer(RenderType.entityCutout(FROST));
        float y=.265F;
        float layerHeight=layers==1?.30F:layers==2?.215F:.17F;
        for(int i=0;i<layers;i++){
            band(pose,sponge,light,.385F,y,y+layerHeight,base,0xFFFFFF,SIDES);
            disk(pose,sponge,light,.385F,y+layerHeight,base,0xFFFFFF);
            y+=layerHeight;
            if(i<layers-1){int swatch=switch(filling){case 1->1;case 2,3->2;case 4->4;default->0;};
                band(pose,icing,light,.396F,y,y+.05F,swatch,0xFFFFFF,SIDES);
                disk(pose,icing,light,.396F,y+.05F,swatch,0xFFFFFF);y+=.05F;}
        }
        if(coverage>0){int panels=Math.min(SIDES,coverage*3);
            band(pose,icing,light,.4F,y-.12F,y+.055F,Math.max(0,frost),0xFFFFFF,panels);
            disk(pose,icing,light,.4F,y+.055F,Math.max(0,frost),0xFFFFFF);y+=.055F;}
        int count=0;for(int type=0;type<10&&count<3;type++)if((toppings&(1<<type))!=0){
            float angle=count*(float)(Math.PI*2/3);float x=(float)Math.sin(angle)*.22F,z=(float)Math.cos(angle)*.22F;
            pose.pushPose();pose.translate(x,y,z);
            int swatch=type==1?3:type==3||type==7?1:type==4?6:2;
            band(pose,sponge,light,.075F,0,.09F,swatch,0xFFFFFF,SIDES);disk(pose,sponge,light,.075F,.09F,swatch,0xFFFFFF);
            pose.popPose();count++;}
        if(topper>=0){int color=switch(topper){case 0->0xE8D7FF;case 1,6->0xFFD36F;case 2->0xF49CC6;case 3->0xF46886;case 4->0xFFF8D8;default->0x37343D;};
            band(pose,icing,light,.065F,y,y+.23F,0,color,SIDES);disk(pose,icing,light,.065F,y+.23F,0,color);}
    }
}
