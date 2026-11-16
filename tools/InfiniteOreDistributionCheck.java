package com.coraline.postgame;

import java.util.*;

/** Exercises the production terrain/vein samplers, not a reimplementation.
 * This cannot certify registered blocks, native mining or resource loading. */
public final class InfiniteOreDistributionCheck {
    private static int assertions;
    private static void require(boolean valid, String message) {
        assertions++;
        if (!valid) throw new AssertionError(message);
    }
    private static int count(Map<InfiniteOreVeins.Ore, Integer> counts, InfiniteOreVeins.Ore ore) {
        return counts.getOrDefault(ore, 0);
    }
    public static void main(String[] args) {
        var total = new EnumMap<InfiniteOreVeins.Ore, Integer>(InfiniteOreVeins.Ore.class);
        long sampled = 0;
        for (long seed : new long[]{124119, -742971, 0, 981377123}) {
            var field = new InfiniteOreVeins(seed);
            var reload = new InfiniteOreVeins(seed);
            var terrain = new InfiniteTerrain(seed);
            var counts = new EnumMap<InfiniteOreVeins.Ore, Integer>(InfiniteOreVeins.Ore.class);
            for (int x = -256; x < 256; x += 2) for (int z = -256; z < 256; z += 3) {
                var column = terrain.column(x, z);
                for (int y = -58; y <= 112; y += 2) {
                    if (y >= column.surfaceY() - 3 || terrain.cave(x, y, z, column)) continue;
                    sampled++;
                    var ore = field.sample(x, y, z, column.biome());
                    if (ore != reload.sample(x, y, z, column.biome()))
                        throw new AssertionError("Recreated sampler moved a vein");
                    if (ore == InfiniteOreVeins.Ore.NONE) continue;
                    if (y < ore.minY || y > ore.maxY) throw new AssertionError("Ore outside depth band");
                    counts.merge(ore, 1, Integer::sum);
                }
            }
            System.out.println("SEED " + seed + " solid-rock samples=" + counts);
            for (var ore : InfiniteOreVeins.Ore.values()) if (ore != InfiniteOreVeins.Ore.NONE)
                require(count(counts, ore) > 0, "Missing mineable rock candidate: " + ore + " seed=" + seed);
            require(count(counts, InfiniteOreVeins.Ore.OTHER_IRON) > count(counts, InfiniteOreVeins.Ore.OTHER_DIAMOND), "Iron hierarchy seed=" + seed);
            require(count(counts, InfiniteOreVeins.Ore.OTHER_DIAMOND) > count(counts, InfiniteOreVeins.Ore.MOONSTONE), "Diamond hierarchy seed=" + seed);
            require(count(counts, InfiniteOreVeins.Ore.MOONSTONE) > count(counts, InfiniteOreVeins.Ore.PERFECT), "Moonstone hierarchy seed=" + seed);
            counts.forEach((ore, amount) -> total.merge(ore, amount, Integer::sum));
            for (var ore : InfiniteOreVeins.Ore.values()) {
                if (ore == InfiniteOreVeins.Ore.NONE) continue;
                require(field.sample(-1, ore.maxY + 20, -17, columnBiome(ore)) != ore, "Ore above depth band");
                require(field.sample(-1, ore.minY - 1, -17, columnBiome(ore)) != ore, "Ore below depth band");
                boolean found = false;
                search: for (int cx = -10; cx <= 10; cx++) for (int cz = -10; cz <= 10; cz++)
                    for (int cy = Math.floorDiv(ore.minY, ore.vertical); cy <= Math.floorDiv(ore.maxY, ore.vertical); cy++) {
                        var center = field.candidate(ore, cx, cy, cz, columnBiome(ore));
                        if (center == null || field.sample(center.x(), center.y(), center.z(), columnBiome(ore)) != ore) continue;
                        found = true;
                        var before = field.sample(center.x(), center.y(), center.z(), columnBiome(ore));
                        for (int i = -19; i < 19; i++) field.sample(i * 17, -31, i * -31, InfiniteTerrain.Biome.MIRROR_MARSH);
                        require(before == field.sample(center.x(), center.y(), center.z(), columnBiome(ore)), "Traversal changed vein");
                        break search;
                    }
                require(found, "No valid deterministic center for " + ore);
            }
        }
        double ratio = (double) count(total, InfiniteOreVeins.Ore.SOUL_GLASS) / count(total, InfiniteOreVeins.Ore.PERFECT);
        require(ratio >= .67 && ratio <= 1.5, "Soul Glass and Perfect frequencies differ excessively: " + ratio);
        require(total.values().stream().mapToInt(Integer::intValue).sum() < sampled / 50, "Ore overwhelms ordinary rock");
        System.out.println("TOTAL " + total + " solid-rock positions=" + sampled + " soul/perfect=" + ratio);
        System.out.println("INFINITE_ORE_DISTRIBUTION_OK checks=" + assertions);
    }
    private static InfiniteTerrain.Biome columnBiome(InfiniteOreVeins.Ore unused) {
        return InfiniteTerrain.Biome.STITCHED_MEADOWS;
    }
}
