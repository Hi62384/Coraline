package com.coraline.feast;

import com.coraline.otherworld.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Disposable post-invitation checkpoint. Normal reserved-seat interaction,
 * production theatre ticks and genuine client mixer ACKs; never seeks, edits
 * playback clocks or awards story completion. Not whole-story acceptance.
 */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class NauticalOpeningVisualCheck {
    private static boolean requested,stopping,finalClamCaptured;
    private static volatile boolean ready,seated,zoomed;
    private static volatile double measuredZoomFov=Double.NaN;
    private static int ticks,lastCue=-1,heardMask,captures;
    private static java.util.UUID spink,forcible;
    private static volatile String capture;
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("coraline.nauticalOpeningCheck")||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;mc.options.framerateLimit().set(30);
        if(mc.player==null||mc.getSingleplayerServer()==null||mc.player.tickCount<60||stopping)return;
        if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)mc.setScreen(null);
        if(seated&&!zoomed&&com.coraline.client.VisitEventClient.showVenue()&&mc.player.getVehicle() instanceof CinematicPropEntity seat&&seat.kind()==CinematicPropEntity.SEAT&&seat.slot()==0){
            // Exercise the normal V handler for detailed reference captures,
            // without changing the camera location or forcing a custom FOV.
            try{
                // Seat attachment can arrive before the ordinary status packet
                // enables opera glasses. Never label an ignored V key as zoom.
                var enabled=com.coraline.client.TheatreViewClient.class.getDeclaredField("available");enabled.setAccessible(true);
                if(enabled.getBoolean(null)){
                    com.coraline.client.TheatreViewClient.key(new net.minecraftforge.client.event.InputEvent.Key(org.lwjgl.glfw.GLFW.GLFW_KEY_V,0,org.lwjgl.glfw.GLFW.GLFW_PRESS,0));
                    var toggled=com.coraline.client.TheatreViewClient.class.getDeclaredField("zoom");toggled.setAccessible(true);
                    require(toggled.getBoolean(null),"Normal V key did not enable opera glasses");
                    mc.options.hideGui=true;zoomed=true;
                }
            }catch(Throwable error){fail(error);}
        }
        if(!requested){requested=true;mc.getSingleplayerServer().execute(()->{
            try{stage(mc.getSingleplayerServer().getPlayerList().getPlayers().get(0));ready=true;}catch(Throwable error){fail(error);}
        });}
        var shot=capture;if(shot!=null){capture=null;try{
            var out=java.nio.file.Path.of("../artwork/nautical_native_qa");java.nio.file.Files.createDirectories(out);
            try(var image=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(out.resolve(shot+".png"));}captures++;
        }catch(Throwable error){fail(error);}}
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void measuredFov(net.minecraftforge.client.event.ViewportEvent.ComputeFov event){
        if(Boolean.getBoolean("coraline.nauticalOpeningCheck")&&zoomed&&!stopping&&com.coraline.client.VisitEventClient.showVenue())measuredZoomFov=event.getFOV();
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void observe(TickEvent.PlayerTickEvent event){
        if(!Boolean.getBoolean("coraline.nauticalOpeningCheck")||!ready||stopping||event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer p))return;
        try{
            ticks++;var s=OtherMotherLoop.scene(p);require(s!=null&&TheatreVisit.owned(p,s),"Lost owned theatre checkpoint");
            var n=TheatreVisit.data(s);require(n.getString("Blocker").isEmpty(),"Production show blocked: "+n.getString("Blocker"));
            require(!n.getBoolean("NeedsRecovery")&&!n.getBoolean("DebugPaused")&&!ShowPlayback.debugSeeking(s),"Show paused or used debug playback");
            if(!seated&&ticks>=100){
                var seat=TheatreSite.ensure(p,s,p.serverLevel(),CinematicPropEntity.SEAT,TheatreSite.playerSeat(p,s),0);
                require(TheatreVisit.interact(p,s,seat)&&p.getVehicle()==seat,"Normal reserved-seat interaction failed");
                spink=ShowActors.actor(p,s,"other_miss_spink").getUUID();forcible=ShowActors.actor(p,s,"other_miss_forcible").getUUID();seated=true;
                capture="seated_closed";return;
            }
            if(!seated)return;
            require(p.getVehicle() instanceof CinematicPropEntity seat&&seat.owned(s)&&seat.slot()==0,"Owner left reserved seat");
            require(ShowActors.actor(p,s,"other_miss_spink").getUUID().equals(spink)&&ShowActors.actor(p,s,"other_miss_forcible").getUUID().equals(forcible),"Performer identity changed");
            if(n.getInt("OpeningSeatDelay")<60){
                require(!ShowPlayback.started(s)&&ShowPlayback.data(s).getString("Sound").isEmpty(),"Opening played before 60 seated ticks");
                var curtains=p.serverLevel().getEntitiesOfClass(CinematicPropEntity.class,new AABB(TheatreSite.center(p,s)).inflate(50),e->e.owned(s)&&e.kind()==CinematicPropEntity.CURTAIN);
                require(curtains.size()==1&&curtains.get(0).value()==0,"Curtain opened during seated delay");return;
            }
            int cue=n.getInt("OpeningCue");
            require(cue>=lastCue&&cue<=lastCue+1||lastCue<0,"Opening skipped/reversed a cue");
            if(cue!=lastCue){lastCue=cue;System.out.println("NAUTICAL_NATIVE_PROGRESS cue="+cue+" heardMask="+heardMask+" ticks="+ticks);}
            // The production controller increments OpeningCue at the END of
            // the completed recording tick. Its old, finished binding remains
            // until the next cue is started on the following ordinary tick.
            if(cue<14&&n.getBoolean("OpeningCueStarted")&&ShowPlayback.started(s)){
                require(ShowPlayback.data(s).getString("Sound").equals(NauticalOpeningPlan.sound((char)('A'+cue))),"Wrong original recording");
                heardMask|=1<<cue;
                if(ShowPlayback.position(s)==Math.max(1,ShowRecordings.duration(NauticalOpeningPlan.sound((char)('A'+cue)))/2))capture="opening_"+(char)('A'+cue);
                if(cue==5&&!finalClamCaptured&&ShowPlayback.position(s)>=ShowRecordings.duration(NauticalOpeningPlan.sound('F'))-12){capture="opening_F_final";finalClamCaptured=true;}
            }
            if(n.getInt("Segment")==1){
                require(cue==14&&heardMask==(1<<14)-1,"Not all 14 originals actually started");
                require(n.getString("Checkpoint").equals("ACT_ONE_COMPLETE")&&!n.getBoolean("PerformanceDone"),"Opening skipped later preserved acts");
                var sets=p.serverLevel().getEntitiesOfClass(CinematicPropEntity.class,new AABB(TheatreSite.center(p,s)).inflate(50),e->e.owned(s));
                require(sets.stream().anyMatch(e->e.kind()==CinematicPropEntity.CURTAIN&&e.value()==0)&&sets.stream().anyMatch(e->e.kind()==CinematicPropEntity.OCEAN_SET&&e.cue()==-1),"N failed to close the curtain and hide/reset the opening set");
                require(captures>=5,"Insufficient actual renderer captures");
                require(zoomed&&Double.isFinite(measuredZoomFov)&&measuredZoomFov>0&&measuredZoomFov<Minecraft.getInstance().options.fov().get()*.5,"Opera-glass captures were not rendered with actual zoom");
                System.out.println("NAUTICAL_NATIVE_ZOOM_VERIFIED actual viewport FOV="+measuredZoomFov);
                System.out.println("NAUTICAL_OPENING_NATIVE_COMPLETE actual online owner, normal seat, 60-tick delay, all A-N real-mixer cues, original actors, transition to unchanged diving; checkpoint only");
                stopping=true;Minecraft.getInstance().execute(Minecraft.getInstance()::stop);
            }
            require(ticks<24000,"Opening checkpoint exceeded its bounded test duration");
        }catch(Throwable error){fail(error);}
    }
    static void stage(ServerPlayer p){
        var house=new BlockPos(36000,160,36000);var door=new DoorData.Door();door.owner=p.getUUID();door.placed=true;door.pos=house;door.activeUntil=Long.MAX_VALUE;DoorData.get(p.server).doors.put(door.id,door);
        var realm=new RealmData.Realm();realm.door=door.id;realm.house=house;realm.doorPos=house;realm.table=house;realm.piano=house;realm.ready=realm.islandComplete=true;realm.radius=100;RealmData.get(p.server).realms.put(door.id,realm);
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=door.id;s.stage=VisitData.Stage.DONE;s.table=house;s.piano=house;s.loop.trust=.5F;s.loop.suspicion=.1F;s.loop.phase=OtherMotherLoopState.Phase.THEATRE_ACTIVE;
        s.loop.event.putString("Type","THEATRE");s.loop.event.putInt("Serial",91);s.loop.event.putBoolean("TheatrePending",true);
        var n=TheatreVisit.data(s);n.putUUID("Owner",s.owner);n.putInt("Serial",91);n.putString("Phase","SEATING");n.putBoolean("InvitationDone",true);n.putString("Checkpoint","BEFORE_SITTING");
        VisitData.get(p.server).scenes.put(s.owner,s);p.getPersistentData().putUUID("CoralineVisitDoor",s.door);
        for(String key:java.util.List.of("CoralineInTunnel","CoralineReturning","CoralineWhiteoutStart","CoralineWhiteoutMoved"))p.getPersistentData().remove(key);
        require(TheatreSite.prepare(p,s),"Actual theatre venue preparation failed");
        var at=TheatreSite.playerSeat(p,s);p.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);p.teleportTo(p.server.getLevel(ActivityWorld.KEY),at.getX()+.5,at.getY(),at.getZ()+1.5,180,0);
        System.out.println("NAUTICAL_NATIVE_STARTED disposable post-invitation checkpoint; no generated speech, captions, seeks or fabricated playback ACKs");
    }
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    private static void fail(Throwable error){if(stopping)return;stopping=true;com.mojang.logging.LogUtils.getLogger().error("NAUTICAL_OPENING_NATIVE_FAILED",error);Minecraft.getInstance().execute(Minecraft.getInstance()::stop);}
    private NauticalOpeningVisualCheck(){}
}
