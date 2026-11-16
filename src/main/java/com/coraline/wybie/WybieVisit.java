package com.coraline.wybie;

import com.coraline.feast.VisitData;
import com.coraline.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The visit claim belongs to the first-visit scene, and disappears with the family reset. */
@Mod.EventBusSubscriber(modid="coraline")
public final class WybieVisit {
    public static boolean eligible(VisitData.Scene scene, boolean overworld, boolean day, boolean nearBed) {
        return scene!=null && scene.stage==VisitData.Stage.DONE && !scene.wybieVisited && overworld && day && nearBed;
    }
    public static boolean spawn(ServerPlayer p,VisitData.Scene scene) {
        if(scene.wybieVisited||scene.wybieActor!=null||com.coraline.bobinsky.BobinskyVisit.pending(p))return false;
        var actor=ModEntities.WYBIE_VISITOR.get().create(p.serverLevel());if(actor==null)return false;
        // Search a ring around the player; never place him inside a wall or in unloaded terrain.
        double start=Math.atan2(p.getLookAngle().z,p.getLookAngle().x)+Math.PI;
        for(int attempt=0;attempt<64;attempt++) {
            double angle=start+(attempt%16)*Math.PI/8;int radius=new int[]{3,5,7,10}[attempt/16];
            BlockPos base=BlockPos.containing(p.getX()+Math.cos(angle)*radius,p.getY(),p.getZ()+Math.sin(angle)*radius);
            for(int dy=3;dy>=-4;dy--) {
                BlockPos at=base.offset(0,dy,0);var level=p.serverLevel();
                if(!level.hasChunkAt(at)||!level.getWorldBorder().isWithinBounds(at))continue;
                actor.setPos(Vec3.atBottomCenterOf(at));
                if(!level.getBlockState(at.below()).isFaceSturdy(level,at.below(),net.minecraft.core.Direction.UP)
                    || !level.noCollision(actor,actor.getBoundingBox().inflate(.1,0,.1)) || level.containsAnyLiquid(actor.getBoundingBox()) || !actor.hasLineOfSight(p))continue;
                actor.bind(p,scene.door);actor.setOnGround(true);
                if(!level.addFreshEntity(actor))return false;
                scene.wybieVisited=true;scene.wybieActor=actor.getUUID();VisitData.get(p.server).setDirty();
                actor.startMusic(p);p.sendSystemMessage(net.minecraft.network.chat.Component.literal("Wybie is nearby. Look at him to talk."));return true;
            }
        }
        return false;
    }
}
