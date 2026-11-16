"""Bounded original-recording import; no generation, transcription or subtitle writes."""
import ctypes,json,os,subprocess,threading,time,sys
from ctypes import wintypes
import check_numbered_candidate as monitor
from run_low_memory_release import job
root=monitor.ROOT
if monitor.free_gib()<monitor.START_FREE_GIB:raise RuntimeError('Below unchanged 640 MiB start floor')
folder=root/'build-character-cast/rescue-import'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime());folder.mkdir(parents=True,exist_ok=False)
api,handle=job(512)
report={'scope':'Supplied masters only; no voice generation/transcription/subtitles','start_free_gib':monitor.free_gib()}
try:
    with (folder/'output.log').open('xb',buffering=0) as log:
        environment=monitor.child_environment();environment['PATH']=r'C:\ffmpeg'+os.pathsep+environment['PATH']
        script='tools/import_spink_boss_music.py' if sys.argv[1:]==['--spink-boss-music-only'] else 'tools/import_game_proposal_music.py' if sys.argv[1:]==['--game-proposal-music-only'] else 'tools/import_rescue_arrival_music.py' if sys.argv[1:]==['--rescue-arrival-music-only'] else 'tools/import_missing_parents_music.py' if sys.argv[1:]==['--missing-parents-music-only'] else 'tools/import_first_escape_music.py' if sys.argv[1:]==['--first-escape-music-only'] else 'tools/import_the_cat_music.py' if sys.argv[1:]==['--the-cat-music-only'] else 'tools/import_long_face_music.py' if sys.argv[1:]==['--long-face-music-only'] else 'tools/import_other_father_boss_music.py' if sys.argv[1:]==['--boss-music-only'] else 'tools/import_rescue_chapters.py'
        child=subprocess.Popen(['python',script],cwd=root,env=environment,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
        if not api.AssignProcessToJobObject(handle,wintypes.HANDLE(int(child._handle))):child.kill();child.wait();raise ctypes.WinError(ctypes.get_last_error())
        reached,errors=threading.Event(),[];reader=threading.Thread(target=monitor.drain_bounded,args=(child.stdout,log,monitor.MAX_LOG_BYTES,reached,errors),daemon=True);reader.start()
        report.update(monitor.watch_child(child,300,monitor.free_gib,monitor.process_memory,log_limit=reached));reader.join(timeout=10)
        if reader.is_alive() or errors or reached.is_set():report['status']='capture_failed'
finally:
    api.CloseHandle(handle);(folder/'report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
print(monitor.tail_bytes(folder/'output.log'));print('Report: '+str(folder/'report.json'))
raise SystemExit(0 if report.get('status')=='passed' and report.get('exit_code')==0 else 1)
