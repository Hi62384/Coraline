package com.coraline.client.renderer;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Only the three eye meshes, depth-tested against the completed world. */
public final class SpectralEyeRenderType extends RenderType {
    private SpectralEyeRenderType(){super("coraline_spectral_eyes",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,4096,false,true,()->{},()->{});}
    public static final RenderType EYES=create("coraline_spectral_eyes",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,4096,false,true,
        CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
            .setTextureState(new TextureStateShard(ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/white_concrete.png"),false,false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
            // The same animated mesh is redrawn against its already-written
            // depth. A tiny view offset prevents equality-rounding rejection;
            // LEQUAL still prevents seeing mobs through walls.
            .setLayeringState(VIEW_OFFSET_Z_LAYERING).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY)
            .setOutputState(new OutputStateShard("coraline_spectral_target",()->com.coraline.client.SpectralEyes.bind(),()->Minecraft.getInstance().getMainRenderTarget().bindWrite(false)))
            .createCompositeState(false));
}
