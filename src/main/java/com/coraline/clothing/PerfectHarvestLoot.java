package com.coraline.clothing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.RegistryObject;

/** Doubles only fully-grown crop drops when the perfect hoe is used to harvest. */
public final class PerfectHarvestLoot extends LootModifier {
    public static final RegistryObject<Codec<PerfectHarvestLoot>> CODEC=MinerLoot.SERIALIZERS.register("perfect_harvest",()->RecordCodecBuilder.create(instance->codecStart(instance).apply(instance,PerfectHarvestLoot::new)));
    public static void init(){}
    public PerfectHarvestLoot(LootItemCondition[] conditions){super(conditions);}
    @Override public Codec<? extends IGlobalLootModifier> codec(){return CODEC.get();}
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot,LootContext context){
        BlockState state=context.getParamOrNull(LootContextParams.BLOCK_STATE);
        if(state==null||!(state.getBlock() instanceof CropBlock crop)||!crop.isMaxAge(state))return loot;
        if(!(context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof Player player)||!player.getMainHandItem().is(OtherMaterials.TOOLS.get("perfect_hoe").get()))return loot;
        var result=new ObjectArrayList<ItemStack>();
        for(var stack:loot){
            result.add(stack);
            if(!stack.isEmpty()&&!stack.isDamageableItem())result.add(stack.copy());
        }
        return result;
    }
}
