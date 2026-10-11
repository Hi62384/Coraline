package com.coraline.event;

import com.coraline.CoralineMod;
import com.coraline.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CoralineMod.MOD_ID)
public final class LeafDropEvents {
    // One rod per 200 leaf blocks on average.
    private static final float DOWSING_ROD_DROP_CHANCE = 0.005F;

    private LeafDropEvents() {
    }

    @SubscribeEvent
    public static void onLeafBroken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || event.getPlayer().isCreative()
                || !event.getState().is(BlockTags.LEAVES)
                || level.random.nextFloat() >= com.coraline.config.CoralineConfig.ROD_DROP.get()) {
            return;
        }

        Block.popResource(level, event.getPos(), new ItemStack(ModItems.DOWSING_ROD.get()));
    }
}
