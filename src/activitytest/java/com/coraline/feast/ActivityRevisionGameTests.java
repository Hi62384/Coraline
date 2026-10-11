package com.coraline.feast;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class ActivityRevisionGameTests {
    @GameTest(template="empty") public static void expandedMallPools(GameTestHelper h){
        h.assertTrue(MallStock.STORES.length==24&&MallStock.CATEGORIES.length==24,"Missing shop categories");
        for(var pool:MallStock.catalogue()){
            h.assertTrue(pool.size()>8,"Pool has no rotation");var ids=new HashSet<String>();
            for(var item:pool){h.assertTrue(ids.add(item.id()),"Duplicate catalogue product");h.assertTrue(!MallStock.stack(item).isEmpty(),"Unregistered catalogue product");}
        }
        for(int seed=0;seed<80;seed++){
            var list=MallStock.generate(seed,false,false);h.assertTrue(list.size()==24*8,"Not eight products per shop");
            int[] counts=new int[24];for(var tag:list)counts[((net.minecraft.nbt.CompoundTag)tag).getInt("Store")]++;
            for(int count:counts)h.assertTrue(count==8,"Unbalanced selection");
        }
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=600) public static void mallCrowdFloorsAndStockAboveOldLimit(GameTestHelper h){
        var f=ExpeditionGameTests.fixture(h,OtherMotherActivities.Activity.SHOPPING_MALL);var p=f.p();var s=f.s();var c=f.c();
        ShoppingMallActivity.build(p,s,p.serverLevel(),c);s.loop.expedition.putInt("Phase",1);
        var shoppers=p.serverLevel().getEntitiesOfClass(FishingContestNpcEntity.class,ExpeditionActivities.bounds(c,s.loop.activity),e->ActivityScenery.owned(e,s)&&e.getPersistentData().getBoolean("MallShopper"));
        h.assertTrue(shoppers.size()==48,"Crowd not populated");var positions=new HashSet<BlockPos>();int upper=0;
        for(var npc:shoppers){var at=npc.blockPosition();h.assertTrue(positions.add(at),"Overlapping shopper spawns");h.assertTrue(!p.level().isEmptyBlock(at.below())&&p.level().isEmptyBlock(at)&&p.level().isEmptyBlock(at.above()),"Shopper in furniture or void");if(at.getY()==c.getY()+8)upper++;}
        h.assertTrue(upper==24,"Upper mall empty");
        var stock=s.loop.expedition.getList("Stock",10);int last=stock.size()-1;var entry=stock.getCompound(last);
        var prop=p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,ExpeditionActivities.bounds(c,s.loop.activity),e->e.slot==1000+last).get(0);
        p.setPos(Vec3.atCenterOf(MallScenery.display(c,23,7)).add(0,0,2));
        h.assertTrue(ShoppingMallActivity.interactProp(p,s,prop)&&s.loop.expedition.getInt("Selected")==last,"New shop outside clickable slot range");
        h.assertTrue(ShoppingMallActivity.purchase(s,p.getUUID(),last),"New product unbuyable");h.succeed();
    }
    @GameTest(template="empty") public static void activityFathersSurviveCompletedHouseScene(GameTestHelper h){
        for(var activity:new OtherMotherActivities.Activity[]{OtherMotherActivities.Activity.SHOPPING_MALL,OtherMotherActivities.Activity.SNOWBALL_BATTLE,OtherMotherActivities.Activity.TREASURE_HUNT}){
            var f=ExpeditionGameTests.fixture(h,activity);var at=f.c().east(2);LoopProps.hold(f.p().serverLevel(),at);
            ActivityScenery.father(f.p(),f.s(),f.p().serverLevel(),at);
            h.runAfterDelay(5,()->{
            var dads=f.p().serverLevel().getEntitiesOfClass(com.coraline.parents.ParentEntity.class,new AABB(at).inflate(3),e->ActivityScenery.owned(e,f.s()));
            h.assertTrue(dads.size()==1,"Father missing at entrance");var dad=dads.get(0);
            for(int i=0;i<25;i++)dad.tick();
            h.assertTrue(dad.isAlive()&&!dad.isRemoved()&&!dad.isInvisible()&&dad.distanceToSqr(Vec3.atBottomCenterOf(at))<.1,"House cleanup removed or moved activity father");
            });
        }h.runAfterDelay(6,h::succeed);
    }
    @GameTest(template="empty") public static void snowArenaAndReachablePrize(GameTestHelper h){
        var c=BlockPos.ZERO;
        for(int x=-37;x<=37;x++)for(int z=-75;z<=11;z++)for(int y=0;y<=8;y++)h.assertTrue(!SnowballBattleActivity.needsRecovery(c,new Vec3(x+.5,y,z+.5)),"Valid arena resets player");
        h.assertTrue(SnowballBattleActivity.needsRecovery(c,new Vec3(0,-8,-40)),"Void recovery disabled");
        var prize=SnowballBattleActivity.prizePosition(c);var eyes=Vec3.atBottomCenterOf(c.offset(-10,0,-2)).add(0,1.62,0);
        h.assertTrue(prize.distanceTo(eyes)<3&&prize.y<1.6,"Prize requires climbing");h.succeed();
    }
    @GameTest(template="empty") public static void snowThrowsFollowVanillaGravity(GameTestHelper h){
        for(int range=6;range<=32;range+=2){var target=new Vec3(range,0,0);var v=SnowballBattleActivity.throwVelocity(Vec3.ZERO,target,Vec3.ZERO,1);var at=Vec3.ZERO;double nearest=100;
            for(int i=0;i<40;i++){at=at.add(v);v=v.scale(.99).add(0,-.03,0);nearest=Math.min(nearest,at.distanceTo(target));}
            h.assertTrue(nearest<.01,"NPC aim misses stationary target at "+range+": "+nearest);
        }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=600) public static void snowRallyNeverMovesPlayer(GameTestHelper h){
        var f=ExpeditionGameTests.fixture(h,OtherMotherActivities.Activity.SNOWBALL_BATTLE);var p=f.p();var s=f.s();
        SnowballBattleActivity.build(p,s,p.serverLevel(),f.c());SnowballBattleActivity.begin(p,s);var n=s.loop.expedition;n.putInt("Phase",2);
        var at=Vec3.atBottomCenterOf(f.c().offset(36,0,-72));p.setPos(at);
        for(int age:new int[]{100,3600,4800}){n.putLong("BattleAt",OtherMotherLoop.now(p)-age);SnowballBattleActivity.tick(p,s,f.c());h.assertTrue(p.position().distanceToSqr(at)<.001,"Battle teleported a valid player");}h.succeed();
    }
    @GameTest(template="empty") public static void treasureProceduralFactsAndEarlyExit(GameTestHelper h){
        var layouts=new HashSet<String>();var verses=new HashSet<String>();
        for(int seed=0;seed<1000;seed++){var facts=TreasureRoute.gardens(seed);h.assertTrue(new HashSet<>(facts).size()==12,"Ambiguous clue solution");layouts.add(facts.toString());var route=TreasureRoute.generate(seed);
            for(int stage=0;stage<6;stage++){var clue=TreasureRoute.clue(seed,route,stage);h.assertTrue(clue.length()<320&&!clue.contains(TreasureRoute.LANDMARKS.get(route[stage]).name()),"Clue reveals destination");verses.add(clue);}}
        h.assertTrue(layouts.size()>990&&verses.size()>100,"Replays reuse same riddles");
        var f=ExpeditionGameTests.fixture(h,OtherMotherActivities.Activity.TREASURE_HUNT);f.s().loop.expedition.putInt("Phase",1);h.assertTrue(ActivityWorld.exitReady(f.s()),"Exit locked until hunt solved");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=600) public static void mansionWitnessesAndStairEnclosure(GameTestHelper h){
        var f=ExpeditionGameTests.fixture(h,OtherMotherActivities.Activity.OTHER_WOODLAND_MANSION);var l=f.p().serverLevel();MansionActivity.build(f.p(),f.s(),l,f.c());
        var mobs=l.getEntitiesOfClass(Mob.class,ExpeditionActivities.bounds(f.c(),f.s().loop.activity),e->ActivityScenery.owned(e,f.s())&&e.getPersistentData().getInt("MansionClue")>0);
        h.assertTrue(mobs.size()==6,"Missing witnesses");var previous=l.getDifficulty();
        try{f.p().server.setDifficulty(net.minecraft.world.Difficulty.PEACEFUL,true);for(var mob:mobs){mob.checkDespawn();h.assertTrue(!mob.isRemoved()&&mob.isNoGravity()&&mob.getCustomName().getString().startsWith("Button-Eyed "),"Witness despawned or lacks visual marker");}}
        finally{f.p().server.setDifficulty(previous,true);}
        mobs.get(0).discard();MansionActivity.ensureWitnesses(f.p(),f.s(),f.c());
        h.assertTrue(l.getEntitiesOfClass(Mob.class,ExpeditionActivities.bounds(f.c(),f.s().loop.activity),e->ActivityScenery.owned(e,f.s())&&e.getPersistentData().getInt("MansionClue")>0).size()==6,"Missing witness not recovered");
        for(int z=-83;z<=-82;z++)for(int y=9;y<=12;y++)for(int x:new int[]{-7,7})h.assertTrue(!l.isEmptyBlock(f.c().offset(x,y,z)),"Open side of stair landing");h.succeed();
    }
    @GameTest(template="empty") public static void signContrast(GameTestHelper h){
        var l=h.getLevel();var at=h.absolutePos(new BlockPos(1,2,1));l.setBlock(at,Blocks.DARK_OAK_WALL_SIGN.defaultBlockState(),2);
        var sign=(SignBlockEntity)l.getBlockEntity(at);var text=ActivityBlocks.readable(sign,sign.getFrontText());h.assertTrue(text.hasGlowingText()&&text.getColor()==DyeColor.WHITE,"Dark sign text still unreadable");h.succeed();
    }
    @GameTest(template="empty") public static void fortressFiveReachableNormalGravityLevels(GameTestHelper h){
        for(int seed=0;seed<1000;seed++)for(int level=0;level<5;level++)for(double response:new double[]{1,.85}){
            var gates=FortressActivity.generate(seed,level);h.assertTrue(gates.size()==7+level*2,"No density progression");
            var obstacles=FortressDropper.obstacles(seed,level);double x=.5,z=-.5,y=-.5,vx=0,vz=0,vy=0;int next=0;
            for(int tick=0;tick<200&&y> -278;tick++){
                var g=gates.get(Math.min(next,gates.size()-1));double dx=g.x()+.5-x,dz=g.z()+.5-z,length=Math.max(1,Math.hypot(dx,dz));
                vx+=dx/length*.0196*response;vz+=dz/length*.0196*response;
                double oldY=y;var from=new net.minecraft.world.phys.Vec3(x,y,z);x+=vx;z+=vz;y+=vy;
                h.assertFalse(FortressDropper.hit(obstacles,from,new net.minecraft.world.phys.Vec3(x,y,z)),"Dropper obstruction seed="+seed+" level="+level+" response="+response+" stage="+next);vx*=.91;vz*=.91;vy=(vy-.08)*.98;
                if(next<gates.size()&&y<=g.y()+1.01&&oldY>g.y()+1.01)next++;
            }
            h.assertTrue(next==gates.size()&&FortressDropper.pool(seed,level).contains(x,z),"Course cannot land safely seed="+seed+" level="+level);
        }h.succeed();
    }
}
