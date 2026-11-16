package com.coraline.client.renderer;

import com.coraline.client.ShowActorPoseClient;
import com.coraline.entity.CharacterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Actual elderly cast rig folds and falls away during the server-timed zipper pose. */
public final class ShowDisguiseLayer extends RenderLayer<CharacterEntity,CharacterModel> {
    private final CharacterModel elderly;
    private static final ResourceLocation ATLAS=ResourceLocation.fromNamespaceAndPath("coraline","characters/cast_atlas.png");
    public ShowDisguiseLayer(RenderLayerParent<CharacterEntity,CharacterModel> parent,CharacterModel elderly){super(parent);this.elderly=elderly;}
    @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,CharacterEntity actor,float walk,float amount,float partial,float age,float yaw,float pitch){var frame=ShowActorPoseClient.frame(actor);if(frame==null||frame.pose()>=80||frame.reveal()<=0||frame.reveal()>=1)return;float q=frame.reveal();elderly.setupAnim(actor,walk,amount,age,yaw,pitch);pose.pushPose();pose.translate(0,q*q*.9,0);pose.scale(1+.12F*q,1-.8F*q,1+.10F*q);elderly.renderToBuffer(pose,buffers.getBuffer(RenderType.entityTranslucent(ATLAS)),light,OverlayTexture.NO_OVERLAY,1,1,1,1-q);pose.popPose();}
}
