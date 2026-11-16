package com.coraline.otherworld;

import com.coraline.block.TinyDoorBlock;
import com.coraline.registry.ModBlocks;
import com.coraline.feast.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class SeamlessTunnelGameTests {
    private static ServerPlayer player(ServerLevel level){var p=new ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"TunnelQA"));p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);return p;}
    private static DoorBlockEntity door(ServerLevel level,BlockPos pos,Direction facing,UUID id){level.setBlock(pos,ModBlocks.TINY_DOOR.get().defaultBlockState().setValue(TinyDoorBlock.FACING,facing).setValue(TinyDoorBlock.OPEN,true).setValue(TinyDoorBlock.PORTAL,true),3);var door=(DoorBlockEntity)level.getBlockEntity(pos);door.doorId=id;return door;}
    @GameTest(template="empty",batch="seamless-shape") public static void openingDoesNotBuildAVisibleFabricCuboid(GameTestHelper h){
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(10,5,10));
        for(var facing:Direction.Plane.HORIZONTAL){var pos=origin.relative(facing,8);var d=door(level,pos,facing,UUID.randomUUID());h.assertTrue(TunnelPassage.ensure(level,d),"Freestanding entrance failed");
            for(int depth=1;depth<=3;depth++)for(int side=-1;side<=1;side++)for(int y=-1;y<=1;y++)h.assertFalse(level.getBlockState(pos.relative(facing,-depth).relative(facing.getClockWise(),side).offset(0,y,0)).is(ModBlocks.TUNNEL_FABRIC.get()),"Opening painted the room/outdoors blue");
            TunnelPassage.restore(level,d);level.removeBlock(pos,false);
        }h.succeed();
    }
    @GameTest(template="empty",batch="seamless-shape",timeoutTicks=150)
    public static void autoCrawlAndRestorablePassagesFaceAllDirections(GameTestHelper h){
        var level=h.getLevel();var p=player(level);
        for(var facing:Direction.Plane.HORIZONTAL){var pos=h.absolutePos(new BlockPos(10+facing.ordinal()*12,2,10));var marker=pos.relative(facing,-2);level.setBlock(marker,Blocks.STONE.defaultBlockState(),2);var door=door(level,pos,facing,UUID.randomUUID());
            h.assertTrue(TunnelPassage.ensure(level,door),"Unable to construct passage: "+facing);h.assertTrue(level.isEmptyBlock(marker),"Back wall wasn't opened");
            p.setPos(TunnelPassage.inside(pos,facing,-.75));p.setShiftKeyDown(false);TunnelPassage.tick(p);
            h.assertTrue(p.getPose()==Pose.SWIMMING&&!p.isShiftKeyDown()&&p.getBoundingBox().getYsize()<.7,"Approach didn't automatically lower the player");
            h.assertFalse(p.getPersistentData().getBoolean("CoralineInTunnel"),"Approach teleported before entering the passage");
            p.setPos(TunnelPassage.inside(pos,facing,1));var saved=door.saveWithoutMetadata();var reload=new DoorBlockEntity(pos,door.getBlockState());reload.load(saved);h.assertTrue(reload.passageBuilt&&reload.passageOriginals.size()==door.passageOriginals.size(),"Restoration snapshot doesn't survive a save");
            door.getBlockState().getBlock().use(door.getBlockState(),level,pos,p,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),facing,pos,false));h.assertTrue(level.getBlockState(pos).getValue(TinyDoorBlock.OPEN),"Door closed on a crawling player");
            p.setPos(TunnelPassage.inside(pos,facing,-2));TunnelPassage.tick(p);h.assertTrue(p.getForcedPose()==null,"Backing away left the player forced to crawl");TunnelPassage.restore(level,door);h.assertTrue(level.getBlockState(marker).is(Blocks.STONE),"Original wall wasn't restored");level.removeBlock(pos,false);
        }h.succeed();
    }
    @GameTest(template="empty",batch="seamless-container",timeoutTicks=100)
    public static void passageDoesNotOverwriteContainerOrPartialFootprint(GameTestHelper h){var level=h.getLevel();var pos=h.absolutePos(new BlockPos(12,2,12));var door=door(level,pos,Direction.NORTH,UUID.randomUUID());var chest=pos.south(2);level.setBlock(chest,Blocks.CHEST.defaultBlockState(),3);var inventory=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(chest);inventory.setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND));var before=level.getBlockState(pos.south());h.assertFalse(TunnelPassage.ensure(level,door),"Passage overwrote a container");h.assertTrue(inventory.getItem(0).is(net.minecraft.world.item.Items.DIAMOND)&&before.equals(level.getBlockState(pos.south()))&&!door.passageBuilt,"Rejected construction changed terrain or inventory");h.succeed();}
    @GameTest(template="empty",batch="seamless-roundtrip",timeoutTicks=300)
    public static void bothDirectionsArriveCrawlingInsideTheDestinationDoor(GameTestHelper h){
        var source=h.getLevel();var other=source.getServer().getLevel(OtherWorld.KEY);var a=new BlockPos(24000,100,24000);var b=new BlockPos(25000,100,25000);var p=player(source);var d=new DoorData.Door();d.owner=p.getUUID();d.pos=a;d.placed=true;d.activeUntil=24000;DoorData.get(p.server).doors.put(d.id,d);source.setDayTime(15000);
        var sourceDoor=door(source,a,Direction.NORTH,d.id);var targetDoor=door(other,b,Direction.EAST,d.id);TunnelPassage.ensure(source,sourceDoor);TunnelPassage.ensure(other,targetDoor);
        var r=new RealmData.Realm();r.door=d.id;r.sourceDoor=a;r.doorPos=b;r.house=b;r.porch=b;r.table=b;r.piano=b;r.bed=b;r.dx=1000;r.dz=1000;r.radius=500;r.ready=true;r.islandComplete=true;r.estateVersion=OtherWorldEstate.VERSION;RealmData.get(p.server).realms.put(d.id,r);
        var scene=new VisitData.Scene();scene.owner=p.getUUID();scene.door=d.id;scene.stage=VisitData.Stage.DONE;scene.table=b;scene.piano=b;scene.loop.phase=OtherMotherLoopState.Phase.EXPLORING;scene.loop.visits=2;scene.loop.trust=.5F;VisitData.get(p.server).scenes.put(p.getUUID(),scene);
        p.setPos(TunnelPassage.inside(a,Direction.NORTH,-.7));p.setYRot(0);p.setShiftKeyDown(false);TunnelPassage.tick(p);h.assertTrue(p.serverLevel()==source,"Door approach changed dimensions");
        p.setPos(TunnelPassage.inside(a,Direction.NORTH,2.3));p.setDeltaMovement(new Vec3(0,0,.12));TunnelPassage.tick(p);h.assertTrue(p.serverLevel()==other&&p.getPersistentData().getBoolean("CoralineInTunnel")&&p.getPose()==Pose.SWIMMING,"Walk-in crossing required sneaking or failed");h.assertTrue(p.getDeltaMovement().horizontalDistance()>.1&&Math.abs(p.getYRot())<.01,"Transfer lost momentum or direction");
        p.getPersistentData().putLong("CoralinePortalCooldown",0);p.setPos(r.tunnel().getX()+.5,300,r.tunnel().getZ()+1.5);OtherWorld.playerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));
        h.assertTrue(p.serverLevel()==source&&p.getForcedPose()==Pose.SWIMMING&&Math.abs(scene.loop.trust-.5F)<.001,"Backing out didn't return to the source tunnel without an entry reward");
        p.setPos(TunnelPassage.inside(a,Direction.NORTH,-1.2));TunnelPassage.tick(p);h.assertTrue(source.getBlockState(a).getValue(TinyDoorBlock.OPEN)&&d.closedNight==Long.MIN_VALUE,"Aborting a crossing consumed the night's entrance");
        scene.loop.phase=OtherMotherLoopState.Phase.EXPLORING;p.getPersistentData().putLong("CoralinePortalCooldown",0);p.getPersistentData().putLong("CoralineTunnelAttempt",0);p.setPos(TunnelPassage.inside(a,Direction.NORTH,2.3));p.setYRot(0);p.setDeltaMovement(new Vec3(0,0,.12));TunnelPassage.tick(p);
        p.getPersistentData().putLong("CoralineTunnelStartedAt",p.server.overworld().getGameTime()-200);p.getPersistentData().putLong("CoralinePortalCooldown",0);p.setPos(r.tunnel().getX()+.5,300,r.tunnel().getZ()+15.5);OtherWorld.playerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));
        h.assertTrue(!p.getPersistentData().getBoolean("CoralineInTunnel")&&Math.abs(TunnelPassage.depth(p,b,Direction.EAST)-2)<.01&&p.getForcedPose()==Pose.SWIMMING,"Arrival dumped the player in the room instead of its tunnel");h.assertTrue(Math.abs(scene.loop.trust-.58F)<.001,"Inbound arrival failed to award entry trust exactly once");
        scene.loop.phase=OtherMotherLoopState.Phase.EXPLORING;p.getPersistentData().putLong("CoralinePortalCooldown",0);p.getPersistentData().putLong("CoralineTunnelAttempt",0);p.setPos(TunnelPassage.inside(b,Direction.EAST,2.3));p.setYRot(90);p.setDeltaMovement(new Vec3(-.12,0,0));TunnelPassage.tick(p);
        h.assertTrue(p.getPersistentData().getBoolean("CoralineReturning")&&p.getPersistentData().getBoolean("CoralineInTunnel"),"Other World passage didn't begin the reverse crossing");p.getPersistentData().putLong("CoralinePortalCooldown",0);p.setPos(r.tunnel().getX()+.5,300,r.tunnel().getZ()+1.5);OtherWorld.playerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));
        h.assertTrue(p.serverLevel()==source&&p.getForcedPose()==Pose.SWIMMING&&Math.abs(TunnelPassage.depth(p,a,Direction.NORTH)-2)<.01,"Return didn't arrive in the original local tunnel");h.assertTrue(source.getBlockState(a).getValue(TinyDoorBlock.OPEN),"Return closed the exit before the player cleared it");
        p.setPos(TunnelPassage.inside(a,Direction.NORTH,-1.2));TunnelPassage.tick(p);h.assertTrue(p.getForcedPose()==null&&!source.getBlockState(a).getValue(TinyDoorBlock.OPEN)&&!sourceDoor.passageBuilt,"Exit didn't restore standing/door/wall after leaving");
        TunnelPassage.restore(other,targetDoor);VisitData.get(p.server).scenes.remove(p.getUUID());RealmData.get(p.server).realms.remove(d.id);DoorData.get(p.server).doors.remove(d.id);h.succeed();
    }
}
