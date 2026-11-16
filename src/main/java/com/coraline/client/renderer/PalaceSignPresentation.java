package com.coraline.client.renderer;

import com.coraline.feast.CinematicPropEntity;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;

/** Actual shaped double-sided wooden sign/raised trim and large native type,
 * not a flat screenshot or tiny default sign. Zero new texture/font assets.
 */
public final class PalaceSignPresentation {
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/white_concrete.png");
    private static void box(PoseStack p,VertexConsumer b,int light,double x,double y,double z,double w,double h,double d,int color){CinematicPropRenderer.box(p,b,light,x,y,z,w,h,d,color);}
    public static boolean render(CinematicPropEntity e,PoseStack p,MultiBufferSource buffers,int light){
        if(e.kind()!=CinematicPropEntity.WORLD_SIGN)return false;
        var b=buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));p.pushPose();p.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
        for(int side:new int[]{-1,1})box(p,b,light,side*3.7,-.3,.1,.32,7.4,.32,0x5F4C37);
        box(p,b,light,0,2.05,0,7.2,3.3,.20,0xB9AF87);
        for(int side:new int[]{-1,1}){
            box(p,b,light,side*3.55,2.0,0,.22,3.55,.36,0x87514C);
            for(int i=0;i<12;i++){double angle=i*Math.PI/13,x=side*(2.65+.65*Math.cos(angle)),y=5.26+.42*Math.sin(angle);box(p,b,light,x,y,-.01,.18,.18,.37,0x87514C);}
        }
        box(p,b,light,0,2,0,7.25,.18,.36,0x87514C);box(p,b,light,0,5.25,0,5.4,.18,.36,0x87514C);
        for(int side:new int[]{-1,1}){p.pushPose();if(side>0)p.mulPose(Axis.YP.rotationDegrees(180));p.translate(0,4.65,-.13);p.mulPose(Axis.YP.rotationDegrees(180));p.scale(.035F,-.035F,.035F);
            var font=Minecraft.getInstance().font;String title=e.slot()==0?"Pink Palace":"Moving Company",subtitle=e.slot()==0?"apartments":"office & loading";
            float scale=Math.min(1,182F/Math.max(1,font.width(title)));p.pushPose();p.scale(scale,scale,1);font.drawInBatch(title,-font.width(title)/2F,0,0xFF81443E,false,p.last().pose(),buffers,Font.DisplayMode.NORMAL,0,light);p.popPose();
            font.drawInBatch(subtitle,-font.width(subtitle)/2F,31,0xFF81443E,false,p.last().pose(),buffers,Font.DisplayMode.NORMAL,0,light);p.popPose();
        }
        p.popPose();return true;
    }
    private PalaceSignPresentation(){}
}
