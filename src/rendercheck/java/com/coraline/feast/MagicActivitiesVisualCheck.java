package com.coraline.feast;

import com.coraline.otherworld.DoorData;
import com.coraline.otherworld.OtherWorld;
import com.coraline.otherworld.RealmData;
import com.coraline.parents.ParentEntity;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.Path;
import java.util.UUID;

/** Opt-in client regression: two twenty-round shows, prize spending and the full flight/harvest. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class MagicActivitiesVisualCheck {
    private static volatile int phase;private static int frames;
    private static VisitData.Scene scene;private static BlockPos show,garden;
    private MagicActivitiesVisualCheck(){}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(!Boolean.getBoolean("coraline.magicActivitiesVisualCheck")||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null)return;
        mc.options.hideGui=true;mc.options.renderDistance().set(12);mc.options.pauseOnLostFocus=false;
        if(phase==0){if(mc.getSingleplayerServer().getPlayerList().getPlayers().isEmpty())return;
            phase=1;mc.getSingleplayerServer().execute(()->setup(mc.getSingleplayerServer()));return;}
        if(phase==2){mc.player.setYRot(180);mc.player.setXRot(-9);
            if(frames==30&&!com.coraline.client.VisitAudioClient.hasVoice(ActivityWorld.introSound(OtherMotherActivities.Activity.MAGIC_SHOW)))
                throw new IllegalStateException("Magic Show Father intro did not play");
            if(++frames>85){screenshot(mc,"magic_show_theatre_native.png");frames=0;phase=9;
                mc.getSingleplayerServer().execute(()->beginShow(mc.getSingleplayerServer()));}}
        if(phase==9){mc.player.setYRot(180);mc.player.setXRot(-8);
            if(++frames>45){screenshot(mc,"magic_show_open_scene_native.png");frames=0;phase=3;
                mc.getSingleplayerServer().execute(()->verifyShows(mc.getSingleplayerServer()));}}
        if(phase==4){mc.player.setYRot(180);mc.player.setXRot(2);
            if(frames==30&&(!com.coraline.client.VisitAudioClient.hasVoice(ActivityWorld.introSound(OtherMotherActivities.Activity.MAGIC_GARDEN))||
                !com.coraline.client.VisitAudioClient.hasMusic("magic_garden_fly")))
                throw new IllegalStateException("Garden intro or uninterrupted flight music missing");
            if(++frames>65){frames=0;phase=5;mc.getSingleplayerServer().execute(()->verifyBoardAndAscent(mc.getSingleplayerServer()));}}
        if(phase==6){mc.player.setYRot(180);mc.player.setXRot(57);
            if(++frames>75){screenshot(mc,"magic_garden_reveal_native.png");frames=0;phase=7;
                mc.getSingleplayerServer().execute(()->verifyGardenFinish(mc.getSingleplayerServer()));}}
        if(phase==8)mc.stop();
    }
    private static void screenshot(Minecraft mc,String name){try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
        image.writeToFile(Path.of("../artwork/"+name));}catch(Exception ex){throw new RuntimeException(ex);}}
    private static void setup(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();
        var door=new DoorData.Door();door.owner=p.getUUID();door.pos=new BlockPos(47000,80,47000);DoorData.get(server).doors.put(door.id,door);
        var realm=RealmData.get(server).realm(door);realm.ready=true;realm.parentGeneration=1;realm.porch=realm.doorPos;
        var other=server.getLevel(OtherWorld.KEY);
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)other.setBlock(realm.doorPos.offset(x,-1,z),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
        scene=new VisitData.Scene();scene.owner=p.getUUID();scene.door=door.id;scene.stage=VisitData.Stage.DONE;
        scene.piano=realm.doorPos;scene.table=realm.doorPos;scene.loop.generation=1;scene.loop.portal=realm.doorPos;
        scene.loop.phase=OtherMotherLoopState.Phase.READY;scene.loop.activity=OtherMotherActivities.Activity.MAGIC_SHOW;
        VisitData.get(server).scenes.put(p.getUUID(),scene);VisitData.get(server).setDirty();p.getPersistentData().putUUID("CoralineVisitDoor",door.id);
        for(long seed=0;seed<500;seed++)MagicShowActivity.validatePerformance(seed*0xDA942042E4DD58B5L);
        p.teleportTo(other,realm.doorPos.getX()+.5,realm.doorPos.getY(),realm.doorPos.getZ()+.5,0,0);
        ActivityWorld.enter(p,scene);show=MagicShowActivity.center(p,scene);
        if(!ActivityWorld.isActivity(p.level())||scene.loop.magicStage!=0||ActivityWorld.exitReady(scene))
            throw new IllegalStateException("Show did not wait for Father intro");
        if(!p.serverLevel().getBlockState(show.below(4)).is(Blocks.MAGENTA_GLAZED_TERRACOTTA))throw new IllegalStateException("Theatre not built");
        int viewers=p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class,new AABB(show).inflate(25,8,23)).size();
        if(viewers<20)throw new IllegalStateException("Button-eyed audience missing: "+viewers);
        phase=2;
    }
    private static void fastTick(net.minecraft.server.level.ServerPlayer p){scene.loop.magicPhaseUntil=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);}
    private static void beginShow(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);var st=scene.loop;
        ActivityWorld.tick(p,scene);if(st.magicStage!=0)throw new IllegalStateException("Father intro gate bypassed");
        st.magicVoiceUntil=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        if(st.magicStage!=1)throw new IllegalStateException("First curtain did not open");
        st.magicPhaseUntil=OtherMotherLoop.now(p)+1000;
    }
    private static void showRound(net.minecraft.server.level.ServerPlayer p,boolean correct,boolean timeout){
        var st=scene.loop;int old=st.magicTokens,round=st.magicRound;
        if(st.magicStage!=1)throw new IllegalStateException("Round "+round+" did not open");
        fastTick(p);if(st.magicStage!=2)throw new IllegalStateException("Observation phase missing");
        fastTick(p);if(st.magicStage!=3)throw new IllegalStateException("Curtain closing missing");
        fastTick(p);if(st.magicStage!=4)throw new IllegalStateException("Transformation missing");
        fastTick(p);if(st.magicStage!=5)throw new IllegalStateException("Answer phase missing");
        var spots=p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new AABB(show.north(5)).inflate(13,5,4),e->e.kind()==ScenePropEntity.MAGIC_SPOT&&p.getUUID().equals(e.owner));
        if(spots.size()<5)throw new IllegalStateException("Selectable display missing in round "+round);
        var target=spots.stream().filter(e->e.slot==(correct?st.magicAnswer:(st.magicAnswer+1)%spots.size())).findFirst().orElseThrow();
        if(timeout)fastTick(p);else{
            p.teleportTo(p.serverLevel(),target.getX(),show.getY(),show.getZ()-.5,180,0);
            LoopProps.interact(p,target);
            int after=st.magicTokens;LoopProps.interact(p,target);
            if(st.magicTokens!=after)throw new IllegalStateException("Spam-click duplicated a token");
        }
        if(st.magicStage!=6||st.magicTokens!=old+(correct&&!timeout?1:0))throw new IllegalStateException("Round score incorrect");
        fastTick(p);if(st.magicStage!=9)throw new IllegalStateException("Inter-round curtain did not close");
        fastTick(p);if(round<19&&st.magicStage!=1||round==19&&st.magicStage!=7)throw new IllegalStateException("Round did not advance");
    }
    private static void exit(net.minecraft.server.level.ServerPlayer p,BlockPos c,int z){
        var list=p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new AABB(c.south(z)).inflate(3),e->e.kind()==ScenePropEntity.ACTIVITY_EXIT&&p.getUUID().equals(e.owner));
        if(list.size()!=1)throw new IllegalStateException("Exit portal missing");
        var portal=list.get(0);p.teleportTo(p.serverLevel(),portal.getX(),portal.getY(),portal.getZ(),180,0);LoopProps.interact(p,portal);
        if(!OtherWorld.isOther(p.level()))throw new IllegalStateException("Exit portal failed");
    }
    private static void verifyShows(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);var st=scene.loop;
        if(st.magicStage!=1)throw new IllegalStateException("First trick missing after intro");
        for(int i=0;i<20;i++)showRound(p,true,false);
        if(st.magicTokens!=20)throw new IllegalStateException("Perfect show was not worth twenty tokens");
        var helper=net.minecraftforge.common.util.FakePlayerFactory.get(p.serverLevel(),new GameProfile(UUID.randomUUID(),"MagicHelper"));
        var jackpot=p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new AABB(show.offset(16,0,11)).inflate(2),e->e.kind()==ScenePropEntity.MAGIC_PRIZE&&e.slot==5).get(0);
        LoopProps.interact(helper,jackpot);if(st.magicTokens!=20)throw new IllegalStateException("Helper spent owner's tokens");
        p.teleportTo(p.serverLevel(),jackpot.getX(),jackpot.getY(),jackpot.getZ()+1,180,0);LoopProps.interact(p,jackpot);
        if(st.magicTokens!=0||p.getInventory().countItem(Items.NETHER_STAR)!=1)throw new IllegalStateException("Jackpot purchase failed");
        LoopProps.interact(p,jackpot);if(p.getInventory().countItem(Items.NETHER_STAR)!=1)throw new IllegalStateException("Jackpot duplicated");
        var finish=p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new AABB(show.offset(16,0,19)).inflate(2),e->e.kind()==ScenePropEntity.MAGIC_FINISH).get(0);
        p.teleportTo(p.serverLevel(),finish.getX(),finish.getY(),finish.getZ()+1,180,0);LoopProps.interact(p,finish);
        if(!ActivityWorld.exitReady(scene))throw new IllegalStateException("Show exit did not open");exit(p,show,21);
        OtherMotherLoop.phase(p,scene,OtherMotherLoopState.Phase.READY);scene.loop.activity=OtherMotherActivities.Activity.MAGIC_SHOW;st.generation++;
        ActivityWorld.enter(p,scene);show=MagicShowActivity.center(p,scene);st.magicVoiceUntil=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        for(int i=0;i<20;i++)showRound(p,i%2==0,i%10==1);
        if(st.magicTokens!=10)throw new IllegalStateException("Mixed-answer tokens wrong: "+st.magicTokens);
        finish=p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new AABB(show.offset(16,0,19)).inflate(2),e->e.kind()==ScenePropEntity.MAGIC_FINISH).get(0);
        p.teleportTo(p.serverLevel(),finish.getX(),finish.getY(),finish.getZ()+1,180,0);LoopProps.interact(p,finish);exit(p,show,21);
        OtherMotherLoop.phase(p,scene,OtherMotherLoopState.Phase.READY);scene.loop.activity=OtherMotherActivities.Activity.MAGIC_GARDEN;st.generation++;
        ActivityWorld.enter(p,scene);garden=MagicGardenActivity.center(p,scene);
        if(!p.serverLevel().getBlockState(garden.below(4)).is(Blocks.PINK_GLAZED_TERRACOTTA)||st.flyingGardenStage!=0)
            throw new IllegalStateException("Garden not built or flight started before introduction");
        frames=0;phase=4;
    }
    private static void verifyBoardAndAscent(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);var st=scene.loop;
        ActivityWorld.tick(p,scene);if(st.flyingGardenStage!=0)throw new IllegalStateException("Garden skipped Father intro");
        st.flyingGardenVoiceUntil=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        if(st.flyingGardenStage!=1||!(p.getVehicle() instanceof ScenePropEntity craft)||craft.kind()!=ScenePropEntity.GARDEN_MANTIS||craft.getPassengers().size()!=2)
            throw new IllegalStateException("Father/player did not board two-seat mantis: stage="+st.flyingGardenStage+
                " vehicle="+p.getVehicle()+" passengers="+(p.getVehicle()==null?0:p.getVehicle().getPassengers().size())+
                " fathers="+p.serverLevel().getEntitiesOfClass(ParentEntity.class,new AABB(garden.south(35)).inflate(18,65,22)).size());
        st.flyingGardenPhaseUntil=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        if(st.flyingGardenStage!=2)throw new IllegalStateException("Ascent did not start");
        st.flyingGardenPhaseUntil=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        if(st.flyingGardenStage!=3||p.getVehicle()==null||p.getVehicle().getY()<garden.getY()+75||st.flyingGardenHarvestUntil!=0)
            throw new IllegalStateException("Aerial reveal altitude or harvest gate failed");
        frames=0;phase=6;
    }
    private static void verifyGardenFinish(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);var st=scene.loop;
        st.flyingGardenPhaseUntil=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        if(st.flyingGardenStage!=4)throw new IllegalStateException("MotherSaid line did not finish before descent");
        st.flyingGardenPhaseUntil=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        if(st.flyingGardenStage!=5||p.isPassenger()||st.flyingGardenHarvestUntil<=OtherMotherLoop.now(p))
            throw new IllegalStateException("Landing did not restore control and start harvest");
        var plant=p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new AABB(garden).inflate(45,5,45),e->e.kind()==ScenePropEntity.GARDEN_PLANT&&e.slot==0).get(0);
        p.teleportTo(p.serverLevel(),plant.getX(),plant.getY(),plant.getZ()+1,180,0);LoopProps.interact(p,plant);
        if((st.flyingGardenCollectedMask&1)==0||p.getInventory().countItem(Items.ENCHANTED_GOLDEN_APPLE)!=1)
            throw new IllegalStateException("Harvest reward or duplicate guard failed");
        st.flyingGardenHarvestUntil=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        if(!ActivityWorld.exitReady(scene))throw new IllegalStateException("Garden exit did not open");
        exit(p,garden,45);
        if(p.getInventory().countItem(Items.ENCHANTED_GOLDEN_APPLE)!=1)throw new IllegalStateException("Harvest reward was lost on exit");
        System.out.println("CORALINE_MAGIC_ACTIVITIES_CHECK passed 500 procedural seeds, two 20-round shows, owner-only answers, timeout, jackpot, curtain phases, Father dialogue, music, mantis flight, landing, harvest, exits");
        phase=8;
    }
}
