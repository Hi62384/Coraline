package com.coraline.mixin;

import com.coraline.feast.GhostVictimDirector;
import com.coraline.feast.VisitData;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ghost flight is not permission to possess an entity or visit another owner's
 * realm through vanilla spectator packets. Authoritative story portals still work. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class GhostVictimSpectatorMixin {
    @Shadow public ServerPlayer player;
    private boolean coraline$ghost(){
        return player.server.isSameThread()&&player.isSpectator()
            &&GhostVictimDirector.active(VisitData.get(player.server).scenes.get(player.getUUID()));
    }
    @Inject(method="handleTeleportToEntityPacket",at=@At("HEAD"),cancellable=true)
    private void coraline$noSpectatorTeleport(ServerboundTeleportToEntityPacket packet,CallbackInfo ci){if(coraline$ghost())ci.cancel();}
    @Inject(method="handleInteract",at=@At("HEAD"),cancellable=true)
    private void coraline$noPossession(ServerboundInteractPacket packet,CallbackInfo ci){if(coraline$ghost())ci.cancel();}
}
