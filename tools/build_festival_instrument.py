"""Tiny three-register harp bank derived once from installed vanilla sample."""
import json,subprocess
from pathlib import Path
root=Path(__file__).resolve().parents[1]
assets=Path.home()/'.gradle/caches/forge_gradle/assets'
objects=json.loads((assets/'indexes/5.json').read_text())['objects']
digest=objects['minecraft/sounds/note/harp.ogg']['hash']
source=assets/'objects'/digest[:2]/digest
dest=root/'src/main/resources/assets/coraline/sounds/festival'
dest.mkdir(parents=True,exist_ok=True)
for name,shift in [('harp_low',.25),('harp',1),('harp_high',4)]:
    subprocess.run(['C:/ffmpeg/ffmpeg.exe','-v','error','-y','-i',str(source),'-af',f'asetrate=44100*{shift},aresample=44100','-c:a','libvorbis','-q:a','5',str(dest/(name+'.ogg'))],check=True)
path=root/'src/main/resources/assets/coraline/sounds.json'
data=json.loads(path.read_text())
for name in ['harp_low','harp','harp_high']:
    data['festival_'+name]={'sounds':[{'name':'coraline:festival/'+name,'stream':False}]}
path.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
