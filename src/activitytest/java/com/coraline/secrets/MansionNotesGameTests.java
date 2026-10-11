package com.coraline.secrets;
import com.coraline.feast.*;
import com.coraline.otherworld.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class MansionNotesGameTests {
    @GameTest(template="empty") public static void roomMarksPersistAndRejectStalePackets(GameTestHelper h){
        var l=h.getLevel().getServer().getLevel(ActivityWorld.KEY);
        var p=FakePlayerFactory.get(l,new GameProfile(UUID.randomUUID(),"MansionNotesQA"));
        var door=new DoorData.Door();door.owner=p.getUUID();door.pos=new BlockPos(68000,80,68000);
        DoorData.get(p.server).doors.put(door.id,door);
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=door.id;s.stage=VisitData.Stage.DONE;s.table=door.pos;s.piano=door.pos;
        s.loop.phase=OtherMotherLoopState.Phase.ACTIVITY;s.loop.activity=OtherMotherActivities.Activity.OTHER_WOODLAND_MANSION;
        VisitData.get(p.server).scenes.put(s.owner,s);p.getPersistentData().putUUID("CoralineVisitDoor",s.door);
        var session=UUID.randomUUID();s.loop.expedition.putUUID("JournalSession",session);
        MansionActivity.markRoom(p,session,0,true);MansionActivity.markRoom(p,session,11,true);MansionActivity.markRoom(p,session,11,true);
        h.assertTrue(s.loop.expedition.getInt("ExcludedRooms")==2049,"Room marks not idempotent across floors");
        MansionActivity.markRoom(p,UUID.randomUUID(),0,false);MansionActivity.markRoom(p,session,12,true);MansionActivity.markRoom(p,session,-1,true);
        h.assertTrue(s.loop.expedition.getInt("ExcludedRooms")==2049,"Invalid packet changed notes");
        var helper=FakePlayerFactory.get(l,new GameProfile(UUID.randomUUID(),"NotesHelper"));
        MansionActivity.markRoom(helper,session,0,false);
        h.assertTrue(s.loop.expedition.getInt("ExcludedRooms")==2049,"Helper changed owner notes");
        var saved=OtherMotherLoopState.load(s.loop.save());h.assertTrue(saved.expedition.equals(s.loop.expedition),"Notes lost on reload");
        MansionActivity.markRoom(p,session,0,false);h.assertTrue(s.loop.expedition.getInt("ExcludedRooms")==2048,"Cannot clear mark");
        h.assertTrue(s.loop.phase==OtherMotherLoopState.Phase.ACTIVITY,"Notes changed activity outcome");h.succeed();
    }
}
