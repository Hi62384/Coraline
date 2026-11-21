# Full First-Escape-to-Post-Boss Route Reinspection — 2026-11-21

## Checked story route

1. First Escape and canonical Wybie, including mask/stitches, wardrobe, Little Door, the 22-cell decayed escape tunnel, the Overworld missing-parent scenes, the single saved shoe projectile (collision, miss, manual rethrow, same-UUID recovery), chase and bicycle departure.
2. The Spink/Forcible tea visit, its Seeing Stone reward receipt, actor loading and voice/watchdog handoff.
3. The Cat's mirror, HELP US reveal, bed/doll scene, original tunnel restoration and Cat departure.
4. The return crawl and Parent Rescue kitchen sequence (original Other Mother/father, key swallowing, seating, kitchen checkpoint recovery).
5. The rescue encounters: garden/Other Father Mantis fight and eye, theatre/Spink-Forcible boss and eye, attic and Bobinsky, Ratmaster fight, moving rat/ball chase and final eye, escape with the original Cat during the collapse.
6. The Beldam confrontation: presentation of all three original eyes, Seeing Stone and key custody, cat-throw fight, opened stage floor, web stealth, door struggle and severed hand, the 52-block final crawl, actual Overworld door locking.
7. Post-boss snow-covered real-parent reunion, original Cat farewell, original ghost-child dream, verified Overworld return, ghost release and completion.

These are **source-path and dependency-free simulation checks**, not a successful Minecraft gameplay recording or an end-to-end Forge GameTest run.

## Repaired risks

- **Tea visit watchdog bypass:** The independent watchdog could continue scripted phases without the saved original Miss Spink and Miss Forcible visitors. Its audio and skip gates now require both authenticated actors; a missing actor stops the interrupted voice run, preserves the phase and resets the watchdog while the original identity is recovered. A Seeing Stone reward cannot be reached by a watchdog skipping absent actors.
- **Reunion saved-stage false positive:** A persisted `ParentsStaged` flag was trusted without rechecking the real mother and father's dimension and location, allowing reunion voices to run while parents were elsewhere. Both canonical parent UUIDs must be in the Overworld reunion area; an invalid marker causes safe same-actor restaging. The recording is paused/restarted when either original parent is unavailable. A player relogging into the wrong dimension is returned using a checked landing by the real Little Door; a player too far away must return to the reunion area before its audio can run.

No changes were made to encounter wins, eye receipts, key identity, canonical actor UUIDs, or player rewards.

## Verification performed

- Parsed **52** First Escape/Wybie phases and simulated the rejected-button route through every ordered checkpoint with explicit player/actor interaction gates.
- Validated **88** expected voiced route keys against `sounds.json` and the **88 corresponding packaged .ogg assets**. Missing: **0**.
- Compiled dependency-free `FirstEscapeTimeline`, `RescueChallengeMath`, and `RescueWebLayout` using Java 17 and ran **253,131** deterministic assertions. This model covers boss health/openings, Cat placement rules, collapse timing, web-noise rules, door struggle and crawl-pursuit timing. It **does not execute** Minecraft pathfinding, entity-lease persistence, audio acknowledgements, dimension transfers or the actual full game.
- Added two `RescueSoftlockGameTests` for the actor/parent gates and two source-level regression tests in `tools/test_current_story_repairs.py`; the native GameTests were **not executable** in the current environment.

## In-game acceptance still required

To certify the entire game path, run a clean survival save through the sequence, then repeat after disconnect/reconnect during: a shoe miss; the tea reward scene with an unloaded visitor; Cat mirror/departure; each of the three boss phases; the rat's circus-to-attic return; Cat pickup during collapse; the final Cat throw; the web/final crawl; and the snowy reunion and dream. Check no duplicate original actors, lost eyes/key, blocked exits, or unintended audio advances. The connected Windows game runner is offline here, so full Forge/engine verification remains **unverified**.

## Independent second pass — November 21, 2026 (reconnect / scene-boundary focus)

