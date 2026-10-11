package com.coraline.feast;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class ActivityFollowupGameTests {
    @GameTest(template="empty") public static void heldLanternDoesNotRewriteWorldAndLegacyLightsClean(GameTestHelper h){
        var f=ExpeditionGameTests.fixture(h,OtherMotherActivities.Activity.OTHER_MINESHAFT);var p=f.p();var at=p.blockPosition();var l=p.serverLevel();
        l.setBlock(at,Blocks.AIR.defaultBlockState(),2);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(com.coraline.clothing.UtilityItems.MOONLIT_LANTERN.get()));
        com.coraline.clothing.UtilityEffects.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));
        h.assertTrue(l.isEmptyBlock(at),"Held lantern still writes moving light blocks");
        var block=com.coraline.registry.ModBlocks.MOONLIT_LANTERN.get();var legacy=block.defaultBlockState().setValue(com.coraline.block.MoonlitLanternBlock.PORTABLE,true);
        l.setBlock(at,legacy,2);block.tick(legacy,l,at,l.random);h.assertTrue(l.isEmptyBlock(at),"Legacy invisible light remained while holding lantern");h.succeed();
    }
    @GameTest(template="empty") public static void mineShovelAndExtraLoans(GameTestHelper h){
        var f=ExpeditionGameTests.fixture(h,OtherMotherActivities.Activity.OTHER_MINESHAFT);var p=f.p();var s=f.s();
        p.getInventory().selected=0;p.getInventory().setItem(2,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLD_INGOT,7));
        p.getInventory().setItem(3,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,4));
        MineshaftActivity.begin(p,s);ExpeditionActivities.state(s).putInt("Phase",1);
        h.assertTrue(p.getInventory().getItem(2).is(com.coraline.registry.ModItems.ACTIVITY_MINING_SHOVEL.get())&&p.getInventory().getItem(3).is(com.coraline.registry.ModItems.ACTIVITY_MINE_MAP.get()),"Missing equipment");
        p.getInventory().selected=2;
        var mob=net.minecraft.world.entity.EntityType.ZOMBIE.create(p.serverLevel());mob.setPos(p.position().add(0,0,1));ActivityScenery.mark(mob,s);mob.getPersistentData().putBoolean("MineEnemy",true);mob.getPersistentData().putInt("Generation",s.loop.generation);mob.setHealth(20);p.serverLevel().addFreshEntity(mob);
        MineshaftActivity.attack(p,mob);h.assertTrue(mob.getHealth()==17,"Shovel must deal exactly 3 damage");
        MineshaftActivity.attack(p,mob);h.assertTrue(mob.getHealth()==17,"Attack spam bypassed cooldown");
        MineshaftActivity.restoreLantern(p,s);ExpeditionActivities.restore(p,s);
        h.assertTrue(p.getInventory().getItem(2).is(net.minecraft.world.item.Items.GOLD_INGOT)&&p.getInventory().getItem(2).getCount()==7,"Shovel slot original lost");
        h.assertTrue(p.getInventory().getItem(3).is(net.minecraft.world.item.Items.DIAMOND)&&p.getInventory().getItem(3).getCount()==4,"Map slot original lost");
        mob.discard();h.succeed();
    }
    @GameTest(template="empty") public static void endLasersCoverEveryTileAndAccelerate(GameTestHelper h){
        for(int room=0;room<5;room++){
            if(room>0)h.assertTrue(EndCityChallenge.cycle(room)<EndCityChallenge.cycle(room-1),"Laser failed to accelerate");
            int r=EndCityCourse.radius(room),cycle=EndCityChallenge.cycle(room);
            var angles=new java.util.HashSet<Integer>();
            for(int a=-r;a<=r;a++)for(int b=-r;b<=r;b++){
                boolean covered=false;
                for(int cue=0;cue<(2*r+1)*(2*r+1);cue++){
                    long t=(long)cue*cycle+cycle-1;var beam=EndCityChallenge.geometry(net.minecraft.core.BlockPos.ZERO,t,room);
                    angles.add((int)(Math.atan2(beam.to().z-beam.from().z,beam.to().x-beam.from().x)*100));
                    var tile=new net.minecraft.world.phys.AABB(a+.4,.2,b+.4,a+.6,1.5,b+.6);
                    if(beam.intersects(tile))covered=true;
                }
                h.assertTrue(covered,"Uncovered floor tile "+a+"/"+b);
            }
            h.assertTrue(angles.size()>20,"Beam angles repeat a simple sweep");
        }h.succeed();
    }
    @GameTest(template="empty") public static void fortressBatchedMovementUsesCrossingPoint(GameTestHelper h){
        var gates=java.util.List.of(new FortressActivity.Gate(-10,0,0,1));
        h.assertTrue(FortressActivity.crossedGates(gates,0,new Vec3(-1,0,.5),new Vec3(4,-20,.5))==1,"Used final packet X instead of crossing X");
        var several=java.util.List.of(new FortressActivity.Gate(-10,0,0,2),new FortressActivity.Gate(-20,0,0,2),new FortressActivity.Gate(-30,0,0,2));
        h.assertTrue(FortressActivity.crossedGates(several,0,new Vec3(.5,0,.5),new Vec3(.5,-35,.5))==3,"Lost gates in batched movement");
        h.assertTrue(FortressActivity.crossedGates(several,0,new Vec3(8,0,.5),new Vec3(8,-35,.5))==-1,"Obstacle crossing accepted");
        h.assertTrue(FortressActivity.crossedGates(several,3,new Vec3(.5,-265,.5),new Vec3(.5,-279,.5))==3,"Valid lava approach rejected");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=600) public static void mineStairTreadsAndLandings(GameTestHelper h){
        var f=ExpeditionGameTests.fixture(h,OtherMotherActivities.Activity.OTHER_MINESHAFT);var l=f.p().serverLevel();var c=f.c();
        var a=new MineMaze.Cell(0,3,6);var b=new MineMaze.Cell(1,4,7);var south=new MineMaze.Cell(1,4,6);
        MineMaze.build(l,c,new MineMaze.Layout(java.util.List.of(a,b,south),java.util.List.of(new MineMaze.Edge(a,b),new MineMaze.Edge(b,south))));
        for(int step=0;step<=14;step++){
            var at=c.offset(MineMaze.stairStart(new MineMaze.Edge(a,b))).offset(0,-step,-step);
            h.assertTrue(l.getBlockState(at.below()).is(Blocks.DEEPSLATE_BRICK_STAIRS),"Missing tread "+step);
            for(int y=0;y<3;y++)h.assertTrue(l.isEmptyBlock(at.above(y)),"Blocked stair headroom "+step+" / "+y);
        }
        for(int x=0;x<=8;x++)for(int y=0;y<2;y++){
            h.assertTrue(l.isEmptyBlock(c.offset(a.pos()).offset(x,y,0)),"Upper landing blocked");
            h.assertTrue(l.isEmptyBlock(c.offset(b.pos()).offset(x,y,0)),"Lower landing blocked");
        }
        h.assertTrue(l.getBlockState(c.offset(MineMaze.stairEnd(new MineMaze.Edge(a,b))).north()).is(Blocks.DEEPSLATE),"Stair end opens into void");
        for(int z=0;z<=14;z++)for(int y=0;y<2;y++)h.assertTrue(l.isEmptyBlock(c.offset(b.pos()).offset(0,y,z)),"Stair blocked the only lower-floor corridor");h.succeed();
    }
    @GameTest(template="empty") public static void mineLanternHandsRestoreAndAmethyst(GameTestHelper h){
        var f=ExpeditionGameTests.fixture(h,OtherMotherActivities.Activity.OTHER_MINESHAFT);var p=f.p();var s=f.s();
        p.getInventory().selected=0;p.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND));
        p.getInventory().setItem(1,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD));
        p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TORCH));
        MineshaftActivity.begin(p,s);var lamp=p.getInventory().getItem(1);p.getInventory().setItem(1,net.minecraft.world.item.ItemStack.EMPTY);p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,lamp);
        MineshaftActivity.enforceHands(p,s);h.assertTrue(p.getOffhandItem().isEmpty(),"Lantern allowed offhand");
        MineshaftActivity.restoreLantern(p,s);ExpeditionActivities.restore(p,s);
        h.assertTrue(p.getOffhandItem().is(net.minecraft.world.item.Items.TORCH)&&p.getInventory().getItem(1).is(net.minecraft.world.item.Items.EMERALD),"Original equipment lost");
        for(int i=0;i<p.getInventory().getContainerSize();i++)h.assertTrue(!ExpeditionActivities.temporary(p.getInventory().getItem(i)),"Loan escaped");
        h.assertTrue(MineshaftActivity.loot(8)==net.minecraft.world.item.Items.AMETHYST_SHARD,"Geode gives iron");h.succeed();
    }
    @GameTest(template="empty") public static void everyActivityIntroGateSkipsOnlyOnce(GameTestHelper h){
        for(var activity:OtherMotherActivities.Activity.values()){
            var s=new OtherMotherLoopState();s.activity=activity;
            s.villageGiftAt=s.archeryGiftAt=s.carnivalGiftAt=s.cakeVoiceUntil=s.fishingVoiceUntil=s.iceVoiceUntil=s.magicVoiceUntil=s.flyingGardenVoiceUntil=s.circusVoiceUntil=s.parkourVoiceUntil=s.gardenVoiceUntil=s.feastVoiceUntil=200;
            for(var n:new net.minecraft.nbt.CompoundTag[]{s.expedition,s.festival,s.temple,s.endCity,s.ocean})n.putLong("VoiceUntil",200);
            h.assertTrue(ActivityIntro.deadline(s)==200,"Missing intro gate: "+activity);
            h.assertTrue(ActivityIntro.finish(s,100),"Cannot skip: "+activity);
            h.assertTrue(ActivityIntro.deadline(s)==100&&!ActivityIntro.finish(s,100),"Repeat skip: "+activity);
            h.assertTrue(s.iceCountdownUntil==0&&s.flyingGardenHarvestUntil==0&&s.carnivalTokensRemaining==0&&s.expedition.getInt("Phase")==0,"Skip mutated gameplay");
        }h.succeed();
    }
    @GameTest(template="empty") public static void introSkipRejectsHelpersStaleVisitsAndMidGame(GameTestHelper h){
        var f=ExpeditionGameTests.fixture(h,OtherMotherActivities.Activity.OTHER_WOODLAND_MANSION);
        var p=f.p();var s=f.s();String sound=ActivityWorld.introSound(s.loop.activity);
        s.loop.expedition.putLong("VoiceUntil",OtherMotherLoop.now(p)+200);
        var helper=net.minecraftforge.common.util.FakePlayerFactory.get(p.serverLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"IntroHelper"));
        h.assertTrue(!ActivityIntro.eligible(helper,s,sound,s.loop.generation),"Helper can skip owner's intro");
        h.assertTrue(!ActivityIntro.skip(p,sound,s.loop.generation-1),"Stale visit accepted");
        h.assertTrue(ActivityIntro.skip(p,sound,s.loop.generation),"Owner skip rejected");
        h.assertTrue(!ActivityIntro.skip(p,sound,s.loop.generation),"Repeated skip accepted");
        s.loop.expedition.putInt("Phase",1);s.loop.expedition.putLong("VoiceUntil",OtherMotherLoop.now(p)+200);
        h.assertTrue(!ActivityIntro.skip(p,sound,s.loop.generation),"Gameplay skipped");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=600) public static void fortressPoolFloorClearsEveryLevelButObstaclesFail(GameTestHelper h){
        var f=ExpeditionGameTests.fixture(h,OtherMotherActivities.Activity.OTHER_NETHER_FORTRESS);
        var p=f.p();var s=f.s();var c=f.c();var n=s.loop.expedition;
        // Reproduce a fast landing that is already onGround on the pool base.
        for(int level=0;level<5;level++){
            n.putInt("FortressLevel",level);n.putInt("Phase",2);
            n.putInt("Passed",FortressActivity.generate(n.getLong("Seed"),level).size());
            n.putLong("DropAt",OtherMotherLoop.now(p));n.putDouble("PreviousY",-265);
            var pool=FortressDropper.pool(n.getLong("Seed"),level);n.putDouble("PreviousX",pool.x()+.5);n.putDouble("PreviousZ",pool.z()+.5);
            p.serverLevel().setBlock(c.offset(pool.x(),-280,pool.z()),Blocks.BLACKSTONE.defaultBlockState(),2);
            p.serverLevel().setBlock(c.offset(pool.x(),-279,pool.z()),Blocks.LAVA.defaultBlockState(),2);
            p.setPos(Vec3.atBottomCenterOf(c.offset(pool.x(),-279,pool.z())));p.setOnGround(true);
            FortressActivity.tick(p,s,c);
            h.assertTrue(n.getInt("Phase")== (level==4?3:1),"Lava floor failed level "+level+": "+n.getString("Failure"));
            if(level<4){
                h.assertTrue(n.getInt("FortressLevel")==level+1,"Did not advance");
                // Stale landing packet arriving after the clear must only reset boarding.
                p.setPos(Vec3.atBottomCenterOf(c.offset(0,-279,0)));p.setOnGround(true);
                FortressActivity.tick(p,s,c);
                h.assertTrue(n.getInt("Phase")==1,"Stale lava packet failed next level");
                // Forge FakePlayer ignores connection teleport; acknowledge the reset explicitly here.
                p.setPos(Vec3.atBottomCenterOf(c.south(4)));n.putLong("FortressResetUntil",OtherMotherLoop.now(p));FortressActivity.tick(p,s,c);
                h.assertTrue(n.getBoolean("FortressBoarded"),"Never finished boarding reset");
            }
        }
        n.putInt("Phase",2);n.putInt("FortressLevel",0);n.putInt("Passed",0);n.putDouble("PreviousY",-9);n.putLong("DropAt",OtherMotherLoop.now(p));
        p.setPos(Vec3.atBottomCenterOf(c.offset(0,-10,0)));p.setOnGround(true);
        n.putDouble("PreviousX",.5);n.putDouble("PreviousZ",.5);
        FortressActivity.tick(p,s,c);h.assertTrue(n.getInt("Phase")==2,"Stale ground flag in air failed");
        p.serverLevel().setBlock(c.offset(0,-11,0),Blocks.BLACKSTONE.defaultBlockState(),2);
        FortressActivity.tick(p,s,c);
        h.assertTrue(n.getInt("Phase")==5,"Solid obstacle counted as success");h.succeed();
    }
}
