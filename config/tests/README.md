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

   (Dev client. For the dev server use `fabric/runs/server/config/`. For the
   dedicated Prism test instance, target `<instance>/minecraft/config/`
   instead — deploy a fresh jar there first with
   `./gradlew :fabric:deployToPrism`, see
   [`MANUAL_TESTING.md`](../../MANUAL_TESTING.md#testing-against-a-real-prism-instance-instead-of-the-dev-client).)

2. **Delete the old world save first if one exists** —
   `fabric/runs/client/saves/<world-name>/`. Worldz settings are baked into
   a world at creation and never re-read afterward, so testing a new config
   against an old save just re-tests the old config.

3. Launch (`./gradlew :fabric:runClient` for the dev client) and create a
   new world. Since Phase 2, the World Type dropdown has **three** Worldz
   entries -- pick the one the table below tells you to: "Worldz" (files
   `01`-`09`, `13`) reads the flat top-level config fields; "Worldz: Single
   Biome" (files `10`-`12`, `14`-`15`) reads only the `singleBiome:`
   section; "Worldz: Chaos Biomes" (files `16`-`19`) reads only the
   `chaosBiomes:` section. Each ignores every other section.

4. Worldz rewrites the file back with every field canonicalized and filled in
   after first load — that's expected, not a sign the test config was wrong.

## Files

| File | Tests |
|---|---|
| `01-starter-at-origin.yaml` | Default strategy: starter zone forced and spawn placed at `(0,0)`. |
| `02-vanilla-spawn.yaml` | `vanilla_spawn` with a starter biome still configured — layout origin and starter zone stay at `(0,0)` even though the player may spawn elsewhere. |
| `03-preferred-natural-biome-found.yaml` | `preferred_natural_biome` with a common starter biome (`plains`) it should actually find nearby, relocating the origin. |
| `04-preferred-natural-biome-no-starter-biome.yaml` | Same strategy with `starterBiome: ''` — expect the "needs a starter biome" warning and a fallback to `starter_at_origin` at `(0,0)`. |
| `05-preferred-natural-biome-unfindable.yaml` | Same strategy with a starter biome unlikely to exist within the search radius (`mushroom_fields`) — expect the "search found no biome" warning and the same fallback. |
| `06-preferred-natural-biome-recentering.yaml` | Combines a found preferred biome with an enabled border, an ocean exterior, and an `ocean` layout — confirms border/exterior/layout all recenter on the *found* origin together, not just the spawn point. |
| `07-legacy-regression-baseline.yaml` | Everything at documented defaults except an explicit `legacy` layout mode and a plain starter zone — a control run to compare every other file against. |
| `08-single-biome-regression.yaml` | `layout.mode: single_biome` (`desert`) — confirms this surviving mode still works after the 2026-07-16 `MIXED`/`LAND_ONLY` removal and real-seed change. |
| `09-void-regression.yaml` | `layout.mode: void` with starter land enabled — confirms the sky-void starter island still works after the same changes. |
| `10-single-biome-basic.yaml` | GOALS 10, the `jlt_worldz:single_biome` typed preset: one land biome fills the world, no starter-zone override. **Select "Worldz: Single Biome" on the creation screen**, not plain "Worldz". |
| `11-single-biome-different-starter.yaml` | GOALS 11: same as 10, but a different biome is forced around spawn. |
| `12-single-biome-seed-chosen-starter.yaml` | GOALS 12: same as 11, but the starter biome's location is seed-chosen (`preferred_natural_biome`) instead of forced at `(0,0)`. |
| `13-vanilla-limited-baseline.yaml` | Control run for Phase 2: plain "Worldz" preset, legacy layout, a modest border — confirms the shared border/limit path Phase 2 didn't touch still works through the `LimitedBiomeSource.resolve()` branch it now shares with `singleBiome:`. |
| `14-single-biome-allow-rivers.yaml` | GOALS 13: `allowRivers: true` — vanilla's own rivers pass through wherever vanilla would naturally place one, everywhere else stays desert. |
| `15-single-biome-allow-oceans.yaml` | GOALS 14: `allowRivers: true` + `allowOceans: true` — vanilla's own rivers and oceans (every depth/temperature) both pass through; coastlines should look exactly like natural vanilla terrain, no straight edges. |
| `16-chaos-biomes-default.yaml` | GOALS 33, default `regionScaleBlocks` (512) and biome list (desert/jungle/ice_spikes/badlands/taiga) — confirms regions of different biomes appear over completely untouched vanilla terrain (hills/valleys unaffected). `allowRivers`/`allowOceans` are off, so natural water bodies are relabeled with a land biome instead of showing as water biomes — expected here, not a bug. |
| `17-chaos-biomes-pass-through-and-starter.yaml` | GOALS 33's rivers/oceans option (`allowRivers`/`allowOceans: true`, same Phase 3.1 mechanism as `singleBiome`'s) plus a `starterBiome: minecraft:plains` safe zone at spawn instead of chaos starting immediately. Test this before 18/19 — it confirms real oceans/rivers survive under chaos biomes before moving on to region-size variations. |
| `18-chaos-biomes-tiny-regions.yaml` | Same as 16 but `regionScaleBlocks: 64` — biome should change much more often while traveling. |
| `19-chaos-biomes-huge-regions.yaml` | Same as 16 but `regionScaleBlocks: 4096` — expect long uninterrupted stretches of one biome. |
| `20-border-static-small-invisible-wall-vs-void.yaml` | GOALS 17/18, plain "Worldz" preset: a small (128) static border (invisible wall) with a void exterior at the same auto-derived boundary — confirms the two are the same distinct systems, plus the small-border End-portal fallback guarantee. |
| `21-border-expanding.yaml` | GOALS 19: border grows from 128 to 1024 over 2 days after a 1-day initial delay. Use `/tick step` to skip ahead — see the file's own steps. |
| `22-border-collapsing.yaml` | GOALS 20: border shrinks from 2048 to a 256 minimum over 3 days after a deliberately much larger 5-day delay (exploration time); spawn stays centered so it's always safe. |
| `23-end-border-carry-over.yaml` | GOALS 17: Overworld border pinned to the 64-block minimum, `endBorder.carryFromOverworld: true` with `minimumRadiusBlocks: 256` — confirms the floor overrides the tiny carried radius so the End border is 256, not 64, keeping the dragon fight winnable. |

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
work — `06` resolves it by using a non-`legacy` layout mode, which decides
biome and height together instead of filtering climate alone.

Use a fixed seed (the example files don't set one — the world-creation
screen's seed field is where you pin it) so repeat runs of the same file are
actually comparable.
