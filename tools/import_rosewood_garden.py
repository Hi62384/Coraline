"""Export authored garden geometry and its irregular terrain surface separately."""
import gzip
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'artwork/otherworld_garden/Rosewood_Moonlit_Garden.blueprint.json'
DEST = ROOT / 'src/main/resources/assets/coraline/structures/rosewood_garden.json.gz'
EARTH = {'stone', 'dirt', 'grass_block', 'coarse_dirt', 'podzol', 'gravel'}

def main():
    raw = SOURCE.read_bytes()
    source = json.loads(raw)
    assert source['schema'] == 'autociv-blueprint-studio/v1'
    palette, indices, blocks, terrain, positions = [], {}, [], {}, set()
    for b in source['blocks']:
        pos = tuple(b[k] for k in ('x', 'y', 'z'))
        assert pos not in positions, pos
        positions.add(pos)
        assert b['id'].startswith('minecraft:')
        state = json.dumps([b['id'], b['properties']], sort_keys=True)
        if state not in indices:
            indices[state] = len(palette)
            palette.append({'id': b['id'], 'properties': b['properties']})
        cell = [*pos, indices[state]]
        if b['phase'] == 'foundation' and b['id'].split(':')[1] in EARTH:
            key = (b['x'], b['z'])
            if key not in terrain or terrain[key][1] < b['y']:
                terrain[key] = cell
        else:
            blocks.append(cell)
    data = {'source_sha256': hashlib.sha256(raw).hexdigest(), 'palette': palette,
            'blocks': blocks, 'terrain': list(terrain.values())}
    DEST.parent.mkdir(parents=True, exist_ok=True)
    DEST.write_bytes(gzip.compress(json.dumps(data, separators=(',', ':')).encode(), mtime=0))
    assert json.loads(gzip.decompress(DEST.read_bytes())) == data
    assert len(source['blocks']) == 33212
    print(f'Garden: {len(blocks)} authored feature cells, {len(terrain)} terrain columns, {DEST.stat().st_size} runtime bytes')

if __name__ == '__main__':
    main()
