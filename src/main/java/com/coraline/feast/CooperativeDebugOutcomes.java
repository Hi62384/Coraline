package com.coraline.feast;

import com.coraline.multiplayer.*;
import net.minecraft.server.level.ServerPlayer;

/** Deliberate operator simulations are marked in the existing scene. They are
 * controls for a test world, never evidence that a real playthrough passed. */
public final class CooperativeDebugOutcomes {
    public static String victory(ServerPlayer operator,ServerPlayer owner,ServerPlayer survivor){
        var s=VisitData.get(owner.server).scenes.get(owner.getUUID());
        if(!operator.hasPermissions(2)||s==null||s.loop.phase!=OtherMotherLoopState.Phase.ACTIVITY||!StoryCoop.member(survivor,s)||survivor.isSpectator()||!survivor.isAlive())return "Select an actual surviving participant inside the current activity.";
        s.loop.story.putBoolean("OperatorPreview",true);s.loop.story.putUUID("DebugVictoryActor",survivor.getUUID());var l=s.loop;
        switch(l.activity){
            case ARCHERY_CONTEST->{l.archeryIssued=true;l.archeryShotsRemaining=0;}
            case CAKE_MAKING_CONTEST->l.cakeStage=8;
            case CARNIVAL->{l.carnivalIssued=true;l.carnivalTokensRemaining=0;l.carnivalBooth=-1;l.carnivalRide=-1;}
            case DREAM_PARKOUR->l.parkourRewarded=true;
            case FISHING_CONTEST->l.fishingStage=5;
            case GARDEN_MAZE->l.gardenFinished=true;
            case GRAND_FEAST->l.feastFinished=true;
            case ICE_BOAT_RACE->{l.iceStage=3;l.iceResultAt=0;}
            case MAGIC_SHOW->l.magicStage=8;
            case MAGIC_GARDEN->l.flyingGardenStage=6;
            case MICE_CIRCUS->l.circusPhase=6;
            case MUSIC_FESTIVAL->l.festival.putInt("Phase",4);
            case OTHER_DESERT_TEMPLE->l.temple.putInt("Phase",4);
            case OTHER_END_CITY->l.endCity.putInt("Phase",4);
            case OTHER_OCEAN_MONUMENT->l.ocean.putInt("Phase",4);
            case TREASURE_HUNT,SHOPPING_MALL,OTHER_MINESHAFT,SNOWBALL_BATTLE,OTHER_NETHER_FORTRESS,OTHER_WOODLAND_MANSION->{l.expedition.putInt("Phase",4);l.expedition.putBoolean("Won",true);}
            default->{l.villageIssued=true;l.villageTokensRemaining=0;}
        }
        var prize=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND);prize.getOrCreateTag().putBoolean("DebugGranted",true);
        StoryRewardDelivery.award(survivor,s,"operator_preview",java.util.List.of(prize));OtherMotherLoop.changed(owner);ActivityWorld.ensureExit(owner,s);
        return "DEBUG group victory attributed to the actual selected survivor. Native exit and per-player receipt paths remain in use.";
    }
    public static String bossPhase(ServerPlayer operator,ServerPlayer owner,VisitData.Scene s){
        if(!operator.hasPermissions(2)||!owner.getUUID().equals(s.owner))return "Operator and actual native owner required.";
        if(RescueChapter.started(s)&&RescueBosses.fighting(RescueChapter.phase(s))){var n=RescueChapter.data(s);n.putFloat("BossHealth",Math.max(0,n.getFloat("BossHealth")-n.getFloat("BossMaximum")*.35F));s.loop.story.putBoolean("OperatorPreview",true);OtherMotherLoop.changed(owner);return "DEBUG health tier advanced on the same native encounter.";}
        var root=s.loop.story.getCompound("PinkPalaceJourney");var stage=root.getCompound("WellStage");var n=stage.getCompound("Combat");
        if(!PalaceWellEncounter.cooperativeCombat(s)||!n.hasUUID("Hand"))return "An actual native shared boss must already be running.";
        var candidate=n.copy();int phase=candidate.getInt("Phase");
        if(phase<3){candidate.putInt("Phase",phase+1);candidate.putInt("Hits",12);candidate.putInt("Ticks",0);candidate.putInt("Total",Math.max(candidate.getInt("Total"),phase*1200));candidate.putInt("LastHitCycle",-1);}
        else{candidate.putInt("Hits",0);candidate.putInt("Ticks",1200);candidate.putInt("Total",Math.max(candidate.getInt("Total"),3600));candidate.putInt("LastHitCycle",0);candidate.putBoolean("Complete",true);}
        candidate.putLong("LastTick",owner.server.overworld().getGameTime());var verified=com.coraline.palace.PalaceHandCombat.load(s.owner,s.door,n.getUUID("Hand"),candidate);
        if(!verified.blocker().isEmpty())return verified.blocker();stage.put("Combat",verified.save());s.loop.story.putBoolean("OperatorPreview",true);OtherMotherLoop.changed(owner);
        return "DEBUG phase advanced on the original Hand. Native key disposal and ending receipts are still required.";
    }
    private CooperativeDebugOutcomes(){}
}
