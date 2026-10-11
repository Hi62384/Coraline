package com.coraline.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.Map;

/** Unlit opaque cloth that actually occludes the stage, including later entity/particle passes. */
final class StageCurtainRenderType extends RenderType {
    private static final Map<ResourceLocation,RenderType> TYPES=new HashMap<>();
    private StageCurtainRenderType(){
        super("coraline_stage_curtain",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,256,false,false,()->{},()->{});
    }
    static RenderType cloth(ResourceLocation texture){
        return TYPES.computeIfAbsent(texture,key->create("coraline_stage_curtain",DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,256,false,false,CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                .setTextureState(new TextureStateShard(key,false,false))
                .setTransparencyState(NO_TRANSPARENCY).setCullState(NO_CULL)
                .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_DEPTH_WRITE)
                .setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(false)));
    }
}
