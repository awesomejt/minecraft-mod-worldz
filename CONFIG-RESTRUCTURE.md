# Config restructure — findings and plan of record

**Status: DECIDED 2026-07-27. Ready to implement as TODO Phase 25.**

Research pass run 2026-07-27 at Jason's request ("restructure the configuration
files… more nested structure… should properties be put into their own yaml
configuration files… better organized documentation"). All ten open questions
from the first draft were answered the same day; §1 records the decisions, and
the rest of the document has been rewritten to match them. Fold the design into
`DESIGN.md` when Phase 25 opens.

The three problems Jason raised:

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

## 1. Decisions

| # | Decision | Consequence |
|---|---|---|
| **D1** | **No backward compatibility.** Jason has no production worlds; nothing downstream depends on the current config shape. | The alias/deprecation layer the first draft proposed is **deleted entirely**. Old configs simply stop loading. Substantially less code and no migration mode to maintain. |
| **D2** | **Split into a directory, by scope.** `config/jlt_worldz/` with one file per world type (§3). | Fixes F3. The 11 generic-preset-only top-level keys stop masquerading as global. |
| **D3** | **Schema-driven config.** One declarative descriptor per setting generates parse, validation, the reference file and the README tables. | Fixes F7 *and* Jason's "2400 lines in any one class needs a very compelling reason". Replaces ~100 hand-written methods with ~30 small classes. Promoted from "deferred" in the first draft to the core of the phase. |
| **D4** | **Stop rewriting the user's config file.** Ship a generated `jlt_worldz.reference.yaml` instead. | Fixes F5. Reverses the 2026-07-14 MEMORY.md decision — explicitly confirmed by Jason. Prerequisite for D5. |
| **D5** | **Convert every sentinel** to a real absent-vs-set state, not just the ones that caused bugs. | Fixes F4. `worldSizeChunks: 0`, `boundaryRadiusBlocks: 0`, `resizeRate*: 0`, `undergroundBiome: ""`, `undergroundBelowSurfaceBlocks: 0` all become genuine "unset". |
| **D6** | **Named shared starter kits** in `kits.yaml`, referenced by name; inline definitions stay legal for one-offs. | Fixes F2. Removes ~38% of the config's bulk. |
| **D7** | **Drop redundant unit suffixes** — `Blocks` goes when context makes it obvious; documentation carries the unit. | Fixes F1's second half. Naming rule in §2. |
| **D8** | **Run next, before Phases 21–24.** | Phases 21–23 all add or rename settings; doing this first means they land in the new shape directly, and Phase 23 (`legacy` → `climate_filter`) becomes nearly free. |
| **D9** | **Strip world moves to an absolute width**, minimum 1 block, centered, with the End portal at the corridor's mid-point. | A deliberate **behavior change** — the one exception to this phase's behavior-preserving rule. Detail in §5. |
| **D10** | `strip:` / `stripWorld:` merge into one `world-types/strip-world.yaml`. | Resolved by D2; see F3 for the split-brain this fixes. |

---

## 2. Naming rule (D7)

Applied in order:

1. **Drop the group prefix** once nesting supplies it —
   `oceanShallowWidthBlocks` → `ocean.shallowWidth`.
2. **Drop `Blocks`.** Blocks are the default unit; the generated reference
   states the unit for every setting, so the suffix is pure noise —
   `starterRadiusBlocks` → `starter.radius`.
3. **Keep a suffix when it *is* the meaning, not a unit restatement.**
   `resize.days` keeps `days` because the key would otherwise be
   contentless; `surfaceY` keeps `Y` because it names an *axis*, not a unit,
   and bare `surface` would read as a block or biome.
4. **Let nesting disambiguate rather than the leaf name.** Several groups end
   up with their own `radius` — that is fine and is the point.

---

## 3. Target structure

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
      strip-world.yaml         # incl. absolute corridor width — no more split brain
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
configures the world type I picked in the Create World screen" needs no lookup.

