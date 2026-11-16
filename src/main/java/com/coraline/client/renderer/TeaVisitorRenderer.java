package com.coraline.client.renderer;
import com.coraline.entity.CharacterEntity;
import com.coraline.tea.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
public final class TeaVisitorRenderer extends MobRenderer<CharacterEntity,CharacterModel>{
    private static final ResourceLocation ATLAS=ResourceLocation.fromNamespaceAndPath("coraline","characters/cast_atlas.png");
    public TeaVisitorRenderer(EntityRendererProvider.Context context,String id){super(context,new CharacterModel(id),.5F);addLayer(new Cup(this,context));}
    @Override public ResourceLocation getTextureLocation(CharacterEntity actor){return ATLAS;}
    private static final class Cup extends RenderLayer<CharacterEntity,CharacterModel>{
        private final CharacterModel cup=new CharacterModel("tea_cup");Cup(TeaVisitorRenderer parent,EntityRendererProvider.Context context){super(parent);}
        @Override public void render(PoseStack pose,MultiBufferSource buffer,int light,CharacterEntity actor,float swing,float amount,float partial,float age,float yaw,float pitch){
            if(!(actor instanceof TeaVisitorEntity tea))return;boolean spink=tea.characterId().equals("miss_spink");int phase=tea.phase();
            if(phase>=100){MissingParentsTeaPresentation.held(tea,getParentModel(),pose,buffer,light,partial);return;}
            boolean holds=tea.hasCup();
            if(!holds)return;pose.pushPose();
            // Center the upright cup between both cupped hands, not on a rotating item plane.
            var left=new PoseStack();var right=new PoseStack();getParentModel().transformBone("left_forearm",left);getParentModel().transformBone("right_forearm",right);float hand=spink?4.9F:5.4F;var a=left.last().pose().transformPosition(new org.joml.Vector3f(0,hand/16,-.05F));var b=right.last().pose().transformPosition(new org.joml.Vector3f(0,hand/16,-.05F));a.add(b).mul(.5F);pose.translate(a.x,a.y+.06,a.z);
            if(phase==TeaVisit.SWIRL){float t=(tea.elapsed()+partial)*.3F;pose.mulPose(Axis.ZP.rotationDegrees((float)Math.sin(t)*5));}
            pose.scale(1.35F,1.35F,1.35F);cup.renderToBuffer(pose,buffer.getBuffer(RenderType.entityCutoutNoCull(ATLAS)),light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,1,1,1,1);pose.popPose();
        }
    }
}
