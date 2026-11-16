"""Peak-safe louder exports of the supplied A-I masters, never generated speech."""
from pathlib import Path
import hashlib,json,math,subprocess,tempfile
from audio_manifest_io import write_json_if_changed

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/coraline'
def sha(path):
    h=hashlib.sha256()
    with path.open('rb') as f:
        for block in iter(lambda:f.read(1024*1024),b''):h.update(block)
    return h.hexdigest()

def main():
    out=ASSETS/'sounds/cat_encounter';out.mkdir(parents=True,exist_ok=True)
    sounds=json.loads((ASSETS/'sounds.json').read_text());lengths=json.loads((ASSETS/'visit_audio_lengths.json').read_text())
    prior_path=ASSETS/'cat_encounter_audio.json';prior=json.loads(prior_path.read_text()) if prior_path.exists() else {}
    report={}
    for letter in 'ABCDEFGHI':
        source=ROOT/'voicelines/cat'/(letter+'.wav');target=out/(letter.lower()+'.ogg');source_sha=sha(source)
        old=prior.get(letter,{})
        if not (old.get('source_sha256')==source_sha and old.get('export_revision')==2 and target.exists() and old.get('ogg_sha256')==sha(target)):
            with tempfile.TemporaryDirectory(prefix='coraline-cat-audio-') as folder:
                staging=Path(folder)/target.name
                # Downmix BEFORE measuring/limiting; doing it afterward can add
                # 3 dB to dual-mono masters and undo the true-peak ceiling.
                subprocess.run(['ffmpeg','-nostdin','-v','error','-y','-threads','1','-filter_threads','1','-i',str(source),'-vn','-af','aformat=sample_rates=24000:channel_layouts=mono,loudnorm=I=-13:TP=-2.5:LRA=7,aresample=24000','-ac','1','-ar','24000','-c:a','libvorbis','-q:a','5',str(staging)],check=True)
                staging.replace(target)
        seconds=float(subprocess.check_output(['ffprobe','-v','error','-show_entries','format=duration','-of','csv=p=0',str(target)],text=True))
        assert math.isfinite(seconds) and seconds>1
        subprocess.run(['ffmpeg','-nostdin','-v','error','-i',str(target),'-f','null','-'],check=True)
        assert sha(source)==source_sha
        event='cat_encounter_'+letter.lower();lengths[event]=math.ceil(seconds*20);sounds[event]={'sounds':[{'name':'coraline:cat_encounter/'+letter.lower(),'stream':True}]}
        report[letter]={'source_sha256':source_sha,'ogg_sha256':sha(target),'seconds':seconds,'ticks':lengths[event],'export_revision':2,'target_lufs':-13,'true_peak_limit_db':-2.5}
    write_json_if_changed(ASSETS/'sounds.json',sounds)
    write_json_if_changed(ASSETS/'visit_audio_lengths.json',lengths)
    write_json_if_changed(prior_path,report)
    print('Imported nine complete cat recordings, normalized louder with peak limiting; masters unchanged.')

if __name__=='__main__':main()
