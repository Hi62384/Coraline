# Other Mother activity proposals and warnings

## Recordings

- `voicelines/othermother/activities/<activity>/proposal_01.wav` and `proposal_02.wav`:
  22 activity folders, 44 recordings.
- `voicelines/othermother/activity_wait/<state>/wait_01.wav` through `wait_03.wav`:
  five existing state folders, 15 recordings.
- `tools/other_mother_activities.json` contains all 59 exact requested texts, copied
  from the supplied request without paraphrasing, plus performance settings.
- `voicelines/othermother/activity_generation_report.json` and
  `activity_validation_report.json` keep this batch separate from prior reports.
- Streamed Minecraft copies are under `assets/coraline/sounds/visit/other_mother/`,
  with the same activity/state subfolders and filenames, using `.ogg`.

The same pinned local IndexTTS2 installation and model, sole `Sample.wav` reference,
decoding settings, silence handles, constant-gain normalization and PCM format are
reused. Masters are mono 22,050 Hz signed 16-bit WAV. No new dependencies or models.
No reverb, pitch shifting, music, ambience, or sound effects are added. Required
model-output license and synthetic-audio notice accompany the output and JAR.

Proposals use the existing cheerful/nurturing food-before profile. Warnings reuse
the exact five welcome-state emotion vectors and temperatures. These coarse model
controls support emotional variation but do not certify acting quality; human
audition is still required for enthusiasm, menace restraint and character nuance.

## Dialogue sequence

`OtherMotherActivities.Activity` defines the 22 activity IDs. Each proposal pool has
two recordings; `waitPool(OtherMotherWelcome.State)` has three per existing state.
Both reuse the existing saved non-repeating selection: histories are independent
per pool, survive reload, and reset with the player's family. No relationship
classification or thresholds are duplicated or changed.

After the loot AFTER line ends, the caller can start the full continuation:

```java
OtherMotherActivities.beginAfterTutorial(player, chosenActivity, listener);
```

This plays one generic tutorial, then the selected activity proposal. For a caller
already at the proposal stage, use `begin(player, chosenActivity, listener)` instead.
`chooseActivity(player.getRandom())` uniformly selects an activity from all 22 IDs;
a gameplay system can instead supply an activity from its currently available set.
False means busy/ineligible: nothing was started; retry from the same stage later.
Do not call again once true is returned. No existing first-visit scene is replaced.

At proposal completion, the sequence reads the **current** `scene.welcomeState`
and selects its warning. When the warning has fully finished, the sequence starts
exactly 6,000 server ticks (five minutes at 20 TPS), and calls:

```java
listener.portalReady(player, activity, deadlineTick);
```

The owning portal system opens/marks its portal there. `remainingTicks(player)`
exposes the remaining window. Portal code calls `arrive(player, activity)` before
teleporting: wrong-activity, premature, duplicate and invalid-context arrivals
return false. A successful arrival now plays a current-state on-time/late greeting
before `listener.arrived` runs once. Expiry calls `listener.expired` once and retains
the deadline for a later arrival; owners may explicitly cancel unavailable activities.
See `OTHER_MOTHER_ARRIVAL_AUDIO.md` for the extended contract. This layer never
assigns penalties or changes Trustness/Suspicion for missing an activity.

While a proposal/warning is playing, other pool requests cannot interrupt it.
The five-minute window does not freeze the player or require the mother to remain
loaded nearby. Leaving the visit, dying, replacing the family or disconnecting
cancels the live sequence and calls `listener.cancelled` where a player exists.
Server stop clears runtime callbacks. Histories persist, but callbacks and active
attendance windows are session-local; a future persistent activity system must
explicitly manage resuming its gameplay across disconnects/restarts.

## Integration boundary

There are still no implemented activity arenas, activity portals, attendance
penalties or repeat-visit loot gameplay loop in this repository. These are real,
tested dialogue/timer hooks ready for that system, not newly invented gameplay.
Nothing automatically announces nonexistent destinations when entering the world.
Normal mob spawning, Little Door behavior, and first-visit piano/supper are unchanged.

## Reproduction

```powershell
$env:CORALINE_VOICE_MANIFEST='tools/other_mother_activities.json'
& '.voice-tools/index-tts/.venv/Scripts/python.exe' tools/generate_other_mother_welcome.py
python tools/import_welcome_audio.py
& '.voice-tools/index-tts/.venv/Scripts/python.exe' tools/check_welcome_audio.py --transcribe --speaker-check --ogg
Remove-Item Env:CORALINE_VOICE_MANIFEST
.\gradlew.bat -PgameTests -PwelcomeTests -PfreshGameTests runGameTestServer
.\gradlew.bat clean build
```

Generation skips existing masters. A confirmed defective take can be replaced with
`--state activities/other_village --line 1 --replace --seed-offset 1000` (or an
`activity_wait/<state>` group). Do not run multiple generators or memory-heavy
Gradle tasks while IndexTTS is loaded on this machine. Failed scratch generations
stay outside final asset folders. Validation reports transcriptions and reference
speaker similarity; inspect any nonzero transcription differences before delivery.

## Code changes

- Added `OtherMotherActivities.java`, the manifest, recordings/reports and this guide.
- Extended the existing voice scripts with per-pool filename prefixes and separate
  report names; default welcome/loot/tutorial workflows remain unchanged.
- Extended `OtherMotherDialogue` pool registration and playback guards; existing
  `VisitAudio` and `VisitData` loops handle sounds and history automatically.
- Extended `FamilyVisitReset` cancellation and Forge dialogue tests.
- Updated Gradle sequential imports, version 1.13.4, sound/duration JSON, synthetic
  audio notice and README. No actual activity rewards or relationship math changed.

The existing state-extra WAV folder was found renamed to `loot_given_state` before
this batch. The loot manifest now explicitly maps those physical source folders
while preserving the existing `state_extra` sound IDs/resource paths/save history.
No recordings are moved back, recreated, or overwritten for this rename.

## Audio verification — 2026-09-30

The final set contains exactly 44 proposals and 15 warnings, totaling 556.93 seconds
(9 minutes 17 seconds), with individual durations from 6.12 to 15.39 seconds.
All 59 PCM masters and streamed Oggs decode. All pass waveform/clipping checks and
CAMPPlus reference-speaker screening (similarity 0.660–0.880; no low-similarity flags).
The reference checksum and 41 earlier master recordings remain unchanged.

Three recordings received one replacement take each after repeated ASR differences:
the first mice-circus proposal, first cake-making proposal and first high-suspicion/
low-trust warning. Final mice/warning transcripts match exactly. The cake line's
full-context ASR still confused “set/said” and “than/and”; independent excerpt
decoding recovered the requested words correctly without an expected-text prompt.
The other differences are compound spelling, digit formatting or an initial
recognizer contraction resolved by beam-search decoding. See
`activity_transcript_review.json` and `tools/review_voice_transcripts.py` for evidence.
Raw ASR scores are retained rather than adjusted to hide these differences.

These are automated technical checks, not human listening approval of emotional
performance. No claim is made that speaker similarity proves acting quality.
Temporary/rejected takes are not retained in the final activity folders.

All 13 Forge dialogue GameTests passed, covering all registered audio resources,
pool counts/non-repetition, saved histories, current-state routing, full-duration
proposal/warning timing, the 6,000-tick window, timely/late arrivals and one-shot
completion behavior. These tests exercise the dialogue/timer mechanism, not
nonexistent activity arenas or an in-game human listening session.
