package com.coraline.clothing;

import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.gametest.*;
import com.coraline.registry.ModItems;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class ArmorSetGameTests {
    private static void equip(Player p,ArmorSets.Set set){for(var type:ArmorItem.Type.values())p.setItemSlot(type.getSlot(),ArmorSets.ITEMS.get(set).get(type).get().getDefaultInstance());}
    @GameTest(template="empty") public static void completeSetsAndArmorTotals(GameTestHelper h){
        int[] totals={9,6,11,14,5,11,14,20,9,20,30,24,20,36,43,50,22,18,28};
        var p=h.makeMockSurvivalPlayer();int i=0;
        for(var set:ArmorSets.Set.values()){
            equip(p,set);int total=0;for(var stack:p.getArmorSlots())total+=((ArmorItem)stack.getItem()).getDefense();
            h.assertTrue(total==totals[i++],"Incorrect armor total: "+set);
            h.assertTrue(ArmorSets.full(p,set),"Matching full set not recognized");
            p.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);h.assertFalse(ArmorSets.full(p,set),"Three pieces counted as a full set");
        }h.succeed();
    }
    @GameTest(template="empty") public static void dreamSpeedScalesPerPiece(GameTestHelper h){
        var p=h.makeMockSurvivalPlayer();int n=0;
        for(var type:ArmorItem.Type.values()){
            p.setItemSlot(type.getSlot(),ArmorSets.ITEMS.get(ArmorSets.Set.DREAMWEAVE).get(type).get().getDefaultInstance());
            h.assertTrue(Math.abs(ClothingEffects.speedBonus(p)-++n*.1)<.00001,"Wrong per-piece movement bonus");
        }h.succeed();
    }
    @GameTest(template="empty") public static void highArmorIsNotTruncated(GameTestHelper h){
        var p=h.makeMockSurvivalPlayer();equip(p,ArmorSets.Set.OTHER_WORLD_VOID);
        p.getAttribute(Attributes.ARMOR).setBaseValue(50);
        h.assertTrue(p.getArmorValue()==50,"High-tier armor was clamped at vanilla's attribute limit");h.succeed();
    }
    @GameTest(template="empty") public static void gogglesDoNotRemovePotionVision(GameTestHelper h){
        var p=h.makeMockSurvivalPlayer();var tick=new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p);
        p.setItemSlot(EquipmentSlot.HEAD,ModItems.MIDNIGHT_GOGGLES.get().getDefaultInstance());GoggleVision.tick(tick);
        h.assertTrue(p.hasEffect(MobEffects.NIGHT_VISION),"Goggles did not grant night vision");
        p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);GoggleVision.tick(tick);
        h.assertFalse(p.hasEffect(MobEffects.NIGHT_VISION),"Goggle effect lingered after removal");
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,1200));
        p.setItemSlot(EquipmentSlot.HEAD,ModItems.MIDNIGHT_GOGGLES.get().getDefaultInstance());GoggleVision.tick(tick);
        p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);GoggleVision.tick(tick);
        h.assertTrue(p.getEffect(MobEffects.NIGHT_VISION).getDuration()==1200,"Goggles destroyed independent potion vision");h.succeed();
    }
    @GameTest(template="empty") public static void soulboundHitAndKillHealing(GameTestHelper h){
        var p=h.makeMockSurvivalPlayer();equip(p,ArmorSets.Set.SOULBOUND);p.setHealth(5);
        var mob=EntityType.ZOMBIE.create(h.getLevel());var damage=p.damageSources().playerAttack(p);
        ArmorSetEffects.soulHit(new LivingDamageEvent(mob,damage,3));h.assertTrue(p.getHealth()==6,"Mob hit should heal half a heart");
        ArmorSetEffects.soulKill(new LivingDeathEvent(mob,damage));h.assertTrue(p.getHealth()==12,"Mob kill should heal three hearts");
        p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);ArmorSetEffects.soulHit(new LivingDamageEvent(mob,damage,3));h.assertTrue(p.getHealth()==12,"Partial soul set healed");h.succeed();
    }
    @GameTest(template="empty") public static void retaliationUsesDamageAndAvoidsRecursion(GameTestHelper h){
        var p=h.makeMockSurvivalPlayer();equip(p,ArmorSets.Set.BUTTONGUARD);
        var mob=EntityType.ZOMBIE.create(h.getLevel());mob.getAttribute(Attributes.ARMOR).setBaseValue(0);mob.setHealth(20);
        ArmorSetEffects.retaliation(new LivingDamageEvent(p,p.damageSources().mobAttack(mob),10));
        h.assertTrue(Math.abs(mob.getHealth()-16)<.001,"Four pieces should reflect 40% of actual damage");
        float health=mob.getHealth();ArmorSetEffects.retaliation(new LivingDamageEvent(p,p.damageSources().thorns(mob),10));
        h.assertTrue(mob.getHealth()==health,"Thorns reflected recursively");h.succeed();
    }
    @GameTest(template="empty") public static void luckRequiresMatchingFullSet(GameTestHelper h){
        var p=h.makeMockSurvivalPlayer();equip(p,ArmorSets.Set.LUCK);var mob=EntityType.ZOMBIE.create(h.getLevel());
        var event=new LootingLevelEvent(mob,p.damageSources().playerAttack(p),1);ArmorSetEffects.looting(event);
        h.assertTrue(event.getLootingLevel()==3,"Luck did not add to existing Looting");
        p.setItemSlot(EquipmentSlot.HEAD,ModItems.TOP_HAT.get().getDefaultInstance());event=new LootingLevelEvent(mob,p.damageSources().playerAttack(p),1);ArmorSetEffects.looting(event);
        h.assertTrue(event.getLootingLevel()==1,"Mixed armor gained full-set loot bonus");h.succeed();
    }
    @GameTest(template="empty") public static void miningUsesActualLootAndSkipsContainers(GameTestHelper h){
        var p=h.makeMockSurvivalPlayer();equip(p,ArmorSets.Set.MINERS);var pos=h.absolutePos(new net.minecraft.core.BlockPos(1,1,1));
        var drops=Block.getDrops(Blocks.STONE.defaultBlockState(),h.getLevel(),pos,null,p,new ItemStack(Items.IRON_PICKAXE));
        h.assertTrue(drops.stream().mapToInt(ItemStack::getCount).sum()==2,"Mining loot modifier did not double cobblestone");
        h.getLevel().setBlock(pos,Blocks.CHEST.defaultBlockState(),3);
        var chest=h.getLevel().getBlockEntity(pos);
        drops=Block.getDrops(Blocks.CHEST.defaultBlockState(),h.getLevel(),pos,chest,p,new ItemStack(Items.IRON_AXE));
        h.assertTrue(drops.stream().filter(s->s.is(Items.CHEST)).mapToInt(ItemStack::getCount).sum()==1,"Container drop duplicated");h.succeed();
    }
    @GameTest(template="empty") public static void spiderSetWebMovementAndClimb(GameTestHelper h){
        var p=h.makeMockSurvivalPlayer();equip(p,ArmorSets.Set.SUPER_SPIDERVISION);
        h.assertTrue(SpiderArmor.hasWebBonus(p),"Super spider set lacks web bonus");
        var pos=h.absolutePos(new net.minecraft.core.BlockPos(1,3,1));p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        double before=p.getX();p.makeStuckInBlock(Blocks.COBWEB.defaultBlockState(),new Vec3(.25,.05,.25));p.move(MoverType.SELF,new Vec3(1,0,0));
        h.assertTrue(p.getX()-before>.8,"Cobweb movement mixin did not improve speed");
        p.horizontalCollision=true;p.setDeltaMovement(Vec3.ZERO);
        SpiderArmor.movement(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));
        h.assertTrue(p.getDeltaMovement().y>=ClothingConfig.CLIMB_SPEED.get(),"Wall ascent missing");
        p.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);h.assertFalse(SpiderArmor.hasWebBonus(p),"Partial spider set active");h.succeed();
    }
    @GameTest(template="empty") public static void spiderCeilingGripReleasesOnSneak(GameTestHelper h){
        var p=h.makeMockSurvivalPlayer();equip(p,ArmorSets.Set.SUPER_SPIDERVISION);
        var pos=h.absolutePos(new net.minecraft.core.BlockPos(2,2,2));p.setPos(pos.getX()+.5,pos.getY()+.12,pos.getZ()+.5);
        h.getLevel().setBlock(pos.above(2),Blocks.STONE.defaultBlockState(),3);
        p.getPersistentData().putBoolean("CoralineSpiderJump",true);p.getPersistentData().putInt("CoralineSpiderInputTick",p.tickCount);
        SpiderArmor.movement(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));
        h.assertTrue(p.getPersistentData().getBoolean("CoralineCeilingCling"),"Jump did not grip nearby ceiling");
        p.setShiftKeyDown(true);SpiderArmor.movement(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));
        h.assertFalse(p.getPersistentData().getBoolean("CoralineCeilingCling"),"Sneaking did not release ceiling");h.succeed();
    }
    @GameTest(template="empty",batch="clothing_weather") public static void midnightSetsFollowDayNight(GameTestHelper h){
        var p=h.makeMockSurvivalPlayer();var level=h.getLevel();long time=level.getDayTime();
        try{
            equip(p,ArmorSets.Set.MIDNIGHT);level.setDayTime(18000);h.assertTrue(ClothingEffects.slowFall(p),"Night set slow fall missing");
            p.setDeltaMovement(0,.42,0);ClothingEffects.jump(new LivingEvent.LivingJumpEvent(p));h.assertTrue(p.getDeltaMovement().y>.9,"Night set jump is not high");
            level.setDayTime(6000);h.assertFalse(ClothingEffects.slowFall(p),"Night set still active during day");
            equip(p,ArmorSets.Set.MIDNIGHT_CLOAK);level.setDayTime(18000);var event=new LivingEvent.LivingVisibilityEvent(p,EntityType.ZOMBIE.create(level),1);ClothingEffects.visibility(event);
            h.assertTrue(Math.abs(event.getVisibilityModifier()-.2)<.001,"Night cloak detection incorrect");
        }finally{level.setDayTime(time);}h.succeed();
    }
    @GameTest(template="empty") public static void gardenVinesApplyBothRetaliationEffects(GameTestHelper h){
        var p=h.makeMockSurvivalPlayer();equip(p,ArmorSets.Set.GARDEN_VINES);var mob=EntityType.PIG.create(h.getLevel());double old=ClothingConfig.VINE_CHANCE.get();
        try{ClothingConfig.VINE_CHANCE.set(1.0);ArmorSetEffects.retaliation(new LivingDamageEvent(p,p.damageSources().mobAttack(mob),2));}
        finally{ClothingConfig.VINE_CHANCE.set(old);}
        h.assertTrue(mob.hasEffect(MobEffects.POISON)&&mob.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),"Garden retaliation missing poison or slowness");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=60) public static void defensiveWebExpiresWithoutDrops(GameTestHelper h){
        var pos=h.absolutePos(new net.minecraft.core.BlockPos(3,2,3));int old=ClothingConfig.WEB_LIFETIME.get();
        try{ClothingConfig.WEB_LIFETIME.set(20);h.getLevel().setBlock(pos,com.coraline.registry.ModBlocks.FADING_COBWEB.get().defaultBlockState(),3);}
        finally{ClothingConfig.WEB_LIFETIME.set(old);}
        h.runAtTickTime(25,()->{h.assertTrue(h.getLevel().getBlockState(pos).isAir(),"Defensive cobweb did not expire");h.succeed();});
    }
}
