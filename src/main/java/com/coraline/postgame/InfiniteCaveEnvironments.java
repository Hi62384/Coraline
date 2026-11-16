package com.coraline.postgame;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Noise-stage cave materials and sparse supported formations. All samples
 * use absolute coordinates, never live neighboring chunks or mutable RNGs. */
final class InfiniteCaveEnvironments {
    static BlockState stone(InfiniteTerrain terrain,int x,int y,int z,InfiniteTerrain.Column column,BlockState ordinary){
        if(y>=column.surfaceY()-8)return ordinary;
        return switch(terrain.caveEnvironment(x,y,z,column)){
            case PORCELAIN->InfiniteContent.block("porcelain_stone").defaultBlockState();
            case SOUL_GLASS->Math.floorMod(terrain.caveDetail(x,y,z),97)==0?Blocks.TINTED_GLASS.defaultBlockState():InfiniteContent.block("porcelain_stone").defaultBlockState();
            case THREADROOT->InfiniteContent.block("threadbound_rock").defaultBlockState();
            case CLOCKWORK->InfiniteContent.block("clockwork_stone").defaultBlockState();
            case MIRROR->InfiniteContent.block("mirrorstone").defaultBlockState();
            case WHITE_RIFT->InfiniteContent.block("pale_rift_stone").defaultBlockState();
        };
    }
    static boolean flooded(InfiniteTerrain terrain,int x,int y,int z,InfiniteTerrain.Column column){
        if(terrain.caveEnvironment(x,y,z,column)==InfiniteTerrain.Cave.MIRROR)return y<=24;
        // Deep lakes occupy selected horizontal regions, leaving most deep
        // passages dry; Soul Glass remains the existing rare ore vein.
        return y<=-36&&Math.floorMod(terrain.caveDetail(Math.floorDiv(x,96),0,Math.floorDiv(z,96)),5)==0;
    }
    static BlockState formation(InfiniteTerrain terrain,int x,int y,int z,InfiniteTerrain.Column column){
        if(flooded(terrain,x,y,z,column)||y>=column.surfaceY()-8)return null;long detail=terrain.caveDetail(x,y,z);
        // Leave the great majority of passage width open. One noise query for
        // support is performed only at sparse candidate cells.
        if(Math.floorMod(detail,97)!=0)return null;
        boolean floor=!terrain.cave(x,y-1,z,column),ceiling=!terrain.cave(x,y+1,z,column);
        if(!floor&&!ceiling)return null;
        return switch(terrain.caveEnvironment(x,y,z,column)){
            case PORCELAIN->Blocks.CALCITE.defaultBlockState();
            case THREADROOT->floor?Blocks.MOSS_BLOCK.defaultBlockState():Blocks.HANGING_ROOTS.defaultBlockState();
            case CLOCKWORK->Blocks.CUT_COPPER.defaultBlockState();
            case MIRROR->InfiniteContent.ILLUSION_MIRROR.get().defaultBlockState();
            case WHITE_RIFT->InfiniteContent.block("pale_rift_stone").defaultBlockState();
            case SOUL_GLASS->null;
        };
    }
    private InfiniteCaveEnvironments(){}
}
