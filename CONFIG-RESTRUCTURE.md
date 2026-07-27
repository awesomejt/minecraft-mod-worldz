# Config restructure — research findings and proposal

**Status: PROPOSAL, awaiting Jason's review.** Nothing here is implemented.
Written 2026-07-27 in an autonomous research session at Jason's request
("restructure the configuration files for execution in the next few
iterations"). Once Jason answers §8's open questions, the approved parts fold
into `DESIGN.md` as a new section and the TODO items in `TODO.md` Phase 25
become live.

Jason raised three problems. This document measures each against the real
codebase, proposes a target shape, and records the decisions that need his
answer before anyone writes code.

> 1. The configuration has limited depth so properties under each main object
>    often repeat parts of similar property or setting names. I want a more
>    nested structure overall with relatively minimal overlap in property names.
> 2. Since there are a lot of world types, we need to take a solid review to see
>    if the current structure makes sense. Should properties be put into their
>    own yaml configuration files since many world type defaults actually
>    conflict with each other (or rely on empty/zero values to trigger default
>    behavior)?
> 3. Better organized documentation in property/setting values.

---

## 1. Method

Everything in §2 was measured against the working tree at `97330ed` + the
uncommitted 17.8 change, not estimated:

- Field inventories extracted from all 26 classes in
  `common/src/main/java/media/jlt/minecraft/mods/worldz/config/`.
- The real default config dumped by calling `new WorldzConfig().toYaml()` from
  a throwaway JUnit test (deleted afterwards) — this is byte-for-byte what the
  mod writes to a user's disk.
- Call-site tracing for every shared section to establish which presets
  actually read it.
- `config/tests/*.yaml` (104 files) scanned for opt-out boilerplate.

---

## 2. Findings

### F1 — The file is flat and repetitive, exactly as Jason described

165 leaf scalar settings across 25 top-level sections, in a 384-line generated
file. Fields that are conceptually one nested object are spelled as a shared
prefix repeated across siblings. Every one of these is a mechanical
collapse candidate:

| Class | Flat fields today | Nests to |
|---|---|---|
| `BorderConfig` | `resizeDays`, `resizeDelayDays`, `resizeRateBlocks`, `resizeRateDays`, `resizeStyle` | `resize: {days, delayDays, rateBlocks, rateDays, style}` |
| `CaveConfig` | `sealedSurface`, `sealedSurfaceY`, `sealedSurfaceBlock`, `sealedSurfaceThicknessBlocks` | `sealedSurface: {enabled, y, block, thicknessBlocks}` |
| `CaveConfig` | `cavernEnabled`, `cavernRadiusBlocks`, `cavernHeightBlocks` | `cavern: {enabled, radiusBlocks, heightBlocks}` |
| `CaveConfig` | `chestEnabled`, `chestTier` | `chest: {enabled, tier}` |
| `OceanIslandConfig` | `oceanShallowWidthBlocks`, `oceanDeepenWidthBlocks`, `oceanShallowDepthBlocks`, `oceanDeepDepthBlocks`, `oceanRegionScaleBlocks` | `ocean: {shallowWidthBlocks, deepenWidthBlocks, shallowDepthBlocks, deepDepthBlocks, regionScaleBlocks}` |
| `OceanIslandConfig` | `islandSource`, `islandBiome` | `island: {source, biome}` |
| `ChunkIslandConfig` | `topOnly`, `topOnlyDepthBlocks` | `topOnly: {enabled, depthBlocks}` |
| `ChunkIslandConfig` | `applyToNether`, `applyToEnd` | `applyTo: {nether, end}` |
| `FloatingIslandsConfig` | `minRadiusBlocks`, `maxRadiusBlocks` | `radiusBlocks: {min, max}` |
| `FloatingIslandsConfig` | `oreDepositsEnabled`, `oreFeatureIds` | `oreDeposits: {enabled, featureIds}` |
| `FloatingIslandsConfig` | `lootChestEnabled`, `lootKit` | `lootChest: {enabled, kit}` |
| `StripConfig` | `widthRadiusBlocks`, `widthMode` | `width: {radiusBlocks, mode}` |
| `RisingLavaConfig` | `rateBlocks`, `rateDays` | `rate: {blocks, days}` |
| `StarterCapsuleConfig` | `lightSource`, `lightSpacingBlocks` | `light: {source, spacingBlocks}` |

Plus four patterns duplicated *across* classes rather than within one:

| Repeated shape | Appears in | Collapses to |
|---|---|---|
| `exclusionZoneEnabled` + `exclusionZoneRadiusBlocks` | `ChunkIsland`, `FloatingIslands`, `OceanIsland`, `SkyIsland` | one shared `exclusionZone: {enabled, radiusBlocks}` |
| `allowRivers` + `allowOceans` + `allowBeaches` | `ChaosBiomes`, `SingleBiome`, `StripBands`, + top level (2 of 3) | one shared `naturalBiomes: {rivers, oceans, beaches}` |
| `undergroundBiome` + `undergroundBelowSurfaceBlocks` | `Flat`, `SkyIsland` | one shared `underground: {biome, belowSurfaceBlocks}` |
| `chestTier` + `easyKit` + `mediumKit` + `hardKit` | `Cave`, `EndStart`, `NetherStart`, `SkyIsland` | one shared `chest: {tier, easy, medium, hard}` |

**Recommended naming rule:** drop the redundant *group prefix*, keep the
*unit suffix*. `oceanShallowWidthBlocks` → `ocean.shallowWidthBlocks`, not
`ocean.shallowWidth`. In a plain text file with no IDE and no type hints, the
`Blocks`/`Days`/`Y` suffix is the only thing telling a user what unit to type,
and it is not the part that repeats. (See Q1 — Jason may prefer full stripping.)

### F2 — 38% of the config file is duplicated starter-kit item lists

145 of the 384 generated lines are 12 near-identical `easyKit`/`mediumKit`/
`hardKit`/`starterKit`/`lootKit` blocks with the same `essentials`/`extras`/
`extrasCount` shape. Four presets each carry a full private copy of the
three-tier structure. This is the single largest source of bulk in the file
and the biggest reason it reads as unnavigable.

### F3 — Top-level sections are not actually global, and nothing says so

The flat top level mixes three genuinely different scopes with no visual or
structural distinction. Traced by call site:

| Scope | Sections | Behavior |
|---|---|---|
| **Live global runtime rules** | `foreverNight`, `risingLava`, `structureDistance` | Read from the config file *every time* via `WorldzCommon.config()` (`WorldHazardManager:64-66,87`, `EnvelopedChunkGenerator:1719`). Editing these **changes existing worlds**. |
| **World-creation defaults, every preset** | `overworldBorder`, `netherBorder`, `endBorder`, `overworldExterior`, `netherExterior` | Read once at world creation into a `*Customization` record, then persisted in the save. Editing these has **no effect on an existing world**. |
| **World-creation defaults, generic `worldz` preset only** | `allowedBiomes`, `starterBiome`, `starterRadiusBlocks`, `ensureStarterLand`, `starterLandTransitionBlocks`, `starterLandFoundationDepthBlocks`, `layout`, `spawn`, `allowRivers`, `allowOceans`, `strip` | Inert for the other 12 presets, but sit at the top level looking universal. |

The live-vs-baked distinction is invisible in the file and undocumented in
`README.md`. A user who edits `overworldBorder` and reloads their world sees
nothing happen; the same user editing `risingLava` sees an immediate change.
Nothing explains why.

**`strip:` is the sharpest case of the third row.** The `strip_world` preset
reads its *corridor width* from the shared top-level `strip:` section
(`StripWorldCustomization:101-103`) but its *biome bands* from `stripWorld:`
(`:104-111`). One preset, two unrelated top-level keys, and `README.md:212`
vs `:253` has to explain the split to the reader.

### F4 — Sentinel values exist because parse throws away the one fact it has

`WorldzConfig.parse` gates every field on `object.containsKey("…")` — at parse
time it knows exactly which keys the user wrote. That knowledge is discarded
into a plain POJO, and the code then has to reconstruct it with magic values.
`StackedConfig:19-30` documents the consequence in its own Javadoc:

> plain config fields carry no "was this explicitly set" flag, so there is no
> way to tell "left at default" apart from "explicitly configured the same
> shape stacked would have picked anyway"

This is the direct cause of a bug that has now been fixed **three times** —
TODO 17.4a, 17.5, and 17.6 — and it cost Jason two full in-game test rounds
(`config/tests/76-stacked-void-exterior.yaml:20-30` records the second).
Six test configs carry `worldSizeChunks: 0` opt-out boilerplate that exists
only to work around it, three of them with a multi-line comment explaining why.

Other live sentinels: `ExteriorConfig.boundaryRadiusBlocks: 0` = "derive from
border", `BorderConfig.resizeRateBlocks/resizeRateDays: 0` = "use resizeDays",
`undergroundBiome: ""` = "disabled", `undergroundBelowSurfaceBlocks: 0` =
"disabled even if a biome is set".

### F5 — The mod silently rewrites the user's config and destroys their comments

`loadExisting` (`WorldzConfig:181-191`) unconditionally calls
`config.save(configFile)` after every successful parse, and `toYaml()`
(`:862-896`) dumps through SnakeYAML, which does not preserve comments.

**This is a deliberate, recorded decision, not an oversight** —
`MEMORY.md`'s Decisions list, 2026-07-14: *"Rewrite successfully parsed config
atomically after sanitation, but never rewrite malformed or wrongly typed
input."* It dates from Phase 0, when the config was one section
(`allowedBiomes`) and rewriting was a cheap way to show users their sanitized
values. It has not been revisited across the 25 sections added since. R1
proposes reversing it; Q4 asks Jason to confirm, since overturning a settled
MEMORY.md decision is his call, not an executor's.

Two consequences:

1. **Every comment in a user's `jlt_worldz.yaml` is deleted on first launch.**
   Every one of Jason's 104 carefully commented `config/tests/*.yaml` files
   loses its entire header the moment it is used.
2. **Every setting becomes explicit after one launch.** `toYaml` writes all 25
   sections and all 165 settings unconditionally, so a 5-line config becomes a
   384-line one. This would silently defeat any presence-tracking fix from F4
   — the two problems must be solved together or not at all.

### F6 — The example config documents 13 of 25 sections; the README claims otherwise

`config/jlt_worldz.example.yaml` (194 lines) covers `allowedBiomes` through
`chaosBiomes` plus the two hazards. Missing entirely: `strip`, `stripWorld`,
`oceanIsland`, `skyIsland`, `chunkIsland`, `cave`, `netherStart`, `endStart`,
`flat`, `deepFlat`, `stacked`, `structureDistance` — **12 of 25 sections, and
every typed preset shipped since Phase 6.**

`README.md:71` tells users the example file "documents every setting with
comments." It does not. The real reference is 139 table rows spread across
`README.md`'s 1619 lines.

`WorldzConfigTest.documentedExampleParsesToTheSameDefaultsAsCode` does not
catch this: it only compares fields the example *does* specify, so an omitted
section trivially matches. The existing TODO backlog entry (2026-07-24) logs
the gap and defers it to Phase 20.1; this proposal supersedes that.

### F7 — Each setting is hand-written into four parallel places

For all 25 config classes there are four hand-maintained methods:
`readXConfig` (YAML→POJO), `sanitizeX` (clamp/validate), `xMap` (POJO→YAML),
`xSummary` (log line). That is ~100 methods and is why `WorldzConfig.java` is
**2400 lines**. Adding one setting means five edits (POJO + four methods),
plus README, plus the example file — seven places, no compiler enforcement of
any of them. F6 is the predictable outcome.

### F8 — The blast radius is smaller than it looks

`grep -l Codec` over the config package returns **zero files**. Config classes
never touch the world-save codecs; `*Customization` records own that, with
their own independent snake_case field names (`force_top_village` etc.).

**Renaming or renesting config keys cannot break an existing saved world.**
The cost is confined to: the parse layer, `config/tests/*.yaml` (104 files),
`README.md`, the example file, and the config unit tests. Combined with the
project's standing "new worlds only" ground rule (`CLAUDE.md`), this is a far
cheaper refactor than its surface area suggests.

---

## 3. Proposed target structure

A directory, replacing the single file. Organized by **scope** (F3) rather
than by the order features happened to ship in.

```
config/
  jlt_worldz/
    runtime.yaml               # live rules — edit any time, affects existing worlds
    world-defaults.yaml        # borders/exteriors — seed new worlds only
    kits.yaml                  # named, reusable starter kits
    world-types/
      worldz.yaml              # the generic preset (was: 11 top-level keys)
      single-biome.yaml
      chaos-biomes.yaml
      strip-world.yaml         # incl. corridor width — no more split brain
      ocean-island.yaml
      sky-island.yaml
      sky-chunk.yaml
      cave.yaml
      nether-start.yaml
      end-start.yaml
      flat.yaml
      deep-flat.yaml
      stacked.yaml
  jlt_worldz.reference.yaml    # GENERATED, never read — always-current docs
```

Each `world-types/*.yaml` filename matches its world preset id, so "which file
configures the world type I picked in the Create World screen" needs no lookup
table. Fixing F3's third row is most of the value: `allowedBiomes`,
`starterBiome`, `layout`, `strip` and friends stop masquerading as global
settings and move into `world-types/worldz.yaml` where they actually apply.

Sketch of the two most-changed files:

```yaml
# world-types/stacked.yaml
layers:
  - "minecraft:taiga;minecraft:bedrock:1,minecraft:stone:43;30"
  - "minecraft:desert"
seedRandomizedOrder: false
worldSizeChunks: 4          # sizes the void wall; no longer forces a border
reliefBlocks: 4
forceTopVillage: false

# world-types/cave.yaml
spawnDepthY: -32
sealedSurface:
  enabled: false
  y: 128
  block: stone
  thicknessBlocks: 5
cavern:
  enabled: false
  radiusBlocks: 48
  heightBlocks: 24
chest:
  enabled: false
  tier: medium
  kits: { easy: cave-easy, medium: cave-medium, hard: cave-hard }   # → kits.yaml
```

---

## 4. Recommendations

Ordered by dependency. R1–R3 are prerequisites: R4 onward is unsound without
them.

### R1 — Stop rewriting the user's config file (fixes F5)

**Recommended — but this reverses a settled MEMORY.md decision (2026-07-14),
so it needs Jason's explicit sign-off (Q4), not an executor's judgement.**

On load: parse, validate, log, and *never write back*.
Instead write `jlt_worldz.reference.yaml` on every launch — a fully-commented,
generated dump of every setting and its default, clearly marked as generated
and never read. Users keep their comments and their 5-line file; they always
have a current reference beside it.

This also permanently kills F6: the reference cannot drift from the code
because it *is* the code.

### R2 — Make "explicitly set" a first-class fact (fixes F4)

**Recommended.** Capture the dotted paths the user actually wrote during parse
into an immutable `Set<String>` carried on `WorldzConfig`, and keep the POJOs
with their inline defaults exactly as they are today.

Chosen over the two alternatives:
- *Nullable boxed fields* would work but destroys the inline-default
  self-documentation and forces `orElse(...)` at ~165 call sites.
- *A `Setting<T>` wrapper* is cleaner in theory but rewrites every consumer.

With presence tracking, `StackedConfig.effectiveOverworldBorder`'s whole
problem statement dissolves — the rule becomes "the user's explicit value
wins; otherwise apply the preset's default" — and the `worldSizeChunks: 0`
opt-out boilerplate can be deleted from six test configs.

### R3 — Adopt Phase 23's alias-with-deprecation-warning precedent for migration

**Recommended.** Jason already settled this policy for `legacy` →
`climate_filter` (TODO 23.2: "special-case in parse to resolve with a one-time
deprecation warning — warn-and-continue, matching this project's existing
config posture"). Reuse it verbatim for key *paths*: an old
`config/jlt_worldz.yaml` keeps loading, each moved key logs one deprecation
line naming its new file and path, and a one-shot migrator writes the new
directory alongside a `.bak` of the original.

This makes the whole restructure non-breaking for Jason's own machine and for
anyone who has ever downloaded the mod.

### R4 — Split into a directory by scope (fixes F3, addresses Jason's Q2)

**Recommended: yes, split — see §3.** Three options were weighed:

| Option | Verdict |
|---|---|
| **A.** Single file, better nested | Rejected. Fixes F1 but leaves F3 entirely — 25 sections in one file still can't show scope, and the file stays ~380 lines. |
| **B.** Directory split by scope (§3) | **Recommended.** Each file answers one question; the world-type files are named after the preset the user picked. |
| **C.** Fully self-contained per-preset files (each carries its own border/exterior/hazards) | Rejected. Would duplicate the border/exterior block 13 times and re-create F1's cross-class duplication at file scale. |

**Important scoping note:** splitting files does *not* by itself fix the
"empty/zero values trigger default behavior" half of Jason's question 2 —
that is R2's job, and R2 is worth doing even if he rejects the split.

### R5 — Extract named, reusable starter kits (fixes F2)

**Recommended.** Define kits once in `kits.yaml` under stable names and have
presets reference them by name. Ships with the current 12 kits pre-named so
existing behavior is byte-identical. Removes ~145 lines (38%) from the
user-visible surface and means a user who wants "the same kit everywhere"
edits one block instead of four.

Inline kit definitions should stay legal for one-off overrides — reference by
name *or* spell it out.

### R6 — Collapse the flat prefixes (fixes F1)

**Recommended**, per F1's two tables and the "drop the group prefix, keep the
unit suffix" rule. Purely mechanical once R3's alias layer exists.

### R7 — Label live-vs-baked scope in the file itself (fixes F3's invisible half)

**Recommended.** The `runtime.yaml` / `world-defaults.yaml` filenames carry
most of it; the generated reference should state it explicitly at the top of
each file, and `README.md` needs a short section. This is a real user-facing
bug in the current docs, independent of everything else here.

### R8 — Generate parse, validation, docs and summary from one schema (fixes F7)

**Recommended in principle, deferred in practice.** A declarative descriptor
per setting (path, type, default, range, unit, one-line doc, applies-to,
Customize-exposed?) would collapse `WorldzConfig.java` from 2400 lines to a
schema plus a small engine, and make F6-style drift structurally impossible.

It is also the largest and riskiest item here, and it should not be entangled
with a restructure that already touches every config file. **Recommendation:
do R1–R7 first, land them, then evaluate R8 as its own phase** with the
restructured (and by then, far more regular) config as its input.

### R9 — Add a documentation-completeness test now, before any of the above

**Recommended, and cheap enough to do immediately.** A unit test asserting
that every leaf setting reachable from `new WorldzConfig()` appears in the
example/reference file and in a README table. It fails today (F6, 12 sections)
— which is the point. It guards the restructure while it is in flight and
replaces the discipline F7 currently depends on.

---

## 5. Coordination with existing phases

- **Phase 23 (`legacy` → `climate_filter`)** is a config-value rename that
  needs exactly the alias machinery R3 describes. Recommend building the alias
  layer once, in whichever phase lands first, and having the other consume it.
  If the restructure runs first, 23.2/23.4 shrink to near-nothing.
- **Phase 24 (shared code extraction)** is independent — it refactors generator
  internals, not config — but R5's shared-kit extraction overlaps with
  **24.5** (consolidating duplicated `NetherStartPlan`/`EndStartPlan` capsule
  config fields). Recommend 24.5 be folded into this work instead.
- **Phase 20.1** (full README/config-reference/example rewrite) is largely
  *subsumed*: R1's generated reference plus R9's completeness test deliver most
  of it structurally. Recommend 20.1 be reduced to a prose/challenge-framing
  pass once this lands.
- **Phases 21–22** add new settings (GOALS 42/43). Every setting they add is
  another one to migrate. Sequencing is Q7.

---

## 6. Risks

| Risk | Severity | Mitigation |
|---|---|---|
| 104 test configs need migrating | Medium | The R3 alias layer means they keep working unmigrated; migrate them mechanically with a script, and only re-run Jason's acceptance configs for presets whose *shape* changed. |
| Restructure lands mid-flight with Phases 21–23 also editing config | Medium | Q7 — sequence deliberately; do not run concurrently. |
| R5's kit references add indirection a user must follow | Low | Keep inline definitions legal; generated reference shows the resolved kit. |
| A generated reference file appearing in users' config dirs looks like clutter | Low | Name it `.reference.yaml`, state "generated, never read" in line 1. |
| Scope creep into R8 | Medium | R8 is explicitly deferred to its own phase. |

## 7. Non-goals

- No change to any `*Customization` record or world-save codec (F8) — saved
  worlds are untouched.
- No gameplay or generation behavior change. Every item here is
  behavior-preserving, same hard constraint Phase 24 carries.
- No change to the Customize screen's fields or layout.

---

## 8. Open questions for Jason

Answer these and the TODO Phase 25 items below become executable. Q1–Q4 are
blocking; Q5–Q9 have a recommended default that can be taken as-is.

**Q1 — Unit suffixes.** R6 recommends keeping `Blocks`/`Days`/`Y` suffixes on
leaves and only dropping the grouping prefix (`ocean.shallowWidthBlocks`).
Fully stripping them reads better (`ocean.shallowWidth`) but loses the only
unit hint in a plain text file. *Recommended: keep the suffixes.* Prefer the
shorter form?

**Q2 — Directory split.** R4 recommends splitting into `config/jlt_worldz/`
with one file per world type (§3). This is the biggest single change in the
proposal. Confirm — or say you'd rather keep one file and take only the
nesting/presence fixes (R1, R2, R5, R6 all work standalone).

**Q3 — Named starter kits.** R5 moves ~145 lines of kit definitions into a
shared `kits.yaml` referenced by name. Confirm, or keep kits inline per preset?

**Q4 — Stop rewriting the config file.** R1 means the mod never touches
`jlt_worldz.yaml` again and instead writes a separate generated reference.
This **reverses your own 2026-07-14 MEMORY.md decision** ("Rewrite successfully
parsed config atomically after sanitation"), made when the config was a single
`allowedBiomes` list and never revisited across the 25 sections added since. It
is also a prerequisite for R2. Confirm the reversal?

**Q5 — Sentinel replacement policy.** With R2's presence tracking, sentinels
like `undergroundBiome: ""` and `boundaryRadiusBlocks: 0` could become real
absent-vs-set states. That is stricter and clearer, but a user who *writes*
`""` today gets the same result either way. *Recommended: convert
`worldSizeChunks`-style precedence sentinels (the ones that caused real bugs),
leave cosmetic ones like `""` alone.* Convert all of them instead?

**Q6 — Deprecation window.** R3 keeps old key paths working with a one-time
warning. *Recommended: keep the aliases through 1.0, drop after.* Or drop them
at the next minor, since you are the only user today?

**Q7 — Sequencing.** *Recommended: run this after Phase 23 and before Phase
20, and fold Phase 24.5 into it.* Phases 21–22 add new settings, so running
before them means migrating fewer settings; running after them means not
re-planning 21/22 around a moving config shape. Which order?

**Q8 — Scope of the first pass.** *Recommended: R1, R2, R3, R9 as one landable
unit ("make the config honest"), then R4, R5, R6, R7 as a second ("restructure
the files"), then R8 evaluated separately.* Or land it all at once?

**Q9 — `strip` / `stripWorld` merge.** F3 shows the `strip_world` preset reads
corridor width from a different top-level section than its bands. *Recommended:
merge both into `world-types/strip-world.yaml`* — but note the generic `worldz`
preset also reads `strip:`, so the shared corridor setting needs a home either
way. Confirm the merge, and whether the generic preset keeps its own copy?

**Q10 — Anything else you want restructured while the file is open?** This is
the cheapest it will ever be to rename or move a setting.
