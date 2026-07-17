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