Two files as they will actually read, with D5/D6/D7 applied:

```yaml
# world-types/cave.yaml
spawnY: -32
sealedSurface:
  enabled: false
  y: 128
  block: stone
  thickness: 5
cavern:
  enabled: false
  radius: 48
  height: 24
chest:
  enabled: false
  tier: medium
  kits: { easy: cave-easy, medium: cave-medium, hard: cave-hard }
```

```yaml
# world-types/ocean-island.yaml
island:
  source: artificial
  biome: minecraft:plains
  radius: 128
  shapeAmplitude: 0.3
fluid: water
shoreWidth: 12
ocean:
  shallowWidth: 64
  deepenWidth: 128
  shallowDepth: 8
  deepDepth: 32
  regionScale: 128
exclusionZone:
  enabled: false
  radius: 2000
starterKit: ocean-island-default
```

---

## 4. Findings

Measured against the working tree at `989ade7`, not estimated. Field
inventories came from all 26 classes in
`common/src/main/java/media/jlt/minecraft/mods/worldz/config/`; the reference
numbers came from dumping `new WorldzConfig().toYaml()` — byte-for-byte what
the mod writes to disk today.

### F1 — The file is flat and repetitive, exactly as Jason described

165 leaf scalar settings across 25 top-level sections, in a 384-line generated
file. Fields that are conceptually one nested object are spelled as a shared
prefix repeated across siblings:

| Class | Flat fields today | Nests to (D7 applied) |
|---|---|---|
| `BorderConfig` | `resizeDays`, `resizeDelayDays`, `resizeRateBlocks`, `resizeRateDays`, `resizeStyle` | `resize: {days, delayDays, rateBlocks, rateDays, style}` |
| `CaveConfig` | `sealedSurface`, `sealedSurfaceY`, `sealedSurfaceBlock`, `sealedSurfaceThicknessBlocks` | `sealedSurface: {enabled, y, block, thickness}` |
| `CaveConfig` | `cavernEnabled`, `cavernRadiusBlocks`, `cavernHeightBlocks` | `cavern: {enabled, radius, height}` |
| `CaveConfig` | `chestEnabled`, `chestTier` | `chest: {enabled, tier}` |
| `OceanIslandConfig` | `oceanShallowWidthBlocks`, `oceanDeepenWidthBlocks`, `oceanShallowDepthBlocks`, `oceanDeepDepthBlocks`, `oceanRegionScaleBlocks` | `ocean: {shallowWidth, deepenWidth, shallowDepth, deepDepth, regionScale}` |
| `OceanIslandConfig` | `islandSource`, `islandBiome`, `radiusBlocks`, `shapeAmplitude` | `island: {source, biome, radius, shapeAmplitude}` |
| `ChunkIslandConfig` | `topOnly`, `topOnlyDepthBlocks` | `topOnly: {enabled, depth}` |
| `ChunkIslandConfig` | `applyToNether`, `applyToEnd` | `applyTo: {nether, end}` |
| `FloatingIslandsConfig` | `minRadiusBlocks`, `maxRadiusBlocks` | `radius: {min, max}` |
| `FloatingIslandsConfig` | `oreDepositsEnabled`, `oreFeatureIds` | `oreDeposits: {enabled, featureIds}` |
| `FloatingIslandsConfig` | `lootChestEnabled`, `lootKit` | `lootChest: {enabled, kit}` |
| `StripConfig` | `widthRadiusBlocks`, `widthMode` | `width`, `widthMode` (see §5 — width becomes absolute) |
| `RisingLavaConfig` | `rateBlocks`, `rateDays` | `rate: {blocks, days}` |
| `StarterCapsuleConfig` | `lightSource`, `lightSpacingBlocks` | `light: {source, spacing}` |

Plus four shapes duplicated *across* classes rather than within one:

