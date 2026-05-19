# Social Context — Part 2

**Project:** [OpenRefine](https://github.com/OpenRefine/OpenRefine)

**Team members:** Kingson Zhang, Mouhamed Osman, Zhenyu Song, Zian Xu

## 1. Team selection criteria

After each member presented the three issues from their Part 1, we agreed on five criteria for the joint pick. The criteria are chosen to be useful *at this point* in the project — right after the Assignment 2 architectural deep-dive on *Cluster and edit* — so that the team can move from reading the code to actually contributing.

- **C1 — Domain continuity with Assignment 2.** Prefer issues that touch the clustering / fingerprinting / operation-replay subsystems we already mapped, so the prior reading pays off.
- **C2 — Manageable scope.** Prefer *Good First Issue* labels or issues with clear, localized reproduction steps, so the team can credibly finish them in a single PR.
- **C3 — Community signal.** Prefer issues where a core maintainer has weighed in or a roadmap is visible, and where no open PR is already in flight, so we are not duplicating work.
- **C4 — Difficulty gradient.** Pick a mix — one small surgical task, one regression bug, one enhancement, one architectural change — so every team member has a realistic on-ramp and no one is stuck on the same kind of work.
- **C5 — Joint integration.** Each member must see their interests reflected, and at least one issue should appear on more than one Part 1 list to anchor the consensus.

## 2. Process and integration

We compared the twelve individual nominations and found exactly one overlap: [#7330](https://github.com/OpenRefine/OpenRefine/issues/7330) was independently nominated by both Zhenyu and Kingson, so we took it as our consensus anchor. We then picked one further issue from each of the remaining members' lists to cover four distinct subsystems — fingerprinting/reconciliation, clustering engine, expression/operation replay, and frontend reconciliation UI — while keeping the difficulty gradient described in C4.

## 3. Chosen issues

### 3.1 [#7330 — Improve stop-word handling: customizable and/or multi-language](https://github.com/OpenRefine/OpenRefine/issues/7330)

https://github.com/OpenRefine/OpenRefine/issues/7330

**Discussion.** Today `StandardReconConfig` carries a single hard-coded English stop-word list used only during reconciliation. The issue proposes promoting it to a user-facing `stopwords` preference and notes the same list could also feed the `fingerprint` keyer used by Cluster and edit. The issue is labeled *Good First Issue* and includes a concrete proposed solution.

**Why the team picked it.** Joint pick from Zhenyu and Kingson (satisfies **C5**). It extends the `FingerprintKeyer` area we documented in Assignment 2 (**C1**), is officially scoped as a first issue (**C2**), and has visible user impact for non-English projects.

**Originally proposed by:** Zhenyu and Kingson.

### 3.2 [#289 — Clustering should be cancelable](https://github.com/OpenRefine/OpenRefine/issues/289)

https://github.com/OpenRefine/OpenRefine/issues/289

**Discussion.** Closing the clustering dialog does not stop the backend computation, so it keeps running and saturates CPU. The thread contains a multi-phase plan from maintainers (`tfmorris`, `thadguidry`) to replace legacy Vicino dependencies and convert the clustering engine into an asynchronous, cancellable `LongRunningProcess` with a UI progress bar.

**Why the team picked it.** Directly continues the cluster-and-edit subsystem we studied (**C1**) and offers the richest social context of all our candidates: a maintainer-authored roadmap shows the team how a real refactor is sequenced (**C3**). It is the most architectural item in our set, fulfilling the heavy end of the gradient (**C4**).

**Originally proposed by:** Zian.

### 3.3 [#7760 — Backslash escaping regression in operation history](https://github.com/OpenRefine/OpenRefine/issues/7760)

https://github.com/OpenRefine/OpenRefine/issues/7760

**Discussion.** A GREL formula such as `if(value=="\\","Backslash warning","")` works when typed in the UI but fails with `Missing )` when the same step is re-applied from exported operation JSON — a regression from OpenRefine 3.9.0. The issue is unassigned and has a precise repro.

**Why the team picked it.** Sits squarely on the JSON-deserialization path into the GREL parser — the same `MassEditOperation` / change-replay flow that Assignment 2 walked through (**C1**). It is clearly scoped (**C2**), is a regression rather than a green-field feature, and the fix requires a regression test, giving the team practice with the project's testing conventions (**C4**).

**Originally proposed by:** Zhenyu.

### 3.4 [#6697 — Recon Suggest widget: middle-click to open URL in new tab](https://github.com/OpenRefine/OpenRefine/issues/6697)

https://github.com/OpenRefine/OpenRefine/issues/6697

**Discussion.** In the reconciliation suggestion widget, middle-clicking a candidate currently selects the entity instead of opening its URL in a new tab. The fix is a small frontend interaction tweak — distinguish middle-click from primary click and route it to `window.open`.

**Why the team picked it.** Smallest, most self-contained item in our set (**C2**, **C4**). It complements the heavier backend work by giving the team a quick first PR through the frontend reconciliation UI, which is adjacent to the reconciliation code touched by issue #7330 (**C1**). Brings Mouhamed's frontend/UX perspective into the team plan (**C5**).

**Originally proposed by:** Mouhamed.

## 4. Summary

| # | Issue | Proposer(s) | Subsystem | Criteria matched | Difficulty slot |
|---|---|---|---|---|---|
| 1 | [#7330](https://github.com/OpenRefine/OpenRefine/issues/7330) Stop-word handling | Zhenyu + Kingson | Reconciliation / Fingerprint | C1, C2, C5 | Enhancement (GFI) |
| 2 | [#289](https://github.com/OpenRefine/OpenRefine/issues/289) Cancelable clustering | Zian | Clustering engine | C1, C3, C4 | Architectural |
| 3 | [#7760](https://github.com/OpenRefine/OpenRefine/issues/7760) Backslash regression | Zhenyu | GREL / Operation replay | C1, C2, C4 | Regression bug |
| 4 | [#6697](https://github.com/OpenRefine/OpenRefine/issues/6697) Middle-click in recon | Mouhamed | Frontend (recon UI) | C2, C4, C5 | Small surgical |

Each row maps back to a different teammate's Part 1, and together the four issues span all three layers we identified in Assignment 2 (frontend, command/operation, backend engine), satisfying both the completeness and the integration rubric criteria for Part 2.
