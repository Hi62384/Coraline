package com.coraline.feast;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Rule tests only. The real venue belongs in a separate disposable developer test world. */
@GameTestHolder("coraline")
@PrefixGameTestTemplate(false)
public final class ScriptedShowGameTests {
    @GameTest(template="empty") public static void theatreFurnitureSurvivesProtectedRealmSpawn(GameTestHelper h){
        var level=h.getLevel().getServer().getLevel(ActivityWorld.KEY);
        require(level!=null,"Activity dimension missing");
        var s=new VisitData.Scene();s.owner=UUID.randomUUID();s.door=UUID.randomUUID();s.loop.event.putInt("Serial",1);
        var at=new net.minecraft.core.BlockPos(78000,180,78000);var chunk=new net.minecraft.world.level.ChunkPos(at);
        // Match a real stage's neighboring entity-ticking chunks. One forced
        // corner chunk can queue an entity without admitting it to the level.
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){level.setChunkForced(chunk.x+x,chunk.z+z,true);level.getChunk(chunk.x+x,chunk.z+z);}
        VisitData.get(level.getServer()).scenes.put(s.owner,s);var props=new ArrayList<CinematicPropEntity>();
        s.table=at;s.piano=at;
        h.runAtTickTime(20,()->{
        for(int kind:new int[]{CinematicPropEntity.SEAT,CinematicPropEntity.DOG_ROW,CinematicPropEntity.CURTAIN}){
            var prop=CinematicPropEntity.TYPE.get().create(level);require(prop!=null,"Furniture couldn't be created");
            prop.configure(s,kind,0);prop.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);
            require(level.addFreshEntity(prop),"Realm spawn protection discarded theatre furniture");
            props.add(prop);
        }
        });
        h.runAtTickTime(50,()->{try{for(var prop:props)h.assertTrue(level.getEntity(prop.getUUID())==prop&&!prop.isRemoved(),"Furniture wasn't admitted: kind="+prop.kind()+", removed="+prop.isRemoved()+", reason="+prop.getRemovalReason()+", scene="+VisitData.get(level.getServer()).scenes.containsKey(s.owner));h.assertTrue(TheatreSite.ROWS*TheatreSite.SEATS_PER_ROW>=280,"Theatre lost its full audience rows");h.succeed();}finally{for(var prop:props)prop.discard();VisitData.get(level.getServer()).scenes.remove(s.owner);for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)level.setChunkForced(chunk.x+x,chunk.z+z,false);}});
    }
    @GameTest(template="empty") public static void cinematicAndBicycleTypesAreDeferredAndCreatable(GameTestHelper h){
        var prop=CinematicPropEntity.TYPE.get().create(h.getLevel());
        var bike=WybieAdventureEntities.BICYCLE.get().create(h.getLevel());
        require(prop!=null&&bike!=null,"Deferred event entities could not be created");
        require(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(prop.getType()).toString().equals("coraline:cinematic_prop"),"Cinematic saved ID changed");
        require(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(bike.getType()).toString().equals("coraline:wybie_bicycle"),"Bicycle saved ID changed");
        require(CircusSouvenirs.COTTON_CANDY.get().isEdible()&&CircusSouvenirs.POPCORN.get().isEdible(),"Circus souvenirs did not register");
        prop.discard();bike.discard();h.succeed();
    }
    private static Map<String,Integer> fixture(){var n=new HashMap<String,Integer>();n.put("theatre_music",2000);n.put("theatre_a",151);n.put("theatre_b",61);n.put("theatre_c",122);n.put("theatre_d",183);n.put("theatre_e",450);n.put("theatre_f",689);n.put("mouse_circus_intro",545);n.put("mouse_circus_music",2000);n.put("mouse_circus_end",415);return n;}
    private static long at(ScriptedShowTimeline p,String cue){return p.cues().stream().filter(c->c.id().equals(cue)).findFirst().orElseThrow().tick();}
    private static void require(boolean value,String label){if(!value)throw new AssertionError(label);}
    @GameTest(template="empty") public static void openingNeverLeavesStageOrCollidesPerformers(GameTestHelper h){
        for(int tick=0;tick<=4000;tick++){
            var a=TheatreOpeningChoreography.position(-1,tick/4000.0);var b=TheatreOpeningChoreography.position(1,tick/4000.0);
            for(var p:List.of(a,b))require(Double.isFinite(p.x())&&Double.isFinite(p.y())&&Double.isFinite(p.z())&&Math.abs(p.x())<=18.001&&p.y()>=1&&p.y()<=2.801&&p.z()>=-39.001&&p.z()<=-25.999,"Opening left its supported stage envelope");
            require(Math.hypot(b.x()-a.x(),b.z()-a.z())>=2.19,"Opening performers intersect");
        }
        require(TheatreOpeningChoreography.position(-1,1).pose()==7&&TheatreOpeningChoreography.position(1,1).pose()==7,"Finale did not settle into bows");h.succeed();
    }
    @GameTest(template="empty") public static void openingActBoundariesHaveNoPositionJumps(GameTestHelper h){
        for(double boundary:new double[]{.14,.30,.52,.82})for(int side:new int[]{-1,1}){
            var before=TheatreOpeningChoreography.position(side,boundary-.000001);var after=TheatreOpeningChoreography.position(side,boundary+.000001);
            double distance=Math.sqrt(Math.pow(before.x()-after.x(),2)+Math.pow(before.y()-after.y(),2)+Math.pow(before.z()-after.z(),2));require(distance<.002,"Act boundary teleported a performer");
        }
        boolean rejected=false;try{TheatreOpeningChoreography.position(1,Double.NaN);}catch(IllegalArgumentException invalid){rejected=true;}require(rejected,"Non-finite pose input was accepted");h.succeed();
    }
    @GameTest(template="empty") public static void exactRelativeTheatreCues(GameTestHelper h){var p=ScriptedShowTimeline.theatre(fixture());require(at(p,"D_FREEZE")==p.segment("D").start()+50,"D freeze");require(at(p,"E_PLAYER_APPROACH")==p.segment("E").end()-20,"E approach");require(at(p,"E_PLAYER_GRAB")==p.segment("E").end(),"E grab");require(at(p,"F_THROW")==p.segment("F").start()+480,"F throw");h.succeed();}
    @GameTest(template="empty") public static void circusMusicOwnsFinalPose(GameTestHelper h){var p=ScriptedShowTimeline.circus(fixture());require(at(p,"INTRO_REVEAL")==p.segment("INTRO").end()-60,"reveal final three seconds");require(at(p,"MUSIC_END")==p.segment("MUSIC").end(),"music end");require(at(p,"BOBINSKY_APPEAR")==at(p,"MUSIC_END"),"Bobinsky at final end only");for(int i=0;i<48;i++)require(ScriptedShowTimeline.mouse(i,48,1).equals(ScriptedShowTimeline.star(i,48)),"final mouse pose");h.succeed();}
    @GameTest(template="empty") public static void missingRecordingsNeverGetGuessed(GameTestHelper h){var values=fixture();values.remove("theatre_music");boolean rejected=false;try{ScriptedShowTimeline.theatre(values);}catch(IllegalArgumentException missing){rejected=true;}require(rejected,"missing original must block");values=fixture();values.put("theatre_f",479);rejected=false;try{ScriptedShowTimeline.theatre(values);}catch(IllegalArgumentException incompatible){rejected=true;}require(rejected,"incompatible F must block");h.succeed();}
    @GameTest(template="empty") public static void throwAndCatchAreFiniteAndSafe(GameTestHelper h){for(int tick=0;tick<689;tick++){var p=ScriptedShowTimeline.theatrePlayer(tick,689);require(Double.isFinite(p.x())&&Double.isFinite(p.y())&&Double.isFinite(p.z()),"finite stunt");require(p.y()>=.25,"no uncontrolled pit");}require(ScriptedShowTimeline.theatrePlayer(540,689).y()==1.05,"caught above pool");h.succeed();}
}
