package com.coraline.otherworld;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockSetType;
/** Vanilla paired-door interaction/collision, with framed glass native models. */
public final class RosewoodGlassDoorBlock extends DoorBlock {
    public RosewoodGlassDoorBlock(){super(Properties.copy(Blocks.OAK_DOOR).noOcclusion().sound(SoundType.GLASS),BlockSetType.OAK);}
}
