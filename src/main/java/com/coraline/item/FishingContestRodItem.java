package com.coraline.item;

import com.coraline.feast.FishingContestActivity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Father's issued rod; only the matching contest session may cast it. */
public final class FishingContestRodItem extends FishingRodItem {
    public FishingContestRodItem(){super(new Properties().durability(256));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(!level.isClientSide&&player instanceof ServerPlayer p&&!FishingContestActivity.canFish(p,player.getItemInHand(hand))){
            p.displayClientMessage(Component.literal("Wait for Father to finish, then fish the contest lake with his rod."),true);
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        return super.use(level,player,hand);
    }
}
