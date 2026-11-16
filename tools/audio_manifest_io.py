"""Safe, deterministic publication of already-merged audio metadata."""
import json
from pathlib import Path
import tempfile


def write_json_if_changed(path, value):
    """Avoid truncating shared manifests or rewriting unchanged content."""
    encoded=(json.dumps(value,indent=2)+'\n').encode('utf-8')
    if path.exists() and path.read_bytes()==encoded:return
    staged=None
    try:
        with tempfile.NamedTemporaryFile(dir=path.parent,prefix=path.name+'.',suffix='.tmp',delete=False) as output:
            staged=Path(output.name)
            output.write(encoded)
        staged.replace(path)
    finally:
        if staged is not None and staged.exists():staged.unlink()
