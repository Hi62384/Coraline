package com.coraline.feast;

import java.util.Comparator;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

/** Scoped region tickets keep only the active owner venue loaded; no global forced-chunk edits. */
public final class ShowStageLease {
    private static final TicketType<UUID> TICKET=TicketType.create("coraline_performance",Comparator.comparing(UUID::toString));
    public static void hold(ServerPlayer p,VisitData.Scene s,boolean circus){var level=p.server.getLevel(ActivityWorld.KEY);if(level==null)return;var c=ShowSiteData.get(p.server).center(s.owner,circus);level.getChunkSource().addRegionTicket(TICKET,new ChunkPos(c),5,s.owner);}
    public static void release(ServerPlayer p,VisitData.Scene s,boolean circus){var level=p.server.getLevel(ActivityWorld.KEY);if(level==null)return;var c=ShowSiteData.get(p.server).center(s.owner,circus);level.getChunkSource().removeRegionTicket(TICKET,new ChunkPos(c),5,s.owner);}
    private ShowStageLease(){}
}
