package com.coraline.wybie;

import com.coraline.bobinsky.*;
import com.coraline.feast.VisitData;
import com.coraline.tea.TeaVisit;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class NeighborVisitsGameTests {
    @GameTest(template="empty",timeoutTicks=200)
    public static void wybieCanVisitInsideBedroomAndCannotOverlapAnotherVisit(GameTestHelper h){
        var level=h.getLevel();var c=h.absolutePos(new BlockPos(1400,250,1400));BobinskyHouse.hold(level,c);
        for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)for(int y=0;y<=5;y++)level.setBlock(c.offset(x,y,z),(y==0||Math.abs(x)==6||Math.abs(z)==6?Blocks.STONE:Blocks.AIR).defaultBlockState(),2);
        var p=player(h,c);var scene=scene(p,c);scene.bobinskyVisited=true;scene.teaVisited=true;
        h.runAfterDelay(5,()->{
            NeighborVisits.tryVisit(p,scene);
            h.assertTrue(scene.neighborVisitsReady&&scene.wybieActor!=null,"Wybie failed to spawn in a bedroom with nearby clear space");
            var actor=level.getEntity(scene.wybieActor);h.assertTrue(actor!=null&&actor.distanceToSqr(p)<49&&level.noCollision(actor,actor.getBoundingBox()),"Wybie placed through bedroom wall");
            var id=scene.wybieActor;scene.nextNeighborAttempt=0;NeighborVisits.tryVisit(p,scene);
            h.assertTrue(id.equals(scene.wybieActor)&&!TeaVisit.offer(p,scene),"Active Wybie overlapped a new visit");
            actor.discard();VisitData.get(p.server).scenes.remove(scene.owner);
            for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)for(int y=0;y<=5;y++)level.setBlock(c.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=240)
    public static void teaInvitationSurvivesIndoorFailureAndStartsAwayFromBed(GameTestHelper h){
        var level=h.getLevel();var c=h.absolutePos(new BlockPos(1600,180,1600));
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)level.setBlock(c.offset(x,0,z),Blocks.STONE.defaultBlockState(),2);
        var p=player(h,c);var scene=scene(p,c);scene.wybieVisited=true;scene.bobinskyVisited=true;scene.nextNeighborVisit=2;
        // setDayTime changes the clock immediately; isDay's skylight cache is
        // refreshed by the following server tick, not by the setter itself.
        h.runAfterDelay(5,()->{
        NeighborVisits.tryVisit(p,scene);
        h.assertTrue(scene.neighborVisitsReady&&scene.teaSiteHint&&!BobinskyVisit.pending(p),"No-space tea invitation wasn't retained/explained");
        var saved=VisitData.load(VisitData.get(p.server).save(new CompoundTag())).scenes.get(scene.owner);
        h.assertTrue(saved.neighborVisitsReady&&saved.teaSiteHint,"Pending invitation lost on save");VisitData.get(p.server).scenes.put(saved.owner,saved);
        for(int x=-46;x<=46;x++)for(int z=-46;z<=46;z++)level.setBlock(c.offset(x,0,z),Blocks.STONE.defaultBlockState(),2);
        var chunk=new net.minecraft.world.level.ChunkPos(c);for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)level.getChunkSource().addRegionTicket(TicketType.PORTAL,new net.minecraft.world.level.ChunkPos(chunk.x+x,chunk.z+z),2,c);
        p.setPos(Vec3.atBottomCenterOf(c.offset(20,1,0)));
        h.runAfterDelay(5,()->{
            saved.nextNeighborAttempt=0;NeighborVisits.tryVisit(p,saved);
            var data=BobinskyVisitData.get(p.server);var tea=data.teaVisits.get(p.getUUID());
            h.assertTrue(tea!=null&&tea.getBoolean("Active")&&tea.getInt("Phase")==TeaVisit.KNOCK,"Tea still requires staying beside bed");
            h.assertFalse(WybieVisit.spawn(p,saved),"Wybie started during tea invitation");
            TeaVisit.cancel(p.server,tea,null);data.teaVisits.remove(p.getUUID());VisitData.get(p.server).scenes.remove(saved.owner);
            for(int x=-46;x<=46;x++)for(int z=-46;z<=46;z++)level.setBlock(c.offset(x,0,z),Blocks.AIR.defaultBlockState(),2);h.succeed();
        });
        });
    }
    private static ServerPlayer player(GameTestHelper h,BlockPos c){
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"NeighborQA"));
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);p.setPos(Vec3.atBottomCenterOf(c.above()));
        var bed=c.offset(1,1,0);h.getLevel().setBlock(bed,Blocks.BLUE_BED.defaultBlockState(),2);p.setRespawnPosition(Level.OVERWORLD,bed,0,true,false);h.getLevel().setDayTime(1000);return p;
    }
    private static VisitData.Scene scene(ServerPlayer p,BlockPos c){var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.table=s.piano=c;s.stage=VisitData.Stage.DONE;VisitData.get(p.server).scenes.put(s.owner,s);return s;}
}
