package com.coraline.postgame;

import java.util.UUID;

/** Designated live structure locks only; never a generic vanilla lock bypass. */
interface SiteLockTarget {
    int tier();
    boolean locked();
    UUID site();
    boolean authentic();
    boolean unlock(UUID site);
    default boolean canPick(){return true;}
    default String unlockFeedback(){return "message.coraline.site_cache_unlocked";}
}
