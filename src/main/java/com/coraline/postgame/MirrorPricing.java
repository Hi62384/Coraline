package com.coraline.postgame;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Conservative stored-state whitelist and deterministic prices. A price
 * override never overrides currency/progression/data safety. */
@Mod.EventBusSubscriber(modid="coraline")
final class MirrorPricing {
    enum Problem { NONE, EMPTY, RESTRICTED, UNSUPPORTED, QUANTITY }
    record Price(Problem problem,int diamonds,int quantity){boolean valid(){return problem==Problem.NONE;}}
    private record Closure(boolean perfect,List<? extends String> deny,Set<Item> blocked){}
    private static final Map<RecipeManager,Closure> CLOSURES=new WeakHashMap<>();
    private static final Set<String> MATERIALS=Set.of("button","button_eye","other_iron_ingot","other_iron_block","other_iron_ore","soulglass","soulglass_block","soulglass_ore","perfect_ingot","perfect_block","perfect_ore","perfect_essence");
    private static final Set<String> TOOLS=Set.of("story_shoe","needle_rapier","button_mace","spidervision_sword","thread_whip","midnight_crossbow","perfect_crossbow","perfect_tree_cutter","perfect_excavating_tool","other_flint_and_steel","potent_other_flint_and_steel","bottomless_water_bucket","bottomless_lava_bucket","other_iron_lockpick","other_iron_thread_cutter","other_iron_drill_disruptor");
    private static final Set<String> CLOTHES=Set.of("midnight_coat","star_shirt","raincoat","comfy_pajamas","garden_overalls","circus_jacket","silent_slippers","feather_boots","top_hat","midnight_goggles");
    private static boolean hardRestricted(ResourceLocation id){
        String path=id.getPath();if(!id.getNamespace().equals("coraline"))return false;
        return path.contains("void")||path.equals("soul_capturer")||path.equals("ghost_eye")||path.equals("seeing_stone")||path.contains("story_key")||path.contains("door_key")||path.contains("soul_record")||path.contains("soul_token");
    }
    private static Set<Item> blocked(ServerLevel level){
        var manager=level.getRecipeManager();boolean perfect=MirrorConfig.PERFECT.get();var deny=MirrorConfig.DENY.get();var old=CLOSURES.get(manager);
        if(old!=null&&old.perfect==perfect&&old.deny.equals(deny))return old.blocked;
        var ids=new HashSet<String>(List.of("other_diamond","other_diamond_block","other_diamond_ore","moonstone","moonstone_block","moonstone_ore"));
        if(!perfect)ids.addAll(List.of("perfect_ingot","perfect_block","perfect_ore","perfect_essence"));var items=new HashSet<Item>();
        for(var entry:ForgeRegistries.ITEMS.getEntries())if(entry.getKey().location().getNamespace().equals("coraline")&&ids.contains(entry.getKey().location().getPath()))items.add(entry.getValue());
        // Close processing/storage/conversion paths backward from forbidden
        // currency. This is cached per recipe reload, not a per-tick scan.
        boolean changed;
        do{changed=false;for(var recipe:manager.getRecipes()){
            ItemStack result;try{result=recipe.getResultItem(level.registryAccess());}catch(RuntimeException unsupported){continue;}
            if(!result.isEmpty()&&items.contains(result.getItem()))for(var ingredient:recipe.getIngredients())for(var input:ingredient.getItems())if(!input.isEmpty()&&items.add(input.getItem()))changed=true;
        }}while(changed);
        for(String entry:deny){var key=ResourceLocation.tryParse(entry);var item=key==null?null:ForgeRegistries.ITEMS.getValue(key);if(item!=null)items.add(item);}
        var result=Set.copyOf(items);CLOSURES.put(manager,new Closure(perfect,List.copyOf(deny),result));return result;
    }
    @SubscribeEvent public static void reload(OnDatapackSyncEvent event){CLOSURES.clear();}
    static Problem eligibility(ServerLevel level,ItemStack stack){
        if(stack.isEmpty())return Problem.EMPTY;var id=ForgeRegistries.ITEMS.getKey(stack.getItem());if(id==null||blocked(level).contains(stack.getItem())||hardRestricted(id))return Problem.RESTRICTED;
        if(stack.getCount()<1||stack.getCount()>stack.getMaxStackSize()||stack.getMaxStackSize()<1||stack.getMaxStackSize()>64)return Problem.UNSUPPORTED;
        if(stack.getItem() instanceof BlockItem block&&block.getBlock() instanceof EntityBlock||stack.is(Items.BUNDLE)||stack.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent())return Problem.UNSUPPORTED;
        if(id.getNamespace().equals("coraline")&&!MATERIALS.contains(id.getPath())&&!TOOLS.contains(id.getPath())&&!CLOTHES.contains(id.getPath())&&!InfiniteContent.material(stack.getItem())&&!com.coraline.clothing.OtherMaterials.TOOLS.containsKey(id.getPath())&&!(stack.getItem() instanceof ArmorItem))return Problem.RESTRICTED;
        var root=stack.serializeNBT();for(String key:root.getAllKeys())if(!Set.of("id","Count","tag","ForgeCaps").contains(key))return Problem.UNSUPPORTED;
        if(root.contains("ForgeCaps")&&(!root.contains("ForgeCaps",Tag.TAG_COMPOUND)||!root.getCompound("ForgeCaps").isEmpty()))return Problem.UNSUPPORTED;
        var tag=stack.getTag();if(tag==null||tag.isEmpty())return Problem.NONE;
        for(String key:tag.getAllKeys())if(!Set.of("Damage","RepairCost","Unbreakable","display","Enchantments","StoredEnchantments").contains(key))return Problem.UNSUPPORTED;
        if(tag.contains("Damage")&&(!stack.isDamageableItem()||!tag.contains("Damage",Tag.TAG_INT)||tag.getInt("Damage")<0||tag.getInt("Damage")>stack.getMaxDamage()))return Problem.UNSUPPORTED;
        if(tag.contains("RepairCost")&&(!tag.contains("RepairCost",Tag.TAG_INT)||tag.getInt("RepairCost")<0||tag.getInt("RepairCost")>1000000))return Problem.UNSUPPORTED;
        if(tag.contains("Unbreakable")&&(!tag.contains("Unbreakable",Tag.TAG_BYTE)||tag.getByte("Unbreakable")<0||tag.getByte("Unbreakable")>1))return Problem.UNSUPPORTED;
        if(tag.contains("display")){
            if(!tag.contains("display",Tag.TAG_COMPOUND))return Problem.UNSUPPORTED;var display=tag.getCompound("display");
            for(String key:display.getAllKeys())if(!Set.of("Name","Lore","color").contains(key))return Problem.UNSUPPORTED;
            if(display.contains("Name")&&(!display.contains("Name",Tag.TAG_STRING)||display.getString("Name").length()>512))return Problem.UNSUPPORTED;
            if(display.contains("color")&&(!display.contains("color",Tag.TAG_INT)||display.getInt("color")<0||display.getInt("color")>0xffffff))return Problem.UNSUPPORTED;
            if(display.contains("Lore")){if(!display.contains("Lore",Tag.TAG_LIST))return Problem.UNSUPPORTED;var lore=display.getList("Lore",Tag.TAG_STRING);if(lore.size()>16||lore.size()!=((ListTag)display.get("Lore")).size())return Problem.UNSUPPORTED;for(var line:lore)if(line.getAsString().length()>1024)return Problem.UNSUPPORTED;}
        }
        for(String name:List.of("Enchantments","StoredEnchantments"))if(tag.contains(name)){
            if(!tag.contains(name,Tag.TAG_LIST))return Problem.UNSUPPORTED;var list=tag.getList(name,Tag.TAG_COMPOUND);if(list.size()>32||list.size()!=((ListTag)tag.get(name)).size())return Problem.UNSUPPORTED;
            var seen=new HashSet<String>();for(var raw:list){var enchant=(CompoundTag)raw;if(!enchant.getAllKeys().equals(Set.of("id","lvl"))||!enchant.contains("id",Tag.TAG_STRING)||!enchant.contains("lvl",Tag.TAG_SHORT)||enchant.getShort("lvl")<1||enchant.getShort("lvl")>255)return Problem.UNSUPPORTED;var key=ResourceLocation.tryParse(enchant.getString("id"));if(key==null||!seen.add(key.toString())||!ForgeRegistries.ENCHANTMENTS.containsKey(key))return Problem.UNSUPPORTED;}
        }
        return Problem.NONE;
    }
    private static int units(ItemStack stack){
        var id=ForgeRegistries.ITEMS.getKey(stack.getItem());String name=id.toString();
        for(String row:MirrorConfig.OVERRIDES.get()){int split=row.lastIndexOf('=');if(split>0&&row.substring(0,split).equals(name))return Integer.parseInt(row.substring(split+1));}
        if(name.equals("minecraft:diamond"))return 256;if(name.equals("minecraft:diamond_block"))return 2304;
        if(name.equals("minecraft:diamond_ore")||name.equals("minecraft:deepslate_diamond_ore"))return 1024;
        if(name.equals("minecraft:iron_ingot")||name.equals("minecraft:raw_iron"))return 4;if(name.equals("minecraft:iron_block")||name.equals("minecraft:raw_iron_block"))return 36;
        if(name.equals("minecraft:gold_ingot")||name.equals("minecraft:raw_gold"))return 8;if(name.equals("minecraft:gold_block")||name.equals("minecraft:raw_gold_block"))return 72;
        if(stack.is(ItemTags.LOGS)||stack.is(ItemTags.PLANKS)||Set.of("minecraft:cobblestone","minecraft:stone","minecraft:cobbled_deepslate","minecraft:dirt","minecraft:gravel","minecraft:sand","minecraft:deepslate","minecraft:netherrack").contains(name))return MirrorConfig.COMMON.get();
        if(stack.getItem() instanceof ArmorItem||stack.getItem() instanceof TieredItem||stack.isDamageableItem()||stack.getMaxStackSize()==1)return MirrorConfig.EQUIPMENT.get();
        if(id.getNamespace().equals("coraline"))return MATERIALS.contains(id.getPath())?MirrorConfig.RARE.get():MirrorConfig.EQUIPMENT.get();
        if(!id.getNamespace().equals("minecraft"))return MirrorConfig.UNKNOWN.get();
        if(stack.getItem() instanceof BlockItem)return MirrorConfig.PROCESSED.get();return MirrorConfig.NATURAL.get();
    }
    static Price price(ServerLevel level,ItemStack stack,int quantity){
        var problem=eligibility(level,stack);if(problem!=Problem.NONE)return new Price(problem,0,0);
        if(quantity<1||quantity>Math.min(stack.getCount(),Math.min(stack.getMaxStackSize(),MirrorConfig.MAX_QUANTITY.get())))return new Price(Problem.QUANTITY,0,0);
        long value=units(stack);var tag=stack.getTag();if(tag!=null){for(String key:List.of("Enchantments","StoredEnchantments"))for(var entry:tag.getList(key,Tag.TAG_COMPOUND))value+=128L*((CompoundTag)entry).getShort("lvl");if(tag.getBoolean("Unbreakable"))value+=65536;}
        long diamonds=Math.max(1,(value*quantity+63)/64);return diamonds>Integer.MAX_VALUE?new Price(Problem.UNSUPPORTED,0,0):new Price(Problem.NONE,(int)diamonds,quantity);
    }
    private MirrorPricing(){}
}
