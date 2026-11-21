package com.coraline.feast;

import com.coraline.otherworld.OtherWorld;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class FirstEscapeStoryGameTests {
    @GameTest(template="empty") public static void missingParentsSpeechSurvivesOrdinaryOverworldAudioCleanup(GameTestHelper h){for(char c='a';c<='g';c++)h.assertTrue(VisitAudio.overworldStoryVoice("missing_parents_"+c),"Overworld cleanup would stop Missing Parents "+c);for(String legacy:List.of("wybie_visit_a","bobinsky_visit_b","tea_visit_c"))h.assertTrue(VisitAudio.overworldStoryVoice(legacy),"Existing neighbor speech lost its cleanup exemption");h.assertFalse(VisitAudio.overworldStoryVoice("ambient_theatre"),"Unrelated ambient music escaped cleanup");h.succeed();}
    @GameTest(template="empty",timeoutTicks=110) public static void canonicalMotherIsConcealedAndSeatedBeforeRoomLocks(GameTestHelper h){var p=fixture(h);var s=OtherMotherLoop.scene(p);var l=p.serverLevel();var n=FirstEscapeStory.data(s);n.putBoolean("Started",true);n.putLong("House",p.blockPosition().asLong());n.putLong("LittleDoor",p.blockPosition().west(20).asLong());var center=new net.minecraft.world.level.ChunkPos(p.blockPosition());for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)l.setChunkForced(center.x+x,center.z+z,true);var realm=new com.coraline.otherworld.RealmData.Realm();realm.door=s.door;realm.house=p.blockPosition();realm.doorPos=p.blockPosition();realm.table=s.table;realm.piano=s.table;realm.ready=true;com.coraline.otherworld.RealmData.get(p.server).realms.put(s.door,realm);h.runAtTickTime(30,()->{var mother=com.coraline.registry.ModEntities.MOM.get().create(l);mother.makeOtherParent(0);mother.setNoAi(true);mother.getPersistentData().putUUID("RealmDoor",s.door);mother.getPersistentData().putBoolean("CoralineDecoration",true);mother.setPos(p.position());h.assertTrue(l.addFreshEntity(mother),"Canonical Mother fixture rejected");s.mom=mother.getUUID();});h.runAtTickTime(70,()->{try{FirstEscapeStory.go(p,s,FirstEscapeTimeline.Phase.ROOM_DARK);var mother=l.getEntity(s.mom);var couch=FirstEscapeScene.prop(p,s,"BugCouch");h.assertTrue(mother!=null&&couch!=null,"Story actors failed to become live in warmed chunks");h.assertTrue(mother.isInvisible()&&mother.getVehicle()==couch,"Mother was not staged invisibly on the actual couch");FirstEscapeStory.go(p,s,FirstEscapeTimeline.Phase.ROOM_TRANSFORMING);h.assertTrue(n.getBoolean("RoomLocked")&&mother.getVehicle()==couch,"Room lock stranded Mother outside her couch");FirstEscapeScene.restore(p,s);mother.stopRiding();mother.discard();}finally{VisitData.get(p.server).scenes.remove(s.owner);com.coraline.otherworld.RealmData.get(p.server).realms.remove(s.door);for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)l.setChunkForced(center.x+x,center.z+z,false);}h.succeed();});}
    @GameTest(template="empty") public static void decayedCorridorIsCrawlSizedClearAndFacesActualDoor(GameTestHelper h){var p=fixture(h);var s=OtherMotherLoop.scene(p);var l=p.serverLevel();var c=new BlockPos(107232,220,107232);try{var n=FirstEscapeStory.data(s);n.putLong("Prison",c.asLong());n.putLong("LittleDoor",c.below(40).asLong());l.getChunkAt(c);l.setBlock(c.below(40),com.coraline.registry.ModBlocks.TINY_DOOR.get().defaultBlockState().setValue(com.coraline.block.TinyDoorBlock.FACING,net.minecraft.core.Direction.EAST),2);FirstEscapeStory.ensureDecayedTunnel(p,s);h.assertTrue(FirstEscapeStory.escapeDirection(s)==net.minecraft.core.Direction.WEST,"Corridor rotates player away from the actual doorway");for(int along=1;along<=21;along++){var cell=FirstEscapeStory.tunnelCell(s,0,0,along);var at=net.minecraft.world.phys.Vec3.atBottomCenterOf(cell);h.assertTrue(l.noCollision(p,p.getDimensions(net.minecraft.world.entity.Pose.SWIMMING).makeBoundingBox(at)),"Crawl route blocked at "+along);h.assertFalse(l.noCollision(p,p.getDimensions(net.minecraft.world.entity.Pose.STANDING).makeBoundingBox(at)),"Corridor is standing-height instead of crawl-sized");h.assertTrue(l.getBlockState(cell.below()).isFaceSturdy(l,cell.below(),net.minecraft.core.Direction.UP),"Missing tunnel floor");}FirstEscapeScene.restore(p,s);l.removeBlock(c.below(40),false);}finally{VisitData.get(p.server).scenes.remove(s.owner);}h.succeed();}
    @GameTest(template="empty",timeoutTicks=110) public static void realmProtectionCannotConsumeRescuePunchOrHeadClick(GameTestHelper h){var p=fixture(h);var s=OtherMotherLoop.scene(p);var n=FirstEscapeStory.data(s);n.putBoolean("Started",true);n.putString("Phase","ESCAPE_A");n.putUUID("Choice",UUID.randomUUID());var chunk=new net.minecraft.world.level.ChunkPos(p.blockPosition());p.serverLevel().setChunkForced(chunk.x,chunk.z,true);p.serverLevel().getChunkAt(p.blockPosition());h.runAtTickTime(30,()->{var actor=ShowActors.acquire(p,s,"other_wybie",p.serverLevel(),p.position());h.assertTrue(actor!=null,"Could not lease real Other Wybie");});h.runAtTickTime(70,()->{try{var w=ShowActors.actor(p,s,"other_wybie");h.assertTrue(w!=null,"Recorded Wybie UUID never became live");p.setPos(w.position().add(1,0,0));float health=((net.minecraft.world.entity.LivingEntity)w).getHealth();var hit=new net.minecraftforge.event.entity.player.AttackEntityEvent(p,w);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(hit);h.assertTrue(hit.isCanceled()&&n.getBoolean("Punched"),"Protection consumed punch before rescue handler");h.assertTrue(((net.minecraft.world.entity.LivingEntity)w).getHealth()==health,"Rescue punch caused lethal gameplay damage");n.putString("Phase","MASK_REMOVAL");var body=new net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific(p,net.minecraft.world.InteractionHand.MAIN_HAND,w,new net.minecraft.world.phys.Vec3(0,.2,0));net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(body);h.assertTrue(FirstEscapeStory.phase(s)==FirstEscapeTimeline.Phase.MASK_REMOVAL,"Clicking a boot removed the mask");var head=new net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific(p,net.minecraft.world.InteractionHand.MAIN_HAND,w,new net.minecraft.world.phys.Vec3(0,w.getBbHeight()*.9,0));net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(head);h.assertTrue(head.isCanceled()&&FirstEscapeStory.phase(s)==FirstEscapeTimeline.Phase.MASK_ANIMATION,"Head click did not reach rescue interaction before realm protection");w.discard();}finally{VisitData.get(p.server).scenes.remove(s.owner);p.serverLevel().setChunkForced(chunk.x,chunk.z,false);}h.succeed();});}
    /** The shoe throw is an OVERWORLD checkpoint. Do not accidentally run its
     * owner+dimension authorization test using the prison/Other World fixture. */
    private static net.minecraft.server.level.ServerPlayer overworldFixture(GameTestHelper h){
        var l=h.getLevel().getServer().overworld();
        var p=new net.minecraft.server.level.ServerPlayer(l.getServer(),l,
                new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ShoeRetryQA"));
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();
        s.stage=VisitData.Stage.DONE;s.table=h.absolutePos(new BlockPos(1,2,1));
        s.loop.event.putInt("Serial",2);VisitData.get(p.server).scenes.put(s.owner,s);
        p.getPersistentData().putUUID("CoralineVisitDoor",s.door);
        p.setPos(s.table.getCenter());return p;
    }
    private static net.minecraft.server.level.ServerPlayer fixture(GameTestHelper h){var l=h.getLevel().getServer().getLevel(OtherWorld.KEY);var p=new net.minecraft.server.level.ServerPlayer(l.getServer(),l,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"FirstEscapeQA"));p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.table=new BlockPos(107200,180,107200);s.loop.event.putInt("Serial",2);VisitData.get(p.server).scenes.put(s.owner,s);p.getPersistentData().putUUID("CoralineVisitDoor",s.door);p.setPos(s.table.getCenter());return p;}
    @GameTest(template="empty") public static void missedShoePreservesTheSingleOriginalForRethrow(GameTestHelper h){
        var p=overworldFixture(h);var s=OtherMotherLoop.scene(p);CinematicPropEntity shoe=null;
        try{
            var n=FirstEscapeStory.data(s);n.putBoolean("Started",true);n.putUUID("Token",UUID.randomUUID());
            n.putString("Phase",FirstEscapeTimeline.Phase.THROW_SHOE.name());
            shoe=FirstEscapeScene.ensure(p,s,"ShoeFlight",CinematicPropEntity.STORY_SHOE,0,p.serverLevel(),p.position());
            h.assertTrue(shoe!=null&&n.hasUUID("ShoeFlight"),"Original airborne shoe not staged");
            var id=shoe.getUUID();
            // A fall or wall collision is a miss, not permission to skip the shoe scene.
            shoe.setPos(p.position().add(0,-15,0));shoe.setDeltaMovement(Vec3.ZERO);
            FirstEscapeStory.shoe(p,s);
            h.assertTrue(FirstEscapeStory.phase(s)==FirstEscapeTimeline.Phase.THROW_SHOE,
                    "Missing Wybie incorrectly advanced the story");
            h.assertTrue(shoe.cue()==1&&n.getBoolean("ShoeMissed")&&n.getUUID("ShoeFlight").equals(id),
                    "Miss did not retain the single authentic projectile");
            // Even if the player moves away after a save/reload, the same shoe
            // must return within reach without minting a new bound inventory item.
            shoe.setPos(p.position().add(30,0,0));shoe.frame(1,80,0);
            FirstEscapeStory.shoe(p,s);
            h.assertTrue(shoe.distanceToSqr(p)<16&&shoe.getUUID().equals(id),
                    "Unreachable original shoe did not recover to the owner");
            h.assertTrue(FirstEscapeStory.interact(p,shoe),"Original missed shoe cannot be used again");
            h.assertTrue(shoe.cue()==0&&shoe.getDeltaMovement().lengthSqr()>0
                    &&n.getUUID("ShoeFlight").equals(id)&&!n.getBoolean("ShoeMissed"),
                    "Retry spawned a new shoe or did not relaunch the saved one");
            for(int i=0;i<p.getInventory().getContainerSize();i++)
                h.assertFalse(FirstEscapeStory.validTool(s,p.getInventory().getItem(i),true),
                        "Retry duplicated the spent co-op custody-tracked shoe");
        }finally{
            if(shoe!=null)shoe.discard();
            VisitData.get(p.server).scenes.remove(s.owner);
        }
        h.succeed();
    }

    @GameTest(template="empty") public static void shoeCanBeRetriedWithoutClickingTinyGroundHitbox(GameTestHelper h){
        var p=overworldFixture(h);var s=OtherMotherLoop.scene(p);CinematicPropEntity shoe=null;
        try{
            var n=FirstEscapeStory.data(s);n.putBoolean("Started",true);
            n.putUUID("Token",UUID.randomUUID());
            n.putString("Phase",FirstEscapeTimeline.Phase.THROW_SHOE.name());
            var choice=UUID.randomUUID();n.putUUID("Choice",choice);
            shoe=FirstEscapeScene.ensure(p,s,"ShoeFlight",
                    CinematicPropEntity.STORY_SHOE,0,p.serverLevel(),p.position());
            h.assertTrue(shoe!=null,"Original physical shoe could not be prepared");
            var original=shoe.getUUID();
            shoe.setPos(p.position().add(0,-15,0));
            FirstEscapeStory.shoe(p,s);
            h.assertTrue(n.getBoolean("ShoeMissed")&&shoe.cue()==1,"Miss did not enter retry state");
            h.assertFalse(FirstEscapeStory.retryShoe(p,UUID.randomUUID()),"Foreign story nonce relaunched the shoe");
            h.assertTrue(FirstEscapeStory.retryShoe(p,choice),"Use-input failed to rethrow the same shoe");
            h.assertTrue(shoe.getUUID().equals(original)&&shoe.cue()==0&&!n.getBoolean("ShoeMissed"),
                    "Retry failed to preserve the same entity identity or active flight");
            h.assertFalse(FirstEscapeStory.retryShoe(p,choice),"Repeated input relaunched a shoe already flying");
            // A second miss must be just as retryable as the first.
            shoe.setPos(p.position().add(0,-15,0));
            FirstEscapeStory.shoe(p,s);
            h.assertTrue(n.getBoolean("ShoeMissed"),"Second miss advanced or permanently consumed the shoe");
            h.assertTrue(FirstEscapeStory.retryShoe(p,choice),"Second use-input could not recover the original shoe");
            h.assertTrue(shoe.getUUID().equals(original)&&FirstEscapeStory.phase(s)==FirstEscapeTimeline.Phase.THROW_SHOE,
                    "Retry duplicated the object or silently advanced the story");
            // The old five-second airborne timeout looked like a stuck shoe.
            // Time and distance bounds must each make the saved one retryable.
            shoe.frame(0,45,0);shoe.setDeltaMovement(Vec3.ZERO);
            shoe.setPos(p.position().add(0,2,0));FirstEscapeStory.shoe(p,s);
            h.assertTrue(n.getBoolean("ShoeMissed")&&shoe.cue()==1,
                    "Open-air miss did not recover at the shorter flight cap");
            h.assertTrue(FirstEscapeStory.retryShoe(p,choice),"Timed-out shoe did not rethrow");
            shoe.frame(0,0,0);shoe.setDeltaMovement(Vec3.ZERO);
            shoe.setPos(p.position().add(30,0,0));FirstEscapeStory.shoe(p,s);
            h.assertTrue(n.getBoolean("ShoeMissed")&&shoe.cue()==1,
                    "Far missed shoe did not return to retry state");
            h.assertTrue(FirstEscapeStory.retryShoe(p,choice),"Distant shoe did not rethrow");
            h.assertTrue(shoe.getUUID().equals(original),"Flight recovery duplicated the original entity");
        }finally{
            if(shoe!=null)shoe.discard();
            VisitData.get(p.server).scenes.remove(s.owner);
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void allOriginalRecordingsAndCueOffsets(GameTestHelper h){
        var sounds=new HashSet<String>();for(var phase:FirstEscapeTimeline.Phase.values()){var sound=FirstEscapeTimeline.sound(phase);if(!sound.isEmpty()){h.assertTrue(sounds.add(sound),"Duplicate recording phase");h.assertTrue(ShowRecordings.duration(sound)>0,"Missing original "+sound);}}
        h.assertTrue(sounds.size()==26,"Expected all 26 original recordings");
        for(var entry:Map.of("cocoa_beetles_e",40,"cocoa_beetles_g",10,"mirror_prison_c",60,"mirror_prison_d",100,"first_escape_b",40,"first_escape_c",20,"first_escape_d",40,"first_escape_f",40).entrySet())h.assertTrue(ShowRecordings.duration(entry.getKey())>entry.getValue(),"Recording too short for exact cue "+entry.getKey());h.succeed();
    }
    @GameTest(template="empty") public static void interactiveCheckpointsCannotAdvanceOnTimer(GameTestHelper h){
        for(var phase:List.of(FirstEscapeTimeline.Phase.FIND_CANE,FirstEscapeTimeline.Phase.CANE_COLLECTED,FirstEscapeTimeline.Phase.APOLOGY_CHOICE,FirstEscapeTimeline.Phase.MASK_REMOVAL,FirstEscapeTimeline.Phase.STITCH_REMOVAL,FirstEscapeTimeline.Phase.PUSH_WARDROBE,FirstEscapeTimeline.Phase.OPEN_LITTLE_DOOR,FirstEscapeTimeline.Phase.WYBIE_GAZE,FirstEscapeTimeline.Phase.SHOW_DOOR,FirstEscapeTimeline.Phase.THROW_SHOE))h.assertTrue(FirstEscapeTimeline.after(phase)==null,"Interaction bypass "+phase);
        h.assertTrue(FirstEscapeTimeline.after(FirstEscapeTimeline.Phase.COCOA_D)==FirstEscapeTimeline.Phase.APOLOGY_CHOICE,"D skipped apology");h.assertTrue(FirstEscapeTimeline.after(FirstEscapeTimeline.Phase.MISSING_D)==FirstEscapeTimeline.Phase.THROW_SHOE,"D incorrectly requires finding the absent doll");h.succeed();
    }
    @GameTest(template="empty") public static void apologyRejectIsOwnerNonceBoundAndAcceptIsNoOp(GameTestHelper h){
        var p=fixture(h);var s=OtherMotherLoop.scene(p);try{var n=FirstEscapeStory.data(s);n.putBoolean("Started",true);n.putString("Phase","APOLOGY_CHOICE");var nonce=UUID.randomUUID();n.putUUID("Choice",nonce);
            h.assertFalse(FirstEscapeStory.choice(p,UUID.randomUUID(),"escape:reject"),"Forged nonce accepted");h.assertFalse(FirstEscapeStory.choice(p,nonce,"escape:apologize"),"Incomplete acceptance fixture incorrectly advanced without original-story prerequisites");h.assertTrue(FirstEscapeStory.phase(s)==FirstEscapeTimeline.Phase.APOLOGY_CHOICE,"Accept implemented an unrequested branch");h.assertTrue(FirstEscapeStory.choice(p,nonce,"escape:reject"),"Real reject failed");h.assertTrue(FirstEscapeStory.phase(s)==FirstEscapeTimeline.Phase.COCOA_E,"Reject skipped E");h.assertFalse(FirstEscapeStory.choice(p,nonce,"escape:reject"),"Replay of choice advanced again");
        }finally{VisitData.get(p.server).scenes.remove(s.owner);}h.succeed();
    }
    @GameTest(template="empty") public static void toolsRequireExactOwnerAndStoryToken(GameTestHelper h){
        var p=fixture(h);var s=OtherMotherLoop.scene(p);try{var n=FirstEscapeStory.data(s);n.putBoolean("Started",true);n.putUUID("Token",UUID.randomUUID());var key=new ItemStack(FirstEscapeItems.CANE.get());h.assertFalse(FirstEscapeStory.validTool(s,key,false),"Unbound cane accepted");key.getOrCreateTag().putUUID("StoryOwner",s.owner);key.getTag().putUUID("StoryToken",n.getUUID("Token"));h.assertTrue(FirstEscapeStory.validTool(s,key,false),"Canonical cane rejected");h.assertFalse(FirstEscapeStory.validTool(s,key,true),"Cane was accepted as shoe");key.getTag().putUUID("StoryOwner",UUID.randomUUID());h.assertFalse(FirstEscapeStory.validTool(s,key,false),"Another owner's tool accepted");}finally{VisitData.get(p.server).scenes.remove(s.owner);}h.succeed();
    }
    @GameTest(template="empty") public static void allStoryPropsConstructAndWardrobeCollisionFallsAway(GameTestHelper h){
        var p=fixture(h);var s=OtherMotherLoop.scene(p);try{for(int kind=CinematicPropEntity.STORY_CANE;kind<=CinematicPropEntity.STORY_SHOE;kind++){var prop=CinematicPropEntity.TYPE.get().create(p.serverLevel());h.assertTrue(prop!=null,"Story prop constructor failed");prop.configure(s,kind,0);prop.setPos(p.position());h.assertTrue(prop.owned(s),"Prop owner serial lost");if(kind==CinematicPropEntity.ROACH_WARDROBE){h.assertTrue(prop.canBeCollidedWith()&&prop.getBoundingBox().getYsize()>3,"Wardrobe does not block the door");prop.frame(3,20,1);h.assertFalse(prop.canBeCollidedWith(),"Fallen wardrobe still blocks passage");}var saved=new CompoundTag();prop.saveWithoutId(saved);var restored=CinematicPropEntity.TYPE.get().create(p.serverLevel());restored.load(saved);h.assertTrue(restored.kind()==kind&&restored.owned(s),"Prop reload lost identity/cue");}}finally{VisitData.get(p.server).scenes.remove(s.owner);}h.succeed();
    }
    @GameTest(template="empty") public static void sealAndCheckpointPersistWithoutSealingOtherPlayers(GameTestHelper h){
        var p=fixture(h);var s=OtherMotherLoop.scene(p);try{var n=FirstEscapeStory.data(s);n.putBoolean("Started",true);n.putBoolean("OtherWorldSealed",true);n.putString("Phase","ESCAPE_COMPLETE");n.putBoolean("ChaseRewardPaid",true);n.putBoolean("HandMissing",true);var loaded=OtherMotherLoopState.load(s.loop.save());h.assertTrue(loaded.story.getCompound("FirstEscape").getString("Phase").equals("ESCAPE_COMPLETE"),"Checkpoint lost");h.assertTrue(loaded.story.getCompound("FirstEscape").getBoolean("HandMissing"),"Missing hand lost");h.assertTrue(FirstEscapeStory.sealed(p.server,s.owner),"Owner is not sealed");h.assertFalse(FirstEscapeStory.sealed(p.server,UUID.randomUUID()),"Another player was sealed");}finally{VisitData.get(p.server).scenes.remove(s.owner);}h.succeed();
    }
    @GameTest(template="empty") public static void prisonIsEnclosedAndEnvironmentRestores(GameTestHelper h){
        var p=fixture(h);var s=OtherMotherLoop.scene(p);var c=new BlockPos(107200,220,107200);var l=p.serverLevel();try{FirstEscapeStory.data(s).putLong("Prison",c.asLong());l.getChunkAt(c);var original=l.getBlockState(c.below());FirstEscapeScene.preparePrison(p,s);h.assertTrue(l.getBlockState(c.below()).is(Blocks.DEEPSLATE_BRICKS),"Prison lacks floor");for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL)h.assertTrue(!l.getBlockState(c.relative(direction,4)).isAir(),"Prison is open at "+direction);h.assertTrue(l.getBlockState(c).isAir(),"Prison has no player space");FirstEscapeScene.restore(p,s);h.assertTrue(l.getBlockState(c.below()).equals(original),"Journal did not restore source terrain");}finally{VisitData.get(p.server).scenes.remove(s.owner);}h.succeed();
    }
    @GameTest(template="empty") public static void debuggerIncludesAllFourActsAndInteractionCheckpoints(GameTestHelper h){for(var section:List.of("cocoa","prison","escape","missing")){h.assertTrue(NumberedDebugControls.SECTIONS.contains(section),"Missing debugger section "+section);for(var id:List.of("state","recover","reset","scene"))h.assertTrue(NumberedDebugControls.find(section,id)!=null,"Missing recovery control "+section+"/"+id);}h.assertTrue(NumberedDebugControls.find("escape","phase_mask_removal")!=null,"Missing mask test checkpoint");h.succeed();}
    @GameTest(template="empty") public static void fullCreativeInventoryCannotDeletePorchCane(GameTestHelper h){var p=fixture(h);var s=OtherMotherLoop.scene(p);try{var n=FirstEscapeStory.data(s);n.putBoolean("Started",true);n.putUUID("Token",UUID.randomUUID());n.putString("Phase","FIND_CANE");p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(net.minecraft.world.item.Items.STONE,64));var cane=CinematicPropEntity.TYPE.get().create(p.serverLevel());cane.configure(s,CinematicPropEntity.STORY_CANE,0);cane.setPos(p.position());FirstEscapeStory.interact(p,cane);h.assertTrue(FirstEscapeStory.phase(s)==FirstEscapeTimeline.Phase.FIND_CANE&&!cane.isRemoved(),"Full creative inventory consumed the cane");p.getInventory().setItem(0,ItemStack.EMPTY);FirstEscapeStory.interact(p,cane);h.assertTrue(!cane.isRemoved()&&FirstEscapeStory.phase(s)==FirstEscapeTimeline.Phase.FIND_CANE,"Unregistered fake player incorrectly bypassed native quest-item custody merely by freeing a slot");h.assertFalse(FirstEscapeStory.validTool(s,p.getInventory().getItem(0),false),"Unauthenticated fake player gained an owner-bound cane");}finally{VisitData.get(p.server).scenes.remove(s.owner);}h.succeed();}
    @GameTest(template="empty") public static void all26PlaybackClocksRequireRealSessionAcknowledgements(GameTestHelper h){var p=fixture(h);var s=OtherMotherLoop.scene(p);try{for(var phase:FirstEscapeTimeline.Phase.values()){String sound=FirstEscapeTimeline.sound(phase);if(sound.isEmpty())continue;h.assertTrue(ShowPlayback.start(p,s,sound,false),"Could not start original "+sound);var cursor=ShowPlayback.data(s);var nonce=cursor.getUUID("Nonce");ShowPlayback.signal(p,UUID.randomUUID(),sound,0);h.assertFalse(ShowPlayback.tick(p,s),"Forged playback start advanced "+sound);h.assertTrue(ShowPlayback.position(s)==0,"Loading moved the cue clock");ShowPlayback.signal(p,nonce,sound,1);h.assertFalse(cursor.getBoolean("Ended"),"End accepted before start");ShowPlayback.signal(p,nonce,sound,0);ShowPlayback.signal(p,nonce,sound,1);h.assertFalse(ShowPlayback.tick(p,s),"Early end skipped the original performance");cursor.putInt("Position",cursor.getInt("Duration")-1);h.assertFalse(ShowPlayback.tick(p,s),"Fast server ticks bypassed real elapsed audio");PlaybackFixtureClock.elapseOriginal(s);h.assertTrue(ShowPlayback.tick(p,s),"Measured duration and acknowledged end failed to finish "+sound);ShowPlayback.stop(p,s);}}finally{VisitData.get(p.server).scenes.remove(s.owner);}h.succeed();}
}
