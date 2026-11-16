package com.coraline.mixin;
import com.coraline.feast.PartyStorageGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin({DispenserBlock.class,DropperBlock.class})
public abstract class PartyDispenserMixin {
    @Inject(method="dispenseFrom",at=@At("HEAD"),cancellable=true) private void coraline$dispense(ServerLevel level,BlockPos at,CallbackInfo ci){if(PartyStorageGuard.protectedAt(level,at))ci.cancel();}
}
