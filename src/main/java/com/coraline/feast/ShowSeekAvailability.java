package com.coraline.feast;

import java.util.Map;

/** Build-time release gate. Neither packets, config nor a resource pack can enable it. */
public final class ShowSeekAvailability {
    // Change only after the native acceptance checklist in docs/audio-seek-integration.md passes.
    private static final boolean NATIVE_CONTINUITY_VERIFIED = false;
    // Original source identities measured on the owner computer; no original audio is bundled here.
    private static final Map<String,String> ORIGINAL_SHA256 = Map.ofEntries(
        Map.entry("theatre_music", "9a16cbd9d4265440eb4aa2ab780a2a8a5e564330cd846574e4fd0f5ff6c4787c"),
        Map.entry("theatre_a", "6272fee05291ee401e5187409346b6a88af6eef71e5b85d7640fcaf4277d6b1f"),
        Map.entry("theatre_b", "64ed4ee898b8c168e25769658117d1f70e2291371a5a31e99bdff58deb219970"),
        Map.entry("theatre_c", "c0367f4fcc750f5738261c47d9b6369984eb9862651876273684032099527382"),
        Map.entry("theatre_d", "c2c0e7537159552d2da960e146ad4957c02dcefbe8ce1ac95c538e939a490070"),
        Map.entry("theatre_e", "950ebc42f7144c75926867a1cd16b746e2852d6a67063cdfadb9d80cbde75e5d"),
        Map.entry("theatre_f", "b70b9cba49a75425771045a20f1541b74e8f3a4ab8aab29a290f56cf297c9380"),
        Map.entry("theatre_meet_parents", "0749cfa9e28bde7d24dff31e9d4e15844041263c3be2af23e1d6d66704648afb"),
        Map.entry("mouse_circus_invite", "3ce0094b694a9bbbe74ca9201922ea438ce33bfefbf12319919180abffc8cf0c"),
        Map.entry("mouse_circus_intro", "beae22138a01eb425f89c199594fc9d53cc286a64b47311e6cbc54ee044a5d6c"),
        Map.entry("mouse_circus_music", "4fafb47d16f5a048daebe70ae767bd3e2915e89a327b9ff19805683f4668b74f"),
        Map.entry("mouse_circus_end", "8eac7ccc0321c7054799147455cfa65d90234378ce2ea5b9566dd08c270346c8"));

