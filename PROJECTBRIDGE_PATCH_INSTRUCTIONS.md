# REQUIRED: ProjectBridge Offline Patch Delivery Instructions

**Repository:** `Hi62384/Coraline`  
**Applier:** ProjectBridge Offline Patcher (the Windows offline app)  
**ZIP format:** `projectbridge.offline-patch.v1`

> **MANDATORY PRE-FLIGHT FOR CHATGPT:** Before making any changes to this project or promising a downloadable fix, read this file **in full** and read the repository-root `AGENTS.md`. Apply both documents. The user should only need to give a feature request, not identify individual files or assemble project ZIPs.

## 1. Required workflow

1. Access the latest relevant code from `https://github.com/Hi62384/Coraline` **read-only**, preferably with the GitHub connector. Inspect the current `main` branch (or explicitly requested ref), repository structure, dependencies and related files; do not stop at the first matching filename. Read the root `AGENTS.md` and this entire document before editing.
2. Record the **exact source commit SHA** you used as the baseline. If GitHub code is stale relative to the user's local copy, say so. **Never silently overwrite local modifications.**
3. Implement the request in a temporary working directory, preserving repository-relative paths. Examine usages/references and affected configs/assets. Keep changes targeted but complete; create new files or delete obsolete files when needed. Follow existing Gradle/asset optimization rules in `AGENTS.md`.
4. Obtain the **actual original bytes** of every modified or deleted file from the recorded GitHub commit. This is essential: GitHub's blob SHA-1 is **not** the SHA-256 required by the patcher. Preserve raw contents and compute SHA-256 from bytes; don't guess hashes or hash a rendered/web-formatted excerpt. If exact original bytes are unavailable, **ask for a narrow baseline file** instead of building an unverified patch.
5. Build **one downloadable ZIP named `ProjectBridge_patch.zip`**, using the v1 manifest below. Include *only* files that changed, files that are new, and deletion operations. Do not package the full repository, ignored build outputs, or unrelated assets.
6. Validate the manifest/ZIP against the ProjectBridge Offline Patcher v1 parser and, when possible, validate against a temporary checkout of the exact baseline. Run meaningful tests if the available environment permits. **Never claim a build, runtime, gameplay, or visual test passed unless it was actually performed.**
7. Reply with a download link to the ZIP and a brief, honest summary: updated paths, effects, tests that did/didn't run, known limitations, and any additional manual steps. The user imports the ZIP into the 100%-offline app, reviews it, and clicks **Apply verified changes**; the app backs up originals.

**No direct push, merge, or modification to the GitHub repository is required for this workflow.** A GitHub write must not replace ZIP delivery unless the user explicitly asks for it.

## 2. Exact ZIP layout

The ZIP root must contain ONLY these entries (no enclosing project folder):

```text
ProjectBridge_patch.zip
├── projectbridge_patch.json       # REQUIRED; UTF-8 JSON
├── README_CHANGES.txt             # OPTIONAL; human-readable summary
├── files/0001                     # byte-for-byte replacement for first write
├── files/0002                     # next write payload
└── files/0003                     # and so on
```

The `files/NNNN` entry names are payload identifiers, **not** destination paths. The JSON manifest maps each payload to an exact path in the user's selected project folder. Do not add empty directory entries or unreferenced files.

### Example `projectbridge_patch.json`

```json
{
  "format": "projectbridge.offline-patch.v1",
  "title": "Fix activity behavior",
  "description": "Improve activity logic and related registration",
  "source": {
    "repository": "Hi62384/Coraline",
    "commit": "ACTUAL_SOURCE_COMMIT_SHA"
  },
  "changes": [
    {
      "action": "write",
      "path": "src/main/java/com/example/ExistingActivity.java",
      "before_sha256": "SHA256_OF_EXACT_EXISTING_FILE_BYTES",
      "after_sha256": "SHA256_OF_EXACT_FILES_0001_PAYLOAD_BYTES",
      "payload": "files/0001",
      "summary": "Fix activity completion logic"
    },
    {
      "action": "write",
      "path": "src/main/java/com/example/NewActivity.java",
      "before_sha256": null,
      "after_sha256": "SHA256_OF_EXACT_FILES_0002_PAYLOAD_BYTES",
      "payload": "files/0002",
      "summary": "Add the new activity implementation"
    },
    {
      "action": "delete",
      "path": "src/main/java/com/example/UnusedActivity.java",
      "before_sha256": "SHA256_OF_EXACT_OLD_FILE_BYTES",
      "summary": "Remove obsolete activity code"
    }
  ]
}
```

**Placeholder hashes and paths above are illustrative only.** The delivered JSON **must** contain real, lowercase, 64-character hexadecimal SHA-256 hashes computed using `hashlib.sha256(original_bytes).hexdigest()` and actual repository paths. The example `com/example` package is not a claim about this project's real package structure.

