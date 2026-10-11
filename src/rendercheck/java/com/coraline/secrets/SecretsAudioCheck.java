package com.coraline.secrets;
import com.coraline.feast.*;
import com.coraline.otherworld.*;
import com.coraline.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.nio.file.Path;

/** Test-only: actually play every registered recording once, without truncation. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class SecretsAudioCheck {
    private static int tick,index=-1,next,started;private static volatile boolean ready;private static boolean heard,voiceTab;private static long firstActive,lastActive;private static String current="";
    private static volatile int flow;private static volatile boolean flowBusy;private static int flowTicks,emeralds;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(!Boolean.getBoolean("coraline.secretsAudioCheck")||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.player==null||mc.getSingleplayerServer()==null)return;mc.options.pauseOnLostFocus=false;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);tick++;
        if(index==-1){index=0;mc.getSingleplayerServer().execute(()->{try{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);p.server.getPlayerList().op(p.getGameProfile());var door=new DoorData.Door();door.owner=p.getUUID();door.pos=new BlockPos(76000,80,76000);DoorData.get(p.server).doors.put(door.id,door);var r=RealmData.get(p.server).realm(door);r.house=r.doorPos;r.bed=r.house;r.porch=r.house.south(10);r.ready=true;r.islandComplete=false;r.parentGeneration=1;var level=p.server.getLevel(OtherWorld.KEY);for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)level.setBlock(r.house.offset(x,-1,z),Blocks.DARK_OAK_PLANKS.defaultBlockState(),2);p.teleportTo(level,r.house.getX()+.5,r.house.getY(),r.house.getZ()+.5,0,0);var s=new VisitData.Scene();s.owner=p.getUUID();s.door=door.id;s.stage=VisitData.Stage.DONE;s.table=r.house;s.piano=r.house;s.loop.phase=OtherMotherLoopState.Phase.EXPLORING;s.loop.visits=1;s.loop.secrets.visit=1;s.loop.secrets.journal.addAll(SecretRegistry.ALL.keySet());s.loop.secrets.recalculate();s.loop.secrets.invisibleUntil=Long.MAX_VALUE;s.loop.secrets.world.putLong("RemarkAfter",Long.MAX_VALUE);VisitData.get(p.server).scenes.put(p.getUUID(),s);p.getPersistentData().putUUID("CoralineVisitDoor",door.id);var m=ModEntities.MOM.get().create(level);m.makeOtherParent(0);m.getPersistentData().putUUID("RealmDoor",door.id);m.getPersistentData().putInt("RealmGeneration",1);m.getPersistentData().putUUID("LoopOwner",p.getUUID());m.setPos(p.getX()+3,p.getY(),p.getZ());level.addFreshEntity(m);s.mom=m.getUUID();SecretsNetwork.open(p,true,false);ready=true;}catch(Throwable ex){com.mojang.logging.LogUtils.getLogger().error("SECRETS40_AUDIO_FAILED",ex);mc.execute(mc::stop);}});return;}
        if(!ready)return;
        if(flow>0){flow(mc);return;}
        if(!voiceTab&&mc.screen instanceof com.coraline.client.SecretJournalScreen){for(var widget:mc.screen.children())if(widget instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals("Voice")){b.onPress();voiceTab=true;break;}}
        if(!current.isEmpty()&&com.coraline.client.VisitAudioClient.hasVoice(current)&&sound()!=null&&mc.getSoundManager().isActive(sound())){heard=true;lastActive=System.nanoTime();if(firstActive==0)firstActive=lastActive;}
        if(tick<next)return;
        if(!current.isEmpty()&&!heard){com.mojang.logging.LogUtils.getLogger().error("SECRETS40_AUDIO_FAILED: not active {}",current);mc.stop();return;}
        if(!current.isEmpty()&&(lastActive-firstActive)/1.0e9<VisitAudio.length(current)/20.0-1){com.mojang.logging.LogUtils.getLogger().error("SECRETS40_AUDIO_FAILED: early end {} after {}s",current,(lastActive-firstActive)/1.0e9);mc.stop();return;}
        if(!current.isEmpty())com.mojang.logging.LogUtils.getLogger().info("SECRETS40_PLAYED {}",current);
        if(index>=60){com.mojang.logging.LogUtils.getLogger().info("SECRETS40_AUDIO_PASSED: all 60 streamed recordings played in full");mc.stop();return;}
        if(index==1){try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../artwork/secrets40_voice.png"));}catch(Exception ex){throw new RuntimeException(ex);}if(Boolean.getBoolean("coraline.secretsUiCheck")){if(!voiceTab)throw new IllegalStateException("Voice tab not opened");com.mojang.logging.LogUtils.getLogger().info("SECRETS40_UI_PASSED");flow=1;return;}}
        int e=index/10,s=(index%10)/2,v=index%2;current=SecretDialogue.pool(SecretDialogue.Event.values()[e],OtherMotherWelcome.State.values()[s]).sound(v);started=tick;heard=false;firstActive=lastActive=0;next=tick+VisitAudio.length(current)+40;index++;
        mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);SecretDialogue.preview(p,e,s,v);});
        if(index==1)mc.execute(()->{if(mc.screen!=null)for(var widget:mc.screen.children())if(widget instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals("Voice")){b.onPress();break;}});
    }
    private static void flow(Minecraft mc){
        if(++flowTicks>2400){com.mojang.logging.LogUtils.getLogger().error("SECRETS40_FLOW_FAILED: timeout at {}",flow);mc.stop();return;}
        if(flowBusy||tick%10!=0)return;
        if(flow==3){if(mc.screen!=null)for(var widget:mc.screen.children())if(widget instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals("Choose emeralds")){try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../artwork/secrets40_reward.png"));}catch(Exception ex){throw new RuntimeException(ex);}b.onPress();flow=4;return;}return;}
        flowBusy=true;mc.getSingleplayerServer().execute(()->{try{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var s=SecretTransactions.state(p);var n=s.dialogue;
            if(flow==1||flow==5){if(SecretDialogue.busy(p))return;String id=flow==1?"house_codes":"soulglass";s.journal.add(id);if(!SecretTransactions.makeBlackmail(p,java.util.List.of(id)))throw new IllegalStateException("Blackmail setup failed");var item=p.getInventory().items.stream().filter(i->i.is(SecretsItems.BLACKMAIL.get())&&SecretTransactions.owned(p,i)&&s.blackmail.containsKey(i.getTag().getUUID("SecretToken"))).findFirst().orElseThrow();if(flow==1)emeralds=p.getInventory().countItem(net.minecraft.world.item.Items.EMERALD);if(!SecretTransactions.offer(p,item,flow==1?1:0))throw new IllegalStateException("Offer refused");flow++;}
            else if(flow==2&&s.pendingReward>=0){if(!n.getBoolean("Finished"))throw new IllegalStateException("Reward picker opened before real playback acknowledgement");flow=3;}
            else if(flow==4&&s.pendingReward<0&&!SecretDialogue.busy(p)&&n.getBoolean("Finished")&&n.getString("Playing").contains("secrets_accept_")){int total=p.getInventory().countItem(net.minecraft.world.item.Items.EMERALD);if(total<=emeralds)throw new IllegalStateException("Missing accepted payout");emeralds=total;flow=5;}
            else if(flow==6&&!SecretDialogue.busy(p)&&n.getBoolean("Finished")&&n.getString("Playing").contains("secrets_reject_")){if(p.getInventory().countItem(net.minecraft.world.item.Items.EMERALD)!=emeralds||s.pendingReward>=0||!s.exposed.containsAll(java.util.List.of("house_codes","soulglass")))throw new IllegalStateException("Rejection/exposure mismatch");com.mojang.logging.LogUtils.getLogger().info("SECRETS40_FLOW_PASSED: real offer -> reward UI click -> acceptance, and offer -> rejection, all voices completed");mc.execute(mc::stop);}
        }catch(Throwable ex){com.mojang.logging.LogUtils.getLogger().error("SECRETS40_FLOW_FAILED",ex);mc.execute(mc::stop);}finally{flowBusy=false;}});
    }
    private static net.minecraft.client.resources.sounds.SoundInstance sound(){try{var f=com.coraline.client.VisitAudioClient.class.getDeclaredField("voice");f.setAccessible(true);return (net.minecraft.client.resources.sounds.SoundInstance)f.get(null);}catch(Exception ex){throw new RuntimeException(ex);}}
}