| Repeated shape | Appears in | Collapses to |
|---|---|---|
| `exclusionZoneEnabled` + `exclusionZoneRadiusBlocks` | `ChunkIsland`, `FloatingIslands`, `OceanIsland`, `SkyIsland` | shared `exclusionZone: {enabled, radius}` |
| `allowRivers` + `allowOceans` + `allowBeaches` | `ChaosBiomes`, `SingleBiome`, `StripBands`, + top level (2 of 3) | shared `naturalBiomes: {rivers, oceans, beaches}` |
| `undergroundBiome` + `undergroundBelowSurfaceBlocks` | `Flat`, `SkyIsland` | shared `underground: {biome, belowSurface}` |
| `chestTier` + `easyKit` + `mediumKit` + `hardKit` | `Cave`, `EndStart`, `NetherStart`, `SkyIsland` | shared `chest: {tier, kits}` (D6) |

### F2 — 38% of the config file is duplicated starter-kit item lists

145 of the 384 generated lines are 12 near-identical
`easyKit`/`mediumKit`/`hardKit`/`starterKit`/`lootKit` blocks with the same
`essentials`/`extras`/`extrasCount` shape. Four presets each carry a private
copy of the three-tier structure. Largest single source of bulk in the file.
→ D6.

### F3 — Top-level sections are not actually global, and nothing says so

The flat top level mixes three genuinely different scopes with no visual or
structural distinction. Traced by call site:

| Scope | Sections | Behavior |
|---|---|---|
| **Live global runtime rules** | `foreverNight`, `risingLava`, `structureDistance` | Re-read from config *every time* via `WorldzCommon.config()` (`WorldHazardManager:64-66,87`, `EnvelopedChunkGenerator:1719`). Editing these **changes existing worlds**. |
| **World-creation defaults, every preset** | `overworldBorder`, `netherBorder`, `endBorder`, `overworldExterior`, `netherExterior` | Read once at world creation into a `*Customization` record, then persisted in the save. Editing these does **nothing** to an existing world. |
| **World-creation defaults, generic `worldz` preset only** | `allowedBiomes`, `starterBiome`, `starterRadiusBlocks`, `ensureStarterLand`, `starterLandTransitionBlocks`, `starterLandFoundationDepthBlocks`, `layout`, `spawn`, `allowRivers`, `allowOceans`, `strip` | Inert for the other 12 presets, but sit at the top level looking universal. |

The live-vs-baked distinction is invisible in the file and undocumented in
`README.md`. A user who edits `overworldBorder` and reloads their world sees
nothing happen; the same user editing `risingLava` sees an immediate change.

**`strip:` is the sharpest case of the third row.** The `strip_world` preset
reads its *corridor width* from the shared top-level `strip:` section
(`StripWorldCustomization:101-103`) but its *biome bands* from `stripWorld:`
(`:104-111`). One preset, two unrelated top-level keys, and `README.md:212` vs
`:253` has to explain the split to the reader. → D10.

### F4 — Sentinel values exist because parse throws away the one fact it has

`WorldzConfig.parse` gates every field on `object.containsKey("…")` — at parse
time it knows exactly which keys the user wrote. That knowledge is discarded
into a plain POJO, and the code then reconstructs it with magic values.
`StackedConfig:19-30` documents the consequence in its own Javadoc:

> plain config fields carry no "was this explicitly set" flag, so there is no
> way to tell "left at default" apart from "explicitly configured the same
> shape stacked would have picked anyway"

This is the direct cause of a bug fixed **three times** — TODO 17.4a, 17.5 and
17.6 — costing Jason two full in-game test rounds
(`config/tests/76-stacked-void-exterior.yaml:20-30` records the second). Six
test configs carry `worldSizeChunks: 0` opt-out boilerplate that exists only to
work around it, three with a multi-line comment explaining why.

