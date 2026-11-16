package com.coraline.tycoon;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import com.coraline.otherworld.MouseEntity;

/** Fixed stitched dolls and mobile button-eyed rats share perception, not AI. */
public final class TycoonSpies {
    public static String purchase(ServerPlayer p,String kind){
        var h=TycoonWorld.home(p);if(!kind.equals("rat")&&!kind.equals("doll"))return "Unknown surveillance type.";
        if(p.serverLevel().dimension()!=TycoonWorld.KEY||!TycoonLandscape.owned(h,p.blockPosition()))return "Stand on your owned terrain to place surveillance.";
        var all=h.state.getCompound("Spies").copy();if(all.size()>=64)return "Maximum 64 surveillance actors per owner.";
        var n=new CompoundTag();var id=UUID.randomUUID();n.putUUID("Id",id);n.putString("Kind",kind);n.putLong("At",p.blockPosition().asLong());n.putInt("Tier",1);n.putFloat("Yaw",p.getYRot());all.put(id.toString(),n);var patch=new CompoundTag();patch.put("Spies",all);
        return TycoonTransactions.purchase(p,kind.equals("rat")?16:12,net.minecraft.world.item.ItemStack.EMPTY,patch,null,new CompoundTag());
    }
    public static String upgrade(ServerPlayer p,UUID id){
        var all=TycoonWorld.home(p).state.getCompound("Spies").copy();var n=all.getCompound(id.toString());if(n.isEmpty()||n.getInt("Tier")>=3)return "Select an owned spy below Tier 3.";
        int cost=n.getInt("Tier")*12;n.putInt("Tier",n.getInt("Tier")+1);all.put(id.toString(),n);var patch=new CompoundTag();patch.put("Spies",all);return TycoonTransactions.purchase(p,cost,net.minecraft.world.item.ItemStack.EMPTY,patch,null,new CompoundTag());
    }
    public static void tick(ServerPlayer p){
        var h=TycoonWorld.home(p);var l=p.server.getLevel(TycoonWorld.KEY);if(l==null)return;var all=h.state.getCompound("Spies");
        for(String key:all.getAllKeys()){
            var n=all.getCompound(key);var at=BlockPos.of(n.getLong("At"));if(!l.hasChunkAt(at))continue;
            Entity actor=n.hasUUID("Actor")?l.getEntity(n.getUUID("Actor")):null;
            if(actor==null&&n.contains("LastAt")){var last=BlockPos.of(n.getLong("LastAt"));l.getChunkAt(last);if(n.hasUUID("Actor"))actor=l.getEntity(n.getUUID("Actor"));}
            if(actor==null){
                if(n.hasUUID("Actor")){var last=BlockPos.of(n.contains("LastAt")?n.getLong("LastAt"):n.getLong("At"));l.getChunkAt(last);int wait=n.getInt("MissingTicks")+20;n.putInt("MissingTicks",wait);if(wait<100||!l.areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(last).toLong()))continue;}
                if(n.getString("Kind").equals("rat")){var rat=com.coraline.registry.ModEntities.MOUSE.get().create(l);if(rat==null)continue;rat.spying(true);actor=rat;}
                else{var doll=TycoonPhaseTwoContent.DOLL_ACTOR.get().create(l);if(doll==null)continue;doll.configure(TycoonWorld.owner(p),n.getUUID("Id"),n.getUUID("Id"),Math.floorMod(n.getUUID("Id").hashCode(),32768));actor=doll;}
                if(n.hasUUID("Actor"))actor.setUUID(n.getUUID("Actor"));actor.getPersistentData().putUUID("TycoonOwner",TycoonWorld.owner(p));actor.getPersistentData().putUUID("TycoonSpy",n.getUUID("Id"));actor.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,n.getFloat("Yaw"),0);if(!l.addFreshEntity(actor))continue;n.putUUID("Actor",actor.getUUID());
            }
            n.remove("MissingTicks");
            n.putLong("LastAt",actor.blockPosition().asLong());
            if(actor instanceof TycoonDollEntity){actor.setYRot(n.getFloat("Yaw")+(float)Math.sin(h.clock()/80.)*(20+n.getInt("Tier")*10));scan(p,actor,n);}
        }
        TycoonWorld.dirty(p);
    }
    public static boolean dollTick(TycoonDollEntity doll){
        if(!doll.getPersistentData().hasUUID("TycoonSpy"))return false;
        var h=doll.owner==null?null:TycoonData.get(doll.getServer()).homes.get(doll.owner);var n=h==null?new CompoundTag():h.state.getCompound("Spies").getCompound(doll.getPersistentData().getUUID("TycoonSpy").toString());
        if(n.isEmpty()||!n.hasUUID("Actor")||!doll.getUUID().equals(n.getUUID("Actor")))doll.discard();return true;
    }
    public static boolean mouseTick(MouseEntity rat){
        var tag=rat.getPersistentData();if(!tag.hasUUID("TycoonSpy"))return false;var p=TycoonOfflineCollaboration.representative(rat.getServer(),tag.getUUID("TycoonOwner"));if(p==null){rat.getNavigation().stop();return true;}
        return TycoonScope.call(p,tag.getUUID("TycoonOwner"),()->mouseOwned(p,rat),true);
    }
    private static boolean mouseOwned(ServerPlayer p,MouseEntity rat){
        var tag=rat.getPersistentData();if(TycoonWorld.busy(p)){rat.getNavigation().stop();return true;}
        var h=TycoonWorld.home(p);var n=h.state.getCompound("Spies").getCompound(tag.getUUID("TycoonSpy").toString());
        if(n.isEmpty()||!n.hasUUID("Actor")||!rat.getUUID().equals(n.getUUID("Actor"))){rat.discard();return true;}
        if(rat.tickCount%20!=0)return true;scan(p,rat,n);n.putLong("LastAt",rat.blockPosition().asLong());
        var home=BlockPos.of(n.getLong("At"));if(rat.getNavigation().isDone()&&h.clock()>=n.getLong("PatrolAfter")){
            int phase=Math.floorMod((int)(h.clock()/200)+tag.getUUID("TycoonSpy").hashCode(),4),radius=3+n.getInt("Tier");var at=home.offset(phase==0?radius:phase==2?-radius:0,0,phase==1?radius:phase==3?-radius:0);
            if(TycoonLandscape.owned(h,at)&&TycoonWorld.safeFeet((ServerLevel)rat.level(),at)){var path=rat.getNavigation().createPath(at,0);if(path!=null&&path.canReach())rat.getNavigation().moveTo(path,.6+.15*n.getInt("Tier"));}n.putLong("PatrolAfter",h.clock()+100);
        }TycoonWorld.dirty(p);return true;
    }
    private static void scan(ServerPlayer p,Entity spy,CompoundTag n){
        var h=TycoonWorld.home(p);int tier=n.getInt("Tier");double range=(spy instanceof MouseEntity?7:10)+tier*3,cosine=spy instanceof MouseEntity?-.1:Math.cos(Math.toRadians(40+tier*10));
        for(var r:h.residents.values()){
            if(!TycoonVisits.inside(r)||!r.n.getCompound("Activity").getString("State").equals("WANDERING")&&!r.n.hasUUID("RescuingFarm")||!r.n.hasUUID("Entity"))continue;
            var child=spy.level() instanceof ServerLevel l?l.getEntity(r.n.getUUID("Entity")):null;if(child==null||!TycoonEvidence.perceives(spy,child.getEyePosition(),range,cosine))continue;
            var marker=r.n.getCompound("SpyMarker");boolean urgent=r.n.hasUUID("RescuingFarm"),newReport=marker.getLong("Expires")<=h.clock()||urgent&&!marker.getBoolean("Urgent");marker.putLong("At",child.blockPosition().asLong());marker.putLong("Expires",h.clock()+TycoonAttractionConfig.TRACK_TICKS.get());marker.putUUID("Spy",n.getUUID("Id"));marker.putString("Source",n.getString("Kind"));marker.putBoolean("Urgent",urgent);if(urgent)marker.putUUID("Farm",r.n.getUUID("RescuingFarm"));r.n.put("SpyMarker",marker);
            if(newReport)TycoonWorld.message(p,urgent?"Spy "+n.getString("Kind")+" detected a containment rescue by "+r.name()+" at Soul Farm "+TycoonSoulFarms.farms(h).getCompound(r.n.getUUID("RescuingFarm").toString()).getInt("Ordinal")+"! Temporary last-known marker active.":"Spy "+n.getString("Kind")+" spotted "+r.name()+" near "+child.blockPosition().toShortString()+". Last-known marker is temporary.");
        }
    }
    public static ListTag markers(TycoonData.Home h){var list=new ListTag();for(var r:h.residents.values()){var marker=r.n.getCompound("SpyMarker");if(marker.getLong("Expires")<=h.clock())continue;var row=marker.copy();row.putString("Name",r.name());row.putLong("Remaining",marker.getLong("Expires")-h.clock());list.add(row);}return list;}
    private TycoonSpies(){}
}
