package com.coraline.clothing;

import com.coraline.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

public final class UtilityItems {
    public static final RegistryObject<Item> SEWING_NEEDLE=ModItems.ITEMS.register("beldam_sewing_needle",BeldamSewingNeedle::new);
    public static final RegistryObject<Item> MOONLIT_LANTERN=ModItems.ITEMS.register("moonlit_lantern",()->new net.minecraft.world.item.BlockItem(com.coraline.registry.ModBlocks.MOONLIT_LANTERN.get(),new Item.Properties()));
    public static final RegistryObject<Item> EXCAVATOR=ModItems.ITEMS.register("perfect_excavating_tool",PerfectExcavatorItem::new);
    public static final RegistryObject<Item> BUILDERS_DREAM=ModItems.ITEMS.register("builders_dream",()->new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MAGIC_UMBRELLA=ModItems.ITEMS.register("magic_umbrella",()->new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MANTIS_ARM=ModItems.ITEMS.register("garden_mantis_arm",GardenMantisArmItem::new);
    public static void init(){}
}
