"""Reuse the checked IndexTTS manifest and audio import conventions for exploration."""
import argparse, hashlib, json, math, re, subprocess, shutil
from pathlib import Path
import wave
from types import SimpleNamespace
from visit_event_audio_catalog import EVENT_MANIFESTS, event_directory, check_reference, effective_recordings
from voice_recording_proof import validate as validate_voice

ROOT = Path(__file__).resolve().parents[1]
STATES = ['low_low', 'mid_mid', 'high_low', 'low_high', 'high_high']
PROFILES = ['low_trust_low_suspicion', 'mid_trust_mid_suspicion', 'high_trust_low_suspicion', 'high_suspicion_low_trust', 'high_trust_high_suspicion']

def prepare(prompt, dinner=False):
    text = Path(prompt).read_text(encoding='utf-8-sig')
    source = json.loads((ROOT/'tools/other_mother_welcome.json').read_text(encoding='utf-8'))
    states = {}
    for short, profile in zip(STATES, PROFILES):
        lines = []
        for i in (1, 2):
            match = re.search(r'\*\*exploration_'+short+'_'+str(i)+r'\*\*\s+"([^"]+)"', text)
            if not match: raise ValueError('Missing exact script: '+short+str(i))
            lines.append(match.group(1))
        entry = dict(source['states'][profile]); entry['lines'] = lines
        states[short] = entry
    source.update(states=states, output='voicelines/othermother/events/exploration', prefix='line')
    (ROOT/'tools/exploration_voice_manifest.json').write_text(json.dumps(source, indent=2)+'\n', encoding='utf-8')

def prepare_dinner(prompt):
    text=Path(prompt).read_text(encoding='utf-8-sig'); base=json.loads((ROOT/'tools/other_mother_welcome.json').read_text())
    states={}
    for category in ['invitation','ontime','late','search','finish']:
        for short, profile in zip(STATES,PROFILES):
            entry=dict(base['states'][profile]);entry['lines']=[re.search(r'\*\*'+category+'_'+short+'_'+str(i)+r'\*\*\s+"([^"]+)"',text).group(1) for i in (1,2)]
            states[category+'/'+short]=entry
    base.update(states=states,output='voicelines/othermother/events/family_dinner',prefix='line')
    (ROOT/'tools/dinner_mother_voice_manifest.json').write_text(json.dumps(base,indent=2)+'\n')
    states={}
    for name,words in re.findall(r'\*\*(father_(?:dinner|pass)_[a-z0-9_]+)\*\*\s+"([^"]+)"',text):
        states[name]={'direction':'Warm, cheerful, slightly awkward and playful.','emotion':[.35,0,0,0,0,0,.05,.55],'temperature':.72,'lines':[words],'filename':name+'.wav','source_directory':''}
    assert len(states)==8
    ref='voicelines/otherfather/otherfathersample.wav'
    base.update(states=states,reference=ref,reference_sha256=hashlib.sha256((ROOT/ref).read_bytes()).hexdigest(),output='voicelines/otherfather/events/family_dinner',prefix='father')
    (ROOT/'tools/dinner_father_voice_manifest.json').write_text(json.dumps(base,indent=2)+'\n')

def prepare_study():
    base=json.loads((ROOT/'tools/dinner_father_voice_manifest.json').read_text());source=(ROOT/'src/main/java/com/coraline/feast/VisitEvents.java').read_text()
    states={}
    for node,text in re.findall(r'Map\.entry\("([a-z_]+)",new Node\("([^"]+)"',source):
        if node=='mother':continue
        name='father_study_'+node
        states[name]={'direction':'Warm, absent-minded and gently hesitant; never openly defiant.','emotion':[.25,0,0,.08 if node in ['hesitate','uncomfortable'] else 0,0,0,.04,.6],'temperature':.72,'lines':[text],'filename':name+'.wav','source_directory':''}
    assert len(states)>=18
    base.update(states=states,output='voicelines/otherfather/events/study',prefix='study')
    (ROOT/'tools/father_study_voice_manifest.json').write_text(json.dumps(base,indent=2)+'\n')

