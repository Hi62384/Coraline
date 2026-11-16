"""Read-only structure/loot resource contract; not native acceptance."""
from pathlib import Path
import hashlib
import json
import time

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / 'src/main/resources/data/coraline'
tracked = {}


def read(path):
    raw = path.read_bytes()
    tracked[path.relative_to(ROOT).as_posix()] = hashlib.sha256(raw).hexdigest()
    return json.loads(raw)


def loot_pools(path, ancestors=()):
    """Validate nested shared tables without ignoring hidden credential drops."""
    assert path not in ancestors and len(ancestors) < 12, 'Cyclic/deep structure loot reference'
    table = read(path)
    assert table['type'] == 'minecraft:chest'
    for pool in table['pools']:
        assert pool['rolls'] == 1
        for condition in pool.get('conditions', []):
            assert condition['condition'] == 'minecraft:random_chance' and 0 < condition['chance'] <= 1
        items = []
        for entry in pool['entries']:
            if entry['type'] == 'minecraft:loot_table':
                assert not entry.get('functions') and entry.get('weight', 1) > 0
                namespace, name = entry['name'].split(':', 1)
                assert namespace == 'coraline' and name.startswith('postgame/') and '..' not in name
                yield from loot_pools(DATA / ('loot_tables/' + name + '.json'), (*ancestors, path))
            else:
                items.append(entry)
        if items:
            yield {**pool, 'entries': items}


