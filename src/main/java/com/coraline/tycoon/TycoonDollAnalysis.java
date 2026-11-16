package com.coraline.tycoon;

import java.util.Locale;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;

/** Active simulation and meaningful receipts advance a child's own knowledge.
 * Opening a menu is read-only; estimates form nested intervals, never rerolls. */
public final class TycoonDollAnalysis {
    public static final long COMPLETE = 48000;
    public static double progress(TycoonData.Resident r) { return Math.min(1, Math.max(0, r.n.getLong("AnalysisTicks") / (double) COMPLETE)); }
    public static double[] interval(TycoonData.Resident r, TycoonPersonality.Trait trait) {
        double p = progress(r), value = r.trait(trait.key());
        return new double[]{value * p + trait.min() * (1-p), value * p + trait.max() * (1-p)};
    }
    public static void tick(ServerPlayer p) {
        var h = TycoonWorld.home(p);
        boolean changed=false;for (var r : h.residents.values()) if (r.active() && progress(r)<1 && TycoonDolls.valid(p,r)){advance(r,100);changed=true;}if(changed)TycoonWorld.dirty(p);
    }
    private static void advance(TycoonData.Resident r, long ticks) { r.n.putLong("AnalysisTicks", Math.min(COMPLETE, Math.max(0,r.n.getLong("AnalysisTicks")) + Math.max(0,Math.min(1200,ticks)))); }
    public static void observe(TycoonData.Resident r, String receipt, String description, int ticks, long clock) {
        if (!r.n.getCompound("Doll").getString("Status").equals("PLACED") || receipt.isBlank()) return;
        var facts = r.n.getList("AnalysisObservations",10);
        for (var fact : facts) if (((CompoundTag)fact).getString("Receipt").equals(receipt)) return;
        var entry = new CompoundTag();entry.putString("Receipt",receipt);entry.putString("Text",description);entry.putLong("At",clock);facts.add(entry);
        while(facts.size()>40)facts.remove(0);r.n.put("AnalysisObservations",facts);advance(r,ticks);
    }
    public static CompoundTag view(TycoonData.Resident r) {
        var result = new CompoundTag(); result.putUUID("Id",r.id);result.putString("Name",r.name());result.putInt("Appearance",r.n.getInt("Appearance"));result.putDouble("Progress",progress(r));
        var rows = new ListTag();
        for (var trait : TycoonPersonality.TRAITS) {
            var row = new CompoundTag();row.putString("Trait",trait.label());var range=interval(r,trait);String text;
            if(progress(r)==0)text="Unknown";
            else if(progress(r)<.25)text=range[0]>.5?"Probably high":range[1]<.5?"Probably low":"Uncertain";
            else if(progress(r)>=1)text=String.format(Locale.ROOT,"%.2f",r.trait(trait.key()));
            else text=String.format(Locale.ROOT,"%.2f to %.2f",range[0],range[1]);
            row.putString("Estimate",text);rows.add(row);
        }
        result.put("Traits",rows);var observations=new ListTag();var facts=r.n.getList("AnalysisObservations",10);
        for(int i=Math.max(0,facts.size()-4);i<facts.size();i++)observations.add(StringTag.valueOf(facts.getCompound(i).getString("Text")));
        result.put("Observations",observations);return result;
    }
    private TycoonDollAnalysis() {}
}
