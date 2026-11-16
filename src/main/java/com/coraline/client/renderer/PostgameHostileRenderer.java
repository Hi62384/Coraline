package com.coraline.client.renderer;

import com.coraline.postgame.OtherWorldHostile;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

public final class PostgameHostileRenderer extends MobRenderer<OtherWorldHostile,PostgameHostileModel> {
    private static final ResourceLocation FABRIC=ResourceLocation.fromNamespaceAndPath("coraline","textures/models/sculpted/fabric.png");
    public PostgameHostileRenderer(EntityRendererProvider.Context context){super(context,new PostgameHostileModel(context.bakeLayer(PostgameHostileModel.LAYER)),.4F);}
    @Override public ResourceLocation getTextureLocation(OtherWorldHostile entity){return FABRIC;}
}
