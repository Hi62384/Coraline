# Coraline 1.7.0

The Other World's visual update and separate client settings are documented in `OTHER_WORLD_VISUALS.md`. Parent behavior below is unchanged in this release.

## New dialogue and death sequence

Idle dialogue now occurs every 50–70 seconds by default, with an independent random timer for each parent. If one parent is speaking when the other's idle timer expires, the other waits and retries after a random 1–5 seconds, leaving at least one second between recordings. Right-click and idle chatter use Neglect clips during the busy/unavailable phase, and Task clips during the available/task phase (the bed's green check). DadNeglectDuringRain is included only while the dimension is raining. Repeated clicks cannot overlap the parent's current dialogue.

Lethal damage freezes both real parents and disables interaction/damage. The first parent's death line starts immediately; Mom dies 20 ticks (1 second) into her line, while Dad dies 80 ticks (4 seconds) into his. Only after the complete first recording ends does the partner's death line start, using their own delay. Positional audio continues after the entity dies. The sequence is saved; audio itself does not resume across a server restart. The family's doll/door bonds end when the sequence begins.

Configure `parents.dialogueIntervalTicks` (1200), `dialogueVariationTicks` (200), `momDeathAfterVoiceTicks` (20), and `dadDeathAfterVoiceTicks` (80). The old shared `deathAfterVoiceTicks` option is no longer used. Old `voiceIntervalTicks`/`voiceIntervalVariationTicks` are no longer used.

Entering the crawl tunnel starts snapshot weaving immediately. The far door stays locked and reports progress when clicked until weaving finishes, then can be opened. You remain protected in the loaded tunnel during generation.

Failed bed placement no longer predicts item consumption on the client. If you lost an unplaced family bed, sneak-right-click one of your tamed parents with an empty hand to recover a replacement; carrying that family's bed prevents repeated replacements. The dowsing rod requires a claimed living parent pair. Its tooltip is yellow without parents and green with parents. Receiving a doll disables the rod for that pair's lifetime, even if the doll is lost or placed. When those parents die, the doll is destroyed and the received flag resets; claim new parents to start again.

Wybie's approach now lets navigation steer his body, uses throttled path updates, and avoids close-range back-and-forth following. His departure selects reachable destinations away from the player and replans when blocked rather than applying a fixed velocity. Approach/departure speeds, repath interval, and approach timeout are configurable under `[wybie]`. Enclosed areas still cannot provide an escape route; he disappears after line C as before.

## Parents

Parent pairs occasionally appear on a clear, bright Overworld surface 24–48 blocks from a player. The default check is once per minute with a 3.5% chance per player, limited to two nearby pairs. Each parent gets one of 32 original local Minecraft skins per role, avoiding consecutive repeats within a server session. Existing parents keep their saved skin. Monsters can target them, and they regenerate one health point every ten seconds.

All mod items and blocks are in the **Coraline** creative tab, including the Parents Bed, doll, tiny door, and both spawn eggs. Use the **Parents Spawn Egg** on solid ground to spawn a linked Mom and Dad together. It consumes one egg in survival, none in creative, and only succeeds if both parents fit safely. Right-click a parent to tame the pair and receive their assigned bed. To assign a generic creative bed, hold it and right-click one of your already-tamed parents. A pair can still have only one placed bed.

Look at an unclaimed parent for the claim hint, then right-click either parent to tame the pair. Each player can have only one living pair, checked across dimensions. You receive one **Parents Bed**, linked to that pair. They follow their owner when the bed is not placed, including safe catch-up teleports and dimension changes. They stay with the bed when it is placed. Lethal damage starts the sequential death scene described above. After losing a pair, you can claim another. Each parent independently speaks approximately once a minute and on right-click, using the current task/neglect pool with an anti-overlap cooldown.

Place the full-size double bed in a clear, supported **2×2 area**. Its foot-end chest is part of the bed, with 27 internal storage slots—not a separate chest block. Right-click your bed to access storage (the chest front or sneak-right-click also works). Right-click Mom herself to assign Mom's jobs, or Dad himself to assign Dad's jobs. Each menu shows only that parent's tasks and only the owner can assign them. Task selection can be made while following, but production still requires a placed Parents Bed. The bed retains its green availability checkmark.

