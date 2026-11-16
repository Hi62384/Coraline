"""Authored recipe pass for existing registered equipment.

Preflights mirrored shaped-recipe conflicts before writing; preserves unrelated
recipes. Native crafting/behavior acceptance is a separate required step.
Button Eye components and the Other Void Sword have distinct real item adapters.
"""
import hashlib
import json
from pathlib import Path
import time

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / 'src/main/resources/data/coraline'
MATERIALS = {
    'I': 'coraline:other_iron_ingot', 'P': 'coraline:perfect_ingot',
    'G': 'coraline:soulglass', 'K': 'coraline:soulglass_block',
    'M': 'coraline:moonstone', 'N': 'coraline:moonstone_block',
    'B': 'coraline:button', 'D': 'coraline:other_diamond',
    'H': 'coraline:void_heart', 'X': 'coraline:other_iron_block',
    'S': 'minecraft:string', 'T': 'minecraft:stick', 'L': 'minecraft:leather',
    'F': 'minecraft:flint', 'W': '#minecraft:wool', 'O': 'coraline:perfect_ore', 'E': 'coraline:button_eye',
    'Q': 'coraline:perfect_block',
}
PATTERNS = {
    'story_shoe': ['L ', 'LL'],
    'soul_capturer': ['IKI', 'IHI', 'III'],
    'dream_staff': [' M ', ' I ', ' I '],
    'dreamstone': [' D ', 'DND', ' D '],
    'perfect_tree_cutter': ['PP ', 'PI ', ' I '],
    'other_flint_and_steel': ['I ', ' F'],
    'potent_other_flint_and_steel': ['IG', ' F'],
    'moonlit_lantern': ['III', 'IMI', 'III'],
    'perfect_excavating_tool': ['PPP', 'PTP', ' T '],
    'builders_dream': ['PPP', 'P P', 'PPP'],
    'midnight_crossbow': ['IMI', 'SIS', ' I '],
    'perfect_crossbow': ['IPI', 'SPS', ' I '],
    'needle_rapier': [' P ', ' I ', ' I '],
    'button_mace': ['BBB', 'BXB', ' I '],
    'spidervision_sword': [' D ', ' GD', ' S '],
    'thread_whip': [' OS', ' SO', 'S  '],
    'midnight_wand': [' D ', ' M ', ' D '],
    'miners_helmet': ['III', 'IMI'],
    'button_eye': ['BSB', ' I ', 'BSB'],
    'unbinding_button_eyes': ['GGG', 'EME', 'GGG'],
    'other_void_sword': [' H ', ' H ', ' T '],
}
ARMOR = {
    'dreamweave': [['WMW', 'WSW'], ['W W', 'WMW', 'SWS'], ['WMW', 'S S', 'W W'], ['W W', 'SMS']],
    'comfy_silk': [['WWW', 'S S'], ['W W', 'SWS', 'WWW'], ['WWW', 'S S', 'W W'], ['W W', 'S S']],
    'midnight': [['WMW', 'W W'], ['W W', 'WMW', 'WWW'], ['WMW', 'W W', 'W W'], ['W W', 'M W']],
    'buttonguard': [['BBB', 'B B'], ['B B', 'BBB', 'BBB'], ['BBB', 'B B', 'B B'], ['B B', 'B B']],
    'midnight_cloak': [['WWW', 'M W'], ['W W', 'WWW', 'WMW'], ['WWW', 'M W', 'W W'], ['M W', 'W W']],
    'luck': [['IPI', 'WMW'], ['I I', 'WPW', 'IMI'], ['IPI', 'W W', 'M M'], ['W W', 'IPM']],
    'spidervision': [['DGD', 'S S'], ['D D', 'SGS', 'DDD'], ['DGD', 'S S', 'D D'], ['D D', 'SGS']],
    'beldam_weave': [['GGG', 'G G'], ['G G', 'GGG', 'GGG'], ['GGG', 'G G', 'G G'], ['G G', 'G G']],
    'soulbound': [['GMG', 'G G'], ['G G', 'GMG', 'GGG'], ['GMG', 'G G', 'G G'], ['G G', 'M G']],
    'super_spidervision': [['NGN', 'S S'], ['N N', 'SGS', 'NNN'], ['NGN', 'S S', 'N N'], ['N N', 'SGS']],
    'potent_beldam_weave': [['GKG', 'G G'], ['G G', 'GKG', 'GGG'], ['GKG', 'G G', 'G G'], ['G G', 'K G']],
    'soulbound_potent_beldam_weave': [['KNK', 'K K'], ['K K', 'KNK', 'KKK'], ['KNK', 'K K', 'K K'], ['K K', 'N K']],
    'other_world_void': [['QKQ', 'NHN'], ['Q Q', 'KHK', 'NNN'], ['QKQ', 'N N', 'KHK'], ['K K', 'QHN']],
    'perfect': [['PPP', 'P P'], ['P P', 'PPP', 'PPP'], ['PPP', 'P P', 'P P'], ['P P', 'P P']],
}
FORBIDDEN = {'midnight_coat', 'star_shirt', 'raincoat', 'comfy_pajamas', 'garden_overalls',
             'circus_jacket', 'silent_slippers', 'feather_boots', 'top_hat', 'midnight_goggles'}


