# jlt_worldz — Implementation Memory

Durable decisions, verified API notes, and rationale that should survive across implementation sessions.

## Known Risks (resolved — see 2026-07-16 replan updates below each entry)

- 2026-07-16 / **`MIXED`/`OCEAN`/`LAND_ONLY` coastlines are exactly straight,
  not just imperfect at grid corners** — Confirmed in-game (Worldz13,
  `config/tests/09-mixed-natural-oceans-and-rivers.yaml`), via screenshots
  showing ruler-straight land/ocean boundaries at multiple locations, plus
  a sharp right-angle "crack" right at spawn (the layout origin default of
  `(0,0)` coincides exactly with a region-grid corner). `WorldLayoutPlan`
  classifies land/ocean/beach per uniform `regionScaleBlocks` grid cell with
  no jitter or noise perturbation of the cell boundary itself — only the
  *height/biome blend* across `coastBlendWidthBlocks` is smoothed, never the
  boundary's shape. This is a bigger deviation from "natural terrain" than
  the already-logged "minor kink very close to a grid corner" (DESIGN §17,
  README) suggested: the entire edge between two cells is straight, not just
  the corner joint where two edges meet. **Status: logged at Jason's
  direction (2026-07-16), not fixed.** A real fix needs an algorithm change
  (perturb the effective cell boundary with its own noise field) — a design
  pass, not a mid-testing patch. Revisit before promising "natural-looking"
  coastlines for `mixed`/`ocean` layouts.
  **Update 2026-07-16 replan:** resolved by removal, not by fixing —
  `MIXED`/`LAND_ONLY` grid composition was deleted in TODO Phase 1.2 (DESIGN
  §20.1); ocean-island shorelines get a proper redesign in the ocean-island
  core phase (TODO Phase 7.1 as of the 2026-07-16 numbering) instead.
  **Done (Phase 1.2, same session):** `LayoutMode.MIXED`/`LAND_ONLY` and all
  boundary/coast-blend/structure-suppression code removed from
  `WorldLayoutPlan`, `EnvelopedChunkGenerator`, config, codecs, Customize UI,
  lang keys, and tests; `config/tests/08`/`09` deleted;
  `config/tests/06` switched to `layout.mode: ocean`. `OCEAN`/`SINGLE_BIOME`/
  `VOID`/`LEGACY` unaffected. 178 tests passing, full multiloader build green.

- 2026-07-16 / **`layout.biomes` picks one biome per whole region cell, so
  linear vanilla biomes like `river` render as a huge flat area, not a
  channel** — Confirmed in-game (Worldz14, config `09` before its fix): an
  isolated `MIXED` land cell surrounded by ocean generated as an ordinary
  round island (correct terrain) but labeled "River" over the whole thing,
  no channel visible. Not a bug — `mixed`/`land_only`/`ocean` all pick a
  single weighted biome per `regionScaleBlocks` cell (up to 512x512 blocks),
  which is the right grain for broad biomes but meaningless for one vanilla
  only ever generates as a narrow, noise-carved channel. Natural rivers/
  ponds already appear inside land cells unaided (`LayoutTerrainProfile.
  targetHeight` only raises columns that are too low, never flattens a
  natural dip), so adding `river` to `layout.biomes` has no upside. **Status:
  documented (DESIGN §17, README, `config/tests/09`'s header), not a code
  change** — proportional/linear-feature support in coordinated layouts
  would be a real feature addition, not attempted. If a future config or
  Customize-screen input includes `river` (or another inherently linear/thin
  biome) in `layout.biomes` again, this is why it looks wrong, not a
  regression.
  **Update 2026-07-16 replan:** moot after Phase 1.2 removes the grid modes;
  natural rivers/oceans in single-biome worlds are instead delivered by
  vanilla pass-through selection (DESIGN §20.5), which needs no biome-cell
  concept at all. **Done (Phase 1.2, same session):** the grid modes that
  caused this are gone; Phase 3 implements the vanilla pass-through
  replacement.

## Decisions

- 2026-07-14 — Use `../reseed` as an exact structural/build template, while keeping Worldz gameplay code loader-neutral in `common`. This matches DESIGN.md and minimizes divergence among the sibling mods.
- 2026-07-14 — Keep the existing Fabric gametest run configuration but omit a `fabric-gametest` entrypoint until tests are intentionally added. This is the explicit Phase 0 requirement.
- 2026-07-14 — Do not push commits. The user requested local commits per iteration, and TODO.md reserves publishing decisions for Jason.
- 2026-07-14 — Canonicalize valid config ids to explicit namespaces and deduplicate them in first-seen order. This makes persisted settings stable while preserving the design's order-independent semantics.
- 2026-07-14 — Rewrite successfully parsed config atomically after sanitation, but never rewrite malformed or wrongly typed input. Non-string entries inside `allowedBiomes` are the specified exception: they are warned, dropped, and the otherwise valid file is rewritten.
- 2026-07-14 — In the biome-source codec, `starter_radius` is always encoded and doubles as a resolved-state marker. On decode, an absent starter plus present radius means explicitly disabled; only the fieldless preset consults config for the starter. This preserves config independence for existing worlds without adding a schema field.
- 2026-07-14 — If no allowed biome has an overworld climate entry, delegate to the full overworld parameter list and expose those fallback biomes through `possibleBiomes()`. Reporting an empty possible set would contradict runtime generation and can break structure/feature decisions.
- 2026-07-14 — Build overworld climate parameters with `new MultiNoiseBiomeSourceParameterList(Preset.OVERWORLD, biomeGetter)` during codec decode. Retrieving the parameter-list registry holder fails in 26.2 because world presets are decoded while that holder is still unbound by parallel registry loading.
- 2026-07-14 — Expand configured biome tags lazily on first biome-source use.
  Minecraft 26.2 decodes world presets before dynamic-registry tags are bound;
  eager `HolderSet.Named.stream()` access prevents datapacks from loading. The
  memoized resolution is encoded as a direct holder list in world-gen settings.
- 2026-07-14 — Extract climate-entry selection into the pure generic
  `AllowedEntryFilter`. This keeps every climate point for an allowed biome,
  preserves vanilla entry order, separately tracks distinct matched biomes,
  and makes the core filtering behavior directly testable without booting
  Minecraft registries.
- 2026-07-14 — Use MIT as specified by DESIGN.md and loader metadata. The
  reseed template unexpectedly supplied CC0; the completion audit replaced it
  with the SPDX MIT text attributed to Jason Taylor and added metadata tests.
- 2026-07-14 — For non-fallback generation, expose only climate-matched allowed
  holders through `possibleBiomes()`. Unsupported configured biomes are warned
  and ignored, so advertising them to feature/structure logic would contradict
  what the delegate can generate.
- 2026-07-14 — Switch the canonical config to `jlt_worldz.yaml` at the user's
  request. Use SnakeYAML 2.6's `SafeConstructor` and manually validate a plain
  map/list/scalar tree rather than constructing Java beans. If YAML is absent,
  a valid legacy JSON config is parsed as YAML-compatible input, written to
  YAML, and retained as `.json.bak`; malformed input remains untouched.
- 2026-07-14 — Limited-world borders use Minecraft's vanilla square border,
  centered at `(0,0)`, with config values expressed as center-to-side radii.
  Overworld and Nether schedules are independent and disabled by default.
  Initial/final radii plus `resizeDays` express static, growing, and shrinking
  borders. Optional progression objectives need to be reachable at the final
  size (up to the requested reference of 100 in-game days), not on day zero.
- 2026-07-14 — Persist `WorldLimitPlan` inside `LimitedBiomeSource`, not in the
  global config or only in border saved data. The fieldless preset snapshots
  current config; encoded sources missing the new field are recognized by the
  already-present `starter_radius` resolved marker and decode to disabled
  limits. A separate saved-data marker records that native border interpolation
  has been started so reopening a world never resets its schedule.
- 2026-07-14 — Progression guarantees prefer vanilla structures with a
  128-block safety margin inside the final border. If they do not fit, place a
  deterministic compact site near `(32,0)`: a zero-eye surface End portal or an
  enclosed Nether blaze-spawner room. This preserves strict small borders and
  avoids global structure-set changes or brittle worldgen mixins. Eyes of Ender
  do not target the fallback, so it is deliberately visible near the origin and
  its exact coordinate is logged.
- 2026-07-14 — Release 0.1.1 uses the version already selected in the user's
  working tree. A project-metadata test deliberately pins the release version,
  so every future bump must update that contract in the same iteration.
- 2026-07-14 — The Worldz Customize screen uses YAML only as its initial state;
  Done creates an explicit `LimitedBiomeSource` without writing global config.
  This keeps dedicated servers config-driven while letting every singleplayer
  world choose any biome/tag and all existing starter/border options. Reopening
  Customize reads the explicit source so already-applied choices are retained.
- 2026-07-14 — Register the preset editor with NeoForge's client-only
  `RegisterPresetEditorsEvent`. Fabric 26.2 has no equivalent event, so its
  client-only mixin intercepts `WorldCreationUiState.getPresetEditor()` only for
  the `jlt_worldz:worldz` key; no server class references the GUI.
- 2026-07-15 — Exterior terrain is an independent square envelope, baked into
  new-world generators. Overworld supports normal/ocean/void, Nether supports
  normal/void, and normal remains the compatibility default. Boundary `0`
  derives from the largest scheduled border radius; without an enabled border,
  a non-normal exterior requires an explicit boundary. Ocean starts inward by
  a configurable accessible transition width and remains ocean infinitely.
- 2026-07-15 — Border rate fields express continuous X radius blocks per Y
  Minecraft days. A complete positive rate pair takes precedence over legacy
  total `resizeDays` and is converted to one vanilla linear transition, keeping
  restart persistence and old encoded schedules without adding a tick manager.
- 2026-07-15 — Exterior and rate values use optional codec/YAML fields with
  compatibility defaults. A persisted older biome source resolves to normal
  exterior terrain; a fieldless new-world preset snapshots sanitized YAML.
  Rate timing rounds a partial final interval up to a whole game tick and uses
  overflow-checked long arithmetic.
- 2026-07-15 — `jlt_worldz:enveloped` persists a vanilla-compatible delegate,
  dimension identity, and resolved envelope. It changes blocks only outside the
  square, reapplies the profile after noise/surface/carvers/features, skips
  structures and decoration for wholly exterior chunks, removes displaced block
  entities, and delegates the End unchanged. Ocean uses deep-ocean biomes, a
  solid stone floor 16 blocks below sea level, bottom bedrock, water, and air.
- 2026-07-15 — Customize retains independent exterior settings for both
  dimensions and exposes rate blocks/days beside legacy total days. Nether mode
  cycling intentionally offers only normal/void. Objective searches and compact
  fallbacks use the minimum final-border and solid-terrain radius; therefore an
  explicit exterior without a border still honors the default guarantee flag.
- 2026-07-15 — Border resize delay is expressed as `resizeDelayDays` and uses
  24,000 server game ticks per day. The initial radius remains static until a
  persisted per-dimension start tick is due; offline real time does not count.
  Delay `0` preserves existing behavior, while a zero-duration resize with a
  positive delay jumps to its final radius only after the wait.
- 2026-07-15 — Release 0.1.2 packages the exterior terrain envelopes,
  rate-based resizing, world-creation controls, supportive progression bounds,
  and persisted initial resize delays completed after 0.1.1.
- 2026-07-15 — Version 0.1.3 identifies the guaranteed starter-land feature;
  0.1.2 had already been built and documented before this terrain work began.
- 2026-07-15 — Worldz4 screenshots and saved settings confirmed a design
  defect rather than a codec/config failure. With radius 128 and transition
  128, the original profile forced every submerged core column toward the same
  first-free height and interpolated that absolute height outward, visibly
  producing a level shelf, underwater terraces, and exaggerated village
  foundations. Exterior and borders were both disabled.
- 2026-07-15 — Starter-land profile revision 2 treats sea level + 2 as a
  baseline, adds up to eight blocks each of compressed natural ocean-floor
  relief and broad seeded `minecraft:surface_secondary` noise, and blends the
  required lift with nearest-block rounding. Revision 1 retains its original
  ceiling behavior. The optional persisted `profile_version` defaults to 1 for
  old encoded worlds; config and Customize create revision 2 plans.
- 2026-07-15 — The reported floating starter island was not an active Worldz
  ocean envelope: the saved Worldz3 settings encoded normal Overworld/Nether
  exteriors. A starter biome controls biome selection, but vanilla noise,
  aquifers, and carvers can still produce a shallow island over deep water.
- 2026-07-15 — Guaranteed starter land will be an optional Overworld-only
  persisted plan. It raises only columns below sea level + 2, fills upward from
  the delegate's natural ocean floor, retains the delegate's surface rules, and
  uses a smooth circular transition back to untouched natural heights. The same
  pure profile drives base height/column queries. Encoded older sources missing
  the plan decode disabled; fieldless new presets use YAML defaults.
- 2026-07-15 — Worldz5 and Worldz6 establish that filtering vanilla multi-noise
  biome entries is not a terrain-composition system. Vanilla continentalness
  can remain ocean for thousands of blocks while Worldz reports allowed land,
  beach, or ocean biomes over it; structures may then create isolated adapted
  platforms. The fix is a versioned, seed-aware layout sampled by both the
  biome source and terrain wrapper, not a larger starter disc or a different
  order of the current independent operations.
- 2026-07-15 — Planned layout modes are land-only (rivers/small water allowed),
  mixed (recommended coverage plus configurable biome weights), ocean (starter
  island and beach transition into multiple possible ocean biomes), single
  biome, and sky void (starter island). Existing encoded worlds remain on an
  explicit legacy vanilla-terrain mode. Beach and ocean selections have terrain
  roles; the starter plan overlays and blends into the shared base layout.
- 2026-07-15 — Treat spawn preference, forced starter biome, and layout origin
  as separate concepts. A future preferred-natural-biome strategy may search
  the finalized seed's unmodified climate view, but it may become a Worldz
  layout origin only if resolved before chunk generation and persisted. Moving
  spawn alone while leaving starter, border, exterior, and progression math at
  `(0,0)` is rejected.
- 2026-07-15 — Defer a customizable flat mode until coordinated noise layouts
  stabilize. Verified 26.2 `FlatLevelGeneratorSettings` already supports an
  arbitrary fixed biome, ordered block layers, feature/lake flags, and optional
  structure-set overrides. The default settings select villages + strongholds;
  the Overworld preset adds mineshafts, outposts, and ruined portals, while
  trial chambers are absent. Worldz should expose these clearly, add a preset
  whose spawn surface is at Y 40 or higher to avoid the ordinary slime-chunk
  rule, and test structure placement rather than assuming `FlatLevelSource`
  cannot support it. Biome-specific surface slime spawning remains separate.
- 2026-07-15 — Sample `WorldLayoutPlan` with an independent 64-bit hash per
  `(seed, salt, cellX, cellZ, ...)` rather than gradient/Perlin noise or a
  Minecraft `DensityFunction`. This keeps the sampler pure and JUnit-testable
  like `BiomeListSpec`/`ExteriorPlan`, and per-cell independence means
  measured ocean coverage converges to the target fraction with no
  calibration curve. Select a biome within a role by `hash ** (1/weight)`
  argmax (Efraimidis-Spirakis weighted sampling), not `hash * weight` — the
  latter was fixture-tested and found to starve low-weight biomes.
- 2026-07-15 — Coast blending in `WorldLayoutPlan` picks only the single
  nearest role-differing cell boundary (checking up to 4 axis-aligned
  candidates) rather than a fully radial 2D blend. This keeps the formula a
  pure function of signed distance to one fixed line (genuinely continuous
  crossing that boundary) instead of needing to reconcile two simultaneously
  active boundaries near grid corners. Same simplification class as
  `ExteriorPlan`'s Chebyshev `max(abs(x),abs(z))` square distance; a perfectly
  radial blend can be revisited during 15.4 playtesting if corners look wrong.
- 2026-07-15 — `WorldzConfig.sanitizeLayout` falls back the whole `layout.mode`
  to `legacy` (with a warning) when the sanitized biome list has zero usable
  entries for a role the mode requires, rather than letting `WorldLayoutPlan`'s
  constructor throw at world-creation decode time. Mirrors the existing
  empty-allowed-list fail-safe: a broken layout config must never prevent
  world creation.
- 2026-07-15 — `EnvelopedChunkGenerator` coordinates layout terrain by a
  uniform vertical shift (raise/lower delta applied identically to every
  `Heightmap.Types` query and to the actual placed blocks) rather than
  replacing terrain outright. This preserves vanilla local relief shape (hills
  keep looking like hills, just moved) and automatically keeps `getBaseHeight`
  agreeing with `getBaseColumn` and the real chunk, since both derive the same
  delta from the same ocean-floor baseline. Layout and starter-land raises are
  applied as fully independent sequential passes (not merged into one
  max-of-both calculation) because starter land's raise already treats
  water/air as replaceable foundation — so starter land correctly wins in its
  own zone even against a layout that lowered it first, without either pass
  needing to know about the other's target.
- 2026-07-15 — `VOID` layout mode is excluded from `EnvelopedChunkGenerator`'s
  terrain adjustment entirely (`resolveLayout` treats it like `LEGACY`).
  `WorldLayoutPlan.sampleAt` returns a hardcoded `landFactor=1.0` placeholder
  for `VOID` (its real sky-island overlay is Phase 15.5), and applying that
  literally would raise the whole world into land instead of leaving it void.
- 2026-07-15 — The fieldless preset's `WorldLayoutPlan` sampling seed is
  chosen with `new Random().nextLong()` once per newly created world rather
  than a fixed constant. `BiomeSource` codecs decode from `RegistryOps`, which
  has no seed-aware hook, so the actual Minecraft world seed isn't available
  at decode time; a random-per-world seed at least gives distinct worlds
  distinct layouts (matching player expectations) even though entering the
  same seed string twice won't yet reproduce the same layout. Revisit once
  Phase 16's finalized-seed-timing spike identifies where the real seed
  becomes available.
- 2026-07-15 — `WorldzCustomization.LayoutSettings` validates strictly (throws
  on a malformed weighted-biome entry, tag, or role-override, same as the
  outer record's `allowedBiomes`/`starterBiome`) rather than leniently dropping
  bad entries the way `WorldzConfig.sanitizeLayout` does for YAML. Direct
  Customize-screen input should surface a mistake immediately in the error
  message widget; a background YAML file must never block world creation over
  a typo, so it stays lenient there instead.
- 2026-07-15 — `VOID` layout mode implements its sky-island by forcing
  `EnvelopedChunkGenerator`'s existing exterior-envelope mechanism to
  `ExteriorMode.VOID` at the starter radius, rather than building a separate
  "void everywhere except an island" system. The exterior mechanism already
  does exactly that (solid inside a boundary, void outside), so this reuses
  well-tested code instead of duplicating it — same instinct as reusing
  `ExteriorPlan`'s Chebyshev-distance envelope for other DESIGN §17 pieces.
- 2026-07-15 — The starter-land transition (`StarterLandProfile.targetHeight`)
  now blends back toward the layout-adjusted floor instead of raw vanilla
  terrain when a layout is active, by swapping only the `naturalHeight`
  argument fed into it (not the value used for `foundationMinY`/column-editing
  overwrite checks, which must stay tied to the true natural floor). This was
  a one-parameter fix because at full starter strength the formula already
  collapses to `shapedMinimum` regardless of that argument, and at zero
  strength it returns that argument unchanged — exactly the "blend toward X"
  behavior needed, just pointed at a different X.
- 2026-07-15 — The compact End-portal fallback site tries a small fixed set of
  nearby Z offsets (`{64, -64, 128, -128}`) when its default column isn't
  layout-supportive, rather than a general search or relocating along X too.
  Kept deliberately small and deterministic (same philosophy as the existing
  "near positive X" fallback): enough to usually dodge one bad grid cell
  without turning objective placement into an open-ended search that could
  behave unpredictably near a small border.
- 2026-07-15 — `SpawnSearchPlan`'s search order is concentric rings (origin,
  then every `stepBlocks` out to `maxRadiusBlocks`, `pointsPerRing` points per
  ring by even angle) rather than a spiral or random-offset walk, and defaults
  to `2048`/`32`/`8` specifically to mirror vanilla's own `Climate.SpawnFinder`
  radius (confirmed during the 16.1 spike) — reusing a known-reasonable
  vanilla bound rather than inventing a new one. The actual biome test at each
  offset is deliberately left to the (impure, MC-dependent) caller in Phase
  16.3; this class only generates the deterministic candidate order.
- 2026-07-15 — `LimitedBiomeSource`'s layout origin is a mutable, non-codec
  `volatile int` pair (`setOrigin`), not a `WorldLayoutPlan` record field.
  The origin is resolved after codec decode (needs the real seed and a live
  `ServerLevel`), so it can't be a persisted record component the way every
  other Phase-15/16 setting is; `SpawnOriginState` (a `SavedData`) is the
  actual persistence, re-applied to this mutable field on every load.
- 2026-07-15 — `SpawnOriginManager` exposes two separate entry points
  (`reapplyPersistedOrigin`, `resolveFreshOrigin`) rather than one method that
  branches on `initialized()`. A single conflated method was drafted first and
  caught as a bug during design, not testing: registering it at both
  `LevelEvent.Load` and `LevelEvent.CreateSpawnPosition` for the same fresh
  world would let the earlier `Load` call perform the search and mark it
  resolved, so the later `CreateSpawnPosition` call -- the one that actually
  needs the found position to override vanilla's spawn -- would see
  `initialized() == true` and silently no-op. Keeping the two responsibilities
  in clearly separate methods makes that failure mode structurally impossible
  rather than relying on call-order discipline.
- 2026-07-15 — Recentering uses a coordinate-shift-at-integration-boundary
  pattern: `StarterZone`, `ExteriorPlan.DimensionEnvelope`, `WorldLayoutPlan`,
  and `ObjectiveSite` stay origin-agnostic (always relative to an implicit
  `(0,0)`); only `LimitedBiomeSource`, `EnvelopedChunkGenerator`,
  `WorldLimitManager`, and `ProgressionGuarantees` subtract the resolved
  origin from query coordinates before calling into that pure logic. This
  avoids threading an origin parameter through every pure-logic method
  signature and keeps those classes' existing unit tests origin-agnostic.
  Nether intentionally stays centered at `(0,0)` -- DESIGN §18's strategies
  are Overworld-only in scope, so the Nether border/progression objective are
  unaffected by any Overworld origin move.
- 2026-07-16 — Fixed the floating-village bug (see Known Risks/TODO Q.2) by
  suppressing structure starts near a `MIXED` layout's coast-blend
  transition, rather than trying to make vanilla's `JigsawPlacement`
  re-adapt each piece to varying terrain (not realistically patchable from
  a `ChunkGenerator` subclass; it samples one anchor height and never
  re-queries per piece). `WorldLayoutPlan.isNearRoleBoundary` deliberately
  reuses `nearestDifferingBoundary` -- the exact same function that drives
  the actual height blend -- rather than an independent distance check, so
  the suppression boundary can never disagree with where the height cliff
  actually is. Suppressing the whole chunk's structure starts (checked at
  all 4 corners, mirroring `isEntirelyExterior`) is coarser than only
  suppressing the exact structure that would've been unstable, but doing
  better would need knowing a structure's footprint before it's placed --
  not available at the `createStructures` suppression point.
