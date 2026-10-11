"""Decode all WAV/Ogg files and optionally transcribe locally for synthesis errors."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import wave
import time

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = json.loads((ROOT/os.environ.get('CORALINE_VOICE_MANIFEST', 'tools/other_mother_welcome.json')).read_text(encoding='utf-8'))
SOURCE = ROOT / MANIFEST.get('output', 'voicelines/othermother/welcome')
PREFIX = MANIFEST.get('prefix', 'welcome')

def output_filename(profile, index, prefix):
    return profile.get('filename', f'{prefix}_{index:02d}.wav')


def words(text):
    return re.findall(r"[a-z]+", text.lower().replace('’', "'"))


def error_rate(expected, actual):
    left, right = words(expected), words(actual)
    previous = list(range(len(right)+1))
    for i, a in enumerate(left, 1):
        current = [i]
        for j, b in enumerate(right, 1):
            current.append(min(current[-1]+1, previous[j]+1, previous[j-1]+(a != b)))
        previous = current
    return previous[-1] / max(1, len(left))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--transcribe', action='store_true')
    parser.add_argument('--ogg', action='store_true')
    parser.add_argument('--speaker-check', action='store_true')
    parser.add_argument('--wait-for-files', action='store_true',
                        help='Validate a generation batch as each checksum-confirmed master becomes ready.')
    args = parser.parse_args()
    assert hashlib.sha256((ROOT/MANIFEST['reference']).read_bytes()).hexdigest() == MANIFEST['reference_sha256']
    model = None
    speaker = None
    if args.speaker_check:
        import torch
        import torchaudio
        import soundfile as sf
        torch.set_num_threads(4)
        sys.path.insert(0, str(ROOT/'.voice-tools/index-tts'))
        from indextts.s2mel.modules.campplus.DTDNN import CAMPPlus
        speaker = CAMPPlus(feat_dim=80, embedding_size=192).eval()
        speaker.load_state_dict(torch.load(ROOT/'.voice-tools/index-tts/checkpoints_2/hf_cache/campplus/campplus_cn_common.bin', map_location='cpu', weights_only=True))
        @torch.no_grad()
        def embedding(path):
            samples, rate = sf.read(path, dtype='float32')
            audio = torch.from_numpy(samples[:rate*15]).unsqueeze(0)
            audio = torchaudio.functional.resample(audio, rate, 16000)
            feat = torchaudio.compliance.kaldi.fbank(audio, num_mel_bins=80, dither=0, sample_frequency=16000)
            feat -= feat.mean(dim=0, keepdim=True)
            return speaker(feat.unsqueeze(0))
        reference_embedding = embedding(ROOT/MANIFEST['reference'])
    if args.transcribe:
        import torch
        import whisper
        torch.set_num_threads(6)
        model = whisper.load_model('base.en', device='cpu', download_root=str(ROOT/'.voice-tools/whisper'))
    report = {'reference_unchanged': True, 'checks': [],
              'note': 'ASR checks words, not acting. Human listening review is not replaced by these checks.'}
    for state, profile in MANIFEST['states'].items():
        for i, text in enumerate(profile['lines'], 1):
            prefix = profile.get('prefix', PREFIX)
            filename = output_filename(profile, i, prefix)
            relative = f'{state}/{filename}'.lstrip('/')
            path = SOURCE/profile.get('source_directory', state)/filename
            if args.wait_for_files:
                deadline = time.monotonic()+1800
                while True:
                    try:
                        generated = json.loads((SOURCE/MANIFEST.get('generation_report','generation_report.json')).read_text(encoding='utf-8'))
                        ready = path.exists() and hashlib.sha256(path.read_bytes()).hexdigest() == generated['lines'][relative]['sha256']
                    except (FileNotFoundError, KeyError, json.JSONDecodeError):
                        ready = False
                    if ready:
                        break
                    if time.monotonic()>=deadline:
                        raise RuntimeError('Timed out waiting for generated recording: '+relative)
                    time.sleep(2)
            with wave.open(str(path)) as audio:
                assert (audio.getnchannels(), audio.getsampwidth(), audio.getframerate()) == (1, 2, 22050)
                seconds = audio.getnframes()/audio.getframerate()
                assert 2 < seconds < MANIFEST.get('max_seconds', 40)
            subprocess.run(['ffmpeg','-v','error','-i',str(path),'-f','null','-'], check=True)
            item = {'file': relative, 'seconds': seconds, 'pcm_decode': 'pass'}
            if speaker is not None:
                similarity = torch.nn.functional.cosine_similarity(reference_embedding, embedding(path)).item()
                item['reference_speaker_cosine'] = similarity
                item['speaker_review'] = similarity < .35
                item['needs_review'] = item['speaker_review']
            if args.ogg:
                relative_asset = profile.get('resource_path', '/'.join(part for part in (state, Path(filename).stem) if part))
                ogg = ROOT/'src/main/resources/assets/coraline/sounds/visit'/MANIFEST.get('resource_folder', 'welcome')/(relative_asset+'.ogg')
                subprocess.run(['ffmpeg','-v','error','-i',str(ogg),'-f','null','-'], check=True)
                item['ogg_decode'] = 'pass'
            if model:
                result = model.transcribe(str(path), language='en', fp16=False, temperature=0,
                                          condition_on_previous_text=False)
                item['transcription'] = result['text'].strip()
                item['word_error_rate'] = error_rate(text, result['text'])
                item['needs_review'] = item['word_error_rate'] > .20 or item.get('speaker_review', False)
            report['checks'].append(item)
            print(json.dumps(item, ensure_ascii=True), flush=True)
    expected = sum(len(p['lines']) for p in MANIFEST['states'].values())
    for state, profile in MANIFEST['states'].items():
        folder = SOURCE/profile.get('source_directory', state)
        expected_files = {output_filename(other, i, other.get('prefix', PREFIX))
                          for group, other in MANIFEST['states'].items()
                          if SOURCE/other.get('source_directory', group) == folder
                          for i in range(1, len(other['lines'])+1)}
        assert {p.name for p in folder.glob('*.wav')} == expected_files, f'Unexpected WAV files in {folder}'
    (SOURCE/MANIFEST.get('validation_report', 'validation_report.json')).write_text(json.dumps(report, indent=2, ensure_ascii=False)+'\n', encoding='utf-8')
    flagged = [c['file'] for c in report['checks'] if c.get('needs_review')]
    if flagged:
        raise SystemExit('Review speech recognition mismatch: '+', '.join(flagged))
    print(f'All {expected} files passed objective checks; reference unchanged.')


if __name__ == '__main__':
    main()
