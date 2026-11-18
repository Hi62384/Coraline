package com.coraline.postgame;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Bounded installation in the canonical Palace garden. No chunk tickets,
 * terrain replacement, second apparatus or per-player Breach allocation. */
@Mod.EventBusSubscriber(modid="coraline")
public final class BreachDriveController {
    static final int CANDIDATES=63;
    static BlockPos candidate(BlockPos palace,int index){
        if(index<9)return palace.offset(28+4*(index/3),0,-40+4*(index%3));
        int extra=index-9;
        return palace.offset(17+6*(extra/9),0,-48+4*(extra%9));
    }
    static boolean clear(ServerLevel level,BlockPos at){return level.hasChunkAt(at)&&at.getY()>level.getMinBuildHeight()&&at.getY()<level.getMaxBuildHeight()-1&&level.getWorldBorder().isWithinBounds(at)&&level.isEmptyBlock(at)&&level.getBlockEntity(at)==null&&level.getFluidState(at).isEmpty()&&level.getBlockState(at.below()).isFaceSturdy(level,at.below(),net.minecraft.core.Direction.UP);}
    static boolean obstructed(ServerLevel level,BlockPos at){
        return level.hasChunkAt(at)&&!clear(level,at)
                &&!level.getBlockState(at).is(com.coraline.registry.ModBlocks.BREACH_DRIVE.get())
                &&!(level.getBlockEntity(at) instanceof BreachDriveBlockEntity);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;var server=event.getServer();if(server.getTickCount()%20!=0)return;
        ServerLevel bound=null;
        for(var level:server.getAllLevels())if(level.getChunkSource().getGenerator() instanceof InfiniteChunkGenerator generator){try{generator.structureGeneration();bound=level;break;}catch(IllegalStateException unavailable){}}
        if(bound!=null)ensure(bound);
    }
    static void ensure(ServerLevel bound){
        var server=bound.getServer();var data=BreachWorldData.get(server);if(!data.view().blocker().isEmpty())return;
        var overworld=server.overworld();var sites=com.coraline.palace.PalaceSiteData.get(server);
        var original=data.drivePosition().orElse(null);
        boolean recover=original!=null&&!data.view().active()&&obstructed(overworld,original);
        if(!data.hasDriveReceipt()||recover){
            if(!sites.ready()||!sites.generated(com.coraline.palace.PalaceSitePlan.Kind.PALACE))return;var palace=sites.position(com.coraline.palace.PalaceSitePlan.Kind.PALACE).orElseThrow();
            for(int i=0;i<CANDIDATES;i++){
                var column=candidate(palace,i);
                if(!overworld.hasChunkAt(column))continue;
                int x=column.getX(),z=column.getZ();
                var at=new BlockPos(x,overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z),z);
                if(!data.view().active()&&clear(overworld,at)){data.claimDrive(bound,at);break;}
            }
        }
        if(!data.view().blocker().isEmpty())return;var position=data.drivePosition();if(position.isEmpty())return;var at=position.orElseThrow();
        if(overworld.hasChunkAt(at)){
            boolean installed=false;
            if(clear(overworld,at))installed=overworld.setBlock(at,com.coraline.registry.ModBlocks.BREACH_DRIVE.get().defaultBlockState(),3);
            if((installed||!data.view().active())&&overworld.getBlockEntity(at) instanceof BreachDriveBlockEntity device){
                device.configure(data.driveId().orElseThrow(),data.driveGeneration().orElseThrow());
            }
            if(overworld.getBlockEntity(at) instanceof BreachDriveBlockEntity device&&data.matchesDrive(overworld,at,device)&&!data.view().active())data.activateFromBoundLevel(bound);
        }
        if(data.view().active()){
            data.advanceFromBoundLevel(bound);data.consumeInput(bound);
            if(overworld.hasChunkAt(at)&&overworld.getBlockEntity(at) instanceof BreachDriveBlockEntity device&&data.matchesDrive(overworld,at,device)){
                boolean powered=data.view().fuelTicks()>0;var state=overworld.getBlockState(at);
                if(state.getValue(BreachDriveBlock.POWERED)!=powered)overworld.setBlock(at,state.setValue(BreachDriveBlock.POWERED,powered),3);
            }
        }
    }
    private BreachDriveController(){}
}
