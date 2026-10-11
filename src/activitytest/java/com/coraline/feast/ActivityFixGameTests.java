package com.coraline.feast;

import com.coraline.registry.ModItems;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class ActivityFixGameTests {
    @GameTest(template="empty",timeoutTicks=60) public static void potentLavaExpiresAndToggles(GameTestHelper h){
        var level=h.getLevel();var p=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"LavaTest"));
        var item=com.coraline.clothing.ArtifactItems.POTENT_OTHER_FLINT_AND_STEEL.get();p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(item));
        var base=h.absolutePos(new BlockPos(22,4,22));
        level.setBlockAndUpdate(base,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var context=new net.minecraft.world.item.context.UseOnContext(p,net.minecraft.world.InteractionHand.MAIN_HAND,
            new net.minecraft.world.phys.BlockHitResult(base.getCenter(),net.minecraft.core.Direction.UP,base,false));
        item.useOn(context);h.assertTrue(level.getBlockState(base.above()).is(net.minecraft.world.level.block.Blocks.LAVA),"Source not created");
        item.useOn(context);h.assertTrue(level.isEmptyBlock(base.above()),"Second use did not remove source");
        item.useOn(context);
        h.runAfterDelay(19,()->h.assertTrue(level.getBlockState(base.above()).is(net.minecraft.world.level.block.Blocks.LAVA),"Source removed before one second"));
        h.runAfterDelay(21,()->{h.assertTrue(level.isEmptyBlock(base.above()),"Source survived one second");
            item.useOn(context);level.setBlockAndUpdate(base.above(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        });
        h.runAfterDelay(43,()->{h.assertTrue(level.getBlockState(base.above()).is(net.minecraft.world.level.block.Blocks.STONE),"Timer erased a replacement block");h.succeed();});
    }
    @GameTest(template="empty") public static void soulControlWorksFromMainHand(GameTestHelper h){
        var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"SoulTest"));
        // Default FakePlayer spawn can be inside generated terrain. Give this
        // jump test a dedicated floor and genuinely empty headroom.
        var at=new BlockPos(90000,100,90000);h.getLevel().getChunkAt(at);
        for(int x=-2;x<=4;x++)for(int z=-2;z<=2;z++){h.getLevel().setBlock(at.offset(x,-1,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);for(int y=0;y<4;y++)h.getLevel().setBlock(at.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);}
        p.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(at));
        var mob=EntityType.PIG.create(h.getLevel());mob.setPos(p.position().add(2,0,0));h.getLevel().addFreshEntity(mob);
        var stack=new ItemStack(com.coraline.clothing.ArtifactItems.SOUL_CAPTURER.get());
        var list=new net.minecraft.nbt.ListTag();var record=new net.minecraft.nbt.CompoundTag();record.putUUID("Id",mob.getUUID());record.putString("Name","Test Pig");list.add(record);stack.getOrCreateTag().put("CoralineCaptured",list);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,ItemStack.EMPTY);
        var body=p.position();float yaw=p.getYRot();com.coraline.clothing.SoulCapturerItem.action(p,mob.getUUID(),1);
        h.assertTrue(mob.isNoAi(),"Main-hand control silently failed");h.assertTrue(p.getCamera()==p,"Server camera moved the body");
        var before=mob.position();com.coraline.clothing.SoulCapturerItem.move(p,0,1,false,false,90,0);
        h.assertTrue(mob.getX()<before.x,"Controlled mob did not move with camera yaw");
        h.assertTrue(p.position().equals(body)&&p.getYRot()==yaw,"Control changed real body or head");
        mob.setOnGround(true);double groundY=mob.getY();
        com.coraline.clothing.SoulCapturerItem.move(p,0,0,true,false,90,0);
        h.assertTrue(mob.getY()>groundY+.3,"Controlled soul cannot jump");
        mob.setOnGround(false);
        com.coraline.clothing.SoulCapturerItem.move(p,0,0,false,false,90,0);
        h.assertTrue(mob.getY()>groundY+.6,"Jump velocity did not persist");
        com.coraline.clothing.SoulCapturerItem.exit(p);h.assertTrue(!mob.isNoAi(),"Exit failed to restore AI");mob.discard();h.succeed();
    }
    @GameTest(template="empty") public static void soulToolsMatchBlocks(GameTestHelper h){
        var dirt=net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
        var stone=net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        h.assertTrue(!com.coraline.clothing.SoulCapturerItem.canMine(ItemStack.EMPTY,dirt),"Empty hand can mine");
        h.assertTrue(com.coraline.clothing.SoulCapturerItem.canMine(new ItemStack(net.minecraft.world.item.Items.IRON_SHOVEL),dirt),"Shovel cannot dig dirt");
        h.assertTrue(!com.coraline.clothing.SoulCapturerItem.canMine(new ItemStack(net.minecraft.world.item.Items.IRON_SHOVEL),stone),"Shovel mines stone");
        h.assertTrue(com.coraline.clothing.SoulCapturerItem.canMine(new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE),stone),"Pick cannot mine stone");
        h.assertTrue(!com.coraline.clothing.SoulCapturerItem.canMine(new ItemStack(net.minecraft.world.item.Items.WOODEN_PICKAXE),net.minecraft.world.level.block.Blocks.DIAMOND_ORE.defaultBlockState()),"Wrong tool tier accepted");
        h.succeed();
    }
    @GameTest(template="empty") public static void dreamMovesWithoutMovingBody(GameTestHelper h){
        var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"DreamTest"));
        var item=com.coraline.clothing.ArtifactItems.DREAMSTONE.get();p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(item));
        var body=p.position();item.use(h.getLevel(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(com.coraline.clothing.DreamstoneItem.active(p),"Dream did not activate");h.assertTrue(p.getCamera()==p,"Server camera follows projection");
        com.coraline.clothing.DreamstoneItem.move(p,1,0,true,false,0,0);
        var projections=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class,new AABB(body,body).inflate(3),e->e.isInvisible());
        h.assertTrue(projections.stream().anyMatch(e->e.getX()>body.x&&e.getY()>body.y),"Dream left/rise input did not move projection");
        h.assertTrue(p.position().equals(body),"Dream moved real body");com.coraline.clothing.DreamstoneItem.stop(p);h.succeed();
    }
    @GameTest(template="empty") public static void perfectRecipesBeatRivals(GameTestHelper h){
        var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"PerfectCakeTest"));
        for(int base=0;base<7;base++)for(boolean milk:new boolean[]{false,true}){
            var s=scene();var l=s.loop;l.cakeBase=base;l.cakeIngredients=15|(1<<(base+4))|(milk?1<<11:0);l.cakeStep=4;
            l.cakeMixQuality=4;l.cakeBakeQuality=4;l.cakeLayers=3;l.cakeFilling=0;l.cakeFrosting=0;l.cakeFrostCoverage=4;l.cakeFrostQuality=4;l.cakeToppings=7;l.cakeTopper=0;
            CakeMaking.complete(p,s,h.absolutePos(new BlockPos(30,5,30)),true);
            h.assertTrue(l.cakeScore==100,"Perfect recipe cannot attain 100: "+base);
            for(int seed=0;seed<100;seed++){l.cakeSeed=seed;h.assertTrue(CakeContestActivity.rank(s)==1,"Perfect cake lost");}
        }h.succeed();
    }
    @GameTest(template="empty") public static void fishingRewardsConsistentCatching(GameTestHelper h){
        h.assertTrue(FishingContestActivity.catchPoints(1)==11,"Successful junk catch lacks effort points");
        h.assertTrue(FishingContestActivity.catchPoints(110)==30,"Jackpot dominates contest scoring");
        var l=new OtherMotherLoopState();l.fishingScore=17*FishingContestActivity.catchPoints(8);
        java.util.Arrays.fill(l.fishingRivalScores,13*FishingContestActivity.catchPoints(12));
        h.assertTrue(FishingContestActivity.rank(l)==1,"Consistent 17-catch performance loses to 13 ordinary catches");h.succeed();
    }
    @GameTest(template="empty") public static void allActivityAudioDurationsPresent(GameTestHelper h) throws Exception {
        try(var in=VisitAudio.class.getResourceAsStream("/assets/coraline/visit_audio_lengths.json")){
            var table=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            for(var name:VisitAudio.SOUNDS.keySet())h.assertTrue(table.has(name)&&table.get(name).getAsInt()>0,"Missing registered voice timing: "+name);
        }h.succeed();
    }
    @GameTest(template="empty") public static void missingTimingRecoversFromRecording(GameTestHelper h) throws Exception {
        String name=ActivityWorld.introSound(OtherMotherActivities.Activity.CAKE_MAKING_CONTEST);
        int expected=VisitAudio.length(name);
        var field=VisitAudio.class.getDeclaredField("lengths");field.setAccessible(true);var saved=field.get(null);
        try{
            field.set(null,new com.google.gson.JsonObject());
            h.assertTrue(Math.abs(VisitAudio.length(name)-expected)<=1,"Missing timing did not recover full recording length");
        }finally{field.set(null,saved);}
        h.succeed();
    }
    @GameTest(template="empty") public static void debugItemsNotSilentlyBlocked(GameTestHelper h){
        var level=h.getLevel().getServer().getLevel(ActivityWorld.KEY);
        h.assertTrue(level!=null,"Activity dimension unavailable");
        var p=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"DebugInputTest"));
        p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        for(var item:new net.minecraft.world.item.Item[]{ModItems.ICE_BOAT_RACE_ACTIVITY_DEBUG.get(),
            ModItems.MAGIC_SHOW_ACTIVITY_DEBUG.get(),ModItems.MAGIC_GARDEN_ACTIVITY_DEBUG.get(),
            ModItems.MICE_CIRCUS_ACTIVITY_DEBUG.get(),ModItems.OCEAN_MONUMENT_ACTIVITY_DEBUG.get(),ModItems.CARNIVAL_ACTIVITY_DEBUG.get()}){
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(item));
            var air=new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem(p,net.minecraft.world.InteractionHand.MAIN_HAND);
            com.coraline.otherworld.RealmProtection.useItem(air);
            h.assertTrue(!air.isCanceled(),"Creative debug air click canceled: "+item);
            var block=new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(p,net.minecraft.world.InteractionHand.MAIN_HAND,BlockPos.ZERO,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.ZERO,net.minecraft.core.Direction.UP,BlockPos.ZERO,false));
            com.coraline.otherworld.RealmProtection.useBlock(block);
            h.assertTrue(!block.isCanceled()&&block.getUseItem()==net.minecraftforge.eventbus.api.Event.Result.ALLOW,"Debug block click not dispatched: "+item);
        }
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var denied=new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem(p,net.minecraft.world.InteractionHand.MAIN_HAND);
        com.coraline.otherworld.RealmProtection.useItem(denied);
        h.assertTrue(denied.isCanceled(),"Survival debug protections were bypassed");h.succeed();
    }
    @GameTest(template="empty") public static void magicShowVisibleReplacements(GameTestHelper h){
        for(long seed=0;seed<1000;seed++)MagicShowActivity.validatePerformance(seed);
        h.succeed();
    }
    @GameTest(template="empty") public static void carnivalRingContact(GameTestHelper h){
        var box=new AABB(-.21,0,-.21,.21,.9,.21);
        var from=new net.minecraft.world.phys.Vec3(0,.78,2);
        h.assertTrue(CarnivalActivity.ringHitsNeck(box,from,new net.minecraft.world.phys.Vec3(0,-.05,-3)),"Fast neck hit did not count");
        h.assertTrue(!CarnivalActivity.ringHitsNeck(box,new net.minecraft.world.phys.Vec3(0,.1,2),new net.minecraft.world.phys.Vec3(0,0,-3)),"Low body hit scored");
        h.assertTrue(!CarnivalActivity.ringHitsNeck(box,from.add(2,0,0),new net.minecraft.world.phys.Vec3(0,0,-3)),"Miss scored");
        var peg=com.coraline.registry.ModEntities.SCENE_PROP.get().create(h.getLevel());
        peg.setPos(h.absolutePos(new BlockPos(25,8,25)).getCenter());
        // Entity-only hit results are exactly what vanilla's snowball collision overload returns.
        var hit=new net.minecraft.world.phys.EntityHitResult(peg);
        h.assertTrue(hit.getLocation().y==peg.getY(),"Vanilla origin assumption changed");h.succeed();
    }
    @GameTest(template="empty") public static void carnivalGuestExclusion(GameTestHelper h){
        for(var b:CarnivalActivity.Booth.values())if(b.ordinal()<6){
            h.assertTrue(!CarnivalActivity.crowdAllowed(b.x+.5,b.z+.5),"Guest allowed inside "+b);
            h.assertTrue(!CarnivalActivity.crowdAllowed(b.x+.5,b.z+6),"Guest allowed in playing lane "+b);
            h.assertTrue(!CarnivalActivity.crowdSegmentAllowed(new net.minecraft.world.phys.Vec3(b.x-9,0,b.z),new net.minecraft.world.phys.Vec3(b.x+9,0,b.z),BlockPos.ZERO),"Guest could cross booth");
        }
        h.assertTrue(CarnivalActivity.crowdAllowed(0,20),"Public avenue blocked");h.succeed();
    }
    @GameTest(template="empty") public static void carnivalDuckReach(GameTestHelper h){
        for(int tick=0;tick<900;tick++)for(int slot=0;slot<8;slot++){
            var at=CarnivalActivity.duckPosition(BlockPos.ZERO,slot,tick);
            var eye=new net.minecraft.world.phys.Vec3(at.x,1.62,4.35);
            h.assertTrue(eye.distanceTo(at.add(0,.36,0))<3,"Duck beyond normal reach from counter");
            for(int other=0;other<8;other++)if(other!=slot){
                var there=CarnivalActivity.duckPosition(BlockPos.ZERO,other,tick);
                h.assertTrue(at.distanceTo(there)>.75,"Duck hitboxes overlap");
                var box=new AABB(there.x-.36,there.y,there.z-.36,there.x+.36,there.y+.72,there.z+.36);
                h.assertTrue(box.clip(eye,at.add(0,.36,0)).isEmpty(),"Front duck obstructs rear duck");
            }
        }h.succeed();
    }
    @GameTest(template="empty") public static void oceanBeamAndReset(GameTestHelper h){
        int charge=0;for(int i=0;i<79;i++)charge=OceanMonumentActivity.nextCharge(charge,true);
        h.assertTrue(charge<OceanMonumentActivity.CHARGE_TICKS,"Partial lock failed early");
        h.assertTrue(OceanMonumentActivity.nextCharge(charge,false)==0,"Cover must clear the ENTIRE charge");
        h.assertTrue(OceanMonumentActivity.nextCharge(charge,true)==80,"A full lock did not resolve");
        var g=EntityType.GUARDIAN.create(h.getLevel());OceanMonumentActivity.beam(g,123);
        h.assertTrue(g.hasActiveAttackTarget(),"Vanilla synchronized beam was not activated");
        OceanMonumentActivity.beam(g,0);h.assertTrue(!g.hasActiveAttackTarget(),"Beam not cleared");h.succeed();
    }
    @GameTest(template="empty") public static void oceanGuardiansSurvivePeaceful(GameTestHelper h) throws Exception {
        var method=net.minecraft.world.entity.monster.Monster.class.getDeclaredMethod("shouldDespawnInPeaceful");method.setAccessible(true);
        var g=EntityType.GUARDIAN.create(h.getLevel());
        h.assertTrue((boolean)method.invoke(g),"Ordinary Guardian behavior was changed");
        g.getPersistentData().putUUID("CoralineOceanOwner",UUID.randomUUID());
        h.assertTrue(!(boolean)method.invoke(g),"Course Guardian disappears in Peaceful");h.succeed();
    }
    @GameTest(template="empty") public static void oceanCoverBlocksSight(GameTestHelper h){
        var l=h.getLevel();var at=h.absolutePos(new BlockPos(30,8,30));
        var from=net.minecraft.world.phys.Vec3.atCenterOf(at.west(2));var to=net.minecraft.world.phys.Vec3.atCenterOf(at.east(2));
        var g=EntityType.GUARDIAN.create(l);g.setPos(from);
        for(int x=-3;x<=3;x++)l.setBlock(at.offset(x,0,0),net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(),2);
        h.assertTrue(OceanMonumentActivity.clearSight(l,from,to,g),"Water blocked beam");
        l.setBlock(at,net.minecraft.world.level.block.Blocks.PRISMARINE.defaultBlockState(),2);
        h.assertTrue(!OceanMonumentActivity.clearSight(l,from,to,g),"Prismarine cover leaked beam");
        l.setBlock(at,net.minecraft.world.level.block.Blocks.DARK_PRISMARINE.defaultBlockState(),2);
        h.assertTrue(!OceanMonumentActivity.clearSight(l,from,to,g),"Cycling gate leaked beam");h.succeed();
    }
    @GameTest(template="empty") public static void oceanRewardOwnershipAndSave(GameTestHelper h){
        var s=scene();s.loop.ocean.putInt("Phase",1);
        h.assertTrue(!OceanMonumentActivity.takePrize(s,s.owner),"Unfinished course awarded treasure");
        s.loop.ocean.putInt("Phase",3);h.assertTrue(!OceanMonumentActivity.takePrize(s,s.owner),"Failure awarded treasure");
        s.loop.ocean.putInt("Phase",2);h.assertTrue(!OceanMonumentActivity.takePrize(s,UUID.randomUUID()),"Helper stole prize");
        h.assertTrue(OceanMonumentActivity.takePrize(s,s.owner),"Winner could not claim");
        s.loop=OtherMotherLoopState.load(s.loop.save());
        h.assertTrue(!OceanMonumentActivity.takePrize(s,s.owner),"Reload duplicated treasure");
        h.assertTrue(OceanMonumentActivity.exitReady(s),"Claim did not open exit");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=400) public static void oceanAllSixSectionsConnected(GameTestHelper h){
        var l=h.getLevel();var c=new BlockPos(h.absolutePos(new BlockPos(900,0,900)).getX(),180,h.absolutePos(new BlockPos(900,0,900)).getZ());
        OceanMonumentCourse.build(l,c);
        for(int z=OceanMonumentCourse.start(2)-36;z>=OceanMonumentCourse.start(2)-42;z--){
            var target=new net.minecraft.world.phys.Vec3(c.getX()-5.5,c.getY()+2,c.getZ()+z+.5);
            for(int slot=0;slot<3;slot++){
                var eye=OceanMonumentCourse.guardian(2,slot).add(net.minecraft.world.phys.Vec3.atLowerCornerOf(c)).add(0,.85,0);
                h.assertTrue(eye.distanceToSqr(target)>=24*24||!OceanMonumentActivity.clearSight(l,eye,target,null),"Section 3 exit corridor exposed at "+z+" to guardian "+slot);
            }
        }
        int width=25,height=16,depth=264,total=width*height*depth;
        boolean[] clear=new boolean[total],seen=new boolean[total];int[] queue=new int[total];
        for(int z=0;z<depth;z++)for(int y=0;y<height;y++)for(int x=0;x<width;x++){
            var at=c.offset(x-12,y,-12-z);clear[(z*height+y)*width+x]=l.getBlockState(at).getCollisionShape(l,at).isEmpty()&&l.getBlockState(at.above()).getCollisionShape(l,at.above()).isEmpty();
        }
        int first=(1*height+1)*width+12,head=0,tail=0;queue[tail++]=first;seen[first]=true;
        while(head<tail){int id=queue[head++],x=id%width,y=id/width%height,z=id/(width*height);
            for(int d=0;d<6;d++){int nx=x+(d==0?1:d==1?-1:0),ny=y+(d==2?1:d==3?-1:0),nz=z+(d==4?1:d==5?-1:0);
                if(nx<0||nx>=width||ny<0||ny>=height||nz<0||nz>=depth)continue;
                int next=(nz*height+ny)*width+nx;if(clear[next]&&!seen[next]){seen[next]=true;queue[tail++]=next;}
            }
        }
        for(int section=0;section<6;section++){
            int z=section*44+43,y=OceanMonumentCourse.exitHeight(section),x=OceanMonumentCourse.doorway(section)+12;
            h.assertTrue(seen[(z*height+y)*width+x],"Section "+(section+1)+" exit is unreachable");
            for(int slot=0;slot<OceanMonumentCourse.guardians(section);slot++){
                var pos=OceanMonumentCourse.guardian(section,slot).add(net.minecraft.world.phys.Vec3.atLowerCornerOf(c));
                h.assertTrue(!l.getBlockCollisions(null,new AABB(pos.x-.42,pos.y,pos.z-.42,pos.x+.42,pos.y+.85,pos.z+.42)).iterator().hasNext(),"Guardian embedded in architecture: "+section+"/"+slot);
            }
        }
        OceanMonumentCourse.openVault(l,c);
        for(int y=8;y<=11;y++){
            var at=c.offset(5,y,-277);h.assertTrue(l.getBlockState(at).canSurvive(l,at),"Vault airlock has unsupported decoration");
            h.assertTrue(l.getBlockState(at).getCollisionShape(l,at).isEmpty(),"Vault doorway obstructs player");
        }
        h.runAfterDelay(20,()->{
            h.assertTrue(l.getFluidState(c.offset(5,8,-278)).isEmpty(),"Vault airlock flooded the treasure gallery");
            OceanMonumentCourse.remove(l,c);h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=60) public static void scriptedCleanupDoesNotDropDecorations(GameTestHelper h){
        var l=h.getLevel();var at=h.absolutePos(new BlockPos(20,8,0));
        for(int x=0;x<3;x++)l.setBlock(at.offset(x,0,0),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
        l.setBlock(at.above(),net.minecraft.world.level.block.Blocks.PINK_CARPET.defaultBlockState(),2);
        l.setBlock(at.offset(1,1,0),net.minecraft.world.level.block.Blocks.LANTERN.defaultBlockState(),2);
        l.setBlock(at.offset(2,1,0),net.minecraft.world.level.block.Blocks.POTTED_POPPY.defaultBlockState(),2);
        var bed=net.minecraft.world.level.block.Blocks.RED_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING,net.minecraft.core.Direction.EAST);
        for(int x=0;x<2;x++)l.setBlock(at.offset(x,0,2),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
        l.setBlock(at.offset(0,1,2),bed,2);
        l.setBlock(at.offset(1,1,2),bed.setValue(net.minecraft.world.level.block.BedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.HEAD),2);
        for(int x=0;x<3;x++)ActivityBlocks.set(l,at.offset(x,0,0),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        for(int x=0;x<3;x++)ActivityBlocks.set(l,at.offset(x,1,0),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        for(int x=0;x<2;x++)ActivityBlocks.set(l,at.offset(x,0,2),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        for(int x=0;x<2;x++)ActivityBlocks.set(l,at.offset(x,1,2),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        h.runAfterDelay(5,()->{h.assertTrue(l.getEntitiesOfClass(ItemEntity.class,new AABB(at).inflate(4)).isEmpty(),"Scripted teardown spawned broken decoration items");h.succeed();});
    }
    @GameTest(template="empty") public static void circusWagerOutcomes(GameTestHelper h){
        h.assertTrue(MiceCircusActivity.payout(0,1,0)==4,"Winner should recover stake plus bookie's two ingots");
        h.assertTrue(MiceCircusActivity.payout(0,1,1)==0,"Bookie's win should cost stake");
        h.assertTrue(MiceCircusActivity.payout(0,1,2)==2,"Neither winner must refund entire stake");
        var state=new OtherMotherLoopState();state.circusPhase=5;state.circusBookieMouse=2;state.circusGifted=true;
        state.circusPhaseUntil=100;state.circusWager=new ItemStack(ModItems.PERFECT_INGOT.get(),2);
        var restored=OtherMotherLoopState.load(state.save());
        h.assertTrue(restored.circusPhase==5&&restored.circusBookieMouse==2&&restored.circusGifted&&restored.circusPhaseUntil==100&&restored.circusWager.getCount()==2,"Circus save lost wager state");h.succeed();
    }
    @GameTest(template="empty") public static void circusPaceAndFinishOrder(GameTestHelper h){
        int unpredictable=0;
        for(int seed=1;seed<=30;seed++){
            int early=0;for(int i=0;i<5;i++){
                double previous=0;for(int t=0;t<=1600;t+=10){double progress=CircusRaceCourse.progress(seed,i,t);h.assertTrue(progress>=previous,"Race moved backward");previous=progress;}
                h.assertTrue(previous==1,"Racer did not finish within 80 seconds");
                if(CircusRaceCourse.progress(seed,i,200)>CircusRaceCourse.progress(seed,early,200))early=i;
            }
            if(early!=CircusRaceCourse.winner(seed))unpredictable++;
        }
        h.assertTrue(unpredictable>=10,"Early leader predicts too many winners");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=400) public static void circusRouteClearsApparatus(GameTestHelper h){
        var origin=h.absolutePos(new BlockPos(256,0,256));var c=new BlockPos(origin.getX(),200,origin.getZ());MiceCircusActivity.build(h.getLevel(),c);
        var collisions=new java.util.LinkedHashMap<CircusRaceCourse.Leg,String>();
        for(int mouse=0;mouse<5;mouse++)for(int sample=0;sample<=1800;sample++){
            var at=CircusRaceCourse.position(sample/1800.0,mouse).add(net.minecraft.world.phys.Vec3.atBottomCenterOf(c));
            var box=new AABB(at.x-.18,at.y+.02,at.z-.18,at.x+.18,at.y+.39,at.z+.18);
            if(h.getLevel().getBlockCollisions(null,box).iterator().hasNext())collisions.putIfAbsent(CircusRaceCourse.legAt(sample/1800.0),at.subtract(net.minecraft.world.phys.Vec3.atBottomCenterOf(c))+" block="+h.getLevel().getBlockState(BlockPos.containing(at)));
        }
        h.assertTrue(collisions.isEmpty(),"Mouse intersects apparatus: "+collisions.values());h.succeed();
    }
    private static VisitData.Scene scene(){var s=new VisitData.Scene();s.owner=UUID.randomUUID();s.door=UUID.randomUUID();return s;}
    @GameTest(template="empty") public static void exitsNeverRequireTokens(GameTestHelper h){
        var s=scene();s.loop.carnivalTokensRemaining=15;s.loop.carnivalBooth=0;
        h.assertTrue(CarnivalActivity.exitReady(s),"Carnival locked during a game");
        h.assertTrue(VillageActivity.exitReady(s),"Village locked before token issue");h.succeed();
    }
    @GameTest(template="empty") public static void fullCreativeInventoryDropsTokens(GameTestHelper h){
        var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"TokenTest"));
        var at=h.absolutePos(new BlockPos(1,2,1));p.setPos(at.getX(),at.getY(),at.getZ());p.getAbilities().instabuild=true;
        for(int i=0;i<36;i++)p.getInventory().items.set(i,new ItemStack(Items.STONE,64));
        ActivityTokens.give(p,new ItemStack(ModItems.VILLAGE_TOKEN.get(),15));
        int dropped=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(at).inflate(2),e->e.getPersistentData().hasUUID("CoralineAwardOwner")&&p.getUUID().equals(e.getPersistentData().getUUID("CoralineAwardOwner"))&&e.getItem().is(ModItems.VILLAGE_TOKEN.get())).stream().mapToInt(e->e.getItem().getCount()).sum();
        h.assertTrue(dropped==15,"Full inventory lost or duplicated tokens: "+dropped);h.succeed();
    }
    @GameTest(template="empty") public static void pantryDisplaysNeverOverlap(GameTestHelper h){
        var s=scene();var c=h.absolutePos(new BlockPos(8,2,8));CakeMaking.install(h.getLevel(),s,c);
        var props=h.getLevel().getEntitiesOfClass(ScenePropEntity.class,new AABB(c).inflate(7),e->s.owner.equals(e.owner)&&e.kind()==ScenePropEntity.CAKE_KITCHEN);
        h.assertTrue(props.size()==19,"Expected 12 ingredients and 7 recipes");
        for(int i=0;i<props.size();i++)for(int j=i+1;j<props.size();j++)h.assertFalse(props.get(i).getBoundingBox().intersects(props.get(j).getBoundingBox()),"Overlapping kitchen click targets");
        CakeMaking.install(h.getLevel(),s,c);
        h.assertTrue(h.getLevel().getEntitiesOfClass(ScenePropEntity.class,new AABB(c).inflate(7),e->s.owner.equals(e.owner)&&e.kind()==ScenePropEntity.CAKE_KITCHEN).size()==19,"Repeated install duplicates kitchen props");h.succeed();
    }
    @GameTest(template="empty") public static void cakeQualityRewardsExecution(GameTestHelper h){
        var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"CakeTest"));var c=h.absolutePos(new BlockPos(1,2,1));
        var bad=scene();bad.loop.cakeBase=0;bad.loop.cakeIngredients=4095;bad.loop.cakeStep=4;
        CakeMaking.complete(p,bad,c,true);h.assertTrue(bad.loop.cakeScore<=35,"Random ingredients score too highly");
        var good=scene();var l=good.loop;l.cakeBase=0;l.cakeIngredients=31;l.cakeStep=4;l.cakeMixQuality=4;l.cakeBakeQuality=4;l.cakeLayers=3;
        l.cakeFilling=0;l.cakeFrosting=0;l.cakeFrostCoverage=4;l.cakeFrostQuality=4;l.cakeToppings=7;l.cakeTopper=0;l.cakePiping=0;
        CakeMaking.complete(p,good,c,true);h.assertTrue(l.cakeScore>=92,"Excellent cake not rewarded");h.succeed();
    }
    @GameTest(template="empty") public static void countdownBoatMixinLoads(GameTestHelper h){
        Boat b=EntityType.BOAT.create(h.getLevel());h.assertTrue(b!=null,"Boat construction failed");
        b.setCustomName(net.minecraft.network.chat.Component.literal("CoralineRaceCountdownLock"));
        b.setDeltaMovement(1,0,1);
        try {net.minecraftforge.fml.util.ObfuscationReflectionHelper.findMethod(Boat.class,"m_38396_").invoke(b);}
        catch(ReflectiveOperationException e){throw new RuntimeException(e);}
        h.assertTrue(b.getDeltaMovement().lengthSqr()==0,"Countdown control did not clear motion");h.succeed();
    }
}
