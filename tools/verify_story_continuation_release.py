"""Audit source identities, runtime story references and exported Blender geometry."""
from pathlib import Path
import argparse,hashlib,json,math,zipfile
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/coraline'

def verify(jar=None):
    archive=zipfile.ZipFile(jar) if jar else None
    def read(name):return archive.read('assets/coraline/'+name) if archive else (ASSETS/name).read_bytes()
    manifest=json.loads(read('missing_parents_tea_audio.json'));sounds=json.loads(read('sounds.json'));lengths=json.loads(read('visit_audio_lengths.json'));envelopes=json.loads(read('visit_voice_envelopes.json'))
    expected={prefix+c for prefix,letters in [('missing_parents_tea_','abcdefghi'),('missing_parents_cat_','abcdef'),('parent_rescue_','abcdefghijklmn')] for c in letters}
    assert set(manifest)==expected
    for name in expected:
        item=manifest[name];assert hashlib.sha256((ROOT/item['source']).read_bytes()).hexdigest()==item['source_sha256'],name
        prefix,letter=name.rsplit('_',1);path=f'sounds/{prefix}/{letter}.ogg'
        assert hashlib.sha256(read(path)).hexdigest()==item['ogg_sha256'],name
        assert sounds[name]['sounds']==[{'name':f'coraline:{prefix}/{letter}','stream':True}],name
        assert lengths[name]==item['ticks']==math.ceil(item['seconds']*20),name
        assert name in envelopes and envelopes[name] and max(envelopes[name])>0,name
    geometry={}
    for name in ('theatre_spink_costume','theatre_forcible_costume','theatre_ocean_set'):
        doc=json.loads(read(f'characters/{name}.json'));bones={b['name'] for b in doc['bones']};assert doc['meshes'] and len(bones)>=4
        for mesh in doc['meshes']:
            assert mesh['bone'] in bones and mesh['faces']
            assert all(math.isfinite(v) for vertex in mesh['vertices'] for v in vertex)
            assert all(0<=i<len(mesh['vertices']) for face in mesh['faces'] for i in face)
        geometry[name]=sum(len(m['vertices']) for m in doc['meshes'])
        assert (ROOT/f'artwork/theatre_performance/{name}.blend').is_file(),name
    for name in ('seeing_stone','debug_complete_first_escape','little_door_story_key'):
        assert json.loads(read(f'models/item/{name}.json'))
    result={'original_complete_recordings':len(expected),'source_masters_unchanged':True,'native_geometry_vertices':geometry,'subtitle_review':'Caption identity and quality proofs are checked separately by verify_numbered_events_audio.py; this geometry/recording audit does not replace that gate.'}
    if archive:
        assert archive.testzip() is None
        assert not any('GameTests' in p or 'VisualCheck' in p or p.endswith(('.blend','.wav')) for p in archive.namelist())
        result['jar_bytes']=Path(jar).stat().st_size
        result['largest_compressed_entries']=[{'name':e.filename,'bytes':e.compress_size} for e in sorted(archive.infolist(),key=lambda e:e.compress_size,reverse=True)[:10]]
    print(json.dumps(result,indent=2))

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--jar');verify(parser.parse_args().jar)
