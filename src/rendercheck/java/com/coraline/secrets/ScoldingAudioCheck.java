package com.coraline.secrets;

import com.coraline.feast.*;
import com.coraline.otherworld.*;
import com.coraline.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.nio.file.Path;
import java.util.*;

/** Opt-in live client QA. No fake completion acknowledgements or shortened recordings. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class ScoldingAudioCheck {
    private static int tick,preview,flow=-1,flowTicks,checks;
    private static volatile boolean ready,busy;
    private static String current="",observed="",lastPhase="";
    private static boolean heard,scoldTab,shot;
    private static long first,last;
    private static int next;
    private static float trust,suspicion;
    private static Set<String> exposed;
    private static int emeralds;
    private static final Set<String> completed=new HashSet<>();
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("coraline.scoldingAudioCheck")||e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.getSingleplayerServer()==null)return;
        mc.options.pauseOnLostFocus=false;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);tick++;
        if(tick==1){server(mc,()->setup(mc.getSingleplayerServer().getPlayerList().getPlayers().get(0)));return;}
        if(!ready)return;
        if(!scoldTab&&mc.screen instanceof com.coraline.client.SecretJournalScreen){for(var widget:mc.screen.children())if(widget instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals("Scold")){b.onPress();scoldTab=true;break;}}
        if(scoldTab&&!shot&&tick>80){try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../artwork/scolding41_debug.png"));shot=true;}catch(Exception ex){fail(mc,ex);}}
        var sound=sound();String active=sound==null?"":sound.getLocation().getPath();
        boolean audible=sound!=null&&mc.getSoundManager().isActive(sound);
        if(!active.equals(observed)){
            if(!observed.isEmpty()&&heard){double seconds=(last-first)/1e9;require(seconds>=VisitAudio.length(observed)/20.0-1,"Truncated "+observed+" at "+seconds);completed.add(observed);com.mojang.logging.LogUtils.getLogger().info("SCOLD41_PLAYED {} {}s",observed,seconds);}
            observed=active;first=last=0;heard=false;
        }
        if(audible){heard=true;last=System.nanoTime();if(first==0)first=last;}
        if(flow<0){
            if(tick<next||busy)return;
            if(!current.isEmpty())require(completed.contains(current),"Preview never completed: "+current);
            if(preview>=10){require(scoldTab,"Debug tab missing");com.mojang.logging.LogUtils.getLogger().info("SCOLD41_PREVIEWS_PASSED all ten recordings");flow=0;mc.setScreen(null);return;}
            int i=preview++;current=Reconciliation.pools().get(i).sound(0);next=tick+VisitAudio.length(current)+60;
            server(mc,()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);Reconciliation.preview(p,i/5,i%5);});return;
        }
        if(++flowTicks>4200){fail(mc,new IllegalStateException("Flow timeout "+flow+" "+lastPhase));return;}
        if(mc.screen instanceof com.coraline.client.SecretJournalScreen){for(var widget:mc.screen.children())if(widget instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals("Choose emeralds")){b.onPress();break;}}
        if(tick%5==0&&!busy)server(mc,()->flow(mc.getSingleplayerServer().getPlayerList().getPlayers().get(0),mc));
    }
    private static void setup(ServerPlayer p){
        p.server.getPlayerList().op(p.getGameProfile());VisitAudio.stopVoice(p);var door=new DoorData.Door();door.owner=p.getUUID();door.pos=new BlockPos(88000,80,88000);DoorData.get(p.server).doors.put(door.id,door);
        var r=RealmData.get(p.server).realm(door);r.house=r.doorPos;r.bed=r.house;r.porch=r.house;r.ready=true;r.islandComplete=false;r.parentGeneration=1;
        var level=p.server.getLevel(OtherWorld.KEY);for(int x=-32;x<=64;x++)for(int z=-32;z<=32;z++)level.setBlock(r.house.offset(x,-1,z),Blocks.DARK_OAK_PLANKS.defaultBlockState(),2);
        p.teleportTo(level,r.porch.getX()+.5,r.porch.getY(),r.porch.getZ()+.5,180,0);
        var scene=new VisitData.Scene();scene.owner=p.getUUID();scene.door=door.id;scene.stage=VisitData.Stage.DONE;scene.table=r.house;scene.piano=r.house;scene.loop.phase=OtherMotherLoopState.Phase.EXPLORING;scene.loop.visits=1;scene.loop.secrets.visit=1;
        scene.loop.secrets.journal.addAll(SecretRegistry.ALL.keySet());scene.loop.secrets.recalculate();scene.loop.secrets.invisibleUntil=Long.MAX_VALUE;scene.loop.secrets.world.putLong("RemarkAfter",Long.MAX_VALUE);
        VisitData.get(p.server).scenes.put(p.getUUID(),scene);p.getPersistentData().putUUID("CoralineVisitDoor",door.id);
        var m=ModEntities.MOM.get().create(level);m.makeOtherParent(0);m.getPersistentData().putUUID("RealmDoor",door.id);m.getPersistentData().putInt("RealmGeneration",1);m.getPersistentData().putUUID("LoopOwner",p.getUUID());m.setPos(p.getX()+3,p.getY(),p.getZ());level.addFreshEntity(m);scene.mom=m.getUUID();scene.loop.motherPos=m.blockPosition();
        SecretsNetwork.open(p,true,false);int start=Integer.getInteger("coraline.scoldingFlowStart",-1);if(start>=0){flow=start;Minecraft.getInstance().execute(()->Minecraft.getInstance().setScreen(null));}ready=true;
    }
    private static void flow(ServerPlayer p,Minecraft mc){
        var scene=OtherMotherLoop.scene(p);var s=scene.loop.secrets;var n=s.dialogue;
        if(checks==0){
            require(!SecretDialogue.busy(p),"Busy before flow");OtherMotherLoop.clearActivityWindow(p);OtherMotherLoop.relationship(p,.55F,.4F);s.inspectionProgress=0;
            s.world.remove("CaughtEncounter");s.world.remove("CatchUntil");p.getInventory().clearContent();
            var home=SecretWorld.realm(p).porch;var at=flow==1||flow==2?home.east(40):home;p.teleportTo(p.serverLevel(),at.getX()+.5,at.getY(),at.getZ()+.5,180,0);var m=MotherSearch.mother(p);require(m!=null,"Missing mother");m.teleportTo(p.getX()+2,p.getY(),p.getZ());
            if(flow==1){var room=new CompoundTag();room.putLong("Pos",at.asLong());room.putInt("Tier",1);var rooms=new ListTag();rooms.add(room);s.world.put("Rooms",rooms);s.unlocked.add(0);}else s.world.remove("Rooms");
            if(flow==2){s.journal.remove("soulglass");SecretTransactions.issue(p,"soulglass");require(p.getInventory().items.stream().anyMatch(i->!SecretTransactions.evidence(p,i).isEmpty()),"No physical evidence in fixture");}
            if(flow<=2)MotherSearch.catchPlayer(p);
            else if(flow==3){s.search=SecretsState.Search.SEARCHING;m.teleportTo(p.getX()+20,p.getY(),p.getZ());MotherSearch.bell(p);require(n.getString("Queue").isEmpty(),"Bell invented opening");}
            else {String id=flow==4?"house_codes":"soulglass";s.journal.add(id);require(SecretTransactions.makeBlackmail(p,List.of(id)),"Blackmail setup");var item=p.getInventory().items.stream().filter(i->i.is(SecretsItems.BLACKMAIL.get())).findFirst().orElseThrow();require(SecretTransactions.offer(p,item,flow==4?1:0),"Blackmail refused");}
            if(flow<=2)require(n.getString("Queue").contains("secrets_"+new String[]{"ordinary","secret","item"}[flow]+"_mid_mid"),"Wrong opening/state");
            checks=1;lastPhase="";emeralds=p.getInventory().countItem(Items.EMERALD);return;
        }
        String phase=Reconciliation.active(p)?Reconciliation.stage(p):scene.loop.phase.name();if(!phase.equals(lastPhase)){com.mojang.logging.LogUtils.getLogger().info("SCOLD41_FLOW {} {}",flow,phase);lastPhase=phase;}
        if(phase.equals("SCOLDING")&&checks==1){require(n.getCompound("Reconcile").getString("Category").equals(flow==0||flow==3?"MINOR":"SERIOUS"),"Wrong category");require(n.getCompound("Reconcile").getString("State").equals(OtherMotherWelcome.State.MID_TRUST_MID_SUSPICION.id()),"State changed");trust=scene.loop.trust;suspicion=scene.loop.suspicion;exposed=Set.copyOf(s.exposed);checks=2;}
        if(phase.equals("PROPOSAL")||phase.equals("WARNING")){require(checks>=2,"Skipped scold");require(p.distanceToSqr(SecretWorld.realm(p).porch.getCenter())<144,"Not home");}
        if(phase.equals("WINDOW")||phase.equals("ARRIVAL")){
            if(p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new net.minecraft.world.phys.AABB(scene.loop.portal).inflate(4),prop->p.getUUID().equals(prop.owner)&&prop.kind()==ScenePropEntity.ACTIVITY_PORTAL).isEmpty())return;
            require(checks==2,"Skipped lifecycle");require(scene.loop.arrivalDeadline-OtherMotherLoop.now(p)>=5960,"Not fresh five-minute window");require(scene.loop.trust==trust&&scene.loop.suspicion==suspicion&&s.exposed.containsAll(exposed),"Forgiveness altered consequences");require(scene.loop.visits==1&&scene.loop.gifts.isEmpty(),"Duplicate visit gift");
            if(flow==4)require(p.getInventory().countItem(Items.EMERALD)>emeralds,"No accepted reward");if(flow==5)require(p.getInventory().countItem(Items.EMERALD)==emeralds,"Rejected reward");
            com.mojang.logging.LogUtils.getLogger().info("SCOLD41_FLOW_PASSED {}",flow);flow++;checks=0;flowTicks=0;
            if(flow==6){com.mojang.logging.LogUtils.getLogger().info("SCOLD41_ALL_PASSED ten previews and six complete live flows");mc.execute(mc::stop);}
        }
    }
    private static void server(Minecraft mc,Runnable action){busy=true;mc.getSingleplayerServer().execute(()->{try{action.run();}catch(Throwable ex){fail(mc,ex);}finally{busy=false;}});}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private static void fail(Minecraft mc,Throwable ex){com.mojang.logging.LogUtils.getLogger().error("SCOLD41_FAILED",ex);mc.execute(mc::stop);}
    private static net.minecraft.client.resources.sounds.SoundInstance sound(){try{var f=com.coraline.client.VisitAudioClient.class.getDeclaredField("voice");f.setAccessible(true);return (net.minecraft.client.resources.sounds.SoundInstance)f.get(null);}catch(Exception ex){throw new RuntimeException(ex);}}
}
