"""Verify original identities, runtime references and exact Ogg-derived timings."""
from pathlib import Path
import argparse, hashlib, json, math, subprocess, zipfile
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/coraline'
PARTS={'cocoa_beetles':'abcdefg','mirror_prison':'abcde','first_escape':'abcdefg','missing_parents':'abcdefg'}

def verify(jar=None):
    archive=zipfile.ZipFile(jar) if jar else None
    def read(name):return archive.read('assets/coraline/'+name) if archive else (ASSETS/name).read_bytes()
    report=json.loads(read('first_escape_audio.json'));sounds=json.loads(read('sounds.json'));lengths=json.loads(read('visit_audio_lengths.json'));envelopes=json.loads(read('visit_voice_envelopes.json'))
    expected={prefix+'_'+c for prefix,letters in PARTS.items() for c in letters}
    assert set(report)==expected, 'All 26 originals must be represented exactly once'
    for name in sorted(expected):
        entry=report[name];source=ROOT/entry['source'];assert source.is_file(),source
        assert hashlib.sha256(source.read_bytes()).hexdigest()==entry['source_sha256'], 'Original master changed: '+name
        prefix,letter=name.rsplit('_',1);path='sounds/'+prefix+'/'+letter+'.ogg'
        assert hashlib.sha256(read(path)).hexdigest()==entry['ogg_sha256'], 'Packaged recording identity changed: '+name
        assert sounds[name]['sounds']==[{'name':'coraline:'+prefix+'/'+letter,'stream':True}], 'Wrong sound mapping: '+name
        assert lengths[name]==entry['ticks']==math.ceil(entry['seconds']*20), 'Timing mismatch: '+name
        assert name in envelopes and envelopes[name] and max(envelopes[name])>0, 'Missing speech envelope: '+name
    from PIL import Image
    import io
    with Image.open(io.BytesIO(read('textures/block/insect_parlor_wallpaper.png'))) as image:assert image.size==(512,512)
    for item in ('story_cane','story_shoe'):
        model=json.loads(read('models/item/'+item+'.json'));assert model['elements'] and model['display']['gui'], 'Missing native item geometry: '+item
    if archive:
        assert archive.testzip() is None
        assert not any('GameTests' in p or 'VisualCheck' in p or p.startswith(('artwork/','voicelines/')) for p in archive.namelist()), 'Development/source artifacts leaked into release'
        largest=sorted(archive.infolist(),key=lambda e:e.compress_size,reverse=True)[:10]
        print(json.dumps({'jar':str(Path(jar).resolve()),'bytes':Path(jar).stat().st_size,'sha256':hashlib.sha256(Path(jar).read_bytes()).hexdigest(),'largest_compressed_entries':[{'path':e.filename,'bytes':e.compress_size} for e in largest]},indent=2))
    print('PASS: 26 complete original identities, merged timings, runtime mappings, wallpaper and native item models.')

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--jar');args=parser.parse_args();verify(args.jar)
