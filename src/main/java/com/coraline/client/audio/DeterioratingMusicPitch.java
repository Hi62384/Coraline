package com.coraline.client.audio;

/** Smooth, bounded detuning: the tune remains recognizable even at full deterioration. */
public final class DeterioratingMusicPitch {
    public static float at(float deterioration, double ticks) {
        double amount = Math.max(0, Math.min(1, deterioration));
        // A constant slowdown only transposes a song. These independent, slow drifts
        // move its tuning during phrases, increasingly unsettling its intervals.
        double drift = Math.sin(ticks * .037) * .75
            + Math.sin(ticks * .071 + .8) * .35
            + Math.sin(ticks * .013) * .40;
        double semitones = amount * (-1.8 + drift * amount);
        return (float) Math.pow(2, semitones / 12);
    }

    private DeterioratingMusicPitch() {}
}
