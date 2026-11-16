package com.coraline.mixin;
import com.coraline.feast.PartyStorageGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Level.class)
public abstract class PartyWorldStorageMixin implements PartyStorageGuard.GuardedWorld {
    @Inject(method="destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z",at=@At("HEAD"),cancellable=true)
    private void coraline$destroyStorage(BlockPos at,boolean drops,net.minecraft.world.entity.Entity actor,int depth,CallbackInfoReturnable<Boolean> ci){var level=(Level)(Object)this;if(!level.isClientSide&&PartyStorageGuard.protectedBlock(level,at))ci.setReturnValue(false);}
    @Inject(method="setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",at=@At("HEAD"),cancellable=true)
    private void coraline$preserveStorage(BlockPos at,BlockState next,int flags,int depth,CallbackInfoReturnable<Boolean> ci){var level=(Level)(Object)this;if(!level.isClientSide&&level.getBlockState(at).getBlock()!=next.getBlock()&&PartyStorageGuard.protectedBlock(level,at))ci.setReturnValue(false);}
}
