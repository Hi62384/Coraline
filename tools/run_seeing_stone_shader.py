"""Capped hidden OpenGL shader/pixel regression; no game launch or user windows."""
import ctypes, hashlib, json, os, subprocess, threading, time
from ctypes import wintypes
import check_numbered_candidate as monitor
from run_low_memory_release import job

root=monitor.ROOT
folder=root/'build-character-cast/seeing-stone-shader'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime())
folder.mkdir(parents=True,exist_ok=False)
deps=(root/monitor.CP).read_text(encoding='utf-8-sig').splitlines()
cp=os.pathsep.join([str(folder),*deps])
shader=root/'src/main/resources/assets/coraline/shaders/program/seeing_stone.fsh'
vanilla=next(p for p in deps if p.replace('\\','/').endswith('/1.20.1/client-extra.jar'))
report={'scope':'Actual hidden OpenGL Seeing Stone/rescue-world fragment shader and readback; NOT Minecraft scene/hand/eye acceptance','shader_sha256':hashlib.sha256(shader.read_bytes()).hexdigest(),'rescue_shader_sha256':hashlib.sha256((root/'src/main/resources/assets/coraline/shaders/program/rescue_world.fsh').read_bytes()).hexdigest(),'steps':{}}
commands={
 'compile':[str(monitor.JAVA/'javac.exe'),'-J-Xmx128m','-proc:none','-cp',cp,'-d',str(folder),str(root/'tools/SeeingStoneShaderSmoke.java'),str(root/'tools/SeeingStoneDepthSmoke.java'),str(root/'src/main/java/com/coraline/client/SeeingStoneFramebuffer.java')],
 'pixels':[str(monitor.JAVA/'java.exe'),'-Xmx128m','-XX:+UseSerialGC','-XX:MaxDirectMemorySize=64m','-cp',cp,'SeeingStoneShaderSmoke',str(shader),vanilla,str(root/'src/main/resources/assets/coraline/shaders/program/rescue_world.fsh')],
 'depth':[str(monitor.JAVA/'java.exe'),'-Xmx128m','-XX:+UseSerialGC','-XX:MaxDirectMemorySize=64m','-cp',cp,'SeeingStoneDepthSmoke']
}
for label,cmd in commands.items():
    if monitor.free_gib()<monitor.START_FREE_GIB:raise RuntimeError('Below unchanged 640 MiB start floor')
    api,handle=job(512)
    try:
        with (folder/(label+'.log')).open('xb',buffering=0) as log:
            child=subprocess.Popen(cmd,cwd=root,env=monitor.child_environment(),stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
            if not api.AssignProcessToJobObject(handle,wintypes.HANDLE(int(child._handle))):child.kill();child.wait();raise ctypes.WinError(ctypes.get_last_error())
            reached,errors=threading.Event(),[];reader=threading.Thread(target=monitor.drain_bounded,args=(child.stdout,log,monitor.MAX_LOG_BYTES,reached,errors),daemon=True);reader.start()
            result=monitor.watch_child(child,180,monitor.free_gib,monitor.process_memory,log_limit=reached);reader.join(timeout=10)
            if reader.is_alive() or errors or reached.is_set():result['status']='capture_failed'
            report['steps'][label]=result
    finally:
        api.CloseHandle(handle);(folder/'report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
    print(monitor.tail_bytes(folder/(label+'.log')),flush=True)
    if result.get('status')!='passed' or result.get('exit_code')!=0:raise SystemExit(1)
print('Report: '+str(folder/'report.json'))
