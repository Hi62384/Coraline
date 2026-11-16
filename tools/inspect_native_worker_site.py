"""Read-only block/fixture diagnostic for a stopped disposable native QA world."""
import argparse
import io
import json
from pathlib import Path
import struct
import zlib


def nbt(raw):
    stream = io.BytesIO(raw)
    def unpack(fmt):
        size = struct.calcsize(fmt)
        return struct.unpack(fmt, stream.read(size))[0]
    def string():
        return stream.read(unpack('>H')).decode('utf8')
    def value(kind):
        if kind in (1, 2, 3, 4, 5, 6):
            return unpack({1:'>b', 2:'>h', 3:'>i', 4:'>q', 5:'>f', 6:'>d'}[kind])
        if kind == 7:return stream.read(unpack('>i'))
        if kind == 8:return string()
        if kind == 9:
            child, count = unpack('>b'), unpack('>i')
            return [value(child) for _ in range(count)]
        if kind == 10:
            out = {}
            while (child := unpack('>b')) != 0:
                name = string()
                out[name] = value(child)
            return out
        if kind in (11, 12):
            return [unpack('>i' if kind == 11 else '>q') for _ in range(unpack('>i'))]
        raise ValueError(kind)
    kind = unpack('>b')
    string()
    return value(kind)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('world', type=Path)
    parser.add_argument('x', type=int)
    parser.add_argument('y', type=int)
    parser.add_argument('z', type=int)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    world = args.world.resolve(strict=True)
    if world.parent.parent != root or not world.parent.name.startswith('run-tests-isolated-') or world.name != 'world':
        raise ValueError('Only this runner\'s stopped disposable QA world is in scope')
    cache = {}
    def chunk(x, z):
        key = x >> 4, z >> 4
        if key not in cache:
            path = world/'region'/f'r.{key[0] >> 5}.{key[1] >> 5}.mca'
            with path.open('rb') as source:
                source.seek(4*((key[0] & 31) + (key[1] & 31)*32))
                sector = int.from_bytes(source.read(3), 'big')
                source.seek(sector*4096)
                length = int.from_bytes(source.read(4), 'big')
                if source.read(1) != b'\x02':raise ValueError('Expected zlib chunk')
                cache[key] = nbt(zlib.decompress(source.read(length-1)))
        return cache[key]
    def block(x, y, z):
        section = next((s for s in chunk(x,z)['sections'] if s['Y'] == y >> 4), {})
        state = section.get('block_states', {})
        palette = state.get('palette', [{'Name':'minecraft:air'}])
        if len(palette) == 1:return palette[0]['Name']
        bits = max(4, (len(palette)-1).bit_length())
        per = 64//bits
        index = ((y & 15)*16 + (z & 15))*16 + (x & 15)
        word = state['data'][index//per] & ((1 << 64)-1)
        return palette[(word >> ((index % per)*bits)) & ((1 << bits)-1)]['Name']
    rows = []
    for y in range(args.y-2,args.y+4):
        for z in range(args.z-3,args.z+4):
            rows.append({'y':y, 'z':z, 'x_from':args.x-3,
                         'blocks':[block(x,y,z) for x in range(args.x-3,args.x+7)]})
    entities = [be for c in cache.values() for be in c.get('block_entities',[])
                if abs(be.get('x',0)-args.x) < 16 and abs(be.get('z',0)-args.z) < 16]
    print(json.dumps({'rows':rows,'block_entities':entities}, indent=2))


if __name__ == '__main__':main()
