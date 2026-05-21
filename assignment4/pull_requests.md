# Submit Two Pull Requests

**Project:** [OpenRefine](https://github.com/OpenRefine/OpenRefine)

**Team members:** Kingson Zhang, Mouhamed Osman, Zhenyu Song, Zian Xu

---

## PR 1 — Open Project page: clear search input when switching tags

- **Title:** `fix(open-project): clear search input when switching tags`
- **Issue:** [OpenRefine/OpenRefine#7632 — On "Open project" page, search string persists after clicking "All"](https://github.com/OpenRefine/OpenRefine/issues/7632)
- **Pull request:** https://github.com/OpenRefine/OpenRefine/pull/7791
- **Branch on fork:** [fix/7632-clear-search-on-tag-change](https://github.com/eric-song-dev/OpenRefine/tree/fix/7632-clear-search-on-tag-change)

**What it fixes.** On the *Open Project* page, after typing into the search box, clicking the **All** tag (or any other tag) reset the row visibility but left the search input populated. The visible state then disagreed with the input field. Inside `Refine.OpenProjectUI._filterTags`, we now also clear `#search-input` and reset the list filter so the tag class, the list filter, and the input box are always consistent. A Cypress regression test was added to `filter_projects.cy.js`.

**Files changed.**
- `main/webapp/modules/core/scripts/index/open-project-ui.js`
- `main/tests/cypress/cypress/e2e/open-project/filter_projects.cy.js`

---

## PR 2 — Wikibase: focus first input when statement-configuration dialog opens

- **Title:** `fix(wikibase): focus first input when statement-configuration dialog opens`
- **Issue:** [OpenRefine/OpenRefine#7609 — A bug that occurs when creating the Wikidata schema](https://github.com/OpenRefine/OpenRefine/issues/7609)
- **Pull request:** https://github.com/OpenRefine/OpenRefine/pull/7792
- **Branch on fork:** [fix/7609-focus-statement-config-dialog](https://github.com/eric-song-dev/OpenRefine/tree/fix/7609-focus-statement-config-dialog)

**What it fixes.** When the statement-configuration dialog opened in the Wikidata schema editor, no element received keyboard focus, so the user had to click into the dialog before they could interact with it. Other Wikibase dialogs (`save-schema-dialog.js`, `perform-edits-dialog.js`) already focus their primary input at the end of `launch()`; we now do the same in `StatementConfigurationDialog.launch` by focusing the **Mode** select. A Cypress regression test was added in `wikibase_schema.cy.js` that opens the dialog from the `wikidata-schema` fixture project and asserts the mode select is focused.

**Files changed.**
- `extensions/wikibase/module/scripts/dialogs/statement-configuration-dialog.js`
- `main/tests/cypress/cypress/e2e/extensions/wikibase/wikibase_schema.cy.js`
