package com.coraline.mixin;

import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Synced name is available on the client before it can predict steering. */
@Mixin(Boat.class)
public abstract class IceBoatCountdownMixin {
    @Inject(method="controlBoat",at=@At("HEAD"),cancellable=true)
    private void coraline$lockCountdown(CallbackInfo ci){
        Boat boat=(Boat)(Object)this;
        if(boat.hasCustomName()&&"CoralineRaceCountdownLock".equals(boat.getCustomName().getString())){
            boat.setDeltaMovement(Vec3.ZERO);boat.setPaddleState(false,false);ci.cancel();
        }
    }
}
