"""Export the nineteen supplied story masters; merge, never replace shared manifests."""
from pathlib import Path
import hashlib,json,math,subprocess,tempfile
from audio_manifest_io import write_json_if_changed

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/coraline'
def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def main():
    out=ASSETS/'sounds/black_is_traditional';out.mkdir(parents=True,exist_ok=True)
    manifest=ASSETS/'black_is_traditional_audio.json'
    prior=json.loads(manifest.read_text()) if manifest.exists() else {}
    sounds=json.loads((ASSETS/'sounds.json').read_text());lengths=json.loads((ASSETS/'visit_audio_lengths.json').read_text());report={}
    for letter in 'ABCDEFGHIJKLMNOPQRS':
        source=ROOT/'voicelines/story/BlackIsTraditional'/(letter+'.wav');target=out/(letter.lower()+'.ogg');sha=digest(source)
        previous=prior.get(letter,{})
        if not(target.exists() and previous.get('mix_revision')==2 and previous.get('source_sha256')==sha and previous.get('ogg_sha256')==digest(target)):
            with tempfile.TemporaryDirectory(prefix='coraline-black-story-') as folder:
                staging=Path(folder)/target.name
                subprocess.run(['ffmpeg','-nostdin','-v','error','-y','-threads','1','-filter_threads','1','-i',str(source),'-vn','-af','aformat=sample_rates=24000:channel_layouts=mono,loudnorm=I=-13:TP=-2.5:LRA=7,aresample=24000','-ac','1','-ar','24000','-c:a','libvorbis','-q:a','5',str(staging)],check=True)
                staging.replace(target)
        seconds=float(subprocess.check_output(['ffprobe','-v','error','-show_entries','format=duration','-of','csv=p=0',str(target)],text=True))
        if not math.isfinite(seconds) or seconds<=0:raise RuntimeError('Invalid story duration '+letter)
        minimum={'J':1,'L':2,'O':2,'Q':3,'R':1.5}.get(letter,0)
        if seconds<=minimum:raise RuntimeError('Recording too short for specified cue: '+letter)
        subprocess.run(['ffmpeg','-nostdin','-v','error','-threads','1','-i',str(target),'-f','null','-'],check=True)
        if digest(source)!=sha:raise RuntimeError('Original changed '+letter)
        name='black_is_traditional_'+letter.lower();lengths[name]=math.ceil(seconds*20)
        sounds[name]={'sounds':[{'name':'coraline:black_is_traditional/'+letter.lower(),'stream':True}]}
        report[letter]={'source_sha256':sha,'ogg_sha256':digest(target),'seconds':seconds,'ticks':lengths[name],'source_bytes':source.stat().st_size,'mix_revision':2,'target_lufs':-13,'true_peak_limit_db':-2.5}
    write_json_if_changed(ASSETS/'sounds.json',sounds)
    write_json_if_changed(ASSETS/'visit_audio_lengths.json',lengths)
    write_json_if_changed(manifest,report)
    print('Imported 19 complete original Black Is Traditional recordings; exact durations and timed offsets validated.')
if __name__=='__main__':main()
