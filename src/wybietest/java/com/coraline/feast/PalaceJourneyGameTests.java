package com.coraline.feast;

import java.util.UUID;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;

/** Native fixtures are compiled by the small API check; run them only once RAM
 * is available. These are negative/serialization gates, not a Palace playthrough.
 */
@GameTestHolder("coraline")
@PrefixGameTestTemplate(false)
public final class PalaceJourneyGameTests {
    private static VisitData.Scene ready(){
        var s=new VisitData.Scene();s.owner=UUID.randomUUID();s.door=UUID.randomUUID();
        var rescue=new CompoundTag();var end=new CompoundTag();end.putBoolean("Started",true);end.putString("Phase","COMPLETE");
        for(String key:new String[]{"PermanentlySealed","ParentsReturned","FarewellComplete","FarewellCatDeparted","HandSevered"})end.putBoolean(key,true);
        rescue.put("Continuation",end);rescue.putBoolean("DoorLocked",true);var eyes=new CompoundTag();eyes.putBoolean("Released",true);rescue.put("GhostEyes",eyes);s.loop.story.put("ParentRescue",rescue);return s;
    }
    @GameTest(template="empty") public static void palacePrerequisitesCannotBeSkipped(GameTestHelper h){
        var s=ready();h.assertTrue(PalaceJourney.prerequisites(s).ready(),"Actual completed-epilogue flags rejected");
        var end=s.loop.story.getCompound("ParentRescue").getCompound("Continuation");
        for(String key:new String[]{"Started","PermanentlySealed","ParentsReturned","FarewellComplete","FarewellCatDeparted","HandSevered"}){
            end.putBoolean(key,false);h.assertFalse(PalaceJourney.prerequisites(s).ready(),"Missing prerequisite ignored: "+key);end.putBoolean(key,true);
        }
        end.putString("Phase","FAREWELL_C");h.assertFalse(PalaceJourney.prerequisites(s).ready(),"Incomplete farewell accepted");end.putString("Phase","COMPLETE");
        s.loop.story.getCompound("ParentRescue").putBoolean("DoorLocked",false);h.assertFalse(PalaceJourney.prerequisites(s).ready(),"Unsealed door accepted");
        s=ready();s.loop.story.getCompound("ParentRescue").getCompound("GhostEyes").putBoolean("Released",false);h.assertFalse(PalaceJourney.prerequisites(s).ready(),"Unreleased eyes accepted");h.succeed();
    }
    @GameTest(template="empty") public static void absentForeignAndFutureJourneyAreNeverOverwritten(GameTestHelper h){
        var s=ready();var before=s.loop.story.copy();h.assertTrue(PalaceJourney.existing(s)==null,"Absent journey fabricated");
        h.assertTrue(s.loop.story.equals(before),"Read allocated journey state");
        var root=new CompoundTag();root.putInt("Version",2);root.putUUID("Owner",s.owner);root.putUUID("Door",s.door);root.putString("Evidence","preserve");s.loop.story.put("PinkPalaceJourney",root);
        before=s.loop.story.copy();h.assertTrue(PalaceJourney.existing(s)==null&&s.loop.story.equals(before),"Future journey schema overwritten");
        root.putInt("Version",1);root.putUUID("Owner",UUID.randomUUID());before=s.loop.story.copy();h.assertTrue(PalaceJourney.existing(s)==null&&s.loop.story.equals(before),"Foreign journey overwritten");
        root.putUUID("Owner",s.owner);h.assertTrue(PalaceJourney.existing(s)==root,"Valid owner journey not retained");
        for(String key:new String[]{"MapId","MapIssued","Company","MapRecoveryAfter","Well","KeyCustody","BundleEntity"}){
            root.putString(key,"bad schema");before=s.loop.story.copy();h.assertTrue(PalaceJourney.existing(s)==null&&s.loop.story.equals(before),"Malformed journey overwritten: "+key);root.remove(key);
        }
        root.putBoolean("MapIssued",true);h.assertTrue(PalaceJourney.existing(s)==null,"Issued map with no actual ID/company accepted");root.remove("MapIssued");h.succeed();
    }
    @GameTest(template="empty") public static void explorerMapBindingsSurviveActualItemSerialization(GameTestHelper h){
        var s=ready();var company=new net.minecraft.core.BlockPos(1800,74,2600);var map=new ItemStack(Items.FILLED_MAP);var n=map.getOrCreateTag();n.putInt("map",19);
        n.putUUID("PalaceMapOwner",s.owner);n.putUUID("PalaceMapDoor",s.door);n.putLong("PalaceMapCompany",company.asLong());
        net.minecraft.world.level.saveddata.maps.MapItemSavedData.addTargetDecoration(map,company,"moving_company",net.minecraft.world.level.saveddata.maps.MapDecoration.Type.RED_X);
        var loaded=ItemStack.of(map.save(new CompoundTag()));h.assertTrue(PalaceJourney.map(loaded,s,company),"Vanilla item reload lost company binding");
        h.assertTrue(MapItem.getMapId(loaded)==19&&loaded.getTag().getList("Decorations",Tag.TAG_COMPOUND).size()==1,"Map ID or company marker lost");
        h.assertFalse(PalaceJourney.map(loaded,s,company.east()),"Stale company coordinates accepted");
        loaded.getTag().putUUID("PalaceMapOwner",UUID.randomUUID());h.assertFalse(PalaceJourney.map(loaded,s,company),"Foreign map accepted");
        h.assertFalse(PalaceJourney.map(new ItemStack(Items.MAP),s,company),"Empty map accepted as explorer map");h.succeed();
    }
}
