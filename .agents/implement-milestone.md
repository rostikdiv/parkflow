# implement-milestone.md

Called as `/implement-milestone <number, e.g. M2>` in the agent chat.

## Instructions for the agent

1. Open `docs/parkflow_final_plan.md`, find the relevant milestone line in the §15 table, and all plan sections it references (eg M2 → §4.1, §4.2, §4.3, §5, §14 Phase B).
2. Generate the Plan Artifact according to the structure from `GEMINI.md` (links to the section, files, Phase A/B tests, out of scope).
3. Stop and wait for confirmation of the Plan Artifact from the user - do not execute immediately.
4. After confirmation — implement, following `AGENTS.md' (English comments, sealed-types without default, links to the plan in comments to key decisions).
5. Write the tests immediately within the same step (not a separate next prompt) according to the Phase specified in the plan for this milestone.
6. Finish with a structured block "What was done / Technical explanation / Tests / What to check manually".
7. Do not go to the next milestone on your own - stop and wait for a new team.

## Call example
```
/implement-milestone M2
```
Expected output: Plan Artifact with reference to §4.1 (Reservation, ReservationAudit), §4.2 (sealed ReservationStatus), §4.3 (exclusion constraint + race-test), §5 (idempotency), §14 Phase B — and a list of files to create/change, before any code.