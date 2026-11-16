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

/** Disposable native house / seated piano / dinner furniture and navigation QA. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class VisitEventsVisualCheck {
    private static boolean started;private static volatile boolean ready;private static int ticks;private static BlockPos piano,table;
    private static final BlockPos CENTER=new BlockPos(28400,100,28400);
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(Boolean.getBoolean("coraline.firstEscapeVisualCheck")||Boolean.getBoolean("coraline.missingParentsTeaVisualCheck"))return;
        if(!Boolean.getBoolean("coraline.visitEventsVisualCheck")||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;if(mc.player==null||mc.getSingleplayerServer()==null)return;if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)mc.setScreen(null);if(mc.player.tickCount<60)return;
        mc.options.pauseOnLostFocus=false;mc.options.hideGui=false;mc.options.renderDistance().set(4);if(mc.screen!=null&&!com.coraline.client.VisitEventClient.dialogueOpen()&&!(mc.screen instanceof com.coraline.client.TabletopGameScreen))mc.setScreen(null);
        if(!started){started=true;mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var level=p.server.getLevel(OtherWorld.KEY);var door=new DoorData.Door();door.owner=p.getUUID();door.placed=true;door.pos=CENTER;door.activeUntil=Long.MAX_VALUE;DoorData.get(p.server).doors.put(door.id,door);
            var realm=new RealmData.Realm();realm.door=door.id;realm.ready=true;realm.islandComplete=true;realm.house=CENTER;realm.radius=150;realm.sourceDoor=door.pos;RosewoodHouse.build(level,CENTER,door,realm);RealmData.get(p.server).realms.put(door.id,realm);piano=realm.piano;table=realm.table;
            var s=new VisitData.Scene();s.owner=p.getUUID();s.door=door.id;s.table=table;s.piano=piano;s.stage=VisitData.Stage.DONE;s.loop.visits=2;s.loop.secrets.visit=2;s.wybieVisited=s.bobinskyVisited=s.teaVisited=true;s.neighborVisitsReady=false;
            var mother=ModEntities.MOM.get().create(level);mother.makeOtherParent(0);mother.getPersistentData().putUUID("RealmDoor",door.id);mother.getPersistentData().putUUID("LoopOwner",p.getUUID());mother.setPos(Vec3.atBottomCenterOf(RosewoodHouse.householdStops(CENTER).get(0)));level.addFreshEntity(mother);s.mom=mother.getUUID();VisitData.get(p.server).scenes.put(p.getUUID(),s);
            p.getInventory().clearContent();for(String key:java.util.List.of("CoralineInTunnel","CoralineWhiteoutStart","CoralineWhiteoutMoved","CoralineWhiteoutAnchor"))p.getPersistentData().remove(key);p.getPersistentData().putUUID("CoralineVisitDoor",s.door);p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);p.teleportTo(level,piano.getX()+3.5,piano.getY()+1,piano.getZ()-4,40,10);
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,10000,0,false,false));VisitEvents.start(p,s,VisitEvents.Type.EXPLORATION_30_MIN);s.loop.phase=OtherMotherLoopState.Phase.FREE_TIME;ready=true;
        });}
        if(!ready)return;ticks++;
        if(ticks==100)camera(mc,piano.offset(4,2,-4),piano);
        if(ticks==180){if(!com.coraline.client.VisitEventClient.pianoIsSpatial(piano))throw new IllegalStateException("Local spatial piano did not become active");shot(mc,"study");}
        if(ticks==190)mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var s=OtherMotherLoop.scene(p);if(!(p.serverLevel().getEntity(s.dad) instanceof com.coraline.parents.ParentEntity father)||!VisitEvents.interact(p,father))throw new IllegalStateException("Native study conversation failed");});
        if(ticks==220){if(!com.coraline.client.VisitEventClient.dialogueOpen())throw new IllegalStateException("Owner conversation panel did not open");shot(mc,"conversation");}
        if(ticks==230&&mc.screen!=null)mc.screen.onClose();
        if(ticks==250)mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var s=OtherMotherLoop.scene(p);VisitEvents.start(p,s,VisitEvents.Type.FAMILY_DINNER);FamilyDinner.beginWindow(p,s);});
        if(ticks==280)camera(mc,table.offset(5,3,-7),table);
        if(ticks==720){shot(mc,"dinner");mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var s=OtherMotherLoop.scene(p);var mother=p.serverLevel().getEntity(s.mom);var father=p.serverLevel().getEntity(s.dad);if(mother==null||father==null||mother.getVehicle()==null||father.getVehicle()==null)throw new IllegalStateException("Dinner parents failed to reach seats: phase="+s.loop.phase+" mother="+(mother==null?"missing":mother.position()+" / "+mother.getPersistentData())+" father="+(father==null?"missing":father.position()+" / "+father.getPersistentData()));com.mojang.logging.LogUtils.getLogger().info("VISIT_EVENTS_NATIVE_OK: piano pose, local music and dinner parent seats captured");});}
        if(ticks==740)mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var s=OtherMotherLoop.scene(p);var chairs=p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new net.minecraft.world.phys.AABB(table).inflate(10),e->p.getUUID().equals(e.owner)&&e.kind()==ScenePropEntity.CHAIR&&e.slot==0&&e.getPersistentData().contains("VisitEventSerial"));if(chairs.size()!=1)throw new IllegalStateException("Duplicate player dinner chairs");var chair=chairs.get(0);p.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);p.teleportTo(p.serverLevel(),chair.getX(),chair.getY(),chair.getZ()-1,chair.getYRot(),0);if(!FamilyDinner.arrive(p,s,chair))throw new IllegalStateException("Native owner cannot take dinner seat");s.loop.phase=OtherMotherLoopState.Phase.DINNER;s.loop.event.putInt("MealElapsed",0);});
        if(ticks==840)shot(mc,"seated");
        if(ticks==850)mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var s=OtherMotherLoop.scene(p);for(var plate:p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new net.minecraft.world.phys.AABB(table).inflate(10),e->p.getUUID().equals(e.owner)&&e.kind()==ScenePropEntity.PLATE&&e.slot==4)){for(int i=0;i<3;i++){s.loop.event.putLong("BiteAfter",0);FamilyDinner.interact(p,plate);}}});
        if(ticks==920)shot(mc,"eating");
        if(ticks==960)mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);p.server.getPlayerList().op(p.getGameProfile());var s=OtherMotherLoop.scene(p);VisitEvents.start(p,s,VisitEvents.Type.FAMILY_GAME_NIGHT);FamilyGameNight.beginWindow(p,s);});
        for(int i=0;i<TabletopGames.Game.values().length;i++){
            final int gameIndex=i;int base=1000+i*130;
            if(ticks==base)mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var s=OtherMotherLoop.scene(p);p.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);p.stopRiding();
                var chair=p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new net.minecraft.world.phys.AABB(table).inflate(12),e->p.getUUID().equals(e.owner)&&e.kind()==ScenePropEntity.CHAIR&&e.slot==0&&e.getPersistentData().contains("VisitEventSerial")).get(0);
                p.teleportTo(p.serverLevel(),chair.getX(),chair.getY(),chair.getZ(),chair.getYRot(),0);p.startRiding(chair,true);
                FamilyGameNight.debug(p,"game,"+TabletopGames.Game.values()[gameIndex].name());FamilyGameNight.debug(p,"active");
            });
            if(ticks==base+40){if(!(mc.screen instanceof com.coraline.client.TabletopGameScreen))throw new IllegalStateException("Native tabletop view missing for "+TabletopGames.Game.values()[i]);shot(mc,"game_"+TabletopGames.voice(TabletopGames.Game.values()[i])+"_ui");}
            if(ticks==base+50){if(mc.screen!=null)mc.screen.onClose();mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);p.stopRiding();p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);});camera(mc,table.offset(4,3,-5),table);}
            if(ticks==base+110)shot(mc,"game_"+TabletopGames.voice(TabletopGames.Game.values()[i])+"_table");
        }
        if(ticks==1800)mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var s=OtherMotherLoop.scene(p);p.stopRiding();s.loop.trust=1;BlackIsTraditional.begin(p,s);BlackIsTraditional.debug(p,s,"sit","");BlackIsTraditional.debug(p,s,"gift","");});
        if(ticks==1840){mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);p.stopRiding();p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);});camera(mc,table.offset(2,2,-4),table.offset(0,1,-1));}
        if(ticks==1900)shot(mc,"traditional_open_gift");
        if(ticks==1920)mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var s=OtherMotherLoop.scene(p);var father=p.serverLevel().getEntity(s.dad);father.stopRiding();father.setPos(Vec3.atBottomCenterOf(piano).add(0,0,-1));father.setYRot(180);BlackIsTraditional.debug(p,s,"phase_q","");});
        if(ticks==1930)camera(mc,piano.offset(2,2,-4),piano.above());
        if(ticks==1970)shot(mc,"traditional_mechanical_hand");
        if(ticks==2010)mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);BlackIsTraditional.debug(p,OtherMotherLoop.scene(p),"phase_r","");});
        if(ticks==2090)shot(mc,"traditional_stretched_face");
        if(ticks==2120)mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);BlackIsTraditional.debug(p,OtherMotherLoop.scene(p),"phase_s","");});
        if(ticks==2170)shot(mc,"traditional_face_cover");
        if(ticks==2200){com.mojang.logging.LogUtils.getLogger().info("FAMILY_GAMES_NATIVE_CAPTURED: six tabletop games and traditional story preview captures");mc.stop();}
    }
    private static void camera(Minecraft mc,BlockPos at,BlockPos target){var pos=Vec3.atCenterOf(at);var delta=target.getCenter().subtract(pos.add(0,mc.player.getEyeHeight(),0));float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z)),pitch=(float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance()));mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);p.teleportTo(p.serverLevel(),pos.x,pos.y,pos.z,yaw,pitch);});}
    private static void shot(Minecraft mc,String name){try{var out=java.nio.file.Path.of("../artwork/visit_events_qa");java.nio.file.Files.createDirectories(out);try(var image=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(out.resolve(name+".png"));}}catch(Exception e){throw new RuntimeException(e);}}
}
