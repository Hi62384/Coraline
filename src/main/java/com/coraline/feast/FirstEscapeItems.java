package com.coraline.feast;

import com.coraline.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;

/** Bound story tools retain custody. Ordinary crafted shoes have no story authority. */
public final class FirstEscapeItems {
    public static final RegistryObject<Item> CANE=ModItems.ITEMS.register("story_cane",()->new Tool(false));
    public static final RegistryObject<Item> SHOE=ModItems.ITEMS.register("story_shoe",()->new Tool(true));
    public static final class Tool extends Item {
        private final boolean shoe;Tool(boolean shoe){super(new Properties().stacksTo(1));this.shoe=shoe;}
        @Override public net.minecraft.network.chat.Component getName(ItemStack stack){return net.minecraft.network.chat.Component.literal(shoe?"Wybie's shoe":"Worn porch cane");}
        @Override public boolean onDroppedByPlayer(ItemStack stack,Player player){return shoe&&!CraftedShoeThrow.bound(stack);}
        @Override public boolean canFitInsideContainerItems(){return false;}
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player player,InteractionHand hand){
            var stack=player.getItemInHand(hand);if(!shoe)return super.use(l,player,hand);
            if(player.getCooldowns().isOnCooldown(this))return InteractionResultHolder.fail(stack);
            if(player instanceof ServerPlayer p) {
                if(CraftedShoeThrow.bound(stack)) {
                    int count=stack.getCount();FirstEscapeStory.throwShoe(p,stack);
                    if(stack.getCount()==count)return InteractionResultHolder.fail(stack);
                    p.swing(hand,true);
                } else if(!CraftedShoeThrow.throwOrdinary(p,hand,stack))return InteractionResultHolder.fail(stack);
            }
            return InteractionResultHolder.sidedSuccess(stack,l.isClientSide);
        }
    }
    public static void init(){}
    private FirstEscapeItems(){}
}
