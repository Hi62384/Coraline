package com.coraline.client;

import com.coraline.feast.BlackIsTraditional;
import com.coraline.secrets.SecretsNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RenderGuiEvent;

/** Owner-only first-person presentation; irreversible cues are controlled by the server. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class TraditionalClient {
    private static CompoundTag data=new CompoundTag();private static int age;
    public static void clear(){data=new CompoundTag();age=121;ScenePreludeMusic.set("traditional_music",false,null);var mc=Minecraft.getInstance();if(mc.screen instanceof Decision)mc.setScreen(null);}
    public static void receive(CompoundTag n){data=n.copy();age=0;LongFaceMusic.update();var mc=Minecraft.getInstance();ScenePreludeMusic.set("traditional_music",active()&&data.getBoolean("Prelude"),data.contains("Table")?net.minecraft.core.BlockPos.of(data.getLong("Table")):null);if(phase().equals("AWAITING_CHOICE")&&!StoryGroupView.guest()){if(!(mc.screen instanceof Decision))mc.setScreen(new Decision());}else if(mc.screen instanceof Decision)mc.setScreen(null);}
    public static boolean active(){return age<120&&data.contains("Phase");}
    @SubscribeEvent public static void proposalInput(net.minecraftforge.client.event.MovementInputUpdateEvent event){
        var p=Minecraft.getInstance().player;
        if(p!=null&&event.getEntity()==p&&p.getVehicle() instanceof com.coraline.feast.ScenePropEntity chair&&chair.proposalSeatLocked()){
            event.getInput().shiftKeyDown=false;p.setShiftKeyDown(false);
        }
    }
    public static String phase(){return active()?data.getString("Phase"):"";}
    public static int time(){int actual=VisitAudioClient.storyTicks(data.getString("Sound"));return active()?(actual>=0?actual:data.getInt("Ticks")+age):0;}
    public static int father(){return active()?data.getInt("Father"):-1;}
    public static int mother(){return active()?data.getInt("Mother"):-1;}
    public static int eyeColor(int id){if(VisitAudioClient.storyTicks(data.getString("Sound"))<0||time()>=20)return 0;if(id==mother()&&phase().equals("E"))return 0xED7FAA;if(id==father()&&phase().equals("F"))return 0xE34234;return 0;}
    public static boolean stretched(int id){return id==father()&&phase().equals("R")&&time()>=30;}
    public static float stretch(){return phase().equals("R")?Math.min(1,Math.max(0,(time()-30)/25F)):1;}
    public static boolean needleHeld(int id){return id==father()&&phase().equals("J")&&time()<com.coraline.feast.VisitAudio.length("black_is_traditional_j")-20;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.isPaused())return;if(mc.player==null||mc.level==null){data=new CompoundTag();age=120;return;}age++;if(!active()&&mc.screen instanceof Decision)mc.setScreen(null);if((phase().equals("N")||phase().equals("O"))&&mc.screen instanceof net.minecraft.client.gui.screens.InBedChatScreen)mc.setScreen(null);}
    public static boolean darkHouse(){return active()&&data.getBoolean("Dark");}
    @SubscribeEvent public static void bedHint(RenderGuiEvent.Post event){if(!active()||!data.contains("Bed"))return;String phase=phase();if(!phase.equals("RUN_UPSTAIRS")&&!phase.equals("M")&&!phase.equals("GO_TO_BED"))return;var mc=Minecraft.getInstance();if(mc.player==null)return;var target=net.minecraft.core.BlockPos.of(data.getLong("Bed")).getCenter();var delta=target.subtract(mc.player.position());float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));float angle=net.minecraft.util.Mth.wrapDegrees(yaw-mc.player.getYRot());String direction=Math.abs(angle)<25?"ahead":Math.abs(angle)>150?"behind you":angle>0?"to your right":"to your left";String floor=delta.y>2?"upstairs, ":"";var g=event.getGuiGraphics();g.drawCenteredString(mc.font,((phase.equals("M")||phase.equals("GO_TO_BED"))&&data.getInt("BarricadeProgress")<3?"Barricade the bedroom ("+data.getInt("BarricadeProgress")+"/3) — ":"Go to bed — ")+floor+direction+" ("+(int)Math.ceil(delta.length())+"m)",mc.getWindow().getGuiScaledWidth()/2,mc.getWindow().getGuiScaledHeight()-66,0xFFEEDDC4);}
    @SubscribeEvent public static void covers(RenderGuiEvent.Post event){if(!active())return;String phase=phase();if(!phase.equals("N")&&!phase.equals("O"))return;var mc=Minecraft.getInstance();if(!mc.options.getCameraType().isFirstPerson())return;
        float amount=phase.equals("N")?Math.min(1,time()/20F):time()<40?1:Math.max(0,1-(time()-40)/20F);
        int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight(),top=h-(int)(h*amount);var g=event.getGuiGraphics();
        if(amount>0)g.blit(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("coraline","textures/block/bed_cloth.png"),0,top,0,0,w,h-top,16,16);
        if(amount>0&&amount<1)g.fill(0,top,w,top+5,0xFF7B8E9D);
        float closed=phase.equals("N")?com.coraline.feast.CocoaBeetlesChoreography.ramp(time(),20,18):1-com.coraline.feast.CocoaBeetlesChoreography.ramp(time(),40,20);
        StoryEyelids.draw(g,w,h,closed,event.getPartialTick());
    }
    private static final class Decision extends Screen {
        Decision(){super(Component.literal("The offer"));}
        @Override public boolean isPauseScreen(){return false;}
        @Override public boolean shouldCloseOnEsc(){return false;}
        @Override protected void init(){int cx=width/2,cy=height/2;addRenderableWidget(Button.builder(Component.literal("Accept offer"),b->choose(false)).bounds(cx-105,cy+10,100,22).build());addRenderableWidget(Button.builder(Component.literal("Reject offer"),b->choose(true)).bounds(cx+5,cy+10,100,22).build());}
        void choose(boolean reject){if(!phase().equals("AWAITING_CHOICE"))return;SecretsNetwork.request(data.getUUID("Choice"),"traditional_choice",reject?"traditional:reject":"traditional:accept");if(reject)Minecraft.getInstance().setScreen(null);}
        @Override public void render(GuiGraphics g,int x,int y,float partial){int cx=width/2,cy=height/2;g.fill(cx-120,cy-40,cx+120,cy+45,0xED302535);g.drawCenteredString(font,"Black is traditional.",cx,cy-22,0xFFE8D3AD);super.render(g,x,y,partial);}
    }
    private TraditionalClient(){}
}
