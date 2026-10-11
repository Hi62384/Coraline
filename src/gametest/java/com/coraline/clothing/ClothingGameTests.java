package com.coraline.clothing;

import com.coraline.registry.ModItems;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.gametest.*;
import static com.coraline.clothing.ClothingItem.Style.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class ClothingGameTests {
    private static void tick(net.minecraft.world.entity.player.Player p){ClothingEffects.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));}
    @GameTest(template="empty") public static void armorValuesAndSlots(GameTestHelper h){
        for(var entry:ModItems.ITEMS.getEntries())if(entry.get() instanceof ClothingItem item){
            h.assertTrue(item.getDefense()==item.style.armor,"Wrong armor value: "+item.style);
            h.assertTrue(item.getEquipmentSlot()==item.style.slot.getSlot(),"Wrong wearable slot");
            h.assertTrue(item.getDefaultInstance().getMaxStackSize()==1&&item.getDefaultInstance().isDamageableItem(),"Clothes need normal equipment durability");
        }h.succeed();
    }
    @GameTest(template="empty") public static void midnightWindowsAndDimensionRules(GameTestHelper h){
        h.assertTrue(ClothingEffects.inWindow(18000,17000,19000),"Midnight excluded");
        h.assertFalse(ClothingEffects.inWindow(6000,17000,19000),"Noon included");
        h.assertTrue(ClothingEffects.inWindow(24500,23000,1000),"Wrapped time window failed");
        h.assertFalse(ClothingEffects.hasDayCycle(h.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER)),"Nether treated as a rainy midnight world");h.succeed();
    }
    @GameTest(template="empty",batch="clothing_weather") public static void speedIsConditionalAndNeverStacks(GameTestHelper h){
        var p=h.makeMockSurvivalPlayer();var level=h.getLevel();long time=level.getDayTime();
        try{
            level.setDayTime(18000);p.setItemSlot(EquipmentSlot.CHEST,ModItems.MIDNIGHT_COAT.get().getDefaultInstance());
            for(int i=0;i<30;i++)tick(p);
            var speed=p.getAttribute(Attributes.MOVEMENT_SPEED);
            h.assertTrue(speed.getModifier(ClothingEffects.SPEED_ID)!=null,"Midnight speed missing");
            h.assertTrue(speed.getModifiers().stream().filter(m->m.getId().equals(ClothingEffects.SPEED_ID)).count()==1,"Speed bonus stacked");
            level.setDayTime(6000);tick(p);h.assertTrue(speed.getModifier(ClothingEffects.SPEED_ID)==null,"Bonus survived daytime");
            level.setDayTime(18000);tick(p);p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);tick(p);
            h.assertTrue(speed.getModifier(ClothingEffects.SPEED_ID)==null,"Bonus survived unequipping");
        }finally{level.setDayTime(time);}h.succeed();
    }
    @GameTest(template="empty") public static void pajamasHealSlowlyAndStopWhenRemoved(GameTestHelper h){
        var p=h.makeMockSurvivalPlayer();p.setHealth(10);p.setItemSlot(EquipmentSlot.CHEST,ModItems.COMFY_PAJAMAS.get().getDefaultInstance());
        for(int i=0;i<ClothingConfig.REGEN_TICKS.get()-1;i++)tick(p);
        h.assertTrue(p.getHealth()==10,"Pajamas healed too early");tick(p);
        h.assertTrue(p.getHealth()==10+ClothingConfig.REGEN_HEALTH.get(),"Pajama regeneration wrong");
        float health=p.getHealth();p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);
        for(int i=0;i<ClothingConfig.REGEN_TICKS.get();i++)tick(p);
        h.assertTrue(p.getHealth()==health,"Pajamas healed while unequipped");h.succeed();
    }
    @GameTest(template="empty") public static void overallsHealFromConsumedNutritionOnly(GameTestHelper h){
        var p=h.makeMockSurvivalPlayer();p.setHealth(5);p.setItemSlot(EquipmentSlot.CHEST,ModItems.GARDEN_OVERALLS.get().getDefaultInstance());
        var food=new ItemStack(Items.COOKED_BEEF);
        ClothingEffects.ate(new LivingEntityUseItemEvent.Finish(p,food,32,ItemStack.EMPTY));
        h.assertTrue(p.getHealth()==9,"Eight nutrition should restore four health points");
        ClothingEffects.ate(new LivingEntityUseItemEvent.Finish(p,new ItemStack(Items.MILK_BUCKET),32,new ItemStack(Items.BUCKET)));
        h.assertTrue(p.getHealth()==9,"Nonfood use healed player");
        p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);ClothingEffects.ate(new LivingEntityUseItemEvent.Finish(p,food,32,ItemStack.EMPTY));
        h.assertTrue(p.getHealth()==9,"Unequipped overalls healed player");h.succeed();
    }
    @GameTest(template="empty") public static void slippersReduceDetectionWithoutChangingMobStats(GameTestHelper h){
        var p=h.makeMockSurvivalPlayer();var zombie=EntityType.ZOMBIE.create(h.getLevel());double range=zombie.getAttributeValue(Attributes.FOLLOW_RANGE);
        p.setItemSlot(EquipmentSlot.FEET,ModItems.SILENT_SLIPPERS.get().getDefaultInstance());
        var event=new LivingEvent.LivingVisibilityEvent(p,zombie,1);ClothingEffects.visibility(event);
        h.assertTrue(event.getVisibilityModifier()==.5,"Slippers should halve detection");
        h.assertTrue(zombie.getAttributeValue(Attributes.FOLLOW_RANGE)==range,"Slippers modified the mob globally");h.succeed();
    }
    @GameTest(template="empty") public static void featherBootsSlowDescentAndCircusBoostsJump(GameTestHelper h){
        var p=h.makeMockSurvivalPlayer();p.setItemSlot(EquipmentSlot.FEET,ModItems.FEATHER_BOOTS.get().getDefaultInstance());p.setOnGround(false);p.setDeltaMovement(0,-1,0);p.fallDistance=12;tick(p);
        h.assertTrue(p.getDeltaMovement().y>=-ClothingConfig.FALL_SPEED.get()&&p.fallDistance==0,"Feather boots did not slow descent safely");
        p.setDeltaMovement(0,.42,0);tick(p);h.assertTrue(p.getDeltaMovement().y==.42,"Feather boots changed rising motion");
        p.setItemSlot(EquipmentSlot.CHEST,ModItems.CIRCUS_JACKET.get().getDefaultInstance());ClothingEffects.jump(new LivingEvent.LivingJumpEvent(p));
        h.assertTrue(Math.abs(p.getDeltaMovement().y-.42-ClothingConfig.CIRCUS_JUMP.get())<.0001,"Circus jump incorrect");h.succeed();
    }
}
