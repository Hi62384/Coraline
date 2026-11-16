package com.coraline.mixin;
import com.coraline.client.renderer.AcceptedPlayerVisual;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(PlayerRenderer.class)
public abstract class AcceptedPlayerHandMixin {
    @Redirect(method="renderHand",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private VertexConsumer coraline$hand(MultiBufferSource source,RenderType type,PoseStack pose,MultiBufferSource original,int light,AbstractClientPlayer player,ModelPart arm,ModelPart sleeve){
        float alpha=AcceptedPlayerVisual.opacity(player);return alpha>=1?source.getBuffer(type):AcceptedPlayerVisual.alpha(source.getBuffer(RenderType.entityTranslucent(player.getSkinTextureLocation())),alpha);
    }
}
