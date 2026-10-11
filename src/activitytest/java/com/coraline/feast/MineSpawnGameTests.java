package com.coraline.feast;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class MineSpawnGameTests {
    @GameTest(template="empty") public static void fewerEnemiesAndQuietUpperFloors(GameTestHelper h){
        int previous=0,interval=1000;
        for(int depth=0;depth<4;depth++){
            h.assertTrue(MineThreats.limit(depth)>previous&&MineThreats.limit(depth)<4+depth*2,"Spawn caps not reduced/progressive");
            h.assertTrue(MineThreats.spawnInterval(depth)<interval&&MineThreats.spawnInterval(depth)>120-depth*25,"Spawn intervals not reduced/progressive");
            previous=MineThreats.limit(depth);interval=MineThreats.spawnInterval(depth);
        }
        var f=ExpeditionGameTests.fixture(h,OtherMotherActivities.Activity.OTHER_MINESHAFT);var p=f.p();var s=f.s();
        p.setPos(f.c().getX()+30.5,f.c().getY(),f.c().getZ()+.5);p.tickCount=5;
        s.loop.expedition.putInt("Phase",1);s.loop.expedition.putInt("Hearts",5);s.loop.expedition.putLong("NextSpawn",Long.MAX_VALUE);
        for(int i=0;i<6;i++){var mob=EntityType.ZOMBIE.create(p.serverLevel());ActivityScenery.mark(mob,s);mob.getPersistentData().putBoolean("MineEnemy",true);mob.getPersistentData().putLong("Born",OtherMotherLoop.now(p));mob.setNoGravity(true);mob.setNoAi(true);mob.setPos(p.position().add(i+8,0,0));p.serverLevel().setChunkForced(mob.blockPosition().getX()>>4,mob.blockPosition().getZ()>>4,true);p.serverLevel().addFreshEntity(mob);}
        h.runAfterDelay(10,()->{MineThreats.tick(p,s,f.c(),OtherMotherLoop.now(p));
        var alive=p.serverLevel().getEntitiesOfClass(Mob.class,ExpeditionActivities.bounds(f.c(),s.loop.activity),e->ActivityScenery.owned(e,s)&&e.getPersistentData().getBoolean("MineEnemy"));
        h.assertTrue(alive.size()==2,"Ascending population="+alive.size());h.succeed();});
    }
}
