package com.coraline.otherworld;

/** QA-only access to the existing house builder; no production visibility change. */
public final class AcceptedGhostHouseFixture {
    public static void build(net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos at,DoorData.Door door,RealmData.Realm realm){RosewoodHouse.build(level,at,door,realm);}
    private AcceptedGhostHouseFixture(){}
}
