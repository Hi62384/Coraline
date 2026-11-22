package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.registry.*;
import com.coraline.parents.*;
import com.coraline.doll.DollBondData;
import com.coraline.bobinsky.*;
import com.coraline.tea.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraftforge.gametest.GameTestHolder;
import java.util.*;

@GameTestHolder("coraline")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public final class MissingParentsTeaGameTests {
    @GameTest(template="empty") public static void savedTeaChairRejectsUnrelatedEntityWithoutLosingIdentity(GameTestHelper h){
        var p=fixture(h);var s=OtherMotherLoop.scene(p);var n=MissingParentsTea.data(s);
        com.coraline.tea.TeaSeatEntity original=null;
        try{
            owner(p,s);n.putBoolean("MissingParentsTea",true);n.putBoolean("Active",true);
            BobinskyVisitData.get(p.server).teaVisits.put(s.owner,n);
            var at=p.position().add(1,0,0);
            original=MissingParentsTea.seat(p,n,"Seat",at);
            h.assertTrue(original!=null&&MissingParentsTea.authenticSeat(original,p,n),
                "The owner-bound original visitor chair was not staged");
            var originalId=original.getUUID();
            h.assertTrue(MissingParentsTea.seat(p,n,"Seat",at)==original,
                "The saved chair must reuse the same actor, never generate a duplicate");
            original.getPersistentData().putUUID("TeaToken",UUID.randomUUID());
            h.assertTrue(MissingParentsTea.seat(p,n,"Seat",at)==null,
                "A same-UUID chair with a foreign visit token was accepted");
            h.assertTrue(n.getUUID("Seat").equals(originalId),
                "Invalid saved chair identity was silently replaced");
        }finally{
            if(original!=null)original.discard();
            remove(p,s);
        }
        h.succeed();
    }

    static ServerPlayer fixture(GameTestHelper h){var l=h.getLevel().getServer().overworld();var p=new ServerPlayer(l.getServer(),l,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"SeeingStoneQA"));p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.loop.event.putInt("Serial",3);s.table=new BlockPos(108000,180,108000);s.piano=s.table;p.setPos(s.table.getCenter());VisitData.get(p.server).scenes.put(s.owner,s);return p;}
    static void ready(ServerPlayer p,VisitData.Scene s){var n=FirstEscapeStory.data(s);n.putBoolean("Started",true);n.putBoolean("Complete",true);n.putBoolean("OtherWorldSealed",true);n.putString("Phase","ESCAPE_COMPLETE");}
    static void owner(ServerPlayer p,VisitData.Scene s){var n=MissingParentsTea.data(s);n.putUUID("Owner",p.getUUID());n.putUUID("Door",s.door);n.putUUID("Token",UUID.randomUUID());}
    static void remove(ServerPlayer p,VisitData.Scene s){OtherWorld.cancelCopy(p.server,s.door);RealmData.get(p.server).realms.remove(s.door);VisitData.get(p.server).scenes.remove(s.owner);BobinskyVisitData.get(p.server).teaVisits.remove(s.owner);DoorData.get(p.server).doors.remove(s.door);FamilyData.get(p.server).families.values().removeIf(f->s.owner.equals(f.owner));DollBondData.get(p.server).bonds.remove(s.owner);}
    @GameTest(template="empty") public static void porchIsOutsideEntryRestrictionEvenWithTerrainClearance(GameTestHelper h){
        var n=new CompoundTag();n.putLong("House",new BlockPos(100,80,100).asLong());n.putBoolean("DetailedTeaHouse",true);
        for(boolean clearance:new boolean[]{false,true})for(boolean floating:new boolean[]{false,true}){
            n.putBoolean("TerrainClearanceV3",clearance);n.putBoolean("FloatingLanding",floating);
            var door=BobinskyHouse.door(n);
            for(int step=0;step<7;step++)h.assertFalse(BobinskyHouse.insideRooms(n,door.getCenter().add(0,0,-step*.5)),"Approaching the door triggered entry restriction");
            h.assertTrue(BobinskyHouse.insideRooms(n,door.getCenter().add(0,0,1)),"Actual interior lost entry protection");
        }h.succeed();
    }
    @GameTest(template="empty") public static void debugItemIsRegisteredAndRejectsNonOperators(GameTestHelper h){var p=fixture(h);var s=OtherMotherLoop.scene(p);try{h.assertTrue(ModItems.FIRST_ESCAPE_DEBUG.get() instanceof com.coraline.item.FirstEscapeDebugItem,"Missing creative registry item");h.assertTrue(com.coraline.item.FirstEscapeDebugItem.activate(p,false).contains("permission"),"Non-operator bypassed permissions");h.assertFalse(FirstEscapeStory.engaged(s),"Denied item changed progression");}finally{remove(p,s);}h.succeed();}
    @GameTest(template="empty") public static void canonicalCompletionIsIdempotentSealedAndRewardFree(GameTestHelper h){var p=fixture(h);var s=OtherMotherLoop.scene(p);var l=p.serverLevel();var pos=new BlockPos(108300,180,108300);l.getChunkAt(pos);l.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),2);l.setBlock(pos,ModBlocks.TINY_DOOR.get().defaultBlockState(),2);var d=new DoorData.Door();d.id=s.door;d.owner=s.owner;d.pos=pos;d.placed=true;DoorData.get(p.server).doors.put(d.id,d);var family=new FamilyData.Family();family.owner=s.owner;FamilyData.get(p.server).families.put(family.id,family);var bond=new DollBondData.Bond();bond.familyId=family.id;DollBondData.get(p.server).bonds.put(s.owner,bond);try{h.assertTrue(FirstEscapeStory.finishFirstEscape(p,s,false),"Canonical finalizer failed");h.assertTrue(FirstEscapeStory.finishFirstEscape(p,s,false),"Repeated finalizer failed");h.assertTrue(FirstEscapeStory.sealed(p.server,s.owner)&&family.storyMissing&&bond.storyMissing,"Missing/sealed ledgers not committed");h.assertFalse(FirstEscapeStory.sealed(p.server,UUID.randomUUID()),"Another owner sealed");h.assertTrue(l.getBlockState(pos.relative(Direction.SOUTH)).is(Blocks.BRICKS),"Door backing not bricked");h.assertTrue(BlackIsTraditional.data(s).getBoolean("Rejected"),"Proposal path inconsistent");h.assertTrue(FirstEscapeStory.data(s).getBoolean("WybieDeparted"),"Departure checkpoint absent");h.assertFalse(p.getInventory().contains(new ItemStack(Items.DIAMOND_BLOCK)),"Skipping granted chase reward");h.assertTrue(MissingParentsTea.eligible(s),"Aftermath not eligible");}finally{l.removeBlock(pos,false);l.removeBlock(pos.south(),false);remove(p,s);}h.succeed();}
    @GameTest(template="empty") public static void chanceRollPersistsAndCannotRepeatOrRunBeforeEscape(GameTestHelper h){var p=fixture(h);var s=OtherMotherLoop.scene(p);try{h.assertFalse(MissingParentsTea.select(p,s,false),"Selected before completion");ready(p,s);MissingParentsTea.select(p,s,false);var n=MissingParentsTea.data(s);double roll=n.getDouble("Roll");boolean selected=n.getBoolean("Selected");for(int i=0;i<30;i++)MissingParentsTea.select(p,s,false);h.assertTrue(n.getDouble("Roll")==roll&&n.getBoolean("Selected")==selected,"Chance rerolled");var loaded=OtherMotherLoopState.load(s.loop.save());h.assertTrue(loaded.story.getCompound(MissingParentsTea.KEY).getDouble("Roll")==roll,"Roll lost on reload");n.putBoolean("Completed",true);h.assertFalse(MissingParentsTea.select(p,s,true),"Completed visit was selected again");}finally{remove(p,s);}h.succeed();}
    @GameTest(template="empty") public static void seeingStoneFullCreativeInventoryIsReservedAndGrantedExactlyOnce(GameTestHelper h){var p=fixture(h);var s=OtherMotherLoop.scene(p);try{ready(p,s);owner(p,s);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));MissingParentsTea.award(p,s);h.assertTrue(s.loop.story.getBoolean("SeeingStonePending"),"Full creative inventory destroyed stone");p.getInventory().setItem(5,ItemStack.EMPTY);MissingParentsTea.claim(p,s);h.assertTrue(p.getInventory().getItem(5).is(ModItems.SEEING_STONE.get()),"Reserved stone not delivered");h.assertFalse(s.loop.story.getBoolean("SeeingStonePending"),"Delivered claim still pending");MissingParentsTea.award(p,s);MissingParentsTea.claim(p,s);int count=0;for(var item:p.getInventory().items)if(item.is(ModItems.SEEING_STONE.get()))count+=item.getCount();h.assertTrue(count==1,"Repeated cue duplicated stone");h.assertTrue(p.getInventory().getItem(5).getTag().getUUID("StoryOwner").equals(s.owner),"Stone lost owner receipt");}finally{remove(p,s);}h.succeed();}
    @GameTest(template="empty") public static void helperCannotClaimOwnerStone(GameTestHelper h){var p=fixture(h);var helper=fixture(h);var s=OtherMotherLoop.scene(p);var hs=OtherMotherLoop.scene(helper);try{ready(p,s);owner(p,s);MissingParentsTea.award(helper,s);h.assertFalse(s.loop.story.getBoolean("SeeingStoneReceipt"),"Helper created owner receipt");h.assertFalse(helper.getInventory().contains(new ItemStack(ModItems.SEEING_STONE.get())),"Helper stole stone");}finally{remove(p,s);remove(helper,hs);}h.succeed();}
    @GameTest(template="empty") public static void allTenOriginalsUseAcknowledgedMeasuredClocks(GameTestHelper h){var p=fixture(h);var s=OtherMotherLoop.scene(p);try{var ids=new HashSet<String>();for(var phase:MissingParentsTea.Phase.values()){String id=MissingParentsTea.sound(phase);if(id.isEmpty())continue;h.assertTrue(ids.add(id),"Duplicate audio checkpoint");h.assertTrue(VisitAudio.overworldStoryVoice(id),"OW cleanup silences "+id);h.assertTrue(ShowPlayback.start(p,s,id,false),"Missing original "+id);var playback=ShowPlayback.data(s);ShowPlayback.tick(p,s);h.assertTrue(ShowPlayback.position(s)==0,"Loading advanced cue");var nonce=playback.getUUID("Nonce");ShowPlayback.signal(p,nonce,id,0);ShowPlayback.signal(p,nonce,id,1);ShowPlayback.tick(p,s);h.assertFalse(ShowPlayback.finished(s),"Early acknowledgement skipped original");playback.putInt("Position",playback.getInt("Duration")-1);h.assertFalse(ShowPlayback.tick(p,s),"Fast server ticks bypassed real elapsed audio");PlaybackFixtureClock.elapseOriginal(s);h.assertTrue(ShowPlayback.tick(p,s),"Measured ending failed");ShowPlayback.stop(p,s);}h.assertTrue(ids.size()==10&&ids.contains("tea_visit_a")&&ids.contains("missing_parents_tea_a"),"Greeting confused with story A");h.assertTrue(ShowRecordings.duration("missing_parents_tea_i")>100,"I cannot fit release cue");}finally{remove(p,s);}h.succeed();}
    @GameTest(template="empty") public static void candyCrushingEndsExactlyAtRecordingEnd(GameTestHelper h){int duration=ShowRecordings.duration("missing_parents_tea_f");h.assertTrue(MissingParentsTea.crush(0,duration)==0&&MissingParentsTea.crush(28,duration)==0,"Candy crushed before raised-needle moment");h.assertTrue(MissingParentsTea.crush(duration-1,duration)<1,"Candy completed early");h.assertTrue(MissingParentsTea.crush(duration,duration)==1,"Intact candy remains after F");h.succeed();}
    @GameTest(template="empty") public static void checkpointReloadPreservesBowlAndReward(GameTestHelper h){var p=fixture(h);var s=OtherMotherLoop.scene(p);try{ready(p,s);owner(p,s);var n=MissingParentsTea.data(s);n.putString("StoryPhase","STORY_G");n.putBoolean("Powder",true);n.putBoolean("Revealed",true);n.putBoolean("Seated",true);n.putInt("BowlStep",4);s.loop.story.putBoolean("SeeingStoneReceipt",true);s.loop.story.putBoolean("SeeingStonePending",true);var root=OtherMotherLoopState.load(s.loop.save()).story;var r=root.getCompound(MissingParentsTea.KEY);h.assertTrue(MissingParentsTea.phase(r)==MissingParentsTea.Phase.STORY_G&&r.getBoolean("Powder")&&r.getBoolean("Revealed"),"Bowl checkpoint lost");h.assertTrue(root.getBoolean("SeeingStoneReceipt")&&root.getBoolean("SeeingStonePending"),"Reward receipt lost");for(String id:List.of("complete_escape","force","knock","sit","phase_story_a","phase_story_h","phase_story_i","cleanup","reset"))h.assertTrue(NumberedDebugControls.find("spink",id)!=null,"Missing debug control "+id);}finally{remove(p,s);}h.succeed();}
}
