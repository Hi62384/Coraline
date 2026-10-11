package com.coraline.client;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Captions follow actual playback, including Wybie's positional encounter recordings. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class StorySubtitles {
    private record Playing(SoundInstance sound,JsonObject track,long start){}
    private static final List<Playing> playing=new ArrayList<>();
    private static JsonObject tracks;
    private static long ticks;
    private static String manualCaption="";
    private static long manualUntil;
    public static void stop(String name){playing.removeIf(p->p.sound.getLocation().getPath().equals(name));}
    public static void caption(String speaker,String text,int duration){manualCaption=speaker+": "+text;manualUntil=ticks+Math.max(1,duration);}
    // LOWEST is supported by older Forge 1.20.1 event buses; capture after the LOW music guard.
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void sound(PlaySoundEvent event){
        var sound=event.getSound();var mc=Minecraft.getInstance();
        if(sound==null||mc.player==null||!sound.getLocation().getNamespace().equals("coraline"))return;
        if(tracks==null)try(var reader=new java.io.InputStreamReader(Objects.requireNonNull(StorySubtitles.class.getResourceAsStream("/assets/coraline/story_subtitles.json")),java.nio.charset.StandardCharsets.UTF_8)){
            tracks=JsonParser.parseReader(reader).getAsJsonObject();
        }catch(Exception failure){throw new IllegalStateException("Story subtitles missing",failure);}
        var track=tracks.getAsJsonObject(sound.getLocation().getPath());if(track==null)return;
        if(!sound.isRelative()&&mc.player.distanceToSqr(sound.getX(),sound.getY(),sound.getZ())>1024)return;
        playing.removeIf(p->p.track.get("speaker").getAsString().equals(track.get("speaker").getAsString()));
        playing.add(new Playing(sound,track,ticks));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null){playing.clear();manualCaption="";manualUntil=0;return;}
        if(mc.isPaused())return;ticks++;
        playing.removeIf(p->ticks-p.start>40&&!mc.getSoundManager().isActive(p.sound));
    }
    @SubscribeEvent public static void render(RenderGuiEvent.Post event){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.options.hideGui)return;
        int width=Math.min(420,mc.getWindow().getGuiScaledWidth()-24),y=mc.getWindow().getGuiScaledHeight()-82;
        if(ticks<manualUntil&&!manualCaption.isEmpty()){
            var lines=mc.font.split(Component.literal(manualCaption),width);int top=y-lines.size()*11;
            for(var line:lines){int x=(mc.getWindow().getGuiScaledWidth()-mc.font.width(line))/2;
                event.getGuiGraphics().fill(x-4,top-2,x+mc.font.width(line)+4,top+10,0xB0000000);
                event.getGuiGraphics().drawString(mc.font,line,x,top,0xFFF1DC,true);top+=11;}
            y-=lines.size()*11+8;
        }
        for(int i=playing.size()-1;i>=0;i--){
            var p=playing.get(i);double elapsed=(ticks-p.start)/20.0*Math.max(.01,p.sound.getPitch());
            for(var element:p.track.getAsJsonArray("cues")){
                var cue=element.getAsJsonObject();if(elapsed<cue.get("start").getAsDouble()||elapsed>=cue.get("end").getAsDouble())continue;
                var lines=mc.font.split(Component.literal(p.track.get("speaker").getAsString()+": "+cue.get("text").getAsString()),width);
                int top=y-lines.size()*11;
                for(var line:lines){int x=(mc.getWindow().getGuiScaledWidth()-mc.font.width(line))/2;
                    event.getGuiGraphics().fill(x-4,top-2,x+mc.font.width(line)+4,top+10,0xB0000000);
                    event.getGuiGraphics().drawString(mc.font,line,x,top,0xFFF1DC,true);top+=11;}
                y-=lines.size()*11+8;break;
            }
        }
    }
}
