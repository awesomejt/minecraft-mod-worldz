# jlt_worldz — Design

World-generation control mod: compose a new world from one or more chosen
biomes, with a terrain layout that keeps land, coast, ocean, and void shapes
consistent with those choices and an optional starter zone around world spawn.
Fabric + NeoForge multiloader, same toolchain as the sibling mods (MC 26.2, loader
0.19.3, fabric-api 0.154.2+26.2, NeoForge 26.2.0.12-beta, Java 25, Gradle
9.5.1, Loom 1.17-SNAPSHOT). **Template repo: `../reseed`** — it is the
smallest working multiloader sibling; mirror its structure and build wiring
exactly. Execution checklist: `TODO.md` in this repo.

## 1. Identity

- Repo: `minecraft-mod-worldz`; `rootProject.name`/`archivesName` `mod-worldz`.
- Mod id **`jlt_worldz`**. Config `config/jlt_worldz.yaml`, lang keys under
  `*.jlt_worldz.*`.
- Package `media.jlt.minecraft.mods.worldz`. Modules `common` / `fabric` /
  `neoforge` + `build-logic`, entrypoints `WorldzCommon`, `WorldzFabric`,
  `WorldzNeoForge` (mirror `ReseedCommon`/`ReseedFabric`/`ReseedNeoForge`).
- `group media.jlt.minecraft.mods`, `version 0.1.7`, license MIT.
- Description: "Limit new worlds to a chosen set of biomes, with an optional
  starter biome around world spawn."

## 2. Player-facing behavior

1. Player edits `config/jlt_worldz.yaml` (or accepts defaults), then creates a
   new world and picks the **"Worldz"** world type in the world-creation
   screen. The preset's **Customize** screen starts from those defaults and can
   override every setting for that new world without rewriting YAML. Dedicated
   servers use `level-type=jlt_worldz:worldz` and the YAML values directly.
2. Legacy worlds generate vanilla terrain and restrict its biome labels to the
   configured set. New coordinated-layout worlds use one persisted layout to
   choose both broad terrain class and biome, preventing land biomes and their
   structures from being placed over ocean-shaped terrain. See §17. A
   single-entry list remains a supported whole-world single-biome case.
3. If a starter biome is configured, everything within `starterRadiusBlocks`
   of the origin (where world spawn lands) is that biome, regardless of the
   allowed list.
4. Config values are **baked into the world at creation** (persisted in
   the saved world-generation settings via generator/biome-source codecs).
   Editing the config later only
   affects newly created worlds. Worlds not using the Worldz preset are
   untouched. Nether terrain is vanilla unless its exterior is void; End
   generation remains vanilla.

## 3. Architecture — preset, biome source, and generator wrapper

Three pieces, loader-neutral apart from registry calls:

1. A custom `BiomeSource` type **`jlt_worldz:limited`** registered in the
   vanilla `Registries.BIOME_SOURCE` codec registry (stable API; this is how
   all worldgen mods do it).
2. A **world preset datapack** shipped in the mod jar
   (`data/jlt_worldz/worldgen/world_preset/worldz.json`) whose overworld stem
   uses that biome source with *no explicit fields*, plus a
   `data/minecraft/tags/worldgen/world_preset/normal.json` tag entry so it
   appears in the world-type dropdown.
3. A delegating `ChunkGenerator` type **`jlt_worldz:enveloped`**, registered in
   `Registries.CHUNK_GENERATOR`, which wraps the preset's Overworld and Nether
   generators and applies the resolved terrain envelope (§14).

The biome-source codec's fields are all optional. Absent fields (the shipped
preset's case) are resolved from the mod config **at decode time** — i.e. at
world creation.
The encode path always writes the *resolved* values, so once the world saves
its `level.dat`, the settings are self-contained and config-independent.

## 4. `worldgen/LimitedBiomeSource`

Extends `BiomeSource`. State after resolution:

- `HolderSet<Biome> biomes` — the allowed set.
- `Optional<Holder<Biome>> starterBiome`, `int starterRadiusBlocks`.
- `MultiNoiseBiomeSource delegate` — built (not serialized) by filtering the
  vanilla overworld climate-parameter list down to entries whose biome is in
  the allowed set.

Codec sketch (Mojmap; **verify every name against the 26.2 sources** — minor
renames are likely, log deviations):

