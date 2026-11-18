package com.coraline.feast;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

/** Native fixtures COMPILED ONLY during low-RAM work, not a live well playthrough. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class PalaceWellGameTests {
    @GameTest(template="empty") public static void rescueApproachAcceptsDownhillWithoutCuttingThroughTheUpperTile(GameTestHelper h){
        var actor=com.coraline.registry.CharacterCast.ENTITIES.get("wybie").get().create(h.getLevel());h.assertTrue(actor!=null,"Character unavailable");
        h.setBlock(new BlockPos(2,1,2),Blocks.STONE);h.setBlock(new BlockPos(3,0,2),Blocks.STONE);
        var upper=h.absoluteVec(new net.minecraft.world.phys.Vec3(2.5,2,2.5));var lower=upper.add(1,-1,0);actor.setPos(upper);
        h.assertTrue(PalaceWellMotion.walkStep(h.getLevel(),actor,upper,lower),"Ordinary one-block downhill rescue route rejected");
        h.assertTrue(PalaceWellMotion.walkStep(h.getLevel(),actor,lower,upper),"Reverse one-block uphill route rejected");
        h.setBlock(new BlockPos(3,2,2),Blocks.STONE);
        h.assertFalse(PalaceWellMotion.walkStep(h.getLevel(),actor,upper,lower),"Downhill route crossed a solid wall");
        h.assertTrue(actor.position().equals(upper),"Route survey moved the original actor");h.succeed();
    }
    @GameTest(template="empty") public static void rescueApproachRejectsBlockedHeadroomAndNonWalkingJumps(GameTestHelper h){
        var actor=com.coraline.registry.CharacterCast.ENTITIES.get("wybie").get().create(h.getLevel());h.assertTrue(actor!=null,"Character unavailable");
        var upper=h.absoluteVec(new net.minecraft.world.phys.Vec3(2.5,2,2.5));actor.setPos(upper);
        h.setBlock(new BlockPos(2,1,2),Blocks.STONE);h.setBlock(new BlockPos(3,0,2),Blocks.STONE);h.setBlock(new BlockPos(3,3,2),Blocks.STONE);
        h.assertFalse(PalaceWellMotion.walkStep(h.getLevel(),actor,upper,upper.add(1,-1,0)),"Downhill route passed through low headroom");
        h.assertFalse(PalaceWellMotion.walkStep(h.getLevel(),actor,upper,upper.add(2,-1,0)),"Two-block jump accepted as one walkable edge");
        h.assertFalse(PalaceWellMotion.walkStep(h.getLevel(),actor,upper,upper.add(1,0,1)),"Diagonal corner cut accepted");h.succeed();
    }
    @GameTest(template="empty") public static void handGroundingUsesActualBottomOfCollider(GameTestHelper h){
        h.setBlock(new BlockPos(2,1,2),Blocks.STONE);var prop=CinematicPropEntity.TYPE.get().create(h.getLevel());h.assertTrue(prop!=null,"Prop unavailable");
        var s=new VisitData.Scene();s.owner=UUID.randomUUID();s.door=UUID.randomUUID();prop.configure(s,RescueProps.HAND,0);
        var feet=h.absoluteVec(new net.minecraft.world.phys.Vec3(2.5,2,2.5));prop.setPos(feet.add(0,2,0));
        var grounded=PalaceWellMotion.ground(h.getLevel(),prop,feet,4);
        h.assertTrue(grounded!=null&&grounded.y>feet.y+.14&&grounded.y<feet.y+.20,"Hand collider would be buried in floor");
        h.assertTrue(PalaceWellMotion.clear(h.getLevel(),prop,grounded),"Grounding result intersects terrain");h.succeed();
    }
    @GameTest(template="empty") public static void controlledMotionCannotCrossSolidWall(GameTestHelper h){
        var prop=CinematicPropEntity.TYPE.get().create(h.getLevel());h.assertTrue(prop!=null,"Prop unavailable");var s=new VisitData.Scene();s.owner=UUID.randomUUID();s.door=UUID.randomUUID();prop.configure(s,RescueProps.HAND,0);
        var at=h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5,2,2.5));prop.setPos(at);h.setBlock(new BlockPos(3,2,2),Blocks.STONE);h.setBlock(new BlockPos(3,3,2),Blocks.STONE);
        h.assertFalse(PalaceWellMotion.move(h.getLevel(),prop,at.add(4,0,0),4),"Swept movement passed through wall");
        h.assertTrue(prop.position().equals(at),"Blocked movement partially teleported actor");h.succeed();
    }
    @GameTest(template="empty") public static void foreignFutureAndMalformedWellAdapterRemainUntouched(GameTestHelper h){
        var s=new VisitData.Scene();s.owner=UUID.randomUUID();s.door=UUID.randomUUID();var root=new CompoundTag();root.putInt("Version",1);root.putUUID("Owner",s.owner);root.putUUID("Door",s.door);s.loop.story.put("PinkPalaceJourney",root);
        var n=new CompoundTag();n.putInt("Version",1);n.putUUID("Owner",s.owner);n.putUUID("Door",s.door);n.putInt("Ticks",0);n.putLong("LastTick",-1);root.put("WellStage",n);
        h.assertTrue(PalaceWellEncounter.control(s)==n,"Valid well adapter rejected");
        for(int kind=0;kind<6;kind++){
            var bad=n.copy();bad.putString("Evidence","keep");switch(kind){case 0->bad.putInt("Version",2);case 1->bad.putUUID("Owner",UUID.randomUUID());case 2->bad.putString("Ticks","wrong type");case 3->bad.putString("Phase","FUTURE_SCENE");case 4->bad.putByte("Paused",(byte)2);case 5->bad.putString("Combat","wrong type");}
            root.put("WellStage",bad);var before=s.loop.story.copy();h.assertTrue(PalaceWellEncounter.control(s)==null&&s.loop.story.equals(before),"Malformed well adapter overwritten");
        }h.succeed();
    }
    @GameTest(template="empty") public static void mountedBicycleRefusesLowCeilingThenRidesWhenCleared(GameTestHelper h){
        var bike=com.coraline.registry.ModEntities.WYBIE_BICYCLE.get().create(h.getLevel());
        h.assertTrue(bike!=null,"Bicycle unavailable");
        var start=h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5,2,2.5));
        var goal=start.add(2,0,0);
        for(int x=0;x<=4;x++)for(int z=1;z<=3;z++)h.setBlock(new BlockPos(x,1,z),Blocks.STONE);
        bike.setPos(start);
        var low=new BlockPos(3,4,2);
        h.setBlock(low,Blocks.STONE);
        h.assertTrue(StoryBikeRoute.toward(bike,goal)==null,"Route fit a pedestrian but lacked rider clearance");
        h.setBlock(low,Blocks.AIR);
        var route=StoryBikeRoute.toward(bike,goal);
        h.assertTrue(route!=null&&!route.isEmpty(),"Clear ground did not produce a bicycle route");
        for(var point:route)for(int tick=0;tick<4&&bike.position().distanceToSqr(point)>.02;tick++)
            h.assertTrue(bike.storyAdvance(point),"Clear route stalled during real movement");
        h.assertTrue(bike.position().distanceToSqr(goal)<.02,"Bicycle did not actually reach the destination");
        h.succeed();
    }
}
