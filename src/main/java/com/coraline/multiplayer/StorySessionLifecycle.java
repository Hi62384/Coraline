package com.coraline.multiplayer;

import com.coraline.feast.VisitData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One observed owner director. Entrance/scene adapters opt guests in only after
 * their transport and restoration transaction has been prepared. */
@Mod.EventBusSubscriber(modid="coraline")
public final class StorySessionLifecycle {
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        var server=event.getServer();if(com.coraline.postgame.TrueHeartBattle.resetting(server))return;
        var sessions=StorySessionData.get(server);
        for(var p:server.getPlayerList().getPlayers()){
            if(sessions.guest(p.getUUID()))continue;
            if(server.getTickCount()%20!=0&&!sessions.hasGuests(p.getUUID()))continue;
            var scene=VisitData.get(server).scenes.get(p.getUUID());
            if(scene!=null&&!com.coraline.postgame.TrueHeartBattle.recoveryPending(p)){sessions.observe(p,scene);if(com.coraline.feast.AcceptedButtonEyes.active(scene)){if(server.getTickCount()%20==0)GroupInventoryEscrow.capture(p,scene);sessions.authorizeInventoryReturn(server,scene);}}
        }
        StoryCoop.tick(server);
        if(server.getTickCount()%20==0)for(var p:server.getPlayerList().getPlayers())StoryRewardDelivery.tick(p);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){
        if(event.getEntity() instanceof ServerPlayer p)StorySessionData.get(p.server).pause(p.server,p.getUUID());
    }
    private StorySessionLifecycle(){}
}
