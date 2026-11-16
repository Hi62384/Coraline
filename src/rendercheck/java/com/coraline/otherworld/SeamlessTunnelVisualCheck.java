package com.coraline.otherworld;

import com.coraline.block.TinyDoorBlock;
import com.coraline.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Actual first-person door material/clipping/crawl views: all four facings, oblique and decayed. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class SeamlessTunnelVisualCheck {
    private static boolean started;private static volatile boolean ready;private static int ticks;
    private static final BlockPos BASE=new BlockPos(1400,90,1400);
    private static final Direction[] FACING={Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST};
    private static final double[] DEPTH={-1.7,-.65,.75,1.75};
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("coraline.seamlessTunnelVisualCheck")||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(mc.player==null||mc.getSingleplayerServer()==null||mc.player.tickCount<60)return;
        mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);if(mc.screen!=null)mc.setScreen(null);
        if(!started){started=true;mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var level=p.server.overworld();
            com.coraline.feast.VisitData.get(p.server).scenes.remove(p.getUUID());for(String key:new String[]{"CoralineVisitDoor","CoralineInTunnel","CoralineReturning"})p.getPersistentData().remove(key);
            for(int i=0;i<4;i++)for(int mounting=0;mounting<2;mounting++){var base=BASE.offset(i*40,0,mounting*24);var facing=FACING[i];var back=facing.getOpposite();var side=facing.getClockWise();
                for(int u=-6;u<=6;u++)for(int d=-6;d<=7;d++)for(int y=-1;y<=4;y++)level.setBlock(base.relative(side,u).relative(back,d).above(y),y==-1?net.minecraft.world.level.block.Blocks.SPRUCE_PLANKS.defaultBlockState():mounting==0&&d==1?net.minecraft.world.level.block.Blocks.PINK_TERRACOTTA.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2|16|32);
                level.setBlock(base,ModBlocks.TINY_DOOR.get().defaultBlockState().setValue(TinyDoorBlock.FACING,facing).setValue(TinyDoorBlock.OPEN,true).setValue(TinyDoorBlock.PORTAL,true),3);
                var door=(DoorBlockEntity)level.getBlockEntity(base);door.doorId=java.util.UUID.randomUUID();if(!TunnelPassage.ensure(level,door))throw new IllegalStateException("Visual passage could not be built");
            }
            p.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);level.setDayTime(1000);level.setWeatherParameters(100000,0,false,false);ready=true;
        });return;}
        if(!ready)return;ticks++;int index=(ticks-1)/60;
        if(index>=36){com.mojang.logging.LogUtils.getLogger().info("SEAMLESS_TUNNEL_NATIVE_OK: 36 native views: four-facing wall apertures and freestanding closed/open/portal models; screenshots require visual inspection");mc.stop();return;}
        if(ticks%60==1){int i=index;mc.getSingleplayerServer().execute(()->{
            int facingIndex=i>=24?(i-24)/3:i<16?i/4:i%4;var facing=FACING[facingIndex];var base=BASE.offset(facingIndex*40,0,i>=24?24:0);var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var level=p.server.overworld();
            if(i>=24){int variant=(i-24)%3;var state=level.getBlockState(base);level.setBlock(base,state.setValue(TinyDoorBlock.OPEN,variant!=0).setValue(TinyDoorBlock.PORTAL,variant==2),3);
                var at=TunnelPassage.inside(base,facing,-1.7).add(Vec3.atLowerCornerOf(facing.getClockWise().getNormal()).scale(.65));var delta=Vec3.atCenterOf(base).subtract(at.add(0,p.getEyeHeight(),0));
                p.teleportTo(level,at.x,at.y,at.z,(float)Math.toDegrees(Math.atan2(-delta.x,delta.z)),(float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance())));p.setDeltaMovement(Vec3.ZERO);TunnelPassage.tick(p);return;
            }
            double depth=i<16?DEPTH[i%4]:i<20?-1.1:-1.7;var at=TunnelPassage.inside(base,facing,depth);
            if(i>=16&&i<20)at=at.add(Vec3.atLowerCornerOf(facing.getClockWise().getNormal()).scale(.35));
            var door=(DoorBlockEntity)level.getBlockEntity(base);door.tunnelPreview=i>=20?2:1;door.setChanged();level.sendBlockUpdated(base,door.getBlockState(),door.getBlockState(),3);
            p.teleportTo(level,at.x,at.y,at.z,facing.getOpposite().toYRot()+(i>=16&&i<20?12:0),i<16&&i%4>0?0:35);p.setDeltaMovement(Vec3.ZERO);TunnelPassage.tick(p);
            if(i<16&&i%4>0&&p.getPose()!=net.minecraft.world.entity.Pose.SWIMMING)throw new IllegalStateException("Native approach failed to crawl for "+facing);
        });}
        if(ticks%60==50)try{var out=java.nio.file.Path.of("screenshots","seamless_tunnel_qa");java.nio.file.Files.createDirectories(out);try(var shot=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){shot.writeToFile(out.resolve(String.format(java.util.Locale.ROOT,"view_%02d.png",index)));}}catch(Exception e){throw new RuntimeException(e);}
    }
}
