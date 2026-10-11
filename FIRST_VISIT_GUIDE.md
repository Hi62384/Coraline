# First Other World visit — 1.9.3

## 1.9.3 fixes and dialogue

Verification: all 61 server GameTests passed. The opt-in client check confirmed Mysterious Supper remains active at the cake reveal and See You Soon plays during sleep before returning home. One earlier run hit the existing intermittent normal-parent delivery test; the final fresh-world run passed it as well.

- Player chair movement no longer overrides camera yaw/pitch. Seat recovery selects only the owner's player chair, never a parent's reserved chair.
- Parents walk to their dinner seats; if stalled for 10 seconds (or still travelling after 20 seconds), seating can recover directly to the assigned chair after collision/fluid/occupancy checks. Configure `supperSeatStallRecoveryTicks` and `supperSeatMaximumTravelTicks` (capped at 400 ticks). Missing chairs from old saved scenes are repaired without changing blocks.
- `OtherMotherSupper0`, `1`, and `2` play in order after Dad's golden-chicken line. There is a configurable five-second pause between recordings (`supperDialogueGapTicks`). The cake waits until all dialogue has completed even if `feastTicks` is reduced.
- The food-grabbing phase remains one minute by default. `MysteriousSupper` continues naturally through the cake reveal and final dialogue, without restarting. Sleeping early waits for the remaining soundtrack before returning home, because dimension changes stop Minecraft sounds.
- `SeeYouSoon.wav` plays when the player starts sleeping in the correct Other World bed. Sleep lasts until the full recording finishes before teleporting (Minecraft clears sounds during dimension changes). Disconnecting or dying stops it; repeated clicks while sleeping do not restart it.

The door owner receives this introduction once per player, per save. Existing saves with no introduction record receive it on their next visit. Other players are not locked into someone else's scene. Later visits use the existing wandering Other Parents behavior.

## Sequence

1. Other Mother approaches, watches the player and plays her introduction. The Little Door and early sleeping are locked.
2. Follow Other Father's outline to his piano. Entering its radius starts the scene: entrance recording, introduction, mechanical hands, spinning piano/dancing performance, then dinner announcement. The radius is enforced server-side until the announcement finishes.
3. Follow Other Mother back to the dinner table and right-click the chair. The chair holds the player for the feast. Supper music starts; the chicken-breast line follows five seconds later.
4. For sixty seconds, right-click the packed plates and glasses to take individual food portions. Empty slots replenish periodically. An empty plate accepts one held food item; an empty glass accepts a feast drink. Inventory-full checks prevent losing portions.
5. Plates clear and the strawberry cake slides forward. After three seconds it can be collected, and the waiting-for-you recording plays. The farewell recording then releases the chair and marks the player's copied bed with lights.
6. Both parents follow and stare. Sleep in the correct copied bed to wake at home in the morning, with the portal closed. Ordinary house doors/gates remain usable so the bed is reachable.

## Food and cake

The feast includes the requested vanilla foods plus eleven new foods/drinks. They also appear in the Coraline creative tab. Golden Chicken Breast has the requested quotation. Drinks use glass artwork and the drinking animation.

Welcome Home Strawberry Cake can only be placed in the Overworld. It has seven servings. Breaking it in survival drops a block item with `CakeBites` NBT; placing that item restores the eaten state. Its tooltip reports remaining servings. Like vanilla creative breaking, creative destruction does not produce a survival loot drop.

## Configuration

`config/coraline-first-visit.toml` contains feast duration, refill interval, cake reveal delay, piano search distances, confinement radius, music volume and voice volume. Durations are game ticks (20 ticks = one second). Recording lengths are measured automatically from the original audio during the build, rather than guessed.

## Persistence and safety

Progress and furniture persist across saves. Rejoining resumes the current scene; a partially played voice restarts instead of skipping its ending. Supper retains its elapsed game time. An interrupted welcome does not force a new world copy over its actors. A missing bed, revoked door or lost family returns the player home safely and permits a later retry.

Furniture is a small set of decorative entities; it does not replace copied blocks, machines or inventories. There is no extra normal mob spawning. Existing block protection remains active. The source house is not remodeled.

Startup now retries automatically (including when loading a save already inside the Other World) until the actors and furniture locations are ready. Completed introductions do not replay. Placement checks the actual furniture footprints, allows short grass and small ground-height differences, searches up/down around the entrance, and keeps the piano away from the table. Other Parents can open ordinary wooden doors during the scene. If no safe reachable space exists, an on-screen explanation and a log message identify what is missing; retries continue without trapping the player or changing copied blocks.

`startupRetryTicks` (default 100), `placementSearchAttempts` (512), and `placementVerticalRange` (16) control the bounded search. No full-world scan runs every tick. Audio sequencing is tick-based, so a heavily overloaded server may delay transitions relative to sound playback.

