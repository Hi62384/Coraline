"""Run ForgeGradle's real QA launch after the compiler JVM has exited."""
from pathlib import Path
import argparse, ctypes, hashlib, json, os, re, shutil, subprocess, sys, threading, time
from datetime import datetime
from ctypes import wintypes
import check_numbered_candidate as monitor
from run_low_memory_release import job

root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser();parser.add_argument('kind',choices=['server','client']);parser.add_argument('--test-classes');parser.add_argument('--heap-mib',type=int,choices=[384,512,768,1024,1536],default=1024);parser.add_argument('--refresh-resources',action='store_true');parser.add_argument('--timeout-seconds',type=int,choices=[180,300,600,1800],default=600);parser.add_argument('--first-escape',action='store_true');parser.add_argument('--insect-parlor',action='store_true');parser.add_argument('--missing-parents-tea',action='store_true');parser.add_argument('--missing-parents-cat',action='store_true')
parser.add_argument('--minimum-start-free-mib',type=int,choices=[640,1536,1792,2048,3072],default=640)
parser.add_argument('--memory-wait-seconds',type=int,choices=[0,30,45],default=0)
parser.add_argument('--compiled-report',help='Full-source isolated javac report; server QA or explicit isolated rescue-rig renderer only')
parser.add_argument('--rescue-rig',action='store_true',help='Real renderer capture at the title screen; no world, not scene acceptance')
parser.add_argument('--story-audio',action='store_true',help='Muted original-recording OpenAL observer test at title screen; no world or scene acceptance')
parser.add_argument('--rescue-attic',action='store_true',help='Continue the disposable parlor capture through Bobinsky furnace/ceiling staging')
parser.add_argument('--rescue-finale',action='store_true',help='Checkpoint playthrough using acknowledged original recordings and real client movement/input')
parser.add_argument('--resume-without-theatre',action='store_true',help='Explicit disposable resumed Cat-departure checkpoint with the theatre flag cleared')
parser.add_argument('--rescue-web',action='store_true',help='Standalone production web/crouch/hearing client regression; not story completion')
parser.add_argument('--family-game-night',action='store_true',help='Post-invitation online owner, natural original NPC arrival and normally completed match; disposable authored first-floor slice')
parser.add_argument('--nautical-opening',action='store_true',help='Post-invitation normal seat and all A-N genuine mixer cues; not whole-story acceptance')
parser.add_argument('--resume-finale-report',help='Copy only a stopped, tool-created disposable finale world; never a user save')
parser.add_argument('--story-rebirth',action='store_true',help='Disposable post-ending ritual checkpoint, not a full story replay')
parser.add_argument('--tycoon-phase-one',action='store_true',help='Disposable owned-world/physical-care/inspection Phase 1 checkpoint, not a user save')
parser.add_argument('--tycoon-phase-two',action='store_true',help='Disposable personalized-doll/group-visit/procedural-world Phase 2 checkpoint; no user save')
parser.add_argument('--tycoon-food',action='store_true',help='With --tycoon-phase-two, disposable real shop, placement, fridge, meal and cake regression')
parser.add_argument('--tycoon-expansion',action='store_true',help='With --tycoon-phase-two, developed-world Phase 3/4/5 UI, transaction, ritual and renderer checkpoint; not a full automation loop')
parser.add_argument('--resume-tycoon-two-report',help='Resume only a successfully stopped disposable Phase 2 QA save')
parser.add_argument('--theatre-aerial',action='store_true',help='With --nautical-opening, test the real E/F grab, throw and rescue instead of A-N')
parser.add_argument('--grab-regression',action='store_true',help='With --first-escape, actual chair capture then post-refusal F/G grab checkpoint')
parser.add_argument('--portal-aperture',action='store_true',help='With --first-escape, real four-facing door aperture views')
parser.add_argument('--connected-story',action='store_true',help='Disposable loopback host; requires a separate actual CoralineQAGuest client and reconnect')
parser.add_argument('--connected-transactions',action='store_true',help='Extend real connected-story QA through temple death/win, mall delivery and online/offline household purchases; requires a second guest reconnect')
parser.add_argument('--seeing-stone-music',action='store_true',help='Visible stone/mob/sky GPU capture and full real FirstEscape music across native dimension transfers')
parser.add_argument('--accepted-ghost',action='store_true',help='With --first-escape, normal sewing followed by explicit post-activity ghost-prison checkpoint and native manual mirror exit')
args=parser.parse_args()
if args.heap_mib==384 and (args.kind!='server' or not args.compiled_report or not args.test_classes):raise RuntimeError('384 MiB heap is limited to explicitly selected precompiled server regressions')
if args.accepted_ghost and (not args.first_escape or not args.compiled_report or args.kind!='client' or args.insect_parlor or args.grab_regression or args.portal_aperture):raise RuntimeError('Accepted ghost requires its own precompiled first-escape client')
if args.connected_transactions:args.connected_story=True
if args.seeing_stone_music and (args.kind!='client' or not args.compiled_report or args.refresh_resources or any((args.connected_story,args.first_escape,args.rescue_rig,args.rescue_web,args.story_audio,args.family_game_night,args.nautical_opening,args.story_rebirth,args.tycoon_phase_one,args.tycoon_phase_two,args.missing_parents_cat,args.missing_parents_tea))):raise RuntimeError('Visible stone/music requires its own precompiled disposable client')
if args.connected_story and (args.kind!='client' or not args.compiled_report or args.refresh_resources or any((args.first_escape,args.rescue_rig,args.rescue_web,args.story_audio,args.family_game_night,args.nautical_opening,args.story_rebirth,args.tycoon_phase_one,args.tycoon_phase_two,args.missing_parents_cat,args.missing_parents_tea))):raise RuntimeError('Connected-story requires its own precompiled disposable client')
if args.tycoon_food and (not args.tycoon_phase_two or args.resume_tycoon_two_report):raise RuntimeError('Food requires its own disposable Phase2 client, without resume')
if args.tycoon_expansion and (not args.tycoon_phase_two or args.tycoon_food or args.resume_tycoon_two_report):raise RuntimeError('Expansion requires its own disposable Phase2 client, without food or resume')
if args.tycoon_phase_two and (args.kind!='client' or not args.compiled_report or args.refresh_resources or any((args.tycoon_phase_one,args.story_rebirth,args.nautical_opening,args.family_game_night,args.first_escape,args.rescue_rig,args.rescue_web,args.story_audio,args.missing_parents_cat,args.missing_parents_tea))):raise RuntimeError('Tycoon Phase2 requires its own precompiled disposable client')
if args.tycoon_phase_one and (args.kind!='client' or not args.compiled_report or args.refresh_resources or any((args.story_rebirth,args.nautical_opening,args.family_game_night,args.first_escape,args.rescue_rig,args.rescue_web,args.story_audio,args.missing_parents_cat,args.missing_parents_tea))):raise RuntimeError('Tycoon requires its own precompiled disposable client')
if (args.grab_regression or args.portal_aperture) and (not args.first_escape or args.kind!='client' or args.insect_parlor or args.rescue_attic or args.rescue_finale):raise RuntimeError('Grab/aperture checks require a standalone first-escape client mode')
if args.grab_regression and args.portal_aperture:raise RuntimeError('Run the native regressions serially')
if args.theatre_aerial and not args.nautical_opening:raise RuntimeError('Aerial regression requires the isolated theatre client mode')
if args.story_rebirth and (args.kind!='client' or not args.compiled_report or args.refresh_resources or any((args.nautical_opening,args.family_game_night,args.first_escape,args.rescue_rig,args.rescue_web,args.story_audio,args.missing_parents_cat,args.missing_parents_tea))):raise RuntimeError('Story rebirth requires its own precompiled disposable client')
if args.nautical_opening and (args.kind!='client' or not args.compiled_report or args.refresh_resources or any((args.first_escape,args.rescue_rig,args.rescue_web,args.story_audio,args.family_game_night,args.missing_parents_cat,args.missing_parents_tea))):raise RuntimeError('Nautical opening requires its own precompiled disposable client')
if args.family_game_night and (args.kind!='client' or not args.compiled_report or args.refresh_resources or args.first_escape or args.rescue_rig or args.rescue_web or args.story_audio or args.missing_parents_cat or args.missing_parents_tea):raise RuntimeError('Natural game night requires its own precompiled disposable client mode')
if args.story_audio and (args.kind!='client' or not args.compiled_report or args.first_escape or args.rescue_rig or args.rescue_web or args.missing_parents_cat or args.missing_parents_tea):raise RuntimeError('Story mixer check requires its own precompiled title-screen client')
if args.rescue_rig and (args.kind!='client' or not args.compiled_report or args.first_escape or args.missing_parents_cat or args.missing_parents_tea):raise RuntimeError('Rescue rig requires explicit precompiled client mode only')
if args.insect_parlor and not args.first_escape:raise RuntimeError('Parlor preview requires the first-escape visual harness')
if args.rescue_attic and not (args.kind=='client' and args.first_escape and args.insect_parlor and args.compiled_report):raise RuntimeError('Attic capture requires the explicit isolated parlor client fixture')
if args.rescue_finale and not (args.kind=='client' and args.first_escape and args.insect_parlor and args.compiled_report) or args.rescue_finale and args.rescue_attic:raise RuntimeError('Finale requires its own explicit disposable parlor continuation')
if args.resume_without_theatre and not args.resume_finale_report:raise RuntimeError('No-theatre checkpoint requires an explicitly resumed disposable finale')
if args.rescue_web and (args.kind!='client' or not args.compiled_report or args.first_escape or args.rescue_rig or args.missing_parents_cat or args.missing_parents_tea):raise RuntimeError('Web regression requires its own precompiled disposable client mode')
if args.resume_finale_report and not args.rescue_finale:raise RuntimeError('Disposable resume is only for the explicit finale checkpoint')
if args.resume_tycoon_two_report and (not args.tycoon_phase_two or args.resume_finale_report):raise RuntimeError('Phase 2 resume requires its own explicit tycoon checkpoint')
if sum((args.first_escape,args.missing_parents_tea,args.missing_parents_cat))>1:raise RuntimeError('Select one visual harness')
stage=('native' if args.kind=='server' else 'render') if args.refresh_resources else ('native-ready' if args.kind=='server' else 'render-ready')
command=[sys.executable,str(root/'tools/run_low_memory_release.py'),stage,'--export-only','--lean-prepare']
if args.kind=='client':command+=['--render-classes','MissingParentsCatVisualCheck.java' if args.missing_parents_cat else 'MissingParentsTeaVisualCheck.java' if args.missing_parents_tea else 'FirstEscapeVisualCheck.java' if args.first_escape else 'VisitEventsVisualCheck.java']
if args.test_classes:command+=['--test-classes',args.test_classes]
task='runGameTestServer' if args.kind=='server' else 'runClient'
manifest=root/'build-character-cast'/('qa-launch-'+task+'.json')
started=time.time()
compiled=None;expected_tests=None
if args.compiled_report:
    if (args.kind!='server' and not (args.seeing_stone_music or args.connected_story or args.rescue_rig or args.first_escape or args.rescue_web or args.story_audio or args.family_game_night or args.nautical_opening or args.story_rebirth or args.tycoon_phase_one or args.tycoon_phase_two or args.missing_parents_tea or args.missing_parents_cat)) or args.refresh_resources:raise RuntimeError('Precompiled mode requires server QA or an explicit supported isolated capture, with normally optimized resources')
    supplied=Path(args.compiled_report).resolve();base=(root/'build-character-cast/cat-focused-compile').resolve()
    if not supplied.is_relative_to(base) or supplied.name!='report.json':raise RuntimeError('Unexpected compiler report')
    compiled=json.loads(supplied.read_text());hashes=compiled.get('source_sha256',{})
    if compiled.get('status')!='passed' or compiled.get('exit_code')!=0:raise RuntimeError('Successful source compilation required')
    if not compiled.get('full_source_compile'):
        if args.kind!='client' or not compiled.get('validated_full_source_base'):raise RuntimeError('Complete successful source compilation required')
        ancestor=Path(compiled['validated_full_source_base']).resolve()
        if not ancestor.is_relative_to(base) or ancestor.name!='report.json' or hashlib.sha256(ancestor.read_bytes()).hexdigest()!=compiled.get('validated_base_report_sha256'):raise RuntimeError('Invalid inherited full-source report')
        prior_compile=json.loads(ancestor.read_text());prior_hashes={key.replace('\\','/'):value for key,value in prior_compile.get('source_sha256',{}).items()}
        if prior_compile.get('status')!='passed' or prior_compile.get('exit_code')!=0 or prior_compile.get('full_source_compile') is not True:raise RuntimeError('Inherited base is not a complete successful compile')
        for relative,value in hashes.items():
            key=relative.replace('\\','/')
            if not key.startswith('src/rendercheck/java/') and prior_hashes.get(key)!=value:raise RuntimeError('Inherited production source changed; full recompile required')
    required={str(p.relative_to(root)).replace('\\','/') for p in (root/'src/main/java').rglob('*.java')}
    normalized={p.replace('\\','/'):digest for p,digest in hashes.items()}
    if not required.issubset(normalized):raise RuntimeError('Compiler did not cover every production source')
    for relative,digest in normalized.items():
        source=(root/relative).resolve()
        if not source.is_relative_to(root) or hashlib.sha256(source.read_bytes()).hexdigest()!=digest:raise RuntimeError('Source changed after isolated compile: '+relative)
    if args.kind=='server':
        selection=set(args.test_classes.split(',')) if args.test_classes else None
        expected_tests=sum(len(re.findall(r'@GameTest\s*\(', (root/path).read_text(encoding='utf8'))) for path in normalized if path.startswith('src/wybietest/') and path.endswith('GameTests.java') and (selection is None or Path(path).name in selection))
        if not expected_tests:raise RuntimeError('No compiled actual GameTest methods')
    isolated=supplied.parent/'classes'
    if args.rescue_rig and not (isolated/'com/coraline/client/RescueRigVisualCheck.class').is_file():raise RuntimeError('Isolated renderer fixture was not compiled')
    if args.first_escape and not (isolated/'com/coraline/otherworld/FirstEscapeVisualCheck.class').is_file():raise RuntimeError('Isolated first-escape fixture was not compiled')
    if args.grab_regression and not (isolated/'com/coraline/feast/FirstEscapeGrabVisualCheck.class').is_file():raise RuntimeError('Chair/grab fixture was not compiled; game not started')
    if args.portal_aperture and not (isolated/'com/coraline/otherworld/SeamlessTunnelVisualCheck.class').is_file():raise RuntimeError('Tunnel aperture fixture was not compiled; game not started')
    if args.rescue_web and not (isolated/'com/coraline/otherworld/RescueWebVisualCheck.class').is_file():raise RuntimeError('Standalone web client regression was not compiled')
    if args.story_audio and not (isolated/'com/coraline/client/StoryAudioPlaybackVisualCheck.class').is_file():raise RuntimeError('Story mixer observer regression was not compiled')
    if args.family_game_night and not (isolated/'com/coraline/feast/FamilyGameNightNaturalVisualCheck.class').is_file():raise RuntimeError('Natural game-night fixture was not compiled')
    if args.nautical_opening and not (isolated/'com/coraline/feast/NauticalOpeningVisualCheck.class').is_file():raise RuntimeError('Nautical opening fixture was not compiled')
    if args.story_rebirth and not (isolated/'com/coraline/rebirth/StoryRebirthVisualCheck.class').is_file():raise RuntimeError('Story rebirth fixture was not compiled')
    if args.tycoon_phase_two and not (isolated/'com/coraline/tycoon/TycoonPhaseTwoVisualCheck.class').is_file():raise RuntimeError('Tycoon Phase2 fixture was not compiled')
    if args.missing_parents_tea and not (isolated/'com/coraline/feast/MissingParentsTeaVisualCheck.class').is_file():raise RuntimeError('Tea fixture was not compiled')
    if args.missing_parents_cat and not (isolated/'com/coraline/feast/MissingParentsCatVisualCheck.class').is_file():raise RuntimeError('Cat fixture was not compiled')
    if args.connected_story and not (isolated/'com/coraline/multiplayer/ConnectedStoryVisualCheck.class').is_file():raise RuntimeError('Connected story fixture was not compiled')
    if args.connected_transactions and not (isolated/'com/coraline/feast/ConnectedActivityVisualCheck.class').is_file():raise RuntimeError('Connected transaction fixture was not compiled')
    if args.seeing_stone_music and not (isolated/'com/coraline/client/SeeingStoneMusicVisualCheck.class').is_file():raise RuntimeError('Visible stone/music fixture was not compiled')
    for name in args.test_classes.split(',') if args.test_classes else []:
        if not re.fullmatch(r'[A-Za-z0-9_]+GameTests\.java',name) or len(list((isolated/'com/coraline').rglob(name.replace('.java','.class'))))!=1:raise RuntimeError('Selected native test was not uniquely compiled: '+name)
    if not all((isolated/('com/coraline/qa/'+name+'.class')).is_file() for name in ('LeanWorldVisualCheck','PalaceBackgroundVisualCheck')) or not (root/'build-character-cast/reports/asset-size.json').is_file():raise RuntimeError('Missing current QA mixins or normal optimizer output; recompile QA sources')
    # Reuse only ForgeGradle's real bootstrap configuration, never hand-invent
    # loader arguments. Code, test fixtures and disposable cwd are isolated below.
    if not manifest.is_file():raise RuntimeError('Cached ForgeGradle launch manifest missing')
