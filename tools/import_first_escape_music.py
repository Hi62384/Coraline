"""Import only the supplied complete escape song; never synthesize or touch voices."""
from pathlib import Path
import hashlib, json, subprocess, tempfile, wave
from audio_manifest_io import write_json_if_changed
from import_numbered_original_audio import ogg_measure

ROOT=Path(__file__).resolve().parents[1]
SOURCE='music/eventbased/FirstEscape.mp3'
EVENT='first_escape_music'
RESOURCE='scenes/first_escape_music'
RECEIPT='first_escape_music_audio.json'
ASSETS=Path('src/main/resources/assets/coraline')

def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def settings():return {'sounds':[{'name':'coraline:'+RESOURCE,'stream':True}]}
def original(root):
    path=root/SOURCE
    metadata=json.loads(subprocess.check_output(['ffprobe','-v','error','-select_streams','a:0','-show_entries','stream=sample_rate,channels:format=duration','-of','json',str(path)],text=True))
    stream=metadata['streams'][0]
    return {'source':SOURCE,'source_sha256':digest(path),'channels':int(stream['channels']),
            'sample_rate':int(stream['sample_rate']),'seconds':float(metadata['format']['duration'])}


def validate(root):
    source=root/ASSETS;master=original(root);receipt=json.loads((source/RECEIPT).read_text(encoding='utf8'))
    target=source/'sounds'/f'{RESOURCE}.ogg';measured=ogg_measure(target.read_bytes())
    if any(receipt.get(k)!=v for k,v in master.items()):raise RuntimeError('Escape music original receipt mismatch')
    if receipt.get('ogg_sha256')!=digest(target) or receipt.get('export')!=measured:raise RuntimeError('Escape music export receipt mismatch')
    if measured['channels']!=master['channels'] or measured['sample_rate']!=min(48000,master['sample_rate']) or abs(measured['seconds']-master['seconds'])>.15:
        raise RuntimeError('Escape song must preserve complete duration, channels and supported sample rate')
    if json.loads((source/'sounds.json').read_text())[EVENT]!=settings():raise RuntimeError('Unexpected escape music playback settings')
    if json.loads((source/'visit_audio_lengths.json').read_text())[EVENT]!=measured['ticks']:raise RuntimeError('Escape music timing mismatch')
    return receipt

def main():
    source=ROOT/ASSETS;master=original(ROOT);target=source/'sounds'/f'{RESOURCE}.ogg';target.parent.mkdir(parents=True,exist_ok=True)
    prior=json.loads((source/RECEIPT).read_text()) if (source/RECEIPT).exists() else {}
    if not(target.is_file() and all(prior.get(k)==v for k,v in master.items()) and prior.get('ogg_sha256')==digest(target)):
        with tempfile.TemporaryDirectory(prefix='coraline-escape-song-') as directory:
            staging=Path(directory)/'full.ogg'
            subprocess.run(['ffmpeg','-nostdin','-v','error','-y','-threads','1','-filter_threads','1','-i',str(ROOT/SOURCE),'-vn','-ar',str(min(48000,master['sample_rate'])),'-c:a','libvorbis','-q:a','6',str(staging)],check=True)
            subprocess.run(['ffmpeg','-nostdin','-v','error','-threads','1','-i',str(staging),'-f','null','-'],check=True)
            staging.replace(target)
    receipt={**master,'ogg_sha256':digest(target),'export':ogg_measure(target.read_bytes())}
    sounds=json.loads((source/'sounds.json').read_text());sounds[EVENT]=settings();write_json_if_changed(source/'sounds.json',sounds)
    for name in ('visit_audio_lengths.json','scene_audio_lengths.json'):
        table=source/name;lengths=json.loads(table.read_text()) if table.exists() else {};lengths[EVENT]=receipt['export']['ticks'];write_json_if_changed(table,lengths)
    write_json_if_changed(source/RECEIPT,receipt);validate(ROOT)
    print('Imported complete supplied FirstEscape.mp3; original unchanged; no voice/subtitle generation.')

if __name__=='__main__':main()
