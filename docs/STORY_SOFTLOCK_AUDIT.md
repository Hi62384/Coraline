# Coraline story softlock audit

## Follow-up — November 20, 2026: canonical Cat revalidation and older saved final-room recovery

This pass re-inspected the complete rejected-button-eye first-escape-to-post-boss route: Wybie's single saved shoe and bicycle, temporary Spink/Forcible tea and original Seeing Stone, Cat/mirror, the authenticated original return tunnel, parent-rescue kitchen, Mantis and theatre and Ratmaster eye quests, final Other Mother Cat/web/hand/tunnel/door struggle, and the snow-covered parent reunion and ghost-child dream.

### Confirmed progression issues repaired on `main`

1. **Normal final-room Cat handoff:** `RescueEncounters.HOUSE_SAFE` previously closed its doors and committed `ROOM_LOCKED` even if `BeldamFinale.catAtMantel` returned `null`. It now commits only after the **original** Cat is recovered and staged.
2. **Older `ROOM_LOCKED` saved checkpoints:** A stored room-lock phase advanced to `FINAL_A` without checking the Cat, original three ghost-eye claims or Cat rescue flags. The phase now rechecks these prerequisites, reopens the real doors if one is missing, and requires the real player to stand inside the room before it seals and advances. It does not create actors, add ghost eyes or change quest receipts.
3. **Recovery intercepted by early player movement:** `RescueChapter.tick` used to force owners back into `ROOM_LOCKED` before the save recovery could run. The checkpoint now handles its own re-entry and unlock. The genuine `FINAL_A` through `FINAL_O` fight still runs its own locked-room check.
4. **Stale `CatMantel` flag:** Old progress might claim the original Cat is staged although it is physically elsewhere. `BeldamFinale.catAtMantel` now repositions that same actor for the `HOUSE_SAFE` or `ROOM_LOCKED` handoff; it never snaps a carried or airborne Cat during `CAT_THROW`.

### Checks performed

- Confirmed **178 enumerated checkpoint labels**: First Escape 52; tea 18; Cat 15; parent-rescue 23; rescue/finale/epilogue 70.
- Verified **88 voiced checkpoint audio keys plus the Cat intro song** have sound registration and timing metadata.
- Decompressed the exact shipped Rosewood house blueprint: 32,438 authored block entries; confirmed both real kitchen ovens and kitchen chairs, and available authored roof material near Bobinsky's original swing site. This does not verify NPC collision/pathfinding in-game.
- Recompiled the five source-hash-matched original Java timing/geometry helpers at Java 17 language level and ran **1,634,693 deterministic assertions**. These are standalone source-driven simulations, **not a Minecraft/Forge gameplay test**.
- Verified 11 targeted source-level requirements against the new `main` implementations and added source regression coverage for both ordinary and legacy locked-room handoffs.
- Merged former PR #3 and committed the supplementary stale-mantel fix to `main`.

**Unverified:** Native Forge compilation, client input, actual entity collision and actor pathfinding, chunk save/reload, server restarts, every bossfight encounter, and the complete post-boss dream were **not executed** in Minecraft during this pass. The Windows native runner was disconnected and the repository had no Actions runs. Previous failing Forge GameTests remain unverified; no full route is certified softlock-free.

## Follow-up — November 20, 2026: repeated full-route review

Reviewed the rejected-button-eye First Escape, original Wybie shoe/throw and bicycle departure, temporary Spink/Forcible visit, Cat/mirror, original tunnel, parent return/kitchen, all three eye bosses, Ratmaster chase, final Beldam fight, physical web/tunnel/door exit, snow-covered real-parent reunion, Cat, and ghost-child dream.

### New source fixes

