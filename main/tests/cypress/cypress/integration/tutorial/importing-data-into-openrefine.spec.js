// This spec is implementation for chapter 02 of following tutorial https://librarycarpentry.org/lc-open-refine/02-importing-data/index.html

describe(__filename, function () {
  afterEach(() => {
    cy.addProjectForDeletion();
  });
  
  it('Create your first OpenRefine project (using provided data)', function () {
    // For sake of the tutorial we have already downloaded the data into fixtures
    // Step-1:Once OpenRefine is launched in your browser, click Create project from the left hand menu and select Get data from This Computer
    cy.visitOpenRefine();
    cy.navigateTo('Create project');
    cy.get('#create-project-ui-source-selection-tabs > div')
      .contains('This Computer')
      .click();
    // Step-2 Click Choose Files (or ‘Browse’, depending on your setup) and locate the file which you have downloaded called doaj-article-sample.csv
    cy.get('.create-project-ui-source-selection-tab-body.selected').contains(
      'Locate one or more files on your computer to upload'
    );
    // add file
    const csvFile = {
      filePath: 'doaj-article-sample.csv',
      mimeType: 'application/csv',
    };
    cy.get(
      '.create-project-ui-source-selection-tab-body.selected input[type="file"]'
    ).attachFile(csvFile);
    cy.get(
      '.create-project-ui-source-selection-tab-body.selected button.button-primary'
    )
      .contains('Next »')
      .click();

    // then ensure we are on the preview page
    cy.get('.create-project-ui-panel').contains('Configure parsing options');
    // Step-3 Click in the Character encoding box and set it to UTF-8

    cy.get('input[bind="encodingInput"]').should('have.value', 'UTF-8');

    // step-4 Configure the import options
    cy.get('input[bind="trimStringsCheckbox"]').check();

    // create the project and ensure its successful
    // wait until the grid appear, this ensure the job is ready
    cy.get('div[bind="dataPanel"] table.data-table').should('to.exist');
    cy.get('.default-importing-wizard-header button[bind="nextButton"]')
      .contains('Create project »')
      .click();

    cy.waitForProjectTable(1001);
  });
});
