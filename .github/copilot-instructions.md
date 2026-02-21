# Copilot Instructions for OpenRefine

## Project Overview

OpenRefine is a Java-based power tool for loading, cleaning, reconciling, and augmenting data, accessed through a web browser. The backend is written in Java and the frontend uses JavaScript/jQuery. The project uses a multi-module Maven build.

## Repository Structure

- **main/** – Core application: Java source (`src/main/java`), web application (`webapp/`), and tests (`src/test/java`, `tests/cypress/`)
- **modules/grel/** – GREL expression language module
- **modules/core/** – Core module (shared code)
- **extensions/** – Optional extensions: `database`, `jython`, `pc-axis`, `wikibase`
- **server/** – Embedded Jetty server infrastructure
- **packaging/** – Build and distribution packaging scripts
- **conf/** – Application configuration files

## Requirements

- **JDK 11** or newer
- **Apache Maven 3.8+**
- **Node.js 18** or newer (for frontend dependencies)

## Build, Run, and Test

```bash
# Run OpenRefine from source (Mac/Linux)
./refine

# Run OpenRefine from source (Windows)
refine.bat

# Run all Java unit tests
./refine test

# Run only server tests
./refine server_test

# Run only extension tests
./refine extensions_test

# Run end-to-end (Cypress) tests
./refine e2e_tests

# Reformat source code (run before submitting a PR)
./refine lint
```

## Code Style

- Always run `./refine lint` before submitting a PR. CI will fail if linting is not applied.
- Java code is formatted using the Maven Formatter plugin (`formatter:format`) and import ordering is enforced with `impsort:sort`.
- Follow the existing patterns in the file you are editing.

## Pull Request Guidelines

- Branch names should include the issue number and a brief description (e.g., `1234-fix-csv-import`).
- Keep changes focused: avoid unrelated modifications in a single PR.
- Add unit tests and/or end-to-end tests for every bug fix or new feature.
- Ensure all tests pass before submitting.
- Java unit tests live under `main/src/test/java/` and `modules/*/src/test/java/`.
- End-to-end tests are Cypress tests located in `main/tests/cypress/`.

## Key Architecture Notes

- The backend exposes a REST/JSON API consumed by the single-page frontend in `main/webapp/`.
- Extensions follow a plugin architecture; see `extensions/` for examples.
- Reconciliation service integration is a core feature; be careful when modifying reconciliation-related code.
- Internationalisation (i18n) strings for the UI live in `main/webapp/modules/core/langs/` and are also managed via [Weblate](https://hosted.weblate.org/engage/openrefine/).

## Useful Links

- [User Manual](https://openrefine.org/docs)
- [Developer Guide & Architecture](https://github.com/OpenRefine/OpenRefine/wiki/Documentation-For-Developers)
- [Contributing Guide](./CONTRIBUTING.md)
- [Project Governance](./GOVERNANCE.md)
- [Community Forum](https://forum.openrefine.org)
