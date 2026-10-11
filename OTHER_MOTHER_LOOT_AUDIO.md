# Other Mother loot and state-extra dialogue

Uses the existing local IndexTTS2 installation, pinned model, reference and processing
from `OTHER_MOTHER_WELCOME_AUDIO.md`. No new dependencies or reference voices.
`Sample.wav` is checksum-guarded and is never written. Existing welcome masters
are preserved. All new masters are mono 22,050 Hz signed 16-bit PCM WAV.

## Assets

- `voicelines/othermother/loot/food/{before,after}/line_01.wav` through `line_03.wav`
- `voicelines/othermother/loot/treasure/{before,after}/line_01.wav` through `line_03.wav`
- `voicelines/othermother/state_extra/<state>/line_01.wav` and `line_02.wav`, for each
  of the five existing welcome states.
- Exact dialogue, direction and model parameters: `tools/other_mother_loot.json`.
- Generation and validation reports: `voicelines/othermother/` (welcome reports
  remain separately under `welcome/`).
- Game conversions: `assets/coraline/sounds/visit/other_mother/` with matching paths.

State-extra profiles reuse the exact welcome emotion vectors and temperatures.
Food and treasure profiles use restrained happy/calm blends. The model's eight
coarse emotion controls cannot guarantee fine acting such as passive aggression;
human audition remains necessary. Direction notes are not spoken or supplied as
additional dialogue. No reverb, pitch shifting, sound effects, music or ambience.
The existing conservative silence handles, constant gain and -1.5 dB peak ceiling
are unchanged.

## Runtime hooks

`OtherMotherDialogue.playLoot(player, Loot.FOOD, Moment.BEFORE)` selects and plays
one of three food-before lines. `TREASURE` and `AFTER` select the other pools.
It returns duration in server ticks, or zero if ineligible/busy. The reward caller
must wait that duration before granting its reward and requesting AFTER. If zero
is returned, do not interpret it as completion; retry later if appropriate.
`playStateExtra(player)` optionally plays one of two lines for `scene.welcomeState`
after the prior line finishes. Extra lines are not automatically appended.

Each pool remembers its previous line in family-scoped saved data and excludes
it next time, including across reloads. Family reset removes this history.
Playback uses existing player-relative voice audio and shares the welcome
cooldown to prevent overlaps. First-visit scenes and tunnel playback are excluded;
the player's assigned Other Mother must be alive and loaded in the Other World.

There is currently no Other Mother food/treasure gifting workflow or implemented
Trustness/Suspicion calculation in this project. These are ready-to-call audio
hooks, not a new reward mechanic: nothing automatically grants loot or triggers
them during normal-parent chores or supper. The selected welcome state remains
the source of truth, defaulting to low-trust/low-suspicion. No thresholds are added.

## Reproduction (PowerShell, project root)

```powershell
$env:CORALINE_VOICE_MANIFEST='tools/other_mother_loot.json'
& '.voice-tools/index-tts/.venv/Scripts/python.exe' tools/generate_other_mother_welcome.py
python tools/import_welcome_audio.py
& '.voice-tools/index-tts/.venv/Scripts/python.exe' tools/check_welcome_audio.py --transcribe --speaker-check --ogg
Remove-Item Env:CORALINE_VOICE_MANIFEST
.\gradlew.bat clean build
```

Generation skips existing masters. Use `--state loot/food/before --line 1 --replace --seed-offset 1000`
only for a confirmed bad take. Validation uses local
Whisper transcription and CAMPPlus speaker similarity as objective screens, not
as proof of acting quality. The importer checks hashes, converts and decodes every
Ogg and merges sound durations/registrations. Gradle imports welcome then loot
sequentially to avoid concurrent resource edits. Never run two generators together:
they share a scratch file and the GPU.

The existing IndexTTS output license and AI-audio notice are distributed alongside
these recordings and in the mod JAR; model weights are not bundled.

## Audio verification, 2026-09-30

All 22 clips generated successfully on their first take. Total duration is 125.28
seconds; individual clips range from 3.61 to 7.45 seconds. All WAVs and Oggs decode.
Local Whisper base.en recovered every requested word (normalized word error rate
zero on all 22). CAMPPlus reference similarities range from 0.606 to 0.878 with no
low-similarity flags. These are technical screens, not a human listening approval.
All 15 existing welcome master hashes and the reference checksum remain unchanged.

An early simultaneous Gradle compilation ran out of native memory while IndexTTS
was resident. Build/test work was moved after generation to release that memory;
the voice generation itself was not interrupted. No new dependencies were needed.

Eight Forge GameTests passed, including the existing welcome tests plus new
resource/selection and history-persistence tests for all nine added pools.

## Changed code

- Added `tools/other_mother_loot.json` and `OtherMotherDialogue.java`.
- Extended the existing generator, importer and checker with an optional manifest;
  their default welcome workflow is unchanged.
- Extended `VisitAudio.java` registration and `VisitData.java` saved pool history.
- Extended `WelcomeAudioGameTests.java` with loot/extra coverage.
- Updated Gradle resource importing, version 1.13.2, sound/duration JSON,
  AI-audio notice and README. No reward or Trustness/Suspicion logic was changed.