- **Kitchen appliance discovery:** The parent-rescue KITCHEN phase scanned for block IDs containing furnace, oven or stove, but omitted the vanilla smoker. The appliance search now explicitly recognizes smoker and protects against an unregistered/null block ID. An accepted smoker allows the original kitchen interaction to continue; it does not create or substitute new appliances.
- **Authentic tea-visitor loss:** Spink/Forcible encounter phases required their original saved actors, yet spawn() ignored any existing recorded UUID even if that actor had actually disappeared, causing the scene to wait indefinitely. Recovery now loads the saved location and examines native entity storage. Only verified ABSENT allows re-creating the temporary visitor with the SAME original UUID, owner and Tea token; PRESENT, WAITING, UNVERIFIED and any live UUID collision refuse replacement. Actor lookup additionally checks the expected type, owner and saved token.
- **Unnecessary disk saves:** The previous spawn loop saved all story data on every tick while a recorded actor was missing. Now newly created actors are checkpointed only when actually created; verified restoration saves separately.

Added focused source regressions to tools/test_current_story_repairs.py and Forge GameTests to RescueSoftlockGameTests.java for the missing-actor proof and kitchen appliance recognition. **The Forge tests were not run.**

### Re-execution and limits

- Compiled five actual dependency-free Java files copied from the current GitHub source: FirstEscapeTimeline.java, RescueChallengeMath.java, RescueWebLayout.java, StoryPlaybackClock.java and MissingParentsCatTimeline.java. Their git blob hashes matched the repository blobs. A standalone Java harness executed **5,613,900 assertions** (phase ordering, weakpoint/combat clocks, web positions, hearing/struggle rules, tunnel pursuit, recorded-time limits and Cat animation bounds); all passed.
- Rechecked the five main phase enumerations (52+18+15+23+70 = **178 checkpoints**) and **88 distinct voice events** for recorded duration and sounds.json event metadata. No missing voice keys or phase names were identified in these static checks.
- This is **not a native Minecraft playthrough**. The Windows runner was offline, no GitHub Actions runs were available, and the new Minecraft-entity recovery, forge compile, real actor movement, ray interaction, dimension crossings and post-boss cutscene still require native integration testing. A previously recorded 147-test GameTest run (131 pass / 16 fail) was not repeated here. Do not infer that all known runtime failures have been cleared.


## Follow-up audit — 2026-11-20 (missing shoe, finale floor, kitchen service)

Latest full-path inspection revisited First Escape and Wybie's thrown shoe; Miss Spink/Forcible and Cat; the physical return tunnel; parent-return kitchen; first three ghost-eye bosses and Ratmaster chase; finale Cat/web/tunnel and Little Door key; real-parent reunion and the ghost children's dream.

### Newly fixed

1. **Truly lost original shoe entity:** If the canonical shoe was removed from Minecraft entity storage, its saved UUID prevented new inventory issuance, but the shoe tick and Use retry could not find anything to interact with. The flight can now become a miss after 60 ticks of unresolved entity lookup. The game first holds/loads its saved chunk, then uses LegacyActorRecovery.check to prove that UUID is genuinely ABSENT from native entity storage. It can reconstruct exactly one shoe with the same saved UUID and story ownership only after this proof. PRESENT, WAITING, UNVERIFIED and any live UUID collision never permit replacement. Successful physical hits retire both the old flight and any stale missing-state flags.
2. **Final Other Mother floor-collapse perimeter stall:** In FINAL_O, the player can stand on surviving room edges even when the floor opening has been verified, so the stage could wait indefinitely for a fall. After the real recording and an 80-tick grace window, a bounded recovery verifies air under the center, player clearance, world border and the original dimension, then moves the real owner into the already-open falling gap. The boss's real web transfer, passage and physical exit are still required. Recovery cooldown uses persistent world time to survive chapter tick resets.
3. **Parent-rescue kitchen serving path stall:** The scene previously waited indefinitely if the original Other Mother couldn't navigate beside the kitchen table; the watchdog only repeated a blocker. After 360 normal walking ticks, a safe, collision-checked same-actor recovery may bring her to the original serving position. Progress still requires the existing physical meal prop, with a clear blocker if no safe position exists.

### Tests/evidence

