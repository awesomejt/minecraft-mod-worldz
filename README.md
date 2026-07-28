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

1. (Optional) Create `config/jlt_worldz.yaml` if you want different reusable
   defaults than the ones built into the mod — copy from
   [`config/jlt_worldz.example.yaml`](config/jlt_worldz.example.yaml), which
   documents every setting with comments, then restart Minecraft. No config
   file at all is a normal, supported setup: the mod's own defaults apply
   directly and it never creates one for you. The mod never rewrites your
   `config/jlt_worldz.yaml` either — comments and settings you didn't set are
   preserved across every launch. Instead, it writes
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
| `undergroundBiome` | `""` (disabled) | Biome reported at/below `undergroundBelowSurfaceBlocks` blocks under `surfaceY`, within the island's own footprint (GOAL 42) — same single-fixed-biome shape as `flat`'s identical field. Blank disables the band entirely. Note the interaction with `thicknessBlocks`: with the defaults (`thicknessBlocks: 6`, `undergroundBelowSurfaceBlocks: 10`), the band starts *below* the slab's own solid ground (in the void beneath it) — for the band to fall within diggable ground, set `undergroundBelowSurfaceBlocks` smaller than `thicknessBlocks`. Config-only for now (not yet on the Customize screen). |
| `undergroundBelowSurfaceBlocks` | `10` | How many blocks below `surfaceY` the underground band starts. Only takes effect when `undergroundBiome` is also set; `0` disables the band even with a biome configured. |

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
A void buffer (`exclusionZoneEnabled`/`exclusionZoneRadiusBlocks`) keeps
the area immediately around the starter chunk empty before scattered
islands begin.

