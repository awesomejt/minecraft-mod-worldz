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
