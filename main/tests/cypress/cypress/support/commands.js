// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************

import 'cypress-file-upload';

/**
 * Check that a column is reconciled
 */
Cypress.Commands.add('assertColumnIsReconciled', (columnName) => {
  cy.get(
    `table.data-table thead th[title="${columnName}"] div.column-header-recon-stats-matched`
  ).should('to.exist');
});

/**
 * Return the .facets-container for a given facet name
 */
Cypress.Commands.add('getFacetContainer', (facetName) => {
  return cy
    .get(
      `#refine-tabs-facets .facets-container .facet-container span[bind="titleSpan"]:contains("${facetName}")`,
      { log: false }
    )
    .parentsUntil('.facets-container', { log: false });
});

Cypress.Commands.add('getNumericFacetContainer', (facetName) => {
  return cy
    .get(
      `#refine-tabs-facets .facets-container .facet-container span[bind="facetTitle"]:contains("${facetName}")`,
      { log: false }
    )
    .parentsUntil('.facets-container', { log: false });
});

/**
 * Edit a cell, for a given row index, a column name and a value
 */
Cypress.Commands.add('editCell', (rowIndex, columnName, value) => {
  cy.getCell(rowIndex, columnName)
    .trigger('mouseover')
    .find('button.data-table-cell-edit')
    .click();
  cy.get('.menu-container.data-table-cell-editor textarea').type(value);
  cy.get('.menu-container button[bind="okButton"]').click();
});

/**
 * Ensure a textarea have a value that id equal to the JSON given as parameter
 */
Cypress.Commands.add('assertTextareaHaveJsonValue', (selector, json) => {
  cy.get(selector).then((el) => {
    // expected json needs to be parsed / restringified, to avoid inconsistencies about spaces and tabs
    const present = JSON.parse(el.val());
    cy.expect(JSON.stringify(present)).to.equal(JSON.stringify(json));
  });
});

/**
 * Open OpenRefine
 */
Cypress.Commands.add('visitOpenRefine', (options) => {
  cy.visit(Cypress.env('OPENREFINE_URL'), options);
});

Cypress.Commands.add('createProjectThroughUserInterface', (fixtureFile) => {
  cy.navigateTo('Create project');

  const uploadFile = { filePath: fixtureFile, mimeType: 'application/csv' };
  cy.get(
    '.create-project-ui-source-selection-tab-body.selected input[type="file"]'
  ).attachFile(uploadFile);
  cy.get(
    '.create-project-ui-source-selection-tab-body.selected button.button-primary'
  ).click();
});

/**
 * Cast a whole column to the given type, using Edit Cell / Common transform / To {type}
 */
Cypress.Commands.add('castColumnTo', (selector, target) => {
  cy.get('body[ajax_in_progress="false"]');
  cy.get(
    '.data-table th:contains("' + selector + '") .column-header-menu'
  ).click();

  const targetAction = 'To ' + target;

  cy.get('body > .menu-container').eq(0).contains('Edit cells').click();
  cy.get('body > .menu-container').eq(1).contains('Common transforms').click();
  cy.get('body > .menu-container').eq(2).contains(targetAction).click();
});

/**
 * Return the td element for a given row index and column name
 */
Cypress.Commands.add('getCell', (rowIndex, columnName) => {
  const cssRowIndex = rowIndex + 1;
  // first get the header, to know the cell index
  cy.get(`table.data-table thead th[title="${columnName}"]`).then(($elem) => {
    // there are 3 td at the beginning of each row
    const columnIndex = $elem.index() + 3;
    return cy.get(
      `table.data-table tbody tr:nth-child(${cssRowIndex}) td:nth-child(${columnIndex})`
    );
  });
});

/**
 * Make an assertion about the content of a cell, for a given row index and column name
 */
Cypress.Commands.add('assertCellEquals', (rowIndex, columnName, value) => {
  const cssRowIndex = rowIndex + 1;
  // first get the header, to know the cell index
  cy.get(`table.data-table thead th[title="${columnName}"]`).then(($elem) => {
    // there are 3 td at the beginning of each row
    const columnIndex = $elem.index() + 3;
    cy.get(
      `table.data-table tbody tr:nth-child(${cssRowIndex}) td:nth-child(${columnIndex}) div.data-table-cell-content div > span`
    ).should(($cellSpan) => {
      if (value == null) {
        // weird, "null" is returned as a string in this case, bug in Chai ?
        expect($cellSpan.text()).equals('null');
      } else {
        expect($cellSpan.text()).equals(value);
      }
    });
  });
});

