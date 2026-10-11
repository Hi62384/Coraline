# Other Father activity introductions — 1.14.6

The existing local IndexTTS2 pipeline generates one finalized Other Father introduction for each of the 22 activity IDs. The synthetic reference is `voicelines/otherfather/Otherfathersample.wav`; its SHA-256 is pinned in `tools/other_father_activity_intros.json` and checked before and after every generation or validation run.

Each source folder at `voicelines/otherfather/activities/<activity>/` contains exactly one PCM-16, mono, 22.05 kHz `intro.wav`. Minecraft receives a streamed Vorbis resource at `sounds/visit/other_father/activities/<activity>/intro.ogg`. Sound IDs follow `other_father_activity_<activity>_intro`, and captions identify the speaker as Other Father.

`ActivityWorld.enter` plays the selected activity's sound after the saved loop phase changes from `READY` to `ACTIVITY`. The phase guard makes playback once-per-activity-session. Existing Other Mother proposal, warning, arrival, and completion audio is unchanged. Magic Garden's later dialogue remains separate.

Generation uses explicit emotion vectors, deterministic seeds, native pacing, silence trimming with natural handles, active-speech gain normalization, and a -1.5 dBFS peak ceiling. Four first attempts were rejected for raw synthesis clipping and regenerated successfully: Grand Feast, Mice Circus, Snowball Battle, and Other Nether Fortress. All final WAV and Ogg files decode, all transcripts passed with at most 4.5% word error, and reference-speaker cosine scores ranged from 0.728 to 0.816. The reference checksum remained unchanged.

Verification command: `gradlew.bat runGameTestServer -PgameTests -PwelcomeTests -PfreshGameTests --offline`. All 39 required tests passed. Objective audio checks cannot replace a final human listening review for acting preference.
