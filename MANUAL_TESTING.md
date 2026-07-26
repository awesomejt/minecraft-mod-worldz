# Manual test strategy

`./gradlew build` proves the code compiles and the pure logic is correct; it
never launches Minecraft. Every phase to date has deferred actual in-game
verification, so this is the first pass at a repeatable process for doing
that in a controlled way — start here, and extend the scenario tables below
as new features land.

## Principles

- **One variable at a time.** Change exactly one config value (or one
  Customize-screen field) between runs. If two things differ between a
  passing and failing run, you haven't isolated anything.
- **Fresh world per scenario.** Worldz decisions (spawn strategy, layout
  origin, border, exterior) are baked in at world creation and never
  re-evaluated. Reusing a world across scenarios silently tests stale state.
  Delete the world folder (or rename it aside) before each new scenario.
- **Fixed seed for anything you want to compare or repeat.** A blank/random
  seed makes two runs of "the same" scenario incomparable. Pick one seed (e.g.
  `12345`) and reuse it for every scenario in a session unless a test is
  specifically about seed independence.
- **Both loaders.** Fabric and NeoForge share `common` but wire events
  differently (`ServerLevelEvents.LOAD` + a mixin on Fabric,
  `LevelEvent.Load`/`CreateSpawnPosition` on NeoForge). A bug in one loader's
  wiring won't show up on the other. Run the matrix on both unless a scenario
  is explicitly loader-agnostic.
- **Read the log, not just the screen.** Worldz logs a `WARN` for every
  fallback (unresolved starter biome, search radius exhausted, invalid
  config value, etc.) via SLF4J. A fallback that "looks fine" in-game but
  logged a warning is still a bug worth noticing.

## Environment

Dev run directories (created by Loom / NeoGradle, gitignored):

| Loader | Client | Server |
|---|---|---|
| Fabric | `fabric/runs/client/` | `fabric/runs/server/` |
| NeoForge | `neoforge/runs/client/` | `neoforge/runs/server/` |

Launch with:

```bash
./gradlew :fabric:runClient      # or :neoforge:runClient
./gradlew :fabric:runServer      # or :neoforge:runServer
```

Config file per run directory: `<rundir>/config/jlt_worldz.yaml`. It is
optional — absent means the mod's built-in defaults apply directly, and the
mod never creates the file for you (see
[`config/jlt_worldz.example.yaml`](config/jlt_worldz.example.yaml) for the
documented reference to copy from). Worlds live in
`<rundir>/saves/<world-name>/` (client) — delete the folder for a clean
slate. A dedicated server's single world is `<rundir>/world/`.

### Testing against a real Prism instance instead of the dev client

Jason tests Fabric against a dedicated Prism Launcher instance rather than
the dev client (it carries the recommended test-aid mods — Xaero's
world/minimap, Chunky, Spark; MiniHUD is disabled since `jlt_info` covers
the same ground). Deploy a freshly built jar to it with:

```bash
./gradlew :fabric:deployToPrism
```

This copies the built Fabric jar into the instance's `mods/` folder,
replacing any older `jlt_worldz` jar there. It reads the instance path from
`prism.instance.dir` in a git-ignored `local.properties` file at the repo
root (create one if missing — never committed), or accept it ad hoc with
`-PprismInstanceDir=<path>`. Config file and worlds/logs for that instance
live under `<instance>/minecraft/` (Prism's own layout), following the same
"config is optional, delete the world for a clean slate" rules as above.

Log file: `<rundir>/logs/latest.log`. Grep for the mod's warnings:

```bash
grep -i "worldz\|preferred_natural_biome\|starter_at_origin" <rundir>/logs/latest.log
```

F3 debug screen gives you: block position, biome id, and (F3+G / vanilla
chunk borders) the current world seed if unsure — all needed to verify
recentering and strategy behavior below.

## Phase 1 acceptance (2026-07-16 challenge-world replan, TODO 1.1/1.7)

Everything below is unverified in-game as of 0.2.0. Use the dedicated Prism
instance (see above) with a fixed seed (e.g. `12345`) and a fresh world per
scenario, per the principles above. None of this needs the dev client unless
you prefer it.

### 1. Dummy-RandomState fix (TODO 1.1)

Shipped in 0.1.15, carried into 0.2.0, never confirmed in-game. This is the
most important check in this pass — it affects the terrain shape of every
Worldz world regardless of layout mode.

1. Create a world with any config (e.g. `07-legacy-regression-baseline.yaml`,
   or no config file at all for pure defaults).
2. Go to spectator mode and drop below Y-64, well outside the starter zone.
3. **Pass:** ordinary bedrock at the bottom, and normal winding cave systems
   (not huge open gaps, not near-total lava). This is the exact failure
   pattern described in `MEMORY.md`'s dummy-RandomState entry — compare
   against that description if anything looks off.
4. Repeat step 1-3 briefly on **NeoForge** (`./gradlew :neoforge:runClient`
   or a NeoForge Prism instance if you set one up) — this is the first
   NeoForge-side mixin this project has shipped, so it needs its own check,
   not just an assumption that "Fabric passed so NeoForge did too."
5. While you're down there: do the Worldz14 orange/glitchy-terrain screenshots
   reproduce? They were never explained and are plausibly the same root
   cause. Note yes/no either way — a "no" closes out the question, not just
   a "not tested."

### 2. Removed-mode regression + default generation (TODO 1.7)

1. **Default-config world.** No `jlt_worldz.yaml` present (delete it if the
   instance already has one) — confirms defaults apply without a file at
   all (TODO 1.4). Create a world, confirm normal cave/structure generation,
   and open **Customize → Layout**: confirm the mode cycle button only ever
   shows `legacy` / `ocean` / `single_biome` / `void` — never `land_only` or
   `mixed`.
2. **Single-biome-style world**, using
   `08-single-biome-regression.yaml` (`layout.mode: single_biome`, desert).
   Confirm the whole world is desert-shaped terrain (raised to the land
   role), normal structures/caves still generate, and the same seed
   reproduces the same result on a second world (TODO 1.3's real-seed
   change).
3. **Void mode**, using `09-void-regression.yaml`. Confirm a solid starter
   island floats in sky void, sized to the starter radius plus its land
   transition.
4. **Ocean mode / recentering**, using
   `06-preferred-natural-biome-recentering.yaml` (now `ocean` instead of the
   removed `mixed`). Confirm border/exterior/layout all still recenter on
   the found origin together (see the existing recentering checklist below)
   — this exercises the exact code path Phase 1.2/1.3 touched.
5. **Legacy baseline**, using `07-legacy-regression-baseline.yaml`, as a
   known-good control to compare the above against.

## Phase 2 acceptance (2026-07-16 challenge-world replan, TODO 2.7)

Uses configs `10`-`13` (see [`config/tests/README.md`](config/tests/README.md)).
**Configs `10`-`12` need the new "Worldz: Single Biome" entry in the World
Type dropdown** — a separate preset from plain "Worldz", with its own small
Customize screen (land biome / starter biome / starter radius / spawn
strategy only; no border/exterior/starter-land controls here yet — those
stay YAML-only for this type until Phase 5.3). Config `13` uses plain
"Worldz" as a control. Fixed seed recommended for all four.

1. **Basic single biome (GOALS 10)**, `10-single-biome-basic.yaml`. Create a
   world, confirm the entire explored area is desert (F3 biome id), normal
   structures/caves/features generate (compare against the Phase 1.1
   dummy-RandomState check — same underlying terrain pipeline), and no
   starter-zone seam is visible anywhere (there is none to see: the whole
   world is already the land biome).
2. **Different starter biome (GOALS 11)**, `11-single-biome-different-starter.yaml`.
   Confirm spawn is inside a ~256-block plains circle, desert begins beyond
   it, and the transition is blended (starter land guarantee), not a hard
   cliff.
3. **Seed-chosen starter (GOALS 12)**, `12-single-biome-seed-chosen-starter.yaml`.
   Confirm spawn lands inside a *naturally occurring* plains patch found via
   search (not forced at `(0,0)`) — note the coordinates. Recreate the world
   with the same seed and confirm the same location is found again (real-seed
   reproducibility, TODO 1.3). If no natural plains patch exists within the
   search radius for your seed, confirm the documented fallback: spawn at
   `(0,0)` behaving like `starter_at_origin`, with a "search found no biome"
   warning in the log.
4. **Vanilla-limited control**, `13-vanilla-limited-baseline.yaml`. Confirm
   this looks and feels like ordinary vanilla Minecraft (climate-filtered
   biomes, no single-biome uniformity) ringed off by a 1000-block border —
   this is the regression guard for the shared border/limit path, which now
   flows through the same `LimitedBiomeSource.resolve()` method as the new
   `singleBiome:` branch.
5. **Per-world snapshot (TODO 2.4).** After any of the above, check the new
   world's save folder for `jlt_worldz-snapshot.yaml` (sibling to
   `level.dat`). Confirm it exists, is readable, and its values match what
   you actually selected — this file is a reference record only; editing it
   does nothing.
6. **Customize-screen sanity.** Open "Worldz: Single Biome"'s Customize
   screen before creating a world: confirm it shows land biome/starter
   biome/starter radius/spawn strategy, plus Overworld/Nether Border, End
   Border, and Overworld/Nether Exterior buttons — none of the full
   "Worldz" preset's allowed-biomes list or layout-mode controls. (Border/
   exterior/End Border buttons were added in Phase 5.3 — see TODO.md's
   Deviation log; this item originally required their absence too, before
   that phase existed.)

## Phase 3 acceptance (2026-07-17 challenge-world replan, TODO 3.2)

Uses configs `14`-`15` (see [`config/tests/README.md`](config/tests/README.md)).
Both need **"Worldz: Single Biome"** on the creation screen. A fixed seed
with a known nearby river and ocean makes this much easier to check —
[Chunkbase's biome finder](https://www.chunkbase.com/apps/biome-finder) (or
similar) against the same seed, checked in plain vanilla first, gives exact
coordinates to fly to before checking the same spot in Worldz.

