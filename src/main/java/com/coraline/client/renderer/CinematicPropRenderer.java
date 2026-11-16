package com.coraline.client.renderer;

import com.coraline.feast.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Authored geometry informed by the supplied theatre reference; native comparison remains required. */
@Mod.EventBusSubscriber(modid="coraline",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class CinematicPropRenderer extends EntityRenderer<CinematicPropEntity> {
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/white_concrete.png");
    private static final int RED=0x99192B,GOLD=0xE6AB48,DARK=0x211C21,CREAM=0xFFE8BC;
    public CinematicPropRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=0;}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(CinematicPropEntity.TYPE.get(),CinematicPropRenderer::new);}
    @Override public ResourceLocation getTextureLocation(CinematicPropEntity entity){return WHITE;}
    @Override public void render(CinematicPropEntity e,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        if(e.isInvisible())return;
        if(RebirthPresentation.render(e,pose,buffers,light,partial))return;
        if(PalaceSignPresentation.render(e,pose,buffers,light))return;
        if(PalaceTransportPresentation.render(e,pose,buffers,light,partial))return;
        if(com.coraline.client.SpectralEyes.world(e,pose,buffers))return;
        if(RescuePresentation.prop(e,pose,buffers,light,partial))return;
        if(MissingParentsPresentation.prop(e,pose,buffers,light,partial))return;
        if(TheatreOceanPresentation.render(e,pose,buffers,light,partial))return;
        if(MissingParentsTeaPresentation.prop(e,pose,buffers,light,partial))return;
        if(GhostVictimMirrorPresentation.render(e,pose,buffers,light,partial))return;
        if(FirstEscapePresentation.render(e,pose,buffers,light,partial))return;
        var b=buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));double t=e.visualTime(partial);pose.pushPose();
        switch(e.kind()){
            case AcceptedButtonEyesWorld.SEWING_TABLE->{box(pose,b,light,0,.8,0,1.25,.12,.8,0x583327);for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})box(pose,b,light,x*.48,0,z*.27,.10,.8,.1,0x39221C);}
            case AcceptedButtonEyesWorld.BUTTONS->{for(int side:new int[]{-1,1}){box(pose,b,light,side*.065,0,0,.10,.018,.10,0x111117);for(int q=0;q<4;q++)box(pose,b,light,side*.065+(q%2-.5)*.025,.019,(q/2-.5)*.025,.012,.003,.012,0x8C8991);}}
            case AcceptedButtonEyesWorld.NEEDLE->{pose.pushPose();var direction=e.cue()==0?new net.minecraft.world.phys.Vec3(1,0,0):e.grabTarget();if(direction.lengthSqr()>.001)pose.mulPose(new org.joml.Quaternionf().rotationTo(new org.joml.Vector3f(0,1,0),new org.joml.Vector3f((float)direction.x,(float)direction.y,(float)direction.z).normalize()));box(pose,b,light,0,0,0,.018,.28,.018,0xD4D8DC);box(pose,b,light,0,.28,0,.009,.05,.009,0xD4D8DC);pose.popPose();}
            case CinematicPropEntity.BUTTON_GIFT->TraditionalPresentation.gift(e,pose,buffers,light,partial);
            case CinematicPropEntity.PIANO_HAND->TraditionalPresentation.hand(e,pose,buffers,light,partial);
            case CinematicPropEntity.SEAT->{if(e.cue()>=0)chair(pose,b,light);}
            case CinematicPropEntity.DOG_ROW->{for(int col=0;col<20;col++){double x=col<10?-24+col*2:6+(col-10)*2;pose.pushPose();pose.translate(x,0,0);if(!com.coraline.feast.TheatreSite.reserved(e.slot(),col)){chair(pose,b,light);if(e.value()>=0)dog(pose,b,light,t,col+e.slot()*20,e.cue());}pose.popPose();}}
            case CinematicPropEntity.CURTAIN->{double open=Math.max(0,Math.min(1,e.value()));for(int side:new int[]{-1,1})for(int i=0;i<28;i++){double x=side*(.32+i*.8+open*(22-i*.70));box(pose,b,light,x,0,-.18+Math.sin(i*.8)*.22,.86,25,.55,RED);box(pose,b,light,x,24.6,-.3,.9,.28,.72,GOLD);}
                // Fixed upper velvet valance and fan-shaped gold corner mouldings from the reference.
                for(int x=-22;x<=22;x++){double top=28+6*Math.sqrt(Math.max(0,1-x*x/484.0));box(pose,b,light,x,25,-.45,1.04,top-25,.55,RED);}
                for(int side:new int[]{-1,1})for(int step=0;step<=18;step++){double a=step*Math.PI/36,x=side*(3+19*Math.sin(a)),y=33-8*Math.cos(a);box(pose,b,light,x,y,-.85,1.25,.45,.55,GOLD);}
            }
            case CinematicPropEntity.DIVING->{for(int side:new int[]{-1,1}){box(pose,b,light,side*10,0,0,.45,18,.45,GOLD);box(pose,b,light,side*10,17.3,0,1.2,.5,7,CREAM);double bend=e.cue()==1?CinematicPropEntity.divingBoardOffset(t,side)*e.value():0;
                for(int plank=0;plank<5;plank++)box(pose,b,light,side*10,17.8+bend*Math.min(1,(plank+.5)/2.5),1.5+plank*.8,1.7,.22,.82,CREAM);}}
            case CinematicPropEntity.TRAPEZE->{var target=e.grabTarget();for(int side:new int[]{-1,1}){var q=e.cue()>=2?TheatreAerialGeometry.carrying(side,t,(int)e.value(),e.cue()==3):TheatreAerialGeometry.approach(side,t,(int)e.value(),target.x,target.y,target.z);double z=q.z()+15;box(pose,b,light,q.x(),q.y(),z,1.4,.10,.10,GOLD);for(int sign:new int[]{-1,1})rope(pose,b,light,q.anchorX()+sign*.62,q.anchorY(),q.anchorZ()+15,q.x()+sign*.62,q.y(),z);}}
            case CinematicPropEntity.TENT->{if(e.slot()==99)pose.mulPose(Axis.YP.rotationDegrees(180));for(int i=0;i<16;i++){if(i>=7&&i<=9)continue;pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(i*22.5F));box(pose,b,light,0,0,-1.48,.62,1.7,.15,i%2==0?RED:CREAM);pose.popPose();}for(int tier=0;tier<10;tier++){double radius=1.8-tier*.16;for(int i=0;i<16;i++){pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(i*22.5F));box(pose,b,light,0,1.7+tier*.13,-radius,.72,.16,.25,i%2==0?RED:CREAM);pose.popPose();}}box(pose,b,light,0,2.8,0,.06,.6,.06,GOLD);box(pose,b,light,.25,3.2,0,.55,.22,.06,RED);box(pose,b,light,-1.04,.05,1.35,.34,1.65,.22,RED);box(pose,b,light,1.04,.05,1.35,.34,1.65,.22,RED);box(pose,b,15728880,-.8,.1,1.45,.12,1.75,.15,GOLD);box(pose,b,15728880,.8,.1,1.45,.12,1.75,.15,GOLD);}
            case CinematicPropEntity.FERRIS->{
                box(pose,b,light,0,0,0,1.6,.14,1.1,RED);
                for(int side:new int[]{-1,1}){rope(pose,b,light,-.7,.1,side*.4,0,1.7,side*.4);rope(pose,b,light,.7,.1,side*.4,0,1.7,side*.4);}
                box(pose,b,light,0,1.6,0,.22,.22,1,GOLD);
                for(int i=0;i<24;i++){
                    double a=i*Math.PI/12+t*.006,next=a+Math.PI/12,x=Math.cos(a)*1.15,y=1.7+Math.sin(a)*1.15;
                    rope(pose,b,light,x,y,0,Math.cos(next)*1.15,1.7+Math.sin(next)*1.15,0);
                    if(i%3==0){rope(pose,b,light,0,1.7,0,x,y,0);box(pose,b,15728880,x,y,0,.10,.1,.10,GOLD);box(pose,b,light,x,y-.32,0,.36,.3,.35,RED);for(int q=0;q<5;q++)box(pose,b,light,x+(q%3-1)*.08,y-.08,((q*3)%5-2)*.05,.10,.12,.1,CREAM);}
                }
            }
            case CinematicPropEntity.CANNON->{box(pose,b,light,0,0,0,1.2,.25,.8,GOLD);pose.pushPose();pose.mulPose(Axis.ZP.rotationDegrees(-30));box(pose,b,light,0,.35,0,.48,.85,.5,RED);pose.popPose();double cycle=(t%100)/100;if(cycle<.5){double q=cycle*2;box(pose,b,15728880,q*1.5,.8+q*2.5-q*q*1.6,0,.32,.32,.32,0xF8A8DB);}}
            case CinematicPropEntity.HOOPS->{for(int lane=0;lane<8;lane++){double x=(lane-3.5)*2.2;box(pose,b,light,x,0,-8,1.2,1,2,GOLD);for(int i=0;i<16;i++){double a=i*Math.PI/8;box(pose,b,light,x+Math.cos(a)*1.05,5.1+Math.sin(a)*1.05,0,.2,.2,.2,RED);}box(pose,b,light,x,0,8,1.5,1,2,CREAM);}}
            case CinematicPropEntity.BEAMS->{for(int lane=0;lane<6;lane++){double z=(lane-2.5)*3;box(pose,b,light,0,4,z,24,.12,.12,GOLD);for(int side:new int[]{-1,1})box(pose,b,light,side*12,0,z,.45,4,.45,RED);}}
            case CinematicPropEntity.MOUSE_TRAPEZES->{
                if(e.cue()==-1)pose.scale(1,Math.max(.01F,e.value()),1);
                for(int i=0;i<8;i++)trapeze(pose,b,light,(i-3.5)*2.6,16,0,e.cue()==3?-Math.sin((e.value()*2+i/8.0)*2*Math.PI)*.9:0,8);
            }
            default->{}
        }
        pose.popPose();super.render(e,yaw,partial,pose,buffers,light);
    }
    private static void chair(PoseStack p,VertexConsumer b,int l){box(p,b,l,0,.3,0,1.1,.22,.85,RED);box(p,b,l,0,.52,.32,1.12,1.05,.18,RED);for(int s:new int[]{-1,1}){box(p,b,l,s*.61,.57,0,.12,.12,.95,GOLD);box(p,b,l,s*.48,0,0,.1,.30,.65,DARK);}box(p,b,l,0,1.575,.32,1.2,.1,.24,GOLD);}
    private static void dog(PoseStack p,VertexConsumer b,int l,double t,int index,int cue){
        double stagger=index*1.7,beat=Math.sin(t*.18+stagger),sway=cue==1?Math.sin(t*.10+stagger)*.018:0;
        p.pushPose();p.translate(0,cue==3?Math.max(0,beat)*.035:0,0);p.mulPose(Axis.ZP.rotation((float)sway));
        box(p,b,l,0,.53,0,.54,.48,.58,0x17131B);box(p,b,l,0,.76,-.2,.42,.55,.35,0x22202A);
        double tilt=Math.sin(t*.021+stagger)*.07;
        p.pushPose();p.translate(0,1.03,-.24);p.mulPose(Axis.YP.rotation((float)tilt));
        if(cue==2)p.mulPose(Axis.XP.rotation(.48F+(float)Math.sin(stagger)*.05F));
        box(p,b,l,0,0,0,.5,.4,.4,0x25222C);box(p,b,l,0,.01,-.25,.35,.18,.26,0x15131B);
        for(int side:new int[]{-1,1}){
            box(p,b,l,side*.23,.06,.02,.16,.4,.16,0x0E0D14);box(p,b,l,side*.14,.19,-.211,.13,.13,.035,0x050509);
            for(int q=0;q<4;q++)box(p,b,l,side*.14+(q%2-.5)*.04,.22-(q/2)*.04,-.238,.017,.017,.013,0xC2BBB7);
        }
        p.popPose();
        for(int side:new int[]{-1,1}){
            if(cue==3){p.pushPose();p.translate(side*(.15-.065*(beat+1)),.7,-.3);p.mulPose(Axis.XP.rotation(-.8F));box(p,b,l,0,0,0,.12,.3,.16,DARK);p.popPose();}
            else box(p,b,l,side*.15,.4,-.27,.12,.28,.16,DARK);
        }
        p.popPose();
    }
    public static void waterBucket(PoseStack p,MultiBufferSource buffers,int l,double time){
        var b=buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        for(int i=0;i<32;i++){double a=i*Math.PI/16;p.pushPose();p.translate(Math.cos(a)*1.32,0,Math.sin(a)*1.32);p.mulPose(Axis.YP.rotationDegrees((float)(-a*180/Math.PI)));box(p,b,l,0,0,0,.14,.84,.30,i%2==0?0xA84857:0xB65A63);box(p,b,l,0,.84,0,.20,.10,.31,CREAM);p.popPose();}
        box(p,b,15728880,0,.73,0,2.38,.035,2.38,0x59B6CC);
        for(int sign:new int[]{-1,1})rope(p,b,l,sign*1.32,.65,0,sign*1.14,2.02,0);rope(p,b,l,-1.14,2.02,0,1.14,2.02,0);
    }
    private static void trapeze(PoseStack p,VertexConsumer b,int l,double x,double y,double z,double a,double length){p.pushPose();p.translate(x,y,z);p.mulPose(Axis.XP.rotation((float)a));for(int side:new int[]{-1,1})box(p,b,l,side*.65,-length,0,.06,length,.06,CREAM);box(p,b,l,0,-length,0,1.5,.12,.12,GOLD);p.popPose();}
    private static void rope(PoseStack p,VertexConsumer b,int l,double x,double y,double z,double tx,double ty,double tz){var delta=new org.joml.Vector3f((float)(tx-x),(float)(ty-y),(float)(tz-z));float length=delta.length();if(length<.001F)return;p.pushPose();p.translate(x,y,z);p.mulPose(new org.joml.Quaternionf().rotationTo(new org.joml.Vector3f(0,1,0),delta.normalize()));box(p,b,l,0,0,0,.055,length,.055,CREAM);p.popPose();}
    private static final int[][] FACE_CORNERS={{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{3,7,6,2},{0,1,5,4}};
    private static final float[][] NORMALS={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
    static void box(PoseStack p,VertexConsumer v,int light,double x,double y,double z,double w,double h,double d,int rgb){
        float left=(float)(x-w/2),right=(float)(x+w/2),bottom=(float)y,top=(float)(y+h),back=(float)(z-d/2),front=(float)(z+d/2);
        var matrix=p.last().pose();var normal=p.last().normal();int red=(rgb>>16)&255,green=(rgb>>8)&255,blue=rgb&255;
        for(int face=0;face<6;face++)for(int corner=0;corner<4;corner++){int k=FACE_CORNERS[face][corner];float px=(k==1||k==2||k==5||k==6)?right:left,py=(k==2||k==3||k==6||k==7)?top:bottom,pz=k>=4?front:back;float[] n=NORMALS[face];v.vertex(matrix,px,py,pz).color(red,green,blue,255).uv(corner==0||corner==3?0:1,corner<2?0:1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal,n[0],n[1],n[2]).endVertex();}
    }
}
