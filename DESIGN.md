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
  exactly once.
- **Collapse is the easy direction:** chunks first generated outside the
  shrunken radius are void automatically; terrain already generated outside
  it must be actively cleared to void — an incremental, per-tick-budgeted
  ring sweep (bounded block edits, no regeneration).
- **Expansion is the hard direction:** chunks the player already caused to
  generate as void (walking near the edge generates chunks well past it)
  must be **backfilled with real terrain** when the radius grows.
- **Backfill overwrites** whatever was built in the void ring (Jason,
  2026-07-18): documented challenge rule — the void is unclaimed; build out
  there at your own risk. Thematically the world "reveals itself".

**Spike findings (TODO 5c.1, 2026-07-19):**

*Live radius (done, low risk).* `EnvelopedChunkGenerator.envelope` is now a
plain `volatile` field with a `setEnvelope(ExteriorPlan.DimensionEnvelope)`
setter (0.2.18). Every read site already re-reads `this.envelope` fresh per
call rather than caching it across a chunk's generation, and swapping in a
whole new immutable `DimensionEnvelope` is inherently atomic, so this needed
no other changes. Not yet wired to anything — no tick driver calls
`setEnvelope` yet; that is 5c.2's job if greenlit.

*Applying backfilled terrain to an already-loaded chunk (verified easy).*
Once new terrain exists as ordinary block states, applying it to a chunk a
player has already seen is completely ordinary: `Level.setBlock` (the same
API any block placement uses) calls `LevelChunk.setBlockState`, which
already updates all four heightmaps inline and queues the light engine
(`ThreadedLevelLightEngine.checkBlock`) whenever light-relevant properties
change, then `sendBlockUpdated` dispatches the client packet automatically.
**No custom relighting or client-resync code is needed** — this de-risks
half of the original 5c.1 task description ("rebuild heightmaps/lighting,
resync the client"), confirmed by reading `LevelChunk.setBlockState` and
`Level.setBlock` directly in the 26.2 sources.

*Producing correct backfill terrain (still unresolved, harder than
assumed).* This is the part that remains genuinely hard, and turns out
harder than the original "run the delegate's stages into a scratch
ProtoChunk" framing suggested. Read `ChunkPyramid`'s actual generation
graph: `STRUCTURE_REFERENCES`, `BIOMES`, `NOISE`, `SURFACE`, and `CARVERS`
each declare `addRequirement(ChunkStatus.STRUCTURE_STARTS, 8)` — an
**8-chunk-radius (17×17 chunk) neighborhood** must already have
structure-starts resolved before a single chunk can correctly reach
`FEATURES`/`FULL`, because vanilla structures can claim or influence
chunks far from their own origin. Hand-driving `ChunkStatusTasks`'
`generateXxx` methods ourselves (the literal reading of the task) would
mean either reimplementing that whole neighborhood-dependency cascade or
constructing `StaticCache2D<GenerationChunkHolder>`/`WorldGenContext`
instances ourselves — internal orchestration types that exist to be built
by `ChunkMap`'s own async pipeline, not by arbitrary third-party code
calling in in isolation. High effort, high risk of subtly-wrong output that
only surfaces as a visual defect in a live client, exactly what this
project's "no automated game tests, JUnit only" policy has no way to catch
before Jason manually finds it.

**Second research pass (2026-07-19, Jason: "get more information"), three
candidate designs now on the table:**

**B. Delete-and-regenerate** (WorldEdit-`//regen` style) — invalidate the
target chunk and let vanilla's own async pipeline regenerate it as if
freshly visited, instead of us reimplementing the neighbor cascade.
Verified further: `RegionFileStorage.write(pos, null)` calls
`region.clear(pos)` — deleting a chunk's persisted NBT is a genuine,
first-class, `public`, reflection-free vanilla operation
(`SimpleRegionStorage.write`, inherited by `ChunkMap`, reachable as
`((ServerChunkCache)level.getChunkSource()).chunkMap.write(pos, null)`).
And `ChunkMap.scheduleChunkLoad`'s `EMPTY`-status handling already falls
through to `createEmptyChunk` (a brand-new, unpopulated `ProtoChunk`)
whenever `readChunk` finds nothing persisted — at that point the *rest* of
the pipeline (structure-starts through full, including the whole 8-chunk
neighbor cascade) is orchestrated entirely by `ChunkMap`'s own
`ChunkGenerationTask`/`applyStep`/`StaticCache2D` machinery, exactly as
for any chunk a player explores into for the first time. **We would not
need to construct any of that ourselves** — only request the chunk again
at `FULL` status (a ticket, same as normal exploration). This is much
smaller and safer than approach A. The genuinely unresolved piece: forcing
an *already-resident* chunk (a live `LevelChunk` with a `ChunkHolder`
mid-ticket-lifecycle, likely with a player standing right next to it,
which is exactly our use case) to actually discard its in-memory state and
restart from `EMPTY` — deleting the region-file entry alone does nothing
while the chunk stays resident in memory, since the file is only consulted
on a fresh load. No public API for "downgrade this specific resident
chunk" turned up in `ChunkHolder`; this likely needs deeper ticket/promotion
manipulation, possibly a mixin. Still not attempted.

**C. Mask, don't discard (new, most promising).** Instead of ever
generating true void and later reconstructing terrain, let the delegate
generate the **real terrain normally and fully** for chunks in the
between-current-and-final-radius band (decoration, structures, everything
— exactly as vanilla would, with full natural neighbor context, since it's
generating in the normal order the first time it's ever visited). Persist
a hidden copy of that real terrain for any chunk currently masked as
void-exterior (a custom side-store, the same pattern this project already
uses for `WorldLimitState`/`SpawnOriginState`), then show the player void
by overwriting the *live* chunk with void blocks. "Reveal" later is simply
copying the cached real blockstates back over the live chunk via ordinary
`Level.setBlock` calls (already proven above to need zero custom
lighting/sync code) when the schedule's radius grows to include it — no
regeneration, no chunk-lifecycle manipulation, no neighbor-radius problem,
because the real terrain was already correctly generated once, in the
right order, with full context, at the normal time. Overwriting later with
cached real blocks matches Jason's already-decided overwrite rule exactly
(a player who builds on the void in the meantime loses that build when the
real terrain gets revealed over it).

Tradeoffs: needs new persisted storage for the hidden terrain (bounded to
the currently-unrevealed area between current and final radius, shrinking
as the border grows — not the whole theoretical world), and pays the full
generation cost for that band up front instead of deferring it — but since
that band is exactly the area the schedule guarantees will eventually
become real, playable terrain anyway, this isn't wasted work, it's the
same cost vanilla would pay whenever the player got there regardless. This
approach should be **scoped only to a border-schedule-driven soft-void
exterior**, not today's shipped static void exteriors (ocean/sky islands
etc., which stay void forever and must keep the cheap always-void
behavior) — a clean boundary, since it only applies to the new feature,
leaving every existing use case's code path untouched. `isEntirelyExterior`'s
existing skip of decoration/structures for permanently-void chunks would
need a schedule-aware carve-out so schedule-driven exterior chunks
decorate normally instead of being skipped.

**Recommendation:** pursue **C** if this is picked back up — it is the
only one of the three that avoids both major risks found so far (the
8-chunk neighbor-cascade reimplementation of A, and the resident-chunk-
discard uncertainty of B), at the cost of a well-understood kind of work
(custom NBT persistence) this project already does safely elsewhere,
rather than deep unverified engine surgery. Genuinely new code either way,
so it still needs live Prism testing to validate — this project has no
automated way to catch a subtly wrong result before Jason finds it live.

**Decision (Jason, 2026-07-19): defer.** GOAL 38 and Phase 5c.2 are set
aside, not abandoned — two research rounds produced a credible plan (C
above) but nothing is built or tested yet, and it isn't worth pursuing
blind right now. Work continues with Phase 6. If GOAL 38 comes back into
scope later, start implementation from approach C rather than
re-researching A/B from scratch.

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

## 23. Strip world / 1D Minecraft (GOALS 32, 36) — design spike (TODO 6.1)

A narrow corridor along one axis; everything happens in that strip. Design
verified 2026-07-19 against the real 26.2 sources and this codebase's
existing border/exterior/progression machinery.

**Vanilla `WorldBorder` is confirmed square-only.** Read the real class:
one `centerX`/`centerZ` pair and a single `extent` (size), used identically
for both axes in `getMinX`/`getMaxX`/`getMinZ`/`getMaxZ` — there is no way
to give it an independent X/Z half-width. Any genuinely rectangular bound
has to come from this mod's own exterior-envelope mechanism, not vanilla's
border.

**Key finding: the strip's *length* needs no new machinery at all.** Pick
one fixed axis for the corridor to run along — say X, width constrains Z
(seeds have no privileged axis, so hardcoding this is a pure implementation
simplification, not a scope gap). Size vanilla's `WorldBorder` to the
strip's *length* exactly as every other world type already does. The
border's redundant Z-reach (it's square, so it also nominally bounds Z to
the same large radius) is never actually experienced, since the width
constraint below always kicks in first, well inside the border's radius.
This means the *length* axis gets real collision, the existing delayed/
expanding/collapsing schedule machinery (GOALS 17/19/20), and the
warning vignette — all for free, unmodified, exactly satisfying "composes
with limited length (17) and the expanding/collapsing schedules (19-20)"
with zero new code.