1. **Rivers pass through (GOALS 13)**, `14-single-biome-allow-rivers.yaml`.
   Locate a river in plain vanilla at this seed, note its coordinates, then
   create the Worldz world at the same seed and fly there: confirm a river
   (F3 biome id `minecraft:river` or `minecraft:frozen_river`) actually
   exists in the same place, with the same natural channel shape — not the
   configured desert. Everywhere else (and outside the river) should still
   be desert. Confirm the starter zone (within ~256 blocks of spawn) is
   never a river even if vanilla would have put one there — starter land
   stays guaranteed.
2. **Oceans pass through too (GOALS 14)**, `15-single-biome-allow-oceans.yaml`.
   Same idea at an ocean location instead: confirm the ocean (any of
   vanilla's ocean biome variants — warm/lukewarm/cold/frozen, deep or not)
   generates naturally, with an unmodified vanilla coastline — no straight
   edges, no visible height blending at the shore. Rivers should still pass
   through too (`allowRivers` stays on alongside `allowOceans`).
3. **Per-world snapshot.** Check `jlt_worldz-snapshot.yaml` for either world:
   confirm it now includes `allowRivers`/`allowOceans` matching what the
   config file set.

## Phase 4 acceptance (2026-07-18 challenge-world replan, TODO 4.2)

Uses configs `16`-`19` (see [`config/tests/README.md`](config/tests/README.md)).
All four need **"Worldz: Chaos Biomes"** on the creation screen.

1. **Default regions (GOALS 33)**, `16-chaos-biomes-default.yaml`. Fly
   around and confirm: (a) multiple different biomes appear (desert,
   jungle, ice_spikes, badlands, taiga) in distinct regions, not one biome
   everywhere; (b) terrain shape is completely ordinary vanilla — hills,
   mountains, ravines, natural water bodies all look exactly like a normal
   vanilla world, just with an unusual biome label on top (e.g. a jungle
   region can still have a natural lake or a snowy-looking valley if
   that's what the terrain naturally does there — the biome doesn't match
   the terrain the way vanilla normally correlates them, and that's
   expected for this config since rivers/oceans pass-through is off); (c)
   normal structures/caves/features generate.
2. **Pass-through + starter zone**, `17-chaos-biomes-pass-through-and-starter.yaml`.
   Tested here, right after the default-regions check and before the
   region-size variations, since it's the config that actually confirms
   real rivers/oceans work under chaos biomes. Confirm spawn is inside a
   ~256-block plains circle (the starter zone), with chaos biomes starting
   beyond it. Using the same vanilla-first coordinate-finding approach as
   Phase 3's rivers/oceans check, confirm a known vanilla river or ocean
   location (outside the starter zone) still generates as a real
   river/ocean here too, with natural terrain — not flattened, not
   replaced by one of the configured land biomes (same mechanism as Phase
   3.1, just confirming it composes correctly with `chaos_biomes`).
3. **Tiny regions**, `18-chaos-biomes-tiny-regions.yaml`. Confirm biome
   changes noticeably more often while flying in a straight line than
   config 16 — regions should feel small and frequent.
4. **Huge regions**, `19-chaos-biomes-huge-regions.yaml`. Confirm the
   opposite — long, uninterrupted stretches of one biome before it changes.
5. **Customize-screen sanity.** Open "Worldz: Chaos Biomes"'s Customize
   screen before creating a world: confirm it shows the weighted biomes
   list, region size, starter biome/radius, spawn strategy,
   allowRivers/allowOceans checkboxes, and Overworld/Nether Border, End
   Border, and Overworld/Nether Exterior buttons — nothing from the full
   "Worldz" preset's allowed-biomes list or layout-mode controls. (Border/
   exterior/End Border buttons were added in Phase 5.3 — see TODO.md's
   Deviation log; this item originally required their absence too, before
   that phase existed.)

## Phase 5 acceptance (2026-07-18 challenge-world replan, TODO 5.4)

Uses configs `20`-`23` (see [`config/tests/README.md`](config/tests/README.md)),
all on the plain **"Worldz"** preset (this phase is about the shared
border/exterior/limit mechanism itself, not any particular world type).

