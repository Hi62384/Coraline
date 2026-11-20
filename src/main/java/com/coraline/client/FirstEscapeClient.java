package com.coraline.client;

import com.coraline.secrets.SecretsNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Owner presentation clock uses the same actual recording-start acknowledgement as the story. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class FirstEscapeClient {
    private static CompoundTag data=new CompoundTag();private static int age=120;private static boolean storyCrawl,shoeUseDown;
    public static void clear(){FirstEscapeMusic.stop();data=new CompoundTag();age=121;shoeUseDown=false;ScenePreludeMusic.set("cake_baking",false,null);var mc=Minecraft.getInstance();if(mc.screen instanceof Apology)mc.setScreen(null);}
    public static void receive(CompoundTag packet){data=packet.copy();age=0;LongFaceMusic.update();var mc=Minecraft.getInstance();ScenePreludeMusic.set("cake_baking",packet.getBoolean("CocoaMusic"),null);if(phase().equals("APOLOGY_CHOICE")&&!StoryGroupView.guest()){if(!(mc.screen instanceof Apology))mc.setScreen(new Apology());}else if(mc.screen instanceof Apology)mc.setScreen(null);}
    public static boolean active(){return age<120&&data.contains("Phase");}
    public static boolean prison(){return com.coraline.feast.FirstEscapeVisualRules.prison(phase())||phase().equals("ESCAPE_A")&&!flag("Rescued")||(phase().equals("COCOA_G")||phase().equals("MIRROR_THROW"))&&flag("MirrorPrisonEntered");}
    public static net.minecraft.core.BlockPos tunnelOrigin(){return active()&&data.contains("EscapeTunnel")?net.minecraft.core.BlockPos.of(data.getLong("EscapeTunnel")):null;}
    public static net.minecraft.core.Direction tunnelDirection(){var direction=net.minecraft.core.Direction.byName(data.getString("EscapeDirection"));return direction==null?net.minecraft.core.Direction.SOUTH:direction;}
    public static String phase(){return active()?data.getString("Phase"):"";}
    public static int time(){int actual=VisitAudioClient.storyTicks(data.getString("Sound"));return active()?(actual>=0?actual:data.getInt("Ticks")+(data.getBoolean("ClockRunning")?age:0)):0;}
    public static int duration(){return data.getInt("Duration");}
    public static boolean knockedDown(){return active()&&java.util.Set.of("ESCAPE_A","MASK_REMOVAL").contains(phase());}
    public static int knockdown(){return data.contains("KnockdownAge")?data.getInt("KnockdownAge")+age:-1;}
    public static boolean flag(String flag){return active()&&data.getBoolean(flag);}
    public static boolean decayed(java.util.UUID door){return flag("DecayedTunnel")&&data.hasUUID("Door")&&data.getUUID("Door").equals(door);}
    public static boolean decayedTunnel(){return active()&&(flag("DecayedTunnel")||java.util.Set.of("ESCAPE_E","ESCAPE_F","ESCAPE_G","ESCAPE_PUSH","ESCAPE_TUNNEL").contains(phase()));}
    public static int mother(){return active()?data.getInt("Mother"):-1;}
    public static boolean inheritedDarkHouse(){return active()&&com.coraline.feast.FirstEscapeVisualRules.inheritedDarkHouse(phase());}
    public static boolean brightParlor(){
        var mc=Minecraft.getInstance();if(!active()||mc.player==null||!com.coraline.feast.FirstEscapeVisualRules.brightParlor(phase()))return false;
        if(com.coraline.feast.FirstEscapeVisualRules.carryParlorLight(phase(),held()))return true;
        return data.contains("RoomMin")&&data.contains("RoomMax")&&new net.minecraft.world.phys.AABB(net.minecraft.core.BlockPos.of(data.getLong("RoomMin")),net.minecraft.core.BlockPos.of(data.getLong("RoomMax"))).contains(mc.player.position());
    }
    public static float parlorLightBlend(){return brightParlor()?com.coraline.feast.FirstEscapeVisualRules.parlorBlend(phase(),time()):0;}
    public static boolean angry(int actor){return actor==mother()&&(phase().equals("COCOA_E")||phase().equals("COCOA_F")||phase().equals("COCOA_G")||phase().equals("MIRROR_DRAG")||phase().equals("MIRROR_THROW"));}
    public static int wybie(){return active()?data.getInt("OtherWybie"):-1;}
    public static int realWybie(){return active()?data.getInt("RealWybie"):-1;}
    public static float morph(int actor){return morph(actor,0);}
    public static float morph(int actor,float partial){if(actor!=mother())return 0;String phase=phase();if(phase.equals("COCOA_F"))return Math.min(1,(time()+partial)/(float)Math.max(1,duration()));return phase.equals("COCOA_G")||phase.equals("MIRROR_DRAG")||phase.equals("MIRROR_THROW")?1:0;}
    private static boolean held(){var p=Minecraft.getInstance().player;return phase().equals("COCOA_G")&&p!=null&&p.getVehicle() instanceof com.coraline.feast.CinematicPropEntity prop&&prop.kind()==com.coraline.feast.CinematicPropEntity.GRAB_CARRIER;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null){data=new CompoundTag();age=120;shoeUseDown=false;return;}if(!mc.isPaused())age++;boolean use=mc.options.keyUse.isDown();if(mc.screen==null&&use&&!shoeUseDown&&!StoryGroupView.guest()&&phase().equals("THROW_SHOE")&&flag("ShoeMissed")&&data.hasUUID("Choice"))SecretsNetwork.request(data.getUUID("Choice"),"escape_shoe","retry");shoeUseDown=use;boolean crawl=phase().equals("ESCAPE_TUNNEL")||phase().equals("MIRROR_DRAG")||phase().equals("MIRROR_THROW")||held()||ParentRescueClient.crawl()||RescueClient.phase().equals("TUNNEL_CHASE");if(crawl){mc.player.setForcedPose(net.minecraft.world.entity.Pose.SWIMMING);mc.player.setPose(net.minecraft.world.entity.Pose.SWIMMING);}else if(storyCrawl&&mc.player.getForcedPose()==net.minecraft.world.entity.Pose.SWIMMING)mc.player.setForcedPose(null);storyCrawl=crawl;if(!active()&&mc.screen instanceof Apology)mc.setScreen(null);}
    @SubscribeEvent public static void mirrorDistortion(net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles e){if(!active()||!phase().equals("MIRROR_THROW"))return;float t=Math.min(1,time()/30F);e.setRoll(e.getRoll()+(float)Math.sin(t*Math.PI*4)*t*12);}
    private static final class Apology extends Screen {
        Apology(){super(Component.literal("Other Mother's offer"));}
        @Override public boolean isPauseScreen(){return false;}
        @Override public boolean shouldCloseOnEsc(){return false;}
        @Override protected void init(){int w=Math.min(330,width-30),x=(width-w)/2,y=height/2;addRenderableWidget(Button.builder(Component.literal("Apologize (let her sew buttons in your eyes)"),b->choose(false)).bounds(x,y,w,24).build());addRenderableWidget(Button.builder(Component.literal("Don't apologize"),b->choose(true)).bounds(x,y+30,w,24).build());}
        private void choose(boolean reject){if(!phase().equals("APOLOGY_CHOICE"))return;SecretsNetwork.request(data.getUUID("Choice"),"escape_choice",reject?"escape:reject":"escape:apologize");Minecraft.getInstance().setScreen(null);}
        @Override public void render(GuiGraphics g,int x,int y,float partial){g.fill(width/2-180,height/2-35,width/2+180,height/2+65,0xDB26152B);g.drawCenteredString(font,"Other Mother is waiting.",width/2,height/2-22,0xFFEEDCD1);super.render(g,x,y,partial);}
    }
    private FirstEscapeClient(){}
}
