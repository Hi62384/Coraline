package com.coraline.client.clothing;

import com.coraline.clothing.RapierCombat;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class RapierInput {
    @SubscribeEvent public static void attack(InputEvent.InteractionKeyMappingTriggered event) {
        var p=Minecraft.getInstance().player;
        if(event.isAttack()&&p!=null&&RapierCombat.held(p)&&(p.getCooldowns().isOnCooldown(p.getMainHandItem().getItem())||p.getAttackStrengthScale(.5F)<1)) {
            event.setCanceled(true);event.setSwingHand(false);
        }
    }
    private RapierInput() {}
}