## 3. Required operation rules

| Operation | `action` | `before_sha256` | `after_sha256` | `payload` |
|---|---|---|---|---|
| Modify existing file | `write` | SHA-256 of GitHub baseline bytes | SHA-256 of complete edited bytes | unique `files/NNNN` |
| Create new file | `write` | `null` | SHA-256 of complete new bytes | unique `files/NNNN` |
| Delete existing file | `delete` | SHA-256 of GitHub baseline bytes | omit | omit |
| Rename/move | one `delete` at old path + one `write` at new path | hashes as above | as applicable | as applicable |

- A `write` payload is the **entire resulting file content**, not a diff or a fragment. Bytes in the ZIP must hash to `after_sha256`.
- All `path` values are **relative to the repository/project root**, with `/` separators. Never include a drive letter, leading slash, backslash, `.`/`..` path components, symlink targets, or paths outside the project.
- Never modify `.git/`, `.gradle/`, `.venv/`, `.projectbridge/`, `.projectbridge_backups/`, or secret/credential files (`.env`, keys, tokens, keystores, etc.). Follow the applier's stricter path-validation rules where applicable.
- Do not include unchanged files. Do not specify the same target twice, or reuse a payload entry for multiple operations.
- Do not invent an original hash if the local file differs. The app intentionally rejects conflicts. Ask for the affected local files or an updated GitHub commit when necessary.
- The app recognizes *some* pure UTF-8 LF-vs-CRLF differences for files up to 8 MiB and preserves Windows CRLF, but do not assume it will accept other local changes or mixed endings.
- One patch supports at most **1,500 operations**, **512 MiB per file**, and **2 GiB total uncompressed contents**. Large projects (40–50+ GiB) are supported because **only affected files are transferred**.
- Store descriptions in `description` and per-file `summary`, plus optionally `README_CHANGES.txt`.

## 4. Recommended patch builder

If the ProjectBridge Offline Patcher's `make_patch.py` and `patchcore.py` are available in the working environment, **use them rather than hand-authoring JSON**. The builder computes hashes and produces ZIP members automatically.

Prepare two small temporary folders containing **only affected paths**:

```text
github_baseline/                     # ORIGINAL bytes for existing modified/deleted files
  src/main/java/.../ExistingActivity.java
  src/main/java/.../UnusedActivity.java
edited_files/                         # FINAL bytes for new/modified files only
  src/main/java/.../ExistingActivity.java
  src/main/java/.../NewActivity.java
```

Then run (with actual paths, title, repo and full commit SHA):

```bash
python make_patch.py \
  --before github_baseline \
  --edits edited_files \
  --out ProjectBridge_patch.zip \
  --title "Fix activity behavior" \
  --description "Improve activity logic and registration" \
  --repo Hi62384/Coraline \
  --commit ACTUAL_SOURCE_COMMIT_SHA \
  --delete src/main/java/com/example/UnusedActivity.java
```

If those helper scripts are unavailable, Python's standard `hashlib`, `json`, and `zipfile` can create the exact same v1 format; validate all required fields and payload hashes yourself. Do **not** silently invent a different ZIP format.

## 5. Verification before returning ZIP

- The archive opens successfully, uses the exact `projectbridge.offline-patch.v1` format string, and contains `projectbridge_patch.json` at ZIP root.
- The archive contains only the manifest, optional README, and `files/NNNN` write payloads referenced by operations.
- Every `before_sha256` matches the exact GitHub baseline bytes used for the operation. Every `after_sha256` matches its payload bytes. File creation uses `null` only when the file truly does not exist in the baseline.
- Every destination path is project-relative and passes the offline patcher's path safety constraints.
- Validate against the *same version* of the actual offline applier, and if possible apply+undo on a disposable baseline copy. Don't test the patch by modifying the user's real project.
- If there are no file changes, **do not create an empty patch ZIP**. Explain why.
- Report tests honestly; compilation or gameplay may require the user's own PC, installed SDKs, game runtime and assets.

## 6. Future-chat mandatory opening prompt

A normal ChatGPT conversation is **not guaranteed to automatically open repository instruction files**. To ensure this document is actually read at the start of a new chat, begin the request with:

> **Before doing anything else, open `https://github.com/Hi62384/Coraline/blob/main/AGENTS.md` and `https://github.com/Hi62384/Coraline/blob/main/PROJECTBRIDGE_PATCH_INSTRUCTIONS.md`, read both in full, and follow the offline patch ZIP specification. Read-only GitHub; return `ProjectBridge_patch.zip` compatible with my offline applier. Then implement: [YOUR REQUEST].**

These instructions are the repository's required workflow for assistants that have read them. The repository files themselves cannot override ChatGPT's system settings or guarantee that a new conversation reads them unprompted.
