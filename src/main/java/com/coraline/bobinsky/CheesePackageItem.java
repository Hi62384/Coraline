package com.coraline.bobinsky;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
public final class CheesePackageItem extends Item {
    public CheesePackageItem(){super(new Properties().stacksTo(1));}
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected){
        if(!level.isClientSide&&entity instanceof net.minecraft.server.level.ServerPlayer p&&stack.hasTag()&&stack.getTag().hasUUID("BobinskyToken"))BobinskyVisit.pickedUp(p,stack);
    }
}
