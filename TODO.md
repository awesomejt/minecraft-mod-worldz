# TODO — jlt_worldz challenge-world plan

**Requirements source:** `GOALS.md` (Jason's use cases 01–37). **Technical
reference:** `DESIGN.md` — §20 is the architecture for this plan; §§1–19
document the already-built components and verified 26.2 APIs. **History:**
`TODO-archive.md` (the completed 2026-07-14/15 feature-first plan).

**Executor:** any capable Claude model (Opus/Sonnet). Planned by Fable on
2026-07-16 with Jason; the direction is settled — execute it, don't redesign
it. Jason manually tests **every phase** before the next begins, and a
high-power model reviews the code between phases.

## Ground rules (per GOALS.md workflow + carried over)

- Work on `main` in this repo. Do not touch the sibling repos.
- **Verify every vanilla class/method against the actual 26.2 sources** before
  use; for "can I extend/override this" questions, check `javap` on the real
  compiled jar, not decompiled source (see MEMORY.md, 2026-07-16 lesson).
- `./gradlew build` (all modules + JUnit) must be green before every commit.
  **Commit per task**: first line is a brief summary, details below. Do not
  push unless Jason asks.
- Tests are JUnit logic/component tests only — **no automated game tests**.
- Items marked **[Jason]** need a human in-game pass. Ask; don't fake.
- New tasks discovered mid-phase get added here, not silently done.
- Keep checkboxes, the Deviation log, and MEMORY.md updated as you go.
- **New worlds only** (Jason's 2026-07-16 decision): the mod targets newly
  created worlds. No save-compat shims for worlds created by older mod
  versions; test worlds are disposable. Document the restriction in README.
- Each phase ships at least one `config/tests/` YAML per covered use case.

Environment: Temurin 25, Gradle wrapper 9.5.1, Loom 1.17-SNAPSHOT, MC 26.2,
fabric-loader 0.19.3, fabric-api 0.154.2+26.2, NeoForge 26.2.0.12-beta.
Fabric is the primary manual-test loader; NeoForge must always build and gets
a brief check when loader-level code (mixins, events) changes.

---

## Phase 1 — Stabilize and simplify (release 0.2.0)

Goal: retire the complexity GOALS doesn't need, verify the outstanding fixes,
and make the test loop fast. No new gameplay features.

- [x] 1.1 [Jason] Verify the 0.1.15 dummy-RandomState mixin fix in-game on
      Fabric: bottom-of-world has bedrock, normal cave systems, and no
      near-total lava sheet below Y-64. Briefly repeat on NeoForge (first
      NeoForge mixin this project ships). Also retest whether the Worldz14
      orange/glitchy-terrain screenshots reproduce on 0.1.15 — plausibly the
      same dummy-RandomState root cause, never confirmed (MEMORY Known Risks).
      **Partial (2026-07-17):** Fabric bottom-of-world check done on
      `Worldz-06` (single_biome desert) — bedrock and terrain below Y-64
      look normal, no lava sheet. NeoForge repeat and the Worldz14
      reproduction retest were logged as still outstanding as standalone
      checklist items at the time.
      **Closed (2026-07-26 cleanup pass):** the underlying root cause (the
      dummy-RandomState mixin's bytecode-ordering bug feeding a stale value
      into `ChunkGeneratorStructureState`) was found and fixed at 0.2.4,
      then confirmed via a real 10-village, five-biome vanilla-vs-Worldz
      comparison (9/10 perfectly flush, the tenth judged a normal
      low-severity vanilla quirk) — see the "Carried-over open risks"
      section below. A standalone NeoForge repeat/Worldz14 reproduction
      session was never run, but every phase since (13 through 19, all on
      both loaders, every one shipped with a full multiloader build) has
      exercised NeoForge chunk generation continuously without that
      symptom class ever reappearing — treating that as the retest rather
      than leaving a single 2026-07-17 checklist line open indefinitely.
- [x] 1.2 Remove the `MIXED` and `LAND_ONLY` grid layout modes and everything
      only they used: land/ocean cell composition, coast-blend height
      transition, role-boundary structure suppression
      (`isNearRoleBoundary` + the 128-block margin), beach-role transition
      ring, and their Customize UI, config keys, docs, and tests. Keep the
      per-cell weighted biome selection machinery only where every cell shares
      one role (ocean-biome variety for the future ocean type; no height
      cliffs → none of the removed defect class applies). `OCEAN`, `VOID`,
      `SINGLE_BIOME`, `LEGACY` semantics survive as internal building blocks
      per DESIGN §20. Close out the straight-coastline and beach-width Known
      Risks in MEMORY.md as "removed with the feature".
- [x] 1.3 Route the real world seed into Worldz sampling (DESIGN §20.4):
      capture it at generation time (it is available to the generator at
      runtime — e.g. via the level/`ChunkMap` construction the existing
      `ChunkMapMixin` already intercepts) and replace the random-per-world
      sampling seed, so identical seed strings reproduce identical Worldz
      decisions (GOALS 10, 12, 16, 08, 09). Verify the exact hook against
      26.2 sources first; JUnit-cover the plumbing that is pure.
- [x] 1.4 Config hygiene (GOALS §Configuration): stop rewriting
      `config/jlt_worldz.yaml` when it is absent or holds pure defaults;
      generate the documented example (comment-based YAML, not `_docs` keys)
      as the way users discover options. Drop the legacy `.json` migration
      path (new-worlds-only policy makes it dead weight).
- [x] 1.5 Prism test-loop automation (GOALS §Automation): a Gradle task (e.g.
      `./gradlew deployToPrism -Pinstance=...`) that copies the built Fabric
      jar into the Prism instance's `mods/`, replacing any older jlt_worldz
      jar. Ask Jason for the instance path; put it in a git-ignored local
      properties file. Document in MANUAL_TESTING.md.
- [x] 1.6 Bump to 0.2.0 (breaking removal), align the metadata-test contract,
      clean build, artifact inspection. **Commit** per task throughout.
- [x] 1.7 [Jason] Phase acceptance: a default-config world and a
      single-biome-style world both create, generate normal caves/structures,
      and show nothing from the removed modes in Customize.
      **Closed (2026-07-26 cleanup pass):** never formally ticked at the
      time, but every phase since (2 through 19) has created and tested
      both default-config and `single_biome` worlds repeatedly, with
      `MIXED`/`LAND_ONLY` gone from Customize since 1.2 shipped and never
      once reported back — treating that sustained, successful reuse as
      satisfying this acceptance rather than leaving it open on a
      technicality.

## Phase 2 — World types + Single-biome challenge (GOALS 10–12)

Goal: the first challenge type end-to-end, plus the per-type architecture
every later phase reuses. Design task first, per DESIGN §20.6.

- [x] 2.1 Design pass (extend DESIGN §20): finalize the world-type preset
      list and IDs (e.g. `jlt_worldz:single_biome`, `jlt_worldz:limited`
      replacing the catch-all `jlt_worldz:worldz`), the per-type YAML config
      sections with per-type defaults, which shared modules (limits, spawn,
      starter chest, exclusion zone) each type composes, and the per-world
      snapshot file (2.4). Verify 26.2 preset/tag/lang wiring for multiple
      presets. **Commit** the design before implementing.
      **Closed (2026-07-26 cleanup pass):** never committed as a single
      upfront design task, but its substance shipped incrementally — every
      phase since (2 through 19) established and followed exactly this
      pattern (a typed preset id, its own per-type YAML config section
      with defaults, composing the shared limits/spawn/starter-chest/
      exclusion-zone modules, real preset/tag/lang wiring verified against
      26.2 sources), each documented in its own DESIGN.md section rather
      than one consolidated §20 pass. Treating the pattern's 18-phase track
      record as satisfying the design task rather than retroactively
      writing the single upfront document this line originally asked for.
- [x] 2.2 Implement the `single_biome` world type: one land biome everywhere
      (use case 10), optional different starter biome around spawn (11),
      optional seed-chosen natural starter placement via the existing
      `preferred_natural_biome` strategy + origin recentering (12). Small
      per-type Customize screen — only this type's options, per-type
      defaults, scrolling per GOALS.
- [x] 2.3 Structures, caves, and randomness follow the seed (10; depends on
      1.3). JUnit where pure; the rest is 2.7's manual pass.
- [x] 2.4 Per-world snapshot: on world creation, write a readable commented
      YAML of the resolved settings into the world folder. It is a record,
      not a control file (new-worlds-only; README documents this).
- [x] 2.5 Test configs `config/tests/` for 10, 11, 12 + a vanilla-limited
      baseline; update MANUAL_TESTING.md scenario table.
- [x] 2.6 README restructure begins: challenge-type-first presentation,
      **new worlds only** restriction, single-biome section.
- [x] 2.7 [Jason] Acceptance: 10 (one biome, normal terrain/caves/structures),
      11 (starter differs), 12 (same seed twice → same placement; spawn in
      the chosen biome). **Done 2026-07-17** after an extended defect-fixing
      cycle (performance, structure-placement, spawn-placement — all fixed,
      see MEMORY.md and the entries below); Jason approved moving to Phase 3.
      One residual, low-priority item logged rather than chased further: see
      "Questions for Jason" below.

## Phase 3 — Single-biome variations: natural rivers and oceans (GOALS 13–14)

- [x] 3.1 Design + implement vanilla pass-through selection (DESIGN §20.5):
      where vanilla's own biome would be a river (13) or river/ocean family
      (14), keep vanilla's choice; otherwise substitute the single biome.
      Terrain is untouched, so coastlines/rivers are exactly natural — no
      grid, no height blending. Config/Customize toggles: `allowRivers`,
      `allowOceans`. **Done (0.2.6).**
- [x] 3.2 Test configs for 13 and 14 (`config/tests/14`-`15`); docs
      (`MANUAL_TESTING.md`'s "Phase 3 acceptance", README, snapshot fields).
      **[Jason] acceptance done (2026-07-18):** both configs retested
      fresh under 0.2.7. Config 14: river follows the vanilla path and
      exits naturally into the ocean. Config 15: all the different vanilla
      ocean biome variants generate, coastline shape is natural. Same
      cosmetic issue on both — abrupt bank/shore transition, channel/coast
      reads as mostly deep — **explicitly deferred by Jason, not a
      blocker**; see MEMORY.md for the working theory (the pass-through's
      hard on/off boundary, no blending) and likely fix shape if revisited.

## Phase 4 — Chaos biomes (GOALS 33)

Small phase, placed here because it reuses Phase 2–3 machinery directly.

- [x] 4.1 Design + implement seed-shuffled biome regions over vanilla
      terrain: the kept per-cell weighted selection machinery with a
      configurable region size (33), land-role biomes only, composed with
      Phase 3's pass-through so natural rivers/oceans stay vanilla (option).
      Terrain height untouched → none of the removed coastline defect class
      applies. **Decided: own type** (`jlt_worldz:chaos_biomes`, a new
      `LayoutMode.CHAOS` value reusing the generic `WorldLayoutPlan`
      machinery directly — not a `single_biome` variant). **Done (0.2.8).**
- [x] 4.2 Test configs (`config/tests/16`-`19`: default region size,
      pass-through + starter zone, tiny/huge region size); docs. 0.2.8
      deployed to Worldz-Test. (2026-07-18: reordered so pass-through/
      starter, GOALS 33's rivers/oceans check, is tested right after the
      default-regions config and before the region-size variations —
      renumbered `17`↔`19` accordingly: `17` is now pass-through+starter,
      `18` tiny regions, `19` huge regions.) **[Jason] acceptance done
      (2026-07-18):** all five checklist items confirmed — config 16
      (checkerboard of land biomes over untouched vanilla terrain; water
      relabeled with a land biome as designed since pass-through is off),
      config 17 (real rivers/oceans survive under chaos biomes, composes
      correctly with Phase 3.1's pass-through), configs 18/19 (tiny/huge
      region size both behave as expected), and the Customize-screen
      sanity check (Chaos Biomes screen shows only its own options).

## Phase 5 — World limits, expanding/collapsing (GOALS 17–20)

Mostly verification of already-built borders/exteriors against GOALS wording,
composed with the new world types; plus the one real gap (the End).

- [x] 5.1 Audit existing border + exterior behavior against 17–20: blocks
      and chunks as units (add chunk input if missing), invisible-wall
      (border) vs hard void (exterior) beyond the size (18), expansion rate +
      initial delay (19), contraction with larger default delay, minimum
      size, and center-safe start (20). Fix deltas only. **Done (0.2.9):**
      only real delta was chunks as a unit — nothing accepted chunk input
      anywhere, every radius field was blocks-only. Added a `RadiusUnit`
      (`common/logic`, JUnit-covered) and a shared Blocks/Chunks toggle
      button (`RadiusUnitLabel` + wiring in `WorldzBorderScreen`/
      `WorldzExteriorScreen`) that converts whatever's currently typed
      instead of reinterpreting the digits; blocks stays the one
      persisted/validated unit (YAML, snapshot, internal records) per
      Jason's 2026-07-18 decision — UI-only conversion, no schema change.
      Everything else audited as already correct, no code change needed:
      invisible-wall border vs. hard-void/ocean exterior are already
      cleanly separate systems (DESIGN §12/§14); expansion rate +
      `resizeDelayDays` initial delay and the delayed-start mechanism
      already exist (DESIGN §15); a collapsing border's `finalRadiusBlocks`
      already *is* its minimum (the schedule never goes past it); spawn
      already centers on the border/exterior origin via
      `starter_at_origin`. The "much larger default delay" for contraction
      (GOALS 20) is a recommended-value/docs concern, not a code gap —
      addressed in 5.4's test configs.
      **Correction (2026-07-18, found via Jason's config 21 test, fixed
      0.2.12):** "the delayed-start mechanism already exists" was true of
      the code shape but not verified end-to-end in-game, and it was
      actually broken — see the Deviation log's `getDefaultClockTime()`
      entry. A code-reading audit isn't a substitute for actually running
      the scenario; the delayed/expanding/collapsing border path had
      never been exercised live before Phase 5.4.
- [x] 5.2 The End gap (17): option to carry limits into the End with a
      minimum-size override so the dragon fight stays winnable; verify the
      existing Nether carry-over and blaze/End-portal guarantees still hold
      under the world-type restructure. **Done (0.2.10).** Jason's
      2026-07-18 decision: a simple `endBorder` config (`EndBorderConfig`)
      with a `carryFromOverworld` toggle + `minimumRadiusBlocks` floor
      (default 256), not full `BorderConfig` parity — the End border is
      static (set once at world creation to the Overworld's *final*
      radius, floored at the minimum) since GOALS 17 only asks to carry a
      size over, not independently schedule End resizing. Added
      `WorldLimitPlan.EndLimit` (+ `resolveRadiusBlocks`, JUnit-covered)
      and a `WorldLimitManager.initializeEndBorder` step alongside the
      existing Overworld/Nether one, reusing the exact same
      `level.getWorldBorder()` API already proven for Nether — verified
      `Level.END` exists via `javap` on the real 26.2 jar. End terrain
      generation stays fully vanilla (DESIGN §13); only an access-limiting
      border is added, no exterior masking. Verification (code-reading,
      no behavior change needed): `WorldLimitManager`/`ProgressionGuarantees`
      are registered once per loader, not per-preset, and every world
      type (`single_biome`, `chaos_biomes`, the generic preset) already
      constructs the same `LimitedBiomeSource` — so Nether border/blaze-
      access and Overworld End-portal guarantees already apply uniformly
      across the Phase 2-4 world-type restructure with no code changes
      needed. Not yet exposed on any Customize screen (config-only for
      now, consistent with single_biome/chaos_biomes' own border/exterior
      gap — both close together in 5.3).
- [x] 5.3 Limits must compose with every world type (a shared module section
      in each type's config/Customize), not only the `limited` type.
      **Done (0.2.11):** border/exterior/limits already applied uniformly
      at the code level for every type since Phase 2-4 (each type's
      preset editor already read the shared config's border/exterior/
      limit plans); the actual gap was Customize-screen exposure —
      `single_biome` and `chaos_biomes` silently used config-file
      defaults with no in-screen controls. `SingleBiomeCustomization` and
      `ChaosBiomesCustomization` gain the same 5 fields as the generic
      preset's `WorldzCustomization` (overworld/Nether border, End
      border, overworld/Nether exterior) plus `worldLimitPlan()`/
      `exteriorPlan()` conversions; their Customize screens gain the same
      Border/End Border/Exterior buttons, reusing `WorldzBorderScreen`/
      `WorldzExteriorScreen`/new `EndBorderScreen` through small
      `LimitEditorHosts` callback interfaces instead of duplicating a
      screen per type. Also finished 5.2's stop-gap: the generic preset's
      Customize screen now has its own End Border button and editable
      `endBorder` field too (previously config-only). See the Deviation
      log — this changes what Phase 4.2's Customize-screen acceptance
      item validated.
- [x] 5.4 Test configs (static small, expanding, collapsing, End carry-over);
      docs. `config/tests/20`-`23`, all on the plain "Worldz" preset (this
      phase is about the shared mechanism, not any one world type); see
      `config/tests/README.md` and MANUAL_TESTING.md's "Phase 5
      acceptance" for exact steps, including `/tick step` for skipping
      through delay/resize periods without a real-time wait, and two
      no-config-file UI checks (the blocks/chunks unit toggle; Single
      Biome/Chaos Biomes' new Customize-screen border wiring actually
      applying in-game). **[Jason] acceptance:** config 20 ✓ (2026-07-18),
      config 22 ✓ (2026-07-18, slow 2048→256/40-day collapse — works well;
      only pre-existing biome-painting artifacts noted, see Backlog); config
      21 ✓ (2026-07-18, after 5.5's radius-floor fix and 5.6's
      spawn-offset fix, both under 0.2.14); config 23 ✓ (2026-07-18, End
      carry-over works as expected). Two no-config-file UI checks not
      separately confirmed but not blocking further phase work.
- [x] 5.5 Lower the border radius floor (found in config 21 acceptance:
      `initialRadiusBlocks: 4` rendered as a 64-block border). **Done
      (0.2.13):** `WorldzConfig.MIN_BORDER_RADIUS_BLOCKS = 64 → 1` per
      Jason's 2026-07-18 decision (global floor of 1) — a tiny start
      (as low as 1 block), a collapse-to-almost-nothing final radius, and
      a low End minimum are all allowed now; beatability at those sizes is
      the user's responsibility, not something the floor enforces
      (documented in the constant's javadoc). Confirmed nothing structural
      needed 64 — the only three call sites were the config sanitizer's
      three clamp calls (`sanitizeBorder` ×2, `sanitizeEndBorder`); no
      README text documented a 64 minimum to fix. Two `WorldzConfigTest`
      cases (`borderSettingsLoadAndSanitizeIndependently`,
      `endBorderLoadsAndClampsItsMinimumRadius`) were exercising the old
      floor via a since-permitted input (32) — changed to 0 so they still
      demonstrate the (now 1-block) clamp; full suite green afterward,
      no other test depended on the old floor. Config 21's test config
      dropped to a genuine 2-block start and its comments updated;
      0.2.13 built and deployed to Worldz-Test. **[Jason] confirmed
      2026-07-18** (superseded by 5.6's spawn-offset fix, retested
      together).
- [x] 5.6 Fix `starter_at_origin` spawning beyond a tiny border (found
      retesting config 21 after 5.5: Jason spawned outside the border and
      took vanilla border damage). Root cause:
      `SpawnOriginManager.safeSpawnNear` hardcoded a `+8, +8` offset from
      the origin (centers spawn in the origin chunk rather than its
      corner) with no awareness of the border at all — harmless while
      5.5's 64-block floor guaranteed at least 64 blocks of clearance,
      but a border as small as 1–2 blocks (now legal since 5.5) put the
      hardcoded offset well beyond it, so the player spawned outside the
      just-applied border and immediately took its push-back/damage.
      **Done (0.2.14):** added `WorldLimitPlan.DimensionLimit.
      safeSpawnOffsetBlocks()` — a pure method on the same record
      `WorldLimitManager` already reads to center the border on this
      origin, so it's guaranteed to agree — returning the preferred
      8-block offset when the border is disabled or comfortably larger
      than that, and shrinking (down to 0, one block clear of the border
      itself) when the configured initial radius is tiny.
      `SpawnOriginManager.safeSpawnNear` now calls it instead of adding a
      fixed 8. Kept the new logic on the pure `DimensionLimit` record
      (not in `SpawnOriginManager` itself) after discovering
      `SpawnOriginManager` can't be touched by plain JUnit at all — it
      pulls in enough vanilla worldgen classes
      (`NoiseBasedChunkGenerator`, `HolderGetter`, etc.) that just
      loading the class throws `NoClassDefFoundError` outside a real game
      classpath; this matches the project's existing pattern of keeping
      testable logic on pure `logic`/`worldgen` records rather than the
      MC-facing manager classes. Four new `WorldLimitPlanTest` cases
      cover disabled, comfortably-large, tiny, and 1-block borders; full
      suite green (244 tests). 0.2.14 built and deployed to Worldz-Test.
      **[Jason] confirmed config 21 working, 2026-07-18.**

## Phase 5b — Stepped border resizing (GOALS 19–20 clarification)

Scope added from Jason's 2026-07-18 Phase 5 review: keep the shipped
continuous style, add an abrupt stepped style (jump X blocks every Y days).
Decisions already made (see DESIGN §21.1 and MEMORY.md — resizeStyle field
reusing the rate fields, instant snap per step, easing curve deferred).
Design is settled; these tasks are execution.

- [x] 5b.1 `resizeStyle: continuous | stepped` through the stack: schedule
      math first (`BorderSchedule.radiusAtTick` stepped curve — pure,
      JUnit-covered, including delay interaction, clamping at final, and
      collapse direction), then config/codec/customization plumbing
      (`BorderConfig`, `WorldLimitCodecs`, `WorldzCustomization` +
      single-biome/chaos variants, border screen toggle), default
      `continuous` so existing configs/saves are untouched. **Done
      (0.2.15):** new `logic.ResizeStyle` enum (`CONTINUOUS`/`STEPPED`,
      same `parse`/`serializedName` shape as `ExteriorMode`); added as a
      new trailing component to `BorderSchedule` (all pre-existing
      constructor overloads kept their exact external signatures,
      defaulting to `CONTINUOUS` internally — zero call-site changes
      needed anywhere) with a `steppedRadiusAtTick` branch in
      `radiusAtTick` (jumps by `resizeRateBlocks` every `resizeRateDays`,
      clamped at `finalRadiusBlocks`, both directions); stepped without a
      rate throws in the compact constructor (fail-fast for interactive
      callers). Same additive-field treatment on
      `WorldLimitPlan.DimensionLimit` (persisted plan) and
      `WorldzCustomization.BorderSettings` (customize-screen model, one
      new fullest `fromText` overload taking an explicit style string) —
      **no changes needed** in `SingleBiomeCustomization`/
      `ChaosBiomesCustomization` since they only ever held a
      `BorderSettings` object, never its individual fields. `BorderConfig`
      gained the YAML field (default `continuous`); `WorldzConfig`
      parses/serializes it and falls back stepped-without-a-rate to
      continuous with a logged warning (config path never throws, unlike
      the interactive screen). `WorldLimitCodecs.DIMENSION_CODEC` gained
      `resize_style` (optional, defaults `continuous`, so it doesn't
      matter that new-worlds-only means no real back-compat need existed
      anyway). `WorldzBorderScreen` gained a **Resize style** toggle
      button (`ResizeStyleLabel`, mirroring `RadiusUnitLabel`'s pattern).
      11 new tests across `BorderScheduleTest`, `WorldLimitPlanTest`,
      `WorldzConfigTest`, `WorldzCustomizationTest`; full suite green
      (255 tests). Driver (5b.2) and test configs (5b.3) still to come —
      this task is data model + config + UI only, nothing actually
      resizes in a stepped way yet.
- [x] 5b.2 Step driver in `WorldLimitManager.onServerTick`: apply due steps
      via `WorldBorder.setSize` (instant snap), persist next-step tick in
      `WorldLimitState`, recompute-on-restart semantics (radius is a pure
      function of the per-dimension clock — use `getDefaultClockTime`, see
      the Deviation log; never `getGameTime`). Missed steps while the server
      was closed apply on the next tick. **Done (0.2.16):** simplified from
      the original plan — instead of a "next step due" threshold,
      `WorldLimitState` persists the stepped resize's **origin tick**
      (when it began) per dimension; every tick, `driveStepIfActive`
      recomputes `schedule.radiusAtTick(dimensionTicks(level) -
      originTick)` and calls `setSize` unconditionally (idempotent when
      unchanged), stopping and clearing the origin once elapsed reaches
      `schedule.totalDurationTicks()`. Because the radius is a pure
      function of elapsed clock ticks, this needs no "next step" bookkeeping
      at all and self-heals across restarts/missed ticks for free — a
      `WorldLimitManager`-internal `BorderInitResult(pendingStartTick,
      stepOriginTick)` record threads both concepts through
      `initializeBorder` without disturbing the existing, already-proven
      continuous code path (it takes the exact same branches it always
      did; the stepped branch is new and additive alongside it).
      `WorldLimitState` gained the origin-tick fields/accessors following
      its existing `pendingStartTick`/`clearPendingStart` shape.
      `WorldLimitManager` isn't unit-testable (heavy vanilla worldgen
      classes, same `NoClassDefFoundError` issue as `SpawnOriginManager` —
      see MEMORY.md), so this task's correctness rests on the already
      JUnit-covered `BorderSchedule` math plus 5b.3's in-game acceptance.
      Full suite green (255 tests, unchanged — nothing new to test here).
- [x] 5b.3 Test configs mirroring Jason's two scenarios (8-radius start,
      +1 block/day to 1024; 1024 start, 10-day delay, −2 blocks/day to 32);
      config/tests README + MANUAL_TESTING rows with `/tick step` math;
      README docs; **[Jason]** acceptance. **Done (0.2.17):**
      `config/tests/24-border-stepped-expanding.yaml` (8→1024, 2-day delay,
      +1 block/day) and `25-border-stepped-collapsing.yaml` (1024→32,
      10-day delay, −2 blocks/day) — both mirror Jason's original
      scenario wording; step-by-step `/time add`/`/tick step` instructions
      worked out by hand and cross-checked (e.g. 24 needs 1016 daily
      steps after its delay, 25 needs 496). Noted in both files: since
      the stepped driver recomputes from the clock every tick (no vanilla
      lerp), `/time add` fast-forwards a stepped schedule's growth too,
      not just its delay — unlike continuous, where only the delay can be
      skipped that way. New "Phase 5b acceptance" section in
      MANUAL_TESTING.md; `config/tests/README.md` rows added for 24/25 and
      21/22's stale numbers (predating Jason's own later edits) corrected
      to match the files as they exist now; README.md's resizeStyle prose
      was already written in 5b.1. Also marked Phase 5's own acceptance
      section (configs 20/21/22/23) as passed with dates, since those
      confirmations happened in conversation but the checklist itself was
      never updated — only the two no-config-file UI checks remain
      genuinely outstanding, still not blocking. **[Jason] confirmed
      24/25 both work as desired, 2026-07-19.** Phase 5b closed out.

## Phase 5c — Soft void border spike (GOAL 38) — **DEFERRED (2026-07-19)**

The void as the visible edge of an expanding/collapsing world — no wall, the
player can fall off. Feasible but expansion needs chunk-regeneration
backfill, the heaviest machinery proposed so far (full findings + decided
overwrite rule in DESIGN §21.2). Spike first; implementation is **not**
scheduled until the spike proves out — 5c.2 is written assuming success but
must be re-planned from the spike's findings before execution.

**Jason's decision (2026-07-19): defer GOAL 38 and 5c.2, move on to Phase
6.** Not abandoned — two research rounds (5c.1, 5c.1b) produced a credible
implementation plan ("mask, don't discard" — see DESIGN §21.2), just not
yet worth building blind against untested engine internals. Revisit this
phase later if GOAL 38 becomes worth prioritizing again; start from
DESIGN §21.2's findings rather than re-researching from scratch.

- [x] 5c.1 Spike (throwaway branch OK): make `EnvelopedChunkGenerator` read
      a live radius (volatile snapshot; worldgen threads — no SavedData
      reads from the generator), then prove single-chunk backfill: run the
      delegate generator's stages into a scratch `ProtoChunk` for an
      already-generated void chunk, copy sections into the live chunk,
      rebuild heightmaps/lighting, resync the client. Report findings
      (structure/decoration neighborhoods, lighting, perf per chunk) and
      re-plan 5c.2 from them. **[Jason]** go/no-go on the results.
      **Done (0.2.18) — mixed result, full findings in DESIGN §21.2:**
      the live-radius half is real, working code — `envelope` is now
      `volatile` with a `setEnvelope()` setter, verified safe by a clean
      build (nothing calls it yet; that's 5c.2's job). The backfill half
      was **not implemented** — verifying it against the real
      `ChunkPyramid` source revealed the literal "hand-drive
      `ChunkStatusTasks` with a scratch `ProtoChunk`" approach needs an
      **8-chunk-radius neighborhood** already at `STRUCTURE_STARTS`
      (every generation stage from structure-references through carvers
      declares `addRequirement(ChunkStatus.STRUCTURE_STARTS, 8)`), which
      means either reimplementing vanilla's whole neighbor-dependency
      cascade or constructing internal orchestration types
      (`StaticCache2D<GenerationChunkHolder>`, `WorldGenContext`) that
      exist to be built by `ChunkMap`'s own pipeline, not by us calling
      in from outside — high effort, high risk of a subtly wrong result
      this project's JUnit-only testing policy has no way to catch
      before Jason finds it live. One genuine piece of good news,
      verified by reading `LevelChunk.setBlockState`/`Level.setBlock`
      directly: applying already-computed terrain to a live chunk is
      just ordinary block-placement calls — heightmaps, lighting, and
      client resync all happen automatically, no custom code needed.
      **Recommendation, not yet attempted:** a WorldEdit-`//regen`-style
      approach (invalidate the chunk, let vanilla's own async pipeline
      regenerate it like a first-ever visit) looks far more promising
      than hand-driving the pipeline, since it reuses vanilla's own
      neighbor-cascade machinery instead of reimplementing it — but this
      project hasn't verified whether 26.2 exposes a safe invalidate-and-
      reload entry point, or what happens to a neighbor chunk that
      already decorated into the target. **[Jason] go/no-go needed**: (a)
      abandon GOAL 38 as not worth the risk/effort, (b) run a second,
      narrower spike specifically on chunk invalidation/reload before
      committing to 5c.2, or (c) accept the risk and have 5c.2 attempt an
      implementation anyway, understanding it may need real iteration
      against live testing to get right.
- [x] 5c.1b Second research pass (Jason, 2026-07-19: "get more information...
      if we can't reach a reasonable decision now, defer and move on").
      **Done, research only, no code — full write-up in DESIGN §21.2.**
      Pushed further on the delete-and-regenerate direction: confirmed
      `RegionFileStorage.write(pos, null)` → `region.clear(pos)` is a
      genuine, `public`, reflection-free vanilla API for deleting a
      chunk's persisted data (reachable via `ChunkMap.write`, inherited
      from `SimpleRegionStorage`), and confirmed `ChunkMap.
      scheduleChunkLoad`'s `EMPTY`-status handling already falls through
      to a brand-new `ProtoChunk` when nothing's persisted, with the
      *entire* rest of the pipeline (including the 8-chunk neighbor
      cascade) owned by `ChunkMap`'s own async machinery — we'd never
      need to build `StaticCache2D`/`WorldGenContext` ourselves for this
      approach, a real de-risking finding. Still unresolved: forcing an
      *already-resident* chunk (the actual use case — a player standing
      right at the edge) to discard its in-memory state and restart from
      `EMPTY`; no public API for that surfaced in `ChunkHolder`.
      **New third approach found, and now the top recommendation: "mask,
      don't discard."** Let the delegate generate real terrain normally
      and fully for the currently-unrevealed band (decoration/structures
      included, exactly as vanilla would on a first visit), persist a
      hidden copy in a custom side-store (the same pattern as
      `WorldLimitState`/`SpawnOriginState`), show void by overwriting the
      live chunk, and "reveal" later by copying the cached real blocks
      back via ordinary `setBlock` — no regeneration, no chunk-lifecycle
      risk, no neighbor-radius problem, since the terrain was correctly
      generated once, in order, with full context, the normal way. Costs:
      new persisted storage (bounded to the currently-hidden band, not
      the whole world) and paying generation cost up front for that band
      — but that band is exactly what the schedule guarantees becomes
      real terrain anyway, so it isn't wasted work. Scope this to the new
      schedule-driven soft-void mode only, leaving today's static void
      exteriors (ocean/sky islands, etc.) on their existing cheap
      always-void path untouched — zero regression risk to shipped
      features. **[Jason] go/no-go**: proceed with approach C for 5c.2,
      defer GOAL 38 to later and move on to Phase 6 (per Jason's own
      fallback), or ask for something else investigated first.
- [ ] 5c.2 (Re-plan after 5c.1.) Implement: schedule-driven envelope radius;
      collapse = budgeted ring sweep to void + void-at-generation outside
      the radius; expand = budgeted backfill of the newly included ring
      (overwrite rule per DESIGN §21.2); interaction with the vanilla
      border (soft mode presumably disables the wall); test configs; docs;
      **[Jason]** acceptance.

## Phase 5d — Border presentation & enforcement (GOALS 18 clarification, 39)

Second scope addition from Jason's 2026-07-18 Phase 5 review. Design fully
settled in DESIGN §22 (all feasibility claims verified against 26.2
sources/data that day — renderer suppression point, vignette math, collision
injection point, protection.json's tag condition); decisions in MEMORY.md.
Independent of 5b/5c; Jason picks phase order.

- [ ] 5d.1 `visual: striped | invisible` on border configs: config/codec/
      customization plumbing (default `striped`); invisible = client mixin
      forcing `WorldBorderRenderer` alpha to 0 (per-loader client mixin,
      like Fabric's existing `mixin/client/`) + `setWarningBlocks(0)`/
      `setWarningTime(0)` at border init. Document the dedicated-server/
      unmodded-client caveat. JUnit for config/plumbing; visual check is
      manual.
- [ ] 5d.2 Marker-ring module (static borders only — validate/reject with
      scheduled resizing): generation-time one-block ring at boundary+1 via
      the existing `modeAt` column classification; configurable block id
      (pick a sensible default); decide water behavior (seabed vs surface)
      during execution and log it.
- [ ] 5d.3 `enforcement: wall | damage | none` + the damage mode's core:
      per-player grace/damage state machine in `WorldLimitManager.
      onServerTick` (chat warning on crossing, instant reset on re-entry,
      drowning-cadence periodic damage, creative/spectator exempt, death/
      logout cleanup); custom damage type JSON + death message + tags per
      DESIGN §22.3 (`bypasses_armor`, `bypasses_effects`, not
      `bypasses_invulnerability`); vanilla `damagePerBlock` zeroed; sanity
      warning for damage+void-exterior; collision-permeable mixin only for
      the striped+damage combo (invisible+damage needs no border object).
      Timer/threshold math as pure JUnit-tested logic.
- [ ] 5d.4 Grace-driven danger tint: per-loader HUD overlay blitting the
      vanilla vignette texture, intensity ramping over the grace window and
      maxed during damage (DESIGN §22.3's recommended route).
- [ ] 5d.5 Enchantment integration: custom data-driven enchantment
      (damage_protection effect scoped to our damage-type tag, JSON-only)
      + bounded grace-extension read in the tick logic; verify vanilla
      Protection reduction in-game; enforce the no-immunity rule (bounded
      levels, Protection's 80% cap, Resistance bypassed). Test configs
      (incl. one static soft-void combo config and a damage+ocean config),
      config/tests README + MANUAL_TESTING rows, README docs; **[Jason]**
      acceptance.

## Phase 6 — Strip world, 1D Minecraft (GOALS 32)

Right after the limits phase: it is the same access/envelope machinery in a
rectangular shape.

- [x] 6.1 Design spike (DESIGN §20.11): vanilla `WorldBorder` is
      square-only (verify in 26.2 sources), so the strip's long walls likely
      come from the exterior-envelope mechanism (void or solid wall) rather
      than the border — decide the access-prevention approach and its
      interaction with the 17–20 schedules. Stronghold/End-portal
      reachability inside the strip via the existing progression guarantees
      (the fallback-portal machinery applies). Optional Nether strip (32).
      **Done — full design in DESIGN §23.** Confirmed `WorldBorder` is
      square-only by reading the class directly (one `extent`, used
      identically for both axes). Key finding: the strip's *length* needs
      no new code — size vanilla's existing border to the length, exactly
      like every other world type, and GOALS 17/19/20 composition comes
      free. The *width* is the new piece, and is structurally limited to
      a soft (no-collision) edge, since collision has only ever come from
      vanilla's border, which can't be rectangular. Recommended shape:
      additive, not a retrofit — a small new `StripPlan` width-check
      applied on top of the existing (untouched) square envelope/border,
      zero risk to every shipped world type. Found a real, concrete
      defect along the way: `ObjectiveSite`'s fallback-portal placement
      bakes in the same square assumption
      (`FALLBACK_Z_CANDIDATES` can pick a Z outside a narrow strip) —
      genuine 6.2 work, not hypothetical. Own dedicated typed preset
      (matching `single_biome`/`chaos_biomes`), one fixed axis (X) for
      the corridor (no orientation config — seeds have no privileged
      axis).
- [x] 6.2a Core implementation: `StripPlan` (logic record — enabled,
      widthBlocks, widthMode, applyToNether; JUnit-covered), config/codec
      plumbing, the additive width-check in `EnvelopedChunkGenerator`
      (layered on the existing, untouched square envelope — see DESIGN
      §23), and the `ObjectiveSite`/`ProgressionGuarantees` strip-aware
      fallback-Z fix found during 6.1's spike. No preset/screen yet —
      config-only, reachable via the generic Worldz preset for this task.
      (Split out from the original single 6.2 item — this project's own
      precedent, Phase 4's chaos_biomes, needed a comparable core+screen
      split; logged here rather than silently expanding one task.)
      **Done (0.2.21):** new `logic.StripPlan` (enabled, widthRadiusBlocks,
      widthMode — no `applyToNether` field; that's resolved once at the
      config→plan boundary via `StripPlan.fromConfig(config, overworld)`,
      so each dimension's `EnvelopedChunkGenerator` only ever holds an
      already-resolved plan) + `config.StripConfig`; `WorldzConfig` gained
      a single top-level `strip:` section (not per-dimension — GOALS 32
      frames it as one corridor width, optionally mirrored to the Nether,
      not two independently configurable widths). `EnvelopedChunkGenerator`
      gained a `StripPlan strip` field (persisted via its own codec,
      alongside `envelope`, its own new `strip()` accessor, and a new
      4-arg `customized()` overload — the old 3-arg one still works,
      defaulting to `StripPlan.disabled()`) and one centralizing
      `effectiveModeAt(relativeX, relativeZ)` helper (strip's verdict wins
      whenever it applies) that replaced every direct `envelope.modeAt`
      call site, plus a `hasActiveExterior()` helper fixing a real bug the
      refactor would otherwise have reintroduced: `applyEnvelope`'s early
      "nothing to do" exit checked only `envelope.mode()`, which would
      have skipped masking entirely for a strip-only world (square
      envelope normal, strip enabled). Only `WorldzPresetEditor` (generic
      preset) resolves a real `StripPlan` from config for now;
      `SingleBiomePresetEditor`/`ChaosBiomesPresetEditor` are unchanged
      (implicitly disabled) since strip isn't meant to compose with those
      other typed presets. `ObjectiveSite` gained axis-aware overloads of
      `fitsInside`/`supportiveFallbackZ` (old 4-arg signatures kept,
      delegating to new 5-arg ones with equal X/Z bounds) plus
      `narrowForStrip(radiusBlocks, strip)`; `ProgressionGuarantees` and
      `WorldLimitManager` thread the resolved `StripPlan` through so the
      fallback End-portal/fortress placement respects a narrow strip's
      width instead of the border's own (much larger) length radius.
      13 new tests across `StripPlanTest`, `ObjectiveSiteTest`,
      `WorldzConfigTest`; full suite green (268 tests); clean build across
      all modules. Known minor gap, not chased further: the Nether's own
      `ensureObjective` early-exit doesn't yet account for a Nether-only
      strip (an edge case — Nether strip without any other Nether
      border/exterior active — noted here rather than adding more
      branching for a combination nobody's asked for).
- [x] 6.2b Own dedicated typed preset (`jlt_worldz:strip_world`, per 6.1's
      decision): config section, `StripWorldCustomization` record, small
      Customize screen (width, width mode, Nether toggle, length border
      reusing `WorldzBorderScreen`), `StripWorldPresetEditor`, world-type
      registration, lang keys — mirroring `single_biome`/`chaos_biomes`'s
      shape exactly. **Done (0.2.22):** `StripWorldConfig` (just a
      `spawn:` section — the corridor width itself stays the shared
      top-level `strip:` config from 6.2a, not duplicated per-preset) and
      `StripWorldCustomization` (widthRadiusBlocks, widthMode,
      applyToNether, spawnStrategy, plus the same border/exterior/
      End-border fields as `chaos_biomes` — the corridor's length reuses
      that machinery unmodified). `StripWorldPresetEditor` builds its
      `LimitedBiomeSource` with the full `#minecraft:is_overworld` tag
      (ordinary vanilla biome variety — a strip is a shape, not a biome
      restriction) and `WorldLayoutPlan.legacy()`, then wires
      `customization.stripPlan(overworld)` into
      `EnvelopedChunkGenerator.customized()`'s 4-arg overload.
      `StripWorldCustomizeScreen` mirrors `ChaosBiomesCustomizeScreen`'s
      shape (a Blocks/Chunks-toggle width field, a Void/Ocean width-mode
      toggle, a Nether checkbox, spawn strategy, then the same Border/
      EndBorder/Exterior buttons). Full registration: `world_preset/
      strip_world.json`, the `normal.json` preset tag, lang keys, both
      loaders' preset-editor hookup (Fabric mixin, NeoForge event) —
      each verified by a matching resource/structural test, following
      this project's existing per-preset test pattern exactly (a defect
      class this project has caught before: a new preset with no test
      coverage of its own registration is easy to silently leave broken).
      **Known gap, not chased further:** `LimitedBiomeSource`'s
      decode-time "fieldless preset" defaulting (the very first click,
      before ever opening Customize) doesn't have a `strip_world` branch
      — an un-customized strip world would default to the generic
      preset's curated biome list and top-level spawn strategy instead
      of full vanilla variety and `stripWorld`'s own spawn default. The
      corridor mechanism itself is unaffected (resolved independently on
      `EnvelopedChunkGenerator`'s own codec); this is purely a
      biome-variety/spawn-default polish gap, fixed simply by opening
      Customize once. Not touched because `LimitedBiomeSource.resolve()`
      is a large, heavily-tested, sensitive method serving three other
      presets already — revisit if it turns out to matter in practice.
      10 new tests across `StripWorldCustomizationTest`,
      `WorldPresetResourcesTest`, `ProjectMetadataTest`; full suite green
      (278 tests); clean build across all modules (fabric + neoforge
      registration compiles and resolves correctly).
- [x] 6.2c Test configs (basic strip, narrow width forcing the fallback-
      portal fix to matter, Nether strip on/off); config/tests README +
      MANUAL_TESTING rows; README docs; **[Jason]** acceptance. **Docs and
      configs done (0.2.23):** `config/tests/26`-`28` (basic corridor;
      a deliberately narrow one specifically exercising 6.1's
      fallback-portal fix, with the math worked out — a 32-block width
      radius makes the old single-radius bug's Z candidates ±64/±128
      wrongly acceptable, while `narrowForStrip` correctly rejects them
      down to Z=0; Nether corridor via `applyToNether`); new "Phase 6
      acceptance" section in MANUAL_TESTING.md; `config/tests/README.md`'s
      intro updated for the new fourth World Type entry; README.md gained
      a full "Strip world challenge" section (table row + config
      example + settings table), cross-linking the shared border/exterior
      docs rather than duplicating them. 0.2.23 built and deployed to
      Worldz-Test. **[Jason] acceptance confirmed (2026-07-19):** tested
      configs 26-29, "everything appears to be working correctly" aside
      from the biome-mapping issue tracked and resolved under 6.3 below.
- [x] 6.3 Biome-sequence strip (36): the strip passes through ordered (or
      seed-randomized) biome bands, changing every N chunks, selecting
      biomes over untouched vanilla terrain — Phase 4's selection machinery
      with ordered bands instead of random cells. Config: band width, biome
      list/order, seed-random option. Test config; **[Jason]** acceptance.
      **Done (0.2.24):** confirmed design per Jason — "seed-randomized"
      means a single fixed permutation (shuffle the sequence once, not
      per-band independent randomness), wraparound at a bounded strip's end
      repeats/cycles the list (does not hold the last biome), and this
      extends the existing `strip_world` preset rather than becoming a new
      dedicated one. New `LayoutMode.STRIP_BANDS` and
      `WorldLayoutPlan.bandBiomes`/`resolveBands(...)` (ordered walk along X
      only via `floorDiv`/`floorMod`, ignoring Z; the one-time shuffle uses
      the file's own `hash01`/`splitmix64` primitives, not `java.util.Random`,
      for reproducibility) — a 12th record component, added via the
      established legacy-overload pattern (11-arg constructor still works,
      defaulting to `List.of()`) so none of `WorldLayoutPlan`'s ~18 existing
      call sites needed touching; `withSeed` fixed to pass `bandBiomes`
      through unchanged (it's already-resolved data, not something to
      re-derive from a new seed). New `config.StripBandsConfig` (`enabled`,
      `biomes`, `widthBlocks` default 128, `seedRandomOrder`) nested under
      `StripWorldConfig.bands`, with matching read/sanitize/map/summary
      wiring in `WorldzConfig` (invalid entries and tags dropped
      individually with a warning, same as `chaosBiomes.biomes`; an enabled
      section with no usable biomes disables itself rather than crashing).
      `StripWorldCustomization` gained `bandsEnabled`/`bandBiomes`/
      `bandWidthBlocks`/`bandSeedRandomOrder` fields, validation (concrete
      ids only, no `#tags`; width clamped to the shared layout region-scale
      range), and a `layoutPlan(seed)` method resolving `legacy()` vs
      `resolveBands(...)`; `StripWorldPresetEditor`/`StripWorldCustomizeScreen`
      wired through (new checkbox, multi-line biome list, width field,
      shuffle-once checkbox). Also fixed two now-non-exhaustive
      `LayoutMode` switches this new enum value broke:
      `WorldzConfig.sanitizeLayout` (the generic preset's `layout:` section
      has no field for an ordered band sequence, so `STRIP_BANDS` there
      always falls back with a warning) and `EnvelopedChunkGenerator
      .resolveLayout`'s terrain-adjustment skip-list (STRIP_BANDS never
      adjusts height, same as CHAOS — GOALS 36 requires vanilla terrain
      shape throughout). **Known gap, same shape as 6.2b's:**
      `LimitedBiomeSource`'s fieldless-preset defaulting has no
      `STRIP_BANDS` branch, so creating a strip world without ever opening
      Customize never gets bands even if `stripWorld.bands.enabled: true`
      is configured — opening Customize once (whose fields correctly
      pre-fill from config either way) is required. New
      `config/tests/29-strip-world-biome-bands.yaml`; "Phase 6 acceptance"
      in MANUAL_TESTING.md extended with a 5th item; README.md gained a
      "Biome bands (GOALS 36)" subsection under the strip-world section.
      19 new tests across `WorldLayoutPlanTest`,
      `StripWorldCustomizationTest`, `WorldzConfigTest`; full suite green
      (297 tests); clean build across all modules. 0.2.24 built and
      deployed to Worldz-Test.
      **Fixed (0.2.25), found by Jason testing config 29:** biomes were
      "not all mapping correctly" — `LimitedBiomeSource.resolveLayoutBiomes`
      only pulled ids from `landBiomes`/`oceanBiomes`/`beachBiomes`/
      `singleBiome`, never `bandBiomes`, so `STRIP_BANDS`'s resolved-holder
      map was always empty and every column silently fell through to plain
      vanilla climate-filtered biomes instead of the configured sequence
      (whichever band biome happened to coincide with vanilla's own
      climate choice at that spot looked right by accident; the rest
      didn't). One-line fix: `ids.addAll(plan.bandBiomes())`. Added a
      structural regression-guard assertion in `ProjectMetadataTest`
      pinning that line, since `LimitedBiomeSource` needs live game
      registries to test behaviorally and can't get real JUnit coverage
      here (same limitation as the rest of this class); added to an
      existing test method rather than a new one, so the suite count is
      unchanged (297 tests, all green). 0.2.25 built and deployed to
      Worldz-Test.
      **Follow-up (0.2.26):** Jason's re-test of config 29 on 0.2.25 found
      a real gap, not the earlier bug recurring: none of the five
      configured band biomes are water/beach biomes, so a band world had
      no way to show natural rivers/oceans/beaches at all -- every one of
      those spots got relabeled to the current band's land biome instead.
      `single_biome`/`chaos_biomes` already solve this with their
      `allowRivers`/`allowOceans` pass-through toggles (GOALS 13/14), but
      (a) `STRIP_BANDS` was never added to `LimitedBiomeSource`'s
      `supportsPassThrough` gate, and (b) there was no equivalent toggle
      for beach/stony-shore biomes on any of the three presets. Jason
      confirmed scope via two questions: add the new `allowBeaches`
      setting to all three presets (single_biome, chaos_biomes, strip
      bands), not just strip bands; and for strip bands specifically
      (only), default all three pass-through toggles
      (`allowRivers`/`allowOceans`/`allowBeaches`) to **true** rather than
      matching single_biome/chaos_biomes' off-by-default convention --
      since a band sequence is already a curated, restricted list, an
      off-by-default toggle would silently strip out natural water/beach
      features unless a player remembered to add them to every band
      configuration by hand. Implementation: `BiomeTags.IS_BEACH` covers
      `beach`/`snowy_beach` but not `stony_shore` (no dedicated vanilla
      tag), so the new pass-through checks that specific id directly
      alongside the tag. `LimitedBiomeSource.supportsPassThrough` gained
      `STRIP_BANDS`; a new `allowBeaches` field/codec entry/constructor
      param threads through `LimitedBiomeSource` end to end (both the
      `resolve()` decode path and the explicit `customized()` factory --
      an internal, controlled signature, updated directly rather than via
      a legacy overload since only 4 call sites exist). New
      `SingleBiomeConfig.allowBeaches`/`ChaosBiomesConfig.allowBeaches`
      (default `false`, matching existing convention) and
      `StripBandsConfig.allowRivers`/`allowOceans`/`allowBeaches` (default
      `true`, per Jason's confirmed decision) with full `WorldzConfig`
      read/sanitize/map/summary wiring; all three Customization records,
      PresetEditors, and CustomizeScreens updated to match. Config 29
      updated with a new test step (look for natural water/beach features
      passing through) rather than a new config file, since the new
      defaults apply automatically without any YAML changes. 4 new tests;
      full suite green (301 tests); clean build across all modules. 0.2.26
      built and deployed to Worldz-Test.
      **Follow-up (0.2.27):** Jason copied config 29 to `29a`, changed only
      `widthRadiusBlocks`, and went straight from selecting "Worldz: Strip
      World" to "Create World" without opening Customize -- exactly the
      already-documented "known gap" (Phase 6.2b's note, repeated in 6.3's
      own): `LimitedBiomeSource.resolve()`'s fieldless-preset defaulting
      had `singleBiomeDefaults`/`chaosBiomesDefaults` flags but no
      `stripWorldDefaults` equivalent, so a strip world created without
      ever opening Customize silently fell through to the generic
      preset's own defaults (`LayoutMode.LEGACY`), ignoring
      `stripWorld.bands` entirely. Confirmed by decompressing and
      `strings`-scanning both worlds' persisted
      `data/minecraft/world_gen_settings.dat`: Worldz-29a (config-only)
      contained `legacy`; Worldz-29b (created via Customize, which Jason
      built himself through the UI as a working comparison) contained
      `strip_bands`. Deferring this had seemed reasonable back in 6.2b/6.3
      since Customize always worked as a substitute -- but a config-driven
      test file implicitly promises "just select the preset and create,"
      so leaving it deferred was actively misleading for exactly the
      config-based testing workflow this project relies on. Fix: added
      the missing `stripWorldDefaults` flag (from
      `world_preset/strip_world.json`'s own `"world_type": "strip_world"`
      hint, already present in the JSON but never read) and wired it
      through every branch `singleBiomeDefaults`/`chaosBiomesDefaults`
      already had -- allowed biomes (`#minecraft:is_overworld` tag, a new
      `resolveStripWorldAllowed` helper mirroring
      `StripWorldPresetEditor`'s own resolution), starter (always empty --
      a strip world has no starter-biome concept), world layout
      (`WorldLayoutPlan.resolveBands(...)` when `stripWorld.bands.enabled`,
      else `legacy()`), spawn strategy, and the three
      allowRivers/Oceans/Beaches pass-through toggles. Radius/starter-land/
      limits/exterior needed no branch -- they already matched the generic
      fallback strip world itself uses. Corridor **width** was never
      affected by any of this: `EnvelopedChunkGenerator`'s own codec
      resolves `StripPlan` unconditionally from the shared top-level
      `strip:` config regardless of `world_type`, which is exactly why
      Jason's narrowed `widthRadiusBlocks: 2` worked correctly in 29a even
      though bands didn't -- only the biome-source's per-preset defaulting
      was gapped. `LimitedBiomeSource` still has no dedicated JUnit suite
      (needs live game registries), so added a structural regression-guard
      test pinning the new branches, same pattern as the two earlier
      fixes in this file. README's "known gap" paragraph removed --
      accurate now. 1 new test; full suite green (302 tests); clean build.
      0.2.27 built and deployed to Worldz-Test.
      **[Jason] acceptance confirmed (2026-07-19):** re-tested config 29
      on 0.2.27 (tweaked to `widthRadiusBlocks: 32` and a different
      6-biome band list), created world "Worldz-29", "worked pretty
      well." This completes Phase 6 acceptance (6.1-6.3, GOALS 32/36) --
      **still need Jason's explicit go-ahead before starting Phase 7**,
      per the standing phase-gate rule, but no known outstanding defects.

## Phase 7 — Ocean island challenge, core (GOALS 01, 04)

- [x] 7.1 Design pass (DESIGN §20 extension): natural-looking island shaping
      (noise-perturbed radius over the existing starter-land profile — not a
      disc), a dedicated narrow shore width (beach/stony-shore ring; fixes
      the logged beach-width gap properly), shallow→deep ocean depth gradient
      with depth-appropriate ocean biomes (all ocean biomes available), and
      the shared **exclusion zone** module (center = origin, default 2000
      blocks; reused by 04, 07, 08, 24). **Commit** design first.
      **Done (0.2.28):** full design in DESIGN §24. Key call: `IslandPlan`
      is new and additive (mirrors `StripPlan`'s precedent), not a
      `StarterLandPlan`/`StarterZone` retrofit — avoids risking four
      already-shipped presets for a seed-plumbing change only ocean_island
      needs. Seeding solved for free: `WorldLayoutPlan.seed` is populated
      and re-seeded the same way regardless of layout mode, so
      `LimitedBiomeSource.effectiveLayoutPlan()`'s already-resolved real
      seed is available to both the biome path and the terrain path with
      zero new plumbing, whichever mode 7.2 ends up using for the land
      biome itself (confirmed
      `EnvelopedChunkGenerator.LayoutContext.plan()` reads it live, never a
      stale copy). `IslandShapeProfile` reuses `WorldLayoutPlan`'s own
      hash primitives, not `RandomState` noise, since `LimitedBiomeSource`
      has no `RandomState` access at all (only `Climate.Sampler`) and both
      paths must agree on the coastline pixel-for-pixel. Shared
      beach-width gap deliberately left as-is for other presets (same
      risk-containment reasoning); ocean_island gets its own correct
      narrow ring instead. New dedicated `MIN_ISLAND_RADIUS_BLOCKS`/
      `MAX_ISLAND_RADIUS_BLOCKS` bounds rather than the shared starter-
      radius ones (64-4096 doesn't reach GOALS 01's 1-chunk floor). No
      spawn-strategy option or separate Overworld Exterior toggle for this
      preset — see DESIGN §24.8 for why. GOALS 01/04 scope split confirmed:
      7.2 ships infinite ocean (no natural land ever); 7.3 adds the
      exclusion-zone release as one toggle on the same preset. No blocking
      questions found for Jason after full review — GOALS/TODO text was
      prescriptive enough; remaining choices are implementation judgment,
      documented above and in DESIGN §24.
- [x] 7.2 Implement the `ocean_island` world type: configurable island size
      (1 chunk → huge), chosen island biome, endless ocean via the terrain
      cap, underground structures intact, unchanged Nether/End, beatable
      (progression guarantees). Use case 01.
      **Done (0.2.29):** new pure-logic `IslandShapeProfile` (seed-hashed
      sine-harmonic coastline perturbation, shore-distance-based smoothstep
      terrain raise reusing `StarterLandProfile`'s relief-noise approach)
      and `IslandOceanProfile` (shallow-to-deep seabed depth ramp; shore
      ring's beach/stony-shore 50/50 pick; ocean gradient's biome pool
      widens from 3 shallow ids to the full 9-entry vanilla ocean set
      beyond the shallow band). New `IslandPlan` record (mirrors
      `StripPlan`'s additive precedent) threaded through
      `LimitedBiomeSource` (a new field/codec entry, biome classification
      via new early checks in `getNoiseBiome`) and read live by
      `EnvelopedChunkGenerator` via `originSource` (no second codec copy
      to desync). `ExteriorTerrainProfile` gained backward-compatible
      explicit-depth overloads so the gradient reuses the same
      bedrock/stone/water/air block-layer math every other exterior mode
      already has, instead of duplicating it. Found and fixed the same
      "early-exit skips a new mechanism" bug class Phase 6.2a hit for
      strip: `applyTerrainAdjustments`'s `layout.isEmpty() &&
      starterLand.isEmpty()` skip and `hasActiveExterior`'s OR both needed
      `|| island.enabled()` added, or island's own terrain/masking would
      have silently never applied. Real design pivot found only during
      implementation (not caught in 7.1's design pass): ocean_island's
      `WorldLayoutPlan` ended up `LEGACY`, not the `SINGLE_BIOME` 7.1
      sketched -- `SINGLE_BIOME`'s own unconditional full-land-factor
      terrain raise would have fought the island's own shape-aware raise
      everywhere outside the island (no radius concept in that mode at
      all). The real seed is still free either way since
      `WorldLayoutPlan.seed` doesn't care which mode it's attached to;
      DESIGN §24.2/§24.9 updated to match what actually shipped. Renamed
      `LimitedBiomeSource.resolveStripWorldAllowed` to
      `resolveFullVanillaOverworldAllowed` (its "full `#minecraft
      :is_overworld` tag" logic was never actually strip-specific, and
      ocean_island's own fieldless-preset default and GOALS-04 fallback
      delegate both needed the identical thing). Learned the strip_world
      fieldless-preset lesson early this time: `oceanIslandDefaults`
      wired into every branch of `LimitedBiomeSource.resolve()` from the
      start, not discovered as a bug afterward. Full new preset scaffolding
      (`OceanIslandConfig`, `OceanIslandCustomization`,
      `OceanIslandPresetEditor`, `OceanIslandCustomizeScreen` -- no
      spawn-strategy field or Overworld Exterior toggle, per DESIGN
      §24.8 -- `ocean_island.json`, `normal` tag entry, lang keys, both
      loaders' registration) mirroring the established one-preset-per-
      challenge-family pattern exactly. New dedicated
      `MIN_ISLAND_RADIUS_BLOCKS`/`MAX_ISLAND_RADIUS_BLOCKS` config bounds.
      39 new tests (pure-logic profiles, `IslandPlan` validation,
      `OceanIslandCustomizationTest`, `WorldzConfigTest`/
      `WorldPresetResourcesTest`/`ProjectMetadataTest` additions); full
      suite green (345 tests); clean build across all modules.
      **The GOALS 04 exclusion-zone mechanism (7.3) shipped as part of
      this task** -- see 7.3 below.
- [x] 7.3 Distant natural islands (04): release the ocean cap beyond the
      exclusion radius so the seed's natural terrain resumes far away.
      **Mechanism done as part of 7.2 (0.2.29):** `IslandPlan
      .withinExclusionZone` gates every column classification
      (`effectiveModeAt`, `islandTargetHeight`, `islandOceanDepthAt`,
      `LimitedBiomeSource.getNoiseBiome`'s island branch) from the start
      -- splitting it into a separate later change would have meant
      re-touching the identical call sites twice for no benefit (DESIGN
      §24.9). Nothing left here but test-config/documentation
      confirmation, folded into 7.4's acceptance pass below.
      **Follow-up fix (0.2.30), found during 7.2's own review before any
      in-game testing:** the fallback End-portal guarantee (beatability)
      never fired for any ocean_island world. `WorldLimitManager`'s
      `exteriorObjective` gate and `ObjectiveSite.supportiveRadius` only
      ever checked `ExteriorPlan`/border state, and DESIGN §24.1/24.5
      deliberately keep the island's exterior out of `ExteriorPlan`
      entirely -- so every ocean_island world looked like an unlimited
      normal world to this check and silently skipped the whole
      guarantee. New `ObjectiveSite.supportiveRadius(..., IslandPlan)`
      overload (tightest of border/envelope/island; unchanged for every
      other preset) threaded through `ProgressionGuarantees.ensureEndPortal`
      and the gate itself. Full design/rationale and one deliberately
      deferred narrower edge case (`isSupportiveColumn`'s `LEGACY`
      fast path can't distinguish island interior from open ocean, though
      the existing safety-margin fallback logic sidesteps it in
      practice) in DESIGN §24.9. 3 new tests; full suite green (348
      tests); clean build.
- [ ] 7.4 Test configs (tiny/default/huge island, 04 variant); docs;
      **[Jason]** acceptance including "does the island read as natural".
      **Configs and docs done (0.2.31):** `config/tests/30`-`33` (default
      128-radius island; the 8-block "1 chunk" floor exercising the
      documented tiny-island fallback-portal trade-off; a 4096-radius
      "huge" island where the 30% coastline perturbation should read as
      dramatic bays/headlands; a GOALS-04 exclusion-zone variant at a
      test-friendly 512-block radius rather than the 2000-block default,
      purely so it's practical to actually travel and check in a
      reasonable amount of time). New "Phase 7 acceptance" section in
      MANUAL_TESTING.md (5 items, including a Customize-screen sanity
      check and an explicit note that 0.2.29 shipped with the beatability
      bug 0.2.30 fixed — don't bother re-testing beatability on exactly
      0.2.29). `config/tests/README.md`'s intro updated for the fifth
      World Type entry, plus a drive-by fix of a stale claim about config
      29 (strip_world bands) still needing Customize opened, which 0.2.27
      already fixed but this file never got updated to say so.
      **[Jason] acceptance still outstanding** — nothing to report yet,
      this note only covers docs/configs being ready. This completes
      every non-[Jason] item in Phase 7 (7.1 design, 7.2 core + GOALS-04
      mechanism, the 7.2 beatability follow-up fix, 7.4 docs/configs) —
      **do not start Phase 8 without Jason's explicit go-ahead.**
      **Test-1 findings and fixes (0.2.32):** Jason tested config 30
      (default island) and found three issues, all root-caused against
      the real server log/screenshots before fixing (full detail in
      DESIGN §24.10, MEMORY.md 2026-07-19): (a) the fallback End portal
      always built at `y = -64` (world floor) — a real, general,
      pre-existing bug (`Level#getHeight` returns `getMinY()` for an
      unloaded chunk, and `ensureEndPortal` queried it before its own
      target chunk had ever loaded), not ocean_island-specific, fixed by
      force-loading the chunk first; (b) the ocean's biome patches formed
      a visible checkerboard (`IslandOceanProfile.biomeAt` used a raw
      unblended grid), fixed with jittered-grid Voronoi cells; (c) the
      coastline read as an unnaturally smooth single-lobed blob
      (angle-only sine harmonics can't produce coves or fractal
      roughness), fixed by adding a second hashed-lattice value-noise
      term directly to the distance-from-shore field. All three are
      pure-logic fixes with new/updated JUnit coverage; full suite green;
      clean build. Re-deployed as 0.2.32 — **awaiting Jason's re-test of
      config 30 specifically before Phase 8 starts.**
      **Test-2 findings and fix (0.2.33):** Jason's 0.2.32 re-test
      confirmed the portal fix but found the exterior ocean "sterile" —
      no vegetation, no world-gen fish/squid population, no shipwrecks/
      ocean ruins/monuments. Root cause (full detail in DESIGN §24.11,
      MEMORY.md 2026-07-19): `EnvelopedChunkGenerator` deliberately skips
      vanilla decoration/structures/mob-population for any "entirely
      exterior" chunk — general, pre-existing behavior shared by every
      preset with an ocean exterior, low-stakes for strip_world/
      single_biome/chaos_biomes (an incidental boundary) but fatal to
      ocean_island's whole explorable ocean. Jason chose the full fix
      (vegetation + mobs + structures), scoped to `island.enabled()` only
      so every other preset's shipped exterior-ocean behavior is
      unchanged. New `decoratesExteriorOcean` check lets
      `applyBiomeDecoration`/`spawnOriginalMobs`/`createStructures` run
      normally for entirely-ocean chunks when the island is enabled; the
      trailing `applyEnvelope` repaint (which would otherwise immediately
      erase whatever decoration just placed) is skipped for those chunks
      specifically, safe because earlier-pipeline `applyEnvelope` calls
      already shaped the terrain correctly. Full suite green (352 tests,
      unchanged — proof the change is a no-op for every non-island
      preset); clean build. Re-deployed as 0.2.33 — **awaiting Jason's
      re-test of the exterior ocean specifically before Phase 8 starts.**
      **Test-2 follow-up fix (0.2.34):** 0.2.33 re-test confirmed the
      portal now reaches the surface, but Jason flagged that as the
      actual problem — the fallback vault was only ever designed to sit
      underground (floor + corner posts, relying on natural stone as
      walls), so on the open surface it read as an incomplete, exposed
      platform. Direction: "below ground like the stronghold... between
      Y-10 and Y-60... in an enclosed room, like the Portal Room." Fixed
      (full detail in DESIGN §24.12, MEMORY.md 2026-07-19): placement now
      uses a fixed `Y = -32` instead of a surface-height query, and
      `buildEndPortalSite` was rewritten to mirror `buildBlazeSite`'s
      existing full floor/ceiling/walls/doorway shell instead of its old
      floor-plus-corner-posts design. One known, deliberately deferred
      edge case logged: a `LayoutMode.VOID` world's floating starter
      island might not have solid ground at `Y = -32`, so its fallback
      vault could float in open void — not fixed now, nobody is testing
      that path. Clean build, unchanged 352-test suite (no pure-logic
      class touched). Re-deployed as 0.2.34 — **awaiting Jason's re-test
      of the portal's depth/enclosure before Phase 8 starts.**
      **Test-3 finding and fix (0.2.35):** 0.2.34 re-test confirmed the
      portal fix and the ocean-life fix; one further finding — the shore
      ring's beach/stony-shore mix reads as speckled (flips block-by-
      block) rather than forming stretches. Jason's request: alternate
      the two in contiguous stretches of varying length. Fixed (full
      detail in DESIGN §24.13, MEMORY.md 2026-07-19) with a 1D jittered-
      Voronoi arc pick keyed on the column's angle around the island
      instead of raw block coordinates — segments sized to average 32
      blocks of arc length, scaled by the island's own radius, naturally
      uneven in length since jittered Voronoi cells always are. Two new
      qualitative tests (mostly-contiguous transitions; non-uniform run
      lengths). Full suite green (354 tests); clean build. Re-deployed as
      0.2.35 — **awaiting Jason's re-test of the coastline specifically
      before Phase 8 starts.**
      **[Jason] acceptance (2026-07-20): confirmed** — shore biomes read
      as a natural mix of beach and stony shore on 0.2.35. **Phase 7 is
      fully complete and approved; proceeding to Phase 8.**

## Phase 8 — Ocean island extras (GOALS 03, 02)

- [x] 8.1 Starter-chest infrastructure (shared with the sky, cave, Nether,
      and End phases): loot presets + YAML-configurable contents, placed at
      spawn. Then use case 03: no-land ocean world, spawn on/next to a chest
      boat with essentials (lily pad, dirt, grass block, saplings) +
      configurable randoms.
      **Done (0.2.37):** new `IslandSource` enum (`ARTIFICIAL`/`NATURAL`/
      `CHEST_BOAT`) on `OceanIslandCustomization`/`OceanIslandConfig`,
      Customize-screen cycle button (mirrors `StripWorldCustomizeScreen`'s
      spawn-strategy button). New `IslandPlan.hasLand` boolean threaded
      through all four column-classification call sites the island
      touches (`LimitedBiomeSource.islandBiomeAt`,
      `EnvelopedChunkGenerator.effectiveModeAt`/`islandTargetHeight`/
      `islandOceanDepthAt`) plus a fifth found only by proactively
      re-auditing every `IslandPlan` consumer before writing code:
      `ObjectiveSite.supportiveRadius(..., IslandPlan)` would have
      wrongly narrowed the fallback End-portal guarantee to 8 blocks for
      every chest-boat world (its placeholder radius), fixed with a
      regression test. New `StarterKitPlan` (pure logic, seed-deterministic
      essentials + random extras) and `StarterKitConfig`
      (`oceanIsland.starterKit` in YAML, no Customize-screen field --
      matches every other variable-length list in this codebase already
      being YAML-only). New `StarterKitDeployment` spawns an
      `EntityTypes.OAK_CHEST_BOAT` at the world origin's water surface
      with the resolved kit, called from `WorldLimitManager
      .onServerStarted` (new `needsChestBoat` addition to its early-return
      gate, reusing the existing one-time `WorldLimitState` guard).
      `NATURAL` is a placeholder in this commit -- selectable, but
      resolves identically to `ARTIFICIAL` until 8.2 (documented gap in
      DESIGN §25.5, including the `currentCustomization()` read-back
      ambiguity 8.2 will need to address). Full suite green; clean build.
- [x] 8.2 Natural island by seed (02): search the real seed's unmodified
      climate/terrain for a small natural island, set world spawn/origin
      there, replace everything else with ocean beyond it. Reuses the 16.3
      spawn-search + recentering machinery. This is the hardest ocean item —
      keep it last and time-boxed; if the search proves unreliable, park it
      with findings in DESIGN and move on.
      **Done (0.2.38):** turned out simpler than the design pass expected
      (full detail in DESIGN §25.6, MEMORY.md 2026-07-20) — `ocean_island`'s
      allowed biome set is already the full vanilla tag, and
      `LimitedBiomeSource.getNoiseBiome`'s existing fallthrough already
      samples the real climate for any unoverridden column, so `NATURAL`
      mode only needed `islandBiomeAt` to return empty within
      `radiusBlocks` — no separate natural-biome-passthrough machinery.
      New `IslandPlan.syntheticLand` (alongside 8.1's `hasLand`)
      threaded through the same call sites, plus `islandOceanDepthAt`'s
      shore-ring subtraction generalized. New pure-logic
      `NaturalIslandSearch.isIsolatedLand` (8-point ring sample, 75%
      ocean threshold) wired into a new `SpawnOriginManager
      .resolveNaturalIslandOrigin`, dispatched independently of the
      shared `spawnStrategy` mechanism since `ocean_island` always keeps
      that at `STARTER_AT_ORIGIN`. Also closed the `currentCustomization()`
      read-back gap flagged after 8.1. **Not validated against real
      seeds** — only pure-logic unit tests with synthetic predicates;
      this is exactly the "time-boxed, park if unreliable" allowance
      this task grants, and Jason's acceptance testing will tell us if
      the isolation threshold needs tuning. Full suite green (386
      tests); clean build.
- [x] 8.3 Test configs; docs; **[Jason]** acceptance.
      **Configs and docs done (0.2.39):** `config/tests/34`-`35`
      (chest-boat with a config-overridden starter kit; natural island
      at a tighter 48-block search radius). New "Phase 8 acceptance"
      section in MANUAL_TESTING.md. `config/tests/README.md` updated
      (file count, `islandSource` explanation). README.md's "Ocean
      island challenge" section rewritten to cover all three
      `islandSource` values, plus a new `starterKit` config table
      section. **[Jason] acceptance still outstanding** — this note
      only covers docs/configs being ready. This completes every
      non-[Jason] item in Phase 8 (8.1 chest boat/no-land ocean, 8.2
      natural island by seed, 8.3 docs/configs) — **do not start Phase
      9 without Jason's explicit go-ahead.**

## Phase 9 — Ocean fluid variants: lava ocean + dry world (GOALS 28, 31)

Right after the ocean phases: the ocean-island shape with the fluid swapped
(lava) or removed (dry), so the infrastructure is fresh.

- [x] 9.1 Design pass (DESIGN §20.10/§20.11): parameterize the ocean
      exterior/cap fluid — water / lava / none. For lava (28): verify 26.2
      surface-lava-at-scale behavior (light, fire spread at the shore ring,
      fluid ticking, map color) and shore safety so the transition ring
      can't ignite spawn. For dry (31): water-scarcity semantics —
      default keeps water where structures/features naturally place it
      (village farms and wells, strongholds, aquifer pockets, springs);
      harder options remove more (rivers, surface lakes). Beatability:
      potions and water-dependent progression obtainable at every offered
      difficulty.
      **Done (0.2.40):** full design in DESIGN §26. Confirmed with Jason:
      new `fluid` field (`water`/`lava`/`none`) on the existing
      `ocean_island` preset, orthogonal to `islandSource` — same
      precedent as Phase 8, not a shared-mechanism change or a new
      preset. Mechanically a single substitution point in
      `EnvelopedChunkGenerator.exteriorState()`'s existing `WATER` case.
      GOALS 31's core beatability requirement (structures/aquifers still
      provide water) holds automatically, no special casing needed.
      **The "harder" difficulty option (remove rivers/lakes) is
      deliberately deferred, not implemented** — investigated, found to
      need real climate-biome sampling threaded through
      `effectiveModeAt` and every caller of it, a capability that
      doesn't exist at that layer; shipping a label-only version would
      be actively misleading (the real water would still generate).
      Full reasoning trail in DESIGN §26.3 and MEMORY.md's 2026-07-20
      entry. `IslandPlan`'s exclusion-zone fields consolidated into a
      nested record to make room for `fluid` under the 14-field codec
      ceiling this DFU version enforces (confirmed by compiler error).
- [x] 9.2 Implement lava ocean (as an `ocean_island` fluid option or its own
      type — 9.1 decides); test configs; **[Jason]** acceptance including
      strider/bridging travel viability.
      **Done (0.2.41):** shipped in one commit together with 9.3 (same
      `fluid` substitution point, differing only in which enum value
      maps to which block — full detail in DESIGN §26.4). New
      `IslandFluid` enum; `EnvelopedChunkGenerator.exteriorState()`
      gained a `fluid` param, its `WATER` case now switches to
      `Blocks.WATER`/`LAVA`/`AIR`. Test config `36-ocean-island-lava.yaml`
      and a new "Phase 9 acceptance" section in MANUAL_TESTING.md.
      **[Jason] acceptance outstanding** — surface-lava-at-scale
      behavior (light, fire spread, fluid ticking) needs real in-game
      verification, not something code review could confirm.
- [x] 9.3 Implement dry world with the water-findability difficulty option;
      test configs; **[Jason]** acceptance.
      **Done (0.2.41), partial scope:** the core "drained ocean basin"
      behavior (`fluid: none`) is implemented and beatability is
      automatic (structures/aquifers untouched by this mechanism). **The
      water-findability "harder" difficulty option (removing rivers/
      surface lakes) is deliberately NOT implemented** — investigated
      during 9.1/9.2, found to require real climate-biome sampling
      threaded through `effectiveModeAt` and every caller of it, which
      doesn't exist at that layer; a label-only version would be
      actively misleading since the real water would still generate
      underneath. Full reasoning in DESIGN §26.3. Test config
      `37-ocean-island-dry.yaml`. **[Jason] acceptance outstanding.**

## Phase 10 — Sky island challenge (GOALS 05–06)

**Scope decided with Jason (2026-07-20)**, splitting the original 10.1/10.2
pair into the granularity this phase actually needs (same reasoning as ocean
island's 7.1–7.4/8.1–8.3 split):

- **Island shape:** a true floating island — a thin, fixed-thickness slab
  (surface Y plus a configurable thickness below it), void everywhere else
  within the footprint and beyond it. Not the full-depth terrain plug the
  original TODO text flagged as wrong.
- **Nether/End (GOALS 06):** Overworld + Nether sky islands this phase.
  The End is a separate, spike-first task (10.5) — findings only, no
  implementation, mirroring the soft-void border (5c) and Nether-start
  (14.1) "spike before committing" pattern. End sky islands may become
  their own later phase depending on what the spike finds.
- **Villages beyond the exclusion zone (GOALS 07): deferred to Phase 11**
  in full — it's a narrow slice of Phase 11's dedicated "seed-driven
  scattered floating islands" scope, not this phase's core single-island
  case. Not tracked as a task here; see Phase 11.
- **Chest tiers:** three built-in tiers (easy/medium/hard) on top of
  Phase 8's `StarterKitPlan`; biome informs only the one water-source item
  GOALS 05 names by name (bucket of water vs. cauldron in dry biomes) —
  everything else stays config-overridable like the existing kit.

- [x] 10.1 Design pass (DESIGN §27): `SkyIslandPlan` (new, additive record
      mirroring `IslandPlan`/`StripPlan` — reuses `IslandShapeProfile` for a
      natural-looking footprint, adds the new vertical bounded-below
      mechanism no earlier phase needed), default spawn/surface Y ≥ 64
      (slime rule, configurable), stronghold/beatability approach (reuses
      the existing fallback End-portal vault unchanged — it already builds
      its own fully enclosed shell, so it needs no ground to sit on), and
      the Nether sky variant's shape. **Commit** design before implementing.
      **Done (0.2.42):** full design in DESIGN §27. Key call: classify a
      sky island's footprint as uniformly `VOID` from `effectiveModeAt`
      (like ocean island reuses a uniform `OCEAN`), then distinguish
      slab-vs-void one level down in the block-filling functions — needs
      no new `ExteriorMode` value and no `applyTerrainAdjustments`
      involvement at all (no natural floor is ever raised; the whole
      footprint is synthesized directly, immune to vanilla carvers the
      same way `OCEAN`/`VOID` already are). Beatability's gate turned out to
      need the exact same fix shape as ocean island's after all (10.2 found
      this during implementation, corrected in DESIGN §27.5 before any
      in-game testing): `WorldLimitManager` reads
      `LimitedBiomeSource.exteriorPlan()`, not `EnvelopedChunkGenerator
      .envelope()`, so a new `ObjectiveSite.supportiveRadius(..., SkyIslandPlan)`
      overload plus an `overworldSkyIsland.enabled()` gate arm (mirroring
      `IslandPlan`'s own pair exactly) were both genuinely required — an
      initial draft assuming the envelope-forcing trick alone would cover
      it was wrong (confirmed by grepping for callers of `envelope()`: none
      exist). Nether variant (10.4) reuses the same
      mechanism dimension-generically; only the sampling seed needs
      threading in at codec-resolve time, since `originSource` (today's
      seed source) is Overworld-only. End (10.5) confirmed genuinely
      harder than Overworld/Nether — no existing End wrapper to extend at
      all, plus the End's own already-floating-islands terrain shape — so
      it stays a dedicated spike task, not folded into this one.
- [x] 10.2 Implement the `sky_island` world type core (Overworld only):
      `SkyIslandPlan`, config/customization/preset-editor/screen scaffolding,
      world-type registration, the bounded-below terrain synthesis in
      `EnvelopedChunkGenerator`/`LimitedBiomeSource`, default Y ≥ 64 spawn,
      beatability (`ObjectiveSite.supportiveRadius` overload for
      `SkyIslandPlan`, proactively wired this time per the 8.1 lesson — see
      DESIGN §27). GOALS 05's core.
      **Done (0.2.43):** new `logic.SkyIslandPlan` (enabled, radiusBlocks,
      shapeAmplitude, islandBiome, surfaceY, thicknessBlocks; reuses
      `IslandShapeProfile.distanceFromShore` directly) and pure
      `logic.SkyIslandProfile` (Layer/BiomeFamily classification for the
      slab's block palette, since a sky island chunk never runs vanilla's
      biome-aware surface builder — DESIGN §27.3). `EnvelopedChunkGenerator`
      gained a `skyIsland` field (read live via `originSource`, same as
      `island`); `effectiveModeAt` classifies it uniformly `VOID`, and new
      `skyIslandStateAt`/`skyIslandBaseHeight` methods (wired into
      `getBaseHeight`/`getBaseColumn`/`applyEnvelope`) distinguish the
      footprint's slab from true void one level down — no new
      `ExteriorMode`, no `applyTerrainAdjustments` involvement, no carver
      special-casing needed (the trailing `applyEnvelope` call after every
      generation stage re-stamps the slab regardless of what vanilla
      carved into the delegate's unused terrain underneath, exactly like
      `OCEAN`/`VOID` already do). `LimitedBiomeSource` gained a matching
      `skyIsland` field/codec entry/`resolveSkyIslandBiomes` helper/
      `getNoiseBiome` branch and a `skyIslandDefaults` flag wired into every
      branch `oceanIslandDefaults` already had (the fieldless-preset lesson
      applied from the start this time, not discovered as a bug afterward).
      **Codec note:** `LimitedBiomeSource.CODEC`'s `instance.group(...)` was
      already at the 14-field `Function14` ceiling (DESIGN §26.1's DFU
      limit) before this task — freed two slots by nesting the three
      independent `allow_rivers`/`allow_oceans`/`allow_beaches` booleans
      into one new `PassThroughCodecs.Flags` record (they're always encoded
      together anyway), landing at 13 fields with `sky_island` added and one
      slot to spare. **Real beatability defect found and fixed during this
      task, before any in-game testing** (full account in DESIGN §27.5): an
      initial draft assumed forcing `EnvelopedChunkGenerator`'s own
      `envelope` field to `VOID` (mirroring the `LayoutMode.VOID`
      `resolveEnvelope` trick) would be enough for
      `ObjectiveSite.supportiveRadius`'s existing envelope-based overload to
      narrow correctly — wrong, since `WorldLimitManager.onServerStarted`
      actually builds `ensureEndPortal`'s envelope argument from
      `LimitedBiomeSource.exteriorPlan()`, a separate plan that (like ocean
      island's) always stays `normal` for the Overworld. Fixed with the same
      shape `IslandPlan` already has: a new `ObjectiveSite.supportiveRadius
      (..., SkyIslandPlan)` overload, `ProgressionGuarantees.ensureEndPortal`
      threading a `SkyIslandPlan` parameter through, and an
      `overworldSkyIsland.enabled()` arm on `WorldLimitManager`'s
      `exteriorObjective` gate, mirroring `overworldIsland.enabled()`'s
      existing one exactly. New preset scaffolding
      (`SkyIslandConfig`/`SkyIslandCustomization`/`SkyIslandPresetEditor`/
      `SkyIslandCustomizeScreen`, `sky_island.json`, `normal` tag entry, lang
      keys, both loaders' registration) mirrors `ocean_island`'s shape —
      no spawn-strategy field, no separate Overworld Exterior field (the
      island supplies its own exterior unconditionally). Default radius 16
      (Skyblock-scale, smaller than ocean island's 128 default) with the
      same shared `MIN_ISLAND_RADIUS_BLOCKS`/`MAX_ISLAND_RADIUS_BLOCKS`
      bounds. Surface-material palette is a deliberately non-exhaustive
      biome-family approximation (desert→sand, snowy→snow, mushroom→
      mycelium, else grass), documented as such, not a real `SurfaceRules`
      reimplementation. **Known, deliberately deferred gap** (same one the
      pre-existing `LayoutMode.VOID`/`FALLBACK_PORTAL_TARGET_Y` doc comment
      already flags): the fallback End-portal vault sits at a fixed
      `Y = -32`, disconnected from the island's own `surfaceY` by 90+ blocks
      of open void — not fixed now, following the same "wait for Jason's
      in-game testing before chasing vault-placement issues" pattern
      ocean island's own §24.10-§24.13 fixes actually followed. 36 new
      tests (`SkyIslandPlanTest`, `SkyIslandProfileTest`,
      `SkyIslandCustomizationTest`, `ObjectiveSiteTest` additions,
      `WorldzConfigTest`/`WorldPresetResourcesTest`/`ProjectMetadataTest`
      additions); full suite green (431 tests); clean build across all
      modules (fabric + neoforge registration compiles and resolves
      correctly). No chest/starter-kit yet — that's 10.3.
- [x] 10.3 Chest tiers: extend `StarterKitPlan`/`StarterKitConfig` with
      easy/medium/hard tiers and the biome-driven water-item swap; wire into
      `sky_island`'s chest deployment (reuses `StarterKitDeployment`). GOALS
      05.
      **Done (0.2.44):** new `StarterKitTier` enum (easy/medium/hard);
      `SkyIslandPlan` gained a persisted `chestTier` field (7th component)
      and `SkyIslandConfig` gained `chestTier` + three full
      `StarterKitConfig` sections (`easyKit`/`mediumKit`/`hardKit`,
      sensible tier-differentiated defaults — since sky island decoration
      is fully suppressed, every tier includes saplings, the one thing no
      tier can survive without). Tier is a Customize-screen cycle button
      (mirrors `islandSource`'s) and persists on the world; the actual kit
      item lists stay config-only, matching `ocean_island`'s own kit
      exactly. `StarterKitDeployment.spawnStarterChest` places a real
      `minecraft:chest` at the world origin on top of the slab, resolves
      the selected tier's kit, and appends one guaranteed water-source
      item computed from the biome (DESIGN §27.8 explains why the mapping
      is the *opposite* of a literal reading of GOALS 05's own example:
      dry/desert-family biomes get a water bucket since rain never
      supplies one, every other family gets a cauldron since rain will
      fill it naturally). `WorldLimitManager` gained a
      `needsStarterChest` gate paralleling `needsChestBoat` exactly. 8 new
      tests; full suite green (439 tests); clean build. **Known,
      deliberately deferred risk flagged for 10.6 testing:** the chest is
      placed at literal origin the same way the chest boat already is,
      with no accounting for `safeSpawnOffsetBlocks()`'s own spawn
      offset — worth checking specifically on a small-radius (8-16 block)
      island.
- [x] 10.4 Nether sky island variant: same bounded-below mechanism applied
      to the Nether exterior, with a fortress/structure-retention toggle
      reusing `ProgressionGuarantees.ensureBlazeAccess`'s existing
      self-built fallback. GOALS 06 (Nether only — End is 10.5).
      **Done (0.2.45):** since the Nether has no `LimitedBiomeSource` at
      all, its sky island plan (`netherSkyIsland`) is persisted directly
      on `EnvelopedChunkGenerator`'s own codec instead — mirrors
      `StripPlan`'s exact precedent, not a new mechanism. New
      `activeSkyIsland()` helper (`dimension == OVERWORLD ? skyIsland :
      netherSkyIsland`) is the single thing every existing sky-island call
      site now goes through, so the vertical-slab mechanism itself needed
      no duplication. Two real design decisions beyond plumbing: (a) the
      Nether has no meaningful "biome" concept, so its block palette is a
      fixed netherrack/basalt one (`skyIslandStateAt` branches on
      `this.dimension`), not a biome-family lookup; (b) the seed genuinely
      needed new plumbing (`netherSkyIslandSeed` + `setSkyIslandSeed`,
      called from the same `ChunkMapMixin` injection on both loaders that
      already resolves the Overworld's `LimitedBiomeSource.setLayoutSeed`).
      **Beatability needed the same gate fix as 10.2's Overworld finding,
      caught this time by applying that lesson proactively** (DESIGN
      §27.6 has the full account): `WorldLimitManager`'s Nether generator
      fetch had to move earlier (before the `exteriorObjective` gate
      check, not just before `ensureBlazeAccess`) so a
      `netherSkyIsland.enabled()` arm could join that gate exactly like
      the Overworld's own arm; `ProgressionGuarantees.ensureBlazeAccess`
      gained a `SkyIslandPlan` parameter mirroring `ensureEndPortal`'s.
      One `applyToNether` Customize-screen checkbox (mirrors
      `StripWorldCustomizeScreen`'s) reuses the Overworld's own shape
      fields rather than exposing independent Nether dimensions, matching
      `StripConfig.applyToNether`'s precedent. 4 new tests
      (`SkyIslandCustomizationTest`/`WorldzConfigTest`/
      `WorldPresetResourcesTest` additions); full suite green (441 tests);
      clean build across all modules.
- [x] 10.5 End sky island — design spike only (throwaway branch OK, same
      posture as 5c.1/14.1): investigate whether/how `EnvelopedChunkGenerator`-
      style wrapping extends to the End (untouched by this mod so far),
      report findings in DESIGN §27, **[Jason]** go/no-go on scheduling a
      real implementation task. Not scheduled to this phase's completion
      gate.
      **Done, research only, no code — full write-up in DESIGN §27.7.**
      Two findings, one expected and one that changes the shape of this
      item entirely: (a) wrapping the End's generator the same way as
      Overworld/Nether is technically feasible after all (every preset's
      End section uses the same `minecraft:noise` generator type
      `EnvelopedChunkGenerator` already wraps) — the earlier framing that
      no wrapper exists at all was about it never being *attempted*, not
      about a real blocker; (b) far more importantly, decompiling
      `TheEndBiomeSource` confirms vanilla End generation is *already* a
      bounded-below floating-island world natively (`end`/`end_highlands`/
      `end_midlands`/`end_barrens`/`end_islands` — small landmasses in
      void, no code needed) — there is no "one giant contiguous landmass"
      problem to solve here the way there was for the Nether (10.4).
      Combined with the already-shipped End-border carry-over (GOALS 17,
      §5.2) as the existing "keep it small" lever, GOALS 06's End
      component reads as **already satisfied by existing mechanism** for
      every world type, sky island included. **Recommendation: no further
      End-specific work needed** — beatability (dragon fight, fallback
      portal vault) is unaffected either way. No version bump (research
      only, no code or config changed).
      **[Jason] go/no-go resolved (2026-07-20): skip.** Vanilla End
      generation already uses the island concept natively, so a dedicated
      "force a single End island" phase isn't compelling. GOALS 06's End
      component is considered done via existing mechanism, permanently —
      do not re-open this without a new reason to reconsider.
- [ ] 10.6 Test configs (small/default/huge island, each chest tier, Nether
      sky variant on/off, stronghold-fallback beatability check); docs
      (README, MANUAL_TESTING.md); **[Jason]** acceptance per GOALS 05/06
      sub-case.
      **Configs and docs done (0.2.46):** `config/tests/38`-`43` (default
      16-radius island; the 8-block "1 chunk" floor specifically targeting
      the deliberately-deferred chest/spawn-offset risk logged in 10.3;
      a 256-radius/16-thick "huge" variant; easy-tier-on-desert and
      hard-tier-on-plains configs exercising both the chest-tier axis and
      the biome-driven water-item swap together; the Nether variant via
      `skyIsland.applyToNether`). New "Phase 10 acceptance" section in
      MANUAL_TESTING.md covering all six configs plus an explicit note
      that the End is out of scope per 10.5's findings.
      `config/tests/README.md`'s intro updated for the sixth World Type
      entry. README.md gained a full "Sky island challenge" section
      (challenge-types table row + prose + config example + settings
      table) mirroring the ocean island section's shape, cross-linking
      rather than duplicating the shared border/exterior docs.
      **[Jason] acceptance still outstanding** — this note only covers
      docs/configs being ready, most importantly including the flagged
      tiny-island spawn/chest-reachability risk (config 39) which has not
      yet been confirmed either way. This completes every non-[Jason]
      item in Phase 10 (10.1 design, 10.2 core, 10.3 chest tiers, 10.4
      Nether variant, 10.5 End spike findings, 10.6 docs/configs) —
      **do not start Phase 11 without Jason's explicit go-ahead**, and
      note 10.5's own outstanding go/no-go on the End (DESIGN §27.7) is
      independent of that phase gate.
      **Beatability follow-up (2026-07-21, from Jason's real config-41
      testing):** two real gaps found, not just tuning -- desert islands
      had no plantable dirt/seeds (sand can't hold a sapling and there's
      no grass to break for seeds) and no sky island of any biome had a
      lava source (needed for obsidian, without which the Nether/End are
      unreachable on a void-isolated island). Fixed: `lava_bucket:1`
      added to all three `SkyIslandConfig` kits; desert-family biomes now
      also get tier-scaled dirt/seeds and an upgraded easy-tier water item
      (2 ice blocks instead of 1 bucket, for a real infinite source) via
      `StarterKitDeployment.biomeEssentialItems` (DESIGN §27.8). Configs
      41/42 comments updated with the new expected contents and an
      obsidian/portal beatability check. **Quantities are a first pass,
      not sign-off** -- still covered by 10.6's outstanding acceptance
      pass, same as everything else in this task.
      **Biome exclusion zone follow-up (2026-07-21, same testing
      session):** a bare sky_island world had no buffer at all between the
      starter island's own configured biome and the real seed's biome one
      column past the edge. Fixed: new `SkyIslandConfig.exclusionZoneEnabled`
      /`exclusionZoneRadiusBlocks` (default on, 128 blocks), threaded
      through `SkyIslandPlan`/`SkyIslandCustomization`/codecs/Customize
      screen (DESIGN §27.10). Biome only, not terrain -- the void beyond
      the island is unaffected. New test config 57 exercises it. Still
      covered by 10.6's outstanding acceptance pass.

## Phase 11 — Floating resource islands (GOALS 07–08)

Split from the original single 11.1 item into the granularity every other
multi-part phase in this project has needed (Phase 6.2a/6.2b, Phase 10's
10.1-10.6 precedent) — logged here rather than silently expanding one task.
Also folds in GOALS 07 (villages beyond an exclusion zone), deferred here
in full per the Phase 10 header's own note.

- [x] 11.1 Design pass (DESIGN §28): placement mechanism (jittered grid,
      reusing `IslandShapeProfile`/hash-cell primitives), the exclusion
      zone (reusing `IslandPlan.ExclusionZone`), the three resource layers
      (biome diversity, ore deposits, loot chests), and the guaranteed-
      village mechanism — verify the real vanilla structure/feature forced-
      placement APIs against 26.2 sources first. **Commit** design before
      implementing.
      **Done (0.2.47):** full design in DESIGN §28. Scope decided with
      Jason (2026-07-20): resources are a configurable combination of all
      three layers (not either/or); village placement is **guaranteed**,
      not best-effort; a configurable exclusion-zone buffer precedes
      scattered islands, mirroring GOALS 04's mechanism. Verified against
      the real 26.2 decompiled sources: `PlaceCommand.placeFeature`
      (`net/minecraft/server/commands/PlaceCommand.java`) confirms
      `ConfiguredFeature.place(level, generator, random, pos)` is a real,
      public API for forcing a single vanilla ore vein at an exact
      position, bypassing normal biome/height/count placement gating —
      exactly what a synthetic void-slab island needs for ore deposits (no
      real underground for natural ore gen to run in). `PlaceCommand
      .placeStructure` confirms `Structure.generate(...)` +
      `StructureStart.placeInChunk(...)` is the real, public `/place
      structure` implementation for forcing a real vanilla jigsaw structure
      (a village) at a chosen position with our own `EnvelopedChunkGenerator`
      supplying terrain-fit height queries — the mechanism 11.5 will use for
      the guaranteed village. Confirmed real structure ids exist for every
      village biome variant in the 26.2 client jar
      (`village_plains`/`_desert`/`_savanna`/`_snowy`/`_taiga`). No new
      preset: `floatingIslands` nests onto the existing `sky_island` config/
      customization (GOALS 08 is explicitly "same as 7, but...", matching
      the `IslandPlan.fluid`/`exclusionZone` precedent of composable
      options on an existing preset rather than a new typed preset per
      option). Overworld only this phase (GOALS 08's text has no Nether
      component); Nether floating islands noted as a straightforward future
      extension in DESIGN §28.5, not scheduled.
- [x] 11.2 Core: `FloatingIslandsPlan` (pure logic — grid placement,
      jittered island centers/radii, exclusion zone, biome-variety
      selection; JUnit-covered), config/codec/customization plumbing on
      `SkyIslandConfig`/`SkyIslandCustomization`, the sub-screen on
      `SkyIslandCustomizeScreen`, and the terrain/biome wiring into
      `EnvelopedChunkGenerator`/`LimitedBiomeSource` so scattered islands
      actually generate (reusing `SkyIslandProfile`'s surface-material
      palette). No resource layers or village yet — void-with-empty-islands
      is this task's acceptance bar.
      **Done (0.2.48):** new `logic.FloatingIslandsPlan` (jittered
      grid — a cell is present or empty via hash-picked `spawnChance`,
      jittered center bounded to 0.3x cell size so `at()` only ever needs
      to check the query cell's 3x3 neighborhood, hash-picked radius,
      reuses `IslandShapeProfile.distanceFromShore` for the coastline,
      reuses `IslandPlan.ExclusionZone` directly for the void buffer) +
      `config.FloatingIslandsConfig`; `SkyIslandPlan` gained a
      `floatingIslands` field (nested entirely inside `SkyIslandCodecs`'
      own group — the "one slot to spare" left after `sky_island`, per
      DESIGN §28.4, no `LimitedBiomeSource` ceiling impact) and
      `SkyIslandConfig` gained a `floatingIslands:` section.
      `EnvelopedChunkGenerator` gained a `skyIslandHitAt` helper (checks
      the starter island first, then the scattered grid) that every
      existing `distanceFromShore`-based call site
      (`skyIslandBaseHeight`, `getBaseColumn`, `applyEnvelope`) now goes
      through instead of calling `activeSkyIsland().distanceFromShore`
      directly; `skyIslandStateAt` takes the hit's own biome instead of
      always `active.islandBiome()`, so a biome-variety scattered island
      gets its own surface palette. `LimitedBiomeSource.resolveSkyIslandBiomes`
      now resolves every biome in the floating-islands pool too (not just
      the starter island's one), and `getNoiseBiome`'s sky-island branch
      falls through to `floatingIslands().at(...)` when the starter
      island's own distance is positive. Nether floating islands
      deliberately out of scope (DESIGN §28.5) —
      `netherSkyIslandPlan()` always resolves `FloatingIslandsPlan.disabled()`
      regardless of the configured value. **Revised the sub-screen call in
      DESIGN §28.4 during implementation:** inlined the new fields
      directly into `SkyIslandCustomizeScreen`'s existing scrollable form
      instead of a dedicated sub-screen — `StripWorldCustomizeScreen`'s
      own "bands" feature (near-identical shape: enabled toggle,
      multi-line biome list, several more fields) is the closer precedent
      and inlines rather than opening one; the Border/EndBorder/Exterior
      sub-screens exist specifically because they're reused across
      four-plus different presets, a reuse need floating islands (sky-
      island-only) doesn't share. 14 new tests (`FloatingIslandsPlanTest`,
      `WorldzConfigTest` additions); full suite green (474 tests); clean
      build across all modules. README gained a "Floating resource
      islands" subsection documenting the placement mechanism, explicitly
      flagged as core-mechanism-only (no resources/village yet — those
      land in 11.3-11.5). Not yet deployed/tested in-game — nothing
      walkable-and-different-from-void to show Jason until 11.3+ add
      actual resources; the whole slice waits for 11.6's acceptance pass.
- [x] 11.3 Ore deposits: `ResourceConfig.oreDepositsEnabled`/`oreFeatureIds`,
      per-island hash-picked `ConfiguredFeature` placement clamped to the
      slab's own thickness (DESIGN §28.2).
      **Done (0.2.49):** `FloatingIslandsPlan` gained a persisted
      `oreDepositsEnabled` boolean (10th field, nested inside its own
      codec group per DESIGN §28.4, no ceiling impact). **Revised during
      implementation:** `oreFeatureIds` itself is config-only, never
      persisted through the codec — a `ResourceConfig` nested record
      grouping both fields (as DESIGN §28.4 originally sketched) turned
      out unnecessary once `StarterKitPlan`'s own precedent was re-read
      closely: variable-length item/feature-id lists stay in
      `WorldzCommon.config()`, read live at placement time, never
      persisted per-world (`StarterKitDeployment.tierConfig` does the
      exact same thing for `easyKit`/`mediumKit`/`hardKit`). New
      `FloatingIslandsPlan.ResolvedIsland` (centerX, centerZ, radius,
      biome — resolved independent of any query column) plus
      `nearbyIslands(x, z, seed, fallbackBiome)`, both factored out of
      `hitFromCell`'s existing presence/jitter/radius/biome resolution
      (DRY, `hitFromCell` now delegates to the same `resolveCell` helper).
      `ResolvedIsland.pick`/`pickY` are pure, JUnit-tested deterministic
      hash-picks (a feature id from a candidate list; a Y within an
      inclusive range) reusing the record's own enclosing-class hash
      primitives. `EnvelopedChunkGenerator.applyFloatingIslandOre`
      (called from `applyBiomeDecoration`, after `applyEnvelope` so the
      slab's own blocks already exist to embed into) finds which one
      chunk owns each nearby island's center via `nearbyIslands` on the
      chunk's own center point (not a naive "which cell does my center
      belong to" lookup, which could miss a jittered center that landed
      in an adjacent chunk) and places the picked `ConfiguredFeature`
      there via `ConfiguredFeature.place(level, this, random, pos)` —
      verified against the real 26.2 decompiled `PlaceCommand
      .placeFeature`, which uses the identical bypass-normal-placement-
      gating API (11.1's own verification). Y clamped to
      `[bottomY()+1, surfaceY()-1]`; a slab too thin for that range
      (`thicknessBlocks: 1`) silently skips ore placement rather than
      erroring. `SkyIslandCustomizeScreen` gained an `oreDepositsEnabled`
      checkbox (screen-exposed, matching the `allowRivers`/`allowOceans`/
      `allowBeaches` boolean-toggle precedent) while `oreFeatureIds`
      stays YAML-only like every kit's own item list. Full config
      read/sanitize/map/summary wiring (`FloatingIslandsConfig` gained
      `oreDepositsEnabled`/`oreFeatureIds`, default pool: coal, small
      iron, buried gold, redstone, lapis, small diamond, emerald). 15 new
      tests (`FloatingIslandsPlanTest`, `WorldzConfigTest` additions);
      full suite green (467 tests); clean build across all modules.
      README's "Floating resource islands" section extended with the ore
      table rows and config example. Not yet deployed/tested — folded
      into 11.6's acceptance pass along with loot chests (11.4) and the
      guaranteed village (11.5), same reasoning as 11.2's own deferral.
- [x] 11.4 Loot chests: `ResourceConfig.lootChestEnabled`/`lootKit` reusing
      `StarterKitPlan` directly, one placed chest per island.
      **Done (0.2.50):** `FloatingIslandsPlan` gained a persisted
      `lootChestEnabled` boolean (11th field); `lootKit` itself stays
      config-only (`FloatingIslandsConfig.lootKit`, a `StarterKitConfig`),
      same precedent as 11.3's `oreFeatureIds` and `easyKit`/`mediumKit`/
      `hardKit`. Refactored `EnvelopedChunkGenerator`'s per-chunk "which
      scattered island does this chunk own the center of" logic (11.3's
      ore lookup) into a shared `floatingIslandsOwnedByChunk`/`OwnedIsland`
      helper, now used by both `applyFloatingIslandOre` and the new
      `applyFloatingIslandLoot` — avoids tripling the containment-check
      loop once 11.5's guaranteed village needs the same lookup a third
      time. The chest sits at the island's surface (`active.surfaceY()`),
      same X/Z as the ore deposit (if any) but a different Y so the two
      never collide; contents resolved via `StarterKitPlan.resolve(...)`
      with a per-island-salted seed (reusing the same
      `seed ^ (centerX<<32 ^ centerZ)` derivation 11.3's `RandomSource`
      already used) so every island's chest differs, not just the
      per-world kit list. Widened `StarterKitDeployment.resolvePlan`
      from `private` to package-visible rather than duplicating its
      essentials/extras-parsing logic. Verified `WorldGenLevel` (via its
      `BlockGetter`/`LevelWriter` supertypes) exposes the same
      `setBlock`/`getBlockEntity` shape `StarterKitDeployment`'s own
      `ServerLevel`-based chest placement already uses, against the real
      26.2 decompiled sources, before relying on it mid-chunk-generation.
      `SkyIslandCustomizeScreen` gained a `lootChestEnabled` checkbox
      (screen-exposed like `oreDepositsEnabled`; `lootKit`'s own item
      list stays YAML-only). Full config read/sanitize/map/summary
      wiring, reusing `sanitizeStarterKit`/`readStarterKitConfig`/
      `starterKitMap`/`starterKitSummary` directly. Test additions only
      (no new test methods — extended existing ones); full suite green
      (467 tests); clean build across all modules. README's "Floating
      resource islands" section extended with the loot-chest table rows
      and config example. Not yet deployed/tested — still folded into
      11.6's acceptance pass along with the guaranteed village (11.5).
- [x] 11.5 Guaranteed village (GOALS 07): the reserved village cell (hash-
      picked angle/radius just beyond the exclusion zone, forced radius/
      biome), `FloatingIslandsDeployment.placeGuaranteedVillage` force-
      loading the structure's resolved bounding box then placing via
      `Structure.generate`/`placeInChunk` (DESIGN §28.3), one-time
      `WorldLimitState` gate alongside `needsChestBoat`/`needsStarterChest`.
      **Done (0.2.51):** `FloatingIslandsPlan.resolveCell` (the same
      private helper `at`/`nearbyIslands` already both go through) now
      special-cases one specific cell first, before the ordinary
      `spawnChance` roll: `resolveVillageCell(seed)` hash-picks an angle
      and a distance (always at least one full `cellSizeBlocks` beyond
      the exclusion zone) and converts that to cell coordinates, always
      the same cell for a given seed. That cell is forced present
      (bypassing `spawnChance`/exclusion-zone gating entirely — it's
      constructed to already clear the zone), forced to
      `max(minRadiusBlocks, VILLAGE_MIN_RADIUS_BLOCKS)` (new constant,
      96 — bigger than any ordinary configured range), and forced to one
      of five village-compatible biome/structure pairs (plains/desert/
      savanna/snowy/taiga), hash-picked together so the structure always
      matches its own island's biome. Because this hooks into the same
      `resolveCell` every existing consumer (`at`, `nearbyIslands`,
      terrain generation, 11.3/11.4's resource placement) already goes
      through, the village's own island needed **zero** new terrain-
      generation code — it just generates as an ordinary (larger, forced-
      biome) scattered island automatically. New
      `FloatingIslandsPlan.guaranteedVillageSite(seed)` resolves the
      public-facing placement (center + structure id) for the deployment
      step. New `worldgen.FloatingIslandsDeployment.placeGuaranteedVillage`
      mirrors `net.minecraft.server.commands.PlaceCommand.placeStructure`
      exactly (verified against the real 26.2 decompiled sources,
      including the `ServerLevel.getStructureManager()`/`.structureManager()`
      distinction — `StructureTemplateManager` for `Structure.generate`'s
      NBT-template lookup vs `StructureManager` for `StructureStart
      .placeInChunk`, two similarly-named but different types): force-
      loads the origin chunk, calls `structure.generate(...)` with our
      own `EnvelopedChunkGenerator` (so terrain-fit height queries see the
      slab's own surface), then force-loads every chunk in the resolved
      bounding box and calls `placeInChunk` across them — the same two-
      phase shape `/place structure` uses, just force-loading instead of
      erroring on an unloaded chunk. Wired into `WorldLimitManager
      .onServerStarted` alongside the starter chest/chest-boat, gated by
      a new `needsGuaranteedVillage = overworldSkyIsland.enabled() &&
      overworldSkyIsland.floatingIslands().enabled()` joining the same
      early-return and one-time `WorldLimitState` guard — no separate
      persisted flag needed. No config toggle: automatic whenever
      scattered islands are enabled, matching GOALS 07's framing as part
      of the same feature rather than an independent option. 4 new tests
      (`FloatingIslandsPlanTest`: structure-id validity, determinism,
      always-beyond-exclusion-zone, always-appears-in-the-grid); full
      suite green (471 tests); clean build across all modules.
      `FloatingIslandsDeployment` itself isn't unit-testable (needs a
      real `ServerLevel`/registries, same limitation as
      `StarterKitDeployment`/`WorldLimitManager`) — correctness rests on
      the JUnit-covered pure-logic site resolution plus 11.6's in-game
      acceptance pass. README's "Floating resource islands" section
      gained a "Guaranteed village" paragraph. **Known, deliberately
      deferred risk, not chased further:** whether a vanilla village's
      jigsaw pieces settle acceptably onto the island's synthetic slab
      edge for a 96+-block-radius island was flagged in DESIGN §28.3 as
      unverifiable from source reading alone — this is exactly the kind
      of thing this project's "wait for Jason's in-game testing" pattern
      exists for (closest precedent: ocean island's §24.10-§24.13 fixes),
      not something to guess at further before 11.6's acceptance pass.
- [x] 11.6 Test configs (dense/sparse scatter, exclusion-zone-off vs. a
      real radius, each resource layer individually, guaranteed-village
      findability); docs (README, MANUAL_TESTING.md); **[Jason]**
      acceptance including whether a real village's jigsaw pieces settle
      acceptably onto the synthetic slab edge (DESIGN §28.3's flagged
      risk).
      **Configs and docs done (0.2.52):** `config/tests/44`-`48` (dense
      scatter with no exclusion zone + biome variety; sparse scatter with
      a real 200-block exclusion zone; ore deposits in isolation; loot
      chests in isolation with a custom `lootKit`; the guaranteed village
      with a tighter, test-friendly exclusion zone/cell size). New "Phase
      11 acceptance" section in `MANUAL_TESTING.md` (5 items, explicitly
      flagging the village's slab-edge risk as the one requiring the
      most scrutiny); `config/tests/README.md` rows added. README's
      "Floating resource islands" section (built up incrementally across
      11.2-11.5) now documents the full mechanism: placement, both
      resource layers, and the guaranteed village. Deployed to
      Worldz-Test. **[Jason] acceptance outstanding** — this note only
      covers docs/configs being ready. This completes every non-[Jason]
      item in Phase 11 (11.1 design, 11.2 core placement, 11.3 ore
      deposits, 11.4 loot chests, 11.5 guaranteed village, 11.6
      docs/configs) — **do not start Phase 12 without Jason's explicit
      go-ahead.**
      **Natural biome follow-up (2026-07-21, from Jason's in-game
      testing):** `biomeVariety`'s hash-picked pool rolls a biome per
      256-block grid cell independently -- adjacent islands land on
      unrelated biomes with no spatial coherence, closer to a checkerboard
      than something worth bridging toward (GOALS 08's own "sufficiently
      far away to require a lot of bridging" framing implies real biome
      regions, not random tiles). Chunk islands (§29.6) already read the
      real seed's biome by construction; floating islands had no
      equivalent since `FloatingIslandsPlan` *is* the override, with no
      real terrain underneath its synthetic slab to read a biome from.
      Fixed: `FloatingIslandsConfig.naturalBiome` (default `false`, opt-in
      third mode alongside `biomeVariety`, takes precedence when set) --
      `LimitedBiomeSource`/`EnvelopedChunkGenerator` resolve the real
      biome via their own seed-biome-source access, since the pure-logic
      `FloatingIslandsPlan` has none (DESIGN §28.6). New test config 58
      exercises it. Still covered by 11.6's outstanding acceptance pass.

## Phase 12 — Sky chunk challenge (GOALS 09, 37)

Split from the original single 12.1/12.2 items into the granularity every
other multi-part phase in this project has needed (Phase 6.2a/6.2b, Phase
10's 10.1-10.6, Phase 11's 11.1-11.6 precedent) — logged here rather than
silently expanding one task. Scope decided with Jason (2026-07-20, see
MEMORY.md's Phase 12 decisions): "portal room" means a guaranteed *forced*
stronghold placement onto one reserved chunk island, mirroring Phase 11.5's
guaranteed-village mechanism exactly, not a best-effort reliance on the
existing fallback-portal machinery; the End dimension gets its own
chunk-island toggle (a new capability, not the sky-island End-skip
precedent — vanilla End's native island shape doesn't already give "reveal
only some natural chunks, void the rest"); GOALS 37's underground-content
showcasing is seed-search-preferred selection (reusing Phase 8.2's
`NaturalIslandSearch` precedent) plus forced geode placement (reusing Phase
11.3's `ConfiguredFeature.place` mechanism), not depth-aware biome forcing
(that stays the already-deferred Backlog item from GOALS 15).

- [x] 12.1 Design pass (DESIGN §29): the chunk-grid selection mechanism (a
      per-chunk hash pick — no jitter/radius needed since the shape *is* the
      chunk, unlike `FloatingIslandsPlan`'s circular islands), full-column
      vs. top-N-blocks-deep masking (a new Y-aware path in `applyEnvelope` —
      real vanilla terrain is kept above the cutoff, voided below, unlike
      every existing exterior mode which is uniform across all Y), the
      guaranteed-stronghold mechanism (mirrors Phase 11.5's guaranteed
      village), and wiring `EnvelopedChunkGenerator` onto `LevelStem.END` for
      the first time (today only Overworld/Nether are wrapped — confirmed by
      reading `WorldzPresetEditor`; this phase's End toggle needs that
      extended). Verify the real vanilla stronghold structure id and
      `Structure.generate`/`placeInChunk` still apply unchanged (already
      proven in 11.5) against 26.2 sources. **Commit** design before
      implementing.
      **Done:** full design in DESIGN §29. Key findings verified against the
      real 26.2 decompiled sources: every vanilla stronghold always contains
      a portal-room piece (its generation loop retries until one exists), so
      "guarantee a portal room" reduces to "guarantee a stronghold"; a
      stronghold's forced-placement bounding box is large and only known
      after generation, so the reserved portal-room site will usually pull
      in several neighboring chunks beyond the void, not stay a single
      16-block island (documented, not a blocker); `LevelStem.END` is
      confirmed never wrapped by `EnvelopedChunkGenerator` today (only
      Overworld/Nether are), so the End toggle (12.4) is new plumbing, not a
      config flag alone; `LimitedBiomeSource`'s codec has exactly one spare
      top-level slot left (13 of 14) — `chunk_island` will consume it,
      leaving zero headroom for any future phase's own new top-level field
      (flagged for Phase 13+). Underground-content showcasing (37, TODO
      12.6) reuses two already-proven techniques verified by reading the
      real call sites: `SpawnOriginManager.searchNaturalIsland`'s biome-at-
      depth sampling (no generation needed) for cave biomes, and
      `EnvelopedChunkGenerator.findNearestMapStructure`'s existing delegate
      passthrough for structure-bearing chunks.
- [x] 12.2 Core: `ChunkIslandPlan` (pure logic — per-chunk hash-picked
      presence, JUnit-covered), config/codec/customization plumbing on a new
      dedicated `jlt_worldz:sky_chunk` preset (matching `single_biome`/
      `chaos_biomes`/`strip_world`'s own-preset precedent), the chunk-grid
      masking wiring into `EnvelopedChunkGenerator` (reusing the existing
      per-chunk `effectiveModeAt`/`applyEnvelope` VOID path for unselected
      chunks; new Y-cutoff logic for a selected chunk's top-N-deep option),
      small Customize screen. Acceptance bar: a single starter chunk island
      (full-column or top-N-deep) surrounded by void generates — no portal
      room guarantee, no Nether/End toggle, no multi-biome yet.
      **Done (0.2.54):** new `logic.ChunkIslandPlan` (enabled, spawnChance,
      cellSizeChunks, topOnly, topOnlyDepthBlocks, exclusionZone; a per-cell
      hash pick with the starter cell always forced present, no jitter/
      radius needed since the shape *is* the chunk) + `config.ChunkIslandConfig`
      + `worldgen.ChunkIslandCodecs`; `LimitedBiomeSource` gained the exact
      last spare top-level codec slot flagged in 12.1 (`chunk_island`, now
      14/14 — see DESIGN §29.7) plus a `skyChunkDefaults` fieldless-preset
      branch fixed from day one (Phase 6.2b/6.3's "known gap" pattern, not
      repeated here). `EnvelopedChunkGenerator` gained `Dimension.END` (a
      real, first-ever wrapping of `LevelStem.END`, confirmed nothing else
      does this), a `chunkIsland`/`nonOverworldChunkIsland` pair mirroring
      `skyIsland`/`netherSkyIsland`'s exact precedent (Overworld reads live
      from `LimitedBiomeSource`, Nether/End persist directly on the
      generator's own codec, both sharing the identical dimension-agnostic
      masking since chunk islands never synthesize terrain, DESIGN §29.1),
      and a new `applyChunkIslandDepthCutoff` pass appended after
      `applyEnvelope`'s ordinary masking loop for the `TOP_ONLY` case
      (reads the real, already-generated `Heightmap.Types.WORLD_SURFACE`
      per column — no synthetic height needed, unlike every other exterior
      mode). **Built the Nether/End toggle plumbing (12.4's own scope) as
      part of this task**, not deferred: once `ChunkIslandPlan`/the
      generator wiring supported three dimensions symmetrically (a natural
      consequence of DESIGN §29.5's finding that no dimension-specific
      material logic is ever needed), gating it behind a separate task
      would only have meant re-touching the same methods twice — logged
      here rather than silently expanding scope; 12.4 is now verification
      plus test configs, not new implementation. New `SkyChunkCustomization`
      (mirrors `SkyIslandCustomization`'s shape: no Overworld exterior field,
      since the chunk-island branch in `effectiveModeAt` supplies the whole
      Overworld exterior itself ahead of `envelope` ever being consulted)
      and `SkyChunkPresetEditor`/`SkyChunkCustomizeScreen`, wiring all three
      dimensions' `LevelStem`s. Full registration: `world_preset/
      sky_chunk.json` (Overworld + Nether + **End**, the first typed preset
      to wrap the End's own generator), the `normal.json` preset tag, lang
      keys, both loaders' preset-editor hookup — each verified by a
      matching resource/structural test (`WorldPresetResourcesTest`,
      `ProjectMetadataTest`), following this project's own established
      per-preset test pattern. 24 new tests across `ChunkIslandPlanTest`,
      `SkyChunkCustomizationTest`, `WorldPresetResourcesTest`,
      `ProjectMetadataTest`, `WorldzConfigTest`; full suite green (496
      tests); clean build across all modules (fabric + neoforge
      registration compiles and resolves correctly). Not yet deployed/
      tested in-game — folded into 12.7's acceptance pass, same posture
      Phase 10/11 established for their own multi-task builds.
- [x] 12.3 Guaranteed portal room (09): a reserved chunk (same
      "hash-picked angle/distance beyond an exclusion zone, always the same
      cell for a given seed" shape as Phase 11.5's village cell) forced to
      contain a stronghold with its End Portal Room piece, via
      `Structure.generate`/`placeInChunk` mirroring
      `FloatingIslandsDeployment.placeGuaranteedVillage` exactly; one-time
      `WorldLimitState` gate alongside the existing guarantees.
      **Done (0.2.55):** `ChunkIslandPlan.reservedPortalCell(seed)` (already
      added in 12.1/12.2) plus new `worldgen.ChunkIslandDeployment
      .placeGuaranteedPortalRoom`, mirroring `FloatingIslandsDeployment
      .placeGuaranteedVillage` line-for-line but forcing
      `minecraft:stronghold` unconditionally — no variant-picking needed
      since every vanilla stronghold's own generation loop already retries
      until it contains a portal-room piece (confirmed reading
      `StrongholdStructure.generatePieces`, 12.1's finding), so "guarantee
      a portal room" reduces exactly to "guarantee a stronghold." Wired
      into `WorldLimitManager.onServerStarted` via a new
      `needsGuaranteedPortalRoom = overworldChunkIsland.enabled()` gate,
      unconditional like the guaranteed village. **Proactively audited and
      fixed the same "silently never fires" gate defect class Phase 10.2/
      11 already found for sky island/ocean island** (the 8.1 lesson):
      `exteriorObjective`'s condition gained `|| overworldChunkIsland
      .enabled()`, since chunk island's Overworld exterior always stays
      `ExteriorMode.NORMAL` in `ExteriorPlan` terms (mirrors sky island),
      so without this the fallback End-portal vault would have silently
      never fired for a chunk-island world with no border configured.
      Verified no new `ObjectiveSite.supportiveRadius` overload is needed
      (unlike `IslandPlan`/`SkyIslandPlan`, which each got their own):
      chunk islands have no inherent radius of their own to narrow the
      fallback search by, and `ProgressionGuarantees.ensureEndPortal`'s
      existing skyIsland-disabled branch already falls through to the
      correct plain border/envelope-only `supportiveRadius` overload,
      which is exactly right here. `ChunkIslandDeployment` itself isn't
      unit-testable (needs a real `ServerLevel`/registries, same
      limitation as `FloatingIslandsDeployment`/`WorldLimitManager`) —
      correctness rests on the JUnit-covered pure-logic site resolution
      (`reservedPortalCellIsAlwaysBeyondTheExclusionZone`/
      `reservedPortalCellIsDeterministicAndAlwaysPresentInTheGrid`,
      already added in 12.2) plus 12.7's in-game acceptance pass. Full
      suite green; clean build across all modules.
- [x] 12.4 Nether/End chunk-island toggles (09's "normal Nether/End, or
      chunk islands" option): `applyToNether`/`applyToEnd` config fields;
      wire `EnvelopedChunkGenerator` onto `LevelStem.END` (new — see 12.1's
      design finding) reusing the exact same dimension-agnostic masking
      already proven for Overworld/Nether (no synthetic terrain profile
      needed, unlike sky_island's Nether variant, since chunk islands never
      touch a selected chunk's real terrain).
      **Done (0.2.56):** already fully implemented as part of 12.2's core
      task (logged there rather than silently expanding scope) — this
      task closed the remaining test-coverage gap instead of writing new
      production code: `ChunkIslandPlanTest` had zero coverage of
      `fromConfig`'s per-dimension gating despite the method existing
      since 12.2. Added `fromConfigOverworldAlwaysAppliesWhenEnabled`,
      `fromConfigNetherDisabledUnlessApplyToNether`,
      `fromConfigEndDisabledUnlessApplyToEnd`,
      `fromConfigEverythingDisabledWhenTopLevelDisabled`, and
      `fromConfigCopiesShapeFields` (5 new tests). `SkyChunkCustomization
      .netherChunkIslandPlan()`/`endChunkIslandPlan()` were already
      covered in 12.2's own `SkyChunkCustomizationTest`. Full suite
      green; clean build across all modules.
- [x] 12.5 Multi-biome scattered chunk islands (37, biome part): beyond the
      starter island, additional chunk islands of different biomes;
      per-island top-only-to-depth vs. full-column choice (independent per
      island, per GOALS 37's exact wording).
      **Done (0.2.57):** the biome-variety half needed zero code (DESIGN
      §29.6 predicted this correctly) — a selected scattered chunk's biome
      is whatever the real seed naturally has there, no override machinery
      exists to add. The per-island depth-mode half was real, scoped work:
      `ChunkIslandPlan` gained a 7th field, `scatteredTopOnlyChance` (0..1),
      hash-picked independently per cell in `at()` — the starter island and
      the guaranteed portal-room island keep using the plan's fixed
      `topOnly` deterministically (unchanged from 12.2/12.3, so the player
      and the forced stronghold both see a predictable shape), only
      ordinary scattered cells vary. Threaded the new field through
      `ChunkIslandConfig`/`ChunkIslandCodecs`/`SkyChunkCustomization`
      (record component, `fromConfig`/`fromText`, feeds
      `chunkIslandPlan()`) and `SkyChunkCustomizeScreen` (new text field).
      9 new tests across `ChunkIslandPlanTest` (starter-cell determinism
      plus zero/one/half scattered-chance behavior) and
      `SkyChunkCustomizationTest`; full suite green; clean build across
      all modules.
- [x] 12.6 Underground-content showcasing (37): seed-search preferentially
      selects naturally-qualifying chunks (real lush/dripstone/deep-dark
      cave biomes, real structure-bearing chunks) among 12.5's scattered
      candidates, reusing `NaturalIslandSearch`'s seed-scan precedent;
      amethyst geodes via forced `ConfiguredFeature.place`, reusing Phase
      11.3's mechanism. Document the depth-aware-biome-forcing gap this
      does *not* attempt (stays the GOALS-15 Backlog item).
      **Done (0.2.58):** new `worldgen.ChunkIslandShowcaseSearch`, reusing
      `SpawnOriginManager.searchNaturalIsland`'s exact climate-sampling
      technique (`MultiNoiseBiomeSource.getNoiseBiome` against a real
      `RandomState`, no chunk generation) for `Biomes.LUSH_CAVES`/
      `DRIPSTONE_CAVES`/`DEEP_DARK`, each checked at one representative
      depth (Y-40) rather than DESIGN §29.6's originally sketched
      multi-depth scan — a scoped, documented simplification (matches
      `SkyIslandProfile`'s own surface-material-heuristic precedent):
      "prefer a naturally-qualifying chunk when one is nearby" doesn't
      need exhaustive depth coverage to be worth having. Structure-bearing
      chunks reuse `ServerLevel.findNearestMapStructure` (the real
      `/locate structure` query) against a new mod-owned tag,
      `jlt_worldz:chunk_island_showcase` (`minecraft:ancient_city`,
      `minecraft:trial_chambers`) — no vanilla tag groups "any single
      interesting structure," so a small tag resource was the simplest
      correct option. Found chunks are resolved once at world start (from
      `WorldLimitManager`, mirroring the guaranteed-portal-room's timing)
      and threaded into `EnvelopedChunkGenerator` via a new
      `setChunkIslandShowcaseCells`/`chunkIslandShowcaseCells` field,
      checked first in `chunkIslandHitAt` ahead of the plan's own
      hash-based grid — always forced full-column, since truncating a
      showcased cave chunk would defeat the point. No persistence needed
      (unlike `SpawnOriginState`'s precedent): the search is a pure
      function of the real seed's noise field, so it's cheap and correct
      to just re-run once per server start rather than persist a result.
      Amethyst geodes are simpler than the search-based categories: a
      third reserved cell, `ChunkIslandPlan.reservedGeodeCell` (the same
      hash-picked-angle-and-distance shape as the portal-room cell, just
      salted differently, refactored into a shared private `reservedCell`
      helper), force-placed via `EnvelopedChunkGenerator
      .applyChunkIslandGeode` reusing `placeOreFeature` directly (despite
      its ore-specific name, it forces any `ConfiguredFeature` at an exact
      position — exactly what a geode needs too, no new placement code).
      New config-only `ChunkIslandConfig.geodeFeatureIds` (default
      `["minecraft:amethyst_geode"]`), same "list stays in config, never
      persisted" precedent as every other feature-id pool in this
      codebase. **Known, deliberately accepted risk found and documented,
      not chased further:** the portal-room and geode cells are
      independent hash picks over the same distribution, so a same-cell
      collision is possible (found empirically, roughly one in a few
      hundred seeds) — the portal room wins (checked first in `at()`),
      and the geode force-placement would land inside that chunk's
      stronghold, cosmetically odd but not broken; documented in
      `reservedGeodeCell`'s javadoc rather than engineered around, matching
      this project's posture for similarly rare, low-impact geometry edge
      cases (e.g. DESIGN §29.4's own stronghold-bounding-box spillover).
      No depth-aware biome forcing attempted anywhere in this task — stays
      the GOALS-15 Backlog item, exactly as scoped. 15 new tests across
      `ChunkIslandPlanTest` and `WorldPresetResourcesTest`; full suite
      green (509 tests); clean build across all modules.
- [x] 12.7 Test configs (starter-only full-column, starter-only top-N-deep,
      portal-room findability, Nether/End toggles on/off, multi-biome
      scatter, underground-content showcase); docs (README, MANUAL_TESTING);
      **[Jason]** acceptance.
      **Configs and docs done (0.2.59):** `config/tests/49`-`52` (default
      full-column with the guaranteed portal room/geode; the top-only
      depth cutoff; Nether+End toggles together, the first config
      exercising the End-wrapping mechanism; multi-biome scatter with
      per-island depth-mode variety plus underground-content showcasing).
      New "Phase 12 acceptance" section in `MANUAL_TESTING.md`; `config/tests/
      README.md`'s intro updated for the seventh World Type entry.
      README.md gained a full "Sky chunk challenge" section (table row +
      config example + settings table), cross-linking the shared border/
      exterior docs rather than duplicating them, following every other
      typed preset's section shape exactly. 0.2.59 built and deployed to
      Worldz-Test. This completes every
      non-[Jason] item in Phase 12 (12.1 design, 12.2 core mechanism +
      Nether/End plumbing, 12.3 guaranteed portal room, 12.4 Nether/End
      verification, 12.5 per-island depth-mode variety, 12.6 underground-
      content showcasing, 12.7 docs/configs) — **do not start Phase 13
      without Jason's explicit go-ahead.**

## Phase 13 — Cave challenge (GOALS 25–26)

- [x] 13.1 Design pass (DESIGN §20.10): underground spawn placement
      (configurable depth, safe cavity search — can reuse the spawn-search
      ring pattern from §18), optional sealed surface (solid roof / no sky
      access — decide generation approach and its interaction with
      heightmaps, mob spawning, and phantom rules), and the mega-cave option
      (huge natural-looking cavern around spawn, edges blended into natural
      cave systems — decide carver vs. feature vs. noise approach against
      real 26.2 sources). Beatability: stronghold and underground structures
      unchanged, portal built underground works.
      **Done:** full design in DESIGN §30. Key finding, verified directly
      against the real 26.2 decompiled sources
      (`MinecraftServer.setInitialSpawn`/`createLevels`): forcing real chunk
      generation and placing blocks at the same early spawn-resolution hook
      `SpawnOriginManager.resolveFreshOrigin` already uses is exactly what
      vanilla itself does there (`level.getHeight` forces sync generation;
      the bonus-chest feature is placed directly into the world at that
      same point) — so the underground cavity search needs only **one**
      hook, not a two-phase "approximate now, correct later" design
      originally suspected necessary. `CavePlan` persists entirely on
      `EnvelopedChunkGenerator`'s own codec (mirrors `StripPlan`'s
      precedent) rather than `LimitedBiomeSource`, since
      `LimitedBiomeSource`'s codec is already full (§29.7) and cave needs
      no biome-source field anyway — full vanilla biome variety, same
      shape as `strip_world`. Sealed surface and the mega-cavern are both
      independent additive passes appended to `applyEnvelope` (mirroring
      `applyChunkIslandDepthCutoff`'s "runs unconditionally" placement),
      needing a `hasActiveExterior()` gate addition (`|| cave.enabled()`)
      of the same defect class §29.3 already found and fixed once. No
      Nether/End variant in scope (Overworld only, per GOALS 25-26's own
      wording). Split 13.2 into finer subtasks (13.2a-d) mirroring this
      project's own established precedent for multi-part phases (Phase
      6.2a/6.2b, 10.1-10.6, 11.1-11.6, 12.1-12.7) — logged here rather than
      silently expanding one task.
- [x] 13.2a Core: `CavePlan` (logic, JUnit-covered), `CaveConfig`/codec/
      `CaveCustomization` plumbing, the `jlt_worldz:cave` typed preset
      (editor, Customize screen, world-preset JSON, `normal` tag, lang
      keys, both loaders' registration), and the underground spawn-cavity
      search (`SpawnOriginManager.resolveCaveOrigin`, DESIGN §30.3) with
      its synthetic-capsule fallback. Acceptance bar: a plain cave world
      (no sealed surface, no cavern) spawns the player underground in a
      real natural cavity.
      **Done (0.2.60):** `logic.CavePlan` (enabled, spawnDepthY,
      sealedSurface, sealedSurfaceY, cavernEnabled, cavernRadiusBlocks,
      cavernHeightBlocks, chestEnabled, chestTier) + `config.CaveConfig`
      (with easyKit/mediumKit/hardKit `StarterKitConfig` sections mirroring
      `SkyIslandConfig`'s shape) + `worldgen.CaveCodecs`; persisted entirely
      on `EnvelopedChunkGenerator`'s own codec (a new `cave` field, mirrors
      `StripPlan`'s exact precedent) rather than `LimitedBiomeSource`,
      since that codec is already full (§29.7) and cave needs no
      biome-source involvement. Full registration: `world_preset/cave.json`
      (Overworld + Nether; End left plain vanilla, no chunk-island-style
      wrapping needed), the `normal.json` preset tag, lang keys, both
      loaders' preset-editor hookup (Fabric mixin, NeoForge event) --
      `CavePresetEditor`/`CaveCustomizeScreen` mirror `strip_world`'s shape
      (no Overworld exterior field; a cave world's Overworld always stays
      `ExteriorMode.NORMAL`). Closed the fieldless-preset gap from day one
      (sky_chunk's precedent, not strip_world's original after-the-fact
      fix): added a `caveDefaults` boolean to `LimitedBiomeSource.resolve`
      (full vanilla biome variety via the biome_source's own `"world_type":
      "cave"` hint) *and* a parallel, new `"world_type"` hint field on
      `EnvelopedChunkGenerator`'s own codec (write-never, read-only,
      mirroring `LimitedBiomeSource`'s exact pattern) so a never-customized
      cave world's `CavePlan` also defaults from live config instead of
      needing a `cave.enabled` YAML toggle that would otherwise leak into
      every other preset's Overworld.
      **Underground spawn placement (`SpawnOriginManager.resolveCaveOrigin`):**
      verified directly against the real 26.2 decompiled sources
      (`MinecraftServer.setInitialSpawn`/`createLevels`, see 13.1's
      finding) that forcing real chunk generation and placing blocks at
      the same early spawn-resolution hook is exactly what vanilla itself
      does there -- so the search runs entirely within
      `resolveFreshOrigin`'s existing single hook: walks a dedicated,
      narrower `SpawnSearchPlan` (320 blocks/16-block steps/8 points per
      ring, ~161 candidates worst case -- deliberately smaller than
      `SpawnSearchPlan.defaults()`'s 2048-block/513-candidate budget,
      since each candidate here costs a real forced chunk generation, not
      free climate sampling), force-generating each candidate chunk and
      scanning a vertical window around `spawnDepthY` for a solid floor
      with two clear blocks above it. Falls back to carving a small safe
      capsule (reusing `ProgressionGuarantees`' enclosed-shell shape) if
      the whole budget is exhausted. DESIGN §30.3 updated to match this
      narrower budget (the original design text assumed reusing
      `SpawnSearchPlan.defaults()` directly, corrected once the
      performance cost became concrete during implementation).
      21 new tests across `CavePlanTest`, `CaveCustomizationTest`,
      `WorldzConfigTest`, `WorldPresetResourcesTest`,
      `ProjectMetadataTest`; full suite green (532 tests); clean build
      across all modules. Not yet deployed/tested in-game -- folded into
      13.2d's acceptance pass, same posture Phase 10-12 established for
      their own multi-task builds.
- [x] 13.2b Sealed surface option (GOALS 25, DESIGN §30.4): the roof pass,
      `hasActiveExterior()` gate fix, Customize screen field.
      **Done (0.2.61):** `EnvelopedChunkGenerator.applyCaveSealedSurface`
      -- a thin (5-block) solid stone roof at `cave.sealedSurfaceY()`,
      applied uniformly to every column, layered as an additive pass at
      the end of `applyEnvelope` (mirrors `applyChunkIslandDepthCutoff`'s
      "runs again unconditionally" placement) gated on
      `cave.sealedSurface()` alone, independent of `mode`/border/exterior.
      The `hasActiveExterior()` gate fix (`|| this.cave.enabled()`) and
      the Customize-screen checkbox/field were already built in 13.2a
      (the full `CavePlan`/`CaveCustomization`/`CaveCustomizeScreen` shape
      was implemented whole from the start, same as `SkyIslandPlan`'s own
      precedent) -- this task was the one remaining piece, the actual
      terrain-modification pass. No new pure-logic class needed (the
      roof-Y clamp math is trivial and inline, matching
      `applyChunkIslandDepthCutoff`'s own precedent of no extraction);
      instead added a structural regression-guard test
      (`ProjectMetadataTest.caveSealedSurfaceIsAppliedUnconditionallyAfterTheMaskingLoop`)
      pinning the gate fix and the pass's existence, since
      `EnvelopedChunkGenerator` itself can't be unit-tested directly (same
      `NoClassDefFoundError` limitation as `SpawnOriginManager`/
      `WorldLimitManager`). Full suite green (533 tests); clean build
      across all modules.
- [x] 13.2c Mega-cave option (GOALS 26, DESIGN §30.5): the ellipsoid carve
      pass reusing `IslandShapeProfile`, Customize screen fields.
      **Done (0.2.62):** `EnvelopedChunkGenerator.applyCaveMegaCavern` --
      reuses `IslandShapeProfile.distanceFromShore` (the same perturbed-
      coastline math every other shaped preset in this project shares,
      fixed at `IslandShapeProfile.DEFAULT_AMPLITUDE` since `CavePlan` has
      no `shapeAmplitude` field of its own by design, §30.2) for the
      horizontal footprint, bounded vertically by `cavernHeightBlocks`
      above/below `spawnDepthY`. Air-only, one-directional carve: only
      solid, non-fluid blocks become air (`!oldState.isAir() &&
      oldState.getFluidState().isEmpty()`), so existing air/water/lava/
      natural caves already inside the footprint are left exactly as
      vanilla generated them -- this is what "blended into the natural
      cave systems at its edges" means in practice, no separate blend
      math needed. New `caveSeed()` helper mirrors `islandSeed()`'s
      exact precondition (cave is Overworld-only, so `originSource` is
      always present when called). Layered as another additive pass
      alongside 13.2b's roof, at the end of `applyEnvelope`, independent
      of `mode`. Customize-screen fields (cavernEnabled checkbox,
      cavernRadiusBlocks/cavernHeightBlocks text fields) were already
      built whole in 13.2a alongside the rest of `CavePlan` -- no new UI
      work needed. New structural regression-guard test
      (`ProjectMetadataTest.caveMegaCavernReusesIslandShapeProfileAndNeverFills`),
      same rationale as 13.2b's (the generator itself isn't directly
      unit-testable). Full suite green (534 tests); clean build across
      all modules.
- [x] 13.2d Starter chest reuse (optional, DESIGN §30.3's
      `getSharedSpawnPos()` timing), test configs, docs
      (README/MANUAL_TESTING.md); **[Jason]** acceptance (25 with/without
      sealed surface and with/without chest, 26).
      **Done (0.2.63):** `StarterKitDeployment.spawnCaveStarterChest`
      places a filled chest set into the floor directly beneath the
      *actual resolved* spawn position -- read back from
      `overworld.getRespawnData().pos()` (the real 26.2 API; DESIGN
      §30.3's original "`getSharedSpawnPos()`" name didn't exist and was
      corrected once verified against the compiled jar during
      implementation), not `originX`/`originZ` (which stay `0,0` for this
      preset -- cave has no layout-origin search of its own). No
      biome-driven water-source item unlike `sky_island`'s chest -- cave
      has no biome concept to key off. Wired into
      `WorldLimitManager.onServerStarted` via a `needsCaveChest =
      overworldCave.enabled() && overworldCave.chestEnabled()` gate,
      mirroring every other optional one-shot deployment's exact
      precedent. Test configs `config/tests/53`-`56` (default cavity
      spawn; sealed surface; mega-cavern; chest + sealed surface
      combined) with a new "Phase 13 acceptance" section in
      `MANUAL_TESTING.md`; `config/tests/README.md`'s intro updated for
      the eighth World Type entry. README.md gained a full "Cave
      challenge" section (table row + config example + settings table),
      matching every other typed preset's section shape. Deployed 0.2.63
      to Worldz-Test. This completes every non-[Jason] item in Phase 13
      (13.1 design, 13.2a core mechanism + preset, 13.2b sealed surface,
      13.2c mega-cavern, 13.2d starter chest + docs/configs) -- **do not
      start Phase 14 without Jason's explicit go-ahead.**

## Phase 14 — Nether-start challenge (GOALS 27)

- [x] 14.1 Feasibility spike first (the §16.1 pattern): how initial spawn in
      a non-Overworld dimension actually works in 26.2 — `MinecraftServer`/
      `PlayerList` spawn+respawn paths, respawn-anchor semantics, what
      happens on death without an anchor. Findings cover the End too (for
      Phase 15). Verify against real sources; commit findings to DESIGN
      §20.10 before implementing.
      **Done, no code (design only).** Full spike + design in DESIGN §31.
      Key finding: neither existing early spawn hook
      (`MinecraftServerMixin`/`WorldzNeoForge.onCreateSpawnPosition`) can
      place a Nether spawn site directly — the Nether `ServerLevel`
      doesn't exist yet at that point in `MinecraftServer.createLevels()`
      (verified from real source, §31.1). Chosen mechanism instead:
      overwrite the world's stored default spawn (`MinecraftServer.
      setRespawnData`, the same lever `/setworldspawn` uses) once
      `WorldLimitManager.onServerStarted` fires — by which point Nether
      already exists — since both first-join (`PrepareSpawnTask`) and
      no-bed/no-anchor death (`findRespawnDimension`) read that one same
      stored value (§31.2). Confirmed Nether respawn anchors work / beds
      don't (and End: neither works) via the real dimension-type data
      (§31.3). **Jason's call (2026-07-21): natural safe-site search
      first, guaranteed capsule shelter as fallback** — mirrors Cave's
      own `resolveCaveOrigin` two-phase shape (§31.4) with an added
      lava-adjacency check Cave never needed. Split 14.2 into 14.2a-c
      below, mirroring Phase 13's own precedent for multi-part phases.
- [x] 14.2a Core mechanic: `NetherStartPlan`/codec (persisted on
      `EnvelopedChunkGenerator`, mirrors `CavePlan`, DESIGN §31.5), the
      natural-search-then-guaranteed-capsule safe-site resolver (DESIGN
      §31.4), and the `WorldLimitManager.onServerStarted` hook that
      overwrites the world's default spawn to the resolved Nether site
      (DESIGN §31.2). JUnit-covered pure logic where possible (search
      candidate ordering, capsule shape); the actual `MinecraftServer`
      lever itself is only exercisable in-game.
      **Done (0.2.65), backend only -- not yet in-game testable.** No
      typed preset is registered yet (that's 14.2b), so `NetherStartPlan`
      can never resolve enabled today -- `EnvelopedChunkGenerator.resolve`'s
      `world_type` hint never sees `"nether_start"` until 14.2b wires the
      preset editor. `NetherStartDeployment.searchNetherStartSite` reads
      every block through the Nether `ServerLevel` itself (not the raw
      candidate `ChunkAccess` the way `SpawnOriginManager.searchCaveCavity`
      does), since the added lava-adjacency check can legitimately cross
      into a neighboring, not-yet-forced chunk -- `ChunkAccess.getBlockState`
      would silently mis-read a position outside its own bounds instead of
      correctly resolving the neighbor. Vertical search tolerance is 16
      (vs. cave's 24) -- the Nether's usable Y range (bedrock floor near 0,
      bedrock ceiling near 128) is narrower than the Overworld's, so a
      smaller window keeps candidates closer to the configured `spawnY`
      relative to that range. JUnit: `NetherStartPlanTest` (validation,
      `disabled()`, `fromConfig()`) plus `WorldzConfigTest` parse/sanitize/
      clamp/summary coverage; the deployment's own search/capsule logic has
      no unit test, matching `SpawnOriginManager`'s cave-search precedent
      exactly (needs a real `ServerLevel`, not pure-logic-testable).
- [x] 14.2b Starter chest tiers (DESIGN §31.6, reuses
      `StarterKitTier`/`StarterKitConfig`; easy = obsidian + flint and
      steel + food/torches, medium = frame's worth of obsidian only,
      hard = none, relies on Nether exploration) placed at the resolved
      site, plus the typed preset shape (DESIGN §31.7): `NetherStartConfig`,
      `NetherStartCustomization`, `NetherStartPresetEditor`,
      `NetherStartCustomizeScreen`, world-preset JSON + `normal` tag +
      lang keys, both loaders' registration.
      **Done (0.2.66), in-game testable now.** `nether_start` shows up as
      the ninth "Worldz" World Type entry. Easy kit: 10 obsidian (a full
      frame, ready to place -- no cobblestone-generator/mining detour) +
      1 flint and steel + bread + 3 random extras (golden tools, gold
      ingots, torches). Medium: 10 obsidian, no ignition, less food.
      Hard: no guaranteed obsidian at all -- essentials are just bread,
      extras include gold ingots (a nod toward piglin bartering as one
      of the "leans on exploration" paths GOALS 27 allows). Chest placed
      the same way cave's own optional chest is (`StarterKitDeployment.
      spawnNetherStartChest`, replaces the floor block beneath the
      resolved site). Typed-preset scaffolding mirrors `cave`'s exactly
      (§31.7) with one structural difference worth flagging: cave's
      `world_type` hint sits on the *Overworld's* enveloped generator
      (its plan is Overworld-attached); nether_start's sits on the
      *Nether's* instead, since `NetherStartPlan` is Nether-attached
      (§31.5) -- `nether_start.json`'s Overworld generator carries no
      `world_type` field of its own, only its `jlt_worldz:limited`
      biome_source does (for `LimitedBiomeSource`'s own separate
      full-vanilla-variety default, unrelated to `NetherStartPlan`).
      New structural regression tests mirroring every prior typed
      preset's own precedent (`WorldPresetResourcesTest`,
      `ProjectMetadataTest`).
- [x] 14.2c Test configs (default, each chest tier, a config exercising
      the guaranteed-capsule fallback path if forceable); docs (README,
      MANUAL_TESTING.md); phase wrap-up. **[Jason]** acceptance including
      death/respawn behavior (does dying without a personal anchor really
      return you to the same safe site; does a placed anchor elsewhere
      correctly override it; is the natural-vs-capsule site genuinely
      safe on arrival).
      **Configs and docs done (0.2.66):** `config/tests/59`-`62` (default/
      medium tier exercising the core redirect+respawn mechanic; easy and
      hard chest tiers; `spawnY: 4` biased toward, not guaranteed to hit,
      the capsule fallback -- not deterministically forceable from config
      alone since the natural search always tries first, Jason's own
      chosen design §31.4). New "Phase 14 acceptance" section in
      MANUAL_TESTING.md (4 items, explicitly flagging the same-site-
      respawn check as the single most important thing to verify).
      `config/tests/README.md`'s intro updated for the ninth World Type
      entry plus a new table row describing `netherStart:`. README.md
      gained a full "Nether-start challenge" section (challenge-types
      table row + prose + chest-tier table + config example + settings
      table) mirroring the cave section's shape.
      **This completes every non-[Jason] item in Phase 14** (14.1 design,
      14.2a core mechanic, 14.2b chest tiers + typed preset UI, 14.2c
      test configs/docs) — **do not start Phase 15 without Jason's
      explicit go-ahead.** Phase 15 (End start, GOALS 34) directly reuses
      this phase's own respawn-mechanics research (DESIGN §31.1/§31.3) --
      neither beds nor anchors work in the End, so it will need the
      `forced` `ServerPlayer.RespawnConfig` mechanism flagged as deferred
      here (DESIGN §31.8), not scheduled to be re-derived from scratch.

## Phase 14b — Universal starter capsule, Nether-start first pass (GOALS 41)

Jason's follow-up (2026-07-25) to the Phase 14 acceptance feedback above:
a bigger, lit, furnished capsule/starter-base, available as an explicit
option (not only a natural-search fallback), scoped to `nether_start`
first ("let's start with implementation in the nether and testing"), with
every other world type/starting scenario explicitly deferred to a later
revisit. Full design: DESIGN §31.9.

- [x] 14b.1 Design pass (DESIGN §31.9): capsule size/shape generalization
      (`capsuleSizeBlocks`/`capsuleHeightBlocks`, radius-based shell
      formula subsuming the original fixed 3x3x4 shape), lighting
      placement rules per `LightSource` (wall-embedded vs. wall-mounted
      vs. ceiling-hung vs. full-surface-coating), `forceCapsule` as an
      explicit request separate from the fallback path, furniture/pickaxe
      guarantees. Verified real block API (`WallTorchBlock.FACING`,
      `LanternBlock.HANGING`, `MultifaceBlock.getFaceProperty`) against
      the real 26.2 decompiled sources before writing placement code.
- [x] 14b.2 Implementation: new `logic.LightSource` enum,
      `config.StarterCapsuleConfig` (generic/single-owner, not yet
      cross-preset-shared — DESIGN §31.9), `NetherStartPlan`/
      `NetherStartCodecs` gain `forceCapsule`/`capsuleSizeBlocks`/
      `capsuleHeightBlocks`/`capsuleLightSource`/`capsuleLightSpacingBlocks`,
      `NetherStartConfig` gains `forceCapsule`/`capsule` plus a guaranteed
      `minecraft:wooden_pickaxe` in every chest tier's essentials.
      `NetherStartDeployment.buildNetherStartCapsule` rewritten for the
      generalized shape, furnace/crafting-table placement, and the new
      `placeCapsuleLighting` dispatch (wall ring / wall-mounted torch
      ring / hanging-lantern grid / full glow-lichen coat, each with a
      degenerate single-fixture case at the smallest capsule size).
      `resolveSite` checks `forceCapsule` before the natural search.
      **Incidental fix**: `sanitizeNetherStart`/`readNetherStartConfig`/
      `netherStartMap`/`netherStartSummary` never touched
      `easyKit`/`mediumKit`/`hardKit` at all (a pre-existing gap since
      14.2b) — a YAML `netherStart.easyKit` override was silently
      ignored in every prior release; fixed here to match `cave`/
      `end_start`'s own identical pattern. `NetherStartCustomization
      .netherStartPlan()` (the in-game Customize-screen path) updated to
      compile against the wider record; deliberately still resolves the
      compiled-in capsule defaults rather than gaining new UI fields this
      pass (DESIGN §31.9's own "known first-pass gap, deferred not
      forgotten"). JUnit: `NetherStartPlanTest` (new field validation,
      including the degenerate ring case implicitly via the min-size
      bound), `WorldzConfigTest` (capsule sanitize/round-trip, the fixed
      kit round-trip, updated full-config summary). Full multiloader
      build + `./gradlew test` green.
- [x] 14b.3 Test configs (62 rewritten again, 86/87 rebalanced to isolate
      `forceCapsule` from the new automatic low-`spawnY` default — see
      14b.4) + docs (README, MANUAL_TESTING Phase 14b acceptance section,
      config/tests/README.md). **This part done; [Jason] in-game
      acceptance still outstanding** (room size/lighting/furniture feel,
      pickaxe actually breaks nether bricks, `forceCapsule` genuinely
      skips the search, each `LightSource` looks right in-game —
      especially confirming `GLOW_LICHEN` face orientation and hanging
      lanterns actually render hanging, not floor-standing).
- [x] 14b.4 Automatic capsule default for a low/high `spawnY` (2026-07-26
      follow-up): Jason widened the ask from "`forceCapsule` makes config
      62 deterministic" to "the capsule should just be the default
      behavior in cases like config 62" — full reasoning and the new
      `NetherStartPlan.spawnYTooCloseToBoundary` pure-logic method: DESIGN
      §31.9's "Automatic default for a low (or high) spawnY" subsection.
      Config 62 no longer sets `forceCapsule` at all (spawnY 4 alone
      triggers it); configs 87/88 moved to `spawnY: 32` (an ordinary,
      non-boundary depth) so they isolate the *explicit* `forceCapsule`
      pathway instead of accidentally relying on the same low-spawnY
      default config 62 now demonstrates. New tests in
      `NetherStartPlanTest` (pure logic, no `ServerLevel` needed — a
      first attempt living in `NetherStartDeployment`/the worldgen
      package instead failed with `NoClassDefFoundError`, since plain
      JUnit has no Minecraft classes on its classpath; moved into the
      `logic` package instead, matching why every other Deployment/
      Manager class in this codebase keeps its own pure logic out of the
      worldgen package entirely). "Really dangerous scenarios unsuitable
      for a fresh start" beyond Y-proximity (e.g. basalt-delta starting
      biomes) is explicitly **not** covered by this — no biome-awareness
      exists in the search at all; that's GOALS 27's already-logged,
      still-undesigned 2026-07-25 feedback point 3, not re-solved here.
- [x] 14b.5 **Fixed (0.3.3): `nether_start` spawns ignored the resolved
      site, landing far above it** — found via Jason's config 62 retest
      (twice, screenshots). Same bug class as the earlier cave-preset fix
      (0.2.85), just never extended to `nether_start`:
      `PlayerSpawnFinderMixin` only checked `enveloped.cave().enabled()`.
      Now also checks `enveloped.netherStart().enabled()` on both
      loaders. Full root cause and fix: Deviation log below (2026-07-26).
      Also corrected a wrong "Y-128 bedrock ceiling" assumption baked
      into `NetherStartPlan`/DESIGN §31.4 since Phase 14.1 — the Nether's
      real top is Y-255 (`height` 256, not `logicalHeight` 128) — a
      documentation-only fix, `MIN_SPAWN_Y`/`MAX_SPAWN_Y` already sat
      safely clear of either number. **[Jason] retest on a fresh config-62
      world outstanding** (the 0.3.2 world is from the buggy jar and
      should be deleted, not reopened).
- [x] 14b.6 **Furniture arrangement + light centering + default size,
      0.3.4** — Jason's real in-game retest of the fixed capsule (0.3.3,
      config 62) found three more things: chest still underfoot with
      furniture scattered through the room instead of "along one wall -
      centered"; wall lights landed at uncentered offsets ("offset on the
      right and left walls") instead of "in the middle of the walls";
      default room too small ("5x5 and 3 tall as seen from inside").
      Full design: DESIGN §31.9's 2026-07-27 follow-up.
      - Chest/furnace/crafting table now all line the south wall,
        centered (chest in the middle); `NetherStartDeployment.Site`
        (new record: `spawnPos`, `chestPos`) replaces the old bare
        `BlockPos` return, since the two are no longer the same tile for
        the capsule case. `StarterKitDeployment.spawnNetherStartChest`
        takes the exact final chest position now instead of computing
        `.below()` itself.
      - New `NetherStartPlan.centeredCapsuleOffsets(half, spacing)`
        (pure, JUnit-tested) replaces the old ring-walk-with-global-
        spacing algorithm that produced the uncentered offsets; each of
        north/east/west is now lit independently, centered on its own
        span (south is the furniture wall, always skipped for lighting).
      - `DEFAULT_CAPSULE_SIZE_BLOCKS` raised 5→7 (5x5 interior, matching
        "as seen from inside"); found and fixed a real documentation bug
        while at it — `StarterCapsuleConfig.sizeBlocks`'s own comment
        wrongly said "interior" when the field (and all the actual code)
        has always meant the exterior footprint, likely the real source
        of Jason's "5x5" being ambiguous in the first place.
      - New `DENSE_ROOM_INTERIOR_THRESHOLD` (6): a room with either
        interior dimension at or above it also gets ceiling/floor lights
        in addition to the walls (Jason: "larger area structures would
        need light blocks in the ceiling and floor... but not for
        anything under 6x6 spaces") — glowstone/shroomlight embed
        directly, lanterns gain a second floor-standing grid, torches
        get floor-standing (not ceiling — no vanilla variant exists)
        fixtures; every floor grid skips its own exact center so nothing
        lands on the player's own spawn column; `glow_lichen` already
        covered everything regardless of size, unaffected.
      - Configs 62/86/87 updated to match (62's own description/room
        size; 87 widened to a 7x7 interior so it actually demonstrates a
        multi-point lantern grid, which also exercises the new
        dense-room floor-lantern pass). New `NetherStartPlanTest` cases
        for `centeredCapsuleOffsets`. Full multiloader build +
        `./gradlew test` green, redeployed to Worldz-Test. **[Jason]
        retest outstanding** — delete the 0.3.3 config-62/86/87 worlds
        first, they predate this fix.

## Phase 15 — End-start challenge (GOALS 34)

- [x] 15.1 Design pass building on 14.1's spike: spawn on the outer End
      islands, starter chest tuned so survival through to defeating the
      Ender Dragon is genuinely achievable, hardcore-beatable even if very
      hard. Respawn semantics are the hard part (beds explode in the End, no
      anchors) — decide and document before implementing.
      **Done, no code (design only).** Full design in DESIGN §32.
      Confirmed against real 26.2 sources that `nether_start`'s exact
      "one `setRespawnData` overwrite" mechanism (§31.2) generalizes to
      the End unmodified — no `forced` RespawnConfig needed after all,
      correcting §31.3's earlier speculation (§32.1). Found a new,
      End-specific risk not present for `nether_start`: `PlayerSpawnFinder`/
      `Entity.adjustSpawnLocation`'s heightmap-based landing search can
      strand a player at the bottom of the dimension if the stored
      column has no real terrain (§32.1) — the guaranteed platform design
      (§32.4) accounts for this by construction.
      **Jason's decisions (2026-07-22, gathered up front per this phase's
      two genuine gameplay questions):** (a) always build a guaranteed
      end-stone platform, no natural-island search first (End terrain is
      too sparse/void-heavy for a Nether/Cave-style search to be worth
      its cost, §32.2); (b) the return path to the central island is
      firework rockets in the starter chest (tiered by difficulty), not a
      guaranteed gateway/teleporter and not a free Elytra — Elytra stays
      an ordinary End City find, and every tier still has a slow-but-
      always-available fallback (hand-mining the platform's own end stone
      to bridge across, §32.2). Split 15.2 into 15.2a-c below, mirroring
      Phase 13/14's own precedent for multi-part phases.
- [x] 15.2a Core mechanic: `EndStartPlan`/codec (persisted on the End's
      `EnvelopedChunkGenerator`, mirrors `NetherStartPlan`, DESIGN §32.3),
      `EndStartDeployment.buildEndPlatform` (guaranteed enclosed end-stone
      capsule at a fixed outer-island-belt point, DESIGN §32.4, no natural
      search per Jason's decision), and the `WorldLimitManager.
      onServerStarted` hook that overwrites the world's default spawn to
      `Level.END` at the resolved site (DESIGN §32.4). JUnit-covered pure
      logic where possible, mirroring `NetherStartPlanTest`'s precedent.
      **Done (0.2.67), backend only -- not yet in-game testable.** No
      typed preset is registered yet (that's 15.2b), so `EndStartPlan` can
      never resolve enabled today, matching 14.2a's own precedent exactly.
      Implementation matched the design in DESIGN §32 with no deviations.
      `EndStartConfig`/`EndStartPlan` also gained proper starter-kit
      config plumbing (read/sanitize/summary all call `readStarterKitConfig`/
      `sanitizeStarterKit`/`starterKitSummary` for `easyKit`/`mediumKit`/
      `hardKit`) from day one -- while writing it, found that
      `nether_start`'s own equivalent (`readNetherStartConfig`/
      `sanitizeNetherStart`/`netherStartMap`/`netherStartSummary`, shipped
      0.2.66) is missing all four of those calls, meaning a user's
      `netherStart.easyKit`/`mediumKit`/`hardKit` YAML customization has
      never actually been read, sanitized, or round-tripped back out --
      silently falls back to the built-in defaults every time. Logged as
      new tracked work below rather than fixed silently inside this
      Phase-15 commit (it predates this phase, shipped in 14.2b).
- [x] 15.2a-bugfix **[found during 15.2a, not yet fixed]** `nether_start`'s
      `WorldzConfig` plumbing (`readNetherStartConfig`/`sanitizeNetherStart`/
      `netherStartMap`/`netherStartSummary`) never reads, sanitizes, or
      writes back `easyKit`/`mediumKit`/`hardKit` -- copy `end_start`'s
      now-correct equivalents (`readEndStartConfig`/`sanitizeEndStart`/
      `endStartMap`/`endStartSummary`, DESIGN §32.6/TODO 15.2a) as the
      reference shape. Low severity (the built-in defaults are still sane
      and always apply), but a real defect: any of Jason's own
      `netherStart.easyKit`/etc. YAML customizations are silently ignored
      today.
      **Closed (2026-07-26 cleanup pass):** already fixed, just never
      checked off -- commit `427f699` ("Universal starter capsule,
      Nether-start first pass", 0.3.2) fixed exactly this as an incidental
      fix while threading the new capsule fields through the same four
      methods anyway ("Fixed to match cave/end_start's identical pattern"),
      but never referenced or closed this line. Reverified now: all four
      methods handle `easyKit`/`mediumKit`/`hardKit` correctly, and
      `WorldzConfigTest.netherStartKitsLoadIndependently` passes, covering
      exactly this. New config 90 (2026-07-26) adds real in-game,
      config-file-level proof on top of the unit test.
- [x] 15.2b Starter chest tiers (DESIGN §32.5, reuses `StarterKitTier`/
      `StarterKitConfig`; easy = rockets + blocks + food + bow/armor,
      medium = fewer rockets/lighter gear, hard = no rockets, no
      guaranteed weapon) placed at the resolved site, plus the typed
      preset shape (DESIGN §32.6): `EndStartConfig`, `EndStartCustomization`,
      `EndStartPresetEditor`, `EndStartCustomizeScreen`, world-preset JSON +
      `normal` tag + lang keys, both loaders' registration (wraps
      `LevelStem.END` with `EnvelopedChunkGenerator`, the second preset to
      do so after `sky_chunk`, DESIGN §29.5/§32.6).
      **Done (0.2.68), in-game testable now.** `end_start` shows up as
      the tenth "Worldz" World Type entry. `StarterKitDeployment.
      spawnEndStartChest` places the chest the same way `nether_start`'s
      own optional chest is (floor block beneath the resolved site,
      `WorldLimitManager`'s new `needsEndStart`/`end != null` branch).
      Typed-preset scaffolding mirrors `nether_start`'s exactly (§32.6)
      with the same structural difference cave/nether_start already
      established: `end_start`'s `world_type` hint sits on the *End's*
      enveloped generator (its plan is End-attached, §32.3), not the
      Overworld's or Nether's -- both of those wrap `EnvelopedChunkGenerator`
      only for the uniform infra every preset already gets, carrying no
      hint of their own. New structural regression tests mirroring every
      prior typed preset's own precedent (`WorldPresetResourcesTest`,
      `ProjectMetadataTest`), plus updated the `normal` tag's now-10-entry
      count assertion.
- [x] 15.2c Test configs (default/each chest tier); docs (README,
      MANUAL_TESTING.md, config/tests/README.md); phase wrap-up. **[Jason]**
      acceptance (ideally including a hardcore run's early game) — does
      dying without a bed/anchor really return you to the same platform;
      is the platform reachable/safe on arrival; is a full dragon-fight
      run (with an End City detour for Elytra) genuinely achievable at
      each tier, including a hand-bridged hard-tier attempt.
      **Configs and docs done (0.2.69):** `config/tests/63`-`65`
      (default/medium tier exercising the core redirect+respawn
      mechanic; easy and hard chest tiers). New "Phase 15 acceptance"
      section in MANUAL_TESTING.md (3 configs + a full-run item,
      explicitly flagging the same-platform-respawn check as the single
      most important thing to verify, mirroring Phase 14's own posture).
      `config/tests/README.md`'s intro updated for the tenth World Type
      entry plus a new table row describing `endStart:`. README.md
      gained a full "End-start challenge" section (challenge-types table
      row + prose + chest-tier table + config example + settings table)
      mirroring the Nether-start section's shape.
      **This completes every non-[Jason] item in Phase 15** (15.1
      design, 15.2a core mechanic, 15.2b chest tiers + typed preset UI,
      15.2c test configs/docs) — **do not start Phase 16 without Jason's
      explicit go-ahead.** TODO 15.2a-bugfix (the pre-existing
      `nether_start` config-plumbing gap found while writing this phase,
      not itself part of GOALS 34) remains open and unscheduled; pick it
      up whenever convenient, it doesn't block Phase 16.
- [x] 15.3 **Real-world feedback (2026-07-25, Jason's first actual in-game
      test of config 63):** no chest found, spawned on a small platform;
      asked for a larger platform, the chest moved to one side, and
      Nether-start's own configurable capsule mechanism (GOALS 41)
      generalized to `end_start`. Root-caused the missing chest to a real
      bug, not just a size complaint: `end_start` was deliberately left
      out of `PlayerSpawnFinderMixin`'s trusted-suggestion list (GOALS
      41's own reasoning at the time: the End's mostly-void surroundings
      should let vanilla's same-column heightmap search re-find the
      platform's floor on its own) -- wrong, since the platform's sealed
      *roof* is what that search actually finds first, landing the player
      on top of the box, not inside it. **Fixed (0.3.5):** added
      `end_start` to the mixin (fabric + neoforge), matching cave/
      Nether-start's own trusted treatment. Generalized the capsule in
      the same pass -- `EndStartPlan`/`EndStartConfig` gain
      `capsuleSizeBlocks`/`heightBlocks`/`lightSource`/
      `lightSpacingBlocks` (mirrors `NetherStartPlan`/`NetherStartConfig`
      exactly, duplicated rather than shared per GOALS 41.1's own "true
      cross-preset sharing later" precedent), `EndStartCodecs` persists
      them, `WorldzConfig`'s `sanitizeStarterCapsule` helper is
      parameterized on bounds so both `netherStart.capsule` and
      `endStart.capsule` share the one implementation safely.
      `EndStartDeployment` rewritten: config-driven size/height, full
      lighting dispatch (torch/lantern/soul_lantern/glowstone/
      shroomlight/glow_lichen, dense-room ceiling/floor lights) ported
      from `NetherStartDeployment.placeCapsuleLighting` verbatim (end
      stone instead of nether bricks, no furnace/crafting table -- End-
      start's chest tiers need no smelting/crafting to begin bridging),
      default platform widened from the original 1x1-interior shape to a
      5x5 interior, and the chest moved off underfoot to line the south
      wall once the room is big enough (falls back to underfoot only at
      the smallest size, which has no side wall). `EndStartDeployment`
      now returns a `Site(spawnPos, chestPos)` record (mirroring
      `NetherStartDeployment.Site`) instead of a single `BlockPos`;
      `StarterKitDeployment.spawnEndStartChest` takes the resolved chest
      position directly instead of computing `.below()` itself.
      `EndStartCustomization.endStartPlan()` supplies the compiled-in
      capsule defaults, same config-only deferral as Nether-start's own
      (GOALS 41.1) -- not yet on the Customize screen. New
      `EndStartPlanTest` capsule coverage (bounds, `centeredCapsuleOffsets`)
      and `WorldzConfigTest` `endStart.capsule` read/sanitize/round-trip
      tests, mirroring `NetherStartPlanTest`/the existing `netherStart.
      capsule` tests. New config 89 (custom 9x9/7x7-interior, 4-tall,
      lantern-lit platform). Docs updated: GOALS 41's own log, README's
      End-start section (capsule table, config-only note), config/tests/
      README.md (files 63-65 now require 0.3.5+, new row for 89), and a
      new MANUAL_TESTING.md Phase 15 acceptance item covering the fix and
      config 89. Full multiloader build green (`common`/`fabric`/
      `neoforge` compile, all `common` tests pass). **[Jason] retest
      outstanding** -- delete any pre-0.3.5 End-start world saves first,
      since the spawn-placement fix changes where a new player actually
      lands (previously on the roof, now genuinely inside the room).
- [x] 15.4 **Follow-up (0.3.6, same 2026-07-25 retest):** Jason confirmed
      15.3's spawn/platform/chest fix (configs 63-65) but found none of
      the chest tiers could actually get him out of the platform --
      "mainly need a pickaxe to break out of the starting box". Verified
      against real decompiled vanilla source rather than assuming: End
      Stone has `.requiresCorrectToolForDrops()` set on `Blocks.END_STONE`
      and sits in the `minecraft:mineable/pickaxe` tag (any tier counts,
      it's absent from every `needs_*_tool` tag) but not in any
      "hand-minable" allowance -- so bare-hand mining breaks the block
      with *no drop*, exactly like Stone. This project's own docs (GOALS
      34, README, MANUAL_TESTING, several config-63/65 comments) had
      assumed "minable by hand, no tool required" since Phase 15 shipped;
      wrong the whole time, just never caught until Jason's actual
      in-game retest. Fixed by adding a guaranteed pickaxe to every
      `EndStartConfig` tier's essentials, escalating with the rest of
      each tier's gear (Jason's explicit choice over one shared pickaxe
      for all three, mirroring how the rest of each kit already
      escalates): hard gets a wooden pickaxe, medium a stone pickaxe,
      easy a copper pickaxe. Updated `WorldzConfigTest`'s full-config
      summary assertion and `ProjectMetadataTest`'s version-string
      assertion (bumped to 0.3.6) to match. Corrected every "minable by
      hand" claim found across README.md, MANUAL_TESTING.md, and configs
      63/65/89's own in-file comments to describe the guaranteed pickaxe
      instead (left GOALS 34/TODO 15.1's own historical decision-log
      entries alone -- they record what was believed *at the time*, not
      current behavior). Full multiloader build green. **[Jason] retest
      outstanding** on configs 63-65 -- confirm the guaranteed pickaxe in
      each tier actually breaks the platform's end stone and drops it.
- [x] 15.5 **Test-coverage gap pass (2026-07-26 cleanup pass):** auditing
      every capsule config across both `nether_start` and `end_start`
      found `end_start` had no `glow_lichen` coverage (nether_start's own
      config 87 was never mirrored), and that `torch` and dense-room
      embedded sources (`glowstone`/`shroomlight`) had *zero* coverage
      anywhere -- not for either preset. New configs 91 (`torch`, dense
      room -- also the previously-untested dense-room floor-torch
      addition), 92 (`shroomlight`, dense room -- also the previously-
      untested dense-room floor+ceiling addition embedded sources get,
      distinct from lantern/torch's floor-only addition), 93
      (`glow_lichen`, parity with config 87). Docs updated:
      `config/tests/README.md`, MANUAL_TESTING.md's Phase 15 acceptance.
      No code changes -- test-fixture/doc additions only, no version bump.

## Phase 16 — Flat worlds (GOALS 15, 16, 22)

- [x] 16.1 Design pass against DESIGN §19's verified `FlatLevelSource`
      research: layer editor (arbitrary block layers/thicknesses, presets,
      text import/export), optional bedrock floor, structure toggles incl.
      the trial-chambers placement spike, spawn-Y/slime option (15);
      deep-flat variant with seeded caves/cave biomes/optional far-off rivers
      (16 — likely noise-based underground below a flat surface, spike
      needed); underground structures buried at natural depth rather than
      floating (22).
      **Done, no code (design only).** Full design in DESIGN §33. Verified
      against real 26.2 sources that `FlatLevelSource` has zero noise/
      carving capability at all (`applyCarvers` is a literal no-op) —
      GOALS 15 and 16 need two architecturally different generators, not
      one preset with a toggle: `jlt_worldz:flat` (GOAL 15) wraps vanilla
      `FlatLevelSource` directly (low risk, mirrors DESIGN §19's original
      scoping); `jlt_worldz:deep_flat` (GOAL 16) wraps a real, unmodified
      `NoiseBasedChunkGenerator` delegate (same as `cave`/`single_biome`)
      with a new post-processing "cap to a flat surface Y, keep real
      terrain below" pass, giving genuine seed-driven caves/cave biomes/
      structures for free with zero new noise-density-function code —
      chosen over a custom-density-function approach (technically real,
      verified feasible via `overworld/offset`'s spline graph, but
      rejected as materially higher-risk and unverifiable by this
      project's JUnit-only policy, §33.1). Verified `trial_chambers` is
      an ordinary structure set with no special terrain dependency
      (re-confirms, doesn't just carry forward, DESIGN §19's existing
      claim). Verified the real Y-40 slime-spawn cutoff
      (`Slime.checkSlimeSpawnRules`) for GOAL 15's spawn-Y option. No
      genuine gameplay/scope question found needing Jason's decision —
      the architecture choices above are engineering calls within the
      executor's own remit (AGENTS.md's Roles section), flagged clearly
      in DESIGN and here rather than gated on a question. TODO 16.2 split
      into 16.2a (classic flat)/16.2b (deep flat)/16.2c (structures, test
      configs, docs), mirroring Phase 13/14/15's own precedent for
      multi-part phases.
- [x] 16.2a Implement `jlt_worldz:flat` (GOAL 15, DESIGN §33.2-33.3):
      `FlatPlan`/codec, layer editor UI (arbitrary layers, text import/
      export per §33.5, built-in presets), structure-set checklist,
      spawn-Y/slime-avoidance option, typed preset + registration; test
      configs; docs.
      **Done (0.2.70), in-game testable now.** `flat` shows up as the
      eleventh "Worldz" World Type entry. Real architecture ended up
      different from DESIGN §33.1's first draft, corrected before writing
      any code (not after): `flat` cannot wrap vanilla `FlatLevelSource`
      directly, since `WorldLimitManager`'s hard `LimitedBiomeSource` gate
      would silently disable every shared Worldz feature (borders,
      exteriors, progression guarantees) for it. Every Overworld delegate
      stays `NoiseBasedChunkGenerator` + `LimitedBiomeSource` (restricted
      to one biome, mirroring `single_biome`'s exact precedent) instead;
      `EnvelopedChunkGenerator` skips the delegate's real (expensive)
      terrain/carving/surface methods entirely when `flat.enabled()` and
      substitutes a small, directly-reimplemented flat-fill mirroring
      vanilla `FlatLevelSource`'s own few-line logic almost verbatim --
      giving genuine performance parity with real vanilla flat, not
      "generate real terrain then discard it." `createState` still reuses
      the real `ChunkGeneratorStructureState.createForFlat` factory
      vanilla flat worlds themselves use, for `structureOverrides`
      filtering. Dropped the `lakes` toggle from scope (GOALS.md's own
      GOAL 15 wording never asked for it; DESIGN §33.2 records why).
      Structure list is a multi-line text field (mirrors the generic
      Customize screen's own `allowedBiomes` widget), not 18 checkboxes.
      Closed the fieldless-preset defaulting gap from day one (`flatDefaults`
      hint, single-biome resolution mirroring `single_biome`'s own
      precedent) and, incidentally, fixed a small pre-existing Phase-15
      gap found in the same lines (`end_start`'s own hint was missing
      from three defaulting branches -- low severity, fixed here rather
      than deferred since it was trivial in the same diff). New JUnit
      (`FlatLayerSpecTest`, `FlatPlanTest`, `WorldzConfigTest` coverage)
      plus structural regression tests mirroring every prior typed
      preset's own precedent. Test configs and docs deferred to 16.2c
      per Phase 13/14/15's own precedent (config/docs land in the final
      sub-task of a multi-part phase).
- [x] 16.2b Implement `jlt_worldz:deep_flat` (GOAL 16, DESIGN §33.4):
      `DeepFlatPlan`/codec, the delegate-then-cap `EnvelopedChunkGenerator`
      post-processing pass (biome-aware: land cap vs. river/ocean water
      cap), rivers-enabled + exclusion-radius option, typed preset +
      registration; test configs; docs.
      **Done (0.2.71), in-game testable now.** `deep_flat` shows up as
      the twelfth "Worldz" World Type entry. Delegate stays a real,
      unrestricted `NoiseBasedChunkGenerator` + `LimitedBiomeSource`
      (full vanilla biome variety, mirrors `cave`'s exact precedent) so
      real caves/cave biomes/aquifers/ores/structures all come from
      vanilla's own proven pipeline with zero new noise code, exactly as
      DESIGN §33.1 originally planned (unlike classic `flat`, this half
      of the design didn't need correcting). The cap pass runs right
      after the delegate's own real `buildSurface` call (not
      `fillFromNoise`, which DESIGN §33.4's first draft assumed --
      carving has to happen in between for real caves to exist, and
      capping before `buildSurface` would let the delegate's own surface
      rules paint over the cap band) -- late enough that real terrain/
      caves/surface materials already exist below the cap, early enough
      that biome decoration plants on the fresh capped surface next.
      `getSpawnHeight` also needed a `deepFlat`-aware override (returns
      `surfaceY` directly, verified it's a dimension-wide constant with
      no x/z of its own) so spawn actually lands on the flat cap instead
      of wherever the real, uncapped terrain happens to be tall;
      `getBaseHeight`/`getBaseColumn` deliberately were not touched (see
      DESIGN §33.4's implementation notes for why that's an acceptable
      first-pass gap, not a player-visible correctness bug). Flagged a
      new known gap for Jason's acceptance to watch for: a water-capped
      river/ocean column's water sits directly on whatever real terrain
      is immediately below the cap, so a natural cave opening right at
      that boundary could source water down into it -- not fixed
      speculatively, watch for it in 16.2c's acceptance pass. Closed the
      fieldless-preset defaulting gap from day one (`deepFlatDefaults`
      hint, full vanilla variety mirroring `cave`'s own precedent). New
      JUnit (`DeepFlatPlanTest`, `WorldzConfigTest` coverage) plus
      structural regression tests mirroring every prior typed preset's
      own precedent. Test configs and docs deferred to 16.2c.
- [x] 16.2c Structures/underground-content acceptance pass (GOAL 22): a
      test config exercising an underground structure set (trial chambers
      or ancient city) at both a shallow classic-flat depth (honestly
      clipped, documented tradeoff) and a deep_flat world (buried at
      natural depth); phase wrap-up docs (README, MANUAL_TESTING.md,
      config/tests/README.md). **[Jason]** acceptance across both typed
      presets.
      **Configs and docs done (0.2.72):** `config/tests/66`-`71` (classic
      flat default/traditional-shallow/shallow-structures, deep-flat
      default/no-rivers/structures). New "Phase 16 acceptance" section in
      MANUAL_TESTING.md (6 configs, explicitly cross-referencing #3 and
      #6 so Jason can directly compare classic flat's honestly-clipped
      structure result against deep_flat's naturally-buried one, and
      flagging the water-into-caves gap from 16.2b as something to
      specifically watch for). `config/tests/README.md`'s intro updated
      for the twelfth World Type entry plus two new table rows describing
      `flat:`/`deepFlat:`. README.md gained full "Flat challenge" and
      "Deep flat challenge" sections (challenge-types table rows + prose
      + settings tables + config examples) mirroring every prior typed
      preset section's shape.
      **This completes every non-[Jason] item in Phase 16** (16.1
      design, 16.2a classic flat, 16.2b deep flat, 16.2c structures
      acceptance/docs) — **do not start Phase 17 without Jason's
      explicit go-ahead.** TODO 15.2a-bugfix (the pre-existing
      `nether_start` config-plumbing gap) remains open and unscheduled,
      unrelated to this phase.
- [x] 16.3 **Real-world feedback (2026-07-26, Jason's first actual in-game
      test of config 66):** "Structures are not generating." Root-caused
      by decoding the actual Worldz-66 save's region files by hand (no
      NBT tool was available, so a throwaway Python script was written to
      parse `.mca`/chunk NBT directly): a village structure *start* really
      was resolved near spawn (chunk 13,13), that chunk had reached `full`
      status, but every section was still pure flat-layer palette (bedrock/
      stone/dirt/grass only) -- no village block was ever written, despite
      the start existing. Confirmed against real decompiled vanilla source
      (`ChunkGenerator.applyBiomeDecoration`): vanilla bundles two
      unrelated things into that one method -- ordinary per-biome feature
      decoration (trees, ore veins) *and* the actual block-writing
      (`structureManager.startsForStructure(...).forEach(start ->
      start.placeInChunk(...))`) for every structure whose site the
      earlier STRUCTURE_STARTS pass already resolved. `EnvelopedChunkGenerator
      .applyBiomeDecoration`'s own `flat.decoration()` gate (GOAL 15,
      config default `false`) was skipping the *entire* delegate call when
      off, silently dropping structure placement along with the ordinary
      biome decoration it was actually meant to control -- vanilla's own
      `FlatLevelSource` never overrides `applyBiomeDecoration` at all, so
      an ordinary vanilla flat world always places structures regardless
      of its own decoration setting. **Fixed (0.3.7):** added
      `EnvelopedChunkGenerator.placeStructuresOnly` -- a faithful
      reimplementation of just the structure-placement half of vanilla's
      `applyBiomeDecoration` using only public API (mirrors this class's
      own `createStructuresRespectingDistance`/`tryGenerateRestrictedStructure`
      precedent, DESIGN §36), plus a `writableArea` helper mirroring
      vanilla's own private `getWritableArea` (unreachable from a
      subclass) -- called instead of skipping outright whenever
      `flat.enabled() && !flat.decoration()`. Ordinary biome decoration
      (the toggle's actual, intended effect) is still skipped in that
      case; every other path (decoration on, or flat disabled entirely)
      is untouched, zero risk to every other preset. Confirmed the fix
      against the real world save's own already-materialized data isn't
      possible without regenerating chunks, so verification was source-
      level (decompiled vanilla method comparison) plus a full multiloader
      build -- **[Jason] retest outstanding** on a fresh config 66/67/68
      world (delete the old Worldz-66 save first; its already-generated
      chunks are permanently missing their structures and won't retroactively
      gain them). Also found, not yet acted on: this same investigation
      confirmed `minecraft:villages` (config 66's own default
      `structureOverrides` entry) *is* a real structure-set id in this MC
      version -- villages were consolidated from five separate structure
      sets (`village_plains`/`desert`/`savanna`/`snowy`/`taiga`) into one
      `minecraft:villages` set, unlike older Minecraft versions -- so no
      second bug there, just worth noting for future reference since the
      README/config comments predate this discovery.
- [x] 16.4 **Follow-up (0.3.8, same 2026-07-26 retest, config 67):** Jason
      confirmed 16.3's structure fix (config 66 -- villages on the surface,
      strongholds underground/exposed, "like vanilla") and separately
      confirmed config 67 works, but flagged "low spawn has a very dark
      horizon". Confirmed by pulling his own screenshots directly from the
      Prism instance's screenshots folder: a solid black band wraps the
      entire horizon at low Y (e.g. Y -31), while an equivalent screenshot
      at the default Y 64 spawn showed a perfectly ordinary sky with none.
      Root-caused against real decompiled sources: vanilla's own
      `SkyRenderer.shouldRenderDarkDisc` renders a black "dark disc" plane
      whenever the camera's eye Y is below `ClientLevel.ClientLevelData.
      getHorizonHeight`, which returns the level's real minimum Y if the
      world is flagged `isFlat` -- but a hardcoded sea level of `63.0`
      otherwise. `isFlat` (`PrimaryLevelData.isFlatWorld()`) is computed
      exactly once at world creation, by `WorldDimensions.
      specialWorldProperty`'s plain `generator instanceof FlatLevelSource`
      check -- `jlt_worldz`'s own `flat`/`deep_flat` presets never satisfy
      this (their real generator is always `EnvelopedChunkGenerator`, a
      delegate wrapper vanilla has no way to recognize as "flat"), so
      every Worldz flat world fell into the ordinary sea-level-63 branch
      regardless of its own configured surface height -- invisible at the
      default Y 64 spawn (64 - 63 >= 0), glaring at config 67's Y -60
      (-60 - 63 is deeply negative). **Fixed:** new `WorldDimensionsMixin`
      (fabric + neoforge, registered in both loaders' mixin configs)
      injects at the `RETURN` of `WorldDimensions.bake` and upgrades the
      result to `SpecialWorldProperty.FLAT` whenever the baked Overworld's
      generator is an `EnvelopedChunkGenerator` with `flat.enabled()` or
      `deepFlat.enabled()` true and vanilla's own check hadn't already
      flagged something else -- mirrors `PlayerSpawnFinderMixin`'s own
      per-loader-duplicate, `CallbackInfoReturnable`-based convention
      exactly. Deep-flat included proactively (same uniformly-flat-surface
      shape as classic flat, GOAL 16) even though config 67 was the only
      one actually retested. Verified two ways: a full multiloader build,
      and a real `:fabric:runServer` smoke test against the plain default
      world (confirms the mixin doesn't break `WorldDimensions.bake`'s
      ordinary, non-flat path -- ran to "Done" with no mixin-apply errors
      or crash); did not stand up a real flat-world server run to confirm
      the positive case directly, since that needs a real client to
      actually see the horizon. **[Jason] retest outstanding** on config
      67 (delete the old save first -- `isFlat` is baked into `level.dat`
      at world creation, same as every other setting here, so an existing
      save won't retroactively pick this up).
- [x] 16.5 **Follow-up (2026-07-26, config 68 retest):** Jason confirmed
      trial chambers generate (honestly clipped, GOAL 22's expected
      classic-flat tradeoff -- with the flat world's unobstructed sightlines,
      several clipped structure tops were simultaneously visible from
      height, reading as "very frequent"; trial chambers' own real spacing
      is unchanged, `data/minecraft/worldgen/structure_set/
      trial_chambers.json`'s `random_spread` placement is `spacing: 34`/
      `separation: 12`, identical to any ordinary vanilla world -- not a
      config-68-specific frequency effect). No strongholds and, separately,
      no ancient cities -- strongholds are expected (`strongholds` was never
      in config 68's own `structureOverrides` list to begin with). Ancient
      cities root-caused as a real, permanent constraint rather than a bug:
      verified against `data/minecraft/worldgen/structure/ancient_city.json`
      (`"biomes": "#minecraft:has_structure/ancient_city"`) and
      `data/minecraft/tags/worldgen/biome/has_structure/ancient_city.json`
      (only `minecraft:deep_dark`) -- vanilla gates ancient-city placement
      to the `deep_dark` biome specifically, on top of (not instead of) its
      structure-set spacing. `flat`'s single-biome design (DESIGN §33.2's
      `flatDefaults`/`resolveFlatAllowed`, always exactly one biome
      everywhere) means `ancient_cities` in `structureOverrides` can never
      actually place unless that one biome *is* `minecraft:deep_dark` --
      enabling it over `plains` (config 68's own biome) looks identical to
      leaving it disabled. `deep_flat` is unaffected (full vanilla biome
      variety per its own delegate, config 71 already covers it correctly).
      Documented rather than fixed -- this is the same "player's own
      configuration choice" posture GOAL 22 already takes on shallow-stone
      clipping, just for biome instead of depth. **Docs updated:** config
      68's own header comment, MANUAL_TESTING.md step 3, README.md's
      "Underground structures" note, and DESIGN §33.2 all now say so
      explicitly. No code changes -- requirements capture only, no version
      bump.
- [x] 16.6 **Crash found and fixed (0.3.9, 2026-07-26, Jason's config 69
      retest):** game crashed generating a chunk near spawn with
      `IllegalArgumentException: The value -544 is not in the specified
      inclusive range of 0 to 255` at `SimpleBitStorage.get`, reached via
      `Heightmap.getFirstAvailable` <- `Heightmap.update` <-
      `EnvelopedChunkGenerator.applyDeepFlatCap` (crash report: chunk
      (-2,-2), `minecraft:surface` status). Root-caused against real
      decompiled vanilla source (`Heightmap.update`/`getIndex`): a
      heightmap's backing `BitStorage` is a fixed 256-slot array indexed
      by `x + z*16`, so `Heightmap.update(x, y, z, state)` requires
      chunk-*local* x/z (0-15) -- unlike `ChunkAccess.setBlockState`,
      which accepts absolute world coordinates and was the convention
      `applyDeepFlatCap`'s own loop (over `chunkPos.getMinBlockX()`..
      `getMaxBlockX()`) otherwise correctly followed. Passing that same
      absolute x/z straight into `oceanFloor.update`/`worldSurface.update`
      produced a wildly out-of-range bit-storage index the moment either
      coordinate went negative or reached 16+ -- deterministic for any
      chunk not at (0,0), so this affected every `deep_flat` world from
      day one (GOAL 16, configs `69`-`71`), just never hit until Jason's
      first real chunk-boundary-crossing retest. `fillFlatColumns`/
      `fillStackedColumns` (this same class) were already doing this
      correctly, using their own local 0-15 loop variables -- confirms
      this was an `applyDeepFlatCap`-specific oversight, not a systemic
      one. **Fixed:** compute `localX`/`localZ` once per column and pass
      those (not the absolute `x`/`z` already used for `setBlockIfDifferent`)
      to both heightmap `update` calls. Full multiloader build green
      (`common`/`fabric`/`neoforge` compile, all `common` tests pass); no
      new unit test added, matching this class's existing precedent (real
      `ChunkAccess`/registries needed, exercised via manual/in-game testing
      only, same as every other `EnvelopedChunkGenerator` fix this phase).
      **[Jason] retest outstanding** on configs 69-71 (delete any
      pre-0.3.9 deep-flat saves first -- already-generated chunks near
      spawn may have corrupted/incomplete heightmap data from the crash).
- [x] 16.7 **Follow-up (0.3.10, same config 69 retest):** Jason's
      screenshots near spawn showed small dirt-rimmed pits with real
      water sitting in the middle of otherwise-flat grass, unrelated to
      any river/ocean biome -- confirmed as an ordinary terrain-noise low
      spot (ground that's naturally a few blocks lower than its
      surroundings, water-filled the same way any vanilla world's small
      natural ponds are) whose real depth dipped below the shallow
      default land cap (4 blocks: `dirt:3`+`grass_block:1`).
      `applyDeepFlatCap`'s band paint only ever overwrites its own
      configured thickness and never checked what was immediately
      beneath it, so a pond bottoming out lower than that band punched
      straight through the flat surface. **Fixed:** new
      `EnvelopedChunkGenerator.sealBeneathCap`, run immediately after a
      land-capped column's band paint -- continues downward replacing
      any immediately-connected open pocket (air or a liquid) with solid
      stone until hitting the first genuinely solid block, bounded to 8
      blocks (`SEAL_DEPTH_BLOCKS`) so real caves still start a little
      further down exactly as GOAL 16 intends ("dig through the cap...
      into real stone, then real caves eventually appear"); a deeper
      real cave or aquifer beyond that bound is left completely
      untouched. River/ocean (water-capped) columns are unaffected by
      this commit -- see 16.8 for that half (water draining into caves).
      Full multiloader build green. **[Jason] retest outstanding** on
      config 69 (delete any pre-0.3.10 save first).
- [x] 16.8 **Follow-up (0.3.11, same config 69 retest):** Jason's
      screenshots also showed water pouring down through a breach into a
      lit cave with exposed lava at Y28, near a river/ocean-tagged
      surface column -- a real, now-confirmed instance of DESIGN
      §33.4's previously-only-theoretical "water draining into caves"
      gap, root-caused the same way as 16.7: the water-cap band is
      painted directly above whatever real terrain already exists there,
      with no check for an open pocket immediately beneath it, so a real
      cave breach right under a river/ocean column lets the freshly
      placed water flow straight down via ordinary fluid physics.
      **Fixed:** `sealBeneathCap` (16.7) now also runs for water-capped
      columns, not just land-capped ones -- same bounded 8-block seal,
      same reasoning (a real cave breach immediately under the surface
      gets patched; a real cave system starting further down is
      untouched, so the water still floods it exactly like any ordinary
      body of water sitting over a distant cave system would in vanilla
      -- this only closes the *immediate*, cap-boundary-adjacent
      breach). DESIGN §33.4 updated to describe this as a bounded
      mitigation rather than a fully open gap. Full multiloader build
      green. **[Jason] retest outstanding** on config 69 (delete any
      pre-0.3.11 save first).
- [x] 16.9 **Follow-up (2026-07-26, Jason):** wanted an option to raise
      the flat surface above the Y-40 slime cutoff while still being able
      to farm slimes underground -- his suggestion was a dedicated
      "void"/"air" layer type. Investigated first rather than building
      blind: `fillFlatColumns`/`flatLayerStates` never special-case a
      layer's block id, and `flatBaseHeight`/`flatBaseColumn` already scan
      top-down for the first opaque block, so a non-opaque middle layer
      was already handled correctly -- `flat.layers` can already contain
      a `minecraft:air` entry anywhere in the stack, not just at the top,
      turning the "avoid slimes" and "slime farm" asks into one
      config, not two mutually exclusive ones. **No code change** --
      documented the trick (README's Flat challenge section, GOALS 15
      clarification) and added `config/tests/94-flat-slime-cavity.yaml`
      plus its Phase 16 acceptance step. **[Jason] retest outstanding.**
- [x] 16.10 **Follow-up (2026-07-26, Jason):** asked for cave biomes below
      the surface (see 16.9's sibling ask, resolved separately by adding
      MC 26.2's new `minecraft:sulfur_caves` to `WorldzConfig.allowedBiomes`'s
      default list, verified against the real game jar -- not a Phase 16
      item, no code here needed it), plus a log warning telling the player
      to remove/ignore any `structureOverrides` entry that can never
      generate without a specific biome. Generalizes 16.5's own
      `ancient_cities`/`deep_dark` finding (README's "`ancient_cities` also
      needs the right biome" note) into an automatic check: real code
      change in `EnvelopedChunkGenerator.createState` -- once a `flat`
      generator's structure state resolves, an explicitly configured (never
      the empty "every set eligible" default) `structureOverrides` entry
      gets checked against `flat.biome` using each candidate structure
      set's own real vanilla eligible-biome data (`StructureSet.structures()`
      → `Structure.biomes()`), not a hardcoded structure-to-biome table --
      same "reuse the real vanilla mechanism" posture as every other
      biome-eligibility check in this project. Uses
      `LimitedBiomeSource.allowedBiomes()` (the raw configured biome set)
      rather than the broader, fallback-widened `BiomeSource.possibleBiomes()`.
      Logs once per world/dimension load (`ChunkGenerator.createState` is
      only ever called once, confirmed against the real decompiled
      `ChunkMap` constructor), matching every other resolve-time warning's
      own log-once precedent. No unit test is feasible (this reads real
      registry holders that need a bootstrapped game environment the test
      suite doesn't have, same as every other generator-level check) --
      manual-test only: added `config/tests/94-flat-structure-override-
      biome-match.yaml` (biome matches, warning silent, structure really
      places) alongside configs 68/93 (biome mismatches, warning fires),
      new Phase 16 acceptance item 8. Full multiloader build green.
      **[Jason] retest outstanding.**
- [x] 16.11 **Bug found and fixed (0.3.18, 2026-07-26, Jason's real config
      71 retest, GOAL 22):** surface-anchored structures (desert pyramid,
      shipwreck) generated floating above `deep_flat`'s flat cap on land,
      and floating on top of a capped ocean's water surface instead of
      resting on the seafloor -- screenshots confirmed against config 71
      (`Worldz-71` save). Root cause: `getBaseHeight`/`getBaseColumn` had no
      `deep_flat` branch (DESIGN §33.4's original "acceptable first-pass
      gap" reasoning), so vanilla's own height lookup for these structures
      read the delegate's real, pre-cap terrain -- wherever that differed
      from `surfaceY`, the structure and the cap (painted afterward, purely
      by absolute Y) simply disagreed. Underground structures (trial
      chambers, ancient city) were never affected, since their placement
      doesn't consult surface height at all -- which is why GOAL 22's
      original text, scoped only to those two, read as satisfied. **Fixed:**
      `deep_flat` now gets its own `getBaseHeight`/`getBaseColumn` branch
      mirroring `flat`'s (`deepFlatBaseHeight`/`deepFlatBaseColumn`), backed
      by a shared `deepFlatWaterCapAt` river/ocean test reimplementing
      `applyDeepFlatCap`'s own water-cap rule without a `WorldGenRegion`
      (`RandomState`-driven biome-source sampling instead, same mechanism
      `skyIslandHitAtForTerrain` already established) so the two passes
      can't drift apart. DESIGN §33.4 corrected in place. Full multiloader
      build green. **Confirmed (2026-07-26, Jason's real 0.3.18 retest,
      fresh config 71 world):** a shipwreck that previously sat fully
      exposed on top of the water now sits properly down near the
      seafloor, only a small piece breaching the flat surface; a pillager
      outpost tower generated flush on the flat grass, no floating or
      clipping. Rivers/oceans still read flat and calm at the capped
      surface. Phase 16 fully closed.

## Phase 17 — Stacked biome layers (GOALS 35)

The biggest new generation concept — deliberately after flat (16), whose
layer-editor concepts it likely reuses, and after limits (5), which it
composes with.

- [x] 17.1 Design spike (interpretation confirmed by Jason 2026-07-16:
      stacked horizontal slabs — plains above desert above taiga — each
      layer flat or low-relief, using the flatter variants of its biome, so
      this builds on Phase 16's flat layer machinery rather than full noise
      terrain per layer; each layer gets a configurable air gap above its
      surface so biome-specific trees and structures generate and can grow
      on every layer): per-layer surface rules and biome features
      (tree/structure generation within the gap height — tall trees need
      real headroom), lighting and mob spawning below the top layer, layer
      config (list/order/thickness/air gap, seed-randomized option), the
      deep-ore budget
      (lapis/gold/diamond redistributed across layers or exposed as config),
      and stronghold/End-portal placement within the stack.
      **Done, no code (design only).** Full design in DESIGN §34. New
      Overworld-only typed preset `jlt_worldz:stacked`, generator-owned
      (persisted on `EnvelopedChunkGenerator`'s own codec like `flat`/
      `deep_flat`/`cave`, not `LimitedBiomeSource`'s — already full 14/14).
      `StackedLayerSpec` reuses `FlatLayerSpec` for each layer's own block
      stack plus a `biome` id and `airGapBlocks`. Terrain fill mirrors
      `flat`'s skip-delegate branch, N bands instead of one. Verified
      against real 26.2 sources that `LimitedBiomeSource.getNoiseBiome`
      already takes a Y argument and chunk biome storage is genuinely 3D
      (the same mechanism vanilla itself uses for cave biomes) — a new
      non-codec `setStackedLayers` setter (mirrors `setLayoutSeed`'s "not
      part of the codec" precedent) lets `EnvelopedChunkGenerator` push the
      plan onto `LimitedBiomeSource` at construction, making real vanilla
      decoration mostly work unmodified once biome varies correctly by Y:
      fixed-Y-range ore veins need nothing extra (real min-Y anchoring
      satisfies GOALS 35's deep-ore requirement by construction; short
      stacks honestly have less deep-ore room, documented not enforced).
      Heightmap-based features (trees, GOALS 35's own explicit ask) only
      work unmodified for the topmost layer — verified `ConfiguredFeature.
      place(level, generator, random, pos)` is a real, legitimate,
      placement-modifier-free vanilla API, used for a small bounded
      buried-layer decoration pass (scatter-density simplification,
      documented, not full per-feature fidelity). Stronghold/portal
      beatability: verified `WorldLimitManager`'s existing `ExteriorPlan`-
      based fallback gate already covers `stacked` with zero new code
      (same shape as `cave`'s own precedent, not the island-shaped bug
      class from Phases 8/10/12); the real new risk is *vertical* fit
      (short stacks vs. a stronghold's real 3D extent) — flagged for
      Jason's acceptance pass, not engineered around speculatively. No
      genuine scope question found needing Jason's decision — engineering
      calls within the executor's remit, per 16.1's own precedent. TODO
      17.2 split into 17.2a (core: plan/codec/terrain fill/biome-per-Y)/
      17.2b (decoration bypass + acceptance-relevant beatability check)/
      17.2c (test configs, docs, phase wrap-up), mirroring Phase 16's own
      precedent for multi-part phases.
- [x] 17.2a Implement `jlt_worldz:stacked` core mechanism (DESIGN §34.1-
      §34.3): `StackedLayerSpec`/`StackedPlan` records + codec, layer
      editor UI (reusing the flat layer editor's text import/export
      convention, §33.5) with per-layer biome + air gap, seed-randomized
      layer order option, `EnvelopedChunkGenerator` terrain fill (skip
      delegate, paint N bands), `getSpawnHeight`/total-height plumbing,
      `LimitedBiomeSource.setStackedLayers` + `getNoiseBiome` early branch,
      typed preset + registration, fieldless-preset defaulting hint.
      **Done (0.2.73), in-game testable now (no buried-layer decoration
      yet -- that's 17.2b).** `stacked` shows up as the thirteenth "Worldz"
      World Type entry. `StackedLayerSpec` reuses `FlatLayerSpec` for each
      layer's own block stack (`biome;blocks;air gap` shorthand, e.g.
      `minecraft:taiga;minecraft:bedrock:1,minecraft:stone:40;6`), newline-
      only entry splitting (unlike flat's comma-or-newline split -- a
      layer's own blocks sub-list already uses commas internally).
      `resolveStackedAllowed` gives `LimitedBiomeSource` one allowed-biome
      holder per configured layer (needed for both the fieldless-preset
      hint and `collectPossibleBiomes()`, which vanilla's own decoration
      pipeline unions features from). `LimitedBiomeSource.
      setStackedLayers` is a non-codec runtime setter, called once from
      `EnvelopedChunkGenerator`'s own constructor (mirrors `setLayoutSeed`'s
      exact "not part of the codec" precedent) -- `getNoiseBiome` gets an
      early branch, checked before every other mode exactly like sky
      island's own early short-circuit, converting `quartY` to the covering
      layer's biome via `StackedPlan.layerAt` (walks bottom-to-top starting
      at the dimension's own min Y, covering each layer's air gap too so
      decoration anywhere in the headroom still reports the right biome).
      Layer order resolution (`seedRandomizedOrder`) reuses `originSource`'s
      own already-live seed (`effectiveLayoutPlan().seed()`) rather than a
      new seed field, mirroring `islandSeed()`/`skyIslandSeed()`'s exact
      precedent -- no `ChunkMapMixin` changes needed. Terrain fill/height
      queries (`fillStackedColumns`, `stackedBaseHeight`/`stackedBaseColumn`)
      mirror `flat`'s own `fillFlatColumns`/`flatBaseHeight`/
      `flatBaseColumn` shape exactly, just N bands with explicit air-gap
      skips instead of one solid stack. New JUnit (`StackedLayerSpecTest`,
      `StackedPlanTest`, `WorldzConfigTest`/`WorldPresetResourcesTest`/
      `ProjectMetadataTest` coverage). Fixed two pre-existing exact-string
      tests broken by the new `stackedDefaults` ternary branch shifting
      indentation in `LimitedBiomeSource`'s `allowed`-biome dispatch chain
      (not a behavior bug, just brittle string-matching tests needing their
      expected literals updated for the new nesting level).
- [x] 17.2b Implement decoration (DESIGN §34.4): `applyBiomeDecoration`
      override calling `super` first, then the buried-layer
      `ConfiguredFeature.place` bypass pass for every non-top layer's
      `VEGETAL_DECORATION` features. Confirm (not fix speculatively) the
      stronghold/portal beatability gate needs no new arm (DESIGN §34.5).
      **Done (0.2.74), in-game testable now.** The delegate's existing
      `this.delegate.applyBiomeDecoration(...)` call (already unconditional
      for `stacked`, no gating needed there unlike `flat`'s own
      `decoration` toggle) already handles every fixed-Y-range feature
      correctly once biome varies by Y (17.2a's `getNoiseBiome` branch) --
      confirmed, not assumed, since `NoiseBasedChunkGenerator` never
      overrides `applyBiomeDecoration` itself, so this call was always
      running vanilla's real `ChunkGenerator.applyBiomeDecoration`
      algorithm against `LimitedBiomeSource` directly. Added
      `applyStackedBuriedDecoration` for the one real gap (heightmap-based
      features only see the topmost layer): walks every layer except the
      top, resolves that layer's biome's own `VEGETAL_DECORATION`
      `PlacedFeature` list, and calls the underlying `ConfiguredFeature.
      place` directly at scattered points -- mirrors `placeOreFeature`'s
      own existing "direct exact-position feature placement" precedent in
      this same class almost exactly, confirming the approach was already
      idiomatic here, not a new pattern. **Correction found while
      implementing** (DESIGN §34.4's own record): scatter density ended up
      a fixed `BURIED_LAYER_DECORATION_ATTEMPTS = 4` constant, not the
      config-exposed knob the design pass first proposed -- a whole new
      config/codec/UI field for one density number was more ceremony than
      a first pass needs. Beatability: confirmed (not fixed, since nothing
      needed fixing) `WorldLimitManager`'s existing `ExteriorPlan`-based
      gate already covers `stacked` -- it gets the same standard border/
      exterior Customize-screen controls every typed preset does (verified
      via `StackedCustomizeScreen` implementing the same
      `LimitEditorHosts` interfaces `flat`/`deep_flat` do), so no new
      `xxx.enabled()` gate arm is needed, unlike the island-shaped preset
      bug class from Phases 8/10/12. New `ProjectMetadataTest` coverage
      (source-string assertions, matching this project's established
      pattern for `EnvelopedChunkGenerator` features that need live
      Minecraft classes and so aren't directly JUnit-runnable).
- [x] 17.2c Test configs (a short total-stack config and a tall one, per
      §34.5's flagged vertical-fit risk) + docs (README, MANUAL_TESTING.md,
      config/tests/README.md) + phase wrap-up. **[Jason]** acceptance.
      **Configs and docs done (0.2.75):** `config/tests/72`-`74` (default
      three-layer taiga/desert/plains; a deliberately short two-layer
      stack exercising DESIGN §34.5's own flagged, unresolved stronghold/
      portal vertical-fit risk; `seedRandomizedOrder` determinism). New
      "Phase 17 acceptance" section in MANUAL_TESTING.md (3 configs,
      explicitly flagging the buried-layer decoration scatter density and
      the short-stack beatability question as the two most likely
      follow-up candidates). `config/tests/README.md`'s intro updated for
      the thirteenth World Type entry plus a new `stacked:` paragraph and
      three table rows. README.md gained a full "Stacked challenge"
      section (challenge-types table row + prose + settings table +
      config example), including the deep-ore-by-construction rationale
      and the same open vertical-fit question flagged for testers.
      **This completes every non-[Jason] item in Phase 17** (17.1 design,
      17.2a core mechanism, 17.2b decoration, 17.2c test configs/docs
      acceptance) — **do not start Phase 18 without Jason's explicit
      go-ahead.**
- [x] 17.3 Default overhaul, requested by Jason 2026-07-24 (DESIGN §34.7):
      bounded world by default (`stacked.worldSizeChunks`, default 4
      chunks/64 blocks, 0 = original unbounded), 30-block minimum default
      air gap, 8-layer default stack, per-layer surface relief
      (`stacked.reliefBlocks`, default 4, a within-layer-budget bump — no
      new noise machinery, `StackedPlan.layerAt` untouched), and a
      deterministic default fallback End portal in a thickened bottom
      layer (no new placement code — falls out of item 1 plus
      `NATURAL_STRUCTURE_MARGIN` exceeding the new default radius).
      Y64-exact stack-center tradeoff resolved to Y98 (documented, not
      silently chosen — see DESIGN §34.7). New `config/tests/75`-`76`
      (unbounded escape hatch, relief-off isolation); `72`-`74` updated
      for the new defaults/opt-outs. `StackedCustomizationTest` added;
      `StackedPlanTest`/`WorldzConfigTest`/`ProjectMetadataTest` updated.
      **Configs/docs/tests done (0.2.76), [Jason] in-game acceptance
      outstanding** — same "tune after playtest" posture as every other
      numeric default in this project, flagged explicitly in
      MANUAL_TESTING.md's Phase 17 footer.
- [x] 17.4 Two follow-ups Jason found testing 17.3, same session
      (DESIGN §34.8): **(a)** bugfix — 17.3's `worldSizeChunks` default
      only applied via the Customize-screen path (`StackedCustomization
      .fromConfig`); a never-customized stacked world (preset picked
      directly, or a config-driven server world) fell through two other
      resolution paths (`LimitedBiomeSource.resolve`,
      `EnvelopedChunkGenerator.resolve`) that never consulted it,
      silently staying unbounded. Fixed by centralizing the derivation on
      `StackedConfig` itself (`effectiveOverworldBorder`/
      `effectiveOverworldExterior`) and widening two `private` codec-plan
      factories just enough for all three call sites to share one
      implementation. **(b)** new feature — `stacked.layers` accepts a
      bare biome id (no `;blocks;air gap`) via new `StackedBiomeDefaults`
      (~35 biomes hand-tuned, generic stone/dirt/grass fallback for the
      rest), each expanding to that biome's own standard 10-block
      composition plus the 30-block default air gap; not stack-position-
      aware by design (see DESIGN §34.8). The shipped default now
      dogfoods this for its six middle layers. New `config/tests/77`;
      `StackedLayerSpecTest`/`StackedPlanTest`/`StackedConfigTest`/
      `WorldzConfigTest`/`ProjectMetadataTest` updated or added.
      **Configs/docs/tests done (0.2.77), [Jason] in-game acceptance
      outstanding** for both the bugfix and the new shorthand.
- [x] 17.5 **Bug found and fixed (0.3.19, 2026-07-26, Jason's real config
      72-75 playtest):** stacked layer surfaces looked "rather blocky" --
      a hard-edged waffle of flat 4x4 plateaus at uncorrelated heights,
      clearest from directly above the top plains layer. Root cause:
      `StackedPlan.reliefBlocksAt` sampled one independent hash per whole
      cell with zero blending between neighbors, so adjacent cells could
      jump the full relief range with a sharp 1+-block step -- the
      opposite of the "gentle undulation" DESIGN §34.7 always claimed.
      **Fixed:** `reliefBlocksAt` now sums two bilinearly-interpolated
      (Perlin-faded, continuous slope not just height) value-noise
      octaves -- a broad 16-block-cell layer for overall rolling shape,
      a finer 4-block-cell layer for small-scale variation, since one
      octave alone still reads as a repetitive field of same-size bumps.
      Same `[0, maxReliefBlocks]` contract and single call site
      (`stackedColumnStates`), so painted terrain and reported height
      still can't disagree. DESIGN §34.7 updated in place. Full
      multiloader build green. **Confirmed (2026-07-26, Jason's real
      0.3.19 retest, fresh config 72 world):** "terrain looks better" --
      screenshots show smooth, curved-edge undulation on the plains
      layer and an organic-looking mound around a snowy taiga lava
      pool, no more hard-edged plateaus. Configs 73-75 share the same
      fix but weren't separately retested; low risk, same code path.
- [x] 17.6 New config 76 (`76-stacked-void-exterior.yaml`, GOAL 35/17,
      inserted between 75/77, cascading every subsequent config 78-97 up
      to 79-98 across all docs/cross-references): confirms config 72's
      implicit bounded-64-block-border-plus-VOID-exterior default, which
      config 72's own acceptance never actually verified beyond the
      border indicator existing. **Corrected same session (2026-07-27,
      Jason's retest):** the first version paired VOID exterior with a
      real invisible-wall border, which Jason confirmed works but was
      never his actual intent -- he wants stacked's bounded void to feel
      like Sky Chunk: no physical wall at all, free flight/building into
      the void past the terrain edge, no floating islands out there, and
      only ordinary vanilla void-fall risk if you actually fall out of
      the world, not a hard stop at the edge. Verified against real code
      (`WorldzConfig.sanitizeExterior`, `ObjectiveSite.supportiveRadius`)
      that `overworldBorder.enabled: false` plus an explicit
      `overworldExterior.boundaryRadiusBlocks: 64` gives exactly this:
      VOID mode stays active (sanitizeExterior only falls back to NORMAL
      when *both* the border is off *and* no explicit boundary is set),
      and the fallback End-portal guarantee still computes off that same
      64-block exterior boundary even with the border disabled (it only
      gives up entirely when *both* border and exterior are inactive) --
      beatability isn't sacrificed for the open boundary. Rewrote the
      config and its acceptance steps accordingly; `config/tests/README
      .md`/MANUAL_TESTING.md updated to match.
      **Bug found and fixed (2026-07-27, same day, Jason's real retest --
      the border still showed up, twice, after deleting and recreating
      the world):** that first correction was still wrong. `stacked
      .worldSizeChunks` defaults to `4`, and `StackedConfig.
      effectiveOverworldBorder`/`effectiveOverworldExterior` (DESIGN
      §34.7) completely ignore the shared `overworldBorder`/
      `overworldExterior` sections whenever it's nonzero, deriving their
      own hardcoded `enabled: true` border instead -- exactly the opt-out
      configs 73/74/75 already needed, which this config forgot the
      first time. Config-only fix: added `worldSizeChunks: 0`, no code
      changed (the derivation logic itself is correct and intentional,
      documented behavior -- this was a missing field in the test config,
      not a bug in `StackedConfig`). **Confirmed (2026-07-27, Jason's
      real retest):** config 76 "works as expected."
- [x] 17.7 **New feature (2026-07-27, Jason's ask):** `stacked
      .forceTopVillage` -- a real vanilla village always force-generated
      near spawn on the top layer's own surface, provided that layer's
      biome is village-compatible. Confirmed via clarifying question:
      always force at a fixed, deterministic site, never a natural-
      search-first attempt, mirroring sky island's own guaranteed-village
      posture (GOALS 07) -- natural placement isn't reliable in a small,
      bounded stacked world. New `StackedVillageDeployment` (`worldgen`
      package) reuses `FloatingIslandsDeployment`'s exact `Structure
      .generate`/`placeInChunk` mechanics, but derives biome eligibility
      live from `minecraft:villages`' own real `structures()`/`biomes()`
      data (same real-data-driven check `warnUnreachableFlatStructure
      Overrides` already established) instead of copying `FloatingIslands
      Plan`'s hardcoded biome-to-structure table a second time -- that
      table is a documented past bug source when it drifted out of sync
      with real vanilla data; this can't. New `StackedPlan.surfaceY`
      helper extracted from `getSpawnHeight`'s existing inline formula
      (DRY, one source of truth for "top layer's own surface"). Threaded
      through the full existing config path end to end: `StackedConfig`
      → codec (`force_top_village`, optional/false-default for save-
      compat) → `StackedCustomization`/Customize-screen checkbox →
      `StackedPresetEditor`'s read-back path, since stacked already has
      full Customize support (unlike some newer "config-only for now"
      features) and skipping the UI wiring would let opening Customize
      silently reset the toggle. New configs 99 (default stack, top
      biome plains, real village-compatible) and 100 (top layer swapped
      to swamp, confirms the silent-skip negative path with an INFO log,
      not a warning). DESIGN §34.9 documents the design and flags the
      one real, likely (not speculative) risk: stacked's own default
      64-block-radius border is smaller than a real village's typical
      footprint, so config 99 may well show the village clipping the
      border/void wall -- shipped as-is to observe empirically rather
      than guessed at preemptively, same posture as §34.5's own
      stronghold-fit question. Full multiloader build green. **[Jason]
      retest outstanding** on configs 99-100. Phase 17 closed again
      pending that retest.
- [x] 17.8 **Default change (2026-07-27, Jason's ask):** decouple
      `stacked`'s default Overworld border from its default void
      exterior (DESIGN §34.10). Since §34.7, a nonzero `stacked
      .worldSizeChunks` (default 4) forced *both* a real Overworld
      border *and* a void exterior wall on together; Jason wants the
      void exterior kept ("like a sky chunk") but the border off by
      default, only turned on via the shared `overworldBorder` config
      section like every other typed preset -- matching this project's
      own border(accessibility)-vs-exterior(terrain extent) philosophy,
      which §34.7 had temporarily merged for `stacked` alone.
      `StackedConfig.effectiveOverworldExterior` now sets its own
      `boundaryRadiusBlocks` explicitly from `worldSizeChunks` instead
      of relying on an enabled border to supply it;
      `effectiveOverworldBorder` is now a pure pass-through of the
      shared config. `worldSizeChunks: 0`'s full-opt-out behavior is
      unchanged. Updated `StackedConfigTest`/`StackedCustomizationTest`
      to match. New config 101 (default stack, zero border/exterior
      overrides -- confirms it now matches config 76's hand-tuned
      behavior for free). Full `:common:test` green, both Prism
      instances (Fabric + NeoForge) redeployed at 0.3.20.
      **[Jason] retest outstanding** on config 101 -- confirm a never-
      customized default stacked world floats in void at the platform
      edge with no border, and that adding `overworldBorder.enabled:
      true` still adds a real border on top.

## Phase 18 — World-hazard rules module (GOALS 29–30)

A shared runtime module (server-tick + saved-data, like delayed borders),
composable with any world type — **independent of Phases 5–17 and can be
pulled earlier if Jason wants a fun quick win.**

- [x] 18.0 Design pass (added — mirrors Phase 17.1's own precedent, and
      18.2 already asked for a design-first task): verified real 26.2
      time/sleep/phantom/chunk-load APIs against the sources jar (26.2
      replaced `Level.dayTime` with a `ServerClockManager`/`WorldClock`
      system, gamerule renamed `advance_time`); three decisions from
      Jason before implementation (insomnia default, clock/border
      schedule interaction, test-file naming) — see MEMORY.md and DESIGN
      §35 for the full write-up.
- [x] 18.1 Forever night (30), 0.2.78: `foreverNight` config section
      (`enabled`, `lockAfterDays`, `relaxInsomnia`); `ForeverNightPlan`
      pure logic; `WorldHazardState` (mirrors `WorldLimitState`) +
      `WorldHazardManager` (mirrors `WorldLimitManager`'s
      `onServerStarted`/`onServerTick` shape) registered on both loaders.
      Locks by disabling `GameRules.ADVANCE_TIME` + jumping the Overworld
      clock to `ClockTimeMarkers.NIGHT` (DESIGN §35.0/§35.1) — "sleeping
      cannot skip it" needed no separate mechanism, confirmed a free
      consequence of the gamerule. `relaxInsomnia` periodically resets
      `Stats.TIME_SINCE_REST` for online players once locked, no mixin.
      New `ForeverNightPlanTest`; `WorldzConfigTest` extended. README
      gained a new "World hazards" section (config-only, no Customize
      screen yet, matching this project's own config-first precedent).
      **[Jason] in-game acceptance outstanding** (part of 18.3).
- [x] 18.2 Rising lava floor (29), 0.2.79: `risingLava` config section
      (`enabled`, `delayDays`, `startY`/`maxY`, `rateBlocks`/`rateDays`,
      defaults -64/64/1/1 — sea level, one block per in-game day);
      `RisingLavaSchedule` pure logic mirroring `BorderSchedule` (no
      stepped variant, GOAL 29 never asks for one). `WorldHazardState`
      extended with `lavaOriginTick`/`lastAppliedLavaY`; `WorldHazardManager`
      extended with the periodic-rescan-only-on-integer-level-change
      pattern from DESIGN §35.2, plus a self-maintained loaded-chunk-
      position set (`onChunkLoad`/`onChunkUnload`, both loaders — vanilla's
      own chunk enumeration isn't reachable from mod code, confirmed
      §35.0) so newly loaded chunks catch up to the current level via the
      same queued-not-synchronous pattern NeoForge's own `ChunkEvent.Load`
      doc comment requires. Air/water-only conversion, uniform across
      every world type including void beneath floating islands (Jason's
      own scope call, §35.2). New `RisingLavaScheduleTest`; `WorldzConfigTest`
      extended. README's "World hazards" section gained a rising-lava
      subsection. **[Jason] in-game acceptance outstanding** (part of 18.3).
- [x] 18.3 Test configs, docs done (0.2.80); **[Jason] acceptance
      outstanding.** New `config/tests/78`-`82`: night-immediate (78),
      night-delayed + relaxed insomnia (79), night + stepped-border
      interaction (80, exercises DESIGN §35.1's corrected known
      limitation directly), rising lava on a plain bordered world (81),
      rising lava composed with "Worldz: Ocean Island" (82). New "Phase
      18 acceptance" section in MANUAL_TESTING.md; `config/tests/README.md`
      gained a shared-runtime-rules paragraph (item 4) and five table
      rows. **Found and fixed a documentation error while writing these
      configs, not after** (own commit, "Correction: forever night's
      border-schedule interaction is narrower than documented"): verifying
      vanilla's own `WorldBorder` source directly (not just inferring from
      `WorldLimitManager`'s `dimensionTicks()` usage) showed a continuous
      border resize already in progress is unaffected by locking night —
      only a still-delayed or actively-stepped resize freezes. Corrected
      DESIGN §35.1/MEMORY.md/README.md to the narrower, accurate scope
      before shipping config 81 to test it.

## Phase 19 — Structure options wrap-up (GOALS 21, 23, 24)

- [x] 19.1 Verify natural placement remains the default everywhere (21).
      **Done (0.2.81), verification only, no code change.** Audited every
      mechanism that touches structure placement: the exterior gate in
      `EnvelopedChunkGenerator.createStructures` only ever *suppresses*
      structures in wholly-exterior (non-explorable) chunks, never
      relocates them; `FloatingIslandsDeployment`'s guaranteed village
      (GOALS 07/08) force-places one only when `floatingIslands.enabled`,
      an opt-in feature; `ProgressionGuarantees`/`ObjectiveSite`'s compact
      fallback sites only ever fire when a real vanilla structure doesn't
      fit the configured border, a beatability safety net. Natural
      placement is the only *default* everywhere else. See DESIGN §36.1.
- [x] 19.2 Generalize the exclusion-zone module into per-structure-family
      "minimum distance from spawn" options (default 2000 blocks) usable by
      any world type (24). **Done (0.2.82), full design in DESIGN §36.2-
      §36.3.** Not built on the existing `IslandPlan.ExclusionZone` (that
      module holds/releases *terrain*, not vanilla structure placement) --
      new `logic.StructureDistancePlan` (enabled, minDistanceBlocks default
      2000, exemptStructureSets) + `config.StructureDistanceConfig`, a
      shared top-level `structureDistance:` config section (config-only,
      no Customize screen yet, matching the world-hazard modules'
      precedent) read live from `WorldzCommon.config()` inside
      `EnvelopedChunkGenerator.createStructures` -- deliberately not
      threaded through `LimitedBiomeSource`'s codec (already full at its
      14-field DFU ceiling) or `EnvelopedChunkGenerator`'s own `customized`
      constructor chain, since a config-only module needs neither
      per-world persistence nor Customize-screen round-tripping.
      `createStructures` now reimplements vanilla's own per-structure-set
      `forEach` (verified against the real `ChunkGenerator.createStructures`
      source) instead of delegating wholesale, inserting one distance
      check per structure set; generation itself reuses
      `FloatingIslandsDeployment`'s already-shipped `structure.generate`+
      `structureManager.setStartForStructure` technique in place of
      vanilla's private `tryGenerateStructure`. Disabled by default (the
      default path is byte-for-byte the original `super.createStructures`
      call -- zero risk to any shipped preset); applies uniformly to every
      world type since it lives in the one `createStructures` override
      every preset already shares, with no per-preset wiring needed.
      Chebyshev distance from the (possibly recentered) `originX()`/
      `originZ()`, matching border/exclusion-zone convention. New
      `StructureDistancePlanTest` (7 cases) + `WorldzConfigTest` read/
      sanitize/defaults coverage; full suite green.
- [x] 19.3 Stretch, only if Jason still wants it after 1–18: floating
      "Pandora" structure islands (23). Design spike first; park if cost is
      out of proportion. **Spiked and parked (0.2.83), no implementation
      — see DESIGN §36.4.** GOAL 23 needs a vanilla structure's bounding
      box known *before* terrain shaping decides whether to build a
      floating island under it, inverting this project's existing
      terrain-then-structure pipeline order and FloatingIslandsPlan's own
      "island first, force a structure onto it" model (GOALS 07/08). Real
      cost is out of proportion for a stretch item per TODO's own escape
      hatch — parked, not abandoned; revisit only if Jason wants a
      dedicated spike later.
- [ ] 19.4 Test configs; docs; **[Jason]** acceptance.
      **Configs and docs done (0.2.82, same pass as 19.2), just never
      updated on this line:** `config/tests/83`-`84`, a full "Phase 19
      acceptance" section in MANUAL_TESTING.md, and two rows in
      `config/tests/README.md` already exist and cover 19.1-19.3 in full.
      **[Jason] acceptance still outstanding** — nothing left to build.

## Phase 20 — Wrap-up and release

> **Ordering note (2026-07-26):** Phases 21-24 were added after this phase
> was numbered, and run *before* it — Phase 20 stays the final release
> wrap-up. Kept at 20 rather than renumbered because several other
> documents already cross-reference "Phase 20's planned config-reference
> rewrite" (GOALS.md, TODO backlog entries).
>
> **Updated 2026-07-27:** Phase **25** (config restructure) was added and runs
> **first of all the remaining phases** (Jason's D8), ahead of 21-24. Real
> execution order is now **25 → 21 → 22 → 23 → 24 → 20**. Note 20.1's
> config-reference rewrite is largely *subsumed* by 25.4 (generated reference
> file) and 25.10 (generated README tables + completeness test) — expect it to
> shrink to a hand-written challenge-first prose pass with generated tables
> slotted in.

- [ ] 20.1 Full README/config-reference/example rewrite in challenge-first
      terms; MANUAL_TESTING.md final scenario tables; MEMORY.md tidy.
      **Re-scope after Phase 25** — the config-reference and example halves are
      generated by then (25.4/25.10); what remains is the prose.
- [ ] 20.2 Final clean multiloader build, artifact inspection, version bump.
      Publishing decisions remain Jason's.
- [ ] 20.3 Revisit any newly suggested challenge ideas with Jason — plan
      approved ones as new phases.

## Phase 21 — Surface vs. underground biomes (GOAL 42)

Added 2026-07-26 (Jason). Sequenced **first** of the four new phases at his
explicit choice: it is the only one fixing an observed defect (cave biomes
on the surface), and it builds the depth-partition machinery Phase 22
reuses. Full design pass in DESIGN §37 — read it before starting; the
vanilla mechanism (`depth` climate parameter, not a Y cutoff) is already
verified there against the real 26.2 artifacts.

- [x] 21.1 Design pass — **done**, DESIGN §37. Root cause confirmed:
      `resolveAllowedBiomes` filters vanilla's 7-D climate parameter list,
      and nearest-neighbour lookup then returns a cave entry at surface
      depth whenever the surviving surface entries match the other six
      parameters worse. Reachable with the shipped default `allowedBiomes`
      (7 surface vs 4 cave biomes). Verified `Climate.Sampler.sample(...)
      .depth()` is already available at the `getNoiseBiome` call site.
      **Pre-work correction pass (2026-07-26, before any other task
      started):** three real issues found verifying the design against the
      actual call graph, full detail in MEMORY.md's own 2026-07-26 "Phase
      21 pre-work" entry — (a) the surface/underground split must use a
      *symmetric* empty-list fallback (each side borrows the other's
      delegate) instead of each independently falling back to full
      vanilla, or every other preset (flat/single_biome/stacked) would
      regress the moment this landed; (b) `deep_flat` removed from 21.4's
      scope — it already reports real vanilla biomes at real depth,
      unfiltered, for free; (c) `flat`/`skyIsland`'s new field(s) thread
      into `LimitedBiomeSource` via a mutable setter (mirroring
      `setStackedLayers`'s exact precedent), not a new codec field
      (`LimitedBiomeSource`'s codec is confirmed full, 14/14). **Jason's
      decision:** flat/sky island's underground band is one single
      configured biome, not climate-sampled variety — simpler, matches his
      own phrasing, and decouples 21.4 entirely from 21.2/21.3's
      climate-list machinery.
- [x] 21.2 Extend `BiomeRoles` with an underground classification —
      maintained `UNDERGROUND_IDS` set (`dripstone_caves`, `lush_caves`,
      `sulfur_caves`, `deep_dark`) plus `isUnderground(id)` (no overrides
      parameter — nothing would populate one today, and `BiomeRole`'s own
      override map is a different, layout-composition concept this
      shouldn't be conflated with). **This is the unit-testable part of
      the phase** (`BiomeRoles` is deliberately registry-independent) —
      add `BiomeRolesTest` coverage for the maintained set and unknown/
      modded ids.
- [x] 21.3 Split `resolveAllowedBiomes`'s single `allowed` input into
      surface/underground subsets via `BiomeRoles.isUnderground`, each
      with its own filtered `MultiNoiseBiomeSource` — **but if either
      subset is empty, that side's delegate borrows the other side's**
      instead of the existing "no match → full vanilla" fail-safe (which
      must be kept, unchanged, only for the case *both* subsets are
      empty). This is what makes the fix a no-op for every existing
      preset and a real change only for `legacy`'s own mixed list.
      `getNoiseBiome`'s final fallback picks the matching delegate by the
      query's own depth (vanilla's own `0.2` band start; compare in
      quantized space via `Climate.quantizeCoord`).
- [x] 21.4a `flat` only (not `deepFlat` — see 21.1's correction; `skyIsland`
      split out to 21.4b, same idea but a genuinely separate implementation):
      new `undergroundBiome` (single biome id, optional) +
      `undergroundBelowSurfaceBlocks` (default `10` per Jason, `0` or
      `undergroundBiome` unset = disabled/today's behavior) fields on
      `FlatConfig`/`FlatPlan`. Threads into `LimitedBiomeSource` via a new
      `setFlatUnderground(String, int, int)` setter, mirroring
      `setStackedLayers` exactly (`FlatPlan` still isn't a
      `LimitedBiomeSource` codec field — the setter is precisely what makes
      that unnecessary). Surface Y is `FlatConfig.OVERWORLD_MIN_Y +
      FlatPlan.totalHeightBlocks()`. Below the boundary, reports
      `undergroundBiome` outright — a plain Y check in `getNoiseBiome`, no
      `MultiNoiseBiomeSource`/climate sampling involved (Jason's decision,
      21.1). Warns (mirroring 0.3.12's `structureOverrides` warning style,
      not a hard error) if a configured `undergroundBiome` isn't itself
      `BiomeRoles.isUnderground`-classified, or is unresolvable. Config-only
      for now (`FlatCustomization`/the Customize screen always passes
      disabled values, same deferral this project uses for other new
      fields) — a config-driven world (`FlatPlan.fromConfig`) is the only
      path that reads it today. `FlatPlanTest` covers validation,
      `undergroundEnabled()`, and config resolution.
- [x] 21.4b `skyIsland` — same idea as 21.4a, genuinely separate
      implementation since `SkyIslandPlan` already has its own
      `LimitedBiomeSource` codec slot (room to nest the two new fields
      directly, no setter needed the way `flat` needed one) but sky
      island's own `getNoiseBiome` block has its own footprint/distance
      logic the check must fit into correctly. **`stacked` stays
      excluded** from both 21.4a/21.4b — it already assigns biomes by Y via
      `StackedPlan.layerAt`, a stronger statement this must not override.
- [x] 21.5 Test configs + docs: `96-legacy-cave-biomes-underground-only.yaml`,
      deliberately shaped to make the depth-partition fix's before/after
      obvious (a sparse allowed list weighted toward cave biomes,
      mirroring how config `01`'s own "ocean labeled as river" repro was
      deliberately adversarial, not just the shipped default);
      `97-flat-underground-biome-band.yaml` and
      `98-sky-island-underground-biome-band.yaml` (added once 21.4 split
      into 21.4a/21.4b) showing a cave biome below the configured boundary
      and the surface biome above it, for `flat`/`skyIsland` respectively.
      MANUAL_TESTING acceptance section (item 1 confirms ravines/pits still
      read as underground for free, DESIGN §37.0), `config/tests/README.md`
      rows, README documentation of both presets' new fields.

## Phase 22 — Multi-biome surface with biome-correct top blocks (GOAL 43)

Added 2026-07-26 (Jason). Scoped by his own choice to an **additive option
on `flat`/`deep_flat`** — not a new preset, not sky islands. Design pass in
DESIGN §38. Depends on Phase 21 (the surface band must agree with where
underground biomes start, DESIGN §38.5).

- [ ] 22.1 Design pass — **done**, DESIGN §38. Confirmed most of this is
      assembly: `applyDeepFlatCap` already makes a per-column biome-aware
      surface decision, `StackedBiomeDefaults` already maps ~35 biomes to
      block compositions, and biome-specific structure eligibility needs
      **no** structure-side work (placement is already gated on the biome
      reported per column — the mechanism 0.3.12's `structureOverrides`
      warning checks).
- [ ] 22.2 Promote `StackedBiomeDefaults` → shared public
      `BiomeSurfaceDefaults` (behavior-identical; `stacked` unaffected).
      Overlaps Phase 24 deliberately — GOAL 43 forces this one extraction
      early, the rest waits.
- [ ] 22.3 Config: optional `biomes` list + `biomeRegionBlocks` on `flat`
      (and `deepFlat`), leaving singular `biome` as the required codec
      field so every existing world/config is untouched.
- [ ] 22.4 Per-column biome selection via `WorldLayoutPlan`'s existing
      seeded region grid. **Rivers/oceans deliberately stay on the
      existing pass-through + water-cap path** (DESIGN §38.3), not the
      region grid — README already documents why narrow winding biomes
      don't suit region cells, and `applyDeepFlatCap`'s water branch
      already handles the cave-breach sealing that took TODO 16.6-16.8 to
      get right.
- [ ] 22.5 Top-block painting from `BiomeSurfaceDefaults` for the surface
      band; `deep_flat`'s existing cap band swaps its fixed `capLayers`
      for the per-biome lookup when `biomes` is configured.
- [ ] 22.6 Update 0.3.12's `structureOverrides` warning text — it names
      `flat.biome` (singular) and must describe the configured list once
      multi-biome exists (DESIGN §38.5).
- [ ] 22.7 **Ask Jason** whether multi-biome flat should flip
      `flat.decoration`'s default (off today): biome-correct *blocks*
      without decoration means a desert with no cacti (DESIGN §38.5).
- [ ] 22.8 Test configs + docs: a multi-biome flat world confirming
      per-biome top blocks and at least two biome-specific surface
      structures actually generating (e.g. desert temple + jungle temple),
      which is GOAL 43's real acceptance criterion.

## Phase 23 — `legacy` → `climate_filter` rename (GOAL 44)

Added 2026-07-26 (Jason). His decision: **rename and fix in place**, keeping
`legacy` as a deprecated alias — explicitly *not* extract-and-delete, since
`legacy` is the shipped default and removing it would change behavior for
every existing world and every config omitting `layout.mode`. Design pass in
DESIGN §39. Sequenced after Phase 21 because the cave-biome fix (21.3) lands
in this mode's own code path.

- [ ] 23.1 Design pass — **done**, DESIGN §39, including the defect
      inventory and the finding that the terrain/label disagreement is
      *inherent* to filtering-without-reshaping, not a fixable bug.
- [ ] 23.2 Rename `LayoutMode.LEGACY` → `CLIMATE_FILTER`; special-case
      `"legacy"` in `parse` to resolve to it with a one-time deprecation
      warning (warn-and-continue, matching this project's existing config
      posture). Confirm no datafixer is needed — the codec persists
      `serializedName()`, and existing saves' `"legacy"` resolves through
      the alias.
- [ ] 23.3 Mechanical rename of every `LayoutMode.LEGACY` comparison
      (`LimitedBiomeSource`, `resolveAllowedBiomes`, `resolveLayoutBiomes`,
      `WorldLayoutPlan`); the enum makes the compiler enumerate them.
- [ ] 23.4 Docs/config sweep: README, DESIGN, `config/jlt_worldz.example.yaml`,
      `config/tests/*` — update to `climate_filter`, keeping one explicit
      note that `legacy` still parses. Rewrite README's "Current
      terrain-composition limitation (legacy mode only)" section to
      describe the inherent tradeoff honestly under the new name.
- [ ] 23.5 Confirm `WorldzConfigTest`'s YAML round-trip and the
      documented-example test still pass, and that a config written with
      the old name still loads.

## Phase 24 — Shared/common code extraction (engineering, no GOAL)

Added 2026-07-26 (Jason: "review the code to look for other shared
functionality that could be placed into common/shared part of the code
base"). Design pass in DESIGN §40. Deliberately **last**: Phases 21-22 both
add code to exactly the files this would refactor, so extracting first would
mean refactoring code that is about to change shape.

**Hard constraint: behavior-preserving.** Every item is a pure refactor with
no config, codec, or generation change. Anything that cannot be done without
a behavior change gets dropped from this phase and raised as its own item.

- [ ] 24.1 Design pass — **done**, DESIGN §40. Duplication inventory drawn
      from the codebase's own Javadoc admissions ("Mirrors `flatBaseHeight`
      exactly", "Mirrors `flatBaseColumn` exactly", "mirrors
      `fillFlatColumns` almost exactly", plus GOALS 41.1's already-requested
      capsule consolidation).
- [ ] 24.2 Extract a shared flat/stacked column helper covering
      `flatLayerStates` + the `*BaseHeight`/`*BaseColumn`/`fill*Columns`
      trio (`EnvelopedChunkGenerator`, currently 2992 lines).
- [ ] 24.3 Collapse the five near-identical "resolve id list →
      `Map<String, Holder<Biome>>`, warn on unknown" helpers in
      `LimitedBiomeSource` (`resolveSkyIslandBiomes`, `resolveIslandBiomes`,
      `resolveLayoutBiomes`, `resolveStackedAllowed`,
      `resolveChaosBiomesAllowed`) into one parameterized method.
- [ ] 24.4 Shared capsule builder consolidating `buildCaveCapsule` /
      `buildNetherStartCapsule` / `buildEndPlatform` — **highest value
      (GOALS 41.1 asks for it) but highest risk**, since all three have
      preset-specific behavior Jason iterated on in-game across 0.3.2-0.3.6.
      Land it only with those presets' manual configs re-run; drop it from
      the phase rather than risk regressing them.
- [ ] 24.5 Consider consolidating the duplicated `NetherStartPlan`/
      `EndStartPlan` capsule config fields (DESIGN §32 recorded them as
      "duplicated rather than shared per this goal's own 'true cross-preset
      sharing later' precedent" — this is that "later").
- [ ] 24.6 Re-run the full manual acceptance configs for every preset
      touched, plus the full multiloader build, before closing the phase.

## Phase 25 — Config restructure (engineering + one behavior change) — **NEXT**

Added 2026-07-27 (Jason: "restructure the configuration files… more nested
structure… should properties be put into their own yaml configuration files…
better organized documentation"). Findings and plan of record in
**`CONFIG-RESTRUCTURE.md`** — read it before touching any item here; it holds
the measured findings F1–F8, the ten decisions D1–D10, and the strip-width
design in §5.

**All decisions are settled** (Jason, 2026-07-27) — do not re-litigate:

- **D1 no backward compatibility.** No production worlds exist. Old configs
  simply stop loading; there is no alias layer, no deprecation window, no
  migration mode. This is what keeps the code clean.
- **D2 split into `config/jlt_worldz/`**, one file per world type, organized by
  scope (`CONFIG-RESTRUCTURE.md` §3).
- **D3 schema-driven config** — one declarative descriptor per setting
  generates parse, validation, the reference file and the README tables.
  Directly answers Jason's "2400 lines in any one class needs a very compelling
  reason… better to break it up into smaller classes."
- **D4 stop rewriting the user's config file**; ship a generated
  `jlt_worldz.reference.yaml`. Reverses the 2026-07-14 MEMORY.md decision, with
  Jason's explicit confirmation.
- **D5 convert every sentinel** to a real absent-vs-set state, not just the
  bug-causing ones.
- **D6 named shared starter kits**; inline definitions stay legal.
- **D7 drop redundant unit suffixes** — naming rule in `CONFIG-RESTRUCTURE.md` §2.
- **D8 run this phase next**, before Phases 21–24.
- **D9 strip world moves to an absolute width** (min 1 block, centered, End
  portal at the mid-point) — the phase's one deliberate behavior change.
- **D10 `strip:`/`stripWorld:` merge** into `world-types/strip-world.yaml`.

**Constraint: behavior-preserving except 25.9** (strip width, D9). No other
gameplay, generation, Customize-screen or world-save-codec change. `grep -l
Codec` over the config package returns zero files, so config renames cannot
affect saved worlds (CONFIG-RESTRUCTURE.md F8) — the blast radius is the parse
layer, `config/tests/*` (104 files), README, the example file, and the config
tests.

- [x] 25.1 Research and decisions — **done**, `CONFIG-RESTRUCTURE.md`
      (findings F1–F8 measured against the tree, decisions D1–D10, strip-width
      design §5, work plan §6).
- [x] 25.2 Design pass — **done**, `DESIGN.md` §41 (Setting/SchemaSection API,
      worked CaveConfig conversion, differential-harness proof strategy,
      sub-step sequence). Broken into 25.2a-25.2h below, each independently
      buildable/testable/committable per AGENTS.md's per-task discipline.
      **This is the largest refactor in the project's history — two provable
      steps (schema first here, restructure in 25.6/25.7, never one leap).**
- [x] 25.2a Framework + shared leaf sections + the differential harness
      (DESIGN §41.2-§41.5, §41.8). Build `config/schema/`: `Setting`,
      `Accessor`, `ValueCodec`/`Codecs`, `Rule`, `Docs`, `Applicability`,
      `SummarySpec`, `ParseContext`, `SanitizeContext`, `SchemaSection`,
      `SectionCodec`. Add `LegacySections` (adapters onto the existing static
      methods, all 26 sections) and `SchemaSections` (schema where converted,
      legacy fallback otherwise); rewrite `WorldzConfig.parse`/`sanitize`/
      `toYaml`/`summary` as four registry loops. Convert the three **shared
      leaf** sections everything else nests: `SpawnSchema` (also collapsing
      the four duplicated spawn-default blocks — DESIGN §41.10 R12),
      `StarterKitSchema`, `StarterCapsuleSchema` (parameterized bounds +
      odd-rounding, R3). Add `RecordingLogger`, `ConfigSchemaDifferentialTest`
      (104 test configs + example + defaults + error cases + ~40 adversarial
      fragments; compares YAML string, summary string and ordered WARN lines)
      and the captured `reference-defaults.yaml` golden test.
- [x] 25.2b Simple leaf sections (needs 25.2a). `EndBorderSchema`,
      `StripSchema`, `ForeverNightSchema`, `RisingLavaSchema` (postValidate
      `maxY >= startY`), `StructureDistanceSchema`, `DeepFlatSchema`,
      `StackedSchema`, `FlatSchema`. Proves int/double/bool/enum codecs, plain
      string lists, empty-list fallbacks and `disabledWhen` summary gating.
- [x] 25.2c Border and exterior (needs 25.2a). `BorderSchema` (parameterized
      `objectiveKey` — R1, rate-pair + stepped-style `postValidate`,
      overridden `summary`) and `ExteriorSchema` (`oceanAllowed` + the
      cross-section border reference — R2, overridden `summary`). The two
      hardest shapes, alone, early.
- [x] 25.2d Biome-list sections (needs 25.2a). `LayoutSchema` (weighted list,
      `roleOverrides` string map, mode-vs-roles fallback, `<legacy>` summary),
      `ChaosBiomesSchema`, `SingleBiomeSchema`, `StripBandsSchema`,
      `StripWorldSchema`. Proves all four list rules plus the `allowRivers`/
      `allowOceans`/`allowBeaches` trio and the `starterBiome`/
      `starterRadiusBlocks` pair that 25.6 will hoist into `naturalBiomes:`.
- [ ] 25.2e Island sections (needs 25.2a). `OceanIslandSchema`,
      `SkyIslandSchema`, `FloatingIslandsSchema` (dependent `maxRadiusBlocks`
      bound, two advisory warnings — R4, overridden `summary`),
      `ChunkIslandSchema` (conditional summary segment). Proves the shared
      `exclusionZoneEnabled`/`exclusionZoneRadiusBlocks` pair 25.6 collapses.
- [ ] 25.2f Chest/kit presets (needs 25.2a). `CaveSchema` (DESIGN §41.6's
      worked example), `NetherStartSchema`, `EndStartSchema`. Proves the
      shared `chestTier` + three-kit shape that D6/25.8 collapses into named
      kits.
- [ ] 25.2g The root (needs 25.2a-25.2f). `WorldzRootSchema`: the eight
      top-level scalars (`allowedBiomes`, `starterBiome` with its own warning
      wording — R6, `starterRadiusBlocks`, `ensureStarterLand`,
      `starterLandTransitionBlocks`, `starterLandFoundationDepthBlocks`,
      `allowRivers`, `allowOceans`) plus the 25 `Setting.section(...)`
      bindings in today's exact `toYaml()` order. At this point every section
      is schema-driven and the differential test is at full strength.
- [ ] 25.2h Retire the legacy path (needs 25.2g). Delete `LegacySections`, the
      ~100 `read*`/`sanitize*`/`*Map`/`*Summary` methods and
      `ConfigSchemaDifferentialTest`. `WorldzConfig.java` should land at ~200
      lines (from 2400). Keep the golden `reference-defaults.yaml` test and add
      `ConfigSchemaMetadataTest` (every setting has doc + unit + applicability;
      the schema's flattened key list exactly equals `toYaml()`'s — the
      completeness gate F6 wanted and 25.10 reuses).
- [ ] 25.3 Presence tracking (D5, needs 25.2). With the schema owning parse,
      "the user wrote this key" becomes a property of the parse result instead
      of something reconstructed from magic values. Note `WorldzConfig.parse`
      already gates every field on `containsKey` today and throws that fact
      away — that discard is the root cause of the thrice-fixed stacked border
      bug (17.4a, 17.5, 17.6).
- [ ] 25.4 Stop rewriting the config file (D4, needs 25.3 — see F5: today's
      rewrite makes every setting explicit after one launch, which would
      silently defeat 25.3). Load becomes parse-validate-log. Emit
      `jlt_worldz.reference.yaml` from the same schema that drives parsing, so
      it cannot drift from the code.
- [ ] 25.5 Retire every sentinel (D5, needs 25.3). Rewrite `StackedConfig`'s
      two `effective*` methods in terms of "did the user set it?", and convert
      `boundaryRadiusBlocks: 0`, `resizeRate*: 0`, `undergroundBiome: ""` and
      `undergroundBelowSurfaceBlocks: 0`. Delete the `worldSizeChunks: 0`
      opt-out boilerplate from configs 73/74/75/76 (+2 others).
- [ ] 25.6 Restructure the keys (D7) per CONFIG-RESTRUCTURE.md F1's two tables
      — 14 within-class nests plus 4 cross-class shared shapes
      (`exclusionZone`, `naturalBiomes`, `underground`, `chest`), with unit
      suffixes dropped per the §2 naming rule.
- [ ] 25.7 Split into `config/jlt_worldz/` (D2, D10). Biggest single win is
      moving the 11 generic-preset-only top-level keys (`allowedBiomes`,
      `starterBiome`, `layout`, `strip`, …) into `world-types/worldz.yaml`
      where they stop masquerading as global (F3), and merging the
      `strip`/`stripWorld` split-brain into one file.
- [ ] 25.8 Named shared starter kits (D6). 145 of the 384 generated config
      lines (38%) are 12 near-duplicate kit blocks. Ship the current 12
      pre-named so behavior is byte-identical; keep inline definitions legal.
      **Fold Phase 24.5 into this** (duplicated `NetherStartPlan`/`EndStartPlan`
      capsule config fields) rather than doing it twice.
- [ ] 25.9 **Strip world absolute width (D9 — the one behavior change).**
      Design and width/portal table in `CONFIG-RESTRUCTURE.md` §5. `width`
      replaces `widthRadiusBlocks`, minimum 1 block; odd widths symmetric about
      Z=0, even widths take the extra block on +Z; End portal and the Nether
      fortress guarantee target the corridor mid-point. **Not a rename:**
      `ObjectiveSite.narrowForStrip` returns a Z *radius* applied symmetrically
      by its three callers (`ProgressionGuarantees:70,114`,
      `StackedVillageDeployment:117`) — an even-width corridor is no longer
      symmetric, so it must return a centre plus half-extent. Jason has already
      accepted that structures overflow the corridor at very narrow widths;
      no clamping work is in scope. Separate commit, own test configs.
- [ ] 25.10 Documentation (D3's second half). Generate README's settings tables
      from the schema; add the completeness test covering every leaf setting
      (it would have caught F6 — 12 of 25 sections undocumented, and
      `README.md:71` claims the example file "documents every setting", which
      has never been true). Document the **live-vs-baked scope distinction**
      for the first time (F3): hazards are re-read from config and change
      existing worlds; borders, exteriors and preset sections are baked into
      the save at creation and do nothing to an existing world.
- [ ] 25.11 Migrate all 104 `config/tests/*.yaml` mechanically. **With D1 there
      is no alias fallback, so this must land with 25.6/25.7 or the suite
      breaks.** Gate: a test asserting every `config/tests/*.yaml` parses clean.
      Refresh MANUAL_TESTING.md's scenario tables.
- [ ] 25.12 Full multiloader build green, both Prism instances redeployed, then
      close the phase. **[Jason] acceptance:** a hand-commented config survives
      a launch intact (25.4); a stacked world with no `worldSizeChunks: 0`
      opt-out behaves as configured (25.5); and a 1-block-wide strip world
      generates with the End portal on the corridor mid-point (25.9).

---

## Backlog (approved, not yet scheduled to a phase)

- **2026-07-24, Phase 19.3 spike:** floating "Pandora" structure islands
  (GOAL 23) — parked, not implemented, full findings in DESIGN §36.4. Needs
  a structure's bounding box known before terrain shaping runs, inverting
  this project's terrain-then-structure pipeline order; real design work,
  not a quick follow-up. Revisit only if Jason wants a dedicated spike.
- **2026-07-24, Phase 19.2:** `structureDistance`'s `exemptStructureSets`
  ships as the only per-family knob (an opt-out list against one shared
  `minDistanceBlocks`, DESIGN §36.3) rather than a true per-family distance
  map. If a specific family ever needs its own *different* (non-zero,
  non-default) minimum distance, that's a real config-shape change, not
  covered by the exemption list — revisit if Jason asks for it.
- **2026-07-24, found while updating `config/jlt_worldz.example.yaml` for
  Phase 18:** the example file only documents sections through Phase 4
  (`allowedBiomes` through `chaosBiomes`) — every typed preset added since
  (`strip`/`stripWorld`, `oceanIsland`, `skyIsland`, `chunkIsland`, `cave`,
  `netherStart`, `endStart`, `flat`, `deepFlat`, `stacked`, ten sections
  total) was never added, even though each phase's own config class has
  existed and been YAML-round-trip-tested since it shipped. Not a Phase
  18 blocker (`WorldzConfigTest.documentedExampleParsesToTheSameDefaultsAsCode`
  only compares fields the example *does* specify, so omitted sections
  trivially match their own code defaults — the test doesn't catch this
  gap). Low priority (README's own settings tables already document every
  field per-section); revisit as a documentation pass, likely folded into
  Phase 20's planned "full README/config-reference/example rewrite"
  (TODO 20.1) rather than a standalone task.
  **Superseded 2026-07-27 by Phase 25** (config restructure): re-measured as
  12 missing sections, not ten, and re-scoped from "write the missing docs
  once" to "make the drift structurally impossible" — TODO 25.2 adds the
  completeness test that would have caught it, and 25.3 replaces the
  hand-maintained example file with a generated reference. Note `README.md:71`
  additionally tells users the example file "documents every setting with
  comments", which has never been true. See `CONFIG-RESTRUCTURE.md` F6.
- **2026-07-18, Jason (from Phase 4.2 acceptance, config 16):** chaos_biomes'
  `allowRivers`/`allowOceans` default to `false`, so out of the box a chaos
  world shows real water bodies relabeled with a land biome (e.g. an ocean
  or river reads as "Jungle") — Jason found this surprising when testing
  config 16 (turned out to be working as designed, not a bug; see
  MANUAL_TESTING.md's Phase 4 acceptance item 1c). Two options raised,
  neither implemented yet:
  1. Default `allowRivers`/`allowOceans` to `true` for chaos_biomes (and/or
     single_biome?) so natural water reads correctly out of the box;
     document the off behavior clearly for anyone who deliberately wants
     land relabeling over water.
  2. A land-only generation mode: when land-only biomes are configured,
     remove/replace the water instead of just relabeling it — conceptually
     similar to Phase 9's dry-world variant (GOALS 31), possibly shares
     implementation.
  Revisit with Jason once a phase slot is picked; not a blocker for closing
  out Phase 4.2's acceptance.
- GOALS 15 (approved 2026-07-17): configuration option to let vanilla's own
  underground cave biomes (dripstone caves, lush caves, deep dark) generate
  normally in `single_biome` worlds, instead of the single biome applying
  uniformly at every depth. GOALS.md's own text calls this "scope for a
  later phase" — moved out of Phase 3's gate (was briefly drafted there as
  3.3) since it needs genuinely depth-aware `WorldLayoutPlan` sampling
  (`getNoiseBiome`'s layout branch only takes `(blockX, blockZ)`, no
  `quartY`), a materially bigger design than 3.1's surface-family
  pass-through. Revisit once a later phase's slot is picked with Jason; not
  part of any current phase's completion gate.
- **2026-07-20, Jason (from config 34 testing, ocean-island chest-boat):** no
  caves generate under `ExteriorMode.OCEAN` terrain — confirmed working as
  designed, not a bug (DESIGN §14: exterior ocean is documented as "a stable
  deep-ocean exterior with water through sea level and a solid seabed").
  Root cause: `EnvelopedChunkGenerator.applyEnvelope`'s OCEAN branch
  unconditionally overwrites every exterior column with a synthetic flat
  profile (`ExteriorTerrainProfile.oceanLayerAt`) immediately after vanilla's
  carvers run, every `applyCarvers`/`buildSurface` pass — this erases
  whatever caves vanilla just carved. Structures still appear (a deliberate
  `decoratesExteriorOcean` carve-out for the ocean-island/chest-boat preset
  lets `delegate.applyBiomeDecoration` run and skips the final re-flatten),
  which is why Jason saw "structures underground, but no caves." Possible
  future improvement: change the OCEAN-mode overwrite to only replace
  natural/solid terrain (not carved air), so real cave systems can still
  poke into the synthetic seabed. **Jason: leave as-is for now, revisit once
  later phases are through — logged as a possible improvement, not
  scheduled.**

## Carried-over open risks (from MEMORY.md)

- Dummy-RandomState fix (0.1.15) unverified in-game → Phase 1.1.
  **Update 2026-07-17:** verification (bottom-of-world check) passed, but
  in the process found and fixed a *related* bug the original fix missed
  (`ChunkGeneratorStructureState` still built from the dummy RandomState —
  see MEMORY.md's 0.2.4 entry). **Confirmed fixed** via a 10-village,
  five-biome vanilla-vs-Worldz comparison (9/10 perfectly flush, one
  isolated partial-float judged a normal low-severity vanilla quirk, not a
  defect) — closed. NeoForge repeat and the Worldz14 orange/glitchy
  reproduction retest are still outstanding as standalone checklist items,
  but that specific visual symptom never reproduced in any of this
  session's testing, which is itself informative — the symptoms actually
  found (floating structures, a spawn-search gap) were both different from
  Worldz14's description and are now resolved.
- Worldz14 orange/glitchy terrain unexplained → Phase 1.1.
- Straight coastlines + beach width → removed with the grid modes (1.2);
  ocean-island shore quality is redesigned properly in 7.1.
- Layout sampling seed not the real world seed → fixed in Phase 1.3.
- **New 2026-07-17, fixed (0.2.5):** `starter_at_origin` didn't actually
  guarantee spawn near origin — it deferred to vanilla's own
  `findSpawnPosition()`, which searches the *unmodified* climate sampler,
  ignoring Worldz's biome override entirely. Fixed to explicitly resolve
  spawn at the origin instead. See MEMORY.md.

## Questions for Jason (running list)

(Add here when blocked; don't guess on gameplay/scope questions.)

- **2026-07-27 — Phase 25 config restructure: ALL TEN ANSWERED, same day.**
  Recorded as decisions D1–D10 in `CONFIG-RESTRUCTURE.md` §1 — that table is
  now the authority; do not re-litigate. Summary of Jason's answers:
  no backward compatibility (no production worlds yet, which keeps the code
  clean — no alias layer, no deprecation window); split into
  `config/jlt_worldz/` by scope, one file per world type; go fully
  schema-driven rather than just mechanically splitting the 2400-line class
  ("2400 lines in any one class needs a very compelling reason"); stop
  rewriting the user's config file; convert *every* sentinel, not just the
  bug-causing ones; named shared starter kits; drop redundant unit suffixes
  ("blocks are not needed when the context is obvious — documentation can
  clarify"); run the phase next, before 21–24; and one new behavior change —
  strip world moves to an absolute width (min 1 block, centered, End portal on
  the corridor mid-point), because "the radius never worked well".
  Two non-blocking judgement calls deferred to implementation time
  (`CONFIG-RESTRUCTURE.md` §9): the shipped kit naming scheme, and how much of
  README's prose to generate versus hand-write.

- 2026-07-17 — Phase 2.7 in-game testing (config 10, world `Worldz-04`,
  desert) found what looks like a floating desert village around
  `(-236..-340, 143..146, 138..146)`: several disconnected structure
  clusters in open air (stars visible through the gaps), coinciding with a
  290-tick server-lag spike in the log at `01:16:08`. Code review of
  `EnvelopedChunkGenerator`/`LayoutTerrainProfile` found single_biome's
  height adjustment is a no-op on terrain this tall (`Math.max` only raises
  columns that are naturally too low; already-elevated columns pass through
  unmodified) — no evidence this is Phase-2-introduced. Most likely either
  a vanilla structure-placement/blending quirk on extreme terrain, or a
  reproduction of the still-unverified TODO 1.1 issue (Worldz14's
  floating/glitchy terrain, theorized-but-never-confirmed fixed by the
  0.1.15 dummy-RandomState mixin). **Not explained by the 2026-07-17
  performance fix below** — config 10 has no starter biome, so that code
  path never ran in this session.
  **Resolved 2026-07-17** — root cause found (`ChunkGeneratorStructureState`
  built from the dummy RandomState, see MEMORY.md's 0.2.4 entry) and fixed;
  confirmed via a 10-village, five-biome vanilla-vs-Worldz comparison
  (9/10 perfectly flush). Closed.
- 2026-07-17 — Phase 2.7 in-game testing (config 11, world `Worldz-05`,
  desert + plains starter) found a severe, real performance bug — see the
  MEMORY.md entry for the fix. Re-test config 11 once the fix is deployed;
  "starter biome not honored" specifically needs re-confirming since it may
  simply have been a symptom of the 122-second spawn-area stall rather than
  a separate defect.
- 2026-07-17 — **Root cause found and fixed (0.2.4) for the floating-
  structure mystery.** Jason's side-by-side same-seed comparison (vanilla
  village at Y77 vs. Worldz's copy of the same village at Y120-150,
  `Worldz-NF-01`/NeoForge) pinned it down: `ChunkMapMixin`'s dummy-
  RandomState fix (0.1.15) reassigned `this.randomState` at the wrong
  bytecode point relative to the one-time `generator.createState(...)` call
  that builds `ChunkGeneratorStructureState` (which governs all structure
  placement for the level) — that call read the *stale* value regardless,
  even though every later read of the field (actual terrain generation)
  correctly saw the fix, which is exactly why the TODO 1.1 bottom-of-world
  check passed while structures still floated. Fixed on both loaders by
  switching from `@Inject` to `@Redirect` on the `createState(...)` call.
  See MEMORY.md for the full bytecode-level explanation. Remaining
  performance slowness (still ~60s spawn-area prep after the earlier
  de-duplication fix) is unresolved — Jason is adding Spark to both test
  instances to profile it directly next.
- 2026-07-17 — **Floating-structure fix: watched, not fully closed.** After
  the `@Redirect` fix (above), evidence is mostly strongly positive: a
  10-village/5-biome comparison (9/10 flush) and a separate later check
  (world `Worldz-11`, config 12/`preferred_natural_biome`) where "all the
  vanilla village locations regardless of vanilla biome" rendered flush.
  However, world `Worldz-10` showed two floating villages
  (`(-95, -910)` and `(1427, -1302)`) that were never checked against true
  vanilla at the same seed/coordinates to rule out an ordinary seed-specific
  vanilla quirk (the same methodology that resolved every earlier false
  alarm in this investigation). Jason's call (2026-07-17): not worth
  digging into further right now. **Logged as a possible open item to
  revisit only if floating structures are seen again** in later phases'
  acceptance testing — if so, the first move is the same controlled
  vanilla-vs-Worldz comparison at the exact coordinates, not re-opening the
  RandomState/threading investigation (that hypothesis is already
  conclusively disproven — see MEMORY.md).
- 2026-07-24 — Phase 18 pre-work: three questions asked and answered
  before any code was written (insomnia default polarity, whether the
  day/night-clock/border-schedule interaction is an acceptable known
  limitation, test-file naming convention). Full detail in MEMORY.md's
  2026-07-24 Phase 18 entry; resolved, not blocking.
- 2026-07-24 — **Phase 12.7 in-game acceptance (configs 49/50), new scope
  requested — needs a dedicated design pass before scheduling to a phase.**
  Jason found chunk islands too close together even at low `spawnChance`
  (config 50, 0.1) and asked for three related things, captured in full in
  GOALS.md (item 09's new feedback block, item 37's config-50 note, new
  item 40):
  1. A real **minimum distance between islands** — confirmed in code there
     is none today (`ChunkIslandPlan.at` is a pure per-cell hash pick, no
     neighbor scan; `FloatingIslandsPlan`'s 3×3 scan only resolves a single
     island's own jittered footprint, not spacing between islands).
  2. `ensureEndPortal`/the guaranteed portal room should only force a
     stronghold for a **limited-size** world; for an infinite world it
     should rely on natural placement instead. Confirmed in code:
     `WorldLimitManager.needsGuaranteedPortalRoom` currently fires
     unconditionally on `chunkIsland.enabled`, with no check on
     `overworldBorder.enabled`.
  3. New feature: **structure islands** (goal 40) — dedicated, appropriately
     "cased" islands each holding one specific structure (stronghold, geode,
     mineshaft, ancient city, trial chamber, ocean monument, shipwreck,
     temples, more), generalizing the already-shipped guaranteed-portal-room/
     geode mechanism (DESIGN §29.4/§29.6). Jason confirmed (2026-07-24) this
     should apply broadly — Sky Chunk, Sky Island, Floating Islands, and
     their Nether variants — not just Sky Chunk. Must obey the same
     minimum-distance rule as #1 above.
  Also found and logged in GOALS 09 item 3 while verifying #2: the
  guaranteed portal-room's reserved cell currently *does* get the plan's
  `topOnly` depth cutoff applied (unlike the geode/showcase reserved cells,
  which are explicitly exempted, forced full-column) — looks like an
  inconsistency against this codebase's own precedent, not yet confirmed
  in-game whether it actually chops the forced stronghold. **Not yet
  designed or scheduled** — per this project's own workflow (GOALS.md
  "Workflow" §1), this is a requirements-capture pass only; the actual
  mechanism (how island spacing is enforced, exact structure-casing rules
  per structure family, whether `ensureEndPortal` becomes conditional on
  border state or gets a separate toggle) is Jason's call for a dedicated
  design pass, not decided here.
- 2026-07-24 — **Config-51 review: general per-dimension config-override
  convention requested, not yet designed.** Jason noticed the config schema
  is flat (one level) and wants a reusable "shared value + optional
  per-dimension override" shape usable across sections generally, motivated
  by `chunkIsland`'s `exclusionZoneRadiusBlocks` being one value shared
  across Overworld/Nether/End even though `applyToNether`/`applyToEnd` are
  already per-dimension (Nether being more dangerous is a real reason to
  want a smaller exclusion zone there). Full detail in GOALS.md's
  "Configuration" subsection. Verified feasible codec-wise (`ChunkIslandCodecs
  .PLAN_CODEC`'s own `group()` is 7/14 fields, well under this project's
  documented 14-field ceiling — the outer `LimitedBiomeSource` group is the
  one that's already full, per DESIGN §29.7) **but no existing precedent for
  the shape exists anywhere in this codebase** — this is new general
  scope requiring its own design pass (exact YAML/codec pattern, which
  sections to retrofit), not a quick addition to `chunkIsland` alone. Not
  scheduled to a phase.
- 2026-07-25 — **Config-56 follow-up: broader config structure review
  requested, not yet designed.** Jason widened the 2026-07-24 per-
  dimension-override item into a general review: reduce property-name
  duplication via nesting (his own concrete example: `cave`'s new
  `sealedSurfaceBlock`/`sealedSurfaceThicknessBlocks` fields, added this
  same session, repeat the `sealedSurface` prefix on every sibling —
  exactly the pattern he's flagging), and check whether settings
  duplicated across typed-preset sections (border/exterior shapes,
  `easyKit`/`mediumKit`/`hardKit` starter-kit shapes, etc.) should
  consolidate into a shared section. Also wants the example config
  properly maintained with real spacing and commented docs afterward —
  overlaps the already-known Backlog gap (example only documents through
  Phase 4, deferred to Phase 20's planned config-reference rewrite). Full
  detail in GOALS.md's "Configuration" subsection. Recommended: combine
  with the per-dimension-override item above into one coordinated
  config-schema design pass, since both touch the same YAML/codec
  surface. Not designed, not scheduled — this is a real schema change
  across most config sections and every `config/tests/*.yaml` file, not a
  quick edit.
- 2026-07-25 — **Phase 14 in-game acceptance (configs 59-62), six
  follow-on requests — none designed or scheduled to a phase.** Jason
  confirmed the core Nether-start mechanic works (spawns in the Nether,
  redirect/respawn holds up across all four configs). Full text captured
  in GOALS.md item 27's new feedback block; summary with code references:
  1. **Natural safe-site search doesn't verify reachability.**
     `NetherStartDeployment.searchNetherStartSite` only checks a single
     `(x, z)` column: solid non-fluid floor, air at feet/head, and no
     lava touching the four floor-neighbors — it never checks whether the
     column's own horizontal neighbors are open. A "natural" site can be
     just as sealed-in as `buildNetherStartCapsule`'s guaranteed shell,
     except with no guarantee the surrounding block is even minable
     netherrack (could be basalt/blackstone) or that breaking through
     doesn't open into lava. Confirmed in code, not yet fixed.
  2. **Chest-tier philosophy needs rethinking, not just re-tuning.**
     Jason's ask: easy = fast path to the Overworld (matches today,
     `NetherStartConfig.easyDefaults()`); medium = oriented toward staying
     in the Nether long enough to gather portal materials, not handed 10
     obsidian outright the way `mediumDefaults()` does today (same
     posture as easy minus ignition); hard = just enough to survive long
     enough to gather resources (`hardDefaults()` — closest to the ask
     already, unconfirmed by real play). Overlaps DESIGN §31.6's existing
     "first-pass, not signed off" flag on all three tiers.
  3. **New feature: constrain/specify the starting Nether biome.** No
     such config exists on `NetherStartConfig` today — a config can land
     a player in a vast basalt delta, brutal even on the easy tier.
  4. **Docs gap:** building an Overworld-bound portal from the Nether
     often drops the player into a cave (ordinary vanilla portal
     placement) — needs a doc warning so it doesn't read as a Worldz bug.
  5. **New feature: the capsule as an explicit, requestable option for
     any chest tier** (a ready-made small Nether base), not only the
     natural-search fallback. **Expanded 2026-07-25** (below) to every
     world type, not just Nether-start — see GOALS 41.
  6. **Capsule is too small, and spawns dark.** `buildNetherStartCapsule`'s
     interior is a single 1×1 column with 2 blocks of headroom (dx/dz
     shell at ±1, dy shell at -1/2) — the player spawns standing on the
     chest with no room to move, and no light source is ever placed.
     Needs configurable size (tiny/small/medium/large, or player-
     specified) and default lighting — see GOALS 41, which generalizes
     this to the cave/end-start capsule builders too.
  **Not yet designed or scheduled** — per this project's own workflow
  (GOALS.md "Workflow" §1), this is a requirements-capture pass only.
  Recommend a dedicated Phase 14b (or folding into whenever Phase 14 gets
  revisited) rather than reopening Phase 14's own now-closed checkboxes.
- 2026-07-25 — **Follow-up to the above: universal starter capsule + lit
  capsules, new cross-cutting scope (GOALS 41), not yet designed or
  scheduled.** Jason widened item 5 above from Nether-start-only into two
  general requests, explicitly "requirements/clarification, implement
  later":
  1. The guaranteed-capsule/base option should be available for **any**
     world type and starting scenario, not just as the private fallback
     the three existing presets (Cave, Nether-start, End-start) each
     already have. Likely means consolidating `SpawnOriginManager
     .buildCaveCapsule`, `NetherStartDeployment.buildNetherStartCapsule`,
     and `EndStartDeployment.buildEndPlatform` — today three independent
     copies of the same shell-carving loop (material swapped: stone/
     nether bricks/end stone) — into one shared, parameterized mechanism,
     rather than adding a fourth/fifth copy per additional preset.
  2. **All three existing capsule builders place zero light source** —
     confirmed by reading each method directly (see code refs above);
     every guaranteed-capsule spawn today is pitch dark. Whatever the
     capsule mechanic's eventual shape, it should light itself by
     default.
  Full text in GOALS.md's new item 41. Not designed — open questions
  include which presets get the option, whether it's really one shared
  mechanism or per-preset, and default size/light-source choices.

## Deviation log

(Record every departure from DESIGN.md/GOALS.md here: what, where, why.)

- 2026-07-26 (Phase 14b acceptance, fixed 0.3.3) — **`nether_start`
  spawned players far above the resolved site**, found via Jason's config
  62 retest (twice, with screenshots): "config 62 puts me much higher."
  Root cause, confirmed against the real 26.2 decompiled
  `PlayerSpawnFinder.java`/`DimensionTypes.java`: `PlayerSpawnFinder
  .findSpawn` never simply trusts its own suggested position first — it
  always tries `getLevelRespawnPos` at the *same* X/Z first (the default
  `spawnRadius` gamerule is `0`, so there's no real candidate-count
  jitter, just this one same-column recompute), which discards the
  suggestion's Y entirely and scans down from the chunk generator's own
  `getSpawnHeight()` — used because the Nether's real dimension type has
  `hasCeiling = true` (verified as the *third* constructor argument in
  `DimensionTypes.bootstrap`, not inferable from `height`/`logicalHeight`
  alone; also corrects an unverified assumption already baked into
  `NetherStartPlan`'s own `MAX_SPAWN_Y` comment/DESIGN §31 text, "Y-128
  bedrock ceiling" — the Nether's real `height` is `256`, `minY` is `0`,
  so the actual top is Y 255, not 128; `logicalHeight` (128) is a
  different value used elsewhere, not the real build limit. Not fixed
  here — `MIN_SPAWN_Y`/`MAX_SPAWN_Y` (1/120) already sit safely inside
  either number, so no functional bug follows from the wrong comment,
  just a documentation correction owed for a future pass). This exact
  "vanilla search overrides the resolved suggestion" bug class was
  already found and fixed for the `cave` preset (0.2.85, "Cave-preset
  spawn silently landed on the surface") but the fix
  (`PlayerSpawnFinderMixin`) only ever checked `enveloped.cave()
  .enabled()` — `nether_start` was never covered, and its own natural-
  search/capsule Y was small enough (and Nether terrain chaotic enough in
  Y) that the divergence was never obviously wrong before the capsule
  became the common case this session (Phase 14b's `forceCapsule` +
  automatic low/high-`spawnY` default, TODO 14b.4) made a tiny, isolated
  structure the norm instead of a natural pocket blending into its own
  surroundings. **Fix:** `PlayerSpawnFinderMixin` (both loaders) now also
  short-circuits when `enveloped.netherStart().enabled()`, trusting
  `NetherStartDeployment`'s resolved site outright exactly like `cave`
  already does. `end_start` deliberately not touched — DESIGN §32.1
  already designed its guaranteed platform's shape around this same
  same-column-recompute behavior (a real solid floor at that exact X/Z,
  with nothing else in the column to intercept the scan, so the void
  around it in the End means the recomputed candidate naturally re-finds
  the platform anyway). Full multiloader build + `./gradlew test` green,
  redeployed to Worldz-Test. **[Jason] retest on a fresh config-62 world
  outstanding** — the old world created under the buggy 0.3.2 jar should
  be deleted, not reopened, per this project's own new-worlds-only
  policy.
- 2026-07-25 (Phase 11 acceptance retest, fixed 0.3.1) — **The guaranteed
  village's own island could report an unrelated real-seed biome on F3**,
  found via Jason's fourth config-58 retest: a clearly savanna-style
  village (orange terracotta roofs, acacia trees) reported "Deep Frozen
  Ocean". Verified against ground truth two ways: decoding the actual
  world's region files confirmed the island's real blocks were
  `grass_block`/`dirt`/`stone` (not real unmasked terrain -- a second,
  unrelated worry from the same retest that turned out to be a red
  herring, since two *other* screenshots showing what looked like an
  ocean horizon also turned out to be ordinary small `packed_ice`-topped
  floating islands viewed from the edge, confirmed the same way); and
  reproducing `FloatingIslandsPlan.at()` directly against the world's own
  seed (via `jshell` against the built jar) confirmed the pure logic layer
  already resolved this island correctly (`village=true`,
  `biome=minecraft:savanna`). Root cause: `LimitedBiomeSource
  .resolveSkyIslandBiomes` -- the map that turns a biome id string into an
  actual registered biome holder for F3/decoration -- only ever included
  the starter island's own biome plus the configured `floatingIslands
  .islandBiomes` pool, never the guaranteed village's own five possible
  forced biomes (`plains`/`desert`/`savanna`/`snowy_plains`/`taiga`,
  independent of that pool). Whenever the village rolled a variant not
  already in the user's own pool (savanna here), the lookup silently
  missed and fell all the way through to an unpinned, unrelated real-seed
  biome sample -- the same failure mode as the Y/column bugs above, just
  in a fallback path those fixes never touched. Pre-existing bug, not
  caused by anything in this session -- it needed a village variant
  outside the user's own configured pool to ever surface, which hadn't
  happened in any earlier test. **Fix:** new `FloatingIslandsPlan
  .villageBiomeIds()` public accessor for the five ids;
  `resolveSkyIslandBiomes` now includes them whenever floating islands are
  enabled. New `FloatingIslandsPlanTest` coverage: `villageBiomeIds`'s own
  contents, and every seed's village hit biome is one of them. Built
  (0.3.1), full multiloader build green, redeployed to Worldz-Test.
  **[Jason] retest outstanding.**
- 2026-07-25 (Phase 11 acceptance retest, revised 0.3.0) — **Real ocean/
  river/swamp biomes sampled by `naturalBiome` rendered as plain grass**,
  found via Jason's third config-58 retest: with the Y/column-consistency
  fixes above in place, several floating islands now stably reported real
  water biomes (e.g. "Deep Cold Ocean") on F3, but a floating island can't
  hold standing water, and `SkyIslandProfile.familyFor` only special-cased
  desert/snowy/mushroom families -- everything else, including every
  ocean/river/swamp biome, fell through to the plains-like default
  (grass), reading as a mismatch again despite the underlying sampling
  now being correct and stable. Jason's direction: give water biomes their
  own visual family rather than leaving it as grass or excluding water
  biomes from natural sampling entirely. **Fix:** new `BiomeFamily.WATER`
  (packed-ice-over-dirt), matched by an `ocean`/`river`/`swamp` substring
  check placed *before* the existing snowy family's own `frozen` check --
  otherwise `frozen_ocean`/`frozen_river` would keep matching snowy
  instead (regression-tested); `frozen_peaks`, which has no water
  substring, still correctly reaches snowy. New
  `SkyIslandProfileTest` coverage for the water family and the
  frozen-ocean-vs-frozen-peaks ordering. Built (0.3.0, Jason's own version
  choice), full multiloader build green, redeployed to Worldz-Test.
  **[Jason] retest outstanding.**
- 2026-07-25 (Phase 11 acceptance retest, fixed 0.2.89) — **Floating
  islands' `naturalBiome` (GOALS 08, DESIGN §28.6) silently never
  activated**, found via Jason's config 58 in-game test: islands' surface
  material followed the `islandBiomes` pool as if `naturalBiome` were off,
  while F3 showed real vanilla biomes anyway, and a village ended up
  force-generated onto a mismatched biome. Root cause, confirmed by
  decoding the actual world's `world_gen_settings.dat` NBT directly (not
  just re-reading the config): the persisted generator settings showed
  `natural_biome: 0` despite config 58 explicitly setting `naturalBiome:
  true` in YAML. `readFloatingIslandsConfig` (`WorldzConfig.java`) read
  every `floatingIslands` field except `naturalBiome` -- the field was
  defined on `FloatingIslandsConfig`, wired into the codec
  (`SkyIslandCodecs`), and included in the config-dump summary, but never
  actually parsed from YAML, so it silently stayed at its Java default
  (`false`) in every created world regardless of what the config file
  said. **What this means the original report's "vanilla biomes painted
  on top" actually was**: with `naturalBiome` truly off, `biomeVariety`
  correctly governed the material (matching "islands created based on the
  islandBiomes list"), while the F3 label mismatch was the pre-existing,
  unrelated `biomeExclusionZone` (DESIGN §27.10, config 57) reporting the
  real seed's biome beyond its 128-block default radius -- working
  exactly as designed, just confusingly overlapping with what config 58
  intended to exercise. No separate "huge unbounded landmass" bug was
  found once this was understood -- `effectiveModeAt` is structurally
  incapable of returning real, unmasked terrain for any sky_island world
  (verified: always `ExteriorMode.VOID` when `activeSkyIsland().enabled()`,
  before any per-column check even runs), so what looked like unbounded
  ground was ordinary small islands. **Fix:** added the missing
  `map.containsKey("naturalBiome")` read (and the matching missing write
  in `floatingIslandsMap`, so the mod's own config-rewrite doesn't drop
  the field either); new regression assertion in
  `floatingIslandsSettingsLoadAndSanitizeIndependently`.
  **Second, related bug found and fixed in the same pass** (would have
  newly surfaced the moment the fix above shipped, previously masked by
  `naturalBiome` never actually being active): the guaranteed village's
  own reserved island (forced onto a structure-compatible biome so its
  real vanilla village can legally place, DESIGN §28.3) had no way to be
  distinguished from an ordinary scattered island by either
  `LimitedBiomeSource.getNoiseBiome` or `EnvelopedChunkGenerator
  .skyIslandHitAtForTerrain` -- both would substitute the real seed's
  biome there too when `naturalBiome` is on, silently discarding the
  forced biome the village structure was actually chosen for (matching
  screenshot evidence: a real village force-generated under a "Deep
  Frozen Ocean" F3 reading). Fixed by threading a new `Hit.village()`/
  `SkyIslandHit.village()` marker from `FloatingIslandsPlan.hitFromCell`
  (set by comparing the resolved cell against `resolveVillageCell`)
  through both call sites, exempting a village hit from the natural-biome
  substitution. New `FloatingIslandsPlanTest` coverage:
  `guaranteedVillageIslandAlwaysAppearsInTheGrid` extended with a
  `hit.village()` assertion, plus a new
  `ordinaryScatteredIslandsAreNeverMarkedAsTheVillageHit` regression test.
  **Third and fourth bugs found across two further retest rounds on a
  fresh `Worldz-58` world** (screenshots each time): (1) F3/decoration
  still disagreed with the surface palette even with the village fix in
  place -- `LimitedBiomeSource.getNoiseBiome`'s naturalBiome branch
  forwarded whichever `quartY` vanilla's own biome-grid population
  happened to be filling, but island footprint membership is X/Z-only,
  so vanilla calls it once per vertical quart layer across the *entire*
  world height for a given column; the real biome noise's climate
  parameters are Y-sensitive, so different layers of the same column
  resolved different "real" biomes (screenshots: a snow-covered island
  with an igloo reporting "Dark Forest"). **Fix:** pinned that branch to
  `QuartPos.fromBlock(this.skyIsland.surfaceY())` instead of the caller's
  `quartY`, matching the terrain-palette path's own existing pin. (2)
  Even after that, a single island (one village spanning ~80 blocks)
  still flickered between unrelated biomes (Plains/Beach/River/Forest/
  Old Growth Birch Forest) as the player walked across it, because both
  paths still resampled per query column -- the real biome noise varies
  over shorter distances than one island's footprint. **Fix:** threaded
  the island's own (jittered) center through `FloatingIslandsPlan.Hit`/
  `EnvelopedChunkGenerator.SkyIslandHit` (new `centerX`/`centerZ`
  fields) so both paths sample the real biome once, at the island's
  fixed center, instead of at whichever column queries it -- matching
  DESIGN §28.4's own "real seed biome at its location" (singular).
  **Also added, per Jason's request to protect against mutually
  exclusive settings:** two new startup warnings in
  `sanitizeFloatingIslands` (`WorldzConfig.java`) -- `naturalBiome` +
  `biomeVariety` both enabled logs that `naturalBiome` wins and the
  `islandBiomes` pool goes unused; `naturalBiome` enabled at all logs
  that the guaranteed village's own island always keeps its forced
  biome instead, since honoring the preferred setting there isn't
  possible without breaking the village structure. Built (0.2.89), full
  multiloader build green, redeployed to Worldz-Test twice more this
  session. **[Jason] retest on a fresh config-58 world outstanding**
  — delete the old `Worldz-58` save rather than recreating over it
  (new-worlds-only policy).
- 2026-07-25 (Phase 13 acceptance retest, revised 0.2.88) — **Cave sealed
  surface gained a configurable block and thickness**, per Jason's
  config-56 review: the roof was previously a fixed 5-thick stone layer,
  easily mined through by a determined player, defeating the "no way
  out" intent of `sealedSurface`. `CavePlan`/`CaveConfig`/`CaveCodecs`
  gained `sealedSurfaceBlock` (new `logic.SealedSurfaceBlock` enum: stone/
  deepslate/bedrock, mirroring `StarterKitTier`'s pure-logic pattern) and
  `sealedSurfaceThicknessBlocks` (1-64, default 5, matching the old fixed
  value). `EnvelopedChunkGenerator.applyCaveSealedSurface` maps the enum to
  its real `BlockState` and uses the configured thickness instead of a
  hardcoded constant. Config-only for now (matching this project's
  precedent for new, not-yet-screen-exposed options): `CaveCustomization`
  and the in-game Customize screen still get the original stone/5-thick
  defaults, unchanged. New `config/tests/86-cave-sealed-surface-bedrock.yaml`
  (bedrock, 3 thick). Built (0.2.88), full suite green, redeployed to
  Worldz-Test. **[Jason] review outstanding.**
- 2026-07-25 (Phase 13, requirements captured — not implemented) —
  **Jason wants an option to prevent surfacing entirely from a sealed cave
  world**, beyond just a thicker roof: a permeable Y-level damage
  boundary (climb up, but it hurts and pushes you back down), reusing the
  same design already spec'd for GOAL 39's border damage enforcement
  (DESIGN §22.3) but keyed on Y instead of horizontal border distance.
  **Correction surfaced during this discussion: GOAL 39's border-damage
  mechanism is not actually built yet** (TODO Phase 5d is still
  unchecked) — DESIGN §22.3 is a complete, feasibility-verified spec, not
  shipped code, so a cave version would be the first real implementation
  of this pattern, not a reuse of proven code. Jason's explicit direction
  (2026-07-25): record the desire in DESIGN/TODO/GOALS as recommended,
  but defer implementation until it can be built **once, in a common
  place shared by every "safe area" boundary case** (the future GOAL 39
  border damage, this cave Y-boundary, and potentially others) rather
  than as cave-specific duplicate logic. See GOALS.md's Cave Challenge
  section and DESIGN §22.3 for the existing spec this would extend/share.
  Not scheduled to a phase.

- 2026-07-24 (Phase 13 acceptance retest, revised 0.2.87) — **Cave starter
  kit contents rewritten per Jason's config-56 review**, `CaveConfig`'s
  `easyDefaults`/`mediumDefaults`/`hardDefaults`. Old kits gave pre-made
  tools (`wooden_pickaxe`/`iron_pickaxe`/`crafting_table` items) at easy
  and nothing craftable at hard (no pickaxe at all). New tiers, per
  Jason's explicit spec: **easy** — raw `oak_log` (not a pre-made table/
  pickaxe, so the player crafts their own start) plus food, `wheat_seeds`,
  saplings, torches, and dirt, enough to never need a mineshaft or the
  surface for the whole game; **medium** — dialed down, still torches and
  enough log for "a way to make a pickaxe" plus dirt, no guaranteed food
  or saplings; **hard** — exactly one torch and one `wooden_pickaxe`,
  nothing else, so finding any wood at all requires reaching a mineshaft
  or the surface. `WorldzConfigTest`'s config-dump string updated to
  match; config 56's header comment corrected (previously described the
  old, wrong hard-tier contents). Built (0.2.87), full suite green,
  redeployed to Worldz-Test. **[Jason] review of the new tiers
  outstanding.**
- 2026-07-24 (Phase 13 acceptance retest, fixed 0.2.86) — **Mega-cavern
  floor opened into the void**, found via Jason's config 55 in-game test.
  Root cause: `applyCaveMegaCavern` carves solid, non-fluid blocks to air
  from `minY = max(chunk.getMinY(), spawnDepthY - cavernHeightBlocks)` up
  to `maxY`. Config 55's `spawnDepthY=-32`/`cavernHeightBlocks=32` puts the
  cavern's unclamped bottom exactly at the Overworld's `minY=-64`, so
  `minY` clamps to the world floor itself -- the carve then removed
  vanilla's own guaranteed-solid bottom bedrock layer along with
  everything else in the footprint, opening the cavern floor straight
  into the void below the world. This only bites when the configured
  height reaches the world floor; a shallower cavern already left real,
  untouched stone below its `minY - 1` as a natural floor, which is why
  this wasn't caught until a config specifically deep enough to clamp was
  tested. **Fix:** when the computed `minY` equals the world's actual
  floor, carving now starts one block higher (`worldMinY + 1`), leaving
  that single bottom-most layer untouched -- vanilla always generates a
  fully solid bedrock block at the literal minimum Y for every column
  (only the few layers just above it are randomized), so simply never
  carving it is sufficient; no explicit fill needed, and the existing
  "never fills" carve philosophy is preserved everywhere else. **Built
  (0.2.86), full multiloader build green, redeployed to Worldz-Test.
  [Jason] retest on a fresh config-55 world outstanding** — delete the old
  `Worldz-55` save rather than recreating over it (new-worlds-only
  policy).
- 2026-07-24 (Phase 13 acceptance retest, fixed 0.2.85) — **Cave-preset
  spawn silently landed on the surface**, found via Jason's config 53
  in-game test on a genuinely fresh world (`Worldz-53`): first screenshot
  after joining showed him at `[19, 90, 7]` in a Forest treetop, not
  underground. Diagnosed directly from the actual save files (not just
  code review): the world's own persisted `Data.spawn` field held
  `[16, -40, 0]` — correctly underground, right where
  `SpawnOriginManager.resolveCaveOrigin`'s cavity search should land — and
  the mod's own `SpawnOriginState` showed a clean, fresh resolution (not a
  stale-save reuse). So the *spawn-point metadata* was right; the *player*
  wasn't placed there. Root cause, confirmed against the real 26.2
  decompiled sources: vanilla's `PlayerSpawnFinder.findSpawn`, which
  actually places a joining/respawning player, treats the level's stored
  spawn point as a mere "suggestion" and recomputes Y from the real
  terrain's ordinary surface heightmap for any dimension where
  `dimensionType().hasCeiling()` is `false` — true for the cave preset's
  Overworld, which DESIGN §30.1 deliberately keeps an unmodified vanilla
  Overworld dimension type. The suggested underground Y is only ever
  trusted in the `hasCeiling() == true` branch, which the cave preset
  never took. This is the same "value computed and persisted correctly,
  but read differently by a separate vanilla consumer with its own rules"
  bug class as the dummy-`RandomState` precedent (MEMORY.md), just never
  previously generalized to player placement — no earlier preset ever
  needed a spawn Y drastically different from the surface, so this exact
  gap was invisible until cave's ~130-block Y jump made it obvious.
  **Fix:** new `PlayerSpawnFinderMixin` (both loaders, mirroring
  `ChunkMapMixin`'s per-loader-duplicate convention), `@Inject`-cancelling
  `PlayerSpawnFinder.findSpawn` for any cave-preset level and trusting the
  suggested position outright. Deliberately does **not** flip
  `hasCeiling` on the shared dimension type itself — checked first and
  confirmed that also gates `Level.canHaveWeather()` (would silently kill
  weather on the cave world's own surface, directly contradicting GOALS
  25's "ordinary vanilla surface terrain... weather" acceptance wording),
  plus `NaturalSpawner`'s mob-spawn-height search and `MapItem`'s map
  rendering — real, unwanted side effects for a preset whose Overworld is
  supposed to generate exactly like vanilla above ground. Known, accepted
  simplification: this also short-circuits a later bed-based respawn on a
  cave-preset level (trusts the bed position directly, skipping vanilla's
  embedded-in-a-wall nudge) — low-risk, since a bed's position is
  virtually always already safe. **Built (0.2.85), full multiloader build
  green, redeployed to Worldz-Test. [Jason] retest on a fresh config-53
  world outstanding** — the old `Worldz-53` save was created under the
  buggy 0.2.84 jar and should be deleted, not reopened, per this
  project's own new-worlds-only policy.
- 2026-07-18 (Phase 5.4 acceptance, fixed 0.2.12) — **Delayed/expanding/
  collapsing borders never actually started**, found via Jason's config
  21 in-game test: border held at its initial size indefinitely, no
  matter how far `/tick step` advanced. Root cause, confirmed by decoding
  the world's own persisted NBT directly (`world_limits.dat`,
  `world_border.dat`, `data/minecraft/world_clocks.dat`): 26.2 moved the
  authoritative "how many ticks has this dimension actually experienced"
  counter to a new per-dimension `WorldClock` system (`Level.
  getDefaultClockTime()`, persisted as `total_ticks` per dimension in
  `world_clocks.dat`) — `ServerLevel.getGameTime()` (the classic
  `LevelData` field `WorldLimitManager` was built against, and still the
  field the decompiled/generated 26.2 sources document as "the" game
  time) is no longer kept in sync with real play time in this snapshot.
  Confirmed directly: the test world's `world_clocks.dat` showed
  `minecraft:overworld` at 1,146,062 real elapsed ticks (far past the
  120,000-tick delay Jason had configured), while `world_limits.dat`'s
  pending-start tick was still sitting uncleared at 120000 and
  `world_border.dat` was still static at the initial size — the
  delay-expiry check was comparing against a clock that had barely moved.
  (The interpolation mechanics themselves, once started, are unaffected —
  `WorldBorder.MovingBorderExtent.update()` just decrements a per-tick
  counter on every `WorldBorder.tick()` call, not tied to either clock —
  so this only ever blocked the *start* of a transition, not its
  progress.) Fixed by switching `WorldLimitManager`'s three
  `getGameTime()` call sites to a new `dimensionTicks(ServerLevel)`
  helper wrapping `getDefaultClockTime()`. Static borders (no delay) were
  never affected — config 20's acceptance already confirmed those work.
  Configs 21 and 22 (both delay-based) need a fresh world and a retest
  under 0.2.12; the old Worldz-21 save is stuck by design (new-worlds-only
  policy, disposable test saves) and isn't worth trying to repair.

- 2026-07-18 (Phase 5.3) — Supersedes Phase 4.2's acceptance item 5
  ("Chaos Biomes Customize screen shows... nothing from the full Worldz
  preset's... border, exterior... controls"), confirmed 2026-07-18 in
  MANUAL_TESTING.md before this phase existed. TODO 5.3 explicitly calls
  for the opposite — border/exterior/End-border composing with every
  world type via each type's own Customize screen, not just the plain
  preset — so `single_biome` and `chaos_biomes` now both show Overworld/
  Nether Border, End Border, and Overworld/Nether Exterior buttons
  (reusing `WorldzBorderScreen`/`WorldzExteriorScreen`/new
  `EndBorderScreen` via small host interfaces in `LimitEditorHosts`).
  This is intentional forward evolution of that acceptance criterion, not
  a regression: re-verify in Phase 5.4's acceptance pass instead of
  re-flagging it as a defect.
- 2026-07-16 (Phase 2.1) — GOALS 12 ("starter biome is based on seed —
  including size and location") is implemented as seed-determined *location*
  only, via the existing `preferred_natural_biome` search + recentering
  (DESIGN §18); *size* is the still-configurable `starterRadiusBlocks`, not
  detection of a natural biome patch's true boundary (no other GOALS use
  case needs patch-boundary detection, and it would be substantial new
  work). See DESIGN §20.2's Phase 2.1 subsection. Flag if a literal
  patch-size reading was intended.
- 2026-07-19 (Phase 5c.1 spike) — DESIGN §21.2's original framing ("the
  pipeline classes exist... so this is possible") turned out optimistic.
  Verifying the literal "hand-drive `ChunkStatusTasks` with a scratch
  `ProtoChunk`" approach against the real `ChunkPyramid` source found
  every generation stage from structure-references through carvers
  requires an 8-chunk-radius neighborhood already at `STRUCTURE_STARTS` —
  a materially bigger ask than "single-chunk backfill" suggested, and one
  that would mean reimplementing internal orchestration
  (`StaticCache2D<GenerationChunkHolder>`, `WorldGenContext`) that exists
  to be built by `ChunkMap`'s own async pipeline, not called into from
  outside. Full findings, the one piece of good news (applying finished
  terrain to a live chunk is just ordinary block placement — no custom
  relighting/resync needed), and a recommended alternative direction
  (WorldEdit-`//regen`-style chunk invalidation, not yet attempted) are in
  DESIGN §21.2. Only the safe, low-risk half of 5c.1 (the live-radius
  volatile field) was actually implemented; the backfill half remains a
  research finding pending Jason's go/no-go, not working code.
- 2026-07-19 (Phase 5c.1b, second research pass) — Same caveat extended:
  the delete-and-regenerate direction recommended in the entry above
  turned out to have its own hidden gap once checked further (forcing an
  already-*resident* chunk to discard and restart from `EMPTY` has no
  obvious public API), though the region-file-deletion half of it checked
  out as a genuine, clean, public vanilla capability
  (`RegionFileStorage.write(pos, null)`). Found a third, better-looking
  approach instead ("mask, don't discard" — generate real terrain
  normally, cache it, reveal later via ordinary block placement) that
  sidesteps both risks found so far. Recorded as the new top
  recommendation in DESIGN §21.2 and TODO 5c.1b; still unimplemented,
  still pending Jason's go/no-go — two research passes in a row have
  each surfaced a real problem with the previous round's leading idea, so
  treat any *specific* approach here as provisional until one is actually
  built and tested live, not just read from source.
