package com.coraline.wybie;
import com.coraline.feast.*;
import com.coraline.parents.FamilyData;
import com.coraline.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.gametest.*;
import java.util.UUID;
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class WybieVisitGameTests {
    private static VisitData.Scene scene(){var s=new VisitData.Scene();s.owner=UUID.randomUUID();s.door=UUID.randomUUID();s.family=UUID.randomUUID();s.piano=s.table=BlockPos.ZERO;s.stage=VisitData.Stage.DONE;return s;}
    @GameTest(template="empty") public static void onlyCompletedDaytimeHomeVisitIsEligible(GameTestHelper h){
        var s=scene();h.assertTrue(WybieVisit.eligible(s,true,true,true),"Completed visit blocked");
        h.assertFalse(WybieVisit.eligible(s,false,true,true),"Other dimension allowed");h.assertFalse(WybieVisit.eligible(s,true,false,true),"Night allowed");h.assertFalse(WybieVisit.eligible(s,true,true,false),"Away from bed allowed");
        s.stage=VisitData.Stage.BED;h.assertFalse(WybieVisit.eligible(s,true,true,true),"Unfinished visit allowed");s.stage=VisitData.Stage.DONE;s.wybieVisited=true;h.assertFalse(WybieVisit.eligible(s,true,true,true),"Second visit allowed");h.succeed();
    }
    @GameTest(template="empty") public static void claimedVisitSurvivesSaveAndParentDeathClearsIt(GameTestHelper h){
        var data=VisitData.get(h.getLevel().getServer());var s=scene();s.wybieVisited=true;s.wybieActor=UUID.randomUUID();data.scenes.put(s.owner,s);
        var copy=VisitData.load(data.save(new CompoundTag())).scenes.get(s.owner);h.assertTrue(copy.wybieVisited&&s.wybieActor.equals(copy.wybieActor),"Claim lost on save");
        var family=new FamilyData.Family();family.owner=s.owner;family.id=s.family;FamilyVisitReset.endFamily(h.getLevel().getServer(),family);
        h.assertFalse(data.scenes.containsKey(s.owner),"Parent death retained Wybie claim");h.assertTrue(WybieVisit.eligible(scene(),true,true,true),"Fresh story did not reset");h.succeed();
    }
    @GameTest(template="empty") public static void visitorCannotDieFromDamageOrKill(GameTestHelper h){
        var e=ModEntities.WYBIE_VISITOR.get().create(h.getLevel());h.assertTrue(e!=null,"No visitor attributes");float health=e.getHealth();
        h.assertFalse(e.hurt(h.getLevel().damageSources().generic(),1000),"Damage accepted");h.assertFalse(e.hurt(h.getLevel().damageSources().fellOutOfWorld(),1000),"Void killed visitor");e.kill();e.die(h.getLevel().damageSources().generic());
        h.assertTrue(e.isAlive()&&!e.isRemoved()&&e.getHealth()==health,"Visitor died");h.assertTrue(e.characterId().equals("wybie"),"Wrong improved model");e.discard();h.succeed();
    }
    @GameTest(template="empty") public static void actorProgressAndCameraClaimSurviveSave(GameTestHelper h){
        var e=ModEntities.WYBIE_VISITOR.get().create(h.getLevel());var n=new CompoundTag();n.putUUID("VisitOwner",UUID.randomUUID());n.putUUID("VisitDoor",UUID.randomUUID());n.putInt("VisitPhase",WybieVisitorEntity.G);n.putInt("VisitTime",21);n.putBoolean("CameraGiven",true);n.putBoolean("FlashDone",true);e.readAdditionalSaveData(n);
        var saved=new CompoundTag();e.addAdditionalSaveData(saved);var copy=ModEntities.WYBIE_VISITOR.get().create(h.getLevel());copy.readAdditionalSaveData(saved);
        h.assertTrue(copy.phase()==WybieVisitorEntity.G&&copy.elapsed()==21&&copy.hasSlug(),"Pose lost on reload");h.assertTrue(saved.getBoolean("CameraGiven")&&saved.getBoolean("FlashDone"),"One-time action flags lost");h.assertTrue(e.owner.equals(copy.owner)&&e.door.equals(copy.door),"Ownership lost");e.discard();copy.discard();h.succeed();
    }
    @GameTest(template="empty") public static void twelveRecordingsAndMusicHaveActualDurations(GameTestHelper h){
        StringBuilder lines=new StringBuilder();for(int phase=1;phase<=14;phase++){String line=WybieVisitorEntity.line(phase);if(!line.isEmpty()){lines.append(line);h.assertTrue(VisitAudio.length("wybie_visit_"+line)>0,"Missing timing "+line);}}
        h.assertTrue(lines.toString().equals("abcdefghijkl"),"Dialogue order changed");h.assertTrue(VisitAudio.length("wybie_visit_music")>0,"Music missing");h.succeed();
    }
}
