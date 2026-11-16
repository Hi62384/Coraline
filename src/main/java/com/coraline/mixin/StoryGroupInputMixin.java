package com.coraline.mixin;

import com.coraline.multiplayer.StoryCoop;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reject drops before vanilla removes the stack, rather than deleting it afterward. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class StoryGroupInputMixin {
    @Shadow public ServerPlayer player;
    @Inject(method="handleContainerClick",at=@At("HEAD"),cancellable=true)
    private void coraline$questStorage(net.minecraft.network.protocol.game.ServerboundContainerClickPacket packet,CallbackInfo ci){
        if(com.coraline.multiplayer.QuestItemRecovery.pendingSource(player)){player.containerMenu.broadcastFullState();ci.cancel();return;}
        int slot=packet.getSlotNum();
        boolean guarded=com.coraline.multiplayer.QuestItemRecovery.guarded(player,player.containerMenu.getCarried());
        if(slot>=0&&slot<player.containerMenu.slots.size())guarded|=com.coraline.multiplayer.QuestItemRecovery.guarded(player,player.containerMenu.getSlot(slot).getItem());
        if(packet.getClickType()==net.minecraft.world.inventory.ClickType.SWAP){int button=packet.getButtonNum();if(button>=0&&button<9)guarded|=com.coraline.multiplayer.QuestItemRecovery.guarded(player,player.getInventory().getItem(button));else if(button==40)guarded|=com.coraline.multiplayer.QuestItemRecovery.guarded(player,player.getOffhandItem());}
        if(guarded&&(player.containerMenu!=player.inventoryMenu||packet.getClickType()==net.minecraft.world.inventory.ClickType.THROW||slot<0||slot>=1&&slot<=4)){player.containerMenu.broadcastFullState();ci.cancel();}
    }
    @Inject(method="handlePlayerAction",at=@At("HEAD"),cancellable=true)
    private void coraline$cinematicAction(ServerboundPlayerActionPacket packet,CallbackInfo ci){
        boolean quest=com.coraline.multiplayer.QuestItemRecovery.pendingSource(player)||com.coraline.multiplayer.QuestItemRecovery.guarded(player,player.getMainHandItem());
        if(quest&&(packet.getAction()==ServerboundPlayerActionPacket.Action.DROP_ITEM||packet.getAction()==ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS)){player.containerMenu.broadcastFullState();ci.cancel();return;}
        if(!StoryCoop.locked(player))return;
        switch(packet.getAction()){
            case DROP_ITEM,DROP_ALL_ITEMS,SWAP_ITEM_WITH_OFFHAND,START_DESTROY_BLOCK,STOP_DESTROY_BLOCK,ABORT_DESTROY_BLOCK -> {player.containerMenu.broadcastChanges();ci.cancel();}
            default -> {}
        }
    }
}