Vanilla background music is stopped when a story recording begins and new vanilla tracks are blocked while it plays. This covers the first-visit dialogue/songs and the earlier mice/door/tunnel recordings. Custom recordings and volume sliders are not muted; normal music is allowed again once playback finishes.

## Development and assets

Main code: `com.coraline.feast` (`FirstVisit`, `VisitData`, `VisitConfig`, `VisitAudio`, `ScenePropEntity`, food/cake classes). Client presentation: `ScenePropRenderer`, the piano pose in `ParentRenderer`, and `VisitAudioClient`. Integration is limited to the existing portal, parent, registry and realm-protection hooks.

`tools/import_visit_audio.py` converts the supplied WAV/MP3 recordings to streamed OGG and records their lengths. Originals are retained. `tools/build_feast_resources.py` exports artwork/model resources; generation prompts and saved art paths are in `artwork/FEAST_ART.md`.

Run `gradlew.bat -PgameTests -PfreshGameTests runGameTestServer` for regressions in a new disposable test directory. The opt-in `-PrenderChecks -PfeastVisualCheck runClient` harness uses only the disposable `CoralineVisualQA170` world and captures `artwork/first_visit_1.png` through `first_visit_3.png`. Test/harness classes are excluded from a normal clean release build.

## Verification (2026-09-29, 1.9.1)

### 1.9.2 presentation update

The entrance now stages the piano and parents from inside the tunnel before allowing the owner to exit into the copied house. Dialogue stays paused until arrival. Safe-space failures remain retryable without exposing the unfinished scene. New scenes use `pianoStagingMinimumDistance = 40` and `pianoStagingMaximumDistance = 72`; already saved scenes retain their original placement.

Scene recordings are listener-relative with no distance attenuation. First-visit songs use **Jukebox/Note Blocks**, dialogue uses **Voice/Speech**, and vanilla background music is still suppressed. Dialogue assets are loudness-normalized to -14 LUFS with a -1.5 dB true-peak ceiling; originals and song mixes are unchanged. Master/category sliders still apply.

The piano body is reddish stripped mangrove wood with black/white keys. Dinner has three red-upholstered, gold-trimmed high-back chairs. The northern chair is the player's; the two opposite chairs are reserved for the parents, who walk to their seats and leave them at bedtime. Furniture remains non-destructive decoration.

The dinner table now uses an ivory cloth, a green runner, gold candleholders, porcelain place settings, silverware and three drinks. Four shared platters contain separate roast/fish, vegetable-side, fruit and dessert menus. Personal plates can receive food but do not refill themselves. Each serving platter/drink refills only one depleted slot every 15–25 seconds, independently (`servingRefillMinimumTicks` and `servingRefillRandomExtraTicks`). Supper still lasts a minute, but there is no countdown; the interaction hint clears after ten seconds.

The live client test verified both `other_father_song` and `mysterious_supper` remain active with the vanilla Music slider at zero, and verified listener-relative scene audio plus vanilla music suppression. Full-duration listening and multiplayer playback still need manual play-testing.

Final 1.9.2 server regression: all 58 GameTests passed in a fresh test world, including separate Dad/Mom death timing, full parent dialogue rotation, themed platters, three chairs, entrance staging and the copied-world protections.

- All 56 server GameTests passed in a fresh test world, including first-visit sequencing, delayed-parent startup retries on uneven ground, collision-safe furniture placement, plate ownership/transfer, save/load and cake bite-state drops. The older reused test world still exhibited its intermittent parent-delivery fixture failure; no normal-parent task behavior was changed for this patch.
- The client music guard check passed: existing vanilla music stops, attempted vanilla restarts are blocked, story sound is retained and suppression expires after playback (`CORALINE_STORY_MUSIC_GUARD_OK`).
- A connected-player client check reached every first-visit stage and completed the seat-release/ending flow (`CORALINE_FIRST_VISIT_RENDER_AND_FLOW_OK`).
- In-game screenshots checked piano seating/lid, crowded dinner table, custom food icons, and readable cake lettering from the dining seat.
- The existing copy/protection/mob restrictions and 224-block-radius copy regression remained passing. No furniture placement edits copied blocks or inventories.
- Full-duration audio playback with real multiplayer clients has not been manually play-tested; recordings are imported and timed from measured file lengths. Arbitrary modded terrain/pathfinding combinations still need player testing.

## Repeat-visit loop (1.14.0)

1.14.1 final verification: all 50 focused audio, parent and loop GameTests passed in a fresh world. Tests include repeated deliveries across busy periods, physical portal closure only after returning home, tunnel turn-around without restarting the visit, physical gift pickup without a GUI, saved reward ownership, second-visit activation, debug completion and subtitle coverage. A real-client disposable-world check visually verified the hollow gift box with visible contents and the caption overlay (`artwork/giftbox_story_subtitles.png`).

### Creative first-visit shortcut (1.14.1)

