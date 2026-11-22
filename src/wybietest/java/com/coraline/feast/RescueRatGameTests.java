package com.coraline.feast;

import com.coraline.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class RescueRatGameTests {
    private static ServerPlayer player(GameTestHelper h){var l=h.getLevel();var p=new ServerPlayer(l.getServer(),l,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"RatQA"));p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);var s=new VisitData.Scene();s.owner=p.getUUID();s.door=java.util.UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.loop.event.putInt("Serial",7);VisitData.get(p.server).scenes.put(s.owner,s);ParentRescueStory.data(s).putBoolean("Started",true);ParentRescueStory.data(s).put("Scenery",new CompoundTag());RescueChapter.data(s).putBoolean("Started",true);return p;}
    @GameTest(template="empty") public static void planningAndPhysicsRequireClearanceForTheEntireMountedAssembly(GameTestHelper h){var p=player(h);var s=OtherMotherLoop.scene(p);var l=h.getLevel();var start=h.absolutePos(new BlockPos(1,123,1001));var target=start.east(7);var rat=ModEntities.MOUSE.get().create(l);rat.getPersistentData().putUUID("RescueOwner",s.owner);rat.refreshDimensions();rat.setPos(Vec3.atBottomCenterOf(start));rat.setOnGround(true);var ball=CinematicPropEntity.TYPE.get().create(l);ball.configure(s,RescueProps.RAT_BALL,0);ball.setPos(rat.position());
        for(int x=-1;x<=9;x++)for(int z=-1;z<=1;z++){l.setBlock(start.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);for(int y=0;y<3;y++)l.setBlock(start.offset(x,y,z),y==1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);}
        try{var low=rat.rescueBallPath(target);h.assertTrue(low==null||!low.canReach(),"Ball route was planned using only mouse-height clearance");h.assertFalse(l.noCollision(ball,ball.getBoundingBox()),"Mounted rat can clip through a low lintel");
            for(int x=-1;x<=9;x++)for(int z=-1;z<=1;z++){l.setBlock(start.offset(x,1,z),Blocks.AIR.defaultBlockState(),2);l.setBlock(start.offset(x,2,z),Blocks.STONE.defaultBlockState(),2);}
            var clear=rat.rescueBallPath(target);h.assertTrue(clear!=null&&clear.canReach(),"Ordinary two-block corridor cannot admit the assembly");h.assertTrue(Math.abs(rat.getBbHeight()-.9F)<.001,"Temporary path-planning dimensions leaked into the mouse model");h.assertTrue(l.noCollision(ball,ball.getBoundingBox()),"Assembly collision disagrees with the valid corridor");
        }finally{rat.discard();ball.discard();VisitData.get(p.server).scenes.remove(s.owner);for(int x=-1;x<=9;x++)for(int z=-1;z<=1;z++)for(int y=-1;y<3;y++)l.setBlock(start.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);}h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=250) public static void realMountedBallRollsAcrossCarpetAndUpAFullStep(GameTestHelper h){
        var p=player(h);var s=OtherMotherLoop.scene(p);var l=h.getLevel();var start=h.absolutePos(new BlockPos(1,123,1101));var target=start.offset(8,1,0);
        for(int x=-1;x<=10;x++)for(int z=-1;z<=1;z++){int top=x>=4?0:-1;for(int y=-1;y<=3;y++)l.setBlock(start.offset(x,y,z),y<=top?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);}l.setBlock(start.east(2),Blocks.RED_CARPET.defaultBlockState(),2);p.setPos(Vec3.atBottomCenterOf(start.south(2)));
        var rat=ModEntities.MOUSE.get().create(l);rat.getPersistentData().putUUID("RescueOwner",s.owner);rat.refreshDimensions();rat.setNoAi(true);rat.setPos(Vec3.atBottomCenterOf(start));l.addFreshEntity(rat);
        RescueChapter.data(s).putUUID("ChaseRat",rat.getUUID());
        int[] ticks={0};
        java.util.UUID[] discarded={null};
        h.onEachTick(()->{LoopProps.hold(l,start);LoopProps.hold(l,target);if(++ticks[0]<25)return;RescueEncounters.rollRat(p,s,rat,target,ticks[0],2);var ball=RescueProps.find(p,s,"RatBall2");if(ball!=null){
                if(discarded[0]==null){
                    discarded[0]=ball.getUUID();
                    ball.ejectPassengers();
                    ball.discard();
                    return;
                }
                h.assertFalse(ball.getUUID().equals(discarded[0]),"Destroyed ball UUID was reused");
                h.assertTrue(rat.getVehicle()==ball,"Actual rat lost its ball during movement");h.assertTrue(!l.getBlockCollisions(ball,ball.getBoundingBox().deflate(.001)).iterator().hasNext(),"Moving mounted assembly penetrated terrain: "+ball.position()+" / "+ball.getBoundingBox());if(ball.position().distanceToSqr(Vec3.atBottomCenterOf(target))<.36){RescueProps.retire(p,s,"RatBall2");rat.discard();VisitData.get(p.server).scenes.remove(s.owner);h.succeed();return;}}
            if(ticks[0]>225){String detail=RescueChapter.data(s).getString("Blocker")+" at "+rat.position();RescueProps.retire(p,s,"RatBall2");rat.discard();VisitData.get(p.server).scenes.remove(s.owner);h.fail("Physical rat-ball movement stalled: "+detail);}
        });
    }
    @GameTest(template="empty",timeoutTicks=200) public static void ratReturnPersistsTransferredOriginalAndRecoversLegacyCircusCheckpoint(GameTestHelper h){
        var p=player(h);var s=OtherMotherLoop.scene(p);
        var fixture=h.getLevel();
        var circus=p.server.getLevel(ActivityWorld.KEY);var house=p.server.getLevel(com.coraline.otherworld.OtherWorld.KEY);
        h.assertTrue(circus!=null&&house!=null,"Rat return worlds are unavailable");
        var fixtureAt=h.absolutePos(new BlockPos(2,3,2));
        var oldAt=MouseCircusSite.center(p,s).south(10);
        var returnedAt=new BlockPos(24500,180,24500);
        ShowStageLease.hold(p,s,true);
        // Both destinations must be entity-ticking before native transfers;
        // the actual player holds the attic on the real playable route.
        LoopProps.hold(house,returnedAt);
        fixture.getChunkAt(fixtureAt);circus.getChunkAt(oldAt);house.getChunkAt(returnedAt);
        var rat=ModEntities.MOUSE.get().create(fixture);
        h.assertTrue(rat!=null,"The original mouse type is unavailable");
        rat.doorId=s.door;rat.circusStyle(6);rat.catPose(0,1);rat.spying(true);rat.setInvulnerable(true);
        rat.getPersistentData().putBoolean("CoralineDecoration",true);
        rat.getPersistentData().putUUID("RescueOwner",s.owner);
        rat.setPos(Vec3.atBottomCenterOf(fixtureAt));
        h.assertTrue(fixture.addFreshEntity(rat),"Could not register the original rat in the native GameTest spawn world");
        var original=rat;
        var circusRat=new com.coraline.otherworld.MouseEntity[1];
        var returnedRat=new com.coraline.otherworld.MouseEntity[1];
        // Do not double-teleport in one tick: native dimensions register their
        // actor on subsequent ticks, just as the story's circus visit does.
        h.runAfterDelay(8,()->{
            var entered=ShowTravel.guest(original,p,s,circus,Vec3.atBottomCenterOf(oldAt),true,"RatTestCircusEntry");
            h.assertTrue(entered instanceof com.coraline.otherworld.MouseEntity
                    &&original.getUUID().equals(entered.getUUID())&&entered.level()==circus,
                    "Moving the original rat into its authored circus lost its saved native identity");
            circusRat[0]=(com.coraline.otherworld.MouseEntity)entered;
        });
        h.runAfterDelay(18,()->{
            h.assertTrue(circusRat[0]!=null,"The original rat never registered at its circus checkpoint");
            var id=circusRat[0].getUUID();var chapter=RescueChapter.data(s);
            chapter.putString("Phase","RAT_CHASE");
            chapter.putUUID("ChaseRat",id);chapter.putInt("RatLeg",1);
            RescueEncounters.recordChaseRat(chapter,circusRat[0]);
            LoopProps.hold(house,returnedAt);
            var moved=ShowTravel.guest(circusRat[0],p,s,house,Vec3.atBottomCenterOf(returnedAt),true,"RescueRatReturn");
            h.assertTrue(moved instanceof com.coraline.otherworld.MouseEntity&&id.equals(moved.getUUID())
                    &&moved.level()==house,"Native circus-to-attic transfer lost the original rat identity");
            returnedRat[0]=(com.coraline.otherworld.MouseEntity)moved;
            chapter.putInt("RatLeg",2);
            chapter.putLong("RatAt",oldAt.asLong());
            chapter.putString("RatDimension",circus.dimension().location().toString());
        });
        h.runAfterDelay(35,()->{
            try{
                h.assertTrue(returnedRat[0]!=null,"The original circus-to-attic transition never completed");
                s.loop=OtherMotherLoopState.load(s.loop.save());
                var found=RescueEncounters.chaseRat(p,s);
                var id=returnedRat[0].getUUID();
                h.assertTrue(found!=null&&id.equals(found.getUUID())&&found.level()==house
                                &&found.getPersistentData().hasUUID("RescueOwner")
                                &&s.owner.equals(found.getPersistentData().getUUID("RescueOwner")),
                        "A saved original rat must be found after native registration and reload. "
                                +"indexed="+(house.getEntity(id)!=null)
                                +", destinationLoaded="+house.hasChunkAt(returnedAt)
                                +", returnedRemoved="+returnedRat[0].isRemoved());
                RescueEncounters.recordChaseRat(RescueChapter.data(s),found);
                s.loop=OtherMotherLoopState.load(s.loop.save());
                var reloaded=RescueChapter.data(s);
                h.assertTrue(reloaded.getLong("RatAt")==found.blockPosition().asLong()
                        &&reloaded.getString("RatDimension").equals(house.dimension().location().toString())
                        &&id.equals(reloaded.getUUID("ChaseRat")),
                        "Ratmaster reload will look for a transferred rat in the old circus world");
            }finally{
                if(returnedRat[0]!=null)returnedRat[0].discard();
                original.discard();ShowStageLease.release(p,s,true);
                VisitData.get(p.server).scenes.remove(s.owner);
            }
            h.succeed();
        });
    }
    private RescueRatGameTests(){}
}
