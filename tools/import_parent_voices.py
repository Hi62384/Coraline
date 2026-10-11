"""Run before a build to import user-supplied parent dialogue. Never alters source audio."""
from pathlib import Path
import json, re, shutil, subprocess, math
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/coraline'
path=assets/'sounds.json'
sounds=json.loads(path.read_text())
groups={name:[] for name in ('angry_parent','mom_neglect','mom_task','mom_death','dad_neglect','dad_neglect_rain','dad_task','dad_death')}
durations={}
rotations={name:[] for name in groups}
ffmpeg=shutil.which('ffmpeg') or (r'C:\ffmpeg\ffmpeg.exe' if Path(r'C:\ffmpeg\ffmpeg.exe').exists() else None)
for file in sorted((root/'voicelines').rglob('*')):
    if file.suffix.lower() not in ('.wav','.mp3','.ogg','.flac','.m4a'): continue
    stem=re.sub('[^a-z0-9]','',file.stem.lower())
    folder=file.parent.name.lower()
    if stem.startswith('angryparent'): targets=['angry_parent']
    elif stem.startswith('momdeath'):targets=['mom_death']
    elif stem.startswith('daddeath'):targets=['dad_death']
    elif stem.startswith('momtask'):targets=['mom_task']
    elif stem.startswith('dadtask'):targets=['dad_task']
    elif stem.startswith('dadneglectduringrain'):targets=['dad_neglect_rain']
    elif stem.startswith('momneglect'):targets=['mom_neglect']
    elif stem.startswith('dadneglect'):targets=['dad_neglect','dad_neglect_rain']
    else:continue
    if not ffmpeg:raise SystemExit('FFmpeg is required to import parent audio. Put ffmpeg on PATH.')
    name=re.sub('[^a-z0-9_]+','_',str(file.relative_to(root/'voicelines').with_suffix('')).lower())
    out=assets/'sounds/parents'/f'{name}.ogg';out.parent.mkdir(parents=True,exist_ok=True)
    if not out.exists() or max(file.stat().st_mtime,Path(__file__).stat().st_mtime)>out.stat().st_mtime:
        subprocess.run([ffmpeg,'-hide_banner','-loglevel','error','-y','-i',str(file),'-vn','-af','loudnorm=I=-14:TP=-1:LRA=7,alimiter=limit=0.95:level=false','-ac','1','-ar','48000','-c:a','libvorbis','-q:a','5',str(out)],check=True)
    probe=shutil.which('ffprobe') or str(Path(ffmpeg).with_name('ffprobe.exe'))
    seconds=float(subprocess.check_output([probe,'-v','error','-show_entries','format=duration','-of','default=noprint_wrappers=1:nokey=1',str(out)],text=True).strip())
    clip='parent_voice_'+name.replace('/','_')
    sounds[clip]={'sounds':[{'name':f'coraline:parents/{name}','stream':True}]}
    durations[clip]=math.ceil(seconds*20)
    for group in targets:
        rotations[group].append(clip)
        groups[group].append({'name':f'coraline:parents/{name}','stream':True})
        durations[group]=max(durations.get(group,0),math.ceil(seconds*20))
groups['mom_chat']=groups['mom_neglect']
groups['dad_chat']=groups['dad_neglect']
for name,entries in groups.items():sounds[name]={'sounds':entries}
path.write_text(json.dumps(sounds,indent=2)+'\n')
(assets/'parent_voice_lengths.json').write_text(json.dumps(durations,indent=2)+'\n')
(assets/'parent_voice_groups.json').write_text(json.dumps(rotations,indent=2)+'\n')
print('Parent voice assets: '+', '.join(f'{k}={len(v)}' for k,v in groups.items()))
