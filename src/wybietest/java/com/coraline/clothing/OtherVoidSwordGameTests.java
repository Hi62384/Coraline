package com.coraline.clothing;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;

/** Real server attack calls with a normally ticking player charge and a
 * disposable target. Not graphical combat or complete multiplayer acceptance. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class OtherVoidSwordGameTests {
    @GameTest(template="empty")
    public static void voidSwordHasSixtyBaseDamageAndNearAxeSpeed(GameTestHelper h) {
        var sword=ArtifactItems.OTHER_VOID_SWORD.get();
        var modifiers=sword.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND);
        double damage=1+modifiers.get(Attributes.ATTACK_DAMAGE).stream().mapToDouble(m->m.getAmount()).sum();
        double speed=4+modifiers.get(Attributes.ATTACK_SPEED).stream().mapToDouble(m->m.getAmount()).sum();
        h.assertTrue(Math.abs(damage-60)<.001&&Math.abs(speed-.9)<.001,"Other Void Sword lost its actual heavy weapon attributes");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=180)
    public static void rapidClicksAndCooldownRemovalCannotRepeatFullVoidDamage(GameTestHelper h) {
        for(int x=0;x<6;x++)for(int z=0;z<7;z++)h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
        var player=FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"VoidSwordQA"));
        var at=h.absolutePos(new BlockPos(2,1,2));player.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);player.setOnGround(true);
        player.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ArtifactItems.OTHER_VOID_SWORD.get()));
        // ServerPlayer.doTick calls the normal Player tick, including charge;
        // FakePlayer.tick itself intentionally does nothing in Forge.
        for(int i=0;i<100;i++)player.doTick();
        h.assertTrue(player.getAttackStrengthScale(.5F)==1,"Real player attack charge did not fill");
        var target=EntityType.COW.create(h.getLevel());
        h.assertTrue(target!=null,"No disposable target");target.setNoAi(true);target.setNoGravity(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);target.setHealth(1000);
        target.setPos(at.getX()+.5,at.getY(),at.getZ()+2);h.getLevel().addFreshEntity(target);
        player.attack(target);
        h.assertTrue(Math.abs(target.getHealth()-940)<.01,"Charged server hit did not deal the expected unmitigated 60 damage");
        for(int i=0;i<20;i++) {
            // Removing the target's generic hurt window deliberately isolates
            // the weapon gate; production never removes this protection.
            target.invulnerableTime=0;player.attack(target);
        }
        h.assertTrue(Math.abs(target.getHealth()-940)<.01,"Rapid clicks repeated the heavy strike");
        player.getCooldowns().removeCooldown(ArtifactItems.OTHER_VOID_SWORD.get());
        for(int i=0;i<100;i++)player.doTick();
        target.invulnerableTime=0;player.attack(target);
        h.assertTrue(Math.abs(target.getHealth()-940)<.01,"Removing HUD cooldown/recharging in the same world tick bypassed the server clock");
        h.runAfterDelay(24,()->{
            for(int i=0;i<100;i++)player.doTick();target.invulnerableTime=0;player.attack(target);
            h.assertTrue(Math.abs(target.getHealth()-880)<.01,"A fully charged later strike never unlocked");target.discard();h.succeed();
        });
    }
    private OtherVoidSwordGameTests() {}
}
