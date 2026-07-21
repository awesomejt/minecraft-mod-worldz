Read AGENTS.md and follow it exactly.

Goal: complete Phase 1 of TODO.md — "Stabilize and simplify (release 0.2.0)".

Work the phase as a loop until done: pick the next unchecked non-[Jason]
task; verify you have everything it needs (including verifying every 26.2
API against the real sources per AGENTS.md); implement with JUnit coverage;
update docs, TODO checkboxes, and MEMORY; get `./gradlew build` green;
commit with a summary-first message; move to the next task.

Specifics for this phase:
- 1.1 and 1.7 are mine ([Jason]) — flag them in your final report, don't
  attempt them. They don't block 1.2–1.6.
- For 1.5 (Prism deploy task), ask me for the instance path when you reach
  it rather than guessing.

Stop when every non-[Jason] Phase 1 task is done and committed. Then report:
what changed, exactly what I should test in-game (worlds to create, configs
to use, what to look for — including the 1.1 RandomState verification), and
anything you added to Questions for Jason or the Deviation log. Do not start
Phase 2.
