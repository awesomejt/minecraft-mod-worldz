# TODO — jlt_worldz implementation

**Executor:** any capable Claude model (Opus/Sonnet). Planned by Fable on
2026-07-14; the design is settled — execute it, don't redesign it.

**Read first, before writing any code:**
1. `DESIGN.md` (this repo) — the mod spec.
2. `../reseed` — the structural template. Copy its build wiring; do not modify it.
3. `../trees/common/src/main/java/media/jlt/minecraft/mods/trees/config/` —
   the config-class pattern (flat JSON + `_docs`, sanitization, tests) and how
   the loader entrypoints hand the config dir to common code.

**Ground rules**
- Work on `main` in this repo (`/shared/projects/minecraft/worldz`,
  github.com/awesomejt/minecraft-mod-worldz). Do not touch the sibling repos.
- DESIGN.md's Java sketches use Mojmap names remembered from ~1.20-era APIs.
  **Verify every vanilla class/method against the actual 26.2 sources**
  (Loom's `genSources` / IDE navigation) before use. Shape adjustments are
  fine; log every deviation from DESIGN.md in the Deviation log below.
- `./gradlew build` (which runs the unit tests) must be green before every
  commit. Commit at each numbered checkpoint with a descriptive message; do
  **not** push unless Jason asks.
- Items marked **[Jason]** need a human (in-game smoke tests). Ask; don't fake.
- Keep checkboxes in this file updated as you go.

Environment: only JDK is Temurin 25. Toolchain identical to `../reseed`
(Gradle wrapper 9.5.1, Loom 1.17-SNAPSHOT, MC 26.2, fabric-loader 0.19.3,
fabric-api 0.154.2+26.2, NeoForge 26.2.0.12-beta).

---

## Phase 0 — Repo skeleton (copy from ../reseed)

- [x] 0.1 Copy `gradlew`, `gradlew.bat`, `gradle/wrapper/`, `build-logic/`
      (sources only — no `.gradle/` caches), `.github/workflows/build.yml`,
      and `LICENSE` from `../reseed`.
- [x] 0.2 `settings.gradle` copied and adapted: `rootProject.name = 'mod-worldz'`.
- [x] 0.3 `gradle.properties` copied and adapted: `version=0.1.0`,
      `mod_id=jlt_worldz`, `mod_name=JLT Worldz`, description from DESIGN §1;
      all version numbers unchanged from reseed.
- [x] 0.4 `common/`, `fabric/`, `neoforge/` module `build.gradle` files copied
      from reseed (keep the gametest wiring if it comes along; it's harmless).
- [x] 0.5 Entrypoints `WorldzCommon` / `WorldzFabric` / `WorldzNeoForge` in
      `media.jlt.minecraft.mods.worldz` (log-only init, mirroring reseed),
      `fabric.mod.json` + `neoforge.mods.toml` adapted (id, name, entrypoint
      class, sources URL `minecraft-mod-worldz`; drop reseed's
      `fabric-gametest` entrypoint).
- [x] 0.6 `./gradlew build` green. **Commit** ("Repo skeleton from reseed template").

## Phase 1 — Config

- [x] 1.1 `config/WorldzConfig`: fields, defaults, clamps per DESIGN §6;
      loader entrypoints pass the config dir `Path` (mirror trees' wiring).
- [x] 1.2 Unit tests: defaults on missing/malformed file, unknown-key
      tolerance, list-entry filtering, radius clamp.
- [x] 1.3 `logic/BiomeListSpec` (+ starter-zone math helper): pure parsing /
      validation / quart-distance per DESIGN §5, with unit tests including
      boundary cases (`r*r` inclusive, block→quart conversion).
- [x] 1.4 `config/jlt_worldz.example.json` checked into the repo.
- [x] 1.5 Build green. **Commit** ("Config + pure biome-list logic").

## Phase 2 — LimitedBiomeSource

- [x] 2.1 `worldgen/LimitedBiomeSource` per DESIGN §4: codec with optional
      fields + registry getters, config fallback at decode, resolved values
      on encode, filtered multi-noise delegate, starter-zone override in
      `getNoiseBiome`, possible-biomes union.
- [x] 2.2 Fail-safes: empty filter result → WARN + unfiltered overworld list;
      WARN for allowed ids with no overworld climate entry; unknown ids
      skipped with WARN.
- [x] 2.3 Registration under `jlt_worldz:limited`: Fabric — direct
      `Registry.register` in `WorldzFabric.onInitialize`; NeoForge —
      `DeferredRegister.create(Registries.BIOME_SOURCE, MOD_ID)` on the mod
      bus in `WorldzNeoForge`.
- [x] 2.4 Build green on both loaders. **Commit** ("LimitedBiomeSource + registration").

## Phase 3 — World preset data

- [x] 3.1 `data/jlt_worldz/worldgen/world_preset/worldz.json` per DESIGN §8
      (overworld stem with bare `jlt_worldz:limited` source; vanilla nether +
      end stems copied for 26.2).
- [x] 3.2 `data/minecraft/tags/worldgen/world_preset/normal.json` tag entry.
- [x] 3.3 `assets/jlt_worldz/lang/en_us.json` with the preset display name
      (verify the 26.2 world-preset lang key format against vanilla).
- [ ] 3.4 `./gradlew :fabric:runClient` — verify at minimum that the game
      reaches the title screen and the create-world screen lists "Worldz"
      under World Type without datapack-validation errors. **Commit**
      ("Worldz world preset").
      Automated status: Fabric reached title-screen resource reload; Fabric and
      NeoForge dedicated servers both created and saved fresh
      `level-type=jlt_worldz:worldz` worlds. Visual dropdown confirmation remains.

## Phase 4 — Smoke test [Jason]

- [ ] 4.1 [Jason] Fabric client: create a Worldz world with default config
      (plains only); fly around, F3 shows only plains; terrain still has
      rivers/hills shape.
- [ ] 4.2 [Jason] Multi-biome config (e.g. plains + desert + snowy_plains) →
      distinct regions appear.
- [ ] 4.3 [Jason] Starter biome config (e.g. allowed cherry_grove, starter
      plains, radius 512): spawn is in plains, cherry_grove past the radius.
      **Note whether spawn landed inside the zone.**
- [ ] 4.4 [Jason] NeoForge client: repeat 4.1 briefly.
- [ ] 4.5 [Jason] Dedicated-server check (either loader):
      `level-type=jlt_worldz:worldz` in server.properties generates a limited
      world.
- [ ] 4.6 Config-change isolation: reopen the 4.1 world after changing
      `allowedBiomes` — world must be unchanged (settings baked at creation).

## Phase 5 — Spawn guard (only if 4.3 showed spawn outside the zone)

- [ ] 5.1 Server-started hook on both loaders per DESIGN §7; skipped entirely
      when the overworld source isn't a `LimitedBiomeSource` with a starter
      biome. If 4.3 was fine, check this box with a note and skip.
- [ ] 5.2 Build green. **Commit** if implemented.

## Phase 6 — Wrap-up

- [ ] 6.1 README.md: what it does, config reference, world-type selection
      (client + server.properties), caveats from DESIGN §10. Mirror reseed's
      README structure.
- [ ] 6.2 Check off remaining boxes here, fill the Deviation log, final
      **commit**. Do not push, tag, or publish — [Jason] decides.

---

## Deviation log

(record every departure from DESIGN.md here: what, where, why)

- DESIGN §4's remembered `ResourceLocation` API is named `Identifier` in the
  verified Minecraft 26.2 sources. All ids use `net.minecraft.resources.Identifier`.
- Fabric registers the codec in `BuiltInRegistries.BIOME_SOURCE`; `Registries.BIOME_SOURCE`
  is the corresponding registry key and is used by NeoForge's `DeferredRegister`.
- A persisted `starter_radius` is treated as the marker that an absent
  `starter_biome` is resolved-disabled. Without this distinction, the optional
  field codec would omit an empty starter and reload an existing world's starter
  from a newly edited config, violating the baked-settings requirement.
- When the empty-filter fail-safe activates, `collectPossibleBiomes()` reports
  the fallback delegate's vanilla overworld biomes (plus any starter), rather
  than an empty configured set. This keeps feature/structure logic consistent
  with the biomes the fail-safe actually returns.
- The codec does not retrieve the multi-noise parameter-list registry getter in
  26.2. World presets and that registry load in parallel, so its overworld holder
  is still unbound during preset decode. Instead it constructs the public vanilla
  `Preset.OVERWORLD` parameter list with the retrieved biome getter; this uses the
  identical vanilla provider and is safe during parallel registry loading.
