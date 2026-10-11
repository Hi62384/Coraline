package com.coraline.client;

import com.coraline.otherworld.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/** Opt-in integration fixture in a COPY of the disposable GameTest world. Never shipped. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class OtherWorldVisualCheck {
    private static boolean requested;private static volatile int phase;private static int frames,seen=-1;
    private static RealmData.Realm realm;private static DoorData.Door door;private static RealmCopy copy;
    private static BlockPos initialTunnel;
    private static final BlockPos HOME=new BlockPos(12000,80,12000);
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("coraline.worldVisualCheck")||e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null)return;
        mc.options.hideGui=true;mc.options.renderDistance().set(10);mc.options.pauseOnLostFocus=false;
        if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)mc.setScreen(null);
        if(!requested){requested=true;mc.getSingleplayerServer().execute(()->prepare(mc.getSingleplayerServer()));}
    }
    private static void prepare(net.minecraft.server.MinecraftServer server){
        var source=server.overworld();source.setDayTime(18000);source.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,server);
        var player=server.getPlayerList().getPlayers().get(0);player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        for(int x=-48;x<=48;x++)for(int z=-48;z<=48;z++){
            var floor=HOME.offset(x,-1,z);source.setBlock(floor.below(),Blocks.DIRT.defaultBlockState(),2);
            source.setBlock(floor,Math.abs(x-18-(int)(3*Math.sin(z*.13)))<4?Blocks.WATER.defaultBlockState():Blocks.GRASS_BLOCK.defaultBlockState(),2);
        }
        for(int x=-5;x<=5;x++)for(int z=0;z<=9;z++)for(int y=0;y<=6;y++){
            var pos=HOME.offset(x,y,z);boolean wall=x==-5||x==5||z==0||z==9;
            var state=y==0?Blocks.OAK_PLANKS.defaultBlockState():y==6?Blocks.DARK_OAK_PLANKS.defaultBlockState():wall?Blocks.OAK_PLANKS.defaultBlockState():Blocks.AIR.defaultBlockState();
            if(wall&&y>=2&&y<=3&&Math.abs(x)<=3&&z==0)state=Blocks.GLASS.defaultBlockState();source.setBlock(pos,state,2);
        }
        for(int x=-4;x<=4;x+=4)for(int z=2;z<=8;z+=3)source.setBlock(HOME.offset(x,4,z),Blocks.SHROOMLIGHT.defaultBlockState(),2);
        for(int x=-2;x<=2;x++)for(int z=-13;z<0;z++)source.setBlock(HOME.offset(x,-1,z),Blocks.DIRT_PATH.defaultBlockState(),2);
        for(int i=0;i<18;i++){
            int x=i%2==0?-12-i%5*4:29+i%4*3,z=8+i/2*4;
            for(int y=0;y<6;y++)source.setBlock(HOME.offset(x,y,z),Blocks.OAK_LOG.defaultBlockState(),2);
            for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)for(int y=4;y<=7;y++)if(Math.abs(dx)+Math.abs(dz)<4)source.setBlock(HOME.offset(x+dx,y,z+dz),Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT,true),2);
        }
        door=new DoorData.Door();door.owner=player.getUUID();door.pos=HOME.offset(0,1,1);door.dimension=source.dimension().location().toString();door.exists=true;door.placed=true;
        DoorData.get(server).doors.put(door.id,door);source.setBlock(door.pos,com.coraline.registry.ModBlocks.TINY_DOOR.get().defaultBlockState(),2);
        ((DoorBlockEntity)source.getBlockEntity(door.pos)).doorId=door.id;
        realm=RealmData.get(server).realm(door);
        if(realm.radius!=300)throw new IllegalStateException("The player island is not 300 blocks in radius");
        realm.radius=128;realm.previousRadius=128; // bounded visual fixture only
        initialTunnel=realm.tunnel();
        copy=new RealmCopy(source,server.getLevel(OtherWorld.KEY),door,realm);phase=1;
    }
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){
        if(!Boolean.getBoolean("coraline.worldVisualCheck")||e.phase!=TickEvent.Phase.END||phase<1||copy==null)return;
        boolean done=copy.tick();
        if(copy.failed)throw new IllegalStateException("Visual fixture copy failed");
        if(copy.takeReady()){
            if(copy.remainingChunks()==0||realm.islandComplete)throw new IllegalStateException("Tunnel waited for the entire island instead of only the estate");
            verifyEstate(e.getServer());
            move(e.getServer(),false);phase=2;
        }
        if(done){
            if(!realm.islandComplete)throw new IllegalStateException("Background island copy did not finish");
            if(!e.getServer().getLevel(OtherWorld.KEY).getBlockState(realm.doorPos).is(com.coraline.registry.ModBlocks.TINY_DOOR.get()))
                throw new IllegalStateException("Background copy overwrote the finished estate");
            verifyIslandEdge(e.getServer());
            copy=null;
        }
    }
    private static void verifyEstate(net.minecraft.server.MinecraftServer server){
        var target=server.getLevel(OtherWorld.KEY);var source=server.overworld();var c=realm.house;
        if(!initialTunnel.equals(realm.tunnel()))throw new IllegalStateException("Tunnel moved when the estate door was relocated");
        if(c==null||realm.piano==null||realm.table==null||realm.bed==null)throw new IllegalStateException("Estate scene anchors missing");
        for(int x=-20;x<=20;x++)for(int z=-14;z<=16;z++){
            if(target.isEmptyBlock(c.offset(x,15,z)))throw new IllegalStateException("Open roof at "+c.offset(x,15,z)+" house="+c);
        }
        for(int y=0;y<=25;y++)if(target.isEmptyBlock(c.offset(-18,y,20)))
            throw new IllegalStateException("Turret is floating or has a gap at Y="+y);
        if(!target.getBlockState(c.offset(24,15,9)).is(Blocks.CHERRY_STAIRS)||
            !target.getBlockState(c.offset(24,0,24)).is(Blocks.CHERRY_STAIRS)||
            !target.getBlockState(c.offset(24,15,7)).is(Blocks.CHERRY_PLANKS)||
            target.isEmptyBlock(c.offset(20,9,5))||
            target.getBlockState(c.offset(-5,9,-11)).is(Blocks.DARK_OAK_STAIRS))
            throw new IllegalStateException("Attic side flight, landing, or exclusive entrance is broken");
        var portal=target.getBlockState(realm.doorPos);
        if(!portal.is(com.coraline.registry.ModBlocks.TINY_DOOR.get())||
            portal.getValue(com.coraline.block.TinyDoorBlock.FACING)!=net.minecraft.core.Direction.EAST||
            !target.isEmptyBlock(realm.doorPos.above())||!target.isEmptyBlock(realm.doorPos.east())||
            !target.isEmptyBlock(realm.doorPos.east().above())||
            !portal.getCollisionShape(target,realm.doorPos).isEmpty())
            throw new IllegalStateException("Little Door lacks standing-height clearance on both sides");
        if(!target.isEmptyBlock(realm.piano)||!target.isEmptyBlock(realm.table)||
            !target.getBlockState(realm.bed.below()).isFaceSturdy(target,realm.bed.below(),net.minecraft.core.Direction.UP))
            throw new IllegalStateException("A story room or upstairs bed is obstructed");
        var studyDoor=OtherWorldEstate.studyDoor(c);
        OtherWorldEstate.lockStudy(target,c,true);
        if(target.getBlockState(studyDoor).getValue(net.minecraft.world.level.block.DoorBlock.OPEN))throw new IllegalStateException("Study failed to lock");
        OtherWorldEstate.lockStudy(target,c,false);
        if(!target.getBlockState(studyDoor).getValue(net.minecraft.world.level.block.DoorBlock.OPEN))throw new IllegalStateException("Study failed to reopen");
        if(!target.getBlockState(studyDoor).is(Blocks.DARK_OAK_DOOR)||
            !target.getBlockState(OtherWorldEstate.atticDoor(c)).is(Blocks.SPRUCE_DOOR))
            throw new IllegalStateException("Study or attic door is missing");
        for(int z=-13;z<=-2;z++)for(int y=1;y<=6;y++)
            if(target.isEmptyBlock(c.offset(-2,y,z)))throw new IllegalStateException("Study side wall is open at "+c.offset(-2,y,z));
        for(int z=3;z<=4;z++)for(int x=-3;x<=-2;x++)
            if(!target.isEmptyBlock(c.offset(x,9,z))||!target.isEmptyBlock(c.offset(x,10,z))||target.isEmptyBlock(c.offset(x,8,z)))
                throw new IllegalStateException("Upstairs parlor entrance is blocked or lacks a floor");
        if(target.getBlockState(c.offset(0,11,15)).is(Blocks.DEEPSLATE_TILES))
            throw new IllegalStateException("Porch roof pokes through upstairs");
        if(!target.getBlockState(c.offset(0,1,12)).is(Blocks.DARK_OAK_STAIRS)||
            target.isEmptyBlock(c.offset(2,1,12))||!target.getBlockState(c.offset(2,2,12)).is(Blocks.DARK_OAK_FENCE))
            throw new IllegalStateException("Main stair has a missing tread or unsupported rail");
        if(!target.getBlockState(c.offset(-15,4,-14)).is(Blocks.YELLOW_STAINED_GLASS)||
            !target.isEmptyBlock(c.offset(-15,4,-13))||
            !target.getBlockState(c.offset(7,11,-14)).is(Blocks.YELLOW_STAINED_GLASS)||
            !target.isEmptyBlock(c.offset(7,11,-13)))
            throw new IllegalStateException("A planned window is incomplete or blocked from inside");
        if(!target.getBlockState(c.offset(10,1,-13)).is(Blocks.BIRCH_PLANKS))
            throw new IllegalStateException("Kitchen counter is detached from its wall");
        if(!target.getBlockState(c.offset(-17,4,-13)).is(Blocks.BOOKSHELF)||
            !target.getBlockState(c.offset(-17,9,-13)).is(Blocks.BOOKSHELF)||
            !target.getBlockState(c.offset(10,1,-9)).is(com.coraline.registry.ModBlocks.LOW_MARBLE_TABLE.get())||
            !target.getBlockState(c.offset(-11,1,13)).is(com.coraline.registry.ModBlocks.BLUE_VELVET_SOFA.get()))
            throw new IllegalStateException("Interior furnishing is missing");
        var dropped=target.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
            new net.minecraft.world.phys.AABB(c.offset(-20,0,-14),c.offset(20,16,16)));
        if(!dropped.isEmpty())throw new IllegalStateException("Unsupported decorations dropped in the house: "+
            dropped.stream().map(item->item.getItem().getItem()+"@"+item.blockPosition()).limit(40).toList());
        int gx=c.getX(),gz=c.getZ()+38;
        int original=source.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,gx-realm.dx,gz-realm.dz);
        while(original>source.getMinBuildHeight()+4){
            var ground=source.getBlockState(new BlockPos(gx-realm.dx,original-1,gz-realm.dz));
            if(ground.is(Blocks.GRASS_BLOCK)||ground.is(Blocks.DIRT)||ground.is(Blocks.STONE))break;
            original--;
        }
        int garden=target.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,gx,gz);
        if(Math.abs(original-garden)>2)throw new IllegalStateException("Garden did not follow the original surface: "+original+" vs "+garden);
        System.out.println("CORALINE_ESTATE_CHECK passed at "+c);
    }
    private static void verifyIslandEdge(net.minecraft.server.MinecraftServer server){
        var target=server.getLevel(OtherWorld.KEY);var c=realm.house;
        int edgeX=realm.sourceDoor.getX()+realm.dx+realm.radius+4,edgeZ=realm.sourceDoor.getZ()+realm.dz;
        if(target.getBlockState(new BlockPos(edgeX,c.getY()-4,edgeZ)).is(Blocks.WHITE_CONCRETE))
            throw new IllegalStateException("Static white boundary still exists");
        OtherWorldEstate.extendWhite(target,realm,new BlockPos(edgeX,c.getY(),edgeZ));
        int surface=target.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            realm.sourceDoor.getX()+realm.dx+realm.radius-4,edgeZ)-1;
        if(!target.getBlockState(new BlockPos(edgeX,surface,edgeZ)).is(Blocks.WHITE_CONCRETE))
            throw new IllegalStateException("Dynamic white ground did not meet the island");
        OtherWorldEstate.cleanupWhite(target);
        if(target.getBlockState(new BlockPos(edgeX,surface,edgeZ)).is(Blocks.WHITE_CONCRETE))
            throw new IllegalStateException("White ground remained after the visitor left the edge");
        System.out.println("CORALINE_ISLAND_EDGE_CHECK passed");
    }
    private static void move(net.minecraft.server.MinecraftServer server,boolean original){
        var player=server.getPlayerList().getPlayers().get(0);var home=original?HOME:realm.house;
        player.getPersistentData().putUUID("CoralineVisitDoor",door.id);player.getPersistentData().remove("CoralineInTunnel");
        player.teleportTo(original?server.overworld():server.getLevel(OtherWorld.KEY),home.getX()+.5,home.getY()+(original?2:18),home.getZ()+(original?-17:48),original?0:180,original?-12:16);
        player.getAbilities().flying=true;player.onUpdateAbilities();player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    }
    private static void moveInterior(net.minecraft.server.MinecraftServer server,int room){
        var player=server.getPlayerList().getPlayers().get(0);var c=realm.house;
        int x=0,y=3,z=0;float yaw=0,pitch=12;
        switch(room){
            case 3->{x=-5;z=12;y=3;yaw=100;}
            case 4->{x=-4;z=-4;y=3;yaw=120;}
            case 5->{x=16;z=-6;y=3;yaw=120;}
            case 6->{x=17;z=12;y=3;yaw=150;}
            case 7->{x=5;z=2;y=11;yaw=-145;}
            case 8->{x=1;z=13;y=11;yaw=140;}
        }
        player.teleportTo(server.getLevel(OtherWorld.KEY),c.getX()+x+.5,c.getY()+y,c.getZ()+z+.5,yaw,pitch);
        player.getAbilities().flying=true;player.onUpdateAbilities();player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(!Boolean.getBoolean("coraline.worldVisualCheck")||e.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL||phase<2)return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        if(seen!=phase){seen=phase;frames=0;}if(++frames<(phase==2?500:250))return;
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            String name=switch(phase){case 2->"other_world_scene_native";case 3->"sitting_room_native";case 4->"study_native";case 5->"kitchen_native";case 6->"supper_native";case 7->"bedroom_native";case 8->"upper_hall_native";default->"overworld_scene_native";};
            image.writeToFile(java.nio.file.Path.of("../artwork/"+name+".png"));
        }catch(Exception ex){throw new RuntimeException(ex);}
        System.out.println("CORALINE_WORLD_VISUAL_CHECK phase="+phase+" FPS="+mc.getFps());
        if(phase<8){int next=++phase;mc.getSingleplayerServer().execute(()->moveInterior(mc.getSingleplayerServer(),next));}
        else if(phase==8){phase=9;mc.getSingleplayerServer().execute(()->move(mc.getSingleplayerServer(),true));}
        else{phase=10;mc.stop();}
    }
}
