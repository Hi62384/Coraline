package com.coraline.item;

import com.coraline.registry.ModItems;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import java.util.function.Predicate;

/** The contest bow cannot silently switch to arrows brought from home. */
public final class ArcheryBowItem extends BowItem {
    private static final Predicate<ItemStack> ISSUED=stack->stack.is(ModItems.ARCHERY_ARROW.get())&&
        stack.hasTag()&&stack.getTag().getBoolean("CoralineArcheryIssued");
    public ArcheryBowItem(){super(new Properties().durability(384));}
    @Override public Predicate<ItemStack> getAllSupportedProjectiles(){return ISSUED;}
    @Override public Predicate<ItemStack> getSupportedHeldProjectiles(){return ISSUED;}
}
