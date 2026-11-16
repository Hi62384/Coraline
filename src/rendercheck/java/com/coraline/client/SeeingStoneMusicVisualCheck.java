package com.coraline.client;

import com.coraline.feast.CurrentStoryVisualCheck;
import com.coraline.feast.VisitAudio;
import com.coraline.otherworld.OtherWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** QA-only visible stone/GPU and actual full FirstEscape music lifetime check.
 * Native music packets, ordinary stop packets, next voice and two dimension
 * transfers. No ending/progression witness or subjective listening claim. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class SeeingStoneMusicVisualCheck {
    private static boolean requested,musicRequested,stoneDone,stopped,voiced,repeated,outbound,inbound;
    private static volatile boolean ready;private static volatile Throwable failure;
    private static volatile SoundInstance track;private static int starts,frames,skyFrames;private static long began,ended;
    private static boolean visitedOther,returned;
    @SubscribeEvent public static void sound(PlaySoundEvent event){
        if(Boolean.getBoolean("coraline.seeingStoneMusicCheck")&&event.getSound()!=null&&FirstEscapeMusic.owns(event.getSound())){starts++;track=event.getSound();}
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void server(Minecraft mc,Runnable action){mc.getSingleplayerServer().execute(()->{try{action.run();}catch(Throwable error){failure=error;}});}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("coraline.seeingStoneMusicCheck")||event.phase!=TickEvent.Phase.END)return;
        if(failure!=null)throw new IllegalStateException("Visible stone/music fixture failed",failure);
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||mc.player.tickCount<60)return;
        mc.options.pauseOnLostFocus=false;mc.options.hideGui=false;mc.options.renderDistance().set(4);
        if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)mc.setScreen(null);
        if(!requested){requested=true;server(mc,()->CurrentStoryVisualCheck.beginSeeingStoneOnly(mc));return;}
        if(!ready){if(mc.player.blockPosition().getX()<31000)return;ready=true;}
        if(++frames>2400)throw new AssertionError("Visible stone/music fixture exceeded two minutes");
        if(!musicRequested){musicRequested=true;server(mc,()->VisitAudio.play(mc.getSingleplayerServer().getPlayerList().getPlayers().get(0),FirstEscapeMusic.NAME,true));}
        if(!stoneDone&&CurrentStoryVisualCheck.tick(mc)){stoneDone=true;mc.player.setXRot(-55);skyFrames=1;}
        if(skyFrames>0&&++skyFrames==12){require(SeeingStoneClient.raised(),"Sky capture lost the visibly raised stone");shot(mc,"raised_stone_sky");mc.options.keyUse.setDown(false);mc.gameMode.releaseUsingItem(mc.player);mc.player.setXRot(0);}
        if(OtherWorld.isOther(mc.level))visitedOther=true;
        if(visitedOther&&mc.level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD))returned=true;
        var current=track;if(current==null){require(frames<200,"Actual FirstEscape music packet did not create a track");return;}
        require(starts==1&&!current.isLooping(),"FirstEscape music looped or restarted");
        boolean active=mc.getSoundManager().isActive(current);long now=System.nanoTime();
        if(began==0){if(active){began=now;System.out.println("QA_MUSIC_START file="+current.getSound().getLocation()+" pitch="+current.getPitch()+" source="+current.getSource());}else require(frames<200,"Music never became active in the real mixer");return;}
        double seconds=(now-began)/1_000_000_000.;double duration=VisitAudio.length(FirstEscapeMusic.NAME)/20.;
        if(!stopped&&seconds>2){stopped=true;server(mc,()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);VisitAudio.stopMusic(p,"");VisitAudio.stopMusic(p,FirstEscapeMusic.NAME);});}
        if(!voiced&&seconds>3){voiced=true;server(mc,()->VisitAudio.play(mc.getSingleplayerServer().getPlayerList().getPlayers().get(0),"missing_parents_cat_a",false));}
        if(!repeated&&seconds>4){repeated=true;server(mc,()->VisitAudio.play(mc.getSingleplayerServer().getPlayerList().getPlayers().get(0),FirstEscapeMusic.NAME,true));}
        if(stoneDone&&skyFrames>=12&&!outbound&&seconds>10){outbound=true;server(mc,()->transfer(mc,true));}
        if(visitedOther&&!inbound&&seconds>14){inbound=true;server(mc,()->transfer(mc,false));}
        if(!active){require(seconds+.25>=duration,"FirstEscape music cut short at "+seconds+" / "+duration+"s");if(ended==0){ended=now;System.out.println("FIRST_ESCAPE_MUSIC_NATIVE_EOF seconds="+seconds+" expected="+duration+" starts="+starts);}}
        if(ended!=0&&now-ended>3_000_000_000L){
            require(!active&&!FirstEscapeMusic.active()&&starts==1,"Music did not retire at natural EOF");
            require(stoneDone&&stopped&&voiced&&repeated&&visitedOther&&returned,"Required real GPU or music transition was not observed: stone="+stoneDone+" stops="+stopped+" voice="+voiced+" repeat="+repeated+" other="+visitedOther+" return="+returned);
            shot(mc,"normal_view_after_music");System.out.println("SEEING_STONE_MUSIC_NATIVE_COMPLETE: visible held-stone/mob GPU colors/wall depth/sky; one real FirstEscape stream survived stop packets, next voice and two dimension handoffs to full natural EOF without loop. No human listening verdict.");mc.stop();
        }
        require(seconds<duration+12,"Music failed to finish within its complete packaged duration");
    }
    private static void transfer(Minecraft mc,boolean other){
        var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var l=other?p.server.getLevel(OtherWorld.KEY):p.server.overworld();var at=new BlockPos(other?32000:31700,110,32000);
        if(other){
            // A real Other World visit needs a valid realm lease. Without it,
            // native safety immediately returns the fixture before a client
            // can observe the destination. This is an inert disposable realm,
            // not a completed chapter or canonical ending witness.
            var door=new com.coraline.otherworld.DoorData.Door();door.owner=p.getUUID();door.pos=p.blockPosition();door.activeUntil=Long.MAX_VALUE;door.placed=true;
            com.coraline.otherworld.DoorData.get(p.server).doors.put(door.id,door);
            var realm=new com.coraline.otherworld.RealmData.Realm();realm.door=door.id;realm.sourceDoor=at;realm.house=at;realm.doorPos=at;realm.porch=at;realm.bed=at;realm.table=at;realm.piano=at;realm.radius=128;realm.ready=realm.islandComplete=true;
            com.coraline.otherworld.RealmData.get(p.server).realms.put(door.id,realm);
            var scene=new com.coraline.feast.VisitData.Scene();scene.owner=p.getUUID();scene.door=door.id;scene.stage=com.coraline.feast.VisitData.Stage.DONE;
            com.coraline.feast.VisitData.get(p.server).scenes.put(p.getUUID(),scene);
            p.getPersistentData().putUUID("CoralineVisitDoor",door.id);p.getPersistentData().putBoolean("CoralineDebugVisitCompleted",true);
        }
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){l.getChunkAt(at.offset(x,0,z));l.setBlock(at.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);for(int y=0;y<3;y++)l.setBlock(at.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);}
        p.teleportTo(l,at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
    }
    private static void shot(Minecraft mc,String name){try{var path=java.nio.file.Path.of("../artwork/seeing_stone_music_qa");java.nio.file.Files.createDirectories(path);try(var pixels=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){pixels.writeToFile(path.resolve(name+".png"));}}catch(java.io.IOException error){throw new IllegalStateException(error);}}
    private SeeingStoneMusicVisualCheck(){}
}
