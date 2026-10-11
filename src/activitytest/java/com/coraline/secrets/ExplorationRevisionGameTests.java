package com.coraline.secrets;

import com.coraline.feast.*;
import com.coraline.otherworld.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class ExplorationRevisionGameTests {
    @GameTest(template="empty") public static void pursuitNeedsContactAndRemembersFourSeconds(GameTestHelper h){
        var p=SecretsGameTests.fixture(h);SecretsGameTests.warm(p);h.runAfterDelay(20,()->{
            var at=p.blockPosition();for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)for(int y=-1;y<4;y++)p.serverLevel().setBlock(at.offset(x,y,z),(y==-1?Blocks.STONE:Blocks.AIR).defaultBlockState(),2);
            var m=com.coraline.registry.ModEntities.MOM.get().create(p.serverLevel());m.makeOtherParent(0);m.setPos(p.position().add(0,0,6));m.setYHeadRot(180);m.tickCount=1;p.serverLevel().addFreshEntity(m);OtherMotherLoop.scene(p).mom=m.getUUID();
            var s=SecretTransactions.state(p);s.search=SecretsState.Search.SEARCHING;
            var before=m.position();SecretSpies.report(p);h.assertTrue(m.position().equals(before)&&!Reconciliation.active(p),"Spy report teleported Mother or caught player");
            SecretTransactions.issue(p,"key_drawer");h.assertFalse(MotherInterrupts.check(p),"Evidence hook caught player at a distance during search");
            MotherSearch.control(m,p);h.assertTrue(s.search==SecretsState.Search.DIRECT_CHASE&&!Reconciliation.active(p),"Sight should chase, not catch");var last=s.target;
            m.setYHeadRot(0);p.setPos(p.position().add(4,0,0));s.world.putLong("ChaseSeenAt",OtherMotherLoop.now(p)-79);MotherSearch.control(m,p);
            h.assertTrue(s.search==SecretsState.Search.DIRECT_CHASE&&s.target.equals(last),"Lost sight should pursue last known position for four seconds");
            s.world.putLong("ChaseSeenAt",OtherMotherLoop.now(p)-80);MotherSearch.control(m,p);h.assertTrue(s.search==SecretsState.Search.SEARCHING,"Expired chase never resumed search");
            s.search=SecretsState.Search.DIRECT_CHASE;m.setPos(p.position().add(.5,0,0));MotherSearch.control(m,p);h.assertTrue(Reconciliation.active(p),"Physical contact failed to catch player");
            Reconciliation.cancel(p);m.discard();SecretsGameTests.cool(p);h.succeed();
        });
    }
    @GameTest(template="empty") public static void portalStaysOffFrontApproach(GameTestHelper h){
        var house=new BlockPos(20,80,20);
        h.assertFalse(LoopProps.portalSite(house,house.south(30)),"Front approach accepted");
        h.assertFalse(LoopProps.portalSite(house,house.east(29)),"Portal too close to house");
        h.assertFalse(LoopProps.portalSite(house,house.east(47)),"Old close portal still accepted");
        h.assertTrue(LoopProps.portalSite(house,house.east(57))&&LoopProps.portalSite(house,house.west(57)),"Side lawns rejected");h.succeed();
    }
    @GameTest(template="empty") public static void expansionPersistsOldDisk(GameTestHelper h){
        var p=SecretsGameTests.fixture(h);var data=RealmData.get(p.server);var r=SecretWorld.realm(p);r.radius=300;r.islandComplete=true;
        RealmData.expand(r);h.assertTrue(r.radius==500&&r.preservedRadius==300&&!r.islandComplete,"Old disk not protected");
        var saved=RealmData.load(data.save(new net.minecraft.nbt.CompoundTag())).realms.get(r.door);
        h.assertTrue(saved.radius==500&&saved.preservedRadius==300,"Expansion lost across reload");
        RealmData.expand(saved);h.assertTrue(saved.preservedRadius==300,"Repeated expansion changed preservation");h.succeed();
    }
    @GameTest(template="empty") public static void oreRarityAndNormalDrops(GameTestHelper h){
        int[] counts=new int[4];for(int i=0;i<100000;i++){counts[WildOres.kind(i/100000.0,250)]++;h.assertTrue(WildOres.kind(i/100000.0,100)!=3,"Soulglass spawned near estate");}
        h.assertTrue(counts[3]==200&&counts[0]>counts[1]&&counts[1]>counts[2],"Ore rarity incorrect");
        var p=SecretsGameTests.fixture(h);var pos=p.blockPosition();SecretWorld.place(p,pos,WildOres.block(1));
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));
        h.assertTrue(SecretsRuntime.active(p),"Ore fixture not exploring");
        h.assertTrue(p.serverLevel().getBlockState(pos).is(WildOres.block(1)),"Ore not placed in fixture");
        h.assertTrue(p.getMainHandItem().isCorrectToolForDrops(p.serverLevel().getBlockState(pos)),"Ore tool tags incorrect");
        h.assertTrue(WildOres.canMine(p,pos),"Normal iron pickaxe cannot mine diamond deposit");
        var drops=net.minecraft.world.level.block.Block.getDrops(p.serverLevel().getBlockState(pos),p.serverLevel(),pos,null,p,p.getMainHandItem());
        h.assertTrue(drops.size()==1&&drops.get(0).is(com.coraline.clothing.OtherMaterials.OTHER_DIAMOND.get()),"Wrong normal ore drops");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.WOODEN_PICKAXE));h.assertFalse(WildOres.canMine(p,pos),"Wood bypassed tool tier");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_SHOVEL));h.assertFalse(WildOres.canMine(p,pos),"Shovel mined ore");h.succeed();
    }
    @GameTest(template="empty") public static void expansionCopiesBoundaryWithoutErasingOldBlocks(GameTestHelper h){
        var p=SecretsGameTests.fixture(h);var r=SecretWorld.realm(p);var door=DoorData.get(p.server).doors.get(r.door);
        r.preservedRadius=8;r.radius=500;r.estateVersion=OtherWorldEstate.VERSION;
        var source=p.server.overworld();var inside=door.pos.east(8).above(10);var outside=door.pos.east(9).above(10);
        source.setBlock(outside,Blocks.DIAMOND_BLOCK.defaultBlockState(),2);
        var saved=inside.offset(r.dx,0,r.dz);p.serverLevel().setBlock(saved,Blocks.EMERALD_BLOCK.defaultBlockState(),2);
        try{
            var copy=new RealmCopy(source,p.serverLevel(),door,r,true);
            var method=RealmCopy.class.getDeclaredMethod("copy",net.minecraft.world.level.chunk.LevelChunk.class,net.minecraft.world.level.chunk.LevelChunk.class);method.setAccessible(true);
            method.invoke(copy,source.getChunkAt(inside),p.serverLevel().getChunkAt(saved));
        }catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
        h.assertTrue(p.serverLevel().getBlockState(saved).is(Blocks.EMERALD_BLOCK),"Expansion overwrote existing inner disk");
        h.assertTrue(p.serverLevel().getBlockState(outside.offset(r.dx,0,r.dz)).is(Blocks.DIAMOND_BLOCK),"Expansion did not fill boundary extension");
        source.setBlock(outside,Blocks.AIR.defaultBlockState(),2);h.succeed();
    }
    @GameTest(template="empty") public static void scoldingApproachesAndFacesListener(GameTestHelper h){
        var p=SecretsGameTests.fixture(h);SecretsGameTests.warm(p);h.runAfterDelay(20,()->{
            var at=p.blockPosition();for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=-1;y<=3;y++)p.serverLevel().setBlock(at.offset(x,y,z),(y==-1?Blocks.STONE:Blocks.AIR).defaultBlockState(),2);
            var m=com.coraline.registry.ModEntities.MOM.get().create(p.serverLevel());m.makeOtherParent(0);m.setPos(p.position().add(8,0,0));p.serverLevel().addFreshEntity(m);OtherMotherLoop.scene(p).mom=m.getUUID();
            Reconciliation.begin(p,Reconciliation.Category.SERIOUS,OtherMotherWelcome.State.fromId("neutral"),false);p.tickCount=40;Reconciliation.hold(p);
            h.assertTrue(m.distanceToSqr(p)<=3.24,"Speaker remains far away");
            var direction=p.getEyePosition().subtract(m.getEyePosition()).normalize();h.assertTrue(m.getLookAngle().dot(direction)>.99,"Speaker not facing listener");
            Reconciliation.cancel(p);m.discard();SecretsGameTests.cool(p);h.succeed();
        });
    }
}
