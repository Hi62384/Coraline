package com.coraline.feast;

import com.coraline.otherworld.DoorData;
import com.coraline.otherworld.OtherWorld;
import com.coraline.otherworld.RealmData;
import com.coraline.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.Path;

/** Opt-in real-client visual and full 15-ticket carnival-loop check. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class CarnivalActivityVisualCheck {
    private static volatile int phase;
    private static int frames;
    private static VisitData.Scene scene;
    private static BlockPos center;
    private CarnivalActivityVisualCheck(){}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(!Boolean.getBoolean("coraline.carnivalVisualCheck")||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null)return;
        mc.options.hideGui=true;mc.options.renderDistance().set(12);mc.options.pauseOnLostFocus=false;
        if(phase==0){if(mc.getSingleplayerServer().getPlayerList().getPlayers().isEmpty())return;
            phase=1;mc.getSingleplayerServer().execute(()->setup(mc.getSingleplayerServer()));return;}
        if(phase==2&&frames==30&&!com.coraline.client.VisitAudioClient.hasVoice(ActivityWorld.introSound(OtherMotherActivities.Activity.CARNIVAL)))
            throw new IllegalStateException("Other Father carnival voice stopped after entry");
        if(phase==2&&++frames>140){
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../artwork/carnival_native.png"));}
            catch(Exception ex){throw new RuntimeException(ex);}
            System.out.println("CORALINE_CARNIVAL_CHECK screenshot saved");phase=3;
            mc.getSingleplayerServer().execute(()->verify(mc.getSingleplayerServer()));
        }
        if(phase==5)mc.stop();
    }
    private static void setup(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();
        var door=new DoorData.Door();door.owner=p.getUUID();door.pos=new BlockPos(29000,80,29000);
        DoorData.get(server).doors.put(door.id,door);
        var r=RealmData.get(server).realm(door);r.ready=true;r.parentGeneration=1;r.porch=r.doorPos;
        var other=server.getLevel(OtherWorld.KEY);
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)other.setBlock(r.doorPos.offset(x,-1,z),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
        scene=new VisitData.Scene();scene.owner=p.getUUID();scene.door=door.id;scene.stage=VisitData.Stage.DONE;
        scene.piano=r.doorPos;scene.table=r.doorPos;scene.loop.generation=1;scene.loop.portal=r.doorPos;
        scene.loop.phase=OtherMotherLoopState.Phase.READY;scene.loop.activity=OtherMotherActivities.Activity.CARNIVAL;
        VisitData.get(server).scenes.put(p.getUUID(),scene);VisitData.get(server).setDirty();
        p.getPersistentData().putUUID("CoralineVisitDoor",door.id);
        p.teleportTo(other,r.doorPos.getX()+.5,r.doorPos.getY(),r.doorPos.getZ()+.5,0,0);
        ActivityWorld.enter(p,scene);
        if(!ActivityWorld.isActivity(p.level()))throw new IllegalStateException("Carnival portal did not transport the player");
        center=CarnivalActivity.center(p,scene);var level=p.serverLevel();
        if(!level.getBlockState(center.below(2)).is(Blocks.PINK_GLAZED_TERRACOTTA)||
           level.isEmptyBlock(center.offset(0,34,-40)))
            throw new IllegalStateException("Carnival or illuminated Ferris wheel was not built");
        int npc=level.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class,new AABB(center).inflate(54,9,54),CarnivalActivity::isCarnivalNpc).size();
        if(npc<40)throw new IllegalStateException("Carnival crowd too small: "+npc);
        int attendants=level.getEntitiesOfClass(FishingContestNpcEntity.class,new AABB(center).inflate(55,12,55),e->e.getPersistentData().getBoolean("CoralineCarnivalAttendant")).size();
        if(attendants!=8)throw new IllegalStateException("Expected six game and two ride attendants, with no food vendors: "+attendants);
        if(level.getEntitiesOfClass(ScenePropEntity.class,new AABB(center).inflate(55,40,55),e->e.kind()==ScenePropEntity.CHAIR&&(e.slot==70||e.slot==71)).size()!=2)
            throw new IllegalStateException("Moving ride seats missing");
        if(level.getEntitiesOfClass(com.coraline.parents.ParentEntity.class,new AABB(center.south(46)).inflate(8),f->f.getPersistentData().getBoolean("CoralineCarnivalFather")).size()!=1)
            throw new IllegalStateException("Other Father did not arrive at the carnival");
        if(CarnivalActivity.exitReady(scene))throw new IllegalStateException("Return portal appeared early");
        System.out.println("CORALINE_CARNIVAL_CHECK arena, crowd and Father spawned");phase=2;
    }
    private static void verify(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);var level=p.serverLevel();var l=scene.loop;
        if(l.carnivalIssued)throw new IllegalStateException("Tickets arrived before Father's voice completed");
        l.carnivalGiftAt=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        if(!l.carnivalIssued||l.carnivalTokensRemaining!=15||p.getInventory().items.stream().noneMatch(s->s.is(ModItems.CARNIVAL_TOKEN.get())))
            throw new IllegalStateException("Father did not grant exactly fifteen tickets");
        var attendant=level.getEntitiesOfClass(FishingContestNpcEntity.class,new AABB(center).inflate(55,12,55),e->e.getPersistentData().getBoolean("CoralineCarnivalAttendant")&&e.getPersistentData().getInt("AttendantSlot")==0).get(0);
        p.teleportTo(level,attendant.getX(),center.getY(),attendant.getZ()+2,180,0);
        l.carnivalLastActionAt=OtherMotherLoop.now(p)-10;CarnivalActivity.interactNpc(p,attendant);
        if(l.carnivalTokensRemaining!=14||CarnivalActivity.exitReady(scene))throw new IllegalStateException("Game charge or portal timing failed");
        var ammo=p.getInventory().items.stream().filter(st->st.is(ModItems.CARNIVAL_RING.get())).findFirst().orElseThrow();
        CarnivalActivity.throwProjectile(p,ammo,false);
        if(level.getEntitiesOfClass(net.minecraft.world.entity.projectile.Snowball.class,new AABB(p.blockPosition()).inflate(5),e->e.getPersistentData().getBoolean("CarnivalThrown")).isEmpty())
            throw new IllegalStateException("Ring was not thrown into the world");
        l.carnivalGameUntil=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        if(l.carnivalBooth!=-1)throw new IllegalStateException("Challenge timeout did not resolve");
        var food=CarnivalActivity.Booth.POPCORN;
        for(int i=0;i<14;i++){
            p.teleportTo(level,center.getX()+food.x+.5,center.getY(),center.getZ()+food.z+4.5,180,0);
            l.carnivalLastActionAt=OtherMotherLoop.now(p)-10;
            CarnivalActivity.interactBlock(p,center.offset(food.x,0,food.z+2));
            if(l.carnivalTokensRemaining!=13-i)throw new IllegalStateException("Self-serve food token ledger failed at purchase "+i);
        }
        if(!CarnivalActivity.exitReady(scene)||l.carnivalTokensRemaining!=0||
            LoopProps.nearby(level,scene,center.south(49)).stream().noneMatch(e->e.kind()==ScenePropEntity.ACTIVITY_EXIT))
            throw new IllegalStateException("Fifteenth ticket did not open the return portal");
        if(p.getInventory().items.stream().anyMatch(s->s.is(ModItems.CARNIVAL_TOKEN.get())))
            throw new IllegalStateException("Unspent issued tickets remain after the ledger emptied");
        ActivityWorld.exit(p,scene);
        if(!OtherWorld.isOther(p.level())||scene.loop.phase!=OtherMotherLoopState.Phase.COMPLETE||!level.isEmptyBlock(center.below()))
            throw new IllegalStateException("Carnival did not clean up and return to the Other World");
        System.out.println("CORALINE_CARNIVAL_CHECK passed voice, NPC-free food, physical ring, 15-token economy, portal and return");phase=5;
    }
}
