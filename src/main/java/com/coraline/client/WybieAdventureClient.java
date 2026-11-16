package com.coraline.client;

import com.coraline.feast.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Owner-only timer and brief, occluded target particles; never sets global entity glow. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class WybieAdventureClient {
    private static int age=100,remaining=-1,current,best;private static String mode="";private static net.minecraft.client.multiplayer.ClientLevel world;
    private static Vec3 hint;private static int hintTicks,entityId=-1;private static UUID entityUuid;
    public static void receive(CompoundTag n){
        if(n==null)return;if(n.getBoolean("WybieDebug")){WybieAdventureDebugClient.receive(n);return;}var mc=Minecraft.getInstance();world=mc.level;
        if(n.getBoolean("WybieStatus")){if(n.getBoolean("Close")){clear();return;}age=0;remaining=n.getInt("Remaining");current=n.getInt("CurrentScore");best=n.getInt("BestScore");mode=n.getString("Mode");}
        if(n.getBoolean("WybieHint")){hint=new Vec3(n.getDouble("X"),n.getDouble("Y"),n.getDouble("Z"));hintTicks=Math.min(100,n.getInt("Ticks"));entityId=n.contains("Entity")?n.getInt("Entity"):-1;entityUuid=n.hasUUID("EntityUuid")?n.getUUID("EntityUuid"):null;}
        if(n.getBoolean("WybieReturnChoice")&&n.hasUUID("Nonce"))mc.setScreen(new ReturnScreen(n.getUUID("Nonce")));
    }
    private static void clear(){age=100;mode="";hint=null;hintTicks=0;entityId=-1;entityUuid=null;var mc=Minecraft.getInstance();if(mc.screen instanceof ReturnScreen)mc.setScreen(null);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.isPaused())return;
        if(mc.player==null||mc.level==null||mc.level!=world){clear();world=mc.level;return;}age++;
        if(mc.player.getVehicle() instanceof WybieBicycleEntity bike&&bike.session()!=null&&mc.player.tickCount%2==0){float forward=mc.screen==null&&mc.options.keyUp.isDown()?1:0;float turn=mc.screen==null?((mc.options.keyLeft.isDown()?-1:0)+(mc.options.keyRight.isDown()?1:0)):0;boolean brake=mc.screen!=null||mc.options.keyDown.isDown()||mc.options.keyJump.isDown();WybieAdventureNetwork.input(new WybieAdventureNetwork.Input(bike.getId(),bike.session(),forward,turn,brake));}
        if(hintTicks>0&&hint!=null){
            hintTicks--;Vec3 at=hint;if(entityId>=0){var target=mc.level.getEntity(entityId);if(target==null||!target.isAlive()||entityUuid==null||!entityUuid.equals(target.getUUID())){hintTicks=0;return;}at=target.getBoundingBox().getCenter();}
            if(mc.player.tickCount%8!=0||mc.player.distanceToSqr(at)>36*36)return;
            var hit=mc.level.clip(new ClipContext(mc.player.getEyePosition(),at,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player));if(hit.getType()!=HitResult.Type.MISS&&(entityId>=0||!hit.getBlockPos().equals(net.minecraft.core.BlockPos.containing(at))))return;
            double angle=mc.player.tickCount*.2;mc.level.addParticle(ParticleTypes.END_ROD,at.x+Math.cos(angle)*.28,at.y+.25,at.z+Math.sin(angle)*.28,0,.005,0);
        }
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e){var mc=Minecraft.getInstance();if(age>60||mode.isEmpty()||remaining<0||mc.options.hideGui||mc.player==null)return;int estimate=Math.max(0,remaining-age);String text=estimate==0?"Wybie Adventure · Return home":"Wybie Adventure · "+String.format(Locale.ROOT,"%d:%02d",(estimate+19)/1200,((estimate+19)/20)%60);int width=Math.max(mc.font.width(text),mc.font.width("Run "+current+" · Best "+best));var g=e.getGuiGraphics();g.fill(6,26,width+15,57,0x90302038);g.drawString(mc.font,text,10,30,estimate<=1200?0xFFFFC28C:0xFFE9D9BD);g.drawString(mc.font,"Run "+current+" · Best "+best,10,44,0xFFBFE6D0);}
    private static final class ReturnScreen extends Screen {
        private final UUID nonce;
        ReturnScreen(UUID nonce){super(Component.literal("Had enough adventuring for today, sweetheart?"));this.nonce=nonce;}
        @Override public boolean isPauseScreen(){return false;}
        @Override protected void init(){int x=width/2-130,y=height/2;addRenderableWidget(Button.builder(Component.literal("Yes, we're finished."),b->{WybieAdventureNetwork.choice(nonce,true);Minecraft.getInstance().setScreen(null);}).bounds(x,y+8,260,20).build());addRenderableWidget(Button.builder(Component.literal("Not yet."),b->onClose()).bounds(x,y+34,260,20).build());}
        @Override public void onClose(){WybieAdventureNetwork.choice(nonce,false);super.onClose();}
        @Override public void render(GuiGraphics g,int mx,int my,float partial){int x=width/2,y=height/2;g.fill(x-146,y-45,x+146,y+66,0xEE2B2535);g.drawCenteredString(font,"Other Mother",x,y-32,0xFFF4CE94);g.drawCenteredString(font,title,x,y-15,0xFFEDE1D0);super.render(g,mx,my,partial);}
    }
    private WybieAdventureClient(){}
}
