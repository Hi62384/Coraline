# Other World: the beautiful facade (1.7.0)

## Existing architecture preserved

Minecraft 1.20.1 / Forge 47.4.23. The `coraline:other_world` dimension still uses its existing void generator, fixed midnight, owner-specific island offsets, incremental `RealmCopy`, underground-home preservation, and crawl tunnel. The actual current radius default is **160 blocks**, configurable from 32–300; this update does not resize it. Existing 200/224-block configurations remain supported.

`RealmProtection` is unchanged: players cannot place/break blocks and normal mobs are rejected. Portals, Little Doors, NPCs, loot, audio playback, and storyline logic are unchanged. No corruption progression is implemented.

The prior snapshot intentionally strips copied chest contents/loot and executable block-entity data to prevent duplication. That policy is unchanged; source inventories, blocks and signs are never edited by this visual update.

## Presentation systems

- **Sky:** extend the registered `OtherWorldSky` dimension effect. A larger porcelain-blue button moon with engraved rim, 1,900 cached stars of varied size/brightness, slow low-amplitude twinkle, subdued blue/indigo curtains and a brief meteor approximately every two minutes. A blue horizon gradient avoids a black seam. No replacement Overworld sky or global shader.
- **Lighting:** Forge's `adjustLightmapColors` receives each sky/block-light combination. Skylight lifts exterior visibility into cool blue; strong block light reduces that lift and preserves warm lamps. Skyless corners retain their original light level. No Night Vision, fullbright, changed light-emitting blocks, or global gamma manipulation.
- **Natural palette:** `perfected_garden.json` supplies richer greens and blue water using native biome tinting, compatible with ordinary resource-pack textures. Only copied chunk biome palettes are assigned to it, including old islands on their next refresh. The biome has no generation features or spawns. Its music/mood metadata exactly preserves the previously used vanilla cherry-grove biome; no new audio or audio logic is added.
- **Atmosphere:** dimension-gated Forge fog events produce blue haze and clearer underwater visibility. Lava, powder snow, blindness and darkness retain their effects. Per-visitor visual metadata supplies the existing island center/radius, allowing a gentle reduction in far visibility toward the island edge. Tunnels retain their existing fog distances.
- **Gardens:** the client samples a small number of nearby, already-loaded heightmap columns each tick. Deterministic four-block cells select little single-species clumps of daisies, tulips, cornflowers, lilies, bluets and ferns. Only clear, sky-exposed air above grass/moss/rooted dirt is eligible; density increases near the Little Door. These are rendered models, not placed blocks. They have no collision, drops, ticking block entities, storage, or server presence. Resource-pack models/textures are reused.
- **Ambience:** one shared lightweight mote collection renders warm pulsing fireflies, cool drifting pollen, canopy leaf specks and water-surface glints. Slow drift, fade and distance culling prevent a glitter storm. Depth testing hides them behind walls. No mobs or persistent entities are spawned.

## Configuration and performance

Forge creates `config/coraline-visuals-client.toml`; an editable example is supplied here. It centralizes moon scale, stars/twinkle, curtains, sky/haze colors, moonlight, lamp warmth, fog distance, underwater clarity and nature budgets. Restart after edits. The enable switch disables custom **client effects**; biome colors are server/datapack data, not reverted by that switch.

Grass/foliage/water colors live together in `data/coraline/worldgen/biome/perfected_garden.json`, overridable by a datapack. Client textures are not globally replaced. Future facade deterioration can change these centralized palettes and strengths without coupling to portal/NPC logic.

Default bounds: 12 sampled columns per client tick, 36-block nature radius, 512 cached habitat anchors, 220 decorative models, 100 motes. Reduced particle settings halve the mote cap; Minimal disables motes. Decoration is cached, frustum/distance culled and periodically validated. Leaving the dimension clears caches. No full-island scans, chunk generation requests or entity-per-firefly implementation. Stars and sky-dome positions are cached.

For lower-end machines, try radius 24, columnsPerTick 6, maximumDecorations 100, maximumMotes 40. These are tuning suggestions, not FPS guarantees.

## Verification and limitations

Regression coverage includes island copying/refresh, source inventory preservation, copied sign text and machine orientation, original-biome isolation, placement/break protection and cow/zombie/bat rejection. Development-only renderer checks test the lightmap's cool exterior / warm lamp / unchanged shadow relationships and produce native sky previews. The optional world visual fixture uses a **copy of the disposable GameTest world**, never a player save, and is excluded from release jars.

Validation for this release: **46 GameTests passed**. The native-client scene comparison completed successfully; see `artwork/other_world_scene_native.png` and `artwork/overworld_scene_native.png`. The scene is a disposable test house, not a player's actual base. No third-party shader pack or representative large-base FPS benchmark was run. The computer-use inspection attempt failed in the Windows capture helper; the in-engine render harness supplied the successful comparison instead.

This is the standard Forge renderer, not a shader pack: there are no true reflections, volumetric light shafts, bloom or dynamic light cast by fireflies. Water glints are decorative, not reflected geometry. Haze softens island edges but does not make the void impossible to see when standing at the rim. Resource packs with radically different plant models and third-party shader mods need their own compatibility check. Underground builds retain their layout and benefit from lighting, but surface gardens/fireflies are intentionally not scattered through enclosed rooms.

Next visual work: tailor optional facade material details and additional flower-clump variants after testing several real bases; then an opt-in shader compatibility profile for physically reflective water. Keep corruption and sound design separate.

## File inventory

Added runtime Java:

- `config/OtherWorldVisualConfig.java`
- `otherworld/RealmVisualState.java`
- `client/OtherWorldLighting.java`
- `client/OtherWorldAtmosphere.java`
- `client/OtherWorldNature.java`
- `client/OtherWorldMotes.java`

Added resources/config: `data/coraline/worldgen/biome/perfected_garden.json`, `config/coraline-visuals-client.toml`.

Modified runtime: `CoralineMod.java` (config/channel registration), `client/OtherWorldSky.java` (existing effect), `otherworld/RealmCopy.java` (biome palette only), `data/coraline/dimension/other_world.json` (default visual biome). The dimension type and protection class are unchanged.

Verification/build/docs: `OtherWorldGameTests.java`, `DollRenderCheck.java`, new development-only `OtherWorldVisualCheck.java`, `build.gradle`, `gradle.properties`, README and parent guide. No development test classes ship in the release jar.
