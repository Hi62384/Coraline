"""Separate placeholder-build integrity gate; never certifies genuine voice quality."""
from pathlib import Path
import argparse, hashlib, json, math, subprocess, tempfile, zipfile
from prepare_temporary_event_audio import ROOT, ASSETS, FFMPEG, FFMPEG_PIN, sha

def forbidden_resource(name):
    lowered=name.lower()
    return (lowered.endswith(('.wav','.mp3','.blend','.glb','.safetensors','.pth','.pt','.java'))
            or 'gametests' in lowered or 'visualcheck' in lowered or lowered.endswith('coraline.qa.mixins.json')
            # The runtime garden reader has a legitimate nested Blueprint class.
            # Reject authoring blueprint files, not that required compiled code.
            or ('blueprint' in lowered and not lowered.endswith('.class'))
            or lowered.endswith('/empty.nbt'))

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--jar');args=parser.parse_args()
    if sha(FFMPEG)!=FFMPEG_PIN:raise RuntimeError('Approved FFmpeg identity changed')
    jar=zipfile.ZipFile(args.jar) if args.jar else None
    prefix='assets/coraline/'
    read=lambda name:jar.read(prefix+name) if jar else (ASSETS/name).read_bytes()
    report=json.loads(read('temporary_event_audio.json'));sounds=json.loads(read('sounds.json'))
    lengths=json.loads(read('visit_audio_lengths.json'));captions=json.loads(read('story_subtitles.json'))
    if report['build_kind']!='placeholder-audio':raise RuntimeError('Wrong audio build kind')
    entries=report['temporary_events'];by_id={e['event']:e for e in entries}
    if len(by_id)!=len(entries):raise RuntimeError('Duplicate temporary event IDs')
    active={name for name,event in sounds.items() if any(isinstance(e,dict) and str(e.get('name','')).startswith('coraline:temporary_event_audio/') for e in event.get('sounds',[]))}
    if active!=set(by_id):raise RuntimeError('Unlisted or missing temporary events')
    if jar:
        manifest=jar.read('META-INF/MANIFEST.MF').decode('utf-8')
        if 'Audio-Build-Kind: placeholder-audio' not in manifest:raise RuntimeError('JAR is not labelled placeholder-audio')
        jar.read('META-INF/TEMPORARY_EVENT_AUDIO_NOTICE.txt')
        forbidden=[name for name in jar.namelist() if forbidden_resource(name)]
        if forbidden:raise RuntimeError('Authoring/test resources packaged: '+str(forbidden[:20]))
    decoded={}
    with tempfile.TemporaryDirectory(prefix='coraline-temporary-audio-') as folder:
        def pcm(resource):
            if resource not in decoded:
                raw=read('sounds/'+resource+'.ogg')
                if not raw.startswith(b'OggS'):raise RuntimeError('Not genuine Ogg container: '+resource)
                path=Path(folder)/'input.ogg';path.write_bytes(raw)
                result=subprocess.run([str(FFMPEG),'-v','error','-nostdin','-i',str(path),'-f','s16le','-ac','1','-ar','24000','-'],stdout=subprocess.PIPE,stderr=subprocess.PIPE,check=True,timeout=120)
                decoded[resource]=(len(result.stdout)/48000,hashlib.sha256(raw).hexdigest(),hashlib.sha256(result.stdout).hexdigest())
            return decoded[resource]
        for name,entry in by_id.items():
            if (ROOT/entry['missing_master']).exists():raise RuntimeError('A real master now exists; replace its temporary mapping: '+name)
            actual=sounds[name]['sounds'][0]['name'].removeprefix('coraline:')
            if actual!=entry['temporary_resource']:raise RuntimeError('Unexpected temporary mapping: '+name)
            seconds,digest,_=pcm(actual)
            if digest!=entry['ogg_sha256'] or abs(seconds-entry['seconds'])>.03:raise RuntimeError('Temporary media identity/timing mismatch: '+name)
            if lengths[name]!=math.ceil(seconds*20):raise RuntimeError('Event duration disagrees with decoded recording: '+name)
            caption=captions[name]
            if 'temporary audio' not in caption['speaker']:raise RuntimeError('Temporary caption is not labelled: '+name)
            if ' '.join(c['text'] for c in caption['cues'])!=entry['text']:raise RuntimeError('Authored dialogue changed: '+name)
            if not caption['cues'] or abs(caption['cues'][-1]['end']-lengths[name]/20)>.01:raise RuntimeError('Caption/gate duration mismatch: '+name)
        for entry in report['genuine_events']:
            if sha(ROOT/entry['source'])!=entry['source_sha256']:raise RuntimeError('Genuine master changed: '+entry['event'])
            name=entry['event'];actual=sounds[name]['sounds'][0]['name'].removeprefix('coraline:')
            seconds,digest,decoded_hash=pcm(actual)
            expected=ASSETS/'sounds'/(entry['resource']+'.ogg')
            if sha(expected)!=entry['ogg_sha256']:raise RuntimeError('Preserved genuine runtime source changed: '+name)
            if digest!=entry['ogg_sha256']:
                # Optimizer may alias byte-distinct Oggs only when their decoded PCM is identical.
                actual_file=Path(folder)/'actual-genuine.ogg';actual_file.write_bytes(read('sounds/'+actual+'.ogg'))
                decode_args=['-f','f32le','-']  # Preserve native channels and sample rate for identity.
                original=subprocess.run([str(FFMPEG),'-v','error','-nostdin','-i',str(expected),*decode_args],stdout=subprocess.PIPE,stderr=subprocess.PIPE,check=True,timeout=120)
                packaged=subprocess.run([str(FFMPEG),'-v','error','-nostdin','-i',str(actual_file),*decode_args],stdout=subprocess.PIPE,stderr=subprocess.PIPE,check=True,timeout=120)
                if original.stdout!=packaged.stdout:raise RuntimeError('Packaged genuine performance changed: '+name)
            if abs(lengths[name]/20-seconds)>.051:raise RuntimeError('Genuine event duration mismatch: '+name)
            if 'temporary audio' in captions[name]['speaker']:raise RuntimeError('Genuine line mislabeled temporary: '+name)
    if jar:jar.close()
    print(json.dumps({'status':'placeholder_integrity_passed','temporary_events':len(entries),'genuine_events':len(report['genuine_events']),'decoded_unique_recordings':len(decoded),'genuine_voice_quality_certified':False,'native_playback_verified':False}),flush=True)

if __name__=='__main__':main()