/**
 * Make an assertion about the content of the whole grid
 * The values parameter is the same as the one provided in fixtures
 * Example
 * cy.assertGridEquals(
 *   [
 *     ['Column A', 'Column B', 'Column C'],
 *     ['Row 0 A', 'Row 0 B', 'Row 0 C'],
 *     ['Row 1 A', 'Row 1 B', 'Row 1 C']
 *   ]
 * )
 */
Cypress.Commands.add('assertGridEquals', (values) => {
  /**
   * This assertion is performed inside a should() method
   * So it will be retried until the timeout expires
   * "Should()" expect assertions to be made, so promises can't be used
   * Hence the use of Jquery with the Cypress.$ wrapper, to collect values for headers and grid cells
   */
  cy.get('table.data-table').should((table) => {
    const headers = Cypress.$('table.data-table th')
      .filter(function (index, element) {
        return element.innerText !== 'All';
      })
      .map(function (index, element) {
        return element.innerText;
      })
      .get();

    const cells = Cypress.$('table.data-table tbody tr')
      .map(function (i, el) {
        const innerTexts = Cypress.$('td', el).filter(index => index > 2)
          .map(function (index, element) {
            return element.querySelector('div.data-table-cell-content div > span').innerText;
          }).get();
        return [ innerTexts
          .map(function (innerText) {
            // a nulled cell value is exposed in the DOM as the string "null"
            return innerText === 'null' ? null : innerText
          }) ];
      })
      .get();
    const fullTable = [headers, ...cells];
    expect(fullTable).to.deep.equal(values);
  });
});

/**
 * Navigate to one of the entries of the main left menu of OpenRefine (Create project, Open Project, Import Project, Language Settings)
 */
Cypress.Commands.add('navigateTo', (target) => {
  cy.get('#action-area-tabs li').contains(target).click();
});

/**
 * Utility method to fill something into the expression input
 */
Cypress.Commands.add('typeExpression', (expression, options = {}) => {
    cy.get('textarea.expression-preview-code', options).clear().type(expression);
    const expectedText = expression.length <= 30 ? expression : `${expression.substring(0, 30)} ...`;
    cy.get('tbody > tr:nth-child(1) > td:nth-child(3)', options).should('contain', expectedText);
});

/**
 * Utility method to select the Python/Jython interpreter
 */
Cypress.Commands.add('selectPython', () => {
  cy.get('textarea.expression-preview-code').clear()
  cy.get('select[bind="expressionPreviewLanguageSelect"]').select('jython');
  // Wait for Jython interpreter to load (as indicated by changed error message)
  cy.get('.expression-preview-parsing-status').contains('Syntax error');
});

/**
 * Utility method to select the Clojure interpreter
 */
Cypress.Commands.add('selectClojure', () => {
  cy.get('textarea.expression-preview-code').clear().type('(');
  cy.get('select[bind="expressionPreviewLanguageSelect"]').select('clojure');
  // Wait for Clojure interpreter to load (as indicated by changed error message)
  cy.get('.expression-preview-parsing-status').contains('Syntax error reading source');
});

/**
 * Delete a column from the grid
 */
Cypress.Commands.add('deleteColumn', (columnName) => {
  cy.get('.data-table th[title="' + columnName + '"]').should('exist');
  cy.columnActionClick(columnName, ['Remove column']);
  cy.get('.data-table th[title="' + columnName + '"]').should('not.exist');
});

/**
 * Wait until a dialog panel appear
 */
Cypress.Commands.add('waitForDialogPanel', () => {
  cy.get('body > .dialog-container > .dialog-frame').should('be.visible');
});

/**
 * Click on the OK button of a dialog panel
 */
Cypress.Commands.add('confirmDialogPanel', () => {
  cy.get(
    'body > .dialog-container > .dialog-frame .dialog-footer button[bind="okButton"]'
  ).click();
  cy.get('body > .dialog-container > .dialog-frame').should('not.exist');
});

/**
 * Click on the Cancel button of a dialog panel
 */
Cypress.Commands.add('cancelDialogPanel', () => {
  cy.get(
    'body > .dialog-container > .dialog-frame .dialog-footer button[bind="cancelButton"]'
  ).click();
  cy.get('body > .dialog-container > .dialog-frame').should('not.exist');
});

