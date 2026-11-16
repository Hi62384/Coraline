"""Geometry-only regression; no claim of Minecraft graphical verification."""
import json
from pathlib import Path
import unittest

ASSETS = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/coraline'


def model(name):
    return json.loads((ASSETS / 'models/block' / (name + '.json')).read_text(encoding='utf-8-sig'))


def covers(elements, point):
    return any(all(lo <= v <= hi for lo, v, hi in zip(e['from'], point, e['to'])) for e in elements)


class LittleDoorRim(unittest.TestCase):
    def test_brick_backing_never_projects_in_front_of_wooden_frame(self):
        for name in ('tiny_door_closed', 'tiny_door_open', 'tiny_door_portal'):
            with self.subTest(model=name):
                data = model(name)
                brick = [e for e in data['elements'] if any(f['texture'] == '#bricks' for f in e['faces'].values())]
                self.assertTrue(brick, 'The original brick backing disappeared')
                self.assertTrue(all(e['from'][2] >= 14 for e in brick), 'Brick projects in front of the wooden frame')
                self.assertTrue(all(set(e['faces']) == {'north', 'south', 'east', 'west', 'up', 'down'} for e in brick))
                self.assertTrue(all('cullface' not in f for e in brick for f in e['faces'].values()))
                for point in ((.5, 8, 5), (15.5, 8, 5), (8, 15.5, 5)):
                    self.assertFalse(covers(brick, point), 'Freestanding door acquired a deep brick surround')

    def test_portal_surround_has_no_depth_gap_but_keeps_crawling_aperture(self):
        elements = model('tiny_door_portal')['elements']
        surround = [e for e in elements if e.get('name', '').startswith('brick_reveal_') or
                    (e['from'][2] == 11 and e['to'][2] == 14)]
        for z in (i / 4 for i in range(44, 65)):
            for point in ((.5, 8, z), (15.5, 8, z), (8, 15.5, z)):
                self.assertTrue(covers(surround, point), point)
            self.assertFalse(covers(surround, (8, 8, z)), 'Rim seals the tunnel')

    def test_wooden_frame_is_unchanged_in_all_door_states(self):
        frames = []
        for name in ('tiny_door_closed', 'tiny_door_open', 'tiny_door_portal'):
            frame = [e for e in model(name)['elements'] if e['from'][2] == 11 and e['to'][2] == 14]
            self.assertEqual(len(frame), 3)
            self.assertTrue(all(f['texture'] == '#wood' for e in frame for f in e['faces'].values()))
            frames.append(frame)
        self.assertEqual(frames[0], frames[1])
        self.assertEqual(frames[1], frames[2])

    def test_all_facings_use_the_same_fixed_surround(self):
        variants = json.loads((ASSETS / 'blockstates/tiny_door.json').read_text())['variants']
        for facing, rotation in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
            variant = variants[f'facing={facing},open=true,portal=true']
            self.assertEqual(variant['model'], 'coraline:block/tiny_door_portal')
            self.assertEqual(variant['y'], rotation)


if __name__ == '__main__':
    unittest.main()
