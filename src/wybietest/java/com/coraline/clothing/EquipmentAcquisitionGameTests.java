package com.coraline.clothing;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

/** Audited previously source-less equipment; native grids, not ability proof. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class EquipmentAcquisitionGameTests {
    private static TransientCraftingContainer grid(){
        return new TransientCraftingContainer(new AbstractContainerMenu(null,-1){
            @Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
            @Override public boolean stillValid(Player player){return true;}
        },3,3);
    }
    @GameTest(template="empty",timeoutTicks=200)
    public static void missingArmorAndBottomlessBucketsHaveDistinctRealSurvivalRecipes(GameTestHelper h){
        var manager=h.getLevel().getRecipeManager();var access=h.getLevel().registryAccess();
        for(String name:List.of("miners_chestplate","miners_leggings","miners_boots","garden_vines_helmet","garden_vines_chestplate","garden_vines_leggings","garden_vines_boots","bottomless_water_bucket","bottomless_lava_bucket")){
            var id=ResourceLocation.fromNamespaceAndPath("coraline",name);var item=ForgeRegistries.ITEMS.getValue(id);
            h.assertTrue(item!=null&&item!=Items.AIR,"Missing actual equipment registration: "+name);
            var raw=manager.byKey(id).orElse(null);h.assertTrue(raw instanceof ShapedRecipe,"Missing real acquisition recipe: "+name);
            var recipe=(ShapedRecipe)raw;var grid=grid();var counts=new HashMap<Item,Integer>();
            for(int y=0;y<recipe.getHeight();y++)for(int x=0;x<recipe.getWidth();x++){
                var ingredient=recipe.getIngredients().get(y*recipe.getWidth()+x);if(ingredient.isEmpty())continue;
                h.assertTrue(ingredient.getItems().length>0,"Unresolved ingredient: "+name);
                var stack=ingredient.getItems()[0].copyWithCount(1);grid.setItem(y*3+x,stack);counts.merge(stack.getItem(),1,Integer::sum);
            }
            h.assertTrue(recipe.matches(grid,h.getLevel()),"Valid grid rejected: "+name);
            var output=recipe.assemble(grid,access);h.assertTrue(output.is(item)&&output.getCount()==1,"Wrong acquisition output: "+name);
            h.assertTrue(manager.getRecipesFor(RecipeType.CRAFTING,grid,h.getLevel()).stream().allMatch(other->other.getResultItem(access).is(item)),"Conflicting real recipe: "+name);
            for(int slot=0;slot<9;slot++)if(!grid.getItem(slot).isEmpty()){
                var input=grid.getItem(slot);grid.setItem(slot,ItemStack.EMPTY);h.assertFalse(recipe.matches(grid,h.getLevel()),"Missing ingredient accepted: "+name+" / "+slot);grid.setItem(slot,input);
            }
            if(name.startsWith("bottomless_")){
                h.assertTrue(counts.getOrDefault(com.coraline.registry.ModItems.PERFECT_INGOT.get(),0)==4&&counts.getOrDefault(OtherMaterials.OTHER_DIAMOND.get(),0)==2&&counts.getOrDefault(OtherMaterials.MOONSTONE.get(),0)==2,"Bottomless bucket bypassed late material cost");
                h.assertTrue(counts.getOrDefault(name.contains("water")?Items.WATER_BUCKET:Items.LAVA_BUCKET,0)==1,"Wrong source fluid: "+name);
                var remainder=recipe.getRemainingItems(grid);h.assertTrue(remainder.stream().filter(stack->!stack.isEmpty()).count()==1&&remainder.stream().filter(stack->!stack.isEmpty()).allMatch(stack->stack.is(Items.BUCKET)&&stack.getCount()==1),"Bucket recipe returned materials or lost normal empty bucket remainder");
            }else{
                h.assertTrue(counts.getOrDefault(OtherMaterials.MOONSTONE.get(),0)==1,"Moonstone is not a minor armor ingredient: "+name);
                if(name.startsWith("garden_"))h.assertTrue(counts.containsKey(Items.VINE)&&counts.containsKey(Items.STRING),"Garden armor lost its distinct material identity");
            }
            h.assertTrue(h.getLevel().getServer().getAdvancements().getAdvancement(ResourceLocation.fromNamespaceAndPath("coraline","recipes/equipment/"+name))!=null,"Recipe book entry failed to load: "+name);
        }
        // Every actual registered armor piece now has a recipe. This does not
        // infer an ability or availability of its still-pending world content.
        var outputs=new HashSet<Item>();for(var recipe:manager.getAllRecipesFor(RecipeType.CRAFTING))outputs.add(recipe.getResultItem(access).getItem());
        for(var set:ArmorSets.ITEMS.values())for(var piece:set.values())h.assertTrue(outputs.contains(piece.get()),"Registered armor piece still lacks acquisition: "+piece.getId());
        h.succeed();
    }
    private EquipmentAcquisitionGameTests(){}
}
