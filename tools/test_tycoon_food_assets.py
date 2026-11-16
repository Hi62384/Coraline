"""Native-model, stock-filter and server-authority source contracts; NOT gameplay QA."""
from pathlib import Path
import hashlib,json,os,unittest,zipfile
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/coraline'
FOODS=['button_biscuits','moonberry_jam_toast','other_fathers_vegetable_soup','golden_garden_pie','little_door_macarons','other_mothers_chocolate_truffles','grand_other_world_supper','welcome_home_cake']
class FoodAssets(unittest.TestCase):
    def test_distinct_real_models_and_no_cake_in_meal_tag(self):
        models=[json.loads((ASSETS/'models/item'/f'{id}.json').read_text()) for id in FOODS]
        self.assertEqual(len({hashlib.sha256(json.dumps(m['elements']).encode()).hexdigest() for m in models}),8)
        self.assertTrue(all(len(m['elements'])>=8 for m in models))
        self.assertGreater(len(models[-1]['elements']),len(models[0]['elements']))
        tag=json.loads((ROOT/'src/main/resources/data/coraline/tags/items/other_world_supper_food.json').read_text())['values']
        self.assertEqual(tag,['coraline:'+id for id in FOODS[:-1]])
    def test_model_materials_and_fridge_frames(self):
        source=Path(os.environ.get('APPDATA',''))/'.minecraft/versions/1.20.1/1.20.1.jar'
        if not source.is_file():self.skipTest('Native vanilla texture archive unavailable')
        with zipfile.ZipFile(source) as z:names=set(z.namelist())
        models=[ASSETS/'models/item'/f'{id}.json' for id in FOODS]
        states=json.loads((ASSETS/'blockstates/other_world_fridge.json').read_text())['variants'];self.assertEqual(len(states),64)
        models += [ASSETS/'models/block'/f'other_world_fridge_open_{i}.json' for i in range(4)]
        models += [ASSETS/'models/block/other_world_supper_table.json']
        for path in models:
            model=json.loads(path.read_text())
            for ref in model['textures'].values():
                self.assertIn('assets/minecraft/textures/'+ref.split(':')[1]+'.png',names)
            for cell in model['elements']:
                self.assertTrue(all(a<=b for a,b in zip(cell['from'],cell['to'])),path)
                if 'rotation' in cell:self.assertIn(cell['rotation']['angle'],[-45,-22.5,0,22.5,45])
    def test_server_menu_filter_and_debug_data_guard(self):
        java=ROOT/'src/main/java/com/coraline/tycoon'
        self.assertIn('boolean mayPlace(ItemStack item)',(java/'TycoonFridgeMenu.java').read_text())
        sync=(java/'TycoonFoodSync.java').read_text();self.assertIn('p.hasPermissions(2)',sync);self.assertIn('if(debug){row.putDouble',sync)
        self.assertIn('actor.getUUID()', (java/'TycoonWelcomeCake.java').read_text())
        self.assertIn('TycoonTransactions.consumeHeld', (java/'TycoonWelcomeCake.java').read_text())
    def test_food_controls_clear_footer_at_supported_gui_sizes(self):
        source=(ROOT/'src/main/java/com/coraline/client/TycoonWorldScreen.java').read_text()
        self.assertIn('Math.min(151+row*26,h-100+row*24)',source)
        self.assertRegex(source,r'button\("Special Gifts",left\+20\+\(w-32\)/2,foodButtonY\(1\)')
        for height in [240,256,300,360,480,720]:
            h=min(360,height-16)
            rows=[min(151+i*26,h-100+i*24) for i in range(3)]
            self.assertGreaterEqual(rows[0],124)
            self.assertTrue(all(a+20<=b for a,b in zip(rows,rows[1:])))
            self.assertLessEqual(rows[-1]+20,h-28)
if __name__=='__main__':unittest.main()
