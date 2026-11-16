"""Authored reusable loot tables used by the real owner cabinet-open adapter."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / 'src/main/resources/data/coraline/loot_tables'


def item(name, low=1, high=None, weight=1):
    value = {'type': 'minecraft:item', 'name': name if ':' in name else 'coraline:' + name, 'weight': weight}
    if low != 1 or high is not None:
        value['functions'] = [{'function': 'minecraft:set_count', 'count': low if high is None else {'type': 'minecraft:uniform', 'min': low, 'max': high}}]
    return value


def pool(entries, chance=None):
    result = {'rolls': 1, 'entries': entries}
    if chance is not None:
        result['conditions'] = [{'condition': 'minecraft:random_chance', 'chance': chance}]
    return result


def reference(name):
    return {'type': 'minecraft:loot_table', 'name': 'coraline:secrets/shared/' + name}


def write(name, pools):
    path = BASE / (name + '.json')
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps({'type': 'minecraft:chest', 'pools': pools}, indent=2) + '\n', encoding='utf-8')


write('secrets/shared/sewing_materials', [
    pool([item('button', 2, 5)]),
    pool([item('other_iron_ingot', 1, 3, 6), item('minecraft:string', 3, 8, 4)]),
])
write('secrets/shared/rare_resources', [
    pool([item('soulglass', 1, 2)]),
    pool([item('other_diamond')], .5),
])
write('secrets/shared/ordinary_clothing', [pool([
    item('raincoat', weight=3), item('comfy_pajamas', weight=3),
    item('star_shirt', weight=3), item('garden_overalls', weight=2)])])

profiles = {
    'hall': [pool([reference('sewing_materials')])],
    'kitchen': [pool([reference('sewing_materials')]), pool([item('minecraft:cookie', 2, 4), item('minecraft:bread', 1, 3)])],
    'bedroom': [pool([reference('sewing_materials')]), pool([reference('ordinary_clothing')], .6)],
    'sitting_room': [pool([reference('sewing_materials')]), pool([item('top_hat', weight=3), item('circus_jacket', weight=2)], .35)],
    'storage': [pool([reference('sewing_materials')]), pool([item('garden_overalls', weight=3), item('midnight_coat', weight=1)], .5), pool([item('other_diamond')], .2)],
    'attic': [pool([reference('sewing_materials')]), pool([reference('rare_resources')]), pool([item('feather_boots'), item('midnight_goggles')], .35)],
    'basement': [pool([reference('sewing_materials')]), pool([reference('rare_resources')]), pool([item('silent_slippers', weight=3), item('midnight_goggles')], .3)],
    'archive': [pool([reference('sewing_materials')]), pool([reference('rare_resources')]), pool([item('moonstone')]), pool([item('midnight_coat'), item('feather_boots'), item('midnight_goggles')], .4)],
    'landing': [pool([reference('sewing_materials')]), pool([reference('ordinary_clothing')], .3)],
}
# The adapter selects the late archive only from an actual late owner visit.
# No ordinary cabinet table contains a Heart or Heart-costing endgame gear.
profiles['archive_late'] = [*profiles['archive'], pool([item('perfect_ingot')], .01)]
for name, pools in profiles.items():
    write('secrets/cabinets/' + name, pools)
print(f'Wrote {len(profiles)} real cabinet profiles and three reusable pools; native loot loading/rolls remain pending.')