- 2026-07-16 — The anchor-chunk-only version of the above fix was
  insufficient: confirmed in-game (Worldz14) still stranding villages and an
  ocean monument. Before assuming the fix logic was wrong, verified the
  *installed jar* actually contained the compiled fix (`javap` on the classes
  inside the mods-folder jar) -- it did, so the gap was in the check's reach,
  not a deployment issue. Root cause: the check only looked at the anchor
  chunk's own corners (16 blocks wide), but a structure's actual pieces can
  land up to vanilla's `JigsawStructure.MAX_TOTAL_STRUCTURE_RANGE` (128
  blocks) away -- comparable to `coastBlendWidthBlocks` itself. Fixed by also
  checking corners expanded by a 128-block margin, added to (not replacing)
  the original corners: expanding outward alone moves the checked points
  further from a boundary sitting near/inside the anchor chunk itself,
  reopening exactly the case the original check covered. Ocean monuments use
  a different, non-jigsaw `Structure` subclass
  (`Structure.onTopOfChunkCenter`) but hit the identical single-anchor-height
  pattern, so the same generic per-chunk suppression covers both without
  structure-type-specific logic.
- 2026-07-16 — `mixed`'s `BEACH` role spans the entire coast-blend
  transition width (`nearest != null`, true for the whole
  `2 * coastBlendWidthBlocks` zone), not a narrow shoreline strip --
  confirmed via screenshot showing beach extending 100+ blocks into both
  land and water. `coastBlendWidthBlocks` was designed as a height/biome
  *smoothing* width, but is also being used, unintentionally, as the beach
  role's own footprint. Logged, not fixed: a real fix needs a separate,
  narrower width concept for where `BEACH` applies, which is a new tunable
  (config field, docs, tests) rather than a one-line patch.
