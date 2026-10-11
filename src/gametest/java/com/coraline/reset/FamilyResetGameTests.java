package com.coraline.reset;

import com.coraline.feast.*;
import com.coraline.otherworld.*;
import com.coraline.parents.*;
import com.coraline.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class FamilyResetGameTests {
    private static VisitData.Scene scene(UUID owner,UUID family,UUID door){
        var s=new VisitData.Scene();s.owner=owner;s.family=family;s.door=door;s.table=new BlockPos(0,80,0);s.piano=new BlockPos(20,80,0);s.stage=VisitData.Stage.DONE;s.elapsed=800;s.cakeTaken=true;return s;
    }
    @GameTest(template="empty") public static void familyIdentitySurvivesSaving(GameTestHelper h){
        var data=new VisitData();var s=scene(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID());data.scenes.put(s.owner,s);
        var copy=VisitData.load(data.save(new CompoundTag())).scenes.get(s.owner);
        h.assertTrue(s.family.equals(copy.family)&&copy.stage==VisitData.Stage.DONE,"Family-scoped completion lost on restart");h.succeed();
    }
    @GameTest(template="empty") public static void deadFamilyResetsOfflineOwnerWithoutTouchingOtherPlayer(GameTestHelper h){
        var server=h.getLevel().getServer();var family=new FamilyData.Family();family.owner=UUID.randomUUID();family.dead=true;
        var door=new DoorData.Door();door.owner=family.owner;DoorData.get(server).doors.put(door.id,door);
        var realm=RealmData.get(server).realm(door);realm.ready=true;
        var data=VisitData.get(server);var old=scene(family.owner,family.id,door.id);data.scenes.put(old.owner,old);
        var unrelated=scene(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID());data.scenes.put(unrelated.owner,unrelated);
        FamilyVisitReset.endFamily(server,family);
        h.assertFalse(data.scenes.containsKey(family.owner),"Dead parents retained completed visit/cake flags");
        h.assertFalse(RealmData.get(server).realms.containsKey(door.id),"Dead family kept old realm record");
        h.assertTrue(data.scenes.get(unrelated.owner)==unrelated,"Reset affected another player's story");
        h.assertFalse(VisitData.load(data.save(new CompoundTag())).scenes.containsKey(family.owner),"Reset did not survive save/load");h.succeed();
    }
    @GameTest(template="empty") public static void replacementFamilyStartsFreshButSameFamilyDoesNot(GameTestHelper h){
        var server=h.getLevel().getServer();var s=scene(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID());var data=VisitData.get(server);data.scenes.put(s.owner,s);
        FamilyVisitReset.validate(server,s.owner,s.family);h.assertTrue(data.scenes.get(s.owner)==s,"Same pair lost progress");
        FamilyVisitReset.validate(server,s.owner,UUID.randomUUID());h.assertFalse(data.scenes.containsKey(s.owner),"New parents inherited old completed visit");h.succeed();
    }
    @GameTest(template="empty") public static void legacyRevokedDoorDoesNotAdoptNewFamily(GameTestHelper h){
        var server=h.getLevel().getServer();var door=new DoorData.Door();door.owner=UUID.randomUUID();door.revoked=true;DoorData.get(server).doors.put(door.id,door);
        var s=scene(door.owner,null,door.id);VisitData.get(server).scenes.put(s.owner,s);
        FamilyVisitReset.validate(server,s.owner,UUID.randomUUID());
        h.assertFalse(VisitData.get(server).scenes.containsKey(s.owner),"Legacy save incorrectly kept old parent's completion");h.succeed();
    }
    @GameTest(template="empty") public static void unloadedOldPropsRetireDuringNewVisit(GameTestHelper h){
        var level=h.getLevel().getServer().getLevel(OtherWorld.KEY);var s=scene(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID());s.stage=VisitData.Stage.SUPPER;VisitData.get(level.getServer()).scenes.put(s.owner,s);
        var old=ModEntities.SCENE_PROP.get().create(level);old.owner=s.owner;old.storyDoor=UUID.randomUUID();old.kind(ScenePropEntity.CHAIR);old.setPos(s.table.getCenter());
        old.tick();h.assertTrue(old.isRemoved(),"Old family chair joined the replacement family's supper");
        var legacy=ModEntities.SCENE_PROP.get().create(level);legacy.owner=s.owner;legacy.setPos(s.table.offset(2048,0,0).getCenter());legacy.tick();h.assertTrue(legacy.isRemoved(),"Unversioned old prop joined new island");h.succeed();
    }
}
