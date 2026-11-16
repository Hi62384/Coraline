package com.coraline.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Mark the reported location, never attach a permanent outline to the child. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class TycoonSpyMarkers {
    private static ListTag markers=new ListTag();private static int age;
    public static void receive(ListTag values){markers=values.copy();age=0;}
    public static void clear(){markers=new ListTag();age=0;}
    private static boolean active(){var mc=Minecraft.getInstance();return mc.player!=null&&mc.level!=null&&mc.level.dimension()==com.coraline.tycoon.TycoonWorld.KEY;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;age++;if(!active())clear();}
    @SubscribeEvent public static void draw(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS||!active())return;var mc=Minecraft.getInstance();var pose=e.getPoseStack();var camera=e.getCamera().getPosition();var buffers=mc.renderBuffers().bufferSource();
        for(var value:markers){var n=(CompoundTag)value;if(age>=n.getLong("Remaining"))continue;var at=BlockPos.of(n.getLong("At"));pose.pushPose();pose.translate(at.getX()+.5-camera.x,at.getY()+2.5-camera.y,at.getZ()+.5-camera.z);pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees((age+e.getPartialTick())*2));LevelRenderer.renderLineBox(pose,buffers.getBuffer(RenderType.lines()),new AABB(-.25,-.25,-.25,.25,.25,.25),.35F,.85F,1F,1F);pose.popPose();}
        buffers.endBatch(RenderType.lines());
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e){if(!active())return;var mc=Minecraft.getInstance();if(mc.options.hideGui)return;int y=32;for(var value:markers){var n=(CompoundTag)value;if(age>=n.getLong("Remaining"))continue;var at=BlockPos.of(n.getLong("At"));String line=n.getString("Name")+" — last seen "+(int)Math.sqrt(mc.player.distanceToSqr(at.getCenter()))+"m — "+Math.max(0,(n.getLong("Remaining")-age)/20)+"s";e.getGuiGraphics().drawString(mc.font,line,8,y,0x7CDBFF,true);y+=12;if(y>92)break;}}
    private TycoonSpyMarkers(){}
}
