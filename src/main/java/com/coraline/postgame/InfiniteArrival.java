package com.coraline.postgame;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

/** One bounded asynchronous chunk request, followed by a small arrival frame.
 * A planned/partially built frame is reconciled in place, never moved or
 * duplicated on reload. Existing player blocks are never cleared. */
final class InfiniteArrival {
    private static final Map<MinecraftServer,Job> JOBS=new WeakHashMap<>();
    private static final class Job {boolean working,stopping;CompletableFuture<?> future;String problem="";int garden,candidateFailures;long demandUntil,gardenRetry,arrivalRetry,searchRestartAt;}
    private static Job job(MinecraftServer server){return JOBS.computeIfAbsent(server,s->new Job());}
    static void request(ServerLevel level){job(level.getServer()).demandUntil=level.getServer().overworld().getGameTime()+200;}
    static String blocker(MinecraftServer server){return job(server).problem;}
    static Map<BlockPos,BlockState> frame(BlockPos floor){
        var out=new LinkedHashMap<BlockPos,BlockState>();var stone=InfiniteContent.block("mirrorstone").defaultBlockState();var wood=InfiniteContent.block("buttonwood_log").defaultBlockState();
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)out.put(floor.offset(x,0,z),stone);
        for(int x:new int[]{-1,1})for(int y=1;y<=3;y++)out.put(floor.offset(x,y,0),wood);
        out.put(floor.above(3),com.coraline.registry.ModBlocks.SOULGLASS_BLOCK.get().defaultBlockState());
        out.put(floor.above(),com.coraline.registry.ModBlocks.INFINITE_RETURN_PORTAL.get().defaultBlockState());out.put(floor.above(2),com.coraline.registry.ModBlocks.INFINITE_RETURN_PORTAL.get().defaultBlockState());return out;
    }
    private static boolean clear(ServerLevel level,BlockPos floor){
        if(floor.getY()<level.getMinBuildHeight()+1||floor.getY()+4>=level.getMaxBuildHeight())return false;
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            var column=floor.offset(x,0,z);if(!level.getWorldBorder().isWithinBounds(column)||!level.getBlockState(column.below()).isFaceSturdy(level,column.below(),Direction.UP))return false;
            for(int y=0;y<=3;y++){var at=column.above(y);if(!level.isEmptyBlock(at)||!level.getFluidState(at).isEmpty()||level.getBlockEntity(at)!=null)return false;}
        }return true;
    }
    static boolean ready(ServerLevel level,InfiniteTransitData data){
        if(!data.built()||data.anchor().isEmpty()||!level.hasChunkAt(data.anchor().orElseThrow()))return false;
        for(var cell:frame(data.anchor().orElseThrow()).entrySet())if(!level.getBlockState(cell.getKey()).equals(cell.getValue()))return false;return true;
    }
    private static void load(ServerLevel level,BlockPos at,Job job,Runnable loaded,Runnable failed){
        if(job.working||job.stopping)return;if(level.hasChunkAt(at)){loaded.run();return;}job.working=true;
        job.future=level.getChunkSource().getChunkFuture(at.getX()>>4,at.getZ()>>4,ChunkStatus.FULL,true);
        job.future.whenComplete((result,error)->level.getServer().execute(()->{
            job.working=false;if(job.stopping)return;
            if(error!=null||!level.hasChunkAt(at)){failed.run();return;}loaded.run();
        }));
    }
    private static void reconcile(ServerLevel level,InfiniteTransitData data,Job job,BlockPos at){
        var frame=frame(at);
        boolean repair=false;
        for(var cell:frame.entrySet()){
            var state=level.getBlockState(cell.getKey());if(state.equals(cell.getValue()))continue;
            repair=true;
            if(!state.isAir()||!level.getFluidState(cell.getKey()).isEmpty()||level.getBlockEntity(cell.getKey())!=null){job.problem="The saved arrival frame is obstructed";job.arrivalRetry=level.getServer().overworld().getGameTime()+40;return;}
        }
        // The reserved walking cells are checked as well as the frame itself.
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=1;y<=3;y++){
            var cell=at.offset(x,y,z);if(frame.containsKey(cell))continue;if(!level.isEmptyBlock(cell)||!level.getFluidState(cell).isEmpty()){job.problem="The arrival walking space is obstructed";job.arrivalRetry=level.getServer().overworld().getGameTime()+40;return;}
        }
        if(!data.built()||repair){
            for(var cell:frame.entrySet())if(!level.getBlockState(cell.getKey()).equals(cell.getValue()))level.setBlock(cell.getKey(),cell.getValue(),3);
            for(var cell:frame.entrySet())if(!level.getBlockState(cell.getKey()).equals(cell.getValue())){job.problem="Arrival frame placement was refused";return;}
            if(!level.getServer().saveEverything(true,true,true)||!data.built()&&!data.built(level.getServer()))job.problem="Arrival frame save is paused";
        }
    }
    static void tick(ServerLevel level){
        var server=level.getServer();var identity=InfiniteRealm.identity(level);if(identity.isEmpty())return;var data=InfiniteTransitData.get(server,identity.orElseThrow());var job=job(server);
        if(job.stopping||job.working||!data.problem().isEmpty())return;
        // Preparing a new frame is allowed once. A finished frame is loaded
        // only for a nearby entrant, never by a permanent forced-chunk loop.
        long now=server.overworld().getGameTime();
        boolean needed=!data.built()||now<=job.demandUntil;
        if(!job.problem.isEmpty()&&now>=job.arrivalRetry)job.problem="";
        if(needed&&data.anchor().isEmpty()&&data.candidate()==InfiniteTransitData.ARRIVAL_CANDIDATES){
            if(job.searchRestartAt==0)job.searchRestartAt=now+200;
            else if(now>=job.searchRestartAt){data.restartCandidates();job.searchRestartAt=0;}
        }
        if(needed&&job.problem.isEmpty()&&now>=job.arrivalRetry){
            job.arrivalRetry=now+5;
            if(data.anchor().isPresent()){
                var anchor=data.anchor().orElseThrow();
                load(level,anchor,job,()->reconcile(level,data,job,anchor),()->job.arrivalRetry=server.overworld().getGameTime()+40);
            }
            else if(data.candidate()<InfiniteTransitData.ARRIVAL_CANDIDATES){
                var binding=((InfiniteChunkGenerator)level.getChunkSource().getGenerator()).structureGeneration();int i=data.candidate();
                int x=(i%16-8)*64+8+(int)((binding.seed()>>>8)&15)*16;
                int z=(i/16-4)*64+8+(int)((binding.seed()>>>24)&15)*16;
                var column=binding.terrain().column(x,z);
                if(column.surfaceY()<InfiniteTerrain.SEA_LEVEL+3||column.biome()==InfiniteTerrain.Biome.WHITE_RIFT_WASTES){data.rejectCandidate();return;}
                var at=new BlockPos(x,column.surfaceY()+1,z);
                load(level,at,job,()->{
                    job.candidateFailures=0;
                    if(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)!=at.getY()||!clear(level,at)){data.rejectCandidate();return;}
                    if(data.plan(server,at))reconcile(level,data,job,at);else job.problem="Arrival construction save is paused";
                },()->{
                    job.arrivalRetry=server.overworld().getGameTime()+40;
                    if(++job.candidateFailures==3){data.rejectCandidate();job.candidateFailures=0;}
                });
            }
        }
        if(job.working)return;
        BreachDriveController.ensure(level);
        if(!BreachWorldData.get(server).view().blocker().isEmpty())return;
        if(!BreachWorldData.get(server).view().active()&&server.overworld().getGameTime()>=job.gardenRetry){
            var sites=com.coraline.palace.PalaceSiteData.get(server);var palace=sites.position(com.coraline.palace.PalaceSitePlan.Kind.PALACE);
            if(palace.isPresent()&&sites.generated(com.coraline.palace.PalaceSitePlan.Kind.PALACE)){
                var receipt=BreachWorldData.get(server).drivePosition();BlockPos garden=receipt.filter(at->!BreachDriveController.obstructed(server.overworld(),at))
                        .orElseGet(()->BreachDriveController.candidate(palace.orElseThrow(),job.garden));
                load(server.overworld(),garden,job,()->BreachDriveController.ensure(level),()->job.gardenRetry=server.overworld().getGameTime()+40);job.garden++;
                if(job.garden>=BreachDriveController.CANDIDATES){job.garden=0;job.gardenRetry=server.overworld().getGameTime()+200;}
            }
        }
        InfinitePalacePortal.install(server,data,identity.orElseThrow());
    }
    static boolean reserved(ServerLevel level,BlockPos at){
        var id=InfiniteRealm.identity(level);if(id.isEmpty())return false;var anchor=InfiniteTransitData.get(level.getServer(),id.orElseThrow()).anchor();
        return anchor.isPresent()&&Math.abs(at.getX()-anchor.orElseThrow().getX())<=1&&Math.abs(at.getZ()-anchor.orElseThrow().getZ())<=1&&at.getY()>=anchor.orElseThrow().getY()&&at.getY()<=anchor.orElseThrow().getY()+3;
    }
    static void stopping(MinecraftServer server){var job=JOBS.remove(server);if(job!=null){job.stopping=true;job.future=null;}}
    private InfiniteArrival(){}
}
