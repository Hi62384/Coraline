"""Reject incomplete generated resource caches before a disposable native launch.

Read-only preflight, not an optimizer or a substitute for processResources.
Interrupted Gradle copy tasks can leave an old asset-size report beside an empty
or partial output tree; the report's mere existence is not sufficient evidence.
"""
from pathlib import Path
import hashlib
import json


def verify(resources: Path, report_path: Path):
    report = json.loads(report_path.read_text(encoding='utf8'))
    expected = report.get('after_bytes')
    if type(expected) is not int or expected <= 0:
        raise RuntimeError('Missing normal optimizer byte-count receipt')
    required = ('coraline.mixins.json', 'coraline.party.mixins.json',
                'coraline.refmap.json', 'META-INF/mods.toml', 'pack.mcmeta',
                'assets/coraline/sounds.json', 'assets/coraline/visit_audio_lengths.json',
                'assets/coraline/visit_voice_envelopes.json')
    hashes = {}
    for name in required:
        path = resources / name
        if path.is_symlink() or not path.is_file() or path.stat().st_size == 0:
            raise RuntimeError('Incomplete prepared runtime resource: ' + name + '; rerun normal processResources')
        data = path.read_bytes()
        if name.endswith('.json') or name.endswith('.mcmeta'):
            if not isinstance(json.loads(data), dict):
                raise RuntimeError('Malformed prepared runtime resource: ' + name)
        hashes[name] = hashlib.sha256(data).hexdigest()
    files = list(resources.rglob('*'))
    if any(path.is_symlink() for path in files):
        raise RuntimeError('Linked runtime resource refused')
    actual = sum(path.stat().st_size for path in files if path.is_file())
    if actual != expected:
        raise RuntimeError('Prepared runtime differs from normal optimizer receipt; rerun processResources')
    return {'normal_optimizer_bytes': actual, 'critical_resource_sha256': hashes}
