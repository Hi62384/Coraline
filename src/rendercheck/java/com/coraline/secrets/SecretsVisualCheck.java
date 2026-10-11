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
import java.util.*;
import java.nio.file.Path;

@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class SecretsVisualCheck {
    private static void click(Minecraft mc,String label){mc.execute(()->{if(mc.screen!=null)for(var widget:mc.screen.children())if(widget instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals(label)){b.onPress();return;}});}
    private static int ticks,step;private static volatile boolean busy;private static VisitData.Scene scene;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(!Boolean.getBoolean("coraline.secretsVisualCheck")||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.player==null||mc.getSingleplayerServer()==null)return;mc.options.pauseOnLostFocus=false;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);if(busy||++ticks%80!=0)return;
        if(step>0)try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../artwork/secrets39_"+step+".png"));}catch(Exception ex){throw new RuntimeException(ex);}
        if(step==16){com.mojang.logging.LogUtils.getLogger().info("SECRETS39_CLIENT_PASSED");mc.stop();return;}busy=true;mc.getSingleplayerServer().execute(()->{try{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
            if(step==0){p.server.getPlayerList().op(p.getGameProfile());p.getInventory().clearContent();var door=new DoorData.Door();door.owner=p.getUUID();door.pos=new BlockPos(72000,80,72000);DoorData.get(p.server).doors.put(door.id,door);var r=RealmData.get(p.server).realm(door);r.house=r.doorPos;r.bed=r.house;r.porch=r.house.south(10);r.ready=r.islandComplete=true;r.parentGeneration=1;var level=p.server.getLevel(OtherWorld.KEY);for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++)level.setBlock(r.house.offset(x,-1,z),Blocks.DARK_OAK_PLANKS.defaultBlockState(),2);
                scene=new VisitData.Scene();scene.owner=p.getUUID();scene.door=door.id;scene.stage=VisitData.Stage.DONE;scene.table=r.house;scene.piano=r.house;scene.loop.phase=OtherMotherLoopState.Phase.EXPLORING;scene.loop.visits=3;scene.loop.secrets.visit=3;scene.loop.secrets.seed=7238;VisitData.get(p.server).scenes.put(scene.owner,scene);p.getPersistentData().putUUID("CoralineVisitDoor",door.id);p.teleportTo(level,r.house.getX(),r.house.getY(),r.house.getZ(),0,0);
                var mother=ModEntities.MOM.get().create(level);mother.makeOtherParent(0);mother.getPersistentData().putUUID("RealmDoor",door.id);mother.getPersistentData().putInt("RealmGeneration",1);mother.getPersistentData().putUUID("LoopOwner",p.getUUID());mother.setPos(r.house.getX()+5,r.house.getY(),r.house.getZ());level.addFreshEntity(mother);scene.mom=mother.getUUID();
                for(int index=0;index<12;index++){var random=new Random(scene.loop.secrets.seed+index*733L);for(int attempt=0;attempt<24;attempt++){int x=random.nextInt(421)-210,z=random.nextInt(421)-210;var c=r.house.offset(x,0,z);if(Math.abs(x)<48&&Math.abs(z)<48)continue;if(SecretWorld.rooms(scene.loop.secrets).stream().anyMatch(n->BlockPos.of(n.getLong("Pos")).distSqr(c)<32*32))continue;for(int dx=-8;dx<=8;dx++)for(int dz=-12;dz<=8;dz++)level.setBlock(c.offset(dx,-1,dz),Blocks.GRASS_BLOCK.defaultBlockState(),2);break;}SecretWorld.buildNext(p);}
                while(SecretWorld.pending(scene.loop.secrets)>0)SecretWorld.drain(p,96);
                if(SecretWorld.rooms(scene.loop.secrets).size()!=12)throw new IllegalStateException("Missing generated modules: "+SecretWorld.rooms(scene.loop.secrets).size());
                for(String id:List.of("key_drawer","house_codes","spying_dolls","magical_threads","servant_commands")){SecretTransactions.issue(p,id);if(!SecretTransactions.deposit(p,id))throw new IllegalStateException("Deposit failed");}SecretsNetwork.open(p,false,false);
            }else if(step==1){SecretsNetwork.open(p,true,false);}
            else if(step==2){mc.execute(()->mc.setScreen(null));var room=SecretWorld.rooms(scene.loop.secrets).get(3);var at=BlockPos.of(room.getLong("Pos"));p.teleportTo(p.serverLevel(),at.getX()+.5,at.getY()+1,at.getZ()-8.5,0,5);SecretLocks.open(p,3);}
            else if(step==3){mc.execute(()->mc.setScreen(null));var room=SecretWorld.rooms(scene.loop.secrets).get(9);var at=BlockPos.of(room.getLong("Pos"));p.teleportTo(p.serverLevel(),at.getX()+.5,at.getY()+2,at.getZ()-9,0,4);}
            else if(step==4){scene.loop.secrets.journal.addAll(SecretRegistry.ALL.keySet());SecretTransactions.dirty(p);SecretsNetwork.open(p,false,false);}
            else if(step==5){click(mc,"Connections");}
            else if(step==6){click(mc,"Abilities");}
            else if(step==7){click(mc,"Map");}
            else if(step==8){SecretsNetwork.open(p,true,false);}
            else if(step==9){click(mc,"Mother");}
            else if(step==10){click(mc,"Awareness");}
            else if(step==11){var r=SecretWorld.realm(p);p.teleportTo(p.serverLevel(),r.bed.getX()+.5,r.bed.getY(),r.bed.getZ()+.5,0,0);var at=SecretWorld.hiding(p,0);SecretWorld.place(p,at,Blocks.LECTERN);SecretWorld.node(scene.loop.secrets,at,"hiding","0",-1);SecretWorld.interact(p,at);}
            else if(step==12){click(mc,"Make blackmail");}
            else if(step==13){mc.execute(()->{var screen=mc.screen;int w=Math.min(570,screen.width-16),h=Math.min(320,screen.height-16);screen.mouseClicked((screen.width-w)/2+w/3,(screen.height-h)/2+h-47,0);});}
            else if(step==14){if(scene.loop.secrets.journal.contains("key_drawer"))throw new IllegalStateException("Client blackmail confirmation did not sacrifice evidence");var mail=p.getInventory().items.stream().filter(i->i.is(SecretsItems.BLACKMAIL.get())).findFirst().orElseThrow();if(!SecretTransactions.offer(p,mail,1))throw new IllegalStateException("Client offer failed");}
            else if(step==15){click(mc,"Choose emeralds");}
            step++;
        }catch(Throwable ex){com.mojang.logging.LogUtils.getLogger().error("SECRETS39_CLIENT_FAILED",ex);mc.execute(mc::stop);}finally{busy=false;}});
    }
}