1. **Static small border, invisible wall vs. void**, `20-border-static-small-invisible-wall-vs-void.yaml`.
   Confirm the border stops you (vanilla's invisible-wall effect) at
   exactly 128 blocks from spawn in every direction, terrain generation is
   completely ordinary vanilla right up to that same edge, and it turns to
   void immediately beyond it — the two systems track the same boundary
   here but are otherwise independent. Confirm a compact fallback End
   portal exists somewhere near positive X (no real stronghold fits inside
   128 blocks). **Passed 2026-07-18.**
2. **Expanding border**, `21-border-expanding.yaml`. Follow the file's own
   steps — they carry the current values and the time-skipping commands.
   (26.2 note, verified 2026-07-18: `/time add <Nd>` now works for burning
   through the *delay* — in this version `/time` advances the dimension's
   WorldClock, the exact counter the schedule reads — but it will not
   fast-forward a resize already in progress; the vanilla lerp counts down
   per real tick, so use `/tick step` to watch the border actually move.)
   Confirm: the border holds at its initial radius through the whole
   delay, then grows continuously (not in a jump) and stops exactly at
   `finalRadiusBlocks`. **Passed 2026-07-18** (after 0.2.13's radius-floor
   fix and 0.2.14's spawn-offset fix — see TODO 5.5/5.6).
3. **Collapsing border**, `22-border-collapsing.yaml`. Same `/tick step`
   approach. Confirm: the border holds at its initial radius during a
   *much longer* delay than 21's (GOALS 20's explicit ask — exploration
   time before the collapse), then collapses continuously to its final
   radius and stops there — not below it. Confirm spawn (and anything
   built there) stays safely inside the fully-collapsed border, since
   it's centered at the origin. **Passed 2026-07-18.**
4. **End border carry-over**, `23-end-border-carry-over.yaml`. Overworld
   border is pinned to the 64-block minimum on purpose. Confirm: a compact
   fallback End portal exists (Overworld border is far too small for a
   natural stronghold), and once in the End, the border there is centered
   at `(0, 0)` with radius **256** (the configured `minimumRadiusBlocks`),
   not 64 — the main island, every obsidian pillar, and the exit portal
   should all be comfortably inside it. Confirm the dragon fight is normal
   and winnable. **Passed 2026-07-18.**
5. **Blocks/Chunks unit toggle** (no config file — a Customize-screen UI
   check). Open any Border or Exterior sub-screen (from any preset's
   Customize screen) and click the **Radius units** button: confirm it
   switches between "Blocks" and "Chunks" and converts whatever's already
   typed in each radius field (e.g. 512 blocks becomes 32 chunks) rather
   than reinterpreting the same digits under the new unit.
6. **Single Biome / Chaos Biomes Customize-screen border wiring** (no
   config file). Open "Worldz: Single Biome"'s and "Worldz: Chaos
   Biomes"'s Customize screens: confirm both now show Overworld/Nether
   Border, End Border, and Overworld/Nether Exterior buttons (Phase 5.3;
   see items 6/5 of Phases 2/4's acceptance sections for what else those
   screens show) and that toggling a border/exterior/End Border setting
   there and creating a world actually applies it — e.g. enable the
   Overworld border in Single Biome's Customize screen with a small
   radius and confirm it's actually in effect in-game, the same as
   setting `overworldBorder` in `singleBiome:`-free top-level config would
   do for the plain preset. **Outstanding** — not yet confirmed, not
   treated as blocking further phase work.

## Phase 5b acceptance (stepped border resizing, TODO 5b.3)

Uses configs `24`-`25` (see [`config/tests/README.md`](config/tests/README.md)),
plain **"Worldz"** preset, same as Phase 5's own acceptance configs.
Requires 0.2.16+ (the `resizeStyle`/stepped driver did not exist before it).

1. **Stepped expanding border**, `24-border-stepped-expanding.yaml`. Follow
   the file's own steps. Confirm: the border holds at exactly 8 (radius)
   through the whole delay, then jumps by whole-block increments once per
   day — never a smooth creep, and never a fractional radius between
   jumps — until it reaches exactly 1024 and stops.
2. **Stepped collapsing border**, `25-border-stepped-collapsing.yaml`.
   Same approach. Confirm: the border holds at exactly 1024 through a
   much longer delay (exploration time, mirroring GOALS 20's continuous
   case), then jumps down by whole-block increments once per day until it
   reaches exactly 32 and stops — never below it. Confirm spawn (and
   anything built there) stays safely inside the fully-collapsed border.
3. **Contrast with continuous.** Side by side with configs 21/22, confirm
   the visual/behavioral difference is obvious: continuous borders creep
   smoothly tick by tick, stepped borders hold perfectly still and then
   snap.

## Phase 6 acceptance (strip world, GOALS 32/36, TODO 6.2c/6.3)

Uses configs `26`-`29` (see [`config/tests/README.md`](config/tests/README.md)).
**Select "Worldz: Strip World"** on the creation screen for all four, not
plain "Worldz". Requires 0.2.22+ (the dedicated preset did not exist before
it); test 5 requires 0.2.27+ (biome bands, GOALS 36, including the
fieldless-creation fix -- 0.2.24-0.2.26 needed Customize opened at least
once for bands to apply at all).

1. **Basic strip**, `26-strip-world-basic.yaml`. Confirm: ordinary vanilla
   terrain and full biome variety (this preset does not restrict biomes);
   walking along Z (the narrow axis) the terrain ends into void at roughly
   64 blocks out with no collision (you can walk off the edge and fall);
   walking along X (the long axis) the ordinary square border stops you at
   2048 blocks with vanilla's usual invisible-wall push-back; a compact
   fallback End portal exists and the dragon fight is winnable.
2. **Narrow strip, fallback-portal fix**, `27-strip-world-narrow-fallback-portal.yaml`.
   Confirm the corridor is narrow (~32 blocks either side of center) and
   the compact fallback End portal sits inside it (roughly Z=0), not at one
   of the wider candidate offsets (64/-64/128/-128) the border's own
   radius alone would have wrongly allowed pre-fix. Confirm the dragon
   fight is winnable.
3. **Nether corridor**, `28-strip-world-nether-corridor.yaml`. Confirm the
   Overworld corridor as in test 1, then confirm the Nether is *also* a
   narrow corridor (void beyond ~64 blocks in Z) with its own independent
   length border (512 blocks) and a compact fallback blaze site reachable
   inside it.
4. **Customize screen sanity.** Open "Worldz: Strip World"'s Customize
   screen: confirm the Blocks/Chunks radius-unit toggle, the Void/Ocean
   width-mode toggle, the Nether checkbox, spawn strategy, and the Border/
   End Border/Exterior buttons all work and a customized world reflects
   the chosen values in-game.
5. **Biome bands, config-only (no Customize)**, `29-strip-world-biome-bands.yaml`
   (GOALS 36). Copy the config, then go straight from selecting "Worldz:
   Strip World" to **Create World** without opening Customize at all. This
   is exactly the path that was broken through 0.2.26: bands need
   0.2.27+ to apply without ever opening Customize. Confirm terrain stays
   ordinary vanilla shape (only the biome changes); walking along +X in
   ~256-block increments the biome should cycle desert → jungle →
   ice_spikes → badlands → taiga → back to desert; walking along -X from
   spawn should show the same sequence in reverse (band index wraps via
   floor division, not mirrored oddly at X=0); confirm Z still ends into
   void at ~64 blocks out either side, unchanged from the plain strip
   world; look for a natural river, ocean, beach, or stony shore along the
   corridor and confirm it shows through as its real vanilla biome instead
   of being relabeled to the current band's biome.
6. **Biome bands, via Customize.** Same config, but this time open
   Customize before creating. Confirm the "Pass through an ordered
   biome-band sequence" checkbox, the band-biomes list, band width,
   shuffle-once checkbox, and the three river/ocean/beach pass-through
   checkboxes (all checked by default) are all present and pre-filled from
   the config, and the resulting world matches test 5's expectations.

## Phase 7 acceptance (ocean island, GOALS 01/04, TODO 7.2-7.4)

Uses configs `30`-`33` (see [`config/tests/README.md`](config/tests/README.md)).
**Select "Worldz: Ocean Island"** on the creation screen for all four, not
plain "Worldz". Requires 0.2.29+ (the dedicated preset did not exist before
it); requires 0.2.30+ for the fallback End-portal guarantee to actually
work (see [`MEMORY.md`](MEMORY.md) — 0.2.29 shipped with a real
beatability bug found and fixed before this acceptance pass, so don't
bother testing beatability on exactly 0.2.29). **Requires 0.2.32+** for a
usable fallback End portal (0.2.29-0.2.31 always built it at the world
floor, `y = -64`, encased in bedrock — confirmed via config 30's first
test pass), a non-checkerboard ocean, and coastline detail noise (config
30's first pass also found the coastline read as an unnaturally smooth
single-lobed blob and the ocean's biome patches formed a visible grid —
both fixed in 0.2.32; see MEMORY.md's 2026-07-19 "Phase 7 test-1
findings" entry). **Requires 0.2.33+** for a living ocean — 0.2.29-0.2.32
never placed vanilla decoration (kelp/seagrass), world-gen-time fish/
squid population, or structures (shipwrecks/ocean ruins/monuments)
anywhere past one chunk from shore, confirmed sterile on 0.2.32's
re-test; see MEMORY.md's 2026-07-19 "Phase 7 test-2 findings" entry.
**Requires 0.2.34+** for the fallback End portal to be a proper buried,
enclosed vault (Y-10 to Y-60, full floor/ceiling/walls/doorway) — 0.2.32
and 0.2.33 correctly reached the terrain surface but built an exposed,
unenclosed platform there; see MEMORY.md's 2026-07-19 "Phase 7 test-2
follow-up" entry. **Requires 0.2.35+** for the shore ring to read as
alternating contiguous beach/stony-shore stretches rather than a
speckled per-block mix; see MEMORY.md's 2026-07-19 "Phase 7 test-3
finding" entry.

1. **Default island**, `30-ocean-island-default.yaml`. Confirm: the
   coastline reads as natural/irregular, not a circle or square; a narrow
   beach/stony-shore ring (mix of both) sits right at the coastline, not a
   wide sandy apron; sailing outward, the ocean biome transitions from
   warm/lukewarm/plain ocean near shore to exclusively deep-ocean-family
   biomes further out (check F3 periodically); the seabed visibly deepens
   the further out you go; underground structures/caves under the island
   generate normally; a compact fallback End portal exists and the dragon
   fight is winnable; the Nether is completely ordinary, unrestricted
   vanilla generation.
2. **Tiny island (the "1 chunk" floor)**, `31-ocean-island-tiny.yaml`.
   Confirm the island is genuinely tiny (roughly one chunk across) and the
   compact fallback End portal consumes most of the surface — this is the
   documented trade-off (README's "Ocean island challenge" section), not
   a defect. Confirm the shore ring and ocean gradient still work
   correctly at this scale, and the dragon fight is winnable.
3. **Huge island**, `32-ocean-island-huge.yaml`. Use a map mod (Xaero's)
   or fly the perimeter to confirm the coastline reads as a large, natural
   landmass with clearly visible bays/headlands, not a circle and not
   visibly tiled. Spot-check the shore ring and ocean gradient at a few
   different points around the coastline. Confirm generation performance
   feels reasonable near the coastline.
4. **Distant natural islands**, `33-ocean-island-distant-natural-islands.yaml`
   (GOALS 04). Confirm the island and its gradient ocean behave like test
   1 close to the origin; travel past 512 blocks in any direction
   (spectator mode is fastest) and confirm the artificial ocean cap stops,
   the delegate's own real vanilla terrain resumes (ordinary hills/biome
   variety, and — seed-dependent — maybe a small natural island poking
   above sea level somewhere out there), and the transition itself isn't
   jarring (no visible seam, no floating terrain) right at the boundary.
   Confirm the dragon fight is still winnable.
5. **Customize screen sanity.** Open "Worldz: Ocean Island"'s Customize
   screen: confirm island biome, radius, coastline shape (amplitude),
   shore width, all five ocean-gradient fields, the exclusion-zone
   toggle/radius, and the Border/End Border/Nether Exterior buttons all
   work (note: no Overworld Exterior button and no spawn-strategy option
   — both deliberately absent for this preset, see DESIGN §24.8) and a
   customized world reflects the chosen values in-game.

## Phase 8 acceptance (ocean island extras, GOALS 02/03, TODO 8.1-8.2)

Uses configs `34`-`35` (see [`config/tests/README.md`](config/tests/README.md)).
**Select "Worldz: Ocean Island"** for both. Requires 0.2.37+ for
`islandSource: chest_boat` (34), 0.2.38+ for `islandSource: natural` (35).
Both extend the same preset config 30-33 already used (DESIGN §25.1) —
there is no separate World Type for either.

1. **Chest boat, no land**, `34-ocean-island-chest-boat.yaml` (GOALS 03).
   Confirm no land is visible anywhere near spawn. Confirm a chest boat
   exists at/very near spawn and the player starts on or immediately next
   to it. Open its inventory: confirm 2 lily pads, 6 dirt (this file's
   configured essentials), plus exactly 1 random extra (bread or a water
   bucket, this file's configured extras pool/count) — proves the YAML
   `starterKit` override actually took effect, not just the shipped
   defaults. Confirm the ocean still reads shallow near spawn, deepening
   further out (same gradient as the artificial island). Confirm a
   compact, buried, enclosed fallback End portal exists and the dragon
   fight is winnable. Confirm the Nether is completely ordinary vanilla.
2. **Natural island by seed**, `35-ocean-island-natural.yaml` (GOALS 02).
   **This search is not guaranteed to succeed on every seed** — it's
   explicitly time-boxed and unvalidated against real seeds (DESIGN
   §25.6). Check the log for either "Natural island search found..."
   (succeeded — note the coordinates) or "...using the origin instead"
   (the documented fallback, not a bug). If it succeeded: confirm the
   land within ~48 blocks of spawn looks like ordinary, unmodified
   vanilla terrain — varied biomes/elevation, no single flattened biome,
   no raised platform, real ocean beginning right at the edge of that
   radius. Confirm the ocean gradient beyond it still reads shallow near
   the coastline, deepening further out. Confirm a compact, buried,
   enclosed fallback End portal exists and the dragon fight is winnable.
   **If it's unreliable across a few different seeds**, note which
   seeds/outcomes so the isolation threshold/ring sample count
   (`NaturalIslandSearch`) can be tuned rather than redesigned.

## Phase 9 acceptance (ocean fluid variants, GOALS 28/31, TODO 9.2-9.3)

Uses configs `36`-`37` (see [`config/tests/README.md`](config/tests/README.md)).
**Select "Worldz: Ocean Island"** for both. Requires 0.2.53+ (0.2.41 for the
core lava/dry mechanism; 0.2.53 fixed lava's structure suppression below).
Both extend the same preset configs 30-35 already used (DESIGN §26.1) —
there is no separate World Type for either.

1. **Lava ocean**, `36-ocean-island-lava.yaml` (GOALS 28). Confirm the
   endless ocean is lava, not water, beyond the shore ring. Confirm the
   shore ring (beach/stony-shore) still separates the island's land from
   the lava, and walk the coastline checking nothing is on fire or
   catches fire. Confirm boats cannot be placed on the lava and a
   strider can walk across it. Confirm the seabed still visibly deepens
   further from shore. Confirm a compact, buried, enclosed fallback End
   portal exists and the dragon fight is winnable. Confirm the Nether is
   completely ordinary vanilla. **None of this phase's design/code
   review could verify actual in-game lava-at-scale behavior** (light,
   fire spread, fluid ticking, map color) — this is genuinely new
   ground for acceptance testing to cover, not a formality. **(0.2.53)**
   Confirm no ocean monuments, shipwrecks, ocean ruins, kelp/seagrass, or
   fish generate in the lava — Jason found these looked wrong in the
   original testing pass (config 36), so the lava exterior now reverts
   to the same silent, decoration-free boundary every non-ocean-island
   preset already has.
2. **Dry world**, `37-ocean-island-dry.yaml` (GOALS 31). Confirm the
   area beyond the shore ring is an exposed, walkable stone basin with
   no water anywhere, and that its depth still visibly increases further
   from shore. If any structure with a water feature (village well,
   etc.) happens to generate, confirm it still has real water —
   structures should be completely unaffected. Confirm a compact,
   buried, enclosed fallback End portal exists and the dragon fight is
   winnable. Confirm the Nether is completely ordinary vanilla. **Note:
   the "harder" difficulty option (removing rivers/surface lakes too) is
   not implemented in this phase** — deliberately deferred, see DESIGN
   §26.3; this config only exercises the core drained-ocean behavior.

## Phase 10 acceptance (sky island, GOALS 05/06, TODO 10.2-10.4)

Uses configs `38`-`43`, `57` (see [`config/tests/README.md`](config/tests/README.md)).
**Select "Worldz: Sky Island"** for all seven. Requires 0.2.45+ (`57`
requires 0.2.64+).

1. **Default island**, `38-sky-island-default.yaml` (GOALS 05 core).
   Confirm you spawn on a small, natural-looking, grass-topped island at
   Y 64 with a filled chest nearby. Confirm the slab is exactly 6 blocks
   thick (dig straight down and count) before falling into open void, and
   that void extends in every direction beyond the island's footprint —
   at every Y level, not just below. Confirm nothing generates naturally
   anywhere else in the Overworld (no trees, mobs, or structures beyond
   the island). Confirm a fallback End portal vault exists (check the
   server log for its coordinates — it sits at a fixed Y=-32, disconnected
   from the floating island by open void, a known and accepted gap, see
   DESIGN §27.5) and the dragon fight is winnable. Confirm the Nether is
   completely ordinary, unrestricted vanilla.
2. **Tiny island**, `39-sky-island-tiny.yaml` (the 8-block radius floor).
   **This is the specific risk DESIGN §27.8/TODO 10.3 flagged as
   deliberately deferred, not yet confirmed safe** — check carefully
   whether you spawn on the island itself (not beyond its edge in void)
   and whether the chest is actually reachable on foot from your spawn
   point. Report exact spawn/chest/island-center coordinates if anything
   looks wrong.
3. **Huge island**, `40-sky-island-huge.yaml`. Confirm the coastline reads
   as a natural, irregular blob from above (fly or climb to see the whole
   shape), and that the thicker 16-block slab is correct.
4. **Chest tiers and the biome water-item swap**, `41-sky-island-chest-easy-desert.yaml`
   and `42-sky-island-chest-hard.yaml`. On 41 (desert, easy tier): confirm
   the surface reads as sand-over-sandstone (not grass), and the chest
   holds the generous easy-tier contents (saplings, bread, crafting table,
   a lava bucket, 3 random extras) plus, in place of a cauldron: 2 ice
   blocks, 6 dirt, and 4 wheat seeds (2026-07-21 beatability follow-up,
   DESIGN §27.8). Confirm the ice melts into two adjacent water sources
   when placed (a real infinite supply), the dirt is enough to plant the
   saplings plus a small crop plot, and the lava bucket can produce
   obsidian for a Nether portal frame. On 42 (plains, hard tier): confirm
   the chest holds only the bare-essentials hard-tier contents (2
   saplings, a lava bucket, 1 random extra) plus a cauldron — plains
   already has natural dirt, so no extra dirt/seeds here — and that the
   world still feels beatable from this minimal a start, including
   reaching the Nether via the lava bucket.
5. **Nether variant**, `43-sky-island-nether.yaml` (GOALS 06). Build a
   portal on the Overworld island and step through. Confirm the Nether is
   also a small floating slab surrounded by void at the same radius —
   always netherrack-over-netherrack with a basalt core regardless of the
   Overworld's biome (DESIGN §27.6), nothing generating naturally beyond
   its footprint (no fortress, vegetation, piglins/hoglins). Confirm a
   fallback blaze spawner exists and blaze rods are obtainable.
6. **Biome exclusion zone**, `57-sky-island-biome-exclusion-zone.yaml`
   (2026-07-21 follow-up, DESIGN §27.10, 0.2.64+). With F3 open, bridge
   outward from the island. Confirm the biome reading stays pinned to the
   configured `minecraft:desert` for roughly 64 blocks past the island's
   own edge (the configured buffer), then switches to whatever the real
   seed's own biome noise reports beyond that — confirm the terrain stays
   void/unbuilt the whole way regardless of which biome F3 reports.

**Not covered by this phase's acceptance, by design:** the End (GOALS 06's
other dimension). Phase 10.5's research spike concluded vanilla End
generation already is a bounded-below floating-island world natively, so
there is nothing new to test there — see DESIGN §27.7 for the full
reasoning and Jason's outstanding go/no-go on treating it as already
satisfied.

## Phase 11 acceptance (floating resource islands, GOALS 07/08, TODO 11.2-11.5)

Uses configs `44`-`48`, `58` (see [`config/tests/README.md`](config/tests/README.md)).
**Select "Worldz: Sky Island"** for all six.

1. **Dense scatter**, `44-sky-island-floating-dense.yaml` (GOALS 08 core,
   0.2.48+). Confirm you spawn on the starter island as usual, and that
   several other small islands are visible nearby right from the edge —
   close enough to reach in a short bridge. Bridge to 3-4 of them; confirm
   they visibly differ in surface material (biome variety working) and
   each reads as a natural "lumpy" shape, not a perfect circle. Confirm no
   ore or chests appear (both resource layers are off in this config).
2. **Sparse scatter with a real exclusion zone**,
   `45-sky-island-floating-sparse-exclusion-zone.yaml` (0.2.48+). Confirm
   nothing is visible from the starter island's edge in any direction —
   just void for a while. Bridge outward past roughly 200 blocks and
   confirm the buffer genuinely holds (no islands appear before that
   point). Keep going until you find the first scattered island; confirm
   it took real, sustained bridging, and that islands stay spaced apart
   from each other rather than clustering. Every island should share the
   starter island's own plains biome (biome variety is off here).
3. **Ore deposits in isolation**, `46-sky-island-floating-ore-deposits.yaml`
   (0.2.49+). Bridge to 2-3 islands and dig straight down through each
   slab. Confirm each island has at most one embedded ore vein somewhere
   in its slab (from the default pool: coal, small iron, buried gold,
   redstone, lapis, small diamond, emerald) and no chest.
4. **Loot chests in isolation**, `47-sky-island-floating-loot-chests.yaml`
   (0.2.50+). Bridge to 2-3 islands. Confirm each has a chest sitting on
   its surface (not buried), contents always include 1 stick plus one
   extra pick from (diamond, gold ingot, book, saddle) — confirm different
   islands roll different extras, not all the same one — and no ore vein
   appears anywhere.
5. **Guaranteed village**, `48-sky-island-floating-guaranteed-village.yaml`
   (GOALS 07, 0.2.51+). **This is the phase's flagship "guaranteed, not
   best-effort" mechanism — check carefully.** Check the server log for
   the "Placed the GOALS 07 guaranteed village..." line and its
   coordinates; bridge toward them. Confirm a real village exists there:
   noticeably larger than the ordinary scattered islands nearby, with
   actual houses/paths/a well/villagers, in a building style consistent
   with its biome. Confirm normal village mechanics work (trading, chest
   loot). **Specifically check for anything visually broken where the
   village meets the island's synthetic slab edge** (overhanging jigsaw
   pieces, floating fragments) — this is a known, deliberately
   unverified-from-source-reading risk flagged in DESIGN §28.3.

6. **Natural biome**, `58-sky-island-floating-natural-biome.yaml`
   (2026-07-21 follow-up, DESIGN §28.6, 0.2.64+). Bridge to several
   scattered islands. Confirm each island's surface material reflects a
   real vanilla biome (not one drawn from the `islandBiomes` pool, even
   though `biomeVariety` is also on here — `naturalBiome` should win).
   Confirm neighboring islands' biomes read as spatially coherent (nearby
   islands more likely related) rather than the checkerboard variety
   configs 44/45 show, and that F3's reported biome matches the surface
   material you see.

**Not covered by this phase's acceptance:** Nether floating islands
(deliberately out of scope this phase, DESIGN §28.5 — GOALS 08's text has
no Nether component).

## Phase 12 acceptance (sky chunk challenge, GOALS 09/37, TODO 12.2-12.6)

Uses configs `49`-`52` (see [`config/tests/README.md`](config/tests/README.md)).
**Select "Worldz: Sky Chunk"** for all four. Every config logs the
guaranteed portal room, forced geode, and any underground-content showcase
finds at server start — check the log first, then bridge to each logged
location.

1. **Default full-column**, `49-sky-chunk-default.yaml` (GOALS 09 core,
   0.2.54+). Confirm you spawn on a normal one-chunk (16x16) island — real
   terrain, bedrock to sky, exactly like an ordinary vanilla chunk with
   void where its neighbors would be. Fly beyond the 256-block exclusion
   zone and confirm scattered one-chunk islands appear roughly 35% of the
   time, each genuinely different (real seed terrain, not copies). Check
   the log for "Placed the GOALS 09 guaranteed portal room (stronghold)
   near (X, Z)"; bridge there and confirm a real stronghold with a working
   End Portal Room exists (it may span more than one chunk — DESIGN §29.4
   flagged this). Check the log for the geode placement and confirm an
   amethyst geode exists roughly mid-underground in that chunk.
2. **Top-only depth cutoff**, `50-sky-chunk-top-only.yaml` (0.2.54+). Dig
   straight down from several points on the starter island; confirm you
   hit void after roughly 5 blocks below each column's own surface (not a
   single flat cutoff Y for the whole chunk — a hillier column should void
   out deeper than a flat one). Confirm any decoration cut off at the
   boundary (e.g. a tree with no visible roots) reads as an accepted
   cross-section artifact, not a bug to report.
3. **Nether/End toggles**, `51-sky-chunk-nether-end.yaml` (GOALS 09's
   "normal Nether/End, or chunk islands" option, 0.2.54+ — **this is the
   first Worldz preset to wrap the End's own generator at all, check
   carefully**). Confirm the Overworld behaves as in config 49. Travel to
   the Nether; confirm it's now also chunk islands (scattered natural
   Nether terrain on selected chunks, void elsewhere) rather than an
   ordinary unrestricted Nether. Reach the End via the guaranteed
   stronghold; confirm only some of vanilla's natural End islands are
   revealed, the rest void. **Confirm the dragon fight is still winnable**
   (the main End island with the exit portal should coincide with the
   forced-present starter cell at the End's own origin — flag it clearly
   if that island is ever missing).
