package com.coraline.mixin;

import com.coraline.otherworld.OtherWorld;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep inherited Overworld weather out of the Other World, on both sides. */
@Mixin(Level.class)
public abstract class OtherWorldWeatherMixin {
    @Inject(method={"getRainLevel","getThunderLevel"},at=@At("HEAD"),cancellable=true)
    private void coraline$clearWeather(float partial,CallbackInfoReturnable<Float> result){
        if(OtherWorld.isOther((Level)(Object)this))result.setReturnValue(0F);
    }
}