/**
 * Will click on a menu entry for a given column name
 */
Cypress.Commands.add('columnActionClick', (columnName, actions) => {
  cy.get('body[ajax_in_progress="false"]'); // OR must not be loading at the moment, column headers will be detached from the dom
  cy.get(
    '.data-table th:contains("' + columnName + '") .column-header-menu'
  ).click();

  for (let i = 0; i < actions.length; i++) {
    cy.get('body > .menu-container').eq(i).contains(actions[i]).click();
  }
  cy.get('body[ajax_in_progress="false"]');
});

/**
 * Go to a project, given it's id
 */
Cypress.Commands.add('visitProject', (projectId) => {
  cy.visit(Cypress.env('OPENREFINE_URL') + '/project?project=' + projectId);
  cy.get('#project-title').should('exist');
});

/**
 * Load a new project in OpenRefine, and open the project
 * The fixture can be
 *   * an arbitrary array that will be loaded in the grid. The first row is for the columns names
 *   * a file referenced in fixtures.js (food.mini | food.small)
 */
Cypress.Commands.add(
    'loadAndVisitProject',
    (fixture, projectName = Cypress.currentTest.title +'-'+Date.now()) => {
      cy.loadProject(fixture, projectName, "fooTag").then((projectId) => {
        cy.visit(Cypress.env('OPENREFINE_URL') + '/project?project=' + projectId);
        cy.waitForProjectTable();
      });
    }
);

Cypress.Commands.add('waitForProjectTable', (numRows) => {
  cy.url().should('contain', '/project?project=')
  cy.get('#left-panel', { log: false }).should('be.visible');
  cy.get('#right-panel', { log: false }).should('be.visible');
  cy.get('#project-title').should('exist');
  cy.get(".data-table").find("tr").its('length').should('be.gte', 0);
  if (numRows) {
    cy.get('#summary-bar').should('to.contain', numRows.toLocaleString('en')+' rows');
  }
});

Cypress.Commands.add('assertNotificationContainingText', (text) => {
  cy.get('#notification-container').should('be.visible');
  cy.get('#notification').should('be.visible').should('to.contain', text);
});

Cypress.Commands.add(
  'assertCellNotString',
  (rowIndex, columnName, expectedType) => {
    cy.getCell(rowIndex, columnName)
      .find('.data-table-value-nonstring')
      .should('to.exist');
  }
);

/**
 * Performs drag and drop on target and source item
 * sourceSelector - jquery selector for the element to be dragged
 * targetSelector - jquery selector for the element to be dropped on
 * position - position relative to the target element to perform the drop
 */
Cypress.Commands.add(
  'dragAndDrop',
  (sourceSelector, targetSelector, position = 'center', sourcePosition = 'center') => {
  cy.get(sourceSelector).trigger('mousedown', sourcePosition, { which: 1 });

  cy.get(targetSelector) // eslint-disable-line
    .trigger('mousemove',position)
    .trigger('mouseup', { force: true });
});

Cypress.Commands.add(
  'loadAndVisitSampleJSONProject',
  (projectName, fixture) => {
    const jsonText = JSON.stringify(fixture)
    cy.visitOpenRefine();
    cy.navigateTo('Create project');
    cy.get('#create-project-ui-source-selection-tabs > a')
      .contains('Clipboard')
      .click();
    cy.get('textarea').invoke('val', jsonText);
    cy.get(
      '.create-project-ui-source-selection-tab-body.selected button.button-primary'
    )
      .contains('Next »')
      .click();
    cy.get(
      '.default-importing-wizard-header input[bind="projectNameInput"]'
    ).clear();
    cy.get(
      '.default-importing-wizard-header input[bind="projectNameInput"]'
    ).type(projectName);
    // need to disable es-lint as force is required to true, if not then
    // cypress won't detect the element due to `position:fixed` and overlays
    cy.get('[data-cy=element0]') // eslint-disable-line
      .first()
      .scrollIntoView()
      .click({ force: true });

    // wait for preview and click next to create the project
    cy.get('div[bind="dataPanel"] table.data-table').should('to.exist');
    cy.get('.default-importing-wizard-header button[bind="nextButton"]')
      .contains('Create project »')
      .click();
    cy.waitForProjectTable();
  }
);