```java
public static final MapCodec<LimitedBiomeSource> CODEC =
    RecordCodecBuilder.mapCodec(instance -> instance.group(
        Biome.LIST_CODEC.optionalFieldOf("biomes").forGetter(...),
        Biome.CODEC.optionalFieldOf("starter_biome").forGetter(...),
        Codec.INT.optionalFieldOf("starter_radius").forGetter(...),
        RegistryOps.retrieveGetter(Registries.BIOME),
        RegistryOps.retrieveGetter(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
    ).apply(instance, LimitedBiomeSource::resolve));
```

`resolve(...)`:

1. `biomes` absent → parse `config.allowedBiomes` strings: `id` → direct
   holder lookup, `#id` → tag lookup, via the `HolderGetter<Biome>`. Unknown
   ids: WARN and skip. All getters/encoders expose the resolved values, never
   the config.
2. `starter_biome` absent → from `config.starterBiome` (empty string → none).
   `starter_radius` absent → from `config.starterRadiusBlocks`.
3. Fetch the `minecraft:overworld` entry of the
   `multi_noise_biome_source_parameter_list` registry; filter its
   `Climate.ParameterList` pairs to allowed biomes; build the delegate with
   `MultiNoiseBiomeSource.createFromList`.
4. **Fail-safe:** if the filtered list is empty (all ids bogus, or only
   biomes with no overworld climate entry, e.g. nether biomes), WARN loudly
   and use the unfiltered overworld list — the world must still be creatable.
   Also WARN per allowed biome that matched no climate entry.

Runtime overrides:

- `getNoiseBiome(x, y, z, sampler)` — quart coordinates. If starter biome
  present and `x*x + z*z <= r*r` where `r = QuartPos.fromBlock(starterRadiusBlocks)`,
  return the starter holder (whole column, circular zone centered on the
  origin); else delegate. World spawn is always searched near the origin, so
  centering on (0,0) covers spawn (see §7 guard).
- `collectPossibleBiomes()` (or the constructor's biome stream, per 26.2
  shape) — union of allowed set + starter biome, so features/spawning/structure
  logic see the starter biome as possible.

## 5. `logic/BiomeListSpec` (pure, testable without MC)

Parses config string lists: distinguishes `id` vs `#tag`, validates
resource-location syntax (`[a-z0-9_.-]+(:[a-z0-9_./-]+)?`, default namespace
`minecraft`), reports invalid entries. Returns a spec object the MC-side code
maps to holders. All syntax validation and the quart-distance/radius math
(block→quart conversion, boundary inclusive) live here or in a small
`StarterZone` helper — pure JUnit targets.

## 6. Config (`config/jlt_worldz.yaml`, flat + `_docs`, mirror trees' `ModConfig` pattern)

Loaded at startup; each loader entrypoint passes its config-dir `Path` into
`WorldzCommon.init(...)` (check how trees wires this and mirror it).

| Field | Default | Notes |
|---|---|---|
| `allowedBiomes` | desert/badlands/cave mix | ids and/or `#tags`; order irrelevant; see `config/jlt_worldz.example.yaml` for the exact default list |
| `starterBiome` | `"minecraft:plains"` | empty = feature off; need not be in `allowedBiomes` |
| `starterRadiusBlocks` | `256` | clamp `64..4096` |

