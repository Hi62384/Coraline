package com.coraline.postgame;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;

/** Terrain variants never yield a second copy of the remembered original. */
public final class InfectionBlock extends Block {
    private final boolean heart;
    public InfectionBlock(boolean heart){super(BlockBehaviour.Properties.copy(heart?Blocks.OBSIDIAN:Blocks.STONE).strength(heart?5:1.8F).noLootTable().pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK).lightLevel(s->heart?7:0));this.heart=heart;}
    @Override public void onRemove(BlockState state,Level level,BlockPos at,BlockState replacement,boolean moving){if(heart&&!state.is(replacement.getBlock())&&level instanceof net.minecraft.server.level.ServerLevel server)InfectionColonies.sourceRemoved(server,at);super.onRemove(state,level,at,replacement,moving);}
}
