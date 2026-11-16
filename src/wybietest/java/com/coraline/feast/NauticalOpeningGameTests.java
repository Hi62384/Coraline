package com.coraline.feast;

import java.util.UUID;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

/** Future server regression cases. No synthetic playback ACKs; these test the
 * negative playback/owner gates, not audibility or a completed live opening.
 */
@GameTestHolder("coraline")
@PrefixGameTestTemplate(false)
public final class NauticalOpeningGameTests {
    private static ServerPlayer player(GameTestHelper helper){
        var level=helper.getLevel().getServer().getLevel(ActivityWorld.KEY);
        if(level==null)throw new AssertionError("Activity dimension missing");
        var player=new ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"NauticalGateQA"));
        player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(player.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player);
        var scene=new VisitData.Scene();scene.owner=player.getUUID();scene.door=UUID.randomUUID();scene.stage=VisitData.Stage.DONE;
        scene.loop.phase=OtherMotherLoopState.Phase.THEATRE_ACTIVE;scene.loop.event.putString("Type","THEATRE");scene.loop.event.putInt("Serial",7);scene.loop.event.putBoolean("TheatrePending",true);
        var n=TheatreVisit.data(scene);n.putUUID("Owner",scene.owner);n.putInt("Serial",7);n.putString("Phase","PERFORMING");n.putBoolean("InvitationDone",true);
        n.putInt("NauticalVersion",1);n.putInt("OpeningCue",3);n.putInt("OpeningSeatDelay",60);
        player.getPersistentData().putUUID("CoralineVisitDoor",scene.door);VisitData.get(player.server).scenes.put(scene.owner,scene);
        player.setPos(Vec3.atBottomCenterOf(TheatreSite.center(player,scene)));
        return player;
    }
    private static void remove(ServerPlayer p){var s=OtherMotherLoop.scene(p);if(s!=null){ShowPlayback.stop(p,s);VisitData.get(p.server).scenes.remove(s.owner);}}
    @GameTest(template="empty") public static void allOriginalsHaveDistinctRegisteredActualTiming(GameTestHelper h){
        for(var entry:FinalChapterRecordings.SOURCES.entrySet()){
            h.assertTrue(VisitAudio.SOUNDS.containsKey(entry.getKey()),"Missing registered original: "+entry.getKey());
            int actual=ShowRecordings.duration(entry.getKey());
            h.assertTrue(actual>0&&actual==VisitAudio.length(entry.getKey()),"Actual OGG/timing table mismatch: "+entry.getKey());
            h.assertFalse(entry.getKey().equals("theatre_music")||entry.getKey().matches("theatre_[a-f]"),"Opening overwrote preserved diving/music identity");
        }h.assertTrue(NauticalOpeningShow.blocker().isEmpty(),"Original set cannot form a valid nautical plan");h.succeed();
    }
    @GameTest(template="empty") public static void noClientAcknowledgementCannotFinishOrAdvanceOpening(GameTestHelper h){
        var p=player(h);var s=OtherMotherLoop.scene(p);try{
            for(int tick=0;tick<20;tick++)h.assertFalse(NauticalOpeningShow.tick(p,s),"An unplayed cue completed");
            h.assertTrue(TheatreVisit.data(s).getInt("OpeningCue")==3,"No-ACK playback skipped cue D");
            h.assertTrue(ShowPlayback.position(s)==0&&!ShowPlayback.started(s),"No-ACK playback invented a heard cursor");
            h.assertFalse(TheatreVisit.data(s).getBoolean("PerformanceDone")||s.loop.story.getBoolean("TheatreCompleted"),"No-ACK playback awarded completion");
        }finally{remove(p);}h.succeed();
    }
    @GameTest(template="empty") public static void interruptPreservesCueAndDoesNotGrantProgress(GameTestHelper h){
        var p=player(h);var s=OtherMotherLoop.scene(p);try{
            var n=TheatreVisit.data(s);n.putBoolean("OpeningCueStarted",true);n.putInt("NauticalPreview",12);n.putString("Checkpoint","ACT_ONE_IN_PROGRESS");
            NauticalOpeningShow.interrupt(s);
            h.assertTrue(n.getInt("OpeningCue")==3&&n.getString("Checkpoint").equals("ACT_ONE_IN_PROGRESS"),"Interrupt reset/skipped the saved cue");
            h.assertFalse(n.getBoolean("OpeningCueStarted")||n.contains("NauticalPreview"),"Interrupt retained stale playback/preview state");
            h.assertFalse(n.getBoolean("PerformanceDone")||n.getBoolean("RoseDelivered"),"Interrupt granted a performance/reward");
        }finally{remove(p);}h.succeed();
    }
    @GameTest(template="empty") public static void entrancePreludeRequiresInsideVenueAndSeating(GameTestHelper h){
        var p=player(h);var s=OtherMotherLoop.scene(p);try{
            var n=TheatreVisit.data(s);n.putString("Phase","SEATING");var packet=new CompoundTag();TheatreVisit.status(p,s,packet);
            h.assertTrue(packet.getBoolean("TheatrePrelude"),"Seating inside the venue lost Entrance");
            p.setPos(p.position().add(100,0,0));TheatreVisit.status(p,s,packet);h.assertFalse(packet.getBoolean("TheatrePrelude"),"Entrance leaked outside venue");
            p.setPos(Vec3.atBottomCenterOf(TheatreSite.center(p,s)));n.putString("Phase","PERFORMING");TheatreVisit.status(p,s,packet);
            h.assertFalse(packet.getBoolean("TheatrePrelude"),"Entrance survived the reserved-seat boundary");
        }finally{remove(p);}h.succeed();
    }
}
