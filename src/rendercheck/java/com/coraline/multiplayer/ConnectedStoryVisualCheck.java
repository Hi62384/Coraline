package com.coraline.multiplayer;

import com.coraline.feast.*;
import com.coraline.otherworld.*;
import com.coraline.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;

/** Two real socket-connected clients. QA-only disposable checkpoint, never a
 * substitute for a full multiplayer story playthrough or ending proof. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class ConnectedStoryVisualCheck {
    private static boolean started; private static volatile boolean done; private static volatile Throwable failure;
    private static int phase, age,lastServerTick=-1; private static UUID ownerId,guestId;
    private static ServerPlayer oldGuest; private static VisitData.Scene scene;
    private static final BlockPos SOURCE=new BlockPos(30000,110,30000);
    private static final BlockPos DEST=new BlockPos(30200,110,30200);
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("coraline.connectedStoryCheck")||e.phase!=TickEvent.Phase.END)return;
        if(failure!=null)throw new IllegalStateException("Connected story fixture failed",failure);
        var mc=Minecraft.getInstance();if(mc.player==null||mc.getSingleplayerServer()==null||mc.player.tickCount<60)return;
        if(Boolean.getBoolean("coraline.connectedTransactionsCheck"))ConnectedActivityVisualCheck.capture(mc);
        mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(2);
        if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)mc.setScreen(null);
        if(done){System.out.println("CONNECTED_STORY_NATIVE_COMPLETE: two real TCP clients; native guest admission, shared camera/control, genuine recording ACK, reconnect, per-player reservations and return. Disposable checkpoint, NOT full multiplayer story acceptance.");mc.stop();return;}
        mc.getSingleplayerServer().execute(()->{
            try{
            if(!started){started=true;prepare(mc);return;}
            advance(mc);
            }catch(Throwable error){failure=error;}
        });
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void floor(net.minecraft.server.level.ServerLevel l,BlockPos base){
        for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){var p=base.offset(x,0,z);l.getChunkAt(p);l.setBlock(p.below(),Blocks.OAK_PLANKS.defaultBlockState(),2);for(int y=0;y<4;y++)l.setBlock(p.above(y),Blocks.AIR.defaultBlockState(),2);}
    }
    private static void prepare(Minecraft mc){
        var server=mc.getSingleplayerServer();var owner=server.getPlayerList().getPlayers().get(0);ownerId=owner.getUUID();
        server.getPlayerList().op(owner.getGameProfile());floor(server.overworld(),SOURCE);var other=server.getLevel(OtherWorld.KEY);floor(other,DEST);
        var door=new DoorData.Door();door.owner=ownerId;door.pos=SOURCE;door.placed=true;door.activeUntil=Long.MAX_VALUE;DoorData.get(server).doors.put(door.id,door);
        server.overworld().setBlock(SOURCE,ModBlocks.TINY_DOOR.get().defaultBlockState(),2);((DoorBlockEntity)server.overworld().getBlockEntity(SOURCE)).doorId=door.id;
        var realm=new RealmData.Realm();realm.door=door.id;realm.sourceDoor=SOURCE;realm.house=DEST;realm.doorPos=DEST;realm.porch=DEST.north(4);realm.bed=DEST.south(4);realm.table=realm.piano=DEST;realm.radius=64;realm.ready=realm.islandComplete=true;RealmData.get(server).realms.put(door.id,realm);
        scene=new VisitData.Scene();scene.owner=ownerId;scene.door=door.id;scene.table=scene.piano=DEST;scene.stage=VisitData.Stage.DONE;scene.loop.phase=OtherMotherLoopState.Phase.READY;VisitData.get(server).scenes.put(ownerId,scene);
        var mother=com.coraline.registry.ModEntities.MOM.get().create(other);mother.makeOtherParent(0);mother.getPersistentData().putUUID("RealmDoor",door.id);mother.getPersistentData().putUUID("LoopOwner",ownerId);mother.setPos(Vec3.atBottomCenterOf(DEST.east(4)));require(other.addFreshEntity(mother),"Disposable native mother unavailable");scene.mom=mother.getUUID();
        owner.getPersistentData().putUUID("CoralineVisitDoor",door.id);owner.setGameMode(GameType.ADVENTURE);owner.teleportTo(other,DEST.getX()+.5,DEST.getY(),DEST.getZ()+.5,0,0);
        require(StorySessionData.get(server).observe(owner,scene),"Owner session not observed");
        // This fresh QA server accepts synthetic local profiles only. Bind the
        // actual listener to loopback; never publish a LAN broadcast/server.
        server.setUsesAuthentication(false);
        try{server.getConnection().startTcpServerListener(java.net.InetAddress.getByName("127.0.0.1"),25570);}catch(java.io.IOException ex){throw new IllegalStateException(ex);}
        System.out.println("CONNECTED_STORY_HOST_READY: 127.0.0.1:25570");phase=1;age=0;
    }
    private static void advance(Minecraft mc){
        var server=mc.getSingleplayerServer();if(server.getTickCount()==lastServerTick)return;lastServerTick=server.getTickCount();var owner=server.getPlayerList().getPlayer(ownerId);var data=StorySessionData.get(server);age++;
        var guest=server.getPlayerList().getPlayers().stream().filter(p->!p.getUUID().equals(ownerId)).findFirst().orElse(null);
        if(phase==1){
            if(guest==null){require(age<18000,"No actual guest connected within 15 minutes");return;}
            require(guest.getGameProfile().getName().equals("CoralineQAGuest"),"Unexpected QA guest");guestId=guest.getUUID();guest.setGameMode(GameType.ADVENTURE);guest.getInventory().clearContent();guest.getInventory().add(new ItemStack(Items.EMERALD,7));guest.teleportTo(server.overworld(),SOURCE.getX()+2.5,SOURCE.getY(),SOURCE.getZ()+.5,0,0);
            require(!data.request(guest,owner,scene.door),"Unpermitted guest admitted");require(data.permit(owner,guestId,true),"Native permit failed");
            require(StoryCoop.touchDoor(guest,(DoorBlockEntity)server.overworld().getBlockEntity(SOURCE)),"Native door interaction failed");phase=2;age=0;
        }else if(phase==2){
            require(guest!=null,"Guest disconnected before admission");if(!data.memberState(guestId).equals("ACTIVE")){require(age<200,"Native guest admission stalled");return;}
            require(guest.serverLevel()==owner.serverLevel()&&data.scene(guest)==scene&&!VisitData.get(server).scenes.containsKey(guestId),"Guest received a second director or wrong realm");
            owner.getPersistentData().putUUID("CoralineCoopDebugDoor",scene.door);owner.getPersistentData().putLong("CoralineCoopDebugUntil",server.overworld().getGameTime()+200);
            phase=3;age=0;System.out.println("CONNECTED_STORY_ADMISSION_OK: actual guest, one owner scene");
        }else if(phase==3&&age>=30){
            boolean locked=guest!=null&&StoryCoop.locked(guest)&&guest.getCamera()==owner&&guest.position().distanceTo(owner.position())<.1;
            if(!locked){require(age<100,"Actual guest camera/control presentation failed: guest="+(guest==null?"missing":guest.position()+" camera="+guest.getCamera().getUUID()+" locked="+StoryCoop.locked(guest)+" cinematic="+guest.getPersistentData().getBoolean("CoopCinematic"))+" owner="+owner.position()+" frame="+data.frame(guestId));return;}
            require(ShowPlayback.start(owner,scene,"missing_parents_cat_a",false),"Actual shared recording did not start");phase=4;age=0;
        }else if(phase==4){
            require(guest!=null,"Guest disconnected during actual recording");if(!ShowPlayback.tick(owner,scene)){require(age<400,"Owner actual recording ACK did not complete");return;}
            owner.getPersistentData().remove("CoralineCoopDebugUntil");phase=5;age=0;System.out.println("CONNECTED_STORY_CAMERA_AUDIO_OK: server camera lock and owner mixer completion, not human listening");
        }else if(phase==5&&age>=30){
            require(guest!=null&&!StoryCoop.locked(guest)&&guest.getCamera()==guest,"Guest presentation failed to release");require(data.checkpoint(server),"Pre-disconnect native checkpoint failed");oldGuest=guest;guest.connection.disconnect(Component.literal("Disposable QA reconnect checkpoint"));phase=6;age=0;System.out.println("CONNECTED_STORY_RECONNECT_REQUIRED: restart same CoralineQAGuest profile on 127.0.0.1:25570");
        }else if(phase==6){
            if(guest==null||guest==oldGuest){require(age<18000,"Actual guest did not reconnect within 15 minutes");return;}
            require(guest.getUUID().equals(guestId)&&data.scene(guest)==scene&&data.memberState(guestId).equals("ACTIVE"),"Reconnect lost authoritative guest membership");
            require(guest.getInventory().countItem(Items.EMERALD)==7,"Reconnect changed original guest inventory");scene.loop.event.putInt("Serial",scene.loop.event.getInt("Serial")+1);require(data.observe(owner,scene),"New owner checkpoint failed");var f=data.frame(ownerId);
            require(data.reserveReward(owner,f.id(),f.phase(),"connected_qa").isPresent()&&data.reserveReward(guest,f.id(),f.phase(),"connected_qa").isPresent(),"Independent participant reservations failed");require(data.reserveReward(guest,f.id(),f.phase(),"connected_qa").isEmpty(),"Duplicate guest reward reservation accepted");
            if(Boolean.getBoolean("coraline.connectedTransactionsCheck")){ConnectedActivityVisualCheck.begin(owner,guest,scene);phase=8;age=0;}
            else{require(data.requestReturn(guest),"Native return refused");StoryCoop.clear(guest);require(data.restore(guest),"Native guest return transaction failed");phase=7;age=0;}
        }else if(phase==8){
            if(ConnectedActivityVisualCheck.tick(owner,guest,scene)){require(guest!=null&&data.requestReturn(guest),"Native transaction guest return refused");StoryCoop.clear(guest);require(data.restore(guest),"Transaction guest return recovery failed");phase=7;age=0;}
        }else if(phase==7&&age>=30){
            require(guest!=null&&!data.guest(guestId)&&guest.serverLevel()==server.overworld()&&guest.position().distanceTo(new Vec3(SOURCE.getX()+2.5,SOURCE.getY(),SOURCE.getZ()+.5))<.2,"Guest original dimension/position not restored");require(guest.getCamera()==guest&&!StoryCoop.locked(guest)&&guest.gameMode.getGameModeForPlayer()==GameType.ADVENTURE&&guest.getInventory().countItem(Items.EMERALD)==7,"Guest mode, control or original inventory not restored");require(data.checkpoint(server),"Final native checkpoint failed");done=true;
        }
    }
    private ConnectedStoryVisualCheck(){}
}
