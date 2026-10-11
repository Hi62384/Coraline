"""Download pinned, official IndexTTS2 weights; never upload the reference audio."""
from pathlib import Path
from huggingface_hub import snapshot_download

ROOT = Path(__file__).resolve().parents[1]
MODEL = ROOT / '.voice-tools/index-tts/checkpoints_2'
REPOS = [
    ('IndexTeam/IndexTTS-2', MODEL,
     ['*.pth', '*.pt', 'bpe.model', 'config.yaml', 'LICENSE*', 'README.md'],
     '740dcaff396282ffb241903d150ac011cd4b1ede'),
    ('facebook/w2v-bert-2.0', MODEL / 'hf_cache/w2v-bert-2.0',
     ['config.json', 'preprocessor_config.json', 'model.safetensors', 'README.md'], None),
    ('amphion/MaskGCT', MODEL / 'hf_cache/maskgct',
     ['semantic_codec/model.safetensors', 'LICENSE*', 'README.md'], None),
    ('funasr/campplus', MODEL / 'hf_cache/campplus',
     ['campplus_cn_common.bin', 'README.md'], None),
    ('nvidia/bigvgan_v2_22khz_80band_256x', MODEL / 'hf_cache/bigvgan',
     ['config.json', 'bigvgan_generator.pt', 'LICENSE*', 'README.md'], None),
]

if __name__ == '__main__':
    for repo, target, patterns, revision in REPOS:
        print(f'Downloading {repo} into {target}', flush=True)
        snapshot_download(repo, local_dir=target, allow_patterns=patterns,
                          revision=revision, max_workers=2)
    print('Voice model downloads complete.', flush=True)
