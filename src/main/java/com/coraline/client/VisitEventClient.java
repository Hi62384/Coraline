package com.coraline.client;

import com.coraline.secrets.SecretsNetwork;
import com.coraline.feast.VisitConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Locale;

/** Small owner-only dialogue panel, quiet timer and one positionally attenuated piano. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class VisitEventClient {
    private static String phase="";private static int remaining,age=100;
    private static boolean showObjective,showVenue;
    private static boolean quietTheatre;
    public static boolean quietTheatre(){return quietTheatre&&showVenue();}
    public static boolean showVenue(){var mc=Minecraft.getInstance();return age<=60&&showVenue&&mc.level!=null&&com.coraline.feast.ActivityWorld.isActivity(mc.level);}
    private static String objective="";private static BlockPos waypoint,deliveryWaypoint;private static boolean parentsReady,kidsMarker;
    private static Piano piano;private static net.minecraft.client.multiplayer.ClientLevel world;
    private static GameRoomMusic gameMusic;
    private static DinnerMusic dinnerMusic;
    private static String dinnerHint="";
    private static boolean packageUseDown;private static java.util.UUID packageUseId;
    public static boolean receive(CompoundTag n){
        var mc=Minecraft.getInstance();
        if(n.contains("GhostVictim")){GhostVictimClient.receive(n);return true;}
        if(n.getBoolean("AcceptedButtonEyes")){AcceptedButtonEyesClient.receive(n);return true;}
        if(n.getBoolean("CatEncounter")){CatEncounterClient.receive(n);return true;}
        if(n.getBoolean("FamilyGame")){TabletopGameScreen.receive(n);return true;}
        if(n.getBoolean("RescueChapter")){RescueClient.receive(n);return true;}
        if(n.getBoolean("ParentRescue")){ParentRescueClient.receive(n);return true;}
        if(n.getBoolean("FirstEscapeStory")){FirstEscapeClient.receive(n);return true;}
        if(n.getBoolean("BlackIsTraditional")){TraditionalClient.receive(n);return true;}
        if(n.getBoolean("OutingTelescope")){mc.setScreen(new TelescopeScreen(n));return true;}
        if(n.getBoolean("VisitConversation")){
            if(n.getBoolean("Close")){if(mc.screen instanceof ConversationScreen)mc.setScreen(null);}
            else mc.setScreen(new ConversationScreen(n));return true;
        }
        if(!n.getBoolean("VisitEventStatus"))return false;
        quietTheatre=n.getBoolean("TheatreQuiet");if(quietTheatre)VisitAudioClient.quietAudience();
        PartyAmbienceClient.receive(n);
        ScenePreludeMusic.set("theatre_arrival_music",false,null);
        ScenePreludeMusic.setEntrance(n.getBoolean("TheatrePrelude"),n.contains("TheatrePreludeAt")?BlockPos.of(n.getLong("TheatrePreludeAt")):null);
        TheatreViewClient.available(n.getBoolean("TheatreOperaGlasses"));
        phase=n.getString("Phase");remaining=n.getInt("Remaining");showVenue=n.getBoolean("ShowVenue");age=0;
        dinnerHint=n.getString("DinnerHint");
        boolean dinner=n.getBoolean("DinnerMusic")&&mc.level!=null;
        var dinnerAt=BlockPos.of(n.getLong("DinnerPosition"));
        if(!dinner||dinnerMusic!=null&&(dinnerMusic.level!=mc.level||!dinnerMusic.at.equals(dinnerAt)))stopDinnerMusic();
        if(dinner&&dinnerMusic==null){dinnerMusic=new DinnerMusic(dinnerAt,mc.level);mc.getSoundManager().play(dinnerMusic);}
        boolean roomActive=n.getBoolean("GameRoomMusic")&&mc.level!=null;
        var roomAt=BlockPos.of(n.getLong("GameRoomPosition"));
        if(!roomActive||gameMusic!=null&&(gameMusic.level!=mc.level||!gameMusic.at.equals(roomAt)))stopGameMusic();
        if(roomActive&&gameMusic==null){gameMusic=new GameRoomMusic(roomAt,mc.level);mc.getSoundManager().play(gameMusic);}
        if(gameMusic!=null)gameMusic.target=n.getBoolean("GameRoomMusicPlaying")?.13F:0;
        kidsMarker=n.getBoolean("KidsMarker");deliveryWaypoint=n.getBoolean("DeliveryMarker")?BlockPos.of(n.getLong("DeliveryWaypoint")):null;showObjective=n.contains("ShowObjective");objective=showObjective?n.getString("ShowObjective"):n.getString("Objective");parentsReady=n.getBoolean("ParentsReady");waypoint=kidsMarker?BlockPos.of(n.getLong("KidsWaypoint")):n.getBoolean("OutingMarker")?BlockPos.of(n.getLong("OutingWaypoint")):null;
        boolean active=n.getBoolean("PianoActive")&&mc.level!=null,playing=n.getBoolean("PianoPlaying");
        var at=BlockPos.of(n.getLong("Piano"));
        if(!active||world!=mc.level||piano!=null&&!piano.at.equals(at))stop();
        if(active&&piano==null){world=mc.level;piano=new Piano(at);mc.getSoundManager().play(piano);}if(piano!=null)piano.target=playing?.65F:0;return true;
    }
    private static void stop(){if(piano!=null)Minecraft.getInstance().getSoundManager().stop(piano);piano=null;world=null;}
    private static void stopGameMusic(){if(gameMusic!=null)Minecraft.getInstance().getSoundManager().stop(gameMusic);gameMusic=null;}
    private static void stopDinnerMusic(){if(dinnerMusic!=null)Minecraft.getInstance().getSoundManager().stop(dinnerMusic);dinnerMusic=null;}
    public static boolean spatialPerformance(SoundInstance sound){return sound instanceof Piano||sound instanceof GameRoomMusic||sound instanceof DinnerMusic;}
    public static boolean pianoIsSpatial(BlockPos at){return piano!=null&&piano.at.equals(at)&&!piano.isRelative()&&piano.getAttenuation()==SoundInstance.Attenuation.LINEAR&&Minecraft.getInstance().getSoundManager().isActive(piano);}
    public static boolean dialogueOpen(){return Minecraft.getInstance().screen instanceof ConversationScreen;}
    @SubscribeEvent public static void dinnerLifecycle(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.isPaused())return;if(mc.player==null||!mc.player.isAlive()||age>60||dinnerMusic!=null&&dinnerMusic.level!=mc.level){stopDinnerMusic();dinnerHint="";}}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.isPaused())return;age++;if(mc.player!=null){boolean use=mc.options.keyUse.isDown();if(use)for(var stack:new net.minecraft.world.item.ItemStack[]{mc.player.getMainHandItem(),mc.player.getOffhandItem()})if(stack.is(com.coraline.feast.SpecialDelivery.PACKAGE.get())&&stack.hasTag()&&stack.getTag().hasUUID("DeliveryId"))packageUseId=stack.getTag().getUUID("DeliveryId");if(packageUseDown&&!use&&packageUseId!=null){SecretsNetwork.request(packageUseId,"delivery_release","");packageUseId=null;}packageUseDown=use;}if(mc.player==null||mc.level!=world&&piano!=null||age>60){stop();stopGameMusic();if(age>60){phase="";waypoint=null;deliveryWaypoint=null;showObjective=false;objective="";}}}
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e){if(!phase.equals("FREE_TIME")&&!phase.equals("DINNER_WAIT")&&waypoint==null&&!showObjective)return;var mc=Minecraft.getInstance();if(mc.options.hideGui||mc.player==null)return;String name=phase.equals("FREE_TIME")?"Free Time":showObjective||waypoint!=null?objective:"Dinner";String text=remaining>0?name+" — "+String.format(Locale.ROOT,"%d:%02d",(remaining+19)/1200,((remaining+19)/20)%60):showObjective?name:name+" is waiting";int w=mc.font.width(text);var hints=mc.font.split(Component.literal(dinnerHint),Math.max(80,Math.min(340,e.getGuiGraphics().guiWidth()-24)));e.getGuiGraphics().fill(6,6,Math.max(w+14,dinnerHint.isEmpty()?0:Math.min(354,e.getGuiGraphics().guiWidth()-10)),waypoint==null?(dinnerHint.isEmpty()?21:25+hints.size()*10):36,0x80302038);e.getGuiGraphics().drawString(mc.font,text,10,10,0xFFE9D9BD);if(!dinnerHint.isEmpty())for(int i=0;i<hints.size();i++)e.getGuiGraphics().drawString(mc.font,hints.get(i),10,24+i*10,0xFFFFCE80);if(waypoint!=null){double distance=Math.sqrt(mc.player.distanceToSqr(waypoint.getCenter()));double angle=net.minecraft.util.Mth.wrapDegrees(Math.toDegrees(Math.atan2(-(waypoint.getX()-mc.player.getX()),waypoint.getZ()-mc.player.getZ()))-mc.player.getYRot());String arrow=Math.abs(angle)<25?"Ahead":angle<0?"Left":"Right";String hint=distance<8&&parentsReady?"Right-click to join your family":arrow+" · "+Math.round(distance)+" blocks"+(kidsMarker?(Math.abs(mc.player.getY()-waypoint.getY())>2?" · "+(mc.player.getY()<waypoint.getY()?"Upstairs":"Downstairs"):" · This floor"):!parentsReady?" · Parents traveling":"");e.getGuiGraphics().drawString(mc.font,hint,10,24,0xFFFFCE80);}}
    public static void drawOutingMap(GuiGraphics g,int cx,int cy,int radius){var mc=Minecraft.getInstance();var marker=deliveryWaypoint!=null?deliveryWaypoint:waypoint;if(marker==null||mc.player==null||age>60)return;int x=cx+(marker.getX()-mc.player.getBlockX())/3,y=cy+(marker.getZ()-mc.player.getBlockZ())/3;if(Math.abs(x-cx)>radius||Math.abs(y-cy)>radius)return;g.fill(x-3,y-3,x+3,y+3,0xFFF0B454);g.drawString(mc.font,deliveryWaypoint!=null?"Delivery":"Family",x+5,y-4,0xFF76551C,false);}
    private static final class TelescopeScreen extends Screen {
        final CompoundTag data;int observed;final java.util.Set<Integer> chosen=new java.util.HashSet<>();int[][] stars;int glowTicks;boolean submitted;
        TelescopeScreen(CompoundTag n){super(Component.literal("Telescope"));data=n;observed=n.getInt("Observation");}
        @Override public boolean isPauseScreen(){return false;}
        @Override protected void init(){stars=com.coraline.feast.ConstellationPuzzle.stars(data.getLong("Seed"),observed);for(var star:stars){star[0]+=width/2;star[1]+=height/2;}if(data.getBoolean("Complete"))glowTicks=45;}
        @Override public void tick(){if(data.getBoolean("Complete")&&--glowTicks<=0)onClose();}
        @Override public void render(GuiGraphics g,int mx,int my,float partial){int cx=width/2,cy=height/2;g.fill(cx-125,cy-113,cx+125,cy+113,0xED090F28);
            g.drawCenteredString(font,data.getBoolean("Complete")?"All six constellations found":"Constellation "+(observed+1)+" / 6 — match the chart",cx,cy-101,0xFFF3D49B);
            if(!data.getBoolean("Complete")){var target=com.coraline.feast.ConstellationPuzzle.targets(data.getLong("Seed"),observed);int[] ids=target.stream().mapToInt(Integer::intValue).toArray();
                for(int i=0;i<ids.length;i++){var a=stars[ids[i]];var b=stars[ids[(i+1)%ids.length]];int ax=cx+(a[0]-cx)/4,ay=cy-76+(a[1]-cy)/4,bx=cx+(b[0]-cx)/4,by=cy-76+(b[1]-cy)/4;int steps=Math.max(1,Math.max(Math.abs(bx-ax),Math.abs(by-ay)));for(int j=0;j<=steps;j++)g.fill(ax+(bx-ax)*j/steps,ay+(by-ay)*j/steps,ax+(bx-ax)*j/steps+1,ay+(by-ay)*j/steps+1,0xFFE9CE98);g.fill(ax-1,ay-1,ax+2,ay+2,0xFFFFFFFF);}
                for(int i=0;i<stars.length;i++){int[] star=stars[i];int color=chosen.contains(i)?0xFF9CF4FF:0xFFE7E7D1;g.fill(star[0]-2,star[1]-2,star[0]+3,star[1]+3,color);}
                g.drawCenteredString(font,data.getBoolean("Retry")?"Not quite — try that pattern again":"Select "+com.coraline.feast.ConstellationPuzzle.count(observed)+" stars · Escape to leave",cx,cy+96,0xFFB2C0D7);
            }super.render(g,mx,my,partial);
        }
        @Override public boolean mouseClicked(double x,double y,int button){if(submitted||data.getBoolean("Complete")||button!=0)return true;for(int i=0;i<stars.length;i++)if(Math.hypot(x-stars[i][0],y-stars[i][1])<9){if(!chosen.add(i))chosen.remove(i);if(chosen.size()==com.coraline.feast.ConstellationPuzzle.count(observed)){String selection=chosen.stream().sorted().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));SecretsNetwork.request(data.getUUID("Nonce"),"visit_choice","star_selection:"+selection);submitted=true;}return true;}return super.mouseClicked(x,y,button);}
    }
    /** Reuses the existing licensed gentle tea instrumental, quietly in this room only. */
    private static final class DinnerMusic extends AbstractTickableSoundInstance {
        final BlockPos at;final net.minecraft.client.multiplayer.ClientLevel level;
        DinnerMusic(BlockPos at,net.minecraft.client.multiplayer.ClientLevel level){super(SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline","mysterious_supper")),SoundSource.RECORDS,RandomSource.create());this.at=at;this.level=level;x=at.getX()+.5;y=at.getY()+1;z=at.getZ()+.5;volume=0;relative=false;attenuation=Attenuation.LINEAR;looping=true;delay=0;}
        @Override public boolean canStartSilent(){return true;}
        @Override public void tick(){var mc=Minecraft.getInstance();if(mc.level!=level||mc.player==null||!mc.player.isAlive()||age>60||!phase.startsWith("DINNER")){stop();return;}float gain=VisitConfig.MUSIC_VOLUME.get().floatValue();float target=(VisitAudioClient.voicePlaying()||dialogueOpen()?.085F:.24F)*gain;volume=net.minecraft.util.Mth.lerp(.12F,volume,target);}
    }
    private static final class GameRoomMusic extends AbstractTickableSoundInstance {
        final BlockPos at;final net.minecraft.client.multiplayer.ClientLevel level;float target=.13F;
        GameRoomMusic(BlockPos at,net.minecraft.client.multiplayer.ClientLevel level){super(SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline","tea_visit_music")),SoundSource.RECORDS,RandomSource.create());this.at=at;this.level=level;x=at.getX()+.5;y=at.getY()+1;z=at.getZ()+.5;volume=0;relative=false;attenuation=Attenuation.LINEAR;looping=true;delay=0;}
        @Override public boolean canStartSilent(){return true;}
        @Override public void tick(){var mc=Minecraft.getInstance();if(mc.level!=level||mc.player==null||age>60){stop();return;}volume=net.minecraft.util.Mth.lerp(.15F,volume,target);}
    }
    private static final class Piano extends AbstractTickableSoundInstance {
        final BlockPos at;float target=.65F;
        Piano(BlockPos at){super(SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline","other_father_song")),SoundSource.RECORDS,RandomSource.create());this.at=at;x=at.getX()+.5;y=at.getY()+.8;z=at.getZ()+.5;volume=0;relative=false;attenuation=Attenuation.LINEAR;looping=true;delay=0;}
        @Override public boolean canStartSilent(){return true;}
        @Override public void tick(){var mc=Minecraft.getInstance();if(mc.level!=world||mc.player==null||age>60){stop();return;}double distance=mc.player.getEyePosition().distanceTo(new net.minecraft.world.phys.Vec3(x,y,z));float gain=(float)Math.max(0,1-distance/8);volume=target*gain*gain;}
    }
    private static final class ConversationScreen extends Screen {
        final CompoundTag data;private int left,top,w,h,readyTicks,choicePending;
        ConversationScreen(CompoundTag data){super(Component.literal(data.getString("Speaker")));this.data=data;readyTicks=data.getInt("ReadyTicks");}
        @Override public boolean isPauseScreen(){return false;}
        @Override public void tick(){if(readyTicks>0)readyTicks--;if(choicePending>0)choicePending--;for(var widget:renderables)if(widget instanceof Button b)b.active=readyTicks<=0&&choicePending==0;}
        private void choose(int choice){if(readyTicks>0||choicePending>0)return;choicePending=20;for(var widget:renderables)if(widget instanceof Button b)b.active=false;SecretsNetwork.request(data.getUUID("Nonce"),"visit_choice",Integer.toString(choice));}
        @Override protected void init(){w=Math.min(360,width-24);var lines=font.split(Component.literal(data.getString("Text")),w-28);var choices=data.getList("Options",8);h=55+lines.size()*10+choices.size()*24;left=(width-w)/2;top=(height-h)/2;
            for(int i=0;i<choices.size();i++){int choice=i;addRenderableWidget(Button.builder(Component.literal(choices.getString(i)),b->choose(choice)).bounds(left+14,top+42+lines.size()*10+i*24,w-28,20).build());}
            for(var widget:renderables)if(widget instanceof Button b)b.active=readyTicks<=0;
        }
        @Override public void render(GuiGraphics g,int mx,int my,float partial){g.fill(left-1,top-1,left+w+1,top+h+1,0xFFD2AD72);g.fill(left,top,left+w,top+h,0xE6292330);g.drawString(font,title,left+14,top+12,0xFFF4CE94);g.drawWordWrap(font,Component.literal(data.getString("Text")),left+14,top+29,w-28,0xFFECE3DA);super.render(g,mx,my,partial);}
        @Override public void onClose(){SecretsNetwork.request(data.getUUID("Nonce"),"visit_choice","close");super.onClose();}
    }
    private VisitEventClient(){}
}
