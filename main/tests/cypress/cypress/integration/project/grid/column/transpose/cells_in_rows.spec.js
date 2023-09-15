/**
 * The following scenarios are inspired by the official OpenRefine documentation
 * https://docs.openrefine.org/manual/transposing/
 */
describe(__filename, function () {
  /**
   * https://docs.openrefine.org/manual/transposing/#transpose-cells-in-rows-into-columns
   */
  it('Transpose cells in rows into columns', function () {
    const fixture = [
      ['Employee'],
      ['Employee: Karen Chiu'],
      ['Job title: Senior analyst'],
      ['Office: New York'],
      ['Employee: Joe Khoury'],
      ['Job title: Junior analyst'],
      ['Office: Beirut'],
      ['Employee: Samantha Martinez'],
      ['Job title: CTO'],
      ['Office: Tokyo'],
    ];
    cy.loadAndVisitProject(fixture);

    // the number of columns is prompted with an alert
    // need to mock it
    cy.window().then(($win) => {
      cy.stub($win, 'prompt').returns('3');
    });

    cy.columnActionClick('Employee', [
      'Transpose',
      'Transpose cells in rows into columns...',
    ]);

    const expected = [
      ['Employee 1', 'Employee 2', 'Employee 3'],
      ['Employee: Karen Chiu', 'Job title: Senior analyst', 'Office: New York'],
      ['Employee: Joe Khoury', 'Job title: Junior analyst', 'Office: Beirut'],
      ['Employee: Samantha Martinez', 'Job title: CTO', 'Office: Tokyo'],
    ];
    cy.assertGridEquals(expected);
  });
});
