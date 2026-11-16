package com.coraline.mixin;

import java.util.*;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ModelPart.class)
public interface InfectionModelPartAccess {
    @Accessor("cubes") List<ModelPart.Cube> coraline$infectionCubes();
    @Accessor("children") Map<String,ModelPart> coraline$infectionChildren();
}
