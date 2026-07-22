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
   new world. Since Phase 14, the World Type dropdown has **nine** Worldz
   entries -- pick the one the table below tells you to: "Worldz" (files
   `01`-`09`, `13`, `20`-`25`) reads the flat top-level config fields;
   "Worldz: Single Biome" (files `10`-`12`, `14`-`15`) reads only the
   `singleBiome:` section; "Worldz: Chaos Biomes" (files `16`-`19`) reads
   only the `chaosBiomes:` section; "Worldz: Strip World" (files `26`-`29`)
   reads the shared top-level `strip:` section (corridor width, same as
   any other Worldz preset) plus its own `stripWorld:` section (spawn
   strategy and the optional `bands:` biome-sequence subsection, GOALS 36);
   "Worldz: Ocean Island" (files `30`-`37`) reads only its own
   `oceanIsland:` section (GOALS 01, 02, 03, 04, 28, 31) plus the shared
   `overworldBorder`/`netherBorder`/`endBorder`/`netherExterior` sections
   if set. `oceanIsland.islandSource` (`artificial`/`natural`/
   `chest_boat`, Phase 8) picks between three ways of sourcing the land,
   and `oceanIsland.fluid` (`water`/`lava`/`none`, Phase 9) independently
   picks the exterior ocean's fluid -- both within one preset, not
   separate World Types. "Worldz: Sky Island" (files `38`-`48`, `57`-`58`,
   GOALS 05/06/07/08, Phase 10/11) reads only its own `skyIsland:` section
   -- a thin, fixed-thickness floating slab, void everywhere else;
   `skyIsland.chestTier` (`easy`/`medium`/`hard`) picks the
   necessities-chest difficulty, `skyIsland.applyToNether` mirrors the
   same shape into the Nether, `skyIsland.exclusionZoneEnabled`/
   `exclusionZoneRadiusBlocks` (2026-07-21 follow-up) pins the biome to
   `islandBiome` in a buffer beyond the island's own edge before the real
   seed's biome resumes, and `skyIsland.floatingIslands` (GOALS 07/08)
   adds scattered secondary islands beyond that buffer, including a
   `naturalBiome` option (2026-07-21 follow-up) that reads each scattered
   island's real seed biome instead of hash-picking from a pool.
   "Worldz: Sky Chunk" (files `49`-`52`, GOALS 09/37, Phase 12) reads only
   its own `chunkIsland:` section -- chunk-shaped islands cut from the
   seed's own natural chunks (real terrain untouched, only unselected
   chunks void); `chunkIsland.topOnly`/`topOnlyDepthBlocks` picks full-
   column vs. a depth cutoff for the starter island, and
   `chunkIsland.applyToNether`/`applyToEnd` mirrors the same grid into
   those dimensions too (the first preset to also wrap the End's own
   generator). "Worldz: Cave" (files `53`-`56`, GOALS 25/26, Phase 13)
   reads only its own `cave:` section -- the Overworld generates exactly
   as vanilla would (no biome restriction, no shape at all); the only
   change is where you spawn (a real natural underground cavity, searched
   out near `cave.spawnDepthY`) plus two independent options:
   `cave.sealedSurface` (a solid roof at `cave.sealedSurfaceY`, no sky
   access anywhere) and `cave.cavernEnabled` (a large carved cavern around
   spawn, `cave.cavernRadiusBlocks`/`cavernHeightBlocks`). `cave.chestEnabled`
   plus `cave.chestTier` (`easy`/`medium`/`hard`) optionally place a
   starter chest at spawn, unlike every other typed preset's chest (which
   is always on). "Worldz: Nether Start" (files `59`-`61`, GOALS 27,
   Phase 14) reads only its own `netherStart:` section -- the Overworld
   generates exactly as vanilla would, and you spawn in the Nether instead
   (a real safe pocket searched out near `netherStart.spawnY`, or a small
   carved capsule if none is found); `netherStart.chestTier`
   (`easy`/`medium`/`hard`, always on, unlike cave's optional one) picks
   the starter-chest difficulty -- easy hands over a full 10-obsidian
   portal frame plus flint and steel, hard gives no guaranteed obsidian at
   all. Each preset ignores every other type's dedicated section.

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
| `21-border-expanding.yaml` | GOALS 19, **continuous** style: border grows smoothly from a genuine 2-block start to 1024 over 88 days after a 2-day initial delay. Use `/time add`/`/tick step` to skip ahead — see the file's own steps. Requires 0.2.13+ (tiny starts) and 0.2.14+ (spawn no longer places you outside the border). |
| `22-border-collapsing.yaml` | GOALS 20, **continuous** style: border shrinks smoothly from 2048 to a 256 minimum over 40 days after a deliberately much larger 10-day delay (exploration time); spawn stays centered so it's always safe. |
| `23-end-border-carry-over.yaml` | GOALS 17: Overworld border pinned to the 64-block minimum, `endBorder.carryFromOverworld: true` with `minimumRadiusBlocks: 256` — confirms the floor overrides the tiny carried radius so the End border is 256, not 64, keeping the dragon fight winnable. |
| `24-border-stepped-expanding.yaml` | GOALS 19, **stepped** style (Phase 5b, DESIGN §21.1): border holds at 8 for a 2-day delay, then jumps abruptly by 1 block/day up to 1024 — snaps, not a creep. Requires 0.2.16+. |
| `25-border-stepped-collapsing.yaml` | GOALS 20, **stepped** style: border holds at 1024 for a 10-day delay, then jumps abruptly by 2 blocks/day down to a 32 minimum — snaps, not a creep; spawn stays centered so it's always safe. Requires 0.2.16+. |
| `26-strip-world-basic.yaml` | GOALS 32 (Phase 6.2): a narrow strip world — corridor width (Z axis, void beyond) is new strip machinery; corridor length (X axis) is the ordinary square border, unmodified. **Select "Worldz: Strip World"**, not plain "Worldz". Requires 0.2.22+. |
| `27-strip-world-narrow-fallback-portal.yaml` | GOALS 32: a deliberately narrow corridor (32-block width radius) exercising the fallback End-portal Z-candidate fix found during 6.1's spike — confirms the compact portal lands inside the corridor, not at a Z candidate the border's own (larger) radius would have wrongly allowed. **Select "Worldz: Strip World"**. |
| `28-strip-world-nether-corridor.yaml` | GOALS 32's optional Nether strip: `strip.applyToNether: true` mirrors the same corridor width into the Nether, with its own independent length border and a compact fallback blaze site. **Select "Worldz: Strip World"**. |
| `29-strip-world-biome-bands.yaml` | GOALS 36: `stripWorld.bands` walks an ordered biome sequence along the corridor's length, wrapping once exhausted; terrain stays ordinary vanilla shape throughout. Applies straight from "Create World", no Customize visit needed (fixed in 0.2.27 — see [`MEMORY.md`](../../MEMORY.md)). **Select "Worldz: Strip World"**. Requires 0.2.27+. |
| `30-ocean-island-default.yaml` | GOALS 01 (Phase 7.2): a default-sized (128-block radius) natural-looking ocean island — non-circular coastline, narrow beach/stony-shore ring, and a shallow-to-deep ocean gradient drawing from all nine vanilla ocean biomes further out. **Select "Worldz: Ocean Island"**. Requires 0.2.29+. |
| `31-ocean-island-tiny.yaml` | GOALS 01's "1 chunk" floor: an 8-block-radius island. Exercises the documented tiny-island trade-off — the compact fallback End portal consumes most of the surface, since its safety margin can never "fit" at this scale. **Select "Worldz: Ocean Island"**. |
| `32-ocean-island-huge.yaml` | GOALS 01's "huge" ceiling: a 4096-block-radius island, where the default 30% coastline perturbation should read as dramatic bays/headlands rather than a slightly-uneven circle. **Select "Worldz: Ocean Island"**. |
| `33-ocean-island-distant-natural-islands.yaml` | GOALS 04: `oceanIsland.exclusionZoneEnabled: true` at a (deliberately test-friendly, smaller-than-default) 512-block radius — island/ocean shaping releases beyond it and the seed's own natural terrain resumes. **Select "Worldz: Ocean Island"**. |
| `34-ocean-island-chest-boat.yaml` | GOALS 03 (Phase 8.1): `oceanIsland.islandSource: chest_boat` — no land anywhere, player starts on/near an oak chest boat at the origin with a custom (config-overridden) starter kit. **Select "Worldz: Ocean Island"**. Requires 0.2.37+. |
| `35-ocean-island-natural.yaml` | GOALS 02 (Phase 8.2): `oceanIsland.islandSource: natural` — searches the real seed for an isolated natural landmass instead of shaping one; real terrain shows through completely unmodified within `radiusBlocks`. Not guaranteed to find a candidate on every seed (documented, time-boxed search) — check the log either way. **Select "Worldz: Ocean Island"**. Requires 0.2.38+. |
| `36-ocean-island-lava.yaml` | GOALS 28 (Phase 9.2): `oceanIsland.fluid: lava` — same island shape/shore ring/gradient as config 30, but the endless ocean is lava. Check fire safety at the shore ring, strider/no-boat travel. **Select "Worldz: Ocean Island"**. Requires 0.2.41+. |
| `37-ocean-island-dry.yaml` | GOALS 31 (Phase 9.3): `oceanIsland.fluid: none` — same island shape/shore ring/gradient as config 30, but the ocean is a drained, exposed basin (no water). Structures/aquifers unaffected. The "harder" rivers/lakes-removal difficulty option is not implemented (documented, deferred). **Select "Worldz: Ocean Island"**. Requires 0.2.41+. |
| `38-sky-island-default.yaml` | GOALS 05 (Phase 10.2): a default-sized (16-block radius, medium tier) floating sky island — thin slab, void everywhere else at every Y level, no natural decoration anywhere. **Select "Worldz: Sky Island"**. Requires 0.2.45+. |
| `39-sky-island-tiny.yaml` | GOALS 05's "1 chunk" floor: an 8-block-radius island, specifically to check the known, deliberately deferred risk that the chest (placed at literal origin) and spawn (offset by the default 8 blocks) might separate on a very small island. **Select "Worldz: Sky Island"**. |
| `40-sky-island-huge.yaml` | GOALS 05's large end: a 256-block-radius, 16-block-thick island with a taller coastline perturbation, natural-looking from above. **Select "Worldz: Sky Island"**. |
| `41-sky-island-chest-easy-desert.yaml` | GOALS 05 (Phase 10.3): easy chest tier on a desert-family biome — exercises the biome-driven water-source item (a guaranteed bucket, since deserts never get rain). **Select "Worldz: Sky Island"**. |
| `42-sky-island-chest-hard.yaml` | GOALS 05 (Phase 10.3): hard chest tier on plains (not a dry family, so a cauldron instead of a bucket) — confirms the bare-essentials tier is still genuinely beatable. **Select "Worldz: Sky Island"**. |
| `43-sky-island-nether.yaml` | GOALS 06 (Phase 10.4): `skyIsland.applyToNether: true` mirrors the same shape into the Nether — fixed netherrack/basalt palette (no biome concept there), compact fallback blaze spawner. **Select "Worldz: Sky Island"**. Requires 0.2.45+. |
| `44-sky-island-floating-dense.yaml` | GOALS 08 (Phase 11.2): dense scattered-island scatter (small cells, high spawn chance, no exclusion zone) with biome variety on — most of the void beyond the starter island should hold nearby, visibly different-biome islands. **Select "Worldz: Sky Island"**. Requires 0.2.48+. |
| `45-sky-island-floating-sparse-exclusion-zone.yaml` | GOALS 07/08: sparse scattered-island scatter (large cells, low spawn chance) with a real 200-block exclusion-zone buffer — confirms the void buffer holds and islands genuinely require sustained bridging to reach. **Select "Worldz: Sky Island"**. Requires 0.2.48+. |
| `46-sky-island-floating-ore-deposits.yaml` | GOALS 08 (Phase 11.3): `floatingIslands.oreDepositsEnabled: true` in isolation — each scattered island should have exactly one embedded vanilla ore vein, no chest. **Select "Worldz: Sky Island"**. Requires 0.2.49+. |
| `47-sky-island-floating-loot-chests.yaml` | GOALS 08 (Phase 11.4): `floatingIslands.lootChestEnabled: true` in isolation with a custom `lootKit` — each scattered island should have exactly one filled chest, no ore. **Select "Worldz: Sky Island"**. Requires 0.2.50+. |
| `48-sky-island-floating-guaranteed-village.yaml` | GOALS 07 (Phase 11.5): a real vanilla village always exists on one specific scattered island beyond the exclusion zone (check the server log for its coordinates) — the flagship "guaranteed, not best-effort" village. **Select "Worldz: Sky Island"**. Requires 0.2.51+. |
| `57-sky-island-biome-exclusion-zone.yaml` | GOALS 05 (2026-07-21 follow-up, DESIGN §27.10): a bare starter island's new biome exclusion zone (64-block test radius) — confirms the biome stays pinned to the configured `islandBiome` out to the buffer, then switches to the real seed's own biome beyond it, with terrain (void) unaffected either way. **Select "Worldz: Sky Island"**. Requires 0.2.64+. |
| `58-sky-island-floating-natural-biome.yaml` | GOALS 08 (2026-07-21 follow-up, DESIGN §28.6): `floatingIslands.naturalBiome: true` — each scattered island reads the real seed's own biome instead of `biomeVariety`'s hash-picked pool (which stays on here to also confirm precedence), producing large, coherent biome regions instead of a checkerboard. **Select "Worldz: Sky Island"**. Requires 0.2.64+. |
| `49-sky-chunk-default.yaml` | GOALS 09 (Phase 12.2/12.3): default full-column chunk islands — a selected chunk's real vanilla terrain is untouched, every unselected chunk is void; a guaranteed portal-room stronghold and a forced amethyst geode each sit on their own reserved chunk (check the server log for coordinates). **Select "Worldz: Sky Chunk"**. Requires 0.2.54+. |
| `50-sky-chunk-top-only.yaml` | GOALS 09 (Phase 12.2): the top-only depth cutoff ("like 5 deep to ensure access to stone") — a real per-column cutoff measured from each column's own natural surface, not a flat world-absolute Y. **Select "Worldz: Sky Chunk"**. Requires 0.2.54+. |
| `51-sky-chunk-nether-end.yaml` | GOALS 09 (Phase 12.4): `applyToNether`/`applyToEnd` — the first Worldz preset to wrap the End's own generator at all; confirms the dragon fight stays winnable when the End is also chunk islands. **Select "Worldz: Sky Chunk"**. Requires 0.2.54+. |
| `52-sky-chunk-scattered-showcase.yaml` | GOALS 37 (Phase 12.5/12.6): per-island independent depth mode (`scatteredTopOnlyChance`) plus underground-content showcasing — seed-search-preferred cave-biome/structure chunks and the forced geode cell, both logged with coordinates. **Select "Worldz: Sky Chunk"**. Requires 0.2.58+. |
| `53-cave-default.yaml` | GOALS 25 (Phase 13.2a): default cave-only start — spawn is a real, searched-out natural underground cavity; no sealed surface, no mega-cavern, no chest. **Select "Worldz: Cave"**. Requires 0.2.60+. |
| `54-cave-sealed-surface.yaml` | GOALS 25 (Phase 13.2b): `sealedSurface: true` — a solid roof at Y 128 seals off sky access everywhere; confirms no way through without breaking it and no phantom spawns. **Select "Worldz: Cave"**. Requires 0.2.61+. |
| `55-cave-mega-cavern.yaml` | GOALS 26 (Phase 13.2c): `cavernEnabled: true` — a large, naturally-edged cavern around spawn with room to build; confirms the perturbed boundary blends into real cave systems and never overwrites existing air/fluid. **Select "Worldz: Cave"**. Requires 0.2.62+. |
| `56-cave-chest-and-sealed.yaml` | GOALS 25 (Phase 13.2d): `chestEnabled: true` with `chestTier: hard`, combined with the sealed surface — confirms the optional starter chest (set into the floor beneath spawn) composes cleanly with another cave option. **Select "Worldz: Cave"**. Requires 0.2.63+. |
| `59-nether-start-default.yaml` | GOALS 27 (Phase 14.2a/14.2b, DESIGN §31): the core mechanic and default (medium) chest tier — spawn is a real, safe Nether pocket (or a carved capsule fallback), the world's default spawn is redirected there so both first join and no-anchor deaths land back at the same site, and a real Nether respawn anchor placed elsewhere correctly overrides it. **Select "Worldz: Nether Start"**. Requires 0.2.66+. |
| `60-nether-start-chest-easy.yaml` | GOALS 27 (Phase 14.2b): the easy chest tier — 10 obsidian (a full portal frame) plus flint and steel, the "everything needed to build a portal out" GOALS 27 names by name. **Select "Worldz: Nether Start"**. Requires 0.2.66+. |
| `61-nether-start-chest-hard.yaml` | GOALS 27 (Phase 14.2b): the hard chest tier — no guaranteed obsidian or flint and steel at all, relies entirely on Nether exploration; confirms the world still feels beatable from this minimal a start. **Select "Worldz: Nether Start"**. Requires 0.2.66+. |
| `62-nether-start-capsule-fallback.yaml` | GOALS 27 (Phase 14.2a, DESIGN §31.4): `spawnY: 4`, biased (not guaranteed) toward the guaranteed-capsule safe-site fallback rather than a natural pocket — confirms the fully-enclosed nether-brick capsule shell when it fires. **Select "Worldz: Nether Start"**. Requires 0.2.66+. |

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
