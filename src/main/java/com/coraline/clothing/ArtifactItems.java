package com.coraline.clothing;

import com.coraline.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

/** Utility artifacts shared by the sewing, dream and Other World equipment systems. */
public final class ArtifactItems {
    public static final RegistryObject<Item> VOID_HEART=ModItems.ITEMS.register("void_heart",()->new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> SOUL_CAPTURER=ModItems.ITEMS.register("soul_capturer",SoulCapturerItem::new);
    public static final RegistryObject<Item> DREAM_STAFF=ModItems.ITEMS.register("dream_staff",DreamStaffItem::new);
    public static final RegistryObject<Item> DREAMSTONE=ModItems.ITEMS.register("dreamstone",DreamstoneItem::new);
    public static final RegistryObject<Item> PERFECT_TREE_CUTTER=ModItems.ITEMS.register("perfect_tree_cutter",PerfectTreeCutterItem::new);
    public static final RegistryObject<Item> OTHER_FLINT_AND_STEEL=ModItems.ITEMS.register("other_flint_and_steel",()->new OtherFlintAndSteelItem(false));
    public static final RegistryObject<Item> POTENT_OTHER_FLINT_AND_STEEL=ModItems.ITEMS.register("potent_other_flint_and_steel",()->new OtherFlintAndSteelItem(true));
    public static final RegistryObject<Item> MIDNIGHT_CROSSBOW=ModItems.ITEMS.register("midnight_crossbow",()->new ArtifactCrossbowItem(false));
    public static final RegistryObject<Item> PERFECT_CROSSBOW=ModItems.ITEMS.register("perfect_crossbow",()->new ArtifactCrossbowItem(true));
    public static final RegistryObject<Item> BOTTOMLESS_LAVA_BUCKET=ModItems.ITEMS.register("bottomless_lava_bucket",()->new BottomlessBucketItem(true));
    public static final RegistryObject<Item> BOTTOMLESS_WATER_BUCKET=ModItems.ITEMS.register("bottomless_water_bucket",()->new BottomlessBucketItem(false));
    public static void init(){}
}
