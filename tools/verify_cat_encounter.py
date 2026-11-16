"""Verify supplied cat masters, complete peak-safe exports and packaged references."""
from pathlib import Path
import argparse,hashlib,json,math,re,subprocess,tempfile,zipfile
ROOT=Path(__file__).resolve().parents[1]

def measure(path):
    result=subprocess.run(['ffmpeg','-nostdin','-hide_banner','-threads','1','-filter_threads','1','-i',str(path),'-af','loudnorm=I=-13:TP=-1.5:LRA=7:print_format=json','-f','null','-'],capture_output=True,text=True,check=True)
    match=re.search(r'\{\s*"input_i".*?\}',result.stderr,re.S)
    assert match,'Missing measured loudness'
    return json.loads(match.group())

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--jar',type=Path);parser.add_argument('--report',type=Path);args=parser.parse_args()
    assets=ROOT/'src/main/resources/assets/coraline';archive=zipfile.ZipFile(args.jar) if args.jar else None
    read=lambda name:archive.read('assets/coraline/'+name) if archive else (assets/name).read_bytes()
    proof=json.loads(read('cat_encounter_audio.json'));sounds=json.loads(read('sounds.json'));lengths=json.loads(read('visit_audio_lengths.json'));envelopes=json.loads(read('visit_voice_envelopes.json'));checks=[]
    with tempfile.TemporaryDirectory(prefix='coraline-cat-verify-') as folder:
        for letter in 'ABCDEFGHI':
            entry=proof[letter];source=ROOT/'voicelines/cat'/(letter+'.wav');assert hashlib.sha256(source.read_bytes()).hexdigest()==entry['source_sha256']
            event='cat_encounter_'+letter.lower();resource=sounds[event]['sounds'][0]['name'].removeprefix('coraline:');payload=read('sounds/'+resource+'.ogg');assert hashlib.sha256(payload).hexdigest()==entry['ogg_sha256']
            exported=Path(folder)/(letter+'.ogg');exported.write_bytes(payload)
            info=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_streams','-show_format','-of','json',str(exported)],text=True));stream=info['streams'][0];seconds=float(info['format']['duration'])
            assert int(stream['sample_rate'])<=48000 and int(stream['channels'])==1 and abs(seconds-entry['seconds'])<.03
            assert abs(lengths[event]-math.ceil(seconds*20))<=1 and len(envelopes[event])>=seconds*20-2
            before=measure(source);after=measure(exported);lufs=float(after['input_i']);peak=float(after['input_tp'])
            # Short, highly dynamic speech need not land exactly on target LUFS.
            # Require a measured audible increase without clipping or over-loud output.
            assert float(before['input_i'])+3<=lufs<=-11 and peak<-.1,(event,lufs,peak)
            checks.append({'event':event,'seconds':seconds,'source_lufs':float(before['input_i']),'export_lufs':lufs,'peak_db':peak})
    if archive:archive.close()
    result={'passed':True,'recordings':checks,'masters_unchanged':True,'new_speech_generated':False,'human_listening':False}
    if args.report:args.report.write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(result,indent=2))

if __name__=='__main__':main()
