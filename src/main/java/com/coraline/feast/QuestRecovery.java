package com.coraline.feast;

import com.coraline.otherworld.OtherWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Bounded route checks and a separate movement-progress watchdog; never skips story stages. */
@Mod.EventBusSubscriber(modid="coraline")
public final class QuestRecovery {
    public static final String HINT="";
    static final int CHECK_TICKS=100,FAILED_CHECKS=3,STALL_TICKS=600;
    static final class Watch {
        final BlockPos goal;
        Vec3 origin,last;
        long since,nextCheck;
        double travel;
        int failed;
        Watch(BlockPos goal,Vec3 pos,long now){this.goal=goal;origin=last=pos;since=now;nextCheck=now+CHECK_TICKS;}
        void resetMovement(Vec3 pos,long now){origin=last=pos;travel=0;since=now;}
        boolean stalled(Vec3 pos,long now){
            double moved=pos.distanceTo(last);if(moved>.01)travel+=Math.min(moved,1);last=pos;
            // Moving out of this small area is real progress/exploration, not a softlock.
            if(pos.distanceToSqr(origin)>9){resetMovement(pos,now);return false;}
            return now-since>=STALL_TICKS&&travel>=8;
        }
    }
    private static final Map<UUID,Watch> watching=new HashMap<>();
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer p))return;
        var scene=FirstVisit.active(p);if(scene==null)scene=OtherMotherLoop.active(p);
        if(!p.isAlive()||!OtherWorld.isOther(p.level())||p.isPassenger()||p.isSleeping()||p.getAbilities().flying||p.getPersistentData().getBoolean("CoralineInTunnel")||scene==null){watching.remove(p.getUUID());return;}
        // Repeat visits are free exploration. A distant/obstructed invitation is
        // not a softlock (route() deliberately returns null beyond 128 blocks).
        if(scene.loop.active()){watching.remove(p.getUUID());return;}
        var goal=VisitGuide.target(p,scene);
        if(goal==null){watching.remove(p.getUUID());return;}
        if(p.distanceToSqr(Vec3.atBottomCenterOf(goal))<=9){
            var sight=p.serverLevel().clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(),Vec3.atBottomCenterOf(goal).add(0,1.5,0),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
            if(sight.getType()==net.minecraft.world.phys.HitResult.Type.MISS){watching.remove(p.getUUID());return;}
        }
        long now=p.server.overworld().getGameTime();var watch=watching.get(p.getUUID());
        if(watch==null||!goal.equals(watch.goal)){watch=new Watch(goal,p.position(),now);watching.put(p.getUUID(),watch);}
        boolean stalled=watch.stalled(p.position(),now);
        if(now<watch.nextCheck)return;watch.nextCheck=now+CHECK_TICKS;
        // A jump, falling, loading chunk, or flight is not evidence of a blocked walking route.
        if(!p.onGround()||!p.serverLevel().hasChunkAt(goal))return;
        var path=VisitGuide.route(p,goal);
        if(path!=null&&path.canReach())watch.failed=0;else watch.failed++;
        if(watch.failed<FAILED_CHECKS&&!stalled)return;
        if(VisitGuide.relocate(p,goal))watching.remove(p.getUUID());
        else{
            // Do not repeatedly search or ever land inside a wall when the destination itself is blocked.
            watch.nextCheck=now+200;watch.resetMovement(p.position(),now);
        }
    }
    /** Silent story-safe recovery: active objective first, estate porch otherwise. */
    public static boolean recover(ServerPlayer p){
        if(!OtherWorld.isOther(p.level())||!p.getPersistentData().hasUUID("CoralineVisitDoor"))return false;
        var r=com.coraline.otherworld.RealmData.get(p.server).realms.get(p.getPersistentData().getUUID("CoralineVisitDoor"));if(r==null)return false;
        var s=FirstVisit.active(p);if(s==null)s=OtherMotherLoop.active(p);
        BlockPos goal=s==null||s.loop.active()?null:VisitGuide.target(p,s);if(goal==null)goal=r.porch!=null?r.porch:r.doorPos;
        boolean moved=VisitGuide.relocate(p,goal);if(!moved&&r.porch!=null&&!goal.equals(r.porch))moved=VisitGuide.relocate(p,r.porch);
        if(moved)watching.remove(p.getUUID());return moved;
    }
    public static boolean recoverToPorch(ServerPlayer p){
        if(!p.getPersistentData().hasUUID("CoralineVisitDoor"))return false;
        var r=com.coraline.otherworld.RealmData.get(p.server).realms.get(p.getPersistentData().getUUID("CoralineVisitDoor"));if(r==null)return false;
        boolean moved=VisitGuide.relocate(p,r.porch!=null?r.porch:r.doorPos);if(moved)watching.remove(p.getUUID());return moved;
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){watching.remove(event.getEntity().getUUID());}
    @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event){watching.clear();}
}