4. **Multi-biome scatter + underground showcase**,
   `52-sky-chunk-scattered-showcase.yaml` (GOALS 37, 0.2.57/0.2.58+).
   Bridge to several scattered islands beyond 128 blocks; confirm they
   show genuinely different biomes (no override — whatever the seed
   naturally has). Dig at several islands' edges; confirm a mix of
   full-column and top-only islands (`scatteredTopOnlyChance: 0.5`), not
   uniform. Check the log for any cave-biome/structure showcase finds
   (lush caves, dripstone caves, deep dark, an ancient city, or trial
   chambers) and bridge to confirm the natural content is really there.

**Not covered by this phase's acceptance:** depth-aware biome forcing
(deliberately out of scope, stays the GOALS-15 Backlog item, DESIGN
§29.6); the rare portal-room/geode cell collision (documented, not
reproducible on demand — see `ChunkIslandPlan.reservedGeodeCell`'s javadoc,
only worth chasing if actually seen in a test world's log).

## Phase 13 acceptance (cave challenge, GOALS 25/26, TODO 13.2a-13.2d)

Uses configs `53`-`56` (see [`config/tests/README.md`](config/tests/README.md)).
**Select "Worldz: Cave"** for all four. Unlike every other typed preset,
the Overworld itself is completely untouched vanilla terrain — only spawn
placement (and the two optional pieces below) differ.

1. **Default underground spawn**, `53-cave-default.yaml` (GOALS 25 core,
   0.2.60+). Confirm you spawn underground — no sky visible, real stone/cave
   terrain around you, not the surface. Dig upward and confirm ordinary
   vanilla surface terrain exists above (hills, biomes, weather). Confirm
   normal caves, mineshafts, and other underground structures still
   generate. Build or find a Nether portal underground and confirm it
   works. If the server log warns "found no natural pocket... carving a
   safe capsule instead", note that in your report — it's an accepted
   fallback, not a bug, but worth flagging which seed triggered it.
2. **Sealed surface**, `54-cave-sealed-surface.yaml` (0.2.61+). Dig straight
   up from spawn; confirm you hit a solid stone ceiling around Y 128 with
   no way through without breaking it. Break through from below and confirm
   there's genuinely no sky beyond it (or, if a very tall mountain happens
   to poke through, that's an accepted clipped-flat case, not a bug — DESIGN
   §30.4). Confirm no phantoms spawn over several nights.
