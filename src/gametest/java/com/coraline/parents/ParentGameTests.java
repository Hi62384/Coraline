package com.coraline.parents;
import com.coraline.registry.*;
import com.coraline.config.CoralineConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class ParentGameTests {
    @GameTest(template="empty") public static void scriptedGazeDoesNotResetToBody(GameTestHelper h){
        var parent=ModEntities.MOM.get().create(h.getLevel());parent.makeOtherParent(7);
        parent.getPersistentData().putUUID("VisitOwner",java.util.UUID.randomUUID());
        var viewer=h.makeMockSurvivalPlayer();parent.setPos(0,4,0);viewer.setPos(4,4,0);
        for(int i=0;i<100;i++){
            parent.lookAtStoryPlayer(viewer);parent.getLookControl().tick();
            if(i>5)h.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(parent.getYHeadRot()+90))<.01,"Scripted gaze wandered away from owner");
        }
        viewer.setPos(-4,6,0);
        for(int i=0;i<20;i++){parent.lookAtStoryPlayer(viewer);parent.getLookControl().tick();}
        h.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(parent.getYHeadRot()-90))<.01&&parent.getXRot()<0,"Gaze did not track player movement");
        parent.getPersistentData().remove("VisitOwner");h.assertFalse(parent.scriptedGaze(),"Normal look AI remains locked after scene");h.succeed();
    }
    @GameTest(template="empty") public static void groceriesIncludeEdibleWrap(GameTestHelper h){
        var mom=ModEntities.MOM.get().create(h.getLevel());double original=CoralineConfig.WRAP_CHANCE.get();
        try{
            CoralineConfig.WRAP_CHANCE.set(1.0);
            var reward=ParentTasks.reward(mom,2);
            h.assertTrue(reward.is(ModItems.MUSTARD_KETCHUP_SALSA_WRAP.get()),"Groceries did not select wrap at 100% chance");
            h.assertTrue(reward.getCount()==CoralineConfig.FOOD_COUNT.get(),"Wrap ignored food delivery count");
            h.assertTrue(reward.isEdible()&&reward.getItem().getFoodProperties().getNutrition()==4,"Wrap must restore the requested two hunger icons");
            CoralineConfig.WRAP_CHANCE.set(0.0);
            h.assertFalse(ParentTasks.reward(mom,2).is(ModItems.MUSTARD_KETCHUP_SALSA_WRAP.get()),"Wrap ignored disabled chance");
        }finally{CoralineConfig.WRAP_CHANCE.set(original);}
        h.succeed();
    }
    @GameTest(template="empty",batch="parent_work",timeoutTicks=300) public static void workingParentWalksThenDelivers(GameTestHelper h){
        var mom=pair(h);var owner=tame(h,mom);var f=mom.family();var chest=placeBed(h,mom,owner);
        f.busy=false;f.availabilityChangesAt=h.getLevel().getGameTime()+1000;f.momTask=4;
        f.momDelivery=h.getLevel().getGameTime()+60;
        mom.setPos(f.bed.getX()+6.5,f.bed.getY(),f.bed.getZ()-.5);
        var start=mom.position();
        java.util.Set<Integer> delays=new java.util.HashSet<>();
        int base=CoralineConfig.TASK_INTERVAL.get();double variation=CoralineConfig.TASK_VARIATION.get();
        for(int i=0;i<50;i++){
            int delay=ParentTasks.nextDelay(mom);delays.add(delay);
            h.assertTrue(delay>=Math.round(base*(1-variation))&&delay<=Math.round(base*(1+variation)),"Delivery delay outside configured range");
        }
        h.assertTrue(delays.size()>1,"Delivery intervals were not randomized");
        h.runAtTickTime(40,()->{
            h.assertTrue(chest.isEmpty(),"Delivery appeared before it was due");
            h.assertTrue(mom.position().distanceToSqr(start)>.25,"Working parent did not move between deliveries");
        });
        h.succeedWhen(()->{
            h.assertTrue(!chest.isEmpty(),"Parent has not delivered: pos="+mom.position()+" bed="+f.bed+" ticks="+mom.tickCount+" due="+f.momDelivery+" now="+h.getLevel().getGameTime()+" path="+mom.getNavigation().isDone()+" task="+f.momTask+" working="+ParentTasks.working(mom,f));
            var front=f.bed.relative(net.minecraft.core.Direction.NORTH);
            h.assertTrue(mom.distanceToSqr(f.bed.getCenter())<=9,"Delivered remotely instead of at chest");
        });
    }
    @GameTest(template="empty") public static void failedPlacementKeepsBedThenValidPlacementConsumesOnce(GameTestHelper h){
        var mom=pair(h);var owner=tame(h,mom);var f=mom.family();
        var anchor=h.absolutePos(new BlockPos(14,1,14));owner.setPos(anchor.getX()-2,anchor.getY(),anchor.getZ()+.5);owner.setYRot(0);
        var stack=ParentsBedItem.forFamily(f.id);owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var hit=new net.minecraft.world.phys.BlockHitResult(anchor.below().getCenter().add(0,.5,0),net.minecraft.core.Direction.UP,anchor.below(),false);
        var context=new net.minecraft.world.item.context.BlockPlaceContext(h.getLevel(),owner,InteractionHand.MAIN_HAND,stack,hit);
        var item=(ParentsBedItem)ModItems.PARENTS_BED.get();
        h.setBlock(15,1,15,Blocks.STONE);
        h.assertFalse(item.place(context).consumesAction(),"Obstructed placement should fail");
        h.assertTrue(stack.getCount()==1&&f.bed==null,"Failed placement consumed or linked the bed");
        h.setBlock(15,1,15,Blocks.AIR);h.setBlock(15,0,15,Blocks.AIR);
        h.assertFalse(item.place(context).consumesAction(),"Unsupported placement should fail");
        h.assertTrue(stack.getCount()==1,"Unsupported placement consumed the bed");
        h.setBlock(15,0,15,Blocks.GRASS_BLOCK);
        h.assertTrue(item.place(context).consumesAction(),"Clear supported placement failed");
        h.assertTrue(stack.isEmpty()&&anchor.equals(f.bed),"Successful placement must consume exactly once");
        for(int part=0;part<4;part++)h.assertTrue(h.getLevel().getBlockState(ParentsBedBlock.partPos(anchor,net.minecraft.core.Direction.NORTH,part)).is(ModBlocks.PARENTS_BED.get()),"Missing placed bed part");
        h.succeed();
    }
    @GameTest(template="empty") public static void lostBedRecoveryDoesNotRepeat(GameTestHelper h){
        var mom=pair(h);var owner=tame(h,mom);owner.getInventory().clearContent();owner.setShiftKeyDown(true);
        mom.interact(owner,InteractionHand.MAIN_HAND);
        h.assertTrue(owner.getInventory().countItem(ModItems.PARENTS_BED.get())==1,"Lost bed not recovered");
        owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        // Keep the replacement in a non-hand slot while repeating the recovery interaction.
        owner.getInventory().setItem(10,ParentsBedItem.forFamily(mom.familyId));
        mom.interact(owner,InteractionHand.MAIN_HAND);
        h.assertTrue(owner.getInventory().countItem(ModItems.PARENTS_BED.get())==1,"Recovery duplicated a carried bed");h.succeed();
    }
    @GameTest(template="empty") public static void oneLivingFamilyPerPlayer(GameTestHelper h){
        var first=pair(h);Player owner=tame(h,first);var second=pair(h);
        second.interact(owner,InteractionHand.MAIN_HAND);
        h.assertTrue(second.family().owner==null,"A player claimed a second living pair");
        first.kill();second.interact(owner,InteractionHand.MAIN_HAND);
        h.assertTrue(owner.getUUID().equals(second.family().owner),"Cannot claim again after losing parents");h.succeed();
    }
    @GameTest(template="empty") public static void timedAvailabilityPersists(GameTestHelper h){
        var mom=pair(h);var f=mom.family();var server=h.getLevel().getServer();long now=server.overworld().getGameTime();
        f.busy=true;f.availabilityChangesAt=now+100;
        h.assertFalse(f.isAvailable(server),"Busy period incorrectly available");
        f.availabilityChangesAt=now;
        h.assertTrue(f.isAvailable(server),"Busy period did not become available");
        h.assertTrue(f.availabilityChangesAt>=now+3600&&f.availabilityChangesAt<=now+4800,"Available duration outside 3-4 minutes");
        var saved=FamilyData.Family.load(f.save());
        h.assertTrue(!saved.busy&&saved.availabilityChangesAt==f.availabilityChangesAt,"Schedule was not persisted");
        f.availabilityChangesAt=now;
        h.assertFalse(f.isAvailable(server),"Available period did not become busy");
        h.assertTrue(f.availabilityChangesAt>=now+6000&&f.availabilityChangesAt<=now+9600,"Busy duration outside 5-8 minutes");h.succeed();
    }
    @GameTest(template="empty") public static void eggCreatesPairAndVariedSkins(GameTestHelper h){
        for(int x=1;x<24;x++)for(int z=1;z<24;z++)h.setBlock(x,0,z,Blocks.GRASS_BLOCK);
        Player player=h.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.PARENTS_SPAWN_EGG.get(),2));
        var ground=h.absolutePos(new BlockPos(8,0,8));
        var hit=new net.minecraft.world.phys.BlockHitResult(ground.getCenter(),net.minecraft.core.Direction.UP,ground,false);
        var context=new net.minecraft.world.item.context.UseOnContext(h.getLevel(),player,InteractionHand.MAIN_HAND,player.getMainHandItem(),hit);
        var existing=new java.util.HashSet<java.util.UUID>();
        h.getLevel().getEntitiesOfClass(ParentEntity.class,new net.minecraft.world.phys.AABB(ground).inflate(8)).forEach(p->existing.add(p.getUUID()));
        h.assertTrue(ModItems.PARENTS_SPAWN_EGG.get().useOn(context).consumesAction(),"Egg failed");
        var parents=h.getLevel().getEntitiesOfClass(ParentEntity.class,new net.minecraft.world.phys.AABB(ground).inflate(8),p->!existing.contains(p.getUUID()));
        h.assertTrue(parents.size()==2,"Egg must spawn exactly two parents");
        h.assertTrue(parents.get(0).familyId.equals(parents.get(1).familyId),"Egg parents must share family");
        h.assertTrue(player.getMainHandItem().getCount()==1,"Egg should consume once");
        for(var parent:parents){
            int old=parent.skin();parent.randomizeSkin();
            h.assertTrue(parent.skin()!=old,"Consecutive skins must differ");
            CompoundTag saved=new CompoundTag();parent.addAdditionalSaveData(saved);
            int selected=parent.skin();parent.readAdditionalSaveData(saved);
            h.assertTrue(parent.skin()==selected,"Skin must persist");
        }
        h.succeed();
    }
    private static ParentEntity pair(GameTestHelper h){
        for(int x=1;x<24;x++)for(int z=1;z<24;z++)h.setBlock(x,0,z,Blocks.GRASS_BLOCK);
        var before=new java.util.HashSet<>(FamilyData.get(h.getLevel().getServer()).families.keySet());
        h.assertTrue(ParentEvents.spawnPair(h.getLevel(),h.absolutePos(new BlockPos(8,1,8))),"Pair failed to spawn");
        var family=FamilyData.get(h.getLevel().getServer()).families.values().stream().filter(f->!before.contains(f.id)).findFirst().orElseThrow();
        return (ParentEntity)h.getLevel().getEntity(family.mom);
    }
    private static Player tame(GameTestHelper h,ParentEntity mom){
        Player player=h.makeMockSurvivalPlayer();player.setUUID(java.util.UUID.randomUUID());var p=h.absolutePos(new BlockPos(7,1,7));player.setPos(p.getX()+.5,p.getY(),p.getZ()+.5);
        mom.interact(player,InteractionHand.MAIN_HAND);return player;
    }
    private static ParentsBedEntity placeBed(GameTestHelper h,ParentEntity mom,Player player){
        h.setBlock(6,1,6,ModBlocks.PARENTS_BED.get());var pos=h.absolutePos(new BlockPos(6,1,6));
        var block=(ParentsBedBlock)ModBlocks.PARENTS_BED.get();block.setPlacedBy(h.getLevel(),pos,h.getLevel().getBlockState(pos),player,ParentsBedItem.forFamily(mom.familyId));
        return (ParentsBedEntity)h.getLevel().getBlockEntity(pos);
    }
    @GameTest(template="empty",timeoutTicks=500) public static void pairedDeathPersists(GameTestHelper h){
        ParentEntity mom=pair(h);var f=mom.family();var dad=(ParentEntity)h.getLevel().getEntity(f.dad);
        h.assertTrue(dad!=null&&dad.isAlive(),"Dad missing from pair");mom.kill();
        h.assertTrue(f.dead&&dad.isAlive()&&mom.isAlive()&&mom.deathFrozen()&&dad.deathFrozen(),"Both parents must freeze before death");
        h.assertTrue(!dad.hurt(dad.damageSources().genericKill(),100),"Frozen partner must reject damage");
        var reloaded=FamilyData.load(FamilyData.get(h.getLevel().getServer()).save(new CompoundTag()));
        h.assertTrue(reloaded.families.get(f.id).dead&&reloaded.families.get(f.id).deathPhase==1,"Death sequence must survive world reload");
        h.runAfterDelay(12,()->h.assertTrue(mom.isAlive()&&dad.isAlive(),"Mom died before one second"));
        h.runAfterDelay(22,()->h.assertTrue(!mom.isAlive()&&dad.isAlive(),"Mom must die after one second, partner must wait"));
        h.runAfterDelay(ParentVoices.length(ModSounds.MOM_DEATH.get())+22,()->h.assertTrue(dad.isAlive(),"Second parent died before two seconds of its own line"));
        h.runAfterDelay(ParentVoices.length(ModSounds.MOM_DEATH.get())+62,()->h.assertTrue(dad.isAlive(),"Dad died before four seconds of his line"));
        h.runAfterDelay(ParentVoices.length(com.coraline.registry.ModSounds.MOM_DEATH.get())+83,()->{
            h.assertTrue(!dad.isAlive(),"Dad must die four seconds after his line starts");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=500) public static void dadDeathGoesFirst(GameTestHelper h){
        var mom=pair(h);var f=mom.family();var dad=(ParentEntity)h.getLevel().getEntity(f.dad);dad.kill();
        h.assertTrue(!f.firstDeathMom&&f.deathPhase==1,"Dad must speak first when Dad receives lethal damage");
        h.runAfterDelay(22,()->h.assertTrue(dad.isAlive()&&mom.isAlive(),"Dad died before two seconds of his line"));
        h.runAfterDelay(62,()->h.assertTrue(dad.isAlive()&&mom.isAlive(),"Dad died before four seconds"));
        h.runAfterDelay(82,()->h.assertTrue(!dad.isAlive()&&mom.isAlive(),"Mom must wait for Dad's recording"));
        h.runAfterDelay(ParentVoices.length(ModSounds.DAD_DEATH.get())+12,()->h.assertTrue(mom.isAlive(),"Mom died before one second of her own line"));
        h.runAfterDelay(ParentVoices.length(ModSounds.DAD_DEATH.get())+23,()->{
            h.assertTrue(!mom.isAlive(),"Mom must die one second after Dad's complete recording");h.succeed();
        });
    }
    @GameTest(template="empty") public static void dialoguePoolsRespectModeAndRain(GameTestHelper h){
        h.assertTrue(ParentVoices.select(false,false,false)==ModSounds.DAD_NEGLECT.get(),"Dry neglect selected rain pool");
        h.assertTrue(ParentVoices.select(false,false,true)==ModSounds.DAD_NEGLECT_RAIN.get(),"Rain did not enable rain pool");
        h.assertTrue(ParentVoices.select(false,true,true)==ModSounds.DAD_TASK.get(),"Rain must not override task mode");
        h.assertTrue(ParentVoices.select(true,false,true)==ModSounds.MOM_NEGLECT.get(),"Mom neglect pool incorrect");
        h.assertTrue(ParentVoices.select(true,true,false)==ModSounds.MOM_TASK.get(),"Mom task pool incorrect");h.succeed();
    }
    @GameTest(template="empty") public static void parentDialogueVisitsEveryEligibleClip(GameTestHelper h){
        var mom=ModEntities.MOM.get().create(h.getLevel());var heard=new java.util.HashSet<String>();
        for(int i=0;i<11;i++){
            var sound=ParentVoices.dialogue(mom);h.assertTrue(heard.add(sound.getLocation().getPath()),"Mother repeated a clip before using all eleven neglect lines");
            h.assertTrue(ParentVoices.length(sound)>0,"Individual clip has no duration");
        }
        h.assertTrue(heard.contains("parent_voice_momneglect10"),"Numbered line 10 was excluded");h.succeed();
    }
    @GameTest(template="empty") public static void idleParentsDoNotSpeakTogether(GameTestHelper h){
        var mom=pair(h);var f=mom.family();var dad=(ParentEntity)h.getLevel().getEntity(f.dad);
        for(var parent:new ParentEntity[]{mom,dad}){
            var saved=new CompoundTag();parent.addAdditionalSaveData(saved);saved.putInt("VoiceCountdown",1);parent.readAdditionalSaveData(saved);
        }
        mom.tickVoice();long reserved=f.dialogueUntil;dad.tickVoice();
        h.assertTrue(reserved>h.getLevel().getServer().overworld().getGameTime()&&f.dialogueUntil==reserved,"Dad started over Mom's idle line");
        var momSaved=new CompoundTag();var dadSaved=new CompoundTag();mom.addAdditionalSaveData(momSaved);dad.addAdditionalSaveData(dadSaved);
        h.assertTrue(momSaved.getInt("VoiceCountdown")>=1000,"Mom's independent minute timer did not reset");
        h.assertTrue(dadSaved.getInt("VoiceCountdown")>=20&&dadSaved.getInt("VoiceCountdown")<=100,"Dad must retry independently after a short random delay");h.succeed();
    }
    @GameTest(template="empty") public static void tamingAndBedRoundTrip(GameTestHelper h){
        ParentEntity mom=pair(h);Player owner=tame(h,mom);var f=mom.family();
        var dad=(ParentEntity)h.getLevel().getEntity(f.dad);dad.interact(owner,InteractionHand.MAIN_HAND);
        h.assertTrue(owner.getInventory().countItem(ModItems.PARENTS_BED.get())==1,"Taming pair must grant exactly one linked bed");
        var bed=placeBed(h,mom,owner);h.assertTrue(f.bed.equals(bed.getBlockPos()),"Bed identity not linked");
        h.assertTrue(h.getLevel().getBlockEntity(f.bed) instanceof ParentsBedEntity,"Built-in storage missing");
        bed.setItem(0,new ItemStack(Items.APPLE,3));
        var loaded=new ParentsBedEntity(bed.getBlockPos(),bed.getBlockState());loaded.load(bed.saveWithFullMetadata());
        h.assertTrue(loaded.getItem(0).getCount()==3,"Storage must survive save/load");
        var anchor=f.bed;
        for(int part=0;part<4;part++)h.assertTrue(h.getLevel().getBlockState(ParentsBedBlock.partPos(anchor,net.minecraft.core.Direction.NORTH,part)).is(ModBlocks.PARENTS_BED.get()),"Double bed missing part "+part);
        h.getLevel().destroyBlock(f.bed,true);
        for(int part=0;part<4;part++)h.assertFalse(h.getLevel().getBlockState(ParentsBedBlock.partPos(anchor,net.minecraft.core.Direction.NORTH,part)).is(ModBlocks.PARENTS_BED.get()),"Orphaned bed part");
        h.assertTrue(f.bed==null&&f.chest==null,"Broken bed must return parents to follow mode");h.succeed();
    }
    @GameTest(template="empty") public static void tasksAndMenuAuthorization(GameTestHelper h){
        ParentEntity mom=pair(h);Player owner=tame(h,mom);var f=mom.family();var bed=placeBed(h,mom,owner);
        f.busy=false;f.availabilityChangesAt=h.getLevel().getGameTime()+1200;
        ParentsMenu menu=new ParentsMenu(1,owner.getInventory(),mom);
        h.assertTrue(menu.clickMenuButton(owner,4),"Owner cannot assign wood task");
        var outsider=h.makeMockSurvivalPlayer();h.assertFalse(menu.clickMenuButton(outsider,2),"Outsider can change tasks");
        h.assertFalse(menu.clickMenuButton(owner,99),"Invalid task accepted");
        h.assertFalse(menu.clickMenuButton(owner,103),"Mom's menu changed Dad's task");
        var dadMenu=new ParentsMenu(2,owner.getInventory(),(ParentEntity)h.getLevel().getEntity(f.dad));
        h.assertTrue(dadMenu.clickMenuButton(owner,103),"Dad's menu rejected coal task");
        h.assertFalse(dadMenu.clickMenuButton(owner,4),"Dad's menu changed Mom's task");
        f.momDelivery=0;
        var chest=(ParentsBedEntity)h.getLevel().getBlockEntity(f.bed);
        mom.setPos(f.bed.getX()+.5,f.bed.getY(),f.bed.getZ()-.5);
        f.busy=true;ParentTasks.tick(mom,f);
        h.assertFalse(chest.isEmpty(),"Assigned tasks must keep delivering during busy periods");
        chest.clearContent();f.momDelivery=0;
        f.busy=false;mom.setPos(f.bed.getX()+8,f.bed.getY(),f.bed.getZ()+8);ParentTasks.tick(mom,f);
        h.assertTrue(chest.isEmpty(),"Delivery occurred without walking to storage");
        mom.setPos(f.bed.getX()+.5,f.bed.getY(),f.bed.getZ()-.5);ParentTasks.tick(mom,f);
        h.assertTrue(!chest.isEmpty(),"Wood task did not deliver logs");
        var dad=(ParentEntity)h.getLevel().getEntity(f.dad);dad.setPos(f.bed.getX()+1.5,f.bed.getY(),f.bed.getZ()-.5);f.dadTask=3;f.dadDelivery=0;ParentTasks.tick(dad,f);
        h.assertTrue(chest.countItem(Items.COAL)>0,"Dad coal task failed");
        for(int cycle=0;cycle<4;cycle++){
            int before=chest.countItem(Items.COAL);f.busy=cycle%2==0;f.dadDelivery=0;ParentTasks.tick(dad,f);
            h.assertTrue(chest.countItem(Items.COAL)>before,"Repeated delivery stalled in cycle "+cycle);
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void gardenCreatesCrops(GameTestHelper h){
        var mom=pair(h);var owner=tame(h,mom);var f=mom.family();placeBed(h,mom,owner);
        f.busy=false;f.availabilityChangesAt=h.getLevel().getGameTime()+1200;
        for(int x=1;x<=13;x+=3)for(int z=1;z<=13;z+=3)h.setBlock(x,4,z,Blocks.GLOWSTONE);
        h.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING).set(true,h.getLevel().getServer());
        h.runAtTickTime(30,()->{
            h.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING).set(true,h.getLevel().getServer());
            f.momTask=1;
            // Plot selection is randomized; allow several delivery attempts in this partially lit fixture.
            for(int attempt=0;attempt<10 && f.gardenPlots==0;attempt++){f.momDelivery=0;ParentTasks.tick(mom,f);}
            int eligible=0;
            for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){
                var soil=f.bed.offset(x,-1,z);
                if(h.getLevel().getBlockState(soil).is(net.minecraft.tags.BlockTags.DIRT)&&h.getLevel().isEmptyBlock(soil.above())&&h.getLevel().getMaxLocalRawBrightness(soil.above())>=9)eligible++;
            }
            h.assertTrue(f.gardenPlots==1,"Garden failed: plots="+f.gardenPlots+" eligible="+eligible+" chest="+f.chest+" dim="+f.homeDim+" mom="+mom.blockPosition());h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=80) public static void monstersTargetParents(GameTestHelper h){
        var mom=pair(h);var zombie=h.spawn(net.minecraft.world.entity.EntityType.ZOMBIE,10.5F,1,8.5F);
        zombie.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE,200));
        // Target acquisition is randomized; require acquisition within the timeout, not on one exact tick.
        h.succeedWhen(()->h.assertTrue(zombie.getTarget() instanceof ParentEntity,"Zombie did not target a parent"));
    }
    @GameTest(template="empty") public static void tinyDoorStaysBlocked(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(3,1,3));h.setBlock(3,1,3,ModBlocks.TINY_DOOR.get());
        var state=h.getLevel().getBlockState(pos);var open=state.setValue(com.coraline.block.TinyDoorBlock.OPEN,true);
        h.assertTrue(!open.getCollisionShape(h.getLevel(),pos).isEmpty(),"Open door must retain brick collision");
        h.assertTrue(open.getCollisionShape(h.getLevel(),pos).bounds().equals(state.getCollisionShape(h.getLevel(),pos).bounds()),"Decorative door changed its brick barrier");h.succeed();
    }
}
