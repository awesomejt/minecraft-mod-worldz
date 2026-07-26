# Goals

Create a mod to generatate a wide set of "challenge worlds" commonly seen on YouTube - like "ocean-only challenge" and "100 days on a small island" type challenges. Mod should exceed the functionality of simple world datapacks - but may use them to make world generation easier and consistent in certain scenarios.


## Workflow

1. Planning and Requirements
 - Lookup any questions online
 - Prompt user for questions that are unresolved or need clarification
2. Document decisions and TODOs - structured based upon phases.
3. Implementation per Phase
 - Review TODO items, choose item/task to work on
 - Ensure all information is needed to proceed with task
 - Create code for each task
 - Create test code for each task using JUnit to test logic and components - no automatic game testing
 - Create or update documentation
 - If new tasks are required - add to TODO file for tracking
 - Commit per task - first line of commit has a brief summary of changes with more details below
4. Wait for manual testing and fix any defects in current phase
5. Proceed to next phase with given permission

## Technical Aspects

### Mod Loader Support

Client only mod loaders:

- Fabric (primary)
- NeoForge

### Configuration

- YAML configuration format
- Comment-based documentation
- Documentation, including examples, in Markdown file like README
- Ideally, mod should not override file if just using defaults
- Mod-level configuration file provides defaults that override defaults in code
- Each world gets own configuration file once world is generated and based on the settings chosen during the world creation process

**Open (added 2026-07-24, Jason, reviewing config 51) — per-dimension config
overrides, general convention, not yet designed:** most config sections are
currently flat (one level), even where a setting could plausibly want to
differ by dimension — e.g. `chunkIsland`'s `exclusionZoneRadiusBlocks` is one
value shared across Overworld/Nether/End even though `applyToNether`/
`applyToEnd` already let each dimension be toggled independently, and the
Nether being more dangerous is a real reason to want a different (likely
smaller) exclusion zone there. Jason wants this as a **general convention** —
a reusable "shared/default value + optional per-dimension override,
falling back when absent" shape usable anywhere it's relevant (exclusion
zones, `structureDistance`, the new island-minimum-distance idea from goal
09, `structureDistance` generally), not a one-off fix just for `chunkIsland`.
Verified feasible without hitting this project's known codec ceiling
(DESIGN §29.7's 14-field `group()` limit applies to `LimitedBiomeSource`'s
own top-level group, already full; `ChunkIslandCodecs.PLAN_CODEC`'s own
group is only 7/14, room for a nested override object at the cost of one
slot there) — **but there is no existing precedent for this shape anywhere
in the codebase today** (checked `EndBorderConfig`, everything named
"override"/"perDimension"); this is new general-purpose scope, not a small
follow-up. Needs a design pass (the actual YAML shape, which existing
sections are worth retrofitting vs. leaving flat, and the codec pattern for
the fallback) before scheduling.

**Open (added 2026-07-25, Jason) — broader config structure review,
not yet designed:** widens the item above from "per-dimension overrides"
to the config schema generally. Two asks: (1) reduce **property-name
duplication** by adding nesting depth where it helps readability — e.g.
`cave`'s `sealedSurface`/`sealedSurfaceY`/`sealedSurfaceBlock`/
`sealedSurfaceThicknessBlocks` (the last two just added, 2026-07-25,
DESIGN §30.4) repeat the `sealedSurface` prefix on every sibling field
instead of nesting into one `sealedSurface: {enabled, y, block,
thicknessBlocks}` object — a concrete, freshly-introduced example of
exactly the pattern being flagged, not a hypothetical one; (2) review
whether any settings duplicated *across* typed-preset sections (border/
exterior shapes, starter-kit tiers, chest mechanics, etc. — several
sections already share near-identical `easyKit`/`mediumKit`/`hardKit`
shapes, for instance) should consolidate into a common/shared section
instead of being redeclared per preset. Also wants the example config
(`config/jlt_worldz.example.yaml`) properly maintained afterward, with
real per-section spacing and commented documentation — this was already a
known gap before today (see TODO Backlog: the example only documents
sections through Phase 4, deferred to Phase 20's planned full config-
reference rewrite). **Not designed or scheduled** — this is a real schema
change touching most config sections, every one of the ~85 `config/tests/
*.yaml` files, and the example file together, not a quick edit; needs its
own design pass (which sections nest, which consolidate, exact YAML shape)
before any implementation, likely combined with the per-dimension-override
item above as one coordinated config-schema pass.

### World Generation Screen

- If no changes are made, use default settings if no config file is defined or use setting from configuration file.
- If it makes the screen easier to manage, have multiple World types for major or perhaps mutually exclusive world generation processes
- Break initial configuration screen into multiple parts
- Change defaults based on type of world generation chosen when it makes sense
- For long screens, use scrolling UI option

### Building

- Have project-level Gradle build that can build all components - including each mod loader - without errors.
- Use "media.jlt.minecraft.mods.worldz" as base package for mod.

## Phases and major use cases

### Ocean Island Challenge:

