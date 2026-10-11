package com.coraline.clothing;

import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class WeaponGameTests {
    @GameTest(template="empty") public static void whipCooldownBlocksRepeatedSwings(GameTestHelper h){
        var p=h.makeMockSurvivalPlayer();var whip=Weapons.ITEMS.get("thread_whip").get();p.setItemSlot(EquipmentSlot.MAINHAND,whip.getDefaultInstance());
        h.assertTrue(WhipAction.begin(p,null),"First whip swing rejected");
        h.assertTrue(!WhipAction.begin(p,null),"Repeated whip swing bypassed cooldown");
        for(int i=0;i<WeaponConfig.WHIP_COOLDOWN.get();i++)p.getCooldowns().tick();
        h.assertTrue(WhipAction.begin(p,null),"Whip did not become usable again");
        h.assertTrue(WeaponConfig.WAND_COOLDOWN.get()==40,"Default wand cooldown is not 2 seconds");h.succeed();
    }
    private static double stat(String name,net.minecraft.world.entity.ai.attributes.Attribute attr,double base){return base+Weapons.ITEMS.get(name).get().getDefaultAttributeModifiers(EquipmentSlot.MAINHAND).get(attr).stream().mapToDouble(m->m.getAmount()).sum();}
    private static double toolDamage(String name){return 1+OtherMaterials.TOOLS.get(name).get().getDefaultAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).stream().mapToDouble(m->m.getAmount()).sum();}
    @GameTest(template="empty") public static void materialToolBalance(GameTestHelper h){
        h.assertTrue(OtherMaterials.IRON.getLevel()==2&&OtherMaterials.IRON.getUses()==1450&&OtherMaterials.IRON.getSpeed()==7.5F,"Other iron must retain iron mining tier below diamond speed and durability");
        h.assertTrue(OtherMaterials.DIAMOND.getLevel()==3&&OtherMaterials.DIAMOND.getUses()==3903&&OtherMaterials.DIAMOND.getSpeed()==9F,"Other diamond tool tier is wrong");
        h.assertTrue(OtherMaterials.MOONSTONE_TIER.getLevel()==3&&OtherMaterials.MOONSTONE_TIER.getUses()==1450&&OtherMaterials.MOONSTONE_TIER.getSpeed()==8F,"Moonstone must mine like diamond with slightly less durability");
        h.assertTrue(toolDamage("other_iron_sword")==8&&toolDamage("other_diamond_sword")==10,"Other-world sword damage is wrong");
        h.assertTrue(toolDamage("moonstone_pickaxe")==6&&toolDamage("moonstone_axe")==11&&toolDamage("moonstone_shovel")==6.5,"Moonstone tool damage is wrong");
        h.assertTrue(Weapons.ITEMS.get("moonblade").get().getMaxDamage()==1450,"Moonblade durability should match moonstone tools");
        h.assertTrue(ArmorSets.Set.MOONSTONE.getDurabilityForType(ArmorItem.Type.CHESTPLATE)==480,"Moonstone armor must be slightly less durable than diamond");
        h.assertTrue(OtherMaterials.PERFECT.getUses()==4683&&toolDamage("perfect_sword")==15&&toolDamage("perfect_pickaxe")==9&&toolDamage("perfect_axe")==20&&toolDamage("perfect_shovel")==9.5,"Perfect tool balance is wrong");
        h.assertTrue(ArmorSets.Set.OTHER_IRON.getDefenseForType(ArmorItem.Type.LEGGINGS)==6&&ArmorSets.Set.OTHER_DIAMOND.getDefenseForType(ArmorItem.Type.CHESTPLATE)==9&&ArmorSets.Set.PERFECT.getDefenseForType(ArmorItem.Type.CHESTPLATE)==12,"Other-world armor protection is wrong");
        h.assertTrue(ArmorSets.Set.OTHER_DIAMOND.getDurabilityForType(ArmorItem.Type.CHESTPLATE)==1056&&ArmorSets.Set.PERFECT.getDurabilityForType(ArmorItem.Type.CHESTPLATE)==1056,"Other-world armor durability is wrong");
        h.succeed();
    }
    @GameTest(template="empty") public static void perfectFarmlandSupportsCrops(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new net.minecraft.core.BlockPos(1,2,1));
        level.setBlock(pos,com.coraline.registry.ModBlocks.PERFECT_FARMLAND.get().defaultBlockState(),3);
        h.assertTrue(net.minecraft.world.level.block.Blocks.WHEAT.defaultBlockState().canSurvive(level,pos.above()),"Crops must survive on perfect farmland");
        h.succeed();
    }
    @GameTest(template="empty") public static void weaponDamageSpeedAndReach(GameTestHelper h){
        h.assertTrue(stat("needle_rapier",Attributes.ATTACK_DAMAGE,1)==3&&stat("needle_rapier",Attributes.ATTACK_SPEED,4)>3,"Rapier is not fast and light");
        for(var entry:java.util.Map.of("moonblade",8,"vine_sword",6,"button_mace",14,"spidervision_sword",8,"thread_whip",5).entrySet())h.assertTrue(stat(entry.getKey(),Attributes.ATTACK_DAMAGE,1)==entry.getValue(),"Wrong damage: "+entry.getKey());
        h.assertTrue(stat("button_mace",Attributes.ATTACK_SPEED,4)<.6,"Mace attacks too quickly");
        h.assertTrue(stat("thread_whip",net.minecraftforge.common.ForgeMod.ENTITY_REACH.get(),3)==5.5,"Whip lacks extra reach");
        h.assertTrue(OtherMaterials.PERFECT.getSpeed()>Tiers.DIAMOND.getSpeed()*1.5&&OtherMaterials.PERFECT.getUses()>Tiers.DIAMOND.getUses()*2,"Perfect tools are not substantially improved");h.succeed();
    }
    @GameTest(template="empty",batch="clothing_weather") public static void moonbladeDamageFollowsNight(GameTestHelper h){
        var p=h.makeMockSurvivalPlayer();var level=h.getLevel();long time=level.getDayTime();var item=Weapons.ITEMS.get("moonblade").get();
        p.setItemSlot(EquipmentSlot.MAINHAND,item.getDefaultInstance());var damage=p.getAttribute(Attributes.ATTACK_DAMAGE);
        for(var modifier:item.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE))damage.addTransientModifier(modifier);
        try{
            level.setDayTime(6000);Weapons.moon(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));h.assertTrue(damage.getValue()==8,"Day damage must be eight");
            level.setDayTime(18000);for(int i=0;i<5;i++)Weapons.moon(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));h.assertTrue(damage.getValue()==15,"Night damage must be fifteen, without stacking");
            p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);Weapons.moon(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));h.assertTrue(damage.getValue()==8,"Night modifier survived putting the blade away");
        }finally{level.setDayTime(time);}h.succeed();
    }
    @GameTest(template="empty") public static void vineDebuffsAndWhipPull(GameTestHelper h){
        var p=h.makeMockSurvivalPlayer();var target=EntityType.PIG.create(h.getLevel());var pos=h.absolutePos(new net.minecraft.core.BlockPos(2,2,2));
        p.setPos(pos.getX()-2,pos.getY(),pos.getZ());target.setPos(pos.getX(),pos.getY(),pos.getZ());
        var vine=Weapons.ITEMS.get("vine_sword").get();vine.hurtEnemy(vine.getDefaultInstance(),target,p);
        h.assertTrue(target.hasEffect(MobEffects.POISON)&&target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),"Vine sword debuffs missing");
        var whip=Weapons.ITEMS.get("thread_whip").get();whip.hurtEnemy(whip.getDefaultInstance(),target,p);
        h.assertTrue(target.getDeltaMovement().x<0,"Whip did not pull target toward attacker");h.succeed();
    }
    @GameTest(template="empty") public static void wandDamageCooldownAndProjectileCleanup(GameTestHelper h){
        var p=h.makeMockSurvivalPlayer();var target=EntityType.PIG.create(h.getLevel());float before=target.getHealth();
        var bolt=new MagicBolt(h.getLevel(),p);bolt.onHit(new EntityHitResult(target));
        h.assertTrue(target.getHealth()==before-5&&bolt.isRemoved(),"Wand bolt damage or impact cleanup incorrect");
        var wand=Weapons.ITEMS.get("midnight_wand").get();p.setItemSlot(EquipmentSlot.MAINHAND,wand.getDefaultInstance());
        wand.use(h.getLevel(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(p.getCooldowns().isOnCooldown(wand),"Wand cooldown missing");
        h.assertTrue(wand.use(h.getLevel(),p,net.minecraft.world.InteractionHand.MAIN_HAND).getResult()==net.minecraft.world.InteractionResult.FAIL,"Wand fired during cooldown");
        for(int i=0;i<39;i++)p.getCooldowns().tick();
        h.assertTrue(p.getCooldowns().isOnCooldown(wand),"Wand cooldown ended before two seconds");
        p.getCooldowns().tick();
        h.assertTrue(!p.getCooldowns().isOnCooldown(wand),"Wand cooldown lasted longer than two seconds");h.succeed();
    }
    @GameTest(template="empty") public static void maceSplashWithoutSweep(GameTestHelper h){
        var p=h.makeMockSurvivalPlayer();var pos=h.absolutePos(new net.minecraft.core.BlockPos(2,2,2));p.setPos(pos.getX(),pos.getY(),pos.getZ()-1);
        var target=h.spawn(EntityType.PIG,new net.minecraft.core.BlockPos(2,2,2));target.setNoAi(true);
        var other=h.spawn(EntityType.COW,new net.minecraft.core.BlockPos(3,2,2));other.setNoAi(true);float before=other.getHealth();
        var mace=Weapons.ITEMS.get("button_mace").get();
        h.assertTrue(!mace.getDefaultInstance().canPerformAction(net.minecraftforge.common.ToolActions.SWORD_SWEEP),"Mace still sweeps like a sword");
        mace.hurtEnemy(mace.getDefaultInstance(),target,p);
        h.assertTrue(other.getHealth()==before-14,"Mace did not splash 14 damage on hit");h.succeed();
    }
}
