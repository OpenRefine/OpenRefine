---
id: functional-tests
title: Functional tests
sidebar_label: Functional tests
---

import useBaseUrl from '@docusaurus/useBaseUrl';

You will need:

-   [Node.js 10 or 12 and above](https://nodejs.org)
-   [Yarn or NPM](https://yarnpkg.com/)
-   A Unix/Linux shell environment or the Windows command line

## Installation

To install Cypress and dependencies, run :

```
cd ./main/tests/cypress
yarn install
```

Cypress tests can be started in two modes:


### Development / Debugging mode

Dev mode assumes that OpenRefine is up and running on the local machine, the tests themselves do not launch OpenRefine, nor restarts it.

Run :

```shell
yarn --cwd ./main/tests/cypress run cypress open
```

It will open the Cypress test runner, where you can choose, replay, visualize tests.
This is the recommended way to run tests when adding or fixing tests.  
The runners assumes

### Command-line mode

Command line mode will starts OpenRefine with a temporary folder for data

```shell
./refine ui_test chrome
```

It will run all tests in the command-line, without windows, displaying results in the standard output
This is the way to run tests in CI/CD

## Cypress brief overview

Cypress operates insides a browser, it's internally using NodeJS.
That's a key difference with tools such as selenium.  

**From the Cypress documentation:**

> But what this also means is that your test code **is being evaluated inside the browser**. Test code is not evaluated in Node, or any other server side language. The **only** language we will ever support is the language of the web: JavaScript.

Good starting points with Cypress are the [Getting started guide](https://docs.cypress.io/guides/getting-started/writing-your-first-test.html#Write-your-first-test), and the [Trade-offs](https://docs.cypress.io/guides/references/trade-offs.html#Permanent-trade-offs-1)

The general workflow of a Cypress test is to

-   Start a browser (yarn run cypress open)
-   Visit a URL
-   Trigger user actions
-   Assert that the DOM contains expected texts and elements using selectors

## Browsers

In terms of browsers, Cypress is using what is installed on your operating system.
See the [Cypress documentation](https://docs.cypress.io/guides/guides/launching-browsers.html#Browsers) for a list of supported browsers

## Folder organization

Tests are located in main/tests/cypress.
The test should not use any file outside the cypress folder.

-   `/fixtures` contains CSVs and OpenRefine project files used by the tests
-   `/integration` contains the tests
-   `/plugins` contains custom plugins for the OR project
-   `/screenshots` and `/videos` contains the recording of the tests, Git ignored
-   `/support` is a custom library of assertion and common user actions, to avoid code duplication in the tests themselves

## Configuration

Cypress execution can be configured with environment variables, they can be declared at the OS level, or when running the test

Available variables are

-   OPENREFINE_URL, determine on which scheme://url:port to access OpenRefine, default to http://localhost:333

Cypress contains and [exaustive documentation](https://docs.cypress.io/guides/guides/environment-variables.html#Setting) about configuration, but here are two simple ways to configure the execution of the tests:

### Overriding with a cypress.env.json file

This file is ignored by Git, and you can use it to configure Cypress locally

### Command-line

You can pass variables at the command-line level

```
yarn --cwd ./main/tests/cypress run cypress open --env OPENREFINE_URL="http://localhost:1234"
```

## Resources

[Cypress command line options](https://docs.cypress.io/guides/guides/command-line.html#Installation)
