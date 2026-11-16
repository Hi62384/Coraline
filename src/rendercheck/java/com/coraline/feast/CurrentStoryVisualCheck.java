package com.coraline.feast;

import com.coraline.client.*;
import com.coraline.registry.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Additional disposable checkpoints after the real Cat playthrough. Online owner,
 * actual departure controller and real mob renderers; not original-save acceptance. */
public final class CurrentStoryVisualCheck {
    private static volatile int stage;private static int frames,age;private static long clock=-1;
    private static Vec3 bikeStart;private static UUID bikeId;private static BlockPos base;private static boolean stoneOnly;
    /** Separate visible renderer fixture; no Cat/story or ending state is fabricated. Server thread only. */
    public static void beginSeeingStoneOnly(Minecraft mc){stage=-1;stoneOnly=true;base=new BlockPos(31600,110,31600);var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);p.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);p.removeAllEffects();mc.options.hideGui=false;stone(mc);}
    public static boolean tick(Minecraft mc){
        if(stage==0){stage=-1;mc.getSingleplayerServer().execute(()->bike(mc));return false;}
        if(stage<0)return false;
        if(stage==1){if(clock!=mc.level.getGameTime()){clock=mc.level.getGameTime();mc.getSingleplayerServer().execute(()->ride(mc));}return false;}
        frames++;
        if(stage==2){
            if(frames>=60&&mc.screen==null){if(!mc.player.getMainHandItem().is(ModItems.SEEING_STONE.get()))throw new AssertionError("Seeing Stone fixture inventory did not synchronize");mc.options.keyUse.setDown(true);if(!mc.player.isUsingItem())mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);}
            if(frames>100&&SeeingStoneClient.raised()&&SpectralEyes.ready()){
                mask(mc,false);shot(mc,"seeing_stone_all_mobs");stage=-1;frames=0;
                mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);for(int x=-2;x<=2;x++)for(int y=0;y<=3;y++)p.serverLevel().setBlock(base.offset(x,y,26),Blocks.STONE.defaultBlockState(),3);stage=3;});
            }else if(frames>300)throw new AssertionError("Real seeing-stone render layer never became ready: using="+mc.player.isUsingItem()+" raised="+SeeingStoneClient.raised()+" spectral="+SpectralEyes.ready());
        }else if(stage==3&&frames>50){mask(mc,true);shot(mc,"seeing_stone_wall_occlusion");System.out.println(stoneOnly?"SEEING_STONE_NATIVE_RENDER_COMPLETE: visible held stone, loaded distant/passive/hostile mob GPU colors and wall depth":"CURRENT_STORY_NATIVE_COMPLETE: online mounted departure moved; loaded distant passive/hostile mobs colored; wall depth retained");return true;}
        return false;
    }
    private static void bike(Minecraft mc){
        var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var s=OtherMotherLoop.scene(p);base=p.blockPosition().east(25);var level=p.serverLevel();
        for(int x=-12;x<=45;x++)for(int z=-8;z<=8;z++){var at=base.offset(x,0,z);level.getChunkAt(at);level.setBlock(at.below(),Blocks.OAK_PLANKS.defaultBlockState(),2);for(int y=0;y<4;y++)level.setBlock(at.above(y),Blocks.AIR.defaultBlockState(),2);}
        p.teleportTo(level,base.getX()-4+.5,base.getY(),base.getZ()+.5,-90,0);
        var actor=ShowActors.acquire(p,s,"wybie",level,Vec3.atBottomCenterOf(base));if(actor==null)throw new AssertionError("Original real Wybie unavailable for online departure checkpoint");
        var n=FirstEscapeStory.data(s);n.putString("Phase","BICYCLE_DEPARTURE");n.putInt("BicycleDepartureTicks",0);n.putInt("DepartureRiddenTicks",0);n.remove("StoryBike");n.remove("BikeRoute");n.remove("BikeRetryAt");n.remove("BikeProgressPosition");stage=1;
    }
    private static void ride(Minecraft mc){
        var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var s=OtherMotherLoop.scene(p);var n=FirstEscapeStory.data(s);age++;
        if(n.hasUUID("StoryBike")&&p.serverLevel().getEntity(n.getUUID("StoryBike")) instanceof WybieBicycleEntity bike){
            if(bikeStart==null){bikeStart=bike.position();bikeId=bike.getUUID();}
            if(!bikeId.equals(bike.getUUID())||bike.getPassengers().isEmpty())throw new AssertionError("Departure replaced the bike or lost its rider");
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,bike.position().add(0,1.1,0));
            if(age>=35){double moved=bike.position().distanceTo(bikeStart);if(moved<8)throw new AssertionError("Actual online departure pedaled in place: "+moved+"m; "+n);
                System.out.println("ACTUAL_DEPARTURE_NATIVE_MOVED: "+moved+"m with original mounted rider");var rider=(com.coraline.entity.CharacterEntity)bike.getPassengers().get(0);CanonicalRealWybie.debugPark(p,s,rider);bike.discard();n.putString("Phase","ESCAPE_COMPLETE");stage=-1;stone(mc);}
        }else if(age>100)throw new AssertionError("Online departure never loaded its original bicycle: "+n);
    }
    private static void stone(Minecraft mc){
        var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);base=base.east(90);var level=p.serverLevel();
        for(int x=-15;x<=15;x++)for(int z=-15;z<=58;z++){var at=base.offset(x,0,z);level.getChunkAt(at);level.setBlock(at.below(),Blocks.STONE.defaultBlockState(),2);for(int y=0;y<4;y++)level.setBlock(at.above(y),Blocks.AIR.defaultBlockState(),2);}
        level.setDayTime(1000);p.teleportTo(level,base.getX()+.5,base.getY(),base.getZ()+.5,0,0);p.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(ModItems.SEEING_STONE.get()));p.getInventory().selected=0;p.inventoryMenu.broadcastFullState();p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
        for(int i=0;i<49;i++)mob(level,EntityType.PIG,base.offset(i%13-6,0,-3-i/13));
        mob(level,EntityType.PIG,base.offset(-1,0,10));mob(level,EntityType.ZOMBIE,base.offset(1,0,10));mob(level,EntityType.PIG,base.offset(0,0,52));
        frames=0;stage=2;
    }
    private static void mob(net.minecraft.server.level.ServerLevel level,EntityType<? extends Mob> type,BlockPos at){var mob=type.create(level);mob.setPos(Vec3.atBottomCenterOf(at));mob.setNoAi(true);mob.setNoGravity(true);mob.setInvulnerable(true);mob.setPersistenceRequired();if(!level.addFreshEntity(mob))throw new AssertionError("Mob fixture was rejected");}
    private static void mask(Minecraft mc,boolean wall){
        if(!SeeingStoneClient.raised()||!SpectralEyes.ready())throw new AssertionError("Seeing Stone was not raised");
        var target=mc.getMainRenderTarget();com.mojang.blaze3d.systems.RenderSystem.bindTexture(SpectralEyes.texture());
        try(var pixels=new com.mojang.blaze3d.platform.NativeImage(target.width,target.height,false)){
            pixels.downloadTexture(0,false);int green=0,red=0,distant=0;
            for(int y=0;y<pixels.getHeight();y++)for(int x=0;x<pixels.getWidth();x++){int c=pixels.getPixelRGBA(x,y),r=c&255,g=c>>8&255,b=c>>16&255;
                boolean colored=g>70&&g>r*1.6&&g>b*1.4;if(colored){green++;if(Math.abs(x-target.width/2)<target.width*.014&&Math.abs(y-target.height/2)<target.height*.07)distant++;}if(r>70&&r>g*1.6&&r>b*1.4)red++;
            }
            if(green<15||red<15||(!wall&&distant<3)||(wall&&distant>0))throw new AssertionError("Mob spectral pixels wrong: green="+green+", red="+red+", distant="+distant+", wall="+wall);
            var folder=java.nio.file.Path.of("../artwork/current_story_qa");java.nio.file.Files.createDirectories(folder);pixels.writeToFile(folder.resolve(wall?"mob_layer_wall.png":"mob_layer_all.png"));
            System.out.println("SEEING_STONE_NATIVE_PIXELS: green="+green+" red="+red+" distant="+distant+" wall="+wall);
        }catch(java.io.IOException e){throw new IllegalStateException(e);}
    }
    private static void shot(Minecraft mc,String name){try{var dir=java.nio.file.Path.of("../artwork/current_story_qa");java.nio.file.Files.createDirectories(dir);try(var pixels=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){pixels.writeToFile(dir.resolve(name+".png"));}}catch(java.io.IOException e){throw new IllegalStateException(e);}}
    private CurrentStoryVisualCheck(){}
}
