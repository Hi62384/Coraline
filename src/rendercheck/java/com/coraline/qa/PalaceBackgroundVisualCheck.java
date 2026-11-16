package com.coraline.qa;

import com.coraline.palace.PalaceWorldGeneration;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** QA-only isolation: other-scene checks must not generate a whole unrelated
 * Overworld region in the background. Explicit Palace worldgen QA opts in.
 * This class/config is never included in a release JAR.
 */
@Mixin(value=PalaceWorldGeneration.class,remap=false)
public abstract class PalaceBackgroundVisualCheck {
    @Inject(method="tick",at=@At("HEAD"),cancellable=true,remap=false)
    private static void coraline$separatePalaceJob(TickEvent.ServerTickEvent event,CallbackInfo callback){
        if(Boolean.getBoolean("coraline.leanVisualWorld")&&!Boolean.getBoolean("coraline.palaceWorldgenQa"))callback.cancel();
    }
}
