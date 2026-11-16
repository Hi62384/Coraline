"""Deterministic authored-geometry regressions, including the compact roof shell."""
import json
import unittest
from collections import deque
from pathlib import Path
from repair_rosewood_house import repair, DROP_X, DROP_Z

ROOT=Path(__file__).resolve().parents[1]
def compact(value,removed): return value-sum(strip<value for strip in removed)

class HouseTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        source=json.loads((ROOT/'artwork/otherworld_house/Rosewood_House_Complete_v3.blueprint.json').read_text())
        cls.blocks=repair(source['blocks'])
        cls.grid={(compact(b['x'],DROP_X),b['y'],compact(b['z'],DROP_Z)):b for b in cls.blocks}
    def test_unique_compact_cells(self): self.assertEqual(len(self.blocks),len(self.grid))
    def test_connected_exterior_stairs(self):
        for x in range(28,19,-1):
            y=34-x
            for z in (9,10):
                p=(compact(x,DROP_X),y,compact(z,DROP_Z))
                self.assertTrue(self.grid[p]['id'].endswith('_stairs'),p)
                for dy in (1,2): self.assertNotIn((p[0],y+dy,p[2]),self.grid)
        for step,z in enumerate(z for z in range(54,65) if z not in DROP_Z):
            p=(compact(7,DROP_X),15+step,compact(z,DROP_Z))
            self.assertTrue(self.grid[p]['id'].endswith('_stairs'),p)
            for dy in (1,2): self.assertNotIn((p[0],p[1]+dy,p[2]),self.grid)
    def test_pink_walls_and_no_plot_pedestal(self):
        self.assertTrue(any(b['id']=='minecraft:pink_concrete' for b in self.blocks))
        self.assertFalse(any(b['id']=='minecraft:pink_terracotta' for b in self.blocks))
        self.assertNotIn((compact(62,DROP_X),0,compact(78,DROP_Z)),self.grid)
    def test_attic_sealed_from_sky(self):
        start=(compact(33,DROP_X),25,40);queue=deque([start]);seen={start}
        while queue:
            x,y,z=queue.popleft()
            self.assertFalse(y==45 or x in (0,62) or z in (-2,78),f'Attic leaks to outside at {(x,y,z)}')
            for dx,dy,dz in [(1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)]:
                p=x+dx,y+dy,z+dz
                if 0<=p[0]<=62 and 24<=p[1]<=45 and -2<=p[2]<=78 and p not in self.grid and p not in seen:seen.add(p);queue.append(p)
    def test_vanilla_paired_blocks(self):
        for p,b in self.grid.items():
            props=b['properties'];x,y,z=p
            if b['id'].endswith('_door'):
                other=self.grid.get((x,y+(1 if props['half']=='lower' else -1),z),{})
                self.assertEqual(b['id'],other.get('id'),f'Door half missing: {p}')
            if b['id'].endswith('_bed'):
                dx,dz={'north':(0,-1),'south':(0,1),'east':(1,0),'west':(-1,0)}[props['facing']]
                direction=1 if props['part']=='foot' else -1
                self.assertEqual(b['id'],self.grid.get((x+dx*direction,y,z+dz*direction),{}).get('id'),f'Bed half missing: {p}')
    def test_native_assets_exist(self):
        for b in self.blocks:
            if b['id'].startswith('coraline:'):
                name=b['id'].split(':')[1]
                self.assertTrue((ROOT/f'src/main/resources/assets/coraline/blockstates/{name}.json').is_file(),name)

if __name__=='__main__': unittest.main()
