package com.coraline.feast;
import com.coraline.otherworld.*;
import com.coraline.registry.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.*;
import java.util.UUID;
import static com.coraline.feast.VisitData.Stage.*;
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class FirstVisitGameTests {
    @GameTest(template="empty",batch="route_safety",timeoutTicks=200) public static void caveFallbackDoesNotExcavateCopiedBuilds(GameTestHelper h){
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(3,1,3));
        level.setBlock(origin,Blocks.CHEST.defaultBlockState(),3);
        var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(origin);
        chest.setItem(0,new ItemStack(Items.DIAMOND,7));
        var positions=ScenePlacement.terrace(level,origin,64);
        h.assertTrue(positions!=null,"Cave fallback did not find empty air above the room");
        h.assertTrue(ScenePlacement.fits(level,positions[0],false)&&ScenePlacement.fits(level,positions[1],true),"Fallback cannot contain safe furniture/chairs");
        h.assertTrue(level.getBlockEntity(origin)==chest&&chest.getItem(0).getCount()==7,"Fallback damaged a copied chest or inventory");h.succeed();
    }
    @GameTest(template="empty") public static void caveArrivalFindsFloorBelowDoor(GameTestHelper h){
        var near=h.absolutePos(new BlockPos(4,6,4));
        h.setBlock(4,2,4,Blocks.STONE);
        var safe=OtherWorld.safePlayer(h.getLevel(),near);
        h.assertTrue(safe.equals(near.below(3)),"Arrival skipped the cave floor below the door");h.succeed();
    }
    @GameTest(template="empty") public static void guideUsesCurrentQuestAndCannotSkipDialogue(GameTestHelper h){
        var p=new net.minecraftforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"RouteOwner"));
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.table=new BlockPos(0,80,0);s.piano=new BlockPos(30,80,0);
        var realm=new RealmData.Realm();realm.door=s.door;realm.bed=new BlockPos(3,80,3);RealmData.get(p.server).realms.put(s.door,realm);
        s.stage=SONG;h.assertTrue(VisitGuide.target(p,s)==null,"Guide can skip locked piano dialogue");
        s.stage=FIND_FATHER;h.assertTrue(VisitGuide.target(p,s).equals(s.piano.north(2)),"Guide chooses wrong piano approach");
        s.stage=BED;h.assertTrue(VisitGuide.target(p,s).equals(realm.bed),"Guide doesn't return cave player to their own bed");
        RealmData.get(p.server).realms.remove(s.door);h.succeed();
    }
    @GameTest(template="empty") public static void supperChairHitboxIncludesBackrestAndArms(GameTestHelper h){
        var chair=ModEntities.SCENE_PROP.get().create(h.getLevel());chair.kind(ScenePropEntity.CHAIR);chair.setPos(h.absolutePos(new BlockPos(4,2,4)).getCenter());
        for(double height:new double[]{.7,1.1,1.65,2.02}){
            var from=chair.position().add(0,height,-2);var to=chair.position().add(0,height,1);
            h.assertTrue(chair.getBoundingBox().clip(from,to).isPresent(),"Visible chair is not clickable at height "+height);
        }
        h.assertTrue(chair.getBoundingBox().contains(chair.position().add(.39,1.12,0)),"Armrest is outside chair hitbox");h.succeed();
    }
    @GameTest(template="empty") public static void supperDialogueFinishesInOrderBeforeCake(GameTestHelper h){
        int previousEnd=100+VisitAudio.length(SupperDialogue.FATHER);
        for(int i=0;i<3;i++){
            int start=SupperDialogue.motherStart(i);
            h.assertTrue(start-previousEnd>=VisitConfig.SUPPER_VOICE_GAP.get(),"Supper voices overlap or lack a pause");
            h.assertTrue(("other_mother_supper_"+i).equals(SupperDialogue.cueAt(start)),"Supper dialogue order is wrong");
            previousEnd=start+VisitAudio.length("other_mother_supper_"+i);
        }
        int saved=VisitConfig.FEAST_TICKS.get();
        try{VisitConfig.FEAST_TICKS.set(100);h.assertTrue(SupperDialogue.endTick()>previousEnd,"Short supper config cuts off final line");}
        finally{VisitConfig.FEAST_TICKS.set(saved);}
        h.assertTrue(SupperDialogue.endTick()<VisitAudio.length("mysterious_supper"),"Cake should appear while supper music is still playing with default settings");
        h.assertTrue(VisitAudio.length("see_you_soon")>0,"Missing farewell recording");h.succeed();
    }
    @GameTest(template="empty") public static void supperChairPreservesPlayerLook(GameTestHelper h){
        var chair=ModEntities.SCENE_PROP.get().create(h.getLevel());chair.kind(ScenePropEntity.CHAIR);chair.setPos(h.absolutePos(new BlockPos(4,2,4)).getCenter());chair.setYRot(180);
        var player=h.makeMockSurvivalPlayer();h.assertTrue(player.startRiding(chair,true),"Could not mount chair");
        for(float yaw:new float[]{-135,42,170}){
            player.setYRot(yaw);player.setXRot(-25);
            chair.positionRider(player,(r,x,y,z)->r.setPos(x,y,z));
            h.assertTrue(player.getYRot()==yaw&&player.getXRot()==-25,"Chair overwrote free camera rotation");
        }
        player.stopRiding();h.succeed();
    }
    @GameTest(template="empty") public static void stalledFatherRecoversToHisDinnerChair(GameTestHelper h){
        var level=h.getLevel();var dad=ModEntities.DAD.get().create(level);dad.makeOtherParent(0);
        var pos=h.absolutePos(new BlockPos(5,2,5));h.setBlock(5,1,5,Blocks.STONE);
        var chair=ModEntities.SCENE_PROP.get().create(level);chair.kind(ScenePropEntity.CHAIR);chair.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        var piano=ModEntities.SCENE_PROP.get().create(level);piano.kind(ScenePropEntity.PIANO);piano.setPos(chair.position().add(15,0,0));dad.setPos(piano.position());dad.startRiding(piano,true);
        SupperSeating.approach(dad,chair);h.assertFalse(dad.isPassenger(),"Father did not leave piano");
        var progress=dad.getPersistentData().getCompound("CoralineSupperApproach");progress.putLong("Progress",level.getGameTime()-VisitConfig.SEAT_STALL_TICKS.get());
        SupperSeating.approach(dad,chair);h.assertTrue(dad.getVehicle()==chair,"Stalled father never reached reserved chair");
        dad.stopRiding();dad.setPos(piano.position());SupperSeating.approach(dad,chair);
        progress=dad.getPersistentData().getCompound("CoralineSupperApproach");progress.putLong("Started",level.getGameTime()-400);progress.putLong("Progress",level.getGameTime());
        SupperSeating.approach(dad,chair);h.assertTrue(dad.getVehicle()==chair,"Father still walking after the 20-second deadline");
        dad.stopRiding();dad.setPos(piano.position());h.setBlock(5,2,5,Blocks.STONE);
        SupperSeating.approach(dad,chair);progress=dad.getPersistentData().getCompound("CoralineSupperApproach");progress.putLong("Progress",level.getGameTime()-VisitConfig.SEAT_STALL_TICKS.get());
        SupperSeating.approach(dad,chair);h.assertFalse(dad.isPassenger(),"Recovery seated father inside a solid block");h.succeed();
    }
    @GameTest(template="empty",batch="realm_startup",timeoutTicks=300) public static void welcomeRetriesAfterLateParentsOnUnevenGround(GameTestHelper h){
        var server=h.getLevel().getServer();var level=server.getLevel(OtherWorld.KEY);
        var p=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"LateWelcome"));
        var door=new DoorData.Door();door.owner=p.getUUID();door.pos=new BlockPos(0,85,0);door.placed=true;DoorData.get(server).doors.put(door.id,door);
        var realm=RealmData.get(server).realm(door);realm.ready=true;realm.bed=realm.doorPos.offset(4,0,3);
        // A one-block dip every fourth row: no perfectly flat 5x5 area exists anywhere.
        for(int x=-76;x<=76;x++)for(int z=-76;z<=76;z++){
            int dip=Math.floorMod(z,4)==0?1:0;level.setBlock(realm.doorPos.offset(x,-1-dip,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);
        }
        level.setBlock(realm.bed,Blocks.RED_BED.defaultBlockState(),2);
        var family=new com.coraline.parents.FamilyData.Family();family.owner=p.getUUID();com.coraline.parents.FamilyData.get(server).families.put(family.id,family);
        p.getPersistentData().putUUID("CoralineVisitDoor",door.id);p.setPos(realm.doorPos.getCenter().add(0,0,2));
        p.getPersistentData().putBoolean("CoralineInTunnel",true);
        h.assertFalse(FirstVisit.prepareEntrance(p,realm),"Entrance must wait for actors");h.assertFalse(VisitData.get(server).scenes.containsKey(p.getUUID()),"Missing actors should defer, not complete, the visit");
        var forced=new net.minecraft.world.level.ChunkPos(realm.doorPos);level.setChunkForced(forced.x,forced.z,true);
        h.runAfterDelay(20,()->{
            for(boolean mom:new boolean[]{true,false}){
                var actor=(mom?ModEntities.MOM:ModEntities.DAD).get().create(level);actor.makeOtherParent(0);actor.setPos(realm.doorPos.getX()+.5,85,realm.doorPos.getZ()+1.5);
                actor.getPersistentData().putUUID("RealmDoor",door.id);actor.getPersistentData().putInt("RealmGeneration",realm.parentGeneration);level.addFreshEntity(actor);
                if(mom)family.mom=actor.getUUID();else family.dad=actor.getUUID();
            }
        });
        h.succeedWhen(()->{
            p.tickCount++;h.assertTrue(FirstVisit.prepareEntrance(p,realm),"Welcome is not staged yet");
            var scene=VisitData.get(server).scenes.get(p.getUUID());h.assertTrue(scene!=null,"Deferred welcome did not retry after actors arrived");
            h.assertTrue(scene.stage==MOTHER_INTRO&&scene.elapsed==0&&!FirstVisit.locked(p),"Introduction advanced before leaving tunnel");
            h.assertTrue(scene.piano.distSqr(realm.doorPos)>=38*38,"Piano was not staged farther from door");
            p.getPersistentData().remove("CoralineInTunnel");h.assertTrue(FirstVisit.locked(p),"Arrival did not activate staged introduction");
            h.assertTrue(scene.table.distSqr(scene.piano)>=64,"Piano overlaps dinner table");
            h.assertTrue(level.getEntity(scene.dad).isPassenger(),"Father was left wandering instead of seated at piano");
            scene.stage=DONE;FirstVisit.entered(p,realm);h.assertTrue(VisitData.get(server).scenes.get(p.getUUID())==scene,"Completed welcome replayed");level.setChunkForced(forced.x,forced.z,false);
        });
    }
    @GameTest(template="empty") public static void furniturePlacementRejectsWallsButAllowsShortGrass(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(12,1,12));
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)h.getLevel().setBlock(pos.offset(x,-1,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);
        h.getLevel().setBlock(pos,Blocks.GRASS.defaultBlockState(),2);
        h.assertTrue(ScenePlacement.fits(h.getLevel(),pos,false),"Short grass blocked the table");
        h.getLevel().setBlock(pos,Blocks.STONE.defaultBlockState(),2);
        h.assertFalse(ScenePlacement.fits(h.getLevel(),pos,false),"Furniture would intersect a wall");h.succeed();
    }
    @BeforeBatch(batch="defaultBatch") public static void establishTimerHistory(ServerLevel level){
        // Missing-item fixtures subtract grace periods from game time; negative values mean "not started".
        level.getServer().getWorldData().overworldData().setGameTime(Math.max(100000,level.getGameTime()));
    }
    @GameTest(template="empty",batch="realm_story") public static void welcomeSequenceReleasesPlayerAndClosesPortal(GameTestHelper h){
        var server=h.getLevel().getServer();var level=server.getLevel(OtherWorld.KEY);
        var p=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"StoryFlow"));p.tickCount=1;
        var door=new DoorData.Door();door.owner=p.getUUID();door.pos=h.absolutePos(new BlockPos(7,1,7));door.placed=true;door.activeUntil=999999;DoorData.get(server).doors.put(door.id,door);
        var r=RealmData.get(server).realm(door);r.ready=true;r.bed=r.doorPos.offset(4,0,0);level.setBlock(r.bed,Blocks.RED_BED.defaultBlockState(),2);
        var family=new com.coraline.parents.FamilyData.Family();family.owner=p.getUUID();com.coraline.parents.FamilyData.get(server).families.put(family.id,family);
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=door.id;s.table=r.doorPos.offset(0,0,6);s.piano=r.doorPos.offset(20,0,0);
        VisitData.get(server).scenes.put(s.owner,s);p.getPersistentData().putUUID("CoralineVisitDoor",door.id);p.setPos(s.piano.getCenter());
        var tick=new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p);
        level.getChunkSource().addRegionTicket(TicketType.PORTAL,new net.minecraft.world.level.ChunkPos(r.doorPos),3,r.doorPos);
        var forced=new net.minecraft.world.level.ChunkPos(r.doorPos);level.setChunkForced(forced.x,forced.z,true);
        h.runAfterDelay(40,()->{
        var actors=new java.util.ArrayList<com.coraline.parents.ParentEntity>();
        for(boolean mom:new boolean[]{true,false}){var actor=(mom?ModEntities.MOM:ModEntities.DAD).get().create(level);actor.makeOtherParent(0);actor.setNoGravity(true);actor.setPos(r.doorPos.getX()+.5,r.doorPos.getY(),r.doorPos.getZ()+.5);level.getChunkAt(actor.blockPosition());h.assertTrue(level.addFreshEntity(actor),"Fixture actor rejected");actors.add(actor);if(mom){s.mom=actor.getUUID();family.mom=actor.getUUID();}else{s.dad=actor.getUUID();family.dad=actor.getUUID();}}
        h.runAfterDelay(10,()->{
        h.assertTrue(level.getBlockState(r.bed).getBlock() instanceof BedBlock,"Fixture bed missing");
        h.assertTrue(level.getEntity(s.mom)!=null&&level.getEntity(s.dad)!=null,"Fixture parents not loaded: "+actors.stream().map(a->a.blockPosition()+" removed="+a.getRemovalReason()+" alive="+a.isAlive()).toList());
        FirstVisit.start(p,s,MOTHER_INTRO);s.elapsed=VisitAudio.length("other_mother_intro")-1;FirstVisit.tick(tick);h.assertTrue(s.stage==FIND_FATHER,"Mother intro transition failed: "+s.stage);
        FirstVisit.tick(tick);h.assertTrue(s.stage==FATHER_ENTER,"Father proximity transition failed");
        for(var stage:new VisitData.Stage[]{FATHER_ENTER,FATHER_INTRO,SONG,DINNER_READY}){
            String name=switch(stage){case FATHER_ENTER->"other_father_enter_scene";case FATHER_INTRO->"other_father_intro";case SONG->"other_father_song";default->"other_father_dinners_ready";};s.elapsed=VisitAudio.length(name)-1;FirstVisit.tick(tick);
        }
        h.assertTrue(s.stage==RETURN_TO_MOTHER,"Father scene did not release range lock");p.setPos(s.table.getCenter());
        // The live scene also reasserts seating every second; exercise that recovery
        // path so this concurrent suite is not sensitive to a delayed entity add.
        p.tickCount=20;FirstVisit.tick(tick);
        var chairs=level.getEntitiesOfClass(ScenePropEntity.class,new net.minecraft.world.phys.AABB(s.table).inflate(5),e->s.owner.equals(e.owner)&&e.kind()==ScenePropEntity.CHAIR);
        h.assertTrue(chairs.size()==3,"Supper needs three chairs");
        var chair=chairs.stream().filter(e->e.slot==0).findFirst().orElseThrow();p.setYRot(42);p.setXRot(-15);
        var result=chair.interactAt(p,new net.minecraft.world.phys.Vec3(0,1.65,-.38),InteractionHand.MAIN_HAND);
        h.assertTrue(result.consumesAction()&&s.stage==SUPPER&&p.getVehicle()==chair,"First backrest click did not immediately start supper");
        h.assertTrue(p.getYRot()==42&&p.getXRot()==-15,"Sitting snapped player camera");
        s.stage=SIT;p.stopRiding();var dadsChair=chairs.stream().filter(e->e.slot==2).findFirst().orElseThrow();p.startRiding(dadsChair,true);s.stage=SUPPER;
        FirstVisit.tick(tick);h.assertTrue(p.getVehicle()==chair,"Seat restoration stole Dad's chair instead of using the player's seat");
        s.elapsed=SupperDialogue.endTick()-1;FirstVisit.tick(tick);h.assertTrue(s.stage==CAKE,"Completed supper did not end with cake");
        s.elapsed=VisitConfig.CAKE_WAIT.get()-1;FirstVisit.tick(tick);h.assertTrue(s.stage==WAITING,"Cake reveal delay failed");
        var cake=level.getEntitiesOfClass(ScenePropEntity.class,new net.minecraft.world.phys.AABB(s.table).inflate(5),e->s.owner.equals(e.owner)&&e.kind()==ScenePropEntity.CAKE).get(0);cake.interact(p,InteractionHand.MAIN_HAND);h.assertTrue(s.cakeTaken&&p.getInventory().countItem(ModItems.WELCOME_CAKE.get())==1,"Cake gift failed");
        s.elapsed=VisitAudio.length("other_parents_waiting")-1;FirstVisit.tick(tick);h.assertTrue(s.stage==BED&&!p.isPassenger(),"Bedtime did not release chair");
        FirstVisit.slept(p);h.assertTrue(s.stage==DONE&&!FirstVisit.locked(p)&&door.activeUntil==0,"Sleep did not finish visit and close portal");level.setChunkForced(forced.x,forced.z,false);h.succeed();
        });
        });
    }
    @GameTest(template="empty") public static void cakeDropsPreserveEveryBiteState(GameTestHelper h){
        var cake=ModBlocks.WELCOME_CAKE.get();
        for(int bites=0;bites<7;bites++){
            var state=cake.defaultBlockState().setValue(CakeBlock.BITES,bites);
            var drops=cake.getDrops(state,new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel()));
            h.assertTrue(drops.size()==1&&drops.get(0).is(ModItems.WELCOME_CAKE.get()),"Cake failed to drop itself");
            h.assertTrue(drops.get(0).getTag().getInt("CakeBites")==bites,"Cake lost eaten-state NBT");
        }h.succeed();
    }
    @GameTest(template="empty") public static void firstVisitStateSurvivesRestart(GameTestHelper h){
        var d=new VisitData();var s=new VisitData.Scene();s.owner=UUID.randomUUID();s.door=UUID.randomUUID();s.mom=UUID.randomUUID();s.dad=UUID.randomUUID();s.table=BlockPos.ZERO;s.piano=new BlockPos(20,80,20);s.stage=SUPPER;s.elapsed=713;s.supperMusicEndsAt=10000;s.cakeTaken=true;d.scenes.put(s.owner,s);
        var copy=VisitData.load(d.save(new CompoundTag())).scenes.get(s.owner);
        h.assertTrue(copy.stage==SUPPER&&copy.elapsed==713&&copy.supperMusicEndsAt==10000&&copy.cakeTaken&&copy.piano.equals(s.piano),"Story progress lost on save/load");h.succeed();
    }
    @GameTest(template="empty") public static void allFeastFoodsAreRegisteredAndEdible(GameTestHelper h){
        h.assertTrue(FeastContent.FOODS.size()==11,"Missing requested feast food");
        for(var entry:FeastContent.FOODS.entrySet()){
            var stack=new ItemStack(entry.getValue().get());h.assertTrue(stack.isEdible(),"Food isn't edible: "+entry.getKey());
            h.assertTrue((stack.getUseAnimation()==UseAnim.DRINK)==FeastContent.drink(stack),"Drink not served as a drink");
        }
        h.assertTrue(VisitAudio.length("other_mother_intro")>0&&VisitAudio.length("mysterious_supper")>VisitConfig.FEAST_TICKS.get(),"Audio timings missing or supper song too short");h.succeed();
    }
    @GameTest(template="empty") public static void servingPlattersKeepTheirThemes(GameTestHelper h){
        var random=net.minecraft.util.RandomSource.create(42);
        for(int i=0;i<100;i++){
            var fruit=FeastContent.serving(random,2);h.assertTrue(fruit.is(Items.APPLE)||fruit.is(Items.GOLDEN_APPLE)||fruit.is(Items.MELON_SLICE),"Fruit platter contains unrelated food");
            var dessert=FeastContent.serving(random,3);h.assertTrue(dessert.is(Items.COOKIE)||dessert.is(Items.PUMPKIN_PIE)||dessert.is(FeastContent.FOODS.get("strawberry_pancakes").get()),"Dessert platter contains unrelated food");
        }
        h.assertTrue(VisitConfig.REFILL_TICKS.get()>=300,"Default refill still too fast");h.succeed();
    }
    @GameTest(template="empty") public static void platesEnforceOwnershipAndDoNotDuplicate(GameTestHelper h){
        var level=h.getLevel().getServer().getLevel(OtherWorld.KEY);
        var p=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"DinnerOwner"));
        var visitor=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"DinnerGuest"));
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.table=new BlockPos(5000,90,5000);s.piano=s.table.offset(20,0,0);s.stage=SUPPER;VisitData.get(p.server).scenes.put(s.owner,s);p.getPersistentData().putUUID("CoralineVisitDoor",s.door);
        var plate=ModEntities.SCENE_PROP.get().create(level);plate.owner=s.owner;plate.kind(ScenePropEntity.PLATE);plate.food(0,new ItemStack(Items.APPLE,3));
        plate.interact(visitor,InteractionHand.MAIN_HAND);h.assertTrue(plate.food(0).getCount()==3,"Visitor stole scene food");
        plate.interact(p,InteractionHand.MAIN_HAND);h.assertTrue(plate.food(0).getCount()==2&&p.getInventory().countItem(Items.APPLE)==1,"Plate pickup duplicated or lost food");
        plate.food(0,ItemStack.EMPTY);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.CARROT,2));plate.interact(p,InteractionHand.MAIN_HAND);
        h.assertTrue(plate.food(0).is(Items.CARROT)&&p.getMainHandItem().getCount()==1,"Empty plate rejected returned food");
        h.assertTrue(FirstVisit.locked(p)&&!FirstVisit.locked(visitor)&&!FirstVisit.bedAllowed(p),"First-visit locks leaked to guest or allowed early sleep");
        s.stage=DONE;h.assertFalse(FirstVisit.locked(p),"Finished story still locks exit");h.succeed();
    }
}
