package com.coraline.feast;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.Path;

/** Opt-in real-client smoke test; excluded from release builds. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class IntroFortressVisualCheck {
    private static int ticks,step,noteSounds;private static boolean recording;private static volatile boolean busy;private static VisitData.Scene scene;private static BlockPos center;
    private static void check(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    private static void capture(Minecraft mc,String name)throws Exception {try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../artwork/"+name));}}
    @SubscribeEvent public static void sound(net.minecraftforge.client.event.sound.PlaySoundEvent e){
        if(!Boolean.getBoolean("coraline.introFortressVisualCheck")||!com.coraline.client.FestivalClient.active()||e.getSound()==null)return;
        String path=e.getSound().getLocation().getPath();
        if(path.startsWith("block.note_block."))noteSounds++;
        if(path.startsWith("music/game/"))recording=true;
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("coraline.introFortressVisualCheck")||e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.getSingleplayerServer()==null||busy||++ticks%80!=0)return;
        mc.options.pauseOnLostFocus=false;
        try {
            if(step==1){
                check(mc.screen!=null,"Intro menu missing");
                capture(mc,"update_intro_menu.png");
                var button=mc.screen.children().stream().filter(v->v instanceof Button b&&b.getMessage().getString().equals("Listen to dialogue")).map(v->(Button)v).findFirst().orElseThrow();
                button.onPress();check(mc.screen==null,"Listen did not close menu");
            }
            if(step==2){
                check(mc.screen==null,"Listen reopened menu");
                var dad=mc.level.getEntitiesOfClass(com.coraline.parents.ParentEntity.class,mc.player.getBoundingBox().inflate(96),a->a.isOtherParent()&&!a.isMom()).get(0);
                var delta=dad.getEyePosition().subtract(mc.player.getEyePosition()).normalize();
                check(delta.dot(mc.player.getLookAngle())>.98,"Camera not facing father");
                capture(mc,"update_intro_listen.png");
            }
            if(step==4){check(mc.screen instanceof com.coraline.client.MineMapScreen,"Map not opened");capture(mc,"update_mine_map.png");mc.screen.onClose();}
            if(step==7){check(com.coraline.client.FestivalClient.active()&&noteSounds>0&&!recording,"Note-only festival audio failed");capture(mc,"update_festival_notes.png");}
            if(step==5){check(mc.screen instanceof com.coraline.client.MineMapScreen,"Deep map not opened");capture(mc,"update_mine_deep_map.png");mc.screen.onClose();}
        }catch(Throwable ex){com.mojang.logging.LogUtils.getLogger().error("CORALINE_UPDATE_CLIENT_CHECK FAILED",ex);mc.stop();return;}
        busy=true;
        mc.getSingleplayerServer().execute(()->{
            try{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                switch(step){
                    case 0->{
                        var setup=RevisionVisualCheck.class.getDeclaredMethod("setup",Minecraft.class);setup.setAccessible(true);setup.invoke(null,mc);
                        var field=RevisionVisualCheck.class.getDeclaredField("s");field.setAccessible(true);scene=(VisitData.Scene)field.get(null);
                        scene.loop.phase=OtherMotherLoopState.Phase.READY;scene.loop.activity=OtherMotherActivities.Activity.OTHER_MINESHAFT;scene.loop.generation++;
                        ActivityWorld.enter(p,scene);center=ExpeditionActivities.center(p,scene);
                        // Keep the real intro running while assertions and screenshots execute.
                    }
                    case 2->{check(ActivityIntro.deadline(scene.loop)>OtherMotherLoop.now(p),"Listen skipped the introduction");
                        ActivityIntro.skip(p,ActivityWorld.introSound(scene.loop.activity),scene.loop.generation);}
                    case 3->{check(scene.loop.expedition.getInt("Phase")==1,"Skip did not begin mine");
                        p.getInventory().selected=(scene.loop.expedition.getInt("Slot")+3)%9;
                        mc.execute(()->{mc.player.getInventory().selected=(scene.loop.expedition.getInt("Slot")+3)%9;mc.gameMode.useItem(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND);});}
                    case 4->{var at=MineMaze.generate(scene.loop.expedition.getLong("Seed")).cells().stream().filter(c->c.tier()==3&&c.col()==6&&c.row()==6).findFirst().orElseThrow().pos().offset(center);
                        p.connection.teleport(at.getX()+.5,at.getY(),at.getZ()+.5,45,0);MineshaftActivity.openMap(p);}
                    case 5->{var enter=RevisionVisualCheck.class.getDeclaredMethod("enter",Minecraft.class,OtherMotherActivities.Activity.class);enter.setAccessible(true);enter.invoke(null,mc,OtherMotherActivities.Activity.MUSIC_FESTIVAL);center=MusicFestivalActivity.center(p,scene);}
                    case 6->{scene.loop.festival.putInt("Phase",1);var console=LoopProps.nearby(p.serverLevel(),scene,center.north(24)).stream().filter(v->v.kind()==ScenePropEntity.FESTIVAL_CONSOLE).findFirst().orElseThrow();MusicFestivalActivity.interactProp(p,scene,console);
                        var note=FestivalChart.track(scene.loop.festival.getInt("Track")).notes().get(0);
                        scene.loop.festival.putLong("Start",OtherMotherLoop.now(p)-note.tick());
                        MusicFestivalActivity.press(p,scene.loop.generation,note.lane());}
                    case 7->{com.mojang.logging.LogUtils.getLogger().info("CORALINE_UPDATE_CLIENT_CHECK PASSED");mc.execute(mc::stop);}
                }step++;
            }catch(Throwable ex){com.mojang.logging.LogUtils.getLogger().error("CORALINE_UPDATE_CLIENT_CHECK FAILED",ex);mc.execute(mc::stop);}finally{busy=false;}
        });
    }
}
