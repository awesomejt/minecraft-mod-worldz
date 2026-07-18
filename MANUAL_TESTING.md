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

Config file per run directory: `<rundir>/config/jlt_worldz.yaml`. It is
optional — absent means the mod's built-in defaults apply directly, and the
mod never creates the file for you (see
[`config/jlt_worldz.example.yaml`](config/jlt_worldz.example.yaml) for the
documented reference to copy from). Worlds live in
`<rundir>/saves/<world-name>/` (client) — delete the folder for a clean
slate. A dedicated server's single world is `<rundir>/world/`.

### Testing against a real Prism instance instead of the dev client

Jason tests Fabric against a dedicated Prism Launcher instance rather than
the dev client (it carries the recommended test-aid mods — Xaero's
world/minimap, Chunky, Spark; MiniHUD is disabled since `jlt_info` covers
the same ground). Deploy a freshly built jar to it with:

```bash
./gradlew :fabric:deployToPrism
```

This copies the built Fabric jar into the instance's `mods/` folder,
replacing any older `jlt_worldz` jar there. It reads the instance path from
`prism.instance.dir` in a git-ignored `local.properties` file at the repo
root (create one if missing — never committed), or accept it ad hoc with
`-PprismInstanceDir=<path>`. Config file and worlds/logs for that instance
live under `<instance>/minecraft/` (Prism's own layout), following the same
"config is optional, delete the world for a clean slate" rules as above.

Log file: `<rundir>/logs/latest.log`. Grep for the mod's warnings:

```bash
grep -i "worldz\|preferred_natural_biome\|starter_at_origin" <rundir>/logs/latest.log
```

F3 debug screen gives you: block position, biome id, and (F3+G / vanilla
chunk borders) the current world seed if unsure — all needed to verify
recentering and strategy behavior below.

## Phase 1 acceptance (2026-07-16 challenge-world replan, TODO 1.1/1.7)

Everything below is unverified in-game as of 0.2.0. Use the dedicated Prism
instance (see above) with a fixed seed (e.g. `12345`) and a fresh world per
scenario, per the principles above. None of this needs the dev client unless
you prefer it.

### 1. Dummy-RandomState fix (TODO 1.1)

Shipped in 0.1.15, carried into 0.2.0, never confirmed in-game. This is the
most important check in this pass — it affects the terrain shape of every
Worldz world regardless of layout mode.

1. Create a world with any config (e.g. `07-legacy-regression-baseline.yaml`,
   or no config file at all for pure defaults).
2. Go to spectator mode and drop below Y-64, well outside the starter zone.
3. **Pass:** ordinary bedrock at the bottom, and normal winding cave systems
   (not huge open gaps, not near-total lava). This is the exact failure
   pattern described in `MEMORY.md`'s dummy-RandomState entry — compare
   against that description if anything looks off.
4. Repeat step 1-3 briefly on **NeoForge** (`./gradlew :neoforge:runClient`
   or a NeoForge Prism instance if you set one up) — this is the first
   NeoForge-side mixin this project has shipped, so it needs its own check,
   not just an assumption that "Fabric passed so NeoForge did too."
5. While you're down there: do the Worldz14 orange/glitchy-terrain screenshots
   reproduce? They were never explained and are plausibly the same root
   cause. Note yes/no either way — a "no" closes out the question, not just
   a "not tested."

### 2. Removed-mode regression + default generation (TODO 1.7)

1. **Default-config world.** No `jlt_worldz.yaml` present (delete it if the
   instance already has one) — confirms defaults apply without a file at
   all (TODO 1.4). Create a world, confirm normal cave/structure generation,
   and open **Customize → Layout**: confirm the mode cycle button only ever
   shows `legacy` / `ocean` / `single_biome` / `void` — never `land_only` or
   `mixed`.
2. **Single-biome-style world**, using
   `08-single-biome-regression.yaml` (`layout.mode: single_biome`, desert).
   Confirm the whole world is desert-shaped terrain (raised to the land
   role), normal structures/caves still generate, and the same seed
   reproduces the same result on a second world (TODO 1.3's real-seed
   change).
3. **Void mode**, using `09-void-regression.yaml`. Confirm a solid starter
   island floats in sky void, sized to the starter radius plus its land
   transition.
4. **Ocean mode / recentering**, using
   `06-preferred-natural-biome-recentering.yaml` (now `ocean` instead of the
   removed `mixed`). Confirm border/exterior/layout all still recenter on
   the found origin together (see the existing recentering checklist below)
   — this exercises the exact code path Phase 1.2/1.3 touched.
5. **Legacy baseline**, using `07-legacy-regression-baseline.yaml`, as a
   known-good control to compare the above against.

## Phase 2 acceptance (2026-07-16 challenge-world replan, TODO 2.7)

Uses configs `10`-`13` (see [`config/tests/README.md`](config/tests/README.md)).
**Configs `10`-`12` need the new "Worldz: Single Biome" entry in the World
Type dropdown** — a separate preset from plain "Worldz", with its own small
Customize screen (land biome / starter biome / starter radius / spawn
strategy only; no border/exterior/starter-land controls here yet — those
stay YAML-only for this type until Phase 5.3). Config `13` uses plain
"Worldz" as a control. Fixed seed recommended for all four.

1. **Basic single biome (GOALS 10)**, `10-single-biome-basic.yaml`. Create a
   world, confirm the entire explored area is desert (F3 biome id), normal
   structures/caves/features generate (compare against the Phase 1.1
   dummy-RandomState check — same underlying terrain pipeline), and no
   starter-zone seam is visible anywhere (there is none to see: the whole
   world is already the land biome).
2. **Different starter biome (GOALS 11)**, `11-single-biome-different-starter.yaml`.
   Confirm spawn is inside a ~256-block plains circle, desert begins beyond
   it, and the transition is blended (starter land guarantee), not a hard
   cliff.
3. **Seed-chosen starter (GOALS 12)**, `12-single-biome-seed-chosen-starter.yaml`.
   Confirm spawn lands inside a *naturally occurring* plains patch found via
   search (not forced at `(0,0)`) — note the coordinates. Recreate the world
   with the same seed and confirm the same location is found again (real-seed
   reproducibility, TODO 1.3). If no natural plains patch exists within the
   search radius for your seed, confirm the documented fallback: spawn at
   `(0,0)` behaving like `starter_at_origin`, with a "search found no biome"
   warning in the log.
4. **Vanilla-limited control**, `13-vanilla-limited-baseline.yaml`. Confirm
   this looks and feels like ordinary vanilla Minecraft (climate-filtered
   biomes, no single-biome uniformity) ringed off by a 1000-block border —
   this is the regression guard for the shared border/limit path, which now
   flows through the same `LimitedBiomeSource.resolve()` method as the new
   `singleBiome:` branch.
5. **Per-world snapshot (TODO 2.4).** After any of the above, check the new
   world's save folder for `jlt_worldz-snapshot.yaml` (sibling to
   `level.dat`). Confirm it exists, is readable, and its values match what
   you actually selected — this file is a reference record only; editing it
   does nothing.
