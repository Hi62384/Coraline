"""Narrow correction audit for a clean finale rejected solely for an irrelevant preview marker.

Preserves the original report/log. Never turns assertions, memory cutoffs or crashes into passes.
"""
from pathlib import Path
import argparse,hashlib,json,re

root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser();parser.add_argument('report');args=parser.parse_args()
path=(root/args.report).resolve();allowed=(root/'build-character-cast/prepared-native').resolve()
if not path.is_relative_to(allowed) or path.name!='report.json' or not re.fullmatch(r'\d{8}T\d{6}Z',path.parent.name):raise RuntimeError('Unexpected native QA report')
raw=path.read_bytes();report=json.loads(raw);log_path=path.parent/'output.log';log_raw=log_path.read_bytes();log=log_raw.decode('utf8',errors='replace')
if report.get('kind')!='client' or report.get('status')!='native_acceptance_failed' or report.get('reason')!='No actual QA completion marker' or report.get('exit_code')!=0 or report.get('clean_runtime_shutdown') is not True:raise RuntimeError('Not the specific clean-exit preview-marker reporting defect')
flags=report.get('native_vm_defaults','')
if '-Dcoraline.rescueFinaleVisualCheck=true' not in flags or '-Dcoraline.rescueFinaleResume=true' not in flags or not report.get('disposable_world_seed',{}).get('copied_regions_or_player_files'):raise RuntimeError('Not a validated stopped-world resumed finale')
markers=re.findall(r'^RESCUE_FINALE_NATIVE_COMPLETE .+$',log,re.MULTILINE)
if len(markers)!=1 or 'RESCUE_FINALE_NATIVE_RESUME loaded original saved identities' not in log or 'RESCUE_FINALE_NATIVE_FAILED' in log or re.search(r'Server thread/ERROR.*Error executing task on Server',log):raise RuntimeError('Missing final invariant marker or actual QA failure present')
compiler=Path(report['isolated_compiler_report']).resolve()
if not compiler.is_relative_to(root/'build-character-cast/cat-focused-compile'):raise RuntimeError('Unexpected original compiler report')
compiled=json.loads(compiler.read_text())
if compiled.get('status')!='passed' or compiled.get('exit_code')!=0 or compiled.get('full_source_compile') is not True:raise RuntimeError('Original successful full-source compiler report missing')
audit={'status':'passed_resumed_finale_completion_audit','scope':'Resumed original-identity reunion/farewell and final invariants only; NOT fresh A-Q/web/crawl, bosses, uninterrupted story, audible or performance acceptance','reason':'Original runner incorrectly required initial insect-parlor preview capture on an explicit finale resume; corrected to require the more specific final invariant marker','original_report_preserved':True,'original_report_sha256':hashlib.sha256(raw).hexdigest(),'original_log_sha256':hashlib.sha256(log_raw).hexdigest(),'original_compiler_report_sha256':hashlib.sha256(compiler.read_bytes()).hexdigest(),'clean_exit_code':0,'min_free_gib':report.get('min_free_gib'),'final_marker':markers[0]}
target=path.parent/'completion-audit.json';target.write_text(json.dumps(audit,indent=2)+'\n',encoding='utf8');print(json.dumps(audit,indent=2));print('Audit: '+str(target))
