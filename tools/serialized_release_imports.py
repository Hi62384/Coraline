"""Run the actual Gradle-exported genuine import graph without a resident JVM.

Never synthesize voices, weaken emergency limits, assemble classes, or skip the
normal resource optimizer/checks. Receipts pin all masters, scripts and exports.
"""
from pathlib import Path
import argparse,ctypes,hashlib,json,os,re,subprocess,sys,threading,time
from ctypes import wintypes
import check_numbered_candidate as monitor
from run_low_memory_release import job,ready_memory
ROOT=monitor.ROOT

def sha(path):
    with path.open('rb') as stream:return hashlib.file_digest(stream,'sha256').hexdigest()

def snapshot(inputs):
    paths=[]
    if inputs:
        paths=[ROOT/'build.gradle',ROOT/'gradle.properties']
        for name in ('tools','voicelines','music','GameMusics','artwork','sound'):
            directory=ROOT/name
            for path in directory.rglob('*'):
                if not path.is_file() or '__pycache__' in path.parts:continue
                if name=='tools' and path.suffix.lower() not in ('.py','.json','.txt'):continue
                paths.append(path)
    else:paths=[p for p in (ROOT/'src/main/resources').rglob('*') if p.is_file()]
    result={}
    for path in sorted(paths):
        if path.is_symlink() or not path.resolve().is_relative_to(ROOT):raise RuntimeError('Linked import input/output refused')
        result[path.relative_to(ROOT).as_posix()]=sha(path)
    return result

def graph_at(path):
    graph=json.loads(path.read_text(encoding='utf-8'))
    if graph.get('format')!=1 or not 20<=len(graph.get('tasks',[]))<=60:raise RuntimeError('Incomplete exported import graph')
    names=set()
    for row in graph['tasks']:
        name=row['name'];command=row['command']
        if name in names or not re.fullmatch(r'[A-Za-z]+',name):raise RuntimeError('Unexpected/duplicate import task')
        names.add(name)
        if not 2<=len(command)<=4 or command[0]!='python' or not re.fullmatch(r'tools/[a-z_]+\.py',command[1]) or not (ROOT/command[1]).is_file():raise RuntimeError('Unreviewed importer command')
        if any('temporary' in value for value in command):raise RuntimeError('Temporary speech forbidden')
        if row.get('environment',{}).keys()-{'CORALINE_VOICE_MANIFEST'}:raise RuntimeError('Unreviewed importer environment')
    if not {'importParentVoices','importGameAudio','importIncidentalDialogueAudio','importAcceptedGhostAudio','buildStorySubtitles','buildFinalChapterEnvelopes'}<=names:raise RuntimeError('Required genuine import/caption steps absent')
    return graph

def owned(path):
    path=Path(path).resolve()
    if not path.is_relative_to(ROOT/'build-character-cast') or path.suffix!='.json':raise RuntimeError('Receipt outside owned build directory')
    return path

def verify(receipt):
    proof=json.loads(receipt.read_text(encoding='utf-8'))
    graph=graph_at(owned(proof['graph_path']))
    if proof.get('status')!='passed' or proof.get('graph')!=graph:raise RuntimeError('Import graph was not completed')
    steps=proof.get('steps',[])
    if len(steps)!=len(graph['tasks']) or any(step.get('status')!='passed' or step.get('exit_code')!=0 or step.get('name')!=row['name'] for step,row in zip(steps,graph['tasks'])):raise RuntimeError('Incomplete serial imports')
    if proof.get('inputs')!=snapshot(True):raise RuntimeError('Master/script inputs changed after serial imports')
    if proof.get('outputs')!=snapshot(False):raise RuntimeError('Imported audio/caption outputs changed after serial imports')
    print('Verified complete genuine serial import graph: '+str(len(steps))+' steps; all input/output hashes match.',flush=True)
    return proof

def prepare(graph_path):
    graph=graph_at(graph_path);run=graph_path.parent;receipt=run/'receipt.json'
    if receipt.exists():raise RuntimeError('Fresh serial preparation required')
    inputs=snapshot(True);state={'status':'running','graph_path':str(graph_path),'graph':graph,'inputs':inputs,'steps':[],'emergency_free_mib':384,'voice_generation':False}
    def save():receipt.write_text(json.dumps(state,indent=2)+'\n',encoding='utf-8')
    save();print('Serial genuine import receipt: '+str(receipt),flush=True)
    try:
        for index,row in enumerate(graph['tasks']):
            ready_memory(640,45)
            print('Import '+str(index+1)+'/'+str(len(graph['tasks']))+': '+row['name'],flush=True)
            api,handle=job(768);result={'name':row['name']}
            try:
                env=monitor.child_environment();env['PATH']=str(ROOT/'.voice-tools/index-tts/.venv/Scripts')+';C:\\ffmpeg;'+env['PATH'];env['PYTHONUTF8']='1';env['PYTHONIOENCODING']='utf-8';env.update(row['environment'])
                with (run/(row['name']+'.log')).open('xb',buffering=0) as log:
                    child=subprocess.Popen(row['command'],cwd=ROOT,env=env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,bufsize=0)
                    if not api.AssignProcessToJobObject(handle,wintypes.HANDLE(int(child._handle))):child.kill();child.wait();raise ctypes.WinError(ctypes.get_last_error())
                    reached,errors=threading.Event(),[];reader=threading.Thread(target=monitor.drain_bounded,args=(child.stdout,log,monitor.MAX_LOG_BYTES,reached,errors),daemon=True);reader.start()
                    result.update(monitor.watch_child(child,600,monitor.free_gib,monitor.process_memory,log_limit=reached));reader.join(timeout=10)
                    if errors or reached.is_set() or reader.is_alive():result['status']='output_capture_failed'
            finally:api.CloseHandle(handle)
            state['steps'].append(result);save()
            if result.get('status')!='passed' or result.get('exit_code')!=0:raise RuntimeError('Serial import stopped: '+row['name'])
        if snapshot(True)!=inputs:raise RuntimeError('Source masters or scripts changed during imports')
        state.update(status='passed',outputs=snapshot(False));save();verify(receipt)
    except BaseException as error:
        state.update(status='failed',error=str(error));save();raise

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('action',choices=['prepare','verify']);parser.add_argument('--graph');parser.add_argument('--receipt');args=parser.parse_args()
    if args.action=='prepare':prepare(owned(args.graph))
    else:verify(owned(args.receipt))
