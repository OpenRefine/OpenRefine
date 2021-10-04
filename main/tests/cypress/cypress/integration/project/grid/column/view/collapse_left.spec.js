describe(__filename, function () {
const fixture = [
        ['a', 'b', 'c'],

        ['0a', '0b', '0c'],
        ['1a', '1b', '1c']
      ];
  it('it collapses all columns to left', function () {
    cy.loadAndVisitProject(fixture);
    cy.columnActionClick('c', ['View', 'Collapse all columns to left']);

    cy.get('[title="a"]').should('to.contain', '');
    cy.get('[title="b"]').should('to.contain', '');

  });
});
