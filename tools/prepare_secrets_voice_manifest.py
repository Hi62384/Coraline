"""Extract the user's exact 60 scripts into the existing offline voice pipeline."""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
base = json.loads((ROOT/'tools/other_mother_welcome.json').read_text(encoding='utf-8'))
text = Path(sys.argv[1]).read_text(encoding='utf-8-sig')
events = {'ordinary':'ordinary_area','secret':'secret_area','item':'restricted_item',
          'offer':'blackmail_offer','accept':'blackmail_accepted','reject':'blackmail_rejected'}
states = {'low_low':'low_trust_low_suspicion','mid_mid':'mid_trust_mid_suspicion',
          'high_low':'high_trust_low_suspicion','low_high':'high_suspicion_low_trust',
          'high_high':'high_trust_high_suspicion'}
acting = {
 'low_low': ([.40,0,0,0,0,0,.05,.45], .76, 'Sweet, maternal, forgiving and persuasive; gentle theatrical conversation.'),
 'mid_mid': ([.12,.17,0,0,0,0,0,.64], .70, 'Familiar, observant, mildly irritated but controlled.'),
 'high_low': ([.32,0,.12,0,0,.08,0,.55], .73, 'Deeply affectionate, possessive, personally hurt; not angry.'),
 'low_high': ([.02,.38,0,0,.07,0,0,.55], .67, 'Cold and stern; forced sweetness, restrained anger, never shouting.'),
 'high_high': ([.14,.10,0,0,.04,0,.04,.82], .69, 'Intimate, knowing, quietly amused and sinister; elegant and calm.')}
matches = re.findall(r'\*\*(ordinary|secret|item|offer|accept|reject)_(low_low|mid_mid|high_low|low_high|high_high)_([12]):\*\*\s*"([^"]+)"', text)
assert len(matches) == 60 and len({m[:3] for m in matches}) == 60
manifest = {k:base[k] for k in ('reference','reference_sha256','emotion_order')}
manifest.update(output='voicelines/othermother/secrets', resource_folder='other_mother', prefix='line',
                generation_report='generation_report.json', validation_report='validation_report.json',
                transcript_review='transcript_review.json', states={})
for event,state,variation,line in matches:
    emotion,temp,direction = acting[state]
    group = f'{events[event]}/{state}_{variation}'
    manifest['states'][group] = dict(emotion=emotion,temperature=temp,direction=direction,
        source_directory=events[event],filename=f'{state}_{variation}.wav',
        sound_name=f'other_mother_secrets_{event}_{state}_{int(variation):02d}', lines=[line],
        dialogue_state=states[state], event=event, variation=int(variation),
        resource_path=f'secrets/{event}/{state}/line_{int(variation):02d}')
(ROOT/'tools/other_mother_secrets.json').write_text(json.dumps(manifest,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
print('Prepared all 60 exact scripts; reference and existing audio unchanged.')
