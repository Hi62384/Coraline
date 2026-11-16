package com.coraline.rebirth;

import com.coraline.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.RegistryObject;

public final class RebirthItems {
    public static final RegistryObject<Item> VOID_HEART=com.coraline.clothing.ArtifactItems.VOID_HEART;
    public static final RegistryObject<Item> CUTTER=ModItems.ITEMS.register("last_thread_cutter",()->new Item(new Item.Properties().stacksTo(1)){
        @Override public Component getName(ItemStack s){return Component.literal("Last Thread Cutter");}
        @Override public boolean onDroppedByPlayer(ItemStack s,net.minecraft.world.entity.player.Player p){return false;}
        @Override public boolean canFitInsideContainerItems(){return false;}
    });
    public static void init(){}
    private RebirthItems(){}
}
