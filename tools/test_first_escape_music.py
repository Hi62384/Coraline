"""Full supplied music and flat key checks; no game process or voice generation."""
import json
from pathlib import Path
import unittest
from import_first_escape_music import ROOT,ASSETS,EVENT,RESOURCE,validate

class EscapeMusicAssets(unittest.TestCase):
    def test_actual_export_is_complete_stereo_original(self):
        receipt=validate(ROOT)
        self.assertLess(abs(receipt['export']['seconds']-receipt['seconds']),.15)
        self.assertEqual(receipt['source'],'music/eventbased/FirstEscape.mp3')
        self.assertEqual(receipt['channels'],2)
        self.assertLessEqual(receipt['export']['sample_rate'],48000)
        self.assertGreater(receipt['seconds'],21)

    def test_existing_audio_manifests_were_merged_not_replaced(self):
        table=json.loads((ROOT/ASSETS/'visit_audio_lengths.json').read_text())
        for name in ('parent_rescue_n','missing_parents_cat_f','tea_visit_music','cocoa_beetles_g',EVENT):
            self.assertGreater(table[name],0,name)
        self.assertTrue((ROOT/ASSETS/'sounds'/f'{RESOURCE}.ogg').is_file())
        importer=(ROOT/'tools/import_game_audio.py').read_text()
        self.assertNotIn('lengths={}',importer)
        self.assertIn('write_json_if_changed',importer)

    def test_flat_key_has_four_real_transparent_button_holes(self):
        from PIL import Image
        from collections import deque
        model=json.loads((ROOT/ASSETS/'models/item/little_door_story_key.json').read_text())
        self.assertEqual(model['parent'],'minecraft:item/generated');self.assertNotIn('elements',model)
        self.assertNotIn('display',model)
        image=Image.open(ROOT/ASSETS/'textures/item/little_door_story_key.png').convert('RGBA')
        self.assertLessEqual(max(image.size),256)
        alpha=image.getchannel('A');self.assertEqual(alpha.getextrema(),(0,255))
        remaining={(x,y) for y in range(image.height) for x in range(image.width) if alpha.getpixel((x,y))<128}
        holes=[]
        while remaining:
            queue=deque([remaining.pop()]);size=0;edge=False
            while queue:
                x,y=queue.popleft();size+=1;edge|=x==0 or y==0 or x==image.width-1 or y==image.height-1
                for cell in ((x-1,y),(x+1,y),(x,y-1),(x,y+1)):
                    if cell in remaining:remaining.remove(cell);queue.append(cell)
            if not edge and size>12:holes.append(size)
        self.assertEqual(len(holes),4,holes)

if __name__=='__main__':unittest.main()
