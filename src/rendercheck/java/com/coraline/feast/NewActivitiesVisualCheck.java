package com.coraline.feast;

import com.coraline.otherworld.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.Path;

/** Development-only client smoke test, in an isolated copy of the QA world. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class NewActivitiesVisualCheck {
    private static volatile int phase;private static int frames;private static VisitData.Scene scene;private static BlockPos c;
    private static volatile long start;private static volatile int[] chart=new int[0];private static int played=-1;
    @SubscribeEvent public static void input(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("coraline.newActivitiesVisualCheck")||event.phase!=TickEvent.Phase.START||phase!=4)return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;for(var key:com.coraline.client.FestivalClient.KEYS)key.setDown(false);
        long elapsed=mc.level.getGameTime()-start;
        for(int i=played+1;i<chart.length;i++){if(chart[i]/4>elapsed)break;played=i;if(elapsed-chart[i]/4<3)com.coraline.client.FestivalClient.KEYS[chart[i]%4].setDown(true);}
    }
    @SubscribeEvent public static void render(RenderGuiEvent.Post event){
        if(!Boolean.getBoolean("coraline.newActivitiesVisualCheck"))return;var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
        mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(10);
        if(phase==0){phase=1;mc.getSingleplayerServer().execute(()->setup(mc.getSingleplayerServer()));return;}
        if(phase==2&&++frames>100){capture(mc,"festival_overview.png");frames=0;phase=3;mc.getSingleplayerServer().execute(()->beginFestival(mc.getSingleplayerServer()));}
        if(phase==4){
            mc.player.setYRot(0);mc.player.setXRot(5);if(++frames==2500)capture(mc,"festival_rhythm.png");
            if(frames%400==0)mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                if(scene.loop.festival.getInt("Phase")==3){if(!ActivityWorld.exitReady(scene))throw new IllegalStateException("Festival exit missing");phase=5;}
            });
        }
        if(phase==5){capture(mc,"festival_result.png");phase=6;frames=0;for(var key:com.coraline.client.FestivalClient.KEYS)key.setDown(false);mc.getSingleplayerServer().execute(()->temple(mc.getSingleplayerServer()));}
        if(phase==7&&++frames>90){capture(mc,"temple_preview.png");phase=8;frames=0;mc.getSingleplayerServer().execute(()->solveTemple(mc.getSingleplayerServer()));}
        if(phase==9&&++frames>90){capture(mc,"end_city_chamber.png");phase=10;frames=0;mc.getSingleplayerServer().execute(()->solveEnd(mc.getSingleplayerServer()));}
        if(phase==11&&++frames>90){capture(mc,"end_city_vault.png");System.out.println("CORALINE_NEW_ACTIVITIES_CLIENT_CHECK PASSED");phase=12;mc.stop();}
    }
    private static void capture(Minecraft mc,String name){try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../artwork/"+name));}catch(Exception ex){throw new RuntimeException(ex);}}
    private static void setup(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);p.setGameMode(GameType.CREATIVE);
        var door=new DoorData.Door();door.owner=p.getUUID();door.pos=new BlockPos(57000,80,57000);DoorData.get(server).doors.put(door.id,door);
        var realm=RealmData.get(server).realm(door);realm.ready=true;realm.parentGeneration=1;realm.porch=realm.doorPos;
        var other=server.getLevel(OtherWorld.KEY);for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)other.setBlock(realm.doorPos.offset(x,-1,z),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
        scene=new VisitData.Scene();scene.owner=p.getUUID();scene.door=door.id;scene.stage=VisitData.Stage.DONE;scene.table=realm.doorPos;scene.piano=realm.doorPos;scene.loop.portal=realm.doorPos;
        scene.loop.generation=1;scene.loop.phase=OtherMotherLoopState.Phase.READY;scene.loop.activity=OtherMotherActivities.Activity.MUSIC_FESTIVAL;
        VisitData.get(server).scenes.put(p.getUUID(),scene);p.getPersistentData().putUUID("CoralineVisitDoor",door.id);p.teleportTo(other,realm.doorPos.getX()+.5,80,realm.doorPos.getZ()+.5,0,0);
        ActivityWorld.enter(p,scene);c=MusicFestivalActivity.center(p,scene);p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(p.serverLevel(),c.getX()-18,c.getY()+12,c.getZ()-3,180,30);phase=2;
    }
    private static void beginFestival(MinecraftServer server){var p=server.getPlayerList().getPlayers().get(0);
        scene.loop.festival.putLong("VoiceUntil",OtherMotherLoop.now(p));MusicFestivalActivity.tick(p,scene,c);
        var console=LoopProps.nearby(p.serverLevel(),scene,c.north(24)).stream().filter(e->e.kind()==ScenePropEntity.FESTIVAL_CONSOLE).findFirst().orElseThrow();
        p.getAbilities().flying=false;p.onUpdateAbilities();ActivityScenery.teleport(p,MusicFestivalActivity.station(c));LoopProps.interact(p,console);
        if(scene.loop.festival.getInt("Phase")!=2)throw new IllegalStateException("Console did not start");start=scene.loop.festival.getLong("Start");chart=FestivalChart.pack(FestivalChart.track(scene.loop.festival.getInt("Track")).notes());phase=4;
    }
    private static void enter(MinecraftServer server,OtherMotherActivities.Activity activity){var p=server.getPlayerList().getPlayers().get(0);
        var previous=scene.loop.activity;scene.loop.activity=previous;ActivityWorld.exit(p,scene);
        scene.loop.phase=OtherMotherLoopState.Phase.READY;scene.loop.activity=activity;scene.loop.generation++;ActivityWorld.enter(p,scene);
    }
    private static void temple(MinecraftServer server){enter(server,OtherMotherActivities.Activity.OTHER_DESERT_TEMPLE);var p=server.getPlayerList().getPlayers().get(0);c=DesertTempleActivity.center(p,scene);
        scene.loop.temple.putLong("VoiceUntil",OtherMotherLoop.now(p));DesertTempleActivity.tick(p,scene,c);ActivityScenery.teleport(p,Vec3.atBottomCenterOf(c.offset(0,0,DesertTempleCourse.top(0)+9)));DesertTempleActivity.tick(p,scene,c);
        scene.loop.temple.putLong("Preview",OtherMotherLoop.now(p)-40);phase=7;
    }
    private static void solveTemple(MinecraftServer server){var p=server.getPlayerList().getPlayers().get(0);
        for(int room=0;room<5;room++){
            if(room>0){ActivityScenery.teleport(p,Vec3.atBottomCenterOf(c.offset(0,0,DesertTempleCourse.top(room)+9)));DesertTempleActivity.tick(p,scene,c);}
            scene.loop.temple.putLong("Preview",OtherMotherLoop.now(p)-200);DesertTempleActivity.tick(p,scene,c);
            for(var tile:TemplePath.generate(room,scene.loop.temple.getLong("Seed")+room*997)){ActivityScenery.teleport(p,Vec3.atBottomCenterOf(DesertTempleCourse.tile(c,room,tile)));DesertTempleActivity.tick(p,scene,c);if(scene.loop.temple.getInt("Phase")==5)throw new IllegalStateException("Valid route failed room "+room);}
        }
        if(!DesertTempleActivity.takePrize(scene,p.getUUID()))throw new IllegalStateException("Temple vault did not unlock");
        enter(server,OtherMotherActivities.Activity.OTHER_END_CITY);c=EndCityActivity.center(p,scene);scene.loop.endCity.putLong("VoiceUntil",OtherMotherLoop.now(p));EndCityActivity.tick(p,scene,c);phase=9;
    }
    private static void solveEnd(MinecraftServer server){var p=server.getPlayerList().getPlayers().get(0);
        for(int room=0;room<5;room++){
            int attempts=0;while(scene.loop.endCity.getInt("Phase")==1&&attempts++<30){scene.loop.endCity.putLong("NextEat",0);EndCityActivity.eat(p,p.getMainHandItem());}
            if(room<4){if(scene.loop.endCity.getInt("Phase")!=2)throw new IllegalStateException("End City softlock "+room);var b=EndCityCourse.room(c,room);ActivityScenery.teleport(p,Vec3.atBottomCenterOf(b.offset(21,2*(EndCityCourse.stages(room)-1),-35)));EndCityActivity.tick(p,scene,c);}
        }
        if(scene.loop.endCity.getInt("Phase")!=3)throw new IllegalStateException("Final End City vault missing");p.connection.teleport(c.getX()+.5,c.getY()+40,c.getZ()-266,180,0);phase=11;
    }
}
