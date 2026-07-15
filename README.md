# JLT Worldz

Limit newly created Minecraft worlds to one or more chosen biomes while keeping
vanilla terrain shapes, caves, rivers, mountains, Nether, and End. An optional
circular starter biome can be forced around the world origin. Worlds may also
use configurable square borders and replace terrain outside a central square
with ocean or void. Optional starter-land reinforcement prevents a chosen
starter biome from becoming a thin island over deep water. Supports Fabric and
NeoForge for Minecraft 26.2.

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

1. Start Minecraft once so the default config is created.
2. Edit `config/jlt_worldz.yaml` if you want different reusable defaults, then
   restart Minecraft.
3. Create a world and select **Worldz** under **World Type**.
4. Select **Customize** to change the biome list, starter zone and land,
   borders, exterior terrain, or resize rates for this world only.

The Customize screen starts with the YAML values. Selecting **Done** bakes the
screen values into the new world without rewriting the YAML file, so each new
singleplayer world can use completely different settings. Biomes and biome tags
can be entered one per line or separated by commas; the screen validates IDs
against the registries loaded for that world.

For a dedicated server, set these values before creating the world:

```properties
level-type=jlt_worldz:worldz
```

Delete or rename an existing `level-name` world only when you intentionally want
the server to create a new one. Worldz never converts an existing world.

## Configuration

The mod reads `config/jlt_worldz.yaml` at startup. These values are the defaults
for the singleplayer Customize screen and the direct inputs for dedicated-server
world creation. A complete documented example is available at
[`config/jlt_worldz.example.yaml`](config/jlt_worldz.example.yaml).
If an older `jlt_worldz.json` exists and no YAML config exists yet, Worldz
migrates it automatically and retains the original as `jlt_worldz.json.bak`.

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
| `layout` | `legacy` | Coordinated land/ocean/beach terrain layout. Configurable and persisted, but not yet applied to generated terrain — see [Coordinated world layouts](#coordinated-world-layouts-in-progress). |

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
terrain. Otherwise it creates
a compact fallback near `(32, 0)`: a visible surface End-portal frame in the
overworld, or an enclosed nether-brick blaze-spawner room at approximately
`(32, 64, 0)` in the Nether. The fallback portal contains no eyes. Exact
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

## How biome limiting works

Worldz filters vanilla's overworld multi-noise climate map to the allowed
biomes. Minecraft still chooses the closest climate entry at every position,
which attempts to preserve climate-shaped regions when several biomes are
allowed but does not coordinate them with broad terrain shape. The
starter biome overrides the entire vertical column inside its circular zone.
For an ocean exterior, Worldz reports the deep-ocean biome outside the solid
square so spawning and climate behavior match the generated water.

### Current terrain-composition limitation

Biome limiting does not currently change vanilla continentalness or density.
On a seed whose nearby vanilla terrain is a large ocean, an allowed land biome
can therefore be reported over submerged terrain, or a selected ocean biome can
dominate a mixed list. Guaranteed starter land corrects only its configured
central radius and transition. It does not rebalance the infinite world.

### Coordinated world layouts (in progress)

A `layout` section (`mode`, weighted `biomes`, `oceanCoverageFraction`,
`regionScaleBlocks`, `coastBlendWidthBlocks`, `singleBiome`, `roleOverrides`) is
configurable and validated today, and its resolved plan is persisted into new
worlds' generator settings. It does **not** yet change generated terrain or
biome placement — every world still uses the climate-filter behavior described
above regardless of `layout.mode`. Modes other than the `legacy` default
(`land_only`, `mixed`, `ocean`, `single_biome`, `void`) require at least one
usable biome for every role they need; an incomplete configuration logs a
warning and falls back to `legacy`. See DESIGN §17 for the full plan.

A coordinated layout generator is planned to make biome and broad terrain
decisions together. Its specified modes are land-only, mixed with configurable
coverage and biome weights, an ocean world with a starter island and beach
transition, single biome, and a starter island in sky void. Existing saves will
retain the current generator to avoid seams. Seed-informed spawn placement and
a richer Java flat-world editor are tracked separately in [`TODO.md`](TODO.md).

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
