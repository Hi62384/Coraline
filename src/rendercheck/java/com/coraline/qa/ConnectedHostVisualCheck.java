package com.coraline.qa;

import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** QA-only equivalent of an open integrated multiplayer session. The fixture
 * opens its socket on 127.0.0.1 itself, without a public socket or LAN broadcast.
 * Prevent single-player pause semantics from stalling that actual TCP server.
 * Never packaged in the mod, and inert for every other QA mode. */
@Mixin(IntegratedServer.class)
public abstract class ConnectedHostVisualCheck {
    @Inject(method="isPublished",at=@At("HEAD"),cancellable=true)
    private void coraline$loopbackPublished(CallbackInfoReturnable<Boolean> result){
        if(Boolean.getBoolean("coraline.connectedStoryCheck"))result.setReturnValue(true);
    }
}
