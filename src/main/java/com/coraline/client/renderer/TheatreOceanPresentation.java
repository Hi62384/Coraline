package com.coraline.client.renderer;

import com.coraline.feast.CinematicPropEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** One Blender-exported set, bone-animated locally from the synchronized music cursor. */
public final class TheatreOceanPresentation {
    private static CharacterModel model;
    public static boolean render(CinematicPropEntity e,PoseStack pose,MultiBufferSource buffers,int light,float partial){
        if(e.kind()!=CinematicPropEntity.OCEAN_SET)return false;if(e.cue()<0)return true;
        if(NauticalStageRenderer.render(e,pose,buffers,light,partial))return true;
        if(model==null)model=new CharacterModel("theatre_ocean_set");
        float tick=e.time()+partial;model.theatreSet(tick,Math.max(1,e.value()));
        pose.pushPose();pose.scale(1,-1,1);
        model.renderSpeech(pose,buffers.getBuffer(StageCurtainRenderType.cloth(ResourceLocation.fromNamespaceAndPath("coraline","characters/cast_atlas.png"))),15728880,OverlayTexture.NO_OVERLAY,1,1,1,1,0);
        pose.popPose();return true;
    }
    private TheatreOceanPresentation(){}
}
