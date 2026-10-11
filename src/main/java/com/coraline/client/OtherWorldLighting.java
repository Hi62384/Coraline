package com.coraline.client;

import com.coraline.config.OtherWorldVisualConfig;
import org.joml.Vector3f;

/** Dimension-local lightmap adjustment, not a screen filter or night vision. */
public final class OtherWorldLighting {
    public static void apply(Vector3f color,int block,int sky){
        float lamp=block/15F;
        float moon=(float)Math.pow(sky/15F,1.65)*OtherWorldVisualConfig.MOONLIGHT.get().floatValue()*(1-lamp*.82F);
        color.set(lift(color.x,moon*.78F),lift(color.y,moon*.89F),lift(color.z,moon));
        float warm=lamp*lamp*OtherWorldVisualConfig.WARMTH.get().floatValue();
        color.set(lift(color.x,warm),lift(color.y,warm*.45F),color.z*(1-warm*.30F));
    }
    private static float lift(float value,float amount){return Math.min(1,value+(1-value)*amount);}
}
