"""Preserve every authored word; share only identical same-speaker scripts.

Existing original/compact masters are retained byte-for-byte. The older compact
plan remains archival, but its alternate-word substitutions are not a full
dialogue release. New manifests synthesize only genuinely missing scripts.
"""
from pathlib import Path
import json
from visit_event_audio_catalog import authored_recordings,effective_recordings
ROOT=Path(__file__).resolve().parents[1]

def prepare(root=ROOT):
    destination=root/'tools/full_event_voice_plan.json'
    if destination.exists():raise RuntimeError('Full-wording plan already exists; do not silently reselect takes')
    authored=authored_recordings(root);prior=effective_recordings(root)
    groups={}
    for name,record in authored.items():groups.setdefault((record['reference_sha256'],record['text']),[]).append(name)
    manifests={};aliases={};overrides={}
    for (_,text),names in groups.items():
        canonical=next((name for name in names if (root/authored[name]['source']).is_file()),None)
        if canonical is None:canonical=next((name for name in names if prior[name]['text']==text and not prior[name].get('shared_recording') and (root/prior[name]['source']).is_file()),None)
        if canonical is not None:
            if not (root/authored[canonical]['source']).is_file():overrides[canonical]={key:prior[canonical][key] for key in ('text','source','resource')}
        else:
            canonical=names[0];record=authored[canonical];who='father' if record['speaker']=='Other Father' else 'mother'
            if who not in manifests:manifests[who]={'reference':record['reference'],'reference_sha256':record['reference_sha256'],'output':'voicelines/other'+who+'/events/full_dialogue_v1','prefix':'line','max_seconds':120,'states':{}}
            profile=dict(record['profile']);profile.update(lines=[text],filename='line_01.wav',source_directory=canonical)
            if 'segments' in profile and ' '.join(segment['text'] for segment in profile['segments'])!=text:profile.pop('segments')
            manifests[who]['states'][canonical]=profile
            overrides[canonical]={'text':text,'source':manifests[who]['output']+'/'+canonical+'/line_01.wav','resource':'compact_events/full_wording/'+who+'/'+canonical}
        for name in names:
            if name!=canonical:aliases[name]=canonical
    plan={'format':2,'preserve_authored_text':True,'aliases':aliases,'new_recordings':overrides,
          'authored_event_count':len(authored),'distinct_authored_scripts':len(groups),
          'note':'Only exact same-speaker text is shared. No alternate-word substitutions.'}
    for who,manifest in manifests.items():
        path=root/('tools/full_'+who+'_voice_manifest.json')
        if path.exists():raise RuntimeError('Full manifest already exists: '+who)
        path.write_text(json.dumps(manifest,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    destination.write_text(json.dumps(plan,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    return {'authored_events':len(authored),'distinct_scripts':len(groups),'exact_duplicate_aliases':len(aliases),'missing':{who:len(manifest['states']) for who,manifest in manifests.items()}}

if __name__=='__main__':print(json.dumps(prepare(),indent=2))
