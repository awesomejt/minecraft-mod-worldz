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
- `group media.jlt.minecraft.mods`, `version 0.2.0`, license MIT.
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

> **Superseded (2026-07-16, Phase 1.4):** the `_docs` in-file documentation
> and the legacy `jlt_worldz.json` migration path described here were
> removed. Current behavior is §20.3 (Implementation): the config file is
> fully optional and never auto-created; documentation lives in
> `config/jlt_worldz.example.yaml` as real YAML comments.

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

> **Superseded (2026-07-16):** the 2026-07-16 replan (§20) removes the
> `MIXED`/`LAND_ONLY` grid composition described here, including coast
> blending, role-boundary structure suppression, and the beach ring. This
> section stays as the record of what was built, why, and what its in-game
> defects were. Where §17 and §20 disagree, §20 wins.

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

### Structures near a coast-blend transition (found 2026-07-16, fixed)

Manual Fabric testing (`MANUAL_TESTING.md`, config `09`) found villages
generating stranded — floating in the air or fully buried underwater — at
nearly every distant location visited. Root cause traced into vanilla's own
`JigsawStructure`/`JigsawPlacement`: a multi-piece structure samples a single
anchor height once, via `chunkGenerator.getFirstFreeHeight` (which reaches
our overridden `getBaseHeight`, already layout-aware), then places every
other piece at a fixed offset relative to that one anchor — it never
re-samples terrain per piece. Within a single region this tracks natural
terrain fine, same as vanilla always has. But inside a `MIXED` coast-blend
transition, height can swing from full land to full ocean depth over as
little as `2 * coastBlendWidthBlocks`, far more than vanilla's own terrain
ever varies across one structure's footprint — enough to strand a structure
regardless of which side its anchor landed on.

Fixed by adding `WorldLayoutPlan.isNearRoleBoundary(blockX, blockZ)` (`true`
only for `MIXED` columns where `nearestDifferingBoundary` would apply a
blend — reuses that exact logic, so it can never disagree with the actual
height blend) and checking all four corners of a chunk against it in
`EnvelopedChunkGenerator.createStructures`, alongside the existing
`isEntirelyExterior` check. Coarser than strictly necessary (a structure
anchored safely inside a stable region could still theoretically reach into
a neighboring blend zone at its edge), but keeps every structure grounded in
stable terrain, matching the existing suppression precedent for void/ocean
exterior chunks. Only `MIXED` mode is affected — `LAND_ONLY`'s target height
is a smooth per-column function of natural floor with no cell-boundary
cliff, and `OCEAN`/`SINGLE_BIOME` never classify neighboring cells
differently at all.

**Follow-up (world "Worldz14"): the anchor-chunk-only check wasn't enough.**
Villages and an ocean monument still generated stranded on the very next
test. Root cause: `createStructures` only checked the anchor chunk's own 4
corners, but a structure's actual pieces can land up to vanilla's own
`JigsawStructure.MAX_TOTAL_STRUCTURE_RANGE` (128 blocks) away from that
anchor — well outside a 16-block chunk, and comparable to
`coastBlendWidthBlocks` itself (128 by default), so an anchor comfortably
"safe" by the original check could still have pieces reaching into a blend
zone. Fixed by also checking corners expanded by a
`STRUCTURE_FOOTPRINT_SAFETY_MARGIN_BLOCKS` (128) margin in every direction —
importantly, in *addition to* the original unexpanded corners, not instead
of them: expanding outward moves the checked points further from a boundary
that runs close to (or through) the anchor chunk itself, so dropping the
original corners would have reopened that case. Ocean monuments (a
different, non-jigsaw `Structure` subclass) hit the exact same failure mode
via `Structure.onTopOfChunkCenter`, which uses the identical
single-anchor-height pattern.

### Beach spans the entire coast-blend width, not a narrow shoreline (found 2026-07-16, logged, not fixed)

Also confirmed on "Worldz14": the `BEACH` role in `sampleMixed` applies
across the *entire* `MIXED` coast-blend transition (the full width where a
differing-role neighbor is within `coastBlendWidthBlocks`), not a narrow
strip near the actual shoreline — so beach can extend over 100 blocks into
both the land and the water at the default `coastBlendWidthBlocks = 128`.
`coastBlendWidthBlocks` was designed as a *height/biome smoothing* width,
but is also (accidentally) being used as the beach role's own footprint,
which is a much larger area than a vanilla beach ever is. A real fix needs a
separate, narrower width concept for where `BEACH` applies (distinct from
how far the height blend itself reaches) — a new tunable, not a one-line
patch; logged here and in `MEMORY.md`, not attempted now.

### Straight-line coastlines (found 2026-07-16, logged, not fixed)

The same testing session also confirmed the region grid produces perfectly
straight coastlines, not just an imperfect blend "very close to a grid
corner" as previously scoped above. Cell centers are a plain uniform grid
with no jitter or noise perturbation, so the boundary between any two
differently-classified cells is, by construction, an exactly straight
axis-aligned line — `coastBlendWidthBlocks` only smooths the height/biome
transition across that line, never its shape. Worse, `layoutOriginBlockX/Z`
default to `(0,0)`, exactly where the starter zone is centered, so this
straight-line artifact is essentially guaranteed to appear right at spawn
for the common case (`STARTER_AT_ORIGIN` with a non-legacy layout). Fixing
this needs an actual algorithm change (perturbing the effective cell
boundary with its own noise field, not just blending height across a fixed
line) — a bigger design pass than a mid-testing patch, deliberately not
attempted now; logged here and in `MEMORY.md`.

### `layout.biomes` should not include linear/thin vanilla biomes (found 2026-07-16)

Confirmed in-game (world "Worldz14", `config/tests/09`): an isolated `MIXED`
land cell surrounded by ocean rendered as an ordinary round island — correct
terrain — but with HUD biome "River" over the whole thing, no channel in
sight. Root cause is by design, not a bug: `mixed`/`land_only`/`ocean` assign
**one** weighted-picked biome per whole `regionScaleBlocks` cell (up to
512×512 blocks by default), which works fine for biomes that are naturally
broad (plains, forest, desert, ocean) but produces nonsense for a biome like
`river` that vanilla only ever generates as a narrow, winding, noise-carved
channel — coordinated layouts have no equivalent concept of a linear
feature. Natural-looking rivers and ponds already appear inside land cells
without any help: `LayoutTerrainProfile.targetHeight` only raises a column
that's below the land floor, it never flattens a natural dip, so vanilla's
own terrain noise still shows through. Including `river` (or any other
inherently linear/thin vanilla biome) in `layout.biomes` therefore adds no
benefit and risks exactly this failure mode. Not a code fix — corrected the
test config and documented the caveat; true proportional/linear-feature
support in coordinated layouts would be a real feature addition, not
attempted now.

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

### Implementation (Phase 15.5)

The Customize screen gains a **Layout** button opening `WorldzLayoutScreen`,
mirroring the exterior/border/starter-land sub-screens: a mode-cycle button,
the weighted-biome and role-override lists as multi-line text, and the
remaining numeric fields. `WorldzCustomization.LayoutSettings` is the editable
counterpart to `WorldLayoutPlan` — validated the same strict way as the
outer record's `allowedBiomes`/`starterBiome` (throwing on a malformed entry
rather than silently dropping it, since this is direct user input) — and
`WorldLayoutPlan.resolve(...)` is factored out of `fromConfig` so both YAML
loading and Customize share the exact same role-partitioning logic. Selecting
**Done** generates a fresh random sampling seed for that world via
`WorldzCustomization.worldLayoutPlan(seed)`, exactly like the fieldless
preset's own resolution.

Three of the four remaining starter-overlay requirements from §17 are
implemented as targeted, independent refinements rather than a new per-mode
overlay generator:

- **Land-only rivers**: a new `LayoutTerrainProfile.landOnlyTarget` raises only
  columns whose natural floor is already deep-ocean-shaped (below the ocean
  ceiling), leaving shallower natural depressions — rivers, ponds — untouched.
  `LAND_ONLY` uses this instead of the generic land/ocean blend.
