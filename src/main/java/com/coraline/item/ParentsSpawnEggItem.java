package com.coraline.item;

import com.coraline.parents.ParentEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.network.chat.Component;

/** A paired summon item: never configures a spawner with a single, orphaned parent. */
public final class ParentsSpawnEggItem extends Item {
    public ParentsSpawnEggItem() { super(new Properties()); }

    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        var player = context.getPlayer();
        var pos = context.getClickedPos().relative(context.getClickedFace());
        if (player != null && !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand()))
            return InteractionResult.FAIL;
        if (!ParentEvents.spawnPair((ServerLevel) context.getLevel(), pos)) {
            if (player != null) player.displayClientMessage(Component.translatable("message.coraline.parents_no_room"), true);
            return InteractionResult.FAIL;
        }
        if (player == null || !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
