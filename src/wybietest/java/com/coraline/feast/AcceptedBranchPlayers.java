package com.coraline.feast;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/** Plain fixture helper: accepted-story tests need not discover unrelated tea tests. */
final class AcceptedBranchPlayers {
    static ServerPlayer fixture(GameTestHelper h){
        var level=h.getLevel().getServer().overworld();
        var p=new ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"AcceptedInventoryQA"));
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.loop.event.putInt("Serial",3);
        s.table=h.absolutePos(BlockPos.ZERO).above(2);s.piano=s.table;p.setPos(s.table.getCenter());VisitData.get(p.server).scenes.put(s.owner,s);return p;
    }
    static void remove(ServerPlayer p,VisitData.Scene s){
        VisitData.get(p.server).scenes.remove(s.owner);
        com.coraline.bobinsky.BobinskyVisitData.get(p.server).teaVisits.remove(s.owner);
        com.coraline.otherworld.DoorData.get(p.server).doors.remove(s.door);
        com.coraline.parents.FamilyData.get(p.server).families.values().removeIf(f->s.owner.equals(f.owner));
        com.coraline.doll.DollBondData.get(p.server).bonds.remove(s.owner);
    }
    private AcceptedBranchPlayers(){}
}
