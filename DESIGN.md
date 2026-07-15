# jlt_worldz — Design

World-generation control mod: limit a new world to one or a few chosen biomes,
with an optional forced "starter biome" zone around world spawn. Fabric +
NeoForge multiloader, same toolchain as the sibling mods (MC 26.2, loader
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
- `group media.jlt.minecraft.mods`, `version 0.1.2`, license MIT.
- Description: "Limit new worlds to a chosen set of biomes, with an optional
  starter biome around world spawn."

## 2. Player-facing behavior

1. Player edits `config/jlt_worldz.yaml` (or accepts defaults), then creates a
   new world and picks the **"Worldz"** world type in the world-creation
   screen. The preset's **Customize** screen starts from those defaults and can
   override every setting for that new world without rewriting YAML. Dedicated
   servers use `level-type=jlt_worldz:worldz` and the YAML values directly.
2. The overworld generates with normal terrain shape (rivers, mountains,
   caves) but only the configured biomes — multi-noise snaps every position to
   the nearest allowed biome, so a short list still yields natural-looking
   regions. A single-entry list gives a whole-world single biome.
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
| `allowedBiomes` | `["minecraft:plains"]` | ids and/or `#tags`; order irrelevant |
| `starterBiome` | `""` | empty = feature off; need not be in `allowedBiomes` |
| `starterRadiusBlocks` | `512` | clamp `64..4096` |

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
level. Natural columns already meeting that height remain unchanged. For lower
columns, a smoothstep curve across the configured transition ring blends the
required height back toward the delegate generator's original height. The
result follows natural relief instead of creating a flat platform or a hard
cliff at the starter-zone edge.

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

The resolved starter-land plan is encoded with `LimitedBiomeSource` alongside
the starter biome and radius. A fieldless preset snapshots the current YAML
default, and Customize persists its explicit selection. For backward
compatibility, an already encoded source lacking the new field decodes with the
guarantee disabled, so installing this version cannot reshape unexplored chunks
in an existing Worldz save.
