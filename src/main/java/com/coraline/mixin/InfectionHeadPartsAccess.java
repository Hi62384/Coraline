package com.coraline.mixin;

import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AgeableListModel.class)
public interface InfectionHeadPartsAccess {
    @Invoker("headParts") Iterable<ModelPart> coraline$infectionHeads();
}
