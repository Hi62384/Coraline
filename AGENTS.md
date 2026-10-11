# Required workflow for ChatGPT and coding assistants

**Before investigating, editing, or preparing a deliverable for this repository, read the full root file [`PROJECTBRIDGE_PATCH_INSTRUCTIONS.md`](PROJECTBRIDGE_PATCH_INSTRUCTIONS.md).** Treat it as the mandatory packaging and handoff procedure for work on `Hi62384/Coraline`. Read this `AGENTS.md` first, then the linked document, even when the user supplies only a feature request. If that document cannot be accessed, ask for it; do not improvise a patch ZIP.

For normal ChatGPT conversations, the standard delivery method is **a downloadable `ProjectBridge_patch.zip` compatible with ProjectBridge Offline Patcher**, not a full project archive, a bare code listing, or a direct GitHub push. GitHub is **read-only** for the patch workflow unless the user explicitly authorizes a different action. Read current repository sources, find all relevant files yourself, make the edits, and package only changed/new/deleted files with correct exact-byte SHA-256 baselines. Validate the ZIP before sharing it.

This repository convention does not force ChatGPT to fetch files automatically; the assistant must be given repository access and should explicitly load these instructions at the start of each new conversation.

---

# Runtime size and optimization

- Keep release size small. Inspect the JAR's largest entries when adding assets; do not remove features to meet a size target.
- Keep high-resolution artwork, source recordings and Blender projects outside packaged runtime resources where practical. Preserve masters; never repeatedly downsample/re-encode a previous optimized export.
- Runtime inventory icons target 256 px maximum. Other textures normally stay at or below 512 px; existing smaller pixel-art textures must not be upscaled. Preserve alpha and aspect ratio. Animation strips need explicit frame-aware handling.
- `processResources` runs `tools/optimize_runtime_assets.py` on generated copies only. Do not bypass this for releases. Inspect `build[-activity]/reports/asset-size.json`; 64 MiB of compressed runtime assets triggers a review warning, not permission to delete content. `check` runs the optimizer regression tests.
- Reuse identical recordings through `sounds.json` references. Keep sound event IDs, subtitle/timing metadata and playback settings intact. Avoid unnecessary lossy audio recompression and do not truncate dialogue or music.
- Export music at at most 48 kHz; do not ship 96/192 kHz masters. The build resamples oversized high-rate recordings while preserving source files; normal-rate audio is not re-encoded.
- Audio importers must merge shared manifests, never reset other importers' duration entries. Validate every registered VisitAudio event has timing metadata; missing intro timings previously crashed activity entry. The runtime asset step reconstructs the complete timing table from packaged recordings.
- Verify referenced assets still exist, compare before/after release sizes, run optimizer tests and compile before delivering. State any visual/audio QA limitations.
- With concurrent builds use `--project-cache-dir .gradle-activity -PisolatedActivityBuild` to isolate both output and Gradle state.
