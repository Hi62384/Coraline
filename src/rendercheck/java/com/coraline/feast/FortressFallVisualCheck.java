package com.coraline.feast;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Normal client movement/physics through all five drops, including teleport acknowledgments. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class FortressFallVisualCheck {
    private static volatile VisitData.Scene scene;private static volatile BlockPos center;private static volatile Vec3 target;
    private static boolean started;private static int ticks,cleared;
    static void start(Minecraft mc,VisitData.Scene s){
        scene=s;started=true;
        mc.getSingleplayerServer().execute(()->{
            try{var enter=RevisionVisualCheck.class.getDeclaredMethod("enter",Minecraft.class,OtherMotherActivities.Activity.class);enter.setAccessible(true);enter.invoke(null,mc,OtherMotherActivities.Activity.OTHER_NETHER_FORTRESS);
                center=ExpeditionActivities.center(mc.getSingleplayerServer().getPlayerList().getPlayers().get(0),scene);
            }catch(Throwable ex){com.mojang.logging.LogUtils.getLogger().error("CORALINE_FORTRESS_FALL_CHECK FAILED",ex);mc.execute(mc::stop);}
        });
    }
    @SubscribeEvent public static void movement(MovementInputUpdateEvent e){
        if(!started||target==null)return;var p=e.getEntity();var delta=target.subtract(p.position());var velocity=p.getDeltaMovement();
        p.setYRot(0);p.setXRot(75);
        e.getInput().forwardImpulse=(float)Math.max(-1,Math.min(1,delta.z*.7-velocity.z*3));
        e.getInput().leftImpulse=(float)Math.max(-1,Math.min(1,delta.x*.7-velocity.x*3));
        e.getInput().jumping=false;e.getInput().shiftKeyDown=false;
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(!started||e.phase!=TickEvent.Phase.END||center==null)return;var mc=Minecraft.getInstance();if(mc.player==null)return;
        if(++ticks>5000){com.mojang.logging.LogUtils.getLogger().error("CORALINE_FORTRESS_FALL_CHECK FAILED: timed out");mc.stop();return;}
        var n=scene.loop.expedition;int phase=n.getInt("Phase"),level=n.getInt("FortressLevel");
        if(phase==5){com.mojang.logging.LogUtils.getLogger().error("CORALINE_FORTRESS_FALL_CHECK FAILED: {}",n.getString("Failure"));mc.stop();return;}
        if(level>cleared){cleared=level;com.mojang.logging.LogUtils.getLogger().info("FORTRESS_REAL_FALL_CLEARED {}",level);}
        if(phase==3){com.mojang.logging.LogUtils.getLogger().info("CORALINE_FORTRESS_FALL_CHECK PASSED: five normal-gravity falls and four live teleport handoffs");started=false;if(Boolean.getBoolean("coraline.dropperOnly")){mc.stop();return;}if(Boolean.getBoolean("coraline.dropperVisualCheck"))DropperVisualCheck.lantern(mc,scene);else VisitGiftVisualCheck.resumePreview(mc);return;}
        if(phase==1&&!n.getBoolean("FortressBoarded")){target=Vec3.atBottomCenterOf(center.south(4));return;}
        if(Boolean.getBoolean("coraline.dropperVisualCheck"))DropperVisualCheck.captureLevel(mc,level,phase);
        var gates=FortressActivity.generate(n.getLong("Seed"),level);var gate=gates.get(Math.min(n.getInt("Passed"),gates.size()-1));
        var pool=FortressDropper.pool(n.getLong("Seed"),level);boolean landing=n.getInt("Passed")>=gates.size();target=new Vec3(center.getX()+(landing?pool.x():gate.x())+.5,center.getY(),center.getZ()+(phase==1?-2:landing?pool.z():gate.z())+.5);
    }
}
