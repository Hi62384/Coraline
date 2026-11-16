package com.coraline.clothing;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;

/** Requires the real current Mixin/refmap. Compiling this does not test a hit. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class RapierCombatGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        for(int x=0;x<6;x++)for(int z=0;z<7;z++)h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
        var p=FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"RapierCombatQA"));
        var at=h.absolutePos(new BlockPos(2,1,2));p.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);p.setNoGravity(true);p.setOnGround(true);
        p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Weapons.ITEMS.get("needle_rapier").get()));
        charge(p);return p;
    }
    private static void charge(ServerPlayer p) {for(int i=0;i<40;i++)p.doTick();p.setOnGround(true);p.fallDistance=0;}
    private static net.minecraft.world.entity.animal.Cow target(GameTestHelper h,ServerPlayer p) {
        var target=EntityType.COW.create(h.getLevel());target.setNoAi(true);target.setNoGravity(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);target.setHealth(1000);
        target.setPos(p.getX(),p.getY(),p.getZ()+1.5);h.getLevel().addFreshEntity(target);return target;
    }
    @GameTest(template="empty",timeoutTicks=150)
    public static void sameOwnerCanThrustQuicklyWhileForeignHitsKeepOrdinaryProtection(GameTestHelper h) {
        h.assertTrue(WeaponConfig.RAPIER_INTERVAL.get()==7,"Regression requires disposable default weapon configuration");
        var first=player(h);var second=player(h);var target=target(h,first);
        first.attack(target);
        h.assertTrue(Math.abs(target.getHealth()-997)<.01,"Initial fully charged rapier hit failed");
        for(int i=0;i<20;i++)first.attack(target);
        h.assertTrue(Math.abs(target.getHealth()-997)<.01,"Repeated same-tick packets dealt rapid unbounded damage");
        h.runAfterDelay(8,()->{
            h.assertTrue(target.invulnerableTime>10,"Fixture did not exercise the ordinary still-active hurt window");
            charge(second);second.attack(target);
            h.assertTrue(Math.abs(target.getHealth()-997)<.01,"Another player bypassed ordinary target protection through rapier provenance");
            charge(first);first.attack(target);
            h.assertTrue(Math.abs(target.getHealth()-994)<.01,"Real primary hurt hook did not permit the owning rapier's rapid second strike");
            for(int i=0;i<20;i++)first.attack(target);
            h.assertTrue(Math.abs(target.getHealth()-994)<.01,"Rapid hit refreshed without the server cadence");target.discard();h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=150)
    public static void invulnerableTargetsKeepProtectionAndAnUnsuccessfulHitRestoresTheWindow(GameTestHelper h) {
        h.assertTrue(WeaponConfig.RAPIER_INTERVAL.get()==7,"Regression requires disposable default weapon configuration");
        var p=player(h);var target=target(h,p);p.attack(target);
        h.assertTrue(Math.abs(target.getHealth()-997)<.01,"Initial rapid-window provenance not established");
        h.runAfterDelay(8,()->{
            int before=target.invulnerableTime;target.setInvulnerable(true);charge(p);p.attack(target);
            h.assertTrue(target.isInvulnerable()&&Math.abs(target.getHealth()-997)<.01,"Rapier bypassed an unrelated damage immunity");
            h.assertTrue(target.invulnerableTime>=before,"A cancelled/immune hit left the ordinary protection window cleared");target.discard();h.succeed();
        });
    }
    private RapierCombatGameTests() {}
}
