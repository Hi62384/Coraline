package com.coraline.client.renderer;
import com.coraline.entity.CharacterEntity;
import com.coraline.wybie.WybieVisitorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
public final class WybieVisitorRenderer extends MobRenderer<CharacterEntity,CharacterModel>{
    private static final ResourceLocation ATLAS=ResourceLocation.fromNamespaceAndPath("coraline","characters/cast_atlas.png");
    public WybieVisitorRenderer(EntityRendererProvider.Context context){super(context,new CharacterModel("wybie"),.45F);addLayer(new Props(this));}
    @Override public ResourceLocation getTextureLocation(CharacterEntity actor){return ATLAS;}
    private static final class Props extends RenderLayer<CharacterEntity,CharacterModel>{
        private final CharacterModel slug=new CharacterModel("wybie_slug"),bike=new CharacterModel("wybie_bicycle");
        Props(WybieVisitorRenderer parent){super(parent);}
        @Override public void render(PoseStack pose,MultiBufferSource buffer,int light,CharacterEntity actor,float swing,float amount,float partial,float age,float yaw,float pitch){
            if(!(actor instanceof WybieVisitorEntity v))return;
            var consumer=buffer.getBuffer(RenderType.entityCutoutNoCull(ATLAS));
            if(v.hasSlug()||v.phase()==WybieVisitorEntity.THROW&&v.elapsed()<22){
                pose.pushPose();
                if(v.phase()==WybieVisitorEntity.I){getParentModel().transformBone("head",pose);pose.translate(0,-2.3/16,-3.9/16);pose.scale(1.3F,.75F,1);}
                else if(v.phase()==WybieVisitorEntity.G){getParentModel().transformBone("head",pose);pose.translate(0,-2.85/16,-4.1/16);pose.mulPose(Axis.YP.rotationDegrees(90));pose.scale(1.8F*(1+Math.min(1,v.elapsed()/20F)),.8775F,1.35F);pose.translate(2.2/16,0,0);}
                else if(v.phase()==WybieVisitorEntity.THROW&&v.elapsed()>=12){getParentModel().transformBone("body",pose);float t=(v.elapsed()-12+partial)/10;pose.translate(-.3-t*.7,-.5+t*.3,-.5-t*.8);pose.mulPose(Axis.ZP.rotationDegrees(t*270));}
                else{getParentModel().transformBone("right_forearm",pose);pose.translate(0,6.6/16,-1.1/16);pose.mulPose(Axis.XP.rotationDegrees(10));pose.mulPose(Axis.ZP.rotationDegrees(-12));}
                if(v.phase()!=WybieVisitorEntity.G)pose.scale(1.8F,1.35F,1.35F);
                slug.renderToBuffer(pose,consumer,light,OverlayTexture.NO_OVERLAY,1,1,1,1);pose.popPose();
            }
            if(v.biking()){pose.pushPose();pose.translate(0,24/16F,0);bike.renderToBuffer(pose,consumer,light,OverlayTexture.NO_OVERLAY,1,1,1,1);pose.popPose();}
        }
    }
}
