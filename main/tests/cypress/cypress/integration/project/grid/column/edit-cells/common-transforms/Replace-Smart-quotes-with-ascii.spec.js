describe(__filename, function () {
  it('Ensures Smart quotes are replaced by ascii', function () {
    const fixture = [
      ['Smartquotes', 'ascii', 'noquote'],
      ['“0a”', "'0b'", '0c'],
      ['‘1a’', "'1b'", '1c'],
    ];

    cy.loadAndVisitProject(fixture);

    cy.columnActionClick('Smartquotes', [
      'Edit cells',
      'Common transforms',
      'Replace smart quotes with ASCII',
    ]);

    //  Check notification and cell content
    cy.assertNotificationContainingText(
      'Text transform on 2 cells in column Smartquotes'
    );
    cy.assertCellEquals(0, 'Smartquotes', '"0a"');
    cy.assertCellEquals(1, 'Smartquotes', "'1a'");

    cy.columnActionClick('ascii', [
      'Edit cells',
      'Common transforms',
      'Replace smart quotes with ASCII',
    ]);

    //  Check notification and cell content
    cy.assertNotificationContainingText(
      'Text transform on 0 cells in column ascii'
    );
    cy.assertCellEquals(0, 'ascii', "'0b'");
    cy.assertCellEquals(1, 'ascii', "'1b'");
  });
});