- Retrieved main source and checked 52 First Escape, 18 tea, 15 Cat, 23 parent-return and 70 final-rescue checkpoint names (178 total) and critical handoff and fail-safe logic.
- Copied five exact current-repository pure Java helper classes, confirmed their git hash-object blobs against GitHub (FirstEscapeTimeline, RescueChallengeMath, RescueWebLayout, StoryPlaybackClock, MissingParentsCatTimeline), compiled with javac --release 17 and executed a deterministic runner. **1,796,194 assertions passed** for story ordering, combat beat/vulnerability windows, Cat animations, recording clocks, web route/struggle and a **modeled** lost-shoe/retry sequence. The shoe model does not run Minecraft's entity registry, storage or collision engine.
- Added source-regression assertions and Forge GameTests for the final floor-timing and kitchen-navigation-fallback gates. These new Forge GameTests are committed but were not executed.

**Not yet certified:** The Windows native command runner was offline, and the repository had no GitHub Actions runs. No Minecraft client/server gameplay simulation, forge build, chunk save/reload, real entity-discard proof, actor pathfinding or complete bossfight-to-reunion playthrough was performed. Existing historical failed native GameTests were not rerun or declared fixed.


## Further review — 2026-11-20 (Wybie shoe miss, full rescue route)

The previous shoe-miss fix was insufficient: a recovered projectile was placed near/in the owner's own feet and only a tiny 0.8 m prop could be clicked. The player could be stuck looking for the pickup even though the same UUID remained saved.

**Production fixes on main:**

- The same saved original shoe is now recoverable by pressing **Use** during THROW_SHOE after a miss (or by clicking the prop). Client input uses the current story Choice nonce; server verifies owner, scene phase, recorded UUID, world, cue and missed state. A second use while flying is rejected; no new shoe/item/UUID is created.
- Missed shoe placement checks clear ground and unobstructed view in front of the owner. When no safe placement exists, the Use input still retries the original even if its fallback ground location is awkward.
- A shot that misses into empty air is capped after 45 ticks or 24 blocks (instead of 100 ticks). Wall and below-floor misses are immediate. Only a real collision with the canonical Wybie hitbox advances to MISSING_E.
- When a saved First Escape post-tunnel checkpoint is loaded in the wrong dimension, recovery attempts the exact owner-bound original Little Door return landing before ticking the Overworld story. Later Parent Rescue sessions are explicitly exempt from this transfer.
- Native GameTest fixtures for repeated miss, wrong-nonce/repeated input, flight timeout and distance, same projectile identity and original Overworld phase now exist, along with source-level regressions.

**Independent executable tests:** five Java helper source files were copied from GitHub main, verified byte-for-byte against their Git blob SHA, compiled with javac and executed using a standalone Java audit runner: **932,894 assertions passed** for original First Escape phase rules, boss/web/tunnel timing, Cat animation and audio clock boundaries, and a *modeled* same-UUID shoe-retry state machine. A separate source-derived projectile/AABB geometry simulation covered **50,029** shot cases and checks, including world-wall occlusion, miss timeout and feasible Wybie hit angles. These are **not** Forge gameplay execution; the shoe state simulation and ray traces do not exercise Minecraft's actual engine.

**Still unverified:** real Minecraft owner/guest hit registration, client Use input packet delivery, actual prop picking/terrain, cross-world save/restart, NPC pathfinding, full-length story and bosses. Windows runner remained disconnected, and no new GitHub Actions runs were available. The 16 historically failing full-server GameTests have not been re-run to completion.


## Follow-up review — 2026-11-19 (main escape through post-boss reunion)

Scope: First Escape (including the rejected button-eye choice, real Wybie shoe and bicycle departure), the temporary Spink/Forcible tea visit, Cat/mirror and restored return tunnel, the parent-return chapter, all three rescue eye bosses and chase legs, the final Other Mother web confrontation, the real Little Door exit/lock and the reunion/dream cutscenes.

**Repaired in source in this follow-up:**

