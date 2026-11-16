package com.coraline.multiplayer;

import java.util.*;
import com.coraline.feast.*;
import com.coraline.otherworld.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One native owner director, with separately authenticated physical guests. */
@Mod.EventBusSubscriber(modid="coraline")
public final class StoryCoop {
    public static boolean guest(ServerPlayer p){return StorySessionData.get(p.server).guest(p.getUUID());}
    public static boolean directorBlocked(ServerPlayer p){if(guest(p))return true;var f=StorySessionData.get(p.server).frame(p.getUUID());var scene=VisitData.get(p.server).scenes.get(p.getUUID());return f!=null&&f.paused()&&!f.archived()&&scene!=null&&scene.door.equals(f.door());}
    public static UUID owner(ServerPlayer p){return StorySessionData.get(p.server).owner(p.getUUID()).orElse(p.getUUID());}
    public static boolean member(ServerPlayer p,VisitData.Scene scene){
        if(scene==null||VisitData.get(p.server).scenes.get(scene.owner)!=scene)return false;if(p.getUUID().equals(scene.owner))return true;
        var data=StorySessionData.get(p.server);var f=data.frame(p.getUUID());return f!=null&&data.admittedOwner(p.getUUID()).filter(scene.owner::equals).isPresent()&&f.door().equals(scene.door);
    }
    public static boolean cinematic(ServerPlayer owner,VisitData.Scene scene){
        if(scene==null)return false;
        var preview=owner.getPersistentData();if(preview.hasUUID("CoralineCoopDebugDoor")&&scene.door.equals(preview.getUUID("CoralineCoopDebugDoor"))&&preview.getLong("CoralineCoopDebugUntil")>owner.server.overworld().getGameTime()&&preview.getLong("CoralineCoopDebugUntil")-owner.server.overworld().getGameTime()<=200)return true;
        if(StoryCombatSafety.active(scene))return false;
        if(GhostVictimDirector.active(scene)&&owner.isSpectator()&&!GhostEscapeProgress.bodyRestored(GhostVictimEntry.data(scene))&&GhostVictimProgress.phase(GhostVictimEntry.data(scene))!=GhostVictimProgress.Phase.ENTER_MIRROR)return false;
        if(ActivityWorld.isActivity(owner.level()))return switch(scene.loop.activity){
            case MAGIC_GARDEN -> scene.loop.flyingGardenStage<5;
            case MICE_CIRCUS -> owner.isPassenger();
            case OTHER_DESERT_TEMPLE -> scene.loop.temple.getInt("Phase")==2;
            case DREAM_PARKOUR -> scene.loop.parkourEndAt<=0&&OtherMotherLoop.now(owner)<scene.loop.parkourVoiceUntil;
            case GARDEN_MAZE -> OtherMotherLoop.now(owner)<scene.loop.gardenVoiceUntil;
            case ICE_BOAT_RACE -> scene.loop.iceStage<2;
            default -> owner.isPassenger()&&scene.loop.activity!=OtherMotherActivities.Activity.ARCHERY_CONTEST;
        };
        if(FirstEscapeStory.engaged(scene)&&FirstEscapeStory.phase(scene)!=FirstEscapeTimeline.Phase.ESCAPE_COMPLETE){String phase=FirstEscapeStory.phase(scene).name();if(Set.of("FIND_CANE","CANE_COLLECTED","MASK_REMOVAL","STITCH_REMOVAL","PUSH_WARDROBE","OPEN_LITTLE_DOOR","ESCAPE_TUNNEL","SHOW_DOOR","THROW_SHOE").contains(phase))return false;return true;}
        if(owner.isPassenger()||owner.getCamera()!=owner||owner.getPersistentData().getBoolean("CoralineInTunnel"))return true;
        if(AcceptedButtonEyes.active(scene)&&GhostEscapeProgress.bodyRestored(GhostVictimEntry.data(scene)))return false;
        if(AcceptedButtonEyes.active(scene))return switch(AcceptedButtonEyesProgress.phase(AcceptedButtonEyes.data(scene))){
            case BASEMENT_INVITATION,WAITING_TO_SIT,MEET_UPSTAIRS,ACTIVITY_INVITATION,ACTIVITY_RUNNING,ACTIVITY_REACTION -> false;
            default -> true;
        };
        if(BlackIsTraditional.active(scene))return true;
        if(scene.stage!=VisitData.Stage.DONE)return !Set.of(VisitData.Stage.FIND_FATHER,VisitData.Stage.DINNER_READY,VisitData.Stage.RETURN_TO_MOTHER,VisitData.Stage.SIT,VisitData.Stage.WAITING,VisitData.Stage.BED).contains(scene.stage);
        return false;
    }
    public static boolean locked(ServerPlayer p){if(!guest(p))return false;var data=StorySessionData.get(p.server);var f=data.frame(p.getUUID());if(f==null||f.paused()||f.archived()||!Set.of("ACTIVE","SPECTATOR").contains(f.state()))return false;var owner=p.server.getPlayerList().getPlayer(f.owner());return owner!=null&&cinematic(owner,data.scene(p));}
    public static boolean pair(ServerPlayer a,ServerPlayer b){if(a==b||a.serverLevel()!=b.serverLevel())return false;var data=StorySessionData.get(a.server);var x=data.frame(a.getUUID());var y=data.frame(b.getUUID());if(x==null||y==null||!x.id().equals(y.id())||x.paused()||x.archived()||!Set.of("ACTIVE","SPECTATOR").contains(x.state())||!Set.of("ACTIVE","SPECTATOR").contains(y.state()))return false;var owner=a.server.getPlayerList().getPlayer(x.owner());return owner!=null&&cinematic(owner,VisitData.get(a.server).scenes.get(x.owner()));}
    private static boolean activityBoundary(ServerPlayer owner,VisitData.Scene scene){
        if(AcceptedButtonEyes.active(scene)||ActivityWorld.groupFinished(scene)||owner.isSpectator())return false;
        var n=scene.loop.expedition;
        return switch(scene.loop.activity){
            case OTHER_NETHER_FORTRESS -> n.getInt("Phase")==1;
            case SNOWBALL_BATTLE -> n.getInt("Phase")==1;
            case OTHER_DESERT_TEMPLE -> scene.loop.temple.getInt("Phase")==2;
            case TREASURE_HUNT -> n.getInt("Phase")==1||n.getInt("Phase")==2;
            case SHOPPING_MALL,OTHER_WOODLAND_MANSION -> n.getInt("Phase")==1;
            case MICE_CIRCUS -> scene.loop.circusPhase==3;
            case MAGIC_SHOW -> scene.loop.magicStage==6;
            case MAGIC_GARDEN -> scene.loop.flyingGardenStage==5;
            case GRAND_FEAST -> !scene.loop.feastFinished&&scene.loop.feastEndAt>0;
            case GARDEN_MAZE -> !cinematic(owner,scene);
            default -> false;
        };
    }
    private static boolean safe(ServerPlayer owner,VisitData.Scene s){
        if(ActivityWorld.isActivity(owner.level()))return activityBoundary(owner,s);
        if(owner.isPassenger()||owner.getCamera()!=owner||cinematic(owner,s)||AcceptedButtonEyes.active(s)||ActivityWorld.isActivity(owner.level())||!OtherWorld.isOther(owner.level())||owner.getPersistentData().getBoolean("CoralineInTunnel"))return false;
        if(RescueChapter.started(s)&&Set.of(RescueChapter.Phase.GARDEN_SEARCH,RescueChapter.Phase.THEATRE_TRAVEL,RescueChapter.Phase.THEATRE_SEARCH,RescueChapter.Phase.ATTIC_TRAVEL,RescueChapter.Phase.ATTIC_SEARCH,RescueChapter.Phase.CIRCUS_TRAVEL,RescueChapter.Phase.HOUSE_SAFE).contains(RescueChapter.phase(s)))return true;
        if(s.stage!=VisitData.Stage.DONE)return Set.of(VisitData.Stage.FIND_FATHER,VisitData.Stage.RETURN_TO_MOTHER,VisitData.Stage.SIT,VisitData.Stage.BED).contains(s.stage);
        return Set.of(OtherMotherLoopState.Phase.EXPLORING,OtherMotherLoopState.Phase.READY,OtherMotherLoopState.Phase.ARRIVAL,OtherMotherLoopState.Phase.FREE_TIME).contains(s.loop.phase);
    }
    /** Native DoorBlockEntity and position are independently checked by request(). */
    public static boolean touchDoor(ServerPlayer p,DoorBlockEntity door){
        if(door.doorId==null)return false;var record=DoorData.get(p.server).doors.get(door.doorId);if(record==null)return false;var data=StorySessionData.get(p.server);
        if(guest(p)){if(!data.memberState(p.getUUID()).equals("PENDING")){data.requestReturn(p);clear(p);data.restore(p);}return true;}
        if(record.owner.equals(p.getUUID()))return false;
        var owner=p.server.getPlayerList().getPlayer(record.owner);var scene=VisitData.get(p.server).scenes.get(record.owner);
        if(owner==null||scene==null||!scene.door.equals(record.id)||!data.observe(owner,scene)||!data.request(p,owner,record.id)){p.displayClientMessage(Component.literal("The story owner must be inside and permit you to join. Finish any active personal scene first."),true);return true;}
        p.displayClientMessage(Component.literal("Join requested. You will enter at the next safe story checkpoint. Use /coraline session leave to cancel."),false);return true;
    }
    public static boolean director(ServerPlayer p){var scene=VisitData.get(p.server).scenes.get(p.getUUID());return !guest(p)&&scene!=null&&scene.owner.equals(p.getUUID());}
    public static boolean canAct(ServerPlayer p,VisitData.Scene scene){
        if(!member(p,scene)||!p.isAlive()||p.isSpectator()||locked(p))return false;
        var data=StorySessionData.get(p.server);var f=data.frame(p.getUUID());return f!=null&&data.mayAct(p,f.id(),f.phase());
    }
    public static void transferGroup(ServerPlayer owner,VisitData.Scene scene){
        if(!owner.getUUID().equals(scene.owner))return;var data=StorySessionData.get(owner.server);data.observe(owner,scene);
        for(var p:data.participants(owner.server,scene.owner,true))if(p!=owner){clear(p);p.getPersistentData().putUUID("CoralineVisitDoor",scene.door);p.getPersistentData().remove("CoralineInTunnel");moveNear(p,owner,false);}
    }
    private static boolean moveNear(ServerPlayer guest,ServerPlayer owner,boolean exact){
        BlockPos at=exact?owner.blockPosition():com.coraline.tycoon.TycoonWorld.landing(owner.serverLevel(),owner.blockPosition());if(at==null)return false;
        guest.getPersistentData().putBoolean("CoopInternalMove",true);
        try{guest.stopRiding();guest.setCamera(guest);guest.setForcedPose(null);guest.fallDistance=0;guest.setDeltaMovement(Vec3.ZERO);guest.teleportTo(owner.serverLevel(),exact?owner.getX():at.getX()+.5,exact?owner.getY():at.getY(),exact?owner.getZ():at.getZ()+.5,owner.getYRot(),owner.getXRot());return true;}
        finally{guest.getPersistentData().remove("CoopInternalMove");}
    }
    public static boolean proxySeat(ScenePropEntity seat){return seat.kind()==ScenePropEntity.CHAIR&&seat.visualSlot()>=10000;}
    public static boolean seatNeeded(ServerPlayer guest){if(!locked(guest))return false;var f=StorySessionData.get(guest.server).frame(guest.getUUID());var owner=f==null?null:guest.server.getPlayerList().getPlayer(f.owner());return owner!=null&&owner.isPassenger();}
    private static void seat(ServerPlayer guest,ServerPlayer owner,StorySessionData.Frame frame){
        ScenePropEntity proxy=guest.getVehicle() instanceof ScenePropEntity s&&proxySeat(s)?s:null;
        if(proxy==null){guest.getPersistentData().putBoolean("CoopInternalMove",true);try{guest.stopRiding();proxy=com.coraline.registry.ModEntities.SCENE_PROP.get().create(guest.serverLevel());if(proxy==null)return;proxy.kind(ScenePropEntity.CHAIR);proxy.cooperativeSeat(guest.getUUID(),frame.id());proxy.setPos(owner.getVehicle().position());if(!guest.serverLevel().addFreshEntity(proxy))return;guest.startRiding(proxy,true);}finally{guest.getPersistentData().remove("CoopInternalMove");}}
        proxy.setPos(owner.getVehicle().position());proxy.setYRot(owner.getVehicle().getYRot());guest.setPos(owner.position());
    }
    public static void clear(ServerPlayer guest){
        if(guest.getPersistentData().getBoolean("CoopCinematic")){guest.getPersistentData().putBoolean("CoopInternalMove",true);try{if(guest.getVehicle() instanceof ScenePropEntity proxy&&proxySeat(proxy)){guest.stopRiding();proxy.discard();}guest.setCamera(guest);guest.setForcedPose(null);guest.setDeltaMovement(Vec3.ZERO);guest.fallDistance=0;}finally{guest.getPersistentData().remove("CoopInternalMove");}guest.getPersistentData().remove("CoopCinematic");}
        StoryGroupNetwork.send(guest,null,false,List.of());
    }
    public static void tick(MinecraftServer server){
        var data=StorySessionData.get(server);
        for(var p:server.getPlayerList().getPlayers()){
            if(p.getPersistentData().contains("CoralineCoopGhostBody")){
                var current=data.scene(p);if(current==null||!data.guest(p.getUUID())||!member(p,current)||!GhostVictimDirector.active(current))GhostCooperation.restoreMode(p);
            }
            if(!data.guest(p.getUUID())){if(server.getTickCount()%10==0){var scene=VisitData.get(server).scenes.get(p.getUUID());var frame=data.frame(p.getUUID());if(frame==null)continue;boolean locked=data.hasGuests(p.getUUID())&&cinematic(p,scene);var ids=locked?data.participants(server,p.getUUID(),true).stream().map(ServerPlayer::getUUID).toList():List.<UUID>of();StoryGroupNetwork.send(p,frame,locked,ids);}continue;}
            var frame=data.frame(p.getUUID());if(frame==null)continue;
            if(frame.state().equals("RETURNING")){if(server.getTickCount()%10==0){clear(p);data.restore(p);}continue;}
            var owner=server.getPlayerList().getPlayer(frame.owner());var scene=VisitData.get(server).scenes.get(frame.owner());
            if(owner==null||scene==null||frame.paused()||frame.archived()||!scene.door.equals(frame.door())){data.requestReturn(p);clear(p);data.restore(p);continue;}
            if(frame.state().equals("PENDING")){
                if(safe(owner,scene)&&p.isAlive()&&!p.isPassenger()&&p.getCamera()==p&&data.activate(server,owner.getUUID(),p.getUUID(),frame.phase())){p.getPersistentData().putUUID("CoralineVisitDoor",frame.door());p.getPersistentData().remove("CoralineInTunnel");moveNear(p,owner,false);p.displayClientMessage(Component.literal("Joined "+owner.getGameProfile().getName()+"'s story."),false);}continue;
            }
            if(!data.admittedOwner(p.getUUID()).filter(owner.getUUID()::equals).isPresent()){data.requestReturn(p);clear(p);data.restore(p);continue;}
            GhostCooperation.tick(p,owner,scene);
            boolean locked=cinematic(owner,scene),was=p.getPersistentData().getBoolean("CoopCinematic");
            if(p.serverLevel()!=owner.serverLevel()){if(!moveNear(p,owner,locked))continue;}
            if(locked){
                if(!was){p.closeContainer();moveNear(p,owner,true);}p.getPersistentData().putBoolean("CoopCinematic",true);
                if(owner.isPassenger())seat(p,owner,frame);else{if(p.getVehicle() instanceof ScenePropEntity proxy&&proxySeat(proxy)){p.getPersistentData().putBoolean("CoopInternalMove",true);try{p.stopRiding();proxy.discard();}finally{p.getPersistentData().remove("CoopInternalMove");}}p.connection.teleport(owner.getX(),owner.getY(),owner.getZ(),owner.getYRot(),owner.getXRot());}
                p.setForcedPose(owner.getForcedPose());p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;p.setCamera(owner.getCamera());
            }else if(was){clear(p);moveNear(p,owner,false);}
            if(server.getTickCount()%10==0||locked!=was){var ids=locked?data.participants(server,owner.getUUID(),true).stream().map(ServerPlayer::getUUID).toList():List.<UUID>of();StoryGroupNetwork.send(p,frame,locked,ids);}
        }
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        var node=Commands.literal("session");
        for(boolean allow:new boolean[]{true,false})node.then(Commands.literal(allow?"allow":"kick").then(Commands.argument("player",UuidArgument.uuid()).executes(c->{var p=c.getSource().getPlayerOrException();var scene=VisitData.get(p.server).scenes.get(p.getUUID());var data=StorySessionData.get(p.server);return scene!=null&&data.observe(p,scene)&&data.permit(p,UuidArgument.getUuid(c,"player"),allow)?1:0;})));
        node.then(Commands.literal("handoff").then(Commands.argument("player",UuidArgument.uuid()).then(Commands.argument("item",com.mojang.brigadier.arguments.StringArgumentType.word()).suggests((c,b)->{for(String kind:List.of("cane","shoe","key","stone","eye_0","eye_1","eye_2"))b.suggest(kind);return b.buildFuture();}).executes(c->{var p=c.getSource().getPlayerOrException();return QuestItemRecovery.handoff(p,UuidArgument.getUuid(c,"player"),com.mojang.brigadier.arguments.StringArgumentType.getString(c,"item"))?1:0;}))));
        node.then(Commands.literal("leave").executes(c->{var p=c.getSource().getPlayerOrException();var data=StorySessionData.get(p.server);if(!data.requestReturn(p))return 0;clear(p);data.restore(p);return 1;}));
        node.then(Commands.literal("status").executes(c->{var p=c.getSource().getPlayerOrException();var f=StorySessionData.get(p.server).frame(p.getUUID());p.sendSystemMessage(Component.literal(f==null?"No story session.":"Story owner: "+f.owner()+" | "+f.state()+" | checkpoint "+f.revision()+(f.paused()?" | paused":"")));return 1;}));event.getDispatcher().register(Commands.literal("coraline").then(node));
    }
    private StoryCoop(){}
}
