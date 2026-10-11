package com.coraline.entity;
import com.coraline.registry.*;
import com.coraline.item.DollItem;
import com.coraline.block.entity.DollBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline")
@PrefixGameTestTemplate(false)
public final class CoralineGameTests {
    @GameTest(template="empty",timeoutTicks=120)
    public static void departureNavigatesAroundObstacle(GameTestHelper h) {
        for(int x=1;x<25;x++)for(int z=1;z<25;z++)h.setBlock(x,0,z,Blocks.STONE);
        for(int z=6;z<=18;z++)for(int y=1;y<=3;y++)h.setBlock(12,y,z,Blocks.STONE);
        var w=h.spawn(ModEntities.WYBIE.get(),9.5F,1,12.5F);
        w.getRandom().setSeed(9182);
        var threat=h.absolutePos(new BlockPos(5,1,12)).getCenter();
        var start=w.position();
        h.runAtTickTime(10,()->{
            var path=w.findEscapePath(threat);
            h.assertTrue(path!=null&&path.canReach(),"No reachable escape route beside obstacle");
            w.getNavigation().moveTo(path,1.15);
        });
        h.runAtTickTime(80,()->{
            h.assertTrue(w.position().distanceToSqr(start)>9,"Escape did not make physical progress");
            h.assertTrue(w.position().distanceToSqr(threat)>start.distanceToSqr(threat)+4,"Escape did not move away");
            h.assertTrue(h.getLevel().noCollision(w,w.getBoundingBox()),"Escape clipped into wall");
            h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void ordinaryWybieSurvivesAndTakesDamage(GameTestHelper h) {
        h.setBlock(3,0,3,Blocks.STONE);
        var w=h.spawn(ModEntities.WYBIE.get(),3.5F,1,3.5F);
        h.runAtTickTime(30,()->{
            h.assertTrue(w.isAlive() && !w.isRemoved(),"Spawned NPC disappeared without encounter owner");
            h.assertTrue(w.shouldBeSaved(),"Ordinary NPC must persist");
            float before=w.getHealth();
            w.hurt(h.getLevel().damageSources().playerAttack(h.makeMockSurvivalPlayer()),2);
            h.assertTrue(w.getHealth()<before && w.hurtTime>0,"Punch must damage and trigger hurt overlay");
            h.succeed();
        });
    }
    @GameTest(template="empty")
    public static void blockedSpawnReturnsNoPosition(GameTestHelper h) {
        for(int x=2;x<=22;x++) for(int z=2;z<=22;z++) for(int y=0;y<=9;y++) h.setBlock(x,y,z,Blocks.STONE);
        var w=ModEntities.WYBIE.get().create(h.getLevel());
        var found=WybieEntity.findSafeSpawn(h.getLevel(),w,h.absolutePos(new BlockPos(12,5,12)));
        h.assertTrue(found==null,"Fully obstructed spawn must fail instead of using unsafe fallback");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void spawnSearchFindsClearBodySpace(GameTestHelper h) {
        for(int x=2;x<=22;x++) for(int z=2;z<=22;z++) h.setBlock(x,0,z,Blocks.STONE);
        for(int y=1;y<=7;y++) h.setBlock(12,y,12,Blocks.STONE);
        var w=ModEntities.WYBIE.get().create(h.getLevel());
        var found=WybieEntity.findSafeSpawn(h.getLevel(),w,h.absolutePos(new BlockPos(12,1,12)));
        h.assertTrue(found!=null,"Search should find an open neighboring column");
        h.assertTrue(h.getLevel().noCollision(w,w.getBoundingBox()),"Entire entity must fit without collision");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void dollPreservesProfileThroughBlockRoundTrip(GameTestHelper h) {
        var player=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"DollTest"));
        player.getGameProfile().getProperties().put("textures",new com.mojang.authlib.properties.Property("textures","regression-profile"));
        var stack=DollItem.createFor(player);
        h.setBlock(3,1,3,ModBlocks.DOLL.get());
        var block=(DollBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(3,1,3)));
        block.load(stack.getTagElement("BlockEntityTag"));
        var recovered=block.createDollStack();
        h.assertTrue(recovered.getTagElement("BlockEntityTag").equals(stack.getTagElement("BlockEntityTag")),"Owner and texture profile changed during block round-trip");
        var saved=block.saveWithoutMetadata();
        block.load(saved);
        h.assertTrue(block.getProfile().getProperties().containsKey("textures"),"Skin property lost on reload");
        h.succeed();
    }
}
