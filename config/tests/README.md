# Test configurations

One `spawn.strategy` variable (plus whatever it's specifically testing) per
file, matching the scenario table in [`MANUAL_TESTING.md`](../../MANUAL_TESTING.md).
Every field not mentioned in a file falls back to Worldz's documented default
— that's deliberate, so each file only shows what's actually under test.

## How to use one

1. Copy the file you want over the active config, renaming it to
   `jlt_worldz.yaml`:

   ```bash
   cp config/tests/03-preferred-natural-biome-found.yaml \
      fabric/runs/client/config/jlt_worldz.yaml
   ```

   (Dev client. For the dev server use `fabric/runs/server/config/`. For a
   real installed instance, use that instance's `config/` folder instead —
   e.g. wherever your launcher points `.minecraft/config/`.)

2. **Delete the old world save first if one exists** —
   `fabric/runs/client/saves/<world-name>/`. Worldz settings are baked into
   a world at creation and never re-read afterward, so testing a new config
   against an old save just re-tests the old config.

3. Launch (`./gradlew :fabric:runClient` for the dev client) and create a
   new world using the shipped `jlt_worldz` world preset.

4. Worldz rewrites the file back with full defaults and `_docs` filled in
   after first load — that's expected, not a sign the test config was wrong.

## Files

| File | Tests |
|---|---|
| `01-starter-at-origin.yaml` | Default strategy: starter zone forced and spawn placed at `(0,0)`. |
| `02-vanilla-spawn.yaml` | `vanilla_spawn` with a starter biome still configured — layout origin and starter zone stay at `(0,0)` even though the player may spawn elsewhere. |
| `03-preferred-natural-biome-found.yaml` | `preferred_natural_biome` with a common starter biome (`plains`) it should actually find nearby, relocating the origin. |
| `04-preferred-natural-biome-no-starter-biome.yaml` | Same strategy with `starterBiome: ''` — expect the "needs a starter biome" warning and a fallback to `starter_at_origin` at `(0,0)`. |
| `05-preferred-natural-biome-unfindable.yaml` | Same strategy with a starter biome unlikely to exist within the search radius (`mushroom_fields`) — expect the "search found no biome" warning and the same fallback. |
| `06-preferred-natural-biome-recentering.yaml` | Combines a found preferred biome with an enabled border, an ocean exterior, and a `mixed` layout — confirms border/exterior/layout all recenter on the *found* origin together, not just the spawn point. |
| `07-legacy-regression-baseline.yaml` | Everything at documented defaults except an explicit `legacy` layout mode and a plain starter zone — a control run to compare every other file against. |
| `08-land-only.yaml` | `layout.mode: land_only` — guarantees land almost everywhere (only clearly-deep ocean gets raised), natural rivers/ponds left alone. Fixes the "ocean mapped as river" mismatch from testing `01` under legacy mode. |
| `09-mixed-natural-oceans-and-rivers.yaml` | `layout.mode: mixed` — real, coherent land *and* ocean *and* rivers together: biome label and terrain height always agree, unlike legacy mode. |

### Why `01` showed ocean labeled as river

Config `01` uses `layout.mode: legacy` (the default — every field a test
file doesn't mention falls back to it), which only filters vanilla's
climate map to the allowed-biome list; it never reconciles the chosen
label with actual terrain height/shape. The default `allowedBiomes` list
has no ocean biome at all, so wherever the underlying vanilla terrain was
genuinely oceanic, the climate filter substituted the *closest allowed
biome in climate space* instead — which turned out to be `river`. This is
documented in the main [README.md](../../README.md#current-terrain-composition-limitation-legacy-mode-only)
as a known legacy-mode limitation, not a bug specific to this session's
work — `08` and `09` both resolve it by using a non-`legacy` layout mode,
which decides biome and height together instead of filtering climate alone.

Use a fixed seed (the example files don't set one — the world-creation
screen's seed field is where you pin it) so repeat runs of the same file are
actually comparable.
