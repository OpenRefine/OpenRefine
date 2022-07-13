describe(__filename, function () {
  it('Ensure only some cells are converted to dates', function () {
    const fixture = [
      ['NDB_No', 'A Date'],
      ['01001', '2021-01-01'],
      ['01002', '2021-01-01 05:35:15'],
      ['01003', 'THIS SHOULD NOT BE TOUCHED'],
    ];
    cy.loadAndVisitProject(fixture);

    // Update grid
    cy.columnActionClick('A Date', [
      'Edit cells',
      'Common transforms',
      'To date',
    ]);

    // Check notification and cell content
    cy.assertNotificationContainingText('Text transform on 2 cells');
    cy.assertCellEquals(0, 'A Date', '2021-01-01T00:00:00Z');
    cy.assertCellEquals(1, 'A Date', '2021-01-01T05:35:15Z');
    cy.assertCellEquals(2, 'A Date', 'THIS SHOULD NOT BE TOUCHED');

    // ensure cells are marked as non-string
    cy.assertCellNotString(0, 'A Date');
    cy.assertCellNotString(1, 'A Date');
  });
});
