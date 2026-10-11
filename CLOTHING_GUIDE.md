# Magical clothing — 1.12.0

All 130 additions are in the Coraline creative tab. Wearables use normal armor slots, right-click equipping, durability and armor enchantments. Armor numbers below are **points**, not full shield icons. Individual clothing repairs with wool; most sets repair with leather. This update does not change parent shopping rewards, story scenes or ore generation. Standard recipes are supplied for Other Iron/Diamond tools and armor, and Moonstone armor; the remaining items are creative/command-accessible for now.

| Item | Slot | Armor | Default ability |
| --- | --- | --- | --- |
| Midnight Coat | Chest | 3 | +25% movement speed and +0.09 upward jump velocity during ticks 17000–19000. Nighttime skylit terrain is easier to see during ticks 13000–23000; dark caves remain dark. |
| Star Shirt | Chest | 2 | Moving visual light, strength 10 versus a torch's 14. |
| Raincoat | Chest | 2 | +65% movement speed during rain, including while sheltered. |
| Comfy Pajamas | Chest | 2 | Restores one health point (half a heart) every five seconds. |
| Garden Overalls | Chest | 2 | Finishing edible food restores its nutrition divided by two in health points; steak restores four health points (two hearts). |
| Circus Jacket | Chest | 3 | +0.20 upward jump velocity. |
| Silent Slippers | Feet | 1 | Halves normal mob target-detection visibility. Does not erase existing targets or prevent retaliation; special boss/scripted targeting can bypass normal detection. |
| Feather Boots | Feet | 1 | Caps descent at 0.125 blocks/tick and prevents fall damage. Does not slow upward jumps, swimming, creative flying or elytra flight. |
| Top Hat | Head | 2 | Fancy raised brim, tall crown, burgundy ribbon and gold buckle. |
| Midnight Goggles | Head | 1 | Night Vision while worn, including caves. Preserves independently supplied potion vision. |
| Unbinding Button Eyes | Head | 2 | Outlines all client-loaded mobs through walls, only for the wearer; no server-wide glowing effect. An optional config value can cap the range. |

## Armor sets

Bonuses require all four matching pieces unless explicitly marked per-piece. Helmet/chest/legs/boots totals:

| Set | Points | Ability |
| --- | --- | --- |
| Dreamweave | 2/3/3/1 = 9 | Each piece adds 10% movement speed, 40% total. |
| Comfy Silk | 1/2/2/1 = 6 | Decorative embroidered silk. |
| Midnight | 2/4/4/1 = 11 | High nighttime jumps (+0.5 upward velocity) and slow falling. |
| Buttonguard | 2/5/5/2 = 14 | Each piece reflects 10% of actual incoming damage to its living attacker; no recursive thorns. |
| Midnight Cloak | 1/2/1/1 = 5 | Nighttime normal target-detection multiplier 0.2. Existing aggro/retaliation still applies. |
| Luck | 2/4/4/1 = 11 | +2 Looting levels; only drops whose loot tables support Looting are increased. |
| Garden Vines | 2/5/5/2 = 14 | 50% chance to poison and slow the attacker for four seconds. |
| Moonstone | 3/8/6/3 = 20 | Diamond-equivalent armor, toughness and durability; crafts/repairs with Moonstone. |
| Miners | 2/3/3/1 = 9 | Doubles ordinary block loot after normal tool/Silk Touch/Fortune processing. Containers/block entities, tagged items and nonstackable equipment are excluded. Includes placed ordinary blocks, as requested. |
| Spidervision | 3/8/6/3 = 20 | Faster cobweb movement; 30% chance on mob damage to create up to three temporary webs, five-second cooldown. |
| Beldam-Weave | 5/11/9/5 = 30 | High armor. |
| Soulbound | 4/9/7/4 = 24 | Heals one health point on a damaging mob hit, six more on a kill. |
| Super Spidervision | 3/8/6/3 = 20 | Spider bonuses plus slow wall ascent. Hold jump to ascend webs or grip a nearby ceiling; sneak to descend/release. Ceiling walking keeps the normal upright camera. |
| Potent Beldam-Weave | 6/13/11/6 = 36 | High armor. |
| Soulbound Potent Beldam-Weave | 7/16/13/7 = 43 | Soulbound hit/kill healing plus high armor. |
| Other World Void | 8/18/16/8 = 50 | High armor. |
| Other Diamond | 4/8/6/4 = 22 | Toughness 2.5 and improved durability; repairs with Other Diamond. |
| Other Iron | 3/7/5/3 = 18 | Improved durability; repairs with Other Iron Ingots. |
| Perfect | 5/10/8/5 = 28 | Toughness 3, exceptional durability; repairs with Other Diamond. |