def prepare_outing(prompt):
    text=Path(prompt).read_text(encoding='utf-8-sig');base=json.loads((ROOT/'tools/other_mother_welcome.json').read_text())
    states={}
    for category in ['invitation','ontime','late','search','finish']:
        script='invite' if category=='invitation' else category
        for short,profile in zip(STATES,PROFILES):
            entry=dict(base['states'][profile]);entry['lines']=[re.search(r'\*\*'+script+'_'+short+'_'+str(i)+r'\*\*\s+"([^"]+)"',text).group(1) for i in (1,2)]
            states[category+'/'+short]=entry
    for subtype in ['picnic','fishing','stargazing','woodland','boat']:
        entry=dict(base['states'][PROFILES[0]]);entry['lines']=[re.search(r'\*\*'+subtype+'_destination_'+str(i)+r'\*\*\s+"([^"]+)"',text).group(1) for i in (1,2)]
        states['destination/'+subtype]=entry
    assert sum(len(v['lines']) for v in states.values())==60
    base.update(states=states,output='voicelines/othermother/events/family_outing',prefix='line')
    (ROOT/'tools/outing_mother_voice_manifest.json').write_text(json.dumps(base,indent=2)+'\n')
    father=json.loads((ROOT/'tools/dinner_father_voice_manifest.json').read_text());states={}
    for name,words in re.findall(r'\*\*((?:picnic|fishing|stargazing|woodland|boat)_father_(?:start|interaction|finish))\*\*\s+"([^"]+)"',text):
        event='father_outing_'+name
        states[event]={'direction':'Cheerful, affectionate, absent-minded and occasionally ridiculous.','emotion':[.4,0,0,0,0,0,.08,.5],'temperature':.72,'lines':[words],'filename':event+'.wav','source_directory':''}
    assert len(states)==15
    father.update(states=states,output='voicelines/otherfather/events/family_outing',prefix='father')
    (ROOT/'tools/outing_father_voice_manifest.json').write_text(json.dumps(father,indent=2)+'\n')

def prepare_game_night(prompt):
    text=Path(prompt).read_text(encoding='utf-8-sig');base=json.loads((ROOT/'tools/other_mother_welcome.json').read_text());states={}
    for category in ['invitation','ontime','late','search','finish']:
        script='invite' if category=='invitation' else category
        for short,profile in zip(STATES,PROFILES):
            entry=dict(base['states'][profile]);entry['lines']=[re.search(r'\*\*'+script+'_'+short+'_'+str(i)+r'\*\*\s+"([^"]+)"',text).group(1) for i in (1,2)];states[category+'/'+short]=entry
    for game in ['cards','thread','tower','mouseboard','flick','charades']:
        entry=dict(base['states'][PROFILES[0]]);entry['lines']=[re.search(r'\*\*'+game+'_intro_'+str(i)+r'\*\*\s+"([^"]+)"',text).group(1) for i in (1,2)];states['intro/'+game]=entry
    assert sum(len(v['lines']) for v in states.values())==62
    base.update(states=states,output='voicelines/othermother/events/family_game_night',prefix='line');(ROOT/'tools/game_night_mother_voice_manifest.json').write_text(json.dumps(base,indent=2)+'\n')
    father=json.loads((ROOT/'tools/dinner_father_voice_manifest.json').read_text());states={}
    for name,words in re.findall(r'\*\*(father_[a-z0-9_]+)\*\*\s+"([^"]+)"',text):
        event='father_game_night_'+name.removeprefix('father_');states[event]={'direction':'Cheerful, slightly awkward, playful and affectionate.','emotion':[.4,0,0,0,0,0,.08,.5],'temperature':.72,'lines':[words],'filename':event+'.wav','source_directory':''}
    assert len(states)==16
    father.update(states=states,output='voicelines/otherfather/events/family_game_night',prefix='father');(ROOT/'tools/game_night_father_voice_manifest.json').write_text(json.dumps(father,indent=2)+'\n')

def prepare_delivery(prompt):
    text=Path(prompt).read_text(encoding='utf-8-sig');base=json.loads((ROOT/'tools/other_mother_welcome.json').read_text());states={}
    for category,script in [('invitation','invite'),('completion','finish'),('caught_opening','caught')]:
        for short,profile in zip(STATES,PROFILES):
            entry=dict(base['states'][profile]);entry['lines']=[re.search(r'\*\*'+script+'_'+short+'_'+str(i)+r'\*\*\s+"([^"]+)"',text).group(1) for i in (1,2)];states[category+'/'+short]=entry
    assert sum(len(v['lines']) for v in states.values())==30
    base.update(states=states,output='voicelines/othermother/events/special_delivery',prefix='line');(ROOT/'tools/delivery_mother_voice_manifest.json').write_text(json.dumps(base,indent=2)+'\n')

