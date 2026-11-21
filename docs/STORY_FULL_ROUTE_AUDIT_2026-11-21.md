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
- **Reunion saved-stage false positive:** A persisted `ParentsStaged` flag was trusted without rechecking the real mother and father's dimension and location, allowing reunion voices to run while parents were elsewhere. Both canonical parent UUIDs must be in the Overworld reunion area; an invalid marker causes safe same-actor restaging. The recording is paused/restarted when either original parent is unavailable.

No changes were made to encounter wins, eye receipts, key identity, canonical actor UUIDs, or player rewards.

## Verification performed

- Parsed **52** First Escape/Wybie phases and simulated the rejected-button route through every ordered checkpoint with explicit player/actor interaction gates.
- Validated **88** expected voiced route keys against `sounds.json` and the **88 corresponding packaged .ogg assets**. Missing: **0**.
- Compiled dependency-free `FirstEscapeTimeline`, `RescueChallengeMath`, and `RescueWebLayout` using Java 17 and ran **253,127** deterministic assertions. This model covers boss health/openings, Cat placement rules, collapse timing, web-noise rules, door struggle and crawl-pursuit timing. It **does not execute** Minecraft pathfinding, entity-lease persistence, audio acknowledgements, dimension transfers or the actual full game.
- Added two `RescueSoftlockGameTests` for the actor/parent gates and two source-level regression tests in `tools/test_current_story_repairs.py`; the native GameTests were **not executable** in the current environment.

## In-game acceptance still required

To certify the entire game path, run a clean survival save through the sequence, then repeat after disconnect/reconnect during: a shoe miss; the tea reward scene with an unloaded visitor; Cat mirror/departure; each of the three boss phases; the rat's circus-to-attic return; Cat pickup during collapse; the final Cat throw; the web/final crawl; and the snowy reunion and dream. Check no duplicate original actors, lost eyes/key, blocked exits, or unintended audio advances. The connected Windows game runner is offline here, so full Forge/engine verification remains **unverified**.
