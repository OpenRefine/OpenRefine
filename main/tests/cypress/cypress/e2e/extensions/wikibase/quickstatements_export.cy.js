describe('Export to QuickStatements', () => {
  afterEach(() => {
    cy.addProjectForDeletion();
  });

  // Ref: https://github.com/cypress-io/cypress-example-recipes/blob/master/examples/testing-dom__download/cypress/e2e/utils.js
  const path = require('path')
  function validateFile(filename, expectedContents) {
    const downloadsFolder = Cypress.config('downloadsFolder')
    const downloadedFilename = path.join(downloadsFolder, filename)

    cy.readFile(downloadedFilename, 'utf8', {timeout: 15000})
        .should((contents) => {
          expect(contents).to.deep.equal(expectedContents);
        });
  }

  it('Export to QuickStatements', () => {
    cy.visitOpenRefine();
    cy.navigateTo('Import project');
    cy.get('.grid-layout').should('to.contain', 'Locate an existing Refine project file');
    
    // load a project with a predefined schema
    cy.get('#project-tar-file-input').attachFile('wikidata-schema.tar.gz.zip')
    cy.get('#import-project-button').click();

    cy.getCell(0, 'item').should('to.contain', 'Choose new match');

    // Open the wikibase extension
    cy.get('#extension-bar-menu-container').contains('Wikibase').click();
    cy.get('.menu-container a').contains('Edit Wikibase schema').click();
    cy.get('#wikibase-issues-panel').should('to.exist');

    // Export the project to QuickStatements
    cy.get('#extension-bar-menu-container').contains('Wikibase').click();
    cy.get('.menu-container a').contains('Export to QuickStatements').click();

    var expectedContents = 'Q4115189\tP1448\ten:"hello"\tP580\t+2018-09-04T00:00:00Z/11\tS143\tQ328\nQ46611\tP1448\ten:"world"\tP580\t+2018-09-04T00:00:00Z/11\tS143\tQ328\n';

    cy.get('.app-path-section').invoke('text').then((name)=>{
      validateFile(`statements.txt`, expectedContents);
    });
  });
});
    
