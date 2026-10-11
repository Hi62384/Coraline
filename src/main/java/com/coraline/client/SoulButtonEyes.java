package com.coraline.client;

import com.coraline.clothing.OtherMaterials;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** A pair of actual button sprites is placed over the face of captured mobs. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class SoulButtonEyes {
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        var camera=event.getCamera();var cam=camera.getPosition();var buffer=mc.renderBuffers().bufferSource();
        ItemStack button=new ItemStack(OtherMaterials.BUTTON.get());
        for(var entity:mc.level.entitiesForRendering()){
            if(!(entity instanceof Mob mob)||!mob.isAlive()||mob.getCustomName()==null||!mob.getCustomName().getString().startsWith("◉ ◉ ")||mob.distanceToSqr(cam)>32*32)continue;
            var look=mob.getViewVector(event.getPartialTick()).normalize();
            var toward=cam.subtract(mob.position()).normalize();if(look.dot(toward)<.15)continue;
            var right=look.cross(new net.minecraft.world.phys.Vec3(0,1,0)).normalize();
            double x=net.minecraft.util.Mth.lerp(event.getPartialTick(),mob.xOld,mob.getX())-cam.x;
            double y=net.minecraft.util.Mth.lerp(event.getPartialTick(),mob.yOld,mob.getY())-cam.y+mob.getEyeHeight()-.1;
            double z=net.minecraft.util.Mth.lerp(event.getPartialTick(),mob.zOld,mob.getZ())-cam.z;
            double faceDistance=Math.max(.12,mob.getBbWidth()*.43);
            PoseStack pose=event.getPoseStack();pose.pushPose();
            pose.translate(x+look.x*faceDistance,y+look.y*faceDistance,z+look.z*faceDistance);
            float separation=Math.max(.1F,Math.min(.28F,mob.getBbWidth()*.25F));
            for(int side:new int[]{-1,1}){
                pose.pushPose();pose.translate(side*right.x*separation,side*right.y*separation,side*right.z*separation);
                pose.mulPose(camera.rotation());pose.scale(.2F,.2F,.2F);
                mc.getItemRenderer().renderStatic(button,ItemDisplayContext.FIXED,LightTexture.FULL_BRIGHT,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,pose,buffer,mc.level,mob.getId()+side);
                pose.popPose();
            }
            pose.popPose();
        }
        buffer.endBatch();
    }
}
