package com.coraline.feast;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class FirstEscapeGuideGameTests {
    @GameTest(template="empty",timeoutTicks=80) public static void storyLookupCannotReenableAiDuringHandGrip(GameTestHelper h){
        var p=player(h);var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.loop.event.putInt("Serial",19);VisitData.get(p.server).scenes.put(s.owner,s);
        var n=FirstEscapeStory.data(s);n.putString("Phase","FOLLOW_WYBIE");
        var actor=ShowActors.acquire(p,s,"other_wybie",p.serverLevel(),h.absolutePos(new BlockPos(2,4,2)).getCenter());
        h.assertTrue(actor!=null,"Actual leased guide creation failed");
        h.runAfterDelay(20,()->{try{
            actor.setNoAi(true);actor.setNoGravity(true);
            var lookup=FirstEscapeStory.class.getDeclaredMethod("wybie",ServerPlayer.class,VisitData.Scene.class);lookup.setAccessible(true);
            h.assertTrue(lookup.invoke(null,p,s)==actor&&actor.isNoAi()&&actor.isNoGravity(),"Story lookup reenabled normal AI/gravity during scripted grip");
            n.putString("Phase","MASK_REMOVAL");lookup.invoke(null,p,s);
            h.assertFalse(actor.isNoAi()||actor.isNoGravity(),"Ordinary mask interaction retained scripted guide controls");
        }catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}finally{actor.discard();VisitData.get(p.server).scenes.remove(s.owner);}h.succeed();});
    }
    private static ServerPlayer player(GameTestHelper h){var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"GuideQA"));p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);return p;}
    @GameTest(template="empty",timeoutTicks=400,batch="guide-running-motion") public static void runningGripCrossesDoorwayAndReachesWardrobe(GameTestHelper h){
        var p=player(h);var l=p.serverLevel();var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.loop.event.putInt("Serial",9);VisitData.get(p.server).scenes.put(s.owner,s);
        var n=FirstEscapeStory.data(s);n.putLong("House",h.absolutePos(new BlockPos(0,20,0)).asLong());n.putLong("LittleDoor",FirstEscapeScene.at(s,13,6,44).asLong());n.putBoolean("Started",true);n.putString("Phase","FOLLOW_WYBIE");n.putInt("GuideRevision",2);n.putUUID("Token",UUID.randomUUID());
        // This hallway is20blocks above the template origin, outside the
        // template's cleared height. Seal its real boundary before clearing
        // the floor/carpet/doorway: natural cave water otherwise enters the
        // next running footprint and correctly fails the liquid safety check.
        for(int x=12;x<=36;x++)for(int z=36;z<=52;z++)for(int y=6;y<=10;y++)
            if(x==12||x==36||z==36||z==52||y==10){var at=FirstEscapeScene.at(s,x,y,z);l.getChunkAt(at);l.setBlock(at,Blocks.STONE.defaultBlockState(),2);}
        for(int x=13;x<=35;x++)for(int z=37;z<=51;z++){
            var at=FirstEscapeScene.at(s,x,6,z);l.getChunkAt(at);l.setBlock(at.below(),Blocks.STONE.defaultBlockState(),2);for(int y=0;y<4;y++)l.setBlock(at.above(y),x==31&&z!=41?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
            if((x!=31||z==41)&&Math.floorMod(x+z,3)!=0)l.setBlock(at,Blocks.BROWN_CARPET.defaultBlockState(),2);
        }
        var w=com.coraline.registry.CharacterCast.ENTITIES.get("other_wybie").get().create(l);w.setPos(FirstEscapeCarry.feet(p,FirstEscapeScene.at(s,33,6,48)));p.setPos(w.position().add(0,0,-.85));
        var grip=FirstEscapeScene.ensure(p,s,"GuideGrip",CinematicPropEntity.GRAB_CARRIER,0,l,p.position());h.assertTrue(grip!=null,"Real grip creation failed");grip.frame(2,0,1);p.startRiding(grip,true);
        var start=w.position();int[] elapsed={0};
        h.onEachTick(()->{try{
            FirstEscapeGuide.tick(p,s,w);if(p.getVehicle()!=null)grip.positionRider(p);
            h.assertTrue(!l.getBlockCollisions(null,w.getBoundingBox()).iterator().hasNext(),"Runner clipped the real doorway");
            h.assertTrue(!l.getBlockCollisions(null,p.getBoundingBox()).iterator().hasNext(),"Held player clipped floor/door frame");
            if(FirstEscapeGuide.arrived(p,s,w)){h.assertTrue(elapsed[0]<220&&w.position().distanceToSqr(start)>64,"Guide did not run the complete corridor");FirstEscapeGuide.release(p,s,w);h.assertFalse(p.isPassenger(),"Wardrobe arrival failed to release grip");VisitData.get(p.server).scenes.remove(s.owner);h.succeed();}
            else if(++elapsed[0]>=250){FirstEscapeGuide.release(p,s,w);VisitData.get(p.server).scenes.remove(s.owner);h.fail("Running grip stalled: "+w.position()+" / "+n.getString("Blocker"));}
        }catch(Throwable failure){FirstEscapeGuide.release(p,s,w);VisitData.get(p.server).scenes.remove(s.owner);throw failure;}});
    }
    @GameTest(template="empty") public static void blockedRouteReleasesPlayerInsteadOfPermanentGrab(GameTestHelper h){
        var p=player(h);var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.loop.event.putInt("Serial",10);VisitData.get(p.server).scenes.put(s.owner,s);var n=FirstEscapeStory.data(s);n.putLong("House",h.absolutePos(new BlockPos(0,45,0)).asLong());n.putLong("LittleDoor",FirstEscapeScene.at(s,13,6,44).asLong());n.putInt("GuideRevision",2);
        var w=com.coraline.registry.CharacterCast.ENTITIES.get("other_wybie").get().create(p.serverLevel());w.setPos(FirstEscapeScene.at(s,33,6,48).getCenter());p.setPos(w.position());
        var grip=FirstEscapeScene.ensure(p,s,"GuideGrip",CinematicPropEntity.GRAB_CARRIER,0,p.serverLevel(),p.position());grip.frame(2,0,1);p.startRiding(grip,true);
        try{for(int i=0;i<40;i++)FirstEscapeGuide.tick(p,s,w);h.assertFalse(p.isPassenger(),"Impossible route leaves player mounted forever");h.assertTrue(n.contains("GuideRetryAt")&&!n.getString("Blocker").isEmpty(),"Blocked route has no retry state");}finally{FirstEscapeGuide.release(p,s,w);VisitData.get(p.server).scenes.remove(s.owner);}h.succeed();
    }
    @GameTest(template="empty") public static void darkHouseLightIsLiftedButPrisonRuleIsIndependent(GameTestHelper h){h.assertTrue(FirstEscapeVisualRules.darkHouseLight(0)>.06&&FirstEscapeVisualRules.darkHouseLight(15)<.9,"House was not slightly lifted or lost night contrast");h.assertTrue(FirstEscapeVisualRules.prisonLight(0)==.075F,"House fix accidentally changes prison lighting");h.succeed();}
    @GameTest(template="empty") public static void bossSkipCanBeArmedBeforeRescueBegins(GameTestHelper h){var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"OperatorGuideQA")){@Override public boolean hasPermissions(int level){return level<=2;}};p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();try{var result=BeldamFinale.debugSkipFight(p,s);h.assertTrue(result.startsWith("Boss skip armed")&&s.loop.story.getBoolean("DebugSkipFinalFightPending"),"Early boss shortcut still rejects missing chapter");h.assertFalse(RescueChapter.started(s),"Arming shortcut fabricated chapter/eyes");}finally{}h.succeed();}
}
