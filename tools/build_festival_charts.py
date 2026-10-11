"""Development-only ONNX transcription to note-block scores; no recording shipped."""
import os
from pathlib import Path
cache_dir=Path(__file__).resolve().parents[1]/".voice-tools/numba-music-cache"
cache_dir.mkdir(parents=True,exist_ok=True)
os.environ["NUMBA_CACHE_DIR"]=str(cache_dir)
import json, subprocess
from collections import defaultdict
from basic_pitch.inference import predict
import onnxruntime as ort
session_factory=ort.InferenceSession
def session(path,**kwargs):
    options=ort.SessionOptions();options.intra_op_num_threads=4;options.inter_op_num_threads=1
    return session_factory(path,sess_options=options,**kwargs)
ort.InferenceSession=session
from basic_pitch import ICASSP_2022_MODEL_PATH
import soundfile as sf
ROOT=Path(__file__).resolve().parents[1]
ASSETS=Path.home()/'.gradle/caches/forge_gradle/assets'
INDEX=json.loads((ASSETS/'indexes/5.json').read_text())['objects']
TRACKS=[('calm1','Minecraft'),('calm2','Clark'),('calm3','Sweden'),('hal1','Subwoofer Lullaby')]
CACHE=ROOT/'.voice-tools/festival-scores'
CACHE.mkdir(parents=True,exist_ok=True)
result=[]
for file,title in TRACKS:
    digest=INDEX['minecraft/sounds/music/game/'+file+'.ogg']['hash']
    source=ASSETS/'objects'/digest[:2]/digest
    wav=CACHE/(file+'.wav')
    subprocess.run(['C:/ffmpeg/ffmpeg.exe','-v','error','-y','-i',str(source),'-ac','1','-ar','22050',str(wav)],check=True)
    duration=min(3600,round(sf.info(wav).duration*20))
    saved=CACHE/(file+'.json')
    if saved.exists(): events=json.loads(saved.read_text())
    else:
        print("Transcribing "+title,flush=True)
        _,_,events=predict(wav,ICASSP_2022_MODEL_PATH,onset_threshold=.42,frame_threshold=.25,minimum_note_length=110,minimum_frequency=65,maximum_frequency=2100)
        events=[[float(a),float(b),int(c),float(d)] for a,b,c,d,_ in events]
        saved.write_text(json.dumps(events))
    groups=defaultdict(dict);ends=defaultdict(dict)
    for start,end,midi,amplitude in events:
        if amplitude<.32 or end-start<.15 or not 48<=midi<=84: continue
        tick=round(start*20)
        if tick<8 or tick>duration-12: continue
        tick=round(tick/2)*2
        groups[tick][midi]=max(groups[tick].get(midi,0),amplitude)
        ends[tick][midi]=round(end*20)
    notes=[];score=[];previous=-999;last_pitch=66;melody=[]
    for tick,voices in sorted(groups.items()):
        strongest=max(voices.values())
        pitches=[m for m in voices if voices[m]>=strongest*.80]
        if not pitches: continue
        # Follow a single confident melodic voice, instead of selecting faint
        # upper harmonics or layering unrelated detections behind player input.
        lead=max(pitches,key=lambda m:voices[m]-.018*max(0,abs(m-last_pitch)-5))
        melody.append((tick,lead,ends[tick][lead]))
        last_pitch=lead
    # Never invent attacks in held notes or rests. Bundle simultaneous source
    # voices into one playable prompt, retaining harmony without extra keys.
    for tick,lead,end in melody:
        progress=tick/duration
        gap=max(5,round(8-3*progress))
        prompt=-1
        if tick-previous>=gap and tick<duration-20:
            prompt=len(notes)
            voices=groups[tick]
            tones=sorted(voices,key=voices.get,reverse=True)[:3]
            if lead not in tones:tones=[lead]+tones[:2]
            notes.append([tick,(lead-48)%4,lead,sorted(tones)])
            previous=tick
        if prompt>=0:last_pitch=lead
    result.append(dict(title=title,resource='minecraft:music/game/'+file,duration=duration,notes=notes,score=score))
    print(title,duration/20,'seconds;',len(notes),'prompts;',len(score),'background attacks (disabled)',flush=True)
(ROOT/'src/main/resources/assets/coraline/festival_charts.json').write_text(json.dumps(result,separators=(',',':'))+'\n',encoding='utf-8')

