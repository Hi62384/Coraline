package com.coraline.feast;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.Path;
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class DropperVisualCheck {
    private static int ticks,phase,frames;private static boolean initialized;private static double fps,work,maxWork;private static VisitData.Scene scene;private static BlockPos room;private static int originalSlot;
    private static final boolean[] captured=new boolean[5];
    static void captureLevel(Minecraft mc,int level,int phase){if(!captured[level]&&phase==2&&mc.player.getY()<ExpeditionActivities.center(mc.getSingleplayerServer().getPlayerList().getPlayers().get(0),scene).getY()-110){captured[level]=true;capture(mc,"dropper38_level_"+(level+1)+".png");}}
    private static void capture(Minecraft mc,String name){try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../artwork/"+name));}catch(Exception ex){throw new RuntimeException(ex);}}
    static void lantern(Minecraft mc,VisitData.Scene s){scene=s;mc.getSingleplayerServer().execute(()->{try{
        var enter=RevisionVisualCheck.class.getDeclaredMethod("enter",Minecraft.class,OtherMotherActivities.Activity.class);enter.setAccessible(true);enter.invoke(null,mc,OtherMotherActivities.Activity.OTHER_MINESHAFT);
        var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var c=ExpeditionActivities.center(p,s);room=c.offset(new MineMaze.Cell(0,3,2).pos());s.loop.expedition.putLong("NextSpawn",Long.MAX_VALUE);originalSlot=s.loop.expedition.getInt("Slot");p.connection.teleport(room.getX()+.5,room.getY(),room.getZ()+.5,0,20);phase=1;ticks=0;
    }catch(Throwable ex){com.mojang.logging.LogUtils.getLogger().error("DROPPER36_CLIENT_FAILED",ex);mc.execute(mc::stop);}});}
    @SubscribeEvent public static void movement(MovementInputUpdateEvent e){if(!Boolean.getBoolean("coraline.dropperVisualCheck")||phase<1)return;var p=e.getEntity();p.getInventory().selected=(originalSlot+(phase==2?1:0))%9;double t=ticks*.018;var target=Vec3.atBottomCenterOf(room).add(Math.sin(t)*1.7,0,Math.cos(t)*1.7);var d=target.subtract(p.position());var v=p.getDeltaMovement();p.setYRot(0);p.setXRot(20);e.getInput().forwardImpulse=(float)Math.max(-1,Math.min(1,d.z-v.z*4));e.getInput().leftImpulse=(float)Math.max(-1,Math.min(1,d.x-v.x*4));}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(!Boolean.getBoolean("coraline.dropperVisualCheck")||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.player==null||mc.getSingleplayerServer()==null)return;mc.options.pauseOnLostFocus=false;ticks++;
        if(!initialized&&ticks>=40){initialized=true;mc.getSingleplayerServer().execute(()->{try{var setup=RevisionVisualCheck.class.getDeclaredMethod("setup",Minecraft.class);setup.setAccessible(true);setup.invoke(null,mc);var field=RevisionVisualCheck.class.getDeclaredField("s");field.setAccessible(true);scene=(VisitData.Scene)field.get(null);FortressFallVisualCheck.start(mc,scene);}catch(Throwable ex){com.mojang.logging.LogUtils.getLogger().error("DROPPER36_CLIENT_FAILED",ex);mc.execute(mc::stop);}});}
        if(phase<1)return;
        if(ticks>100){frames++;fps+=mc.getFps();double elapsed=com.coraline.client.clothing.ClothingLight.lastWorkNanos/1e6;work+=elapsed;maxWork=Math.max(maxWork,elapsed);}
        if(ticks==220)capture(mc,"lantern36_"+phase+".png");
        if(ticks>=400){com.mojang.logging.LogUtils.getLogger().info("LANTERN36_MEASURE phase={} averageFps={} lightingMeanMs={} lightingMaxMs={}",phase,fps/frames,work/frames,maxWork);if(phase==1){phase=2;ticks=frames=0;fps=work=maxWork=0;}else{com.mojang.logging.LogUtils.getLogger().info("DROPPER36_CLIENT_PASSED");mc.stop();}}
    }
}
