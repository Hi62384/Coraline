"""Export the original short nonverbal syllables; merge all shared manifests.

Prepared for the next-victim source activation. This is not registered in the
prior release pipeline and must not be run against its frozen runtime assets.
"""
from pathlib import Path
import argparse
import hashlib
import json
import math
import subprocess
import tempfile
import wave
from audio_manifest_io import write_json_if_changed
from import_numbered_original_audio import ogg_measure

ROOT=Path(__file__).resolve().parents[1]
EVENT='ghost_victim_talk'


def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--assets-root',type=Path,default=ROOT/'src/main/resources/assets/coraline')
    args=parser.parse_args();assets=args.assets_root.resolve()
    if not assets.is_relative_to(ROOT):raise ValueError('Runtime output must stay in the checkout')
    master_root=ROOT/'artwork/audio_masters/ghost_victim'
    proof=json.loads((master_root/'provenance.json').read_text(encoding='utf-8'))
    if proof.get('real_person_reference') is not False or len(proof.get('masters',[]))!=4:
        raise ValueError('Expected four original nonverbal masters')
    previous_path=assets/'ghost_victim_audio.json'
    previous=json.loads(previous_path.read_text(encoding='utf-8')) if previous_path.exists() else {}
    rows=[]
    for index,master in enumerate(proof['masters'],1):
        source=(ROOT/master['path']).resolve()
        if source.parent!=master_root.resolve() or sha(source)!=master['sha256']:
            raise ValueError('Original syllable master changed')
        with wave.open(str(source),'rb') as reader:
            if reader.getparams()[:3]!=(1,2,44100):raise ValueError('Unexpected master PCM format')
            seconds=reader.getnframes()/reader.getframerate()
        if not .2<seconds<.6:raise ValueError('Unexpected syllable duration')
        resource=f'ghost_victim/yada_{index:02d}';target=assets/'sounds'/f'{resource}.ogg'
        target.parent.mkdir(parents=True,exist_ok=True)
        old=previous.get(resource,{})
        if not(target.exists() and old.get('source_sha256')==master['sha256'] and old.get('ogg_sha256')==sha(target)):
            with tempfile.TemporaryDirectory(prefix='coraline-child-syllable-') as temporary:
                staging=Path(temporary)/'syllable.ogg'
                subprocess.run(['ffmpeg','-nostdin','-v','error','-y','-threads','1','-filter_threads','1',
                                '-i',str(source),'-vn','-ac','1','-ar','24000','-c:a','libvorbis','-q:a','5',str(staging)],check=True)
                subprocess.run(['ffmpeg','-nostdin','-v','error','-threads','1','-i',str(staging),'-f','null','-'],check=True)
                target.write_bytes(staging.read_bytes())
        measured=ogg_measure(target.read_bytes())
        if measured['sample_rate']!=24000 or measured['channels']!=1 or abs(measured['seconds']-seconds)>.025:
            raise ValueError('Runtime syllable export changed duration/format')
        if sha(source)!=master['sha256']:raise ValueError('Source master changed during export')
        rows.append(dict(resource=resource,source=str(source.relative_to(ROOT)).replace('\\','/'),
                         source_sha256=master['sha256'],ogg_sha256=sha(target),**measured))
    sounds_path=assets/'sounds.json';sounds=json.loads(sounds_path.read_text(encoding='utf-8')) if sounds_path.exists() else {}
    sounds[EVENT]={'subtitle':'subtitle.coraline.ghost_victim_talk',
                   'sounds':[{'name':'coraline:'+row['resource'],'stream':False} for row in rows]}
    write_json_if_changed(sounds_path,sounds)
    for filename in ('visit_audio_lengths.json','scene_audio_lengths.json'):
        path=assets/filename;lengths=json.loads(path.read_text(encoding='utf-8')) if path.exists() else {}
        lengths[EVENT]=max(math.ceil(row['seconds']*20) for row in rows)
        write_json_if_changed(path,lengths)
    language=assets/'lang/en_us.json';language.parent.mkdir(parents=True,exist_ok=True)
    labels=json.loads(language.read_text(encoding='utf-8')) if language.exists() else {}
    labels['subtitle.coraline.ghost_victim_talk']='A child speaks'
    write_json_if_changed(language,labels)
    write_json_if_changed(previous_path,{row['resource']:row for row in rows})
    print('Exported four complete original syllables; shared sound/timing/language manifests merged.')


if __name__=='__main__':main()
