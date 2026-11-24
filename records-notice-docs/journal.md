# Group Project Journal: "Add book details…" (OpenRefine + Open Library)

Team: Andy, Kaley, Lucien, Nicole

Each member adds their own entry under each section, labelled with their name.

---

## 1. Feature Exploration

### Andy

- **GitHub:** andynilson
- **Branch:** `ol-client` (PR #27, the Open Library client). I also reviewed #25 and #29,
  and merged the pieces into `feat/book-operation` for the demo.
- **Chats:** `GroupProject/chats/andynilson/chat-1.md` (planning, building and testing the
  client, 2026-11-20).

**Getting to know the feature.** My first prompt was: "Before doing any changes can you
tell me what my role is and what I'm supposed to do". The agent read `GroupProject/AGENTS.md`
on `project`, our proposal and Nicole's PR #25. From its summary I learned:

- I own the Java code that talks to Open Library: the Books API for ISBNs, the Search API
  for titles, the User-Agent and request rate, caching, retries after a 429, error types
  and tests against a mock server;
- Lucien's operation calls my client, and his branch is the critical path for the
  checkpoint;
- Nicole's design notes had an open question for me: the exact `BookDetails` field types.

**Planning.** I pasted our proposal and asked: "Before implementing anything lets make a
plan. /grill-me one question at a time please". For each question the agent looked up the
facts in the code and on the live API, then gave options with a recommendation. My answers
became decisions 1–18 of the OL client section in `design.md`. These are the places where
my prompt changed or added something:

| My prompt | What it led to |
|---|---|
| "Yes, ISBN only" | PR 1 is the ISBN path only; title search waits for PR 2 (decision 1) |
| "I think reusing it and following the existing standards/conventions is the way to go" | Reuse OpenRefine's `HttpClient` instead of writing our own (decision 4) |
| "Tell me more about B vs C. which is better optimized for testing" | One concrete client class, plus a `refine.openlibrary.url` setting so the running app can point at a mock server (decision 6) |
| "Would it be possible to allow the end user to configure this somewhere, like adding their email on the client in some config menu or even as an option when they use the function?" | The contact email is a user preference, `openlibrary.contact`, never stored in the operation (decision 7). Nicole later added a field for it to the dialog |
| "I think it could be good to have the most verbose and precise errors on the backend, or in logs, even if we only show user simple things." | One exception with seven kinds and full details in the log, while users see "Network error" (decision 9) |
| "Can we use any of the tools OpenRefine already has to clean this data?" | The agent tried OpenRefine's own date parser on real `publish_date` values. It fails on bare years like `"2009"`, so we use a simple year pattern (decision 10) |
| "Is there a way to get the most popular version or the latest version?" | Title search picks one edition with `editions.sort` and shows popularity as `readerCount` (decision 13) |
| "Agents.md seems to be a living handoff document." | Anything another member needs from me goes in the Handoffs table in `AGENTS.md` (decision 17) |

**AI's role here:** the agent found the facts and checked them against the code and the
live API. I chose every option. Most of the time I took its recommendation; the table shows
where my question changed the result.

### Kaley

GitHub: chungkaley

**Getting to know OpenRefine.** I ran OpenRefine locally and opened a CSV project to explore the column menus. A lot of this was familiar from Assignment 3, like loading a CSV and how the Edit column menu and its code fit together. In the **Edit column** menu, "Add column by fetching URLs…" is the closest existing feature to ours, since it also calls an outside service for each row and adds a new column.

**My role.** I read our group plan to learn my part. I own the `book-title-picker` branch:

- the title search command
- the candidate picker UI
- "apply to all identical titles"
- edition preference
- the sample data
- the setup notes

For the checkpoint I owe the setup notes, the sample data and the agreed picker design.

On 2026-11-22, building title mode was reassigned to Lucien. He built the search command,
the picker and the shared title helper with Claude Code from my design (K1–K13), in PR #30.
My part became the picker design, the setup notes, the sample data, reviewing my
teammates' PRs, and the shared `design.md` and `journal.md`.

**Open Library.** I read Open Library's developer page myself
(<https://openlibrary.org/developers/api>):

| Item | What the page says |
|---|---|
| Unidentified requests | 1 request per second |
| Identified requests (`User-Agent` with app name + contact email) | 3 requests per second |
| API key | Not needed |
| Bulk downloads | They ask people not to use the API for these |

**Decisions I made:**

| Decision | Why |
|---|---|
| Setup notes at `GroupProject/setup.md` | One place for teammates and our tutor to get set up |
| Sample data at `GroupProject/sample-data/sample-books.csv` | It's for humans running the demo, so I kept it separate from Andy's test fixtures in `main/tests/data/openlibrary/` |
| Small dataset (19 rows), one case per row | Each row tests exactly one thing |
| Columns `case`, `isbn`, `title`, `isbn_mode`, `title_mode` | Reviewers can see the expected result in both modes |

**AI's role here:** The agent suggested what the setup notes and sample dataset should
cover and wrote the first drafts. I read through each draft and checked it against the
Open Library docs, the live API and my teammates' PRs before keeping anything. When
something was wrong or unclear, I changed it. I fixed the branch name and removed a line
saying title lookup "takes the first result", since that went against our picker design.
I also replaced a "not found" ISBN that turned out to be a real book, and reorganised the
dataset into separate `isbn_mode` and `title_mode` columns so the expected result for
each row is easier to read.

### Lucien

- **GitHub:** Lucien2714
- **Branches:** `feat/book-operation` (my part), then `title-mode` and `ol-client-pr2` (title mode was assigned to me), then `book-title-isbn-fields`
- **Chats:** `GroupProject/chats/lucien2714/`: `chat-1.md` (book operation, 2026-11-20), `chat-2.md` (title mode and merging the PRs, 2026-11-22), `chat-3.md` (book title and ISBN-13 fields, 2026-11-22)

**Getting to know the feature.** My first prompt asked the agent to read my teammates'
branches: "Read through the code in branches ol-client and book-dialog, what did they
change? … how does those branch affects me". From its summary of Andy's PR #27 and
Nicole's PR #25 I learned:

- my operation should follow "Add column by fetching URLs": a command, an operation and a
  background process;
- the operation must check ISBNs itself, because the dialog's check only sees the rows on
  the current page;
- title mode was not ready yet;
- `design.md` would conflict between our branches.

**Scope.** I set the checkpoint demo: "Click Add book details on an ISBN column, author
column appears". I agreed with the agent's other assumptions (all five fields from the
start, the real Open Library for the demo, a mock server in tests): "Sounds good, your
understanding aligns with ours". I also said: "We will leave the title picker as a
placeholder for now".

**ISBNs.** I asked about Apache Commons `ISBNValidator`, which ISBN form Open Library uses,
and whether to convert ISBN-10s to ISBN-13. The agent first recommended converting. Then
it checked the live API and Open Library's source code (`dynlinks.py`), and found that Open
Library matches a book by either form. My question "So there is no point converting
ISBN-13?" made us keep each ISBN in the form the cell has. Then I decided: "So we should
implement a check for valid ISBN format (both 10 and 13)." (design.md, book-operation
decision 5.)

**Later features.** On 2026-11-22 I asked for two more things: title mode, built from
Kaley's design (K1–K13), and a new field in each mode: "add Book Title if we choosed look
up by ISBN, add ISBN(ISBN-13) if we choose look up by title".

**AI's role here:** the agent read the branches, Open Library's docs and its code, and
summarised them for me. I chose the scope and answered its questions.

### Nicole

- **Branch:** `book-dialog`
- **Date:** 2026-11-22

