"""New thread-cutter acquisition/art sources; generated-copy optimizer exports.
Never edits bitmap pixels or overwrites an existing differing master/recipe.
"""
from pathlib import Path
import hashlib
import json
import shutil
from PIL import Image
from apply_loot_recipe_overhaul import canonical

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
DATA = RES / 'data/coraline'
ASSETS = RES / 'assets/coraline'
SOURCE = Path('C:/Users/User1/.codex/generated_images/01a0e5a1-6507-7363-8a26-a56f9f230b40/exec-c78ff8ff-9706-44de-82e0-0f8bb3431246.png')
PROMPT = "Create one single isolated 2D Minecraft mod inventory sprite of an Other Iron Thread Cutter: compact sinister tailor's thread snips, two short sharp silver-gray metal blades, black wrapped handles with a tiny muted purple detail. Make it clearly recognizable as small cutting shears, distinct from a lockpick, axe or sword. Coraline-inspired handmade supernatural sewing tool, restrained square pixel-art appearance, crisp readable silhouette, no perspective render or 3D scene. Center the entire object diagonally from lower left handles to upper right blades, plenty of transparent margin, no text, no frame, no background, no shadow or extra objects. Output true transparent RGBA background; this is a game inventory icon that will be exported at at most 256 pixels while preserving the original master."

def fresh(path, value):
    raw = (json.dumps(value, indent=2) + '\n').encode()
    if path.exists() and path.read_bytes() != raw:
        raise ValueError('Review existing differing source before replacing: ' + str(path))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(raw)

def main():
    recipe = {'type': 'minecraft:crafting_shaped', 'category': 'equipment',
              'pattern': ['I I', ' S ', 'I I'],
              'key': {'I': {'item': 'coraline:other_iron_ingot'}, 'S': {'item': 'minecraft:string'}},
              'result': {'item': 'coraline:other_iron_thread_cutter', 'count': 1}}
    for path in (DATA / 'recipes').glob('*.json'):
        old = json.loads(path.read_text(encoding='utf-8-sig'))
        if canonical(old) == canonical(recipe) and old['result'] != recipe['result']:
            raise ValueError('Mirrored recipe conflict: ' + str(path))
    with Image.open(SOURCE) as icon:
        assert icon.mode == 'RGBA' and icon.getextrema()[3] == (0, 255)
        inspection = {'size': list(icon.size), 'mode': icon.mode, 'alpha_extrema': list(icon.getextrema()[3]),
                      'visual': 'Inspected built-in output: single pair of silver thread snips, dark wrapped grips, purple accent and true transparency; native game appearance pending'}
    master = ROOT / 'artwork/infiltration/other_iron_thread_cutter.png'
    runtime = ASSETS / 'textures/item/other_iron_thread_cutter.png'
    digest = hashlib.sha256(SOURCE.read_bytes()).hexdigest()
    for path in [master, runtime]:
        if path.exists() and hashlib.sha256(path.read_bytes()).hexdigest() != digest:
            raise ValueError('Existing differing artwork: ' + str(path))
        path.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(SOURCE, path)
    manifest = ROOT / 'artwork/infiltration/manifest.json'
    saved = json.loads(manifest.read_text())
    asset = {'id': 'other_iron_thread_cutter', 'generated_source': SOURCE.as_posix(),
             'master': master.relative_to(ROOT).as_posix(), 'runtime_source': runtime.relative_to(ROOT).as_posix(),
             'prompt': PROMPT, 'sha256': digest, 'inspection': inspection}
    prior = next((a for a in saved['assets'] if a['id'] == asset['id']), None)
    if prior is not None and prior != asset: raise ValueError('Existing different image manifest')
    if prior is None: saved['assets'].append(asset)
    manifest.write_text(json.dumps(saved, indent=2)+'\n')
    fresh(DATA / 'recipes/other_iron_thread_cutter.json', recipe)
    fresh(DATA / 'advancements/recipes/equipment/other_iron_thread_cutter.json', {
        'parent': 'minecraft:recipes/root', 'criteria': {'has_material': {'trigger': 'minecraft:inventory_changed',
        'conditions': {'items': [{'items': ['coraline:other_iron_ingot']}]}}}, 'requirements': [['has_material']],
        'rewards': {'recipes': ['coraline:other_iron_thread_cutter']}})
    fresh(ASSETS / 'models/item/other_iron_thread_cutter.json', {'parent': 'minecraft:item/handheld',
          'textures': {'layer0': 'coraline:item/other_iron_thread_cutter'}})
    fresh(ASSETS / 'blockstates/infinite_thread_binding.json', {'variants': {
        'open=false': {'model': 'coraline:block/infinite_thread_binding'},
        'open=true': {'model': 'coraline:block/infinite_thread_binding_cut'}}})
    elements = []
    faces = {face: {'texture': '#thread'} for face in ['north','south','east','west','up','down']}
    for offset in [3, 8, 13]:
        elements += [{'from': [0, offset-.4, 7.6], 'to': [16, offset+.4, 8.4], 'faces': faces},
                     {'from': [7.6, offset-.4, 0], 'to': [8.4, offset+.4, 16], 'faces': faces},
                     {'from': [offset-.4, 0, 7.6], 'to': [offset+.4, 16, 8.4], 'faces': faces},
                     {'from': [7.6, 0, offset-.4], 'to': [8.4, 16, offset+.4], 'faces': faces}]
    fresh(ASSETS / 'models/block/infinite_thread_binding.json', {'render_type': 'minecraft:cutout',
          'textures': {'thread': 'coraline:models/sculpted/fabric', 'particle': 'coraline:models/sculpted/fabric'}, 'elements': elements})
    fresh(ASSETS / 'models/block/infinite_thread_binding_cut.json', {'textures': {'particle': 'coraline:models/sculpted/fabric'}, 'elements': []})
    def entry(name, lo, hi, weight=1):
        return {'type': 'minecraft:item', 'name': name, 'weight': weight, 'functions': [{'function': 'minecraft:set_count',
                 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]}
    pools = [{'rolls': 1, 'entries': [entry(name, lo, hi)]} for name, lo, hi in [
        ('coraline:button',2,5), ('coraline:other_iron_ingot',1,3), ('minecraft:string',4,8)]]
    pools.append({'rolls':1, 'conditions':[{'condition':'minecraft:random_chance','chance':.18}],
                  'entries':[entry('coraline:garden_overalls',1,1,3),entry('coraline:raincoat',1,1,2),entry('coraline:comfy_pajamas',1,1)]})
    fresh(DATA / 'loot_tables/postgame/structures/thread_workroom.json', {'type':'minecraft:chest','pools':pools})
    lang = ASSETS / 'lang/en_us.json'; words = json.loads(lang.read_text(encoding='utf-8-sig'))
    words.update({'item.coraline.other_iron_thread_cutter':'Other Iron Thread Cutter',
        'block.coraline.infinite_thread_binding':'Stitched Binding',
        'message.coraline.thread_cutter_required':'These supernatural threads need an Other Iron Thread Cutter.',
        'message.coraline.thread_cut':'Thread severed.',
        'tooltip.coraline.other_iron_thread_cutter':'Cuts generated supernatural bindings. Successful cuts cost durability.'})
    lang.write_text(json.dumps(words,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print('Thread cutter recipe conflict preflight, exact artwork copies, bindings and loot source resources written. Runtime export/native checks pending.')
    print(json.dumps(inspection))

if __name__ == '__main__': main()
