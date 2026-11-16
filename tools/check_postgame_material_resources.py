"""Check authored resources without loading Minecraft or rewriting artwork."""
import json
import re
import hashlib
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/coraline'
terrain = json.loads((RES / 'data/coraline/postgame/terrain.json').read_text())
profiles = set(re.findall(r'^        ([A-Z_]+)\(',
    (ROOT / 'src/main/java/com/coraline/postgame/InfiniteTerrain.java').read_text(), re.M))
assert len(profiles) == 16
profiles = {p.lower() for p in profiles}
cave_profiles={'cave_porcelain','cave_soul_glass','cave_threadroot','cave_clockwork','cave_mirror','cave_white_rift'}
assert set(terrain['biome_source']['biomes']) == profiles | cave_profiles
assert set(terrain['palette']['strata']) == profiles
models = set()
textures = set()
for profile, biome_id in terrain['biome_source']['biomes'].items():
    biome = json.loads((RES / ('data/' + biome_id.replace(':', '/worldgen/biome/') + '.json')).read_text())
    assert all(key in biome['effects'] for key in ['fog_color', 'sky_color', 'water_color', 'water_fog_color'])
    assert biome['features'] == []
    if profile in cave_profiles:
        assert biome['spawners'].get('monster') and not biome['has_precipitation']
        continue
    for layer in terrain['palette']['strata'][profile].values():
        assert layer['Name'].startswith('coraline:')
        assert (ASSETS / ('blockstates/' + layer['Name'].split(':')[1] + '.json')).is_file()
for texture in (ASSETS / 'textures/block/postgame').glob('*.png'):
    textures.add(texture.stem)
for name in ['stitched_grass', 'other_soil', 'rootbound_soil', 'otherworld_moss',
             'velvet_mycelium', 'stitched_clay', 'porcelain_stone', 'voidstone',
             'candy_stone', 'clockwork_stone', 'mirrorstone', 'threadbound_rock',
             'pale_rift_stone', 'mirror_sand', 'velvet_mushroom_block', 'velvet_mushroom_stem'] + [
                 s + '_' + part for s in ['buttonwood', 'paperwood', 'lanternwood']
                 for part in ['log', 'planks', 'leaves']]:
    state = json.loads((ASSETS / f'blockstates/{name}.json').read_text())
    for variant in state['variants'].values():
        assert variant['model'] == 'coraline:block/' + name
    model = json.loads((ASSETS / f'models/block/{name}.json').read_text())
    for texture in model['textures'].values():
        assert texture.removeprefix('coraline:block/postgame/') in textures
    if name.endswith('_leaves'):
        assert model['render_type'] == 'minecraft:cutout_mipped'
    assert json.loads((ASSETS / f'models/item/{name}.json').read_text())['parent'] == 'coraline:block/' + name
    loot = json.loads((RES / f'data/coraline/loot_tables/blocks/{name}.json').read_text())
    assert loot['pools'][0]['entries'][0]['name'] == 'coraline:' + name
    models.add(name)
assert len(models) == 25 and len(textures) == 24
# Perfect uses the existing sculpted ore geometry and golden material, without
# adding a second bitmap or pretending the material block is itself an ore.
model = json.loads((ASSETS / 'models/block/perfect_ore.json').read_text())
assert model['textures']['gem'] == 'coraline:block/perfect_block'
assert len(model['elements']) > 1
assert (ASSETS / 'textures/block/perfect_block.png').is_file()
assert json.loads((ASSETS / 'models/item/perfect_ore.json').read_text())['parent'] == 'coraline:block/perfect_ore'
assert json.loads((ASSETS / 'blockstates/perfect_ore.json').read_text())['variants']['']['model'] == 'coraline:block/perfect_ore'
for tag in ['mineable/pickaxe', 'needs_diamond_tool']:
    assert 'coraline:perfect_ore' in json.loads((RES / f'data/minecraft/tags/blocks/{tag}.json').read_text())['values']
loot = json.loads((RES / 'data/coraline/loot_tables/blocks/perfect_ore.json').read_text())
assert loot['pools'][0]['rolls'] == 1
assert loot['pools'][0]['entries'] == [{'type': 'minecraft:item', 'name': 'coraline:perfect_ore'}]
for method, ticks in [('smelting', 200), ('blasting', 100)]:
    recipe = json.loads((RES / f'data/coraline/recipes/perfect_ingot_from_{method}.json').read_text())
    assert recipe['type'] == 'minecraft:' + method
    assert recipe['ingredient'] == {'item': 'coraline:perfect_ore'}
    assert recipe['result'] == 'coraline:perfect_ingot'
    assert recipe['cookingtime'] == ticks
report = {'status': 'passed', 'scope': 'Static links/schema presence only; not native codec, registry, loot, visual or release acceptance',
          'blocks': len(models), 'biomes': len(profiles), 'source_textures': len(textures), 'additional_perfect_ore': True}
tracked = {p for name in models | {'perfect_ore'} for p in [
    ASSETS / f'models/block/{name}.json', ASSETS / f'models/item/{name}.json',
    ASSETS / f'blockstates/{name}.json', RES / f'data/coraline/loot_tables/blocks/{name}.json']}
tracked.update((RES / 'data/coraline/worldgen/biome/postgame').glob('*.json'))
tracked.update((ASSETS / 'textures/block/postgame').glob('*.png'))
tracked.update(RES / f'data/coraline/recipes/perfect_ingot_from_{method}.json' for method in ['smelting', 'blasting'])
tracked.add(RES / 'data/coraline/postgame/terrain.json')
report['resource_sha256'] = {p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(tracked)}
out = ROOT / 'build-character-cast/postgame-material-resources' / time.strftime('%Y%m%dT%H%M%SZ', time.gmtime()) / 'report.json'
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(json.dumps(report, indent=2) + '\n')
print(json.dumps({k: v for k, v in report.items() if k != 'resource_sha256'}))
print('Resource check report: ' + str(out))
