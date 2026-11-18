package com.coraline.client;

import com.coraline.secrets.SecretsNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Owner snapshot presentation and discrete input edges; never advances server progress. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class RescueClient {
    private static CompoundTag data=new CompoundTag();private static int age=121;private static boolean attackDown,useDown,visionQuiet;
    public static void clear(){ScenePreludeMusic.set("spink_and_forcible_boss_music",false,null);data=new CompoundTag();age=121;var mc=Minecraft.getInstance();if(mc.screen instanceof Choice)mc.setScreen(null);}
    public static void receive(CompoundTag packet){if(packet.getBoolean("AlternateGhostEscape"))AcceptedButtonEyesClient.clear();data=packet.copy();age=0;var mc=Minecraft.getInstance();String screen=screenPhase();if(!screen.isEmpty()){if(!(mc.screen instanceof Choice choice)||!choice.phase.equals(screen))mc.setScreen(new Choice(screen));}else if(mc.screen instanceof Choice)mc.setScreen(null);}
    public static boolean active(){return age<120&&Minecraft.getInstance().level!=null&&data.contains("Phase");}
    public static String phase(){return active()?data.getString("Phase"):"";}
    public static java.util.UUID inputSession(){return active()&&data.hasUUID("Input")?data.getUUID("Input"):null;}
    public static int time(){int actual=VisitAudioClient.storyTicks(data.getString("Sound"));return active()?actual>=0?actual:data.getInt("Ticks")+(data.getBoolean("ClockRunning")?age:0):0;}
    public static boolean flag(String name){return active()&&data.getBoolean(name);}
    public static float value(String name){return active()?data.getFloat(name):0;}
    public static long position(String name){return data.getLong(name);}
    public static int mother(){return active()?data.getInt("Mother"):-1;}
    public static int duration(){return active()?data.getInt("Duration"):0;}
    public static boolean celestial(){return phase().equals("CELESTIAL_OPEN")||phase().equals("ORBS")||phase().equals("FAREWELL_C");}
    public static float lids(float partial){return phase().equals("FAREWELL_B")&&flag("FarewellEyesClosing")?Math.min(1,(Math.max(0,time()-460)+partial)/40F):phase().equals("EYES_CLOSE")?Math.min(1,(time()+partial)/40F):phase().equals("CELESTIAL_OPEN")?Math.max(0,1-(time()+partial)/40F):0;}
    private static void send(String action){if(active()&&data.hasUUID("Input"))SecretsNetwork.request(data.getUUID("Input"),"rescue_action",action);}
    private static boolean visionLocked(){return phase().equals("FAREWELL_B")&&flag("FarewellEyesClosing")||java.util.Set.of("EYES_CLOSE","CELESTIAL_OPEN","ORBS","FAREWELL_C").contains(phase());}
    @SubscribeEvent public static void visionInput(InputEvent.InteractionKeyMappingTriggered event){if(visionLocked()){event.setCanceled(true);event.setSwingHand(false);}}
    @SubscribeEvent public static void quietVision(net.minecraftforge.client.event.sound.PlaySoundEvent e){
        if(!visionLocked())return;
        var sound=e.getSound();if(sound!=null&&sound.getSource()!=net.minecraft.sounds.SoundSource.MASTER&&sound.getSource()!=net.minecraft.sounds.SoundSource.VOICE&&sound.getSource()!=net.minecraft.sounds.SoundSource.MUSIC&&sound.getSource()!=net.minecraft.sounds.SoundSource.RECORDS)e.setSound(null);
    }
    private static String screenPhase(){String phase=phase();return phase.equals("SHOW_EYES")||phase.equals("HIDING_CHOICE")||phase.equals("FIGHT_CHOICE")||phase.equals("DOOR_STRUGGLE")?phase:"";}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null){data=new CompoundTag();age=121;attackDown=false;if(mc.screen instanceof Choice)mc.setScreen(null);return;}if(!mc.isPaused())age++;boolean quiet=visionLocked();if(quiet&&!visionQuiet){for(var source:net.minecraft.sounds.SoundSource.values())if(source!=net.minecraft.sounds.SoundSource.MASTER&&source!=net.minecraft.sounds.SoundSource.VOICE&&source!=net.minecraft.sounds.SoundSource.MUSIC&&source!=net.minecraft.sounds.SoundSource.RECORDS)mc.getSoundManager().stop(null,source);}visionQuiet=quiet;if(quiet&&mc.gameMode!=null)mc.gameMode.stopDestroyBlock();boolean use=mc.options.keyUse.isDown();if(mc.screen==null&&use&&!useDown&&(phase().equals("MANTIS_TAKE")||phase().equals("MANTIS_SINK")))send("take_eye");if(mc.screen==null&&use&&!useDown&&(phase().equals("THEATRE_SEARCH")||phase().equals("WRAPPER_REACH")||phase().equals("THEATRE_EYE")))send("theatre_eye");useDown=use;boolean attack=mc.options.keyAttack.isDown();if(mc.screen==null&&attack&&!attackDown&&phase().equals("CAT_THROW")&&flag("CatHeld"))send("throw_cat");attackDown=attack;if(phase().equals("TUNNEL_CHASE")){mc.player.setForcedPose(net.minecraft.world.entity.Pose.SWIMMING);mc.player.setPose(net.minecraft.world.entity.Pose.SWIMMING);}if(!active()&&mc.screen instanceof Choice)mc.setScreen(null);}
    @SubscribeEvent public static void bossMusic(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        // Combat snapshots stop this loop on victory, boss loss, or stale state.
        ScenePreludeMusic.setGain("spink_and_forcible_boss_music",java.util.Set.of("THEATRE_SEARCH","WRAPPER_REACH","THEATRE_GRAB","THEATRE_FIGHT").contains(phase()),.42F);
    }
    @SubscribeEvent public static void eyelids(RenderGuiEvent.Post event){float close=lids(event.getPartialTick());var graphics=event.getGuiGraphics();if(close>0)StoryEyelids.draw(graphics,graphics.guiWidth(),graphics.guiHeight(),close,event.getPartialTick());}
    private static final class Choice extends Screen {
        final String phase;private long lastPress=-10;private boolean holding;
        Choice(String phase){super(Component.literal(phase.equals("DOOR_STRUGGLE")?"Pull the door shut":"Your move"));this.phase=phase;}
        @Override public boolean isPauseScreen(){return false;}
        @Override public boolean shouldCloseOnEsc(){return false;}
        private void option(String label,String action,boolean enabled,int row){var button=Button.builder(Component.literal(label),b->send(action)).bounds(width/2-155,height/2+row*26,310,22).build();button.active=enabled;addRenderableWidget(button);}
        @Override protected void init(){switch(phase){case "SHOW_EYES"->{option("Retrieve and show the three ghost eyes","show_eyes",true,0);option("Close and arrange my inventory","close_eyes",true,1);}case "HIDING_CHOICE"->option("Behind the Little Door","behind_little_door",true,0);case "FIGHT_CHOICE"->option("Fight back","fight",true,0);default->{}}}
        private void pull(){
            var level=Minecraft.getInstance().level;
            if(level==null)return;
            long now=level.getGameTime();
            if(now>=lastPress+4||now<lastPress){send("struggle");lastPress=now;}
        }
        @Override public void tick(){super.tick();if(holding)pull();}
        @Override public boolean mouseClicked(double x,double y,int button){
            if(phase.equals("DOOR_STRUGGLE")&&button==0){holding=true;pull();return true;}
            return super.mouseClicked(x,y,button);
        }
        @Override public boolean mouseReleased(double x,double y,int button){
            if(phase.equals("DOOR_STRUGGLE")&&button==0){holding=false;return true;}
            return super.mouseReleased(x,y,button);
        }
        @Override public void render(GuiGraphics graphics,int x,int y,float partial){if(phase.equals("DOOR_STRUGGLE")){int w=220,c=width/2;graphics.fill(c-w/2,height-52,c+w/2,height-37,0xD922162B);graphics.fill(c-w/2+2,height-50,c-w/2+2+(int)((w-4)*value("Struggle")),height-39,0xFFD9BC86);graphics.drawCenteredString(font,"Hold left click to pull the door shut",c,height-66,0xFFFFE2B0);}else{graphics.fill(width/2-170,height/2-35,width/2+170,height/2+116,0xD922162B);graphics.drawCenteredString(font,title,width/2,height/2-25,0xFFFFE2B0);}super.render(graphics,x,y,partial);}
    }
    private RescueClient(){}
}
