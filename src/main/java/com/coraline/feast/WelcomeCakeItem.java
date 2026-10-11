package com.coraline.feast;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import java.util.List;
public final class WelcomeCakeItem extends BlockItem {
    public WelcomeCakeItem(){super(com.coraline.registry.ModBlocks.WELCOME_CAKE.get(),new Item.Properties().stacksTo(1));}
    @Override public InteractionResult place(BlockPlaceContext context){
        if(!context.getLevel().dimension().equals(Level.OVERWORLD)){if(context.getPlayer()!=null)context.getPlayer().displayClientMessage(Component.literal("Save your welcome cake for the Overworld."),true);return InteractionResult.FAIL;}
        return super.place(context);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){
        int bites=stack.hasTag()?stack.getTag().getInt("CakeBites"):0;
        lines.add(Component.literal((7-Math.max(0,Math.min(6,bites)))+" slices remaining").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
    }
}