- The earlier proposal conversation could still consume generic cinematic prop interactions after First Escape began. The old proposal handler now declines them once that storyline owns the route.
- The unique Wybie story shoe previously completed its cutscene even when the projectile missed. A miss now leaves the SAME persisted shoe prop as a rethrowable physical item, with a bounded retrieval if it becomes unreachable; only the authentic Wybie collision resolves the scene. No second item or custody receipt is issued.
- Saved ShowPlayback state could survive a server restart without its in-memory recording request. This could leave dialogue silently waiting. The server now recognizes the orphaned session and restarts the SAME recording checkpoint with a new nonce, including in the Tea and Cat chapters which do not independently retry all audio blockers.

Previously repaired and retained: limited First Escape prop dispatch, Ratmaster ball-click forwarding to its original rat passenger, invisible rescue props no longer consuming clicks, and full finale/reunion cinematic watchdog dispatch without skipping actual fights or crawling.

**Verification performed here:** Retrieved source at main and inspected the 52 First Escape, 18 tea, 15 Cat, 23 parent-return, and 70 rescue phases (178 phase labels), their handlers and handoffs. Compiled the standalone original FirstEscapeTimeline, RescueChallengeMath, and RescueWebLayout Java helper code and executed 288,533 deterministic assertions for phase ordering, boss timing, web geometry, struggle and tunnel pursuit. Verified new native Forge GameTest fixtures in source for missed-shoe rethrow and interrupted playback sessions; these GameTests were **not executed**.

**Not proven:** This is NOT a Minecraft/Forge end-to-end playthrough. The Windows runner was disconnected and the GitHub repository exposed no new Actions runs. Entity pathfinding, actual hitboxes/interaction event order, per-recording client acknowledgements, save/reload, multiplayer state and complete geometry transitions still require a real runtime test. The previously recorded 16 failing full-server GameTests have not been shown to pass in this follow-up.


Date: 2026-11-15

Latest delivery pass: the production `jar` task completed successfully on 2026-11-15 at 20:58 local time, including audio timing checks and runtime asset optimization. No tests were run in this pass, per the user's instruction. The resulting artifact is `build-activity/libs/coraline-1.16.6.jar` (103,611,238 bytes).

## Scope and evidence

This is a source audit, compilation, runtime asset processing, and one full Forge GameTest server run. The GameTest server launched and ran 147 checks, of which 131 passed and 16 failed. It is **not** a complete story playthrough, and no route is marked end-to-end passed.

Status meanings: **Fixed in source** means a code repair and focused regression coverage were added or an existing recovery was made explicit; it does not claim full in-game validation. **Partial** means a mitigation exists but the whole requested failure mode still needs integration testing or further implementation. **Open** means this audit did not finish a safe repair.

## Issue matrix