Sanitization mirrors trees: malformed file → defaults + WARN (do not
overwrite the user's broken file), unknown keys tolerated, non-string list
entries dropped with WARN, radius clamped. Registry-existence of ids can only
be checked at world creation (§4.1); config load checks syntax only. Ship
`config/jlt_worldz.example.yaml` in the repo. A legacy `jlt_worldz.json` is
migrated automatically and retained as `jlt_worldz.json.bak`.

## 7. Spawn guard (conditional — build only if smoke test shows it's needed)

Vanilla picks spawn by climate fitness near the origin and may wander a few
hundred blocks. With the default 512 radius spawn should land inside the
starter zone; the smoke test verifies this. If it can land outside:
server-started hook (Fabric `ServerLifecycleEvents.SERVER_STARTED`, NeoForge
`ServerStartedEvent`), and when (a) the overworld's biome source is a
`LimitedBiomeSource` with a starter biome, (b) the world is fresh
(`overworld.getGameTime() == 0`), and (c) the current spawn is outside the
zone → move spawn to the origin column at the
`MOTION_BLOCKING_NO_LEAVES` heightmap Y.

## 8. Data + lang files

- `data/jlt_worldz/worldgen/world_preset/worldz.json` — overworld stem:
  `type minecraft:overworld`, generator `minecraft:noise` with
  `settings minecraft:overworld` and `biome_source {"type": "jlt_worldz:limited"}`
  (no other fields). Nether + End stems: copy vanilla's `normal.json` preset
  content verbatim (misode.github.io/worldgen has the 26.2 format).
- `data/minecraft/tags/worldgen/world_preset/normal.json` —
  `{"replace": false, "values": ["jlt_worldz:worldz"]}`.
- `assets/jlt_worldz/lang/en_us.json` — preset display name, key
  `generator.jlt_worldz.worldz` → `"Worldz"` (**verify the world-preset lang
  key format in 26.2**; check how vanilla names e.g. Amplified).

## 9. Tests

- Pure JUnit (no MC): `BiomeListSpec` parsing/validation, starter-zone radius
  math, `WorldzConfig` defaults/sanitization/clamping (mirror trees'
  `ModConfig*Test` granularity).
- The biome-source codec and preset JSON are exercised by the smoke test
  (world creation fails fast and loudly if the JSON or codec is wrong).
- Stretch (only if asked): Fabric gametest wiring copied from `../reseed`.

## 10. Known caveats (document in README)

- Structures follow biomes: a plains-only world has no ocean monuments,
  jungle temples, etc. Villages/strongholds appear only if their biomes are
  allowed. Inherent to the feature.
- Allowed biomes must have overworld climate entries; nether/end/special
  biomes in the list are ignored with a warning (fixed-source mode is
  deferred).
- Existing worlds are never modified; config changes don't affect
  already-created worlds.
- Nether biomes remain vanilla; its terrain may optionally become void outside
  the envelope. End generation remains vanilla.

## 11. Deferred

- Nether/End biome limiting.
- `FixedBiomeSource` mode to allow non-overworld biomes (single-biome mushroom
  island world, crimson-forest overworld, …).
- Per-dimension biome limiting; multiple starter zones; square starter-zone
  shape option.
- `/jlt_worldz reload` command (config only matters at world creation, so low
  value).
- CurseForge/Modrinth publishing (same open flag as the other four mods).

## 12. Optional limited-world borders

Worldz can apply independent, origin-centered vanilla square borders to the
overworld and Nether. Configuration expresses each border as its half-width
(`radiusBlocks`) for player readability; the vanilla API receives twice that
value as its diameter. Borders are disabled by default.

Each dimension has an initial radius, final radius, and resize duration in
Minecraft days. Equal radii make a static border. A larger final radius grows
linearly; a smaller final radius shrinks linearly. A zero-day transition applies
the final size immediately. As an alternative to a total `resizeDays`, players
may set `resizeRateBlocks` and `resizeRateDays` to express a continuous rate of
X radius blocks per Y Minecraft days. When both rate values are positive they
take precedence over `resizeDays`; the exact duration is derived from the total
distance, including a proportional final partial interval. The vanilla border
state persists the resulting transition so restarts do not reset its progress.
Existing worlds are not retroactively limited, and encoded schedules without
rate fields retain the original total-duration behavior.

Optional progression guarantees keep an End portal and blaze access within the
final overworld and Nether borders respectively. They need not be reachable on
day zero, but must be reachable when the configured resize period ends (100
days is the reference gameplay target). For strict small borders, guaranteeing
the functional portal or blaze-spawning area takes priority over fitting an
entire stronghold or Nether fortress inside the border.

At first server start, Worldz locates the nearest natural objective. A natural
structure is accepted only when its reference point plus a 128-block safety
margin fits inside the tightest final-border and solid-terrain radius.
Otherwise Worldz creates a deterministic
compact fallback near positive X: a surface End-portal frame with no eyes, or
an enclosed nether-brick room containing a real blaze spawner. Exact fallback
coordinates are logged. The compact sites remain inside the smallest supported
border and are safe to place repeatedly, though a saved-data marker normally
ensures placement occurs only once.

## 13. World-creation customization

When Worldz is selected on the singleplayer world-creation screen, vanilla's
**Customize** button opens a Worldz editor. It exposes the allowed biome/tag
list, optional starter biome and radius, and both dimensions' enabled state,
initial/final border radii, total/rate timing, progression guarantee, exterior
mode, boundary, and ocean transition where supported. The YAML
configuration seeds the first editor state but does not restrict the registered
biome IDs or tags a player may enter.

Selecting **Done** resolves IDs and tags against the active world-generation
registries, creates an explicit `LimitedBiomeSource`, and replaces the
Overworld and Nether wrappers. The End generator remains that of the Worldz
preset. The explicit source and wrappers serialize all selections into the new
world, so later config edits and later worlds are independent. Reopening the
editor before creation shows the already applied per-world selections.

NeoForge registers the editor through `RegisterPresetEditorsEvent`. Fabric 26.2
has no corresponding preset-editor event, so a client-only mixin supplies the
editor from `WorldCreationUiState.getPresetEditor()`.

## 14. Exterior terrain envelopes

Worldz can replace terrain outside a square, origin-centered envelope without
requiring a world border. The top-level `overworldExterior` accepts `normal`,
`ocean`, or `void`; `netherExterior` accepts `normal` or `void`. Both default to
`normal` for backward compatibility. Each has `boundaryRadiusBlocks`, where `0`
means auto-derive the boundary from the largest radius that dimension's border
will ever expose: `max(initialRadiusBlocks, finalRadiusBlocks)`. Auto with no
enabled border is invalid and safely falls back to `normal`; an explicit
positive boundary works with or without a border.

`void` removes all terrain beyond the boundary and continues infinitely. In
the Overworld, `ocean` creates a stable deep-ocean exterior with water through
sea level and a solid seabed. `oceanTransitionWidthBlocks` moves the beginning
of that ocean inward from the outer boundary, leaving that many blocks of ocean
accessible inside a matching border; the ocean continues infinitely beyond the
border. For example, an auto boundary of 512 and transition width 128 begins
ocean at radius 384. The envelope uses block-level square distance
`max(abs(x), abs(z))` so it aligns exactly with vanilla's square border.

The envelope is baked into each new world's encoded generators. A wrapper
`ChunkGenerator` delegates vanilla generation inside the solid region, masks
base terrain/surfaces/carvers/decorations outside it, reports matching base
heights and columns, and supplies the deep-ocean biome in the ocean exterior.
The Worldz preset wraps both Overworld and Nether generators; the Customize
screen replaces both explicit wrappers while leaving the End vanilla. Existing
unwrapped Worldz saves remain unchanged.

Progression guarantees must fit within terrain that can support them, not only
inside the final border. Natural strongholds/fortresses outside the solid region
are rejected, and compact fallback objectives are clamped inside both the
eventual accessible border and the terrain boundary. Ocean is traversable, but
normal structures and decoration are suppressed in wholly exterior chunks;
fallback End portals remain in the solid starter region rather than on the
seabed.

## 15. Delayed border resizing

Each dimension's border schedule may set `resizeDelayDays`, default `0`. The
border is applied at its initial radius when the world is first initialized and
remains static there for that many Minecraft days before growth or collapse
begins. Delay time uses the same 24,000-game-tick day as resize timing, advances
only while the server is ticking, and therefore does not elapse while a world is
closed. A delayed zero-duration resize jumps to the final radius when the delay
expires.

Worldz persists a pending start game tick for each dimension in its existing
overworld saved-data record. Loader-specific end-server-tick events ask the
shared manager to start due transitions. Once started, vanilla's border state
persists and advances the interpolation as before. Missing delay fields decode
to zero, and older saved-data records without pending ticks remain completed;
neither older configs nor existing Worldz schedules are restarted.

Progression guarantees are created during initial world setup, not after the
delay. Their locations still use the final reachable/supportive radius, while
the changing border determines when players can reach them.

## 16. Guaranteed starter land

Worldz may guarantee usable land beneath an enabled Overworld starter-biome
zone. This is independent of the exterior and border systems: it works in an
otherwise normal infinite world, and it may also form the central land mass of
an ocean or void envelope. The YAML defaults and Customize screen expose
`ensureStarterLand`, a transition width, and a foundation depth. The guarantee
has no effect when no starter biome is selected.

The starter-land profile is circular, matching the starter-biome radius. In
the core it requires the first air block to be at least two blocks above sea
level, but that value is a baseline rather than one absolute target. Compressed
natural ocean-floor relief and broad seed-dependent vanilla surface noise add
rolling elevation above the baseline. Natural columns already above the shaped
target remain unchanged. For lower columns, a smoothstep curve across the
configured transition ring blends only the required vertical lift back toward
the delegate generator's original height. Rounded sub-block lifts disappear at
the outer edge instead of leaving a one-block fringe.

During the noise stage, insufficient columns are raised with ordinary stone
from their original ocean-floor height to the profile height. The configurable
foundation depth also repairs gaps immediately below that original floor. The
delegate's biome-aware surface pass then supplies normal dirt, grass, sand, or
other surface material. After carvers, Worldz repairs only the deep foundation
below the surface shell so caves may still reach the surface naturally without
leaving the guaranteed land as a thin floating island above an aquifer.

The wrapped generator applies the same pure profile to `getBaseHeight` and
`getBaseColumn`; spawn selection, structure placement, and generation therefore
observe the terrain that chunks receive. Exterior replacement still has final
authority outside its envelope. Starter-land work is limited to the Overworld
and never modifies the Nether or End.

The resolved starter-land plan and terrain-profile revision are encoded with
`LimitedBiomeSource` alongside the starter biome and radius. A fieldless preset
snapshots the current YAML default, and Customize persists its explicit
selection. For backward compatibility, an already encoded source lacking the
starter-land field decodes with the guarantee disabled. A plan lacking only the
profile revision retains the original flat-floor profile; newly created worlds
use the current relief profile. Installing a newer version therefore cannot
change the algorithm in unexplored chunks of an established save.

## 17. Coordinated world layouts

The current climate-filter implementation limits biome identity but delegates
continentalness and density entirely to vanilla. Testing Worldz5 and Worldz6
showed why that is insufficient: a seed with a large vanilla ocean can report
desert, badlands, or beach over submerged terrain, allow biome-compatible
structures to construct isolated platforms, or let one selected ocean biome
dominate a mixed list. A starter-land overlay improves only its finite center;
reordering biome and starter generation does not make the unrestricted terrain
and biome decisions agree.

New worlds shall offer a persisted `layoutMode` with these player-facing
semantics:

1. **Land only** — selected land biomes form coherent regions. Rivers and small
   inland water remain permitted, but the layout creates no large ocean basins.
2. **Mixed** — coherent land and ocean regions use recommended default coverage
   and equal within-role biome weights. The player may exclude biomes, prefer
   individual biomes with weights, and change ocean coverage and region scale.
3. **Ocean world** — a starter island blends through a beach/coast transition
   into infinite ocean. One or more selected ocean biomes form coherent ocean
   regions outside the island.
4. **Single biome** — one selected biome fills the generated world, with a
   terrain class appropriate to that biome unless explicitly overridden.
5. **Void** — a supported starter island floats in an otherwise infinite sky
   void. This remains distinct from a finite square void exterior so players
   can choose an island world without enabling a border.
6. **Vanilla terrain (legacy)** — retain the existing climate-filter behavior
   for old saves and as an explicit compatibility choice.

A deterministic, seed-aware `WorldLayoutPlan` is the shared source of truth at
every horizontal coordinate. It classifies land, coast, ocean, or void; chooses
an allowed biome within that role; and supplies a smooth terrain adjustment.
The biome source and generator wrapper must sample the same plan. The wrapper
may retain vanilla relief, caves, aquifers, surfaces, and decorations, but it
raises planned land above sea level, lowers planned ocean where necessary, and
blends the change across coasts. `getBaseHeight` and `getBaseColumn` must report
the identical planned terrain so spawn and structures cannot observe a
different surface from generated chunks.

Biome roles are meaningful. Ocean biomes participate in ocean regions; beach
biomes primarily occupy coast transitions rather than arbitrary submerged
areas; ordinary biomes occupy land; rivers may cross land without becoming
large oceans. Vanilla biome roles should be supplied by maintained mappings or
tags. Unknown modded biomes default to land with a warning and allow an
advanced role override. Within a role, balanced selection gives every
positive-weight biome coherent representation; a climate-affinity option may
prefer vanilla-like temperature and humidity without silently excluding a
positive-weight selection.

The starter plan is an overlay on the base layout, not a competing generator.
It forces the configured starter biome and appropriate supporting terrain in
its core, then blends into the selected base layout. Ocean mode uses the same
mechanism for its guaranteed starter island and beach transition; void mode
uses it for the sky island. Border schedules, exterior terrain, and progression
guarantees remain later constraints and must use the layout's supportive
terrain bounds.

The plan, weights, coverage, region scale, role overrides, layout origin, and
algorithm revision are encoded into each new world's generator settings.
Existing encoded worlds default to the legacy mode, and an algorithm revision
must never change unexplored chunks in an established save. Pure JUnit coverage
shall verify seed determinism, allowed-biome exclusivity, positive-weight
representation, coverage tolerances, coast continuity, starter blending,
terrain/height-query agreement, and codec compatibility.

### `WorldLayoutPlan` — pure model

A new `logic.WorldLayoutPlan` record is the persisted, versioned source of
truth, built and queried without any Minecraft class (mirrors `ExteriorPlan`
and `StarterLandPlan`). Its resolved fields:

| Field | Type | Notes |
|---|---|---|
| `mode` | `LayoutMode` enum | `LAND_ONLY`, `MIXED`, `OCEAN`, `SINGLE_BIOME`, `VOID`, `LEGACY` |
| `seed` | `long` | the world seed; the sole source of randomness (§ sampling below) |
| `regionScaleBlocks` | `int` | grid-cell edge length in blocks; clamped, see defaults table |
| `oceanCoverageFraction` | `double` | `MIXED` only; target fraction of grid cells classified ocean, `0.0..1.0` |
| `coastBlendWidthBlocks` | `int` | smoothing distance either side of a role boundary |
| `landBiomes`, `oceanBiomes`, `beachBiomes` | `List<BiomeWeight>` | `BiomeWeight(String biomeId, double weight)`; empty lists fall back per mode (e.g. `MIXED` with no ocean biomes behaves like `LAND_ONLY`) |
| `singleBiome` | `Optional<String>` | `SINGLE_BIOME` only |
| `roleOverrides` | `Map<String, BiomeRole>` | forces a biome's role (`LAND`/`OCEAN`/`BEACH`), overriding the maintained vanilla mapping |
| `layoutOriginBlockX`, `layoutOriginBlockZ` | `int` | grid and starter-overlay center; `0,0` until Phase 16 (§18) lands a mover |
| `algorithmRevision` | `int` | see versioning below |

The compact constructor validates ranges (mirroring `ExteriorPlan.DimensionEnvelope`
and `StarterLandPlan`): `regionScaleBlocks` and `coastBlendWidthBlocks` must be
positive, `oceanCoverageFraction` must fall in `[0,1]`, `SINGLE_BIOME` requires
`singleBiome` present and land/ocean/beach lists empty, and every mode other than
`LEGACY`/`SINGLE_BIOME` requires at least one positive-weight biome for every
role it actually uses. `BiomeListSpec` still validates individual ids/tags
before they reach this record, exactly as it does for `allowedBiomes` today.

### Deterministic region sampling

The world is partitioned into a square grid of `regionScaleBlocks`-wide cells,
indexed by `(cellX, cellZ) = floorDiv(blockX, regionScaleBlocks)`. Every value
the sampler needs is a pure function of `(seed, salt, cellX, cellZ, ...)` — a
64-bit hash (SplitMix64-style avalanche over the concatenated longs) mapped to
`[0,1)`. No gradient/Perlin noise and no Minecraft `DensityFunction` are
involved, so the sampler is exercisable from plain JUnit exactly like
`BiomeListSpec`.

1. **Role classification** (`MIXED` only — `LAND_ONLY`/`OCEAN`/`SINGLE_BIOME`
   fix every cell's role outright, `VOID` has no base role). Each cell draws
   one hash with salt `"role"`; the cell is `OCEAN` when the hash is below
   `oceanCoverageFraction`, else `LAND`. Because each cell's draw is
   independent and uniform, the *measured* ocean fraction over a sample of `N`
   cells per axis converges to the target with no calibration curve needed.
2. **Biome selection within a role**: naively scoring each candidate biome as
   `hash * weight` and taking the max was tried first and rejected — it
   systematically starves low-weight biomes (a 3:2:1 weight split measured as
   roughly 64:31:5 over many trials, not 50:33:17). The corrected, exact
   method is the standard weighted-argmax transform: score biome `i` as
   `hash_i ** (1 / weight_i)` (equivalently, an exponential-race / Efraimidis–
   Spirakis draw) and take the argmax. This reproduced target ratios to within
   ~1% over 30-seed trials at a 64×64 cell sample, always excludes zero-weight
   biomes, and splits equal weights evenly. `roleOverrides` simply move a
   biome into a different role's candidate list (or forces it exclusive via
   `SINGLE_BIOME`-style single-candidate scoring) before this step runs.
3. Both draws are re-derived per query, not cached in the plan, so the plan
   itself stays a small immutable record; a thin per-world lookup cache is an
   implementation detail for 15.3/15.4, not part of the persisted model.

### Coast blending

Within `coastBlendWidthBlocks` of a cell boundary whose neighbor resolved to a
different role, the sampler reports a continuous `landFactor` (`0` fully
ocean-shaped, `1` fully land-shaped) via a smoothstep across that distance,
the same shape `StarterLandPlan`'s transition ring already uses. The chunk
generator wrapper (15.4) raises/lowers terrain by this blended factor rather
than switching abruptly at the cell edge; biome selection at those columns
prefers a `BEACH`-role biome when one is configured for that boundary, else
falls back to the bordering land role's normal selection. Cells with no
opposite-role neighbor within the blend width behave as pure land or ocean.

### Recommended defaults (fixture-verified)

Defaults below came from a throwaway fixture harness (hash-based grid,
64×64–128×128 cell samples, 8–30 seeds per case; not part of the shipped
code) exercising the two algorithms above before committing to them:

| Field | Default | Fixture finding |
|---|---|---|
| `regionScaleBlocks` | `512` | matches the existing `starterRadiusBlocks` default, so one grid cell reads as "about one starter zone wide"; mean same-role run length measured ~2.2–2.7 cells (~1100–1400 blocks), occasional runs past 19 cells, giving continent/ocean-scale coherence without a second scale concept to explain |
| `oceanCoverageFraction` | `0.35` | measured within ±1.5 percentage points of target at a 64×64 sample and ±0.3 points at 128×128; recommend a JUnit tolerance of ±5 percentage points at ≥64×64 cells to stay comfortably outside sampling noise |
| `coastBlendWidthBlocks` | `128` | a 512:128 (25%) ratio to `regionScaleBlocks`, echoing `starterLandTransitionBlocks`'s original 128-block blend width for one consistent "quarter-scale blend" rule of thumb across the mod |

`MIXED`'s recommended starting biome lists mirror the existing default allowed
list philosophy: unweighted (implicit weight `1`) unless the player raises one,
so a fresh `MIXED` world with no edits behaves like an even split of whatever
biomes are allowed, plus one ocean biome once the player adds it.

### Compatibility and versioning

`algorithmRevision` follows the `StarterLandPlan.*_PROFILE_VERSION` pattern:
`LEGACY_MODE_REVISION = 0` (no layout sampling — today's climate-filter-only
behavior; the only value an already-encoded pre-Phase-15 world can decode to)
and `CURRENT_REVISION = 1` for newly created layout-mode worlds. A future
sampling change ships as `REVISION = 2`, applied only to worlds created after
the update; existing saves keep decoding at whatever revision they were baked
with. `mode = LEGACY` is the codec default for any plan missing the field
entirely, matching how `StarterLandPlan`'s absence decodes to `disabled()`.

### Implementation (Phase 15.4)

`LimitedBiomeSource.getNoiseBiome` samples the plan directly for every mode
except `LEGACY`: the exterior ocean override and the existing starter-zone
override still take precedence (in that order), then a layout sample resolves
to one of the plan's own biomes, falling back to the climate-filter delegate
only if that biome id fails to resolve in the active registry.
`collectPossibleBiomes()` adds every resolved layout biome for non-legacy
modes so structure and feature logic can see them.

Terrain height coordination does not replace generated terrain outright. A new
pure `logic.LayoutTerrainProfile` blends a raised land floor
(`seaLevel + 2`) and a capped ocean ceiling (`seaLevel - 3`) by the sampled
`landFactor`, producing one target floor height per column. `EnvelopedChunkGenerator`
applies the difference between that target and the delegate's natural
ocean-floor height as a uniform vertical adjustment: raised columns get stone
filled up from a foundation depth exactly like starter land (using a smaller
default foundation depth when no starter guarantee is active in that column),
lowered columns get solid ground cleared down to water (below sea level) or
air (at/above it). Every `Heightmap.Types` query in `getBaseHeight` shifts by
this same delta so all heightmap types keep agreeing with each other and with
`getBaseColumn`, and structures — which read these methods for placement —
observe the coordinated (already submerged, where applicable) terrain rather
than the pre-adjustment natural shape. The starter-land guarantee is applied
as a second, independent pass on top and always wins in its own zone, since it
only ever raises and treats existing water/air (including newly lowered
layout columns) as replaceable foundation. `VOID` mode is excluded from this
adjustment entirely until its sky-island overlay lands in Phase 15.5 — its
placeholder sample would otherwise raise the whole world instead of leaving it
void. The sampling seed for a fieldless-preset world is chosen at random when
the world is first created (not yet tied to the player's entered Minecraft
seed string, since no decode-time hook available to a `BiomeSource` codec
currently exposes it); once resolved, the codec persists that value so reloads
stay stable. Tying it to the real world seed is deferred alongside Phase 16's
related finalized-seed-timing investigation.

## 18. Seed-informed spawn and layout origin

World creation should separate three concepts currently conflated at `(0,0)`:
the preferred spawn biome, the optional forced starter biome, and the coordinate
used as the center of layout/border/exterior systems. Planned spawn strategies
are:

- **Starter at origin** — current behavior: force the starter biome and land
  around the layout origin, then choose a safe surface spawn there.
- **Preferred natural biome** — use the finalized seed and an unmodified
  vanilla biome/climate view to find a nearby preferred biome, then consider
  that location for the world spawn without relabeling the search area first.
- **Vanilla spawn** — keep Minecraft's ordinary seed-derived spawn selection.

The preferred-natural strategy requires an implementation spike before it is
promised as a creation-screen option. Minecraft finalizes a random seed after
some client customization state exists, and currently begins spawn selection
from the random state's climate sampler. If the located point becomes the
Worldz layout origin, its coordinates must be resolved before affected chunks
generate, persisted, and consistently applied to starter zones, square border
centers, exterior envelopes, progression sites, and distance math. Merely
moving the player spawn after generation would leave those systems centered on
the wrong point and is not acceptable. Search radius, fallback behavior, and
the interaction with a user-entered fixed seed must also be explicit.

## 19. Deferred customizable flat worlds

A future Worldz flat mode should provide a friendly editor over Java's existing
`FlatLevelGeneratorSettings`, inspired by Bedrock's layer customization but
persisted through Java world-generator codecs. It is not part of the first
coordinated-layout implementation.

Requirements to retain for that phase:

- Edit an ordered bottom-to-top list of block layers and thicknesses, validate
  total build height, and offer useful templates plus import/export text.
- Select any registered fixed biome rather than presenting plains as the
  effective default-only experience.
- Independently control biome decorations, lakes, and structure sets.
- Offer a surface-height or padding control whose spawn surface is at Y 40 or
  higher, avoiding the ordinary below-Y-40 slime-chunk rule while retaining an
  explicit classic shallow preset. This does not suppress biome-specific
  surface slime spawning.
- Expose villages, strongholds, mineshafts, pillager outposts, ruined portals,
  and other compatible structure sets individually. Trial chambers are not in
  Minecraft 26.2's Overworld flat preset and require feasibility/placement
  testing before Worldz enables them.
- Integrate Worldz borders, progression guarantees, spawn strategies, and
  optional void/ocean exteriors without routing flat worlds through noise-based
  terrain adjustment.

Verified Minecraft 26.2 behavior: flat generator settings already encode one
fixed biome, arbitrary layers, lake and feature flags, and an optional
structure-set holder list. Vanilla's programmatic default includes only
villages and strongholds. Its `overworld` flat preset additionally lists
mineshafts, pillager outposts, ruined portals, and strongholds, but not trial
chambers. Therefore the structure difference is preset configuration rather
than an inherent limitation of `FlatLevelSource`.
