# Process Journal: Single Labels for Split i18n Strings (OpenRefine #8081, part of #1858)

**Author:** Kaley Chung

---

## 1. Feature Exploration

The assignment asked me to find and fix an open issue, so the first decision was which one. I browsed goodfirstissue.dev and checked each candidate's thread before committing:

| Issue | Why I passed on it |
| --- | --- |
| cve-bin-tool #4321 (slow tests) | 7+ open PRs already targeting the same tests |
| OpenRefine #3645 (reconciliation manifest) | Already has an open PR (#7317) |
| OpenRefine #2295 (remove diacritics) | Half done (`normalize()` exists); not labeled good first issue |
| OpenRefine #3360 (records mode hint) | Maintainers disagree on whether to do it at all |
| scikit-learn #30537 | Third attempt in progress, a fourth person waiting |
| ansible/awx issues | Need a full Kubernetes deployment to test; maintainers not replying |
| hamaluik/timecop | No code merged in almost two years |

I chose **OpenRefine #1858**: labeled Good First Issue, unassigned, with a maintainer (tfmorris) already describing the fix, and a maintainer (magdmartin) replying the week before.

**Requirement decisions:**
- #1858 is an umbrella issue. A maintainer asked contributors to open a separate issue for each piece, so I filed **#8081** for my part.
- I scoped it to two labels: "Re-transform up to [n] times until no change" (Transform dialog) and "Parse every [n] lines into one row" (line-based importer). Other split labels on the import screen were left for follow-up PRs to keep the change focused.
- I used tfmorris's suggested wording for the importer: "Number of lines to treat as a single row:".

To see the bug myself, I switched OpenRefine's language to Japanese (Language settings → 日本語) and opened the Transform dialog. It showed 再帰的変換 [10] 回（変化しなくなるまで）, which reads "Recursive transformation [10] times", so "up to" was lost. I also compared `translation-de.json` and found German used "Zeiten" (time periods) instead of "Mal".

**AI's role:** The agent checked each candidate issue's thread and linked PRs and summarized whether it was taken, stale or disputed. I made the final choice of issue and scope.

---

## 2. Comprehension Process

Every UI label in OpenRefine is split across three files:

```
translation-en.json   key → English sentence
        ↓
a .js file            $.i18n('key') puts the sentence into a named slot
        ↓
a .html file          empty slot + input box, in order
```

**Navigation technique: follow the text on screen back to the code.** In VS Code (Ctrl+Shift+F):

1. Searched the visible text `Re-transform up to` → found key `core-views/re-trans` in `translation-en.json`.
2. Searched the key `core-views/re-trans` → found `elmts.or_views_reTrans.text(...)` in `menu-edit-cells.js`, giving the slot name.
3. Searched the slot name `or_views_reTrans` → found `bind="or_views_reTrans"` in `text-transform-dialog.html`, right before the number input.

The importer followed the same trail (`parse-every` → `line-based-parser-ui.js` → `line-based-parser-ui.html`). One difference I noticed: the Transform dialog names slots with `bind="..."` and fills them via `elmts.<name>`, while the importer uses `id="..."` and fills them via `$('#id')`.

**Important commands:**

```powershell
git checkout -b fix-retransform-label   # work branch
.\refine.bat build                      # first build
.\refine.bat                            # run at http://127.0.0.1:3333
git checkout master                     # switch to old code for "before" screenshots
```

**AI's role:** The agent pulled the current versions of the HTML, JS and translation files so I could see the exact lines, and explained the text → key → slot trail. I then repeated the searches myself in VS Code.

---

## 3. Implementation Process

I worked solo, so I was the driver throughout, with the agent acting as navigator (suggesting where to look and reviewing my edits).

| Step | What I did | Driver | Navigator |
| --- | --- | --- | --- |
| Pick and scope the issue | Compared candidate issues, chose #1858, limited it to two labels | Me | Agent |
| Reproduce the bug | Ran OpenRefine in Japanese; compared German and Japanese translation files | Me | Agent |
| Transform dialog fix | Replaced the two labels around `repeatCountInput` with one label `or_views_reTransMax` (HTML), one `.text()` call in both `menu-edit-cells.js` and `data-table-view.js` (JS), one new key `core-views/re-trans-max-times` (JSON) | Me | Agent |
| Importer fix | Replaced the two spans with one `<label for="$lines-per-row">`, added the matching `id` to the input, one `.text()` call, new key `core-index-parser/lines-per-row` | Me | Agent |
| Remove old keys | Deleted the four old keys from `translation-en.json` only; other languages fall back to English | Me | Agent |
| File the issue | Opened #8081 with the Japanese/German examples and screenshots | Me | Agent |
| Pull request | Committed, pushed the branch, opened a PR with "Fixes #8081" and before/after screenshots | Me | Agent |

**Roles:** Because I was at the keyboard the whole time, every edit was mine, and I had to understand each line before changing it. Having the agent as navigator helped most when something broke: it could read the file I pasted and point to the exact line.

---

## 4. Running and Testing

