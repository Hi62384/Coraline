package com.coraline.feast;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.phys.Vec3;
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class FortressDropperGameTests {
    @GameTest(template="empty") public static void poolsShrinkAndMissingThemFails(GameTestHelper h){
        for(int seed=0;seed<100;seed++)for(int level=0;level<5;level++){
            var pool=FortressDropper.pool(seed,level);h.assertTrue(pool.size()==13-level*2,"Pool does not shrink");h.assertTrue(pool.contains(pool.x()+.5,pool.z()+.5),"Pool centre unsafe");h.assertFalse(pool.contains(pool.x()+pool.size(),pool.z()),"Blackstone accepted");
        }
        var f=ExpeditionGameTests.fixture(h,OtherMotherActivities.Activity.OTHER_NETHER_FORTRESS);var n=f.s().loop.expedition;var p=f.p();
        n.putInt("Phase",2);n.putInt("FortressLevel",4);n.putDouble("PreviousY",-270);n.putDouble("PreviousX",14.5);n.putDouble("PreviousZ",14.5);n.putLong("DropAt",OtherMotherLoop.now(p));
        p.setPos(Vec3.atBottomCenterOf(f.c().offset(14,-279,14)));FortressActivity.tick(p,f.s(),f.c());h.assertTrue(n.getInt("Phase")==5&&n.getString("Failure").contains("missed"),"Missed pool won");h.succeed();
    }
    @GameTest(template="empty") public static void noStraightDownWinningColumn(GameTestHelper h){
        for(int seed=0;seed<1000;seed++)for(int level=0;level<5;level++){
            var pool=FortressDropper.pool(seed,level);var obstacles=FortressDropper.obstacles(seed,level);int r=pool.size()/2;
            for(int x=pool.x()-r;x<=pool.x()+r;x++)for(int z=pool.z()-r;z<=pool.z()+r;z++)
                h.assertTrue(FortressDropper.hit(obstacles,new Vec3(x+.5,0,z+.5),new Vec3(x+.5,-278,z+.5)),"Straight drop wins seed="+seed+" level="+level);
            var route=FortressActivity.generate(seed,level);
            int spanX=route.stream().mapToInt(FortressActivity.Gate::x).max().orElseThrow()-route.stream().mapToInt(FortressActivity.Gate::x).min().orElseThrow();
            int spanZ=route.stream().mapToInt(FortressActivity.Gate::z).max().orElseThrow()-route.stream().mapToInt(FortressActivity.Gate::z).min().orElseThrow();
            h.assertTrue(Math.max(spanX,spanZ)>=6,"Openings aligned");
        }h.succeed();
    }
    @GameTest(template="empty") public static void beamsAreSparseAndSideCollisionsCount(GameTestHelper h){
        int previous=0;
        for(int level=0;level<5;level++){var obstacles=FortressDropper.obstacles(917,level);h.assertTrue(obstacles.size()>previous,"No hazard density progression");previous=obstacles.size();
            for(var o:obstacles){h.assertTrue((o.X()-o.x()+1)*(o.Z()-o.z()+1)<170,"Full floor substituted for dropper props");h.assertTrue(o.X()-o.x()<20&&o.Z()-o.z()<20,"Repeated shaft-spanning beam");}
            var o=obstacles.get(0);var b=o.bounds();var point=b.getCenter();h.assertTrue(FortressDropper.hit(obstacles,point.add(-40,0,0),point.add(40,0,0)),"Batched side impact missed");
        }h.succeed();
    }
}
