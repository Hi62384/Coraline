package com.coraline.mixin;
import com.coraline.feast.PartyStorageGuard;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.*;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Vanilla containers override this Forge method; record every returned sided/unsided handler. */
@Mixin({BaseContainerBlockEntity.class,ChestBlockEntity.class,AbstractFurnaceBlockEntity.class,BrewingStandBlockEntity.class})
public abstract class PartyCapabilityMixin implements PartyStorageGuard.GuardedCapabilities {
    @Inject(method="getCapability",at=@At("RETURN"),remap=false) private <T> void coraline$capability(Capability<T> cap,Direction side,CallbackInfoReturnable<LazyOptional<T>> ci){if(cap==ForgeCapabilities.ITEM_HANDLER)ci.getReturnValue().ifPresent(value->PartyStorageGuard.track((BlockEntity)(Object)this,(net.minecraftforge.items.IItemHandler)value));}
}
