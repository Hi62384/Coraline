# Coraline story softlock audit

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