- 2026-07-16 — **Resolved the dummy-RandomState terrain risk** (flagged
  Phase 16.1, 2026-07-15; confirmed via Jason's own spectator-mode
  observations on world "Worldz14": almost entirely lava below Y-64 with no
  bedrock, and caves mostly absent -- occasional geode, huge gaps instead of
  winding paths -- instead of vanilla's usual systems). `ChunkMap`'s
  constructor only builds a real `RandomState` from a dimension's actual
  noise settings when `generator instanceof NoiseBasedChunkGenerator`;
  otherwise it silently falls back to `RandomState.create(NoiseGeneratorSettings.dummy(), ...)`
  -- a zero-density router and air surface rule. `EnvelopedChunkGenerator`
  wraps a `NoiseBasedChunkGenerator` delegate by composition, never
  satisfying that check, since Phase 3.
  **The "known fix pattern" recorded when this was first flagged (have
  `EnvelopedChunkGenerator` extend `NoiseBasedChunkGenerator` instead of
  wrapping one) turned out to be wrong for 26.2 and was never actually
  viable.** `javap` against the real compiled game jar
  (`minecraft-merged-deobf-26.2.jar`) shows `NoiseBasedChunkGenerator` is
  declared `final` -- Java cannot subclass a final class, period. The
  decompiled source used throughout this whole investigation (from
  `mergeWithSources_...jar`, this project's usual research source per its own
  "ground rules") misleadingly showed `public class` with no `final`
  modifier -- a real discrepancy between that decompiled source and the
  actual bytecode, not a modding-convention change between MC versions as
  first assumed. An attempted extends-refactor hit the compiler error
  immediately (`git checkout` reverted it cleanly before anything else was
  touched) once this was discovered. **Lesson: for a question as consequential
  as "can I extend this vanilla class," verify against `javap` on the actual
  classpath jar, not just decompiled source text** -- decompilers can drop or
  misrender modifiers that the real bytecode enforces exactly.
  Fixed instead with a mixin into `ChunkMap`'s constructor (`ChunkMapMixin`,
  both loaders), injected via `@Inject` right before the
  `generator.createState(...)` call (by which point vanilla has already
  assigned `this.randomState` to either the real or dummy value): if the
  generator is `instanceof EnvelopedChunkGenerator` wrapping a
  `NoiseBasedChunkGenerator` delegate, overwrites `this.randomState` with one
  built from the delegate's actual settings. Needed `@Shadow @Mutable` since
  vanilla declares that field `final`. NeoForge had never had its own mixin
  support configured in this project before now -- added via a `[[mixins]]`
  entry in `neoforge.mods.toml` (schema confirmed via FML loader's own
  `ModFileParser.getMixinConfigs`) plus a new
  `jlt_worldz.neoforge.mixins.json`, mirroring Fabric's existing config
  shape exactly (both loaders bundle the same upstream SpongePowered Mixin).
  The mixin's own Java source is identical on both loaders -- pure
  Mojang-mapped vanilla code, no loader-specific APIs -- but kept as two
  separate per-loader files rather than shared via `common`, matching this
  project's existing per-loader mixin convention (`MinecraftServerMixin` is
  also Fabric-only). Not yet confirmed fixed in-game.

- 2026-07-16 — **GOALS.md replan (planning session with Jason, Fable).**
  `GOALS.md` (24 challenge-world use cases) replaces the feature-first plan as
  the requirements source; `TODO.md` was rewritten into 12 challenge-first
  phases (old plan archived as `TODO-archive.md`); DESIGN §20 records the
  architecture. Settled decisions, confirmed by Jason explicitly:
  (a) **remove** the `MIXED`/`LAND_ONLY` hash-grid layout system rather than
  fix or park it — no GOALS use case needs region-composed worlds, and it
  caused the straight-coastline/beach-width/floating-structure defect class;
  (b) **adjust, don't restart** — keep the pure-logic core and codec/mixin
  plumbing; (c) first implementation target after stabilization is the
  **single-biome challenge (use cases 10–12)**, the closest to already
  working; (d) **new worlds only** — the mod targets newly created worlds, no
  cross-version save compatibility, restriction documented in README, legacy
  shims (JSON config migration, old-save decode paths) may be dropped;
  (e) **client-first** — Fabric singleplayer is the acceptance path, dedicated
  server stays only as the free config-driven path (worldgen runs on the
  logical server, so a client install covers every use case; Open-to-LAN
  covers multiplayer), never a per-phase test gate.
- 2026-07-16 — One preset per challenge family (`single_biome`,
  `ocean_island`, `sky_island`, `sky_chunk`, `flat`, `limited`) replaces the
  single `jlt_worldz:worldz` preset and its overloaded Customize screen, per
  GOALS' own suggestion of multiple world types for mutually exclusive
  processes. Shared modules (limits, spawn, starter land, progression, plus
  new exclusion-zone and starter-chest modules) compose into each type.
  Each new type/module phase starts with a committed DESIGN §20 design task
  before implementation (the §16–18 pattern that worked).
- 2026-07-16 — Replace the random-per-world layout sampling seed with the
  real world seed captured at generation time (available at `ChunkMap`
  construction, which `ChunkMapMixin` already intercepts, or from the server
  level — exact hook to verify in Phase 1.3). GOALS requires seed-reproducible
  randomness in use cases 08–10, 12, 16; the codec-decode-has-no-seed problem
  only forbids capturing it at decode time, not lazily at generation time.
  **Done (Phase 1.3, same session):** `ChunkMapMixin`'s existing `<init>`
  injection already had `level.getSeed()` in hand for the 0.1.15
  dummy-RandomState fix — it now also calls the new
  `LimitedBiomeSource.setLayoutSeed(level.getSeed())`. No persistence needed:
  unlike the spawn-origin search (§18), `ServerLevel.getSeed()` is already
  vanilla-deterministic across every load, so the mixin just calls it every
  time. `WorldLayoutPlan` gained a pure `withSeed(long)` wither;
  `LimitedBiomeSource` now holds both the exact persisted plan
  (`worldLayoutPlan()`, unchanged, still what the codec/Customize screen
  round-trip) and a mutable `effectiveLayoutPlan` that `setLayoutSeed`
  updates and `getNoiseBiome` actually samples from.
  `EnvelopedChunkGenerator.LayoutContext` was changed from snapshotting a
  `WorldLayoutPlan` at generator-construction time (necessarily before
  `setLayoutSeed` can have run) to holding the `LimitedBiomeSource` and
  reading `effectiveLayoutPlan()` live — same
  coordinate-shift-at-integration-boundary shape as origin recentering, for
  a plan reference instead of a coordinate delta. See DESIGN §20.4
  Implementation.