3. **Mega-cavern**, `55-cave-mega-cavern.yaml` (0.2.62+). Confirm you spawn
   in a large open cavern (roomy enough to build in), not a tight natural
   pocket like config 53. Walk to the cavern's edge and confirm it looks
   naturally irregular, blending into real cave systems rather than reading
   as a perfect sphere with a sharp seam. If a natural water/lava pool or
   passage already exists near the edge, confirm it's left untouched.
4. **Optional starter chest**, `56-cave-chest-and-sealed.yaml` (0.2.63+).
   Confirm a chest exists in the floor directly beneath your spawn position
   (dig one block down if it isn't immediately visible) containing the
   hard-tier kit (a handful of torches plus a little food and coal).
   Confirm the sealed surface (config 54's check) still holds with the
   chest enabled too.

**Not covered by this phase's acceptance:** a Nether or End variant of any
cave option (GOALS 25/26 are Overworld-only in scope, DESIGN §30.6).

## Phase 14 acceptance (Nether-start challenge, GOALS 27, TODO 14.2a-14.2b)

Uses configs `59`-`62` (see [`config/tests/README.md`](config/tests/README.md)).
**Select "Worldz: Nether Start"** for all four. Unlike every other typed
preset, the Overworld itself is completely untouched vanilla terrain —
only where you spawn (and which chest tier you get) differ.

1. **Default core mechanic**, `59-nether-start-default.yaml` (GOALS 27
   core, 0.2.66+). Confirm you spawn in the Nether, not the Overworld —
   standing on solid ground, two blocks of headroom, no lava immediately
   adjacent. Check the server log for "Set the GOALS 27 Nether-start world
   spawn at ..." and confirm the coordinates match. Confirm a chest sits
   in the floor beneath your feet with the medium tier's contents (10
   obsidian, no flint and steel, some bread, one random extra). **Die
   without placing a bed/respawn anchor first and confirm you respawn at
   the exact same Nether site** — this is the core beatability mechanic
   (DESIGN §31.2), the single most important thing to verify this phase.
   Build a portal (find/craft ignition yourself) and confirm the Overworld
   beyond it is genuinely ordinary vanilla terrain. Then place a real
   Nether respawn anchor somewhere else, charge it, die again, and confirm
   you now respawn at the anchor instead — ordinary vanilla behavior
   layering on cleanly on top of the redirected world-spawn default.
2. **Easy chest tier**, `60-nether-start-chest-easy.yaml` (0.2.66+).
   Confirm the chest holds 10 obsidian, 1 flint and steel, 8 bread, and
   3 random extras. Confirm you can build and ignite a complete portal
   frame immediately with no mining/cobblestone-generator detour needed.
3. **Hard chest tier**, `61-nether-start-chest-hard.yaml` (0.2.66+).
   Confirm the chest holds only 2 bread plus 1 random extra — no obsidian,
   no flint and steel. Explore for a way out (ruined portal, bastion/piglin
   bartering, natural lava+water) and report back whether a path out felt
   reasonably discoverable, or whether hard tier needs a small guaranteed
   nudge after all — DESIGN §31.6 flags these defaults as a first pass,
   not signed off.
4. **Capsule fallback**, `62-nether-start-capsule-fallback.yaml` (0.3.4+ —
   see Phase 14b acceptance below for the full capsule/starter-base
   checklist). Confirm the server log shows the capsule was built
   automatically because `spawnY` is close to the Nether's own floor, not
   a fallback from a failed search.

**Not covered by this phase's acceptance:** an End variant (GOALS 34 is
Phase 15, sharing this phase's own respawn-mechanics research, DESIGN
§31.1/§31.3).

## Phase 14b acceptance (Universal starter capsule, Nether-start first pass, GOALS 41, TODO 14b)

Uses configs `62`, `86`-`87`, `89` (see
[`config/tests/README.md`](config/tests/README.md)). **Select "Worldz:
Nether Start"** for all four; each guarantees the capsule fires for a
different reason (no need to hunt for a seed where the natural search
fails) — config 62 via its low `spawnY` alone (the new automatic
default), configs 86/87 via an explicit `forceCapsule: true` at an
ordinary `spawnY` (32).

1. **Default capsule shape, automatic low-spawnY default**,
   `62-nether-start-capsule-fallback.yaml` (0.3.4+, no `forceCapsule` set
   at all). Confirm the server log explains the capsule was built because
   `spawnY` (4) is close to the Nether's own floor — not "explicitly
   requested" and not "search failed, falling back". Confirm you spawn
   inside a decent-sized (7x7x5 exterior, a 5x5x3 room as seen from
   inside), fully enclosed nether-brick room — floor, ceiling, and all
   four walls solid, not just corner posts, no way to fall into
   surrounding lava/void from inside it. Confirm the room is genuinely
   lit by one glowstone block centered on each of the north/east/west
   walls (not off to one side) — you should not need to place your own
   light source to see clearly. Confirm the chest (easy tier: 10
   obsidian, flint and steel, bread, a wooden pickaxe, plus extras), a
   furnace, and a crafting table all line the south wall together,
   centered — chest in the middle, furnace and crafting table on either
   side — not the chest alone underfoot with nothing around it.
2. **Explicit request at a safe spawnY + `glow_lichen` + custom size**,
   `86-nether-start-capsule-glow-lichen.yaml` (0.3.4+, `spawnY: 32`,
   `forceCapsule: true`). Confirm the server log shows the capsule was
   explicitly requested this time (a different log line from config 62's
   automatic one) — proving `forceCapsule` still works independently of
   the low-spawnY default. Confirm the room is noticeably bigger (7x7
   interior floor, one block taller). Confirm glow lichen coats the
   entire interior surface — every wall, the floor, and the ceiling, not
   a few spaced points — including both faces at each corner. Confirm
   it's bright enough to see across the whole room. Confirm the chest,
   furnace, and crafting table still line one wall together here too.
3. **Hanging lanterns, dense-room floor grid, + hard tier**,
   `87-nether-start-capsule-hanging-lanterns.yaml` (0.3.4+, `spawnY: 32`,
   `forceCapsule: true`, `sizeBlocks: 9` — a 7x7 interior, at the 6x6
   "dense room" threshold). Confirm every ceiling lantern is genuinely
   suspended, not floor-standing — this is the one detail most worth
   double-checking closely. Confirm the ceiling lanterns form a real
   spaced 3x3 grid (not just one in the middle, and not a wall ring).
   Confirm a *second* grid of floor-standing lanterns also exists (the
   dense-room addition), but not at the exact center of the floor where
   you spawn. Confirm the hard-tier chest has no obsidian/flint and
   steel but does include a wooden pickaxe (GOALS 41: every tier
   guarantees at least a pickaxe to break out with).
4. **Breaking out**, any of the three. Confirm the guaranteed pickaxe can
   actually mine the capsule's nether-brick walls (wooden pickaxe is the
   vanilla minimum tier nether bricks require) — the intended "at least a
   pickaxe" escape path, GOALS 41.
5. **Normal spawnY still searches naturally**, revisit
   `59-nether-start-default.yaml` (`spawnY: 32`, no `forceCapsule`) if not
   already covered by Phase 14's own acceptance — confirm the natural
   search still runs and typically succeeds at the ordinary default depth
   (the low-spawnY default from item 1 should *not* have made this always
   skip straight to the capsule).
