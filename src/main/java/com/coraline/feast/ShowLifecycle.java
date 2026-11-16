package com.coraline.feast;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Suspension only: the existing visit director owns reconnection/reconciliation and resumption. */
@Mod.EventBusSubscriber(modid="coraline")
public final class ShowLifecycle {
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p)suspend(p);}
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void death(LivingDeathEvent event){if(event.getEntity() instanceof ServerPlayer p)suspend(p);}
    /** A depart/fail caller may continue leaving, but must preserve event ownership until this succeeds. */
    public static void deferCleanup(ServerPlayer p,VisitData.Scene s){if(!s.owner.equals(p.getUUID()))return;s.loop.event.putBoolean("ShowCleanupDeferred",true);suspend(p);OtherMotherLoop.changed(p);}
    @SubscribeEvent public static void rollback(TickEvent.PlayerTickEvent event){if(event.player instanceof net.minecraft.server.level.ServerPlayer coopPlayer&&com.coraline.multiplayer.StoryCoop.directorBlocked(coopPlayer))return;
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer p)||!p.isAlive()||p.tickCount%20!=0)return;var s=OtherMotherLoop.scene(p);if(s==null||!s.owner.equals(p.getUUID())||!s.loop.event.getBoolean("ShowCleanupDeferred"))return;
        boolean theatre=TheatreVisit.pending(s),circus=MouseCircusVisit.pending(s);if(theatre)ShowStageLease.hold(p,s,false);if(circus)ShowStageLease.hold(p,s,true);
        boolean party=VisitEvents.type(s)==VisitEvents.Type.PLAYER_PARTY&&s.loop.event.getBoolean("PartyPending")&&HouseParty.data(s).getBoolean("TheatrePrepared");if(party)ShowStageLease.hold(p,s,false);
        boolean ready=VisitEvents.cleanup(p,s);if(ready){s.loop.event.remove("ShowCleanupDeferred");s.loop.event.remove("ShowTravelGuests");OtherMotherLoop.changed(p);}
    }
    private static void suspend(ServerPlayer p){var s=OtherMotherLoop.scene(p);if(s==null||!s.owner.equals(p.getUUID()))return;if(TheatreVisit.owned(p,s))TheatreVisit.suspend(p,s);if(MouseCircusVisit.owned(p,s))MouseCircusVisit.suspend(p,s);if(VisitEvents.type(s)==VisitEvents.Type.PLAYER_PARTY&&s.loop.event.getBoolean("PartyPending")){PlayerParty.suspend(p,s);if(HouseParty.data(s).getBoolean("TheatrePrepared"))ShowStageLease.release(p,s,false);}}
    private ShowLifecycle(){}
}
