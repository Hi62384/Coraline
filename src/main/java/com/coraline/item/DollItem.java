package com.coraline.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class DollItem extends BlockItem {
    public static final String OWNER_ID = "Owner";
    public static final String OWNER_NAME = "OwnerName";

    public DollItem(Block block, Properties properties) {
        super(block, properties);
    }
    @Override public boolean hasCustomEntity(ItemStack stack){return com.coraline.doll.DollBondData.owner(stack)!=null;}
    @Override public net.minecraft.world.entity.Entity createEntity(Level level,net.minecraft.world.entity.Entity original,ItemStack stack){
        if(original instanceof com.coraline.doll.BoundDollEntity)return null;
        var doll=new com.coraline.doll.BoundDollEntity(com.coraline.registry.ModEntities.BOUND_DOLL.get(),level);
        doll.setPos(original.position());doll.setItem(stack);doll.setDeltaMovement(original.getDeltaMovement());doll.setDefaultPickUpDelay();return doll;
    }
    @Override public void inventoryTick(ItemStack stack,Level level,net.minecraft.world.entity.Entity entity,int slot,boolean selected){
        super.inventoryTick(stack,level,entity,slot,selected);
        if(level instanceof net.minecraft.server.level.ServerLevel sl){
            var data=com.coraline.doll.DollBondData.get(sl.getServer());
            if(com.coraline.doll.DollBondData.owner(stack)==null)return;
            if(!data.current(stack)){stack.setCount(0);return;}
            if(entity.getUUID().equals(com.coraline.doll.DollBondData.owner(stack)))data.satisfied(stack,sl.getServer().overworld().getGameTime(),true);
            else com.coraline.doll.DollStorage.cleanStack(stack,sl);
        }
    }
    @Override protected boolean canPlace(net.minecraft.world.item.context.BlockPlaceContext context,net.minecraft.world.level.block.state.BlockState state){
        if(context.getLevel() instanceof net.minecraft.server.level.ServerLevel sl&&com.coraline.doll.DollBondData.owner(context.getItemInHand())!=null
                &&!com.coraline.doll.DollBondData.get(sl.getServer()).current(context.getItemInHand())){context.getItemInHand().setCount(0);return false;}
        return super.canPlace(context,state);
    }

    public static ItemStack createFor(ServerPlayer player) {
        ItemStack stack = new ItemStack(com.coraline.registry.ModItems.DOLL.get());
        CompoundTag owner = new CompoundTag();
        owner.putUUID(OWNER_ID, player.getUUID());
        owner.putString(OWNER_NAME, player.getGameProfile().getName());
        owner.put("Profile", net.minecraft.nbt.NbtUtils.writeGameProfile(new CompoundTag(), player.getGameProfile()));
        owner.putInt("BondGeneration",0);
        stack.addTagElement("BlockEntityTag", owner);
        return stack;
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new com.coraline.client.renderer.DollItemRenderer();
                return renderer;
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("A little me? I should put it down somewhere near my bed").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("this item will only exist as long as your parents are alive").withStyle(ChatFormatting.YELLOW));
        CompoundTag owner = stack.getTagElement("BlockEntityTag");
        if (owner != null && owner.contains(OWNER_NAME)) {
            tooltip.add(Component.translatable("tooltip.coraline.doll.owner", owner.getString(OWNER_NAME))
                    .withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
