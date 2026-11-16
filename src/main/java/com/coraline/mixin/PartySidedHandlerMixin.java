package com.coraline.mixin;
import com.coraline.feast.PartyStorageGuard;
import net.minecraft.world.WorldlyContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Resolve the actual inventory even for wrappers cached before party activation. */
@Mixin(targets="net.minecraftforge.items.wrapper.SidedInvWrapper",remap=false)
public interface PartySidedHandlerMixin extends PartyStorageGuard.SidedInventory {
    @Accessor(value="inv",remap=false) WorldlyContainer coraline$inventory();
}
