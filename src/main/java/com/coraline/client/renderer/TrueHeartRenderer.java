package com.coraline.client.renderer;

import com.coraline.postgame.TrueHeartEntity;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

/** Reuses the authored fractured body, not a renamed vanilla model. */
public final class TrueHeartRenderer extends MobRenderer<TrueHeartEntity,WhiteVoidAvatarModel<TrueHeartEntity>> {
    public TrueHeartRenderer(EntityRendererProvider.Context context){super(context,new WhiteVoidAvatarModel<>(context.bakeLayer(WhiteVoidAvatarModel.LAYER)),1.6F);}
    @Override protected void scale(TrueHeartEntity entity,com.mojang.blaze3d.vertex.PoseStack pose,float partial){pose.scale(1.2F,1.2F,1.2F);}
    @Override public ResourceLocation getTextureLocation(TrueHeartEntity entity){return ResourceLocation.fromNamespaceAndPath("coraline","textures/models/sculpted/fabric.png");}
}
