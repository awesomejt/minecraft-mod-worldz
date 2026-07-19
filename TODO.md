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

- [ ] 1.1 [Jason] Verify the 0.1.15 dummy-RandomState mixin fix in-game on
      Fabric: bottom-of-world has bedrock, normal cave systems, and no
      near-total lava sheet below Y-64. Briefly repeat on NeoForge (first
      NeoForge mixin this project ships). Also retest whether the Worldz14
      orange/glitchy-terrain screenshots reproduce on 0.1.15 — plausibly the
      same dummy-RandomState root cause, never confirmed (MEMORY Known Risks).
      **Partial (2026-07-17):** Fabric bottom-of-world check done on
      `Worldz-06` (single_biome desert) — bedrock and terrain below Y-64
      look normal, no lava sheet. NeoForge repeat and the Worldz14
      reproduction retest are still outstanding; see MEMORY.md for how this
      interacts with the still-open floating-terrain finding.
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
- [ ] 1.7 [Jason] Phase acceptance: a default-config world and a
      single-biome-style world both create, generate normal caves/structures,
      and show nothing from the removed modes in Customize.

## Phase 2 — World types + Single-biome challenge (GOALS 10–12)

Goal: the first challenge type end-to-end, plus the per-type architecture
every later phase reuses. Design task first, per DESIGN §20.6.

- [ ] 2.1 Design pass (extend DESIGN §20): finalize the world-type preset
      list and IDs (e.g. `jlt_worldz:single_biome`, `jlt_worldz:limited`
      replacing the catch-all `jlt_worldz:worldz`), the per-type YAML config
      sections with per-type defaults, which shared modules (limits, spawn,
      starter chest, exclusion zone) each type composes, and the per-world
      snapshot file (2.4). Verify 26.2 preset/tag/lang wiring for multiple
      presets. **Commit** the design before implementing.
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
      21 blocked on 5.5 (radius floor), configs 21/23 + the two UI checks
      still outstanding.
- [ ] 5.5 Lower the border radius floor (found in config 21 acceptance:
      `initialRadiusBlocks: 4` rendered as a 64-block border). Jason's
      2026-07-18 decision: **global floor of 1** — set
      `WorldzConfig.MIN_BORDER_RADIUS_BLOCKS = 64 → 1` so a tiny start
      (1–2 blocks), a collapse-to-almost-nothing final radius, and a
      low End minimum are all allowed; beatability becomes the user's
      responsibility (the End carry-over override and progression
      guarantees still function). Nothing structural needs 64 —
      `BorderSchedule`'s compact constructor already only requires radii
      `> 0`, and vanilla `WorldBorder.setSize` accepts small diameters; the
      floor is purely the config sanitizer's (`sanitizeBorder`/
      `sanitizeEndBorder`, three clamp sites). Update
      `MIN_BORDER_RADIUS_BLOCKS`'s javadoc, the README's documented minimum,
      any test asserting the 64 floor, and version-bump; then Jason
      re-tests config 21 with a 1–2 block start. (Independent of the
      Phase 5b/5c/5d additions.)

## Phase 5b — Stepped border resizing (GOALS 19–20 clarification)

Scope added from Jason's 2026-07-18 Phase 5 review: keep the shipped
continuous style, add an abrupt stepped style (jump X blocks every Y days).
Decisions already made (see DESIGN §21.1 and MEMORY.md — resizeStyle field
reusing the rate fields, instant snap per step, easing curve deferred).
Design is settled; these tasks are execution.

- [ ] 5b.1 `resizeStyle: continuous | stepped` through the stack: schedule
      math first (`BorderSchedule.radiusAtTick` stepped curve — pure,
      JUnit-covered, including delay interaction, clamping at final, and
      collapse direction), then config/codec/customization plumbing
      (`BorderConfig`, `WorldLimitCodecs`, `WorldzCustomization` +
      single-biome/chaos variants, border screen toggle), default
      `continuous` so existing configs/saves are untouched.
- [ ] 5b.2 Step driver in `WorldLimitManager.onServerTick`: apply due steps
      via `WorldBorder.setSize` (instant snap), persist next-step tick in
      `WorldLimitState`, recompute-on-restart semantics (radius is a pure
      function of the per-dimension clock — use `getDefaultClockTime`, see
      the Deviation log; never `getGameTime`). Missed steps while the server
      was closed apply on the next tick.
