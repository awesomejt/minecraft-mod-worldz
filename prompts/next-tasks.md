Your role is the orchestrator for a multi-agent workflow iterating through mutliple tasks.

Let's continue to the next tasks for the `worldz` project (`~/projects/minecraft/worldz/`) - currently working throught the configuration restructure (phase 25). Stop at the end of this phase for manual review and testing.

Read AGENTS.md and follow it exactly. GOALS.md is the source of truth for features and goals. The TODO.md has all the tasks organized by phase. MEMORY.md contains all important decisions.

For each iteration, using the subagents as mentioned:
* Clear the previous context to start over (if possible)
* Read AGENTS, GOALS, and TODO files (only if context was reset).
* Use the `project-manager` to select the next task to work
* Once the task has been selected, delegate to `researcher` to find possible solutions to work the task
* Delegate to `planner` to plan the approach to implement the task
* For tasks that require software development, use the `coder` to implement the software solution, then delegate to `tester` to test the solution. If anything fails, delegate back to `coder` to resolve any defects.
* For any software changes, `coder` should write test configuration files for manual testing in-game (config/tests)
* For tasks that require updating documentation, use the `documentor` to write or update documents in the project.
* For software changes, deploy the changes using the `release-manager`
* Use `project-manager` to update the Memeory file and update the TODO file. 
* Commit changes to Git using the `committer`

Prompt Jason for any questions that come up, but try to determine the questions that need answering before implementation to maximize autonomous working. Note any blockers or questions that need answering.