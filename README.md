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

1. (Optional) Create `config/jlt_worldz.yaml` if you want different reusable
   defaults than the ones built into the mod — copy from
   [`config/jlt_worldz.example.yaml`](config/jlt_worldz.example.yaml), which
   documents every setting with comments, then restart Minecraft. No config
   file at all is a normal, supported setup: the mod's own defaults apply
   directly and it never creates one for you.
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
`config/jlt_worldz.yaml`:

```yaml
singleBiome:
  landBiome: 'minecraft:desert'
  starterBiome: 'minecraft:plains'
  starterRadiusBlocks: 256
  spawn:
    strategy: starter_at_origin
  allowRivers: false
  allowOceans: false
  allowBeaches: false
```

| Setting | Default | Description |
|---|---|---|
| `landBiome` | `"minecraft:plains"` | The one biome that fills the entire world. |
| `starterBiome` | `""` | Optional different biome forced in a circular zone around spawn; empty means no forced zone (the whole world is already `landBiome`). |
| `starterRadiusBlocks` | `256` | Starter-zone radius, only meaningful when `starterBiome` is set; clamped to `64..4096`. |
| `spawn.strategy` | `starter_at_origin` | Same three values as the shared [Seed-informed spawn](#seed-informed-spawn) setting. `preferred_natural_biome` searches for a *natural* occurrence of `starterBiome` using the real seed and moves spawn there instead of forcing a zone at `(0, 0)` — the way to get a starter biome whose location (and, incidentally, whatever natural shape it has) comes from the seed rather than being placed arbitrarily. |
| `allowRivers` | `false` | Let vanilla's own river biomes generate wherever vanilla would naturally place one, instead of `landBiome` applying there too. Terrain is untouched — the river channel is exactly vanilla's shape. Never overrides the starter zone, which always stays guaranteed land. |
| `allowOceans` | `false` | Same idea for vanilla's own ocean biomes (every temperature and depth variant) — additive over `allowRivers`, so turning this on keeps rivers passing through too. Coastlines are exactly vanilla's: no straight edges, no height blending. |
| `allowBeaches` | `false` | Same idea for vanilla's own `beach`/`snowy_beach` biomes plus `stony_shore` (which has no dedicated vanilla tag, so it's checked directly) — independent of `allowRivers`/`allowOceans`. |

`allowedBiomes` (what structures/features see as possible biomes) is derived
automatically from `landBiome` and `starterBiome` — there is nothing to keep
in sync by hand for this type. When `allowRivers`/`allowOceans`/`allowBeaches`
are on, the matching vanilla biomes (`#minecraft:is_river`,
`#minecraft:is_ocean`, `#minecraft:is_beach` plus `stony_shore`) are folded
in too, so structure/feature placement knows those biomes can occur.

## Chaos biomes challenge

Select **Worldz: Chaos Biomes** under **World Type** for a world where a
list of land biomes is shuffled, seed-based, across regions of the map —
desert beside ice spikes beside jungle, changing every `regionScaleBlocks`
or so. Unlike **Worldz: Single Biome**, terrain shape is *always* completely
untouched vanilla terrain everywhere (hills, mountains, ravines, natural
water bodies stand exactly as the seed generated them) — chaos only ever
relabels biome identity, never height. Its Customize screen also has the
same shared Overworld/Nether Border, End Border, and Overworld/Nether
Exterior buttons as `single_biome` and the generic preset.

Configure its defaults with a `chaosBiomes:` section in
`config/jlt_worldz.yaml`:

```yaml
chaosBiomes:
  biomes:
    - 'minecraft:desert'
    - 'minecraft:jungle'
    - 'minecraft:ice_spikes'
    - 'minecraft:badlands'
    - 'minecraft:taiga'
  regionScaleBlocks: 512
  starterBiome: ''
  starterRadiusBlocks: 256
  spawn:
    strategy: starter_at_origin
  allowRivers: false
  allowOceans: false
  allowBeaches: false
```