- [ ] 5b.3 Test configs mirroring Jason's two scenarios (8-radius start,
      +1 block/day to 1024; 1024 start, 10-day delay, −2 blocks/day to 32);
      config/tests README + MANUAL_TESTING rows with `/tick step` math;
      README docs; **[Jason]** acceptance.

## Phase 5c — Soft void border spike (GOAL 38)

The void as the visible edge of an expanding/collapsing world — no wall, the
player can fall off. Feasible but expansion needs chunk-regeneration
backfill, the heaviest machinery proposed so far (full findings + decided
overwrite rule in DESIGN §21.2). Spike first; implementation is **not**
scheduled until the spike proves out — 5c.2 is written assuming success but
must be re-planned from the spike's findings before execution.

- [ ] 5c.1 Spike (throwaway branch OK): make `EnvelopedChunkGenerator` read
      a live radius (volatile snapshot; worldgen threads — no SavedData
      reads from the generator), then prove single-chunk backfill: run the
      delegate generator's stages into a scratch `ProtoChunk` for an
      already-generated void chunk, copy sections into the live chunk,
      rebuild heightmaps/lighting, resync the client. Report findings
      (structure/decoration neighborhoods, lighting, perf per chunk) and
      re-plan 5c.2 from them. **[Jason]** go/no-go on the results.
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

- [ ] 6.1 Design spike (DESIGN §20.11): vanilla `WorldBorder` is
      square-only (verify in 26.2 sources), so the strip's long walls likely
      come from the exterior-envelope mechanism (void or solid wall) rather
      than the border — decide the access-prevention approach and its
      interaction with the 17–20 schedules. Stronghold/End-portal
      reachability inside the strip via the existing progression guarantees
      (the fallback-portal machinery applies). Optional Nether strip (32).
- [ ] 6.2 Implement (as a `limited` option or its own type — 6.1 decides);
      configurable width in blocks/chunks; test configs; docs; **[Jason]**
      acceptance.
- [ ] 6.3 Biome-sequence strip (36): the strip passes through ordered (or
      seed-randomized) biome bands, changing every N chunks, selecting
      biomes over untouched vanilla terrain — Phase 4's selection machinery
      with ordered bands instead of random cells. Config: band width, biome
      list/order, seed-random option. Test config; **[Jason]** acceptance.

## Phase 7 — Ocean island challenge, core (GOALS 01, 04)

- [ ] 7.1 Design pass (DESIGN §20 extension): natural-looking island shaping
      (noise-perturbed radius over the existing starter-land profile — not a
      disc), a dedicated narrow shore width (beach/stony-shore ring; fixes
      the logged beach-width gap properly), shallow→deep ocean depth gradient
      with depth-appropriate ocean biomes (all ocean biomes available), and
      the shared **exclusion zone** module (center = origin, default 2000
      blocks; reused by 04, 07, 08, 24). **Commit** design first.
- [ ] 7.2 Implement the `ocean_island` world type: configurable island size
      (1 chunk → huge), chosen island biome, endless ocean via the terrain
      cap, underground structures intact, unchanged Nether/End, beatable
      (progression guarantees). Use case 01.
- [ ] 7.3 Distant natural islands (04): release the ocean cap beyond the
      exclusion radius so the seed's natural terrain resumes far away.
- [ ] 7.4 Test configs (tiny/default/huge island, 04 variant); docs;
      **[Jason]** acceptance including "does the island read as natural".

## Phase 8 — Ocean island extras (GOALS 03, 02)

- [ ] 8.1 Starter-chest infrastructure (shared with the sky, cave, Nether,
      and End phases): loot presets + YAML-configurable contents, placed at
      spawn. Then use case 03: no-land ocean world, spawn on/next to a chest
      boat with essentials (lily pad, dirt, grass block, saplings) +
      configurable randoms.
- [ ] 8.2 Natural island by seed (02): search the real seed's unmodified
      climate/terrain for a small natural island, set world spawn/origin
      there, replace everything else with ocean beyond it. Reuses the 16.3
      spawn-search + recentering machinery. This is the hardest ocean item —
      keep it last and time-boxed; if the search proves unreliable, park it
      with findings in DESIGN and move on.