else:
    if subprocess.run(command,cwd=root).returncode:raise SystemExit('QA preparation failed; game not started')
    if not manifest.is_file() or manifest.stat().st_mtime<started:raise RuntimeError('Fresh ForgeGradle launch manifest missing')
connected_class_report=None
if compiled and args.connected_story:
    from stage_connected_qa_classes import stage as stage_connected_classes
    isolated,connected_class_report=stage_connected_classes(root,isolated)
from verify_prepared_resources import verify as verify_prepared_resources
prepared_resources=verify_prepared_resources(root/'build-character-cast/resources/main',root/'build-character-cast/reports/asset-size.json')
raw=manifest.read_bytes();launch=json.loads(raw);cmd=launch['command'];cwd=Path(launch['cwd']).resolve();world_seed=None
if not cmd or Path(cmd[0]).resolve()!= (monitor.JAVA/'java.exe').resolve() or not cwd.is_relative_to(root) or not (cwd.name.startswith('run-tests-') or cwd.name=='run-wybie-visit-qa'):
    raise RuntimeError('Unexpected native launch target')
environment=monitor.child_environment()
# The producing Gradle process itself receives only the explicit essentials
# allowlist. Retain Forge's added launch variables, not arbitrary shell secrets.
allowed=set(environment)|{'JAVA_HOME','JAVA_TOOL_OPTIONS','MOD_CLASSES','MCP_MAPPINGS','MCP_VERSION','MC_VERSION','FORGE_VERSION','FORGE_GROUP','MCP_TO_SRG'}
extra=set(launch['environment'])-allowed
if extra:raise RuntimeError('Unreviewed launch environment names: '+', '.join(sorted(extra)))
environment.update({k:str(v) for k,v in launch['environment'].items()})
if compiled:
    environment['MOD_CLASSES']=os.pathsep.join('coraline%%'+str(p) for p in [root/'build-character-cast/resources/main',isolated,root/'src/gametest/resources'])
    # The bootstrap classpath also names the original production classes.
    # Replace that exact entry so stale interrupted Gradle classes cannot win.
    for flag in ('-cp','-classpath','--class-path'):
        if flag in cmd:
            index=cmd.index(flag)+1
            cmd[index]=os.pathsep.join(str(isolated) if Path(entry).resolve()==(root/'build-character-cast/classes/java/main').resolve() else entry for entry in cmd[index].split(os.pathsep))
    cwd=root/('run-tests-isolated-'+time.strftime('%Y%m%dT%H%M%SZ',time.gmtime()));cwd.mkdir(exist_ok=False)
    if args.kind=='server' and args.test_classes:
        # Discovery scans every class root. A selected CLI list alone does NOT
        # filter a full-source compiler output; stage unchanged class bytes in
        # a fresh QA-only root, omitting only unselected GameTest families.
        from qa_class_selection import stage_selected
        selected_classes=cwd/'selected-classes'
        stage_selected(isolated,selected_classes,set(args.test_classes.split(',')))
        environment['MOD_CLASSES']=environment['MOD_CLASSES'].replace(str(isolated),str(selected_classes))
        for flag in ('-cp','-classpath','--class-path'):
            if flag in cmd:
                index=cmd.index(flag)+1
                cmd[index]=os.pathsep.join(str(selected_classes) if Path(entry).resolve()==isolated.resolve() else entry for entry in cmd[index].split(os.pathsep))
    if args.rescue_rig or args.story_audio:
        # A renderer-only capture must not open a saved user/QA world. Retain
        # ForgeGradle's actual bootstrap and remove only its quick-play pair.
        if '--quickPlaySingleplayer' in cmd:
            quick=cmd.index('--quickPlaySingleplayer');del cmd[quick:quick+2]
    elif args.seeing_stone_music or args.connected_story or args.first_escape or args.missing_parents_tea or args.missing_parents_cat or args.rescue_web or args.family_game_night or args.nautical_opening or args.story_rebirth or args.tycoon_phase_one or args.tycoon_phase_two:
        # Copy only the existing disposable QA world's vanilla level descriptor.
        # No region files, player files, mod SavedData or user world are opened.
        if '--quickPlaySingleplayer' not in cmd or cmd[cmd.index('--quickPlaySingleplayer')+1]!='WybieVisitQA':raise RuntimeError('Unexpected disposable QA quick-play world')
        destination=cwd/'saves/WybieVisitQA'
        if args.resume_finale_report or args.resume_tycoon_two_report:
            resume_report=Path(args.resume_finale_report or args.resume_tycoon_two_report).resolve();reports=(root/'build-character-cast/prepared-native').resolve()
            if not resume_report.is_relative_to(reports) or resume_report.name!='report.json' or not re.fullmatch(r'\d{8}T\d{6}Z',resume_report.parent.name):raise RuntimeError('Resume requires a native QA report inside this checkout')
            previous=json.loads(resume_report.read_text());prior_cwd=Path(previous['cwd']).resolve()
            expected_property='-Dcoraline.tycoonPhaseTwoCheck=true' if args.resume_tycoon_two_report else '-Dcoraline.rescueFinaleVisualCheck=true'
            expected_status=('passed',) if args.resume_tycoon_two_report else ('failed','timed_out','low_memory_cutoff')
            if prior_cwd.parent!=root or not re.fullmatch(r'run-tests-isolated-\d{8}T\d{6}Z',prior_cwd.name) or abs((datetime.strptime(prior_cwd.name.removeprefix('run-tests-isolated-'),'%Y%m%dT%H%M%SZ')-datetime.strptime(resume_report.parent.name,'%Y%m%dT%H%M%SZ')).total_seconds())>120 or previous.get('kind')!='client' or previous.get('status') not in expected_status or expected_property not in previous.get('native_vm_defaults','') or previous.get('manifest_sha256')!=hashlib.sha256(raw).hexdigest():raise RuntimeError('Not a stopped disposable checkpoint owned by this QA runner')
            if args.resume_tycoon_two_report and (previous.get('exit_code')!=0 or not previous.get('clean_runtime_shutdown')):raise RuntimeError('Phase 2 resume requires a successful clean shutdown')
            kernel=ctypes.WinDLL('kernel32',use_last_error=True);kernel.OpenProcess.argtypes=[wintypes.DWORD,wintypes.BOOL,wintypes.DWORD];kernel.OpenProcess.restype=wintypes.HANDLE;kernel.GetExitCodeProcess.argtypes=[wintypes.HANDLE,ctypes.POINTER(wintypes.DWORD)];kernel.GetExitCodeProcess.restype=wintypes.BOOL;kernel.CloseHandle.argtypes=[wintypes.HANDLE]
            probe=kernel.OpenProcess(0x1000,False,int(previous['pid']))
            if probe:
                code=wintypes.DWORD();ok=kernel.GetExitCodeProcess(probe,ctypes.byref(code));kernel.CloseHandle(probe)
                if not ok or code.value==259:raise RuntimeError('Prior QA process is still live; no save copied')
            elif ctypes.get_last_error() not in (0,87):raise RuntimeError('Cannot verify prior QA process termination')
            source=prior_cwd/'saves/WybieVisitQA';files=list(source.rglob('*'))
            if source.is_symlink() or not (source/'level.dat').is_file() or any(p.is_symlink() or not p.resolve().is_relative_to(source.resolve()) for p in files) or sum(p.stat().st_size for p in files if p.is_file())>256*1024*1024:raise RuntimeError('Disposable resume save failed its path/size checks')
            required_files=('coraline_tycoon.dat',) if args.resume_tycoon_two_report else ('coraline_doors.dat','coraline_families.dat','coraline_first_visits.dat','coraline_other_realms.dat')
            for required in required_files:
                if not (source/'data'/required).is_file():raise RuntimeError('Prior QA save lacks canonical story records: '+required)
            shutil.copytree(source,destination)
            world_seed={'source':str(source),'native_report':str(resume_report),'copied_regions_or_player_files':True,'scope':'Stopped tool-created disposable checkpoint only; resumed acceptance, not an uninterrupted playthrough'}
        else:
            seed=root/'run-wybie-visit-qa/saves/WybieVisitQA/level.dat'
            if not seed.is_file() or seed.stat().st_size>1024*1024:raise RuntimeError('Disposable QA seed descriptor is unavailable')
            destination.mkdir(parents=True,exist_ok=False);shutil.copyfile(seed,destination/'level.dat');world_seed={'source':str(seed),'sha256':hashlib.sha256(seed.read_bytes()).hexdigest(),'copied_regions_or_player_files':False}
        # The harness already selects four chunks once in-world. Apply the same
        # view distance before login, avoiding a transient 12-chunk request at
        # the unrelated seed position. This disposable config never touches
        # the user's options or reduces any story collision/input/audio gate.
        # A standalone hearing/collision check needs no distant world rendering.
        # Keep the same server simulation and every gameplay/safety gate.
        draw=2 if args.connected_story or args.rescue_web or args.family_game_night or args.story_rebirth else 4
        (cwd/'options.txt').write_text(f'version:3465\nrenderDistance:{draw}\nsimulationDistance:5\npauseOnLostFocus:false\n'+('maxFps:30\n' if args.rescue_web or args.family_game_night or args.nautical_opening or args.story_rebirth else ''),encoding='utf-8')
    if args.kind=='client':
        # A full-source QA output also contains the generic venue fixture.
        # Disable its explicit cached launcher flag for specialized runs, or it
        # would build a second scene concurrently with the requested harness.
        cmd=['-Dcoraline.visitEventsVisualCheck=false' if item=='-Dcoraline.visitEventsVisualCheck=true' else item for item in cmd]
