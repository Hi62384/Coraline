package com.coraline.feast;
import java.util.Set;
/** States the ordinary house-side driver may own; performers, games, reports and ceremony stay with their controller. */
public final class PartyGuestHouseRules {
    public static boolean ordinary(String state){return Set.of("ARRIVING","WANDERING","CONVERSING","APPROACHING_GROUP","LEAVING","WARNING","TALKING_TO_PLAYER").contains(state);}
    private PartyGuestHouseRules(){}
}