Task-menu availability alternates between **5–8 minutes busy** and **3–4 minutes available**, with a new random duration each period. A green-check speech-bubble icon appears at the headboard while available. Busy attempts display **parents are too busy for you right now!** and use neglect dialogue. Storage is always accessible, and **assigned tasks keep running through busy periods**. Parents roam actively near home; when due for a delivery, they must walk up to the built-in chest before depositing. Either front position or either accessible trunk side can be used. The schedule persists across saves and bed replacement and uses game ticks rather than time of day. All four duration bounds are configurable.

Breaking any part removes the whole double bed, returns its assigned item, drops its stored contents, and switches both parents back to following. Placing the recovered bed reconnects the same family. A dead family's bed cannot be reused for another pair.

**Updating an existing world:** break and re-place old one-block beds to build the new 2×2 layout. Old separate chests are deliberately left intact with their contents; move those contents into the new built-in storage manually.

| Parent | Job | Result |
|---|---|---|
| Mom | Garden | Adds irrigated farmland and wheat, carrots, or potatoes near the bed |
| Mom | Groceries | Apples, vegetables, or bread |
| Mom | Buy clothes | Usually basic armor; 3% chance of a diamond piece |
| Mom | Mine wood | Oak, birch, or spruce logs |
| Mom | Mine stone | Cobblestone |
| Dad | Make dinner | Usually Dad's Slime; sometimes cooked food, pie, or stew |
| Dad | Mine iron | Raw iron |
| Dad | Mine coal | Coal |

Deliveries repeat at randomized intervals while the parent is loaded, near home, and assigned a task (default 1–3 minutes). `parents.taskIntervalTicks` sets the average, `parents.taskIntervalVariation` sets the fractional variation, and `parents.taskWalkSpeed` controls work movement. Working parents continually choose reachable walking destinations between deliveries; their chest approach takes priority over wandering. Items appear only after reaching the chest. Keep the front or sides of the bed accessible. They do not overwrite a full chest. Mining/shopping jobs generate periodic deliveries rather than digging up your world. Gardening modifies only dirt with clear, sufficiently lit space above, adds irrigation when necessary, respects `mobGriefing`, and stops at the configurable plot limit. Break/harvest the crops normally.

## Bound doll

Wybie grants one doll per living parent pair. This persists across player deaths and restarts. The doll's yellow warning reads “this item will only exist as long as your parents are alive”. Parent death cancels recovery, deletes the inventory/placed doll, and invalidates old copies in unloaded storage. Claiming a new pair permits a new doll; old copies cannot become valid again. The placement tooltip reads: “A little me? I should put it down somewhere near my bed”.

While the parents live, the doll is satisfied only in its owner's inventory or placed within 12 horizontal blocks and 4 vertical blocks of the owner's actual Minecraft respawn bed (not the Parents Bed or a respawn anchor). Sleep in a normal bed to establish that home. Container storage, including ender chests and nested shulker boxes, or missing doll items trigger recall after about 60 seconds. Invalid placement recalls after 5 seconds. It reappears falling beside the online owner, below roofs where necessary; if no safe falling position exists it attempts inventory delivery. Unloaded containers are not force-loaded: their obsolete doll copy is removed when loaded or used. Loose-item destruction also schedules recovery. These safeguards do not apply after parent death, which intentionally ends the bond.

Indoors, the placed doll occasionally moves to a nearby supported location in the same connected room. When eligible, it retries every five seconds with a 25% chance, then waits the configured `moveCheckTicks` after a successful move (60 seconds by default). It moves at most four blocks horizontally and preserves its skin/owner. Every destination remains inside the fixed bed radius, so repeated moves cannot drift away from home. Looking away now permits movement even nearby; sleeping players are not observers. Glass roofs count as indoor shelter. Walls, closed doors, occupied blocks, unsupported positions and outdoor locations are rejected.

The bond also keeps a separate backup in the player's saved data. If the world loses its doll record while the same parent pair is alive, the watchdog retires the previous generation, rebuilds the skin-bound doll, and schedules another sky delivery. Recorded placed locations are verified rather than trusting unloaded chunks. Parent death is still the intentional exception: it ends the haunting until you claim a new pair and receive a new doll. No mod can recover from deletion/corruption of both the world records and player save together.

