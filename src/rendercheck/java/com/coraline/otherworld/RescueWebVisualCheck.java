package com.coraline.otherworld;

import com.coraline.feast.*;
import com.coraline.client.RescueClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Standalone physical/hearing checkpoint. No audio ACKs, eyes, key or story completion are fabricated. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class RescueWebVisualCheck {
    private static boolean started;private static volatile boolean ready,stopping;private static BlockPos center;private static int sample,ticks;private static java.util.UUID session;
    private static volatile boolean sawAcceptedMovement;
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST) public static void acceptedStep(TickEvent.PlayerTickEvent event){if(Boolean.getBoolean("coraline.rescueWebVisualCheck")&&ready&&!stopping&&event.phase==TickEvent.Phase.END&&event.player instanceof net.minecraft.server.level.ServerPlayer player){var s=OtherMotherLoop.scene(player);if(!RescueChapter.started(s))return;if(RescueChapter.phase(s)==RescueChapter.Phase.WEB_STEALTH&&RescueChapter.data(s).getInt("LastWebPackets")>0)sawAcceptedMovement=true;if(RescueChapter.phase(s)==RescueChapter.Phase.DOOR_KICK)report(Minecraft.getInstance());}}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("coraline.rescueWebVisualCheck")||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.player==null||mc.getSingleplayerServer()==null||mc.player.tickCount<60)return;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(2);mc.options.framerateLimit().set(30);mc.options.hideGui=true;if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)mc.setScreen(null);
        if(!started){started=true;mc.getSingleplayerServer().execute(()->stage(mc));return;}if(!ready||stopping)return;ticks++;mc.options.keyUp.setDown(false);mc.options.keyShift.setDown(false);
        if(RescueClient.phase().equals("WEB_STEALTH")){if(!java.util.Objects.equals(session,RescueClient.inputSession())){session=RescueClient.inputSession();sample=0;}mc.options.keyShift.setDown(true);var local=RescueWebLayout.point(1,sample/(double)RescueWebLayout.SAMPLES);var target=new Vec3(center.getX()+local.x(),mc.player.getEyeY()-.2,center.getZ()+local.z());if(mc.player.position().subtract(target).horizontalDistanceSqr()<.05&&sample<RescueWebLayout.SAMPLES)sample++;var d=target.subtract(mc.player.getEyePosition());mc.player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));mc.player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())));mc.options.keyUp.setDown(true);}
        if(ticks%100==0)mc.getSingleplayerServer().execute(()->report(mc));
    }
    private static void report(Minecraft mc){if(stopping)return;var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var s=OtherMotherLoop.scene(p);var n=RescueChapter.data(s);System.out.println("RESCUE_WEB_CLIENT_PROGRESS phase="+RescueChapter.phase(s)+" t="+n.getInt("Ticks")+" at="+p.position()+" sample="+sample+" crouching="+p.isCrouching()+" moved="+n.getDouble("LastWebMoved")+" step="+n.getDouble("LastWebStep")+" packets="+n.getInt("LastWebPackets")+" observerSeen="+sawAcceptedMovement+" sticky="+n.getBoolean("LastWebSticky")+" landed="+n.getBoolean("LastWebLanded")+" alarms="+n.getInt("HearingNoiseEvents")+" retries="+n.getInt("RetryCount")+" reason="+n.getString("LastRetryReason"));
        if(RescueChapter.phase(s)==RescueChapter.Phase.DOOR_KICK){boolean quiet=sawAcceptedMovement&&n.getInt("RetryCount")==0&&n.getInt("HearingNoiseEvents")==0;System.out.println(quiet?"RESCUE_WEB_CLIENT_NATIVE_COMPLETE actual accepted movement observer, continuous crouch, zero false alarms/retries; NOT a full story playthrough":"RESCUE_WEB_CLIENT_NATIVE_FAILED quiet route raised an alarm/retry or observer never ran");stopping=true;mc.execute(mc::stop);}
        else if(n.getInt("RetryCount")>=2){System.out.println("RESCUE_WEB_CLIENT_NATIVE_FAILED repeated physical/hearing retry; no completion accepted");stopping=true;mc.execute(mc::stop);}
    }
    private static void stage(Minecraft mc){
        var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);p.removeAllEffects();var s=new VisitData.Scene();s.owner=p.getUUID();s.door=java.util.UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.loop.event.putInt("Serial",73);VisitData.get(p.server).scenes.put(s.owner,s);p.getPersistentData().putUUID("CoralineVisitDoor",s.door);var old=FirstEscapeStory.data(s);old.putBoolean("Started",true);old.putBoolean("Complete",true);old.putString("Phase","ESCAPE_COMPLETE");var parent=ParentRescueStory.data(s);parent.putBoolean("Started",true);parent.putBoolean("Complete",true);var n=RescueChapter.data(s);n.putBoolean("Started",true);n.putBoolean("BeldamBlind",true);n.putFloat("BeldamMorph",1);n.putBoolean("WebEntered",true);center=RescueWebSite.center(p,s);n.putLong("Garden",center.asLong());for(int i=0;i<65&&!RescueWebSite.prepare(p,s);i++);if(!n.getBoolean("WebReady"))throw new IllegalStateException("Original web geometry did not prepare");var l=p.server.getLevel(ActivityWorld.KEY);var at=RescueWebSite.at(p,s,1,0);double top=at.y;for(var shape:l.getBlockCollisions(null,new AABB(at.x-.3,at.y-.3,at.z-.3,at.x+.3,at.y+.5,at.z+.3)))top=Math.max(top,shape.max(net.minecraft.core.Direction.Axis.Y));p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.setNoGravity(false);p.setInvulnerable(false);p.setForcedPose(null);p.setPose(net.minecraft.world.entity.Pose.STANDING);p.teleportTo(l,at.x,top,at.z,0,0);p.setOnGround(true);
        var mother=com.coraline.registry.ModEntities.MOM.get().create(l);mother.makeOtherParent(0);mother.getPersistentData().putUUID("RealmDoor",s.door);mother.getPersistentData().putUUID("LoopOwner",s.owner);mother.setNoAi(true);mother.setNoGravity(true);mother.noPhysics=true;mother.setPos(Vec3.atBottomCenterOf(center));l.addFreshEntity(mother);s.mom=mother.getUUID();var web=RescueProps.ensure(p,s,"Web",RescueProps.WEB,0,Vec3.atLowerCornerOf(center));if(web==null)throw new IllegalStateException("Production web mesh missing");web.frame(0,0,1);n.putDouble("PreviousX",p.getX());n.putDouble("PreviousY",p.getY());n.putDouble("PreviousZ",p.getZ());n.putBoolean("PreviousGround",true);RescueChapter.go(p,s,RescueChapter.Phase.WEB_STEALTH);ready=true;
    }
    private RescueWebVisualCheck(){}
}
