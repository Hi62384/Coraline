package com.coraline.feast;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class NewActivitiesGameTests {
    @GameTest(template="empty") public static void privateArenaFootprints(GameTestHelper h){
        var boxes=List.of(FestivalGrounds.bounds(new BlockPos(0,80,600)),DesertTempleCourse.bounds(new BlockPos(300,80,600)),EndCityCourse.bounds(new BlockPos(600,80,600)));
        for(int i=0;i<boxes.size();i++){var a=boxes.get(i);h.assertTrue(a.minZ>250&&a.maxZ<1000&&a.minX> -1000&&a.maxX<1000,"Arena leaves owner's reserved strip");
            for(int j=i+1;j<boxes.size();j++)h.assertTrue(!a.inflate(10).intersects(boxes.get(j)),"Arenas overlap");
            for(var b:boxes)for(int dx:new int[]{-2048,0,2048})for(int dz:new int[]{-2048,0,2048})if(dx!=0||dz!=0)h.assertTrue(!a.inflate(32).intersects(b.move(dx,0,dz)),"Neighbor owner's arena overlap");
        }h.succeed();
    }
    @GameTest(template="empty") public static void templeThousandsOfSolvableRoutes(GameTestHelper h){
        for(int room=0;room<5;room++){var unique=new HashSet<List<TemplePath.Tile>>();
            for(int seed=0;seed<1000;seed++){var route=TemplePath.generate(room,seed);unique.add(route);
                h.assertTrue(TemplePath.valid(room,route),"Invalid path");h.assertTrue(route.size()>=TemplePath.target(room)&&route.size()<=TemplePath.target(room)+2,"Wrong difficulty length");
                int next=0;for(var tile:route){next=TemplePath.step(route,next,tile);h.assertTrue(next>0,"Valid step rejected");}
                h.assertTrue(TemplePath.step(route,0,route.get(2))==-1,"Skipped path accepted");h.assertTrue(TemplePath.step(route,0,new TemplePath.Tile(-1,0))==-1,"Outside tile accepted");
            }h.assertTrue(unique.size()>=new int[]{40,20,180,100,500}[room],"Routes repeat too often");
        }h.succeed();
    }
    @GameTest(template="empty") public static void festivalChartsAndScoring(GameTestHelper h){
        var unique=new HashSet<String>();
        for(int track=0;track<FestivalChart.count();track++){var song=FestivalChart.track(track);unique.add(song.resource());int previous=-100;
            h.assertTrue(song.duration()==3600&&song.notes().size()>60,"Missing full-length song/chart");
            for(var note:song.notes()){h.assertTrue(note.tick()-previous>=5,"Unplayable input burst");h.assertTrue(note.tick()<song.duration()-20&&note.lane()>=0&&note.lane()<4&&note.midi()>=36&&note.midi()<=96,"Invalid song note");h.assertTrue(!note.tones().isEmpty()&&note.tones().size()<=3,"Missing or excessive chord voices");for(int tone:note.tones())h.assertTrue(tone>=36&&tone<=96,"Invalid arrangement pitch");previous=note.tick();}
            h.assertTrue(song.score().isEmpty(),"Unwanted background notes");
            h.assertTrue(FestivalChart.approach(0,song.duration())>FestivalChart.approach(song.duration()/2,song.duration())&&FestivalChart.approach(song.duration()/2,song.duration())>FestivalChart.approach(song.duration(),song.duration()),"Scroll speed did not progress");
            long early=song.notes().stream().filter(n->n.tick()<900).count(),late=song.notes().stream().filter(n->n.tick()>=2700).count();h.assertTrue(early>=15&&late>=15,"Missing playable opening/finale"); // Preserve real rests instead of fabricating 120 finale attacks.
            for(var audio:song.score()){h.assertTrue(audio.tones().size()<=4&&audio.tick()<song.duration(),"Invalid score event");if(audio.prompt()>=0)h.assertTrue(audio.tones().contains(song.notes().get(audio.prompt()).midi()),"Prompt missing from song");}
            for(int seed=0;seed<1000;seed++)h.assertTrue(FestivalChart.choose(seed,track)!=track,"Consecutive song repeats");
        }
        h.assertTrue(unique.size()==4,"Song rotation missing");var perfect=new FestivalChart.Score();var spam=new FestivalChart.Score();var bad=new FestivalChart.Score();
        for(int i=0;i<200;i++){perfect.hit(true);spam.hit(true);for(int j=0;j<3;j++)spam.miss(true);bad.miss(false);}
        h.assertTrue(perfect.crowd==48&&perfect.accuracy()==100,"Perfect performance cannot fill stage");h.assertTrue(spam.crowd<5&&spam.accuracy()<30,"Spam wins");h.assertTrue(bad.crowd==0,"Bad performance retains crowd");h.succeed();
    }
    @GameTest(template="empty") public static void endCityActiveCircuits(GameTestHelper h){
        for(int room=0;room<5;room++){
            var unique=new HashSet<List<BlockPos>>();
            for(int seed=0;seed<500;seed++)for(int stage=0;stage<EndCityCourse.stages(room);stage++){
                var route=EndCityChallenge.route(seed,room,stage);unique.add(route);
                h.assertTrue(new HashSet<>(route).size()==route.size(),"Duplicate rune");
                h.assertTrue(route.size()==(room<2?3:4),"Wrong difficulty");
                for(var at:route)h.assertTrue(Math.abs(at.getX())<EndCityCourse.radius(room)&&Math.abs(at.getZ())<EndCityCourse.radius(room),"Rune outside arena");
                for(int i=1;i<route.size();i++)h.assertTrue(route.get(i).distSqr(route.get(i-1))>=36,"Standing still collects runes");
            }
            h.assertTrue(unique.size()>10,"Routes lack variety");
            int cycle=EndCityChallenge.cycle(room);
            h.assertTrue(!EndCityChallenge.firing(0,room)&&EndCityChallenge.firing(cycle-1,room),"Beam lacks warning");
        }
        h.assertTrue(EndCityChallenge.DURATION==6000&&EndCityChallenge.HEARTS==3,"Missing stakes");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=600) public static void endCityGeometryHasSafeDestinations(GameTestHelper h){
        var c=new BlockPos(4500,80,4000);var l=h.getLevel().getServer().getLevel(ActivityWorld.KEY);h.assertTrue(l!=null,"Activity dimension missing");EndCityCourse.build(l,c);
        for(int room=0;room<5;room++)for(var d:EndCityCourse.candidates(room))h.assertTrue(EndCityCourse.safe(l,EndCityCourse.room(c,room),d),"Unsafe destination room "+room+": "+d);
        for(int room=0;room<5;room++)for(var rune:EndCityChallenge.route(42,room,0))
            h.assertTrue(EndCityCourse.safe(l,EndCityCourse.room(c,room),new EndCityCourse.Destination(rune.getX(),0,rune.getZ(),0)),"Rune unreachable");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=600) public static void endCityCannotIdleToWin(GameTestHelper h){
        var f=ExpeditionGameTests.fixture(h,OtherMotherActivities.Activity.OTHER_END_CITY);var p=f.p();var s=f.s();var c=EndCityActivity.center(p,s);EndCityCourse.build(p.serverLevel(),c);
        var n=EndCityActivity.state(s);n.putInt("Phase",1);n.putInt("Hearts",3);n.putLong("Deadline",OtherMotherLoop.now(p)+6000);n.putLong("Seed",1729);
        var fruit=new net.minecraft.world.item.ItemStack(com.coraline.registry.ModItems.ACTIVITY_CHORUS_FRUIT.get());fruit.getOrCreateTag().putUUID("Owner",s.owner);fruit.getOrCreateTag().putInt("Generation",s.loop.generation);
        var b=EndCityCourse.room(c,0);p.setPos(Vec3.atBottomCenterOf(b));
        for(int i=0;i<20;i++){n.putLong("NextEat",0);EndCityActivity.eat(p,fruit);}
        h.assertTrue(n.getInt("Stage")==0&&n.getInt("Phase")==1,"Idle fruit spam escapes");
        n.putLong("InvulnerableUntil",Long.MAX_VALUE);
        for(var rune:EndCityChallenge.route(1729,0,0)){p.setPos(Vec3.atBottomCenterOf(b.offset(rune)));EndCityActivity.tick(p,s,c);}
        h.assertTrue(n.getInt("Rune")==3,"Movement did not activate runes");
        p.setPos(Vec3.atBottomCenterOf(b));n.putLong("NextEat",0);EndCityActivity.eat(p,fruit);
        h.assertTrue(n.getInt("Phase")==2,"Earned teleport failed");
        n.putLong("Deadline",OtherMotherLoop.now(p));EndCityActivity.tick(p,s,c);
        h.assertTrue(n.getInt("Phase")==5&&EndCityActivity.exitReady(s)&&!EndCityActivity.takePrize(s,s.owner),"Timeout can win or softlocks");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=600) public static void templeGeometryAndGates(GameTestHelper h){
        var c=new BlockPos(4200,80,4000);var l=h.getLevel().getServer().getLevel(ActivityWorld.KEY);h.assertTrue(l!=null,"Activity dimension missing");DesertTempleCourse.build(l,c);
        for(int room=0;room<5;room++){int n=TemplePath.size(room);for(int x=0;x<n;x++)for(int z=0;z<n;z++){
            var at=DesertTempleCourse.tile(c,room,new TemplePath.Tile(x,z));h.assertTrue(l.getBlockState(at).is(Blocks.STONE_PRESSURE_PLATE),"Covered plate "+room);h.assertTrue(l.isEmptyBlock(at.above()),"Blocked headroom");}
            var gate=c.offset(0,3,DesertTempleCourse.top(room)+4);h.assertTrue(l.getBlockState(gate).is(Blocks.BARRIER),"Preview gate absent");DesertTempleCourse.gate(l,c,room,false);h.assertTrue(l.isEmptyBlock(gate),"Gate never opened");
        }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=600) public static void festivalStageAndFountainContainment(GameTestHelper h){
        var c=new BlockPos(4000,80,4000);var l=h.getLevel().getServer().getLevel(ActivityWorld.KEY);h.assertTrue(l!=null,"Activity dimension missing");FestivalGrounds.build(l,c);
        h.assertTrue(l.getBlockState(c.offset(0,0,-26)).is(Blocks.DARK_OAK_PLANKS),"Player station has no platform");
        for(int[] fountain:new int[][]{{-15,-35},{15,-51}})for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)if(Math.abs(dx)==3||Math.abs(dz)==3)h.assertTrue(l.getBlockState(c.offset(fountain[0]+dx,0,fountain[1]+dz)).is(Blocks.SMOOTH_QUARTZ),"Fountain leaks");h.succeed();
    }
    @GameTest(template="empty") public static void rewardOwnershipAndReplayGuards(GameTestHelper h){
        var s=new VisitData.Scene();s.owner=UUID.randomUUID();s.door=UUID.randomUUID();s.loop.temple.putInt("Phase",4);s.loop.endCity.putInt("Phase",3);
        h.assertTrue(!DesertTempleActivity.takePrize(s,UUID.randomUUID()),"Helper claimed temple reward");h.assertTrue(DesertTempleActivity.takePrize(s,s.owner)&&!DesertTempleActivity.takePrize(s,s.owner),"Temple reward duplication");
        h.assertTrue(!EndCityActivity.takePrize(s,UUID.randomUUID()),"Helper claimed End reward");h.assertTrue(EndCityActivity.takePrize(s,s.owner)&&!EndCityActivity.takePrize(s,s.owner),"End reward duplication");
        s.loop.festival.putInt("Phase",2);h.assertTrue(!MusicFestivalActivity.exitReady(s),"Active rhythm game exposes exit");s.loop.festival.putInt("Phase",3);h.assertTrue(MusicFestivalActivity.exitReady(s),"Festival exit unavailable");
        h.succeed();
    }
}
