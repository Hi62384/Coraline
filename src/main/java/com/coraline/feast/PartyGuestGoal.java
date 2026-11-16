package com.coraline.feast;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;
/** A high-priority owner-scoped driver; ordinary goals resume after release. */
public final class PartyGuestGoal extends Goal {
    private final Mob actor;
    public PartyGuestGoal(Mob actor){this.actor=actor;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    @Override public boolean canUse(){return actor.getPersistentData().hasUUID("PartyOwner");}
    @Override public boolean canContinueToUse(){return canUse();}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void tick(){PartyGuests.control(actor);}
}
