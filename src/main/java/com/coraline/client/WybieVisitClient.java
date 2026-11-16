package com.coraline.client;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class WybieVisitClient {
    private static int flash;
    private static int lookActor=-1,lookTicks;
    public static void look(int actor,int ticks){lookActor=actor;lookTicks=ticks;}
    @SubscribeEvent public static void camera(net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles e){var mc=Minecraft.getInstance();if(lookTicks<=0||mc.player==null||mc.level==null)return;var actor=mc.level.getEntity(lookActor);if(actor==null)return;var delta=actor.getEyePosition().subtract(mc.player.getEyePosition());float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z)),pitch=(float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance()));yaw=net.minecraft.util.Mth.rotLerp(.22F,mc.player.getYRot(),yaw);pitch=net.minecraft.util.Mth.lerp(.22F,mc.player.getXRot(),pitch);mc.player.setYRot(yaw);mc.player.setXRot(pitch);e.setYaw(yaw);e.setPitch(pitch);}
    public static void effect(boolean camera){var mc=Minecraft.getInstance();if(mc.player==null)return;if(camera){mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("Wybie gave you a camera."),true);return;}flash=7;mc.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),1F,1.7F);mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase==TickEvent.Phase.END){if(flash>0)flash--;if(lookTicks>0)lookTicks--;}if(Minecraft.getInstance().level==null){flash=0;lookTicks=0;}}
    @SubscribeEvent public static void render(RenderGuiEvent.Post e){if(flash<=0)return;var g=e.getGuiGraphics();int alpha=Math.min(220,flash*36);g.fill(0,0,g.guiWidth(),g.guiHeight(),alpha<<24|0xFFFFFF);}
}