6. **Customize-screen sanity.** Open "Worldz: Single Biome"'s Customize
   screen before creating a world: confirm it shows only land biome/starter
   biome/starter radius/spawn strategy — none of the full "Worldz" preset's
   allowed-biomes list, border, exterior, or layout-mode controls.

## Phase 3 acceptance (2026-07-17 challenge-world replan, TODO 3.2)

Uses configs `14`-`15` (see [`config/tests/README.md`](config/tests/README.md)).
Both need **"Worldz: Single Biome"** on the creation screen. A fixed seed
with a known nearby river and ocean makes this much easier to check —
[Chunkbase's biome finder](https://www.chunkbase.com/apps/biome-finder) (or
similar) against the same seed, checked in plain vanilla first, gives exact
coordinates to fly to before checking the same spot in Worldz.

1. **Rivers pass through (GOALS 13)**, `14-single-biome-allow-rivers.yaml`.
   Locate a river in plain vanilla at this seed, note its coordinates, then
   create the Worldz world at the same seed and fly there: confirm a river
   (F3 biome id `minecraft:river` or `minecraft:frozen_river`) actually
   exists in the same place, with the same natural channel shape — not the
   configured desert. Everywhere else (and outside the river) should still
   be desert. Confirm the starter zone (within ~256 blocks of spawn) is
   never a river even if vanilla would have put one there — starter land
   stays guaranteed.
