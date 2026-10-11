package com.coraline.mixin;

import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Guardian;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Noncombat course actors must not disappear when a player selects Peaceful. */
@Mixin(Monster.class)
public abstract class ActivityGuardianPeacefulMixin {
    @Inject(method="shouldDespawnInPeaceful",at=@At("HEAD"),cancellable=true)
    private void coraline$keepCourseActor(CallbackInfoReturnable<Boolean> ci){
        var entity=(Monster)(Object)this;
        if(entity instanceof Guardian&&entity.getPersistentData().hasUUID("CoralineOceanOwner"))ci.setReturnValue(false);
        if(entity.getPersistentData().hasUUID("CoralineNewActivityOwner")&&(entity.getPersistentData().getInt("MansionClue")>0||entity.getPersistentData().getBoolean("MineEnemy")))ci.setReturnValue(false);
    }
}
