describe('Use values as identifiers', () => {
  it('Test clearing reconciliation for a reconciled dataset', () => {
    const fixture = [
      ['identifier'],
      ['2253634'],
      ['2328088'],
      ['2868241'],
      [null],
      ['8211794'],
      [null],
    ];

    cy.loadAndVisitProject(fixture);

    cy.columnActionClick('identifier', [
      'Reconcile',
      'Use values as identifiers',
    ]);

    cy.get('.dialog-container .dialog-footer button').contains('OK').click();

    // ensure column is reconciled
    cy.assertColumnIsReconciled('identifier');

    // ensure 4 rows are matched based on the identifier
    cy.get(
      'table.data-table td .data-table-cell-content:contains("Choose new match")'
    ).should('have.length', 4);
  });
});
