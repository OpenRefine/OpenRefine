describe('Facet by judgment', () => {
  afterEach(() => {
    cy.addProjectForDeletion();
  });
  
  it('Facets by judgment', () => {
    cy.visitOpenRefine();
    cy.navigateTo('Import project');
    cy.get('#or-import-locate').should('to.contain', 'Locate an existing Refine project file');

    //we're using here the "automatched" project, so we can test that the facet contains choice for matched and non-matched judgments
    cy.get('#project-tar-file-input').attachFile('reconciled-project-automatch.zip')
    cy.get('#import-project-button').click();

    cy.columnActionClick('species', ['Reconcile', 'Facets', 'By judgment']);

    // ensure a new facet has been added
    cy.getFacetContainer('species: judgment').should('exist').should('to.contain', 'matched').should('to.contain', 'none')
  });
});
