package com.coraline.feast;

import java.util.*;
import com.coraline.entity.CharacterEntity;
import com.coraline.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class CurrentStoryRepairsGameTests {
    @GameTest(template="empty") public static void halfBlockPushesDependOnDistanceAndPreserveOldSaves(GameTestHelper h){
        var n=new CompoundTag();n.putLong("Door",BlockPos.ZERO.asLong());n.putString("Inward","east");
        n.putLong("Origin0",new BlockPos(4,0,0).asLong());n.putLong("Origin1",new BlockPos(12,0,0).asLong());
        for(int i=0;i<2;i++){h.assertTrue(BedroomBarricade.advance(n,i),"First push rejected");h.assertTrue(Math.abs(n.getDouble("Travel"+i)-.5)<.000001,"Far piece jumped farther per click");}
        int near=1,far=1;while(near<100&&BedroomBarricade.advance(n,0))near++;while(far<100&&BedroomBarricade.advance(n,1))far++;
        h.assertTrue(far>near&&near>4,"All chairs still take four giant pushes");
        var old=new CompoundTag();old.putLong("Door",BlockPos.ZERO.asLong());old.putString("Inward","east");old.putLong("Origin0",new BlockPos(4,0,0).asLong());old.putInt("Push0",2);
        double length=Vec3.atBottomCenterOf(new BlockPos(4,0,0)).distanceTo(new Vec3(1.3,0,-.1));
        BedroomBarricade.advance(old,0);double expected=length*.5+.5;
        h.assertTrue(Math.abs(old.getDouble("Travel0")-expected)<.000001,"Legacy half-pushed chair lost/changed physical progress");
        var loaded=old.copy();BedroomBarricade.advance(loaded,0);h.assertTrue(Math.abs(loaded.getDouble("Travel0")-expected-.5)<.000001,"Reload reran the old percentage migration");h.succeed();
    }
    @GameTest(template="empty") public static void rescueHandOnlyExistsDuringActualPullAndGuideGripSurvives(GameTestHelper h){
        h.assertTrue(FirstEscapeVisualRules.rescueHand("ESCAPE_A",false,1),"Actual rescue arm hidden");
        for(int cue:new int[]{0,2})h.assertFalse(FirstEscapeVisualRules.rescueHand("ESCAPE_A",false,cue),"Old carrier rendered as floating hand");
        h.assertFalse(FirstEscapeVisualRules.rescueHand("ESCAPE_A",true,1),"Arm remains after landing");
        var p=MissingParentsTeaGameTests.fixture(h);var s=OtherMotherLoop.scene(p);var prop=CinematicPropEntity.TYPE.get().create(p.serverLevel());
        try{FirstEscapeStory.data(s).putBoolean("Started",true);FirstEscapeStory.data(s).putBoolean("Rescued",true);prop.configure(s,CinematicPropEntity.GRAB_CARRIER,0);prop.frame(1,0,1);
            h.assertTrue(FirstEscapeStory.retiredGrab(prop,s),"Saved rescue arm never retires");prop.frame(2,0,1);h.assertFalse(FirstEscapeStory.retiredGrab(prop,s),"Hand cleanup destroys later guide grip");
        }finally{prop.discard();MissingParentsTeaGameTests.remove(p,s);}h.succeed();
    }
    @GameTest(template="empty") public static void helpUsWritingIsCenteredOnTheMirror(GameTestHelper h){
        for(int i=0;i<MissingParentsCatTimeline.strokes();i++)for(int end=0;end<=1;end++){
            var point=MissingParentsCatTimeline.point(i,end);h.assertTrue(point[1]>=1.21&&point[1]<=1.59&&Math.abs(point[0])<1,"Writing is above the parents rather than in the middle");
        }h.succeed();
    }
    @GameTest(template="empty") public static void arrivingCatApproachesInsteadOfWaitingForThePlayerToFollow(GameTestHelper h){
        var p=MissingParentsTeaGameTests.fixture(h);var s=OtherMotherLoop.scene(p);var n=MissingParentsCat.data(s);var cat=CharacterCast.ENTITIES.get("cat").get().create(p.serverLevel());cat.setPos(p.position().add(0,0,11));
        try{n.putString("Phase","APPROACH");MissingParentsCat.guide(p,s,cat,p.blockPosition(),1.05);h.assertTrue(n.contains("MoveTarget")&&n.getInt("LostTicks")==0,"Cat's arrival applies the follow-player distance gate and waits for the stall watchdog");}
        finally{cat.discard();MissingParentsTeaGameTests.remove(p,s);}h.succeed();
    }
    @GameTest(template="empty") public static void stalledCatWalksAroundFurnitureInSmallCollisionCheckedSteps(GameTestHelper h){
        var p=MissingParentsTeaGameTests.fixture(h);var s=OtherMotherLoop.scene(p);var n=MissingParentsCat.data(s);var level=p.serverLevel();var base=h.absolutePos(new BlockPos(70,7,20));
        for(int x=-2;x<=8;x++)for(int z=-3;z<=3;z++){var at=base.offset(x,0,z);level.getChunkAt(at);level.setBlock(at.below(),Blocks.STONE.defaultBlockState(),2);level.setBlock(at,Blocks.AIR.defaultBlockState(),2);level.setBlock(at.above(),Blocks.AIR.defaultBlockState(),2);}
        level.setBlock(base.east(3),Blocks.OAK_LOG.defaultBlockState(),2);level.setBlock(base.east(3).above(),Blocks.OAK_LOG.defaultBlockState(),2);
        var cat=CharacterCast.ENTITIES.get("cat").get().create(level);cat.setPos(Vec3.atBottomCenterOf(base));p.setPos(Vec3.atBottomCenterOf(base.west(2)));var target=base.east(6);
        try{n.putString("Phase","FOLLOW_MIRROR");for(String key:List.of("MoveTarget","GuideRequested","GuideResolved"))n.putLong(key,target.asLong());n.putLong("GuideRetry",level.getGameTime()+1000);n.putDouble("MoveBest",36);n.putInt("StuckTicks",40);
            boolean arrived=false;for(int i=0;i<160&&!arrived;i++){var before=cat.position();arrived=MissingParentsCat.guide(p,s,cat,target,.95);h.assertTrue(cat.position().distanceTo(before)<=.121,"Cat recovery warped instead of walking");h.assertFalse(level.getBlockCollisions(cat,cat.getBoundingBox().deflate(.001)).iterator().hasNext(),"Cat recovery walked through solid furniture");}
            h.assertTrue(arrived&&n.getBoolean("FloorGuide"),"Collision-checked Cat recovery never reached its destination");
        }finally{cat.discard();MissingParentsTeaGameTests.remove(p,s);}h.succeed();
    }
    @GameTest(template="empty") public static void completedSceneRetiresOnlyItsOwnParentDollProp(GameTestHelper h){
        var p=MissingParentsTeaGameTests.fixture(h);var s=OtherMotherLoop.scene(p);var n=MissingParentsCat.data(s);var prop=CinematicPropEntity.TYPE.get().create(p.serverLevel());
        try{prop.configure(s,CinematicPropEntity.PARENT_DOLLS,0);n.putUUID("ParentDolls",prop.getUUID());n.putString("Phase","F");h.assertFalse(MissingParentsCat.retiredDolls(prop,s),"Dolls removed before final dialogue");
            n.putString("Phase","DEPART");h.assertTrue(MissingParentsCat.retiredDolls(prop,s),"Dolls stay after scene ends");
            var other=CinematicPropEntity.TYPE.get().create(p.serverLevel());other.configure(s,CinematicPropEntity.PARENT_DOLLS,0);h.assertFalse(MissingParentsCat.retiredDolls(other,s),"Unrelated doll removed");other.discard();
        }finally{prop.discard();MissingParentsTeaGameTests.remove(p,s);}h.succeed();
    }
    @GameTest(template="empty") public static void catArrivalAcceptsAdjacentGrassButNotAWall(GameTestHelper h){
        var level=h.getLevel();var base=h.absolutePos(new BlockPos(2,3,2));var cat=CharacterCast.ENTITIES.get("cat").get().create(level);cat.setPos(Vec3.atBottomCenterOf(base).add(.9,1,0));
        try{h.assertTrue(MissingParentsCat.arrived(cat,Vec3.atBottomCenterOf(base)),"Cat on adjacent grass never counts as arriving");
            level.setBlock(base.above(),Blocks.STONE.defaultBlockState(),2);h.assertFalse(MissingParentsCat.arrived(cat,Vec3.atBottomCenterOf(base)),"Cat arrived through a wall");
        }finally{level.removeBlock(base.above(),false);cat.discard();}h.succeed();
    }
    @GameTest(template="empty") public static void songIsAcknowledgedAndCatSpawnsFortyFiveSecondsAfterStart(GameTestHelper h){
        int duration=ShowRecordings.duration(MissingParentsCat.INTRO_MUSIC);h.assertTrue(duration>100,"Missing complete imported song timing");h.assertTrue(MissingParentsCat.spawnCue(duration)==900&&MissingParentsCat.spawnCue(200)==900,"Wrong song spawn cue");
        var p=MissingParentsTeaGameTests.fixture(h);var s=OtherMotherLoop.scene(p);
        try{h.assertFalse(MissingParentsCat.arrivalMusic(p,s),"Cat spawned before music played");var n=ShowPlayback.data(s);h.assertTrue(MissingParentsCat.available(p,s),"Cat's own intro song deadlocks its next story tick");h.assertFalse(ShowPlayback.started(s),"Music fabricated a start acknowledgement");
            ShowPlayback.signal(p,n.getUUID("Nonce"),MissingParentsCat.INTRO_MUSIC,0);PlaybackFixtureClock.elapseOriginal(s);
            h.assertTrue(MissingParentsCat.arrivalMusic(p,s)&&MissingParentsCat.data(s).getBoolean("ArrivalMusicComplete"),"Audible music never releases arrival");
        }finally{ShowPlayback.stop(p,s);MissingParentsTeaGameTests.remove(p,s);}h.succeed();
    }
    @GameTest(template="empty",batch="story-time-command") public static void settingDayCannotJumpOrBreakAnAcknowledgedStoryRecording(GameTestHelper h){
        var p=MissingParentsTeaGameTests.fixture(h);var s=OtherMotherLoop.scene(p);
        try{h.assertTrue(ShowPlayback.start(p,s,"missing_parents_cat_a",false),"Supplied recording timing missing");var n=ShowPlayback.data(s);ShowPlayback.signal(p,n.getUUID("Nonce"),"missing_parents_cat_a",0);long before=p.serverLevel().getGameTime();
            int result=p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4),"time set day");h.assertTrue(result>=0&&p.serverLevel().getDayTime()==1000,"Actual /time set day failed");h.assertTrue(p.serverLevel().getGameTime()==before,"Day command changed the story game clock");
            h.assertFalse(ShowPlayback.tick(p,s)||ShowPlayback.finished(s),"Day command skipped an audible recording");h.assertTrue(ShowPlayback.started(s)&&n.getString("Blocker").isEmpty(),"Day command broke playback state");
        }finally{ShowPlayback.stop(p,s);MissingParentsTeaGameTests.remove(p,s);}h.succeed();
    }
    @GameTest(template="empty") public static void tunnelCatMovesInCrawlSpaceAndDoesNotCollideWithPlayer(GameTestHelper h){
        var p=MissingParentsTeaGameTests.fixture(h);var s=OtherMotherLoop.scene(p);var level=p.serverLevel();var origin=h.absolutePos(new BlockPos(3,5,3));
        for(int z=0;z<=8;z++)for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)level.setBlock(origin.offset(x,y,z),x==0&&y==0?Blocks.AIR.defaultBlockState():Blocks.STONE.defaultBlockState(),2);
        var cat=(CharacterEntity)ShowActors.acquire(p,s,"cat",level,Vec3.atBottomCenterOf(origin.south()));
        try{FirstEscapeStory.data(s).putLong("EscapeTunnel",origin.asLong());FirstEscapeStory.data(s).putString("EscapeDirection","south");
            for(int i=0;i<18;i++)ParentRescueStory.guideTunnelCat(p,s,cat,3);
            h.assertTrue(cat.getZ()<origin.getZ()+2,"Cat must follow behind the player, not lead the tunnel");h.assertTrue(cat.getBbHeight()<=.7F,"Cat's tall collision box hits the tunnel ceiling");
            h.assertFalse(cat.canBeCollidedWith()||cat.canCollideWith(p)||cat.isPushable(),"Player cannot pass its story Cat");
            for(var facing:net.minecraft.core.Direction.Plane.HORIZONTAL){
                FirstEscapeStory.data(s).putString("EscapeDirection",facing.getName());var forward=Vec3.atLowerCornerOf(facing.getNormal());
                cat.setPos(Vec3.atBottomCenterOf(origin).add(forward.scale(8)));ParentRescueStory.guideTunnelCat(p,s,cat,3);
                h.assertTrue(cat.position().subtract(Vec3.atBottomCenterOf(origin)).dot(forward)<2,"Old saved Cat remains ahead of the player facing "+facing);
            }

            ShowActors.acquire(p,s,"cat",level,cat.position());h.assertTrue(cat.getBbHeight()>.7F,"Cat keeps crawl dimensions in later scenes");
        }finally{cat.discard();MissingParentsTeaGameTests.remove(p,s);}h.succeed();
    }
    @GameTest(template="empty") public static void kitchenArrivalDoesNotWaitForMotherAndClickDoesNotReplayProposal(GameTestHelper h){
        var p=MissingParentsTeaGameTests.fixture(h);
        var s=OtherMotherLoop.scene(p);var n=ParentRescueStory.data(s);
        var mother=ModEntities.MOM.get().create(p.serverLevel());
        mother.makeOtherParent(0);s.mom=mother.getUUID();
        // This is a saved/complete kitchen fixture; it needs a real nearby
        // appliance now that the story checks cached block identity.
        var stove=p.blockPosition().east(4);
        p.serverLevel().getChunkAt(stove);
        var original=p.serverLevel().getBlockState(stove);
        try{
            p.serverLevel().setBlock(stove,Blocks.SMOKER.defaultBlockState(),2);
            n.putBoolean("Started",true);n.putString("Phase","KITCHEN");
            n.putLong("Kitchen",p.blockPosition().asLong());
            n.putLong("KitchenTable",p.blockPosition().asLong());
            n.putLong("Stove",stove.asLong());
            mother.setPos(p.position().add(20,0,0));
            ParentRescueStory.tickKitchenArrival(p,s,mother);
            h.assertTrue(ParentRescueStory.phase(s)==ParentRescueStory.Phase.WAIT_KITCHEN_SEAT,
                    "Kitchen prompt waits for Mother at the stove");
            BlackIsTraditional.data(s).putBoolean("Started",true);BlackIsTraditional.data(s).putBoolean("Dark",true);
            h.assertTrue(VisitEvents.interact(p,mother),"Kitchen Mother falls through to old proposal interaction");
            h.assertTrue(ParentRescueStory.phase(s)==ParentRescueStory.Phase.WAIT_KITCHEN_SEAT,
                    "Click restarts kitchen dialogue");
        }finally{
            p.serverLevel().setBlock(stove,original,2);
            mother.discard();MissingParentsTeaGameTests.remove(p,s);
        }h.succeed();
    }
    @GameTest(template="empty",batch="actual-bike-controller",timeoutTicks=110) public static void mountedDepartureControllerMovesImmediatelyAndKeepsMoving(GameTestHelper h){
        var p=MissingParentsTeaGameTests.fixture(h);var s=OtherMotherLoop.scene(p);var level=p.serverLevel();var base=h.absolutePos(new BlockPos(8,5,8));var forced=new HashSet<net.minecraft.world.level.ChunkPos>();
        for(int x=-6;x<=28;x++)for(int z=-6;z<=6;z++){var at=base.offset(x,0,z);var chunk=new net.minecraft.world.level.ChunkPos(at);forced.add(chunk);level.setChunkForced(chunk.x,chunk.z,true);level.getChunkAt(at);level.setBlock(at.below(),Blocks.STONE.defaultBlockState(),2);for(int y=0;y<3;y++)level.setBlock(at.above(y),Blocks.AIR.defaultBlockState(),2);}
        p.setPos(Vec3.atBottomCenterOf(base.west(3)));var actor=ShowActors.acquire(p,s,"wybie",level,Vec3.atBottomCenterOf(base));var n=FirstEscapeStory.data(s);n.putBoolean("Started",true);n.putString("Phase","BICYCLE_DEPARTURE");
        int[] ticks={0};var start=actor.position();
        h.onEachTick(()->{if(++ticks[0]>=20)try{FirstEscapeStory.departure(p,s);}catch(Throwable failure){failure.printStackTrace();throw failure;}});
        h.runAtTickTime(24,()->{
            h.assertTrue(n.hasUUID("StoryBike"),"No bike after actual departure ticks="+ticks[0]+" state="+n+" actor="+ShowActors.actor(p,s,"wybie")+" original="+actor+" removed="+actor.getRemovalReason()+" lease="+s.loop.event.getCompound("ShowActors"));var actual=(WybieBicycleEntity)level.getEntity(n.getUUID("StoryBike"));
            h.assertTrue(actual!=null&&actor.getVehicle()==actual&&actual.position().distanceToSqr(start)>.2,"Mounted controller only posed the rider");
            FirstEscapeStory.departure(p,s);var before=actual.position();FirstEscapeStory.departure(p,s);h.assertTrue(actual.position().equals(before),"Story and vehicle ticks moved twice in one frame");
        });
        h.runAtTickTime(44,()->{
            var actual=(WybieBicycleEntity)level.getEntity(n.getUUID("StoryBike"));
            try{h.assertTrue(actual!=null&&actual.position().distanceToSqr(start)>64,"Actual story departure pedals in place");
                h.assertTrue(actor.getVehicle()==actual,"Bicycle lost original rider");
            }finally{MissingParentsTeaGameTests.remove(p,s);if(actual!=null)actual.discard();actor.discard();for(var chunk:forced)level.setChunkForced(chunk.x,chunk.z,false);}h.succeed();
        });
    }
    @GameTest(template="empty") public static void finishedCatMirrorRetiresWithoutRecreationOrRemovingOtherMirrors(GameTestHelper h){
        var p=MissingParentsTeaGameTests.fixture(h);var s=OtherMotherLoop.scene(p);var n=MissingParentsCat.data(s);
        var mirror=CinematicPropEntity.TYPE.get().create(p.serverLevel());var other=CinematicPropEntity.TYPE.get().create(p.serverLevel());
        try{p.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,2,3))));mirror.configure(s,CinematicPropEntity.PARENT_MIRROR,0);other.configure(s,CinematicPropEntity.PARENT_MIRROR,0);mirror.setPos(p.position());other.setPos(p.position().add(4,0,0));p.serverLevel().addFreshEntity(mirror);p.serverLevel().addFreshEntity(other);
            n.putUUID("MirrorActor",mirror.getUUID());n.putLong("MirrorActorAt",mirror.blockPosition().asLong());n.putLong("Mirror",mirror.blockPosition().asLong());n.putBoolean("Complete",true);n.putString("Phase","COMPLETE");
            mirror.tick();h.assertTrue(mirror.isRemoved(),"Completed saved Cat mirror survives entity loading");
            other.tick();h.assertFalse(other.isRemoved(),"Cleanup removed an unrelated mirror");MissingParentsCat.props(p,s);h.assertTrue(p.serverLevel().getEntity(n.getUUID("MirrorActor"))==null||p.serverLevel().getEntity(n.getUUID("MirrorActor")).isRemoved(),"Props recreated the retired mirror");
        }finally{mirror.discard();other.discard();MissingParentsTeaGameTests.remove(p,s);}h.succeed();
    }
    @GameTest(template="empty") public static void kitchenChairExitStaysOnKitchenFloorAboveBasement(GameTestHelper h){
        var p=MissingParentsTeaGameTests.fixture(h);var s=OtherMotherLoop.scene(p);var floor=h.absolutePos(new BlockPos(3,5,3));
        try{for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){var at=floor.offset(x,0,z);p.serverLevel().setBlock(at.below(),Blocks.STONE.defaultBlockState(),2);p.serverLevel().setBlock(at,Blocks.AIR.defaultBlockState(),2);p.serverLevel().setBlock(at.above(),Blocks.AIR.defaultBlockState(),2);}
            p.setPos(Vec3.atBottomCenterOf(floor).add(0,.3,0));ParentRescueStory.data(s).putLong("Kitchen",floor.asLong());var exit=ParentRescueStory.kitchenExit(p,s);
            h.assertTrue(exit!=null&&Math.abs(exit.y-floor.getY())<.13,"Kitchen dismount selected the basement or furniture instead of its floor");
        }finally{MissingParentsTeaGameTests.remove(p,s);}h.succeed();
    }
    @GameTest(template="empty") public static void layeredEscapeTunnelRetirementKeepsOriginalAndForeignBlocks(GameTestHelper h){
        var p=MissingParentsTeaGameTests.fixture(h);var s=OtherMotherLoop.scene(p);
        var level=p.server.getLevel(OtherWorld.KEY);
        h.assertTrue(level!=null,"The authentic Other World is not available");
        int y=Math.max(level.getMinBuildHeight()+10,Math.min(120,level.getMaxBuildHeight()-10));
        var origin=new BlockPos(492,y,491);var foreign=origin.south();
        level.getChunkAt(origin);level.getChunkAt(foreign);
        var originalA=level.getBlockState(origin);var originalB=level.getBlockState(foreign);
        var escape=FirstEscapeStory.data(s);
        var first=new CompoundTag();var rescue=new CompoundTag();
        var baseRow=new CompoundTag();
        baseRow.put("Original",net.minecraft.nbt.NbtUtils.writeBlockState(originalA));
        baseRow.put("Placed",net.minecraft.nbt.NbtUtils.writeBlockState(Blocks.STONE.defaultBlockState()));
        first.put(Long.toString(origin.asLong()),baseRow);
        var overlayRow=new CompoundTag();
        overlayRow.put("Original",net.minecraft.nbt.NbtUtils.writeBlockState(Blocks.STONE.defaultBlockState()));
        overlayRow.put("Placed",net.minecraft.nbt.NbtUtils.writeBlockState(Blocks.MOSS_BLOCK.defaultBlockState()));
        rescue.put(Long.toString(origin.asLong()),overlayRow);
        var foreignRow=new CompoundTag();
        foreignRow.put("Original",net.minecraft.nbt.NbtUtils.writeBlockState(originalB));
        foreignRow.put("Placed",net.minecraft.nbt.NbtUtils.writeBlockState(Blocks.STONE.defaultBlockState()));
        first.put(Long.toString(foreign.asLong()),foreignRow);
        escape.put("Environment",first);escape.putLong("EscapeTunnel",origin.asLong());
        escape.putString("EscapeDirection","south");escape.putBoolean("EscapeTunnelBuilt",true);
        ParentRescueStory.scenery(s).put("Environment",rescue);
        try{
            level.setBlock(origin,Blocks.MOSS_BLOCK.defaultBlockState(),2);
            level.setBlock(foreign,Blocks.GOLD_BLOCK.defaultBlockState(),2);
            StoryTunnel.retireCrawl(p,s);
            h.assertTrue(level.getBlockState(origin).equals(originalA),"Overlaid tunnel reverted to intermediate stone instead of original terrain");
            h.assertFalse(first.contains(Long.toString(origin.asLong()))||rescue.contains(Long.toString(origin.asLong())),"Layer restoration did not retire both authentic receipts");
            h.assertTrue(level.getBlockState(foreign).is(Blocks.GOLD_BLOCK),"Foreign edit was overwritten by tunnel cleanup");
            h.assertTrue(first.contains(Long.toString(foreign.asLong()))&&escape.getBoolean("EscapeTunnelCleanupPending"),"Unresolved edited cell lost its restoration record");
            h.assertFalse(escape.getBoolean("EscapeTunnelBuilt"),"Retirement left the old escape passage marked usable");
            // Once the foreign editor restores the recorded owned state, cleanup
            // must replay without reconstructing either discarded journal layer.
            level.setBlock(foreign,Blocks.STONE.defaultBlockState(),2);
            StoryTunnel.retireCrawl(p,s);
            h.assertTrue(level.getBlockState(foreign).equals(originalB),"Retry failed to restore the original block");
            h.assertFalse(first.contains(Long.toString(foreign.asLong()))||escape.getBoolean("EscapeTunnelCleanupPending"),"Successful retry left a permanent phantom pending receipt");
        }finally{
            level.setBlock(origin,originalA,2);level.setBlock(foreign,originalB,2);
            MissingParentsTeaGameTests.remove(p,s);
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void sewingPropsNeverShareTransportOrControlPointKinds(GameTestHelper h){
        var kinds=java.util.Set.of(AcceptedButtonEyesWorld.BUTTONS,AcceptedButtonEyesWorld.NEEDLE,AcceptedButtonEyesWorld.SEWING_TABLE,PalaceTransport.MOVER,PalaceTransport.TRUCK,PalaceTransport.CARRIER,CinematicPropEntity.WORLD_SIGN,RescueProps.CONTROL_POINT);
        h.assertTrue(kinds.size()==8,"Story props alias the truck, mover or control-point renderer");h.succeed();
    }
}
