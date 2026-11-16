package com.coraline.mixin;
import com.coraline.feast.PartyStorageGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Containers.class)
public abstract class PartyStorageDropsMixin {
    @Inject(method="dropContents(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/Container;)V",at=@At("HEAD"),cancellable=true)
    private static void coraline$contents(Level level,double x,double y,double z,Container source,CallbackInfo ci){if(PartyStorageGuard.protects(source)||PartyStorageGuard.protectedBlock(level,BlockPos.containing(x,y,z)))ci.cancel();}
}
