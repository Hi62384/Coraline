# Coraline

A Minecraft Forge mod for Minecraft 1.20.1.

Version 1.13.6 adds 15 state-based activity-completion reactions using the same local Other Mother voice pipeline. A cancel-safe hook waits for the recording before returning control to the caller's free-time phase. See [OTHER_MOTHER_ACTIVITY_COMPLETE_AUDIO.md](OTHER_MOTHER_ACTIVITY_COMPLETE_AUDIO.md).

Version 1.13.5 adds 30 state-based on-time/late arrival greetings. The existing attendance deadline selects the condition; the selected activity continues only after the greeting finishes. See [OTHER_MOTHER_ARRIVAL_AUDIO.md](OTHER_MOTHER_ARRIVAL_AUDIO.md).

Version 1.13.4 adds 44 activity proposals and 15 state-based five-minute warnings. Dialogue hooks connect tutorial → proposal → current-state warning → a full five-minute attendance window, with callbacks for future activity portal/gameplay systems. No arenas, penalties or relationship rules are invented. See [OTHER_MOTHER_ACTIVITY_AUDIO.md](OTHER_MOTHER_ACTIVITY_AUDIO.md).

Version 1.13.3 adds four Other Mother tutorial/rules recordings in one non-repeating pool, plus a cancel-safe completion hook for a future activity-proposal stage. It does not change the first-visit story or implement a new activity loop. See [OTHER_MOTHER_TUTORIAL_AUDIO.md](OTHER_MOTHER_TUTORIAL_AUDIO.md).

Version 1.13.2 adds 22 locally cloned Other Mother food, treasure and state-extra recordings, reusing the welcome voice pipeline. Non-repeating, family-persistent audio hooks are ready for a future gifting workflow; no new rewards or relationship calculations are introduced. See [OTHER_MOTHER_LOOT_AUDIO.md](OTHER_MOTHER_LOOT_AUDIO.md).

Version 1.13.1 adds fifteen locally voice-cloned Other Mother welcome lines in five emotional pools, with non-repeating selection and family-scoped saved state. Repeat visits reuse the existing dialogue playback; the first-visit introduction is unchanged. No Trustness/Suspicion progression rules are added. See [OTHER_MOTHER_WELCOME_AUDIO.md](OTHER_MOTHER_WELCOME_AUDIO.md) for generation, integration and listening-review limitations.

Version 1.13.0 rebuilds Dreamweave, Comfy Silk, Buttonguard, Garden Vines, Midnight and Miner's armor from the supplied design sheets: 24 illustrated icons, six fabric textures and distinct articulated models. Includes dimensional leaves/vines, framed button masks, lanterns, embroidered mantles and quilted sleepwear. Existing stats and abilities are unchanged. See `artwork/sculpted_wardrobe/REFERENCE_ARMOR.md` for sources and reproduction.

Version 1.12.0 replaces all 87 wearable icons and worn models with a Coraline-inspired cloth wardrobe: curved meshes, raised buttons and stitching, embroidered cuffs, linings, shaped hats and footwear. See [CLOTHING_GUIDE.md](CLOTHING_GUIDE.md) and the source-art catalogs in `artwork/sculpted_wardrobe/`. Armor stats and abilities are unchanged.

Version 1.11.1 increases Other World color saturation to 2.15, adds quest route particles and no-cheats `/coraline guide` / `/coraline escape` recovery commands, and provides non-destructive fallback scene space for cramped cave homes. Parent death now resets the family's Other World story progress, including completed first visits, with legacy-save repair. See [OTHER_WORLD_RECOVERY.md](OTHER_WORLD_RECOVERY.md) for limitations and verification. The newly requested sculpted armor overhaul remains a separate work in progress and is not included in this patch.

Version 1.11.0 redesigns all nineteen armor sets with Coraline-inspired 3D clothing and 76 matching Blender-rendered icons, adds varied 3D parent hairstyles/outfits (64 identities per role), improves moving shirt light, and increases Other World saturation. Equipment descriptions are green and ability-only. The wand cooldown is 2.5 seconds; the whip now has an animated cord, a crack sound and a configurable 1-second cooldown. Find every item in the Coraline creative tab; see [CLOTHING_GUIDE.md](CLOTHING_GUIDE.md) for abilities, configuration and the editable Blender asset workflow.

Version 1.9.4 aligns supper-chair interaction bounds with the full backrest and armrests, handles direct hit interactions on the first click, and allows sitting immediately upon returning for dinner. Sitting preserves camera direction; the parents' chairs remain reserved. Story parents now have exclusive gaze control instead of competing with random-looking/head-centering AI. All 32 mom and 32 dad skins have been redrawn with varied hair, faces, layered cuffs, patterned clothes, jackets, overalls, scarves and accessories; saved skin IDs and matching Other Parents are preserved. Regenerate only these textures with `python tools/create_parent_skins.py`.