01. Island challenge - configuratable size - including very small (16 blocks/1 chunk) to huge. Player spawns upon an island of chosen biome type - surrounded by endless ocean. Biome should use a combination of beach and/or stony shore to transition to the ocean. Underground structures still remain. Ocean should gracefully go from shallow (warm/lukewarm) to deep, but all ocean biomes available. Nether and End are unchanged. Game is beatable. Ideally, the island should look natural, not just a square or circle.
02. Same as 1, but instead of creating an artifical island, mod chooses a natural island based on seed (sets world spawn) - but replaces all other biomes with ocean biomes.
03. Same as 1, but instead of a starter island, player starts on a chest boat in the middle of the ocean - no land biomes. Chest contains a few essential items (including lily pad, dirt block, grass block, and sapplings) and a few random ones - this can be configurable.
04. Same as 1 or 2, but can contain some very small islands but really far away (2000+ block away). Can just use natural generation (seed) of small islands beyond exclusion zone.

### Sky Island Challenge (similar to Skybock):

05. Sky island challenge - similar to Skyblock. Configuarable necessities starting chest - depending on starting conditions. Can choose from easy, medium, and hard starting materials - all should be beatable given enough time. For example, giving the user a bucket of water vs a couldren to capture rain water. Biome of sky island is configurable but informs what is necessities chest. Sky island is surrounded by void (no ocean). Option to generate structures - specifically a stronghold so game remains beatable. By default, spawn at Y 64 to avoid slimes (may be configurable).
06. Same as 5 but Nether/End is a sky island too. Configuarable to retain structures so game is still beatable.

    **Resolved (2026-07-20):** implemented for the Nether (Phase 10.4,
    `skyIsland.applyToNether`). The End is **skipped, not implemented** —
    vanilla End generation already uses the island concept natively
    (small landmasses surrounded by void), so a dedicated "force a single
    End island" feature wasn't compelling once investigated (Phase 10.5
    spike, DESIGN §27.7). Existing GOALS 17 End-border carry-over already
    covers "keep it small" if wanted.
07. Same as 5/8 but villages can be generated outside of an exclusion zone - requiring significant bridging to reach.

    **Scope note (2026-07-20):** deferred in full to Phase 11 — it's a
    narrow slice of 08's own "scattered floating islands" mechanism (a
    village needs *something* to stand on beyond the exclusion zone), so
    it's built once there rather than twice. Phase 10 (05–06) ships the
    single starter sky island only; End sky islands (part of 06) are
    spike-first, see TODO Phase 10.5.
08. Same as 7, but instead of pure void. Random, small floating islands generate containing different resources with various sizes. Use seed as a way to randomize generatation. Islands should be sufficiently far away to require a lot of bridging.

### Sky Chunk challenge:

09. Similar to 5-8, but based on chunks. Use the natural chunks of the seed. For chunk islands, make sure one generates with a portal room. Have option to include entire chunk or only top (land) until a certain depth - like 5 deep to ensure access to stone. Options for a normal Nether and End, or chunk islands.

    **Feedback (2026-07-24, Jason's Phase 12.7 in-game acceptance of configs
    49/50) — open, not yet designed:**
    1. **Islands are too close together.** `spawnChance`/`cellSizeChunks`
       (DESIGN §29.2) is only a statistical density knob — confirmed in code
       (`ChunkIslandPlan.at`) there is no neighbor-cell scan or spacing check
       at all, unlike `FloatingIslandsPlan`'s 3×3 jitter search (which only
       resolves overlap of one island's own footprint, not spacing between
       different islands). Even a low `spawnChance` (e.g. 0.1, config 50)
       still leaves islands close enough that little bridging effort is
       needed. Wants a real **minimum distance between islands** (a true
       island-to-island spacing guarantee, not just a probability), that
       also applies to the new "structure islands" idea below (40) so those
       stay a genuine destination.
    2. **`ensureEndPortal` should be conditional on world size.** Verified in
       code: `WorldLimitManager.needsGuaranteedPortalRoom` fires
       unconditionally whenever `chunkIsland.enabled` is true, with no check
       on `overworldBorder.enabled`. Jason's intent: the forced/guaranteed
       stronghold only matters for a **limited-size** world where a natural
       stronghold might not fit within the border; for an **infinite**
       world, forcing one is unnecessary as long as structures spawn
       naturally (a stronghold is reachable via ordinary generation, or via
       37's structure-bearing-chunk showcase search) — the guarantee should
       be skipped/relaxed in that case, not applied unconditionally.
    3. **Found while verifying #2, worth resolving either way:** the
       guaranteed portal-room's reserved cell currently *is* subject to the
       plan's `topOnly` depth cutoff like any ordinary island
       (`ChunkIslandPlan.at`'s reserved-portal branch reuses the plan-wide
       `topOnly` field verbatim) — unlike the reserved **geode** cell and
       the 37 **showcase** cells, both of which are explicitly forced
       full-column so they can't be truncated. This looks like an
       inconsistency against the codebase's own established precedent (not
       yet confirmed in-game whether the forced stronghold placement, which
       runs later at server start, ends up visibly chopped or just
       oddly shaped). Needs a decision either way before/alongside #2's fix.
37. Same as 09, but beyond the starter chunk island, additional chunk islands of *different biomes* generate. Each island can independently be top-only (to a configured depth) or the entire chunk column. Where possible, some islands should showcase underground content: cave biomes (lush caves, dripstone caves, deep dark), amethyst geodes, and structure-bearing chunks — so exploration/bridging yields varied resources. (Clarified 2026-07-16.)

    **Config 50 feedback (2026-07-24):** all islands top-only is already
    achievable today (plan-wide `topOnly: true`); what's still wanted is
    "optional structures spawning" on those top-only islands specifically —
    i.e. some top-only islands should still be allowed to carry a full,
    unchopped structure (see new goal 40, "structure islands") rather than
    every structure on a top-only world getting truncated at the depth
    cutoff along with the terrain.