Other live sentinels: `ExteriorConfig.boundaryRadiusBlocks: 0` ("derive from
border"), `BorderConfig.resizeRateBlocks`/`resizeRateDays: 0` ("use
resizeDays"), `undergroundBiome: ""` ("disabled"),
`undergroundBelowSurfaceBlocks: 0` ("disabled even with a biome set"). → D5.

### F5 — The mod rewrites the user's config and destroys their comments

`loadExisting` (`WorldzConfig:181-191`) unconditionally calls
`config.save(configFile)` after every successful parse, and `toYaml()`
(`:862-896`) dumps through SnakeYAML, which does not preserve comments.

1. **Every comment in a user's `jlt_worldz.yaml` is deleted on first launch** —
   including every one of the 104 commented `config/tests/*.yaml` headers.
2. **Every setting becomes explicit after one launch.** `toYaml` writes all 25
   sections unconditionally, so a 5-line config becomes a 384-line one. This
   would silently defeat D5's presence tracking — the two must be fixed
   together.

This was a deliberate decision (`MEMORY.md`, 2026-07-14: *"Rewrite successfully
parsed config atomically after sanitation…"*) made in Phase 0 when the config
was one section, never revisited across the 25 added since. → D4 reverses it,
confirmed by Jason.

### F6 — The example config documents 13 of 25 sections; the README claims otherwise

`config/jlt_worldz.example.yaml` (194 lines) covers `allowedBiomes` through
`chaosBiomes` plus the two hazards. Missing entirely: `strip`, `stripWorld`,
`oceanIsland`, `skyIsland`, `chunkIsland`, `cave`, `netherStart`, `endStart`,
`flat`, `deepFlat`, `stacked`, `structureDistance` — **12 of 25 sections, and
every typed preset shipped since Phase 6.**

`README.md:71` tells users the example file "documents every setting with
comments." It does not. The real reference is 139 table rows spread across
`README.md`'s 1619 lines.

`WorldzConfigTest.documentedExampleParsesToTheSameDefaultsAsCode` cannot catch
this: it only compares fields the example *does* specify, so an omitted section
trivially matches. → D3 makes the drift structurally impossible.

### F7 — Each setting is hand-written into four parallel places

For all 25 config classes there are four hand-maintained methods: `readXConfig`
(YAML→POJO), `sanitizeX` (clamp/validate), `xMap` (POJO→YAML), `xSummary` (log
line). ~100 methods, and the reason `WorldzConfig.java` is **2400 lines**.
Adding one setting means five code edits plus README plus the example file —
seven places, none compiler-enforced. F6 is the predictable outcome. → D3.

### F8 — The blast radius is smaller than it looks

`grep -l Codec` over the config package returns **zero files**. Config classes
never touch the world-save codecs; `*Customization` records own that, with
independent snake_case field names (`force_top_village` etc.).

**Renaming or renesting config keys cannot break a saved world.** Combined with
D1 (no production worlds) and the standing "new worlds only" ground rule, the
cost is confined to the parse layer, `config/tests/*.yaml` (104 files),
`README.md`, the example file, and the config unit tests.

---

## 5. Strip world: absolute width (D9)

**This is the one behavior change in the phase.** Jason: *"the radius never
worked well. I would rather have a fixed/absolute width — even down to only 1
block wide if the player wants it… I would center the end portal on the
mid-point of the width of the world."*

**Today:** `StripConfig.widthRadiusBlocks` is a half-width from origin on the Z
axis (`StripPlan.modeAt` classifies `|relativeZ| > widthRadiusBlocks`). The
corridor is therefore always `2r + 1` blocks wide, always symmetric about Z=0,
and cannot be narrower than 3 (`StripPlan` rejects `widthRadiusBlocks <= 0`).

**Target:** `width` is the absolute block count, minimum **1**.

| `width` | Z range | Portal Z |
|---|---|---|
| 1 | `0` | 0 |
| 2 | `0..1` | 0 |
| 3 | `-1..1` | 0 |
| 4 | `-1..2` | 0 |
| 5 | `-2..2` | 0 |
| 16 | `-7..8` | 0 |

Odd widths sit symmetrically about Z=0; even widths take the extra block on the
**+Z** side. The End portal (and the Nether fortress guarantee) targets the
corridor's mid-point.

**Known API consequence:** `ObjectiveSite.narrowForStrip` currently returns a Z
*radius* that its three callers (`ProgressionGuarantees:70,114`,
`StackedVillageDeployment:117`) apply symmetrically about the origin. An
even-width corridor is no longer symmetric, so it must return a *centre plus
half-extent* (or a small range record) rather than a bare radius. Small, but it
touches every caller — do not treat it as a rename.

**Accepted by Jason, not a bug:** at very narrow widths the End portal and
other structures will overflow the corridor into the void. *"Of course, anything
very narrow — the end portal or structures would not obey this rule."* No
clamping or structure-shrinking work is in scope.

---

## 6. Work plan

Ordered by dependency. Detail lives in `TODO.md` Phase 25.

1. **Schema layer (D3).** Declarative `Setting` descriptors carrying path,
   type, default, range, unit, one-line doc, applies-to-preset, and
   Customize-screen exposure. One small schema class per section (~30 files)
   replaces the ~100 hand-written methods and breaks up the 2400-line class.
2. **Presence tracking (D5).** With the schema owning parse, "the user wrote
   this key" becomes a natural property of the parse result rather than
   something reconstructed from magic values.
3. **Stop rewriting; generate the reference (D4).** Load becomes
   parse-validate-log. The generated `jlt_worldz.reference.yaml` is emitted
   from the same schema that drives parsing, so it cannot drift.
4. **Retire the sentinels (D5).** Rewrite `StackedConfig`'s two `effective*`
   methods in terms of "did the user set it?" and delete the
   `worldSizeChunks: 0` boilerplate from six test configs.
5. **Restructure the keys (D7) and split the files (D2).**
6. **Named kits (D6).** Ships with the current 12 pre-named so behavior is
   byte-identical.
7. **Strip absolute width (D9)** — §5, including the `narrowForStrip` change.
8. **Docs.** README tables generated from the schema; a completeness test
   covering every leaf setting; the live-vs-baked scope distinction (F3)
   written down for the first time.
9. **Migrate `config/tests/*.yaml`** (104 files) mechanically. With D1 there is
   no alias fallback, so this lands in the same change or the test suite breaks.

---

## 7. Risks

| Risk | Severity | Mitigation |
|---|---|---|
| 104 test configs must all migrate at once (no alias fallback, per D1) | **High** | Mechanical rewrite driven by the schema's own old→new path map; a test asserting every `config/tests/*.yaml` still parses clean is the gate. |
| D3 is the largest refactor in the project's history | High | Land the schema layer first *behind the existing shape*, prove it round-trips the current config byte-identically, then restructure keys. Two provable steps, not one leap. |
| D9 changes generation behavior, unlike everything else here | Medium | Separate commit, separate test configs, and it is [Jason]-testable in isolation. |
| Phase 23's `legacy` → `climate_filter` overlaps | Low | D8 sequences this first; 23.2 then becomes a one-line schema edit. |
| Phase 24.5's capsule-config consolidation overlaps D6 | Low | Fold 24.5 into this phase rather than doing it twice. |

## 8. Non-goals

- No change to any `*Customization` record or world-save codec (F8).
- No gameplay change **except D9** (strip width), which is deliberate and
  scoped to §5.
- No change to the Customize screen's fields or layout.
- No backward compatibility, no migration mode, no deprecation aliases (D1).

## 9. Remaining questions

None blocking. Two judgement calls will surface during implementation and
should be raised then rather than guessed:

- **Kit naming scheme.** Whether shipped kits are named per-preset
  (`cave-easy`) or per-role (`underground-easy`) once several presets could
  reasonably share one. Only matters if presets start sharing kits.
- **How much of README's 139 settings rows to generate.** Generating the tables
  is clearly right; generating the surrounding prose is not. Expect a hand-written
  challenge-first narrative with generated tables slotted in.
