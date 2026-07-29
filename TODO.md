# TODO — jlt_worldz challenge-world plan

**Requirements source:** `GOALS.md` (Jason's use cases 01–37). **Technical
reference:** `DESIGN.md` — §20 is the architecture for this plan; §§1–19
document the already-built components and verified 26.2 APIs. **History:**
`TODO-archive.md` (the completed 2026-07-14/15 feature-first plan);
`TODO-done.md` (full checklists for this plan's completed phases — this file
keeps only a one-line stub per finished phase to stay a manageable size).

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

## Phase 1 — Stabilize and simplify (release 0.2.0) — done, moved to `TODO-done.md`
## Phase 2 — World types + Single-biome challenge (GOALS 10–12) — done, moved to `TODO-done.md`
## Phase 3 — Single-biome variations: natural rivers and oceans (GOALS 13–14) — done, moved to `TODO-done.md`
## Phase 4 — Chaos biomes (GOALS 33) — done, moved to `TODO-done.md`
## Phase 5 — World limits, expanding/collapsing (GOALS 17–20) — done, moved to `TODO-done.md`
## Phase 5b — Stepped border resizing (GOALS 19–20 clarification) — done, moved to `TODO-done.md`
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

## Phase 6 — Strip world, 1D Minecraft (GOALS 32) — done, moved to `TODO-done.md`
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
- [x] 7.4 Test configs (tiny/default/huge island, 04 variant); docs;
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

## Phase 8 — Ocean island extras (GOALS 03, 02) — done, moved to `TODO-done.md`
## Phase 9 — Ocean fluid variants: lava ocean + dry world (GOALS 28, 31) — done, moved to `TODO-done.md`
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

## Phase 11 — Floating resource islands (GOALS 07–08) — done, moved to `TODO-done.md`
## Phase 12 — Sky chunk challenge (GOALS 09, 37) — done, moved to `TODO-done.md`
## Phase 13 — Cave challenge (GOALS 25–26) — done, moved to `TODO-done.md`
## Phase 14 — Nether-start challenge (GOALS 27) — done, moved to `TODO-done.md`
## Phase 14b — Universal starter capsule, Nether-start first pass (GOALS 41) — done, moved to `TODO-done.md`
## Phase 15 — End-start challenge (GOALS 34) — done, moved to `TODO-done.md`
## Phase 16 — Flat worlds (GOALS 15, 16, 22) — done, moved to `TODO-done.md`
## Phase 17 — Stacked biome layers (GOALS 35) — done, moved to `TODO-done.md`
## Phase 18 — World-hazard rules module (GOALS 29–30) — done, moved to `TODO-done.md`
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

## Phase 21 — Surface vs. underground biomes (GOAL 42) — done, moved to `TODO-done.md`
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
- [ ] 24.5 Consolidate the duplicated `NetherStartPlan`/`EndStartPlan` capsule
      fields (DESIGN §32 recorded them as "duplicated rather than shared per
      this goal's own 'true cross-preset sharing later' precedent" — this is
      that "later"). **Un-folded back here from TODO 25.8 (2026-07-28, Jason
      confirmed)** — `CONFIG-RESTRUCTURE.md` §7 assumed this overlapped 25.8's
      named-kits work and folded it in, but DESIGN §44.1 found that overlap
      doesn't exist: the *config*-layer duplication this item names was
      already fixed by 25.2a (shared `StarterCapsuleSchema`) and 25.6e (its
      `light` nest) — there is nothing left in the config layer to
      consolidate. What remains is entirely in `logic/`: four flat record
      components, nine `MIN`/`MAX`/`DEFAULT_CAPSULE_*` constants, four
      validation blocks and `centeredCapsuleOffsets(int, int)`, duplicated
      byte-for-byte between `NetherStartPlan.java` and `EndStartPlan.java`
      (full inventory in DESIGN §44.1.2). These records are codec-persisted
      (`NetherStartCodecs`/`EndStartCodecs` → `EnvelopedChunkGenerator`'s own
      codec) but DESIGN §44.1.3 verified a shared `StarterCapsulePlan`
      component forces **no** persisted-shape change — `RecordCodecBuilder`
      binds `fieldOf` to arbitrary getters, so the four flat field names
      (`capsule_size`/`capsule_height`/`capsule_light_source`/
      `capsule_light_spacing`) stay exactly as they serialize today. Pair
      this with 24.4 (its real sibling — a shared `StarterCapsulePlan` is the
      parameter object 24.4's shared capsule builder would take; doing them
      apart means 24.4 re-opens the same files 24.5 just touched) rather than
      with 25.8, which shares zero files with this item.
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
- [x] 25.2e Island sections (needs 25.2a). `OceanIslandSchema`,
      `SkyIslandSchema`, `FloatingIslandsSchema` (dependent `maxRadiusBlocks`
      bound, two advisory warnings — R4, overridden `summary`),
      `ChunkIslandSchema` (conditional summary segment). Proves the shared
      `exclusionZoneEnabled`/`exclusionZoneRadiusBlocks` pair 25.6 collapses.
- [x] 25.2f Chest/kit presets (needs 25.2a). `CaveSchema` (DESIGN §41.6's
      worked example), `NetherStartSchema`, `EndStartSchema`. Proves the
      shared `chestTier` + three-kit shape that D6/25.8 collapses into named
      kits.
- [x] 25.2g The root (needs 25.2a-25.2f). `WorldzRootSchema`: the eight
      top-level scalars (`allowedBiomes`, `starterBiome` with its own warning
      wording — R6, `starterRadiusBlocks`, `ensureStarterLand`,
      `starterLandTransitionBlocks`, `starterLandFoundationDepthBlocks`,
      `allowRivers`, `allowOceans`) plus the 25 `Setting.section(...)`
      bindings in today's exact `toYaml()` order. At this point every section
      is schema-driven and the differential test is at full strength.
- [x] 25.2h Retire the legacy path (needs 25.2g). Delete `LegacySections`, the
      ~100 `read*`/`sanitize*`/`*Map`/`*Summary` methods and
      `ConfigSchemaDifferentialTest`. `WorldzConfig.java` should land at ~200
      lines (from 2400). Keep the golden `reference-defaults.yaml` test and add
      `ConfigSchemaMetadataTest` (every setting has doc + unit + applicability;
      the schema's flattened key list exactly equals `toYaml()`'s — the
      completeness gate F6 wanted and 25.10 reuses).
- [x] 25.3 Presence tracking (D5, needs 25.2) — **done**. `ParseContext` gained
      a `public ParseContext(Logger, Consumer<String>)` constructor recording
      every `markPresent` call (the one-arg constructor keeps discarding, for
      callers that don't care). `WorldzConfig.parse` now wires a
      `LinkedHashSet<String>` into it and stores the result as
      `presentKeys`; `WorldzConfig.present(String dottedPath)` exposes it. No
      schema change was needed — the dotted paths `SchemaSection.readOne` was
      already building were already correct at every nesting depth. Verified
      `presentKeys` survives `.sanitize()`: the root schema has no
      `sanitize`/`postValidate` override, so `SchemaSection.sanitize` mutates
      the same `WorldzConfig` instance in place rather than replacing it.
      `ConfigPresenceTest` covers defaults-only (nothing present), an empty
      `{}` parse (nothing present), a value explicitly set to its own default
      (present anyway — proves this is presence, not value-diffing), nested
      leaves/containers (`stacked.worldSizeChunks`, `cave.easyKit.essentials`)
      and survival across `parse(...).sanitize(...)`. `./gradlew build` green.
      No `config/tests/*.yaml` added — zero in-game-observable effect (no
      schema/key/gameplay change), same as 25.1/25.2. **Not yet consumed
      anywhere** — 25.5 (sentinel retirement) is what will call
      `config.present(...)` for real; this task only proves the capability.
- [x] 25.4 Stop rewriting the config file (D4, needs 25.3 — see F5: today's
      rewrite makes every setting explicit after one launch, which would
      silently defeat 25.3). Load becomes parse-validate-log. Emit
      `jlt_worldz.reference.yaml` from the same schema that drives parsing, so
      it cannot drift from the code. — **done**. `WorldzConfig.load` no
      longer calls `save`; `loadExisting` is now parse-validate-sanitize only,
      so `config/jlt_worldz.yaml` is never rewritten regardless of whether it
      parsed cleanly — comments and omitted settings now survive every
      launch. `load` always regenerates `config/<modId>.reference.yaml` via
      the new `writeReference`/`referenceYaml`: a fixed comment header plus
      plain `new WorldzConfig().sanitize().toYaml()` output (no per-key doc
      comments — that's 25.10's job once its bespoke renderer exists), never
      throwing on write failure so a broken reference file can't block the
      user's own config from loading. The old `save` method and the
      now-unused `StandardCopyOption` import were deleted. 4 existing tests
      reworked (`unknownKeysAreTolerated`,
      `nonStringAndSyntacticallyInvalidBiomeEntriesAreDropped`, and the
      renamed `missingConfigUsesDefaultsWithoutCreatingAUserConfigFile`/
      `malformedConfigUsesDefaultsWithoutOverwritingInput`) plus 5 new ones
      covering the reference file and the headline comment-preservation
      regression. No `config/tests/*.yaml` added — zero in-game-observable
      schema/gameplay change, same precedent as 25.1-25.3.
- [x] 25.5 Retire every sentinel (D5, needs 25.3) — **partially done, see
      Deviation log for the rest.** `StackedConfig.effectiveOverworldExterior`
      now takes a second `boolean sharedOverworldConfigured` parameter
      (`config.present("overworldBorder") || config.present("overworldExterior")`
      at every call site: `StackedCustomization.fromConfig`,
      `LimitedBiomeSource.resolve`, `EnvelopedChunkGenerator.resolve`) instead
      of gating purely on `worldSizeChunks != 0`. **Verified against real
      fixtures that gating on `worldSizeChunks`'s own presence (the literal
      reading of "did the user set it?") would have broken config 72's own
      acceptance test** — 72 relies on the bounded void wall deriving from
      `worldSizeChunks`'s nonzero *default* while never mentioning
      `worldSizeChunks` at all, and TODO.md's own "behavior-preserving except
      25.9" constraint forbids changing that. The actual bug (F4, TODO
      17.4a/17.6) was that *any* nonzero `worldSizeChunks` — including the
      untouched default — silently discarded an already-explicit
      `overworldBorder`/`overworldExterior`; the fix gates on presence of
      those sibling sections instead, which reconciles both constraints
      (config 72/99/101 unchanged, configs 73-76 can drop their opt-out).
      Literal `worldSizeChunks: 0` still opts out unconditionally, regardless
      of presence (StackedSchema's own "zero opts out" doc, and because a
      literal zero-radius void wall would crash `ExteriorPlan
      .DimensionEnvelope`'s own compact constructor if honored any other
      way). `effectiveOverworldBorder` needed no change — confirmed it has
      been a pure pass-through since DESIGN §34.10, no sentinel left. New/
      updated tests: `StackedConfigTest` (2 new presence-boolean cases + 2
      renamed existing ones + 2 new end-to-end `WorldzConfig.parse` cases
      proving the wiring), `ProjectMetadataTest`'s 2 source-string assertions
      updated for the new call-site shape. **Verified the actual boilerplate
      count myself (task explicitly said not to trust "+2 others"): only 4
      configs (73/74/75/76) carry a *live* `worldSizeChunks: 0` key, not 6 —
      99/101 only mention it in prose comments.** All 4 cleaned up (73/74:
      comment reworded, key deleted; 75/76: intro prose updated to describe
      the new mechanism, key deleted). `./gradlew :common:test` green.