High-tier damage calculations preserve totals above vanilla's 30-point attribute clamp for players wearing these armor items. Vanilla's damage-reduction cap still applies, and the normal HUD still displays at most ten shields. This is not invulnerability. Temporary defensive webs expire after ten loaded seconds, replace only empty air over solid ground, respect placement cancellation/spawn protection, never drop loot and never appear in the protected Other World. They use scheduled ticks, not permanent entity tracking.

## Materials and tools

Other Diamond, Other Iron Ingot, Button and Moonstone are separate items. The two Other materials craft full armor and sword/pickaxe/axe/shovel/hoe using standard patterns. Other Iron has 400 tool durability, mining speed 7 and tier damage bonus 2.5; Other Diamond has 2300 durability, speed 9 and tier damage bonus 4. Their mining levels stay iron and diamond respectively. Existing vanilla items/recipes are not replaced. Button is a crafting material, not a placeable redstone button.

## Special weapons and Perfect tools

Damage values are total normal player attack damage before enchantments, critical hits, cooldown scaling and target defenses:

| Weapon | Damage | Attacks/second | Effect |
| --- | --- | --- | --- |
| Needle Rapier | 3 | 3.2 | Fast thrusts. |
| Moonblade | 6 day / 12 night | 1.6 | Night bonus follows the same natural day/night rules as clothing; removed on unequip/daybreak. |
| Vine Sword | 6 | 1.6 | Four seconds of poison and slowness on successful hits. |
| Button Mace | 10 | 0.55 | Charged hits deal five splash damage to up to sixteen nearby, visible, non-allied mobs within the target area's 2.5-block bounds. Uncharged spam does not trigger splash. |
| Midnight Wand | 5 magic | 2.5-second cooldown | Server-authoritative blue/white particle projectile; expires after impact or four seconds. |
| Spidervision Sword | 8 | 1.4 | Creates an expiring cobweb at the struck mob's feet if there is eligible empty space; respects the same world/build protections as spider armor. |
| Thread Whip | 5 | One-second cooldown | Adds 2.5 blocks of entity reach and pulls mobs toward the wielder, respecting knockback resistance; animated lash and whip crack on use. |

Perfect sword/pickaxe/axe/shovel/hoe have 3500 durability, mining speed 14, tier damage bonus 5 and enchantability 25. Perfect armor has 28 total armor, toughness 3 and durability multiplier 50. These are stronger than diamond and repair with Other Diamond; no new ore or automatic loot source is introduced. Weapon ability values are configurable in `serverconfig/coraline-weapons.toml`.

## Foods

Added omelet, bacon and eggs, cotton candy, popcorn, sweet strawberries, scrambled eggs, muffin, pizza, cupcakes, lemonade, orange juice, hot dog in bun, panini, cocoa beetles, lollipop, perfect roast dinner and bottomless candy bag.

- Cotton candy: three bites, each restoring two food points, with saved per-stack bite state and three progressively eaten icons. The final bite consumes the stick.
- Popcorn: 32 servings shown by its bar. Holding right-click eats one serving every half-second, restoring one food point. Stops consuming when full; releasing stops nibbling. Remaining servings persist in item damage data. Not enchantable or repairable.
- Bottomless candy bag: never consumed, restores two food points (one drumstick) with a 0.01 saturation multiplier.
- Perfect roast dinner: 16 food points (eight drumsticks) and high saturation.
- Juices use the drinking animation. Creative-mode consumption follows the usual unlimited-item behavior.

Chest outfits occupy one slot even when their worn model includes trousers or coat tails. All abilities require the correct equipped slot, not merely carrying the item. Speed bonuses are transient and do not stack repeatedly. Potion effects from other sources are not removed.