- **Mixed/ocean coasts and beaches**: within the starter zone's existing
  transition ring (`StarterZone.inRingQuart`, reusing the starter-land
  `transitionWidthBlocks`), `LimitedBiomeSource` prefers a beach-role biome
  from the layout (`WorldLayoutPlan.sampleRole`) over the base layout's own
  choice, when one is configured. Independently, the starter-land height
  transition itself (`EnvelopedChunkGenerator.starterLandTargetHeight`) now
  blends back toward the layout-adjusted floor instead of raw vanilla terrain,
  so the island connects smoothly to what generation actually leaves beyond it
  (e.g. an ocean mode's capped depth) rather than jumping to unrelated natural
  shape.
- **Sky-void island**: `VOID` mode forces the Overworld exterior to
  `ExteriorMode.VOID` with its boundary at the starter radius plus transition
  width (or a 256-block fallback with no starter biome configured),
  overriding any explicitly configured exterior. This reuses the existing
  exterior-envelope mechanism entirely rather than a new one; `VOID` still
  skips the land/ocean height adjustment itself (§ above), since the exterior
  override already replaces everything beyond the island.
- **Single-biome terrain** already worked unchanged from Phase 15.4 (its
  landFactor already reflects the one biome's resolved role) and needed no
  further change here.

A fully radial 2D coast blend (the single-nearest-boundary simplification from
§17 can show a minor kink very close to a grid corner) remains a candidate for
later polish once Jason's in-game acceptance testing shows whether it matters.

### Integration audit (Phase 15.6)

Borders, exteriors, spawn, progression objectives, and structure eligibility
were each checked against non-legacy layout terrain; only progression
objectives needed new code.

- **Progression objectives** — `ObjectiveSite.isSupportiveColumn` rejects a
  natural stronghold/fortress reference point sitting on a layout-classified
  ocean column (in addition to the existing border/exterior `fitsInside`
  check), and a new `ObjectiveSite.supportiveFallbackZ` tries a small
  deterministic set of nearby Z offsets for the compact fallback site when the
  default column is not supportive, falling back to the original point
  unchanged if none are. Both are Overworld-only (`VOID` is excluded, same as
  the terrain-adjustment exclusion above, since its exterior boundary already
  bounds the island); the Nether's fortress guarantee is untouched since
  layout never adjusts the Nether.
- **Borders** — already orthogonal: a border is a coordinate limit, not a
  terrain guarantee, and never claimed to guarantee habitable land on its own
  even before layouts existed (that is what starter land and progression
  objectives are for). No change needed.
- **Exteriors** — already composes correctly since Phase 15.4:
  `applyLayoutAdjustment` skips any column where the exterior mode is not
  `NORMAL`, so an explicitly configured exterior always wins over the layout
  in its own boundary, and `VOID` layout mode's forced exterior (§ above) is
  exactly this same mechanism from the other direction.
- **Spawn** — vanilla's Overworld spawn search reads biomes through
  `LimitedBiomeSource.getNoiseBiome` and heights through
  `EnvelopedChunkGenerator.getBaseHeight`/`getBaseColumn`, both already
  layout-aware since Phase 15.4; no dedicated spawn-guard code exists in this
  mod (Phase 5 remained conditional on smoke testing and was never needed), so
  there is nothing separate to integrate.
- **Structure eligibility** — already the core motivation for Phase 15.4: once
  a column's reported biome and generated terrain agree, vanilla's own
  biome-tag-based structure-set eligibility naturally keeps land structures
  off real (lowered) ocean and lets ocean structures appear there instead,
  which is the fix DESIGN §17 originally called for.

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
promised as a creation-screen option. If the located point becomes the Worldz
layout origin, its coordinates must be resolved before affected chunks
generate, persisted, and consistently applied to starter zones, square border
centers, exterior envelopes, progression sites, and distance math. Merely
moving the player spawn after generation would leave those systems centered on
the wrong point and is not acceptable. Search radius, fallback behavior, and
the interaction with a user-entered fixed seed must also be explicit.

### Feasibility spike results (Phase 16.1)

Verified against decompiled Minecraft 26.2 sources (Mojang-mapped, both the
NeoForge-patched merged jar and the NeoForge API jar).

**Seed timing.** The world seed is a concrete `long` well before any level or
server exists — entirely client-side UI state. `WorldOptions.withSeed`
resolves a blank seed field to `randomSeed()` immediately (`WorldOptions.java`),
and `CreateWorldScreen`/`WorldOpenFlows` bake whatever is currently resolved
into `WorldGenSettings` at "Create." A dedicated server resolves
`server.properties`' `level-seed` the same way, at boot. Either way, the real
seed is available long before any hook this mod could register fires.

**Earliest safe hook.** `MinecraftServer.createLevels()` constructs each
`ServerLevel` (with the real seed already bound) and fires NeoForge's
`LevelEvent.Load` immediately afterward — on *every* load, fresh or existing.
Only for a genuinely new world does it then call `setInitialSpawn(...)`, which
fires the cancellable `LevelEvent.CreateSpawnPosition` immediately before
vanilla's own `chunkSource.randomState().sampler().findSpawnPosition()` search
and before any chunk generation. Both events run identically on integrated and
dedicated servers, never on the client. This answers 16.1's ordering question:
yes, a hook exists with the finalized seed available but strictly before
spawn-chunk generation, and no vanilla mixin is required for the timing itself
— `LevelEvent.CreateSpawnPosition` is cancellable and hands back the
`ServerLevelData` to set an explicit spawn.

**What implementation actually requires (not just the hook).** Two real
engineering gaps, not blockers:
1. `WorldLayoutPlan`/`LimitedBiomeSource`'s origin fields are immutable record
   components fixed at codec decode (before any seed-bound object exists).
   Implementation needs a mutable indirection the biome source and generator
   read through at query time, populated from a `DimensionDataStorage`
   (`SavedData`) entry written once (on `LevelEvent.CreateSpawnPosition`, for
   a fresh world) and re-read on every subsequent `LevelEvent.Load` — the
   same `initialized()`-flag idiom `WorldLimitState`/`WorldLimitManager`
   already use for border scheduling.
2. The level's own `chunkSource.randomState().sampler()` cannot be assumed
   usable for a genuine vanilla-climate search from our own code (see the
   flagged risk below) — a "preferred natural biome" search should build its
   own `RandomState`/`Climate.Sampler` from the wrapped delegate's actual
   `NoiseGeneratorSettings`, mirroring the shape of vanilla's own
   `Climate.SpawnFinder` radial search, rather than trusting the level's
   ambient one.

**⚠ Flagged risk found during this spike (unverified in-game, not yet acted
on at Jason's request):** `ChunkMap`'s constructor only builds a real,
delegate-settings-based `RandomState` when the dimension's top-level
`ChunkGenerator` is an actual `NoiseBasedChunkGenerator` instance
(`generator instanceof NoiseBasedChunkGenerator`); otherwise it silently
builds one from `NoiseGeneratorSettings.dummy()` (zero-density router, air
surface rule). Worldz's own `worldz.json` preset wraps *both* the Overworld
and Nether generators in `jlt_worldz:enveloped` (`EnvelopedChunkGenerator`)
unconditionally, and `EnvelopedChunkGenerator extends ChunkGenerator` directly
(composition over a `NoiseBasedChunkGenerator` delegate, not inheritance) — so
that `instanceof` check is false for every Worldz world. Source tracing
(`ChunkStatusTasks` → `level.getChunkSource().randomState()` →
`NoiseBasedChunkGenerator.fillFromNoise` → `NoiseChunk.forChunk`'s
`randomState.router()`) shows this same per-level `RandomState` is what real
chunk generation actually shapes terrain with, not something re-derived from
the delegate's own stored settings. If this holds up in-game, it would mean
generated Worldz terrain has been shaped by a dummy zero-density router this
entire project, independent of anything in Phase 15 — every phase to date has
deferred visual acceptance testing to Jason, so this may never have been
visually confirmed either way. The known fix pattern for this general class of
"wrapper chunk generator" problem is for the wrapper to `extend
NoiseBasedChunkGenerator` rather than delegate to one by composition, so the
`instanceof` check succeeds — but that is a foundational, save-compatibility-
sensitive change to `EnvelopedChunkGenerator` and deserves its own dedicated
investigation/phase, not a fold-in here. At Jason's explicit direction, Phase
16 work continues without acting on this now; it is logged in `MEMORY.md` as
an open, high-priority, unverified risk.

**Resolved 2026-07-16.** Confirmed exactly as flagged: Jason's first extended
in-game session (world "Worldz14") found the lower part of the world almost
entirely lava (vanilla's aquifer fluid picker still applies its normal
below-Y-54 lava rule, fed by a degenerate density field) and caves mostly
absent instead of vanilla's usual winding systems — geodes still generated
normally, since those place via a separate feature/structure system,
independent of the main density router. The "known fix pattern" noted above
turned out to be **wrong for 26.2**: `javap` against the actual compiled
game jar (not the decompiled source used throughout this investigation, which
misleadingly showed a non-final class) confirms `NoiseBasedChunkGenerator` is
declared `final`, so `EnvelopedChunkGenerator` cannot extend it — Java
forbids subclassing a final class outright. An attempted refactor hit this
compiler error immediately and was reverted. The actual fix is a mixin into
`ChunkMap`'s constructor (`ChunkMapMixin`, both loaders — NeoForge's own
mixin support had never been set up in this project before now, added via a
`[[mixins]]` entry in `neoforge.mods.toml`), injected right before the
`generator.createState(...)` call: if the generator is `instanceof
EnvelopedChunkGenerator` wrapping a `NoiseBasedChunkGenerator` delegate, it
overwrites the already-assigned (dummy) `this.randomState` with a real one
built from the delegate's actual settings, using `@Shadow @Mutable` to permit
reassigning an otherwise-`final` vanilla field. Not yet confirmed fixed
in-game — Jason will verify terrain/caves near the bottom of the world on the
next test.

### Strategy specification (Phase 16.2)

A persisted `SpawnStrategy` (`logic.SpawnStrategy`: `STARTER_AT_ORIGIN`,
`PREFERRED_NATURAL_BIOME`, `VANILLA_SPAWN`) selects between the three
behaviors. `STARTER_AT_ORIGIN` is the codec default for any plan missing the
field, matching how every other Phase-15 plan decodes old saves to today's
behavior — existing worlds are unaffected regardless of which strategy a
YAML/Customize default later changes to.

**`STARTER_AT_ORIGIN`** — the layout origin stays `(0, 0)`; the existing
starter-zone/starter-land guarantee applies as before. **Corrected
2026-07-17:** originally documented here as deferring to "vanilla's own
surface-height spawn search," on the assumption that search would naturally
land inside the guaranteed zone. In-game testing found this assumption
false: vanilla's `findSpawnPosition()` searches the *underlying, unmodified*
climate sampler for a "spawn-favorable" signature, entirely independent of
whatever biome `LimitedBiomeSource` actually reports at that position — so
for seeds whose raw climate near `(0, 0)` doesn't match vanilla's criteria,
the search can travel up to vanilla's own ~2048-block radius away, even
though the starter zone itself is guaranteed safe, solid land in the
intended biome. `STARTER_AT_ORIGIN` now explicitly resolves a safe surface
spawn at `(0, 0)` itself (`SpawnOriginManager.safeSpawnNear`, the same
height-lookup pattern `PREFERRED_NATURAL_BIOME`'s found-target case already
used), bypassing vanilla's search entirely rather than hoping it agrees.

**`VANILLA_SPAWN`** — the layout origin stays `(0, 0)` (so border/exterior/
progression/layout math is unaffected), and Worldz does not touch spawn
selection at all: whatever vanilla's own `findSpawnPosition()` chooses is
final. Distinct from `STARTER_AT_ORIGIN` only when no starter biome is
configured, or when the player wants unmodified vanilla spawn behavior even
though a starter biome exists elsewhere in the world.

**`PREFERRED_NATURAL_BIOME`** — searches near `(0, 0)` for a configured
preferred biome using a fresh `RandomState`/`Climate.Sampler` built from the
delegate's own real `NoiseGeneratorSettings` (not the level's ambient one; see
the flagged risk above) and a `logic.SpawnSearchPlan` (pure, already
implemented and tested): ring `0` is the origin itself, then rings every
`stepBlocks` out to `maxRadiusBlocks` (defaults `2048`/`32`, matching vanilla's
own `Climate.SpawnFinder` radius), `pointsPerRing` candidates per ring
(default `8`), searched in that deterministic order, stopping at the first
column whose sampled biome matches. A safe-height check (solid ground, not
lava/void, mirroring vanilla's own `PlayerSpawnFinder` refinement) applies to
whichever candidate is chosen. **Deterministic fallback**: if no ring produces
a match within `maxRadiusBlocks`, the strategy falls back to `(0, 0)` and
proceeds exactly as `STARTER_AT_ORIGIN` — never fails world creation, and
never silently expands the search past its configured bound.

**Fixed vs. random seeds** need no special-casing: by the time
`LevelEvent.CreateSpawnPosition` fires, the seed is already a concrete `long`
regardless of whether the player typed one or left the field blank (§ above)
— the search is identical either way, and is itself deterministic given that
seed, so re-creating a world with the same explicit seed reproduces the same
found origin.

**Recentering.** If the found point becomes the layout origin, every system
currently assuming a fixed `(0, 0)` needs that coordinate threaded through
instead, applied consistently — not just teleporting the player. `WorldLayoutPlan`
already carries `layoutOriginBlockX`/`layoutOriginBlockZ` fields for exactly
this (currently always `0`; its `sampleAt` already computes cell coordinates
relative to them). The Phase 16.3 implementation must additionally recenter:
`StarterZone.containsQuart`/`inRingQuart` (currently always relative to
`(0,0)`), `ExteriorPlan.DimensionEnvelope.modeAt`'s square-distance check,
`WorldLimitManager`'s `border.setCenter(0.0, 0.0)` call, and
`ObjectiveSite.fitsInside`/`fallbackX`/`supportiveFallbackZ`'s
distance-from-origin assumptions. All must move together or a border, an
exterior boundary, and a progression fallback site could each disagree about
where "center" is.

### Implementation (Phase 16.3)

Origin recentering follows a coordinate-shift-at-integration-boundary
pattern: pure logic classes (`StarterZone`, `ExteriorPlan.DimensionEnvelope`,
`WorldLayoutPlan`, `ObjectiveSite`) stay origin-agnostic, always computing
relative to an implicit `(0,0)`; only the MC-integration classes
(`LimitedBiomeSource`, `EnvelopedChunkGenerator`, `WorldLimitManager`,
`ProgressionGuarantees`) subtract a runtime-resolved origin from query
coordinates before calling into that pure logic. `LimitedBiomeSource` carries
the origin as a mutable, non-codec `volatile int originBlockX`/`originBlockZ`
pair (via `setOrigin(int, int)`) since it is resolved after codec decode;
`EnvelopedChunkGenerator` reads the same values through the Overworld's
`LimitedBiomeSource` instance rather than duplicating them. Nether stays
centered at `(0, 0)` — recentering is Overworld-only, matching the strategies'
own scope.

`SpawnOriginManager` resolves and persists the origin, with two entry points
matching the 16.1 spike's two-hook design: `reapplyPersistedOrigin` runs on
every level load (NeoForge's `LevelEvent.Load`, Fabric's
`ServerLevelEvents.LOAD`) and only re-applies an already-resolved origin;
`resolveFreshOrigin` runs only where vanilla is about to pick the initial
spawn for a brand-new world (NeoForge's cancellable
`LevelEvent.CreateSpawnPosition`; a Fabric mixin into
`MinecraftServer.setInitialSpawn`, since Fabric API has no equivalent event)
and performs the one-time search. The origin-resolved flag and coordinates
are persisted via `SpawnOriginState`, a `SavedData` mirroring
`WorldLimitState`'s `initialized()` idiom. The search itself builds an
independent `RandomState`/`Climate.Sampler` from the delegate's real
`NoiseGeneratorSettings` (per the flagged risk above, not the level's ambient
`RandomState`) and a full unfiltered vanilla `MultiNoiseBiomeSource` view, so
it reflects genuine climate regardless of Worldz's own biome restrictions.

The Customize screen exposes a spawn-strategy cycle button alongside the
other Phase 15/16 controls; `spawn.strategy` is a documented top-level YAML
key (`WorldzConfig`/`SpawnConfig`) with the same default-and-sanitize pattern
as every other setting.

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

## 20. Challenge-first restructure (2026-07-16 replan)

`GOALS.md` (Jason, 2026-07-16) is now the requirements source: the mod's
purpose is generating **challenge worlds** (ocean island, sky island, sky
chunk, single biome, flat, limited/expanding/collapsing size, structure
placement options). §§1–19 above remain the technical reference for the
components already built and the verified 26.2 APIs; where §17 conflicts with
this section, this section wins. Execution order lives in `TODO.md`.

### 20.1 Scope decisions (settled with Jason, 2026-07-16)

- **Remove the grid land/ocean composition.** `MIXED` and `LAND_ONLY` layout
  modes, the coast-blend height transition, role-boundary structure
  suppression, and the beach transition ring are removed rather than fixed —
  no GOALS use case needs region-composed worlds, and they caused the open
  straight-coastline, beach-width, and floating-structure defect class.
  Per-cell weighted biome selection survives only where every cell shares one
  role (e.g. ocean-biome variety), which has no cell-to-cell height cliffs.
- **Adjust, don't restart.** The pure-logic core (config, borders, exteriors,
  starter land, spawn search, progression guarantees) and the plumbing
  (codecs, per-loader registration, `ChunkMapMixin` RandomState fix, NeoForge
  mixin support) all map onto GOALS and are kept.
- **New worlds only.** The mod exists to create new worlds. No save-compat
  obligations for worlds created by older mod versions; legacy decode shims
  are no longer added (and may be deleted when touched). A created world must
  still reopen consistently under the version that created it. README states
  the restriction prominently.
- **Client-first.** Singleplayer world creation on Fabric is the acceptance
  path; NeoForge must build and gets brief checks when loader code changes.
  Server research result: world generation always runs on the *logical*
  server, which the client embeds in singleplayer — so a client installation
  covers every GOALS use case, and Open-to-LAN gives multiplayer without a
  dedicated server. The only scenario needing the jar on a dedicated server
  is hosting challenge worlds there; the existing config-driven
  `level-type` path already covers it and is kept as long as it stays free,
  but it is not a per-phase test gate and no server-only features are built.

### 20.2 World types instead of one preset

The single catch-all `jlt_worldz:worldz` preset + giant Customize screen is
replaced by one world type per challenge family (GOALS §World Generation
Screen: mutually exclusive processes get their own types, each with a small
per-type Customize screen and per-type defaults):

- `jlt_worldz:single_biome` — GOALS 10–14.
- `jlt_worldz:ocean_island` — GOALS 01–04, and 28 (lava ocean) either as a
  fluid option here or as its own type (TODO Phase 9.1 decides); the dry
  world (31) generalizes the same fluid parameter to "none".
- `jlt_worldz:sky_island` — GOALS 05–08.
- `jlt_worldz:sky_chunk` — GOALS 09.
- `jlt_worldz:cave` — GOALS 25–26 (added 2026-07-16).
- `jlt_worldz:nether_start` — GOALS 27 (added 2026-07-16).
- `jlt_worldz:end_start` — GOALS 34 (added 2026-07-16).
- `jlt_worldz:chaos` — GOALS 33 (added 2026-07-16; possibly a
  `single_biome` variant — TODO Phase 4.1 decides).
- `jlt_worldz:stacked` — GOALS 35 (added 2026-07-16; name tentative).
- `jlt_worldz:flat` — GOALS 15–16, 22.
- `jlt_worldz:limited` — vanilla generation + size limits only (GOALS
  17–20); the strip/1D world (32) is either an option here or its own type
  (TODO Phase 6.1 decides).

Shared, composable modules available to every type: size limits/borders +
exteriors (§§12/14/15), progression guarantees (§12), spawn strategies +
origin recentering (§18), starter land (§16), plus new ones — the
**exclusion zone** (20.7), the **starter chest** (20.8), and the
**world-hazard rules** runtime module (20.9). The YAML config
gains one section per world type plus shared-module sections; exact preset
IDs, config shape, and lang/tag wiring are Phase 2.1's design task. The old
`LayoutMode` becomes an internal composition detail of each type, not a
user-facing mode switch.

#### Implementation (Phase 2.1 — `single_biome`, first typed preset)

**Decision (confirmed against MEMORY.md's 2026-07-16 entry — not re-litigated,
only made concrete): typed presets replace `jlt_worldz:worldz` one challenge
family at a time.** Phase 2 adds the first one, `jlt_worldz:single_biome`,
alongside the existing generic preset (unchanged, still the only way to reach
`ocean`/`void`/`legacy` layout modes until their own phases land). Later
phases repeat this pattern for `ocean_island`, `sky_island`, etc.; `worldz`
is retired only once every mode it offers has a typed replacement (tracked,
not scheduled, in TODO's carried-over risks).

**No new registry types.** `jlt_worldz:single_biome` reuses the existing
`jlt_worldz:limited` `BiomeSource` codec and `jlt_worldz:enveloped`
`ChunkGenerator` wrapper unchanged — `LayoutMode.SINGLE_BIOME` already
produces a uniform single-biome, land-shaped world (verified: `sampleAt`
never consults the climate-filtered delegate once `mode != LEGACY`, so the
existing per-cell weighted-selection/terrain-profile machinery already
satisfies GOALS 10 wholesale). The only genuinely new codec surface is one
optional decode-time hint field on `jlt_worldz:limited`:
`Codec.STRING.optionalFieldOf("world_type")`. It is never round-tripped
(`forGetter` always returns `Optional.empty()`) — it exists only to tell
`LimitedBiomeSource.resolve()` which config section to default from when
`biomes`/`starter_biome`/etc. are absent (the fieldless-preset path,
distinguished today by `encodedStarterRadius.isEmpty()`, per §3/§4). When
`world_type` is `"single_biome"` on that path, defaults come from a new
`WorldzConfig.singleBiome` section instead of the flat top-level fields;
absent (the `worldz` preset's case), resolution is byte-for-byte what it is
today. Once a player hits Customize → Done, every field is explicit and
`world_type` is irrelevant — matches how `starter_radius`'s presence already
distinguishes "config defaults" from "fully explicit" (MEMORY, 2026-07-14).

**`single_biome.json`** is `worldz.json` with one line added to the
Overworld `biome_source` block (`"world_type": "single_biome"`); dimensions,
the `jlt_worldz:enveloped` wrapper, and the End are identical. A new
`data/minecraft/tags/worldgen/world_preset/normal.json` entry adds it to the
dropdown alongside `jlt_worldz:worldz`.

**Config section** (`singleBiome:`, new top-level `WorldzConfig` field,
parsed/sanitized/dumped the same way every other section is):

```yaml
singleBiome:
  landBiome: 'minecraft:plains'   # GOALS 10 — the one biome that fills the world
  starterBiome: ''                # GOALS 11 — empty = same as landBiome, no forced zone
  starterRadiusBlocks: 256
  spawn:
    strategy: starter_at_origin   # or preferred_natural_biome for GOALS 12
```

Resolution when `world_type=single_biome` and fields are absent:
`worldLayout` = `WorldLayoutPlan.resolve(SINGLE_BIOME, [], {}, DEFAULT_REGION_SCALE_BLOCKS, landBiome, freshSeed)`
(re-seeded to the real world seed at generation time regardless, per §20.4).
`starterBiome`/`starterRadiusBlocks` = **only forced when `starterBiome` is
non-empty** (GOALS 11) — when empty, no starter-zone override is created at
all, since `SINGLE_BIOME` layout mode already makes every column that biome;
forcing a redundant identical circle would be dead weight. `ensureStarterLand`
defaults `true` regardless (still meaningful when the starter biome differs
from the land biome — exactly the blended-seam case §16 was built for).
`allowedBiomes` (the set structures/features see as "possible") is
auto-derived as `{landBiome}` ∪ `{starterBiome}` if set — never user-edited
directly for this type, avoiding the two-lists-that-must-agree trap the
generic preset's `allowedBiomes` field has.

**Small Customize screen** (`SingleBiomeCustomizeScreen` + a new
`SingleBiomePresetEditor implements PresetEditor`, its own
`ResourceKey<WorldPreset>`, registered alongside the existing one in both
loaders' preset-editor wiring): land biome, starter biome, starter radius,
spawn-strategy cycle button (all three strategies — `vanilla_spawn` stays
available for flexibility even though it is not GOALS 10–12's focus). No
border/exterior/starter-land controls — those shared modules stay
YAML-only for this type until Phase 5.3 formally wires "limits compose with
every world type" into each type's screen; the fields still work if set in
`config/jlt_worldz.yaml` (`overworldBorder`, etc. are unconditional
top-level sections, not gated by `world_type`).

**GOALS 12 interpretation (logged here, not a silent guess): "based on
seed — including size and location"** is read as: seed-determined *location*
via the existing `PREFERRED_NATURAL_BIOME` search + recentering (§18,
already built), with *size* satisfied by the still-configurable
`starterRadiusBlocks` rather than by detecting a natural patch's true
boundary (which would need new flood-fill/region-detection work with no
other GOALS use case asking for it). If Jason means literally the latter,
flag it — noted in TODO's Deviation log.

### 20.3 Per-world snapshot file

On world creation, write a commented, human-readable YAML snapshot of the
resolved settings into the world folder (GOALS §Configuration). It is a
*record* for reference/reproducibility, not a control file — authoritative
settings stay baked in the world's generator codec as today.

#### Implementation (Phase 2.4 — per-world snapshot)

Hooks into the exact same "genuinely new world" call site `SpawnOriginManager
.resolveFreshOrigin` already uses (NeoForge's `LevelEvent.CreateSpawnPosition`,
the matching Fabric `MinecraftServer.setInitialSpawn` mixin — both loaders
already guard this to fire once, guarded by vanilla's own
`!levelData.isInitialized()`) — the natural place, since it already has the
resolved `LimitedBiomeSource` and the live `ServerLevel` in hand, and only
runs for brand-new worlds. A new pure `logic.WorldSnapshotWriter` (mirrors
`WorldzConfig`'s existing map-building/YAML-dump shape, but write-only — this
file is never parsed back, so no `parse`/`sanitize` half is needed) renders
the source's resolved fields (allowed biomes, starter biome/radius, starter
land, world limits, exterior plan, world layout plan, spawn strategy) plus a
header comment stating the mod version and creation timestamp. The MC
integration call site writes it to
`<worldFolder>/jlt_worldz-snapshot.yaml` via
`overworld.getServer().getWorldPath(LevelResource.ROOT)`, best-effort (log a
WARN and continue world creation on any `IOException` — never block world
creation over a reference file). The write itself lives inside
`SpawnOriginManager.markResolved` — the one place every `resolveFreshOrigin`
branch (found, not-found fallback, wrong strategy, missing generator) already
converges through exactly once per fresh world, with the final resolved
origin already in hand, so the snapshot's `layoutOriginBlockX/Z` fields are
accurate even for `preferred_natural_biome` worlds. The mod version comes
from a new build-time-expanded `${mod_id}-version.properties` classpath
resource (added to `build-logic`'s shared `multiloader-common.gradle`,
alongside the existing `fabric.mod.json`/`neoforge.mods.toml` expansion) read
once into `WorldzCommon.MOD_VERSION` — loader-neutral, avoiding a
Fabric-`ModContainer`-vs-NeoForge-`ModList` split for a cosmetic header
field.

#### Implementation (Phase 1.4 — global config hygiene)

`WorldzConfig.load()` no longer creates a file when `config/jlt_worldz.yaml`
is absent — it returns in-memory sanitized defaults directly, and the mod
never writes to disk on that path. A file that already exists is still
loaded, sanitized, and rewritten as before (unchanged: malformed/wrongly-
typed input is still never rewritten). The legacy `jlt_worldz.json`
migration path (`loadLegacy`, the `.json`/`.json.bak` handling) is deleted
outright — the new-worlds-only policy (§20.1) makes it dead weight, since
there is no cross-version config to carry forward either.

The in-file `_docs` map (data living inside the YAML the mod itself
read/wrote) is gone from the schema entirely. Documentation moved to
`config/jlt_worldz.example.yaml`, hand-authored with real `#` comments
above each key rather than generated: SnakeYAML's dumper cannot preserve
comments on round-trip, and building a comment-preserving emitter for a
file that changes only when a field is added would be more machinery than
the problem warrants. A JUnit test (`documentedExampleParsesToTheSameDefaultsAsCode`)
keeps the example's *values* honest by parsing it and comparing against
`new WorldzConfig()`'s own defaults — comments carry no data, so this
catches drift without needing byte-for-byte text comparison.

### 20.4 Real-seed sampling

GOALS repeatedly requires "randomness based on seed" (10, 12, 16, 08, 09).
The random-per-world sampling seed (§17's workaround for codecs decoding
without seed context) is replaced by capturing the real world seed at
generation time, where it *is* available to the generator (e.g. at
`ChunkMap` construction, which `ChunkMapMixin` already intercepts, or from
the server level). Same seed string ⇒ same Worldz decisions.

#### Implementation (Phase 1.3)

`ChunkMapMixin`'s existing `<init>` injection (both loaders) already receives
`ServerLevel level` and already reads `level.getSeed()` for the dummy-
`RandomState` fix -- the identical value is the real Minecraft world seed,
confirmed against the decompiled 26.2 `ChunkMap` constructor signature (no
further source verification needed; this is the same call already shipped
in 0.1.15). The injection now also calls
`LimitedBiomeSource.setLayoutSeed(level.getSeed())` when the generator's
biome source is a `LimitedBiomeSource`.

Unlike the seed-informed spawn origin (§18), this needs **no persistence of
its own**: `ServerLevel.getSeed()` already returns the same deterministic
value on every load (it is vanilla-persisted, not something Worldz has to
search for and remember), so the mixin can simply call `setLayoutSeed` every
time a level's `ChunkMap` is constructed -- on first creation and every
subsequent load alike -- with no `SavedData` and no
`resolveFresh`/`reapplyPersisted` split.

`WorldLayoutPlan` gained a pure `withSeed(long)` wither (returns `this`
unchanged if the seed already matches, otherwise a re-seeded copy -- mode,
biome pools, and every other field are untouched). `LimitedBiomeSource`
holds two views of the plan: `worldLayoutPlan()` keeps returning the exact
persisted plan (round-trips through the codec and the Customize screen
unchanged), while a new mutable `effectiveLayoutPlan` field (initialized to
the persisted plan, `volatile` for cross-thread visibility from the mixin's
injection point) is what `getNoiseBiome` actually samples from, and is what
`setLayoutSeed` updates via `withSeed`. `EnvelopedChunkGenerator`'s
`LayoutContext` was changed from snapshotting a `WorldLayoutPlan` at
generator-construction time (before `setLayoutSeed` can possibly have run)
to holding the `LimitedBiomeSource` itself and reading `effectiveLayoutPlan()`
live at each use -- the same "coordinate-shift-at-integration-boundary"
shape already used for origin recentering (§18), just for a plan reference
instead of a coordinate delta.

### 20.5 Vanilla pass-through selection (natural rivers/oceans)

For single-biome variants 13/14, do not compose terrain at all: sample what
vanilla would have chosen at that position; if it is a river (13) or
river/ocean-family (14) biome, keep vanilla's choice, otherwise substitute
the configured biome. Terrain, coastlines, and channels are exactly vanilla's
— this replaces everything the removed grid tried to do for water, with zero
height-adjustment machinery. (Answers GOALS Question 1 as well: biome
filtering never shaped terrain — see the Worldz5/6 finding in §17/MEMORY —
so endless ocean needs the terrain cap, and distant natural islands (04) come
from *releasing* that cap beyond the exclusion zone.)

**Phase 3.1 implementation decisions (2026-07-17):**

- **Scope:** `LayoutMode.SINGLE_BIOME` only — the only mode 13/14 apply to.
  Two new boolean flags on `LimitedBiomeSource` (not `WorldLayoutPlan`,
  which stays a pure per-cell hash sampler with no vanilla-biome-source
  access): `allowRivers` (13), `allowOceans` (14, additive over 13 — GOALS
  13's rivers stay available when oceans are also on). Persisted as new
  optional codec fields `allow_rivers`/`allow_oceans` (default `false`,
  same "explicit vs. config-default" resolution pattern as every other
  `LimitedBiomeSource` field keyed off `starter_radius`'s presence).
  `SingleBiomeConfig` gains matching `allowRivers`/`allowOceans` fields
  (default `false`); the Customize screen gains two toggle buttons.
- **What "vanilla would have chosen" means:** the full, *unfiltered*
  overworld `MultiNoiseBiomeSource` — the same `Climate.ParameterList`
  `resolveAllowedBiomes` already builds from `MultiNoiseBiomeSourceParameterList
  .Preset.OVERWORLD` before it gets filtered down to Worldz's allowed set.
  Sampling the *filtered* delegate would be self-defeating (rivers/oceans
  are essentially never in a single-biome world's tiny allowed set, so
  they'd never be selectable). Built once, lazily, alongside the existing
  memoized `Resolution` — no new per-column cost beyond the one extra
  `getNoiseBiome` call the check itself needs.
- **The actual check, in `LimitedBiomeSource.getNoiseBiome`:** runs after
  the exterior-ocean check and the starter-zone check (both keep unconditional
  precedence — the starter zone's guaranteed-land promise, and the
  guaranteed-land *terrain* raise `EnvelopedChunkGenerator` applies inside
  it, would otherwise disagree with an ocean biome label there), and before
  `WorldLayoutPlan.sampleAt`'s fixed single-biome return, gated on
  `mode() == SINGLE_BIOME && (allowRivers || allowOceans)`:
  sample the unfiltered vanilla source once; if `allowRivers` and the result
  is `BiomeTags.IS_RIVER`, or `allowOceans` and the result is
  `BiomeTags.IS_OCEAN` (verified in the real 26.2 biome tag data: `is_ocean`
  itself includes `#is_deep_ocean` as a nested tag, so one tag check covers
  every ocean depth/temperature variant), return that real vanilla biome
  holder; otherwise fall through to the existing single-biome substitution
  unchanged.
- **`possibleBiomes()`:** when a flag is on, the corresponding tag's full
  holder set (`biomeGetter.get(BiomeTags.IS_RIVER)` /
  `BiomeTags.IS_OCEAN`) is unioned into `resolveAllowedBiomes`'s `possible`
  set alongside the configured land/starter biomes. Without this, vanilla
  machinery that consults `possibleBiomes()` (structure-set precomputation,
  feature placement checks) would not know rivers/oceans can occur here even
  though `getNoiseBiome` can return them.
- **No new terrain code.** `EnvelopedChunkGenerator`'s starter-land/layout
  height adjustment is entirely biome-blind (it raises columns toward a
  target height regardless of what biome ends up there) and stays that way;
  a real river/ocean channel only reads as natural because *terrain* was
  never touched for `SINGLE_BIOME` outside the starter zone in the first
  place (§17/MEMORY's Worldz5/6 finding — biome selection was never a
  terrain-composition system). This is why 13/14 need zero coordination
  with the starter-land/layout-adjustment pipeline.

### 20.6 Design-first phases

Every phase in `TODO.md` that introduces a new world type or module starts
with a committed design task extending this section (verified 26.2 APIs,
chosen shapes, defaults), following the pattern that worked for §§16–18. The
executor designs details inside this section's decisions; it does not
re-litigate them.

### 20.7 Exclusion zone (shared module — design in TODO Phase 7.1)

A radial zone around the world origin (default 2000 blocks) with per-feature
semantics: suppress structure families inside it (GOALS 07, 24), or hold
terrain overrides inside it and release natural generation beyond it (GOALS
04, 08). One concept, one config shape, reused everywhere.

### 20.8 Starter chest (shared module — design in TODO Phase 8.1)

Configurable loot at spawn: named presets (easy/medium/hard, biome-informed
for sky islands) plus YAML-listed guaranteed and random items. Used by the
chest-boat ocean start (03), all sky variants (05–08), the cave start
(25–26), and the Nether-start (27) and End-start (34) difficulty tiers.

### 20.9 World-hazard rules (shared runtime module — GOALS 29–30, added 2026-07-16)

Unlike everything above, these are not worldgen: they are runtime rules
driven by server ticks + saved data, exactly the mechanism delayed border
resizing (§15) already uses, and they compose with any world type:

- **Forever night (30):** start at permanent night, or lock to night after N
  in-game days; once active, time is held at night and sleeping cannot skip
  it. Insomnia/phantom pressure is an explicit option (vanilla or relaxed).
- **Rising lava floor (29):** a persisted world-wide lava level with
  delay/rate/maximum expressed in the same days-based schedule idiom as
  borders. The hard design question is application: which blocks convert
  (air/water below the level) and how loaded vs. newly loaded chunks get the
  level applied without unacceptable tick cost — TODO Phase 18.2 design task,
  verified against 26.2 chunk/tick APIs before implementation.

### 20.10 Cave, Nether-start, and lava-ocean notes (added 2026-07-16)

- **Cave (25–26):** underground spawn placement can reuse §18's
  deterministic ring-search pattern (searching for a safe cavity at a
  configurable depth instead of a surface biome). The sealed-surface option
  and the mega-cave cavern both need a generation-approach spike (roof
  layer's interaction with heightmaps/mob rules; carver vs. feature vs.
  noise for the cavern) — TODO Phase 13.1. Beatability is free: strongholds,
  mineshafts, trial chambers, and underground portals all work without sky
  access.
- **Nether start (27):** the open question is initial spawn in a
  non-Overworld dimension — vanilla's spawn and respawn paths are
  Overworld-centric, so TODO Phase 14.1 is a §16.1-style feasibility spike
  (its findings must cover the End too, for the End start — GOALS 34)
  (verify `MinecraftServer`/`PlayerList`/respawn-anchor behavior in real
  26.2 sources, commit findings here) before any implementation. Every
  offered starter-chest tier must leave an escape/portal path.
- **Lava ocean (28):** the ocean-island shape with the exterior/cap fluid
  parameterized — water / lava / none (the dry world, 31, approved
  2026-07-16, uses "none"). Needs a 26.2 check on surface lava at scale:
  lighting cost, fire spread at the shore ring, fluid ticking, map color.
  Travel viability (striders/bridging) is an acceptance-test concern, not a
  code one.

### 20.11 Second-wave challenges (approved 2026-07-16 — GOALS 31–35)

- **Dry world (31):** ocean fluid "none" (drained basins) plus
  water-scarcity semantics: default keeps water that structures/features
  place naturally (village farms and wells, strongholds, aquifer pockets,
  springs); harder settings remove more (rivers, surface lakes).
  Beatability constraint at every difficulty: potions and other
  water-dependent progression must remain obtainable.
- **Strip world (32, 36):** vanilla `WorldBorder` is square-only (verify in
  26.2 sources), so the strip's long walls likely come from the
  exterior-envelope mechanism (void or solid wall), not the border — TODO
  Phase 6.1 spike. Stronghold/End-portal reachability inside the strip rides
  the existing progression guarantees (fallback portal). Optional Nether
  strip. Composes with length limits (17) and schedules (19–20). The
  biome-sequence variation (36) selects ordered biome bands every N chunks
  over untouched vanilla terrain — the chaos-biomes selection machinery
  (§20.11 above) with ordered bands instead of random cells; no height
  adjustment, so no coastline-class defects.
- **Chaos biomes (33):** the kept per-cell weighted selection machinery,
  land-role biomes only, over untouched vanilla terrain, with a configurable
  region size; compose with the vanilla pass-through (§20.5) so natural
  rivers/oceans can stay. No height adjustment anywhere → the removed
  coastline defect class cannot apply.

  **Phase 4.1 implementation decisions (2026-07-18):**

  - **A new `LayoutMode.CHAOS` value, not a `single_biome` variant** — it
    reuses the *generic* `WorldLayoutPlan`/`LayoutMode` machinery directly
    (the "kept per-cell weighted selection machinery" is `sampleUniform`,
    already exercised by `OCEAN` mode; `CHAOS` adds the missing `LAND`-role
    case: `sampleUniform(BiomeRole.LAND, landBiomes, x, z, "biome_land")`).
    Every configured biome (desert, jungle, ice_spikes, ...) classifies as
    `LAND` automatically via `BiomeRoles.defaultRole` with no new role
    concept needed. `WorldLayoutPlan`'s constructor gains the same
    "requires at least one candidate" guard `OCEAN` already has, requiring
    `landBiomes` non-empty for `CHAOS`.
  - **Own dedicated typed preset**, `jlt_worldz:chaos_biomes`, mirroring
    `single_biome`'s shape exactly (own small Customize screen, own
    `chaosBiomes:` config section, own `world_type` codec hint) rather than
    only being reachable through the generic `Worldz` preset's advanced
    layout screen — matches every other challenge family's discoverability
    in the World Type dropdown. The generic `Worldz` preset's
    `WorldzLayoutScreen` is deliberately *not* extended to include `CHAOS`
    in its mode cycle button (scope control — it doesn't fully support
    `SINGLE_BIOME`'s `allowRivers`/`allowOceans` either, an existing,
    accepted gap); YAML power users can still set `layout.mode: chaos`
    directly on the generic preset if they want, since `LayoutMode.parse`
    and `WorldLayoutPlan.fromConfig` are generic and need no chaos-specific
    changes for that path.
  - **Terrain is completely untouched** — `CHAOS` joins `LEGACY`/`VOID` in
    `EnvelopedChunkGenerator.resolveLayout`'s skip list, so no
    `LayoutContext` is built and `applyTerrainAdjustments`'s layout pass
    never runs for a chaos world. This is different from `SINGLE_BIOME`
    (which *does* raise columns toward guaranteed land) and is exactly what
    GOALS 33 asks for ("terrain shape stays vanilla") — natural hills,
    valleys, and (without the pass-through option) vanilla water bodies all
    stand exactly as the seed generated them, just relabeled.
  - **Vanilla pass-through (§20.5) generalized to `CHAOS` too**: the
    `LimitedBiomeSource` gate that was `mode() == SINGLE_BIOME &&
    (allowRivers || allowOceans)` becomes `(mode() == SINGLE_BIOME ||
    mode() == CHAOS) && (allowRivers || allowOceans)`. Reuses the exact
    same `allowRivers`/`allowOceans` fields and `naturalPassThroughBiome`
    check — a chaos world's river/ocean columns behave identically to a
    single-biome world's.
  - **Starter zone is optional, default off** — reuses
    `LimitedBiomeSource`'s existing, fully generic starter-biome/radius
    mechanism (already independent of `single_biome`) rather than inventing
    new scope. Default `starterBiome: ''` matches GOALS 33's literal
    reading (chaos from the very first block); setting a `starterBiome`
    gives a safe, guaranteed-land starting patch exactly like
    `single_biome`'s, for anyone who wants one. Not a scope expansion:
    the field already exists and already defaults to disabled.
  - **Config shape** (`chaosBiomes:` section, mirroring `singleBiome:`):
    `biomes` (weighted entries, `id` or `id@weight`, reusing
    `WeightedBiomeListSpec` — same syntax as the generic `layout.biomes`
    field), `regionScaleBlocks` (default `512`, same bounds as the generic
    field), `starterBiome`/`starterRadiusBlocks`/`spawn.strategy` (mirror
    `singleBiome:` exactly), `allowRivers`/`allowOceans` (default `false`,
    mirror `singleBiome:` exactly). `WorldzConfig.sanitizeLayout`'s
    existing per-mode "unsupported → fall back to legacy" switch gains a
    `CHAOS -> <no LAND-role biome>` arm alongside `OCEAN`'s equivalent
    check (only relevant to the generic preset's YAML path, since
    `chaosBiomes:` is sanitized independently, mirroring `singleBiome:`'s
    own sanitizer).
  - **Default biome list**: five visually distinct land biomes chosen to
    make "chaos" immediately obvious in-game — `minecraft:desert`,
    `minecraft:jungle`, `minecraft:ice_spikes`, `minecraft:badlands`,
    `minecraft:taiga` — equal weight. Easily overridden; not a GOALS
    requirement, just a sensible out-of-the-box default (same spirit as
    `single_biome`'s `minecraft:plains` default).
- **End start (34):** shares the Phase 14.1 non-Overworld-spawn spike. The
  hard design question is respawn (beds explode in the End, no anchors);
  must be hardcore-beatable with the starter chest tuned for a genuine but
  achievable dragon fight.
- **Stacked biome layers (35):** stacked horizontal biome slabs replacing
  the deep underground of a limited-size world — plains above desert above
  taiga. Interpretation confirmed by Jason 2026-07-16: each layer is a flat
  or low-relief slab using the flatter variants of its biome (a thin slice
  cannot fit extreme-hills-style relief), so the feature builds on the flat
  layer machinery (§19) plus the limits module, not full noise terrain per
  layer. Each layer has a configurable **air gap above its surface** so
  biome-specific trees and structures generate and grow on every layer
  (Jason, 2026-07-16) — gap height must accommodate the layer biome's tall
  features. Open design questions (TODO Phase 17.1): per-layer surface
  rules and feature generation within the gap, lighting and mob spawning
  below the top layer, layer config (order/thickness/air gap/seed-random),
  redistributing the deep-ore budget (lapis/gold/diamond), and
  stronghold/portal placement within the stack.
- **Multi-biome chunk islands (37):** the sky-chunk world's islands beyond
  the starter can carry different biomes, per-island top-only vs full-column
  depth, and — where feasible — showcased underground content (cave biomes,
  amethyst geodes, structure chunks). Whether specific features can be
  *targeted* per island (seed-search vs forced placement) is a TODO Phase
  12.2 design question.

## 21. Border resize styles + soft void border (2026-07-18)

Jason's Phase 5 review clarified GOALS 19–20 and added GOAL 38. Decisions
recorded 2026-07-18; all four were the recommended options. Planned as TODO
Phases 5b (stepped) and 5c (soft void border); nothing here is implemented yet.

### 21.1 Resize styles: continuous and stepped

Two styles per border schedule, selected by a new `resizeStyle` field
(`continuous` default — existing configs and saves keep today's behavior):

- **Continuous** (shipped): one smooth vanilla `lerpSizeBetween` across the
  whole transition. Keep as-is; Jason explicitly confirmed it (compelling for
  the collapsing challenge).
- **Stepped** (new): the border jumps abruptly by `resizeRateBlocks` every
  `resizeRateDays`, reusing the existing rate fields — in continuous style
  they mean "average speed", in stepped style "jump size / interval". Steps
  snap instantly via `WorldBorder.setSize` (no mini-lerp — the abruptness is
  the point; vanilla's border warning visuals still telegraph it). Chunks
  remain a UI-only unit (RadiusUnit), blocks the persisted unit, as
  everywhere else.

Mechanics: `BorderSchedule.radiusAtTick` gains the stepped curve —
`initial ± floor((elapsed − delay) / intervalTicks) × rateBlocks`, clamped at
`finalRadiusBlocks` — pure and JUnit-testable like the existing math. The
driver extends the existing machinery, not new plumbing: both loaders already
call `WorldLimitManager.onServerTick` every server tick; today it only starts
due transitions, for stepped schedules it also applies the next due step and
persists the next step tick in `WorldLimitState` (alongside the existing
pending-start tick). All timing reads the per-dimension clock
(`getDefaultClockTime` — see the 26.2 `getGameTime` deviation in MEMORY.md).
Because the stepped radius is a pure function of elapsed clock ticks, resume
after server restart is a recompute, not persisted lerp state — steps missed
while the schedule was due are applied on the next tick.

**Deferred (approved future scope):** `resizeCurve: linear | ease_out` — the
rate slows as the border approaches its final size. Continuous style
currently rides one vanilla lerp, which is linear-only; easing needs our own
per-tick driver, which the stepped work incidentally builds, so this gets
cheap afterwards. Not scheduled to a phase.

### 21.2 Soft void border (GOAL 38)

A "soft" edge: no invisible wall at all — terrain simply ends at the current
scheduled radius, void beyond, and the player can physically walk off the
edge and fall out of the world. The same `BorderSchedule` drives the
*envelope* radius over time instead of (or alongside) the vanilla
`WorldBorder`. Feasibility verified 2026-07-18 against the real 26.2 setup:

- Today the exterior envelope is **frozen into `EnvelopedChunkGenerator` at
  world creation** (persisted in the generator codec) and chunks generate
  exactly once. A scheduled void edge therefore needs the generator to read
  a **live radius** (a volatile snapshot maintained by the tick driver;
  chunk generation runs on worker threads, so no direct SavedData reads from
  the generator).
- **Collapse is the easy direction:** chunks first generated outside the
  shrunken radius are void automatically; terrain already generated outside
  it must be actively cleared to void — an incremental, per-tick-budgeted
  ring sweep (bounded block edits, no regeneration).
- **Expansion is the hard direction:** chunks the player already caused to
  generate as void (walking near the edge generates chunks well past it)
  must be **backfilled with real terrain** when the radius grows — i.e.
  chunk regeneration, WorldEdit-`//regen` style: run the delegate
  generator's stages into scratch `ProtoChunk`s and copy the result into the
  live chunks, then rebuild heightmaps/lighting and resync clients. The
  pipeline classes (`ProtoChunk`, `ChunkStatusTasks`, `WorldGenRegion`)
  exist in the 26.2 sources and our generator already drives the delegate's
  stages, so this is possible — but cross-chunk structures/decoration,
  lighting, thread safety, and performance budgeting make it the heaviest
  machinery proposed so far. **A spike proving single-chunk backfill in a
  test world is mandatory before the feature is scheduled for real** (TODO
  5c.1).
- **Backfill overwrites** whatever was built in the void ring (Jason,
  2026-07-18): documented challenge rule — the void is unclaimed; build out
  there at your own risk. Thematically the world "reveals itself".

## 22. Border presentation & enforcement (2026-07-18)

Second planning pass from Jason's Phase 5 review (GOAL 18 clarification +
new GOAL 39). Everything below was feasibility-verified against the real
26.2 sources/data on 2026-07-18; nothing is implemented yet (TODO Phase 5d).
Core model: the border's **visual** and its **enforcement** are independent
axes, both orthogonal to the exterior (per the standing border-vs-world-size
philosophy) and to the resize schedules (§21.1):

- `visual: striped | invisible` (+ optional marker ring)
- `enforcement: wall | damage | none`

### 22.1 Visuals

The vanilla border look has three separable layers (all verified):

1. **Physics** — collision/placement limits, in `WorldBorder` itself,
   independent of rendering. Untouched by visual options.
2. **Red warning vignette** — `Hud.extractVignette` computes its strength
   from `warningBlocks`/`warningTime`; with both set to 0 the strength is
   exactly 0 even during a lerp. Plain server-side setters — killable with
   **no client code**.
3. **Striped wall** (`forcefield.png`) — drawn by the dedicated client
   class `WorldBorderRenderer`, whose `render()` early-outs when
   `state.alpha <= 0`. A one-line client mixin (force alpha 0 in
   `extract`) hides the wall entirely, for static *and* moving borders.
   We already ship client mixins on Fabric and the NeoForge mixin
   bootstrap, so this is established machinery. Caveat (document): on a
   dedicated server an unmodded client still sees stripes — fine under
   the client-first acceptance policy.

**Marker ring** (optional module): a one-block ring on the surface just
beyond the boundary, marking an invisible edge without a curtain.
`EnvelopedChunkGenerator` already classifies every column by radius
(`modeAt`), so placing a marker block on boundary+1 columns at generation
time is a natural extension. Generation-time ⇒ **static borders only** (a
scheduled border would sweep past a stale ring — validate/reject the
combo). Open execution-time choices: marker block id (config, sensible
default), and behavior where the ring crosses water (seabed vs surface).

An alternative physical wall — a generated shell of `minecraft:barrier`
blocks — was considered and parked: it is the only option invisible to
unmodded clients on a server, but it is static-only, forces a wall-height
decision, and leaks visually in creative. Logged, not scheduled.

### 22.2 Enforcement: `wall | damage | none`

- **`wall`** — today's vanilla collision. Default.
- **`none`** — no border object; the edge is whatever the exterior does
  (the soft void edge). Static case needs zero new code: border disabled +
  void exterior + explicit `boundaryRadiusBlocks` is already expressible
  (promote via a test config). The *scheduled* version is GOAL 38 /
  Phase 5c.
- **`damage`** (GOAL 39) — permeable edge with time-based grace, then
  damage-over-time. Details below.

### 22.3 Damage enforcement (GOAL 39)

Vanilla precedent: `LivingEntity`'s tick already damages players outside
the border (`damagePerBlock × distance` beyond a `safeZone` buffer) — but
its grace is distance-based, silent, and the wall normally prevents
voluntary exit. We zero `damagePerBlock` and implement the time-based spec
as per-player logic in the existing `WorldLimitManager.onServerTick`
(already called every tick on both loaders):

- Crossing the current (scheduled) radius → chat warning with the grace
  time; grace timer starts. **Instant reset** on re-entry (Jason,
  2026-07-18) — no separate meter; health is the meter and eating is the
  recovery cost, so vanilla's regen-drains-hunger loop naturally prices
  repeated abuse.
- Grace expiry → periodic damage on a drowning-like cadence (default
  ~1 heart/s; amount and interval configurable) until back inside, then a
  short "safe" message. Creative/spectator exempt; mobs unaffected; state
  cleaned up on death/logout.
- Permeability: with `visual: invisible` no vanilla border object is
  needed at all (pure tick logic — no mixins). With `visual: striped`, the
  border object is kept and one mixin skips its collision shape (verified
  single injection point: `Entity.collectCollidersIgnoringWorldBorder`,
  gated on `WorldBorder.isInsideCloseToBorder`).
- Config sanity: `damage` + void exterior is a nonsense combo (you fall
  before grace matters) — warn at sanitize time. Sweet spots: natural
  generation beyond (GOAL 18) or ocean exterior (swim out and race back).
  Composes with §21.1 schedules for free — the check reads the current
  scheduled radius.

**Danger tint**: recommended implementation is a small custom overlay
(per-loader HUD hook; the same one-texture `vignette.png` blit vanilla's
`Hud` does) driven by *our* grace state — the tint starts at the crossing,
deepens as the grace runs out, and stays maxed during the damage phase, so
the tint itself is the countdown display. Cheap fallback (verified): keep
the vanilla border object and its vignette — `Hud.extractVignette` clamps
to full red when outside the border — at the cost of it being
distance-based rather than grace-aware.

**Damage type** (data + a small `DamageSource` holder lookup): own
`jlt_worldz` damage type JSON ⇒ custom death message lang key. Tag choices
enforce the design: `bypasses_armor` (armor points don't help —
drowning-like), `bypasses_effects` (Resistance V cannot grant its 100%
immunity), and **not** `bypasses_invulnerability` — verified that vanilla
`protection.json`'s only requirement is that tag's absence, so Protection
reduces border damage automatically, and vanilla caps
enchantment-protection stacking at 80%.

**No-immunity rule (Jason, 2026-07-18)**: no combination of armor,
effects, or enchantments may ever grant 100% protection outside the
border — mitigation may *slow* damage and *optionally extend* grace,
never eliminate them. Guaranteed structurally by the tag choices above
(Protection capped at 80%, Resistance bypassed) plus bounded custom-
enchantment effects (fixed per-level values with low max levels — e.g.
a few seconds of grace per level — chosen so damage stays meaningful).

**Enchantments** (26.2 enchantments are fully data-driven — verified):
vanilla Protection works with zero code (above). A custom enchantment
("Border Ward"-style) is JSON-only for damage reduction — a
`minecraft:damage_protection` effect scoped via damage-source predicate to
our damage-type tag — with availability/exclusivity as ordinary tags. The
grace-*extension* effect is the one code-side piece: read the enchant
level from armor in the tick logic and add bounded seconds per level.
Clean split: Protection = discoverable damage relief; the custom enchant =
the specialized grace tool you hunt for. Both bounded per the no-immunity
rule.
