package com.coraline.feast;
import com.coraline.otherworld.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.Path;

/** Development-only real-client rendering and lifecycle smoke check. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class RevisionVisualCheck {
 private static int ticks,step;private static volatile boolean busy;private static VisitData.Scene s;private static BlockPos c;private static volatile Vec3 aim;
 private static ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);}
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
  if(!Boolean.getBoolean("coraline.revisionVisualCheck")||Boolean.getBoolean("coraline.introFortressVisualCheck")||Boolean.getBoolean("coraline.visitGiftVisualCheck")||Boolean.getBoolean("coraline.activityRevisionVisualCheck")||Boolean.getBoolean("coraline.dropperVisualCheck")||e.phase!=TickEvent.Phase.START)return;
  var mc=Minecraft.getInstance();if(mc.player==null||mc.getSingleplayerServer()==null)return;
  mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(10);if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)mc.setScreen(null);
  if(step==2&&aim!=null){var v=aim.subtract(mc.player.getEyePosition());mc.player.setYRot((float)Math.toDegrees(Math.atan2(-v.x,v.z)));mc.player.setXRot((float)-Math.toDegrees(Math.atan2(v.y,Math.hypot(v.x,v.z))));mc.options.keyAttack.setDown(true);}
  if(busy||++ticks%100!=0)return;
  if(step>0)capture(mc,"revision_"+step+".png");
  busy=true;mc.getSingleplayerServer().execute(()->{
   try{var p=player(mc);
    switch(step){
     case 0->{setup(mc);enter(mc,OtherMotherActivities.Activity.OTHER_MINESHAFT);}
     case 1->{check(p.serverLevel().getEntitiesOfClass(com.coraline.parents.ParentEntity.class,new net.minecraft.world.phys.AABB(c).inflate(8),a->ActivityScenery.owned(a,s)).size()==1,"Missing father");
      var ore=MineshaftActivity.generate(s.loop.expedition.getLong("Seed")).ores().get(0);var at=c.offset(ore.offset());p.connection.teleport(at.getX()+2.5,at.getY()-1,at.getZ()+.5,90,10);aim=Vec3.atCenterOf(at);}
     case 2->{mc.options.keyAttack.setDown(false);check(s.loop.expedition.getInt("Count")>0,"Client mining input failed");p.setPos(Vec3.atBottomCenterOf(c.south(7)));ActivityWorld.exit(p,s);check(!p.getPersistentData().contains("CoralineMineDelivery"),"Unsettled mine delivery");enter(mc,OtherMotherActivities.Activity.SHOPPING_MALL);}
     case 3->{check(p.serverLevel().getEntitiesOfClass(com.coraline.parents.ParentEntity.class,new net.minecraft.world.phys.AABB(c).inflate(8),a->ActivityScenery.owned(a,s)).size()==1,"Missing mall father");p.setPos(Vec3.atBottomCenterOf(MallScenery.shop(c,23)));ShoppingMallActivity.open(p,s,23*8);}
     case 4->{ExpeditionNetwork.mall(p,new net.minecraft.nbt.CompoundTag());enter(mc,OtherMotherActivities.Activity.OTHER_WOODLAND_MANSION);p.connection.teleport(c.getX()+.5,c.getY()+9,c.getZ()-78.5,0,0);}
     case 5->{var witness=p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,ExpeditionActivities.bounds(c,s.loop.activity),a->a.getPersistentData().getInt("MansionClue")==1).get(0);p.connection.teleport(witness.getX()+3,witness.getY(),witness.getZ(),90,0);}
     case 6->{for(int i=0;i<6;i++)MansionActivity.discover(p,s,i);}
     case 7->{enter(mc,OtherMotherActivities.Activity.OTHER_END_CITY);c=EndCityActivity.center(p,s);s.loop.endCity.putLong("VoiceUntil",OtherMotherLoop.now(p));EndCityActivity.tick(p,s,c);p.connection.teleport(p.getX(),p.getY(),p.getZ(),180,25);}
     case 8->{check(s.loop.endCity.getInt("Phase")==1,"End City did not start");s.loop.endCity.putLong("Deadline",OtherMotherLoop.now(p));EndCityActivity.tick(p,s,c);check(s.loop.endCity.getInt("Phase")==5&&ActivityWorld.exitReady(s),"End City failure softlock");enter(mc,OtherMotherActivities.Activity.OTHER_DESERT_TEMPLE);}
     case 9->{enter(mc,OtherMotherActivities.Activity.MUSIC_FESTIVAL);c=MusicFestivalActivity.center(p,s);s.loop.festival.putInt("Phase",1);var console=LoopProps.nearby(p.serverLevel(),s,c.north(24)).stream().filter(prop->prop.kind()==ScenePropEntity.FESTIVAL_CONSOLE).findFirst().orElseThrow();MusicFestivalActivity.interactProp(p,s,console);}
     case 10->{check(com.coraline.client.FestivalClient.active(),"Rhythm UI did not start");com.mojang.logging.LogUtils.getLogger().info("CORALINE_FESTIVAL_NOTE_ONLY_CHECK PASSED");}
     case 11->{com.mojang.logging.LogUtils.getLogger().info("CORALINE_REVISION_CLIENT_CHECK PASSED");mc.execute(mc::stop);}
    }
    step++;
   }catch(Throwable ex){com.mojang.logging.LogUtils.getLogger().error("CORALINE_REVISION_CLIENT_CHECK FAILED",ex);mc.execute(mc::stop);}finally{busy=false;}
  });
 }
 private static void check(boolean pass,String message){if(!pass)throw new IllegalStateException(message);}
 private static void capture(Minecraft mc,String name){try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../artwork/"+name));}catch(Exception ex){throw new RuntimeException(ex);}}
 private static void setup(Minecraft mc){var server=mc.getSingleplayerServer();var p=player(mc);p.setGameMode(GameType.SURVIVAL);
  var door=new DoorData.Door();door.owner=p.getUUID();door.pos=new BlockPos(59000,80,59000);DoorData.get(server).doors.put(door.id,door);var realm=RealmData.get(server).realm(door);realm.ready=true;realm.parentGeneration=1;realm.porch=realm.doorPos;
  var other=server.getLevel(OtherWorld.KEY);for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)other.setBlock(realm.doorPos.offset(x,-1,z),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
  s=new VisitData.Scene();s.owner=p.getUUID();s.door=door.id;s.stage=VisitData.Stage.DONE;s.table=realm.doorPos;s.piano=realm.doorPos;s.loop.portal=realm.doorPos;VisitData.get(server).scenes.put(s.owner,s);p.getPersistentData().putUUID("CoralineVisitDoor",door.id);p.teleportTo(other,realm.doorPos.getX()+.5,80,realm.doorPos.getZ()+.5,0,0);
 }
 private static void enter(Minecraft mc,OtherMotherActivities.Activity a){var p=player(mc);mc.execute(()->mc.setScreen(null));if(ActivityWorld.isActivity(p.level())){if(!ActivityWorld.exitReady(s))NewActivitySafety.abort(p,s);ActivityWorld.exit(p,s);}s.loop.phase=OtherMotherLoopState.Phase.READY;s.loop.activity=a;s.loop.generation++;ActivityWorld.enter(p,s);ActivityIntro.skip(p,ActivityWorld.introSound(a),s.loop.generation);c=ExpeditionActivities.center(p,s);
  if(ExpeditionActivities.supports(a)){s.loop.expedition.putLong("VoiceUntil",OtherMotherLoop.now(p));ExpeditionActivities.tick(p,s,c);}p.connection.teleport(p.getX(),p.getY(),p.getZ(),180,0);
 }
}
