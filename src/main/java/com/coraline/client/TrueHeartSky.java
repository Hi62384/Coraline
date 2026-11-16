package com.coraline.client;

/** The unique final region is a pale void, with no ordinary sun or night sky. */
public final class TrueHeartSky extends net.minecraft.client.renderer.DimensionSpecialEffects {
    public TrueHeartSky(){super(Float.NaN,false,SkyType.NONE,false,false);}
    @Override public net.minecraft.world.phys.Vec3 getBrightnessDependentFogColor(net.minecraft.world.phys.Vec3 color,float brightness){return new net.minecraft.world.phys.Vec3(.945,.957,.98);}
    @Override public boolean isFoggyAt(int x,int z){return true;}
}
