"""Offline IndexTTS2 welcome recordings. Run with .voice-tools/index-tts/.venv Python.

Only Sample.wav is used as reference audio. Emotion is controlled with explicit
vectors, not another speaker/clip. Native model pacing is retained (no time stretch).
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import random
import sys
import time
import warnings

ROOT = Path(__file__).resolve().parents[1]
ENGINE = ROOT / '.voice-tools/index-tts'
WORK = ROOT / '.voice-tools/welcome-work'
WORK.mkdir(parents=True, exist_ok=True)
os.environ.setdefault('NUMBA_CACHE_DIR', str(WORK / 'numba'))
os.environ.setdefault('HF_HUB_OFFLINE', '1')
os.environ.setdefault('TOKENIZERS_PARALLELISM', 'false')
sys.path.insert(0, str(ENGINE))

import numpy as np
import soundfile as sf
import torch

MANIFEST = json.loads((ROOT / os.environ.get('CORALINE_VOICE_MANIFEST', 'tools/other_mother_welcome.json')).read_text(encoding='utf-8'))
REFERENCE = ROOT / MANIFEST['reference']
OUTPUT = ROOT / MANIFEST.get('output', 'voicelines/othermother/welcome')
PREFIX = MANIFEST.get('prefix', 'welcome')

def output_filename(profile, index, prefix):
    return profile.get('filename', f'{prefix}_{index:02d}.wav')


def check_reference():
    actual = hashlib.sha256(REFERENCE.read_bytes()).hexdigest()
    if actual != MANIFEST['reference_sha256']:
        raise RuntimeError('Reference checksum changed; refusing to continue.')


def finish_audio(source, destination, text):
    """Trim excess edge silence with generous handles, then apply constant gain only."""
    samples, rate = sf.read(source, dtype='float32')
    if samples.ndim != 1 or rate != 22050 or not np.isfinite(samples).all():
        raise ValueError('Invalid/non-mono model output')
    duration = len(samples) / rate
    words = len(text.split())
    if not max(2, words / 5.5) <= duration <= words / 0.8 + 3:
        raise ValueError(f'Implausible duration: {duration:.2f}s for {words} words')
    peak = float(np.max(np.abs(samples)))
    if peak < .01:
        raise ValueError('Silent/near-silent synthesis')
    # More than a handful of full-scale samples indicates already clipped synthesis.
    clipped = int(np.count_nonzero(np.abs(samples) >= .999))
    if clipped > max(8, len(samples) * .0001):
        raise ValueError(f'Synthesis clipped before processing ({clipped} samples)')
    frame = int(.010 * rate)
    energy = np.array([np.sqrt(np.mean(samples[i:i+frame] ** 2))
                       for i in range(0, len(samples), frame)])
    active = np.flatnonzero(energy > max(.001, float(energy.max()) * .012))
    if not len(active):
        raise ValueError('No speech detected')
    begin = max(0, active[0] * frame - int(.16 * rate))
    end = min(len(samples), (active[-1]+1) * frame + int(.25 * rate))
    samples = samples[begin:end]
    speech = samples[np.abs(samples) > .003]
    rms = float(np.sqrt(np.mean(speech ** 2)))
    # Aim for -20 dB active RMS, cap gain to +6 dB and peak to -1.5 dBFS.
    gain = min(.1 / max(rms, 1e-8), 2.0, 10 ** (-1.5/20) / peak)
    samples = np.pad(samples * gain, (int(.08*rate), int(.12*rate)))
    destination.parent.mkdir(parents=True, exist_ok=True)
    sf.write(destination, samples, rate, subtype='PCM_16', format='WAV')
    info = sf.info(destination)
    decoded, _ = sf.read(destination)
    if info.subtype != 'PCM_16' or not len(decoded):
        raise ValueError('PCM WAV validation failed')
    return {'seconds': info.duration, 'sample_rate': rate, 'channels': info.channels,
            'format': info.subtype, 'gain_db': float(20*np.log10(gain)),
            'peak_dbfs': float(20*np.log10(np.max(np.abs(decoded)))),
            'trimmed_start_seconds': begin/rate, 'trimmed_end_seconds': duration-end/rate,
            'sha256': hashlib.sha256(destination.read_bytes()).hexdigest()}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--state', choices=list(MANIFEST['states']))
    parser.add_argument('--line', type=int, choices=range(1, max(len(p['lines']) for p in MANIFEST['states'].values()) + 1))
    parser.add_argument('--replace', action='store_true')
    parser.add_argument('--only', nargs='+', choices=[
        f'{group}/{output_filename(profile, i, profile.get("prefix", PREFIX))}'.lstrip('/')
        for group, profile in MANIFEST['states'].items() for i in range(1, len(profile['lines']) + 1)],
        help='Limit a repair run to these exact manifest recordings.')
    parser.add_argument('--seed-offset', type=int, default=0)
    parser.add_argument('--device', default='cuda:0')
    args = parser.parse_args()
    check_reference()
    from indextts.infer_v2 import IndexTTS2
    model = ENGINE / 'checkpoints_2'
    aux = model / 'hf_cache'
    print('Loading IndexTTS2 (explicit emotion vectors; no extra reference voice)...', flush=True)
    torch.set_num_threads(6)
    engine = IndexTTS2(cfg_path=str(model/'config.yaml'), model_dir=str(model),
                       use_fp16=args.device.startswith('cuda'), device=args.device,
                       use_cuda_kernel=False, use_deepspeed=False, use_qwen_emo=False,
                       aux_paths={'w2v_bert': str(aux/'w2v-bert-2.0'),
                                  'semantic_codec': str(aux/'maskgct/semantic_codec/model.safetensors'),
                                  'campplus': str(aux/'campplus/campplus_cn_common.bin'),
                                  'bigvgan': str(aux/'bigvgan')})
    if args.device.startswith('cuda') and torch.cuda.get_device_properties(0).total_memory < 8*2**30:
        # Reference encoding is done twice and cached. Keep the full-precision
        # encoder on CPU to leave GPU room for speech on 6 GB cards.
        # No quantization, alternate reference, or model-weight edits are used.
        engine.semantic_model.cpu()
        torch.cuda.empty_cache()
        @torch.no_grad()
        def reference_embeddings(input_features, attention_mask):
            encoded = engine.semantic_model(input_features=input_features.cpu(),
                attention_mask=attention_mask.cpu(), output_hidden_states=True)
            features = encoded.hidden_states[17].to(engine.device)
            return (features-engine.semantic_mean)/engine.semantic_std
        engine.get_emb = reference_embeddings
        print('6 GB GPU: CPU reference encoder; GPU speech generation.', flush=True)
    report_path = OUTPUT / MANIFEST.get('generation_report', 'generation_report.json')
    report = json.loads(report_path.read_text(encoding='utf-8')) if report_path.exists() else {
        'engine': 'IndexTTS2', 'engine_commit': 'd9e41aac89fd00b3d71497fddb287b7f24613712',
        'reference': MANIFEST['reference'], 'reference_sha256': MANIFEST['reference_sha256'],
        'reference_limit_seconds': 15, 'lines': {},
        'note': 'Acting uses model emotion vectors. Human listening review is still required.'}
    for state_index, (state, profile) in enumerate(MANIFEST['states'].items()):
        if args.state and state != args.state:
            continue
        emotion = engine.normalize_emo_vec(profile['emotion'])
        for i, text in enumerate(profile['lines'], 1):
            if args.line and i != args.line:
                continue
            prefix = profile.get('prefix', PREFIX)
            filename = output_filename(profile, i, prefix)
            relative = f'{state}/{filename}'.lstrip('/')
            if args.only and relative not in args.only:
                continue
            final = OUTPUT / profile.get('source_directory', state) / filename
            if final.exists() and not args.replace:
                print(f'Keeping existing {relative}', flush=True)
                continue
            raw = WORK / 'current.wav'
            for attempt in range(2):
                seed = 17400 + state_index*100 + i*10 + attempt + args.seed_offset
                random.seed(seed)
                np.random.seed(seed)
                torch.manual_seed(seed)
                torch.cuda.manual_seed_all(seed)
                print(f'Generating {relative}, attempt {attempt+1}', flush=True)
                began = time.monotonic()
                try:
                    with warnings.catch_warnings(record=True) as caught:
                        warnings.simplefilter('always', RuntimeWarning)
                        segments = profile.get('segments', [{'text':text,'emotion':profile['emotion']}])
                        if ' '.join(part['text'] for part in segments) != text:
                            raise ValueError('Emotional segments do not match the complete script')
                        pieces = []
                        for part_index, part in enumerate(segments):
                            engine.infer(spk_audio_prompt=str(REFERENCE), text=part['text'],
                                         output_path=str(raw), emo_vector=engine.normalize_emo_vec(part['emotion']),
                                         use_random=False, temperature=profile['temperature'],
                                         top_p=.8, top_k=30, num_beams=1,
                                         max_text_tokens_per_segment=120, max_mel_tokens=1500)
                            if len(segments)>1:
                                part_path=WORK/f'part_{part_index}.wav'
                                finish_audio(raw,part_path,part['text'])
                                samples,rate=sf.read(part_path,dtype='float32')
                                pieces.append(samples)
                        if pieces:
                            sf.write(raw,np.concatenate(pieces),22050,subtype='PCM_16')
                    failures = [str(w.message) for w in caught if 'max_mel_tokens' in str(w.message)]
                    if failures:
                        raise ValueError('; '.join(failures))
                    stats = finish_audio(raw, final, text)
                    stats.update(text=text, direction=profile['direction'], emotion=emotion,
                                 seed=seed, attempt=attempt+1, inference_seconds=time.monotonic()-began)
                    if 'segments' in profile: stats['segments']=profile['segments']
                    report['lines'][relative] = stats
                    report_path.write_text(json.dumps(report, indent=2, ensure_ascii=False)+'\n', encoding='utf-8')
                    print(f'Validated {relative}: {stats["seconds"]:.2f}s', flush=True)
                    break
                except ValueError as error:
                    print(f'Retrying invalid audio: {error}', flush=True)
                    if attempt == 1:
                        raise
    check_reference()


if __name__ == '__main__':
    try:
        main()
    finally:
        check_reference()
