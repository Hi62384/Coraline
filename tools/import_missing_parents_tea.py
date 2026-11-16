"""Complete original recordings, hash-cached exports and merge-only shared manifests."""
from pathlib import Path
import hashlib,json,math,subprocess,tempfile
from audio_manifest_io import write_json_if_changed
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/coraline'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    manifest=ASSETS/'missing_parents_tea_audio.json'
    previous=json.loads(manifest.read_text()) if manifest.exists() else {}
    sounds=json.loads((ASSETS/'sounds.json').read_text());lengths=json.loads((ASSETS/'visit_audio_lengths.json').read_text());report={}
    sources={f'missing_parents_tea_{c.lower()}':ROOT/f'voicelines/story/MissingParentsSpinkAndForcible/{c}.wav' for c in 'ABCDEFGHI'}
    sources.update({f'missing_parents_cat_{c.lower()}':ROOT/f'voicelines/story/MissingParentsCat/{c}.wav' for c in 'ABCDEF'})
    sources.update({f'parent_rescue_{c.lower()}':ROOT/f'voicelines/story/EnterOtherWorldParentRescue/{c}.wav' for c in 'ABCDEFGHIJKLMN'})
    for name,source in sources.items():
        duration=float(subprocess.check_output(['ffprobe','-v','error','-show_entries','format=duration','-of','csv=p=0',str(source)],text=True))
        if duration<=dict(missing_parents_tea_a=2,missing_parents_tea_c=1,missing_parents_tea_g=1,missing_parents_tea_h=.5,missing_parents_tea_i=5).get(name,0):raise RuntimeError('Recording cannot accommodate requested cue: '+str(source))
    for name,source in sources.items():
        prefix=name.rsplit('_',1)[0];target=ASSETS/f'sounds/{prefix}/{name[-1]}.ogg';target.parent.mkdir(parents=True,exist_ok=True);original=sha(source);old=previous.get(name,{})
        if not(target.exists() and old.get('source_sha256')==original and old.get('ogg_sha256')==sha(target)):
            with tempfile.TemporaryDirectory(prefix='coraline-taffy-audio-') as folder:
                staged=Path(folder)/target.name
                subprocess.run(['ffmpeg','-nostdin','-v','error','-y','-threads','1','-filter_threads','1','-i',str(source),'-vn','-ac','1','-ar','24000','-c:a','libvorbis','-q:a','5',str(staged)],check=True);staged.replace(target)
        seconds=float(subprocess.check_output(['ffprobe','-v','error','-show_entries','format=duration','-of','csv=p=0',str(target)],text=True))
        subprocess.run(['ffmpeg','-nostdin','-v','error','-threads','1','-i',str(target),'-f','null','-'],check=True)
        assert sha(source)==original
        sounds[name]={'sounds':[{'name':'coraline:'+prefix+'/'+name[-1],'stream':True}]};lengths[name]=math.ceil(seconds*20)
        report[name]={'source':str(source.relative_to(ROOT)).replace('\\','/'),'source_sha256':original,'ogg_sha256':sha(target),'seconds':seconds,'ticks':lengths[name]}
    # Greeting remains the exact existing tea_visit_a event; never alias it to story A.
    assert sounds['tea_visit_a']['sounds'][0]['name']!='coraline:missing_parents_tea/a'
    for path,value in [(ASSETS/'sounds.json',sounds),(ASSETS/'visit_audio_lengths.json',lengths),(manifest,report)]:write_json_if_changed(path,value)
    print('Imported twenty-nine complete supplied Spink/Forcible, Cat and Parent Rescue story performances; reused existing tea_visit_a greeting. Masters unchanged.')
if __name__=='__main__':main()
