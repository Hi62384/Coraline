package com.coraline.rebirth;

import java.util.UUID;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import static com.coraline.rebirth.StoryRun.Phase.*;

/** Native NBT/registry fixtures; not a claim of cinematic or full-playthrough acceptance. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class StoryRebirthGameTests {
    private static void location(StoryRun r){var n=r.details();n.putLong("Center",new net.minecraft.core.BlockPos(0,90,0).asLong());n.putLong("PortraitPosition",new net.minecraft.core.BlockPos(0,91,-2).asLong());n.putUUID("Portrait",UUID.randomUUID());r.details(n);}
    private static StoryRun complete(UUID owner){var r=StoryRun.first(owner);r.finish(true);r.delivered(r.receipt());location(r);return r;}
    private static StoryRun committed(UUID owner){var r=complete(owner);r.commit(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),new CompoundTag());return r;}
    private static void rejected(Runnable code,GameTestHelper h,String why){boolean rejected=false;try{code.run();}catch(IllegalArgumentException e){rejected=true;}h.assertTrue(rejected,why);}
    @GameTest(template="empty") public static void oneHeartOnlyAfterCanonicalFullCompletion(GameTestHelper h){
        var r=StoryRun.first(UUID.randomUUID());h.assertFalse(r.finish(false),"Partial chapter awarded Heart");h.assertTrue(r.finish(true),"Full ending did not reserve reward");
        var receipt=r.receipt();for(int i=0;i<20;i++){r=StoryRun.load(r.owner(),r.save());h.assertFalse(r.finish(true),"Reload replayed reward");h.assertTrue(r.receipt().equals(receipt),"Pending reward identity changed");}
        h.assertTrue(r.completed()==1&&r.earned()==1&&r.rewardPending(),"Lifetime accounting drifted");
        h.assertFalse(r.delivered(UUID.randomUUID()),"Foreign receipt accepted");h.assertTrue(r.delivered(receipt),"Real receipt rejected");h.assertFalse(r.delivered(receipt),"Receipt consumed twice");h.succeed();
    }
    @GameTest(template="empty") public static void firstRunCannotCommitAndRitualCannotGrantHeart(GameTestHelper h){
        var r=StoryRun.first(UUID.randomUUID());h.assertFalse(r.commit(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),new CompoundTag()),"New player could reset");
        r.finish(true);h.assertFalse(r.commit(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),new CompoundTag()),"Pending payout ritual committed");
        r.delivered(r.receipt());location(r);h.assertTrue(r.commit(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),new CompoundTag()),"Eligible commit failed");
        h.assertTrue(r.earned()==1&&!r.rewardPending(),"Ritual paid another Heart");h.assertFalse(r.commit(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),new CompoundTag()),"Duplicate commit accepted");h.succeed();
    }
    @GameTest(template="empty") public static void twoCutsAreOwnerAndSessionBoundNeverAutomatic(GameTestHelper h){
        var owner=UUID.randomUUID();var r=committed(owner);var session=r.session();r.advance(session,SUMMONING,BINDING);r.advance(session,BINDING,FIRST_THREAD);
        for(int i=0;i<20000;i++)r.tick();h.assertTrue(r.phase()==FIRST_THREAD,"Timer cut first thread");
        h.assertFalse(r.advance(session,FIRST_THREAD,FIRST_DEATH),"Automatic transition cut first thread");
        h.assertFalse(r.cut(UUID.randomUUID(),session,true),"Other player cut first thread");h.assertFalse(r.cut(owner,UUID.randomUUID(),true),"Stale session cut thread");
        h.assertTrue(r.cut(owner,session,true),"Owner first cut rejected");h.assertFalse(r.cut(owner,session,true),"First cut replayed");
        r.recordDeath(session,true);r.advance(session,FIRST_DEATH,FINAL_THREAD);r=StoryRun.load(owner,r.save());
        for(int i=0;i<20000;i++)r.tick();h.assertTrue(r.phase()==FINAL_THREAD,"Timer cut surviving parent's thread");h.assertTrue(r.cut(owner,session,false),"Owner final cut rejected");h.succeed();
    }
    @GameTest(template="empty") public static void interruptionAtEveryPhaseRetainsIdentityAndOnlyOneNextRun(GameTestHelper h){
        var owner=UUID.randomUUID();var r=committed(owner);var session=r.session();
        for(var phase:StoryRun.Phase.values()){
            if(phase==IDLE||phase==RETURNING)continue;
            h.assertTrue(r.phase()==phase,"Unexpected phase at "+phase);r=StoryRun.load(owner,r.save());h.assertTrue(session.equals(r.session()),"Reconnect changed transaction");
            if(phase==FIRST_THREAD){r.cut(owner,session,true);continue;}
            if(phase==FINAL_THREAD){r.cut(owner,session,false);continue;}
            if(phase==FIRST_DEATH||phase==FINAL_DEATH)r.recordDeath(session,phase==FIRST_DEATH);
            if(phase==RESTORING){h.assertTrue(r.nextRun(session),"New run initialization failed");break;}
            r.advance(session,phase,StoryRun.Phase.values()[phase.ordinal()+1]);
        }
        r=StoryRun.load(owner,r.save());h.assertFalse(r.nextRun(session),"Reconnect incremented twice");h.assertTrue(r.run()==2&&r.earned()==1&&!r.complete(),"Next run/counters invalid");
        h.assertTrue(r.returned(session),"Return failed");h.assertFalse(r.returned(session),"Return replayed");h.assertFalse(r.commit(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),new CompoundTag()),"Repeated ritual without second completion");
        h.assertTrue(r.finish(true)&&r.earned()==2&&r.completed()==2,"Second full run did not award exactly one additional Heart");h.succeed();
    }
    @GameTest(template="empty") public static void malformedAndForeignJournalsFailClosed(GameTestHelper h){
        var owner=UUID.randomUUID();var r=StoryRun.first(owner);var saved=r.save();
        rejected(()->StoryRun.load(UUID.randomUUID(),saved),h,"Foreign owner accepted");
        for(String key:new String[]{"Run","Completed","HeartsEarned","Phase","Owner"}){var bad=saved.copy();bad.putString(key,"corrupt");rejected(()->StoryRun.load(owner,bad),h,"Corrupt field accepted: "+key);}
        var future=saved.copy();future.putInt("Version",2);rejected(()->StoryRun.load(owner,future),h,"Future schema silently reset");
        var rewind=committed(owner).save();rewind.putString("Phase","RESTORING");rejected(()->StoryRun.load(owner,rewind),h,"Rewind without sacrifice receipts accepted");h.succeed();
    }
    @GameTest(template="empty") public static void ownerRecordsAreIsolatedAndPendingRewardSurvivesCopies(GameTestHelper h){
        var a=complete(UUID.randomUUID());var b=complete(UUID.randomUUID());var before=b.save();a.commit(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),new CompoundTag());
        h.assertTrue(b.save().equals(before)&&!a.receipt().equals(b.receipt()),"Owner ledgers shared rewards/state");
        var copy=a.save();copy.putLong("Run",500);h.assertTrue(a.run()==1,"External NBT mutated live ledger");h.succeed();
    }
    @GameTest(template="empty") public static void questRetirementPreservesHeartsEquipmentAndOtherOwners(GameTestHelper h){
        var owner=UUID.randomUUID();var door=UUID.randomUUID();var heart=new ItemStack(RebirthItems.VOID_HEART.get(),4);var gear=new ItemStack(Items.DIAMOND_SWORD);
        var original=gear.save(new CompoundTag());h.assertFalse(com.coraline.feast.MainStoryRebirth.retireStack(heart,owner,door,0),"Void Hearts retired");
        h.assertTrue(heart.getCount()==4,"Heart count changed");h.assertFalse(com.coraline.feast.MainStoryRebirth.retireStack(gear,owner,door,0),"Permanent equipment retired");h.assertTrue(original.equals(gear.save(new CompoundTag())),"Equipment tags changed");
        var key=new ItemStack(com.coraline.registry.ModItems.LITTLE_DOOR_STORY_KEY.get());key.getOrCreateTag().putUUID("StoryKeyOwner",UUID.randomUUID());key.getTag().putUUID("StoryKeyDoor",door);
        h.assertFalse(com.coraline.feast.MainStoryRebirth.retireStack(key,owner,door,0),"Other owner's key removed");key.getTag().putUUID("StoryKeyOwner",owner);
        h.assertTrue(com.coraline.feast.MainStoryRebirth.retireStack(key,owner,door,0)&&key.isEmpty(),"Old original key remained spendable");h.succeed();
    }
    @GameTest(template="empty") public static void nestedQuestCleanupRetainsOrdinaryStorageAndResources(GameTestHelper h){
        var owner=UUID.randomUUID();var door=UUID.randomUUID();var chest=new ItemStack(Items.SHULKER_BOX);var list=new ListTag();
        var key=new ItemStack(com.coraline.registry.ModItems.LITTLE_DOOR_STORY_KEY.get());key.getOrCreateTag().putUUID("StoryKeyOwner",owner);key.getTag().putUUID("StoryKeyDoor",door);
        var a=key.save(new CompoundTag());a.putByte("Slot",(byte)0);list.add(a);var b=new ItemStack(Items.DIAMOND,23).save(new CompoundTag());b.putByte("Slot",(byte)1);list.add(b);
        chest.getOrCreateTagElement("BlockEntityTag").put("Items",list);h.assertTrue(com.coraline.feast.MainStoryRebirth.retireStack(chest,owner,door,0),"Nested quest not retired");
        var remaining=chest.getTagElement("BlockEntityTag").getList("Items",10);h.assertTrue(!chest.isEmpty()&&remaining.size()==1&&ItemStack.of(remaining.getCompound(0)).getCount()==23,"Ordinary stored resources deleted");h.succeed();
    }
    @GameTest(template="empty") public static void defaultRelationshipAndSecretsHaveNoPriorRunAbilities(GameTestHelper h){
        var loop=new com.coraline.feast.OtherMotherLoopState();h.assertTrue(loop.trust==0&&loop.suspicion==0&&loop.visits==0,"Baseline relationship guessed incorrectly");
        h.assertTrue(loop.secrets.journal.isEmpty()&&loop.secrets.issued.isEmpty()&&loop.secrets.blackmail.isEmpty(),"Fresh secrets inherited capabilities");
        h.assertFalse(com.coraline.rebirth.StoryRebirth.canonicalCompletion(null),"Missing ending treated complete");h.succeed();
    }
    @GameTest(template="empty") public static void canonicalEndingRequiresEveryEpilogueAndMatchingDisposedOriginalReceipt(GameTestHelper h){
        var s=new com.coraline.feast.VisitData.Scene();s.owner=UUID.randomUUID();s.door=UUID.randomUUID();
        var rescue=new CompoundTag();var end=new CompoundTag();end.putBoolean("Started",true);end.putString("Phase","COMPLETE");
        for(String key:new String[]{"PermanentlySealed","ParentsReturned","FarewellComplete","FarewellCatDeparted","HandSevered"})end.putBoolean(key,true);
        rescue.put("Continuation",end);rescue.putBoolean("DoorLocked",true);var eyes=new CompoundTag();eyes.putBoolean("Released",true);rescue.put("GhostEyes",eyes);s.loop.story.put("ParentRescue",rescue);
        var original=new ItemStack(com.coraline.registry.ModItems.LITTLE_DOOR_STORY_KEY.get());var tag=original.getOrCreateTag();tag.putUUID("StoryKeyOwner",s.owner);tag.putUUID("StoryKeyDoor",s.door);tag.putUUID("PalaceKeyIdentity",UUID.randomUUID());
        var tx=com.coraline.palace.PalaceKeyTransaction.prepare(s.owner,s.door,UUID.randomUUID(),original.save(new CompoundTag())).orElseThrow();
        tx.removedAndSaved(s.owner,tx.receipt());tx.dispose(s.owner,tx.receipt(),tx.bundle());
        var well=com.coraline.palace.PalaceWellProgress.start(s.owner,s.door,UUID.randomUUID(),UUID.randomUUID(),false,com.coraline.feast.PalaceJourney.prerequisites(s)).orElseThrow().save();
        well.putString("Phase","COMPLETE");well.putInt("RescueTicks",100);well.putUUID("BundleReceipt",tx.receipt());
        var root=new CompoundTag();root.putInt("Version",1);root.putUUID("Owner",s.owner);root.putUUID("Door",s.door);root.put("Well",well);root.put("KeyCustody",tx.save());s.loop.story.put("PinkPalaceJourney",root);
        h.assertTrue(StoryRebirth.canonicalCompletion(s),"Complete valid ending rejected");
        for(String key:new String[]{"Started","PermanentlySealed","ParentsReturned","FarewellComplete","FarewellCatDeparted","HandSevered"}){end.putBoolean(key,false);h.assertFalse(StoryRebirth.canonicalCompletion(s),"Skipped epilogue gate "+key);end.putBoolean(key,true);}
        eyes.putBoolean("Released",false);h.assertFalse(StoryRebirth.canonicalCompletion(s),"Unfreed ghosts awarded Heart");eyes.putBoolean("Released",true);
        well.putString("Phase","DISPOSED");h.assertFalse(StoryRebirth.canonicalCompletion(s),"Disposal alone skipped ending");well.putString("Phase","COMPLETE");
        well.putUUID("BundleReceipt",UUID.randomUUID());h.assertFalse(StoryRebirth.canonicalCompletion(s),"Mismatched key receipt awarded Heart");h.succeed();
    }
    @GameTest(template="empty") public static void committedLocationAndSacrificeTombstonesCannotBeSilentlyCorrupted(GameTestHelper h){
        var owner=UUID.randomUUID();var run=committed(owner);var saved=run.save();
        var bad=saved.copy();bad.getCompound("Ritual").putLong("Center",new net.minecraft.core.BlockPos(0,500,0).asLong());
        rejected(()->StoryRun.load(owner,bad),h,"Corrupt bedroom height accepted");
        var deaths=saved.copy();deaths.putString("SacrificedParents","corrupt");rejected(()->StoryRun.load(owner,deaths),h,"Corrupt tombstones silently erased");
        var session=run.session();run.advance(session,SUMMONING,BINDING);run.advance(session,BINDING,FIRST_THREAD);run.cut(owner,session,true);run.recordDeath(session,true);
        var mother=run.id("Mother");run=StoryRun.load(owner,run.save());h.assertTrue(run.tombstoned(mother),"First death lost durable identity");h.assertFalse(run.tombstoned(run.id("Father")),"Surviving parent marked dead");h.succeed();
    }
    @GameTest(template="empty") public static void atomicJournalRoundTripsThroughActualWorldFile(GameTestHelper h){
        var server=h.getLevel().getServer();var owner=UUID.randomUUID();var run=complete(owner);
        try{StoryRunStore.save(server,run);var path=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/coraline_story_runs/"+owner+".dat");
            var actual=StoryRun.load(owner,StoryRunStore.read(path));h.assertTrue(actual.save().equals(run.save()),"Actual atomic journal read-back differs");
            var copy=StoryRunStore.get(server,owner);h.assertTrue(copy.claimed()&&copy.earned()==1,"Durable reward receipt changed");
            var status=StoryRunStore.status(server,owner);h.assertTrue(status.run()==1&&status.complete()&&status.claimed()&&!status.active(),"Cached scalar status differs from durable journal");
        }catch(java.io.IOException bad){throw new IllegalStateException(bad);}h.succeed();
    }
    @GameTest(template="empty") public static void failedPlayerCheckpointRollsBackUnexposedHeartAndRetainsReceipt(GameTestHelper h){
        var owner=UUID.randomUUID();var p=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(owner,"rebirth_io_fixture"));
        p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(Items.DIAMOND,7));var old=UUID.randomUUID();
        var persisted=p.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);persisted.putUUID("LastVoidHeartReceipt",old);p.getPersistentData().put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG,persisted);
        var run=StoryRun.first(owner);run.finish(true);
        try{
            StoryRunStore.save(p.server,run);var method=StoryRebirth.class.getDeclaredMethod("reward",net.minecraft.server.level.ServerPlayer.class,StoryRun.class);method.setAccessible(true);
            for(int attempt=0;attempt<2;attempt++){
                boolean failed=false;try{method.invoke(null,p,run);}catch(java.lang.reflect.InvocationTargetException expected){failed=true;}
                h.assertTrue(failed,"A FakePlayer with no real player-data checkpoint unexpectedly committed a payout");
                h.assertTrue(p.getInventory().countItem(RebirthItems.VOID_HEART.get())==0&&p.getInventory().countItem(Items.DIAMOND)==7,"Failed checkpoint exposed a Heart or erased unrelated inventory");
                h.assertTrue(old.equals(p.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG).getUUID("LastVoidHeartReceipt")),"Failure spent/overwrote the previous receipt");
                h.assertTrue(StoryRunStore.get(p.server,owner).rewardPending()&&!run.claimed(),"Failure acknowledged an undelivered reward");
            }
        }catch(ReflectiveOperationException|java.io.IOException bad){throw new IllegalStateException(bad);}h.succeed();
    }
}
