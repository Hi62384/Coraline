"""Export the complete supplied TheCat song, preserving its source master."""
from pathlib import Path
import hashlib, json, math, subprocess, tempfile
from audio_manifest_io import write_json_if_changed
from import_numbered_original_audio import ogg_measure

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/coraline'
SOURCE=ROOT/'music/eventbased/TheCat.mp3'
EVENT='the_cat_music'
RESOURCE='scenes/the_cat_music'

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    source_hash=digest(SOURCE)
    metadata=json.loads(subprocess.check_output(['ffprobe','-v','error','-select_streams','a:0','-show_entries','stream=sample_rate,channels:format=duration','-of','json',str(SOURCE)],text=True))
    stream=metadata['streams'][0];seconds=float(metadata['format']['duration'])
    assert math.isfinite(seconds) and seconds>5
    rate=min(48000,int(stream['sample_rate']))
    receipt_path=ASSETS/'the_cat_music_audio.json'
    previous=json.loads(receipt_path.read_text()) if receipt_path.exists() else {}
    target=ASSETS/'sounds'/f'{RESOURCE}.ogg';target.parent.mkdir(parents=True,exist_ok=True)
    if not(target.exists() and previous.get('source_sha256')==source_hash and previous.get('ogg_sha256')==digest(target)):
        with tempfile.TemporaryDirectory(prefix='coraline-missing-song-') as directory:
            staging=Path(directory)/'complete.ogg'
            subprocess.run(['ffmpeg','-nostdin','-v','error','-y','-threads','1','-filter_threads','1','-i',str(SOURCE),'-vn','-ar',str(rate),'-c:a','libvorbis','-q:a','6',str(staging)],check=True)
            subprocess.run(['ffmpeg','-nostdin','-v','error','-threads','1','-i',str(staging),'-f','null','-'],check=True)
            staging.replace(target)
    measured=ogg_measure(target.read_bytes())
    assert measured['sample_rate']==rate and measured['channels']==int(stream['channels'])
    # MP3 container timing includes encoder padding; decoded audio may be shorter.
    assert abs(measured['seconds']-seconds)<.15 and digest(SOURCE)==source_hash
    sounds=json.loads((ASSETS/'sounds.json').read_text());sounds[EVENT]={'sounds':[{'name':'coraline:'+RESOURCE,'stream':True}]}
    write_json_if_changed(ASSETS/'sounds.json',sounds)
    for name in ('visit_audio_lengths.json','scene_audio_lengths.json'):
        path=ASSETS/name;lengths=json.loads(path.read_text()) if path.exists() else {};lengths[EVENT]=measured['ticks'];write_json_if_changed(path,lengths)
    write_json_if_changed(receipt_path,{'source':'music/eventbased/TheCat.mp3','source_sha256':source_hash,'ogg_sha256':digest(target),'export':measured,'role':'other_world_cat_visit_loop'})
    print('Imported complete TheCat.mp3; unchanged master, looping playback, measured timing.')

if __name__=='__main__':main()
