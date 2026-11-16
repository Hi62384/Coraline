package com.coraline.mixin;
import com.coraline.feast.PartyStorageGuard;
import net.minecraft.world.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(CompoundContainer.class)
public interface PartyCompoundContainerMixin extends PartyStorageGuard.CompoundParts {
    @Accessor("container1") Container coraline$first();
    @Accessor("container2") Container coraline$second();
}
