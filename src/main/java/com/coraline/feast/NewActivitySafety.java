package com.coraline.feast;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.*;

/** Hooks complement the shared protected activity dimension; they do not replace it. */
@Mod.EventBusSubscriber(modid="coraline")
public final class NewActivitySafety {
    public static void abort(ServerPlayer p,VisitData.Scene s){MusicFestivalActivity.abort(p,s);DesertTempleActivity.abort(p,s);EndCityActivity.abort(p,s);ExpeditionActivities.abort(p,s);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){var s=OtherMotherLoop.scene(p);if(s!=null)abort(p,s);}}
    @SubscribeEvent public static void use(PlayerInteractEvent.RightClickItem e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;var s=OtherMotherLoop.active(p);if(s==null||!ActivityWorld.isActivity(p.level()))return;
        boolean puzzle=s.loop.activity==OtherMotherActivities.Activity.OTHER_DESERT_TEMPLE||s.loop.activity==OtherMotherActivities.Activity.OTHER_END_CITY;
        boolean performing=s.loop.activity==OtherMotherActivities.Activity.MUSIC_FESTIVAL&&s.loop.festival.getInt("Phase")==2;
        if(ExpeditionActivities.supports(s.loop.activity)&&!e.getItemStack().is(com.coraline.registry.ModItems.ACTIVITY_MINE_MAP.get())&&!e.getItemStack().is(com.coraline.registry.ModItems.MANSION_KEY.get())&&!e.getItemStack().is(com.coraline.registry.ModItems.SNOWBALL_POUCH.get())&&!e.getItemStack().is(com.coraline.registry.ModItems.TREASURE_NOTE.get())){e.setCanceled(true);return;}
        if((puzzle||performing)&&!EndCityActivity.canEat(p,e.getItemStack()))e.setCanceled(true);
    }
    @SubscribeEvent public static void toss(net.minecraftforge.event.entity.item.ItemTossEvent e){
        if(ExpeditionActivities.temporary(e.getEntity().getItem())&&e.getPlayer() instanceof ServerPlayer p){e.setCanceled(true);ActivityTokens.give(p,e.getEntity().getItem());return;}
        if(e.getEntity().getItem().is(com.coraline.registry.ModItems.ACTIVITY_CHORUS_FRUIT.get())&&e.getPlayer() instanceof ServerPlayer p){e.setCanceled(true);ActivityTokens.give(p,e.getEntity().getItem());}
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p){var s=OtherMotherLoop.scene(p);if(s!=null&&s.loop.endCity.getBoolean("Borrowed"))EndCityActivity.restore(p,s);if(s!=null&&ExpeditionActivities.supports(s.loop.activity)&&s.loop.phase==OtherMotherLoopState.Phase.ACTIVITY)ExpeditionActivities.abort(p,s);if(!ActivityWorld.isActivity(p.level())){ShoppingMallActivity.deliver(p);MineshaftActivity.deliver(p);}}}
    private NewActivitySafety(){}
}
