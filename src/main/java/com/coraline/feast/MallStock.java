package com.coraline.feast;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraftforge.registries.ForgeRegistries;

/** An explicit catalogue: never enumerate the item registry into a shop. */
public final class MallStock {
    public static final String[] STORES={"Candy Corner","Button Boutique","Garden Dreams","The Toy Box","Home Sweet Home","Page & Quill","Perfect Tools","Bargain Basement","Other Armory","Moonstone Jewelers","Velvet Vault","Treasure Gallery","Dream Apothecary","Melody House","Starlight Bakery","Street Food Kitchen","Builder’s Emporium","Clockwork & Redstone","Rainbow Textiles","Trail & Tackle","Moonrise Pizzeria","Sunrise Breakfast","Orchard Drinks","Sugarcloud Desserts"};
    public static final int ITEMS_PER_VISIT=8;
    public static final String[] CATEGORIES={"Sweets","Clothing","Plants & Seeds","Toys & Rails","Furniture & Lighting","Books & Enchantments","Tools","Everyday Supplies","Weapons & Armor","Gems & Materials","Luxury Equipment","Treasure","Potions & Brewing","Music","Bakery","Street Food","Building Materials","Redstone","Textiles","Outdoor Supplies","Pizza & Sandwiches","Breakfast","Drinks & Fruit","Desserts"};
    record Product(String id,int count,int price,String description,int gate){}
    private static Product p(String id,int count,int price,String description){return new Product(id,count,price,description,0);}
    private static Product gated(String id,int price,String description,int gate){return new Product(id,1,price,description,gate);}
    private static List<List<Product>> originals(){return List.of(
        List.of(p("coraline:lollipop",6,30,"A pocketful of sweet treats."),p("coraline:cocoa_beetles",6,45,"Crunchy chocolate insects."),p("coraline:cupcakes",4,60,"Freshly iced cupcakes."),p("minecraft:pumpkin_pie",4,55,"A box of spiced pies.")),
        List.of(p("coraline:star_shirt",1,180,"Wearable star-patterned clothing."),p("coraline:raincoat",1,200,"A bright Other World raincoat."),p("coraline:comfy_pajamas",1,160,"Comfortable nightwear."),p("coraline:top_hat",1,230,"Dress for a grand occasion.")),
        List.of(p("minecraft:cherry_sapling",6,40,"Grow flowering cherry trees."),p("minecraft:flowering_azalea",8,45,"Blooming garden shrubs."),p("minecraft:spore_blossom",2,90,"A ceiling flower with drifting spores."),p("minecraft:glow_berries",16,35,"Plant luminous hanging vines.")),
        List.of(p("minecraft:note_block",4,55,"Build a musical toy."),p("minecraft:minecart",1,65,"Start a model railway."),p("minecraft:rail",32,45,"Track for your railway."),p("minecraft:firework_rocket",12,70,"A bundle of flight rockets.")),
        List.of(p("minecraft:lantern",8,50,"Warm hanging lights."),p("minecraft:painting",4,40,"Decorate your home."),p("minecraft:cherry_hanging_sign",6,55,"Elegant hanging signs."),p("minecraft:sea_lantern",8,90,"Soft luminous building blocks.")),
        List.of(p("minecraft:bookshelf",8,70,"Bookshelves for your library."),p("ench:unbreaking",1,180,"Unbreaking II enchanted book."),p("ench:efficiency",1,200,"Efficiency III enchanted book."),p("ench:protection",1,220,"Protection III enchanted book.")),
        List.of(p("coraline:other_iron_pickaxe",1,240,"Durable Other Iron mining tool."),p("coraline:other_iron_shovel",1,140,"Durable Other Iron digging tool."),p("minecraft:fishing_rod",1,90,"A fishing rod for quieter days."),gated("coraline:moonstone_pickaxe",440,"At night, nearby ores glow through walls.",1)),
        List.of(p("minecraft:torch",32,10,"A useful bundle for spare change."),p("minecraft:oak_log",32,30,"Building timber."),p("minecraft:iron_ingot",8,70,"A bargain materials bundle."),p("minecraft:cooked_chicken",8,25,"A packed picnic lunch.")),
        List.of(p("coraline:needle_rapier",1,220,"A light, fast fencing blade."),p("coraline:other_iron_sword",1,320,"A durable Other Iron sword."),p("coraline:vine_sword",1,400,"An enchanted garden weapon."),gated("coraline:moonblade",450,"Strikes harder at night.",1)),
        List.of(p("minecraft:amethyst_shard",16,80,"Crystals for crafting and decoration."),p("coraline:other_iron_ingot",4,140,"Four Other Iron ingots."),gated("coraline:moonstone",250,"A rare moonlit crafting material.",1),p("minecraft:emerald",8,170,"A pouch of emeralds.")),
        List.of(p("minecraft:enchanted_golden_apple",1,950,"A rare enchanted golden apple."),p("ench:diamond_chestplate",1,650,"Diamond chestplate with Protection III."),p("ench:diamond_boots",1,500,"Diamond boots with Feather Falling IV."),gated("coraline:other_diamond_pickaxe",850,"Premium mining equipment; prior unlock required.",2)),
        List.of(p("minecraft:diamond",3,300,"Three genuine diamonds."),p("minecraft:gold_ingot",12,160,"A small gold collection."),p("minecraft:emerald",12,230,"A jewel collector's pouch."),gated("coraline:other_diamond",600,"Rare crafting material; prior unlock required.",2)),
        List.of(p("potion:night_vision",2,80,"Eight minutes of night vision per bottle."),p("potion:swiftness",2,90,"Eight minutes of speed per bottle."),p("potion:water_breathing",2,100,"Eight minutes underwater per bottle."),p("minecraft:golden_carrot",12,100,"An excellent expedition snack.")),
        List.of(p("minecraft:jukebox",1,100,"Play your collected records."),p("minecraft:music_disc_cat",1,180,"A cheerful record for home."),p("minecraft:music_disc_blocks",1,180,"A bright, rhythmic record."),p("minecraft:bell",1,130,"A decorative ringing bell.")),
        List.of(p("coraline:muffin",8,45,"A warm bakery box."),p("coraline:cupcakes",8,70,"A celebration dessert box."),p("minecraft:cake",2,80,"Two cakes to share."),p("coraline:perfect_roast_dinner",2,160,"A lavish meal for later.")),
        List.of(p("coraline:pizza",6,70,"Six freshly prepared pizzas."),p("coraline:hot_dog_in_bun",6,60,"A tray of hot dogs."),p("coraline:panini",6,70,"Toasted sandwiches."),p("coraline:lemonade",8,35,"Chilled drinks for the journey."))
    );}
    private static final List<List<Product>> CATALOGUE=expandedCatalogue();
    static List<List<Product>> catalogue(){return CATALOGUE;}
    private static List<List<Product>> expandedCatalogue(){
        var shops=new ArrayList<List<Product>>();
        for(var original:originals())shops.add(new ArrayList<>(original));
        while(shops.size()<STORES.length)shops.add(new ArrayList<>());
        pool(shops,0,6,40,"minecraft:cookie","minecraft:honey_bottle","minecraft:apple","minecraft:sweet_berries","coraline:sweet_strawberries","coraline:muffin","minecraft:melon_slice");
        pool(shops,1,1,190,"coraline:midnight_coat","coraline:garden_overalls","coraline:circus_jacket","coraline:silent_slippers","coraline:feather_boots","coraline:midnight_goggles","minecraft:leather_helmet","minecraft:leather_boots");
        pool(shops,2,8,40,"minecraft:oak_sapling","minecraft:spruce_sapling","minecraft:birch_sapling","minecraft:mangrove_propagule","minecraft:pink_petals","minecraft:allium","minecraft:lily_of_the_valley","minecraft:rose_bush","minecraft:sunflower","minecraft:torchflower","minecraft:lily_pad","minecraft:moss_block");
        pool(shops,3,4,65,"minecraft:powered_rail","minecraft:detector_rail","minecraft:activator_rail","minecraft:target","minecraft:slime_block","minecraft:honey_block","minecraft:dispenser","minecraft:firework_star","minecraft:cherry_boat");
        pool(shops,4,4,90,"coraline:blue_velvet_sofa","coraline:rose_velvet_sofa","coraline:low_marble_table","coraline:brass_floor_lamp","coraline:cherry_sideboard","coraline:flower_console","coraline:button_art_panel","coraline:moon_art_panel","coraline:modern_wall_light","minecraft:flower_pot","minecraft:candle");
        pool(shops,5,4,65,"minecraft:book","minecraft:writable_book","minecraft:paper","minecraft:lectern","minecraft:chiseled_bookshelf","minecraft:ink_sac","minecraft:glow_ink_sac","minecraft:item_frame","minecraft:map");
        pool(shops,6,1,120,"minecraft:iron_axe","minecraft:iron_hoe","minecraft:shears","minecraft:brush","minecraft:flint_and_steel","minecraft:compass","minecraft:clock","minecraft:spyglass","minecraft:bucket");
        pool(shops,7,24,30,"minecraft:cobblestone","minecraft:coal","minecraft:glass","minecraft:clay_ball","minecraft:stick","minecraft:string","minecraft:leather","minecraft:copper_ingot","minecraft:flint","minecraft:charcoal");
        pool(shops,8,1,230,"minecraft:shield","minecraft:bow","minecraft:crossbow","minecraft:iron_helmet","minecraft:iron_chestplate","minecraft:iron_leggings","minecraft:iron_boots","minecraft:golden_sword");
        pool(shops,9,8,100,"minecraft:quartz","minecraft:lapis_lazuli","minecraft:prismarine_crystals","minecraft:prismarine_shard","minecraft:glowstone_dust","minecraft:echo_shard","minecraft:gold_nugget","minecraft:amethyst_block");
        pool(shops,10,1,550,"minecraft:diamond_helmet","minecraft:diamond_leggings","minecraft:diamond_sword","minecraft:diamond_axe","minecraft:diamond_pickaxe","minecraft:diamond_shovel","minecraft:diamond_hoe","minecraft:golden_apple");
        pool(shops,11,4,140,"minecraft:gold_block","minecraft:raw_gold","minecraft:raw_iron","minecraft:raw_copper","minecraft:lapis_block","minecraft:amethyst_block","minecraft:nautilus_shell","minecraft:prismarine_crystals");
        pool(shops,12,8,85,"minecraft:blaze_powder","minecraft:nether_wart","minecraft:fermented_spider_eye","minecraft:ghast_tear","minecraft:glistering_melon_slice","minecraft:magma_cream","minecraft:rabbit_foot","minecraft:glass_bottle","minecraft:brewing_stand");
        pool(shops,13,1,170,"minecraft:music_disc_chirp","minecraft:music_disc_far","minecraft:music_disc_mall","minecraft:music_disc_mellohi","minecraft:music_disc_stal","minecraft:music_disc_strad","minecraft:music_disc_wait","minecraft:music_disc_ward","minecraft:music_disc_otherside","minecraft:note_block");
        pool(shops,14,6,45,"minecraft:bread","minecraft:cookie","minecraft:pumpkin_pie","coraline:sweet_strawberries","minecraft:honey_bottle","minecraft:apple","minecraft:sweet_berries");
        pool(shops,15,6,60,"coraline:mustard_ketchup_salsa_wrap","coraline:bacon_and_eggs","coraline:omelet","coraline:scrambled_eggs","minecraft:cooked_chicken","minecraft:baked_potato","coraline:orange_juice","minecraft:cooked_beef");
        pool(shops,16,24,65,"minecraft:quartz_bricks","minecraft:smooth_quartz","minecraft:calcite","minecraft:bricks","minecraft:cherry_planks","minecraft:bamboo_mosaic","minecraft:deepslate_tiles","minecraft:polished_blackstone_bricks","minecraft:prismarine_bricks","minecraft:cut_sandstone","minecraft:terracotta","minecraft:packed_mud","minecraft:glass","minecraft:sea_lantern");
        pool(shops,17,8,65,"minecraft:redstone","minecraft:repeater","minecraft:comparator","minecraft:observer","minecraft:piston","minecraft:sticky_piston","minecraft:hopper","minecraft:dispenser","minecraft:dropper","minecraft:redstone_lamp","minecraft:daylight_detector","minecraft:target");
        pool(shops,18,16,45,"minecraft:pink_wool","minecraft:purple_wool","minecraft:cyan_wool","minecraft:yellow_wool","minecraft:white_wool","minecraft:blue_wool","minecraft:red_carpet","minecraft:magenta_carpet","minecraft:orange_carpet","minecraft:light_blue_carpet","minecraft:pink_banner","minecraft:white_banner","minecraft:purple_dye","minecraft:cyan_dye");
        pool(shops,19,1,85,"minecraft:fishing_rod","minecraft:compass","minecraft:clock","minecraft:spyglass","minecraft:lead","minecraft:saddle","minecraft:cherry_boat","minecraft:bucket","minecraft:campfire","minecraft:iron_shovel","minecraft:iron_axe","minecraft:water_bucket");
        pool(shops,20,6,65,"coraline:pizza","coraline:panini","coraline:mustard_ketchup_salsa_wrap","coraline:hot_dog_in_bun","minecraft:bread","minecraft:baked_potato","coraline:lemonade","coraline:orange_juice","minecraft:cooked_chicken","minecraft:cooked_porkchop");
        pool(shops,21,6,55,"coraline:omelet","coraline:bacon_and_eggs","coraline:scrambled_eggs","coraline:muffin","coraline:orange_juice","minecraft:bread","minecraft:honey_bottle","minecraft:apple","coraline:sweet_strawberries","minecraft:baked_potato");
        pool(shops,22,8,40,"coraline:lemonade","coraline:orange_juice","minecraft:honey_bottle","coraline:sweet_strawberries","minecraft:apple","minecraft:melon_slice","minecraft:sweet_berries","minecraft:glow_berries","minecraft:carrot","minecraft:beetroot");
        pool(shops,23,6,55,"coraline:cupcakes","coraline:muffin","coraline:lollipop","coraline:cocoa_beetles","coraline:sweet_strawberries","minecraft:cookie","minecraft:pumpkin_pie","minecraft:cake","minecraft:honey_bottle","minecraft:sweet_berries");
        return shops.stream().map(List::copyOf).toList();
    }
    private static void pool(List<List<Product>> shops,int store,int count,int price,String... ids){
        var products=shops.get(store);
        for(var id:ids)if(products.stream().noneMatch(p->p.id.equals(id)))
            products.add(p(id,count,price,CATEGORIES[store]+" — packed to take home."));
    }
    static ItemStack stack(Product p){ItemStack s;
        if(p.id.startsWith("ench:")){String id=p.id.substring(5);if(id.startsWith("diamond_")){s=new ItemStack(id.endsWith("boots")?Items.DIAMOND_BOOTS:Items.DIAMOND_CHESTPLATE);s.enchant(id.endsWith("boots")?Enchantments.FALL_PROTECTION:Enchantments.ALL_DAMAGE_PROTECTION,id.endsWith("boots")?4:3);}else{s=new ItemStack(Items.ENCHANTED_BOOK);EnchantedBookItem.addEnchantment(s,new EnchantmentInstance(id.equals("unbreaking")?Enchantments.UNBREAKING:id.equals("efficiency")?Enchantments.BLOCK_EFFICIENCY:Enchantments.ALL_DAMAGE_PROTECTION,id.equals("unbreaking")?2:3));}}
        else if(p.id.startsWith("potion:")){s=new ItemStack(Items.POTION,p.count);net.minecraft.world.item.alchemy.PotionUtils.setPotion(s,p.id.endsWith("night_vision")?net.minecraft.world.item.alchemy.Potions.LONG_NIGHT_VISION:p.id.endsWith("swiftness")?net.minecraft.world.item.alchemy.Potions.LONG_SWIFTNESS:net.minecraft.world.item.alchemy.Potions.LONG_WATER_BREATHING);}
        else {var item=ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(p.id));if(item==null||item==Items.AIR)throw new IllegalStateException("Missing mall product: "+p.id);s=new ItemStack(item,Math.min(p.count,item.getMaxStackSize()));}return s;
    }
    public static ListTag generate(long seed,boolean moon,boolean diamond){var random=new Random(seed);var result=new ListTag();var shops=catalogue();
        for(int store=0;store<shops.size();store++){var choices=new ArrayList<>(shops.get(store));var core=choices.remove(0);Collections.shuffle(choices,random);var selected=new ArrayList<Product>();selected.add(core);for(var product:choices)if(product.gate==0||product.gate==1&&moon||product.gate==2&&diamond){selected.add(product);if(selected.size()==ITEMS_PER_VISIT)break;}
            for(int slot=0;slot<selected.size();slot++){var product=selected.get(slot);var n=new CompoundTag();n.putInt("Store",store);n.putInt("Slot",slot);n.putInt("Price",product.price);n.putInt("Stock",product.price>=150?1:3);n.putString("Description",product.description);n.put("Item",stack(product).save(new CompoundTag()));result.add(n);}}
        return result;
    }
    private MallStock(){}
}
