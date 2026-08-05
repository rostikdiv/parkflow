# GEMINI.md — Antigravity-specific ParkFlow settings

These rules take precedence over `AGENTS.md` when there is a conflict, but usually only supplement it with Antigravity-specific details (Plan Artifact, Agent Manager).

## Mode of operation
- For any task that affects more than one file or new functionality (not dot-fix) — use **Plan mode**, not Fast mode. First generate the Plan Artifact, wait for confirmation, and only then execute.
- Fast mode — only for trivial edits (printing, formatting, variable renaming).

## Plan Artifact — mandatory structure
Each Plan Artifact for a milestone from `docs/parkflow_final_plan.md` must explicitly contain:
1. A reference to a specific section of the plan (e.g. "§4.3, level 3 — exclusion constraint").
2. A list of files to be created/modified.
3. List of tests to be added (marked Phase A / Phase B).
4. Explicit "Out of scope" — what will NOT be done in this task (so as not to spread beyond the milestone).

## Autonomy
- Terminal Policy: Auto — allowed for build/test commands (`mvn test`, `docker compose up`).
- But: commands that change the state of the repository externally (git push, deploy, change cloud infrastructure) — always Review, never Auto.
- Auto-continue: disabled for tasks longer than one milestone — after the milestone is completed, the agent stops and waits for a new Plan Artifact, and does not continue to the next milestone on its own.

## Response format
Follow the structured block "What was done / Technical explanation / Tests / What to check manually" from `AGENTS.md` — Plan Artifact adds this before execution, structured block — after.

## Model
By default, Gemini 3 Pro is used for implementation. If the task is purely architectural/refactoring with high stakes, you can switch to Claude within Antigravity (optional model), clearly state this in the chat.