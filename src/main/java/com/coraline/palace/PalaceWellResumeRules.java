package com.coraline.palace;

import java.util.Objects;
import java.util.UUID;

/**
 * Side-effect-free validation for resuming the same original Wybie bicycle.
 * A reconnect rotates the well session but must never change actor identity.
 */
public final class PalaceWellResumeRules {
    public record Bicycle(UUID owner,UUID door,UUID rider,UUID session,
                          UUID wellLeaseOwner,boolean storyFriend){}

    public static boolean canRebind(Bicycle actor,UUID owner,UUID door,UUID rider,UUID renewedSession){
        return actor!=null&&owner!=null&&door!=null&&rider!=null&&renewedSession!=null
                &&actor.storyFriend()
                &&Objects.equals(owner,actor.owner())
                &&Objects.equals(door,actor.door())
                &&Objects.equals(rider,actor.rider())
                &&Objects.equals(owner,actor.wellLeaseOwner())
                &&actor.session()!=null;
    }
    private PalaceWellResumeRules(){}
}
