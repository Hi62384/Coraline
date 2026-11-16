package com.coraline.tycoon;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Enforce the filter for normal clicks AND quick-move on the server. */
public final class TycoonFridgeMenu extends ChestMenu {
    public TycoonFridgeMenu(int id,Inventory inventory,TycoonBlockEntity fridge){super(MenuType.GENERIC_9x1,id,inventory,fridge,1);}
    @Override protected Slot addSlot(Slot slot){if(slot.container instanceof TycoonBlockEntity be&&be.kind().equals("other_world_fridge"))return super.addSlot(new Slot(slot.container,slot.getContainerSlot(),slot.x,slot.y){@Override public boolean mayPlace(ItemStack item){return be.canPlaceItem(getContainerSlot(),item);}});return super.addSlot(slot);}
}
