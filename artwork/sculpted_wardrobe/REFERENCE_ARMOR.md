# Reference armor pass — 1.13.0

Six sets (24 wearable pieces) use independently authored reference-led geometry:

- Dreamweave: open patchwork hood with temple charm, layered guards, crescent bib, split tassets and embroidered cape.
- Comfy Silk: padded nightcap, ivory/navy quilt jacket, round collar, pockets, bow, drawstrings and soft cuffed boots.
- Buttonguard: enclosed button mask, stepped framing, violet scarf, layered guards and ragged spider-embroidered back cloth.
- Garden Vines: mossy guardian helmet, dimensional vine wraps, curling shoots, layered leaves, flowers and turquoise accents.
- Midnight: squared open helmet, crescent shoulders, silver bindings, four-point stars and pointed back mantle. Midnight Cloak is unchanged.
- Miner's: reinforced cap with amber lantern, leather harness, pouches, small pick, blue lantern, strapped knees and heavy boots.

## Asset sources

`reference_sources.json` records every final prompt and original output path. All icons and six fabric sources were generated with the built-in image tool using the user's design references. Originals are copied here; production textures are packed/downsampled by `tools/export_sculpted_wardrobe.py`. Inventory icons are illustrated images, never Blender renders.

`reference_icons.jpg` is the 24-icon contact sheet. `renders/` and `*_sculpted.blend` are QA previews of the actual wearable meshes, not inventory artwork.

## Runtime

Meshes use the existing humanoid armor renderer and native animation bones. Back cloth has a separate body-following bone with subtle sway. Cloth UVs use box projection with preserved smooth normals, avoiding stretched hood textures. Lantern glass and turquoise motifs are emissive pixels only; they do not add world light or gameplay abilities.

Stats, item IDs, recipes and abilities are unchanged. The other 63 wearable meshes/icons are retained. Tiny seams retain distance culling and all meshes/materials are cached and cleared on resource reload. This is a Minecraft-compatible interpretation, not a pixel-exact reproduction of the reference illustrations.

## Reproduction

1. `python tools/sculpted_wardrobe.py`
2. `python tools/export_sculpted_wardrobe.py`
3. `python tools/reference_contact_sheet.py`
4. Use the existing opt-in clothing render check and clothing GameTests, then a normal `clean build` to exclude test fixtures from the release.

Blender previews are isolated scenes and preserve existing scenes. Each selected preview saves a standalone `.blend` copy. No Blender installation is required to use the mod.

## Verification evidence

- All 30 focused equipment GameTests passed (`run-tests-1790778183257/logs/latest.log`).
- Full Minecraft clothing visual/light/cleanup fixture completed successfully after the new models were loaded. Front, back and raised-limb screenshots: `artwork/clothing_check_21.png` through `26.png`.
- Asset audit compared the other 63 wearables' mesh and icon bytes with the 1.12.0 JAR: unchanged.
- All 24 reference meshes validate finite coordinates, material/bone references and texture availability. Largest individual piece: 10,526 faces.
- Inventory artwork is preserved independently of Blender model renders. Six whole-set Blender files and renders are included for inspection.
- Final package audit checks that release assets match source files and test fixtures are excluded.
