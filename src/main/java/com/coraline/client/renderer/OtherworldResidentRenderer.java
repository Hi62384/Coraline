package com.coraline.client.renderer;

import com.coraline.postgame.OtherworldResident;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

public final class OtherworldResidentRenderer extends MobRenderer<OtherworldResident,OtherworldResidentModel> {
    public OtherworldResidentRenderer(EntityRendererProvider.Context context){super(context,new OtherworldResidentModel(context.bakeLayer(OtherworldResidentModel.LAYER)),.35F);}
    @Override public ResourceLocation getTextureLocation(OtherworldResident entity){return ResourceLocation.fromNamespaceAndPath("coraline","textures/models/sculpted/fabric.png");}
}
