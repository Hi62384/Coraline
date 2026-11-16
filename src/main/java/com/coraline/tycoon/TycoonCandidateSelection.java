package com.coraline.tycoon;

import java.util.*;
import net.minecraft.nbt.CompoundTag;

/** Select only from available roster information. Unknown aptitude/production
 * estimates remain tied; selecting a policy does not reveal hidden traits. */
public final class TycoonCandidateSelection {
    public static double score(TycoonData.Resident candidate,String policy){
        var publicInfo=candidate.n.getCompound("CandidateObservations");String description=TycoonCare.personality(candidate).toLowerCase(Locale.ROOT);
        double promising=(description.contains("curious")?.3:0)+(description.contains("sociable")?.3:0)+(description.contains("patient")?.2:0);
        return switch(policy){case "Highest Worker Aptitude"->publicInfo.contains("WorkerAptitudeEstimate")?publicInfo.getDouble("WorkerAptitudeEstimate"):.5;case "Best expected Soul Production"->publicInfo.contains("SoulProductionEstimate")?publicInfo.getDouble("SoulProductionEstimate"):.5;case "Most promising personality"->promising;case "Lowest placement fee"->-Math.max(0,publicInfo.getInt("QuotedFee"));default->promising*.3;};
    }
    public static void order(List<TycoonData.Resident> roster,String policy,int rank,UUID worker,long clock){
        if(roster.isEmpty())return;if(rank<3||policy.equals("Random selection")){Collections.rotate(roster,Math.floorMod(worker.hashCode()+(int)(clock/1200),roster.size()));return;}
        roster.sort(Comparator.<TycoonData.Resident>comparingDouble(candidate->score(candidate,policy)).reversed().thenComparing(TycoonData.Resident::name));
    }
    private TycoonCandidateSelection(){}
}
