package com.coraline.client.renderer;

import com.coraline.feast.CinematicPropEntity;
import com.coraline.rebirth.StoryRebirth;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;

/** Articulated strands are real mesh, with a shared server phase/clock and restrained accents. */
public final class RebirthPresentation {
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/white_concrete.png");
    private static void box(PoseStack p,VertexConsumer b,int light,double x,double y,double z,double w,double h,double d,int color){CinematicPropRenderer.box(p,b,light,x,y,z,w,h,d,color);}
    private static void thread(PoseStack p,VertexConsumer b,int light,double x,double y,double z,double a,double c,double d,int color){
        // Short closely spaced oriented samples form a continuous fine strand without ribbon billboarding.
        int samples=Math.min(70,Math.max(2,(int)(Math.sqrt((a-x)*(a-x)+(c-y)*(c-y)+(d-z)*(d-z))*32)));
        for(int i=0;i<=samples;i++){double u=i/(double)samples;box(p,b,light,x+(a-x)*u,y+(c-y)*u,z+(d-z)*u,.025,.025,.025,color);}
    }
    public static boolean render(CinematicPropEntity e,PoseStack p,MultiBufferSource buffers,int light,float partial){
        if(!StoryRebirth.kind(e.kind()))return false;var b=buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));double t=e.visualTime(partial);p.pushPose();
        if(e.kind()==StoryRebirth.PORTRAIT){
            p.mulPose(Axis.YP.rotationDegrees(180)); // Picture faces the room, not the supporting north wall.
            box(p,b,light,0,0,0,1.32,1.5,.10,0x3B2B30);box(p,b,light,0,.08,-.065,1.16,1.32,.025,0xB7A58D);
            for(int side:new int[]{-1,1}){box(p,b,light,side*.61,.025,-.09,.07,1.45,.05,0x806443);box(p,b,light,0,side<0?.025:1.43,-.09,1.22,.07,.05,0x806443);}
            for(int person=0;person<3;person++){double x=(person-1)*.33,y=person==1?.43:.57;boolean faded=e.cue()>=5&&person==0||e.cue()>=7&&person==2;
                if(!faded){box(p,b,light,x,y,-.09,.21,.37,.025,person==1?0xB29A4D:0x5A485B);box(p,b,light,x,y+.31,-.10,.19,.21,.026,0xCEB395);box(p,b,light,x,y+.47,-.11,.21,.06,.025,person==1?0x243B55:0x48352D);
                    for(int eye:new int[]{-1,1})box(p,b,light,x+eye*.045,y+.40,-.13,.018,.018,.012,0x28212A);}}
            for(int i=0;i<28;i++){double a=i*Math.PI/14;box(p,b,15728880,Math.cos(a)*.60,.75+Math.sin(a)*.70,-.13,.025,.025,.025,i%5==0?0x5B3D77:0x17101D);}
            if(e.cue()>=8)for(int i=0;i<9;i++)thread(p,b,15728880,-.4+i*.09,.5,-.15,-.3+i*.08,1.1,-.15,0x9A6ABB);
        }else if(e.kind()==StoryRebirth.CIRCLE){
            double bind=e.cue()>=2?Math.min(1,t/120):0;if(e.cue()>2)bind=1;
            double retract=e.cue()>=8?Math.max(0,1-t/140):1;
            for(int i=0;i<72;i++){double a=i*Math.PI/36,next=a+Math.PI/36;thread(p,b,15728880,Math.cos(a)*2.05,.025,Math.sin(a)*2.05,Math.cos(next)*2.05,.025,Math.sin(next)*2.05,0x574064);if(i%4==0)thread(p,b,light,Math.cos(a)*1.9,.03,Math.sin(a)*1.9,Math.cos(a)*1.65,.03,Math.sin(a)*1.65,0x201222);}
            for(int side:new int[]{-1,1})for(int strand=0;strand<3;strand++){
                double px=side*1.05,py=0,pz=.18;
                for(int i=0;i<32*bind*retract;i++){double a=i*.42+strand*2.1,y=i*.043;
                    double x=side*1.05+Math.cos(a)*.34,z=Math.sin(a)*.24;
                    if(i>0)thread(p,b,light,px,py,pz,x,y,z,0x19101F);px=x;py=y;pz=z;}
            }
            if(e.cue()==7)for(int side:new int[]{-1,1})for(int i=0;i<5;i++){double u=((t*.008+i*.2)%1);box(p,b,15728880,side*1.05*(1-u),.3+Math.sin(u*Math.PI)*.8,0,.035,.035,.035,0x8156A2);}
        }else{
            double length=e.cue()==0?1:Math.max(0,e.value());int color=e.cue()==0?0xA77DCD:0x684676;
            thread(p,b,15728880,0,0,0,0,.65*length,0,color);box(p,b,15728880,0,.3*length,0,.07,.07,.07,color);
            thread(p,b,15728880,0,.65*length,0,0,.60*length,-.55*length,color);
        }
        p.popPose();return true;
    }
    private RebirthPresentation(){}
}
