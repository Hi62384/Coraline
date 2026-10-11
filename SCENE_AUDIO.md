# Mouse and tunnel scenes (1.8.0)

The four requested recordings in `GameMusics` are converted to streaming Ogg assets by `tools/import_game_audio.py`. Sources remain unchanged. The unrelated bed song is not used. The script also creates a short mouse squeak and a quiet, seamless synthesized tunnel hum.

- First successful nightly roll with each parent pair starts the intro. Its measured duration is 465 ticks; mice are scheduled 100 ticks later. The scene waits if the owner temporarily leaves the bed or eligibility is lost. Parent death/door destruction cancels the pending event. Milestones persist across saves.
- Creative Mouse Debug continues to bypass the song delay. Mice wait until their owner has an unobstructed view within 12 blocks, looking within about 23 degrees of them for 8 consecutive ticks. One spotted mouse starts the nearby group and plays the chase recording once.
- Navigation speed defaults to 0.7 (formerly 1.2). Mice squeak at spawn, discovery, and occasionally during the chase. Arrival closes the door and activates its portal.
- The source-side portal opening cue is limited to once per door/night. Tunnel entry is once per visitor/door/night. The quiet client-only hum loops in the tunnel and stops on exit, death or disconnect. Scene recordings use the Jukebox/Note Blocks slider; hum uses Ambient/Environment.
- All delays use Minecraft ticks (20 ticks per second at normal server speed). An unloaded/offline owner's pending event resumes after reconnect; audio playback itself is not resumed mid-recording.

New common settings: `otherWorld.mouseChaseSpeed`, `mouseSeenConfirmationTicks`, `mouseSeenRange`, `mouseIntroDelayTicks`, `sceneVolume`, `tunnelHumVolume`, and `doll.creativeMissingConfirmationTicks`.

Mouse eligibility now verifies actual bound doll block entities near the real respawn bed and repairs stale location data. It does not accept another player's doll or a retired generation. Failures report the bed coordinates and allowed radius/height.

Creative deletion does not remove the haunting bond. A missing doll is confirmed in about five seconds and its replacement is scheduled two seconds later. The retired generation becomes invalid when the replacement arrives. Survival/container recovery retains its existing timeout.

Little Door gifts only roll while the doll is placed at home. The door drops onto a free supported space beside the doll; it is never inserted into inventory. The existing tracked-item despawn/destruction handling releases that door identity, allowing another random drop. Existing doors in inventory, storage or placed in the world still prevent duplicates.
