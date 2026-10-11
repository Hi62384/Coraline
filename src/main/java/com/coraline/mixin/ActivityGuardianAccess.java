package com.coraline.mixin;

import net.minecraft.world.entity.monster.Guardian;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Reuse vanilla's synchronized beam renderer, without vanilla combat AI. */
@Mixin(Guardian.class)
public interface ActivityGuardianAccess {
    @Invoker("setActiveAttackTarget") void coraline$beamTarget(int id);
}
