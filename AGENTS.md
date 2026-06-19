# AGENTS.md - OpenRefine Project Information

This file provides important information about the OpenRefine project and repository for AI agents and developers.

## Project Overview

**OpenRefine** is a Java-based power tool that allows you to load data, understand it, clean it up, reconcile it, and augment it with data coming from the web. All from a web browser and the comfort and privacy of your own computer.

- **License**: BSD-3-Clause
- **Official Website**: https://openrefine.org
- **Documentation**: https://openrefine.org/docs
- **Community Forum**: https://forum.openrefine.org
- **Version**: 3.10-SNAPSHOT (as of this writing)

## Technology Stack

### Backend
- **Language**: Java
- **Minimum Java Version**: JDK 11
- **Maximum Java Version**: JDK 21
- **Build Tool**: Apache Maven 
- **Project Structure**: Multi-module Maven project

### Frontend
- **JavaScript Libraries**: jQuery, jQuery UI, Select2, Underscore.js
- **Internationalization**: @wikimedia/jquery.i18n
- **Build/Package Management**: Node.js 18+ and npm 8.11.0+
- **E2E Testing**: Cypress with Node.js 20

### Project Modules
- `modules/core` - Core OpenRefine functionality
- `modules/grel` - GREL (General Refine Expression Language)
- `main` - Main application and webapp
- `server` - Server components
- `extensions` - Extension modules (database, jython, pc-axis, wikibase)
- `packaging` - Distribution packaging
- `benchmark` - Performance benchmarks

## Build System

### Building OpenRefine

```bash
./refine build
```

This command:
- Compiles all Java code using Maven
- Builds the webapp frontend
- Prepares all modules and extensions
- Creates necessary artifacts

### Running OpenRefine

```bash
# On Mac OS and Linux
./refine

# On Windows
refine.bat
```

**Configuration Options:**
- `-c <path>` - Path to refine.ini file (default: ./refine.ini)
- `-d <path>` - Path to the data directory
- `-H <host>` - Expected host header value
- `-i <interface>` - Network interface to bind (default: 127.0.0.1)
- `-m <memory>` - JVM min and max memory heap size (default: 1400M)
- `-p <port>` - Port to listen on (default: 3333)
- `-v <level>` - Verbosity level [error,warn,info,debug,trace]
- `-w <path>` - Path to the webapp (default: main/webapp)
- `--debug` - Enable JVM debugging on port 8000
- `--jmx` - Enable JMX monitoring

### Other Build Commands

```bash
./refine clean              # Clean compiled classes
./refine test              # Run all tests
./refine extensions_test   # Run extension tests
./refine server_test       # Run server tests
./refine e2e_tests         # Run end-to-end tests
./refine lint              # Reformat source code according to conventions
./refine mac_dist <ver>    # Make MacOS binary distribution
./refine windows_dist <ver> # Make Windows binary distribution
./refine linux_dist <ver>  # Make Linux binary distribution
./refine dist <ver>        # Make all distributions
```

## Testing Infrastructure

### Unit Tests
- **Framework**: JUnit (Java)
- **Location**: Throughout the codebase in `src/test` directories
- **Run**: `./refine test` or `./refine server_test` or `./refine extensions_test`

### End-to-End Tests
- **Framework**: Cypress
- **Location**: `main/tests/cypress/`
- **Setup**: 
  ```bash
  cd main/tests/cypress
  npm i -g yarn
  yarn install
  ```
- **Run**: `./refine e2e_tests`
- **Browser**: Chrome (default in CI)
- **Configuration**: Uses environment variables like `CYPRESS_BROWSER`, `CYPRESS_SPECS`, `CYPRESS_GROUP`

### Testing in CI
The project uses GitHub Actions for continuous integration:
- **E2E Tests**: `.github/workflows/pull_request_e2e.yml` - Runs Cypress tests on pull requests
- **Server Tests**: `.github/workflows/pull_request_server.yml` - Runs server-side tests
- **CodeQL Analysis**: Security scanning for Java and JavaScript

## Development Workflow

### Code Contributions
1. Fork the repository
2. Create a branch named with the issue number and brief description
3. Make changes (avoid unrelated modifications)
4. Create unit and/or E2E tests for your changes
5. Run `./refine lint` before submitting (CI will fail if lint fails)
6. Ensure all tests pass
7. Submit a pull request for review