6. **Custom kit contents actually apply**, `89-nether-start-custom-kit.yaml`
   (0.3.2+, 2026-07-26 cleanup pass). TODO 15.2a-bugfix logged
   `netherStart.<tier>Kit` YAML overrides as silently ignored, but that
   turned out to already be fixed — this is the real in-game check that
   was missing. Confirm the hard-tier chest holds *exactly* this config's
   override (5 diamonds, 3 emeralds, 2 bread, one golden apple extra) —
   not the built-in hard defaults (2 bread, a wooden pickaxe, one random
   gold-ingot/torch extra). If you see the built-in defaults instead, the
   config plumbing has regressed.

**Not covered by this phase's acceptance:** `forceCapsule`/`capsule.*`
are not yet exposed on the in-game Customize screen (YAML config only,
DESIGN §31.9's own flagged gap); `cave` doesn't have this capsule option
yet (deferred generalization, GOALS 41.1 — `end_start` gained it at 0.3.5,
see Phase 15's own acceptance below).

## Phase 15 acceptance (End-start challenge, GOALS 34/41, TODO 15.2a-15.2b, 15.3)

Uses configs `63`-`65`, `88`, `90`-`92` (see
[`config/tests/README.md`](config/tests/README.md)).
**Select "Worldz: End Start"** for all seven. Unlike every other typed
preset except Nether Start, the Overworld itself is completely untouched
vanilla terrain — this time the Nether is too, and only the End (where
you spawn) and the chest tier/platform shape differ.

**Real bug found and fixed (0.3.5, Jason's first actual in-game test of
config 63, 2026-07-25):** he spawned standing on top of a small platform
with no chest visible anywhere. Root cause: the guaranteed platform is a
fully sealed shell (floor/ceiling/walls all solid), and `end_start` had
been deliberately left out of `PlayerSpawnFinderMixin`'s "trust the
resolved site outright" list on the theory that the End's mostly-void
surroundings would make vanilla's own same-column heightmap search
naturally re-find the platform's floor. Wrong: that search hits the
sealed *roof* first, same as any other solid ceiling, landing the player
standing on top of the box instead of inside it — reading as "a small
platform, no chest" (the chest is sealed inside, right below the roof).
Fixed by adding `end_start` to the mixin's trusted list, matching cave
and Nether-start's own treatment. Also generalized Nether-start's
configurable capsule mechanism (GOALS 41) to `end_start` in the same
pass: the default platform is now a 5x5 interior (was a 1x1-interior
shape effectively unusable), size/height/lighting are all configurable
via `endStart.capsule.*`, and the starter chest lines one wall to one
side once the room is big enough, instead of sitting invisibly underfoot.
**[Jason] confirmed 0.3.5, configs 63-65, 2026-07-25: spawn/platform/chest
mechanic all correct.**

**Follow-up fix (0.3.6, same retest):** Jason found none of the tiers
could actually get him out of the platform — "mainly need a pickaxe to
break out of the starting box". Confirmed against real vanilla source:
End Stone requires a pickaxe (any tier) to drop at all
(`BlockBehaviour.Properties.requiresCorrectToolForDrops()` on vanilla's
own `Blocks.END_STONE`) — hand-mining it, what this project's docs had
assumed since Phase 15, never actually worked. Every tier now guarantees
a pickaxe, escalating with the rest of each tier's gear (Jason's own
choice over one shared tier): hard gets a wooden pickaxe, medium a stone
pickaxe, easy a copper pickaxe.

1. **Default core mechanic**, `63-end-start-default.yaml` (GOALS 34 core,
   0.3.6+). Confirm you spawn **inside** the End platform (not on top of
   it) — a small, fully enclosed end-stone room (floor, ceiling, all four
   walls solid, not just corner posts), far from the central island (F3
   should read roughly X=1200, Z=0). Check the server log for "Set the
   GOALS 34 End-start world spawn at ..." and confirm the coordinates
   match. Confirm the room is a real 5x5 interior you can walk around in
   (not a single column), lit by default (glowstone), and that the chest
   lines one wall to the side — not directly underfoot — with the medium
   tier's contents (8 firework rockets, 32 cobblestone, 4 bread, 1 iron
   sword, 1 stone pickaxe, one random extra). **Die and confirm you
   respawn at the exact same End platform, still inside it** — beds and
   respawn anchors are both impossible in the End (vanilla's own rule),
   so this is the single most important thing to verify this phase, the
   same way it was for Nether-start (DESIGN §32.1/§32.4). Confirm the
   stone pickaxe actually breaks the platform's own end stone (drops the
   block, not just breaks it).
2. **Easy chest tier**, `64-end-start-chest-easy.yaml` (0.3.6+). Confirm
   the chest holds 16 firework rockets, 64 cobblestone, 8 bread, a bow, 32
   arrows, an iron sword, a copper pickaxe, and 3 random extras. Confirm
   the cobblestone alone is enough to start a real bridge toward the
   central island.
3. **Hard chest tier**, `65-end-start-chest-hard.yaml` (0.3.6+). Confirm
   the chest holds only 2 bread, 1 wooden pickaxe, plus 1 random extra —
   no rockets, no guaranteed weapon. Confirm mining the platform's own end
   stone with the wooden pickaxe and bridging toward the center is still
   genuinely possible, if slow — this is hard tier's *only* guaranteed
   path (no gateway/Elytra is ever provided, per Jason's 15.1 decision),
   so report back whether it felt like a long grind (expected) or
   unreasonably punishing. DESIGN §32.5 flags these defaults as a first
   pass, not signed off.
4. **Custom capsule shape**, `88-end-start-capsule-custom.yaml` (GOALS 41,
   0.3.5+). Custom `endStart.capsule.sizeBlocks: 9`/`heightBlocks: 4`
   (7x7 interior, a full block taller than default) with `lantern`
   lighting. Confirm the room is noticeably bigger than config 63's
   default, lanterns hang from the ceiling in a real spaced grid (this
   room is at/above the 6x6 "dense room" threshold, so also a
   floor-standing lantern grid), and the chest still lines one wall with
   real room to walk around.
5. **Torch lighting, dense room**, `90-end-start-capsule-torch-dense.yaml`
   (test-coverage gap closed 2026-07-26, 0.3.11+). `torch` had never
   actually been used by any capsule config for either preset before this
   one. Confirm wall torches line the north/east/west walls (south is the
   chest wall), spaced per `lightSpacingBlocks`, and that a *second*,
   floor-standing set of torches also exists (the dense-room addition, at
   this 7x7-interior size) — scattered, not at the exact spawn center.
6. **Shroomlight, dense room**, `91-end-start-capsule-shroomlight-dense.yaml`
   (test-coverage gap closed 2026-07-26, 0.3.11+). Confirm shroomlight is
   embedded in the north/east/west walls, *and* in a separate floor grid,
   *and* in a separate ceiling grid — unlike torch/lantern, a dense
   embedded-source room (shroomlight or the default glowstone) gets both
   floor and ceiling additions, not just one.
7. **Glow lichen coating**, `92-end-start-capsule-glow-lichen.yaml`
   (test-coverage parity with nether_start config 86, 2026-07-26,
   0.3.11+). Confirm glow lichen coats every wall, the floor, and the
   ceiling of the default-sized room — not just spaced points — bright
   enough to see the whole room unaided, same behavior as config 86 but
   over end-stone instead of nether brick.

**Ideally also attempt a full run** on at least one config: find or fight
your way to an End City for an Elytra, use the chest's rockets to fly (or
bridge by hand) to the central island, and confirm defeating the Ender
Dragon is genuinely achievable — including a hardcore attempt if you're
up for it, per GOALS 34's own "must be beatable in hardcore, even if
really hard" requirement.

**Not covered by this phase's acceptance:** the platform's shape/lighting
options aren't yet exposed on the in-game Customize screen (config-only
today, same deferral as Nether-start's own capsule, GOALS 41.1) — a
world created via the Customize screen always gets the compiled-in
defaults (5x5 interior, glowstone).

## Phase 16 acceptance (Flat worlds challenge, GOALS 15/16/22, TODO 16.2a-16.2b, 16.3, 16.6)

Uses configs `66`-`71` (see [`config/tests/README.md`](config/tests/README.md)).
Two typed presets this phase, not one — **select "Worldz: Flat"** for
configs `66`-`68`, **"Worldz: Deep Flat"** for `69`-`71`.

