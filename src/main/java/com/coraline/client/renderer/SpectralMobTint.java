package com.coraline.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;

/** Loaded, normally rendered mobs only; no arbitrary range/count cutoff and no wall sight. */
public final class SpectralMobTint implements VertexConsumer {
    private final VertexConsumer out;
    private final int red,green,blue;
    private SpectralMobTint(VertexConsumer out,boolean hostile){this.out=out;red=hostile?255:65;green=hostile?55:245;blue=hostile?55:115;}
    public static void render(Minecraft mc,PoseStack pose,MultiBufferSource source,Vec3 origin,float partial){
        if(mc.level==null||mc.player==null)return;
        for(var entity:mc.level.entitiesForRendering()){
            if(!(entity instanceof Mob mob)||!mob.isAlive()||mob.isInvisible()||!mob.shouldRender(origin.x,origin.y,origin.z))continue;
            boolean hostile=mob instanceof Enemy||mob.getTarget()==mc.player;
            var tint=new SpectralMobTint(source.getBuffer(SpectralEyeRenderType.EYES),hostile);
            MultiBufferSource colored=type->tint;
            pose.pushPose();
            pose.translate(net.minecraft.util.Mth.lerp(partial,mob.xOld,mob.getX())-origin.x,net.minecraft.util.Mth.lerp(partial,mob.yOld,mob.getY())-origin.y,net.minecraft.util.Mth.lerp(partial,mob.zOld,mob.getZ())-origin.z);
            try{mc.getEntityRenderDispatcher().getRenderer(mob).render(mob,mob.getYRot(),partial,pose,colored,15728880);}
            finally{pose.popPose();}
        }
    }
    @Override public VertexConsumer vertex(double x,double y,double z){out.vertex(x,y,z);return this;}
    @Override public VertexConsumer color(int r,int g,int b,int a){out.color(red,green,blue,180);return this;}
    @Override public VertexConsumer uv(float u,float v){out.uv(.5F,.5F);return this;}
    @Override public VertexConsumer overlayCoords(int u,int v){out.overlayCoords(u,v);return this;}
    @Override public VertexConsumer uv2(int u,int v){out.uv2(240,240);return this;}
    @Override public VertexConsumer normal(float x,float y,float z){out.normal(x,y,z);return this;}
    @Override public void endVertex(){out.endVertex();}
    @Override public void defaultColor(int r,int g,int b,int a){out.defaultColor(red,green,blue,180);}
    @Override public void unsetDefaultColor(){out.unsetDefaultColor();}
}
