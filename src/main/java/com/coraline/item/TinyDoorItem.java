package com.coraline.item;

public final class TinyDoorItem extends net.minecraft.world.item.BlockItem {
    public TinyDoorItem(){super(com.coraline.registry.ModBlocks.TINY_DOOR.get(),new Properties().stacksTo(1));}
    @Override public net.minecraft.world.InteractionResult place(net.minecraft.world.item.context.BlockPlaceContext context){
        if(context.getLevel() instanceof net.minecraft.server.level.ServerLevel level&&com.coraline.otherworld.DoorData.get(level.getServer()).revoked(context.getItemInHand())){
            context.getItemInHand().setCount(0);return net.minecraft.world.InteractionResult.FAIL;
        }
        return super.place(context);
    }
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,net.minecraft.world.level.Level level,java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag flag){
        lines.add(net.minecraft.network.chat.Component.literal("Place on a wall to open the way.").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
    @Override public void inventoryTick(net.minecraft.world.item.ItemStack stack,net.minecraft.world.level.Level level,net.minecraft.world.entity.Entity entity,int slot,boolean selected){
        if(level instanceof net.minecraft.server.level.ServerLevel sl&&com.coraline.otherworld.DoorData.get(sl.getServer()).revoked(stack)){stack.setCount(0);return;}
        if(level instanceof net.minecraft.server.level.ServerLevel sl&&entity instanceof net.minecraft.server.level.ServerPlayer player)com.coraline.otherworld.DoorData.get(sl.getServer()).track(stack,sl,"inventory",player.blockPosition(),player.getUUID());
    }
    @Override public boolean hasCustomEntity(net.minecraft.world.item.ItemStack stack){return com.coraline.otherworld.DoorData.id(stack)!=null;}
    @Override public net.minecraft.world.entity.Entity createEntity(net.minecraft.world.level.Level level,net.minecraft.world.entity.Entity source,net.minecraft.world.item.ItemStack stack){
        if(source instanceof com.coraline.otherworld.DoorItemEntity)return null;
        var entity=new com.coraline.otherworld.DoorItemEntity(com.coraline.registry.ModEntities.TRACKED_DOOR.get(),level);entity.setPos(source.position());entity.setItem(stack);entity.setDeltaMovement(source.getDeltaMovement());entity.setDefaultPickUpDelay();return entity;
    }
}
