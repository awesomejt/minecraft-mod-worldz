# TODO — jlt_worldz challenge-world plan

**Requirements source:** `GOALS.md` (Jason's use cases 01–24). **Technical
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
- [ ] 1.2 Remove the `MIXED` and `LAND_ONLY` grid layout modes and everything
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
- [ ] 1.3 Route the real world seed into Worldz sampling (DESIGN §20.4):
      capture it at generation time (it is available to the generator at
      runtime — e.g. via the level/`ChunkMap` construction the existing
      `ChunkMapMixin` already intercepts) and replace the random-per-world
      sampling seed, so identical seed strings reproduce identical Worldz
      decisions (GOALS 10, 12, 16, 08, 09). Verify the exact hook against
      26.2 sources first; JUnit-cover the plumbing that is pure.
- [ ] 1.4 Config hygiene (GOALS §Configuration): stop rewriting
      `config/jlt_worldz.yaml` when it is absent or holds pure defaults;
      generate the documented example (comment-based YAML, not `_docs` keys)
      as the way users discover options. Drop the legacy `.json` migration
      path (new-worlds-only policy makes it dead weight).
- [ ] 1.5 Prism test-loop automation (GOALS §Automation): a Gradle task (e.g.
      `./gradlew deployToPrism -Pinstance=...`) that copies the built Fabric
      jar into the Prism instance's `mods/`, replacing any older jlt_worldz
      jar. Ask Jason for the instance path; put it in a git-ignored local
      properties file. Document in MANUAL_TESTING.md.
- [ ] 1.6 Bump to 0.2.0 (breaking removal), align the metadata-test contract,
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
- [ ] 2.2 Implement the `single_biome` world type: one land biome everywhere
      (use case 10), optional different starter biome around spawn (11),
      optional seed-chosen natural starter placement via the existing
      `preferred_natural_biome` strategy + origin recentering (12). Small
      per-type Customize screen — only this type's options, per-type
      defaults, scrolling per GOALS.
- [ ] 2.3 Structures, caves, and randomness follow the seed (10; depends on
      1.3). JUnit where pure; the rest is 2.7's manual pass.
- [ ] 2.4 Per-world snapshot: on world creation, write a readable commented
      YAML of the resolved settings into the world folder. It is a record,
      not a control file (new-worlds-only; README documents this).
- [ ] 2.5 Test configs `config/tests/` for 10, 11, 12 + a vanilla-limited
      baseline; update MANUAL_TESTING.md scenario table.
- [ ] 2.6 README restructure begins: challenge-type-first presentation,
      **new worlds only** restriction, single-biome section.
- [ ] 2.7 [Jason] Acceptance: 10 (one biome, normal terrain/caves/structures),
      11 (starter differs), 12 (same seed twice → same placement; spawn in
      the chosen biome).

## Phase 3 — Single-biome variations: natural rivers and oceans (GOALS 13–14)

- [ ] 3.1 Design + implement vanilla pass-through selection (DESIGN §20.5):
      where vanilla's own biome would be a river (13) or river/ocean family
      (14), keep vanilla's choice; otherwise substitute the single biome.
      Terrain is untouched, so coastlines/rivers are exactly natural — no
      grid, no height blending. Config/Customize toggles: `allowRivers`,
      `allowOceans`.
- [ ] 3.2 Test configs for 13 and 14; docs; **[Jason]** acceptance.

## Phase 4 — World limits, expanding/collapsing (GOALS 17–20)

Mostly verification of already-built borders/exteriors against GOALS wording,
composed with the new world types; plus the one real gap (the End).

- [ ] 4.1 Audit existing border + exterior behavior against 17–20: blocks
      and chunks as units (add chunk input if missing), invisible-wall
      (border) vs hard void (exterior) beyond the size (18), expansion rate +
      initial delay (19), contraction with larger default delay, minimum
      size, and center-safe start (20). Fix deltas only.
- [ ] 4.2 The End gap (17): option to carry limits into the End with a
      minimum-size override so the dragon fight stays winnable; verify the
      existing Nether carry-over and blaze/End-portal guarantees still hold
      under the world-type restructure.
- [ ] 4.3 Limits must compose with every world type (a shared module section
      in each type's config/Customize), not only the `limited` type.
- [ ] 4.4 Test configs (static small, expanding, collapsing, End carry-over);
      docs; **[Jason]** acceptance.

## Phase 5 — Ocean island challenge, core (GOALS 01, 04)

- [ ] 5.1 Design pass (DESIGN §20 extension): natural-looking island shaping
      (noise-perturbed radius over the existing starter-land profile — not a
      disc), a dedicated narrow shore width (beach/stony-shore ring; fixes
      the logged beach-width gap properly), shallow→deep ocean depth gradient
      with depth-appropriate ocean biomes (all ocean biomes available), and
      the shared **exclusion zone** module (center = origin, default 2000
      blocks; reused by 04, 07, 08, 24). **Commit** design first.
- [ ] 5.2 Implement the `ocean_island` world type: configurable island size
      (1 chunk → huge), chosen island biome, endless ocean via the terrain
      cap, underground structures intact, unchanged Nether/End, beatable
      (progression guarantees). Use case 01.
- [ ] 5.3 Distant natural islands (04): release the ocean cap beyond the
      exclusion radius so the seed's natural terrain resumes far away.
- [ ] 5.4 Test configs (tiny/default/huge island, 04 variant); docs;
      **[Jason]** acceptance including "does the island read as natural".

## Phase 6 — Ocean island extras (GOALS 03, 02)

- [ ] 6.1 Starter-chest infrastructure (shared with sky phases): loot presets
      + YAML-configurable contents, placed at spawn. Then use case 03:
      no-land ocean world, spawn on/next to a chest boat with essentials
      (lily pad, dirt, grass block, saplings) + configurable randoms.
- [ ] 6.2 Natural island by seed (02): search the real seed's unmodified
      climate/terrain for a small natural island, set world spawn/origin
      there, replace everything else with ocean beyond it. Reuses the 16.3
      spawn-search + recentering machinery. This is the hardest ocean item —
      keep it last and time-boxed; if the search proves unreliable, park it
      with findings in DESIGN and move on.
- [ ] 6.3 Test configs; docs; **[Jason]** acceptance.

## Phase 7 — Sky island challenge (GOALS 05–07)

- [ ] 7.1 Design pass: true floating island (bounded below — not the current
      full-depth terrain plug), default spawn platform Y ≥ 64 (slime rule),
      necessities-chest presets easy/medium/hard informed by island biome
      (water bucket vs cauldron etc.), all beatable; stronghold/structure
      option reusing progression guarantees (05). Nether/End sky-island
      options with structure retention (06). Villages beyond the exclusion
      zone (07).
- [ ] 7.2 Implement `sky_island` world type per the design; test configs;
      docs; **[Jason]** acceptance per sub-case.

## Phase 8 — Floating resource islands (GOALS 08)

- [ ] 8.1 Seed-driven scattered floating islands with varied sizes/resources,
      far enough apart to force serious bridging; option replaces pure void
      between them. Design first (placement sampling can reuse the pure
      hash-cell approach — uniform role, so no coastline-class defects), then
      implement, test configs, **[Jason]** acceptance.

## Phase 9 — Sky chunk challenge (GOALS 09)

- [ ] 9.1 Design + implement chunk-grid islands cut from the seed's natural
      chunks: full-chunk vs top-N-blocks-deep option, guarantee one generated
      chunk contains a portal room, normal-vs-chunk-island Nether/End
      options. Test configs; **[Jason]** acceptance.

## Phase 10 — Flat worlds (GOALS 15, 16, 22)

- [ ] 10.1 Design pass against DESIGN §19's verified `FlatLevelSource`
      research: layer editor (arbitrary block layers/thicknesses, presets,
      text import/export), optional bedrock floor, structure toggles incl.
      the trial-chambers placement spike, spawn-Y/slime option (15);
      deep-flat variant with seeded caves/cave biomes/optional far-off rivers
      (16 — likely noise-based underground below a flat surface, spike
      needed); underground structures buried at natural depth rather than
      floating (22).
- [ ] 10.2 Implement `flat` world type(s) per design; test configs; docs;
      **[Jason]** acceptance.

## Phase 11 — Structure options wrap-up (GOALS 21, 23, 24)

- [ ] 11.1 Verify natural placement remains the default everywhere (21).
- [ ] 11.2 Generalize the exclusion-zone module into per-structure-family
      "minimum distance from spawn" options (default 2000 blocks) usable by
      any world type (24).
- [ ] 11.3 Stretch, only if Jason still wants it after 1–10: floating
      "Pandora" structure islands (23). Design spike first; park if cost is
      out of proportion.
- [ ] 11.4 Test configs; docs; **[Jason]** acceptance.

## Phase 12 — Wrap-up and release

- [ ] 12.1 Full README/config-reference/example rewrite in challenge-first
      terms; MANUAL_TESTING.md final scenario tables; MEMORY.md tidy.
- [ ] 12.2 Final clean multiloader build, artifact inspection, version bump.
      Publishing decisions remain Jason's.

---

## Carried-over open risks (from MEMORY.md)

- Dummy-RandomState fix (0.1.15) unverified in-game → Phase 1.1.
- Worldz14 orange/glitchy terrain unexplained → Phase 1.1.
- Straight coastlines + beach width → removed with the grid modes (1.2);
  ocean-island shore quality is redesigned properly in 5.1.
- Layout sampling seed not the real world seed → Phase 1.3.

## Questions for Jason (running list)

(Add here when blocked; don't guess on gameplay/scope questions.)

## Deviation log

(Record every departure from DESIGN.md/GOALS.md here: what, where, why.)
