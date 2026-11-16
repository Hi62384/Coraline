"""Original synthetic, nonverbal childlike syllables, not a person's voice.

Writes lossless masters outside runtime resources. The later audio importer
must export these originals rather than repeatedly re-encoding runtime copies.
"""
from pathlib import Path
import array
import hashlib
import json
import math
import random
import sys
import wave

ROOT=Path(__file__).resolve().parents[1]


def samples(variant, rate=44100):
    randomizer=random.Random(872344+variant)
    vowels=[(430,1700,2500),(780,1250,2500)]
    output=[]
    for syllable in range(2):
        length=.145+.012*((variant+syllable)%3)
        phase=0.
        for index in range(round(length*rate)):
            t=index/rate;fraction=t/length
            f0=(285+variant*13)*(1+.06*math.sin(fraction*math.pi))
            phase+=2*math.pi*f0/rate
            a=vowels[0];b=vowels[1];blend=min(1,fraction*4)
            formants=[a[k]*(1-blend)+b[k]*blend for k in range(3)]
            voiced=0.
            for harmonic in range(1,28):
                frequency=f0*harmonic
                gain=.025+sum(math.exp(-.5*((frequency-formant)/band)**2)
                    for formant,band in zip(formants,(160,220,300)))
                voiced+=math.sin(phase*harmonic)*gain/(harmonic**.85)
            attack=min(1,t/.018);release=min(1,max(0,(length-t)/.033))
            envelope=math.sin(attack*math.pi/2)**2*math.sin(release*math.pi/2)**2
            consonant=randomizer.uniform(-1,1)*.06*math.exp(-t/.012)
            output.append((voiced+consonant)*envelope)
        output.extend([0.]*round(.018*rate))
    peak=max(abs(v) for v in output)
    return array.array('h',(round(v/peak*.72*32767) for v in output))


def main():
    destination=ROOT/'artwork/audio_masters/ghost_victim'
    if destination.exists():raise ValueError('Master directory already exists; never overwrite source recordings')
    destination.mkdir(parents=True)
    files=[]
    for variant in range(4):
        data=samples(variant)
        if sys.byteorder!='little':data.byteswap()
        path=destination/f'yada_{variant+1:02d}.wav'
        with wave.open(str(path),'wb') as out:
            out.setparams((1,2,44100,0,'NONE','not compressed'));out.writeframes(data.tobytes())
        with wave.open(str(path),'rb') as recording:
            assert recording.getframerate()==44100 and recording.getnchannels()==1
            duration=recording.getnframes()/recording.getframerate()
        files.append(dict(path=str(path.relative_to(ROOT)),sha256=hashlib.sha256(path.read_bytes()).hexdigest(),duration_seconds=duration,peak_fraction=.72))
    provenance=dict(kind='original procedural nonverbal syllables',real_person_reference=False,
                    generator_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
                    masters=files,runtime_exported=False,human_listening=False)
    (destination/'provenance.json').write_text(json.dumps(provenance,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(provenance,indent=2))


if __name__=='__main__':main()
