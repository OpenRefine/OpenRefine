describe(__filename, function () {
  it('Join cells', function () {
    // Load a splitted dataset, one number per row,
    // expect that the joined dataset would be 1,2,3,4
    const fixture = [
      ['Column A', 'Column B'],
      ['a', '1'],
      [null, '2'],
      [null, '3'],
      [null, '4'],
      ['b', '5'],
      [null, '6'],
    ];
    cy.loadAndVisitProject(fixture);

    cy.window().then(($win) => {
      cy.stub($win, 'prompt').returns(',');
    });

    cy.columnActionClick('Column B', [
      'Edit cells',
      'Join multi-valued cells…',
    ]);

    cy.assertGridEquals([
      ['Column A', 'Column B'],
      ['a', '1,2,3,4'],
      ['b', '5,6'],
    ]);
  });
});
