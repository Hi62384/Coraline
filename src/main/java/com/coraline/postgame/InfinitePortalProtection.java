package com.coraline.postgame;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Reserve only the transit frame/walking space and canonical active portals.
 * The rest of the generated exploration world remains ordinarily editable. */
@Mod.EventBusSubscriber(modid="coraline")
public final class InfinitePortalProtection {
    private static boolean door(ServerLevel level,BlockPos at){return level.getBlockEntity(at) instanceof com.coraline.otherworld.DoorBlockEntity d&&d.hasPostgameBinding();}
    private static boolean well(ServerLevel level,BlockPos at,boolean placing){
        if(level!=level.getServer().overworld()||!InfiniteWorldData.get(level.getServer()).active())return false;
        var well=InfiniteRealm.well(level.getServer());if(well.isEmpty())return false;var center=well.orElseThrow();int x=at.getX()-center.getX(),z=at.getZ()-center.getZ(),y=at.getY()-center.getY();double radius=Math.hypot(x,z);
        if(y<0||y>2||radius>3.2)return false;
        if(placing&&radius<1.8)return true;
        return radius>=1.8&&(level.getBlockState(at).is(Blocks.MOSSY_COBBLESTONE)||level.getBlockState(at).is(Blocks.SPRUCE_FENCE));
    }
    private static boolean protectedAt(ServerLevel level,BlockPos at,boolean placing){return door(level,at)||InfiniteArrival.reserved(level,at)||well(level,at,placing);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void breakBlock(BlockEvent.BreakEvent event){if(event.getLevel() instanceof ServerLevel level&&protectedAt(level,event.getPos(),false)){event.setCanceled(true);event.getPlayer().displayClientMessage(net.minecraft.network.chat.Component.translatable("message.coraline.infinite.portal_reserved"),true);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void place(BlockEvent.EntityPlaceEvent event){if(event.getLevel() instanceof ServerLevel level&&protectedAt(level,event.getPos(),true))event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void bucket(net.minecraftforge.event.entity.player.FillBucketEvent event){if(event.getLevel() instanceof ServerLevel level&&event.getTarget() instanceof net.minecraft.world.phys.BlockHitResult hit&&(protectedAt(level,hit.getBlockPos(),true)||protectedAt(level,hit.getBlockPos().relative(hit.getDirection()),true)))event.setCanceled(true);}
    @SubscribeEvent public static void explosion(net.minecraftforge.event.level.ExplosionEvent.Detonate event){if(event.getLevel() instanceof ServerLevel level)event.getAffectedBlocks().removeIf(at->protectedAt(level,at,false));}
    private InfinitePortalProtection(){}
}
