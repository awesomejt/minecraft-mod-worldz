Let's continue to the next phase.

Read AGENTS.md and follow it exactly. GOALS.md is the source of truth for features and goals. The TODO.md has all the tasks organized by phase. MEMORY.md contains all important decisions.

Proceed to next phase and execute each task one by one. Stop at the end of the phase. Have one commit per task. As per the workflow, ensure each task has the code required, any linting/code quality reviewed, test code/test execution, clean compile, bumped version (patch level for each task), and updated documentation.

Prompt Jason for any decisions that need to be made during the execution of the work for the phase. If possible gather all questions that need to be answered early into execution. Update the Memory file with decisions, update the TODO file if any decisions impact future tasks.

At the end of the phase, ensure Jason has a set of specific test cases and configuration files to test (config/tests/phase-<n>) - which will be manually tested in Prism creating worlds to try each configuration. Jason will report back general findings but you can review the logs and screenshots captured as well at that point.

Any defects as a result of this phase or that should be addressed in this phase should be addressed before moving on. Any defects/bugs discovered that would better be addressed in later phases, note that and update the TODO/Memory accordingly.

Proceed to the next phase only after Jason's approval.