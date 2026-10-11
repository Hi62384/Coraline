package com.coraline.client.clothing;

import com.coraline.clothing.WhipAction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class WhipVisual {
    private record Lash(int id,Vec3 end,long tick){}
    private static final List<Lash> LASHES=new ArrayList<>();
    private static Object world;
    public static void receive(WhipAction.Lash lash){var mc=Minecraft.getInstance();if(world!=mc.level){world=mc.level;LASHES.clear();}if(mc.level!=null){if(LASHES.size()>=32)LASHES.remove(0);LASHES.add(new Lash(lash.player(),lash.end(),mc.level.getGameTime()));}}
    @SubscribeEvent public static void miss(PlayerInteractEvent.LeftClickEmpty e){if(WhipAction.isWhip(e.getEntity())&&!e.getEntity().getCooldowns().isOnCooldown(e.getItemStack().getItem()))WhipAction.miss();}
    @SubscribeEvent public static void draw(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;var mc=Minecraft.getInstance();if(mc.level==null||world!=mc.level){LASHES.clear();return;}
        double time=mc.level.getGameTime()+e.getPartialTick();LASHES.removeIf(l->time-l.tick>7||time<l.tick);if(LASHES.isEmpty())return;
        var pose=e.getPoseStack();pose.pushPose();var cam=e.getCamera().getPosition();pose.translate(-cam.x,-cam.y,-cam.z);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);RenderSystem.disableCull();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.depthMask(false);
        var b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);var matrix=pose.last().pose();
        for(var l:LASHES){var entity=mc.level.getEntity(l.id);if(entity==null)continue;
            float age=(float)((time-l.tick)/7);var forward=entity.getLookAngle();var side=forward.cross(new Vec3(0,1,0)).normalize();
            var start=entity.getEyePosition(e.getPartialTick()).add(side.scale(.3)).add(0,-.35,0);var delta=l.end.subtract(start);
            double extension=Math.sin(Math.PI*age);Vec3 previous=start;
            for(int i=1;i<=32;i++){double t=i/32.0;var next=start.add(delta.scale(t*(.18+.82*extension))).add(side.scale(Math.sin(t*Math.PI*2-age*5)*Math.sin(t*Math.PI)*(.65*(1-age)))).add(0,-Math.sin(t*Math.PI)*.25,0);
                var width=next.subtract(previous).cross(cam.subtract(previous)).normalize().scale(.013*(1-t*.55));
                for(var p:new Vec3[]{previous.add(width),previous.subtract(width),next.subtract(width),next.add(width)})b.vertex(matrix,(float)p.x,(float)p.y,(float)p.z).color(226,213,180,(int)(255*(1-age*.65))).endVertex();previous=next;
            }
        }
        BufferUploader.drawWithShader(b.end());RenderSystem.depthMask(true);RenderSystem.disableBlend();RenderSystem.enableCull();pose.popPose();
    }
}
