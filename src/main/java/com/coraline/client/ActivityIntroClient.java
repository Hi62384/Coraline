package com.coraline.client;

import com.coraline.feast.VisitAudio;
import com.coraline.feast.ActivityWorld;
import com.coraline.parents.ParentEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Listening dismisses the menu, not the recording or its host-facing camera. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class ActivityIntroClient {
    private static VisitAudio.IntroOffer active;
    private static int remaining,host=-1;
    private static boolean pending,entered;
    private ActivityIntroClient(){}
    public static void offer(VisitAudio.IntroOffer offer){
        if(offer.ticks()<=0){
            if(active!=null&&active.generation()==offer.generation()&&active.sound().equals(offer.sound()))clear();
            return;
        }
        active=offer;remaining=offer.ticks();host=-1;pending=true;entered=false;
    }
    private static void clear(){
        active=null;remaining=0;host=-1;pending=false;
        var mc=Minecraft.getInstance();if(mc.screen instanceof IntroScreen)mc.setScreen(null);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||active==null)return;
        var mc=Minecraft.getInstance();
        if(--remaining<=0){clear();return;}
        // Offers can arrive while the activity dimension is still loading.
        if(mc.player==null||mc.level==null)return;
        if(!mc.player.isAlive()){clear();return;}
        if(!ActivityWorld.isActivity(mc.level)){if(entered)clear();return;}
        entered=true;
        if(pending&&(mc.screen==null||mc.screen instanceof IntroScreen)){pending=false;mc.setScreen(new IntroScreen(active));}
        if(!(mc.level.getEntity(host) instanceof ParentEntity dad)||!dad.isAlive()){
            host=mc.level.getEntitiesOfClass(ParentEntity.class,mc.player.getBoundingBox().inflate(96),p->p.isAlive()&&!p.isInvisible()&&p.isOtherParent()&&!p.isMom())
                .stream().min(java.util.Comparator.comparingDouble(p->p.distanceToSqr(mc.player))).map(ParentEntity::getId).orElse(-1);
        }
    }
    @SubscribeEvent public static void camera(ViewportEvent.ComputeCameraAngles event){
        var mc=Minecraft.getInstance();
        if(active==null||remaining<=0||mc.player==null||mc.level==null||!(mc.level.getEntity(host) instanceof ParentEntity dad))return;
        var delta=dad.getEyePosition((float)event.getPartialTick()).subtract(mc.player.getEyePosition((float)event.getPartialTick()));
        float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));
        float pitch=(float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance()));
        mc.player.setYRot(yaw);mc.player.setXRot(pitch);mc.player.yRotO=yaw;mc.player.xRotO=pitch;
        event.setYaw(yaw);event.setPitch(pitch);
    }
    private static final class IntroScreen extends Screen {
        private final VisitAudio.IntroOffer offer;private boolean sent;
        IntroScreen(VisitAudio.IntroOffer offer){super(Component.literal("Other Father's introduction"));this.offer=offer;}
        @Override public boolean isPauseScreen(){return false;}
        @Override protected void init(){
            addRenderableWidget(Button.builder(Component.literal("Listen to dialogue"),b->onClose())
                .bounds(Math.max(4,width-154),height-56,146,20).build());
            addRenderableWidget(Button.builder(Component.literal("Skip dialogue"),b->{
                if(!sent){sent=true;b.active=false;VisitAudio.skipIntro(offer.sound(),offer.generation());}
            }).bounds(Math.max(4,width-154),height-32,146,20).build());
        }
        @Override public void render(GuiGraphics g,int x,int y,float partial){super.render(g,x,y,partial);}
    }
}
