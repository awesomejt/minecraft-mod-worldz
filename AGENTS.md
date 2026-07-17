# AGENTS.md — jlt_worldz

Guidance for any AI agent working in this repository. Follow it exactly; it
encodes the owner's required workflow.

## What this project is

**jlt_worldz**: a multiloader (Fabric + NeoForge) Minecraft 26.2 mod that
generates YouTube-style *challenge worlds* (ocean island, sky island, sky
chunk, single biome, cave, Nether/End start, flat, limited/expanding size,
world hazards). Java 25, Gradle wrapper, base package
`media.jlt.minecraft.mods.worldz`, modules `common` / `fabric` / `neoforge`.

## Read before working (in this order)

1. `GOALS.md` — requirements: use cases 01–37. The source of truth for scope.
2. `TODO.md` — the active 20-phase plan. Execute it in order; don't redesign it.
3. `DESIGN.md` — §20 is the current architecture; §§1–19 document the built
   components and verified 26.2 APIs. Where §17 conflicts with §20, §20 wins.
4. `MEMORY.md` — settled decisions, known risks, API deviations. Append as
   you learn; never re-litigate what's recorded there.
5. `MANUAL_TESTING.md` — how Jason tests. Keep its scenario tables current.

(`TODO-archive.md` is the completed pre-replan history — reference only.)

## Roles

- **Jason (owner):** all in-game testing, phase approval, version/publishing
  decisions. Items marked **[Jason]** need a human — flag them in your
  report; never attempt, fake, or simulate their results.
- **You (executor):** implement TODO tasks. Planning is settled; a separate
  high-power model reviews the code between phases.

## The loop (per task)

1. Pick the next unchecked item in the **current phase only**. Skip [Jason]
   items (flag them); if blocked on one, take a later *independent* task in
   the same phase.
2. Confirm you have everything the task needs. **Verify every vanilla
   class/method against the actual 26.2 sources before use.** For questions
   like "can I extend/override X", check `javap` on the real compiled jar —
   decompiled source has misrendered modifiers before (see MEMORY.md,
   2026-07-16 `final` lesson).
3. Implement. Keep pure logic in `common`, JUnit-testable without booting
   Minecraft (the project's established pattern).
4. Add/update **JUnit logic and component tests only** — no automated game
   tests, ever.
5. Update docs touched by the change (README, example config,
   MANUAL_TESTING.md; DESIGN §20 for design tasks — design tasks commit
   their design *before* implementation).
6. Bookkeeping: newly discovered work → new TODO items (don't do it
   silently); departures from DESIGN/GOALS → TODO's Deviation log; durable
   decisions/lessons → MEMORY.md; check off the completed box.
7. `./gradlew build` green (all modules + tests), then **commit per task**:
   first line is a brief summary, details in the body. **Never push.**

## Phase gates (hard rules)

- **Stop at the end of the phase** — when every non-[Jason] item is done and
  committed. Report: what changed, exactly what Jason should test in-game
  (which `config/tests/` files map to which use cases), and any new
  Questions/Deviations.
- **Never start the next phase without Jason's explicit permission.** He
  tests each phase manually and has the code reviewed between phases — leave
  the working tree clean and committed for that review.
- Genuine gameplay/scope questions: add to TODO.md "Questions for Jason" and
  stop that task; don't guess.

## Ground rules

- Work on `main` in this repo only. Never modify the sibling repos
  (`../reseed` is the structural template — copy from it, don't touch it).
- **New worlds only:** no save-compat obligations for worlds created by
  older mod versions; test worlds are disposable. A world must still reopen
  consistently under the version that created it.
- **Client-first:** Fabric singleplayer is the acceptance path. NeoForge
  must always build; give it a brief check whenever loader-level code
  (mixins, events, registration) changes.
- Every phase ships at least one `config/tests/` YAML per covered use case.
- Version bumps update the `ProjectMetadataTest` contract in the same
  commit; releases get a clean build plus artifact inspection of both
  loader jars.

## Build & environment

- Only JDK: **Temurin 25**. `./gradlew build` runs all modules and tests;
  use `clean build` for releases.
- MC 26.2, fabric-loader 0.19.3, fabric-api 0.154.2+26.2, NeoForge
  26.2.0.12-beta, Loom 1.17-SNAPSHOT, Gradle wrapper 9.5.1.
- Authoritative API references: the decompiled 26.2 sources for reading
  (paths in MEMORY.md's Reference Log), `javap -p` against the compiled
  game jar for modifiers/signatures that matter.
