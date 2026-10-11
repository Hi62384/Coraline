# Other Mother activity-completion reactions

## Recordings

`voicelines/othermother/activity_complete/<state>/complete_01.wav` through
`complete_03.wav`: five existing dialogue states, three exact requested scripts
each, 15 recordings. The manifest is `tools/other_mother_activity_complete.json`.
Batch reports use `complete_generation_report.json`, `complete_validation_report.json`
and `complete_transcript_review.json` under `voicelines/othermother`.

The same pinned local IndexTTS2 installation/model, sole unchanged `Sample.wav`
reference, five established state emotion profiles, decoding settings, preprocessing,
constant-gain loudness matching and conservative silence handles are reused.
Masters remain mono 22,050 Hz PCM16 WAV. No additional voice reference, music,
ambience, reverb, sound effects or pitch processing. Existing importer produces
streamed Ogg/Vorbis files under `assets/coraline/sounds/visit/other_mother/activity_complete/`.

Model emotion vectors support restrained state variation; metadata direction text
is not a model text prompt. Automated waveform/ASR/speaker checks do not replace
human listening for acting, nuance or subtle artifacts.

## Activity-end integration

The owning gameplay system calls this once an activity ends, before restoring free time:

```java
boolean started = OtherMotherActivityComplete.begin(player, p -> {
    // Restore the activity owner's normal relax/free-time phase here.
});
```

True means one reaction started; do not call again for the same completed activity.
False means busy/ineligible: retain the existing end phase and retry when eligible,
without transitioning to free time early. The controller rejects duplicate pending
reactions and attempts during an active proposal/warning/attendance sequence.

Selection reads the current existing `scene.welcomeState` at playback, not the state
when the activity was proposed or entered. It reuses `OtherMotherDialogue.choose`
and family-scoped `lastDialogue` history: uniform selection from three lines excluding
the previous clip in that state's pool. History persists across reloads and resets
with replacement parents. No Trustness/Suspicion thresholds, formulas or penalties.

The existing `OtherMotherTutorial.Completion` one-shot deadline waits for the full
registered audio duration, then invokes the supplied free-time callback exactly once.
Pending playback blocks unrelated dialogue hooks from interrupting it. Existing
player-relative `VisitAudio` transport is reused; the mother need not remain loaded
near a distant activity. Loaded dead/invalid mothers are rejected. Nothing teleports
the player, moves an NPC, grants loot or chooses another activity.

Death, leaving the visit/dimension, entering the tunnel, family reset, logout or
server stop cancel the live continuation. Active callbacks are session-local and
do not resume after reconnect/restart; saved selection history does persist.

## Existing project boundary

There are no implemented activity arenas, activity portals or repeat-visit activity
gameplay loop in this repository yet. This is a callable completion hook for that
owner, not an automatically triggered event at a nonexistent activity. It does
not replace the first-visit piano/supper storyline or invent a new free-time system.

## Changed code/resources

- Added `OtherMotherActivityComplete.java`, the manifest, 15 WAVs/Oggs, reports and this guide.
- Updated `OtherMotherDialogue.java`: five pools and shared playback guard.
- Updated `FamilyVisitReset.java`: cancel pending completion on family replacement/death.
- Updated `WelcomeAudioGameTests.java`: pool routing, assets/history/non-repetition and
  runtime arrival → synthetic activity end → completion → free-time timing.
- Updated sequential imports in `build.gradle`, release version in `gradle.properties`,
  `sounds.json`, `visit_audio_lengths.json`, synthetic-audio notice and README.
- Voice generation/check/import algorithms are unchanged.

## Reproduce

```powershell
$env:CORALINE_VOICE_MANIFEST='tools/other_mother_activity_complete.json'
& '.voice-tools/index-tts/.venv/Scripts/python.exe' tools/generate_other_mother_welcome.py
python tools/import_welcome_audio.py
& '.voice-tools/index-tts/.venv/Scripts/python.exe' tools/check_welcome_audio.py --transcribe --speaker-check --ogg
& '.voice-tools/index-tts/.venv/Scripts/python.exe' tools/review_voice_transcripts.py
Remove-Item Env:CORALINE_VOICE_MANIFEST
.\gradlew.bat -PgameTests -PwelcomeTests -PfreshGameTests runGameTestServer
.\gradlew.bat clean build
```

Do not run memory-heavy Gradle tasks while IndexTTS is loaded. Existing masters are
skipped; selective repairs use `--only <logical/path.wav> --replace --seed-offset N`.
Only final takes are stored in the requested source/resource folders.

## Verification — 2026-09-30

All 15 final PCM masters and 15 streamed Ogg files decode successfully. The final
set totals 168.23 seconds, with individual lengths of 9.24–13.77 seconds. All pass
the existing waveform, clipping, duration and speaker-similarity screens. Reference
speaker cosine scores range from 0.712 to 0.867. All 15 manifest scripts exactly
match the supplied attachment. `Sample.wav` and all 130 previous masters are unchanged.

Thirteen recordings transcribe exactly. Two received two replacement takes each
(seed offsets 1000 and 2000), but retain a small recognition ambiguity in independent
greedy and beam decoders:

- `high_suspicion_low_trust/complete_03.wav`: expected **stopped**, recognized **stop**.
- `high_trust_high_suspicion/complete_02.wav`: expected **seemed**, recognized **seem**.

These are unresolved listening-review items, not silently corrected transcripts.
The full sentences otherwise transcribe correctly. The script input was not altered
to force a recognizer result, and raw transcription scores are retained in the
validation/review reports. Automated screening cannot establish whether these
connected-speech endings are correct or certify the emotional performance. Human
audition is recommended before considering exact delivery approved.

All 17 Forge dialogue GameTests passed, including registered resources, current-state
routing, non-repetition and persisted histories, duplicate protection, and the
runtime arrival → completion reaction → free-time callback sequence. Tests exercise
the dialogue hooks, not nonexistent activity arenas or subjective listening quality.

The clean release build succeeded: `build/libs/coraline-1.13.6.jar`.
All 15 packaged completion Oggs match the final source assets byte-for-byte,
the completion controller is included, and test classes/model weights are absent.
