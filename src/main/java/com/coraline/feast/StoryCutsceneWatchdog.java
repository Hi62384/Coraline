package com.coraline.feast;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Independent of story early returns: missing actors/paths must not stop their
 * own watchdog. Chapters own safe transitions; deliberate choices are untimed. */
@Mod.EventBusSubscriber(modid="coraline")
public final class StoryCutsceneWatchdog {
    static int voiceBudget(String voice){return Math.max(600,ShowRecordings.duration(voice)+600);}
    static boolean elapsed(CompoundTag state,String step,int increment,int budget){
        if(!state.getString("Step").equals(step)){state.putString("Step",step);state.putInt("Ticks",0);state.putBoolean("Fired",false);}
        if(state.getBoolean("Fired"))return false;
        int ticks=Math.min(144000,Math.max(0,state.getInt("Ticks"))+Math.max(0,Math.min(20,increment)));state.putInt("Ticks",ticks);
        if(ticks<Math.max(20,budget))return false;state.putBoolean("Fired",true);return true;
    }
    static boolean overdue(ServerPlayer p,VisitData.Scene s,String track,String step,int budget){
        var root=s.loop.story.getCompound("CutsceneSafety");var clock=root.getCompound(track);root.put(track,clock);s.loop.story.put("CutsceneSafety",root);
        if(!elapsed(clock,s.loop.event.getInt("Serial")+":"+step,20,budget))return false;
        root.putString("LastSkipped",track+" / "+step);root.putLong("LastSkippedAt",p.server.overworld().getGameTime());
        var history=root.getList("History",10);var entry=new CompoundTag();entry.putString("Step",track+" / "+step);entry.putLong("At",p.server.overworld().getGameTime());history.add(entry);while(history.size()>24)history.remove(0);root.put("History",history);
        p.displayClientMessage(net.minecraft.network.chat.Component.literal("A cutscene action stalled. Continuing safely…"),true);OtherMotherLoop.changed(p);return true;
    }
    static void retry(VisitData.Scene s,String track){var clock=s.loop.story.getCompound("CutsceneSafety").getCompound(track);clock.putBoolean("Fired",false);clock.putInt("Ticks",0);}
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void tick(TickEvent.PlayerTickEvent event){if(event.player instanceof net.minecraft.server.level.ServerPlayer coopPlayer&&com.coraline.multiplayer.StoryCoop.directorBlocked(coopPlayer))return;
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer p)||p.tickCount%20!=0||!p.isAlive())return;
        var s=VisitData.get(p.server).scenes.get(p.getUUID());if(s==null||!p.getUUID().equals(s.owner)||ShowPlayback.debugSeeking(s))return;
        // The accepted-route ghost deliberately uses spectator flight. It still
        // needs its authenticated scene watchdog even though ordinary spectators
        // must not advance other story chapters.
        if(GhostEscapeEnding.epilogueActive(s)){if(!RescueChapter.finished(s))RescueChapter.cutsceneSafety(p,s);return;}
        if(GhostVictimDirector.active(s)){GhostVictimDirector.cutsceneSafety(p,s);return;}
        if(p.isSpectator())return;
        if(RescueChapter.started(s)&&!RescueChapter.finished(s)){RescueChapter.cutsceneSafety(p,s);return;}
        if(FirstVisit.active(p)!=null){FirstVisit.cutsceneSafety(p,s);return;}
        if(ParentRescueStory.started(s)&&!ParentRescueStory.data(s).getBoolean("Complete")){ParentRescueStory.cutsceneSafety(p,s);return;}
        if(MissingParentsTea.active(s)){MissingParentsTea.cutsceneSafety(p,s);return;}
        if(MissingParentsCat.data(s).getBoolean("Started")&&!MissingParentsCat.data(s).getBoolean("Complete")){MissingParentsCat.cutsceneSafety(p,s);return;}
        if(FirstEscapeStory.engaged(s)&&FirstEscapeStory.phase(s)!=FirstEscapeTimeline.Phase.ESCAPE_COMPLETE){FirstEscapeStory.cutsceneSafety(p,s);return;}
        if(BlackIsTraditional.active(s)){BlackIsTraditional.cutsceneSafety(p,s);return;}
        VisitEvents.cutsceneSafety(p,s);
    }
    private StoryCutsceneWatchdog(){}
}