environment['JAVA_TOOL_OPTIONS']=f'-Xms32m -Xmx{args.heap_mib}m -XX:+UseSerialGC -XX:ActiveProcessorCount=4 -XX:MaxMetaspaceSize=192m -XX:ReservedCodeCacheSize=64m -XX:MaxDirectMemorySize=128m'
if args.connected_story:
    # Release unused loading heap between two-client checkpoints. This changes
    # neither maximum heap nor the admission/job/emergency memory guards.
    environment['JAVA_TOOL_OPTIONS']+=' -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20'
# QA-only resource root; neither this config nor its mixin is packaged.
# GameTests load their actual fixture chunks, without unrelated spawn preloads.
environment['MOD_CLASSES']=environment.get('MOD_CLASSES','')+os.pathsep+'coraline%%'+str(root/'src/rendercheck/resources')
environment['JAVA_TOOL_OPTIONS']+=' -Dcoraline.leanVisualWorld=true'
if args.rescue_rig:environment['JAVA_TOOL_OPTIONS']+=' -Dcoraline.rescueRigVisualCheck=true'
if args.story_audio:environment['JAVA_TOOL_OPTIONS']+=' -Dcoraline.storyAudioPlaybackCheck=true'
if args.rescue_web:environment['JAVA_TOOL_OPTIONS']+=' -Dcoraline.rescueWebVisualCheck=true'
if args.connected_story:environment['JAVA_TOOL_OPTIONS']+=' -Dcoraline.connectedStoryCheck=true'
if args.connected_transactions:environment['JAVA_TOOL_OPTIONS']+=' -Dcoraline.connectedTransactionsCheck=true'
if args.seeing_stone_music:environment['JAVA_TOOL_OPTIONS']+=' -Dcoraline.seeingStoneMusicCheck=true'
if args.family_game_night:environment['JAVA_TOOL_OPTIONS']+=' -Dcoraline.familyGameNightNaturalCheck=true'
if args.nautical_opening:environment['JAVA_TOOL_OPTIONS']+=' -Dcoraline.theatreAerialCheck=true' if args.theatre_aerial else ' -Dcoraline.nauticalOpeningCheck=true'
if args.story_rebirth:environment['JAVA_TOOL_OPTIONS']+=' -Dcoraline.storyRebirthCheck=true'
if args.accepted_ghost:environment['JAVA_TOOL_OPTIONS']+=' -Dcoraline.acceptedGhostCheck=true'
if args.tycoon_phase_one:environment['JAVA_TOOL_OPTIONS']+=' -Dcoraline.tycoonPhaseOneCheck=true'
if args.tycoon_phase_two:environment['JAVA_TOOL_OPTIONS']+=' -Dcoraline.tycoonPhaseTwoCheck=true'
if args.tycoon_expansion:environment['JAVA_TOOL_OPTIONS']+=' -Dcoraline.tycoonExpansionCheck=true'
if args.resume_tycoon_two_report:environment['JAVA_TOOL_OPTIONS']+=' -Dcoraline.tycoonPhaseTwoResume=true'
if args.tycoon_food:environment['JAVA_TOOL_OPTIONS']+=' -Dcoraline.tycoonFoodCheck=true'
cmd+=['-mixin.config','coraline.qa.mixins.json']
if args.first_escape:
    if args.kind!='client':raise RuntimeError('First-escape visual QA is client-only')
    environment['JAVA_TOOL_OPTIONS']+= ' -Dcoraline.firstEscapeGrabCheck=true' if args.grab_regression else ' -Dcoraline.seamlessTunnelVisualCheck=true' if args.portal_aperture else ' -Dcoraline.firstEscapeVisualCheck=true'
    if args.insect_parlor:environment['JAVA_TOOL_OPTIONS']+=' -Dcoraline.insectParlorVisualCheck=true'
    if args.rescue_attic:environment['JAVA_TOOL_OPTIONS']+=' -Dcoraline.rescueAtticVisualCheck=true'
    if args.rescue_finale:environment['JAVA_TOOL_OPTIONS']+=' -Dcoraline.rescueFinaleVisualCheck=true'
    if args.resume_finale_report:environment['JAVA_TOOL_OPTIONS']+=' -Dcoraline.rescueFinaleResume=true'
    if args.resume_without_theatre:environment['JAVA_TOOL_OPTIONS']+=' -Dcoraline.rescueFinaleNoTheatre=true'
