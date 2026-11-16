package com.coraline.wybie;
import com.coraline.feast.*;
import com.coraline.bobinsky.*;
import com.coraline.tea.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
/** Full scripted QA in the copied, disposable StoryVisitsQA world. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class StoryVisitsVisualCheck {
    private static boolean requested,musicChecked;private static volatile boolean ready,done;private static volatile int stage;private static int ticks;
    private static net.minecraft.nbt.ListTag original;
    private static final Set<String> captured=new HashSet<>();
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("coraline.storyVisitsVisualCheck")||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.player==null||mc.getSingleplayerServer()==null)return;
        mc.options.pauseOnLostFocus=false;if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)mc.setScreen(null);if(!requested&&mc.player.tickCount<60)return;
        if(!requested){requested=true;if(Boolean.getBoolean("coraline.storyVisitsFinishCheck"))WybieVisitVisualCheck.capturePoses(mc);mc.getMusicManager().startPlaying(net.minecraft.sounds.Musics.GAME);mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var level=p.server.overworld();var center=new BlockPos(100,230,100);
            for(int x=-32;x<=32;x++)for(int z=-32;z<=32;z++)level.setBlock(center.offset(x,-1,z),Blocks.GRASS_BLOCK.defaultBlockState(),3);
            level.setDayTime(1000);level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,p.server);level.setWeatherParameters(100000,0,false,false);
            p.getInventory().clearContent();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.teleportTo(level,100.5,230,100.5,0,0);p.removeAllEffects();p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,10000,0,false,false));
            var bed=center.east(2);level.setBlock(bed,Blocks.RED_BED.defaultBlockState(),3);p.setRespawnPosition(Level.OVERWORLD,bed,0,false,false);
            var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.table=s.piano=center;s.stage=VisitData.Stage.DONE;s.wybieVisited=true;s.teaVisited=true;VisitData.get(p.server).scenes.put(s.owner,s);VisitData.get(p.server).setDirty();
            if(Boolean.getBoolean("coraline.storyVisitsTeaOnly")){s.bobinskyVisited=true;s.teaVisited=false;if(!TeaVisit.offer(p,s))throw new IllegalStateException("No safe tea QA house site");var v=BobinskyVisitData.get(p.server).teaVisits.get(p.getUUID());original=v.getList("Originals",10).copy();var at=BobinskyHouse.front(v);p.teleportTo(at.getX()+.5,at.getY(),at.getZ()-.5);TeaVisit.knock(p,v);stage=2;ready=true;return;}
            BobinskyVisit.offer(p,s);
            if(Boolean.getBoolean("coraline.storyVisitsFinishCheck")){
                var v=BobinskyVisitData.get(p.server).visits.get(p.getUUID());BobinskyVisit.takeParcel(p,BlockPos.of(v.getLong("Parcel")));original=v.getList("Originals",10).copy();var at=BobinskyHouse.front(v);p.teleportTo(at.getX()+.5,at.getY(),at.getZ()-.5);BobinskyVisit.knock(p,v);v.putInt("Phase",BobinskyVisit.H);v.putInt("Elapsed",0);v.putBoolean("BeetGiven",true);p.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BEETROOT));VisitAudio.play(p,"bobinsky_visit_h",false);stage=1;
            }
            ready=true;
        });}
        if(!ready)return;ticks++;if(ticks>8500)throw new IllegalStateException("Story visits QA timeout; stage="+stage);
        if(!musicChecked&&com.coraline.client.VisitAudioClient.visitActive()){if(mc.getMusicManager().isPlayingMusic(net.minecraft.sounds.Musics.GAME))throw new IllegalStateException("Existing vanilla music survived visit start");var vanilla=net.minecraft.client.resources.sounds.SimpleSoundInstance.forMusic(net.minecraft.sounds.SoundEvents.MUSIC_GAME.value());var blocked=new net.minecraftforge.client.event.sound.PlaySoundEvent(null,vanilla);com.coraline.client.StoryMusicGuard.onSound(blocked);if(blocked.getSound()!=null)throw new IllegalStateException("Vanilla music restart was not blocked during visit");musicChecked=true;com.mojang.logging.LogUtils.getLogger().info("VISIT_MUSIC_GUARD_OK: previous vanilla music stopped; attempted restart blocked");}
        if(!done)mc.getSingleplayerServer().execute(()->advance(mc));
        var bob=mc.level.getEntitiesOfClass(BobinskyVisitorEntity.class,mc.player.getBoundingBox().inflate(64));var tea=mc.level.getEntitiesOfClass(TeaVisitorEntity.class,mc.player.getBoundingBox().inflate(24));
        net.minecraft.world.entity.Entity target=!bob.isEmpty()?bob.get(0):tea.stream().filter(t->t.characterId().equals("miss_spink")).findFirst().orElse(tea.isEmpty()?null:tea.get(0));
        mc.options.keyUse.setDown(!tea.isEmpty()&&tea.get(0).phase()==TeaVisit.DRINK);
        if(target!=null){int phase=target instanceof BobinskyVisitorEntity b?b.phase():((TeaVisitorEntity)target).phase();int elapsed=target instanceof BobinskyVisitorEntity b?b.elapsed():((TeaVisitorEntity)target).elapsed();String key=(target instanceof BobinskyVisitorEntity?"bob_":"tea_")+phase;
            if(elapsed>=21&&captured.add(key)){if(target instanceof BobinskyVisitorEntity b&&phase==BobinskyVisit.J&&b.distanceToSqr(mc.player)<1)throw new IllegalStateException("Bobinsky did not move beside the player");capture(mc,key);}
            if(target instanceof BobinskyVisitorEntity b&&b.phase()==BobinskyVisit.WAIT_HIDE){mc.player.setYRot(180);mc.player.setXRot(0);}
            else if(!(target instanceof BobinskyVisitorEntity b&&b.phase()>=BobinskyVisit.H&&b.phase()<=BobinskyVisit.LAND)&&!(target instanceof TeaVisitorEntity t&&t.phase()==TeaVisit.LEAVE)){var delta=target.getEyePosition().subtract(mc.player.getEyePosition());mc.player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));mc.player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance())));}
            else if(target instanceof TeaVisitorEntity t&&t.phase()==TeaVisit.LEAVE){mc.player.setYRot(180);mc.player.setXRot(0);}
        }
        if(done){capture(mc,"finished");com.mojang.logging.LogUtils.getLogger().info(Boolean.getBoolean("coraline.storyVisitsTeaOnly")?"TEA_LIVE_FINAL_OK: tea exchange, cup reading, glass sequence, exit and restoration completed":"STORY_VISITS_LIVE_OK: Bobinsky and tea visits completed, rewards delivered, all house blocks restored");mc.stop();}
    }
    private static void advance(Minecraft mc){
        var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var data=BobinskyVisitData.get(p.server);var scene=VisitData.get(p.server).scenes.get(p.getUUID());
        if(stage==0){var v=data.visits.get(p.getUUID());if(v==null)return;if(v.getInt("Phase")==BobinskyVisit.CHEESE){var pos=BlockPos.of(v.getLong("Parcel"));p.teleportTo(pos.getX()+.5,pos.getY(),pos.getZ()-1.5);BobinskyVisit.takeParcel(p,pos);}else if(v.getInt("Phase")==BobinskyVisit.KNOCK){original=v.getList("Originals",10).copy();var at=BobinskyHouse.front(v);p.teleportTo(at.getX()+.5,at.getY(),at.getZ()-.5);BobinskyVisit.knock(p,v);stage=1;}}
        else if(stage==1&&scene.bobinskyVisited){verifyRestored(p,original);if(!p.getInventory().contains(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BEETROOT)))throw new IllegalStateException("Bobinsky beet missing");p.teleportTo(100.5,230,100.5);scene.teaVisited=false;if(TeaVisit.offer(p,scene)){var v=data.teaVisits.get(p.getUUID());original=v.getList("Originals",10).copy();var at=BobinskyHouse.front(v);p.teleportTo(at.getX()+.5,at.getY(),at.getZ()-.5);TeaVisit.knock(p,v);stage=2;}}
        else if(stage==2){if(scene.teaVisited){verifyRestored(p,original);if(p.getInventory().contains(new net.minecraft.world.item.ItemStack(com.coraline.registry.ModItems.JASMINE_TEA.get()))||p.getInventory().contains(new net.minecraft.world.item.ItemStack(com.coraline.registry.ModItems.EMPTY_TEACUP.get())))throw new IllegalStateException("Loaned tea/cup was not reclaimed");done=true;return;}var v=data.teaVisits.get(p.getUUID());if(v==null)return;int phase=v.getInt("Phase");var center=BlockPos.of(v.getLong("House"));
            if(phase==TeaVisit.ENTER)p.teleportTo(center.getX()+.5,center.getY(),center.getZ()-2.5);
            if(phase==TeaVisit.SEAT&&!v.getBoolean("Seated"))p.teleportTo(center.getX()+1.5,center.getY(),center.getZ()+1.5);
            if(phase==TeaVisit.D){if(!(p.getVehicle() instanceof TeaSeatEntity seat)||!seat.locked())throw new IllegalStateException("Chair is not locked before tea prompt");if(!v.getBoolean("QASeatTested")){p.stopRiding();if(p.getVehicle()!=seat)throw new IllegalStateException("Actual early dismount escaped the chair");v.putBoolean("QASeatTested",true);}if(TeaVisit.canDrink(p,p.getInventory().items.stream().filter(a->a.is(com.coraline.registry.ModItems.JASMINE_TEA.get())).findFirst().orElse(net.minecraft.world.item.ItemStack.EMPTY)))throw new IllegalStateException("Tea permitted before prompt");if(!v.hasUUID("SpinkSeat"))throw new IllegalStateException("Spink is not seated across the table");}
            if(phase==TeaVisit.DRINK&&!p.isUsingItem()){for(int i=0;i<9;i++)if(p.getInventory().items.get(i).is(com.coraline.registry.ModItems.JASMINE_TEA.get())){p.getInventory().selected=i;p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(i));com.coraline.registry.ModItems.JASMINE_TEA.get().use(p.level(),p,net.minecraft.world.InteractionHand.MAIN_HAND);break;}}
            if(phase==TeaVisit.SWIRL&&!v.getBoolean("Drank"))throw new IllegalStateException("Cup exchange did not follow drinking");
            if(phase==TeaVisit.LEAVE){var at=BobinskyHouse.front(v).north(2);p.teleportTo(at.getX()+.5,at.getY(),at.getZ()+.5);}
            if(scene.teaVisited){verifyRestored(p,original);if(p.getInventory().contains(new net.minecraft.world.item.ItemStack(com.coraline.registry.ModItems.JASMINE_TEA.get()))||p.getInventory().contains(new net.minecraft.world.item.ItemStack(com.coraline.registry.ModItems.EMPTY_TEACUP.get())))throw new IllegalStateException("Loaned tea/cup was not reclaimed");done=true;}
        }
    }
    private static void verifyRestored(net.minecraft.server.level.ServerPlayer p,net.minecraft.nbt.ListTag originals){for(var tag:originals){var n=(net.minecraft.nbt.CompoundTag)tag;var pos=BlockPos.of(n.getLong("Pos"));var state=net.minecraft.nbt.NbtUtils.readBlockState(p.serverLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK),n.getCompound("State"));if(!p.serverLevel().getBlockState(pos).equals(state))throw new IllegalStateException("House restoration mismatch at "+pos);}}
    private static void capture(Minecraft mc,String name){try{var out=java.nio.file.Path.of("../artwork/story_visits_qa");java.nio.file.Files.createDirectories(out);try(var image=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(out.resolve(name+".png"));}}catch(Exception e){throw new RuntimeException(e);}}
}
