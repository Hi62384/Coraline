package com.coraline.otherworld;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Authored structure writes must not break neighbouring vegetation/paired decor mid-build. */
final class GenerationBlocks {
    static boolean set(Level level,BlockPos at,BlockState state,int flags){
        return level.setBlock(at,state,(flags&~Block.UPDATE_NEIGHBORS)|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS);
    }
    private GenerationBlocks(){}
}
