"""Import complete post-boss songs from preserved source masters; merge shared timing tables."""
from pathlib import Path
import hashlib, json, subprocess
from audio_manifest_io import write_json_if_changed
from import_numbered_original_audio import ogg_measure
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/coraline'
SOURCES={'garden_ghost_eye_music':'GardenGhostEye.mp3','theatre_ghost_eye_music':'TheatreGhostEye.mp3','circus_ghost_eye_music':'CircusGhostEye.mp3'}
def main():
    sounds=json.loads((ASSETS/'sounds.json').read_text(encoding='utf-8'))
    tables={name:json.loads((ASSETS/name).read_text(encoding='utf-8')) for name in ('visit_audio_lengths.json','scene_audio_lengths.json')}
    receipt=ASSETS/'ghost_eye_music_audio.json'
    previous=json.loads(receipt.read_text(encoding='utf-8')) if receipt.exists() else {}
    results={}
    for event,name in SOURCES.items():
        source=ROOT/'sound'/name
        digest=hashlib.sha256(source.read_bytes()).hexdigest()
        target=ASSETS/'sounds/scenes'/f'{event}.ogg'
        old=previous.get(event,{})
        if not(target.exists() and old.get('source_sha256')==digest and old.get('ogg_sha256')==hashlib.sha256(target.read_bytes()).hexdigest()):
            target.parent.mkdir(parents=True,exist_ok=True)
            temporary=target.with_suffix('.pending.ogg')
            subprocess.run(['ffmpeg','-nostdin','-v','error','-y','-threads','1','-filter_threads','1','-i',str(source),'-vn','-ar','48000','-c:a','libvorbis','-q:a','6',str(temporary)],check=True)
            temporary.replace(target)
        measured=ogg_measure(target.read_bytes())
        sounds[event]={'sounds':[{'name':'coraline:scenes/'+event,'stream':True}]}
        for table in tables.values():table[event]=measured['ticks']
        results[event]={'source':'sound/'+name,'source_sha256':digest,'ogg_sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'export':measured}
    write_json_if_changed(ASSETS/'sounds.json',sounds)
    for name,table in tables.items():write_json_if_changed(ASSETS/name,table)
    write_json_if_changed(receipt,results)
if __name__=='__main__':main()
