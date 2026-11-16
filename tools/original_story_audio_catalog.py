"""Complete supplied story masters, shared by transcription and caption export."""
from pathlib import Path
import json

def originals(root):
    root=Path(root);assets=root/'src/main/resources/assets/coraline'
    result=json.loads((root/'tools/numbered_original_recordings.json').read_text())['recordings']
    legacy_path=root/'tools/legacy_original_recordings.json'
    if legacy_path.exists():
        legacy=json.loads(legacy_path.read_text(encoding='utf-8'))
        if result.keys() & legacy.keys():raise RuntimeError('Supplied legacy/chapter audio IDs overlap')
        result.update(legacy)
    for filename in ('missing_parents_tea_audio.json','first_escape_audio.json','rescue_chapters_audio.json','final_chapter_original_audio.json'):
        path=assets/filename
        if not path.exists():continue
        data=json.loads(path.read_text(encoding='utf-8'))
        for name,entry in data.get('recordings',data).items():
            if not isinstance(entry,dict) or 'source' not in entry:raise RuntimeError('Invalid original audio receipt: '+filename)
            result[name]={**entry,'music':name=='rescue_theatre_music' or name=='theatre_entrance',
                'speaker':'Spink, Forcible & Coraline' if 'tea_' in name or 'theatre_' in name else 'Coraline & the Cat' if 'cat_' in name else 'Story dialogue'}
    for filename,prefix,directory in (('black_is_traditional_audio.json','black_is_traditional_','voicelines/story/BlackIsTraditional'),('cat_encounter_audio.json','cat_encounter_','voicelines/cat')):
        path=assets/filename
        if not path.exists():continue
        for letter,entry in json.loads(path.read_text()).items():
            result[prefix+letter.lower()]={**entry,'source':directory+'/'+letter+'.wav','music':False,
                'speaker':'Coraline & the Cat' if prefix.startswith('cat') else 'Coraline & Other Parents'}
    derived=assets/'accepted_button_eyes_audio.json'
    if derived.exists():
        from import_accepted_ghost_audio import verify
        verify(root)
        records=json.loads(derived.read_text(encoding='utf-8'))
        if set(records)!={'mirror_prison_e_accepted'} or result.keys() & records.keys():raise RuntimeError('Unexpected accepted-route derivative catalogue')
        for name,entry in records.items():result[name]={**entry,'music':False,'speaker':'Coraline & the Ghost Children','derived':True}
    for entry in result.values():entry['source_bytes']=(root/entry['source']).stat().st_size
    return result