def export(manifest_file='tools/exploration_voice_manifest.json'):
    sounds_path = ROOT/'src/main/resources/assets/coraline/sounds.json'
    lengths_path = ROOT/'src/main/resources/assets/coraline/visit_audio_lengths.json'
    captions_path = ROOT/'src/main/resources/assets/coraline/story_subtitles.json'
    sounds = json.loads(sounds_path.read_text(encoding='utf-8')); lengths = json.loads(lengths_path.read_text(encoding='utf-8'))
    captions = json.loads(captions_path.read_text(encoding='utf-8'))
    manifest = json.loads((ROOT/manifest_file).read_text(encoding='utf-8'))
    proof_path=ROOT/'src/main/resources/assets/coraline/temporary_event_audio.json'
    proofs=json.loads(proof_path.read_text(encoding='utf-8')).get('genuine_events',[]) if proof_path.exists() else []
    known_exports={entry['resource']:entry for entry in proofs}
    exported=set()
    check_reference(ROOT,manifest)
    father='otherfather' in manifest['reference']
    effective=effective_recordings(ROOT)
    quality_cache={}
    # Validate all selected masters before touching their runtime mappings.
    selected=[name for name,record in effective.items() if record.get('original_manifest',record['manifest'])==manifest_file]
    for name in selected:validate_voice(ROOT,effective[name],quality_cache)
    for state in manifest['states']:
        for i, text in enumerate(manifest['states'][state]['lines'], 1):
            profile=manifest['states'][state]
            directory=event_directory(manifest)
            name=state if father else 'other_mother_events_'+directory+'_'+state.replace('/','_')+f'_{i:02d}'
            recording=effective[name];text=recording['text'];src=ROOT/recording['source']
            with wave.open(str(src),'rb') as audio: info=SimpleNamespace(duration=audio.getnframes()/audio.getframerate(),channels=audio.getnchannels())
            assert info.duration > 1.5 and info.channels == 1
            directory=event_directory(manifest)
            name=state if father else 'other_mother_events_'+directory+'_'+state.replace('/','_')+f'_{i:02d}'
            asset=recording['resource']
            target = ROOT/f'src/main/resources/assets/coraline/sounds/{asset}.ogg'
            target.parent.mkdir(parents=True, exist_ok=True)
            # Reuse pinned exports; a repaired WAV must never silently leave a
            # stale Ogg. Shared aliases are encoded once in this invocation.
            proof=known_exports.get(asset,{})
            source_hash=hashlib.sha256(src.read_bytes()).hexdigest()
            pinned=(target.exists() and proof.get('source_sha256')==source_hash
                    and proof.get('ogg_sha256')==hashlib.sha256(target.read_bytes()).hexdigest())
            if asset not in exported and not pinned:
                subprocess.run(['ffmpeg','-y','-v','error','-i',str(src),'-c:a','libvorbis','-q:a','4',str(target)],check=True)
            exported.add(asset)
            sounds[name] = {'sounds':[{'name':'coraline:'+asset,'stream':True}]}
            lengths[name] = math.ceil(info.duration*20)
            words=text.split();chunks=[' '.join(words[j:j+16]) for j in range(0,len(words),16)]
            cues=[];elapsed=0
            for chunk in chunks:
                end=elapsed+info.duration*len(chunk.split())/len(words)
                cues.append({'start':elapsed,'end':end,'text':chunk});elapsed=end
            captions[name] = {'speaker':'Other Father' if father else 'Other Mother','cues':cues}
    for path, content in [(sounds_path,sounds),(lengths_path,lengths),(captions_path,captions)]:
        path.write_text(json.dumps(content,indent=2)+'\n',encoding='utf-8')
    # Masters may now live in the shared verified recording store instead of
    # the old per-manifest output directory. Keep the attribution copy valid.
    (ROOT/manifest['output']).mkdir(parents=True, exist_ok=True)
    for name in ['INDEXTTS_AUDIO_LICENSE.txt','VISIT_EVENT_AUDIO_NOTICE.txt']:
        shutil.copyfile(ROOT/'src/main/resources/META-INF'/name,ROOT/manifest['output']/name)
    print('Exported recordings from',manifest_file,'; merged existing sound/timing/subtitle tables.')

if __name__ == '__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--prompt');parser.add_argument('--dinner-prompt');parser.add_argument('--outing-prompt');parser.add_argument('--game-night-prompt');parser.add_argument('--delivery-prompt');parser.add_argument('--study',action='store_true');parser.add_argument('--manifest',default='tools/exploration_voice_manifest.json');parser.add_argument('--export',action='store_true');parser.add_argument('--all',action='store_true');args=parser.parse_args()
    if args.prompt: prepare(args.prompt)
    if args.dinner_prompt: prepare_dinner(args.dinner_prompt)
    if args.outing_prompt: prepare_outing(args.outing_prompt)
    if args.game_night_prompt: prepare_game_night(args.game_night_prompt)
    if args.delivery_prompt: prepare_delivery(args.delivery_prompt)
    if args.study: prepare_study()
    if args.export: export(args.manifest)
    if args.all:
        for manifest in EVENT_MANIFESTS: export(manifest)
