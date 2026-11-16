package com.coraline.tycoon;

import java.util.*;
import com.coraline.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.RegistryObject;

public final class TycoonFoodContent {
    public static final RegistryObject<net.minecraft.world.entity.EntityType<TycoonSupperSeat>> SEAT=com.coraline.registry.ModEntities.ENTITIES.register("tycoon_supper_seat",()->net.minecraft.world.entity.EntityType.Builder.<TycoonSupperSeat>of(TycoonSupperSeat::new,net.minecraft.world.entity.MobCategory.MISC).sized(.1F,.1F).clientTrackingRange(8).updateInterval(2).build("coraline:tycoon_supper_seat"));
    public static final String[] IDS={"button_biscuits","moonberry_jam_toast","other_fathers_vegetable_soup","golden_garden_pie","little_door_macarons","other_mothers_chocolate_truffles","grand_other_world_supper","welcome_home_cake"};
    public static final String[] NAMES={"Button Biscuits","Moonberry Jam Toast","Other Father's Vegetable Soup","Golden Garden Pie","Little Door Macarons","Other Mother's Chocolate Truffles","Grand Other World Supper","Welcome Home Cake"};
    public static final int[] NUTRITION={2,3,5,6,6,7,10,0};
    public static final List<RegistryObject<Item>> ITEMS=new ArrayList<>();
    static{for(int i=0;i<8;i++){final int index=i;ITEMS.add(ModItems.ITEMS.register(IDS[i],()->new Item(index==7?new Item.Properties().stacksTo(1):new Item.Properties().food(new FoodProperties.Builder().nutrition(NUTRITION[index]).saturationMod(.6F+index*.08F).build())){
        @Override public Component getName(ItemStack stack){return Component.literal(NAMES[index]);}
        @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.Level l,List<Component> lines,TooltipFlag flags){lines.add(Component.literal(index==7?"A personal first-visit gift. Hold it to see eligible visitors.":"Stock in your Other World Fridge. One serving per hungry visitor."));}
    }));}}
    public static int index(ItemStack stack){for(int i=0;i<8;i++)if(stack.is(ITEMS.get(i).get()))return i;return -1;}
    public static boolean meal(ItemStack stack){int i=index(stack);return i>=0&&i<7;}
    public static ItemStack owned(net.minecraft.server.level.ServerPlayer p,int i,int count){var stack=new ItemStack(ITEMS.get(i).get(),count);TycoonWorld.tagOwned(p,stack);return stack;}
    public static void init(){}
    private TycoonFoodContent(){}
}