This pass re-read the **current main-branch implementations after the prior merge** and traced all five story modules: 52 First Escape/Wybie, 18 Spink/Forcible tea, 15 Cat, 23 parent-return, and 70 rescue/finale/epilogue checkpoint names (**178 total**). Checked 71 literal rescue transition calls, all resolving to valid states, and confirmed all **88 route audio registrations have a corresponding packaged .ogg file**. Verified earlier single-shoe miss/rethrow requirements, recorded actor identity/eye/key ownership, all three boss chapter handoffs, the rat circus/attic leg transition, Cat collapse, physical final crawl and dream-return bookkeeping in source.

### New bugs fixed in this second pass

1. **Rescue cutscene watchdog bypassed the previous actor readiness gate.** The independent watchdog could finish a reunion recording while the ordinary chapter tick was paused for missing originals, or skip a Bobinsky presentation that was not staged. Both the normal tick and watchdog now check the same scene gates before allowing safety progression.
2. **Wrong-dimension / missing-Cat farewell:** After a disrupted save, `CAT_NOTICE`, `FAREWELL_A` or `FAREWELL_B` could play while the owner was outside the Overworld or the original Cat was no longer at the farewell. This could save a foreign-dimension dream-return anchor and strand the dream. Verify the owner near their real Little Door, their returned parents, the original Cat's saved UUID and safe physical presentation before progressing; the Cat is only moved as its original actor.
3. **Older corrupted dream-return dimension:** Previously, `restoreVision` returned false forever whenever the recorded return dimension was not the Overworld (or the coordinates were not finite). It now refuses the foreign position and can use only the previously verified, owner-bound Overworld Little Door as a safe return fallback; final player-data readback still gates `VisionRestored`.
4. **Other-world house entrance save drift:** `HOUSE_SAFE` could run from the wrong dimension because matching coordinates alone were checked, potentially moving the Cat or sealing the confrontation in the wrong world. It now requires the original Other World. A previously saved `GreenHouse=true` no longer assumes its real entry doors stayed open; it checks and reopens those existing doors while the player is still in `HOUSE_SAFE`.
5. **Stale physically verified 52-block crawl:** `PassageVerified` survived future block changes. Each **new** entry/retry now starts a bounded, receipt-checked verification pass before transferring the owner into the crawl, without resetting verification every tick or overwriting unrelated edits.
6. **Post-crawl `LOCK_DOOR` / `BANGING` reload:** These phases could resume in the Activity/web dimension after a partially persisted transfer and never reach the real door/reunion. The original door and Overworld destination are now reauthenticated, and the player is transferred only to a safe authentic landing. If the saved crawl receipt is missing, the actual passage must be re-entered; if the permanent seal receipt is missing, the original key interaction must be replayed instead of faking completion.

### Test status

- Java 17 **compiled three dependency-free source files byte-for-byte matching their GitHub blob SHAs**: `FirstEscapeTimeline.java`, `RescueChallengeMath.java` and `RescueWebLayout.java`.
- Ran the offline `StoryRouteSimulation` model with **853,154 deterministic checks**, including prior route/boss/collapse/crawl calculations and newly modeled interruption/recovery branches. These tests do **not** execute the changed Forge production code or a Minecraft client. The standalone simulation is a model, not proof of successful Minecraft gameplay.
- Re-scanned **24** specific source wiring/invariant requirements; **24/24** passed.
- Added **three Forge GameTests** and **five source-level regressions**. These newly committed tests are available for native CI/Windows execution but were **not run** in Minecraft during this session.
- The Windows command runner is still disconnected; GitHub Actions reported no existing workflow runs. No full in-game save/reload, live audio, collision, multiplayer, or boss performance validation was possible.

**Residual risk:** The full end-to-end route and its new recovery branches remain engine-uncertified. Re-run the Forge GameTests and a real Minecraft playthrough on a backed-up world before shipping or declaring it softlock-free. In particular, interrupt the final passage and the reunion/farewell with client reconnects, verify the original UUIDs and the real door, and confirm a physically successful wake-up after the ghost dream.
