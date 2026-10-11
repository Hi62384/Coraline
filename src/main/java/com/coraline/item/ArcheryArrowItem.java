package com.coraline.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Distinguishes Father's 64 shots from personal arrows at projectile creation. */
public final class ArcheryArrowItem extends ArrowItem {
    public ArcheryArrowItem(){super(new Properties().stacksTo(64));}
    @Override public AbstractArrow createArrow(Level level,ItemStack stack,LivingEntity shooter){
        var arrow=super.createArrow(level,stack,shooter);
        if(stack.hasTag()&&stack.getTag().getBoolean("CoralineArcheryIssued")){
            var data=arrow.getPersistentData();data.putBoolean("CoralineArcheryArrow",true);
            data.putBoolean("CoralineDecoration",true);
            if(stack.getTag().hasUUID("CoralineArcheryOwner"))data.putUUID("CoralineArcheryOwner",stack.getTag().getUUID("CoralineArcheryOwner"));
            data.putInt("CoralineArcheryGeneration",stack.getTag().getInt("CoralineArcheryGeneration"));
        }
        arrow.pickup=AbstractArrow.Pickup.DISALLOWED;
        return arrow;
    }
}
