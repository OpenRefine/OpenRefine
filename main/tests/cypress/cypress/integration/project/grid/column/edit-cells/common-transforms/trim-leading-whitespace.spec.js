describe(__filename, function () {
  it('Ensure multiple leading/tailing whitespaces are trimmed', function () {
    const fixture = [
      ['NDB_No', 'A column'],
      ['01001', 'TEST'],
    ];

    cy.loadAndVisitProject(fixture);
    cy.editCell(0, 'A column', '  TEST  ');

    cy.columnActionClick('A column', [
      'Edit cells',
      'Common transforms',
      'Trim leading and trailing whitespace',
    ]);

    // ensure notification and cell content
    cy.assertNotificationContainingText('Text transform on 1 cells');
    cy.assertCellEquals(0, 'A column', 'TEST');
  });
});