## Configuration

Forge synchronizes `serverconfig/coraline-clothing.toml` from each world/server to its clients. In singleplayer it is under `saves/<world>/serverconfig/`. Copy desired defaults into `defaultconfigs/coraline-clothing.toml` for new worlds. Configurable values cover midnight timing, speed, jump, regeneration, food healing, detection, fall speed and visual light strength. Midnight/rain abilities require a dimension with a natural, non-fixed day/night cycle; no permanent Nether/End bonuses.

Set values also configure speed per piece, retaliation, poison chance/duration, Looting, mining multiplier, spider webs/cooldown/expiry, climbing speed, healing and outline distance. `serverconfig/coraline-foods.toml` configures nutrition/saturation for all seventeen foods and popcorn servings/timing. Server values synchronize to clients. Defaults above describe shipped settings.

## Visuals and compatibility

The current reference-led visual pass replaces the worn meshes and painted
inventory icons for 13 armor sets (Luck through Perfect in the set table above).
The icons are the original individually illustrated 2D armor pieces, not renders
of the worn models or shared pixel templates.
The Raincoat now has a hood, storm flap, utility pockets, straight mid-thigh
hem, and matching flat icon. Perfect Ingot and Soulglass are new material items;
Other Diamond, Other Iron, Moonstone, Perfect, and Soulglass have placeable
storage blocks with reversible nine-material crafting recipes. These additions
do not change armor abilities or progression drops.

### 1.12 sculpted wardrobe

All 87 wearables (19 full sets and 11 standalone pieces) now have individually generated 256px Coraline-inspired inventory illustrations and curved, articulated wearable meshes. Clothing includes raised seams, threaded buttons, rolled lapels, lined split tails, nightcaps, bonnets, garden vines, lace, spool pockets and shaped footwear. The icons are illustrations, not exact screenshots: game models use Minecraft proportions and lighting while carrying the corresponding palette and main tailoring features.

The active assets are `assets/coraline/sculpted/*.json` and `textures/models/sculpted/fabric.png`. `tools/sculpted_wardrobe.py` authors the meshes; `tools/render_sculpted_wardrobe.py` previews the same data in Blender; `tools/export_sculpted_wardrobe.py` validates and exports the illustrated icons. Source art, prompts, provenance and catalogs are in `artwork/sculpted_wardrobe/`. Meshes are cached and batched; fine stitches and secondary embroidery are culled at distance. No gameplay values changed in this update.

All 30 focused clothing/equipment GameTests passed. The live client check renders every piece, verifies lighting/outline cleanup, and includes close-up front, side and animated-limb poses. No shader pack is required; third-party render replacements remain untested.

### Archived 1.11 wardrobe replacement

All 19 armor sets now use authored 3D humanoid models, with 76 matching inventory renders. Designs use cloth, patches, collars, nightcaps, gardening aprons, button fasteners, shawls and spider-thread accessories. Perfect armor uses yellow rainwear, not medieval plate. Armor values and abilities are unchanged.

The authoritative geometry is `assets/coraline/wardrobe/*.json`, generated by `tools/build_tailored_wardrobe.py`. Minecraft and Blender consume those same cuboids. `tools/blender_tailored_wardrobe.py` creates a separate preview scene and renders the icons; `tools/finish_wardrobe_assets.py` exports the icons, factual tooltips and original synthesized whip audio. The editable Blender file and 76 source renders are in `artwork/tailored_wardrobe/`. The original Blender scene was preserved. These icons replace the old illustrations described below; do not run the legacy armor UV generator to update the current models.

Parents have 64 identities per role, preserving existing saved skin indices. `tools/build_parent_wardrobe.py` generates 128-pixel skins and shared-format 3D hair/clothing details. The added layer follows the original head/limb animation, sitting and damage overlays without changing parent AI.

Equipment descriptions are green and ability-only; stat-only tools and armor have no custom description. Wand cooldown defaults to 40 ticks (2 seconds), using `midnightWand.castCooldownTicksV2` so existing worlds receive the new default. Whip attacks and missed swings share `threadWhip.cooldownTicks` (20 ticks / 1 second); right-click also lashes. The server validates cooldowns and broadcasts a short animated cord and original crack sound to nearby clients.

