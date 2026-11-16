package com.coraline.rebirth;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

/** Reuses the established artifact registry ID and stack limit; all prior Hearts remain valid. */
public final class VoidHeartItem extends Item {
    public VoidHeartItem(){super(new Properties().stacksTo(16).fireResistant());}
    @Override public Component getName(ItemStack s){return Component.literal("Void Heart");}
    @Override public Rarity getRarity(ItemStack s){return Rarity.EPIC;}
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> out){
        out.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){
                if(renderer==null)renderer=new com.coraline.client.renderer.VoidHeartRenderer();return renderer;
            }
        });
    }
}
