package com.coraline.feast;
import com.coraline.otherworld.*;
import com.coraline.registry.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import static com.coraline.feast.VisitData.Stage.*;

/** Opt-in disposable-world client check; excluded from release builds. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class FirstVisitVisualCheck {
    private static boolean requested;private static volatile int phase;private static int frames;private static VisitData.Scene scene;
    private static double previousMusicVolume;
    private static int farewellTicks;
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("coraline.feastVisualCheck")||e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null)return;
        mc.options.hideGui=false;mc.options.pauseOnLostFocus=false;
        if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)mc.setScreen(null);
        if(phase==4&&++farewellTicks==20){
            try{
                var field=com.coraline.client.VisitAudioClient.class.getDeclaredField("farewell");field.setAccessible(true);
                var sound=(net.minecraft.client.resources.sounds.SoundInstance)field.get(null);
                require(OtherWorld.isOther(mc.level)&&sound!=null&&mc.getSoundManager().isActive(sound),"Farewell not playing during sleep");
                com.mojang.logging.LogUtils.getLogger().info("CORALINE_FAREWELL_SLEEP_AUDIO_OK");
            }catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
        }
        if(phase==4&&farewellTicks>=100){require(!OtherWorld.isOther(mc.level),"Sleeping player did not return home after farewell");com.mojang.logging.LogUtils.getLogger().info("CORALINE_FAREWELL_RETURN_OK");phase=0;mc.getSingleplayerServer().execute(()->skinLineup(mc.getSingleplayerServer()));}
        if(!requested){requested=true;verifyMusicGuard();previousMusicVolume=mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MUSIC);mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MUSIC).set(0.0);mc.getSingleplayerServer().execute(()->prepare(mc.getSingleplayerServer()));}
    }
    private static void skinLineup(net.minecraft.server.MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);var level=server.overworld();var origin=new BlockPos(32200,100,32200);
        level.setDayTime(6000);
        for(int x=-8;x<=8;x++)for(int z=-4;z<=12;z++)level.setBlock(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
        for(int row=0;row<2;row++)for(int i=0;i<8;i++){
            var actor=(row==0?ModEntities.MOM:ModEntities.DAD).get().create(level);
            var tag=new net.minecraft.nbt.CompoundTag();actor.saveWithoutId(tag);tag.putInt("Skin",i);actor.load(tag);
            actor.setNoAi(true);actor.setNoGravity(true);actor.setPos(origin.getX()-5.25+i*1.5,origin.getY()+row*2.5,origin.getZ());actor.setYRot(0);actor.setYBodyRot(0);actor.setYHeadRot(0);level.addFreshEntity(actor);
        }
        p.teleportTo(level,origin.getX(),origin.getY()+1.9,origin.getZ()+12,180,0);phase=5;
    }
    private static void verifyMusicGuard(){
        var mc=Minecraft.getInstance();var manager=mc.getMusicManager();manager.startPlaying(net.minecraft.sounds.Musics.GAME);
        var story=net.minecraft.client.resources.sounds.SimpleSoundInstance.forMusic(com.coraline.feast.VisitAudio.SOUNDS.get("other_father_song").get());
        var storyEvent=new net.minecraftforge.client.event.sound.PlaySoundEvent(null,story);com.coraline.client.StoryMusicGuard.onSound(storyEvent);
        require(storyEvent.getSound()==story,"Story music was muted");require(!manager.isPlayingMusic(net.minecraft.sounds.Musics.GAME),"Existing vanilla music was not stopped");
        var positional=new net.minecraft.client.resources.sounds.SimpleSoundInstance(ModSounds.MICE_INTRO.get(),net.minecraft.sounds.SoundSource.RECORDS,.7F,1F,net.minecraft.util.RandomSource.create(),123,80,-456);
        var attached=new net.minecraftforge.client.event.sound.PlaySoundEvent(null,positional);com.coraline.client.StoryMusicGuard.onSound(attached);
        require(attached.getSound().isRelative()&&attached.getSound().getAttenuation()==net.minecraft.client.resources.sounds.SoundInstance.Attenuation.NONE,"Scene music still attenuates with player movement");
        require(attached.getSound().getX()==0&&attached.getSound().getY()==0&&attached.getSound().getZ()==0,"Scene music is not centered on listener");
        attached.getSound().resolve(mc.getSoundManager());require(attached.getSound().getVolume()==.7F,"Scene volume setting was changed");
        var squeak=new net.minecraft.client.resources.sounds.SimpleSoundInstance(ModSounds.MOUSE_SQUEAK.get(),net.minecraft.sounds.SoundSource.NEUTRAL,1,1,net.minecraft.util.RandomSource.create(),123,80,-456);
        var local=new net.minecraftforge.client.event.sound.PlaySoundEvent(null,squeak);com.coraline.client.StoryMusicGuard.onSound(local);require(local.getSound()==squeak,"Entity squeaks must remain positional");
        var vanilla=net.minecraft.client.resources.sounds.SimpleSoundInstance.forMusic(net.minecraft.sounds.SoundEvents.MUSIC_GAME.value());
        var blocked=new net.minecraftforge.client.event.sound.PlaySoundEvent(null,vanilla);com.coraline.client.StoryMusicGuard.onSound(blocked);require(blocked.getSound()==null,"Vanilla music restarted during story audio");
        // This synthetic recording is deliberately never played; after queue grace expires music must recover.
        for(int i=0;i<42;i++)com.coraline.client.StoryMusicGuard.tick(new net.minecraftforge.event.TickEvent.ClientTickEvent(net.minecraftforge.event.TickEvent.Phase.START));
        var restored=new net.minecraftforge.client.event.sound.PlaySoundEvent(null,vanilla);com.coraline.client.StoryMusicGuard.onSound(restored);require(restored.getSound()==vanilla,"Music suppression remained stuck after a clip ended");
        com.mojang.logging.LogUtils.getLogger().info("CORALINE_STORY_MUSIC_GUARD_OK");
    }
    private static void prepare(net.minecraft.server.MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        VisitData.get(server).scenes.remove(p.getUUID());
        var d=new DoorData.Door();d.owner=p.getUUID();d.pos=new BlockPos(32000,85,32000);d.placed=true;d.dimension="minecraft:overworld";DoorData.get(server).doors.put(d.id,d);
        var r=RealmData.get(server).realm(d);r.ready=true;r.radius=64;r.bed=r.doorPos.offset(4,0,4);
        var source=server.overworld();source.setBlock(d.pos.offset(4,-1,4),Blocks.STONE.defaultBlockState(),2);source.setBlock(d.pos.offset(4,0,4),Blocks.RED_BED.defaultBlockState(),2);p.setRespawnPosition(source.dimension(),d.pos.offset(4,0,4),0,true,false);
        var target=server.getLevel(OtherWorld.KEY);
        for(int x=-55;x<=55;x++)for(int z=-55;z<=55;z++){
            var pos=r.doorPos.offset(x,-1,z);target.setBlock(pos,Blocks.GRASS_BLOCK.defaultBlockState(),2);
            for(int y=1;y<=4;y++)target.setBlock(pos.above(y),Blocks.AIR.defaultBlockState(),2);
        }
        target.setBlock(r.bed,Blocks.RED_BED.defaultBlockState(),2);
        target.setBlock(r.doorPos,ModBlocks.TINY_DOOR.get().defaultBlockState(),2);((DoorBlockEntity)target.getBlockEntity(r.doorPos)).doorId=d.id;
        var family=new com.coraline.parents.FamilyData.Family();family.owner=p.getUUID();
        for(boolean mom:new boolean[]{true,false}){
            var parent=(mom?ModEntities.MOM:ModEntities.DAD).get().create(target);parent.makeOtherParent(mom?4:6);parent.setPos(r.doorPos.getX()+(mom?2:4),r.doorPos.getY(),r.doorPos.getZ()+1);
            parent.getPersistentData().putUUID("RealmDoor",d.id);parent.getPersistentData().putInt("RealmGeneration",r.parentGeneration);target.addFreshEntity(parent);if(mom)family.mom=parent.getUUID();else family.dad=parent.getUUID();
        }
        com.coraline.parents.FamilyData.get(server).families.put(family.id,family);
        p.getPersistentData().putUUID("CoralineVisitDoor",d.id);p.getPersistentData().remove("CoralineInTunnel");p.teleportTo(target,r.doorPos.getX()+.5,r.doorPos.getY(),r.doorPos.getZ()-3,0,0);
        FirstVisit.entered(p,r);scene=VisitData.get(server).scenes.get(p.getUUID());if(scene==null){Minecraft.getInstance().execute(Minecraft.getInstance()::stop);throw new IllegalStateException("First visit did not initialize on valid clear ground; parents="+target.getEntitiesOfClass(com.coraline.parents.ParentEntity.class,new net.minecraft.world.phys.AABB(r.doorPos).inflate(64)).size());}
        FirstVisit.start(p,scene,SONG);p.teleportTo(target,scene.piano.getX()+3,scene.piano.getY()+1,scene.piano.getZ()-4,35,12);phase=1;
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(!Boolean.getBoolean("coraline.feastVisualCheck")||e.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL||phase<1||phase==4)return;
        if(++frames<250)return;frames=0;var mc=Minecraft.getInstance();int captured=phase;phase=0;
        if(captured==5){
            try(var image=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(java.nio.file.Path.of("../artwork/parents_wardrobe_ingame.png"));}catch(Exception ex){throw new RuntimeException(ex);}
            com.mojang.logging.LogUtils.getLogger().info("CORALINE_PARENT_WARDROBE_RENDER_OK");mc.stop();return;
        }
        if(captured<=3){
            try{
                var field=com.coraline.client.VisitAudioClient.class.getDeclaredField("music");field.setAccessible(true);
                var sound=(net.minecraft.client.resources.sounds.SoundInstance)field.get(null);
                require(sound!=null&&sound.getLocation().getPath().equals(captured==1?"other_father_song":"mysterious_supper"),"Wrong story song");
                require(sound.getSource()==net.minecraft.sounds.SoundSource.RECORDS&&mc.getSoundManager().isActive(sound),"Story song is not playing with vanilla music muted");
                com.mojang.logging.LogUtils.getLogger().info("CORALINE_STORY_SONG_ACTIVE: {}",sound.getLocation());
            }catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
        }
        if(captured==3)mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MUSIC).set(previousMusicVolume);
        try(var image=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(java.nio.file.Path.of("../artwork/first_visit_"+captured+".png"));}catch(Exception ex){throw new RuntimeException(ex);}
        mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
            if(captured==1){FirstVisit.table(p,scene);FirstVisit.start(p,scene,SUPPER);p.teleportTo(p.serverLevel(),scene.table.getX()+3,scene.table.getY()+2,scene.table.getZ()-3,38,35);phase=2;}
            else if(captured==2){FirstVisit.start(p,scene,CAKE);p.teleportTo(p.serverLevel(),scene.table.getX()+.5,scene.table.getY(),scene.table.getZ()-1.65,0,24);phase=3;}
            else{verifyFlow(p);VisitAudio.play(p,"see_you_soon",false);p.startSleeping(RealmData.get(p.server).realms.get(scene.door).bed);p.getPersistentData().putLong("CoralineWakeAt",p.server.overworld().getGameTime()+Math.max(60,VisitAudio.length("see_you_soon")+10));com.mojang.logging.LogUtils.getLogger().info("CORALINE_FIRST_VISIT_RENDER_AND_FLOW_OK");phase=4;}
        });
    }
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
    private static void verifyFlow(net.minecraft.server.level.ServerPlayer p){
        var tick=new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p);
        FirstVisit.start(p,scene,MOTHER_INTRO);p.stopRiding();scene.elapsed=VisitAudio.length("other_mother_intro")-1;FirstVisit.tick(tick);require(scene.stage==FIND_FATHER,"Mother transition failed: "+scene.stage);
        p.teleportTo(p.serverLevel(),scene.piano.getX()+.5,scene.piano.getY(),scene.piano.getZ()-2,0,0);FirstVisit.tick(tick);require(scene.stage==FATHER_ENTER,"Father approach failed");
        for(String audio:new String[]{"other_father_enter_scene","other_father_intro","other_father_song","other_father_dinners_ready"}){scene.elapsed=VisitAudio.length(audio)-1;FirstVisit.tick(tick);}
        require(scene.stage==RETURN_TO_MOTHER,"Piano did not release");p.teleportTo(p.serverLevel(),scene.table.getX()+.5,scene.table.getY(),scene.table.getZ()-2,0,0);FirstVisit.tick(tick);require(scene.stage==SIT,"Dinner approach failed");
        var chair=p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new net.minecraft.world.phys.AABB(scene.table).inflate(5),e->scene.owner.equals(e.owner)&&e.kind()==ScenePropEntity.CHAIR&&e.slot==0).get(0);chair.interact(p,net.minecraft.world.InteractionHand.MAIN_HAND);require(scene.stage==SUPPER&&p.isPassenger(),"Supper chair failed");
        scene.elapsed=SupperDialogue.endTick()-1;FirstVisit.tick(tick);require(scene.stage==CAKE,"Feast timer failed");scene.elapsed=VisitConfig.CAKE_WAIT.get()-1;FirstVisit.tick(tick);require(scene.stage==WAITING,"Cake delay failed");
        scene.elapsed=VisitAudio.length("other_parents_waiting")-1;FirstVisit.tick(tick);require(scene.stage==BED&&!p.isPassenger(),"Chair release failed");FirstVisit.slept(p);require(scene.stage==DONE&&!FirstVisit.locked(p),"Ending failed");
    }
}