| Setting | Default | Description |
|---|---|---|
| `biomes` | desert/jungle/ice_spikes/badlands/taiga | Weighted land biome entries (`id` or `id@weight`, same syntax as the generic preset's `layout.biomes`), shuffled per region. At least one is required. |
| `regionScaleBlocks` | `512` | Grid-cell edge length in blocks; smaller means more frequent biome changes. Clamped to `16..8192`. |
| `starterBiome` | `""` | Optional biome forced in a circular zone around spawn; empty means chaos starts immediately at spawn (GOALS 33's literal reading). Setting one gives a safe, guaranteed-land starting patch, exactly like `single_biome`'s. |
| `starterRadiusBlocks` | `256` | Starter-zone radius, only meaningful when `starterBiome` is set; clamped to `64..4096`. |
| `spawn.strategy` | `starter_at_origin` | Same three values as the shared [Seed-informed spawn](#seed-informed-spawn) setting. |
| `allowRivers` | `false` | Same mechanism as `single_biome`'s `allowRivers` — vanilla's own river biomes pass through, terrain untouched. |
| `allowOceans` | `false` | Same mechanism as `single_biome`'s `allowOceans` — additive over `allowRivers`. |
| `allowBeaches` | `false` | Same mechanism as `single_biome`'s `allowBeaches` — vanilla's own beach/stony-shore biomes pass through, independent of `allowRivers`/`allowOceans`. |

`allowedBiomes` derives automatically from `biomes` plus `starterBiome`, the
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

Configure it with the shared top-level `strip:` section (the corridor width
applies the same way regardless of which Worldz preset reads it — see
[Limited-world borders](#limited-world-borders) for how `overworldBorder`/
`netherBorder` supply the corridor's length) plus a `stripWorld:` section
for this preset's own defaults:

```yaml
strip:
  enabled: true
  widthRadiusBlocks: 64
  widthMode: void
  applyToNether: false
stripWorld:
  spawn:
    strategy: starter_at_origin
```

| Setting | Default | Description |
|---|---|---|
| `strip.enabled` | `false` | Whether the corridor's width constraint applies. |
| `strip.widthRadiusBlocks` | `32` | Half-width from the origin; the corridor is twice this wide. Clamped to `1..14999992`. |
| `strip.widthMode` | `void` | Terrain generated beyond the width: `void` or `ocean` (never `normal`). |
| `strip.applyToNether` | `false` | Whether the same corridor width also applies to the Nether — one shared width, not two independently configurable ones. |
| `stripWorld.spawn.strategy` | `starter_at_origin` | Same three values as the shared [Seed-informed spawn](#seed-informed-spawn) setting. |

The corridor's length, End border, and exteriors all use the same shared
`overworldBorder`/`netherBorder`/`endBorder`/`overworldExterior`/
`netherExterior` sections every other world type reads, and are also
available on this preset's Customize screen.

### Biome bands (GOALS 36)

Optionally, instead of full vanilla biome variety, the corridor can walk an
ordered sequence of biomes along its length — desert, then jungle, then ice
spikes, and so on, changing every `bands.widthBlocks` — the same
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
    widthBlocks: 128
    seedRandomOrder: false
    allowRivers: true
    allowOceans: true
    allowBeaches: true
```

| Setting | Default | Description |
|---|---|---|
| `bands.enabled` | `false` | Whether the corridor passes through biome bands instead of ordinary vanilla terrain. |
| `bands.biomes` | desert/jungle/ice_spikes/badlands/taiga | Ordered, unweighted biome ids walked along the strip's length. Concrete ids only, no `#tags`. At least one is required when `enabled` is set. |
| `bands.widthBlocks` | `128` | Band width in blocks along the strip's length axis. Clamped to `16..8192`. |
| `bands.seedRandomOrder` | `false` | Shuffle `biomes` once — a fixed permutation baked in at world creation, not per-band randomness — instead of using the list in the order given. |
| `bands.allowRivers` | `true` | Let vanilla's own river biomes pass through the band sequence wherever vanilla would naturally place one, same mechanism as `single_biome`'s `allowRivers`. Defaults **on** here — unlike `single_biome`/`chaos_biomes`, a band sequence is already a curated, restricted list, so without this a player would need to remember to add water biomes to every band configuration just to get them at all. |
| `bands.allowOceans` | `true` | Same idea for vanilla's own ocean biomes — additive over `allowRivers`. Also defaults **on** for the same reason. |
| `bands.allowBeaches` | `true` | Same idea for vanilla's own beach/stony-shore biomes, independent of `allowRivers`/`allowOceans`. Also defaults **on** for the same reason. |

These three only matter when `bands.enabled` is set — the plain, band-free
strip world already shows full vanilla biome variety (including rivers,
oceans, and beaches) with nothing to configure.

## Ocean island challenge

Select **Worldz: Ocean Island** under **World Type** for a small island
surrounded by an endless generated ocean. Unlike every other typed preset,
there is no spawn-strategy option — the island only ever exists at the
origin, so spawn is always the island's own safe surface point near
`(0, 0)`. `islandSource` (in Customize, or `oceanIsland.islandSource` in
config) picks between three ways of sourcing the land itself:

- **`artificial`** (default, GOALS 01) — a natural-looking synthetic island
  of one chosen biome, described below.
- **`natural`** (GOALS 02) — searches the seed's own unmodified terrain for
  a small, isolated real landmass and centers the world there instead;
  nothing about the land itself is synthesized — the real biome and terrain
  show through completely unmodified within `radiusBlocks`, and the same
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
  disallows placing them on lava); travel is by strider or bridging.
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
`config/jlt_worldz.yaml`:

```yaml
oceanIsland:
  islandSource: artificial
  fluid: water
  islandBiome: 'minecraft:plains'
  radiusBlocks: 128
  shapeAmplitude: 0.3
  shoreWidthBlocks: 12
  oceanShallowWidthBlocks: 64
  oceanDeepenWidthBlocks: 128
  oceanShallowDepthBlocks: 8
  oceanDeepDepthBlocks: 32
  oceanRegionScaleBlocks: 128
  exclusionZoneEnabled: false
  exclusionZoneRadiusBlocks: 2000
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

| Setting | Default | Description |
|---|---|---|
| `islandSource` | `"artificial"` | `artificial`, `natural`, or `chest_boat` — see above. |
| `fluid` | `"water"` | `water`, `lava`, or `none` — see above. Independent of `islandSource`. |
| `islandBiome` | `"minecraft:plains"` | The one biome that fills the island's interior (`artificial` only). |
| `radiusBlocks` | `128` | Configured (unperturbed) island radius (`artificial`), or the search-isolation/land radius around whatever the search finds (`natural`). Clamped to `8..65536` — deliberately far below the shared starter-radius bounds other presets use, since GOALS 01 explicitly wants sizes down to "16 blocks/1 chunk." |
| `shapeAmplitude` | `0.3` | Coastline perturbation strength as a fraction of the radius (`artificial` only). `0` is a perfect circle; clamped to `0..0.6` so no combination of harmonics can produce a self-intersecting or negative-radius shape. |
| `shoreWidthBlocks` | `12` | Width of the beach/stony-shore ring measured from the true coastline; also the terrain-height taper width from the island's full guaranteed height down to sea level. |
| `oceanShallowWidthBlocks` | `64` | Width of the shallow ocean band immediately beyond the shore ring. |
| `oceanDeepenWidthBlocks` | `128` | Width over which the seabed smoothly ramps from shallow to deep. |
| `oceanShallowDepthBlocks` | `8` | Seabed depth below sea level in the shallow band. |
| `oceanDeepDepthBlocks` | `32` | Seabed depth below sea level once fully deep — the ocean stays this deep forever beyond the deepening band ("endless ocean"). |
| `oceanRegionScaleBlocks` | `128` | Grid-cell edge length for the ocean biome's per-region pick, so the ocean reads as patches of variety rather than per-block dithering. |
| `exclusionZoneEnabled` | `false` | GOALS 04: when set, island/ocean shaping releases entirely beyond `exclusionZoneRadiusBlocks`, letting the seed's own natural terrain resume — small natural islands then occur wherever the seed's terrain noise happens to poke above sea level, far from the artificial island. Off by default, matching GOALS 01's core "endless ocean, no natural land ever" behavior. |
| `exclusionZoneRadiusBlocks` | `2000` | Radius beyond which shaping releases, when enabled. |
| `starterKit.essentials` | lily pad, dirt x4, grass block x2, oak sapling x3 | Always-included chest-boat items (`chest_boat` only). Each entry is `"<item id>"` (count 1) or `"<item id>:<count>"`. |
| `starterKit.extras` | bread x3, wooden axe, wooden pickaxe, torch x8, water bucket | Candidate items the random picks draw from (`chest_boat` only, same shorthand format). |
| `starterKit.extrasCount` | `2` | How many extras to pick, with replacement, deterministically from the world seed. |

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
footprint, the slab runs from `surfaceY` down `thicknessBlocks` deep: solid
ground (grass/dirt over stone, or a biome-appropriate variant — sand over
sandstone for desert-family biomes, snow over dirt for snowy ones, mycelium
for mushroom fields), then open void below. Dig straight down through the
slab and you fall out the bottom; walk off the edge and you fall forever.
Nothing generates naturally anywhere else in the Overworld — no trees, no
mobs, no structures — since the whole point is starting with only what's
in the chest.

A necessities chest appears on the island at world creation, stocked
according to `chestTier` (`easy`, `medium`, or `hard` — in Customize, or
`skyIsland.chestTier` in config): each tier has its own configurable
essentials/extras list (see `easyKit`/`mediumKit`/`hardKit` below), and
every tier is intended to remain beatable given enough time. The chest
always additionally includes exactly one water-source item, chosen from
the island's biome: a water bucket for a dry, desert-family biome (which
never gets rain, so a cauldron there would never fill), or a cauldron for
every other biome (rain will fill it naturally over time).

`applyToNether` (in Customize: "Also make the Nether a sky island", or
`skyIsland.applyToNether` in config) mirrors the exact same
radius/coastline-shape/surfaceY/thicknessBlocks shape into the Nether. The
Nether's island has no biome concept of its own — its surface is always
netherrack-over-netherrack with a basalt core, regardless of the
Overworld's `islandBiome`. The End is unaffected either way: vanilla End
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
`config/jlt_worldz.yaml`:

```yaml
skyIsland:
  islandBiome: 'minecraft:plains'
  radiusBlocks: 16
  shapeAmplitude: 0.3
  surfaceY: 64
  thicknessBlocks: 6
  chestTier: medium
  applyToNether: false
  easyKit:
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
  mediumKit:
    essentials:
      - 'minecraft:oak_sapling:3'
      - 'minecraft:bread:4'
    extras:
      - 'minecraft:wooden_pickaxe:1'
      - 'minecraft:torch:8'
      - 'minecraft:cobblestone:16'
    extrasCount: 2
  hardKit:
    essentials:
      - 'minecraft:oak_sapling:2'
    extras:
      - 'minecraft:bread:2'
      - 'minecraft:torch:4'
    extrasCount: 1
```

| Setting | Default | Description |
|---|---|---|
| `islandBiome` | `"minecraft:plains"` | The one biome that fills the island's interior and drives its surface-block palette and starter-kit water item. |
| `radiusBlocks` | `16` | Configured (unperturbed) island radius. Clamped to `8..65536`, the same shared bound ocean island uses — Skyblock scale by default, much smaller than ocean island's own 128-block default. |
| `shapeAmplitude` | `0.3` | Coastline perturbation strength as a fraction of the radius. `0` is a perfect circle; clamped to `0..0.6`. |
| `surfaceY` | `64` | The island's walkable surface Y — GOALS 05's own default, chosen to avoid slimes. |
| `thicknessBlocks` | `6` | How many blocks of solid ground extend below `surfaceY` before hitting void. Clamped to `1..64`. |
| `chestTier` | `"medium"` | `easy`, `medium`, or `hard` — which of `easyKit`/`mediumKit`/`hardKit` the starter chest uses. |
| `applyToNether` | `false` | Mirrors this exact shape into the Nether too (GOALS 06). |
| `easyKit`/`mediumKit`/`hardKit` | see above | Each has its own `essentials`/`extras`/`extrasCount`, same shorthand format as ocean island's `starterKit`. |

### Floating resource islands (GOALS 07-08)

Enable `skyIsland.floatingIslands` to fill the void beyond the starter
island with scattered small floating islands instead of leaving it empty —
a jittered grid of cells, each independently rolling whether it holds an
island (`spawnChance`), with a hash-picked center offset, radius
(`minRadiusBlocks`..`maxRadiusBlocks`), and coastline shape reusing the
exact same perturbation as every other island shape in this mod. A
configurable void buffer (`exclusionZoneEnabled`/`exclusionZoneRadiusBlocks`)
keeps the immediate area around the starter island empty before scattered
islands begin, so reaching them always takes real bridging.

```yaml
skyIsland:
  floatingIslands:
    enabled: false
    minRadiusBlocks: 12
    maxRadiusBlocks: 32
    shapeAmplitude: 0.3
    cellSizeBlocks: 256
    spawnChance: 0.6
    biomeVariety: true
    islandBiomes:
      - 'minecraft:plains'
      - 'minecraft:forest'
      - 'minecraft:desert'
      - 'minecraft:taiga'
      - 'minecraft:savanna'
    exclusionZoneEnabled: true
    exclusionZoneRadiusBlocks: 256
    oreDepositsEnabled: false
    oreFeatureIds:
      - 'minecraft:ore_coal'
      - 'minecraft:ore_iron_small'
      - 'minecraft:ore_gold_buried'
      - 'minecraft:ore_redstone'
      - 'minecraft:ore_lapis'
      - 'minecraft:ore_diamond_small'
      - 'minecraft:ore_emerald'
    lootChestEnabled: false
    lootKit:
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

| Setting | Default | Description |
|---|---|---|
| `enabled` | `false` | Whether scattered floating islands generate at all. |
| `minRadiusBlocks`/`maxRadiusBlocks` | `12`/`32` | Range a scattered island's radius is hash-picked from. Same shared `8..65536` bound as every other island radius. |
| `shapeAmplitude` | `0.3` | Coastline perturbation strength, same shape as the starter island's own. |
| `cellSizeBlocks` | `256` | Grid-cell edge length — the primary "how far apart" knob. |
| `spawnChance` | `0.6` | Probability (`0..1`) that a given grid cell holds an island, independent of spacing. |
| `biomeVariety` | `true` | Whether each island hash-picks its own biome from `islandBiomes`, instead of every scattered island sharing the starter island's single `islandBiome`. |
| `islandBiomes` | plains/forest/desert/taiga/savanna | Candidate biome pool when `biomeVariety` is `true`. Concrete biome ids only, no `#tags`. |
| `exclusionZoneEnabled`/`exclusionZoneRadiusBlocks` | `true`/`256` | Void buffer around the starter island before scattered islands begin. |
| `oreDepositsEnabled` | `false` | Whether each island gets one embedded vanilla ore-vein feature, hash-picked from `oreFeatureIds` and placed once at the island's own center, clamped to its slab's thickness. |
| `oreFeatureIds` | coal/iron/gold/redstone/lapis/diamond/emerald | Candidate vanilla `ConfiguredFeature` ids (config-only, like `easyKit`/`mediumKit`/`hardKit` above — not exposed on the Customize screen). |
| `lootChestEnabled` | `false` | Whether each island gets one placed loot chest at the island's surface, reusing `lootKit`. |
| `lootKit` | bread + iron/emerald/arrow/golden-apple/ender-pearl extras | Same `essentials`/`extras`/`extrasCount` shape as `easyKit`/`mediumKit`/`hardKit` above (config-only, not exposed on the Customize screen). |

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

## Configuration

The mod reads `config/jlt_worldz.yaml` at startup if present; it is entirely
optional and the mod never creates or requires one. These values are the
defaults for the singleplayer Customize screen and the direct inputs for
dedicated-server world creation. A complete, comment-documented reference —
the way to discover every available setting — lives at
[`config/jlt_worldz.example.yaml`](config/jlt_worldz.example.yaml); copy the
parts you want into your own `config/jlt_worldz.yaml`.

| Setting | Default | Description |
|---|---|---|
| `allowedBiomes` | desert/badlands/cave mix | Biome ids and/or `#` biome-tag ids. A single biome produces a single-biome overworld. See [`config/jlt_worldz.example.yaml`](config/jlt_worldz.example.yaml) for the exact default list. |
| `starterBiome` | `"minecraft:plains"` | Biome id forced around the origin; empty disables the starter zone. Tags are not accepted here. |
| `starterRadiusBlocks` | `256` | Inclusive circular radius, clamped to `64..4096` blocks. |
| `ensureStarterLand` | `true` | Raise low natural terrain beneath a selected starter biome; has no effect when the starter biome is empty. |
| `starterLandTransitionBlocks` | `128` | Smooth blend beyond the starter radius back to natural terrain, clamped to `0..4096`. |
| `starterLandFoundationDepthBlocks` | `48` | Depth repaired below the natural ocean floor, clamped to `0..384`. |
| `overworldBorder` | disabled | Optional square overworld border and resize schedule. |
| `netherBorder` | disabled | Optional independent Nether border and resize schedule. |
| `endBorder` | disabled | Option to carry the Overworld's eventual radius into the End, clamped up to `minimumRadiusBlocks` so the dragon fight stays winnable. |
| `overworldExterior` | normal | Terrain outside a central square: `normal`, `ocean`, or `void`. |
| `netherExterior` | normal | Nether terrain outside a central square: `normal` or `void`. |
| `layout` | `legacy` | Coordinated land/ocean/beach terrain layout; `legacy` keeps today's climate-filter-only behavior. See [Coordinated world layouts](#coordinated-world-layouts). |
| `spawn` | `strategy: starter_at_origin` | How the layout origin and initial spawn are chosen: `starter_at_origin` (today's behavior), `preferred_natural_biome` (search near the origin for `starterBiome` using the real seed and move the layout origin there), or `vanilla_spawn` (unmodified vanilla spawn selection). See [Seed-informed spawn](#seed-informed-spawn). |

Short ids use the `minecraft` namespace, so `plains` and `minecraft:plains` are
equivalent. Examples:

```yaml
allowedBiomes:
  - 'minecraft:plains'
  - 'minecraft:desert'
  - 'minecraft:snowy_plains'
starterBiome: 'minecraft:cherry_grove'
starterRadiusBlocks: 512
ensureStarterLand: true
starterLandTransitionBlocks: 128
starterLandFoundationDepthBlocks: 32
```

```yaml
allowedBiomes:
  - '#minecraft:is_overworld'
starterBiome: ''
starterRadiusBlocks: 512
```

Quote biome tags in YAML because an unquoted `#` begins a comment.

### Guaranteed starter land

A starter biome changes biome selection, but vanilla terrain noise can still
place that biome over deep ocean, aquifers, or extensive caves. With
`ensureStarterLand: true`, Worldz keeps natural high ground unchanged and raises
only insufficient columns above a baseline two blocks over sea level. It adds
rolling elevation derived from the original seabed and broad seed-dependent
vanilla noise, avoiding a uniformly flat starter shelf. Raised land starts at
the original ocean floor, so it is connected to the terrain below instead of
becoming a thin floating platform.

The full guarantee covers `starterRadiusBlocks`. Beyond it,
`starterLandTransitionBlocks` uses a smooth circular blend back to the original
terrain height. The blend applies to vertical lift, and tiny lifts round to zero
near its outer edge instead of producing a one-block ring. Vanilla surface
rules still add the starter biome's normal grass, dirt, sand, or other
materials. `starterLandFoundationDepthBlocks`
repairs deep air or water gaps below the original floor while leaving a surface
shell available for natural cave openings. Set `ensureStarterLand: false` to
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
  initialRadiusBlocks: 512
  finalRadiusBlocks: 2048
  resizeDays: 100
  resizeDelayDays: 0
  resizeRateBlocks: 0
  resizeRateDays: 0
  ensureEndPortal: true
netherBorder:
  enabled: true
  initialRadiusBlocks: 256
  finalRadiusBlocks: 512
  resizeDays: 100
  resizeDelayDays: 0
  resizeRateBlocks: 0
  resizeRateDays: 0
  ensureBlazeAccess: true
```

Equal initial and final radii make a static border. A larger final radius grows
linearly; a smaller one shrinks. With no delay, `resizeDays: 0` applies the final
radius immediately. The transition uses elapsed Minecraft game time and resumes
rather than restarting when the save is reopened.

`resizeDelayDays` holds the initial radius before any growth or collapse begins.
For example, a collapsing world can start large for 30 days and then shrink over
the configured duration. Only in-game server ticks count: closing the world
pauses both the delay and transition. If `resizeDays` and the rate fields are
zero, the border jumps to its final radius when the delay expires.

Set both rate fields to resize by a distance over an interval. For example,
`resizeRateBlocks: 64` and `resizeRateDays: 5` changes the radius continuously
at 64 blocks every five Minecraft days. A positive rate pair overrides
`resizeDays`; leave both rate fields at zero to use the total duration. The last
partial interval is scaled proportionally, so the border stops exactly at the
configured final radius.

`resizeStyle` picks how the rate fields are applied: `continuous` (the
default) smooths the same distance into one gradual lerp, as above. `stepped`
instead jumps abruptly by `resizeRateBlocks` every `resizeRateDays`, with the
border holding still in between -- for example `resizeRateBlocks: 1` and
`resizeRateDays: 1` reveals one more block of radius each day. `stepped`
requires a positive rate pair; without one it's ignored and treated as
`continuous`. The Customize screen's border editor has a **Resize style**
button that toggles between the two.

#### Carrying the border into the End

```yaml
endBorder:
  carryFromOverworld: true
  minimumRadiusBlocks: 256
```

With `carryFromOverworld: true` and an enabled `overworldBorder`, the End also
gets a static square border centered at `(0, 0)`, sized to the larger of the
Overworld's eventual (final) radius and `minimumRadiusBlocks` -- so a very
small Overworld border does not shrink the End border below a size that keeps
the main island, every obsidian pillar, and the exit portal reachable and
intact. The End border does not resize over time and does not respect
`resizeDelayDays`; it is set once, at world creation, to its eventual size.
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
  boundaryRadiusBlocks: 2048
  oceanTransitionWidthBlocks: 256
netherExterior:
  mode: void
  boundaryRadiusBlocks: 512
  oceanTransitionWidthBlocks: 0
```

`boundaryRadiusBlocks` is the outer radius. Set it to `0` (`auto` in Customize)
to use the larger of the border's initial and final radii; automatic mode needs
an enabled border. With ocean mode, the transition begins inward by
`oceanTransitionWidthBlocks`, making that much ocean accessible before the
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

A `layout` section (`mode`, weighted `biomes`, `regionScaleBlocks`,
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
to `regionScaleBlocks` — 512 blocks by default — on a side), which suits
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
  world seed for a natural, unmodified occurrence of `starterBiome`, in
  concentric rings up to 2048 blocks out. If found, that location becomes the
  new layout origin: the starter zone, guaranteed land, world border,
  exterior terrain, and progression objectives (End portal / blaze access)
  all move there together, not just the player's spawn point. If nothing
  matches within range, or `starterBiome` is empty, the world falls back to
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
