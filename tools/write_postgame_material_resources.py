"""Write authored block resources; merge shared tags and translations safely.

PNG masters are imported separately. The release resource optimizer remains
responsible for resizing generated runtime copies, never these source assets.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/coraline'
DATA = RES / 'data/coraline'


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n', encoding='utf-8')


def tag(kind, name, values):
    path = RES / f'data/minecraft/tags/{kind}/{name}.json'
    current = json.loads(path.read_text(encoding='utf-8-sig')) if path.exists() else {'replace': False, 'values': []}
    current['values'] = list(dict.fromkeys(current['values'] + ['coraline:' + v for v in values]))
    write(path, current)


def cube(name, texture=None, parent='cube_all', textures=None, cutout=False):
    model = {'parent': 'minecraft:block/' + parent,
             'textures': textures or {'all': 'coraline:block/postgame/' + (texture or name)}}
    if cutout:
        model['render_type'] = 'minecraft:cutout_mipped'
    write(ASSETS / f'models/block/{name}.json', model)
    write(ASSETS / f'blockstates/{name}.json', {'variants': {'': {'model': 'coraline:block/' + name}}})
    write(ASSETS / f'models/item/{name}.json', {'parent': 'coraline:block/' + name})


def loot(name, conditions=None, drop=None):
    entry = {'type': 'minecraft:item', 'name': 'coraline:' + (drop or name)}
    if conditions:
        entry['conditions'] = conditions
    write(DATA / f'loot_tables/blocks/{name}.json', {
        'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [entry],
        'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})


ROCK = ['porcelain_stone', 'voidstone', 'candy_stone', 'clockwork_stone',
        'mirrorstone', 'threadbound_rock', 'pale_rift_stone', 'stitched_clay']
SOIL = ['stitched_grass', 'other_soil', 'rootbound_soil', 'velvet_mycelium', 'mirror_sand']
FUNGI = ['velvet_mushroom_block', 'velvet_mushroom_stem']
SPECIES = ['buttonwood', 'paperwood', 'lanternwood']
LOGS = [s + '_log' for s in SPECIES]
PLANKS = [s + '_planks' for s in SPECIES]
LEAVES = [s + '_leaves' for s in SPECIES]
ALL = ROCK + SOIL + ['otherworld_moss'] + FUNGI + LOGS + PLANKS + LEAVES

for name in ROCK + SOIL + ['otherworld_moss'] + PLANKS:
    if name == 'stitched_grass':
        cube(name, parent='cube_bottom_top', textures={
            'top': 'coraline:block/postgame/stitched_grass_top',
            'bottom': 'coraline:block/postgame/other_soil',
            'side': 'coraline:block/postgame/other_soil'})
    else:
        cube(name)
for name in FUNGI:
    cube(name, 'velvet_mycelium' if name.endswith('_block') else 'otherwood_end')
for name in LOGS:
    cube(name, parent='cube_column', textures={'end': 'coraline:block/postgame/otherwood_end',
                                             'side': 'coraline:block/postgame/' + name})
    write(ASSETS / f'blockstates/{name}.json', {'variants': {
        'axis=y': {'model': 'coraline:block/' + name},
        'axis=x': {'model': 'coraline:block/' + name, 'x': 90, 'y': 90},
        'axis=z': {'model': 'coraline:block/' + name, 'x': 90}}})
for name in LEAVES:
    cube(name, cutout=True)
for name in ALL:
    loot(name)
for name in LEAVES:
    loot(name, [{'condition': 'minecraft:any_of', 'terms': [
        {'condition': 'minecraft:match_tool', 'predicate': {'items': ['minecraft:shears']}},
        {'condition': 'minecraft:match_tool', 'predicate': {'enchantments': [
            {'enchantment': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}]}])
for species in SPECIES:
    write(DATA / f'recipes/{species}_planks.json', {
        'type': 'minecraft:crafting_shapeless', 'category': 'building',
        'ingredients': [{'item': 'coraline:' + species + '_log'}],
        'result': {'item': 'coraline:' + species + '_planks', 'count': 4}})
tag('blocks', 'mineable/pickaxe', ROCK)
tag('blocks', 'mineable/shovel', SOIL)
tag('blocks', 'mineable/axe', LOGS + PLANKS + FUNGI)
tag('blocks', 'mineable/hoe', LEAVES + ['otherworld_moss'])
for kind in ['blocks', 'items']:
    tag(kind, 'logs', LOGS)
    tag(kind, 'logs_that_burn', LOGS)
    tag(kind, 'planks', PLANKS)
    tag(kind, 'leaves', LEAVES)

# Explicit authored atmosphere and strata; no imported vanilla biome features,
# mobs or structures. Those adapters must be implemented independently.
PROFILES = [
    ('stitched_meadows', 'stitched_grass', 'other_soil', 'threadbound_rock', 0x9693bd, 0xaea5d8, .7, .65),
    ('buttonwood_forest', 'stitched_grass', 'rootbound_soil', 'threadbound_rock', 0x797b98, 0x8989af, .6, .8),
    ('porcelain_plains', 'porcelain_stone', 'porcelain_stone', 'porcelain_stone', 0xbfcfe3, 0xc0c9ee, .4, .3),
    ('candy_wonderland', 'candy_stone', 'other_soil', 'candy_stone', 0xe3a1c2, 0xd6b1e5, .9, .5),
    ('velvet_mushroom_forest', 'velvet_mycelium', 'other_soil', 'voidstone', 0x5d416e, 0x75648c, .5, .95),
    ('clockwork_highlands', 'clockwork_stone', 'other_soil', 'clockwork_stone', 0x9b8770, 0xc1a68a, .6, .2),
    ('mirror_marsh', 'mirror_sand', 'other_soil', 'mirrorstone', 0x87a8bb, 0xa9bfd5, .7, .95),
    ('threadbound_mountains', 'threadbound_rock', 'threadbound_rock', 'threadbound_rock', 0x82839b, 0xaaa4c0, .1, .3),
    ('paperwood_forest', 'rootbound_soil', 'other_soil', 'porcelain_stone', 0xc4bdab, 0xe0d5bc, .55, .55),
    ('forgotten_gardens', 'otherworld_moss', 'other_soil', 'porcelain_stone', 0x829d8a, 0xa8c3a7, .8, .8),
    ('pale_wasteland', 'pale_rift_stone', 'pale_rift_stone', 'voidstone', 0xcbc8d4, 0xd5d0e0, .5, .1),
    ('otherworld_badlands', 'stitched_clay', 'stitched_clay', 'stitched_clay', 0xad7d70, 0xd5a398, 1.3, 0),
    ('lanternwood', 'otherworld_moss', 'rootbound_soil', 'voidstone', 0x4d686e, 0x69778c, .45, .8),
    ('stitched_coast', 'mirror_sand', 'mirror_sand', 'threadbound_rock', 0x9caec7, 0xb5c2df, .7, .6),
    ('rootbound_wilds', 'rootbound_soil', 'other_soil', 'threadbound_rock', 0x6e8972, 0x90aa91, .95, .9),
    ('white_rift_wastes', 'pale_rift_stone', 'mirrorstone', 'voidstone', 0xe6dfec, 0xeee7f4, .3, 0),
]
strata = {}
for name, surface, subsoil, rock, fog, sky, temperature, downfall in PROFILES:
    write(DATA / f'worldgen/biome/postgame/{name}.json', {
        'has_precipitation': downfall > .15, 'temperature': temperature, 'downfall': downfall,
        'effects': {'fog_color': fog, 'sky_color': sky, 'water_color': 0x638ab3,
                    'water_fog_color': 0x34476d},
        'spawners': {}, 'spawn_costs': {}, 'carvers': {}, 'features': []})
    strata[name] = {key: {'Name': 'coraline:' + value} for key, value in
                    [('surface', surface), ('subsoil', subsoil), ('rock', rock)]}
write(DATA / 'postgame/terrain.json', {
    'type': 'coraline:infinite_other_world',
    'biome_source': {'type': 'coraline:infinite_climate', 'biomes': {
        row[0]: 'coraline:postgame/' + row[0] for row in PROFILES}},
    'palette': {'strata': strata, 'water': {'Name': 'minecraft:water'},
                'foundation': {'Name': 'minecraft:bedrock'}}})
lang = ASSETS / 'lang/en_us.json'
translations = json.loads(lang.read_text(encoding='utf-8-sig'))
translations.update({'block.coraline.' + name: name.replace('_', ' ').title() for name in ALL})
translations.update({'biome.coraline.postgame.' + row[0]: row[0].replace('_', ' ').title() for row in PROFILES})
write(lang, translations)

print(f'Wrote {len(ALL)} material block resources and {len(PROFILES)} authored biome resources; runtime acceptance pending.')

# The base palette writer also retains the authored underground roster.
from write_postgame_cave_resources import main as write_cave_resources
write_cave_resources()
