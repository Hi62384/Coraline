package com.coraline.client;

import com.coraline.entity.CharacterEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Owner-only presentation. The cat is drawn after the white post pass, not bleached away. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class CatEncounterClient {
    private static net.minecraft.client.multiplayer.ClientLevel world;
    private static int musicAge=121;private static int actor;private static String phase="";private static long received,fadeAge=-1;private static boolean active;
    public static void receive(CompoundTag n){var mc=Minecraft.getInstance();world=mc.level;musicAge=0;active=n.getBoolean("Active");actor=n.getInt("Actor");phase=n.getString("Phase");received=world==null?0:world.getGameTime();fadeAge=n.getLong("FadeAge");}
    @SubscribeEvent public static void music(net.minecraftforge.event.TickEvent.ClientTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();if(!mc.isPaused())musicAge++;
        // Refresh one continuous loop for this encounter, including its walk
        // into the void and return. Completion/cancellation fades over 40 ticks.
        ScenePreludeMusic.set("the_cat_music",valid()&&musicAge<120&&com.coraline.otherworld.OtherWorld.isOther(mc.level),null);
    }
    private static boolean valid(){var mc=Minecraft.getInstance();return active&&mc.level!=null&&mc.level==world&&mc.player!=null;}
    public static float fade(float partial){if(!valid()||fadeAge<0)return -1;return Math.max(0,1-(fadeAge+world.getGameTime()-received+partial)/60F);}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL||!valid()||!phase.equals("E")||OtherWorldWhiteout.amount(e.getPartialTick())<.05F)return;
        var mc=Minecraft.getInstance();if(!(world.getEntity(actor) instanceof CharacterEntity cat)||cat.isInvisible())return;
        var camera=e.getCamera().getPosition();double partial=e.getPartialTick();double x=net.minecraft.util.Mth.lerp(partial,cat.xOld,cat.getX())-camera.x,y=net.minecraft.util.Mth.lerp(partial,cat.yOld,cat.getY())-camera.y,z=net.minecraft.util.Mth.lerp(partial,cat.zOld,cat.getZ())-camera.z;
        float start=RenderSystem.getShaderFogStart(),end=RenderSystem.getShaderFogEnd();
        RenderSystem.backupProjectionMatrix();
        try{RenderSystem.setProjectionMatrix(e.getProjectionMatrix(),com.mojang.blaze3d.vertex.VertexSorting.DISTANCE_TO_ORIGIN);RenderSystem.setShaderFogStart(10000);RenderSystem.setShaderFogEnd(10001);var buffers=mc.renderBuffers().bufferSource();mc.getEntityRenderDispatcher().render(cat,x,y,z,cat.getYRot(),e.getPartialTick(),e.getPoseStack(),buffers,15728880);buffers.endBatch();}
        finally{RenderSystem.restoreProjectionMatrix();RenderSystem.setShaderFogStart(start);RenderSystem.setShaderFogEnd(end);}
    }
    private CatEncounterClient(){}
}
