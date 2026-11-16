package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.Heightmap;

/** Feature-stage adapter. Never accesses neighboring chunks or live builds. */
final class InfiniteVegetation {
    static int decorate(ChunkAccess access,InfiniteGeneration generation,InfiniteChunkTerrain.Palette palette) {
        return decorate(access,generation,palette,List.of());
    }
    static int decorate(ChunkAccess access,InfiniteGeneration generation,InfiniteChunkTerrain.Palette palette,List<net.minecraft.world.level.levelgen.structure.BoundingBox> protectedSites) {
        // ImposterProtoChunk wraps a live LevelChunk. It must be rejected too.
        if(access.getClass()!=ProtoChunk.class || !access.getStatus().isOrAfter(ChunkStatus.NOISE) || access.getStatus().isOrAfter(ChunkStatus.FEATURES)) return 0;
        if(access.getMinBuildHeight()!=InfiniteTerrain.MIN_Y || access.getHeight()!=InfiniteTerrain.HEIGHT) return 0;
        var pos=access.getPos(); int minX=pos.getMinBlockX(), minZ=pos.getMinBlockZ(), placed=0;
        var at=new BlockPos.MutableBlockPos();
        for(var plant:new InfiniteVegetationPlan(generation.seed()).forChunk(pos.x,pos.z)) {
            if(plant.x()>=minX && plant.x()<=minX+15 && plant.z()>=minZ && plant.z()<=minZ+15) {
                var column=generation.terrain().column(plant.x(),plant.z());
                if(!access.getBlockState(at.set(plant.x(),plant.y()-1,plant.z())).equals(palette.strata(column.biome()).surface())) continue;
                boolean clear=true;
                for(int y=plant.y();y<plant.y()+plant.height();y++)
                    if(!access.getBlockState(at.set(plant.x(),y,plant.z())).isAir()) { clear=false; break; }
                if(!clear) continue;
            }
            var stem=stem(plant.kind()); var crown=crown(plant.kind());
            for(int x=Math.max(minX,plant.x()-plant.radius());x<=Math.min(minX+15,plant.x()+plant.radius());x++)
                for(int z=Math.max(minZ,plant.z()-plant.radius());z<=Math.min(minZ+15,plant.z()+plant.radius());z++)
                    for(int y=plant.y();y<=plant.y()+plant.height()+1;y++) {
                        var part=InfiniteVegetationPlan.partAt(plant,x,y,z);
                        if(part==InfiniteVegetationPlan.Part.NONE || !access.getBlockState(at.set(x,y,z)).isAir()) continue;
                        // Room air and doorway clearance are functional space,
                        // even though they look replaceable to a foliage pass.
                        if(protectedSites.stream().anyMatch(box->box.isInside(at)))continue;
                        var state=part==InfiniteVegetationPlan.Part.STEM?stem:crown;
                        if(state.hasProperty(LeavesBlock.DISTANCE)) {
                            int distance=Math.abs(x-plant.x())+Math.abs(z-plant.z())+Math.max(0,y-(plant.y()+plant.height()-1));
                            state=state.setValue(LeavesBlock.DISTANCE,Math.max(1,Math.min(7,distance)));
                        }
                        access.setBlockState(at,state,false); placed++;
                    }
        }
        if(placed>0) {
            Heightmap.primeHeightmaps(access,EnumSet.of(Heightmap.Types.WORLD_SURFACE,Heightmap.Types.OCEAN_FLOOR,Heightmap.Types.MOTION_BLOCKING,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES));
            access.setUnsaved(true);
        }
        return placed;
    }
    private static BlockState stem(InfiniteVegetationPlan.Kind kind) {
        return InfiniteContent.block(switch(kind) {
            case BUTTONWOOD -> "buttonwood_log"; case PAPERWOOD -> "paperwood_log";
            case LANTERNWOOD -> "lanternwood_log"; case VELVET_MUSHROOM -> "velvet_mushroom_stem";
        }).defaultBlockState();
    }
    private static BlockState crown(InfiniteVegetationPlan.Kind kind) {
        var state=InfiniteContent.block(switch(kind) {
            case BUTTONWOOD -> "buttonwood_leaves"; case PAPERWOOD -> "paperwood_leaves";
            case LANTERNWOOD -> "lanternwood_leaves"; case VELVET_MUSHROOM -> "velvet_mushroom_block";
        }).defaultBlockState();
        return state;
    }
    private InfiniteVegetation() {}
}
