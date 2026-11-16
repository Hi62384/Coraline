package com.coraline.tycoon;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Physical chamber inspection and emergency controls; no ordinary anchor use. */
@Mod.EventBusSubscriber(modid="coraline")
public final class TycoonSoulInteractions {
    private static boolean within(CompoundTag n,BlockPos at){var c=TycoonAttractions.origin(n);return Math.abs(c.getX()-at.getX())<=6&&Math.abs(c.getZ()-at.getZ())<=6&&at.getY()>=c.getY()&&at.getY()<=c.getY()+5;}
    @SubscribeEvent public static void interact(PlayerInteractEvent.RightClickBlock e){
        if(e.getLevel().dimension()!=TycoonWorld.KEY||e.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND||!(e.getEntity() instanceof ServerPlayer p))return;
        var data=TycoonData.get(p.server);for(var h:data.homes.values()){
            if(Math.abs(h.center().getX()-e.getPos().getX())>128||Math.abs(h.center().getZ()-e.getPos().getZ())>128)continue;
            for(String key:TycoonSoulFarms.farms(h).getAllKeys()){
                var farm=TycoonSoulFarms.farms(h).getCompound(key);if(!within(farm,e.getPos()))continue;
                e.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);e.setCanceled(true);e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
                if(!TycoonCollaborators.allowed(p,h.owner)){TycoonWorld.message(p,"This machinery belongs to another world owner.");return;}
                TycoonScope.run(p,h.owner,()->control(p,e,h,farm));return;
            }
        }
    }
    private static void control(ServerPlayer p,PlayerInteractEvent.RightClickBlock e,TycoonData.Home h,CompoundTag farm){
        if(TycoonWorld.busy(p))return;
                if(e.getPos().equals(TycoonSoulFarms.local(farm,-4,1,-2))){TycoonWorld.message(p,TycoonSoulFarms.breaching(farm)?TycoonSoulFarms.intervene(p,farm.getUUID("Id"),false):TycoonSoulFarms.pause(p,farm.getUUID("Id")));return;}
                var souls=TycoonSoulFarms.assigned(h,farm.getUUID("Id"));for(int i=0;i<farm.getInt("Tier");i++)if(e.getPos().equals(TycoonSoulFarms.local(farm,-4+i*2,2,4))){
                    if(i>=souls.size()){TycoonWorld.message(p,"Empty containment chamber.");return;}var n=souls.get(i);TycoonWorld.message(p,n.getString("Name")+" — "+n.getString("Primary")+" / "+n.getString("Secondary")+String.format(java.util.Locale.ROOT," — base %.4f Essence/s; stability %.2f; resonance %.2f; efficiency %.2f",n.getLong("BaseMicros")/1_000_000.,n.getDouble("Stability"),n.getDouble("Resonance"),n.getDouble("Efficiency")));return;
                }
                TycoonWorld.message(p,"Soul Farm "+farm.getInt("Ordinal")+" — "+farm.getString("State")+" — use the lodestone control to secure containment / pause production.");return;
    }
    @SubscribeEvent public static void breakBlock(BlockEvent.BreakEvent e){if(e.getPlayer() instanceof ServerPlayer p&&p.serverLevel().dimension()==TycoonWorld.KEY&&!e.isCanceled())TycoonCollaborationVisits.homeAt(p,e.getPos()).ifPresent(h->TycoonScope.run(p,h.owner,()->TycoonSoulFarms.edited(p,e.getPos())));}
    @SubscribeEvent public static void explosion(ExplosionEvent.Detonate e){
        if(e.getLevel().isClientSide||e.getLevel().dimension()!=TycoonWorld.KEY)return;var data=TycoonData.get(e.getLevel().getServer());boolean changed=false;
        for(var h:data.homes.values())for(String key:TycoonSoulFarms.farms(h).getAllKeys()){
            var farm=TycoonSoulFarms.farms(h).getCompound(key);var cells=farm.getCompound("DamageCells");for(var at:e.getAffectedBlocks())if(within(farm,at)){farm.putBoolean("Damaged",true);farm.putBoolean("Verified",false);cells.putBoolean(Long.toString(at.asLong()),true);changed=true;}farm.put("DamageCells",cells);
        }if(changed)data.setDirty();
    }
    private TycoonSoulInteractions(){}
}
