"""Independent beam-search decoding of non-exact ASR results; never supplies expected text as a prompt."""
import argparse
import json
import os
from pathlib import Path
import torch
import whisper
from check_welcome_audio import error_rate

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--excerpt-file', help='Additionally decode this recording as two context-independent excerpts.')
parser.add_argument('--split-at', type=float, default=4.0)
args = parser.parse_args()
manifest = json.loads((ROOT/os.environ['CORALINE_VOICE_MANIFEST']).read_text(encoding='utf-8'))
source = ROOT/manifest['output']
report = json.loads((source/manifest['validation_report']).read_text(encoding='utf-8'))
expected = {}
for group, profile in manifest['states'].items():
    for i, text in enumerate(profile['lines'], 1):
        name = profile.get("filename", f'{profile.get("prefix", manifest["prefix"])}_{i:02d}.wav')
        expected[f'{group}/{name}'.lstrip('/')] = (text, source/profile.get('source_directory', group)/name)
torch.set_num_threads(6)
model = whisper.load_model('base.en', device='cpu', download_root=str(ROOT/'.voice-tools/whisper'))
reviews = []
for item in report['checks']:
    if not item.get('word_error_rate', 0):
        continue
    text, path = expected[item['file']]
    decoded = model.transcribe(str(path), language='en', fp16=False, temperature=0,
                              condition_on_previous_text=False, beam_size=5)
    review = {'file': item['file'], 'expected': text, 'first_transcription': item['transcription'],
              'beam_transcription': decoded['text'].strip(), 'beam_word_error_rate': error_rate(text, decoded['text'])}
    if item['file'] == args.excerpt_file:
        audio = whisper.load_audio(str(path))
        split = int(args.split_at * 16000)
        assert 0 < split < len(audio), 'Excerpt split is outside the recording'
        review['excerpt_split_seconds'] = args.split_at
        review['excerpt_transcriptions'] = [model.transcribe(part, language='en', fp16=False,
            temperature=0, beam_size=5, condition_on_previous_text=False)['text'].strip()
            for part in (audio[:split], audio[split:])]
    reviews.append(review)
    print(json.dumps(review, ensure_ascii=True), flush=True)
(source/manifest.get('transcript_review', 'activity_transcript_review.json')).write_text(json.dumps(reviews, indent=2, ensure_ascii=False)+'\n', encoding='utf-8')
