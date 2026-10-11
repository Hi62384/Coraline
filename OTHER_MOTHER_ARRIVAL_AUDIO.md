# Other Mother activity-arrival greetings

## Assets and voice pipeline

30 exact requested lines live in `voicelines/othermother/activity_arrival/`:
five existing state IDs, each with `on_time/` and `late/`, each containing
`arrival_01.wav` through `arrival_03.wav`. The manifest is
`tools/other_mother_arrivals.json`; generation, validation and independent transcript
review reports use separate `arrival_*` filenames under `voicelines/othermother`.

The existing pinned local IndexTTS2 model/pipeline uses only the unchanged
`voicelines/othermother/Sample.wav` reference. The PCM masters remain mono,
22,050 Hz, 16-bit, with the existing conservative silence trimming, padding and
constant-gain loudness matching. No music, ambience, reverb or new effects.
On-time profiles reuse the five established state settings. Late profiles make
restrained emotion-vector adjustments for disappointment, suspicion or knowing
calm; decoding temperatures and all processing stay consistent. Direction text
is metadata, not an additional text prompt to the model. Automated checks cannot
certify subjective acting quality or replace human listening.

The existing importer registers streamed Ogg copies under
`assets/coraline/sounds/visit/other_mother/activity_arrival/`, sound IDs and exact
tick lengths. `importArrivalAudio` runs after the existing imports during builds.

## Timer and continuation contract

`OtherMotherActivities.Sequence` remains the sole attendance timer. Its original
five-minute deadline starts only after the proposal and warning finish. Before
that deadline is ON_TIME; at or after it is LATE. Expiry reports once and retains
the original deadline instead of discarding the activity. No second attendance
timer is started, and greeting duration never changes the arrival classification.

The existing portal integration point is:

```java
boolean arrivalRecorded = OtherMotherActivities.arrive(player, selectedActivity);
// Do NOT teleport here. Put the activity entry in Listener.arrived instead.
```

At arrival, the controller reads the current `scene.welcomeState` and chooses one
of the three recordings from its matching condition pool. Existing saved dialogue
history prevents immediate repetition independently in all ten pools, including
across reloads. Relationship classification, formulas, loot and penalties are not
changed. If expiry occurs in the interaction tick, its existing listener runs
before state selection, so any externally implemented penalty remains authoritative.

True means attendance was recorded and the greeting started or is queued behind
existing audio. Queuing cannot turn a timely arrival late. Repeated interactions
during ARRIVAL_WAIT or GREETING are rejected.
Only after the registered recording duration finishes does `Listener.arrived`
run once with the original selected activity. False means premature, wrong activity,
duplicate or invalid context: do not teleport. The queue retries busy playback
automatically, reading the current state when it can speak. Recordings remain player-relative even at distant portals;
an unloaded mother does not prevent arrival audio, but a loaded invalid/dead mother
does. No character is moved or spawned by this audio layer.

`Listener.expired` is still called exactly once, never replaced with a dialogue
penalty. A gameplay owner that prohibits late entry should call `cancel(player)`
there. Otherwise late attendance remains possible. Call cancel when an activity
is withdrawn before beginning another one. Leaving the visit, death, family reset,
logout and server stop discard pending runtime callbacks, as before. Histories
persist; active activity callbacks are session-local, not restart-persistent.

## Integration boundary

This repository has dialogue/timer callbacks but no implemented activity portals,
arenas or repeat-visit loot loop yet. The greetings extend those callable hooks;
they do not invent destinations or automatically start missing gameplay. Actual
portal code must call `arrive` and perform its entry only in `Listener.arrived`.
The first-visit piano/supper sequence and Little Door remain untouched.

## Reproduce

```powershell
$env:CORALINE_VOICE_MANIFEST='tools/other_mother_arrivals.json'
& '.voice-tools/index-tts/.venv/Scripts/python.exe' tools/generate_other_mother_welcome.py
python tools/import_welcome_audio.py
& '.voice-tools/index-tts/.venv/Scripts/python.exe' tools/check_welcome_audio.py --transcribe --speaker-check --ogg
& '.voice-tools/index-tts/.venv/Scripts/python.exe' tools/review_voice_transcripts.py
Remove-Item Env:CORALINE_VOICE_MANIFEST
.\gradlew.bat -PgameTests -PwelcomeTests -PfreshGameTests runGameTestServer
.\gradlew.bat clean build
```

Do not run Gradle while the generation model is loaded. Existing masters are
skipped; selective repairs use `--only <logical/path.wav> --replace --seed-offset N`.
Scratch and failed takes stay outside the final audio folders.

## Verification — 2026-09-30

All 30 final masters and 30 streamed Oggs decode successfully. Total duration is
215.43 seconds, with individual clips from 5.97 to 8.99 seconds. All pass the
existing PCM, duration, finite-waveform and clipping checks. CAMPPlus similarity
to the unchanged sample ranges from 0.688 to 0.876; none triggers the low-similarity
screen. All 30 manifest texts match the supplied attachment exactly.

One take, high-trust/low-suspicion late arrival 02, was replaced with seed offset
1000 because both initial decoders heard an unclear opening. The replacement
transcribes the entire requested sentence exactly. A separate recognition
ambiguity in high-suspicion/low-trust on-time arrival 01 (managed/manage) resolves
in independent beam decoding; its remaining to/too difference is a homophone.
Raw transcripts and scores remain in the reports. The other 28 initial takes
transcribe exactly. This is automated technical screening, not human approval
of all emotional nuances or an assertion that no subtle synthesis artifact exists.

All 16 Forge dialogue GameTests passed, including the runtime proposal → warning
→ arrival greeting → one-shot continuation path. The first run exposed an unloaded
test-fixture mother chunk; the fixture now waits for entity accessibility before
starting the sequence. No production behavior was weakened to satisfy that test.

The clean release build succeeded: `build/libs/coraline-1.13.5.jar`. All 30 packaged
Oggs match the final source assets byte-for-byte, arrival runtime classes are
present, and test classes/model weights are absent. Checksums confirm all 100
previous masters and `Sample.wav` remain unchanged.

## Changed files

- Added `tools/other_mother_arrivals.json`, this guide, 30 WAVs, 30 Oggs and batch reports.
- Extended `OtherMotherActivities.java` with retained attendance classification,
  queued greetings and one-shot post-audio continuation.
- Extended `OtherMotherDialogue.java` with ten registered arrival pools and
  activity-owned remote playback, reusing existing history and sound transport.
- Extended `WelcomeAudioGameTests.java` with arrival routing, boundary/queue timing,
  non-repetition/resource coverage and an actual callback/playback-duration test.
- Updated `build.gradle`, `gradle.properties` (1.13.5), `sounds.json`,
  `visit_audio_lengths.json`, the synthetic-audio notice and README.
- Updated `tools/review_voice_transcripts.py` to support a batch-specific report name,
  and `OTHER_MOTHER_ACTIVITY_AUDIO.md` to document the revised arrival contract.

No generation model, sample audio, relationship formulas, loot, actual activity
destinations, or first-visit story logic was changed.
