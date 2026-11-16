"""Convert original Wybie visit masters once and merge exact recording durations."""
import hashlib, json, math, shutil, subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/coraline'
def main(character='wybie'):
    ffmpeg=shutil.which('ffmpeg'); ffprobe=shutil.which('ffprobe')
    assert ffmpeg and ffprobe,'FFmpeg and FFprobe are required for the original visit recordings'
    sounds=json.loads((ASSETS/'sounds.json').read_text(encoding='utf8'))
    lengths=json.loads((ASSETS/'visit_audio_lengths.json').read_text(encoding='utf8'))
    report_path=ASSETS/f'{character}_visit_audio.json'
    previous=json.loads(report_path.read_text(encoding='utf8')) if report_path.exists() else {}
    report={}
    for letter in list('abcdefghijkl' if character=='wybie' else 'abcdefghijk' if character=='bobinsky' else 'abcdefghij')+['music']:
        title='Wybie' if character=='wybie' else 'Bobinsky' if character=='bobinsky' else 'SpinkAndForcible';voice='Wybie' if character=='wybie' else 'bobinsky' if character=='bobinsky' else 'MissSpinkAndForcible'
        source=ROOT/(f'music/eventbased/{title}.mp3' if letter=='music' else f'voicelines/{voice}/{letter.upper()}.wav')
        target=ASSETS/f'sounds/{character}_visit/{letter}.ogg'; target.parent.mkdir(parents=True,exist_ok=True)
        digest=hashlib.sha256(source.read_bytes()).hexdigest() if source.exists() else previous.get(letter,{}).get('source_sha256')
        assert digest and (source.exists() or target.exists()),f'Missing both original and exported recording: {source}'
        if source.exists() and (not target.exists() or previous.get(letter,{}).get('source_sha256')!=digest):
            subprocess.run([ffmpeg,'-v','error','-y','-i',str(source),'-vn','-ar','48000' if letter=='music' else '24000','-ac','2' if letter=='music' else '1','-c:a','libvorbis','-q:a','4',str(target)],check=True)
        subprocess.run([ffmpeg,'-v','error','-i',str(target),'-f','null','-'],check=True)
        seconds=float(subprocess.check_output([ffprobe,'-v','error','-show_entries','format=duration','-of','csv=p=0',str(target)],text=True))
        name=f'{character}_visit_{letter}'; lengths[name]=math.ceil(seconds*20)
        sounds[name]={'sounds':[{'name':f'coraline:{character}_visit/{letter}','stream':True}]}
        report[letter]={'source':source.relative_to(ROOT).as_posix(),'source_sha256':digest,'seconds':seconds,'ticks':lengths[name]}
        print(name,lengths[name],'ticks')
    for path,data in [(ASSETS/'sounds.json',sounds),(ASSETS/'visit_audio_lengths.json',lengths),(report_path,report)]:
        raw=(json.dumps(data,indent=2)+'\n').encode('utf8')
        if not path.exists() or path.read_bytes()!=raw:path.write_bytes(raw)
if __name__=='__main__':main();main('bobinsky');main('tea')
