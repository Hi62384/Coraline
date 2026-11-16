"""Fetch one official Whisper checkpoint; no recordings leave the computer."""
from pathlib import Path
import hashlib, shutil, urllib.request
ROOT=Path(__file__).resolve().parents[1]
NAME='large-v3-turbo.pt'
SHA='aff26ae408abcba5fbf8813c21e62b0941638c5f6eebfb145be0c9839262a19a'
URL='https://openaipublic.azureedge.net/main/whisper/models/'+SHA+'/'+NAME
target=ROOT/'.voice-tools/whisper'/NAME
if target.exists():
    with target.open('rb') as stream:digest=hashlib.file_digest(stream,'sha256').hexdigest()
    if digest!=SHA:raise RuntimeError('Existing checkpoint does not match official digest; preserved for inspection')
    print('Official checkpoint already verified.');raise SystemExit(0)
if shutil.disk_usage(target.parent).free<4*1024**3:raise RuntimeError('Need 4 GiB of disk headroom')
partial=target.with_suffix('.download')
if partial.exists():raise RuntimeError('An earlier partial download exists; inspect it before retrying')
total=0;digest=hashlib.sha256();last=0
with urllib.request.urlopen(URL,timeout=30) as response,partial.open('xb') as output:
    while chunk:=response.read(1024*1024):
        total+=len(chunk)
        if total>2*1024**3:raise RuntimeError('Unexpected checkpoint size')
        digest.update(chunk);output.write(chunk)
        if total-last>=128*1024**2:print(f'Downloaded {total//1024**2} MiB',flush=True);last=total
if digest.hexdigest()!=SHA:raise RuntimeError('Official checkpoint checksum failed; partial preserved')
partial.replace(target)
print(f'Official checkpoint verified: {total} bytes. No audio uploaded.',flush=True)
