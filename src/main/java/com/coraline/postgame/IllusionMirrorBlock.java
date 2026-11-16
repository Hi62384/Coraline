package com.coraline.postgame;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;

/** A generated false mirror: visually present, but never a collision barrier.
 * Subtle motes distinguish its passable surface from the solid tower core. */
public final class IllusionMirrorBlock extends GlassBlock {
    public IllusionMirrorBlock(){super(Properties.copy(Blocks.GLASS).noCollission().noLootTable());}
    @Override public void animateTick(net.minecraft.world.level.block.state.BlockState state,Level level,BlockPos at,RandomSource random){
        if(random.nextInt(6)==0)level.addParticle(net.minecraft.core.particles.ParticleTypes.ENCHANT,at.getX()+random.nextDouble(),at.getY()+random.nextDouble(),at.getZ()+random.nextDouble(),0,.015,0);
    }
}
