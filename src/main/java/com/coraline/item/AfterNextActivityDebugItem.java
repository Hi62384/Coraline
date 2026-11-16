package com.coraline.item;
import com.coraline.feast.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
public final class AfterNextActivityDebugItem extends Item {
    public AfterNextActivityDebugItem(){super(new Properties().stacksTo(1));}
    @Override public Component getName(ItemStack stack){return Component.literal("Debug: Deteriorate After Next Activity");}
    private void activate(net.minecraft.world.entity.player.Player player){if(!(player instanceof ServerPlayer p)||p.getCooldowns().isOnCooldown(this))return;
        var scene=OtherMotherLoop.scene(p);
        if(!p.hasPermissions(2)||scene==null||!p.getUUID().equals(scene.owner)||!AcceptedButtonEyesProgress.active(scene)){p.sendSystemMessage(Component.literal("Use this with operator permission in your accepted button-eye storyline."));return;}
        var n=AcceptedButtonEyesProgress.data(scene);if(AcceptedButtonEyesProgress.stage(n)>=5){p.sendSystemMessage(Component.literal("The world is already fully deteriorated."));return;}
        n.putBoolean("DebugCollapseAfterNext",true);OtherMotherLoop.changed(p);p.getCooldowns().addCooldown(this,10);
        p.sendSystemMessage(Component.literal("The next completed activity will fully deteriorate the world. Leaving early will not trigger it."));
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,net.minecraft.world.entity.player.Player player,InteractionHand hand){activate(player);return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);}
    @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context){if(context.getPlayer()!=null)activate(context.getPlayer());return InteractionResult.sidedSuccess(context.getLevel().isClientSide);}
}
