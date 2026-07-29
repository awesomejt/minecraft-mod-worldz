# JLT Worldz

Limit newly created Minecraft worlds to one or more chosen biomes while keeping
vanilla terrain shapes, caves, rivers, mountains, Nether, and End. An optional
circular starter biome can be forced around the world origin. Worlds may also
use configurable square borders and replace terrain outside a central square
with ocean or void. Optional starter-land reinforcement prevents a chosen
starter biome from becoming a thin island over deep water. Supports Fabric and
NeoForge for Minecraft 26.2.

> **Use with new worlds only.** Worldz is a world-*creation* mod: settings are
> baked into a world when it is generated. Do not add Worldz to, or expect it
> to change, an existing world, and do not expect worlds created by one Worldz
> version to be supported by a different version.

> **Status (2026-07-16):** the mod is being restructured around challenge-world
> types (ocean island, sky island, sky chunk, single biome, flat, limited
> size) per [GOALS.md](GOALS.md) and [TODO.md](TODO.md). The `mixed` and
> `land_only` coordinated-layout modes have been removed as part of that
> restructure (TODO.md Phase 1). The first dedicated challenge type,
> **Worldz: Single Biome**, has landed (TODO.md Phase 2) — its own World Type
> entry with a small Customize screen, described below. The original
> flexible **Worldz** preset (allowed-biome list, coordinated layouts,
> borders, exteriors) is unchanged and still the way to reach every other
> mode; later phases give the remaining challenge types their own entries the
> same way.

## Challenge types

Worldz adds more than one entry to the **World Type** dropdown, one per
challenge family, each with its own small Customize screen:

