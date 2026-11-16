package com.coraline.client.renderer;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

/** Beveled, irregular triangular mineral annulus. Every central ray is EMPTY, including the GUI view. */
public final class SeeingStoneRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/white_concrete.png");
    public SeeingStoneRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(Axis.YP.rotationDegrees(context==ItemDisplayContext.GUI?-18:12));pose.mulPose(Axis.XP.rotationDegrees(context==ItemDisplayContext.GUI?12:-8));float scale=context==ItemDisplayContext.GUI?1.05F:.8F;pose.scale(scale,scale,scale);stone(pose,buffers,light);pose.popPose();}
    public static double outer(double theta){return .365*(1+.22*Math.cos(3*theta+.42)+.027*Math.sin(5*theta));}
    public static double inner(double theta){return .145*(1+.055*Math.sin(theta+.4));}
    /** Raised stone is physically pinched at its lower rim, outside the aperture. */
    public static void grasp(net.minecraft.client.player.AbstractClientPlayer player,net.minecraft.world.entity.HumanoidArm arm,PoseStack pose,MultiBufferSource buffers,int light){
        int side=arm==net.minecraft.world.entity.HumanoidArm.RIGHT?1:-1;
        // Render the actual UV-mapped player arm and sleeve. Sampling one skin
        // texel for the entire procedural hand turned many custom skins black.
        var renderer=Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if(!(renderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer nativeArm))return;
        pose.pushPose();pose.translate(side*.36,-.34,.13);pose.mulPose(Axis.ZP.rotationDegrees(side*-18));pose.scale(.62F,.62F,.62F);
        try{if(side>0)nativeArm.renderRightHand(pose,buffers,light,player);else nativeArm.renderLeftHand(pose,buffers,light,player);}
        finally{pose.popPose();}
    }
    private static void joint(PoseStack pose,VertexConsumer out,int light,double[] from,double[] to,double radius,float u,float v){
        var direction=new org.joml.Vector3f((float)(to[0]-from[0]),(float)(to[1]-from[1]),(float)(to[2]-from[2]));
        float length=direction.length();if(length<1e-5)return;direction.div(length);
        pose.pushPose();pose.translate(from[0],from[1],from[2]);pose.mulPose(new org.joml.Quaternionf().rotationTo(new org.joml.Vector3f(0,1,0),direction));
        for(int segment=0;segment<8;segment++){
            double a=segment*Math.PI/4,b=(segment+1)*Math.PI/4;
            for(double[] point:new double[][]{{a,0},{a,length},{b,length},{b,0}})
                out.vertex(pose.last().pose(),(float)(Math.cos(point[0])*radius),(float)point[1],(float)(Math.sin(point[0])*radius)).color(255,255,255,255).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.last().normal(),(float)Math.cos(point[0]),0,(float)Math.sin(point[0])).endVertex();
            for(int cap=0;cap<2;cap++)for(double angle:new double[]{Double.NaN,a,b,Double.NaN})
                out.vertex(pose.last().pose(),Double.isNaN(angle)?0:(float)(Math.cos(angle)*radius),cap*length,Double.isNaN(angle)?0:(float)(Math.sin(angle)*radius)).color(255,255,255,255).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.last().normal(),0,cap==0?-1:1,0).endVertex();
        }
        pose.popPose();
    }
    /** Cross-section rounds both the outside rim and the inside wall without ever capping the hole. */
    private static double[] point(double a,int band){double in=inner(a),out=outer(a),r=switch(band){case 0,5->in+.025;case 1,4->out-.035;case 2,3->out;default->in;};double z=switch(band){case 0,1->.075;case 2,7->.035;case 3,6->-.035;default->-.075;};return new double[]{Math.cos(a)*r,Math.sin(a)*r,z};}
    public static void stone(PoseStack p,MultiBufferSource buffers,int light){var b=buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));for(int i=0;i<80;i++)for(int band=0;band<8;band++){double a=i*Math.PI*2/80,c=(i+1)*Math.PI*2/80;var v0=point(a,band);var v1=point(c,band);var v2=point(c,(band+1)%8);var v3=point(a,(band+1)%8);double[] u={v1[0]-v0[0],v1[1]-v0[1],v1[2]-v0[2]},v={v3[0]-v0[0],v3[1]-v0[1],v3[2]-v0[2]};double nx=u[1]*v[2]-u[2]*v[1],ny=u[2]*v[0]-u[0]*v[2],nz=u[0]*v[1]-u[1]*v[0],len=Math.sqrt(nx*nx+ny*ny+nz*nz);if(len<1e-8)continue;nx/=len;ny/=len;nz/=len;
            for(var xyz:new double[][]{v0,v1,v2,v3}){double marble=Math.sin(xyz[0]*61+Math.sin(xyz[1]*27)*2)+.5*Math.sin(xyz[1]*83+xyz[0]*12),highlight=Math.pow(Math.max(0,Math.cos(a-2.1)),24)*(band==0||band==1?.75:.08);float green=(float)Math.min(1,.34+.22*(marble+1.5)/3+highlight),red=(float)Math.min(.8,.012+highlight*.55),blue=(float)Math.min(.7,.065+highlight*.65);b.vertex(p.last().pose(),(float)xyz[0],(float)xyz[1],(float)xyz[2]).color(red,green,blue,1).uv(.5F,.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.last().normal(),(float)-nx,(float)-ny,(float)-nz).endVertex();}}
    }
}
