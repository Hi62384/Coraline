package com.coraline.item;
import com.coraline.feast.PartyCelebration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="coraline")
public final class WrappedPartyGiftItem extends Item {
    public WrappedPartyGiftItem(){super(new Properties().stacksTo(1));}
    @Override public int getUseDuration(ItemStack stack){return 24;}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.BOW;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){var item=player.getItemInHand(hand);if(player instanceof ServerPlayer p&&!PartyCelebration.valid(p,item))return InteractionResultHolder.fail(item);if(player instanceof ServerPlayer p)PartyCelebration.unwrap(p,item);return InteractionResultHolder.sidedSuccess(item,level.isClientSide);}
    @Override public ItemStack finishUsingItem(ItemStack item,Level level,LivingEntity who){if(who instanceof ServerPlayer p)PartyCelebration.unwrap(p,item);return item;}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void clone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event){if(event.getOriginal().getPersistentData().contains(PartyCelebration.BOOK))event.getEntity().getPersistentData().put(PartyCelebration.BOOK,event.getOriginal().getPersistentData().getCompound(PartyCelebration.BOOK).copy());}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void deliver(net.minecraftforge.event.TickEvent.PlayerTickEvent event){if(event.phase==net.minecraftforge.event.TickEvent.Phase.END&&event.player instanceof ServerPlayer p&&p.tickCount%40==0&&p.getPersistentData().contains(PartyCelebration.BOOK))PartyCelebration.deliver(p);}
}
