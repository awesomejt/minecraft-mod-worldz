# jlt_worldz — Implementation Memory

Durable decisions, verified API notes, and rationale that should survive across implementation sessions.

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
