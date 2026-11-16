package com.coraline.tycoon;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Phase 2 orchestration reuses Phase 1 owners, clocks, residents and network. */
@Mod.EventBusSubscriber(modid="coraline")
public final class TycoonPhaseTwo {
    @SubscribeEvent public static void player(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer p)||p.tickCount%20!=0)return;
        var h=TycoonData.get(p.server).homes.get(p.getUUID());if(h==null||!h.active())return;
        tickHome(p,h);
    }
    static void tickHome(ServerPlayer p,TycoonData.Home h){
        long now=p.server.overworld().getGameTime();if(h.state.contains("LastAutomationTick")&&now>=h.state.getLong("LastAutomationTick")&&now-h.state.getLong("LastAutomationTick")<20)return;
        TycoonTransactions.reconcile(p);TycoonPlacements.reconcile(p);if(TycoonWorld.busy(p))return;
        h.state.putLong("LastAutomationTick",now);
        TycoonTransactions.reconcile(p);TycoonBuildJob.tick(p);TycoonVisits.tick(p);
        TycoonSoulFarms.tick(p);
        TycoonWorkers.tick(p);TycoonMachines.tick(p);
        if(!h.state.getBoolean("VisitActive")&&h.state.contains("VisitWarmUntil")&&h.clock()>h.state.getLong("VisitWarmUntil")){TycoonVisitTickets.release(p.server,h);h.state.remove("VisitWarmUntil");}
        if(Math.floorMod(now/20,5)==0){TycoonDolls.tick(p);TycoonDollAnalysis.tick(p);TycoonAttractions.tick(p);}
        TycoonSpies.tick(p);
        if(!h.state.contains("WorldGoodness"))TycoonLandscape.recalculate(h);
        sync(p);
    }
    public static void sync(ServerPlayer p){var visit=TycoonCollaborationVisits.visitingOwner(p);if(visit.isPresent()&&!visit.get().equals(TycoonWorld.owner(p))){TycoonScope.run(p,visit.get(),()->sync(p));return;}if(p.connection==null||p.serverLevel().dimension()!=TycoonWorld.KEY)return;var h=TycoonWorld.home(p);var n=new CompoundTag();n.putString("Mode","world_visuals");n.putLong("Center",h.center().asLong());n.putInt("SkyTier",h.state.getInt("SkyTier"));n.put("SpyMarkers",TycoonSpies.markers(h));TycoonNetwork.snapshot(p,n);}
    public static void edit(ServerPlayer p,BlockPos at,net.minecraft.world.level.block.state.BlockState replacement){
        if(p.serverLevel().dimension()!=TycoonWorld.KEY)return;var h=TycoonWorld.home(p);if(!TycoonLandscape.owned(h,at))return;
        var edits=h.state.getCompound("PlayerEdits");edits.putBoolean(Long.toString(at.asLong()),true);h.state.put("PlayerEdits",edits);
        var tile=h.state.getCompound("Terrain").getCompound(TycoonLandscape.key(TycoonLandscape.tileX(h,at),TycoonLandscape.tileZ(h,at)));var generated=TycoonLandscape.decode(p.serverLevel(),tile);var expected=generated.get(at);var damage=h.state.getCompound("SceneryDamage");
        if(expected!=null){if(expected.equals(replacement)){damage.remove(Long.toString(at.asLong()));edits.remove(Long.toString(at.asLong()));}else damage.putBoolean(Long.toString(at.asLong()),true);}else if(replacement.isAir())edits.remove(Long.toString(at.asLong()));h.state.put("SceneryDamage",damage);TycoonLandscape.recalculate(h);TycoonWorld.dirty(p);
    }
    @SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event){TycoonBuildJob.clear(event.getEntity().getUUID());}
    private TycoonPhaseTwo(){}
}
