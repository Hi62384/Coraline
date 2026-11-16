package com.coraline.mixin;
import com.coraline.feast.PartyStorageGuard;
import com.coraline.feast.AcceptedButtonEyesInventory;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PartyPacketMixin implements PartyStorageGuard.GuardedPackets {
    @Shadow public ServerPlayer player;
    @Inject(method="handleContainerClick",at=@At("HEAD"),cancellable=true) private void coraline$click(ServerboundContainerClickPacket packet,CallbackInfo ci){
        // Netty invocation must first pass the vanilla thread handoff; inspect only its server-thread replay.
        if(player.server.isSameThread()&&(AcceptedButtonEyesInventory.locked(player)&&player.containerMenu!=player.inventoryMenu||PartyStorageGuard.blocksMenu(player,player.containerMenu))){player.containerMenu.broadcastFullState();ci.cancel();}
    }
    @Inject(method="handleSetCreativeModeSlot",at=@At("HEAD"),cancellable=true) private void coraline$acceptedCreative(ServerboundSetCreativeModeSlotPacket packet,CallbackInfo ci){
        if(player.server.isSameThread()&&AcceptedButtonEyesInventory.locked(player)){player.inventoryMenu.broadcastFullState();ci.cancel();}
    }
    @Inject(method="handlePlayerAction",at=@At("HEAD"),cancellable=true) private void coraline$acceptedDrop(ServerboundPlayerActionPacket packet,CallbackInfo ci){
        if(!player.server.isSameThread()||!AcceptedButtonEyesInventory.locked(player))return;
        switch(packet.getAction()){case DROP_ITEM,DROP_ALL_ITEMS,SWAP_ITEM_WITH_OFFHAND->{player.inventoryMenu.broadcastFullState();ci.cancel();}default->{}}
    }
}
