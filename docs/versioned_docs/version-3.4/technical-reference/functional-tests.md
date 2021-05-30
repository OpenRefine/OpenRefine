---
id: functional-tests
title: Functional tests
sidebar_label: Functional tests
---

import useBaseUrl from '@docusaurus/useBaseUrl';

## Introduction {#introduction}

OpenRefine interface is tested with the [Cypress framework](https://www.cypress.io/).  
With Cypress, tests are performing assertions using a real browser, the same way a real user would use the software.

Cypress tests can be ran

- using the Cypress test runner (development mode)
- using a command line (CI/CD mode)

If you are writing tests, the Cypress test runner is good enough, and the command-line is mainly used by the CI/CD platform (Github actions)

## Cypress brief overview {#cypress-brief-overview}

Cypress operates insides a browser, it's internally using NodeJS.
That's a key difference with tools such as Selenium.

**From the Cypress documentation:**

> But what this also means is that your test code **is being evaluated inside the browser**. Test code is not evaluated in Node, or any other server side language. The **only** language we will ever support is the language of the web: JavaScript.

Good starting points with Cypress are the [Getting started guide](https://docs.cypress.io/guides/getting-started/writing-your-first-test.html#Write-your-first-test), and the [Trade-offs](https://docs.cypress.io/guides/references/trade-offs.html#Permanent-trade-offs-1)

The general workflow of a Cypress test is to

- Start a browser (yarn run cypress open)
- Visit a URL
- Trigger user actions
- Assert that the DOM contains expected texts and elements using selectors

## Getting started {#getting-started}

If this is the first time you use Cypress, it is recommended for you to get familiar with the tool.

- [Cypress overview](https://docs.cypress.io/guides/overview/why-cypress.html)
- [Cypress examples of tests and syntax](https://example.cypress.io/)

### 1. Install Cypress {#1-install-cypress}

You will need:

- [Node.js 10 or 12 and above](https://nodejs.org)
- [Yarn or NPM](https://yarnpkg.com/)
- A Unix/Linux shell environment or the Windows command line

To install Cypress and dependencies, run :

```shell
cd ./main/tests/cypress
yarn install
```

### 2. Start the test runner {#2-start-the-test-runner}

The test runner assumes that OpenRefine is up and running on the local machine, the tests themselves do not launch OpenRefine, nor restarts it.

Start OpenRefine with

```shell
./refine
```

Then start Cypress

```shell
yarn --cwd ./main/tests/cypress run cypress open
```

### 3. Run the existing tests {#3-run-the-existing-tests}

Once the test runner is up, you can choose to run one or several tests by selecting them from the interface.  
Click on one of them and the test will start.

### 4. Add your first test {#4-add-your-first-test}

- Add a `test.spec.js` into the `main/tests/cypress/cypress/integration` folder.
- The test is instantly available in the list
- Click on the test
- Start to add some code

## Tests technical documentation {#tests-technical-documentation}

### A typical test {#a-typical-test}

A typical OpenRefine test starts with the following code

```javascript
it('Ensure cells are blanked down', function () {
  cy.loadAndVisitProject('food.mini');
  cy.get('.viewpanel-sorting a').contains('Sort').click();
  cy.get('.viewpanel').should('to.contain', 'Something');
});
```

The first noticeable thing about a test is the description (`Ensure cells are blanked down`), which describes what the test is doing.  
Lines usually starts with `cy.something...`, which is the main way to interact with the Cypress framework.

A few examples:

- `cy.get('a.my-class')` will retrieve the `<a class="my-class" />` element
- `cy.click()` will click on the element
- eventually, `cy.should()` will perform an assertion, for example that the element contains an expected text with `cy.should('to.contains', 'my text')`

On top of that, OpenRefine contributors have added some functions for common OpenRefine interactions.
For example

- `cy.loadAndVisitProject` will create a fresh project in OpenRefine
- `cy.assertCellEquals` will ensure that a cell contains a given value

See below on the dedicated section 'Testing utilities'

### Testing guidelines {#testing-guidelines}

- `cy.wait` should be used in the last resort scenario. It's considered a bad practice, though sometimes there is no other choice
- Tests should remain isolated from each other. It's best to try one feature at the time
- A test should always start with a fresh project
- The name of the files should mirror the OpenRefine UI organization

### Testing utilities {#testing-utilities}

OpenRefine contributors have added some utility methods on the top of the Cypress framework.
Those methods perform some common actions or assertions on OpenRefine, to avoid code duplication.

Utilities can be found in `cypress/support/commands.js`.

The most important utility method is `loadAndVisitProject`.  
This method will create a fresh OpenRefine project based on a dataset given as a parameter.  
The fixture parameter can be

- An arbitrary array, the first row is for the column names, other rows are for the values  
  Use an arbitrary array **only** if the test requires some specific grid values  
  **Example:**

  ```javascript
  const fixture = [
    ['Column A', 'Column B', 'Column C'],
    ['0A', '0B', '0C'],
    ['1A', '1B', '1C'],
    ['2A', '2B', '2C'],
  ];
  cy.loadAndVisitProject(fixture);
  ```

- A referenced dataset: `food.small` or `food.mini`  
  Most of the time, tests does not require any specific grid values  
  Use food.mini as much as possible, it loads 2 rows and very few columns in the grid  
  Use food.small if the test requires a few hundred rows in the grid

  Those datasets live in `cypress/fixtures`

### Browsers {#browsers}

In terms of browsers, Cypress is using what is installed on your operating system.
See the [Cypress documentation](https://docs.cypress.io/guides/guides/launching-browsers.html#Browsers) for a list of supported browsers

### Folder organization {#folder-organization}

Tests are located in `main/tests/cypress/cypress` folder.
The test should not use any file outside the cypress folder.

- `/fixtures` contains CSVs and OpenRefine project files used by the tests
- `/integration` contains the tests
- `/plugins` contains custom plugins for the OR project
- `/screenshots` and `/videos` contains the recording of the tests, Git ignored
- `/support` is a custom library of assertion and common user actions, to avoid code duplication in the tests themselves

### Configuration {#configuration}

Cypress execution can be configured with environment variables, they can be declared at the OS level, or when running the test

Available variables are

- OPENREFINE_URL, determine on which scheme://url:port to access OpenRefine, default to http://localhost:333

Cypress contains [exaustive documentation](https://docs.cypress.io/guides/guides/environment-variables.html#Setting) about configuration, but here are two simple ways to configure the execution of the tests:

#### Overriding with a cypress.env.json file {#overriding-with-a-cypressenvjson-file}

This file is ignored by Git, and you can use it to configure Cypress locally

#### Command-line {#command-line}

You can pass variables at the command-line level

```shell
yarn --cwd ./main/tests/cypress run cypress open --env OPENREFINE_URL="http://localhost:1234"
```

### Visual testing {#visual-testing}

Tests generally ensure application behavior by making assertions against the DOM, to ensure specific texts or css attributes are present in the document body.  
Visual testing, on the contrary, is a way to test applications by comparing images.
A reference screenshot is taken the first time the test runs, and subsequent runs will compare a new screenshot against the reference, at the pixel level.

Here is an [introduction to visual testing by Cypress](https://docs.cypress.io/plugins/directory#visual-testing).

In some cases, we are using visual testing.  
We are using [Cypress Image Snapshot](https://github.com/jaredpalmer/cypress-image-snapshot)

Identified cases are so far:

- testing images created by OpenRefine backend (scatterplots for example)

Reference screenshots (Called snapshots), are stored in /cypress/snapshots.
And a snapshot can be taken for the whole page, or just a single part of the page.

#### When a visual test fails {#when-a-visual-test-fails}

First, Cypress will display the following error message:

![Diff image when a visual test fails](/img/visual-test-cypress-failure.png)

Then, a diff image will be created in /cypress/snapshots, this directory is ignored by Git.  
The diff images shows the reference image on the left, the image that was taken during the test run on the right, and the diff in the middle.

![Diff image when a visual test fails](/img/failed-visual-test.png)

## CI/CD {#cicd}

In CI/CD, tests are run headless, with the following command-line

```shell
./refine ui_test chrome
```

Results are displayed in the standard output

## Resources {#resources}

[Cypress command line options](https://docs.cypress.io/guides/guides/command-line.html#Installation)
[Lots of good Cypress examples](https://example.cypress.io/)