### Single-biome challenge:

10. Single biome challenge - world contains one land biome. Configuration option to allow structure, caves, and everything else to generate. Randomness is based on seed.
11. Same as 10, but starter biome can be different.
12. Same as 11, but starter biome is based on seed - including size and location. World spawn set to the middle or somewhere in the desired biome.
13. Same as 10-12, but Rivers allowed to generate.
14. Same as 10-13, but natural oceans allowed to generate.
15. (Approved 2026-07-17, scope for a later phase) Same as 10-14, but with a
    configuration option to let vanilla's own underground cave biomes
    (dripstone caves, lush caves, deep dark) generate normally instead of
    the single biome applying uniformly at every depth. Current behavior
    (Phase 2, 0.2.x): the single biome applies from bedrock to sky with no
    vertical variation, so cave biomes never appear — acceptable for now,
    but this option is approved future scope. See DESIGN §20.5's vanilla
    pass-through mechanism (rivers/oceans, Phase 3) for the closest existing
    precedent; this would need a similar but depth-aware (not just
    surface-family-aware) pass-through.

### Flat-World challenge:

15. World is flat or mostly flat. No hills or mountains. Based upon a single biome - default to plains like vanilla. Options to generate structures (including trial chambers). Option for starting Y level to avoid slimes if desired. Options for floor components like different layers with presets. Bottom layer to be bedrock or not - allowing for building "under" the world. Basically, my version of vanilla superflat but with more options.
16. World is flat - no hills or mountains, but underground can contain caves if deep enough. Can use seed to determine randomness of caves, structure, and cave biomes. Option for rivers to generate if enough layers exist - with option ensure they generate far away from world spawn.

    **Clarified (2026-07-26):** a raised surface (above the Y-40 slime
    cutoff, §33.3) and a reachable slime-spawning cavity aren't mutually
    exclusive — no new "void"/"air" layer type is needed, since `flat`'s
    layer list already accepts any block id, including `minecraft:air`,
    for any entry, not just the top. An `air` entry placed below Y 40 and
    sandwiched between solid floor/ceiling entries carves a hollow,
    enclosed cavity purely through existing layer-stack configuration —
    verified against `fillFlatColumns`/`flatBaseHeight`'s own top-down
    opaque-block scan, which already handles a non-opaque middle layer
    correctly. See README's Flat challenge section for the worked example
    and `config/tests/93-flat-slime-cavity.yaml`.

### Minecraft World Limited Size:

17. Limit world size to certain number of blocks (square radius) or chunks. This is independent of above use cases - so vanilla generation or any of the above cases would still be impacted. Option to force access to blaze rods and end portal so game remains beatable. Option to carry world size to Nether and End - but must be large enough to defeat the game. Thus, really small sizes should be overridden in End to allow access to kill the Ender Dragon.
18. Option to have world outside defined size to be hard set (no generation/void) or infinate natural generation based on seed - but invisible wall prevents access to world beyond defined size.

   **Clarification (2026-07-18):** the border's *look* and its *enforcement*
   are independent options (see DESIGN §22). Visuals: the vanilla striped
   wall, or a truly **invisible border** (no wall texture, no red warning
   vignette — feasibility verified, small client mixin + existing warning
   setters), optionally paired with a one-block **marker ring** on the
   surface just beyond the boundary so the edge is findable without a giant
   striped curtain (static borders only — generation-time). A static
   soft-void edge (no border at all, terrain just ends) is already
   expressible today: border disabled + void exterior with an explicit
   boundary radius — the static special case of 38.

39. Damage-enforced soft border ("the world hurts you back"): the player
   *can* walk beyond the world edge — like swimming underwater, minus the
   meter. Crossing the edge posts a chat warning and starts a grace period;
   when it expires the player takes periodic damage until they return
   (grace resets instantly on re-entry — health is the meter, eating is the
   recovery cost, so hunger naturally prices abuse). A screen tint reddens
   as the grace runs out and stays red while taking damage, replacing the
   striped wall as the danger signal. Border damage is its own damage type
   (custom death message); vanilla Protection reduces it (capped well below
   100%), and a custom enchantment can slow the damage and optionally
   extend the grace period — but **no combination may ever grant complete
   immunity to being outside the border** (Jason, 2026-07-18). Only makes
   sense with non-void exteriors (natural generation per 18, or ocean —
   swim out and race back); composes with the expanding/collapsing
   schedules (19–20), e.g. a collapsing world where lingering outside the
   shrinking edge hurts. See DESIGN §22.

### Expanding/Collapsing World:

