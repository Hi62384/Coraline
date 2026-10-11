# First-visit feast artwork

Created with the imagegen skill using the built-in image-generation tool (not the fallback CLI).
Originals are in `artwork/feast/`; runtime food sprites are in `src/main/resources/assets/coraline/textures/item/` (64×64), and cake textures in `textures/block/` (256×256). `tools/build_feast_resources.py` exports nearest-neighbor runtime assets and models. All project assets are local; runtime never contacts image-generation services.

## Final food prompt set

Each subject below was generated separately with this template:

> Use case: stylized-concept. Minecraft inventory food sprite: SUBJECT. Extremely appetizing, colorful, crisp chunky pixel art, dark clean outline, distinct small-size silhouette, 3/4 view. One isolated object centered at 85% canvas size on genuine transparent background. No text, no scenery, no ground shadow. Match a polished 64x64 Minecraft resource-pack icon enlarged with sharp pixel edges.

- `golden_chicken_breast.png`: a succulent golden glazed chicken breast, amber-gold crust, glossy buttery highlights, tiny parsley garnish
- `corn_on_the_cob.png`: a plump buttery golden corn on the cob with bright yellow kernels and a curled emerald green husk
- `sweet_peas.png`: a little mound of glossy emerald sweet peas with a bright pea pod, served in a small cream bowl
- `mashed_potatoes.png`: fluffy creamy mashed potatoes, golden melted butter on top, tiny green chives, in a small blue ceramic bowl
- `strawberry_pancakes.png`: a stack of three golden fluffy pancakes covered with glossy ruby strawberry sauce and fresh sliced strawberries
- `sausages.png`: three juicy browned grilled sausages with caramelized stripes and a tiny parsley garnish
- `mango_milkshake.png`: a tall clear glass of vivid golden-orange mango milkshake, whipped cream, mango slice and striped straw
- `strawberry_milkshake.png`: a tall clear glass of luscious pink strawberry milkshake, whipped cream, strawberry and striped straw
- `kiwi_milkshake.png`: a tall clear glass of vivid pale-green kiwi milkshake, whipped cream, kiwi slice and striped straw
- `chocolate_milkshake.png`: a tall clear glass of rich chocolate milkshake, whipped cream, chocolate shavings and striped straw
- `fruit_punch.png`: a clear rounded glass of ruby tropical fruit punch, visible orange and lime slices, ice cubes and striped straw

## Cake top

Reference: the user-supplied welcome-home cake image (lettering reference). Final prompt: new orthographic top-down square strawberry icing texture, edge-to-edge, pink frosting with cream rosettes and red strawberry halves around the perimeter. Dark raspberry cursive text on two lines: “welcome” and “home!”. The o in home has two intertwined curls. Crisp pixel art, no candles, shadows, scenery or other text.

## Cake side

Final prompt: Use case: stylized-concept. Create a new Minecraft cake SIDE texture that matches the supplied strawberry cake top in color and crisp pixel-art style. Orthographic flat seamless horizontal rectangular frosting surface, no perspective, no complete cake object. Fill square canvas edge to edge with soft strawberry-pink icing, piped cream scallop trim along top and bottom edges, a thin ruby strawberry jam and pale cream layer running horizontally across the middle, tiny strawberry piped decorations. Beautiful delicious warm pink, cream and ruby palette. No words, no plate, no background or empty margins. This is a tileable block material texture, polished 128x128 pixel art enlarged with sharp pixels.