**What we're building:** "Add book details…" is a new column menu item in OpenRefine that looks up each row's ISBN or book title on the free [Open Library API](https://openlibrary.org/developers/api) and writes author, publisher, year published, page count, and subjects into new columns.

**How I familiarized myself with OpenRefine:**
- Cloned the repo and built it locally with `./refine` to see the UI firsthand.
- Loaded a small CSV with ISBNs to understand what a user would actually be doing when they reach the "Add book details" menu item.
- Read through the group plan document which laid out the feature scope, the four sub-parts (dialog, operation, ol-client, title picker), and the design decisions we needed to settle before coding.

**Decisions I made about feature requirements:**

My part was the `book-dialog` branch, the UI that the user sees and interacts with before the fetch begins. This included:
- Where the menu item lives in the column menu
- How the dialog is laid out (source column, ISBN/title mode, field checkboxes, failure handling)
- ISBN validation behavior
- What "Review before fetching" does
- The optional status column for failure tracking

Before writing any code, I went through a structured question-by-question design review with my AI agent (Claude Code) to settle all 10 design decisions. These are documented in `GroupProject/design.md`.

**AI's role in feature exploration:** I pasted in the assignment page and our group plan, and asked the agent to map out the task dependencies to understand what I could start immediately vs. what was blocked. It told me `book-dialog` had no hard blockers and the menu item and dialog shell could start right away.

---

## 2. Comprehension Process

### Andy

**Code the agent explored for the client** (chat-1):

| Code | Why it mattered |
|---|---|
| `modules/core/src/main/java/com/google/refine/util/HttpClient.java` | Already has a throttle, retries of 429/503 with `Retry-After`, timeouts and proxy support. Its 60 s timeout was fixed, so I added a constructor that takes one |
| `ColumnAdditionByFetchingURLsOperation` | The closest existing feature. It uses `HttpClient` and caches per run |
| `CalendarParser` (behind GREL's `toDate()`) | Checked for reuse on publish dates. It fails on `"2009"`, `"c1985"` and `"[1999?]"` |
| How `refine.*` system properties are read (`refine.ini`, `GetVersionCommand`) | The pattern for the `refine.openlibrary.url` setting |
| `ProjectManager`'s preference store and the `set-preference` command | Where the contact email is kept |
| Tests in `main/tests/server/src/`, fixtures in `main/tests/data/` | TestNG, Mockito (in 39 test files in `main`) and OkHttp `MockWebServer`. WireMock is not used |
| Cypress's `cy.intercept` | It only sees the browser's requests, not the Java backend's, so a running app needs the URL setting to use a mock server |

**Live Open Library checks** (during planning):

- One request with 100 ISBNs worked (a 1,964-character URL). We still chose batches of 50.
- Search returns works, not editions: one Hobbit work had 228 publishers and 249 different
  date formats. `editions.sort=new` or `old` picks one edition in the same request, and the
  Books API accepts that edition's key.
- `language=eng` limits the chosen edition to English; `lang=en` does nothing.
- `0000000000` returns a real junk record, so it can't be a "not found" example.
- `pagination` can disagree with `number_of_pages` (*The Great Gatsby*: "134" vs 208).

**How the agent searched:** shell searches over the repo, a subagent with fresh context
for the HTTP and test facts, small runs of OpenRefine code in a scratch folder (the date
parser), and `curl` requests to the live API.

**Teammates' code.** Before reviewing, the agent read Nicole's #25, Lucien's #28 and
Kaley's #29. On #28 it checked that the operation uses my client as agreed: batches of at
most 50 distinct ISBNs, `null` → "Not found", any other failure → "Network error", and
Cancel stops without writing anything.

**Debugger breakpoints:** none. Tests against the mock server, mutation testing and live
requests answered my questions.

**Git commands I used** (through the agent):

```bash
git switch project
git pull
git switch -c ol-client
git worktree add <folder>      # a separate checkout for each reviewing subagent
git log --oneline origin/project..ol-client
git push -u origin ol-client
gh pr create --base project
gh pr view 28 --json reviews,comments
git merge origin/ol-client     # into feat/book-operation, for the demo
git merge book-dialog          # the same
```

**AI's role here:** the agent found and read the code and ran the live checks. I read its
summaries and asked follow-up questions.

### Kaley

**Teammates' PRs.** I read Andy's open PR #27 (`ol-client`) and Nicole's open PR #25
(`book-dialog`), including their design notes and exported chats, to learn what my code
must fit.

What I learned from **#27 (`ol-client`):**

| Topic | Detail |
|---|---|
| Package | `com.google.refine.openlibrary` in `main` |
| HTTP | Reuses OpenRefine's `HttpClient` |
| Batching | `MAX_BATCH_SIZE = 50` |
| User-Agent | `OpenRefine/<version> (<contact>)` when the `openlibrary.contact` preference is set (~350 ms between requests). `OpenRefine/<version>` without it (1,000 ms). |
| Retries | 429/503 retried 3 times (1 s, 2 s, 4 s, or `Retry-After`) |
| Timeout | 30 s |
| Base URL | `-Drefine.openlibrary.url` |
| Tests | OkHttp MockWebServer + TestNG, saved real responses in `main/tests/data/openlibrary/` |

Handoffs for me from #27:

- Code against `BookCandidate` and `EditionPreference` (`BEST_MATCH`, `NEWEST`,
  `OLDEST`). Store a `title -> editionKey` mapping.
- Suggested extras: a language dropdown, and sorting by `readerCount`.
- At the time, `searchByTitle` threw `UnsupportedOperationException` until Andy's PR 2.
  Lucien later built PR 2 in #30.

What I learned from **#25 (`book-dialog`):** the menu item lives in the "Edit column"
submenu (`main/webapp/modules/core/scripts/views/data-table/menu-edit-column.js`).

**Testing the real API in the browser.**

Search API:

```
https://openlibrary.org/search.json?q=dracula&fields=key,title,author_name,first_publish_year,edition_count,number_of_pages_median,subject,publisher,isbn&limit=3
```

- All 9 fields came back. `numFound` was 2,583.
- The top 3 results were different works: *Dracula*, *Bram Stoker's Dracula* (omnibus)
  and *Dracula's Guest*.
- *Dracula* alone has 1,920 editions.
- So Search returns **work-level** data. Publisher and page count for a title must come
  from a Books API call on the chosen edition. This is evidence for why the picker is
  needed.

Books API:

```
https://openlibrary.org/api/books?bibkeys=ISBN:9780140328721&format=json&jscmd=data
```

- `publish_date` was `"October 1, 1988"`. It's free text, not a year.
- Another edition page (*Thinking, Fast and Slow*) showed `"April 2, 2013"`.

**Git commands I used:**

```bash
git branch
git branch --show-current
git status
git fetch origin
git switch <branch>
git merge origin/project
git log --oneline
git branch -r
```

**Debugger breakpoints:** none. I didn't write feature code myself.

**AI's role here:** the agent summarised the PRs and docs. I checked the key facts by
opening the PRs and the live API myself.

### Lucien

**Code the agent explored for the operation** (chat-1):

| Code | Why it mattered |
|---|---|
| `ColumnAdditionByFetchingURLsOperation`, its command and its process | The pattern I copied: command, operation, `LongRunningProcess` |
| `EngineDependentCommand`, `LongRunningProcess` | Background run, progress bar and Cancel |
| `ColumnAdditionChange`, `MassChange`, `RowStarOperation`, `ColumnSplitChange` | How to add several columns as one history entry (one Undo) |
| `main/webapp/modules/core/MOD-INF/controller.js` | Where commands and operations are registered |
| `OperationDescription.properties` | Generated by the localizer plugin, not written by hand |
| `RefineTest.runAndWait` | It races a fast mock, so the tests use their own `runToEnd` helper |

**How the agent searched:** shell searches, small `jshell` prototypes, live Open Library
requests, and Open Library's own code (`openlibrary/plugins/books/dynlinks.py`) through the
GitHub API.

**Title mode** (chat-2): the agent read Kaley's design K1–K13 on PR #29 and Andy's
decisions 12, 13, 14 and 22 for PR 2 of his client. It explained why the title search and
my operation must share one helper for "the same title" (`TitleNormalizer`, Kaley's K6).

**Book title and ISBN-13 fields** (chat-3): the agent read the dialog
(`book-details-dialog.html`, `menu-edit-column.js`), `BookDetailsOperation`,
`OpenLibraryClient`, `BookDetails` and the saved test responses. Then it checked a live
Books API response: the title is in `title`, and the ISBNs are in `identifiers.isbn_13`
and `identifiers.isbn_10`. The 1937 Hobbit edition has no ISBN.

**My own reading:** mostly the agent's summaries, the design docs, and the client code in
the IDE (for example `BookDetails` in `OpenLibraryClient.java`).

**Debugger breakpoints:** none. Tests and live API calls answered my questions.

**Git commands I used** (through the agent):

```bash
git merge --no-ff origin/ol-client
git fetch origin
git switch -c book-title-isbn-fields
git log --oneline project..HEAD
git push -u origin book-title-isbn-fields
gh pr create --base project
gh pr merge 31 --merge
```

**AI's role here:** the agent found and read the code. I read its summaries and the docs
it wrote, and asked follow-up questions.

### Nicole

**Code navigation approach:**
- Used the agent to do an initial codebase exploration (`Explore` subagent) targeting dialog and column menu patterns before touching any code.
- Search terms I used: `add-column`, `fetchingURLs`, `menu-edit-column`, `DialogSystem.launch`, `DOM.loadHTML`, `DOM.bind`, `translation-en.json`.
- Key files the agent surfaced and I read:
  - `main/webapp/modules/core/scripts/views/data-table/menu-edit-column.js` — this is where all column menu items and their associated dialog functions live. My new `doAddBookDetails` function goes here.
  - `main/webapp/modules/core/scripts/dialogs/` — existing dialog HTML files showing the pattern for `DOM.bind`, `bind=` attributes, and `$.i18n()` calls.
  - `main/webapp/modules/core/langs/translation-en.json` — where all UI strings live; every visible string in OpenRefine goes here as a key.
  - `main/tests/cypress/cypress/e2e/project/grid/column/edit-column/` — existing Cypress tests for column menu features, which I modeled my own tests after.

**Key functions to understand:**
- `DialogSystem.launch(frame)` — the standard way to open a modal dialog in OpenRefine.
- `DOM.loadHTML("core", "scripts/dialogs/...")` — loads an HTML template for a dialog.
- `DOM.bind(frame)` — binds all `bind="..."` attributes in the HTML to a JS object so you can reference them as `elmts.fieldName`.
- `Refine.postCoreProcess(...)` — submits an operation to the OpenRefine engine.
- `$.i18n(key)` — internationalization lookup; every visible string needs a key.
- `$.ajax(...)` and `Refine.postCSRF(...)` — patterns for reading and writing preferences.

**How I understood what files to change:**
The agent walked me through how `doAddColumnByFetchingURLs` was structured in `menu-edit-column.js` and I modeled my implementation directly after it. Reading that one function answered most of my questions about conventions (how to populate a column dropdown, how to handle form submit, how to validate and block).

---

## 3. Implementation Process

### Andy

I worked solo with Claude Code (Claude Opus 5.5), with no human pair. The
agent and its subagents wrote all the code, tests and docs. I made the design decisions,
chose how much checking to run, and gave the word for each commit and push. So the agent
was the driver and I was the navigator.

Before any code, I set the way of working: "when i say the word you will do the
implementation, commit it, you will run a quick level checked-build, make updates as
needed, and then I will run checked-build on higher level". I think this makes sense
because a commit can always be rolled back, and saving progress step by step is a good
idea, even if mistakes are made. `checked-build` is one of my own Claude Code skills: subagents with fresh context write tests from the spec before
seeing the code, then fresh subagents that never saw the build review it and try bad input
(design.md decision 18). Later I used another skill of mine, `test-cleaner`, to merge
overlapping tests: subagents with fresh context propose merged versions, and PIT and JaCoCo
run before and after to prove nothing was lost.

**Open Library client** (chat-1, `ol-client`, PR #27):

| Step | What happened | Commits |
|---|---|---|
| Build | On "Start now", the agent added a timeout constructor to `HttpClient`, the value types, the client with batched ISBN lookup, 90 tests and the docs | `4e47e8b`, `c8732fb`, `efb6211`, `31d469d` |
| Quick check | A subagent with fresh context wrote tests from the spec without seeing the code, and another checked the code against the spec. They found two problems with Cancel and one with a JSON `null` record | `c75477c`, `2eba8b9`, `28775d4`, `a70823d`, `0ebcb3f` |
| Mutation testing | I asked: "install PIT and run the mutation tests". PIT found 3 cases no test checked | `593db74` |
| Timing test | A test that timed two requests failed once at 1,007 ms. It now measures the gap where the mock server receives them | `2cdea42` |
| Bigger check | I ran `checked-build` again with more subagents: several test suites written separately from the spec, and fresh subagents trying bad input. They found 11 real problems and 3 gaps in the tests. I made decisions 20–22 | `fc980de`, `88dcc95`, `a3ee485`, `1285290`, `6fde30b`, `0647fe2` |
| Merge the tests | I ran my `test-cleaner` skill: "First make a commit. Then run /test-cleaner Clean the HttpClient tests I added on this branch. PIT and JaCoCo are required before and after." It merged 73 `HttpClient` tests in 11 files into 14 in one file, while keeping everything the old tests checked. The method is similar to the NATO alphabet tests we merged in class: tests that each check one input are combined into one test whose input shows all of them, so `test('a')` and `test('z')` become `test('az')`. After asking whether we were doing too much, I said "Yes, consolidate" and ran it on the Open Library tests: 743 tests in 14 files became 113 in 2. Coverage and killed mutants stayed the same. Then tests for the last gaps | `9c218c0`, `14c97ba` |
| Chat export | `GroupProject/chats/andynilson/chat-1.md` | `0e5fd4b` |
| PR #27 | Opened into `project`. Nicole approved it and Kaley reviewed it | PR #27 |
| Audit and review fixes | A fresh subagent compared the old and new tests with the spec and found a real bug (a malformed charset name). Fixed together with Nicole's review points and a javadoc error that would fail CI | `1f71bb1` |
| PR 2 of the client | Lucien's agent built it on `ol-client-pr2` from my decisions 12, 13, 14 and 22, and he merged it in PR #30 | Lucien's commits |

**Teammates' branches** (2026-11-21):

| Step | What happened | Commits |
|---|---|---|
| Review of #25 | I reviewed Nicole's dialog on 2026-11-20: rows counted only on the current page, Cancel inside the `<form>`, lowercase `x`, unscoped selectors, the parameter contract | Review comment |
| Fixes on `book-dialog` | Fixed the points from the review of #28 that were in Nicole's dialog code: the "Review" `alert`, the ISBN check now matches Lucien's `IsbnCleaner`, one loop for the field checks | `2684c61` |
| Demo branch | Merged `ol-client` and `book-dialog` into `feat/book-operation`, so one branch has the client, the operation and the dialog | `d489f1d`, `2c830c7` |
| Command | `AddBookDetailsCommand` refuses an `invalidIsbnAction` other than `"skip"`, so rows are never skipped without saying so; the parameter table in `AGENTS.md` says what the command does with each parameter | `92eee4f` |
| Review of #29 | Checked Kaley's dataset and setup notes against our code and the live API | Review comment |

**Who decided what:**

| Decision | Who |
|---|---|
| OL client decisions 1–17 (scope, `BookDetails`, batching, HTTP layer, package, test seam, contact email, input rules, errors, field rules, batch size, cache, title search, language, timeouts, tests, branches) | Me, each from the agent's options |
| Decision 18: the build and check process | Me |
| Decision 19: the fixes from the bigger check | The agent recommended them; I approved |
| Decisions 20–22: blank proxy settings mean "no proxy"; no retry when `Retry-After` is over 60 s; Cancel during a request and a shared throttle wait for PR 2 | Me |
| Mutation testing, the bigger check, merging the tests, PIT and JaCoCo as proof | Me |
| The test constructor that takes an `HttpClient`, removing control characters from the contact, refusing a timeout of 0 or less | The agent (it said so when it showed me the plan) |
| Keep `9799999999990` in Kaley's dataset, though Nicole's review asked to change it | Me, after the agent checked its check digit and the live API |

**Role switching:** none with a human. With the agent I stayed the navigator.

**AI's role here:** the agent wrote every line of code and every doc. Subagents with fresh
context wrote tests from the spec and reviewed the code. I made the decisions above, chose
when to check more, and approved each commit and push.

### Kaley

I worked on my own tasks, with my teammates in the group chat as navigators. Claude Code
wrote the files, and a separate Claude chat (claude.ai) helped me plan and check facts.

| Step | What I did | Commit | Driver | Navigator |
|---|---|---|---|---|
| Setup notes | Wrote `GroupProject/setup.md`: endpoints, rate limits, User-Agent, client behaviour, running locally, testing. Updated it twice with facts from my API tests and from PR #27 and #25. | `82d34e6` | Kaley | Group (async, via group chat); agents assisting |
| Fix branch base | Merged `project` into my branch, which I had created from `master` by mistake. | `e684622` | Kaley | Group (async, via group chat); agents assisting |
| Sample dataset | Created `sample-books.csv`, then reorganised it into `case` / `isbn_mode` / `title_mode` columns. Both commits got the same message by mistake; `ca45e5b` is the reorganised version. | `18c1d02`, `ca45e5b` | Kaley | Group (async, via group chat); agents assisting |
| Link dataset | Linked the sample dataset in `setup.md`. | `5f1ec36` | Kaley | Group (async, via group chat); agents assisting |
| Picker design | Added my title picker decisions (K1–K13) and diagrams to design.md | `0563d4e` | Kaley | Group (async, via group chat); agents assisting |
| Shared journal | Set up `journal.md` with the 7 sections and a subsection for each member, and filled in mine | `e9c43f0`, `6de107c` | Kaley | Group (async, via group chat); agents assisting |
| Chat export | `GroupProject/chats/chungkaley/chat-1.md` | `74e34ec` | Kaley | — |
| PR #29 review fixes | Fixed Nicole's and Andy's review points: the test command, the setup notes, K2 and K9. Kept `9799999999990` after checking its check digit | TODO (Kaley): hash | Kaley | Group (async, via PR reviews); agents assisting |

**My reviews of teammates' PRs**, and what changed because of them:

| PR | What I raised | What changed |
|---|---|---|
| #27 (Andy) | Retry waits with a contact, and contact emails with accents being changed into someone else's address | A contact that would have to be changed is no longer sent (`c763e18`) |
| #28 (Lucien) | Stray Cypress files and `package-lock.json`, an invalid "lowercase x" ISBN (`044101452x`) in a Cypress test, an unused spy, and empty columns when no row has a valid ISBN | Files removed (`6f02e1d`), tests fixed (`435ba18`), and a run with no valid ISBN now fails with a message (`53bc5c1`) |
| #25 (Nicole) | `chi` instead of `zho` for Chinese, an "Any language" option, and language names not going through the translation file | `chi` and "Any language" added (`246765d`) |

**Picker design decisions (2026-11-20).** I made these in a question-by-question interview
with Claude Code. The full options, pros and cons, and tradeoffs are in `design.md`,
section 3 (K1–K13).

| # | Question | Decision |
|---|---|---|
| K1 | When the picker appears | After "Fetch details" in title mode, before the operation runs |
| K2 | A title with exactly one match | Auto-select it, and show it pre-selected when the picker is open. If a wrong one slips through, undo fixes it. |
| K3 | A title with no match | Mark it not found (no mapping entry, blank cells) |
| K4 | Candidates shown | 5 per title, each showing title, authors, first published year and the edition that will be fetched (publisher, publish date, language) |
| K5 | Sort order | Open Library's relevance order. `readerCount` is shown as a small "readers" number, and nothing is shown when there's no count. |
| K6 | "Identical" titles | Ignore case and extra spaces, using one shared normalise helper |
| K7 | Several ambiguous titles | One combined list with a section per title |
| K8 | Edition preference | Default `BEST_MATCH`, set in Nicole's dialog (Title mode only) |
| K9 | Language filter | Yes: a dropdown in Nicole's dialog, defaulting to English (`eng`) |
| K10 | "Skip" and Cancel | Skip means not found. Cancel changes nothing and adds no history entry. |
| K11 | Stored for undo/replay | Normalised title → editionKey, as a new operation parameter (e.g. `titleEditions`) |
| K12 | Search command | `SearchBookTitlesCommand extends Command` in `com.google.refine.commands.column`, registered as `search-book-titles` |
| K13 | Before Andy's PR 2 | A client factory plus a Mockito stub in tests; no temporary stub code ships |

Follow-up decisions on the same day:

- **Where the titles come from:** the search command reads them from the project column on
  the server. The browser only sends the column name, edition preference, language and the
  engine config (row filter). This keeps requests small and uses the same filtered rows as
  Lucien's operation.
- **Cypress test location:**
  `main/tests/cypress/cypress/e2e/project/grid/column/edit-column/book_title_picker.cy.js`.
  The `edit-column` folder already exists in the repo.

Handoffs to teammates (all done by 2026-11-22, see `design.md`):

- **Nicole:** the Edition and Language dropdowns, with `chi` and "Any language" (`246765d`).
  The picker hook on "Fetch details" was added in Lucien's title-mode work (`cc77a0b`).
- **Lucien:** agreed to the `titleEditions` parameter, enabled title mode (`065a48e`), and
  added the shared `TitleNormalizer` helper (`d5f9ed0`). The search uses the same row filter
  as his operation.

**AI's role here (picker design):** for each question, the agent read the relevant code
(Lucien's `BookDetailsOperation`, Andy's client classes, Nicole's menu code) and gave 2–3
options with pros and cons and a recommendation. I made every decision. For K1–K13 I chose
the agent's recommended option, and for K2 I wrote my own reasoning. Three later choices
were mine, because the agent had left them as TODOs: showing the "readers" number in K5,
and the two follow-up decisions. The agent first suggested building
`design.md` on Nicole's or Andy's existing version. I said no and had it create a fresh
file with placeholders for my teammates.

**Role switching:** This project was bigger than our earlier assignments, so pairing
live with one teammate wasn't practical, and our roles worked more asynchronously. We
talked through the project and our ideas as a group, and Nicole wrote up a plan we
could all reference and use to divide the tasks. After that, each of us worked on our
own part and asked questions in the group chat. When I was doing my tasks, I was the
driver and my teammates in the group chat acted as navigators, answering questions and
pointing out issues. When they were working on their parts, it was the other way
around.

**AI's role here:** Claude Code wrote and edited `setup.md` and the CSV from my prompts.
The claude.ai chat helped me plan the steps and check facts before I committed.

### Lucien

I worked solo with Claude Code, with no human pair. The agent and its subagents wrote all
the code, tests and docs. I set the goals, answered its design questions, chose how the
work ran, and asked for the merges. So the agent was the driver and I was the navigator.

**Book operation** (chat-1, 2026-11-20, `feat/book-operation`, PR #28):

| Step | What happened | Commits |
|---|---|---|
| Merge Andy's client | I asked for it; the agent chose a merge commit (`--no-ff`) | `a744d17` |
| Design | The agent wrote 13 decisions in `design.md` (superpowers:brainstorming skill). A reviewer subagent found an error in decision 8, and the agent fixed it | `de56677` |
| Plan | `GroupProject/plans/2026-11-20-book-operation.md`: 8 tasks, tests first (superpowers:writing-plans) | `de56677` |
| Build | I chose subagent-driven work in a separate worktree (`.claude/worktrees/book-operation-impl`). Each task had an implementer subagent and a reviewer subagent | `7364fa1` `IsbnCleaner`, `3de7d56` operation, `16f8984` batching, `5374e5c` Cancel and failures, `2495da6` command, `9286359` status column, `5a30e83` mock-server test |
| Demo merge | Merged Nicole's `book-dialog` so the menu item exists | `9e01ec6` |
| Final review | A final reviewer subagent found three issues; the agent fixed them and the reviewer checked again | `1a1b2aa` |
| Finish | I asked to "merge it back into feat/book-operation"; chat exported | `6296e31` |

**Between the exported chats** (2026-11-21 and 22):

- On my branch: subjects joined with `| ` as the group decided (`7c7c777`), and no columns
  added when no row has a valid ISBN, after Kaley's review of PR #28 (`53bc5c1`).
- `TitleNormalizer` (`d5f9ed0`), and title lookups with the editions chosen in the picker
  (`065a48e`).
- Commits on teammates' branches:
  - Nicole's `book-dialog`: `6f02e1d`, `435ba18`, `84fc4a4`, `246765d`;
  - Andy's `ol-client`: `c763e18`, `e387b6e`;
  - Kaley's `book-title-picker`: `3fe8447`.


**Title mode** (chat-2, 2026-11-22, `ol-client-pr2` and `title-mode`, PR #30):

| Step | What happened | Commits |
|---|---|---|
| PR 2 of Andy's client | A background agent built it from Andy's decisions 12, 13, 14 and 22; the main agent reviewed it before merging | `c444417`, `5835d45`, `d2e089a`, `61670f3`, `2203e26`, `8d1cc43`, `b131044` |
| Title search and picker | Built from Kaley's K1–K13 | `73b20f6`, `167a544`, `cc77a0b`, `1103a34`, `9eb8348` |
| Docs | I said: "These task has been assigned to me, so remove the docs where it says that kaley needs to review it(TODOs)" | `7bbb0bf`, `69e2060`, `bd51de0`, `e103183`, `4b04010`, `5032c8f`, merge `5d0e8b8` |
| PRs | Opened PR #30, then merged the group's PRs in order: #27 (`578fc19`), #28 (`d4faf49`), #25 (`ba5b4b5`), #30 (`bf0f37f`), #29 (`6c44d3f`, docs combined in `e89c8ea`) | |

**Book title and ISBN-13 fields** (chat-3, 2026-11-22, `book-title-isbn-fields`, PR #31):

| Step | What happened | Commits |
|---|---|---|
| Client | `BookDetails` gets `getTitle()` and `getIsbn13()`, read from the Books API response | `b7f20df` |
| Operation | New field keys `title` and `isbn13`, written as text cells | `6cd8d27` |
| Dialog | "Book title" in ISBN mode, "ISBN-13" in title mode, unchecked by default | `64ffa80` |
| Docs | `design.md` book-operation decision 16, `AGENTS.md` | `db07abc` |
| PR | I asked to push, open a PR into `project` and merge it | PR #31, merge `168f27e` |

Each step was test-first: the agent wrote the tests, watched them fail, then wrote the code.

**Who decided what:**

| Decision | Who |
|---|---|
| Checkpoint demo; title mode as a placeholder at first | Me |
| Check ISBN-10 and ISBN-13 with check digits; do not convert ISBN-10s | Me, after my question about converting |
| Subagent-driven work (the agent recommended working "Native"), in a separate worktree | Me ("Use the subagent-driven.") |
| Fail with no columns when no row has a valid ISBN | Me (design.md decision 11) |
| `titleEditions`, the mapping from Kaley's K11 | Kaley proposed it, I agreed |
| Merge all PRs now, in order, though Nicole's "changes requested" on #28 and #29 were still open and #30 had no review; Kaley's docs as her own section | Me, choosing from the agent's options |
| A book title field in ISBN mode and an ISBN-13 field in title mode | Me |
| Book-operation decisions 5–13 (cleanup rules, class names, JSON, `MassChange`, batching, number cells, errors) and the details of decision 16 (defaults, column names, ISBN-10 conversion) | The agent. `design.md` marks each one "Claude's call" |

**Role switching:** none with a human. With the agent I stayed the navigator.

**AI's role here:** the agent wrote every line of code and every doc, using subagents for
the book operation and for PR 2 of the client. I chose the goals and the way of working,
and made the decisions in the table above.

### Nicole

All work was done solo on the `book-dialog` branch. I was both driver and navigator throughout, using the agent to write code while I made all design and implementation decisions.

| Step | What I did | AI role |
|------|-----------|---------|
| 1 | Created `GroupProject/AGENTS.md` with feature context for future agents | Agent wrote initial draft, I reviewed |
| 2 | Went through 10 design questions one by one (menu location, source column, mode control, field defaults, column names, blank validation, ISBN warning timing, review dialog, fetch behavior, status column) | Agent presented options with pros/cons; I made every decision |
| 3 | Agent created `GroupProject/design.md` documenting all decisions with rationale and alternatives considered | Agent wrote it based on our Q&A |
| 4 | Agent added i18n strings to `translation-en.json` (23 new keys) | Agent wrote |
| 5 | Agent added `doAddBookDetails()` function and menu item to `menu-edit-column.js` (~194 lines) | Agent wrote |
| 6 | Agent created `book-details-dialog.html` | Agent wrote |
| 7 | I reviewed and tested the initial dialog in-browser, identified visual issues | Me |
| 8 | Addressed code review feedback (cancel button bug, lowercase ISBN-x, unscoped radio selectors, warning label) | Agent fixed each issue after I confirmed the approach |
| 9 | Agent documented the `add-book-details` operation parameter contract in `GroupProject/AGENTS.md` for Lucien | Agent wrote |
| 10 | Agent wrote Cypress tests covering 5 cases (open dialog, cancel, no fields selected, invalid ISBN warning, title mode hides warning) | Agent wrote |
| 11 | Added Edition and Language dropdowns for title mode (at Kaley's request from her PR review) | Agent implemented |
| 12 | Added optional contact email field saved to preferences for Open Library rate limit | Agent implemented |

**Role switching:** I worked solo, so there was no formal driver/navigator switching with a human partner. With the agent, I stayed in the navigator role throughout, asking questions, reviewing outputs, and making decisions. The agent wrote all the code. I found this setup worked well because I could focus on what the feature should do rather than syntax details.

---

## 4. Running and Testing

### Andy

**How to run the tests.** You need JDK 21 or newer and Apache Maven. Build core in the same
run, or `main` may use an old core jar from `~/.m2`:

```bash
mvn -pl modules/core,main test -Dtest='HttpClientTests,OpenLibrary*Tests,IsbnCleanerTests,BookDetails*Tests,AddBookDetailsCommandTests' -Dsurefire.failIfNoSpecifiedTests=false
```

*If `mvn` isn't installed:*

- **macOS:** `brew install maven` (Homebrew also installs a JDK if you don't have one).
- **Linux:** `sudo apt install maven` (or your distribution's package manager), or
  `sdk install maven` with SDKMAN.
- **Windows, or any system:** download the binary zip from
  <https://maven.apache.org/download.cgi>, unzip it, and add its `bin` folder to your
  `PATH`.

Then run `mvn -v` and check which Java it uses: it must be 21 or newer, or set `JAVA_HOME`
to a JDK 21+ first. Maven 3.9 is known to work (`setup.md` lists 3.9.16).

*With IntelliJ (what I used, since Maven isn't on my PATH):* IntelliJ comes with its own
Maven, so point `MAVEN_HOME` at it and run the same command. `./refine` also finds Maven
through `MAVEN_HOME`, so `./refine build` works the same way.

```bash
export MAVEN_HOME="/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3"
"$MAVEN_HOME/bin/mvn" -pl modules/core,main test -Dtest='HttpClientTests,OpenLibrary*Tests,IsbnCleanerTests,BookDetails*Tests,AddBookDetailsCommandTests' -Dsurefire.failIfNoSpecifiedTests=false
```

**Mutation testing and coverage:** PIT 1.30.2 with its TestNG plugin, run from PIT's
command-line tool out of a scratch folder, so `pom.xml` didn't change. JaCoCo for line and
branch coverage. Both used the same JVM settings as the project's own test runs (French
locale, `cp1252`).

**Results:**

| Stage | Tests | Result |
|---|---|---|
| First build | 90 Open Library tests, 3 `HttpClient` tests | All passed on the first run; `./refine lint` clean |
| After the quick check | 287 | All passing |
| PIT, first run | Open Library classes | 136 of 156 mutants killed; 138 after 3 new tests |
| After the bigger check | 1,360: 74 in core, 1,286 in `main` (including the 22 existing tests of "Add column by fetching URLs" and data extension) | All passing |
| After merging the tests | 24 runs in core, 373 in `main` | All passing; coverage the same line by line; the same mutants killed |
| After the last gap tests and review fixes (`1f71bb1`) | 28 runs in core, 383 in `main` | All passing; CI's lint and javadoc command passes |
| PIT, final | Open Library classes | 176 of 179 killed. The 3 left can't change any result |
| PIT, `HttpClient` | Whole class | 45 of 69 killed, the same before and after merging the tests. Most survivors are in older code this branch didn't touch |
| JaCoCo, final | Open Library classes | 309 of 309 lines, 219 of 222 branches |
| Lucien's branch with my fixes merged | 457 related runs, his 74 included | All passing |

**Mocks:** OkHttp `MockWebServer` serves saved real Open Library responses
(`main/tests/data/openlibrary/`). No test calls the live API. I checked the live API by
hand instead: during planning, and for Kaley's dataset (all 13 valid ISBNs found in one
request; three have no page count, so their cell stays blank).

**Write → Run/Test → Debug loops:**

*Loop 1: first build*

- Write: the client and 90 tests from the plan.
- Run: all 90 passed on the first run, including slow responses and retries.
- Debug: nothing failed. But the same agent wrote the code and the tests, which is why the
  next loop used tests written without the code.

*Loop 2: tests from the spec*

- Write: a subagent with fresh context wrote 158 tests from the spec alone.
- Run: they showed that a Cancel during a request returned normally, and that a record
  sent as JSON `null` was read as "not found". Then a test showed the worst one: a Cancel
  during the wait between requests was swallowed by `HttpClient`'s throttle, and the
  request was sent anyway.
- Debug: fixed in the client (`c75477c`) and in core's throttle (`28775d4`).

*Loop 3: mutation testing*

- Run: PIT killed 136 of 156 mutants.
- Debug: three real gaps. No test sent a status 300, removing the line that keeps the
  interrupt flag went unnoticed, and nothing reached the case where OpenRefine isn't
  started. New tests (`593db74`) brought it to 138.

*Loop 4: a timing test that failed at random*

- Run: a test allowed 900 ms for two requests with a contact, and failed once at 1,007 ms.
  The time included start-up costs.
- Debug: it now measures the gap between the requests at the mock server (`2cdea42`). The
  gaps measured 350–355 ms with a contact and 1,002–1,005 ms without, and the test passed
  three times in a row in a fresh JVM.

*Loop 5: bad input*

- Write: fresh subagents tried missing and empty input, malformed input, sizes and limits,
  and real use, such as cancelling at each stage. The main agent ran every failing test
  itself before calling it a bug.
- Found: a byte-order mark before the JSON, a page count written as `208.0`, a snippet cut
  in half an emoji, names made only of no-break spaces, bad base URLs, JSON followed by
  junk, an interrupt while a connection was being set up, a blank proxy setting that
  crashed `HttpClient`, and `Retry-After: 3600` making each retry wait an hour.
- Debug: fixed in `fc980de`, `a3ee485` and `1285290`, with tests.

*Loop 6: merging the tests with `test-cleaner`*

- Write: subagents with fresh context each proposed a merged version of the `HttpClient`
  tests on their own.
- Run: PIT and JaCoCo before and after, in a throwaway clone and then in my checkout,
  compared mutant by mutant and line by line. One proposal was rejected because it no
  longer passed an empty header array anywhere. The chosen one had added a license header
  the file never had, which was taken out.
- Result: 73 tests in 11 files became 14 (24 runs). PIT killed the same 45 of 69 mutants in
  `HttpClient`, and JaCoCo covered the same 98 lines and 44 branches (`9c218c0`). The same
  process took the Open Library tests from 743 tests in 14 files to 113 in 2, with the same
  coverage and the same mutants killed (`14c97ba`).

*Loop 7: an audit after merging the tests*

- Run: a fresh subagent compared the old tests, the new tests and the spec, with the
  coverage and PIT data but none of the main agent's conclusions. It agreed nothing was
  lost, and found a bug: a `Content-Type` like `charset='utf-8'` threw an unchecked
  `IllegalCharsetNameException`.
- Debug: the new test failed 4 of 4 on the old code. It's now a `BAD_RESPONSE`
  (`1f71bb1`).

**Not checked:** timing on Linux or Windows, and the connection-pool timeout (it needs
more than 5 requests at once).

**AI's role here:** the agent and its subagents wrote and ran every test. I chose when to
check more (the quick check, PIT, the bigger check, merging the tests) and asked for proof
before accepting the merged tests.

### Kaley

I didn't write the feature code: Lucien built title mode from my design. My testing was
checking my docs and data, and reviewing my teammates' tests.

**Loop 1: sample data check digits**

- **Write:** sample dataset with valid, invalid and edge-case ISBNs.
- **Run/Test:** computed the check digit of every ISBN. All valid ones pass and the 3
  invalid ones fail. The "should be" digits come from real ISBNs:

  | Invalid ISBN | Based on | Book |
  |---|---|---|
  | `9780141439840` | `9780141439846` | Dracula |
  | `9781451673310` | `9781451673319` | Fahrenheit 451 |
  | `9780141439510` | `9780141439518` | Pride and Prejudice |

- **Debug:** no problems found.

**Loop 2: do the ISBNs match real books?**

- **Run/Test:** opened `https://openlibrary.org/isbn/<ISBN>` for every valid ISBN and
  confirmed the book matched.
- **Bug found:** the agent's "not in Open Library" ISBN `9798888888889` **is** in Open
  Library ("Frijol de Fran y Patri").
- **Debug/fix:** replaced it with `9799999999990`. It has a valid check digit and returns
  "404 - Page Not Found".

  ```
  https://openlibrary.org/isbn/9799999999990   ->  404 - Page Not Found
  ```

**Edge cases in the dataset:**

| Case | Expected |
|---|---|
| Valid ISBN-13 | found |
| ISBN-10 | found |
| Hyphenated ISBN | found (hyphens removed) |
| Wrong check digit | skipped before any network call |
| Valid but not in Open Library | not found |
| Blank ISBN | skipped |
| Ambiguous titles (Emma, Hamlet, Dracula) | picker |
| Pride and Prejudice on 3 rows | one choice applies to all |
| Nonsense title | not found |

**Watch-out:** import the `isbn` column as text. Otherwise the ISBN-10s lose their leading
0 (`0439708184`, `0553380168`, `0316769487`).

**AI's role here:** the agent drafted the dataset and the expected outcomes. Its "not
found" ISBN was wrong, and I only caught it by checking every ISBN against the live site
myself.

### Lucien

**How to run the tests** (Windows):

```bash
# Java tests for the feature. Build core in the same run: with -pl main alone, Maven can
# use an old core jar from the local repository (see Obstacles)
mvn -pl modules/core,main test -Dtest='IsbnCleanerTests,OpenLibrary*Tests,BookDetails*Tests,AddBookDetailsCommandTests,SearchBookTitlesCommandTests,TitleNormalizerTests' -Dsurefire.failIfNoSpecifiedTests=false
```

```powershell
# Cypress, with OpenRefine already running on port 3333 (see Obstacles for the two settings)
cd main/tests/cypress
$env:YARN_NODE_LINKER = 'node-modules'; Remove-Item Env:ELECTRON_RUN_AS_NODE
corepack yarn install
corepack yarn cypress run --browser electron --headless --spec cypress/e2e/project/grid/column/edit-column/book_details.cy.js
```

**Results:**

| Stage | Tests | Result |
|---|---|---|
| Book operation, before any code | `OpenLibrary*Tests` and `ColumnAdditionByFetchingURLsOperationTests` | 362 run, 0 failures |
| Book operation, finished | Full `main` suite | 1256 run, 0 failures; `./refine lint` clean |
| Book operation, smoke test | An agent started a server on port 3334: `get-version` answered, `add-book-details` asked for a CSRF token (so it exists), an unknown command gave 404 | Passed |
| Title mode | Java tests for the feature | 674 run, 0 failures, 1 skipped on Windows |
| Title mode | Cypress: 4 new picker tests (Open Library replies stubbed) and Nicole's 8 | 12 / 12 |
| Title mode, live | Real Open Library through the dialog and picker: both spellings of *The Great Gatsby* got the chosen book, *Dracula* was filled, a made-up title got "Not found", one history entry | Worked |
| Title and ISBN-13 fields | Java tests for the feature | 684 run, 0 failures |
| Title and ISBN-13 fields | Full `core` and `main` suites | core 238 run (1 skipped), main 1517 run, 0 failures |
| Title and ISBN-13 fields | Cypress: `book_details.cy.js` and `book_title_picker.cy.js` | 16 / 16 |
| Whole feature, by me | End to end in the browser against the real Open Library: ISBN mode, title mode with the picker, the book title and ISBN-13 fields | Passed |

**Mocks:** a Mockito mock of `OpenLibraryClient`, and OkHttp `MockWebServer` serving saved
real responses (`main/tests/data/openlibrary/`). No test calls the real API.

**Write → Run/Test → Debug loops:**

*Loop 1: book operation, task by task*

- Write: for each plan task, a subagent wrote the failing test first, then the code.
- Run: the task's test class, then the full `main` suite at the end.
- Debug: the final review found that number cells such as `9.780306406157E12` were marked
  "Invalid ISBN", that an unexpected error left the process "running" forever, and that the
  main Cancel path had no test. Fixed in `1a1b2aa`.

*Loop 2: title mode, live run*

- Run: the real Open Library through the picker.
- Debug: some author lists had 50 names. The picker now shows at most 3 authors, then
  "and N more" (`1103a34`).

*Loop 3: book title and ISBN-13 fields*

- Write: tests first. They did not compile, because `getTitle()`, `getIsbn13()` and
  `IsbnCleaner.toIsbn13` did not exist yet.
- Run: the new Cypress tests against the old dialog gave 8 passing and 4 failing
  (`Expected to find element: [bind="bookTitleRow"], but never found it`).
- Debug: after the code, all passed, except one timing test in the full run. It also failed
  on `project`, so the cause was not my change (see Obstacles).

**My end-to-end test:** after the agents' tests, I tested the whole feature end to end in
OpenRefine myself, in the browser, against the real Open Library: ISBN mode, title mode
with the picker, and the new book title and ISBN-13 fields. Everything worked.

The last automated runs (the full `core` and `main` suites, and the 16 Cypress tests) were
on `project` after all the group's PRs were merged, together with the title and ISBN-13
change.

### Nicole

**How to run:**
```bash
cd <repo root>
./refine
# Wait for "INFO: Started ServerConnector" on port 3333
```
Then open `http://localhost:3333`, load a CSV with an `isbn` column, right-click the column → Edit column → "Add book details…"

**Manual verification I conducted:**
- Opened the dialog from the column menu and confirmed it rendered correctly.
- Verified the source column dropdown pre-filled with the clicked column.
- Switched from ISBN mode to Title mode and confirmed the invalid ISBN warning hid and the Edition/Language dropdowns appeared.
- Unchecked all fields and clicked Fetch and confirmed the "please select at least one field" alert appeared.
- Cleared a column name input and clicked Fetch and confirmed the blank name alert appeared.
- Clicked Cancel and confirmed the dialog closed without submitting (this was the fix for the `type="button"` bug).
- Entered a contact email, clicked Fetch, reopened the dialog and confirmed the email was pre-filled from the saved preference.

**Cypress tests:**
I ran the Cypress test file I wrote (`book_details.cy.js`) against a live OpenRefine instance. Steps:
```bash
cd main/tests/cypress
npm install          # first time only
npx cypress open
# pick E2E Testing → Chrome → book_details.cy.js
```
All 5 tests passed.

**Edge cases I considered:**
- Lowercase `x` check digit in ISBN-10 (e.g. `044101452x`) was initially counted as invalid. Fixed by calling `.toUpperCase()` before the check digit comparison.
- Unscoped radio button selector: `$('input[name="book-details-mode"]')` would match across multiple open dialogs. Fixed by scoping to `frame.find(...)`.
- Cancel button inside `<form>` without `type="button"` defaulted to `submit` and triggered the fetch. Fixed by adding `type="button"`.
- Invalid ISBN count on large projects: the validation only checks the loaded page (10-50 rows). I labeled the warning "in visible rows" to be clear about this limitation. Full backend validation is a follow-up.

**Write → Run/Test → Debug loop:**

*Loop 1 — initial dialog*
- Write: agent added menu item, dialog HTML, JS function, i18n strings.
- Run: opened the dialog in-browser.
- Debug: noticed some visual quirks and that the cancel button was triggering the form.

*Loop 2 — code review fixes*
- Write: applied all 5 fixes from code review (cancel button, lowercase x, scoped selectors, warning label, AGENTS.md contract).
- Run: manual click-test of cancel and ISBN warning.
- Debug: everything passed.

*Loop 3 — Cypress tests*
- Write: agent wrote `book_details.cy.js`.
- Run: `npm install` then `npx cypress open` — initially failed because I ran Cypress from the repo root instead of `main/tests/cypress`.
- Debug: navigated to the right directory, all 5 tests passed.

---

## 5. Obstacles Encountered

### Andy

**1. Maven was not on my PATH**

- **Fix:** use the Maven that comes with IntelliJ (`MAVEN_HOME` in section 4).

**2. Cancel was ignored during the wait between requests**

- **Cause:** `HttpClient`'s throttle caught the interrupt and sent the request anyway.
- **Fix:** in core (`28775d4`): it now throws `InterruptedIOException` and keeps the
  interrupt flag. This also makes "Add column by fetching URLs" stop sooner on Cancel, so I
  pointed it out in PR #27.

**3. A timing test that failed at random**

- **Cause:** its 900 ms limit included start-up costs, and it failed once at 1,007 ms.
- **Fix:** measure the gap between requests at the mock server (`2cdea42`).

**4. Too many tests**

- **Cause:** after the bigger check there were 12,067 lines of tests for about 1,470 lines
  of code, about 8 lines of test per line of code. OpenRefine as a whole has about 0.5, and
  "Add column by fetching URLs" about 1. One boundary change failed 13 tests.
- I asked: "My intention is not to compromise on the coverage or reliability of the tests,
  but more curious to see if we're potentially doing too much."
- **Fix:** merged the overlapping tests (`9c218c0`, `14c97ba`): 3,329 lines in 3 files.
  PIT and JaCoCo measured the same before and after, mutant by mutant and line by line.

**5. Parallel checks shared one Maven repository**

- **Cause:** each reviewing subagent had its own git worktree, but all of them read core's
  jar from `~/.m2`. Installing a new core jar mid-review would have changed what they were
  testing.
- **Fix:** hold the core install until every review had finished.

**6. CI's javadoc step**

- **Cause:** it runs with `-source 11` and rejected the private `record` in
  `OpenLibraryClient`. The agent found it by running CI's command while I reviewed Lucien's
  #28, which carried my commits.
- **Fix:** a small class instead of the record (`1f71bb1`). Lucien hit the same thing
  (his obstacle 2).

**7. A bug that every earlier check missed**

- **Cause:** a malformed charset name in `Content-Type` escaped as an unchecked exception.
- **Fix:** found by the audit after merging the tests, fixed in `1f71bb1`.

**8. A test that couldn't fail**

- **Cause:** the agent's first test for Nicole's `fromHostsToPattern` point used a host
  that matched neither the old code nor the fix, so it caught nothing.
- **Fix:** a test that fails on the old code and passes on the fix (`*.any.*` must not
  match `x.anyhost.com`). **Remember:** check that a new test fails before the fix.

**9. Open Library surprises**

- `0000000000` returns a real junk record. The tests use `9799999999990` for "not found".
- Chinese is `chi` on Open Library, not `zho`. Nicole's dialog sent `zho`, which finds
  nothing; I raised it on #29.
- `pagination` and `number_of_pages` can disagree, so we only use `number_of_pages`.

**10. Coordination with teammates**

- Every PR added its own `design.md`. We decided early to accept the conflict and fix it
  when merging; Lucien combined them.
- I changed teammates' branches: Nicole's `book-dialog` (`2684c61`) and Lucien's
  `feat/book-operation` (`d489f1d`, `2c830c7`, `92eee4f`). The `book-dialog` merge also
  brought stray Cypress example files and a replaced Yarn lockfile into #28. Lucien removed
  them (`6f02e1d`).
- Lucien changed my branch after Kaley's review of #27: a contact email that would have to
  be changed to fit in the header (for example one with an accent) is now not sent at all,
  instead of being altered into someone else's address (`c763e18`).
- Lucien's agent built PR 2 of my client from my written decisions.

**AI's role here:** most of these the agent found and fixed while running the checks.
Obstacles 7 and 8 were the agent's own misses, caught by a fresh subagent and by checking
that a test fails on the old code.

### Kaley

**1. Books API `literal_error` on `jscmd`**

- **Cause:** I pasted extra words onto the end of the URL. The API returned:
  ```json
  {"detail":[{"type":"literal_error","loc":["query","jscmd"],...}]}
  ```
- **Fix:** use the clean URL.
  ```
  https://openlibrary.org/api/books?bibkeys=ISBN:9780140328721&format=json&jscmd=data
  ```
- **Remember:** `jscmd` only accepts `details`, `data` or `viewapi`.

**2. An agent edit to `setup.md` never ran**

- **Cause:** the edit didn't apply, so old TODOs stayed in the file. I only noticed when
  the agent said the file never contained a value it should have added.
- **Remember:** re-read the file after every agent edit.

**3. Draft contradicted our picker design**

- **Cause:** the agent's draft said title lookup "takes the first result".
- **Fix:** changed to "when several works match, the user picks one".

**4. Wrong branch in the setup notes**

- **Cause:** the agent's draft told teammates to switch to `book-title-picker`.
- **Fix:** changed to `project` (the integration branch).

**5. Branch created from `master` instead of `project`**

- **Fix (no force-push):**
  ```bash
  git fetch origin
  git merge origin/project
  ```

**6. Stuck in Vim on the merge message**

- **Cause:** I closed the terminal to get out of Vim, then ran `git commit --no-edit`.
  That saved Git's instruction lines into the message.
- **Fix:**
  ```bash
  git commit --amend -m "Merge origin/project into book-title-picker"
  git push --force-with-lease
  ```
- **Stop Vim opening in future:**
  ```bash
  git config --global core.editor notepad
  ```
- **Note:** AGENTS.md says not to force-push. I used `--force-with-lease` only on my own
  branch, before anyone else had pulled it, to fix the commit message. Next time I'll
  leave a messy message rather than force-push.

**7. Two commits with the same message (`18c1d02`, `ca45e5b`)**

- **Fix:** left as is.
- **Remember:** give a follow-up commit its own message.

**8. "LF will be replaced by CRLF" warnings on Windows**

- **Cause:** Git converts line endings. These warnings are harmless.

**9. Open Library pages sometimes errored while checking ISBNs**

- **Fix:** I re-checked those ISBNs in my own browser.

**AI's role here:** three of these obstacles (#2, #3, #4) were agent mistakes that I
caught by reviewing its output.

### Lucien

**1. A wrong design decision**

- **Cause:** decision 8 first said that adding five columns would need five Undos.
  `MassChange` already makes them one history entry.
- **Fix:** a reviewer subagent caught it, not me, and the agent rewrote the decision.

**2. `mvn install` of `main` fails**

- **Cause:** the javadoc step runs with `-source 11` and rejects the Java `record` in the
  client.
- **Fix:** don't install `main`; use no records in new code.

**3. Tests that raced the mock**

- **Cause:** `RefineTest.runAndWait` can miss the end of a process when a fast mock
  answers at once.
- **Fix:** the tests use their own `runToEnd` helper.

**4. `./refine` does not work in Git Bash**

- **Cause:** the script treats only Cygwin as Windows.
- **Fix:** use `refine.bat`.

**5. An agent started OpenRefine on my real data directory**

- **Fix:** the agent checked, and nothing of mine had changed.
- **Remember:** give test servers their own data directory (`-d`).

**6. Bugs found in the final review** (`1a1b2aa`)

- ISBNs stored as number cells were marked "Invalid ISBN".
- An unexpected error left the process "running" forever.
- The main Cancel path had no test.

**7. The same docs on every branch**

- **Cause:** every PR adds its own `design.md`, and #29 also adds its own `journal.md`. In a
  trial merge, a squash merge of #27 also made #28 conflict.
- **Fix:** merge commits in the order #27 → #28 → #25 → #30 → #29, and combine Kaley's docs
  by hand (`e89c8ea`).

**8. Coordination with teammates**

- PRs #28 and #29 were merged while Nicole's "changes requested" from 2026-11-21 were still
  open, and #30 was merged with no review.
- The agent pushed commits to Nicole's, Andy's and Kaley's branches, so they need to pull.
  Nicole had not seen the `menu-edit-column.js` change, and Andy may not know that PR 2 of
  his client was built.

**9. Author lists with 50 names** (live run)

- **Fix:** the picker shows at most 3 authors, then "and N more" (`1103a34`).

**10. A timing test failed only on my machine**

- **Cause:** `mvn -pl main test` used an old `core` jar from the local Maven repository
  (`E:\cache\openrefine\maven`), from before the thread-safe throttle. The test failed in the
  same way on `project`, without my change.
- **Fix:** run `mvn -pl modules/core,main test`, which builds `core` first.

**11. Cypress would not start on Windows**

- **Cause 1:** Yarn 4's default Plug'n'Play mode fails on Windows with Node 24
  (`EBADF: bad file descriptor`).
  - **Fix:** `YARN_NODE_LINKER=node-modules`. It changes no tracked file, and `node_modules`
    is ignored by git.
- **Cause 2:** VS Code sets `ELECTRON_RUN_AS_NODE=1`, so Cypress's Electron ran as plain
  Node (`bad option: --smoke-test`).
  - **Fix:** remove the variable before running Cypress.

**12. Lint**

- ESLint's `cypress/unsafe-to-chain-command` rejected `.clear().type(...)`. Fixed: two
  separate commands.
- Prettier reported 106 files. They only had Windows line endings from
  `core.autocrlf=true`; with `--end-of-line auto` every file passes.

**13. A failing check on every PR**

- **Cause:** `transfer_tags` is OpenRefine's workflow that copies labels from a linked issue.
  Our PRs have no linked issue. It builds and tests nothing, and GitHub does not block the
  merge.

**AI's role here:** most mistakes were caught by the agent's reviewer subagents and by the
tests, not by me reading the code. The Windows problems (#10–#12) the agent found and fixed
while running the tests.

### Nicole

**1. Cypress configuration error on first run**

Error: `Cannot find module 'cypress'` when opening `cypress.config.js`.

Cause: I ran `npx cypress open` from the repo root. The `cypress.config.js` is in `main/tests/cypress/` and its `require('cypress')` couldn't find the package because `node_modules/cypress` hadn't been installed in that directory.

Fix: 
```bash
cd main/tests/cypress
npm install
npx cypress open
```

Future reference: always run Cypress from `main/tests/cypress/`, not the repo root.

**2. Cancel button submitting the form**

The cancel button in `book-details-dialog.html` was inside a `<form>` without `type="button"`. HTML defaults `<button>` elements inside a form to `type="submit"`, so clicking Cancel was both closing the dialog and triggering the fetch operation.

Fix: added `type="button"` to the cancel button element.

**3. Lowercase ISBN-10 check digit**

ISBNs like `044101452x` (valid, the x is a check character equal to 10) were counted as invalid because the validation only checked for uppercase `X`. 

Fix: added `.toUpperCase()` after stripping dashes and spaces in the validation loop.

**4. Unscoped radio button query**

`$('input[name="book-details-mode"]:checked')` queries the entire document DOM, not just the dialog. If multiple OpenRefine dialogs were open simultaneously, this selector could pick up the wrong radio button.

Fix: replaced all three occurrences with `frame.find('input[name="book-details-mode"]:checked')`.

**5. Coordination with teammates on shared files**

`design.md` and `journal.md` are shared across all branches. Kaley's `book-title-picker` PR added structure and content to both files. I need to merge her changes in and add my content under my own sections, not overwrite hers.

Resolution: Kaley's PR adds section headings for each member. I'll keep her structure and add my entries under the Nicole section.

---

## 6. Resources Consulted

### Andy

| Resource | How I used it | What I learned |
|---|---|---|
| Claude Code (Claude Opus 5.5) | Planned with me, wrote the code, tests and docs, ran the checks, opened PR #27 and posted my reviews | Got better at using Claude Code to navigate a project with several people working in parallel |
| Subagents with fresh context | Wrote tests from the spec without seeing the code, reviewed the code, tried bad input, and audited the merged tests | They found most of the real bugs in my client; the first build's own 90 tests found none |
| My own Claude Code skills: `checked-build` and `test-cleaner` | `checked-build` for the build-and-check process (decision 18); `test-cleaner` for merging the tests with proof that nothing was lost | Tried new configurations and saw the difference in performance: the larger run found 11 problems the quick run had missed |
| The `grill-me` skill | The question-by-question plan (decisions 1–18) | 18 decisions in about 45 minutes, each with options and a recommendation |
| PIT 1.30.2 and its TestNG plugin | Mutation testing, run from the command line without changing `pom.xml` | Passing tests can still miss a deleted line; PIT shows which |
| JaCoCo | Line and branch coverage before and after merging the tests | Saw some of its blind spots more clearly, and how agents used alongside it give better results: the audit found a real bug and test gaps that coverage alone didn't show |
| <https://openlibrary.org/developers/api> and the Books and Search API docs | Endpoints, rate limits, the User-Agent rule | 1 request per second without a contact, 3 with one |
| Live Open Library requests | Batch size, `editions.sort`, `language`, junk records, the sample ISBNs | Search results are works, not editions |
| OpenRefine's code: `HttpClient`, `ColumnAdditionByFetchingURLsOperation`, `CalendarParser`, the existing `MockWebServer` tests | What to reuse and which conventions to follow | `HttpClient` already had the throttle and the retries |
| Nicole's review of #27 | Four points: a discarded return value, an old off-by-one in `HttpClient`, two comments | All fixed in `1f71bb1` |
| Kaley's review of #27 | Retry waits with a contact, and contact emails with accents | Led to Lucien's `c763e18` |
| Teammates' PRs #25, #28, #29 and `GroupProject/AGENTS.md` | What each branch needs from mine | A better understanding of the overall flow of the feature |

**AI's role here:** the agent was my main tool. I relied on fresh subagents and on PIT and
JaCoCo, not the agent's word, to tell me the tests were good enough.

### Kaley

| Resource | How I used it | What I learned |
|---|---|---|
| Claude Code (Claude Opus 5.5) in the repo | Wrote `setup.md` and the CSV | Fast at drafting files, but every edit needs re-reading |
| Claude chat on claude.ai (Claude Opus 5.5) | Explained my tasks, wrote prompts for Claude Code, reviewed its output, checked ISBN check digits and looked up ISBNs | A second agent is useful for checking the first one's work |
| <https://openlibrary.org/developers/api> | Rate limits and usage rules | 1 vs 3 requests/second depending on the User-Agent; no bulk downloads |
| <https://openlibrary.org/dev/docs/api/books> | Books API parameters | `jscmd` only accepts `details`, `data` or `viewapi` |
| <https://openlibrary.org/dev/docs/api/search> | Search API parameters | Search results are works; the `fields` parameter chooses what comes back |
| Live Open Library pages and API responses | Checked fields, counts and ISBNs (section 2) | Search is work-level and `publish_date` is free text |
| Andy's PR #27 (`ol-client`) | Client behaviour and handoffs | The client interfaces my design builds on |
| Nicole's PR #25 (`book-dialog`) | Where the menu item lives | It's in the "Edit column" submenu |
| Our group plan | My role and checkpoint deliverables | What I owe for the checkpoint |
| Lucien's PR #30 (`title-mode`) | Compared the built search command and picker with my design | His agent made some choices I didn't, e.g. at most 3 authors per book, and the picker's button says "Fetch details" |

**AI's role here:** both agents were resources, not sources of truth. I checked their
claims against the docs, the live API and the PRs.

### Lucien

| Resource | How I used it | What I learned |
|---|---|---|
| Claude Code in the repo (VS Code extension) | Explored the code, wrote the designs, plans, code, tests and docs, and opened and merged PRs | |
| Subagents: Sonnet 5.5 (implementers and reviewers), Fable 5.2 (final review) | Book-operation tasks and their reviews | The reviewer subagents caught most of the bugs (Obstacles #1 and #6) |
| Claude Opus 5.5 | The main agent of the title and ISBN-13 session (the model is not recorded in the other two chats) | |
| Superpowers skills: brainstorming, writing-plans, subagent-driven-development, using-git-worktrees, test-driven-development; mattpocock-skills:pr | Design questions, the plan, the build with subagents, tests first, PR descriptions | |
| <https://openlibrary.org/developers/api> and the Books API docs | Endpoints, rate limits, `jscmd=data` | |
| Live Open Library API responses | Checked ISBN matching, titles, `identifiers` and author lists | Open Library matches ISBN-10 and ISBN-13 to the same record and does not check check digits |
| Open Library's `dynlinks.py` (GitHub) | How Open Library reads an ISBN | It only removes hyphens |
| Apache Commons Validator 1.11.0 (`ISBNValidator`) | ISBN check digits and ISBN-10 → ISBN-13 conversion | Already a dependency of core |
| Andy's PR #27 and his design decisions | The client I build on, and PR 2 | |
| Nicole's PR #25 and her design decisions | The dialog and the parameters it sends | |
| Kaley's PR #29 (K1–K13) and her review of PR #28 | The title-mode design; failing a run with no valid ISBN | |
| `GroupProject/AGENTS.md` handoffs table | What each branch needs from the others | |

**AI's role here:** the agent was my main tool. It checked its own claims against the live
API, the tests and teammates' PRs, and showed me the results.

### Nicole

**Agentic tool used:** Claude Code (Sonnet 4.6) via the CLI.

**How I used the agent:**
- Initial codebase exploration to find relevant files without manually reading everything.
- Presented design tradeoffs with pros/cons so I could make informed decisions.
- Wrote the HTML, JS, and i18n for the dialog after I specified exactly what I wanted.
- Applied code review fixes one by one after I confirmed the approach for each.
- Wrote Cypress tests after I confirmed the test cases.
- Answered questions about OpenRefine patterns (e.g. "how does fetch URLs handle on-error? how does set-preference work?").

I didn't let the agent decide anything. I always gave the final answer on design questions. When it made a recommendation I disagreed with (e.g. it suggested stubbing out the "Review" dialog given the checkpoint deadline), I overruled it.

**Web resources:**
- Open Library Books API docs (for understanding the ISBN lookup endpoint and rate limits).
- Kaley's PR #29 — read it to understand what she built for the title picker and what she was asking me to add (Edition and Language dropdowns).

**Peers:**
- Kaley (chungkaley) — her PR review comment explicitly asked me to add Edition and Language dropdowns and to wire up the title flow. I implemented those.
- Lucien — coordination on the `add-book-details` operation parameter contract, documented in `GroupProject/AGENTS.md`.

---

## 7. Reflection

### Andy

**How I worked with the agent.** I kept three steps apart, and the agent waited for my word
before each one:

- plan: the agent asked me one design question at a time, with options and a
  recommendation, and wrote the answers into `design.md` and `AGENTS.md`;
- build: on "Start now" it wrote the code and tests and committed them;
- check: subagents with fresh context, mutation testing and coverage told me whether the
  tests were good enough, instead of the agent's own word.

**Where I changed the outcome:**

- The contact email: my question made it a user preference that never goes into the
  operation's JSON, so it can't leak through project history or exports.
- The errors: full details in the log, a simple "Network error" for the user.
- Reusing OpenRefine's `HttpClient` and its conventions instead of writing our own.
- Asking for the most popular or latest edition, which became `editions.sort` and
  `readerCount` for title search.
- Asking for mutation testing, a bigger check, and later whether we were "potentially
  doing too much", which led to merging the tests with `test-cleaner`, with proof that
  nothing was lost.
- Decisions 20–22, on proxy settings, long `Retry-After` waits and Cancel.

**Where the agent decided.** A few details it filled in itself and told me about: the test
constructor that takes an `HttpClient`, removing control characters from the contact
(changed after Kaley's review), and refusing a timeout of 0 or less. The fixes in decision
19 were its recommendations, which I approved.

**Mistakes and who caught them.** The first build passed all 90 of its own tests. Fresh
subagents then found 3 real problems in the quick check and 11 in the bigger one, and an
audit found one more after that. The worst was Cancel being swallowed during the wait
between requests, which is where a Cancel usually lands. My teammates' reviews caught
things too: Nicole found an old off-by-one in `HttpClient`, and Kaley found that an
accented email was being changed into someone else's address. The chats don't show me
finding a bug in my client by reading the code.

**Navigating contexts.** I worked mostly on `ol-client`, then reviewed and fixed things on
Nicole's and Lucien's branches so the demo branch had everything. `AGENTS.md` was the
handoff between us: what each branch needs from the others, with a status. Because the
PR 2 decisions were written down in `design.md`, Lucien's agent could build PR 2 of my
client from them without me.

**What I learned:**

- **Tests written by the author describe what the code does.** All 90 of the first tests
  passed, and the bugs were still there. Tests from the spec, by an agent that never saw
  the code, found them.
- **Passing tests aren't the whole story.** PIT showed lines I could delete without any
  test noticing.
- **More tests isn't better tests.** At 8 lines of test per line of code, one change
  failed 13 tests. A quarter of the lines, with the same coverage and more mutants killed,
  was easier to read.
- **Check that a test can fail.** One test the agent wrote passed on both the old and the
  fixed code.
- **Write decisions down.** The written decisions let a teammate's agent build my PR 2,
  and let reviewers check the code against something.
- **Real data is messier than docs.** Free-text dates, junk records, `chi` instead of
  `zho`, and page counts that disagree only showed up in live requests.

**What I'll take away for future work:**

- Plan before building, one decision at a time, and record who decided what.
- Get tests written from the spec by someone, or some agent, that hasn't seen the code.
- Use mutation testing and coverage to decide when the tests are good enough, and again to
  prove a cleanup lost nothing.
- Run CI's own commands locally before opening a PR.
- When I change a teammate's branch, tell them and note it in the handoffs table.
- Export my agent chats as I go, not at the end.

### Kaley

*Checkpoint draft — to be finished for the final PR.*

I used two agents: one wrote files, and the other helped me plan and check its work.

The agent was most useful for drafting docs and spotting gaps. It was least reliable on
facts it hadn't checked: a made-up "not found" ISBN that turned out to exist, "take the
first result", the wrong branch, and an edit that never applied. I learned to check every
claim against the live API or the PRs before committing.

Reading teammates' PRs early stopped my docs from contradicting their code. For example,
the User-Agent format had changed from our plan.

Git lessons:

- check the branch before creating a new one
- merge `project` often
- give each commit its own message

TODO (Kaley): in your own words, a few sentences about title mode being reassigned to
Lucien (what it was like designing something someone else built) and how your reviews led
to real fixes in teammates' code.

**What I'll take away.** In group work, it's important to divide tasks clearly and
communicate deadlines, expectations and deliverables early. Our group got confused
about how to handle shared files like `design.md` and `journal.md`, because we were
used to working with one partner on one branch. With four people on separate
branches, we had to agree on a structure first: one shared file, with a section for
each person. Next time I'd settle questions like that at the start of the project,
before anyone begins writing.

### Lucien


**How I worked with the agent.** I gave the goal in one or two sentences, confirm with the agent how we should implement it, what details should we decide on. The agent then:

- read the code and my teammates' branches;
- asked me design questions;
- wrote a design and a plan; Wait for my review and approval.
- built everything test-first, often with subagents.

For the book operation every task had an implementer subagent and a reviewer subagent, and
a final reviewer checked the whole branch.

**Where I changed the outcome:**

- My question about converting ISBN-10s changed the agent's recommendation: we kept each
  ISBN in the form the cell has.
- I chose subagent-driven work over the agent's recommendation, in a separate worktree.
- After Kaley's review, I decided that a run with no valid ISBN fails and adds no columns.
- I asked for the book title and ISBN-13 fields.

**Where the agent decided.** `design.md` marks the agent's decisions as "Claude's call". In
the book operation, that is decisions 5–13: the cleanup rules, class names, the JSON, how
columns are added, batching and errors. In decision 16 it is the defaults, the column
names and the ISBN-10 conversion. I reviewed it and approved the agent's decisions.

**Mistakes and who caught them.** The reviewer subagents and the tests caught most
mistakes: the wrong Undo count in decision 8, number cells marked "Invalid ISBN", a process
that could stay "running", and an untested Cancel path. The chats do not show me finding a
bug by reading the code.

**Navigating contexts.** I worked across my own branches (`feat/book-operation`,
`title-mode`, `book-title-isbn-fields`) and teammates' branches. The `AGENTS.md` handoffs
table and `design.md` were how the agent and my teammates knew what each branch needed. The
shared docs were also the main source of merge conflicts.

**What I learned:**

- **Decide before building.** Each feature started with a design and a plan that I reviewed
  and approved. Because the decisions were settled first, the build went fast, and later
  changes (like the title and ISBN-13 fields) fit in without rethinking everything.
- **My questions matter more than my typing.** The agent wrote all the code, but a short
  question from me, like "So there is no point converting ISBN-13?", changed what got
  built. Asking "why" about a recommendation is where I added the most value.
- **A second reviewer catches what the first one misses.** The reviewer subagents found
  real bugs (the wrong Undo count, number cells marked "Invalid ISBN", a process that could
  hang), and the tests kept them from coming back.
- **Check the real thing, not just the docs.** Live Open Library calls showed things no doc
  said: it matches ISBN-10 and ISBN-13 to the same book, some author lists have 50 names,
  and old editions have no ISBN at all.
- **Not every red test is my bug.** One failing test also failed on `project` without my
  change; the cause was an old jar on my machine. Running the same test on the base branch
  first saved a lot of time.
- **Shared docs are shared memory.** `AGENTS.md` and `design.md` let a fresh agent session,
  and my teammates, pick up the work. They are also where most of our merge conflicts came
  from.

**What I'll take away for future work:**

- Start every feature with a short design and plan, and read them before any code is
  written.
- Question the agent's recommendations instead of accepting them, and write down who
  decided what.
- Keep test-first work and an independent review step, even for small changes.
- Test the feature end to end myself at the end; passing tests are not the same as a
  working feature.
- When I change code that a teammate owns, tell them right away and leave a note in the
  handoffs table.
- Keep commits small and export my agent chats as I go, not at the end.

### Nicole

**How I interacted with AI and what its role was:**

I used Claude Code throughout this feature. My approach was to keep myself in the decision-maker role and use the agent to implement after I decided what I wanted. Before writing any code, I had the agent explore the codebase and then ask me design questions one by one. I found this more productive than asking it to "build the dialog" and then reviewing what it came up with.

The agent was strongest at:
- Finding the right files and patterns quickly in a large codebase.
- Presenting options with real tradeoffs rather than just picking one.
- Applying specific, targeted fixes from code review without changing anything else.

The agent was weakest at:
- Knowing when to stop. It sometimes suggested extra features or "improvements" beyond what I asked for. I had to be direct about keeping scope tight.
- Understanding cross-branch dependencies. I had to supply the context about what Kaley and Lucien were building; it couldn't infer that from the code alone.

**Navigating different contexts while pair programming:**

Since I worked solo on `book-dialog`, I didn't have a human partner to switch contexts with. What I did have was the need to coordinate with teammates asynchronously via AGENTS.md, design.md, and PR review comments. The AGENTS.md file I created at the start of the session turned out to be useful — when I picked up the work in a new session, I could start the agent fresh with just that file and it would have the context it needed.

**What I learned:**

The most valuable thing I learned was to settle design decisions before coding. Going through 10 questions in sequence before touching any files made the actual implementation faster. When a code review came back with bugs, I could fix each one without reconsidering the design because it was already settled.

I also learned to be specific about what I want from the agent. Vague prompts ("add the dialog") produce generic results. Specific prompts ("fix the cancel button — it's inside a form without type=button so it defaults to submit") produce exactly what you need.

**What I'll take away for future work:**

- Use an AI agent for codebase exploration before diving into implementation. It's faster than reading files manually and surfaces patterns I would have missed.
- Always make design decisions before writing code, and document them with rationale and alternatives. The design doc is useful during development and during code review.
- Keep the agent writing code while I make judgment calls on design.
- Export chat history as you go, not at the end of the project.
