# Social Context — Part 1: Three Issues to Work On

Author: Zhenyu Song (zhenyus4@uci.edu)

## Selection criteria

I picked the three issues below using four criteria, chosen to be useful at *this* stage of the project (right after the architecture deep-dive on *Cluster and edit* in Assignment 2):

- **Clear scope and reproducible.** As a first-time contributor I need an issue with an unambiguous repro or acceptance criterion so I can tell when I am done.
- **Not already assigned and no open PR.** I checked each issue's assignees and timeline to avoid duplicating work that another contributor is already doing.
- **Builds on Assignment 2 knowledge.** I prefer issues that touch the subsystems I already mapped — clustering / fingerprinting, the command (HTTP) layer, and the operation/change model — so the prior reading pays off.
- **Mix of difficulty.** One small bug, one regression that needs investigation, and one feature-shaped task. This gives a realistic picture of contributing instead of three look-alike tickets.

## Issue 1 — Critical resource leak in `LoadLanguageCommand.java` ([#7759](https://github.com/OpenRefine/OpenRefine/issues/7759))

https://github.com/OpenRefine/OpenRefine/issues/7759

`LoadLanguageCommand.loadLanguageFile()` opens a `FileInputStream`, wraps it in readers, and never closes the stream on success or on a parse error (`main/src/com/google/refine/commands/lang/LoadLanguageCommand.java`, ~L158–176). The fix is a surgical refactor to a `try-with-resources` block. I want this one because it is small enough to finish end-to-end (find, fix, test, PR) and it lets me practice the command-layer plumbing I touched in Assignment 2 without getting stuck on architectural decisions.

## Issue 2 — Backslash escaping regression in operation history ([#7760](https://github.com/OpenRefine/OpenRefine/issues/7760))

https://github.com/OpenRefine/OpenRefine/issues/7760

A formula like `if(value=="\\","Backslash warning","")` works when typed in the UI but fails with `Missing )` when the same step is re-applied from exported operation JSON — a regression from OpenRefine 3.9.0. The repro is precise and the issue is unassigned. I want this one because diagnosing it requires walking the JSON-deserialization path into the GREL parser, which is exactly the `MassEditOperation` / change-replay flow I studied in Assignment 2. It is also the kind of bug whose fix needs a regression test, so I get practice with the testing conventions of the project.

## Issue 3 — Improve stop word handling — customizable and/or multi-language ([#7330](https://github.com/OpenRefine/OpenRefine/issues/7330))

https://github.com/OpenRefine/OpenRefine/issues/7330

Today there is a single hard-coded English stop-word list in `StandardReconConfig` that is only used in reconciliation; the issue proposes exposing it as a `stopwords` preference and notes it could also feed `fingerprint` keyers (cf [#3200](https://github.com/OpenRefine/OpenRefine/issues/3200)). It is officially labeled *Good First Issue* and has a concrete proposed solution. I want this one because it directly extends the `FingerprintKeyer` area I documented in Assignment 2 and has visible user impact for non-English projects, while still being scoped enough to land in a single PR.
