package com.coraline.clothing;

import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.List;

public final class SetArmorItem extends ArmorItem {
    public final ArmorSets.Set set;
    public SetArmorItem(ArmorSets.Set set,Type type){super(set,type,new Item.Properties());this.set=set;}
    @Override public String getArmorTexture(ItemStack stack,Entity entity,EquipmentSlot slot,String layer){
        String texture=switch(set){case DREAMWEAVE,COMFY_SILK,BUTTONGUARD,GARDEN_VINES,MIDNIGHT,
            LUCK,MOONSTONE,MINERS,SPIDERVISION,BELDAM_WEAVE,SOULBOUND,
            SUPER_SPIDERVISION,POTENT_BELDAM_WEAVE,SOULBOUND_POTENT_BELDAM_WEAVE,
            OTHER_WORLD_VOID,OTHER_DIAMOND,OTHER_IRON,PERFECT->set.id();
            default->"fabric";};
        return "coraline:textures/models/sculpted/"+texture+".png";
    }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){
            @Override public net.minecraft.client.model.Model getGenericArmorModel(LivingEntity entity,ItemStack stack,EquipmentSlot slot,net.minecraft.client.model.HumanoidModel<?> original){
                String piece=switch(slot){case HEAD->"helmet";case CHEST->"chestplate";case LEGS->"leggings";default->"boots";};
                return com.coraline.client.clothing.SculptedArmorModel.forItem(set.id()+"_"+piece,original,entity);
            }
        });
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags){
        switch(set){
            case COMFY_SILK,BELDAM_WEAVE,POTENT_BELDAM_WEAVE,OTHER_WORLD_VOID,OTHER_DIAMOND,OTHER_IRON,PERFECT -> {return;}
            default -> lines.add(Component.translatable("tooltip.coraline.set."+set.id()).withStyle(ChatFormatting.GREEN));
        }
    }
}
