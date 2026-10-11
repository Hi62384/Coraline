package com.coraline.clothing;

import com.coraline.registry.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.sounds.*;
import java.util.*;
import net.minecraftforge.registries.RegistryObject;

public final class ArmorSets {
    public enum Set implements ArmorMaterial {
        DREAMWEAVE(2,3,3,1), COMFY_SILK(1,2,2,1), MIDNIGHT(2,4,4,1),
        BUTTONGUARD(2,5,5,2), MIDNIGHT_CLOAK(1,2,1,1), LUCK(2,4,4,1),
        GARDEN_VINES(2,5,5,2), MOONSTONE(3,8,6,3), MINERS(2,3,3,1),
        SPIDERVISION(3,8,6,3), BELDAM_WEAVE(5,11,9,5), SOULBOUND(4,9,7,4),
        SUPER_SPIDERVISION(3,8,6,3), POTENT_BELDAM_WEAVE(6,13,11,6),
        SOULBOUND_POTENT_BELDAM_WEAVE(7,16,13,7), OTHER_WORLD_VOID(8,18,16,8),
        OTHER_DIAMOND(4,9,8,4), OTHER_IRON(3,7,6,3), PERFECT(5,12,10,5);
        private final int[] defense;
        Set(int head,int chest,int legs,int feet){defense=new int[]{feet,legs,chest,head};}
        public String id(){return name().toLowerCase(Locale.ROOT);}
        public int getDefenseForType(ArmorItem.Type type){return defense[type.getSlot().getIndex()];}
        public int getDurabilityForType(ArmorItem.Type type){return (this==PERFECT||this==OTHER_DIAMOND?66:this==MOONSTONE||this==OTHER_IRON?30:18)*switch(type){case HELMET->11;case CHESTPLATE->16;case LEGGINGS->15;case BOOTS->13;};}
        public int getEnchantmentValue(){return 15;}
        public SoundEvent getEquipSound(){return this==MOONSTONE?SoundEvents.ARMOR_EQUIP_DIAMOND:SoundEvents.ARMOR_EQUIP_LEATHER;}
        public Ingredient getRepairIngredient(){return Ingredient.of(switch(this){case MOONSTONE->OtherMaterials.MOONSTONE.get();case OTHER_DIAMOND->OtherMaterials.OTHER_DIAMOND.get();case PERFECT->ModItems.PERFECT_INGOT.get();case OTHER_IRON->OtherMaterials.OTHER_IRON.get();default->Items.LEATHER;});}
        public String getName(){return "coraline:"+id();}
        public float getToughness(){return switch(this){case MOONSTONE,SPIDERVISION,SUPER_SPIDERVISION->2;case OTHER_DIAMOND->2.5F;case PERFECT->3;default->0;};}
        public float getKnockbackResistance(){return this==PERFECT?.5F:0;}
    }
    public static final Map<Set,Map<ArmorItem.Type,RegistryObject<Item>>> ITEMS=new EnumMap<>(Set.class);
    static {
        for(Set set:Set.values()){
            var pieces=new EnumMap<ArmorItem.Type,RegistryObject<Item>>(ArmorItem.Type.class);
            for(var type:ArmorItem.Type.values())pieces.put(type,ModItems.ITEMS.register(set.id()+"_"+type.getName(),()->new SetArmorItem(set,type)));
            ITEMS.put(set,pieces);
        }
    }
    public static void init(){}
    public static int count(LivingEntity entity,Set set){
        int count=0;
        for(var type:ArmorItem.Type.values())if(entity.getItemBySlot(type.getSlot()).getItem() instanceof SetArmorItem item&&item.set==set&&item.getType()==type)count++;
        return count;
    }
    public static boolean full(LivingEntity entity,Set set){return count(entity,set)==4;}
}
