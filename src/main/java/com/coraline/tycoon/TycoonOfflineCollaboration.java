package com.coraline.tycoon;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One real authorized representative may operate an offline owner's household.
 * Native clocks and production are guarded per household, never per guest. */
@Mod.EventBusSubscriber(modid="coraline")
public final class TycoonOfflineCollaboration {
    public static ServerPlayer representative(MinecraftServer server,java.util.UUID owner){
        var direct=server.getPlayerList().getPlayer(owner);if(direct!=null)return direct;
        var h=TycoonData.get(server).homes.get(owner);if(h==null||!h.active())return null;
        return server.getPlayerList().getPlayers().stream().filter(p->p.isAlive()&&!p.isSpectator()&&TycoonCollaborators.allowed(p,owner))
            .filter(p->p.serverLevel().dimension()==TycoonWorld.KEY&&Math.abs(p.getX()-h.center().getX())<=128&&Math.abs(p.getZ()-h.center().getZ())<=128||p.level()==server.overworld()&&h.entrance()!=null&&p.distanceToSqr(h.entrance().getCenter())<=128*128||TycoonNetwork.managing(p,owner))
            .min(java.util.Comparator.comparing(ServerPlayer::getUUID)).orElse(null);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        var server=event.getServer();if(event.phase!=TickEvent.Phase.END||server.getTickCount()%20!=0||com.coraline.postgame.TrueHeartBattle.resetting(server))return;
        for(var h:TycoonData.get(server).homes.values()){
            if(!h.active()||server.getPlayerList().getPlayer(h.owner)!=null)continue;
            var p=representative(server,h.owner);if(p==null)continue;
            TycoonScope.run(p,h.owner,()->{TycoonRuntime.tickHome(p,h);TycoonPhaseTwo.tickHome(p,h);});
        }
    }
    private TycoonOfflineCollaboration(){}
}
