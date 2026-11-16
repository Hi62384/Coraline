<!-- ProjectBridge delivery-mode policy v2 -->
## Important: Codex edits directly; offline ZIPs are opt-in

**Codex and other agents with write access to this checkout MUST edit the actual files directly, run appropriate tests, and report normal code changes. DO NOT generate a `ProjectBridge_patch.zip` just because it is mentioned in this repository.** For normal ChatGPT reading GitHub without a writable checkout, use a ProjectBridge ZIP for coding tasks by default (unless the user requests another output). For Codex or writable coding agents, use the ZIP workflow only if the user explicitly asks. The detailed `PROJECTBRIDGE_PATCH_INSTRUCTIONS.md` is a reference **only for that optional ZIP workflow**. This newer mode-selection rule overrides any older, unconditional ZIP-delivery instruction below.

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
