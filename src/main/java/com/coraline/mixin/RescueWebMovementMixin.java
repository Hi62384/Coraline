package com.coraline.mixin;

import com.coraline.feast.RescueWebHearing;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe only vanilla-accepted server-thread steps; never modify movement or packets. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class RescueWebMovementMixin {
    @Shadow public ServerPlayer player;
    @Unique private Vec3 coraline$beforeWebMove;
    @Inject(method="handleMovePlayer",at=@At("HEAD"))
    private void coraline$beforeWebMove(ServerboundMovePlayerPacket packet,CallbackInfo ci){
        // Netty's initial invocation schedules a server-thread replay. Never
        // inspect/mutate the scene or shared sample state on that first thread.
        if(player.server.isSameThread())coraline$beforeWebMove=player.position();
    }
    @Inject(method="handleMovePlayer",at=@At("RETURN"))
    private void coraline$acceptedWebMove(ServerboundMovePlayerPacket packet,CallbackInfo ci){
        if(player.server.isSameThread()&&coraline$beforeWebMove!=null){var before=coraline$beforeWebMove;coraline$beforeWebMove=null;RescueWebHearing.acceptedMove(player,before.x,before.z);}
    }
}
