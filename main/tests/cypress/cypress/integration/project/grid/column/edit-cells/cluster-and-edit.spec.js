describe(__filename, function () {
  it('Ensure cells are filled down', function () {
    const fixture = [
      ['location'],

      ['SWABYS HOME'],
      ['SWABYS   HOME'],
      ['BALLARDS river'],
      ['BALLARDS River'],
      ['MOUNT ZION'],
      ['GRAVEL HILL']
      
    ];

    cy.loadAndVisitProject(fixture);
    // // click
    // cy.columnActionClick('b', ['Edit cells', 'Fill down']);

    // // ensure notification and cell content
    // cy.assertNotificationContainingText('Fill down 3 cells in column b');
    // cy.assertCellEquals(0, 'b', '0b'); // untouched
    // cy.assertCellEquals(1, 'b', '0b'); // filled
    // cy.assertCellEquals(2, 'b', '2b'); // untouched
    // cy.assertCellEquals(3, 'b', '2b'); // filled
    // cy.assertCellEquals(4, 'b', '2b'); // filled
    // cy.assertCellEquals(5, 'b', '5b'); // untouched
  });


  // Layout
  // select all / unselect
  // Issue-4004
  // Change method -> expected inputs
  // Merge & Close (multiple merge)
  // Merge & Re-cluster
});