- [ ] 8.3 Test configs; docs; **[Jason]** acceptance.

## Phase 9 — Ocean fluid variants: lava ocean + dry world (GOALS 28, 31)

Right after the ocean phases: the ocean-island shape with the fluid swapped
(lava) or removed (dry), so the infrastructure is fresh.

- [ ] 9.1 Design pass (DESIGN §20.10/§20.11): parameterize the ocean
      exterior/cap fluid — water / lava / none. For lava (28): verify 26.2
      surface-lava-at-scale behavior (light, fire spread at the shore ring,
      fluid ticking, map color) and shore safety so the transition ring
      can't ignite spawn. For dry (31): water-scarcity semantics —
      default keeps water where structures/features naturally place it
      (village farms and wells, strongholds, aquifer pockets, springs);
      harder options remove more (rivers, surface lakes). Beatability:
      potions and water-dependent progression obtainable at every offered
      difficulty.
- [ ] 9.2 Implement lava ocean (as an `ocean_island` fluid option or its own
      type — 9.1 decides); test configs; **[Jason]** acceptance including
      strider/bridging travel viability.
- [ ] 9.3 Implement dry world with the water-findability difficulty option;
      test configs; **[Jason]** acceptance.

## Phase 10 — Sky island challenge (GOALS 05–07)

- [ ] 10.1 Design pass: true floating island (bounded below — not the
      current full-depth terrain plug), default spawn platform Y ≥ 64 (slime
      rule), necessities-chest presets easy/medium/hard informed by island
      biome (water bucket vs cauldron etc.), all beatable;
      stronghold/structure option reusing progression guarantees (05).
      Nether/End sky-island options with structure retention (06). Villages
      beyond the exclusion zone (07).
- [ ] 10.2 Implement `sky_island` world type per the design; test configs;
      docs; **[Jason]** acceptance per sub-case.

## Phase 11 — Floating resource islands (GOALS 08)

- [ ] 11.1 Seed-driven scattered floating islands with varied
      sizes/resources, far enough apart to force serious bridging; option
      replaces pure void between them. Design first (placement sampling can
      reuse the pure hash-cell approach — uniform role, so no
      coastline-class defects), then implement, test configs, **[Jason]**
      acceptance.

## Phase 12 — Sky chunk challenge (GOALS 09, 37)

- [ ] 12.1 Design + implement chunk-grid islands cut from the seed's natural
      chunks: full-chunk vs top-N-blocks-deep option, guarantee one generated
      chunk contains a portal room, normal-vs-chunk-island Nether/End
      options. Test configs; **[Jason]** acceptance.
- [ ] 12.2 Multi-biome chunk islands (37): beyond the starter island,
      further chunk islands of different biomes; per-island top-only vs
      full-column choice; where feasible, islands showcasing underground
      content — cave biomes (lush/dripstone/deep dark), amethyst geodes,
      structure-bearing chunks — so bridging yields varied resources.
      Feasibility of targeting specific underground features per island is
      part of the design task (may require seed-search or forced placement —
      decide there). Test configs; **[Jason]** acceptance.

## Phase 13 — Cave challenge (GOALS 25–26)

- [ ] 13.1 Design pass (DESIGN §20.10): underground spawn placement
      (configurable depth, safe cavity search — can reuse the spawn-search
      ring pattern from §18), optional sealed surface (solid roof / no sky
      access — decide generation approach and its interaction with
      heightmaps, mob spawning, and phantom rules), and the mega-cave option
      (huge natural-looking cavern around spawn, edges blended into natural
      cave systems — decide carver vs. feature vs. noise approach against
      real 26.2 sources). Beatability: stronghold and underground structures
      unchanged, portal built underground works.
- [ ] 13.2 Implement `cave` world type; starter-chest reuse; test configs;
      docs; **[Jason]** acceptance (25 with/without sealed surface, 26).

## Phase 14 — Nether-start challenge (GOALS 27)

- [ ] 14.1 Feasibility spike first (the §16.1 pattern): how initial spawn in
      a non-Overworld dimension actually works in 26.2 — `MinecraftServer`/
      `PlayerList` spawn+respawn paths, respawn-anchor semantics, what
      happens on death without an anchor. Findings cover the End too (for
      Phase 15). Verify against real sources; commit findings to DESIGN
      §20.10 before implementing.
