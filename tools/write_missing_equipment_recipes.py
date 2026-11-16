"""Close audited equipment acquisition gaps, preserving unrelated recipes.

Only source/model links and exact mirrored conflicts are checked here. Actual
registered recipe loading/crafting is tested by EquipmentAcquisitionGameTests.
"""
import hashlib
import json
from pathlib import Path
import time
from apply_loot_recipe_overhaul import canonical

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / 'src/main/resources/data/coraline'
KEYS = {'I': 'coraline:other_iron_ingot', 'M': 'coraline:moonstone',
        'V': 'minecraft:vine', 'S': 'minecraft:string', 'P': 'coraline:perfect_ingot',
        'D': 'coraline:other_diamond', 'W': 'minecraft:water_bucket', 'L': 'minecraft:lava_bucket'}
PATTERNS = {
    'miners_chestplate': ['I I', 'IMI', 'III'],
    'miners_leggings': ['IMI', 'I I', 'I I'],
    'miners_boots': ['I I', 'M I'],
    'garden_vines_helmet': ['VMV', 'S S'],
    'garden_vines_chestplate': ['V V', 'SMS', 'VVV'],
    'garden_vines_leggings': ['VMV', 'S S', 'V V'],
    'garden_vines_boots': ['V V', 'SMS'],
    'bottomless_water_bucket': ['DPD', 'PWP', 'MPM'],
    'bottomless_lava_bucket': ['DPD', 'PLP', 'MPM'],
}


def main():
    recipes = {name: {'type': 'minecraft:crafting_shaped', 'category': 'equipment',
        'pattern': pattern, 'key': {c: {'item': KEYS[c]} for c in sorted(set(''.join(pattern)) - {' '})},
        'result': {'item': 'coraline:' + name, 'count': 1}} for name, pattern in PATTERNS.items()}
    existing = {p.stem: json.loads(p.read_text(encoding='utf-8-sig')) for p in (DATA / 'recipes').glob('*.json')}
    signatures = {}
    for name, recipe in (existing | recipes).items():
        signature = canonical(recipe)
        if signature is None:
            continue
        output = recipe['result']
        if signature in signatures and signatures[signature][1] != output:
            raise ValueError('Recipe conflict: ' + signatures[signature][0] + ' and ' + name)
        signatures[signature] = (name, output)
    for name in recipes:
        assert (ROOT / f'src/main/resources/assets/coraline/models/item/{name}.json').is_file(), name
        for old_name, recipe in existing.items():
            result = recipe.get('result', {})
            output = result.get('item') if isinstance(result, dict) else result
            if output == 'coraline:' + name and old_name != name:
                raise ValueError('Review alternate acquisition recipe: ' + old_name)
    report = ROOT / 'build-character-cast/equipment-acquisition-source' / time.strftime('%Y%m%dT%H%M%SZ', time.gmtime())
    report.mkdir(parents=True, exist_ok=False)
    changed = []
    for name, recipe in recipes.items():
        path = DATA / f'recipes/{name}.json'
        data = (json.dumps(recipe, indent=2) + '\n').encode()
        prior = path.read_bytes() if path.exists() else None
        if prior != data:
            if prior is not None:
                (report / (name + '.before.json')).write_bytes(prior)
            path.write_bytes(data)
            changed.append({'path': path.relative_to(ROOT).as_posix(), 'sha256': hashlib.sha256(data).hexdigest()})
        material = next(iter(recipe['key'].values()))['item']
        unlock = DATA / f'advancements/recipes/equipment/{name}.json'
        unlock.write_text(json.dumps({'parent': 'minecraft:recipes/root',
            'criteria': {'has_material': {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': [material]}]}}},
            'requirements': [['has_material']], 'rewards': {'recipes': ['coraline:' + name]}}, indent=2) + '\n', encoding='utf-8')
    result = {'status': 'source_written', 'recipes': len(recipes), 'changed': changed,
              'scope': 'Source/model/book links and mirrored recipe conflicts only; native crafting, abilities and release acceptance pending'}
    (report / 'report.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(report / 'report.json')


if __name__ == '__main__':
    main()
