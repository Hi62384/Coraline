package com.coraline.client.renderer;
import com.coraline.entity.CharacterEntity;
import com.coraline.bobinsky.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
public final class BobinskyVisitorRenderer extends MobRenderer<CharacterEntity,CharacterModel>{
    private static final ResourceLocation ATLAS=ResourceLocation.fromNamespaceAndPath("coraline","characters/cast_atlas.png");
    public BobinskyVisitorRenderer(EntityRendererProvider.Context context){super(context,new CharacterModel("bobinsky"),.5F);addLayer(new Package(this,context));}
    @Override public ResourceLocation getTextureLocation(CharacterEntity actor){return ATLAS;}
    private static final class Package extends RenderLayer<CharacterEntity,CharacterModel>{
        private final ItemRenderer items;
        private final CharacterModel parcel=new CharacterModel("cheese_parcel");
        Package(BobinskyVisitorRenderer parent,EntityRendererProvider.Context context){super(parent);items=context.getItemRenderer();}
        @Override public void render(PoseStack pose,MultiBufferSource buffer,int light,CharacterEntity actor,float swing,float amount,float partial,float age,float yaw,float pitch){
            if(!(actor instanceof BobinskyVisitorEntity b))return;int phase=b.phase();
            boolean beet=phase==BobinskyVisit.F&&b.elapsed()<20;boolean cheese=phase==BobinskyVisit.B||phase==BobinskyVisit.C;
            if(!beet&&!cheese)return;pose.pushPose();
            if(cheese){if(phase==BobinskyVisit.B){getParentModel().transformBone("head",pose);pose.translate(0,-4.1/16,-6.4/16);}else{getParentModel().transformBone("right_forearm",pose);pose.translate(0,8.5/16,-.18);}pose.scale(1.35F,1.35F,1.35F);parcel.renderToBuffer(pose,buffer.getBuffer(RenderType.entityCutoutNoCull(ATLAS)),light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,1,1,1,1);}
            else{getParentModel().transformBone("right_forearm",pose);pose.translate(0,8.5/16,-.12);pose.mulPose(Axis.XP.rotationDegrees(180));pose.scale(.65F,.65F,.65F);items.renderStatic(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BEETROOT),net.minecraft.world.item.ItemDisplayContext.FIXED,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,pose,buffer,actor.level(),actor.getId());}pose.popPose();
        }
    }
}
