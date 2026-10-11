package com.coraline.client;

import com.coraline.clothing.ClothingEffects;
import com.coraline.clothing.OtherMaterials;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** A bounded, client-only ore overlay; never loads chunks or reveals ores outside a short radius. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class MoonstoneOreSight {
    private static final List<BlockPos> ORES=new ArrayList<>();
    private static int ticks;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null||!ClothingEffects.night(mc.level)||!mc.player.getMainHandItem().is(OtherMaterials.TOOLS.get("moonstone_pickaxe").get())){ORES.clear();return;}
        if(mc.isPaused()||++ticks%20!=0)return;
        var center=mc.player.blockPosition();var found=new ArrayList<BlockPos>();
        for(int dx=-10;dx<=10;dx++)for(int dy=-10;dy<=10;dy++)for(int dz=-10;dz<=10;dz++){
            if(dx*dx+dy*dy+dz*dz>100)continue;
            var pos=center.offset(dx,dy,dz);
            if(!mc.level.hasChunkAt(pos))continue;
            var state=mc.level.getBlockState(pos);
            if(state.is(BlockTags.COAL_ORES)||state.is(BlockTags.IRON_ORES)||state.is(BlockTags.COPPER_ORES)||state.is(BlockTags.GOLD_ORES)||state.is(BlockTags.REDSTONE_ORES)||state.is(BlockTags.EMERALD_ORES)||state.is(BlockTags.LAPIS_ORES)||state.is(BlockTags.DIAMOND_ORES))found.add(pos.immutable());
        }
        ORES.clear();ORES.addAll(found);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||ORES.isEmpty())return;
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null||!ClothingEffects.night(mc.level)||!mc.player.getMainHandItem().is(OtherMaterials.TOOLS.get("moonstone_pickaxe").get()))return;
        Vec3 camera=event.getCamera().getPosition();
        Vector3f right=new Vector3f(1,0,0).rotate(event.getCamera().rotation());
        Vector3f up=new Vector3f(0,1,0).rotate(event.getCamera().rotation());
        var pose=event.getPoseStack().last().pose();
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableDepthTest();RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        try{
            var buffer=Tesselator.getInstance().getBuilder();buffer.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
            for(var pos:ORES){
                var p=Vec3.atCenterOf(pos).subtract(camera);if(p.lengthSqr()>105)continue;
                float s=.17F;
                vertex(buffer,pose,p,right,up,0,s);vertex(buffer,pose,p,right,up,s,0);
                vertex(buffer,pose,p,right,up,0,-s);vertex(buffer,pose,p,right,up,-s,0);
            }
            BufferUploader.drawWithShader(buffer.end());
        }finally{RenderSystem.depthMask(true);RenderSystem.enableDepthTest();RenderSystem.disableBlend();}
    }
    private static void vertex(com.mojang.blaze3d.vertex.BufferBuilder buffer,org.joml.Matrix4f pose,Vec3 pos,Vector3f right,Vector3f up,float x,float y){
        buffer.vertex(pose,(float)pos.x+right.x*x+up.x*y,(float)pos.y+right.y*x+up.y*y,(float)pos.z+right.z*x+up.z*y).color(.43F,.95F,1F,.72F).endVertex();
    }
}