- 2026-07-16 — Per-world settings snapshot: world creation writes a commented
  YAML record of resolved settings into the world folder — a reference
  artifact, not a control file; baked codec settings remain authoritative.
  Global config hygiene: never rewrite `config/jlt_worldz.yaml` when absent or
  all-defaults, document via generated comment-based YAML (our own emitter;
  SnakeYAML doesn't round-trip comments), drop JSON migration.
  **Done (Phase 1.4, same session — global-config half only; the per-world
  snapshot is still Phase 2.4):** `WorldzConfig.load()` returns in-memory
  defaults and writes nothing when the file is absent; an existing file's
  rewrite-after-sanitize behavior is unchanged. `loadLegacy` and all
  `jlt_worldz.json`/`.json.bak` handling deleted outright (new-worlds-only
  makes it dead weight). The `_docs` in-file map is gone from the schema;
  `config/jlt_worldz.example.yaml` is now hand-authored with real `#`
  comments instead of being generated output the mod itself produces —
  building a comment-preserving emitter for a file that changes rarely was
  judged more machinery than the problem warranted. A JUnit test parses the
  example file and compares its *values* against `new WorldzConfig()`'s
  defaults to keep it honest without needing byte-for-byte text comparison.
- 2026-07-16 — **Done (Phase 1.5).** Jason created a dedicated Prism Launcher
  instance for manual testing (`Worldz-Test`, includes Xaero's world/minimap,
  Chunky, Spark; MiniHUD disabled since `jlt_info` overlaps it) and provided
  its path. Added `:fabric:deployToPrism` (a plain `doLast` task, not a
  `Copy` task — this is a one-off local dev convenience, not something that
  needs incremental-build participation): copies the built Fabric jar into
  the instance's `mods/` folder, deleting any older `jlt_worldz-fabric-*`
  jar first. Reads the instance path from `prism.instance.dir` in a
  git-ignored `local.properties` at the repo root (created with Jason's
  path), or accepts `-PprismInstanceDir=<path>` ad hoc. Discovered this Loom
  setup (1.17.14) has no separate `remapJar` task — plain `jar` already
  produces the fully remapped output (confirmed: it's the same file already
  landing in `build/libs/` from `./gradlew build`) — so the task depends on
  `jar`, not `remapJar`. Also added `config/tests/08-single-biome-regression.yaml`
  and `09-void-regression.yaml`, the only two surviving layout modes with no
  existing test-config coverage, plus a new "Phase 1 acceptance" section in
  `MANUAL_TESTING.md` covering TODO 1.1 (RandomState fix verification) and
  1.7 (removed-mode regression) concretely.
- 2026-07-16 — Jason approved five additional challenges in the same planning
  session: cave-only start with mega-cave option (GOALS 25–26), Nether start
  (27), lava ocean (28), rising lava floor (29), and forever night (30). The
  plan grew from 12 to 16 phases (lava ocean lands right after the ocean
  phases to reuse fresh infrastructure; cave and Nether start after the sky
  phases; 29–30 form a shared **world-hazard rules** runtime module — DESIGN
  §20.9 — built on the same tick+saved-data mechanism as delayed borders,
  composable with any world type and movable earlier as a quick win). Four
  further candidate ideas (dry world, strip world, chaos biomes, End start)
  were listed in GOALS pending Jason's explicit approval.
- 2026-07-16 — Jason then approved all four candidate ideas with refinements,
  and added a fifth of his own: dry world (GOALS 31 — water findable in
  villages/strongholds/natural feature spots by default, configurable
  difficulty), strip/1D world (32 — End portal must be reachable in the
  strip; optional Nether strip), chaos biomes (33 — configurable region
  size), End start (34 — hardcore-beatable with a solid starter chest), and
  **stacked biome layers** (35 — vertically stacked biome surfaces replacing
  the deep underground of a limited-size world; deep-ore budget for
  lapis/gold/diamond needs accounting). 35's vertical interpretation is
  flagged in GOALS for Jason's confirmation before its design spike. The
  plan is now 20 phases (chaos = Phase 4 next to the biome-selection work it
  reuses; strip = Phase 6 next to limits; dry world joins lava ocean in
  Phase 9 as ocean-fluid variants; End start = Phase 15 sharing Phase 14's
  non-Overworld-spawn spike; stacked layers = Phase 17 after flat, whose
  layer concepts it reuses). DESIGN §20.11 holds the technical notes.
- 2026-07-16 — Jason confirmed GOALS 35's stacked-biome interpretation:
  stacked **horizontal slabs** (plains above desert above taiga), each layer
  flat or low-relief using the flatter variants of its biome — a thin slice
  can't fit extreme-hills relief — so the feature builds on the flat layer
  machinery (§19), not full noise terrain per layer; and each layer gets a
  configurable air gap above its surface so biome-specific trees and
  structures generate and grow on every layer. He also added two
  variations, captured as GOALS 36 and 37: a biome-sequence strip (ordered
  biome bands every N chunks along the 1D strip, reusing the chaos-selection
  machinery with ordered bands — TODO 6.3) and multi-biome chunk islands
  (sky-chunk islands beyond the starter carry different biomes, per-island
  top-only vs full-column, and where feasible showcased cave biomes, geodes,
  and structure chunks — TODO 12.2, with per-island feature targeting an
  explicit design question).
- 2026-07-16 — Scope decision: **no glowing-ores option in worldz** (Jason
  proposed, agreed to keep it out). It changes ore appearance, not
  generation; emissive-texture resource packs cover it with no code, and
  real per-world light emission would need fragile block-property mixins
  plus an unwanted gameplay side effect (light suppresses mob spawning
  around veins). If ever built, it belongs in jlt_ores as a mod-wide client
  option. Recorded in GOALS.md "Considered and rejected".
- 2026-07-16 — Jason's GOALS Question 1 answered from the Worldz5/6 evidence:
  ocean-only biome filtering cannot prevent land (biome ≠ terrain), so endless
  ocean needs the terrain cap and distant natural islands (use case 04) come
  from releasing that cap beyond the exclusion zone. Recorded in GOALS.md
  inline and DESIGN §20.5.
- 2026-07-16 — **Done (Phase 2.1/2.2).** `jlt_worldz:single_biome`, the first
  typed preset, ships reusing the existing `jlt_worldz:limited`/
  `jlt_worldz:enveloped` registry types unchanged — `LayoutMode.SINGLE_BIOME`
  already produced a uniform, land-shaped single-biome world, so the new work
  is UX exposure plus one non-round-tripped codec hint field (`world_type`)
  telling `LimitedBiomeSource.resolve()` to default from the new
  `singleBiome:` config section instead of the flat top-level fields. A small
  `SingleBiomeCustomizeScreen` exposes only land biome/starter biome/starter
  radius/spawn strategy — border/exterior/starter-land stay YAML-only shared
  sections for this type until Phase 5.3 formally wires limits into every
  type's screen. `allowedBiomes` for this type is never user-edited — it's
  auto-derived as `{landBiome} ∪ {starterBiome}` to avoid a two-lists-that-
  must-agree trap. GOALS 12's "based on seed — including size and location"
  is implemented as seed-determined location only (existing
  `preferred_natural_biome` search + recentering); "size" is the
  configurable `starterRadiusBlocks`, not natural-patch-boundary detection —
  logged in TODO's Deviation log in case Jason meant the literal reading.
  See DESIGN §20.2's Phase 2.1 subsection for the full codec/config shape.
- 2026-07-16 — **Done (Phase 2.3), no new code needed.** Confirmed
  structures/caves/vanilla randomness already follow the real seed for
  `single_biome` worlds through existing, preset-agnostic machinery: (a)
  `ChunkMapMixin`'s `setLayoutSeed` hook type-checks
  `instanceof LimitedBiomeSource` generically, not against a specific
  preset, so it applies to `single_biome` worlds exactly like `worldz`
  worlds; (b) `EnvelopedChunkGenerator.createStructures` delegates to
  `super.createStructures`, and carvers/features run through the wrapped
  vanilla `NoiseBasedChunkGenerator` unchanged — both already covered by
  Phase 15/1.3-era component tests, untouched by Phase 2. The only
  single_biome-specific seed question was whether its own layout sampling
  (not vanilla's) depends on seed; added
  `singleBiomeModeSamplingIsIndependentOfSeed` to document that it
  deliberately does not (one biome, one answer per position — GOALS 10's
  "randomness based on seed" is entirely vanilla's, not Worldz's layout).
- 2026-07-16 — **Done (Phase 2.4).** Per-world snapshot writer implemented:
  `logic.WorldSnapshotWriter` (pure, write-only — no `parse`/`sanitize` half
  since the file is never read back) renders a `LimitedBiomeSource`'s
  resolved fields as commented YAML. The write call lives inside
  `SpawnOriginManager.markResolved`, the single point every
  `resolveFreshOrigin` branch already converges through exactly once per
  fresh world — chosen over duplicating a call at each loader's own
  `LevelEvent.CreateSpawnPosition`/`setInitialSpawn` site because it already
  has the final resolved origin in hand (accurate even for
  `preferred_natural_biome` worlds) and needs no new per-loader wiring.
  Target: `<worldFolder>/jlt_worldz-snapshot.yaml` via
  `overworld.getServer().getWorldPath(LevelResource.ROOT)` (verified against
  the decompiled 26.2 `MinecraftServer`/`LevelResource` sources), best-effort
  (catches `IOException`/`RuntimeException`, logs a WARN, never blocks world
  creation). Added a new loader-neutral way to read the mod's own build
  version at runtime: `${mod_id}-version.properties`, expanded at build time
  by `build-logic`'s shared `multiloader-common.gradle` (same mechanism
  already used for `fabric.mod.json`/`neoforge.mods.toml`) and read once into
  `WorldzCommon.MOD_VERSION` — avoids needing separate Fabric-`ModContainer`
  vs. NeoForge-`ModList` lookups in loader-neutral `common` code for one
  cosmetic header field. See DESIGN §20.3's Phase 2.4 subsection.
- 2026-07-17 — **Phase 2.7 acceptance closed; Jason approved Phase 3.** The
  in-game pass surfaced and fixed three real defects along the way
  (spawn-area performance, `ChunkGeneratorStructureState`'s stale dummy
  `RandomState` causing floating structures, `starter_at_origin` deferring
  to vanilla's climate-blind spawn search) — see the dedicated entries
  above. One residual item was explicitly *not* chased further at Jason's
  direction: two floating villages seen in world `Worldz-10`
  (`(-95, -910)`, `(1427, -1302)`) were never checked against true vanilla
  at the same seed, unlike every earlier floating-structure report in this
  investigation (all of which turned out to be either the real bug, now
  fixed, or an ordinary vanilla terrain-elevation quirk once compared
  properly). Given the weight of contrary evidence — a 10-village/5-biome
  comparison at 9/10 flush, and a separate later Worldz-11 check where every
  vanilla village location was flush regardless of biome — the likely
  explanation is the latter (seed-specific vanilla quirk, not a Worldz
  defect), but this is not confirmed. **Decision: log it, don't dig further
  now.** If floating structures are seen again in a later phase's testing,
  the correct next step is the same controlled vanilla-vs-Worldz comparison
  at the exact coordinates — not re-opening the RandomState/threading
  hypothesis, which diagnostic logging already conclusively disproved (one
  `RandomState` identity observed across an entire session, all call sites,
  both threads). See TODO.md's "Questions for Jason" for the same note.
- 2026-07-17 — **Phase 3.1's pass-through fix was incomplete; found and
  fixed the terrain half (0.2.7).** Jason's first acceptance check on
  config 14 (world `Worldz-14`) found F3 correctly reporting the River
  biome at a known vanilla river location, but the terrain rendered as
  flat plains — no water except small cave pools, trees growing where a
  riverbed should be. Root cause: `getNoiseBiome`'s pass-through (0.2.6)
  only fixed biome *selection*; `EnvelopedChunkGenerator`'s layout terrain
  raise never learned about it. `WorldLayoutPlan.sampleAt` always reports
  `landFactor=1.0` for `SINGLE_BIOME` mode (it has no concept of the
  pass-through at all — that check lives entirely in `LimitedBiomeSource`,
  a separate class), so `LayoutTerrainProfile.targetHeight` raised *every*
  column — including a real river's natural depression, which the
  delegate's own unmodified vanilla noise had already correctly carved —
  up to at least sea level + 2. The biome label was right; the ground
  under it had already been flattened by a completely separate pass.
  Fixed by adding `LimitedBiomeSource.isNaturalPassThroughAt(blockX,
  blockY, blockZ, sampler)`, refactored to share its actual check
  (`naturalPassThroughBiome`) with `getNoiseBiome` so the two can never
  disagree, then having `EnvelopedChunkGenerator.layoutFloorFor` consult
  it (using the already-computed natural floor as the representative Y —
  standard overworld biome climate parameters aren't meaningfully
  Y-sensitive this close to the surface, so this is a reasonable
  approximation rather than trying to replicate vanilla's exact per-cell
  Y sampling) and return the natural floor untouched when it applies,
  before the `landFactor` blend ever runs — exactly like a `VOID`/`LEGACY`
  column.
  **Also found: I forgot to redeploy the jar after 0.2.6.** Jason's
  *first* config-14/15 test round (before this fix) reported "no rivers/
  oceans anywhere" — that turned out to be entirely because the Prism
  instance was still running the pre-3.1 0.2.5 jar (I built 0.2.6 but
  never ran `:fabric:deployToPrism`). Lesson for every future phase:
  **deploy the fresh jar to the active Prism instance(s) as the literal
  last step before telling Jason a phase is ready to test** — a clean
  `./gradlew build` is not sufficient, and forgetting it produces a false
  "feature doesn't work" report that costs a full test round-trip. Also
  worth remembering generally: because `starter_radius` is always encoded
  once a world is actually saved, *any* world created under an older jar
  missing a new optional codec field permanently decodes that field as
  absent/default on every future load, even after the jar is updated —
  new-worlds-only applies per-field, not just per-mod-version. A world
  tested against a bug fix must always be freshly created *after* the
  fixed jar is deployed, never just reopened.
- 2026-07-18 — **Confirmed working (0.2.7), one cosmetic gap logged, not
  fixed.** Jason recreated Worldz-14 under 0.2.7 (config 14, `allowRivers`)
  and confirmed the river follows the exact vanilla path and exits
  naturally into the ocean — the terrain-raise fix above is doing its job.
  Separately noted: bank entry looks abrupt, and the channel reads as
  mostly deep where a natural mix of shallow/deep sections was expected.
  **Working theory, unconfirmed:** the pass-through is a hard on/off
  switch with no blending at its boundary (DESIGN §20.5 deliberately
  specified "zero height-adjustment machinery" for 13/14, unlike the
  removed `MIXED`/`OCEAN` grid modes' dedicated coast-blend width) — one
  column outside the vanilla-classified river area, `single_biome`'s
  `landFactor=1.0` raise applies at full strength with no transition, so
  the natural riverbed likely sits at a genuine elevation step below the
  raised desert rim on both sides, which could visually read as a deeper,
  more abrupt channel even where the underlying vanilla depth is
  ordinary. **Jason's explicit call: log it, don't fix now** — if this
  needs revisiting, a coast-blend-style transition width at the
  pass-through boundary (mirroring the removed grid modes' mechanism, but
  scoped to just this boundary) is the likely fix shape.
  **Update, same day:** config 15 (`allowOceans`) retested fresh under
  0.2.7 too — every vanilla ocean biome variant generates correctly with a
  natural coastline shape, and the same abrupt-transition cosmetic issue
  appears at the shore (confirmed via screenshot: desert meeting a sharp
  vertical wall of deep/frozen ocean, no beach gradient). Same root cause
  as the river case, same deferred status — this is one shared boundary
  behavior affecting both 13 and 14, not two separate issues.
- 2026-07-17 — GOALS 15 (cave-biome pass-through) moved out of Phase 3 into
  a new TODO "Backlog" section rather than kept as draft task 3.3. GOALS.md
  already calls it "scope for a later phase" and it needs a materially
  different design (depth-aware `WorldLayoutPlan` sampling, not the
  surface-family pass-through 3.1 implements) — keeping it inside Phase 3's
  gate would have blocked the phase on a task nobody intended to execute
  yet. Not scheduled to a specific phase number; revisit with Jason later.
- 2026-07-18 (Phase 5) — Two upfront design calls, both Jason's recommended
  option: (1) End border is a simple `EndBorderConfig`/`EndLimit` — a
  `carryFromOverworld` toggle plus a `minimumRadiusBlocks` floor (default
  256) — not full `BorderConfig` schedule parity, since GOALS 17 only asks
  to carry the Overworld's eventual size over, not independently animate
  an End-specific expand/collapse. It's static: resolved once at world
  creation from the Overworld's *final* radius, floored at the minimum.
  (2) Chunk-unit input (GOALS 17/18 "blocks... or chunks") is a UI-only
  `RadiusUnit` toggle in the border/exterior screens that converts
  whatever's currently typed; blocks stays the one persisted/validated
  unit everywhere (YAML, snapshot, codecs) — no schema change.
  Also: 5.1's audit found every other GOALS 17-20 item (invisible-wall vs.
  void-exterior separation, expansion rate + initial delay, a collapsing
  border's `finalRadiusBlocks` already being its minimum, center-safe
  spawn) already correct against the wording — the only real code gap was
  the chunk-unit input. And: border/exterior/limits already applied
  uniformly to every world type since Phase 2-4 at the *code* level (each
  typed preset's editor already read the shared config's plans); 5.3's
  real gap was Customize-*screen* exposure — `single_biome`/`chaos_biomes`
  had no in-screen border/exterior controls at all, and the generic
  preset had no End Border control (5.2 added it config-only, as a
  documented stop-gap, then 5.3 finished it). This means Phase 4.2's
  Customize-screen acceptance ("chaos_biomes shows none of the full
  Worldz preset's border/exterior controls") is now intentionally
  superseded — logged in TODO.md's Deviation log, not a regression.
- 2026-07-18 (post-Phase-5 review, planning only — no code yet) — Jason
  clarified GOALS 19-20 and added GOAL 38 after testing configs 20-21; four
  decisions, all the recommended option (full design in DESIGN §21, tasks in
  TODO Phases 5b/5c): (1) keep the shipped *continuous* resize style (it
  wasn't his original intent, but he likes it — especially for collapsing)
  AND add a *stepped* style — abrupt jumps of X blocks every Y days — via a
  new `resizeStyle: continuous | stepped` field reusing the existing
  `resizeRateBlocks`/`resizeRateDays` (default `continuous`, back-compatible);
  (2) steps snap instantly (`setSize`), no mini-lerp; (3) easing curves
  (`resizeCurve`, rate slowing near the final size) are approved future
  scope but deferred, unscheduled; (4) GOAL 38 soft void border — no wall,
  terrain just ends, player can fall off; expansion backfills void chunks
  with real terrain and **overwrites** anything built in the void ring
  (documented challenge rule). Feasibility verified: collapse direction is
  easy (budgeted clearing), expansion requires WorldEdit-`//regen`-style
  chunk regeneration (pipeline classes confirmed present in 26.2 sources) —
  mandatory spike (5c.1) with a Jason go/no-go before 5c.2 is re-planned
  and executed.
- 2026-07-18 (post-Phase-5 review, second pass — planning only, no code) —
  Border presentation & enforcement (GOAL 18 clarification + new GOAL 39,
  DESIGN §22, TODO Phase 5d). Decisions: (1) border *visual* and
  *enforcement* are independent axes, both orthogonal to the exterior —
  `visual: striped | invisible` (+ optional static-only marker ring one
  block beyond the boundary), `enforcement: wall | damage | none`; Jason's
  original `borderType: void | barrier` framing was remodeled this way to
  preserve the standing border-vs-world-size orthogonality. (2) Damage
  enforcement (GOAL 39): permeable edge, chat warning, time-based grace
  with **instant reset** on re-entry — deliberately no separate "breath
  meter": health is the meter, eating is the recovery cost, hunger prices
  abuse. Screen tint deepens as grace runs out (grace-driven custom
  overlay recommended over reusing vanilla's distance-based vignette).
  (3) **No-immunity rule**: nothing may grant 100% protection outside the
  border — mitigation slows damage / optionally extends grace only;
  enforced structurally via damage-type tags (`bypasses_armor`,
  `bypasses_effects` so Resistance V can't immunize, NOT
  `bypasses_invulnerability` so vanilla Protection works, capped at 80%)
  plus bounded custom-enchantment values. (4) Enchantments: vanilla
  Protection reduces border damage for free (verified protection.json's
  only condition is the bypasses_invulnerability tag's absence); custom
  data-driven enchantment scoped to our damage type for extra reduction +
  code-side bounded grace extension. (5) Barrier-block shell wall
  considered and parked (creative leak, static-only); static soft-void
  edge needs no new code (border off + void exterior + explicit boundary
  radius — promote via test config). Feasibility verified in 26.2 sources:
  `WorldBorderRenderer.render` early-outs at alpha ≤ 0 (one-mixin wall
  suppression), `Hud.extractVignette` strength is 0 when warning
  blocks/time are 0 (server-side vignette kill) and clamps to full red
  outside the border, border collision has a single injection point
  (`Entity.collectCollidersIgnoringWorldBorder` gated on
  `isInsideCloseToBorder`), and vanilla border damage lives in
  `LivingEntity` via `damagePerBlock`/`safeZone` (we zero it and run our
  own timer in `WorldLimitManager.onServerTick`).
- 2026-07-18 (Phase 5 acceptance results + one gap) — Config 20 (static
  border) and config 22 (slow 2048→256 collapse over 40 days after a
  10-day delay) both **pass** in-game under 0.2.12; config 22 "works
  really well", only pre-existing biome-painting artifacts noted (not new;
  related to the Backlog's chaos-biomes water-relabel discussion). Config
  21 exposed a gap, not a bug: `WorldzConfig.MIN_BORDER_RADIUS_BLOCKS = 64`
  clamps the initial radius up, so `initialRadiusBlocks: 4` rendered as a
  64-block border. Jason wants very small starts (1–2 blocks). Decision:
  **lower the floor to 1 globally** (TODO 5.5) — initial, final, and End
  minimum all droppable to 1; beatability is the user's responsibility.
  Safe because nothing structural needs 64 (BorderSchedule only requires
  `> 0`, vanilla setSize accepts tiny diameters); the 64 was an arbitrary
  sanitizer floor. **Done same day (0.2.13, TODO 5.5):** floor lowered to
  1, two tests updated to exercise the new floor instead of the old one
  (full suite green), config 21 dropped to a genuine 2-block start,
  0.2.13 built and deployed to Worldz-Test — Jason to re-test config 21.
- 2026-07-18 (same-day follow-up, TODO 5.6) — Retesting config 21 under
  0.2.13 surfaced a second, worse defect from the same root cause: Jason
  spawned **outside** the (now tiny) border and took vanilla border
  damage. `SpawnOriginManager.safeSpawnNear` had always added a hardcoded
  `+8, +8` to the origin (to center spawn in the origin chunk) with zero
  awareness of the border — invisible for a year of testing only because
  the old 64-block floor guaranteed 8 blocks of slack. **Fixed (0.2.14):**
  new `WorldLimitPlan.DimensionLimit.safeSpawnOffsetBlocks()` — preferred
  8-block offset when the border is disabled/large, shrinking to as low
  as 0 for a tiny configured initial radius; `safeSpawnNear` calls it
  instead of the hardcoded constant. **Reusable lesson:** the logic lives
  on the pure `DimensionLimit` record, not in `SpawnOriginManager` itself,
  because `SpawnOriginManager` turns out to be untestable by plain
  JUnit — merely loading the class throws `NoClassDefFoundError`
  (`NoiseBasedChunkGenerator`, `HolderGetter`, etc. aren't on the test
  classpath). Any future fix to that class needs to extract pure logic
  into a `logic`/`worldgen` record/pure-static-method first, the same way
  `BorderSchedule` and `EndLimit.resolveRadiusBlocks` already do — check a
  class's import list for heavy `net.minecraft.*` worldgen types before
  assuming a plain `assertEquals` test against it will even load. Full
  suite green (244 tests); 0.2.14 deployed to Worldz-Test. **Jason
  confirmed config 21 working, 2026-07-18** — both the radius-floor
  (5.5) and spawn-offset (5.6) fixes hold up in-game; config 21 is
  closed out.
- 2026-07-18 — **Config 23 (End border carry-over) confirmed working**
  in-game (Jason). Phase 5's config-based acceptance is now fully done
  (20, 21, 22, 23 all passed); only the two no-config-file UI checks are
  unconfirmed, not treated as blocking further phase work. Jason then
  gave the go-ahead to proceed through the rest of the "5-series" (5b,
  5c, 5d) — implementing them now, one phase at a time with the usual
  stop-and-test gate at each phase's end; 5c's mandatory spike/go-no-go
  split (DESIGN §21.2/TODO 5c) still applies regardless of this
  go-ahead.
- 2026-07-18 — **Phase 5b.1 done (0.2.15):** `resizeStyle: continuous |
  stepped` data model/config/codec/UI plumbing. Reused the exact
  additive-field technique from Phase 5.2/5.3 (every existing constructor
  overload keeps its old external signature, defaulting the new field
  internally) across `BorderSchedule`, `WorldLimitPlan.DimensionLimit`,
  and `WorldzCustomization.BorderSettings` — zero call-site breakage,
  confirmed by a clean compile before any test file needed touching.
  Notable: `SingleBiomeCustomization`/`ChaosBiomesCustomization` needed
  **no changes at all**, unlike the End-border addition — they only ever
  held a `BorderSettings` object as a field, never its individual values,
  so a field added inside `BorderSettings` doesn't ripple outward the way
  a whole new sibling field did in Phase 5.3. Stepped math
  (`BorderSchedule.steppedRadiusAtTick`) reuses the same rate-based
  ceiling-division formula `durationTicks()` already had for continuous's
  rate mode — no separate "how long will this take" calculation needed.
  This task is model/config/UI only; nothing resizes in a stepped way in
  a live world yet (that's 5b.2's tick driver).

## Reference Log

- Phase 0: Fabric project structure and `fabric.mod.json` entrypoints — https://docs.fabricmc.net/develop/getting-started/project-structure
- Phase 0: Gradle composite `build-logic` and multi-project structure — https://docs.gradle.org/current/userguide/intro_multi_project_builds.html
- Phase 0: Gradle Java toolchains — https://docs.gradle.org/current/userguide/toolchains.html
- Phase 1: Gson malformed-input behavior and strict JSON diagnostics — https://github.com/google/gson/blob/main/Troubleshooting.md
- Phase 1: Gson object serialization/deserialization API — https://github.com/google/gson
- Phase 2: Fabric codec patterns and registry dispatch — https://docs.fabricmc.net/develop/serialization/codecs
- Phase 2: Fabric built-in registry registration pattern — https://docs.fabricmc.net/develop/items/custom-data-components
- Phase 2: NeoForge deferred codec registration pattern — https://docs.neoforged.net/docs/1.21.1/worldgen/biomemodifier/
- Phase 2: Authoritative Minecraft 26.2 generated sources — `common/build/moddev/artifacts/vanilla-26.2-1-sources.jar`
- Phase 3: World preset format reference — https://misode.github.io/worldgen/world-preset/
- Phase 3: World preset normal-tag behavior — https://minecraft.wiki/w/World_preset_tag_%28Java_Edition%29
- Phase 3: Authoritative vanilla preset, tag, and language resources — `~/.gradle/caches/fabric-loom/26.2/minecraft-client.jar`
- Documentation: Fabric player installation guide — https://docs.fabricmc.net/players/installing-mods
- Documentation: NeoForge client installation guide — https://docs.neoforged.net/user/docs/client/
- Tag lifecycle fix: NeoForge holder-set migration primer (named holder sets are
  dynamically managed tag representations) — https://docs.neoforged.net/primer/docs/1.21.2/
- Test hardening: JUnit 5 user guide (`@Test`, assertions, and `@TempDir`) —
  https://docs.junit.org/5.13.1/user-guide/index.html
- Test hardening: Java 25 unmodifiable collection snapshots —
  https://docs.oracle.com/en/java/javase/25/core/java-core-libraries-developer-guide.pdf
- Completion audit: canonical MIT license identifier and text —
  https://spdx.org/licenses/MIT.html
- YAML migration: SnakeYAML 2.6 artifact metadata, Apache-2.0 license, and
  upstream project link — https://central.sonatype.com/artifact/org.yaml/snakeyaml
- YAML migration: verified Fabric `include` and NeoForge `jarJar` dependency
  patterns against the maintained sibling mods `../info` and `../trees`.
- Limited worlds: authoritative Minecraft 26.2 generated sources for
  `WorldBorder`, `ServerLevel`, `ChunkGenerator`, structure placement, portal
  clamping, and spawner configuration —
  `common/build/moddev/artifacts/vanilla-26.2-1-sources.jar`.
- Limited worlds: Fabric server lifecycle event guidance —
  https://github.com/FabricMC/fabric-api/blob/26.1.2/CONTRIBUTING.md
- Limited worlds: NeoForge game-event bus guidance —
  https://docs.neoforged.net/docs/concepts/events/
- Customize UI: NeoForge 26.2 preset-editor registration event —
  https://github.com/neoforged/NeoForge/blob/26.2.x/src/client/java/net/neoforged/neoforge/client/event/RegisterPresetEditorsEvent.java
- Customize UI: Fabric client-only mixin registration —
  https://wiki.fabricmc.net/tutorial:mixin_registration
- Exterior generator: Minecraft 26.2 `ChunkGenerator`,
  `NoiseBasedChunkGenerator`, `ChunkAccess`, and chunk-status sources —
  `common/build/moddev/artifacts/vanilla-26.2-1-sources.jar`.
- Exterior generator: Fabric codec dispatch/registry guidance —
  https://docs.fabricmc.net/develop/serialization/codecs
- Exterior generator: NeoForge asynchronous chunk-generation migration notes —
  https://docs.neoforged.net/primer/docs/1.21/
- Delayed borders: Fabric end-server-tick event source —
  https://github.com/FabricMC/fabric/blob/26.2/fabric-lifecycle-events-v1/src/main/java/net/fabricmc/fabric/api/event/lifecycle/v1/ServerTickEvents.java
- Delayed borders: NeoForge pre/post server-tick event source —
  https://github.com/neoforged/NeoForge/blob/26.2.x/src/main/java/net/neoforged/neoforge/event/tick/ServerTickEvent.java
- Delayed borders: authoritative Minecraft 26.2 `WorldBorder` and saved-data
  sources — `common/build/moddev/artifacts/vanilla-26.2-1-sources.jar`
- Starter land: authoritative Minecraft 26.2 `NoiseBasedChunkGenerator`,
  `ChunkGenerator`, `ChunkPyramid`, `NoiseColumn`, `Heightmap`, and chunk-stage
  sources — `common/build/moddev/artifacts/vanilla-26.2-1-sources.jar`.
- Starter land: Fabric codec guidance for persisted world-generation values —
  https://docs.fabricmc.net/develop/serialization/codecs
- Starter-land relief correction: authoritative Minecraft 26.2 `RandomState`,
  `Noises`, `NormalNoise`, `SurfaceSystem`, and generator sources —
  `common/build/moddev/artifacts/vanilla-26.2-1-sources.jar`.
- Starter-land relief correction: Fabric API upstream repository and worldgen
  integration source survey — https://github.com/FabricMC/fabric-api
- Coordinated layouts: Minecraft's description of terrain shapes no longer
  requiring biome-specific variants —
  https://feedback.minecraft.net/hc/en-us/articles/4409293520269-Minecraft-Java-Edition-Snapshot-21w37a
- Layout/flat/spawn planning: authoritative Minecraft 26.2
  `MultiNoiseBiomeSource`, `BiomeSource`, `RandomState`, `MinecraftServer`,
  `FlatLevelSource`, `FlatLevelGeneratorSettings`, flat preset resources, and
  structure-set and slime spawn-rule sources —
  `common/build/moddev/artifacts/vanilla-26.2-1-sources.jar`.
- Floating-village root cause: authoritative Minecraft 26.2 `JigsawStructure`,
  `JigsawPlacement`, `ChunkGenerator` (`getFirstFreeHeight`/`getBaseHeight`)
  sources — `/tmp/mc-26.2-sources` (decompiled from
  `mergeWithSources_...jar`); confirmed a jigsaw structure's pieces are all
  placed relative to one anchor height sampled once, never re-queried per
  piece.
- Dummy-RandomState fix: authoritative Minecraft 26.2 `ChunkMap`,
  `NoiseBasedChunkGenerator`, `ChunkGenerator` sources —
  `/tmp/mc-26.2-sources` (decompiled from `mergeWithSources_...jar`) for the
  constructor logic and method signatures, PLUS `javap -p` directly against
  `minecraft-merged-deobf-26.2.jar`
  (`~/.gradle/caches/fabric-loom/minecraftMaven/...`, the actual jar Loom
  compiles against) to confirm `NoiseBasedChunkGenerator`'s real `final`
  modifier, since the decompiled source disagreed. NeoForge's own
  `net.neoforged.fml.loading.moddiscovery.ModFileParser` source (from the
  `fancymodloader` sources jar) for the `[[mixins]]` `neoforge.mods.toml`
  schema, since no sibling mod in this workspace had configured NeoForge
  mixins before.

## Verification Log

- 2026-07-14 / Phase 0 — `./gradlew build` succeeded across `common`, `fabric`, and `neoforge` on Temurin 25. No tests exist yet. Javadoc emitted only missing-comment warnings; Gradle reported template/plugin deprecations affecting eventual Gradle 10 compatibility.
- 2026-07-14 / Phase 1 — `./gradlew build` succeeded across both loaders with 16 JUnit tests covering config creation/fallback/tolerance/filtering/clamping, biome/tag parsing, canonicalization, and starter-zone boundary/overflow math.
- 2026-07-14 / Phase 2 — `./gradlew build` succeeded with `LimitedBiomeSource` compiled into both Fabric and NeoForge artifacts. Codec/preset runtime decoding remains assigned to the Phase 3 launch and Phase 4 smoke tests as specified by the design.
- 2026-07-14 / Phase 3 — 20 JUnit tests passed, including exact preset/tag/lang structure checks. Fabric reached title-screen resource reload. Isolated Fabric and NeoForge servers each created, prepared spawn for, saved, and cleanly stopped a fresh `level-type=jlt_worldz:worldz` world. Visual create-screen dropdown confirmation remains a human check.
- 2026-07-14 / Runtime config coverage — A Fabric server created a world from
  `#minecraft:is_overworld` after lazy tag expansion fixed the verified 26.2
  tag-binding lifecycle failure. Fabric and NeoForge also created and saved
  plains worlds with a 512-block cherry-grove starter zone; their saved
  `world_gen_settings.dat` files contain the resolved biome, starter, radius,
  and `jlt_worldz:limited` type.
- 2026-07-14 / Config isolation — The Fabric starter world reopened after the
  global config changed to desert with no starter. Its saved settings remained
  plains + cherry, `locate biome` found cherry at the origin and plains 520
  blocks away, and the server stopped cleanly.
- 2026-07-14 / JUnit hardening — The common suite increased from 20 to 31
  passing tests. New coverage directly verifies climate-entry filtering
  (including duplicate parameter entries and immutable results), malformed
  field types, empty allowed lists, docs sanitation, summaries, null parser
  inputs, immutable parser results, exact diagonal/zero-radius zone boundaries,
  and exact preset/tag/lang resource membership.
- 2026-07-14 / Completion audit — Found and corrected a real license mismatch:
  root `LICENSE` was CC0 while the design and loader manifests declared MIT.
  Two project-metadata tests now lock the license, root project name, package
  group, mod identity, version, Java level, and Minecraft version. The suite
  now contains 33 tests.
- 2026-07-14 / YAML migration — Replaced the runtime Gson config parser with a
  safe map-based YAML parser/emitter and added JSON-to-YAML migration. Focused
  config tests cover defaults, sanitation, type failures, precedence, legacy
  backup, and preservation of malformed YAML/JSON. Both loader artifacts embed
  the parser dependency, attribution notice, and full Apache-2.0 license. A
  clean `./gradlew clean build` and the final `./gradlew build` passed with 37
  JUnit tests; jar inspection confirmed SnakeYAML and license resources in both
  loader artifacts.
- 2026-07-14 / Limited worlds — `./gradlew clean build` passed across common,
  Fabric, and NeoForge with 50 JUnit tests. New tests cover YAML border schema
  and validation, schedule growth/shrink/static interpolation, immutable plan
  snapshots, extreme coordinate bounds, and fallback placement. Javadoc was
  reviewed cleanly; live acceptance remains explicitly assigned to Jason.
- 2026-07-14 / Release 0.1.1 — The initial clean build reproduced the user's
  failure: 49 tests passed and `ProjectMetadataTest` alone failed because it
  still expected 0.1.0. After aligning the metadata contract and documentation,
  `./gradlew clean build` passed all 50 tests and produced versioned Fabric and
  NeoForge artifacts.
- 2026-07-14 / Customize UI — `./gradlew clean build` passed across common,
  Fabric, and NeoForge with 57 JUnit tests. New tests cover config snapshots,
  editable list parsing, canonicalization, invalid starter/numeric/range input,
  immutable border-plan conversion, localized resource presence, and both
  loader registration contracts. Review fixed every new Javadoc warning;
  `git diff --check` passed, and no dedicated lint plugin is configured. Jar
  inspection confirmed the shared screens/editor and Fabric client mixin in the
  Fabric artifact, plus the shared screens/editor and client event registration
  in the NeoForge artifact. Live testing remains assigned to Jason.
- 2026-07-15 / Exterior configuration — `./gradlew build` passed across common,
  Fabric, and NeoForge with 71 JUnit tests. Coverage includes envelope
  classification/resolution, YAML sanitation and compatibility, codec defaults,
  customization validation, rate precedence, growth/shrink interpolation, and
  partial-interval rounding. Javadocs and `git diff --check` are clean.
- 2026-07-15 / Exterior generation — `./gradlew build` passed across common,
  Fabric, and NeoForge with 76 JUnit tests. Pure tests cover ocean layers,
  height-query agreement, build-height clamping, and envelope classification;
  resource/component tests cover both wrapped dimensions, the vanilla End, and
  both loader registrations. Javadocs and `git diff --check` are clean.
- 2026-07-15 / Exterior Customize — `./gradlew build` passed across common,
  Fabric, and NeoForge with 80 JUnit tests. New coverage locks localized UI
  fields, screen wiring, rate submission, Nether mode restrictions, and
  effective progression radii for border-only, exterior-only, combined, and
  ocean-transition worlds. Javadocs and `git diff --check` are clean.
- 2026-07-15 / Exterior completion — `./gradlew clean build` passed all modules
  and 80 JUnit tests. Artifact inspection confirmed the enveloped generator,
  exterior screen, wrapped preset, SnakeYAML, and Apache license in both 0.1.1
  loader jars. README and DESIGN now describe rate precedence, automatic and
  explicit envelopes, ocean transition, persistence, supportive progression
  bounds, and the vanilla End. Fabric Loom still prints its known warning that
  dependency version `2.6` is not valid semver; bundling succeeds. Per the user,
  no live test was run and manual acceptance is deferred until handoff.
- 2026-07-15 / Delay model — `./gradlew build` passed all modules and 85 JUnit
  tests. Delay coverage includes growth, collapse, deferred immediate resize,
  rate-duration independence, YAML sanitation, customization persistence, and
  immutable plan snapshots. Optional codec and saved-data fields default to zero
  or no pending start, preserving old configs and initialized worlds. Javadocs
  and `git diff --check` are clean.
- 2026-07-15 / Delayed runtime — `./gradlew build` passed all modules and 86
  JUnit tests. Fabric uses `END_SERVER_TICK`; NeoForge uses
  `ServerTickEvent.Post`. Initialization installs the initial diameter and saves
  each due game tick. The shared tick manager starts the vanilla transition,
  clears that pending tick, and stops doing plan lookups once neither dimension
  is waiting. Customize displays total duration and delay side by side. Loader
  wiring/localization component tests, Javadocs, and whitespace lint are clean.
- 2026-07-15 / Delay completion — `./gradlew clean build` passed common, Fabric,
  and NeoForge with 86 JUnit tests. Jar inspection confirmed the shared delayed
  manager/state, border editor, localization, and bundled SnakeYAML in both
  0.1.1 artifacts. README, generated YAML documentation, canonical example,
  DESIGN, TODO, and Memory describe the delay and offline-time semantics. The
  known Fabric Loom non-semver warning for SnakeYAML `2.6` remains harmless. Per
  the user, no live test was run.
- 2026-07-15 / Release 0.1.2 — `./gradlew clean build` passed all modules and 86
  JUnit tests. Artifact inspection confirmed `0.1.2` in both loader filenames,
  Fabric `fabric.mod.json`, and NeoForge `neoforge.mods.toml`. Javadocs and
  `git diff --check` are clean; the known SnakeYAML semver warning is unchanged.
- 2026-07-15 / Starter-land model — `./gradlew build` passed all modules and 94
  JUnit tests. New pure coverage locks plan validation/config snapshots,
  smoothstep transition endpoints and midpoint, preservation of natural high
  ground, zero-width behavior, foundation bounds, extreme-coordinate safety,
  YAML sanitation/defaults, and the optional codec compatibility contract.
  Javadocs and `git diff --check` are clean.
- 2026-07-15 / Starter-land generation — `./gradlew build` passed all modules
  and 95 JUnit tests. The Overworld wrapper raises low columns from the
  delegate's `OCEAN_FLOOR_WG` height before vanilla surface rules, repairs
  air/fluid gaps down to the configured foundation depth after carvers, and
  preserves a five-block surface shell. Base height/column queries apply the
  same profile, structures run through the wrapper, and exterior replacement
  remains the final authority. Nether/End and disabled or older plans are
  untouched. Javadocs and `git diff --check` are clean.
- 2026-07-15 / Starter-land Customize — A dedicated editor exposes enabled,
  transition-width, and foundation-depth values without further crowding the
  dimension editors. YAML seeds new fieldless presets; Done persists the plan
  with the explicit source, and reopening Customize reads that plan back.
  Enabling it without a starter biome is allowed but has no effect, which lets
  players edit fields in either order. Spawn/progression use the wrapper's
  corrected queries or finished chunk heightmaps. `./gradlew build` passes 96
  tests; Javadocs and `git diff --check` are clean.
- 2026-07-15 / Release 0.1.3 — `./gradlew clean build` passed common, Fabric,
  and NeoForge with 96 JUnit tests. Artifact inspection confirmed 0.1.3 in both
  loader filenames and embedded manifests, plus the starter-land model,
  profile, codec, wrapper integration, editor, localization, and bundled YAML
  dependency. README, generated YAML docs, example config, DESIGN, TODO, and
  Memory describe the terrain guarantee and old-save compatibility. Javadocs
  and `git diff --check` are clean. The known Fabric Loom warning about
  SnakeYAML's non-semver `2.6` coordinate remains harmless. Per the user, no
  live test was run; manual acceptance is deferred to Jason.
- 2026-07-15 / Release 0.1.4 — Version-only follow-up requested for manual
  acceptance. `./gradlew clean build` passed all modules and 96 JUnit tests;
  artifact inspection confirmed 0.1.4 filenames and embedded Fabric/NeoForge
  metadata. No gameplay behavior changed from 0.1.3.
- 2026-07-15 / Release 0.1.5 — `./gradlew clean build` passed common, Fabric,
  and NeoForge with 97 JUnit tests. Regression coverage proves different low
  natural floors no longer collapse to one height, seeded relief changes the
  shaped target, the outer transition rounds tiny lifts away, and legacy plan
  behavior/persistence remains available. Artifact inspection confirmed 0.1.5
  metadata and all relief/profile classes in both loader jars. Javadocs and
  `git diff --check` are clean; no live test was run.
- 2026-07-15 / Coordinated-layout design — DESIGN, README, TODO, and Memory now
  distinguish current climate filtering from planned terrain composition and
  capture five layout modes, weighted mixed biomes, biome roles, starter
  overlays, seed-informed spawn constraints, and deferred flat-world controls.
  Minecraft 26.2 source review corrected the flat assumptions: arbitrary fixed
  biomes/layers and structure overrides already exist, the Overworld preset
  includes ruined portals, trial chambers are absent, and ordinary slime-chunk
  spawning requires Y below 40. `./gradlew clean build` passed all modules and
  97 JUnit tests; `git diff --check` is clean. No runtime behavior changed.
- 2026-07-15 / `WorldLayoutPlan` model design — DESIGN §17 now specifies the
  record's fields, a pure hash-grid sampler, and a weighted-argmax biome
  selection transform. An uncommitted throwaway fixture harness (Python,
  hash-based grid, 8-30 seeds, 64x64-128x128 cell samples) drove the choice:
  it showed naive `hash * weight` scoring starves low-weight biomes (a 3:2:1
  split measured ~64:31:5) while `hash ** (1/weight)` argmax reproduces target
  ratios to ~1%. It also calibrated recommended defaults
  (`regionScaleBlocks=512`, `oceanCoverageFraction=0.35`,
  `coastBlendWidthBlocks=128`) and a ±5-point coverage tolerance for 15.3's
  JUnit coverage. No runtime code changed; `./gradlew build` still passes 97
  tests.
- 2026-07-15 / `WorldLayoutPlan` implementation + release 0.1.6 — Implemented
  the pure sampler and config exactly as designed in §17: `LayoutMode`,
  `BiomeRole`, `BiomeRoles` (maintained vanilla ocean/beach mapping, unknown
  ids default to land), `WeightedBiomeListSpec` (`id`/`id@weight`, no tags —
  role resolution needs a concrete id), and `WorldLayoutPlan` (SplitMix64-based
  hash grid, `hash ** (1/weight)` argmax selection, single-nearest-boundary
  smoothstep coast blend — deliberately not perfectly radial at grid corners,
  the same simplification `ExteriorPlan`'s Chebyshev-distance square envelope
  already uses). `LayoutConfig` is wired into `WorldzConfig` with a safety net:
  if a configured mode's required role ends up with zero usable biomes after
  sanitization, sanitize logs a warning and falls back to `legacy` rather than
  letting `WorldLayoutPlan`'s constructor throw at world-creation time.
  `LayoutCodecs` persists an optional `world_layout` field on
  `LimitedBiomeSource` (mirroring `starter_land`'s round-trip-only wiring) —
  `getNoiseBiome` still ignores it until Phase 15.4. Released as 0.1.6
  (version-only bump, no other behavior change) so the installed jar is easy
  to tell apart from 0.1.5. `./gradlew clean build` passes all modules and 125
  JUnit tests (28 new); artifact inspection confirmed 0.1.6 in both loader
  filenames. Javadocs and `git diff --check` are clean.
- 2026-07-15 / Default config update — Jason hand-edited
  `config/jlt_worldz.example.yaml` to a desert/beach/river/badlands/cave biome
  mix with a `minecraft:plains` starter (radius 256, foundation depth 48).
  `WorldzConfig.java`'s actual field defaults were updated to match (per
  Jason's choice when asked how to reconcile), plus the doc strings, README
  table, and DESIGN §6 table. This is the mod's first change to its shipped
  default `allowedBiomes`/`starterBiome` since Phase 1.
- 2026-07-15 / Layout generation integration + release 0.1.7 —
  `LimitedBiomeSource.getNoiseBiome` samples `WorldLayoutPlan` for every
  non-legacy mode now (exterior ocean and starter-zone overrides still take
  precedence), and `collectPossibleBiomes()` includes resolved layout biomes.
  New pure `logic.LayoutTerrainProfile` blends a raised land floor
  (`seaLevel+2`) and capped ocean ceiling (`seaLevel-3`) by `landFactor`;
  `EnvelopedChunkGenerator` applies the delta uniformly so every
  `Heightmap.Types` query, `getBaseColumn`, and the actual placed blocks agree
  — raises fill stone from a foundation depth like starter land already does,
  lowers clear to water/air. Starter land runs as an independent second pass
  and always wins in its zone since water/air count as replaceable foundation.
  `VOID` mode is excluded from adjustment (its sky-island overlay is Phase
  15.5's job — its placeholder sample would otherwise raise the whole world
  instead of leaving it void). The fieldless preset's sampling seed is now
  chosen randomly per newly created world rather than a fixed placeholder, so
  distinct worlds get distinct layouts; tying it to the player's actual
  Minecraft seed string is deferred (no verified decode-time hook exposes it
  to a `BiomeSource` codec) alongside Phase 16's related seed-timing spike.
  Added source-scanning component tests for both files (matching this
  project's established pattern for generator-level coverage where true
  MC-bootstrapped unit tests aren't practical) plus full `LayoutTerrainProfile`
  JUnit coverage. `./gradlew clean build` passes all modules and 134 JUnit
  tests (9 new); artifact inspection confirmed 0.1.7 in both loader filenames.
  Javadocs and `git diff --check` are clean. No live test was run; Jason will
  perform acceptance testing for actual generated terrain shape.
- 2026-07-15 / Layout Customize UI + starter overlays + release 0.1.8 — Added
  `WorldzLayoutScreen` and `WorldzCustomization.LayoutSettings` (validated
  strictly like the outer record's other direct-input fields, unlike YAML's
  lenient sanitize), plumbed through a new shared `WorldLayoutPlan.resolve(...)`
  factory so YAML loading and Customize share one role-partitioning
  implementation instead of two. Customize's Done button now generates a
  fresh random seed via `customization.worldLayoutPlan(new Random().nextLong())`,
  matching the fieldless preset. Implemented three of the four remaining
  starter-overlay gaps from §17 as targeted refinements rather than a new
  overlay generator: `LAND_ONLY` uses a new `LayoutTerrainProfile.landOnlyTarget`
  (raises only clearly-deep-ocean floors, so rivers/ponds survive);
  `LimitedBiomeSource` prefers a beach-role biome
  (`WorldLayoutPlan.sampleRole`) within the starter zone's existing transition
  ring (`StarterZone.inRingQuart`); the starter-land height transition now
  blends toward the layout-adjusted floor (via the same `layoutFloorFor`
  dispatch `EnvelopedChunkGenerator` uses elsewhere) instead of raw vanilla
  terrain, so an island connects smoothly to what generation actually leaves
  beyond it. `VOID` mode forces a sky-void exterior at the starter radius plus
  transition width (256-block fallback with no starter biome), reusing the
  existing exterior-envelope mechanism rather than a new one — single-biome
  terrain needed no change. `./gradlew clean build` passes all modules and 151
  JUnit tests (17 new); artifact inspection confirmed 0.1.8 in both loader
  filenames. Javadocs and `git diff --check` are clean. No live test was run;
  Jason will perform acceptance testing, especially for the coast/beach
  blending and the sky-void island.
- 2026-07-15 / Layout integration audit + release 0.1.9 — Audited borders,
  exteriors, spawn, progression objectives, and structure eligibility against
  non-legacy layout terrain; only progression objectives had a real gap.
  Added `ObjectiveSite.isSupportiveColumn` (Overworld-only, `VOID` excluded)
  and `ObjectiveSite.supportiveFallbackZ` (tries `{0, 64, -64, 128, -128}` Z
  offsets at the existing fallback X, falling back to `0` unchanged if none
  are supportive) so the End-portal guarantee never accepts a natural
  stronghold on planned ocean or plants its compact fallback there either.
  Confirmed without code changes: borders are coordinate-only and never
  guaranteed habitable land even pre-layout; `applyLayoutAdjustment` already
  yields to a configured exterior (Phase 15.4); vanilla spawn already reads
  through the same layout-aware biome/height queries; structure eligibility
  was the core motivation Phase 15.4 already addressed (matching biome to
  terrain lets vanilla's own biome-tag eligibility do the rest). `./gradlew
  clean build` passes all modules and 157 JUnit tests (6 new); artifact
  inspection confirmed 0.1.9 and the new classes in both loader jars. Javadocs
  and `git diff --check` are clean. No live test was run; Jason will perform
  final acceptance testing across all of Phase 15.
- 2026-07-15 / Seed-informed spawn implementation + release 0.1.11 — Built
  `PREFERRED_NATURAL_BIOME`'s search and full origin recentering (see the new
  Decisions entries above for the mutable-origin and two-entry-point design).
  `./gradlew clean build` passes common, Fabric, and NeoForge, including
  Loom's mixin validation for the new `MinecraftServerMixin`; 179 JUnit/
  component tests total (13 new) covering `spawn` config parsing/sanitizing,
  `WorldzCustomization.spawnStrategy` round-trips, and source-scan checks for
  every new class and wiring point (`LimitedBiomeSource` codec/origin fields,
  `EnvelopedChunkGenerator` recentering, `SpawnOriginManager`/
  `SpawnOriginState`, both loaders' event/mixin registration, the Customize
  spawn button). The dummy-RandomState risk flagged in 16.1 remains unresolved
  and unrelated to this work (the search deliberately builds its own real
  `RandomState` rather than depending on the level's ambient one). No live
  test was run; Jason will perform in-game acceptance testing of all three
  spawn strategies, especially `PREFERRED_NATURAL_BIOME`'s search and the
  recentered border/exterior/progression placement.
- 2026-07-16 / Structure-suppression fix + release 0.1.13 — Jason's first
  real in-game session (world "Worldz13", config `09`, `mixed` layout) found
  villages floating/buried at nearly every distant location and confirmed
  ruler-straight coastlines; see the two new Known Risks/Decisions entries
  above for root cause and fix. `./gradlew clean build` passes all modules
  and 183 JUnit/component tests (3 new). The straight-coastline finding was
  deliberately logged rather than fixed, at Jason's direction. The dummy-
  RandomState risk from 16.1 remains separately unresolved. Jason will
  continue manual testing with the remaining `config/tests/` files.
- 2026-07-16 / `layout.biomes` linear-biome caveat, no release — World
  "Worldz14" (config `09`) confirmed the whole-region-cell `river` labeling
  described above via screenshot. Documentation-only fix (DESIGN §17,
  README, `config/tests/09` header/`config/tests/README.md`); no main-code
  change, so no version bump. `./gradlew clean build` still passes all
  modules and 183 JUnit/component tests (unchanged count — no test code
  touched this round, only the test fixture YAML and docs).
- 2026-07-16 / Structure-margin fix + release 0.1.14 — Same Worldz14 session
  also showed the Q.2 fix was insufficient (villages/ocean monument still
  stranded); widened `EnvelopedChunkGenerator.isNearLayoutRoleBoundary` to
  check corners expanded by a 128-block safety margin in addition to the
  original corners. `./gradlew clean build` passes all modules and 183
  JUnit/component tests (existing structure-suppression component test
  widened, no new tests added). The beach-width finding (BEACH spans the
  whole coast-blend transition, not a narrow shoreline) was logged, not
  fixed. No live test of this specific fix has been run yet; Jason will
  verify with a further `mixed`-mode test.
- 2026-07-16 / Dummy-RandomState mixin fix + release 0.1.15 — Jason's
  spectator-mode description of Worldz14's bottom-of-world terrain (near-total
  lava, mostly absent caves) confirmed the Phase 16.1 risk exactly; see the
  Decisions entry above for the full investigation, including the discovery
  that the previously-assumed fix was impossible (`NoiseBasedChunkGenerator`
  is `final`) and the mixin-based fix actually shipped. `./gradlew clean
  build` passes all modules including a full NeoForge mixin bootstrap
  (previously unconfigured in this project) and 184 JUnit/component tests (1
  new, verifying both loaders' `ChunkMapMixin` and registration). Not yet
  verified in-game; Jason will check terrain/caves near the bottom of the
  world on the next test, on both loaders if possible since this is the
  first NeoForge-side mixin this project has shipped.
- 2026-07-16 / Release 0.2.0 — First release of the 2026-07-16 challenge-
  world replan's Phase 1 (Stabilize and simplify): `MIXED`/`LAND_ONLY` grid
  layouts removed (TODO 1.2), real world seed routed into layout sampling
  (1.3), config hygiene — optional file, no legacy JSON, comment-based
  `config/jlt_worldz.example.yaml` instead of an in-file `_docs` map (1.4).
  `./gradlew clean build` passed all modules with 176 JUnit/component tests;
  artifact inspection of both loader jars confirmed `0.2.0` in filenames,
  `fabric.mod.json`, and `neoforge.mods.toml`, and confirmed no
  `MIXED`/`LAND_ONLY`-named classes remain while `ChunkMapMixin` (both
  loaders, now also resolving the layout seed), `LimitedBiomeSource`,
  `EnvelopedChunkGenerator`, both loaders' mixin config files, and the
  bundled SnakeYAML/license resources are all present as expected. TODO 1.5
  (Prism deploy Gradle task) is still open pending Jason's instance path;
  Phase 1 acceptance (1.1, 1.7) remains Jason's in-game pass.
- 2026-07-16 / Phase 2 (World types + Single-biome challenge, 0.2.2) —
  `jlt_worldz:single_biome`, the first typed world preset (GOALS 10-12),
  ships alongside the unchanged generic `jlt_worldz:worldz` preset. Design
  committed first (DESIGN §20.2's Phase 2.1 subsection); implementation
  reused the existing `jlt_worldz:limited`/`jlt_worldz:enveloped` registry
  types via one non-round-tripped codec hint field, a new `singleBiome:`
  config section, a small dedicated Customize screen/editor registered in
  both loaders, and confirmed (no code changes needed) that structures/
  caves/vanilla randomness already follow the real seed through existing
  preset-agnostic machinery. Added the per-world settings snapshot
  (`jlt_worldz-snapshot.yaml`, TODO 2.4) and a loader-neutral runtime
  mod-version resource. `./gradlew clean build` passed all modules with 193
  JUnit/component tests (up from 176 at the 0.2.0 release). Test configs
  `10`-`13` and a "Phase 2 acceptance" section in `MANUAL_TESTING.md` are
  ready for Jason's TODO 2.7 in-game pass; version bumped per-task this
  phase (0.2.1 for 2.2, 0.2.2 for 2.4) per Jason's explicit per-task-bump
  instruction for this session, rather than once at phase-end as Phase 1
  did.
- 2026-07-17 — **Performance bug found and fixed during Phase 2.7 testing
  (0.2.3).** Config 11 (`single_biome` + a starter biome — the exact
  combination Phase 1's config 08 also uses, but TODO 1.7's in-game
  acceptance was never actually done) took **122.7 seconds** just to
  prepare the spawn area (vanilla's own log timer), vs. near-instant for
  every other tested config; ongoing 230-290-tick "Can't keep up!" lag
  followed. Root cause: whenever a non-legacy layout mode *and* a starter
  biome are both active, `EnvelopedChunkGenerator`'s two separate per-column
  passes (`applyLayoutAdjustment`, `applyStarterLand`, one call each from
  both `applyCarvers` and `fillFromNoise`) each independently called
  `naturalOceanFloorHeight` — a real vanilla noise-based terrain query, not
  a cheap lookup — for the *same* column, 4× total per column across both
  generation stages. On top of that, `starterLandTargetHeight` sampled an
  expensive `Noises.SURFACE_SECONDARY` relief-noise value unconditionally,
  even for columns far outside the starter zone and its transition, where
  `StarterLandProfile.strengthAt` is provably `0.0` and the noise value
  can't affect the result at all.
  **Fix:** merged the two per-column loops into one
  (`applyTerrainAdjustments`) that computes the natural floor once and
  threads it through both adjustments (4× → 2× worst case, and the
  redundant layout-floor recompute inside the old `starterLandTargetHeight`
  is gone too); added a `strengthAt`-gated early exit that skips the relief
  noise sample entirely outside the starter zone/transition, returning the
  blend baseline directly (locked in by a new pure test,
  `resultIsIndependentOfNoiseOutsideTheStarterZoneAndTransition`, proving
  the result really is noise-independent there). `getBaseHeight` and
  `getBaseColumn` (single-point queries used by structure placement etc.)
  got the same natural-floor/layout-floor de-duplication. No behavior
  change intended — every calling context passes the same values the old
  code computed internally, just without recomputing them. Component tests
  in `ProjectMetadataTest` updated for the renamed/merged method. Not yet
  re-verified in-game; Jason will redeploy and retest config 11.
  **Scope note:** the buggy code (`applyStarterLand`/starter-land blending)
  predates Phase 2 — it's Phase 15/16-era — but was never exercised through
  a live acceptance pass until Phase 2.7 surfaced it, so it's fixed here
  rather than deferred, per the phase-gate rule that defects found during a
  phase's acceptance pass get addressed before moving on.
- 2026-07-17 — The Worldz14-class floating-fragment concern (still open,
  TODO 1.1) reproduced again during Phase 2.7 in a `single_biome` world
  (config 10, no starter biome) — a floating desert village around
  `(-280, 143, 140)`. Confirmed via code review this is **not** explained
  by single_biome's height math (a no-op `Math.max` on terrain already
  this tall) and **not** explained by the performance bug above (that
  world had no starter biome configured, so the buggy code path never
  ran). Reproduced again in `Worldz-06` (config 11, after the performance
  fix) — this time as disconnected floating *terrain* (not just a
  structure) visible right at spawn in broad daylight, ruling out a
  night-lighting illusion. Spawn itself landed at `(-504, 65, -329)`
  despite `starter_at_origin`, which should spawn at/near `(0,0)` —
  plausibly vanilla's own spawn search skipping unsafe/patchy ground near
  the origin. **Jason's TODO 1.1 bottom-of-world check on `Worldz-06`
  passed** (bedrock and terrain below Y-64 look normal, no lava sheet),
  which weakens (a single per-chunk `RandomState` object being "mostly
  fixed but still broken higher up" would be an odd failure mode) but does
  not fully rule out the dummy-RandomState theory — the passing check was
  specifically the deep-generation zone; the floating terrain is at
  surface/near-surface Y. Still unresolved; NeoForge repeat and the
  Worldz14 orange/glitchy reproduction retest (TODO 1.1's other two parts)
  remain outstanding, and no code fix has been attempted for the floating
  terrain since the root cause isn't yet identified.
- 2026-07-17 — **Decision (approved, scope for a later phase, GOALS 15):**
  Jason confirmed single_biome forcing one biome at every depth (no cave
  biomes possible — confirmed via code review: `LimitedBiomeSource
  .getNoiseBiome`'s layout branch samples `WorldLayoutPlan` using only
  `(blockX, blockZ)`, never `quartY`) is acceptable for now, but wants a
  future option to let vanilla's own cave biomes (dripstone/lush/deep dark)
  generate normally. Logged as GOALS 15 and TODO 3.3 (needs its own design
  pass — genuinely depth-aware sampling, not just the surface-family
  pass-through Phase 3.1 already plans for rivers/oceans). Not implemented
  now. Sparse (but present) caves in the shallower layers is separately
  judged likely normal vanilla cave-density falloff near the surface, not
  a defect — not investigated further absent contrary evidence.
- 2026-07-17 — **Root cause found and fixed for the floating-structure
  mystery (0.2.4).** Jason's decisive test: same seed, same position
  `(-6817, y, 5472)`, a desert village exists in both a plain vanilla world
  and a Worldz `single_biome` world (`Worldz-NF-01`, NeoForge) — vanilla
  places it at Y77 (normal); Worldz places the identical village at
  Y120-150, detached from the real terrain below. Same seed/position rules
  out coincidence and proves something in Worldz's own generator wrapping
  changes the outcome relative to true vanilla.

  Root cause: `ChunkMapMixin`'s 0.1.15 dummy-RandomState fix
  (`@Inject` at `@At("INVOKE")` on `generator.createState(...)`,
  reassigning `this.randomState`) has a bytecode-timing bug. Decompiled
  `ChunkMap`'s constructor:
  `this.chunkGeneratorState = generator.createState(registryAccess
  .lookupOrThrow(Registries.STRUCTURE_SET), this.randomState, levelSeed);`
  — `this.randomState` is read via `GETFIELD` as this call's own inline
  argument. An `@At("INVOKE")` callback fires immediately before the
  `INVOKE` instruction itself, which is *after* the argument-loading
  instructions for that same call (including that `GETFIELD`) have already
  executed — so the field reassignment always landed one instruction too
  late for this *specific* call. Every *later* read of `this.randomState`
  (actual terrain generation, via the `randomState()` accessor) correctly
  saw the fix — which is exactly why the TODO 1.1 bottom-of-world check
  passed clean while structures kept floating: bulk terrain generation was
  never affected, only the one-time `ChunkGeneratorStructureState` built
  during construction, which governs every structure-placement decision
  for the whole level and was still built from the dummy, zero-density
  `RandomState` regardless of the "fix." A degenerate density-based
  placement/height estimate for structures, combined with correctly-
  generated real terrain around it, is consistent with structures ending
  up anchored well above (or otherwise detached from) where the terrain
  actually ends up.

  **Fix:** switched both loaders' `ChunkMapMixin` from `@Inject` to
  `@Redirect` on the same `createState(...)` call — the redirect handler
  computes the correct `RandomState` (when applicable) and passes it
  explicitly to `generator.createState(...)` itself, sidestepping the
  field-read-timing issue entirely rather than depending on reassignment
  order. `ProjectMetadataTest` updated to assert the `@Redirect` shape.
  Not yet confirmed in-game — needs a fresh world on both loaders to verify
  villages no longer float. Version bump 0.2.3 → 0.2.4; deployed to both
  Prism instances (manual copy for NeoForge, no `deployToPrism`-equivalent
  Gradle task exists there yet).

  **Confirmed fixed in-game (2026-07-17, same day).** First retest showed
  improvement but not a full fix (Y140→Y80, still ~15 blocks above what
  looked like baseline) — investigated further before accepting that as
  residual, since Jason first confirmed vanilla villages are *always* flush
  (ruling out "this is just an ordinary vanilla quirk"). The real test:
  find a village in a plain vanilla world first, then check the identical
  seed/coordinates in Worldz. Across 10 villages spanning five biomes
  (desert, plains, savanna, snowy, taiga), 9 were perfectly flush with
  Worldz's copy of the same village; one plains village showed a partial
  (not whole-structure) float. The earlier "~15 blocks above Y65" reading
  is now understood as a methodology artifact, not a real gap: desert
  terrain has natural dune variance, so a village's true natural elevation
  at a given spot can legitimately be Y75-80 rather than flat sea level —
  comparing against an assumed flat baseline instead of that location's
  actual vanilla-generated height made correctly-grounded villages look
  like they were floating. The one remaining partial-float case is judged
  an isolated, low-severity per-piece anomaly (the kind unmodified vanilla
  occasionally produces on sloped terrain too), not a systematic defect —
  not investigated further absent a reason to think otherwise. **Closed.**

  **Open question this doesn't yet answer:** whether this also explains
  the remaining ~60s spawn-area-prep slowness (unclear — a degenerate
  density router isn't obviously *slower* to evaluate than a real one, so
  the performance issue may be genuinely separate; Jason is profiling with
  Spark next).
- 2026-07-17 — **Root cause found and fixed for the "Fabric spawns far from
  origin" mystery (0.2.5) — turned out not to be Fabric-specific at all.**
  Jason's diagnosis: after using ChunkBase to pick a seed with
  vanilla-favorable climate right at `(0, 0)`, both loaders spawned
  correctly with it — proving the earlier "bad on Fabric, fine on
  NeoForge" pattern was seed coincidence, not a loader bug. Confirmed
  against decompiled `MinecraftServer.setInitialSpawn`: vanilla finds the
  spawn chunk via `chunkSource.randomState().sampler().findSpawnPosition()`
  — a purely climate-based search (vanilla's own "does this look like a
  good spawn biome" heuristic) operating on the *underlying, unmodified*
  noise sampler. `LimitedBiomeSource` only overrides the *reported* biome
  (`getNoiseBiome`); it never touches the raw climate signature
  `findSpawnPosition()` actually reads. For seeds whose raw climate near
  `(0, 0)` doesn't satisfy vanilla's criteria, that search travels outward
  (up to vanilla's own ~2048-block radius) looking for a match — completely
  unaware that Worldz's `single_biome` mode had already guaranteed solid,
  correctly-biomed land right at the origin. This means `STARTER_AT_ORIGIN`
  never actually guaranteed a near-origin spawn in the first place, on
  either loader — DESIGN §18's "Strategy specification" subsection
  documented it as deferring to vanilla's search on the (untested, now
  disproven) assumption that search would naturally land inside the
  guaranteed zone; that assumption predates this session and was never
  Phase-2-introduced.

  **Fix:** `SpawnOriginManager.resolveFreshOrigin` now explicitly resolves
  and returns a safe surface spawn at `(0, 0)` for `STARTER_AT_ORIGIN` (a
  new `safeSpawnNear` helper, reusing the exact height-lookup pattern
  `PREFERRED_NATURAL_BIOME`'s found-target case already used), instead of
  returning `Optional.empty()` to defer to vanilla's climate search.
  `VANILLA_SPAWN` is unaffected — deferring to vanilla is literally its
  whole point. The three `PREFERRED_NATURAL_BIOME` fallback paths (no
  starter biome configured, no real generator, search found nothing) all
  say "using starter_at_origin instead" in their log messages, so they now
  consistently use the same explicit-spawn behavior too, rather than
  reverting to the old defer-to-vanilla behavior only fallbacks used to
  get. DESIGN §18 corrected to match. Version bump 0.2.4 → 0.2.5. Not yet
  confirmed in-game.

## API Deviations

- Minecraft 26.2 uses `net.minecraft.resources.Identifier` where the design sketch says `ResourceLocation`.
- Fabric's concrete codec registry is `BuiltInRegistries.BIOME_SOURCE`; `Registries.BIOME_SOURCE` is its registry key.
- The codec retrieves only the biome registry getter; it constructs the public vanilla overworld preset parameter list directly to avoid 26.2's parallel-load unbound holder.
- Configured tag holder sets are expanded only after tag binding, then persisted
  as a resolved direct holder list.
- Minecraft 26.2 stores codec-encoded dimension settings in
  `data/minecraft/world_gen_settings.dat`, not directly in `level.dat`.
- Loader jars use the reseed naming convention rather than a single root
  `mod-worldz` archive because Fabric and NeoForge require distinct artifacts.
- See the starter-radius resolved marker and fail-safe possible-biome decisions above.
- **`ServerLevel.getGameTime()` (`LevelData`'s classic field) is not kept in
  sync with real elapsed play time in this 26.2 snapshot.** Authoritative
  per-dimension elapsed ticks now live in a new `WorldClock` system —
  `Level.getDefaultClockTime()`, persisted as `total_ticks` per dimension
  in `data/minecraft/world_clocks.dat` — confirmed by decoding a real
  world's saved NBT directly (not just reading decompiled sources) after
  it silently broke `WorldLimitManager`'s delayed border transitions (see
  worldz/TODO.md's 2026-07-18 Deviation log entry for the full
  diagnosis). Any future code that needs "how much time has actually
  passed in this dimension" must use `getDefaultClockTime()`, not
  `getGameTime()` — the generated/decompiled sources still document
  `getGameTime()` as if it were authoritative, so this is easy to miss on
  a read-only source review; it only surfaced by decoding the actual
  persisted save files of a world that had visibly stopped behaving
  correctly.
- **26.2's `/time` command operates on the WorldClock system, not just the
  daylight cycle** (follow-up to the entry above; Jason noticed in-game,
  verified in `TimeCommand` 2026-07-18): `/time add`/`/time set` without an
  `of` clause resolve the *current dimension's* default clock and call
  `ClockManager.addTicks`/`setTotalTicks` on it — the exact counter
  `getDefaultClockTime()` reads, so `/time add 5d` legitimately triggers a
  pending border-schedule delay (the daylight cycle is now a Timeline
  *derived* from that clock; the old "only changes daylight" intuition is
  pre-26.x). Nuance for test instructions: jumping the clock does **not**
  fast-forward an in-progress continuous border resize — vanilla's
  `MovingBorderExtent` counts down per real tick, independent of any clock
  — so `/time add` to expire a delay, `/tick step` to watch movement.
  (A Phase 5b *stepped* schedule, being a pure function of the clock,
  *would* fast-forward under `/time add`.) `/time` also gained
  `pause`/`resume`/`rate` and per-clock `of <clock>` targeting in 26.2 —
  potentially useful for future test recipes.
