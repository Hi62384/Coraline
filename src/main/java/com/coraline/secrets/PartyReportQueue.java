package com.coraline.secrets;

/** Repairs only an exact legacy queued pool and one of its two selected lines. */
public final class PartyReportQueue {
    public record Selection(String pool, String sound) {}

    public static Selection repair(String queuedPool, String queuedSound, String oldPool, String newPool) {
        if (!oldPool.equals(queuedPool)) return null;
        String oldPrefix = "other_mother_" + oldPool.replace('/', '_');
        String newPrefix = "other_mother_" + newPool.replace('/', '_');
        for (int i = 1; i <= 2; i++) {
            String suffix = "_0" + i;
            if ((oldPrefix + suffix).equals(queuedSound)) return new Selection(newPool, newPrefix + suffix);
        }
        return null;
    }

    private PartyReportQueue() {}
}