19. Similar to 16, but world size becomes starting world size. World expands based on number of days in game, after an initial starting delay. Both the rate of expansion, number of blocks radius/chunks, and initial delay are configurable.
20. World contracts - similar to 19, but in reverse. Default should be a much larger delay to allow exploration of world before the collapsing begins. There should be a minimum size (blocks or chunks) of the world. The starting location should be the center of the world - so any build there should be safe.

   **Clarification (2026-07-18):** 19–20 cover two resize styles, both wanted.
   *Continuous* — the border slides smoothly at the configured rate until the
   final size (what Phase 5 shipped; Jason confirmed keeping it — compelling
   for the collapsing challenge). *Stepped* — the border jumps abruptly by X
   blocks/chunks every Y days (e.g. spawn in an 8-block radius, then +1
   block/day up to 1024; or 1024 shrinking −2 blocks/day down to 32). Chosen
   per schedule via a `resizeStyle: continuous | stepped` field reusing the
   existing rate fields; steps snap instantly. A future `resizeCurve` option
   (rate easing off near the final size) is approved future scope, deferred —
   see DESIGN §21.

38. Soft border via the void: instead of the invisible-wall border, the
   expanding/collapsing edge is represented by terrain simply ending — void
   beyond the current size, nothing physically stopping the player from
   walking off the edge and falling out of the world. As the world expands,
   real terrain backfills the previously-void ring (the world is "revealed"
   over time); as it collapses, terrain outside the shrinking edge falls away
   to void. Backfill overwrites anything built in the void ring (documented
   challenge rule: the void is unclaimed). Feasibility verified 2026-07-18 —
   possible, but expansion requires chunk-regeneration backfill (the hardest
   machinery proposed so far); a design/prototype spike is mandatory before
   implementation. See DESIGN §21.

### Structure Options:

