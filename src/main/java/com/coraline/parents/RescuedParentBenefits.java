package com.coraline.parents;

/** Permanent owner-family reward, using the normal task inventory/delivery system. */
public final class RescuedParentBenefits {
    // A system property is intentionally not used: this is a persisted family reward.
    public static double durationMultiplier(){return com.coraline.config.CoralineConfig.RESCUED_TASK_MULTIPLIER.get();}
    public static void apply(net.minecraft.server.MinecraftServer server,FamilyData.Family family){
        if(family==null||family.rescued)return;
        family.rescued=true;family.busy=false;family.storyMissing=false;
        long now=server.overworld().getGameTime();
        family.momDelivery=now+Math.max(20,(long)((family.momDelivery-now)*durationMultiplier()));
        family.dadDelivery=now+Math.max(20,(long)((family.dadDelivery-now)*durationMultiplier()));
        FamilyData.get(server).setDirty();
    }
    private RescuedParentBenefits(){}
}
