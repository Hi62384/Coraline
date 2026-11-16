package com.coraline.feast;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;

/** Modest, event-limited edible treats, with no power reward or unlimited item dispenser. */
@Mod.EventBusSubscriber(modid="coraline")
public final class CircusSouvenirs {
    public static final RegistryObject<Item> COTTON_CANDY=com.coraline.food.TreatFoods.ITEMS.get("cotton_candy");
    public static final RegistryObject<Item> POPCORN=com.coraline.food.TreatFoods.ITEMS.get("popcorn");
    /** Keep old registry IDs loadable, but replace obsolete treats already in player saves. */
    @SubscribeEvent public static void migrate(net.minecraftforge.event.TickEvent.PlayerTickEvent event){if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END||!(event.player instanceof net.minecraft.server.level.ServerPlayer p)||p.tickCount%20!=0)return;for(int slot=0;slot<p.getInventory().getContainerSize();slot++){var old=p.getInventory().getItem(slot);Item replacement=old.is(com.coraline.registry.ModItems.CIRCUS_COTTON_CANDY.get())?COTTON_CANDY.get():old.is(com.coraline.registry.ModItems.CIRCUS_POPCORN.get())?POPCORN.get():null;if(replacement==null)continue;var fresh=new net.minecraft.world.item.ItemStack(replacement);if(old.hasTag())fresh.setTag(old.getTag().copy());if(old.getCount()==1)p.getInventory().setItem(slot,fresh);else{old.shrink(1);com.coraline.secrets.SecretTransactions.give(p,fresh);}p.getInventory().setChanged();p.containerMenu.broadcastChanges();}}
    private CircusSouvenirs(){}
}