### Code Style and Formatting
- **Linting**: Run `./refine lint` to reformat code according to OpenRefine conventions
- **EditorConfig**: The repository includes `.editorconfig` for consistent formatting:
  - Charset: UTF-8
  - Line endings: LF
  - Indent: 4 spaces (2 for YAML, JSON, LESS, shell scripts)
  - Max line length: 120 characters
  - Java imports organized with specific layout rules
  - Insert final newline

### Important Files
- `pom.xml` - Root Maven project configuration
- `refine` / `refine.bat` - Main launch scripts
- `refine.ini` - Runtime configuration (can be created)
- `.editorconfig` - Code formatting rules
- `CONTRIBUTING.md` - Contribution guidelines
- `GOVERNANCE.md` - Project governance model

## Extension System

OpenRefine supports a plugin architecture for extending functionality. Extensions are located in the `extensions/` directory:
- **database** - Database import/export functionality
- **jython** - Python scripting support via Jython
- **pc-axis** - PC-Axis file format support
- **wikibase** - Wikibase/Wikidata integration

Each extension is a Maven module with its own structure and can include:
- Java backend code
- JavaScript frontend code
- Internationalization files
- Specific tests

## Project Dependencies

### Backend Dependencies
- Jackson (2.21.0) - JSON processing
- Various Apache Commons libraries
- Jetty - Embedded web server
- SLF4J - Logging
- TestNG/JUnit - Testing

### Frontend Dependencies
- jQuery (3.7.1)
- jQuery UI (1.13.3)
- jQuery Migrate (3.6.0)
- Select2 (4.1.0-rc.0)
- js-cookie (3.0.5)
- tablesorter (2.32.0)
- underscore (1.13.7)

## Data and Configuration

- **Default Data Directory**: OS-dependent, can be specified with `-d` flag
- **Default Port**: 3333
- **Default Interface**: 127.0.0.1 (localhost only)
- **Default Memory**: 1400M (can be configured)

## Internationalization

OpenRefine supports multiple languages:
- Translation files located in `*/langs/` directories
- Uses Weblate for community translations
- Managed through @wikimedia/jquery.i18n

## Community and Support

- **Forum**: https://forum.openrefine.org
- **Issue Tracker**: https://github.com/OpenRefine/OpenRefine/issues
- **Developer Forum**: https://forum.openrefine.org/c/dev/8
- **Gitter Chat**: https://gitter.im/OpenRefine/OpenRefine
- **Twitter**: @openrefine

## Fiscal Sponsorship

Since 2020, OpenRefine is fiscally sponsored by Code for Science and Society (CS&S).

## Important Notes for AI Agents

1. **Always run `./refine lint` before submitting code** - The CI will fail if code is not properly formatted
2. **Tests are required** - Both unit tests and E2E tests should be added for new features
3. **Multi-module project** - Changes may span multiple Maven modules (core, grel, main, server, extensions)
4. **Java version compatibility** - Code must work with Java 11-21
5. **Build before running** - Always run `./refine build` after code changes before testing
6. **Memory configuration** - For E2E tests, memory is configured via refine.ini (REFINE_MIN_MEMORY, REFINE_MEMORY)
7. **Extension isolation** - Extensions are separate modules with their own dependencies
8. **Frontend changes** - May require Node.js/npm operations in `main/webapp/`
9. **Path-specific workflows** - Some CI workflows ignore specific paths (e.g., translation files, IDE configs)
10. **No force push** - The repository does not allow force pushes or rebase operations that rewrite history

## Getting Started for Development

```bash
# Clone the repository
git clone https://github.com/OpenRefine/OpenRefine.git
cd OpenRefine

# Ensure you have required tools installed:
# - JDK 11 or newer (up to JDK 21)
# - Apache Maven
# - Node.js 18 or newer

# Build the project
./refine build

# Run OpenRefine
./refine

# Access at http://127.0.0.1:3333
```

## Additional Resources

- Developer Documentation: https://github.com/OpenRefine/OpenRefine/wiki/Documentation-For-Developers
- User Manual: https://openrefine.org/docs
- Technical Reference: https://openrefine.org/docs/technical-reference/
- Writing Extensions: https://openrefine.org/docs/technical-reference/writing-extensions
- Contributing Guide: See CONTRIBUTING.md in the repository
