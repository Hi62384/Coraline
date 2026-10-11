package com.coraline;

import com.coraline.registry.ModBlockEntities;
import com.coraline.registry.ModBlocks;
import com.coraline.registry.ModEntities;
import com.coraline.registry.ModItems;
import com.coraline.registry.ModSounds;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CoralineMod.MOD_ID)
public final class CoralineMod {
    public static final String MOD_ID = "coraline";

    public CoralineMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        context.registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, com.coraline.config.CoralineConfig.SPEC);
        context.registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT, com.coraline.config.OtherWorldVisualConfig.SPEC,"coraline-visuals-client.toml");
        context.registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,com.coraline.clothing.ClothingConfig.SPEC,"coraline-clothing.toml");
        context.registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,com.coraline.food.FoodConfig.SPEC,"coraline-foods.toml");
        context.registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,com.coraline.clothing.WeaponConfig.SPEC,"coraline-weapons.toml");

        context.registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON,com.coraline.feast.VisitGiftConfig.SPEC,"coraline-visit-gifts.toml");
        context.registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON,com.coraline.secrets.SecretsConfig.SPEC,"coraline-secrets.toml");
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        com.coraline.secrets.SecretsItems.init();
        ModItems.register(modEventBus);
        com.coraline.secrets.SecretsNetwork.register();
        com.coraline.clothing.PerfectHarvestLoot.init();
        com.coraline.clothing.MinerLoot.SERIALIZERS.register(modEventBus);
        com.coraline.clothing.SpiderControls.register();
        com.coraline.feast.FeastContent.init();
        com.coraline.food.TreatFoods.init();
        com.coraline.feast.VisitAudio.register();
        com.coraline.feast.CakeContestNetwork.register();
        com.coraline.feast.CarnivalNetwork.register();
        com.coraline.feast.FestivalNetwork.register();
        com.coraline.feast.ExpeditionNetwork.register();
        com.coraline.clothing.ArtifactNetwork.register();
        context.registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON,com.coraline.feast.VisitConfig.SPEC,"coraline-first-visit.toml");
        ModSounds.register(modEventBus);
        com.coraline.parents.ParentStatus.register();
        com.coraline.otherworld.RealmVisualState.register();
        com.coraline.registry.ModMenus.MENUS.register(modEventBus);
        com.coraline.registry.ModCreativeTabs.TABS.register(modEventBus);
    }
}
