# Other World navigation and vivid colors — 1.11.1

- Saturation defaults to **2.15**, up from 1.45. The new client setting is `palette.facadeSaturation` in `coraline-visuals-client.toml`. The old key is superseded so existing installations receive the change. A value of 1 disables the color pass. This remains Other-World-only.
- First-visit travel stages show a short silver particle trail along a navigable route. Searches are bounded to 128 blocks and run at most once every two seconds per quest owner; at most 18 guide particles are emitted per update.
- `/coraline guide` moves the owner to a collision-checked approach to the current travel objective. It does not skip dialogue or advance stages. It is especially useful for cave homes or blocked routes.
- Travel objectives now recover automatically after three failed route checks, spaced five seconds apart. A separate thirty-second movement watchdog detects pacing without escaping a small area. Standing idle alone does not trigger the movement watchdog. Recovery finds a supported, collision-free landing near the current quest without changing blocks, rewards or story progress. No rescue door is created; old rescue props retire automatically. `/coraline guide` remains a manual quest-only fallback, not a teleport-home command.
- Furniture placement tests reachability from the door arrival position rather than whichever position the mother spawned at. If the distant piano cannot fit, it tries a closer location.
- If a cramped cave or house cannot fit the scene, an empty-air-only fenced terrace is created above the copied terrain. The guide provides access; this is a recovery fallback, not a destructive stairway through the player's house. No copied blocks or containers are replaced. If world-height constraints prevent that fallback, the scene reports the obstruction and retries.
- Arrival searches now include floors below the door instead of unexpectedly sending cave visitors to the surface.
- Parent death resets first-visit completion, elapsed scene state, cake-gift state, first-time audio milestones and the family's realm records. Old scene props cannot rejoin a new family after chunk reload. Online visitors are safely returned before retirement. Existing earned inventory items are not confiscated. Legacy completed records whose old door was revoked are repaired on entry.

## Verification

The navigation regression run passed 94 of 96 tests, including all three new cave/furniture/guide tests. Two failures remain: the parent work-delivery timing test and the previously failing full supper-sequence chair-count fixture. This is not an all-green regression run. The dimension-only color pass and escape from a locked supper seat while preserving story progress were checked in a live disposable client world.

The separate five-test family-reset suite passed: saved family identity, offline-owner reset/multiplayer isolation, replacement-family handling, legacy revoked-door migration, and stale unloaded prop retirement.

## Separate armor work

The user-approved illustrated Midnight Coat reference and an unfinished curved-mesh prototype are in `artwork/sculpted_wardrobe/`. They are not release armor replacements. The 87-piece detailed armor redesign is still unfinished; 1.11.1 contains the existing 1.11 wardrobe.
