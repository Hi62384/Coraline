package com.coraline.clothing;

/** Bounds for the ordinary twenty-tick hurt window only. */
public final class RapierCadence {
    public static boolean ordinaryWindow(boolean sameOwnerAndSource,long worldTicks,int targetTicks,int invulnerableTicks,int interval) {
        return sameOwnerAndSource&&interval>=5&&interval<=20&&worldTicks>=interval&&worldTicks<=20
            &&targetTicks>=interval&&targetTicks<10&&invulnerableTicks>10&&invulnerableTicks<=20
            &&invulnerableTicks==20-targetTicks;
    }
    private RapierCadence() {}
}