    // Current export identities, read back from the original-recording ledger and
    // actual Ogg bytes on 2026-10-15. Frame counts are container metadata, NOT a
    // claim of native decoder/actor continuity. Keep the independent release gate.
    private static final Map<String, Media> PINNED_MEDIA = Map.ofEntries(
        Map.entry("theatre_music", new Media("theatre_music", "coraline:sounds/numbered_originals/theatre_music.ogg",
            "9a16cbd9d4265440eb4aa2ab780a2a8a5e564330cd846574e4fd0f5ff6c4787c", "9197e4c021d1097432b2852a61fc5dbd51c99e823d24fb47d825824cdab96242",
            1309720L, 44100, 2, 4303872L)),
        Map.entry("theatre_a", new Media("theatre_a", "coraline:sounds/numbered_originals/theatre_a.ogg",
            "6272fee05291ee401e5187409346b6a88af6eef71e5b85d7640fcaf4277d6b1f", "944c4865ada7eeff7f64bc057a3cf60140493777bba16ee621b6f8f480bd05b5",
            73441L, 44100, 1, 332288L)),
        Map.entry("theatre_b", new Media("theatre_b", "coraline:sounds/numbered_originals/theatre_b.ogg",
            "64ed4ee898b8c168e25769658117d1f70e2291371a5a31e99bdff58deb219970", "12c20014df2aee1bf8df343a8edd8dd8713cfbf0d0c54ab4adc187e71ade6acc",
            33166L, 44100, 1, 132608L)),
        Map.entry("theatre_c", new Media("theatre_c", "coraline:sounds/numbered_originals/theatre_c.ogg",
            "c0367f4fcc750f5738261c47d9b6369984eb9862651876273684032099527382", "eeb0effbe0854ab4aa542ba8fe1acdcc806552a940174b370d736e3116d1372b",
            60964L, 44100, 1, 268288L)),
        Map.entry("theatre_d", new Media("theatre_d", "coraline:sounds/numbered_originals/theatre_d.ogg",
            "c2c0e7537159552d2da960e146ad4957c02dcefbe8ce1ac95c538e939a490070", "9d4ac5c14ed08d0135a076028f6edca73439d032532f0e2fd8697f50a5fdafd3",
            83429L, 44100, 1, 401920L)),
        Map.entry("theatre_e", new Media("theatre_e", "coraline:sounds/numbered_originals/theatre_e.ogg",
            "950ebc42f7144c75926867a1cd16b746e2852d6a67063cdfadb9d80cbde75e5d", "12c6112abe5afa7f328ea952d332ec7ad08ffb30dabc8d0d12d619c45fdebc6b",
            206457L, 44100, 1, 991232L)),
        Map.entry("theatre_f", new Media("theatre_f", "coraline:sounds/numbered_originals/theatre_f.ogg",
            "b70b9cba49a75425771045a20f1541b74e8f3a4ab8aab29a290f56cf297c9380", "54f66e1b7f56915df2f3a513b683656f7f2236edb8efa138558285c8769ccb8b",
            332840L, 44100, 1, 1518640L)),
        Map.entry("theatre_meet_parents", new Media("theatre_meet_parents", "coraline:sounds/numbered_originals/theatre_meet_parents.ogg",
            "0749cfa9e28bde7d24dff31e9d4e15844041263c3be2af23e1d6d66704648afb", "d24762d78748dbd9af530547afc1b6b1378fdc3ad8736607dbfae6c96ba695ae",
            147969L, 44100, 1, 676352L)),
        Map.entry("mouse_circus_invite", new Media("mouse_circus_invite", "coraline:sounds/numbered_originals/mouse_circus_invite.ogg",
            "3ce0094b694a9bbbe74ca9201922ea438ce33bfefbf12319919180abffc8cf0c", "651813da7166c49fb5ab531896f30d7ebad56965387bd1eb5256d3834acdc029",
            94998L, 44100, 1, 452096L)),
        Map.entry("mouse_circus_intro", new Media("mouse_circus_intro", "coraline:sounds/numbered_originals/mouse_circus_intro.ogg",
            "beae22138a01eb425f89c199594fc9d53cc286a64b47311e6cbc54ee044a5d6c", "78937c1f534f0d772a0f25d45f9cd9b79bbfb0a27a36abe874b7df851f36fb9f",
            248142L, 44100, 1, 1200640L)),
        Map.entry("mouse_circus_music", new Media("mouse_circus_music", "coraline:sounds/numbered_originals/mouse_circus_music.ogg",
            "4fafb47d16f5a048daebe70ae767bd3e2915e89a327b9ff19805683f4668b74f", "fb30c2719c0281681cb96008adfb8c548e7e28b5b9411e0d313c6bd88a5a21f6",
            1212609L, 48000, 2, 3932718L)),
        Map.entry("mouse_circus_end", new Media("mouse_circus_end", "coraline:sounds/numbered_originals/mouse_circus_end.ogg",
            "8eac7ccc0321c7054799147455cfa65d90234378ce2ea5b9566dd08c270346c8", "563bf39809cabfc4fd6d3b17848818fe790e73c0eb4d0dcfa88959075ee97943",
            184573L, 44100, 1, 914432L))
    );

    public record Media(String event, String resource, String originalSha256, String runtimeSha256,
                        long compressedBytes, int sampleRate, int channels, long decodedFrames) {
        public Media {
            if (event == null || !ShowRecordings.SOURCES.containsKey(event)
                || resource == null || !resource.matches("coraline:sounds/[a-z0-9_./-]+\\.ogg")
                || resource.contains("..") || !hash(originalSha256) || !originalSha256.equals(ORIGINAL_SHA256.get(event)) || !hash(runtimeSha256)
                || compressedBytes <= 0 || compressedBytes > 64L * 1024 * 1024
                || sampleRate < 8000 || sampleRate > 48000 || channels < 1 || channels > 2
                || decodedFrames <= 0 || decodedFrames > (long) sampleRate * 3600) {
                throw new IllegalArgumentException("Unverified or unsupported show recording identity");
            }
        }
        private static boolean hash(String value) { return value != null && value.matches("[0-9a-f]{64}"); }
        public int durationTicks() { return (int) ((decodedFrames * 20 + sampleRate - 1) / sampleRate); }
        public long frameAtTick(int tick) {
            if (tick < 0 || tick >= durationTicks()) throw new IllegalArgumentException("Seek outside recording");
            long frame = (long) tick * sampleRate / 20;
            if (frame >= decodedFrames) throw new IllegalArgumentException("Seek at or after final decoded frame");
            return frame;
        }
    }

    public static boolean productionReady() { return NATIVE_CONTINUITY_VERIFIED && PINNED_MEDIA.keySet().equals(ORIGINAL_SHA256.keySet()); }
    public static Media media(String event) { return productionReady() ? PINNED_MEDIA.get(event) : null; }
    public static String blocker() {
        return "Audio-offset seeking is disabled pending exact Minecraft 1.20.1 / Forge 47.4.23 compilation, "
            + "native decoded-stream/actor continuity and cancellation tests. Current export hashes are pinned; native acceptance is still pending.";
    }
    private ShowSeekAvailability() {}
}
