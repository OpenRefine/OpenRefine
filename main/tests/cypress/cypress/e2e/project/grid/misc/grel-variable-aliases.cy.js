describe('Top-level GREL record and recon variables', () => {
  it('Previews and applies the current record variable', () => {
    cy.loadAndVisitProject([
      ['key', 'value'],
      ['first', 'a'],
      [null, 'b'],
      ['second', 'c'],
    ]);
    cy.columnActionClick('value', ['Edit cells', 'Transform']);
    cy.typeExpression('record.rowCount');
    cy.get('.expression-preview-table-wrapper tr:nth-child(2) td:last-child').should('have.text', '2');
    cy.get('.expression-preview-table-wrapper tr:nth-child(3) td:last-child').should('have.text', '2');
    cy.get('.expression-preview-table-wrapper tr:nth-child(4) td:last-child').should('have.text', '1');
    cy.confirmDialogPanel();
    cy.assertCellEquals(0, 'value', '2');
    cy.assertCellEquals(1, 'value', '2');
    cy.assertCellEquals(2, 'value', '1');
  });

  it('Previews and applies the current reconciliation variable', () => {
    cy.visitOpenRefine();
    cy.navigateTo('Import project');
    cy.get('#project-tar-file-input').selectFile('cypress/fixtures/reconciled-project-automatch.zip');
    cy.get('#import-project-button').click();
    cy.columnActionClick('species', ['Edit cells', 'Transform']);
    cy.typeExpression('and(isNonBlank(recon), recon.match.id == cell.recon.match.id)');
    cy.get('.expression-preview-table-wrapper tr:nth-child(2) td:last-child').should('have.text', 'true');
    cy.confirmDialogPanel();
    cy.assertCellEquals(0, 'species', 'true');
  });
});
