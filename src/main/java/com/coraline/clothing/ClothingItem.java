package com.coraline.clothing;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import java.util.List;

public final class ClothingItem extends ArmorItem {
    public enum Style {
        MIDNIGHT_COAT(Type.CHESTPLATE,3),STAR_SHIRT(Type.CHESTPLATE,2),RAINCOAT(Type.CHESTPLATE,2),
        COMFY_PAJAMAS(Type.CHESTPLATE,2),GARDEN_OVERALLS(Type.CHESTPLATE,2),CIRCUS_JACKET(Type.CHESTPLATE,3),
        SILENT_SLIPPERS(Type.BOOTS,1),FEATHER_BOOTS(Type.BOOTS,1),TOP_HAT(Type.HELMET,2),MIDNIGHT_GOGGLES(Type.HELMET,1),UNBINDING_BUTTON_EYES(Type.HELMET,2);
        public final Type slot;public final int armor;
        Style(Type slot,int armor){this.slot=slot;this.armor=armor;}
        public String id(){return name().toLowerCase(java.util.Locale.ROOT);}
    }
    public final Style style;
    public ClothingItem(Style style){super(new Fabric(style),style.slot,new Item.Properties());this.style=style;}
    @Override public String getArmorTexture(ItemStack stack,Entity entity,EquipmentSlot slot,String type){return "coraline:textures/models/sculpted/fabric.png";}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags){
        if(style!=Style.TOP_HAT)lines.add(Component.translatable("tooltip.coraline."+style.id()).withStyle(ChatFormatting.GREEN));
    }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){
            @Override public net.minecraft.client.model.Model getGenericArmorModel(LivingEntity entity,ItemStack stack,EquipmentSlot slot,net.minecraft.client.model.HumanoidModel<?> original){
                return com.coraline.client.clothing.SculptedArmorModel.forItem(style.id(),original,entity);
            }
        });
    }
    private record Fabric(Style style) implements ArmorMaterial {
        @Override public int getDurabilityForType(Type type){return switch(type){case HELMET->165;case BOOTS->195;default->240;};}
        @Override public int getDefenseForType(Type type){return type==style.slot?style.armor:0;}
        @Override public int getEnchantmentValue(){return 15;}
        @Override public SoundEvent getEquipSound(){return SoundEvents.ARMOR_EQUIP_LEATHER;}
        @Override public Ingredient getRepairIngredient(){return Ingredient.of(ItemTags.WOOL);}
        @Override public String getName(){return "coraline:"+style.id();}
        @Override public float getToughness(){return 0;}
        @Override public float getKnockbackResistance(){return 0;}
    }
}
