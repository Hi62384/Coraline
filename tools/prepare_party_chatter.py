"""Small original non-verbal, stylized conversation murmurs (not recorded dialogue).

Deterministic formant synthesis needs no speech model. Preserve WAV masters outside
runtime assets and encode each fresh master once; never cut/recompress other voices.
"""
from pathlib import Path
import math
import random
import struct
import subprocess
import wave

ROOT=Path(__file__).resolve().parents[1]
RATE=22050


def samples(seed):
    rng=random.Random(seed);out=[];phase=0.0
    for syllable in range(6):
        duration=rng.uniform(.18,.30);count=int(duration*RATE)
        base=rng.uniform(105,145) if seed%2==0 else rng.uniform(175,215)
        f1,f2=rng.choice([(350,1200),(500,1500),(650,1050),(400,1800)])
        weights=[math.exp(-((k*base-f1)/180)**2)+.55*math.exp(-((k*base-f2)/260)**2)+.1/k for k in range(1,18)]
        norm=sum(weights)
        for i in range(count):
            t=i/max(1,count-1);phase+=2*math.pi*base*(1+.06*math.sin(t*4))/RATE
            envelope=math.sin(math.pi*t)**.8
            value=sum(w*math.sin(k*phase) for k,w in enumerate(weights,1))/norm
            value=(value*.9+rng.uniform(-.035,.035))*envelope*.25
            out.append(int(max(-.8,min(.8,value))*32767))
        out.extend([0]*int(RATE*rng.uniform(.035,.09)))
    out.extend([0]*int(.15*RATE))
    return out


def main():
    master=ROOT/'.voice-tools/party-chatter-masters';runtime=ROOT/'src/main/resources/assets/coraline/sounds/party_chatter'
    master.mkdir(parents=True,exist_ok=True);runtime.mkdir(parents=True,exist_ok=True)
    for index in range(6):
        source=master/f'murmur_{index}.wav';target=runtime/f'murmur_{index}.ogg'
        if not source.exists():
            data=samples(4100+index)
            with wave.open(str(source),'wb') as stream:
                stream.setnchannels(1);stream.setsampwidth(2);stream.setframerate(RATE);stream.writeframes(struct.pack('<'+'h'*len(data),*data))
        if not target.exists():subprocess.run(['C:/ffmpeg/ffmpeg.exe','-v','error','-nostdin','-i',str(source),'-c:a','libvorbis','-q:a','3',str(target)],check=True)
    print('Six original stylized murmur variants:',sum(p.stat().st_size for p in runtime.glob('*.ogg')),'runtime bytes; WAV masters preserved.')


if __name__=='__main__':main()
