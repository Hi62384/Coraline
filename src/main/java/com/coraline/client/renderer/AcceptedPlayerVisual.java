package com.coraline.client.renderer;

import com.coraline.client.AcceptedButtonEyesClient;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class AcceptedPlayerVisual extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
    public AcceptedPlayerVisual(RenderLayerParent<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> parent){super(parent);}
    public static float opacity(AbstractClientPlayer player){return own(player)?AcceptedButtonEyesClient.opacity():1;}
    private static boolean own(AbstractClientPlayer player){var local=Minecraft.getInstance().player;return local!=null&&local.getUUID().equals(player.getUUID());}
    @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers e){for(String skin:e.getSkins()){var renderer=e.getSkin(skin);if(renderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer player)player.addLayer(new AcceptedPlayerVisual(player));}}
    @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,AbstractClientPlayer player,float limb,float amount,float partial,float age,float yaw,float pitch){
        if(!own(player)||!AcceptedButtonEyesClient.buttonEyes()||player.isInvisible())return;
        MultiBufferSource translucent=type->alpha(buffers.getBuffer(RenderType.entityTranslucent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("coraline","textures/entity/doll_button_eyes.png"))),opacity(player));
        DollVisual.buttons(getParentModel().head,pose,translucent,light);
    }
    public static VertexConsumer alpha(VertexConsumer out,float opacity){return new VertexConsumer(){
        public VertexConsumer vertex(double x,double y,double z){out.vertex(x,y,z);return this;}
        public VertexConsumer color(int r,int g,int b,int a){out.color(r,g,b,Math.round(a*opacity));return this;}
        public VertexConsumer uv(float u,float v){out.uv(u,v);return this;}
        public VertexConsumer overlayCoords(int u,int v){out.overlayCoords(u,v);return this;}
        public VertexConsumer uv2(int u,int v){out.uv2(u,v);return this;}
        public VertexConsumer normal(float x,float y,float z){out.normal(x,y,z);return this;}
        public void endVertex(){out.endVertex();}
        public void defaultColor(int r,int g,int b,int a){out.defaultColor(r,g,b,Math.round(a*opacity));}
        public void unsetDefaultColor(){out.unsetDefaultColor();}
    };}
}
