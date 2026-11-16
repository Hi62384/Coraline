package com.coraline.feast;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Explicit disposable E/F checkpoint. Actual recordings and mixer ACKs, normal
 * seat input, production grab/throw/rescue. Never fabricates a completion ACK. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class TheatreAerialVisualCheck {
    private static boolean requested,ready,seated,zoomed,unzoomed,carried,rescued,stopping;
    private static int ticks,lastSegment=-1;
    private static double zoomFov=Double.NaN,grabFov=Double.NaN;
    private static volatile String capture;
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("coraline.theatreAerialCheck")||e.phase!=TickEvent.Phase.END||stopping)return;
        var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;
        if(mc.player==null||mc.getSingleplayerServer()==null||mc.player.tickCount<60)return;
        if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)mc.setScreen(null);
        if(!requested){requested=true;mc.getSingleplayerServer().execute(()->{try{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
            NauticalOpeningVisualCheck.stage(p);var s=OtherMotherLoop.scene(p);
            TheatreVisit.data(s).putInt("Segment",5); // Explicit post-opening fixture, not story progress.
            ready=true;
        }catch(Throwable error){fail(error);}});}
        try{
            if(seated&&!zoomed&&com.coraline.client.VisitEventClient.showVenue()){
                var available=com.coraline.client.TheatreViewClient.class.getDeclaredField("available");available.setAccessible(true);
                if(available.getBoolean(null)){
                    com.coraline.client.TheatreViewClient.key(new net.minecraftforge.client.event.InputEvent.Key(org.lwjgl.glfw.GLFW.GLFW_KEY_V,0,org.lwjgl.glfw.GLFW.GLFW_PRESS,0));
                    require(zoom(),"Normal V input did not enable zoom");zoomed=true;mc.player.setXRot(-25);
                }
            }
            if(zoomed&&mc.player.getVehicle() instanceof CinematicPropEntity carrier&&carrier.kind()==CinematicPropEntity.STUNT_CARRIER){
                require(!zoom(),"Opera glasses remained active on the grabbed player");unzoomed=true;
            }
            var shot=capture;if(shot!=null){capture=null;var out=java.nio.file.Path.of("../artwork/theatre_aerial_qa");java.nio.file.Files.createDirectories(out);
                try(var image=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(out.resolve(shot+".png"));}}
        }catch(Throwable error){fail(error);}
    }
    private static boolean zoom()throws ReflectiveOperationException{var field=com.coraline.client.TheatreViewClient.class.getDeclaredField("zoom");field.setAccessible(true);return field.getBoolean(null);}
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void fov(net.minecraftforge.client.event.ViewportEvent.ComputeFov event)throws ReflectiveOperationException{
        if(!Boolean.getBoolean("coraline.theatreAerialCheck")||!ready||stopping)return;
        var p=Minecraft.getInstance().player;if(p==null)return;
        if(p.getVehicle() instanceof CinematicPropEntity seat&&seat.kind()==CinematicPropEntity.STUNT_CARRIER)grabFov=event.getFOV();
        // Preserve the actual pre-grab zoom sample. After rescue the normal
        // unzoomed camera must not overwrite that earlier observation.
        else if(zoomed&&!unzoomed&&zoom())zoomFov=event.getFOV();
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void observe(TickEvent.PlayerTickEvent event){
        if(!Boolean.getBoolean("coraline.theatreAerialCheck")||!ready||stopping||event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer p))return;
        try{
            ticks++;var s=OtherMotherLoop.scene(p);require(s!=null,"Lost staged scene");var n=TheatreVisit.data(s);
            if(!seated){var seat=TheatreSite.ensure(p,s,p.serverLevel(),CinematicPropEntity.SEAT,TheatreSite.playerSeat(p,s),0);
                var ownerSeat=TheatreSite.playerSeat(p,s);var companionSeat=TheatreSite.companionSeat(p,s);
                require(ownerSeat.getY()==companionSeat.getY()&&ownerSeat.getZ()==companionSeat.getZ()&&ownerSeat.distSqr(companionSeat)==4,"Clear-view aisle seats are not adjacent");
                for(int i=0;i<TheatreSite.ROWS*TheatreSite.SEATS_PER_ROW;i++)require(!ownerSeat.equals(TheatreSite.seat(p,s,i))&&!companionSeat.equals(TheatreSite.seat(p,s,i)),"Aisle seat overlaps a guest row");
                for(var at:java.util.List.of(ownerSeat,companionSeat))require(p.level().getBlockState(at.below()).isFaceSturdy(p.level(),at.below(),net.minecraft.core.Direction.UP),"Aisle seat has no physical support");
                require(TheatreVisit.interact(p,s,seat)&&p.getVehicle()==seat,"Normal seat interaction failed");seated=true;
            }
            int segment=n.getInt("Segment");if(segment!=lastSegment){lastSegment=segment;System.out.println("THEATRE_AERIAL_PROGRESS segment="+segment+" cursor="+ShowPlayback.position(s));capture=segment==5?"seats_and_approach":"grab";}
            require(n.getString("Blocker").isEmpty(),"Production playback blocker: "+n.getString("Blocker"));
            if(ShowPlayerSafety.active(p)){carried=true;if(ShowPlayback.position(s)==100)capture="carrying";if(ShowPlayback.position(s)==500)capture="fall";}
            rescued|=n.getBoolean("Rescued");
            if(n.getBoolean("PerformanceDone")){
                require(carried&&rescued&&zoomed&&unzoomed,"Missing actual carry, rescue, zoom or grab-disable evidence");
                double base=Minecraft.getInstance().options.fov().get();require(Double.isFinite(zoomFov)&&zoomFov<base*.25,"Higher zoom was not rendered: zoom="+zoomFov+" base="+base+" grab="+grabFov);
                require(Double.isFinite(grabFov)&&grabFov>base*.6,"Stunt camera retained zoom");
                require(!ShowPlayerSafety.active(p)&&!s.loop.event.getBoolean("ShowStunt"),"Finished stunt retained protection");
                System.out.println("THEATRE_AERIAL_NATIVE_COMPLETE actual E/F mixer, supported clear-view aisle seats, zoom FOV="+zoomFov+", grab FOV="+grabFov+", real carry/throw/rescue and cleanup; checkpoint only");
                stopping=true;Minecraft.getInstance().execute(Minecraft.getInstance()::stop);return;
            }
            require(ticks<9000,"Aerial checkpoint timed out");
        }catch(Throwable error){fail(error);}
    }
    private static void require(boolean condition,String reason){if(!condition)throw new IllegalStateException(reason);}
    private static void fail(Throwable error){if(stopping)return;stopping=true;com.mojang.logging.LogUtils.getLogger().error("THEATRE_AERIAL_NATIVE_FAILED",error);Minecraft.getInstance().execute(Minecraft.getInstance()::stop);}
}
