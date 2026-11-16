"""Import supplied scene cues and generate quiet, seamless tunnel ambience."""
from pathlib import Path
import json, subprocess, math, wave, struct, tempfile
from audio_manifest_io import write_json_if_changed
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/coraline'
out=assets/'sounds/scenes';out.mkdir(parents=True,exist_ok=True)
sounds=json.loads((assets/'sounds.json').read_text())
lengths=json.loads((assets/'scene_audio_lengths.json').read_text()) if (assets/'scene_audio_lengths.json').exists() else {}
files={'mice_intro':'BeforeSpawnMiceForFirstTimeMusic.wav','mice_chase':'MiceChase.wav','door_portal_open':'DoorPortalOpen.wav','tunnel_enter':'TunnelEnter.wav'}
def encode(source,target):
    # Avoid ffmpeg opening a destination that Gradle may currently have mapped.
    # Always derive the export from its original master, never an earlier Ogg.
    with tempfile.TemporaryDirectory(prefix='coraline-scene-import-') as folder:
        staging=Path(folder)/target.name
        subprocess.run(['ffmpeg','-nostdin','-v','error','-y','-threads','1','-filter_threads','1','-i',str(source),'-vn','-c:a','libvorbis','-q:a','5',str(staging)],check=True)
        import hashlib
        def digest(path):
            with path.open('rb') as stream:return hashlib.file_digest(stream,'sha256').digest()
        if not target.exists() or digest(staging)!=digest(target):
            staging.replace(target)
for name,source in files.items():
    target=out/(name+'.ogg')
    encode(root/'GameMusics'/source,target)
    seconds=float(subprocess.check_output(['ffprobe','-v','error','-show_entries','format=duration','-of','csv=p=0',str(target)],text=True))
    lengths[name]=math.ceil(seconds*20)
    sounds[name]={'sounds':[{'name':'coraline:scenes/'+name,'stream':True}]}
def magical_hum(t,channel):
    # All pitches and modulation periods close exactly over the 16-second loop.
    # A warm major/add9 choir pad, slow stereo chorus and distant glass harmonics.
    phase=channel*math.pi/2
    shimmer=math.sin(2*math.pi*t/8+phase)
    shifted=t+.0012*shimmer
    pad=sum(weight*math.sin(2*math.pi*frequency*shifted)
            for frequency,weight in [(196,.62),(246.9375,.32),(293.6875,.38),(440,.13),(587.375,.09)])
    breath=.86+.14*math.cos(2*math.pi*t/16)
    bells=(.5+.5*math.cos(2*math.pi*t/8+phase))**8
    glass=.065*bells*(math.sin(2*math.pi*880*t)+.45*math.sin(2*math.pi*1174.75*t))
    return .42*pad*breath+glass

for name,duration in [('tunnel_hum',16),('mouse_squeak',.32)]:
    with tempfile.TemporaryDirectory() as temp:
        wav=Path(temp)/'sound.wav'
        with wave.open(str(wav),'wb') as w:
            channels=2 if name=='tunnel_hum' else 1
            w.setparams((channels,2,24000,0,'NONE','not compressed'))
            # Keep identical PCM samples without retaining hundreds of thousands
            # of tiny Python objects alongside the build JVM.
            frames=bytearray()
            for i in range(int(duration*24000)):
                t=i/24000
                for channel in range(channels):
                    if name=='tunnel_hum':
                        value=magical_hum(t,channel)
                    else:
                        value=.19*math.sin(2*math.pi*(2200*t+800*t*t))*math.sin(math.pi*t/duration)**2
                    frames.extend(struct.pack('<h',int(max(-.95,min(.95,value))*32767)))
                if len(frames)>=65536:
                    w.writeframesraw(frames);frames.clear()
            w.writeframes(frames)
        encode(wav,out/(name+'.ogg'))
    sounds[name]={'sounds':[{'name':'coraline:scenes/'+name}]}
write_json_if_changed(assets/'sounds.json',sounds)
write_json_if_changed(assets/'scene_audio_lengths.json',lengths)
print('Scene audio lengths (ticks):',lengths)