I used a 25-row test CSV (names with accents, plus a `note` column wrapped in 0–3 layers of extra quotes) so the re-transform option had something to do.

| Write | Run / Test | Expected | Result / Obstacle |
| --- | --- | --- | --- |
| Transform dialog edits | Edit cells → Transform… | One label: "Re-transform until no change, at most this many times: [10]" | Clicking Transform… did nothing (see Obstacle 2); fixed, then label correct |
| Second Transform menu | All → Transform… | Same single label | Old names still in `data-table-view.js` (see Obstacle 2); fixed, then correct |
| Re-transform still works | Formula `value.replace(/^"|"$/, "")`, checkbox ticked | All quote layers stripped, e.g. `"""three layers"""` → `three layers` | Worked |
| Importer edits | Create project → CSV → Next → Line-based text files | One box: "Number of lines to treat as a single row: [1]" | Two boxes appeared (see Obstacle 3); fixed, then correct |
| Lines-per-row still works | Set to 2 | Preview merges every 2 lines | Worked |
| Non-English UI | Language set to Japanese | New label shows in English (no translation yet) | Shown in English, as expected |
| Before/after screenshots | `git checkout master` vs. fix branch | Old split label vs. new single label | Captured both |

**Edge case:** The new keys don't exist in other language files yet. I confirmed OpenRefine falls back to English instead of showing a blank or a raw key.

---

## 5. Obstacles Encountered

**1. First attempt only moved the input.**
I first tried putting the number box after both text pieces. That made the English read "Re-transform up to times until no change" and still gave translators two fragments. **Fix:** one complete label before the input.

**2. Transform… menu item did nothing.**
`menu-edit-cells.js` still used the old slot name, `elmts.or_views_reTrans`, after I had renamed it in the HTML. That slot no longer existed, so the code crashed and the dialog never opened.
**Fix:** renamed it to `elmts.or_views_reTransMax`.
Later I found a second copy of the same dialog code in `data-table-view.js`, used by the **All → Transform…** menu. It still used both old names and the deleted keys, so that menu would have broken too. I made the same change there.
**Lesson:** the slot name must match exactly between the HTML `bind=` / `id=` and the JS, and one HTML file can be used by more than one JS file, so I search for every use of an old name before testing.

**3. Importer showed two number boxes.**
I added the new row in `line-based-parser-ui.html` but left the old row in place, and `line-based-parser-ui.js` still filled the old id `#or-import-linesIntoRow`.
**Fix:** deleted the old row and changed the JS to `#or-import-linesPerRow`.

**4. "Failed to bind to /127.0.0.1:3333".**
An earlier OpenRefine (started from `master` for screenshots) was still holding the port, so the browser kept showing old code.
**Fix:**
```powershell
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force
.\refine.bat
```

**5. Strange English like "line minutes".**
Chrome was auto-translating the Japanese UI back into English, which made the screen confusing to read. **Fix:** turned off browser translation and switched OpenRefine back to English.

**6. GitHub said "There isn't anything to compare."**
The compare page was set to `master` vs `master`. **Fix:** chose my fix branch in the "compare" dropdown.

---

## 6. Resources Consulted

- **Claude (Opus 5.5) in claude.ai:** used to check candidate issues for open PRs and maintainer activity, pull the relevant source and translation files, explain the text → key → slot pattern, and review the files I pasted when something broke. I learned that UI bugs are often fastest to locate by searching for the visible text and following each name it leads to.
- **OpenRefine issue #1858:** tfmorris's comment defined the remaining work and the suggested wording; magdmartin's comment set the rules (separate issue, focused PR, AI disclosure).
- **OpenRefine translation files** (`translation-en.json`, `translation-de.json`, `translation-ja.json`): showed the concrete translation errors used as evidence in the issue.
- **goodfirstissue.dev:** starting list of candidate issues.
- **OpenRefine "Your first pull request" guide and PR template:** followed when opening the PR.

---

## 7. Reflection

**How I interacted with AI.** I used the agent mostly to save time on research and to catch my mistakes. It was especially useful for ruling out issues: it read each thread and linked PR and showed me that most attractive-looking issues were already taken or disputed, which I would have missed by reading titles alone. For the code, I made every edit myself, and the agent's main value was spotting the exact mismatched name when the dialog broke.

**Managing contexts.** Working solo removed the problem of two laptops and two chats, but I still juggled several contexts: the OpenRefine source, the running app, the UI language setting, and two git branches (`master` for "before" screenshots, my fix branch for "after"). Switching branches while an old server was still running caused the port conflict, so I learned to stop OpenRefine before switching.

**What I learned from writing this journal.** Writing down each obstacle made me notice that most of my bugs had the same cause: a name changed in one file but not the others. Writing it down turns that into a checklist for next time.

**What I'll take away:**
- **Check an issue's thread before starting.** Many "good first issues" already have someone on them.
- **Search for what you see on screen, then follow each name.** It finds the right files in a large codebase fast.
- **When renaming something, search for every use of the old name before testing.**
- **Stop the old server before switching branches**, or you'll be testing the wrong code.
