package com.coraline.secrets;

import com.coraline.feast.*;
import com.coraline.otherworld.*;
import com.coraline.registry.ModBlocks;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class HouseRevisionGameTests {
    @GameTest(template="empty") public static void fixedDisplayYawPointsTheDollIntoItsRoom(GameTestHelper h){
        var doll=net.minecraft.world.entity.EntityType.ITEM_DISPLAY.create(h.getLevel());doll.setPos(0,0,0);
        for(float yaw:new float[]{0,45,90,180,270,-135}){
            doll.setYRot(SecretSpies.displayYaw(yaw));double radians=Math.toRadians(yaw);var front=new Vec3(-Math.sin(radians)*4,0,Math.cos(radians)*4);
            h.assertTrue(SecretSpies.inDollView(doll,front)&&!SecretSpies.inDollView(doll,front.scale(-1)),"Authored room-facing yaw turned the doll into the corner: "+yaw);
        }doll.discard();h.succeed();
    }
    @GameTest(template="empty") public static void lockedDoorCannotToggleWithoutServerAuthorizationOrByRedstone(GameTestHelper h){
        var level=h.getLevel();var at=h.absolutePos(new BlockPos(1,2,1));level.setBlock(at.below(),Blocks.STONE.defaultBlockState(),2);
        var block=(com.coraline.block.LockedHouseDoorBlock)ModBlocks.LOCKED_HOUSE_DOOR.get();var state=block.defaultBlockState();level.setBlock(at,state,2);level.setBlock(at.above(),state.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),2);
        var p=new ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"LockedDoorQA"));
        block.use(state,level,at,p,InteractionHand.MAIN_HAND,new BlockHitResult(at.getCenter(),Direction.NORTH,at,false));
        h.assertFalse(level.getBlockState(at).getValue(DoorBlock.OPEN),"Unauthorised use predicted/opened a locked door");
        level.setBlock(at.east(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);block.neighborChanged(level.getBlockState(at),level,at,Blocks.REDSTONE_BLOCK,at.east(),false);
        h.assertFalse(level.getBlockState(at).getValue(DoorBlock.OPEN),"Redstone bypassed room key");
        block.setOpen(p,level,level.getBlockState(at),at,true);
        h.assertTrue(level.getBlockState(at).getValue(DoorBlock.OPEN)&&level.getBlockState(at.above()).getValue(DoorBlock.OPEN),"Authorized opening did not keep both halves coherent");h.succeed();
    }
    @GameTest(template="empty") public static void spyReportSurvivesAnUnloadedMotherAndReleasesItsTicket(GameTestHelper h){
        var level=h.getLevel().getServer().getLevel(OtherWorld.KEY);var p=new ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"UnloadedMotherQA"));
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.mom=UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.loop.phase=OtherMotherLoopState.Phase.EXPLORING;s.loop.motherPos=new BlockPos(76000,160,76000);VisitData.get(p.server).scenes.put(s.owner,s);
        s.piano=s.table=s.loop.motherPos;var sighting=s.loop.motherPos.east(200);p.setPos(Vec3.atBottomCenterOf(sighting.south(20)));
        try{SecretSpies.report(p,sighting);h.assertTrue(s.loop.secrets.search==SecretsState.Search.INVESTIGATING_NOISE&&s.loop.secrets.target.equals(sighting)&&s.loop.secrets.world.getLong("SpyReportOrigin")==sighting.asLong(),"No loaded actor caused the sighting to be dropped");
            h.assertTrue(s.loop.secrets.world.contains("PursuitTicket"),"Distant Mother has no entity-ticking lease");s.loop.secrets.search=SecretsState.Search.NORMAL;MotherSearch.holdSearch(p);h.assertFalse(s.loop.secrets.world.contains("PursuitTicket"),"Search completion retained its ticket");
        }finally{s.loop.secrets.search=SecretsState.Search.NORMAL;MotherSearch.holdSearch(p);VisitData.get(p.server).scenes.remove(s.owner);}h.succeed();
    }
}
