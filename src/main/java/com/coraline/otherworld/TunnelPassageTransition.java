package com.coraline.otherworld;

/** State comparisons for one journalled cell; writes still require physical readback. */
final class TunnelPassageTransition {
    enum Decision { VERIFIED, WRITE, WAIT }

    static Decision decide(boolean restoring,boolean available,boolean matchesOriginal,boolean matchesPlacement,boolean safeToWrite){
        if(!available)return Decision.WAIT;
        if(restoring){
            // An unchanged original or a verified foreign edit needs no write.
            if(matchesOriginal||!matchesPlacement)return Decision.VERIFIED;
        }else{
            if(matchesPlacement)return Decision.VERIFIED;
            if(!matchesOriginal)return Decision.WAIT;
        }
        return safeToWrite?Decision.WRITE:Decision.WAIT;
    }

    private TunnelPassageTransition(){}
}
