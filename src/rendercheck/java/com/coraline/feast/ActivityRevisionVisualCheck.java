package com.coraline.feast;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.Path;

/** Development smoke check for actual score audio/highway, stairs, held light and rotated beams. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class ActivityRevisionVisualCheck {
    private static VisitData.Scene scene;private static BlockPos center,stairEnd;private static int step,ticks,lastNote=-1,inputs,sounds;private static volatile boolean busy;private static boolean recording;
    private static void check(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    private static void capture(Minecraft mc,String name)throws Exception{try(var im=Screenshot.takeScreenshot(mc.getMainRenderTarget())){im.writeToFile(Path.of("../artwork/"+name));}}
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST) public static void sound(PlaySoundEvent e){if(!Boolean.getBoolean("coraline.activityRevisionVisualCheck")||!com.coraline.client.FestivalClient.active()||e.getSound()==null)return;var id=e.getSound().getLocation();if(id.getPath().startsWith("festival_harp"))sounds++;if(id.getNamespace().equals("minecraft")&&id.getPath().startsWith("music"))recording=true;}
    @SubscribeEvent public static void movement(MovementInputUpdateEvent e){if(!Boolean.getBoolean("coraline.activityRevisionVisualCheck")||step!=3)return;e.getEntity().getInventory().selected=(scene.loop.expedition.getInt("Slot")+1)%9;e.getEntity().setYRot(180);e.getEntity().setXRot(35);e.getInput().forwardImpulse=ticks<290?1:0;e.getInput().leftImpulse=0;e.getInput().jumping=false;e.getInput().shiftKeyDown=false;}
    private static void enter(Minecraft mc,OtherMotherActivities.Activity a)throws Exception{var method=RevisionVisualCheck.class.getDeclaredMethod("enter",Minecraft.class,OtherMotherActivities.Activity.class);method.setAccessible(true);method.invoke(null,mc,a);}
    private interface Work{void run()throws Exception;}
    private static void work(Minecraft mc,Work w){busy=true;mc.getSingleplayerServer().execute(()->{try{w.run();}catch(Throwable ex){com.mojang.logging.LogUtils.getLogger().error("CORALINE_ACTIVITY_REVISION_CHECK FAILED",ex);mc.execute(mc::stop);}finally{busy=false;}});}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("coraline.activityRevisionVisualCheck")||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.player==null||mc.getSingleplayerServer()==null||busy)return;mc.options.pauseOnLostFocus=false;ticks++;
        try{
            if(step==0&&ticks>=40){work(mc,()->{
                var setup=RevisionVisualCheck.class.getDeclaredMethod("setup",Minecraft.class);setup.setAccessible(true);setup.invoke(null,mc);var field=RevisionVisualCheck.class.getDeclaredField("s");field.setAccessible(true);scene=(VisitData.Scene)field.get(null);
                enter(mc,OtherMotherActivities.Activity.MUSIC_FESTIVAL);var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);center=MusicFestivalActivity.center(p,scene);scene.loop.festival.putInt("Phase",1);scene.loop.festival.putInt("Track",2);
                var console=LoopProps.nearby(p.serverLevel(),scene,center.north(24)).stream().filter(v->v.kind()==ScenePropEntity.FESTIVAL_CONSOLE).findFirst().orElseThrow();MusicFestivalActivity.interactProp(p,scene,console);step=1;ticks=0;
            });return;}
            if(step==1||step==2){
                int elapsed=(int)(mc.getSingleplayerServer().overworld().getGameTime()-scene.loop.festival.getLong("Start"));var track=FestivalChart.track(scene.loop.festival.getInt("Track"));
                for(int i=0;i<track.notes().size();i++){var n=track.notes().get(i);if(i>lastNote&&Math.abs(n.tick()-elapsed)<=1){lastNote=i;FestivalNetwork.press(scene.loop.generation,(++inputs%5==0)?(n.lane()+1)%4:n.lane());break;}}
                if(step==1&&ticks>=360){capture(mc,"revision35_music_early.png");work(mc,()->{
                    var live=MusicFestivalActivity.class.getDeclaredField("LIVE");live.setAccessible(true);var run=((java.util.Map<?,?>)live.get(null)).get(scene.owner);var resolved=run.getClass().getDeclaredField("resolved");resolved.setAccessible(true);var flags=(int[])resolved.get(run);int late=track.duration()*3/4;
                    for(int i=0;i<flags.length;i++)if(track.notes().get(i).tick()<late)flags[i]=3;
                    
                    scene.loop.festival.putLong("Start",mc.getSingleplayerServer().overworld().getGameTime()-late);lastNote=-1;for(int i=0;i<track.notes().size();i++)if(track.notes().get(i).tick()<late)lastNote=i;step=2;ticks=0;
                });}
                else if(step==2&&ticks>=240){check(sounds>=10&&inputs>=8&&!recording,"Missing performed/synthesized note-only audio: inputs="+inputs+" sounds="+sounds+" recording="+recording);capture(mc,"revision35_music_finale.png");work(mc,()->{
                    enter(mc,OtherMotherActivities.Activity.OTHER_MINESHAFT);var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);center=ExpeditionActivities.center(p,scene);scene.loop.expedition.putLong("NextSpawn",Long.MAX_VALUE);var edge=MineMaze.generate(scene.loop.expedition.getLong("Seed")).edges().stream().filter(v->v.a().tier()!=v.b().tier()).findFirst().orElseThrow();var start=center.offset(MineMaze.stairStart(edge));stairEnd=center.offset(MineMaze.stairEnd(edge));p.getInventory().selected=(scene.loop.expedition.getInt("Slot")+1)%9;p.connection.teleport(start.getX()+.5,start.getY(),start.getZ()+.5,180,35);step=3;ticks=0;
                });}
                return;
            }
            if(step==3&&ticks>=310){capture(mc,"revision35_mine_stairs_lantern.png");check(mc.player.getY()<=stairEnd.getY()+1.5&&mc.player.getY()>=stairEnd.getY()-.5,"Normal walking stuck on descending stairs: position="+mc.player.position()+" end="+stairEnd+" seed="+scene.loop.expedition.getLong("Seed")+" phase="+scene.loop.expedition.getInt("Phase")+" failure="+scene.loop.expedition.getString("Failure"));work(mc,()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);mc.execute(()->{mc.player.getInventory().selected=(scene.loop.expedition.getInt("Slot")+3)%9;mc.gameMode.useItem(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND);});step=4;ticks=0;});}
            else if(step==4&&ticks>=40){check(mc.screen instanceof com.coraline.client.MineMapScreen,"Mine map missing");capture(mc,"revision35_mine_map.png");mc.setScreen(null);work(mc,()->{
                enter(mc,OtherMotherActivities.Activity.OTHER_END_CITY);var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);center=EndCityActivity.center(p,scene);scene.loop.endCity.putLong("VoiceUntil",OtherMotherLoop.now(p));EndCityActivity.tick(p,scene,center);scene.loop.endCity.putInt("Room",4);scene.loop.endCity.putLong("InvulnerableUntil",Long.MAX_VALUE);scene.loop.endCity.putLong("RoomStarted",OtherMotherLoop.now(p));var at=EndCityCourse.room(center,4);p.connection.teleport(at.getX()+.5,at.getY(),at.getZ()+.5,0,75);step=5;ticks=0;
            });}
            else if(step==5){if(ticks==30)capture(mc,"revision35_laser_a.png");if(ticks>=70){capture(mc,"revision35_laser_b.png");com.mojang.logging.LogUtils.getLogger().info("CORALINE_ACTIVITY_REVISION_CHECK PASSED: {} inputs, {} note sounds, no original music; normal stair traversal and rendered mine map/rotated beams",inputs,sounds);mc.stop();}}
        }catch(Throwable ex){com.mojang.logging.LogUtils.getLogger().error("CORALINE_ACTIVITY_REVISION_CHECK FAILED",ex);mc.stop();}
    }
}
