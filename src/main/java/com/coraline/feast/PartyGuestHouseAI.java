package com.coraline.feast;

import com.coraline.otherworld.RealmData;
import com.coraline.secrets.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** House-side social AI while the owner watches their separate theatre. All geometry uses the actual actor level. */
public final class PartyGuestHouseAI {
    public static boolean away(ServerPlayer p,VisitData.Scene s){return PlayerParty.isPlayerParty(s)&&!p.level().dimension().location().toString().equals(HouseParty.data(s).getString("HouseDimension"));}
    private static boolean house(Mob m,ServerPlayer p,VisitData.Scene s){
        var n=HouseParty.data(s);var tag=m.getPersistentData();
        return m.level() instanceof ServerLevel&&away(p,s)&&m.level().dimension().location().toString().equals(n.getString("HouseDimension"))&&s.owner.equals(p.getUUID())&&tag.hasUUID("PartyOwner")&&s.owner.equals(tag.getUUID("PartyOwner"))&&tag.getInt("PartySerial")==s.loop.event.getInt("Serial");
    }
    private static boolean running(ServerPlayer p,VisitData.Scene s){var n=HouseParty.data(s);return PlayerParty.active(p)==s&&n.getBoolean("Activated")&&!n.getBoolean("Suspended")&&!s.loop.secrets.searching()&&!Reconciliation.active(p)&&!SecretDialogue.busy(p)&&!p.isSleeping();}
    public static boolean control(Mob m,ServerPlayer p,VisitData.Scene s,CompoundTag g){
        if(!house(m,p,s))return false;var n=HouseParty.data(s);if(!running(p,s)){m.getNavigation().stop();return true;}
        long now=n.getLong("Clock");String state=g.getString("State");
        if(!PartyGuestHouseRules.ordinary(state)){m.getNavigation().stop();return true;}
        if(state.equals("WARNING")||state.equals("TALKING_TO_PLAYER")){PartyConversations.closeGuest(p,s,g.getUUID("Id"));g.putString("State","WANDERING");g.remove("Target");state="WANDERING";}
        if(state.equals("LEAVING")){leave(m,p,s,g);return true;}
        if(state.equals("ARRIVING")){
            if(allowed(p,s,(ServerLevel)m.level(),m.position())){g.putString("State","WANDERING");g.remove("Target");PartyGuests.gesture(m,1,40);return true;}
            var realm=RealmData.get(p.server).realms.get(s.door);if(realm!=null&&realm.porch!=null&&m.tickCount%40==0)move(m,realm.porch.south(5),.9);return true;
        }
        if(state.equals("CONVERSING")){
            if(now>=g.getLong("GroupUntil")){g.putString("State","WANDERING");g.remove("Group");g.putLong("RestUntil",now+40+p.getRandom().nextInt(101));return true;}
            m.getNavigation().stop();var group=PartyGuests.guests(s).stream().filter(x->x.getString("State").equals("CONVERSING")&&x.hasUUID("Group")&&g.hasUUID("Group")&&x.getUUID("Group").equals(g.getUUID("Group"))).map(x->PartyGuests.entity(p,x)).filter(actor->actor!=null&&actor.level()==m.level()).toList();
            if(!group.isEmpty()){var speaker=group.get(Math.floorMod((int)(now/100),group.size()));m.getLookControl().setLookAt(speaker,20,15);if(speaker==m&&m.tickCount%100==0)PartyGuests.gesture(m,1+p.getRandom().nextInt(3),50);}return true;
        }
        if(state.equals("APPROACHING_GROUP")){
            if(now>g.getLong("GroupUntil")){g.putString("State","WANDERING");g.remove("Target");return true;}
            if(g.contains("Target")&&m.distanceToSqr(Vec3.atBottomCenterOf(BlockPos.of(g.getLong("Target"))))<1.5){g.putString("State","CONVERSING");m.getNavigation().stop();return true;}
        }
        if(now<g.getLong("RestUntil")){m.getNavigation().stop();if(m.tickCount%80==0)m.setYHeadRot(m.getYRot()+p.getRandom().nextInt(91)-45);return true;}
        if(g.contains("Target")){
            var at=BlockPos.of(g.getLong("Target"));if(!allowed(p,s,(ServerLevel)m.level(),Vec3.atBottomCenterOf(at))||m.distanceToSqr(Vec3.atBottomCenterOf(at))<1.8||now-g.getLong("MoveStarted")>600){g.remove("Target");g.putLong("RestUntil",now+80+p.getRandom().nextInt(241));m.getNavigation().stop();return true;}
            if(m.tickCount%40==0)move(m,at,.75);return true;
        }
        if(m.tickCount%40==0)destination(p,s,m,g.getInt("Order")+(int)(now/1800)*5,n.getLong("Seed")+now+g.getInt("Order")*31L).ifPresent(at->{g.putLong("Target",at.asLong());g.putLong("MoveStarted",now);move(m,at,.8);});return true;
    }
    static boolean allowed(ServerPlayer p,VisitData.Scene s,ServerLevel level,Vec3 point){
        var n=HouseParty.data(s);if(!level.dimension().location().toString().equals(n.getString("HouseDimension"))||!n.contains("House"))return false;var at=BlockPos.containing(point);var house=BlockPos.of(n.getLong("House"));
        for(var room:SecretWorld.rooms(s.loop.secrets)){var c=BlockPos.of(room.getLong("Pos"));if(Math.abs(c.getX()-at.getX())<=7&&Math.abs(c.getZ()-at.getZ())<=7&&at.getY()>=c.getY()-1&&at.getY()<c.getY()+10)return false;}
        for(var room:HouseCabinets.ROOMS)if(Set.of("archive","storage").contains(room.id())&&HouseCabinets.contains(house,room,at))return false;
        for(var room:HouseCabinets.ROOMS)if(Set.of("attic","basement").contains(room.id())&&HouseCabinets.contains(house,room,at))return true;
        if(PartyRegions.rooms().stream().anyMatch(room->room.bounds(house).contains(point)))return true;
        return PartyGarden.grounds(p,s).stream().anyMatch(area->area.dimension().equals(n.getString("HouseDimension"))&&area.bounds().contains(point));
    }
    static Optional<BlockPos> destination(ServerPlayer p,VisitData.Scene s,Mob m,int index,long seed){
        if(!house(m,p,s)||!running(p,s))return Optional.empty();var level=(ServerLevel)m.level();var n=HouseParty.data(s);var house=BlockPos.of(n.getLong("House"));var areas=new ArrayList<AABB>();
        for(var room:PartyRegions.rooms())areas.add(room.bounds(house));for(var room:HouseCabinets.ROOMS)if(Set.of("attic","basement").contains(room.id())){var a=HouseCabinets.at(house,room.min()[0],room.min()[1],room.min()[2]);var b=HouseCabinets.at(house,room.max()[0],room.max()[1],room.max()[2]);areas.add(new AABB(a,b.offset(1,1,1)));}
        for(var area:PartyGarden.grounds(p,s))if(area.dimension().equals(n.getString("HouseDimension"))&&area.id().equals("backyard"))areas.add(area.bounds());
        if(areas.isEmpty())return Optional.empty();var area=areas.get(Math.floorMod(index,areas.size()));var random=new Random(seed);
        for(int attempt=0;attempt<48;attempt++){int x=(int)Math.floor(area.minX+random.nextDouble()*(area.maxX-area.minX)),z=(int)Math.floor(area.minZ+random.nextDouble()*(area.maxZ-area.minZ));for(int y=(int)Math.ceil(area.minY);y<area.maxY;y++){
            var at=new BlockPos(x,y,z);var point=Vec3.atBottomCenterOf(at);if(!level.hasChunkAt(at)||!allowed(p,s,level,point)||!OutingSites.feet(level,at)||!level.noCollision(m,m.getDimensions(m.getPose()).makeBoundingBox(point)))continue;
            if(level.getEntities(m,new AABB(point,point).inflate(.9,1,.9)).stream().anyMatch(e->e instanceof LivingEntity))continue;var path=m.getNavigation().createPath(at,0);if(path!=null&&path.canReach())return Optional.of(at.immutable());
        }}return Optional.empty();
    }
    public static void formGroup(ServerPlayer p,VisitData.Scene s){
        if(!away(p,s)||!running(p,s))return;var available=PartyGuests.guests(s).stream().filter(g->{var actor=PartyGuests.entity(p,g);return g.getString("State").equals("WANDERING")&&actor!=null&&house(actor,p,s);}).toList();if(available.size()<2)return;
        var lead=available.get(p.getRandom().nextInt(available.size()));var anchor=PartyGuests.entity(p,lead);var level=(ServerLevel)anchor.level();UUID id=UUID.randomUUID();long now=HouseParty.data(s).getLong("Clock"),until=now+300+p.getRandom().nextInt(901);int count=0,target=2+p.getRandom().nextInt(3);
        for(var g:available){var actor=PartyGuests.entity(p,g);if(actor.level()!=level||actor.distanceToSqr(anchor)>100||count>=target)continue;double angle=.6+count*1.9;var at=BlockPos.containing(anchor.position().add(Math.cos(angle)*1.7,0,Math.sin(angle)*1.7));var point=Vec3.atBottomCenterOf(at);
            if(!allowed(p,s,level,point)||!OutingSites.feet(level,at)||!level.noCollision(actor,actor.getDimensions(actor.getPose()).makeBoundingBox(point)))continue;var path=actor.getNavigation().createPath(at,0);if(path==null||!path.canReach())continue;
            g.putUUID("Group",id);g.putLong("GroupUntil",until);g.putLong("Target",at.asLong());g.putLong("MoveStarted",now);g.putString("State","APPROACHING_GROUP");actor.getNavigation().moveTo(path,.8);count++;
        }
    }
    private static void move(Mob actor,BlockPos at,double speed){if(actor.getNavigation() instanceof GroundPathNavigation nav){nav.setCanOpenDoors(true);nav.setCanPassDoors(true);}var path=actor.getNavigation().createPath(at,0);if(path!=null&&path.canReach())actor.getNavigation().moveTo(path,speed);}
    private static void leave(Mob actor,ServerPlayer p,VisitData.Scene s,CompoundTag g){
        long now=HouseParty.data(s).getLong("Clock");if(now<g.getLong("LeaveAfter")){actor.getNavigation().stop();return;}var realm=RealmData.get(p.server).realms.get(s.door);if(realm==null||realm.porch==null)return;var level=(ServerLevel)actor.level();if(actor.tickCount%40==0)move(actor,realm.porch.north(12),.9);
        boolean outside=!allowed(p,s,level,actor.position())&&actor.distanceToSqr(realm.porch.getCenter())>36,stalled=now-g.getLong("LeaveAfter")>600&&actor.getNavigation().isDone();
        if((outside||stalled)&&!watched(level,actor.position())){g.putString("State","GONE");PartyGuests.release(actor);}
    }
    private static boolean watched(ServerLevel level,Vec3 at){for(var viewer:level.players())if(viewer.distanceToSqr(at)<144&&level.clip(new ClipContext(viewer.getEyePosition(),at.add(0,1,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,viewer)).getType()==HitResult.Type.MISS)return true;return false;}
    private PartyGuestHouseAI(){}
}
