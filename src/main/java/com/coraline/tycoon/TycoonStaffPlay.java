package com.coraline.tycoon;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

/** A reusable real ball, not a timer that silently completes play requests. */
public final class TycoonStaffPlay {
    public static void hold(ServerPlayer p,CompoundTag w,TycoonWorker worker){if(!w.getBoolean("BallHeld")||w.hasUUID("PlayingChild"))return;var ball=ball(p,w);if(ball==null)return;ball.setPickUpDelay(32767);ball.setInvisible(true);ball.setNoGravity(true);ball.setDeltaMovement(Vec3.ZERO);ball.setPos(worker.position().add(0,1,0));w.putLong("PlayBallAt",ball.blockPosition().asLong());if(!w.contains("Carry"))worker.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ball.getItem());}
    private static ItemEntity ball(ServerPlayer p,CompoundTag w){
        if(!w.hasUUID("PlayBall"))return null;var l=p.server.overworld();if(w.contains("PlayBallAt"))l.getChunkAt(BlockPos.of(w.getLong("PlayBallAt")));return l.getEntity(w.getUUID("PlayBall")) instanceof ItemEntity item?item:null;
    }
    public static boolean step(ServerPlayer p,CompoundTag w,TycoonWorker worker,TycoonData.Resident r,TycoonNpc child){
        var h=TycoonWorld.home(p);if(child==null||child.level()!=worker.level()||!r.n.getString("Request").equals("CATCH")){cancel(p,w,r);return false;}
        ItemEntity ball=ball(p,w);
        if(ball==null){
            if(w.hasUUID("PlayBall")&&!w.getBoolean("BallSpawnPending")){w.putInt("BallMissing",w.getInt("BallMissing")+100);if(w.getInt("BallMissing")<1200)return true;w.remove("PlayBall");cancel(p,w,r);return false;}
            if(!w.hasUUID("PlayBall")){
                if(!TycoonWarehouse.take(h,TycoonContent.BALL.get(),1)){w.putString("Blocker","Supply one real Play Ball for this maid; it will be reused.");return true;}
                w.putUUID("PlayBall",UUID.randomUUID());w.putBoolean("BallSpawnPending",true);w.putLong("PlayBallAt",worker.blockPosition().asLong());TycoonWorld.dirty(p);try{if(!TycoonCheckpoints.home(p))return true;}catch(java.io.IOException failed){return true;}
            }
            ball=new ItemEntity(p.server.overworld(),worker.getX(),worker.getY()+1,worker.getZ(),new ItemStack(TycoonContent.BALL.get()));ball.setUUID(w.getUUID("PlayBall"));ball.setUnlimitedLifetime();ball.setInvulnerable(true);ball.getPersistentData().putUUID("TycoonOwner",h.owner);ball.getPersistentData().putUUID("TycoonWorkerBall",w.getUUID("Id"));if(!p.server.overworld().addFreshEntity(ball))return true;p.server.saveEverything(true,true,true);w.putBoolean("BallSpawnPending",false);w.putBoolean("BallHeld",true);
        }
        ball.setPickUpDelay(32767);w.putLong("PlayBallAt",ball.blockPosition().asLong());w.putUUID("PlayingChild",r.id);var play=r.n.getCompound("StaffPlay");
        if(play.isEmpty()){play.putUUID("Worker",w.getUUID("Id"));play.putUUID("WorkerActor",worker.getUUID());play.putString("Phase","WORKER_HELD");play.putInt("Passes",0);r.n.put("StaffPlay",play);r.n.putBoolean("Accepted",true);r.n.putLong("RequestExpires",h.clock()+2400);r.n.putLong("CatchStart",h.clock());}
        // ItemEntity merging compares the stack, not entity persistent data.
        // Keep each paid-for reusable ball distinct from every other worker's.
        ball.getItem().getOrCreateTag().putUUID("TycoonStaffBall",w.getUUID("Id"));
        if(!play.getUUID("Worker").equals(w.getUUID("Id")))return false;
        w.putString("Task","PLAYING_CATCH_WITH_REAL_BALL");
        if(play.getString("Phase").equals("TO_WORKER")){
            var goal=TycoonWorld.landing(p.server.overworld(),ball.blockPosition());if(worker.distanceToSqr(ball)>2.5){TycoonWorkers.approach(p,w,worker,goal);return true;}play.putString("Phase","WORKER_HELD");w.putBoolean("BallHeld",true);
        }
        if(play.getString("Phase").equals("WORKER_HELD")){
            ball.setNoGravity(true);ball.setInvisible(true);ball.setDeltaMovement(Vec3.ZERO);ball.setPos(worker.position().add(0,1,0));worker.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ball.getItem());
            if(play.getInt("Passes")>=6){r.n.putInt("Passes",3);TycoonCare.staffComplete(p,r);cancel(p,w,r);w.putLong("CompletedJobs",w.getLong("CompletedJobs")+1);return true;}
            if(worker.distanceToSqr(child)>36){TycoonWorkers.approach(p,w,worker,TycoonWorld.landing(p.server.overworld(),child.blockPosition()));return true;}
            throwTo(ball,worker.position().add(0,1,0),child.position().add(0,.8,0));w.putBoolean("BallHeld",false);play.putString("Phase","TO_CHILD");play.putLong("ThrownAt",h.clock());
        }TycoonWorld.dirty(p);return true;
    }
    public static boolean childTick(ServerPlayer p,TycoonData.Resident r,TycoonNpc child){
        var play=r.n.getCompound("StaffPlay");if(play.isEmpty()||!play.hasUUID("Worker"))return false;var h=TycoonWorld.home(p);var w=TycoonWorkers.workers(h).getCompound(play.getUUID("Worker").toString());var ball=ball(p,w);var worker=w.hasUUID("Actor")&&child.level() instanceof ServerLevel level?level.getEntity(w.getUUID("Actor")):null;
        if(w.isEmpty()||ball==null||!(worker instanceof TycoonWorker)||r.need("Hunger")<25||r.need("Thirst")<25||!r.n.getString("Request").equals("CATCH")){cancel(p,w,r);return false;}
        if(w.getBoolean("Break")||!w.getBoolean("Paid")){child.getNavigation().stop();return true;}
        ball.setPickUpDelay(32767);w.putLong("PlayBallAt",ball.blockPosition().asLong());r.n.putString("Task","PLAYING_CATCH_WITH_MAID");r.n.putLong("RequestExpires",h.clock()+1200);
        switch(play.getString("Phase")){
            case "TO_CHILD"->{child.getNavigation().moveTo(ball,.9);if(child.distanceToSqr(ball)<2.5){play.putString("Phase","CHILD_HELD");play.putLong("CaughtAt",h.clock());play.putInt("Passes",play.getInt("Passes")+1);}}
            case "CHILD_HELD"->{child.getNavigation().stop();child.gesture(8,25);child.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ball.getItem());ball.setNoGravity(true);ball.setInvisible(true);ball.setDeltaMovement(Vec3.ZERO);ball.setPos(child.position().add(0,1,0));if(h.clock()-play.getLong("CaughtAt")>=40){throwTo(ball,child.position().add(0,1,0),worker.position().add(0,.8,0));child.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);play.putString("Phase","TO_WORKER");play.putInt("Passes",play.getInt("Passes")+1);}}
            default->child.getNavigation().stop();
        }
        if(h.clock()-play.getLong("ThrownAt")>1200&&!play.getString("Phase").equals("CHILD_HELD")){cancel(p,w,r);return false;}TycoonWorld.dirty(p);return true;
    }
    private static void throwTo(ItemEntity ball,Vec3 from,Vec3 to){ball.setPos(from);ball.setNoGravity(false);ball.setInvisible(false);var delta=to.subtract(from);ball.setDeltaMovement(delta.scale(.12).add(0,.18,0));}
    private static void cancel(ServerPlayer p,CompoundTag w,TycoonData.Resident r){
        if(w!=null&&!w.isEmpty()){w.remove("PlayingChild");w.putBoolean("BallHeld",true);var ball=ball(p,w);if(ball!=null){ball.setNoGravity(true);ball.setInvisible(true);ball.setDeltaMovement(Vec3.ZERO);if(w.hasUUID("Actor")&&p.server.overworld().getEntity(w.getUUID("Actor")) instanceof TycoonWorker worker)ball.setPos(worker.position().add(0,1,0));}}
        r.n.remove("StaffPlay");TycoonWorld.dirty(p);
    }
    private TycoonStaffPlay(){}
}
