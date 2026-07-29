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

(`TODO-archive.md` is the completed pre-replan history — reference only.
`TODO-done.md` holds full checklists for this plan's own completed phases;
`TODO.md` keeps only a one-line stub per finished phase.)

## Roles

- **Jason (owner):** all in-game testing, phase approval, version/publishing
  decisions. Items marked **[Jason]** need a human — flag them in your
  report; never attempt, fake, or simulate their results.
- **You (orchestrator):** run the subagent pipeline below rather than doing
  the work directly in the main session. Planning is settled at the
  phase level in GOALS.md/TODO.md/DESIGN.md; a separate, heavier
  `/code-review ultra` cloud review still checks the code between phases —
  that is distinct from this repo's own lighter `code-reviewer` subagent.

## Subagents

Use the role-matched user-level subagents available in the active tool rather
than doing every step in the main session:

- **Claude Code:** `~/.claude/agents/*.md`
- **Codex:** `~/.codex/agents/*.toml`

Both configurations use the same role names and workflow below. Their model,
reasoning-effort, tool, and sandbox settings are maintained in their own
agent definitions; do not infer one tool's model settings for the other.

- **`project-manager`** — selects the next task/phase to work on,
  keeps TODO.md current, reads/updates MEMORY.md. Delegates the actual
  investigation to `researcher` or `planner` rather than doing it itself.
  No coding, no builds, no commits.
- **`researcher`** — surveys possible approaches for a task (prior
  art, trade-offs) and verifies unfamiliar 26.2 APIs before use, via web
  research and codebase/decompiled-source reading. Read-only, hands
  findings back rather than choosing an approach. Use before `planner` when
  the right approach isn't yet clear.
- **`planner`** — architecture and design once the
  approach is understood, including design work *within* a task (e.g.
  working out an approach for a fiddly item), separate from the phase-level
  planning already covered by GOALS.md/TODO.md/DESIGN.md. Read-only: no code
  edits, builds, tests, or commits. Produces DESIGN.md sections / checkbox
  TODO.md sub-steps (see DESIGN §41/§42 for the pattern this project uses
  when a task is too large to implement in one pass).
- **`coder`** — the default for implementing TODO items.
- **`tester`** — runs `./gradlew build`, `javap`, and
  other checks right after `coder` finishes. Reports failures with actual
  output and delegates back to `coder` to fix them; never lints (that's
  `code-reviewer`) and never commits. (Renamed from `tool-runner` to match
  the other repos' agent set.)
- **`code-reviewer`** — reviews quality after tests pass: runs
  lint/static-analysis and filters signal from noise. Task-level scope is
  the uncommitted diff, reported back for `coder` to fix. Phase-level scope
  is the whole phase, logged into TODO.md and handed to `project-manager`.
  Never fixes code itself. Distinct from the heavier `/code-review ultra`
  cloud review command mentioned above.
- **`documentor`** — writes/updates README, example config and
  `MANUAL_TESTING.md` for features just implemented (DESIGN §20 for design
  tasks — design tasks commit their design *before* implementation). Hands
  off to `committer` to finalize rather than committing itself.
- **`release-manager`** — bumps the SemVer `version`
  field per task/phase completion, updating the `ProjectMetadataTest`
  contract in the same commit (see Ground rules below). MAJOR only on
  explicit user request; MINOR once per new phase (resets PATCH); PATCH once
  per task within a phase. Never commits, tags, or pushes itself.
- **`committer`** — stages and commits **per task**
  once the build is green, following normal git safety rules. Never pushes.

`project-manager` and `code-reviewer` require more judgment than the
mechanical `tester`, `release-manager`, and `committer` roles. Keep that
distinction in each tool's agent configuration.

## The loop (per task)

1. `project-manager` picks the next unchecked item in the **current phase
   only**. Skip [Jason] items (flag them); if blocked on one, take a later
   *independent* task in the same phase.
2. Confirm the task has everything it needs. **Verify every vanilla
   class/method against the actual 26.2 sources before use** — delegate to
   `researcher` (survey/verify) or `planner` (design a fiddly approach, or
   break an oversized task into sub-steps) as needed. For questions like
   "can I extend/override X", check `javap` on the real compiled jar —
   decompiled source has misrendered modifiers before (see MEMORY.md,
   2026-07-16 `final` lesson).
3. `coder` implements. Keep pure logic in `common`, JUnit-testable without
   booting Minecraft (the project's established pattern).
4. `coder` adds/updates **JUnit logic and component tests only** — no
   automated game tests, ever.
5. `tester` runs `./gradlew build` (all modules + tests); loops back to
   `coder` on any failure with the actual output.
6. `code-reviewer` reviews the task-level diff; loops back to `coder` on
   real defects (not lint noise).
7. `documentor` updates docs touched by the change (README, example config,
   `MANUAL_TESTING.md`; DESIGN §20 for design tasks).
8. Bookkeeping: newly discovered work → new TODO items (don't do it
   silently); departures from DESIGN/GOALS → TODO's Deviation log; durable
   decisions/lessons → MEMORY.md; check off the completed box. Whichever
   agent is doing the finishing pass for the task (usually `coder`) does
   this inline rather than deferring it.
9. `release-manager` bumps the version per Ground rules.
10. `committer` commits **per task**: first line is a brief summary, details
    in the body. **Never push.**

## Phase gates (hard rules)

- **Stop at the end of the phase** — when every non-[Jason] item is done and
  committed. Loop back to `project-manager` for phase-level review and
  TODO/MEMORY upkeep, then report: what changed, exactly what Jason should
  test in-game (which `config/tests/` files map to which use cases), and any
  new Questions/Deviations.
- **Never start the next phase without Jason's explicit permission.** He
  tests each phase manually and has the code reviewed between phases (both
  his own in-game pass and, separately, `/code-review ultra`) — leave the
  working tree clean and committed for that review.
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
- Version bumps (see `release-manager` above) update the `ProjectMetadataTest`
  contract in the same commit; releases get a clean build plus artifact
  inspection of both loader jars.

## Build & environment

- Only JDK: **Temurin 25**. `./gradlew build` runs all modules and tests;
  use `clean build` for releases.
- MC 26.2, fabric-loader 0.19.3, fabric-api 0.154.2+26.2, NeoForge
  26.2.0.12-beta, Loom 1.17-SNAPSHOT, Gradle wrapper 9.5.1.
- Authoritative API references: the decompiled 26.2 sources for reading
  (paths in MEMORY.md's Reference Log), `javap -p` against the compiled
  game jar for modifiers/signatures that matter.
