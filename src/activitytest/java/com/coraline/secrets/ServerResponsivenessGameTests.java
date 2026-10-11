package com.coraline.secrets;

import com.coraline.feast.*;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class ServerResponsivenessGameTests {
    @GameTest(template="empty",timeoutTicks=400) public static void chunkRequestCanBePolledWithoutBlocking(GameTestHelper h){
        var source=h.getLevel().getChunkSource();var pos=new net.minecraft.world.level.ChunkPos(120,120);var ticket=java.util.UUID.randomUUID();
        var future=com.coraline.otherworld.RealmCopy.requestChunk(h.getLevel(),pos,ticket);
        h.runAtTickTime(399,()->source.removeRegionTicket(com.coraline.otherworld.RealmCopy.COPY_TICKET,pos,0,ticket));
        h.succeedWhen(()->{h.assertTrue(future.isDone()&&future.getNow(null).left().isPresent(),"Chunk request has not completed");source.removeRegionTicket(com.coraline.otherworld.RealmCopy.COPY_TICKET,pos,0,ticket);});
    }
    @GameTest(template="empty") public static void ordinaryVisitVoiceUsesPlaybackClock(GameTestHelper h){
        var p=SecretsGameTests.fixture(h);var scene=OtherMotherLoop.scene(p);
        String sound=SecretDialogue.pool(SecretDialogue.Event.ORDINARY_AREA_CAUGHT,SecretDialogue.state(p)).sound(0);
        VisitAudio.play(p,sound,false);var n=scene.loop.secrets.dialogue;
        h.assertTrue(n.getString("LoopVoice").equals(sound)&&n.getLong("VoiceClockEnd")>System.currentTimeMillis(),"Normal visit voice has no real playback clock");
        scene.loop.deadline=OtherMotherLoop.now(p)+500;scene.welcomeAfter=scene.loop.deadline;
        n.putLong("VoiceClockEnd",System.currentTimeMillis()-1);SecretDialogue.finished(p,sound);
        h.assertTrue(scene.loop.deadline<=OtherMotherLoop.now(p)+2&&scene.welcomeAfter<=OtherMotherLoop.now(p)+2,"Normal visit waits on slow server after voice ended");h.succeed();
    }
    @GameTest(template="empty") public static void studyFallbackRemainsValid(GameTestHelper h) throws Exception {
        var p=SecretsGameTests.fixture(h);var scene=OtherMotherLoop.scene(p);var realm=SecretWorld.realm(p);
        var choose=LoopProps.class.getDeclaredMethod("choosePortal",net.minecraft.server.level.ServerPlayer.class,VisitData.Scene.class);choose.setAccessible(true);
        h.assertTrue((Boolean)choose.invoke(null,p,scene),"Missing outdoor terrain ended visit");
        h.assertTrue(LoopProps.portalSite(realm.house,scene.loop.portal),"Fallback immediately rejected by portal lifecycle");
        h.assertTrue(scene.loop.portal.getX()<realm.house.getX()&&Math.abs(scene.loop.portal.getZ()-realm.house.getZ())<=5,"Fallback is not in study");h.succeed();
    }
}
