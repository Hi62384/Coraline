package com.coraline.feast;
import net.minecraft.world.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;
/** Additive registration: no replacement of the existing character or vehicle registries. */
@Mod.EventBusSubscriber(modid="coraline",bus=Mod.EventBusSubscriber.Bus.MOD)
public final class WybieAdventureEntities {
    public static final RegistryObject<EntityType<WybieBicycleEntity>> BICYCLE=com.coraline.registry.ModEntities.WYBIE_BICYCLE;
    @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e){WybieAdventureNetwork.register();}
    private WybieAdventureEntities(){}
}
