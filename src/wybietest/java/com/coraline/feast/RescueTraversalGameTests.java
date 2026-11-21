package com.coraline.feast;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

/** Collision-driven traversals, not a substitute for first-person playthrough acceptance. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class RescueTraversalGameTests {
    private static ServerPlayer player(GameTestHelper h){var l=h.getLevel().getServer().getLevel(ActivityWorld.KEY);var p=new ServerPlayer(l.getServer(),l,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TraversalQA"));p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.noPhysics=false;p.getAbilities().flying=false;p.setNoGravity(false);var s=new VisitData.Scene();s.owner=p.getUUID();s.door=java.util.UUID.randomUUID();s.stage=VisitData.Stage.DONE;VisitData.get(p.server).scenes.put(s.owner,s);ParentRescueStory.data(s).putBoolean("Started",true);RescueChapter.data(s).putBoolean("Started",true);return p;}
    @GameTest(template="empty",timeoutTicks=1500) public static void physicalCrawlTraversesAllFiftyTwoMetersWithoutHeadClipping(GameTestHelper h){
        var p=player(h);var s=OtherMotherLoop.scene(p);boolean entered=false;for(int i=0;i<96&&!entered;i++)entered=RescuePassage.enter(p,s);h.assertTrue(entered,"Actual fabric passage did not finish physical build and fresh re-entry verification: "+RescueChapter.data(s).getString("Blocker"));int[] ticks={0};var l=p.serverLevel();
        h.onEachTick(()->{LoopProps.hold(l,p.blockPosition());p.move(MoverType.SELF,new Vec3(.065,-.08,0));h.assertFalse(l.getBlockCollisions(p,p.getBoundingBox().deflate(.001)).iterator().hasNext(),"Crawling body/head entered the actual passage shell: "+p.position()+" / "+p.getBoundingBox()+" / "+p.getPose()+" noclip="+p.noPhysics);h.assertTrue(p.getForcedPose()==Pose.SWIMMING,"Passage lost its low pose");if(RescuePassage.progress(p,s)>=RescuePassage.LENGTH){RescuePassage.restore(p,s);VisitData.get(p.server).scenes.remove(s.owner);h.succeed();}else if(++ticks[0]>1300){var at=p.position();RescuePassage.restore(p,s);VisitData.get(p.server).scenes.remove(s.owner);h.fail("Actual crawl stalled at "+at);}});
    }
    @GameTest(template="empty",timeoutTicks=20000) public static void allThreeSpiralLanesSupportContinuousCrouchedWalking(GameTestHelper h){var p=player(h);var s=OtherMotherLoop.scene(p);for(int i=0;i<65&&!RescueWebSite.prepare(p,s);i++);h.assertTrue(RescueChapter.data(s).getBoolean("WebReady"),"Actual strand build remained incomplete");p.setForcedPose(Pose.CROUCHING);p.setPose(Pose.CROUCHING);p.setShiftKeyDown(true);p.setMaxUpStep(.6F);var l=p.serverLevel();int[] lane={0},sample={1},ticks={0};var start=RescueWebSite.at(p,s,0,0);p.setPos(start.x,top(l,start),start.z);p.setOnGround(true);
        h.onEachTick(()->{LoopProps.hold(l,p.blockPosition());var target=RescueWebSite.at(p,s,lane[0],sample[0]/(double)RescueWebLayout.SAMPLES);var delta=target.subtract(p.position()).multiply(1,0,1);if(delta.horizontalDistanceSqr()<.018){if(++sample[0]>RescueWebLayout.SAMPLES){if(++lane[0]==3){RescueWebSite.restore(p,s);VisitData.get(p.server).scenes.remove(s.owner);h.succeed();return;}sample[0]=1;var next=RescueWebSite.at(p,s,lane[0],0);p.setPos(next.x,top(l,next),next.z);p.setOnGround(true);return;}}
            var step=delta.normalize().scale(Math.min(.065,delta.length()));p.move(MoverType.SELF,step.add(0,-.08,0));p.tickCount++;RescueWebSite.sag(p,s);var support=RescueWebSite.at(p,s,lane[0],Math.max(0,sample[0]-1)/(double)RescueWebLayout.SAMPLES);
            if(p.getY()<support.y-1||l.getBlockCollisions(p,p.getBoundingBox().deflate(.001)).iterator().hasNext()||++ticks[0]>19000){String detail="lane="+lane[0]+" sample="+sample[0]+" feet="+p.position()+" target="+target;RescueWebSite.restore(p,s);VisitData.get(p.server).scenes.remove(s.owner);h.fail("Actual crouched web traversal failed: "+detail);}
        });
    }
    private static double top(net.minecraft.server.level.ServerLevel l,Vec3 point){double top=point.y;var footprint=new net.minecraft.world.phys.AABB(point.x-.3,point.y-.3,point.z-.3,point.x+.3,point.y+.5,point.z+.3);for(var shape:l.getBlockCollisions(null,footprint))top=Math.max(top,shape.max(net.minecraft.core.Direction.Axis.Y));return top;}
    private RescueTraversalGameTests(){}
}