- [ ] 14.2 Implement `nether_start` world type: safe Nether spawn site,
      starter-chest difficulty tiers (easy = portal escape kit; every tier
      beatable), Overworld normal. Test configs; docs; **[Jason]**
      acceptance including death/respawn behavior.

## Phase 15 — End-start challenge (GOALS 34)

- [ ] 15.1 Design pass building on 14.1's spike: spawn on the outer End
      islands, starter chest tuned so survival through to defeating the
      Ender Dragon is genuinely achievable, hardcore-beatable even if very
      hard. Respawn semantics are the hard part (beds explode in the End, no
      anchors) — decide and document before implementing.
- [ ] 15.2 Implement `end_start` world type; test configs; docs; **[Jason]**
      acceptance (ideally including a hardcore run's early game).

## Phase 16 — Flat worlds (GOALS 15, 16, 22)

- [ ] 16.1 Design pass against DESIGN §19's verified `FlatLevelSource`
      research: layer editor (arbitrary block layers/thicknesses, presets,
      text import/export), optional bedrock floor, structure toggles incl.
      the trial-chambers placement spike, spawn-Y/slime option (15);
      deep-flat variant with seeded caves/cave biomes/optional far-off rivers
      (16 — likely noise-based underground below a flat surface, spike
      needed); underground structures buried at natural depth rather than
      floating (22).
- [ ] 16.2 Implement `flat` world type(s) per design; test configs; docs;
      **[Jason]** acceptance.

## Phase 17 — Stacked biome layers (GOALS 35)

The biggest new generation concept — deliberately after flat (16), whose
layer-editor concepts it likely reuses, and after limits (5), which it
composes with.

- [ ] 17.1 Design spike (interpretation confirmed by Jason 2026-07-16:
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
- [ ] 17.2 Implement the `stacked` world type per design; test configs;
      docs; **[Jason]** acceptance.

## Phase 18 — World-hazard rules module (GOALS 29–30)

A shared runtime module (server-tick + saved-data, like delayed borders),
composable with any world type — **independent of Phases 5–17 and can be
pulled earlier if Jason wants a fun quick win.**

- [ ] 18.1 Forever night (30): start-at-night option and night-permanent-
      after-N-days trigger; once active, time is held at night and sleeping
      cannot skip it; insomnia/phantom option (keep vanilla or relax).
      Reuses the day/delay schedule idiom from borders (§12/§15).
- [ ] 18.2 Rising lava floor (29): persisted world-wide lava level with
      delay/rate/max (border-schedule config idiom); design the block-
      conversion rules (air/water below the level) and the application
      strategy for loaded vs. newly loaded chunks with acceptable
      performance — design task first, verified against 26.2 chunk/tick
      APIs.
- [ ] 18.3 Test configs (night-from-day-0, night-after-N, rising lava on a
      vanilla-limited world and on an ocean island); docs; **[Jason]**
      acceptance.

## Phase 19 — Structure options wrap-up (GOALS 21, 23, 24)

- [ ] 19.1 Verify natural placement remains the default everywhere (21).
- [ ] 19.2 Generalize the exclusion-zone module into per-structure-family
      "minimum distance from spawn" options (default 2000 blocks) usable by
      any world type (24).
- [ ] 19.3 Stretch, only if Jason still wants it after 1–18: floating
      "Pandora" structure islands (23). Design spike first; park if cost is
      out of proportion.
- [ ] 19.4 Test configs; docs; **[Jason]** acceptance.

## Phase 20 — Wrap-up and release

- [ ] 20.1 Full README/config-reference/example rewrite in challenge-first
      terms; MANUAL_TESTING.md final scenario tables; MEMORY.md tidy.
- [ ] 20.2 Final clean multiloader build, artifact inspection, version bump.
      Publishing decisions remain Jason's.
- [ ] 20.3 Revisit any newly suggested challenge ideas with Jason — plan
      approved ones as new phases.

---

## Backlog (approved, not yet scheduled to a phase)

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

## Deviation log

(Record every departure from DESIGN.md/GOALS.md here: what, where, why.)

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