Each island independently keeps either its **entire natural column** (bedrock
to sky, `topOnly: false`) or **only its top slice** down to a configured
depth below its own real surface (`topOnly: true`,
`topOnlyDepthBlocks` — GOALS 09's own "like 5 deep to ensure access to
stone" example), voiding everything deeper. The cutoff follows each
column's own natural height, not a flat world-absolute Y. The starter
island uses the plan-wide `topOnly` setting deterministically; ordinary
scattered islands instead each independently hash-pick their own depth
mode via `scatteredTopOnlyChance` (GOALS 37 — "each island can
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

`applyToNether`/`applyToEnd` mirror the exact same chunk-grid mechanism
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
`config/jlt_worldz.yaml`:

```yaml
chunkIsland:
  enabled: false
  spawnChance: 0.35
  cellSizeChunks: 1
  topOnly: false
  topOnlyDepthBlocks: 5
  exclusionZoneEnabled: false
  exclusionZoneRadiusBlocks: 256
  scatteredTopOnlyChance: 0.0
  applyToNether: false
  applyToEnd: false
  geodeFeatureIds:
    - 'minecraft:amethyst_geode'
```

| Setting | Default | Description |
|---|---|---|
| `enabled` | `false` | Whether chunk islands generate at all. |
| `spawnChance` | `0.35` | Probability (`0..1`) that a given grid cell holds an island. |
| `cellSizeChunks` | `1` | Grid-cell edge length in chunks — `1` rolls every chunk independently; larger groups chunks into multi-chunk landmasses. |
| `topOnly` | `false` | Whether the starter island (and the guaranteed portal-room island) keep only their top `topOnlyDepthBlocks`, voiding everything below. |
| `topOnlyDepthBlocks` | `5` | Depth kept below the real generated surface whenever an island resolves top-only. |
| `exclusionZoneEnabled`/`exclusionZoneRadiusBlocks` | `false`/`256` | Void buffer around the starter island before scattered islands begin. |
| `scatteredTopOnlyChance` | `0.0` | Probability (`0..1`) an ordinary scattered island (not the starter, not the guaranteed portal room) independently resolves top-only instead of full-column. |
| `applyToNether`/`applyToEnd` | `false`/`false` | Mirrors the same chunk-grid mechanism into the Nether and/or the End. |
| `geodeFeatureIds` | `['minecraft:amethyst_geode']` | Candidate vanilla `ConfiguredFeature` ids the forced geode cell is hash-picked from (config-only, not exposed on the Customize screen). |

Not exposed on the Customize screen beyond the fields listed above —
`geodeFeatureIds` is YAML-only, matching every other variable-length
feature-id list in this mod's config.

## Cave challenge

Select **Worldz: Cave** under **World Type** for a cave-only start. Unlike
every other typed preset, the Overworld generates exactly as vanilla
would — full biome variety, real seed terrain, no shape or restriction of
any kind. The only change is where you spawn: a real natural cavity,
searched out near a configurable depth (`spawnDepthY`), rather than the
surface. Underground structures (mineshafts, dungeons, trial chambers, a
stronghold) generate normally so the game stays beatable, and the Nether is
reached by an ordinary portal built underground.

If no natural cavity is found near the configured depth within the search
budget, a small safe capsule is carved instead so world creation can never
fail to produce a safe spawn (check the server log for a warning if this
happens).

Two independent options layer on top:

- **Sealed surface** (`sealedSurface`/`sealedSurfaceY`): a solid roof caps
  the entire world at the configured Y, so the whole game is played
  underground with no sky access anywhere. Terrain above that height —
  ordinarily just tall mountain peaks — is deliberately clipped flat, not a
  bug. No sky access also means no phantoms.
- **Mega-cavern** (`cavernEnabled`/`cavernRadiusBlocks`/`cavernHeightBlocks`):
  a large, naturally-edged cavern carved around spawn — a buried "world in
  a cave" with room to build a base. The edge is perturbed (not a perfect
  sphere) using the same coastline-shaping math every other footprint in
  this mod shares, so it blends into whatever natural cave systems the seed
  already has there. The carve only ever turns solid blocks into air —
  existing air, water, lava, or natural caves already inside the footprint
  are left exactly as vanilla generated them.

An optional starter chest (`chestEnabled`/`chestTier`, easy/medium/hard) is
set into the floor directly beneath your spawn position — unlike every
other typed preset's chest, this one defaults to **off**.

Configure its defaults with a `cave:` section in `config/jlt_worldz.yaml`:

```yaml
cave:
  spawnDepthY: -32
  sealedSurface: false
  sealedSurfaceY: 128
  cavernEnabled: false
  cavernRadiusBlocks: 48
  cavernHeightBlocks: 24
  chestEnabled: false
  chestTier: medium
```

| Setting | Default | Description |
|---|---|---|
| `spawnDepthY` | `-32` | Target Y for the underground spawn-cavity search. |
| `sealedSurface` | `false` | Whether a solid roof seals off sky access everywhere. |
| `sealedSurfaceY` | `128` | The roof's Y, meaningful only when `sealedSurface` is set. |
| `cavernEnabled` | `false` | Whether the mega-cavern is carved around spawn. |
| `cavernRadiusBlocks` | `48` | The mega-cavern's horizontal half-width in blocks (`8`-`256`). |
| `cavernHeightBlocks` | `24` | The mega-cavern's vertical half-height in blocks (`8`-`256`). |
| `chestEnabled` | `false` | Whether a starter chest is placed at spawn. |
| `chestTier` | `medium` | Which starter-chest kit (`easy`/`medium`/`hard`) to use. |

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

| Setting | Default | Description |
|---|---|---|
| `capsule.sizeBlocks` | `7` | Total exterior footprint, walls included (must be odd); the room you actually stand in is this minus 2 — the default `7` means a 5x5 interior. |
| `capsule.heightBlocks` | `3` | Interior height (already "as seen from inside" — no adjustment needed). |
| `capsule.lightSource` | `glowstone` | `torch`, `lantern`, `soul_lantern`, `glowstone`, `shroomlight`, or `glow_lichen`. Torches/glowstone/shroomlight each light the north/east/west walls with one fixture centered on that wall (more, symmetric about the center, if the wall is long enough for the configured spacing); lanterns always hang from the ceiling in a grid instead; `glow_lichen` ignores spacing and coats the entire interior surface. The south wall (where the chest/furnace/crafting table sit) never gets a wall fixture. A room with either interior dimension at 6 or more also gets ceiling/floor lights in addition to the walls (floor-standing only for `torch` — there's no vanilla ceiling-mounted torch). |
| `capsule.lightSpacingBlocks` | `5` | Spacing between light fixtures (ignored by `glow_lichen`). |

**Death and respawn work like this:** the world's own default spawn point
is redirected to the resolved Nether site at world creation, so both your
very first join *and* any future death without a personal bed/respawn
anchor return you to that same safe site. Nether respawn anchors work
normally in the Nether (beds don't — vanilla's own rule); place and charge
one anywhere you like, and it overrides the default the same way a bed
would in the Overworld.

A difficulty-tiered starter chest (`chestTier`, easy/medium/hard — always
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
`config/jlt_worldz.yaml`:

```yaml
netherStart:
  spawnY: 32
  chestTier: medium
  forceCapsule: false
  capsule:
    sizeBlocks: 5
    heightBlocks: 3
    lightSource: glowstone
    lightSpacingBlocks: 5
```

| Setting | Default | Description |
|---|---|---|
| `spawnY` | `32` | Target Y for the safe-site search. |
| `chestTier` | `medium` | Which starter-chest kit (`easy`/`medium`/`hard`) to use. |
| `forceCapsule` | `false` | Always build the guaranteed capsule instead of only falling back to it when the natural search fails. |

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
difficulty-tiered starter chest (`chestTier`, easy/medium/hard) lines the
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
`config/jlt_worldz.yaml`:

```yaml
endStart:
  chestTier: medium
  capsule:
    sizeBlocks: 7
    heightBlocks: 3
    lightSource: glowstone
    lightSpacingBlocks: 5
```

| Setting | Default | Description |
|---|---|---|
| `chestTier` | `medium` | Which starter-chest kit (`easy`/`medium`/`hard`) to use. |
| `capsule.sizeBlocks` | `7` | Total *exterior* footprint width/depth, walls included (must be odd) — `7` means a 5x5 interior. |
| `capsule.heightBlocks` | `3` | Interior height (already "as seen from inside"). |
| `capsule.lightSource` | `glowstone` | `torch`, `lantern`, `soul_lantern`, `glowstone`, `shroomlight`, or `glow_lichen` — same placement rules as Nether-start's own `netherStart.capsule.lightSource` (see above). The south wall (where the chest sits) never gets a wall fixture. |
| `capsule.lightSpacingBlocks` | `5` | Spacing between light fixtures (ignored by `glow_lichen`). |

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
with a `flat:` section in `config/jlt_worldz.yaml` (the in-game Customize
screen edits the same fields as plain text):

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
```

| Setting | Default | Description |
|---|---|---|
| `layers` | 128 blocks total (bedrock, 123 stone, 3 dirt, grass) | Ordered bottom-to-top layer list, each entry `"<block id>"` (height 1) or `"<block id>:<height>"`. A bedrock floor is just whether the bottom entry is `minecraft:bedrock` — there is no separate toggle. |
| `biome` | `minecraft:plains` | The single biome reported everywhere. |
| `decoration` | `false` | Whether ordinary biome decoration (trees, flowers, ore veins, etc.) runs, matching vanilla flat's own all-or-nothing `features` flag. Structures are unaffected either way (0.3.7 fix, see below) — this only ever toggled tree/flower/ore-vein placement. |
| `structureOverrides` | `["minecraft:villages", "minecraft:strongholds"]` | Structure sets eligible to place; empty means every registered set is eligible, matching vanilla's own default. |
| `undergroundBiome` | `""` (disabled) | Biome reported at/below `undergroundBelowSurfaceBlocks` blocks under the surface (GOAL 42) — a single fixed biome, not sampled variety, matching `biome`'s own single-fixed-value design. Blank disables the band entirely; `biome` is then reported at every depth, unchanged from before this field existed. Config-only for now (not yet on the Customize screen). |
| `undergroundBelowSurfaceBlocks` | `10` | How many blocks below the flat surface the underground band starts. Only takes effect when `undergroundBiome` is also set; `0` disables the band even with a biome configured. |

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
`undergroundBiome`/`undergroundBelowSurfaceBlocks` report a *different*
biome below a configured depth, rather than carving a physical cavity into
the layer stack at all. Useful for biome-gated content (e.g. structures or
mob spawns that only apply underground) without needing an air pocket:

```yaml
flat:
  biome: minecraft:plains
  undergroundBiome: minecraft:dripstone_caves
  undergroundBelowSurfaceBlocks: 10
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

Configure it with a `deepFlat:` section in `config/jlt_worldz.yaml`:

```yaml
deepFlat:
  surfaceY: 64
  capLayers:
    - "minecraft:dirt:3"
    - "minecraft:grass_block:1"
  riversEnabled: true
  riverExclusionRadiusBlocks: 512
```

| Setting | Default | Description |
|---|---|---|
| `surfaceY` | `64` | The flat cap height — everything above clears to air, real terrain below stays. |
| `capLayers` | `["minecraft:dirt:3", "minecraft:grass_block:1"]` | Land-cap layer stack, painted immediately below `surfaceY`, same `block` or `block:height` shorthand as classic flat's `layers`. |
| `riversEnabled` | `true` | Whether a river/ocean biome column gets a water surface instead of the land cap. |
| `riverExclusionRadiusBlocks` | `512` | Radius around spawn within which river/ocean columns always get the land cap regardless of `riversEnabled` — GOAL 16's "far away from spawn" option. |

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
`config/jlt_worldz.yaml`:

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
  reliefBlocks: 4
  forceTopVillage: false
```

| Setting | Default | Description |
|---|---|---|
| `layers` | 8 bands, 324 blocks total | Ordered bottom-to-top layer list. Each entry is either the full `"<biome>;<blocks>;<air gap>"` shorthand (`<blocks>` reuses `flat.layers`' own comma-separated `block`/`block:height` shorthand for that layer's own material stack; `<biome>` is the layer's reported biome, drives decoration/ore feature selection, not block choice; `<air gap>` is the open headroom above the block stack) — or just a bare biome id (e.g. `"minecraft:jungle"`), which expands to that biome's own standard block composition and a 30-block air gap. A biome not specifically tuned falls back to a generic stone/dirt/grass composition rather than failing — any biome id works. The simplified form never bakes in stack-position choices like bedrock or extra depth; the shipped default's bottom/top layers use the full form for exactly that reason. |
| `seedRandomizedOrder` | `false` | Shuffles the configured layer order, deterministically from the real world seed, instead of using it as written. |
| `worldSizeChunks` | `4` | Overworld border half-width in chunks (a 64-block radius by default) plus a matching `VOID` exterior beyond it, applied automatically — independent of the shared `overworldBorder`/`overworldExterior` sections. `0` restores an unlimited world (the original behavior). |
| `reliefBlocks` | `4` | Maximum per-column height bump on each layer's own surface, traded out of that layer's own air gap so biome-band boundaries never move. `0` restores perfectly flat layers. |
| `forceTopVillage` | `false` | Always force-generates a real vanilla village near spawn on the top layer's own surface, provided that layer's biome is village-compatible (silently skipped, logged at `INFO`, if not) — never a natural-search-first attempt, since natural placement isn't reliable to land anywhere reachable in a small, bounded stacked world. **Known risk with the default border:** the default 64-block-radius border is smaller than a real village's typical footprint, so the forced village may visibly clip the border/void wall — not yet resolved, report back what you actually see. |

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
generation — they compose with **any** World Type, configured via their
own top-level `config/jlt_worldz.yaml` sections (no dedicated Customize
screen yet; edit the config file, same as border/exterior settings before
Phase 5.3 exposed those in-screen).

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

| Setting | Default | Description |
|---|---|---|
| `enabled` | `false` | Whether night eventually locks permanently. |
| `lockAfterDays` | `0` | In-game days before locking; `0` locks immediately at world creation. |
| `relaxInsomnia` | `false` | `false` keeps vanilla phantom rules (sleeping still resets a player's own insomnia timer even though it won't skip time). `true` actively suppresses phantoms for players who don't want that pressure in a night that never ends. |

**Known limitation:** locking night can freeze part of an `overworldBorder`
resize schedule in the Overworld — Minecraft 26.2 ties this mod's own
border-scheduling math to the same per-dimension clock day/night uses. A
resize that's already smoothly animating is **not** affected (vanilla's
own border animation runs independently once started); only a resize
still waiting on its own `resizeDelayDays` countdown, or an active
*stepped*-style resize's own periodic jumps, stalls while night stays
locked. Not engineered around; if you're combining forever night with a
delayed or stepped border schedule, expect it to hold still until night
unlocks.

### Rising lava floor (GOAL 29)

Set `risingLava.enabled: true` for a world-wide lava level that rises over
time in the Overworld. It holds at `startY` for `delayDays`, then rises
`rateBlocks` every `rateDays` until it reaches `maxY`. Every air or water
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
  rateBlocks: 1
  rateDays: 1
```

| Setting | Default | Description |
|---|---|---|
| `enabled` | `false` | Whether the lava level rises over time. |
| `delayDays` | `3` | In-game days before the level starts rising. |
| `startY` | `-64` | Y the lava level starts at (the dimension's own real min Y) — everything at or below this converts as soon as the hazard activates. |
| `maxY` | `64` | Y the lava level stops rising at (sea level by default). |
| `rateBlocks` / `rateDays` | `1` / `1` | The level rises `rateBlocks` every `rateDays` — one block per in-game day by default, a deliberately slow first-pass number. |

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
from spawn until it's at least `minDistanceBlocks` away (Chebyshev — the
same "square" distance border/exterior/exclusion-zone settings use
throughout this mod), turning them into a genuine trip rather than a
next-door neighbor. List any structure set ids that should stay at their
normal, un-restricted vanilla distance in `exemptStructureSets`.

```yaml
structureDistance:
  enabled: false
  minDistanceBlocks: 2000
  exemptStructureSets: []
```

| Setting | Default | Description |
|---|---|---|
| `enabled` | `false` | Whether structures are held back from spawn at all. |
| `minDistanceBlocks` | `2000` | Minimum distance from spawn before a restricted structure set may generate. |
| `exemptStructureSets` | `[]` | Structure set ids (e.g. `minecraft:strongholds`) always allowed at their normal vanilla distance, regardless of `minDistanceBlocks`. |

Vanilla's `/locate structure` predicts a candidate position from the
structure's own placement math and has no idea this mod suppressed
generation there — it can still report a coordinate inside the restricted
radius. That's expected: the actual chunk simply generates without the
structure, exactly like any other suppressed structure set.

## Configuration

The mod reads `config/jlt_worldz.yaml` at startup if present; it is entirely
optional and the mod never creates or requires one. These values are the
defaults for the singleplayer Customize screen and the direct inputs for
dedicated-server world creation. A complete, comment-documented reference —
the way to discover every available setting — lives at
[`config/jlt_worldz.example.yaml`](config/jlt_worldz.example.yaml); copy the
parts you want into your own `config/jlt_worldz.yaml`. The mod never rewrites
`config/jlt_worldz.yaml` itself, so any comments or settings you leave out
stay untouched; it writes `config/jlt_worldz.reference.yaml` alongside it on
every launch instead — generated, never read back, safe to delete, and always
showing every setting at its current built-in default.

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
