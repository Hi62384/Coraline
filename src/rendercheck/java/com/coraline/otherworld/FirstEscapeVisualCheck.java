package com.coraline.otherworld;

import com.coraline.feast.*;
import com.coraline.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Disposable visual previews, deliberately not represented as an end-to-end playthrough. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class FirstEscapeVisualCheck {
    private static boolean started;private static volatile boolean ready;private static int ticks;private static BlockPos room,prison;private static boolean openShot,crawlShot,steamShot,ceilingCamera;private static int ceilingDelay;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("coraline.firstEscapeVisualCheck")||Boolean.getBoolean("coraline.acceptedGhostCheck")||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.getSingleplayerServer()==null||mc.player.tickCount<60)return;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(4);mc.options.hideGui=true;if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)mc.setScreen(null);
        if(Boolean.getBoolean("coraline.rescueFinaleResume")){if(!started){started=true;RescueFinaleVisualCheck.resume(mc);}RescueFinaleVisualCheck.tick(mc);return;}
        if(!started){started=true;mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);p.removeAllEffects();p.server.getPlayerList().op(p.getGameProfile());var l=p.server.getLevel(OtherWorld.KEY);var center=new BlockPos(28800,100,28800);var d=new DoorData.Door();d.owner=p.getUUID();d.placed=true;d.pos=center;d.activeUntil=Long.MAX_VALUE;DoorData.get(p.server).doors.put(d.id,d);
            var r=new RealmData.Realm();r.door=d.id;r.ready=true;r.islandComplete=true;r.house=center;r.radius=150;r.sourceDoor=d.pos;if(Boolean.getBoolean("coraline.rescueFinaleVisualCheck"))RescueFinaleParlor.build(l,center,d,r);else RosewoodHouse.build(l,center,d,r);RealmData.get(p.server).realms.put(d.id,r);RealmData.get(p.server).setDirty();DoorData.get(p.server).setDirty();
            var s=new VisitData.Scene();s.owner=p.getUUID();s.door=d.id;s.table=r.table;s.piano=r.piano;s.stage=VisitData.Stage.DONE;s.loop.event.putInt("Serial",71);s.loop.phase=OtherMotherLoopState.Phase.BLACK_IS_TRADITIONAL;VisitData.get(p.server).scenes.put(s.owner,s);p.getPersistentData().putUUID("CoralineVisitDoor",s.door);p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);p.teleportTo(l,center.getX(),center.getY()+1,center.getZ(),0,0);
            var m=ModEntities.MOM.get().create(l);m.makeOtherParent(0);m.getPersistentData().putUUID("RealmDoor",s.door);m.getPersistentData().putUUID("LoopOwner",s.owner);m.setPos(center.getCenter());l.addFreshEntity(m);s.mom=m.getUUID();
            var old=BlackIsTraditional.data(s);old.putBoolean("Started",true);old.putBoolean("Rejected",true);old.putBoolean("StudyRevealComplete",true);old.putString("Phase","STUDY_REVEAL_COMPLETE");if(!FirstEscapeStory.begin(p,s))throw new IllegalStateException("Preview fixture failed to begin continuation");FirstEscapeStory.debug(p,s,"phase_room_dark","");room=FirstEscapeScene.room(s);prison=FirstEscapeScene.prison(s);m.setPos(room.getCenter().add(3,0,3));ready=true;
        });return;}
        if(!ready)return;ticks++;
        boolean parlorOnly=Boolean.getBoolean("coraline.insectParlorVisualCheck");
        if(ticks==5)camera(mc,room.offset(6,1,-2),room.above());
        if(ticks==35)shot(mc,"dark_room");
        if(ticks==40)phase(mc,"room_transforming");
        if(ticks==60)mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);if(p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4),"time set day")<0)throw new AssertionError("Actual time/day command failed during parlor lighting transition");System.out.println("FIRST_ESCAPE_TIME_COMMAND_DAY_OK: real command during active rendered story");});
        if(ticks==245)shot(mc,"insect_parlor");
        if(ticks==250){phase(mc,"cocoa_e");actorCamera(mc,false);}
        if(ticks==280)shot(mc,"angry_mother");
        if(ticks==300&&!parlorOnly)phase(mc,"cocoa_f");
        if(ticks==370&&!parlorOnly)shot(mc,"evil_mother_morph");
        if(parlorOnly){
            if(ticks==400)phase(mc,"cocoa_f");
            if(ticks==460)shot(mc,"evil_mother_morph");
            if(ticks==545){actorCamera(mc,false);}
            if(ticks==565)shot(mc,"evil_mother_complete");
            if(ticks==590){com.mojang.logging.LogUtils.getLogger().info("INSECT_PARLOR_NATIVE_CAPTURED: five parlor previews; not a full story playthrough");if(Boolean.getBoolean("coraline.rescueFinaleVisualCheck"))RescueFinaleVisualCheck.stage(mc);else if(Boolean.getBoolean("coraline.rescueAtticVisualCheck"))stageAttic(mc);else mc.stop();}
            if(ticks>590&&Boolean.getBoolean("coraline.rescueAtticVisualCheck"))atticShots(mc);
            if(ticks>590&&Boolean.getBoolean("coraline.rescueFinaleVisualCheck"))RescueFinaleVisualCheck.tick(mc);
            return;
        }
        if(ticks==400){phase(mc,"mirror_c");camera(mc,prison.offset(0,1,-2),prison.south(2));}
        if(ticks==610)phase(mc,"mirror_d");
        if(ticks==680)shot(mc,"prison_ghosts");
        if(ticks==700){phase(mc,"escape_a");camera(mc,FirstEscapeScenePlaceholder(),room);}
        if(ticks==755){mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var s=OtherMotherLoop.scene(p);var n=FirstEscapeStory.data(s);n.putBoolean("Punched",true);n.putBoolean("MaskRemoved",true);FirstEscapeStory.debug(p,s,"phase_escape_b","");});camera(mc,room.offset(9,1,8),room.offset(9,1,6));}
        if(ticks==775)actorCamera(mc,true);
        if(ticks==810)shot(mc,"wybie_stitches");
        if(ticks==820){com.mojang.logging.LogUtils.getLogger().info("FIRST_ESCAPE_NATIVE_CAPTURED: six native story scene previews; not a full playthrough");mc.stop();}
    }
    private static BlockPos FirstEscapeScenePlaceholder(){return room.offset(9,1,6);}
    private static void stageAttic(Minecraft mc){mc.getSingleplayerServer().execute(()->{
        var player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var scene=OtherMotherLoop.scene(player);var old=FirstEscapeStory.data(scene);old.putBoolean("InternalTransfer",true);player.stopRiding();old.remove("InternalTransfer");player.setForcedPose(null);player.setPose(net.minecraft.world.entity.Pose.STANDING);old.putString("Phase","ESCAPE_COMPLETE");old.putBoolean("Complete",true);old.putBoolean("Grabbed",false);ShowPlayback.stop(player,scene);
        var parent=ParentRescueStory.data(scene);parent.putBoolean("Started",true);var scenery=new net.minecraft.nbt.CompoundTag();for(String key:java.util.List.of("House","LittleDoor","Prison"))scenery.putLong(key,old.getLong(key));parent.put("Scenery",scenery);
        if(!MouseCircusSite.prepareAttic(player,scene))throw new IllegalStateException("Actual attic fixture could not prepare its original tent");
        var tent=MouseCircusSite.tent(player,scene);var base=Vec3.atBottomCenterOf(tent.south(6));var chamber=base.add(5,0,-1);var front=base.subtract(chamber).normalize();var eye=chamber.add(front.scale(4)).add(front.z*2.3,.35,-front.x*2.3);var aim=chamber.add(0,1.1,0).subtract(eye.add(0,player.getEyeHeight(),0));player.teleportTo(player.serverLevel(),eye.x,eye.y,eye.z,(float)Math.toDegrees(Math.atan2(-aim.x,aim.z)),(float)-Math.toDegrees(Math.atan2(aim.y,aim.horizontalDistance())));
        RescueChapter.data(scene).putBoolean("Started",true);RescueChapter.go(player,scene,RescueChapter.Phase.BOB_C);
    });}
    private static void atticShots(Minecraft mc){String phase=com.coraline.client.RescueClient.phase();int time=com.coraline.client.RescueClient.time();
        if(phase.equals("FURNACE")){if(time>=4&&!openShot){shot(mc,"furnace_open");openShot=true;}if(time>=24&&!crawlShot){shot(mc,"furnace_crawl");crawlShot=true;}if(time>=42&&!steamShot){shot(mc,"furnace_steam");steamShot=true;}}
        if(phase.equals("BOB_D")&&time>=165){if(!ceilingCamera){ceilingCamera=true;mc.getSingleplayerServer().execute(()->{var player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var scene=OtherMotherLoop.scene(player);var base=MouseCircusSite.tent(player,scene).south(6);var n=RescueChapter.data(scene);camera(mc,base.offset(0,1,5),n.contains("BobRoofAnchor")?BlockPos.of(n.getLong("BobRoofAnchor")).below(3):base.above(4));});}else if(++ceilingDelay>=15){if(!openShot||!crawlShot||!steamShot)throw new IllegalStateException("An actual furnace stage was skipped in the native capture");shot(mc,"bobinsky_ceiling");System.out.println("RESCUE_ATTIC_NATIVE_CAPTURED actual authored attic; open/crawl/steam/ceiling stages; NOT full chapter playthrough");mc.stop();}}
    }
    private static void actorCamera(Minecraft mc,boolean wybie){mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var s=OtherMotherLoop.scene(p);var actor=wybie?ShowActors.actor(p,s,"other_wybie"):p.serverLevel().getEntity(s.mom);if(actor==null)return;for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL){var point=actor.position().add(direction.getStepX()*2.7,1,direction.getStepZ()*2.7);var eye=point.add(0,p.getEyeHeight(),0);if(p.serverLevel().clip(new net.minecraft.world.level.ClipContext(eye,actor.getEyePosition(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p)).getType()!=net.minecraft.world.phys.HitResult.Type.MISS)continue;var delta=actor.getEyePosition().subtract(eye);p.teleportTo(p.serverLevel(),point.x,point.y,point.z,(float)Math.toDegrees(Math.atan2(-delta.x,delta.z)),(float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance())));break;}});}
    private static void phase(Minecraft mc,String phase){mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);FirstEscapeStory.debug(p,OtherMotherLoop.scene(p),"phase_"+phase,"");});}
    private static void camera(Minecraft mc,BlockPos at,BlockPos target){mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var pos=Vec3.atCenterOf(at);var delta=target.getCenter().subtract(pos.add(0,p.getEyeHeight(),0));p.teleportTo(p.serverLevel(),pos.x,pos.y,pos.z,(float)Math.toDegrees(Math.atan2(-delta.x,delta.z)),(float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance())));});}
    private static void shot(Minecraft mc,String name){try{var folder=java.nio.file.Path.of("../artwork/first_escape_qa");java.nio.file.Files.createDirectories(folder);try(var image=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(folder.resolve(name+".png"));}}catch(Exception e){throw new RuntimeException(e);}}
}
