package com.coraline.mixin;

import com.coraline.clothing.SetArmorItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserve vanilla reduction math, but don't truncate the requested high-tier armor totals at 30. */
@Mixin(LivingEntity.class)
public abstract class ClothingArmorValueMixin {
    @Inject(method="getArmorValue",at=@At("RETURN"),cancellable=true)
    private void coraline$armor(CallbackInfoReturnable<Integer> ci){
        var self=(LivingEntity)(Object)this;
        if(!(self instanceof Player))return;
        boolean custom=false;for(var stack:self.getArmorSlots())if(stack.getItem() instanceof SetArmorItem){custom=true;break;}
        if(!custom)return;
        var attribute=self.getAttribute(Attributes.ARMOR);if(attribute==null)return;
        double base=attribute.getBaseValue();
        for(var m:attribute.getModifiers(AttributeModifier.Operation.ADDITION))base+=m.getAmount();
        double value=base;
        for(var m:attribute.getModifiers(AttributeModifier.Operation.MULTIPLY_BASE))value+=base*m.getAmount();
        for(var m:attribute.getModifiers(AttributeModifier.Operation.MULTIPLY_TOTAL))value*=1+m.getAmount();
        if(value>30)ci.setReturnValue((int)Math.min(1024,value));
    }
}
