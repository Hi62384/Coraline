"""Lossless palette/coordinate export of the user's authored house (no prose execution)."""
import gzip
import hashlib
import json
from repair_rosewood_house import repair
from build_rosewood_fixtures import build
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'artwork/otherworld_house/Rosewood_House_Complete_v3.blueprint.json'
DEST = ROOT / 'src/main/resources/assets/coraline/structures/rosewood_house.json.gz'

def main():
    build()
    raw = SOURCE.read_bytes()
    source = json.loads(raw)
    assert source['schema'] == 'autociv-blueprint-studio/v1'
    palette, indices, blocks, positions = [], {}, [], set()
    repaired=repair(source['blocks'])
    for block in repaired:
        state = block['id'] + '[' + ','.join(f'{k}={v}' for k, v in sorted(block['properties'].items())) + ']'
        if state not in indices:
            indices[state] = len(palette)
            palette.append({'id': block['id'], 'properties': block['properties']})
        pos = tuple(block[k] for k in ('x', 'y', 'z'))
        assert pos not in positions, pos
        positions.add(pos)
        blocks.append([*pos, indices[state]])
    data = {'source_sha256': hashlib.sha256(raw).hexdigest(), 'palette': palette, 'blocks': blocks}
    encoded = json.dumps(data, separators=(',', ':')).encode()
    DEST.parent.mkdir(parents=True, exist_ok=True)
    DEST.write_bytes(gzip.compress(encoded, mtime=0))
    restored = json.loads(gzip.decompress(DEST.read_bytes()))
    assert len(source['blocks']) == 38114
    assert len(restored['blocks']) == len(repaired)
    for original, cell in zip(repaired, restored['blocks']):
        assert cell[:3] == [original[k] for k in ('x','y','z')]
        assert restored['palette'][cell[3]] == {'id': original['id'], 'properties': original['properties']}
    print(f'Losslessly verified {len(blocks)} blocks / {len(palette)} states; {DEST.stat().st_size} runtime bytes')

if __name__ == '__main__':
    main()
