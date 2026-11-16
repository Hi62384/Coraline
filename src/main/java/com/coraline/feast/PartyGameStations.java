package com.coraline.feast;

import com.coraline.secrets.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.*;
import java.util.*;

/** Visible, persistent social-phase game furniture, prepared before invitations. */
public final class PartyGameStations {
    public static boolean prepare(ServerPlayer p,VisitData.Scene s){
        if(PlayerParty.isPlayerParty(s))return true;var realm=SecretWorld.realm(p);if(realm==null||realm.house==null)return false;
        var existing=tables(p,s);if(!existing.isEmpty())return true;int station=0;
        for(String roomId:List.of("front_west_room","dads_study","front_turret_room")){
            var room=PartyRegions.rooms().stream().filter(r->r.id().equals(roomId)).findFirst().orElse(null);if(room==null)continue;
            boolean built=false;
            for(int x=room.min()[0]+3;x<=room.max()[0]-3&&!built;x+=2)for(int z=room.min()[2]+3;z<=room.max()[2]-3&&!built;z+=2){
                var block=HouseCabinets.at(realm.house,x,room.floor()+1,z);var center=PartyFloor.rectangle(p,block,3,3);if(center==null)continue;
                var bounds=new AABB(center,center).inflate(3,1.1,3);if(!p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,bounds,e->e.kind()!=ScenePropEntity.OUTING_SCENERY).isEmpty())continue;
                int base=5600+station*10;var table=PartyDecorations.furniture(p,s,ScenePropEntity.TABLE,center,base);table.getPersistentData().putBoolean("PartyGameStation",true);
                for(int seat=0;seat<4;seat++){var chair=PartyDecorations.furniture(p,s,ScenePropEntity.CHAIR,PartyMatches.seatPosition(center,false,seat),base+1+seat);chair.setYRot(PartyMatches.facing(seat));chair.getPersistentData().putBoolean("PartyGameStation",true);}
                built=true;station++;
            }
            if(station>=2)break;
        }
        if(station>0){PartyDecorations.decorateTables(p,s);OtherMotherLoop.changed(p);}return station>0;
    }
    public static List<ScenePropEntity> tables(ServerPlayer p,VisitData.Scene s){return VisitEvents.props(p,s).stream().filter(e->e.kind()==ScenePropEntity.TABLE&&e.getPersistentData().getBoolean("PartyGameStation")&&e.isAlive()).toList();}
    public static Vec3 closest(ServerPlayer p,VisitData.Scene s){
        prepare(p,s);var table=tables(p,s).stream().min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
        if(table!=null)return table.position();return PlayerParty.isPlayerParty(s)&&s.table!=null?Vec3.atBottomCenterOf(s.table):null;
    }
    public static boolean invite(ServerPlayer p,VisitData.Scene s){
        if(tables(p,s).isEmpty())return false;
        for(var g:PartyGuests.guests(s)){var actor=PartyGuests.entity(p,g);if(actor!=null&&actor.level()==p.level()&&actor.distanceToSqr(p)<100&&p.hasLineOfSight(actor)&&Set.of("WANDERING","CONVERSING").contains(g.getString("State"))){VisitAudio.caption(p,"Party games",g.getString("Name")+" would like to play. Right-click them and choose a game.",100);return true;}}
        return false;
    }
    private PartyGameStations(){}
}
