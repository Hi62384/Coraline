package com.coraline.client.renderer;

import com.coraline.feast.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Four distinct articulated workers and a real enclosed, furnished truck mesh.
 * All geometry is bounded native code; no flat vehicle image/new bitmap assets.
 * Contact, interior camera, collision and reference fidelity need native review.
 */
public final class PalaceTransportPresentation {
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/white_concrete.png");
    private static void box(PoseStack p,VertexConsumer b,int l,double x,double y,double z,double w,double h,double d,int color){CinematicPropRenderer.box(p,b,l,x,y,z,w,h,d,color);}
    private static void oval(PoseStack p,VertexConsumer b,int l,double x,double y,double z,double rx,double ry,double rz,int color){FirstEscapePresentation.sphere(p,b,l,x,y,z,rx,ry,rz,color,1);}
    private static void bar(PoseStack p,VertexConsumer b,int l,Vec3 a,Vec3 end,double w,int color){var d=end.subtract(a);if(d.lengthSqr()<.00001)return;p.pushPose();p.translate(a.x,a.y,a.z);p.mulPose(new org.joml.Quaternionf().rotationTo(new org.joml.Vector3f(0,1,0),new org.joml.Vector3f((float)d.x,(float)d.y,(float)d.z).normalize()));box(p,b,l,0,0,0,w,d.length(),w,color);p.popPose();}
    public static boolean render(CinematicPropEntity e,PoseStack p,MultiBufferSource buffers,int light,float partial){
        if(e.kind()!=PalaceTransport.MOVER&&e.kind()!=PalaceTransport.TRUCK&&e.kind()!=PalaceTransport.CARRIER)return false;
        if(e.kind()==PalaceTransport.CARRIER)return true;
        if(Minecraft.getInstance().player==null)return true;
        var b=buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));p.pushPose();p.mulPose(Axis.YP.rotationDegrees(e.kind()==PalaceTransport.MOVER?180-e.getYRot():-e.getYRot()));
        if(e.kind()==PalaceTransport.MOVER)worker(e,p,b,light,partial);else truck(e,p,b,buffers,light,partial);
        p.popPose();return true;
    }
    private static void worker(CinematicPropEntity e,PoseStack p,VertexConsumer b,int l,float partial){
        int variant=Math.floorMod(e.slot(),4),skin=new int[]{0xC99977,0x805640,0xDFB594,0xA87955}[variant],shirt=new int[]{0x486B71,0x3C5667,0x687A69,0x596272}[variant];
        double scale=new double[]{.94,1.07,1.0,.98}[variant],width=new double[]{1.12,.91,1.02,1.07}[variant];p.scale((float)width,(float)scale,1);
        double time=e.visualTime(partial),walk=Math.sin(time*.22)*.22*Math.min(1,e.value()),carry=e.cue()>0?1:0;
        box(p,b,l,0,.75,0,.57,.62,.32,shirt);box(p,b,l,0,.63,0,.55,.15,.32,0x3B3735);box(p,b,l,0,.65,-.18,.10,.07,.035,0xB99A58);
        oval(p,b,l,0,1.62,0,.24,.29,.21,skin);box(p,b,l,0,1.38,0,.15,.11,.15,skin);
        box(p,b,l,0,1.84,0,.50,.10,.45,0x263C43);box(p,b,l,0,1.82,-.20,.53,.04,.21,0x263C43);
        for(int side:new int[]{-1,1}){
            box(p,b,l,side*.095,1.66,-.198,.046,.035,.023,0x25201E);oval(p,b,l,side*.235,1.61,0,.045,.075,.035,skin);
            var hip=new Vec3(side*.17,.76,0);var knee=new Vec3(side*.17,.43,side*walk);var foot=new Vec3(side*.17,.10,-side*walk);
            bar(p,b,l,hip,knee,.19,0x384B53);bar(p,b,l,knee,foot,.16,0x384B53);box(p,b,l,foot.x,0,foot.z-.04,.23,.15,.36,0x302C28);
            var shoulder=new Vec3(side*.30,1.29,0);var elbow=new Vec3(side*(carry>0?.45:.40),carry>0?1.03:.91,carry>0?-.26:side*walk);
            double toss=e.cue()==3?Math.sin(Math.min(1,time/30)*Math.PI)*.4:0;
            var hand=new Vec3(side*(carry>0?.38:.40),e.cue()==4?1.75:carry>0?1.18+toss:.69,carry>0?-.63:-side*walk);
            bar(p,b,l,shoulder,elbow,.18,shirt);bar(p,b,l,elbow,hand,.13,skin);oval(p,b,l,hand.x,hand.y,hand.z,.10,.075,.115,0xC9BEA4);
        }
        oval(p,b,l,0,1.61,-.225,.055,.06,.06,skin);box(p,b,l,0,1.48,-.20,.12,.02,.02,0x623D32);
        if(variant==0||variant==3)box(p,b,l,0,1.53,-.22,.16,.035,.035,0x493A31);
    }
    private static void truck(CinematicPropEntity e,PoseStack p,VertexConsumer b,MultiBufferSource buffers,int l,float partial){
        double time=e.visualTime(partial),open=Math.max(0,Math.min(1,e.value()));
        if(e.cue()==2&&time>=60)p.mulPose(Axis.ZP.rotationDegrees((float)(Math.sin(time*.8)*.32)));
        box(p,b,l,0,.48,0,3.55,.23,8.6,0x393C3D);box(p,b,l,0,.75,1.65,3.75,.16,5.55,0x756047);
        for(int side:new int[]{-1,1}){
            box(p,b,l,side*1.86,.90,1.65,.14,2.65,5.6,0xB7B09A);box(p,b,l,side*1.88,1.4,1.65,.04,.42,5.55,0x426B70);
            for(double z:new double[]{-3.0,2.9}){oval(p,b,l,side*1.68,.43,z,.20,.47,.47,0x29292A);oval(p,b,l,side*1.90,.43,z,.035,.22,.22,0x9A9B91);}
            p.pushPose();p.translate(side*1.85,.89,4.45);p.mulPose(Axis.YP.rotationDegrees((float)(-side*open*105)));box(p,b,l,-side*.925,0,0,1.85,2.67,.15,0xB7B09A);box(p,b,l,-side*.12,.9,-.10,.045,.56,.04,0x4D4A40);p.popPose();
        }
        box(p,b,l,0,3.55,1.65,3.9,.15,5.72,0xDED5BA);box(p,b,l,0,.9,-1.15,3.75,2.65,.16,0xAFA38D);
        // Old-fashioned separate cab, bonnet, windshield, bumper and lamps.
        box(p,b,l,0,.68,-3.1,3.28,1.55,2.6,0x426B70);box(p,b,l,0,2.19,-2.8,3.30,.13,2.1,0x35575E);
        box(p,b,l,0,1.55,-4.42,2.7,.60,.06,0xA8C0C1);box(p,b,l,0,.62,-4.46,3.5,.16,.12,0xA4A49A);box(p,b,l,0,.86,-4.44,1.35,.46,.07,0x3E4646);
        for(int side:new int[]{-1,1})oval(p,b,15728880,side*1.16,1.03,-4.47,.18,.18,.035,0xECD99A);
        // Actual cargo silhouette leaves a clear central standing/looking pocket.
        box(p,b,l,-1.24,.92,1.1,.80,.55,1.95,0x63716C);box(p,b,l,-1.56,1.18,1.1,.24,.90,2,0x6A7872);box(p,b,l,-1.24,1.45,.22,.80,.22,.24,0x7B8780);
        box(p,b,l,1.25,.91,2.9,.75,.8,.86,0xA88C65);box(p,b,l,1.25,1.72,2.9,.70,.64,.80,0xB19C76);
        box(p,b,l,-1.15,.9,-.48,.85,1.8,.72,0x806347);box(p,b,l,-1.15,1.8,-.87,.70,.035,.025,0xBDA783);
        box(p,b,l,.95,1.48,.35,.78,.10,.95,0x8E7352);for(double z:new double[]{-.02,.72})for(double x:new double[]{.62,1.28})box(p,b,l,x,.9,z,.08,.59,.08,0x6A513C);
        box(p,b,l,0,.85,2,1.1,.025,3.15,0x8A4E43);bar(p,b,l,new Vec3(-1.72,2.9,-.7),new Vec3(1.72,2.9,3.8),.035,0xB7A476);
        for(int side:new int[]{-1,1}){p.pushPose();p.translate(side*1.96,2.7,1.7);p.mulPose(Axis.YP.rotationDegrees(side<0?90:-90));p.scale(.023F,-.023F,.023F);var font=Minecraft.getInstance().font;String name="Palace Removals";font.drawInBatch(name,-font.width(name)/2F,0,0xFF35575E,false,p.last().pose(),buffers,Font.DisplayMode.NORMAL,0,l);p.popPose();}
    }
    private PalaceTransportPresentation(){}
}
