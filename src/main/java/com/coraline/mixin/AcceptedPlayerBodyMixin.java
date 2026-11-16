package com.coraline.mixin;
import com.coraline.client.renderer.AcceptedPlayerVisual;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntityRenderer.class)
public abstract class AcceptedPlayerBodyMixin {
    @Inject(method="getRenderType",at=@At("HEAD"),cancellable=true)
    private void coraline$renderType(LivingEntity entity,boolean body,boolean translucent,boolean glowing,CallbackInfoReturnable<RenderType> ci){
        if(body&&entity instanceof AbstractClientPlayer player&&AcceptedPlayerVisual.opacity(player)<1)ci.setReturnValue(RenderType.entityTranslucent(player.getSkinTextureLocation()));
    }
    @Redirect(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
    private void coraline$body(EntityModel<?> model,PoseStack pose,VertexConsumer buffer,int light,int overlay,float r,float g,float b,float a,LivingEntity entity,float yaw,float partial,PoseStack original,MultiBufferSource buffers,int packed){
        float opacity=entity instanceof AbstractClientPlayer player?AcceptedPlayerVisual.opacity(player):1;model.renderToBuffer(pose,buffer,light,overlay,r,g,b,a*opacity);
    }
}
