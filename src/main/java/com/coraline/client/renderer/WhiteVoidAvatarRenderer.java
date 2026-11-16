package com.coraline.client.renderer;

import com.coraline.postgame.WhiteVoidAvatarEntity;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

public final class WhiteVoidAvatarRenderer extends MobRenderer<WhiteVoidAvatarEntity,WhiteVoidAvatarModel<WhiteVoidAvatarEntity>> {
    public WhiteVoidAvatarRenderer(EntityRendererProvider.Context context){super(context,new WhiteVoidAvatarModel<>(context.bakeLayer(WhiteVoidAvatarModel.LAYER)),1.1F);}
    @Override public ResourceLocation getTextureLocation(WhiteVoidAvatarEntity entity){return ResourceLocation.fromNamespaceAndPath("coraline","textures/models/sculpted/fabric.png");}
}
