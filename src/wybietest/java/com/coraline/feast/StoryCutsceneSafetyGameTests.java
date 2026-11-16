package com.coraline.feast;

import java.util.*;
import com.coraline.otherworld.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

/** Prepared regression fixtures. Source-only work does not claim these ran. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class StoryCutsceneSafetyGameTests {
    @GameTest(template="empty") public static void timeoutsAreOneShotAndNewStepsHaveTheirOwnBudget(GameTestHelper t){
        var state=new CompoundTag();for(int i=0;i<29;i++)t.assertFalse(StoryCutsceneWatchdog.elapsed(state,"chair",20,600),"Motion skipped before budget");
        t.assertTrue(StoryCutsceneWatchdog.elapsed(state,"chair",20,600),"Motion never timed out");t.assertFalse(StoryCutsceneWatchdog.elapsed(state,"chair",20,600),"Timeout fired twice");
        t.assertFalse(StoryCutsceneWatchdog.elapsed(state,"next_line",20,600),"Next checkpoint inherited expired timer");
        var saved=state.copy();t.assertFalse(StoryCutsceneWatchdog.elapsed(saved,"next_line",0,600),"Reload itself grants elapsed time");t.assertTrue(saved.getInt("Ticks")==20,"Offline time advanced the scene");t.succeed();
    }
    @GameTest(template="empty") public static void furnitureLayoutIsSeededSeparatedAndVariesAcrossRuns(GameTestHelper t){
        var floor=new ArrayList<Vec3>();for(int x=0;x<10;x++)for(int z=0;z<14;z++)floor.add(new Vec3(x+.5,0,z+.5));var patterns=new HashSet<Object>();
        for(long seed=0;seed<24;seed++){var chosen=BedroomBarricade.spread(floor,seed);t.assertTrue(chosen.size()==3,"Missing barricade piece");t.assertTrue(chosen.equals(BedroomBarricade.spread(floor,seed)),"Layout rerolls on reload");for(int a=0;a<3;a++)for(int b=a+1;b<3;b++)t.assertTrue(chosen.get(a).distanceToSqr(chosen.get(b))>=9,"Chairs are adjacent instead of spread around the room");patterns.add(chosen);}
        t.assertTrue(patterns.size()>8,"Every seed produces one fixed layout");t.succeed();
    }
    @GameTest(template="empty") public static void parlorColorsAndPrisonLightAreIndependent(GameTestHelper t){
        for(String phase:List.of("ROOM_DARK","ROOM_TRANSFORMING","CHAIR_CAPTURE","COCOA_A","COCOA_G"))t.assertTrue(FirstEscapeVisualRules.brightParlor(phase),"Cocoa scene inherits dark hallway: "+phase);
        for(String phase:List.of("FIND_CANE","PRY_DOOR","MIRROR_A","ESCAPE_A","COMPLETE",""))t.assertFalse(FirstEscapeVisualRules.brightParlor(phase),"Parlor lighting leaked into "+phase);
        t.assertTrue(FirstEscapeVisualRules.parlorLight(0)>.6F&&FirstEscapeVisualRules.parlorLight(15)==1,"Parlor remains too dark");t.assertTrue(FirstEscapeVisualRules.prisonLight(0)==.075F,"Parlor fix changes prison lighting");t.succeed();
    }
    @GameTest(template="empty") public static void parlorLightingFadesWithLampsAndStaysWithTheDrag(GameTestHelper t){
        t.assertTrue(FirstEscapeVisualRules.parlorBlend("ROOM_DARK",999)==0,"Opening the door snaps the room bright");
        float previous=0;for(int tick=0;tick<=180;tick++){float value=FirstEscapeVisualRules.parlorBlend("ROOM_TRANSFORMING",tick);t.assertTrue(value>=previous&&value-previous<.02F,"Lamp reveal snaps or reverses");previous=value;}
        t.assertTrue(previous==1&&FirstEscapeVisualRules.parlorBlend("COCOA_A",0)==1,"Completed reveal dims again");
        for(String phase:List.of("MIRROR_DRAG","MIRROR_THROW"))t.assertTrue(FirstEscapeVisualRules.carryParlorLight(phase,false)&&FirstEscapeVisualRules.parlorBlend(phase,0)==1,"Hallway darkens during the drag");
        t.assertTrue(FirstEscapeVisualRules.carryParlorLight("COCOA_G",true),"G's carried voice tail darkens in the hall");
        t.assertFalse(FirstEscapeVisualRules.carryParlorLight("COCOA_G",false)||FirstEscapeVisualRules.carryParlorLight("MIRROR_A",true),"Carry brightness leaks outside its presentation");t.succeed();
    }
    @GameTest(template="empty") public static void optionalPresentationTimeoutsNeverCompletePlayerObjectives(GameTestHelper t){
        for(var phase:List.of(OtherMotherLoopState.Phase.EVENT_INTRO,OtherMotherLoopState.Phase.DINNER_GREETING,OtherMotherLoopState.Phase.WYBIE_ENDING,OtherMotherLoopState.Phase.GAME_NIGHT_INSTRUCTIONS))t.assertTrue(VisitEvents.automaticBeat(phase),"Automatic presentation has no bounded fallback: "+phase);
        for(var phase:List.of(OtherMotherLoopState.Phase.FREE_TIME,OtherMotherLoopState.Phase.OUTING_WAIT,OtherMotherLoopState.Phase.OUTING_ACTIVE,OtherMotherLoopState.Phase.GAME_NIGHT_CHOICE,OtherMotherLoopState.Phase.DELIVERY_ACTIVE,OtherMotherLoopState.Phase.WYBIE_ACTIVE,OtherMotherLoopState.Phase.DINNER_WAIT))t.assertFalse(VisitEvents.automaticBeat(phase),"Timeout grants a deliberate player objective: "+phase);
        t.succeed();
    }
    @GameTest(template="empty") public static void missingOptionalActorsReleasePresentationWithoutReward(GameTestHelper t){
        var l=t.getLevel().getServer().getLevel(OtherWorld.KEY);var p=new ServerPlayer(l.getServer(),l,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"OptionalSafetyQA"));p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.table=s.piano=new BlockPos(178000,180,178000);s.loop.phase=OtherMotherLoopState.Phase.DINNER_GREETING;s.loop.trust=.2F;s.loop.suspicion=.3F;s.loop.event.putString("Type","FAMILY_DINNER");s.loop.event.putInt("Serial",1);VisitData.get(p.server).scenes.put(s.owner,s);
        try{for(int i=0;i<120;i++)VisitEvents.cutsceneSafety(p,s);t.assertTrue(s.loop.phase==OtherMotherLoopState.Phase.EXPLORING,"Missing parents permanently lock the optional greeting");t.assertTrue(s.loop.trust==.2F&&s.loop.suspicion==.3F&&!s.loop.event.getBoolean("CompletionAwarded"),"Fallback invented a success/reward");t.assertTrue(s.loop.story.getCompound("CutsceneSafety").getList("History",10).size()==1,"Fallback did not persist its one-shot recovery");}
        finally{VisitData.get(p.server).scenes.remove(s.owner);}t.succeed();
    }
    @GameTest(template="empty",batch="chair-floor-routing") public static void chairRoutesAroundWallAndCaptureHasSafeRecovery(GameTestHelper t){
        var l=t.getLevel().getServer().getLevel(OtherWorld.KEY);var p=new ServerPlayer(l.getServer(),l,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ChairSafetyQA"));
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.loop.phase=OtherMotherLoopState.Phase.BLACK_IS_TRADITIONAL;s.loop.event.putInt("Serial",1);VisitData.get(p.server).scenes.put(s.owner,s);
        var n=FirstEscapeStory.data(s);n.putBoolean("Started",true);n.putLong("House",new BlockPos(131500,180,131500).asLong());n.putString("Phase","CHAIR_CAPTURE");n.putUUID("Choice",UUID.randomUUID());var c=FirstEscapeScene.room(s);n.putLong("LittleDoor",c.west(7).asLong());n.putLong("Prison",c.above(40).asLong());
        CinematicPropEntity chair=null;
        try{
            for(int x=-6;x<=6;x++)for(int z=-6;z<=5;z++){var at=c.offset(x,0,z);l.getChunkAt(at);l.setBlock(at.below(),Blocks.STONE.defaultBlockState(),2);for(int y=0;y<4;y++)l.setBlock(at.above(y),Blocks.AIR.defaultBlockState(),2);}
            for(int z=-2;z<=2;z++)for(int y=0;y<3;y++)l.setBlock(c.offset(0,y,z),Blocks.STONE.defaultBlockState(),2);
            p.setPos(Vec3.atBottomCenterOf(c.offset(3,0,0)));chair=FirstEscapeScene.ensure(p,s,"BugChair",CinematicPropEntity.BUG_CHAIR,0,l,Vec3.atBottomCenterOf(c.offset(-3,0,0)));
            var route=CinematicFloorRoute.plan(p,s,chair,c.offset(3,0,0));t.assertFalse(route.isEmpty(),"Chair cannot find the clear route around the wall");
            for(var at:route)t.assertFalse(at.getX()==c.getX()&&Math.abs(at.getZ()-c.getZ())<=2,"Chair route crosses the wall");
            chair.setPos(Vec3.atBottomCenterOf(c));t.assertTrue(FirstEscapeStory.recoverChairCapture(p,s),"Blocked chair has no safe bounded fallback");
            t.assertTrue(p.getVehicle()==chair&&n.getBoolean("Captured")&&FirstEscapeStory.phase(s)==FirstEscapeTimeline.Phase.COCOA_B,"Capture progressed without a real successful mount");
        }finally{n.putBoolean("InternalTransfer",true);p.stopRiding();if(chair!=null)chair.discard();VisitData.get(p.server).scenes.remove(s.owner);}t.succeed();
    }
    private StoryCutsceneSafetyGameTests(){}
}
