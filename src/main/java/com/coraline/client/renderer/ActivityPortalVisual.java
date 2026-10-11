package com.coraline.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

/** An oval aperture woven from moving threads. No blocks, textures or extra entities. */
final class ActivityPortalVisual {
    private static final int SEGMENTS=64;
    private static final double TAU=Math.PI*2;
    static void render(float tick,PoseStack pose,MultiBufferSource buffers){
        var v=buffers.getBuffer(RenderType.debugQuads());var matrix=pose.last().pose();
        double time=tick*.018;
        // Concentric curved bands form an opaque-looking blue depth rather than a flat glass rectangle.
        for(int band=0;band<12;band++)for(int i=0;i<SEGMENTS;i++){
            double a=i*TAU/SEGMENTS,b=(i+1)*TAU/SEGMENTS,r0=band/12.0,r1=(band+1)/12.0;
            float wave=(float)(.5+.5*Math.sin(a*3-r1*15+time));
            quad(v,matrix,r0,a,r1,a,r1,b,r0,b,.06F,
                .035F+wave*.07F,.055F+wave*.12F,.19F+wave*.19F,.94F);
        }
        // Counter-rotating, tapered spiral filaments suggest a tunnel behind the opening.
        for(int arm=0;arm<5;arm++)for(int i=0;i<64;i++){
            double r=.09+i*.013,rn=r+.013,a=arm*TAU/5+r*5-time,b=arm*TAU/5+rn*5-time;
            quad(v,matrix,r,a,r+.014,a,rn+.014,b,rn,b,-.015F,.24F,.48F,.9F,.3F);
        }
        for(int ring=0;ring<3;ring++)for(int i=0;i<SEGMENTS;i++){
            double a=i*TAU/SEGMENTS,b=(i+1)*TAU/SEGMENTS;
            double radius=1+ring*.035+.008*Math.sin(a*6+time*(ring==1?-1:1));
            quad(v,matrix,radius,a,radius+.018,a,radius+.018,b,radius,b,-.035F,.48F,.82F,1F,.8F);
        }
        // Gold cross-stitches: distinctly sewn rather than a ring of gemstones.
        for(int i=0;i<32;i++){
            double a=i*TAU/32+time*.035;
            quad(v,matrix,.975,a-.009,1.105,a+.025,1.11,a+.035,.98,a+.001,-.055F,1F,.79F,.4F,.9F);
        }
        // A small deterministic orbit of sparks; bounded geometry, no particle/entity allocation.
        for(int i=0;i<18;i++){
            double a=i*2.399+time*(.18+i%3*.06),radius=1.13+.13*Math.sin(time+i);
            float x=(float)(Math.cos(a)*radius*.91),y=1.5F+(float)(Math.sin(a)*radius*1.35);
            float size=.012F+.014F*(float)(.5+.5*Math.sin(time*2+i));
            point(v,matrix,x-size,y,-.08F,.7F,.9F,1, .8F);point(v,matrix,x,y+size*2,-.08F,.7F,.9F,1,.8F);
            point(v,matrix,x+size,y,-.08F,.7F,.9F,1,.8F);point(v,matrix,x,y-size*2,-.08F,.7F,.9F,1,.8F);
        }
    }
    private static void quad(VertexConsumer v,Matrix4f m,double r0,double a0,double r1,double a1,double r2,double a2,double r3,double a3,float z,float red,float green,float blue,float alpha){
        polar(v,m,r0,a0,z,red,green,blue,alpha);polar(v,m,r1,a1,z,red,green,blue,alpha);
        polar(v,m,r2,a2,z,red,green,blue,alpha);polar(v,m,r3,a3,z,red,green,blue,alpha);
    }
    private static void polar(VertexConsumer v,Matrix4f m,double r,double a,float z,float red,float green,float blue,float alpha){
        point(v,m,(float)(Math.cos(a)*r*.91),1.5F+(float)(Math.sin(a)*r*1.35),z,red,green,blue,alpha);
    }
    private static void point(VertexConsumer v,Matrix4f m,float x,float y,float z,float red,float green,float blue,float alpha){v.vertex(m,x,y,z).color(red,green,blue,alpha).endVertex();}
}
