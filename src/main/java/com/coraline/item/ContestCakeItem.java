package com.coraline.item;

import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class ContestCakeItem extends Item {
    public ContestCakeItem(){super(new Properties().stacksTo(1));}

    @Override public void initializeClient(Consumer<IClientItemExtensions> out){
        out.accept(new IClientItemExtensions(){
            private BlockEntityWithoutLevelRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){
                if(renderer==null)renderer=new com.coraline.client.renderer.ContestCakeItemRenderer();
                return renderer;
            }
        });
    }
}
