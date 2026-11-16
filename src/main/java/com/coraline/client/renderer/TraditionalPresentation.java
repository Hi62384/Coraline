package com.coraline.client.renderer;

import com.coraline.client.TraditionalClient;
import com.coraline.feast.*;
import com.coraline.parents.ParentEntity;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Model-space props and localized facial deformation, with no runtime image masters. */
public final class TraditionalPresentation {
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/white_concrete.png");
    private static VertexConsumer consumer(MultiBufferSource buffers){return buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));}
    private static void box(PoseStack p,VertexConsumer b,int l,double x,double y,double z,double w,double h,double d,int color){CinematicPropRenderer.box(p,b,l,x,y,z,w,h,d,color);}
    public static void gift(CinematicPropEntity e,PoseStack p,MultiBufferSource buffers,int light,float partial){
        var b=consumer(buffers);boolean opened=e.value()>0;String phase=TraditionalClient.phase();int time=TraditionalClient.time();
        // Tilt the open tray toward the seated player instead of hiding the
        // buttons below its front rim at first-person eye height.
        p.translate(0,e.value()*.09,0);p.mulPose(Axis.XP.rotationDegrees(e.value()*-24));
        box(p,b,light,0,0,0,.62,.035,.46,0x3C263F);for(double z:new double[]{-.215,.215})box(p,b,light,0,.035,z,.62,.17,.035,0x4E3658);for(double x:new double[]{-.295,.295})box(p,b,light,x,.035,0,.03,.17,.4,0x4E3658);
        box(p,b,light,0,.037,0,.55,.009,.38,0xB39B9F);
        p.pushPose();p.translate(0,.21,.23);float opening=e.value();p.mulPose(Axis.XP.rotationDegrees(opening*112));box(p,b,light,0,0,-.23,.66,.035,.49,0xD8C9B7);box(p,b,light,0,.036,-.23,.075,.008,.49,0x6F344C);box(p,b,light,0,.036,-.23,.66,.008,.055,0x6F344C);for(int side:new int[]{-1,1}){p.pushPose();p.translate(side*.08,.05,-.23);p.mulPose(Axis.ZP.rotationDegrees(side*24));box(p,b,light,0,0,0,.17,.055,.07,0x79435A);p.popPose();}p.popPose();
        if(!opened)return;
        int color=phase.equals("G")&&time<20?0xB5D628:0x111017;
        for(double x:new double[]{-.13,.10}){p.pushPose();p.translate(x,.067,-.045);p.mulPose(Axis.XP.rotationDegrees(-90));button(p,b,light,.068,color);p.popPose();}
        box(p,b,light,.20,.05,.10,.07,.075,.07,0xD7C8A8);for(int i=0;i<5;i++)box(p,b,light,.20,.06+i*.009,.10,.075,.004,.078,0x7F6273);
        boolean taken=phase.equals("J")&&time<VisitAudio.length("black_is_traditional_j")-20;
        if(!taken){double height=phase.equals("J")?Math.max(0,.32*(1-(time-(VisitAudio.length("black_is_traditional_j")-20))/12.)):0;p.pushPose();p.translate(-.07,.055+height,.12);p.mulPose(Axis.YP.rotationDegrees(28));box(p,b,light,0,0,0,.2,.006,.008,0xC0C9D1);p.popPose();}
        for(int i=0;i<7;i++)box(p,b,light,.14-i*.017,.052,.09+Math.sin(i)*.02,.025,.003,.004,0x8B7785);
    }
    /** Black discs have real depth and four separately modeled thread holes. */
    static void button(PoseStack p,VertexConsumer b,int light,double radius,int color){
        for(int i=0;i<24;i++){double a=i*Math.PI/12,c=(i+1)*Math.PI/12;float x=(float)(Math.cos(a)*radius),y=(float)(Math.sin(a)*radius),xx=(float)(Math.cos(c)*radius),yy=(float)(Math.sin(c)*radius);
            for(int side:new int[]{-1,1})for(float[] vertex:new float[][]{{0,0},{x,y},{xx,yy},{0,0}})coloredVertex(p,b,light,vertex[0],vertex[1],side*.009F,color,0,0,side);
            coloredVertex(p,b,light,x,y,-.009F,color,x,y,0);coloredVertex(p,b,light,x,y,.009F,color,x,y,0);coloredVertex(p,b,light,xx,yy,.009F,color,xx,yy,0);coloredVertex(p,b,light,xx,yy,-.009F,color,xx,yy,0);
        }
        for(int sx:new int[]{-1,1})for(int sy:new int[]{-1,1})box(p,b,light,sx*radius*.25,sy*radius*.25,-.011,radius*.18,radius*.18,.004,0x7F776A);
    }
    private static void coloredVertex(PoseStack p,VertexConsumer b,int l,float x,float y,float z,int color,float nx,float ny,float nz){b.vertex(p.last().pose(),x,y,z).color((color>>16)&255,(color>>8)&255,color&255,255).uv(.5F,.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(l).normal(p.last().normal(),nx,ny,nz).endVertex();}
    private static void rod(PoseStack p,VertexConsumer b,int light,Vec3 from,Vec3 to,double width,int color){var d=to.subtract(from);if(d.lengthSqr()<.000001)return;p.pushPose();p.translate(from.x,from.y,from.z);p.mulPose(new Quaternionf().rotationTo(new Vector3f(0,1,0),new Vector3f((float)d.x,(float)d.y,(float)d.z).normalize()));box(p,b,light,0,0,0,width,d.length(),width,color);p.popPose();}
    public static void hand(CinematicPropEntity e,PoseStack p,MultiBufferSource buffers,int light,float partial){
        float amount=e.value();if(amount<=0)return;var level=Minecraft.getInstance().level;var father=level==null?null:level.getEntity(TraditionalClient.father());if(father==null)return;var b=consumer(buffers);
        Vec3 origin=new Vec3(.2,1.05,.5),head=father.getEyePosition(partial).subtract(e.position());var forward=new Vec3(-Math.sin(Math.toRadians(father.getYRot())),0,Math.cos(Math.toRadians(father.getYRot())));
        Vec3 target=head.add(forward.scale(.30)).add(0,e.cue()==1?-.12:.03,0),palm=origin.lerp(target,amount),elbow=new Vec3(palm.x*.5,Math.max(1.35,palm.y+.25),palm.z*.35);
        rod(p,b,light,origin,elbow,.065,0x797F82);rod(p,b,light,elbow,palm,.055,0xA5ABAD);box(p,b,light,elbow.x,elbow.y-.055,elbow.z,.12,.11,.12,0x403F45);
        p.pushPose();p.translate(palm.x,palm.y,palm.z);p.mulPose(Axis.YP.rotationDegrees(-father.getYRot()));double scale=e.cue()==2?1.45:1;box(p,b,light,0,-.09,0,.34*scale,.22*scale,.09,0xA9AA9F);
        for(int finger=0;finger<4;finger++){double x=(finger-1.5)*.085*scale;Vec3 joint=new Vec3(x,.10*scale,0);for(int segment=0;segment<3;segment++){Vec3 next=joint.add(0,.105*scale,-.025*segment*amount);rod(p,b,light,joint,next,.043*scale,segment%2==0?0xB3B7AF:0x686C6D);box(p,b,light,next.x,next.y,next.z,.055*scale,.035*scale,.055*scale,0x414346);joint=next;}}
        rod(p,b,light,new Vec3(-.16*scale,-.025,0),new Vec3(-.26*scale,.05,-.04),.055*scale,0xA9AA9F);p.popPose();
    }
    public static void parent(ParentEntity actor,PlayerModel<ParentEntity> model,PoseStack p,MultiBufferSource buffers,int light,ResourceLocation skin){
        int color=TraditionalClient.eyeColor(actor.getId());boolean stretch=TraditionalClient.stretched(actor.getId());
        if(stretch){p.pushPose();model.head.translateAndRotate(p);var b=buffers.getBuffer(RenderType.entityCutoutNoCull(skin));float bottom=TraditionalClient.stretch()*.82F;
            // Only the central two mouth rows deform. The original head, nose,
            // cheeks, hair and button-eye layers remain rendered at normal size.
            float[][] mouth={{-.125F,-.125F},{-.125F,bottom},{.125F,bottom},{.125F,-.125F}};
            for(int i=0;i<4;i++)b.vertex(p.last().pose(),mouth[i][0],mouth[i][1],-.257F).color(255,255,255,255).uv((i<2?10:14)/64F,(i==0||i==3?14:16)/64F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.last().normal(),0,0,-1).endVertex();
            p.popPose();
        }
        if(color!=0){p.pushPose();model.head.translateAndRotate(p);var b=consumer(buffers);for(double side:new double[]{-.13,.13}){p.pushPose();p.translate(side,-.32,-.301);button(p,b,15728880,.10,color);p.popPose();}p.popPose();}
        if(TraditionalClient.needleHeld(actor.getId())){p.pushPose();model.rightArm.translateAndRotate(p);var b=consumer(buffers);box(p,b,light,-.045,.68,-.08,.006,.20,.006,0xD2DBDE);box(p,b,light,-.045,.84,-.08,.018,.025,.008,0xB0BDC3);p.popPose();}
    }
    private static void skinCap(PoseStack p,VertexConsumer b,int light,float y,boolean bottom){
        float[][] corners={{-.25F,-.25F},{.25F,-.25F},{.25F,.25F},{-.25F,.25F}};
        for(int i=0;i<4;i++){int j=bottom?3-i:i;var c=corners[j];b.vertex(p.last().pose(),c[0],y,c[1]).color(255,255,255,255).uv(((bottom?16:8)+(c[0]+.25F)*16)/64F, (c[1]+.25F)*16/64F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.last().normal(),0,bottom?1:-1,0).endVertex();}
    }
    private static void skinBand(PoseStack p,VertexConsumer b,int light,float y0,float y1,int v0,int v1){
        float[][] xz={{-.25F,-.25F},{.25F,-.25F},{.25F,.25F},{-.25F,.25F}};
        for(int face=0;face<4;face++){int next=(face+1)%4,u=face==0?8:face==1?16:face==2?24:0;float[][] points={{xz[face][0],y0,xz[face][1]},{xz[face][0],y1,xz[face][1]},{xz[next][0],y1,xz[next][1]},{xz[next][0],y0,xz[next][1]}};for(int i=0;i<4;i++)b.vertex(p.last().pose(),points[i][0],points[i][1],points[i][2]).color(255,255,255,255).uv((u+(i>=2?8:0))/64F,(i==0||i==3?v0:v1)/64F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.last().normal(),face==1?1:face==3?-1:0,0,face==0?-1:face==2?1:0).endVertex();}
    }
    private TraditionalPresentation(){}
}
