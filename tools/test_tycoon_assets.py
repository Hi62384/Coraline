"""Phase 1 recipe/model references, including real installed vanilla texture names."""
import json
import os
from pathlib import Path
import unittest
import zipfile

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/coraline'
IDS=['void_foundation','special_dimension_little_door','world_work_desk','orphan_bed','orphan_food_table','orphan_fridge','water_station','orphan_rug','orphan_lamp','orphan_plant','toy_box','storage_cabinet']
ITEMS=['world_weaver','void_power','soul_essence','orphanage_phone','orphan_play_ball']
class Assets(unittest.TestCase):
    def test_one_heart_and_no_circular_unlock_cost(self):
        recipe=json.loads((ROOT/'src/main/resources/data/coraline/recipes/world_weaver.json').read_text())
        heart=[key for key,value in recipe['key'].items() if value.get('item')=='coraline:void_heart']
        self.assertEqual(sum(row.count(key) for row in recipe['pattern'] for key in heart),1)
        self.assertFalse(any(v.get('item','').startswith('coraline:') and v['item']!='coraline:void_heart' for v in recipe['key'].values()))
        power=json.loads((ROOT/'src/main/resources/data/coraline/recipes/void_power.json').read_text())
        self.assertTrue(all(i['item'].startswith('minecraft:') for i in power['ingredients']))
        self.assertGreater(power['result']['count'],0)
        self.assertFalse((ROOT/'src/main/resources/data/coraline/recipes/soul_essence.json').exists())
    def test_all_rotations_damage_and_models_exist(self):
        for name in IDS:
            states=json.loads((ASSETS/'blockstates'/f'{name}.json').read_text())['variants']
            self.assertEqual(len(states),16)
            for state in states.values():
                path=ASSETS/'models'/f"{state['model'].split(':')[1]}.json"
                self.assertTrue(path.is_file(),path)
            self.assertTrue((ASSETS/'models/item'/f'{name}.json').is_file())
        for name in ITEMS:self.assertGreater(len(json.loads((ASSETS/'models/item'/f'{name}.json').read_text())['elements']),1)
    def test_vanilla_materials_actually_exist(self):
        jar=Path(os.environ.get('APPDATA',''))/'.minecraft/versions/1.20.1/1.20.1.jar'
        if not jar.is_file():self.skipTest('Installed vanilla jar unavailable; native visual QA still required')
        with zipfile.ZipFile(jar) as archive:
            resources=set(archive.namelist())
        for kind,names in [('block',[name+suffix for name in IDS for suffix in ('','_broken')]),('item',ITEMS)]:
            for name in names:
                model=json.loads((ASSETS/'models'/kind/f'{name}.json').read_text())
                for ref in model.get('textures',{}).values():
                    if ref.startswith('minecraft:'):self.assertIn('assets/minecraft/textures/'+ref.split(':',1)[1]+'.png',resources)
    def test_white_void_has_no_terrain_or_spawns(self):
        dimension=json.loads((ROOT/'src/main/resources/data/coraline/dimension/personal_other_world.json').read_text())
        settings=dimension['generator']['settings']
        self.assertEqual(settings['layers'],[]);self.assertEqual(settings['structure_overrides'],[])
        biome=json.loads((ROOT/'src/main/resources/data/coraline/worldgen/biome/white_void.json').read_text())
        self.assertEqual(biome['effects']['sky_color'],0xFFFFFF);self.assertEqual(biome['spawners'],{})
if __name__=='__main__':unittest.main()
