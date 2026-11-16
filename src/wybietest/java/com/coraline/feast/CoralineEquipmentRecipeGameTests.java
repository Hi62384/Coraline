package com.coraline.feast;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

/** Registry-backed positive/negative matching for every authored armor piece
 * and equipment recipe. Crafting is not proof of the resulting item's ability. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class CoralineEquipmentRecipeGameTests {
    private static TransientCraftingContainer grid() {
        return new TransientCraftingContainer(new AbstractContainerMenu(null,-1) {
            @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
            @Override public boolean stillValid(Player p){return true;}
        },3,3);
    }
    private static Map<String,Integer> check(GameTestHelper h,String name) {
        var id=ResourceLocation.fromNamespaceAndPath("coraline",name);
        h.assertTrue(ForgeRegistries.ITEMS.containsKey(id),"Recipe output is not a registered item: "+name);
        var raw=h.getLevel().getRecipeManager().byKey(id).orElse(null);
        h.assertTrue(raw instanceof ShapedRecipe,"Real shaped recipe missing: "+name);
        var recipe=(ShapedRecipe)raw;var grid=grid();var counts=new HashMap<String,Integer>();
        for(int y=0;y<recipe.getHeight();y++)for(int x=0;x<recipe.getWidth();x++) {
            var ingredient=recipe.getIngredients().get(y*recipe.getWidth()+x);
            if(ingredient.isEmpty())continue;
            var options=ingredient.getItems();h.assertTrue(options.length>0,"Unresolved registry/tag ingredient in "+name);
            var input=options[0].copyWithCount(1);grid.setItem(y*3+x,input);
            counts.merge(ForgeRegistries.ITEMS.getKey(input.getItem()).toString(),1,Integer::sum);
        }
        h.assertTrue(recipe.matches(grid,h.getLevel()),"Positive survival grid failed: "+name);
        var output=recipe.assemble(grid,h.getLevel().registryAccess());
        h.assertTrue(output.is(ForgeRegistries.ITEMS.getValue(id))&&output.getCount()==(name.equals("button_eye")?2:1),"Wrong result or duplicate output: "+name);
        var matches=h.getLevel().getRecipeManager().getRecipesFor(RecipeType.CRAFTING,grid,h.getLevel());
        h.assertTrue(matches.stream().allMatch(other->other.getResultItem(h.getLevel().registryAccess()).is(output.getItem())),"Another recipe takes the same real ingredient grid: "+name);
        h.assertTrue(recipe.getRemainingItems(grid).stream().allMatch(ItemStack::isEmpty),"Consumed progression material was returned unexpectedly: "+name);
        for(int slot=0;slot<9;slot++)if(!grid.getItem(slot).isEmpty()) {
            var input=grid.getItem(slot).copy();grid.setItem(slot,ItemStack.EMPTY);
            h.assertFalse(recipe.matches(grid,h.getLevel()),"Missing ingredient still crafts: "+name+" slot="+slot);
            grid.setItem(slot,new ItemStack(Items.STONE));
            h.assertFalse(recipe.matches(grid,h.getLevel()),"Wrong ingredient still crafts: "+name+" slot="+slot);
            grid.setItem(slot,input);
        }
        var mirror=grid();for(int y=0;y<3;y++)for(int x=0;x<3;x++)mirror.setItem(y*3+x,grid.getItem(y*3+2-x).copy());
        h.assertTrue(recipe.matches(mirror,h.getLevel()),"Standard mirrored crafting failed: "+name);
        h.assertTrue(h.getLevel().getServer().getAdvancements().getAdvancement(ResourceLocation.fromNamespaceAndPath("coraline","recipes/equipment/"+name))!=null,"Recipe-book unlock did not load: "+name);
        return counts;
    }
    @GameTest(template="empty",timeoutTicks=200)
    public static void everyArmorPieceCraftsAndRejectsMissingIngredients(GameTestHelper h) {
        for(String set:List.of("dreamweave","comfy_silk","midnight","buttonguard","midnight_cloak","luck","spidervision","beldam_weave","soulbound","super_spidervision","potent_beldam_weave","soulbound_potent_beldam_weave","other_world_void","perfect"))
            for(String piece:List.of("helmet","chestplate","leggings","boots")) {
                var counts=check(h,set+"_"+piece);
                if(set.equals("dreamweave")||set.equals("midnight")||set.equals("midnight_cloak"))h.assertTrue(counts.getOrDefault("coraline:moonstone",0)==1,"Moonstone is no longer a minor armor ingredient: "+set+"_"+piece);
                if(set.equals("luck"))h.assertTrue(counts.getOrDefault("coraline:perfect_ingot",0)==1,"Luck armor uses excessive or missing Perfect: "+piece);
                if(set.equals("buttonguard"))h.assertTrue(counts.size()==1&&counts.containsKey("coraline:button"),"Buttonguard is not Button-only: "+piece);
                if(set.equals("beldam_weave"))h.assertTrue(counts.size()==1&&counts.containsKey("coraline:soulglass"),"Beldam Weave is not Soul Glass-only: "+piece);
                if(set.equals("other_world_void"))h.assertTrue(counts.getOrDefault("coraline:void_heart",0)==1,"Void armor bypasses repeated story progression: "+piece);
            }
        check(h,"miners_helmet");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200)
    public static void everySpecifiedExistingToolAndWeaponHasARealCraftingPath(GameTestHelper h) {
        for(String name:List.of("story_shoe","soul_capturer","dream_staff","dreamstone","perfect_tree_cutter","other_flint_and_steel","potent_other_flint_and_steel","moonlit_lantern","perfect_excavating_tool","builders_dream","midnight_crossbow","perfect_crossbow","needle_rapier","button_mace","spidervision_sword","thread_whip","midnight_wand","button_eye","unbinding_button_eyes","other_void_sword")) {
            var counts=check(h,name);
            switch(name) {
                case "soul_capturer" -> h.assertTrue(counts.getOrDefault("coraline:void_heart",0)==1&&counts.getOrDefault("coraline:soulglass_block",0)==1&&counts.containsKey("coraline:other_iron_ingot"),"Soul Capturer lost required progression ingredients");
                case "dreamstone" -> h.assertTrue(counts.equals(Map.of("coraline:moonstone_block",1,"coraline:other_diamond",4)),"Dream Stone costs are wrong");
                case "needle_rapier" -> h.assertTrue(counts.getOrDefault("coraline:perfect_ingot",0)==1,"Rapier does not require exactly one Perfect Ingot");
                case "thread_whip" -> h.assertTrue(counts.getOrDefault("coraline:perfect_ore",0)==2&&!counts.containsKey("coraline:perfect_ingot"),"Thread Whip silently substituted another Perfect material");
                case "story_shoe" -> h.assertTrue(counts.size()==1&&counts.containsKey("minecraft:leather"),"Shoe lacks its ordinary leather crafting path");
                case "button_eye" -> h.assertTrue(counts.equals(Map.of("coraline:button",4,"minecraft:string",2,"coraline:other_iron_ingot",1)),"Explicit Button Eye preparation conversion is wrong");
                case "unbinding_button_eyes" -> {
                    h.assertTrue(counts.equals(Map.of("coraline:soulglass",6,"coraline:button_eye",2,"coraline:moonstone",1)),"Unbinding eyes lost the required exact ingredients");
                    var recipe=(ShapedRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("coraline",name)).orElseThrow();
                    h.assertFalse(recipe.getIngredients().get(3).test(new ItemStack(com.coraline.clothing.OtherMaterials.BUTTON.get()))||recipe.getIngredients().get(3).test(new ItemStack(com.coraline.registry.ModItems.GHOST_EYE.get())),"Ordinary Buttons/quest soul eyes silently substituted for Button Eyes");
                }
                case "other_void_sword" -> h.assertTrue(counts.equals(Map.of("coraline:void_heart",2,"minecraft:stick",1)),"Void sword bypassed its two-completion material cost");
            }
        }
        h.succeed();
    }
    @GameTest(template="empty")
    public static void specifiedClothingRemainsLootOnly(GameTestHelper h) {
        var forbidden=Set.of("midnight_coat","star_shirt","raincoat","comfy_pajamas","garden_overalls","circus_jacket","silent_slippers","feather_boots","top_hat","midnight_goggles");
        for(var recipe:h.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            var id=ForgeRegistries.ITEMS.getKey(recipe.getResultItem(h.getLevel().registryAccess()).getItem());
            h.assertFalse(id!=null&&id.getNamespace().equals("coraline")&&forbidden.contains(id.getPath()),"Loot-only clothing has a loaded crafting recipe: "+id);
        }
        h.succeed();
    }
    @GameTest(template="empty")
    public static void rareMaterialsRequireTheirAcquisitionPathsAndBlockConversionsBalance(GameTestHelper h) {
        for(String material:List.of("moonstone","soulglass")) {
            var grid=grid();
            for(int slot:new int[]{1,3,5,7})grid.setItem(slot,new ItemStack(material.equals("moonstone")?Items.AMETHYST_SHARD:Items.GLASS));
            grid.setItem(4,new ItemStack(material.equals("moonstone")?Items.DIAMOND:Items.SOUL_SAND));
            h.assertTrue(h.getLevel().getRecipeManager().getRecipesFor(RecipeType.CRAFTING,grid,h.getLevel()).stream().noneMatch(recipe->ForgeRegistries.ITEMS.getKey(recipe.getResultItem(h.getLevel().registryAccess()).getItem()).toString().equals("coraline:"+material)),"Mundane ingredients still bypass rare material progression: "+material);
            var ore=ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("coraline",material+"_ore"));
            var container=new net.minecraft.world.SimpleContainer(new ItemStack(ore));
            var recipe=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING,container,h.getLevel()).orElse(null);
            h.assertTrue(recipe!=null,"Silk-harvested ore has no processing recipe: "+material);
            var output=recipe.assemble(container,h.getLevel().registryAccess());
            h.assertTrue(output.is(ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("coraline",material)))&&output.getCount()==1,"Processing multiplies or loses rare material");
        }
        for(String material:List.of("other_iron_ingot","other_diamond","moonstone","perfect_ingot","soulglass")) {
            String block=material.replace("_ingot","")+"_block";
            var unit=ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("coraline",material));
            var storage=ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("coraline",block));
            var full=grid();for(int slot=0;slot<9;slot++)full.setItem(slot,new ItemStack(unit));
            var compress=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,full,h.getLevel()).orElse(null);
            h.assertTrue(compress!=null&&compress.assemble(full,h.getLevel().registryAccess()).is(storage)&&compress.assemble(full,h.getLevel().registryAccess()).getCount()==1,"Storage does not consume nine units: "+material);
            var single=grid();single.setItem(0,new ItemStack(storage));
            var expand=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,single,h.getLevel()).orElse(null);
            h.assertTrue(expand!=null&&expand.assemble(single,h.getLevel().registryAccess()).is(unit)&&expand.assemble(single,h.getLevel().registryAccess()).getCount()==9,"Storage conversion duplicates or loses material: "+material);
        }
        h.succeed();
    }
    private CoralineEquipmentRecipeGameTests() {}
}
