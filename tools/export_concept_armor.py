"""Refresh only the reference-led armor meshes and their manifest rows."""
from pathlib import Path
import json

import concept_armor

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'src/main/resources/assets/coraline/sculpted'
MANIFEST=ROOT/'artwork/sculpted_wardrobe/manifest.json'
PIECES=('helmet','chestplate','leggings','boots')

manifest=json.loads(MANIFEST.read_text())
entries={entry['id']:entry for entry in manifest}
for style in concept_armor.PALETTES:
    for piece in PIECES:
        name=style+'_'+piece
        data=concept_armor.build(style,piece)
        data['id']=name
        (DEST/(name+'.json')).write_text(json.dumps(data,separators=(',',':')))
        entries[name].update(style=data['style'],piece=piece,
                             polygons=sum(len(m['faces']) for m in data['meshes']))
        print(name,entries[name]['polygons'],'polygons')
MANIFEST.write_text(json.dumps(manifest,indent=2)+'\n')
