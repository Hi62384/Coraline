"""Merge reusable equipment pools into actual native site-cache reward paths.

Keeps existing resource rolls and unrelated pools. Heart-priced, quest-owned,
and credential items are deliberately not turned into repeatable chest loot.
"""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/data/coraline/loot_tables'


def write(name, value):
    path = ROOT / (name + '.json')
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n', encoding='utf-8')


def item(name, weight=1):
    return {'type': 'minecraft:item', 'name': 'coraline:' + name, 'weight': weight}


def shared(name, entries):
    write('postgame/equipment/' + name, {'type': 'minecraft:chest', 'pools': [{'rolls': 1, 'entries': entries}]})


def reference(name, chance, slot):
    return {'name': 'coraline_equipment_' + slot, 'rolls': 1,
            'conditions': [{'condition': 'minecraft:random_chance', 'chance': chance}],
            'entries': [{'type': 'minecraft:loot_table', 'name': 'coraline:postgame/equipment/' + name}]}


def merge(name, pools):
    path = ROOT / ('postgame/structures/' + name + '.json')
    value = json.loads(path.read_text(encoding='utf-8-sig'))
    replaced = {pool['name'] for pool in pools}
    value['pools'] = [pool for pool in value.get('pools', []) if pool.get('name') not in replaced] + pools
    write('postgame/structures/' + name, value)


def derived(name, parent, pools):
    # Each locked room keeps its family's material profile while selecting
    # equipment independently. Higher tiers are never referenced by the hall.
    write('postgame/structures/' + name, {'type': 'minecraft:chest', 'pools': [
        {'rolls': 1, 'entries': [{'type': 'minecraft:loot_table', 'name': 'coraline:postgame/structures/' + parent}]}, *pools]})


def main():
    shared('ordinary_clothing', [item('raincoat', 3), item('comfy_pajamas', 3), item('star_shirt', 3), item('garden_overalls', 2)])
    shared('rare_wardrobe', [item('midnight_coat', 3), item('circus_jacket', 3), item('top_hat', 3), item('silent_slippers'), item('feather_boots'), item('midnight_goggles')])
    shared('exploration', [item('silent_slippers', 2), item('feather_boots', 2), item('midnight_goggles', 2), item('moonlit_lantern', 4), item('dream_staff'), item('dreamstone')])
    shared('factory_tools', [item('other_iron_lockpick', 5), item('other_flint_and_steel', 4), item('potent_other_flint_and_steel', 2), item('miners_helmet', 3), item('midnight_crossbow'), item('button_mace', 2)])
    shared('sewing_tools', [item('button_eye', 5), item('other_iron_thread_cutter', 4), item('needle_rapier'), item('thread_whip')])
    early = ['comfy_silk', 'buttonguard', 'dreamweave', 'midnight', 'midnight_cloak']
    late = ['spidervision', 'beldam_weave', 'soulbound', 'super_spidervision', 'potent_beldam_weave', 'soulbound_potent_beldam_weave', 'luck']
    shared('early_armor', [item(f'{armor}_{piece}') for armor in early for piece in ['helmet', 'chestplate', 'leggings', 'boots']])
    shared('deep_armor', [item(f'{armor}_{piece}', 1 if armor in late[-3:] else 3) for armor in late for piece in ['helmet', 'chestplate', 'leggings', 'boots']])
    shared('perfect_treasure', [item('perfect_' + piece) for piece in ['helmet', 'chestplate', 'leggings', 'boots', 'crossbow', 'tree_cutter', 'excavating_tool']] + [item('builders_dream')])
    shared('supernatural_weapons', [item('spidervision_sword', 3), item('midnight_wand', 3), item('unbinding_button_eyes')])

    merge('thread_workroom', [reference('ordinary_clothing', .2, 'clothing'), reference('sewing_tools', .12, 'sewing')])
    merge('doll_workshop', [reference('ordinary_clothing', .18, 'clothing'), reference('early_armor', .12, 'armor')])
    merge('clockwork_factory', [reference('factory_tools', .18, 'tools'), reference('early_armor', .12, 'armor')])
    merge('soul_glass_mine', [reference('factory_tools', .12, 'tools'), reference('exploration', .12, 'exploration')])
    merge('rift_drill_center', [reference('factory_tools', .2, 'tools'), reference('supernatural_weapons', .08, 'weapons')])
    derived('doll_workshop_control', 'doll_workshop', [reference('rare_wardrobe', .3, 'wardrobe'), reference('exploration', .15, 'exploration')])
    derived('clockwork_factory_control', 'clockwork_factory', [reference('deep_armor', .12, 'armor'), reference('rare_wardrobe', .12, 'wardrobe')])
    derived('soul_glass_mine_vault', 'soul_glass_mine', [reference('deep_armor', .18, 'armor'), reference('perfect_treasure', .025, 'perfect')])
    derived('rift_drill_center_control', 'rift_drill_center', [reference('deep_armor', .16, 'armor'), reference('perfect_treasure', .02, 'perfect')])


if __name__ == '__main__':
    main()
