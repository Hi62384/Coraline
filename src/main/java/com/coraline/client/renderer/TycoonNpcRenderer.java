package com.coraline.client.renderer;
import com.coraline.tycoon.TycoonNpc;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
/** Reusable procedural humanoid components, not reskinned identical named villagers. */
public final class TycoonNpcRenderer extends EntityRenderer<TycoonNpc> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/white_concrete.png");
    public TycoonNpcRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=.3F;}
    @Override public ResourceLocation getTextureLocation(TycoonNpc a){return TEXTURE;}
    public static void box(PoseStack p,com.mojang.blaze3d.vertex.VertexConsumer out,int light,float x,float y,float z,float w,float h,float d,int color){float[][] points={{x,y,z},{x+w,y,z},{x+w,y+h,z},{x,y+h,z},{x,y,z+d},{x+w,y,z+d},{x+w,y+h,z+d},{x,y+h,z+d}};int[][] faces={{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{0,1,5,4},{3,7,6,2}};float[][] normals={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,-1,0},{0,1,0}};for(int f=0;f<6;f++)for(int i=0;i<4;i++){var v=points[faces[f][i]];var n=normals[f];out.vertex(p.last().pose(),v[0],v[1],v[2]).color((color>>16)&255,(color>>8)&255,color&255,255).uv(i==0||i==3?0:1,i<2?1:0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.last().normal(),n[0],n[1],n[2]).endVertex();}}
    private static void limb(PoseStack p,com.mojang.blaze3d.vertex.VertexConsumer b,int light,float x,float y,float z,float angle,float length,int color){p.pushPose();p.translate(x,y,z);p.mulPose(Axis.XP.rotationDegrees(angle));box(p,b,light,-.06F,-length,-.06F,.12F,length,.12F,color);p.popPose();}
    @Override public void render(TycoonNpc a,float yaw,float partial,PoseStack p,MultiBufferSource buffers,int light){p.pushPose();p.mulPose(Axis.YP.rotationDegrees(180-yaw));int seed=a.appearance();boolean staff=a.staff();float size=staff?1.2F:.90F+(seed%5)*.035F;p.scale(size,size,size);int skin=new int[]{0xE1B994,0xBA845E,0x835C43,0xF1D2BA,0xA66D4B}[(seed/5)%5],hair=new int[]{0x392A25,0xBB8139,0x251F25,0x77482D,0xD8C39E}[(seed/25)%5],cloth=staff?0x333D56:new int[]{0x557892,0xAB6679,0x708657,0xAC935B,0x786894,0x477F77}[(seed/125)%6];var out=buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));int gesture=a.gesture();if(gesture==4||gesture==6){p.translate(0,.22,0);p.mulPose(Axis.XP.rotationDegrees(a.getPose()==net.minecraft.world.entity.Pose.SWIMMING?-82:82));}if(gesture==3||a.isPassenger())p.translate(0,-.3,0);
        if(a.ritual()>80){float dissolve=Math.max(.02F,(160-a.ritual()-partial*20)/80F);p.translate(0,(1-dissolve)*1.2,0);p.scale(dissolve,dissolve,dissolve);}
        float stride=(float)Math.sin(a.walkAnimation.position(partial)*.8F)*a.walkAnimation.speed(partial)*35;
        limb(p,out,light,-.105F,.55F,0,gesture==3||a.isPassenger()?85:stride,.55F,staff?0x272B3A:0x39465A);limb(p,out,light,.105F,.55F,0,gesture==3||a.isPassenger()?85:-stride,.55F,staff?0x272B3A:0x39465A);box(p,out,light,-.21F,.52F,-.12F,.42F,.46F,.24F,cloth);
        float arm=gesture==1||gesture==7?105:gesture==8?80:gesture==9?135:gesture==2?50:-stride;float otherArm=gesture==2?35:stride;limb(p,out,light,-.27F,.93F,0,arm,.43F,skin);limb(p,out,light,.27F,.93F,0,otherArm,.43F,skin);
        p.pushPose();p.translate(0,1,0);p.mulPose(Axis.YP.rotationDegrees(-net.minecraft.util.Mth.wrapDegrees(net.minecraft.util.Mth.rotLerp(partial,a.yHeadRotO,a.getYHeadRot())-yaw)));p.mulPose(Axis.XP.rotationDegrees(-net.minecraft.util.Mth.lerp(partial,a.xRotO,a.getXRot())));p.translate(0,-1,0);
        float width=.39F+(seed%3)*.018F;box(p,out,light,-width/2,.99F,-.17F,width,.43F,.34F,skin);box(p,out,light,-width/2-.015F,1.37F,-.18F,width+.03F,.09F,.36F,hair);int hairstyle=(seed/750)%4;if(hairstyle==1||hairstyle==3){box(p,out,light,-width/2-.04F,1.08F,.08F,.08F,.34F,.14F,hair);box(p,out,light,width/2-.04F,1.08F,.08F,.08F,.34F,.14F,hair);}if(hairstyle==2){box(p,out,light,-width/2-.09F,1.16F,0,.12F,.2F,.2F,hair);box(p,out,light,width/2-.03F,1.16F,0,.12F,.2F,.2F,hair);}
        for(float x:new float[]{-.09F,.09F}){box(p,out,light,x-.025F,1.22F,-.181F,.05F,.065F,.02F,0x222633);box(p,out,light,x-.01F,1.26F,-.19F,.013F,.018F,.01F,0xF0F4F1);
            if(a.ritual()>=40){box(p,out,light,x-.056F,1.202F,-.22F,.112F,.112F,.035F,0x131318);for(float hx:new float[]{-.022F,.022F})for(float hy:new float[]{.03F,.075F})box(p,out,light,x+hx-.007F,1.202F+hy,-.231F,.014F,.014F,.012F,0x918D80);}
        }box(p,out,light,-.055F,1.095F,-.185F,.11F,.015F,.012F,gesture==5?0x7B4641:0x6B453D);p.popPose();if(staff){box(p,out,light,-.03F,.74F,-.135F,.06F,.19F,.02F,0xA37B59);box(p,out,light,.09F,.81F,-.135F,.09F,.06F,.02F,0xDCCBA8);}else if((seed/3000)%2==1)box(p,out,light,-.18F,.65F,-.135F,.36F,.04F,.02F,0xE9D6A4);
        held(a,gesture==7?new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.POTION):a.getMainHandItem(),p,buffers,light,-.27F,arm);
        held(a,a.getOffhandItem(),p,buffers,light,.27F,otherArm);
        p.popPose();if(!com.coraline.client.TycoonFoodOverlay.labels(a,p,buffers,light))super.render(a,yaw,partial,p,buffers,light);}
    private static void held(TycoonNpc actor,net.minecraft.world.item.ItemStack item,PoseStack pose,MultiBufferSource buffers,int light,float side,float angle){
        if(item.isEmpty())return;pose.pushPose();pose.translate(side,.93,0);pose.mulPose(Axis.XP.rotationDegrees(angle));pose.translate(0,-.43,0);pose.mulPose(Axis.XP.rotationDegrees(-angle));pose.scale(.32F,.32F,.32F);
        net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(item,net.minecraft.world.item.ItemDisplayContext.FIXED,light,OverlayTexture.NO_OVERLAY,pose,buffers,actor.level(),actor.getId());pose.popPose();
    }
}
