package com.coraline.client.renderer;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Desaturates the original fabric at draw time; the source artwork stays intact. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class DeadTunnelRenderType extends RenderType {
    private static ShaderInstance shader;
    private static final RenderType FABRIC=create("coraline_dead_tunnel",DefaultVertexFormat.NEW_ENTITY,
        VertexFormat.Mode.QUADS,256,false,false,CompositeState.builder()
            .setShaderState(new ShaderStateShard(()->shader))
            .setTextureState(new TextureStateShard(ResourceLocation.fromNamespaceAndPath("coraline","textures/block/tunnel_fabric.png"),false,false))
            .setTransparencyState(NO_TRANSPARENCY).setCullState(NO_CULL)
            .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_DEPTH_WRITE)
            .createCompositeState(false));
    private DeadTunnelRenderType(){super("coraline_dead_tunnel",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,256,false,false,()->{},()->{});}
    static RenderType fabric(){return FABRIC;}
    @SubscribeEvent public static void register(RegisterShadersEvent event)throws java.io.IOException{
        event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.fromNamespaceAndPath("coraline","dead_tunnel"),DefaultVertexFormat.NEW_ENTITY),loaded->shader=loaded);
    }
}
