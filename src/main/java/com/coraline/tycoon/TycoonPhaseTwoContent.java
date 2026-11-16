package com.coraline.tycoon;

import com.coraline.registry.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.RegistryObject;

public final class TycoonPhaseTwoContent {
    public static final RegistryObject<Item> DOLL=ModItems.ITEMS.register("personalized_tycoon_doll",()->new Item(new Item.Properties().stacksTo(1)){
        @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new com.coraline.client.renderer.TycoonDollItemRenderer();return renderer;}
        });}
        @Override public void appendHoverText(ItemStack item,net.minecraft.world.level.Level level,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flags){if(item.hasTag()){lines.add(net.minecraft.network.chat.Component.literal("Handmade for "+item.getTag().getString("ResidentName")));lines.add(net.minecraft.network.chat.Component.literal("Right-click the matching resident to deliver."));}}
    });
    public static final RegistryObject<EntityType<TycoonDollEntity>> DOLL_ACTOR=ModEntities.ENTITIES.register("tycoon_bedside_doll",()->EntityType.Builder.<TycoonDollEntity>of(TycoonDollEntity::new,MobCategory.MISC).sized(.3F,.5F).clientTrackingRange(12).updateInterval(10).build("coraline:tycoon_bedside_doll"));
    public static void init(){}
    private TycoonPhaseTwoContent(){}
}