| # | Finding | Status | Repair / remaining work |
|---|---|---|---|
| 1 | Rat eye before return can strand the player | Fixed in source | Rat eye requires the committed return leg; return retries from either side and phase advances only after transfer. Focused GameTest covers leg gating. Runtime/reload tests remain outstanding. |
| 2 | Player enters circus without Bobinsky | Fixed in source | Circus travel is resumable and verifies both original actor and player transfer before committing. Full missing/unloaded actor test remains outstanding. |
| 3 | Final eye presentation can trap owner while helper has an eye | Partial | Reconciliation/recovery and client presentation handling were added. Multiplayer full-inventory, disconnected-helper and repeated-request integration cases remain outstanding. |
| 4 | Boss fights impossible without a weapon | Fixed in source | Properly timed unarmed weakpoint strikes do modest damage; focused GameTest checks cooldown and damage bounds. Full four-boss minimum-loadout run remains outstanding. |
| 5 | Parlor room flag did not enforce doors | Fixed in source | Server-side door interaction checks enforce the lock. Door-half, disconnect and transition cases still need GameTest coverage. |
| 6 | Hand sever or final web entry committed before actor/transfer existed | Fixed in source | Original Hand identity/creation and actual transfer are verified before advancement; recoveries preserve identity. Spawn-failure integration remains outstanding. |
| 7 | Moving Company and taffy bowl shared entity kind 78 | Fixed in source | Movers have a separate kind; legacy kind migration is identity/context guarded. Focused ID assertion added; actual multi-mover transport remains untested. |
| 8 | Well encounter session ID changed on resume | Fixed in source | Persistent encounter identity is retained and the global Hand encounter rebinds only for the matching owner/door/original Hand. Restart and distance-pause run remains outstanding. |
| 9 | Temporary key-save failure permanently blocked retries | Fixed in source | Transient custody blockers are cleared/reconciled for retry while identity conflicts remain guarded. Fault-injected disk tests remain outstanding. |
| 10 | Offline riders can occupy transport bays forever | Partial | Logout pauses and journals unfinished rides for owner reconnect; completed rides can now reap exact props and release stale bays when the owner is away/offline and no player is near the truck. Unfinished rides intentionally retain their physical reservation. Three-player/offline/restart transport GameTests remain outstanding. |
| 11 | Bedroom mirror conversation target was out of range | Fixed in source | Child destination is selected from reachable positions validated against mirror range. Actual imported-house path/line-of-sight verification remains outstanding. |
| 12 | Hiding arrival radius exceeded concealment radius | Fixed in source | Navigation now stops strictly inside the concealment radius and the director verifies concealment. Focused boundary GameTest added; actual navigation still needs runtime coverage. |
| 13 | Final child crossing could use expired/solid tunnel | Partial | Crossing now rebuilds and validates both authenticated tunnel passages before transfer; passage occupancy includes story actors; persisted crawl stage resumes when the original child is already across. Actual two-world geometry, night close, unload and reload tests remain outstanding. |
| 14 | Interrupted escrow writes could reject legitimate inventory state | Partial | Reconciliation accepts monotonic mirrored writes and delivery recovery computes missing serialized stacks. Full versioned immutable confiscation ledger and interruption-at-every-write tests remain outstanding. |
| 15 | Accepted-route completion/reset left the route active | Partial | Alternative ending gets an explicit completion receipt and rebirth recognizes/resets its runtime keys. Full accepted-ending replay cycle is not run. |
| 16 | Zero energy regeneration could exhaust mandatory ghost abilities | Fixed in source | Supported tuning enforces a positive recovery floor. Boundary configuration runtime test remains outstanding. |
| 17 | Rebirth cleared the wrong tea key | Fixed in source | Reset includes `MissingParentsTea.KEY` and keeps the legacy alias. Focused GameTest added. |
| 18 | Rebirth left route-bound actors/items or stale bindings | Partial | Existing reset is provenance-aware and preserves unrelated worlds/items while retiring route state. A complete second-playthrough test is outstanding. |
| 19 | Existing tests were stale/shallow for route regressions | Partial | Focused GameTests were added for rat gating, unarmed boss attacks, hiding threshold, transport kind uniqueness, and rebirth tea/accepted keys. A complete transition graph/watchdog and all-state interruption suite are not implemented. |
| 20 | Map recovery could be missing, stale, or misassigned | Fixed in source | Explicit recovery rebuilds a missing vanilla map dataset only for the recorded story owner/company and retargets only exact-bound copies. Full inventory/ender/cursor/map-copy integration tests remain outstanding. |
| 21 | Map grant could not recover from missing dataset | Fixed in source | Initial issuance and explicit recovery repair the dataset when incomplete; provenance checks remain in place. Runtime map exploration test remains outstanding. |
| 22 | Wybie's bike could wait indefinitely for an unreachable route | Fixed in source | Departure retries alternate grounded pathfinder destinations; it commits only after actual arrival. Real-house path integration remains outstanding. |
| 23 | Dream return had no valid landing fallback | Fixed in source | Return is retried until a verified safe destination and restored player state are confirmed. Invalid-destination runtime test remains outstanding. |
| 24 | Covert searches could remain stuck on unusable exits | Fixed in source | Search chooses a physically reachable authored doorway and stores that exit; if the selected door changes, it finds another reachable room exit. The child still has to path out and close the actual doors, and discoveries/custody remain intact. Geometry integration tests remain outstanding. |
| 25 | Ghost HUD hid recorded blockers | Fixed in source | Saved actionable blocker text is exposed through the snapshot/objective path. Client presentation review remains outstanding. |
| 26 | Bed completion could be saved before the block was restored | Fixed in source | Restoration is applied and verified before committing the saved completion receipt. Crash-boundary GameTest remains outstanding. |