**The *width* is the genuinely new piece, and is structurally limited to
"soft" (no real collision).** The existing exterior envelope
(`ExteriorPlan.DimensionEnvelope`) only ever governs *terrain
generation* (what's there), never player collision — collision on the
existing square shape comes exclusively from vanilla's border, which
(confirmed above) cannot be made rectangular. So there is no way to give
the strip's width a real invisible-wall push-back without building a
wholly new collision mechanism (e.g. an actual placed wall of
unbreakable blocks along both long edges) — out of scope for this phase
unless Jason wants it explicitly; the default should be the same "soft,
walk-off-into-void" experience already established for GOAL 38-style
soft edges, reusing `ExteriorMode` (`VOID`/`OCEAN`) exactly as-is.

**Recommended shape: additive, not a generalization of the square
envelope.** Rather than retrofitting `ExteriorPlan.DimensionEnvelope`
into a rectangle (touching the heavily-tested, already-shipped square
path used by every existing challenge type), add a small, new, orthogonal
concept — a `StripPlan` (`enabled`, `widthBlocks`, `widthMode: void |
ocean`, optional `applyToNether`) — checked *additionally* to whatever
the existing square envelope/border already decide: a column is
classified by the strip's width mode whenever `abs(z - originZ) >
widthBlocks / 2`, regardless of what the square envelope says, with the
strip's own check taking precedence when both could apply (they should
rarely overlap in practice, since a "narrow corridor" width is normally
much smaller than any configured square boundary). This is a pure
addition: zero behavior change, zero risk, to every existing world type,
since `widthBlocks` defaults to disabled/unbounded everywhere else.

**A real, concrete defect found for free by this exercise:**
`ObjectiveSite`'s fallback End-portal/fortress placement (`ProgressionGuarantees`)
bakes in the same square assumption — `fitsInside` checks `abs(x) <=
radius && abs(z) <= radius` against one shared radius, and
`FALLBACK_Z_CANDIDATES = {0, 64, -64, 128, -128}` can pick a Z offset
*outside* a strip narrower than 128 blocks, placing the compact fallback
portal in the void/wall zone. This needs the fallback-Z candidate search
(and `fitsInside`'s Z bound) to respect the strip's own `widthBlocks`
separately from the border's length radius — real, necessary 6.2 work,
not hypothetical.

**Preset shape:** own dedicated typed preset (`jlt_worldz:strip_world`,
name TBD at 6.2), mirroring `single_biome`/`chaos_biomes` — own Customize
screen, own config section, own `world_type` codec hint — matching this
project's established one-preset-per-challenge-family convention (GOALS
32 is its own numbered use case, not a variant), rather than folding a
"strip mode" toggle into the generic Worldz preset's advanced screens.

**Biome-sequence variation (36):** ordered (or seed-randomized) biome
bands selected over untouched vanilla terrain every N chunks along the
strip's length axis — the exact same per-cell selection machinery Phase
4's chaos biomes already built (`WorldLayoutPlan`/`sampleUniform`), just
walking bands in *order* along one axis instead of *randomly* over a 2D
grid. No height adjustment, so none of the removed coastline-defect class
can apply here either. Scoped to TODO 6.3, after the core strip (6.2)
ships.

**Nether strip (32, optional):** the same additive width-check applied to
the Nether's own `EnvelopedChunkGenerator` instance; independently
toggleable, mirroring how Nether border/exterior settings are already
independent of the Overworld's throughout this codebase.

Decided 2026-07-19; no code yet — 6.2 implements from this design.

## 24. Ocean island challenge, core (GOALS 01, 04) — design pass (TODO 7.1)

A small artificial island of one chosen biome at the origin, surrounded by
an endless generated ocean. Design verified 2026-07-19 against this
codebase's existing starter-land, exterior, layout, and progression
machinery.

### 24.1 Additive, not retrofit — a new `IslandPlan`, not a `StarterLandPlan` extension

TODO 7.1's own phrasing ("noise-perturbed radius over the existing
starter-land profile") could be read as extending the shared
`StarterLandPlan`/`StarterZone`/`StarterLandProfile` classes every other
typed preset already uses. Considered and rejected: those classes are
circular by design, unseeded (pure functions of `x`, `z`, and an
externally-sampled relief noise value), and shared by `single_biome`,
`chaos_biomes`, the generic `worldz` preset, and (indirectly) the
End/Nether progression code. Giving them a seed-dependent angular
perturbation would need a whole new "placeholder seed at decode, real seed
resolved at generation time" plumbing path (the same two-phase dance
`WorldLayoutPlan.seed`/`LimitedBiomeSource.setLayoutSeed`/
`effectiveLayoutPlan()` already exists for) grafted onto a class four other
already-shipped, already-tested presets depend on — real risk for no
shared benefit, since nothing else wants a non-circular starter zone.

Decision: **`IslandPlan` is a new, additive, ocean-island-only mechanism**,
threaded through `EnvelopedChunkGenerator` (mirroring `StripPlan`, added
Phase 6.2a) and `LimitedBiomeSource` (a new field, mirroring `allowBeaches`,
added the GOALS 36 follow-up) — not a modification of the shared
starter-land system. Every other preset is completely unaffected; the
island's land/shore/ocean-gradient logic lives entirely in new pure-logic
classes that *reuse the same approach* `StarterLandProfile` established
(smoothstep blending, a relief-noise-flavored natural variation) without
touching or depending on it directly. This matches this project's
established precedent exactly: strip world's width (Phase 6.1) was kept as
an additive check layered on the untouched square envelope rather than a
retrofit of `ExteriorPlan.DimensionEnvelope` into a rectangle, for the same
reason.

The shared starter-land system's own beach-ring width gap (BEACH currently
spans the whole `transitionWidthBlocks` blend, logged 2026-07-16, "not
fixed") is deliberately **not** touched by this phase either, for the same
risk-containment reason — `single_biome`/`chaos_biomes`/the generic preset
keep their existing (accepted, documented) behavior unchanged.
Ocean island gets a correctly narrow shore ring from day one because GOALS
01 requires it explicitly ("beach and/or stony shore to transition to the
ocean"), not because the shared system was patched.

### 24.2 Seeding the perturbation without new seed plumbing

`IslandPlan` itself carries no seed field. `WorldLayoutPlan.seed` is
populated and re-seeded the same way regardless of `mode` (a plain field on
the record, not gated on which mode is active), so ocean_island can read a
real, live-resolved seed via `LimitedBiomeSource.effectiveLayoutPlan()`
without needing its own seed-resolution plumbing, no matter which layout
mode it ends up using for the land biome itself (§24.9's as-built notes
settle that as `LEGACY`, not `SINGLE_BIOME` as first sketched here —
`SINGLE_BIOME`'s own unconditional `landFactor = 1.0` terrain-raise
would have fought the island's own shape-aware raise everywhere outside
the island). `EnvelopedChunkGenerator`'s own `LayoutContext.plan()` already
calls that same method live (confirmed by reading the class directly:
`LayoutContext` holds a reference to the `LimitedBiomeSource` instance and
calls `source.effectiveLayoutPlan()` on every access, never a stale copy),
and `EnvelopedChunkGenerator.originSource` gives the terrain code path its
own direct route to the same seed independent of `LayoutContext`/`layout`
being present at all — so both the biome-classification code path
(`LimitedBiomeSource`) and the terrain-height code path
(`EnvelopedChunkGenerator`) observe the identical, already-resolved real
world seed through the exact same object regardless of layout mode. The
new `IslandShapeProfile` perturbation functions take that seed as a plain
parameter
(`effectiveLayoutPlan().seed()` on one side, `layout.get().plan().seed()`
on the other — same value) and reuse `WorldLayoutPlan`'s own
`hash01`/`splitmix64` deterministic-hash primitives (not vanilla
`RandomState` noise) for the angular perturbation itself. Vanilla
`RandomState`-derived noise (`Noises.SURFACE_SECONDARY`, the way starter
land's relief noise already works) was considered and rejected here
specifically because `LimitedBiomeSource.getNoiseBiome` only ever receives
a `Climate.Sampler`, never a `RandomState` — it has no way to sample
arbitrary vanilla noise directly, so a `RandomState`-based approach would
work in `EnvelopedChunkGenerator` but not in `LimitedBiomeSource`, and the
two absolutely must agree pixel-for-pixel on where the coastline is (this
project has hit the biome/terrain-mismatch defect class before — see
§20.1's straight-coastline/floating-structure history). A seed-and-hash
function callable identically from both places, with no `RandomState`
dependency, sidesteps the mismatch entirely by construction.

### 24.3 Natural island shape

`IslandShapeProfile` (new pure-logic class) computes an angle-dependent
effective radius: `radiusAt(baseRadiusBlocks, amplitude, angleRadians,
seed) = baseRadiusBlocks * (1 + perturbation)`, where `perturbation` sums a
small number (3-4) of sine harmonics with per-harmonic amplitude decreasing
by `1/k` and a per-harmonic phase hashed deterministically from `seed` (via
`WorldLayoutPlan`'s existing hash primitives) — a cheap, deterministic,
JUnit-testable "lumpy circle" with a dominant low-frequency wobble plus
finer detail, clamped to a safe range (default amplitude `0.3`, hard clamp
`0.0`-`0.6` of the base radius) so no combination of harmonics can produce
a self-intersecting or negative-radius shape. `distanceFromShore(x, z,
baseRadiusBlocks, amplitude, seed)` returns `hypot(x, z) -
radiusAt(baseRadiusBlocks, amplitude, angleOf(x, z), seed)` — negative
inside the island, positive outside, zero at the (perturbed) coastline.
This single signed-distance function is the one shared primitive every
other piece of island logic (biome classification, terrain height, shore
ring, ocean gradient) is defined in terms of, so they can never disagree
about where the coastline actually is.

Terrain height reuses `StarterLandProfile`'s exact smoothstep-and-relief
approach (a new small function taking the signed shore distance instead of
a raw circular one, otherwise the same shape), so an island reads as "the
existing starter-land island, just not circular" rather than a visually
distinct algorithm.

### 24.4 Dedicated narrow shore ring (beach/stony-shore)

A new `shoreWidthBlocks` (default `12`, distinct from and narrower than the
terrain-height blend's own transition width) defines a ring measured
outward from the perturbed coastline (`0 <= distanceFromShore <=
shoreWidthBlocks`). Columns in that ring resolve to `BiomeRole.BEACH`, with
the biome itself a 50/50-weighted pick between `minecraft:beach` and
`minecraft:stony_shore` per column (a small dedicated weighted-pick helper,
not routed through `WorldLayoutPlan`'s land/ocean/beach lists — those exist
for the coordinated-layout system's own composition needs, which
ocean_island doesn't use). This directly fixes the logged beach-width gap
for this preset specifically (§24.1): a narrow ring at the true coastline,
not the whole raised-terrain blend.

### 24.5 Ocean depth/biome gradient

The generic `ExteriorMode.OCEAN`/`ExteriorTerrainProfile` mechanism
(`OCEAN_DEPTH = 16` fixed everywhere, one hardcoded `Biomes.DEEP_OCEAN`
holder in `LimitedBiomeSource`) is **not reused or modified** — it has no
notion of per-column depth variation and is shared by every other preset's
"ocean exterior" option, which stays exactly as flat/simple as it is today.
Ocean island's gradient is new, self-contained logic keyed off the same
`distanceFromShore` value:

- **Shore band** (`shoreWidthBlocks` to `shoreWidthBlocks +
  oceanShallowWidthBlocks`, default width `64`): shallow floor (default `8`
  blocks below sea level), biome weighted-picked from `{warm_ocean,
  lukewarm_ocean, ocean}` — GOALS 01's literal "shallow (warm/lukewarm)."
- **Deepening band** (next `oceanDeepenWidthBlocks`, default `128`): floor
  smoothly ramps (same smoothstep curve as everywhere else in this
  codebase) from the shallow depth to the deep depth (default `32` blocks
  below sea level); biome weighted-picked from the full 9-entry vanilla
  ocean set (`BiomeRoles`' existing `OCEAN_IDS` constant already enumerates
  exactly this list) — "all ocean biomes available."
- **Deep band** (beyond that, continuing to infinity — "endless ocean"):
  floor holds at the deep depth; biome continues drawing from the same
  full 9-entry set.

All width/depth numbers above are config defaults, not hardcoded
constants, for Jason to tune during acceptance testing without a code
change. The weighted picks reuse `WorldLayoutPlan`'s existing
`hash01`-based deterministic per-cell selection approach (same seed as
§24.2), sampled at a coarse region scale (not per-block) so the ocean reads
as patches of biome variety, not per-block dithering.

### 24.6 Progression guarantees at small radii

`ObjectiveSite.supportiveRadius` already returns a non-empty (finite)
radius whenever the exterior/envelope mode is non-`NORMAL`, independent of
whether a border is enabled — ocean island's `IslandPlan` mode is
non-`NORMAL` by construction, so `ensureObjective = true` on the Overworld
border settings (default for this preset, border itself left *disabled* --
GOALS 01 has no size limit, only the ocean shape) is all that's needed to
reuse the entire existing compact-fallback-End-portal machinery unmodified.
For a genuinely tiny island (radius near the GOALS 01 floor, "16 blocks/1
chunk"), the existing `fitsInside`/`NATURAL_STRUCTURE_MARGIN` (128 blocks)
check will essentially never consider the fallback site "safely fits," so
`supportiveFallbackZ` falls through to its documented last-resort `(0, 0)`
placement — meaning the compact 11x11 End portal structure gets built at
the island's own center, consuming most or all of a 1-chunk island's
surface. **Accepted trade-off, not a defect to chase**: GOALS 01 requires
beatability, not that a tiny island stays fully buildable-on; a player who
wants both a tiny island and room to build picks a larger radius. Documented
in README/MANUAL_TESTING rather than special-cased in code.

Nether stays fully vanilla by default (`ensureObjective = false` on the
Nether border settings, matching GOALS 01's "Nether... unchanged" exactly)
— no artificial restriction means no compact-fallback-fortress need;
ordinary vanilla Nether exploration already finds a natural fortress.

### 24.7 Exclusion zone (§20.7) and distant natural islands (GOALS 04, TODO 7.3)

The core ocean_island preset (TODO 7.2, GOALS 01) ships with the ocean
gradient's deep band extending to infinity — no natural land anywhere
beyond the artificial island, ever. GOALS 04 is the *same* preset with one
additional toggle: an **exclusion zone** (`exclusionZoneEnabled`, default
`false`; `exclusionZoneRadiusBlocks`, default `2000` per §20.7's own
placeholder) beyond which `IslandPlan`'s masking releases entirely and
`effectiveModeAt` reports `NORMAL` again, letting the delegate's real
vanilla terrain resume — small natural islands then occur wherever the
seed's own terrain noise happens to poke above sea level (§20's Q1 answer:
confirmed no additional "island guarantee" is needed or possible, since
biome and terrain shape are independent in modern Minecraft and restricting
biomes alone doesn't suppress land). This is a pure extension of
`effectiveModeAt`'s existing radius check (compare against the exclusion
radius in addition to the island's own perturbed radius) — no new terrain
math beyond what 7.2 already builds. Reused as-is by later phases per
§20.7 (GOALS 07, 08, 24) once they need it.

### 24.8 New typed preset shape

`jlt_worldz:ocean_island` (GOALS 01-04), following the established
one-preset-per-challenge-family pattern exactly: `OceanIslandConfig`
(island biome, `radiusBlocks`, `shapeAmplitude`, `shoreWidthBlocks`, ocean
gradient widths/depths, `exclusionZoneEnabled`/`exclusionZoneRadiusBlocks`),
`OceanIslandCustomization` record, `OceanIslandPresetEditor`,
`OceanIslandCustomizeScreen`, `ocean_island.json` preset resource + `normal`
tag entry + lang keys, both loaders' registration, matching resource/
structural tests. Unlike every prior typed preset, ocean_island has **no
spawn-strategy option** — the island only ever exists artificially at the
origin, so `PREFERRED_NATURAL_BIOME`/`VANILLA_SPAWN` are meaningless for
it; spawn is always the island's own safe surface point near the origin
(reusing the existing `STARTER_AT_ORIGIN`-style safe-spawn search, just
with no strategy toggle exposed). The shared Overworld/Nether Border,
End Border, and Nether Exterior Customize buttons remain available and
optional (all default off/normal) for players who want to compose a size
limit or restrict the Nether on top of the island shape; there is
deliberately no separate Overworld Exterior toggle, since `IslandPlan`
unconditionally supplies the Overworld's entire exterior itself.

Radius bounds are new, dedicated constants
(`MIN_ISLAND_RADIUS_BLOCKS = 8`, satisfying GOALS 01's "16 blocks/1 chunk"
under either a diameter or radius reading; `MAX_ISLAND_RADIUS_BLOCKS =
65536` as a generous "huge" ceiling) rather than the shared
`MIN_STARTER_RADIUS_BLOCKS`/`MAX_STARTER_RADIUS_BLOCKS` (64-4096) every
other preset's starter zone uses — those bounds were tuned for "a starter
zone inside an otherwise-normal world," not "the entire visible island,"
and GOALS 01 explicitly requires going smaller than 64.

Decided 2026-07-19; no code yet — 7.2/7.3 implement from this design.

### 24.9 As-built notes (TODO 7.2)

- **`ExteriorTerrainProfile` gained explicit-depth overloads** (`oceanFloorY`/
  `oceanLayerAt`/`baseHeight`, each now also accepting a `depthBlocks`
  parameter; the original fixed-`OCEAN_DEPTH` overloads delegate to them
  unchanged) rather than a parallel island-only copy of the same block-layer
  math -- a small, additive, backward-compatible refactor (every other
  preset's plain `ExteriorMode.OCEAN` option is untouched) that let the
  island's shallow-to-deep gradient reuse the exact same bedrock/stone/
  water/air classification instead of duplicating it.
- **`resolveStripWorldAllowed` renamed to `resolveFullVanillaOverworldAllowed`**
  in `LimitedBiomeSource` -- its "full `#minecraft:is_overworld` tag" logic
  was never actually strip-specific, and ocean_island's own fieldless-preset
  default (and its exclusion-zone fallback delegate, GOALS 04) needed the
  exact same thing.
- **The GOALS 04 exclusion-zone mechanism shipped as part of 7.2**, not as
  a separate 7.3 change: `IslandPlan.withinExclusionZone` is a single check
  already threaded through every column classification
  (`effectiveModeAt`, `islandTargetHeight`, `islandOceanDepthAt`,
  `LimitedBiomeSource.getNoiseBiome`'s island branch) from the start, since
  retrofitting it in after the fact would have meant re-touching the exact
  same call sites a second time for no benefit. 7.3 is therefore a
  test-config/documentation task confirming already-shipped behavior, not
  a new implementation task.
- **Seeding confirmed to work exactly as designed in §24.2**: ocean_island's
  `WorldLayoutPlan` stays `LEGACY` (not `SINGLE_BIOME` -- an earlier idea
  rejected during implementation once it became clear `SINGLE_BIOME`'s own
  `landFactor = 1.0` terrain-raise would fight the island's own shape-aware
  raise everywhere outside the island, since that mode has no notion of a
  radius at all). `LimitedBiomeSource.effectiveLayoutPlan().seed()` is
  still populated and re-seeded normally regardless of mode, so the "free"
  real seed described in §24.2 holds even at `LEGACY` -- confirmed by the
  full test suite passing with zero changes needed to the seed-resolution
  path itself.
- **Found and fixed during 7.2's own review, before any in-game testing:**
  `WorldLimitManager.onServerStarted`'s `exteriorObjective` gate and
  `ObjectiveSite.supportiveRadius` both only ever consulted
  `ExteriorPlan`/border state to decide whether a world needs the fallback
  End-portal guarantee. Since §24.1/24.5 deliberately keep the island's
  exterior *out* of `ExteriorPlan` entirely (the flat single-depth
  envelope model can't represent the gradient), every ocean_island world
  looked exactly like an unlimited normal world to this check and the
  guarantee silently never fired -- a complete beatability failure for
  the whole preset, caught by re-reading `WorldLimitManager` against the
  new `island` field rather than by a later bug report. Fixed with a new
  `ObjectiveSite.supportiveRadius(borderEnabled, finalBorderRadius,
  envelope, island)` overload (returns the tightest of border/envelope/
  island radii; delegates unchanged to the existing overload when island
  is disabled) and threading `IslandPlan` through
  `ProgressionGuarantees.ensureEndPortal` and the `exteriorObjective`
  gate. `ensureBlazeAccess` (Nether) needed no equivalent change --
  GOALS 01 keeps the Nether completely vanilla, so its own guarantee
  path is correctly untouched.
- **Known, deliberately deferred edge case, not chased further:**
  `ObjectiveSite.isSupportiveColumn` treats every column as supportive
  whenever the layout plan is `LEGACY` (its documented fast path for "no
  coordinated layout, assume supportive") -- but for ocean_island,
  `LEGACY` doesn't mean "no information," it means "handled by
  `IslandPlan` instead," so this fast path can't actually tell open ocean
  from island interior. In practice this rarely matters: the fallback
  portal's own `NATURAL_STRUCTURE_MARGIN` (128 blocks) means candidate
  Z offsets other than `0` essentially never "fit" inside a modestly
  sized island anyway, so placement already falls through to the
  documented `(0, 0)` last resort -- unambiguously inside any island
  shape regardless of amplitude. Only a fairly specific island size/
  amplitude combination could theoretically place the fallback at a
  candidate offset that clears the margin check yet lands outside the
  perturbed coastline in that direction. Threading `IslandPlan` into
  `isSupportiveColumn` itself would close this properly; deferred rather
  than chased now given how narrow the actual exposure is -- revisit if
  Jason's acceptance testing (large/oddly-amplituded islands especially)
  ever actually surfaces it.

### 24.10 Test-1 findings and fixes (config 30, 0.2.31 → 0.2.32)

Jason's first in-game pass (config 30, default 128-radius island) surfaced
three issues, all confirmed against the actual server log and screenshots
before any fix was written:

- **Fallback End portal always built at the world floor (`y = -64`), a real,
  general, pre-existing bug -- not ocean_island-specific.** Every single
  logged `ensureEndPortal` call across every prior test session (radius 128
  *and* radius 2048 alike) shows the identical `BlockPos{x=32, y=-64, z=0}`.
  Root cause, confirmed against decompiled `Level.java`:
  `Level#getHeight(Heightmap.Types, x, z)` returns `getMinY()` outright for
  any chunk that is not *already loaded* -- `LevelReader`'s plain height
  query never forces generation the way `getChunk(chunkX, chunkZ)` does.
  `ProgressionGuarantees.ensureEndPortal` runs at world creation, before the
  fallback site's own chunk has ever loaded, so it hit this fallback path
  every time. Fixed with a one-line `overworld.getChunk(x >> 4, z >> 4)`
  (forces synchronous `ChunkStatus.FULL` generation) immediately before the
  height query. `ensureBlazeAccess`'s Nether fallback needed no equivalent
  fix -- its spawner Y is a fixed constant, not a heightmap query.
  `SpawnOriginManager.safeSpawnNear`'s own `getHeight` call has the same
  latent shape but is guarded behind a `height < overworld.getMinY()`
  condition that real generators essentially never trigger; left alone
  (see MEMORY.md) rather than speculatively touched with no reproduction.
- **Ocean biome patches formed a visible checkerboard, confirmed in
  screenshots (both first-person and the map).** `IslandOceanProfile
  .biomeAt` picked a biome per raw axis-aligned grid cell
  (`Math.floorDiv(x, scale)`) with zero blending -- literal square regions.
  Replaced with jittered-grid Voronoi: each grid cell gets a seed-hashed
  feature point offset within it (`JITTER_MARGIN = 0.2` of the cell edge,
  so the true nearest feature point is always within the query cell's 3x3
  neighborhood), and the *nearest* feature point's cell wins the biome
  pick. Same per-cell deterministic hash as before, just keyed on the
  jittered cell coordinates instead of the raw grid ones -- boundaries
  now read as organic patch edges instead of a grid line. `shoreBiomeAt`
  (already per-block, already fine) is untouched.
- **Island coastline read as an unnaturally smooth blob.** `IslandShapeProfile`
  computes radius as a function of angle alone (4 sine harmonics) --
  mathematically this can only ever produce a smooth, single-lobed "lumpy
  circle" (star-convex by construction), never coves, inlets, or
  fractal-scale roughness, regardless of amplitude. Added a second,
  independent perturbation term: classic hashed-lattice value noise
  (smoothstep-interpolated), added directly to the *distance* field rather
  than to the angle-based radius, with wavelength and amplitude both
  scaled off the island's own base radius (`baseRadiusBlocks / 6`,
  floored at 4 blocks; `8%` of `baseRadiusBlocks`, clamped to `1..32`
  blocks) so a 1-chunk-floor island and a multi-thousand-block one each
  get proportionate small-scale detail. Rides the same `amplitude` dial as
  the large-scale harmonics (scaled by `clampedAmplitude / MAX_AMPLITUDE`)
  rather than always being on, so `amplitude = 0` still gives an exact,
  fully predictable circle -- kept deliberately, since a tiny island with
  uncontrolled small-scale noise could otherwise lose a large fraction of
  its own area. Being a pure function of `(x, z, seed)` added to the same
  `distanceFromShore` every caller already shares, no separate wiring was
  needed anywhere else -- biome classification, terrain height, the shore
  ring, and the ocean gradient all inherit the new roughness automatically.

All three fixes are pure-logic changes covered by new/updated JUnit tests
(see `IslandShapeProfileTest`, `IslandOceanProfileTest`); none required
touching `EnvelopedChunkGenerator`, `LimitedBiomeSource`, or any codec.
Re-deployed as 0.2.32 for Jason to re-test config 30 specifically.

### 24.11 Test-2 findings and fix: sterile ocean (0.2.32 → 0.2.33)

Jason's 0.2.32 re-test confirmed the portal fix (now surfaces correctly)
but found the ocean itself "sterile" -- no vegetation, no fish/squid
population at world-gen time, no shipwrecks/ocean ruins/monuments.

Root cause: `EnvelopedChunkGenerator.applyBiomeDecoration`/
`spawnOriginalMobs`/`createStructures` all skip the delegate entirely for
any chunk where `isEntirelyExterior` is true (every corner column is
`OCEAN` or `VOID`) -- this is deliberate, pre-existing, general behavior
(DESIGN §14: the wrapper "masks base terrain/surfaces/carvers/decorations
outside" the solid region), shared by every preset with an ocean/void
exterior. For strip_world/single_biome/chaos_biomes this was always a
low-stakes trade-off -- their exterior ocean is an incidental boundary
nobody is meant to explore. For ocean_island it guts the entire
explorable ocean (everything past one chunk from shore), which directly
undercuts GOALS 01's "shallow to deep, but all ocean biomes available"
premise -- an ocean with zero life reads as broken, not as a boundary.

Jason chose the full fix, scoped to `island.enabled()` only (not a
blanket change to the shared exterior mechanism, to keep
strip_world/single_biome/chaos_biomes's own already-shipped, already-
tested exterior-ocean behavior byte-for-byte unchanged): a new
`decoratesExteriorOcean(ChunkPos)` check (true only when the island is
enabled *and* every corner of the chunk is specifically `OCEAN`, never
`VOID`) now lets `applyBiomeDecoration`/`spawnOriginalMobs`/
`createStructures` run the normal vanilla pass for those chunks instead
of skipping it. `isEntirelyExterior`'s corner-checking loop was factored
out into a small `allCornersMatch(ChunkPos, Predicate<ExteriorMode>)`
helper so the new OCEAN-only variant (`isEntirelyExteriorOcean`) shares
it rather than duplicating the loop.

**The subtlety that made this more than a one-line gate flip:**
`applyBiomeDecoration` calls `applyEnvelope(chunk)` *after* the delegate's
decoration pass on every invocation -- that repaint unconditionally
overwrites every exterior column back to the flat bedrock/stone/water/air
profile, which would immediately erase any kelp/seagrass/structure
pieces decoration had just placed. The fix skips that trailing
`applyEnvelope` call specifically when `decorateExteriorOcean` is true --
safe because the *earlier* `applyEnvelope` calls inside `fillFromNoise`
and `applyCarvers`/`buildSurface` (all of which run before
`applyBiomeDecoration` in the generation pipeline) have already shaped
the column correctly by the time decoration runs; there is nothing left
to repaint. `spawnOriginalMobs` and `createStructures` needed no
equivalent change since neither calls `applyEnvelope` at all.

Structures and decoration both rely on `getBiomeSource()`/
`getBaseHeight()`/`getBaseColumn()` for placement decisions, all of which
already report correct real ocean biomes and correct synthetic depth for
island columns (unchanged since 7.2/24.10) -- so shipwrecks, ocean ruins,
and monuments should place using the same logic vanilla always has,
just against our painted terrain instead of noise-generated terrain.
Mixed chunks (partly island, partly ocean -- i.e. not "entirely
exterior") were already fully decorated before this change and are
untouched by it; the narrow pre-existing edge case where a mixed chunk's
ocean-side columns can still lose decoration to the trailing
`applyEnvelope` repaint was not touched here, out of scope for this fix.

No test file exists for `EnvelopedChunkGenerator` itself (it needs a real
Minecraft server runtime, consistent with the project's JUnit-only, no
automated game tests convention); validated by a clean full build and the
existing 352-test suite passing completely unchanged (proof the change is
a no-op for every non-island preset). Re-deployed as 0.2.33 for Jason to
re-test the exterior ocean specifically.

### 24.12 Test-2 follow-up: buried, enclosed portal vault (0.2.33 → 0.2.34)

Confirming the 0.2.32 Y-fix, Jason's 0.2.33 re-test found the fallback
End portal correctly landing on the terrain surface (`y = 75`/`76`,
logged) -- but flagged that as itself a new problem: `buildEndPortalSite`
was only ever designed to sit *underground*, relying on the surrounding
natural stone to act as walls (it only ever built a floor and four
corner posts, never real walls or a ceiling). Buried in bedrock (the
0.2.29-0.2.31 bug), that omission was invisible by accident; sitting on
the open surface, it read as an incomplete, exposed platform.

Jason's direction: "below ground like the stronghold... somewhere
between Y-10 and Y-60... in an enclosed room, like the Portal Room."
Two changes:

- **Placement**: `ProgressionGuarantees.ensureEndPortal` no longer
  queries surface height at all. A new `FALLBACK_PORTAL_TARGET_Y = -32`
  constant (mid-band of Jason's requested range, clamped to
  `overworld.getMinY() + 5`) replaces it -- deliberately a fixed depth,
  not surface-relative, since the vault is carved and walled in
  regardless of what's actually there (stone, water, an intersected
  cave) either way. `overworld.getChunk(x >> 4, z >> 4)` is kept (still
  needed to safely place blocks in a chunk that hasn't loaded yet at
  world-creation time), just no longer for a height query.
- **Structure**: `buildEndPortalSite` rewritten to mirror
  `buildBlazeSite`'s existing shell approach (which already did this
  correctly for the Nether blaze-spawner fallback) -- full stone-brick
  floor, ceiling, and all four walls (not just corner posts), with a
  3-wide, 2-tall doorway carved into the north wall so the vault isn't
  sealed. `placePortalFrames` and the portal's own footprint are
  untouched; only the surrounding shell changed.

**Known, deliberately deferred edge case, not chased further:** a fixed
`Y = -32` assumes normal solid ground extends continuously down to
bedrock, which holds for every terrain-generating preset tested so far
but not necessarily for a `LayoutMode.VOID` world's floating starter
island (Phase 15.5) -- a thin floating landmass surrounded by empty sky
void might not have any solid material anywhere near `Y = -32`, and the
vault would build as a floating brick box in open void instead of a
buried one. Not fixed now: nobody is currently testing VOID-layout
fallback portals, Jason's request was specifically about normal/
ocean_island terrain (where this assumption always holds, matching real
stronghold generation's own Y-band), and detecting "is there actually
solid ground here" robustly would need real sampling logic disproportionate
to how rarely this fallback path is hit for VOID worlds. Revisit if a
VOID-layout world's fallback portal is ever actually reported floating.

No test file exists for `ProgressionGuarantees` (needs a real server
runtime); validated by a clean full build and the unchanged 352-test
suite (this change touches no pure-logic class). Re-deployed as 0.2.34
for Jason to re-test the fallback portal's depth and enclosure.

### 24.13 Test-3 finding and fix: contiguous shore arcs (0.2.34 → 0.2.35)

0.2.34's re-test confirmed both the portal depth/enclosure and (per
Jason) that the ocean is no longer sterile. One further finding: the
shore ring's beach/stony-shore mix reads as speckled -- individual
blocks flip between the two biomes rather than forming stretches. Root
cause: `IslandOceanProfile.shoreBiomeAt` picked a biome per raw block
(`hashIndex(seed, x, z)`), completely independent between neighboring
columns.

Jason's request: alternate beach and stony shore in contiguous stretches
along the coastline, with the stretches varying in length (not uniform).
Fixed by reframing the pick in terms of position *along the coastline*
rather than raw block coordinates: `shoreBiomeAt` now computes the
column's angle (`atan2(z, x)`, same angle every other island primitive
already keys off) and runs a 1D analog of `biomeAt`'s jittered-grid
Voronoi -- the full circle is divided into `segmentCount` angular
segments (sized so each averages `SHORE_ARC_TARGET_LENGTH_BLOCKS = 32`
blocks of arc length at the island's own `baseRadiusBlocks`, floored at
`SHORE_ARC_MIN_SEGMENTS = 4` for very small islands), each segment gets
a seed-hashed jittered feature angle, and the nearest feature angle's
segment index picks the biome. Jittered Voronoi cells are inherently
uneven in size, which is what delivers "varying length" for free without
a separate length-randomization step. Wraparound handled via
`angularDistance`'s circular distance rather than a plain angle
subtraction. New `shoreBiomeAt` parameter `baseRadiusBlocks` (so segment
density scales with island size, keeping arc length roughly constant in
blocks rather than in degrees); its one caller,
`LimitedBiomeSource.islandBiomeAt`, now passes `this.island.radiusBlocks()`.
`SHORE_ARC_SALT` XORed into both the jitter and final-pick hashes so
this arc-Voronoi doesn't correlate with the ocean-region one right above
it in the same file, even though the two operate on different domains
(angle vs. x/z) anyway.

Two new tests (`IslandOceanProfileTest`) verify the qualitative
properties Jason asked for without hardcoding hash-dependent exact
values: `shoreArcsAreContiguousNotSpeckled` sweeps the full coastline at
a fixed radius and asserts the transition count stays well below what a
per-block coin-flip would produce; `shoreArcLengthsVary` sweeps a larger
radius and asserts the resulting contiguous run lengths are not all
identical. Full suite green (354 tests); clean build. Re-deployed as
0.2.35.

## 25. Ocean island extras (GOALS 02, 03) — design pass (TODO 8.1, 8.2)

### 25.1 Scope and structural decision

GOALS 02 (natural island by seed) and 03 (chest boat, no land) are both
"same as 1 [ocean_island], but..." variants: they reuse the entire ocean
gradient (`IslandOceanProfile`), beatability guarantees, exclusion zone
(GOALS 04), and unchanged Nether/End — only *how the land is sourced*
changes. **Confirmed with Jason: extend the existing `ocean_island`
preset with a new "Island Source" choice, rather than ship three
separate presets.** One World Type entry, one set of
JSON/lang/Customize-screen/test scaffolding to maintain, matching how
other presets already express a variant as a sub-choice (e.g.
`SpawnStrategy`) rather than a sibling preset.

New `logic.IslandSource` enum: `ARTIFICIAL` (existing, default — unchanged
behavior byte-for-byte), `NATURAL` (GOALS 02), `CHEST_BOAT` (GOALS 03).
Added to `OceanIslandCustomization` as a new field; every already-shipped
`config/tests/30`-`33` file and prior test coverage keeps working
unchanged since it decodes as `ARTIFICIAL` by default (legacy-overload/
fieldless-default pattern, same discipline as every prior addition this
phase).

### 25.2 `IslandPlan.hasLand` (backs GOALS 03)

New boolean `hasLand` field on `IslandPlan` (14th component), defaulting
`true` everywhere except `CHEST_BOAT` mode's own island plan. When
`false`, every consumer treats the column as "beyond the shore" even at
distance `0` from the (nonexistent) coastline -- no interior biome, no
shore ring, ever:

- `LimitedBiomeSource.islandBiomeAt`: the interior (`distance <= 0`) and
  shore-ring (`distance <= shoreWidthBlocks`) branches are skipped when
  `!hasLand`; every column goes straight to `IslandOceanProfile.biomeAt`,
  with `beyondShore = distance` (not `distance - shoreWidthBlocks`,
  since there is no shore ring to subtract) -- the shallow-to-deep
  gradient starts right at the spawn point instead of past a ring that
  doesn't exist.
- `EnvelopedChunkGenerator.effectiveModeAt`'s island branch returns
  `OCEAN` unconditionally (never `NORMAL`) whenever `!hasLand`.
- `islandTargetHeight`/`applyTerrainAdjustments`'s per-column raise
  no-ops immediately when `!hasLand`, mirroring the existing
  `!island.enabled()` no-op path exactly.
- `islandOceanDepthAt` uses raw `distance` instead of
  `distance - shoreWidthBlocks` when `!hasLand`, for the same "no ring to
  subtract" reason as the biome pick above.

This reuses the entire existing gradient/depth/biome-pool machinery
unchanged; only the "is there land at this column at all" branch varies.
Every one of these four call sites is exactly the kind of "is anything
special active here" gate this session has repeatedly found broken by a
new mechanism (Phase 6.2a, Phase 7.2's `hasActiveExterior`/
`applyTerrainAdjustments`, the 0.2.30 beatability bug) -- auditing all
four up front here, before writing any code, is deliberate.

`OceanIslandCustomization.islandPlan()` builds the `IslandPlan` with
`hasLand = (islandSource != CHEST_BOAT)`; `islandBiome`/`radiusBlocks`
still need *some* valid value even when land is absent (the record's own
validation still runs), so `CHEST_BOAT` mode reuses the exact
placeholder convention `IslandPlan.disabled()` already established
(minimum radius, `minecraft:plains`) -- harmless, since `hasLand=false`
guarantees that biome is never actually selected anywhere.

### 25.3 Starter chest infrastructure (TODO 8.1, backs GOALS 03)

New pure-logic `logic.StarterKitPlan` (JUnit-testable, no Minecraft
runtime needed for its own logic): an "essentials" list (fixed item/count
pairs, always included) plus a YAML-configurable "extras" pool (a list of
possible item/count pairs) and an extras-pick count, resolved
deterministically from the world seed so a given world always hands out
the same kit. Kept general enough that a later phase (10 sky island, 25
cave, 27 Nether, 34 End) can layer a tier concept (easy/medium/hard) on
top later without retrofitting this record -- **but no tier enum is
built now**, since GOALS 03 itself never asks for one and speculative
tiering today would be unused code (judgment call, documented here so it
isn't re-litigated later).

Default essentials (GOALS 03's own named list): 1 lily pad, 4 dirt, 2
grass blocks, 3 oak saplings. Default extras pool: a small, clearly
"nice to have, not essential" set (a loaf of bread, a wooden axe/pickaxe,
a torch stack, a water bucket) with a default extras-pick count of 2 --
proposed defaults, reviewed here for Jason rather than blocked on his
sign-off; adjustable via YAML like every other Worldz default, and easy
to correct from his in-game testing feedback.

Placement: `EntityTypes.OAK_CHEST_BOAT` (confirmed via `javap` against
the compiled 26.2 jar -- 26.2 has a separate chest-boat entity type per
wood species, e.g. `OAK_CHEST_BOAT`/`BIRCH_CHEST_BOAT`/etc., all
extending `AbstractChestBoat implements ContainerEntity`, with a plain
`setItem(int, ItemStack)` on a fixed-size container) spawned at the
resolved spawn point in the water, its inventory populated from the
resolved `StarterKitPlan`. Player spawn position is placed at/adjacent
to the boat (GOALS 03: "spawn on/next to a chest boat"). Oak chosen as
the one fixed wood species -- GOALS 03 doesn't ask for a configurable
boat material, and picking one avoids a needless extra config knob.

### 25.4 Natural island by seed (TODO 8.2, backs GOALS 02)

Reuses `SpawnOriginManager`'s existing `PREFERRED_NATURAL_BIOME`
machinery -- the same real `RandomState`/`Climate.Sampler` construction,
the same `SpawnSearchPlan.defaults().offsetsInSearchOrder()` concentric
ring search -- but with a new predicate. The existing search only checks
"is the biome at this exact point the target biome"; natural-island
search additionally needs "is this point plausibly a small, isolated
landmass," which needs its own new isolation check: sample several
points on a ring at `radiusBlocks` around the candidate and require most
of them to be ocean-family biomes.

Once a candidate is found, origin recenters there exactly like
`PREFERRED_NATURAL_BIOME` already does (`markResolved` and friends).
Terrain: within `radiusBlocks` of the found origin, real vanilla
terrain/biome generation is left completely untouched (`effectiveModeAt`
returns `NORMAL`) -- there is no synthetic shape to apply, the real seed
already generated the island. Beyond `radiusBlocks`, the same ocean
gradient as `ARTIFICIAL` mode applies, keyed on plain Euclidean distance
from the origin (not a coastline-distance formula -- there is no
`distanceFromShore` for real, irregular terrain, just a hard radius
cutoff, the same reference-radius pattern `withinExclusionZone` already
uses elsewhere).

**Honest, upfront limitation** (matches TODO 8.2's own "time-boxed; if
the search proves unreliable, park it with findings and move on"
allowance): this is a search-plus-fixed-radius *approximation*, not true
landmass flood-fill/connectivity detection, which isn't tractable for
infinite procedural terrain. It can clip a real coastline unevenly --
part of a real peninsula kept within the radius, or a stray inlet of
un-ocean-ified water left just past it. Accepted rather than chased
further. If the isolation search rarely finds a good candidate within
`SpawnSearchPlan`'s default 2048-block range, the response is tuning
(wider search, relaxed isolation threshold), not a redesign; if still
unreliable after reasonable tuning, park per TODO's own sanctioned
escape hatch rather than open-endedly iterating.

### 25.5 As-built notes (TODO 8.1)

- **`IslandSource` and `IslandPlan.hasLand` shipped exactly as designed in
  §25.1/25.2**, with one addition found necessary during implementation,
  not anticipated in the design pass: `ObjectiveSite.supportiveRadius(...,
  IslandPlan)` (the beatability-radius overload added during Phase 7's
  own follow-up fix) narrows by `island.radiusBlocks()` unconditionally --
  which, for a land-free island, is only ever the harmless
  `MIN_ISLAND_RADIUS_BLOCKS` placeholder (DESIGN §25.2), not a real bound.
  Left as-is, the fallback End-portal guarantee would have wrongly
  shrunk its search to 8 blocks for every chest-boat world. Fixed by
  skipping that narrowing entirely when `!island.hasLand()`. Caught by
  proactively re-reading every consumer of `IslandPlan` before writing
  any implementation code (the exact discipline named as a lesson after
  Phase 7's own beatability bug), not by a test failure or a later bug
  report.
- **Customize-screen scope, decided during implementation, not
  pre-specified in §25.1**: `IslandSource` gets a proper 3-way cycle
  button on the Ocean Island Customize screen (mirroring
  `StripWorldCustomizeScreen`'s `spawnStrategy` button exactly), but the
  starter-chest kit itself (`StarterKitConfig`) is YAML-config-only --
  no in-game field for its item lists. Precedent: every other
  variable-length list in this codebase (`strip.bands.biomes`, etc.) is
  also YAML-only with no dedicated Customize UI, so this isn't a new
  category of limitation; building a dynamic add/remove item-list editor
  widget for one preset's optional starter kit was judged disproportionate
  scope for what GOALS 03 only ever asked to be "configurable."
- **`NATURAL` (GOALS 02, TODO 8.2) is a placeholder in this commit,
  not yet implemented.** The enum value exists (the type needs to be
  stable for the codec/config), and it's selectable in the Customize
  screen and YAML, but `LimitedBiomeSource.resolve()`'s fieldless-preset
  path and `OceanIslandCustomization.islandPlan()` both currently treat
  it identically to `ARTIFICIAL` -- real seed-search behavior lands in
  8.2. `OceanIslandPresetEditor.currentCustomization()`'s read-back
  (reopening Customize on an already-generated world) can't yet
  distinguish `NATURAL` from `ARTIFICIAL` either, since both resolve to
  `IslandPlan.hasLand() == true` and nothing else currently marks which
  one was chosen -- falls back to reporting `ARTIFICIAL`. 8.2 needs to
  either add a persisted marker or find another way to tell them apart
  if that read-back accuracy matters once `NATURAL` actually does
  something different from `ARTIFICIAL`.
- **Chest boat placement**: `EntityTypes.OAK_CHEST_BOAT.create(level,
  EntitySpawnReason.STRUCTURE)` (confirmed via `javap`: 26.2 keeps a
  separate chest-boat `EntityType` per wood species, all implementing
  `ContainerEntity` with a plain `setItem(int, ItemStack)`), spawned at
  the world origin's water surface (`Heightmap.Types
  .MOTION_BLOCKING_NO_LEAVES`, same chunk-forcing fix as the fallback
  End portal). Item ids resolve via `BuiltInRegistries.ITEM.getValue(...)`
  (a `DefaultedRegistry`, so a malformed configured item id quietly
  resolves to air rather than crashing world creation -- consistent with
  how invalid values are handled elsewhere in this codebase rather than
  a new failure mode). `StarterKitDeployment` (new, `worldgen` package)
  is called from `WorldLimitManager.onServerStarted`, gated on
  `island.enabled() && !island.hasLand()`, reusing the exact same
  one-time `WorldLimitState` guard as border/End-portal setup so it
  never re-spawns a duplicate boat on a server restart. Needed its own
  addition to the method's early-return gate (`needsChestBoat`), since a
  chest-boat world with no borders/objective configured would otherwise
  never reach the code that spawns it.

### 25.6 As-built notes (TODO 8.2)

- **Simpler than §25.4 sketched, found during implementation**: the design
  pass assumed a `naturalDelegate`-style biome-passthrough mechanism
  would be needed for GOALS 02's real-terrain biome variety. Re-reading
  `LimitedBiomeSource.getNoiseBiome`'s existing fallthrough chain showed
  this wasn't necessary: `ocean_island`'s `allowed` biome set is already
  the *full* `#minecraft:is_overworld` tag (`OceanIslandPresetEditor
  .apply()` always resolves it that way, restriction comes entirely from
  `islandBiomeAt`'s explicit override, not from a narrowed allowed set),
  and `getNoiseBiome`'s final fallback (`this.resolution.get().delegate()
  .getNoiseBiome(...)`) already samples the real climate for any column
  `islandBiomeAt` doesn't override. So `NATURAL` mode only needed
  `islandBiomeAt` to return `Optional.empty()` within `radiusBlocks`
  instead of a fixed biome id -- the existing "no override, fall
  through" pattern already used for the starter-zone/pass-through checks
  above it in the same method -- and the real biome shows through
  automatically, with zero new biome-sampling machinery.
- **`IslandPlan.syntheticLand`** (new 14th field, alongside `hasLand`)
  captures the distinction cleanly: `hasLand` answers "is there land at
  all" (false only for `CHEST_BOAT`), `syntheticLand` answers "is that
  land artificially shaped" (false only for `NATURAL`). Both default
  `true` for `ARTIFICIAL`/`disabled()`, preserving every already-shipped
  behavior unchanged. Threaded through the same four call sites §25.2
  already identified for `hasLand`, plus `islandOceanDepthAt`'s
  `beyondShore` calculation generalized to `hasLand && syntheticLand`
  (true only for `ARTIFICIAL`) rather than `hasLand` alone, since
  `NATURAL` has no shore-ring width to subtract either.
- **No separate shore ring for `NATURAL`**: setting `shapeAmplitude = 0`
  for the natural-land plan makes `IslandShapeProfile.distanceFromShore`
  degenerate to a perfect circle (`radiusAt` with amplitude 0 always
  returns the base radius), so `distance <= 0.0` is exactly "within
  `radiusBlocks` of origin" with no separate width parameter needed --
  reused directly as the interior/passthrough test in both
  `LimitedBiomeSource.islandBiomeAt` and `EnvelopedChunkGenerator
  .effectiveModeAt` (whose land-mask width becomes `0` instead of
  `shoreWidthBlocks` when `!syntheticLand`).
- **Terrain never raised for natural land**: `islandTargetHeight` no-ops
  whenever `!syntheticLand`, on top of its existing `!hasLand` no-op --
  the real seed already generated real land there (confirmed non-ocean
  by the isolation search before that location was ever chosen), so
  there's nothing to guarantee-raise, and doing so anyway would risk
  visibly altering terrain GOALS 02 explicitly wants left alone.
- **Search**: new pure-logic `NaturalIslandSearch.isIsolatedLand` (ring
  of `RING_SAMPLE_COUNT = 8` samples at the candidate's own `radiusBlocks`,
  `75%` ocean threshold) takes a `BiPredicate<Integer, Integer>` for "is
  this point ocean," keeping the geometry itself fully Minecraft-runtime-
  free and JUnit-testable. `SpawnOriginManager` wires it into a new
  `resolveNaturalIslandOrigin`/`searchNaturalIsland` pair mirroring the
  existing `PREFERRED_NATURAL_BIOME` search's `RandomState`/
  `MultiNoiseBiomeSource` construction exactly, reusing
  `SpawnSearchPlan.defaults().offsetsInSearchOrder()`'s same concentric-
  ring point sequence -- just with the new isolation predicate instead of
  a single-biome match. Dispatched from a new, independent check at the
  top of `resolveFreshOrigin` (`island.enabled() && island.hasLand() &&
  !island.syntheticLand()`), not through the shared `spawnStrategy`
  dispatch, since `ocean_island` always keeps `spawnStrategy` at
  `STARTER_AT_ORIGIN` regardless of `islandSource` (DESIGN §24.8) --
  `NATURAL` needed its own entry point that runs anyway despite that.
  Failure path matches every existing search-failure branch in this
  class exactly: log a warning, fall back to the plain world origin
  (real terrain there is used as-is, whatever it happens to be).
- **Read-back fixed**: `OceanIslandPresetEditor.currentCustomization()`
  (reopening Customize on an already-generated world) now distinguishes
  all three sources correctly via `hasLand`/`syntheticLand`, closing the
  gap §25.5 flagged as a known limitation of the 8.1 commit.
- **Not implemented, deliberately out of scope**: the isolation search's
  reliability against real, varied seeds has not been evaluated beyond
  the pure-logic unit tests for `NaturalIslandSearch.isIsolatedLand`
  itself (which use synthetic predicates, not a real climate sampler) --
  this is exactly the "time-boxed, park if unreliable" territory TODO
  8.2 anticipated. If Jason's acceptance testing finds the search rarely
  succeeds or picks poor candidates on real seeds, the response is
  tuning `ISOLATION_FRACTION`/`RING_SAMPLE_COUNT` or widening
  `SpawnSearchPlan`'s search radius, not a redesign.

## 26. Ocean fluid variants: lava ocean + dry world (GOALS 28, 31) — design pass (TODO 9.1)

### 26.1 Structural decision: a `fluid` axis on the existing Ocean Island preset

GOALS 28's own text ties itself explicitly to "the ocean island challenge
(01/04)," and TODO 9's phase intro frames both 28 and 31 as "the
ocean-island shape with the fluid swapped (lava) or removed (dry)."
**Confirmed with Jason: a new `fluid` field (`water`/`lava`/`none`) on
the existing `ocean_island` preset, independent of Phase 8's
`islandSource` axis** -- any island source can pair with any fluid --
rather than a change to the shared exterior mechanism other presets'
own "ocean" option uses, or a new preset. Matches the precedent set by
`islandSource` itself: one preset, orthogonal axes, instead of
proliferating near-identical presets.

Mechanically, this is a single substitution point:
`EnvelopedChunkGenerator.exteriorState()`'s classification of an ocean
column already funnels through `ExteriorTerrainProfile.oceanLayerAt`'s
four-case enum (`BEDROCK`/`STONE`/`WATER`/`AIR`) regardless of preset;
only the block the `WATER` case maps to needs to vary
(`Blocks.WATER`/`Blocks.LAVA`/`Blocks.AIR`), and only when the column is
island-driven (`island.enabled()`) -- every other preset's own exterior
ocean keeps mapping `WATER` to `Blocks.WATER` unconditionally, zero
behavior change. `ExteriorTerrainProfile.oceanLayerAt` itself needs no
change at all -- it stays a pure "is this position submerged" classifier,
oblivious to which actual fluid fills that classification.

### 26.2 GOALS 28 (lava): what does and doesn't need new code

- **Island shape, shore ring, ocean gradient bands, exclusion zone**: all
  completely unchanged -- GOALS 28 explicitly wants "the island remains a
  normal land biome with a transition shore," which falls out for free
  from touching only the fluid substitution point above.
- **No boats, striders/bridging travel**: already true of vanilla lava
  physics (boats cannot be placed on lava; striders already walk on it
  unaided) -- no new code, just a documentation/manual-testing note.
- **Fire hazards near the shore**: the shore ring (beach/stony-shore, both
  non-flammable) already sits between the island's land and the lava at
  `shoreWidthBlocks` (12 blocks by default) -- comfortably beyond
  vanilla fire spread's effective range from a lava source. Not a new
  mechanism to build; a property to verify holds at the default and to
  spot-check at a narrow custom `shoreWidthBlocks` during acceptance
  testing, not something feasible to verify without booting the game.
- **Nether/End, beatability**: already dimension- and radius-scoped
  independent of fluid (`ObjectiveSite.supportiveRadius`'s island
  overload keys off `radiusBlocks`, never fluid) -- no change needed.
- **Not independently verified by this design pass**: "surface-lava-at-
  scale behavior (light, fire spread, fluid ticking, map color)" per
  TODO 9.1's own phrasing -- these are exactly the kind of in-game-only
  observations no amount of code review can substitute for; flagged
  explicitly for Jason's acceptance pass rather than asserted as safe.

### 26.3 GOALS 31 (dry): scope, and the "harder" difficulty option

The core deliverable -- "oceans generate as drained, empty basins" --
falls out of `fluid: none` exactly like lava does: the `WATER` layer
case maps to `Blocks.AIR` instead, exposing the (already-existing,
always-stone) seabed floor. **Water-scarcity beatability is automatic,
not something to build**: structures (village wells, strongholds,
aquifer pockets, springs) are generated entirely outside this
mechanism's reach (nothing about `fluid` touches structure generation
or aquifer noise), so GOALS 31's explicit "by default, water still
appears where it naturally spawns as part of structures and features"
requirement, and its beatability note ("potions... must remain
obtainable at every offered difficulty"), both hold without any special
casing.

**The "harder settings remove more (e.g. no rivers or surface lakes)"
difficulty option is deferred, not implemented in this phase.**
Investigated during implementation: reporting an ocean/dry biome for a
would-be river column (achievable cheaply, mirroring how `NATURAL`
land's passthrough already works) is not sufficient on its own --
`EnvelopedChunkGenerator.effectiveModeAt` would still classify that
column as `NORMAL` (real, unmasked terrain), and vanilla's own
below-sea-level water fill runs independent of which biome ID
`getNoiseBiome` reports, so the real river's water would still
generate regardless of the biome label. Correctly removing it needs
`effectiveModeAt` (and everything that calls it: `getBaseHeight`,
`getBaseColumn`, `applyEnvelope`, `isEntirelyExterior`,
`hasActiveExterior`) to also mask such columns, which requires real
climate-biome sampling to be threaded through all of them -- a
capability that doesn't currently exist at that layer (`RandomState`/
`Climate.Sampler` reach `EnvelopedChunkGenerator` only where vanilla's
own pipeline already hands one in, not inside `effectiveModeAt`
itself). Shipping a config option that only changes the reported biome
label while the real water physically remains would be actively
misleading, not a smaller version of the feature -- worse than not
having it. Parked with these findings rather than guessed at, matching
this task's own "if [it] proves [more complex than scoped], park it
with findings in DESIGN and move on" allowance (originally written
about 8.2's search reliability, the same spirit applies here). Revisit
as a dedicated task if Jason wants the harder tier badly enough to
justify threading real climate sampling through the exterior mechanism.

### 26.4 As-built notes (TODO 9.2, 9.3)

- **9.2 (lava) and 9.3 (dry) shipped in one commit, not two** -- both are
  the exact same `fluid` substitution point (§26.1), differing only in
  which enum value maps to which block. Splitting them into separate
  commits would have meant an artificial, meaningless partition of one
  three-way `switch` expression. Same reasoning already used for GOALS
  04 shipping alongside 7.2 rather than as its own change.
- **`IslandFluid`** (new enum, `WATER`/`LAVA`/`NONE`) follows the exact
  `parse`/`serializedName` pattern every other Worldz enum uses.
  `EnvelopedChunkGenerator.exteriorState()` gained a `fluid` parameter;
  its `WATER` case now switches on it (`Blocks.WATER`/`LAVA`/`AIR`).
  Both call sites (`applyEnvelope`'s per-column loop, `getBaseColumn`'s
  non-`NORMAL` branch) compute `this.island.enabled() ? this.island
  .fluid() : IslandFluid.WATER` -- the exact same conditional shape
  already used for `depthBlocks` right next to it, so every non-island
  exterior (strip_world's own `OCEAN` option, etc.) is provably
  unaffected.
- **`IslandPlan` codec arity**: confirmed via actual compiler error (not
  assumption) that `RecordCodecBuilder.create`'s `instance.group(...)`
  tops out at `P14`/`Function14` in this DFU version (10.0.21).
  `IslandPlan` was already at exactly 14 fields before Phase 9; adding
  `fluid` required freeing a slot. Chose to nest `exclusionZoneEnabled`/
  `exclusionZoneRadiusBlocks` into a new `IslandPlan.ExclusionZone`
  record (10 external call sites, the fewest of any candidate pairing --
  checked via grep before choosing, not guessed) rather than the
  5-field, 16-call-site ocean-gradient group. Convenience accessor
  methods (`exclusionZoneEnabled()`, `exclusionZoneRadiusBlocks()`) on
  `IslandPlan` itself keep every pre-existing call site
  source-compatible; only the handful of direct `new IslandPlan(...)`
  constructor calls (the record's own factories, plus
  `OceanIslandCustomization`/tests) needed updating.
- **The `removeNaturalRivers` false start**: built completely (new
  `IslandPlan.WaterSettings` nested record, `OceanIslandConfig`/
  `OceanIslandCustomization` fields, a Customize-screen checkbox, and
  `LimitedBiomeSource.islandBiomeAt` river-detection logic sampling
  `resolution.get().delegate()` for `BiomeTags.IS_RIVER`), then fully
  reverted once the terrain-masking gap described in §26.3 was found by
  reasoning through what `effectiveModeAt` would still do with the
  column. Reverting included simplifying `IslandPlan.fluid` back down
  to a flat field (a `WaterSettings` wrapper was only ever needed to fit
  two new fields under the 14-field ceiling; with one field removed,
  nesting only `ExclusionZone` was enough). No dead code or inert
  YAML/Customize-screen fields were left behind from the false start --
  confirmed by grepping for `removeNaturalRivers`/`WaterSettings` across
  the codebase after reverting.
- Full suite green (395 tests, up from 386 -- new `IslandFluid`
  coverage across `IslandPlanTest`, `OceanIslandCustomizationTest`,
  `WorldzConfigTest`); clean build across all modules. No pure-logic
  test exists for `EnvelopedChunkGenerator`'s block substitution itself
  (needs a real Minecraft runtime, per the project's established
  convention) -- validated by code review and the existing
  `depthBlocks`-parallel pattern, not a unit test; real in-game
  verification is Jason's acceptance pass.

## 27. Sky island challenge (GOALS 05–06) — design pass (TODO 10.1)

A small, genuinely floating island at the origin: solid only for a thin,
fixed-thickness band around a configurable surface Y, void everywhere else
within its own footprint and beyond it. Scope for this phase decided with
Jason 2026-07-20 (see TODO Phase 10's header): Overworld + Nether this
phase; the End is a spike-only task (10.5); villages beyond the exclusion
zone (GOALS 07) defer in full to Phase 11.

### 27.1 Why this needs a new mechanism, not a reuse of `IslandPlan`

Today's closest built machinery is `LayoutMode.VOID` (§17/§15.5): choosing
it forces the Overworld exterior to `ExteriorMode.VOID` with a boundary at
the starter radius, but *within* that boundary nothing overrides the
delegate's real vanilla terrain at all — `resolveLayout` explicitly excludes
`VOID` from the terrain-adjustment pass ("its placeholder sample would
otherwise raise the whole world instead of leaving it void"). Combined with
the ordinary starter-land guarantee (which raises a natural ocean floor up
to a target height with a stone foundation reaching down to bedrock), the
practical result is a full-depth column from bedrock to the surface — the
"full-depth terrain plug" TODO 10.1 already flagged as wrong for a true
floating island. This is exactly the deferred "sky-island overlay" §15.5's
own comments pointed at; this phase builds it, as a new dedicated preset
rather than finishing out `LayoutMode.VOID`.

`IslandPlan` (ocean island, DESIGN §24) is not a fit either: its entire
model is a horizontal shore/ocean-gradient classification layered on top of
real, full-depth vanilla terrain (`effectiveModeAt` returns `NORMAL` inside
the island and lets the delegate generate real ground, then only raises it).
A sky island needs the opposite: the interior of its own footprint must
*never* delegate to vanilla terrain, because vanilla carvers/caves/aquifers
running underneath a 6-block slab would routinely punch through it. So:

Decision: **`SkyIslandPlan`, a new, additive, sky-island-only record**
mirroring `IslandPlan`/`StripPlan`'s precedent, threaded through
`EnvelopedChunkGenerator` and `LimitedBiomeSource` the same way. Reuses
`IslandShapeProfile.distanceFromShore` directly for a natural-looking
(non-circular) footprint — that class is already seed/shape-pure with no
dependency on `IslandPlan` itself, so borrowing it costs nothing and keeps
every island-shaped preset's coastline/footprint edge visually consistent.
No ocean/fluid axis, no shore ring, no exclusion zone: GOALS 05 surrounds
the island with void, not a gradient, and GOALS 07's exclusion-zone-driven
villages are deferred to Phase 11 entirely (§ TODO Phase 10 header).

### 27.2 The bounded-below mechanism: classify as VOID everywhere, then carve out the slab

`effectiveModeAt` gains a new top-of-method check, ahead of the existing
`island`/`strip`/`envelope` dispatch:

```java
if (this.skyIsland.enabled()) {
    return ExteriorMode.VOID;
}
```

Sky island columns are *uniformly* `VOID` from `effectiveModeAt`'s point of
view, both inside and outside the footprint — matching how ocean island
reuses a single `OCEAN` classification and varies the seabed depth per
column via `islandOceanDepthAt` rather than inventing a new `ExteriorMode`.
The footprint distinction lives one level down, in the block-filling
functions:

- `getBaseColumn`'s and `applyEnvelope`'s per-Y loops call a new
  `skyIslandStateAt(relativeX, relativeZ, y)` instead of `exteriorState(...)`
  whenever `skyIsland.enabled()`. It computes
  `distance = skyIsland.distanceFromShore(relativeX, relativeZ, seed)` once
  per column; outside the footprint (`distance > 0`) it returns air for
  every `y`. Inside, it returns air above `surfaceY` and below
  `surfaceY - thicknessBlocks`, and a biome-driven fill (§27.4) for the band
  between.
- `getBaseHeight` gains a parallel `skyIslandBaseHeight(relativeX,
  relativeZ, heightAccessor)`: `surfaceY` inside the footprint (so spawn
  search, structure placement, and heightmaps all see the slab's top as
  "the ground"), `heightAccessor.getMinY()` (true void) outside it — the
  same shape `ExteriorTerrainProfile.baseHeight`'s `VOID` case already
  returns unconditionally today, just no longer uniform across the whole
  dimension.

Because `applyEnvelope` runs again, unconditionally, after every generation
stage (carvers, `fillFromNoise`, `buildSurface`, decoration —  the exact
mechanism that already makes `OCEAN`/`VOID` immune to whatever the vanilla
delegate did underneath), the slab needs no carver exclusion of its own:
whatever vanilla carved into the *delegate's* real terrain in that chunk is
irrelevant, because the delegate's terrain for this footprint is never
looked at in the first place — every stage's trailing `applyEnvelope` call
stamps the exact slab shape over it regardless. This also means, unlike
ocean island, sky island needs **no `applyTerrainAdjustments` involvement at
all** — there is no "raise/lower the natural floor" step, because there is
no natural floor being used.

`hasActiveExterior()` gains `|| this.skyIsland.enabled()` alongside the
existing `island`/`strip`/`envelope` checks.

Vanilla decoration/mobs/structures (`applyBiomeDecoration`,
`spawnOriginalMobs`, `createStructures`) stay fully suppressed for a sky
island chunk, unlike ocean island's `decoratesExteriorOcean` carve-out —
there is deliberately no vegetation/mob decoration pass to opt back into
for a Skyblock-style island; the starter kit (§27.7) is the intended
source of "getting started" materials, and empty resource scarcity is a
defining trait of the challenge, not a gap to fill.

### 27.3 Surface material without vanilla's own surface pass

Because the sky island's chunk never runs the delegate's biome-aware
surface builder over its own terrain (§27.2's `applyEnvelope`-after-every-
stage trick intentionally bypasses it, exactly like `OCEAN`/`VOID` always
have), `skyIslandStateAt` needs its own top-block choice instead of relying
on vanilla to paint it later. A new pure `SkyIslandProfile.fillAt(y,
surfaceY, thicknessBlocks, islandBiome)` classifies three layers by depth
from the top (top block, 2 blocks of subsoil, then stone for the rest) and
picks a block family from the configured biome id via a small, deliberately
non-exhaustive set of substring/id checks — desert/badlands/beach-family
biomes get sand-over-sandstone, snowy-family biomes get snow-block-over-
dirt, mushroom fields gets mycelium, everything else gets the plains
default of grass-block-over-dirt. This is a scoped approximation, not a
reimplementation of vanilla's real per-biome `SurfaceRules` — good enough
to make a desert sky island visually read as sand rather than grass,
without chasing every one of vanilla's ~60 overworld biomes. Documented as
an intentional simplification in code, not silently incomplete.

### 27.4 Natural footprint shape

`SkyIslandPlan.distanceFromShore(x, z, seed)` delegates straight to
`IslandShapeProfile.distanceFromShore(x, z, radiusBlocks, shapeAmplitude,
seed)` — the exact function ocean island's coastline already uses, so a
sky island gets the same natural "lumpy, jagged-edged blob" shape (§24.3,
§24.13) for free, at whatever radius/amplitude the player configures.
Unlike ocean island there is no shore ring or ocean-gradient width to
subtract — the footprint's edge simply drops to void.

### 27.5 Beatability: the fallback vault needs no ground, but the guarantee gate does

`ProgressionGuarantees.buildEndPortalSite`/`buildBlazeSite` already build a
**fully enclosed** shell (floor, ceiling, all four walls — the Phase 7
test-2 follow-up fix, DESIGN §24.12) at a fixed Y, independent of whatever
terrain (or lack of it) surrounds that point. A sky island's fallback vault
therefore needs zero new code to actually place correctly, floating in the
void exactly as designed — it was already built to not lean on natural
ground.

What does need attention is the *gate* that decides whether the guarantee
fires at all — and here the 8.1 lesson ("proactively audit every consumer")
turned out to matter after all, in a way only found by tracing the real
call path rather than reasoning from `EnvelopedChunkGenerator` alone.
`WorldLimitManager.onServerStarted` does not read
`EnvelopedChunkGenerator.envelope()` at all: it builds `ensureEndPortal`'s
`envelope` argument from `LimitedBiomeSource.exteriorPlan().overworld()`, a
*separate*, independently-persisted plan that — exactly like the ocean
island (§24.1) — always stays `normal` for a sky island world
(`SkyIslandCustomization.exteriorPlan()` mirrors `OceanIslandCustomization`'s
own "Overworld side always normal" shape). An earlier draft of this section
assumed forcing `EnvelopedChunkGenerator`'s own `envelope` field to `VOID`
(mirroring the `LayoutMode.VOID` `resolveEnvelope` trick from §17/§15.5)
would be enough; it compiles and is harmless (it does make the F3 debug
line read `void` correctly), but grepping for callers of
`EnvelopedChunkGenerator.envelope()` found none in production code —
nothing was actually reading it for beatability purposes, so that alone
would have silently shipped the exact "guarantee never fires" defect
Phase 7.2's own follow-up fix (§24.9) found and fixed for the ocean island.

The real fix mirrors `IslandPlan`'s own shape exactly, just as a sibling
overload rather than a shared one (only one of `island`/`skyIsland` is ever
enabled per world, so combining them into one signature would only add
complexity): a new `ObjectiveSite.supportiveRadius(borderEnabled,
finalBorderRadius, envelope, SkyIslandPlan)` overload narrowing to
`skyIsland.radiusBlocks()` the same way the `IslandPlan` overload narrows to
`island.radiusBlocks()`; `ProgressionGuarantees.ensureEndPortal` gains a
`SkyIslandPlan skyIsland` parameter and picks whichever of the two overloads
applies (`island.enabled() ? ...(..., island) : ...(..., skyIsland)`); and
`WorldLimitManager.onServerStarted` gains an `overworldSkyIsland.enabled()`
arm on `exteriorObjective`'s condition, exactly parallel to
`overworldIsland.enabled()`'s existing one. Found and fixed during 10.2's
own implementation, before any in-game testing — the same "proactive, not
reactive" posture the 8.1 lesson asked for, just requiring one more level of
call-graph tracing to actually apply correctly this time.

One known, deliberately deferred gap carried over unchanged from the
pre-existing `LayoutMode.VOID` precedent (§17's `FALLBACK_PORTAL_TARGET_Y`
doc comment already flags it): the fallback vault is built at a fixed
`Y = -32`, independent of the sky island's own `surfaceY`. For a typical
island (surface 64, thickness 6) the vault lands roughly 90+ blocks straight
down through open void, disconnected from the island itself — reachable
only by building/digging a long way down, not attached to the starter
island the way ocean island's vault (DESIGN §24.12) sits directly beneath
its starter land. Not fixed here: this is the same accepted limitation the
codebase already documents for any void-exterior floating island, and
ocean island's own vault-placement fixes (§24.10–§24.13) were all found and
resolved *after* Jason's in-game testing, not guessed at up front — if this
reads as a real problem once Jason tests a sky island world, the fix shape
(anchor the vault relative to `surfaceY` instead of a fixed absolute Y) is
the same kind of targeted follow-up, not a design change.

`ObjectiveSite.isSupportiveColumn`/`supportiveFallbackZ` already treat
`LayoutMode.VOID` as universally supportive (§17 comment: "bounded by its
own sky-island exterior instead") — a sky island world's `WorldLayoutPlan`
stays `LEGACY` (no coordinated layout runs for it, same as ocean island),
so this path is unaffected and needs no change either.

### 27.6 Nether sky variant (GOALS 06, Nether only — TODO 10.4) — as-built

The Nether has no `LimitedBiomeSource` at all (plain vanilla
`MultiNoiseBiomeSource`), so its half of the sky island can't be sourced the
way the Overworld's is (`originSource`, §27.2). Decision: mirror `StripPlan`'s
exact precedent instead of inventing a new mechanism — a per-dimension-
resolved plan persisted directly on `EnvelopedChunkGenerator`'s own codec,
needing no biome-source involvement at all. `EnvelopedChunkGenerator` gained
a `netherSkyIsland` field (own `SkyIslandCodecs.PLAN_CODEC.optionalFieldOf
("nether_sky_island")` entry, resolved from `config.skyIsland.applyToNether`
when absent, exactly like `strip` resolves from `config.strip`) and a new
`activeSkyIsland()` helper (`dimension == OVERWORLD ? skyIsland :
netherSkyIsland`) that every existing sky-island call site
(`effectiveModeAt`, `hasActiveExterior`, `getBaseHeight`, `getBaseColumn`,
`applyEnvelope`, `skyIslandStateAt`, `addDebugScreenInfo`) now goes through
instead of reading `this.skyIsland` directly — since exactly one of the two
fields is ever enabled per instance (each is only ever populated for its own
dimension), this needed no branching at any of those call sites beyond the
one method swap.

**One real design choice, not just plumbing:** the Nether's block palette
can't reuse `SkyIslandProfile`'s biome-family classification the way the
Overworld's does, because the Nether sky island has no meaningful "biome"
concept at all — GOALS 06 never asks for one, and reusing `SkyIslandPlan`'s
`islandBiome` field there would only ever hold an unused placeholder (the
same harmless-placeholder pattern `IslandPlan`'s `hasLand`-false case already
established). `skyIslandStateAt` branches directly on `this.dimension`
instead: the Nether always gets a fixed netherrack-family palette
(netherrack top/subsoil, basalt core), the Overworld keeps its existing
biome-family logic unchanged.

**The seed plumbing needed a genuinely new mechanism** (correctly
anticipated in the original design pass): `EnvelopedChunkGenerator` gained
a `volatile long netherSkyIslandSeed` field and a public `setSkyIslandSeed
(long)` setter, called unconditionally from the same `ChunkMapMixin`
injection that already resolves `LimitedBiomeSource.setLayoutSeed(long)`
for the Overworld (both loaders) — harmless no-op for every other preset
and for the Overworld's own instance, which still sources its seed from
`originSource` unchanged. `skyIslandSeed()` branches by dimension to pick
whichever source applies.

**Beatability needed the exact same gate fix as the Overworld's own 10.2
finding, discovered by applying the same scrutiny proactively this time**
(DESIGN §27.5's lesson applied without waiting to rediscover it): the
Nether sky island's exterior also never expresses itself through
`ExteriorPlan` (`SkyIslandCustomization.exteriorPlan()`'s Nether side is the
ordinary, independent `netherExterior` toggle, unrelated to
`netherSkyIsland`), so `WorldLimitManager.onServerStarted`'s
`exteriorObjective` gate needed a `netherSkyIsland.enabled()` arm exactly
parallel to the Overworld's `overworldSkyIsland.enabled()` one — this
required fetching the Nether's live `EnvelopedChunkGenerator` *before* that
gate check runs (previously only fetched inside the `nether != null` block
further down, too late to influence the gate), not just at the point
`ensureBlazeAccess` is called. `ObjectiveSite.supportiveRadius`'s existing
`SkyIslandPlan` overload (added in 10.2) already covers the radius
narrowing once `ProgressionGuarantees.ensureBlazeAccess` gained the same
`SkyIslandPlan skyIsland` parameter `ensureEndPortal` already has.

**Customize-screen shape**: one `applyToNether` checkbox (mirrors
`StripWorldCustomizeScreen`'s Nether toggle) reusing the *same*
radius/shapeAmplitude/surfaceY/thicknessBlocks shape as the Overworld —
not independently configurable Nether dimensions, matching GOALS 06's own
framing ("Nether... is a sky island too") and `StripConfig.applyToNether`'s
precedent exactly. `SkyIslandCustomization.netherSkyIslandPlan()` resolves
the Nether's plan from the same fields, disabled entirely unless the
checkbox is set.

### 27.7 End sky island — explicitly out of scope this phase (TODO 10.5)

The End has never been touched by this mod (§14: "leaving the End vanilla").
Wrapping it the same way as the Overworld/Nether is a materially bigger
lift than either: there is no existing `EnvelopedChunkGenerator` wiring for
the End preset slot at all today (not "extend an existing wrapper," but
"build the wrapper's End registration from nothing"), and the End's actual
terrain shape (floating islands already, a central island plus scattered
outer islands, no contiguous "delegate terrain" the way Overworld/Nether
noise generation has) makes "bounded-below slab over void" a different
problem than a square/void cutover on noise terrain — the existing
End-border carry-over (§5.2) only ever added a `WorldBorder` limit, never
touched generation. Per Jason's decision (TODO Phase 10 header), 10.5 is a
throwaway-branch-OK spike: confirm what, if anything, in `EnvelopedChunkGenerator`
generalizes to wrapping the End's generator, and report findings rather
than attempting an implementation now. Findings go here once 10.5 runs.

### 27.8 Chest tiers (GOALS 05, TODO 10.3) — as-built

Phase 8's `StarterKitPlan` (essentials + seed-picked extras, DESIGN §25.3)
is reused, not replaced. Three built-in tiers (`StarterKitTier`: EASY,
MEDIUM, HARD) each get their own `StarterKitConfig` section
(`SkyIslandConfig.easyKit`/`mediumKit`/`hardKit`, mirroring
`OceanIslandConfig.starterKit`'s shape three times over) plus a
`chestTier` selector — persisted on `SkyIslandPlan` itself (so a reloaded
world remembers which tier it was created with, even though the actual
item lists are still read live from config at deployment time, exactly
like `ocean_island`'s own chest-boat kit already does) and exposed as a
Customize-screen cycle button mirroring `islandSource`'s.

**The water-source item's biome mapping is the opposite of a literal
reading of GOALS 05's own example.** The text ("giving the user a bucket
of water vs a cauldron to capture rain water") illustrates a *tier*
axis, not a biome one — re-read closely, the biome-driven swap is a
separate sentence entirely ("Biome... informs what is necessities
chest"). Since every sky island is surrounded by void with vegetation/
decoration fully suppressed (§27.2), the *only* water a desert-family
(no-rain) biome will ever see is whatever's in the chest — it gets a
guaranteed water bucket. Every other family gets a cauldron instead,
since rain will fill it naturally over time and a bucket would be
redundant. Reuses `SkyIslandProfile.familyFor`'s existing DESERT
classification (§27.3) rather than inventing a second biome-family axis
— the item is appended to the resolved kit's essentials
(`StarterKitDeployment.spawnStarterChest`), not stored in config, since
it's fully determined by the already-configured `islandBiome`.

The chest itself is a literal placed `minecraft:chest` block at the
world origin, on top of the slab (`Y = surfaceY`) — mirrors the chest
*boat's* placement-at-origin choice (DESIGN §25.3) exactly, including
that neither accounts for `safeSpawnOffsetBlocks()`'s own spawn-position
offset. Flagged as a testing focus for 10.6, not fixed speculatively:
whether a very small island radius (8–16 blocks) combined with the
default 8-block spawn offset ever separates the player from the chest by
more than a walkable step is exactly the kind of thing this project has
consistently found through real in-game testing rather than by guessing
ahead of time (the Phase 5.5/5.6 border-radius-floor bug is the closest
precedent).

### 27.9 New typed preset shape (`jlt_worldz:sky_island`)

Mirrors `ocean_island`'s scaffolding exactly (DESIGN §24.8): `SkyIslandConfig`
(YAML defaults), `SkyIslandCustomization` (record: tier, islandBiome,
radiusBlocks, shapeAmplitude, surfaceY, thicknessBlocks, applyToNether,
netherRadiusBlocks/shapeAmplitude, border/exterior/end-border settings
reused from `WorldzCustomization` the same way `OceanIslandCustomization`
does), `SkyIslandPresetEditor`, `SkyIslandCustomizeScreen`, world-preset
JSON + `normal` tag entry + lang keys, both loaders' registration — each
verified by the same structural-test pattern every prior typed preset
established (`WorldPresetResourcesTest`, `ProjectMetadataTest`). Like
`ocean_island`, no spawn-strategy field (the island only ever exists at the
origin) and no separate Overworld Exterior field (`SkyIslandPlan`
unconditionally supplies the entire Overworld — and, when enabled, Nether
— exterior itself, the same reasoning as DESIGN §24.8).


