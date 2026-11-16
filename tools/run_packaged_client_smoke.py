"""Offline production Forge launch in a disposable workspace game directory.

Reads installed launcher version metadata/libraries, never accounts or profiles.
Loads only the supplied reobfuscated mod JAR, not MOD_CLASSES/dev mappings.
Stops only its own child after resources finish loading and a quiet grace period.
"""
from pathlib import Path
import argparse
import ctypes
from ctypes import wintypes
import hashlib
import json
import os
import re
import shutil
import subprocess
import time
import zipfile
import uuid
import check_numbered_candidate as monitor
from run_low_memory_release import job


def allowed(entry):
    rules = entry.get('rules')
    if not rules:
        return True
    result = False
    for rule in rules:
        system = rule.get('os', {})
        if system.get('name', 'windows') != 'windows':
            continue
        if system.get('arch', 'x86_64') not in ('amd64', 'x86_64'):
            continue
        if 'version' in system and not re.search(system['version'], '10.0'):
            continue
        if any(rule.get('features', {}).values()):
            continue
        result = rule['action'] == 'allow'
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('jar', type=Path)
    parser.add_argument('--run', action='store_true')
    parser.add_argument('--interactive', action='store_true', help='Keep a bounded disposable client available for actual UI acceptance; readiness alone is not a pass.')
    parser.add_argument('--profile-name', default='PackagedQA', help='Synthetic offline QA identity, never an installed user account.')
    parser.add_argument('--connect', help='Interactive QA only: loopback server 127.0.0.1:port.')
    args = parser.parse_args()
    if not re.fullmatch(r'[A-Za-z0-9_]{1,16}', args.profile_name):
        raise ValueError('Expected a synthetic 1-16 character QA profile name')
    if args.connect and (not args.interactive or not re.fullmatch(r'127\.0\.0\.1:[1-9][0-9]{0,4}', args.connect) or int(args.connect.split(':')[1])>65535):
        raise ValueError('Connected QA is restricted to a loopback server')
    source = args.jar.resolve(strict=True)
    root = Path(__file__).resolve().parents[1]
    installed = Path(os.environ['APPDATA']) / '.minecraft'
    version = '1.20.1-forge-47.4.10'
    forge = json.loads((installed / 'versions' / version / (version + '.json')).read_text())
    base_id = forge['inheritsFrom']
    base = json.loads((installed / 'versions' / base_id / (base_id + '.json')).read_text())
    directory = root / 'build-character-cast/packaged-client' / time.strftime('%Y%m%dT%H%M%SZ', time.gmtime())
    directory.mkdir(parents=True, exist_ok=False)
    if args.interactive:
        (directory/'options.txt').write_text('version:3465\nrenderDistance:4\nsimulationDistance:5\npauseOnLostFocus:false\nmaxFps:30\nonboardAccessibility:false\n',encoding='utf-8')
    (directory / 'mods').mkdir()
    shutil.copy2(source, directory / 'mods' / source.name)
    natives = directory / 'natives'
    natives.mkdir()
    libs = {}
    for entry in base['libraries'] + forge['libraries']:
        if allowed(entry):
            name = entry['name'].split(':')
            libs[tuple(name[:2] + name[3:])] = entry
    classpath = []
    for entry in libs.values():
        artifact = entry.get('downloads', {}).get('artifact')
        if artifact:
            path = installed / 'libraries' / artifact['path']
        else:
            group, name, ver, *classifier = entry['name'].split(':')
            suffix = '-' + classifier[0] if classifier else ''
            path = installed / 'libraries' / group.replace('.', '/') / name / ver / f'{name}-{ver}{suffix}.jar'
        if not path.is_file():
            raise FileNotFoundError(path)
        classpath.append(str(path))
        if 'natives-windows' in path.name:
            with zipfile.ZipFile(path) as archive:
                for member in archive.namelist():
                    if member.lower().endswith('.dll'):
                        with archive.open(member) as native_source, (natives / Path(member).name).open('wb') as native_target:
                            shutil.copyfileobj(native_source,native_target,length=64*1024)
    classpath.append(str(installed / 'versions' / version / (version + '.jar')))
    tokens = {
        'library_directory': str(installed / 'libraries'), 'classpath_separator': ';',
        'version_name': version, 'natives_directory': str(natives),
        'launcher_name': 'CoralinePackagedQA', 'launcher_version': '1',
        'classpath': ';'.join(classpath), 'auth_player_name': args.profile_name,
        'game_directory': str(directory), 'assets_root': str(installed / 'assets'),
        'assets_index_name': base['assetIndex']['id'],
        'auth_uuid': uuid.UUID(bytes=hashlib.md5(('OfflinePlayer:'+args.profile_name).encode('utf-8')).digest(),version=3).hex, 'auth_access_token': '0',
        'clientid': 'PackagedQA', 'auth_xuid': '0',
        'user_type': 'legacy', 'version_type': 'release',
    }
    def expand(items):
        output = []
        for item in items:
            if isinstance(item, dict):
                if not allowed(item):
                    continue
                item = item['value']
            for value in item if isinstance(item, list) else [item]:
                value = re.sub(r'\$\{([^}]+)\}', lambda match: tokens[match[1]], value)
                output.append(value)
        return output
    command = [str(monitor.JAVA / 'java.exe'), '-Xms64m', '-Xmx1024m',
               '-XX:+UseSerialGC', '-XX:ActiveProcessorCount=4',
               *expand(base['arguments']['jvm'] + forge['arguments']['jvm']),
               forge['mainClass'], *expand(base['arguments']['game'] + forge['arguments']['game'])]
    if args.connect:
        command += ['--quickPlayMultiplayer',args.connect]
    with source.open('rb') as jar_source:
        jar_sha256=hashlib.file_digest(jar_source,'sha256').hexdigest()
    report = {'jar': str(source), 'sha256': jar_sha256,
              'forge': version, 'command': command, 'game_directory': str(directory),
              'production_jar_only': True, 'status': 'prepared', 'interactive_qa':args.interactive,
              'profile_name':args.profile_name,'connection':args.connect,'scope':'Resource startup only' if not args.interactive else 'Interactive runtime; no automatic gameplay acceptance'}
    report_path = directory / 'report.json'
    report_path.write_text(json.dumps(report, indent=2))
    print('Packaged client report: ' + str(report_path), flush=True)
    if not args.run:
        return 0
    floor=3 if args.interactive else 2
    if monitor.free_gib() < floor:
        raise RuntimeError(f'Client QA requires {floor} GiB free before launch')
    api, handle = job(3072)
    child = None
    try:
        log = directory / 'output.log'
        with log.open('wb') as output:
            child = subprocess.Popen(command, cwd=directory, env=monitor.child_environment(),
                                     stdout=output, stderr=subprocess.STDOUT)
            if not api.AssignProcessToJobObject(handle, wintypes.HANDLE(int(child._handle))):
                child.kill()
                raise ctypes.WinError(ctypes.get_last_error())
            report['pid'] = child.pid
            report['process_started_epoch'] = time.time()
            report_path.write_text(json.dumps(report, indent=2),encoding='utf-8')
            ready_since = None
            deadline = time.monotonic() + (1800 if args.interactive else 240)
            while child.poll() is None and time.monotonic() < deadline:
                if log.stat().st_size>monitor.MAX_LOG_BYTES:
                    report['status']='output_limit_stopped'
                    break
                text = log.read_text(errors='replace')
                if 'FATAL' in text or 'Mixin apply failed' in text or 'Mod loading error' in text:
                    report['status'] = 'startup_failed'
                    break
                if 'minecraft:textures/atlas/mob_effects.png' in text and 'Sound engine started' in text:
                    ready_since = ready_since or time.monotonic()
                    if time.monotonic() - ready_since >= 15:
                        report['status'] = 'interactive_ready' if args.interactive else 'startup_resources_loaded'
                        if not args.interactive:break
                if monitor.free_gib() < monitor.STOP_FREE_GIB:
                    report['status'] = 'memory_guard_stopped'
                    break
                time.sleep(0.5)
            if args.interactive and report['status']=='interactive_ready':
                report['status']='interactive_exited' if child.poll()==0 else 'interactive_early_exit' if child.poll() is not None else 'timeout'
            elif report['status'] == 'prepared':
                report['status'] = 'early_exit' if child.poll() is not None else 'timeout'
            report['natural_exit_code'] = child.poll()
    finally:
        # Job contains only this QA launch, never the user's launcher/game.
        api.CloseHandle(handle)
        if child:
            child.wait(timeout=15)
        report_path.write_text(json.dumps(report, indent=2))
    print(report['status'], flush=True)
    return 0 if report['status'] in ('startup_resources_loaded','interactive_exited') else 1


if __name__ == '__main__':
    raise SystemExit(main())
