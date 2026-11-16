package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.secrets.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class DeliveryGameTests {
    private static ServerPlayer fixture(GameTestHelper h,int offset){
        var level=h.getLevel().getServer().getLevel(OtherWorld.KEY);var at=new BlockPos(32000+offset*160,130,32000);level.getChunkAt(at);
        var p=new ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"DeliveryQA"));p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);p.setPos(Vec3.atBottomCenterOf(at));
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.table=at;s.piano=at.east(20);s.loop.phase=OtherMotherLoopState.Phase.EXPLORING;s.loop.visits=2;s.loop.secrets.visit=2;s.loop.trust=.5F;s.loop.suspicion=.1F;VisitData.get(p.server).scenes.put(p.getUUID(),s);p.getPersistentData().putUUID("CoralineVisitDoor",s.door);
        var r=new RealmData.Realm();r.door=s.door;r.doorPos=at;r.sourceDoor=at;r.house=at;r.porch=at;r.table=s.table;r.piano=s.piano;r.ready=true;RealmData.get(p.server).realms.put(s.door,r);
        s.loop.event.putString("Type",VisitEvents.Type.SPECIAL_DELIVERY.name());s.loop.event.putInt("Serial",1);
        h.assertTrue(SpecialDelivery.preparePayload(p,s),"No eligible meaningful delivery payload");SpecialDelivery.begin(p,s);
        s.loop.event.putLongArray("DeliveryRoute",new long[]{at.asLong(),at.east().asLong(),at.east(2).asLong()});return p;
    }
    private static void clear(ServerPlayer p){var s=OtherMotherLoop.scene(p);VisitEvents.cleanup(p,s);RealmData.get(p.server).realms.remove(s.door);VisitData.get(p.server).scenes.remove(p.getUUID());}
    @GameTest(template="empty",batch="delivery-prepared",timeoutTicks=200)
    public static void backgroundPlanDoesNotBuildUntilImmediateActivation(GameTestHelper h){
        var p=fixture(h,7);var s=OtherMotherLoop.scene(p);var level=p.serverLevel();var start=p.blockPosition();var c=start.east(130);var realm=RealmData.get(p.server).realms.get(s.door);realm.radius=350;realm.islandComplete=true;
        s.loop.event.putString("Type",VisitEvents.Type.EXPLORATION_30_MIN.name());
        for(int x=0;x<=136;x++)for(int z=-7;z<=6;z++){var at=start.offset(x,0,z);level.getChunkAt(at);level.setBlock(at.below(),net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(),2);for(int y=0;y<11;y++)level.setBlock(at.above(y),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);}
        var plan=new net.minecraft.nbt.CompoundTag();plan.putLong("HouseAnchor",realm.house.asLong());plan.putLong("DeliverySeed",42);plan.putBoolean("PlanningOnly",true);plan.putLong("DeliveryHouse",c.asLong());plan.putLong("RouteStart",start.asLong());plan.putLong("RouteGoal",c.north(6).asLong());s.loop.story.put("PreparedDeliveryRoute",plan);
        for(int tick=0;tick<180&&!DeliveryRoute.ready(s);tick++)DeliveryRoute.prewarm(p,s);
        h.assertTrue(DeliveryRoute.ready(s),"Background planning did not produce a ready route");
        h.assertTrue(level.getBlockState(c.below()).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)&&level.getBlockState(c.above()).isAir(),"Planning changed terrain before selection");
        s.loop.event.putString("Type",VisitEvents.Type.SPECIAL_DELIVERY.name());
        h.assertTrue(SpecialDelivery.prepare(p,s),"Ready delivery failed immediate activation");
        h.assertTrue(s.loop.phase==OtherMotherLoopState.Phase.EVENT_INTRO&&level.getBlockState(c.below()).is(net.minecraft.world.level.block.Blocks.OAK_PLANKS),"Delivery still waited for route construction");
        h.assertFalse(DeliveryRoute.ready(s),"Activated route remained reusable");clear(p);h.succeed();
    }
    @GameTest(template="empty",batch="delivery-capability",timeoutTicks=100)
    public static void parcelOwnerAndLedgerRejectForgery(GameTestHelper h){
        var p=fixture(h,0);var s=OtherMotherLoop.scene(p);var parcel=SpecialDelivery.parcel(p,s);
        h.assertTrue(SpecialDelivery.valid(p,s,parcel),"Actual owner parcel rejected");
        parcel.getOrCreateTag().putUUID("DeliveryOwner",UUID.randomUUID());h.assertTrue(!SpecialDelivery.valid(p,s,parcel),"Foreign ownership accepted");
        parcel=SpecialDelivery.parcel(p,s);parcel.getOrCreateTag().putUUID("DeliveryId",UUID.randomUUID());h.assertTrue(!SpecialDelivery.valid(p,s,parcel),"Forged delivery ID accepted");
        parcel=SpecialDelivery.parcel(p,s);parcel.getOrCreateTag().putBoolean("Delivered",true);parcel.getOrCreateTag().putInt("WarningStage",999);SpecialDelivery.openClick(p,parcel);
        h.assertTrue(s.loop.event.getInt("DeliveryWarning")==1&&!s.loop.event.getBoolean("DeliveryOpened")&&!s.loop.event.getBoolean("DeliveryDelivered"),"Editable item tags bypassed server ledger");clear(p);h.succeed();
    }
    @GameTest(template="empty",batch="delivery-opening",timeoutTicks=100)
    public static void threeDistinctClicksIssueOneSecretWithoutAlert(GameTestHelper h){
        var p=fixture(h,1);var s=OtherMotherLoop.scene(p);var parcel=SpecialDelivery.parcel(p,s);String payload=s.loop.event.getString("DeliveryPayload");float trust=s.loop.trust,suspicion=s.loop.suspicion;
        SpecialDelivery.openClick(p,parcel);SpecialDelivery.openClick(p,parcel);
        h.assertTrue(s.loop.event.getInt("DeliveryWarning")==1,"Held use skipped first warning");
        SpecialDelivery.released(p,UUID.randomUUID());SpecialDelivery.openClick(p,parcel);h.assertTrue(s.loop.event.getInt("DeliveryWarning")==1,"Foreign release cleared debounce");
        h.runAtTickTime(15,()->{SpecialDelivery.released(p,s.loop.event.getUUID("DeliveryId"));SpecialDelivery.openClick(p,parcel);h.assertTrue(s.loop.event.getInt("DeliveryWarning")==2&&!s.loop.event.getBoolean("DeliveryOpened"),"Second click opened instead of final warning");});
        h.runAtTickTime(30,()->{SpecialDelivery.released(p,s.loop.event.getUUID("DeliveryId"));SpecialDelivery.openClick(p,parcel);
            h.assertTrue(s.loop.event.getBoolean("DeliveryOpened")&&s.loop.secrets.issued.containsValue(payload),"Third click failed to issue real secret");
            int count=s.loop.secrets.issued.size();SpecialDelivery.openClick(p,parcel);SpecialDelivery.open(p,s);
            h.assertTrue(s.loop.secrets.issued.size()==count&&s.loop.trust==trust&&s.loop.suspicion==suspicion&&!s.loop.secrets.searching()&&!s.loop.event.getBoolean("DeliveryKnown"),"Opening duplicated reward or magically alerted Mother");
            h.assertTrue(!SpecialDelivery.protectedRoute(p)&&SecretRegistry.ALL.get(payload).severity()>=.75,"Opened package retained immunity or issued low-value evidence");clear(p);h.succeed();});
    }
    @GameTest(template="empty",batch="delivery-route",timeoutTicks=100)
    public static void routeGeometryDoesNotErasePursuit(GameTestHelper h){
        var p=fixture(h,2);var s=OtherMotherLoop.scene(p);var start=p.position();h.assertTrue(SpecialDelivery.protectedRoute(p),"Owner on route not protected");
        p.setPos(start.add(0,0,9));h.assertTrue(!SpecialDelivery.protectedRoute(p),"Rectangular immunity leaked into wilderness");p.setPos(start);
        s.loop.secrets.search=SecretsState.Search.DIRECT_CHASE;h.assertTrue(!SpecialDelivery.protectedRoute(p)&&s.loop.secrets.search==SecretsState.Search.DIRECT_CHASE,"Reentry erased witnessed chase");
        s.loop.secrets.search=SecretsState.Search.NORMAL;s.loop.event.putBoolean("DeliveryDelivered",true);SpecialDelivery.removePackage(p);h.assertTrue(SpecialDelivery.protectedRoute(p),"Legitimate recipient return lost route protection");
        s.loop.event.putBoolean("DeliveryOpened",true);h.assertTrue(!SpecialDelivery.protectedRoute(p),"Opened ledger restored immunity on return");clear(p);h.succeed();
    }
    @GameTest(template="empty",batch="delivery-capture",timeoutTicks=120)
    public static void betrayalNeedsRealContactAndNeverMagicallyDeletesFiledEvidence(GameTestHelper h){
        var p=fixture(h,4);var s=OtherMotherLoop.scene(p);String payload=s.loop.event.getString("DeliveryPayload");var at=p.blockPosition();var chunk=new net.minecraft.world.level.ChunkPos(at);p.serverLevel().setChunkForced(chunk.x,chunk.z,true);
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){p.serverLevel().setBlock(at.offset(x,-1,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);for(int y=0;y<3;y++)p.serverLevel().setBlock(at.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);}
        h.runAtTickTime(40,()->{
            var m=com.coraline.registry.ModEntities.MOM.get().create(p.serverLevel());m.makeOtherParent(0);m.getPersistentData().putUUID("RealmDoor",s.door);m.getPersistentData().putUUID("LoopOwner",p.getUUID());m.setNoAi(true);m.setNoGravity(true);m.setPos(p.position().add(3,0,0));p.serverLevel().addFreshEntity(m);s.mom=m.getUUID();
        });
        h.runAtTickTime(50,()->{
            SpecialDelivery.open(p,s);s.loop.secrets.journal.add(payload);for(int i=0;i<p.getInventory().getContainerSize();i++)if(SecretTransactions.evidence(p,p.getInventory().getItem(i)).equals(payload))p.getInventory().setItem(i,ItemStack.EMPTY);
            float trust=s.loop.trust,suspicion=s.loop.suspicion;h.assertFalse(SpecialDelivery.caught(p,SecretDialogue.state(p),Set.of()),"Remote Mother magically discovered an opened parcel");
            var mother=MotherSearch.mother(p);h.assertTrue(mother!=null,"Native capture Mother missing");mother.setPos(p.position());h.assertTrue(SpecialDelivery.caught(p,SecretDialogue.state(p),Set.of()),"Actual contact did not discover betrayal");
            h.assertTrue(s.loop.trust<trust&&s.loop.suspicion>suspicion&&s.loop.event.getBoolean("DeliveryKnown"),"Contact did not apply significant betrayal consequences");
            h.assertTrue(s.loop.secrets.journal.contains(payload)&&!s.loop.secrets.exposed.contains(payload),"Mother magically learned or removed filed payload without identifying it");
            float afterTrust=s.loop.trust,afterSuspicion=s.loop.suspicion;h.assertFalse(SpecialDelivery.caught(p,SecretDialogue.state(p),Set.of()),"Betrayal penalty could be applied twice");h.assertTrue(s.loop.trust==afterTrust&&s.loop.suspicion==afterSuspicion,"Repeated catch altered relationship again");
            String id=s.loop.event.getUUID("DeliveryId").toString();h.assertTrue(SecretsState.load(s.loop.secrets.save()).deliveryBetrayals.contains(id),"Permanent betrayal forgotten on save/load");mother.discard();clear(p);p.serverLevel().setChunkForced(chunk.x,chunk.z,false);h.succeed();
        });
    }
    @GameTest(template="empty",batch="delivery-build",timeoutTicks=100)
    public static void cottageBuildHasClosedShellAndExactReversibleFootprint(GameTestHelper h){
        var p=fixture(h,3);var s=OtherMotherLoop.scene(p);var c=p.blockPosition().east(24);s.loop.event.putLong("DeliveryHouse",c.asLong());s.loop.event.putLong("DeliverySeed",2);
        var level=p.serverLevel();for(int x=-6;x<=6;x++)for(int z=-7;z<=6;z++){level.setBlock(c.offset(x,-1,z),net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(),2);for(int y=0;y<11;y++)level.setBlock(c.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);}
        var path=new ArrayList<BlockPos>();for(int i=0;i<15;i++){var at=c.north(6+i);level.setBlock(at.below(),net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(),2);level.setBlock(at,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);level.setBlock(at.above(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);path.add(at);}Collections.reverse(path);
        s.loop.event.putLongArray("DeliveryRoute",path.stream().mapToLong(BlockPos::asLong).toArray());DeliveryRoute.build(p,s,path);String status="PENDING";for(int i=0;i<40&&!status.equals("READY");i++)status=DeliveryRoute.tick(p,s);
        h.assertTrue(status.equals("READY"),"Bounded build did not finish: "+status+" / "+s.loop.event.getLong("DeliveryConflictPosition")+" / "+s.loop.event.getString("DeliveryConflictActual"));
        var door=DeliveryRoute.door(s);h.assertTrue(level.getBlockState(door).is(net.minecraft.world.level.block.Blocks.SPRUCE_DOOR)&&level.getBlockState(door.above()).is(net.minecraft.world.level.block.Blocks.SPRUCE_DOOR),"Door missing a half");
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
            h.assertTrue(!level.getBlockState(c.offset(x,-1,z)).isAir(),"Missing cottage floor");
            if(Math.abs(x)==4||Math.abs(z)==4)for(int y=0;y<=4;y++)h.assertTrue(!level.getBlockState(c.offset(x,y,z)).isAir(),"Gap in cottage walls");
            h.assertTrue(!level.getBlockState(c.offset(x,5+(5-Math.abs(x)),z)).isAir(),"Missing roof slope");
        }
        for(var at:path)h.assertTrue(!level.getBlockState(at.below()).isAir()&&level.noCollision(new net.minecraft.world.phys.AABB(at.getX()+.18,at.getY(),at.getZ()+.18,at.getX()+.82,at.getY()+1.9,at.getZ()+.82)),"Path obstructed or floating");
        var edited=c.offset(-4,2,0);level.setBlock(edited,net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState(),2);DeliveryRoute.cleanup(p,s);
        h.assertTrue(level.getBlockState(c.below()).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)&&level.getBlockState(c.above(10)).isAir(),"Cleanup did not restore original terrain");
        h.assertTrue(level.getBlockState(edited).is(net.minecraft.world.level.block.Blocks.GOLD_BLOCK),"Cleanup overwrote subsequent player edits");clear(p);h.succeed();
    }
}
