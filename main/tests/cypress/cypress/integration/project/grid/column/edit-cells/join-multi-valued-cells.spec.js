describe(__filename, function () {
  it('Join cells', function () {
    // Load a splitted dataset, one number per row,
    // expect that the joined dataset would be 1234
    const fixture = [['A column'], ['a,b,c'], ['b'], ['c'], ['d']];
    cy.loadAndVisitProject(fixture);

    cy.window().then(($win) => {
      cy.stub($win, 'prompt').returns('\\d');
    });

    cy.columnActionClick('A column', [
      'Edit cells',
      'Join multi-valued cells...',
    ]);

    cy.assertCellEquals(0, 'A column', 'abcdef');
  });
});
