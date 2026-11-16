"""Check imported original identities and real Vorbis timing; no playback/transcription."""
import json,math
from pathlib import Path
from import_final_chapter_recordings import ROOT, ASSET_REL, RECEIPT, REVISION, digest, expected_event, resource
from inspect_final_chapter_recordings import inspect_recordings
from import_numbered_original_audio import ogg_measure, contained


def verify():
    assets=contained(ROOT,ASSET_REL)
    originals=inspect_recordings()
    receipts=json.loads((assets/RECEIPT).read_text(encoding='utf8'))
    if receipts.get('format')!=1 or set(receipts.get('recordings',{}))!=set(originals):
        raise RuntimeError('Missing/incompatible supplied-original receipt set')
    sounds=json.loads((assets/'sounds.json').read_text(encoding='utf8'))
    lengths=json.loads((assets/'visit_audio_lengths.json').read_text(encoding='utf8'))
    envelopes=json.loads((assets/'visit_voice_envelopes.json').read_text(encoding='utf8'))
    total=0
    for sound,original in originals.items():
        receipt=receipts['recordings'][sound]
        target=contained(ROOT,ASSET_REL/'sounds'/(resource(sound)+'.ogg'))
        if (receipt.get('source')!=original['source'] or receipt.get('source_sha256')!=original['sha256']
                or receipt.get('export_revision')!=REVISION or receipt.get('resource')!=resource(sound)
                or not receipt.get('decode_validated') or digest(target)!=receipt.get('ogg_sha256')
                or sounds.get(sound)!=expected_event(sound)):
            raise RuntimeError('Supplied-original identity/settings mismatch: '+sound)
        measured=ogg_measure(target.read_bytes())
        if any(receipt.get(k)!=v for k,v in measured.items()) or lengths.get(sound)!=measured['ticks']:
            raise RuntimeError('Incomplete actual Vorbis timing metadata: '+sound)
        if (measured['channels']!=original['channels'] or measured['sample_rate']!=min(48000,original['sample_rate'])
                or abs(measured['seconds']-original['seconds'])>1/measured['sample_rate']):
            raise RuntimeError('Truncated/changed original export: '+sound)
        if sound.startswith('crawling_hand_'):
            values=envelopes.get(sound)
            if (not isinstance(values,list) or abs(len(values)-measured['ticks'])>1 or not values
                    or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not math.isfinite(v) or not 0<=v<=1 for v in values)
                    or max(values)<=0):
                raise RuntimeError('Missing/invalid full-length well mouth waveform: '+sound)
        total+=target.stat().st_size
    return {'recordings':len(originals),'source_ogg_bytes':total,'masters_preserved':True,
            'scope':'Identity, full Vorbis granules/CRC and timing only; NOT speaker content, audibility or native playback'}


if __name__=='__main__':
    print(json.dumps(verify(),indent=2))
