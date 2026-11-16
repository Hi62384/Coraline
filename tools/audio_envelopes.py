"""Retained export -> 20 Hz RMS only. Does not recognize words or change a recording."""
import array,math,subprocess,sys
def envelope(path):
    pcm=subprocess.check_output(['ffmpeg','-nostdin','-v','error','-threads','1','-filter_threads','1','-i',str(path),'-f','s16le','-ar','16000','-ac','1','-'])
    samples=array.array('h');samples.frombytes(pcm)
    if sys.byteorder!='little':samples.byteswap()
    values=[math.sqrt(sum(float(v)*v for v in samples[i:i+800])/max(1,len(samples[i:i+800])))/32768 for i in range(0,len(samples),800)]
    peak=max(values,default=0);gate=max(.004,peak*.035)
    return [round(min(1,max(0,(v-gate)/max(.01,peak-gate)))**.65,4) if v>gate else 0 for v in values]
