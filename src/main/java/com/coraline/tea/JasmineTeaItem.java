package com.coraline.tea;
import net.minecraft.world.item.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
public final class JasmineTeaItem extends Item {
    public JasmineTeaItem(){super(new Properties().stacksTo(1));}
    @Override public int getUseDuration(ItemStack stack){return 32;}@Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.DRINK;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand){if(level.isClientSide&&(!(p.getVehicle() instanceof TeaSeatEntity seat)||!seat.canDrink())||!level.isClientSide&&p instanceof net.minecraft.server.level.ServerPlayer sp&&!TeaVisit.canDrink(sp,p.getItemInHand(hand)))return InteractionResultHolder.fail(p.getItemInHand(hand));p.startUsingItem(hand);return InteractionResultHolder.consume(p.getItemInHand(hand));}
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity e){if(!level.isClientSide&&e instanceof net.minecraft.server.level.ServerPlayer p){if(!TeaVisit.canDrink(p,stack))return stack;TeaVisit.drunk(p);var cup=new ItemStack(com.coraline.registry.ModItems.EMPTY_TEACUP.get());if(stack.hasTag())cup.setTag(stack.getTag().copy());return cup;}return super.finishUsingItem(stack,level,e);}
}
