package com.coraline.feast;

import com.coraline.palace.*;
import com.coraline.palace.PalaceSitePlan.Kind;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import static com.coraline.palace.PalaceTransportProgress.Phase.*;

/** Physical owner-specific company transport. Structures remain world-unique;
 * only four workers and their real truck/carrier belong to this ride. No client
 * packet supplies progress, elapsed time or a destination. No keys are granted.
 */
@Mod.EventBusSubscriber(modid="coraline")
public final class PalaceTransport {
    public static final int MOVER=78,TRUCK=79,CARRIER=80;
    private static final Set<UUID> UNLOCKED=new HashSet<>();
    private static CompoundTag data(VisitData.Scene s){
        var root=PalaceJourney.existing(s);if(root==null||!root.contains("Transport",Tag.TAG_COMPOUND))return null;var n=root.getCompound("Transport");
        if(!n.contains("Version",Tag.TAG_INT)||n.getInt("Version")!=1||!n.hasUUID("Owner")||!s.owner.equals(n.getUUID("Owner"))||!n.hasUUID("Door")||!s.door.equals(n.getUUID("Door"))||!n.contains("Progress",Tag.TAG_COMPOUND)||!n.contains("Bay",Tag.TAG_INT)||n.getInt("Bay")<0||n.getInt("Bay")>1)return null;
        for(String key:new String[]{"StartX","StartY","StartZ"})if(!n.contains(key,Tag.TAG_DOUBLE)||!Double.isFinite(n.getDouble(key)))return null;
        if(Math.abs(n.getDouble("StartX"))>PalaceSitePlan.WORLD_LIMIT||Math.abs(n.getDouble("StartZ"))>PalaceSitePlan.WORLD_LIMIT||n.getDouble("StartY")< -64||n.getDouble("StartY")>319)return null;
        for(String key:new String[]{"Truck","Carrier","Mover0","Mover1","Mover2","Mover3"})if(n.contains(key)&&!n.hasUUID(key))return null;
        for(String key:new String[]{"Paused","Reconnect","Arrived","Cleaned","Approached0","Approached1","Approached2","Approached3"})if(n.contains(key)&&(!n.contains(key,Tag.TAG_BYTE)||n.getByte(key)<0||n.getByte(key)>1))return null;
        if(n.contains("Route")&&(!n.contains("Route",Tag.TAG_INT)||n.getInt("Route")<0||n.getInt("Route")>5))return null;
        if(n.contains("DestinationBay")&&(!n.contains("DestinationBay",Tag.TAG_INT)||n.getInt("DestinationBay")<0||n.getInt("DestinationBay")>1))return null;
        for(String key:new String[]{"Truck","Carrier","Mover0","Mover1","Mover2","Mover3"})if(n.contains(key+"Position")&&(!n.hasUUID(key)||!n.contains(key+"Position",Tag.TAG_LONG)))return null;
        return n;
    }
    private static PalaceTransportProgress progress(VisitData.Scene s,CompoundTag n){return PalaceTransportProgress.load(s.owner,s.door,n.getCompound("Progress"));}
    static boolean completed(VisitData.Scene s){var n=s==null?null:data(s);if(n==null)return false;var state=progress(s,n);return state.blocker().isEmpty()&&state.phase()==COMPLETE;}
    private static BlockPos company(ServerPlayer p){return PalaceSiteData.get(p.server).position(Kind.COMPANY).orElseThrow();}
    public static BlockPos desk(ServerPlayer p){return company(p).offset(-5,1,-4);}
    private static Vec3 parked(ServerPlayer p,CompoundTag n,boolean arrived){
        var sites=PalaceSiteData.get(p.server);int bay=arrived&&n.contains("DestinationBay")?n.getInt("DestinationBay"):n.getInt("Bay");return Vec3.atBottomCenterOf(sites.position(arrived?Kind.PALACE:Kind.COMPANY).orElseThrow()).add(arrived?bay==0?-7:7:17,arrived?0:1,arrived?-55:bay==0?-8:5);
    }
    private static boolean nearSites(ServerPlayer p,BlockPos at){var sites=PalaceSiteData.get(p.server);return sites.position(Kind.COMPANY).map(c->c.distSqr(at)<96*96).orElse(false)||sites.position(Kind.PALACE).map(c->c.distSqr(at)<96*96).orElse(false);}
    private static boolean freeBay(ServerPlayer p,VisitData.Scene own,CompoundTag n,boolean arrived){
        int bay=arrived?n.getInt("DestinationBay"):n.getInt("Bay");var at=parked(p,n,arrived);
        for(var other:VisitData.get(p.server).scenes.values())if(!other.owner.equals(own.owner)){
            var reservation=data(other);if(reservation==null||reservation.getBoolean("Cleaned"))continue;
            var state=progress(other,reservation);if(!state.blocker().isEmpty())return false;
            int reserved=arrived&&reservation.contains("DestinationBay")?reservation.getInt("DestinationBay"):reservation.getInt("Bay");
            if(reserved==bay&&(arrived||!reservation.getBoolean("Arrived")))return false;
        }
        return p.server.overworld().getEntitiesOfClass(CinematicPropEntity.class,new AABB(at.add(-2.1,-1,-5),at.add(2.1,4,5)),e->e.kind()==TRUCK&&(!n.hasUUID("Truck")||!n.getUUID("Truck").equals(e.getUUID()))).isEmpty();
    }
    private static void rememberActors(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        for(String key:new String[]{"Truck","Carrier","Mover0","Mover1","Mover2","Mover3"})if(n.hasUUID(key)){
            var e=p.server.overworld().getEntity(n.getUUID(key));if(e instanceof CinematicPropEntity prop&&prop.owned(s))n.putLong(key+"Position",prop.blockPosition().asLong());
        }
    }
    private static CinematicPropEntity find(ServerPlayer p,VisitData.Scene s,CompoundTag n,String key){
        if(!n.hasUUID(key))return null;
        if(n.contains(key+"Position",Tag.TAG_LONG)){
            var at=BlockPos.of(n.getLong(key+"Position"));if(!nearSites(p,at))return null;
            p.server.overworld().getChunkSource().addRegionTicket(TicketType.PORTAL,new net.minecraft.world.level.ChunkPos(at),2,at);
        }
        var e=p.server.overworld().getEntity(n.getUUID(key));
        int kind=key.equals("Truck")?TRUCK:key.equals("Carrier")?CARRIER:MOVER;int slot=kind==MOVER?Integer.parseInt(key.substring(5)):0;
        return e instanceof CinematicPropEntity prop&&prop.owned(s)&&prop.kind()==kind&&prop.slot()==slot?prop:null;
    }
    private static CinematicPropEntity ensure(ServerPlayer p,VisitData.Scene s,CompoundTag n,String key,int kind,int slot,Vec3 at){
        if(n.contains(key))return find(p,s,n,key); // Unloaded/missing UUID is never permission to clone.
        var e=CinematicPropEntity.TYPE.get().create(p.server.overworld());if(e==null)return null;e.configure(s,kind,slot);e.setPos(at);
        if(!p.server.overworld().addFreshEntity(e))return null;n.putUUID(key,e.getUUID());n.putLong(key+"Position",e.blockPosition().asLong());OtherMotherLoop.changed(p);return e;
    }
    private static boolean eligible(ServerPlayer p,VisitData.Scene s){
        var root=PalaceJourney.existing(s);return PalaceJourney.owns(p,s)&&p.isAlive()&&!p.isSpectator()&&p.serverLevel()==p.server.overworld()
                &&PalaceSiteData.get(p.server).ready()&&root!=null&&!root.contains("PersonalFarewell")&&root.getBoolean("MapIssued")&&(!root.contains("Transport")||data(s)!=null);
    }
    private static void offer(ServerPlayer p,VisitData.Scene s){
        if(!eligible(p,s)||p.distanceToSqr(desk(p).getCenter())>25)return;var root=PalaceJourney.existing(s);var n=data(s);
        if(n!=null&&progress(s,n).phase()==COMPLETE)return;
        if(n!=null){p.sendSystemMessage(Component.literal("Your existing truck is waiting. Use /coralinepalace resume beside it, or /coralinepalace leave for a safe exit."));return;}
        var nonce=UUID.randomUUID();root.putUUID("MoverOffer",nonce);root.putLong("MoverOfferUntil",p.server.overworld().getGameTime()+600);OtherMotherLoop.changed(p);
        p.sendSystemMessage(Component.literal("Sign up as a mover. ").append(Component.literal("[Sign up]").withStyle(style->style.withColor(net.minecraft.ChatFormatting.GOLD).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/coralinepalace register "+nonce)))));
    }
    private static boolean clearPlayer(ServerPlayer p,Vec3 at){
        var box=p.getDimensions(p.getPose()).makeBoundingBox(at).deflate(.025);
        return !p.serverLevel().getBlockCollisions(p,box).iterator().hasNext()&&!p.serverLevel().containsAnyLiquid(box);
    }
    private static boolean clearRoute(ServerPlayer p,Vec3 a,Vec3 b){
        int samples=Math.max(1,(int)Math.ceil(a.distanceTo(b)*4));
        for(int i=0;i<=samples;i++)if(!clearPlayer(p,a.lerp(b,i/(double)samples)))return false;return true;
    }
    private static Vec3[] route(ServerPlayer p,CompoundTag n){
        var c=Vec3.atBottomCenterOf(company(p));var truck=parked(p,n,false);
        return new Vec3[]{new Vec3(n.getDouble("StartX"),n.getDouble("StartY"),n.getDouble("StartZ")),c.add(0,2,-6),c.add(0,2,-2),c.add(8,2,-2),c.add(14,2,-2),new Vec3(truck.x,truck.y+1,truck.z+5.3)};
    }
    private static boolean destination(ServerPlayer p,CompoundTag n){
        var at=parked(p,n,true);var level=p.server.overworld();var center=BlockPos.containing(at);
        level.getChunkSource().addRegionTicket(TicketType.PORTAL,new net.minecraft.world.level.ChunkPos(center),3,center);
        for(int x:new int[]{center.getX()-3,center.getX()+3})for(int z:new int[]{center.getZ()-5,center.getZ()+5})if(level.getChunkSource().getChunkNow(x>>4,z>>4)==null)return false;
        var box=new AABB(at.x-2.1,at.y+.01,at.z-4.8,at.x+2.1,at.y+3.8,at.z+4.7);
        var rear=BlockPos.containing(at.add(0,0,5.4));
        boolean occupied=!level.getEntities(p,box,e->e instanceof net.minecraft.world.entity.LivingEntity&&e.isAlive()&&(!(e instanceof com.coraline.entity.CharacterEntity actor)||!CanonicalRealWybie.dormant(actor))).isEmpty();
        return !occupied&&freeBay(p,OtherMotherLoop.scene(p),n,true)&&!level.getBlockCollisions(null,box).iterator().hasNext()&&level.getBlockState(rear.below()).isFaceSturdy(level,rear.below(),net.minecraft.core.Direction.UP)
                &&clearPlayer(p,Vec3.atBottomCenterOf(rear));
    }
    public static int register(ServerPlayer p,UUID offer){
        var s=OtherMotherLoop.scene(p);if(!eligible(p,s)||p.distanceToSqr(desk(p).getCenter())>25)return 0;
        if(p.getZ()>company(p).getZ()-5.3){p.sendSystemMessage(Component.literal("Please stand in front of the reception desk to sign up."));return 0;}
        var root=PalaceJourney.existing(s);long now=p.server.overworld().getGameTime();
        if(!root.hasUUID("MoverOffer")||!offer.equals(root.getUUID("MoverOffer"))||now>root.getLong("MoverOfferUntil"))return 0;
        if(data(s)!=null){p.sendSystemMessage(Component.literal("Your existing ride needs recovery; no second truck has been created."));return 0;}
        var n=new CompoundTag();n.putInt("Version",1);n.putUUID("Owner",s.owner);n.putUUID("Door",s.door);n.putInt("Bay",0);
        for(int bay=0;bay<2;bay++){
            n.putInt("Bay",bay);
            if(!freeBay(p,s,n,false)){if(bay==1){p.sendSystemMessage(Component.literal("The loading bays are busy. Please try again shortly."));return 0;}continue;}break;
        }
        for(int bay=0;bay<2;bay++){n.putInt("DestinationBay",bay);if(freeBay(p,s,n,true))break;if(bay==1){p.sendSystemMessage(Component.literal("Both Palace arrival bays are occupied. Please try again shortly."));return 0;}}
        n.putDouble("StartX",p.getX());n.putDouble("StartY",p.getY()+1);n.putDouble("StartZ",p.getZ());
        var route=route(p,n);for(int i=1;i<route.length;i++)if(!clearRoute(p,route[i-1],route[i])){p.sendSystemMessage(Component.literal("The carrying route is obstructed. Nothing has been moved."));return 0;}
        if(!destination(p,n)){p.sendSystemMessage(Component.literal("Preparing the destination safely. Please confirm again in a moment."));return 0;}
        n.put("Progress",PalaceTransportProgress.start(s.owner,s.door).save());root.put("Transport",n);root.remove("MoverOffer");root.remove("MoverOfferUntil");
        OtherMotherLoop.changed(p);p.server.overworld().getDataStorage().save();return 1;
    }
    private static boolean attached(ServerPlayer p,CinematicPropEntity e){if(e==null)return false;if(p.getVehicle()==e)return true;UNLOCKED.add(p.getUUID());try{return p.startRiding(e,true);}finally{UNLOCKED.remove(p.getUUID());}}
    private static void fail(ServerPlayer p,VisitData.Scene s,CompoundTag n,String reason){
        n.putBoolean("Paused",true);n.putString("Blocker",reason);OtherMotherLoop.changed(p);UNLOCKED.add(p.getUUID());try{p.stopRiding();}finally{UNLOCKED.remove(p.getUUID());}
        p.sendSystemMessage(Component.literal(reason+" Use /coralinepalace leave for a safe exit."));
    }
    private static boolean movePlayerCarrier(ServerPlayer p,CinematicPropEntity carrier,Vec3 feet,double speed){
        var current=carrier.position().add(0,1,0);var delta=feet.subtract(current);var next=current.add(delta.length()>speed?delta.normalize().scale(speed):delta);
        if(!clearRoute(p,current,next))return false;carrier.setPos(next.add(0,-1,0));p.fallDistance=0;return true;
    }
    private static boolean movers(ServerPlayer p,VisitData.Scene s,CompoundTag n,Vec3 center,int cue,int ticks){
        boolean close=true;
        var c=Vec3.atBottomCenterOf(company(p));
        for(int i=0;i<4;i++){
            var e=ensure(p,s,n,"Mover"+i,MOVER,i,c.add(6+i,1,-6+i*2));if(e==null)throw new IllegalStateException("Original mover is unavailable; no duplicate created");
            var target=center.add((i%2==0?-1:1)*.85,0,(i<2?-1:1)*.7);target=new Vec3(target.x,n.getBoolean("Arrived")?parked(p,n,true).y:c.y+1,target.z);var d=target.subtract(e.position());
            if(cue==0&&!n.getBoolean("Approached"+i)){
                var staging=c.add(1.5+i*.15,1,-6.5);
                if(e.position().distanceToSqr(staging)<.04)n.putBoolean("Approached"+i,true);
                else{target=staging;d=target.subtract(e.position());close=false;}
            }
            var step=e.position().add(d.length()>.12?d.normalize().scale(.12):d);
            if(!p.serverLevel().getBlockCollisions(e,new AABB(step.x-.28,step.y,step.z-.28,step.x+.28,step.y+1.85,step.z+.28)).iterator().hasNext())e.setPos(step);
            var look=cue==4?parked(p,n,true).add(i%2==0?-.2:.2,1.75,4.45):center;
            e.setYRot((float)Math.toDegrees(Math.atan2(look.z-e.getZ(),look.x-e.getX()))-90);e.frame(cue==4&&i>=2?0:cue,ticks,(float)Math.min(1,d.length()));
            close&=e.position().distanceToSqr(target)<.25;
        }
        return close;
    }
    private static void tick(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        var state=progress(s,n);if(!state.blocker().isEmpty())return;
        if(state.phase()==COMPLETE){
            if(n.getBoolean("Cleaned"))return;
            var truck=find(p,s,n,"Truck");if(truck!=null&&p.distanceToSqr(truck)>576&&p.server.overworld().players().stream().noneMatch(q->q.distanceToSqr(truck)<256)){
                for(String key:new String[]{"Truck","Carrier","Mover0","Mover1","Mover2","Mover3"}){var prop=find(p,s,n,key);if(prop!=null&&prop.kind()>=MOVER&&prop.kind()<=CARRIER)prop.discard();}n.putBoolean("Cleaned",true);OtherMotherLoop.changed(p);
            }return;
        }
        if(n.getBoolean("Paused"))return;
        if(!eligible(p,s)){fail(p,s,n,"Ride paused because its owner or world is not available.");return;}
        var pickup=new Vec3(n.getDouble("StartX"),n.getDouble("StartY"),n.getDouble("StartZ"));
        if(pickup.distanceToSqr(Vec3.atBottomCenterOf(company(p)))>24*24){fail(p,s,n,"Saved pickup lies outside the original company; compatibility review is required.");return;}
        var truck=ensure(p,s,n,"Truck",TRUCK,0,parked(p,n,false));if(truck==null){fail(p,s,n,"Original truck is unavailable; no duplicate created.");return;}
        var carrier=ensure(p,s,n,"Carrier",CARRIER,0,p.position().add(0,-1,0));if(carrier==null){fail(p,s,n,"Original carrier is unavailable; no duplicate created.");return;}
        var phase=state.phase();boolean inTruck=phase.ordinal()>=CLOSE.ordinal()&&phase.ordinal()<=ARRIVE.ordinal();
        if(phase!=NOTICE&&phase!=EXIT&&!attached(p,inTruck?truck:carrier)){fail(p,s,n,"The cinematic attachment could not be established.");return;}
        long now=p.server.overworld().getGameTime();if(!state.tick(s.owner,state.session(),now,p.getVehicle()==(inTruck?truck:carrier)))return;int t=state.ticks();
        boolean next=false;var nextPhase=phase;boolean ready=false;
        switch(phase){
            case NOTICE->{
                if(p.distanceToSqr(desk(p).getCenter())>64){fail(p,s,n,"Registration paused because you left reception before pickup.");return;}
                boolean near=movers(p,s,n,p.position().add(0,1,0),0,t);truck.frame(0,t,1);ready=destination(p,n);next=t>=40&&ready&&near;nextPhase=LIFT;carrier.setPos(p.position().add(0,-1,0));
                if(next){n.putDouble("StartX",p.getX());n.putDouble("StartY",p.getY()+1);n.putDouble("StartZ",p.getZ());}
            }
            case LIFT->{movers(p,s,n,carrier.position().add(0,1,0),1,t);var target=new Vec3(n.getDouble("StartX"),n.getDouble("StartY"),n.getDouble("StartZ"));if(!movePlayerCarrier(p,carrier,target,.04)){fail(p,s,n,"The lifting space is blocked.");return;}next=t>=30&&carrier.position().add(0,1,0).distanceToSqr(target)<.02;nextPhase=CARRY;}
            case CARRY->{
                var path=route(p,n);int index=n.getInt("Route");if(index>=path.length)index=path.length-1;
                if(!movePlayerCarrier(p,carrier,path[index],.08)){fail(p,s,n,"The carrying route became obstructed.");return;}
                if(!movers(p,s,n,carrier.position().add(0,1,0),2,t)){fail(p,s,n,"The workers cannot follow the clear carrying route safely.");return;}
                if(carrier.position().add(0,1,0).distanceToSqr(path[index])<.01){if(index+1<path.length)n.putInt("Route",index+1);else{next=true;nextPhase=THROW;}}
            }
            case THROW->{
                var start=route(p,n)[route(p,n).length-1];var end=truck.position().add(0,.88,2);double u=Math.min(1,t/30.0);var at=start.lerp(end,u).add(0,Math.sin(u*Math.PI)*.65,0);
                if(!movePlayerCarrier(p,carrier,at,.35)){fail(p,s,n,"The cargo opening became obstructed.");return;}movers(p,s,n,start,3,t);truck.frame(0,t,1);
                next=t>=30&&carrier.position().add(0,1,0).distanceToSqr(end)<.02;nextPhase=CLOSE;
            }
            case CLOSE->{truck.frame(1,t,(float)Math.max(0,1-t/40.0));ready=destination(p,n);next=t>=40&&ready;nextPhase=TRANSIT;}
            case TRANSIT->{
                ready=destination(p,n);if(!ready){fail(p,s,n,"Destination safety changed; the truck stopped rather than transferring into an obstruction.");return;}
                truck.frame(2,t,0);if(t>=60&&t<160&&t%20==0)p.playNotifySound(SoundEvents.MINECART_RIDING,SoundSource.NEUTRAL,.28F,.7F);
                if(t<160)truck.setPos(parked(p,n,false).add(0,t>=60?Math.sin(t*1.17)*.025:0,0));
                if(t==160){
                    var workers=new ArrayList<CinematicPropEntity>();for(int i=0;i<4;i++){var mover=find(p,s,n,"Mover"+i);if(mover==null){fail(p,s,n,"An original worker is unavailable before transfer.");return;}workers.add(mover);}
                    var at=parked(p,n,true);truck.setPos(at);UNLOCKED.add(p.getUUID());try{p.stopRiding();p.connection.teleport(at.x,at.y+.88,at.z+2,p.getYRot(),p.getXRot());p.startRiding(truck,true);}finally{UNLOCKED.remove(p.getUUID());}
                    n.putBoolean("Arrived",true);truck.frame(3,0,0);
                    if(p.getVehicle()!=truck){fail(p,s,n,"Destination attachment failed; safe exit is available.");return;}
                    for(int i=0;i<4;i++)workers.get(i).setPos(at.add(i%2==0?-1:1,0,5.2+(i<2?-.6:.6)));
                    n.putBoolean("Arrived",true);p.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(SoundEvents.MINECART_RIDING.getLocation(),SoundSource.NEUTRAL));p.playNotifySound(SoundEvents.GRINDSTONE_USE,SoundSource.NEUTRAL,.3F,.65F);next=true;nextPhase=ARRIVE;
                }
            }
            case ARRIVE->{boolean near=movers(p,s,n,truck.position().add(0,1,5.3),4,t);truck.frame(3,t,near?t/40F:0);next=t>=40&&near&&verifiedExit(truck,p)!=null;nextPhase=EXIT;}
            case EXIT->{truck.frame(3,40,1);if(p.getVehicle()!=truck&&p.distanceToSqr(truck)<144&&p.onGround()&&clearPlayer(p,p.position())){next=true;nextPhase=COMPLETE;}}
            case COMPLETE->{}
        }
        if(next)state.motionFinished(s.owner,state.session(),nextPhase,true,ready);
        n.put("Progress",state.save());rememberActors(p,s,n);OtherMotherLoop.changed(p);
    }
    public static int leave(ServerPlayer p){
        var s=OtherMotherLoop.scene(p);var n=s==null?null:data(s);if(n==null||!p.isAlive()||p.serverLevel()!=p.server.overworld()||progress(s,n).phase()==COMPLETE)return 0;
        var truck=find(p,s,n,"Truck");var carrier=find(p,s,n,"Carrier");if((truck==null||p.distanceToSqr(truck)>256)&&(carrier==null||p.distanceToSqr(carrier)>256))return 0;var center=parked(p,n,n.getBoolean("Arrived"));
        Vec3 safe=null;for(int distance=6;distance<=12&&safe==null;distance++){
            var column=BlockPos.containing(center.add(0,0,distance));p.server.overworld().getChunkAt(column);
            for(int dy=3;dy>=-3;dy--){var at=column.offset(0,dy,0);if(p.server.overworld().getBlockState(at.below()).isFaceSturdy(p.server.overworld(),at.below(),net.minecraft.core.Direction.UP)&&clearPlayer(p,Vec3.atBottomCenterOf(at))){safe=Vec3.atBottomCenterOf(at);break;}}
        }
        if(safe==null){p.sendSystemMessage(Component.literal("No verified truck exit is clear yet. Clear the space behind the truck, then retry."));return 0;}
        n.putBoolean("Paused",true);UNLOCKED.add(p.getUUID());try{p.stopRiding();p.teleportTo(p.server.overworld(),safe.x,safe.y,safe.z,p.getYRot(),p.getXRot());p.fallDistance=0;}finally{UNLOCKED.remove(p.getUUID());}OtherMotherLoop.changed(p);return 1;
    }
    public static int resume(ServerPlayer p){
        var s=OtherMotherLoop.scene(p);var n=s==null?null:data(s);if(n==null||!eligible(p,s)||!n.getBoolean("Paused"))return 0;var state=progress(s,n);
        if(!state.blocker().isEmpty()||state.phase()==COMPLETE)return 0;
        var vehicle=find(p,s,n,state.phase().ordinal()>=CLOSE.ordinal()?"Truck":"Carrier");if(vehicle==null||p.distanceToSqr(vehicle)>144)return 0;
        if(!state.resume(s.owner))return 0;
        if(state.phase()!=NOTICE&&state.phase()!=EXIT&&!attached(p,vehicle))return 0;
        n.put("Progress",state.save());n.remove("Paused");n.remove("Reconnect");n.remove("Blocker");OtherMotherLoop.changed(p);return 1;
    }
    public static boolean locked(ServerPlayer p,CinematicPropEntity prop){
        if(UNLOCKED.contains(p.getUUID()))return false;var s=OtherMotherLoop.scene(p);var n=s==null?null:data(s);if(n==null||n.getBoolean("Paused")||!prop.owned(s)||!p.isAlive())return false;
        var state=progress(s,n);return state.blocker().isEmpty()&&state.phase()!=EXIT&&state.phase()!=COMPLETE;
    }
    private static Vec3 verifiedExit(CinematicPropEntity truck,net.minecraft.world.entity.LivingEntity passenger){
        var at=truck.position().add(0,0,5.4);var l=truck.level();var p=BlockPos.containing(at);
        for(int dy=2;dy>=-3;dy--){var ground=p.offset(0,dy,0);var target=Vec3.atBottomCenterOf(ground);if(l.hasChunkAt(ground)&&l.getBlockState(ground.below()).isFaceSturdy(l,ground.below(),net.minecraft.core.Direction.UP)&&l.noCollision(passenger,passenger.getDimensions(passenger.getPose()).makeBoundingBox(target)))return target;}
        return null;
    }
    public static Vec3 exit(CinematicPropEntity truck,net.minecraft.world.entity.LivingEntity passenger){var safe=verifiedExit(truck,passenger);return safe==null?truck.position().add(0,0,5.4):safe;}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){if(event.player instanceof net.minecraft.server.level.ServerPlayer coopPlayer&&com.coraline.multiplayer.StoryCoop.directorBlocked(coopPlayer))return;
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer p))return;var s=OtherMotherLoop.scene(p);if(s==null)return;var n=data(s);
        if(n!=null){try{if(n.getBoolean("Reconnect"))resume(p);tick(p,s,n);}catch(RuntimeException failure){fail(p,s,n,"Ride paused: "+failure.getMessage());}return;}
        if(p.tickCount%40==0&&eligible(p,s)&&p.distanceToSqr(desk(p).getCenter())<36)p.displayClientMessage(Component.literal("Sign up as a mover."),true);
    }
    @SubscribeEvent public static void desk(PlayerInteractEvent.RightClickBlock event){if(!(event.getEntity() instanceof ServerPlayer p))return;var s=OtherMotherLoop.scene(p);if(!eligible(p,s)||!event.getPos().equals(desk(p)))return;event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);offer(p,s);}
    @SubscribeEvent public static void mount(EntityMountEvent event){if(event.isDismounting()&&event.getEntityMounting() instanceof ServerPlayer p&&event.getEntityBeingMounted() instanceof CinematicPropEntity prop&&(prop.kind()==TRUCK||prop.kind()==CARRIER)&&!UNLOCKED.contains(p.getUUID())){if(locked(p,prop)||prop.kind()==TRUCK&&verifiedExit(prop,p)==null){event.setCanceled(true);if(p.tickCount%20==0)p.sendSystemMessage(Component.literal("The normal exit is obstructed. Use /coralinepalace leave for a verified safe exit."));}}}
    @SubscribeEvent public static void damage(LivingAttackEvent event){if(event.getEntity() instanceof ServerPlayer p&&p.getVehicle() instanceof CinematicPropEntity prop&&(prop.kind()==TRUCK||prop.kind()==CARRIER)&&locked(p,prop))event.setCanceled(true);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p){if(com.coraline.multiplayer.StoryCoop.guest(p))return;var s=OtherMotherLoop.scene(p);var n=s==null?null:data(s);if(n!=null&&progress(s,n).phase()!=COMPLETE){n.putBoolean("Paused",true);n.putBoolean("Reconnect",true);OtherMotherLoop.changed(p);}}}
    private PalaceTransport(){}
}