A satisfied doll has an 8% chance each minute to drop a tiny door, but only if its owner has no existing tracked door. Door identity survives placing/breaking, inventory transfers, containers, nested shulkers, and chunk unloading. Actual destruction permits another gift; missing stored items have a one-minute confirmation window to avoid mistaking transfers for deletion. Unloaded storage is conservatively treated as still existing. The door tooltip says: “Where did this strange little door come from? I should attach it to my wall somewhere”. Picking up a doll's gift also displays this line. Both the drop interval and chance are configurable.

## Tiny door

A one-block-high ivory-and-wood door with corrected edge UVs and a front-facing inventory/held sprite. Normally it opens onto bricks; an eligible nighttime event can replace those bricks with the magical tunnel. Craft it with this 2×3 recipe:

```text
Plank  Plank
Plank  Iron nugget
Brick  Brick
```

Normally it remains bricked up. On an eligible night it can become the entrance described below. Right-click an older placed door once to assign its new tracking record to you.

Bound little doors also expire when the owner's parents die. The tracked placed door is removed without drops; inventory, dropped, and stored copies are permanently invalidated. Unloaded copies are cleaned when accessed/loaded, and cannot be placed again. Visitors already in the Other World retain their return exit.

## Mice and the Other World

The relevant owner must have living claimed parents, their doll placed near their real respawn bed, and a placed little door. Between night ticks 13000 and 23000, coming within 12 blocks of that bed permits **one 30% roll per owner per night**, not repeated rolls from multiple doors or other nearby players. Up to five mice search for supported surfaces within 8 blocks horizontally and 8 blocks vertically of the bed, independent of the owner's flying height. Passable grass and flowers are allowed; solid obstructions and liquids are not. A reachable route to the door is still required. They run toward the door and disappear into it; an arriving mouse opens its portal for the rest of that night. Dull gray particles confirm the nighttime requirements are met, even after the nightly roll (six per second by default, controlled by `eligibleGrayParticleCount`, replacing `readyParticleCount`). Active portals emit bright blue and white particles. Spawn search bounds and particle counts are configurable.

Creative testing: the Coraline tab contains **Mouse Event Debug** (shell icon) and **Doll Binding Debug** (totem icon). Right-click the mouse item to bypass the random chance and nightly limit; missing requirements or inaccessible ground are reported in chat. Right-click the doll item to instantly receive a properly bound doll without Wybie's scene. It requires living claimed parents and an empty inventory slot, and will not duplicate an existing bound doll. Both items reject Survival use.

Open the activated door, then **hold sneak and walk into the opening**. The blue-purple fabric tunnel is 3D and crawled through in a forced crawling pose. Walk forward to reach the copied island. All players may use an active entrance; visitors do not need their own doll or parents to enter it.

The Other World creates a circular island with a **160-block horizontal radius** by default. `otherWorld.snapshotRadius` replaces the legacy `islandRadius` setting and applies on the next snapshot, including existing realms. Terrain, house blocks, block appearances, signs, paintings, item frames, and armor stands are refreshed from the normal world on entry. The doll block is deliberately excluded. A permanent fantasy midnight sky has blue/lilac auroras, twinkling stars, and a luminous four-hole button moon. Copies of the relevant owner's parents retain their skins and have button eyes; saved incarnation tracking retires obsolete copies, including unloaded ones. Real parents stay home.

Snapshots retain the surface and 16 blocks below it, rather than cloning all buried geology. Within 64 horizontal blocks of the door or respawn bed, the retained depth also extends at least 32 blocks below their elevation to preserve underground homes, cave floors, and ceilings. These limits are controlled by `surfaceDepth`, `undergroundHomeRadius`, and `undergroundHomeDepth`; unusually deep or sprawling underground bases may need larger values. Blocks above the surface remain included. All source-world blocks remain untouched.

