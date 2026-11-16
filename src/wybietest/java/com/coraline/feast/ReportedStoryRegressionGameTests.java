package com.coraline.feast;

import java.util.UUID;
import com.coraline.block.TinyDoorBlock;
import com.coraline.otherworld.*;
import com.coraline.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

/** Real actors/doors/custody, not merely synthetic phase or source-string checks. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class ReportedStoryRegressionGameTests {
    @GameTest(template="empty") public static void movingWardrobeBlocksOpenPortalAndReleasesAtTheRealEscapeStep(GameTestHelper h){
        var l=h.getLevel().getServer().getLevel(OtherWorld.KEY);var p=new ServerPlayer(l.getServer(),l,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"WardrobeGateQA"));
        var s=new VisitData.Scene();s.owner=p.getUUID();s.stage=VisitData.Stage.DONE;var record=new DoorData.Door();record.owner=s.owner;s.door=record.id;
        var at=new BlockPos(130300,180,130300);var original=l.getBlockState(at);h.assertTrue(l.getBlockEntity(at)==null,"Fixture must not replace a block entity");
        DoorData.get(p.server).doors.put(record.id,record);VisitData.get(p.server).scenes.put(s.owner,s);var n=FirstEscapeStory.data(s);n.putBoolean("Started",true);n.putLong("LittleDoor",at.asLong());
        try{
            for(var facing:Direction.Plane.HORIZONTAL){
                var state=ModBlocks.TINY_DOOR.get().defaultBlockState().setValue(TinyDoorBlock.FACING,facing).setValue(TinyDoorBlock.OPEN,true).setValue(TinyDoorBlock.PORTAL,true);l.getChunkAt(at);l.setBlock(at,state,2);
                var door=(DoorBlockEntity)l.getBlockEntity(at);door.doorId=s.door;
                for(var phase:java.util.List.of(FirstEscapeTimeline.Phase.ROOM_DARK,FirstEscapeTimeline.Phase.ROOM_TRANSFORMING,FirstEscapeTimeline.Phase.COCOA_A,FirstEscapeTimeline.Phase.COCOA_E,FirstEscapeTimeline.Phase.PUSHING)){
                    n.putString("Phase",phase.name());door.refreshStoryPassageBlock();
                    h.assertTrue(door.storyPassageBlocked&&!state.getCollisionShape(l,at).isEmpty(),"Open portal permits outrunning wardrobe during "+phase+" / "+facing);
                    h.assertTrue(door.getUpdateTag().getBoolean("StoryPassageBlocked"),"Client prediction did not receive gate");
                    h.assertTrue(!door.saveWithoutMetadata().contains("StoryPassageBlocked"),"Temporary gate became a permanent saved seal");
                }
                n.putString("Phase","OPEN_LITTLE_DOOR");door.refreshStoryPassageBlock();h.assertTrue(!door.storyPassageBlocked&&state.getCollisionShape(l,at).isEmpty(),"Real escape did not release portal");
                n.putString("Phase","ROOM_TRANSFORMING");door.refreshStoryPassageBlock();n.putBoolean("Started",false);door.refreshStoryPassageBlock();h.assertTrue(!door.storyPassageBlocked,"Retired story retained tunnel gate");n.putBoolean("Started",true);
            }
        }finally{l.setBlock(at,original,2);DoorData.get(p.server).doors.remove(record.id);VisitData.get(p.server).scenes.remove(s.owner);}h.succeed();
    }
    @GameTest(template="empty") public static void mirrorWritingIsCenteredOnFrontGlassAndShattersDuringVoiceTail(GameTestHelper h){
        for(int i=0;i<MissingParentsCatTimeline.strokes();i++)for(double u:new double[]{0,1}){
            var point=MissingParentsCatTimeline.point(i,u);h.assertTrue(point[1]>=1.21&&point[1]<=1.59,"HELP US is not in the middle of the front glass");
        }
        h.assertTrue(MissingParentsCatTimeline.shatterCue(200)==150,"Mirror shatter no longer precedes C by 2.5 seconds");
        var face=new org.joml.Vector3f(0,0,-1);new org.joml.Matrix4f().rotateX((float)-Math.PI/2).scale(.32F,-.32F,-.32F).transformDirection(face);
        h.assertTrue(face.y>.3,"Parent dolls' button-eye face points at the ground");h.succeed();
    }
    @GameTest(template="empty",batch="return-door-facing") public static void restoredDoorArrivalMatchesAllFourFacings(GameTestHelper h){
        var p=MissingParentsTeaGameTests.fixture(h);var s=OtherMotherLoop.scene(p);var l=p.serverLevel();var at=new BlockPos(129700,180,129700);
        var n=ParentRescueStory.data(s);n.putBoolean("Started",true);var scene=new CompoundTag();scene.putLong("LittleDoor",at.asLong());n.put("Scenery",scene);
        try{
            for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){var cell=at.offset(x,0,z);l.getChunkAt(cell);l.setBlock(cell.below(),Blocks.STONE.defaultBlockState(),2);for(int y=0;y<3;y++)l.setBlock(cell.above(y),Blocks.AIR.defaultBlockState(),2);}
            for(var facing:Direction.Plane.HORIZONTAL){
                l.setBlock(at,ModBlocks.TINY_DOOR.get().defaultBlockState().setValue(TinyDoorBlock.FACING,facing),2);p.setPos(at.getCenter().add(0,0,5));
                h.assertTrue(ParentRescueStory.emerge(p,s),"No threshold landing for "+facing);
                h.assertTrue(p.distanceToSqr(Vec3.atBottomCenterOf(at.relative(facing)))<.01,"Wrong arrival side for "+facing);
                h.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(p.getYRot()-facing.toYRot()))<.01,"Arrival looks away from room");
            }
        }finally{l.removeBlock(at,false);MissingParentsTeaGameTests.remove(p,s);}h.succeed();
    }
    @GameTest(template="empty") public static void staleLeaseGoalAndCreativeCatPunchAreSafe(GameTestHelper h){
        var p=MissingParentsTeaGameTests.fixture(h);var s=OtherMotherLoop.scene(p);
        var cat=(com.coraline.entity.CharacterEntity)ShowActors.acquire(p,s,"cat",p.serverLevel(),p.position());
        try{
            h.assertTrue(cat!=null&&ShowActors.storyCat(cat),"Canonical Cat lease missing");p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            float health=cat.getHealth();h.assertFalse(cat.hurt(p.damageSources().playerAttack(p),1000),"Creative punch damaged the story Cat");
            h.assertTrue(cat.getHealth()==health&&!cat.canBeCollidedWith()&&!cat.isPushable(),"Canonical Cat blocks the crawl or lost health");
            var goal=new ShowActors.ControlGoal(cat);h.assertTrue(goal.canUse(),"Fixture goal not leased");
            cat.getPersistentData().remove("ShowActorOwner");h.assertFalse(goal.canContinueToUse(),"Stale goal continued after lease removal");goal.tick();
            h.assertFalse(ShowActors.storyCat(cat),"Ordinary cat inherited story-only protection");
        }finally{if(cat!=null)cat.discard();MissingParentsTeaGameTests.remove(p,s);}h.succeed();
    }
    @GameTest(template="empty",batch="rescue-chair-return",timeoutTicks=500)
    public static void fatherEntersMountsWithoutWalkingAwayAndKeySceneAdvances(GameTestHelper h){
        var l=h.getLevel().getServer().getLevel(OtherWorld.KEY);
        var p=new ServerPlayer(l.getServer(),l,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ChairReturnQA"));
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.loop.event.putInt("Serial",2);VisitData.get(p.server).scenes.put(s.owner,s);
        var n=ParentRescueStory.data(s);n.putBoolean("Started",true);n.putString("Phase","FATHER_ENTRY");var scene=new CompoundTag();scene.putLong("House",new BlockPos(129600,180,129600).asLong());n.put("Scenery",scene);
        var little=FirstEscapeScene.at(s,13,6,38);scene.putLong("LittleDoor",little.asLong());scene.putBoolean("RoomBuilt",true);scene.putInt("WallpaperRevision",2);
        FirstEscapeStory.data(s).putLong("LittleDoor",little.asLong());
        for(int x=13;x<=35;x++)for(int z=36;z<=52;z++){var at=FirstEscapeScene.at(s,x,6,z);l.getChunkAt(at);l.setBlock(at.below(),Blocks.STONE.defaultBlockState(),2);for(int y=0;y<4;y++)l.setBlock(at.above(y),Blocks.AIR.defaultBlockState(),2);}
        for(int z=36;z<=52;z++)for(int y=6;y<=8;y++)l.setBlock(FirstEscapeScene.at(s,31,y,z),Blocks.STONE.defaultBlockState(),2);
        for(int z:new int[]{41,42}){var at=FirstEscapeScene.at(s,31,6,z);var door=Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING,Direction.EAST);l.setBlock(at,door,2);l.setBlock(at.above(),door.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),2);}
        l.setBlock(little,ModBlocks.TINY_DOOR.get().defaultBlockState().setValue(TinyDoorBlock.FACING,Direction.EAST),2);
        var original=ModEntities.MOM.get().create(l);original.makeOtherParent(0);original.setPos(Vec3.atBottomCenterOf(FirstEscapeScene.room(s).west(3)));l.addFreshEntity(original);s.mom=original.getUUID();
        p.setPos(Vec3.atBottomCenterOf(little.east()));h.assertTrue(ParentRescueStory.emerge(p,s),"Actual threshold arrival failed");
        h.assertTrue(p.distanceToSqr(Vec3.atBottomCenterOf(little.east()))<.01,"Player spawned in room center instead of matching door");
        FirstEscapeScene.roomProps(p,s);ParentRescueStory.grantKey(p,s);
        var count=new int[]{0};var heldFather=new Vec3[1];
        h.onEachTick(()->{
            ParentRescueStory.tick(p,s);count[0]++;
            var dad=ShowActors.actor(p,s,"evil_other_father");
            if(dad!=null)h.assertTrue(dad.getBbWidth()<=.60F,"Father still clips an open 3/16-block door leaf");
            if(ParentRescueStory.phase(s)==ParentRescueStory.Phase.KEY_SCENE){
                h.assertTrue(p.getVehicle()==FirstEscapeScene.prop(p,s,"BugChair"),"Phase advanced without actual chair capture");
                if(heldFather[0]==null)heldFather[0]=dad.position();else h.assertTrue(dad.distanceToSqr(heldFather[0])<.01,"Father walked away after placing chair");
            }
            if(ParentRescueStory.phase(s)==ParentRescueStory.Phase.E){
                h.assertTrue(n.getBoolean("KeyTaken")&&n.getBoolean("KeySwallowed")&&n.getBoolean("DoorLocked")&&n.getBoolean("WardrobeBlocking"),"Chair skipped real key/door/wardrobe choreography");
                h.assertTrue(heldFather[0]!=null,"Chair/key phases were never exercised");
                n.putBoolean("Seated",false);p.stopRiding();original.discard();if(dad!=null)dad.discard();VisitData.get(p.server).scenes.remove(s.owner);h.succeed();
            }else if(count[0]>=450){n.putBoolean("Seated",false);p.stopRiding();h.fail("Return stalled: "+n.getString("Phase")+" step="+n.getInt("KeyStep")+" "+n.getString("Blocker"));}
        });
    }
    @GameTest(template="empty",batch="shore-bike-route") public static void bicycleClimbsTheBankBeforeItsCentreReachesTheStep(GameTestHelper h){
        var p=MissingParentsTeaGameTests.fixture(h);var s=OtherMotherLoop.scene(p);var l=p.serverLevel();var base=new BlockPos(130100,180,130100);var bike=ModEntities.WYBIE_BICYCLE.get().create(l);
        try{
            for(int x=-6;x<=22;x++)for(int z=-6;z<=6;z++){var cell=base.offset(x,0,z);l.getChunkAt(cell);for(int y=-2;y<=4;y++)l.setBlock(cell.above(y),y<0||x>=2&&y==0?Blocks.SAND.defaultBlockState():Blocks.AIR.defaultBlockState(),2);}
            bike.setPos(Vec3.atBottomCenterOf(base));bike.setYRot(-90);var start=bike.position();
            // The first raised block is not yet beneath the centre. The full
            // wheelbase must step up instead of colliding against its side.
            bike.storyAdvance(Vec3.atBottomCenterOf(base.east(1)));
            h.assertTrue(bike.getX()>start.x+.5&&bike.getY()==start.y+1&&bike.speed()>.5,"Bike pedals at the one-block bank instead of climbing it");
            for(int x=2;x<=12;x++)for(int tick=0;tick<3;tick++)bike.storyAdvance(Vec3.atBottomCenterOf(base.east(x).above()));
            h.assertTrue(bike.getX()>start.x+10,"Bicycle failed to continue away after the bank");
            h.assertFalse(l.getBlockCollisions(bike,StoryBikeRoute.bounds(bike.position(),bike.getYRot())).iterator().hasNext(),"Departure crossed solid terrain");
        }finally{bike.discard();MissingParentsTeaGameTests.remove(p,s);}h.succeed();
    }
    @GameTest(template="empty",batch="shore-bike-spawn") public static void bicycleSpawnRequiresAnActualDryFirstStep(GameTestHelper h){
        var p=MissingParentsTeaGameTests.fixture(h);var s=OtherMotherLoop.scene(p);var l=p.serverLevel();var base=new BlockPos(130300,180,130300);var bike=ModEntities.WYBIE_BICYCLE.get().create(l);
        try{
            for(int x=-8;x<=12;x++)for(int z=-8;z<=8;z++){var cell=base.offset(x,0,z);l.getChunkAt(cell);for(int y=-2;y<=4;y++)l.setBlock(cell.above(y),y<0?Blocks.SAND.defaultBlockState():y==0&&x<1?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState(),2);}
            p.setPos(Vec3.atBottomCenterOf(base.west(3)));var spawn=StoryBikeRoute.spawn(bike,Vec3.atBottomCenterOf(base),p);bike.setPos(spawn);double angle=Math.toRadians(bike.getYRot());
            var step=StoryBikeRoute.step(bike,spawn,spawn.x-Math.sin(angle),spawn.z+Math.cos(angle),bike.getYRot());
            h.assertTrue(step!=null&&!l.containsAnyLiquid(StoryBikeRoute.bounds(spawn,bike.getYRot())),"Spawn selected a stranded/wet bike pose");
            bike.storyAdvance(step);h.assertTrue(bike.position().subtract(spawn).horizontalDistance()>.5,"Mounted departure did not move on its first frame");
        }finally{bike.discard();MissingParentsTeaGameTests.remove(p,s);}h.succeed();
    }
    @GameTest(template="empty",batch="forest-bike-route") public static void storyBikeRoutesAroundTrunksAndReallyMovesFast(GameTestHelper h){
        var p=MissingParentsTeaGameTests.fixture(h);var s=OtherMotherLoop.scene(p);var l=p.serverLevel();var base=new BlockPos(129900,180,129900);
        var bike=ModEntities.WYBIE_BICYCLE.get().create(l);
        try{
            for(int x=-8;x<=28;x++)for(int z=-8;z<=8;z++){var cell=base.offset(x,0,z);l.getChunkAt(cell);l.setBlock(cell.below(),Blocks.STONE.defaultBlockState(),2);for(int y=0;y<4;y++)l.setBlock(cell.above(y),Blocks.AIR.defaultBlockState(),2);}
            for(int y=0;y<3;y++)l.setBlock(base.offset(4,y,0),Blocks.BIRCH_LOG.defaultBlockState(),2);
            l.setBlock(base.offset(5,2,1),Blocks.BIRCH_LEAVES.defaultBlockState(),2);
            p.setPos(Vec3.atBottomCenterOf(base.west(4)));bike.setPos(Vec3.atBottomCenterOf(base));var start=bike.position();
            long[] path=new long[0];for(int attempt=0;attempt<3&&path.length==0;attempt++)path=StoryBikeRoute.plan(bike,p);
            h.assertTrue(path.length>0,"No full-bike route around the trunk");
            for(long raw:path){var target=Vec3.atBottomCenterOf(BlockPos.of(raw));for(int step=0;step<4&&bike.position().subtract(target).horizontalDistanceSqr()>.02;step++){
                bike.storyAdvance(target);h.assertFalse(l.getBlockCollisions(bike,StoryBikeRoute.bounds(bike.position(),bike.getYRot())).iterator().hasNext(),"Bike/rider clipped trunk or low canopy");
            }}
            h.assertTrue(bike.distanceToSqr(start)>9&&bike.wheelRotation>0,"Bike only pedalled in place");
            bike.getPersistentData().putUUID("FirstEscapeOwner",s.owner);bike.stop();h.assertTrue(bike.speed()==0,"Blocked story bicycle still pedals");
        }finally{bike.discard();MissingParentsTeaGameTests.remove(p,s);}h.succeed();
    }
}