def main():
    families = ['doll_workshop', 'clockwork_factory', 'soul_glass_mine', 'rift_drill_center', 'rift_soldier_outpost', 'mirror_tower', 'sunken_otherworld_ruins', 'porcelain_mansion', 'dimensional_seam', 'other_village', 'abandoned_village', 'rootbound_temple', 'ancient_boundary_shrine', 'resistance_sanctuary', 'forgotten_observatory', 'grand_theatre', 'endless_carnival', 'abandoned_orphanage', 'entity_heart_chamber', 'false_pink_palace']
    structure_set = read(DATA / 'worldgen/structure_set/infinite_sites.json')
    assert {v['structure'] for v in structure_set['structures']} == {'coraline:' + name for name in families}
    assert all(0 < v['weight'] <= 10 for v in structure_set['structures'])
    placement = structure_set['placement']
    assert placement['type'] == 'coraline:independent_spread'
    assert 16 <= placement['spacing'] <= 256 and 8 <= placement['separation'] < placement['spacing']
    assert placement['separation'] * 16 > 96, 'Structures can overlap adjacent candidate cells'
    prohibited = {'void_heart', 'other_void_sword', 'soul_capturer', 'ghost_eye', 'little_door_story_key'}
    recorded = {}
    for name in families + ['common_supply', 'thread_workroom', 'doll_workshop_control', 'clockwork_factory_control', 'soul_glass_mine_vault', 'rift_drill_center_control', 'rift_soldier_outpost_control', 'abandoned_village_crypt', 'rootbound_temple_sanctum', 'resistance_sanctuary_supplies', 'endless_carnival_supplies']:
        if name in families:
            structure = read(DATA / f'worldgen/structure/{name}.json')
            assert structure['type'] == 'coraline:' + name
            assert structure['step'] == 'surface_structures' and structure['terrain_adaptation'] == 'none'
            assert structure['spawn_overrides'] == {}, 'No fake or vanilla defender substitutions'
            assert structure['biomes']
            for biome in structure['biomes']:
                assert biome.startswith('coraline:postgame/')
                authored = read(DATA / ('worldgen/biome/' + biome.split(':', 1)[1] + '.json'))
                assert authored['features'] == [] and authored['carvers'] == {}
        table = read(DATA / f'loot_tables/postgame/structures/{name}.json')
        assert table['type'] == 'minecraft:chest'
        drops = set()
        for pool in loot_pools(DATA / f'loot_tables/postgame/structures/{name}.json'):
            assert pool['rolls'] == 1
            for condition in pool.get('conditions', []):
                assert condition['condition'] == 'minecraft:random_chance' and 0 < condition['chance'] <= 1
            for entry in pool['entries']:
                assert entry['type'] == 'minecraft:item' and entry['weight'] > 0
                namespace, item = entry['name'].split(':', 1)
                assert namespace in ('minecraft', 'coraline')
                assert not (namespace == 'coraline' and (item in prohibited or item.startswith('other_world_void_')))
                if namespace == 'coraline':
                    assert (ROOT / f'src/main/resources/assets/coraline/models/item/{item}.json').is_file(), item
                drops.add(entry['name'])
                for function in entry.get('functions', []):
                    assert function['function'] == 'minecraft:set_count', 'No credentials/ownership/NBT in ordinary loot'
                    count = function['count']
                    assert count['type'] == 'minecraft:uniform' and 1 <= count['min'] <= count['max'] <= 12
        assert 'coraline:button' in drops and 'coraline:other_iron_ingot' in drops
        recorded[name] = sorted(drops)
    assert 'coraline:soulglass' in recorded['soul_glass_mine']
    assert 'coraline:midnight_crossbow' in recorded['clockwork_factory']
    assert {'coraline:garden_overalls','coraline:raincoat','coraline:comfy_pajamas','minecraft:string'}.issubset(recorded['thread_workroom'])
    cutter=read(DATA/'recipes/other_iron_thread_cutter.json')
    assert cutter['pattern']==['I I',' S ','I I'] and cutter['key']=={'I':{'item':'coraline:other_iron_ingot'},'S':{'item':'minecraft:string'}}
    assert cutter['result']=={'item':'coraline:other_iron_thread_cutter','count':1}
    book=read(DATA/'advancements/recipes/equipment/other_iron_thread_cutter.json')
    assert book['rewards']['recipes']==['coraline:other_iron_thread_cutter']
    assets=ROOT/'src/main/resources/assets/coraline'
    assert read(assets/'models/item/other_iron_thread_cutter.json')['textures']['layer0']=='coraline:item/other_iron_thread_cutter'
    assert (assets/'textures/item/other_iron_thread_cutter.png').is_file()
    bindings=read(assets/'blockstates/infinite_thread_binding.json')
    assert set(bindings['variants'])=={'open=false','open=true'}
    assert read(assets/'models/block/infinite_thread_binding.json')['elements']
    assert read(assets/'models/block/infinite_thread_binding_cut.json')['elements']==[]
    # Validate every authored biome, including biomes not referenced by the
    # first three structures. Original-world biome files must not acquire
    # postgame enemies through an over-broad data edit.
    expected = {
        'button_eyed_wanderer': {'stitched_meadows', 'buttonwood_forest', 'paperwood_forest', 'forgotten_gardens', 'porcelain_plains'},
        'clockwork_watcher': {'clockwork_highlands', 'otherworld_badlands'},
        'rift_soldier': {'white_rift_wastes', 'pale_wasteland', 'threadbound_mountains'},
    }
    actual = {name: set() for name in expected}
    for path in sorted((DATA / 'worldgen/biome').rglob('*.json')):
        biome = read(path)
        for category, entries in biome.get('spawners', {}).items():
            for entry in entries:
                namespace, name = entry['type'].split(':', 1)
                if namespace != 'coraline' or name not in expected:
                    continue
                assert path.parent.name == 'postgame', 'Postgame hostile leaked into original-world biome'
                assert category == 'monster' and 0 < entry['weight'] <= 12
                assert 1 <= entry['minCount'] <= entry['maxCount'] <= 3
                assert path.stem not in actual[name], 'Duplicate custom spawn entry'
                actual[name].add(path.stem)
    assert actual == expected, (actual, expected)
    for name in expected:
        table = read(DATA / f'loot_tables/entities/{name}.json')
        assert table['type'] == 'minecraft:entity'
        for pool in table['pools']:
            for entry in pool['entries']:
                assert entry['type'] == 'minecraft:item'
                assert entry['name'] in {'minecraft:string', 'coraline:other_iron_ingot'}, 'Mob bypassed central Button or story-reward authority'
        assert (ROOT / 'src/main/resources/assets/coraline/textures/models/sculpted/fabric.png').is_file()
    themed = read(DATA / 'tags/entity_types/button_drop_themed.json')
    assert 'coraline:button_eyed_wanderer' in themed['values']
    report = ROOT / 'build-character-cast/structure-source-checks' / time.strftime('%Y%m%dT%H%M%SZ', time.gmtime())
    report.mkdir(parents=True, exist_ok=False)
    (report / 'report.json').write_text(json.dumps({'status': 'passed', 'source_sha256': tracked,
        'families': recorded, 'hostile_biomes': {name: sorted(biomes) for name, biomes in actual.items()},
        'scope': 'Static native-resource links, hostile spawn pools and loot restrictions only; no actual registry, structure generation, mining, loot RNG, persistence, visual or release acceptance'}, indent=2) + '\n')
    print(report / 'report.json')


if __name__ == '__main__':
    main()
