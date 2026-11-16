"""20 Hz RMS envelopes from retained voice exports; no runtime PCM decoder or re-encoding."""
from pathlib import Path
import array,json,math,subprocess,sys
ROOT=Path(__file__).resolve().parents[1];ASSETS=ROOT/'src/main/resources/assets/coraline'
envelope_path=ASSETS/'visit_voice_envelopes.json'
output=json.loads(envelope_path.read_text(encoding='utf8')) if envelope_path.exists() else {}
for who,letters in [('wybie','abcdefghijkl'),('bobinsky','abcdefghijk'),('cat','abcdefghi'),('cocoa_beetles','abcdefg'),('mirror_prison','abcde'),('first_escape','abcdefg'),('missing_parents','abcdefg'),('missing_parents_tea','abcdefghi'),('missing_parents_cat','abcdef'),('parent_rescue','abcdefghijklmn')]:
    for letter in letters:
        prefix='cat_encounter' if who=='cat' else who+'_visit' if who in ('wybie','bobinsky') else who
        pcm=subprocess.check_output(['ffmpeg','-nostdin','-v','error','-threads','1','-filter_threads','1','-i',str(ASSETS/f'sounds/{prefix}/{letter}.ogg'),'-f','s16le','-ar','16000','-ac','1','-'])
        samples=array.array('h');samples.frombytes(pcm)
        if sys.byteorder!='little':samples.byteswap()
        values=[math.sqrt(sum(float(v)*v for v in samples[i:i+800])/max(1,len(samples[i:i+800])))/32768 for i in range(0,len(samples),800)]
        peak=max(values,default=0);gate=max(.004,peak*.035)
        output[f'{prefix}_{letter}']=[round(min(1,max(0,(v-gate)/max(.01,peak-gate)))**.65,4) if v>gate else 0 for v in values]
(ASSETS/'visit_voice_envelopes.json').write_text(json.dumps(output,separators=(',',':'))+'\n',encoding='utf8')
assert len(output)>=87 and all(0<=n<=1 for a in output.values() for n in a)
print('Validated',len(output),'RMS voice envelopes; silence gated at -48 dB or 3.5% of the recording peak')
