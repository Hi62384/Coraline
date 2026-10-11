package com.coraline.secrets;
import com.coraline.feast.*;
import com.coraline.otherworld.OtherWorldEstate;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class SecretsRevisionGameTests {
    @GameTest(template="empty") public static void lateInvitationStaysOpenAndSchedulesSearchOnce(GameTestHelper h){
        var p=SecretsGameTests.fixture(h);var l=OtherMotherLoop.scene(p).loop;l.phase=OtherMotherLoopState.Phase.WINDOW;l.arrivalDeadline=OtherMotherLoop.now(p)-1;
        MotherSearch.attendanceExpired(p);long delay=l.secrets.pendingTicks;
        h.assertTrue(delay>=1200&&delay<=12000,"Search delay outside 1-10 minutes");
        MotherSearch.attendanceExpired(p);h.assertTrue(l.secrets.pendingTicks==delay&&l.phase==OtherMotherLoopState.Phase.WINDOW,"Expiry restarted timer or removed invitation");
        l.secrets.pendingTicks=1;MotherSearch.tick(p);h.assertTrue(l.secrets.search==SecretsState.Search.SEARCHING&&l.phase==OtherMotherLoopState.Phase.WINDOW,"Search closed late invitation");
        MotherSearch.attended(p);h.assertTrue(l.secrets.search==SecretsState.Search.NORMAL&&l.secrets.pendingTicks==0,"Late arrival did not stop search");h.succeed();
    }
    @GameTest(template="empty") public static void hidingBehindWallKeepsOnlyLastSeenPosition(GameTestHelper h){
        var p=SecretsGameTests.fixture(h);var s=SecretTransactions.state(p);var m=com.coraline.registry.ModEntities.MOM.get().create(p.serverLevel());
        m.makeOtherParent(0);m.setPos(p.getX()+10,p.getY(),p.getZ());m.tickCount=1;
        var wall=p.blockPosition().east(5);for(int y=0;y<4;y++)for(int z=-2;z<=2;z++)p.serverLevel().setBlock(wall.offset(0,y,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
        s.search=SecretsState.Search.DIRECT_CHASE;s.target=p.blockPosition().south(15);var previous=s.target;
        h.assertFalse(m.hasLineOfSight(p),"Test wall did not block vision");MotherSearch.control(m,p);
        h.assertTrue(s.target.equals(previous),"Mother tracked hidden player through blocks");
        for(int y=0;y<4;y++)for(int z=-2;z<=2;z++)p.serverLevel().setBlock(wall.offset(0,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);h.succeed();
    }
    @GameTest(template="empty") public static void ratNeedsFourUnbrokenSecondsAndFacesItsSight(GameTestHelper h){
        var n=new net.minecraft.nbt.CompoundTag();for(int i=1;i<80;i++)h.assertTrue(SecretSpies.watchTicks(n,i,true)<SecretSpies.RAT_WATCH_TICKS,"Rat alerted early");
        h.assertTrue(SecretSpies.watchTicks(n,80,true)==SecretSpies.RAT_WATCH_TICKS,"Four-second sight not completed");
        h.assertTrue(SecretSpies.watchTicks(n,81,false)==0&&SecretSpies.watchTicks(n,82,true)==1,"Broken sight not reset");
        h.assertTrue(SecretSpies.watchTicks(n,90,true)==1,"Unloaded interval accumulated sight");
        var p=SecretsGameTests.fixture(h);var rat=com.coraline.registry.ModEntities.MOUSE.get().create(p.serverLevel());rat.setPos(p.position());rat.setYRot(0);
        p.setPos(rat.getX(),rat.getY(),rat.getZ()+4);h.assertTrue(SecretSpies.inRatView(rat,p),"Front invisible");
        p.setPos(rat.getX(),rat.getY(),rat.getZ()-4);h.assertFalse(SecretSpies.inRatView(rat,p),"Rat sees behind itself");h.succeed();
    }
    @GameTest(template="empty") public static void visibleEvidenceInterruptsDialogueAwayFromPortal(GameTestHelper h){
        var p=SecretsGameTests.fixture(h);SecretsGameTests.warm(p);h.runAfterDelay(20,()->{var scene=OtherMotherLoop.scene(p);var s=SecretTransactions.state(p);
        var m=com.coraline.registry.ModEntities.MOM.get().create(p.serverLevel());m.makeOtherParent(0);m.setNoGravity(true);m.setPos(p.getX()+3,p.getY(),p.getZ());p.serverLevel().addFreshEntity(m);scene.mom=m.getUUID();
        SecretTransactions.issue(p,"key_drawer");s.dialogue.putString("Playing","old_voice");s.dialogue.putLong("Until",OtherMotherLoop.now(p)+1000);
        h.assertTrue(MotherInterrupts.check(p),"Evidence ignored while voice busy away from portal");
        h.assertTrue(Reconciliation.active(p)&&s.search==SecretsState.Search.CONFRONTING,"Confrontation did not begin");
        h.assertFalse(s.dialogue.getString("Playing").equals("old_voice"),"Old voice not cancelled");
        h.assertTrue(s.dialogue.getString("Event").equals("RESTRICTED_ITEM_CAUGHT"),"Wrong confrontation type");m.discard();SecretsGameTests.cool(p);h.succeed();});
    }
    @GameTest(template="empty") public static void pursuitRestoresMovementAfterDialoguePose(GameTestHelper h){
        var p=SecretsGameTests.fixture(h);var s=SecretTransactions.state(p);var m=com.coraline.registry.ModEntities.MOM.get().create(p.serverLevel());
        m.makeOtherParent(0);m.setPos(p.getX()+60,p.getY(),p.getZ());m.setNoAi(true);m.setNoGravity(true);s.search=SecretsState.Search.DIRECT_CHASE;s.target=p.blockPosition();m.tickCount=1;
        h.assertTrue(MotherSearch.control(m,p),"Pursuit controller inactive");h.assertFalse(m.isNoAi()||m.isNoGravity(),"Dialogue freeze retained during chase");h.succeed();
    }
    @GameTest(template="empty") public static void structuresWaitOnlyForTheirOwnTerrain(GameTestHelper h){
        var r=new com.coraline.otherworld.RealmData.Realm();r.dx=160;r.dz=-160;
        var at=new net.minecraft.core.BlockPos(168,70,-152);
        h.assertFalse(SecretWorld.terrainCopied(r,at),"Uncopied terrain accepted");
        for(int x=0;x<=0;x++)for(int z=-1;z<=0;z++)r.copiedChunks.add(net.minecraft.world.level.ChunkPos.asLong(x,z));
        h.assertTrue(SecretWorld.terrainCopied(r,at),"Ready footprint waits for unrelated island chunks");h.succeed();
    }
    @GameTest(template="empty") public static void ratsIgnoreEstateAndFortyBlockBuffer(GameTestHelper h){
        var p=SecretsGameTests.fixture(h);var house=SecretWorld.realm(p).house;
        for(var at:new net.minecraft.core.BlockPos[]{house,house.south(70),house.south(126),house.east(82)}){
            p.setPos(at.getCenter());h.assertFalse(SecretSpies.ratMayWatch(p),"Rat watches safe estate/buffer");
        }
        p.setPos(house.south(127).getCenter());h.assertTrue(SecretSpies.ratMayWatch(p),"Rat cannot watch beyond garden boundary");h.succeed();
    }
    @GameTest(template="empty") public static void secretSitesUseGroundBelowTreesAndProtectBuildings(GameTestHelper h){
        var level=h.getLevel();var origin=h.absolutePos(new net.minecraft.core.BlockPos(20,4,20));var center=new net.minecraft.core.BlockPos(origin.getX(),100,origin.getZ());
        for(int x=-6;x<=6;x++)for(int z=-9;z<=6;z++){
            for(int y=-3;y<=13;y++)level.setBlock(center.offset(x,y,z),(y<0?net.minecraft.world.level.block.Blocks.DIRT:net.minecraft.world.level.block.Blocks.AIR).defaultBlockState(),2);
        }
        for(int y=0;y<9;y++)level.setBlock(center.above(y),net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState(),2);
        h.assertTrue(SecretWorld.ground(level,center.getX(),center.getZ())==center.getY(),"Tree mistaken for ground: actual="+SecretWorld.ground(level,center.getX(),center.getZ())+" expected="+center.getY()+" top="+level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,center.getX(),center.getZ()));
        h.assertTrue(center.equals(SecretWorld.site(level,center)),"Forest site rejected");
        level.setBlock(center.east(2),net.minecraft.world.level.block.Blocks.BRICKS.defaultBlockState(),2);
        h.assertTrue(SecretWorld.site(level,center)==null,"Existing building overwritten");h.succeed();
    }
    @GameTest(template="empty") public static void everyActivityAllowsLeavingImmediately(GameTestHelper h){
        var p=SecretsGameTests.fixture(h);var s=OtherMotherLoop.scene(p);s.loop.phase=OtherMotherLoopState.Phase.ACTIVITY;
        for(var activity:OtherMotherActivities.Activity.values()){s.loop.activity=activity;h.assertTrue(ActivityWorld.exitReady(s),"Exit locked: "+activity);}
        s.loop.phase=OtherMotherLoopState.Phase.NONE;h.assertFalse(ActivityWorld.exitReady(s),"Inactive scene exit accepted");h.succeed();
    }
    @GameTest(template="empty") public static void journalCarryReturnsWithoutChangingOrigin(GameTestHelper h){
        var p=SecretsGameTests.fixture(h);var s=SecretTransactions.state(p);int original=s.hiding;
        p.setPos(SecretWorld.hiding(p,original).getCenter());JournalPlacement.begin(p);h.assertTrue(JournalPlacement.carrying(p),"Journal not picked up");
        p.setPos(SecretWorld.realm(p).porch.getCenter());JournalPlacement.tick(p);
        h.assertFalse(JournalPlacement.carrying(p),"Journal escaped room");h.assertTrue(s.hiding==original,"Journal origin lost");h.succeed();
    }
    @GameTest(template="empty") public static void estateDistanceIncludesGarden(GameTestHelper h){
        var c=net.minecraft.core.BlockPos.ZERO;
        h.assertTrue(OtherWorldEstate.outsideDistance(c,c.south(87))==0,"Garden excluded");
        h.assertTrue(OtherWorldEstate.outsideDistance(c,c.south(127))==40,"Wrong garden distance");
        h.assertTrue(OtherWorldEstate.outsideDistance(c,c.east(83))==40,"Wrong side distance");
        h.assertTrue(SecretWorld.distanceTier(60)==0&&SecretWorld.distanceTier(130)==1&&SecretWorld.distanceTier(200)==2&&SecretWorld.distanceTier(280)==3,"Tiers not distance based");h.succeed();
    }
}
