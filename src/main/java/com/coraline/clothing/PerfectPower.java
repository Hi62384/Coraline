package com.coraline.clothing;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Essence is actual inventory stock, never a copied energy balance. */
@Mod.EventBusSubscriber(modid="coraline")
public final class PerfectPower {
    private static final UUID SPEED=UUID.fromString("ea43c131-8f48-418d-b720-b0dcfa660e65");
    private static final Map<ServerPlayer,Boolean> VISUALS=new WeakHashMap<>();
    private static void sync(ServerPlayer p){boolean active=powered(p);var old=VISUALS.put(p,active);if(old==null||old!=active)PerfectAbilityNetwork.power(p,active);}
    public static boolean equipment(ItemStack stack){
        if(stack.isEmpty())return false;var item=stack.getItem();
        return item instanceof TieredItem tool&&tool.getTier()==OtherMaterials.PERFECT||item instanceof SetArmorItem armor&&armor.set==ArmorSets.Set.PERFECT||item==UtilityItems.EXCAVATOR.get()||item==ArtifactItems.PERFECT_CROSSBOW.get();
    }
    public static boolean essence(ItemStack stack){return stack.is(com.coraline.registry.ModItems.PERFECT_ESSENCE.get())&&(!stack.hasTag()||stack.getTag().isEmpty());}
    public static int fuel(Player player){int count=0;for(var stack:player.getInventory().items)if(essence(stack))count+=stack.getCount();if(essence(player.getOffhandItem()))count+=player.getOffhandItem().getCount();return count;}
    public static boolean powered(Player player){return player.isAlive()&&!player.isSpectator()&&fuel(player)>0;}
    public static boolean consume(ServerPlayer player,int amount){
        if(amount<1||fuel(player)<amount)return false;
        for(int i=0;i<=40&&amount>0;i++){if(i>=36&&i<40)continue;var stack=player.getInventory().getItem(i);if(!essence(stack))continue;int take=Math.min(amount,stack.getCount());stack.shrink(take);amount-=take;}
        player.getInventory().setChanged();updateSpeed(player);sync(player);return true;
    }
    public static void removeMending(ItemStack stack){
        if(!equipment(stack)||!stack.hasTag())return;var n=stack.getTag();var list=n.getList("Enchantments",Tag.TAG_COMPOUND);
        for(int i=list.size()-1;i>=0;i--)if(list.getCompound(i).getString("id").equals("minecraft:mending"))list.remove(i);
    }
    private static void updateSpeed(ServerPlayer player){
        var attribute=player.getAttribute(Attributes.ATTACK_SPEED);if(attribute==null)return;
        var hand=player.getMainHandItem();boolean enabled=powered(player)&&equipment(hand)&&(hand.getItem() instanceof SwordItem||hand.getItem() instanceof AxeItem);
        double amount=PerfectPowerConfig.SPEED.get();var old=attribute.getModifier(SPEED);
        if(old!=null&&(!enabled||old.getAmount()!=amount))attribute.removeModifier(SPEED);
        if(enabled&&attribute.getModifier(SPEED)==null)attribute.addTransientModifier(new AttributeModifier(SPEED,"Perfect Essence attack speed",amount,AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
    private static void repair(ServerPlayer player,ItemStack stack){
        if(!equipment(stack)||!stack.isDamaged())return;int damage=stack.getDamageValue();
        int amount=damage/10;if(amount==0&&stack.getMaxDamage()-damage<=2)amount=1;amount=Math.min(amount,fuel(player));
        if(amount>0&&consume(player,amount))stack.setDamageValue(Math.max(0,damage-amount*10));
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer p))return;
        if(p.tickCount%20==0)for(int slot=0;slot<=40;slot++)removeMending(p.getInventory().getItem(slot));
        if(p.tickCount%5==0&&powered(p)){
            repair(p,p.getMainHandItem());repair(p,p.getOffhandItem());
            for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})repair(p,p.getItemBySlot(slot));
        }
        updateSpeed(p);
        sync(p);
        if(p.tickCount%10==0&&powered(p)){
            boolean held=equipment(p.getMainHandItem())||equipment(p.getOffhandItem());int armor=ArmorSets.count(p,ArmorSets.Set.PERFECT);
            if(held||armor>0){var dust=new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1,.74F,.12F),.65F);p.serverLevel().sendParticles(dust,p.getX(),p.getY()+1,p.getZ(),armor>0?4:2,.3,.5,.3,.015);}
        }
    }
    public static boolean otherWorld(Entity target){
        var id=ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        return id!=null&&id.getNamespace().equals("coraline")||target.getPersistentData().getBoolean("CoralineInfected")||target.level().dimension().location().getNamespace().equals("coraline");
    }
    @SubscribeEvent public static void speed(PlayerEvent.BreakSpeed event){
        var p=event.getEntity();var tool=p.getMainHandItem();if(powered(p)&&equipment(tool)&&tool.getItem() instanceof DiggerItem&&tool.isCorrectToolForDrops(event.getState()))event.setNewSpeed((float)(event.getNewSpeed()*PerfectPowerConfig.MINING.get()));
    }
    @SubscribeEvent public static void damage(LivingHurtEvent event){
        if(event.getSource().getEntity() instanceof ServerPlayer p&&powered(p)&&equipment(p.getMainHandItem())&&otherWorld(event.getEntity()))event.setAmount((float)(event.getAmount()*PerfectPowerConfig.HOSTILE_DAMAGE.get()));
        if(event.getEntity() instanceof ServerPlayer p&&powered(p)){
            int pieces=ArmorSets.count(p,ArmorSets.Set.PERFECT);var attacker=event.getSource().getEntity();
            if(pieces>0&&(attacker!=null&&otherWorld(attacker)||event.getSource().is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO)))event.setAmount((float)(event.getAmount()*(1-pieces*PerfectPowerConfig.ARMOR_REDUCTION.get())));
        }
    }
    @SubscribeEvent public static void anvil(net.minecraftforge.event.AnvilUpdateEvent event){
        if(!equipment(event.getLeft()))return;
        if(EnchantmentHelper.getEnchantments(event.getRight()).containsKey(Enchantments.MENDING)){event.setCanceled(true);return;}
    }
    @SubscribeEvent public static void crafted(PlayerEvent.ItemCraftedEvent event){removeMending(event.getCrafting());}
    @SubscribeEvent public static void pickup(EntityItemPickupEvent event){removeMending(event.getItem().getItem());}
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking event){if(event.getEntity() instanceof ServerPlayer viewer&&event.getTarget() instanceof ServerPlayer subject)PerfectAbilityNetwork.tracking(viewer,subject);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p)VISUALS.remove(p);}
    @SubscribeEvent public static void itemSpawn(net.minecraftforge.event.entity.EntityJoinLevelEvent event){if(!event.getLevel().isClientSide&&event.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity item)removeMending(item.getItem());}
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event){if(equipment(event.getItemStack())){event.getToolTip().add(net.minecraft.network.chat.Component.translatable("tooltip.coraline.perfect_power").withStyle(net.minecraft.ChatFormatting.GOLD));var item=event.getItemStack().getItem();if(item instanceof ArmorItem||item instanceof TieredItem)event.getToolTip().add(net.minecraft.network.chat.Component.translatable(item instanceof ArmorItem?"tooltip.coraline.perfect_armor":"tooltip.coraline.perfect_ability").withStyle(net.minecraft.ChatFormatting.GRAY));}}
    private PerfectPower(){}
}
