package com.coraline.feast;

import com.coraline.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Keep old registry IDs loadable, but only the original Button Token is used. */
@Mod.EventBusSubscriber(modid="coraline")
public final class ButtonTokenMigration {
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||event.player.level().isClientSide||event.player.tickCount%20!=0)return;
        var inventory=event.player.getInventory();
        for(int i=0;i<inventory.getContainerSize();i++){
            var old=inventory.getItem(i);
            if(old.is(ModItems.MAGIC_BUTTON_TOKEN.get())||old.is(ModItems.CARNIVAL_TOKEN.get())){
                var replacement=new ItemStack(ModItems.VILLAGE_TOKEN.get(),old.getCount());
                if(old.hasTag())replacement.setTag(old.getTag().copy());
                inventory.setItem(i,replacement);inventory.setChanged();
            }
        }
    }
}
