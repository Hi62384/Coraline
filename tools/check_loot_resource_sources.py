"""Read-only source checks. Native crafting, loot RNG and gameplay are separate."""
from collections import Counter
import hashlib
import json
from pathlib import Path
import time

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
DATA = RES / 'data/coraline'
ASSETS = RES / 'assets/coraline'
tracked = set()


def read(path):
    tracked.add(path)
    return json.loads(path.read_text(encoding='utf-8-sig'))


def counts(recipe):
    result = Counter()
    for row in recipe['pattern']:
        for c in row:
            if c != ' ':
                ingredient = recipe['key'][c]
                assert set(ingredient) in ({'item'}, {'tag'})
                result[ingredient.get('item', '#' + ingredient.get('tag', ''))] += 1
    return result


sets = ['dreamweave', 'comfy_silk', 'midnight', 'buttonguard', 'midnight_cloak', 'luck', 'spidervision',
        'beldam_weave', 'soulbound', 'super_spidervision', 'potent_beldam_weave', 'soulbound_potent_beldam_weave', 'other_world_void', 'perfect']
equipment = ['story_shoe', 'soul_capturer', 'dream_staff', 'dreamstone', 'perfect_tree_cutter', 'other_flint_and_steel',
             'potent_other_flint_and_steel', 'moonlit_lantern', 'perfect_excavating_tool', 'builders_dream',
             'midnight_crossbow', 'perfect_crossbow', 'needle_rapier', 'button_mace', 'spidervision_sword',
             'thread_whip', 'midnight_wand', 'button_eye', 'unbinding_button_eyes', 'other_void_sword', 'miners_helmet']
names = equipment + [armor + '_' + piece for armor in sets for piece in ['helmet', 'chestplate', 'leggings', 'boots']]
assert len(names) == len(set(names)) == 77
for name in names:
    recipe = read(DATA / f'recipes/{name}.json')
    assert recipe['type'] == 'minecraft:crafting_shaped'
    assert recipe['result'] == {'item': 'coraline:' + name, 'count': 2 if name == 'button_eye' else 1}
    assert 1 <= len(recipe['pattern']) <= 3 and len({len(row) for row in recipe['pattern']}) == 1
    assert max(map(len, recipe['pattern'])) <= 3
    assert set(recipe['key']) == set(''.join(recipe['pattern'])) - {' '}
    assert (ASSETS / f'models/item/{name}.json').is_file(), name
    tracked.add(ASSETS / f'models/item/{name}.json')
    unlock = read(DATA / f'advancements/recipes/equipment/{name}.json')
    assert unlock['rewards']['recipes'] == ['coraline:' + name]
    predicates = unlock['criteria']['has_material']['conditions']['items']
    assert len(predicates) == 1 and set(predicates[0]) in ({'items'}, {'tag'})
    cost = counts(recipe)
    if name.startswith(('dreamweave_', 'midnight_', 'midnight_cloak_')) and name not in ('midnight_crossbow', 'midnight_wand'):
        assert cost['coraline:moonstone'] == 1 and cost['#minecraft:wool'] > 1
    if name.startswith('buttonguard_'): assert set(cost) == {'coraline:button'}
    if name.startswith('beldam_weave_'): assert set(cost) == {'coraline:soulglass'}
    if name.startswith('luck_'): assert cost['coraline:perfect_ingot'] == 1
    if name.startswith('other_world_void_'):
        assert cost['coraline:void_heart'] == 1 and cost['coraline:perfect_block'] >= 1
        assert cost['coraline:soulglass_block'] >= 1 and cost['coraline:moonstone_block'] >= 1
        assert cost['coraline:perfect_ingot'] == 0

