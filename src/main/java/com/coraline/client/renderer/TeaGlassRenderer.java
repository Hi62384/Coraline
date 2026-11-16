package com.coraline.client.renderer;
import com.coraline.tea.TeaGlassEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
public final class TeaGlassRenderer extends EntityRenderer<TeaGlassEntity> {
    private static final ResourceLocation ATLAS=ResourceLocation.fromNamespaceAndPath("coraline","characters/cast_atlas.png");
    private final CharacterModel glass=new CharacterModel("tea_glass"),shards=new CharacterModel("tea_glass_shards");
    public TeaGlassRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=0;}
    @Override public ResourceLocation getTextureLocation(TeaGlassEntity e){return ATLAS;}
    @Override public void render(TeaGlassEntity e,float yaw,float partial,PoseStack pose,MultiBufferSource buffer,int light){pose.pushPose();pose.scale(1,-1,-1);(e.broken()?shards:glass).renderToBuffer(pose,buffer.getBuffer(RenderType.entityTranslucent(ATLAS)),light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,1,1,1,.65F);pose.popPose();super.render(e,yaw,partial,pose,buffer,light);}
}