ready_floor=max(monitor.START_FREE_GIB,args.minimum_start_free_mib/1024)
# Preparation hashes/stages many files. Release only this launcher's clean
# mappings before measuring the unchanged physical-memory start requirement.
from low_memory_voice_loader import trim_own_mapped_pages
trim_own_mapped_pages()
if monitor.free_gib()<ready_floor and args.memory_wait_seconds:
    print(f'Waiting at most {args.memory_wait_seconds}s for {args.minimum_start_free_mib} MiB free before QA launch; safety floors unchanged.',flush=True)
    deadline=time.monotonic()+args.memory_wait_seconds
    while monitor.free_gib()<ready_floor and time.monotonic()<deadline:time.sleep(1)
if monitor.free_gib()<ready_floor:raise RuntimeError('Insufficient available memory for the selected QA start floor; game not started')
if args.missing_parents_tea:
    if args.kind!='client':raise RuntimeError('Tea visual QA is client-only')
    environment['JAVA_TOOL_OPTIONS']+=' -Dcoraline.missingParentsTeaVisualCheck=true'
if args.missing_parents_cat:
    if args.kind!='client':raise RuntimeError('Cat visual QA is client-only')
    environment['JAVA_TOOL_OPTIONS']+=' -Dcoraline.missingParentsCatVisualCheck=true'
