package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.gametest.*;

/** Actual types/stats, one alarm reaction, data spawn pools and Button events.
 * Not complete navigation/combat, spawn-density or client-model acceptance. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class PostgameHostilesGameTests {
    @GameTest(template="empty")
    public static void customTypesHaveDistinctStatsAndCannotNaturallySpawnInOriginalWorlds(GameTestHelper h){
        // Full authored roster, including the nine later expansion types.
        double[] health={24,32,30,20,26,70,16,42,40,28,28,160},attack={4,6,5,4,5,12,5,8,8,5,4,10},speed={.23,.17,.24,.31,.32,.15,.27,.2,.29,.25,.22,.2};int index=0;
        h.assertTrue(PostgameHostiles.Kind.values().length==health.length,"Authored twelve-type hostile roster changed without reviewed expectations");
        for(var kind:PostgameHostiles.Kind.values()){
            var type=PostgameHostiles.TYPES.get(kind).get();var mob=type.create(h.getLevel());
            h.assertTrue(mob!=null&&mob.getClass()==OtherWorldHostile.class&&mob.kind()==kind&&type.getCategory()==MobCategory.MONSTER,"Type substituted a vanilla/entity-name placeholder: "+kind);
            h.assertTrue(mob.getAttributeBaseValue(Attributes.MAX_HEALTH)==health[index]&&mob.getAttributeBaseValue(Attributes.ATTACK_DAMAGE)==attack[index]&&Math.abs(mob.getAttributeBaseValue(Attributes.MOVEMENT_SPEED)-speed[index])<.0001,"Distinct registered attributes failed: "+kind);
            h.assertTrue(!OtherWorldHostile.naturalSpawn(type,h.getLevel(),MobSpawnType.NATURAL,h.absolutePos(new BlockPos(1,1,1)),net.minecraft.util.RandomSource.create(1)),"Postgame hostile leaked into original/vanilla spawning");mob.discard();index++;
        }
        var biomes=h.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        for(var pair:Map.of("stitched_meadows",PostgameHostiles.Kind.BUTTON_EYED_WANDERER,"clockwork_highlands",PostgameHostiles.Kind.CLOCKWORK_WATCHER,"white_rift_wastes",PostgameHostiles.Kind.RIFT_SOLDIER).entrySet()){
            var biome=biomes.get(ResourceLocation.fromNamespaceAndPath("coraline","postgame/"+pair.getKey()));
            h.assertTrue(biome!=null&&biome.getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream().anyMatch(entry->entry.type==PostgameHostiles.TYPES.get(pair.getValue()).get()&&entry.minCount>=1&&entry.maxCount<=3),"Actual biome pool is missing its bounded custom spawn group: "+pair.getKey());
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void clockworkWatcherAlertsAnActualIdleRiftSoldier(GameTestHelper h){
        for(int x=0;x<8;x++)for(int z=0;z<7;z++)h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
        var level=h.getLevel();var watcher=PostgameHostiles.TYPES.get(PostgameHostiles.Kind.CLOCKWORK_WATCHER).get().create(level);var soldier=PostgameHostiles.TYPES.get(PostgameHostiles.Kind.RIFT_SOLDIER).get().create(level);
        var at=h.absolutePos(new BlockPos(2,1,2));watcher.setPos(Vec3.atBottomCenterOf(at));soldier.setPos(Vec3.atBottomCenterOf(at.east(3)));soldier.setNoAi(true);level.addFreshEntity(watcher);level.addFreshEntity(soldier);
        var target=FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"WatcherAlarmQA"));target.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);target.setPos(Vec3.atBottomCenterOf(at.south(3)));watcher.setTarget(target);
        h.assertTrue(soldier.getTarget()==null,"Idle defender fixture already had a target");watcher.aiStep();
        h.assertTrue(soldier.getTarget()==target,"Real watcher reaction did not alert its nearby custom ally");watcher.discard();soldier.discard();h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void realButtonEyedWandererUsesTheHigherSmallDropPool(GameTestHelper h){
        var level=h.getLevel().getServer().getLevel(com.coraline.otherworld.OtherWorld.KEY);h.assertTrue(level!=null,"Other World fixture absent");
        var player=FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"WandererDropQA"));player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var type=PostgameHostiles.TYPES.get(PostgameHostiles.Kind.BUTTON_EYED_WANDERER).get();h.assertTrue(type.is(OtherWorldButtonDrops.BUTTON_CREATURES),"Actual custom Button creature is not tagged for its pool");
        var corpse=type.create(level);corpse.setHealth(0);corpse.getRandom().setSeed(981377123);int hits=0,twos=0;
        for(int i=0;i<6000;i++){
            corpse.getPersistentData().remove("CoralineButtonDeathRolled");var event=new LivingDropsEvent(corpse,corpse.damageSources().playerAttack(player),new ArrayList<ItemEntity>(),0,true);OtherWorldButtonDrops.drops(event);
            if(!event.getDrops().isEmpty()){
                hits++;h.assertTrue(event.getDrops().size()==1,"One custom mob generated multiple Button stacks");var stack=event.getDrops().iterator().next().getItem();
                h.assertTrue(stack.is(com.coraline.clothing.OtherMaterials.BUTTON.get())&&stack.getCount()>=1&&stack.getCount()<=2,"Custom Button creature generated wrong/excessive loot");if(stack.getCount()==2)twos++;
            }
        }
        h.assertTrue(hits>1950&&hits<2250&&twos>700&&twos<1400,"Higher Button drop frequency/quantity failed: "+hits+" / "+twos);corpse.discard();h.succeed();
    }
}
