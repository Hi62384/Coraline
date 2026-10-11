package com.coraline.clothing;

import com.coraline.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.*;
import net.minecraftforge.registries.RegistryObject;
import java.util.*;

public final class OtherMaterials {
    // Each Forge tier needs its own requirement tag. Reusing vanilla tags makes
    // vanilla tools fail those tags because a later custom tier also claims them.
    private static TagKey<Block> requirement(String tier){return TagKey.create(Registries.BLOCK,ResourceLocation.fromNamespaceAndPath("coraline","needs_"+tier+"_tool"));}
    public static final RegistryObject<Item> OTHER_DIAMOND=ModItems.ITEMS.register("other_diamond",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> OTHER_IRON=ModItems.ITEMS.register("other_iron_ingot",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> BUTTON=ModItems.ITEMS.register("button",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> MOONSTONE=ModItems.ITEMS.register("moonstone",()->new Item(new Item.Properties()));
    public static final Tier DIAMOND=TierSortingRegistry.registerTier(new ForgeTier(3,3903,9F,6F,18,requirement("other_diamond"),()->Ingredient.of(OTHER_DIAMOND.get())),ResourceLocation.fromNamespaceAndPath("coraline","other_diamond"),List.of(Tiers.DIAMOND),List.of(Tiers.NETHERITE));
    public static final Tier IRON=TierSortingRegistry.registerTier(new ForgeTier(2,1450,7.5F,4F,17,requirement("other_iron"),()->Ingredient.of(OTHER_IRON.get())),ResourceLocation.fromNamespaceAndPath("coraline","other_iron"),List.of(Tiers.IRON),List.of(Tiers.DIAMOND));
    public static final Tier MOONSTONE_TIER=TierSortingRegistry.registerTier(new ForgeTier(3,1450,8F,3F,18,requirement("moonstone"),()->Ingredient.of(MOONSTONE.get())),ResourceLocation.fromNamespaceAndPath("coraline","moonstone"),List.of(Tiers.DIAMOND),List.of(Tiers.NETHERITE));
    public static final Tier PERFECT=TierSortingRegistry.registerTier(new ForgeTier(4,4683,14F,7F,25,requirement("perfect"),()->Ingredient.of(com.coraline.registry.ModItems.PERFECT_INGOT.get())),ResourceLocation.fromNamespaceAndPath("coraline","perfect"),List.of(Tiers.NETHERITE),List.of());
    public static final Map<String,RegistryObject<Item>> TOOLS=new LinkedHashMap<>();
    static {tools("other_diamond",DIAMOND);tools("other_iron",IRON);tools("perfect",PERFECT);moonstoneTools();}
    private static void moonstoneTools(){
        TOOLS.put("moonstone_pickaxe",ModItems.ITEMS.register("moonstone_pickaxe",()->new PickaxeItem(MOONSTONE_TIER,2,-2.8F,new Item.Properties())));
        TOOLS.put("moonstone_axe",ModItems.ITEMS.register("moonstone_axe",()->new AxeItem(MOONSTONE_TIER,7,-3F,new Item.Properties())));
        TOOLS.put("moonstone_shovel",ModItems.ITEMS.register("moonstone_shovel",()->new ShovelItem(MOONSTONE_TIER,2.5F,-3F,new Item.Properties())));
        TOOLS.put("moonstone_hoe",ModItems.ITEMS.register("moonstone_hoe",()->new HoeItem(MOONSTONE_TIER,-3,0,new Item.Properties())));
    }
    private static void tools(String name,Tier tier){
        TOOLS.put(name+"_sword",ModItems.ITEMS.register(name+"_sword",()->new SwordItem(tier,tier==PERFECT?7:3,tier==PERFECT?-2.2F:-2.4F,new Item.Properties())));
        TOOLS.put(name+"_pickaxe",ModItems.ITEMS.register(name+"_pickaxe",()->new PickaxeItem(tier,1,-2.8F,new Item.Properties())));
        TOOLS.put(name+"_axe",ModItems.ITEMS.register(name+"_axe",()->new AxeItem(tier,tier==PERFECT?12:tier==IRON?6:5,tier==PERFECT?-2.8F:-3F,new Item.Properties())));
        TOOLS.put(name+"_shovel",ModItems.ITEMS.register(name+"_shovel",()->new ShovelItem(tier,1.5F,-3F,new Item.Properties())));
        TOOLS.put(name+"_hoe",ModItems.ITEMS.register(name+"_hoe",()->tier==PERFECT?new PerfectHoeItem():new HoeItem(tier,tier==IRON?-2:-3,tier==IRON?-1:0,new Item.Properties())));
    }
    public static void init(){}
}
