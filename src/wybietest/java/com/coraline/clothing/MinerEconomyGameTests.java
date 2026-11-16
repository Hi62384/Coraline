package com.coraline.clothing;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

/** Real block loot modifier calls with full armor and Silk Touch; no test-side
 * counterfeit bonus or manually edited loot result. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class MinerEconomyGameTests {
    private static net.minecraft.server.level.ServerPlayer miner(GameTestHelper h){
        var player=FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"MinerEconomyQA"));
        for(var type:ArmorItem.Type.values())player.setItemSlot(type.getSlot(),new ItemStack(ArmorSets.ITEMS.get(ArmorSets.Set.MINERS).get(type).get()));
        return player;
    }
    @GameTest(template="empty")
    public static void minerCannotMultiplySupernaturalStorageOrSilkTouchedOres(GameTestHelper h){
        var player=miner(h);var tool=new ItemStack(OtherMaterials.TOOLS.get("perfect_pickaxe").get());tool.enchant(Enchantments.SILK_TOUCH,1);
        for(String material:List.of("other_iron","other_diamond","moonstone","perfect","soulglass"))for(String suffix:List.of("_ore","_block")){
            var id=ResourceLocation.fromNamespaceAndPath("coraline",material+suffix);
            var block=ForgeRegistries.BLOCKS.getValue(id);h.assertTrue(block!=null&&block!=Blocks.AIR,"Missing actual economy block: "+id);
            var loot=Block.getDrops(block.defaultBlockState(),h.getLevel(),h.absolutePos(new net.minecraft.core.BlockPos(1,1,1)),null,player,tool);
            h.assertTrue(loot.size()==1&&loot.get(0).is(block.asItem())&&loot.get(0).getCount()==1,"Placed resource/Silk Touch loop multiplied currency or lost original drop: "+id+" / "+loot);
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void ordinaryPlacedBlockBonusAndNormalUnarmoredLootRemain(GameTestHelper h){
        var player=miner(h);var tool=new ItemStack(OtherMaterials.TOOLS.get("perfect_pickaxe").get());tool.enchant(Enchantments.SILK_TOUCH,1);
        for(var block:List.of(Blocks.STONE,Blocks.OAK_PLANKS)){
            var loot=Block.getDrops(block.defaultBlockState(),h.getLevel(),h.absolutePos(new net.minecraft.core.BlockPos(1,1,1)),null,player,tool);
            h.assertTrue(loot.stream().allMatch(stack->stack.is(block.asItem()))&&loot.stream().mapToInt(ItemStack::getCount).sum()==ClothingConfig.MINER_MULTIPLIER.get(),"Ordinary placed-block bonus was removed: "+block);
        }
        for(var type:ArmorItem.Type.values())player.setItemSlot(type.getSlot(),ItemStack.EMPTY);
        var loot=Block.getDrops(Blocks.STONE.defaultBlockState(),h.getLevel(),h.absolutePos(new net.minecraft.core.BlockPos(1,1,1)),null,player,tool);
        h.assertTrue(loot.size()==1&&loot.get(0).is(Items.STONE)&&loot.get(0).getCount()==1,"Unarmored mining began multiplying ordinary drops");h.succeed();
    }
    private MinerEconomyGameTests(){}
}
