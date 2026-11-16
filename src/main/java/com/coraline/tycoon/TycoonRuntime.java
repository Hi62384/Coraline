package com.coraline.tycoon;
import java.util.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
/** Small per-online-owner clocks; no global entity scans or unloaded pathfinding. */
@Mod.EventBusSubscriber(modid="coraline")
public final class TycoonRuntime {
    private record Cached(long until,TycoonRooms.Survey survey){}
    private static final Map<UUID,Cached> ROOMS=new HashMap<>();
    public static TycoonRooms.Survey survey(ServerPlayer p){var c=ROOMS.get(TycoonWorld.owner(p));long now=p.server.overworld().getGameTime();if(c==null||now>c.until){c=new Cached(now+200,TycoonRooms.survey(p));ROOMS.put(TycoonWorld.owner(p),c);}return c.survey;}
    @SubscribeEvent public static void player(TickEvent.PlayerTickEvent e){if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p))return;if(com.coraline.postgame.TrueHeartBattle.recoveryPending(p))return;var data=TycoonData.get(p.server);var h=data.homes.get(p.getUUID());if(p.tickCount%100==0){try{var run=com.coraline.rebirth.StoryRunStore.status(p.server,p.getUUID());if(run.complete()&&run.claimed()){TycoonWorld.rewardEntrance(p,run.run());h=data.homes.get(p.getUUID());}}catch(java.io.IOException bad){/* Preserve receipt and retry; never manufacture a reward from corrupt story data. */}}
        if(h==null)return;TycoonWorld.activationTick(p);if(p.tickCount%20==0)tickHome(p,h);
        if(p.serverLevel().dimension().equals(TycoonWorld.KEY)&&!TycoonCollaborationVisits.guest(p)&&!TycoonRetiredParcels.ordinaryAccess(p,p.blockPosition())){if(!h.active()||Math.abs(p.getX()-h.center().getX())>128||Math.abs(p.getZ()-h.center().getZ())>128||p.getY()<h.center().getY()-12){p.fallDistance=0;if(h.active())TycoonWorld.enter(p);else TycoonWorld.leave(p);}}
    }
    static void tickHome(ServerPlayer p,TycoonData.Home h){
        long now=p.server.overworld().getGameTime();if(h.state.contains("LastHouseholdTick")&&now>=h.state.getLong("LastHouseholdTick")&&now-h.state.getLong("LastHouseholdTick")<20)return;
        TycoonTransactions.reconcile(p);TycoonPlacements.reconcile(p);if(TycoonWorld.busy(p))return;
        h.state.putLong("LastHouseholdTick",now);
        h.state.putLong("ActiveTicks",h.clock()+20);if(h.active()){TycoonPlacements.tick(p);TycoonAgency.tick(p);var housing=survey(p);if(housing.approved()){h.milestone("basic_housing",25);if(housing.capacity()>=2)h.milestone("capacity_two",25);if(housing.capacity()>=4)h.milestone("capacity_four",50);h.state.putInt("UnlockedTier",Math.max(h.state.getInt("UnlockedTier"),housing.capacity()>=4?3:housing.capacity()>=2?2:1));}}TycoonData.get(p.server).setDirty();
    }
    private static boolean region(ServerPlayer p,net.minecraft.core.BlockPos at){var h=TycoonData.get(p.server).homes.get(TycoonWorld.owner(p));return h!=null&&h.active()&&TycoonLandscape.owned(h,at);}
    @SubscribeEvent public static void building(BlockEvent.EntityPlaceEvent e){
        if(e.getEntity() instanceof ServerPlayer p&&p.serverLevel().dimension().equals(TycoonWorld.KEY)){
            if(TycoonRetiredParcels.ordinaryAccess(p,e.getPos()))return;
            var h=TycoonCollaborationVisits.homeAt(p,e.getPos()).orElse(null);if(h==null){e.setCanceled(true);return;}
            TycoonScope.run(p,h.owner,()->buildingOwned(e));
        }
    }
    private static void buildingOwned(BlockEvent.EntityPlaceEvent e){if(e.getEntity() instanceof ServerPlayer p&&p.serverLevel().dimension().equals(TycoonWorld.KEY)){if(TycoonWorld.busy(p)||!region(p,e.getPos())||TycoonBuildJob.touches(TycoonWorld.home(p),e.getPos())){e.setCanceled(true);return;}TycoonPhaseTwo.edit(p,e.getPos(),e.getPlacedBlock());}}
    @SubscribeEvent public static void breaking(BlockEvent.BreakEvent e){
        if(!(e.getPlayer() instanceof ServerPlayer p))return;
        if(TycoonRetiredParcels.ordinaryAccess(p,e.getPos()))return;
        if(p.serverLevel().dimension()!=TycoonWorld.KEY&&!(e.getLevel().getBlockEntity(e.getPos()) instanceof TycoonBlockEntity)){breakingOwned(e);return;}
        var owner=p.serverLevel().dimension().equals(TycoonWorld.KEY)?TycoonCollaborationVisits.homeAt(p,e.getPos()).map(h->h.owner).orElse(null):p.serverLevel().getBlockEntity(e.getPos()) instanceof TycoonBlockEntity be?be.owner:p.getUUID();
        if(owner==null||!TycoonCollaborators.allowed(p,owner)){e.setCanceled(true);return;}
        if(owner.equals(p.getUUID()))TycoonData.get(p.server).home(owner);
        TycoonScope.run(p,owner,()->breakingOwned(e));
    }
    private static void breakingOwned(BlockEvent.BreakEvent e){
        if(!(e.getPlayer() instanceof ServerPlayer p))return;
        boolean personal=p.serverLevel().dimension().equals(TycoonWorld.KEY);
        if((personal||e.getState().getBlock() instanceof TycoonBlock)&&TycoonWorld.busy(p)){e.setCanceled(true);return;}
        if(personal&&(!region(p,e.getPos())||TycoonBuildJob.touches(TycoonWorld.home(p),e.getPos()))){e.setCanceled(true);return;}
        if(!(e.getState().getBlock() instanceof TycoonBlock b)){
            if(!personal||!TycoonSoulFarms.edited(p,e.getPos()))TycoonPhaseTwo.edit(p,e.getPos(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());return;
        }
        var be=e.getLevel().getBlockEntity(e.getPos());if(be instanceof TycoonBlockEntity t&&!TycoonWorld.owner(p).equals(t.owner)){e.setCanceled(true);return;}
        if(b==TycoonContent.DOOR.get()){e.setCanceled(true);TycoonWorld.pickup(p,e.getPos());return;}
        if(b==TycoonContent.DESK.get()||b==TycoonContent.FOUNDATION.get()){e.setCanceled(true);return;}
        if(be instanceof TycoonBlockEntity t){e.setCanceled(true);if(p.getInventory().getFreeSlot()<0){TycoonWorld.message(p,"Free a slot before moving owned furniture.");return;}var item=TycoonWorld.bound(p,b.kind);if(TycoonWorld.deliver(p,item))p.serverLevel().removeBlock(e.getPos(),false);}
    }
    @SubscribeEvent public static void destroyed(net.minecraftforge.event.entity.EntityLeaveLevelEvent e){if(e.getLevel().isClientSide||!(e.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity entity)||entity.getItem().isEmpty()||!entity.getItem().is(TycoonContent.DOOR.get().asItem())||entity.getRemovalReason()!=net.minecraft.world.entity.Entity.RemovalReason.KILLED)return;var tag=entity.getItem().getTag();if(tag==null||!tag.hasUUID("TycoonOwner")||!tag.hasUUID("DoorToken"))return;var data=TycoonData.get(e.getLevel().getServer());var h=data.homes.get(tag.getUUID("TycoonOwner"));if(h!=null&&h.entrance()==null&&h.state.hasUUID("DoorToken")&&tag.getUUID("DoorToken").equals(h.state.getUUID("DoorToken"))){h.state.putBoolean("DoorLost",true);data.setDirty();}}
    @SubscribeEvent public static void pickup(EntityItemPickupEvent e){if(e.getEntity() instanceof ServerPlayer p)TycoonCare.ballPickup(p,e.getItem());}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){TycoonNetwork.clear(e.getEntity().getUUID());ROOMS.remove(e.getEntity().getUUID());if(e.getEntity() instanceof ServerPlayer p){var h=TycoonData.get(p.server).homes.get(p.getUUID());if(h!=null){h.state.remove("ActivationStart");TycoonData.get(p.server).setDirty();}}}
    @SubscribeEvent public static void stop(net.minecraftforge.event.server.ServerStoppedEvent e){ROOMS.clear();TycoonNetwork.clear();}
}
