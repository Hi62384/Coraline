package com.coraline.wybie;

import com.coraline.bobinsky.*;
import com.coraline.feast.VisitData;
import com.coraline.tea.TeaVisit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One persistent, fair queue for the three daytime neighbour encounters. */
@Mod.EventBusSubscriber(modid="coraline")
public final class NeighborVisits {
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer p)||p.tickCount%20!=0)return;
        var scene=VisitData.get(p.server).scenes.get(p.getUUID());if(scene!=null)tryVisit(p,scene);
    }
    static void tryVisit(ServerPlayer p,VisitData.Scene scene){
        if(com.coraline.feast.FirstEscapeStory.engaged(scene))return;
        if(scene.stage!=VisitData.Stage.DONE||!p.isAlive()||p.isSpectator()||!p.level().dimension().equals(Level.OVERWORLD)||!p.level().isDay())return;
        var data=BobinskyVisitData.get(p.server);var stories=VisitData.get(p.server);
        if(!scene.neighborVisitsReady){
            var bed=p.getRespawnPosition();boolean near=bed!=null&&p.getRespawnDimension().equals(Level.OVERWORLD)&&p.distanceToSqr(Vec3.atCenterOf(bed))<=64&&p.level().getBlockState(bed).getBlock() instanceof BedBlock;
            // Existing saves with an offered/completed encounter already passed
            // the bedside trigger. Continue their remaining invitations outdoors.
            if(!near&&!scene.wybieVisited&&!scene.bobinskyVisited&&!scene.teaVisited&&!data.visits.containsKey(p.getUUID())&&!data.teaVisits.containsKey(p.getUUID()))return;
            scene.neighborVisitsReady=true;stories.setDirty();
        }
        long now=p.server.overworld().getGameTime();
        if(scene.loop.story.getBoolean("NeighborVisitsDebugComplete")||scene.wybieVisited&&scene.bobinskyVisited&&scene.teaVisited)return;
        if(BobinskyVisit.pending(p)||scene.wybieActor!=null||now<scene.nextNeighborAttempt)return;
        scene.nextNeighborAttempt=now+100;
        // Advance even when a placement fails, so one unsuitable site cannot
        // monopolize every attempt. Active encounters still remain exclusive.
        for(int attempt=0;attempt<3;attempt++){
            int kind=Math.floorMod(scene.nextNeighborVisit,3);scene.nextNeighborVisit=(kind+1)%3;
            if(kind==0){if(scene.wybieVisited)continue;WybieVisit.spawn(p,scene);break;}
            if(kind==1){
                var previous=data.visits.get(p.getUUID());if(scene.bobinskyVisited||previous!=null&&now<previous.getLong("RetryAt"))continue;
                BobinskyVisit.offer(p,scene);break;
            }
            var previous=data.teaVisits.get(p.getUUID());if(scene.teaVisited||previous!=null&&now<previous.getLong("RetryAt"))continue;
            if(!TeaVisit.offer(p,scene)&&!scene.teaSiteHint){scene.teaSiteHint=true;p.sendSystemMessage(net.minecraft.network.chat.Component.literal("Miss Spink and Miss Forcible want to invite you to tea. Step outside into open ground nearby so their house can appear."));}
            break;
        }
        stories.setDirty();
    }
}
