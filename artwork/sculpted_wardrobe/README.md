# Sculpted wardrobe redesign

The wardrobe contains 87 individually addressed wearable assets: 19 four-piece sets and 11 standalone garments. The replacement uses curved, folded cloth meshes, dimensional seams, buttons, embroidery, linings and family-specific accessories rather than the 1.11 cuboid wardrobe.

`midnight_coat_concept.png` is the user-approved art direction, generated with the built-in image tool. Illustrated icons are design illustrations, not screenshots: the Minecraft-fitted wearable meshes preserve their colors and principal tailoring details, but use game lighting and articulated Minecraft proportions rather than the illustration's studio lighting and natural human proportions. Originals remain in the generated-images folder and the local `icons` directory.

## Asset pipeline

Current reference-led additions: Luck, Moonstone, Miners, Spidervision,
Beldam Weave, Soulbound, Super Spidervision, Potent Beldam Weave,
Soulbound Potent Beldam Weave, Other World Void, Other Diamond, Other Iron,
and Perfect. `tools/concept_armor.py` supplies their four-piece animated
geometry. `tools/install_concept_icons.py` restores the original individually
illustrated 2D armor icons from `artwork/armor`, `artwork/advanced_armor`,
`artwork/materials`, and `artwork/weapons` (not Blender/model renders).
`tools/install_concept_fabrics.py`
installs painted cloth and plate swatches, retained as source images here.
`tools/raincoat_model.py` replaces the old flared coat with a shorter,
straight-hem hooded raincoat. `tools/install_new_material_assets.py` installs
the new material and storage-block artwork and models.

- `tools/sculpted_wardrobe.py` authors indexed meshes attached to native humanoid bones. Split coat tails, sleeves and trouser legs follow the corresponding limbs.
- `tools/render_sculpted_wardrobe.py` imports those exact meshes into an isolated Blender scene for inspection and renders. It does not replace existing Blender scenes. Pass item IDs after `--`; `--inspect` leaves a selected asset ready for inspection without rendering.
- `icon_prompts.json` records the per-item illustration prompts. Each icon was generated separately using the approved coat and its corresponding mesh render as references.
- `tools/export_sculpted_wardrobe.py` validates mesh indices, UVs and alpha, downsamples icons to 256px, exports a low-contrast cloth texture, and produces catalog contact sheets.
- `SculptedArmorModel` caches resources and batches geometry by bone/material. Small stitching is culled beyond 16 blocks, with secondary embroidery culled beyond 32 blocks; silhouettes remain. Resource reload clears the cache.

Armor values, abilities, inventory IDs, and parent rendering are unchanged. The legacy tailored geometry remains because parent clothing still uses it. Do not run old icon generators over the new assets.

## Exact concept prompt

Create a single premium game inventory icon for a MIDNIGHT COAT for a Coraline-inspired Minecraft mod. Transparent background, no text, no character or mannequin visible. A beautiful handmade stop-motion miniature dark ink-blue velvet walking coat, whimsical contemporary storybook clothing, absolutely NOT medieval armor. Three-quarter front view entire garment and sleeves visible, richly sculpted flowing split swallowtail hem, soft rounded folded oversized lapels, teal satin lining, asymmetric mismatched black four-hole buttons with visible cross stitches, tiny silver crescent moon brooch, delicate silver embroidered stars and curling constellation thread across cuffs and lower hem, small stitched patch pocket with one peeping spool of blue thread. Distinct fabric folds, tailored shoulders, subtle velvet pile texture, finely sewn seams. Inviting uncanny Coraline fantasy mood, not a knight, not metal plate, no pauldrons, no breastplate, no boxy Minecraft cuboids, no square rigid torso. Strong clean readable silhouette, high quality hand-painted stylized 3D game item art, saturated deep blues and restrained silver with warm button highlights. Centered with margin. This establishes the actual garment design to be modeled for the wearable item, not decorative armor concept art.
