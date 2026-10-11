package com.coraline.feast;

import com.coraline.clothing.*;
import com.coraline.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import java.util.*;
import java.util.function.*;

/** All visit-present definitions and progression rules live here, independently of activity prizes. */
public final class VisitGiftPools {
    public record Progress(int tier,boolean moon,boolean diamond,boolean perfect){}
    public record Category(OtherMotherDialogue.Loot kind,Predicate<Progress> eligible,BiFunction<RandomSource,Progress,List<ItemStack>> rewards){
        public String id(){return kind.id();}
        public String displayName(){return kind.displayName;}
        public int weight(){return VisitGiftConfig.WEIGHTS.get(kind).get();}
        public OtherMotherDialogue.Pool before(){return OtherMotherDialogue.lootPool(kind,OtherMotherDialogue.Moment.BEFORE);}
        public OtherMotherDialogue.Pool after(){return OtherMotherDialogue.lootPool(kind,OtherMotherDialogue.Moment.AFTER);}
    }
    private static final Map<OtherMotherDialogue.Loot,Category> ALL=new EnumMap<>(OtherMotherDialogue.Loot.class);
    static {
        define(OtherMotherDialogue.Loot.FOOD,VisitGiftPools::food);
        define(OtherMotherDialogue.Loot.TREASURE,VisitGiftPools::treasure);
        define(OtherMotherDialogue.Loot.WEAPONS,VisitGiftPools::weapons);
        define(OtherMotherDialogue.Loot.ARMOR,VisitGiftPools::armor);
        define(OtherMotherDialogue.Loot.TOOLS,VisitGiftPools::tools);
        define(OtherMotherDialogue.Loot.BUILDING_MATERIALS,VisitGiftPools::building);
        define(OtherMotherDialogue.Loot.REDSTONE,VisitGiftPools::redstone);
        define(OtherMotherDialogue.Loot.CLOTHING,VisitGiftPools::clothing);
        define(OtherMotherDialogue.Loot.MUSIC,VisitGiftPools::music);
        define(OtherMotherDialogue.Loot.MYSTERY,VisitGiftPools::mystery);
    }
    private static void define(OtherMotherDialogue.Loot kind,BiFunction<RandomSource,Progress,List<ItemStack>> factory){ALL.put(kind,new Category(kind,p->true,factory));}
    public static Collection<Category> categories(){return Collections.unmodifiableCollection(ALL.values());}
    public static Category category(OtherMotherDialogue.Loot kind){return Objects.requireNonNull(ALL.get(kind));}
    public static Progress progress(ServerPlayer p,VisitData.Scene s){return new Progress(s.loop.visits<4?0:s.loop.visits<9?1:2,
        MineshaftActivity.knownMaterial(p,"moonstone"),MineshaftActivity.knownMaterial(p,"other_diamond"),knownPerfect(p));}
    private static boolean knownPerfect(ServerPlayer p){
        for(int i=0;i<p.getInventory().getContainerSize();i++){var item=p.getInventory().getItem(i).getItem();
            if(item==ModItems.PERFECT_INGOT.get()||item instanceof TieredItem tool&&tool.getTier()==OtherMaterials.PERFECT||item instanceof SetArmorItem armor&&armor.set==ArmorSets.Set.PERFECT)return true;
        }return false;
    }
    static OtherMotherDialogue.Loot choose(RandomSource random,Progress progress,ToIntFunction<Category> weights){
        int total=0;for(var c:categories())if(c.eligible.test(progress))total+=Math.max(0,weights.applyAsInt(c));
        if(total==0)return random.nextBoolean()?OtherMotherDialogue.Loot.FOOD:OtherMotherDialogue.Loot.TREASURE;
        int roll=random.nextInt(total);for(var c:categories())if(c.eligible.test(progress)){roll-=Math.max(0,weights.applyAsInt(c));if(roll<0)return c.kind;}
        throw new IllegalStateException("Visit gift weight selection failed");
    }
    static OtherMotherDialogue.Loot chooseConfigured(RandomSource random,Progress progress,ToIntFunction<Category> weights,boolean equal){
        // Normalize before rolling the category, not after choosing an item. Pool
        // sizes and persisted pre-update weights must not bias the default odds.
        return choose(random,progress,c->{int weight=Math.max(0,weights.applyAsInt(c));return equal&&weight>0?1:weight;});
    }
    public static OtherMotherDialogue.Loot choose(ServerPlayer p,VisitData.Scene s){return chooseConfigured(p.getRandom(),progress(p,s),Category::weight,VisitGiftConfig.EQUAL_CATEGORY_CHANCES.get());}
    public static List<ItemStack> generate(OtherMotherDialogue.Loot kind,RandomSource random,Progress progress){
        var category=category(kind);if(!category.eligible.test(progress))throw new IllegalArgumentException("Ineligible gift "+kind);
        var items=category.rewards.apply(random,progress);
        if(items.isEmpty()||items.stream().anyMatch(stack->stack.isEmpty()||stack.getCount()>stack.getMaxStackSize()||ExpeditionActivities.temporary(stack)))throw new IllegalStateException("Invalid visit reward "+kind);
        return items.stream().map(ItemStack::copy).toList();
    }
    private static Item pick(RandomSource r,Item... items){return items[r.nextInt(items.length)];}
    private static ItemStack equipment(RandomSource r,Progress p,Item item,Enchantment enchantment){
        var stack=new ItemStack(item);if(p.tier>0&&r.nextInt(p.tier==2?2:4)==0&&enchantment.canEnchant(stack))stack.enchant(enchantment,p.tier==2?2:1);return stack;
    }
    // These are the original Food/Treasure loops, with identical order, quantities and random calls.
    private static List<ItemStack> food(RandomSource r,Progress p){var items=new ArrayList<ItemStack>();for(int theme=0;theme<4;theme++)for(int i=0;i<3;i++)items.add(FeastContent.serving(r,theme));for(int i=0;i<3;i++)items.add(FeastContent.random(r,true));return items;}
    private static List<ItemStack> treasure(RandomSource r,Progress p){var items=new ArrayList<ItemStack>();for(var item:new Item[]{Items.IRON_INGOT,Items.GOLD_INGOT,Items.COPPER_INGOT,Items.COAL,Items.LAPIS_LAZULI,Items.REDSTONE,Items.EMERALD,Items.DIAMOND,OtherMaterials.OTHER_IRON.get(),OtherMaterials.OTHER_DIAMOND.get()})items.add(new ItemStack(item,1+r.nextInt(item==Items.DIAMOND||item==OtherMaterials.OTHER_DIAMOND.get()?3:6)));return items;}
    private static List<ItemStack> weapons(RandomSource r,Progress p){
        Item item=pick(r,Items.IRON_SWORD,Items.BOW,Items.CROSSBOW);
        if(p.tier>=1&&r.nextInt(3)==0)item=pick(r,Weapons.ITEMS.get("needle_rapier").get(),Weapons.ITEMS.get("vine_sword").get(),Weapons.ITEMS.get("thread_whip").get(),OtherMaterials.TOOLS.get("other_iron_sword").get());
        if(p.tier==2&&r.nextInt(5)==0)item=pick(r,Items.DIAMOND_SWORD,Items.TRIDENT,Weapons.ITEMS.get("moonblade").get(),Weapons.ITEMS.get("button_mace").get(),Weapons.ITEMS.get("midnight_wand").get(),Weapons.ITEMS.get("spidervision_sword").get());
        if(p.tier==2&&p.perfect&&r.nextInt(30)==0)item=OtherMaterials.TOOLS.get("perfect_sword").get();
        var items=new ArrayList<ItemStack>();items.add(equipment(r,p,item,item instanceof BowItem?Enchantments.POWER_ARROWS:item instanceof CrossbowItem?Enchantments.QUICK_CHARGE:Enchantments.SHARPNESS));
        if(item instanceof BowItem||item instanceof CrossbowItem)items.add(new ItemStack(p.tier==2&&r.nextBoolean()?Items.SPECTRAL_ARROW:Items.ARROW,16+8*p.tier));
        return items;
    }
    private static List<ItemStack> armor(RandomSource r,Progress p){
        Item item=pick(r,Items.IRON_HELMET,Items.IRON_CHESTPLATE,Items.IRON_LEGGINGS,Items.IRON_BOOTS,Items.CHAINMAIL_CHESTPLATE);
        var type=ArmorItem.Type.values()[r.nextInt(ArmorItem.Type.values().length)];
        if(p.tier>=1&&r.nextInt(3)==0)item=ArmorSets.ITEMS.get(r.nextBoolean()?ArmorSets.Set.OTHER_IRON:ArmorSets.Set.DREAMWEAVE).get(type).get();
        if(p.tier==2&&r.nextInt(5)==0)item=pick(r,Items.DIAMOND_HELMET,Items.DIAMOND_CHESTPLATE,Items.DIAMOND_LEGGINGS,Items.DIAMOND_BOOTS);
        if(p.tier==2&&p.moon&&r.nextInt(8)==0)item=ArmorSets.ITEMS.get(ArmorSets.Set.MOONSTONE).get(type).get();
        if(p.tier==2&&p.diamond&&r.nextInt(16)==0)item=ArmorSets.ITEMS.get(ArmorSets.Set.OTHER_DIAMOND).get(type).get();
        if(p.tier==2&&p.perfect&&r.nextInt(40)==0)item=ArmorSets.ITEMS.get(ArmorSets.Set.PERFECT).get(type).get();
        return List.of(equipment(r,p,item,Enchantments.ALL_DAMAGE_PROTECTION));
    }
    private static List<ItemStack> tools(RandomSource r,Progress p){
        Item item=pick(r,Items.IRON_PICKAXE,Items.IRON_AXE,Items.IRON_SHOVEL,Items.IRON_HOE,Items.FISHING_ROD,Items.SHEARS,Items.BRUSH);
        String suffix=new String[]{"pickaxe","axe","shovel","hoe"}[r.nextInt(4)];
        if(p.tier>=1&&r.nextInt(3)==0)item=OtherMaterials.TOOLS.get("other_iron_"+suffix).get();
        if(p.tier==2&&r.nextInt(5)==0)item=pick(r,Items.DIAMOND_PICKAXE,Items.DIAMOND_AXE,Items.DIAMOND_SHOVEL);
        if(p.tier==2&&p.moon&&r.nextInt(8)==0)item=OtherMaterials.TOOLS.get("moonstone_"+suffix).get();
        if(p.tier==2&&p.diamond&&r.nextInt(16)==0)item=OtherMaterials.TOOLS.get("other_diamond_"+suffix).get();
        if(p.tier==2&&p.perfect&&r.nextInt(30)==0)item=OtherMaterials.TOOLS.get("perfect_"+suffix).get();
        return List.of(equipment(r,p,item,Enchantments.UNBREAKING));
    }
    private static ItemStack stack(Item item,int count){return new ItemStack(item,Math.min(count,item.getMaxStackSize()));}
    private static List<ItemStack> building(RandomSource r,Progress p){int bulk=32+16*p.tier;return switch(r.nextInt(4)){
        case 0->List.of(stack(Items.OAK_PLANKS,bulk),stack(Items.OAK_LOG,16+8*p.tier),stack(Items.STONE_BRICKS,bulk),stack(Items.GLASS,16+8*p.tier),stack(Items.LANTERN,4+p.tier*2));
        case 1->List.of(stack(Items.QUARTZ_BLOCK,bulk),stack(Items.POLISHED_DIORITE,bulk),stack(Items.WHITE_STAINED_GLASS,16+8*p.tier),stack(Items.CUT_COPPER,8+4*p.tier),stack(Items.LANTERN,4));
        case 2->List.of(stack(Items.OAK_LEAVES,bulk),stack(Items.OAK_FENCE,16+8*p.tier),stack(Items.MOSS_BLOCK,bulk),stack(Items.ALLIUM,8),stack(Items.LANTERN,4),stack(Items.OAK_SAPLING,4));
        default->List.of(stack(Items.CHERRY_PLANKS,bulk),stack(Items.BRICKS,bulk),stack(Items.PINK_TERRACOTTA,16+8*p.tier),stack(Items.LIGHT_BLUE_CONCRETE,16+8*p.tier),stack(Items.GLASS,16));};}
    private static List<ItemStack> redstone(RandomSource r,Progress p){int n=4+2*p.tier;return switch(r.nextInt(3)){
        case 0->List.of(stack(Items.REDSTONE,32+16*p.tier),stack(Items.HOPPER,n),stack(Items.OBSERVER,n),stack(Items.PISTON,n),stack(Items.REPEATER,n),stack(Items.DROPPER,n));
        case 1->List.of(stack(Items.RAIL,32+16*p.tier),stack(Items.POWERED_RAIL,8+4*p.tier),stack(Items.DETECTOR_RAIL,n),stack(Items.MINECART,1),stack(Items.REDSTONE_TORCH,n),stack(Items.LEVER,n));
        default->List.of(stack(Items.STICKY_PISTON,n),stack(Items.OBSERVER,n),stack(Items.COMPARATOR,n),stack(Items.REPEATER,n),stack(Items.REDSTONE,32+16*p.tier),stack(Items.STONE_BUTTON,n),stack(Items.STONE_PRESSURE_PLATE,n));};}
    private static List<ItemStack> clothing(RandomSource r,Progress p){
        Item item=pick(r,ModItems.RAINCOAT.get(),ModItems.COMFY_PAJAMAS.get(),ModItems.GARDEN_OVERALLS.get(),ModItems.TOP_HAT.get());
        if(p.tier>0&&r.nextBoolean())item=pick(r,ModItems.MIDNIGHT_COAT.get(),ModItems.STAR_SHIRT.get(),ModItems.CIRCUS_JACKET.get(),ModItems.SILENT_SLIPPERS.get(),ModItems.FEATHER_BOOTS.get(),ModItems.MIDNIGHT_GOGGLES.get());
        return List.of(new ItemStack(item));
    }
    private static List<ItemStack> music(RandomSource r,Progress p){
        Item disc=pick(r,Items.MUSIC_DISC_13,Items.MUSIC_DISC_CAT,Items.MUSIC_DISC_BLOCKS,Items.MUSIC_DISC_CHIRP,Items.MUSIC_DISC_FAR,Items.MUSIC_DISC_MALL,Items.MUSIC_DISC_MELLOHI,Items.MUSIC_DISC_STAL,Items.MUSIC_DISC_STRAD,Items.MUSIC_DISC_WARD,Items.MUSIC_DISC_WAIT);
        if(p.tier==2&&r.nextInt(8)==0)disc=Items.MUSIC_DISC_OTHERSIDE;
        return r.nextBoolean()?List.of(new ItemStack(Items.JUKEBOX),new ItemStack(disc)):List.of(new ItemStack(disc),stack(Items.NOTE_BLOCK,4+2*p.tier),new ItemStack(Items.BELL));
    }
    private static List<ItemStack> mystery(RandomSource r,Progress p){
        var kinds=new OtherMotherDialogue.Loot[]{OtherMotherDialogue.Loot.WEAPONS,OtherMotherDialogue.Loot.ARMOR,OtherMotherDialogue.Loot.TOOLS,OtherMotherDialogue.Loot.BUILDING_MATERIALS,OtherMotherDialogue.Loot.REDSTONE,OtherMotherDialogue.Loot.CLOTHING,OtherMotherDialogue.Loot.MUSIC};
        int rarity=r.nextInt(100);
        // Ordinary surprises use basic equipment even late; exceptional equipment
        // remains behind both visit and prior-material gates.
        var quality=rarity<70?new Progress(0,false,false,false):rarity<95?new Progress(Math.min(1,p.tier),false,false,false):p;
        var items=new ArrayList<ItemStack>(generate(kinds[r.nextInt(kinds.length)],r,quality));
        if(r.nextInt(4)==0)items.add(FeastContent.random(r,false));
        if(rarity>=95&&p.tier==2)items.add(stack(Items.DIAMOND,1+r.nextInt(2)));else if(rarity>=70)items.add(stack(Items.EMERALD,2+r.nextInt(3)));
        return items;
    }
    private VisitGiftPools(){}
}