21. Default should be for structure to generate in natural locations and Y levels.
22. Flat worlds with deep enough layers should have option to have underground structures generate below surface level so they aren't floating and too easy to find.

    **Clarified (2026-07-26):** a related but separate footgun to depth —
    some vanilla structure sets are also biome-gated (e.g.
    `minecraft:ancient_cities` only ever places in `minecraft:deep_dark`,
    README's own already-documented case). Since `flat` reports one single
    fixed biome everywhere, an explicitly configured `structureOverrides`
    entry the configured `biome` can never satisfy used to fail silently —
    accepted as "eligible" but never actually placed, with nothing telling
    the player why. Now warns automatically at world creation, checked
    directly against real vanilla structure/biome registry data
    (`Structure.biomes()`), not a hardcoded table — see README's Flat
    challenge section and `config/tests/93`/`94-flat-structure-override-
    biome-match.yaml` for the mismatched/matched pair demonstrating it.

23. Having an option for certain land structures to float high above - like Pandora in Avatar. Floating islands containing a village would be pretty cool generation - but only as an option.

    **Status:** spiked and parked (TODO 19.3, DESIGN §36.4) — needs a
    vanilla structure's bounding box known *before* terrain shaping, which
    inverts this project's terrain-then-structure pipeline. **Goal 40 below
    is a different, more tractable idea** — don't conflate the two when
    revisiting: 40 reuses this project's own existing forced-placement
    precedent (build/select the island first, force the structure onto or
    into it after — already shipped for the guaranteed village, §28.3, and
    the guaranteed stronghold/geode, §29.4/§29.6) rather than needing a
    *natural*, seed-placed structure's terrain to be known in advance.
24. Have option for structures like villages, outposts, strongholds, trail chambers, etc to generate far enough way from spawn to require a significant trip. Should be configuratable - but 2000 blocks is a good distance away.

40. **Structure islands (added 2026-07-24, Jason).** Dedicated islands that
    each exist specifically to hold one vanilla structure — a stronghold, an
    amethyst geode, a mineshaft, an ancient city, a trial chamber, an ocean
    monument, a shipwreck, a desert/jungle/other temple, and other
    structures. The island is "cased" appropriately for the structure it
    holds: encased in underground blocks for an underground structure (a
    mineshaft, ancient city, trial chamber, stronghold), water for an ocean
    structure (ocean monument, shipwreck), or a thin foundation of a few
    blocks for a surface structure (a temple). Applies broadly — Sky Chunk
    (09/37), Sky Island/Floating Islands (05-08), and their Nether variants
    (06, 27) — not just one preset (scope confirmed with Jason 2026-07-24).
    Structure islands must be treated like any other island for spacing
    purposes: the same minimum-distance-between-islands rule (see 09's
    feedback item 1) applies, so reaching one is still a genuine challenge,
    not a freebie next to the starter island. Generalizes two mechanisms
    already shipped for Sky Chunk — the guaranteed portal room (a forced
    stronghold, DESIGN §29.4) and the guaranteed geode (DESIGN §29.6) — into
    a configurable family covering more structure types, plus a casing
    concept that's new. See goal 23 for why this is a different, more
    tractable problem than the parked "Pandora" floating-structure idea.

### Cave Challenge:

25. Cave-only start — player spawns deep underground in a natural cave (configurable depth), optionally with a starter chest. Option to seal the surface so the entire game is played underground (solid roof / no sky access). Underground structures (mineshafts, dungeons, trial chambers, stronghold) generate normally so the game stays beatable; the Nether is reached via a portal built underground.
26. Same as 25, but with a mega-cave option: a huge natural-looking cavern (configurable size) around spawn — a buried "world in a cave" with room to build a base, blended into the natural cave systems at its edges.

    **Sealed-surface hardening (2026-07-25, Jason).** The sealed-surface
    roof (25) is now a configurable block (stone/deepslate/bedrock) and
    thickness, not a fixed 5-thick stone layer — a determined player could
    always just mine through stone, defeating the "entire game played
    underground" intent. **Also requested, deferred:** a genuine
    "can't surface" option beyond a thicker roof — a permeable Y-level
    damage boundary (you can climb up, but it hurts and pushes you back
    down), reusing the same design already spec'd for GOAL 39's border
    damage enforcement (DESIGN §22.3) but keyed on Y instead of horizontal
    distance. Jason's explicit call: build this **once, in a shared place
    usable by every "safe area" boundary case** (GOAL 39's own border
    damage — not yet implemented, TODO Phase 5d — and this cave case, at
    minimum) rather than duplicating cave-specific logic; recorded as a
    recommended future direction in DESIGN/TODO, not implemented yet.

### Nether-Start Challenge:

27. Player begins in the Nether instead of the Overworld (Overworld generates normally and is reachable by portal). Configurable starter chest sets the difficulty — easy includes what is needed to build a portal out (obsidian, flint and steel); harder tiers give less, but every offered tier must leave the game beatable. Respawn behavior in the Nether (respawn anchor semantics, spawn-point safety) needs design.

    **Acceptance feedback (2026-07-25, Jason, configs 59-62).** Core
    mechanic confirmed working — every config spawned him in the Nether,
    and the redirect-spawn/respawn mechanic held up. Six follow-on points,
    none designed or implemented yet:
    1. **Natural safe-site search can strand the player.** The site search
       only guarantees a single 1×2 air pocket with a solid floor and no
       lava touching that floor — it never checks whether the surrounding
       rock is open or even reachable. A "natural" site can be just as
       sealed-in as the guaranteed capsule, except without the capsule's
       guarantee that what surrounds you is minable at all (could be
       basalt/blackstone) or that breaking through won't open straight
       into lava on the other side. Sometimes the chest ends up hard, or
       in rare cases effectively impossible, to reach.
    2. **Chest-tier philosophy needs rethinking**, not just re-tuning.
       Jason's framing: easy should get the player to the Overworld fast
       (matches today's design — ready-to-use portal kit); medium should
       push the player to *stay in the Nether* long enough to gather what
       a portal needs, rather than just handing over the obsidian outright
       (today's medium is the same posture as easy minus ignition — no
       gathering required); hard should give just enough to survive long
       enough to gather Nether resources (today's hard is close to this
       already, but unconfirmed by real play). Overlaps DESIGN §31.6's own
       "first-pass, not signed off" flag.
    3. **New feature request: constrain/specify the starting Nether
       biome.** No config exists for this today — a config can currently
       land a player in a vast basalt delta, which is brutally hard even
       on the easy chest tier.
    4. **Documentation gap:** building a portal from the Nether often
       drops the player into a cave in the Overworld (ordinary vanilla
       portal-placement behavior) — worth a warning in the docs so it
       doesn't read as a bug.
    5. **New feature request: capsule as an explicit option for any
       tier**, not just the natural-search fallback — a ready-made small
       base in the Nether, available on request regardless of whether a
       natural pocket exists. **Superseded/expanded by goal 41 below
       (2026-07-25 follow-up)** — Jason wants this available for every
       world type/starting scenario, not just Nether-start.
    6. **Capsule is too small to be usable, and spawns pitch dark.**
       Interior is a single 1×1 column, 2 blocks of headroom (the
       "3×3×4" shell is almost entirely wall/floor/ceiling) — the player
       spawns standing directly on top of the chest with no room to move
       at all. Needs configurable size options (tiny/small/medium/large,
       or a player-specified size). Confirmed in code
       (`NetherStartDeployment.buildNetherStartCapsule`, and its two
       siblings `SpawnOriginManager.buildCaveCapsule`/
       `EndStartDeployment.buildEndPlatform`): none of the three place
       any light source at all, so every guaranteed-capsule spawn today
       starts the player in complete darkness. See goal 41 for the
       shared fix (this applies to all three capsule builders, not just
       Nether-start's).

41. **Universal starter capsule/base, and lit capsules (added 2026-07-25,
    Jason).** Two related requests, both cross-cutting — not scoped to
    Nether-start:
    1. The guaranteed-capsule mechanic (today: `SpawnOriginManager
       .buildCaveCapsule` for Cave, `NetherStartDeployment
       .buildNetherStartCapsule` for Nether-start, `EndStartDeployment
       .buildEndPlatform` for End-start — each only fires as a
       last-resort fallback when its own natural-site search comes up
       empty) should be offered as an explicit, requestable option for
       **every** world type and starting scenario, not just the three
       presets that already have a private fallback shape, and not only
       as a fallback — a genuine "spawn with a ready-made small base"
       toggle usable on its own terms (e.g. Ocean Island, Sky Island,
       Sky Chunk, Single-biome — any preset with an otherwise-uncertain
       or hazardous immediate surroundings). Likely needs promoting the
       three existing private capsule builders into one shared,
       parameterized mechanism (material, size, doorway) rather than
       three independent copies of the same shell-carving loop — mirrors
       this project's own precedent for consolidating duplicated
       mechanics (e.g. GOAL 39's shared safe-area-boundary request).
    2. **Every capsule spawns pitch dark today** — confirmed in code,
       none of the three existing capsule builders place a light source.
       Whatever shape the capsule mechanic ends up taking (goal 41.1, or
       the narrower per-preset fixes at goals 25/26/27), it should place
       light by default (e.g. a lantern/glowstone in the ceiling or
       floor) so the player never spawns unable to see.
    **First pass implemented (2026-07-25, Nether-start only, 0.3.2,
    DESIGN §31.9, TODO Phase 14b).** Jason confirmed the shared-mechanism
    question for now: build it directly in `nether_start` first, revisit
    `cave`/`end_start` and true cross-preset sharing later rather than
    designing that ahead of time. `netherStart.forceCapsule` requests the
    capsule explicitly (41.1); `netherStart.capsule.*` configures size,
    light source (`torch`/`lantern`/`soul_lantern`/`glowstone`/
    `shroomlight`/`glow_lichen`, appropriately Nether-default `glowstone`),
    and light spacing (41.2); every chest tier guarantees a pickaxe;
    furnace + crafting table are always placed once the room is big
    enough. **2026-07-26 follow-up:** the capsule is now the automatic
    default (not just biased-toward) whenever `spawnY` is close enough to
    the Nether's own floor or ceiling that natural pockets are inherently
    rare — no `forceCapsule` needed for cases like config 62 — per
    Jason's own "using the capsule should be the default behavior in
    cases like [config 62]" (TODO 14b.4, DESIGN §31.9). His broader
    "or in really dangerous scenarios unsuitable for a fresh start" isn't
    fully covered — only Y-proximity is a modeled danger signal today;
    biome-based danger (e.g. basalt deltas) is still the separate,
    undesigned 2026-07-25 feedback point 3 above. Not yet on the
    Customize screen (config-only), not yet generalized beyond
    Nether-start. **Fixed (0.3.3, TODO 14b.5):** Jason's first in-game
    retest found the resolved capsule site was ignored entirely, spawning
    him far above it — vanilla's own `PlayerSpawnFinder` was never
    trusted for `nether_start` the way it already was for `cave`, only
    surfacing now that the capsule became the common case.
    **Follow-up fixed (0.3.4, TODO 14b.6):** his second real in-game
    retest (once the spawn placement itself was fixed) found the chest
    still underfoot with furniture scattered instead of "along one wall
    - centered", wall lights landing at uncentered offsets instead of
    "in the middle of the walls", and the default room too small ("5x5
    and 3 tall as seen from inside"). All three fixed — furniture now
    lines the south wall together; each wall lights independently,
    centered on its own span; default capsule widened to a 5x5 interior;
    plus a new size threshold (6x6 interior) past which ceiling/floor
    lights join the walls. **[Jason] retest still outstanding** (TODO
    14b.3, configs 62/86/87 — delete the pre-0.3.4 worlds first).
    **Generalized to `end_start` (0.3.5, TODO 15.3, 2026-07-25):** Jason's
    first real in-game test of config 63 (goal 34) found no chest at all
    and asked for "a larger platform" with "the chest to one side" plus
    "the starter base thing from the nether... implemented here as an
    option" — exactly this goal's own "revisit... end_start... later"
    deferral. Investigating the missing chest found a real bug, not just
    a size complaint: `end_start`'s guaranteed platform is a fully sealed
    shell, and it had been deliberately left out of `PlayerSpawnFinderMixin`'s
    trusted-suggestion list on the (wrong) theory that the End's mostly-
    void surroundings would let vanilla's own same-column heightmap search
    naturally re-find the platform's floor; it actually hits the sealed
    *roof* first, landing the player standing on top of the box instead of
    inside it — the chest was never missing, just sealed inside a room the
    player never actually entered. Fixed by adding `end_start` to that
    mixin's trusted list (matching cave/Nether-start). Generalized the
    capsule mechanism itself in the same pass: `endStart.capsule.*`
    (`sizeBlocks`/`heightBlocks`/`lightSource`/`lightSpacingBlocks`,
    `EndStartPlan`/`EndStartConfig` mirroring `NetherStartPlan`/
    `NetherStartConfig`'s own fields, duplicated rather than shared per
    this goal's own "true cross-preset sharing later" precedent), default
    platform widened from the original 1x1-interior shape to a 5x5
    interior (matches Nether-start's own now-current default), lit by
    default (glowstone), chest moved off the original underfoot placement
    to line one wall to the side once the room is big enough to have one
    (falls back to underfoot only at the smallest, 1x1-interior size,
    which has no side wall to line). No furnace/crafting table unlike
    Nether-start's own furnished capsule -- End-start's chest tiers need
    no smelting or crafting to begin bridging/hand-mining, so Jason's
    "starter base thing" ask is read here as the shape/lighting mechanism,
    not the furniture. Config-only for now, same Customize-screen
    deferral as Nether-start's own capsule (GOALS 41.1). New config 88
    exercises a custom size/height/lantern-lighting combination. Full
    multiloader build green. **[Jason] retest outstanding** (configs
    63-65, 88 — delete any pre-0.3.5 End-start worlds first, the spawn-
    placement fix changes where a new player actually lands).
    **Follow-up fix (0.3.6, same retest):** Jason confirmed the
    spawn/platform/chest fix above but found none of the chest tiers
    could actually get him out of the platform — "mainly need a pickaxe
    to break out of the starting box". Confirmed against real decompiled
    vanilla source: End Stone has `requiresCorrectToolForDrops()` set and
    sits in the `mineable/pickaxe` tag (any tier works) but isn't
    hand-minable — bare-hand mining breaks it with no drop, same as
    Stone. This project's own "minable by hand, no tool required"
    assumption (GOALS 34's original wording, carried into README/
    MANUAL_TESTING/several config comments) was wrong the whole time.
    Fixed by adding a guaranteed pickaxe to every tier, escalating with
    the rest of each tier's gear (Jason's own choice over a single shared
    pickaxe): hard = wooden, medium = stone, easy = copper. All "minable
    by hand" doc claims corrected to describe the guaranteed pickaxe
    instead (historical decision-log entries left alone -- they record
    what was believed at the time). **[Jason] retest outstanding** on
    configs 63-65 to confirm the pickaxes actually work.

### Lava-Ocean Challenge:

28. Same shape as the ocean island challenge (01/04), but the endless ocean is lava instead of water. The island remains a normal land biome with a transition shore. Consider travel (no boats — striders/bridging) and fire hazards near the shore. Nether and End unchanged; game beatable.

### World-Hazard Rules (composable with any world type above):

29. Rising lava floor — after a configurable delay (in-game days), a world-wide lava level rises from a configurable starting depth at a configurable rate (blocks per days), stopping at a configurable maximum level (sensible default). Design decides exactly which blocks convert (air/water below the level) and how existing and newly loaded chunks are handled without performance problems.
30. Forever night — either the world starts at permanent night, or night becomes permanent after a configurable number of days. Once active, sleeping cannot skip the night and time stays at night. Consider phantom/insomnia pressure (option to keep or relax vanilla rules).

### Dry-World Challenge (approved 2026-07-16):

31. Oceans generate as drained, empty basins (exposed sand/gravel/stone floors); water is scarce. Configurable difficulty for finding water: by default, water still appears where it naturally spawns as part of structures and features (village farms and wells, strongholds, aquifer pockets, springs); harder settings remove more (e.g. no rivers or surface lakes). Strong lore-driven challenge potential. Beatability note: potions and other water-dependent progression must remain obtainable at every offered difficulty.

### Strip-World Challenge — 1D Minecraft (approved 2026-07-16):

32. The world is a narrow strip (configurable width in blocks or chunks) running along one axis; everything happens in that corridor. The stronghold/End portal must be reachable within the strip (progression guarantee — the existing fallback-portal machinery applies). Optionally apply the strip to the Nether as well. Composes with limited length (17) and the expanding/collapsing schedules (19–20).
36. Same as 32, but the strip passes through an ordered (or seed-randomized) sequence of biomes, changing every N chunks — plains for N chunks, then desert, then taiga, and so on. Bands select biomes over untouched vanilla terrain (like 33, but ordered instead of random), so the terrain stays natural. Band width, biome list, and order are configurable. (Added 2026-07-16 as a variation Jason suggested alongside 35.)

### Chaos-Biomes Challenge (approved 2026-07-16):

33. Biome assignment is shuffled randomly (seed-based) per region over vanilla terrain — deserts beside ice spikes beside jungles. Configurable size for each random biome region. Terrain shape stays vanilla; option to keep natural rivers/oceans where vanilla put them.

### End-Start Challenge (approved 2026-07-16):

34. Player spawns on the outer End islands with a solid starter chest that makes surviving in the End genuinely possible (food, building blocks, tools — tuned so reaching and defeating the Ender Dragon is achievable). Must be beatable in hardcore, even if really hard. Respawn design needs care (beds explode in the End; no respawn anchors) — shares the Nether-start (27) spawn/respawn design work.

### Stacked-Biome-Layers Challenge (added 2026-07-16):

35. A limited-size world (blocks/chunks, per 17) where the underground is replaced by stacked horizontal biome layers instead of normal caves — plains above desert above taiga, and so on, each layer a horizontal slab with its own surface. Layer list, order, and thicknesses configurable, with a seed-randomized option. Must account for ores that normally require deep levels (lapis, gold, diamond) — e.g. distribute an ore budget across the layers or expose config. Stronghold/End-portal placement within the stack needs design so the game stays beatable.
    **Clarified 2026-07-16:** each layer is a flat or low-relief slab — a thin slice through the world can't fit multiple "extreme hills"-style biomes, so layers mainly use the flatter variants of their biome. This ties the feature to the flat-world layer machinery (15–16) rather than full noise terrain per layer. There must be an **air gap above each layer's surface** (configurable headroom) so biome-specific trees and structures can generate and grow on every layer, not just the top one.

### Biome Fidelity (added 2026-07-26, Jason):

42. **Surface biomes and underground biomes should coexist the way vanilla
    does today — cave biomes must never be reported at the surface.** In
    real vanilla the two are tied together (deep dark appears *under*
    extreme hills and mountains, not instead of them); the goal is that
    same relationship, not a separate underground-only world type. The
    concrete, already-observed defect is cave biomes appearing **on the
    surface** in the `legacy` layout mode (see 44). For world types with a
    synthetic surface at a known Y — the flat family and sky islands — the
    boundary can simply be "N layers below the surface starts underground"
    (Jason's own suggestion, ~10). For real noise terrain with varying
    height, a reasonable low point decides what still counts as surface,
    **except** ravines and pits, which should legitimately read as
    underground the way they do in vanilla.

    **Feasibility confirmed (2026-07-26)** against the real 26.2 sources:
    vanilla already separates the two purely through the `depth` climate
    parameter (`OverworldBiomeBuilder.addSurfaceBiome` registers each
    surface biome at depth `0.0` *and* `1.0`; `addUndergroundBiome` uses
    depth `span(0.2, 0.9)`; `addBottomBiome` pins deep dark at `1.1`), and
    that value is already available at this project's own biome-query call
    site via `Climate.Sampler.sample(...).depth()`. Because vanilla's depth
    is derived from the terrain-shaping density function rather than a flat
    Y cutoff, it *already* treats a ravine floor as genuinely deep — the
    ravine/pit carve-out Jason asked for falls out for free rather than
    needing special-casing. See DESIGN §37.

43. **Multiple surface biomes in one world, with each column's surface
    painted in that biome's own top block** — plains grass, desert sand,
    water for river/ocean, and so on — so that **biome-specific surface
    structures** (desert temples, jungle temples, shipwrecks, ocean
    monuments, igloos) can actually generate instead of being silently
    biome-gated out of a single-biome world. Scoped (Jason, 2026-07-26) as
    an additive option on the existing `flat` and `deep_flat` presets
    rather than a new preset or a sky-island change.

    **Feasibility confirmed (2026-07-26):** half of this already exists —
    `deep_flat`'s own cap pass already makes a per-column, biome-aware
    surface decision (real water where the underlying biome is river/ocean
    tagged, land layers otherwise, DESIGN §33.4), and `StackedBiomeDefaults`
    already maintains a pure biome-to-block-composition table covering ~35
    vanilla biomes with a generic fallback. The missing pieces are a
    per-column biome source for `flat` and promoting that table to shared
    code. Structure eligibility then needs no new work at all: placement is
    already gated on the biome this project reports per column, which is
    exactly the mechanism the 0.3.12 `structureOverrides` warning was built
    against. See DESIGN §38.

44. **The `legacy` layout mode is badly named and buggy — rename it and fix
    it.** It is the *default* `layout.mode`, so "legacy" is actively
    misleading: it describes a mode most worlds are actually using. Decision
    (Jason, 2026-07-26): **rename to `climate_filter`** — an honest
    description of what it really does (filter vanilla's own climate table
    down to an allowed-biome list) — keep `legacy` parsing as a deprecated
    alias so existing configs and saved worlds keep working, and fix its
    bugs in place rather than extracting its good parts and deleting it.
    Known defects to resolve or honestly document: cave biomes on the
    surface (42), and biome labels that disagree with the terrain actually
    generated beneath them (the already-documented ocean-reported-as-river
    case). See DESIGN §39.

## Considered and rejected (2026-07-16):

- **Glowing ores option** — proposed as an off-by-default world-generation
  setting to make ores easier to see in caves. Rejected for worldz: ore
  *placement* wouldn't change, only appearance, so it isn't world generation.
  Emissive-texture resource packs deliver the visual with no code and work in
  any world; actual light emission is a code-level block property (not
  per-world data) and would suppress mob spawning around every vein — a
  gameplay side effect this mod shouldn't own. If it ever becomes a mod
  feature, jlt_ores is the natural home as a mod-wide client option.

## Questions:

1. Depending on ocean biomes, do small islands naturally happen. If so, then after the starter island (if one is generated), use ocean and deep ocean to prevent islands from generating until beyond exclusion zone.

   **Answer (2026-07-16):** Biome choice and terrain shape are independent in
   modern Minecraft — restricting biomes to ocean/deep-ocean does **not**
   prevent land; full continents still generate, just labeled as ocean biomes
   (confirmed in-game during the Worldz5/6 tests, see MEMORY.md). Endless
   ocean therefore requires the terrain height-cap Worldz already has, and
   distant natural islands (use case 04) come from *releasing* that cap
   beyond the exclusion zone so the seed's natural terrain resumes. Small
   natural islands do occur in vanilla ocean regions (terrain noise poking
   above sea level), which is what makes use case 02's "find a natural
   island" search plausible. See DESIGN.md §20.5/§20.7.

## Testing and Automation

### Test Configurations

- Need to create a configuration file per test case. Perhaps multiple per phase.

### Automation:

- Point to Instance in Prism for managing mods - automatic replacement of existing mod to speed up manual testing.
- Review logs, world information, and screenshots.

### Advice to user:

- what additional mods should be added to aid in manual testing?

  **Answer (2026-07-16)** — recommended test-aid mods for the Fabric
  instance (check each has a 26.2 build in Prism before adding):
  - **Xaero's World Map + Minimap** — the single biggest time-saver here:
    biome layout, island shapes, and coastlines verifiable from the map
    instead of flying everywhere.
  - **MiniHUD** — overlays for light level (slime checks), biome/coords, and
    structure bounding boxes (needs its structure data channel; works in
    singleplayer).
  - **Chunky** — pre-generate a radius around spawn, then inspect the map;
    catches distant-terrain defects (like the past stranded-village bugs)
    in minutes instead of long survival flights.
  - **Spark** — only if generation feels slow; profiles worldgen cost.
  - Vanilla commands cover the rest: `/locate biome`, `/locate structure`,
    `/fill`, spectator mode. External seed maps (e.g. Chunkbase) help
    compare natural vs. modded placement for the same seed.