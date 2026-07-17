# Manual test strategy

`./gradlew build` proves the code compiles and the pure logic is correct; it
never launches Minecraft. Every phase to date has deferred actual in-game
verification, so this is the first pass at a repeatable process for doing
that in a controlled way — start here, and extend the scenario tables below
as new features land.

## Principles

- **One variable at a time.** Change exactly one config value (or one
  Customize-screen field) between runs. If two things differ between a
  passing and failing run, you haven't isolated anything.
- **Fresh world per scenario.** Worldz decisions (spawn strategy, layout
  origin, border, exterior) are baked in at world creation and never
  re-evaluated. Reusing a world across scenarios silently tests stale state.
  Delete the world folder (or rename it aside) before each new scenario.
- **Fixed seed for anything you want to compare or repeat.** A blank/random
  seed makes two runs of "the same" scenario incomparable. Pick one seed (e.g.
  `12345`) and reuse it for every scenario in a session unless a test is
  specifically about seed independence.
- **Both loaders.** Fabric and NeoForge share `common` but wire events
  differently (`ServerLevelEvents.LOAD` + a mixin on Fabric,
  `LevelEvent.Load`/`CreateSpawnPosition` on NeoForge). A bug in one loader's
  wiring won't show up on the other. Run the matrix on both unless a scenario
  is explicitly loader-agnostic.
- **Read the log, not just the screen.** Worldz logs a `WARN` for every
  fallback (unresolved starter biome, search radius exhausted, invalid
  config value, etc.) via SLF4J. A fallback that "looks fine" in-game but
  logged a warning is still a bug worth noticing.

## Environment

Dev run directories (created by Loom / NeoGradle, gitignored):

| Loader | Client | Server |
|---|---|---|
| Fabric | `fabric/runs/client/` | `fabric/runs/server/` |
| NeoForge | `neoforge/runs/client/` | `neoforge/runs/server/` |

Launch with:

```bash
./gradlew :fabric:runClient      # or :neoforge:runClient
./gradlew :fabric:runServer      # or :neoforge:runServer
```

Config file per run directory: `<rundir>/config/jlt_worldz.yaml`. Edit it
directly, or delete it to regenerate documented defaults on next launch.
Worlds live in `<rundir>/saves/<world-name>/` (client) — delete the folder for
a clean slate. A dedicated server's single world is `<rundir>/world/`.

Log file: `<rundir>/logs/latest.log`. Grep for the mod's warnings:

```bash
grep -i "worldz\|preferred_natural_biome\|starter_at_origin" <rundir>/logs/latest.log
```

F3 debug screen gives you: block position, biome id, and (F3+G / vanilla
chunk borders) the current world seed if unsure — all needed to verify
recentering and strategy behavior below.

## Scenario table: seed-informed spawn (Phase 16)

This is the current unverified feature. Run each row on **both** loaders
(12 runs total) with a fixed seed and `starterBiome: minecraft:plains`,
`starterRadiusBlocks: 256` unless noted.

| # | `spawn.strategy` | Setup | Expected result |
|---|---|---|---|
| 1 | `starter_at_origin` | defaults | Spawn lands inside the plains starter zone at/near `(0,0)`. F3 shows biome `plains` at spawn. |
| 2 | `vanilla_spawn` | defaults | Spawn is wherever vanilla's own search would put it for that seed (biome is whatever the allowed-biome filter reports there — not necessarily plains). Layout origin stays `(0,0)`: if a border is also enabled (see below), it's still centered at `(0,0)`. |
| 3 | `preferred_natural_biome` | `starterBiome: minecraft:plains` | Spawn is offset from `(0,0)` (unless a natural plains patch happens to already be there) at a natural, unmodified plains biome. Note the exact spawn coordinates. |
| 4 | `preferred_natural_biome` | `starterBiome: ''` (empty) | Log shows the "needs a starter biome" warning; behaves exactly like `starter_at_origin` (spawn at/near `(0,0)`, no starter zone since no starter biome). |
| 5 | `preferred_natural_biome` | pick a seed/biome combination unlikely to exist within 2048 blocks (e.g. `starterBiome: minecraft:mushroom_fields`) | Log shows the "search found no biome within N blocks" warning; falls back to spawning at `(0,0)` as `starter_at_origin` would. |

### Recentering verification (row 3 above, repeat with these enabled)

With `preferred_natural_biome` actually finding and relocating the origin
(row 3), additionally enable each of the following **one at a time** and
confirm the feature is centered on the *new* origin, not `(0,0)`:

- `overworldBorder.enabled: true` — walk toward `(0,0)` from spawn; the
  border should NOT be there. It should be centered on the found origin
  (use F3 to compare your position against the border-hit coordinates).
- `overworldExterior.mode: ocean` (with a boundary) — same check: the
  ocean/void transition should be centered on the new origin, not `(0,0)`.
- `overworldBorder.ensureObjective: true` — locate the End portal (`/locate
  structure` is fine for this, it's a placement check not a spoiler) and
  confirm it's reachable relative to the new origin's border, not `(0,0)`'s.
- A non-`legacy` `layout.mode` (e.g. `ocean`) — confirm the beach ring around
  the starter zone is centered on the new origin, not `(0,0)`.

### Persistence across restarts

1. Create a world with `preferred_natural_biome` (row 3), note the found
   origin coordinates from the log (`SpawnOriginState` write) or by walking
   back to the starter zone.
2. Fully stop the client/server and relaunch, load the same world.
3. Confirm: the log does **not** show a new search running, and the starter
   zone / border / exterior are still centered on the *same* origin as
   before (not re-rolled, not reset to `(0,0)`).

## Regression checks

- **Existing pre-Phase-16 worlds.** If you have a world saved before this
  session's work (or create one, then check out an older commit temporarily
  to generate one — do not do this in a way that discards uncommitted work),
  confirm it still loads and plays exactly as before: no spawn strategy
  field existed in its data, so it must decode as `starter_at_origin`
  with origin `(0,0)`.
- **`legacy` layout mode still works** with each spawn strategy — Phase 16
  only touches origin/spawn, not the layout algorithm itself.
- **Dedicated server parity.** Repeat at least one row of the matrix via
  `runServer` + a separate vanilla client connecting to it, since
  `LevelEvent.CreateSpawnPosition`/the Fabric mixin only fire server-side —
  a client-only smoke test would miss a server-side wiring bug entirely.

## Terrain sanity check (flagged risk, MEMORY.md "Known Risks")

Not a pass/fail test, but worth a look while you're in-game: `MEMORY.md`
flags that `EnvelopedChunkGenerator` may cause Minecraft to build terrain
from a dummy zero-density `RandomState` (flat, structureless noise) rather
than real vanilla shaping, for every Worldz world, unrelated to Phase 16.
While testing any scenario above, glance at terrain well outside the starter
zone/layout-controlled area — if hills, ravines, and ordinary vanilla terrain
variation are visibly present, that's evidence against the flagged risk; if
terrain looks suspiciously flat or noise-free, that's worth flagging back for
investigation before trusting anything else about generated terrain shape.

## Recording results

Keep it simple — a checklist per session is enough, e.g.:

```
2026-07-16, Fabric, seed 12345
[x] Row 1 starter_at_origin — spawn in plains at ~(4,~,8), OK
[x] Row 2 vanilla_spawn — spawn at (312,~,-198) biome=forest, border still at (0,0), OK
[ ] Row 3 preferred_natural_biome — spawn at (???) — BUG: border followed spawn but exterior didn't
```

File bugs (or just note them in conversation) with: loader, strategy, seed,
config snippet, expected vs. actual, and the relevant `latest.log` lines.
