package com.coraline.feast;
import com.coraline.otherworld.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.Path;

/** Opt-in real-client integration/visual check; never included in a production build. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class MallBattleHuntVisualCheck {
    private static volatile int phase;private static int ticks;private static VisitData.Scene s;private static BlockPos c;private static int stage;private static long snowStart;private static volatile Vec3 aim;
    private static ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);}
    private static void check(boolean value,String message){if(!value)throw new IllegalStateException("MBH QA: "+message);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(!Boolean.getBoolean("coraline.mallBattleHuntVisualCheck")||e.phase!=TickEvent.Phase.START)return;var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(12);ticks++;if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)mc.setScreen(null);
        if(phase==0){phase=1;mc.getSingleplayerServer().execute(()->setup(mc));return;}
        if(phase==20&&aim!=null){mc.setScreen(null);look(mc,aim);mc.options.keyUse.setDown(true);mc.options.keyLeft.setDown(ticks/60%2==0);mc.options.keyRight.setDown(ticks/60%2!=0);}
        if(ticks%100!=0)return;
        switch(phase){
            case 2->{capture(mc,"mall_entrance.png");if(s.loop.expedition.getInt("Phase")==1){phase=3;mc.getSingleplayerServer().execute(()->{var p=player(mc);check(ActivityWorld.exitReady(s),"Mall exit missing");var b=MallScenery.shop(c,0);p.connection.teleport(b.getX()+10.5,b.getY(),b.getZ()+.5,90,5);});}}
            case 3->{capture(mc,"mall_candy_shop.png");phase=4;mc.getSingleplayerServer().execute(()->ShoppingMallActivity.open(player(mc),s,0));}
            case 4->{capture(mc,"mall_purchase_ui.png");phase=5;mc.getSingleplayerServer().execute(()->{var p=player(mc);var n=s.loop.expedition;ShoppingMallActivity.request(p,n.getUUID("MenuNonce"),0,1);check(n.getInt("Balance")==970,"Purchase failed");check(n.getList("ShoppingBag",10).size()==1,"Bag empty");p.connection.teleport(c.getX()+.5,c.getY()+8,c.getZ()-84.5,0,5);ExpeditionNetwork.mall(p,new net.minecraft.nbt.CompoundTag());});mc.setScreen(null);}
            case 5->{mc.setScreen(null);capture(mc,"mall_upper_gallery.png");phase=6;mc.getSingleplayerServer().execute(()->{var p=player(mc);ActivityWorld.exit(p,s);check(p.getInventory().countItem(com.coraline.registry.ModItems.ITEMS.getEntries().stream().filter(r->r.getId().getPath().equals("lollipop")).findFirst().orElseThrow().get())>=6,"Checkout lost purchase");enter(mc,OtherMotherActivities.Activity.TREASURE_HUNT);phase=7;});}
            case 7->{if(s.loop.expedition.getInt("Phase")==1){capture(mc,"hunt_first_clue.png");mc.setScreen(null);phase=8;mc.getSingleplayerServer().execute(()->{var p=player(mc);var m=TreasureRoute.LANDMARKS.get(s.loop.expedition.getIntArray("Route")[0]);p.connection.teleport(m.x()+c.getX()+.5,c.getY(),m.z()+c.getZ()+10.5,180,0);});}}
            case 8->{mc.setScreen(null);capture(mc,"hunt_landmark_"+stage+".png");phase=9;mc.getSingleplayerServer().execute(()->{var p=player(mc);var route=s.loop.expedition.getIntArray("Route");var at=TreasureRoute.LANDMARKS.get(route[stage]).note(c);ActivityScenery.teleport(p,Vec3.atBottomCenterOf(at.south(2)));TreasureHuntActivity.interactBlock(p,at);check(s.loop.expedition.getInt("Step")==stage+1,"Physical clue did not advance");stage++;});}
            case 9->{capture(mc,"hunt_clue_"+stage+".png");mc.setScreen(null);phase=10;mc.getSingleplayerServer().execute(()->{var p=player(mc);var m=TreasureRoute.LANDMARKS.get(s.loop.expedition.getIntArray("Route")[stage]);p.connection.teleport(m.x()+c.getX()+.5,c.getY(),m.z()+c.getZ()+10.5,180,0);phase=stage<5?8:11;});}
            case 11->{capture(mc,"hunt_final_landmark.png");phase=12;mc.getSingleplayerServer().execute(()->{var p=player(mc);var door=TreasureScenery.vaultDoor(c,s.loop.expedition.getIntArray("Route")[5]);ActivityScenery.teleport(p,Vec3.atBottomCenterOf(door.south(2)));TreasureHuntActivity.interactBlock(p,door);check(s.loop.expedition.getInt("Phase")==2,"Vault stayed locked");p.connection.teleport(door.getX()+.5,door.getY(),door.getZ()+1.5,180,30);});}
            case 12->{mc.setScreen(null);capture(mc,"hunt_cellar_stairs.png");phase=13;mc.options.keyUp.setDown(true);}
            case 13->{mc.options.keyUp.setDown(false);capture(mc,"hunt_vault.png");phase=14;mc.getSingleplayerServer().execute(()->{var p=player(mc);check(p.getY()<c.getY()-3,"Cannot walk down cellar stairs");var prize=p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,ExpeditionActivities.bounds(c,s.loop.activity),a->a.slot==780).stream().findFirst().orElseThrow();ActivityScenery.teleport(p,prize.position().add(0,0,2));TreasureHuntActivity.interactProp(p,s,prize);check(ActivityWorld.exitReady(s),"Hunt exit missing");enter(mc,OtherMotherActivities.Activity.SNOWBALL_BATTLE);phase=18;});}
            case 18->{mc.setScreen(null);capture(mc,"snow_entry.png");if(s.loop.expedition.getInt("Phase")==2){phase=20;mc.getSingleplayerServer().execute(()->snowStart=OtherMotherLoop.now(player(mc)));}}
            case 20->{capture(mc,"snow_battle.png");mc.getSingleplayerServer().execute(()->{var p=player(mc);var alive=SnowballBattleActivity.living(p,s);int total=0;for(var tag:s.loop.expedition.getList("Fighters",10))total+=((net.minecraft.nbt.CompoundTag)tag).getInt("Health");long age=OtherMotherLoop.now(p)-snowStart;com.mojang.logging.LogUtils.getLogger().info("MBH SNOW age={} ownerHp={} npcHp={} fighters={}",age,s.loop.expedition.getInt("Health"),total,alive.size());if(age>2000)check(total<30,"NPC projectiles never hit anyone");var target=alive.stream().filter(a->a!=p&&SnowballBattleActivity.visible(p,a)).min(java.util.Comparator.comparingDouble(p::distanceToSqr)).orElse(null);aim=target==null?null:target.getEyePosition().add(0,p.distanceTo(target)*.12,0);if(s.loop.expedition.getInt("Phase")>=3)phase=21;});}
            case 21->{mc.options.keyUse.setDown(false);mc.options.keyLeft.setDown(false);mc.options.keyRight.setDown(false);capture(mc,"snow_result.png");phase=22;mc.getSingleplayerServer().execute(()->{var p=player(mc);if(s.loop.expedition.getInt("Phase")==3){var prize=p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,ExpeditionActivities.bounds(c,s.loop.activity),a->a.slot==760).stream().findFirst().orElseThrow();SnowballBattleActivity.interactProp(p,s,prize);}check(ActivityWorld.exitReady(s),"Snow exit missing");ActivityWorld.exit(p,s);com.mojang.logging.LogUtils.getLogger().info("CORALINE_MALL_BATTLE_HUNT_CLIENT_CHECK PASSED");phase=23;});}
            case 23->mc.stop();
        }
    }
    private static void look(Minecraft mc,Vec3 at){var v=at.subtract(mc.player.getEyePosition());mc.player.setYRot((float)Math.toDegrees(Math.atan2(-v.x,v.z)));mc.player.setXRot((float)-Math.toDegrees(Math.atan2(v.y,Math.sqrt(v.x*v.x+v.z*v.z))));}
    private static void capture(Minecraft mc,String name){try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../artwork/"+name));}catch(Exception ex){throw new RuntimeException(ex);}}
    private static void setup(Minecraft mc){var server=mc.getSingleplayerServer();var p=player(mc);p.setGameMode(GameType.SURVIVAL);var door=new DoorData.Door();door.owner=p.getUUID();door.pos=new BlockPos(59000,80,59000);DoorData.get(server).doors.put(door.id,door);var realm=RealmData.get(server).realm(door);realm.ready=true;realm.parentGeneration=1;realm.porch=realm.doorPos;var other=server.getLevel(OtherWorld.KEY);for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)other.setBlock(realm.doorPos.offset(x,-1,z),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
        s=new VisitData.Scene();s.owner=p.getUUID();s.door=door.id;s.stage=VisitData.Stage.DONE;s.table=realm.doorPos;s.piano=realm.doorPos;s.loop.portal=realm.doorPos;s.loop.generation=1;s.loop.phase=OtherMotherLoopState.Phase.READY;s.loop.activity=Boolean.getBoolean("coraline.snowBattleOnly")?OtherMotherActivities.Activity.SNOWBALL_BATTLE:OtherMotherActivities.Activity.SHOPPING_MALL;VisitData.get(server).scenes.put(p.getUUID(),s);p.getPersistentData().putUUID("CoralineVisitDoor",door.id);p.teleportTo(other,realm.doorPos.getX()+.5,80,realm.doorPos.getZ()+.5,0,0);ActivityWorld.enter(p,s);c=ExpeditionActivities.center(p,s);p.connection.teleport(p.getX(),p.getY(),p.getZ(),180,0);phase=Boolean.getBoolean("coraline.snowBattleOnly")?18:2;
    }
    private static void enter(Minecraft mc,OtherMotherActivities.Activity activity){var p=player(mc);if(ActivityWorld.isActivity(p.level()))ActivityWorld.exit(p,s);s.loop.phase=OtherMotherLoopState.Phase.READY;s.loop.activity=activity;s.loop.generation++;ActivityWorld.enter(p,s);c=ExpeditionActivities.center(p,s);p.connection.teleport(p.getX(),p.getY(),p.getZ(),180,0);}
}
