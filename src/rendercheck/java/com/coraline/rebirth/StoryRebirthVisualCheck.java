package com.coraline.rebirth;

import com.coraline.feast.*;
import com.coraline.parents.*;
import com.coraline.registry.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;

/** Disposable post-ending DEBUG checkpoint. Actual online inventory files, normal consent packets,
 * original real-parent NPCs, two explicit thread interactions and production reset/recovery adapters.
 * It does NOT play the main storyline or prove whole second-playthrough acceptance. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class StoryRebirthVisualCheck {
    private static boolean requested,beginSent,consentOpened;
    private static volatile boolean cancelledClient;
    private static boolean cancelValidated;
    private static volatile boolean ready,stopping;
    private static volatile String capture;
    private static int clientTicks,serverTicks,holdTicks;
    private static UUID mother,father,family,door;
    private static String phase="";
    private static int phaseTicks;
    private static int warmTicks,stableParentTicks;
    private static boolean parentsReady;
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("coraline.storyRebirthCheck")||e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;
        if(mc.player==null||mc.getSingleplayerServer()==null||mc.player.tickCount<60)return;
        if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)mc.setScreen(null);
        if(!requested){requested=true;mc.getSingleplayerServer().execute(()->{try{stage(mc.getSingleplayerServer().getPlayerList().getPlayers().get(0));ready=true;}catch(Throwable bad){fail("stage",bad);}});}
        if(mc.screen!=null&&mc.screen.getClass().getName().equals("com.coraline.client.RebirthClient$ConsentScreen"))try{
            var nonceField=mc.screen.getClass().getDeclaredField("nonce");nonceField.setAccessible(true);var nonce=(UUID)nonceField.get(mc.screen);
            var page=mc.screen.getClass().getDeclaredField("second");page.setAccessible(true);boolean second=page.getBoolean(mc.screen);
            clientTicks++;
            if(!second&&!cancelledClient&&clientTicks==10){RebirthNetwork.input(nonce,"cancel");cancelledClient=true;mc.setScreen(null);return;}
            if(!second&&!beginSent&&clientTicks>=30){capture="explanation";beginSent=true;RebirthNetwork.input(nonce,"begin");}
            if(second){if(holdTicks==0)capture="confirmation";if(++holdTicks%4==0)RebirthNetwork.input(nonce,"hold");}
        }catch(Throwable bad){fail("consent",bad);}
        var shot=capture;if(shot!=null){capture=null;try{
            var out=java.nio.file.Path.of("../artwork/rebirth_native_qa");java.nio.file.Files.createDirectories(out);
            try(var image=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(out.resolve(shot+".png"));}
        }catch(Exception bad){fail("capture",bad);}}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void server(TickEvent.PlayerTickEvent e){
        if(!Boolean.getBoolean("coraline.storyRebirthCheck")||!ready||stopping||e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p))return;
        try{
            // The freshly staged bedroom spans a chunk boundary. Consent must
            // wait for the original UUIDs to be visible to the native entity
            // sections, rather than calling an unloaded original "dead".
            if(!parentsReady){
                var a=p.server.overworld().getEntity(mother);var b=p.server.overworld().getEntity(father);
                if(a instanceof ParentEntity m&&m.isAlive()&&b instanceof ParentEntity d&&d.isAlive())stableParentTicks++;else stableParentTicks=0;
                require(++warmTicks<400,"Original fixture parents did not finish loading");
                if(stableParentTicks>=40)parentsReady=true;
                return;
            }
            serverTicks++;var run=StoryRunStore.get(p.server,p.getUUID());
            if(cancelledClient&&!cancelValidated&&serverTicks%20==0){
                require(!run.active()&&run.session()==null&&run.run()==1&&run.earned()==1,"Cancel changed the story transaction");
                require(p.server.overworld().getEntity(mother) instanceof ParentEntity m&&m.isAlive()&&p.server.overworld().getEntity(father) instanceof ParentEntity d&&d.isAlive(),"Cancel original parent unavailable/dead; mother="+p.server.overworld().getEntity(mother)+" father="+p.server.overworld().getEntity(father));
                cancelValidated=true;consentOpened=false;System.out.println("STORY_REBIRTH_NATIVE_CANCEL_VERIFIED parents alive, run/reward unchanged");
            }
            if(!phase.equals(run.phase().name())){phase=run.phase().name();phaseTicks=0;System.out.println("STORY_REBIRTH_NATIVE_PROGRESS "+phase+" run="+run.run()+" earned="+run.earned());capture=phase.toLowerCase(java.util.Locale.ROOT);
                if(run.phase()==StoryRun.Phase.BINDING){var at=net.minecraft.world.phys.Vec3.atBottomCenterOf(BlockPos.of(run.details().getLong("Center"))).add(0,0,4);p.teleportTo(p.serverLevel(),at.x,at.y,at.z,180,0);}
            }
            phaseTicks++;
            if((run.phase()==StoryRun.Phase.FIRST_DEATH||run.phase()==StoryRun.Phase.FINAL_DEATH)&&phaseTicks==50)capture=phase.toLowerCase(java.util.Locale.ROOT)+"_collapse";
            if(run.phase()==StoryRun.Phase.SUMMONING&&phaseTicks%100==0){
                var f=FamilyData.get(p.server).families.get(family);
                System.out.println("STORY_REBIRTH_NATIVE_NAV owner="+p.position()+" center="+BlockPos.of(run.details().getLong("Center"))+" mother="+p.server.overworld().getEntity(mother)+" father="+p.server.overworld().getEntity(father));
                if(phaseTicks>=250)p.teleportTo(p.serverLevel(),p.getX(),p.getY(),p.getZ(),0,-90); // Legitimate owner look-away permits only production's safe offscreen recovery.
            }
            if(run.phase()==StoryRun.Phase.IDLE&&run.run()==1&&run.claimed()&&!consentOpened&&serverTicks%20==0){
                StoryRebirth.debug(p,"portrait");StoryRebirth.debug(p,"location");p.setYRot(180);p.setXRot(0);
                var d=StoryRunStore.get(p.server,p.getUUID()).details();if(d.hasUUID("Portrait")){
                    var prop=p.server.overworld().getEntity(d.getUUID("Portrait"));if(prop instanceof CinematicPropEntity portrait){StoryRebirth.interact(p,portrait);consentOpened=true;}
                }
            }
            if(run.phase()==StoryRun.Phase.FIRST_THREAD||run.phase()==StoryRun.Phase.FINAL_THREAD){
                require(run.earned()==1,"Ritual granted a second reward");
                // Leave each interactive phase waiting for 60 real ticks: timers must not kill either parent.
                if(phaseTicks==55)require(run.phase()==StoryRunStore.get(p.server,p.getUUID()).phase(),"Thread was severed automatically");
                if(run.phase()==StoryRun.Phase.FIRST_THREAD&&phaseTicks==50){
                    var entity=p.server.overworld().getEntity(run.details().getUUID("FirstLife"));require(entity instanceof CinematicPropEntity,"First thread absent");
                    var old=p.getMainHandItem();p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_SWORD));
                    StoryRebirth.interact(p,(CinematicPropEntity)entity);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,old);
                    require(StoryRunStore.get(p.server,p.getUUID()).phase()==StoryRun.Phase.FIRST_THREAD,"Ordinary weapon severed a life thread");
                }
                if(phaseTicks==70){
                    var f=FamilyData.get(p.server).families.get(family);var victim=run.phase()==StoryRun.Phase.FIRST_THREAD?mother:father;
                    var alive=p.server.overworld().getEntity(victim);require(alive instanceof ParentEntity parent&&parent.isAlive(),"Parent died before owner cut");
                    for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(RebirthItems.CUTTER.get())){p.getInventory().selected=i<9?i:p.getInventory().selected;if(i>=9){var old=p.getInventory().getItem(0);p.getInventory().setItem(0,p.getInventory().getItem(i));p.getInventory().setItem(i,old);p.getInventory().selected=0;}break;}
                    String key=run.phase()==StoryRun.Phase.FIRST_THREAD?"FirstLife":"FinalLife";var entity=p.server.overworld().getEntity(run.details().getUUID(key));
                    require(entity instanceof CinematicPropEntity,"Actual life-thread prop missing");
                    var at=entity.position().add(0,-.7,1.5);p.teleportTo(p.server.overworld(),at.x,at.y,at.z,180,0);
                    require(StoryRebirth.interact(p,(CinematicPropEntity)entity),"Owner thread interaction not handled");
                    require(StoryRunStore.get(p.server,p.getUUID()).phase()!=run.phase(),"Bound tool/reach/LOS cut was rejected");
                }
            }
            if(run.phase()==StoryRun.Phase.FINAL_THREAD)require(run.tombstoned(mother)&&!run.tombstoned(father),"First death did not leave surviving father alive");
            if(run.phase()==StoryRun.Phase.REWINDING)require(run.tombstoned(mother)&&run.tombstoned(father),"Rewind preceded genuine death receipts");
            if(run.phase()==StoryRun.Phase.IDLE&&run.run()==2){
                var f=ParentAccess.family(p.server,p.getUUID());require(f!=null&&f.id.equals(family)&&!f.rescued&&f.momTask==0&&f.dadTask==0,"Original family behavior not restored");
                require(!f.mom.equals(mother)&&!f.dad.equals(father),"Sacrificed versions silently reused instead of earlier timeline instances");
                require(p.server.overworld().getEntity(f.mom) instanceof ParentEntity a&&a.isAlive()&&p.server.overworld().getEntity(f.dad) instanceof ParentEntity b&&b.isAlive(),"New earlier parents unavailable");
                require(VisitData.get(p.server).scenes.get(p.getUUID())==null,"Old one-time story scene survived rewind");
                require(cancelValidated&&run.earned()==1&&run.completed()==1&&!run.complete(),"Cancel/ritual changed earned/completed accounting");
                require(p.getInventory().countItem(RebirthItems.VOID_HEART.get())==1&&p.getInventory().countItem(Items.DIAMOND)==7&&p.getInventory().countItem(ModItems.DOWSING_ROD.get())==1,"Permanent possessions/Heart lost");
                require(com.coraline.otherworld.DoorData.get(p.server).doors.get(door).revoked,"Old door still usable");
                System.out.println("STORY_REBIRTH_NATIVE_COMPLETE actual online inventory receipt, two-stage nonce hold, original parents, separate explicit cuts, health-zero deaths, tombstones, durable reset, preserved Heart/gear, original intro gate; DEBUG post-ending checkpoint, not whole story replay");
                stopping=true;Minecraft.getInstance().execute(Minecraft.getInstance()::stop);
            }
            if(serverTicks>5000)throw new IllegalStateException("Ritual checkpoint stalled: "+StoryRebirth.debug(p,"state"));
        }catch(Throwable bad){fail("flow",bad);}
    }
    private static void stage(ServerPlayer p){
        p.server.getPlayerList().op(p.getGameProfile()); // Disposable tool-created world only; exercise real operator checks.
        p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);var l=p.server.overworld();var base=new BlockPos(32,90,32);
        var chunk=new net.minecraft.world.level.ChunkPos(base);for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)l.setChunkForced(chunk.x+x,chunk.z+z,true);
        for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)for(int y=-1;y<=5;y++){
            var at=base.offset(x,y,z);l.getChunkAt(at);l.setBlock(at,(y==-1||x==-6||x==6||z==-6||z==6?Blocks.DARK_OAK_PLANKS:y==5?Blocks.GLASS:Blocks.AIR).defaultBlockState(),2);
        }
        p.teleportTo(l,base.getX()+.5,base.getY(),base.getZ()+2.5,0,0);p.getInventory().clearContent();
        p.getInventory().setItem(0,new ItemStack(ModItems.DOWSING_ROD.get()));p.getInventory().setItem(1,new ItemStack(Items.DIAMOND,7));
        var f=new FamilyData.Family();f.owner=p.getUUID();f.bed=base;f.rescued=true;family=f.id;
        var mom=ModEntities.MOM.get().create(l);var dad=ModEntities.DAD.get().create(l);require(mom!=null&&dad!=null,"Cannot create fixture parents");
        mom.familyId=dad.familyId=f.id;mom.setPos(base.getX()-2,base.getY(),base.getZ()+2);dad.setPos(base.getX()+2,base.getY(),base.getZ()+2);
        f.mom=mother=mom.getUUID();f.dad=father=dad.getUUID();f.momPos=mom.blockPosition();f.dadPos=dad.blockPosition();
        FamilyData.get(p.server).families.put(f.id,f);FamilyData.get(p.server).setDirty();require(l.addFreshEntity(mom)&&l.addFreshEntity(dad),"Fixture parent insertion failed");
        var bed=ModBlocks.PARENTS_BED.get().defaultBlockState();l.setBlock(base,bed,3);
        if(l.getBlockEntity(base) instanceof ParentsBedEntity entity){entity.familyId=f.id;entity.setChanged();}
        for(int part=1;part<4;part++)l.setBlock(ParentsBedBlock.partPos(base,bed.getValue(ParentsBedBlock.FACING),part),bed.setValue(ParentsBedBlock.PART,part),3);
        var d=new com.coraline.otherworld.DoorData.Door();d.owner=p.getUUID();door=d.id;com.coraline.otherworld.DoorData.get(p.server).doors.put(d.id,d);com.coraline.otherworld.DoorData.get(p.server).setDirty();
        var s=new VisitData.Scene();s.owner=p.getUUID();s.family=f.id;s.door=d.id;s.stage=VisitData.Stage.DONE;VisitData.get(p.server).scenes.put(s.owner,s);VisitData.get(p.server).setDirty();
        // Deliberately marked operator checkpoint, never reported as a legitimately completed chapter.
        System.out.println("STORY_REBIRTH_NATIVE_STARTED DEBUG post-ending fixture: "+StoryRebirth.debug(p,"eligible"));
        require(StoryRunStoreSafe(p).claimed(),"Real online Heart receipt failed");
    }
    private static StoryRun StoryRunStoreSafe(ServerPlayer p){try{return StoryRunStore.get(p.server,p.getUUID());}catch(Exception bad){throw new IllegalStateException(bad);}}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException(why);}
    private static void fail(String stage,Throwable error){if(stopping)return;stopping=true;com.mojang.logging.LogUtils.getLogger().error("STORY_REBIRTH_NATIVE_FAILED "+stage,error);Minecraft.getInstance().execute(Minecraft.getInstance()::stop);}
    private StoryRebirthVisualCheck(){}
}
