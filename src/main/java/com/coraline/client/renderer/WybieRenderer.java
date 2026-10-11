package com.coraline.client.renderer;
import com.coraline.entity.WybieEntity;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
public final class WybieRenderer extends MobRenderer<WybieEntity, WybieModel> {
    public WybieRenderer(EntityRendererProvider.Context context) {
        super(context, new WybieModel(context.bakeLayer(WybieModel.LAYER)), 0.65F);
    }
    @Override public ResourceLocation getTextureLocation(WybieEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("coraline", "textures/entity/wybie_skin.png");
    }
    @Override protected void scale(WybieEntity entity, com.mojang.blaze3d.vertex.PoseStack pose, float partialTick) {
        float scale=com.coraline.config.CoralineConfig.WYBIE_SCALE.get().floatValue();
        pose.scale(scale,scale,scale);
    }
}
