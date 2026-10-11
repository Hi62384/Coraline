package com.coraline.item;
import net.minecraft.world.item.*;
/** Temporary equipment is identified by type, not a player-editable name. */
public final class ExpeditionItem extends Item {
    public ExpeditionItem(){super(new Properties().stacksTo(1));}
    @Override public boolean isFoil(ItemStack s){return true;}
    @Override public void inventoryTick(ItemStack stack,net.minecraft.world.level.Level level,net.minecraft.world.entity.Entity entity,int slot,boolean selected){if(!level.isClientSide&&(!com.coraline.feast.ActivityWorld.isActivity(level)||!stack.hasTag()||!stack.getTag().hasUUID("ExpeditionOwner")||!stack.getTag().getUUID("ExpeditionOwner").equals(entity.getUUID())))stack.setCount(0);}
    @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level l,net.minecraft.world.entity.player.Player p,net.minecraft.world.InteractionHand h){if(!l.isClientSide&&p instanceof net.minecraft.server.level.ServerPlayer sp){if(p.getItemInHand(h).is(com.coraline.registry.ModItems.ACTIVITY_MINE_MAP.get()))com.coraline.feast.MineshaftActivity.openMap(sp);else if(p.getItemInHand(h).is(com.coraline.registry.ModItems.MANSION_KEY.get()))com.coraline.feast.MansionActivity.openJournal(sp);else if(p.getItemInHand(h).is(com.coraline.registry.ModItems.TREASURE_NOTE.get()))com.coraline.feast.TreasureHuntActivity.openNote(sp);else if(p.getItemInHand(h).is(com.coraline.registry.ModItems.SNOWBALL_POUCH.get()))com.coraline.feast.SnowballBattleActivity.throwBall(sp);}return net.minecraft.world.InteractionResultHolder.sidedSuccess(p.getItemInHand(h),l.isClientSide);}
}
