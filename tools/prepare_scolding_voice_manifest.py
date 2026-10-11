"""Extract the user's ten scripts and direct a two-part emotional arc with IndexTTS2."""
import json
import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
request = Path('C:/Users/User1/.codex/attachments/f36ad1b9-7af3-47cc-9da5-bbb16851c145/Pasted text.txt').read_text(encoding='utf-8')
base = json.loads((root/'tools/other_mother_secrets.json').read_text(encoding='utf-8'))
result = {key:base[key] for key in ('reference','reference_sha256','emotion_order')}
result.update(output='voicelines/othermother/scolding',resource_folder='other_mother',prefix='line',max_seconds=120,
              generation_report='generation_report.json',validation_report='validation_report.json',transcript_review='transcript_review.json',states={})
scripts = re.findall(r'\*\*(minor|serious)_(low_low|mid_mid|high_low|low_high|high_high)\.wav\*\*\s+"([^"]+)"',request)
assert len(scripts)==10
for category,state,text in scripts:
    original = base['states']['ordinary_area/'+state+'_1']
    emotion = list(original['emotion'])
    emotion[1] += .10 if category=='minor' else .18
    emotion[2] += .08
    soft = list(original['emotion'])
    soft[0] += .08
    marker = 'Still, I' if state=='low_high' and category=='minor' else 'Never mind.' if state=='high_high' and category=='minor' else 'But '
    at = text.index(marker)
    segments = [{'text':text[:at].strip(),'emotion':emotion},{'text':text[at:].strip(),'emotion':soft}]
    assert ' '.join(p['text'] for p in segments)==text
    result['states'][category+'/'+state] = dict(emotion=emotion,temperature=original['temperature'],
        direction=original['direction']+' Begin disappointed and controlling; soften deliberately into reluctant forgiveness.',
        source_directory=category,filename=state+'.wav',lines=[text],segments=segments,
        sound_name='other_mother_scolding_'+category+'_'+state+'_01',resource_path='scolding/'+category+'/'+state+'/line_01')
(root/'tools/other_mother_scolding.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
print('Prepared ten scolding scripts with two-part emotional direction.')
