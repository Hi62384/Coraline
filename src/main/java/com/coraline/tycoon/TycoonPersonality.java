package com.coraline.tycoon;

import java.util.*;
import net.minecraft.nbt.CompoundTag;

/** One persistent trait registry. Aliases use the existing Phase 1 traits rather
 * than inventing competing sociability/sensitivity values. Appearance is never
 * an input to interests, including FoodInterest. */
public final class TycoonPersonality {
    public record Trait(String key, String label, double min, double max, String alias) {}
    public static final List<Trait> TRAITS = List.of(
        new Trait("Explorativeness", "Explorativeness", 0, 1, ""),
        new Trait("Curiosity", "Curiosity", 0, 1, ""),
        new Trait("Playfulness", "Playfulness", 0, 1, ""),
        new Trait("Socialness", "Socialness", 0, 1, "Sociability"),
        new Trait("Aweness", "Aweness", 0, 1, ""),
        new Trait("Artisticness", "Artisticness", 0, 1, ""),
        new Trait("FoodInterest", "Food Interest", 0, 1, ""),
        new Trait("Giftableness", "Giftableness", -1, 1, ""),
        new Trait("Independence", "Independence", 0, 1, ""),
        new Trait("Patience", "Patience", 0, 1, ""),
        new Trait("EmotionalSensitivity", "Emotional Sensitivity", 0, 1, "Sensitivity"),
        new Trait("Mischievousness", "Mischievousness", 0, 1, "Mischief"),
        new Trait("ComfortPreference", "Comfort Preference", 0, 1, ""),
        new Trait("Adventurousness", "Adventurousness", 0, 1, ""),
        new Trait("WorkerAptitude", "Worker Aptitude", 0, 1, ""));
    public static Trait definition(String key) {
        return TRAITS.stream().filter(t -> t.key.equals(key) || !t.alias.isEmpty() && t.alias.equals(key)).findFirst().orElse(null);
    }
    public static boolean ensure(TycoonData.Resident r) {
        if(r.n.getInt("PersonalitySchema")==1)return false;
        var values = r.n.getCompound("Traits"); boolean changed = false;
        for (var trait : TRAITS) {
            String key = trait.alias.isEmpty() ? trait.key : trait.alias;
            if (!values.contains(key)) {
                // Separate deterministic streams make migration stable regardless
                // of which older generator/trait list created this resident.
                var random = new Random(r.id.getMostSignificantBits() ^ Long.rotateLeft(r.id.getLeastSignificantBits(), 23) ^ ((long) key.hashCode() * 0x9E3779B97F4A7C15L));
                values.putDouble(key, trait.min + random.nextDouble() * (trait.max - trait.min)); changed = true;
            }
        }
        if (changed) r.n.put("Traits", values);
        r.n.putInt("PersonalitySchema",1);
        return changed;
    }
    public static double value(TycoonData.Resident r, String key) {
        ensure(r); var trait = definition(key);
        String stored = trait != null && !trait.alias.isEmpty() ? trait.alias : key;
        var values = r.n.getCompound("Traits"); double value = values.contains(stored) ? values.getDouble(stored) : .5;
        double min = trait == null ? 0 : trait.min, max = trait == null ? 1 : trait.max;
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : (min + max) / 2;
    }
    public static void set(TycoonData.Resident r, String key, double value) {
        var trait = definition(key); if (trait == null || !Double.isFinite(value)) throw new IllegalArgumentException("Unknown trait/value");
        ensure(r); var values = r.n.getCompound("Traits"); values.putDouble(trait.alias.isEmpty() ? trait.key : trait.alias, Math.max(trait.min, Math.min(trait.max, value))); r.n.put("Traits", values);
    }
    public static CompoundTag exact(TycoonData.Resident r) {
        var n = new CompoundTag(); for (var trait : TRAITS) n.putDouble(trait.key, value(r, trait.key)); return n;
    }
    private TycoonPersonality() {}
}
