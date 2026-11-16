package com.coraline.postgame;

import com.coraline.clothing.OtherMaterials;
import com.coraline.otherworld.OtherWorld;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Ordinary shared-world loot only. Story actors, bosses and completion
 * rewards never enter this path; it awards no owner/run credentials. */
@Mod.EventBusSubscriber(modid="coraline")
public final class OtherWorldButtonDrops {
    private static final String ROLLED="CoralineButtonDeathRolled";
    public static final TagKey<EntityType<?>> BUTTON_CREATURES=tag("button_drop_themed");
    public static final TagKey<EntityType<?>> STRONG_ENEMIES=tag("button_drop_strong");
    public static final TagKey<EntityType<?>> EXCLUDED=tag("button_drop_excluded");
    private static TagKey<EntityType<?>> tag(String name){return TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath("coraline",name));}
    static boolean otherWorld(ServerLevel level){return level.dimension().equals(OtherWorld.KEY)||com.coraline.feast.ActivityWorld.isActivity(level)||level.getChunkSource().getGenerator() instanceof InfiniteChunkGenerator;}
    static boolean eligible(LivingEntity entity){
        // Hard exclusions remain even if a datapack accidentally removes the
        // exclusion tag. Never infer combat loot from an NPC's display name.
        return entity instanceof Monster&&entity.getType()!=EntityType.WITHER&&entity.getType()!=EntityType.WARDEN
            &&!entity.getType().is(EXCLUDED)&&!entity.isInvulnerable();
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void drops(LivingDropsEvent event){
        var entity=event.getEntity();
        if(event.isCanceled()||entity.isAlive()||!(entity.level() instanceof ServerLevel level)||!otherWorld(level)&&!InfectedSummons.active(entity)&&!BreachRaids.active(entity)&&!ColonyInfectedMobs.active(entity)||!eligible(entity)
            ||!OtherWorldLootConfig.BUTTON_DROPS.get()||!level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)
            ||!(event.getSource().getEntity() instanceof ServerPlayer killer)||killer.isCreative()||killer.isSpectator()
            ||killer.serverLevel()!=level||entity.getPersistentData().getBoolean(ROLLED))return;
        entity.getPersistentData().putBoolean(ROLLED,true);
        boolean themed=entity.getType().is(BUTTON_CREATURES),strong=entity.getType().is(STRONG_ENEMIES);
        double base=themed?OtherWorldLootConfig.THEMED_CHANCE.get():strong?OtherWorldLootConfig.STRONG_CHANCE.get():OtherWorldLootConfig.ORDINARY_CHANCE.get();
        int looting=Math.max(0,Math.min(3,event.getLootingLevel()));
        double chance=base<=0?0:Math.min(1,base+looting*OtherWorldLootConfig.LOOTING_BONUS.get());
        if(entity.getRandom().nextDouble()>=chance)return;
        int count=1+(strong?entity.getRandom().nextInt(3):themed?entity.getRandom().nextInt(2):0);
        // Looting improves the chance, not both chance and stack size. Even a
        // high-level modded enchantment cannot generate unbounded Buttons.
        event.getDrops().add(new ItemEntity(level,entity.getX(),entity.getY(),entity.getZ(),new ItemStack(OtherMaterials.BUTTON.get(),count)));
    }
    private OtherWorldButtonDrops(){}
}