**Real bug found and fixed (0.3.7, Jason's first actual in-game test of
config 66, 2026-07-26):** no structures anywhere. Confirmed directly
against the actual world save (region files decoded by hand): structure
*starts* existed (a village's site really was chosen) and its chunk had
reached `full` generation status, but every section was still pure
flat-layer palette — no village block was ever written. Root cause:
vanilla's own `ChunkGenerator.applyBiomeDecoration` bundles two unrelated
things into one method call — ordinary per-biome feature decoration
(trees, ore veins) *and* the actual block-writing for every structure
whose site the earlier STRUCTURE_STARTS pass already resolved. This
project's own classic-flat `decoration` toggle (off by default) was
gating that entire method, so turning decoration off — the default —
silently dropped structure placement too, even though vanilla's own flat
worlds always place structures regardless of their own decoration
setting. Fixed by placing structures on their own when decoration is
off, leaving ordinary biome decoration as the only thing the toggle
actually controls. This affects every classic-flat config below (`66`-
`68`) — Jason's [66] retest is the first real confirmation any of them
ever worked as far as structures go; `69`-`71` (Deep Flat) were never
affected, since deep-flat has no `decoration` toggle of its own.

1. **Classic flat default**, `66-flat-default.yaml` (GOAL 15 core,
   0.3.7+). Confirm the world is completely flat everywhere, no hills or
   mountains, surface at Y 64. Confirm there are zero caves anywhere —
   classic flat has no noise/carving of any kind. Confirm the biome is
   plains everywhere. Locate a stronghold or village (both in the default
   `structureOverrides`) and confirm it generates normally, real blocks
   and all. Subjectively compare world-creation speed against a real
   terrain preset (e.g. config 01) — classic flat should feel closer to
   vanilla superflat's own near-instant generation, since the real noise
   pipeline never runs (DESIGN §33.1).
2. **Classic flat, thin/traditional layers**, `67-flat-classic-shallow.yaml`
   (0.3.8+). Confirm you spawn at Y -60 on grass with only 3 blocks of
   solid ground before bedrock. Confirm slimes *can* spawn here (wait for
   night in an unlit area) — unlike config 66's default, this stack sits
   below the Y-40 slime cutoff, confirming "avoiding slimes" is purely a
   property of layer height (DESIGN §33.3), not a separate setting.
   **Bug found and fixed (0.3.8, Jason's real retest):** low spawn used
   to show a very dark horizon band, all the way around, cutting the sky
   off sharply above the grass line. Root cause, confirmed against real
   decompiled sources: vanilla's own "dark disc" horizon plane
   (`SkyRenderer.shouldRenderDarkDisc`) only renders below sea level
   (Y 63) *unless* the world is flagged `isFlat` (vanilla's own superflat
   marker, set by a plain `instanceof FlatLevelSource` check at world
   creation) -- in which case the threshold drops to the world's real
   floor instead. `jlt_worldz`'s own `flat`/`deep_flat` presets never
   satisfied that check (their real generator is always
   `EnvelopedChunkGenerator`, not `FlatLevelSource`), so a low classic-flat
   spawn always fell below the ordinary sea-level threshold. Fixed via a
   `WorldDimensionsMixin` that marks a `flat`/`deep_flat` Overworld
   `isFlat` the same way vanilla's own superflat is. Confirm the horizon
   now looks like an ordinary open sky all the way down, no dark band.
3. **Classic flat, shallow underground structures**, `68-flat-structures-shallow.yaml`
   (0.3.7+, GOAL 22). Locate a trial chamber (`trial_chambers` forced
   eligible, `structureOverrides` set explicitly) over only 10 blocks of
   stone. Confirm the structure actually generates now (real blocks, not
   just a resolved site), but looks honestly clipped/cut off near the
   bedrock — this is GOAL 22's documented classic-flat tradeoff (depth is
   the player's own configuration choice), not a bug; from height, flat's
   open sightlines can make several clipped structures visible at once,
   which is the same tradeoff, not unusually frequent placement. **Don't
   expect an ancient city here**, even though `minecraft:ancient_cities`
   is also listed in this config's `structureOverrides` — real vanilla
   gates ancient-city placement to the `minecraft:deep_dark` biome
   specifically (DESIGN §33.2), and this config's single biome is
   `plains`, so it can never place regardless of stone depth; this is a
   permanent constraint of any single-biome `flat` world, not something
   this config's shallow stone broke. Keep this result in mind for #6.
4. **Deep flat default**, `69-deep-flat-default.yaml` (GOAL 16 core,
   0.3.11+ (0.2.71-0.3.8 crashed generating almost any chunk away from
   spawn's own (0,0) — `applyDeepFlatCap` passed absolute world
   coordinates into `Heightmap.update`, which needs chunk-local 0-15;
   0.3.9-0.3.10 no longer crashed but could show small dirt-rimmed ponds
   punched through the flat grass, or water draining down into a cave
   breach under a river/ocean — both fixed via a bounded `sealBeneathCap`
   pass, DESIGN §33.4; delete any pre-0.3.11 deep-flat saves first)).
   Confirm the surface is flat everywhere at Y 64, with no stray ponds or
   pits breaking the flat grass. Dig straight down from spawn and confirm
   you pass through ~3 blocks of cap into real stone, then find genuine,
   varied real caves/ravines. Explore for a real cave biome (lush caves,
   dripstone, deep dark). Find a river or ocean biome (F3) — note that
   `riverExclusionRadiusBlocks` (default 512) keeps a large area around
   spawn land-capped regardless of the real biome underneath, so look
   well past that radius — and confirm it shows as water at the flat
   surface, not paved over. A real cave breach immediately under the
   water should no longer drain it away (fixed, DESIGN §33.4); a
   genuinely deeper cave system further down still floods normally, same
   as any ordinary vanilla body of water. Confirm spawn itself
   lands exactly at Y 64, not wherever the real underlying terrain
   height happens to be.
5. **Deep flat, rivers disabled**, `70-deep-flat-no-rivers.yaml`
   (0.2.71+). Confirm a river/ocean location shows the ordinary land cap
   instead of water at the surface, confirming `riversEnabled: false`
   works. Confirm caves/structures below the cap are otherwise unaffected.
6. **Deep flat underground structures**, `71-deep-flat-structures.yaml`
   (0.2.71+, GOAL 22). Use `/locate structure minecraft:trial_chambers`
   (or `minecraft:ancient_city`), travel there, and dig down. Confirm the
   structure is genuinely, fully buried — no clipping, nothing exposed by
   the flat surface above it, GOAL 22 satisfied by construction. **Compare
   directly against #3's classic-flat result** — same structure types,
   completely different (correct) burial here.

**Not covered by this phase's acceptance:** the exact `(1200, 64, 0)`-style
fixture defaults for `flat`/`deep_flat` are first-pass numbers, same
"tune after playtest" posture as every other numeric default in this
project — report if `surfaceY: 64`/the default layer stack feel wrong,
not just whether they work.

## Phase 17 acceptance (Stacked biome layers, GOAL 35, TODO 17.2a-17.2c, updated DESIGN §34.7-§34.8)

Uses configs `72`-`77` (see [`config/tests/README.md`](config/tests/README.md)).
**Select "Worldz: Stacked"** for all six.

1. **Stacked default**, `72-stacked-default.yaml` (GOAL 35 core, updated for
   DESIGN §34.7's defaults, 0.2.77+). Confirm a world border exists at a
   64-block radius (`stacked.worldSizeChunks`'s own default of 4) — this
   config sets no explicit `overworldBorder`. Confirm spawn lands on the
   plains surface (top of the stack). Dig straight down and confirm you
   pass through all eight bands in order (taiga/desert/badlands/swamp/
   jungle/savanna/snowy_taiga/plains, bottom to top) separated by ~30-block
   air gaps, then bedrock — F3's biome readout should match each band, not
   just the surface. **Confirm each layer's own surface is gently uneven**
   from column to column (small bumps, not a perfectly flat plane) — this
   is the new relief default (up to 4 blocks); confirm trees/vegetation in
   the bumped terrain aren't floating or buried. **Confirm trees/vegetation
   generate in every layer's own air gap**, not only the surface — GOAL
   35's own explicit "trees on every layer" ask, and the part of this
   phase most likely to look wrong if the buried-layer decoration bypass
   (DESIGN §34.4) needs tuning: report the scatter density
   (`BURIED_LAYER_DECORATION_ATTEMPTS = 4`, fixed not configurable yet) if
   it looks too sparse or too dense. Confirm ore veins
   (coal/iron/copper/redstone/gold/diamond/lapis) show up in the bottom
   (taiga) layer's thick stone. Locate the End portal — DESIGN §34.7
   predicts the fallback vault, not a natural stronghold (the 64-block
   radius is smaller than `ProgressionGuarantees.NATURAL_STRUCTURE_MARGIN`
   = 128, so a natural stronghold is always rejected); confirm this is
   what you find, near the bottom layer's Y -32, and that it's reachable.
2. **Short stack beatability**, `73-stacked-short-stack-beatability.yaml`
   (DESIGN §34.5's flagged, deliberately unresolved risk, 0.2.77+; opts
   out of the new bounded-world default via `worldSizeChunks: 0` to keep
   its own explicit 512-block border). This is a genuine open question,
   not a known-good check — **report exactly what you find**, whichever
   way it goes: does the stronghold/End portal generate intact and
   reachable despite the short (29-block) solid stack, or does its
   geometry get clipped/interrupted by the air gaps or the dimension's own
   real min-Y anchoring? If natural search fails within the border,
   confirm whether `WorldLimitManager`'s fallback guaranteed-vault safety
   net still kicks in.
3. **Seed-randomized layer order**, `74-stacked-seed-randomized-order.yaml`
   (0.2.77+; opts out of the new bounded-world default to keep testing an
   explicitly unbounded world). Confirm the bottom-to-top order differs
   from config 72's own fixed order (any permutation is valid). Restart
   the server/reopen the world and dig down again in a freshly loaded
   chunk — confirm the same order reappears, proving the shuffle is
   deterministic from the real world seed rather than re-randomized per load.
4. **Unbounded world**, `75-stacked-unbounded.yaml` (DESIGN §34.7's
   `worldSizeChunks: 0` escape hatch, 0.2.77+). Confirm no world border
   appears at all, and that the same eight-band stack as config 72 still
   generates identically otherwise.
5. **Relief off**, `76-stacked-relief-off.yaml` (DESIGN §34.7's
   `reliefBlocks: 0`, 0.2.77+). Confirm every layer's surface is perfectly
   flat again (the pre-§34.7 look), with everything else matching config 72.
6. **Simplified layers**, `77-stacked-simplified-layers.yaml` (DESIGN
   §34.8, 0.2.77+). Every layer is written as a bare biome id, no
   `;blocks;air gap` at all. Confirm the world still generates five
   distinct, reasonable-looking bands (taiga/jungle/savanna/
   mushroom_fields/plains) despite no material stack ever being written in
   the config — including `mushroom_fields`, which has no hand-tuned
   `StackedBiomeDefaults` entry and should fall back to a plain
   stone/dirt/grass composition rather than erroring or failing to
   generate. Confirm every non-top layer's air gap is the simplified
   shorthand's own 30-block default.

**Not covered by this phase's acceptance:** exact per-layer default block
choices/thicknesses, the 64-block default world-size radius, and the
Y64→Y98 stack-center tradeoff (DESIGN §34.7) are all first-pass numbers,
same "tune after playtest" posture as every other numeric default in this
project. The buried-layer decoration bypass's scatter density and the
short-stack beatability question (item 2 above) remain open too.

## Phase 18 acceptance (World-hazard rules, GOALS 29-30, TODO 18.0-18.2, DESIGN §35)

Uses configs `78`-`82` (see [`config/tests/README.md`](config/tests/README.md)).
Unlike every earlier phase, these are shared runtime rules layered on top
of whatever World Type you pick — configs `78`-`81` use plain "Worldz";
`82` specifically uses **"Worldz: Ocean Island"** to confirm the rule
still applies under a typed preset.

1. **Forever night, immediate lock**, `78-forever-night-immediate.yaml`
   (0.2.79+). Confirm the world is already permanent night at spawn — no
   ordinary daylight ever. Confirm `/gamerule advance_time` reports
   `false`. Confirm sleeping in a bed doesn't skip time (the sky stays
   dark either way) — this should be a *free* consequence of the gamerule
   being off, not anything Worldz built separately; report if it doesn't
   hold. Confirm ordinary night-time hostile mobs spawn normally — this
   should be real permanent night, not a cosmetic dark filter.
2. **Forever night, delayed lock + relaxed insomnia**,
   `79-forever-night-delayed-relaxed.yaml` (0.2.79+). Confirm the world
   starts with an ordinary day/night cycle, then locks permanently around
   the 1-day mark (`/tick step 24000` to fast-forward). Confirm phantoms
   do **not** spawn even after a long stretch with no sleeping — the
   `relaxInsomnia` toggle should be actively suppressing them via a
   periodic stat reset, not merely relying on vanilla's own bed-reset
   (contrast with config 78's default vanilla-rules behavior).
3. **Forever night + border interaction**,
   `80-forever-night-border-interaction.yaml` (0.2.79+). This is the
   first real in-game confirmation of a **corrected** design claim (DESIGN
   §35.1) — the original assumption (locking night pauses *any* active
   border resize) turned out to be too broad once vanilla's own
   `WorldBorder` source was checked directly; only a still-delayed or
   actively-stepping resize should freeze, not a continuous one. Confirm
   the stepped border here (mirrors config 24's own shape) stays frozen
   at radius 8 for as long as night stays locked, even across many
   `/tick step` calls that would normally have produced several visible
   jumps. **Report exactly what you observe**, including anything that
   contradicts this — it's a corrected claim being tested for the first
   time, not a known-good check.
4. **Rising lava, plain bordered world**,
   `81-rising-lava-vanilla-limited.yaml` (0.2.79+, accelerated demo rate,
   not the shipped slow default). Confirm no lava exists until `delayDays`
   elapses, then confirm the level rises at the configured rate,
   converting air and water to lava while leaving solid terrain (stone,
   ore, structures) untouched. Confirm a chunk you visit for the first
   time after the level has already risen shows lava caught up throughout
   its whole risen range, not just newly forming at the surface. Confirm
   the level stops exactly at `maxY` and holds there.
5. **Rising lava, ocean island**, `82-rising-lava-ocean-island.yaml`
   (0.2.79+). Confirm the real ocean water (both near-shore and the
   deeper gradient) converts to lava from the bottom up as the level
   rises, exactly like config 81's underground demonstration — confirms
   DESIGN §35.2's own "uniform across every world type, no special-casing"
   scope call holds for a real typed preset, not just the plain preset.

**Not covered by this phase's acceptance:** the exact default rates
(1 block/day for lava, README's own documented first-pass numbers),
Customize-screen exposure for either hazard (config-only for this phase,
same "config-first, UI-later" posture flat/deep_flat/stacked all used
before their own defaults stabilized), rising lava's behavior under a
void-based floating preset (sky island/chunk island — no test config
exists yet; DESIGN §35.2 documents the intended uniform behavior but it
hasn't been observed in-game), and whether `/time add` bypasses the
`advance_time` gamerule on this snapshot (config 80's own steps flag this
as unconfirmed — use `/tick step` instead until it's checked).

## Phase 19 acceptance (Structure options wrap-up, GOALS 21/23/24, TODO 19.1-19.3, DESIGN §36)

Uses configs `83`-`84` (see [`config/tests/README.md`](config/tests/README.md)).
No acceptance steps for 19.1 (verification-only, no behavior change) or
19.3 (spiked and parked, no implementation — see DESIGN §36.4).

1. **Structure distance, basic**, `83-structure-distance-basic.yaml`
   (0.2.82+, 400-block radius — shrunk for testability, shipped default is
   2000). `/locate structure minecraft:village_plains` (or another village
   variant if that one's far away) and note the result. **Important:**
   `/locate` predicts from vanilla's own placement math and has no idea
   this mod suppressed generation — if the reported coordinate is inside
   400 blocks, travel there and confirm there is genuinely no village
   (plain terrain), not that `/locate` itself reports nothing. Then fly
   out past 400 blocks and confirm a real structure generates normally
   once you're clear of the radius.
2. **Structure distance + exemption, typed preset**,
   `84-structure-distance-single-biome-exempt-stronghold.yaml` (0.2.82+,
   500-block radius, **select "Worldz: Single Biome"**). Confirms the
   module composes with a typed preset (not just plain "Worldz") with no
   extra setup — proof that it lives in the one shared
   `EnvelopedChunkGenerator.createStructures` override every preset uses.
   `/locate structure minecraft:stronghold` and travel to the result
   regardless of distance — confirm a real stronghold exists there even if
   it falls inside 500 blocks, since `minecraft:strongholds` is exempted.
   Separately confirm an ordinary (non-exempt) structure inside 500 blocks
   is suppressed exactly like config 83.

**Not covered by this phase's acceptance:** Customize-screen exposure
(config-only for this phase, same posture as the Phase 18 world-hazard
modules), and a true per-structure-family *distinct* minimum distance
(the shipped shape is one shared distance plus an opt-out exemption list —
see the Backlog entry in TODO.md if a real per-family number is ever
needed).

## Scenario table: seed-informed spawn (Phase 16)

This is the current unverified feature. Run each row on **both** loaders
(12 runs total) with a fixed seed and `starterBiome: minecraft:plains`,
`starterRadiusBlocks: 256` unless noted.

| # | `spawn.strategy` | Setup | Expected result |
|---|---|---|---|
| 1 | `starter_at_origin` | defaults | Spawn lands inside the plains starter zone at/near `(0,0)`. F3 shows biome `plains` at spawn. |
| 2 | `vanilla_spawn` | defaults | Spawn is wherever vanilla's own search would put it for that seed (biome is whatever the allowed-biome filter reports there — not necessarily plains). Layout origin stays `(0,0)`: if a border is also enabled (see below), it's still centered at `(0,0)`. |
| 3 | `preferred_natural_biome` | `starterBiome: minecraft:plains` | Spawn is offset from `(0,0)` (unless a natural plains patch happens to already be there) at a natural, unmodified plains biome. Note the exact spawn coordinates. |
| 4 | `preferred_natural_biome` | `starterBiome: ''` (empty) | Log shows the "needs a starter biome" warning; behaves exactly like `starter_at_origin` (spawn at/near `(0,0)`, no starter zone since no starter biome). |
| 5 | `preferred_natural_biome` | pick a seed/biome combination unlikely to exist within 2048 blocks (e.g. `starterBiome: minecraft:mushroom_fields`) | Log shows the "search found no biome within N blocks" warning; falls back to spawning at `(0,0)` as `starter_at_origin` would. |

### Recentering verification (row 3 above, repeat with these enabled)

With `preferred_natural_biome` actually finding and relocating the origin
(row 3), additionally enable each of the following **one at a time** and
confirm the feature is centered on the *new* origin, not `(0,0)`:

- `overworldBorder.enabled: true` — walk toward `(0,0)` from spawn; the
  border should NOT be there. It should be centered on the found origin
  (use F3 to compare your position against the border-hit coordinates).
- `overworldExterior.mode: ocean` (with a boundary) — same check: the
  ocean/void transition should be centered on the new origin, not `(0,0)`.
- `overworldBorder.ensureObjective: true` — locate the End portal (`/locate
  structure` is fine for this, it's a placement check not a spoiler) and
  confirm it's reachable relative to the new origin's border, not `(0,0)`'s.
- A non-`legacy` `layout.mode` (e.g. `ocean`) — confirm the beach ring around
  the starter zone is centered on the new origin, not `(0,0)`.

### Persistence across restarts

1. Create a world with `preferred_natural_biome` (row 3), note the found
   origin coordinates from the log (`SpawnOriginState` write) or by walking
   back to the starter zone.
2. Fully stop the client/server and relaunch, load the same world.
3. Confirm: the log does **not** show a new search running, and the starter
   zone / border / exterior are still centered on the *same* origin as
   before (not re-rolled, not reset to `(0,0)`).

## Regression checks

- **Existing pre-Phase-16 worlds.** If you have a world saved before this
  session's work (or create one, then check out an older commit temporarily
  to generate one — do not do this in a way that discards uncommitted work),
  confirm it still loads and plays exactly as before: no spawn strategy
  field existed in its data, so it must decode as `starter_at_origin`
  with origin `(0,0)`.
- **`legacy` layout mode still works** with each spawn strategy — Phase 16
  only touches origin/spawn, not the layout algorithm itself.
- **Dedicated server parity.** Repeat at least one row of the matrix via
  `runServer` + a separate vanilla client connecting to it, since
  `LevelEvent.CreateSpawnPosition`/the Fabric mixin only fire server-side —
  a client-only smoke test would miss a server-side wiring bug entirely.

## Terrain sanity check (superseded by "Phase 1 acceptance" above)

The dummy-RandomState fix this section used to flag as an open risk shipped
in 0.1.15; see "Phase 1 acceptance" above for the concrete pass/fail check.
Still worth a passing glance during any scenario below: terrain well outside
the starter zone/layout-controlled area should show ordinary hills, ravines,
and vanilla-typical variation, not suspiciously flat or noise-free ground.

## Recording results

Keep it simple — a checklist per session is enough, e.g.:

```
2026-07-16, Fabric, seed 12345
[x] Row 1 starter_at_origin — spawn in plains at ~(4,~,8), OK
[x] Row 2 vanilla_spawn — spawn at (312,~,-198) biome=forest, border still at (0,0), OK
[ ] Row 3 preferred_natural_biome — spawn at (???) — BUG: border followed spawn but exterior didn't
```

File bugs (or just note them in conversation) with: loader, strategy, seed,
config snippet, expected vs. actual, and the relevant `latest.log` lines.
