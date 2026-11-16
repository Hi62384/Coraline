package com.coraline.mixin;

import com.coraline.client.GhostVictimClient;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Spectator flight retains the normal player's complete visible ghost body. */
@Mixin(PlayerRenderer.class)
public abstract class GhostVictimPlayerModelMixin {
    @Inject(method="setModelProperties",at=@At("TAIL"))
    private void coraline$ghostBody(AbstractClientPlayer player,CallbackInfo callback){
        if(GhostVictimClient.ghost()&&player==net.minecraft.client.Minecraft.getInstance().player)
            ((PlayerRenderer)(Object)this).getModel().setAllVisible(true);
    }
}
