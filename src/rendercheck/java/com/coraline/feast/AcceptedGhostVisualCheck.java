package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Native sewing and ghost entry. Activities between these scenes are an
 * explicitly declared operator checkpoint, never a canonical ending witness. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class AcceptedGhostVisualCheck {
    private static boolean requested,ready,done,ghostCheckpoint,left;
    private static volatile Throwable failure;
    private static volatile String capture;
    private static VisitData.Scene scene;
    private static int ticks,age,exitTicks;
    private static String phase="";
    private static ListTag inventory;
    private static java.util.UUID escrow;
    private static final java.util.Set<String> audible=new java.util.HashSet<>();
    private static final java.util.Set<String> phases=new java.util.HashSet<>();
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("coraline.acceptedGhostCheck")||e.phase!=TickEvent.Phase.END)return;
        if(failure!=null)throw new IllegalStateException("Accepted ghost native fixture",failure);
        var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(2);
        if(mc.player==null||mc.getSingleplayerServer()==null||mc.player.tickCount<60)return;
        if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)mc.setScreen(null);
        if(!requested){requested=true;mc.getSingleplayerServer().execute(()->{try{prepare(mc.getSingleplayerServer().getPlayerList().getPlayers().get(0));ready=true;}catch(Throwable bad){failure=bad;}});}
        var shot=capture;if(shot!=null){capture=null;try{var folder=java.nio.file.Path.of("../artwork/accepted_ghost_qa");java.nio.file.Files.createDirectories(folder);try(var pixels=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){pixels.writeToFile(folder.resolve(shot+".png"));}}catch(Exception bad){failure=bad;}}
        if(done){System.out.println("ACCEPTED_GHOST_NATIVE_COMPLETE: actual online sewing chair/procedure/escrow; explicit post-activity checkpoint; genuine C/D/E playback, manual nonce/range/look-validated prison exit, spectator control and bounded native reconstruction. NOT activities, full ghost escape, ending or reset acceptance.");mc.stop();}
    }
    private static void prepare(ServerPlayer p){
        p.server.getPlayerList().op(p.getGameProfile());var l=p.server.getLevel(OtherWorld.KEY);var center=new BlockPos(28800,100,28800);
        var d=new DoorData.Door();d.owner=p.getUUID();d.placed=true;d.pos=center;d.activeUntil=Long.MAX_VALUE;DoorData.get(p.server).doors.put(d.id,d);
        var r=new RealmData.Realm();r.door=d.id;r.ready=true;r.islandComplete=true;r.house=center;r.radius=150;r.sourceDoor=d.pos;AcceptedGhostHouseFixture.build(l,center,d,r);RealmData.get(p.server).realms.put(d.id,r);RealmData.get(p.server).setDirty();DoorData.get(p.server).setDirty();
        scene=new VisitData.Scene();scene.owner=p.getUUID();scene.door=d.id;scene.table=r.table;scene.piano=r.piano;scene.stage=VisitData.Stage.DONE;scene.loop.phase=OtherMotherLoopState.Phase.READY;VisitData.get(p.server).scenes.put(scene.owner,scene);
        p.getPersistentData().putUUID("CoralineVisitDoor",d.id);p.setGameMode(GameType.ADVENTURE);p.teleportTo(l,center.getX()+.5,center.getY()+1,center.getZ()+.5,0,0);
        var m=ModEntities.MOM.get().create(l);m.makeOtherParent(0);m.getPersistentData().putUUID("RealmDoor",d.id);m.getPersistentData().putUUID("LoopOwner",p.getUUID());m.setPos(center.getCenter());require(l.addFreshEntity(m),"Original fixture mother unavailable");scene.mom=m.getUUID();
        p.getInventory().setItem(0,new ItemStack(Items.EMERALD,7));p.getInventory().setItem(1,new ItemStack(Items.IRON_PICKAXE));inventory=p.getInventory().save(new ListTag());
        require(AcceptedButtonEyes.begin(p,scene,AcceptedButtonEyesProgress.Route.ACCEPTED_PROPOSAL),"Native accepted route did not begin");
        System.out.println("ACCEPTED_GHOST_NATIVE_STARTED: disposable Rosewood house and original actors; normal sewing; later explicit post-activities checkpoint");
    }
    private static void move(ServerPlayer p,Vec3 at,Vec3 target){var delta=target.subtract(at.add(0,p.getEyeHeight(),0));p.teleportTo(p.serverLevel(),at.x,at.y,at.z,(float)Math.toDegrees(Math.atan2(-delta.x,delta.z)),(float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance())));}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void server(TickEvent.PlayerTickEvent e){
        if(!Boolean.getBoolean("coraline.acceptedGhostCheck")||!ready||done||failure!=null||e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p))return;
        try{
            require(++ticks<18000,"Native accepted/ghost check exceeded bound at "+phase);var n=AcceptedButtonEyes.data(scene);var current=AcceptedButtonEyesProgress.phase(n).name();
            var playback=ShowPlayback.data(scene);if(playback.getBoolean("Started"))audible.add(playback.getString("Sound"));
            require(!playback.getBoolean("SafetySkipped")&&n.getString("LastSafetySkip").isEmpty(),"Native accepted scene needed playback safety skip");
            if(!current.equals(phase)){phase=current;age=0;phases.add(phase);System.out.println("ACCEPTED_GHOST_NATIVE_PHASE "+phase);}age++;
            if(age==20)capture=phase.toLowerCase(java.util.Locale.ROOT);
            if(phase.equals("WAITING_TO_SIT")){
                require(inventory.equals(p.getInventory().save(new ListTag())),"Inventory confiscated before actual sewing");var seat=FirstEscapeScene.prop(p,scene,"SewingChair");
                if(seat!=null&&age>20){move(p,seat.position().add(0,0,1),seat.position().add(0,.6,0));require(AcceptedButtonEyes.interact(p,scene,seat)&&p.isPassenger(),"Native chair interaction failed");
                    // With Mother already beside the chair this native phase
                    // legitimately completes before the next observer tick.
                    require(AcceptedButtonEyesProgress.phase(n)==AcceptedButtonEyesProgress.Phase.APPROACH_CHAIR,"Chair interaction missed native approach phase");phases.add("APPROACH_CHAIR");}
            }
            if(java.util.Set.of("TAKE_BUTTONS","TAKE_NEEDLE","RAISE_NEEDLE","BLACKOUT","PROCEDURE_COMPLETE").contains(phase))require(p.isPassenger(),"Sewing lost its native chair hold at "+phase);
            if(phase.equals("MEET_UPSTAIRS")&&!ghostCheckpoint){
                for(String required:java.util.List.of("BASEMENT_INVITATION","WAITING_TO_SIT","APPROACH_CHAIR","TAKE_BUTTONS","TAKE_NEEDLE","RAISE_NEEDLE","BLACKOUT","PROCEDURE_COMPLETE"))require(phases.contains(required),"Missing normal sewing beat "+required);
                require(n.getBoolean("SewingCompleted")&&n.getBoolean("EscrowStored")&&AcceptedButtonEyesInventory.locked(p)&&!p.isPassenger(),"Actual procedure did not finish/release/lock inventory");
                require(n.getCompound("InventoryEscrow").getList("Inventory",Tag.TAG_COMPOUND).equals(inventory),"Escrow changed original inventory");escrow=n.getCompound("InventoryEscrow").getUUID("Transaction");
                require(p.getInventory().countItem(Items.EMERALD)==0,"Confiscated item still usable");ghostCheckpoint=true;
                System.out.println("ACCEPTED_GHOST_NATIVE_SEWING_OK; staging explicit post-activity prison checkpoint, no activity/ending credit");p.setYRot(0);p.setXRot(0);require(AcceptedButtonEyes.debug(p,"ghosts").contains("applied"),"Explicit prison checkpoint unavailable");
            }
            if(phase.equals("ACCEPTED_BUTTON_EYES_MIRROR_PRISON")){
                var g=GhostVictimEntry.data(scene);if(!g.getBoolean("Started"))return;
                if(!left){
                    require(audible.contains(FirstEscapeTimeline.sound(FirstEscapeTimeline.Phase.MIRROR_C))&&audible.contains(FirstEscapeTimeline.sound(FirstEscapeTimeline.Phase.MIRROR_D))&&audible.contains(AcceptedButtonEyes.ACCEPTED_GHOST_E),"Missing actual ghost recordings");
                    require(!GhostVictimEntry.leave(p,scene,java.util.UUID.randomUUID()),"Foreign ghost exit nonce accepted");
                    if(age<60){require(GhostVictimProgress.phase(g)==GhostVictimProgress.Phase.ENTER_MIRROR&&!p.isSpectator(),"Waiting silently exited prison");return;}
                    var target=GhostVictimEntry.prisonMirror(scene).getCenter();move(p,target.add(0,-1,2),target);
                    var delta=target.subtract(p.getEyePosition());var hall=GhostVictimWorld.hall(scene);var safe=OtherWorld.safePlayer(p.serverLevel(),hall);
                    var evidence=" owned="+GhostVictimMirrors.owned(p,scene,g)+" phase="+GhostVictimProgress.phase(g)+" range="+delta.length()+" look="+p.getLookAngle().dot(delta.normalize())+" player="+p.position()+" yaw="+p.getYRot()+" pitch="+p.getXRot()+" hall="+hall+" safe="+safe+" safeDistance="+safe.distSqr(hall);
                    System.out.println("ACCEPTED_GHOST_EXIT_ATTEMPT"+evidence);
                    require(GhostVictimEntry.leave(p,scene,g.getUUID("Session")),"Native manual mirror exit failed"+evidence);require(p.isSpectator()&&GhostVictimProgress.phase(g)==GhostVictimProgress.Phase.EMPTY_WORLD,"Ghost exit did not preserve free spectator control");left=true;capture="manual_ghost_exit";
                }else{
                    exitTicks++;require(p.isSpectator()&&AcceptedButtonEyesInventory.locked(p)&&escrow.equals(n.getCompound("InventoryEscrow").getUUID("Transaction")),"Ghost controls/custody changed during reconstruction");
                    if(exitTicks==40){var far=GhostVictimWorld.hall(scene).getCenter().add(1000,0,0);move(p,far,far.add(0,0,1));}
                    if(exitTicks==45)require(p.position().distanceToSqr(GhostVictimWorld.hall(scene).getCenter())<128*128,"Ghost escaped owner-local bounds");
                    var gp=GhostVictimProgress.phase(g);if(gp==GhostVictimProgress.Phase.RECONSTRUCTION&&exitTicks%200==0)capture="reconstruction_"+exitTicks;
                    if(gp==GhostVictimProgress.Phase.WAITING_FOR_VICTIM){require(GhostVictimProgress.hasVictim(g),"Natural reconstruction lost next-victim identity");capture="waiting_for_victim";done=true;}
                }
            }
        }catch(Throwable bad){failure=bad;}
    }
    private AcceptedGhostVisualCheck(){}
}
