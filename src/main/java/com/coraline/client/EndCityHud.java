package com.coraline.client;

import com.coraline.feast.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Beam geometry and warning use the same timing and bounds as server damage. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class EndCityHud {
    private static ExpeditionNetwork.EndStatus data;private static int lease,ticks,received,hurtUntil;
    public static void receive(ExpeditionNetwork.EndStatus next){if(data!=null&&next.hearts()<data.hearts())hurtUntil=ticks+40;data=next;lease=next.phase()<3?40:0;received=ticks;}
    private static boolean active(){var mc=Minecraft.getInstance();return lease>0&&data!=null&&mc.player!=null&&ActivityWorld.isActivity(mc.player.level());}
    private static long elapsed(){return data.elapsed()+ticks-received;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase==TickEvent.Phase.END){ticks++;if(lease>0)lease--;if(Minecraft.getInstance().player==null)lease=0;}}
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e){
        var mc=Minecraft.getInstance();if(!active()||mc.options.hideGui)return;int x=mc.getWindow().getGuiScaledWidth()/2-91,y=mc.getWindow().getGuiScaledHeight()-64;
        for(int i=0;i<3;i++)e.getGuiGraphics().drawString(mc.font,"♥",x+i*12,y,i<data.hearts()?0xFFFF5555:0xFF555555,true);
        if(ticks<hurtUntil)e.getGuiGraphics().drawString(mc.font,"Recovering…",x+42,y,0xFFFFBBDD,true);
        if(data.phase()==1){boolean firing=EndCityChallenge.firing(elapsed(),data.room());String text=firing?"PURPLE BEAM — DANGER":"YELLOW LINE — WARNING";e.getGuiGraphics().drawCenteredString(mc.font,text,mc.getWindow().getGuiScaledWidth()/2,24,firing?0xFFFF66FF:0xFFFFFF55);}
    }
    @SubscribeEvent public static void beam(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS||!active()||data.phase()!=1)return;
        var mc=Minecraft.getInstance();long time=elapsed();boolean firing=EndCityChallenge.firing(time,data.room());
        var beam=EndCityChallenge.geometry(data.center(),time,data.room());var middle=beam.from().add(beam.to()).scale(.5);
        double length=beam.from().distanceTo(beam.to())*.5,angle=Math.atan2(beam.to().z-beam.from().z,beam.to().x-beam.from().x);
        var box=new AABB(-length,firing?0:.03,-beam.halfWidth(),length,firing?2:.06,beam.halfWidth());
        var pose=e.getPoseStack();pose.pushPose();var camera=e.getCamera().getPosition();pose.translate(middle.x-camera.x,middle.y-camera.y,middle.z-camera.z);pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float)-Math.toDegrees(angle)));
        var buffers=mc.renderBuffers().bufferSource();LevelRenderer.renderLineBox(pose,buffers.getBuffer(RenderType.lines()),box,1F,firing?.15F:1F,firing?1F:.1F,1F);
        buffers.endBatch(RenderType.lines());pose.popPose();
    }
}