Other World `palette.saturation` defaults to 1.45, configurable in its client visual config. A dedicated world-only color pass leaves HUD, Overworld and other dimensions unchanged; setting it to 1 disables the pass. Star Shirt light uses fractional lightmap values and eighth-block source positions, updates while moving each tick, and only dirties affected render sections. It does not change actual block lighting or mob spawning. Alternate chunk renderers and shader packs remain compatibility-dependent.

### Archived 1.10 asset provenance

Inventory icons: nine original transparent images produced with the built-in image-generation tool, exported at 128×128. Originals are in `artwork/clothing/`; exact prompts are in `artwork/clothing_prompts.json`. Export with `python tools/export_clothing_icons.py`. Generated-image originals remain untouched.

The expanded collection adds 123 icon images (including two eaten-candy variants), also generated with the built-in image-generation tool and exported at 128×128. Originals and exact prompt manifests are in `artwork/armor/`, `artwork/advanced_armor/`, `artwork/foods/`, `artwork/materials/`, `artwork/cotton_stages/`, `artwork/weapons/` and their sibling `*_prompts.json` files. `artwork/equipment_contact_sheet.png` previews them together. `tools/export_equipment_assets.py` preserves originals, validates transparency and exports models/recipes/translations. `tools/create_armor_set_models.py` creates native vanilla-layout worn armor textures; these deliberately retain Minecraft's blocky silhouettes rather than rendering the detailed inventory illustrations directly on the body.

Worn visuals use native animated humanoid armor models: chest/sleeve fabrics, articulated trouser/coat hems, a folded raincoat hood, slipper soles, feathered boots, and a tall 3D hat. Their UV-native texture source is `tools/create_clothing_models.py`; it exports `textures/models/armor/*.png` and item model/translation data.

Star light and coat visibility use two client-only packed-render-light hooks. They never insert blocks, create entities, change server light, or affect spawning. Star light uses a bounded flood-fill that stops at opaque full blocks, tracks up to eight nearby wearers, and refreshes only affected render sections. Stationary sources are cached between periodic geometry checks. Removing clothing, changing dimension or disconnecting clears the visual state.

The coat increases only skylit surface brightness at night, not global gamma or Night Vision. Ordinary rendering is unchanged with neither effect active. Alternative chunk renderers/shader packs that replace vanilla packed-light sampling may need their own compatibility integration; this build targets the project's normal Forge renderer. These are not ray-traced shadows/reflections, and translucent/fluid rendering can differ from opaque terrain.

## Checks

1.13 reference pass: Dreamweave, Comfy Silk, Buttonguard, Garden Vines, Midnight (not Midnight Cloak), and Miner's armor have independent reference-led models, 24 illustrated inventory icons and six dedicated fabric atlases. `tools/dreamweave_model.py`, `tools/reference_wardrobes.py` and `tools/field_wardrobes.py` supply these geometries through the existing generator. Back cloth follows the torso, detailed cloth uses authored normals and per-panel UV projection, and lantern/crystal accents are visual-only emissive surfaces. Source prompts and originals are recorded in `artwork/sculpted_wardrobe/reference_sources.json`. Other sets and standalone pieces retain their 1.12 assets and all abilities remain unchanged.

1.11 verification: all 30 focused equipment/food/weapon GameTests passed, including whip cooldown enforcement and the new wand default. The live client fixture checks local-only mob outlines, goggles cleanup, all inventory models and worn armor, both parent wardrobes and visual light cleanup. A separate effects pass checks the whip cord and Other World color grading. The earlier 1.10 broader regression run passed 78 of 79 tests; its existing supper sequence failure ("Supper needs three chairs") was outside this visual/equipment update.

Clothing GameTests cover armor/slot definitions, midnight and dimension rules, speed cleanup/non-stacking, slow healing, food healing, reduced detection, jump boost and safe slow falling. The opt-in `-PrenderChecks -PclothingVisualCheck runClient` fixture renders the wardrobe and icons, checks moving visual light without server lighting changes, verifies the coat leaves unlit caves unchanged, then verifies cleanup. Test fixtures are excluded from release builds.
