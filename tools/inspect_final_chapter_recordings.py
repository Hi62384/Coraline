"""Inspect supplied RIFF headers and identities only; never transcribe or edit audio."""
from pathlib import Path
import hashlib,json,struct

ROOT=Path(__file__).resolve().parents[1]
def inspect_recordings(root=ROOT):
    masters={**{'theatre_opening_'+chr(c).lower():f'music/theatre/{chr(c)}.wav' for c in range(ord('A'),ord('N')+1)},
             'theatre_entrance':'music/theatre/Entrance.wav',
             **{'crawling_hand_'+chr(c).lower():f'voicelines/story/CrawlingHandBossFight/{chr(c)}.wav' for c in range(ord('A'),ord('I')+1)}}
    result={}
    for sound,relative in masters.items():
        path=root/relative
        if path.is_symlink() or not path.resolve().is_relative_to(root) or not path.is_file():raise RuntimeError('Missing/unsafe original recording: '+relative)
        size=path.stat().st_size;fmt=None;data_bytes=0
        with path.open('rb') as stream:
            header=stream.read(12)
            if len(header)!=12 or header[:4]!=b'RIFF' or header[8:]!=b'WAVE':raise RuntimeError('Unsupported original WAV container: '+relative)
            end=struct.unpack_from('<I',header,4)[0]+8
            if end>size:raise RuntimeError('Truncated original RIFF: '+relative)
            while stream.tell()+8<=end:
                kind,length=struct.unpack('<4sI',stream.read(8));start=stream.tell()
                if start+length>end:raise RuntimeError('Truncated original chunk: '+relative)
                if kind==b'fmt ':
                    if length<16 or length>4096:raise RuntimeError('Unexpected format header: '+relative)
                    fmt=struct.unpack_from('<HHIIHH',stream.read(length))
                elif kind==b'data':data_bytes+=length
                stream.seek(start+length+(length&1))
        if fmt is None:raise RuntimeError('Missing original WAV format: '+relative)
        codec,channels,rate,byte_rate,align,bits=fmt
        if codec not in (1,3,65534) or not 1<=channels<=8 or rate<=0 or align<=0 or byte_rate!=rate*align or data_bytes<=0 or data_bytes%align:raise RuntimeError('Invalid original PCM duration: '+relative)
        frames=data_bytes//align
        with path.open('rb') as stream:
            identity=hashlib.file_digest(stream,'sha256').hexdigest()
        result[sound]={'source':relative,'sha256':identity,'frames':frames,'sample_rate':rate,'channels':channels,'bits':bits,'seconds':frames/rate,'ticks':(frames*20+rate-1)//rate}
    for sound in ('theatre_opening_e','theatre_opening_f','crawling_hand_a'):
        if result[sound]['frames']<=2*result[sound]['sample_rate']:raise RuntimeError('Original recording conflicts with mandatory two-second beat: '+sound)
    return result
if __name__=='__main__':print(json.dumps(inspect_recordings(),indent=2))