- [x] 25.6 Restructure the keys (D7) per CONFIG-RESTRUCTURE.md F1's two tables,
      as corrected by DESIGN §42.1 — 13 within-class nests (strip deferred to
      25.9), 3 live cross-class shapes (`exclusionZone`, `naturalBiomes`,
      `chest`) plus 3 shapes F1 missed (`starter`, `starter.land`,
      `deepFlat.rivers`), with `Blocks` suffixes dropped per the §2 naming
      rule. Design and full old→new key map in **DESIGN §42**. Broken into
      25.6a-25.6h below, each independently buildable/testable/committable.
      **No POJO, `logic/` or `client/` changes** — nesting is done with
      `Setting.group` over the parent's own type (§42.2). **Done** — all
      8 sub-steps (a-h) complete; see 25.6h for the final close-out note.
- [x] 25.6a Framework + the fixture gate — **done**. Added `Setting.group(String
      key, SchemaSection<S> group)` (`Setting.java`) and
      `SchemaSection.copyInto(S from, S to)` (`SchemaSection.java`), exactly
      per DESIGN §42.2's shapes, with one required deviation from its literal
      sketch: the accessor's setter is the lambda `(owner, value) ->
      group.copyInto(value, owner)`, **not** the bare `group::copyInto` method
      reference the design sketch showed. `Accessor.set(owner, value)` calls
      `setter.accept(owner, value)`, so a direct `group::copyInto` bound
      reference would call `copyInto(owner, value)` — copying the *real*
      target's (still-default) fields onto the freshly-parsed throwaway
      instead of the other way around. Caught by a new `SettingGroupTest`
      (synthetic two-field POJO exercising read/sanitize/toMap through the
      mechanism in isolation, since no real `*Schema.java` uses it yet — 25.6b
      is first); verified the failure mode directly by temporarily reverting
      to the bare method reference and confirming
      `readCopiesTheGroupsFieldsOntoTheRealTargetNotTheReverse` goes red, then
      restoring the fix. Sanitize-time self-assign is unaffected either way
      (owner and value are the same reference there, per the identity getter).
      Promoted `ConfigSchemaMetadataTest`'s private `collectKeys`/duplicate
      logic into a new shared `SchemaKeyWalker` (package-private, `common/src/
      test`), which now has two methods: `collectKeys` (moved verbatim, used
      by `ConfigSchemaMetadataTest` as before) and the new `findUnknownKeys`
      (walks a raw YAML map against the schema tree in lockstep, recursing
      into a nested section only when the schema itself declares a
      `Rule.Nested` setting for that key *and* the file's value there is a
      map — never based on the raw shape alone, since `layout.roleOverrides`
      is a leaf `stringMap` setting with arbitrary user-defined sub-keys that
      must not be mistaken for section keys). New `ConfigFixturesTest` reads
      every `config/tests/*.yaml`, asserts it parses (`WorldzRootSchema.read`)
      and sanitizes (`.sanitize`) without throwing, asserts the file count,
      and asserts the exact unknown-key set per file via `findUnknownKeys` —
      empty for every fixture except three, hardcoded in a
      `KNOWN_UNKNOWN_KEYS` map keyed by filename with the exact expected keys
      (not just "some failure"), each pointing at the TODO item that resolves
      it:
      - `57-sky-island-biome-exclusion-zone.yaml` →
        `skyIsland.exclusionZoneEnabled`/`exclusionZoneRadiusBlocks` (25.6d)
      - `97-flat-underground-biome-band.yaml` →
        `flat.undergroundBiome`/`undergroundBelowSurfaceBlocks` (25.6g)
      - `98-sky-island-underground-biome-band.yaml` →
        `skyIsland.undergroundBiome`/`undergroundBelowSurfaceBlocks` (25.6g)
      Confirmed these are the *only* 3 red fixtures — the other 100 pass the
      new gate outright — and confirmed (via `SkyIslandSchema.java`/
      `FlatSchema.java`) that all 5 keys are genuinely absent from the
      schema, matching DESIGN §42.1/§42.5's claims exactly.
      **Found and flagged, not silently fixed: the real `config/tests/*.yaml`
      count is 103, not 104.** `TODO.md`/`DESIGN.md`/`CONFIG-RESTRUCTURE.md`/
      `GOALS.md` all say "104"; `ls config/tests/*.yaml` gives 103 (101
      numbered files, two of which — 27, 29 — also have an "a"-suffixed
      sibling), and `git log --diff-filter=D` shows no `config/tests/*.yaml`
      was ever deleted. `EXPECTED_FIXTURE_COUNT` is hardcoded to the verified
      103, with a comment explaining the discrepancy; see Deviation log. No
      config keys moved, no POJO/`logic/`/`client/` changes. `./gradlew
      :common:test` green (full suite, not just the new tests).
- [x] 25.6b Root + generic-preset sections (needs 25.6a) — **done**. Two new
      shared, generic (`<S>`) group classes in `config/schema/`:
      `StarterSchema<S>` (`biome`, `radius`, optional `land` sub-group typed
      `SchemaSection<S>`, non-null only for the root) and
      `NaturalBiomesSchema<S>` (`rivers`, `oceans`, optional `beaches` —
      `null` accessor omits the leaf). Both take an `Accessor<S, T>` per leaf
      (already-public getter/setter record) plus a `Supplier<S> factory`,
      instantiated once per call site with lambdas onto that owner's own
      flat fields — never singletons, per DESIGN R1/§42.2. `WorldzRootSchema`
      gained a private static nested `StarterLandSchema` (root-only, not
      shared, since only the root has `ensureStarterLand`/
      `starterLandTransitionBlocks`/`starterLandFoundationDepthBlocks`) and
      moved `naturalBiomes` up next to `starter` in `declare()`/emit order
      (was stranded after `structureDistance`), per DESIGN §42.3. Root's
      `summary()` now delegates to `starter.summary(value)`/
      `naturalBiomes.summary(value)` instead of hand-folding; `StarterSchema`
      itself is derived (no override) except `biome`'s render (`<none>` for
      empty, ported from `SingleBiomeSchema`/`ChaosBiomesSchema`'s old
      per-setting renderers, now shared); `StarterLandSchema.summary()` keeps
      the exact `transition=X, foundation=Y`/`<disabled>` folding.
      `SingleBiomeSchema`/`ChaosBiomesSchema`/`StripBandsSchema` all changed
      from field renames to one `starter`(+`spawn`)/`naturalBiomes` group
      pair each (`StripBandsSchema` has no `starter`, matching DESIGN); their
      `declare()` order is unchanged since the groups simply replace the
      fields they absorbed in place. `landBiome`→`biome` in
      `SingleBiomeSchema`; `regionScaleBlocks`→`regionScale` in
      `ChaosBiomesSchema` and separately in `LayoutSchema` (`layout
      .regionScale`, including its hand-written `postValidate` clamp message
      and `summary()` label); `widthBlocks`→`width` in `StripBandsSchema`.
      Fixtures migrated (all 22 in scope, `git diff --stat` confirms no
      others touched): `01`-`09` (root `starter`(+`.land` in `09`)),
      `10`-`12`/`14`/`15`/`85` (`singleBiome`), `16`-`19` (`chaosBiomes`),
      `29`/`29a` (`stripWorld.bands.width`); `13` needed no change (grepped
      empty for every old key). `config/jlt_worldz.example.yaml` migrated
      (root `starter`/`naturalBiomes`, `layout.regionScale`, `singleBiome`,
      `chaosBiomes` — `stripWorld` isn't in the example file, confirmed by
      grep before editing). `reference-defaults.yaml` **regenerated**: ran
      `WorldzConfigTest.defaultConfigMatchesTheCapturedReferenceDefaults`,
      extracted the actual `toYaml()` output from the assertion failure's
      XML report, diffed it against the old file by hand (`diff` output
      reviewed line-by-line — only the expected `starter`/`naturalBiomes`/
      `regionScale`/`biome`/`width` hunks moved, root's trailing
      `allowRivers: false` / `allowOceans: false` two lines are gone since
      `naturalBiomes` now emits near the top), then wrote it as the new
      golden file and confirmed the test goes green. `WorldzConfigTest`
      (~20 methods touched, each reviewed individually, not bulk-replaced)
      and `ConfigPresenceTest` (`topLevelScalarSetToItsOwnDefaultValueIs
      StillReportedPresent` renamed to `nestedGroupLeafSetToItsOwnDefault
      ValueIsStillReportedPresent` since `starter.radius` is no longer a
      top-level scalar; two `assertFalse` probes changed from the
      no-longer-real `starterRadiusBlocks` to `starter.radius`) updated.
      R13's summary assertion (`summaryUsesCanonicalValuesAndReadable
      DisabledStarter`) changed deliberately, segment by segment: root's
      `starterBiome=`/`starterRadiusBlocks=`/`starterLand=` trio collapsed
      into one `starter=biome=<none>, radius=256, land=transition=128,
      foundation=48` segment placed right after `allowedBiomes=`, a new
      `naturalBiomes=rivers=false, oceans=false` segment follows it
      immediately (matching the moved emit order), the trailing root
      `allowRivers=false, allowOceans=false` is deleted, and `singleBiome=`/
      `chaosBiomes=` each get the same `starter=`/`naturalBiomes=`
      restructuring inline. `README.md` mechanical key-name pass for the
      single-biome/chaos-biomes/strip-bands tables+examples, the root
      settings table+examples, "Guaranteed starter land", and "Coordinated
      world layouts"/"Seed-informed spawn" prose (`islandBiome`/
      `islandBiomes` left untouched — out of scope, 25.6d). No `logic/`/
      `client/` files in the diff (verified via `git diff --name-only`); no
      POJO field renames. `./gradlew build` green (`common:test` 839 tests,
      0 failures; fabric/neoforge assemble clean).
- [x] 25.6c Borders, exteriors, hazards (needs 25.6a) — **done**.
      `BorderSchema` gained two private nested group classes, `ResizeSchema`
      (`resize: {days, delayDays, style, rate}`) and `RateSchema` (`resize
      .rate: {blocks, days}`), a group inside a group over `BorderSchema`'s
      own `BorderConfig` type (DESIGN §42.2's "groups nest" — `resize`'s
      `Setting.group` accessor is itself an identity `Setting<BorderConfig,
      BorderConfig>`, so `copyInto` composes down through `rate` with zero
      extra framework code). `resizeStyle` now joins `resize` as `style`,
      moving from after `ensureEndPortal`/`ensureBlazeAccess` to inside the
      `resize` block — the one deliberate emit-order change DESIGN §42.3
      called out, confirmed via the regenerated `reference-defaults.yaml`.
      `initialRadiusBlocks`/`finalRadiusBlocks` → `initialRadius`/
      `finalRadius`; `EndBorderSchema.minimumRadiusBlocks` → `minimumRadius`;
      `ExteriorSchema.boundaryRadiusBlocks`/`oceanTransitionWidthBlocks` →
      `boundaryRadius`/`oceanTransitionWidth`; `RisingLavaSchema` gained its
      own private `RateSchema` (`rate: {blocks, days}`, no override needed —
      the inherited default `summary()` already renders `blocks=X, days=Y`);
      `StructureDistanceSchema.minDistanceBlocks` → `minDistance`. No POJO
      field renames anywhere — every `*Config` class keeps its original flat
      field names; only `Setting` key strings, nesting, and derived warning/
      summary text changed. `BorderSchema`/`RisingLavaSchema` `postValidate`
      keep their exact clamps and ordering (incomplete-rate-pair reset then
      stepped-fallback for border; `maxY>=startY` then the two rate clamps
      for rising lava) — only the path strings inside the warning messages
      changed (e.g. `resizeRateBlocks` → `resize.rate.blocks`). R2 verified
      by reading `ExteriorSchema.postValidate` directly (not just trusting
      DESIGN's claim): the sibling-border cross-check reads `BorderConfig`
      POJO fields (`initialRadiusBlocks`/`finalRadiusBlocks`/`enabled`)
      directly, never a YAML key, so the rename has zero interaction beyond
      its own warning text.
      **Found broader-than-listed fixture scope**, flagged rather than
      silently expanded: the TODO text named fixtures 13/20-25/79-85, but
      `overworldBorder`/`netherBorder`/`endBorder`/`*Exterior`/`risingLava`/
      `structureDistance` are shared root-level sections nearly every world
      type's own fixture sets just to fence/hazard the world, not only the
      ones whose own subject is border/hazard behavior — so `ConfigFixturesTest`'s
      unknown-key gate would have gone red on collateral fixtures otherwise.
      Grepped every `config/tests/*.yaml` for the old leaf-key strings and
      migrated all 21 that actually used them: 06, 13, 20-29 (+27a/29a), 73,
      76, 81-85 — 13 more than the 8 the TODO text named (`oceanIsland`'s own
      keys in 83 left untouched, out of scope for 25.6d). `strip:` left
      completely untouched (`widthRadiusBlocks`/`widthMode` unchanged in
      every fixture and in `StripSchema.java`) — confirmed via `git diff`
      touching no `Strip*.java` file; the F1 deviation was already logged at
      25.6a/§42.1, no new entry needed. `config/jlt_worldz.example.yaml`
      migrated for the 5 sections it covers (`structureDistance` isn't in
      the example file, confirmed by grep before editing). `reference-
      defaults.yaml` regenerated and diffed by hand (only the expected
      `resize`/`boundaryRadius`/`oceanTransitionWidth`/`minimumRadius`/
      `minDistance`/`rate` hunks moved). `WorldzConfigTest` (border/exterior/
      risingLava/structureDistance test bodies' YAML re-nested, Java field
      assertions on `BorderConfig`/`ExteriorConfig`/etc. untouched since
      those POJO field names didn't change), `StackedConfigTest` (2 fixtures
      reusing `overworldBorder`/`overworldExterior` inline YAML), and
      `ProjectMetadataTest` (2 README-content assertions moved to
      `resize.rate.blocks`/`resize.delayDays` wording) updated; `ConfigPresenceTest`
      needed no change (grepped empty for every affected key). `README.md`
      mechanical key-name pass for "Limited-world borders", "Carrying the
      border into the End", "Ocean and void exteriors", forever-night's
      known-limitation note, "Rising lava floor", and "Structures far from
      spawn". `config/tests/README.md`/`MANUAL_TESTING.md` prose left alone
      per DESIGN §42.5 (explicitly deferred to 25.11). No `logic/`/`client/`
      files in the diff (verified via `git status`); `client/WorldzBorderScreen
      .java`'s own `resizeRateBlocks`/`resizeRateDays`/`resizeDelayDays` Java
      field names are unaffected (client-side widget fields, not YAML keys,
      out of scope) and its own `ProjectMetadataTest` assertions (lines
      693-695) needed no change. `./gradlew build` green (`common:test` 838
      tests, 0 failures — matching 25.6b's own count of "839" minus nothing:
      `@Test` annotation count is unchanged, 735 before and after, confirming
      no test was silently dropped); `ConfigFixturesTest` still reports the
      full 104 (103 fixtures + 1 count assertion), all green, including the
      21 migrated fixtures' unknown-key gate.
- [x] 25.6d Islands (needs 25.6a) — **done**.
      Two new shared, generic group classes: `ExclusionZoneSchema<S>`
      (`enabled`/`radius`, parameterized on the radius bound — `[1,
      MAX_VALUE]` for `oceanIsland`/`skyIsland`/`floatingIslands`, `[0,
      MAX_BORDER_RADIUS_BLOCKS]` for `chunkIsland`) and `ChestSchema<S>`
      (`tier`/`kits: {easy, medium, hard}`, optional `enabled` leaf — not
      exercised yet, only `cave` needs it at 25.6e — with a private nested
      `KitsSchema<S>` group proving "groups nest" a third time). Both
      instantiated per call site, never singletons (DESIGN R1).
      `oceanIsland`: new `island: {source, biome, radius, shapeAmplitude}`
      group (contiguous — `fluid` moved out from between `islandSource`/
      `islandBiome` per DESIGN §42.3's own example) and `ocean: {shallowWidth,
      deepenWidth, shallowDepth, deepDepth, regionScale}`; `shoreWidthBlocks`
      → `shoreWidth`; `exclusionZone` reuses the shared class at `[1, MAX]`.
      No `summary()` override needed — was already fully derived pre-25.6d,
      stays that way (every group Setting got its own `.render(...)`).
      `skyIsland`: prefix drop (`islandBiome`/`radiusBlocks`/`thicknessBlocks`
      → `biome`/`radius`/`thickness`, no `island:` wrapper — the section name
      already supplies it, DESIGN §42.3's noted asymmetry); `chestTier`/
      `easyKit`/`mediumKit`/`hardKit` → `chest.tier`/`chest.kits.easy`/
      `.medium`/`.hard`; `applyToNether` stays bare (one-member group would
      be noise, matching `strip`'s own precedent).
      **`skyIsland.exclusionZone` wired up as a real, working group — a
      confirmed behavior change, not a pure rename** (DESIGN §42.1/§42.7's
      answered open question): `exclusionZoneEnabled`/`exclusionZoneRadiusBlocks`
      were real `SkyIslandConfig` fields, already consumed by world-gen logic
      (verified directly — `SkyIslandCustomization`/the Customize-screen path
      reads them independently of this config path), but never threaded
      through read/sanitize at all before this commit; `skyIslandSummary`
      rendered their untouched constructor defaults (`true`/`128`), which
      happen to be what the new derived summary also renders, so the
      summary text is unchanged even though the mechanism underneath it is
      now real. Wired via the same shared `ExclusionZoneSchema` at `[1, MAX]`
      (closest conceptual precedent: `oceanIsland`'s own bound). Because
      `SkyIslandSchema` no longer has any dead/unmapped field left (the
      `undergroundBiome` pair is the sole remaining gap, deferred to 25.6g),
      its previously-mandatory hand-written `summary()` override — needed
      solely to fake-render the dead pair — is gone; the inherited default
      now produces the identical segment mechanically. **Verified the wire-up
      actually threads values, not just that the keys parse**: 3 new
      `WorldzConfigTest` methods (`skyIslandExclusionZoneLoadsAndSanitizes
      Independently`, `...DefaultsToEnabledWithA128BlockRadius`, `...
      RadiusIsClamped`) assert `config.skyIsland.exclusionZoneEnabled`/
      `exclusionZoneRadiusBlocks` after parsing non-default YAML values,
      the same regression shape that would have caught this gap originally.
      `config/tests/57-sky-island-biome-exclusion-zone.yaml` (its own
      dedicated in-game test for exactly this feature) is migrated to
      `exclusionZone: {enabled, radius}` and moved off `ConfigFixturesTest
      .KNOWN_UNKNOWN_KEYS` — it was there specifically because these two keys
      were unrecognized; now they're real, declared settings, so the
      unknown-key gate no longer flags it (confirmed by running
      `ConfigFixturesTest` green with the entry removed, not merely deleting
      the map entry and assuming).
      `skyIsland.floatingIslands`: `minRadiusBlocks`/`maxRadiusBlocks` →
      `radius: {min, max}` (kept automatic per-setting sanitize — not part of
      the divergent tail, unlike the three below); `cellSizeBlocks`/
      `islandBiomes` → `cellSize`/`biomes`; `exclusionZone` (shared class,
      `[1, MAX]`), `oreDeposits: {enabled, featureIds}` and
      `lootChest: {enabled, kit}` are private, non-shared groups, each with
      `Rule.None` on the *outer* group Setting and a manual
      `<group>.sanitize(value, ctx)` call in `postValidate` at the exact
      original imperative position (DESIGN R4 — the same treatment `lootKit`
      already had pre-25.6d); `oreFeatureIds`' manual trim became a real
      `Rule.TrimNonEmpty` on the inner leaf (still runs before the
      enabled+empty cross-check, since the group's own per-setting sanitize
      always precedes its own `postValidate`). Every advisory-warning
      ordering/wording preserved verbatim; only path strings changed.
      `chunkIsland`: `topOnly`/`topOnlyDepthBlocks`/`scatteredTopOnlyChance`
      → `topOnly: {enabled, depth, scatteredChance}` — required pulling
      `scatteredTopOnlyChance` out of its old position (after
      `exclusionZoneRadiusBlocks`) to sit contiguously with `topOnly`/
      `topOnlyDepthBlocks`, the same kind of emit-order change DESIGN §42.3
      documents for `oceanIsland.fluid`; confirmed no cross-field dependency
      exists between the three, so the reorder is behavior-neutral.
      `exclusionZone` reuses the shared class at its own pre-existing
      `[0, MAX_BORDER_RADIUS_BLOCKS]` bound (the one call site floored at
      `0`, not `1`) and stays `hiddenFromSummary` (moot — `summary()` is
      still fully hand-overridden and never delegates through it).
      `applyToNether`/`applyToEnd` → `applyTo: {nether, end}`.
      Fixtures migrated: 30-37 (`oceanIsland`), 38-43 (`skyIsland` prefix
      drop), 44-48 (`floatingIslands`), 49-52 (`chunkIsland`), 57
      (`skyIsland.exclusionZone`, the behavior-change fixture), 58
      (`floatingIslands`, `skyIsland` scope), 83 (`oceanIsland` keys inside a
      `risingLava` fixture, left untouched by 25.6c per its own deviation
      note) — matching the TODO line's own list exactly, plus 98's
      `islandBiome`/`radiusBlocks` → `biome`/`radius` (its own
      `undergroundBiome`/`undergroundBelowSurfaceBlocks` pair correctly left
      alone, still deferred to 25.6g) — found by grepping every
      `config/tests/*.yaml` for the old key strings rather than trusting the
      TODO line's list alone (25.6b/c's own documented lesson). Prose-only
      mentions of old keys inside fixture header comments (34, 35, 38, 98)
      also corrected for accuracy, even though `config/tests/README.md`
      itself stays deferred to 25.11 per DESIGN §42.5.
      `config/jlt_worldz.example.yaml` not touched — confirmed via grep it
      covers none of these four sections. `reference-defaults.yaml`
      regenerated and diffed by hand (only `oceanIsland`/`skyIsland`/
      `chunkIsland` hunks moved; `skyIsland.exclusionZone` newly appears in
      the golden file at all, which *is* the behavior change made visible).
      `WorldzConfigTest`: every YAML string containing an old key was
      updated (found several that would have silently kept passing on stale
      keys — e.g. `oceanIslandInvalidIslandBiomeFallsBackToDefault` — because
      the asserted value coincidentally equals the untouched POJO default,
      the same false-positive trap 25.6b's lesson warned about; fixed
      regardless of whether the test was in the initial red list). R13's
      96-line summary assertion updated for the `oceanIsland`/`skyIsland`
      segments only, reviewed deliberately, one section at a time.
      `ConfigPresenceTest` needed no change (grepped empty for every affected
      key). `README.md` mechanical key-name pass for "Ocean island
      challenge", "Sky island challenge" (+ floating resource islands), and
      "Sky chunk challenge", including a new prose paragraph + table row for
      `skyIsland.exclusionZone` (previously undocumented since it wasn't
      real). No `logic/`/`client/` files in the diff (verified via `git
      status`). `./gradlew build` green (all modules): `common:test` 841
      tests, 0 failures (838 + 3 new); `ConfigFixturesTest` 103 fixtures + 1
      count assertion, all green, with `KNOWN_UNKNOWN_KEYS` down to just 97/98.
- [x] 25.6e Cave / Nether-start / End-start (needs 25.6d for `ChestSchema`) —
      **done**. `CaveSchema`: `spawnDepthY` → `spawnY` (per
      CONFIG-RESTRUCTURE.md §3's `cave.yaml` example, matching
      `netherStart.spawnY`'s existing naming); two new private, non-generic
      groups over `CaveSchema`'s own `CaveConfig` type (there is only ever one
      owner shape, unlike `ChestSchema`/`ExclusionZoneSchema`, so no `<S>`
      parameterization needed — same pattern as `BorderSchema.ResizeSchema`):
      `SealedSurfaceSchema` (`sealedSurface`/`sealedSurfaceY`/
      `sealedSurfaceBlock`/`sealedSurfaceThicknessBlocks` → `sealedSurface:
      {enabled, y, block, thickness}`) and `CavernSchema` (`cavernEnabled`/
      `cavernRadiusBlocks`/`cavernHeightBlocks` → `cavern: {enabled, radius,
      height}`). DESIGN R5's one conditional clamp (`.when(c ->
      c.sealedSurface)` on `y`'s `IntBuilder`) survives verbatim inside the
      new group — the predicate reads `CaveConfig.sealedSurface` directly,
      unaffected by the nest — only the key/path string changed
      (`cave.sealedSurfaceY` → `cave.sealedSurface.y`), confirmed by
      `caveSealedSurfaceYIsClampedOnlyWhenEnabled` still passing unmodified
      in its assertions (only the input YAML shape changed).
      **`chest.enabled` is the first real exercise of `ChestSchema`'s full
      (`enabled`-leaf) constructor**, TODO 25.6d's Javadoc-flagged gap: `cave`
      now instantiates it with `chestEnabled`'s accessor as the fifth
      constructor argument, alongside `chestTier`/three kits →
      `chest.tier`/`chest.kits.{easy,medium,hard}` exactly as `skyIsland`
      already does with the no-`enabled` constructor. Confirmed working via
      `caveSettingsLoadAndSanitizeIndependently` (round-trips `chest.enabled:
      true`) and the regenerated golden file, which shows `chest.enabled`
      before `chest.tier` in `cave`'s own emit order but *not* in
      `netherStart`/`endStart`'s (no `enabled` leaf there) — the parameterized
      constructor branch is real, not dead code.
      `NetherStartSchema`/`EndStartSchema`: both switched from three flat
      `easyKitSchema`/`mediumKitSchema`/`hardKitSchema` fields + hand-written
      `chestTier` enum setting to a single `ChestSchema<NetherStartConfig>`/
      `ChestSchema<EndStartConfig>` instance (no-`enabled` constructor, chest
      unconditional in both) — `chestTier`/three kits → `chest.tier`/
      `chest.kits.*`, identical to `skyIsland`'s own 25.6d conversion.
      **`StarterCapsuleSchema` (DESIGN R3): renamed `sizeBlocks`/`heightBlocks`
      → `size`/`height` (odd-rounding-before-clamp order on `size` untouched)
      and nested `lightSource`/`lightSpacingBlocks` into a new private
      `LightSchema` group (`light: {source, spacing}`) — the light-spacing
      bound stays per-parent, threaded through `LightSchema`'s own
      constructor exactly like the outer class's size/height bounds.** Both
      parameterized instantiation sites (`NetherStartSchema`'s and
      `EndStartSchema`'s constructors, at `NetherStartPlan`'s vs
      `EndStartPlan`'s own `MIN`/`MAX_CAPSULE_*` bounds) renamed together in
      this one commit — verified by grepping both call sites before
      declaring done, per DESIGN R3's own warning that the two must not
      diverge. Confirmed via `netherStartSettingsLoadAndSanitizeIndependently`/
      `netherStartCapsuleSizeIsOddenedAndClamped`/`endStartSettingsLoadAnd
      SanitizeIndependently`/`endStartCapsuleSizeIsOddenedAndClamped`, all
      updated to the new nested YAML shape and still green.
      Fixtures actually touched: the TODO line's own list (53-56, 59-65,
      86-93) plus a stale prose-only `chestTier` comment in
      `38-sky-island-default.yaml` (a 25.6d leftover, corrected for accuracy —
      same class of miss 25.6b/c/d's own lessons already flagged; verified by
      grepping every `config/tests/*.yaml` for the old key strings rather
      than trusting the TODO line's list alone). `config/tests/README.md`
      stays untouched, deferred to 25.11 per DESIGN §42.5. `config/
      jlt_worldz.example.yaml` not touched — confirmed via grep it covers
      none of these three sections. `reference-defaults.yaml` regenerated
      (only `cave`/`netherStart`/`endStart` hunks moved, captured by
      temporarily writing `toYaml()` to a scratch file rather than
      hand-transcribing). `WorldzConfigTest`: every YAML string containing an
      old key updated (`caveSettingsLoadAndSanitizeIndependently`,
      `caveSealedSurfaceYIsClampedOnlyWhenEnabled`,
      `caveCavernRadiusAndHeightAreClamped`,
      `netherStartSettingsLoadAndSanitizeIndependently`,
      `netherStartKitsLoadIndependently`, `netherStartCapsuleSizeIsOddenedAndClamped`,
      `netherStartSpawnYIsClamped`, `endStartSettingsLoadAndSanitizeIndependently`,
      `endStartCapsuleSizeIsOddenedAndClamped`,
      `endStartKitExtrasCountIsClampedWhenPoolIsEmpty` — POJO field accessors
      like `config.cave.spawnDepthY` left untouched throughout, only the
      parsed YAML text and R13's 96-line summary assertion (`cave`/
      `netherStart`/`endStart` segments, captured the same scratch-file way
      rather than hand-derived, then reviewed) changed. `ConfigPresenceTest`
      updated (`cave.easyKit.essentials`/`cave.easyKit`/`cave.mediumKit`/
      `cave.spawnDepthY` → `cave.chest.kits.easy.essentials`/`cave.chest.kits
      .easy`/`cave.chest.kits.medium`/`cave.spawnY`) — the one presence test
      DESIGN §42.6's checklist calls out by name. `README.md` mechanical
      key-name pass for "Cave challenge", "Nether-start challenge" and
      "End-start challenge". No `logic:`/`client:` files in the diff (verified
      via `git status`). `./gradlew build` green (all modules): `common:test`
      841 tests, 0 failures (same count as 25.6d — no tests added, only
      updated); `ConfigFixturesTest` still 103 fixtures + 1 count assertion,
      all green.
- [x] 25.6f Flat family (needs 25.6a) — **done**. `DeepFlatSchema`:
      `riversEnabled`/`riverExclusionRadiusBlocks` → a new private
      `RiversSchema` group (`rivers: {enabled, exclusionRadius}`) over
      `DeepFlatSchema`'s own `DeepFlatConfig` type — there is only ever one
      owner shape here (DESIGN §42.1's own "single-owner" call, confirmed
      against §42.3's table), so it's private and non-generic, same pattern
      as `CaveSchema.SealedSurfaceSchema`/`CavernSchema` and
      `BorderSchema.ResizeSchema`/`RateSchema`, not a shared `<S>`-parameterized
      class like `ExclusionZoneSchema`/`ChestSchema`. `StackedSchema`:
      `reliefBlocks` → `relief`, a bare rename (DESIGN §42.3 lists no nesting
      for it). `FlatSchema`: **no change** — re-read `FlatSchema.java`/
      `FlatConfig.java` directly rather than trusting the TODO line's "flat
      suffix drops" at face value; its four declared settings (`layers`,
      `biome`, `decoration`, `structureOverrides`) have no `Blocks` suffix at
      all, and its one `Blocks`-suffixed POJO field
      (`undergroundBelowSurfaceBlocks`) is the pre-existing gap explicitly
      deferred to 25.6g (never wired into the schema, per `FlatSchema`'s own
      Javadoc) — so there was nothing to drop.
      Fixtures actually touched: `69-deep-flat-default.yaml`/
      `70-deep-flat-no-rivers.yaml` (`riversEnabled`/`riverExclusionRadiusBlocks`
      → `rivers: {enabled, exclusionRadius}`, plus stale `riverExclusionRadiusBlocks`/
      `riversEnabled` mentions in both files' own prose comments) and
      `72-stacked-default.yaml`/`77-stacked-relief-off.yaml` (`reliefBlocks` →
      `relief`, plus a stale `reliefBlocks` mention in each file's own prose
      comment) — grepped every `config/tests/*.yaml` for both old key strings
      first (25.6b/c/d/e's own recurring lesson) and confirmed no other
      fixture in the TODO line's 66-78/94-96/99-101 range references either
      key; the rest of that range (66-68, 71, 73-76, 78, 94-96, 99-101) needed
      no edits at all. Fixture 96 (`legacy-cave-biomes-underground-only.yaml`)
      is unrelated to this shape (an `allowedBiomes` fixture) despite being in
      the listed range. Fixtures 97/98 (the deferred `underground` pair)
      untouched, still exactly `ConfigFixturesTest.KNOWN_UNKNOWN_KEYS`'
      expected set. `config/tests/README.md`/`MANUAL_TESTING.md` stay
      untouched, deferred to 25.11 per DESIGN §42.5 (same precedent as every
      prior sub-step). `config/jlt_worldz.example.yaml` not touched —
      confirmed via grep it covers neither `deepFlat`/`stacked`/`flat`.
      `reference-defaults.yaml` regenerated by capturing the real
      `WorldzConfigTest.defaultConfigMatchesTheCapturedReferenceDefaults`
      failure's own "actual" `toYaml()` output rather than hand-transcribing —
      diffed against the old file first to confirm only the two expected
      hunks moved. `WorldzConfigTest` updated: `deepFlatSettingsLoadAndSanitizeIndependently`,
      `stackedSettingsLoadAndSanitizeIndependently`,
      `stackedReliefBlocksClampsToConfiguredMaximum` (method name unchanged,
      only its YAML body) and R13's 96-line summary assertion (`deepFlat`/
      `stacked` segments: `riversEnabled=true, riverExclusionRadiusBlocks=512`
      → `rivers=enabled=true, exclusionRadius=512`; `reliefBlocks=4` →
      `relief=4`) — POJO field accessors like `config.deepFlat.riversEnabled`/
      `config.stacked.reliefBlocks` left untouched throughout, only parsed
      YAML text changed. `README.md` mechanical key-name pass for "Deep flat
      challenge"/"Stacked challenge". No `logic:`/`client:` files in the diff
      (verified via `git status`). `./gradlew build` green (all modules):
      `common:test` 841 tests, 0 failures (same count as 25.6e — no tests
      added, only updated); `ConfigFixturesTest` still 103 fixtures + 1 count
      assertion, all green.
- [x] 25.6g Wire the dead `underground` pair (needs 25.6d, 25.6f) — **done**.
      Jason confirmed 2026-07-28: wire it up. New shared, reusable
      `UndergroundSchema<S>` (`config/schema/UndergroundSchema.java`,
      parameterized only on `S` — unlike `ExclusionZoneSchema`/`ChestSchema`,
      both owners share one identical shape with no per-site bound
      difference, so there's nothing else to parameterize): declares
      `biome`/`belowSurface` as real `Setting`s for the first time, reused at
      both `FlatSchema` (new `underground` field + one new `Setting.group`
      entry, appended after `structureOverrides` matching `FlatConfig`'s own
      field order) and `SkyIslandSchema` (same shape, inserted between
      `exclusionZone` and `floatingIslands` matching `SkyIslandConfig`'s own
      field order). `biome` reuses `Rule.BiomeId(allowEmpty=true, …)` — the
      same "blank means disabled" shape `StarterSchema.biome` already uses —
      rather than `BiomeIdOrDefault`, since an empty `undergroundBiome` is
      the documented, legal "band disabled" state (DESIGN §37.3), not an
      error requiring a fallback. `belowSurface` floors at `0` (`range(0,
      Integer.MAX_VALUE)`) — the one real invariant this field has (`FlatPlan`/
      `SkyIslandPlan`'s own compact constructors both reject a negative
      value) — with no invented upper bound, since neither `Plan` record nor
      any Customize-screen control (the band stays config-only, per both
      classes' own pre-existing Javadoc) imposes one today. No `logic:`
      changes needed: `FlatPlan.fromConfig`/`SkyIslandPlan.fromConfig` already
      read `config.undergroundBiome`/`undergroundBelowSurfaceBlocks` directly
      and unconditionally — they were only ever fed the untouched constructor
      defaults before this task because nothing upstream ever parsed a real
      value onto those fields; now that the schema actually populates them,
      the existing read-path needs no change at all. `FlatConfig`/
      `SkyIslandConfig` POJO field names themselves untouched, per this
      task's own constraint. Fixtures 97 (`97-flat-underground-biome-band.yaml`)
      and 98 (`98-sky-island-underground-biome-band.yaml`) migrated to the new
      `underground: {biome, belowSurface}` key (plus each file's own stale
      prose-comment mentions of the old flat keys) and removed from
      `ConfigFixturesTest.KNOWN_UNKNOWN_KEYS` (now `Map.of()` — empty, kept
      live rather than deleted so a future gap of this same shape has an
      obvious place to be recorded); `ConfigFixturesTest` confirms all 104
      fixture-related test cases (103 fixtures + 1 count assertion) stay
      green with both entries gone, proving 97/98 now genuinely exercise the
      schema rather than merely being tolerated as unknown keys. 7 new
      `WorldzConfigTest` regression cases (`flatUndergroundLoadsAndSanitizesIndependently`,
      `flatUndergroundDefaultsToDisabled`, `flatUndergroundBelowSurfaceIsFlooredAtZero`,
      `flatUndergroundInvalidBiomeIsIgnoredRatherThanFailing`,
      `skyIslandUndergroundLoadsAndSanitizesIndependently`,
      `skyIslandUndergroundDefaultsToDisabled`,
      `skyIslandUndergroundBelowSurfaceIsFlooredAtZero`) confirm a
      non-default configured value actually threads through to
      `config.flat.undergroundBiome`/`undergroundBelowSurfaceBlocks` and the
      `SkyIslandConfig` equivalents — not just that the keys parse without
      error, the same discipline 25.6d's `skyIslandExclusionZoneLoadsAndSanitizesIndependently`
      established. R13's 96-line summary assertion updated with the two new
      `underground=biome=<none>, belowSurface=10` segments (`flat=`'s own,
      after `structureOverrides=…`; `skyIsland=`'s own, after
      `exclusionZone=radius=128`) — no summary override needed on
      `UndergroundSchema` itself, since `biome`/`belowSurface` render as two
      independent mechanical segments exactly like `StarterSchema`'s own
      `biome=<none>, radius=256` precedent (not a collapsed `enabled`/
      `radius` pair like `ExclusionZoneSchema`). `reference-defaults.yaml`
      regenerated by capturing the real `toYaml()` output (diffed against the
      old file first: exactly the two expected `underground:` blocks added,
      nothing else moved). `README.md` updated at both tables (sky island
      and flat) and the flat section's two example snippets/prose (the
      "Underground biome band" subsection) — old flat key names no longer
      appear anywhere in `README.md` (grepped to confirm).
      `config/tests/README.md`/`MANUAL_TESTING.md` and
      `config/jlt_worldz.example.yaml` stay untouched, same deferral/scope
      precedent 25.6f already logged (25.11 for the first two;
      `jlt_worldz.example.yaml` doesn't cover `flat`/`skyIsland` at all,
      confirmed via grep). `./gradlew build` green (all modules): `common:test`
      848 tests, 0 failures (841 + 7 new regression tests); `ConfigFixturesTest`
      still 103 fixtures + 1 count assertion, all green, `KNOWN_UNKNOWN_KEYS`
      now empty.
- [x] 25.6h Close-out (needs 25.6b-25.6g) — **done**. Full exhaustive
      `README.md` grep pass for every old key name in DESIGN §42.3's map (all
      ~60 old names individually grepped, not just the ones a single
      sub-step touched): zero stragglers found. Every remaining bare hit
      (`islandSource`, `chestTier`, `shapeAmplitude`, `applyToNether`,
      `widthRadiusBlocks`/`widthMode`) is either a correct Customize-screen
      UI label paired with the right config-key parenthetical, or one of the
      two deliberate un-renamed cases DESIGN §42.3 itself calls out
      (`skyIsland`/`strip` both keep a bare `applyToNether`; `strip.width*`
      untouched per 25.9). `config/jlt_worldz.example.yaml` re-read in full:
      no stale key names or stale prose (it only ever covered the 13
      sections F6 named, all already correct from their own sub-steps).
      Tightened `ConfigSchemaMetadataTest` with a new
      `noLeafKeyEndsInTheDroppedBlocksSuffix` test — walks the same
      `SchemaKeyWalker.collectKeys` tree `theSchemasDeclaredKeysExactlyMatchWhatToMapEmits`
      already does (so it checks every leaf in the whole schema, not just
      what an individual sub-step touched) and asserts no dotted leaf key
      ends in `Blocks`, except a one-entry `BLOCKS_SUFFIX_ALLOW_LIST`
      (`strip.widthRadiusBlocks` — the sole deliberate DESIGN §42.1 holdout,
      allow-listed by exact key rather than exempting `strip` wholesale so
      any other stray `Blocks` leaf still fails). Ran green with zero
      allow-list misses beyond that one entry — confirmed via source grep
      first that `StripSchema.java` is the only `*Schema.java` with a
      `"...Blocks"` key-string literal anywhere in the tree. Logged the one
      missing Deviation-log entry (F1's four corrections — the `strip`
      deviation was already re-logged at 25.6c); see Deviation log,
      2026-07-28 (25.6h). `./gradlew build` green (all modules): `common:test`
      849 tests, 0 failures (848 + 1 new test); `ConfigFixturesTest` still
      103 fixtures + 1 count assertion, `KNOWN_UNKNOWN_KEYS` confirmed
      empty. **TODO 25.6 (all of 25.6a-h) is now fully closed**: the
      framework (`Setting.group`/`SchemaSection.copyInto`, 25.6a) plus 8
      sub-steps nested 13 within-class shapes, 3 shared cross-class shapes
      (`exclusionZone`, `naturalBiomes`, `chest`), 3 shapes F1 missed
      (`starter`, `starter.land`, `deepFlat.rivers`), wired the two
      previously-dead pairs (`skyIsland.exclusionZone` at 25.6d,
      `underground` at 25.6g), and dropped every `Blocks` suffix except the
      one deliberately deferred to 25.9 — with zero POJO/`logic/`/`client/`
      changes throughout, exactly per DESIGN §42's hard constraint.
- [x] 25.7 Split into `config/jlt_worldz/` (D2, D10). Biggest single win is
      moving the 11 generic-preset-only top-level keys (`allowedBiomes`,
      `starterBiome`, `layout`, `strip`, …) into `world-types/worldz.yaml`
      where they stop masquerading as global (F3), and merging the
      `strip`/`stripWorld` split-brain into one file (file-level only —
      see 25.7's note under 25.9 below for the key-level half).
      **Design pass done, DESIGN §43** (2026-07-28): full 26-entry
      section-to-file mapping across 15 files, merge-before-schema-walk
      load mechanics (no framework class touched), per-file unknown-key
      gate, and `config/jlt_worldz/all.yaml` kept as an optional
      single-file bundle that wins wholesale when present — **Jason
      confirmed 2026-07-28: keep the bundle.** This means `config/tests/`
      fixtures and `MANUAL_TESTING.md`'s workflow stay single-file (no
      103-fixture-directory conversion), so 25.11 shrinks to prose-only
      updates. `kits.yaml` is explicitly **not** created by 25.7 — deferred
      to 25.8 (DESIGN §43.6). Broken into 25.7a-25.7e below, each
      independently buildable/testable/committable (the bundle path keeps
      today's single-file load alive throughout, so nothing here is forced
      to land atomically).
- [x] 25.7a `ConfigLayout`/`ConfigFile` (main, `config/` package): — **done**. The
      26-root-key → 15-file mapping table (DESIGN §43.2/§43.4.1);
      corrected `Applicability` on the 26 root settings to match; new
      `ConfigLayoutTest` proving the mapping totally partitions
      `WorldzRootSchema.declare()`'s keys with no duplicates/gaps, each
      unwrapped file owns exactly one key, and each world-type filename
      matches its `world_preset/*.json` id (`sky-chunk.yaml`↔`chunkIsland`
      allow-listed by name, DESIGN §43.9 row a). No load-path change yet.
- [x] 25.7b `parse(String)` → `parseMap(Map)` refactor; `readSplit`/
      `readBundle`; `WorldzConfig.load` rewritten per DESIGN §43.4.2-4;
      per-file skip/WARN handling (absent/blank/non-mapping/YAML-syntax,
      each isolated per file; value-level errors still all-defaults, per
      DESIGN §43.4.4); bundle wins wholesale + WARN when present. New
      `ConfigDirectoryLoadTest`: split the golden `reference-defaults.yaml`
      into the 15 files via `ConfigLayout`, load from a `@TempDir`, assert
      `toYaml()` equals the golden byte-for-byte; plus absent-dir, isolated
      per-file failure, bundle-wins, and cross-file
      `config.present("overworldBorder")` (25.5's `stacked` gate) cases.
      **`git diff` must not touch `reference-defaults.yaml`** — no key
      moved in this task, only the file a key is read from. — **done**.
- [x] 25.7c `SchemaKeyWalker.findUnknownKeysInFile` (DESIGN §43.5) +
      shallow production misfile WARN via `ConfigLayout.owning`. Proves: a
      stray key in `cave.yaml` reports against `CaveSchema` only (not the
      whole root); a `cave:` key written into `runtime.yaml` reports as
      misfiled, naming `world-types/cave.yaml`; `layout.roleOverrides`'
      arbitrary sub-keys still don't trip it. `ConfigFixturesTest` itself
      is unchanged (fixtures stay bundle-shaped). — **done**.
- [x] 25.7d Docs: README.md config section (file tree, wrapped-vs-unwrapped,
      the bundle, first written statement of F3's live-vs-baked split as
      it maps to `runtime.yaml` vs. everything else); `REFERENCE_HEADER`
      grows a file map (e.g. `# foreverNight/risingLava/structureDistance
      -> config/jlt_worldz/runtime.yaml`); one-line `config/tests/
      README.md` update for the new `cp` target
      (`config/jlt_worldz/all.yaml`). 25.10 still owns the generated
      settings tables. — **done**. Note: `config/jlt_worldz.example.yaml`
      header left stale (its first section layout block); will fix in 25.10.
- [x] 25.7e Close-out: Deviation log (strip file-level-only merge, key-level
      merge moved to 25.9; `kits.yaml` deferred to 25.8); amend 25.9's text
      to explicitly own the `strip`/`stripWorld` key-level merge and
      unwrapping `strip-world.yaml` (DESIGN §43.3); confirm 25.11's scope
      is prose-only (DESIGN §43.8); full `./gradlew build` all modules;
      NeoForge brief check (no loader-level code expected —
      `WorldzCommon.java:30-31`'s `load` signature is unchanged). **[Jason]**
      redeploy both Prism instances before requesting test (per memory:
      deploy-jar-before-requesting-test). — **bookkeeping half done**
      (`project-manager`, 2026-07-28): three Deviation log entries added —
      strip file-level-only merge with the key-level merge deferred to 25.9
      (DESIGN §43.3), `kits.yaml` deferred whole to 25.8 (DESIGN §43.6), and
      a minor 25.7a doc-staleness note on DESIGN §43.4.1's Applicability-
      override count (3 claimed, 6 actual, no behavior impact) — see
      Deviation log, 2026-07-28 (25.7e close-out / 25.7a). Verified rather
      than re-amended: 25.9's text already explicitly owns the
      `strip`/`stripWorld` key-level merge and `strip-world.yaml` unwrapping,
      and 25.11's text already reflects the reduced prose-only scope — both
      amended correctly in the 859f51a design pass before implementation
      started, and both still accurately describe what 25.7a-d actually
      shipped. **Not covered by this check-off:** the full `./gradlew build`
      all-modules run, the NeoForge brief check, and the Prism redeploy —
      the automated portion was handled later by `tester`; Prism deployment
      remains Jason-only.
      **TODO 25.7 (all of 25.7a-25.7e) is complete.** Automated gate
      subsequently passed under Temurin 25: full `./gradlew build`, 902 tests
      green, clean-tree verification at that point, and both Fabric and
      NeoForge artifacts present. **[Jason]** Prism redeployment remains
      outstanding and is not implied by this check-off.
- [x] 25.8 Named shared starter kits (D6): pull the 14 starter-kit item lists
      (not 12 — F2 miscounted, see Deviation log) out of their preset
      sections into a new named library, referenceable by name, inline
      definitions still legal. **Design pass done, DESIGN §44** (2026-07-28):
      polymorphic ref-or-inline leaf (`Codecs.namedOrSection`/
      `Rule.KitReference`), a new `kits` root section (`config/jlt_worldz/
      kits.yaml`, merges over the shipped defaults rather than replacing
      them), 14 pre-named shipped kits (`cave-easy`, `ocean-island-default`,
      etc. — full table in DESIGN §44.5), resolution as a same-pass
      cross-*section* read (`SanitizeContext.root`), not a cross-file lookup
      (25.7's merge-before-walk already put every file in one map).
      **Jason confirmed 2026-07-28 (all three questions in DESIGN §44.9):**
      (1) proceed even though named kits *relocate* rather than shrink the
      config — no two of the 14 kits share contents, so nothing merges; the
      real wins are locality (`cave.yaml` drops ~41→~14 lines) and one-line
      kit swapping, not size reduction; (2) the generated
      `jlt_worldz.reference.yaml` emits all 14 kits' **full contents**
      inline (stays a valid standalone `all.yaml` bundle), not just names;
      (3) **old TODO 24.5 (capsule-field consolidation) is un-folded back to
      Phase 24**, merged with 24.4 — the assumed overlap with this item
      never existed (DESIGN §44.1, and see the rewritten 24.5 above). Broken
      into 25.8a-25.8h below, each independently buildable/testable/
      committable; `a`→`b`→`c`→`d` is a hard chain, `e` needs `b`, `f`/`g`
      need `d`, `h` last.
- [x] 25.8a Framework only (DESIGN §44.3, §44.4.2): `StarterKitConfig.ref` +
      `reference(...)`; `Codecs.namedOrSection`; `Rule.KitReference` +
      `Rule.NestedMap`; `Codecs.sectionMap`; `StarterKitSchema.reference(...)`
      helper. No site converted, no `kits` root key added yet. New
      `KitReferenceTest` on a synthetic two-site POJO (mirrors
      `SettingGroupTest`'s style). Proves: an inline mapping round-trips
      byte-identically through the new codec; a string reads as a reference;
      a non-string/non-map still throws today's exact message.
      **`reference-defaults.yaml` must not change** — if it does, (a) has a
      bug.
- [x] 25.8b The library (DESIGN §44.4) — **done**. `KitLibrary` with the 14 shipped
      entries moved **verbatim** (not retyped) from `CaveConfig`/
      `SkyIslandConfig`/`NetherStartConfig`/`EndStartConfig`'s private
      `*Defaults()` methods and `FloatingIslandsConfig.lootKitDefaults`;
      `WorldzConfig.kits`; the `kits` root `Setting` declared **first** in
      `WorldzRootSchema.declare()` (load-bearing — pin with an assertion,
      DESIGN §44.4.3) with a merging (not replacing) setter; new
      `ConfigLayout.FILES` entry for `kits.yaml` (unwrapped) +
      `ConfigLayoutTest` branch; `REFERENCE_HEADER` gains the `kits` line.
      Sites still inline — nothing depends on the library yet. Proves: the
      library parses/sanitizes/merges-over-defaults/emits correctly in
      isolation; golden file gains only a leading `kits:` block, every
      preset block byte-unchanged; a user `kits.yaml` entry overrides a
      shipped name or adds a new one without deleting the 14; the setter's
      self-`putAll` is a no-op (assert explicitly, per the 25.6a
      `Setting.group` caution DESIGN §44.4.2 cites).
- [x] 25.8c — **done** (kits library reference applied to 12 tiered starter-kit sites, method deletions, reference-defaults.yaml regenerated) The 12 tiered sites (DESIGN §44.3.5, §44.5 rows 1-12):
      `ChestSchema.KitsSchema` binds `StarterKitSchema.reference(...)`;
      `CaveConfig`/`SkyIslandConfig`/`NetherStartConfig`/`EndStartConfig` kit
      fields become `reference("cave-easy")` etc.; their 12 `*Defaults()`
      methods deleted. Confirm `config/tests/90-nether-start-custom-kit.yaml`
      (inline) still parses **unchanged** — that it does is itself the
      proof inline stays legal. Proves: zero-kit-key configs resolve to
      byte-identical `essentials`/`extras`/`extrasCount` per site (assert
      against pre-25.8 values, not just "it parses"); an unknown kit name
      warns and falls back to that site's own shipped default. Golden file
      regenerated + hand-diffed (25.6f's technique — capture real `toYaml()`
      output from the failing test, don't hand-transcribe).
- [x] 25.8d — **done**. The last 2 sites (DESIGN §44.5 rows 13-14): `oceanIsland
      .starterKit` and `skyIsland.floatingIslands.lootChest.kit`;
      `FloatingIslandsConfig.lootKitDefaults` deleted; `ocean-island-default`
      = `new StarterKitConfig()` with constructor defaults **unchanged**
      (DESIGN §44.3.2 — load-bearing for partial-inline kits). Confirm
      fixtures 34 and 47 (both inline) still parse unchanged. Proves: all 14
      sites converted; a test writing only `extrasCount:` inline still
      inherits the constructor's `essentials`/`extras` (the partial-inline
      case).
- [x] 25.8e Unknown-key gate — **Done (0.3.29):** restored unknown-key checking for named-kit sections (DESIGN §44.6 error-classification table,
      needs 25.8b): `NestedMap` branches added to `SchemaKeyWalker
      .findUnknownKeys`/`recurseIfNested` so `kits.yaml`'s arbitrary *names*
      are tolerated while each kit body's `essentials`/`extras`/
      `extrasCount` keys **are** checked. New `SchemaKeyWalkerFileTest`
      cases: a typo'd `essentails:` inside a named kit is caught; an
      arbitrary kit name is not flagged; `layout.roleOverrides`'s guard
      still holds. `ConfigFixturesTest` stays green, `KNOWN_UNKNOWN_KEYS`
      still empty.
- [x] 25.8f Docs — **Done (0.3.30):** (needs 25.8d): new README "Shared starter kits" subsection
      (the two forms, the 14 shipped names, merge-over-defaults, unknown-name
      fallback) + updated kit rows in the ocean-island/cave/sky-island/
      floating-islands tables; a short `kits:` example added to
      `config/jlt_worldz.example.yaml` (it covers none of these sections
      today — confirm via grep before editing; the full rewrite stays
      25.10's). `config/tests/README.md`/`MANUAL_TESTING.md` stay deferred
      to 25.11, same as every prior 25.7/25.8 sub-step.
- [x] 25.8g Test configs (needs 25.8d) — **the first Phase 25 sub-step that
      warrants one**, since this is the first time the YAML users actually
      write changes (25.1-25.7 correctly shipped none). Two new
      `config/tests/*.yaml`: (i) one kit defined once in `kits.yaml`,
      referenced by two different presets' chests — proves real
      cross-preset sharing in-game; (ii) a misspelled kit name — proves the
      WARN + fallback is visible and the world still generates. Update
      `ConfigFixturesTest.EXPECTED_FIXTURE_COUNT` (103 → 105).
      **[Jason]** in-game acceptance of both.
- [x] 25.8h Close-out: Deviation log (F2's 12-vs-14 miscount and the "named
      kits relocate, don't shrink" correction, DESIGN §44.2; 24.5 un-folded
      back to Phase 24, already reflected in 24.5's rewritten text above —
      just cross-reference it here rather than re-logging); full
      `./gradlew build` all modules; NeoForge brief check (no loader-level
      code expected — `WorldzCommon.java:30-31`'s `load` signature is
      unchanged). **[Jason]** redeploy both Prism instances before
      requesting test (memory: deploy-jar-before-requesting-test). **Done
      (bookkeeping half):** three Deviation log entries added above (F2's
      12-vs-14 miscount, the "named kits relocate, don't shrink" correction,
      and the 24.5 un-fold record commit e3c6a1d's own task text referenced
      but had not actually added), all citing DESIGN §44.1/§44.2/§44.9;
      25.8a-25.8g reverified as accurately checked off. The
      `./gradlew build`/NeoForge-check/Prism-redeploy portion of this
      sub-step was not covered by this bookkeeping check-off; the automated
      portion was handled later by `tester`, while Prism deployment remains
      Jason-only. **TODO 25.8 (all of 25.8a-25.8h) is
      complete.** The outstanding automated gate subsequently passed under
      Temurin 25: full `./gradlew build`, 902 tests green, clean-tree
      verification at that point, and both Fabric and NeoForge artifacts
      present. **[Jason]** Prism redeployment and in-game acceptance of
      configs 102-103 remain outstanding; neither is implied by this
      check-off.
- [x] 25.9 **Strip world absolute width (D9 — the one behavior change).**
      Design and width/portal table in `CONFIG-RESTRUCTURE.md` §5. `width`
      replaces `widthRadiusBlocks`, minimum 1 block; odd widths symmetric about
      Z=0, even widths take the extra block on +Z; End portal and the Nether
      fortress guarantee target the corridor mid-point. **Also owns the
      `strip`/`stripWorld` key-level merge deferred by 25.7** (DESIGN
      §43.3): fold `strip`'s three keys (`widthRadiusBlocks`→`width`,
      `widthMode`, `applyToNether`) into `StripWorldSchema` and unwrap
      `world-types/strip-world.yaml` down to one flat body, since this
      task already has to touch `StripConfig`/`StripWorldCustomization`/
      `ObjectiveSite.narrowForStrip`/the Customize screen for the width
      change — doing the key move here avoids paying for the same
      surgery twice. **Not a rename:**
      `ObjectiveSite.narrowForStrip` returns a Z *radius* applied symmetrically
      by its three callers (`ProgressionGuarantees:70,114`,
      `StackedVillageDeployment:117`) — an even-width corridor is no longer
      symmetric, so it must return a centre plus half-extent. Jason has already
      accepted that structures overflow the corridor at very narrow widths;
      no clamping work is in scope. Separate commit, own test configs.
      **Done (0.3.32):** merged all four former `StripConfig` fields
      (`enabled`, `widthRadiusBlocks`→absolute `width`, `widthMode`,
      `applyToNether`) into `StripWorldConfig`/`StripWorldSchema`; deleted
      `StripConfig`/`StripSchema`; and unwrapped
      `world-types/strip-world.yaml` to one `stripWorld:` owner. Width now
      defaults to 65 and floors at 1: `minZ = -(width - 1) / 2`,
      `maxZ = width / 2`, giving odd widths symmetric bounds and even widths
      their extra +Z column. The old config key and old persisted strip-codec
      radius shape are intentionally unsupported under D1/new-worlds-only.
      `ObjectiveSite.ZBounds(center, negativeExtent, positiveExtent)` now
      carries the asymmetric corridor through the End-portal, Nether-blaze,
      and stacked-village placement paths instead of collapsing it back to a
      radius.

      Two review findings and one existing Customize bug were fixed in scope,
      not deferred: explicit generator hints keep generic `worldz`'s
      `stripWorld.enabled` opt-in from leaking into typed presets while the
      dedicated `strip_world` preset remains always active; block/chunk UI
      display is lossless for non-chunk-aligned widths (for example 65 blocks
      displays as 4.0625 chunks) and review coverage protects typed-preset
      isolation plus unit toggling; reopening Customize now reconstructs the
      Nether generator from the edited `applyToNether` state instead of
      retaining the stale entry-time generator. Fixtures 26-29a were migrated
      to the merged shape. Added sequenced manual configs 104-107 and raised
      the fixture count from 105 to 109. Final gate: 63 suites / 922 tests,
      Fabric and NeoForge artifacts green; two task-level review passes, with
      no findings on the second. **[Jason] manual acceptance of configs
      104-107 remains outstanding** (also recorded under 25.12 and Questions
      for Jason).
- [ ] 25.10 Documentation (D3's second half). Generate README's settings tables
      from the schema; add the completeness test covering every leaf setting
      (it would have caught F6 — 12 of 25 sections undocumented, and
      `README.md:71` claims the example file "documents every setting", which
      has never been true). Document the **live-vs-baked scope distinction**
      for the first time (F3): hazards are re-read from config and change
      existing worlds; borders, exteriors and preset sections are baked into
      the save at creation and do nothing to an existing world.
- [ ] 25.11 **Scope reduced by DESIGN §43.8 (2026-07-28):** 25.6 migrated
      every renamed key across the then-103 `config/tests/*.yaml` fixtures
      (25.6a-h), 25.8 added two (105 total), and 25.9 migrated every affected
      strip fixture plus added four more (109 total). 25.7's file split moves
      no keys and keeps
      fixtures single-file (`config/jlt_worldz/all.yaml`-shaped, per the
      kept bundle) — so there is no fixture content left to migrate here.
      There is therefore no fixture content left to migrate here: 25.9's
      `strip.widthRadiusBlocks`→`stripWorld.width` and key-level merge landed
      with 25.9 itself, under the same "no alias fallback" reasoning as the
      earlier migrations. What remains is a prose-only pass over
      `config/tests/README.md`/`MANUAL_TESTING.md` for anything 25.7d/25.9
      did not already cover. Gate: `ConfigFixturesTest` (already exists,
      25.6a) stays green throughout at 109 fixtures.
- [ ] 25.12 Full multiloader build green, both Prism instances redeployed, then
      close the phase. **[Jason] acceptance:** a hand-commented config survives
      a launch intact (25.4); a stacked world with no `worldSizeChunks: 0`
      opt-out behaves as configured (25.5); and a 1-block-wide strip world
      generates with the End portal on the corridor mid-point (25.9).
      **[Jason] Phase 25.9 sequence still outstanding:** config 104
      (one-block `Z=0` corridor, usable midpoint portal, accepted overflow);
      105 (four columns exactly `Z=-1..2`, midpoint portal); 106 (two columns
      `Z=0..1` in Overworld and Nether, midpoint blaze fallback); and 107
      (lossless Blocks/Chunks display plus width/mode/Apply-to-Nether
      persistence after closing and reopening Customize).

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

- **2026-07-28 — Phase 25.9 manual acceptance outstanding (not a design
  question):** run sequenced configs 104-107 on fresh "Worldz: Strip World"
  worlds. Verify the one-block and even-width ranges, End-portal and
  Nether-blaze midpoint placement with accepted narrow-corridor overflow,
  and Customize's lossless Blocks/Chunks display plus width/mode/
  Apply-to-Nether persistence across close/reopen. Automated coverage is
  green (63 suites / 922 tests; both loader artifacts), but this in-game
  acceptance remains **[Jason]** and must not be marked complete by an agent.

- **2026-07-28 — Phase 25.6: five documented settings the mod has never
  read. ANSWERED same day: wire them up.** `skyIsland.exclusionZoneEnabled`/
  `exclusionZoneRadiusBlocks` and `flat`/`skyIsland`'s `undergroundBiome`/
  `undergroundBelowSurfaceBlocks` are documented in README, set by
  `config/tests/57`, `97` and `98`, and silently ignored by the parse layer
  (verified: no `Setting` declares them, no `underground*` key exists in
  `reference-defaults.yaml`). Jason chose to wire them up as part of 25.6
  rather than delete the keys — the only way 57/97/98 test what they claim,
  and it matches what README already promises. Implement in isolated commits
  (25.6d for `skyIsland.exclusionZone`, 25.6g for `underground`) so each can
  be accepted or reverted independently of the rename. Supersedes the
  2026-07-27 and 2026-07-28 Deviation-log flags on this same gap. DESIGN
  §42.7 has the full detail.
  **Fully resolved as of 25.6g.** `skyIsland.exclusionZone` was wired at
  25.6d — into `SkyIslandSchema` via the shared `ExclusionZoneSchema`,
  config 57 migrated and moved off `ConfigFixturesTest.KNOWN_UNKNOWN_KEYS`,
  3 new `WorldzConfigTest` regression tests confirm real values thread
  through (not just that the keys parse). `underground` (`flat`/`skyIsland`)
  was wired at 25.6g — the shared `UndergroundSchema<S>`, reused at both
  `FlatSchema` and `SkyIslandSchema`, configs 97/98 migrated to
  `underground: {biome, belowSurface}` and moved off `KNOWN_UNKNOWN_KEYS`
  (now empty), 7 new `WorldzConfigTest` regression tests confirm the same.
  All five originally-flagged settings are now genuinely declared and
  honored by the parse layer; see 25.6g's own done-note for the full detail.

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

- 2026-07-28 (Phase 25.8h close-out) — **`CONFIG-RESTRUCTURE.md` F2 miscounted
  the shared starter-kit blocks as 12; the real count is 14** (DESIGN §44.2).
  F2 says "145 of the 384 generated lines are 12 near-identical
  `easyKit`/`mediumKit`/`hardKit`/`starterKit`/`lootKit` blocks" — its own list
  of shapes includes `starterKit` (`oceanIsland.starterKit`) and `lootKit`
  (`skyIsland.floatingIslands.lootChest.kit`), but its total only counts the
  4×3 tiered blocks (`cave`/`skyIsland`/`netherStart`/`endStart` ×
  `easy`/`medium`/`hard`), silently dropping those same two sites from the
  sum. `CONFIG-RESTRUCTURE.md` §3's own worked example already assumes the
  13th site is named (`starterKit: ocean-island-default`), so §3 and F2
  contradict each other; §44.2 resolves in §3's favor — all 14 sites are
  pre-named (44.5), and 25.8b-d shipped all 14 as library entries, not 12.
  No behavior impact; documentation-accuracy only.

- 2026-07-28 (Phase 25.8h close-out) — **"Named kits relocate, don't shrink":
  D6 does not remove ~38% of the config's bulk, contrary to what F2/D6
  originally implied** (DESIGN §44.2, §44.9 Q1). Compared field by field, none
  of the 14 kit blocks share contents with another (closest pair,
  `netherStart.hard`/`endStart.hard`, shares `essentials` but differs on
  `extras`) — so nothing merges away. The ~146 lines of item lists do not
  disappear; they *move* into `kits.yaml`. Measured across the sub-steps: the
  generated `jlt_worldz.reference.yaml` started at 428 lines (25.7), grew past
  that once 25.8b added the `kits:` library block while the 14 sites were
  still inline, then shrank back down as 25.8c/25.8d converted each site to a
  one-line reference — landing at 443 lines once 25.8d finished, matching
  §44.9 Q1's ~443 estimate almost exactly. **Jason confirmed proceeding
  anyway on 2026-07-28** (DESIGN §44.9 Q1): the real value is locality
  (`cave.yaml` drops ~41→~14 lines), reuse across presets, and one-line kit
  swapping — not size reduction — and the design went ahead unchanged,
  including emitting each kit's full contents inline in the generated
  reference (not just names) so it stays a valid standalone `all.yaml`
  bundle.

- 2026-07-28 (Phase 25.8h close-out) — **Old TODO 24.5 (capsule-config-field
  consolidation) is un-folded from Phase 25 and returned to Phase 24, merged
  with 24.4** (DESIGN §44.1, confirmed by Jason 2026-07-28, DESIGN §44.9 Q2).
  `CONFIG-RESTRUCTURE.md` §7 justified folding 24.5 into 25.8 with "overlaps
  D6", but the two touch disjoint file sets: the config-layer duplication
  24.5 named no longer exists (`NetherStartConfig.capsule`/
  `EndStartConfig.capsule` already share one `StarterCapsuleConfig`/
  `StarterCapsuleSchema` since 25.2a/25.6e); the remaining duplication is
  entirely in `logic/` (`NetherStartPlan`/`EndStartPlan`'s four capsule
  record components, nine bound constants, four validation blocks, and
  `centeredCapsuleOffsets`) and is codec-persisted but consolidatable with
  zero shape change to the saved NBT (DESIGN §44.1.3). 24.4 ("shared capsule
  *builder*") is 24.5's real pair — doing 24.5 now would mean 24.4 re-opens
  the same six files later — and Phase 25's own "no world-save-codec change"
  constraint makes declining the fold-in the conservative direction anyway.
  TODO 24.5's text above (Phase 24) was rewritten with these findings at the
  point of the fold (commit e3c6a1d, 2026-07-28) so Phase 24 does not have to
  re-derive them; this entry is the Deviation-log record that commit's own
  25.8h task text called for but did not yet add.

- 2026-07-28 (Phase 25.7e close-out) — **`strip`/`stripWorld` merged at file
  level only by 25.7; the key-level merge is deferred to TODO 25.9** (DESIGN
  §43.3). `world-types/strip-world.yaml` is now the one *wrapped* world-type
  file, holding `strip:` and `stripWorld:` as two still-separate sections with
  a header comment noting they merge fully at 25.9. Folding `strip`'s three
  keys (`widthRadiusBlocks`, `widthMode`, `applyToNether`) into
  `StripWorldSchema` and unwrapping the file down to one flat body was
  deliberately left for 25.9 rather than done now: `StripWorldSchema` (a
  `SchemaSection<StripWorldConfig>`) reaching fields that live on
  `StripConfig` requires a POJO move that drags
  `EnvelopedChunkGenerator.java:1008-1010`, `StripWorldCustomization:101-103,190`
  and the strip Customize screen in — exactly the surgery 25.9 already has to
  do for the absolute-width behavior change (D9,
  `ObjectiveSite.narrowForStrip` returning a centre plus half-extent), so the
  key move rides along there for free instead of being paid for twice. Same
  treatment DESIGN §42.1 gave the `strip.widthRadiusBlocks` rename deferral
  (logged at 25.6c above). **Accepted consequence, documentation-only:**
  `strip` is still read as a fallback for *any* world type
  (`EnvelopedChunkGenerator.java:1008-1010`), so a generic-`worldz` corridor
  is configured out of a file named `strip-world.yaml` until 25.9 lands — no
  behavior change (same key → same POJO field → same read path).

- 2026-07-28 (Phase 25.7e close-out) — **`kits.yaml` is not created by 25.7;
  deferred whole to TODO 25.8** (DESIGN §43.6). `ConfigLayout`'s completeness
  invariant (`ConfigLayoutTest`, 25.7a) requires the files' owned root keys to
  *exactly partition* `WorldzRootSchema.declare()`'s keys, with no
  duplicates/gaps. There is no `kits` root key until 25.8 adds the `kits`
  root section, so a placeholder `kits.yaml` entry owning nothing would fail
  that very gate on day one — weakening the gate to accommodate a placeholder
  was the wrong trade. 25.8 adds `kits.yaml` as a new `ConfigFile` entry
  alongside the `kits` schema it introduces, per the same precedent 25.6 set
  of introducing each shared schema class in the sub-step that first needed
  it (`ChestSchema` at 25.6d, `UndergroundSchema` at 25.6g). `ConfigLayout`
  carries a one-line comment reserving the name and pointing at 25.8 so the
  intended path is discoverable in the meantime.

- 2026-07-28 (Phase 25.7a, doc-staleness note, not a behavior deviation) —
  **DESIGN §43.4.1's claim that "only 3 schema classes ever override
  [`Applicability`]" (i.e. call `.live()`/`.preset(...)` rather than leaving
  `Setting.Builder`'s default `Applicability.worldDefault()`) was already
  stale at design time.** Confirmed via `git show 8b932cd` (the commit at the
  point of the §43 design pass): 6 schema classes already did —
  `DeepFlatSchema`, `FlatSchema`, `ForeverNightSchema`, `RisingLavaSchema`,
  `StackedSchema`, `StructureDistanceSchema` — not 3. No behavior impact:
  §43.4.1's actual conclusion ("today's metadata cannot carry the [26-key
  file] mapping, so `ConfigLayout` is declarative and hand-written") holds
  regardless of whether the pre-existing-override count is 3 or 6, and
  25.7a's correction of the 26 root-level settings' applicability
  (`WorldzRootSchema.java`) didn't depend on the count either. Logged as a
  one-line note for a future reader relying on §43.4.1's number, not as a
  Deviation-log item requiring any follow-up.

- 2026-07-28 (Phase 25.6h close-out) — **F1's two tables (`CONFIG-RESTRUCTURE.md`)
  needed four corrections once re-verified against the current tree, all
  already worked out and applied during 25.6a-25.6g; logged here as 25.6h's
  own close-out task requires, in one place, per DESIGN §42.1:
  1. **Within-class table: 13 confirmed, not 14.** All 14 of F1's flat field
     sets still existed with matching names/emit order, but row 12
     (`StripConfig.widthRadiusBlocks`/`widthMode`) was deferred whole to
     TODO 25.9 rather than nested now (see the 25.6c entry above) — so only
     13 of the 14 rows were actually restructured by 25.6, not 14.
  2. **Cross-class table: 2 confirmed, 2 materially wrong at measurement
     time.** `naturalBiomes` (`ChaosBiomes`/`SingleBiome`/`StripBands` + root)
     and `chest` (`Cave`/`EndStart`/`NetherStart`/`SkyIsland`) were both
     confirmed exactly as F1 described. `exclusionZone` was **3 live, not
     4** — `SkyIslandConfig.exclusionZoneEnabled`/`exclusionZoneRadiusBlocks`
     were unread/unmapped dead config at the time F1 measured, not a fourth
     real owner (`ChunkIsland`/`FloatingIslands`/`OceanIsland` were the
     genuine 3). `underground` was **0 live, not 2** — neither `FlatSchema`
     nor `SkyIslandSchema` declared the pair at measurement time; both were
     documented in `README.md` and exercised by fixtures 97/98 while being
     silently ineffective. Both gaps are now fully resolved: `skyIsland
     .exclusionZone` was wired into the schema at 25.6d, and `underground`
     (a real, shared `UndergroundSchema<S>`) was wired at 25.6g — both
     `exclusionZone` and `underground` are genuine 4-owner and 2-owner live
     shapes respectively as of those sub-steps.
  3. **Three shapes F1 missed entirely**, cheaper to fix during 25.6 than to
     defer: the root's 5 `starter`-prefixed keys (`starterBiome`/
     `starterRadiusBlocks`/`ensureStarterLand`/`starterLandTransitionBlocks`/
     `starterLandFoundationDepthBlocks` → `starter: {biome, radius, land:
     {enabled, transition, foundationDepth}}`), the same `starter: {biome,
     radius}` pair (without `land`) on `SingleBiomeConfig`/`ChaosBiomesConfig`,
     and `DeepFlatConfig`'s `riversEnabled`/`riverExclusionRadiusBlocks` →
     `rivers: {enabled, exclusionRadius}`. F1 missed the first two because
     its inventory walked config *classes* and the root's scalars belong to
     none; it missed `DeepFlat` outright. All three were nested during
     25.6b/25.6f as documented additions to F1, not F1 rewrites.
  Net, as DESIGN §42.1 itself concludes: 13 within-class nests + 3 live
  cross-class shared shapes + 3 added shapes, plus the §2 suffix sweep, with
  `strip` the sole deferred exception — all delivered across 25.6a-25.6h.

- 2026-07-28 (Phase 25.6c, F1 deviation re-logged at the point it bites) —
  **`strip.widthRadiusBlocks`/`strip.widthMode` were deliberately left
  untouched by 25.6c**, per DESIGN §42.1's own recommendation: F1 itself
  annotates that row "see §5 — width becomes absolute", and renaming
  `widthRadiusBlocks` → `widthRadius` now, then → `width` with new (absolute,
  not radius) semantics in TODO 25.9, would cost two fixture migrations of
  the same six strip configs and leave a one-commit window where the key
  reads like an absolute width while still behaving as a radius. `strip:` is
  untouched in every `*Schema.java` file and every `config/tests/*.yaml`
  fixture touched this sub-step (confirmed via `git diff --name-only` —
  no `Strip*.java` file appears in the 25.6c diff). The whole `strip.width*`
  question is handed to TODO 25.9 unchanged. (25.6h's own close-out task
  also names this deviation for its final log pass; this entry exists so it
  isn't silently undocumented in the meantime, per the 25.6c task's own
  instruction to log it here if not already present.)

- 2026-07-28 (Phase 25.6a, doc count found stale) — **`config/tests/*.yaml`'s
  real count is 103, not the "104" repeated throughout `TODO.md` (this
  phase's own 25.6/25.11/25.12), `DESIGN.md` §42, `CONFIG-RESTRUCTURE.md` and
  `GOALS.md`.** Verified empirically (`ls config/tests/*.yaml`): 101 numbered
  files, two of which (27, 29) also have an "a"-suffixed sibling (27a, 29a) —
  103 total, not 104. `git log --diff-filter=D -- config/tests/*.yaml` shows
  no fixture has ever been deleted, so this looks like a stale prose count
  rather than a fixture that used to exist and was lost. `ConfigFixturesTest`
  (25.6a) hardcodes `EXPECTED_FIXTURE_COUNT = 103` — the real, current,
  verified count — with a comment pointing at this entry. Flagged rather than
  silently reconciling the doc references (`TODO.md`'s own 25.6/25.11/25.12
  wording, `DESIGN.md` §42, `GOALS.md`, `CONFIG-RESTRUCTURE.md`): whether "104"
  should be corrected to "103" everywhere, or whether a 104th fixture was
  always meant to exist and never got written, is a call for Jason/
  `project-manager`, not something to guess at mid-task.

- 2026-07-28 (Phase 25.5, scope found narrower than described) — **Only
  `StackedConfig`'s `worldSizeChunks` sentinel was actually convertible to a
  presence-based check; the other 4 sentinels D5/F4 named
  (`ExteriorConfig.boundaryRadiusBlocks`, `BorderConfig.resizeRateBlocks`/
  `resizeRateDays`, `FlatConfig`/`SkyIslandConfig`'s `undergroundBiome`/
  `undergroundBelowSurfaceBlocks`) were left untouched, each for a distinct,
  verified reason — not guessed, checked against real code/tests:
  1. **`boundaryRadiusBlocks: 0` and `resizeRateBlocks`/`resizeRateDays: 0`
     both have a default that already equals their own sentinel value** (0),
     unlike `worldSizeChunks` (default 4, sentinel 0). `value != 0` and
     "explicitly present" are therefore logically identical for every
     YAML-parsed config — presence tracking would add nothing. Worse,
     `ExteriorConfig`/`BorderConfig`/`DimensionEnvelope` are routinely
     constructed directly in Java (client Customize-screen editors,
     `StackedConfig`'s own derived `ExteriorConfig`, most of the existing
     test suite) with no backing `WorldzConfig` to call `.present()` on at
     all — `ExteriorPlanTest.configAutoBoundaryUsesLargestScheduledBorderRadius`
     is one such test, and it would break if `DimensionEnvelope.fromConfig`
     were made presence-gated. Confirmed a literal `boundaryRadiusBlocks: 0`
     genuinely cannot be "honored" as a real value either way: `ExteriorPlan
     .DimensionEnvelope`'s own compact constructor throws for any non-`NORMAL`
     mode paired with a zero boundary, so the derive-from-border fallback is
     the only safe behavior regardless of presence.
  2. **`undergroundBiome`/`undergroundBelowSurfaceBlocks` are not wired into
     the schema at all** — `FlatSchema`/`SkyIslandSchema`'s own class Javadoc
     already documents this as a pre-existing gap predating 25.2 (`readOne`
     never runs for these two fields, so `WorldzConfig.present(...)` would
     always report `false` for them regardless of what a user writes). They
     are only ever set directly in Java (`FlatCustomization.flatPlan()`
     hardcodes `"", 10`; `FlatPlan.fromConfig`/`SkyIslandPlan.fromConfig` read
     the untouched class defaults). TODO 25.6 already plans a real
     `underground` shared shape (shared by `flat`/`skyIsland`) as part of its
     own key restructure — that is the right place to wire these fields into
     the schema for the first time, at which point presence tracking will
     apply naturally. Converting them now, before that wiring exists, isn't
     possible without doing 25.6's own work early.

     **Resolved 2026-07-28 (TODO 25.6g):** `underground` is now a real,
     wired `Setting.group` (`UndergroundSchema<S>`, shared by `FlatSchema`/
     `SkyIslandSchema`) — `WorldzConfig.present(...)` reports accurately for
     `flat.underground.biome`/`.belowSurface` and the `skyIsland` equivalents
     going forward, the same as every other presence-tracked setting. No
     further action needed here; point 2's gap no longer exists.
  Flagged for `project-manager`/Jason: is leaving these 4 as-is acceptable
  for 25.5, or should the TODO 25.5 item be split so 25.6 explicitly inherits
  "wire + convert `underground`"? No code or config/tests change needed for
  `boundaryRadiusBlocks`/`resizeRate*` either way — recommend closing them out
  of D5's scope rather than deferring, since converting them would only add
  risk (see point 1) for zero behavioral benefit. **`underground` itself is
  resolved as of 25.6g (see above); only `boundaryRadiusBlocks`/`resizeRate*`
  remain open questions from this entry.**

- 2026-07-27 (Phase 25.2e, found not fixed) — **`SkyIslandConfig`'s own
  `exclusionZoneEnabled`/`exclusionZoneRadiusBlocks` (defaults `true`/`128`,
  distinct from the nested `floatingIslands` exclusion zone) are dead from
  config's perspective**: `readSkyIslandConfig` never reads them and
  `skyIslandMap` never writes them (both confirmed by reading the actual
  methods), yet `skyIslandSummary` references them and `SkyIslandPlan`/
  `SkyIslandCustomization` genuinely consume them at their hardcoded
  defaults. Compare `oceanIsland`/`chunkIsland`/nested `floatingIslands`,
  whose own exclusion-zone pairs all round-trip correctly — this is an
  isolated gap on `skyIsland`'s own top-level pair, not a pattern. Not
  documented in README's `skyIsland` example/table either (only
  `floatingIslands`' copy is). **25.2e reproduces this exactly** (behavior-
  preserving is this sub-step's hard constraint) rather than silently fixing
  it. Flagged for Jason: fix now as a quick out-of-band bugfix (wire the two
  missing `containsKey` blocks into `readSkyIslandConfig`/`skyIslandMap`), or
  let 25.7 (file split, `world-types/sky-island.yaml`) pick it up as part of
  that section's own rewrite? Either way it's a real functional gap, not
  cosmetic — today's users cannot configure a documented-as-real setting.

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