Find **First Visit Complete Debug** (green book icon) in the Coraline Creative tab, or use `/give @s coraline:first_visit_debug`. Right-click in the air or on a block. It requires your living claimed parents and bound Little Door, works before your first entry, and safely releases an in-progress supper scene without giving food/cake rewards. It records the introduction as complete so your next entry starts the main loop. Repeated use leaves an active loop and relationship values unchanged. Completion belongs to the current family and resets when those parents die.

### Loop behavior

The 1.14.1 regression run passed all 28 targeted tests, including Creative-only pre-entry completion, saved completion, preservation of an active loop, and release from the supper chair without rewards.

The main loop starts on visit **two**, immediately after the first-visit story. The Other Father is retired on subsequent visits. Completed saves already inside the Other World start the loop automatically. Counts and relationships reset with the claimed family.

The sequence is welcome → food/treasure before line → collect every gift → after line → optional state comment (50%) → tutorial → random activity proposal → warning. Audio uses the existing 145-recording pipeline and measured durations, with no overlapping phase transitions. The feast reuses themed platters, drinks, candles and the decorated table. Treasure boxes open to reveal physical piles of ingots, coal, lapis, redstone, emeralds, diamonds and Other Iron/Other Diamonds inside the box. Right-click the open box to collect one stack per click, without an inventory screen or treasure plates. Full inventories preserve unclaimed items.

The highlighted activity portal gets a **60-second debug countdown** after the warning ends. Arrival within four blocks records on-time/late and locks the player within six blocks during the greeting. The portal becomes usable only after that recording ends. All 22 activities currently share a protected, family-isolated platform in `coraline:activities`; the selected activity is retained, but no activity gameplay is implemented yet. The exit returns to the previous portal position, removes that portal, plays completion dialogue, and begins another invitation at a new location.

During the countdown (including after it expires), the player may crawl home through the existing tunnel or sleep in their copied respawn bed. Sleep advances the real world to morning. Turning around inside the return tunnel preserves the active visit. Reaching the Overworld closes the physical Little Door and its portal for that night. Unused portals also expire at dawn, including skipped nights; the mouse introduction can repeat until the family's first actual entry. Little Doors can open and close during locked dialogue, but travel remains locked. `/coraline guide` supports gifts and activity portals. Three failed route checks five seconds apart automatically relocate a stuck player safely near their quest. A separate movement watchdog catches repeated pacing without progress. Recovery never creates a rescue door or skips story stages. There is no chat teleport-home button or `/coraline escape` command. Copied containers and builds are not replaced.

The Other Mother faces her route while walking to the activity portal, then watches the player from a checked waiting position beside it. The portal uses a bounded client mesh: an oval blue aperture, rotating spiral threads, stitched luminous rims and orbiting sparks. It does not place decorative blocks or spawn additional entities.

### Story subtitles

Story dialogue automatically displays wrapped captions above the hotbar, including Wybie, the Other Parents' spoken dialogue, farewell and every generated repeat-visit dialogue pool (159 recordings). The Other Father's piano song has no subtitles. Normal parents have no dialogue subtitles, including their death recordings. Captions are tied to playback and disappear when the recording stops. Existing scripts supply repeat-visit text; legacy recordings use cached local speech recognition transcripts. Long lines are split into readable timed chunks. Scripted captions do not require enabling vanilla's sound-effect subtitles. F1 hides them with the rest of the HUD. Legacy transcription and fine-grained timing may need manual polish; this does not alter audio.

Trust and suspicion persist from 0–1 and appear above the Other Mother with the selected state. No automatic gain/loss rules have been invented. Operators can test values with `/coraline relationship <trust> <suspicion>`. Both values in 0.35–0.65 select the middle state; outside that central region, each value below/above 0.5 selects the corresponding low/high quadrant. New families start at 0/0.

Saved reward remainders, gift-box state, selected activity and attendance deadline survive ordinary reloads. Interrupted dialogue restarts its current line on login; the attendance deadline does not reset. Family reset removes the session, meters, visit count and props. Runtime bars are removed on logout/server stop. Block placement/breaking and normal mob spawning are also blocked in the placeholder dimension.

Regression command: `gradlew.bat runGameTestServer -PgameTests -PwelcomeTests -PfreshGameTests --offline`. A normal `clean build` excludes test classes. Client visual appearance and a complete real-player playthrough still need manual confirmation.

1.14.5 verification: all 39 targeted GameTests passed, including blocked-route automatic recovery, movement-watchdog behavior, retired rescue props, safe portal waiting positions, dawn expiry, locked-story door interaction, and intro replay until the first actual entry. Client portal appearance still needs in-game review.

2026-09-30 verification: all 26 targeted GameTests passed (17 existing audio tests and 9 loop tests). Coverage includes the welcome/gifts/tutorial chain, actual activity-dimension round trip and next invitation, third-visit activation, full-inventory reward handling, ownership, persisted meters/rewards, deadline boundary classification, nightly closure, family reset, activity block protection and vanilla mob rejection.
