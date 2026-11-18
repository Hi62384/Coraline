package com.coraline.feast;

import java.util.*;
import com.coraline.entity.CharacterEntity;
import com.coraline.palace.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import static com.coraline.palace.PalaceWellProgress.Phase.*;

/** Actual original-Hand/original-Wybie well adapter. Audio advances only through
 * ShowPlayback's real mixer gate. All scene actions and inputs are server-owned.
 * Source implementation; native physical/chapter acceptance remains mandatory. */
@Mod.EventBusSubscriber(modid="coraline")
public final class PalaceWellEncounter {
    private static CompoundTag root(VisitData.Scene s){return PalaceJourney.existing(s);}
    private static PalaceWellProgress state(VisitData.Scene s){return PalaceWellProgress.load(s.owner,s.door,root(s).getCompound("Well"));}
    static CompoundTag control(VisitData.Scene s){
        var r=root(s);if(r==null||!r.contains("WellStage",Tag.TAG_COMPOUND))return null;var n=r.getCompound("WellStage");
        if(!n.contains("Version",Tag.TAG_INT)||n.getInt("Version")!=1||!n.hasUUID("Owner")||!s.owner.equals(n.getUUID("Owner"))||!n.hasUUID("Door")||!s.door.equals(n.getUUID("Door")))return null;
        if(!n.contains("Ticks",Tag.TAG_INT)||n.getInt("Ticks")<0||!n.contains("LastTick",Tag.TAG_LONG)||n.getLong("LastTick")< -1)return null;
        for(String key:new String[]{"Paused","CueStarted","ThrowRequested","SmashRequested","HandAtWell","Falling","PresentationRetired","BikeArrived"})if(n.contains(key)&&(!n.contains(key,Tag.TAG_BYTE)||n.getByte(key)<0||n.getByte(key)>1))return null;
        for(String key:new String[]{"Bike"})if(n.contains(key)&&!n.hasUUID(key))return null;
        for(String key:new String[]{"BikePosition","Entry","Departure"})if(n.contains(key)&&!n.contains(key,Tag.TAG_LONG))return null;
        if(n.contains("Combat")&&!n.contains("Combat",Tag.TAG_COMPOUND))return null;
        if(n.contains("ThrowStage")&&(!n.contains("ThrowStage",Tag.TAG_INT)||n.getInt("ThrowStage")<0||n.getInt("ThrowStage")>2))return null;
        if(n.contains("Phase")){if(!n.contains("Phase",Tag.TAG_STRING))return null;try{PalaceWellProgress.Phase.valueOf(n.getString("Phase"));}catch(IllegalArgumentException bad){return null;}}
        if(n.contains("Lunge")){if(!n.contains("Lunge",Tag.TAG_COMPOUND))return null;for(String axis:new String[]{"X","Y","Z"})if(!n.getCompound("Lunge").contains(axis,Tag.TAG_DOUBLE)||!Double.isFinite(n.getCompound("Lunge").getDouble(axis)))return null;}
        return n;
    }
    private static Vec3 center(ServerPlayer p){return Vec3.atBottomCenterOf(PalaceSiteData.get(p.server).position(PalaceSitePlan.Kind.WELL).orElseThrow());}
    private static boolean eligible(ServerPlayer p,VisitData.Scene s){return PalaceJourney.owns(p,s)&&p.isAlive()&&(!p.isSpectator()||com.coraline.multiplayer.StoryCombatSafety.held(p,s))&&!p.isSleeping()&&p.serverLevel()==p.server.overworld()&&PalaceSiteData.get(p.server).ready();}
    public static java.util.UUID combatSession(VisitData.Scene scene){
        var root=scene==null?null:root(scene);if(root==null||!root.contains("Well"))return null;
        var progress=state(scene);return progress.blocker().isEmpty()&&(progress.phase()==RESCUE||progress.phase()==RESCUED||progress.phase()==BOSS)?progress.session():null;
    }
    public static boolean cooperativeCombat(VisitData.Scene scene){
        var root=scene==null?null:root(scene);if(root==null||!root.contains("Well"))return false;var n=control(scene);if(n==null||n.getBoolean("Paused"))return false;
        var progress=state(scene);return progress.blocker().isEmpty()&&(progress.phase()==RESCUE||progress.phase()==BOSS);
    }
    private static ServerPlayer interactionDirector(ServerPlayer actor,VisitData.Scene scene){
        if(scene==null||!com.coraline.multiplayer.StoryCoop.member(actor,scene)||!actor.isAlive()||actor.isSpectator())return null;
        var director=actor.server.getPlayerList().getPlayer(scene.owner);
        return director!=null&&eligible(director,scene)&&actor.serverLevel()==director.serverLevel()?director:null;
    }
    private static java.util.List<ServerPlayer> fighters(ServerPlayer director,VisitData.Scene scene,CinematicPropEntity hand){
        var members=com.coraline.multiplayer.StorySessionData.get(director.server).participants(director.server,scene.owner,true);
        if(members.isEmpty())return director.isAlive()&&!director.isSpectator()?java.util.List.of(director):java.util.List.of();
        return members.stream().filter(p->p.isAlive()&&!p.isSpectator()&&p.serverLevel()==director.serverLevel()&&p.distanceToSqr(center(director))<=48*48).toList();
    }
    private static void save(ServerPlayer p,VisitData.Scene s,PalaceWellProgress progress){root(s).put("Well",progress.save());OtherMotherLoop.changed(p);if(progress.phase()==COMPLETE)com.coraline.rebirth.StoryRebirth.ending(p,s);}
    private static void pause(ServerPlayer p,VisitData.Scene s,CompoundTag n,String why){
        if(!n.getBoolean("Paused")){ShowPlayback.stop(p,s);n.putBoolean("Paused",true);n.putBoolean("CueStarted",false);n.putString("Blocker",why);OtherMotherLoop.changed(p);p.sendSystemMessage(Component.literal(why+" Return to the well and use /coralinepalace well-resume."));}
    }
    private static CinematicPropEntity hand(ServerPlayer p,VisitData.Scene s,PalaceWellProgress progress){
        var e=RescueProps.find(p,s,"SeveredHand");return e!=null&&e.getUUID().equals(progress.originalHand())&&e.kind()==RescueProps.HAND&&e.slot()==0?e:null;
    }
    private static void startBlocked(ServerPlayer p,VisitData.Scene s,CompoundTag root,String reason){
        if(reason.equals(root.getString("WellBlocker")))return;root.putString("WellBlocker",reason);OtherMotherLoop.changed(p);p.sendSystemMessage(Component.literal(reason));
    }
    private static boolean start(ServerPlayer p,VisitData.Scene s){
        var r=root(s);if(r==null||r.contains("Well")||!r.getBoolean("MapIssued")||!PalaceTransport.completed(s)||!eligible(p,s)||!p.onGround()||p.isPassenger()||p.distanceToSqr(center(p))>8*8||p.getY()<center(p).y)return false;
        boolean key=false;for(int i=0;i<p.getInventory().getContainerSize();i++)key|=ParentRescueStory.key(p.getInventory().getItem(i),s);if(!key)return false;
        // No ensure/spawn until the actual severed-hand UUID is proven.
        if(!ParentRescueStory.scenery(s).hasUUID("RescueSeveredHand")){startBlocked(p,s,r,"The original severed hand record is missing; no substitute has been created.");return false;}
        var original=RescueProps.find(p,s,"SeveredHand");if(original==null||original.kind()!=RescueProps.HAND||original.slot()!=0)return false;
        var ghost=GhostVictimEntry.data(s);var alternative=GhostEscapeEndingProgress.existing(ghost,s.owner,s.door);
        if(alternative!=null&&alternative.getBoolean("AlternativeBeldamVictory")&&!GhostEscapeHandProgress.handoffReady(ghost,s.owner,s.door,s.mom,original.getUUID())){
            startBlocked(p,s,r,"The original Hand has not finished its physical detachment at the Little Door.");return false;
        }
        var identity=CanonicalRealWybie.identity(s);
        if(identity==null){var existing=ShowActors.actor(p,s,"wybie");if(existing instanceof CharacterEntity w)CanonicalRealWybie.bind(p,s,w);identity=CanonicalRealWybie.identity(s);}
        if(identity==null||!identity.blocker().isEmpty()){startBlocked(p,s,r,"Original real Wybie is missing or incompatible. Missing is not death; an older deleted save needs an explicit recovery decision.");return false;}
        var progress=PalaceWellProgress.start(s.owner,s.door,original.getUUID(),identity.entity(),identity.fate()==PalaceWybieIdentity.Fate.DEAD,PalaceJourney.prerequisites(s));if(progress.isEmpty())return false;
        var n=new CompoundTag();n.putInt("Version",1);n.putUUID("Owner",s.owner);n.putUUID("Door",s.door);n.putInt("Ticks",0);n.putLong("LastTick",-1);
        r.put("WellStage",n);r.put("Well",progress.get().save());OtherMotherLoop.changed(p);
        // Persist the exact native candidate before reserving its UUID. Recovery
        // can adopt this candidate if interrupted between the two checkpoints.
        p.server.saveEverything(true,false,true);
        if(!com.coraline.postgame.GlobalHandEncounter.claim(p.server,s,progress.get())){
            // A failed read-back may still have written this reservation. Keep
            // its previously saved native candidate so reconnect can retry it.
            if(!com.coraline.postgame.GlobalHandEncounter.reservedFor(p.server,s,progress.get())){r.remove("WellStage");r.remove("Well");}
            startBlocked(p,s,r,"The world Hand encounter is reserved elsewhere or waiting for its saved checkpoint.");return false;
        }
        r.remove("WellBlocker");OtherMotherLoop.changed(p);return true;
    }
    private static CharacterEntity wybie(ServerPlayer p,VisitData.Scene s,PalaceWellProgress progress){
        if(progress.wybieLife()!=PalaceWellProgress.WybieLife.ALIVE)return null;var id=CanonicalRealWybie.identity(s);
        if(id==null||id.fate()!=PalaceWybieIdentity.Fate.ALIVE)return null;var actor=CanonicalRealWybie.find(p,s);
        return actor!=null&&progress.originalWybie().filter(actor.getUUID()::equals).isPresent()?actor:null;
    }
    private static CharacterEntity bring(ServerPlayer p,VisitData.Scene s,CharacterEntity w,Vec3 at){
        if(!CanonicalRealWybie.bind(p,s,w))return null;
        var moved=ShowTravel.guest(w,p,s,p.server.overworld(),at,true,"PalaceWybieReturn");if(!(moved instanceof CharacterEntity result)||!result.getUUID().equals(w.getUUID()))return null;
        result.setPos(at);result.setNoAi(true);result.setNoGravity(true);result.noPhysics=false;result.setInvulnerable(true);result.setInvisible(false);return result;
    }
    private static CinematicPropEntity prop(ServerPlayer p,VisitData.Scene s,String key,int kind,Vec3 at){return RescueProps.ensure(p,s,"Well"+key,kind,0,at);}
    private static WybieBicycleEntity bike(ServerPlayer p,VisitData.Scene s,CompoundTag n,PalaceWellProgress progress,CharacterEntity w){
        if(n.hasUUID("Bike")){
            if(n.contains("BikePosition",Tag.TAG_LONG)){
                var at=BlockPos.of(n.getLong("BikePosition"));if(at.distSqr(BlockPos.containing(center(p)))>96*96)return null;
                p.server.overworld().getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.PORTAL,new net.minecraft.world.level.ChunkPos(at),2,at);
            }
            var raw=p.server.overworld().getEntity(n.getUUID("Bike"));return raw instanceof WybieBicycleEntity b&&b.getPersistentData().hasUUID("PalaceWellOwner")&&s.owner.equals(b.getPersistentData().getUUID("PalaceWellOwner"))?b:null;
        }
        if(w==null)return null;var c=center(p);var palace=PalaceSiteData.get(p.server).position(PalaceSitePlan.Kind.PALACE).orElseThrow();var away=c.subtract(Vec3.atBottomCenterOf(palace)).multiply(1,0,1).normalize();
        if(away.lengthSqr()<.1)away=new Vec3(0,0,1);
        var b=com.coraline.registry.ModEntities.WYBIE_BICYCLE.get().create(p.serverLevel());
        if(b==null)return null;
        long now=p.serverLevel().getGameTime();
        if(now<n.getLong("BikeSpawnRetryAt"))return null;
        int attempt=n.getInt("BikeSpawnSearch");
        n.putInt("BikeSpawnSearch",attempt==Integer.MAX_VALUE?0:attempt+1);
        n.putLong("BikeSpawnRetryAt",now+5);
        var direction=away.yRot((float)Math.toRadians(45*Math.floorMod(attempt,8)));
        var column=c.add(direction.scale(18+4*Math.floorMod(attempt/8,3)));
        var ground=PalaceWellMotion.ground(p.serverLevel(),w,column,6);
        if(ground==null)return null;
        float heading=StoryBikeRoute.yaw(c.subtract(ground));
        var spawn=StoryBikeRoute.floor(b,ground.x,ground.z,ground.y,heading);
        if(spawn==null)return null;
        for(var viewer:p.serverLevel().players()){
            if(viewer.distanceToSqr(spawn)<128*128
                    &&viewer.getLookAngle().dot(spawn.subtract(viewer.getEyePosition()).normalize())>.15
                    &&p.serverLevel().clip(new ClipContext(viewer.getEyePosition(),spawn.add(0,1.25,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,viewer)).getType()==HitResult.Type.MISS)return null;
        }
        b.setYRot(heading);
        b.setPos(spawn);
        var target=entryTarget(p,w,c,attempt/24);
        if(target==null)return null;
        var route=StoryBikeRoute.toward(b,target);
        if(route==null||route.isEmpty())return null;
        b.configure(s.owner,s.door,progress.session(),s.loop.event.getInt("Serial"),w.getUUID(),true);
        b.getPersistentData().putUUID("PalaceWellOwner",s.owner);
        if(!p.serverLevel().addFreshEntity(b))return null;
        n.putUUID("Bike",b.getUUID());
        n.putLong("BikePosition",b.blockPosition().asLong());
        n.putLong("Entry",BlockPos.containing(target).asLong());
        b.wellRoute.addAll(route);
        OtherMotherLoop.changed(p);
        w=bring(p,s,w,spawn);
        if(w!=null)w.startRiding(b,true);
        return b;
    }
    private static Vec3 entryTarget(ServerPlayer p,CharacterEntity w,Vec3 center,int attempt){
        var offset=new Vec3(4.5,0,-4.5).yRot((float)Math.toRadians(45*Math.floorMod(attempt,8)));
        var at=PalaceWellMotion.ground(p.serverLevel(),w,center.add(offset),8);
        return at==null?null:Vec3.atBottomCenterOf(BlockPos.containing(at));
    }
    public static void driveBike(WybieBicycleEntity b){
        var owner=b.getPersistentData().getUUID("PalaceWellOwner");var s=VisitData.get(b.getServer()).scenes.get(owner);var p=b.getServer().getPlayerList().getPlayer(owner);var n=s==null?null:control(s);
        if(p==null||n==null||n.getBoolean("Paused")||!n.hasUUID("Bike")||!b.getUUID().equals(n.getUUID("Bike"))||!eligible(p,s)){b.stop();return;}
        var progress=state(s);var w=wybie(p,s,progress);if(w==null||w.getVehicle()!=b){b.stop();return;}
        boolean entry=progress.phase()==INTRO_A&&ShowPlayback.started(s)&&ShowPlayback.position(s)>=40;
        boolean departing=progress.phase()==DEPARTING;
        if(!entry&&!departing){b.stop();return;}
        String key=departing?"Departure":"Entry";
        if(!n.contains(key)){b.stop();return;}
        var goal=Vec3.atBottomCenterOf(BlockPos.of(n.getLong(key)));
        if(b.distanceToSqr(goal)<.36){
            b.wellRoute.clear();
            b.stop();
            if(entry)n.putBoolean("BikeArrived",true);
            return;
        }
        if(b.wellRoute.isEmpty()){
            long now=p.serverLevel().getGameTime();
            if(now<b.wellReplanAt)return;
            b.wellReplanAt=now+10;
            int attempt=n.getInt("BikeRouteSearch");
            n.putInt("BikeRouteSearch",attempt==Integer.MAX_VALUE?0:attempt+1);
            var route=StoryBikeRoute.toward(b,goal);
            if(route==null){
                Vec3 alternative;
                var c=center(p);
                if(entry)alternative=entryTarget(p,w,c,attempt);
                else{
                    var away=new Vec3(0,0,44).yRot((float)Math.toRadians(45*Math.floorMod(attempt,8)));
                    var ground=PalaceWellMotion.ground(p.serverLevel(),w,c.add(away),18);
                    alternative=ground==null?null:Vec3.atBottomCenterOf(BlockPos.containing(ground));
                }
                if(alternative!=null&&!alternative.equals(goal)){
                    route=StoryBikeRoute.toward(b,alternative);
                    if(route!=null)n.putLong(key,BlockPos.containing(alternative).asLong());
                }
            }
            if(route==null||route.isEmpty()){
                b.stop();
                n.putString("MotionBlocker","No clear bicycle route. Clear the path around Wybie to continue.");
                return;
            }
            n.remove("MotionBlocker");
            b.wellRoute.addAll(route);
        }
        var next=b.wellRoute.peek();
        if(b.storyAdvance(next))b.wellStallTicks=0;
        else if(++b.wellStallTicks>=3){b.wellRoute.clear();b.wellStallTicks=0;return;}
        if(b.position().distanceToSqr(next)<.02)b.wellRoute.remove();
        n.putLong("BikePosition",b.blockPosition().asLong());
    }
    private static boolean cue(ServerPlayer p,VisitData.Scene s,CompoundTag n,char letter){
        String sound="crawling_hand_"+Character.toLowerCase(letter);
        if(!n.getBoolean("CueStarted")||!sound.equals(n.getString("Cue"))){if(!ShowPlayback.start(p,s,sound,false)){pause(p,s,n,"Original well recording is unavailable.");return false;}n.putBoolean("CueStarted",true);n.putString("Cue",sound);}
        boolean done=ShowPlayback.tick(p,s);
        if(ShowPlayback.data(s).getBoolean("SafetySkipped")||!ShowPlayback.data(s).getString("Blocker").isEmpty()){
            pause(p,s,n,"The original well recording was interrupted. Use /coralinepalace well-resume to retry.");
            return false;
        }
        return done&&!ShowPlayback.debugSeeking(s);
    }
    private static boolean move(ServerPlayer p,Entity e,Vec3 target,double speed){return PalaceWellMotion.move(p.serverLevel(),e,target,speed);}
    private static boolean arrived(Entity e,Vec3 at){return e.position().distanceToSqr(at)<.035;}
    private static Vec3 forward(CharacterEntity w){double yaw=Math.toRadians(w.getYRot());return new Vec3(-Math.sin(yaw),0,Math.cos(yaw));}
    private static void face(CharacterEntity w,Vec3 at){var d=at.subtract(w.position());float yaw=(float)Math.toDegrees(Math.atan2(-d.x,d.z));w.setYRot(yaw);w.setYBodyRot(yaw);w.setYHeadRot(yaw);}
    private static void handPosition(VisitData.Scene s,CinematicPropEntity hand){var n=ParentRescueStory.scenery(s);n.putLong("RescueSeveredHandPosition",hand.blockPosition().asLong());n.putString("RescueSeveredHandDimension",hand.level().dimension().location().toString());}
    private static void tick(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        var progress=state(s);if(!progress.blocker().isEmpty()||n.getBoolean("Paused"))return;
        if(!com.coraline.postgame.GlobalHandEncounter.permits(p.server,s,progress)){pause(p,s,n,"Another story owns the world Hand encounter.");return;}
        if(progress.phase()==COMPLETE){
            com.coraline.multiplayer.StoryCombatSafety.restoreGroup(p,s);
            if(!n.getBoolean("PresentationRetired")){for(String key:new String[]{"WellTongs","WellRock","WellPhoto","WellBundle"}){var prop=RescueProps.find(p,s,key);if(prop!=null)prop.setInvisible(true);}ShowPlayback.stop(p,s);n.putBoolean("PresentationRetired",true);OtherMotherLoop.changed(p);}return;
        }
        if(!eligible(p,s)||p.distanceToSqr(center(p))>48*48){pause(p,s,n,"Well encounter paused while its owner is away.");return;}
        long now=p.serverLevel().getGameTime();if(now<=n.getLong("LastTick"))return;n.putLong("LastTick",now);
        if(!progress.phase().name().equals(n.getString("Phase"))){n.putString("Phase",progress.phase().name());n.putInt("Ticks",0);n.putBoolean("CueStarted",false);}
        int t=n.getInt("Ticks");if(t==Integer.MAX_VALUE){pause(p,s,n,"Well clock reached its safety bound.");return;}n.putInt("Ticks",t+1);
        OtherMotherLoop.changed(p);
        var h=hand(p,s,progress);if(h==null){pause(p,s,n,"Waiting for the original severed hand; no duplicate created.");return;}
        var c=center(p);var w=wybie(p,s,progress);
        if(progress.wybieLife()==PalaceWellProgress.WybieLife.ALIVE&&w==null){pause(p,s,n,"Waiting for the original living Wybie; no resurrection or replacement performed.");return;}
        if(!n.getBoolean("HandAtWell")){
            if(PalaceWellMotion.seen(p,h))return;
            h=RescueProps.ensure(p,s,"SeveredHand",RescueProps.HAND,0,p.position());if(h==null)return;
            h.setPos(p.position().add(0,.17,0));h.setInvisible(false);n.putBoolean("HandAtWell",true);
        }
        switch(progress.phase()){
            case DRAG->{
                var target=c.add(0,1,-4.5);var delta=target.subtract(p.position()).multiply(1,0,1);
                if(delta.length()>.5){p.setDeltaMovement(p.getDeltaMovement().add(delta.normalize().scale(.045)));p.hurtMarked=true;}
                move(p,h,p.position().add(0,.17,0),.30);h.frame(10,t,1);progress.activeTick(s.owner,progress.session(),now);
            }
            case INTRO_A->{
                var b=bike(p,s,n,progress,w);if(b==null){if(t%80==0)p.displayClientMessage(Component.literal("Wybie is looking for a clear, hidden approach to the well."),true);return;}w=wybie(p,s,progress);if(w==null)return;boolean done=cue(p,s,n,'A');if(!ShowPlayback.started(s))return;
                var target=Vec3.atBottomCenterOf(BlockPos.of(n.getLong("Entry")));boolean at=n.getBoolean("BikeArrived")&&b.distanceToSqr(target)<.36;
                if(!at&&w.getVehicle()!=b&&w.distanceToSqr(b)>32*32){
                    if(PalaceWellMotion.seen(p,b))return;
                    w=bring(p,s,w,b.position());
                    if(w==null)return;
                }
                if(!at&&w.getVehicle()!=b){var near=PalaceWellMotion.ground(p.serverLevel(),w,b.position().add(1.3,0,0),4);if(near!=null)move(p,w,near,.10);if(w.distanceToSqr(b)<5)w.startRiding(b,true);}
                w.gameGesture(at?70:77,5);
                if(at){b.stop();w.stopRiding();w.setNoGravity(true);w.noPhysics=false;w.setNoAi(true);w.gameGesture(70,5);face(w,h.position());
                    var grip=w.position().add(forward(w).scale(.7)).add(0,1.2,0);var tong=prop(p,s,"Tongs",RescueProps.TONGS,grip);if(tong==null)return;tong.setPos(grip);tong.setYRot(w.getYRot()+180);var caught=grip.add(forward(w).scale(.6));move(p,h,caught,.20);
                    at=arrived(h,caught);}
                progress.finishIntro(s.owner,progress.session(),'A',done,at,0);
            }
            case INTRO_B,INTRO_C,INTRO_D->{
                char letter=progress.phase().name().charAt(6);boolean done=cue(p,s,n,letter);if(!ShowPlayback.started(s))return;
                Vec3 target=letter=='B'?c.add(0,2.3,-4):letter=='C'?c.add(0,2.3,0):c.add(0,-.45,0);
                boolean motion=move(p,w,target,.075)&&arrived(w,target);w.gameGesture(letter=='D'?71:70,5);face(w,p.position());
                var grip=letter=='D'?c.add(0,2.18,-2):w.position().add(forward(w).scale(1.1)).add(0,1.4,0);move(p,h,grip,.20);h.frame(11,t,1);
                var tong=RescueProps.find(p,s,"WellTongs");if(tong!=null){tong.setPos(grip.subtract(forward(w).scale(.5)));tong.setYRot(w.getYRot()+180);tong.setInvisible(letter=='D');}
                double route=letter=='D'&&motion?PalaceWellMotion.rescueRoute(p,h):0;
                progress.finishIntro(s.owner,progress.session(),letter,done,motion&&arrived(h,grip),route);
            }
            case RESCUE->{
                w.gameGesture(71,5);h.frame(11,t,1);progress.activeTick(s.owner,progress.session(),now);
                if(progress.wybieLife()==PalaceWellProgress.WybieLife.LOST_AT_WELL||progress.wybieLife()==PalaceWellProgress.WybieLife.DEAD_AT_WELL){n.putBoolean("Falling",true);ShowPlayback.stop(p,s);}
            }
            case RESCUED->{
                w.gameGesture(72,5);var target=t<50?c.add(0,2.3,0):t<110?c.add(0,2.3,-4.5):PalaceWellMotion.ground(p.serverLevel(),w,c.add(0,0,-4.5),6);
                if(target!=null&&move(p,w,target,.065)&&t>=110&&arrived(w,target)){w.setOnGround(true);progress.climbedOut(s.owner,progress.session(),w.getUUID(),w.onGround());}
                var away=PalaceWellMotion.ground(p.serverLevel(),h,c.add(5,1,-5),8);if(away!=null)move(p,h,away,.16);
            }
            case BOSS->boss(p,s,n,progress,h,w,now);
            case DEFEATED->{
                h.frame(20,t,0);
                var rock=prop(p,s,"Rock",RescueProps.ROCK,h.position().add(0,.8,0));if(rock==null)return;
                if(w!=null&&!n.getBoolean("SmashRequested")){
                    face(w,h.position());var offset=w.position().subtract(h.position()).multiply(1,0,1).normalize();var approach=PalaceWellMotion.ground(p.serverLevel(),w,h.position().add(offset.scale(1.3)),5);
                    if(approach!=null)move(p,w,approach,.10);boolean near=w.distanceToSqr(h)<4;w.gameGesture(near?73:76,5);
                    if(near){var lifted=w.position().add(forward(w).scale(.7)).add(0,1.7,0);if(move(p,rock,lifted,.12)&&arrived(rock,lifted)&&t>=60)n.putBoolean("SmashRequested",true);}
                }else if(w!=null)w.gameGesture(73,5);
                if(n.getBoolean("SmashRequested")){rock.frame(1,t,0);move(p,rock,h.position().add(0,.1,0),.08);if(t>=60&&rock.getY()<=h.getY()+.25&&rock.getBoundingBox().intersects(h.getBoundingBox())){h.frame(21,0,0);h.setInvisible(true);progress.shattered(s.owner,progress.session(),h.getUUID(),true);}}
                else if(t%80==0)p.displayClientMessage(Component.literal("Use the rock to smash the defeated hand."),true);
            }
            case SHATTERED->{
                var bundle=prop(p,s,"Bundle",RescueProps.KEY_BUNDLE,h.position());if(bundle==null)return;
                root(s).putUUID("BundleEntity",bundle.getUUID());bundle.frame(0,t,0);if(w!=null)w.gameGesture(74,5);
                if(t>=60){var rock=RescueProps.find(p,s,"WellRock");if(rock!=null)rock.setInvisible(true);progress.wrapped(s.owner,progress.session(),true);}
            }
            case WRAPPED->{if(w!=null)w.gameGesture(74,5);if(t%80==0)p.displayClientMessage(Component.literal("Place the original Little Door key into the wrapped bundle."),true);}
            case KEY_BUNDLED->{
                var bundle=RescueProps.find(p,s,"WellBundle");if(bundle==null||!root(s).hasUUID("BundleEntity")||!root(s).getUUID("BundleEntity").equals(bundle.getUUID()))return;
                boolean heard=w==null||cue(p,s,n,'E');int cursor=w==null?t:ShowPlayback.position(s);int duration=w==null?60:ShowRecordings.duration("crawling_hand_e");
                if(w!=null){w.gameGesture(75,5);if(cursor>=Math.max(0,duration-40))n.putBoolean("ThrowRequested",true);}
                if(n.getBoolean("ThrowRequested")){
                    int leg=PalaceWellMotion.throwBundle(p.serverLevel(),bundle,c,n.getInt("ThrowStage"));
                    n.putInt("ThrowStage",leg);
                    ParentRescueStory.scenery(s).putLong("RescueWellBundlePosition",bundle.blockPosition().asLong());
                    bundle.frame(1,t,1);
                    if(heard&&leg==2&&PalaceKeyCustody.dispose(p,s,bundle))progress=state(s);
                }else if(w==null&&t%80==0)p.displayClientMessage(Component.literal("Throw the sealed bundle into the well."),true);
            }
            case DISPOSED->{ShowPlayback.stop(p,s);progress.beginFarewell(s.owner,progress.session());}
            case FAREWELL_F,FAREWELL_G,FAREWELL_H,FAREWELL_I->{
                char letter=progress.phase().name().charAt(9);boolean done=cue(p,s,n,letter);if(!ShowPlayback.started(s))return;w.gameGesture(letter=='I'?77:letter=='H'?76:74,5);face(w,letter=='I'?c:p.position());
                var held=w.position().add(forward(w).scale(.65)).add(0,1.1,0);var photo=prop(p,s,"Photo",RescueProps.PHOTO,held);if(photo==null)return;photo.setInvisible(letter>='H'&&ShowPlayback.position(s)>20);photo.setPos(held);photo.setYRot(w.getYRot()+180);
                progress.finishFarewell(s.owner,progress.session(),letter,done,ShowPlayback.started(s));
            }
            case DEPARTING->{
                var b=bike(p,s,n,progress,w);if(b==null)return;
                if(!n.contains("Departure")){var away=c.subtract(p.position()).multiply(1,0,1).normalize();if(away.lengthSqr()<.1)away=new Vec3(0,0,1);var target=PalaceWellMotion.ground(p.serverLevel(),w,c.add(away.scale(44)),18);if(target==null)return;n.putLong("Departure",BlockPos.containing(target).asLong());}
                if(w.getVehicle()!=b){var at=PalaceWellMotion.ground(p.serverLevel(),w,b.position().add(1.3,0,0),4);if(at!=null&&move(p,w,at,.10)&&w.distanceToSqr(b)<5)w.startRiding(b,true);}
                w.gameGesture(77,5);
                if(p.distanceToSqr(w)>32*32&&!PalaceWellMotion.seen(p,w)&&CanonicalRealWybie.park(p,s,w)){b.discard();progress.departed(s.owner,progress.session(),w.getUUID(),true);}
            }
            case COMPLETE->{}
        }
        handPosition(s,h);save(p,s,progress);
    }
    private static void boss(ServerPlayer p,VisitData.Scene s,CompoundTag n,PalaceWellProgress progress,CinematicPropEntity h,CharacterEntity w,long now){
        var c=center(p);
        if((progress.wybieLife()==PalaceWellProgress.WybieLife.LOST_AT_WELL||progress.wybieLife()==PalaceWellProgress.WybieLife.DEAD_AT_WELL)&&n.getBoolean("Falling")){
            var original=CanonicalRealWybie.find(p,s);if(original==null)return;
            if(move(p,original,c.add(0,-25,0),.30)&&original.getY()<=c.y-24){
                if(progress.wybieLife()==PalaceWellProgress.WybieLife.DEAD_AT_WELL){
                    // Preserve the historical death result; new saves use the
                    // explicit loss transition. No inferred resurrection here.
                    if(!CanonicalRealWybie.archiveDeath(original))return;
                }else if(!CanonicalRealWybie.archiveWellFall(p,s,progress,original))return;
                n.remove("Falling");
            }else return;
        }
        if(!n.contains("Combat"))n.put("Combat",PalaceHandCombat.start(s.owner,s.door,h.getUUID(),progress.session()).save());
        var fight=PalaceHandCombat.load(s.owner,s.door,h.getUUID(),n.getCompound("Combat"));if(!fight.blocker().isEmpty()){pause(p,s,n,fight.blocker());return;}
        if(!fight.session().equals(progress.session())){pause(p,s,n,"Saved combat session requires owner-bound recovery.");return;}
        if(fight.complete()){progress.defeated(s.owner,progress.session(),h.getUUID());return;}
        if(!fight.tick(s.owner,progress.session(),now,h.level()==p.level())){pause(p,s,n,"The active hand-combat clock could not advance safely.");return;}
        var beat=fight.beat();h.frame(30+fight.phase()*4+beat.ordinal(),fight.cursor(),fight.hits()/12F);
        var targetPlayer=fighters(p,s,h).stream().min(java.util.Comparator.comparingDouble(h::distanceToSqr)).orElse(null);
        if(targetPlayer==null){pause(p,s,n,"No active participant remains in the well arena.");com.coraline.multiplayer.StoryCombatSafety.restoreGroup(p,s);return;}
        var toward=targetPlayer.position().subtract(h.position()).multiply(1,0,1);if(toward.lengthSqr()>.01)h.setYRot((float)Math.toDegrees(Math.atan2(-toward.x,toward.z)));
        if(beat==PalaceHandCombat.Beat.TELEGRAPH){
            if(fight.cursor()==1){p.playNotifySound(SoundEvents.SPIDER_AMBIENT,SoundSource.HOSTILE,.6F,.7F);var target=new CompoundTag();target.putDouble("X",targetPlayer.getX());target.putDouble("Y",targetPlayer.getY());target.putDouble("Z",targetPlayer.getZ());n.put("Lunge",target);}
        }else if(beat==PalaceHandCombat.Beat.STRIKE){
            if(!n.contains("Lunge",Tag.TAG_COMPOUND)){pause(p,s,n,"The telegraphed attack target is missing; no unannounced attack performed.");return;}
            var aim=n.getCompound("Lunge");var target=new Vec3(aim.getDouble("X"),aim.getDouble("Y"),aim.getDouble("Z"));if(target.distanceToSqr(c)>48*48){pause(p,s,n,"Saved attack target lies outside the original well arena.");return;}
            boolean intercepted=false;var tong=RescueProps.find(p,s,"WellTongs");
            if(w!=null&&tong!=null&&fight.totalTicks()%300<60&&tong.getBoundingBox().intersects(h.getBoundingBox())&&w.hasLineOfSight(h)){intercepted=true;n.putInt("InterceptCycle",fight.cycle());n.putInt("InterceptPhase",fight.phase());}
            intercepted|=n.contains("InterceptCycle",Tag.TAG_INT)&&n.getInt("InterceptCycle")==fight.cycle()&&n.getInt("InterceptPhase")==fight.phase();
            var ground=PalaceWellMotion.ground(p.serverLevel(),h,target,3);
            if(ground!=null&&!intercepted){double hop=fight.phase()==3?Math.sin((fight.cursor()-32)/12.0*Math.PI)*.65:0;move(p,h,ground.add(0,hop,0),.24+fight.phase()*.045);}
            if(fight.cursor()==40&&!intercepted)for(var participant:fighters(p,s,h))if(participant.distanceToSqr(h)<2.5*2.5&&participant.hasLineOfSight(h)){participant.hurt(participant.damageSources().generic(),fight.phase()==3?5:3);var push=participant.position().subtract(h.position()).multiply(1,0,1).normalize().scale(.35);participant.setDeltaMovement(participant.getDeltaMovement().add(push).add(0,.15,0));participant.hurtMarked=true;}
        }else if(beat==PalaceHandCombat.Beat.RECOVERY){
            var floor=PalaceWellMotion.ground(p.serverLevel(),h,h.position(),2);if(floor!=null)move(p,h,floor,.08);
        }else if(beat==PalaceHandCombat.Beat.PURSUE){
            var direction=toward.normalize();if(toward.length()<2)direction=direction.scale(-1);
            if(fight.phase()==2)direction=new Vec3(direction.z*.8+direction.x*.3,0,-direction.x*.8+direction.z*.3).normalize();
            var target=PalaceWellMotion.ground(p.serverLevel(),h,h.position().add(direction.scale(.09+fight.phase()*.025)),2);
            if(target!=null&&target.subtract(c).multiply(1,0,1).lengthSqr()>16)move(p,h,target,.11+fight.phase()*.025);
        }
        if(w!=null){w.gameGesture(fight.totalTicks()%300<60?70:76,5);var safe=PalaceWellMotion.ground(p.serverLevel(),w,c.add(0,0,-5.5),7);if(safe!=null)move(p,w,safe,.07);face(w,h.position());
            var tong=RescueProps.find(p,s,"WellTongs");if(tong!=null){var forward=h.position().subtract(w.position()).multiply(1,0,1).normalize();tong.setInvisible(false);tong.setPos(w.position().add(forward.scale(.9)).add(0,.7,0));tong.setYRot(w.getYRot()+180);}}
        if(fight.totalTicks()%40==0)p.displayClientMessage(Component.literal("Crawling Hand — phase "+fight.phase()+"/3 — "+fight.hits()+" hits remaining. "+(beat==PalaceHandCombat.Beat.RECOVERY?"Strike now!":"Keep moving.")),true);
        n.put("Combat",fight.save());if(fight.complete())progress.defeated(s.owner,progress.session(),h.getUUID());
    }
    public static boolean interact(ServerPlayer p,CinematicPropEntity prop){
        var s=OtherMotherLoop.scene(p);var n=s==null?null:control(s);if(n==null||interactionDirector(p,s)==null||n.getBoolean("Paused")||!prop.owned(s)||p.distanceToSqr(prop)>9||!p.hasLineOfSight(prop))return false;
        var progress=state(s);
        var actualRock=RescueProps.find(p,s,"WellRock");
        if(prop.kind()==RescueProps.ROCK&&prop==actualRock&&progress.phase()==DEFEATED)n.putBoolean("SmashRequested",true);
        else if(p.getUUID().equals(s.owner)&&prop.kind()==RescueProps.KEY_BUNDLE&&root(s).hasUUID("BundleEntity")&&prop.getUUID().equals(root(s).getUUID("BundleEntity"))&&progress.phase()==KEY_BUNDLED&&progress.wybieLife()!=PalaceWellProgress.WybieLife.ALIVE)n.putBoolean("ThrowRequested",true);
        else return false;OtherMotherLoop.changed(p);return true;
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void attack(AttackEntityEvent event){
        if(!(event.getEntity() instanceof ServerPlayer p)||!(event.getTarget() instanceof CinematicPropEntity h)||h.kind()!=RescueProps.HAND)return;
        var s=OtherMotherLoop.scene(p);var n=s==null?null:control(s);var director=interactionDirector(p,s);if(n==null||director==null||n.getBoolean("Paused")||!h.owned(s)||!com.coraline.multiplayer.StoryCoop.canAct(p,s))return;var progress=state(s);
        if(!h.getUUID().equals(progress.originalHand()))return;event.setCanceled(true);long now=p.serverLevel().getGameTime();boolean reach=p.distanceToSqr(h)<=9,visible=p.hasLineOfSight(h);
        if(progress.phase()==RESCUE){if(progress.rescueHit(s.owner,progress.session(),h.getUUID(),now,true,reach,visible)){h.frame(12,0,0);save(director,s,progress);}}
        else if(progress.phase()==BOSS&&n.contains("Combat",Tag.TAG_COMPOUND)){
            var fight=PalaceHandCombat.load(s.owner,s.door,h.getUUID(),n.getCompound("Combat"));if(fight.hit(s.owner,progress.session(),h.getUUID(),now,true,reach,visible,p.getAttackStrengthScale(.5F)>=.85F)){
                n.put("Combat",fight.save());p.playNotifySound(SoundEvents.IRON_GOLEM_HURT,SoundSource.HOSTILE,.55F,1.3F);OtherMotherLoop.changed(p);
            }
        }
    }
    public static int resume(ServerPlayer p){
        var s=OtherMotherLoop.scene(p);var n=s==null?null:control(s);if(n==null||!eligible(p,s)||p.distanceToSqr(center(p))>32*32||!n.getBoolean("Paused"))return 0;var progress=state(s);
        if(!progress.resume(s.owner))return 0;var h=hand(p,s,progress);if(h==null)return 0;
        if(progress.wybieLife()==PalaceWellProgress.WybieLife.ALIVE&&wybie(p,s,progress)==null){p.sendSystemMessage(Component.literal("The original living Wybie has not loaded. His recorded chunk was requested; no replacement or resurrection was performed."));return 0;}
        if(n.contains("Combat")){var fight=PalaceHandCombat.load(s.owner,s.door,h.getUUID(),n.getCompound("Combat"));if(!fight.complete()&&!fight.resume(s.owner,progress.session()))return 0;n.put("Combat",fight.save());}
        n.remove("Paused");n.remove("Blocker");n.putLong("LastTick",-1);n.putBoolean("CueStarted",false);ShowPlayback.stop(p,s);save(p,s,progress);return 1;
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){if(event.player instanceof net.minecraft.server.level.ServerPlayer coopPlayer&&com.coraline.multiplayer.StoryCoop.directorBlocked(coopPlayer))return;
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer p))return;var s=OtherMotherLoop.scene(p);if(s==null)return;var r=root(s);if(r==null)return;
        if(!r.contains("Well")){if(p.tickCount%10==0&&eligible(p,s))start(p,s);return;}
        var n=control(s);if(n==null)return;try{tick(p,s,n);}catch(RuntimeException failure){pause(p,s,n,"Well scene paused safely: "+failure.getMessage());}
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p){if(com.coraline.multiplayer.StoryCoop.guest(p))return;var s=OtherMotherLoop.scene(p);var n=s==null?null:control(s);if(n!=null&&state(s).phase()!=COMPLETE)pause(p,s,n,"Well encounter paused for reconnect.");}}
    private PalaceWellEncounter(){}
}
