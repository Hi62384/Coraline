# Other Mother tutorial / house rules

Four final masters live at `voicelines/othermother/tutorial/tutorial_01.wav` through
`tutorial_04.wav`. Exact scripts and direction are in `tools/other_mother_tutorial.json`.
The existing local IndexTTS2 model, sole `Sample.wav` reference, decoding settings,
CPU reference encoder/GPU synthesis, light silence trimming and constant-gain
post-processing are reused. Format remains mono 22,050 Hz signed 16-bit PCM.
No new dependencies, music, effects, reverb, pitch shifting or ambience.

The affectionate/calm profile is the existing `[0.38,0,0,0,0,0,0,0.68]` vector at
temperature 0.72. The model's coarse clip-wide emotion controls and the dialogue's
wording/punctuation provide the intended warm parental direction. They do not
guarantee phrase-by-phrase acting shifts; a human listening review is still needed.
No other voice references or text were inserted into the spoken scripts.

## Runtime

`OtherMotherTutorial.POOL` is one shared four-choice pool. It does not read trust,
suspicion, loot theme, or quality. The previous choice is saved under `tutorial`
in the existing family-scoped dialogue history, and is excluded from the next
random choice. Family replacement clears it through the existing scene reset.

At the caller's tutorial stage, after the loot AFTER clip finishes:

```java
boolean started = OtherMotherTutorial.begin(player, activityProposalCallback);
```

The callback is a `Consumer<ServerPlayer>` that starts the actual activity proposal.
False means busy/ineligible: no playback or callback was started; remain at the
same stage and retry if appropriate. True means the caller should wait, not call
again each tick. A repeated call while pending returns false.

The shared voice cooldown prevents interrupting the welcome or loot comments.
The continuation runs once on the server player tick at the end of the complete
clip duration, without an added pause. The tutorial guard is removed before the
callback so it can immediately start the next recording. Another loot/extra line
cannot interrupt while this continuation is pending. Leaving the realm, entering
the tunnel, dying, losing the mother, changing families, logging out, or stopping
the server cancels the pending continuation. Do not retain gameplay progress in
the callback: a future story sequencer must manage its own saved stage and resume
policy. The last selected variation persists; the runtime callback does not.

**Integration update in 1.13.4:** activity proposals and state warnings now exist.
`OtherMotherActivities.beginAfterTutorial` connects this tutorial to the proposal,
warning and five-minute window; see `OTHER_MOTHER_ACTIVITY_AUDIO.md`. The actual
repeat-visit loot loop, activity arenas and activity portals are still not implemented.
These recordings are not inserted into the unrelated first-visit piano/supper quest
(which would also contradict the free-to-leave text). No rewards, activity gameplay,
relationship calculations or escape rules change.

## Reproduce

```powershell
$env:CORALINE_VOICE_MANIFEST='tools/other_mother_tutorial.json'
& '.voice-tools/index-tts/.venv/Scripts/python.exe' tools/generate_other_mother_welcome.py
python tools/import_welcome_audio.py
& '.voice-tools/index-tts/.venv/Scripts/python.exe' tools/check_welcome_audio.py --transcribe --speaker-check --ogg
Remove-Item Env:CORALINE_VOICE_MANIFEST
.\gradlew.bat -PgameTests -PwelcomeTests -PfreshGameTests runGameTestServer
.\gradlew.bat clean build
```

Use `--line 4 --replace --seed-offset 1000` only for a confirmed defective take.
All four clips share a single unnamed manifest group, so no `--state` is needed.
Do not run multiple generators or memory-heavy builds while IndexTTS is loaded.
Generation and validation reports are stored alongside the tutorial masters.
Model-output license and synthetic-audio notice are included with the WAVs and JAR.

## Files changed

- Added tutorial manifest, four WAV masters, four streamed Ogg conversions,
  reports, this guide, and `OtherMotherTutorial.java`.
- Extended the existing generator/checker/importer for a root-level four-file pool.
- Extended `OtherMotherDialogue.java` with the pool and pending-playback guard;
  registration and saved history reuse existing loops without new state fields.
- `FamilyVisitReset.java` cancels pending tutorial callbacks on reset.
- Extended `WelcomeAudioGameTests.java` for pool resources, selection, persistence,
  exact completion timing and cancellation.
- Updated Gradle importing, release version, sound/duration resources, README and
  the synthetic-audio notice. No prior voice masters are regenerated.

## Audio verification — 2026-09-30

All four clips succeeded on their first take. Durations are 19.07, 16.11, 17.96 and
14.47 seconds (67.60 seconds total). All WAV and Ogg files decode. Local Whisper
recovered every requested word: normalized word error rate zero for all four.
CAMPPlus reference similarity was 0.812–0.839, with no identity-review flags.
These automated results do not substitute for listening approval of the subtle
warm-to-controlling performance. The original reference checksum and all 37 prior
welcome/loot/state-extra master hashes were verified unchanged.

The first long recording took approximately eight minutes to synthesize on the
6 GB GPU; later recordings were faster. No failed takes or alternate recordings
were left in the tutorial output folder. Builds were kept separate from inference
to avoid the memory contention encountered in the previous batch.

All ten Forge dialogue GameTests passed, including resource registration,
non-repetition, saved history, one-shot completion at the deadline, and cancelled
continuation behavior. This verifies the continuation mechanism, not a complete
activity gameplay sequence, which is not yet implemented in the project.
