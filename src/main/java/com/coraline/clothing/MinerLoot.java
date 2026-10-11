package com.coraline.clothing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.*;
import net.minecraftforge.registries.*;

/** Post-process existing block loot, preserving tool requirements, Silk Touch and Fortune. */
public final class MinerLoot extends LootModifier {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS=DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,"coraline");
    public static final RegistryObject<Codec<MinerLoot>> CODEC=SERIALIZERS.register("miner_bonus",()->RecordCodecBuilder.create(instance->codecStart(instance).apply(instance,MinerLoot::new)));
    public MinerLoot(LootItemCondition[] conditions){super(conditions);}
    @Override public Codec<? extends IGlobalLootModifier> codec(){return CODEC.get();}
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot,LootContext context){
        if(!context.hasParam(LootContextParams.BLOCK_STATE)||context.hasParam(LootContextParams.BLOCK_ENTITY))return loot;
        if(context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof Player p&&ArmorSets.full(p,ArmorSets.Set.MINERS)){
            var result=new ObjectArrayList<ItemStack>();
            for(var stack:loot){
                if(stack.hasTag()||stack.isDamageableItem()||stack.getMaxStackSize()==1){result.add(stack);continue;}
                int amount=stack.getCount()*ClothingConfig.MINER_MULTIPLIER.get();
                while(amount>0){int size=Math.min(amount,stack.getMaxStackSize());result.add(stack.copyWithCount(size));amount-=size;}
            }
            return result;
        }
        return loot;
    }
}
