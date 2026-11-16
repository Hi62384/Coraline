package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;

/** Actual recipes, block NBT and wrong-world/ordinary-block denial. Real
 * successful cuts, wear, client collision and exploration remain separate. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class ThreadCutterGameTests {
    private static TransientCraftingContainer grid(){return new TransientCraftingContainer(new AbstractContainerMenu(null,-1){
        @Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
        @Override public boolean stillValid(Player player){return true;}
    },3,3);}
    @GameTest(template="empty")
    public static void realCutterRecipeMatchesAndEveryMissingIngredientFails(GameTestHelper h){
        var item=com.coraline.clothing.ArtifactItems.OTHER_IRON_THREAD_CUTTER.get();var tool=new ItemStack(item);
        h.assertTrue(item instanceof OtherIronThreadCutter&&tool.getMaxDamage()==128,"Actual durable thread snips missing");
        var raw=h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("coraline","other_iron_thread_cutter")).orElse(null);
        h.assertTrue(raw instanceof ShapedRecipe,"Cutter survival recipe missing");var recipe=(ShapedRecipe)raw;var grid=grid();
        var iron=new ItemStack(com.coraline.clothing.OtherMaterials.OTHER_IRON.get());for(int slot:new int[]{0,2,6,8})grid.setItem(slot,iron.copy());grid.setItem(4,new ItemStack(Items.STRING));
        h.assertTrue(recipe.matches(grid,h.getLevel())&&recipe.assemble(grid,h.getLevel().registryAccess()).is(item)&&recipe.assemble(grid,h.getLevel().registryAccess()).getCount()==1,"Actual four-Iron/string crafting path failed");
        h.assertTrue(h.getLevel().getRecipeManager().getRecipesFor(RecipeType.CRAFTING,grid,h.getLevel()).stream().allMatch(r->r.getResultItem(h.getLevel().registryAccess()).is(item)),"Cutter grid has a conflicting loaded recipe");
        for(int slot:new int[]{0,2,4,6,8}){var stack=grid.getItem(slot);grid.setItem(slot,ItemStack.EMPTY);h.assertFalse(recipe.matches(grid,h.getLevel()),"Cutter crafted without required ingredient "+slot);grid.setItem(slot,stack);}
        h.assertTrue(item.isValidRepairItem(tool,iron)&&!item.isValidRepairItem(tool,new ItemStack(Items.IRON_INGOT)),"Cutter repair bypassed Other Iron");
        h.assertTrue(h.getLevel().getServer().getAdvancements().getAdvancement(ResourceLocation.fromNamespaceAndPath("coraline","recipes/equipment/other_iron_thread_cutter"))!=null,"Cutter book advancement did not load");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200)
    public static void originalWorldCannotCutCopiedBindingsAndUnknownMetadataSurvives(GameTestHelper h){
        var epoch=UUID.fromString("11111111-2222-3333-4444-555555555555");InfiniteStructurePlan.Plan plan=null;
        for(int x=-12288;x<=12288&&plan==null;x+=768)for(int z=-12288;z<=12288;z+=768){var p=InfiniteStructurePlan.at(epoch,124119,InfiniteStructurePlan.Family.DOLL_WORKSHOP,x,z);if(p.isPresent()){plan=p.orElseThrow();break;}}
        h.assertTrue(plan!=null,"No workshop binding fixture");int module=0;while(module<plan.modules().size()&&plan.modules().get(module).role()!=InfiniteStructurePlan.Role.WORKROOM)module++;
        h.assertTrue(module<plan.modules().size(),"Workshop workroom missing");var at=SiteBindingReceipt.cells(plan,module).get(0);var level=h.getLevel();level.getChunkAt(at);
        var block=com.coraline.registry.ModBlocks.INFINITE_THREAD_BINDING.get();level.setBlock(at,block.defaultBlockState(),2);
        h.assertTrue(level.getBlockEntity(at) instanceof SiteBindingBlockEntity,"Actual registered binding block entity missing");var binding=(SiteBindingBlockEntity)level.getBlockEntity(at);binding.configure(plan,module);
        h.assertTrue(!binding.authentic()&&!binding.cut()&&!level.getBlockState(at).getValue(SiteThreadBinding.OPEN),"A metadata snapshot cut a foreign-world binding");
        var player=FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ThreadCutterQA"));player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);player.setPos(at.getCenter().add(0,0,-2));
        var tool=new ItemStack(com.coraline.clothing.ArtifactItems.OTHER_IRON_THREAD_CUTTER.get());player.setItemSlot(EquipmentSlot.MAINHAND,tool);
        var use=new net.minecraft.world.item.context.UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(at.getCenter(),Direction.NORTH,at,false));
        h.assertTrue(tool.getItem().useOn(use)==InteractionResult.FAIL&&tool.getDamageValue()==0,"Denied cut charged durability");
        var saved=binding.saveWithFullMetadata();var receipt=saved.getCompound("CoralineThreadBinding").copy();receipt.putBoolean("Cut",true);saved.put("CoralineThreadBinding",receipt);binding.load(saved);level.setBlock(at,level.getBlockState(at).setValue(SiteThreadBinding.OPEN,true),2);
        h.assertTrue(!level.getBlockState(at).getCollisionShape(level,at,CollisionContext.empty()).isEmpty(),"Copied cut snapshot opened collision in an original dimension");
        receipt.putInt("Version",999);receipt.putString("FutureState","preserve");saved.put("CoralineThreadBinding",receipt);binding.load(saved);
        h.assertTrue(!binding.cut()&&binding.saveWithFullMetadata().getCompound("CoralineThreadBinding").equals(receipt),"Future binding metadata reset or cut");
        saved.putString("CoralineThreadBinding","future representation");binding.load(saved);binding.configure(plan,module);h.assertTrue(binding.saveWithFullMetadata().getString("CoralineThreadBinding").equals("future representation"),"Unknown binding representation erased");
        level.setBlock(at,Blocks.COBWEB.defaultBlockState(),2);h.assertTrue(tool.getItem().useOn(use)==InteractionResult.PASS&&tool.getDamageValue()==0&&level.getBlockState(at).is(Blocks.COBWEB),"Cutter became generic shears or charged an ordinary web");h.succeed();
    }
}