## Additional repairs in this change set

- Final eye receipt reconciliation, no-close deadlock handling, and client screen inventory synchronization were tightened.
- Accepted-branch parent recovery validates the recorded original actor and waits for it instead of spawning a replacement.
- Quest-item delivery can resume an interrupted prepared transaction from the recorded original stacks without resending already-held stacks.
- A restored accepted-route player now remains under the durable farewell/ending recovery controller if the ghost director declines a tick; the prison controller cannot pull them back into the Other World.
- Dream return verifies the actual world, destination and player safety before restoring state.
- An outbound Other World tunnel crossing now waits for its far physical door to open. A player who turns around during a return crawl re-enters the same visit through that open door without completing or restarting the visit.

## Focused test coverage

`src/gametest/java/com/coraline/feast/RescueSoftlockGameTests.java` covers rat stage gating, unarmed strike validation/damage, hiding-radius boundary, unique transport kind, and reset-key membership.

`src/gametest/java/com/coraline/multiplayer/QuestItemRecoveryGameTests.java` covers interrupted prepared-delivery reconciliation and exactly-once recovery.

These are actual Forge GameTest classes. The full server run exposed 16 failures: `storysubtitlecoverage`, `perfectfarmlandsupportscrops`, `daylightclosesentrybutvisitorscanreturn`, `giftpickupisownedandneverduplicates`, `lootandextrapoolsarecomplete`, `macesplashwithoutsweep`, `lostbedrecoverydoesnotrepeat`, `returntunnelclosesphysicaldooronlyathome`, `accesstooltipshaverequestedcolors`, `treasurestaysinsidegiftbox`, `activityroundtripcontinuestheloop`, `snapshotcopiesandrefresheshousewithoutloot`, `arrivaldeadlineandlocks`, `completesetsandarmortotals`, `welcomewaitsforeverygiftbeforetutorial`, and `welcomesequencereleasesplayerandclosesportal`. After the tunnel recovery change, a focused 39-test welcome/loop run passed the return-tunnel reversal case and failed 7 other checks: `treasurestaysinsidegiftbox`, `storysubtitlecoverage`, `arrivaldeadlineandlocks`, `activityroundtripcontinuestheloop`, `welcomewaitsforeverygiftbeforetutorial`, `lootandextrapoolsarecomplete`, and `giftpickupisownedandneverduplicates`. Some appear to have stale assumptions (music caption and exact recording count), while the interaction and story-loop failures still need fixture/code investigation. The hiding-radius and effective audio-resource checks passed. No complete route, save/reload route, or multiplayer route was exercised.

## Route coverage matrix

| Route | Source review | Focused regression tests | Full integrated route | Save/reload, multiplayer, geometry |
|---|---|---|---|---|
| A — rescue, four bosses, escape | Partial; critical named blockers reviewed | Partial; focused tests plus shared checks, with failures listed above | **Not run** | **Not run** |
| B — dream, map, Pink Palace, well, ending | Partial; named map, transport, well, bike and dream-return code reviewed | Partial; shared GameTests, with failures listed above | **Not run** | **Not run** |
| C — accepted proposal, ghost escape, body restoration, epilogue | Partial; named state/transaction/transition blockers reviewed | Partial; shared GameTests, with failures listed above | **Not run** | **Not run** |

## Remaining limitations and risk

The build and GameTest server establish compile correctness and exercise automated scenarios, but they cannot establish that the three complete authored routes, geometry, dialogue timing, pathfinding, dimension crossing or restart recovery all work. The 16 failing tests remain open. Several requested save/reload, disconnection, full-inventory and real-world geometry cases are not covered. Therefore this report does not claim that all softlocks are eliminated or that any complete route has passed.
