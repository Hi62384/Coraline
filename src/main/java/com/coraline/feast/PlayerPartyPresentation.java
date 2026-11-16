package com.coraline.feast;

import com.coraline.otherworld.RealmData;
import com.coraline.secrets.HouseCabinets;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.*;

/** Actual Player Party set dressing, safe gathering selection and compact attic audience show. */
public final class PlayerPartyPresentation implements PlayerParty.Presentation {
    @Override public List<PlayerParty.Area> grounds(ServerPlayer p,VisitData.Scene s){try{return PartyGarden.grounds(p,s);}catch(IllegalStateException unavailable){HouseParty.data(s).putString("PresentationBlocker",unavailable.getMessage());return List.of();}}
    @Override public int capacity(ServerPlayer p,VisitData.Scene s){return Math.min(24,Math.max(8,PartyRegions.capacity(p)+4));}
    @Override public boolean eligible(ServerPlayer p,VisitData.Scene s,String kind){return switch(kind){case "other_bobinsky"->s.loop.story.getBoolean("BobinskyIntroductionComplete");case "other_miss_spink","other_miss_forcible"->s.loop.story.getBoolean("TeaIntroductionComplete");case "other_wybie"->WybieOptionalVisits.allowed(s);default->true;};}
    @Override public boolean storageSecured(ServerPlayer p,VisitData.Scene s){return PartyStorageGuard.prepare(p,s);}
    @Override public boolean prepare(ServerPlayer p,VisitData.Scene s){
        var n=HouseParty.data(s);if(!n.contains("Celebration")){var chosen=chooseCelebration(p,s);if(chosen==null){n.putString("PresentationBlocker","No safe actual garden or main-floor gathering has enough crowd space");return false;}n.putLong("Celebration",chosen.asLong());}
        var center=celebration(p,s);if(!PartyDecorations.prepare(p,s,true))return false;
        PartyDecorations.garden(p,s,center);if(!PartyDecorations.refreshments(p,s,center)){n.putString("PresentationBlocker","No safe refreshment station fits in an actual public party area");return false;}
        var pedestal=center.add(0,0,2.5);if(!p.serverLevel().noCollision(new AABB(pedestal.x-1.8,pedestal.y,pedestal.z-1.8,pedestal.x+1.8,pedestal.y+.84,pedestal.z+1.8))){n.putString("PresentationBlocker","Celebration pedestal is obstructed");return false;}
        PartyDecorations.furniture(p,s,ScenePropEntity.TABLE,pedestal,5480);PartyDecorations.decorateTables(p,s);n.remove("PresentationBlocker");return true;
    }
    private static BlockPos chooseCelebration(ServerPlayer p,VisitData.Scene s){
        var realm=RealmData.get(p.server).realms.get(s.door);if(realm==null||realm.house==null)return null;var candidates=new ArrayList<BlockPos>();
        // Authored garden cells are actual feet-height terrain, not an invented house radius.
        var garden=new ArrayList<>(PartyGarden.layout());Collections.shuffle(garden,new Random(HouseParty.data(s).getLong("Seed")));
        for(int i=0;i<garden.size()&&candidates.size()<180;i+=Math.max(1,garden.size()/180))candidates.add(PartyGarden.feet(realm.house,garden.get(i)));
        for(var room:PartyRegions.rooms())if(room.floor()<10&&!room.id().contains("stair")&&!room.id().contains("hall"))candidates.add(room.center(realm.house));
        int best=0;BlockPos selected=null;var areas=PartyGarden.grounds(p,s);
        for(var at:candidates){var center=Vec3.atBottomCenterOf(at);if(!safe(p,at)||!p.serverLevel().noCollision(new AABB(at.offset(-2,0,-2),at.offset(3,3,3))))continue;int fits=0;
            for(int i=0;i<24;i++){var point=crowd(center,i);var feet=BlockPos.containing(point);if(!inside(realm.house,areas,point)||!safe(p,feet))break;fits++;}
            if(fits>best&&fits>=8){best=fits;selected=at;if(best>=HouseParty.data(s).getInt("Capacity"))break;}
        }
        if(selected!=null)HouseParty.data(s).putInt("Capacity",Math.min(HouseParty.data(s).getInt("Capacity"),best));return selected;
    }
    private static boolean inside(BlockPos house,List<PlayerParty.Area> areas,Vec3 at){return PartyRegions.rooms().stream().anyMatch(r->r.floor()<10&&r.bounds(house).contains(at))||areas.stream().anyMatch(a->a.bounds().contains(at));}
    static Vec3 crowd(Vec3 center,int index){var offset=PartyLayoutRules.crowd(index);return center.add(offset.x(),0,offset.z());}
    private static boolean safe(ServerPlayer p,BlockPos at){return p.serverLevel().hasChunkAt(at)&&OutingSites.feet(p.serverLevel(),at)&&p.serverLevel().noCollision(new AABB(at).inflate(.35,0,.35).expandTowards(0,1.6,0));}
    @Override public Vec3 celebration(ServerPlayer p,VisitData.Scene s){return Vec3.atBottomCenterOf(BlockPos.of(HouseParty.data(s).getLong("Celebration")));}
    @Override public Vec3 cakePosition(ServerPlayer p,VisitData.Scene s){return celebration(p,s).add(0,.9,2.5);}
    private static BlockPos stage(ServerPlayer p,VisitData.Scene s){var n=HouseParty.data(s);if(n.contains("CircusStage"))return BlockPos.of(n.getLong("CircusStage"));var realm=RealmData.get(p.server).realms.get(s.door);return realm==null||realm.house==null?null:HouseCabinets.at(realm.house,32,24,40);}
    @Override public boolean circusReady(ServerPlayer p,VisitData.Scene s){
        var n=HouseParty.data(s);var at=stage(p,s);if(at==null)return false;
        if(!n.contains("CircusStage")&&!clearStage(p,at)){
            var realm=RealmData.get(p.server).realms.get(s.door);var room=MouseCircusSite.attic();at=null;
            for(int x=room.min()[0]+5;x<=room.max()[0]-5&&at==null;x+=2)for(int z=room.min()[2]+5;z<=room.max()[2]-8&&at==null;z+=2){var point=HouseCabinets.at(realm.house,x,room.min()[1],z);if(clearStage(p,point))at=point;}
        }
        if(at==null||!clearStage(p,at)){n.putString("PresentationBlocker","No clear compact attic stage fits without changing the house");return false;}n.putLong("CircusStage",at.asLong());
        PartyDecorations.decorate(p,s,5500,Vec3.atBottomCenterOf(at),PartyDecorations.CIRCUS_STAGE,8);
        for(int i=0;i<4;i++){var seat=Vec3.atBottomCenterOf(at.offset(-3+i*2,0,7));if(safe(p,BlockPos.containing(seat))){var chair=PartyDecorations.furniture(p,s,ScenePropEntity.CHAIR,seat,5510+i);chair.setYRot(180);}}
        return true;
    }
    private static boolean clearStage(ServerPlayer p,BlockPos at){for(var pos:BlockPos.betweenClosed(at.offset(-4,0,-4),at.offset(4,5,4)))if(!p.serverLevel().hasChunkAt(pos)||!p.serverLevel().getBlockState(pos).isAir())return false;return OutingSites.feet(p.serverLevel(),at);}
    @Override public void tickCircus(ServerPlayer p,VisitData.Scene s,Mob bob){
        var n=HouseParty.data(s);boolean before=BobinskyAudienceShow.playing(s);BobinskyAudienceShow.tick(p,s);
        if(before&&!BobinskyAudienceShow.playing(s)){for(var g:PartyGuests.guests(s))if(g.getBoolean("CircusAudience")||g.getString("Kind").equals("other_bobinsky")&&g.getString("State").equals("PERFORMING")){var actor=PartyGuests.entity(p,g);if(actor!=null){actor.stopRiding();PartyGuests.gesture(actor,7,50);}g.putString("State","WANDERING");g.remove("CircusAudience");}n.putLong("NextCircus",n.getLong("Clock")+1800);}
        if(BobinskyAudienceShow.playing(s)){seatCircus(p,s);return;}
        if(bob==null||bob.level()!=p.level()||n.getBoolean("FeastDue")||n.getBoolean("Suspended")||s.loop.secrets.searching()||PlayerPartyLedger.phase(n)!=PlayerPartyLedger.Phase.SOCIAL||!MouseCircusSite.inAttic(p,s)||n.getLong("Clock")<n.getLong("NextCircus"))return;
        var guest=PartyGuests.guests(s).stream().filter(g->g.getString("Kind").equals("other_bobinsky")).findFirst().orElse(null);if(guest==null||!Set.of("WANDERING","CONVERSING","APPROACHING_CIRCUS").contains(guest.getString("State")))return;
        var stage=stage(p,s);if(stage==null)return;var mark=Vec3.atBottomCenterOf(stage.offset(4,0,3));guest.putString("State","APPROACHING_CIRCUS");guest.putLong("CircusTarget",BlockPos.containing(mark).asLong());
        if(bob.distanceToSqr(mark)>3){if(bob.tickCount%40==0)bob.getNavigation().moveTo(mark.x,mark.y,mark.z,.9);return;}
        if(!BobinskyAudienceShow.start(p,s,bob)){n.putString("PresentationBlocker",BobinskyAudienceShow.blocker(s));n.putLong("NextCircus",n.getLong("Clock")+100);return;}
        guest.putString("State","PERFORMING");int index=0;for(var g:PartyGuests.guests(s)){var actor=PartyGuests.entity(p,g);if(index==4)break;if(actor==null||actor==bob||actor.level()!=p.level()||actor.distanceToSqr(mark)>400||!Set.of("WANDERING","CONVERSING").contains(g.getString("State")))continue;g.putBoolean("CircusAudience",true);g.putInt("CircusSeat",index++);g.putString("State","AUDIENCE");}
    }
    private static void seatCircus(ServerPlayer p,VisitData.Scene s){if(!MouseCircusSite.inAttic(p,s))return;for(var g:PartyGuests.guests(s))if(g.getBoolean("CircusAudience")){var actor=PartyGuests.entity(p,g);if(actor==null||actor.level()!=p.level())continue;var chair=VisitEvents.props(p,s).stream().filter(e->e.slot==5510+g.getInt("CircusSeat")&&e.kind()==ScenePropEntity.CHAIR).findFirst().orElse(null);if(chair==null)continue;if(actor.getVehicle()==chair)continue;if(actor.distanceToSqr(chair)<4)actor.startRiding(chair,true);else if(actor.tickCount%40==0)actor.getNavigation().moveTo(chair.getX(),chair.getY(),chair.getZ(),.85);}}
    @Override public boolean circusPlaying(VisitData.Scene s){return BobinskyAudienceShow.playing(s);}
    @Override public void finishCircus(VisitData.Scene s){BobinskyAudienceShow.requestFinish(s);}
    @Override public void cakeChanged(ServerPlayer p,VisitData.Scene s,ScenePropEntity cake,int slices,boolean candlesOut){var data=new CompoundTag();data.putBoolean("PlayerPartyCake",true);data.putInt("Slices",PartyLayoutRules.cakeSlices(slices));data.putBoolean("CandlesOut",candlesOut);cake.cakeData(data);}
    @Override public void cleanup(ServerPlayer p,VisitData.Scene s){PartyDecorations.cleanup(p,s);PartyStorageGuard.release();}
}
