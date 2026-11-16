package com.coraline.client;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
/** Presentation metadata is valid only in the server-supplied owner region. */
public final class TycoonWorldClientState {
    private static BlockPos center;private static int sky;
    public static void receive(CompoundTag n){center=n.contains("Center")?BlockPos.of(n.getLong("Center")):null;sky=Math.max(0,Math.min(5,n.getInt("SkyTier")));TycoonSpyMarkers.receive(n.getList("SpyMarkers",10));}
    public static int sky(){var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.level.dimension()!=com.coraline.tycoon.TycoonWorld.KEY||center==null||Math.abs(mc.player.getX()-center.getX())>=128||Math.abs(mc.player.getZ()-center.getZ())>=128)return 0;return sky;}
    public static void clear(){center=null;sky=0;TycoonSpyMarkers.clear();}
    private TycoonWorldClientState(){}
}
