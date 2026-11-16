package com.coraline.client;

import com.coraline.secrets.SecretsNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Owner-only ghost presentation. Short original child syllables are emitted
 * only during server-authored speech; English meanings remain timed subtitles.
 * Clearing an utterance stops its own syllable, never dialogue/music channels. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class GhostVictimClient {
    private static CompoundTag data=new CompoundTag();private static int age=120,talkAge;
    private static Syllable syllable;private static final RandomSource RANDOM=RandomSource.create();
    public static void receive(CompoundTag packet){
        var incoming=packet.getCompound("GhostVictim");
        if(!incoming.getBoolean("Active")){clear();return;}
        if(!data.getString("ChildWords").equals(incoming.getString("ChildWords"))||data.getLong("Utterance")!=incoming.getLong("Utterance")){stopSyllable();talkAge=0;}
        data=incoming.copy();age=0;
    }
    public static boolean active(){return age<100&&data.getBoolean("Active");}
    public static boolean ghost(){return active()&&!data.getString("Phase").equals("ENTER_MIRROR");}
    public static int victim(){return active()?data.getInt("VictimEntity"):-1;}
    public static boolean reflected(java.util.UUID id){return ghost()&&data.getBoolean("InsideMirror")&&data.hasUUID("MirrorEntity")&&id.equals(data.getUUID("MirrorEntity"));}
    public static float opacity(){return ghost()?.28F:1;}
    public static void clear(){stopSyllable();data=new CompoundTag();age=120;talkAge=0;}
    private static void stopSyllable(){if(syllable!=null){Minecraft.getInstance().getSoundManager().stop(syllable);syllable=null;}}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.isPaused())return;
        if(mc.player==null||mc.level==null){clear();return;}age++;
        if(!active()||data.getInt("TalkingTicks")<=age||data.getString("ChildWords").isBlank()){stopSyllable();return;}
        var child=mc.level.getEntity(victim());
        if(child==null||!data.hasUUID("VictimActor")||!child.getUUID().equals(data.getUUID("VictimActor"))){stopSyllable();return;}
        if(++talkAge%9==1&&mc.player.distanceToSqr(child)<1024){
            stopSyllable();syllable=new Syllable(child.getId(),.88F+RANDOM.nextFloat()*.28F);mc.getSoundManager().play(syllable);
        }
    }
    @SubscribeEvent public static void input(InputEvent.InteractionKeyMappingTriggered event){
        if(!active())return;
        if(event.isAttack()){if(ghost()){event.setCanceled(true);event.setSwingHand(false);}return;}
        if(!event.isUseItem())return;event.setCanceled(true);event.setSwingHand(false);
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||!data.hasUUID("Session"))return;
        var eye=mc.player.getEyePosition();var look=mc.player.getLookAngle();String selected="";double nearest=25;
        for(Tag entry:data.getList("InteractionPoints",Tag.TAG_COMPOUND)){
            var point=(CompoundTag)entry;var target=new Vec3(point.getDouble("X"),point.getDouble("Y"),point.getDouble("Z"));
            var delta=target.subtract(eye);double length=delta.lengthSqr();
            if(length<nearest&&length>.001&&look.dot(delta.normalize())>.965){nearest=length;selected=point.getString("Action");}
        }
        if(!selected.isEmpty())SecretsNetwork.request(data.getUUID("Session"),"ghost_victim",selected);
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post event){
        if(!active())return;var mc=Minecraft.getInstance();var graphics=event.getGuiGraphics();
        int width=mc.getWindow().getGuiScaledWidth(),height=mc.getWindow().getGuiScaledHeight();
        if(ghost()){
            bar(graphics,mc,"Manifestation",data.getDouble("Energy"),10,10,0xFF85C9E5);
            bar(graphics,mc,"Beldam's attention",data.getDouble("Attention"),10,34,0xFFCD627C);
        }
        String objective=data.getString("Objective");
        if(!objective.isBlank())graphics.drawCenteredString(mc.font,objective,width/2,height-72,0xE1E9F2);
        int remaining=Math.max(0,data.getInt("CountdownTicks")-age);
        if(remaining>0)graphics.drawCenteredString(mc.font,(remaining+19)/20+" seconds",width/2,height-59,0xAFC8D5);
        if(data.getInt("TalkingTicks")>age&&!data.getString("ChildWords").isBlank()){
            var lines=mc.font.split(Component.literal(data.getString("ChildName")+": "+data.getString("ChildWords")),Math.min(420,width-28));
            int y=height-110-lines.size()*10;for(var line:lines){int x=(width-mc.font.width(line))/2;graphics.fill(x-3,y-1,x+mc.font.width(line)+3,y+10,0xA0000000);graphics.drawString(mc.font,line,x,y,0xFFF1F1F1);y+=11;}
        }
    }
    private static void bar(net.minecraft.client.gui.GuiGraphics graphics,Minecraft mc,String title,double amount,int x,int y,int color){
        float value=(float)(Double.isFinite(amount)?Math.max(0,Math.min(1,amount)):0);
        graphics.drawString(mc.font,title,x,y,0xE6EDF5);graphics.fill(x,y+11,x+112,y+16,0xAA1C2330);graphics.fill(x,y+11,x+Math.round(112*value),y+16,color);
    }
    private static final class Syllable extends AbstractTickableSoundInstance {
        private final int child;private int life;
        Syllable(int child,float pitch){super(SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline","ghost_victim_talk")),SoundSource.VOICE,RANDOM);
            this.child=child;this.pitch=pitch;volume=.6F;looping=false;delay=0;relative=false;attenuation=Attenuation.LINEAR;position();}
        private void position(){var mc=Minecraft.getInstance();var actor=mc.level==null?null:mc.level.getEntity(child);if(actor==null){stop();return;}x=actor.getX();y=actor.getEyeY();z=actor.getZ();}
        @Override public void tick(){position();if(++life>12||!active())stop();}
    }
    private GhostVictimClient(){}
}
