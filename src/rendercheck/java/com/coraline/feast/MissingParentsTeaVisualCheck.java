package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.registry.*;
import com.coraline.bobinsky.BobinskyHouse;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Isolated native controller playthrough: real recordings/acknowledgements, no fabricated audio completion. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class MissingParentsTeaVisualCheck {
    private static boolean started;private static volatile boolean ready,complete;private static int ticks,finishedTicks;private static volatile String state="";private static String lastPhase="";private static final Set<String> shots=new HashSet<>();
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(!Boolean.getBoolean("coraline.missingParentsTeaVisualCheck")||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.player==null||mc.getSingleplayerServer()==null||mc.player.tickCount<60)return;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(2);if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)mc.setScreen(null);
if(!started){started=true;mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);p.removeAllEffects();p.server.getPlayerList().op(p.getGameProfile());var l=p.server.overworld();var c=new BlockPos(29200,100,29200);resetPriorFixture(p,c);p.getInventory().clearContent();for(int x=-52;x<=52;x++)for(int z=-52;z<=52;z++){l.getChunkAt(c.offset(x,0,z));for(int y=-5;y<=-1;y++)l.setBlock(c.offset(x,y,z),(y==-1?Blocks.GRASS_BLOCK:Blocks.DIRT).defaultBlockState(),2);for(int y=0;y<=10;y++)l.setBlock(c.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);}l.setDayTime(1000);l.setWeatherParameters(100000,0,false,false);p.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);p.teleportTo(l,c.getX()+.5,c.getY(),c.getZ()+.5,0,0);var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.table=s.piano=c;s.loop.event.putInt("Serial",79);VisitData.get(p.server).scenes.put(s.owner,s);var d=new DoorData.Door();d.id=s.door;d.owner=s.owner;d.pos=c;d.placed=true;DoorData.get(p.server).doors.put(d.id,d);l.setBlock(c,ModBlocks.TINY_DOOR.get().defaultBlockState(),2);String result=com.coraline.item.FirstEscapeDebugItem.activate(p,true);if(!FirstEscapeStory.data(s).getBoolean("Complete"))throw new IllegalStateException("Debug completion failed: "+result);ready=true;});return;}
        if(!ready)return;ticks++;if(ticks>5200)throw new IllegalStateException("Post-escape tea visit stalled: "+state);
mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var s=OtherMotherLoop.scene(p);var n=MissingParentsTea.data(s);var phase=MissingParentsTea.phase(n);state=phase+" / "+n.getString("Blocker")+" / t="+n.getInt("Ticks");if(!phase.name().equals(lastPhase)){lastPhase=phase.name();com.mojang.logging.LogUtils.getLogger().info("TEA_QA_PHASE: {}",state);}if(!n.getBoolean("Built")){if(n.getBoolean("Completed")){int stones=0;for(var item:p.getInventory().items)if(item.is(ModItems.SEEING_STONE.get()))stones+=item.getCount();if(stones!=1||s.loop.story.getBoolean("SeeingStonePending")||n.getBoolean("Active"))throw new IllegalStateException("Invalid final tea reward/cleanup state");for(String key:List.of("Spink","Forcible","Seat","SpinkSeat","BowlProp"))if(n.hasUUID(key)&&p.server.overworld().getEntity(n.getUUID(key))!=null)throw new IllegalStateException("Tea actor not cleaned: "+key);var lookup=p.serverLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK);for(var raw:n.getList("Originals",10)){var cell=(net.minecraft.nbt.CompoundTag)raw;if(!p.level().getBlockState(BlockPos.of(cell.getLong("Pos"))).equals(net.minecraft.nbt.NbtUtils.readBlockState(lookup,cell.getCompound("State"))))throw new IllegalStateException("Tea terrain not restored");}complete=true;}return;}var c=BlockPos.of(n.getLong("House"));
            if(phase==MissingParentsTea.Phase.WAITING_FOR_KNOCK){var at=BobinskyHouse.front(n).north();p.teleportTo(p.serverLevel(),at.getX()+.5,at.getY(),at.getZ()+.5,0,0);MissingParentsTea.block(p,n,BobinskyHouse.door(n));}
            if(phase==MissingParentsTea.Phase.FOLLOW_SPINK&&!n.getBoolean("QAEntered")){n.putBoolean("QAEntered",true);p.teleportTo(p.serverLevel(),c.getX()+.5,c.getY(),c.getZ()-6.5,0,0);}
            if(phase==MissingParentsTea.Phase.FOLLOW_SPINK&&n.getBoolean("QAEntered")&&n.getInt("Ticks")>30){p.teleportTo(p.serverLevel(),c.getX()+.5,c.getY(),c.getZ()+.5,0,0);}
            if(phase==MissingParentsTea.Phase.WAITING_FOR_SEAT){p.teleportTo(p.serverLevel(),c.getX()+1.5,c.getY(),c.getZ()+2.5,90,8);MissingParentsTea.block(p,n,c.offset(1,0,1));}
            if(phase.name().startsWith("STORY_")){p.setYRot(90);p.setXRot(18);}
            if(phase==MissingParentsTea.Phase.WAITING_FOR_EXIT){p.teleportTo(p.serverLevel(),c.getX()+.5,c.getY(),c.getZ()-13.5,180,0);}
        });
        if(mc.level!=null){var actors=mc.level.getEntitiesOfClass(com.coraline.tea.TeaVisitorEntity.class,mc.player.getBoundingBox().inflate(48));if(!actors.isEmpty()){var a=actors.get(0);String phase=a.phase()>=100&&a.phase()-100<MissingParentsTea.Phase.values().length?MissingParentsTea.Phase.values()[a.phase()-100].name():"";String shot=phase.equals("STORY_A")&&a.elapsed()>50?"seated_gaze":phase.equals("STORY_D")&&a.hasCup()?"carrying_bowl":phase.equals("STORY_F")&&a.elapsed()>14&&a.elapsed()<24?"raised_needles":phase.equals("STORY_F")&&a.elapsed()>110?"crushing_taffy":phase.equals("STORY_G")&&a.elapsed()>33?"dust_reveal":phase.equals("STORY_I")&&a.elapsed()>110?"exit_released":"";if(!shot.isEmpty()&&shots.add(shot))capture(mc,shot);}}
        if(complete){if(++finishedTicks==1)mc.setScreen(new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("Seeing Stone native model preview")){@Override public boolean isPauseScreen(){return false;}@Override public void render(net.minecraft.client.gui.GuiGraphics g,int x,int y,float partial){g.fill(0,0,width,height,0xFFE6DDD0);for(int i=0;i<width;i+=16)for(int j=0;j<height;j+=16)if((i/16+j/16)%2==0)g.fill(i,j,i+16,j+16,0xFFD3C9BC);g.pose().pushPose();g.pose().translate(width/2F-64,height/2F-64,0);g.pose().scale(8,8,8);g.renderItem(new net.minecraft.world.item.ItemStack(ModItems.SEEING_STONE.get()),0,0);g.pose().popPose();}});
            if(finishedTicks==15)capture(mc,"seeing_stone_gui");if(finishedTicks==25){com.mojang.logging.LogUtils.getLogger().info("MISSING_PARENTS_TEA_NATIVE_COMPLETE: original tea house, real A-I playback, physical NPC routes, seating, reward and offscreen restore");mc.stop();}}
    }
    private static void resetPriorFixture(net.minecraft.server.level.ServerPlayer p,BlockPos center){
        var visits=com.coraline.bobinsky.BobinskyVisitData.get(p.server);var old=visits.teaVisits.get(p.getUUID());
        if(old==null||!old.getBoolean("Active"))return;
        var scene=OtherMotherLoop.scene(p);var house=BlockPos.of(old.getLong("House"));
        if(scene==null||scene.loop.event.getInt("Serial")!=79||!old.getBoolean("MissingParentsTea")||house.distSqr(center)>6000)
            throw new IllegalStateException("Unrelated active tea visit in QA world; refusing to overwrite it");
        p.stopRiding();ShowPlayback.stop(p,scene);var level=p.server.overworld();
        for(String key:List.of("Spink","Forcible","Seat","SpinkSeat","BowlProp"))if(old.hasUUID(key)){
            level.getChunkAt(house);var actor=level.getEntity(old.getUUID(key));if(actor!=null){actor.ejectPassengers();actor.discard();}
        }
        var safe=new net.minecraft.nbt.ListTag();var lookup=level.holderLookup(net.minecraft.core.registries.Registries.BLOCK);
        for(var raw:old.getList("Originals",10)){var cell=(net.minecraft.nbt.CompoundTag)raw;var at=BlockPos.of(cell.getLong("Pos"));var key=Long.toString(at.asLong());
            if(old.getCompound("Placed").contains(key)&&level.getBlockState(at).equals(net.minecraft.nbt.NbtUtils.readBlockState(lookup,old.getCompound("Placed").getCompound(key))))safe.add(cell.copy());}
        old.put("Originals",safe);BobinskyHouse.restore(level,old);visits.teaVisits.remove(p.getUUID());visits.setDirty();
    }
    private static void capture(Minecraft mc,String name){try{var dir=java.nio.file.Path.of("../artwork/missing_parents_tea_qa");java.nio.file.Files.createDirectories(dir);try(var image=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(dir.resolve(name+".png"));}}catch(Exception failure){throw new RuntimeException(failure);}}
}
