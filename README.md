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
| **Worldz** | The original flexible preset: allowed-biome list, coordinated `ocean`/`single_biome`/`void`/`legacy` layouts, borders, exteriors, starter land. See [Using Worldz](#using-worldz) below. | Full screen: biomes, starter zone, borders, exteriors, layout, spawn strategy. |
| **Worldz: Single Biome** | One land biome fills the entire world, everything else (structures, caves, seed-based randomness) generates normally; optional different starter biome; optional seed-chosen starter location. See [Single-biome challenge](#single-biome-challenge) below. | Small screen: land biome, starter biome, starter radius, spawn strategy only. |

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

Delete or rename an existing `level-name` world only when you intentionally want
the server to create a new one. Worldz never converts an existing world.

## Single-biome challenge

Select **Worldz: Single Biome** under **World Type** for a world where one
chosen biome fills the entire generated world — structures, caves, and
vanilla randomness all generate normally and follow the world seed exactly
as they would in a vanilla world of that biome. Select **Customize** for a
small screen with only this type's fields: land biome, an optional different
starter biome around spawn, starter radius, and spawn strategy. This is a
separate World Type from plain **Worldz** above — it does not read
`allowedBiomes`, borders, exteriors, or `layout` at all; those shared
modules stay YAML-only for this type for now (a future phase gives every
type its own Customize section for them).

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
```

| Setting | Default | Description |
|---|---|---|
| `landBiome` | `"minecraft:plains"` | The one biome that fills the entire world. |
| `starterBiome` | `""` | Optional different biome forced in a circular zone around spawn; empty means no forced zone (the whole world is already `landBiome`). |
| `starterRadiusBlocks` | `256` | Starter-zone radius, only meaningful when `starterBiome` is set; clamped to `64..4096`. |
| `spawn.strategy` | `starter_at_origin` | Same three values as the shared [Seed-informed spawn](#seed-informed-spawn) setting. `preferred_natural_biome` searches for a *natural* occurrence of `starterBiome` using the real seed and moves spawn there instead of forcing a zone at `(0, 0)` — the way to get a starter biome whose location (and, incidentally, whatever natural shape it has) comes from the seed rather than being placed arbitrarily. |
| `allowRivers` | `false` | Let vanilla's own river biomes generate wherever vanilla would naturally place one, instead of `landBiome` applying there too. Terrain is untouched — the river channel is exactly vanilla's shape. Never overrides the starter zone, which always stays guaranteed land. |
| `allowOceans` | `false` | Same idea for vanilla's own ocean biomes (every temperature and depth variant) — additive over `allowRivers`, so turning this on keeps rivers passing through too. Coastlines are exactly vanilla's: no straight edges, no height blending. |

`allowedBiomes` (what structures/features see as possible biomes) is derived
automatically from `landBiome` and `starterBiome` — there is nothing to keep
in sync by hand for this type. When `allowRivers`/`allowOceans` are on, the
matching vanilla biome tags (`#minecraft:is_river`, `#minecraft:is_ocean`)
are folded in too, so structure/feature placement knows those biomes can
occur.

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
