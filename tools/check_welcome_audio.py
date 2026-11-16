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
from contextlib import nullcontext
from low_memory_voice_loader import release_own_loading_pages

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
    parser.add_argument('--asr-turbo',action='store_true',help='Use the checksum-pinned local GPU ASR model, never concurrently with synthesis.')
    parser.add_argument('--memory-efficient-loading',action='store_true',help='Release only own clean import/checkpoint pages during pinned Turbo loading; no precision/quality changes.')
    parser.add_argument('--resume-checks',action='store_true',help='Resume source/script/reference/model-bound per-take progress; final coverage and quality gates remain unchanged.')
    parser.add_argument('--existing-only',action='store_true',help='Explicit partial audit of retained masters only; missing files are not claimed as complete.')
    parser.add_argument('--state', choices=list(MANIFEST['states']),help='Validate one exact resumable state; mark the report as partial.')
    parser.add_argument('--wait-for-files', action='store_true',
                        help='Validate a generation batch as each checksum-confirmed master becomes ready.')
    args = parser.parse_args()
    if args.resume_checks and not (args.asr_turbo and args.transcribe and args.speaker_check):raise RuntimeError('Resumable checks require full pinned ASR and speaker checks')
    if args.memory_efficient_loading and not args.asr_turbo:raise RuntimeError('Mapped quality loading requires the pinned Turbo path')
    assert hashlib.sha256((ROOT/MANIFEST['reference']).read_bytes()).hexdigest() == MANIFEST['reference_sha256']
    model = None
    speaker = None
    with release_own_loading_pages() if args.memory_efficient_loading else nullcontext():
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
            if args.asr_turbo:
                from local_turbo_asr import load
                model=load(ROOT,memory_efficient=args.memory_efficient_loading)
            else:model = whisper.load_model('base.en', device='cpu', download_root=str(ROOT/'.voice-tools/whisper'))
    report = {'reference_unchanged': True, 'checks': [],
              'note': 'ASR checks words, not acting. Human listening review is not replaced by these checks.'}
    checkpoint_path=SOURCE/'validation_checkpoint.json';checkpoint=None
    if args.resume_checks:
        import voice_validation_cache as progress
        def digest(path):
            with path.open('rb') as stream:return hashlib.file_digest(stream,'sha256').hexdigest()
        context={'format':1,'reference_sha256':MANIFEST['reference_sha256'],
            'manifest_sha256':hashlib.sha256(json.dumps(MANIFEST,sort_keys=True,ensure_ascii=False).encode('utf-8')).hexdigest(),
            'validator_sha256':digest(Path(__file__)),
            'asr_loader_sha256':digest(ROOT/'tools/local_turbo_asr.py'),
            'memory_adapter_sha256':digest(ROOT/'tools/low_memory_voice_loader.py'),
            'asr_checkpoint_sha256':digest(ROOT/'.voice-tools/whisper/large-v3-turbo.pt'),
            'speaker_checkpoint_sha256':digest(ROOT/'.voice-tools/index-tts/checkpoints_2/hf_cache/campplus/campplus_cn_common.bin')}
        checkpoint=progress.load(checkpoint_path,context)
        report.update(resumable_checks=True,quality_context=context,reused_checks=0)
    for state, profile in MANIFEST['states'].items():
        if args.state and state!=args.state:continue
        for i, text in enumerate(profile['lines'], 1):
            prefix = profile.get('prefix', PREFIX)
            filename = output_filename(profile, i, prefix)
            relative = f'{state}/{filename}'.lstrip('/')
            path = SOURCE/profile.get('source_directory', state)/filename
            if args.existing_only and not path.exists():continue
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
            source_sha=hashlib.sha256(path.read_bytes()).hexdigest()
            text_sha=hashlib.sha256(text.encode('utf-8')).hexdigest()
            if checkpoint is not None:
                prior=progress.reusable(checkpoint,relative,source_sha,text_sha)
                if prior is not None:
                    report['checks'].append(prior);report['reused_checks']+=1
                    print(json.dumps(dict(prior,reused_source_matched_check=True),ensure_ascii=True),flush=True)
                    continue
            item = {'file': relative, 'seconds': seconds, 'pcm_decode': 'pass','source_sha256':source_sha,
                    'expected_text_sha256':text_sha}
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
                result = model.transcribe(str(path), language='en', fp16=args.asr_turbo, temperature=0,
                                          condition_on_previous_text=False)
                item['transcription'] = result['text'].strip()
                item['word_error_rate'] = error_rate(text, result['text'])
                item['needs_review'] = item['word_error_rate'] > .20 or item.get('speaker_review', False)
            report['checks'].append(item)
            if hashlib.sha256(path.read_bytes()).hexdigest()!=source_sha:raise RuntimeError('Master changed during validation: '+relative)
            if checkpoint is not None:progress.save(checkpoint_path,checkpoint,item)
            print(json.dumps(item, ensure_ascii=True), flush=True)
    expected = sum(len(p['lines']) for state,p in MANIFEST['states'].items() if not args.state or state==args.state)
    report['partial']=bool(args.state or args.existing_only);report['selected_state']=args.state;report['existing_only']=args.existing_only
    for state, profile in MANIFEST['states'].items():
        if args.state and state!=args.state:continue
        folder = SOURCE/profile.get('source_directory', state)
        expected_files = {output_filename(other, i, other.get('prefix', PREFIX))
                          for group, other in MANIFEST['states'].items()
                          if SOURCE/other.get('source_directory', group) == folder
                          for i in range(1, len(other['lines'])+1)}
        actual_files={p.name for p in folder.glob('*.wav')}
        assert (actual_files.issubset(expected_files) if args.existing_only else actual_files==expected_files), f'Unexpected WAV files in {folder}'
    if hashlib.sha256((ROOT/MANIFEST['reference']).read_bytes()).hexdigest()!=MANIFEST['reference_sha256']:
        raise RuntimeError('Reference changed during validation')
    report_path=SOURCE/MANIFEST.get('validation_report','validation_report.json')
    staged_report=report_path.with_suffix('.tmp')
    staged_report.write_text(json.dumps(report, indent=2, ensure_ascii=False)+'\n', encoding='utf-8')
    staged_report.replace(report_path)
    flagged = [c['file'] for c in report['checks'] if c.get('needs_review')]
    if flagged:
        raise SystemExit('Review speech recognition mismatch: '+', '.join(flagged))
    print(f'All {len(report["checks"])} inspected files passed objective checks; reference unchanged. Partial audit: {report["partial"]}.')


if __name__ == '__main__':
    main()