2. **Oceans pass through too (GOALS 14)**, `15-single-biome-allow-oceans.yaml`.
   Same idea at an ocean location instead: confirm the ocean (any of
   vanilla's ocean biome variants — warm/lukewarm/cold/frozen, deep or not)
   generates naturally, with an unmodified vanilla coastline — no straight
   edges, no visible height blending at the shore. Rivers should still pass
   through too (`allowRivers` stays on alongside `allowOceans`).
3. **Per-world snapshot.** Check `jlt_worldz-snapshot.yaml` for either world:
   confirm it now includes `allowRivers`/`allowOceans` matching what the
   config file set.

## Phase 4 acceptance (2026-07-18 challenge-world replan, TODO 4.2)

Uses configs `16`-`19` (see [`config/tests/README.md`](config/tests/README.md)).
All four need **"Worldz: Chaos Biomes"** on the creation screen.

1. **Default regions (GOALS 33)**, `16-chaos-biomes-default.yaml`. Fly
   around and confirm: (a) multiple different biomes appear (desert,
   jungle, ice_spikes, badlands, taiga) in distinct regions, not one biome
   everywhere; (b) terrain shape is completely ordinary vanilla — hills,
   mountains, ravines, natural water bodies all look exactly like a normal
   vanilla world, just with an unusual biome label on top (e.g. a jungle
   region can still have a natural lake or a snowy-looking valley if
   that's what the terrain naturally does there — the biome doesn't match
   the terrain the way vanilla normally correlates them, and that's
   expected for this config since rivers/oceans pass-through is off); (c)
   normal structures/caves/features generate.
2. **Tiny regions**, `17-chaos-biomes-tiny-regions.yaml`. Confirm biome
   changes noticeably more often while flying in a straight line than
   config 16 — regions should feel small and frequent.
3. **Huge regions**, `18-chaos-biomes-huge-regions.yaml`. Confirm the
   opposite — long, uninterrupted stretches of one biome before it changes.
4. **Pass-through + starter zone**, `19-chaos-biomes-pass-through-and-starter.yaml`.
   Confirm spawn is inside a ~256-block plains circle (the starter zone),
   with chaos biomes starting beyond it. Using the same vanilla-first
   coordinate-finding approach as Phase 3's rivers/oceans check, confirm a
   known vanilla river or ocean location (outside the starter zone) still
   generates as a real river/ocean here too, with natural terrain — not
   flattened, not replaced by one of the configured land biomes (same
   mechanism as Phase 3.1, just confirming it composes correctly with
   `chaos_biomes`).
5. **Customize-screen sanity.** Open "Worldz: Chaos Biomes"'s Customize
   screen before creating a world: confirm it shows the weighted biomes
   list, region size, starter biome/radius, spawn strategy, and
   allowRivers/allowOceans checkboxes — nothing from the full "Worldz"
   preset's allowed-biomes list, border, exterior, or layout-mode controls.

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

## Terrain sanity check (superseded by "Phase 1 acceptance" above)

The dummy-RandomState fix this section used to flag as an open risk shipped
in 0.1.15; see "Phase 1 acceptance" above for the concrete pass/fail check.
Still worth a passing glance during any scenario below: terrain well outside
the starter zone/layout-controlled area should show ordinary hills, ravines,
and vanilla-typical variation, not suspiciously flat or noise-free ground.

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
