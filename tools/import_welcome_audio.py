"""Import checked PCM masters as streamed Ogg/Vorbis for Minecraft 1.20.1."""
import hashlib
import json
import math
import os
from pathlib import Path
import shutil
import subprocess
import wave

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/coraline'
MANIFEST = json.loads((ROOT/os.environ.get('CORALINE_VOICE_MANIFEST', 'tools/other_mother_welcome.json')).read_text(encoding='utf-8'))
SOURCE = ROOT / MANIFEST.get('output', 'voicelines/othermother/welcome')
FOLDER = MANIFEST.get('resource_folder', 'welcome')
PREFIX = MANIFEST.get('prefix', 'welcome')
OUT = ASSETS / 'sounds/visit' / FOLDER

def output_filename(profile, index, prefix):
    return profile.get('filename', f'{prefix}_{index:02d}.wav')


def main():
    sample = ROOT / MANIFEST['reference']
    assert hashlib.sha256(sample.read_bytes()).hexdigest() == MANIFEST['reference_sha256'], 'Reference changed'
    sounds = json.loads((ASSETS/'sounds.json').read_text())
    lengths = json.loads((ASSETS/'visit_audio_lengths.json').read_text())
    report = json.loads((SOURCE/MANIFEST.get('generation_report', 'generation_report.json')).read_text(encoding='utf-8'))
    for state, profile in MANIFEST['states'].items():
        for index in range(1, len(profile['lines']) + 1):
            prefix = profile.get('prefix', PREFIX)
            filename = output_filename(profile, index, prefix)
            relative = f'{state}/{filename}'.lstrip('/')
            source = SOURCE / profile.get('source_directory', state) / filename
            assert source.is_file(), f'Missing master: {source}'
            assert hashlib.sha256(source.read_bytes()).hexdigest() == report['lines'][relative]['sha256'], f'Unvalidated edit: {source}'
            with wave.open(str(source)) as wav:
                assert (wav.getnchannels(), wav.getsampwidth(), wav.getframerate()) == (1, 2, 22050)
                assert wav.getnframes() > 22050
            relative_asset = profile.get('resource_path', '/'.join(part for part in (state, Path(filename).stem) if part))
            target = OUT / (relative_asset+'.ogg')
            target.parent.mkdir(parents=True, exist_ok=True)
            if not target.exists() or target.stat().st_mtime < source.stat().st_mtime:
                subprocess.run(['ffmpeg','-v','error','-y','-i',str(source),'-vn',
                                '-c:a','libvorbis','-q:a','5',str(target)], check=True)
            subprocess.run(['ffmpeg','-v','error','-i',str(target),'-f','null','-'], check=True)
            seconds = float(subprocess.check_output(['ffprobe','-v','error','-show_entries',
                'format=duration','-of','csv=p=0',str(target)], text=True))
            name = (f'other_mother_welcome_{state}_{index:02d}' if PREFIX == 'welcome'
                    else f'other_mother_{state.replace("/", "_")}_{index:02d}')
            if 'sound_name' in profile:
                name = profile['sound_name']
            if 'sound_prefix' in MANIFEST:
                name = f'{MANIFEST["sound_prefix"]}_{index:02d}'
            asset_path = '/'.join(part for part in (FOLDER, relative_asset) if part)
            sounds[name] = {'sounds': [{'name': f'coraline:visit/{asset_path}', 'stream': True}]}
            lengths[name] = math.ceil(seconds * 20)
    (ASSETS/'sounds.json').write_text(json.dumps(sounds, indent=2)+'\n')
    (ASSETS/'visit_audio_lengths.json').write_text(json.dumps(lengths, indent=2)+'\n')
    # Distribute the model's required notice with its generated outputs, not model weights.
    bundled_license = ROOT/'src/main/resources/META-INF/INDEXTTS_AUDIO_LICENSE.txt'
    if not bundled_license.exists():
        shutil.copyfile(ROOT/'.voice-tools/index-tts/checkpoints_2/LICENSE.txt', bundled_license)
    shutil.copyfile(bundled_license, SOURCE/'INDEXTTS_AUDIO_LICENSE.txt')
    notice = ROOT/MANIFEST.get('notice', 'src/main/resources/META-INF/WELCOME_AUDIO_NOTICE.txt')
    shutil.copyfile(notice, SOURCE/'NOTICE.txt')
    print(f'Imported and decoded {sum(len(p["lines"]) for p in MANIFEST["states"].values())} scripted voice clips.')


if __name__ == '__main__':
    main()
