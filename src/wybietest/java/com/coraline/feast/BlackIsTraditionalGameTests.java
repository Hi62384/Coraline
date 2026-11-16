package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.registry.ModEntities;
import com.coraline.secrets.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class BlackIsTraditionalGameTests {
    /** Declared authored bedroom slice; execute every real quarter-block push.
     * Only the click cooldown is advanced by the fixture, not the win flags. */
    private static void completeBarricadeFixture(ServerPlayer p,VisitData.Scene s,BlockPos house){
        var room=PartyRegions.rooms().stream().filter(r->r.id().equals("child_bedroom")).findFirst().orElseThrow();var level=p.serverLevel();
        for(int x=room.min()[0]-1;x<=room.max()[0]+1;x++)for(int z=room.min()[2]-1;z<=room.max()[2]+1;z++){
            var floor=HouseCabinets.at(house,x,room.floor(),z);level.getChunkAt(floor);level.setBlock(floor,Blocks.STONE.defaultBlockState(),2);
            for(int y=1;y<=3;y++)level.setBlock(floor.above(y),Blocks.AIR.defaultBlockState(),2);
        }
        var door=HouseCabinets.at(house,room.min()[0],room.floor()+1,(room.min()[2]+room.max()[2])/2);
        level.setBlock(door,Blocks.OAK_DOOR.defaultBlockState(),2);level.setBlock(door.above(),Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),2);
        p.setPos(Vec3.atBottomCenterOf(room.center(house)));BedroomBarricade.tick(p,s);
        for(int piece=0;piece<3;piece++){
            final int index=piece;
            var chair=VisitEvents.props(p,s).stream().filter(e->e.getPersistentData().getBoolean("BedroomBarricade")&&e.getPersistentData().getInt("BarricadePiece")==index).findFirst().orElseThrow();
            for(int push=0;push<256&&!chair.getPersistentData().getBoolean("BarricadePlaced");push++){p.setPos(chair.position());BlackIsTraditional.data(s).getCompound("Barricade").putLong("NextPush",0);BedroomBarricade.interact(p,s,chair);}
        }
        if(!BedroomBarricade.done(s))throw new AssertionError("Actual push interactions did not complete the barricade");
    }
    @GameTest(template="empty") public static void recordingsAndExactCueOffsets(GameTestHelper h){int count=0;for(var phase:BlackIsTraditional.Phase.values()){String sound=BlackIsTraditional.sound(phase);if(sound.isEmpty())continue;count++;h.assertTrue(ShowRecordings.duration(sound)>0,"Missing original recording "+sound);}h.assertTrue(count==19,"A-S sequence count changed");for(var entry:Map.of("j",20,"l",40,"o",40,"q",60,"r",30).entrySet())h.assertTrue(ShowRecordings.duration("black_is_traditional_"+entry.getKey())>entry.getValue(),"Original shorter than required timed cue");h.assertFalse(BlackIsTraditional.threshold(.999F),"Story starts below threshold");h.assertTrue(BlackIsTraditional.threshold(1F),"Clamped trust never triggers");h.succeed();}
    static void finishAudio(ServerPlayer p,VisitData.Scene s){BlackIsTraditional.tick(p,s);var n=ShowPlayback.data(s);String sound=n.getString("Sound");if(sound.isEmpty())throw new GameTestAssertException("Expected original recording at "+BlackIsTraditional.phase(s)+" / "+BlackIsTraditional.data(s));ShowPlayback.signal(p,n.getUUID("Nonce"),sound,0);n.putInt("Position",n.getInt("Duration")-1);ShowPlayback.signal(p,n.getUUID("Nonce"),sound,1);PlaybackFixtureClock.elapseOriginal(s);BlackIsTraditional.tick(p,s);}
    @GameTest(template="empty",timeoutTicks=220) public static void ownerDecisionAndFullServerCheckpointSequence(GameTestHelper h){
        var p=EventRevisionGameTests.fixture(h);var s=OtherMotherLoop.scene(p);var l=p.serverLevel();s.table=new BlockPos(104000,180,104000);s.piano=s.table.east(12);p.setPos(s.table.getCenter());var r=new RealmData.Realm();r.door=s.door;r.house=s.table;r.doorPos=s.table.west(18);r.porch=s.table.north(20);r.table=s.table;r.piano=s.piano;r.bed=s.table.south(6);r.ready=true;RealmData.get(p.server).realms.put(s.door,r);
        var forced=new HashSet<net.minecraft.world.level.ChunkPos>();for(int x=-10;x<=18;x++)for(int z=-10;z<=9;z++){var at=s.table.offset(x,-1,z);var chunk=new net.minecraft.world.level.ChunkPos(at);forced.add(chunk);l.setChunkForced(chunk.x,chunk.z,true);l.getChunkAt(at);l.setBlock(at,Blocks.STONE.defaultBlockState(),2);for(int y=0;y<4;y++)l.setBlock(at.above(y+1),Blocks.AIR.defaultBlockState(),2);}
        l.setBlock(r.bed,Blocks.BLUE_BED.defaultBlockState().setValue(BedBlock.PART,BedPart.HEAD).setValue(BedBlock.FACING,Direction.NORTH),2);l.setBlock(r.bed.south(),Blocks.BLUE_BED.defaultBlockState().setValue(BedBlock.PART,BedPart.FOOT).setValue(BedBlock.FACING,Direction.NORTH),2);
        h.runAtTickTime(70,()->{
            var mom=ModEntities.MOM.get().create(l);var dad=ModEntities.DAD.get().create(l);for(var parent:List.of(mom,dad)){parent.makeOtherParent(0);parent.getPersistentData().putUUID("RealmDoor",s.door);parent.getPersistentData().putBoolean("CoralineDecoration",true);parent.setPos(p.position().add(1,0,0));h.assertTrue(l.addFreshEntity(parent),"Story parent fixture rejected");}mom.getPersistentData().putUUID("LoopOwner",p.getUUID());dad.getPersistentData().putUUID("VisitEventOwner",p.getUUID());s.mom=mom.getUUID();s.dad=dad.getUUID();
        });
        h.runAtTickTime(100,()->{try{
            var mom=MotherSearch.mother(p);var dad=VisitEvents.father(p,s);h.assertTrue(mom!=null&&dad!=null,"Story parents not registered in warmed fixture chunks");mom.setPos(p.position().add(1,0,0));dad.setPos(p.position().add(-1,0,0));
            OtherMotherLoop.relationship(p,1,0);h.assertTrue(s.loop.phase==OtherMotherLoopState.Phase.GAME_NIGHT_GREETING,"Trust interrupted an active event");VisitEvents.next(p,s,false);h.assertTrue(BlackIsTraditional.active(s),"Boundary selected an ordinary event instead of proposal");int serial=s.loop.event.getInt("Serial");
            finishAudio(p,s);h.assertTrue(BlackIsTraditional.phase(s)==BlackIsTraditional.Phase.INTRO_B,"A didn't advance to B");finishAudio(p,s);h.assertTrue(BlackIsTraditional.phase(s)==BlackIsTraditional.Phase.FOLLOW_PARENTS,"B didn't require following");
            for(var parent:List.of(mom,dad)){var chair=BlackIsTraditional.prop(p,s,ScenePropEntity.CHAIR,parent.isMom()?1:2);parent.setPos(chair.position());BlackIsTraditional.parentControl(parent,p,s);}
            BlackIsTraditional.tick(p,s);h.assertTrue(BlackIsTraditional.phase(s)==BlackIsTraditional.Phase.WAITING_FOR_SEAT,"Dining didn't wait for real seat");var chair=BlackIsTraditional.prop(p,s,ScenePropEntity.CHAIR,0);p.setPos(chair.position());BlackIsTraditional.interact(p,chair);h.assertTrue(p.getVehicle()==chair&&BlackIsTraditional.phase(s)==BlackIsTraditional.Phase.C,"Seat did not start C");p.stopRiding();h.assertTrue(p.getVehicle()==chair&&chair.proposalSeatLocked(),"Proposal allowed manual dismount");finishAudio(p,s);p.stopRiding();h.assertTrue(p.getVehicle()==chair,"Gift pause unlocked proposal seat");
            var n=BlackIsTraditional.data(s);var gift=(CinematicPropEntity)l.getEntity(n.getUUID("Gift"));BlackIsTraditional.interact(p,s,gift);h.assertTrue(BlackIsTraditional.phase(s)==BlackIsTraditional.Phase.GIFT_OPENED,"Gift didn't require interaction");for(int i=0;i<19;i++)BlackIsTraditional.tick(p,s);h.assertTrue(BlackIsTraditional.phase(s)==BlackIsTraditional.Phase.GIFT_OPENED,"D began before one second");BlackIsTraditional.tick(p,s);h.assertTrue(BlackIsTraditional.phase(s)==BlackIsTraditional.Phase.D,"D did not begin after one second");
            for(var expected:List.of(BlackIsTraditional.Phase.E,BlackIsTraditional.Phase.F,BlackIsTraditional.Phase.G,BlackIsTraditional.Phase.H,BlackIsTraditional.Phase.I,BlackIsTraditional.Phase.J,BlackIsTraditional.Phase.K,BlackIsTraditional.Phase.AWAITING_CHOICE)){finishAudio(p,s);h.assertTrue(BlackIsTraditional.phase(s)==expected,"Incorrect D-K sequence at "+expected);p.stopRiding();h.assertTrue(p.getVehicle()==chair,"Proposal seat unlocked at "+expected);}
            // Acceptance now has its own real, owner-scoped branch. Exercise
            // refusal independently; the accepted-entry tests cover that path.
            var nonce=n.getUUID("Choice");BlackIsTraditional.choice(p,UUID.randomUUID(),"traditional:reject");h.assertTrue(BlackIsTraditional.phase(s)==BlackIsTraditional.Phase.AWAITING_CHOICE,"Forged choice advanced story");BlackIsTraditional.choice(p,nonce,"traditional:reject");h.assertTrue(p.getVehicle()==null&&n.getBoolean("Rejected")&&!AcceptedButtonEyes.active(s),"Reject didn't stand/confine player or entered acceptance");BlackIsTraditional.choice(p,nonce,"traditional:reject");h.assertTrue(BlackIsTraditional.phase(s)==BlackIsTraditional.Phase.L,"Duplicate choice advanced twice");finishAudio(p,s);
            h.assertTrue(BlackIsTraditional.phase(s)==BlackIsTraditional.Phase.RUN_UPSTAIRS,"L skipped the physical upstairs objective");
            for(int i=0;i<20;i++)BlackIsTraditional.tick(p,s);
            h.assertTrue(BlackIsTraditional.phase(s)==BlackIsTraditional.Phase.RUN_UPSTAIRS,"Idle time bypassed the upstairs objective");
            var landing=HouseCabinets.at(r.house,24,11,27);l.getChunkAt(landing);l.setBlock(landing.below(),Blocks.STONE.defaultBlockState(),2);p.setPos(Vec3.atBottomCenterOf(landing));BlackIsTraditional.tick(p,s);
            h.assertTrue(BlackIsTraditional.phase(s)==BlackIsTraditional.Phase.M,"Actual upstairs checkpoint did not begin M");
            finishAudio(p,s);h.assertTrue(BlackIsTraditional.phase(s)==BlackIsTraditional.Phase.GO_TO_BED,"M didn't request actual bed interaction");
            completeBarricadeFixture(p,s,r.house);p.setPos(r.bed.getCenter());BlackIsTraditional.block(p,r.bed);h.assertTrue(p.isSleeping()&&BlackIsTraditional.phase(s)==BlackIsTraditional.Phase.N,"Real bed didn't begin N");finishAudio(p,s);h.assertTrue(p.isSleeping()&&BlackIsTraditional.phase(s)==BlackIsTraditional.Phase.O,"O prematurely stood player up");finishAudio(p,s);h.assertTrue(!p.isSleeping()&&n.getBoolean("SleepDisabled")&&n.getBoolean("Dark"),"O didn't eject/disable bed/darken house");
            BlackIsTraditional.go(p,s,BlackIsTraditional.Phase.P);for(var expected:List.of(BlackIsTraditional.Phase.Q,BlackIsTraditional.Phase.R,BlackIsTraditional.Phase.S,BlackIsTraditional.Phase.STUDY_REVEAL_COMPLETE)){finishAudio(p,s);h.assertTrue(BlackIsTraditional.phase(s)==expected,"Incorrect P-S checkpoint "+expected);}
            var saved=OtherMotherLoopState.load(s.loop.save());h.assertTrue(saved.story.getCompound("BlackIsTraditional").getBoolean("StudyRevealComplete"),"Endpoint not persisted");VisitEvents.next(p,s,false);h.assertTrue(s.loop.event.getInt("Serial")==serial&&BlackIsTraditional.phase(s)==BlackIsTraditional.Phase.STUDY_REVEAL_COMPLETE,"Endpoint restarted random activities");h.assertTrue(p.getVehicle()==null&&!p.isSleeping(),"Endpoint left player attached/frozen");
        }finally{s.loop.phase=OtherMotherLoopState.Phase.EXPLORING;p.stopRiding();p.stopSleepInBed(true,false);VisitEvents.cleanup(p,s);for(String key:List.of("Gift","Hand")){var n=BlackIsTraditional.data(s);if(n.hasUUID(key)){var entity=l.getEntity(n.getUUID(key));if(entity!=null)entity.discard();}}for(var id:Arrays.asList(s.mom,s.dad))if(id!=null){var entity=l.getEntity(id);if(entity!=null)entity.discard();}VisitData.get(p.server).scenes.remove(s.owner);RealmData.get(p.server).realms.remove(s.door);for(var chunk:forced)l.setChunkForced(chunk.x,chunk.z,false);}h.succeed();});
    }
}
