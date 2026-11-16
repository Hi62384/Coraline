"""Import supplied rescue/finale masters only; no synthesis or transcription."""
import hashlib
import json
import math
from pathlib import Path
import subprocess
import tempfile
from audio_manifest_io import write_json_if_changed
from audio_envelopes import envelope

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/coraline'

def sources():
    result={}
    for key,file in [('mantis_intro','MotherMakingMe'),('mantis_take','TakeIt'),('mantis_gone','Blurgh')]:
        result['rescue_'+key]='voicelines/otherfather/extra/'+file+'.wav'
    result['rescue_theatre_grab']='voicelines/MissSpinkAndForcible/other/ThiefGiveItBack.wav'
    result['rescue_clothes']='voicelines/story/EvilWitchImNotScared.wav'
    result['rescue_theatre_music']='music/eventbased/Theatre.mp3'
    for letter in 'ABCDE':result['rescue_bobinsky_'+letter.lower()]='voicelines/bobinsky/other/'+letter+'.wav'
    for letter in 'ABCDEFGHIJKMNOPQS':result['finale_'+letter.lower()]='voicelines/story/FinalBossFight/'+letter+'.wav'
    for letter in 'AB':result['reunion_'+letter.lower()]='voicelines/story/ParentsReturn/'+letter+'.wav'
    for letter in 'ABC':result['farewell_'+letter.lower()]='voicelines/story/GhostChildrenFreed/'+letter+'.wav'
    return result

def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    manifest=ASSETS/'rescue_chapters_audio.json'
    prior=json.loads(manifest.read_text(encoding='utf-8')) if manifest.exists() else {}
    sounds=json.loads((ASSETS/'sounds.json').read_text(encoding='utf-8'))
    lengths=json.loads((ASSETS/'visit_audio_lengths.json').read_text(encoding='utf-8'))
    report={};out=ASSETS/'sounds/rescue_chapters';out.mkdir(parents=True,exist_ok=True)
    envelope_path=ASSETS/'visit_voice_envelopes.json'
    envelopes=json.loads(envelope_path.read_text(encoding='utf-8')) if envelope_path.exists() else {}
    for name,relative in sources().items():
        source=ROOT/relative
        if not source.is_file():raise RuntimeError('Missing supplied recording: '+relative)
        sha=digest(source);target=out/(name+'.ogg');old=prior.get(name,{})
        if name=='rescue_theatre_music':
            # Preserve the complete existing performance recording; one physical OGG, two event IDs.
            event=sounds.get('theatre_music',{});entries=event.get('sounds',[])
            if len(entries)!=1:raise RuntimeError('Existing theatre music reference is unavailable')
            reference=entries[0] if isinstance(entries[0],str) else entries[0]['name']
            namespace,path=(reference.split(':',1) if ':' in reference else ('coraline',reference))
            if namespace!='coraline':raise RuntimeError('Unexpected theatre music namespace')
            shared=ASSETS/'sounds'/(path+'.ogg')
            if not shared.is_file() or not lengths.get('theatre_music',0):raise RuntimeError('Existing theatre music/timing is unavailable')
            sounds[name]={'sounds':[dict(entries[0]) if isinstance(entries[0],dict) else entries[0]]}
            lengths[name]=lengths['theatre_music']
            report[name]={'source':relative,'source_sha256':sha,'ogg_sha256':digest(shared),'shared_reference':reference,'ticks':lengths[name]}
            continue
        if not(target.exists() and old.get('source_sha256')==sha and old.get('ogg_sha256')==digest(target)):
            with tempfile.TemporaryDirectory(prefix='coraline-rescue-audio-') as folder:
                staging=Path(folder)/target.name
                music=name.endswith('_music')
                subprocess.run(['ffmpeg','-nostdin','-v','error','-y','-threads','1','-filter_threads','1','-i',str(source),'-vn','-ac','1','-ar','48000' if music else '24000','-c:a','libvorbis','-q:a','5',str(staging)],check=True)
                staging.replace(target)
        seconds=float(subprocess.check_output(['ffprobe','-v','error','-show_entries','format=duration','-of','csv=p=0',str(target)],text=True))
        minimum={'rescue_bobinsky_d':8,'finale_h':2,'finale_n':4,'finale_q':6,'farewell_c':17}.get(name,0)
        if not math.isfinite(seconds) or seconds<=minimum:raise RuntimeError('Recording conflicts with required cue: '+name)
        if digest(source)!=sha:raise RuntimeError('Source master changed: '+relative)
        lengths[name]=math.ceil(seconds*20)
        event=dict(sounds.get(name,{}));event['sounds']=[{'name':'coraline:rescue_chapters/'+name,'stream':True}];sounds[name]=event
        report[name]={'source':relative,'source_sha256':sha,'ogg_sha256':digest(target),'seconds':seconds,'ticks':lengths[name]}
        envelopes[name]=envelope(target)
    write_json_if_changed(ASSETS/'sounds.json',sounds)
    write_json_if_changed(ASSETS/'visit_audio_lengths.json',lengths)
    write_json_if_changed(manifest,report)
    write_json_if_changed(envelope_path,envelopes)
    print('Imported '+str(len(report))+' supplied rescue/finale/reunion recordings; masters unchanged. L/R intentionally unassigned.')

if __name__=='__main__':main()