| World Type | Covers | Customize screen |
|---|---|---|
| **Worldz** | The original flexible preset: allowed-biome list, coordinated `ocean`/`single_biome`/`void`/`chaos`/`legacy` layouts, borders, exteriors, starter land. See [Using Worldz](#using-worldz) below. | Full screen: biomes, starter zone, borders, exteriors, layout, spawn strategy. |
| **Worldz: Single Biome** | One land biome fills the entire world, everything else (structures, caves, seed-based randomness) generates normally; optional different starter biome; optional seed-chosen starter location; optional natural rivers/oceans. See [Single-biome challenge](#single-biome-challenge) below. | Small screen: land biome, starter biome, starter radius, spawn strategy, allow rivers/oceans. |
| **Worldz: Chaos Biomes** | Seed-shuffled land biome regions over completely untouched vanilla terrain — deserts beside ice spikes beside jungles; configurable region size; optional starter zone; optional natural rivers/oceans. See [Chaos biomes challenge](#chaos-biomes-challenge) below. | Small screen: weighted biome list, region size, starter biome, starter radius, spawn strategy, allow rivers/oceans. |
| **Worldz: Strip World** | A narrow corridor along one axis — everything happens in that strip, ordinary vanilla terrain and biome variety otherwise; configurable width; optional Nether corridor; optional ordered biome-band sequence along its length. See [Strip world challenge](#strip-world-challenge) below. | Small screen: corridor width and unit, width mode (void/ocean), apply-to-Nether, biome bands toggle/list/width/shuffle, spawn strategy, borders, exteriors. |
| **Worldz: Ocean Island** | An island surrounded by an endless generated ocean that gradually deepens from shore to open water: an `artificial` natural-looking island of one chosen biome, a `natural` island found in the seed's own unmodified terrain, or `chest_boat` — no land at all, spawn on a stocked chest boat. Optional distant natural islands beyond an exclusion zone. See [Ocean island challenge](#ocean-island-challenge) below. | Small screen: island source, island biome, radius, coastline shape, shore-ring width, ocean gradient widths/depths, exclusion zone toggle/radius, borders, Nether exterior. |
| **Worldz: Sky Island** | A true floating island: a thin, fixed-thickness slab surrounded by void above, below, and beyond its footprint — Skyblock-style. Necessities chest with easy/medium/hard tiers plus a biome-driven water-source item. Optional matching Nether sky island. See [Sky island challenge](#sky-island-challenge) below. | Small screen: island biome, radius, coastline shape, surface Y, slab thickness, chest tier, apply-to-Nether, borders, Nether exterior. |
| **Worldz: Sky Chunk** | Chunk-shaped islands cut from the seed's own natural chunks: unlike every other island type, a selected chunk's real vanilla terrain (biome, caves, structures) is left completely untouched — only unselected chunks mask to void. Optional top-only depth cutoff; a guaranteed portal-room stronghold and a forced amethyst geode; optional Nether/End application; optional underground-content showcasing. See [Sky chunk challenge](#sky-chunk-challenge) below. | Small screen: spawn chance, cell size, top-only toggle/depth, scattered top-only chance, exclusion zone, apply-to-Nether/End, borders, Nether exterior. |
| **Worldz: Cave** | Cave-only start: the Overworld generates exactly as vanilla would (no biome restriction, no shape at all) — only your spawn changes, placed in a real, searched-out natural underground cavity. Optional solid roof sealing off sky access everywhere; optional large carved mega-cavern around spawn; optional starter chest. See [Cave challenge](#cave-challenge) below. | Small screen: spawn depth, sealed-surface toggle/Y, mega-cavern toggle/radius/height, chest toggle/tier, borders, Nether exterior. |
| **Worldz: Nether Start** | Nether-start challenge: the Overworld generates exactly as vanilla would — you spawn in the Nether instead, in a real safe pocket (or a guaranteed, lit, furnished capsule/starter base, either as a fallback or on request), with a difficulty-tiered starter chest (easy hands over a ready-to-use portal frame, hard leans on exploration; every tier guarantees a pickaxe). Dying without a personal bed/anchor returns you to the same safe site. See [Nether-start challenge](#nether-start-challenge) below. | Small screen: spawn depth, chest tier, borders, Nether exterior. |
| **Worldz: End Start** | End-start challenge: the Overworld and the Nether both generate exactly as vanilla would — you spawn in the End instead, on a guaranteed safe platform far out along the outer-island belt, with a difficulty-tiered starter chest tuned toward reaching and defeating the Ender Dragon (easy hands over rockets/blocks/combat gear, hard leans entirely on the platform's own end stone — plus its guaranteed pickaxe — to bridge across). Dying (beds/anchors are both impossible in the End) returns you to the same platform. See [End-start challenge](#end-start-challenge) below. | Small screen: chest tier, borders. |
| **Worldz: Flat** | Classic flat challenge: my version of vanilla superflat, with more options — an editable bottom-to-top block layer stack, a single fixed biome, an optional bedrock floor (just whether the layer list's bottom entry is bedrock), an eligible-structure-set list, and an optional decoration toggle. Zero noise or caves of any kind, matching vanilla's own real superflat behavior — and genuinely as fast to generate, since the real noise pipeline never runs at all. See [Flat challenge](#flat-challenge) below. | Small screen: layers (text), biome, decoration, structure list (text), borders, exteriors. |
| **Worldz: Deep Flat** | Deep-flat challenge: a flat surface capped over real, unmodified vanilla terrain — caves, cave biomes, aquifers, ores, and structures all come from the seed's own real generation below the cap, completely untouched. Rivers/oceans show as water at the flat surface (optional, with a spawn-adjacent exclusion radius). See [Deep flat challenge](#deep-flat-challenge) below. | Small screen: surface Y, cap layers (text), rivers toggle, exclusion radius, borders, exteriors. |
| **Worldz: Stacked** | Stacked-biome-layers challenge: the underground is replaced entirely by horizontal biome bands, bottom to top, starting at the dimension's own min Y — eight bands by default, or any editable ordering/count — each with its own block stack, a gently uneven surface, and an air gap for that biome's own trees/vegetation to grow into. Ore veins naturally land in whichever layer sits at their real vanilla depth; the default bottom layer is deep enough to reliably anchor the End portal too. Bounded to a small border by default. Optional seed-randomized layer order, optional forced village on the top layer. See [Stacked challenge](#stacked-challenge) below. | Small screen: layers (text), seed-randomized order toggle, relief blocks, force-top-village toggle, borders, exteriors. |

## Supported loaders

| Loader | Status |
|---|---|
| Fabric | supported; requires Fabric API |
| NeoForge | supported |

Use the jar built for your loader; Fabric and NeoForge jars are not
interchangeable. Install the mod on the game or server that creates and hosts
the world. Clients that need to create a Worldz singleplayer world also need the
mod installed.

## Using Worldz

Worldz only affects worlds that explicitly select its preset. Vanilla and other
world types are untouched.

For singleplayer:

1. (Optional) Create `config/jlt_worldz/all.yaml` if you want different
   reusable defaults than the ones built into the mod — copy from
   [`config/jlt_worldz.example.yaml`](config/jlt_worldz.example.yaml), a
   curated illustrative `all.yaml` bundle, then restart Minecraft. No config
   at all is a normal, supported setup: the mod's own defaults apply directly
   and it never creates any config file for you. For the exhaustive setting
   reference, use the generated tables in this README or the generated
   `config/jlt_worldz.reference.yaml` described below. You can also split
   settings across the individual files under `config/jlt_worldz/` instead of
   one `all.yaml` — see [Configuration](#configuration) below for both shapes.
   The mod never rewrites a config file it read — comments and settings you
   didn't set are preserved across every launch. Instead, it writes
   `config/jlt_worldz.reference.yaml` on every launch: a generated, never-read,
   safe-to-delete file showing every setting at its built-in default, handy to
   copy values from.
2. Create a world and select **Worldz** under **World Type**.
3. Select **Customize** to change the biome list, starter zone and land,
   borders, exterior terrain, coordinated layout, or resize rates for this
   world only.

The Customize screen starts with the YAML values. Selecting **Done** bakes the
screen values into the new world without rewriting the YAML file, so each new
singleplayer world can use completely different settings. Biomes and biome tags
can be entered one per line or separated by commas; the screen validates IDs
against the registries loaded for that world.

For a dedicated server, set these values before creating the world:

```properties
level-type=jlt_worldz:worldz
```

For the single-biome challenge instead, use `level-type=jlt_worldz:single_biome`
and the `singleBiome:` config section (see [Single-biome challenge](#single-biome-challenge)).
For the chaos biomes challenge, use `level-type=jlt_worldz:chaos_biomes` and the
`chaosBiomes:` config section (see [Chaos biomes challenge](#chaos-biomes-challenge)).

Delete or rename an existing `level-name` world only when you intentionally want
the server to create a new one. Worldz never converts an existing world.

## Single-biome challenge

Select **Worldz: Single Biome** under **World Type** for a world where one
chosen biome fills the entire generated world — structures, caves, and
vanilla randomness all generate normally and follow the world seed exactly
as they would in a vanilla world of that biome. Select **Customize** for a
small screen with this type's fields (land biome, an optional different
starter biome around spawn, starter radius, spawn strategy) plus the same
shared Overworld/Nether Border, End Border, and Overworld/Nether Exterior
buttons as the generic preset (see [Limited-world borders](#limited-world-borders)
and [Carrying the border into the End](#carrying-the-border-into-the-end)).
This is a separate World Type from plain **Worldz** above — it still does
not read `allowedBiomes` or `layout` at all; `allowedBiomes` derives
automatically (below) and coordinated layout modes are specific to the
generic preset.

Configure its defaults with a `singleBiome:` section in
`config/jlt_worldz/all.yaml` (or unwrapped in
`config/jlt_worldz/world-types/single-biome.yaml` — see
[Configuration](#configuration) below):

```yaml
singleBiome:
  biome: 'minecraft:desert'
  starter:
    biome: 'minecraft:plains'
    radius: 256
  spawn:
    strategy: starter_at_origin
  naturalBiomes:
    rivers: false
    oceans: false
    beaches: false
```

<!-- BEGIN GENERATED CONFIG TABLE: single-biome -->
| Setting | Default | Unit / range | Applies | Description |
|---|---|---|---|---|
| `singleBiome.biome` | `'minecraft:plains'` | biome id | Baked: single_biome; new worlds only; Customize | The one biome that fills the generated world. |
| `singleBiome.starter.biome` | `''` | biome id | Baked: single_biome; new worlds only; Customize | Optional biome id forced in a circular zone around the starter origin; empty disables the starter zone. |
| `singleBiome.starter.radius` | `256` | blocks; 64..4096 | Baked: single_biome; new worlds only; Customize | Starter-zone radius, only meaningful when biome is set. |
| `singleBiome.spawn.strategy` | `'starter_at_origin'` | — | Baked: single_biome; new worlds only; Customize | How the layout origin and initial spawn are chosen. |
| `singleBiome.naturalBiomes.rivers` | `false` | — | Baked: single_biome; new worlds only; Customize | Let vanilla's own river biomes generate where vanilla would place one. |
| `singleBiome.naturalBiomes.oceans` | `false` | — | Baked: single_biome; new worlds only; Customize | Let vanilla's own river/ocean-family biomes generate naturally, additive over rivers. |
| `singleBiome.naturalBiomes.beaches` | `false` | — | Baked: single_biome; new worlds only; Customize | Let vanilla's own beach/stony-shore biomes generate where vanilla would place one. |
<!-- END GENERATED CONFIG TABLE: single-biome -->

`allowedBiomes` (what structures/features see as possible biomes) is derived
automatically from `biome` and `starter.biome` — there is nothing to keep
in sync by hand for this type. When `naturalBiomes.rivers`/`.oceans`/
`.beaches` are on, the matching vanilla biomes (`#minecraft:is_river`,
`#minecraft:is_ocean`, `#minecraft:is_beach` plus `stony_shore`) are folded
in too, so structure/feature placement knows those biomes can occur.

## Chaos biomes challenge

Select **Worldz: Chaos Biomes** under **World Type** for a world where a
list of land biomes is shuffled, seed-based, across regions of the map —
desert beside ice spikes beside jungle, changing every `regionScale`
or so. Unlike **Worldz: Single Biome**, terrain shape is *always* completely
untouched vanilla terrain everywhere (hills, mountains, ravines, natural
water bodies stand exactly as the seed generated them) — chaos only ever
relabels biome identity, never height. Its Customize screen also has the
same shared Overworld/Nether Border, End Border, and Overworld/Nether
Exterior buttons as `single_biome` and the generic preset.

Configure its defaults with a `chaosBiomes:` section in
`config/jlt_worldz/all.yaml` (or unwrapped in
`config/jlt_worldz/world-types/chaos-biomes.yaml`):

```yaml
chaosBiomes:
  biomes:
    - 'minecraft:desert'
    - 'minecraft:jungle'
    - 'minecraft:ice_spikes'
    - 'minecraft:badlands'
    - 'minecraft:taiga'
  regionScale: 512
  starter:
    biome: ''
    radius: 256
  spawn:
    strategy: starter_at_origin
  naturalBiomes:
    rivers: false
    oceans: false
    beaches: false
```

<!-- BEGIN GENERATED CONFIG TABLE: chaos-biomes -->
| Setting | Default | Unit / range | Applies | Description |
|---|---|---|---|---|
| `chaosBiomes.biomes` | `['minecraft:desert', 'minecraft:jungle', 'minecraft:ice_spikes', 'minecraft:badlands', 'minecraft:taiga']` | biome id | Baked: chaos_biomes; new worlds only; Customize | Weighted land biome entries (id or id@weight) shuffled per region. |
| `chaosBiomes.regionScale` | `512` | blocks; 16..8192 | Baked: chaos_biomes; new worlds only; Customize | Grid-cell edge length in blocks; smaller means more frequent biome changes. |
| `chaosBiomes.starter.biome` | `''` | biome id | Baked: chaos_biomes; new worlds only; Customize | Optional biome id forced in a circular zone around the starter origin; empty disables the starter zone. |
| `chaosBiomes.starter.radius` | `256` | blocks; 64..4096 | Baked: chaos_biomes; new worlds only; Customize | Starter-zone radius, only meaningful when biome is set. |
| `chaosBiomes.spawn.strategy` | `'starter_at_origin'` | — | Baked: chaos_biomes; new worlds only; Customize | How the layout origin and initial spawn are chosen. |
| `chaosBiomes.naturalBiomes.rivers` | `false` | — | Baked: chaos_biomes; new worlds only; Customize | Let vanilla's own river biomes generate where vanilla would place one. |
| `chaosBiomes.naturalBiomes.oceans` | `false` | — | Baked: chaos_biomes; new worlds only; Customize | Let vanilla's own river/ocean-family biomes generate naturally, additive over rivers. |
| `chaosBiomes.naturalBiomes.beaches` | `false` | — | Baked: chaos_biomes; new worlds only; Customize | Let vanilla's own beach/stony-shore biomes generate where vanilla would place one. |
<!-- END GENERATED CONFIG TABLE: chaos-biomes -->

`allowedBiomes` derives automatically from `biomes` plus `starter.biome`, the
same way `single_biome`'s does.

## Strip world challenge

Select **Worldz: Strip World** under **World Type** for a world that is a
narrow corridor along one fixed axis — everything happens in that strip.
Unlike `single_biome`/`chaos_biomes`, this preset does not restrict biomes
at all by default: ordinary vanilla terrain and full biome variety generate,
just shaped into a corridor. Optionally, an ordered sequence of biome bands
can walk the corridor's length instead (see [Biome
bands](#biome-bands-goals-36) below).

The corridor has two independent parts. Its **length** (the long axis) uses
the ordinary square border unmodified — the same [limited-world
borders](#limited-world-borders) every other world type has, including
resize schedules and the invisible-wall push-back. Its **width** (the short
axis) is new: a soft edge with no collision at all — terrain simply ends
into void (or ocean) beyond the configured width, and you can walk or fall
past it, the same philosophy as a [void exterior](#ocean-and-void-exteriors).

Configure it with one `stripWorld:` section. On the generic **Worldz**
preset, `enabled` is the opt-in switch for adding a corridor to that preset;
on **Worldz: Strip World**, the Overworld corridor always applies and this
section supplies its settings. No other typed preset reads these settings.
`overworldBorder`/`netherBorder` still supply the corridor's length (see
[Limited-world borders](#limited-world-borders)).

```yaml
stripWorld:
  enabled: true
  width: 65
  widthMode: void
  applyToNether: false
  spawn:
    strategy: starter_at_origin
```

<!-- BEGIN GENERATED CONFIG TABLE: strip-world -->
| Setting | Default | Unit / range | Applies | Description |
|---|---|---|---|---|
| `stripWorld.enabled` | `false` | — | Baked: worldz; new worlds only | Whether the generic worldz preset applies this corridor; the dedicated strip_world preset always does. |
| `stripWorld.width` | `65` | blocks; 1+ | Baked: worldz, strip_world; new worlds only; Customize | Absolute corridor width in blocks; even widths take the extra block on +Z. |
| `stripWorld.widthMode` | `'void'` | — | Baked: worldz, strip_world; new worlds only; Customize | Terrain generated beyond the corridor -- void or ocean, never normal. |
| `stripWorld.applyToNether` | `false` | — | Baked: worldz, strip_world; new worlds only; Customize | Whether the same corridor width also applies to the Nether. |
| `stripWorld.spawn.strategy` | `'starter_at_origin'` | — | Baked: strip_world; new worlds only; Customize | How the layout origin and initial spawn are chosen. |
<!-- END GENERATED CONFIG TABLE: strip-world -->

For width `w`, the inclusive Z range is from `-(w - 1) / 2` through
`w / 2`. Thus width 1 is `Z=0`, width 2 is `Z=0..1`, width 4 is
`Z=-1..2`, and the default width 65 is `Z=-32..32`. The compact End-portal
and blaze fallback targets the corridor midpoint at Z=0. At very narrow
widths their rooms may visibly extend into the exterior; that overflow is
intentional so the structures remain usable.

The Strip World Customize screen edits an absolute **Corridor width**. Its
Blocks/Chunks display is lossless: a non-chunk-aligned width is shown exactly
(for example, 65 blocks as `4.0625` chunks) and returns to the same block
count when switched back. The Apply to Nether checkbox, width, and width mode
are reconstructed from the selected world's persisted settings when the
screen is reopened.

The corridor's length, End border, and exteriors all use the same shared
`overworldBorder`/`netherBorder`/`endBorder`/`overworldExterior`/
`netherExterior` sections every other world type reads, and are also
available on this preset's Customize screen.

### Biome bands (GOALS 36)

Optionally, instead of full vanilla biome variety, the corridor can walk an
ordered sequence of biomes along its length — desert, then jungle, then ice
spikes, and so on, changing every `bands.width` — the same
"terrain stays vanilla, only the biome relabels" philosophy as [Chaos
biomes](#chaos-biomes-challenge), but ordered along one axis instead of
scattered in a 2D grid. The sequence repeats (wraps) once exhausted, so it
stays well-defined regardless of how long the corridor's border eventually
makes it.

Configure it with a nested `bands:` section under `stripWorld:`:

```yaml
stripWorld:
  bands:
    enabled: false
    biomes:
      - 'minecraft:desert'
      - 'minecraft:jungle'
      - 'minecraft:ice_spikes'
      - 'minecraft:badlands'
      - 'minecraft:taiga'
    width: 128
    seedRandomOrder: false
    naturalBiomes:
      rivers: true
      oceans: true
      beaches: true
```

<!-- BEGIN GENERATED CONFIG TABLE: strip-bands -->
| Setting | Default | Unit / range | Applies | Description |
|---|---|---|---|---|
| `stripWorld.bands.enabled` | `false` | — | Baked: strip_world; new worlds only; Customize | Whether the strip passes through biome bands instead of ordinary vanilla terrain. |
| `stripWorld.bands.biomes` | `[]` | biome id | Baked: strip_world; new worlds only; Customize | Ordered land biome ids walked along the strip's length; repeats once exhausted. |
| `stripWorld.bands.width` | `128` | blocks; 16..8192 | Baked: strip_world; new worlds only; Customize | Band width in blocks along the strip's length axis. |
| `stripWorld.bands.seedRandomOrder` | `false` | — | Baked: strip_world; new worlds only; Customize | Shuffle the sequence once (a fixed permutation) instead of using it as given. |
| `stripWorld.bands.naturalBiomes.rivers` | `true` | — | Baked: strip_world; new worlds only; Customize | Let vanilla's own river biomes generate where vanilla would place one. |
| `stripWorld.bands.naturalBiomes.oceans` | `true` | — | Baked: strip_world; new worlds only; Customize | Let vanilla's own river/ocean-family biomes generate naturally, additive over rivers. |
| `stripWorld.bands.naturalBiomes.beaches` | `true` | — | Baked: strip_world; new worlds only; Customize | Let vanilla's own beach/stony-shore biomes generate where vanilla would place one. |
<!-- END GENERATED CONFIG TABLE: strip-bands -->

These three only matter when `bands.enabled` is set — the plain, band-free
strip world already shows full vanilla biome variety (including rivers,
oceans, and beaches) with nothing to configure.

## Ocean island challenge

Select **Worldz: Ocean Island** under **World Type** for a small island
surrounded by an endless generated ocean. Unlike every other typed preset,
there is no spawn-strategy option — the island only ever exists at the
origin, so spawn is always the island's own safe surface point near
`(0, 0)`. `islandSource` (in Customize, or `oceanIsland.island.source` in
config) picks between three ways of sourcing the land itself:

- **`artificial`** (default, GOALS 01) — a natural-looking synthetic island
  of one chosen biome, described below.
- **`natural`** (GOALS 02) — searches the seed's own unmodified terrain for
  a small, isolated real landmass and centers the world there instead;
  nothing about the land itself is synthesized — the real biome and terrain
  show through completely unmodified within `island.radius`, and the same
  ocean gradient begins immediately past it. Not guaranteed to find a
  candidate on every seed; falls back to a plain world origin (real terrain
  used as-is) if the search comes up empty.
- **`chest_boat`** (GOALS 03) — no land at all. The player starts on/next to
  an oak chest boat floating at the origin, stocked with a configurable
  starter kit (see `starterKit` below): a fixed list of essentials (a lily
  pad, some dirt, grass blocks, saplings by default) plus a handful of
  random extras drawn from a configurable pool. The ocean gradient starts
  right at the origin instead of past a shore ring.

Independent of `islandSource`, `fluid` (in Customize, or
`oceanIsland.fluid` in config) picks what the exterior ocean is made of —
any island source can pair with any fluid:

- **`water`** (default, GOALS 01/02/03) — an ordinary ocean.
- **`lava`** (GOALS 28) — the endless ocean is lava instead of water. The
  island shape, shore ring, and ocean gradient bands are otherwise
  unchanged; the shore ring's non-flammable blocks (beach/stony-shore)
  buffer the island's land from the lava. No boats (vanilla already
  disallows placing them on lava); travel is by strider or bridging. No
  ocean monuments, shipwrecks, ocean ruins, kelp/seagrass, or fish
  generate in the lava — it falls back to the same silent,
  decoration-free boundary every non-ocean-island preset already has.
- **`none`** (GOALS 31) — the ocean is a drained, exposed basin (no fluid
  at all, real stone floor). Water-scarcity beatability is automatic —
  this only changes the exterior ocean's own fluid, so village wells,
  strongholds, and aquifer pockets still generate with real water exactly
  as vanilla always has. The "harder" option of also removing rivers and
  surface lakes elsewhere in the world is not implemented (deferred —
  doing it correctly needs real climate-biome sampling threaded through
  the terrain-masking code, which doesn't exist yet at that layer).

The rest of this section describes the `artificial` source's own shape.

The island's coastline is deliberately not a perfect circle: a handful of
seed-derived sine harmonics perturb the radius by direction, giving a
natural "lumpy" shape instead of a disc. A narrow beach/stony-shore ring
(50/50 mix) sits right at the true coastline — narrower than, and
independent from, the terrain-height blend that raises the island's
interior toward guaranteed land. Beyond the shore ring, the ocean itself
gradually deepens: a shallow band (warm/lukewarm/plain ocean only) close to
shore, then a smooth transition to a deep band drawing from the complete
vanilla ocean-biome set (all nine temperature/depth variants) — GOALS 01's
"shallow to deep, but all ocean biomes available." Both the coastline shape
and the ocean gradient are keyed off the same underlying signed distance
from the coastline, so biome and terrain height can never disagree about
where the shore actually is.

Nether and End are completely unaffected by any of this — the island
mechanism only ever touches the Overworld. The shared Overworld/Nether
Border, End Border, and Nether Exterior Customize buttons remain available
and optional if you also want to compose a size limit or restrict the
Nether on top of the island shape; there is no separate Overworld Exterior
option, since the island unconditionally supplies the entire Overworld
exterior itself.

Configure its defaults with an `oceanIsland:` section in
`config/jlt_worldz/all.yaml` (or unwrapped in
`config/jlt_worldz/world-types/ocean-island.yaml`):

```yaml
oceanIsland:
  island:
    source: artificial
    biome: 'minecraft:plains'
    radius: 128
    shapeAmplitude: 0.3
  fluid: water
  shoreWidth: 12
  ocean:
    shallowWidth: 64
    deepenWidth: 128
    shallowDepth: 8
    deepDepth: 32
    regionScale: 128
  exclusionZone:
    enabled: false
    radius: 2000
  starterKit:
    essentials:
      - 'minecraft:lily_pad:1'
      - 'minecraft:dirt:4'
      - 'minecraft:grass_block:2'
      - 'minecraft:oak_sapling:3'
    extras:
      - 'minecraft:bread:3'
      - 'minecraft:wooden_axe:1'
      - 'minecraft:wooden_pickaxe:1'
      - 'minecraft:torch:8'
      - 'minecraft:water_bucket:1'
    extrasCount: 2
```

`starterKit` also accepts a bare name from the shared kit library instead of
the inline block shown above — e.g. `starterKit: ocean-island-default` (its
own shipped default) — see [Shared starter kits](#shared-starter-kits)
below.

<!-- BEGIN GENERATED CONFIG TABLE: ocean-island -->
| Setting | Default | Unit / range | Applies | Description |
|---|---|---|---|---|
| `oceanIsland.island.source` | `'artificial'` | — | Baked: ocean_island; new worlds only; Customize | How the land is sourced: artificial, natural (seed), or chest-boat/none. |
| `oceanIsland.island.biome` | `'minecraft:plains'` | biome id | Baked: ocean_island; new worlds only; Customize | The one biome that fills the island's interior. |
| `oceanIsland.island.radius` | `128` | blocks; 8..65536 | Baked: ocean_island; new worlds only; Customize | Configured (unperturbed) island radius. |
| `oceanIsland.island.shapeAmplitude` | `0.3` | factor; 0..0.6 | Baked: ocean_island; new worlds only; Customize | Coastline perturbation strength. |
| `oceanIsland.fluid` | `'water'` | — | Baked: ocean_island; new worlds only; Customize | The exterior/ocean gradient's fluid: water, lava, or none. |
| `oceanIsland.shoreWidth` | `12` | blocks; 1+ | Baked: ocean_island; new worlds only; Customize | Width of the beach/stony-shore ring; also the terrain-height taper width. |
| `oceanIsland.ocean.shallowWidth` | `64` | blocks; 0+ | Baked: ocean_island; new worlds only; Customize | Width of the shallow ocean band immediately beyond the shore. |
| `oceanIsland.ocean.deepenWidth` | `128` | blocks; 0+ | Baked: ocean_island; new worlds only; Customize | Width over which the seabed ramps from shallow to deep. |
| `oceanIsland.ocean.shallowDepth` | `8` | blocks; 1+ | Baked: ocean_island; new worlds only; Customize | Seabed depth below sea level in the shallow band. |
| `oceanIsland.ocean.deepDepth` | `32` | blocks; 1+ | Baked: ocean_island; new worlds only; Customize | Seabed depth below sea level once fully deep. |
| `oceanIsland.ocean.regionScale` | `128` | blocks; 1+ | Baked: ocean_island; new worlds only; Customize | Grid-cell edge length for the ocean biome's per-region pick. |
| `oceanIsland.exclusionZone.enabled` | `false` | — | Baked: ocean_island; new worlds only; Customize | Whether shaping/generation releases to the seed's own terrain beyond radius. |
| `oceanIsland.exclusionZone.radius` | `2000` | blocks; 1+ | Baked: ocean_island; new worlds only; Customize | Radius beyond which the buffer ends, when enabled. |
| `oceanIsland.starterKit` | `'ocean-island-default'` | — | Baked: ocean_island; new worlds only | Chest-boat starter kit, consulted only when islandSource is chest_boat. |
<!-- END GENERATED CONFIG TABLE: ocean-island -->

Not exposed on the Customize screen — YAML-only, matching every other
variable-length list in this mod's config (biome lists, etc.).

Underground structures beneath the island itself generate normally — Worldz
never suppresses structure placement, only exterior terrain far from the
island (which, like every other exterior mode in this mod, never supported
structures to begin with). A genuinely tiny island (near the 1-chunk floor)
will have the compact fallback End portal (see [Limited-world
borders](#limited-world-borders)) consume most or all of its surface, since
the portal's own safety margin can never "fit" at that scale — an accepted
trade-off, not a defect: GOALS 01 requires the game stay beatable, not that
a tiny island also stays fully buildable-on. Pick a larger radius if you
want both.

## Sky island challenge

Select **Worldz: Sky Island** under **World Type** for a true floating
island, Skyblock-style: a thin, fixed-thickness slab surrounded by void —
not just outside its radius, but above and below the slab too. Like ocean
island, there is no spawn-strategy option and no separate Overworld
Exterior toggle — the island supplies its entire Overworld exterior itself,
and spawn is always its own safe surface point at `(0, 0)`.

The footprint's edge is deliberately not a perfect circle — it reuses the
exact same seed-derived coastline perturbation ocean island's shore uses,
so it reads as a natural "lumpy" shape rather than a disc. Within the
footprint, the slab runs from `surfaceY` down `thickness` deep: solid
ground (grass/dirt over stone, or a biome-appropriate variant — sand over
sandstone for desert-family biomes, snow over dirt for snowy ones, mycelium
for mushroom fields), then open void below. Dig straight down through the
slab and you fall out the bottom; walk off the edge and you fall forever.
Beyond the footprint (and its `exclusionZone` buffer, below), the biome
reading changes to whatever the real seed's own noise reports there —
terrain stays void either way, since only the biome buffer is affected.
Nothing generates naturally anywhere else in the Overworld — no trees, no
mobs, no structures — since the whole point is starting with only what's
in the chest.

A necessities chest appears on the island at world creation, stocked
according to `chestTier` (`easy`, `medium`, or `hard` — in Customize, or
`skyIsland.chest.tier` in config): each tier has its own configurable
essentials/extras list (see `chest.kits.easy`/`.medium`/`.hard` below), and
every tier is intended to remain beatable given enough time. The chest
always additionally includes exactly one water-source item, chosen from
the island's biome: a water bucket for a dry, desert-family biome (which
never gets rain, so a cauldron there would never fill), or a cauldron for
every other biome (rain will fill it naturally over time).

A configurable void buffer (`exclusionZone.enabled`/`exclusionZone.radius`)
pins the biome to the configured `biome` in a ring beyond the island's own
edge, before the real seed's own biome takes over — a purely cosmetic/F3
distinction, since the terrain stays void either way regardless of which
biome is reported.

`applyToNether` (in Customize: "Also make the Nether a sky island", or
`skyIsland.applyToNether` in config) mirrors the exact same
radius/coastline-shape/surfaceY/thickness shape into the Nether. The
Nether's island has no biome concept of its own — its surface is always
netherrack-over-netherrack with a basalt core, regardless of the
Overworld's `biome`. The End is unaffected either way: vanilla End
generation is already a bounded-below floating-island world natively (small
landmasses surrounded by void), so it needs no changes to fit the same
theme — use the existing End Border option (below) if you want to keep it
small too.

The fallback End portal (Overworld) and fortress blaze spawner (Nether, if
`applyToNether` is set) work exactly like every other typed preset's
beatability guarantee — but note a known trade-off: both sit at a fixed
depth, disconnected from the floating island itself by open void. Reaching
them means building or digging straight down.

Configure its defaults with a `skyIsland:` section in
`config/jlt_worldz/all.yaml` (or unwrapped in
`config/jlt_worldz/world-types/sky-island.yaml`):

```yaml
skyIsland:
  biome: 'minecraft:plains'
  radius: 16
  shapeAmplitude: 0.3
  surfaceY: 64
  thickness: 6
  chest:
    tier: medium
    kits:
      easy:
        essentials:
          - 'minecraft:oak_sapling:4'
          - 'minecraft:bread:8'
          - 'minecraft:crafting_table:1'
        extras:
          - 'minecraft:wooden_pickaxe:1'
          - 'minecraft:wooden_axe:1'
          - 'minecraft:torch:16'
          - 'minecraft:cobblestone:32'
        extrasCount: 3
      medium:
        essentials:
          - 'minecraft:oak_sapling:3'
          - 'minecraft:bread:4'
        extras:
          - 'minecraft:wooden_pickaxe:1'
          - 'minecraft:torch:8'
          - 'minecraft:cobblestone:16'
        extrasCount: 2
      hard:
        essentials:
          - 'minecraft:oak_sapling:2'
        extras:
          - 'minecraft:bread:2'
          - 'minecraft:torch:4'
        extrasCount: 1
  applyToNether: false
  exclusionZone:
    enabled: true
    radius: 128
  underground:
    biome: ''
    belowSurface: 10
```

<!-- BEGIN GENERATED CONFIG TABLE: sky-island -->
| Setting | Default | Unit / range | Applies | Description |
|---|---|---|---|---|
| `skyIsland.biome` | `'minecraft:plains'` | biome id | Baked: sky_island; new worlds only; Customize | The one biome that fills the island's interior. |
| `skyIsland.radius` | `16` | blocks; 8..65536 | Baked: sky_island; new worlds only; Customize | Configured (unperturbed) island radius -- small by default, matching Skyblock's scale. |
| `skyIsland.shapeAmplitude` | `0.3` | factor; 0..0.6 | Baked: sky_island; new worlds only; Customize | Coastline perturbation strength. |
| `skyIsland.surfaceY` | `64` | Y level | Baked: sky_island; new worlds only; Customize | The island's walkable surface Y. |
| `skyIsland.thickness` | `6` | blocks; 1..64 | Baked: sky_island; new worlds only; Customize | How many blocks of solid ground extend below surfaceY. |
| `skyIsland.chest.tier` | `'medium'` | — | Baked: sky_island; new worlds only; Customize | Which of the easy/medium/hard kits the starter chest uses. |
| `skyIsland.chest.kits.easy` | `'sky-island-easy'` | — | Baked: sky_island; new worlds only | Generous starter-chest contents. |
| `skyIsland.chest.kits.medium` | `'sky-island-medium'` | — | Baked: sky_island; new worlds only | Middle-ground starter-chest contents. |
| `skyIsland.chest.kits.hard` | `'sky-island-hard'` | — | Baked: sky_island; new worlds only | Bare-essentials starter-chest contents. |
| `skyIsland.applyToNether` | `false` | — | Baked: sky_island; new worlds only; Customize | Whether the Nether is also a sky island, reusing this same shape. |
| `skyIsland.exclusionZone.enabled` | `true` | — | Baked: sky_island; new worlds only; Customize | Whether shaping/generation releases to the seed's own terrain beyond radius. |
| `skyIsland.exclusionZone.radius` | `128` | blocks; 1+ | Baked: sky_island; new worlds only; Customize | Radius beyond which the buffer ends, when enabled. |
| `skyIsland.underground.biome` | `''` | biome id | Baked: sky_island; new worlds only | Biome reported below belowSurface blocks under the surface; blank disables the underground band entirely. |
| `skyIsland.underground.belowSurface` | `10` | blocks; 0+ | Baked: sky_island; new worlds only | How many blocks below the surface the underground band starts; ignored (band never applies) at 0 even with a biome configured. |
<!-- END GENERATED CONFIG TABLE: sky-island -->

### Floating resource islands (GOALS 07-08)

Enable `skyIsland.floatingIslands` to fill the void beyond the starter
island with scattered small floating islands instead of leaving it empty —
a jittered grid of cells, each independently rolling whether it holds an
island (`spawnChance`), with a hash-picked center offset, radius
(`radius.min`..`radius.max`), and coastline shape reusing the
exact same perturbation as every other island shape in this mod. A
configurable void buffer (`exclusionZone.enabled`/`exclusionZone.radius`)
keeps the immediate area around the starter island empty before scattered
islands begin, so reaching them always takes real bridging.

```yaml
skyIsland:
  floatingIslands:
    enabled: false
    radius:
      min: 12
      max: 32
    shapeAmplitude: 0.3
    cellSize: 256
    spawnChance: 0.6
    biomeVariety: true
    biomes:
      - 'minecraft:plains'
      - 'minecraft:forest'
      - 'minecraft:desert'
      - 'minecraft:taiga'
      - 'minecraft:savanna'
    exclusionZone:
      enabled: true
      radius: 256
    oreDeposits:
      enabled: false
      featureIds:
        - 'minecraft:ore_coal'
        - 'minecraft:ore_iron_small'
        - 'minecraft:ore_gold_buried'
        - 'minecraft:ore_redstone'
        - 'minecraft:ore_lapis'
        - 'minecraft:ore_diamond_small'
        - 'minecraft:ore_emerald'
    lootChest:
      enabled: false
      kit:
        essentials:
          - 'minecraft:bread:2'
        extras:
          - 'minecraft:iron_ingot:2'
          - 'minecraft:emerald:1'
          - 'minecraft:arrow:8'
          - 'minecraft:golden_apple:1'
          - 'minecraft:ender_pearl:1'
        extrasCount: 2
```

<!-- BEGIN GENERATED CONFIG TABLE: floating-islands -->
| Setting | Default | Unit / range | Applies | Description |
|---|---|---|---|---|
| `skyIsland.floatingIslands.enabled` | `false` | — | Baked: sky_island; new worlds only; Customize | Whether scattered islands generate at all. |
| `skyIsland.floatingIslands.radius.min` | `12` | blocks; 8..65536 | Baked: sky_island; new worlds only; Customize | Smallest hash-picked island radius. |
| `skyIsland.floatingIslands.radius.max` | `32` | blocks; min..65536 | Baked: sky_island; new worlds only; Customize | Largest hash-picked island radius; floored at min (DESIGN R4). |
| `skyIsland.floatingIslands.shapeAmplitude` | `0.3` | factor; 0..0.6 | Baked: sky_island; new worlds only; Customize | Coastline perturbation strength. |
| `skyIsland.floatingIslands.cellSize` | `256` | blocks; 16..8192 | Baked: sky_island; new worlds only; Customize | Grid-cell edge length -- the primary "how far apart" knob. |
| `skyIsland.floatingIslands.spawnChance` | `0.6` | chance; 0..1 | Baked: sky_island; new worlds only; Customize | Probability that a given cell holds an island, independent of spacing. |
| `skyIsland.floatingIslands.biomeVariety` | `true` | — | Baked: sky_island; new worlds only; Customize | Whether each island hash-picks its own biome from biomes. |
| `skyIsland.floatingIslands.biomes` | `['minecraft:plains', 'minecraft:forest', 'minecraft:desert', 'minecraft:taiga', 'minecraft:savanna']` | biome id | Baked: sky_island; new worlds only; Customize | Candidate biome pool when biomeVariety is true. |
| `skyIsland.floatingIslands.exclusionZone.enabled` | `true` | — | Baked: sky_island; new worlds only; Customize | Whether shaping/generation releases to the seed's own terrain beyond radius. |
| `skyIsland.floatingIslands.exclusionZone.radius` | `256` | blocks; 1+ | Baked: sky_island; new worlds only; Customize | Radius beyond which the buffer ends, when enabled. |
| `skyIsland.floatingIslands.oreDeposits.enabled` | `false` | — | Baked: sky_island; new worlds only; Customize | Whether each island gets one embedded vanilla ore-vein feature. |
| `skyIsland.floatingIslands.oreDeposits.featureIds` | `['minecraft:ore_coal', 'minecraft:ore_iron_small', 'minecraft:ore_gold_buried', 'minecraft:ore_redstone', 'minecraft:ore_lapis', 'minecraft:ore_diamond_small', 'minecraft:ore_emerald']` | — | Baked: sky_island; new worlds only | Candidate vanilla ore ConfiguredFeature ids one island's deposit is hash-picked from. |
| `skyIsland.floatingIslands.lootChest.enabled` | `false` | — | Baked: sky_island; new worlds only; Customize | Whether each island gets one placed loot chest. |
| `skyIsland.floatingIslands.lootChest.kit` | `'floating-islands-loot'` | — | Baked: sky_island; new worlds only | The loot chest's contents. |
| `skyIsland.floatingIslands.naturalBiome` | `false` | — | Baked: sky_island; new worlds only; Customize | Whether each island reads the real underlying seed's own biome instead of biomeVariety's pool. |
<!-- END GENERATED CONFIG TABLE: floating-islands -->

**Guaranteed village (GOALS 07):** whenever `floatingIslands.enabled` is
set, one specific scattered island beyond the exclusion zone is always a
real vanilla village — plains, desert, savanna, snowy, or taiga, hash-
picked per seed and forced to a village-safe minimum radius. It's placed
with the same `/place structure`-style API vanilla itself uses, so it's a
genuine village (jigsaw pieces, loot chests, villagers), not a hand-built
approximation — bridging out from the starter island in the right
direction (also hash-picked per seed) will always find one. No separate
setting: it's automatic whenever scattered islands are on.

Not exposed on the Customize screen beyond the tier selector and the Nether
checkbox — the kit contents themselves are YAML-only, matching every other
variable-length list in this mod's config.

## Sky chunk challenge

Select **Worldz: Sky Chunk** under **World Type** for chunk-shaped islands
cut straight from the seed's own natural chunks. Unlike every other island
type in this mod, a selected chunk's real vanilla terrain — biome, caves,
structures, decoration — is left completely untouched; only unselected
chunks mask to void. There is no synthetic terrain, no surface-material
palette, no coastline shape to configure: the island's shape is exactly one
16×16 chunk (or a group of them, if `cellSizeChunks` is larger than 1).

Every chunk independently rolls whether it's a present island
(`spawnChance`), grouped into `cellSizeChunks`×`cellSizeChunks`-chunk cells
if configured larger than 1. The starter chunk (spawn) is always present.
A void buffer (`exclusionZone.enabled`/`exclusionZone.radius`) keeps
the area immediately around the starter chunk empty before scattered
islands begin.

Each island independently keeps either its **entire natural column** (bedrock
to sky, `topOnly.enabled: false`) or **only its top slice** down to a
configured depth below its own real surface (`topOnly.enabled: true`,
`topOnly.depth` — GOALS 09's own "like 5 deep to ensure access to
stone" example), voiding everything deeper. The cutoff follows each
column's own natural height, not a flat world-absolute Y. The starter
island uses the plan-wide `topOnly.enabled` setting deterministically;
ordinary scattered islands instead each independently hash-pick their own
depth mode via `topOnly.scatteredChance` (GOALS 37 — "each island can
independently be top-only... or the entire chunk column"), so the same
world always gives the same scattered island the same depth mode, but
different islands can differ from each other and from the starter.

A real vanilla stronghold — with its End Portal Room — is guaranteed on
one specific chunk beyond the exclusion zone, forced with the same
`/place structure`-style API vanilla itself uses. Because a stronghold's
own generated footprint is often much larger than one chunk, this reserved
site can pull in several neighboring chunks beyond the void, not stay a
clean single-chunk island; check the server log for its coordinates. An
amethyst geode is separately force-placed on its own reserved chunk
(also logged) — no configuration needed beyond enabling chunk islands at
all.

`applyTo.nether`/`applyTo.end` mirror the exact same chunk-grid mechanism
into the Nether and/or the End — the first typed preset in this mod to
apply to the End's own generator at all. Because chunk islands never
synthesize terrain, no biome-family palette logic is needed the way sky
island's Nether variant needed one: a selected Nether or End chunk shows
real, untouched Nether/End terrain.

**Underground-content showcasing (GOALS 37):** if the seed naturally has a
lush cave, dripstone cave, or deep dark biome, or an ancient city or trial
chambers structure, nearby chunks containing them are preferentially
selected as islands so exploring the scattered grid actually surfaces
varied content — a real seed-search (no chunk generation needed for the
biome check), not depth-aware biome forcing. This is "prefer a naturally-
qualifying chunk when one is nearby," not a guarantee — a seed with no such
content nearby simply won't showcase it.

Configure its defaults with a `chunkIsland:` section in
`config/jlt_worldz/all.yaml` (or unwrapped in
`config/jlt_worldz/world-types/sky-chunk.yaml` — the one filename that
doesn't match its section key):

```yaml
chunkIsland:
  enabled: false
  spawnChance: 0.35
  cellSizeChunks: 1
  topOnly:
    enabled: false
    depth: 5
    scatteredChance: 0.0
  exclusionZone:
    enabled: false
    radius: 256
  applyTo:
    nether: false
    end: false
  geodeFeatureIds:
    - 'minecraft:amethyst_geode'
```

<!-- BEGIN GENERATED CONFIG TABLE: sky-chunk -->
| Setting | Default | Unit / range | Applies | Description |
|---|---|---|---|---|
| `chunkIsland.enabled` | `false` | — | Baked: sky_chunk; new worlds only | Whether chunk islands generate at all. |
| `chunkIsland.spawnChance` | `0.35` | chance; 0..1 | Baked: sky_chunk; new worlds only; Customize | Probability that a given grid cell holds an island. |
| `chunkIsland.cellSizeChunks` | `1` | chunks; 1..512 | Baked: sky_chunk; new worlds only; Customize | Grid-cell edge length in chunks; 1 rolls every chunk independently. |
| `chunkIsland.topOnly.enabled` | `false` | — | Baked: sky_chunk; new worlds only; Customize | Whether a selected island keeps only its top depth, void below. |
| `chunkIsland.topOnly.depth` | `5` | blocks; 1..14999992 | Baked: sky_chunk; new worlds only; Customize | Depth kept below the real generated surface when enabled. |
| `chunkIsland.topOnly.scatteredChance` | `0` | chance; 0..1 | Baked: sky_chunk; new worlds only; Customize | Probability an ordinary scattered island independently resolves top-only. |
| `chunkIsland.exclusionZone.enabled` | `false` | — | Baked: sky_chunk; new worlds only; Customize | Whether shaping/generation releases to the seed's own terrain beyond radius. |
| `chunkIsland.exclusionZone.radius` | `256` | blocks; 0..14999992 | Baked: sky_chunk; new worlds only; Customize | Radius beyond which the buffer ends, when enabled. |
| `chunkIsland.applyTo.nether` | `false` | — | Baked: sky_chunk; new worlds only; Customize | Whether the same chunk-island mechanism also applies to the Nether. |
| `chunkIsland.applyTo.end` | `false` | — | Baked: sky_chunk; new worlds only; Customize | Whether the same chunk-island mechanism also applies to the End. |
| `chunkIsland.geodeFeatureIds` | `['minecraft:amethyst_geode']` | — | Baked: sky_chunk; new worlds only | Candidate configured-feature ids force-placed on the reserved geode showcase cell. |
<!-- END GENERATED CONFIG TABLE: sky-chunk -->

Not exposed on the Customize screen beyond the fields listed above —
`geodeFeatureIds` is YAML-only, matching every other variable-length
feature-id list in this mod's config.

## Cave challenge

Select **Worldz: Cave** under **World Type** for a cave-only start. Unlike
every other typed preset, the Overworld generates exactly as vanilla
would — full biome variety, real seed terrain, no shape or restriction of
any kind. The only change is where you spawn: a real natural cavity,
searched out near a configurable depth (`spawnY`), rather than the
surface. Underground structures (mineshafts, dungeons, trial chambers, a
stronghold) generate normally so the game stays beatable, and the Nether is
reached by an ordinary portal built underground.

If no natural cavity is found near the configured depth within the search
budget, a small safe capsule is carved instead so world creation can never
fail to produce a safe spawn (check the server log for a warning if this
happens).

Two independent options layer on top:

- **Sealed surface** (`sealedSurface.enabled`/`sealedSurface.y`): a solid
  roof caps the entire world at the configured Y, so the whole game is
  played underground with no sky access anywhere. Terrain above that
  height — ordinarily just tall mountain peaks — is deliberately clipped
  flat, not a bug. No sky access also means no phantoms.
- **Mega-cavern** (`cavern.enabled`/`cavern.radius`/`cavern.height`):
  a large, naturally-edged cavern carved around spawn — a buried "world in
  a cave" with room to build a base. The edge is perturbed (not a perfect
  sphere) using the same coastline-shaping math every other footprint in
  this mod shares, so it blends into whatever natural cave systems the seed
  already has there. The carve only ever turns solid blocks into air —
  existing air, water, lava, or natural caves already inside the footprint
  are left exactly as vanilla generated them.

An optional starter chest (`chest.enabled`/`chest.tier`, easy/medium/hard) is
set into the floor directly beneath your spawn position — unlike every
other typed preset's chest, this one defaults to **off**.

Configure its defaults with a `cave:` section in `config/jlt_worldz/all.yaml`
(or unwrapped in `config/jlt_worldz/world-types/cave.yaml`):

```yaml
cave:
  spawnY: -32
  sealedSurface:
    enabled: false
    y: 128
  cavern:
    enabled: false
    radius: 48
    height: 24
  chest:
    enabled: false
    tier: medium
```

<!-- BEGIN GENERATED CONFIG TABLE: cave -->
| Setting | Default | Unit / range | Applies | Description |
|---|---|---|---|---|
| `cave.spawnY` | `-32` | Y level | Baked: cave; new worlds only; Customize | Target Y for the underground spawn-cavity search. |
| `cave.sealedSurface.enabled` | `false` | — | Baked: cave; new worlds only; Customize | Whether a solid roof seals off sky access everywhere. |
| `cave.sealedSurface.y` | `128` | Y level; -32+ | Baked: cave; new worlds only; Customize | The roof's Y; only meaningful when enabled is set. |
| `cave.sealedSurface.block` | `'stone'` | — | Baked: cave; new worlds only | The roof's block: stone, deepslate or bedrock. |
| `cave.sealedSurface.thickness` | `5` | blocks; 1..64 | Baked: cave; new worlds only | The roof's thickness. |
| `cave.cavern.enabled` | `false` | — | Baked: cave; new worlds only; Customize | Whether the mega-cavern is carved around spawn. |
| `cave.cavern.radius` | `48` | blocks; 8..256 | Baked: cave; new worlds only; Customize | The mega-cavern's horizontal half-width. |
| `cave.cavern.height` | `24` | blocks; 8..256 | Baked: cave; new worlds only; Customize | The mega-cavern's vertical half-height. |
| `cave.chest.enabled` | `false` | — | Baked: cave; new worlds only; Customize | Whether the starter chest generates at all. |
| `cave.chest.tier` | `'medium'` | — | Baked: cave; new worlds only; Customize | Which of the easy/medium/hard kits the starter chest uses. |
| `cave.chest.kits.easy` | `'cave-easy'` | — | Baked: cave; new worlds only | Generous starter-chest contents. |
| `cave.chest.kits.medium` | `'cave-medium'` | — | Baked: cave; new worlds only | Middle-ground starter-chest contents. |
| `cave.chest.kits.hard` | `'cave-hard'` | — | Baked: cave; new worlds only | Bare-essentials starter-chest contents. |
<!-- END GENERATED CONFIG TABLE: cave -->

**New worlds only**, same restriction as every other typed preset here: no
save-compat obligations for worlds created by an older mod version.

## Nether-start challenge

Select **Worldz: Nether Start** under **World Type** to begin in the
Nether instead of the Overworld. Like Cave, the Overworld generates
exactly as vanilla would — full biome variety, real seed terrain, no
shape or restriction of any kind, reachable by an ordinary portal once
you build one out. The only change is where you spawn: a real safe Nether
pocket, searched out near a configurable depth (`spawnY`), rather than a
carved cavity or the surface.

If no natural pocket is found near the configured depth within the search
budget, a guaranteed capsule/starter base is carved instead so world
creation can never fail to produce a safe spawn (check the server log for
a warning if this happens) — or set `forceCapsule: true` to request it
outright, skipping the natural search entirely. The capsule is also the
*automatic* choice, with no `forceCapsule` needed, whenever `spawnY` is
close enough to the Nether's own bedrock floor or ceiling (within 16
blocks) that a real open pocket would be rare anyway — the search is
skipped entirely rather than spending real chunk generation on a search
unlikely to succeed.

The capsule is a decent-sized, fully enclosed, always-lit nether-brick
room (default 7x7x5 exterior, a 5x5x3 room as seen from inside). The
chest, a furnace, and a crafting table all line one wall together,
centered (chest in the middle), once the room is big enough to hold them
without crowding your own spawn point in the center. Its size, light
source, and light spacing are all configurable:

<!-- BEGIN GENERATED CONFIG TABLE: nether-start-capsule -->
| Setting | Default | Unit / range | Applies | Description |
|---|---|---|---|---|
| `netherStart.capsule.size` | `7` | blocks; 3..15; odd | Baked: nether_start; new worlds only | Total exterior footprint width/depth, walls included; must stay odd. |
| `netherStart.capsule.height` | `3` | blocks; 2..8 | Baked: nether_start; new worlds only | Interior height. |
| `netherStart.capsule.light.source` | `'glowstone'` | — | Baked: nether_start; new worlds only | Which block lights the capsule. |
| `netherStart.capsule.light.spacing` | `5` | blocks; 1..15 | Baked: nether_start; new worlds only | Spacing between embedded/hung light sources. |
<!-- END GENERATED CONFIG TABLE: nether-start-capsule -->

**Death and respawn work like this:** the world's own default spawn point
is redirected to the resolved Nether site at world creation, so both your
very first join *and* any future death without a personal bed/respawn
anchor return you to that same safe site. Nether respawn anchors work
normally in the Nether (beds don't — vanilla's own rule); place and charge
one anywhere you like, and it overrides the default the same way a bed
would in the Overworld.

A difficulty-tiered starter chest (`chest.tier`, easy/medium/hard — always
on, unlike Cave's optional one) is set into the floor directly beneath
your spawn position. Every tier guarantees at least a wooden pickaxe, so
you can always mine your way out of the capsule's nether-brick walls if
you land in one:

| Tier | Contents |
|---|---|
| `easy` | 10 obsidian (a full portal frame, ready to place) + 1 flint and steel + 8 bread + a wooden pickaxe, plus 3 random extras (golden tools, gold ingots, torches). |
| `medium` | 10 obsidian with no guaranteed ignition, a wooden pickaxe, plus less food and fewer extras. |
| `hard` | No guaranteed obsidian or flint and steel at all, just a wooden pickaxe and bread — leans entirely on Nether exploration (ruined portals, bastion/piglin bartering, a natural lava+water combination) to stay beatable. |

Configure its defaults with a `netherStart:` section in
`config/jlt_worldz/all.yaml` (or unwrapped in
`config/jlt_worldz/world-types/nether-start.yaml`):

```yaml
netherStart:
  spawnY: 32
  chest:
    tier: medium
  forceCapsule: false
  capsule:
    size: 5
    height: 3
    light:
      source: glowstone
      spacing: 5
```

<!-- BEGIN GENERATED CONFIG TABLE: nether-start -->
| Setting | Default | Unit / range | Applies | Description |
|---|---|---|---|---|
| `netherStart.spawnY` | `32` | Y level; 1..120 | Baked: nether_start; new worlds only; Customize | Target Y for the safe-site search. |
| `netherStart.chest.tier` | `'medium'` | — | Baked: nether_start; new worlds only; Customize | Which of the easy/medium/hard kits the starter chest uses. |
| `netherStart.chest.kits.easy` | `'nether-start-easy'` | — | Baked: nether_start; new worlds only | Generous starter-chest contents. |
| `netherStart.chest.kits.medium` | `'nether-start-medium'` | — | Baked: nether_start; new worlds only | Middle-ground starter-chest contents. |
| `netherStart.chest.kits.hard` | `'nether-start-hard'` | — | Baked: nether_start; new worlds only | Bare-essentials starter-chest contents. |
| `netherStart.forceCapsule` | `false` | — | Baked: nether_start; new worlds only | Whether to always build the guaranteed capsule instead of only falling back to it. |
<!-- END GENERATED CONFIG TABLE: nether-start -->

**Not yet available:** `forceCapsule` and the `capsule:` settings are
config-only for now — the in-game Customize screen doesn't expose them
yet. End-start's own platform now shares this same configurable
shape/lighting mechanism (see below); Cave doesn't have it yet, a planned
follow-up.

**New worlds only**, same restriction as every other typed preset here: no
save-compat obligations for worlds created by an older mod version.

## End-start challenge

Select **Worldz: End Start** under **World Type** to begin in the End
instead of the Overworld. The Overworld and the Nether both generate
exactly as vanilla would — full biome variety, real seed terrain, no
shape or restriction of any kind. The only change is where you spawn: a
small, fully enclosed end-stone platform, always built (no natural-site
search — the End's outer regions are mostly void, so a Nether/Cave-style
search would rarely find real terrain worth the cost) far out along the
outer-island belt, never the central island itself. The platform's size,
interior height, and lighting are configurable (`endStart.capsule.*`,
shape shared with Nether-start's own capsule mechanism); the chest lines
one wall to one side once the room is big enough to have a real interior,
rather than sitting underfoot.

**Death and respawn work like this:** the world's own default spawn point
is redirected to the platform at world creation, so both your very first
join *and* any future death return you to that same safe site — beds and
respawn anchors are both impossible in the End (vanilla's own rule, they
explode on use), so there is no personal-spawn override to layer on top
the way Nether-start's respawn anchors work.

**Reaching the Ender Dragon is part of the challenge.** No guaranteed
gateway or teleporter is built, and no Elytra is handed over — Elytra
stays an ordinary End City find, the same way vanilla intends. A
difficulty-tiered starter chest (`chest.tier`, easy/medium/hard) lines the
platform's south wall (or sits underfoot at the smallest platform size,
where there's no side wall to line). Every tier guarantees a pickaxe —
End Stone requires one (any tier) to actually drop when mined, so a
platform with no pickaxe at all would trap you (2026-07-25 fix, Jason's
in-game retest: "mainly need a pickaxe to break out of the starting box"):

| Tier | Contents |
|---|---|
| `easy` | 16 firework rockets, 64 cobblestone, 8 bread, a bow, 32 arrows, an iron sword, a copper pickaxe, plus 3 random extras (iron armor pieces, golden apples, ender pearls). |
| `medium` | Fewer rockets and cobblestone, less food, lighter combat gear, a stone pickaxe. |
| `hard` | No rockets and no guaranteed weapon at all — just a wooden pickaxe, leaning entirely on the platform's own end stone to bridge across the void, plus whatever an End City visit turns up. |

Every tier keeps one guaranteed, always-available path to beatability
regardless of chest contents: the platform itself is minable end stone
(with the guaranteed pickaxe above), so even a zero-rockets hard-tier
world can be hand-bridged toward the central island.

Configure its defaults with an `endStart:` section in
`config/jlt_worldz/all.yaml` (or unwrapped in
`config/jlt_worldz/world-types/end-start.yaml`):

```yaml
endStart:
  chest:
    tier: medium
  capsule:
    size: 7
    height: 3
    light:
      source: glowstone
      spacing: 5
```

<!-- BEGIN GENERATED CONFIG TABLE: end-start -->
| Setting | Default | Unit / range | Applies | Description |
|---|---|---|---|---|
| `endStart.chest.tier` | `'medium'` | — | Baked: end_start; new worlds only; Customize | Which of the easy/medium/hard kits the starter chest uses. |
| `endStart.chest.kits.easy` | `'end-start-easy'` | — | Baked: end_start; new worlds only | Generous starter-chest contents. |
| `endStart.chest.kits.medium` | `'end-start-medium'` | — | Baked: end_start; new worlds only | Middle-ground starter-chest contents. |
| `endStart.chest.kits.hard` | `'end-start-hard'` | — | Baked: end_start; new worlds only | Bare-essentials starter-chest contents. |
| `endStart.capsule.size` | `7` | blocks; 3..15; odd | Baked: end_start; new worlds only | Total exterior footprint width/depth, walls included; must stay odd. |
| `endStart.capsule.height` | `3` | blocks; 2..8 | Baked: end_start; new worlds only | Interior height. |
| `endStart.capsule.light.source` | `'glowstone'` | — | Baked: end_start; new worlds only | Which block lights the capsule. |
| `endStart.capsule.light.spacing` | `5` | blocks; 1..15 | Baked: end_start; new worlds only | Spacing between embedded/hung light sources. |
<!-- END GENERATED CONFIG TABLE: end-start -->

**New worlds only**, same restriction as every other typed preset here: no
save-compat obligations for worlds created by an older mod version.

## Flat challenge

Select **Worldz: Flat** under **World Type** for a classic flat world —
Worldz's own version of vanilla superflat, with more options. There is
zero noise or caves of any kind, matching vanilla's own real flat-world
behavior: the world is a fixed, editable stack of block layers from
bottom to top, and nothing else. Generation is genuinely fast too, since
the real terrain-noise pipeline never runs at all — only structure
placement uses the real vanilla mechanism.

Configure the layer stack, biome, decoration, and eligible structure sets
with a `flat:` section in `config/jlt_worldz/all.yaml` (or unwrapped in
`config/jlt_worldz/world-types/flat.yaml`; the in-game Customize screen edits
the same fields as plain text):

```yaml
flat:
  layers:
    - "minecraft:bedrock:1"
    - "minecraft:stone:123"
    - "minecraft:dirt:3"
    - "minecraft:grass_block:1"
  biome: minecraft:plains
  decoration: false
  structureOverrides:
    - "minecraft:villages"
    - "minecraft:strongholds"
  underground:
    biome: ''
    belowSurface: 10
```

<!-- BEGIN GENERATED CONFIG TABLE: flat -->
| Setting | Default | Unit / range | Applies | Description |
|---|---|---|---|---|
| `flat.layers` | `['minecraft:bedrock:1', 'minecraft:stone:123', 'minecraft:dirt:3', 'minecraft:grass_block:1']` | — | Baked: flat; new worlds only; Customize | Ordered bottom-to-top layer list. |
| `flat.biome` | `'minecraft:plains'` | biome id | Baked: flat; new worlds only; Customize | Single fixed biome for the whole world. |
| `flat.decoration` | `false` | — | Baked: flat; new worlds only; Customize | Whether ordinary biome decoration (trees, flowers, ore veins, etc.) runs. |
| `flat.structureOverrides` | `['minecraft:villages', 'minecraft:strongholds']` | — | Baked: flat; new worlds only; Customize | Structure sets eligible to place; empty means every registered set is eligible. |
| `flat.underground.biome` | `''` | biome id | Baked: flat; new worlds only | Biome reported below belowSurface blocks under the surface; blank disables the underground band entirely. |
| `flat.underground.belowSurface` | `10` | blocks; 0+ | Baked: flat; new worlds only | How many blocks below the surface the underground band starts; ignored (band never applies) at 0 even with a biome configured. |
<!-- END GENERATED CONFIG TABLE: flat -->

**Bug fixed (0.3.7):** `decoration: false` (the default) used to silently skip real structure
placement too, not just ordinary biome decoration — a village or stronghold's *site* would still
be selected, but no block of it was ever actually written into the world. Vanilla's own
`applyBiomeDecoration` bundles structure-piece placement and biome-feature decoration into one
method; this project's own decoration toggle used to skip that whole method rather than just the
feature-decoration half. Fixed by placing structures on their own when decoration is off, matching
how vanilla's own flat worlds always place structures regardless of their own decoration setting.

**Spawn Y and slimes**: there is no separate spawn-Y setting — spawn is
always the top of the configured layer stack (this project's Overworld
starts layer 0 at Y -64, so a 128-block stack like the default above
lands the surface at Y 64). Since slimes only ever spawn where
`Y < 40` in a "slime chunk" (a real, verified vanilla rule, unrelated to
darkness or biome), keeping your layer stack tall enough to clear Y 40
is what "avoiding slimes" means in practice; a short stack close to
vanilla's own historical `classic_flat` numbers (4 blocks total, landing
around Y -60) intentionally allows them.

**Slime cavity trick**: "avoiding slimes" and "still being able to farm
them" aren't mutually exclusive. Any layer entry can be `minecraft:air`,
not just the top one — an air layer placed below Y 40 and sandwiched
between solid floor/ceiling entries carves a hollow, enclosed cavity
where slimes can still spawn (in a "slime chunk"), while your actual
playable surface sits safely above Y 40:

```yaml
flat:
  layers:
    - "minecraft:bedrock:1"
    - "minecraft:stone:90"
    - "minecraft:air:10"
    - "minecraft:stone:23"
    - "minecraft:dirt:3"
    - "minecraft:grass_block:1"
```

This keeps the same 128-block total and Y 64 surface as the default
above, but hollows out a 10-block-tall cavity between Y 27 and Y 36 —
entirely below the Y-40 cutoff, and only reachable by digging down to it.
See `config/tests/94-flat-slime-cavity.yaml` for a ready-to-use example.

**Underground biome band (GOAL 42)**: a related but separate idea —
`underground.biome`/`underground.belowSurface` report a *different*
biome below a configured depth, rather than carving a physical cavity into
the layer stack at all. Useful for biome-gated content (e.g. structures or
mob spawns that only apply underground) without needing an air pocket:

```yaml
flat:
  biome: minecraft:plains
  underground:
    biome: minecraft:dripstone_caves
    belowSurface: 10
```

With the default 128-block layer stack (Y 64 surface), this reports
`dripstone_caves` at Y 53 and below, `plains` everywhere from Y 54 up —
purely a biome-reporting change, not a shape change; the layer stack
still paints the exact same solid blocks either way. Composes with the
slime-cavity trick above (an air pocket can sit inside the underground
band, or outside it) but is independent of it.

**Underground structures aren't automatically buried** — with no real
terrain to bury into, a structure set like `trial_chambers` or
`ancient_cities` is only as "buried" as your own stone-layer depth makes
it. A shallow stack gives an honestly clipped result; that's an accepted
tradeoff of classic flat, not a bug (see [Deep flat challenge](#deep-flat-challenge)
below for a variant where structures are always naturally buried).

**`ancient_cities` also needs the right biome**: real vanilla only places
ancient cities in the `minecraft:deep_dark` biome, on top of its usual
structure spacing. Since `flat` reports one single `biome` everywhere,
listing `minecraft:ancient_cities` in `structureOverrides` only actually
does anything if `biome` is itself set to `minecraft:deep_dark` — over
any other biome it behaves exactly as if disabled, no matter the stone
depth.

**Biome-gated `structureOverrides` entries warn automatically**: this isn't
just an `ancient_cities`-specific caveat — any explicitly listed structure
set that's biome-gated in real vanilla data (checked directly against that
structure's own real eligible-biome list, not a hardcoded table) logs a
warning at world creation if `biome` can never satisfy it, e.g.
`flat.structureOverrides lists 'minecraft:ancient_cities', but it is only
ever eligible in biomes flat.biome ('minecraft:plains') doesn't include; it
will never generate.` Only fires for an explicit `structureOverrides` list —
the empty "every registered set eligible" default deliberately mirrors
vanilla's own all-eligible behavior and stays silent, matching how vanilla
itself never warns about that case either.

**New worlds only**, same restriction as every other typed preset here: no
save-compat obligations for worlds created by an older mod version.

## Deep flat challenge

Select **Worldz: Deep Flat** under **World Type** for a flat surface
capped over real, unmodified vanilla terrain. Unlike classic flat, this
variant delegates to the real noise-based generator (full vanilla biome
variety, exactly like Cave/Single Biome) and only caps the *result*:
everything above a configured Y clears to air, a land-layer band paints
immediately below it, and everything further down is completely
untouched — real caves, real cave biomes, real aquifers and ores, real
structures at their natural depth. This means underground structures are
buried automatically, by construction, unlike classic flat.

River and ocean biome columns get a water surface at the cap instead of
the land band (unless disabled, or within a configurable radius of
spawn) — real rivers stay visible as rivers, just flattened into the cap
plane rather than carved as a valley.

Configure it with a `deepFlat:` section in `config/jlt_worldz/all.yaml` (or
unwrapped in `config/jlt_worldz/world-types/deep-flat.yaml`):

```yaml
deepFlat:
  surfaceY: 64
  capLayers:
    - "minecraft:dirt:3"
    - "minecraft:grass_block:1"
  rivers:
    enabled: true
    exclusionRadius: 512
```

<!-- BEGIN GENERATED CONFIG TABLE: deep-flat -->
| Setting | Default | Unit / range | Applies | Description |
|---|---|---|---|---|
| `deepFlat.surfaceY` | `64` | Y level; -32..256 | Baked: deep_flat; new worlds only; Customize | The flat cap height -- everything above clears to air, real terrain below stays. |
| `deepFlat.capLayers` | `['minecraft:dirt:3', 'minecraft:grass_block:1']` | — | Baked: deep_flat; new worlds only; Customize | Land-cap layer stack, painted immediately below surfaceY. |
| `deepFlat.rivers.enabled` | `true` | — | Baked: deep_flat; new worlds only; Customize | Whether a river/ocean biome column gets a water-surface cap instead of the land cap. |
| `deepFlat.rivers.exclusionRadius` | `512` | blocks; 0+ | Baked: deep_flat; new worlds only; Customize | Radius around the origin within which river/ocean columns always get the land cap. |
<!-- END GENERATED CONFIG TABLE: deep-flat -->

**Known first-pass limitation, not yet fixed:** a water-capped river/ocean
column's water sits directly on whatever real terrain is immediately
below the cap band — if a natural cave opening happens to sit right at
that boundary, the placed water could source down into it. Report back if
you actually see this during testing.

**New worlds only**, same restriction as every other typed preset here: no
save-compat obligations for worlds created by an older mod version.

## Stacked challenge

Select **Worldz: Stacked** under **World Type** for a world whose
underground is replaced entirely by stacked horizontal biome layers
instead of normal caves — eight bands by default (taiga through plains),
or any ordering/count you configure. Each layer is its own low-relief
slab reporting its own biome, stacked bottom to top starting at the
dimension's own min Y, with an air gap above its own block stack sized
for that biome's own trees and vegetation to grow into. By default the
world is bounded to a small 64-block-radius border (a good fit for the
default 8-layer stack) and each layer's surface has a small, gentle bump
so bands don't read as perfectly flat slabs.

Ore veins that normally need deep levels (lapis, gold, diamond) need no
special handling — because the stack starts at the dimension's own real
min Y, whichever layer ends up deepest naturally gets the same real ore
range vanilla's own deep Overworld would. A short total stack genuinely
has less deep-Y room for rare ores to occur, the same honest tradeoff a
real shallow world would have; there's no synthetic "ore budget"
redistribution, just real vanilla placement against real Y coordinates.
The default bottom layer's stone is deliberately thick (44 blocks) so the
End portal has real room to generate there — see below.

Configure the layer stack and order with a `stacked:` section in
`config/jlt_worldz/all.yaml` (or unwrapped in
`config/jlt_worldz/world-types/stacked.yaml`):

```yaml
stacked:
  layers:
    - "minecraft:taiga;minecraft:bedrock:1,minecraft:stone:43;30"
    - "minecraft:desert"
    - "minecraft:badlands"
    - "minecraft:swamp"
    - "minecraft:jungle"
    - "minecraft:savanna"
    - "minecraft:snowy_taiga"
    - "minecraft:plains;minecraft:stone:6,minecraft:dirt:3,minecraft:grass_block:1;0"
  seedRandomizedOrder: false
  worldSizeChunks: 4
  relief: 4
  forceTopVillage: false
```

<!-- BEGIN GENERATED CONFIG TABLE: stacked -->
| Setting | Default | Unit / range | Applies | Description |
|---|---|---|---|---|
| `stacked.layers` | `['minecraft:taiga;minecraft:bedrock:1,minecraft:stone:43;30', 'minecraft:desert', 'minecraft:badlands', 'minecraft:swamp', 'minecraft:jungle', 'minecraft:savanna', 'minecraft:snowy_taiga', 'minecraft:plains;minecraft:stone:6,minecraft:dirt:3,minecraft:grass_block:1;0']` | — | Baked: stacked; new worlds only; Customize | Ordered bottom-to-top layer list: eight bands, deep taiga through surface plains. |
| `stacked.seedRandomizedOrder` | `false` | — | Baked: stacked; new worlds only; Customize | Whether the configured layer order is shuffled, seeded off the real world seed. |
| `stacked.worldSizeChunks` | `4` | chunks; 0..937499 | Baked: stacked; new worlds only | Overworld exterior half-width in chunks; zero opts out to the shared exterior section. |
| `stacked.relief` | `4` | blocks; 0..16 | Baked: stacked; new worlds only; Customize | Maximum per-column height bump applied to each layer's own surface. |
| `stacked.forceTopVillage` | `false` | — | Baked: stacked; new worlds only; Customize | Whether a real vanilla village is always force-generated near spawn on the top layer. |
<!-- END GENERATED CONFIG TABLE: stacked -->

The stack's vertical center lands around Y98 with the default layer list
(not exactly Y64) — an 8-layer stack with 30-block gaps and a genuinely
thick bottom layer doesn't fit under the build-height cap at an exact
Y64 center without shrinking every band down to a few blocks each; Y98
was chosen to keep real, mineable ~10-block bands instead. Tune the
layer list directly if you want a different balance.

**Trees and vegetation generate in every layer**, not just the surface —
real vanilla decoration already places ore veins and other fixed-depth
features correctly once biome varies by Y, but heightmap-based features
(trees, grass) can only ever see the topmost layer through the ordinary
mechanism, since a chunk's heightmap is fundamentally single-valued per
column. Every layer below the top gets its own small decoration pass
instead, scattering that layer's own biome's trees directly into its air
gap — a deliberate simplification (fixed scatter density, not an attempt
at full vanilla placement-modifier fidelity), not a gap found after the
fact.

**The End portal at default settings is reliable, not just likely:** the
default 64-block world radius is smaller than the fallback beatability
search's own natural-structure margin, so a natural stronghold is always
rejected and the compact fallback vault always fires instead, landing
inside the thick bottom layer's stone near Y -32. This is a byproduct of
the default world size, not bespoke placement code for `stacked` — a much
larger `worldSizeChunks` reopens the door to a natural stronghold, with
the same open question below.

**Known open question, not yet resolved:** a short total stack (thin
layers, little solid material) hasn't been confirmed against a
stronghold/End portal's own real 3D size — the structure could plausibly
end up clipped by the stack's own air gaps. Not fixed speculatively;
report back what you actually see if you test a short stack.

**New worlds only**, same restriction as every other typed preset here: no
save-compat obligations for worlds created by an older mod version.

## World hazards

Unlike every preset above, these are shared runtime rules, not world
generation — they compose with **any** World Type, configured via their own
top-level sections in `config/jlt_worldz/all.yaml` or, wrapped the same way,
in `config/jlt_worldz/runtime.yaml` (no dedicated Customize screen yet; edit
the config file, same as border/exterior settings before Phase 5.3 exposed
those in-screen). Unlike everything else in this mod, these three sections
are re-read live and affect worlds that already exist — see
[Live vs. baked](#live-vs-baked) below.

### Forever night (GOAL 30)

Set `foreverNight.enabled: true` for a world where night eventually
becomes permanent — either immediately (`lockAfterDays: 0`, the default)
or after a configured number of in-game days. Once locked, Minecraft's own
day/night clock stops advancing and sits at night; sleeping in a bed no
longer skips time (this falls out of disabling the same `advance_time`
gamerule vanilla's own sleep logic already checks — no separate mechanism
needed), though players can still physically sleep.

```yaml
foreverNight:
  enabled: false
  lockAfterDays: 0
  relaxInsomnia: false
```

<!-- BEGIN GENERATED CONFIG TABLE: forever-night -->
| Setting | Default | Unit / range | Applies | Description |
|---|---|---|---|---|
| `foreverNight.enabled` | `false` | — | Live; existing worlds after relaunch | Whether night eventually locks permanently. |
| `foreverNight.lockAfterDays` | `0` | days; 0..1000000 | Live; existing worlds after relaunch | In-game days before night locks; zero locks immediately at world creation. |
| `foreverNight.relaxInsomnia` | `false` | — | Live; existing worlds after relaunch | Whether phantom/insomnia pressure is suppressed once night is locked. |
<!-- END GENERATED CONFIG TABLE: forever-night -->

**Known limitation:** locking night can freeze part of an `overworldBorder`
resize schedule in the Overworld — Minecraft 26.2 ties this mod's own
border-scheduling math to the same per-dimension clock day/night uses. A
resize that's already smoothly animating is **not** affected (vanilla's
own border animation runs independently once started); only a resize
still waiting on its own `resize.delayDays` countdown, or an active
*stepped*-style resize's own periodic jumps, stalls while night stays
locked. Not engineered around; if you're combining forever night with a
delayed or stepped border schedule, expect it to hold still until night
unlocks.

### Rising lava floor (GOAL 29)

Set `risingLava.enabled: true` for a world-wide lava level that rises over
time in the Overworld. It holds at `startY` for `delayDays`, then rises
`rate.blocks` every `rate.days` until it reaches `maxY`. Every air or water
block (source or flowing — vanilla represents both as the same block) at
or below the current level converts to lava; solid terrain is untouched.
Already-loaded chunks convert incrementally as the level rises; a chunk
that loads later (or reloads after a restart) catches up to the current
level immediately.

```yaml
risingLava:
  enabled: false
  delayDays: 3
  startY: -64
  maxY: 64
  rate:
    blocks: 1
    days: 1
```

<!-- BEGIN GENERATED CONFIG TABLE: rising-lava -->
| Setting | Default | Unit / range | Applies | Description |
|---|---|---|---|---|
| `risingLava.enabled` | `false` | — | Live; existing worlds after relaunch | Whether the lava level rises over time. |
| `risingLava.delayDays` | `3` | days; 0..1000000 | Live; existing worlds after relaunch | In-game days before the level starts rising. |
| `risingLava.startY` | `-64` | Y level; -64..319 | Live; existing worlds after relaunch | Y the lava level starts at, before any rise. |
| `risingLava.maxY` | `64` | Y level; -64..319 | Live; existing worlds after relaunch | Y the lava level stops rising at. |
| `risingLava.rate.blocks` | `1` | blocks; 1..14999992 | Live; existing worlds after relaunch | Blocks the level rises per rate.days. |
| `risingLava.rate.days` | `1` | days; 1..1000000 | Live; existing worlds after relaunch | In-game days per rate.blocks of rise. |
<!-- END GENERATED CONFIG TABLE: rising-lava -->

No special handling for floating/void-based world types (sky island, sky
chunk, chunk island) — the rule is uniform everywhere air or water exists,
including flooding void space beneath a floating island as the level
rises past it. World hazards in this project are allowed to be
destructive by design (see GOAL 38's own expanding-border behavior).

## Structure options

Like world hazards above, this is a shared runtime rule composing with
**any** World Type — config-only for now, no dedicated Customize screen.

### Structures far from spawn (GOAL 24)

Set `structureDistance.enabled: true` to hold every vanilla structure set
(villages, pillager outposts, strongholds, trail ruins, and so on) back
from spawn until it's at least `minDistance` away (Chebyshev — the
same "square" distance border/exterior/exclusion-zone settings use
throughout this mod), turning them into a genuine trip rather than a
next-door neighbor. List any structure set ids that should stay at their
normal, un-restricted vanilla distance in `exemptStructureSets`.

```yaml
structureDistance:
  enabled: false
  minDistance: 2000
  exemptStructureSets: []
```

<!-- BEGIN GENERATED CONFIG TABLE: structure-distance -->
| Setting | Default | Unit / range | Applies | Description |
|---|---|---|---|---|
| `structureDistance.enabled` | `false` | — | Live; existing worlds after relaunch | Whether vanilla structure sets are held back from spawn at all. |
| `structureDistance.minDistance` | `2000` | blocks; 0..14999992 | Live; existing worlds after relaunch | Minimum block distance (Chebyshev) from spawn. |
| `structureDistance.exemptStructureSets` | `[]` | — | Live; existing worlds after relaunch | Structure set ids always allowed at their normal vanilla distance. |
<!-- END GENERATED CONFIG TABLE: structure-distance -->

Vanilla's `/locate structure` predicts a candidate position from the
structure's own placement math and has no idea this mod suppressed
generation there — it can still report a coordinate inside the restricted
radius. That's expected: the actual chunk simply generates without the
structure, exactly like any other suppressed structure set.

## Configuration

The mod reads configuration from `config/jlt_worldz/` at startup if present;
every file in it is entirely optional and the mod never creates or requires
any of them. These values are the defaults for the singleplayer Customize
screen and the direct inputs for dedicated-server world creation. The generated
settings tables in this README and the runtime-generated
`config/jlt_worldz.reference.yaml` are the exhaustive references for every
available setting. [`config/jlt_worldz.example.yaml`](config/jlt_worldz.example.yaml)
is instead a curated, illustrative `all.yaml` bundle: copy and adapt its
examples, but do not treat it as a complete listing. See [Config file
layout](#config-file-layout) below for the two shapes the live config can take.
The mod never rewrites a config file it read, so any comments or settings you
leave out stay untouched; it writes `config/jlt_worldz.reference.yaml` on every
launch instead — a sibling of `config/jlt_worldz/`, not a file inside it,
generated, never read back, safe to delete, and always showing every setting at
its current built-in default.

### Config file layout

Settings live under `config/jlt_worldz/`, in either of two shapes:

- **Split, one file per section** — up to 16 files, one per typed preset plus
  three shared files, each holding only the sections that apply to it:

  ```
  config/jlt_worldz/
    runtime.yaml          # foreverNight, risingLava, structureDistance
    world-defaults.yaml   # overworldBorder, netherBorder, endBorder,
                           # overworldExterior, netherExterior
    kits.yaml              # kits (see Shared starter kits below)
    world-types/
      worldz.yaml          # allowedBiomes, starter, naturalBiomes, layout, spawn
      strip-world.yaml     # stripWorld
      single-biome.yaml    # singleBiome
      chaos-biomes.yaml    # chaosBiomes
      ocean-island.yaml    # oceanIsland
      sky-island.yaml      # skyIsland
      sky-chunk.yaml       # chunkIsland (the one filename that doesn't match its key)
      cave.yaml            # cave
      nether-start.yaml    # netherStart
      end-start.yaml       # endStart
      flat.yaml            # flat
      deep-flat.yaml       # deepFlat
      stacked.yaml         # stacked
  ```

  Each of the 16 files is independently optional — write only the ones you
  actually want to change and leave the rest unwritten; a file the mod
  doesn't find just leaves its sections at their built-in defaults, and none
  of the 16 is ever created for you.

- **Single bundle** — `config/jlt_worldz/all.yaml`, the whole config in one
  file, shaped exactly like `config/jlt_worldz.example.yaml` and the
  generated reference file below. **This is the recommended, primary way to
  configure Worldz** — when `all.yaml` exists it wins wholesale over every
  split file above (logging a warning if any split files are also present),
  so there's never anything to reconcile between the two shapes, and it's
  what every `config/tests/*.yaml` fixture in this repo is shaped like.

Most of the split files are **unwrapped**: the twelve one-section files under
`world-types/` (every one except `worldz.yaml`, which holds more than one
section) only ever hold that one section, so their root mapping *is* that
section's body directly, with no wrapper key at all —
`world-types/cave.yaml` starts straight at `spawnY: -32`, not
`cave: {spawnY: -32}`. `world-types/strip-world.yaml` is likewise flat:
it starts with `enabled: false`, `width: 65`, and its other `stripWorld`
fields, not a `stripWorld:` wrapper. `kits.yaml` is unwrapped the same way:
its root mapping *is* the name-to-kit map directly, not `kits: {...}` — see
[Shared starter kits](#shared-starter-kits) below. The remaining three files
(`runtime.yaml`, `world-defaults.yaml`, `world-types/worldz.yaml`) are
**wrapped** instead: their root mapping is a slice of the same `key: {...}`
shape the settings tables below and the single-bundle form use (e.g.
`runtime.yaml` still starts with a `foreverNight:` block).

### Shared starter kits

Every kit-bearing setting — ocean island's `starterKit`, the tiered
`chest.kits.easy`/`.medium`/`.hard` cave/sky-island/Nether-start/End-start
share, and floating islands' `lootChest.kit` — accepts either of two forms
in that exact key position:

- **A bare name**, referencing an entry in the `kits` library described
  below:

  ```yaml
  cave:
    chest:
      kits:
        easy: cave-easy
  ```

- **A full inline definition**, exactly as before this feature — its own
  `essentials`/`extras`/`extrasCount`, same shorthand item format as always:

  ```yaml
  cave:
    chest:
      kits:
        easy:
          essentials: ['minecraft:bread:4']
          extras: ['minecraft:torch:8']
          extrasCount: 1
  ```

Both forms remain fully legal at all 14 kit sites; nothing about the inline
form changed.

The library itself lives under a `kits` root section — unwrapped into its
own `config/jlt_worldz/kits.yaml` file (see [Config file
layout](#config-file-layout) above), or a `kits:` block in `all.yaml` — a
plain name-to-kit map, each entry shaped exactly like an inline kit above.
It ships with 14 pre-named entries, one per site's own previous default, so
an untouched config resolves to exactly the same kit contents it always has:

| Name | Site |
|---|---|
| `cave-easy` | `cave.chest.kits.easy` |
| `cave-medium` | `cave.chest.kits.medium` |
| `cave-hard` | `cave.chest.kits.hard` |
| `sky-island-easy` | `skyIsland.chest.kits.easy` |
| `sky-island-medium` | `skyIsland.chest.kits.medium` |
| `sky-island-hard` | `skyIsland.chest.kits.hard` |
| `nether-start-easy` | `netherStart.chest.kits.easy` |
| `nether-start-medium` | `netherStart.chest.kits.medium` |
| `nether-start-hard` | `netherStart.chest.kits.hard` |
| `end-start-easy` | `endStart.chest.kits.easy` |
| `end-start-medium` | `endStart.chest.kits.medium` |
| `end-start-hard` | `endStart.chest.kits.hard` |
| `ocean-island-default` | `oceanIsland.starterKit` |
| `floating-islands-loot` | `skyIsland.floatingIslands.lootChest.kit` |

<!-- BEGIN GENERATED CONFIG TABLE: shared-kits -->
| Setting | Default | Unit / range | Applies | Description |
|---|---|---|---|---|
| `kits.<name>.essentials` | `varies by named entry` | item list | Baked: ocean_island, sky_island, cave, nether_start, end_start; new worlds only | Always-included items. |
| `kits.<name>.extras` | `varies by named entry` | item list | Baked: ocean_island, sky_island, cave, nether_start, end_start; new worlds only | Candidate items the random picks draw from. |
| `kits.<name>.extrasCount` | `varies by named entry` | count; 0+ | Baked: ocean_island, sky_island, cave, nether_start, end_start; new worlds only | How many extras to pick, with replacement. |
<!-- END GENERATED CONFIG TABLE: shared-kits -->

A user's own `kits.yaml` (or `kits:` block in `all.yaml`) **merges over**
this set — adding new names, or overriding a shipped one by reusing its
name — rather than replacing it wholesale; the 14 shipped names are never
deleted, so every site still referencing one keeps working even if you've
never touched `kits.yaml` at all.

Naming an unknown kit (a typo, or a name you removed) logs a warning and
falls back to that specific site's own shipped default — an unresolved
`cave.chest.kits.easy` falls back to `cave-easy`, never to some generic
empty kit — so one bad reference never blanks out a chest or aborts the
rest of the config.

This relocates kit contents into one shared file rather than shrinking the
overall config: none of the 14 shipped kits share contents with another, so
nothing actually merges away. The benefit is locality — a preset's own file
gets much shorter once its kits are just names — and reuse: define a kit
once, point several sites at it, or swap an entire preset's kit tier by
editing one line.

### Live vs. baked

Which file a setting lives in also says when it takes effect:

- **`runtime.yaml`** (`foreverNight`, `risingLava`, `structureDistance`) is
  re-read after relaunch: these runtime hazard rules affect worlds that already
  exist, not just newly created ones. See [World hazards](#world-hazards) above.
- **Everything else** — `world-defaults.yaml`'s borders and exteriors,
  `kits.yaml`'s shared kits, the generic defaults in `world-types/worldz.yaml`,
  and every typed `world-types/*.yaml` preset section — is copied into (baked
  into) a world when it is created. Editing that YAML cannot retrofit an
  existing world; the new values apply only to worlds created afterward.

<!-- BEGIN GENERATED CONFIG TABLE: generic-and-world-defaults -->
| Setting | Default | Unit / range | Applies | Description |
|---|---|---|---|---|
| `allowedBiomes` | `['minecraft:desert', 'minecraft:beach', 'minecraft:river', 'minecraft:badlands', 'minecraft:eroded_badlands', 'minecraft:wooded_badlands', 'minecraft:stony_shore', 'minecraft:dripstone_caves', 'minecraft:lush_caves', 'minecraft:deep_dark', 'minecraft:sulfur_caves']` | biome id | Baked: worldz; new worlds only; Customize | Biome ids and biome-tag ids allowed in new Worldz worlds. |
| `starter.biome` | `'minecraft:plains'` | biome id | Baked: worldz; new worlds only; Customize | Optional biome id forced in a circular zone around the starter origin; empty disables the starter zone. |
| `starter.radius` | `256` | blocks; 64..4096 | Baked: worldz; new worlds only; Customize | Starter-zone radius, only meaningful when biome is set. |
| `starter.land.enabled` | `true` | — | Baked: worldz; new worlds only; Customize | Whether low terrain beneath a starter biome is raised into usable land. |
| `starter.land.transition` | `128` | blocks; 0..4096 | Baked: worldz; new worlds only; Customize | Outward distance used to blend reinforced land into natural terrain. |
| `starter.land.foundationDepth` | `48` | blocks; 0..384 | Baked: worldz; new worlds only; Customize | Depth below the natural ocean floor repaired as solid foundation. |
| `naturalBiomes.rivers` | `false` | — | Baked: worldz; new worlds only | Let vanilla's own river biomes generate where vanilla would place one. |
| `naturalBiomes.oceans` | `false` | — | Baked: worldz; new worlds only | Let vanilla's own river/ocean-family biomes generate naturally, additive over rivers. |
| `overworldBorder.enabled` | `false` | — | Baked: all presets; new worlds only; Customize | Whether this dimension receives a limited border. |
| `overworldBorder.initialRadius` | `512` | blocks; 1..14999992 | Baked: all presets; new worlds only; Customize | Border half-width when the world is created. |
| `overworldBorder.finalRadius` | `512` | blocks; 1..14999992 | Baked: all presets; new worlds only; Customize | Border half-width after the configured resize period. |
| `overworldBorder.resize.days` | `0` | days; 0..1000000 | Baked: all presets; new worlds only; Customize | In-game days used for the linear transition from initial to final radius. |
| `overworldBorder.resize.delayDays` | `0` | days; 0..1000000 | Baked: all presets; new worlds only; Customize | In-game days to hold the initial radius before resizing. |
| `overworldBorder.resize.style` | `'continuous'` | — | Baked: all presets; new worlds only; Customize | Whether the rate fields drive one smooth lerp or abrupt jumps. |
| `overworldBorder.resize.rate.blocks` | `0` | blocks; 0..14999992 | Baked: all presets; new worlds only; Customize | Radius blocks traversed per rate interval, or zero to use resize.days. |
| `overworldBorder.resize.rate.days` | `0` | days; 0..1000000 | Baked: all presets; new worlds only; Customize | In-game days per rate interval, or zero to use resize.days. |
| `overworldBorder.ensureEndPortal` | `true` | — | Baked: all presets; new worlds only; Customize | Whether the dimension's progression objective must be reachable inside the final border. |
| `netherBorder.enabled` | `false` | — | Baked: all presets; new worlds only; Customize | Whether this dimension receives a limited border. |
| `netherBorder.initialRadius` | `512` | blocks; 1..14999992 | Baked: all presets; new worlds only; Customize | Border half-width when the world is created. |
| `netherBorder.finalRadius` | `512` | blocks; 1..14999992 | Baked: all presets; new worlds only; Customize | Border half-width after the configured resize period. |
| `netherBorder.resize.days` | `0` | days; 0..1000000 | Baked: all presets; new worlds only; Customize | In-game days used for the linear transition from initial to final radius. |
| `netherBorder.resize.delayDays` | `0` | days; 0..1000000 | Baked: all presets; new worlds only; Customize | In-game days to hold the initial radius before resizing. |
| `netherBorder.resize.style` | `'continuous'` | — | Baked: all presets; new worlds only; Customize | Whether the rate fields drive one smooth lerp or abrupt jumps. |
| `netherBorder.resize.rate.blocks` | `0` | blocks; 0..14999992 | Baked: all presets; new worlds only; Customize | Radius blocks traversed per rate interval, or zero to use resize.days. |
| `netherBorder.resize.rate.days` | `0` | days; 0..1000000 | Baked: all presets; new worlds only; Customize | In-game days per rate interval, or zero to use resize.days. |
| `netherBorder.ensureBlazeAccess` | `true` | — | Baked: all presets; new worlds only; Customize | Whether the dimension's progression objective must be reachable inside the final border. |
| `endBorder.carryFromOverworld` | `false` | — | Baked: all presets; new worlds only; Customize | Whether the End receives a border matching the Overworld's eventual (final) radius. |
| `endBorder.minimumRadius` | `256` | blocks; 1..14999992 | Baked: all presets; new worlds only; Customize | Smallest End border half-width regardless of the carried Overworld radius. |
| `overworldExterior.mode` | `'normal'` | — | Baked: all presets; new worlds only; Customize | Terrain generated outside the central envelope: normal, ocean or void. |
| `overworldExterior.boundaryRadius` | `0` | blocks; 0..14999992 | Baked: all presets; new worlds only; Customize | Outer envelope half-width, or zero to derive it from an enabled border. |
| `overworldExterior.oceanTransitionWidth` | `128` | blocks; 0..14999992 | Baked: all presets; new worlds only; Customize | Ocean width inside the outer boundary; ignored by normal and void modes. |
| `netherExterior.mode` | `'normal'` | — | Baked: all presets; new worlds only; Customize | Terrain generated outside the central envelope: normal, ocean or void. |
| `netherExterior.boundaryRadius` | `0` | blocks; 0..14999992 | Baked: all presets; new worlds only; Customize | Outer envelope half-width, or zero to derive it from an enabled border. |
| `netherExterior.oceanTransitionWidth` | `128` | blocks; 0..14999992 | Baked: all presets; new worlds only; Customize | Ocean width inside the outer boundary; ignored by normal and void modes. |
| `layout.mode` | `'legacy'` | — | Baked: worldz; new worlds only; Customize | Layout mode; legacy preserves pre-Phase-15 climate-filter-only behavior. |
| `layout.biomes` | `[]` | biome id | Baked: worldz; new worlds only; Customize | Weighted candidate biome ids, id or id@weight; tags are not accepted. |
| `layout.regionScale` | `512` | blocks; 16..8192 | Baked: worldz; new worlds only; Customize | Grid-cell edge length in blocks. |
| `layout.singleBiome` | `''` | biome id | Baked: worldz; new worlds only; Customize | SINGLE_BIOME mode only: the one biome id filling the world. |
| `layout.roleOverrides` | `{}` | — | Baked: worldz; new worlds only; Customize | Explicit biome id to role (land/ocean/beach) overrides. |
| `spawn.strategy` | `'starter_at_origin'` | — | Baked: worldz; new worlds only; Customize | How the layout origin and initial spawn are chosen. |
<!-- END GENERATED CONFIG TABLE: generic-and-world-defaults -->

Short ids use the `minecraft` namespace, so `plains` and `minecraft:plains` are
equivalent. Examples:

```yaml
allowedBiomes:
  - 'minecraft:plains'
  - 'minecraft:desert'
  - 'minecraft:snowy_plains'
starter:
  biome: 'minecraft:cherry_grove'
  radius: 512
  land:
    enabled: true
    transition: 128
    foundationDepth: 32
```

```yaml
allowedBiomes:
  - '#minecraft:is_overworld'
starter:
  biome: ''
  radius: 512
```

Quote biome tags in YAML because an unquoted `#` begins a comment.

### Guaranteed starter land

A starter biome changes biome selection, but vanilla terrain noise can still
place that biome over deep ocean, aquifers, or extensive caves. With
`starter.land.enabled: true`, Worldz keeps natural high ground unchanged and
raises only insufficient columns above a baseline two blocks over sea level.
It adds rolling elevation derived from the original seabed and broad
seed-dependent vanilla noise, avoiding a uniformly flat starter shelf. Raised
land starts at the original ocean floor, so it is connected to the terrain
below instead of becoming a thin floating platform.

The full guarantee covers `starter.radius`. Beyond it,
`starter.land.transition` uses a smooth circular blend back to the original
terrain height. The blend applies to vertical lift, and tiny lifts round to zero
near its outer edge instead of producing a one-block ring. Vanilla surface
rules still add the starter biome's normal grass, dirt, sand, or other
materials. `starter.land.foundationDepth`
repairs deep air or water gaps below the original floor while leaving a surface
shell available for natural cave openings. Set `starter.land.enabled: false` to
retain completely vanilla terrain shapes beneath the forced biome.

These settings are available from the **Starter Land** button in Customize.
They affect only the Overworld; Nether and End terrain are unchanged.

### Limited-world borders

Borders use Minecraft's visible square world border centered at `(0, 0)`.
Radius values are the distance from the center to each side, so a radius of
`512` creates a `1024 × 1024` playable square. Borders affect only newly
created Worldz worlds.

The config file and internal storage always use blocks. The Customize
screen's border and exterior editors have a **Radius units** button that
displays and accepts either blocks or chunks (1 chunk = 16 blocks); switching
it converts whatever is currently typed instead of reinterpreting the digits.

```yaml
overworldBorder:
  enabled: true
  initialRadius: 512
  finalRadius: 2048
  resize:
    days: 100
    delayDays: 0
    style: continuous
    rate:
      blocks: 0
      days: 0
  ensureEndPortal: true
netherBorder:
  enabled: true
  initialRadius: 256
  finalRadius: 512
  resize:
    days: 100
    delayDays: 0
    style: continuous
    rate:
      blocks: 0
      days: 0
  ensureBlazeAccess: true
```

Equal initial and final radii make a static border. A larger final radius grows
linearly; a smaller one shrinks. With no delay, `resize.days: 0` applies the final
radius immediately. The transition uses elapsed Minecraft game time and resumes
rather than restarting when the save is reopened.

`resize.delayDays` holds the initial radius before any growth or collapse begins.
For example, a collapsing world can start large for 30 days and then shrink over
the configured duration. Only in-game server ticks count: closing the world
pauses both the delay and transition. If `resize.days` and the rate fields are
zero, the border jumps to its final radius when the delay expires.

Set both rate fields to resize by a distance over an interval. For example,
`resize.rate.blocks: 64` and `resize.rate.days: 5` changes the radius continuously
at 64 blocks every five Minecraft days. A positive rate pair overrides
`resize.days`; leave both rate fields at zero to use the total duration. The last
partial interval is scaled proportionally, so the border stops exactly at the
configured final radius.

`resize.style` picks how the rate fields are applied: `continuous` (the
default) smooths the same distance into one gradual lerp, as above. `stepped`
instead jumps abruptly by `resize.rate.blocks` every `resize.rate.days`, with the
border holding still in between -- for example `resize.rate.blocks: 1` and
`resize.rate.days: 1` reveals one more block of radius each day. `stepped`
requires a positive rate pair; without one it's ignored and treated as
`continuous`. The Customize screen's border editor has a **Resize style**
button that toggles between the two.

#### Carrying the border into the End

```yaml
endBorder:
  carryFromOverworld: true
  minimumRadius: 256
```

With `carryFromOverworld: true` and an enabled `overworldBorder`, the End also
gets a static square border centered at `(0, 0)`, sized to the larger of the
Overworld's eventual (final) radius and `minimumRadius` -- so a very
small Overworld border does not shrink the End border below a size that keeps
the main island, every obsidian pillar, and the exit portal reachable and
intact. The End border does not resize over time and does not respect
`resize.delayDays`; it is set once, at world creation, to its eventual size.
With no Overworld border enabled, or `carryFromOverworld: false`, the End
stays completely unbordered. End terrain generation itself is always
untouched vanilla -- this only limits how far a player can fly from the
main island.

### Ocean and void exteriors

Exterior terrain is independent of the world border. It can create a genuinely
finite landmass even without a border, or it can sit behind a border so players
cannot reach the generated ocean or void. The square is centered at `(0, 0)`.

```yaml
overworldExterior:
  mode: ocean
  boundaryRadius: 2048
  oceanTransitionWidth: 256
netherExterior:
  mode: void
  boundaryRadius: 512
  oceanTransitionWidth: 0
```

`boundaryRadius` is the outer radius. Set it to `0` (`auto` in Customize)
to use the larger of the border's initial and final radii; automatic mode needs
an enabled border. With ocean mode, the transition begins inward by
`oceanTransitionWidth`, making that much ocean accessible before the
outer boundary. Deep ocean with a solid seabed continues indefinitely beyond
the boundary. Void mode produces empty columns indefinitely. Nether supports
normal and void only; the End always retains vanilla generation.

When a progression guarantee is enabled, Worldz uses a natural stronghold or
Nether fortress if one fits safely inside both the final border and solid
terrain — for the Overworld End portal, a coordinated `layout` also rejects a
natural stronghold sitting on planned ocean. Otherwise it creates
a compact fallback near `(32, 0)`: a visible surface End-portal frame in the
overworld, or an enclosed nether-brick blaze-spawner room at approximately
`(32, 64, 0)` in the Nether. If `(32, 0)` itself is planned ocean, the
Overworld fallback tries a few nearby points before falling back to `(32, 0)`
unchanged. The fallback portal contains no eyes. Exact
coordinates are written to the game log. An exterior-only world also receives
the guarantee even when its border is disabled. Fallback sites are placed within
the tightest configured bound, so they may become reachable before a growing
border finishes its schedule.

Syntax errors use safe defaults and leave the broken file untouched. Invalid
list entries are logged and removed; unknown biome or tag ids are logged when a
new Worldz world is created. If none of the configured biomes can be used, the
mod falls back to the full vanilla overworld biome list so world creation still
succeeds.

Configuration is baked into a Worldz world's saved biome source when that world
is created. Later config edits affect only newly created worlds; reopening an
existing Worldz world keeps its original biome list, starter biome, starter
radius and land plan, border schedules (including pending delays), and exterior
envelopes. Worlds created before starter-land support decode with reinforcement
disabled, so unexplored chunks in those saves do not change shape.
Starter-land profile revisions are also baked into the save: worlds made with
0.1.4 or earlier retain the legacy profile, while fresh 0.1.5 worlds use rolling
relief. Create a new world to evaluate the newer terrain algorithm.

Every newly created world also gets a `jlt_worldz-snapshot.yaml` file written
into its save folder (alongside `level.dat`), recording the settings it was
created with. It is a reference for your own notes and reproducibility only —
the mod never reads it back, and editing it has no effect; the world's actual
generator settings are the codec-encoded values described above.

## How biome limiting works

Worldz filters vanilla's overworld multi-noise climate map to the allowed
biomes. Minecraft still chooses the closest climate entry at every position,
which attempts to preserve climate-shaped regions when several biomes are
allowed but does not coordinate them with broad terrain shape. The
starter biome overrides the entire vertical column inside its circular zone.
For an ocean exterior, Worldz reports the deep-ocean biome outside the solid
square so spawning and climate behavior match the generated water.

### Current terrain-composition limitation (legacy mode only)

With `layout.mode: legacy` (still the default), biome limiting does not change
vanilla continentalness or density. On a seed whose nearby vanilla terrain is a
large ocean, an allowed land biome can therefore be reported over submerged
terrain. Guaranteed starter land corrects only its configured central radius
and transition; it does not rebalance the infinite world. Selecting any other
`layout.mode` resolves this by making biome and terrain height decisions
together instead (see below).

### Coordinated world layouts

A `layout` section (`mode`, weighted `biomes`, `regionScale`,
`singleBiome`, `roleOverrides`) selects one of three modes: `ocean`,
`single_biome`, or `void`, plus the `legacy` default described above.
Non-legacy modes classify every column into a role from a deterministic
seeded grid, choose a weighted biome within that role, and raise or lower
terrain to match — so an ocean world's biomes generate real (lowered) ocean
rather than a land shape mislabeled underwater. `mode` requires at least one
usable biome for every role it needs; an incomplete configuration logs a
warning and falls back to `legacy` rather than failing world creation. The
**Layout** button in Customize exposes every one of these fields for a single
new world without editing the YAML file.

`ocean` places a beach biome in a ring just outside a configured starter zone
when one is available, so the guaranteed starter island tapers through a
beach before reaching open water instead of cutting off abruptly; the
starter-land transition itself blends toward the layout's own adjusted
terrain (e.g. the capped ocean depth) rather than unrelated raw vanilla
shape. `void` forces a sky-void exterior around the starter zone (radius plus
its transition width, or a 256-block fallback with no starter biome
configured) so a guaranteed island floats in an otherwise infinite void —
configure a starter biome and enable starter land for a solid island; without
one, whatever vanilla terrain naturally exists in that fallback radius is
what floats. `single_biome` fills the world with one biome and raises or caps
its terrain to match that biome's role. See TODO.md for the region-composed
(land+ocean-in-one-world) layouts removed in 0.2.0 and their planned
ocean-island replacement.

Non-legacy `layout.biomes` picks **one** biome for an entire region cell (up
to `regionScale` — 512 blocks by default — on a side), which suits
broad biomes like `plains`/`forest`/`ocean` but not a biome vanilla only ever
generates as a narrow, winding, noise-carved channel, like `river`. Don't add
`river` (or similarly linear/thin biomes) to `layout.biomes`.

### Seed-informed spawn

The `spawn.strategy` setting (also exposed as a cycle button in Customize)
chooses how the layout origin and initial spawn point are picked for a
newly created world:

- `starter_at_origin` (default) — the layout origin stays `(0, 0)`; the
  starter biome and starter land, if configured, are forced there and
  vanilla's own surface-height spawn search runs inside that guaranteed area.
- `preferred_natural_biome` — searches outward from `(0, 0)` using the real
  world seed for a natural, unmodified occurrence of `starter.biome`, in
  concentric rings up to 2048 blocks out. If found, that location becomes the
  new layout origin: the starter zone, guaranteed land, world border,
  exterior terrain, and progression objectives (End portal / blaze access)
  all move there together, not just the player's spawn point. If nothing
  matches within range, or `starter.biome` is empty, the world falls back to
  `starter_at_origin` automatically.
- `vanilla_spawn` — the layout origin stays `(0, 0)`, and Worldz leaves
  Minecraft's ordinary spawn selection untouched.

The search and recentering only run once, when a world is first created; the
resolved origin is then saved and re-applied on every later load, so it
survives server restarts.

## Caveats

- Structures follow their allowed biomes. A plains-only world has no ocean
  monuments or jungle temples; villages, strongholds, and other structures can
  appear only where their own biome rules permit.
- Allowed biomes need vanilla overworld climate entries. Nether, End, and
  special biomes in `allowedBiomes` are ignored with a warning. Fixed-source
  support for those biomes is deferred.
- Existing worlds are never modified, and config changes do not alter worlds
  already created with Worldz.
- Nether terrain remains vanilla unless its exterior mode is set to void. End
  generation always remains vanilla.
- Worldz does not provide an in-game reload command; existing worlds remain
  independent of both the YAML file and later Customize choices.
- Shrinking a border does not delete chunks that were previously generated; it
  makes the area outside the new border inaccessible.
- Exterior boundaries are baked into generation and do not grow or shrink with
  the border. Use an automatic boundary to cover the largest scheduled border.

## Building and testing

Requires Java 25. Build both loader artifacts and run all tests with:

```bash
./gradlew build
```

Artifacts are written to:

- `fabric/build/libs/jlt_worldz-fabric-26.2-<version>.jar`
- `neoforge/build/libs/jlt_worldz-neoforge-26.2-<version>.jar`

The common test suite covers config handling, biome/tag syntax, climate-entry
filtering, starter-zone and exterior boundary math, border rates, ocean column
profiles, starter-land blending and persistence, progression bounds,
customization wiring, and preset resources.
Manual acceptance should create fresh Worldz worlds on both loaders and confirm
terrain and border behavior in-game; see [`TODO.md`](TODO.md) for the checklist.

## License

MIT