def canonical(recipe):
    if recipe.get('type') != 'minecraft:crafting_shaped':
        return None
    pattern = recipe['pattern']
    width = max(map(len, pattern))
    if any(len(row) != width for row in pattern):
        raise ValueError('Uneven recipe rows')
    rows = [[None if c == ' ' else json.dumps(recipe['key'][c], sort_keys=True) for c in row] for row in pattern]
    while rows and all(v is None for v in rows[0]): rows.pop(0)
    while rows and all(v is None for v in rows[-1]): rows.pop()
    if not rows: raise ValueError('Empty shaped recipe')
    while all(row[0] is None for row in rows): rows = [row[1:] for row in rows]
    while all(row[-1] is None for row in rows): rows = [row[:-1] for row in rows]
    forms = [json.dumps(rows), json.dumps([list(reversed(row)) for row in rows])]
    return min(forms)


def generate():
    patterns = dict(PATTERNS)
    for armor, pieces in ARMOR.items():
        for suffix, pattern in zip(['helmet', 'chestplate', 'leggings', 'boots'], pieces):
            patterns[armor + '_' + suffix] = pattern
    recipes = {}
    for name, pattern in patterns.items():
        keys = set(''.join(pattern)) - {' '}
        ingredients = {c: {'tag' if MATERIALS[c].startswith('#') else 'item': MATERIALS[c].lstrip('#')} for c in sorted(keys)}
        recipes[name] = {'type': 'minecraft:crafting_shaped', 'category': 'equipment',
                         'pattern': pattern, 'key': ingredients, 'result': {'item': 'coraline:' + name, 'count': 2 if name == 'button_eye' else 1}}
    return recipes


def main():
    recipes = generate()
    processing = {name: {'type': 'minecraft:smelting', 'category': 'misc',
                        'ingredient': {'item': 'coraline:' + ore}, 'result': 'coraline:' + name,
                        'experience': .7, 'cookingtime': 200}
                  for name, ore in [('moonstone', 'moonstone_ore'), ('soulglass', 'soulglass_ore')]}
    existing = {p.stem: json.loads(p.read_text(encoding='utf-8-sig')) for p in (DATA / 'recipes').glob('*.json')}
    for name, recipe in existing.items():
        result = recipe.get('result', {})
        output = result.get('item') if isinstance(result, dict) else result
        if output in {'coraline:' + v for v in FORBIDDEN}:
            raise ValueError('Inspect obsolete noncraftable-clothing recipe before removal: ' + name)
        if output in {'coraline:' + v for v in recipes} and name not in recipes:
            raise ValueError('Inspect alternate obsolete target recipe: ' + name)
    signatures = {}
    for name, recipe in (existing | recipes | processing).items():
        signature = canonical(recipe)
        if signature is None: continue
        output = recipe['result']
        if signature in signatures and signatures[signature][1] != output:
            raise ValueError('Indistinguishable mirrored recipes: ' + signatures[signature][0] + ' and ' + name)
        signatures[signature] = (name, output)
    stamp = time.strftime('%Y%m%dT%H%M%SZ', time.gmtime())
    report = ROOT / 'build-character-cast/loot-recipe-source' / stamp
    report.mkdir(parents=True, exist_ok=False)
    changes = []
    for name, recipe in (recipes | processing).items():
        path = DATA / 'recipes' / (name + '.json')
        prior = path.read_bytes() if path.exists() else None
        data = (json.dumps(recipe, indent=2) + '\n').encode()
        if prior != data:
            if prior is not None:
                (report / (name + '.before.json')).write_bytes(prior)
            path.write_bytes(data)
            changes.append({'path': path.relative_to(ROOT).as_posix(),
                            'before_sha256': hashlib.sha256(prior).hexdigest() if prior is not None else None,
                            'after_sha256': hashlib.sha256(data).hexdigest()})
        first = next(iter(recipe['key'].values())) if 'key' in recipe else recipe['ingredient']
        predicate = {'items': [first['item']]} if 'item' in first else {'tag': first['tag']}
        advancement = {'parent': 'minecraft:recipes/root',
            'criteria': {'has_material': {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [predicate]}}},
            'requirements': [['has_material']], 'rewards': {'recipes': ['coraline:' + name]}}
        unlock = DATA / ('advancements/recipes/equipment' if 'key' in recipe else 'advancements/recipes/misc') / (name + '.json')
        unlock.parent.mkdir(parents=True, exist_ok=True)
        unlock.write_text(json.dumps(advancement, indent=2) + '\n', encoding='utf-8')
    receipt = {'status': 'source_written', 'equipment_recipes': len(recipes), 'ore_processing_recipes': len(processing), 'changed': changes,
        'scope': 'JSON and exact mirrored-pattern preflight only; native registry, crafting, behavior and release acceptance pending',
        'noncraftable_clothing_existing_recipes': 0,
        'remaining_recipe_dependencies': [],
        'button_eye_conversion': 'Four ordinary Buttons, two String and one Other Iron craft two distinct Button Eyes. No implicit alias or use of quest-bound ghost eyes.'}
    (report / 'report.json').write_text(json.dumps(receipt, indent=2) + '\n')
    print(json.dumps({k: v for k, v in receipt.items() if k != 'changed'}))
    print('Recipe source receipt: ' + str(report / 'report.json'))


if __name__ == '__main__':
    main()
