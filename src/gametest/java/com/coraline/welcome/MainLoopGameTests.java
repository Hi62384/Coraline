package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import net.minecraftforge.common.util.FakePlayer;
import java.util.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class MainLoopGameTests {
    private static int fixtureIndex;
    private record Fixture(FakePlayer player,VisitData.Scene scene,RealmData.Realm realm){}
    private static Fixture fixture(GameTestHelper h){
        var server=h.getLevel().getServer();var level=server.getLevel(OtherWorld.KEY);int x=1024+(fixtureIndex++)*128;
        var pos=new BlockPos(x,100,1024);level.setChunkForced(x>>4,1024>>4,true);level.getChunkAt(pos);
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)level.setBlock(pos.offset(dx,-1,dz),Blocks.STONE.defaultBlockState(),3);
        var p=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"MainLoopTest")){
            // Forge's fake connection deliberately ignores same-level teleport packets.
            @Override public void teleportTo(net.minecraft.server.level.ServerLevel destination,double x,double y,double z,float yaw,float pitch){
                super.teleportTo(destination,x,y,z,yaw,pitch);if(serverLevel()==destination)moveTo(x,y,z,yaw,pitch);
            }
        };p.setPos(x+.5,100,1024.5);
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.piano=pos;s.table=pos;s.loop.portal=pos;s.loop.generation=1;
        VisitData.get(server).scenes.put(s.owner,s);p.getPersistentData().putUUID("CoralineVisitDoor",s.door);
        var r=new RealmData.Realm();r.door=s.door;r.doorPos=pos;r.bed=pos;r.house=pos.north(40);r.porch=pos.north(18);r.sourceDoor=pos;r.radius=60;r.ready=true;RealmData.get(server).realms.put(r.door,r);
        var d=new DoorData.Door();d.id=s.door;d.owner=s.owner;DoorData.get(server).doors.put(d.id,d);
        return new Fixture(p,s,r);
    }
    @GameTest(template="empty") public static void relationshipAndSaveRoundTrip(GameTestHelper h){
        var l=new OtherMotherLoopState();l.visits=3;l.trust=.8F;l.suspicion=.2F;l.phase=OtherMotherLoopState.Phase.GIFTS;l.extra=true;l.opened=true;l.generation=4;l.arrivalDeadline=1234;
        l.gifts.add(new ItemStack(Items.DIAMOND,2));l.gifts.add(ItemStack.EMPTY);
        var copy=OtherMotherLoopState.load(l.save());h.assertTrue(copy.visits==3&&copy.generation==4&&copy.opened&&copy.extra&&copy.arrivalDeadline==1234,"Loop flags/timing lost");
        h.assertTrue(copy.gifts.size()==2&&copy.gifts.get(0).getCount()==2&&copy.gifts.get(1).isEmpty(),"Reward ledger changed on load");
        h.assertTrue(OtherMotherLoopState.clamp(-1)==0&&OtherMotherLoopState.clamp(2)==1&&OtherMotherLoopState.clamp(Float.NaN)==0,"Meters not clamped");
        h.assertTrue(OtherMotherLoopState.classify(.8F,.2F)==OtherMotherWelcome.State.HIGH_TRUST_LOW_SUSPICION,"High trust classification wrong");
        h.assertTrue(OtherMotherLoopState.classify(.2F,.8F)==OtherMotherWelcome.State.HIGH_SUSPICION_LOW_TRUST,"Suspicion classification wrong");
        h.assertTrue(OtherMotherLoopState.classify(.5F,.5F)==OtherMotherWelcome.State.MID_TRUST_MID_SUSPICION,"Middle classification wrong");h.succeed();
    }
    @GameTest(template="empty") public static void startsOnSecondVisitAndPreservesFirstStory(GameTestHelper h){
        var f=fixture(h);var s=f.scene;s.stage=VisitData.Stage.MOTHER_INTRO;
        h.assertFalse(OtherMotherLoop.entered(f.player,f.realm),"Hijacked first visit");s.stage=VisitData.Stage.DONE;
        h.assertTrue(OtherMotherLoop.entered(f.player,f.realm),"Second visit did not start");
        h.assertTrue(s.loop.phase==OtherMotherLoopState.Phase.WELCOME&&OtherMotherLoop.locked(f.player),"Welcome unlocked prematurely");h.succeed();
    }
    @GameTest(template="empty") public static void giftPickupIsOwnedAndNeverDuplicates(GameTestHelper h){
        var f=fixture(h);var s=f.scene;s.loop.phase=OtherMotherLoopState.Phase.GIFTS;s.loop.loot=OtherMotherDialogue.Loot.FOOD;s.loop.gifts.add(new ItemStack(Items.APPLE,3));
        var prop=LoopProps.spawn(f.player.serverLevel(),s,ScenePropEntity.PLATE,f.player.position(),0);
        LoopProps.interact(f.player,prop);LoopProps.interact(f.player,prop);
        h.assertTrue(f.player.getInventory().countItem(Items.APPLE)==3&&s.loop.collected(),"Repeated click duplicated reward");
        s.loop.gifts.set(0,new ItemStack(Items.DIAMOND,8));for(int i=0;i<36;i++)f.player.getInventory().setItem(i,new ItemStack(Items.STONE,64));
        LoopProps.interact(f.player,prop);h.assertTrue(s.loop.gifts.get(0).getCount()==8,"Full inventory destroyed gifts");
        prop.owner=UUID.randomUUID();LoopProps.interact(f.player,prop);h.assertTrue(s.loop.gifts.get(0).getCount()==8,"Foreign player could claim gift");h.succeed();
    }
    @GameTest(template="empty") public static void arrivalDeadlineAndLocks(GameTestHelper h){
        var f=fixture(h);var l=f.scene.loop;l.phase=OtherMotherLoopState.Phase.WINDOW;l.arrivalDeadline=OtherMotherLoop.now(f.player)+OtherMotherLoop.ATTENDANCE_TICKS;
        h.assertTrue(OtherMotherLoop.ATTENDANCE_TICKS==6000,"Production relaxation period is not five minutes");h.assertFalse(OtherMotherLoop.locked(f.player),"Free time blocked exit");
        tick(f.player);h.assertTrue(l.phase==OtherMotherLoopState.Phase.ARRIVAL&&!l.late,"On-time proximity arrival wrong");h.assertTrue(OtherMotherLoop.locked(f.player),"Arrival failed to lock exit");
        l.phase=OtherMotherLoopState.Phase.WINDOW;l.arrivalDeadline=OtherMotherLoop.now(f.player)+2401;tick(f.player);
        h.assertTrue(!l.late&&l.phase==OtherMotherLoopState.Phase.ARRIVAL,"First three minutes must remain on-time");
        l.phase=OtherMotherLoopState.Phase.WINDOW;l.arrivalDeadline=OtherMotherLoop.now(f.player)+2400;tick(f.player);
        h.assertTrue(l.late&&l.phase==OtherMotherLoopState.Phase.ARRIVAL,"Last two minutes must count as late");
        l.phase=OtherMotherLoopState.Phase.WINDOW;l.arrivalDeadline=OtherMotherLoop.now(f.player);tick(f.player);
        h.assertTrue(l.phase==OtherMotherLoopState.Phase.ARRIVAL&&l.late,"Expired invitation must still admit a late arrival");h.succeed();
    }
    @GameTest(template="empty") public static void departureClosesNightAndResetsNextVisit(GameTestHelper h){
        var f=fixture(h);var s=f.scene;s.loop.phase=OtherMotherLoopState.Phase.WINDOW;s.loop.visits=3;
        var door=DoorData.get(f.player.server).doors.get(s.door);long day=Math.floorDiv(f.player.server.overworld().getDayTime(),24000);
        OtherMotherLoop.depart(f.player,false);h.assertTrue(door.closedNight==day&&door.activeUntil==0&&!s.loop.active(),"Departure failed to end night");
        var saved=DoorData.load(DoorData.get(f.player.server).save(new CompoundTag()));h.assertTrue(saved.doors.get(s.door).closedNight==day,"Night lock did not persist");
        OtherMotherLoop.entered(f.player,f.realm);h.assertTrue(s.loop.phase==OtherMotherLoopState.Phase.WELCOME&&s.loop.gifts.isEmpty(),"New visit did not restart welcome");h.succeed();
    }
    @GameTest(template="empty") public static void activityDimensionAndProtection(GameTestHelper h){
        var server=h.getLevel().getServer();var level=server.getLevel(ActivityWorld.KEY);h.assertTrue(level!=null,"Activity dimension missing");
        var p=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ArenaTest"));
        var event=new net.minecraftforge.event.level.BlockEvent.BreakEvent(level,BlockPos.ZERO,Blocks.STONE.defaultBlockState(),p);RealmProtection.breakBlock(event);h.assertTrue(event.isCanceled(),"Activity blocks breakable");
        var sheep=net.minecraft.world.entity.EntityType.SHEEP.create(level);var join=new net.minecraftforge.event.entity.EntityJoinLevelEvent(sheep,level);RealmProtection.entityJoin(join);h.assertTrue(join.isCanceled(),"Vanilla mobs allowed into activity arena");
        h.assertFalse(ActivityWorld.protectedWorld(server.overworld()),"Overworld unexpectedly protected");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200) public static void activityRoundTripContinuesTheLoop(GameTestHelper h){
        var f=fixture(h);var p=f.player;var s=f.scene;var level=p.serverLevel();
        var mom=ModEntities.MOM.get().create(level);mom.makeOtherParent(0);mom.setPos(p.position());mom.setNoAi(true);level.addFreshEntity(mom);s.mom=mom.getUUID();s.loop.motherPos=mom.blockPosition();
        h.runAfterDelay(5,()->{
            s.loop.phase=OtherMotherLoopState.Phase.PROPOSAL;
            finishAudio(p,s,h,OtherMotherLoopState.Phase.PROPOSAL);
            h.assertTrue(s.loop.phase==OtherMotherLoopState.Phase.WARNING,"Proposal did not lead to warning");
            finishAudio(p,s,h,OtherMotherLoopState.Phase.WARNING);
            h.assertTrue(s.loop.phase==OtherMotherLoopState.Phase.WINDOW,"Warning did not open attendance window");
            h.assertTrue(s.loop.arrivalDeadline==OtherMotherLoop.now(p)+6000,"Timer did not start after warning finished");
            var portal=s.loop.portal;p.setPos(portal.getX()+.5,portal.getY(),portal.getZ()-1.5);tick(p);
            h.assertTrue(s.loop.phase==OtherMotherLoopState.Phase.ARRIVAL,"No arrival greeting");
            ActivityWorld.enter(p,s);h.assertTrue(OtherWorld.isOther(p.level()),"Portal bypassed greeting");
            finishAudio(p,s,h,OtherMotherLoopState.Phase.ARRIVAL);
            h.assertTrue(s.loop.phase==OtherMotherLoopState.Phase.READY,"Greeting did not unlock portal");
            ActivityWorld.enter(p,s);h.assertTrue(ActivityWorld.isActivity(p.level())&&s.loop.phase==OtherMotherLoopState.Phase.ACTIVITY,"Activity transfer failed");
            h.assertTrue(!p.level().getBlockState(p.blockPosition().below()).getCollisionShape(p.level(),p.blockPosition().below()).isEmpty(),"Activity arrival has no safe floor");
            ActivityWorld.exit(p,s);h.assertTrue(OtherWorld.isOther(p.level())&&s.loop.phase==OtherMotherLoopState.Phase.COMPLETE,"Activity return failed");
            h.assertTrue(p.distanceToSqr(portal.getCenter())<4,"Returned to wrong portal position");
            finishAudio(p,s,h,OtherMotherLoopState.Phase.COMPLETE);
            h.assertTrue(s.loop.phase==OtherMotherLoopState.Phase.PROPOSAL,"Completion did not start another activity");h.succeed();
        });
    }
    private static void finishAudio(FakePlayer p,VisitData.Scene s,GameTestHelper h,OtherMotherLoopState.Phase expected){
        tick(p);h.assertTrue(s.loop.phase==expected&&s.loop.deadline>OtherMotherLoop.now(p),"Audio did not hold phase "+expected);
        s.loop.deadline=OtherMotherLoop.now(p);s.welcomeAfter=0;tick(p);
    }
    @GameTest(template="empty",timeoutTicks=200) public static void welcomeWaitsForEveryGiftBeforeTutorial(GameTestHelper h){
        var f=fixture(h);var p=f.player;var s=f.scene;
        var mom=ModEntities.MOM.get().create(p.serverLevel());mom.makeOtherParent(0);mom.setNoAi(true);mom.setPos(p.position());p.serverLevel().addFreshEntity(mom);s.mom=mom.getUUID();
        s.loop.phase=OtherMotherLoopState.Phase.WELCOME;s.loop.extra=true;
        h.runAfterDelay(5,()->{
            finishAudio(p,s,h,OtherMotherLoopState.Phase.WELCOME);
            h.assertTrue(s.loop.phase==OtherMotherLoopState.Phase.BEFORE,"Welcome did not lead to loot before line");
            finishAudio(p,s,h,OtherMotherLoopState.Phase.BEFORE);
            h.assertTrue(s.loop.phase==OtherMotherLoopState.Phase.GIFTS&&s.loop.gifts.size()==15,"Feast not generated after before line");
            tick(p);h.assertTrue(s.loop.phase==OtherMotherLoopState.Phase.GIFTS,"Skipped uncollected gifts");
            for(int i=0;i<s.loop.gifts.size();i++)s.loop.gifts.set(i,ItemStack.EMPTY);
            tick(p);finishAudio(p,s,h,OtherMotherLoopState.Phase.AFTER);
            h.assertTrue(s.loop.phase==OtherMotherLoopState.Phase.EXTRA,"Optional state comment omitted");
            finishAudio(p,s,h,OtherMotherLoopState.Phase.EXTRA);
            finishAudio(p,s,h,OtherMotherLoopState.Phase.TUTORIAL);
            h.assertTrue(s.loop.phase==OtherMotherLoopState.Phase.PROPOSAL,"Tutorial did not hand off to activity");h.succeed();
        });
    }
    @GameTest(template="empty") public static void familyResetClearsRelationshipAndLoop(GameTestHelper h){
        var f=fixture(h);f.scene.loop.phase=OtherMotherLoopState.Phase.GIFTS;f.scene.loop.trust=.9F;f.scene.loop.visits=9;
        var prop=LoopProps.spawn(f.player.serverLevel(),f.scene,ScenePropEntity.GIFT,f.player.position(),-1);
        FamilyVisitReset.forget(f.player.server,f.player.getUUID());
        h.assertTrue(!VisitData.get(f.player.server).scenes.containsKey(f.player.getUUID()),"Family kept old progress/meters");
        h.assertFalse(LoopProps.valid(prop),"Old family props still valid");h.succeed();
    }
    @GameTest(template="empty") public static void debugCompletesUnvisitedFamilyAndPersists(GameTestHelper h){
        var f=fixture(h);var p=f.player;var data=VisitData.get(p.server);data.scenes.remove(p.getUUID());
        var family=new com.coraline.parents.FamilyData.Family();family.owner=p.getUUID();com.coraline.parents.FamilyData.get(p.server).families.put(family.id,family);
        FirstVisit.debugComplete(p);h.assertFalse(data.scenes.containsKey(p.getUUID()),"Survival could skip story");
        p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);FirstVisit.debugComplete(p);
        var s=data.scenes.get(p.getUUID());h.assertTrue(s!=null&&s.stage==VisitData.Stage.DONE&&s.loop.visits==1&&family.id.equals(s.family),"Pre-visit shortcut did not bind to family");
        var saved=VisitData.load(data.save(new CompoundTag())).scenes.get(p.getUUID());h.assertTrue(saved.stage==VisitData.Stage.DONE&&saved.loop.visits==1,"Debug completion did not persist");
        h.assertTrue(OtherMotherLoop.entered(p,f.realm),"Next entry did not start loop");
        s.loop.trust=.7F;s.loop.gifts.add(new ItemStack(Items.DIAMOND));FirstVisit.debugComplete(p);
        h.assertTrue(s.loop.phase==OtherMotherLoopState.Phase.NONE&&s.loop.trust==.7F&&s.loop.gifts.isEmpty(),"Debug completion did not release active loop");h.succeed();
    }
    @GameTest(template="empty") public static void debugReleasesSupperChairWithoutRewards(GameTestHelper h){
        var f=fixture(h);var p=f.player;var s=f.scene;
        var family=new com.coraline.parents.FamilyData.Family();family.owner=p.getUUID();com.coraline.parents.FamilyData.get(p.server).families.put(family.id,family);s.family=family.id;
        p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);s.stage=VisitData.Stage.SUPPER;
        var seat=FirstVisit.spawn(p,s,ScenePropEntity.CHAIR,p.getX(),p.getY(),p.getZ(),0);p.startRiding(seat,true);p.getPersistentData().putLong("CoralineWakeAt",10000);
        FirstVisit.debugComplete(p);
        h.assertTrue(s.stage==VisitData.Stage.DONE&&!p.isPassenger()&&!p.getPersistentData().contains("CoralineWakeAt"),"Debug skip left player locked in scene");
        h.assertTrue(p.getInventory().isEmpty()&&!s.cakeTaken,"Debug skip granted story rewards");h.succeed();
    }
    @GameTest(template="empty") public static void treasureStaysInsideGiftBox(GameTestHelper h){
        var f=fixture(h);var s=f.scene;s.loop.phase=OtherMotherLoopState.Phase.GIFTS;s.loop.loot=OtherMotherDialogue.Loot.TREASURE;
        s.loop.gifts.add(new ItemStack(Items.DIAMOND,3));LoopProps.ensure(f.player,s);
        h.runAfterDelay(2,()->{
        var props=LoopProps.nearby(f.player.serverLevel(),s,s.table);
        h.assertFalse(props.stream().anyMatch(e->e.kind()==ScenePropEntity.PLATE||e.kind()==ScenePropEntity.GLASS),"Treasure was displayed on plates");
        var box=props.stream().filter(e->e.kind()==ScenePropEntity.GIFT).findFirst().orElseThrow();
        LoopProps.interact(f.player,box);
        h.assertTrue(box.treasure().get(0).getCount()==3&&box.mode()==1&&f.player.getInventory().isEmpty(),"Opening the lid must display treasure without collecting it");
        LoopProps.interact(f.player,box);LoopProps.interact(f.player,box);
        h.assertTrue(f.player.containerMenu==f.player.inventoryMenu,"Gift opened an inventory screen");
        h.assertTrue(s.loop.collected()&&f.player.getInventory().countItem(Items.DIAMOND)==3,"Pickup did not update saved reward ledger");h.succeed();});
    }
    @GameTest(template="empty") public static void returnTunnelClosesPhysicalDoorOnlyAtHome(GameTestHelper h){
        var f=fixture(h);var p=f.player;var d=DoorData.get(p.server).doors.get(f.scene.door);var source=p.server.overworld();
        d.pos=h.absolutePos(new BlockPos(3,1,3));d.dimension=source.dimension().location().toString();d.activeUntil=source.getGameTime()+10000;
        source.setBlock(d.pos,com.coraline.registry.ModBlocks.TINY_DOOR.get().defaultBlockState().setValue(com.coraline.block.TinyDoorBlock.PORTAL,true).setValue(com.coraline.block.TinyDoorBlock.OPEN,true),3);
        f.scene.loop.phase=OtherMotherLoopState.Phase.WINDOW;
        h.assertTrue(OtherWorld.rescueTunnel(p),"Could not enter return corridor");
        h.assertTrue(d.activeUntil>0&&source.getBlockState(d.pos).getValue(com.coraline.block.TinyDoorBlock.PORTAL),"Portal closed before reaching home");
        var tunnel=f.realm.tunnel();var end=tunnel.offset(0,0,17);
        p.serverLevel().setBlock(end,p.serverLevel().getBlockState(end).setValue(com.coraline.block.TinyDoorBlock.OPEN,true),3);
        p.setPos(tunnel.getX()+.5,tunnel.getY(),tunnel.getZ()+17.5);
        OtherWorld.playerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));
        h.assertTrue(!p.getPersistentData().getBoolean("CoralineInTunnel")&&OtherWorld.isOther(p.level())&&f.scene.loop.phase==OtherMotherLoopState.Phase.WINDOW,"Turning around failed or restarted the visit");
        h.assertTrue(d.activeUntil>0,"Turning around closed the portal");
        OtherWorld.rescueTunnel(p);
        OtherWorld.leave(p,false);
        var state=source.getBlockState(d.pos);
        h.assertTrue(!state.getValue(com.coraline.block.TinyDoorBlock.PORTAL)&&!state.getValue(com.coraline.block.TinyDoorBlock.OPEN)&&d.activeUntil==0,"Actual exit left portal or open door behind");h.succeed();
    }
    @GameTest(template="empty") public static void storyGazeTurnsBodyAndRetiresFather(GameTestHelper h){
        var f=fixture(h);var level=f.player.serverLevel();var mom=ModEntities.MOM.get().create(level);mom.makeOtherParent(0);mom.setPos(f.player.position().add(3,0,0));mom.setYRot(0);mom.yBodyRot=0;
        for(int i=0;i<12;i++)mom.lookAtStoryPlayer(f.player);
        h.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(mom.getYRot()-90))<1,"Stationary mother body did not turn toward player");
        var dad=ModEntities.DAD.get().create(level);dad.makeOtherParent(0);dad.getPersistentData().putUUID("RealmDoor",f.scene.door);
        h.assertTrue(OtherMotherLoop.retiredFather(dad),"Father retained after first story");
        f.scene.stage=VisitData.Stage.MOTHER_INTRO;h.assertFalse(OtherMotherLoop.retiredFather(dad),"First-visit father retired");h.succeed();
    }
    @GameTest(template="empty") public static void storySubtitleCoverage(GameTestHelper h){
        try(var in=MainLoopGameTests.class.getResourceAsStream("/assets/coraline/story_subtitles.json")){
            var data=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            for(var name:VisitAudio.SOUNDS.keySet())if(name.startsWith("other_mother")||name.startsWith("other_father")&&!name.equals("other_father_song"))h.assertTrue(data.has(name),"Missing story caption: "+name);
            h.assertFalse(data.has("other_father_song"),"Piano song must not have subtitles");
            for(var activity:OtherMotherActivities.Activity.values()){
                var name=ActivityWorld.introSound(activity);
                h.assertTrue(VisitAudio.SOUNDS.containsKey(name),"Activity intro sound was not registered: "+name);
                h.assertTrue(data.has(name),"Activity intro subtitle missing: "+name);
                h.assertTrue(VisitAudio.length(name)>20,"Activity intro duration missing: "+name);
            }
            for(var name:java.util.List.of("wybie_line_a","wybie_line_b","wybie_line_c"))h.assertTrue(data.has(name),"Missing Wybie caption");
            for(var track:data.entrySet())for(var cue:track.getValue().getAsJsonObject().getAsJsonArray("cues")){
                var c=cue.getAsJsonObject();h.assertTrue(c.get("end").getAsDouble()>c.get("start").getAsDouble()&&!c.get("text").getAsString().isBlank(),"Invalid subtitle cue");
            }
        }catch(java.io.IOException e){throw new RuntimeException(e);}h.succeed();
    }
    @GameTest(template="empty") public static void recoveryProgressWatchIgnoresIdleAndTravel(GameTestHelper h){
        var origin=new net.minecraft.world.phys.Vec3(0,100,0);var goal=new BlockPos(20,100,0);
        var idle=new QuestRecovery.Watch(goal,origin,0);
        h.assertFalse(idle.stalled(origin,1200),"Standing idle counted as stuck");
        var walking=new QuestRecovery.Watch(goal,origin,0);
        for(int i=0;i<40;i++)h.assertFalse(walking.stalled(origin.add(i,0,0),i*20),"Normal travel counted as stuck");
        var trapped=new QuestRecovery.Watch(goal,origin,0);boolean detected=false;
        for(int i=0;i<40;i++)detected|=trapped.stalled(origin.add(i%2==0?.5:-.5,0,0),i*20);
        h.assertTrue(detected,"Repeated movement without progress was not detected");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=380) public static void repeatVisitNeverAutomaticallyPullsPlayerToPortal(GameTestHelper h){
        var f=fixture(h);var p=f.player;var s=f.scene;var level=p.serverLevel();var start=p.blockPosition();
        s.loop.phase=OtherMotherLoopState.Phase.WINDOW;s.loop.portal=start.east(8);p.setOnGround(true);var original=p.position();
        for(int x=4;x<=10;x++)for(int z=-4;z<=2;z++)level.setBlock(start.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(Math.abs(x)==1||Math.abs(z)==1)for(int y=0;y<3;y++)level.setBlock(start.offset(x,y,z),Blocks.STONE.defaultBlockState(),3);
        h.assertTrue(OtherMotherLoop.active(p)!=null&&p.isAlive()&&!p.getAbilities().flying,"Recovery fixture is not eligible");
        for(int time=1;time<=340;time+=10){int at=time;h.runAtTickTime(at,()->{
            p.setOnGround(true);QuestRecovery.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));
        });}
        h.runAtTickTime(345,()->{
            h.assertTrue(p.distanceToSqr(original)<.01,"Invitation silently teleported player");
            h.assertTrue(s.loop.phase==OtherMotherLoopState.Phase.WINDOW&&OtherWorld.isOther(p.level()),"Recovery changed quest stage or sent player home");
            h.assertTrue(level.getBlockState(start.east()).is(Blocks.STONE),"Recovery edited copied blocks");
            h.assertTrue(VisitGuide.guide(p)==1&&p.distanceToSqr(VisitGuide.target(p,s).getCenter())<9,"Explicit guide rescue stopped working");h.succeed();
        });
    }
    @GameTest(template="empty") public static void oldRescueDoorsAreRetired(GameTestHelper h){
        var f=fixture(h);var prop=LoopProps.spawn(f.player.serverLevel(),f.scene,ScenePropEntity.RESCUE_DOOR,f.player.position(),-1);prop.tick();
        h.assertTrue(prop.isRemoved(),"Legacy rescue door survived");h.succeed();
    }
    @GameTest(template="empty") public static void portalWaitSpotAvoidsEntrance(GameTestHelper h){
        var f=fixture(h);var level=f.player.serverLevel();var portal=f.player.blockPosition();
        var spot=OtherMotherLoop.portalWaitingSpot(level,portal);
        h.assertTrue(Math.abs(spot.x-(portal.getX()+.5))>=2,"Mother waiting in entrance");
        for(int y=0;y<3;y++)level.setBlock(portal.east(2).above(y),Blocks.STONE.defaultBlockState(),3);
        spot=OtherMotherLoop.portalWaitingSpot(level,portal);
        h.assertTrue(spot.x<portal.getX(),"Did not choose clear opposite side");h.succeed();
    }
    @GameTest(template="empty") public static void unusedPortalExpiresAtDawn(GameTestHelper h){
        var f=fixture(h);var source=h.getLevel();var d=DoorData.get(f.player.server).doors.get(f.scene.door);
        d.pos=h.absolutePos(new BlockPos(2,1,2));d.dimension=source.dimension().location().toString();d.placed=true;
        var block=com.coraline.registry.ModBlocks.TINY_DOOR.get();
        source.setBlock(d.pos,block.defaultBlockState().setValue(com.coraline.block.TinyDoorBlock.PORTAL,true).setValue(com.coraline.block.TinyDoorBlock.OPEN,true),3);
        long previous=source.getDayTime();
        try{source.setDayTime(13000);d.activeUntil=24000;OtherWorld.expireNight(source,d);h.assertTrue(d.activeUntil==24000,"Expired during valid night");
            source.setDayTime(24000);OtherWorld.expireNight(source,d);
            h.assertTrue(d.activeUntil==0&&!source.getBlockState(d.pos).getValue(com.coraline.block.TinyDoorBlock.PORTAL)&&!source.getBlockState(d.pos).getValue(com.coraline.block.TinyDoorBlock.OPEN),"Dawn failed to close portal");
            source.setDayTime(37000);h.assertFalse(OtherWorld.active(source,d),"Unused portal returned next night");
        }finally{source.setDayTime(previous);}h.succeed();
    }
    @GameTest(template="empty") public static void lockedStoryAllowsDoorToggle(GameTestHelper h){
        var f=fixture(h);var p=f.player;var level=p.serverLevel();var pos=p.blockPosition();
        f.scene.loop.phase=OtherMotherLoopState.Phase.ARRIVAL;
        level.setBlock(pos,com.coraline.registry.ModBlocks.TINY_DOOR.get().defaultBlockState(),3);
        var hit=new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),net.minecraft.core.Direction.NORTH,pos,false);
        var event=new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(p,net.minecraft.world.InteractionHand.MAIN_HAND,pos,hit);
        RealmProtection.useBlock(event);h.assertFalse(event.isCanceled(),"Story lock blocked cosmetic door toggle");
        h.assertTrue(OtherMotherLoop.locked(p),"Travel lock was removed");
        h.assertFalse(OtherWorld.lockTunnelDoor(p,pos),"Scene lock prevented opening door");h.succeed();
    }
    @GameTest(template="empty") public static void skippedIntroRetriesUntilFirstEntry(GameTestHelper h){
        var f=fixture(h);var p=f.player;var d=DoorData.get(p.server).doors.get(f.scene.door);
        var family=new com.coraline.parents.FamilyData.Family();family.owner=p.getUUID();
        com.coraline.parents.FamilyData.get(p.server).families.put(family.id,family);f.scene.family=family.id;
        SceneAudio.begin(p,d);var data=SceneAudio.get(p.server);
        h.assertTrue(data.save(new CompoundTag()).contains("Pending_"+p.getUUID()),"Intro did not start");
        SceneAudio.unusedNightEnded(p.server,d);var saved=data.save(new CompoundTag());
        h.assertFalse(saved.getBoolean("Intro_"+family.id)||saved.contains("Pending_"+p.getUUID()),"Skipped intro still consumed");
        SceneAudio.begin(p,d);h.assertTrue(data.save(new CompoundTag()).contains("Pending_"+p.getUUID()),"Intro did not retry");
        SceneAudio.otherWorldEntered(p);SceneAudio.unusedNightEnded(p.server,d);
        h.assertTrue(data.save(new CompoundTag()).getBoolean("Intro_"+family.id),"Actual first entry lost its intro milestone");
        SceneAudio.endFamily(p.server,p.getUUID(),family.id,Set.of(d.id));
        h.assertFalse(data.save(new CompoundTag()).contains("Visited_"+family.id),"Family reset kept visit milestone");h.succeed();
    }
    private static void tick(FakePlayer p){OtherMotherLoop.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));}
}
