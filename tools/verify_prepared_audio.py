"""Validate already-imported audio before a code-only package resume.

This is NOT a resource optimizer replacement. The build must still run normal
processResources, check, jar and Forge reobfuscation. Refuse any changed audio.
"""
from pathlib import Path
import hashlib,json,zipfile,subprocess
from optimize_runtime_assets import sound_files,pcm_key

def digest(path):
    value=hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda:stream.read(1024*1024),b''):value.update(chunk)
    return value.hexdigest()

def packed_pcm_key(data):
    info=json.loads(subprocess.check_output(['ffprobe','-v','error','-i','pipe:0','-show_entries','stream=sample_rate,channels','-of','json'],input=data))['streams'][0]
    pcm=subprocess.check_output(['ffmpeg','-v','error','-i','pipe:0','-map','0:a:0','-c:a','pcm_s32le','-f','hash','-hash','sha256','-'],input=data).decode().strip()
    return f"{info['sample_rate']}:{info['channels']}:{pcm}"

def verify(root,jar,expected,*,preserve_captions=False,added_original_manifest=None,added_final_originals=False):
    if len(expected)!=64 or any(c not in '0123456789abcdef' for c in expected) or digest(jar)!=expected:raise RuntimeError('Previously audited audio JAR pin mismatch')
    source=root/'src/main/resources/assets/coraline'
    report=json.loads((jar.parent.parent/'reports/asset-size.json').read_text())
    if report['resampled_sounds']:raise RuntimeError('Prepared-audio reuse currently requires no resampled sources')
    aliases={item['file']:item['shared_with'] for item in report['duplicate_sounds']}
    originals=json.loads((source/'sounds.json').read_text());normalized=json.loads(json.dumps(originals))
    for definition in normalized.values():
        for i,entry in enumerate(definition.get('sounds',[])):
            if isinstance(entry,dict) and entry.get('type','file')!='file':continue
            name=entry if isinstance(entry,str) else entry['name']
            if ':' in name and not name.startswith('coraline:'):continue
            relative=name.removeprefix('coraline:')
            if relative in aliases:
                if isinstance(entry,str):definition['sounds'][i]='coraline:'+aliases[relative]
                else:entry['name']='coraline:'+aliases[relative]
    count=0;masters=0;pcm_aliases=0;container_only=0;new_files={};new_events={}
    with zipfile.ZipFile(jar) as archive:
        if archive.testzip() is not None:raise RuntimeError('Invalid audited input JAR')
        if preserve_captions:
            for name in ('story_subtitles.json','numbered_event_dialogue.json'):
                if (source/name).read_bytes()!=archive.read('assets/coraline/'+name):raise RuntimeError('Deferred existing caption data changed: '+name)
        previous=json.loads(archive.read('assets/coraline/sounds.json'))
        if added_original_manifest is not None:
            # Narrow exception for this already-run supplied-recording importer,
            # not an override permitting edits to any previously audited sound.
            if added_original_manifest!='rescue_chapters_audio.json':raise RuntimeError('Unrecognized added-original manifest')
            from import_rescue_chapters import sources
            receipts=json.loads((source/added_original_manifest).read_text(encoding='utf8'))
            if set(receipts)!=set(sources()):raise RuntimeError('Incomplete added-original receipt set')
            for event,relative in sources().items():
                receipt=receipts[event];master=(root/relative).resolve()
                if receipt.get('source')!=relative or not master.is_relative_to(root) or not master.is_file() or digest(master)!=receipt.get('source_sha256'):raise RuntimeError('Added-original master receipt mismatch: '+event)
                if event in previous:continue
                if event=='rescue_theatre_music':
                    if originals.get(event,{}).get('sounds')!=originals.get('theatre_music',{}).get('sounds'):raise RuntimeError('Theatre reuse changed playback settings')
                else:
                    path='rescue_chapters/'+event
                    if originals.get(event)!={'sounds':[{'name':'coraline:'+path,'stream':True}]}:raise RuntimeError('Unexpected new recording settings: '+event)
                    recording=source/'sounds'/f'{path}.ogg'
                    if not recording.is_file() or digest(recording)!=receipt.get('ogg_sha256'):raise RuntimeError('Added-original export receipt mismatch: '+event)
                    new_files[path]=receipt['ogg_sha256']
                new_events[event]=normalized[event]
        if added_final_originals:
            # The only second exception is the exact 24 supplied originals.
            # Validate full decoded-export receipts/timing before allowing new
            # event IDs; old events still have to match the pinned archive.
            from inspect_final_chapter_recordings import inspect_recordings
            from import_final_chapter_recordings import RECEIPT, REVISION, resource, expected_event, inspect_names
            from import_numbered_original_audio import ogg_measure
            supplied=inspect_recordings(root)
            receipts=json.loads((source/RECEIPT).read_text(encoding='utf8'))
            if receipts.get('format')!=1 or set(receipts.get('recordings',{}))!=inspect_names() or set(supplied)!=inspect_names():raise RuntimeError('Incomplete final-original receipt set')
            lengths=json.loads((source/'visit_audio_lengths.json').read_text(encoding='utf8'))
            for event,original in supplied.items():
                receipt=receipts['recordings'][event];path=resource(event)
                recording=source/'sounds'/f'{path}.ogg'
                if (receipt.get('source')!=original['source'] or receipt.get('source_sha256')!=original['sha256']
                        or receipt.get('export_revision')!=REVISION or receipt.get('resource')!=path
                        or receipt.get('decode_validated') is not True or not recording.is_file()
                        or digest(recording)!=receipt.get('ogg_sha256') or originals.get(event)!=expected_event(event)):
                    raise RuntimeError('Final-original identity/settings mismatch: '+event)
                measured=ogg_measure(recording.read_bytes())
                if (any(receipt.get(k)!=v for k,v in measured.items()) or lengths.get(event)!=measured['ticks']
                        or measured['channels']!=original['channels'] or measured['sample_rate']!=min(48000,original['sample_rate'])
                        or abs(measured['seconds']-original['seconds'])>1/measured['sample_rate']):
                    raise RuntimeError('Final-original full-duration/timing mismatch: '+event)
                masters+=1
                if event not in previous:
                    new_files[path]=receipt['ogg_sha256'];new_events[event]=normalized[event]
        if (source/'first_escape_music_audio.json').is_file():
            # Exact supplied song only. Existing recordings still match the pin.
            from import_first_escape_music import validate,EVENT,RESOURCE
            receipt=validate(root);masters+=1
            if EVENT not in previous:
                new_files[RESOURCE]=receipt['ogg_sha256'];new_events[EVENT]=normalized[EVENT]
        if {key:value for key,value in normalized.items() if key not in new_events}!=previous:raise RuntimeError('Sound definitions changed; rerun normal importers')
        for name in set(sound_files(originals,'coraline')):
            raw=(source/'sounds'/f'{name}.ogg').read_bytes()
            if name in new_files:
                if hashlib.sha256(raw).hexdigest()!=new_files[name]:raise RuntimeError('Added recording changed during validation: '+name)
                continue
            packed=archive.read('assets/coraline/sounds/'+aliases.get(name,name)+'.ogg')
            if raw!=packed:
                # Existing optimizer aliases are decoded-PCM-identical, not
                # necessarily byte-identical Ogg containers. Verify both the
                # packaged canonical bytes and the native PCM/rate/channels.
                canonical=source/'sounds'/f'{aliases.get(name,name)}.ogg'
                if name in aliases and canonical.read_bytes()==packed and pcm_key(source/'sounds'/f'{name}.ogg')==pcm_key(canonical):pcm_aliases+=1
                elif name not in aliases and pcm_key(canonical)==packed_pcm_key(packed):container_only+=1
                else:raise RuntimeError('Imported recording changed: '+name)
            count+=1
        def walk(value):
            nonlocal masters
            if isinstance(value,dict):
                if isinstance(value.get('source'),str) and isinstance(value.get('source_sha256'),str):
                    path=(root/value['source']).resolve()
                    if not path.is_relative_to(root) or digest(path)!=value['source_sha256']:raise RuntimeError('Original recording master changed: '+value['source'])
                    masters+=1
                for item in value.values():walk(item)
            elif isinstance(value,list):
                for item in value:walk(item)
        for manifest in source.glob('*audio.json'):walk(json.loads(manifest.read_text(encoding='utf-8')))
    if count<100 or masters<29:raise RuntimeError('Incomplete prepared audio validation')
    return {'audited_jar_sha256':expected,'byte_identical_referenced_recordings':count-pcm_aliases-container_only,'verified_existing_pcm_aliases':pcm_aliases,'verified_container_only_changes':container_only,'verified_source_master_receipts':masters,'new_original_events':len(new_events),'new_original_export_receipts':len(new_files),'existing_captions_preserved':preserve_captions,'normal_processResources_still_required':True}
