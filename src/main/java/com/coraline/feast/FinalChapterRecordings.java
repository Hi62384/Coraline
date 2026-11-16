package com.coraline.feast;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Supplied originals only. Opening IDs never alias the later diving A-F IDs.
 * No speaker labels or new subtitle/transcription data are inferred here.
 */
public final class FinalChapterRecordings {
    public static final Map<String, String> SOURCES;
    static {
        var sources = new LinkedHashMap<String, String>();
        for (char cue = 'A'; cue <= 'N'; cue++)
            sources.put(NauticalOpeningPlan.sound(cue), "music/theatre/" + cue + ".wav");
        sources.put(NauticalOpeningPlan.ENTRANCE, "music/theatre/Entrance.wav");
        for (char cue = 'A'; cue <= 'I'; cue++)
            sources.put("crawling_hand_" + Character.toLowerCase(cue),
                    "voicelines/story/CrawlingHandBossFight/" + cue + ".wav");
        SOURCES = Collections.unmodifiableMap(sources);
    }
    private FinalChapterRecordings() {}
}
