package com.coraline.client.renderer;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Shared live skin rendering for the inventory item and placed doll. */
public final class DollVisual {
    private final PlayerModel<LivingEntity> regular;
    private final PlayerModel<LivingEntity> slim;
    public DollVisual() {
        var models=Minecraft.getInstance().getEntityModels();
        regular=new PlayerModel<>(models.bakeLayer(ModelLayers.PLAYER),false);
        slim=new PlayerModel<>(models.bakeLayer(ModelLayers.PLAYER_SLIM),true);
    }
    public void render(GameProfile profile,PoseStack pose,MultiBufferSource buffer,int light) {
        render(profile,pose,buffer,light,false);
    }
    public void render(GameProfile profile,PoseStack pose,MultiBufferSource buffer,int light,boolean sitting) {
        Minecraft mc=Minecraft.getInstance();
        if (profile==null || profile.getId()==null) {
            if(mc.player==null) return;
            profile=mc.player.getGameProfile();
        }
        ResourceLocation skin=mc.getSkinManager().getInsecureSkinLocation(profile);
        boolean narrow=false;
        if(mc.level!=null && mc.level.getPlayerByUUID(profile.getId()) instanceof AbstractClientPlayer player) {
            skin=player.getSkinTextureLocation(); narrow=player.getModelName().equals("slim");
        } else {
            var info=mc.getSkinManager().getInsecureSkinInformation(profile);
            var texture=info.get(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN);
            narrow=texture!=null && "slim".equals(texture.getMetadata("model"));
        }
        renderSkin(skin,narrow,pose,buffer,light,sitting);
    }
    public void renderSkin(ResourceLocation skin,boolean narrow,PoseStack pose,MultiBufferSource buffer,int light) {
        renderSkin(skin,narrow,pose,buffer,light,false);
    }
    public void renderSkin(ResourceLocation skin,boolean narrow,PoseStack pose,MultiBufferSource buffer,int light,boolean sitting) {
        PlayerModel<LivingEntity> model=narrow?slim:regular;
        model.young=false;
        model.riding=false;
        model.crouching=false;
        model.head.resetPose(); model.hat.resetPose();
        model.body.resetPose(); model.leftArm.resetPose(); model.rightArm.resetPose();
        model.leftLeg.resetPose(); model.rightLeg.resetPose();
        model.setAllVisible(true);
        model.head.xRot=model.head.yRot=model.head.zRot=0;
        model.hat.copyFrom(model.head);
        model.leftArm.zRot=0.08F; model.rightArm.zRot=-0.08F;
        model.leftSleeve.copyFrom(model.leftArm); model.rightSleeve.copyFrom(model.rightArm);
        if(sitting) {
            // Straight legs project forward from the hips, resting on the floor.
            model.leftLeg.xRot=model.rightLeg.xRot=-(float)Math.PI/2;
            // Forward points toward -Z in model space: these signs splay outward.
            // Separate the hips too, leaving clearance for the outer pants layers.
            model.leftLeg.x=2.6F; model.rightLeg.x=-2.6F;
            model.leftLeg.yRot=-0.20F; model.rightLeg.yRot=0.20F;
        }
        model.leftPants.copyFrom(model.leftLeg); model.rightPants.copyFrom(model.rightLeg);
        model.jacket.copyFrom(model.body);
        model.renderToBuffer(pose,buffer.getBuffer(RenderType.entityCutoutNoCull(skin)),light,OverlayTexture.NO_OVERLAY,1,1,1,1);
        buttons(model.head,pose,buffer,light);
    }
    public static void buttons(net.minecraft.client.model.geom.ModelPart head,PoseStack pose,MultiBufferSource buffer,int light){
        // Render just ahead of the outer hat layer so glasses/skin overlays cannot cover the buttons.
        pose.pushPose(); head.translateAndRotate(pose);
        VertexConsumer v=buffer.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath("coraline","textures/entity/doll_button_eyes.png")));
        // Face runs from -0.5 to 0: center the oversized buttons 60% up it.
        vertex(v,pose,-0.28F,-0.53F,0,0,light); vertex(v,pose,-0.28F,-0.07F,0,1,light);
        vertex(v,pose,0.28F,-0.07F,1,1,light); vertex(v,pose,0.28F,-0.53F,1,0,light);
        pose.popPose();
    }
    private static void vertex(VertexConsumer v,PoseStack p,float x,float y,float u,float t,int light) {
        v.vertex(p.last().pose(),x,y,-0.29F).color(255,255,255,255).uv(u,t).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(p.last().normal(),0,0,-1).endVertex();
    }
}