Snapshot construction is asynchronous, with one chunk-sized copy step per server tick by default. You enter the protected tunnel immediately while weaving runs. The far door refuses to open until generation completes and reports its percentage when clicked. Both exits require an open door and reaching its threshold; standing near the normal-world door does not teleport you. Tunnel chunks are held loaded and sent immediately on entry; a fall/boundary guard restores escaped players to the corridor, and tunnel damage is blocked. Preparation still takes time, especially for unexplored terrain; it is not a fully seamless-rendering portal. Each door's island is kept far from other owners' islands, and all visitors to that door share its replica.

The replica is protected: no player block breaking/placement, item use that changes the world, attacks on decorations, or storage extraction. Containers retain their appearance but do not duplicate stored items. Vanilla decorative entities are visual protected copies; live animals, machinery behavior, and arbitrary third-party mod block logic are not promised as exact clones. Server operators and commands can bypass normal gameplay protections.

To leave through the island's little door, sneak into it and crawl all the way back through the tunnel; touching the door does not teleport you directly home. **This exit works after sunrise**, even when normal-world entry has become bricks again. Alternatively, right-click the copied counterpart of **your own actual respawn bed**: after a short sleep you wake beside that bed in the normal world, without changing your respawn point. Other beds refuse sleep; visitors whose own bed is outside the copied area must use the tunnel. Leaving the island's bounds or falling into its void also returns you safely.

Back up important worlds before installing a version adding a dimension. Restart the game/server after replacing the old jar. For quick testing, set `otherWorld.nightChance = 1.0`, keep the door reachable from the bedroom, and use a new night. Restore `0.30` afterward. A failed copy returns the entering player safely to the real world, and an existing visitor retains a return route.

The generated tunnel material, exact ImageGen prompt, and runtime asset path are recorded in `artwork/TUNNEL_ART.md`.

## Configuration

Forge generates `config/coraline-common.toml` in the Minecraft instance. An editable default copy is included in this project's `config` folder. It controls spawn frequency/range/cap, health, regeneration, following, home radius, busy/available durations, task timing and quantities, rare armor, slime chance/nutrition, gardening, voice intervals, Wybie size/movement/damage/poison, and rod drop/cooldown values. Restart the game/server after editing; use matching visual settings on clients. Legacy `bedBusyChance`, `voiceCheckIntervalTicks`, and `voiceChance` settings are no longer used.

## Adding parent audio

The mod includes 19 Mom recordings and 10 Dad recordings for random dialogue, plus Angryparent for the busy-bed response. WAV, MP3, OGG, FLAC and M4A are supported by the import script.

- `voicelines/MomNeglect*.wav`, `DadNeglect*.wav`: neglect dialogue.
- `voicelines/MomTask*.wav`, `DadTask*.wav`: task-mode dialogue.
- `voicelines/DadNeglectDuringRain*.wav`: rain-only neglect dialogue.
- `voicelines/MomDeath.wav`, `DadDeath.wav`: staged death recordings; durations are measured automatically at build time.
- `AngryParent` stays registered for compatibility; busy parent clicks now use Neglect dialogue.

Builds automatically import matching files using Python and FFmpeg, producing positional mono OGG clips normalized to -14 LUFS with peak limiting for louder, clearer parent dialogue. Source recordings stay unchanged. You can also run `python tools/import_parent_voices.py` explicitly. Rebuild and replace the installed mod jar afterward.

## Art and development

The doll explicitly uses the adult model transform, with larger filled button eyes mounted higher on the face to cover the skin's original eyes. Its inventory model and placed block still use the owning player's skin/profile. Wybie defaults to 72% of his previous model size.

Dad's Slime uses a new built-in ImageGen asset saved as `artwork/dads_slime_source.png`; its prompt was a transparent Minecraft pixel-art wooden bowl of pale green lumpy slime porridge, with an olive-flecked blob dripping over one rim, no text or background. The 64×64 game icon is `src/main/resources/assets/coraline/textures/item/dads_slime.png`. Parent skins, door textures, and bed textures follow Minecraft's native UV/model layout and are generated by `tools/create_family_assets.py`.

Run `gradlew.bat -PgameTests runGameTestServer` for gameplay regression checks. Run `gradlew.bat -PrenderChecks runClient` for a Minecraft-rendered doll image in `artwork/doll_native_render.png`. Both development-only suites are excluded from normal release jars.
