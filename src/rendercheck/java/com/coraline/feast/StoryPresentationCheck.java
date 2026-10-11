package com.coraline.feast;

import com.coraline.otherworld.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Explicitly enabled disposable-world render check; never included in the release jar. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class StoryPresentationCheck {
    private static boolean requested;private static volatile boolean ready;private static int ticks;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("coraline.storyPresentationCheck")||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.getSingleplayerServer()==null)return;
        mc.options.pauseOnLostFocus=false;if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)mc.setScreen(null);
        if(ready){mc.player.getAbilities().flying=true;mc.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);}
        if(!requested){requested=true;mc.getSingleplayerServer().execute(()->{
            var server=mc.getSingleplayerServer();var p=server.getPlayerList().getPlayers().get(0);var level=server.getLevel(OtherWorld.KEY);var pos=new BlockPos(50000,100,50000);
            for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)level.setBlock(pos.offset(x,-1,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);
            var d=new DoorData.Door();d.owner=p.getUUID();d.pos=pos;DoorData.get(server).doors.put(d.id,d);
            var r=new RealmData.Realm();r.door=d.id;r.doorPos=pos;r.bed=pos;r.radius=40;r.ready=true;RealmData.get(server).realms.put(d.id,r);
            var s=new VisitData.Scene();s.owner=p.getUUID();s.door=d.id;s.stage=VisitData.Stage.DONE;s.table=pos;s.piano=pos;s.loop.phase=OtherMotherLoopState.Phase.GIFTS;s.loop.loot=OtherMotherDialogue.Loot.TREASURE;s.loop.opened=true;
            for(var item:new Item[]{Items.IRON_INGOT,Items.GOLD_INGOT,Items.DIAMOND,Items.COAL,Items.EMERALD,Items.LAPIS_LAZULI,Items.REDSTONE,Items.COPPER_INGOT,com.coraline.clothing.OtherMaterials.OTHER_IRON.get(),com.coraline.clothing.OtherMaterials.OTHER_DIAMOND.get()})s.loop.gifts.add(new ItemStack(item,3));
            VisitData.get(server).scenes.put(p.getUUID(),s);p.getPersistentData().putUUID("CoralineVisitDoor",d.id);p.getPersistentData().remove("CoralineInTunnel");
            p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(level,pos.getX()+.5,pos.getY()+1.8,pos.getZ()-2.4,0,36);
            LoopProps.ensure(p,s);ready=true;
        });}
        if(ready&&++ticks==30)com.coraline.client.VisitAudioClient.accept(new VisitAudio.Cue("other_mother_welcome_low_trust_low_suspicion_01",false,false));
        if(ready&&ticks==80){
            try(var screenshot=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){screenshot.writeToFile(java.nio.file.Path.of("../artwork/giftbox_story_subtitles.png"));}
            catch(Exception failure){throw new RuntimeException(failure);}
            com.mojang.logging.LogUtils.getLogger().info("CORALINE_GIFTBOX_SUBTITLES_RENDER_OK");mc.stop();
        }
    }
}