output=root/'build-character-cast/prepared-native'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime());output.mkdir(parents=True,exist_ok=False)
job_limit=3072 if args.kind=='client' else args.heap_mib+768
report={'kind':args.kind,'selected_test_classes':args.test_classes,'manifest_sha256':hashlib.sha256(raw).hexdigest(),'command':cmd,'cwd':str(cwd),'compiler_exited_before_game':True,'native_vm_defaults':environment['JAVA_TOOL_OPTIONS'],'aggregate_job_limit_mib':job_limit,'start_free_gib':monitor.free_gib(),'prepared_resource_preflight':prepared_resources}
if compiled:report['isolated_compiler_report']=str(supplied)
if connected_class_report:report['connected_class_staging_report']=connected_class_report
if world_seed:report['disposable_world_seed']=world_seed
if expected_tests is not None:report['expected_compiled_game_tests']=expected_tests
print('Native runtime report: '+str(output/'report.json'),flush=True)
api,handle=job(job_limit)
try:
    with (output/'output.log').open('xb',buffering=0) as stream:
        child=subprocess.Popen(cmd,cwd=cwd,env=environment,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,bufsize=0)
        report['pid']=child.pid
        (output/'report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
        if not api.AssignProcessToJobObject(handle,wintypes.HANDLE(int(child._handle))):
            child.kill();child.wait();raise ctypes.WinError(ctypes.get_last_error())
        reached,errors=threading.Event(),[]
        reader=threading.Thread(target=monitor.drain_bounded,args=(child.stdout,stream,monitor.MAX_LOG_BYTES,reached,errors),daemon=True);reader.start()
        report.update(monitor.watch_child(child,args.timeout_seconds,monitor.free_gib,monitor.process_memory,log_limit=reached))
        reader.join(timeout=10)
        if reader.is_alive() or errors or reached.is_set():report['status']='output_capture_failed'
    transcript=(output/'output.log').read_text(encoding='utf-8',errors='replace')
    assertions=re.search(r'All ([1-9]\d*) required tests passed',transcript) if args.kind=='server' else None
    if assertions:report['observed_assertions_passed']=int(assertions.group(1))
    # A completion line before a memory cutoff is evidence, not a clean run.
    report['clean_runtime_shutdown']=report.get('status')=='passed' and report.get('exit_code')==0
    if report.get('status')=='passed':
        completion=re.search(r'All ([1-9]\d*) required tests passed',transcript) if args.kind=='server' else ('RESCUE_WEB_CLIENT_NATIVE_COMPLETE' if args.rescue_web else 'RESCUE_RIG_NATIVE_CAPTURED' if args.rescue_rig else 'MISSING_PARENTS_CAT_NATIVE_COMPLETE' if args.missing_parents_cat else 'MISSING_PARENTS_TEA_NATIVE_COMPLETE' if args.missing_parents_tea else 'INSECT_PARLOR_NATIVE_CAPTURED' if args.insect_parlor else 'FIRST_ESCAPE_NATIVE_CAPTURED' if args.first_escape else 'FAMILY_GAMES_NATIVE_CAPTURED') in transcript
        if args.story_audio:completion='STORY_AUDIO_MIXER_NATIVE_COMPLETE' in transcript
        if args.connected_story:completion='CONNECTED_STORY_NATIVE_COMPLETE' in transcript
        if args.connected_transactions:completion=completion and 'CONNECTED_TRANSACTIONS_NATIVE_COMPLETE' in transcript
        if args.seeing_stone_music:completion='SEEING_STONE_MUSIC_NATIVE_COMPLETE' in transcript
        if args.family_game_night:completion='FAMILY_GAME_NIGHT_NATURAL_NATIVE_COMPLETE' in transcript
        if args.nautical_opening:completion='NAUTICAL_OPENING_NATIVE_COMPLETE' in transcript
        if args.theatre_aerial:completion='THEATRE_AERIAL_NATIVE_COMPLETE' in transcript
        if args.story_rebirth:completion='STORY_REBIRTH_NATIVE_COMPLETE' in transcript
        if args.accepted_ghost:completion='ACCEPTED_GHOST_NATIVE_COMPLETE' in transcript
        if args.tycoon_phase_one:completion='TYCOON_PHASE_ONE_NATIVE_COMPLETE' in transcript
        if args.tycoon_phase_two:completion=('TYCOON_EXPANSION_NATIVE_COMPLETE' if args.tycoon_expansion else 'TYCOON_FOOD_NATIVE_COMPLETE' if args.tycoon_food else 'TYCOON_PHASE_TWO_RESUME_COMPLETE' if args.resume_tycoon_two_report else 'TYCOON_PHASE_TWO_NATIVE_COMPLETE') in transcript
        if args.grab_regression:completion='FIRST_ESCAPE_GRAB_NATIVE_COMPLETE' in transcript
        if args.portal_aperture:completion='SEAMLESS_TUNNEL_NATIVE_OK' in transcript
        if args.rescue_finale:completion='RESCUE_FINALE_NATIVE_COMPLETE' in transcript
        if not completion:report.update(status='native_acceptance_failed',reason='No actual QA completion marker')
        elif args.rescue_rig and 'CELESTIAL_SKY_NATIVE_CAPTURED' not in transcript:report.update(status='native_acceptance_failed',reason='Celestial environment capture did not complete')
        elif args.rescue_rig and 'RESCUE_HAND_POSES_NATIVE_CAPTURED' not in transcript:report.update(status='native_acceptance_failed',reason='Original and final mother hand-pose regression/capture did not complete')
        elif args.rescue_rig and 'RESCUE_REUNION_FROST_NATIVE_CAPTURED' not in transcript:report.update(status='native_acceptance_failed',reason='Original parent wardrobe frost/melt regression/capture did not complete')
        elif args.rescue_rig and 'FIRST_ESCAPE_GEOMETRY_NATIVE_VERIFIED' not in transcript:report.update(status='native_acceptance_failed',reason='Exact cached ellipsoid vertex/normal regression did not complete')
        elif args.rescue_attic and 'RESCUE_ATTIC_NATIVE_CAPTURED' not in transcript:report.update(status='native_acceptance_failed',reason='Actual attic furnace/ceiling capture did not complete')
        elif args.rescue_finale and 'RESCUE_FINALE_NATIVE_COMPLETE' not in transcript:report.update(status='native_acceptance_failed',reason='Connected finale/reunion/farewell checkpoint playthrough did not complete')
        elif args.kind=='server':
            report['required_tests_passed']=int(completion.group(1))
            if expected_tests is not None and report['required_tests_passed']!=expected_tests:report.update(status='native_acceptance_failed',reason='Discovered native test count did not match all compiled test methods')
finally:
    api.CloseHandle(handle)
    (output/'report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(monitor.tail_bytes(output/'output.log'),flush=True)
print('Report: '+str(output/'report.json'),flush=True)
raise SystemExit(0 if report.get('status')=='passed' and report.get('exit_code')==0 else 1)
