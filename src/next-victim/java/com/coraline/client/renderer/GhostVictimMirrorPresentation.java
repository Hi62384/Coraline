package com.coraline.client.renderer;

import com.coraline.client.GhostVictimClient;
import com.coraline.feast.CinematicPropEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;

/** Complete player-skinned apparition confined to the occupied mirror surface.
 * No world copy, distant actor or vanilla spectator head is used as a stand-in. */
public final class GhostVictimMirrorPresentation {
    private static PlayerModel<LivingEntity> wide,slim;
    public static boolean render(CinematicPropEntity mirror,PoseStack pose,MultiBufferSource buffers,int light,float partial){
        var mc=Minecraft.getInstance();if(mc.player==null||mirror.kind()!=CinematicPropEntity.STORY_MIRROR
            ||!mc.player.getUUID().equals(mirror.owner)||!GhostVictimClient.reflected(mirror.getUUID()))return false;
        FirstEscapePresentation.render(mirror,pose,buffers,light,partial);
        boolean narrow=mc.player.getModelName().equals("slim");PlayerModel<LivingEntity> model=narrow?slim:wide;
        if(model==null){model=new PlayerModel<>(mc.getEntityModels().bakeLayer(narrow?ModelLayers.PLAYER_SLIM:ModelLayers.PLAYER),narrow);if(narrow)slim=model;else wide=model;}
        model.setAllVisible(true);model.young=false;model.riding=false;model.crouching=false;model.attackTime=0;
        model.setupAnim(mc.player,0,0,mc.player.tickCount+partial,0,0);
        pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(-mirror.getYRot()));pose.translate(0,2.35,-.16);pose.scale(-.97F,-.97F,.10F);
        var vertices=buffers.getBuffer(RenderType.entityTranslucent(mc.player.getSkinTextureLocation()));
        model.renderToBuffer(pose,vertices,Math.max(light,0x00B000B0),OverlayTexture.NO_OVERLAY,.60F,.82F,1,.42F);pose.popPose();return true;
    }
    private GhostVictimMirrorPresentation(){}
}
