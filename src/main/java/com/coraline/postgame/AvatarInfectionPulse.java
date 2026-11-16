package com.coraline.postgame;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.eventbus.api.Event;

/** A real Avatar command; infection sources consume this bounded pulse when
 * present. It never acknowledges a colony objective or grants containment. */
public final class AvatarInfectionPulse extends Event {
    public final ServerLevel level;public final WhiteVoidAvatarEntity avatar;public final int phase;
    AvatarInfectionPulse(ServerLevel level,WhiteVoidAvatarEntity avatar,int phase){this.level=level;this.avatar=avatar;this.phase=phase;}
}
