package com.coraline.feast;

import java.util.*;
import com.coraline.block.TinyDoorBlock;
import com.coraline.otherworld.*;
import com.coraline.registry.*;
import com.coraline.secrets.HouseCabinets;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import static com.coraline.feast.AcceptedButtonEyesProgress.Phase.*;

/** Real entities, mount events, inventory and owned blocks at the sewing checkpoint.
 * Audio ACKs and the complete client storyline remain separate acceptance. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class AcceptedButtonEyesSewingGameTests {
    private static void atAge(ServerPlayer p,VisitData.Scene s,int age){
        AcceptedButtonEyes.data(s).putLong("PhaseStarted",OtherMotherLoop.now(p)-age);AcceptedButtonEyes.tick(p,s);
    }
    private static void atMusic(ServerPlayer p,VisitData.Scene s,double fraction){
        AcceptedButtonEyes.data(s).putLong("PhaseStarted",OtherMotherLoop.now(p)-60);AcceptedButtonEyes.data(s).putLong("SewingMusicStarted",OtherMotherLoop.now(p)-Math.round(Math.max(300,VisitAudio.length("accepted_little_me_music"))*fraction));AcceptedButtonEyes.tick(p,s);
    }
    @GameTest(template="empty",timeoutTicks=1200)
    public static void sewingLocksSeatReachesEyeAndConfiscatesOnlyAfterFiveSecondBlackout(GameTestHelper h){
        var server=h.getLevel().getServer();var level=server.getLevel(OtherWorld.KEY);h.assertTrue(level!=null,"Other World missing");
        var p=new ServerPlayer(server,level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"SewingCheckpointQA"));
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.loop.phase=OtherMotherLoopState.Phase.ACCEPTED_BUTTON_EYES;
        VisitData.get(server).scenes.put(s.owner,s);var realm=new RealmData.Realm();realm.door=s.door;realm.house=new BlockPos(110256,180,110256);realm.radius=500;realm.ready=true;realm.islandComplete=true;
        var center=HouseCabinets.at(realm.house,34,-1,22);realm.doorPos=center.south(4);realm.bed=center.north(4);RealmData.get(server).realms.put(s.door,realm);
        var n=AcceptedButtonEyes.data(s);AcceptedButtonEyesProgress.begin(n,AcceptedButtonEyesProgress.Route.ACCEPTED_PROPOSAL,s.door,5,OtherMotherLoop.now(p));
        var scenery=AcceptedButtonEyesProgress.scenery(s);scenery.putLong("House",realm.house.asLong());scenery.putLong("LittleDoor",realm.doorPos.asLong());scenery.putLong("Prison",realm.house.above(80).asLong());
        var changed=new ArrayList<BlockPos>();var actors=new ArrayList<net.minecraft.world.entity.Entity>();
        var forced=new HashSet<net.minecraft.world.level.ChunkPos>();
        for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){
            var cell=center.offset(x,0,z);var chunk=new net.minecraft.world.level.ChunkPos(cell);
            if(!level.getForcedChunks().contains(chunk.toLong())){forced.add(chunk);level.setChunkForced(chunk.x,chunk.z,true);}level.getChunkAt(cell);
        }
        boolean[] ran={false};int[] waiting={0};
        h.onEachTick(()->{
        if(ran[0])return;
        if(forced.stream().anyMatch(chunk->!level.areEntitiesLoaded(chunk.toLong()))){
            if(++waiting[0]<280)return;ran[0]=true;for(var chunk:forced)level.setChunkForced(chunk.x,chunk.z,false);
            VisitData.get(server).scenes.remove(s.owner);RealmData.get(server).realms.remove(s.door);h.fail("Sewing fixture entity sections did not load within bounded setup wait");return;
        }
        ran[0]=true;
        try{
            for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)for(int y=-1;y<=3;y++){
                var cell=center.offset(x,y,z);level.setBlock(cell,(y==-1?Blocks.STONE:Blocks.AIR).defaultBlockState(),2);changed.add(cell);
            }
            var door=ModBlocks.TINY_DOOR.get().defaultBlockState();level.setBlock(realm.doorPos,door,2);
            var otherDoor=center.east(5);level.setBlock(otherDoor,door,2);
            var foot=Blocks.BLUE_BED.defaultBlockState().setValue(BedBlock.FACING,Direction.NORTH).setValue(BedBlock.PART,BedPart.FOOT);
            level.setBlock(realm.bed,foot,2);level.setBlock(realm.bed.north(),foot.setValue(BedBlock.PART,BedPart.HEAD),2);
            for(var type:List.of(ModEntities.MOM.get(),ModEntities.DAD.get())){
                var actor=type.create(level);h.assertTrue(actor!=null,"Parent creation failed");actor.makeOtherParent(0);actor.setPos(center.getCenter());h.assertTrue(level.addFreshEntity(actor),"Parent could not join warmed fixture");actors.add(actor);
                h.assertTrue(level.getEntity(actor.getUUID())==actor,"Parent is not live in warmed fixture");
                if(actor.isMom())s.mom=actor.getUUID();else s.dad=actor.getUUID();
            }
            s.table=center;s.piano=center;p.setPos(Vec3.atBottomCenterOf(center));p.getPersistentData().putUUID("CoralineVisitDoor",s.door);p.getInventory().setItem(0,new ItemStack(Items.DIAMOND,7));
            AcceptedButtonEyesProgress.move(n,WAITING_TO_SIT,OtherMotherLoop.now(p));AcceptedButtonEyes.tick(p,s);
            var seat=FirstEscapeScene.prop(p,s,"SewingChair");h.assertTrue(seat!=null,"Sewing checkpoint did not prepare chair");
            p.setPos(seat.position().add(0,0,1));h.assertTrue(AcceptedButtonEyes.interact(p,s,seat)&&p.getVehicle()==seat,"Manual sit failed");
            p.stopRiding();h.assertTrue(p.getVehicle()==seat,"Player dismounted during sewing");
            atMusic(p,s,.12);h.assertTrue(AcceptedButtonEyesProgress.phase(n)==TAKE_BUTTONS,"Chair approach did not advance");
            atMusic(p,s,.26);atMusic(p,s,.40);h.assertTrue(AcceptedButtonEyesProgress.phase(n)==RAISE_NEEDLE,"Props did not advance in order");
            atMusic(p,s,.57);var needle=FirstEscapeScene.prop(p,s,"SewingNeedle");
            h.assertTrue(needle!=null&&needle.position().add(needle.grabTarget().scale(.33)).distanceToSqr(p.getEyePosition())<.0001,"Needle tip missed eye position: "+(needle==null?"missing":needle.position()+" direction "+needle.grabTarget())+" eye "+p.getEyePosition()+" mother "+(AcceptedButtonEyesWorld.mother(p,s)!=null)+" phase "+AcceptedButtonEyesProgress.phase(n));
            atMusic(p,s,.58);h.assertTrue(AcceptedButtonEyesProgress.phase(n)==BLACKOUT,"Needle did not trigger blackout");
            // Hold before the finale boundary, then cross it using the actual score clock.
            atMusic(p,s,.579);h.assertTrue(!n.getBoolean("SewingCompleted")&&!AcceptedButtonEyesInventory.locked(p)&&p.getInventory().getItem(0).getCount()==7,"Blackout ended or confiscated early");
            atMusic(p,s,.82);h.assertTrue(AcceptedButtonEyesProgress.phase(n)==PROCEDURE_COMPLETE&&n.getBoolean("SewingCompleted")&&AcceptedButtonEyesInventory.locked(p),"Score finale did not finish sewing");
            var saved=n.getCompound("InventoryEscrow").copy();h.assertTrue(p.getInventory().isEmpty(),"Confiscated inventory remained visible");
            var sealed=level.getBlockState(realm.doorPos);h.assertTrue(sealed.getValue(TinyDoorBlock.OPEN)&&!sealed.getValue(TinyDoorBlock.PORTAL)&&level.getBlockEntity(realm.doorPos) instanceof DoorBlockEntity be&&be.storyBricked,"Owned Little Door was not visibly sealed");
            h.assertTrue(level.getBlockState(otherDoor).equals(door),"Another door changed");
            h.assertTrue(level.getBlockState(realm.bed).isAir()&&level.getBlockState(realm.bed.north()).isAir(),"Both story-bed halves were not removed");
            AcceptedButtonEyes.tick(p,s);h.assertTrue(n.getCompound("InventoryEscrow").equals(saved),"Later tick overwrote original inventory snapshot");
        }finally{
            n.putBoolean("SeatRelease",true);p.stopRiding();ShowPlayback.stop(p,s);FirstEscapeScene.restore(p,s);
            for(var e:level.getEntitiesOfClass(CinematicPropEntity.class,new AABB(center).inflate(12),e->e.owned(s)))e.discard();
            for(var e:actors)e.discard();for(var cell:changed)level.setBlock(cell,Blocks.AIR.defaultBlockState(),2);
            VisitData.get(server).scenes.remove(s.owner);RealmData.get(server).realms.remove(s.door);
            for(var chunk:forced)level.setChunkForced(chunk.x,chunk.z,false);
        }
        h.succeed();
        });
    }
    private AcceptedButtonEyesSewingGameTests(){}
}
