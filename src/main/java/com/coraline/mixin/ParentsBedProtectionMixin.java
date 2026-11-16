package com.coraline.mixin;
import com.coraline.parents.ParentsBedSafety;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Protect all four bed parts from direct destruction as well as player attacks. */
@Mixin(Level.class)
public abstract class ParentsBedProtectionMixin {
    @Inject(method="destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z",at=@At("HEAD"),cancellable=true)
    private void coraline$keepBed(BlockPos pos,boolean drops,net.minecraft.world.entity.Entity actor,int depth,CallbackInfoReturnable<Boolean> result){
        if((Object)this instanceof ServerLevel level&&ParentsBedSafety.protectedAt(level,pos))result.setReturnValue(false);
    }
    @Inject(method="setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",at=@At("HEAD"),cancellable=true)
    private void coraline$keepBedFromReplacement(BlockPos pos,BlockState next,int flags,int depth,CallbackInfoReturnable<Boolean> result){
        if((Object)this instanceof ServerLevel level&&level.getBlockState(pos).getBlock()!=next.getBlock()&&ParentsBedSafety.protectedAt(level,pos))result.setReturnValue(false);
    }
}