Version 1.9.3 fixes free camera movement while seated, restores players only to their own dinner chair, repairs missing chairs in existing scenes, and recovers parents safely to their reserved seats if the walk from the piano stalls (20-second maximum). Other Mother's supper lines play 0–2 with five-second gaps; Mysterious Supper continues through the cake reveal. See You Soon plays while sleeping, and returning home waits for the recordings to finish. Timing is configurable in `coraline-first-visit.toml`.

Parent idle/interaction dialogue now cycles through every eligible clip in shuffled order before repeating. Dad's scripted death occurs four seconds into his line; Mom dies after one second. The mustard ketchup salsa wrap now has its requested lunch tooltip and restores four hunger points (two icons).

Version 1.9.2 stages the welcome while you are in the tunnel, moves the piano 40–72 blocks from the door, normalizes Other Parent dialogue, and keeps story audio centered on the listener. Story songs use the Jukebox/Note Blocks slider instead of vanilla Music. The piano uses reddish stripped wood; supper has three upholstered, gold-trimmed chairs with reserved parent seating. Existing already-started scenes keep their saved piano location. Version 1.9 introduced the Other Parents piano-and-supper sequence, eleven feast foods/drinks, and a collectible strawberry cake that remembers its eaten state. See [FIRST_VISIT_GUIDE.md](FIRST_VISIT_GUIDE.md) for behavior, settings and placement limits. Back up saves before updating.

For the new parents, linked bed/chest, jobs, tiny door, configuration, and parent audio setup, see [PARENTS_GUIDE.md](PARENTS_GUIDE.md).

Version 1.7.0 introduces the beautiful Other World facade: a larger button moon, rich blue starry sky, bright cool moonlight with warm lamps, atmospheric haze, lush biome colors, and lightweight client-only flower clusters/fireflies. Copy geometry, protections, portals, NPCs and audio are unchanged. See OTHER_WORLD_VISUALS.md for presentation settings, limits and verification. Back up existing worlds before updating.

## Current content

- **Dowsing Rod** (`coraline:dowsing_rod`): a rare item with a 0.5% (1 in 200) chance to drop when a player breaks a block in the `minecraft:leaves` block tag. Creative-mode breaks do not roll a drop.
- With a claimed living parent pair, right-clicking the rod summons a killable Wybie encounter behind the player. He rings his bicycle bell, rides into the player, follows and watches them through voice lines A, B, and C, gives the player-skin doll one second into line B, applies poison after B, and rides away during C. The rod then stays disabled until those parents die; their death destroys the doll and resets eligibility for a new family.
- **Button-Eyed Doll** (`coraline:doll`): records the receiving player's UUID/name and skin profile. Both the inventory icon and placed block render a miniature player with that skin and button eyes. Supports slim arms; ownership and skin profile survive breaking and reloading. Skin resolution uses Minecraft's skin service/cache, with vanilla fallback until available.
- **Wybie Spawn Egg** (`coraline:wybie_spawn_egg`): creates a persistent, killable NPC without automatically triggering rod dialogue.
- Wybie uses a 3D Minecraft player model with custom skin, raised curls, riding pose, bent knees, and a bicycle with rotating wheels. The standard living renderer supplies damage flashes and death animation.
- Rod spawning searches nearby supported positions and checks the full collision box. If no safe location exists, it fails without starting the cooldown. Wybie no longer teleports through walls to reach the player.
- Every mod item and block is available in the Coraline creative tab.

## Development

This project uses Forge 47.4.23 and requires a 64-bit Java 17 JDK.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot'
.\gradlew.bat build
.\gradlew.bat runClient
```

The built mod jar is written to `build/libs/`.

Use the latest jar only; remove older Coraline jars from your Minecraft instance's mods folder before installing a new version.

## Verification and artwork

Run `.\gradlew.bat -PgameTests runGameTestServer` for gameplay regression checks covering collision-safe spawning, persistence/damage, parents, beds/tasks, doll recovery, family lifetimes, and tooltips. These tests are excluded from normal release builds.

`tools/create_wybie_skin.py` creates the UV-aligned 64x64 character skin. The live doll uses Minecraft's player textures, not the old generic doll PNG.

`tools/preview_wybie_blender.py` produces `artwork/wybie_3d.blend` and a geometry preview using Blender 5.2. The game renders the native Java model in `WybieModel.java`; the Blender scene is an editable reference, not a runtime dependency.

Version 1.2.0 validation: release compilation, client startup/resource loading, and all four server GameTests passed. Full visual inspection inside a running game was unavailable because the Windows capture helper failed; the Blender preview was inspected instead.
