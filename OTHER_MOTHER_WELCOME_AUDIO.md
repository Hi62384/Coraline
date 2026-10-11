# Other Mother welcome audio

These are synthetic voice-cloned performances, not recordings by the original
speaker. The only supplied voice reference is `voicelines/othermother/Sample.wav`.
The scripts and intended acting are in `tools/other_mother_welcome.json`.

## Local generation

- Engine: IndexTTS2, official code revision
  `d9e41aac89fd00b3d71497fddb287b7f24613712`.
- Main model revision: `740dcaff396282ffb241903d150ac011cd4b1ede`.
- Isolated environment: `.voice-tools/index-tts/.venv` (Python 3.11,
  PyTorch 2.8 CUDA 12.8, torchaudio 2.8, Transformers 4.52.1).
- Dependencies and model weights stay outside the mod JAR. The system Python
  installation is not changed. `.voice-tools` is ignored by version control.
- No audio is uploaded. Models/packages are downloaded from their official
  repositories; synthesis and speech-recognition checks run locally.
- IndexTTS uses the first 15 seconds of the reference. The complete original
  16.05-second file is retained unchanged and checked against SHA-256
  `154985720c45577f3c985dfea91b57bbf278caf1e3ff4077384f16d1e4ac3afe`.
- Explicit emotion vectors distinguish the five performances without another
  reference voice. The direction text is documentation, not a hidden prompt that
  gets spoken. IndexTTS's eight emotion controls cannot guarantee every nuance
  of possessiveness or restrained menace; human listening review remains important.
- On a 6 GB GPU the full-precision reference encoder runs on CPU. Reference
  features are cached; speech generation remains GPU-accelerated.

Run from the project directory:

```powershell
.\.voice-tools\index-tts\.venv\Scripts\python.exe -u tools/generate_other_mother_welcome.py
.\.voice-tools\index-tts\.venv\Scripts\python.exe -u tools/check_welcome_audio.py --transcribe --speaker-check
python tools/import_welcome_audio.py
```

Existing final WAVs are preserved unless `--replace` is explicitly supplied.
Use `--state <state_id> --line <1..3> --replace --seed-offset 1` for a targeted retry. There are
at most two automated attempts for a failed synthesis. Raw scratch audio stays
outside the final folders.

Masters are mono 22050 Hz signed 16-bit PCM WAV. Processing is limited to edge
silence trimming with handles, constant gain, headroom and short silent padding.
No pitch shifting, time stretching, compression, music or effects are added.
`generation_report.json` records settings, checksums, duration, gain and attempts;
`validation_report.json` records decoding/transcription and reference-speaker
embedding checks. These are screening tools, not proof of acting quality or
perceptually identical voice identity.

## Minecraft integration

Minecraft 1.20.1 uses Ogg/Vorbis sound resources, not the PCM master WAVs. The build
imports all fifteen masters into `assets/coraline/sounds/visit/welcome/`, registers
streamed sound events in `sounds.json`, and merges durations into
`visit_audio_lengths.json`. Generation is never run by Gradle or in Minecraft.

`OtherMotherWelcome.State` contains five pools of three clips. `choose` randomly
selects from the active pool while excluding the immediately preceding clip.
`VisitData.Scene` saves the selected state, previous sound and playback cooldown.
The existing family reset discards these fields with the family's scene.

Actual tunnel exit calls the welcome system **only after the first-visit story
has been completed**, and only for that player's matching realm with its Other
Mother present. Staging, the tunnel, other dimensions, another family's realm,
and unfinished first-visit dialogue do not trigger it. Playback reuses the existing
non-attenuating VOICE channel, so distance does not make the line fade away.

There is no Trustness/Suspicion progression system in the inspected project.
Accordingly, no score thresholds, relationship changes or storyline rules were
invented. Until an external relationship system selects another state, the
friendly `LOW_TRUST_LOW_SUSPICION` pool is active. The integration point is:

```java
OtherMotherWelcome.setState(player, OtherMotherWelcome.State.HIGH_TRUST_HIGH_SUSPICION);
```

This changes only the audio pool for subsequent welcomes. It returns false if the
player has no family visit scene yet; it does not create a scene or alter progress.

## Verification

All 15 masters passed PCM decoding, duration/headroom checks, local Whisper
transcription and CAMPPlus reference-speaker screening. Transcription recovered
the supplied words (one benign "anymore" / "any more" spelling split). Clip
durations range from 3.76 to 10.96 seconds, totaling approximately 104 seconds.
All 15 Ogg game resources were also decoded successfully. The reference checksum
is unchanged. These checks do not constitute a human audition of the acting.

Six Forge GameTests passed: pool selection without repeats, saved-state roundtrip,
legacy defaults, replacement-family reset, first-visit/realm/cooldown protection,
and registration/resource/duration coverage. Re-run with:

```powershell
.\gradlew.bat -PgameTests -PwelcomeTests -PfreshGameTests runGameTestServer
.\gradlew.bat clean build
```

The clean release build excludes the test classes. Full in-game listening and
approval of the emotional performances remain manual checks.

## Model notice

The IndexTTS license accompanying these generated outputs is included as
`META-INF/INDEXTTS_AUDIO_LICENSE.txt` in the JAR. Keep it with redistributed audio.
No model weights are shipped in the mod.

Any modifications made to the original model in this Derivative Work are not
endorsed, warranted, or guaranteed by the original right-holder of the original
model, and the original right-holder disclaims all liability related to this
Derivative Work.
