package com.coraline.clothing;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid="coraline")
public final class MoonstoneEffects {
    private static final UUID ARMOR_ID=UUID.fromString("713a9b08-c787-47b1-a34c-e41839092396");
    private static final UUID AXE_ID=UUID.fromString("3f6faabe-0198-4d23-b75f-958d0908c537");
    private static final String SILK_MARK="CoralineNightSilk";
    private static final String FORTUNE_MARK="CoralinePerfectFortuneOriginal";
    private static boolean tool(ItemStack stack){return stack.is(Weapons.ITEMS.get("moonblade").get())||OtherMaterials.TOOLS.entrySet().stream().anyMatch(e->e.getKey().startsWith("moonstone_")&&stack.is(e.getValue().get()));}
    private static boolean held(net.minecraft.world.entity.player.Player player,String name){return player.getMainHandItem().is(OtherMaterials.TOOLS.get("moonstone_"+name).get());}

    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||event.player.level().isClientSide)return;
        var player=event.player;
        boolean night=player.isAlive()&&ClothingEffects.night(player.level());
        var armor=player.getAttribute(Attributes.ARMOR);
        if(armor!=null){
            double bonus=0;
            if(night)for(var type:net.minecraft.world.item.ArmorItem.Type.values()){
                var item=player.getItemBySlot(type.getSlot()).getItem();
                if(item instanceof SetArmorItem piece&&piece.set==ArmorSets.Set.MOONSTONE)bonus+=piece.getDefense()/2.0;
            }
            update(armor,ARMOR_ID,"Moonstone night protection",bonus);
        }
        var attack=player.getAttribute(Attributes.ATTACK_DAMAGE);
        if(attack!=null)update(attack,AXE_ID,"Moonstone axe night edge",night&&held(player,"axe")?7:0);
        // The marker lets us remove only the Silk Touch supplied by moonlight, not a real enchantment.
        for(int slot=0;slot<player.getInventory().getContainerSize();slot++){
            var stack=player.getInventory().getItem(slot);
            if(stack.getTag()!=null&&stack.getTag().getBoolean(SILK_MARK)&&(!night||stack!=player.getMainHandItem()))removeSilk(stack);
            if(stack.getTag()!=null&&stack.getTag().contains(FORTUNE_MARK)&&stack!=player.getMainHandItem())removeFortune(stack);
        }
        var main=player.getMainHandItem();
        if(night&&held(player,"shovel")&&EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH,main)==0){
            var enchants=EnchantmentHelper.getEnchantments(main);enchants.put(Enchantments.SILK_TOUCH,1);
            EnchantmentHelper.setEnchantments(enchants,main);main.getOrCreateTag().putBoolean(SILK_MARK,true);
        }
        if(main.is(OtherMaterials.TOOLS.get("perfect_pickaxe").get())&&EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE,main)<3){
            int original=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE,main);
            var enchants=EnchantmentHelper.getEnchantments(main);enchants.put(Enchantments.BLOCK_FORTUNE,3);
            EnchantmentHelper.setEnchantments(enchants,main);main.getOrCreateTag().putInt(FORTUNE_MARK,original);
        }
    }
    private static void removeSilk(ItemStack stack){
        var enchants=EnchantmentHelper.getEnchantments(stack);enchants.remove(Enchantments.SILK_TOUCH);
        EnchantmentHelper.setEnchantments(enchants,stack);stack.getOrCreateTag().remove(SILK_MARK);
    }
    private static void removeFortune(ItemStack stack){
        int original=stack.getOrCreateTag().getInt(FORTUNE_MARK);
        var enchants=EnchantmentHelper.getEnchantments(stack);
        if(original>0)enchants.put(Enchantments.BLOCK_FORTUNE,original);else enchants.remove(Enchantments.BLOCK_FORTUNE);
        EnchantmentHelper.setEnchantments(enchants,stack);stack.getOrCreateTag().remove(FORTUNE_MARK);
    }
    private static void cleanTemporary(ItemStack stack){
        if(stack.getTag()==null)return;
        if(stack.getTag().getBoolean(SILK_MARK))removeSilk(stack);
        if(stack.getTag()!=null&&stack.getTag().contains(FORTUNE_MARK))removeFortune(stack);
    }
    @SubscribeEvent public static void toss(ItemTossEvent event){cleanTemporary(event.getEntity().getItem());}
    @SubscribeEvent public static void deathDrops(LivingDropsEvent event){
        if(event.getEntity() instanceof net.minecraft.world.entity.player.Player)for(var drop:event.getDrops())cleanTemporary(drop.getItem());
    }
    private static void update(net.minecraft.world.entity.ai.attributes.AttributeInstance attribute,UUID id,String name,double value){
        var old=attribute.getModifier(id);
        if(old!=null&&old.getAmount()!=value)attribute.removeModifier(id);
        if(value>0&&attribute.getModifier(id)==null)attribute.addTransientModifier(new AttributeModifier(id,name,value,AttributeModifier.Operation.ADDITION));
    }
    @SubscribeEvent public static void speed(PlayerEvent.BreakSpeed event){
        var player=event.getEntity();
        boolean moon=ClothingEffects.night(player.level())&&(held(player,"axe")||held(player,"shovel"));
        var main=player.getMainHandItem();
        boolean perfect=main.is(OtherMaterials.TOOLS.get("perfect_pickaxe").get())||main.is(OtherMaterials.TOOLS.get("perfect_axe").get())||main.is(OtherMaterials.TOOLS.get("perfect_shovel").get())||main.is(OtherMaterials.TOOLS.get("perfect_hoe").get());
        if(!moon&&!perfect)return;
        if(player.getMainHandItem().getDestroySpeed(event.getState())<=1)return;
        // Efficiency V adds 5^2+1 to an effective tool's base speed.
        event.setNewSpeed(Math.max(event.getNewSpeed(),player.getMainHandItem().getDestroySpeed(event.getState())+26));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void crops(BlockEvent.BreakEvent event){
        if(!(event.getLevel() instanceof ServerLevel level)||event.getPlayer().isCreative()||!ClothingEffects.night(level)||!held(event.getPlayer(),"hoe"))return;
        if(!(event.getState().getBlock() instanceof CropBlock crop)||!crop.isMaxAge(event.getState()))return;
        if(level.random.nextFloat()<.35F){
            var block=event.getState().getBlock();
            var extra=block==Blocks.WHEAT?Items.WHEAT:block==Blocks.CARROTS?Items.CARROT:block==Blocks.POTATOES?Items.POTATO:block==Blocks.BEETROOTS?Items.BEETROOT:null;
            if(extra!=null)Block.popResource(level,event.getPos(),new ItemStack(extra));
        }
        if(level.random.nextFloat()<.02F)Block.popResource(level,event.getPos(),new ItemStack(Items.GOLDEN_CARROT));
    }
}
