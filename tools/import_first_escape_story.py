"""Import complete supplied performances; preserve masters and merge shared audio tables."""
from pathlib import Path
import hashlib, json, math, subprocess, tempfile
from audio_manifest_io import write_json_if_changed

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/coraline'
PARTS={'CocoaBeetles':('cocoa_beetles','ABCDEFG'),'MirrorPrison':('mirror_prison','ABCDE'),
       'FirstEscape':('first_escape','ABCDEFG'),'MissingParents':('missing_parents','ABCDEFG')}
MINIMUM={'cocoa_beetles_e':2,'cocoa_beetles_g':.5,'mirror_prison_c':3,'mirror_prison_d':5,
         'first_escape_b':2,'first_escape_c':1,'first_escape_d':2,'first_escape_f':2}
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    manifest=ASSETS/'first_escape_audio.json';prior=json.loads(manifest.read_text()) if manifest.exists() else {}
    sounds=json.loads((ASSETS/'sounds.json').read_text());lengths=json.loads((ASSETS/'visit_audio_lengths.json').read_text());report={}
    # Preflight every recording before writing shared manifests.
    for part,(prefix,letters) in PARTS.items():
        for letter in letters:
            source=ROOT/'voicelines/story'/part/(letter+'.wav')
            seconds=float(subprocess.check_output(['ffprobe','-v','error','-show_entries','format=duration','-of','csv=p=0',str(source)],text=True))
            if not math.isfinite(seconds) or seconds<=MINIMUM.get(prefix+'_'+letter.lower(),0):raise RuntimeError('Recording shorter than required cue: '+str(source))
    for part,(prefix,letters) in PARTS.items():
        out=ASSETS/'sounds'/prefix;out.mkdir(parents=True,exist_ok=True)
        for letter in letters:
            name=prefix+'_'+letter.lower();source=ROOT/'voicelines/story'/part/(letter+'.wav');target=out/(letter.lower()+'.ogg');sha=digest(source);old=prior.get(name,{})
            if not(target.exists() and old.get('source_sha256')==sha and old.get('ogg_sha256')==digest(target)):
                with tempfile.TemporaryDirectory(prefix='coraline-escape-audio-') as folder:
                    staging=Path(folder)/target.name
                    subprocess.run(['ffmpeg','-nostdin','-v','error','-y','-threads','1','-filter_threads','1','-i',str(source),'-vn','-ac','1','-ar','24000','-c:a','libvorbis','-q:a','5',str(staging)],check=True)
                    staging.replace(target)
            seconds=float(subprocess.check_output(['ffprobe','-v','error','-show_entries','format=duration','-of','csv=p=0',str(target)],text=True))
            subprocess.run(['ffmpeg','-nostdin','-v','error','-threads','1','-i',str(target),'-f','null','-'],check=True)
            assert digest(source)==sha
            sounds[name]={'sounds':[{'name':'coraline:'+prefix+'/'+letter.lower(),'stream':True}]};lengths[name]=math.ceil(seconds*20)
            report[name]={'source':str(source.relative_to(ROOT)).replace('\\','/'),'source_sha256':sha,'ogg_sha256':digest(target),'seconds':seconds,'ticks':lengths[name]}
    write_json_if_changed(ASSETS/'sounds.json',sounds)
    write_json_if_changed(ASSETS/'visit_audio_lengths.json',lengths)
    write_json_if_changed(manifest,report)
    print('Imported and decoded all 26 complete first-escape recordings; masters unchanged; timing offsets validated.')
if __name__=='__main__':main()