exact = {
    'unbinding_button_eyes': {'coraline:soulglass': 6, 'coraline:button_eye': 2, 'coraline:moonstone': 1},
    'dreamstone': {'coraline:moonstone_block': 1, 'coraline:other_diamond': 4},
    'other_void_sword': {'coraline:void_heart': 2, 'minecraft:stick': 1},
    'thread_whip': {'coraline:perfect_ore': 2, 'minecraft:string': 3},
    'button_eye': {'coraline:button': 4, 'minecraft:string': 2, 'coraline:other_iron_ingot': 1},
    'other_flint_and_steel': {'minecraft:flint': 1, 'coraline:other_iron_ingot': 1},
}
for name, cost in exact.items(): assert counts(read(DATA / f'recipes/{name}.json')) == cost
capturer = counts(read(DATA / 'recipes/soul_capturer.json'))
assert capturer['coraline:void_heart'] == 1 and capturer['coraline:soulglass_block'] == 1 and capturer['coraline:other_iron_ingot'] > 0
for material in ['moonstone', 'soulglass']:
    processing = read(DATA / f'recipes/{material}.json')
    assert processing['type'] == 'minecraft:smelting' and processing['ingredient'] == {'item': 'coraline:' + material + '_ore'}
    assert processing['result'] == 'coraline:' + material

for material in ['other_iron_ingot', 'other_diamond', 'moonstone', 'perfect_ingot', 'soulglass']:
    block = material.replace('_ingot', '') + '_block'
    forward = read(DATA / f'recipes/{block}.json')
    backward = read(DATA / f'recipes/{material}_from_block.json')
    assert counts(forward) == {'coraline:' + material: 9}
    assert forward['result'].get('count', 1) == 1
    assert backward['ingredients'] == [{'item': 'coraline:' + block}]
    assert backward['result'] == {'item': 'coraline:' + material, 'count': 9}

clothing = {'midnight_coat', 'star_shirt', 'raincoat', 'comfy_pajamas', 'garden_overalls', 'circus_jacket', 'silent_slippers', 'feather_boots', 'top_hat', 'midnight_goggles'}
for path in (DATA / 'recipes').glob('*.json'):
    recipe = read(path)
    output = recipe.get('result', {})
    output = output.get('item', '') if isinstance(output, dict) else output
    assert output not in {'coraline:' + v for v in clothing}, path

profiles = ['hall', 'kitchen', 'bedroom', 'sitting_room', 'storage', 'attic', 'basement', 'archive', 'archive_late', 'landing']
all_loot_items = set()
visited = set()


def inspect_table(name, parents=()):
    assert name not in parents, 'Recursive loot table: ' + name
    table = read(DATA / ('loot_tables/' + name + '.json'))
    assert table['type'] == 'minecraft:chest'
    visited.add(name)
    items = set()
    for pool in table['pools']:
        assert pool['rolls'] == 1
        for condition in pool.get('conditions', []):
            assert condition['condition'] == 'minecraft:random_chance' and 0 < condition['chance'] <= 1
        for entry in pool['entries']:
            assert entry.get('weight', 1) > 0
            if entry['type'] == 'minecraft:loot_table':
                assert entry['name'].startswith('coraline:')
                items.update(inspect_table(entry['name'].split(':')[1], (*parents, name)))
            else:
                assert entry['type'] == 'minecraft:item'
                item = entry['name']
                assert item not in ['coraline:void_heart', 'coraline:other_void_sword', 'coraline:ghost_eye']
                assert not item.startswith('coraline:other_world_void_')
                if item.startswith('coraline:'):
                    assert (ASSETS / 'models/item' / (item.split(':')[1] + '.json')).is_file()
                for function in entry.get('functions', []): assert function['function'] == 'minecraft:set_count'
                items.add(item)
    all_loot_items.update(items)
    return items


for profile in profiles:
    items = inspect_table('secrets/cabinets/' + profile)
    assert 'coraline:button' in items
    if profile != 'archive_late': assert 'coraline:perfect_ingot' not in items
assert {'coraline:' + v for v in clothing}.issubset(all_loot_items)
assert len(visited) == 13
report = {'status': 'passed', 'scope': 'Static source/data checks only; no native registry, crafting, RNG, container, combat, visual or release acceptance',
          'equipment_recipes': len(names), 'armor_pieces': 57, 'loot_profiles': len(profiles), 'referenced_loot_tables': len(visited),
          'source_sha256': {p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(tracked)}}
out = ROOT / 'build-character-cast/loot-resource-source-checks' / time.strftime('%Y%m%dT%H%M%SZ', time.gmtime()) / 'report.json'
out.parent.mkdir(parents=True, exist_ok=False)
out.write_text(json.dumps(report, indent=2) + '\n')
print(json.dumps({k: v for k, v in report.items() if k != 'source_sha256'}))
print('Loot source check report: ' + str(out))
