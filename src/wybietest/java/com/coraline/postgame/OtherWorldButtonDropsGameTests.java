package com.coraline.postgame;

import java.util.*;
import com.coraline.clothing.OtherMaterials;
import com.coraline.otherworld.OtherWorld;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.gametest.*;

/** Disposable death-event and seeded payout checks, not a full survival hunt. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class OtherWorldButtonDropsGameTests {
    private static ServerLevel other(GameTestHelper h){
        var level=h.getLevel().getServer().getLevel(OtherWorld.KEY);
        h.assertTrue(level!=null,"Normal Other World dimension not loaded");return level;
    }
    private static ServerPlayer player(ServerLevel level){
        var player=FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ButtonLootQA"));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);return player;
    }
    private static LivingDropsEvent event(LivingEntity mob,ServerPlayer player,int looting){
        return new LivingDropsEvent(mob,mob.damageSources().playerAttack(player),new ArrayList<ItemEntity>(),looting,true);
    }
    @GameTest(template="empty")
    public static void buttonsExcludeOriginalWorldPetsStoryActorsAndBosses(GameTestHelper h){
        var level=h.getLevel();
        h.assertTrue(!OtherWorldButtonDrops.otherWorld(level),"Disposable Overworld became an Other World loot source");
        for(var type:List.of(EntityType.CAT,EntityType.COW,EntityType.VILLAGER,EntityType.WITHER,EntityType.WARDEN,com.coraline.registry.CharacterCast.ENTITIES.get("cat").get())){
            var entity=type.create(level);h.assertTrue(entity instanceof LivingEntity,"Missing fixture entity "+type);
            h.assertTrue(!OtherWorldButtonDrops.eligible((LivingEntity)entity),"Pet/NPC/boss accepted for ordinary combat loot: "+type);entity.discard();
        }
        var mob=EntityType.ZOMBIE.create(level);mob.setHealth(0);
        var e=event(mob,player(level),3);OtherWorldButtonDrops.drops(e);
        h.assertTrue(e.getDrops().isEmpty()&&!mob.getPersistentData().getBoolean("CoralineButtonDeathRolled"),"Overworld kill rolled Other World loot");mob.discard();h.succeed();
    }
    @GameTest(template="empty")
    public static void actualForgeDeathEventPaysOnceAndHonorsCancellation(GameTestHelper h){
        var activities=h.getLevel().getServer().getLevel(com.coraline.feast.ActivityWorld.KEY);
        h.assertTrue(activities!=null&&OtherWorldButtonDrops.otherWorld(activities),"Other World combat activity was excluded from its resource drops");
        for(var level:List.of(other(h),activities)){
        var player=player(level);var mob=EntityType.ZOMBIE.create(level);mob.setHealth(0);
        // Select a deterministic successful ordinary roll without changing the
        // production config or replacing its random source.
        long seed=0;while(net.minecraft.util.RandomSource.create(seed).nextDouble()>=.18)seed++;
        mob.getRandom().setSeed(seed);
        var canceled=event(mob,player,0);canceled.setCanceled(true);MinecraftForge.EVENT_BUS.post(canceled);
        h.assertTrue(canceled.getDrops().isEmpty()&&!mob.getPersistentData().getBoolean("CoralineButtonDeathRolled"),"Canceled death consumed a loot roll");
        var first=event(mob,player,0);MinecraftForge.EVENT_BUS.post(first);
        h.assertTrue(first.getDrops().size()==1,"Registered subscriber failed to append ordinary Button drop");
        var stack=first.getDrops().iterator().next().getItem();
        h.assertTrue(stack.is(OtherMaterials.BUTTON.get())&&stack.getCount()==1,"Ordinary mob awarded wrong item/quantity");
        var replay=event(mob,player,3);MinecraftForge.EVENT_BUS.post(replay);
        h.assertTrue(replay.getDrops().isEmpty(),"Repeated death event paid the same corpse again");mob.discard();
        }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void actualSeededDropsRespectChanceLootingCapAndSmallStacks(GameTestHelper h){
        var level=other(h);var player=player(level);
        h.assertTrue(Math.abs(OtherWorldLootConfig.ORDINARY_CHANCE.get()-.18)<.0001&&Math.abs(OtherWorldLootConfig.STRONG_CHANCE.get()-.28)<.0001&&Math.abs(OtherWorldLootConfig.LOOTING_BONUS.get()-.025)<.0001,"Fixture requires clean default disposable loot config");
        for(var type:List.of(EntityType.ZOMBIE,EntityType.RAVAGER)){
            var mob=type.create(level);mob.setHealth(0);boolean strong=type==EntityType.RAVAGER;
            h.assertTrue(type.is(OtherWorldButtonDrops.STRONG_ENEMIES)==strong,"Strong-enemy data tag failed to load");
            for(int looting:new int[]{0,3,100}){
                mob.getRandom().setSeed(742971);int rolls=6000,hits=0;
                for(int i=0;i<rolls;i++){
                    mob.getPersistentData().remove("CoralineButtonDeathRolled");var e=event(mob,player,looting);OtherWorldButtonDrops.drops(e);
                    if(!e.getDrops().isEmpty()){
                        hits++;h.assertTrue(e.getDrops().size()==1,"One death generated multiple Button stacks");var stack=e.getDrops().iterator().next().getItem();
                        h.assertTrue(stack.is(OtherMaterials.BUTTON.get())&&stack.getCount()>=1&&stack.getCount()<=(strong?3:1),"Looting generated excessive/wrong resources");
                    }
                }
                double expected=(strong?.28:.18)+(looting==0?0:.075);
                h.assertTrue(Math.abs(hits/(double)rolls-expected)<.025,"Measured Button payout outside seeded probability tolerance: "+type+" / "+looting+" / "+hits);
            }mob.discard();
        }h.succeed();
    }
    private OtherWorldButtonDropsGameTests(){}
}
